# 0013 - 商品库存、种植养殖与收获闭环

## 当前已有

当前项目已经具备：

- 用户注册。
- 初始钱包、土地、牧栏。
- 交易密码设置。
- 经营摘要查询。
- 土地和牧栏扩建。
- Flyway 数据库迁移。
- 本地 Git 管理。

## 本次完成

本次实现经营核心闭环：

- 初始化 MVP 商品数据。
- 查询商品列表。
- 查询玩家库存。
- 农场土地种植。
- 牧场栏位养殖。
- 成熟后收获产出。
- 防止提前收获。
- 防止重复收获。
- 生产投入和收获产出写入资产流水。

## 数据迁移

新增 Flyway 迁移：

```text
backend/src/main/resources/db/migration/V2__seed_mvp_items.sql
```

初始化商品：

- `WHEAT`：小麦。
- `CORN`：玉米。
- `EGG`：鸡蛋。
- `MILK`：牛奶。
- `WHEAT_SEED`：小麦种子，30 分钟成熟，产出 12 小麦。
- `CORN_SEED`：玉米种子，60 分钟成熟，产出 10 玉米。
- `CHICKEN`：鸡苗，2 小时成熟，产出 20 鸡蛋。
- `COW`：奶牛，6 小时成熟，产出 30 牛奶。
- `BULK_TOKEN`：大宗交易令牌。

当前 `farm_exchange` Flyway 版本：

```text
v1 init schema
v2 seed mvp items
```

## 新增接口

### 查询商品

```text
GET /api/items
GET /api/items?itemType=SEED
```

### 查询库存

```text
GET /api/users/{userId}/inventory
```

### 种植

```text
POST /api/users/{userId}/farm/plots/{plotId}/plant?itemCode=WHEAT_SEED
```

规则：

- 用户必须可用。
- 土地必须属于该用户。
- 土地必须是 `EMPTY`。
- 商品必须是 `SEED`。
- 玩家必须有足够种子库存。
- 扣减种子库存。
- 创建成长实例。
- 土地状态改为 `GROWING`。
- 写入资产流水 `PLANT_INPUT`。

### 养殖

```text
POST /api/users/{userId}/ranch/slots/{slotId}/raise?itemCode=CHICKEN
```

规则：

- 用户必须可用。
- 牧栏必须属于该用户。
- 牧栏必须是 `EMPTY`。
- 商品必须是 `ANIMAL`。
- 玩家必须有足够动物库存。
- 扣减动物库存。
- 创建成长实例。
- 牧栏状态改为 `GROWING`。
- 写入资产流水 `RAISE_INPUT`。

### 收获

```text
POST /api/users/{userId}/growth/{growthId}/harvest
```

规则：

- 成长实例必须属于该用户。
- 成长实例状态必须是 `GROWING` 或 `READY`。
- 当前时间必须达到 `ready_at`。
- 成功后成长实例改为 `HARVESTED`。
- 对应土地或牧栏恢复 `EMPTY`。
- 增加产出物库存。
- 写入资产流水 `HARVEST_OUTPUT`。

## 并发与一致性

本次实现了以下保护：

- 启动生产时对土地或牧栏 `for update` 加锁。
- 收获时对成长实例 `for update` 加锁。
- 库存扣减使用 `available_quantity >= 1` 条件。
- 收获增加库存使用 PostgreSQL `on conflict` 原子累加。
- 数据库约束保证 `ready_at > started_at`。
- 已收获的成长实例不能重复收获。

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

- Flyway 自动执行 v2 商品迁移。
- 测试用户获得种子和动物库存。
- 小麦种子可以种到农场土地。
- 鸡苗可以放入牧场栏位。
- 投入库存会扣减。
- 未成熟时收获返回 409。
- 成熟后收获增加小麦和鸡蛋库存。
- 收获后土地和牧栏恢复空闲。
- 重复收获返回 409。
- 收获产出写入资产流水。

## 当前风险

当前仍未实现：

- 商店购买种子或动物。
- 成长实例自动从 `GROWING` 变为 `READY` 的定时任务。
- 前端倒计时接入。
- 交易密码校验。
- 真实登录态。
- 批量收获。
- 生产取消或异常补偿。

当前收获接口直接根据 `ready_at` 判断是否成熟，即使状态仍是 `GROWING`，只要时间达到也允许收获。这适合 MVP，后续可加定时任务或查询时动态展示 `READY`。

## 下次待做

建议下一阶段进入 `0014`：

- 实现商店购买种子和动物。
- 购买时扣金币、增加库存、写资产流水。
- 接入交易密码要求。
- 增加库存不足和金币不足的测试。

