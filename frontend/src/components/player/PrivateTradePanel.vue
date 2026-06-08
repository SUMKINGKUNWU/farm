<template>
  <section v-show="activeTab === 'private'" class="farm-canvas private-canvas">
    <div class="canvas-heading">
      <div><span class="section-kicker">Private Trade</span><h2>私下交易</h2></div>
      <button class="button ghost" type="button" @click="showFloat('trade')">交易确认浮层</button>
    </div>
    <div class="private-game-grid">
      <form class="game-card-form" @submit.prevent="createPrivateTrade">
        <div v-if="!player.summary?.tradePasswordSet" class="requirement-hint">
          <strong>私下交易前置条件</strong>
          <span>私下交易需要双方都设置并输入自己的交易密码。</span>
        </div>
        <label>买方用户 ID<input v-model.trim="forms.privateBuyerUserId" placeholder="对方玩家 UUID" /></label>
        <label>
          商品
          <select v-model="forms.privateItemCode">
            <option v-for="item in harvestOptions" :key="item.code" :value="item.code">{{ item.name }} · {{ item.code }}</option>
          </select>
        </label>
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
          <button
            v-if="offer.buyerUserId === player.currentUser?.userId && offer.status === 'WAIT_ACCEPT'"
            class="button mini"
            type="button"
            :disabled="player.loading"
            @click="acceptPrivateTrade(offer.offerId)"
          >
            接受
          </button>
          <button
            v-else-if="offer.sellerUserId === player.currentUser?.userId && offer.status === 'WAIT_ACCEPT'"
            class="button ghost mini"
            type="button"
            :disabled="player.loading"
            @click="player.cancelPrivateTrade(offer.offerId)"
          >
            取消
          </button>
        </div>
        <div v-if="!player.privateTrades.length" class="empty-action">
          <strong>暂无私下交易报价</strong>
          <span>卖方创建报价后，买方可在这里输入交易密码并接受报价。</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  activeTab: { type: String, required: true },
  player: { type: Object, required: true },
  forms: { type: Object, required: true },
  harvestOptions: { type: Array, required: true },
  privateTaxEstimate: { type: Number, required: true },
  formatMoney: { type: Function, required: true },
  showFloat: { type: Function, required: true },
  createPrivateTrade: { type: Function, required: true },
  acceptPrivateTrade: { type: Function, required: true }
})
</script>
