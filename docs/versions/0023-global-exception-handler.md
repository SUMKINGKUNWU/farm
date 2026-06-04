# 0023 全局异常处理增强

## 当前已有内容

- 后端已有 `ApiException` 业务异常。
- 旧版全局异常处理只覆盖 `ApiException` 和请求体参数校验异常。
- 旧版错误响应主要包含 `success` 和 `message`。

## 本次完成内容

- 新增统一错误响应对象 `ApiErrorResponse`。
- 保留兼容字段：
  - `success`
  - `message`
- 新增错误上下文字段：
  - `code`
  - `status`
  - `path`
  - `timestamp`
  - `fieldErrors`
- 扩展全局异常处理类型：
  - `ApiException`
  - `MethodArgumentNotValidException`
  - `MissingServletRequestParameterException`
  - `MissingRequestHeaderException`
  - `MethodArgumentTypeMismatchException`
  - `HttpMessageNotReadableException`
  - `HttpRequestMethodNotSupportedException`
  - `DataIntegrityViolationException`
  - `DataAccessException`
  - 兜底 `Exception`
- 对数据库异常做日志记录：
  - 数据约束冲突返回 409。
  - 数据库访问异常返回 500。
  - 未知系统异常返回 500。
- 补充后端测试：
  - 验证业务异常响应包含 `success/code/status/message/path`。
  - 验证参数校验异常响应包含 `fieldErrors`。

## 验证结果

- 后端测试通过：
  - `mvn -s maven-settings.xml -q test`

## 风险与后续注意

- 当前业务异常统一使用 `BUSINESS_ERROR`，后续可以在 `ApiException` 中增加业务错误码字段。
- 当前字段校验错误返回第一条错误作为主 `message`，完整错误列表放在 `fieldErrors`。
- 兜底异常不会向前端暴露堆栈，避免泄露服务内部信息。

## 下次建议目标

- 给 `ApiException` 增加明确业务错误码，例如 `TRADE_PASSWORD_REQUIRED`、`INSUFFICIENT_BALANCE`、`BULK_TOKEN_REQUIRED`。
- 或继续开发前端玩家操作台，接入 `/api/me` 登录态接口。
