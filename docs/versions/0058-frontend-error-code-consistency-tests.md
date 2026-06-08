# 0058 前端错误码一致性测试

## 变更目标

- 为前端 `apiError.js` 增加回归测试，保证以下后端业务错误码能被前端稳定识别：
  - `INSUFFICIENT_BALANCE`
  - `INSUFFICIENT_INVENTORY`
  - `STATE_CONFLICT`
  - `BULK_TOKEN_REQUIRED`
  - `BULK_TOKEN_INVALID`
  - `BULK_TOKEN_EXPIRED`
  - `BULK_TOKEN_LIMIT_EXCEEDED`

## 实现说明

- 在 `frontend/package.json` 增加 `npm run test`，使用 Node 原生 `node:test`，不额外引入测试框架。
- 新增 `frontend/src/apiError.test.js`：
  - 断言余额不足、库存不足、状态冲突的 `code/message/action` 映射稳定。
  - 直接读取后端 `backend/src/main/java/com/farm/exchange/common/ErrorCode.java`，校验前端完整覆盖全部 `BULK_TOKEN_*` 错误码。
  - 校验未知错误码仍保留后端返回的 `code` 和 `message`，避免前端吞码。
  - 校验 `normalizeError` 对 `ApiClientError` 和普通 `Error` 的兼容行为。

## 验证方式

- 在 `frontend` 目录执行：
  - `npm.cmd run test`
  - `npm.cmd run build`

## 结果

- 前端现在对这四类核心业务错误具备可回归的一致性测试。
- 后端如果未来新增或改动大宗令牌错误码，前端测试会直接暴露映射缺口。
