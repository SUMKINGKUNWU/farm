<template>
  <div class="game-page">
    <NoticeBlock :message="player.message" :error="player.error" :detail="player.errorDetail" />

    <section class="game-shell">
      <div class="game-topbar">
        <button class="avatar-button" type="button" @click="showFloat('profile')">
          <span class="avatar-seed">穗</span>
          <span>
            <b>{{ player.currentUser?.nickname || player.currentUser?.username || player.username || '农场主' }}</b>
            <small>点击查看个人信息</small>
          </span>
        </button>
        <div class="wallet-strip">
          <span>金币</span>
          <strong>{{ formatMoney(player.summary?.balance ?? 10000) }}</strong>
        </div>
        <div class="wallet-strip muted-strip">
          <span>可收获</span>
          <strong>{{ readyCount }}</strong>
        </div>
        <button class="round-action" type="button" :disabled="player.loading" @click="player.loadDashboard">刷新</button>
      </div>

      <div class="game-layout">
        <section v-show="activeTab === 'farm'" class="farm-canvas">
          <div class="canvas-heading">
            <div>
              <span class="section-kicker">Farm & Ranch</span>
              <h2>农田与牧场</h2>
            </div>
            <div class="expand-actions">
              <button class="button ghost" type="button" :disabled="player.loading" @click="player.expand('FARM')">
                扩建农田 {{ formatMoney(player.summary?.nextFarmExpandCost) }}
              </button>
              <button class="button ghost" type="button" :disabled="player.loading" @click="player.expand('RANCH')">
                扩建牧场 {{ formatMoney(player.summary?.nextRanchExpandCost) }}
              </button>
            </div>
          </div>

          <div class="production-toolbar game-toolbar">
            <label>
              农田种子
              <select v-model="forms.farmItemCode">
                <option v-for="item in seedOptions" :key="item.code" :value="item.code">{{ item.name }} · {{ formatDuration(item.growSeconds) }}</option>
              </select>
            </label>
            <label>
              牧场动物
              <select v-model="forms.ranchItemCode">
                <option v-for="item in animalOptions" :key="item.code" :value="item.code">{{ item.name }} · {{ formatDuration(item.growSeconds) }}</option>
              </select>
            </label>
            <button class="button subtle-light" type="button" @click="showFloat('item', currentMarketItem)">物品信息</button>
          </div>

          <div class="game-field-board">
            <article class="game-zone">
              <div class="zone-heading"><span>Farm Plots</span><strong>农田 {{ player.summary?.farmSlots || 0 }}/16</strong></div>
              <div class="game-slot-grid">
                <button
                  v-for="cell in slotCells(player.farm)"
                  :key="'game-farm-' + cell.index"
                  class="game-slot field-slot"
                  :class="{ locked: !cell.unlocked, busy: cell.growth, ready: isReady(cell.growth) }"
                  type="button"
                  :disabled="player.loading || !cell.unlocked"
                  @click="cell.growth ? harvestGrowth(cell.growth.growthId) : startFarmProduction(cell.slot.id)"
                >
                  <span class="slot-top"><i>#{{ cell.index }}</i><b>{{ slotStatusText(cell) }}</b></span>
                  <strong>{{ !cell.unlocked ? '未扩建' : cell.growth ? cell.growth.outputItemCode : '空闲' }}</strong>
                  <small>{{ slotActionText(cell, 'Plant') }}</small>
                </button>
              </div>
            </article>
            <article class="game-zone ranch-game-zone">
              <div class="zone-heading"><span>Ranch Slots</span><strong>牧场 {{ player.summary?.ranchSlots || 0 }}/16</strong></div>
              <div class="game-slot-grid">
                <button
                  v-for="cell in slotCells(player.ranch)"
                  :key="'game-ranch-' + cell.index"
                  class="game-slot ranch-slot"
                  :class="{ locked: !cell.unlocked, busy: cell.growth, ready: isReady(cell.growth) }"
                  type="button"
                  :disabled="player.loading || !cell.unlocked"
                  @click="cell.growth ? harvestGrowth(cell.growth.growthId) : startRanchProduction(cell.slot.id)"
                >
                  <span class="slot-top"><i>#{{ cell.index }}</i><b>{{ slotStatusText(cell) }}</b></span>
                  <strong>{{ !cell.unlocked ? '未扩建' : cell.growth ? cell.growth.outputItemCode : '空闲' }}</strong>
                  <small>{{ slotActionText(cell, 'Raise') }}</small>
                </button>
              </div>
            </article>
          </div>
        </section>

        <section v-show="activeTab === 'shop'" class="farm-canvas">
          <div class="canvas-heading">
            <div><span class="section-kicker">Shop</span><h2>农场商店</h2></div>
            <button class="button ghost" type="button" @click="showFloat('item', allShopOptions[0])">查看商品浮层</button>
          </div>
          <div class="shop-layout">
            <form class="game-card-form" @submit.prevent="purchaseShopItem">
              <label>商品<select v-model="forms.shopItemCode"><option v-for="item in allShopOptions" :key="item.code" :value="item.code">{{ item.name }} · {{ item.code }}</option></select></label>
              <label>数量<input v-model.number="forms.shopQuantity" type="number" min="1" /></label>
              <label>交易密码<input v-model="forms.shopTradePassword" type="password" placeholder="6 位数字" /></label>
              <button class="button wide" type="submit" :disabled="player.loading">购买入库</button>
            </form>
            <div class="inventory-cloud">
              <h3>库存</h3>
              <button v-for="item in player.inventory" :key="item.itemId" type="button" class="inventory-pill" @click="showFloat('item', item)">
                <span>{{ item.itemName }}</span><strong>{{ item.availableQuantity }}</strong>
              </button>
              <p v-if="!player.inventory.length" class="muted">暂无库存，先购买种子或动物。</p>
            </div>
          </div>
        </section>

        <section v-show="activeTab === 'market'" class="farm-canvas market-canvas">
          <div class="canvas-heading">
            <div><span class="section-kicker">Market</span><h2>交易站买卖</h2></div>
            <button class="button ghost" type="button" @click="showFloat('trade')">交易确认浮层</button>
          </div>
          <div class="market-game-grid">
            <form class="game-card-form" @submit.prevent="submitMarket('BUY')">
              <label>商品<select v-model="forms.marketItemCode"><option v-for="item in harvestOptions" :key="item.code" :value="item.code">{{ item.name }} · {{ item.code }}</option></select></label>
              <label>数量<input v-model.number="forms.marketQuantity" type="number" min="1" /></label>
              <label>交易密码<input v-model="forms.marketTradePassword" type="password" placeholder="6 位数字" /></label>
              <label>大宗令牌<input v-model.trim="forms.marketBulkTokenCode" placeholder="非大宗可留空" /></label>
              <div class="fee-preview"><span>交易站税费 3%</span><strong>{{ formatMoney(marketTaxEstimate) }}</strong></div>
              <button class="button" type="submit" :disabled="player.loading">买入</button>
              <button class="button ghost" type="button" :disabled="player.loading" @click="submitMarket('SELL')">卖出</button>
            </form>
            <article class="trade-explain-card">
              <span class="section-kicker">Price Rule</span>
              <h3>{{ currentMarketItem?.name || forms.marketItemCode }}</h3>
              <p class="muted">交易站价格按最近成交量、成交价和系统保护参数浮动。大宗交易需要令牌，普通交易只需要交易密码。</p>
              <div class="fee-preview"><span>24h 成交量</span><strong>{{ player.quote?.volume24h ?? '-' }}</strong></div>
              <div class="fee-preview"><span>24h 成交笔数</span><strong>{{ player.quote?.tradeCount24h ?? '-' }}</strong></div>
            </article>
          </div>
        </section>

        <aside class="market-side">
          <div class="market-board">
            <span class="section-kicker">Market Station</span>
            <h3>{{ player.quote?.itemCode || forms.marketItemCode }}</h3>
            <strong class="market-price">{{ formatMoney(player.quote?.currentPrice) }}</strong>
            <p>最近成交量影响价格，交易站税费 3%。</p>
            <div class="fake-chart"><i></i></div>
            <button class="button wide" type="button" :disabled="player.loading" @click="player.quote(forms.marketItemCode)">刷新行情</button>
          </div>

          <div class="market-trade-card">
            <h3>税费规则</h3>
            <div class="fee-preview"><span>交易站</span><strong>3%</strong></div>
            <div class="fee-preview"><span>私下交易</span><strong>5%</strong></div>
            <div class="fee-preview"><span>令牌</span><strong>{{ player.bulkTokens.length }}</strong></div>
          </div>
        </aside>

        <section v-show="activeTab === 'private'" class="farm-canvas private-canvas">
          <div class="canvas-heading">
            <div><span class="section-kicker">Private Trade</span><h2>私下交易</h2></div>
            <button class="button ghost" type="button" @click="showFloat('trade')">交易确认浮层</button>
          </div>
          <div class="private-game-grid">
            <form class="game-card-form" @submit.prevent="createPrivateTrade">
              <label>买方用户 ID<input v-model.trim="forms.privateBuyerUserId" placeholder="对方玩家 UUID" /></label>
              <label>商品<select v-model="forms.privateItemCode"><option v-for="item in harvestOptions" :key="item.code" :value="item.code">{{ item.name }} · {{ item.code }}</option></select></label>
              <label>数量<input v-model.number="forms.privateQuantity" type="number" min="1" /></label>
              <label>总价金币<input v-model.number="forms.privatePriceAmount" type="number" min="1" /></label>
              <label>交易密码<input v-model="forms.privateTradePassword" type="password" placeholder="卖方交易密码" /></label>
              <div class="fee-preview"><span>私下交易税 5%</span><strong>{{ formatMoney(privateTaxEstimate) }}</strong></div>
              <button class="button wide" type="submit" :disabled="player.loading">创建报价</button>
            </form>
            <div class="offer-stack">
              <label>接受报价交易密码<input v-model="forms.privateAcceptPassword" type="password" placeholder="买方交易密码" /></label>
              <label>大宗令牌<input v-model.trim="forms.privateBulkTokenCode" placeholder="可选" /></label>
              <div v-for="offer in player.privateTrades" :key="offer.offerId" class="offer-card">
                <strong>{{ offer.itemCode }} × {{ offer.quantity }}</strong>
                <span>{{ formatMoney(offer.priceAmount) }} · {{ offer.status }}</span>
                <button v-if="offer.buyerUserId === player.currentUser?.userId && offer.status === 'WAIT_ACCEPT'" class="button mini" type="button" :disabled="player.loading" @click="acceptPrivateTrade(offer.offerId)">接受</button>
                <button v-else-if="offer.sellerUserId === player.currentUser?.userId && offer.status === 'WAIT_ACCEPT'" class="button ghost mini" type="button" :disabled="player.loading" @click="player.cancelPrivateTrade(offer.offerId)">取消</button>
              </div>
              <p v-if="!player.privateTrades.length" class="muted">暂无私下交易报价。</p>
            </div>
          </div>
        </section>
      </div>

      <nav class="game-bottom-tabs">
        <button v-for="item in navItems" :key="item.id" type="button" :class="{ active: activeTab === item.id }" @click="activeTab = item.id">
          <span>{{ item.icon }}</span>{{ item.label }}
        </button>
        <button class="main-tab" type="button" @click="showFloat('profile')">+</button>
      </nav>
    </section>

    <div v-if="activeFloat" class="float-layer" @click.self="closeFloat">
      <article class="glass-pop">
        <button class="close-pop" type="button" @click="closeFloat">×</button>
        <template v-if="activeFloat === 'profile'">
          <span class="section-kicker">Profile</span>
          <h3>{{ player.currentUser?.nickname || player.currentUser?.username || '农场主' }}</h3>
          <div class="float-grid">
            <StatCard label="可用金币" :value="formatMoney(player.summary?.balance)" />
            <StatCard label="锁定金币" :value="formatMoney(player.summary?.lockedBalance)" />
            <StatCard label="大宗令牌" :value="player.bulkTokens.length" />
            <StatCard label="交易密码" :value="player.summary?.tradePasswordSet ? '已设置' : '未设置'" />
          </div>
          <form class="compact-form float-password" @submit.prevent="player.setTradePassword(forms.tradePassword)">
            <label>设置交易密码<input v-model="forms.tradePassword" type="password" maxlength="6" placeholder="6 位数字" /></label>
            <button class="button" type="submit" :disabled="player.loading">保存</button>
          </form>
          <div class="float-tip">交易密码只在交易相关操作中使用，未设置时禁止购买、出售和私下交易。</div>
        </template>
        <template v-else-if="activeFloat === 'item'">
          <span class="section-kicker">Item Info</span>
          <h3>{{ selectedInfo?.name || selectedInfo?.itemName || selectedInfo?.itemCode || '物品信息' }}</h3>
          <p class="muted">点击物品、库存、地块时展示，不占用主界面空间。</p>
          <div class="float-grid">
            <StatCard label="编码" :value="selectedInfo?.code || selectedInfo?.itemCode || '-'" />
            <StatCard label="类型" :value="selectedInfo?.itemType || '-'" />
            <StatCard label="可用数量" :value="selectedInfo?.availableQuantity ?? '-'" />
            <StatCard label="成长时间" :value="formatDuration(selectedInfo?.growSeconds)" />
          </div>
          <div class="float-tip">物品卡用于承载价格、成长、库存和用途信息，后续可以接入作物贴图和市场波动提示。</div>
        </template>
        <template v-else>
          <span class="section-kicker">Trade Confirm</span>
          <h3>交易确认</h3>
          <p class="muted">交易站收取 3% 税，私下交易收取 5% 税；大宗交易需要令牌，双方私下交易都要输入交易密码。</p>
          <div class="float-grid">
            <StatCard label="交易商品" :value="forms.marketItemCode" />
            <StatCard label="交易数量" :value="forms.marketQuantity" />
            <StatCard label="交易站预估税" :value="formatMoney(marketTaxEstimate)" />
            <StatCard label="私下预估税" :value="formatMoney(privateTaxEstimate)" />
          </div>
          <div class="float-tip">交易确认层只做二次确认和风险提示，真正成交仍由后端校验余额、库存、交易密码和大宗令牌。</div>
        </template>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import NoticeBlock from './common/NoticeBlock.vue'
