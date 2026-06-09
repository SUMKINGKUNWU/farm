# 0072 玩家活动文案映射共享化

## 本次调整

- 新增玩家活动共享文案工具 `frontend/src/utils/playerLabels.js`
- 玩家活动记录面板的交易来源、方向、状态、交易原因、资产类型、流水原因统一走共享映射
- 不改筛选逻辑和后端接口，只调整前端展示一致性

## 变更文件

- `frontend/src/utils/playerLabels.js`
- `frontend/src/components/player/ActivityHistoryPanel.vue`

## 结果

- 玩家活动筛选项和列表展示不再直接暴露原始码值
- 玩家端与管理端的交易原因、状态等文案风格保持一致
