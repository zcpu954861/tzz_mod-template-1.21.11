# WebAdmin Editing Capability Matrix 7.14

This matrix summarizes the current 7.x WebAdmin editing layer at the 7.14 stabilization point. It is a documentation snapshot, not a new feature specification.

## Capability Matrix

| Module | Current WebUI Capability | Editable Scope | Readonly Scope | Not Done / Notes |
| --- | --- | --- | --- | --- |
| Dashboard | Overview and quick navigation. | None. | Server status, device/signal/doctor/history summaries. | Dashboard remains an observation surface. |
| Device metadata | Device detail metadata card and modal. | WebAdmin-only display name, note, icon key. | Device identity, runtime state, debug and Doctor summaries. | Does not change game logic. |
| Physical device basic config | Device config modal. | Enabled and primary channel where supported. | Device type, world, position, runtime state. | No raw JSON editing. |
| Physical device extended config | Device config modal. | Supported low-risk fields such as VBD interact channel, success/fail channel, cooldowns, receiver pulse ticks, action relay cooldown. | Unsupported fields are shown as readonly/unavailable. | Does not expose itemSubmit or matcher through this layer. |
| VBD native trigger | VBD native trigger section and modal. | Redstone, blockstate, right-click, container open/close/change trigger fields supported by existing service. | Bound block, runtime status, validation issues. | No ConditionEngine and no new trigger source. |
| Interaction matcher | VBD interaction item matcher UI. | Template, source, matcher options, count mode, vanilla interaction policy within existing runtime semantics. | Runtime result and diagnostic summaries. | No arbitrary NBT path editor. |
| itemSubmit unified editor | Unified requirement list editor and WebAdmin entry/summary. | 0/1/N requirements, template item, enabled, count mode, matcher options, consume count, global consume/order/policy fields. | Runtime last result fields and diagnostic summaries. | itemSubmit remains under right-click interaction; no new trigger source. |
| Container template | Container item condition template session. | Slot/total item template conditions through existing GUI/session path. | Existing itemConditions summaries. | No raw JSON and no arbitrary container scan. |
| Signal channel metadata | Signal channel detail metadata modal. | WebAdmin-only display name, note, icon key. | Channel consumers, recent events, Doctor/history. | Metadata does not create real channels or consumers. |
| ActionRelay | Device detail action summary card and action list modal. | Action add, single delete, clear, enabled, cooldown, requiresOp/notifyOps for command actions. | Action count, summaries, runtime status. | Dangerous commands remain blocked; no raw JSON. |
| SignalReceiver | Device detail and config modal. | Receiver channel through basic config and pulse ticks where loaded/supported. | Redstone output state and history. | No standalone receiver CRUD. |
| RegionController | Region controller list/detail/editing. | Create, delete, enabled, name, regionId, target filter, stay interval, enter/exit/stay action add/delete/clear. | Runtime status, Doctor/history, region catalog summaries. | Runtime semantics unchanged; no path graph. |
| SignalListener | Virtual listener list/detail/editing. | Create, delete, enabled, channel, cooldown, action add/single delete/clear, dynamic action fields. | Recent events, runtime summaries, Doctor/history/channel links. | SignalBridge runtime is not rewritten. |
| Logic Chain Viewer | Cross-channel logic chain list and read-only mind-map/tree viewer. | WebAdmin-only view metadata: display name, note, icon key, tags/group, root type/ref, include disabled, max depth, layout preference. | SignalBridge-derived channel tree, producers, same-channel parallel consumers, ordered actions, downstream channel child subtrees, main/sub-chain hierarchy, warnings, cycle/max-depth references. | 7.15 MVP only; draggable viewport and SVG curved connectors; no runtime editing, no ConditionEngine, no table-like fixed lane layout, no long cross-channel line mixing. |
| Users | Users page and password tools. | Current-user password change and OWNER password reset/set API. | User list/session summaries according to role. | Full WebUI user CRUD remains limited. |
| Settings | Settings/config page. | No broad settings editing. | WebAdmin server status, storage/security summaries. | Do not expose host/port/access mode editing as completed. |
| Doctor | Diagnostic page. | None. | Issues, severity, affected targets, suggested action. | No auto-fix button. |
| History | Signal history page. | None. | Timeline, filters, recent signal events. | No history deletion/replay from WebAdmin. |
| Local Test MCP | Local auxiliary tooling. | Safe local reports/screenshots/scenarios as explicit tools. | Health, repo status, WebAdmin diagnostics, TestBridge inspection. | Manual testing is primary; MCP does not replace user acceptance. |

## Current Cross-Cutting Guarantees

- WebAdmin writes require role permission, CSRF/same-origin, validation, edit lock where applicable, expected fingerprint, `WebAdminWriteResult`, audit, and realtime.
- Detail-page editing should preserve scroll, tab, filters, open modals, and unsaved form state during silent refresh.
- Channel and region selectors should use dark combobox/searchable selector patterns.
- Action lists should not directly expand unbounded rows on detail pages.
- Delete/clear confirmations should be clear and should not require typing IDs/names unless explicitly requested for a future high-risk flow.

## Known Consistency Follow-ups

- Older typed-delete confirmations and lifecycle/delete edit-lock behavior should be normalized in focused follow-up passes, not hidden by this matrix.
- Same-origin and CSRF behavior, failure-audit realtime publication, and create/delete fingerprint coverage should remain visible as write-safety review topics.
- Region selectors are selectors for existing region IDs; if the selected/current value is missing from the catalog, WebUI should preserve it and show a warning instead of clearing it.

## Explicit Future Work

- 8.x: ConditionEngine / conditional decision layer.
- Later: GameController / MissionSystem / PhaseController.
- Later: any graph/path visualization or Scratch-like editor, if still desired.
