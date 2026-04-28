# 7.0 WebAdmin 编辑基础回归测试清单

## 1. 构建与护栏

- 运行 `./gradlew.bat clean build`。
- 运行 `./gradlew.bat stabilizationGuardTest --rerun-tasks`。
- 确认资源完整性测试仍通过。
- 确认 `stabilizationGuardTest` 覆盖 metadata validation、权限、CSRF、write result、audit redaction、realtime event 和 frontend guard。

## 2. 准备 WebAdmin 用户

在服务器控制台或 OP level 4 玩家执行：

```text
/tzz webadmin user create owner_test OWNER
/tzz webadmin user create editor_test EDITOR
/tzz webadmin user create tester_test TESTER
/tzz webadmin user create viewer_test VIEWER
/tzz webadmin status
```

保存创建命令返回的一次性初始密码。确认 WebAdmin 启用后访问 `http://127.0.0.1:18080/`。

## 3. 准备设备数据

至少准备一个已有 Signal 设备或 virtual block device。可使用当前项目中已有的 Signal 设备绑定流程；如果命令格式不确定，请使用 TAB 补全或让 Codex 输出当前实现的准确命令。

建议至少覆盖：

- signal_emitter
- signal_receiver
- action_relay
- virtual_block_device

在 WebAdmin 设备列表确认设备可见，并进入设备详情页。

## 4. OWNER 编辑设备显示元数据

使用 `owner_test` 登录。

1. 打开 `/app#/devices/<deviceId>`。
2. 找到“WebAdmin 显示信息”卡片。
3. 点击“编辑显示信息”。
4. 修改显示名称、备注和图标。
5. 点击保存。

预期：

- 保存成功。
- 页面不跳转。
- 页面不闪屏。
- 滚动位置不回到顶部。
- 详情页 returnTo 不丢失。
- 卡片显示新显示名称、备注、图标、最后修改时间和最后修改人。
- 设备列表中显示名称 / 图标同步更新或静默刷新后更新。
- 不影响游戏内设备逻辑。

## 5. EDITOR 编辑设备显示元数据

使用 `editor_test` 登录，重复 OWNER 测试。

预期：

- EDITOR 可以保存。
- 保存成功后产生 audit 和 realtime 事件。
- 不允许编辑 enabled、channel、itemSubmit、action、region 或系统设置。

## 6. VIEWER / TESTER 权限拒绝

分别使用 `viewer_test` 和 `tester_test` 登录。

预期：

- 设备详情页不显示可执行保存入口，或显示禁用说明。
- 直接调用 `PATCH /api/webadmin/device-metadata/{deviceId}` 返回 `permission_denied`。
- 响应不包含 passwordHash、passwordSalt、session token、cookie value 或明文密码。
- metadata 文件不发生变化。

## 7. CSRF 测试

使用 EDITOR 或 OWNER session 测试 PATCH：

- 缺少 `X-TZZ-WebAdmin-CSRF`：预期失败。
- 使用错误 token：预期失败。
- 使用 `/api/webadmin/write/capabilities` 返回的正确 token：预期成功或进入 validation 结果。

预期：

- CSRF 失败不写 metadata。
- 响应为统一 `WebAdminWriteResult`。
- 不返回 stack trace。

## 8. Validation 测试

使用 EDITOR 或 OWNER：

- displayName 超过 64 字符。
- note 超过 500 字符。
- displayName 或 note 包含控制字符。
- iconKey 使用非预设值，例如外部 URL。
- displayName / note 为空。

预期：

- 超长、控制字符和非法 iconKey 返回 `validation_failed`。
- validation errors 为中文。
- 用户输入保留在表单中。
- 空 displayName / note 可保存，表示回退默认展示。
- metadata 文件不写入非法值。

## 9. no_change 测试

使用 EDITOR 或 OWNER：

1. 打开设备详情。
2. 进入编辑。
3. 不修改任何字段直接保存。

预期：

- 返回 `no_change` 或 `changed=false`。
- 页面显示“没有变更”。
- 不产生误导性的成功配置变更。

## 10. Audit 测试

执行一次成功保存、一次 validation 失败、一次权限拒绝。

预期：

- audit log 位于 `<world-save-root>/tzz/webadmin/web_admin_audit.log`。
- 成功记录包含 actor、role、operationType、targetId、changed fields 和 result。
- 失败记录包含 errorCode 和安全摘要。
- 不记录明文密码、passwordHash、passwordSalt、session token 或 cookie value。

## 11. Realtime 测试

同时打开两个浏览器 session：

1. A 使用 EDITOR / OWNER 打开设备详情并保存 metadata。
2. B 打开同一设备详情页。

预期：

- B 收到 `device_config_changed` 后静默更新。
- 不全页 reload。
- 不跳顶部。
- 不丢筛选、排序或折叠状态。
- 其他无关页面不被强制刷新。

## 12. 游戏逻辑不变

保存 displayName / note / iconKey 后回归：

- Signal emitter 仍按原 channel 触发。
- Signal receiver / action_relay 联动不变。
- virtual_block_device interaction / itemSubmit / cooldown 行为不变。
- `/tzz signal device debug` 输出不因 WebAdmin metadata 写入而报错。
- `/tzz signal doctor` 输出正常。

## 13. 只读页面无回归

逐页烟测：

- `/app#/dashboard`
- `/app#/devices`
- `/app#/signals`
- `/app#/doctor`
- `/app#/history`
- `/app#/users`
- `/app#/settings`
- `/app#/regions`
- `/app#/actions`

预期所有页面仍可打开，时间格式、中文显示、sidebar 固定、详情页上下文返回和 realtime 状态正常。

## 14. 明确未开放内容

确认页面没有出现可执行入口：

- 编辑 enabled。
- 编辑 channel。
- 编辑 itemSubmit / matcher / consume。
- 编辑 action / command action。
- 编辑 region bounds。
- 编辑 WebAdmin 用户。
- 编辑系统设置。
- 执行 signal 或 action。
