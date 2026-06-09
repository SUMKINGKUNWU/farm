# 0065 活动原因展示与审计日期边界保护

## 本次调整

- 玩家活动记录：
  - `/api/me/trades` 返回结果补充 `tradeReason`
  - 前端“交易与流水”面板新增“原因”列，筛选后可直接核对返回值
- 管理端审计日志：
  - 后端新增日期范围边界保护，`from` 晚于 `to` 时直接返回 `INVALID_OPERATION`
- 管理端玩家资产：
  - 保留已有按 `itemType` 筛选能力
  - 补齐相关面板中文文案，避免筛选项可见但不可辨认

## 变更文件

- `backend/src/main/java/com/farm/exchange/activity/PlayerTradeRecordResponse.java`
- `backend/src/main/java/com/farm/exchange/activity/PlayerActivityService.java`
- `backend/src/main/java/com/farm/exchange/admin/AdminService.java`
- `backend/src/test/java/com/farm/exchange/FarmExchangeApplicationTests.java`
- `frontend/src/components/player/ActivityHistoryPanel.vue`
- `frontend/src/components/admin/AuditLogPanel.vue`
- `frontend/src/components/admin/PlayerAssetsPanel.vue`

## 验证点

- 玩家交易列表可见 `tradeReason`，并与筛选值一致
- 审计日志 `from > to` 返回 400 和稳定错误码
- 资产筛选面板可正常识别筛选项文案
