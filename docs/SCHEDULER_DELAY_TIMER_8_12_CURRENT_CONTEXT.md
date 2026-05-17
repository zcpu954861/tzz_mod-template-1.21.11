# 8.12 Scheduler / Delay / Timer 当前上下文

## 8.12 目标

8.12 Scheduler / Delay / Timer 的目标是给 Signal / Action / State / Join 系统补上通用时间轴能力：

```text
什么时候执行
延迟多久执行
是否重复执行
倒计时结束后做什么
如何取消一个正在等待的 Timer
```

本阶段新增的是通用 Scheduler / Timer 能力，不是只能通过 Signal/channel 使用的功能。Timer 可以由 `timer_start` action 启动，由 `timer_cancel` action 取消，也可以通过 WebAdmin 手动启动 / 取消 / 重置运行态。`outputChannel` 可选，只用于兼容现有 SignalBridge 链路；直接的 `onCompleteActions` / `onTickActions` 才是 Timer 自身的动作入口。

## 模型

核心模型：

- `TimerDefinition`
- `TimerRuntimeInstance`
- `TimerStatusSnapshot`
- `TimerStore`
- `TimerRuntimeService`
- `TimerActionExecutor`
- `TimerMode`
- `TimerScopeMode`
- `TimerStartPolicy`

`TimerDefinition` 主要字段：

- `id`
- `displayName`
- `note`
- `enabled`
- `mode`
- `scopeMode`
- `durationTicks`
- `intervalTicks`
- `maxRuns`
- `startPolicy`
- `onStartActions`
- `onTickActions`
- `onCompleteActions`
- `onCancelActions`
- `outputChannel`
- `createdAt`
- `updatedAt`
- `updatedBy`
- `version`

## 持久化与运行态

Timer 配置持久化到 world-scoped 文件：

```text
<world-save-root>/tzz/webadmin/timers.json
```

Active timer runtime state 是内存态：

- 服务器停止后清空。
- 配置保存、删除或禁用相关 Timer 时清空对应运行中实例。
- WebAdmin reset 只清运行态，不删除配置。
- 不把运行态写入 `StateVariable`，不写全局 config。

读取损坏的 `timers.json` 会进入 degraded 状态，写入和运行安全失败，并返回中文诊断，避免覆盖损坏文件。

## Timer modes

### DELAY

```text
start timer
等待 durationTicks
执行 onCompleteActions
可选 emit outputChannel
结束
```

### COUNTDOWN

```text
start countdown
每 intervalTicks 执行 onTickActions
durationTicks 到达后执行 onCompleteActions
可选 emit outputChannel
结束
status 显示 remainingTicks
```

### REPEAT

```text
每 intervalTicks 执行 onTickActions
maxRuns > 0 时达到次数后执行 onCompleteActions 并结束
maxRuns = 0 时持续重复直到 timer_cancel 或 WebAdmin 手动取消 / reset
```

`REPEAT` 的 status 中 `remainingTicks` / `nextFireInTicks` 表示距离下一次 interval 触发的 tick 数，`remainingRuns` 表示剩余重复次数；无限重复的 `remainingRuns = -1`。

`COUNTDOWN` / `REPEAT` 的 `intervalTicks` 必须大于 0。Doctor 会提示 `REPEAT intervalTicks < 20` 的高频风险，但本阶段允许高级用户配置 tick 级轻量逻辑。

## scopeMode

8.12 支持：

- `GLOBAL`：同一个 Timer definition 只有一个全局 active instance。
- `PLAYER`：按 playerId 隔离 active instance。

PLAYER scope 使用 `context_player` 时必须有触发玩家。缺玩家上下文时 start / cancel fail closed，返回中文 failureReason，不创建运行态，不执行 action，不发 signal。

后续 deferred：

- TEAM
- REGION
- DEVICE
- CUSTOM_KEY
- GAME / MISSION

## startPolicy

8.12 支持：

- `RESTART`：运行中再次启动时重新从初始时间开始。
- `IGNORE_IF_RUNNING`：运行中再次启动时保持现有实例，返回 no-op success。
- `FAIL_IF_RUNNING`：运行中再次启动时失败，返回中文 reason。

## Timer actions

新增 ActionEngine action type：

- `timer_start`
- `timer_cancel`

`timer_start` 字段：

- `timerId`
- `timerTargetMode`
- `timerTargetId`
- `timerStartPolicyOverride`
- `timerDurationOverrideTicks`

`timer_cancel` 字段：

- `timerId`
- `timerTargetMode`
- `timerTargetId`
- `timerMissingBehavior`：`noop_success` / `fail`，默认 `noop_success`

运行语义：

- 缺 `timerId`：validation fail。
- Timer 不存在：start / cancel 返回中文失败。
- Timer disabled：start fail，中文 reason。
- `timer_cancel` 引用已删除 Timer 时，`noop_success` 返回 no-op success，`fail` 返回中文失败。
- cancel 不执行 `onCompleteActions`。
- cancel 可以执行 `onCancelActions`。
- `timer_start` / `timer_cancel` 仍受 list-level gate 和 single action gate 约束；gate false 时不会启动或取消 Timer。

## onTickActions / onCompleteActions

Timer action list 使用现有 `ActionConfig` 和 `ActionEngine`。Timer 专用 executor 会逐条执行 action，并在每条 action 前评估 single action gate；不能直接用 `ActionEngine.executeAll` 跳过 per-action gate。

支持的动作包括现有 command / signal / message / sound / `state_variable` / `timer_start` / `timer_cancel`。

