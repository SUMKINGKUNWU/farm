<template>
  <main class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">穗</div>
        <div>
          <p>Farm Exchange</p>
          <h1>{{ activeMode === 'player' ? '玩家农场' : '运营管理台' }}</h1>
        </div>
      </div>

      <div class="mode-switch">
        <button :class="{ active: activeMode === 'player' }" type="button" @click="activeMode = 'player'">玩家端</button>
        <button :class="{ active: activeMode === 'admin' }" type="button" @click="activeMode = 'admin'">管理台</button>
      </div>

      <PlayerIdentity v-if="activeMode === 'player'" />
      <AdminIdentity v-else />

      <nav class="nav" v-if="activeMode === 'player'">
        <a href="#player-overview">概览</a>
        <a href="#player-shop">商店</a>
        <a href="#player-fields">田地牧场</a>
        <a href="#player-market">交易站</a>
        <a href="#player-private">私下交易</a>
      </nav>
      <nav class="nav" v-else>
        <a href="#tax">税率</a>
        <a href="#token">令牌</a>
        <a href="#assets">资产</a>
        <a href="#trades">交易</a>
      </nav>
    </aside>

    <section class="workspace">
      <GamePlayerWorkspace v-if="activeMode === 'player'" />
      <AdminWorkspace v-else />
    </section>
  </main>
