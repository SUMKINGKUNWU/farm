# 0009 - 数据库命名修正

## 当前已有

上一阶段已经确认：

- Spring Boot 服务可以启动。
- Flyway 已成功执行迁移。
- `/api/health` 健康检查可用。
- 17 张业务表已创建。

## 本次发现

用户截图显示，17 张业务表创建在：

```text
postgres.public
```

这说明 Flyway 没有失败，但业务表落在了 PostgreSQL 默认数据库 `postgres` 中。

从项目长期维护角度看，这不够理想。默认 `postgres` 数据库更适合作为系统管理和临时连接使用，不建议承载具体业务项目表。

## 本次完成

本次新增独立业务数据库：

```text
farm_exchange
```

并将后端默认数据库连接从：

```text
jdbc:postgresql://localhost:5432/postgres
```

调整为：

```text
jdbc:postgresql://localhost:5432/farm_exchange
```

修改文件：

- `backend/src/main/resources/application.yml`

## 保留说明

本次没有删除 `postgres.public` 中已经创建的 17 张表。

原因：

- 避免误删数据。
- 当前阶段仍在开发初期，保留旧表不会影响新库迁移。
- 等确认 `farm_exchange` 迁移成功后，可以再决定是否清理旧库中的业务表。

## 后续验证

下一次启动后端时，Flyway 应该会在：

```text
farm_exchange.public
```

中重新创建业务表和 `flyway_schema_history`。

可验证：

```sql
select table_name
from information_schema.tables
where table_schema = 'public'
order by table_name;
```

连接数据库：

```text
Database: farm_exchange
Host: localhost
Port: 5432
User: postgres
```

## 下次待做

建议下一步：

- 启动后端，让 Flyway 在 `farm_exchange` 中执行迁移。
- 验证 `farm_exchange.public` 下有 17 张表。
- 如确认无误，再考虑是否清理 `postgres.public` 下的旧业务表。

