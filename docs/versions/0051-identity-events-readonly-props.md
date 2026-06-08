# 0051 身份卡只读入参和事件通信

## 当前已有

- 玩家登录卡和管理员登录卡已从 `App.vue` 抽成独立组件。
- 玩家端、管理台工作区已拆分，并完成只读入参和事件通信解耦。
- `App.vue` 继续作为根布局、模式切换和全局初始化入口。

## 本次完成

- `PlayerIdentity.vue` 不再直接接收完整 `player` store。
- `PlayerIdentity.vue` 改为接收 `identityForm`、`isLoggedIn`、`currentUser`、`loading`。
- `PlayerIdentity.vue` 通过 `login`、`register`、`refresh-dashboard`、`logout` 事件交给父组件执行动作。
- `AdminIdentity.vue` 不再直接接收完整 `admin` store。
- `AdminIdentity.vue` 改为接收 `identityForm`、`isLoggedIn`、`currentUser`、`loading`。
- `AdminIdentity.vue` 通过 `login`、`refresh-console`、`logout` 事件交给父组件执行动作。
- `App.vue` 统一绑定 store 动作，身份卡组件只负责表单展示和用户意图触发。

## 验证结果

- 已搜索 `frontend/src/components/identity`，确认身份卡组件内无 `player.` 和 `admin.` 直接访问。
- 已确认 `App.vue` 绑定身份卡事件：
  - `@login`
  - `@register`
  - `@refresh-dashboard`
  - `@refresh-console`
  - `@logout`
- 已执行 `npm.cmd run build`，前端生产构建通过。
- 构建仍提示 ECharts 独立 chunk 超过 500 kB；该 chunk 是动态加载的管理台图表包。

## 下次建议

- 为管理台补充回归脚本，覆盖税率读取、税率保存、令牌发放、资产读取和交易记录刷新。
- 或继续整理前端 API 错误提示与登录态失效流程，提升测试时的错误可读性。
