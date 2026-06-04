# 0019 管理端基础能力

## 当前已有内容

- 玩家侧已支持注册、交易密码、田地/牧栏扩建、种植养殖、收获、商店购买、交易站即时交易、行情快照、私下交易报价单和大宗交易令牌校验。
- 大宗交易令牌已支持玩家查询、MVP 发放、交易站扣减、私下交易接受时扣减。
- 数据库已有 `tax_config`、`bulk_trade_tokens`、`asset_ledger`、`market_trades`、`private_trade_offers` 和 `admin_audit_logs`。

## 本次完成内容

- 新增管理端基础接口，统一前缀为 `/api/admin`。
- 新增临时管理员校验：
  - 通过请求参数 `adminUserId` 校验用户 `role = 'ADMIN'` 且 `status = 'ACTIVE'`。
  - 这是 MVP 脚手架鉴权，后续接入登录态/token 后替换。
- 新增税率管理：
  - `GET /api/admin/tax-configs`
  - `PUT /api/admin/tax-configs/{tradeType}`
  - 支持 `MARKET`、`PRIVATE`、`BULK` 三类税率配置。
  - 税率以 basis points 存储，例如 `300` 表示 3%。
- 新增管理员发放大宗交易令牌：
  - `POST /api/admin/users/{targetUserId}/bulk-tokens`
  - 复用大宗令牌发放逻辑，支持商品类型、单笔限额、总限额、次数和有效期。
- 新增用户资产查询：
  - `GET /api/admin/users/{targetUserId}/assets`
  - 返回用户基础信息、钱包余额、锁定余额和库存列表。
- 新增用户交易记录查询：
  - `GET /api/admin/users/{targetUserId}/trades`
  - 合并返回交易站记录和私下交易报价记录，按时间倒序，最多 100 条。
- 新增审计日志写入：
  - 调整税率写入 `UPDATE_TAX_CONFIG`。
  - 发放大宗令牌写入 `ISSUE_BULK_TOKEN`。
- 补充后端测试：
  - 非管理员访问管理端被拒绝。
  - 管理员可查询与更新税率。
  - 管理员可给玩家发放大宗交易令牌。
  - 管理员可查询玩家资产和交易记录。
  - 验证审计日志写入。

## 风险与后续注意

- 当前管理端鉴权仍依赖显式传入 `adminUserId`，不能作为正式安全方案。
- 管理端交易查询暂未分页，只限制最近 100 条，后续需要支持游标分页和筛选条件。
- 税率修改会立即影响后续交易，后续可以增加生效时间、审批流和变更前后快照。

## 下次建议目标

- 开始前端联调管理端页面：税率配置、大宗令牌发放、玩家资产和交易记录查看。
- 或优先做正式登录态/token 鉴权，让玩家端和管理端都不再依赖 URL 中传入用户 ID。
