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
      <StatCard label="交易记录" :value="tradeHistory.total" />
      <StatCard label="资产流水" :value="ledgerEntries.total" />
      <StatCard label="最新成交" :value="tradeHistory.records[0]?.tradeSource || '-'" />
      <StatCard label="最新流水" :value="ledgerEntries.records[0]?.assetType || '-'" />
    </div>

    <div class="records-grid">
      <div>
        <div class="toolbar-row">
          <label>
            交易来源
            <select v-model="tradeDraft.source">
              <option value="ALL">全部</option>
              <option value="MARKET">交易站</option>
              <option value="PRIVATE">私下交易</option>
            </select>
          </label>
          <label>
            交易状态
            <select v-model="tradeDraft.status">
              <option value="ALL">全部</option>
              <option value="COMPLETED">已完成</option>
              <option value="WAIT_ACCEPT">待接受</option>
              <option value="SETTLING">结算中</option>
              <option value="CANCELLED">已取消</option>
              <option value="EXPIRED">已过期</option>
              <option value="FAILED">失败</option>
            </select>
          </label>
          <label>
            交易原因
            <select v-model="tradeDraft.reason">
              <option value="ALL">全部</option>
              <option v-for="reason in tradeReasonOptions" :key="reason" :value="reason">{{ reason }}</option>
            </select>
          </label>
          <label>
            每页
            <select v-model.number="tradeDraft.pageSize">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </label>
          <button class="button subtle" type="button" :disabled="loading" @click="applyTradeFilters">
            应用筛选
          </button>
        </div>

        <div class="table-meta">
          <span>共 {{ tradeHistory.total }} 条</span>
          <span>第 {{ tradeHistory.page }} / {{ tradeTotalPages }} 页</span>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>时间</th><th>来源</th><th>方向</th><th>商品</th><th>数量</th><th>金额</th><th>税费</th><th>对手方</th><th>状态</th></tr>
            </thead>
            <tbody>
              <tr v-for="trade in tradeHistory.records" :key="trade.tradeId">
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
              <tr v-if="!tradeHistory.records.length"><td colspan="9">暂无交易记录</td></tr>
            </tbody>
          </table>
        </div>

        <div class="pager-row">
          <button class="button ghost" type="button" :disabled="loading || tradeHistory.page <= 1" @click="changeTradePage(tradeHistory.page - 1)">
            上一页
          </button>
          <label class="page-jump">
            页码
            <input v-model.number="tradeJumpPage" type="number" min="1" :max="tradeTotalPages" />
          </label>
          <button class="button subtle" type="button" :disabled="loading" @click="goTradePage">
            跳转
          </button>
          <button class="button ghost" type="button" :disabled="loading || !tradeHistory.hasNext" @click="changeTradePage(tradeHistory.page + 1)">
            下一页
          </button>
        </div>
      </div>

      <div>
        <div class="toolbar-row">
          <label>
            资产类型
            <select v-model="ledgerDraft.assetType">
              <option value="ALL">全部</option>
              <option value="COIN">金币</option>
              <option value="ITEM">物品</option>
              <option value="TOKEN">令牌</option>
            </select>
          </label>
          <label>
            变动方向
            <select v-model="ledgerDraft.direction">
              <option value="ALL">全部</option>
              <option value="IN">收入</option>
              <option value="OUT">支出</option>
            </select>
          </label>
          <label>
            流水原因
            <select v-model="ledgerDraft.reason">
              <option value="ALL">全部</option>
              <option v-for="reason in ledgerReasonOptions" :key="reason" :value="reason">{{ reason }}</option>
            </select>
          </label>
          <label>
            每页
            <select v-model.number="ledgerDraft.pageSize">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </label>
          <button class="button subtle" type="button" :disabled="loading" @click="applyLedgerFilters">
            应用筛选
          </button>
        </div>

        <div class="table-meta">
          <span>共 {{ ledgerEntries.total }} 条</span>
          <span>第 {{ ledgerEntries.page }} / {{ ledgerTotalPages }} 页</span>
        </div>

        <div class="table-wrap slim-table">
          <table>
            <thead>
              <tr><th>时间</th><th>资产</th><th>商品</th><th>变动</th><th>余额后</th><th>原因</th></tr>
            </thead>
            <tbody>
              <tr v-for="entry in ledgerEntries.records" :key="entry.ledgerId">
                <td>{{ formatDate(entry.createdAt) }}</td>
                <td>{{ entry.assetType }}</td>
                <td>{{ entry.itemCode || '-' }}</td>
                <td :class="entry.changeAmount >= 0 ? 'delta-up' : 'delta-down'">
                  {{ entry.changeAmount > 0 ? `+${entry.changeAmount}` : entry.changeAmount }}
                </td>
                <td>{{ formatMoney(entry.balanceAfter) }}</td>
                <td>{{ entry.reason }}</td>
              </tr>
              <tr v-if="!ledgerEntries.records.length"><td colspan="6">暂无资产流水</td></tr>
            </tbody>
          </table>
        </div>

        <div class="pager-row">
          <button class="button ghost" type="button" :disabled="loading || ledgerEntries.page <= 1" @click="changeLedgerPage(ledgerEntries.page - 1)">
            上一页
          </button>
          <label class="page-jump">
            页码
            <input v-model.number="ledgerJumpPage" type="number" min="1" :max="ledgerTotalPages" />
          </label>
          <button class="button subtle" type="button" :disabled="loading" @click="goLedgerPage">
            跳转
          </button>
          <button class="button ghost" type="button" :disabled="loading || !ledgerEntries.hasNext" @click="changeLedgerPage(ledgerEntries.page + 1)">
            下一页
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import StatCard from '../common/StatCard.vue'

