# 7.2 回归测试清单

## 1. 分支与构建

```text
git branch --show-current
git diff --check
git grep -n "6\\.0\\.1\\|6\\.0\\.2" -- README.md CHANGELOG.md docs src
./gradlew.bat clean build
./gradlew.bat stabilizationGuardTest --rerun-tasks
```

预期：当前分支为 `feature/web-admin-device-basic-config`，构建和稳定化护栏通过。

## 2. 测试账号

在当前世界中准备 WebAdmin 用户：

```text
/tzz webadmin user create owner OWNER
/tzz webadmin user create editor EDITOR
/tzz webadmin user create tester TESTER
/tzz webadmin user create viewer VIEWER
/tzz webadmin status
```

## 3. 测试设备数据

优先使用当前项目已有设备。可先查看：

```text
/tzz signal device list
```

如需要真实方块设备，请先在游戏内放置 signal_emitter、signal_receiver 或 action_relay 方块，记录坐标，再使用项目现有 TAB 补全或让 Codex 输出当前实现的准确绑定命令创建测试设备。VirtualBlockDevice 可使用现有 VBD 创建/绑定命令准备。

## 4. GET 基础配置

登录 WebAdmin 后打开设备详情：

```text
/app#/devices/<deviceId>
```

预期：

- 显示“设备基础配置”卡片。
- 显示启用状态和主频道。
- 显示“这些配置会影响设备触发与 Signal 分发”的说明。
- Network 中 `GET /api/webadmin/device-basic-config/{deviceId}` 返回 safe DTO。
- DTO 包含 expectedFingerprint 和 lockStatus。
- 响应不包含 passwordHash、passwordSalt、session token、cookie value 或明文密码。

## 5. 权限

以 VIEWER / TESTER 登录：

- 不应显示可执行“编辑基础配置”按钮，或按钮为不可点击权限说明。
- 直接调用 PATCH 应返回 `permission_denied`。
- 不能获取 `device_basic_config` 编辑锁。

以 EDITOR / OWNER 登录：

- 可以获取编辑锁。
- 可以进入编辑模式。
- 保存时需要 CSRF、lockId 和 expectedFingerprint。

## 6. enabled 编辑

以 EDITOR / OWNER 登录：

1. 打开设备详情。
2. 点击“编辑基础配置”。
3. 切换启用状态。
4. 保存。

预期：

- PATCH 返回 `ok`。
- 设备详情静默刷新，不跳页、不闪屏、不跳顶部。
- audit 有 EDIT_DEVICE_BASIC_CONFIG 成功记录。
- realtime 推送 `config_changed` / `device_config_changed` / `write_audit_appended`。
- 设备列表和详情状态一致。

## 7. channel 编辑

1. 输入合法频道，例如 `webadmin.test`.
2. 保存。

预期：

- PATCH 返回 `ok`。
- 主频道更新。
- Signal 相关页面能看到新的频道关联。
- 不改变 interactChannel、success/fail/off channel。

## 8. 校验失败

分别测试：

- enabled 传非 boolean。
- channel 超过 128 字符。
- channel 包含控制字符。
- channel 为空字符串。

预期：

- 返回 `validation_failed`。
- 字段错误中文显示。
- 表单输入保留。
- 不写入配置。
- audit 记录 validation 失败摘要。

## 9. no_change

在不修改 enabled / channel 的情况下保存。

预期：

- 返回 `no_change` 或 changed=false。
- 不误报成功变更。
- 页面不跳转，不清空输入。

## 10. 编辑锁

1. 用户 A 进入设备基础配置编辑。
2. 用户 B 打开同设备详情。
3. 用户 B 尝试编辑。

预期：

- 用户 B 看到锁占用提示。
- 用户 B 不能同时编辑。
- 用户 A 取消或保存后锁释放。
- 锁状态通过 realtime 更新。

## 11. 冲突检测

1. 用户 A 打开编辑表单。
2. 用户 B 保存 enabled 或 channel，导致 fingerprint 改变。
3. 用户 A 使用旧 expectedFingerprint 保存。

预期：

- 返回 `conflict_detected`。
- 不覆盖用户 B 的修改。
- 用户 A 输入保留。
- 页面提示刷新后重新编辑。

## 12. 字段保留

修改 enabled / channel 后检查：

- itemSubmitRequirements 未丢失。
- interactionItem / matcher / consume 未丢失。
- container 配置未丢失。
- itemConditions 未丢失。
- redstone mode / offChannel / condition 未丢失。
- WebAdmin display metadata 未丢失。

## 13. Realtime 与 UI 稳定性

保存成功后确认：

- 当前详情页静默刷新。
- 不闪屏、不跳顶部。
- 不清空搜索 / 筛选 / 折叠状态。
- 正在编辑中的其他输入不被远端事件覆盖。
- 高频事件不会造成 API 风暴。

## 14. 边界确认

确认没有新增以下入口：

- itemSubmit / matcher / consume 编辑。
- action / command action 编辑。
- region bounds 编辑。
- 用户 / 系统设置编辑。
- signal emit / action execute。
- 草稿 / 发布 / 回滚。

## 15. 旧页面回归

逐页 smoke test：

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

预期：页面加载正常，登录/session/realtime 正常，无敏感字段泄漏。
