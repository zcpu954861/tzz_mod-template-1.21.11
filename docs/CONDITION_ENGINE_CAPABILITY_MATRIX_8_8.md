# ConditionEngine Capability Matrix 8.8

8.8 在 8.6 / 8.7 runtime gate 之上补齐可观察、可诊断、可复现能力。它不新增 runtime gate target，不改变 ConditionEngine 判断语义，不新增业务功能。

## Runtime / Debugger

| 能力 | 状态 | 说明 |
|---|---|---|
| Runtime history | 已实现 | `ConditionGateHistory` 记录 configured gate 的 allowed / blocked / error，最大 200 条，内存环形缓冲。 |
| blank gate skip | 已实现 | 未配置 conditionGroupId 时不读 store、不构造 context、不 evaluate、不记录 history。 |
| History record | 已实现 | 包含 target/source/channel/group/result/failureReason/evaluatedCount/duration/contextSummary/debugTree/replay snapshot。 |
| Debug tree | 已实现 | `ConditionGateDebugNode` 从 `ConditionEvaluationResult` 派生，保留 children。 |
| Realtime event | 已实现 | `CONDITION_GATE_HISTORY_APPENDED` / `condition_gate_history_appended`。 |
| History persistence | 不做 | 8.8 history 为进程内调试缓冲，不写世界文件。 |
| world-scoped history | 不做 | DTO 标记 `worldScoped=false`；record 内有 `worldId` 用于识别。 |
| raw JSON editor | 不做 | 条件调试器只读展示，不提供 raw JSON editor。 |

## Replay / Simulation

| 能力 | 状态 | 说明 |
|---|---|---|
| Replay 只读 | 已实现 | `ConditionGateReplayService` 返回 `readOnly/noSideEffects/noLiveWorldRead`。 |
| Snapshot replay | 已实现 | 使用历史 `replayContext` 和 `definitionSnapshot`，不调用 live context builder。 |
| Allowed / blocked replay | 已实现 | 返回 originalResult / replayResult / resultConsistent / debugTree。 |
| Changed group warning | 已实现 | 当前 definition fingerprint 不同则 `fingerprintChanged=true` 并返回中文 warning；评估仍使用历史快照。 |
| Deleted group safe failure | 已实现 | 当前 group 删除或不可读取时安全失败，返回 `condition_gate_replay_group_deleted`。 |
| Missing snapshot safe failure | 已实现 | 缺少 context 或 definition snapshot 时安全失败，不读取 live world / player / inventory / region / SignalBridge。 |
| Execute action replay | 不做 | replay 不执行 action，不 emit signal，不消费或移动物品。 |

## Doctor

| 诊断 | 状态 | 说明 |
|---|---|---|
| missing condition group | 已实现 | `condition-runtime-missing-group` ERROR。 |
| disabled condition group | 已实现 | `condition-runtime-disabled-group` WARNING。 |
| invalid condition group | 已实现 | `condition-runtime-invalid-group` ERROR。 |
| missing definition | 已实现 | `condition-runtime-definition-missing` ERROR。 |
| incompatible group | 已实现 | `condition-runtime-incompatible-group` ERROR。 |
| context_player on no-player target | 已实现 | 通过 compatibility message 输出中文原因。 |
| container condition on non-container target | 已实现 | 通过 compatibility message 输出“容器快照”。 |
| item / inventory unsupported target | 已实现 | 通过 compatibility message 输出“物品快照 / 背包快照”。 |
| signal history unsupported target | 已实现 | 通过 compatibility message 输出“信号历史快照”。 |
| logic chain unsupported target | 已实现 | 通过 compatibility message 输出“逻辑链快照”。 |
| always_false warning | 已实现 | `condition-runtime-always-false-node` WARNING。 |
| Auto fix | 不做 | Doctor 不自动修改配置，不清空 conditionGroupId。 |
| SignalReceiver gate missing error | 不做 | 不把 deferred SignalReceiver gate 当作错误。 |
| 单条 Action gate missing error | 不做 | 不把 deferred 单条 Action gate 当作错误。 |

## WebAdmin API

| API | 权限 | 状态 | 说明 |
|---|---|---|---|
| `GET /api/webadmin/condition-gates/history` | VIEWER | 已实现 | 只读 list，支持 targetType/result/conditionGroupId/targetId/channel/limit。 |
| `GET /api/webadmin/condition-gates/history/{id}` | VIEWER | 已实现 | 只读 detail，包含 debug tree 与 replay flags。 |
| `POST /api/webadmin/condition-gates/history/{id}/replay` | VIEWER | 已实现 | 只读 replay / simulation，不写 store、不 emit signal、不执行 action。 |
| `GET /api/doctor` | VIEWER | 已扩展 | 追加 condition runtime diagnostics。 |

## WebAdmin UI

| 能力 | 状态 | 说明 |
|---|---|---|
| `#/condition-debugger` | 已实现 | sidebar “条件调试”。 |
| `#/condition-debugger/{historyId}` | 已实现 | 独立完整宽度 detail route；兼容 `#/condition-debugger?id=...`。 |
| History list | 已实现 | 表格展示最近 gate 记录。 |
| Filters | 已实现 | targetType、result、conditionGroupId、targetId、channel。 |
| Debug detail | 已实现 | 点击整行或“详情”进入完整宽度详情页，分区展示 result、context summary、debug tree、replay 和只读折叠技术信息。 |
| Replay button | 已实现 | “模拟重放”，只读。 |
| Condition group jump | 已实现 | 跳转 `#/condition-groups/{id}`。 |
| Recent gate status | 已实现 | VBD / SignalListener / ActionRelay / RegionController 页面展示最近一次条件判断。 |
| Silent refresh | 已实现 | condition gate history event 标记 route dirty，不整页 reload。 |
| Real action replay | 不做 | “真实重放 Action” disabled。 |
| Raw JSON editor | 不做 | 主页面不提供 JSON 编辑入口。 |

## Scope guard

8.8 明确不实现：

- SignalReceiver gate。
- 单条 Action gate。
- Action list 执行策略变更。
- GameController / MissionSystem / PhaseController。
- 具体任务 / 关卡。
- 小游戏 IDE 模板系统。
- 版本回滚系统。
- 新 runtime target。
- raw JSON editor。
- 任意 NBT path。
- 通用脚本表达式。
- MCP scenario。
- 启动 Minecraft。
- 截图矩阵。

## 测试覆盖

| 测试 | 覆盖 |
|---|---|
| `ConditionGateHistoryServiceTest` | history allowed / blocked / missing / error、blank skip、最大 200 条、WebAdmin list/detail/recent status。 |
| `ConditionGateReplayServiceTest` | replay allowed / blocked、changed fingerprint warning、deleted safe failure、missing record / missing snapshot safe failure。 |
| `WebAdminConditionRuntimeDoctorServiceTest` | missing / disabled / invalid / incompatible、context_player、container、inventory、signal history、always_false warning、blank no issue。 |
| `StabilizationGuardTest` | docs、API、UI markers、realtime marker、recent gate status、no out-of-scope、no raw JSON editor。 |
