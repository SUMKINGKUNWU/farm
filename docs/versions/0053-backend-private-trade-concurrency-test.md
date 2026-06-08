# 0053 私下交易并发成交防护测试

## 当前已有

- 后端已有较完整的 Spring Boot 集成测试，覆盖注册、登录、初始资产、扩建、生产、收获、商店购买、交易站买卖、大宗令牌、私下交易和管理台接口。
- 私下交易服务使用 `for update` 锁定报价单和钱包，并通过状态校验防止重复接受报价。
- 大宗令牌服务使用 `for update` 锁定令牌，避免并发重复消费。

## 本次完成

- 在 `FarmExchangeApplicationTests` 中新增 `concurrentPrivateTradeAcceptOnlySettlesOnce`。
- 测试流程：
  - 创建卖方和买方。
  - 双方设置交易密码。
  - 给卖方发放小麦库存。
  - 卖方创建私下交易报价并冻结库存。
  - 使用两个线程同时接受同一报价。
  - 断言两个请求中只能有一个成功，另一个必须返回 `409 Conflict`。
  - 断言报价最终只有一条 `COMPLETED`。
  - 断言私下交易税费只记录一次。
  - 断言卖方库存只扣减一次，锁定库存归零。
  - 断言买方库存只增加一次。

## 验证结果

- 直接执行 `mvn test` 会受全局 Maven 本地仓库 `E:\Maven_m2\repository` 不可访问影响失败。
- 已使用后端项目内 `maven-settings.xml` 执行：
  - `D:\maven\apache-maven-3.6.3\bin\mvn.cmd -s maven-settings.xml test`
- 后端测试通过：
  - Tests run: 14
  - Failures: 0
  - Errors: 0
  - Skipped: 0

## 下次建议

- 继续补交易站并发测试，重点覆盖同一用户并发买入或卖出时钱包版本、库存版本和税费记录不会重复或错扣。
- 可考虑把大型 `FarmExchangeApplicationTests` 按领域拆分为多个测试类，降低单文件维护成本。
