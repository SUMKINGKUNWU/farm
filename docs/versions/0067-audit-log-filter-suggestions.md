# 0067 审计日志筛选建议项

## 本次调整

- 管理端新增 `GET /api/admin/audit-logs/filter-options`
- 后端返回最近使用过的审计原因建议列表 `reasonOptions`
- 管理端审计日志面板保留“原因关键词”手输，同时增加建议项下拉选择
- 补充接口测试，覆盖建议项返回包含发放令牌和税率调整原因

## 变更文件

- `backend/src/main/java/com/farm/exchange/admin/AdminController.java`
- `backend/src/main/java/com/farm/exchange/admin/AdminService.java`
- `backend/src/main/java/com/farm/exchange/admin/AdminAuditLogFilterOptionsResponse.java`
- `backend/src/test/java/com/farm/exchange/FarmExchangeApplicationTests.java`
- `frontend/src/stores/adminStore.js`
- `frontend/src/components/AdminWorkspace.vue`
- `frontend/src/components/admin/AuditLogPanel.vue`
- `frontend/src/App.vue`

## 结果

- 审计日志筛选现在既支持关键词模糊检索，也支持直接选最近常用原因
- 前端可以稳定复用后端返回的原因枚举，不需要手工记忆审计 reason 文本
