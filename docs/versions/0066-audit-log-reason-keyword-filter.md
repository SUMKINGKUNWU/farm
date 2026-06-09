# 0066 审计日志原因关键词筛选

## 本次调整

- 管理端审计日志接口新增 `reason` 查询参数
- 后端按 `admin_audit_logs.reason` 做不区分大小写的模糊匹配
- 管理端审计日志筛选栏新增“原因关键词”
- 补充接口测试，覆盖“大宗”“税率”两个检索场景

## 变更文件

- `backend/src/main/java/com/farm/exchange/admin/AdminController.java`
- `backend/src/main/java/com/farm/exchange/admin/AdminService.java`
- `backend/src/test/java/com/farm/exchange/FarmExchangeApplicationTests.java`
- `frontend/src/stores/adminStore.js`
- `frontend/src/components/admin/AuditLogPanel.vue`

## 结果

- 现在可以按发放说明、税率调整原因等文本快速筛审计日志
