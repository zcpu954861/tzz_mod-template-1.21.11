# 8.15 Templates / Prefab / Import-Export Current Context

## 8.15 Templates / Prefab / Import-Export

8.15 adds the WebAdmin template system for reusable Signal / Join / Timer / SignalListener structures. The goal is to let admins start from built-in starter templates or user-imported prefab JSON, preview the exact resources that will be created, then safe apply into existing real config stores.

This stage adds:

- Template package schema `tzz_template_v1`.
- Template Center / 模板中心 route `#/templates`.
- built-in starter templates:
  - `join_all_two_inputs`
  - `timer_delay_with_start_listener`
  - `listener_message_action`
- user template store at world-scoped `templates.json`.
- Import JSON preview and save-as-user-template.
- Export JSON from template detail.
- apply dry-run and safe apply with prefix remap, conflict policy, placeholder policy, permission, CSRF / same-origin, edit lock, expectedFingerprint, audit and realtime.

Built-in template detail lookup is explicit-source safe: the route keeps `source=built_in` separate from the template id, strips `returnTo` query parameters before id decoding, and the backend can also resolve blank-source detail/export requests by checking built-in templates before the user template store. Detail errors remain distinct: `template_not_found`, `template_permission_denied`, `template_source_invalid` and `template_schema_invalid` are not collapsed into a generic permission/not-found message.

Template detail uses the shared WebAdmin fixed two-column detail layout. The left column contains the template overview, included resources, placeholder / deferred-resource notes and warnings. The right rail contains apply/export operations, the JSON preview, JSON copy/download actions and compatibility notes. The JSON preview is intentionally a right-rail panel, not the main left-column content, so long package JSON no longer makes the main content column much taller than the operation column.

Template Center primary UI copy is Chinese outside the JSON payload itself. Resource type labels map `channel` to 频道, `signalJoin` to 信号汇合, `timer` to 计时器, `signalListener` to 信号监听器, `action` / `actions` to 动作, `stateVariable` to 状态变量, `conditionGroup` to 条件组 and `placeholder` to 占位引用. Template source labels show 内置, 用户模板, 导入模板 and 导出组件. Apply flow buttons use 预览 / 应用 wording while preserving the backend dry-run / apply API semantics.

Logic Chain list entry semantics are whole-component based. The list groups strong Signal / Join / Timer / Listener / Action signal-output associations into one connected component entry with `componentId`, `defaultFocusChannel` and `includedChannels`; it does not create one row per channel. Saved metadata roots and auto-discovered channels inside the same component merge into one entry, preferring the saved metadata name/source, while auto-discovered-only components appear once. Shared ConditionGroup or StateVariable references are weak associations and do not merge unrelated components into a global graph. The detail page provides a focus channel selector; switching focus updates the `focusChannel` route/API state and re-renders the viewer for that focus without creating a new list row. Direct old channel-style routes such as `#/logic-chains?channel=...` resolve to the owning component detail with that channel as focus.

## Schema

`WebAdminTemplatePackage` is the package DTO. It includes schema version, template id, display name, category, parameters, resources, placeholders and metadata.

Resources currently supported for real apply:

- channel metadata
- `SignalJoinDefinition`
- `TimerDefinition`
- `SignalListener`
- owned `ActionConfig` entries inside listener/timer definitions for safe local actions such as message, signal and timer control

Top-level ActionResource apply deferred; owned `ActionConfig` is supported only inside Listener / Timer resources where the owner is created by the same template and the action type does not require unresolved external binding. StateVariable definition apply deferred. StateVariable action binding apply deferred. Command action apply deferred because command text can hide world entity or coordinate references that 8.15 cannot safely bind. ConditionGroup apply deferred. Embedded `conditionGroupId` references inside Listener or ActionConfig are blocked during apply instead of being copied through to the target world. These resource arrays can be represented in JSON, but apply blocks with a deferred warning until a later stage defines safe cross-store creation semantics.

## Store

User templates are stored in `templates.json` under the WebAdmin world-scoped directory. Built-in templates are code-defined by `WebAdminBuiltInTemplates` and are never written into the user store unless imported/copied by the user.

