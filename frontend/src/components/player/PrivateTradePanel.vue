<template>
  <section v-show="activeTab === 'private'" class="farm-canvas private-canvas">
    <div class="canvas-heading">
      <div><span class="section-kicker">Private Trade</span><h2>私下交易</h2></div>
      <button class="button ghost" type="button" @click="emit('open-float', 'trade')">交易确认浮层</button>
    </div>
    <div class="private-game-grid">
      <form class="game-card-form" @submit.prevent="emit('create-private-trade')">
        <div v-if="!summary?.tradePasswordSet" class="requirement-hint">
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
        <button class="button wide" type="submit" :disabled="loading">创建报价</button>
      </form>
      <div class="offer-stack private-offer-groups">
        <label>接受报价交易密码<input v-model="forms.privateAcceptPassword" type="password" placeholder="买方交易密码" /></label>
        <label>大宗令牌<input v-model.trim="forms.privateBulkTokenCode" placeholder="可选" /></label>

        <div class="offer-group">
          <div class="offer-group-head">
            <strong>发给我的</strong>
            <span>{{ incomingOffers.length }}</span>
          </div>
          <div v-for="offer in incomingOffers" :key="offer.offerId" class="offer-card">
            <strong>{{ offer.itemCode }} × {{ offer.quantity }}</strong>
            <span>{{ formatMoney(offer.priceAmount) }} · {{ offer.status }}</span>
            <small>卖方：{{ offer.sellerUsername || offer.sellerUserId }}</small>
            <button
              v-if="offer.status === 'WAIT_ACCEPT'"
              class="button mini"
              type="button"
              :disabled="loading"
              @click="emit('accept-private-trade', offer.offerId)"
            >
              接受
            </button>
          </div>
          <div v-if="!incomingOffers.length" class="empty-action">
            <strong>暂无待处理报价</strong>
            <span>别人发给你的私下交易会出现在这里。</span>
          </div>
        </div>

        <div class="offer-group">
          <div class="offer-group-head">
            <strong>我发出的</strong>
            <span>{{ outgoingOffers.length }}</span>
          </div>
          <div v-for="offer in outgoingOffers" :key="offer.offerId" class="offer-card">
            <strong>{{ offer.itemCode }} × {{ offer.quantity }}</strong>
            <span>{{ formatMoney(offer.priceAmount) }} · {{ offer.status }}</span>
            <small>买方：{{ offer.buyerUsername || offer.buyerUserId }}</small>
            <button
              v-if="offer.status === 'WAIT_ACCEPT'"
              class="button ghost mini"
              type="button"
              :disabled="loading"
              @click="emit('cancel-private-trade', offer.offerId)"
            >
              取消
            </button>
          </div>
          <div v-if="!outgoingOffers.length" class="empty-action">
            <strong>暂无我发出的报价</strong>
            <span>创建报价后，会在这里查看状态和取消未成交报价。</span>
          </div>
        </div>

        <div class="offer-group">
          <div class="offer-group-head">
            <strong>已结束</strong>
            <span>{{ archivedOffers.length }}</span>
          </div>
          <div v-for="offer in archivedOffers" :key="offer.offerId" class="offer-card archived-offer">
            <strong>{{ offer.itemCode }} × {{ offer.quantity }}</strong>
            <span>{{ formatMoney(offer.priceAmount) }} · {{ offer.status }}</span>
            <small>{{ formatDate(offer.createdAt) }}</small>
          </div>
          <div v-if="!archivedOffers.length" class="empty-action">
            <strong>暂无已结束报价</strong>
            <span>已完成、已取消和已过期的报价会归档在这里。</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  activeTab: { type: String, required: true },
  summary: { type: Object, default: null },
  currentUser: { type: Object, default: null },
  privateTrades: { type: Array, required: true },
  loading: { type: Boolean, required: true },
  forms: { type: Object, required: true },
  harvestOptions: { type: Array, required: true },
  privateTaxEstimate: { type: Number, required: true },
  formatMoney: { type: Function, required: true }
})

const emit = defineEmits(['open-float', 'create-private-trade', 'accept-private-trade', 'cancel-private-trade'])

const incomingOffers = computed(() =>
  props.privateTrades.filter((offer) => offer.buyerUserId === props.currentUser?.userId && offer.status === 'WAIT_ACCEPT')
)

const outgoingOffers = computed(() =>
  props.privateTrades.filter((offer) => offer.sellerUserId === props.currentUser?.userId && offer.status === 'WAIT_ACCEPT')
)

const archivedOffers = computed(() =>
  props.privateTrades.filter((offer) => offer.status !== 'WAIT_ACCEPT')
)

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}
</script>
