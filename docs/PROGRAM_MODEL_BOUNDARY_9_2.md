# TZZ Mod 9.2 Program Model Boundary

## Canonical Boundary

9.2 is a Resource Graph typed action configuration phase.

It prepares action metadata, validation, editor and summary foundations for future work, but it does not implement a Program Model.

## Resource Graph Layer

The Resource Graph layer contains existing configurable resources and references:

| Resource Graph area | 9.2 relationship |
| --- | --- |
| Signal / channel metadata | Existing routing resources and graph references. |
| SignalListener | Existing `ActionConfig` owner. |
| ActionRelay | Existing `ActionConfig` owner. |
| Timer | Existing `ActionConfig` owner through lifecycle buckets. |
| RegionController | Existing `ActionConfig` owner through enter / exit / stay buckets. |
| VBD / SignalDevice | Existing typed resource and trigger config, but not an `ActionConfig` owner for native triggers. |
| StateVariable | State target and `state_variable` action integration. |
| ConditionGroup | Gate reference, not a branch node. |
| ActionConfig | Existing flat owner-owned action entries. |

## Program Model Layer

The following concepts are not implemented in 9.2:

- GameController;
- MissionSystem;
- PhaseController;
- Branch / if-else runtime;
- typed action sequence runtime;
- mission step / task lifecycle;
- fallback action list / stop-list policy;
- Scratch-like visual programming;
- full Rich Text Builder;
- automatic datapack conversion.

These belong to 10.x or a separately confirmed future phase.

## Action List Semantics

9.2 action lists remain owner-owned flat lists.

Phase 1 `ActionSchemaRegistry` is still Resource Graph metadata. It describes existing `ActionConfig` structure and current owner applicability, but it does not introduce typed action sequences, branch nodes, fallback lists or a Program Model store.

They are not:

- cross-owner draggable programs;
- cross-bucket action pipelines;
- nested branches;
- if / else trees;
- retry / fallback programs;
- mission step scripts.

Delete and reorder operations must remain scoped to the same owner and bucket.

## Condition Gate Semantics

`conditionGroupId` remains a gate reference.

It can allow, block, skip or fail closed according to existing gate behavior. It is not a branch node and does not introduce an else action list or a fallback action.

## VBD Boundary

VBD native triggers, itemSubmit requirements and container conditions are Resource Graph configuration. They can be indexed and documented near typed actions because they emit signals or participate in Logic Chain editing.

They are not `ActionConfig` owner buckets in 9.2. Any future attempt to attach arbitrary action lists to VBD triggers would be a runtime feature expansion and must be scoped separately.

## Rich Text Boundary

The old 9.0 roadmap grouped Typed Actions and Rich Text Builder as related future candidates. The current 9.2 phase intentionally narrows that scope.

9.2 may document message action fields and summary requirements, but it does not implement:

- tellraw component builder;
- title / subtitle / actionbar builder;
- hover / click component authoring;
- raw JSON text editor;
- dynamic score / selector / NBT component UI.

## Stop Rules

Stop and ask for explicit confirmation if any proposed implementation requires:

- creating Program Model classes or stores;
- changing `ActionEngine` runtime behavior;
- changing existing action owner save payloads;
- changing `WebAdminWriteResult` shape or error codes;
- adding new action types;
- turning VBD native triggers into action owners;
- implementing Rich Text Builder functionality.
