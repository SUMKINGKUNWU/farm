<template>
  <section v-show="activeTab === 'market'" class="farm-canvas market-canvas">
    <div class="canvas-heading">
      <div><span class="section-kicker">Market</span><h2>交易站买卖</h2></div>
      <button class="button ghost" type="button" @click="emit('open-float', 'trade')">交易确认浮层</button>
    </div>
    <div class="market-game-grid">
      <form class="game-card-form" @submit.prevent="emit('submit-market', 'BUY')">
        <div v-if="!summary?.tradePasswordSet" class="requirement-hint">
          <strong>交易密码未设置</strong>
          <span>交易站买入和卖出都需要交易密码，系统会在后端再次校验。</span>
        </div>
        <label>
          商品
          <select v-model="forms.marketItemCode">
            <option v-for="item in harvestOptions" :key="item.code" :value="item.code">{{ item.name }} · {{ item.code }}</option>
          </select>
        </label>
        <label>数量<input v-model.number="forms.marketQuantity" type="number" min="1" /></label>
        <label>交易密码<input v-model="forms.marketTradePassword" type="password" placeholder="6 位数字" /></label>
        <label>大宗令牌<input v-model.trim="forms.marketBulkTokenCode" placeholder="非大宗可留空" /></label>
        <div class="fee-preview"><span>交易站税费 3%</span><strong>{{ formatMoney(marketTaxEstimate) }}</strong></div>
        <button class="button" type="submit" :disabled="loading">买入</button>
        <button class="button ghost" type="button" :disabled="loading" @click="emit('submit-market', 'SELL')">卖出</button>
      </form>
      <article class="trade-explain-card">
        <span class="section-kicker">Price Rule</span>
        <h3>{{ currentMarketItem?.name || forms.marketItemCode }}</h3>
        <p class="muted">交易站价格按最近成交量、成交价和系统保护参数浮动。大宗交易需要令牌，普通交易只需要交易密码。</p>
        <div class="fee-preview"><span>24h 成交量</span><strong>{{ quote?.volume24h ?? '-' }}</strong></div>
        <div class="fee-preview"><span>24h 成交笔数</span><strong>{{ quote?.tradeCount24h ?? '-' }}</strong></div>
      </article>
    </div>
  </section>
</template>

<script setup>
defineProps({
  activeTab: { type: String, required: true },
  summary: { type: Object, default: null },
  quote: { type: Object, default: null },
  loading: { type: Boolean, required: true },
  forms: { type: Object, required: true },
  harvestOptions: { type: Array, required: true },
  currentMarketItem: { type: Object, default: null },
  marketTaxEstimate: { type: Number, required: true },
  formatMoney: { type: Function, required: true }
})

const emit = defineEmits(['open-float', 'submit-market'])
</script>
