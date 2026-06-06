# 0036 管理台工作区组件拆分

## 当前已有内容

- 玩家端游戏界面已拆分为 `GamePlayerWorkspace.vue`。
- 公共展示组件 `NoticeBlock.vue`、`StatCard.vue` 已抽取。
- `App.vue` 仍内联管理台工作区，文件复杂度偏高。

## 本次完成内容

- 新增管理台工作区组件：`frontend/src/components/AdminWorkspace.vue`。
- `App.vue` 改为导入并使用 `AdminWorkspace`。
- 删除 `App.vue` 中内联的管理台工作区组件。
- 管理台继续复用公共 `NoticeBlock` 和 `StatCard`。
- 管理台 ECharts 图表逻辑移动到 `AdminWorkspace.vue` 内部。
- 管理台模板文案改为正常中文，修复原有乱码和破损属性。
- `App.vue` 移除不再需要的 `echarts`、`nextTick`、`onBeforeUnmount` 导入。

## 验证

- 已执行前端构建：`npm.cmd run build`。
- 构建通过。
- 仍存在 ECharts 大 chunk 警告，与历史情况一致，不影响本次拆分。

## 下次建议

- 拆分玩家身份卡和管理员身份卡，继续降低 `App.vue` 复杂度。
- 将管理台的税率、令牌、资产、交易记录进一步拆成子组件。
- 逐步清理 `App.vue` 剩余旧中文乱码文案。
