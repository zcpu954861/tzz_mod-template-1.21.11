# 6.10 WebAdmin 写入前置稳定化报告

## 1. 版本定位

6.10 是 WebAdmin 进入 7.0 配置编辑前的安全闸门，定位为“写入前置稳定化 / 编辑阶段前总审查”。本阶段不开放真实配置编辑，不新增公开配置写入 API，不新增编辑 UI，不写 JSON，也不改变 5.x 已封版的 SignalBridge / SignalDevice / VirtualBlockDevice / ItemStackMatcher / itemSubmit 工具链语义。

6.10 的目标是确认 6.9 写入前置体系足够清晰、可测、可审计，并补齐进入 7.0 前必须具备的 guard。

## 2. 6.9 写入前置审查结果

### 权限矩阵

后端权限判断由 `WebAdminPermissionService` / `WebAdminRolePolicy` 承担，前端 capabilities 只用于展示，不作为安全边界。

| 角色 | 允许 | 禁止 |
| --- | --- | --- |
| VIEWER | READ | TEST、所有编辑、用户管理、系统设置、危险操作 |
| TESTER | READ、TEST | 普通配置编辑、用户管理、系统设置、危险操作 |
| EDITOR | READ、TEST、EDIT_DEVICE、EDIT_SIGNAL、EDIT_REGION、EDIT_ACTION、EDIT_ITEM_MATCHER | EDIT_USER、EDIT_SYSTEM_SETTINGS、DANGEROUS_OPERATION |
| OWNER | 全部操作 | 无 |

审查结论：权限矩阵符合 7.0 前置要求，且已由 guard 覆盖。

### 写结果模型

未来写 API 应统一返回 `WebAdminWriteResult`。当前模型覆盖：

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

6.10 补充了 `WebAdminWriteResult.noChange(...)`，用于未来 mutation service 表达“请求有效但没有实际变更”。`validationErrors` 会对敏感字段和值脱敏，不返回 stack trace、password hash、salt、session token、cookie value 或明文密码。

### CSRF / 安全

`WebAdminWriteSecurityService` 当前提供：

- session 绑定的 CSRF token 生成。
- 缺 token / 错 token / 正确 token 校验结果。
- Origin 同源判断。
- Origin 或 Referer 的统一同源 helper。

未来写 API 必须按顺序经过：

1. 有效 WebAdmin session。
2. 后端权限检查。
3. CSRF / 同源写请求安全校验。
4. 请求 validation。
5. 审计记录。
6. 成功后发布 realtime config_changed 或对象级变更事件。

### 审计模型

`WebAdminAuditEvent` / `WebAdminAuditWriter` / `WebAdminWriteAuditContext` 已能表达：

- `auditId`
- `occurredAt`
- `actorUsername`
- `actorRole`
- session 摘要
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

审计摘要通过 `WebAdminWriteSanitizer` 脱敏。明文密码、password hash、password salt、session token、cookie value 和 secret 类字段不会进入审计 JSON。

当前审计主要是模型和 helper，尚未接入真实配置写入，因为 6.10 不开放写 API。

### Mutation Service 规范

`WebAdminConfigMutationService`、`WebAdminMutationContext`、`WebAdminMutationPreview` 和 `WebAdminMutationValidator` 作为未来写 service 规范存在。7.0 以后具体 mutation service 应遵守：

- Web UI 不直接读写 JSON。
- 不绕过 domain service / store。
- 写前 validate。
- 后端权限检查。
- CSRF / 同源写请求安全检查。
- 审计记录。
- 成功后发布 realtime `config_changed` 和对象级事件。
- 返回统一 `WebAdminWriteResult`。
- 支持 no-op / no-change。
- 预留 conflict detection、preview / dry-run、草稿 / 发布 / 回滚扩展。

### Realtime 写入事件

当前已定义并由 guard 覆盖的写相关 realtime 类型包括：

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

这些类型当前主要作为协议预留，不伪造事件。未来真实写入成功后应发布 `config_changed`、对象级事件和 `write_audit_appended`；写入失败时可发布 `permission_denied` 或 `validation_failed`。payload 必须轻量且不包含敏感信息。

### 前端只读边界

`WebAdminFrontendAssets` 当前仍保持只读。6.10 guard 检查没有真实设备写 POST、DELETE、reset password 函数、可点击保存按钮或删除按钮。capabilities 只显示写入能力后续开放的说明，不提供真实编辑入口。

### 敏感信息保护

审查重点字段：

- `passwordHash`
- `passwordSalt`
- `sessionToken`
- `cookieValue`
- `plainPassword`
- 明文密码
- `TZZ_WEBADMIN_SESSION` 的真实 value

当前 write result、validation error、audit event、realtime event 和 capabilities guard 均覆盖敏感信息不泄漏。

## 3. Guard 覆盖情况

`stabilizationGuardTest` 已覆盖：

- VIEWER / TESTER / EDITOR / OWNER 权限矩阵。
- `permission_denied`、`validation_failed`、`csrf_invalid`、`no_change`、`internal_error` 等写结果语义。
- validation error 对敏感字段和值脱敏。
- CSRF 缺失 / 错误 / 正确 token。
- Origin / Referer 同源 helper。
- audit event 脱敏。
- write realtime event types。
- capabilities 不泄漏 hash、salt 或 session hash。
- 前端只读 guard。

## 4. 已修复问题

本轮修复和补强：

- `WebAdminValidationError` 现在会按 `field` 字段名判断敏感性，敏感字段的 rejected value summary 强制显示“已隐藏”。
- 新增 `WebAdminWriteResult.noChange(...)`，让未来 mutation service 能稳定表达无变更结果。
- `WebAdminWriteSecurityService` 新增 Origin / Referer 统一同源判断 helper。
- 扩展 guard，覆盖完整权限矩阵、更多写结果 code、CSRF / origin、审计脱敏和 capabilities 安全。

## 5. 仍存在的技术债

- 尚未接入真实写 API。
- 尚未实现具体 config mutation transaction。
- 尚未实现 conflict versioning。
- 尚未实现草稿 / 发布 / 回滚。
- 尚未实现多人编辑锁。
- 尚未实现具体设备、Signal、Region、Action 编辑 service。
- 尚未实现写操作的 UI 表单、preview 和确认流程。

这些不是 6.10 阻塞项，因为本阶段目标是安全审查和前置稳定化。

## 6. 7.0 进入条件判断

当前具备进入 7.0 的基础条件：

- 权限矩阵清晰并由后端执行。
- 写结果结构统一。
- CSRF / 同源写请求安全 helper 已具备。
- 审计模型可表达成功、失败、拒绝和校验失败。
- mutation service 规范已存在。
- realtime 写入事件协议已预留。
- 前端仍保持只读，没有真实写入口。
- guard 覆盖写入前置关键安全边界。

未发现必须阻塞 7.0 的安全缺口。

## 7. 推荐 7.0 范围

建议 7.0 第一批只做低风险编辑：

- 设备显示名称 / 备注 / iconKey。
- 基础 enabled 开关，但需要明确运行时影响。
- 基础 channel 字段。
- 低风险 display metadata。

不建议 7.0 第一批直接做：

- itemSubmit / matcher 编辑。
- action command 编辑。
- region bounds 编辑。
- 用户管理写操作。
- 系统安全设置写操作。
- 高风险危险操作。

这些高风险对象应在后续阶段单独设计 validation、preview、冲突检测、审计和回滚策略。
