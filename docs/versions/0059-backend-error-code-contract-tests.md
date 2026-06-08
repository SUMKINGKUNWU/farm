# 0059 后端错误码契约测试

## 变更目标

- 为前端已经依赖的关键业务错误补一层后端接口契约测试。
- 固定真实接口返回的 `HTTP status`、`code`、`path`，避免后端后续改动让前端稳定识别失效。

## 覆盖范围

- `INSUFFICIENT_BALANCE`
- `INSUFFICIENT_INVENTORY`
- `STATE_CONFLICT`
- `BULK_TOKEN_REQUIRED`
- `BULK_TOKEN_INVALID`
- `BULK_TOKEN_EXPIRED`
- `BULK_TOKEN_LIMIT_EXCEEDED`

## 实现说明

- 在 `backend/src/test/java/com/farm/exchange/FarmExchangeApplicationTests.java` 新增
  `businessErrorContractsStayStableForFrontendRecognition`。
- 使用真实业务接口构造错误场景，而不是直接 mock service：
  - 商店超额购买触发余额不足
  - 交易站无库存卖出触发库存不足
  - 未成熟收获触发状态冲突
  - 大宗买入分别覆盖缺令牌、无效令牌、过期令牌、超限令牌

## 验证方式

- 在 `backend` 目录执行：
  - `D:\maven\apache-maven-3.6.3\bin\mvn.cmd -q -Dtest=FarmExchangeApplicationTests test`

## 结果

- 前端错误码回归测试与后端接口契约测试形成闭环。
- 后端如果未来改动关键业务错误码或状态码，测试会直接暴露不兼容变更。
