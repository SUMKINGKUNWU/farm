# 0052 管理台回归脚本与错误提示优化

## 当前已有

- 玩家端已有 `frontend/scripts/player_regression.py` 交互回归脚本。
- 玩家端、管理台、身份卡组件均已完成拆分、只读入参和事件通信解耦。
- `NoticeBlock.vue` 已支持展示错误消息、错误码、操作建议、HTTP 状态、接口路径和字段错误。

## 本次完成

- 新增管理台交互回归脚本：
  - `frontend/scripts/admin_regression.py`
- 管理台脚本内置 mock API 响应，不依赖当前后端服务和数据库状态。
- 管理台脚本覆盖目标：
  - 切换到管理台。
  - 填写目标玩家 ID。
  - 刷新控制台并读取资产、交易记录。
  - 读取税率配置。
  - 保存交易站税率。
  - 发放大宗交易令牌。
  - 读取玩家资产和交易记录。
- 管理员本地校验错误从普通 `Error` 调整为 `ApiClientError`：
  - 未登录管理员操作使用 `AUTH_REQUIRED`。
  - 未填写目标玩家 ID 使用 `TARGET_USER_REQUIRED`。
  - 登录表单缺失使用 `VALIDATION_ERROR`。
  - 非管理员账号登录管理台使用 `ADMIN_REQUIRED`。
- `apiError.js` 补充 `ADMIN_REQUIRED`、`TARGET_USER_REQUIRED` 的用户提示和操作建议。

## 验证结果

- 已执行 `npm.cmd run build`，前端生产构建通过。
- 已执行 `python -m py_compile frontend/scripts/admin_regression.py frontend/scripts/player_regression.py`，两个回归脚本语法检查通过。
- 已搜索确认 `adminStore.js` 本地校验均使用 `ApiClientError`。
- 当前环境仍未安装 Playwright，未实际执行浏览器回归脚本。

## 下次建议

- 若允许安装 Playwright，可执行玩家端和管理台两个回归脚本，形成自动化交互验收闭环。
- 继续补充后端接口级测试，覆盖税率、令牌、资产、交易、私下交易和并发锁定场景。
