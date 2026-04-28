# 7.3 回归测试清单

目标版本：`v1.32.0-web-admin-device-extended-config`

## 1. 分支与构建

```powershell
git branch --show-current
git diff --check
./gradlew.bat clean build
./gradlew.bat stabilizationGuardTest --rerun-tasks
```

预期：

- 当前分支为 `feature/web-admin-device-extended-config`。
- `clean build` 通过。
- `stabilizationGuardTest` 输出 `Stabilization guard checks passed.`。

## 2. 登录与权限

1. 使用 OWNER 登录 WebAdmin。
2. 打开设备详情页，确认 supported 设备显示“设备扩展配置”编辑入口。
3. 使用 EDITOR 登录，重复检查，确认可以获取编辑锁并保存。
4. 使用 VIEWER 登录，确认只能查看，不能编辑。
5. 使用 TESTER 登录，确认只能查看，不能编辑。

预期：

- VIEWER / TESTER 不能 acquire `device_extended_config` lock。
- PATCH 对 VIEWER / TESTER 返回权限拒绝。
- 响应不包含 passwordHash、passwordSalt、session token、cookie value 或明文密码。

## 3. virtual_block_device 扩展配置

选择一个 `virtual_block_device`：

1. 编辑 `interactChannel`。
2. 编辑 `successChannel`。
3. 编辑 `failChannel`。
4. 编辑 `interactionCooldownTicks`。
5. 对可选频道使用“设为未设置”。

预期：

- channel 使用深色 combobox，可选择已有频道，也可手动输入新频道。
- 未发现频道时显示不会自动创建消费者的提示。
- 保存需要 CSRF、valid lock、expectedFingerprint。
- 保存成功后页面不闪屏、不跳顶部。
- `itemSubmit`、matcher、interactionItem source/consume、container、itemConditions、redstone、condition 不丢失。

## 4. signal_receiver 扩展配置

选择一个所在区块已加载的 `signal_receiver`：

1. 打开设备详情页。
2. 编辑 `pulseTicks`。
3. 尝试输入 `0`。
4. 尝试输入负数和超大值。

预期：

- 合法 pulseTicks 保存成功。
- `0`、负数、超出范围返回 validation_failed。
- receiver 主 channel、enabled 和其它字段保持。
- 所在区块未加载时显示只读 unsupported 提示，不强制加载区块。

## 5. action_relay 扩展配置

选择一个所在区块已加载的 `action_relay`：

1. 打开设备详情页。
2. 编辑 `cooldownTicks`。
3. 尝试输入负数和超大值。

预期：

- 合法 cooldownTicks 保存成功。
- 非法数值返回 validation_failed。
- 不显示 action 列表编辑入口。
- 不允许编辑 command / message / sound / signal action 内容。
- 所在区块未加载时显示只读 unsupported 提示，不强制加载区块。

## 6. unsupported 设备

选择 `signal_emitter` 或其它不支持扩展配置的设备。

预期：

- 显示“该设备类型暂无可编辑扩展配置”或等价中文提示。
- 不显示可执行扩展配置编辑入口。
- PATCH unsupported field 返回 validation_failed 或 unsupported_field。

## 7. 冲突检测

1. 用户 A 打开设备扩展配置，获取 expectedFingerprint。
2. 用户 B 修改同一扩展字段并保存。
3. 用户 A 使用旧 expectedFingerprint 保存。

预期：

- 用户 A 返回 `conflict_detected`。
- 不覆盖用户 B 的修改。
- 用户 A 的表单输入保留。
- 页面不跳转、不闪屏、不跳顶部。

## 8. 编辑锁

1. 用户 A 进入扩展配置编辑。
2. 用户 B 尝试编辑同一设备扩展配置。
3. 用户 A 取消编辑。
4. 用户 B 再次尝试编辑。

预期：

- 用户 B 初次看到锁占用提示。
- 取消后锁释放，用户 B 可获取锁。
- heartbeat 正常，编辑中锁不会提前过期。
- 锁过期后其它用户可以重新获取。

## 9. Realtime

1. 打开设备详情页。
2. 另一会话修改该设备扩展配置。
3. 观察当前页面。

预期：

- 收到 `device_config_changed` / `config_changed`。
- 当前设备详情静默刷新相关卡片。
- 不闪屏、不跳顶部、不丢折叠状态。
- 如果当前用户正在编辑，不覆盖输入。

## 10. Audit

检查服务端日志或 WebAdmin audit 观测信息：

- 成功保存有 `EDIT_DEVICE_EXTENDED_CONFIG` 记录。
- validation_failed 有失败摘要。
- conflict_detected 有失败摘要。
- edit_lock_conflict / edit_lock_expired 有失败摘要。
- 不记录 passwordHash、passwordSalt、session token、cookie value 或完整 raw device JSON。

## 11. 只读边界

确认没有新增以下编辑入口：

- itemSubmit / matcher。
- interactionItem source / consume。
- action / command action。
- Region bounds / target filter。
- 用户 / 系统设置。
- Scratch-like 编辑器。
- ConditionEngine / GameController / MissionSystem。

## 12. 全站 smoke test

逐页打开：

- `/app#/dashboard`
- `/app#/devices`
- `/app#/signals`
- `/app#/doctor`
- `/app#/history`
- `/app#/users`
- `/app#/settings`
- `/app#/regions`
- `/app#/actions`

预期：

- 页面无回归。
- 登录 / session 正常。
- realtime 状态正常。
- 时间格式和中文显示正常。
