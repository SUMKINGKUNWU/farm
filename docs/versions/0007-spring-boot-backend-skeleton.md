# 0007 - Spring Boot 后端骨架与 Flyway 接入

## 当前已有

上一阶段已经完成：

- PostgreSQL 本地连接验证。
- PostgreSQL 首版表结构设计。
- 数据库迁移脚本 `database/migrations/001_init_schema.sql`。
- 功能分析和 MVP 边界。

## 本次完成

本次新增 Spring Boot 后端工程骨架：

- `backend/pom.xml`
- `backend/maven-settings.xml`
- `backend/src/main/java/com/farm/exchange/FarmExchangeApplication.java`
- `backend/src/main/java/com/farm/exchange/common/HealthController.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/migration/V1__init_schema.sql`

后端技术选择：

- Java 11
- Spring Boot 2.7.18
- Spring Web
- Spring Validation
- Spring JDBC
- PostgreSQL Driver
- Flyway

## 本次验证

已执行 Maven 编译：

```powershell
& 'D:\maven\apache-maven-3.6.3\bin\mvn.cmd' -s maven-settings.xml -q -DskipTests compile
```

结果：

```text
编译通过
```

## Maven 配置说明

本机 Maven 全局配置会尝试使用：

```text
E:\Maven_m2\repository
```

该路径当前不可访问，因此本次新增项目专用配置：

```text
backend/maven-settings.xml
```

后续在 `backend` 目录运行 Maven 时建议使用：

```powershell
& 'D:\maven\apache-maven-3.6.3\bin\mvn.cmd' -s maven-settings.xml ...
```

依赖会缓存到：

```text
backend/.m2/repository
```

## 数据库配置

当前 `application.yml` 使用本地开发默认值：

```text
FARM_DB_URL=jdbc:postgresql://localhost:5432/postgres
FARM_DB_USERNAME=postgres
FARM_DB_PASSWORD=123456
```

后续生产或其他环境应通过环境变量覆盖。

## Flyway 迁移

已将首版建表脚本复制到 Spring Boot Flyway 目录：

```text
backend/src/main/resources/db/migration/V1__init_schema.sql
```

理论上服务启动后，Flyway 会自动执行该脚本，并生成：

```text
flyway_schema_history
```

## 当前未完成验证

尝试启动 Spring Boot 并调用 `/api/health` 时，启动后台进程的操作被当前环境审批系统拦截。

因此本次尚未完成：

- Spring Boot 运行时启动验证。
- Flyway 自动建表验证。
- `/api/health` 接口运行验证。

这不是代码编译问题。当前已确认编译通过。

## 下次待做

建议下一步在用户允许启动后端服务后执行：

```powershell
cd D:\ai-project\farm\backend
& 'D:\maven\apache-maven-3.6.3\bin\mvn.cmd' -s maven-settings.xml spring-boot:run
```

然后访问：

```text
http://127.0.0.1:3000/api/health
```

并检查数据库：

```sql
select installed_rank, version, description, success
from flyway_schema_history
order by installed_rank;
```

如果验证通过，下一阶段可以进入 `0008`：

- 实现用户注册。
- 注册时初始化钱包 10,000。
- 初始化土地和牧栏。
- 写入初始资产流水。
- 实现交易密码设置接口。

