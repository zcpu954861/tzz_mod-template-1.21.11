# 8.10 Signal Join / Barrier / Aggregator 当前上下文

## 8.10 目标

8.10 在现有 SignalBridge、Logic Chain Viewer、Condition runtime gate 与 Doctor 之上，补齐多事件汇合能力：

```text
A 信号到达
B 信号到达
A + B 满足后
发出 C 信号
```

本阶段实现底层 Signal Join / Barrier / Aggregator，不实现 GameController、MissionSystem、PhaseController 或具体小游戏任务。

## 概念边界

- Join：多个 input channel 共同驱动一个 output channel。
- Barrier：在 ALL / ANY_N 等条件满足前阻止 output signal 产生，但不阻断原始 input signal。
- Aggregator：COUNT 模式下累计匹配输入事件次数，到达 threshold 后输出。

Join 是 SignalBridge 的 passive observer：

- 原始 signal 仍按旧逻辑进入 receiver、ActionRelay、SignalListener、history。
- Join 只观察已经 accepted 的 signal。
- Join 满足后通过 `SignalBridgeServer.emit` 发出 output signal。
- 未配置 Join 时旧 SignalBridge / listener / relay / region / action gate 语义不变。

## 定义模型

8.10 新增：

- `SignalJoinDefinition`
- `SignalJoinInputDefinition`
- `SignalJoinRuntimeState`
- `SignalJoinStatusSnapshot`
- `SignalJoinMode`
- `SignalJoinScopeMode`
- `SignalJoinResetPolicy`
- `SignalJoinStore`
- `SignalJoinRuntimeService`
- `WebAdminSignalJoinService`

定义字段包括：

- `id`
- `displayName`
- `note`
- `enabled`
- `inputChannels`
- `outputChannel`
- `mode`
- `threshold`
- `scopeMode`
- `resetPolicy`
- `timeoutTicks`
- `cooldownTicks`
- `version`
- `fingerprint`
- `createdAt`
- `updatedAt`
- `updatedBy`

`inputChannels` 至少包含 channel，可带 displayName / note。`requiredCount` 是模型预留字段，8.10 runtime 不启用 per-input count；COUNT 模式只按 Join 级别的 `threshold` 累计匹配输入事件。

## 持久化与运行态

Join 配置持久化到 world-scoped 文件：

```text
<world-save-root>/tzz/webadmin/signal_joins.json
```

也就是 WebAdmin 世界级目录下的：

```text
tzz/webadmin/signal_joins.json
```

runtime pending state 是内存态，不持久化：

- 服务器重启后 pending state 清空。
- `LATCH_UNTIL_MANUAL_RESET` 的锁存状态也是运行期内存态。
- 配置保存、删除或禁用后会清理对应 Join 的 runtime state。

这样避免高频输入窗口写 JSON，也避免把 Join 内部窗口状态混入 StateVariable 或 ConditionEngine。

## mode 语义

8.10 支持：

- `ALL`：所有 distinct input channel 至少触发一次后输出。
- `ANY_N`：任意 N 个 distinct input channel 触发后输出；`threshold` 必须在 `1..inputCount`。
- `COUNT`：匹配 input channel 的累计事件次数达到 `threshold` 后输出；重复同一 channel 会计数。

## scopeMode 语义

8.10 支持：

- `GLOBAL`：所有输入事件共享一个 pending state。
- `PLAYER`：按 player UUID 隔离 pending state。

PLAYER scope 如果收到没有玩家上下文的 signal：

- 不创建 pending state。
- 不 emit output signal。
- 记录中文 diagnostic。
- Doctor 会提示该 Join 依赖玩家上下文。

## resetPolicy 语义

- `RESET_AFTER_EMIT`：满足条件并输出后清空当前 scope 的 pending state，可继续下一轮累计。
- `LATCH_UNTIL_MANUAL_RESET`：首次满足条件后输出并锁存；手动 reset 前不会重复输出。
- WebAdmin/API reset 只清理 runtime state，不删除配置。

## timeoutTicks lazy timeout

`timeoutTicks <= 0` 表示不启用超时。

`timeoutTicks > 0` 时采用 lazy timeout：

- 不启动后台 tick scanner。
- 不引入 Scheduler。
- 不发 failure channel。
- 只在下一次相关 input event 或 status 查询时清理过期 pending state。
- timeout reset 会记录中文 diagnostic。

