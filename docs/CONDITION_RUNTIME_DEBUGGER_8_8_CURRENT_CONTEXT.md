# 8.8 Condition Runtime Debugger / Doctor / Simulation 当前上下文

## 8.8 目标

8.8 不继续扩展 runtime gate 目标，而是让 8.6 / 8.7 已经接入的 gate 可观察、可诊断、可复现。

本阶段新增：

- Condition gate runtime history。
- gate debug detail / debug tree。
- Doctor runtime binding diagnostics。
- 从 history record 进行只读 replay / simulation。
- WebAdmin `#/condition-debugger` 条件调试入口。
- 已接入页面上的最近一次 gate 状态。

核心问题是回答：

- 为什么这个逻辑没有触发。
- 哪个 condition 未通过。
- 当时的 context summary 是什么。
- 绑定的 condition group 是否 missing / disabled / invalid / incompatible。
- 能否用记录中的 snapshot 复现这次判断。

## 8.8 不做内容

本阶段仍不做：

- SignalReceiver gate。
- 单条 Action gate。
- GameController / MissionSystem / PhaseController。
- 具体任务 / 关卡。
- 复杂小游戏 IDE 模板。
- 版本回滚系统。
- 文档中心。
- Scratch-like editor。
- 新 condition type 大扩展。
- raw JSON editor。
- 任意 NBT path。
- 通用脚本表达式。
- MCP scenario。
- 启动 Minecraft。
- 截图矩阵。

## Optional Gate 原则保持不变

未配置 conditionGroupId 时旧逻辑保持不变：

- 不读取 condition group store。
- 不构造 `ConditionEvaluationContext`。
- 不 evaluate。
- 不记录 runtime history。
- 不影响旧 SignalBridge / itemSubmit / container / ActionRelay / RegionController 语义。

配置 conditionGroupId 后仍只作为外层 gate：

```text
旧系统准备触发副作用
-> ConditionGateService
-> true: 进入旧流程
-> false/error: fail closed，不进入旧副作用流程
-> 记录最近一次 gate history
```

8.8 只观察、诊断和只读重放，不改变 8.6 / 8.7 gate decision。

## Runtime history

新增 `ConditionGateHistory` 作为进程内内存环形缓冲，最大 200 条。它不是持久化 store，不写入世界目录，也不写入 device JSON。

当前选择内存态 / 全局进程缓冲的原因：

- history 是调试观测数据，不是配置事实源。
- 避免把 transient runtime context 写入存档。
- 避免跨世界持久残留旧上下文。
- 当前 WebAdmin / Codex 验收重点是最近触发链路解释，而不是长期审计。

`WebAdminConditionGateHistoryService` 在 DTO 中标记：

- `readOnly=true`
- `inMemory=true`
- `worldScoped=false`
- `maxRecords=200`

这里的 `worldScoped=false` 表示 history 没有按世界持久化文件分区；record 内仍包含 `worldId` summary，供用户识别来源。

记录策略：

- 有 conditionGroupId 的 configured gate 至少记录 allowed / blocked / error。
- blank / unset `conditionGroupId` 视为 skipped，不记录 history，保持旧逻辑不受影响。
- history 写入和 realtime publish 失败不会改变 gate decision；运行时仍按原 evaluate 结果返回。

## History record 字段

每条 `ConditionGateHistoryRecord` 包含：

- `id`
- `sequence`
- `wallTimeMillis`
- `occurredAt`
- `gameTime`
- `worldId`
- `targetType`
- `targetTypeId`
- `targetTypeDisplayName`
- `targetId`
- `sourceType`
- `sourceId`
- `channel`
- `deviceId`
- `listenerId`
- `regionId`
- `actionId`
- `playerId`
- `playerName`
- `conditionGroupId`
- `conditionGroupDisplayName`
- `conditionGroupFingerprint`
- `definitionFingerprint`
- `result`
- `allowed`
- `skipped`
- `code`
- `failureReason`
- `debugSummary`
- `evaluatedCount`
- `durationNanos`
- `contextSummary`
- `debugTree`
- `replayContext`
- `definitionSnapshot`

`replayContext` 是 `ConditionEvaluationContext` snapshot；`definitionSnapshot` 是当时的 condition group definition snapshot。二者不包含 live world、live player、live inventory、live region 或 live SignalBridge service 引用。

## Debug detail

WebAdmin detail 显示：

- 目标类型 / targetId。
- channel。
- condition group。
- result。
- failureReason。
- evaluatedCount。
- duration。
- context summary。
- debug tree。
- condition group 跳转。
- replay 结果。

主 UI 不提供 raw JSON editor。技术字段只作为只读调试信息，不作为编辑入口。

## Replay 只读语义

`ConditionGateReplayService` 从 history record replay：

```text
history record
-> replayContext snapshot
-> definitionSnapshot
-> ConditionEvaluator.evaluateTrace
-> ConditionGateReplayResult
```

Replay 只读：

