# 0074 玩家活动筛选摘要

## 本次调整

- 玩家活动记录新增“当前交易条件”“当前流水条件”摘要
- 只展示非默认筛选项，并复用共享文案映射生成可读标签
- 同步把玩家活动面板里的历史乱码中文收敛为正常 UTF-8 文案

## 变更文件

- `frontend/src/components/player/ActivityHistoryPanel.vue`
- `frontend/src/utils/playerLabels.js`

## 结果

- 玩家在结果为空或分页切换时，可以直接看到当前生效的筛选条件
- 重置筛选后，摘要会回到“默认条件”，更容易判断当前视图状态