</template>
<script setup>
import { computed, defineComponent, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { useAdminStore } from './stores/adminStore'
import { usePlayerStore } from './stores/playerStore'
import GamePlayerWorkspace from './components/GamePlayerWorkspace.vue'
import NoticeBlock from './components/common/NoticeBlock.vue'
import StatCard from './components/common/StatCard.vue'

const activeMode = ref('player')
const admin = useAdminStore()
const player = usePlayerStore()

const adminTaxTypes = [
  { code: 'MARKET', name: '交易站' },
  { code: 'PRIVATE', name: '私下交易' },
  { code: 'BULK', name: '大宗交易' }
]

const taxForm = reactive({
  MARKET: { rateBasisPoints: 300, reason: '' },
  PRIVATE: { rateBasisPoints: 500, reason: '' },
  BULK: { rateBasisPoints: 300, reason: '' }
})

const tokenForm = reactive({
  allowedItemType: 'HARVEST',
  singleTradeLimit: 200000,
  totalLimit: 200000,
  remainingUses: 1,
  expireHours: 24
})

const playerForms = reactive({
  tradePassword: '',
  shopItemCode: 'WHEAT_SEED',
  shopQuantity: 1,
  shopTradePassword: '',
  farmItemCode: 'WHEAT_SEED',
  ranchItemCode: 'CHICKEN',
  marketItemCode: 'WHEAT',
  marketQuantity: 10,
  marketTradePassword: '',
  marketBulkTokenCode: '',
  harvestGrowthId: '',
  privateBuyerUserId: '',
  privateItemCode: 'WHEAT',
  privateQuantity: 10,
  privatePriceAmount: 500,
  privateTradePassword: '',
  privateAcceptPassword: '',
  privateBulkTokenCode: ''
})

function formatRate(value) {
  if (value === undefined || value === null) return '-'
  return `${(Number(value) / 100).toFixed(2)}%`
}

function formatMoney(value) {
  if (value === undefined || value === null) return '-'
  return Number(value).toLocaleString('zh-CN')
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

function formatDuration(seconds) {
  if (!seconds) return '-'
  if (seconds < 3600) return `${Math.ceil(seconds / 60)} 鍒嗛挓`
  return `${(seconds / 3600).toFixed(1)} 灏忔椂`
}

function slotLabel(slot) {
  if (!slot) return '-'
  return `#${slot.slotIndex} ${slot.status}`
}

function growthBySlot(slotId) {
  return player.activeGrowths.find((growth) => growth.slotId === slotId)
}

function isReady(growth) {
  return Boolean(growth && new Date(growth.readyAt).getTime() <= Date.now())
}

function slotCells(slotList) {
  const slots = slotList?.slots || []
  const maxSlots = slotList?.maxSlots || 16
  return Array.from({ length: maxSlots }, (_, index) => {
    const slot = slots[index]
    return {
      index: index + 1,
      unlocked: Boolean(slot),
      slot,
      growth: slot ? growthBySlot(slot.id) : null
    }
  })
}

async function saveTax(type) {
  await admin.updateTaxConfig(type, taxForm[type])
}

async function issueToken() {
  await admin.issueBulkToken(tokenForm)
}

async function loadAdminEverything() {
  await admin.loadTaxConfigs()
  if (admin.targetUserId) {
    await admin.loadPlayerConsole()
  }
}

async function purchaseShopItem() {
  await player.purchase({
    itemCode: playerForms.shopItemCode,
    quantity: playerForms.shopQuantity,
    tradePassword: playerForms.shopTradePassword
  })
}

async function startFarmProduction(slotId) {
  await player.startProduction('FARM', slotId, playerForms.farmItemCode)
}

async function startRanchProduction(slotId) {
  await player.startProduction('RANCH', slotId, playerForms.ranchItemCode)
}

async function tradeMarket(side) {
  await player.marketTrade(side, {
    itemCode: playerForms.marketItemCode,
    quantity: playerForms.marketQuantity,
    tradePassword: playerForms.marketTradePassword,
    bulkTokenCode: playerForms.marketBulkTokenCode || null
  })
}

async function createPrivateTrade() {
  await player.createPrivateTrade({
    buyerUserId: playerForms.privateBuyerUserId,
    itemCode: playerForms.privateItemCode,
    quantity: playerForms.privateQuantity,
    priceAmount: playerForms.privatePriceAmount,
    tradePassword: playerForms.privateTradePassword
  })
}

async function acceptPrivateTrade(offerId) {
  await player.acceptPrivateTrade(offerId, {
    tradePassword: playerForms.privateAcceptPassword,
    bulkTokenCode: playerForms.privateBulkTokenCode || null
  })
}

async function harvestGrowth(growthId) {
  await player.harvest(growthId)
}

function syncTaxForm(configs) {
  configs.forEach((config) => {
    if (taxForm[config.tradeType]) {
      taxForm[config.tradeType].rateBasisPoints = config.rateBasisPoints
      taxForm[config.tradeType].reason = config.updatedReason || ''
    }
  })
}

const PlayerIdentity = defineComponent({
  components: { NoticeBlock },
  setup() {
    return { player }
  },
  template: `
    <div class="identity-card">
      <template v-if="!player.isLoggedIn">
        <label>鐜╁鐢ㄦ埛鍚?input v-model.trim="player.username" placeholder="渚嬪 farmer_001" /></label>
        <label>鐜╁鏄电О<input v-model.trim="player.nickname" placeholder="娉ㄥ唽鏃跺～鍐欐樀绉? /></label>
        <label>鐧诲綍瀵嗙爜<input v-model="player.password" type="password" placeholder="鑷冲皯 6 浣? @keydown.enter="player.login" /></label>
        <div class="button-row">
          <button class="button ghost" type="button" :disabled="player.loading" @click="player.login">鐧诲綍</button>
          <button class="button subtle" type="button" :disabled="player.loading" @click="player.register">娉ㄥ唽骞剁櫥褰?/button>
        </div>
      </template>
      <template v-else>
        <div class="login-badge">
          <span>褰撳墠鐜╁</span>
          <strong>{{ player.currentUser?.username || player.username || '宸茬櫥褰? }}</strong>
        </div>
        <button class="button ghost" type="button" :disabled="player.loading" @click="player.loadDashboard">鍒锋柊鍐滃満</button>
        <button class="button subtle" type="button" @click="player.logout">閫€鍑虹櫥褰?/button>
      </template>
    </div>
  `
})

const AdminIdentity = defineComponent({
  setup() {
    return { admin, loadAdminEverything }
  },
  template: `
    <div class="identity-card">
      <template v-if="!admin.isLoggedIn">
        <label>绠＄悊鍛樼敤鎴峰悕<input v-model.trim="admin.username" placeholder="璇疯緭鍏ョ鐞嗗憳鐢ㄦ埛鍚? /></label>
        <label>鐧诲綍瀵嗙爜<input v-model="admin.password" type="password" placeholder="璇疯緭鍏ョ櫥褰曞瘑鐮? @keydown.enter="admin.login" /></label>
        <button class="button ghost" type="button" :disabled="admin.loading" @click="admin.login">鐧诲綍绠＄悊鍙?/button>
      </template>
      <template v-else>
        <div class="login-badge">
          <span>褰撳墠绠＄悊鍛?/span>
          <strong>{{ admin.currentUser?.username || admin.username || '宸茬櫥褰? }}</strong>
        </div>
        <label>鐩爣鐜╁鐢ㄦ埛 ID<input v-model.trim="admin.targetUserId" placeholder="瑕佹煡璇㈡垨鍙戜护鐗岀殑鐜╁ UUID" /></label>
        <button class="button ghost" type="button" :disabled="admin.loading" @click="loadAdminEverything">鍒锋柊鎺у埗鍙?/button>
        <button class="button subtle" type="button" @click="admin.logout">閫€鍑虹櫥褰?/button>
      </template>
    </div>
  `
})

const PlayerWorkspace = defineComponent({
  components: { NoticeBlock, StatCard },
  setup() {
    const seedOptions = computed(() => player.seeds.length ? player.seeds : [{ code: 'WHEAT_SEED', name: '灏忛害绉嶅瓙' }, { code: 'CORN_SEED', name: '鐜夌背绉嶅瓙' }])
    const animalOptions = computed(() => player.animals.length ? player.animals : [{ code: 'CHICKEN', name: '楦¤嫍' }, { code: 'COW', name: '濂剁墰' }])
    const harvestOptions = computed(() => player.harvests.length ? player.harvests : [{ code: 'WHEAT', name: '灏忛害' }, { code: 'CORN', name: '鐜夌背' }, { code: 'EGG', name: '楦¤泲' }, { code: 'MILK', name: '鐗涘ザ' }])
    return {
      player,
      playerForms,
      seedOptions,
      animalOptions,
      harvestOptions,
      formatMoney,
      formatDate,
      formatDuration,
      slotLabel,
      slotCells,
      isReady,
      purchaseShopItem,
      startFarmProduction,
      startRanchProduction,
      harvestGrowth,
      tradeMarket,
      createPrivateTrade,
      acceptPrivateTrade
    }
  },
  template: `
    <div>
      <header class="hero player-hero">
        <div>
          <span class="eyebrow">Player Farm Console</span>
          <h2>浠?1 涓囬噾甯佸紑濮嬬粡钀ワ紝鎵╁缓鏍忎綅銆佺敓浜ф敹鑾凤紝鍐嶈繘鍏ヤ氦鏄撶珯娴侀€氥€?/h2>
          <p>鐜╁绔紭鍏堣蛋鐧诲綍鎬佹帴鍙ｏ紝浜ゆ槗瀵嗙爜銆佷綑棰濅笉瓒炽€佸簱瀛樹笉瓒炽€佸ぇ瀹椾护鐗岀瓑閿欒浼氫娇鐢ㄧ粺涓€閿欒鐮佹彁绀恒€?/p>
        </div>
        <div class="hero-stats">
          <StatCard label="鍙敤閲戝竵" :value="formatMoney(player.summary?.balance)" />
          <StatCard label="鍐滅敯鏍忎綅" :value="player.summary ? \`\${player.summary.farmSlots}/16\` : '-'" />
          <StatCard label="鐗у満鏍忎綅" :value="player.summary ? \`\${player.summary.ranchSlots}/16\` : '-'" />
        </div>
      </header>

      <NoticeBlock :message="player.message" :error="player.error" :detail="player.errorDetail" />

      <section id="player-overview" class="panel">
        <div class="panel-heading">
          <div><span class="section-kicker">Overview</span><h3>璧勪骇涓庡畨鍏?/h3></div>
          <button class="button ghost" type="button" :disabled="player.loading" @click="player.loadDashboard">鍒锋柊</button>
        </div>
        <div class="asset-summary">
          <StatCard label="閿佸畾閲戝竵" :value="formatMoney(player.summary?.lockedBalance)" />
          <StatCard label="涓嬫鍐滅敯鎵╁缓" :value="formatMoney(player.summary?.nextFarmExpandCost)" />
          <StatCard label="涓嬫鐗у満鎵╁缓" :value="formatMoney(player.summary?.nextRanchExpandCost)" />
          <StatCard label="浜ゆ槗瀵嗙爜" :value="player.summary?.tradePasswordSet ? '宸茶缃? : '鏈缃?" />
        </div>
        <form class="form-grid compact-form" @submit.prevent="player.setTradePassword(playerForms.tradePassword)">
          <label>璁剧疆浜ゆ槗瀵嗙爜<input v-model="playerForms.tradePassword" type="password" maxlength="6" placeholder="6 浣嶆暟瀛? /></label>
          <button class="button" type="submit" :disabled="player.loading">淇濆瓨浜ゆ槗瀵嗙爜</button>
        </form>
      </section>

      <section id="player-shop" class="panel split">
        <div>
          <span class="section-kicker">Shop</span>
          <h3>鍟嗗簵璐拱</h3>
          <p class="muted">璐拱绉嶅瓙鎴栧姩鐗╀細鎵ｉ櫎閲戝竵骞跺鍔犲簱瀛橈紝蹇呴』杈撳叆浜ゆ槗瀵嗙爜銆?/p>
          <form class="form-grid" @submit.prevent="purchaseShopItem">
            <label>鍟嗗搧<select v-model="playerForms.shopItemCode"><option v-for="item in [...seedOptions, ...animalOptions]" :key="item.code" :value="item.code">{{ item.name }} 路 {{ item.code }}</option></select></label>
            <label>鏁伴噺<input v-model.number="playerForms.shopQuantity" type="number" min="1" /></label>
            <label>浜ゆ槗瀵嗙爜<input v-model="playerForms.shopTradePassword" type="password" placeholder="6 浣嶆暟瀛? /></label>
            <button class="button wide" type="submit" :disabled="player.loading">璐拱鍏ュ簱</button>
          </form>
        </div>
        <div class="table-wrap slim-table">
          <table>
            <thead><tr><th>搴撳瓨</th><th>鍙敤</th><th>閿佸畾</th></tr></thead>
            <tbody>
              <tr v-for="item in player.inventory" :key="item.itemId"><td>{{ item.itemName }}<small>{{ item.itemCode }}</small></td><td>{{ item.availableQuantity }}</td><td>{{ item.lockedQuantity }}</td></tr>
              <tr v-if="!player.inventory.length"><td colspan="3">鏆傛棤搴撳瓨</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <section id="player-fields" class="panel">
        <div class="panel-heading">
          <div><span class="section-kicker">Fields & Ranch</span><h3>鐢板湴涓庣墽鍦?/h3></div>
          <div class="button-row">
            <button class="button ghost" type="button" :disabled="player.loading" @click="player.expand('FARM')">鎵╁缓鍐滅敯</button>
            <button class="button ghost" type="button" :disabled="player.loading" @click="player.expand('RANCH')">鎵╁缓鐗у満</button>
          </div>
        </div>
        <div class="production-toolbar">
          <label>鍐滅敯绉嶅瓙<select v-model="playerForms.farmItemCode"><option v-for="item in seedOptions" :key="item.code" :value="item.code">{{ item.name }} 路 {{ formatDuration(item.growSeconds) }}</option></select></label>
          <label>鐗у満鍔ㄧ墿<select v-model="playerForms.ranchItemCode"><option v-for="item in animalOptions" :key="item.code" :value="item.code">{{ item.name }} 路 {{ formatDuration(item.growSeconds) }}</option></select></label>
          <div class="growth-summary">
            <span>杩涜涓壒娆?/span>
            <strong>{{ player.activeGrowths.length }}</strong>
          </div>
        </div>
        <div class="field-board">
          <article class="field-zone">
            <div class="zone-heading"><span>Farm Plots</span><strong>鍐滅敯 16 鏍?/strong></div>
            <div class="slot-grid">
              <button v-for="cell in slotCells(player.farm)" :key="'farm-' + cell.index" class="slot-cell" :class="{ locked: !cell.unlocked, busy: cell.growth, ready: isReady(cell.growth) }" type="button" :disabled="player.loading || !cell.unlocked" @click="cell.growth ? harvestGrowth(cell.growth.growthId) : startFarmProduction(cell.slot.id)">
                <span>#{{ cell.index }}</span>
                <strong>{{ !cell.unlocked ? '鏈墿寤? : cell.growth ? cell.growth.outputItemCode : '绌洪棽' }}</strong>
                <small>{{ cell.growth ? (isReady(cell.growth) ? '鍙敹鑾? : formatDate(cell.growth.readyAt)) : '鐐瑰嚮绉嶆' }}</small>
              </button>
            </div>
          </article>
          <article class="field-zone ranch-zone">
            <div class="zone-heading"><span>Ranch Slots</span><strong>鐗у満 16 鏍?/strong></div>
            <div class="slot-grid">
              <button v-for="cell in slotCells(player.ranch)" :key="'ranch-' + cell.index" class="slot-cell ranch-cell" :class="{ locked: !cell.unlocked, busy: cell.growth, ready: isReady(cell.growth) }" type="button" :disabled="player.loading || !cell.unlocked" @click="cell.growth ? harvestGrowth(cell.growth.growthId) : startRanchProduction(cell.slot.id)">
                <span>#{{ cell.index }}</span>
                <strong>{{ !cell.unlocked ? '鏈墿寤? : cell.growth ? cell.growth.outputItemCode : '绌洪棽' }}</strong>
                <small>{{ cell.growth ? (isReady(cell.growth) ? '鍙敹鑾? : formatDate(cell.growth.readyAt)) : '鐐瑰嚮鍏绘畺' }}</small>
              </button>
            </div>
          </article>
        </div>
        <div class="table-wrap growth-table">
          <table>
            <thead><tr><th>鎵规</th><th>鏍忎綅</th><th>鎶曞叆</th><th>浜у嚭</th><th>鎴愮啛鏃堕棿</th><th>鐘舵€?/th><th>鎿嶄綔</th></tr></thead>
            <tbody>
              <tr v-for="growth in player.growthInstances" :key="growth.growthId">
                <td><small>{{ growth.growthId }}</small></td>
                <td>{{ growth.slotType }} #{{ growth.slotId.slice(0, 8) }}</td>
                <td>{{ growth.inputItemCode }}</td>
                <td>{{ growth.outputItemCode }} 脳 {{ growth.outputQuantity }}</td>
                <td>{{ formatDate(growth.readyAt) }}</td>
                <td>{{ growth.status }}</td>
                <td><button class="button ghost mini" type="button" :disabled="player.loading || growth.status === 'HARVESTED'" @click="harvestGrowth(growth.growthId)">鏀惰幏</button></td>
              </tr>
              <tr v-if="!player.growthInstances.length"><td colspan="7">鏆傛棤鐢熶骇鎵规</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <section id="player-market" class="panel split">
        <div>
          <span class="section-kicker">Market</span>
          <h3>浜ゆ槗绔欎拱鍗?/h3>
          <form class="form-grid" @submit.prevent="tradeMarket('BUY')">
            <label>鍟嗗搧<select v-model="playerForms.marketItemCode"><option v-for="item in harvestOptions" :key="item.code" :value="item.code">{{ item.name }} 路 {{ item.code }}</option></select></label>
            <label>鏁伴噺<input v-model.number="playerForms.marketQuantity" type="number" min="1" /></label>
            <label>浜ゆ槗瀵嗙爜<input v-model="playerForms.marketTradePassword" type="password" placeholder="6 浣嶆暟瀛? /></label>
            <label>澶у畻浠ょ墝<input v-model.trim="playerForms.marketBulkTokenCode" placeholder="闈炲ぇ瀹楀彲鐣欑┖" /></label>
            <button class="button" type="button" :disabled="player.loading" @click="player.quote(playerForms.marketItemCode)">鏌ヨ琛屾儏</button>
            <button class="button" type="submit" :disabled="player.loading">涔板叆</button>
            <button class="button ghost" type="button" :disabled="player.loading" @click="tradeMarket('SELL')">鍗栧嚭</button>
          </form>
        </div>
        <article class="token-ticket">
          <span>褰撳墠琛屾儏</span>
          <strong>{{ player.quote?.itemCode || playerForms.marketItemCode }}</strong>
          <p>鐜颁环锛歿{ formatMoney(player.quote?.currentPrice) }} / 鍩哄噯锛歿{ formatMoney(player.quote?.basePrice) }}</p>
          <p>24h 鎴愪氦锛歿{ player.quote?.volume24h ?? '-' }} 浠?路 {{ player.quote?.tradeCount24h ?? '-' }} 绗?/p>
          <p>鍙敤浠ょ墝锛歿{ player.bulkTokens.length }} 涓?/p>
        </article>
      </section>

      <section id="player-private" class="panel">
        <div class="panel-heading">
          <div><span class="section-kicker">Private Trade</span><h3>绉佷笅浜ゆ槗鎶ヤ环</h3></div>
          <button class="button ghost" type="button" :disabled="player.loading" @click="player.loadDashboard">鍒锋柊鎶ヤ环</button>
        </div>
        <div class="private-trade-layout">
          <form class="private-form" @submit.prevent="createPrivateTrade">
            <span class="section-kicker">Create Offer</span>
            <h4>鍒涘缓鍗栧嚭鎶ヤ环</h4>
            <label>涔版柟鐢ㄦ埛 ID<input v-model.trim="playerForms.privateBuyerUserId" placeholder="瀵规柟鐜╁ UUID" /></label>
            <label>鍟嗗搧<select v-model="playerForms.privateItemCode"><option v-for="item in harvestOptions" :key="item.code" :value="item.code">{{ item.name }} 路 {{ item.code }}</option></select></label>
            <label>鏁伴噺<input v-model.number="playerForms.privateQuantity" type="number" min="1" /></label>
            <label>鎬讳环閲戝竵<input v-model.number="playerForms.privatePriceAmount" type="number" min="1" /></label>
            <label>浜ゆ槗瀵嗙爜<input v-model="playerForms.privateTradePassword" type="password" placeholder="鍗栨柟浜ゆ槗瀵嗙爜" /></label>
            <button class="button wide" type="submit" :disabled="player.loading">鍒涘缓鎶ヤ环骞跺喕缁撳簱瀛?/button>
          </form>
          <article class="private-form accept-box">
            <span class="section-kicker">Accept Offer</span>
            <h4>鎺ュ彈鎶ヤ环閰嶇疆</h4>
            <p class="muted">涔版柟鎺ュ彈 WAIT_ACCEPT 鎶ヤ环鏃朵娇鐢ㄣ€傚ぇ瀹椾氦鏄撲护鐗岄潪蹇呭～锛岃Е鍙戝ぇ瀹楅槇鍊兼椂蹇呭～銆?/p>
            <label>浜ゆ槗瀵嗙爜<input v-model="playerForms.privateAcceptPassword" type="password" placeholder="涔版柟浜ゆ槗瀵嗙爜" /></label>
            <label>澶у畻浠ょ墝<input v-model.trim="playerForms.privateBulkTokenCode" placeholder="鍙€? /></label>
          </article>
        </div>
        <div class="table-wrap private-table">
          <table>
            <thead><tr><th>鎶ヤ环</th><th>瑙掕壊</th><th>瀵规柟</th><th>鍟嗗搧</th><th>鏁伴噺</th><th>鎬讳环</th><th>绋庤垂</th><th>鐘舵€?/th><th>鎿嶄綔</th></tr></thead>
            <tbody>
              <tr v-for="offer in player.privateTrades" :key="offer.offerId">
                <td><small>{{ offer.offerId }}</small></td>
                <td>{{ offer.sellerUserId === player.currentUser?.userId ? '鍗栨柟' : '涔版柟' }}</td>
                <td>{{ offer.sellerUserId === player.currentUser?.userId ? offer.buyerUsername : offer.sellerUsername }}<small>{{ offer.sellerUserId === player.currentUser?.userId ? offer.buyerUserId : offer.sellerUserId }}</small></td>
                <td>{{ offer.itemCode }}</td>
                <td>{{ offer.quantity }}</td>
                <td>{{ formatMoney(offer.priceAmount) }}</td>
                <td>{{ formatMoney(offer.taxAmount) }}</td>
                <td>{{ offer.status }}<small>鍒版湡 {{ formatDate(offer.expiresAt) }}</small></td>
                <td>
                  <button v-if="offer.buyerUserId === player.currentUser?.userId && offer.status === 'WAIT_ACCEPT'" class="button mini" type="button" :disabled="player.loading" @click="acceptPrivateTrade(offer.offerId)">鎺ュ彈</button>
                  <button v-else-if="offer.sellerUserId === player.currentUser?.userId && offer.status === 'WAIT_ACCEPT'" class="button ghost mini" type="button" :disabled="player.loading" @click="player.cancelPrivateTrade(offer.offerId)">鍙栨秷</button>
                  <span v-else class="muted">鏃犳搷浣?/span>
                </td>
              </tr>
              <tr v-if="!player.privateTrades.length"><td colspan="9">鏆傛棤绉佷笅浜ゆ槗鎶ヤ环</td></tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  `
})

const AdminWorkspace = defineComponent({
  components: { NoticeBlock, StatCard },
  setup() {
    const chartEl = ref(null)
    let chart
    function renderChart() {
      if (!chartEl.value) return
      if (!chart) chart = echarts.init(chartEl.value)
      const rows = admin.trades.slice().reverse()
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis' },
        grid: { left: 42, right: 18, top: 24, bottom: 34 },
        xAxis: { type: 'category', data: rows.map((item) => item.itemCode), axisLine: { lineStyle: { color: '#9b8a6c' } }, axisLabel: { color: '#6d604c' } },
        yAxis: { type: 'value', axisLabel: { color: '#6d604c' }, splitLine: { lineStyle: { color: 'rgba(88, 65, 38, .12)' } } },
        series: [
          { name: '浜ゆ槗閲戦', type: 'bar', data: rows.map((item) => item.tradeAmount), itemStyle: { color: '#527449', borderRadius: [8, 8, 0, 0] } },
          { name: '绋庤垂', type: 'line', data: rows.map((item) => item.taxAmount), smooth: true, lineStyle: { color: '#b96f2b', width: 3 }, itemStyle: { color: '#b96f2b' } }
        ]
      })
    }
    watch(() => admin.trades, () => nextTick(renderChart), { deep: true })
    onMounted(() => {
      renderChart()
      window.addEventListener('resize', renderChart)
    })
    onBeforeUnmount(() => {
      window.removeEventListener('resize', renderChart)
      chart?.dispose()
    })
    return { admin, adminTaxTypes, taxForm, tokenForm, chartEl, formatRate, formatMoney, formatDate, saveTax, issueToken }
  },
  template: `
    <div>
      <header class="hero">
        <div>
          <span class="eyebrow">Bearer Token Admin Console</span>
          <h2>鐢ㄧ櫥褰曟€佺鐞嗙粡娴庡弬鏁般€佷护鐗屽彂鏀惧拰鐜╁璧勪骇鏍稿銆?/h2>
          <p>绠＄悊绔帴鍙ｉ€氳繃 Authorization: Bearer token 閴存潈锛屽悗绔細瑙ｆ瀽 token 骞跺鏍告暟鎹簱涓殑绠＄悊鍛樿鑹层€?/p>
        </div>
        <div class="hero-stats">
          <StatCard label="浜ゆ槗绔欑◣鐜? :value="formatRate(admin.marketTax?.rateBasisPoints)" />
          <StatCard label="绉佷笅绋庣巼" :value="formatRate(admin.privateTax?.rateBasisPoints)" />
          <StatCard label="鐩爣浣欓" :value="formatMoney(admin.assets?.balance)" />
        </div>
      </header>
      <NoticeBlock :message="admin.message" :error="admin.error" :detail="admin.errorDetail" />
      <section id="tax" class="panel tax-panel">
        <div class="panel-heading"><div><span class="section-kicker">Tax Config</span><h3>绋庣巼閰嶇疆</h3></div><button class="button ghost" type="button" :disabled="admin.loading" @click="admin.loadTaxConfigs">璇诲彇绋庣巼</button></div>
        <div class="tax-grid">
          <article v-for="type in adminTaxTypes" :key="type.code" class="tax-card">
            <div><span>{{ type.name }}</span><strong>{{ formatRate(taxForm[type.code].rateBasisPoints) }}</strong></div>
            <label>Basis Points<input v-model.number="taxForm[type.code].rateBasisPoints" type="number" min="0" max="5000" /></label>
            <label>璋冩暣鍘熷洜<input v-model.trim="taxForm[type.code].reason" placeholder="渚嬪锛氭椿鍔ㄦ湡闄嶄綆浜ゆ槗绔欑◣鐜? /></label>
            <button class="button" type="button" :disabled="admin.loading" @click="saveTax(type.code)">淇濆瓨 {{ type.name }}</button>
          </article>
        </div>
      </section>
      <section id="token" class="panel split">
        <div>
          <span class="section-kicker">Bulk Token</span><h3>鍙戞斁澶у畻浜ゆ槗浠ょ墝</h3>
          <p class="muted">鐢ㄤ簬楂樻暟閲忔垨楂橀噾棰濅氦鏄擄紝鎴愪氦鏃舵墸鍑忔鏁板苟绱浣跨敤閲戦銆?/p>
          <form class="form-grid" @submit.prevent="issueToken">
            <label>鍏佽鍟嗗搧绫诲瀷<select v-model="tokenForm.allowedItemType"><option value="HARVEST">HARVEST 鏀惰幏鐗?/option><option value="">涓嶉檺绫诲瀷</option></select></label>
            <label>鍗曠瑪闄愰<input v-model.number="tokenForm.singleTradeLimit" type="number" min="1" /></label>
            <label>鎬婚檺棰?input v-model.number="tokenForm.totalLimit" type="number" min="1" /></label>
            <label>鍙敤娆℃暟<input v-model.number="tokenForm.remainingUses" type="number" min="1" /></label>
            <label>鏈夋晥灏忔椂<input v-model.number="tokenForm.expireHours" type="number" min="1" /></label>
            <button class="button wide" type="submit" :disabled="admin.loading">鍙戞斁浠ょ墝</button>
          </form>
        </div>
        <article class="token-ticket"><span>鏈€杩戝彂鏀?/span><strong>{{ admin.issuedToken?.tokenCode || '灏氭湭鍙戞斁' }}</strong><p>娆℃暟锛歿{ admin.issuedToken?.remainingUses ?? '-' }} / 鐘舵€侊細{{ admin.issuedToken?.status || '-' }}</p><p>鍒版湡锛歿{ formatDate(admin.issuedToken?.expiresAt) }}</p></article>
      </section>
      <section id="assets" class="panel">
        <div class="panel-heading"><div><span class="section-kicker">Player Assets</span><h3>鐜╁璧勪骇</h3></div><button class="button ghost" type="button" :disabled="admin.loading" @click="admin.loadAssets">璇诲彇璧勪骇</button></div>
        <div class="asset-summary"><StatCard label="鐢ㄦ埛鍚? :value="admin.assets?.username || '-'" /><StatCard label="鐘舵€? :value="admin.assets?.status || '-'" /><StatCard label="鍙敤閲戝竵" :value="formatMoney(admin.assets?.balance)" /><StatCard label="閿佸畾閲戝竵" :value="formatMoney(admin.assets?.lockedBalance)" /></div>
        <div class="table-wrap"><table><thead><tr><th>鍟嗗搧</th><th>绫诲瀷</th><th>鍙敤</th><th>閿佸畾</th></tr></thead><tbody><tr v-for="item in admin.assets?.inventory || []" :key="item.itemId"><td>{{ item.itemName }} <small>{{ item.itemCode }}</small></td><td>{{ item.itemType }}</td><td>{{ item.availableQuantity }}</td><td>{{ item.lockedQuantity }}</td></tr><tr v-if="!admin.assets?.inventory?.length"><td colspan="4">鏆傛棤搴撳瓨鏁版嵁</td></tr></tbody></table></div>
      </section>
      <section id="trades" class="panel">
        <div class="panel-heading"><div><span class="section-kicker">Trade Records</span><h3>浜ゆ槗璁板綍</h3></div><button class="button ghost" type="button" :disabled="admin.loading" @click="admin.loadTrades">璇诲彇浜ゆ槗</button></div>
        <div ref="chartEl" class="chart"></div>
        <div class="table-wrap"><table><thead><tr><th>鏉ユ簮</th><th>鏂瑰悜</th><th>鍟嗗搧</th><th>鏁伴噺</th><th>閲戦</th><th>绋庤垂</th><th>鐘舵€?/th></tr></thead><tbody><tr v-for="trade in admin.trades" :key="trade.tradeId"><td>{{ trade.tradeSource }}</td><td>{{ trade.side }}</td><td>{{ trade.itemCode }}</td><td>{{ trade.quantity }}</td><td>{{ formatMoney(trade.tradeAmount) }}</td><td>{{ formatMoney(trade.taxAmount) }}</td><td>{{ trade.status }}</td></tr><tr v-if="!admin.trades.length"><td colspan="7">鏆傛棤浜ゆ槗璁板綍</td></tr></tbody></table></div>
      </section>
    </div>
  `
})

watch(() => admin.taxConfigs, syncTaxForm, { deep: true })

onMounted(() => {
  player.loadItems().catch(() => {})
  player.loadMe().catch(() => player.clearSession())
  admin.loadMe().catch(() => admin.clearSession?.() || admin.logout())
})
</script>
