# Scheduler Capability Matrix 8.12

## 范围

8.12 Scheduler / Delay / Timer 提供通用时间轴能力，不把 Scheduler 绑死在 Signal/channel 上。Timer 可以由 ActionEngine 调用，也可以由 WebAdmin 手动操作。`outputChannel` 可选，只是兼容 SignalBridge 的输出方式。

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| TimerDefinition | 已实现 | world-scoped 配置模型 |
| TimerStore | 已实现 | `<world-save-root>/tzz/webadmin/timers.json` |
| Runtime active state | 已实现 | 内存态，服务器停止后清空 |
| Runtime definition cache | 已实现 | server start / WebAdmin Timer 读写刷新；runtime action 不重复读 `timers.json` |
| DELAY | 已实现 | durationTicks 后执行 onCompleteActions / outputChannel |
| COUNTDOWN | 已实现 | remainingTicks + interval onTickActions + complete |
| REPEAT | 已实现 | interval onTickActions；status 暴露 nextFireInTicks / remainingRuns；maxRuns=0 表示直到取消 |
| GLOBAL scope | 已实现 | 每个 timer 一个全局运行实例 |
| PLAYER scope | 已实现 | 按 playerId 隔离运行实例 |
| RESTART | 已实现 | 运行中再次 start 会重启 |
| IGNORE_IF_RUNNING | 已实现 | 运行中再次 start 为 no-op success |
| FAIL_IF_RUNNING | 已实现 | 运行中再次 start 失败 |
| timer_start | 已实现 | 受控 ActionEngine action |
| timer_cancel | 已实现 | 受控 ActionEngine action |
| onStartActions | 已实现 | 启动时执行，可为空 |
| onTickActions | 已实现 | COUNTDOWN / REPEAT interval 执行 |
| onCompleteActions | 已实现 | 完成时直接执行 action list |
| onCancelActions | 已实现 | cancel 时执行；不执行 onCompleteActions |
| outputChannel 可选 | 已实现 | complete 时可 emit SignalBridge |
| per-action gate | 已实现 | TimerActionExecutor 逐条评估 single action gate |
| WebAdmin Timer API | 已实现 | CRUD/status/start/cancel/reset |
| WebAdmin Timer UI | 已实现 | `#/timers` 和 `#/timers/{id}` |
| Doctor | 已实现 | invalid / missing / disabled / player context / repeat risk |
| audit / realtime | 已实现 | config 和 runtime 写操作都会记录 |
| Logic Chain minimal producer | 已实现 | Timer outputChannel 可显示为 source/producer |

## Mode 语义

| Mode | durationTicks | intervalTicks | maxRuns | 完成行为 |
| --- | --- | --- | --- | --- |
| DELAY | 必须 >= 0 | 可为 0 | 忽略 | duration 到达后 onCompleteActions + outputChannel |
| COUNTDOWN | 必须 >= 0 | 必须 > 0 | 忽略 | interval 执行 onTickActions，duration 到达后 complete |
| REPEAT | 可为 0 | 必须 > 0 | 0 表示无限 | interval 执行 onTickActions，达到 maxRuns 后 complete |

## Action 语义

| Action | 必填字段 | 可选字段 | 失败边界 |
| --- | --- | --- | --- |
| `timer_start` | `timerId` | targetMode、targetId、startPolicyOverride、durationOverrideTicks | missing / disabled / invalid / PLAYER 缺上下文 fail closed |
| `timer_cancel` | `timerId` | targetMode、targetId、missingBehavior=`noop_success`/`fail` | missing timerId fail；Timer 不存在可 no-op；未运行按 no-op success |

`timer_start` / `timer_cancel` 不绕过既有 action list gate 或 single action gate。gate false 时不会启动或取消 Timer。

## WebAdmin

| 能力 | 路径 / marker |
| --- | --- |
| 列表 | `GET /api/webadmin/timers` |
| 详情 | `GET /api/webadmin/timers/{id}` |
| 创建 | `POST /api/webadmin/timers` |
| 更新 | `PATCH /api/webadmin/timers/{id}` |
| 删除 | `DELETE /api/webadmin/timers/{id}` / `POST /api/webadmin/timers/{id}/delete` |
| 状态 | `GET /api/webadmin/timers/{id}/status` |
| 手动启动 | `POST /api/webadmin/timers/{id}/start` |
| 手动取消 | `POST /api/webadmin/timers/{id}/cancel` |
| 重置运行态 | `POST /api/webadmin/timers/{id}/reset` |
| UI route | `#/timers` / `#/timers/{id}` |
| edit lock target | `timer_config` |

写操作必须具备 permission、CSRF / same-origin、edit lock、expectedFingerprint、validation、audit、realtime 和中文错误提示。

## Doctor

| 诊断 | 状态 |
| --- | --- |
| store degraded | ERROR |
| invalid definition | ERROR |
| timer without output/action | WARNING |
| REPEAT intervalTicks < 20 | WARNING |
| infinite repeat without clear cancel path | WARNING |
| active instance count high | WARNING |
| timer action missing timerId | ERROR |
| timer action references missing timer | ERROR |
| timer_start references disabled timer | WARNING |
| PLAYER scope action lacks player context | ERROR |

## 明确不做

- 不做 GameController / MissionSystem / PhaseController。
- 不做具体任务 / 关卡。
- 不做完整 Logic Chain Editor。
- 不做 Scratch-like editor。
- 不做 StateVariable 新 scope。
- 不做 Scheduler 持久恢复。
- 不做 cron / calendar。
- 不做多服务器调度。
- 不做 version rollback。
- 不做 raw JSON editor。
- 不做任意 NBT path。
- 不做脚本表达式。
- 不跑 MCP scenario。
- 不启动 Minecraft。
- 不生成截图矩阵。

## 测试 / Guard

| 测试 | 覆盖 |
| --- | --- |
| `TimerStoreTest` | store roundtrip、bad file fallback、validation、fingerprint |
| `TimerRuntimeServiceTest` | DELAY / COUNTDOWN / REPEAT、cancel-before-complete、infinite repeat until cancel、PLAYER missing context、repeat status、action failure recording、due budget、active-limit existing-scope policy |
| `TimerActionExecutionTest` | timer_start / timer_cancel config、summary、result details、ActionValidator failures、WebAdmin timer action DTO roundtrip、ActionEngine dispatch/source DTO markers |
| `WebAdminTimerServiceTest` | create/detail/update/delete/status/start/cancel/reset、validation、permission、CSRF、same-origin、edit lock、expectedFingerprint conflict、audit、realtime |
| `TimerDoctorTest` | no-output、repeat risk、missing/disabled/player-context action references |
| `StabilizationGuardTest.testSchedulerDelayTimer812` | docs、runtime、WebAdmin、Doctor、Logic Chain、no out-of-scope |
| `LocalTestMcpFoundationGuardTest` | 不新增 Timer / Scheduler MCP tooling |
