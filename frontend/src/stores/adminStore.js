import { defineStore } from 'pinia'

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
    throw new Error(body?.message || `请求失败：${response.status}`)
  }
  return body
}

export const useAdminStore = defineStore('admin', {
  state: () => ({
    adminUserId: '',
    targetUserId: '',
    taxConfigs: [],
    assets: null,
    trades: [],
    issuedToken: null,
    loading: false,
    message: '',
    error: ''
  }),
  getters: {
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
    setMessage(message) {
      this.message = message
      this.error = ''
    },
    setError(error) {
      this.error = error instanceof Error ? error.message : String(error)
      this.message = ''
    },
    async run(task, successMessage) {
      this.loading = true
      this.error = ''
      try {
        const result = await task()
        if (successMessage) this.setMessage(successMessage)
        return result
      } catch (error) {
        this.setError(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    adminQuery() {
      if (!this.adminUserId.trim()) {
        throw new Error('请先填写管理员用户 ID')
      }
      return `adminUserId=${encodeURIComponent(this.adminUserId.trim())}`
    },
    targetId() {
      if (!this.targetUserId.trim()) {
        throw new Error('请先填写目标玩家用户 ID')
      }
      return this.targetUserId.trim()
    },
    async loadTaxConfigs() {
      return this.run(async () => {
        this.taxConfigs = await requestJson(`/api/admin/tax-configs?${this.adminQuery()}`)
        return this.taxConfigs
      }, '税率配置已刷新')
    },
    async updateTaxConfig(tradeType, payload) {
      return this.run(async () => {
        const config = await requestJson(`/api/admin/tax-configs/${tradeType}?${this.adminQuery()}`, {
          method: 'PUT',
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
        this.issuedToken = await requestJson(`/api/admin/users/${this.targetId()}/bulk-tokens?${this.adminQuery()}`, {
          method: 'POST',
          body: JSON.stringify(payload)
        })
        return this.issuedToken
      }, '大宗交易令牌已发放')
    },
    async loadAssets() {
      return this.run(async () => {
        this.assets = await requestJson(`/api/admin/users/${this.targetId()}/assets?${this.adminQuery()}`)
        return this.assets
      }, '玩家资产已刷新')
    },
    async loadTrades() {
      return this.run(async () => {
        this.trades = await requestJson(`/api/admin/users/${this.targetId()}/trades?${this.adminQuery()}`)
        return this.trades
      }, '交易记录已刷新')
    },
    async loadPlayerConsole() {
      await this.loadAssets()
      await this.loadTrades()
    }
  }
})
