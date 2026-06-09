# 0063 审计日志筛选分页、玩家活动后端分页、资产类型筛选

## 本次完成

- 管理端审计日志接口升级为筛选 + 分页：
  - `GET /api/admin/audit-logs`
  - 支持参数：
    - `action=ALL|UPDATE_TAX_CONFIG|ISSUE_BULK_TOKEN`
    - `targetType=ALL|TAX_CONFIG|APP_USER`
    - `page`
    - `pageSize`
  - 返回结构：
    - `records`
    - `total`
    - `page`
    - `pageSize`
    - `hasNext`

- 玩家活动记录改为后端分页，避免只在前端本地筛：
  - `GET /api/me/trades`
    - 支持 `source`、`status`、`page`、`pageSize`
  - `GET /api/me/ledger`
    - 支持 `assetType`、`direction`、`page`、`pageSize`
  - 两个接口都返回分页结果对象，不再返回纯数组。

- 管理端玩家资产支持物品类型筛选：
  - `GET /api/admin/users/{targetUserId}/assets`
  - 支持 `itemType=ALL|SEED|ANIMAL|FEED|HARVEST|TOKEN|CONSUMABLE`

## 前端补齐

- 管理端：
  - 审计日志面板补动作/目标筛选、分页、总数展示
  - 玩家资产面板补物品类型筛选
- 玩家端：
  - 活动记录页改为前后端联动筛选
  - 交易记录和资产流水分别支持独立翻页

## 主要文件

- 后端
  - `backend/src/main/java/com/farm/exchange/admin/AdminController.java`
  - `backend/src/main/java/com/farm/exchange/admin/AdminService.java`
  - `backend/src/main/java/com/farm/exchange/admin/AdminAuditLogQueryResponse.java`
  - `backend/src/main/java/com/farm/exchange/activity/PlayerActivityService.java`
  - `backend/src/main/java/com/farm/exchange/activity/PlayerTradeQueryResponse.java`
  - `backend/src/main/java/com/farm/exchange/activity/PlayerLedgerQueryResponse.java`
  - `backend/src/main/java/com/farm/exchange/me/MeController.java`
  - `backend/src/test/java/com/farm/exchange/FarmExchangeApplicationTests.java`

- 前端
  - `frontend/src/stores/adminStore.js`
  - `frontend/src/stores/playerStore.js`
  - `frontend/src/components/admin/AuditLogPanel.vue`
  - `frontend/src/components/admin/PlayerAssetsPanel.vue`
  - `frontend/src/components/player/ActivityHistoryPanel.vue`
  - `frontend/src/components/AdminWorkspace.vue`
  - `frontend/src/components/GamePlayerWorkspace.vue`

## 验证

- 后端：
  - `D:\maven\apache-maven-3.6.3\bin\mvn.cmd -q -s maven-settings.xml -Dtest=FarmExchangeApplicationTests test`
- 前端单测：
  - `npm.cmd run test`
- 前端构建：
  - `npm.cmd run build`

## 说明

- 审计日志表是共享测试库里的持久数据，因此测试不再假定总数固定，只验证当前筛选结果第一页内容和分页行为。
- Vite 大包体告警仍存在，本轮未新增构建失败。
