<template>
  <div>
    <header class="hero">
      <div>
        <span class="eyebrow">Admin Console</span>
        <h2>管理经济参数、发放大宗令牌，并核对玩家资产与交易记录。</h2>
        <p>管理端接口通过 Bearer Token 鉴权。税率、令牌、资产和交易操作都应保留审计记录。</p>
      </div>
      <div class="hero-stats">
        <StatCard label="交易站税率" :value="formatRate(admin.marketTax?.rateBasisPoints)" />
        <StatCard label="私下税率" :value="formatRate(admin.privateTax?.rateBasisPoints)" />
        <StatCard label="目标余额" :value="formatMoney(admin.assets?.balance)" />
      </div>
    </header>

    <NoticeBlock :message="admin.message" :error="admin.error" :detail="admin.errorDetail" />

    <TaxConfigPanel
      :admin-tax-types="adminTaxTypes"
      :tax-form="taxForm"
      :loading="admin.loading"
      :format-rate="formatRate"
      @load-tax-configs="admin.loadTaxConfigs"
      @save-tax="saveTax"
    />
    <BulkTokenPanel
      :token-form="tokenForm"
      :issued-token="admin.issuedToken"
      :loading="admin.loading"
      :format-date="formatDate"
      @issue-token="issueToken"
    />
    <PlayerAssetsPanel
      :assets="admin.assets"
      :loading="admin.loading"
      :format-money="formatMoney"
      @load-assets="admin.loadAssets"
    />
    <TradeRecordsPanel
      :trade-result="admin.tradeResult"
      :trade-filters="admin.tradeFilters"
      :loading="admin.loading"
      :format-money="formatMoney"
      :format-date="formatDate"
      @load-trades="admin.loadTrades"
    />
    <AuditLogPanel
      :audit-logs="admin.auditLogs"
      :loading="admin.loading"
      :format-date="formatDate"
      @load-audit-logs="admin.loadAuditLogs"
    />
  </div>
</template>

<script setup>
import { useAdminStore } from '../stores/adminStore'
import NoticeBlock from './common/NoticeBlock.vue'
import StatCard from './common/StatCard.vue'
import TaxConfigPanel from './admin/TaxConfigPanel.vue'
import BulkTokenPanel from './admin/BulkTokenPanel.vue'
import PlayerAssetsPanel from './admin/PlayerAssetsPanel.vue'
import TradeRecordsPanel from './admin/TradeRecordsPanel.vue'
import AuditLogPanel from './admin/AuditLogPanel.vue'

defineProps({
  adminTaxTypes: { type: Array, required: true },
  taxForm: { type: Object, required: true },
  tokenForm: { type: Object, required: true },
  formatRate: { type: Function, required: true },
  formatMoney: { type: Function, required: true },
  formatDate: { type: Function, required: true },
  saveTax: { type: Function, required: true },
  issueToken: { type: Function, required: true }
})

const admin = useAdminStore()
</script>
