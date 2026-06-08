# 0061 管理端玩家搜索与私下交易分组

## 变更目标

- 提升管理端目标玩家选择效率，避免只能手填 UUID。
- 提升玩家侧私下交易列表的可读性，减少待处理报价和历史报价混杂。

## 本次实现

### 管理端

- 新增 `GET /api/admin/users/search?q=`
- 支持按 `username` / `nickname` 模糊搜索玩家，返回最近 20 条匹配结果。
- 管理端登录卡新增：
  - 搜索输入
  - 搜索结果列表
  - 点击结果后自动带入 `targetUserId`
  - 选中后立即加载该玩家资产与交易记录

### 玩家侧

- 私下交易面板拆分为三组：
  - 发给我的
  - 我发出的
  - 已结束
- 保留原有创建、接受、取消能力，但把待处理和归档状态分开显示。

## 测试补充

- 管理端玩家搜索接口回归测试

## 验证方式

- `frontend`
  - `npm.cmd run test`
  - `npm.cmd run build`
- `backend`
  - `D:\maven\apache-maven-3.6.3\bin\mvn.cmd -q -s maven-settings.xml -Dtest=FarmExchangeApplicationTests test`
