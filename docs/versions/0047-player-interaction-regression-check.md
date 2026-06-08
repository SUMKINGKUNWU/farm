# 0047 玩家端交互回归检查

## 当前已有

- 玩家端组件已拆分并改为事件通信。
- 玩家端子组件已改用只读数据入参，不再直接依赖完整 `player` store。
- 前端生产构建持续通过，ECharts 图表仍保持动态加载。

## 本次完成

- 新增可复用的玩家端交互回归脚本：
  - `frontend/scripts/player_regression.py`
- 脚本覆盖目标：
  - 玩家端初始页面渲染。
  - 底部页签切换到商店、交易站、私下交易。
  - 商品信息浮层打开与 Esc 关闭。
  - 交易确认浮层打开与 Esc 关闭。
  - 行情刷新接口调用。
  - 交易密码保存接口调用。
- 脚本内置 mock API 响应，后续执行时不依赖当前后端服务和数据库状态。

## 本次验证

- 已执行 `npm.cmd run build`，前端生产构建通过。
- 已静态检查 `frontend/src/components/player`，确认子组件内无 `player.` 直接访问。
- 已静态检查父组件关键事件绑定，确认仍包含：
  - `@start-production`
  - `@submit-market`
  - `@refresh-quote`
  - `@change-tab`
  - `@save-trade-password`
- 已静态检查玩家端子组件均定义 `defineEmits`。

## 未完成原因

- 当前系统 Python 未安装 Playwright，无法执行 `frontend/scripts/player_regression.py`。
- 当前 Node 环境也未安装 Playwright。
- 内置浏览器访问 `http://127.0.0.1:8010` 被安全策略拒绝，不能绕过该限制。

## 下次建议

- 若允许安装测试依赖，可安装 Python Playwright 后执行该回归脚本。
- 也可以先继续做管理台组件解耦，将管理台子组件从直接依赖完整 `admin` store 改为只读入参和事件通信。
