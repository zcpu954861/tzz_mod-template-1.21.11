# 8.0 ConditionEngine Core Current Context

## Stage

8.0 ConditionEngine Core / 条件判断核心系统。

Stable baseline: `v1.45.0-web-admin-logic-chain-viewer`.

This stage is the first 8.x foundation step. It introduces a pure condition judgment core, not a concrete game mode and not a WebAdmin visual editor.

## Hard Boundary

旧数据包只作为条件复杂度参考。It helps explain what future conditions may need to express, but 8.0 does not copy datapack functions, scoreboard flows, trigger panels, old mission scripts, or task logic into the mod.

Condition has no side effects.

ConditionEngine only answers whether a condition tree matches a provided context and why. It must not:

- write state,
- emit signals,
- send messages,
- give or clear items,
- teleport players,
- execute commands,
- mutate inventories,
- change blocks,
- schedule tasks,
- update runtime stores.

Those effects remain the responsibility of Action / StateAction / SignalBridge / future GameController systems.

## Implemented In 8.0

The core package is `com.zcpu.tzzmod.condition`.

Implemented core pieces:

- `ConditionDefinition`
- `ConditionGroupDefinition`
- `ConditionNode`
- `ConditionNodeConfig`
- `ConditionGroupMode`
- `ConditionNodeType`
- `ConditionEvaluationContext`
- `ConditionEvaluationResult`
- `ConditionEvaluationTrace`
- `ConditionRegistry`
- `ConditionEvaluator`
- `ConditionTypeHandler`
- `ConditionPredicate`
- `ConditionValidationResult`
- `ConditionValidationIssue`
- `ConditionEngineLimits`

## Data Model

`ConditionGroupDefinition` is a stable condition group wrapper:

- id
- version
- displayName
- note
- tags
- root `ConditionNode`
- stable fingerprint

`ConditionNode` supports:

- id
- type
- name
- note
- enabled / disabled state
- groupMode for group nodes
- config map for leaf nodes
- nested children

`ConditionGroupMode` supports:

- AND / OR / NOT

`ConditionNodeType` currently includes:

- `group`
- `always_true`
- `always_false`
- `context_exists`
- `context_field_exists`
- `context_equals`

## EvaluationContext

`ConditionEvaluationContext` is deliberately broader than the current `ActionContext`. It is designed so later stages can pass SignalBridge, device, listener, region, item, and game-state data without redesigning the core.

Current fields:

- playerId
- playerName
- worldId
- sourceType
- sourceId
- channel
- deviceId
- listenerId
- regionId
- actionId
- blockPos
- itemStackSummary
- triggerType
- detail
- gameTime
- signalDepth
- eventMetadata
- variables

Future stages may adapt this from `SignalEvent`, `ActionContext`, VBD interaction context, RegionController events, itemSubmit evaluation context, and game-state context. 8.0 does not wire those runtime systems into ConditionEngine.

## ConditionResult

`ConditionEvaluationResult` is not just a boolean. It includes:

- matched
- conditionId
- nodeId
- type
- label
- reasonCode
- failureReason
- message
- debugSummary
- childResults
- skipped
- error
- evaluatedNodeCount
- durationNanos
- contextSummary

The `childResults` tree is the 8.0 debug tree. It is intended for later WebAdmin / Doctor / History display so failure reason can be traced to the exact leaf or group.

## Registry

`ConditionRegistry` supports:

- condition type registration
- condition type metadata
- future field schema metadata
- validation dispatch
- evaluation dispatch
- unknown condition type safe failure

Built-in handlers:

- always_true
- always_false
- context_exists
- context_field_exists
- context_equals

## Validation

Validation is separate from evaluation.

Validation checks include:

- null definition / node
- unknown type
- invalid config
- empty group
- NOT group child count
- max depth
- max node
- duplicate node id

Evaluation also fails safely for unknown or invalid runtime behavior and converts handler exceptions into error results.

## Safety Limits

Defaults:

- max depth: 16
- max node count: 128

These limits exist to prevent recursive or very large condition trees from harming the server.

## Testing

8.0 guard tests cover:

- AND true + true
- AND true + false
- OR false + true
- OR false + false
- NOT true
- NOT false
- nested group
- disabled node behavior
- always_true / always_false
- context field exists
- context equals
- failure reason
- unknown type safe failure
- invalid config validation
- max depth
- max node
- stable fingerprint
- result debug tree

## Store / API Status

8.0 does not add a world-scoped ConditionGroup store or WebAdmin Condition API.

Reason: a runtime condition group store and WebAdmin writes need a focused design for permissions, edit lock, fingerprint, audit, realtime, validation, migration, and no raw JSON editor. This is intentionally deferred to a later 8.x step.

8.0 therefore has no WebAdmin condition editor, no raw JSON editor, and no runtime integration.

## Explicit Non-Goals

8.0 does not implement:

- concrete TZZ escape-game tasks,
- old datapack missions,
- task one / task two / task three,
- hunter spawn selection,
- escape energy,
- OP timer,
- player/admin panels,
- GameController,
- MissionSystem,
- PhaseController,
- 不做 GameController,
- 不做 MissionSystem,
- 不做 PhaseController,
- ConditionEngine WebAdmin visual editor,
- raw JSON editor,
- NBT path editor,
- new MCP tool,
- MCP scenario,
- screenshots or screenshot matrix,
- Minecraft client startup.
- 不做 raw JSON editor,
- 不新增 MCP tool,
- 不跑 MCP scenario,
- 不生成截图.

8.0 also does not:

- 不接入 VBD runtime,
- 不接入 SignalListener runtime,
- 不接入 RegionController runtime,
- 不接入 ActionRelay runtime,
- 不接入 itemSubmit runtime,
- 不改 SignalBridge runtime.

## Later 8.x Direction

Suggested follow-up sequence:

- 8.1: player / context conditions such as player exists, OP, tag, team, channel/source equals.
- 8.2: region and time conditions.
- 8.3: item/inventory conditions using existing matcher/itemSubmit concepts without duplicating consume semantics.
- 8.4: state variables and scoreboard-like game state.
- 8.5: WebAdmin condition group list and safe visual editor.
- 8.6: runtime integration points for listener/VBD/region gates.
- 8.7: Doctor / History condition trace display.
- 8.8: logic chain viewer condition badges after runtime integration.
- 8.9: migration and templates for higher-level game systems.

GameController / MissionSystem / PhaseController remain later high-level systems, not 8.0.