import StatCard from './common/StatCard.vue'
import { usePlayerStore } from '../stores/playerStore'

const player = usePlayerStore()

const activeTab = ref('farm')
const activeFloat = ref('')
const selectedInfo = ref(null)

const forms = reactive({
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
  privateBuyerUserId: '',
  privateItemCode: 'WHEAT',
  privateQuantity: 10,
  privatePriceAmount: 500,
  privateTradePassword: '',
  privateAcceptPassword: '',
  privateBulkTokenCode: ''
})

const seedOptions = computed(() => player.seeds.length ? player.seeds : [{ code: 'WHEAT_SEED', name: '小麦种子' }, { code: 'CORN_SEED', name: '玉米种子' }])
const animalOptions = computed(() => player.animals.length ? player.animals : [{ code: 'CHICKEN', name: '鸡苗' }, { code: 'COW', name: '奶牛' }])
const harvestOptions = computed(() => player.harvests.length ? player.harvests : [{ code: 'WHEAT', name: '小麦' }, { code: 'CORN', name: '玉米' }, { code: 'EGG', name: '鸡蛋' }, { code: 'MILK', name: '牛奶' }])
const readyCount = computed(() => player.activeGrowths.filter((growth) => isReady(growth)).length)
const allShopOptions = computed(() => [...seedOptions.value, ...animalOptions.value])
const currentMarketItem = computed(() => harvestOptions.value.find((item) => item.code === forms.marketItemCode) || harvestOptions.value[0])
const marketTaxEstimate = computed(() => {
  const price = Number(player.quote?.currentPrice || 0)
  const quantity = Number(forms.marketQuantity || 0)
  return Math.round(price * quantity * 0.03)
})
const privateTaxEstimate = computed(() => Math.round(Number(forms.privatePriceAmount || 0) * 0.05))
const navItems = [
  { id: 'farm', label: '农场', icon: '田' },
  { id: 'shop', label: '商店', icon: '仓' },
  { id: 'market', label: '交易站', icon: '市' },
  { id: 'private', label: '私下', icon: '约' }
]

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
  if (seconds < 3600) return `${Math.ceil(seconds / 60)} 分钟`
  return `${(seconds / 3600).toFixed(1)} 小时`
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

