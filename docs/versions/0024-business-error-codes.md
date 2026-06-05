# 0024 业务错误码

## 当前已有能力

- 后端已具备统一异常响应结构，业务异常、参数校验异常、请求格式异常、数据库异常都会返回统一 JSON。
- 玩家侧已支持注册、登录、交易密码、商店购买、种植养殖、收获、交易站、私下交易、大宗令牌。
- 管理侧已支持税率配置、大宗令牌发放、用户资产与交易记录查询。
- 测试已覆盖主要玩家链路、交易链路、大宗令牌链路和后台基础链路。

## 本次完成内容

- 为 `ApiException` 增加 `code` 字段，业务异常响应不再固定返回 `BUSINESS_ERROR`。
- 新增 `ErrorCode` 常量类，集中管理前端可判断的业务错误码。
- 补齐认证、用户、交易密码、大宗令牌、农场扩建、商店、交易站、私下交易、生产、后台管理的核心错误码。
- 将高频业务错误拆分为稳定类别：
  - `AUTH_REQUIRED`、`AUTH_INVALID`、`AUTH_EXPIRED`、`AUTH_FAILED`
  - `PERMISSION_DENIED`
  - `USER_NOT_FOUND`、`USER_INACTIVE`、`USERNAME_EXISTS`
  - `TRADE_PASSWORD_REQUIRED`、`TRADE_PASSWORD_LOCKED`、`TRADE_PASSWORD_INVALID`
  - `INSUFFICIENT_BALANCE`、`INSUFFICIENT_INVENTORY`
  - `BULK_TOKEN_REQUIRED`、`BULK_TOKEN_INVALID`、`BULK_TOKEN_EXPIRED`、`BULK_TOKEN_LIMIT_EXCEEDED`
  - `ITEM_NOT_FOUND`、`ITEM_NOT_TRADABLE`、`WALLET_NOT_FOUND`、`RESOURCE_NOT_FOUND`
  - `STATE_CONFLICT`、`CONFIG_MISSING`、`INVALID_OPERATION`
- 更新 MockMvc 测试，开始断言未登录、交易密码、大宗令牌、商品不可交易、后台权限等场景的 `code`。

## 验证结果

- 已执行：`D:\maven\apache-maven-3.6.3\bin\mvn.cmd -s maven-settings.xml -q test`
- 结果：通过。
- Flyway 当前验证到 2 个迁移，测试库 schema 已是版本 2，无需迁移。

## 风险与说明

- 目前错误码已经覆盖主要业务路径，但错误码分层还属于 MVP 阶段。
- 部分状态类错误暂时统一归入 `STATE_CONFLICT`，后续如果前端需要更细的提示，可以继续拆出如 `SLOT_FULL`、`OFFER_EXPIRED`、`GROWTH_NOT_READY`。
- 前端后续应优先根据 `code` 做交互分支，`message` 只作为展示文本，不建议再解析中文文案。

## 下次建议目标

- 前端接入统一错误处理：根据 `code` 显示登录失效、交易密码未设置、余额不足、大宗令牌缺失等明确提示。
- 增加 Pinia 全局通知/错误状态，让所有接口调用复用同一套提示逻辑。
- 后端可继续补充更细粒度业务码，并输出一份前后端共用的错误码对照文档。
