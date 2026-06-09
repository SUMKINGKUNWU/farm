# 0073 玩家活动筛选重置与可读选项

## 本次调整

- 玩家交易原因、流水原因筛选继续保留精确码值提交，但下拉项统一显示中文文案
- 玩家活动面板新增“重置交易”“重置流水”按钮
- 顺手把玩家活动面板和共享文案工具里的历史乱码文本收敛为 UTF-8 正常中文

## 变更文件

- `frontend/src/components/player/ActivityHistoryPanel.vue`
- `frontend/src/utils/playerLabels.js`

## 结果

- 玩家活动筛选现在更接近管理端体验：选项可读、可快速回到默认条件
- 不改后端协议，仍然保持前端向后端提交稳定的精确 reason / source / status 值
