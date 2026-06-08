# 0060 审计、活动记录与过期一致性补全

## 变更目标

- 补全管理端审计日志查看能力。
- 补全玩家侧交易历史与资产流水查看能力。
- 修正私下交易报价过期后列表状态与库存释放不一致的问题。

## 本次实现

### 管理端

- 新增 `GET /api/admin/audit-logs`
- 返回最近 100 条管理员审计日志，包含：
  - 管理员用户
  - 动作类型
  - 目标类型 / 目标 ID
  - 原因
  - 创建时间
- 前端新增审计日志面板，并在管理端登录后自动刷新。

### 玩家侧

- 新增 `GET /api/me/trades`
- 新增 `GET /api/me/ledger`
- 玩家前端新增“交易与流水”视图，展示：
  - 最近交易记录
  - 最近资产流水
  - 对手方信息
  - 资产变动方向

### 私下交易

- `PrivateTradeService.offers()` 在返回列表前主动规范化已过期的 `WAIT_ACCEPT` 报价。
- 过期时自动：
  - 更新报价状态为 `EXPIRED`
  - 释放卖方锁定库存
  - 写入 `PRIVATE_TRADE_EXPIRE_RELEASE` 流水

## 测试补充

- 管理端审计日志接口回归测试
- 玩家 `me/trades` 与 `me/ledger` 接口回归测试
- 私下交易过期列表一致性测试

## 验证方式

- `frontend`:
  - `npm.cmd run test`
  - `npm.cmd run build`
- `backend`:
  - `D:\maven\apache-maven-3.6.3\bin\mvn.cmd -q -s maven-settings.xml -Dtest=FarmExchangeApplicationTests test`
