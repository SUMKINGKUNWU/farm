# 0048 管理台只读入参和事件通信

## 当前已有

- 玩家端组件已完成拆分、事件通信和只读数据入参解耦。
- 管理台已拆分为税率配置、大宗令牌、玩家资产、交易记录 4 个子面板。
- 前端生产构建持续通过，ECharts 仍保持动态导入。

## 本次完成

- 管理台子组件不再直接使用完整 `admin` store。
- `AdminWorkspace.vue` 继续作为唯一管理端 store 汇聚点，负责读取 `admin` 状态并分发给子组件。
- `TaxConfigPanel.vue` 改为接收 `loading`，并通过 `load-tax-configs`、`save-tax` 事件触发父组件动作。
- `BulkTokenPanel.vue` 改为接收 `issuedToken`、`loading`，并通过 `issue-token` 事件触发父组件动作。
- `PlayerAssetsPanel.vue` 改为接收 `assets`、`loading`，并通过 `load-assets` 事件触发父组件动作。
- `TradeRecordsPanel.vue` 改为接收 `trades`、`loading`，并通过 `load-trades` 事件触发父组件动作。
- 交易记录图表继续在子组件内渲染，但数据来源改为只读 `trades` props。

## 验证结果

- 已搜索 `frontend/src/components/admin`，确认管理台子组件内无 `admin.` 直接访问。
- 已搜索确认管理台子组件不再导入 `useAdminStore`。
- 已执行 `npm.cmd run build`，前端生产构建通过。
- 构建仍提示 ECharts 独立 chunk 超过 500 kB；该 chunk 是动态加载的管理台图表包。

## 下次建议

- 继续整理 `App.vue` 中遗留的旧版玩家工作区代码和乱码模板，避免后续维护时误用。
- 或者为管理台补充与玩家端类似的回归脚本，覆盖税率读取、令牌发放、资产读取和交易记录刷新。
