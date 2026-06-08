# 0045 玩家端组件事件化

## 当前已有

- 玩家端工作区已拆分为顶部状态、农牧地块、商店、交易站、行情侧栏、私下交易、底部页签和悬浮层组件。
- 父组件 `GamePlayerWorkspace.vue` 负责表单状态、计算属性和后端动作调用。
- 子组件负责各自区域展示和用户操作入口。

## 本次完成

- 将玩家端子组件中的父级动作函数 props 改为 `emit` 事件。
- `GameTopbar.vue` 改为触发 `open-float`、`refresh`。
- `FarmRanchPanel.vue` 改为触发 `expand`、`open-float`、`start-production`、`harvest`。
- `ShopPanel.vue` 改为触发 `open-float`、`purchase`。
- `MarketPanel.vue` 改为触发 `open-float`、`submit-market`。
- `PrivateTradePanel.vue` 改为触发 `open-float`、`create-private-trade`、`accept-private-trade`、`cancel-private-trade`。
- `GameBottomTabs.vue` 改为触发 `change-tab`、`open-float`。
- `PlayerFloatLayer.vue` 改为触发 `close`、`save-trade-password`。
- 父组件统一接收事件并调用 Pinia store 或本地状态方法，降低子组件对父组件实现细节的依赖。

## 验证结果

- 已执行 `npm.cmd run build`，前端生产构建通过。
- 当前会话未暴露可用的 in-app browser 操作工具，未执行浏览器点击回归。
- 构建仍提示 ECharts 独立 chunk 超过 500 kB；该 chunk 是动态加载的管理台图表包。

## 下次建议

- 继续做浏览器级交互回归，重点验证事件化后的按钮点击、页签切换、悬浮层关闭、交易密码保存和表单提交。
- 后续可继续减少展示组件对完整 `player` store 对象的依赖，改传更小的只读数据结构。
