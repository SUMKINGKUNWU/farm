<template>
  <section id="assets" class="panel">
    <div class="panel-heading">
      <div>
        <span class="section-kicker">Player Assets</span>
        <h3>玩家资产</h3>
      </div>
      <button class="button ghost" type="button" :disabled="loading" @click="refreshAssets">
        刷新资产
      </button>
    </div>

    <div class="asset-summary">
      <StatCard label="用户名" :value="assets?.username || '-'" />
      <StatCard label="状态" :value="assets?.status || '-'" />
      <StatCard label="可用金币" :value="formatMoney(assets?.balance)" />
      <StatCard label="锁定金币" :value="formatMoney(assets?.lockedBalance)" />
    </div>

    <div class="toolbar-row">
      <label>
        物品类型
        <select v-model="draftItemType">
          <option value="ALL">全部</option>
          <option value="SEED">种子</option>
          <option value="ANIMAL">动物</option>
          <option value="FEED">饲料</option>
          <option value="HARVEST">产出物</option>
          <option value="TOKEN">令牌</option>
          <option value="CONSUMABLE">消耗品</option>
        </select>
      </label>
      <button class="button subtle" type="button" :disabled="loading" @click="applyFilter">
        应用筛选
      </button>
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
import { ref, watch } from 'vue'
import StatCard from '../common/StatCard.vue'

const props = defineProps({
  assets: { type: Object, default: null },
  assetFilters: { type: Object, required: true },
  loading: { type: Boolean, required: true },
  formatMoney: { type: Function, required: true }
})

const emit = defineEmits(['load-assets'])
const draftItemType = ref('ALL')

watch(
  () => props.assetFilters.itemType,
  (value) => {
    draftItemType.value = value
  },
  { immediate: true }
)

function applyFilter() {
  emit('load-assets', { itemType: draftItemType.value })
}

function refreshAssets() {
  emit('load-assets', { itemType: draftItemType.value })
}
</script>
