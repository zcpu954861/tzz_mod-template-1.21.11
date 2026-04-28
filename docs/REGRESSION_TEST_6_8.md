# 6.8 WebAdmin 实时同步基础回归测试清单

本清单用于验证 WebAdmin realtime event stream 在只读边界内工作正常。6.8 不新增写 API、不做配置编辑、不做 WebSocket 双向通信。

## 1. 构建与护栏

执行：

```powershell
git diff --check
git grep -n "6\\.0\\.1\\|6\\.0\\.2" -- README.md CHANGELOG.md docs src
./gradlew.bat clean build
./gradlew.bat stabilizationGuardTest --rerun-tasks
```

预期：

- 构建通过。
- `stabilizationGuardTest` 输出 `Stabilization guard checks passed.`
- realtime event type、event bus、frontend realtime helper 和 `/api/realtime/events` route 被 guard 覆盖。
- 不出现旧 hotfix 阶段编号残留。

## 2. 登录后建立连接

准备：

```mcfunction
/tzz webadmin status
/tzz webadmin user create admin OWNER
```

步骤：

1. 浏览器访问 WebAdmin。
2. 使用 OWNER 用户登录。
3. 进入 `/app#/dashboard`。
4. 打开浏览器 Network，查看 `/api/realtime/events`。

预期：

- `/api/realtime/events` 返回 `text/event-stream`。
- topbar 显示“实时同步：已连接”。
- 能收到 `realtime_connected` 或 `heartbeat`。

## 3. 未登录不能连接

步骤：

1. 登出 WebAdmin。
2. 直接访问 `/api/realtime/events`。

预期：

- 返回 401。
- 不建立匿名事件流。
- 不返回 session token、cookie value 或 stacktrace。

## 4. 登出后连接关闭

步骤：

1. 登录 WebAdmin 并确认事件流已连接。
2. 点击退出登录。

预期：

- 前端关闭 EventSource。
- 页面回到登录页。
- 后续不再继续处理 realtime 事件。

## 5. Dashboard 事件处理

步骤：

1. 登录后进入 `/app#/dashboard`。
2. 在游戏内执行：

```mcfunction
/tzz signal emit webadmin.realtime.test
```

预期：

- 事件流收到 `signal_emitted` / `history_appended`。
- Dashboard 当前页面被节流刷新或更新最近事件。
- 页面不全局 reload。
- 不刷新无关页面。

## 6. History 收到新 Signal

步骤：

1. 打开 `/app#/history`。
2. 执行：

```mcfunction
/tzz signal emit webadmin.realtime.history
```

预期：

- History 页面收到相关事件。
- 当前列表刷新或显示新事件。
- 时间显示为 `YYYY-MM-DD HH:mm:ss` 或相对时间，不显示 ISO 原始字符串。

## 7. Signal 详情 route filtering

步骤：

1. 打开 `/app#/signals/webadmin.realtime.a`。
2. 执行：

```mcfunction
/tzz signal emit webadmin.realtime.b
```

预期：

- 不刷新当前 `webadmin.realtime.a` 详情。

继续执行：

```mcfunction
/tzz signal emit webadmin.realtime.a
```

预期：

- 当前 Signal 详情刷新或最近事件更新。

## 8. Device 详情 route filtering

步骤：

1. 打开某个设备详情页 `/app#/devices/<deviceId>`。
2. 触发与该设备无关的 Signal。

预期：

- 设备详情不因无关事件刷新。
- 如果后续事件包含同一 `deviceId`，设备详情才刷新。

说明：6.8 当前真实接入的是 Signal history 事件，很多 Signal 事件可能没有 deviceId；无 deviceId 时设备详情会忽略。

## 9. Doctor 页面

步骤：

1. 打开 `/app#/doctor`。
2. 触发一个 Signal 事件。

预期：

- 如果事件会影响 Doctor hint，Doctor 当前页面可节流刷新。
- 不出现自动修复按钮。
- 不出现配置写入行为。

## 10. 断线 / 重连

步骤：

1. 登录后确认实时同步已连接。
2. 临时停止 WebAdmin 服务或关闭服务器。
3. 重新启动并重新访问。

预期：

- 断开时 topbar 显示“正在重连”或“已断开”。
- 重连使用退避，不疯狂请求。
- 服务恢复后连接可重新建立。

## 11. 浏览器 hidden / visible

步骤：

1. 打开任一只读页面。
2. 切到其他浏览器标签页。
3. 在游戏内触发 Signal。
4. 切回 WebAdmin 页面。

预期：

- hidden 时不进行高成本页面刷新。
- hidden 期间相关事件会记录 dirty route。
- visible 后连接状态恢复，并静默刷新当前相关页面。
- 刷新后滚动位置、搜索框、筛选器、排序和已展开折叠区保持不变。
- 不出现多条重复 EventSource 连接。

## 11.1 无感刷新 / 滚动保持

步骤：

1. 打开 `/app#/history`。
2. 滚动到页面中部，并设置任意筛选条件。
3. 在游戏内连续触发 5～10 次 Signal。

预期：

- 页面不会全屏 loading。
- 页面不会跳回顶部。
- 筛选条件和排序保持不变。
- 高频事件会合并刷新，不产生明显 API 风暴。

## 12. 只读边界

确认全站仍不存在：

- 新增 / 编辑 / 删除设备。
- 修改 channel。
- Signal emit 按钮。
- 新增 / 删除 / 修改 listener、receiver、action_relay。
- 新增 / 编辑 / 删除 region。
- 新增 / 编辑 / 删除 / 执行 action。
- 用户写操作。
- 系统设置写操作。
- 配置保存。
- WebSocket 双向写操作。

## 13. 安全检查

测试项：

- realtime event payload 不包含 passwordHash。
- realtime event payload 不包含 passwordSalt。
- realtime event payload 不包含 sessionId。
- realtime event payload 不包含 cookie value。
- 错误响应不暴露 Java stacktrace。
- WebAdmin 持久化目录仍为 `<world-save-root>/tzz/webadmin/`。
