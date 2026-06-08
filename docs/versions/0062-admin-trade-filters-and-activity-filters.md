# 0062 管理端交易筛选分页与玩家记录筛选

## 本次补齐

- 管理端交易记录接口升级为可筛选、可分页：
  - `GET /api/admin/users/{targetUserId}/trades`
  - 支持查询参数：
    - `source=ALL|MARKET|PRIVATE`
    - `status=ALL|COMPLETED|WAIT_ACCEPT|SETTLING|CANCELLED|EXPIRED|FAILED`
    - `page`
    - `pageSize`
  - 返回结构调整为：
    - `records`
    - `total`
    - `page`
    - `pageSize`
    - `hasNext`
- 管理端前端控制台补齐交易筛选和分页：
  - 来源筛选
  - 状态筛选
  - 每页条数切换
  - 上一页 / 下一页翻页
  - 总条数和页码显示
- 管理端侧边导航补上审计入口 `#audits`。
- 玩家“交易与流水”页补齐前端筛选：
  - 交易来源筛选
  - 交易状态筛选
  - 流水资产类型筛选
  - 流水收支方向筛选

## 主要文件

- 后端
  - `backend/src/main/java/com/farm/exchange/admin/AdminController.java`
  - `backend/src/main/java/com/farm/exchange/admin/AdminService.java`
  - `backend/src/main/java/com/farm/exchange/admin/AdminTradeQueryResponse.java`
  - `backend/src/test/java/com/farm/exchange/FarmExchangeApplicationTests.java`
- 前端
  - `frontend/src/stores/adminStore.js`
  - `frontend/src/components/admin/TradeRecordsPanel.vue`
  - `frontend/src/components/player/ActivityHistoryPanel.vue`
  - `frontend/src/components/AdminWorkspace.vue`
  - `frontend/src/App.vue`
  - `frontend/src/styles.css`

## 验证

- 前端单测：
  - `npm.cmd run test`
- 前端构建：
  - `npm.cmd run build`
- 后端测试：
  - `D:\maven\apache-maven-3.6.3\bin\mvn.cmd -q -s maven-settings.xml -Dtest=FarmExchangeApplicationTests test`

## 说明

- Vite 仍有大包体告警，但本轮未新增构建失败或功能回退。
- 这次把几个核心文件顺手收敛成 UTF-8，后续继续补功能时会比在乱码文件上打小补丁稳定得多。
