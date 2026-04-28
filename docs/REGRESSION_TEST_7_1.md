# 7.1 回归测试清单

## 1. 基线

```text
./gradlew.bat clean build
./gradlew.bat stabilizationGuardTest --rerun-tasks
```

预期输出：

```text
Stabilization guard checks passed.
```

## 2. 权限

1. 使用 OWNER 登录 WebAdmin，打开设备详情页。
2. 点击“编辑显示信息”，预期可进入编辑模式。
3. 使用 EDITOR 登录，重复上述步骤，预期可编辑。
4. 使用 TESTER 登录，预期看不到可执行编辑入口或显示权限不足。
5. 使用 VIEWER 登录，预期看不到可执行编辑入口或显示权限不足。

## 3. 编辑锁

1. 用户 A 以 EDITOR 或 OWNER 打开设备详情并进入编辑模式。
2. 用户 B 打开同一设备详情，预期显示“某用户正在编辑”。
3. 用户 B 点击编辑，预期获取锁失败。
4. 用户 A 点击取消，预期释放锁。
5. 用户 B 再次点击编辑，预期可获得锁。
6. 用户 A 关闭浏览器或断线，等待锁 TTL 后，用户 B 应可重新获取锁。

## 4. 版本冲突

1. 用户 A 打开设备详情，进入编辑模式。
2. 用户 B 在 A 保存前完成一次 metadata 修改，使 version 递增。
3. 用户 A 基于旧 version 保存。
4. 预期返回 `conflict_detected`，A 的输入保留，B 的修改不被覆盖。

## 5. 保存行为

1. 保存 displayName / note / iconKey，预期保存成功并退出编辑模式。
2. 重复保存相同内容，预期返回 no-change 或等价提示。
3. 提交超长 displayName，预期 validation_failed。
4. 提交超长 note，预期 validation_failed。
5. 提交非预设 iconKey，预期 validation_failed。
6. 保存失败时，表单输入不应丢失。

## 6. realtime

1. 用户 A 获取锁，用户 B 当前设备详情页应通过 realtime 看到锁状态变化。
2. 用户 A 释放锁，用户 B 应看到锁释放状态。
3. 用户 A 保存 metadata，当前设备详情页应无闪屏更新。
4. 页面不应跳到顶部，不应丢失筛选、折叠状态或 returnTo。

## 7. 只读边界

确认页面仍没有以下编辑入口：

- enabled
- channel / interactChannel / success channel / fail channel
- cooldown / redstone mode
- interactionItem
- itemSubmit / matcher / consume
- action / command action
- region bounds
- user / settings

## 8. WebAdmin 回归

检查以下页面无回归：

- `/app#/dashboard`
- `/app#/devices`
- `/app#/devices/<deviceId>`
- `/app#/signals`
- `/app#/signals/<channel>`
- `/app#/doctor`
- `/app#/history`
- `/app#/users`
- `/app#/settings`
- `/app#/regions`
- `/app#/actions`

## 9. 敏感信息

Network / realtime payload 中不应出现：

- `passwordHash`
- `passwordSalt`
- `sessionToken`
- `cookieValue`
- 明文密码
