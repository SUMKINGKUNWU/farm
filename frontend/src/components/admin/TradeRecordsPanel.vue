<template>
  <section id="trades" class="panel">
    <div class="panel-heading">
      <div>
        <span class="section-kicker">Trade Records</span>
        <h3>交易记录</h3>
      </div>
      <button class="button ghost" type="button" :disabled="admin.loading" @click="admin.loadTrades">
        读取交易
      </button>
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
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useAdminStore } from '../../stores/adminStore'

defineProps({
  formatMoney: { type: Function, required: true }
})

const admin = useAdminStore()
const chartEl = ref(null)
let chart
let echartsModulePromise

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
