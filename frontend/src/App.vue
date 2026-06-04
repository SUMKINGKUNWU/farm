<template>
  <main class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">穗</div>
        <div>
          <p>Farm Exchange</p>
          <h1>运营管理台</h1>
        </div>
      </div>

      <div class="identity-card">
        <template v-if="!admin.isLoggedIn">
          <label>
            管理员用户名
            <input v-model.trim="admin.username" placeholder="请输入管理员用户名" />
          </label>
          <label>
            登录密码
            <input v-model="admin.password" type="password" placeholder="请输入登录密码" @keydown.enter="admin.login" />
          </label>
          <button class="button ghost" type="button" @click="admin.login">登录管理台</button>
        </template>

        <template v-else>
          <div class="login-badge">
            <span>当前管理员</span>
            <strong>{{ admin.currentUser?.username || admin.username || '已登录' }}</strong>
          </div>
          <label>
            目标玩家用户 ID
            <input v-model.trim="admin.targetUserId" placeholder="要查询或发令牌的玩家 UUID" />
          </label>
          <button class="button ghost" type="button" @click="loadEverything">刷新控制台</button>
          <button class="button subtle" type="button" @click="admin.logout">退出登录</button>
        </template>
      </div>

      <nav class="nav">
        <a href="#tax">税率</a>
        <a href="#token">令牌</a>
        <a href="#assets">资产</a>
        <a href="#trades">交易</a>
      </nav>
    </aside>

    <section class="workspace">
      <header class="hero">
        <div>
          <span class="eyebrow">Bearer Token Admin Console</span>
          <h2>用登录态管理经济参数、令牌发放和玩家资产核对。</h2>
          <p>管理端接口现在通过 <b>Authorization: Bearer token</b> 鉴权，后端会解析 token 并复核数据库中的管理员角色。</p>
        </div>
        <div class="hero-stats">
          <StatCard label="交易站税率" :value="formatRate(admin.marketTax?.rateBasisPoints)" />
          <StatCard label="私下税率" :value="formatRate(admin.privateTax?.rateBasisPoints)" />
          <StatCard label="目标余额" :value="formatMoney(admin.assets?.balance)" />
        </div>
      </header>

      <div v-if="admin.message" class="notice success">{{ admin.message }}</div>
      <div v-if="admin.error" class="notice error">{{ admin.error }}</div>

      <section id="tax" class="panel tax-panel">
        <div class="panel-heading">
          <div>
            <span class="section-kicker">Tax Config</span>
            <h3>税率配置</h3>
          </div>
          <button class="button ghost" type="button" @click="admin.loadTaxConfigs">读取税率</button>
        </div>

        <div class="tax-grid">
          <article v-for="type in taxTypes" :key="type.code" class="tax-card">
            <div>
              <span>{{ type.name }}</span>
              <strong>{{ formatRate(taxForm[type.code].rateBasisPoints) }}</strong>
            </div>
            <label>
              Basis Points
              <input v-model.number="taxForm[type.code].rateBasisPoints" type="number" min="0" max="5000" />
            </label>
            <label>
              调整原因
              <input v-model.trim="taxForm[type.code].reason" placeholder="例如：活动期降低交易站税率" />
            </label>
            <button class="button" type="button" @click="saveTax(type.code)">保存 {{ type.name }}</button>
          </article>
        </div>
      </section>

      <section id="token" class="panel split">
        <div>
          <span class="section-kicker">Bulk Token</span>
          <h3>发放大宗交易令牌</h3>
          <p class="muted">用于高数量或高金额交易，成交时扣减次数并累计使用金额。MVP 阶段先由管理员直接发放。</p>
          <form class="form-grid" @submit.prevent="issueToken">
            <label>
              允许商品类型
              <select v-model="tokenForm.allowedItemType">
                <option value="HARVEST">HARVEST 收获物</option>
                <option value="">不限类型</option>
              </select>
            </label>
            <label>
              单笔限额
              <input v-model.number="tokenForm.singleTradeLimit" type="number" min="1" />
            </label>
            <label>
              总限额
              <input v-model.number="tokenForm.totalLimit" type="number" min="1" />
            </label>
            <label>
              可用次数
              <input v-model.number="tokenForm.remainingUses" type="number" min="1" />
            </label>
            <label>
              有效小时
              <input v-model.number="tokenForm.expireHours" type="number" min="1" />
            </label>
            <button class="button wide" type="submit">发放令牌</button>
          </form>
        </div>

        <article class="token-ticket">
          <span>最近发放</span>
          <strong>{{ admin.issuedToken?.tokenCode || '尚未发放' }}</strong>
          <p>次数：{{ admin.issuedToken?.remainingUses ?? '-' }} / 状态：{{ admin.issuedToken?.status || '-' }}</p>
          <p>到期：{{ formatDate(admin.issuedToken?.expiresAt) }}</p>
        </article>
      </section>

      <section id="assets" class="panel">
        <div class="panel-heading">
          <div>
            <span class="section-kicker">Player Assets</span>
            <h3>玩家资产</h3>
          </div>
          <button class="button ghost" type="button" @click="admin.loadAssets">读取资产</button>
        </div>

        <div class="asset-summary">
          <StatCard label="用户名" :value="admin.assets?.username || '-'" />
          <StatCard label="状态" :value="admin.assets?.status || '-'" />
          <StatCard label="可用金币" :value="formatMoney(admin.assets?.balance)" />
          <StatCard label="锁定金币" :value="formatMoney(admin.assets?.lockedBalance)" />
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>商品</th>
                <th>类型</th>
                <th>可用</th>
                <th>锁定</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in admin.assets?.inventory || []" :key="item.itemId">
                <td>{{ item.itemName }} <small>{{ item.itemCode }}</small></td>
                <td>{{ item.itemType }}</td>
                <td>{{ item.availableQuantity }}</td>
                <td>{{ item.lockedQuantity }}</td>
              </tr>
              <tr v-if="!admin.assets?.inventory?.length">
                <td colspan="4">暂无库存数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section id="trades" class="panel">
        <div class="panel-heading">
          <div>
            <span class="section-kicker">Trade Records</span>
            <h3>交易记录</h3>
          </div>
          <button class="button ghost" type="button" @click="admin.loadTrades">读取交易</button>
        </div>

        <div ref="chartEl" class="chart"></div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>来源</th>
                <th>方向</th>
                <th>商品</th>
                <th>数量</th>
                <th>金额</th>
                <th>税费</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="trade in admin.trades" :key="trade.tradeId">
                <td>{{ trade.tradeSource }}</td>
                <td>{{ trade.side }}</td>
                <td>{{ trade.itemCode }}</td>
                <td>{{ trade.quantity }}</td>
                <td>{{ formatMoney(trade.tradeAmount) }}</td>
                <td>{{ formatMoney(trade.taxAmount) }}</td>
                <td>{{ trade.status }}</td>
              </tr>
              <tr v-if="!admin.trades.length">
                <td colspan="7">暂无交易记录</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { useAdminStore } from './stores/adminStore'

