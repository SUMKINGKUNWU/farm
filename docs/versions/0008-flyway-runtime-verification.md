# 0008 - Flyway 运行验证成功

## 当前已有

上一阶段已经完成：

- Spring Boot 后端骨架。
- PostgreSQL 连接配置。
- Flyway 首版迁移脚本。
- `/api/health` 健康检查接口。

## 本次完成

本次确认用户手动启动 Spring Boot 后，Flyway 已成功执行数据库迁移。

## 验证结果

Spring Boot 日志显示：

```text
Successfully validated 1 migration
Creating Schema History table "public"."flyway_schema_history"
Migrating schema "public" to version "1 - init schema"
Successfully applied 1 migration to schema "public", now at version v1
Tomcat started on port(s): 3000
```

数据库查询结果：

```text
installed_rank | version | description | success
1              | 1       | init schema | true
```

健康检查接口：

```text
GET http://127.0.0.1:3000/api/health
```

返回：

```json
{"status":"UP","service":"farm-exchange-backend","time":"2026-06-04T17:33:43.762251500+08:00"}
```

## 当前已创建数据表

当前 public schema 中已有 17 张表：

- `admin_audit_logs`
- `app_users`
- `asset_ledger`
- `bulk_trade_tokens`
- `expansion_config`
- `farm_plots`
- `flyway_schema_history`
- `growth_instances`
- `items`
- `market_price_snapshots`
- `market_trades`
- `player_inventory`
- `private_trade_offers`
- `ranch_slots`
- `tax_config`
- `tax_records`
- `wallets`

## 注意事项

日志中出现：

```text
Flyway upgrade recommended: PostgreSQL 15.17 is newer than this version of Flyway
```

这是兼容性提醒，不是迁移失败。

当前使用的 Spring Boot 2.7.18 内置 Flyway 8.5.13，官方提示 PostgreSQL 15 比该 Flyway 版本声明测试的最高版本新。实际本次迁移已经成功。

后续如果想消除警告，可以升级 Flyway 或升级 Spring Boot，但当前阶段不必急着处理。

## 下次待做

建议下一阶段进入 `0009`：

- 实现用户注册接口。
- 注册时初始化钱包 10,000。
- 注册时初始化土地和牧栏。
- 注册时写入初始资产流水。
- 实现交易密码设置接口。

