import { defineStore } from 'pinia'
import { ApiClientError, createApiError, normalizeError } from '../apiError'

async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  })
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw createApiError(body, response)
  }
  return body
}

export const usePlayerStore = defineStore('player', {
  state: () => ({
    username: '',
    nickname: '',
    password: '',
    currentUser: null,
    accessToken: localStorage.getItem('farm_player_token') || '',
    summary: null,
    farm: null,
    ranch: null,
    inventory: [],
    items: [],
    growthInstances: [],
    quote: null,
    bulkTokens: [],
    privateTrades: [],
    tradeHistory: [],
    ledgerEntries: [],
    lastProduction: null,
    lastTrade: null,
    loading: false,
    message: '',
    error: '',
    errorDetail: null
  }),
  getters: {
    isLoggedIn(state) {
      return Boolean(state.accessToken)
    },
    seeds(state) {
      return state.items.filter((item) => item.itemType === 'SEED')
    },
    animals(state) {
      return state.items.filter((item) => item.itemType === 'ANIMAL')
    },
    harvests(state) {
      return state.items.filter((item) => item.itemType === 'HARVEST')
    },
    activeGrowths(state) {
      return state.growthInstances.filter((item) => ['GROWING', 'READY'].includes(item.status))
    }
  },
  actions: {
    authHeaders() {
      if (!this.accessToken) {
        throw new ApiClientError({ message: '请先登录玩家账号', code: 'AUTH_REQUIRED' })
      }
      return { Authorization: `Bearer ${this.accessToken}` }
    },
    setMessage(message) {
      this.message = message
      this.error = ''
      this.errorDetail = null
    },
    setError(error) {
      const normalized = normalizeError(error)
      this.error = normalized.message
      this.errorDetail = {
        code: normalized.code,
        status: normalized.status,
        path: normalized.path,
        action: normalized.action,
        fieldErrors: normalized.fieldErrors
      }
      this.message = ''
    },
    clearSession() {
      this.currentUser = null
      this.accessToken = ''
      localStorage.removeItem('farm_player_token')
    },
    async run(task, successMessage) {
      this.loading = true
      this.error = ''
      this.errorDetail = null
      try {
        const result = await task()
        if (successMessage) this.setMessage(successMessage)
        return result
      } catch (error) {
        this.setError(error)
        if (error instanceof ApiClientError && ['AUTH_REQUIRED', 'AUTH_INVALID', 'AUTH_EXPIRED'].includes(error.code)) {
          this.clearSession()
        }
        throw error
      } finally {
        this.loading = false
      }
    },
    async register() {
      return this.run(async () => {
        if (!this.username.trim() || !this.nickname.trim() || !this.password) {
          throw new ApiClientError({ message: '请填写用户名、昵称和密码', code: 'VALIDATION_ERROR' })
        }
        const result = await requestJson('/api/auth/register', {
          method: 'POST',
          body: JSON.stringify({
            username: this.username.trim(),
            nickname: this.nickname.trim(),
            password: this.password
          })
        })
        await this.login()
        return result
      }, '玩家注册成功，已自动登录')
    },
    async login() {
      return this.run(async () => {
        if (!this.username.trim() || !this.password) {
          throw new ApiClientError({ message: '请填写玩家用户名和密码', code: 'VALIDATION_ERROR' })
        }
        const result = await requestJson('/api/auth/login', {
          method: 'POST',
          body: JSON.stringify({
            username: this.username.trim(),
            password: this.password
          })
        })
        this.currentUser = result
        this.accessToken = result.accessToken
        localStorage.setItem('farm_player_token', result.accessToken)
        await this.loadDashboard()
        return result
      }, '玩家登录成功')
    },
    logout() {
      this.clearSession()
      this.password = ''
      this.summary = null
      this.farm = null
      this.ranch = null
      this.inventory = []
      this.growthInstances = []
      this.bulkTokens = []
      this.privateTrades = []
      this.tradeHistory = []
      this.ledgerEntries = []
      this.lastProduction = null
      this.lastTrade = null
      this.setMessage('已退出玩家账号')
    },
    async loadMe() {
      if (!this.accessToken) return null
      return this.run(async () => {
        this.currentUser = await requestJson('/api/auth/me', {
          headers: this.authHeaders()
        })
        await this.loadDashboard()
        return this.currentUser
      })
    },
    async loadItems() {
      return this.run(async () => {
        this.items = await requestJson('/api/items')
        return this.items
      })
    },
    async loadDashboard() {
      return this.run(async () => {
        const headers = this.authHeaders()
        const [summary, farm, ranch, inventory, growthInstances, bulkTokens, privateTrades] = await Promise.all([
          requestJson('/api/me/summary', { headers }),
          requestJson('/api/me/farm/plots', { headers }),
          requestJson('/api/me/ranch/slots', { headers }),
          requestJson('/api/me/inventory', { headers }),
          requestJson('/api/me/growth', { headers }),
          requestJson('/api/me/bulk-tokens', { headers }),
          requestJson('/api/me/private-trades', { headers })
        ])
        this.summary = summary
        this.farm = farm
        this.ranch = ranch
        this.inventory = inventory
        this.growthInstances = growthInstances
        this.bulkTokens = bulkTokens
        this.privateTrades = privateTrades
        return summary
      }, '玩家数据已刷新')
    },
    async setTradePassword(tradePassword) {
      return this.run(async () => {
        await requestJson('/api/me/trade-password', {
          method: 'POST',
          headers: this.authHeaders(),
          body: JSON.stringify({ tradePassword })
        })
        await this.loadDashboard()
      }, '交易密码已设置')
    },
    async expand(slotType) {
      return this.run(async () => {
        const path = slotType === 'FARM' ? '/api/me/farm/expand' : '/api/me/ranch/expand'
        await requestJson(path, { method: 'POST', headers: this.authHeaders() })
        await this.loadDashboard()
      }, slotType === 'FARM' ? '农田栏位已扩建' : '牧场栏位已扩建')
    },
    async purchase(payload) {
      return this.run(async () => {
        await requestJson('/api/me/shop/purchase', {
          method: 'POST',
          headers: this.authHeaders(),
          body: JSON.stringify(payload)
        })
        await this.loadDashboard()
      }, '商店购买成功')
    },
    async startProduction(slotType, slotId, itemCode) {
      return this.run(async () => {
        const encodedItemCode = encodeURIComponent(itemCode)
        const path = slotType === 'FARM'
          ? `/api/me/farm/plots/${slotId}/plant?itemCode=${encodedItemCode}`
          : `/api/me/ranch/slots/${slotId}/raise?itemCode=${encodedItemCode}`
        this.lastProduction = await requestJson(path, {
          method: 'POST',
          headers: this.authHeaders()
        })
        await this.loadDashboard()
        return this.lastProduction
      }, '生产已开始')
    },
    async harvest(growthId) {
      return this.run(async () => {
        await requestJson(`/api/me/growth/${growthId}/harvest`, {
          method: 'POST',
          headers: this.authHeaders()
        })
        await this.loadDashboard()
      }, '收获成功')
    },
    async quote(itemCode) {
      return this.run(async () => {
        this.quote = await requestJson(`/api/me/market/items/${encodeURIComponent(itemCode)}/quote`, {
          headers: this.authHeaders()
        })
        return this.quote
      }, '行情已刷新')
    },
    async marketTrade(side, payload) {
      return this.run(async () => {
        this.lastTrade = await requestJson(`/api/me/market/${side.toLowerCase()}`, {
          method: 'POST',
          headers: this.authHeaders(),
          body: JSON.stringify(payload)
        })
        await this.loadDashboard()
        return this.lastTrade
      }, side === 'BUY' ? '交易站买入成功' : '交易站卖出成功')
    },
    async createPrivateTrade(payload) {
      return this.run(async () => {
        await requestJson('/api/me/private-trades', {
          method: 'POST',
          headers: this.authHeaders(),
          body: JSON.stringify(payload)
        })
        await this.loadDashboard()
      }, '私下交易报价已创建')
    },
    async acceptPrivateTrade(offerId, payload) {
      return this.run(async () => {
        await requestJson(`/api/me/private-trades/${offerId}/accept`, {
          method: 'POST',
          headers: this.authHeaders(),
          body: JSON.stringify(payload)
        })
        await this.loadDashboard()
      }, '私下交易已成交')
    },
    async cancelPrivateTrade(offerId) {
      return this.run(async () => {
        await requestJson(`/api/me/private-trades/${offerId}/cancel`, {
          method: 'POST',
          headers: this.authHeaders()
        })
        await this.loadDashboard()
      }, '私下交易报价已取消')
    },
    async loadActivity() {
      return this.run(async () => {
        const headers = this.authHeaders()
        const [tradeHistory, ledgerEntries] = await Promise.all([
          requestJson('/api/me/trades', { headers }),
          requestJson('/api/me/ledger', { headers })
        ])
        this.tradeHistory = tradeHistory
        this.ledgerEntries = ledgerEntries
        return { tradeHistory, ledgerEntries }
      }, '活动记录已刷新')
    }
  }
})
