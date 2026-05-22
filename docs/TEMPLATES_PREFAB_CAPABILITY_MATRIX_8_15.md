# Templates / Prefab Capability Matrix 8.15

## Scope

8.15 Templates / Prefab / Import-Export introduces reusable template packages and a WebAdmin Template Center. It is a prefab system for existing low-level config stores, not a new runtime.

| Capability | Status | Notes |
| --- | --- | --- |
| Template Center / 模板中心 | Implemented | Route `#/templates`, separate from legacy `#/action-templates`. |
| schema `tzz_template_v1` | Implemented | `WebAdminTemplatePackage` includes parameters, resources, placeholders and metadata. |
| built-in starter templates | Implemented | `join_all_two_inputs`, `timer_delay_with_start_listener`, `listener_message_action`. |
| built-in template detail | Implemented | Detail route preserves `source=built_in`, ignores `returnTo` query text during id parsing, and backend blank-source lookup checks built-in before user templates. |
| user template store | Implemented | World-scoped `templates.json`; bad file fallback blocks writes. |
| Import JSON | Implemented | Preview first; save as user template only; import does not apply. |
| Export JSON | Implemented | Detail/export endpoint returns normalized JSON for copy/download. |
| dry-run apply | Implemented | Lists creates, conflicts, missing placeholders, deferred resources, warnings and expectedFingerprint. |
| safe apply | Implemented | Recomputes dry-run, requires confirmation, edit lock, expectedFingerprint, audit and realtime. |
| channel metadata apply | Implemented | Writes WebAdmin channel metadata, no SignalBridge runtime emission. |
| SignalJoinDefinition apply | Implemented | Writes `signal_joins.json`. |
| TimerDefinition apply | Implemented | Writes `timers.json`. |
| SignalListener / ActionConfig apply | Implemented | Writes `signal_listeners.json` with owned listener actions for safe local action types. |
| Logic Chain visibility | Implemented | Writes WebAdmin logic chain metadata so applied template can be opened from the viewer; existing `<prefix>.template` metadata conflicts block apply. The Logic Chain list renders one connected component entry with `componentId`, `defaultFocusChannel` and `includedChannels`, merges saved metadata roots with auto-discovered channels in the same component, keeps weak ConditionGroup / StateVariable references out of list merging, and moves focus-channel switching into the detail page. |
| Template detail JSON layout | Implemented | JSON preview, copy and download actions live in the right rail using the shared two-column/stretch detail layout with responsive fallback markers. |
| Template UI localization | Implemented | Primary labels for resources, sources and apply preview/apply actions are Chinese outside the JSON payload. |
| StateVariable definitions | Deferred | StateVariable definition apply deferred. |
| ConditionGroup definitions | Deferred | ConditionGroup apply deferred; embedded Listener / ActionConfig conditionGroupId references are blocked rather than copied through. |
| component export | Deferred | component export deferred because reverse dependency extraction needs a separate graph/resource tracing stage. |
| world entity copy | Not implemented | Placeholder binding apply deferred; no automatic block/region/device creation. |
| external reference fail closed | Implemented | Join/Timer/Listener/Action channel and timer references must be declared in template resources or explicit rootChannel remap. |
| overwrite/merge policy | Not implemented | conflict policy is fail closed. |
| template apply recovery protection | Implemented | Since 8.18, template import/apply routes create write-before auto snapshots and annotate successful operation diff, so the protecting save point shows what import/apply changed. |
| full multi-store transaction rollback | Deferred | Apply validates first, then saves stores in a fixed order; a later save failure returns failure/audit context. Snapshot/Rollback is configuration recovery, not an in-service transaction rollback. |
| template marketplace | Not implemented | Local import/export only. |

## Apply Resource Matrix

| Resource | Can dry-run | Can apply | Conflict behavior |
| --- | --- | --- | --- |
| Channel metadata | Yes | Yes | Existing metadata conflicts; no silent overwrite. |
| SignalJoinDefinition | Yes | Yes | Existing ID conflicts. |
| TimerDefinition | Yes | Yes | Existing ID conflicts. |
| SignalListener | Yes | Yes | Existing ID or display name conflicts. |
| ActionConfig | Yes | Yes, as owned listener/timer actions for message/signal/timer control | No old action move/delete/reorder. Top-level ActionResource, StateVariable action binding and command action apply deferred. |
| StateVariable | Yes, listed as deferred | No | Blocks apply as deferred. |
| ConditionGroup | Yes, listed as deferred | No | Blocks apply as deferred. |
| Region / VBD / ActionRelay block / coordinates | Placeholder only | No automatic creation | Missing mapping blocks apply; mapped placeholders remain deferred until typed binding exists. |

## Security Matrix

| Operation | Permission | CSRF / Same-Origin | Edit Lock | Fingerprint | Audit / Realtime |
| --- | --- | --- | --- | --- | --- |
| List/detail/export | READ | No write token | No | Template fingerprint returned | No write event. |
| Import preview | READ | No write lock | No | No write | No write event. |
| Import save | `IMPORT_TEMPLATE` | Required | `TARGET_TEMPLATE_STORE` | user store expectedFingerprint | audit + `TEMPLATE_STORE_CHANGED`. |
| Apply preview | READ + CSRF/same-origin | Required | No | plan expectedFingerprint returned | No write event. |
| Apply write | `APPLY_TEMPLATE` | Required | `TARGET_TEMPLATE_APPLY` | plan expectedFingerprint | audit + `CONFIG_CHANGED` + `TEMPLATE_APPLIED`. |

Apply rejects stale plan fingerprints. Import save rejects stale user template store fingerprints. Both paths keep the failure read-only.

## UI Markers

- `data-template-center-nav`
- `data-template-list-route`
- `data-template-detail-route`
- `data-template-apply-wizard`
- `data-template-dry-run-preview`
- `data-template-import-json-modal`
- `data-template-export-json-action`
- `data-template-built-in-detail-route`
- `data-template-detail-source-built-in`
- `data-template-detail-export-apply-visible`
- `data-template-no-browser-dialogs`
- `data-template-placeholder-mapping`
- `data-template-detail-right-json-preview`
- `data-template-json-copy-in-right-panel`
- `data-template-json-download-in-right-panel`
- `data-template-detail-two-column-stretch`
- `data-template-detail-responsive-stack`
- `data-logic-chain-list-metadata-first`
- `data-logic-chain-one-entry-per-component`
- `data-logic-chain-list-no-duplicate-component-channels`
- `data-logic-chain-component-entry-list`
- `data-logic-chain-component-entry`
- `data-logic-chain-included-channel-count`
- `data-logic-chain-source-metadata`
- `data-logic-chain-source-auto-component`
- `data-logic-chain-detail-focus-channel-selector`
- `data-logic-chain-focus-switch-updates-route-state`
- `data-logic-chain-old-channel-route-compatible`

The UI keeps WebAdmin dark admin styling, Chinese primary copy, top-center toast behavior and no browser `alert` / `confirm` / `prompt`.

## Non-Goals

8.15 does not implement GameController, MissionSystem, PhaseController, full Logic Chain Editor, Scratch editor, if / else runtime, old node move/delete/reorder, old action move/delete/reorder, runtime semantic changes, new ActionType, new ConditionNodeType, automatic world entity creation, Git-like branch/merge/rebase system or marketplace sync.
