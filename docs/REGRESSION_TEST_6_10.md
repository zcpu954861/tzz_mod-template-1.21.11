# 6.10 WebAdmin 写入前置稳定化回归清单

## 1. 自动化检查

在 `feature/web-admin-write-stabilization` 上执行：

```powershell
git diff --check
git grep -n "6\\.0\\.1\\|6\\.0\\.2" -- README.md CHANGELOG.md docs src
./gradlew.bat clean build
./gradlew.bat stabilizationGuardTest --rerun-tasks
```

预期：

- `clean build` 通过。
- `stabilizationGuardTest` 输出 `Stabilization guard checks passed.`。
- 没有错误阶段编号残留。
- 无真正空白错误。

## 2. Capabilities 权限矩阵

准备 WebAdmin 用户：

```text
/tzz webadmin user create owner_guard OWNER
/tzz webadmin user create editor_guard EDITOR
/tzz webadmin user create tester_guard TESTER
/tzz webadmin user create viewer_guard VIEWER
```

分别登录 WebAdmin，访问：

```text
GET /api/webadmin/write/capabilities
```

预期：

- 未登录访问返回 401。
- VIEWER 只允许 READ。
- TESTER 允许 READ / TEST，不允许 EDIT_DEVICE。
- EDITOR 允许普通配置编辑能力摘要，不允许 EDIT_USER / EDIT_SYSTEM_SETTINGS / DANGEROUS_OPERATION。
- OWNER 显示完整权限摘要。
- 响应只表示未来能力，不执行写操作。

## 3. CSRF Helper

通过 `stabilizationGuardTest` 确认：

- 缺 token 校验失败。
- 错 token 校验失败。
- 正确 token 校验通过。
- Origin / Referer 同源 helper 能拒绝跨源来源。

手动浏览器测试：

- 登录后 capabilities 中可看到未来写请求需要 CSRF。
- 未登录无法获取该信息。
- 不显示 session cookie value。

## 4. 敏感信息不泄漏

检查以下响应和事件：

- `/api/auth/me`
- `/api/status`
- `/api/webadmin/users`
- `/api/webadmin/settings`
- `/api/webadmin/write/capabilities`
- `/api/realtime/events`
- 所有只读 DTO API

预期页面和 Network 响应不包含：

- passwordHash
- passwordSalt
- session token
- cookie value
- plainPassword
- 明文密码

类名、字段常量或服务端源代码中出现这些名称可以接受；真实响应、事件 payload 和前端 DOM 不应泄漏真实值。

## 5. 前端无写入口

打开以下页面：

```text
/app#/dashboard
/app#/devices
/app#/signals
/app#/doctor
/app#/history
/app#/users
/app#/settings
/app#/regions
/app#/actions
```

预期：

- 不出现可执行保存按钮。
- 不出现可执行编辑按钮。
- 不出现可执行删除按钮。
- 不出现 reset password 按钮。
- 不出现 emit signal 或 execute action 按钮。
- 如有“后续开放”提示，必须是说明文字或禁用态，不应触发写请求。

## 6. Realtime 回归

登录 WebAdmin 后确认：

- topbar 显示实时同步状态。
- `/api/realtime/events` 连接存在。
- logout 后连接关闭。
- signal history 新增时仍能推送 `signal_emitted` / `history_appended`。
- 写相关事件类型当前不伪造，不会在没有真实写操作时乱发。
- event payload 不包含敏感信息。

## 7. 只读页面烟测

逐页检查：

- Dashboard 统计、最近事件、Doctor 摘要正常。
- Devices 列表、详情、返回上下文正常。
- Signals 列表、详情、逻辑链正常。
- Doctor 页面搜索、筛选、跳转正常。
- History 页面搜索、筛选、排序正常。
- Users 页面 OWNER 可访问，非 OWNER 不泄漏敏感信息。
- Settings 页面 OWNER 显示完整摘要，非 OWNER 隐藏敏感路径。
- Regions / Actions 列表和详情正常。

## 8. 7.0 前置判断

确认 6.10 后仍满足：

- 没有公开配置写入 API。
- 没有真实配置编辑。
- 没有 JSON 写入。
- 没有新增 WebSocket 或编辑功能。
- 没有修改 5.x 底层工具链语义。
- 具备进入 7.0 低风险编辑的基础护栏。
