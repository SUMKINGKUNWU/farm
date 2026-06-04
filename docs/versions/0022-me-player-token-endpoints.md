# 0022 玩家侧登录态接口第一阶段

## 当前已有内容

- 后端已支持登录接口和 Bearer token。
- 管理端接口已迁移到 `Authorization: Bearer {token}`。
- 玩家侧接口仍主要使用 `/api/users/{userId}/...`，前端玩家侧尚未正式联调。

## 本次完成内容

- 新增 `/api/me` 玩家侧登录态包装接口。
- 新接口从 `Authorization: Bearer {token}` 解析当前登录玩家 ID，再复用现有业务服务。
- 本阶段保留旧 `/api/users/{userId}/...` 接口，避免破坏已有调用和测试。
- 已覆盖的 `/api/me` 接口包括：
  - `POST /api/me/trade-password`
  - `GET /api/me/summary`
  - `GET /api/me/farm/plots`
  - `GET /api/me/ranch/slots`
  - `POST /api/me/farm/expand`
  - `POST /api/me/ranch/expand`
  - `GET /api/me/inventory`
  - `POST /api/me/farm/plots/{plotId}/plant`
  - `POST /api/me/ranch/slots/{slotId}/raise`
  - `POST /api/me/growth/{growthId}/harvest`
  - `POST /api/me/shop/purchase`
  - `POST /api/me/market/buy`
  - `POST /api/me/market/sell`
  - `GET /api/me/market/items/{itemCode}/quote`
  - `POST /api/me/private-trades`
  - `POST /api/me/private-trades/{offerId}/accept`
  - `POST /api/me/private-trades/{offerId}/cancel`
  - `GET /api/me/bulk-tokens`
- 补充后端测试：
  - 未登录访问 `/api/me/summary` 返回 401。
  - 登录后可通过 `/api/me/summary` 获取自己的经营摘要。
  - 登录后可设置交易密码。
  - 登录后可购买商店商品。
  - 登录后可查询自己的库存。
  - 登录后可查询交易站行情。

## 验证结果

- 后端测试通过：
  - `mvn -s maven-settings.xml -q test`

## 风险与后续注意

- `/api/me` 当前是包装层，底层仍复用以 `userId` 为参数的业务服务。
- 旧 `/api/users/{userId}/...` 接口仍然可用，后续需要决定是保留兼容、加鉴权限制，还是逐步废弃。
- 玩家侧前端尚未接入 `/api/me`，仍需新增玩家操作台页面。

## 下次建议目标

- 开始做前端玩家操作台：
  - 登录/注册。
  - 设置交易密码。
  - 查看经营摘要。
  - 商店购买。
  - 库存、田地、牧场和交易站基础操作。
- 或继续强化后端安全：给旧 `/api/users/{userId}` 接口加“只能访问自己或管理员”的鉴权限制。
