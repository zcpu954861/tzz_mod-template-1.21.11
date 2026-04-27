# 6.0 WebAdmin Foundation

WebAdmin Foundation 是 TZZ Mod 6.x 管理界面的后端地基和登录最小闭环。本阶段只实现登录、session、基础状态页和基础状态 API，不做完整 Dashboard。

本阶段保持 5.x 已封版能力兼容，不改变 SignalBridge、SignalDevice、`virtual_block_device`、ItemStackMatcher、itemSubmit、Doctor、debug、旧命令或旧 JSON schema。

## 1. 当前范围

已实现：

- 轻量 JDK HTTP server。
- 登录页。
- 登录后基础状态页。
- 用户账号文件。
- PBKDF2 密码哈希。
- 短期 session cookie。
- `/api/auth/login`。
- `/api/auth/logout`。
- `/api/auth/me`。
- `/api/status`。
- `/tzz webadmin status`。
- `/tzz webadmin user list|create|disable|enable|resetPassword`。

暂不包含：

- 设备列表。
- Signal 频道页。
- 逻辑链视图。
- Doctor 完整页。
- History 完整页。
- WebSocket。
- 配置编辑。
- 用户管理 Web 页面完整 CRUD。
- 区域管理页。
- 动作系统页。
- 节点编辑。
- 多人协作锁。
- 内置 HTTPS。
- OAuth / 2FA / 邮箱找回。

## 2. 配置文件

配置文件位置（6.1 起按当前世界 / 当前存档隔离）：

```text
<world-save-root>/tzz/webadmin/web_admin_config.json
```

默认内容：

```json
{
  "enabled": false,
  "host": "127.0.0.1",
  "port": 18080,
  "accessMode": "LOCAL_ONLY",
  "sessionTtlMinutes": 120,
  "rememberMeTtlMinutes": 120,
  "loginCodeTtlSeconds": 120,
  "auditEnabled": true
}
```

默认 `enabled=false`。启用 WebAdmin 需要手动修改配置并重启服务器。

WebAdmin 的所有持久化文件都属于当前世界 / 当前地图项目：

```text
<world-save-root>/tzz/webadmin/web_admin_config.json
<world-save-root>/tzz/webadmin/web_admin_users.json
<world-save-root>/tzz/webadmin/web_admin_audit.log
```

单人世界 A 与单人世界 B 不共享 WebAdmin 用户、密码、访问模式、端口、安全设置或审计日志。Dedicated Server 的 WebAdmin 设置也跟随当前 server world。旧版 `config/tzz` 下的 WebAdmin 文件不会自动加载，也不会自动删除；如需迁移，需要管理员手动复制到对应世界的 `tzz/webadmin/` 目录。

## 3. 访问模式

`LOCAL_ONLY`

- 默认模式。
- 建议监听 `127.0.0.1`。
- 适合本机管理和开发。

`LAN_DEV`

- 面向局域网协作开发。
- 需要显式配置。
- 启动日志和 `/tzz webadmin status` 会显示安全提示。

`MULTIPLAYER_DEV`

- 面向多人服务器协作开发。
- 必须显式配置。
- 不会隐式开启。
- 只应在可信网络和可信账号范围内使用。

安全提示：本阶段不提供内置 HTTPS、公网暴露自动化、OAuth、2FA 或邮箱找回。不要在不受信网络中开放端口。

## 4. 用户账号

用户文件位置：

```text
<world-save-root>/tzz/webadmin/web_admin_users.json
```

用户字段包含：

- `username`
- `displayName`
- `role`
- `enabled`
- `passwordHash`
- `passwordSalt`
- `passwordAlgorithm`
- `passwordIterations`
- `createdAt`
- `createdBy`
- `lastLoginAt`
- `failedLoginCount`
- `lockedUntil`
- `forcePasswordChange`

密码不会明文保存。当前使用 JDK 原生 `PBKDF2WithHmacSHA256`，并保存算法、迭代次数、salt 和 hash。

角色已预留：

- `VIEWER`
- `TESTER`
- `EDITOR`
- `OWNER`

本阶段命令层主要用于创建和维护账号，Web 页面暂不提供完整用户 CRUD。

## 5. WebAdmin 命令

查看状态：

```text
/tzz webadmin status
```

用户管理：

```text
/tzz webadmin user list
/tzz webadmin user create <username> <role>
/tzz webadmin user disable <username>
/tzz webadmin user enable <username>
/tzz webadmin user resetPassword <username>
```

权限：

- 控制台允许。
- OP / 创造级管理员允许。
- 普通玩家禁止。

创建用户或重置密码时，服务端会生成初始密码并只显示一次。请立即保存。

## 6. 访问与登录

启用后访问：

```text
http://<host>:<port>
```

示例：

```text
http://127.0.0.1:18080
```

登录成功后，服务端会写入：

```text
TZZ_WEBADMIN_SESSION
```

cookie 属性：

- `HttpOnly`
- `SameSite=Lax`
- `Path=/`
- `Max-Age` 按 `sessionTtlMinutes` 或 `rememberMeTtlMinutes` 计算

当前不是 HTTPS 时不会强制 `Secure`。代码预留 `secureCookie` 配置位，后续接入 HTTPS 或反向代理时可扩展。

## 7. API

统一成功响应：

```json
{
  "ok": true,
  "data": {}
}
```

统一失败响应：

```json
{
  "ok": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "请先登录。"
  }
}
```

### POST /api/auth/login

请求：

```json
{
  "username": "admin",
  "password": "xxxx",
  "rememberMe": true
}
```

成功返回当前用户摘要，并写入 session cookie。

### POST /api/auth/logout

清除服务端 session，并清除浏览器 cookie。

### GET /api/auth/me

返回当前登录用户、角色、session 过期时间和 accessMode。

### GET /api/status

返回基础状态：

- 平台名。
- WebAdmin 是否启用和运行。
- host / port / accessMode。
- 当前 session 数。
- Minecraft server 状态。
- Mod 版本。
- 当前用户和角色。

## 8. 前端页面

本阶段只提供：

- 登录页。
- 登录后的基础状态页。

设计方向：

- 平台名：游戏开发编辑平台。
- 深色纯色背景。
- 暗色 navy / black 管理平台风格。
- 青蓝 cyan 强调色。
- 左侧平台介绍，右侧登录卡片。
- 现代、简洁、开发工具感。

基础状态页只展示 WebAdmin Foundation 状态。设备管理、Signal 频道、Doctor、历史记录等页面将在后续阶段接入。

## 9. 审计日志

当前会记录：

- WebAdmin server 启动 / 停止。
- 用户创建。
- 用户禁用 / 启用。
- 密码重置。
- 登录成功。
- 登录失败。
- 登出。

审计日志不会记录明文密码，也不会记录完整 sessionId。

## 10. 后续原则

Web UI 后续必须遵守 5.15 稳定化结论：

- Web UI 不直接读写 JSON。
- 命令、游戏内工具、Web UI 应共用服务层。
- Web UI 需要 DTO / structured response。
- Web UI 最终必须覆盖所有可配置功能，不是缩水版。
- 所有写操作应走服务端统一服务层，并保留权限与审计。
- 未来需要 WebSocket 或等价机制做实时同步。

建议后续阶段：

- 6.1 Service / DTO 只读层。
- 6.2 设备只读列表与详情 API。
- 6.3 Doctor / History 只读页面。
- 后续再进入配置编辑、实时同步、多人协作和完整 Dashboard。