## cooldownTicks 输出冷却

`cooldownTicks <= 0` 表示不启用输出冷却。

`cooldownTicks > 0` 时，Join 在上次 output signal 后进入输出冷却窗口：

- 冷却期间新的匹配 input 会被忽略。
- 冷却期间不累计 pending state，也不发出 output signal。
- 冷却不启动后台 tick scanner。
- WebAdmin 主 label 使用“输出冷却 tick”，`cooldownTicks` 只作为技术字段副文本。

## SignalBridge 接入点

`SignalBridgeServer.emit` 在 accepted signal 记录 history 后调用：

```text
SignalJoinRuntimeService.observeAcceptedSignal(event, channel, depth)
```

该接入覆盖没有 listener 的 input channel，因此 Join 可以观察“只有 Join 消费”的 signal。

output signal 使用：

```text
ActionSourceType.SIGNAL_JOIN
sourceId = joinId
detail = joinId / mode / scopeKey / matchedInputs / triggerCount
```

output 继续走 `SignalBridgeServer.emit`，因此自然进入 history、debugger、receiver、relay、listener 和既有 recursion guard。

## self-loop / recursion

8.10 的保护包括：

- validation 拒绝 `outputChannel == inputChannel`。
- Doctor 诊断明显互相输出 / 输入的 Join cycle 风险。
- runtime 使用 Join active path 避免同一个 Join 在输出链路中递归自触发。
- output signal 继续走 SignalBridge depth guard。

## WebAdmin API

新增 API：

```text
GET /api/webadmin/signal-joins
GET /api/webadmin/signal-joins/{id}
POST /api/webadmin/signal-joins
PATCH /api/webadmin/signal-joins/{id}
DELETE /api/webadmin/signal-joins/{id}
POST /api/webadmin/signal-joins/{id}/delete
GET /api/webadmin/signal-joins/{id}/status
POST /api/webadmin/signal-joins/{id}/reset
```

写操作全部走：

- permission
- CSRF / same-origin
- edit lock：`signal_join_config`
- expectedFingerprint
- validation
- `WebAdminWriteResult`
- audit
- realtime：`signal_join_changed`

## WebAdmin UI

新增入口：

```text
#/signal-joins
#/signal-joins/{id}
```

UI 提供：

- 列表页：名称、enabled、mode、input count、output channel、scopeMode、pending scopes、last result。
- 详情页：配置摘要、input channels、output channel、runtime status、逻辑链入口。
- create / edit modal：结构化字段，不提供 raw JSON editor。
- delete modal：不要求输入完整 ID/name。
- reset runtime state：只清 runtime state，不删除配置。
- realtime silent refresh：不清空草稿，不关闭 modal，不跳顶。

## Logic Chain Viewer

8.10 让 Join 在只读 Logic Chain Viewer 中可见：

- input channel 展开时，Join 显示为 consumer。
- Join 节点展示 mode、threshold、scope、resetPolicy、timeoutTicks、input channels、outputChannel 和 runtime status。
- Join outputChannel 作为 downstream channel 继续展开。
- output channel 展开时，Join 显示为 producer/source。
- 不做完整 Logic Chain Editor，不保存 runtime 图结构。

## Doctor

Doctor 新增诊断：

- disabled Join。
- invalid Join config。
- input channel empty。
- output channel empty。
- duplicate input channels。
- outputChannel 等于 inputChannel。
- ANY_N threshold invalid。
- COUNT threshold invalid。
- PLAYER scope 但 signal 可能没有 player context。
- timeoutTicks 过小。
- Join 间互相输出 / 输入的循环风险。

Doctor 不自动修复配置。

## 不做内容

8.10 明确不做：

- 不做 GameController。
- 不做 MissionSystem。
- 不做 PhaseController。
- 不做具体任务 / 关卡。
- 不做 Scheduler。
- 不做后台 tick 扫描。
- 不做 Controlled State Actions。
- 不做完整 Logic Chain Editor。
- 不做 Scratch editor。
- 不做 SignalReceiver gate。
- 不做 failure action / failure channel。
- 不做 per-input conditionGroup。
- 不做 raw JSON editor。
- 不改写 8.6 / 8.7 / 8.9 gate 语义。
