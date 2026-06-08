<template>
  <div>
    <header class="hero">
      <div>
        <span class="eyebrow">Admin Console</span>
        <h2>管理经济参数、发放大宗令牌，并核对玩家资产与交易记录。</h2>
        <p>管理端接口通过 Bearer Token 鉴权。所有税率、令牌、资产和交易操作都应保留审计记录。</p>
      </div>
      <div class="hero-stats">
        <StatCard label="交易站税率" :value="formatRate(admin.marketTax?.rateBasisPoints)" />
        <StatCard label="私下税率" :value="formatRate(admin.privateTax?.rateBasisPoints)" />
        <StatCard label="目标余额" :value="formatMoney(admin.assets?.balance)" />
      </div>
    </header>

    <NoticeBlock :message="admin.message" :error="admin.error" :detail="admin.errorDetail" />

    <section id="tax" class="panel tax-panel">
      <div class="panel-heading">
        <div>
          <span class="section-kicker">Tax Config</span>
          <h3>税率配置</h3>
        </div>
        <button class="button ghost" type="button" :disabled="admin.loading" @click="admin.loadTaxConfigs">读取税率</button>
      </div>
      <div class="tax-grid">
        <article v-for="type in adminTaxTypes" :key="type.code" class="tax-card">
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
          <button class="button" type="button" :disabled="admin.loading" @click="saveTax(type.code)">保存 {{ type.name }}</button>
        </article>
      </div>
    </section>

    <section id="token" class="panel split">
      <div>
        <span class="section-kicker">Bulk Token</span>
        <h3>发放大宗交易令牌</h3>
        <p class="muted">用于高数量或高金额交易。成交时扣减可用次数，并累计令牌使用金额。</p>
        <form class="form-grid" @submit.prevent="issueToken">
          <label>
            允许商品类型
            <select v-model="tokenForm.allowedItemType">
              <option value="HARVEST">HARVEST 收获物</option>
              <option value="">不限类型</option>
            </select>
          </label>
          <label>单笔限额<input v-model.number="tokenForm.singleTradeLimit" type="number" min="1" /></label>
          <label>总限额<input v-model.number="tokenForm.totalLimit" type="number" min="1" /></label>
          <label>可用次数<input v-model.number="tokenForm.remainingUses" type="number" min="1" /></label>
          <label>有效小时<input v-model.number="tokenForm.expireHours" type="number" min="1" /></label>
          <button class="button wide" type="submit" :disabled="admin.loading">发放令牌</button>
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
        <button class="button ghost" type="button" :disabled="admin.loading" @click="admin.loadAssets">读取资产</button>
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
            <tr><th>商品</th><th>类型</th><th>可用</th><th>锁定</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in admin.assets?.inventory || []" :key="item.itemId">
              <td>{{ item.itemName }} <small>{{ item.itemCode }}</small></td>
              <td>{{ item.itemType }}</td>
              <td>{{ item.availableQuantity }}</td>
              <td>{{ item.lockedQuantity }}</td>
            </tr>
            <tr v-if="!admin.assets?.inventory?.length"><td colspan="4">暂无库存数据</td></tr>
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
        <button class="button ghost" type="button" :disabled="admin.loading" @click="admin.loadTrades">读取交易</button>
      </div>
      <div ref="chartEl" class="chart"></div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>来源</th><th>方向</th><th>商品</th><th>数量</th><th>金额</th><th>税费</th><th>状态</th></tr>
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
            <tr v-if="!admin.trades.length"><td colspan="7">暂无交易记录</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { useAdminStore } from '../stores/adminStore'
import NoticeBlock from './common/NoticeBlock.vue'
import StatCard from './common/StatCard.vue'

defineProps({
  adminTaxTypes: { type: Array, required: true },
  taxForm: { type: Object, required: true },
  tokenForm: { type: Object, required: true },
  formatRate: { type: Function, required: true },
  formatMoney: { type: Function, required: true },
  formatDate: { type: Function, required: true },
  saveTax: { type: Function, required: true },
  issueToken: { type: Function, required: true }
})

const admin = useAdminStore()
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
</script>
