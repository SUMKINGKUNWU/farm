<template>
  <section id="trades" class="panel">
    <div class="panel-heading">
      <div>
        <span class="section-kicker">Trade Records</span>
        <h3>交易记录</h3>
      </div>
      <button class="button ghost" type="button" :disabled="loading" @click="refreshCurrentPage">
        刷新交易
      </button>
    </div>

    <div class="toolbar-row">
      <label>
        来源
        <select v-model="draftFilters.source">
          <option value="ALL">全部</option>
          <option value="MARKET">交易站</option>
          <option value="PRIVATE">私下交易</option>
        </select>
      </label>
      <label>
        状态
        <select v-model="draftFilters.status">
          <option value="ALL">全部</option>
          <option value="COMPLETED">已完成</option>
          <option value="WAIT_ACCEPT">待接受</option>
          <option value="SETTLING">结算中</option>
          <option value="CANCELLED">已取消</option>
          <option value="EXPIRED">已过期</option>
          <option value="FAILED">失败</option>
        </select>
      </label>
      <label>
        原因建议
        <select :value="selectedReasonOption" @change="applyReasonOption($event.target.value)">
          <option value="ALL">全部</option>
          <option v-for="option in reasonOptions" :key="option" :value="option">
            {{ adminTradeReasonLabel(option) }}
          </option>
        </select>
      </label>
      <label>
        每页
        <select v-model.number="draftFilters.pageSize">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
      </label>
      <button class="button subtle" type="button" :disabled="loading" @click="applyFilters">
        应用筛选
      </button>
    </div>

    <div ref="chartEl" class="chart"></div>

    <div class="table-meta">
      <span>共 {{ tradeResult.total }} 条</span>
      <span>第 {{ tradeResult.page }} / {{ totalPages }} 页</span>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>时间</th>
            <th>来源</th>
            <th>方向</th>
            <th>商品</th>
            <th>数量</th>
            <th>金额</th>
            <th>税费</th>
            <th>原因</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="trade in tradeResult.records" :key="trade.tradeId">
            <td>{{ formatDate(trade.createdAt) }}</td>
            <td>{{ adminTradeSourceLabel(trade.tradeSource) }}</td>
            <td>{{ adminTradeSideLabel(trade.side) }}</td>
            <td>{{ trade.itemCode }}</td>
            <td>{{ trade.quantity }}</td>
            <td>{{ formatMoney(trade.tradeAmount) }}</td>
            <td>{{ formatMoney(trade.taxAmount) }}</td>
            <td>{{ adminTradeReasonLabel(trade.tradeReason) }}</td>
            <td>{{ adminTradeStatusLabel(trade.status) }}</td>
          </tr>
          <tr v-if="!tradeResult.records.length">
            <td colspan="9">暂无交易记录</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pager-row">
      <button class="button ghost" type="button" :disabled="loading || tradeResult.page <= 1" @click="goPrev">
        上一页
      </button>
      <button class="button ghost" type="button" :disabled="loading || !tradeResult.hasNext" @click="goNext">
        下一页
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  adminTradeReasonLabel,
  adminTradeSideLabel,
  adminTradeSourceLabel,
  adminTradeStatusLabel
} from '../../utils/adminLabels'

const props = defineProps({
  tradeResult: { type: Object, required: true },
  tradeFilters: { type: Object, required: true },
  tradeFilterOptions: {
    type: Object,
    default: () => ({
      reasons: []
    })
  },
  loading: { type: Boolean, required: true },
  formatMoney: { type: Function, required: true },
  formatDate: { type: Function, required: true }
})

const emit = defineEmits(['load-trades'])
const chartEl = ref(null)
const draftFilters = reactive({
  source: 'ALL',
  status: 'ALL',
  reason: 'ALL',
  pageSize: 10
})
let chart
let echartsModulePromise

const totalPages = computed(() => {
  const size = Math.max(Number(props.tradeResult.pageSize || props.tradeFilters.pageSize || 10), 1)
  return Math.max(1, Math.ceil(Number(props.tradeResult.total || 0) / size))
})

const reasonOptions = computed(() => props.tradeFilterOptions.reasons || [])
const selectedReasonOption = computed(() => {
  return reasonOptions.value.includes(draftFilters.reason) ? draftFilters.reason : 'ALL'
})

watch(
  () => props.tradeFilters,
  (filters) => {
    draftFilters.source = filters.source
    draftFilters.status = filters.status
    draftFilters.reason = filters.reason
    draftFilters.pageSize = filters.pageSize
  },
  { deep: true, immediate: true }
)

async function loadEcharts() {
  if (!echartsModulePromise) {
    echartsModulePromise = import('echarts')
  }
  return echartsModulePromise
}

async function renderChart() {
  if (!chartEl.value) return
  const echarts = await loadEcharts()
  if (!chart) chart = echarts.init(chartEl.value)
  const rows = props.tradeResult.records.slice().reverse()
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
        itemStyle: { color: '#527449', borderRadius: [8, 8, 0, 0] }
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

function applyReasonOption(value) {
  draftFilters.reason = value || 'ALL'
}

function applyFilters() {
  emit('load-trades', {
    source: draftFilters.source,
    status: draftFilters.status,
    reason: draftFilters.reason,
    pageSize: draftFilters.pageSize,
    page: 1
  })
}

function refreshCurrentPage() {
  emit('load-trades', { page: props.tradeResult.page })
}

function goPrev() {
  emit('load-trades', { page: props.tradeResult.page - 1 })
}

function goNext() {
  emit('load-trades', { page: props.tradeResult.page + 1 })
}

watch(() => props.tradeResult.records, () => nextTick(renderChart), { deep: true })

onMounted(() => {
  renderChart()
  window.addEventListener('resize', renderChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', renderChart)
  chart?.dispose()
})
</script>