const admin = useAdminStore()
const chartEl = ref(null)
let chart

const taxTypes = [
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

async function saveTax(type) {
  await admin.updateTaxConfig(type, taxForm[type])
}

async function issueToken() {
  await admin.issueBulkToken(tokenForm)
}

async function loadEverything() {
  await admin.loadTaxConfigs()
  if (admin.targetUserId) {
    await admin.loadPlayerConsole()
  }
}

function syncTaxForm(configs) {
  configs.forEach((config) => {
    if (taxForm[config.tradeType]) {
      taxForm[config.tradeType].rateBasisPoints = config.rateBasisPoints
      taxForm[config.tradeType].reason = config.updatedReason || ''
    }
  })
}

function renderChart() {
  if (!chartEl.value) return
  if (!chart) chart = echarts.init(chartEl.value)
  const rows = admin.trades.slice().reverse()
  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    grid: { left: 42, right: 18, top: 24, bottom: 34 },
    xAxis: {
      type: 'category',
      data: rows.map((item) => item.itemCode),
      axisLine: { lineStyle: { color: '#9b8a6c' } },
      axisLabel: { color: '#6d604c' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#6d604c' },
      splitLine: { lineStyle: { color: 'rgba(88, 65, 38, .12)' } }
    },
    series: [
      {
        name: '交易金额',
        type: 'bar',
        data: rows.map((item) => item.tradeAmount),
        itemStyle: {
          color: '#527449',
          borderRadius: [8, 8, 0, 0]
        }
      },
      {
        name: '税费',
        type: 'line',
        data: rows.map((item) => item.taxAmount),
        smooth: true,
        lineStyle: { color: '#b96f2b', width: 3 },
        itemStyle: { color: '#b96f2b' }
      }
    ]
  })
}

watch(() => admin.taxConfigs, syncTaxForm, { deep: true })
watch(() => admin.trades, () => nextTick(renderChart), { deep: true })

onMounted(() => {
  admin.loadMe().catch(() => admin.logout())
  renderChart()
  window.addEventListener('resize', renderChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', renderChart)
  chart?.dispose()
})
</script>

<script>
export default {
  components: {
    StatCard: {
      props: {
        label: { type: String, required: true },
        value: { type: [String, Number], required: true }
      },
      template: '<article class="stat-card"><span>{{ label }}</span><strong>{{ value }}</strong></article>'
    }
  }
}
</script>
