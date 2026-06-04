# 0010 - 用户注册、初始化资产与交易密码

## 当前已有

当前项目已经具备：

- 静态页面原型。
- Spring Boot 后端骨架。
- PostgreSQL 独立业务库 `farm_exchange`。
- Flyway 首版迁移。
- 基础健康检查接口。

## 本次完成

本次实现第一批真实后端业务接口：

- 用户注册接口。
- 注册时初始化钱包 10,000。
- 注册时初始化农场土地。
- 注册时初始化牧场栏位。
- 注册时写入初始金币资产流水。
- 设置交易密码接口。
- 交易密码使用 BCrypt 哈希保存。
- 基础异常处理。
- 请求参数校验。
- 接口级测试。

## 新增或修改文件

后端新增：

- `backend/src/main/java/com/farm/exchange/config/SecurityBeans.java`
- `backend/src/main/java/com/farm/exchange/common/ApiException.java`
- `backend/src/main/java/com/farm/exchange/common/ApiExceptionHandler.java`
- `backend/src/main/java/com/farm/exchange/user/AuthController.java`
- `backend/src/main/java/com/farm/exchange/user/RegisterRequest.java`
- `backend/src/main/java/com/farm/exchange/user/RegisterResponse.java`
- `backend/src/main/java/com/farm/exchange/user/SetTradePasswordRequest.java`
- `backend/src/main/java/com/farm/exchange/user/TradePasswordResponse.java`
- `backend/src/main/java/com/farm/exchange/user/UserService.java`

测试修改：

- `backend/src/test/java/com/farm/exchange/FarmExchangeApplicationTests.java`

依赖修改：

- `backend/pom.xml` 增加 `spring-security-crypto`。

## 接口说明

### 用户注册

```text
POST /api/auth/register
```

请求：

```json
{
  "username": "tester001",
  "nickname": "测试农夫",
  "password": "123456"
}
```

响应：

```json
{
  "userId": "...",
  "username": "tester001",
  "nickname": "测试农夫",
  "balance": 10000,
  "farmSlots": 4,
  "ranchSlots": 2,
  "tradePasswordSet": false
}
```

注册事务内完成：

- 创建用户。
- 创建钱包，余额 10,000。
- 根据 `expansion_config` 创建初始土地。
- 根据 `expansion_config` 创建初始牧栏。
- 写入 `asset_ledger` 初始金币流水。

### 设置交易密码

```text
POST /api/users/{userId}/trade-password
```

请求：

```json
{
  "tradePassword": "654321"
}
```

响应：

```json
{
  "userId": "...",
  "tradePasswordSet": true
}
```

规则：

- 交易密码必须是 6 位数字。
- 交易密码不明文保存。
- 当前使用 BCrypt 哈希。
- 重设密码会清空失败次数和锁定时间。

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

- Spring Boot 上下文启动。
- Flyway 连接 `farm_exchange`。
- 注册接口返回 10,000 初始余额。
- 注册接口创建 4 块土地。
- 注册接口创建 2 个牧栏。
- 注册接口写入初始资产流水。
- 交易密码接口可设置成功。
- 交易密码不以明文保存。

## 当前风险

当前仍是早期实现：

- 尚未实现登录和认证 token。
- 设置交易密码接口目前通过 URL 中的 `userId` 指定用户，后续登录体系完成后应改为当前登录用户。
- 尚未实现交易密码校验接口。
- 尚未实现交易密码错误次数限制。
- 尚未实现用户详情查询接口。
- 测试会向真实 `farm_exchange` 写入测试用户，后续应引入测试库或测试事务清理策略。

## 下次待做

建议下一阶段进入 `0011`，实现经营系统基础接口：

- 查询我的农场土地。
- 查询我的牧场栏位。
- 查询钱包和基础用户状态。
- 扩建土地。
- 扩建牧栏。

扩建必须重点保证：

- 最大 16 个栏位。
- 扩建费用服务端计算。
- 每次扩建费用翻倍。
- 钱包扣款和新增栏位在同一事务内完成。
- 写入资产流水。