Bad or unreadable `templates.json` degrades list/detail and blocks writes so WebAdmin does not overwrite a damaged file.

## Import JSON

Import JSON validates `tzz_template_v1`, rejects invalid JSON and unknown schema versions with Chinese errors, then previews what would be saved. Import does not apply config, does not emit signals, and does not execute actions.

Saving an import requires permission, CSRF / same-origin, `TARGET_TEMPLATE_STORE` edit lock, expectedFingerprint, audit and `TEMPLATE_STORE_CHANGED` realtime.

## Export JSON

Template detail can export the normalized package JSON. The frontend exposes download and copy actions. Component export deferred: reverse-exporting an existing Logic Chain component needs dependency tracing across channel metadata, SignalJoin, Timer, SignalListener, action owner buckets, placeholders and world entity references. 8.15 deliberately ships template JSON export first.

## Apply / Instantiate

world entity limitations: 8.15 treats Region, VBD, ActionRelay block, coordinates and other physical-world references as external placeholders only.

Apply flow:

1. User selects a template.
2. User fills prefix, optional root channel, displayNamePrefix and placeholder mappings.
3. WebAdmin performs dry-run.
4. Dry-run shows channels, SignalJoins, Timers, SignalListeners, Action count, conflicts, missing placeholders, deferred resources and warnings.
5. User confirms.
6. Backend re-computes the plan, validates expectedFingerprint, then writes real stores.

Apply writes真实配置, never a fake graph:

- channel metadata to WebAdmin channel metadata store
- logic chain metadata for a view entry
- SignalJoinDefinition to `signal_joins.json`
- TimerDefinition to `timers.json`
- SignalListener config to `signal_listeners.json`

Conflict policy: default fail closed. Existing channel metadata, SignalJoin, Timer or SignalListener IDs are not overwritten. Re-applying the same prefix fails with conflicts; using a different prefix succeeds.

Placeholder policy: world entity placeholders such as regions, VBDs, ActionRelay blocks, coordinates and other external references are not auto-created or copied. Missing required placeholders block apply. Placeholder binding apply deferred for 8.15: because the package schema has no field-level binding path yet, a template that contains placeholders can be imported/exported and previewed, but apply stays fail closed even when the user fills a string mapping. A later stage must add typed placeholder binding before those references can be written into real config.

External reference fail closed: channel and Timer references used by Join, Timer, Listener or owned ActionConfig must come from the template resource graph and be remapped through `resources.channels` / `resources.timers`, or through the explicit `rootChannel` remap for root-like channels. Unknown channel/timer IDs are not copied through to the target server.

Logic Chain metadata conflict policy: safe apply also plans the WebAdmin logic-chain metadata entry (`<prefix>.template`). Existing metadata with the same ID is reported in dry-run conflicts and blocks apply, so template apply does not silently overwrite viewer entries.

Expected fingerprint policy: dry-run returns the apply plan fingerprint. Apply recomputes the plan, including conflicts and planned real-store creates, and rejects stale fingerprints before writing.

Write failure policy: 8.15 validates all planned resources before writing and then saves the existing stores in a fixed order. If a later store save fails, the backend returns a failure result and audit context, but full multi-store transaction rollback is deferred to a later snapshot/rollback stage.

## Boundaries

8.15 does not change runtime semantics. It does not emit signals, start timers, execute actions, move old nodes, delete old nodes, reorder old actions or add new `ActionType` / `ConditionNodeType`.

This stage does not implement:

- 不恢复旧 full Logic Chain Editor
- Scratch editor
- if / else runtime
- GameController
- MissionSystem
- PhaseController
- 不做 GameController
- 不做 MissionSystem
- 不做 PhaseController
- version rollback
- external template marketplace
- automatic world entity copy or block placement

Future direction:

- component export deferred follow-up
- stronger template parameters
- batch rename / remap policy
- richer placeholder binding UI
- template marketplace after local safety is stable
- simple snapshot / rollback integration with template apply
