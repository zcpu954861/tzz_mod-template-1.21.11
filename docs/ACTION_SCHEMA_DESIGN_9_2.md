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

Names may be adjusted to match code style during implementation.

| Type | Responsibility |
| --- | --- |
| `ActionSchema` | Metadata for one existing action type. |
| `ActionFieldSchema` | Metadata for one saved / derived field. |
| `ActionFieldType` | `text`, `number`, `boolean`, `select`, `textarea`, `channel_picker`, `state_variable_picker`, `condition_group_picker`, `player_target_mode`, `readonly_summary`. |
| `ActionOwnerType` | Existing owners and buckets: listener, relay, region enter/exit/stay, timer start/tick/complete/cancel. |
| `ActionCapability` | Owner support for an action type and operation. |
| `ActionSchemaRegistry` | Static immutable lookup by `ActionType`. |
| `ActionCapabilityMatrix` | Static immutable lookup by owner/bucket. |

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
| `state_variable` | state operation, scope, target mode, target id, key, value type, value, delta, create-if-missing, initial value. |
| `timer_start` | timer id, target mode, target player name, missing policy. |
| `timer_cancel` | timer id, target mode, target player name, missing policy. |

Common fields remain `type`, `enabled`, `cooldownTicks`, `requiresOp`, `notifyOps` and `conditionGroupId`.

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

Validation must eventually cover:

- unknown type fail-closed before legacy `ActionType.fromId` fallback is used for saves;
- owner supports action type;
- required fields present;
- type and range checks;
- channel syntax and existence rules where applicable;
- state variable operation compatibility;
- timer target and missing policy compatibility;
- condition group compatibility by owner/bucket/action target;
- dangerous command policy / confirmation;
- preservation of old error code compatibility where callers depend on it.

## Compatibility Requirements

- Old `ActionConfig` JSON remains readable.
- Save payload field names remain stable.
- `WebAdminWriteResult` fields and code ids remain stable.
- Existing owner action list order and same-index edit semantics remain stable.
- Snapshot storage remains resource-level unless a later phase explicitly designs an additive typed summary layer.

## Summary / Audit Direction

Later phases should produce human Chinese summaries such as:

- `发送信号到频道 xxx`
- `设置状态变量 xxx = yyy`
- `执行命令 /xxx`
- `向玩家显示消息 xxx`
- `启动计时器 xxx`
- `取消计时器 xxx`

Command summaries must keep current redaction / safety boundaries. Raw JSON must not become the primary user-facing summary.

## Extension Rule

Adding a new action type in the future must be a separate scoped task. It must update runtime, schema, capability, validation, editor, summary, docs, Obsidian and guard together.
