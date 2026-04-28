# WebAdmin 6.9 写入前置 / 权限审计 / Service API 基础

## 1. 版本定位

6.9 是 WebAdmin 配置编辑前的安全基础阶段。它不开放真实配置编辑，不新增公开可调用的配置写入 API，不写业务 JSON，也不改变 5.x 已封版的 SignalBridge、SignalDevice、VirtualBlockDevice、ItemStackMatcher、itemSubmit、RegionController 或 ActionEngine 语义。

本阶段目标是为 7.0 之后的 WebAdmin 配置编辑准备统一的结果模型、权限判断、CSRF / 同源安全边界、审计事件模型、mutation service 规范和 realtime 变更事件协议。

## 2. 为什么不能直接进入 7.0 编辑

WebAdmin 编辑能力会触碰设备配置、Signal channel、Region、Action、item matcher、用户和系统安全设置。直接开放编辑会带来这些风险：

- 绕过现有命令和 store 的校验逻辑。
- 缺少权限分层，导致 VIEWER / TESTER 获得写能力。
- 外部网页可能诱导浏览器携带 cookie 发起写请求。
- 写失败、权限拒绝和校验失败缺少统一审计。
- 前端无法统一处理 validation、conflict、no-change 和 dangerous confirmation。
- 配置变化后不能稳定通知当前 WebAdmin 页面。

因此 6.9 只建立基础设施，不修改任何真实配置。

## 3. 权限矩阵

| 角色 | READ | TEST | 普通配置编辑 | 用户管理 | 系统设置 | 危险操作 |
| --- | --- | --- | --- | --- | --- | --- |
| VIEWER | 是 | 否 | 否 | 否 | 否 | 否 |
| TESTER | 是 | 是 | 否 | 否 | 否 | 否 |
| EDITOR | 是 | 是 | device / Signal / Region / Action / item matcher | 否 | 否 | 否 |
| OWNER | 是 | 是 | 是 | 是 | 是 | 是 |

服务端通过 `WebAdminPermissionService` 和 `WebAdminRolePolicy` 判断权限。前端只可使用权限摘要控制展示，不可作为安全边界。

## 4. 写 API 结果格式

未来写 API 必须返回统一 `WebAdminWriteResult`：

```json
{
  "success": false,
  "code": "validation_failed",
  "message": "提交内容未通过校验。",
  "targetType": "DEVICE",
  "targetId": "device-id",
  "changed": false,
  "validationErrors": [],
  "auditId": "",
  "realtimeEventId": "",
  "requiresConfirmation": false,
  "conflict": {},
  "data": {}
}
```

常见 code：

- `ok`
- `permission_denied`
- `unauthenticated`
- `csrf_required`
- `csrf_invalid`
- `validation_failed`
- `target_not_found`
- `conflict_detected`
- `dangerous_operation_requires_confirmation`
- `no_change`
- `internal_error`

所有 message 必须中文可读。失败响应不得返回 Java stack trace、password hash、salt、session token、cookie value 或大型内部对象。

## 5. validationErrors

`WebAdminValidationError` 用于字段级校验错误：

- `field`
- `code`
- `message`
- `rejectedValueSummary`

`rejectedValueSummary` 必须是安全摘要。敏感内容必须显示为“已隐藏”，不能泄漏密码、hash、salt、token、cookie 或 secret。

## 6. CSRF / 安全策略

6.9 新增 `WebAdminWriteSecurityService`，为未来写请求提供：

- session 级 CSRF token。
- CSRF token 校验。
- Same-origin / Origin 基础校验 helper。

未来所有写请求必须满足：

- 有效 WebAdmin session。
- 服务端权限检查通过。
- CSRF token 正确。
- 请求来源符合 WebAdmin host / port 预期。
- 危险操作具备二次确认。

新增只读接口：

```text
GET /api/webadmin/write/capabilities
```

该接口只返回当前用户的权限摘要、未来写操作能力、CSRF 策略和 token，不执行任何配置写入。

## 7. 审计模型

6.9 定义结构化 `WebAdminAuditEvent`：

- `auditId`
- `occurredAt`
- `actorUsername`
- `actorRole`
- `sessionIdHashSummary`
- `remoteAddress`
- `operationType`
- `targetType`
- `targetId`
- `targetDisplayName`
- `beforeSummary`
- `afterSummary`
- `result`
- `errorCode`
- `message`

审计 summary 经过 `WebAdminWriteSanitizer` 脱敏。写成功、写失败、权限拒绝、校验失败和冲突都应可审计。

## 8. realtime config_changed 事件规范

6.9 补充未来写操作相关 realtime 事件类型：

- `config_changed`
- `write_audit_appended`
- `permission_denied`
- `validation_failed`
- `user_changed`
- `system_settings_changed`
- `device_config_changed`
- `signal_config_changed`
- `region_config_changed`
- `action_config_changed`

当前没有真实写操作，因此不会伪造这些事件。未来写操作成功后必须发布轻量 `config_changed` 或对象专用事件，不推送完整 DTO 或敏感字段。

## 9. 未来 service 层模式

未来 mutation service 应遵守：

```text
WebAdminConfigMutationService<T>
  preview(context, request)
  apply(context, request)
```

写入流程：

```text
request
→ WebAdminMutationContext
→ permission check
→ CSRF / write security
→ validate
→ preview
→ apply through existing domain service / store
→ audit
→ realtime config_changed
→ WebAdminWriteResult
```

Web UI 不能直接改 JSON，不能绕过现有 store / domain service。

## 10. 当前未开放内容

6.9 不包含：

- 设备编辑页面。
- Signal 编辑页面。
- Region / Action 编辑页面。
- 用户写操作页面。
- 系统设置写操作页面。
- 公开配置写 API。
- 配置写入。
- JSON schema 修改。
- 草稿 / 发布 / 回滚。
- 多人编辑锁。
- ConditionEngine。
- GameController / MissionSystem。

## 11. 7.0 前需要 6.10 审查

进入真实编辑前建议先做 6.10 审查：

- 权限矩阵是否覆盖全部操作。
- CSRF / Origin 检查是否覆盖所有写请求。
- 审计日志是否可追踪每次写入。
- mutation service 是否复用既有 store / domain service。
- realtime config_changed 是否能驱动当前页面刷新。
- conflict/no-change/dangerous confirmation 是否有统一前端处理。
- 是否需要草稿 / 发布 / 回滚 / 多人编辑锁。
