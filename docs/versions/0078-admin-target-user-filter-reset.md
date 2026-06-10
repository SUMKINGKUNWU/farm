# 0078 管理端切换玩家时重置筛选上下文

## 本次调整

- 管理端切换目标玩家时，重置玩家资产筛选条件
- 管理端切换目标玩家时，重置交易记录筛选条件与原因选项
- 管理端切换目标玩家时，清空上一位玩家的资产与交易列表结果
- 审计日志仍保留当前页码重置逻辑，仅回到第 1 页

## 原因

此前管理端在切换目标玩家后，会沿用上一位玩家的：

- `itemType`
- 交易来源
- 交易状态
- 交易原因
- 每页条数

这会带来两个问题：

1. 新玩家数据被旧筛选条件误过滤，看起来像“没有数据”
2. 交易原因下拉仍可能短暂保留上一位玩家上下文

## 调整文件

- `frontend/src/stores/adminStore.js`

## 调整内容

在 `selectTargetUser(user)` 中：

- 重置 `assetFilters`
- 重置 `tradeFilters`
- 重置 `tradeFilterOptions`
- 清空 `assets`
- 清空 `tradeResult`

这样后续执行 `loadPlayerConsole()` 时，会基于新玩家重新拉取：

- 默认资产视图
- 默认交易筛选
- 新玩家自己的交易原因选项

## 验证

```powershell
cd D:\ai-project\farm\frontend
npm.cmd run test -- apiError.test.js
npm.cmd run build
```
