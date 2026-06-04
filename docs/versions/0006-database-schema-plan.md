# 0006 - PostgreSQL 数据库落地设计

## 当前已有

当前已确定：

- 页面原型采用 `0004` 版本方向。
- 功能边界采用 `0005` 的 MVP 分析。
- 本地 PostgreSQL 配置：
  - Host：localhost
  - Port：5432
  - User：postgres
  - Database：postgres
  - Password：本地开发密码，不写入项目文件。

## 本次完成

本次新增 PostgreSQL 首版建表脚本：

- `database/migrations/001_init_schema.sql`

该脚本覆盖 MVP 所需核心表：

- 用户。
- 钱包。
- 交易密码状态。
- 商品配置。
- 玩家库存。
- 土地。
- 牧栏。
- 成长实例。
- 扩建配置。
- 税率配置。
- 资产流水。
- 税收流水。
- 交易站成交。
- 私下交易报价单。
- 大宗交易令牌。
- 行情快照。
- 管理员审计日志。

## 数据库执行说明

当前环境没有找到 `psql` 命令，因此本次没有直接连接本地数据库执行脚本。

可以使用 Navicat、pgAdmin、DataGrip 或其他数据库工具连接：

```text
Host: localhost
Port: 5432
User: postgres
Database: postgres
```

然后执行：

```text
database/migrations/001_init_schema.sql
```

如果后续配置好 `psql`，也可以执行：

```bash
psql -h localhost -p 5432 -U postgres -d postgres -f database/migrations/001_init_schema.sql
```

## 设计原则

### 金额使用整数

所有金额字段都使用 `BIGINT`，表示最小金币单位，不使用浮点数。

原因：

- 避免浮点误差。
- 税费计算更稳定。
- 资产流水更容易审计。

### 资产必须有流水

所有金币、库存、令牌变化都应该写入 `asset_ledger`。

例如：

- 初始发放 10,000。
- 购买种子。
- 扩建栏位。
- 收获增加库存。
- 交易扣款。
- 交易收入。
- 税费扣除。

### Redis 不能替代数据库事务

Redis 后续可以用于：

- 限流。
- 幂等键。
- 行情缓存。
- 排行榜。
- 在线状态。

但金币、库存、订单和税收最终账本必须以 PostgreSQL 为准。

## 核心表说明

### app_users

保存玩家和管理员账号。

关键字段：

- `password_hash`：登录密码哈希。
- `trade_password_hash`：交易密码哈希。
- `trade_password_failed_count`：交易密码错误次数。
- `trade_password_locked_until`：交易密码锁定时间。
- `role`：玩家或管理员。
- `status`：正常、冻结、封禁。

交易密码不能明文保存。

### wallets

保存玩家金币余额。

规则：

- 新钱包默认 `balance = 10000`。
- `locked_balance` 用于后续订单或交易冻结。
- 余额不能为负数。
- 钱包更新需要事务和版本控制。

### items

保存商品定义。

支持类型：

- `SEED`：种子。
- `ANIMAL`：动物或幼崽。
- `FEED`：饲料。
- `HARVEST`：收获物。
- `TOKEN`：令牌。
- `CONSUMABLE`：消耗道具。

### player_inventory

保存玩家库存。

规则：

- `available_quantity` 表示可用库存。
- `locked_quantity` 表示已冻结库存。
- 挂单或私下交易创建后，应先冻结库存。
- 库存不能为负数。

### expansion_config

保存土地和牧栏扩建配置。

当前默认：

- 农场初始栏位 4 个。
- 牧场初始栏位 2 个。
- 最大栏位都是 16 个。
- 农场第一次扩建费用 1,000。
- 牧场第一次扩建费用 2,000。
- 下一次扩建费用翻倍。

### farm_plots 与 ranch_slots

分别保存玩家土地和牧栏。

规则：

- `slot_index` 范围 1 到 16。
- 每个玩家同一类型栏位编号唯一。
- 状态包含空闲、成长中、可收获、锁定。

### growth_instances

保存种植和养殖成长实例。

好处：

- 土地和牧栏本身只表示栏位。
- 成长实例记录具体种了什么、养了什么、什么时候成熟、是否已收获。
- 可以防止重复收获和并发状态混乱。

### tax_config 与 tax_records

`tax_config` 保存当前税率。

默认：

- 交易站 3%。
- 私下交易 5%。
- 大宗交易暂按 3%。

`tax_records` 保存每次收税记录，便于后台审计。

### market_trades

MVP 交易站建议先做即时买卖。

`market_trades` 记录每次交易：

- 用户。
- 商品。
- 买入或卖出。
- 数量。
- 单价。
- 总金额。
- 税费。
- 实际金额。

### private_trade_offers

保存私下交易报价单。

状态：

- `WAIT_ACCEPT`：等待对方接受。
- `SETTLING`：结算中。
- `COMPLETED`：已完成。
- `CANCELLED`：已取消。
- `EXPIRED`：已过期。
- `FAILED`：失败。

私下交易确认时需要双方交易密码参与校验，但交易密码本身不记录在报价单中。

### bulk_trade_tokens

保存大宗交易令牌。

当前支持：

- 单笔额度。
- 总额度。
- 使用次数。
- 有效期。
- 状态。

### admin_audit_logs

保存后台管理员操作审计。

后续任何后台修改都应该写入：

- 修改人。
- 操作。
- 修改对象。
- 修改前数据。
- 修改后数据。
- 修改原因。

## 后续开发注意事项

### 新用户初始化

注册成功后应在同一事务内完成：

```text
创建 app_users
创建 wallets，默认 10,000
创建初始 farm_plots
创建初始 ranch_slots
写入初始金币资产流水
```

### 扩建事务

扩建时不能相信前端传入价格。

后端应执行：

```text
读取 expansion_config
统计当前栏位数
校验当前栏位数 < 16
计算下一次扩建费用
锁定钱包
校验余额
扣款
新增栏位
写资产流水
提交事务
```

### 私下交易事务

创建报价单时：

```text
校验发起方交易密码
校验库存
冻结库存
创建报价单
写资产冻结流水或库存流水
```

接受报价单时：

```text
校验接受方交易密码
锁定报价单
锁定双方钱包和库存
校验报价单仍有效
校验金币足够
计算税费
转移金币和库存
写交易、税收、资产流水
完成报价单
```

## 风险与漏洞

需要重点测试：

- 同一用户并发扩建导致栏位超过 16。
- 同一土地并发收获导致重复增加库存。
- 同一私下交易被重复接受。
- 余额扣除成功但交易失败。
- 库存冻结后报价单过期未释放。
- 税率修改后旧交易和新交易税率混用。
- 管理员改配置未写审计。

## 下次待做

建议下一阶段进入 `0007`：

- 初始化 Spring Boot 后端工程。
- 配置 PostgreSQL 与 Redis。
- 引入数据库迁移工具 Flyway 或 Liquibase。
- 先实现用户注册、钱包初始化、交易密码设置。

推荐优先使用 Flyway，因为当前已经开始按 `database/migrations` 方式管理 SQL。

