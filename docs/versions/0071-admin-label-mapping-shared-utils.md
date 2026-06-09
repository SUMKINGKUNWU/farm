# 0071 管理端文案映射共享化

## 本次调整

- 新增前端共享文案工具 `frontend/src/utils/adminLabels.js`
- 把管理端交易记录和审计日志里的状态、来源、方向、原因、动作、目标类型文案映射收拢到共享函数
- 两个面板改为复用同一套映射，不再各自维护重复逻辑

## 变更文件

- `frontend/src/utils/adminLabels.js`
- `frontend/src/components/admin/TradeRecordsPanel.vue`
- `frontend/src/components/admin/AuditLogPanel.vue`

## 结果

- 管理端交易和审计相关文案映射现在只有一份实现
- 后续新增码值或调整展示文案时，只需要改一个公共模块
