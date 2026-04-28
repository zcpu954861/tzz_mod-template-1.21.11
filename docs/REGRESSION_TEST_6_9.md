# 6.9 WebAdmin 写入前置回归测试清单

## 1. 构建与护栏

- 运行 `./gradlew.bat clean build`，必须通过。
- 运行 `./gradlew.bat stabilizationGuardTest --rerun-tasks`，必须输出 `Stabilization guard checks passed.`。
- 确认没有 `6.0.1` / `6.0.2` 阶段编号残留。

## 2. 权限矩阵

- VIEWER 只能 READ，不能 EDIT_DEVICE。
- TESTER 可以 READ / TEST，不能 EDIT_DEVICE。
- EDITOR 可以 EDIT_DEVICE / EDIT_SIGNAL / EDIT_REGION / EDIT_ACTION / EDIT_ITEM_MATCHER。
- EDITOR 不能 EDIT_USER / EDIT_SYSTEM_SETTINGS / DANGEROUS_OPERATION。
- OWNER 可以 EDIT_USER / EDIT_SYSTEM_SETTINGS / DANGEROUS_OPERATION。

## 3. 写结果模型

- `permission_denied` 返回 `success=false`，message 为中文。
- `validation_failed` 返回 `success=false`，包含 `validationErrors`。
- `ok` 返回 `success=true`。
- `dangerous_operation_requires_confirmation` 应带 `requiresConfirmation=true`。
- 响应不包含 Java stack trace。
- 响应不包含 passwordHash、passwordSalt、session token、cookie value 或明文密码。

## 4. CSRF / 写请求安全

- 缺少 CSRF token 时校验失败。
- 错误 CSRF token 校验失败。
- 正确 CSRF token 校验通过。
- 未登录不能访问 `GET /api/webadmin/write/capabilities`。
- 登录后可访问 `GET /api/webadmin/write/capabilities`，但该接口不执行写入。
- capabilities 响应不包含 session token 或 cookie value。

## 5. 审计脱敏

- 审计事件包含 actor、role、operation、target、result 和 errorCode。
- 审计 summary 中的 passwordHash / passwordSalt / token / cookie 字段被隐藏。
- 写失败、权限拒绝和 validation failed 都能形成结构化审计事件。

## 6. realtime 写入事件规范

- `config_changed` 类型存在。
- `write_audit_appended` 类型存在。
- `permission_denied` 类型存在。
- `validation_failed` 类型存在。
- `device_config_changed` / `signal_config_changed` / `region_config_changed` / `action_config_changed` 类型存在。
- 事件 payload 不包含敏感字段。
- 当前没有真实写操作时，不应伪造 config changed 事件。

## 7. users/settings 敏感信息

- `/api/webadmin/users` 仍然 OWNER-only。
- 非 OWNER 访问用户列表返回 403。
- `/api/webadmin/settings` 对非 OWNER 隐藏敏感存储路径。
- API 不返回 passwordHash、passwordSalt、session token、cookie value、明文密码。

## 8. realtime payload 安全

- `/api/realtime/events` 仍需要有效 WebAdmin session。
- 事件不包含 passwordHash、passwordSalt、session token、cookie value 或明文密码。
- 6.8 的 hidden / visible dirty refresh、silent refresh、route filtering 仍正常。

## 9. 前端只读边界

- Dashboard、Devices、Signals、Doctor、History、Users、Settings、Regions、Actions 页面仍可访问。
- 页面上没有可执行保存、删除、重置密码、编辑设备、执行 action、emit signal 等写操作按钮。
- 如出现“后续开放”说明，必须不可点击且不触发写 API。
- 现有搜索、筛选、排序、详情页返回、时间格式化、中文显示不回退。

## 10. 5.x / 6.x 兼容

- SignalBridge / SignalDevice / VirtualBlockDevice / ItemStackMatcher / itemSubmit 行为不变。
- WebAdmin 登录 / session / logout 不变。
- 6.1 只读 API 不变。
- 6.2～6.8 页面无回归。
- WebAdmin 持久化目录仍为 `<world-save-root>/tzz/webadmin/`。
