<template>
  <main class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">穗</div>
        <div>
          <p>Farm Exchange</p>
          <h1>{{ activeMode === 'player' ? '玩家农场' : '运营管理台' }}</h1>
        </div>
      </div>

      <div class="mode-switch">
        <button :class="{ active: activeMode === 'player' }" type="button" @click="activeMode = 'player'">玩家端</button>
        <button :class="{ active: activeMode === 'admin' }" type="button" @click="activeMode = 'admin'">管理台</button>
      </div>

      <PlayerIdentity v-if="activeMode === 'player'" :player="player" />
      <AdminIdentity v-else :admin="admin" @refresh-console="loadAdminEverything" />

      <nav class="nav" v-if="activeMode === 'player'">
        <a href="#player-overview">概览</a>
        <a href="#player-shop">商店</a>
        <a href="#player-fields">田地牧场</a>
        <a href="#player-market">交易站</a>
        <a href="#player-private">私下交易</a>
      </nav>
      <nav class="nav" v-else>
        <a href="#tax">税率</a>
        <a href="#token">令牌</a>
        <a href="#assets">资产</a>
        <a href="#trades">交易</a>
      </nav>
    </aside>

    <section class="workspace">
      <GamePlayerWorkspace v-if="activeMode === 'player'" />
      <AdminWorkspace
        v-else
        :admin-tax-types="adminTaxTypes"
        :tax-form="taxForm"
        :token-form="tokenForm"
        :format-rate="formatRate"
        :format-money="formatMoney"
        :format-date="formatDate"
        :save-tax="saveTax"
        :issue-token="issueToken"
      />
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useAdminStore } from './stores/adminStore'
import { usePlayerStore } from './stores/playerStore'
import GamePlayerWorkspace from './components/GamePlayerWorkspace.vue'
import AdminWorkspace from './components/AdminWorkspace.vue'
import PlayerIdentity from './components/identity/PlayerIdentity.vue'
import AdminIdentity from './components/identity/AdminIdentity.vue'

const activeMode = ref('player')
const admin = useAdminStore()
const player = usePlayerStore()

const adminTaxTypes = [
  { code: 'MARKET', name: '交易站' },
  { code: 'PRIVATE', name: '私下交易' },
  { code: 'BULK', name: '大宗交易' }
]

const taxForm = reactive({
  MARKET: { rateBasisPoints: 300, reason: '' },
  PRIVATE: { rateBasisPoints: 500, reason: '' },
  BULK: { rateBasisPoints: 300, reason: '' }
})

const tokenForm = reactive({
  allowedItemType: 'HARVEST',
  singleTradeLimit: 200000,
  totalLimit: 200000,
  remainingUses: 1,
  expireHours: 24
})

function formatRate(value) {
  if (value === undefined || value === null) return '-'
  return `${(Number(value) / 100).toFixed(2)}%`
}

function formatMoney(value) {
  if (value === undefined || value === null) return '-'
  return Number(value).toLocaleString('zh-CN')
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

async function saveTax(type) {
  await admin.updateTaxConfig(type, taxForm[type])
}

async function issueToken() {
  await admin.issueBulkToken(tokenForm)
}

async function loadAdminEverything() {
  await admin.loadTaxConfigs()
  if (admin.targetUserId) {
    await admin.loadPlayerConsole()
  }
}

function syncTaxForm(configs) {
  configs.forEach((config) => {
    if (taxForm[config.tradeType]) {
      taxForm[config.tradeType].rateBasisPoints = config.rateBasisPoints
      taxForm[config.tradeType].reason = config.updatedReason || ''
    }
  })
}

watch(() => admin.taxConfigs, syncTaxForm, { deep: true })

onMounted(() => {
  player.loadItems().catch(() => {})
  player.loadMe().catch(() => player.clearSession())
  admin.loadMe().catch(() => admin.clearSession?.() || admin.logout())
})
</script>
