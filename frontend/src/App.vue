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

      <PlayerIdentity v-if="activeMode === 'player'" />
      <AdminIdentity v-else />

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
import { defineComponent, onMounted, reactive, ref, watch } from 'vue'
import { useAdminStore } from './stores/adminStore'
import { usePlayerStore } from './stores/playerStore'
import GamePlayerWorkspace from './components/GamePlayerWorkspace.vue'
import AdminWorkspace from './components/AdminWorkspace.vue'

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

const PlayerIdentity = defineComponent({
  setup() {
    return { player }
  },
  template: `
    <div class="identity-card">
      <template v-if="!player.isLoggedIn">
        <label>玩家用户名<input v-model.trim="player.username" placeholder="例如 farmer_001" /></label>
        <label>玩家昵称<input v-model.trim="player.nickname" placeholder="注册时填写昵称" /></label>
        <label>登录密码<input v-model="player.password" type="password" placeholder="至少 6 位" @keydown.enter="player.login" /></label>
        <div class="button-row">
          <button class="button ghost" type="button" :disabled="player.loading" @click="player.login">登录</button>
          <button class="button subtle" type="button" :disabled="player.loading" @click="player.register">注册并登录</button>
        </div>
      </template>
      <template v-else>
        <div class="login-badge">
          <span>当前玩家</span>
          <strong>{{ player.currentUser?.username || player.username || '已登录' }}</strong>
        </div>
        <button class="button ghost" type="button" :disabled="player.loading" @click="player.loadDashboard">刷新农场</button>
        <button class="button subtle" type="button" @click="player.logout">退出登录</button>
      </template>
    </div>
  `
})

const AdminIdentity = defineComponent({
  setup() {
    return { admin, loadAdminEverything }
  },
  template: `
    <div class="identity-card">
      <template v-if="!admin.isLoggedIn">
        <label>管理员用户名<input v-model.trim="admin.username" placeholder="请输入管理员用户名" /></label>
        <label>登录密码<input v-model="admin.password" type="password" placeholder="请输入登录密码" @keydown.enter="admin.login" /></label>
        <button class="button ghost" type="button" :disabled="admin.loading" @click="admin.login">登录管理台</button>
      </template>
      <template v-else>
        <div class="login-badge">
          <span>当前管理员</span>
          <strong>{{ admin.currentUser?.username || admin.username || '已登录' }}</strong>
        </div>
        <label>目标玩家用户 ID<input v-model.trim="admin.targetUserId" placeholder="要查询或发令牌的玩家 UUID" /></label>
        <button class="button ghost" type="button" :disabled="admin.loading" @click="loadAdminEverything">刷新控制台</button>
        <button class="button subtle" type="button" @click="admin.logout">退出登录</button>
      </template>
    </div>
  `
})

watch(() => admin.taxConfigs, syncTaxForm, { deep: true })

onMounted(() => {
  player.loadItems().catch(() => {})
  player.loadMe().catch(() => player.clearSession())
  admin.loadMe().catch(() => admin.clearSession?.() || admin.logout())
})
</script>
