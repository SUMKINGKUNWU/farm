# 0064 审计时间范围筛选与活动原因筛选

## 本次完成

- 管理端审计日志新增时间范围筛选：
  - `GET /api/admin/audit-logs`
  - 新增参数：
    - `from=YYYY-MM-DD`
    - `to=YYYY-MM-DD`
  - 语义：
    - `from` 为当天 00:00 起
    - `to` 为当天结束，服务端按次日 00:00 前处理

- 玩家活动记录新增原因筛选：
  - `GET /api/me/trades`
    - 新增 `reason`
  - `GET /api/me/ledger`
    - 新增 `reason`

- 玩家活动筛选选项接口：
  - `GET /api/me/trades/filter-options`
  - `GET /api/me/ledger/filter-options`

## 前端补齐

- 管理端审计面板：
  - 开始日期
  - 结束日期
  - 页码跳转

- 玩家活动面板：
  - 交易原因筛选
  - 流水原因筛选
  - 交易页码跳转
  - 流水页码跳转

## 主要文件

- 后端
  - `backend/src/main/java/com/farm/exchange/admin/AdminController.java`
  - `backend/src/main/java/com/farm/exchange/admin/AdminService.java`
  - `backend/src/main/java/com/farm/exchange/activity/PlayerActivityService.java`
  - `backend/src/main/java/com/farm/exchange/activity/PlayerTradeFilterOptionResponse.java`
  - `backend/src/main/java/com/farm/exchange/activity/PlayerLedgerFilterOptionResponse.java`
  - `backend/src/main/java/com/farm/exchange/me/MeController.java`
  - `backend/src/test/java/com/farm/exchange/FarmExchangeApplicationTests.java`

- 前端
  - `frontend/src/stores/adminStore.js`
  - `frontend/src/stores/playerStore.js`
  - `frontend/src/components/admin/AuditLogPanel.vue`
  - `frontend/src/components/player/ActivityHistoryPanel.vue`
  - `frontend/src/components/GamePlayerWorkspace.vue`
  - `frontend/src/styles.css`

## 验证

- 后端：
  - `D:\maven\apache-maven-3.6.3\bin\mvn.cmd -q -s maven-settings.xml -Dtest=FarmExchangeApplicationTests test`
- 前端单测：
  - `npm.cmd run test`
- 前端构建：
  - `npm.cmd run build`

## 说明

- 审计日期筛选按自然日处理，适合管理台做日常追溯。
- 玩家流水原因选项来自当前玩家已有流水数据；交易原因选项由服务端提供固定枚举。
