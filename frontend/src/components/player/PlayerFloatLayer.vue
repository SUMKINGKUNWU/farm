<template>
  <div v-if="activeFloat" class="float-layer" @click.self="emit('close')">
    <article class="glass-pop">
      <button ref="closeButtonRef" class="close-pop" type="button" @click="emit('close')">×</button>
      <template v-if="activeFloat === 'profile'">
        <span class="section-kicker">Profile</span>
        <h3>{{ currentUser?.nickname || currentUser?.username || '农场主' }}</h3>
        <div class="float-grid">
          <StatCard label="可用金币" :value="formatMoney(summary?.balance)" />
          <StatCard label="锁定金币" :value="formatMoney(summary?.lockedBalance)" />
          <StatCard label="大宗令牌" :value="bulkTokenCount" />
          <StatCard label="交易密码" :value="summary?.tradePasswordSet ? '已设置' : '未设置'" />
        </div>
        <form class="compact-form float-password" @submit.prevent="emit('save-trade-password', forms.tradePassword)">
          <label>设置交易密码<input v-model="forms.tradePassword" type="password" maxlength="6" placeholder="6 位数字" /></label>
          <button class="button" type="submit" :disabled="loading">保存</button>
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
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import StatCard from '../common/StatCard.vue'

const props = defineProps({
  activeFloat: { type: String, required: true },
  selectedInfo: { type: Object, default: null },
  currentUser: { type: Object, default: null },
  summary: { type: Object, default: null },
  bulkTokenCount: { type: Number, required: true },
  loading: { type: Boolean, required: true },
  forms: { type: Object, required: true },
  marketTaxEstimate: { type: Number, required: true },
  privateTaxEstimate: { type: Number, required: true },
  formatMoney: { type: Function, required: true },
  formatDuration: { type: Function, required: true }
})

const closeButtonRef = ref(null)
const emit = defineEmits(['close', 'save-trade-password'])

watch(() => props.activeFloat, (value) => {
  if (value) {
    nextTick(() => closeButtonRef.value?.focus())
  }
})
</script>
