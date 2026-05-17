# SignalBridge Capability Matrix 8.10

8.10 新增 Signal Join / Barrier / Aggregator。它是 SignalBridge 之上的 passive observer，不改变原始 signal 的 receiver / ActionRelay / SignalListener / history 语义。

## Runtime

| 能力 | 状态 | 说明 |
|---|---|---|
| Signal Join 定义模型 | 已实现 | `SignalJoinDefinition` + `SignalJoinInputDefinition`。 |
| world-scoped config | 已实现 | `<world-save-root>/tzz/webadmin/signal_joins.json`。 |
| runtime state 内存态 | 已实现 | pending / latched state 不持久化，服务器重启清空。 |
| passive observer | 已实现 | accepted signal 记录 history 后观察，不阻断原 signal。 |
| no listener input | 已实现 | input channel 没有 listener 时仍可被 Join 观察。 |
| output signal | 已实现 | 通过 `SignalBridgeServer.emit` 发出，`ActionSourceType.SIGNAL_JOIN`。 |
| output metadata | 已实现 | detail 包含 joinId、mode、scope、matched inputs、totalCount、depth。 |
| recursion guard | 已实现 | 拒绝 self-output，runtime active path + SignalBridge depth guard。 |

## Modes

| mode | 状态 | 语义 |
|---|---|---|
| `ALL` | 已实现 | 所有 distinct input channel 至少触发一次后输出。 |
| `ANY_N` | 已实现 | 任意 N 个 distinct input channel 触发后输出。 |
| `COUNT` | 已实现 | 匹配 input channel 的累计事件次数达到 threshold 后输出。 |

## Scope

| scopeMode | 状态 | 语义 |
|---|---|---|
| `GLOBAL` | 已实现 | 所有输入共享一个 pending state。 |
| `PLAYER` | 已实现 | 按 player UUID 隔离；缺 player context 时 no-op 并记录中文 diagnostic。 |
| `TEAM / REGION / CUSTOM_KEY` | 不做 | 后续阶段再评估。 |

## Reset / Timeout

| 能力 | 状态 | 说明 |
|---|---|---|
| `RESET_AFTER_EMIT` | 已实现 | 输出后清空 pending state，可重复触发。 |
| `LATCH_UNTIL_MANUAL_RESET` | 已实现 | 输出后锁存，手动 reset 前不重复输出。 |
| manual reset | 已实现 | WebAdmin/API reset 清 runtime state，不删除配置。 |
| lazy timeout | 已实现 | `timeoutTicks > 0` 时下一次事件或 status 查询清理过期 pending state。 |
| output cooldown | 已实现 | `cooldownTicks > 0` 时 output 后进入冷却；冷却期间忽略新的 Join 输入，不发 output。 |
| Scheduler / tick scanner | 不做 | 8.10 不引入 Scheduler，也不后台扫描 Join。 |
| failure channel | 不做 | timeout 不自动发 failure signal。 |

## WebAdmin API / UI

| 能力 | 权限 | 状态 | 说明 |
|---|---|---|---|
| `GET /api/webadmin/signal-joins` | VIEWER | 已实现 | 列表 + status 摘要。 |
| `GET /api/webadmin/signal-joins/{id}` | VIEWER | 已实现 | 详情 + lockStatus + runtime status。 |
| `POST /api/webadmin/signal-joins` | EDITOR/OWNER | 已实现 | create，走写入安全链路。 |
| `PATCH /api/webadmin/signal-joins/{id}` | EDITOR/OWNER | 已实现 | update，expectedFingerprint 冲突检测。 |
| `DELETE /api/webadmin/signal-joins/{id}` | EDITOR/OWNER | 已实现 | delete，保留兼容 REST 路径。 |
| `POST /api/webadmin/signal-joins/{id}/delete` | EDITOR/OWNER | 已实现 | delete modal 使用，不要求输入完整 ID/name。 |
| `GET /api/webadmin/signal-joins/{id}/status` | VIEWER | 已实现 | 查询内存 runtime state。 |
| `POST /api/webadmin/signal-joins/{id}/reset` | EDITOR/OWNER | 已实现 | manual reset，audit + realtime。 |
| `#/signal-joins` | WebAdmin | 已实现 | 列表页。 |
| `#/signal-joins/{id}` | WebAdmin | 已实现 | 详情页。 |
| raw JSON editor | 不做 | 不提供 Signal Join raw JSON editor。 |

写操作使用：

- `signal_join_config` edit lock。
- `EDIT_SIGNAL_JOIN` permission。
- CSRF / same-origin。
- expectedFingerprint。
- validation。
- audit。
- `signal_join_changed` realtime。

## Logic Chain / Doctor / History

| 能力 | 状态 | 说明 |
|---|---|---|
| input channel consumer | 已实现 | Join 在 input channel 段显示为 consumer。 |
| output channel producer | 已实现 | Join 在 output channel 段显示为 producer/source。 |
| downstream output expansion | 已实现 | Join consumer 节点用 `join_output` 边连接 output channel。 |
| runtime status display | 已实现 | Logic Chain 节点和 Signal Join 详情展示 pending scopes / lastResult。 |
| history visibility | 已实现 | output signal 进入 Signal history，sourceType 为 `signal_join`。 |
| Doctor validation | 已实现 | 扫描 invalid / duplicate / self-output / threshold / PLAYER scope / timeout / cycle risk。 |
| full Logic Chain Editor | 不做 | 8.10 只读接入，不做图编辑器。 |

## 测试覆盖

| 测试 | 覆盖 |
|---|---|
| `SignalJoinBarrierAggregatorTest` | Store roundtrip / bad file fallback、ALL、ANY_N、COUNT、GLOBAL/PLAYER、missing player diagnostic、latch、manual reset、lazy timeout、validation。 |
| `WebAdminSignalJoinServiceTest` | WebAdmin Signal Join 写入服务、raw request validation、expectedFingerprint、reset confirm / fingerprint、VIEWER 禁写。 |
| `StabilizationGuardTest.testSignalJoinBarrierAggregator810` | 文件、docs、API/UI marker、SignalBridge 接入、Logic Chain、Doctor、no out-of-scope guard。 |

## Scope Guard

8.10 不做：

- GameController
- MissionSystem
- PhaseController
- 具体任务 / 关卡
- Scheduler
- Controlled State Actions
- full Logic Chain Editor
- Scratch editor
- SignalReceiver gate
- failure action/channel
- per-input condition group
- raw JSON editor
- MCP scenario
- 启动 Minecraft
- 截图矩阵
