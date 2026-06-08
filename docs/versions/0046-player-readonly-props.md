# 0046 玩家端只读数据入参解耦

## 当前已有

- 玩家端工作区已拆分为多个展示组件，并通过 `emit` 事件向父组件传递用户操作。
- `GamePlayerWorkspace.vue` 统一持有 Pinia store、表单状态、计算属性和后端动作调用。
- 子组件已不再通过函数 props 调用父级动作。

## 本次完成

- 移除玩家端子组件对完整 `player` store 对象的直接依赖。
- `GameTopbar.vue` 改为接收 `currentUser`、`username`、`summary`、`loading`。
- `FarmRanchPanel.vue` 改为接收 `summary`、`farm`、`ranch`、`loading`。
- `ShopPanel.vue` 改为接收 `summary`、`inventory`、`loading`。
- `MarketPanel.vue` 改为接收 `summary`、`quote`、`loading`。
- `MarketSidebar.vue` 改为接收 `quote`、`bulkTokenCount`、`loading`，并通过 `refresh-quote` 事件刷新行情。
- `PrivateTradePanel.vue` 改为接收 `summary`、`currentUser`、`privateTrades`、`loading`。
- `PlayerFloatLayer.vue` 改为接收 `currentUser`、`summary`、`bulkTokenCount`、`loading`。

## 验证结果

- 已搜索 `frontend/src/components/player`，确认子组件内无 `player.` 直接访问。
- 已执行 `npm.cmd run build`，前端生产构建通过。
- 构建仍提示 ECharts 独立 chunk 超过 500 kB；该 chunk 是动态加载的管理台图表包。

## 下次建议

- 做浏览器级交互回归，重点验证行情刷新事件、底部页签、悬浮层、交易密码保存、商店购买、交易站买卖和私下交易报价。
- 若回归通过，可继续拆分或规范管理台组件入参，使管理端也逐步降低对完整 store 的直接依赖。
