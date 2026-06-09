# 0077 管理端资产筛选摘要与类型中文化

## 本次调整

- 管理端玩家资产面板新增“当前资产条件”摘要条
- 管理端玩家资产面板新增“重置资产”按钮
- 资产表格中的 `itemType` 统一改为中文展示
- 审计日志摘要排除了默认当天日期，避免默认值被误显示为额外筛选条件

## 影响文件

- `frontend/src/components/admin/PlayerAssetsPanel.vue`
- `frontend/src/components/admin/AuditLogPanel.vue`
- `frontend/src/utils/adminLabels.js`

## 资产面板调整

- 新增 `adminAssetTypeLabel`
- 资产筛选摘要仅在 `itemType != ALL` 时展示
- 表格“类型”列不再直接显示原始码值：
  - `SEED -> 种子`
  - `ANIMAL -> 动物`
  - `FEED -> 饲料`
  - `HARVEST -> 产出物`
  - `TOKEN -> 令牌`
  - `CONSUMABLE -> 消耗品`

## 审计摘要调整

- 审计默认筛选仍保留当天 `from/to`
- 摘要条只展示用户显式改动后的日期条件

## 验证

```powershell
cd D:\ai-project\farm\frontend
npm.cmd run build
```
