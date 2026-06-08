<template>
  <section v-if="activeTab === 'records'" id="player-records" class="farm-canvas">
    <div class="canvas-heading">
      <div>
        <span class="section-kicker">Activity Records</span>
        <h2>交易与流水</h2>
      </div>
      <button class="button ghost" type="button" :disabled="loading" @click="emit('refresh-activity')">
        刷新记录
      </button>
    </div>

    <div class="asset-summary player-activity-summary">
      <StatCard label="交易记录" :value="filteredTradeHistory.length" />
      <StatCard label="资产流水" :value="filteredLedgerEntries.length" />
      <StatCard label="最新成交" :value="filteredTradeHistory[0]?.tradeSource || '-'" />
      <StatCard label="最新流水" :value="filteredLedgerEntries[0]?.assetType || '-'" />
    </div>

    <div class="records-grid">
      <div>
        <div class="toolbar-row">
          <label>
            交易来源
            <select v-model="tradeSourceFilter">
              <option value="ALL">全部</option>
              <option value="MARKET">交易站</option>
              <option value="PRIVATE">私下交易</option>
            </select>
          </label>
          <label>
            交易状态
            <select v-model="tradeStatusFilter">
              <option value="ALL">全部</option>
              <option value="COMPLETED">已完成</option>
              <option value="WAIT_ACCEPT">待接受</option>
              <option value="SETTLING">结算中</option>
              <option value="CANCELLED">已取消</option>
              <option value="EXPIRED">已过期</option>
              <option value="FAILED">失败</option>
            </select>
          </label>
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
                <th>对手方</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="trade in filteredTradeHistory" :key="trade.tradeId">
                <td>{{ formatDate(trade.createdAt) }}</td>
                <td>{{ trade.tradeSource }}</td>
                <td>{{ trade.side }}</td>
                <td>{{ trade.itemCode }}</td>
                <td>{{ trade.quantity }}</td>
                <td>{{ formatMoney(trade.tradeAmount) }}</td>
                <td>{{ formatMoney(trade.taxAmount) }}</td>
                <td>{{ trade.counterpartyUsername || '-' }}<small>{{ trade.counterpartyUserId || '-' }}</small></td>
                <td>{{ trade.status }}</td>
              </tr>
              <tr v-if="!filteredTradeHistory.length">
                <td colspan="9">暂无交易记录</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div>
        <div class="toolbar-row">
          <label>
            资产类型
            <select v-model="ledgerAssetTypeFilter">
              <option value="ALL">全部</option>
              <option value="COIN">金币</option>
              <option value="ITEM">物品</option>
              <option value="TOKEN">令牌</option>
            </select>
          </label>
          <label>
            变动方向
            <select v-model="ledgerDirectionFilter">
              <option value="ALL">全部</option>
              <option value="IN">收入</option>
              <option value="OUT">支出</option>
            </select>
          </label>
        </div>

        <div class="table-wrap slim-table">
          <table>
            <thead>
              <tr>
                <th>时间</th>
                <th>资产</th>
                <th>商品</th>
                <th>变动</th>
                <th>余额后</th>
                <th>原因</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="entry in filteredLedgerEntries" :key="entry.ledgerId">
                <td>{{ formatDate(entry.createdAt) }}</td>
                <td>{{ entry.assetType }}</td>
                <td>{{ entry.itemCode || '-' }}</td>
                <td :class="entry.changeAmount >= 0 ? 'delta-up' : 'delta-down'">
                  {{ entry.changeAmount > 0 ? `+${entry.changeAmount}` : entry.changeAmount }}
                </td>
                <td>{{ formatMoney(entry.balanceAfter) }}</td>
                <td>{{ entry.reason }}</td>
              </tr>
              <tr v-if="!filteredLedgerEntries.length">
                <td colspan="6">暂无资产流水</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import StatCard from '../common/StatCard.vue'

const props = defineProps({
  activeTab: { type: String, required: true },
  tradeHistory: { type: Array, required: true },
  ledgerEntries: { type: Array, required: true },
  loading: { type: Boolean, required: true },
  formatMoney: { type: Function, required: true },
  formatDate: { type: Function, required: true }
})

const emit = defineEmits(['refresh-activity'])
const tradeSourceFilter = ref('ALL')
const tradeStatusFilter = ref('ALL')
const ledgerAssetTypeFilter = ref('ALL')
const ledgerDirectionFilter = ref('ALL')

const filteredTradeHistory = computed(() => props.tradeHistory.filter((trade) => {
  const sourceMatched = tradeSourceFilter.value === 'ALL' || trade.tradeSource === tradeSourceFilter.value
  const statusMatched = tradeStatusFilter.value === 'ALL' || trade.status === tradeStatusFilter.value
  return sourceMatched && statusMatched
}))

const filteredLedgerEntries = computed(() => props.ledgerEntries.filter((entry) => {
  const typeMatched = ledgerAssetTypeFilter.value === 'ALL' || entry.assetType === ledgerAssetTypeFilter.value
  const directionMatched = ledgerDirectionFilter.value === 'ALL'
    || (ledgerDirectionFilter.value === 'IN' && entry.changeAmount >= 0)
    || (ledgerDirectionFilter.value === 'OUT' && entry.changeAmount < 0)
  return typeMatched && directionMatched
}))
</script>
