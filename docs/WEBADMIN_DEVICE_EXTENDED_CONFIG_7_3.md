# 7.3 WebAdmin 设备扩展基础配置编辑

## 1. 版本定位

7.3 在 7.2 设备基础配置编辑之后，继续开放少量结构简单、风险较低的设备扩展配置字段。该阶段仍然不进入 itemSubmit、matcher、Action、Region、用户或系统设置编辑。

当前目标版本：`v1.32.0-web-admin-device-extended-config`。

## 2. 可编辑字段

按设备类型返回 supported fields，前端只展示该设备真实支持的字段：

- `virtual_block_device`
  - `interactChannel`
  - `successChannel`
  - `failChannel`
  - `interactionCooldownTicks`
- `signal_receiver`
  - `pulseTicks`，仅在对应 block entity 已加载时可编辑。
- `action_relay`
  - `cooldownTicks`，仅在对应 block entity 已加载时可编辑。
- `signal_emitter`
  - 当前暂无可编辑扩展基础配置。

## 3. 不做字段

7.3 不开放以下内容：

- itemSubmit requirements、consume、ItemStackMatcher。
- interactionItem source、matcher、consume 策略。
- container item condition。
- action 列表、command / message / sound / signal action 内容。
- action 执行或测试触发。
- Region bounds、target filter。
- 用户、角色、系统设置。
- Scratch-like 编辑器、ConditionEngine、GameController / MissionSystem。

## 4. API

新增只读/写入 API：

- `GET /api/webadmin/device-extended-config/{deviceId}`
- `PATCH /api/webadmin/device-extended-config/{deviceId}`

GET 返回：

- 设备 ID / 类型。
- supported fields。
- 当前字段值。
- clearable fields。
- `expectedFingerprint`。
- `device_extended_config` lock status。

PATCH 需要：

- 登录 session。
- `EDITOR` 或 `OWNER` 权限。
- CSRF / same-origin 校验。
- 有效 `device_extended_config` edit lock。
- `expectedFingerprint`。
- JSON body。

## 5. 权限与安全

写入继续复用 WebAdmin write foundation：

- `WebAdminPermissionService`
- `WebAdminWriteSecurityService`
- `WebAdminEditLockService`
- `WebAdminWriteResult`
- `WebAdminValidationError`
- `WebAdminAuditWriter`
- `WebAdminWriteSanitizer`

`VIEWER` / `TESTER` 只能查看，不能 acquire lock 或 PATCH。`EDITOR` / `OWNER` 可以编辑 supported fields。

## 6. edit lock

7.3 使用独立 lock target：

```text
targetType = device_extended_config
targetId = <deviceId>
```

它与 `device_metadata`、`device_basic_config` 分离。不同卡片可以分别锁定，但保存时仍依赖 fingerprint 检测实际配置冲突。

## 7. expectedFingerprint

GET 返回当前扩展字段 fingerprint。PATCH 保存前重新计算当前 fingerprint：

- 匹配：继续校验并写入。
- 不匹配：返回 `conflict_detected`，不覆盖当前服务器数据。

冲突提示为中文用户可读信息，并附带安全的当前扩展配置摘要。

## 8. validation

channel 字段：

- trim / normalize。
- 最大长度 128。
- 禁止控制字符。
- 复用 `SignalChannel` 合法性。
- 可选择已有频道，也可手动输入新频道。
- 手动新频道不会自动创建 listener、receiver 或 action_relay。

清空规则：

- `interactChannel`、`successChannel`、`failChannel` 是可选扩展频道，支持显式“设为未设置”。
- 空输入本身不等于清空；需要显式清空操作。

ticks 字段：

- 必须是整数。
- `interactionCooldownTicks` 允许 `0..72000`，0 表示无冷却。
- `pulseTicks` 使用 receiver block entity 现有最小/最大值，0 不合法。
- `cooldownTicks` 使用 action relay block entity 现有最小/最大值。

## 9. audit

保存成功记录：

- actor / role。
- operationType：`EDIT_DEVICE_EXTENDED_CONFIG`。
- targetType：`DEVICE`。
- targetId。
- beforeSummary / afterSummary。
- changed fields。
- result：success。

失败路径至少覆盖：

- `permission_denied`
- `validation_failed`
- `conflict_detected`
- `edit_lock_conflict`
- `edit_lock_expired`

审计不记录 session token、cookie value、passwordHash、passwordSalt 或完整 raw device JSON。

## 10. realtime

保存成功发布轻量事件：

- `config_changed`
- `device_config_changed`
- `write_audit_appended`

payload 只包含 deviceId、changedFields、occurredAt、actor、routeTarget、summary 等安全摘要，不推送完整设备列表或完整 `SignalDeviceData`。

前端在当前设备详情页静默刷新扩展配置、Debug / Doctor 摘要，不闪屏、不跳顶部、不覆盖正在编辑的输入。

## 11. UI 行为

设备详情页新增“设备扩展配置”卡片：

- unsupported 设备显示中文只读空状态。
- supported 设备按字段展示中文标签。
- `EDITOR` / `OWNER` 显示编辑入口。
- `VIEWER` / `TESTER` 只显示权限提示。
- channel 字段复用 7.2 深色 combobox。
- ticks 字段使用 number input。
- validation / conflict / lock 错误保留输入并显示中文提示。

## 12. field preservation

修改扩展字段时必须保留：

- enabled / 主 channel / offChannel。
- interactionItem matcher/source/consume。
- itemSubmit requirements / consume。
- container open/close/change channel 和 item conditions。
- BlockState condition。
- redstone mode。
- WebAdmin metadata。
- runtime/history summary。
- device id / world / pos / type。

## 13. 后续建议

7.4 可以继续评估 Signal / Listener / Receiver / ActionRelay 更系统化的编辑能力。itemSubmit、matcher、Action command、Region bounds 等高风险配置应继续独立设计、独立审查。
