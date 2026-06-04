# 0012 - 经营查询与栏位扩建接口

## 当前已有

当前项目已经具备：

- Spring Boot 后端工程。
- PostgreSQL 独立业务库 `farm_exchange`。
- Flyway 数据库迁移。
- 用户注册。
- 注册时初始化 10,000 金币。
- 注册时初始化土地和牧栏。
- 交易密码设置接口。
- 本地 Git 管理。

## 本次完成

本次实现经营系统第一批接口：

- 查询玩家经营摘要。
- 查询玩家农场土地。
- 查询玩家牧场栏位。
- 扩建农场土地。
- 扩建牧场栏位。

新增后端模块：

- `backend/src/main/java/com/farm/exchange/farm/FarmController.java`
- `backend/src/main/java/com/farm/exchange/farm/FarmService.java`
- `backend/src/main/java/com/farm/exchange/farm/SlotType.java`
- `backend/src/main/java/com/farm/exchange/farm/PlayerSummaryResponse.java`
- `backend/src/main/java/com/farm/exchange/farm/SlotResponse.java`
- `backend/src/main/java/com/farm/exchange/farm/SlotListResponse.java`
- `backend/src/main/java/com/farm/exchange/farm/ExpansionResponse.java`
- `backend/src/main/java/com/farm/exchange/farm/ExpansionConfig.java`

## 接口说明

### 查询经营摘要

```text
GET /api/users/{userId}/summary
```

返回内容：

- 金币余额。
- 冻结余额。
- 当前土地数量。
- 当前牧栏数量。
- 下一次土地扩建费用。
- 下一次牧栏扩建费用。
- 是否已设置交易密码。

### 查询农场土地

```text
GET /api/users/{userId}/farm/plots
```

返回内容：

- 当前土地数量。
- 最大土地数量。
- 下一次扩建费用。
- 土地列表。

### 查询牧场栏位

```text
GET /api/users/{userId}/ranch/slots
```

返回内容：

- 当前牧栏数量。
- 最大牧栏数量。
- 下一次扩建费用。
- 牧栏列表。

### 扩建土地

```text
POST /api/users/{userId}/farm/expand
```

规则：

- 最大 16 块土地。
- 扩建费用由服务端按 `expansion_config` 计算。
- 首次土地扩建费用当前为 1,000。
- 每次扩建后，下次费用翻倍。
- 扣除金币和新增土地在同一事务中完成。
- 写入 `asset_ledger`，原因是 `EXPAND_FARM`。

### 扩建牧栏

```text
POST /api/users/{userId}/ranch/expand
```

规则：

- 最大 16 个牧栏。
- 扩建费用由服务端计算。
- 首次牧栏扩建费用当前为 2,000。
- 每次扩建后，下次费用翻倍。
- 扣除金币和新增牧栏在同一事务中完成。
- 写入 `asset_ledger`，原因是 `EXPAND_RANCH`。

## 并发与一致性

扩建接口当前做了以下保护：

- 校验用户必须是 `ACTIVE`。
- 查询钱包时使用 `for update` 锁定钱包行。
- 钱包更新带 `version` 乐观锁条件。
- 栏位数量达到最大值时拒绝扩建。
- 扩建费用不接收前端传值，全部服务端计算。
- 扣款、新增栏位、资产流水在同一事务内完成。

## 验证结果

已执行：

```powershell
& 'D:\maven\apache-maven-3.6.3\bin\mvn.cmd' -s maven-settings.xml -q test
```

结果：

```text
测试通过
```

测试覆盖：

- 注册后摘要显示余额 10,000。
- 注册后土地数量为 4。
- 注册后牧栏数量为 2。
- 土地首次扩建费用为 1,000。
- 牧栏首次扩建费用为 2,000。
- 扩建土地后余额变为 9,000，土地数量变为 5，下次费用变为 2,000。
- 扩建牧栏后余额变为 7,000，牧栏数量变为 3，下次费用变为 4,000。
- 扩建操作写入资产流水。

## 当前风险

当前仍未实现：

- 登录态和当前用户识别。
- 扩建接口的交易密码校验。
- 达到 16 个栏位的完整边界测试。
- 并发扩建压力测试。
- 前端页面接入真实接口。

后续登录体系完成后，应将当前路径中的 `userId` 改为从认证上下文读取，避免玩家操作其他用户资源。

## 下次待做

建议下一阶段进入 `0013`：

- 实现商品种子和动物配置查询。
- 实现玩家库存查询。
- 实现种植接口。
- 实现养殖接口。
- 实现收获接口。

种植和收获必须重点防止：

- 同一土地重复播种。
- 未成熟提前收获。
- 同一成长实例重复收获。
- 库存扣减和产出增加不一致。

