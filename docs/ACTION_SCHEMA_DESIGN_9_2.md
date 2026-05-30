# TZZ Mod 9.2 Action Schema Design

## Purpose

Typed action schema is metadata for existing `ActionConfig` entries. It describes configuration structure, field labels, editor hints, validation hints and summary hints.

It does not execute actions. Runtime execution remains owned by `ActionEngine` and the existing owner adapters.

## Non-Goals

- no new `ActionType`;
- no rewrite of `ActionEngine`;
- no change to `ActionConfig` JSON payload fields;
- no Program Model, typed action sequence or branch runtime;
- no full Rich Text Builder;
- no VBD native trigger / itemSubmit / container conversion into action owners.

## Proposed Types

Implemented Phase 1 package:

```text
src/main/java/com/zcpu/tzzmod/action/schema/
```

| Type | Responsibility |
| --- | --- |
| `ActionSchema` | Metadata for one existing action type. |
| `ActionFieldSchema` | Metadata for one saved / derived field. |
| `ActionFieldType` | `text`, `number`, `boolean`, `select`, `textarea`, `channel_picker`, `state_variable_picker`, `condition_group_picker`, `player_target_mode`, `readonly_summary`. |
| `ActionOwnerType` | Existing owners and buckets: listener, relay, region enter/exit/stay, timer start/tick/complete/cancel. |
| `ActionCapability` | Phase 1 metadata-only owner support for current action types. |
| `ActionSchemaRegistry` | Static immutable lookup by `ActionType`, strict lookup by id, owner metadata query. |
| `ActionCapabilityMatrix` | Phase 2 authoritative owner/bucket matrix used by backend validation. |

## Schema Boundary

Schema describes:

- action type id and display name;
- Chinese description and help text;
- field ids, labels, required/default semantics and value type;
- editor hint and picker requirements;
- summary hint and audit redaction hint;
- condition group support;
- owner-independent validation facts.

Schema must not:

- execute an action;
- load a Minecraft world or server context;
- read stores on a render path;
- mutate saved config;
- silently translate unknown action types into `command`;
- change runtime failure semantics.

## Registry Requirements

The registry should be static, immutable and cheap to query.

Expected shape:

```text
Map<ActionType, ActionSchema>
Map<ActionOwnerType, Set<ActionType>>
```

Do not rebuild schema objects on every WebAdmin render, hover, save validation or runtime tick. Runtime tick paths should not depend on schema registry at all unless explicitly proven safe and necessary.

## Field Groups

| Action type | Required schema coverage |
| --- | --- |
| `command` | `value`, dangerous command warning / confirmation marker, `requiresOp`, `notifyOps`, `cooldownTicks`, optional gate. |
| `message` | `value`, target context note, optional gate. |
| `sound` | `value` as legacy display/config field, with caveat that runtime does not yet use it as a full custom sound id. |
| `signal` | `value` / channel picker, channel validation, optional gate. |
| `state_variable` | state operation, scope, target mode, target id, key, value type, value, delta, create-if-missing, initial value; value fields follow the current 512-character validation limit and default value type remains `BOOLEAN` to match existing editor defaults. |
| `timer_start` | `timerId`, `timerTargetMode`, `timerTargetId`, `timerStartPolicyOverride`, `timerDurationOverrideTicks`; `timerTargetMode` has an editor default but blank payloads remain compatibility-valid. |
| `timer_cancel` | `timerId`, `timerTargetMode`, `timerTargetId`, `timerMissingBehavior`; options include legacy-compatible `fail_if_missing` so future validation does not reject old data. |

Common fields remain `type`, `enabled`, `cooldownTicks`, `requiresOp`, `notifyOps` and `conditionGroupId`.

Phase 1 schema intentionally does not expose `requiresOp` / `notifyOps` as editable fields for `state_variable`, `timer_start` or `timer_cancel`, because the current `ActionConfig` canonical constructor normalizes those values to `false` for those action families. The fields remain part of the compatibility payload contract, not a new editor promise.

Timer owner bucket ids in `ActionOwnerType` use `timer_on_start` / `timer_on_tick` / `timer_on_complete` / `timer_on_cancel` to avoid colliding with `timer_start` / `timer_cancel` action type ids.

## Owner Capability Separation

