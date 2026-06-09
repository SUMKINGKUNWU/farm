# 0068 审计日志动作与目标筛选项下发

## 本次调整

- 扩展管理端 `GET /api/admin/audit-logs/filter-options`
- 后端新增返回 `actionOptions` 和 `targetTypeOptions`
- 管理端审计日志面板的“动作”“目标”筛选从后端枚举渲染，不再写死在前端
- 保留现有 `reason` 建议项和模糊查询语义

## 变更文件

- `backend/src/main/java/com/farm/exchange/admin/AdminAuditLogFilterOptionsResponse.java`
- `backend/src/main/java/com/farm/exchange/admin/AdminService.java`
- `backend/src/test/java/com/farm/exchange/FarmExchangeApplicationTests.java`
- `frontend/src/stores/adminStore.js`
- `frontend/src/components/AdminWorkspace.vue`
- `frontend/src/components/admin/AuditLogPanel.vue`

## 结果

- 管理端审计日志筛选项现在由后端统一提供：动作、目标类型、原因建议
- 后续如果审计动作或目标类型扩展，前端不需要再同步改硬编码枚举
