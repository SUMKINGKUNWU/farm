<template>
  <section id="assets" class="panel">
    <div class="panel-heading">
      <div>
        <span class="section-kicker">Player Assets</span>
        <h3>玩家资产</h3>
      </div>
      <button class="button ghost" type="button" :disabled="loading" @click="emit('load-assets')">
        读取资产
      </button>
    </div>
    <div class="asset-summary">
      <StatCard label="用户名" :value="assets?.username || '-'" />
      <StatCard label="状态" :value="assets?.status || '-'" />
      <StatCard label="可用金币" :value="formatMoney(assets?.balance)" />
      <StatCard label="锁定金币" :value="formatMoney(assets?.lockedBalance)" />
    </div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr><th>商品</th><th>类型</th><th>可用</th><th>锁定</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in assets?.inventory || []" :key="item.itemId">
            <td>{{ item.itemName }} <small>{{ item.itemCode }}</small></td>
            <td>{{ item.itemType }}</td>
            <td>{{ item.availableQuantity }}</td>
            <td>{{ item.lockedQuantity }}</td>
          </tr>
          <tr v-if="!assets?.inventory?.length"><td colspan="4">暂无库存数据</td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import StatCard from '../common/StatCard.vue'

defineProps({
  assets: { type: Object, default: null },
  loading: { type: Boolean, required: true },
  formatMoney: { type: Function, required: true }
})

const emit = defineEmits(['load-assets'])
</script>
