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

function defaultTradeFilters() {
  return {
    source: 'ALL',
    status: 'ALL',
    page: 1,
    pageSize: 10
  }
}

function defaultTradeResult() {
  return {
    records: [],
    total: 0,
    page: 1,
    pageSize: 10,
    hasNext: false
  }
}

export const useAdminStore = defineStore('admin', {
  state: () => ({
    username: '',
    password: '',
    currentUser: null,
    accessToken: localStorage.getItem('farm_admin_token') || '',
    targetUserId: '',
    userSearchQuery: '',
    userSearchResults: [],
    tradeFilters: defaultTradeFilters(),
    taxConfigs: [],
    assets: null,
    tradeResult: defaultTradeResult(),
    auditLogs: [],
    issuedToken: null,
    loading: false,
    message: '',
    error: '',
    errorDetail: null
  }),
  getters: {
    isLoggedIn(state) {
      return Boolean(state.accessToken)
    },
    marketTax(state) {
      return state.taxConfigs.find((item) => item.tradeType === 'MARKET')
    },
    privateTax(state) {
      return state.taxConfigs.find((item) => item.tradeType === 'PRIVATE')
    },
    bulkTax(state) {
      return state.taxConfigs.find((item) => item.tradeType === 'BULK')
    }
  },
  actions: {
    authHeaders() {
      if (!this.accessToken) {
        throw new ApiClientError({ message: '请先登录管理员账号', code: 'AUTH_REQUIRED' })
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
      this.userSearchQuery = ''
      this.userSearchResults = []
      this.tradeFilters = defaultTradeFilters()
      this.tradeResult = defaultTradeResult()
      localStorage.removeItem('farm_admin_token')
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
    targetId() {
      if (!this.targetUserId.trim()) {
        throw new ApiClientError({ message: '请先填写目标玩家用户 ID', code: 'TARGET_USER_REQUIRED' })
      }
      return this.targetUserId.trim()
    },
    async searchUsers() {
      return this.run(async () => {
        const keyword = this.userSearchQuery.trim()
        if (!keyword) {
          this.userSearchResults = []
          return []
        }
        this.userSearchResults = await requestJson(`/api/admin/users/search?q=${encodeURIComponent(keyword)}`, {
          headers: this.authHeaders()
        })
        return this.userSearchResults
      }, '玩家搜索结果已刷新')
    },
    selectTargetUser(user) {
      this.targetUserId = user?.userId || ''
      this.userSearchQuery = user?.username || ''
      this.tradeFilters.page = 1
    },
    async login() {
      return this.run(async () => {
        if (!this.username.trim() || !this.password) {
          throw new ApiClientError({ message: '请填写管理员用户名和密码', code: 'VALIDATION_ERROR' })
        }
        const result = await requestJson('/api/auth/login', {
          method: 'POST',
          body: JSON.stringify({
            username: this.username.trim(),
            password: this.password
          })
        })
        if (result.role !== 'ADMIN') {
          throw new ApiClientError({ message: '该账号不是管理员', code: 'ADMIN_REQUIRED' })
        }
        this.currentUser = result
        this.accessToken = result.accessToken
        localStorage.setItem('farm_admin_token', result.accessToken)
        return result
      }, '管理员登录成功')
    },
    logout() {
      this.currentUser = null
      this.accessToken = ''
      this.password = ''
      this.taxConfigs = []
      this.assets = null
      this.tradeFilters = defaultTradeFilters()
      this.tradeResult = defaultTradeResult()
      this.auditLogs = []
      this.issuedToken = null
      localStorage.removeItem('farm_admin_token')
      this.setMessage('已退出登录')
    },
    async loadMe() {
      if (!this.accessToken) return null
      return this.run(async () => {
        this.currentUser = await requestJson('/api/auth/me', {
          headers: this.authHeaders()
        })
        return this.currentUser
      })
    },
    async loadTaxConfigs() {
      return this.run(async () => {
        this.taxConfigs = await requestJson('/api/admin/tax-configs', {
          headers: this.authHeaders()
        })
        return this.taxConfigs
      }, '税率配置已刷新')
    },
    async updateTaxConfig(tradeType, payload) {
      return this.run(async () => {
        const config = await requestJson(`/api/admin/tax-configs/${tradeType}`, {
          method: 'PUT',
          headers: this.authHeaders(),
          body: JSON.stringify(payload)
        })
        const index = this.taxConfigs.findIndex((item) => item.tradeType === config.tradeType)
        if (index >= 0) this.taxConfigs.splice(index, 1, config)
        else this.taxConfigs.push(config)
        return config
      }, `${tradeType} 税率已更新`)
    },
    async issueBulkToken(payload) {
      return this.run(async () => {
        this.issuedToken = await requestJson(`/api/admin/users/${this.targetId()}/bulk-tokens`, {
          method: 'POST',
          headers: this.authHeaders(),
          body: JSON.stringify(payload)
        })
        return this.issuedToken
      }, '大宗交易令牌已发放')
    },
    async loadAssets() {
      return this.run(async () => {
        this.assets = await requestJson(`/api/admin/users/${this.targetId()}/assets`, {
          headers: this.authHeaders()
        })
        return this.assets
      }, '玩家资产已刷新')
    },
    async loadTrades(overrides = {}) {
      return this.run(async () => {
        this.tradeFilters = {
          ...this.tradeFilters,
          ...overrides
        }
        const params = new URLSearchParams({
          source: this.tradeFilters.source,
          status: this.tradeFilters.status,
          page: String(this.tradeFilters.page),
          pageSize: String(this.tradeFilters.pageSize)
        })
        this.tradeResult = await requestJson(`/api/admin/users/${this.targetId()}/trades?${params.toString()}`, {
          headers: this.authHeaders()
        })
        return this.tradeResult
      }, '交易记录已刷新')
    },
    async loadAuditLogs() {
      return this.run(async () => {
        this.auditLogs = await requestJson('/api/admin/audit-logs', {
          headers: this.authHeaders()
        })
        return this.auditLogs
      }, '审计日志已刷新')
    },
    async loadPlayerConsole() {
      await this.loadAssets()
      await this.loadTrades({ page: 1 })
    }
  }
})
