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
      </nav>
      <nav class="nav" v-else>
        <a href="#tax">税率</a>
        <a href="#token">令牌</a>
        <a href="#assets">资产</a>
        <a href="#trades">交易</a>
      </nav>
    </aside>

    <section class="workspace">
      <PlayerWorkspace v-if="activeMode === 'player'" />
      <AdminWorkspace v-else />
    </section>
  </main>
</template>

<script setup>
import { computed, defineComponent, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { useAdminStore } from './stores/adminStore'
import { usePlayerStore } from './stores/playerStore'

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
  harvestGrowthId: ''
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
  if (seconds < 3600) return `${Math.ceil(seconds / 60)} 分钟`
  return `${(seconds / 3600).toFixed(1)} 小时`
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

const NoticeBlock = defineComponent({
  props: {
    message: { type: String, default: '' },
    error: { type: String, default: '' },
    detail: { type: Object, default: null }
  },
  template: `
    <div>
      <div v-if="message" class="notice success">{{ message }}</div>
      <div v-if="error" class="notice error">
        <div class="notice-title">
          <span>{{ error }}</span>
          <code v-if="detail?.code">{{ detail.code }}</code>
        </div>
        <p v-if="detail?.action">{{ detail.action }}</p>
        <p v-if="detail?.status || detail?.path" class="notice-meta">HTTP {{ detail.status || '-' }} · {{ detail.path || '本地校验' }}</p>
        <ul v-if="detail?.fieldErrors?.length" class="field-errors">
          <li v-for="fieldError in detail.fieldErrors" :key="\`\${fieldError.field}-\${fieldError.message}\`">
            {{ fieldError.field }}：{{ fieldError.message }}
          </li>
        </ul>
      </div>
    </div>
  `
})

const StatCard = defineComponent({
  props: {
    label: { type: String, required: true },
    value: { type: [String, Number], required: true }
  },
  template: '<article class="stat-card"><span>{{ label }}</span><strong>{{ value }}</strong></article>'
})

const PlayerIdentity = defineComponent({
  components: { NoticeBlock },
  setup() {
    return { player }
  },
  template: `
    <div class="identity-card">
      <template v-if="!player.isLoggedIn">
        <label>玩家用户名<input v-model.trim="player.username" placeholder="例如 farmer_001" /></label>
        <label>玩家昵称<input v-model.trim="player.nickname" placeholder="注册时填写昵称" /></label>
        <label>登录密码<input v-model="player.password" type="password" placeholder="至少 6 位" @keydown.enter="player.login" /></label>
        <div class="button-row">
          <button class="button ghost" type="button" :disabled="player.loading" @click="player.login">登录</button>
          <button class="button subtle" type="button" :disabled="player.loading" @click="player.register">注册并登录</button>
        </div>
      </template>
      <template v-else>
        <div class="login-badge">
          <span>当前玩家</span>
          <strong>{{ player.currentUser?.username || player.username || '已登录' }}</strong>
        </div>
        <button class="button ghost" type="button" :disabled="player.loading" @click="player.loadDashboard">刷新农场</button>
        <button class="button subtle" type="button" @click="player.logout">退出登录</button>
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
        <label>管理员用户名<input v-model.trim="admin.username" placeholder="请输入管理员用户名" /></label>
        <label>登录密码<input v-model="admin.password" type="password" placeholder="请输入登录密码" @keydown.enter="admin.login" /></label>
        <button class="button ghost" type="button" :disabled="admin.loading" @click="admin.login">登录管理台</button>
      </template>
      <template v-else>
        <div class="login-badge">
          <span>当前管理员</span>
          <strong>{{ admin.currentUser?.username || admin.username || '已登录' }}</strong>
        </div>
        <label>目标玩家用户 ID<input v-model.trim="admin.targetUserId" placeholder="要查询或发令牌的玩家 UUID" /></label>
        <button class="button ghost" type="button" :disabled="admin.loading" @click="loadAdminEverything">刷新控制台</button>
        <button class="button subtle" type="button" @click="admin.logout">退出登录</button>
      </template>
    </div>
  `
})

const PlayerWorkspace = defineComponent({
  components: { NoticeBlock, StatCard },
  setup() {
    const seedOptions = computed(() => player.seeds.length ? player.seeds : [{ code: 'WHEAT_SEED', name: '小麦种子' }, { code: 'CORN_SEED', name: '玉米种子' }])
    const animalOptions = computed(() => player.animals.length ? player.animals : [{ code: 'CHICKEN', name: '鸡苗' }, { code: 'COW', name: '奶牛' }])
    const harvestOptions = computed(() => player.harvests.length ? player.harvests : [{ code: 'WHEAT', name: '小麦' }, { code: 'CORN', name: '玉米' }, { code: 'EGG', name: '鸡蛋' }, { code: 'MILK', name: '牛奶' }])
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
      tradeMarket
    }
  },
  template: `
    <div>
      <header class="hero player-hero">
        <div>
          <span class="eyebrow">Player Farm Console</span>
          <h2>从 1 万金币开始经营，扩建栏位、生产收获，再进入交易站流通。</h2>
          <p>玩家端优先走登录态接口，交易密码、余额不足、库存不足、大宗令牌等错误会使用统一错误码提示。</p>
        </div>
        <div class="hero-stats">
          <StatCard label="可用金币" :value="formatMoney(player.summary?.balance)" />
          <StatCard label="农田栏位" :value="player.summary ? \`\${player.summary.farmSlots}/16\` : '-'" />
          <StatCard label="牧场栏位" :value="player.summary ? \`\${player.summary.ranchSlots}/16\` : '-'" />
        </div>
      </header>

      <NoticeBlock :message="player.message" :error="player.error" :detail="player.errorDetail" />

      <section id="player-overview" class="panel">
        <div class="panel-heading">
          <div><span class="section-kicker">Overview</span><h3>资产与安全</h3></div>
          <button class="button ghost" type="button" :disabled="player.loading" @click="player.loadDashboard">刷新</button>
        </div>
        <div class="asset-summary">
          <StatCard label="锁定金币" :value="formatMoney(player.summary?.lockedBalance)" />
          <StatCard label="下次农田扩建" :value="formatMoney(player.summary?.nextFarmExpandCost)" />
          <StatCard label="下次牧场扩建" :value="formatMoney(player.summary?.nextRanchExpandCost)" />
          <StatCard label="交易密码" :value="player.summary?.tradePasswordSet ? '已设置' : '未设置'" />
        </div>
        <form class="form-grid compact-form" @submit.prevent="player.setTradePassword(playerForms.tradePassword)">
          <label>设置交易密码<input v-model="playerForms.tradePassword" type="password" maxlength="6" placeholder="6 位数字" /></label>
          <button class="button" type="submit" :disabled="player.loading">保存交易密码</button>
        </form>
      </section>

      <section id="player-shop" class="panel split">
        <div>
          <span class="section-kicker">Shop</span>
          <h3>商店购买</h3>
          <p class="muted">购买种子或动物会扣除金币并增加库存，必须输入交易密码。</p>
          <form class="form-grid" @submit.prevent="purchaseShopItem">
            <label>商品<select v-model="playerForms.shopItemCode"><option v-for="item in [...seedOptions, ...animalOptions]" :key="item.code" :value="item.code">{{ item.name }} · {{ item.code }}</option></select></label>
            <label>数量<input v-model.number="playerForms.shopQuantity" type="number" min="1" /></label>
            <label>交易密码<input v-model="playerForms.shopTradePassword" type="password" placeholder="6 位数字" /></label>
            <button class="button wide" type="submit" :disabled="player.loading">购买入库</button>
          </form>
        </div>
        <div class="table-wrap slim-table">
          <table>
            <thead><tr><th>库存</th><th>可用</th><th>锁定</th></tr></thead>
            <tbody>
              <tr v-for="item in player.inventory" :key="item.itemId"><td>{{ item.itemName }}<small>{{ item.itemCode }}</small></td><td>{{ item.availableQuantity }}</td><td>{{ item.lockedQuantity }}</td></tr>
              <tr v-if="!player.inventory.length"><td colspan="3">暂无库存</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <section id="player-fields" class="panel">
        <div class="panel-heading">
          <div><span class="section-kicker">Fields & Ranch</span><h3>田地与牧场</h3></div>
          <div class="button-row">
            <button class="button ghost" type="button" :disabled="player.loading" @click="player.expand('FARM')">扩建农田</button>
            <button class="button ghost" type="button" :disabled="player.loading" @click="player.expand('RANCH')">扩建牧场</button>
          </div>
        </div>
        <div class="production-toolbar">
          <label>农田种子<select v-model="playerForms.farmItemCode"><option v-for="item in seedOptions" :key="item.code" :value="item.code">{{ item.name }} · {{ formatDuration(item.growSeconds) }}</option></select></label>
          <label>牧场动物<select v-model="playerForms.ranchItemCode"><option v-for="item in animalOptions" :key="item.code" :value="item.code">{{ item.name }} · {{ formatDuration(item.growSeconds) }}</option></select></label>
          <div class="growth-summary">
            <span>进行中批次</span>
            <strong>{{ player.activeGrowths.length }}</strong>
          </div>
        </div>
        <div class="field-board">
          <article class="field-zone">
            <div class="zone-heading"><span>Farm Plots</span><strong>农田 16 格</strong></div>
            <div class="slot-grid">
              <button v-for="cell in slotCells(player.farm)" :key="'farm-' + cell.index" class="slot-cell" :class="{ locked: !cell.unlocked, busy: cell.growth, ready: isReady(cell.growth) }" type="button" :disabled="player.loading || !cell.unlocked" @click="cell.growth ? harvestGrowth(cell.growth.growthId) : startFarmProduction(cell.slot.id)">
                <span>#{{ cell.index }}</span>
                <strong>{{ !cell.unlocked ? '未扩建' : cell.growth ? cell.growth.outputItemCode : '空闲' }}</strong>
                <small>{{ cell.growth ? (isReady(cell.growth) ? '可收获' : formatDate(cell.growth.readyAt)) : '点击种植' }}</small>
              </button>
            </div>
          </article>
          <article class="field-zone ranch-zone">
            <div class="zone-heading"><span>Ranch Slots</span><strong>牧场 16 格</strong></div>
            <div class="slot-grid">
              <button v-for="cell in slotCells(player.ranch)" :key="'ranch-' + cell.index" class="slot-cell ranch-cell" :class="{ locked: !cell.unlocked, busy: cell.growth, ready: isReady(cell.growth) }" type="button" :disabled="player.loading || !cell.unlocked" @click="cell.growth ? harvestGrowth(cell.growth.growthId) : startRanchProduction(cell.slot.id)">
                <span>#{{ cell.index }}</span>
                <strong>{{ !cell.unlocked ? '未扩建' : cell.growth ? cell.growth.outputItemCode : '空闲' }}</strong>
                <small>{{ cell.growth ? (isReady(cell.growth) ? '可收获' : formatDate(cell.growth.readyAt)) : '点击养殖' }}</small>
              </button>
            </div>
          </article>
        </div>
        <div class="table-wrap growth-table">
          <table>
            <thead><tr><th>批次</th><th>栏位</th><th>投入</th><th>产出</th><th>成熟时间</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="growth in player.growthInstances" :key="growth.growthId">
                <td><small>{{ growth.growthId }}</small></td>
                <td>{{ growth.slotType }} #{{ growth.slotId.slice(0, 8) }}</td>
                <td>{{ growth.inputItemCode }}</td>
                <td>{{ growth.outputItemCode }} × {{ growth.outputQuantity }}</td>
                <td>{{ formatDate(growth.readyAt) }}</td>
                <td>{{ growth.status }}</td>
                <td><button class="button ghost mini" type="button" :disabled="player.loading || growth.status === 'HARVESTED'" @click="harvestGrowth(growth.growthId)">收获</button></td>
              </tr>
              <tr v-if="!player.growthInstances.length"><td colspan="7">暂无生产批次</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <section id="player-market" class="panel split">
        <div>
          <span class="section-kicker">Market</span>
          <h3>交易站买卖</h3>
          <form class="form-grid" @submit.prevent="tradeMarket('BUY')">
            <label>商品<select v-model="playerForms.marketItemCode"><option v-for="item in harvestOptions" :key="item.code" :value="item.code">{{ item.name }} · {{ item.code }}</option></select></label>
            <label>数量<input v-model.number="playerForms.marketQuantity" type="number" min="1" /></label>
            <label>交易密码<input v-model="playerForms.marketTradePassword" type="password" placeholder="6 位数字" /></label>
            <label>大宗令牌<input v-model.trim="playerForms.marketBulkTokenCode" placeholder="非大宗可留空" /></label>
            <button class="button" type="button" :disabled="player.loading" @click="player.quote(playerForms.marketItemCode)">查询行情</button>
            <button class="button" type="submit" :disabled="player.loading">买入</button>
            <button class="button ghost" type="button" :disabled="player.loading" @click="tradeMarket('SELL')">卖出</button>
          </form>
        </div>
        <article class="token-ticket">
          <span>当前行情</span>
          <strong>{{ player.quote?.itemCode || playerForms.marketItemCode }}</strong>
          <p>现价：{{ formatMoney(player.quote?.currentPrice) }} / 基准：{{ formatMoney(player.quote?.basePrice) }}</p>
          <p>24h 成交：{{ player.quote?.volume24h ?? '-' }} 件 · {{ player.quote?.tradeCount24h ?? '-' }} 笔</p>
          <p>可用令牌：{{ player.bulkTokens.length }} 个</p>
        </article>
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
          { name: '交易金额', type: 'bar', data: rows.map((item) => item.tradeAmount), itemStyle: { color: '#527449', borderRadius: [8, 8, 0, 0] } },
          { name: '税费', type: 'line', data: rows.map((item) => item.taxAmount), smooth: true, lineStyle: { color: '#b96f2b', width: 3 }, itemStyle: { color: '#b96f2b' } }
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
          <h2>用登录态管理经济参数、令牌发放和玩家资产核对。</h2>
          <p>管理端接口通过 Authorization: Bearer token 鉴权，后端会解析 token 并复核数据库中的管理员角色。</p>
        </div>
        <div class="hero-stats">
          <StatCard label="交易站税率" :value="formatRate(admin.marketTax?.rateBasisPoints)" />
          <StatCard label="私下税率" :value="formatRate(admin.privateTax?.rateBasisPoints)" />
          <StatCard label="目标余额" :value="formatMoney(admin.assets?.balance)" />
        </div>
      </header>
      <NoticeBlock :message="admin.message" :error="admin.error" :detail="admin.errorDetail" />
      <section id="tax" class="panel tax-panel">
        <div class="panel-heading"><div><span class="section-kicker">Tax Config</span><h3>税率配置</h3></div><button class="button ghost" type="button" :disabled="admin.loading" @click="admin.loadTaxConfigs">读取税率</button></div>
        <div class="tax-grid">
          <article v-for="type in adminTaxTypes" :key="type.code" class="tax-card">
            <div><span>{{ type.name }}</span><strong>{{ formatRate(taxForm[type.code].rateBasisPoints) }}</strong></div>
            <label>Basis Points<input v-model.number="taxForm[type.code].rateBasisPoints" type="number" min="0" max="5000" /></label>
            <label>调整原因<input v-model.trim="taxForm[type.code].reason" placeholder="例如：活动期降低交易站税率" /></label>
            <button class="button" type="button" :disabled="admin.loading" @click="saveTax(type.code)">保存 {{ type.name }}</button>
          </article>
        </div>
      </section>
      <section id="token" class="panel split">
        <div>
          <span class="section-kicker">Bulk Token</span><h3>发放大宗交易令牌</h3>
          <p class="muted">用于高数量或高金额交易，成交时扣减次数并累计使用金额。</p>
          <form class="form-grid" @submit.prevent="issueToken">
            <label>允许商品类型<select v-model="tokenForm.allowedItemType"><option value="HARVEST">HARVEST 收获物</option><option value="">不限类型</option></select></label>
            <label>单笔限额<input v-model.number="tokenForm.singleTradeLimit" type="number" min="1" /></label>
            <label>总限额<input v-model.number="tokenForm.totalLimit" type="number" min="1" /></label>
            <label>可用次数<input v-model.number="tokenForm.remainingUses" type="number" min="1" /></label>
            <label>有效小时<input v-model.number="tokenForm.expireHours" type="number" min="1" /></label>
            <button class="button wide" type="submit" :disabled="admin.loading">发放令牌</button>
          </form>
        </div>
        <article class="token-ticket"><span>最近发放</span><strong>{{ admin.issuedToken?.tokenCode || '尚未发放' }}</strong><p>次数：{{ admin.issuedToken?.remainingUses ?? '-' }} / 状态：{{ admin.issuedToken?.status || '-' }}</p><p>到期：{{ formatDate(admin.issuedToken?.expiresAt) }}</p></article>
      </section>
      <section id="assets" class="panel">
        <div class="panel-heading"><div><span class="section-kicker">Player Assets</span><h3>玩家资产</h3></div><button class="button ghost" type="button" :disabled="admin.loading" @click="admin.loadAssets">读取资产</button></div>
        <div class="asset-summary"><StatCard label="用户名" :value="admin.assets?.username || '-'" /><StatCard label="状态" :value="admin.assets?.status || '-'" /><StatCard label="可用金币" :value="formatMoney(admin.assets?.balance)" /><StatCard label="锁定金币" :value="formatMoney(admin.assets?.lockedBalance)" /></div>
        <div class="table-wrap"><table><thead><tr><th>商品</th><th>类型</th><th>可用</th><th>锁定</th></tr></thead><tbody><tr v-for="item in admin.assets?.inventory || []" :key="item.itemId"><td>{{ item.itemName }} <small>{{ item.itemCode }}</small></td><td>{{ item.itemType }}</td><td>{{ item.availableQuantity }}</td><td>{{ item.lockedQuantity }}</td></tr><tr v-if="!admin.assets?.inventory?.length"><td colspan="4">暂无库存数据</td></tr></tbody></table></div>
      </section>
      <section id="trades" class="panel">
        <div class="panel-heading"><div><span class="section-kicker">Trade Records</span><h3>交易记录</h3></div><button class="button ghost" type="button" :disabled="admin.loading" @click="admin.loadTrades">读取交易</button></div>
        <div ref="chartEl" class="chart"></div>
        <div class="table-wrap"><table><thead><tr><th>来源</th><th>方向</th><th>商品</th><th>数量</th><th>金额</th><th>税费</th><th>状态</th></tr></thead><tbody><tr v-for="trade in admin.trades" :key="trade.tradeId"><td>{{ trade.tradeSource }}</td><td>{{ trade.side }}</td><td>{{ trade.itemCode }}</td><td>{{ trade.quantity }}</td><td>{{ formatMoney(trade.tradeAmount) }}</td><td>{{ formatMoney(trade.taxAmount) }}</td><td>{{ trade.status }}</td></tr><tr v-if="!admin.trades.length"><td colspan="7">暂无交易记录</td></tr></tbody></table></div>
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
