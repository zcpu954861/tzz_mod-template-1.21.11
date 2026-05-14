# ConditionEngine Capability Matrix 8.0

This matrix documents the 8.0 ConditionEngine Core baseline. It is a core engine summary, not a WebAdmin editor specification and not a concrete TZZ game task plan.

## Implemented Capability Matrix

| Area | 8.0 Status | Details | Not Done |
| --- | --- | --- | --- |
| ConditionEngine Core | Implemented | Pure evaluation package under `com.zcpu.tzzmod.condition`. | No runtime integration. |
| 无副作用 | Implemented | Conditions only evaluate and return structured results. | No state writes, no signals, no actions, no commands. |
| ConditionDefinition | Implemented | Wrapper over `ConditionGroupDefinition` for future compatibility. | No persistent store yet. |
| ConditionGroupDefinition | Implemented | id, version, displayName, note, tags, root node, stable fingerprint. | No world save path yet. |
| ConditionNode | Implemented | id/name/note/type/enabled/config/children. | No WebAdmin editor. |
| ConditionGroupMode | Implemented | AND / OR / NOT. | No sequence/action branch semantics. |
| ConditionEvaluationContext | Implemented | player/world/source/channel/device/listener/region/action/block/item/gameTime/eventMetadata/variables fields. | Not adapted from live runtime yet. |
| ConditionEvaluationResult | Implemented | matched, reasonCode, failureReason, childResults, skipped, error, evaluatedNodeCount, duration, context summary. | No WebAdmin debug tree page yet. |
| ConditionRegistry | Implemented | Type registration, metadata, validation dispatch, evaluation dispatch, unknown type safe failure. | No dynamic plugin loading. |
| Validation | Implemented | unknown type, invalid config, empty group, NOT child count, duplicate node id, maxDepth, maxNodes. | No cross-reference validation because no store exists. |
| maxDepth | Implemented | Default maxDepth 16. | No per-group persisted limit. |
| maxNodes | Implemented | Default maxNodes 128. | No per-group persisted limit. |
| always_true | Implemented | Core test predicate. | Only for testing/placeholder use. |
| always_false | Implemented | Core test predicate. | Only for testing/placeholder use. |
| context_field_exists | Implemented | Checks field presence in EvaluationContext. | Does not inspect Minecraft runtime directly. |
| context_equals | Implemented | Checks simple string equality against EvaluationContext. | No expression parser. |
| Store/API | Deferred | 8.0 intentionally avoids raw JSON and incomplete write flows. | No list/get/create/update/delete ConditionGroup API. |
| WebAdmin UI | Deferred | Future 8.5 visual editor should use typed forms, edit lock and fingerprint. | No raw JSON editor. |
| Runtime integration | Deferred | Future stages can gate VBD/listener/region/action paths. | 8.0 不接入现有运行时. |

## Current Test Coverage

- AND / OR / NOT
- nested group
- disabled node
- always_true / always_false
- context_field_exists
- context_equals
- unknown type safe failure
- invalid config
- maxDepth
- maxNodes
- stable fingerprint
- failure reason and debug tree

## Explicit Non-Goals

8.0 does not do:

- 不做具体逃走中任务
- 不做旧数据包任务迁移
- 不做 GameController
- 不做 MissionSystem
- 不做 PhaseController
- 不做 VBD / SignalListener / RegionController / ActionRelay / itemSubmit runtime integration
- 不改 SignalBridge runtime
- 不做 WebAdmin 条件可视化编辑器
- 不做 raw JSON / NBT path
- 不新增 MCP tool
- 不跑 MCP scenario
- 不生成截图

## 8.1-8.9 Planning

| Version | Suggested Focus | Notes |
| --- | --- | --- |
| 8.1 | Player and signal context conditions | player exists, OP, tag/team, channel/source equals. |
| 8.2 | Region/time conditions | region membership, cooldown/recent signal, timer-style conditions. |
| 8.3 | Item/context conditions | use existing matcher semantics; do not duplicate consume. |
| 8.4 | Game state variables | scoreboard-like booleans/numbers with controlled write APIs outside ConditionEngine. |
| 8.5 | WebAdmin condition editor | typed visual editor, no raw JSON, edit lock/fingerprint/audit/realtime. |
| 8.6 | Runtime gating | carefully integrate into listener/VBD/region gates after tests. |
| 8.7 | Doctor/history traces | show ConditionEvaluationResult debug tree. |
| 8.8 | Logic chain condition badges | visualize condition status in the 7.15 logic-chain viewer. |
| 8.9 | Templates and migration prep | prepare for GameController / MissionSystem without implementing old datapack tasks directly. |

## Relation To Old Datapack

旧数据包只作为条件复杂度参考. It suggests future needs such as player tag/team checks, region checks, item checks, global state variables, timers, and grouped failure reasons. It is not copied into this stage.

## Relation To Future High-Level Systems

GameController / MissionSystem / PhaseController should call ConditionEngine later as a pure predicate layer. They remain responsible for phase changes, mission state, rewards, punishments, timers, player operations, reset/rollback, and UI prompts.
