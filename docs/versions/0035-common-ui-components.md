# 0035 公共 UI 组件抽取

## 当前已有内容

- `0034` 已经将玩家端游戏界面拆分为 `GamePlayerWorkspace.vue`。
- `App.vue` 和 `GamePlayerWorkspace.vue` 中仍各自定义了 `NoticeBlock`、`StatCard`，存在重复。

## 本次完成内容

- 新增公共提示组件：`frontend/src/components/common/NoticeBlock.vue`。
- 新增公共统计卡片组件：`frontend/src/components/common/StatCard.vue`。
- `App.vue` 改为导入公共 `NoticeBlock` 和 `StatCard`。
- `GamePlayerWorkspace.vue` 改为导入公共 `NoticeBlock` 和 `StatCard`。
- 删除两个文件中的重复内联组件定义。

## 验证

- 已执行前端构建：`npm.cmd run build`。
- 构建通过。
- 仍存在 ECharts 大 chunk 警告，与历史情况一致，不影响本次抽取。

## 下次建议

- 继续拆分管理台组件，降低 `App.vue` 体积。
- 将身份登录卡片也拆成独立组件，后续可以分别优化玩家登录和管理员登录。
- 逐步清理旧模板中的乱码中文文案。
