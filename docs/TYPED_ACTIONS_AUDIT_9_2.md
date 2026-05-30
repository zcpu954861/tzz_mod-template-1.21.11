# TZZ Mod 9.2 Typed Actions Phase 0 Audit

## Baseline

| Item | Value |
| --- | --- |
| Phase | 9.2 Phase 0 preflight audit |
| Docs patch tag | `v1.68.4-docs-accuracy` |
| Baseline commit | `f8fa12c6e5c20ca82a3d5ea0a87f24d26462fb4c` |
| Runtime release lineage | `v1.68.3-real-performance-deep-simplification` |
| Scope | docs / Obsidian audit only |

Phase 0 does not change runtime code, WebAdmin API shape, save payloads, tests, guards, action execution order or action side effects.

## Scope

9.2 is a Resource Graph typed action configuration cleanup phase. It audits and later unifies the configuration, capability, validation, editor, summary, audit and diff model for existing `ActionConfig` lists.

It is not a Program Model phase. GameController, MissionSystem, PhaseController, branch / if-else runtime, typed action sequence, Scratch-like editor and full Rich Text Builder remain out of scope.

## Existing Action Types

The current action runtime has these existing action types only:

| ActionType id | Runtime notes | Phase 0 status |
| --- | --- | --- |
| `command` | Executes a server command after stripping a leading slash. | Existing |
| `message` | Sends to player context when present, otherwise broadcasts. | Existing |
| `sound` | Runtime currently plays the existing fixed feedback sound; `value` is not a full custom sound runtime. | Existing legacy caveat |
| `signal` | Emits a SignalBridge channel. | Existing |
| `state_variable` | Mutates controlled state through StateVariable services. | Existing |
| `timer_start` | Starts a timer through scheduler runtime. | Existing |
| `timer_cancel` | Cancels a timer through scheduler runtime. | Existing |

Phase 0 does not add any `ActionType`.

## ActionConfig Field Inventory

Existing `ActionConfig` payloads are still the compatibility contract:

| Field group | Fields |
| --- | --- |
| Common | `type`, `value`, `enabled`, `requiresOp`, `cooldownTicks`, `notifyOps`, `conditionGroupId` |
| State variable | `stateOperation`, `stateScope`, `stateTargetMode`, `stateTargetId`, `stateKey`, `stateValueType`, `stateValue`, `stateDelta`, `stateCreateIfMissing`, `stateInitialValue` |
| Timer | `timerId`, `timerTargetMode`, `timerTargetPlayerName`, `timerMissingPolicy` |
| DTO / UI derived | `summary`, `stateActionSummary`, validation errors and owner-specific lock/fingerprint fields |

The later schema registry must describe these fields; it must not rename or reinterpret the saved JSON payload.

## ActionConfig Owners

Current Resource Graph action owners are owner-owned flat lists:

| Owner | Buckets / list | Runtime / save boundary |
| --- | --- | --- |
| SignalListener | `actions` | SignalBridge listener actions |
| ActionRelay | `actions` | Loaded ActionRelay block entity actions |
| RegionController | `enterActions`, `exitActions`, `stayActions` | Region enter / exit / stay buckets |
| Timer | `onStartActions`, `onTickActions`, `onCompleteActions`, `onCancelActions` | Scheduler lifecycle buckets |

VBD native triggers, interaction matcher, itemSubmit requirements and container item conditions are adjacent Resource Graph configuration, not `ActionConfig` owners. They emit signals or perform trigger-specific feedback / consume logic and must not be modeled as typed action owners in 9.2 Phase 0.

## Runtime Invariants

9.2 typed action work must preserve these existing runtime facts:

- `ActionEngine.executeAll` executes the owner list in order, skips unusable entries, and stops on the first failed action result.
- SignalBridge dispatch order remains receiver, ActionRelay, SignalListener, accepted history / SignalJoin observer.
- ActionRelay runtime only works on loaded block entities and must not force-load chunks.
- RegionController first entry initialization records inside state and does not retroactively emit enter actions.
- Region stay actions retain the existing interval behavior.
- Timer start creates the active instance before onStart actions; cancel removes the instance before onCancel actions; complete runs onComplete before outputChannel.
- `conditionGroupId` remains a gate reference, not a branch node. Blank gates must keep the old lazy skip behavior.

## Save / Validation / Summary / Audit / Snapshot Current State

Current implementation is intentionally owner-specific:

| Area | Current state | 9.2 target |
| --- | --- | --- |
| Allowed action types | Repeated across owner services / UI helpers. | Single capability matrix. |
| Backend validation | ActionRelay has the most complete common validator; SignalListener and Region reuse parts plus owner gate target checks; Timer has action checks but is not yet fully described by a unified schema/capability validator. | Unified backend validation service. |
| WebAdmin editor | Per-owner modals and Java string script helpers. Logic Chain has the richest owner/bucket model but remains ad hoc. | Schema-driven field renderer with owner adapters. |
| Summary | Chinese summaries exist in several paths, but no single action summary service. | Unified summary service used by cards, diff, snapshot and audit. |
| Snapshot diff | Resource-level diff with shallow summaries / hashes; not action-index typed diff. | Compatible typed action summaries without changing snapshot storage. |
| Audit | Owner-specific audit and redaction; not unified action summary. | Stable summaries and command redaction preserved. |
| Unsaved diff | Logic Chain frontend helper can summarize typed action draft operations, but field labels are still partly technical. | Shared human summary where practical. |

## Confirmed Gaps

- Timer action validation is the most visible migration gap. It is not "no validation"; it is not yet governed by a unified schema/capability model, and common fields such as command / signal / message / sound / state_variable are not uniformly covered the same way as ActionRelay.
- `ActionType.fromId` and legacy `ActionConfig` construction still have compatibility fallback behavior. New save validation must fail closed before relying on that fallback.
- Summary, allowed type filtering and field rendering are duplicated across ActionRelay, SignalListener, Region, Timer and Logic Chain paths.
- VBD native trigger editing has typed resource behavior of its own, but it is not an `ActionConfig` action list and should not be forced into this matrix.

## Phase 0 Deliverables

- `docs/TYPED_ACTIONS_AUDIT_9_2.md`
- `docs/ACTION_CAPABILITY_MATRIX_9_2.md`
- `docs/ACTION_SCHEMA_DESIGN_9_2.md`
- `docs/PROGRAM_MODEL_BOUNDARY_9_2.md`
- `docs/TYPED_ACTIONS_ROADMAP_9_2.md`
- Obsidian `18_9.2_TypedActions/` notes and related index updates.

## Stop Conditions

Stop before implementation if a plan requires any of the following:

- new runtime `ActionType`;
- changed `ActionEngine` dispatch, list order or failure semantics;
- changed owner save payload or `WebAdminWriteResult` shape;
- VBD native trigger / itemSubmit / container treated as `ActionConfig` owners;
- Program Model, Rich Text Builder, GameController, MissionSystem, PhaseController or if/else runtime work in 9.2.
