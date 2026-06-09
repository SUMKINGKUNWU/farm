export function adminTradeSourceLabel(value) {
  if (value === 'MARKET') return '交易站'
  if (value === 'PRIVATE') return '私下交易'
  return value || '-'
}

export function adminTradeSideLabel(value) {
  if (value === 'BUY') return '买入'
  if (value === 'SELL') return '卖出'
  return value || '-'
}

export function adminTradeStatusLabel(value) {
  if (value === 'COMPLETED') return '已完成'
  if (value === 'WAIT_ACCEPT') return '待接受'
  if (value === 'SETTLING') return '结算中'
  if (value === 'CANCELLED') return '已取消'
  if (value === 'EXPIRED') return '已过期'
  if (value === 'FAILED') return '失败'
  return value || '-'
}

export function adminTradeReasonLabel(value) {
  if (value === 'MARKET_BUY') return '交易站买入'
  if (value === 'MARKET_SELL') return '交易站卖出'
  if (value === 'PRIVATE_TRADE_CREATE') return '私下交易创建'
  if (value === 'PRIVATE_TRADE_ACCEPT') return '私下交易成交'
  if (value === 'PRIVATE_TRADE_CANCEL') return '私下交易取消'
  if (value === 'PRIVATE_TRADE_EXPIRE') return '私下交易过期'
  if (value === 'PRIVATE_TRADE_UPDATE') return '私下交易更新'
  return value || '-'
}

export function adminAuditActionLabel(value) {
  if (value === 'UPDATE_TAX_CONFIG') return '更新税率'
  if (value === 'ISSUE_BULK_TOKEN') return '发放大宗令牌'
  return value || '-'
}

export function adminAuditTargetTypeLabel(value) {
  if (value === 'TAX_CONFIG') return '税率配置'
  if (value === 'APP_USER') return '玩家'
  return value || '-'
}
