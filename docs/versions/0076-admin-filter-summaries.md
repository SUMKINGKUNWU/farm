# 0076 管理端筛选摘要补全

## 本次调整

- 管理端交易记录面板新增“当前交易条件”摘要条
- 管理端审计日志面板新增“当前审计条件”摘要条
- 两个面板都补了重置按钮，支持一键回到默认筛选条件
- 管理端标签映射文件改为统一中文文案，修复原有乱码

## 交易记录面板

文件：

- `frontend/src/components/admin/TradeRecordsPanel.vue`
- `frontend/src/utils/adminLabels.js`

调整点：

- 摘要条仅展示非默认筛选项：
  - 来源
  - 状态
  - 原因
  - 每页
- 新增“重置交易”按钮
- 图表图例和表格标题统一改为正常中文

## 审计日志面板

文件：

- `frontend/src/components/admin/AuditLogPanel.vue`
- `frontend/src/utils/adminLabels.js`

调整点：

- 摘要条仅展示非默认筛选项：
  - 动作
  - 目标
  - 原因
  - 开始日期
  - 结束日期
  - 每页
- 新增“重置日志”按钮
- 表格、按钮、筛选项中文文案统一修复

## 验证

前端构建通过：

```powershell
cd D:\ai-project\farm\frontend
npm.cmd run build
```
