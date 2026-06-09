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
    reason: 'ALL',
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

function defaultTradeFilterOptions() {
  return {
    reasons: []
  }
}

function defaultAuditFilters() {
  const today = new Date().toISOString().slice(0, 10)
  return {
    action: 'ALL',
    targetType: 'ALL',
    reason: '',
    from: today,
    to: today,
    page: 1,
    pageSize: 10
  }
}

function defaultAuditResult() {
  return {
    records: [],
    total: 0,
    page: 1,
    pageSize: 10,
    hasNext: false
  }
}

function defaultAuditFilterOptions() {
  return {
    actionOptions: [],
    targetTypeOptions: [],
    reasonOptions: []
  }
}

function defaultAssetFilters() {
  return {
    itemType: 'ALL'
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
    assetFilters: defaultAssetFilters(),
    tradeFilters: defaultTradeFilters(),
    tradeFilterOptions: defaultTradeFilterOptions(),
    auditFilters: defaultAuditFilters(),
    auditFilterOptions: defaultAuditFilterOptions(),
    taxConfigs: [],
    assets: null,
    tradeResult: defaultTradeResult(),
    auditResult: defaultAuditResult(),
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
      this.assetFilters = defaultAssetFilters()
      this.tradeFilters = defaultTradeFilters()
      this.tradeFilterOptions = defaultTradeFilterOptions()
      this.auditFilters = defaultAuditFilters()
      this.auditFilterOptions = defaultAuditFilterOptions()
      this.tradeResult = defaultTradeResult()
      this.auditResult = defaultAuditResult()
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
      this.auditFilters.page = 1
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
      this.assetFilters = defaultAssetFilters()
      this.tradeFilters = defaultTradeFilters()
      this.tradeFilterOptions = defaultTradeFilterOptions()
      this.auditFilters = defaultAuditFilters()
      this.auditFilterOptions = defaultAuditFilterOptions()
      this.tradeResult = defaultTradeResult()
      this.auditResult = defaultAuditResult()
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
    async loadAssets(overrides = {}) {
      return this.run(async () => {
        this.assetFilters = {
          ...this.assetFilters,
          ...overrides
        }
        const params = new URLSearchParams({
          itemType: this.assetFilters.itemType
        })
        this.assets = await requestJson(`/api/admin/users/${this.targetId()}/assets?${params.toString()}`, {
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
          reason: this.tradeFilters.reason,
          page: String(this.tradeFilters.page),
          pageSize: String(this.tradeFilters.pageSize)
        })
        this.tradeResult = await requestJson(`/api/admin/users/${this.targetId()}/trades?${params.toString()}`, {
          headers: this.authHeaders()
        })
        return this.tradeResult
      }, '交易记录已刷新')
    },
    async loadTradeFilterOptions() {
      return this.run(async () => {
        const result = await requestJson(`/api/admin/users/${this.targetId()}/trades/filter-options`, {
          headers: this.authHeaders()
        })
        this.tradeFilterOptions = {
          reasons: Array.isArray(result?.reasons) ? result.reasons : []
        }
        return this.tradeFilterOptions
      })
    },
    async loadAuditLogs(overrides = {}) {
      return this.run(async () => {
        this.auditFilters = {
          ...this.auditFilters,
          ...overrides
        }
        const params = new URLSearchParams({
          action: this.auditFilters.action,
          targetType: this.auditFilters.targetType,
          reason: this.auditFilters.reason || '',
          from: this.auditFilters.from || '',
          to: this.auditFilters.to || '',
          page: String(this.auditFilters.page),
          pageSize: String(this.auditFilters.pageSize)
        })
        this.auditResult = await requestJson(`/api/admin/audit-logs?${params.toString()}`, {
          headers: this.authHeaders()
        })
        return this.auditResult
      }, '审计日志已刷新')
    },
    async loadAuditFilterOptions() {
      return this.run(async () => {
        const result = await requestJson('/api/admin/audit-logs/filter-options', {
          headers: this.authHeaders()
        })
        this.auditFilterOptions = {
          actionOptions: Array.isArray(result?.actionOptions) ? result.actionOptions : [],
          targetTypeOptions: Array.isArray(result?.targetTypeOptions) ? result.targetTypeOptions : [],
          reasonOptions: Array.isArray(result?.reasonOptions) ? result.reasonOptions : []
        }
        return this.auditFilterOptions
      })
    },
    async loadPlayerConsole() {
      await this.loadAssets()
      await this.loadTradeFilterOptions()
      await this.loadTrades({ page: 1 })
      await this.loadAuditLogs({ page: 1 })
    }
  }
})
