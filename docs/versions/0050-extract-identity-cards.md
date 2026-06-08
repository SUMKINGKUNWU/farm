# 0050 抽取登录身份卡组件

## 当前已有

- `App.vue` 已清理旧版玩家工作区，只负责模式切换、根布局、管理台表单和初始化逻辑。
- 玩家端、管理台工作区均已拆分为独立组件。
- 玩家端和管理台子组件均已完成只读入参和事件通信解耦。

## 本次完成

- 新增 `frontend/src/components/identity/PlayerIdentity.vue`。
- 新增 `frontend/src/components/identity/AdminIdentity.vue`。
- 将 `App.vue` 中内联的玩家登录卡和管理员登录卡抽到独立组件。
- `PlayerIdentity.vue` 负责玩家登录、注册、刷新农场、退出登录入口。
- `AdminIdentity.vue` 负责管理员登录、目标玩家 ID、刷新控制台、退出登录入口。
- 管理员刷新控制台通过 `refresh-console` 事件交给 `App.vue` 执行，保持父组件集中处理管理台组合动作。

## 验证结果

- 已搜索确认 `App.vue` 中无 `defineComponent`、内联模板字符串、`PlayerIdentity` 内联定义和 `AdminIdentity` 内联定义。
- 已执行 `npm.cmd run build`，前端生产构建通过。
- 构建仍提示 ECharts 独立 chunk 超过 500 kB；该 chunk 是动态加载的管理台图表包。

## 下次建议

- 为管理台补充回归脚本，覆盖税率读取、税率保存、令牌发放、资产读取和交易记录刷新。
- 或继续梳理 `App.vue` 与身份卡组件的 store 依赖，改为只读入参和事件通信，进一步降低根组件与身份卡之间的耦合。
