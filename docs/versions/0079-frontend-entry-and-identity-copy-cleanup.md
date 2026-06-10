# 0079 前端入口与身份面板中文文案清理

## 本次调整

- 清理应用入口 `App.vue` 的导航、模式切换与管理税种名称乱码
- 清理玩家身份面板文案与输入占位文案乱码
- 清理管理身份面板文案、搜索结果文案与控制台入口文案乱码

## 调整文件

- `frontend/src/App.vue`
- `frontend/src/components/identity/PlayerIdentity.vue`
- `frontend/src/components/identity/AdminIdentity.vue`

## 说明

这一轮只处理高频入口层的中文可见性问题，不改状态流和接口行为：

- 玩家端 / 管理台模式切换
- 左侧导航
- 玩家登录与注册
- 管理员登录
- 管理员搜索玩家与刷新控制台

这样可以先恢复最常用入口的可读性，后续再继续向工作区面板和提示消息扩展。

## 验证

```powershell
cd D:\ai-project\farm\frontend
npm.cmd run test -- apiError.test.js
npm.cmd run build
```
