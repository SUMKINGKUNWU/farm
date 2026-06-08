<template>
  <section id="token" class="panel split">
    <div>
      <span class="section-kicker">Bulk Token</span>
      <h3>发放大宗交易令牌</h3>
      <p class="muted">用于高数量或高金额交易。成交时扣减可用次数，并累计令牌使用金额。</p>
      <form class="form-grid" @submit.prevent="emit('issue-token')">
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
        <button class="button wide" type="submit" :disabled="loading">发放令牌</button>
      </form>
    </div>
    <article class="token-ticket">
      <span>最近发放</span>
      <strong>{{ issuedToken?.tokenCode || '尚未发放' }}</strong>
      <p>次数：{{ issuedToken?.remainingUses ?? '-' }} / 状态：{{ issuedToken?.status || '-' }}</p>
      <p>到期：{{ formatDate(issuedToken?.expiresAt) }}</p>
    </article>
  </section>
</template>

<script setup>
defineProps({
  tokenForm: { type: Object, required: true },
  issuedToken: { type: Object, default: null },
  loading: { type: Boolean, required: true },
  formatDate: { type: Function, required: true }
})

const emit = defineEmits(['issue-token'])
</script>
