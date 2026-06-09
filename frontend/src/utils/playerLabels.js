export function playerTradeSourceLabel(value) {
  if (value === 'MARKET') return '交易站'
  if (value === 'PRIVATE') return '私下交易'
  return value || '-'
}

export function playerTradeSideLabel(value) {
  if (value === 'BUY') return '买入'
  if (value === 'SELL') return '卖出'
  return value || '-'
}

export function playerTradeStatusLabel(value) {
  if (value === 'COMPLETED') return '已完成'
  if (value === 'WAIT_ACCEPT') return '待接受'
  if (value === 'SETTLING') return '结算中'
  if (value === 'CANCELLED') return '已取消'
  if (value === 'EXPIRED') return '已过期'
  if (value === 'FAILED') return '失败'
  return value || '-'
}

export function playerTradeReasonLabel(value) {
  if (value === 'MARKET_BUY') return '交易站买入'
  if (value === 'MARKET_SELL') return '交易站卖出'
  if (value === 'PRIVATE_TRADE_CREATE') return '私下交易创建'
  if (value === 'PRIVATE_TRADE_ACCEPT') return '私下交易成交'
  if (value === 'PRIVATE_TRADE_CANCEL') return '私下交易取消'
  if (value === 'PRIVATE_TRADE_EXPIRE') return '私下交易过期'
  if (value === 'PRIVATE_TRADE_UPDATE') return '私下交易更新'
  return value || '-'
}

export function playerAssetTypeLabel(value) {
  if (value === 'COIN') return '金币'
  if (value === 'ITEM') return '物品'
  if (value === 'TOKEN') return '令牌'
  return value || '-'
}

export function playerLedgerDirectionLabel(value) {
  if (value === 'IN') return '收入'
  if (value === 'OUT') return '支出'
  return value || '-'
}

export function playerLedgerReasonLabel(value) {
  if (value === 'INITIAL_GRANT') return '初始发放'
  if (value === 'EXPAND_FARM') return '扩建农田'
  if (value === 'EXPAND_RANCH') return '扩建牧场'
  if (value === 'SHOP_PURCHASE') return '商店购买'
  if (value === 'HARVEST_OUTPUT') return '收获产出'
  if (value === 'MARKET_BUY') return '交易站买入'
  if (value === 'MARKET_SELL') return '交易站卖出'
  if (value === 'PRIVATE_TRADE_LOCK') return '私下交易锁定'
  if (value === 'PRIVATE_TRADE_RELEASE') return '私下交易释放'
  if (value === 'PRIVATE_TRADE_EXPIRE_RELEASE') return '私下交易过期释放'
  if (value === 'BULK_TOKEN_USE') return '使用大宗令牌'
  return value || '-'
}
