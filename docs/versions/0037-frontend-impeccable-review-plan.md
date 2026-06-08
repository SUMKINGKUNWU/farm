# 0037 前端页面审查与优化方案

## 当前已有内容

- 已按 `impeccable` 要求新增 `PRODUCT.md`，确认默认设计语境为 `product`。
- 当前前端已拆分为：
  - `frontend/src/App.vue`
  - `frontend/src/components/GamePlayerWorkspace.vue`
  - `frontend/src/components/AdminWorkspace.vue`
  - `frontend/src/components/common/NoticeBlock.vue`
  - `frontend/src/components/common/StatCard.vue`
- 当前构建命令 `npm.cmd run build` 可通过。

## 审查结论

当前页面已经从“功能面板”推进到“游戏化产品界面”，方向正确；但距离稳定可测的 Web MVP 还有明显差距。主要问题不在接口，而在 UI 质量、状态表达、可访问性、响应式和文案一致性。

### P0 必须优先处理

1. 管理台文案仍有乱码和破损属性风险。
   - 影响文件：`frontend/src/components/AdminWorkspace.vue`
   - 表现：标题、按钮、表格列、说明文案和部分 `StatCard` label 在源码/终端中仍显示乱码。
   - 风险：管理台虽然构建通过，但用户实际阅读成本高，后续维护时容易再次引入模板破损。
   - 建议：先完整重写管理台中文文案，统一为正常 UTF-8。

2. 交互组件缺少统一 focus 样式。
   - 影响：按钮、输入框、select、底部页签、地块按钮、库存 pill、悬浮层关闭按钮。
   - 风险：键盘用户无法稳定判断当前位置，不符合 WCAG AA。
   - 建议：增加 `:focus-visible` 视觉规范，使用清晰外描边或盒阴影，并避免只靠颜色表达。

3. 模态悬浮层没有完整键盘和焦点管理。
   - 影响文件：`GamePlayerWorkspace.vue`
   - 表现：玻璃浮层打开后没有焦点捕获、Esc 关闭、关闭后焦点回到触发按钮等行为。
   - 风险：交易确认、个人信息、物品信息属于重要操作提示，当前键盘可用性不足。
   - 建议：短期增加 Esc 关闭和打开后聚焦关闭按钮；中期使用原生 `<dialog>` 或抽 `ModalLayer`。

## P1 高优先级优化

1. 视觉系统需要收敛。
   - 当前存在大量 `border + 大阴影 + 大圆角 + 背景渐变` 叠加。
   - `impeccable` 产品型 UI 规则更推荐克制、可信、任务优先。
   - 建议：保留农场氛围，但减少装饰性阴影和过大圆角；普通卡片半径控制在 12 到 18px，底部导航和标签可以保持 pill。

2. 颜色和对比度需要系统化。
   - 当前 `--paper / --paper-strong / --muted` 等暖色变量较多，且 muted 文本在浅暖背景上可能偏淡。
   - 建议：新增语义 token：`--surface`、`--surface-raised`、`--text`、`--text-muted`、`--focus`、`--success`、`--warning`、`--danger`。
   - 建议：对正文、表格、提示、禁用状态做对比度复核。

3. 玩家端状态表达仍偏英文和技术化。
   - 当前地块角标为 `Locked / Open / Growing / Ready`。
   - 建议：改为中文 `未扩建 / 可投入 / 成长中 / 可收获`，并配合图标或形状，不只依赖颜色。

4. 空状态和前置条件提示不足。
   - 库存为空、报价为空已有简单提示，但不够指导下一步。
   - 建议：空库存提示增加“去商店购买种子/动物”；未设置交易密码时在交易表单内直接提示“先设置交易密码”。

5. 管理台信息架构仍偏长页面。
   - 税率、令牌、资产、交易记录目前在一个组件中纵向堆叠。
   - 建议：拆成 `TaxConfigPanel`、`BulkTokenPanel`、`PlayerAssetsPanel`、`TradeRecordsPanel`。

## P2 中优先级优化

1. ECharts 进入首包导致构建大 chunk 警告。
   - 当前构建 JS 约 1.15MB。
   - 建议：管理台图表组件使用动态导入 ECharts，玩家端首屏不加载图表库。

2. 响应式需要实际验收。
   - 当前 CSS 有 `max-width: 1100px` 断点，但未见移动端真实验收截图。
   - 建议：至少检查 390px、768px、1366px 三个宽度。

3. 动效缺少 reduced motion 保护。
   - 当前 hover/transition 存在，但没有统一 `prefers-reduced-motion`。
   - 建议：添加全局 reduced motion 规则，禁用 transform 动画。

4. 文案体系需要统一。
   - 玩家端部分文案已经正常中文，管理台仍不一致。
   - 建议：短期统一中文；中期抽 `copy` 常量，后续支持 i18n。

## 建议落地顺序

1. 修复管理台 UTF-8 文案和破损中文，确保所有页面可读。
2. 增加统一 focus-visible、disabled、loading、empty 状态样式。
3. 优化玻璃悬浮层：Esc 关闭、焦点管理、点击外部关闭规则确认。
4. 收敛视觉 token：减少过度阴影、大圆角、装饰背景。
5. 动态加载 ECharts，降低玩家端首包。
6. 拆管理台子面板，继续降低单组件复杂度。
7. 做三档响应式实测并根据结果修复布局。

## 本次验证

- 已执行 `npm.cmd run build`。
- 构建通过。
- 仍存在 ECharts 大 chunk 警告。

## 下次建议

下一轮建议直接从 P0 开始：修复管理台乱码文案，并补统一 focus-visible 样式。这样收益最高，风险最低。
