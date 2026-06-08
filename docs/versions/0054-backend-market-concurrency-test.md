# 0054 后端交易站并发卖出测试

## 当前已有

- 后端已具备交易站买入、卖出、税费、流水、价格快照和大宗令牌测试。
- 私下交易已补充并发成交测试，验证同一个私下交易单只能结算一次。
- Maven 测试需要使用 `backend/maven-settings.xml`，避免读取不可访问的全局本地仓库。

## 本次完成

- 在 `FarmExchangeApplicationTests` 新增 `concurrentMarketSellOnlyConsumesAvailableInventoryOnce`。
- 模拟同一用户持有 10 个 `WHEAT`，两条并发请求同时卖出 10 个。
- 断言并发结果只能有 1 笔成功、1 笔返回 `409 Conflict`。
- 断言最终库存扣为 0，交易站卖出记录、市场税收记录都只产生 1 条。
- 断言 `MARKET_SELL` 资产流水只产生金币和物品两条，余额只按一笔卖出结算。

## 验证方式

```powershell
D:\maven\apache-maven-3.6.3\bin\mvn.cmd -s maven-settings.xml test
```

## 下次建议

- 继续补交易站并发买入测试，重点验证同一钱包余额在并发扣款时不会重复透支。
- 后续可补价格快照并发更新测试，确认成交量驱动价格在高并发下不会出现异常跳变。