失败语义：

- onTick / onComplete action 失败会记录到 status / Doctor 可读的 lastFailureReason。
- Scheduler 不会因为单个 action 失败而崩溃。
- 允许纯 status timer；Doctor 会 warning，提示如果不是有意作为纯状态计时器，应配置 onTick / onComplete 动作或 outputChannel。仅配置 onStart / onCancel 不会消除该 warning。

## outputChannel

`outputChannel` 可选。Timer complete 时如果配置了 outputChannel，会走 `SignalBridgeServer.emit` 发出 signal：

- `sourceType = scheduler_timer`
- source id = timerId
- metadata/detail 包含 timerId、scope、mode、elapsedTicks、runCount

outputChannel 只是兼容现有 Signal 链，不是唯一入口。没有 outputChannel 但有 `onCompleteActions` 的 Timer 是完整可用的 Timer。

## Runtime tick

`TimerServer.register()` 使用 Fabric `ServerTickEvents.END_SERVER_TICK` 注册 `TimerRuntimeService.tick`。

Timer definitions 会在服务器启动时从 `timers.json` 刷新到内存缓存，WebAdmin Timer 读写也会刷新该缓存。`timer_start` / `timer_cancel` runtime action 使用内存缓存查 definition，不在执行路径上重复读取 `timers.json`。

运行要求：

- 不使用 `Thread.sleep`。
- 不阻塞 server thread。
- 不扫描世界。
- 不扫描所有方块 / 容器。
- 每 tick 只处理 active timer instances。
- `MAX_ACTIVE_TIMERS_PER_SERVER` 限制运行中实例数量。
- `MAX_DUE_EXECUTIONS_PER_TICK` 限制单 tick 到期执行量，超出部分延后并记录中文诊断。

## WebAdmin API

新增 Timer API：

```text
GET /api/webadmin/timers
GET /api/webadmin/timers/{id}
POST /api/webadmin/timers
PATCH /api/webadmin/timers/{id}
DELETE /api/webadmin/timers/{id}
POST /api/webadmin/timers/{id}/delete
GET /api/webadmin/timers/{id}/status
POST /api/webadmin/timers/{id}/start
POST /api/webadmin/timers/{id}/cancel
POST /api/webadmin/timers/{id}/reset
```

权限：

- VIEWER 可读 list / detail / status。
- EDITOR / OWNER 可 create / update / delete / start / cancel / reset。

写操作走：

- permission
- CSRF / same-origin
- edit lock：`timer_config`
- expectedFingerprint
- validation
- `WebAdminWriteResult`
- audit
- realtime：`timer_changed` / `timer_runtime_changed`

删除不要求输入完整 ID / name。保存失败保留用户输入。

## WebAdmin UI

新增入口：

```text
调度器 / 计时器
#/timers
#/timers/{id}
```

页面能力：

- Timer 列表：名称、enabled、mode、scopeMode、duration / interval / maxRuns、active instance count、last result。
- Timer 详情：配置摘要、status panel、onTickActions、onCompleteActions、写入安全说明。
- Timer 编辑 modal：结构化字段，不提供 raw JSON。
- mode selector：DELAY / COUNTDOWN / REPEAT，中文主文案。
- scope selector：GLOBAL / PLAYER，中文解释。
- ticks 字段显示秒换算，20 ticks = 1 秒。
- outputChannel 使用 dark combobox，可选择已有 channel，也可输入新 channel。
- action list 使用 summary card + modal，不在详情页撑爆表单。
- manual start / cancel / reset 按钮清晰。
- realtime silent refresh 不跳顶、不清空草稿、不关闭 modal。

## Doctor

Timer Doctor 诊断：

- timer store degraded。
- invalid timer definition。
- Timer 没有任何 onCompleteActions / onTickActions / outputChannel。
- REPEAT intervalTicks 太小。
- maxRuns=0 的无限 repeat 需要明确取消路径。
- active timer 数量过高。
- timer action 缺少 timerId。
- timer action 引用不存在的 Timer。
- timer_start 引用 disabled Timer。
- PLAYER scope Timer 由无 player context 的 action source 启动 / 取消。

Doctor 不自动修改配置。

## Logic Chain Viewer

8.12 只做最小只读接入：

- 有 `outputChannel` 的 Timer 在 Signal channel / Logic Chain 中显示为 producer/source。
- Timer action 启动 Timer 的可视化留到后续阶段。
- 不做完整 Logic Chain Editor。
- 不保存 runtime graph。

## 当前不做

- 不做 GameController。
- 不做 MissionSystem。
- 不做 PhaseController。
- 不做具体任务 / 关卡。
- 不做完整 Logic Chain Editor。
- 不做 Scratch-like editor。
- 不做 Signal Join 新功能。
- 不做 StateVariable 新 scope。
- 不做 Scheduler 持久恢复。
- 不做 cron / calendar。
- 不做多服务器调度。
- 不做 version rollback。
- 不做 raw JSON editor。
- 不做任意 NBT path。
- 不做任意脚本表达式。
- 不跑 MCP scenario。
- 不启动 Minecraft。
- 不生成截图矩阵。

## 测试方式

以 Java 单元测试、WebAdmin service test、Doctor test、stabilization guard 和 Local Test MCP guard 为主。

本阶段最终验证命令：

```powershell
cd tools\tzz-test-mcp
npm run build
npm test
cd ..\..
.\gradlew.bat clean build
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
git diff --check
```
