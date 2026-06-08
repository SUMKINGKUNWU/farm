# 0055 后端交易站并发买入测试

## 当前已有

- 交易站已覆盖单线程买入、卖出、税费、流水、价格快照和大宗令牌。
- 已补充交易站并发卖出测试，验证同一份库存不会被并发卖单重复扣减。

## 本次完成

- 在 `FarmExchangeApplicationTests` 新增 `concurrentMarketBuyOnlyDebitsAffordableTradeOnce`。
- 模拟同一用户初始资金 10000，同时发起两笔 `WHEAT` 买入请求。
- 单笔买入 200 个 `WHEAT`，价格为 25，市场税率为 3%，单笔总成本为 5150。
- 两笔并发请求总成本超过钱包余额，因此断言只能 1 笔成功、1 笔返回 `409 Conflict`。
- 断言最终钱包余额为 4850，库存为 200，交易记录、税务记录和 `MARKET_BUY` 资产流水都只按一笔成功交易落库。

## 验证方式

```powershell
D:\maven\apache-maven-3.6.3\bin\mvn.cmd -s maven-settings.xml test
```

## 下次建议

- 继续补价格快照和行情价格更新的并发测试，验证高并发成交不会造成价格异常跳变。
- 后续可补异常错误码一致性测试，确保前端能稳定识别余额不足、库存不足和状态冲突。
