# 0049 清理 App 旧版玩家工作区

## 当前已有

- 玩家端主界面已由 `GamePlayerWorkspace.vue` 和 `frontend/src/components/player` 子组件承载。
- 管理台主界面已由 `AdminWorkspace.vue` 和 `frontend/src/components/admin` 子组件承载。
- 玩家端和管理台组件均已完成只读入参和事件通信解耦。

## 本次完成

- 清理 `App.vue` 中未被使用的旧版 `PlayerWorkspace` 内联组件。
- 删除旧版玩家表单 `playerForms`、旧版生产/交易函数和旧版地块辅助函数。
- 删除 `App.vue` 中未使用的 `computed`、`NoticeBlock`、`StatCard` 导入。
- 修复侧边栏玩家登录卡和管理登录卡中的乱码文案。
- 保留 `App.vue` 的职责为：
  - 玩家端和管理台模式切换。
  - 登录身份卡展示。
  - 管理台税率表单、令牌表单和格式化函数。
  - 首次加载玩家物品、玩家会话和管理员会话。

## 验证结果

- 已搜索 `App.vue`，确认无 `PlayerWorkspace`、`playerForms`、旧版玩家交易函数和旧版乱码片段残留。
- 已执行 `npm.cmd run build`，前端生产构建通过。
- 构建仍提示 ECharts 独立 chunk 超过 500 kB；该 chunk 是动态加载的管理台图表包。

## 下次建议

- 为管理台补充与玩家端类似的回归脚本，覆盖税率读取、税率保存、令牌发放、资产读取和交易记录刷新。
- 或继续把登录身份卡从 `App.vue` 抽成独立组件，进一步降低根组件复杂度。
