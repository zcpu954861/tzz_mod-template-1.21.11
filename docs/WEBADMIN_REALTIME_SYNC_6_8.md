# 6.8 WebAdmin 实时同步基础报告

## 1. 版本定位

6.8 是 WebAdmin Realtime Sync Foundation，中文定位为 WebAdmin 实时同步基础。本阶段目标是让服务器端的低成本状态变化主动通知 WebAdmin 前端，为后续实时 Dashboard、配置编辑协作、编辑锁、草稿 / 发布 / 回滚打基础。

6.8 仍是只读阶段：

- 不新增写 API。
- 不新增配置编辑。
- 不新增设备 / Signal / Region / Action / 用户写操作。
- 不做自动修复。
- 不做全站轮询。
- 不引入 npm、前端构建链或大型框架。

## 2. 技术方案

当前实现采用 Server-Sent Events / Event Stream，而不是 WebSocket。

选择原因：

- 当前 WebAdmin 基于 JDK 内置 `HttpServer`。
- SSE 可直接复用现有 HTTP server 和 session cookie 认证。
- 本阶段只需要服务端到浏览器的只读通知，不需要双向通信。
- 避免引入新的 WebSocket 依赖或复杂前端构建链。

事件连接：

```text
GET /api/realtime/events
Content-Type: text/event-stream; charset=utf-8
```

未登录或 session 无效时，连接返回 401，不允许匿名订阅。

## 3. 服务端结构

新增 realtime 组件：

- `WebAdminRealtimeEventType`
- `WebAdminRealtimeEvent`
- `WebAdminRealtimeClient`
- `WebAdminRealtimeEventBus`
- `WebAdminRealtimeService`

`WebAdminServer` 只新增认证后的 realtime route 分发：

```text
GET /api/realtime/events
```

服务器停止时会关闭所有 realtime client，释放连接资源。

## 4. 事件模型

事件字段：

- `id`
- `type`
- `occurredAt`
- `channel`
- `deviceId`
- `regionId`
- `actionId`
- `sourceType`
- `severity`
- `summary`
- `routeTarget`
- `payload`

事件原则：

- 只推轻量信息。
- 不推完整 devices / history / doctor DTO。
- 不推用户列表。
- 不推业务 JSON 原文。
- 不包含 password hash、salt、session token、cookie value。

## 5. 已真实接入事件

当前已接入：

- `realtime_connected`
- `heartbeat`
- `webadmin_user_connected`
- `webadmin_user_disconnected`
- `signal_emitted`
- `history_appended`

其中 `signal_emitted` / `history_appended` 来自 `SignalEventHistory.record`。Signal history 追加后发布轻量事件，包含 channel、sourceType、sourceId、playerName、result、listener/executed/failed count 等摘要信息。

## 6. 预留但未接入事件

已预留类型：

- `device_updated`
- `doctor_changed`
- `action_executed`
- `receiver_pulse`
- `region_event`
- `config_changed`

暂未接入原因：

- 这些路径分散在 device store、receiver/action relay、RegionController、ActionEngine 和 Doctor 计算中。
- 贸然接入可能改变 5.x 已验收路径或引入高频重复通知。
- 后续应在对应服务层稳定后逐步接入，保持轻量事件，不推全量 DTO。

## 7. 前端 route filtering

前端在 `WebAdminFrontendAssets.appJs()` 中新增：

- `connectRealtime`
- `closeRealtime`
- `handleRealtimeEvent`
- `shouldHandleRealtimeEvent`
- `scheduleRealtimeRefresh`

规则：

- Dashboard 处理 Signal / history / doctor / device / session 相关事件。
- History 处理 `signal_emitted` / `history_appended`。
- Signal 列表处理 Signal / history / doctor 相关事件。
- Signal 详情只处理当前 channel 匹配事件。
- Device 详情只处理当前 deviceId 匹配事件。
- Doctor 处理 doctor/device/signal/history hint。
- Region / Action 页面预留对应 regionId / actionId 匹配逻辑。
- Users / Settings 处理 WebAdmin 用户连接/断开和 config_changed hint。

如果事件与当前 route 无关，前端忽略，不刷新页面。

## 8. 刷新策略

6.8 使用“轻量事件通知 + 当前页面局部 refetch”的稳定策略。

- 不全页 reload。
- 不全站刷新。
- 不在后台轮询全部页面。
- 同一路由事件在 700ms 内合并。
- 浏览器标签页 hidden 时不执行高成本 DOM 刷新，只记录当前匹配 route 的 dirty 标记。
- 页面 visible 后只对当前匹配 route 执行静默局部刷新。
- 静默刷新不全页 reload，不重建 sidebar/topbar/app shell。
- 静默刷新保留主内容滚动位置、当前焦点、筛选条件、排序和 `details` 折叠状态。
- 如果刷新期间又收到同一路由新事件，旧响应不会覆盖当前页面，会等待下一轮 dirty 刷新。

## 9. 性能边界

服务端：

- 不扫描世界。
- 不扫描区块。
- 不扫描背包。
- 不强制加载区块。
- 不每 tick 扫描 WebAdmin 状态。
- 不为每个客户端重复构建完整 DTO。
- 每个 client 有有界队列。
- 慢客户端会丢弃旧事件，不拖垮服务端。

前端：

- 使用 EventSource 自动保持连接。
- 断开后使用指数退避重连。
- 不使用全局 `setInterval` 做全站刷新。
- 只在相关事件发生时刷新当前 route 对应数据。

## 10. 安全边界

- realtime 连接必须通过 WebAdmin session cookie 认证。
- 未登录不能连接。
- session 过期时前端会停止正常处理并回到登录流程。
- 事件 payload 不包含密码、hash、salt、session token、cookie value。
- 事件权限当前与 6.x 只读 API 一致。
- 不接受客户端写消息。

## 11. 测试护栏

`stabilizationGuardTest` 新增覆盖：

- realtime event type id / displayName 非空。
- event JSON 序列化不包含敏感字段。
- event bus subscribe / publish / unsubscribe 基本流程。
- frontend assets 中存在 realtime helper。
- frontend assets 中存在 `/api/realtime/events`。
- frontend 不使用 `setInterval(` 做全局轮询。

## 12. 后续建议

后续可分阶段推进：

- 接入 `device_updated`、`doctor_changed`、`action_executed`、`receiver_pulse`、`region_event`。
- 为 Dashboard / History / Signal 详情做更细粒度 DOM patch，减少当前页面 refetch。
- 设计 WebAdmin Event Stream 状态面板和诊断工具。
- 在进入配置编辑前，设计 service 层写操作、权限审计、编辑锁、草稿 / 发布 / 回滚。
- WebSocket 可在需要双向协作时单独评估，不应替代当前只读 SSE 基础。