Action schema says what an action type looks like. Owner capability says whether a specific owner/bucket can use it and which write rules apply.

This separation is required because owner-specific state still differs:

- lock target;
- expected fingerprint source;
- max action count;
- condition runtime target type;
- confirmation requirement;
- add / edit / delete / reorder availability;
- snapshot and realtime identity.

## Backend Validation Direction

Backend validation is authoritative. Frontend schema-driven rendering and filtering must only make the UI easier to use.

Phase 2 implementation adds:

- `ActionCapabilityMatrix` and `ActionOwnerCapability` under `com.zcpu.tzzmod.action.schema`;
- `ActionDraft`, `ActionValidationService`, `ActionValidationResult` and `ActionValidationError` under `com.zcpu.tzzmod.action.validation`;
- owner-service integration for ActionRelay, SignalListener, Region enter/exit/stay and Timer start/tick/complete/cancel buckets.

Validation now covers:

- unknown type fail-closed before legacy `ActionType.fromId` fallback is used for saves;
- owner supports action type;
- required fields present;
- type and range checks;
- channel syntax rules where applicable;
- state variable operation compatibility;
- timer target and missing policy compatibility;
- condition group compatibility by owner/bucket/action target;
- dangerous command policy / confirmation;
- preservation of old error code compatibility where callers depend on it.

State-variable existence and channel metadata existence are not upgraded into new cross-store requirements in Phase 2, because that would change current save compatibility. Phase 2 keeps validation aligned with existing owner services while centralizing the facts they already enforce.

## Phase 3 Frontend Renderer

Phase 3 adds Java-string frontend modules:

```text
src/main/java/com/zcpu/tzzmod/webadmin/WebAdminActionSchemaScripts.java
src/main/java/com/zcpu/tzzmod/webadmin/WebAdminActionFieldRenderScripts.java
```

`WebAdminActionSchemaScripts` exports the existing Java `ActionSchemaRegistry` / `ActionCapabilityMatrix` facts into a static readonly `TZZ_ACTION_EDITOR_DATA` structure. It provides helpers such as `actionSchemaByType`, `actionOwnerId`, `actionSupportedTypesForOwner` and `actionTypeOptions`. Unknown owners fail closed unless a caller passes an explicit fallback list; explicit non-owners remain VBD trigger, itemSubmit, container and branch.

`WebAdminActionFieldRenderScripts` renders value fields for the current action types. It delegates `state_variable` to the existing state action editor and `timer_start` / `timer_cancel` to the existing timer action editor, preserving legacy field ids and sync helpers. Signal actions use owner-specific channel comboboxes when available; the fallback uses the existing custom `channel-combo` style and never uses native `<datalist>`.

The renderer only builds UI fields. It does not save data, create APIs, validate writes, execute actions, change `ActionConfig` payloads or replace owner-specific lock / fingerprint / audit / realtime boundaries.

## Compatibility Requirements

- Old `ActionConfig` JSON remains readable.
- Save payload field names remain stable.
- `WebAdminWriteResult` fields and code ids remain stable.
- Existing owner action list order and same-index edit semantics remain stable.
- Snapshot storage remains resource-level unless a later phase explicitly designs an additive typed summary layer.

## Summary / Audit Boundary

Phase 4 implements WebAdmin-only summary helpers:

```text
src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionSummaryService.java
src/main/java/com/zcpu/tzzmod/webadmin/WebAdminActionSummaryScripts.java
```

They produce human Chinese summaries such as:

- `发送信号到频道 xxx`
- `设置状态变量 xxx = yyy`
- `执行命令 /xxx`
- `向玩家显示消息 xxx`
- `启动计时器 xxx`
- `取消计时器 xxx`

The backend service has separate display and audit methods. Display summaries are used by cards, action lists and snapshot diff rows; audit summaries keep command redaction and stable state/timer fingerprints where needed. The frontend helper mirrors display logic for draft UI and unsaved diff text only.

Summary helpers must not execute actions, validate writes, read stores, create APIs, change `ActionConfig` payloads, change dirty comparison, or write snapshot storage. Raw JSON must not become the primary user-facing summary.

## Extension Rule

Adding a new action type in the future must be a separate scoped task. It must update runtime, schema, capability, validation, editor, summary, docs, Obsidian and guard together.