function slotStatusText(cell) {
  if (!cell?.unlocked) return 'Locked'
  if (!cell.growth) return 'Open'
  return isReady(cell.growth) ? 'Ready' : 'Growing'
}

function slotActionText(cell, emptyText) {
  if (!cell?.unlocked) return 'Expand first'
  if (!cell.growth) return emptyText
  return isReady(cell.growth) ? 'Click harvest' : formatDate(cell.growth.readyAt)
}

function showFloat(type, info = null) {
  selectedInfo.value = info
  activeFloat.value = type
}

function closeFloat() {
  activeFloat.value = ''
  selectedInfo.value = null
}

async function purchaseShopItem() {
  await player.purchase({
    itemCode: forms.shopItemCode,
    quantity: forms.shopQuantity,
    tradePassword: forms.shopTradePassword
  })
}

async function startFarmProduction(slotId) {
  await player.startProduction('FARM', slotId, forms.farmItemCode)
}

async function startRanchProduction(slotId) {
  await player.startProduction('RANCH', slotId, forms.ranchItemCode)
}

async function harvestGrowth(growthId) {
  await player.harvest(growthId)
}

async function submitMarket(side) {
  await player.marketTrade(side, {
    itemCode: forms.marketItemCode,
    quantity: forms.marketQuantity,
    tradePassword: forms.marketTradePassword,
    bulkTokenCode: forms.marketBulkTokenCode || null
  })
  showFloat('trade', { side })
}

async function createPrivateTrade() {
  await player.createPrivateTrade({
    buyerUserId: forms.privateBuyerUserId,
    itemCode: forms.privateItemCode,
    quantity: forms.privateQuantity,
    priceAmount: forms.privatePriceAmount,
    tradePassword: forms.privateTradePassword
  })
}

async function acceptPrivateTrade(offerId) {
  await player.acceptPrivateTrade(offerId, {
    tradePassword: forms.privateAcceptPassword,
    bulkTokenCode: forms.privateBulkTokenCode || null
  })
}
</script>
