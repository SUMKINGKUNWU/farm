<template>
  <aside class="market-side">
    <div class="market-board">
      <span class="section-kicker">Market Station</span>
      <h3>{{ quote?.itemCode || forms.marketItemCode }}</h3>
      <strong class="market-price">{{ formatMoney(quote?.currentPrice) }}</strong>
      <p>最近成交量影响价格，交易站税费 3%。</p>
      <div class="fake-chart"><i></i></div>
      <button class="button wide" type="button" :disabled="loading" @click="emit('refresh-quote', forms.marketItemCode)">刷新行情</button>
    </div>

    <div class="market-trade-card">
      <h3>税费规则</h3>
      <div class="fee-preview"><span>交易站</span><strong>3%</strong></div>
      <div class="fee-preview"><span>私下交易</span><strong>5%</strong></div>
      <div class="fee-preview"><span>令牌</span><strong>{{ bulkTokenCount }}</strong></div>
    </div>
  </aside>
</template>

<script setup>
defineProps({
  quote: { type: Object, default: null },
  bulkTokenCount: { type: Number, required: true },
  loading: { type: Boolean, required: true },
  forms: { type: Object, required: true },
  formatMoney: { type: Function, required: true }
})

const emit = defineEmits(['refresh-quote'])
</script>
