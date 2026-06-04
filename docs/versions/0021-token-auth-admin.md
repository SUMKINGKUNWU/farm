# 0021 登录态与管理端 Token 鉴权

## 当前已有内容

- 后端已支持玩家经营、交易站、私下交易、大宗交易令牌和管理端基础能力。
- 前端已具备 Vue 管理台页面，可操作税率配置、令牌发放、资产查询和交易记录查询。
- 管理端上一阶段仍使用 `adminUserId` 查询参数作为临时管理员校验。

## 本次完成内容

- 新增登录接口：
  - `POST /api/auth/login`
  - 使用用户名和登录密码登录。
  - 返回 `tokenType = Bearer` 和 `accessToken`。
- 新增当前用户接口：
  - `GET /api/auth/me`
  - 通过 `Authorization: Bearer {token}` 获取当前登录用户信息。
- 新增轻量 Bearer Token：
  - 使用 HMAC-SHA256 签名。
  - Token 载荷包含用户 ID、用户名、角色和过期时间。
  - 默认有效期为 86400 秒。
  - 默认本地密钥可通过 `farm.auth.token-secret` 配置覆盖。
- 管理端接口改为强制 Token 鉴权：
  - 移除管理端接口对 `adminUserId` 查询参数的依赖。
  - 管理端控制器统一解析 `Authorization` header。
  - 后端仍会到数据库复核 `role = 'ADMIN'` 且 `status = 'ACTIVE'`。
- 前端管理台改造：
  - 侧栏从“管理员用户 ID”输入改为“管理员用户名 + 登录密码”。
  - 登录成功后保存 access token。
  - 管理端 API 请求统一携带 `Authorization: Bearer {token}`。
  - 支持退出登录。
  - 页面主要中文文案恢复为正常 UTF-8。
- 测试覆盖：
  - 登录密码错误返回 401。
  - 登录成功返回 token。
  - `/api/auth/me` 可读取当前用户。
  - 非管理员 token 访问管理端被拒绝。
  - 管理员 token 可操作税率、令牌、资产和交易记录接口。

## 验证结果

- 后端测试通过：
  - `mvn -s maven-settings.xml -q test`
- 前端构建通过：
  - `npm run build`
- 前端构建仍有 ECharts chunk 体积提示，不影响运行。

## 风险与后续注意

- 当前 Token 是项目内轻量实现，不依赖外部 JWT 库；后续若需要标准生态兼容，可替换为正式 JWT 方案。
- 当前 Token 尚未支持刷新令牌、撤销令牌、设备列表和强制下线。
- 玩家侧接口仍保留 `/api/users/{userId}/...` 形式，尚未完全迁移到登录态。
- 本地默认 token secret 仅适合开发环境，部署时必须配置强随机密钥。

## 下次建议目标

- 继续把玩家侧接口迁移到登录态：
  - 新增 `/api/me/summary`、`/api/me/farm/...`、`/api/me/market/...` 等接口。
  - 前端玩家侧不再手填用户 ID。
- 或补一套管理员初始化流程，避免只能通过数据库手动把用户改成 `ADMIN`。
