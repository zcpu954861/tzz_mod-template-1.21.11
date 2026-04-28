# 7.1 WebAdmin 对象版本 / 编辑锁基础

## 1. 版本定位

7.1 在 7.0 的最小安全写入闭环上增加对象版本、保存冲突检测和编辑锁。它不扩大编辑能力，只增强 WebAdmin 设备显示元数据编辑。

当前仍只允许编辑：

- `displayName`
- `note`
- `iconKey`

这些字段只影响 WebAdmin 展示，不改变 Minecraft 游戏逻辑、SignalBridge 行为、`SignalDeviceData` schema 或 `signal_devices.json`。

## 2. version / expectedVersion

WebAdmin 设备显示元数据存放在：

```text
<world-save-root>/tzz/webadmin/web_admin_device_metadata.json
```

每条 metadata 包含：

- `deviceId`
- `displayName`
- `note`
- `iconKey`
- `updatedAt`
- `updatedBy`
- `version`

`GET /api/webadmin/device-metadata/{deviceId}` 返回当前版本。`PATCH /api/webadmin/device-metadata/{deviceId}` 必须提交 `expectedVersion`。

如果提交的 `expectedVersion` 与当前版本不一致，服务端返回：

```text
code = conflict_detected
```

并拒绝覆盖当前数据。

## 3. lock lifecycle

编辑锁是内存态协作状态，不写入世界配置文件。服务端重启后锁自然消失。

生命周期：

1. 用户点击“编辑显示信息”。
2. 前端调用 acquire 获取 `device_metadata:<deviceId>` 锁。
3. 编辑模式中定期 heartbeat 续锁。
4. 保存成功或取消编辑时释放锁。
5. 页面断线、关闭或异常退出时，由 TTL 自动过期。

默认 TTL：5 分钟。

## 4. lock API

新增 transient edit-lock API：

```text
POST /api/webadmin/edit-locks/acquire
POST /api/webadmin/edit-locks/heartbeat
POST /api/webadmin/edit-locks/release
GET  /api/webadmin/edit-locks/status?targetType=device_metadata&targetId=<deviceId>
```

写类锁 API 需要：

- 有效 WebAdmin session
- EDITOR 或 OWNER 权限
- CSRF token
- same-origin / Referer 校验

`status` 是只读接口，登录后可查看安全摘要。非持有人不会获得可用于提交的 `lockId`。

## 5. conflict / lock result code

新增或使用的统一结果：

- `conflict_detected`：metadata version 已被别人更新。
- `edit_lock_required`：PATCH 没有有效锁。
- `edit_lock_conflict`：目标正在被其他用户编辑。
- `edit_lock_expired`：锁已过期。
- `validation_failed`：字段校验失败。
- `permission_denied`：权限不足。

所有结果走 `WebAdminWriteResult`，不返回 Java stacktrace 或敏感字段。

## 6. realtime lock event

锁状态变化发布轻量事件：

```text
edit_lock_changed
```

payload 仅包含：

- `targetType`
- `targetId`
- `locked`
- `holderUsername`
- `holderRole`
- `expiresAt`
- `actor`
- `auditId`

不包含 session token、cookie value、password hash、password salt。

## 7. audit

审计覆盖：

- lock acquired
- lock released
- lock conflict
- metadata save success
- metadata save conflict
- validation failed
- permission denied

heartbeat 不写审计，避免高频刷日志。

## 8. 权限规则

- OWNER：可获取、续期、释放锁，可保存 metadata。
- EDITOR：可获取、续期、释放自己的锁，可保存 metadata。
- TESTER：不可获取锁，不可保存。
- VIEWER：不可获取锁，不可保存。

OWNER 可以释放非自己持有的锁；普通用户只能释放自己持有的锁。

## 9. 不做的内容

7.1 不包含：

- enabled / channel / itemSubmit / matcher / consume 编辑
- action / region / user / settings 编辑
- signal emit / action execute
- 草稿 / 发布 / 回滚
- 多人编辑合并界面
- ConditionEngine
- GameController / MissionSystem

## 10. 后续 7.2 建议

下一步可在版本和编辑锁基础上评估低风险设备逻辑配置编辑，例如：

- 设备备注 / iconKey 的更细粒度 UI
- 设备基础 channel 字段的 preview / validate
- enabled 开关的权限、审计和冲突策略

高风险项如 itemSubmit、command action、region bounds 应继续后置。
