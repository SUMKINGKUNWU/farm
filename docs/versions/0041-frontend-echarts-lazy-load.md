# 0041 前端 ECharts 动态加载

## 当前已有内容

- `0040` 已增强玩家端空状态和交易前置条件提示。
- 构建长期存在 ECharts 大 chunk 警告。
- 管理台图表需要 ECharts，但玩家端首屏不需要图表库。

## 本次完成内容

- 修改 `frontend/src/components/AdminWorkspace.vue`：
  - 移除静态 `import * as echarts from 'echarts'`。
  - 改为在管理台图表首次渲染时动态 `import('echarts')`。
  - 复用同一个 `echartsModulePromise`，避免重复加载。
- 玩家端首屏不再主动加载 ECharts 代码。

## 构建验证

- 已执行前端构建：`npm.cmd run build`。
- 构建通过。
- 构建结果从单个大 JS 拆为两个 JS chunk：
  - 主入口：约 `115.25 kB`，gzip 约 `42.66 kB`。
  - ECharts 动态 chunk：约 `1,042.31 kB`，gzip 约 `346.46 kB`。
- 大 chunk 警告仍存在，但已转移到按需加载的 ECharts chunk，不再阻塞玩家端首屏。

## 下次建议

- 继续优化构建输出：给动态 ECharts chunk 命名，便于后续分析。
- 响应式人工检查 390px、768px、1366px 三档布局。
- 继续拆管理台子面板，降低单组件复杂度。