const props = defineProps({
  activeTab: { type: String, required: true },
  tradeHistory: { type: Object, required: true },
  ledgerEntries: { type: Object, required: true },
  tradeFilters: { type: Object, required: true },
  ledgerFilters: { type: Object, required: true },
  tradeReasonOptions: { type: Array, required: true },
  ledgerReasonOptions: { type: Array, required: true },
  loading: { type: Boolean, required: true },
  formatMoney: { type: Function, required: true },
  formatDate: { type: Function, required: true }
})

const emit = defineEmits(['refresh-activity', 'change-trade-filters', 'change-ledger-filters'])

const tradeJumpPage = ref(1)
const ledgerJumpPage = ref(1)

const tradeDraft = reactive({
  source: 'ALL',
  status: 'ALL',
  reason: 'ALL',
  pageSize: 10
})

const ledgerDraft = reactive({
  assetType: 'ALL',
  direction: 'ALL',
  reason: 'ALL',
  pageSize: 10
})

const tradeTotalPages = computed(() => {
  const size = Math.max(Number(props.tradeHistory.pageSize || props.tradeFilters.pageSize || 10), 1)
  return Math.max(1, Math.ceil(Number(props.tradeHistory.total || 0) / size))
})

const ledgerTotalPages = computed(() => {
  const size = Math.max(Number(props.ledgerEntries.pageSize || props.ledgerFilters.pageSize || 10), 1)
  return Math.max(1, Math.ceil(Number(props.ledgerEntries.total || 0) / size))
})

watch(
  () => props.tradeFilters,
  (filters) => {
    tradeDraft.source = filters.source
    tradeDraft.status = filters.status
    tradeDraft.reason = filters.reason
    tradeDraft.pageSize = filters.pageSize
  },
  { deep: true, immediate: true }
)

watch(
  () => props.ledgerFilters,
  (filters) => {
    ledgerDraft.assetType = filters.assetType
    ledgerDraft.direction = filters.direction
    ledgerDraft.reason = filters.reason
    ledgerDraft.pageSize = filters.pageSize
  },
  { deep: true, immediate: true }
)

watch(
  () => props.tradeHistory.page,
  (page) => {
    tradeJumpPage.value = page
  },
  { immediate: true }
)

watch(
  () => props.ledgerEntries.page,
  (page) => {
    ledgerJumpPage.value = page
  },
  { immediate: true }
)

function applyTradeFilters() {
  emit('change-trade-filters', {
    source: tradeDraft.source,
    status: tradeDraft.status,
    reason: tradeDraft.reason,
    pageSize: tradeDraft.pageSize,
    page: 1
  })
}

function applyLedgerFilters() {
  emit('change-ledger-filters', {
    assetType: ledgerDraft.assetType,
    direction: ledgerDraft.direction,
    reason: ledgerDraft.reason,
    pageSize: ledgerDraft.pageSize,
    page: 1
  })
}

function changeTradePage(page) {
  emit('change-trade-filters', { page })
}

function changeLedgerPage(page) {
  emit('change-ledger-filters', { page })
}

function goTradePage() {
  const page = Math.min(Math.max(Number(tradeJumpPage.value || 1), 1), tradeTotalPages.value)
  emit('change-trade-filters', { page })
}

function goLedgerPage() {
  const page = Math.min(Math.max(Number(ledgerJumpPage.value || 1), 1), ledgerTotalPages.value)
  emit('change-ledger-filters', { page })
}
</script>
