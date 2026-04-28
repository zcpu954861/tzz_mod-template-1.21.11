# 7.0 WebAdmin 配置编辑基础报告

## 1. 版本定位

7.0 是 WebAdmin 配置编辑基础 / 最小安全写入闭环。本阶段的目标不是开放完整配置编辑，而是验证一条低风险、可审计、可回滚设计前置充分的 Web UI 写入链路。

本阶段只允许编辑 WebAdmin 设备显示元数据：

- `displayName`
- `note`
- `iconKey`

这些字段只影响 WebAdmin 展示，不改变 Minecraft 游戏逻辑，不改变 SignalBridge、SignalDevice、VirtualBlockDevice、itemSubmit、ActionEngine、RegionController 或命令语义。

## 2. 为什么第一批只编辑显示元数据

设备显示名称、备注和图标是低风险字段：

- 不参与 signal emit。
- 不参与 redstone、interaction、container、itemSubmit、matcher、consume 判断。
- 不影响 action 执行。
- 不影响 region bounds 或 target filter。
- 不影响已有 JSON schema 的游戏逻辑字段。

因此它适合用于验证 WebAdmin 写入链路：权限、CSRF、validation、统一写结果、审计、realtime 和前端失败保留状态。

## 3. 存储位置

新增 WebAdmin 专用 world-save scoped 元数据文件：

```text
<world-save-root>/tzz/webadmin/web_admin_device_metadata.json
```

该文件由 `WebAdminDeviceMetadataStore` 管理。旧世界没有该文件时默认空 metadata；不同世界之间不共享设备显示元数据。

## 4. API

```text
GET /api/webadmin/device-metadata/{deviceId}
PATCH /api/webadmin/device-metadata/{deviceId}
```

`GET` 要求有效 WebAdmin session，`VIEWER`、`TESTER`、`EDITOR`、`OWNER` 均可读取安全 DTO。

`PATCH` 是 7.0 唯一新增的真实写 API，但仅写 WebAdmin 设备显示元数据。它要求：

- 有效 WebAdmin session。
- `EDITOR` 或 `OWNER` 权限。
- CSRF token 校验通过。
- 同源写请求校验通过。
- JSON 请求体。
- validation 通过。

## 5. 权限

权限规则：

- `VIEWER`：只能查看，不能保存。
- `TESTER`：只能查看，不能保存。
- `EDITOR`：可以编辑设备 WebAdmin 显示元数据。
- `OWNER`：可以编辑设备 WebAdmin 显示元数据。

前端使用 `/api/webadmin/write/capabilities` 控制按钮展示，但后端仍在 `PATCH` 路径强制执行权限检查。

## 6. CSRF 与写请求安全

`PATCH` 请求必须携带 `X-TZZ-WebAdmin-CSRF`。服务端通过 `WebAdminWriteSecurityService` 校验 token，并使用 Origin / Referer helper 做同源检查。

缺失 token、错误 token 或同源校验失败会返回统一的 `WebAdminWriteResult` 失败结果，不返回 stack trace、session token 或 cookie value。

## 7. Validation

`displayName`：

- 可为空。
- trim 后保存。
- 最大 64 字符。
- 禁止控制字符。

`note`：

- 可为空。
- trim 后保存。
- 最大 500 字符。
- 禁止控制字符。
- 只保存纯文本。

`iconKey`：

- 可为空，等价于 `auto`。
- 必须属于预设 icon key。
- 不支持上传图片、外部 URL 或任意资源引用。

校验失败返回 `validation_failed`，并带 `validationErrors`。错误信息为中文，`rejectedValueSummary` 经过脱敏。

## 8. WebAdminWriteResult

本阶段写 API 统一返回 `WebAdminWriteResult`：

- 成功：`ok`，`success=true`，`changed=true`。
- 无变化：`no_change`，`changed=false`。
- 权限不足：`permission_denied`。
- 目标不存在：`target_not_found`。
- 校验失败：`validation_failed`。
- CSRF / 同源失败：对应安全错误。

结果中只包含安全摘要，不返回内部大对象或敏感字段。

## 9. Audit

保存成功、校验失败和权限拒绝都会进入结构化审计模型。审计记录包含：

- actor
- role
- operationType：`EDIT_DEVICE_METADATA`
- targetType：`DEVICE_METADATA`
- targetId
- beforeSummary
- afterSummary
- result
- errorCode
- message

审计不记录明文密码、password hash、salt、session token 或 cookie value。输入摘要通过 `WebAdminWriteSanitizer` 脱敏。

## 10. Realtime

保存成功后发布轻量 realtime 事件：

- `config_changed`
- `device_config_changed`
- `write_audit_appended`

payload 只包含 deviceId、changedFields、actor、routeTarget 和摘要信息，不推送完整 metadata store，不包含敏感内容。

前端收到 `device_config_changed` 后按当前 route 过滤：

- 当前设备详情页匹配 deviceId 时静默刷新 metadata。
- 设备列表页可标记或局部刷新。
- 无关页面只更新 realtime 状态或忽略。

刷新不整页 reload，不跳顶部，不丢搜索、筛选、排序、折叠状态或详情页 returnTo。

## 11. UI 行为

设备详情页新增“WebAdmin 显示信息”卡片：

- 默认只读展示。
- `EDITOR` / `OWNER` 显示“编辑显示信息”按钮。
- `VIEWER` / `TESTER` 显示权限说明，不显示可执行保存入口。
- 表单包含显示名称、备注、图标预设。
- 保存中只显示局部状态。
- 保存成功后更新卡片并保留页面状态。
- 保存失败后保留用户输入并展示 validation errors。
- 取消后恢复原值。

页面文案明确提示：这些信息仅用于 WebAdmin 展示，不改变游戏逻辑。

## 12. 本阶段不做的内容

7.0 不编辑：

- `enabled`
- `channel`
- `interactChannel`
- success/fail channel
- cooldown
- redstone mode
- interactionItem
- itemSubmit
- matcher
- consume
- action / command action
- region bounds
- WebAdmin 用户
- 系统设置

不做 Scratch-like 模块编辑器、节点画布、模板、向导、多人编辑锁、草稿 / 发布 / 回滚、ConditionEngine 或 GameController / MissionSystem。

## 13. 后续建议

7.1 可以优先做对象版本号和 conflict detection，为并发编辑和草稿模型打基础。

7.2 可继续开放低风险设备基础显示 / 标签类配置。涉及 enabled、channel、itemSubmit、action command、region bounds 等高风险字段前，应先补 preview、dry-run、回滚、冲突检测和更细粒度权限。