- 不写 store。
- 不 emit signal。
- 不执行 action。
- 不消费或移动物品。
- 不读取 live world / player / inventory / region / SignalBridge。
- 不调用 `ConditionRuntimeContextBuilder` 重建 live context。

如果 condition group 当前定义已变化：

- replay 使用历史 `definitionSnapshot` 评估。
- 返回 `fingerprintChanged=true`。
- 中文 warning 说明“使用历史快照只读评估，结果可能不同于当前配置”。

如果 condition group 当前已删除或不可读取：

- replay 安全失败。
- 返回 `condition_gate_replay_group_deleted`。
- 明确说明没有执行 action、没有 emit signal、没有写 store。

如果历史记录缺少 context snapshot 或 definition snapshot：

- replay 安全失败。
- 返回中文 failureReason。
- 不尝试读取 live world / player / inventory。

## Doctor 规则

`WebAdminConditionRuntimeDoctorService` 扫描 8.6 / 8.7 已接入 gate binding：

- VBD redstone。
- VBD blockstate。
- VBD interaction。
- itemSubmit。
- container open / close / change。
- SignalListener gate。
- ActionRelay gate。
- RegionController enter / exit / stay gate。

当前 Doctor 诊断：

- 已绑定但不存在的 condition group。
- 绑定到 disabled condition group。
- 绑定到 definition missing group。
- 绑定到 invalid condition group。
- target profile 与 condition group capability 不兼容。
- context_player 用在无 player target。
- container condition 用在非 container target。
- item / inventory condition 用在无对应 snapshot 的 target。
- signal history condition 用在无对应 snapshot 的 target。
- logic chain condition 用在无对应 snapshot 的 target。
- `always_false` 节点 warning。

Doctor 不自动修改配置，不自动清空 conditionGroupId，不把未配置 conditionGroupId 报错，也不把 SignalReceiver gate / 单条 Action gate 当成缺失能力报错。

## WebAdmin API

新增只读/模拟 API：

```text
GET  /api/webadmin/condition-gates/history
GET  /api/webadmin/condition-gates/history/{id}
POST /api/webadmin/condition-gates/history/{id}/replay
```

list 支持 filter：

- `targetType`
- `result`
- `conditionGroupId`
- `targetId`
- `channel`
- `limit`

`POST replay` 是只读 simulation endpoint；使用 POST 是为了避免将操作语义挤进 URL，但服务端不会写 store、不会 emit signal、不会执行 action。

Doctor 仍复用：

```text
GET /api/doctor
```

8.8 在该报告中追加 condition runtime diagnostics。

## WebAdmin UI

新增入口：

```text
#/condition-debugger
#/condition-debugger/{historyId}
```

sidebar 文案为“条件调试”。

页面包含：

- 最近 gate history 列表。
- targetType / result / conditionGroupId / channel / targetId 筛选。
- 点击整行或“详情”进入独立完整宽度 detail route；旧 `#/condition-debugger?id=...` 仅作兼容入口，也渲染详情页。
- detail page 分区展示 result / failureReason、replay、context summary、debug tree、只读折叠技术信息。
- condition group 跳转。
- 只读模拟重放。
- 中文空状态和错误提示。

Realtime 事件：

```text
CONDITION_GATE_HISTORY_APPENDED
condition_gate_history_appended
```

前端将事件映射到：

- dashboard。
- conditionDebugger。
- history。
- doctor。

刷新仍走 route-level silent refresh，不整页 reload，不重置筛选，不关闭 modal。

## 现有页面最近 gate 状态

以下页面 / 模块显示最近一次条件判断：

- VBD native trigger cards。
- SignalListener detail 侧栏。
- ActionRelay readonly card。
- RegionController enter / exit / stay action summary cards。

显示内容：

- 通过 / 阻断 / 错误 / 暂无记录。
- failureReason 或 debugSummary。
- 查看 debug detail。
- 跳转 condition group。

没有 history 时显示中文空状态；history API 失败不应让主页面崩溃。

## 测试与 guard

新增或扩展：

- `ConditionGateHistoryServiceTest`
- `ConditionGateReplayServiceTest`
- `WebAdminConditionRuntimeDoctorServiceTest`
- `StabilizationGuardTest` 的 8.8 marker guard

覆盖：

- allowed / blocked / error / missing group history。
- blank conditionGroupId 不读 store、不构造 context、不记录 history。
- 最大 200 条内存环形缓冲。
- WebAdmin list / detail / recent status DTO。
- replay allowed / blocked。
- replay changed condition group fingerprint warning。
- replay deleted condition group safe failure。
- replay missing record / missing snapshot safe failure。
- Doctor missing / disabled / invalid / incompatible。
- context_player / container / inventory / signal history incompatibility。
- always_false warning。
- no SignalReceiver gate。
- no 单条 Action gate。
- no GameController / MissionSystem / PhaseController。
- no raw JSON editor。

## 后续 deferred

- SignalReceiver gate。
- 单条 Action gate。
- GameController / MissionSystem / PhaseController。
- 版本回滚系统。
- 完整文档中心。
- 更完整的长期 history 持久化策略。
- 更完整的 condition group diff 视图。
