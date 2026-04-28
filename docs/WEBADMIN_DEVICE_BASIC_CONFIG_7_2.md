# 7.2 WebAdmin 设备基础配置编辑

## 1. 版本定位

7.2 是 WebAdmin 第一批真实游戏逻辑配置编辑阶段。它建立在 7.0 的写入闭环和 7.1 的对象版本 / 编辑锁基础上，只开放设备 `enabled` 与主 `channel` 两个低风险字段。

7.0 的 `displayName`、`note`、`iconKey` 只影响 WebAdmin 展示；7.2 的 `enabled` 与 `channel` 会影响设备触发和 Signal 分发，因此保存路径必须经过权限、CSRF、编辑锁、冲突检测、校验、audit 和 realtime。

## 2. 可编辑字段

- `enabled`：boolean，控制设备是否启用。
- `channel`：主频道 / primary channel，必须映射到当前设备类型真实使用的主 channel 字段。

7.2 暂不支持清空主频道。当前 channel 为空的设备可以设置为非空频道。

## 3. 不做字段

7.2 不开放以下字段或能力：

- `interactChannel`、success/fail/off channel。
- cooldown、pulseTicks、redstone mode。
- BlockState condition。
- interactionItem、itemSubmit、matcher、consume。
- action、command action。
- region bounds。
- 用户、系统设置。
- 草稿 / 发布 / 回滚、多人编辑锁之外的协作模型、ConditionEngine、GameController / MissionSystem。

## 4. API

```text
GET /api/webadmin/device-basic-config/{deviceId}
PATCH /api/webadmin/device-basic-config/{deviceId}
```

`GET` 需要登录，VIEWER / TESTER / EDITOR / OWNER 均可读取 safe DTO。DTO 包含当前 enabled、channel、supported / editable 状态、unsupported reason、expectedFingerprint 和 lock status。

`PATCH` 需要登录、EDITOR / OWNER 权限、CSRF / same-origin、有效 `device_basic_config` 编辑锁和 `expectedFingerprint`。返回统一 `WebAdminWriteResult`。

## 5. 权限

- VIEWER：只读，不能获取基础配置编辑锁，不能 PATCH。
- TESTER：只读 / 测试能力，不能获取基础配置编辑锁，不能 PATCH。
- EDITOR：可以编辑 enabled / 主 channel。
- OWNER：可以编辑 enabled / 主 channel。

前端仅用于 UX 隐藏或禁用按钮；后端仍强制权限判断。

## 6. CSRF 与写请求安全

PATCH 使用 WebAdmin write foundation：

- 必须有有效 session。
- 必须有 `X-TZZ-WebAdmin-CSRF`。
- 必须通过 same-origin / referer 校验。
- Content-Type 使用 JSON。
- 失败时返回 `csrf_required` 或 `csrf_invalid`，不返回 stack trace。

## 7. 编辑锁

7.2 使用新的锁目标：

```text
targetType = device_basic_config
targetId = <deviceId>
```

保存前必须先获取锁。编辑中前端 heartbeat 续锁；保存、取消或离开页面时释放锁；断线时依靠 TTL 自动过期。该锁与 7.0 的 `device_metadata` 锁分离，因此显示元数据编辑和基础配置编辑互不阻塞，但各自 PATCH 必须验证自己的锁。

## 8. expectedFingerprint

若设备配置对象没有稳定版本字段，7.2 使用基础配置 fingerprint 检测冲突。fingerprint 由 device id、type、enabled 和 normalized channel 计算。

流程：

1. GET 返回 `expectedFingerprint`。
2. 前端 PATCH 原样提交。
3. 服务端保存前重新计算当前 fingerprint。
4. 不匹配时返回 `conflict_detected`，不覆盖当前服务器数据。

冲突提示应保留用户输入，并要求刷新后重新编辑。

## 9. 校验

- `enabled` 必须为 boolean。
- `channel` 会 trim / normalize。
- channel 最大长度为 128。
- channel 不允许控制字符。
- channel 必须符合现有 SignalChannel 规则。
- 7.2 暂不支持空 channel。

校验失败返回 `validation_failed`，并带字段级 `WebAdminValidationError`。

## 10. 字段保留

修改 enabled / channel 时必须保留所有复杂字段：

- interactionItem / matcher / consume。
- itemSubmitRequirements / consume order。
- container settings。
- itemConditions。
- redstone mode / offChannel。
- BlockState condition。
- action / region / WebAdmin metadata。

WebAdmin 不直接重建 JSON，也不清空现有复杂配置。

## 11. Audit

成功保存记录 `EDIT_DEVICE_BASIC_CONFIG` 审计事件，包含 actor、role、target device、beforeSummary、afterSummary、changed fields 和 result。

权限拒绝、validation_failed、conflict_detected、edit_lock_conflict、edit_lock_expired 等失败路径应记录安全摘要。审计不记录 session token、cookie value、passwordHash、passwordSalt 或大型 raw device JSON。

## 12. Realtime

保存成功后发布轻量事件：

- `config_changed`
- `device_config_changed`
- `write_audit_appended`

payload 包含 deviceId、changedFields、actor、routeTarget 和简短 summary，不推送完整设备列表或完整 SignalDeviceData。

前端收到相关事件时只静默刷新当前相关区域，不全页 reload，不跳顶部，不丢筛选、折叠或正在编辑的输入。

## 13. UI 行为

设备详情页新增“设备基础配置”卡片：

- 默认只读显示启用状态和主频道。
- EDITOR / OWNER 可点击“编辑基础配置”。
- VIEWER / TESTER 只显示权限说明。
- 保存前获取 `device_basic_config` 锁。
- 保存带 lockId 和 expectedFingerprint。
- 成功后退出编辑模式并静默刷新。
- validation / conflict / lock error 保留输入。

页面文案必须明确：这些配置会影响设备触发与 Signal 分发，修改后会立即应用到当前世界。

## 14. 兼容性

7.2 不改变 5.x 底层工具链语义，不修改 SignalDeviceData schema，不扫描世界，不强制加载区块，不扫描背包，不触发 Signal 或 Action。未加载的物理方块设备不会被强制加载；若无法安全编辑，API 返回 unsupported / validation_failed。

## 15. 后续 7.3 建议

建议后续先评估较低风险字段：

- success/fail channel 的安全编辑。
- cooldown / pulseTicks。
- 更明确的 channel 管理 UI。

itemSubmit、matcher、action command、region bounds 等高风险字段应继续单独设计校验、预览、冲突和回滚策略。
