# 0070 管理端交易记录原因筛选

## 本次调整

- 管理端玩家交易记录接口新增 `reason` 筛选参数
- 新增 `GET /api/admin/users/{targetUserId}/trades/filter-options`
- 后端返回交易原因建议项 `reasons`
- 管理端交易记录面板新增原因建议筛选，并在表格中展示 `tradeReason`

## 变更文件

- `backend/src/main/java/com/farm/exchange/admin/AdminController.java`
- `backend/src/main/java/com/farm/exchange/admin/AdminService.java`
- `backend/src/main/java/com/farm/exchange/admin/AdminTradeRecordResponse.java`
- `backend/src/main/java/com/farm/exchange/admin/AdminTradeFilterOptionsResponse.java`
- `backend/src/test/java/com/farm/exchange/FarmExchangeApplicationTests.java`
- `frontend/src/stores/adminStore.js`
- `frontend/src/components/AdminWorkspace.vue`
- `frontend/src/components/admin/TradeRecordsPanel.vue`

## 结果

- 管理端交易记录现在可以按交易原因过滤，如 `MARKET_BUY`、`PRIVATE_TRADE_CANCEL`
- 前端交易记录列表也会直接展示交易原因，筛选和列表语义一致
