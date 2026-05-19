# Logic Chain Editor Capability Matrix 8.14

## Scope

8.14 is the first controlled Logic Chain Editor MVP. Editing stays inside the current Viewer canvas and supports new pure-config node creation, placement, draft connection, append-only ActionConfig creation for existing action containers, and save to existing typed config. It is not a full Logic Chain Editor.

| Capability | Status | Notes |
| --- | --- | --- |
| current Viewer canvas edit mode | Implemented | No new editor route; the existing Logic Chain Viewer gains an edit toolbar |
| dedicated edit lock | Implemented | Uses `logic_chain_editor` and `EDIT_LOGIC_CHAIN` |
| stable editor lock target | Implemented | Enter returns canonical targetType / targetId; heartbeat, save and cancel reuse it instead of recomputing from refreshed graph state; validation and typed save failures preserve the main editor lock |
| base graph fingerprint | Implemented | Save rejects stale `baseGraphFingerprint` |
| Signal Join new node | Implemented | Pure config node, legal visual columns are derived from upstream channel cards |
| Timer new node | Implemented | Pure config node, legal column C0; C5 Timer reference / target placement is deferred |
| nearest-slot-only placement | Implemented | Join slots appear in the visible downstream column of channel / downstream channel / channel reference / draft channel endpoint cards; empty target columns show one context-middle slot, while occupied target columns, including listener/action/consumer columns, expose one middle insert slot per existing-card gap plus bottom append without filling a far-empty column. Timer / draft channel reference / action append slots stay context-local; Join draft mode no longer forces an empty processing column |
| shared output channel endpoint | Implemented | Signal Join and Timer can select existing downstream channels or create draft channel endpoints; saved config stores real channel ids only |
| Timer action graph visibility | Implemented | `timer_start` / `timer_cancel` actions and Timer bucket actions render as graph nodes and link to the target Timer / output channel |
| signal action output placement | Implemented | Listener signal actions render as action -> downstream channel, with consumers after the output channel and no far-left listener producer duplicate |
| Action append to existing action containers | Implemented | Append one new ActionConfig to SignalListener, ActionRelay, Region enter / exit / stay, or Timer tick / complete lists; edit preview uses the same right-side action lane resolver as the saved graph and no longer clamps listener append previews into the same/wrong column |
| guided config modal | Implemented | ID, display name and node-intrinsic fields collected before draft placement; Join ALL hides threshold, ANY_N / COUNT show threshold; topology fields are not handwritten |
| white dashed legal slot outline | Implemented | Legal slot / drop preview markers rendered on the canvas |
| real pointer drag | Implemented | Draft node starts unplaced, follows pointer movement, previews only near a legal slot and snaps on pointerup |
| placed draft card re-drag | Implemented | Only the current draft card can be picked up again before save; old nodes remain fixed |
| snap to canonical slot | Implemented | Draft node stores column + slot instead of free coordinates |
| new node drag only | Implemented | Old node drag disabled; only draft card uses pointer drag marker |
| green plus connection handles | Implemented | Upstream / downstream connection mode markers, larger hit target and event delegation |
| connection mode close | Implemented | Clicking the active same-side plus exits; switching sides changes mode; Escape / blank canvas exits |
| draft edge toggle | Implemented | Clicking the same connected candidate removes that draft edge and validation recomputes from final edges |
| draft channel endpoint picker | Implemented | Dark searchable combobox lists existing channel displayName + id and accepts new channel ids |
| channel metadata drafts | Implemented | New channel endpoint metadata is saved through WebAdmin channel metadata after the typed write succeeds; unreferenced / orphan metadata drafts are pruned or rejected; action append mode rejects draft edges and only accepts metadata referenced by a Signal action value |
| new logic chain entry | Implemented | Logic Chain list can create root channel / logic chain metadata for an independent chain |
| existing channel reference card | Implemented | Uses same-side primary or reference channel cards first; creates visual-only reference cards only when no same-side reusable card exists |
| local reference card placement | Implemented | Reference cards use nearest free slot around the draft node on the required side, not first free slot from column top |
| Join input / output mutual exclusion | Implemented | Same Join rejects any channel used as both inputChannels and outputChannel |
| visual lane independent Join semantics | Implemented | Visual upstream / downstream placement does not decide input/output membership; selected input/output edges do |
| Join cycle guard | Implemented | Bounded traversal rejects output paths that return to selected input channels, reports the concrete cycle path, and rejects truncation instead of infinite expansion |
| highlighted draft edge | Implemented | New edge remains highlighted until save / cancel |
| save validation | Implemented | Frontend and backend validation, backend authoritative; Join / Timer topology is derived from draft edges |
| save writes underlying config | Implemented | Calls existing Signal Join / Timer / action append typed services |
| dirty exit dialog | Implemented | Custom modal, no browser confirm |
| dirty route guard | Implemented | Dirty draft can only stay on the original editor route or silent refresh; other Logic Chain route changes prompt before discard |
| top-center toast | Implemented | Custom toast, auto dismiss marker |
| no runtime mutation from viewer | Implemented | Editor writes config only; no emit, no action execution |
| world entity create | Deferred | World entities must exist first; future in-game draft create / cancel rollback is documented but not implemented |
| virtual SignalListener create from canvas | Deferred | SignalListener is pure config and does not need a world entity, but the canvas create path is disabled until listener create has a Logic Chain editor-safe edit lock flow |
| old node move | Deferred | Not in 8.14 |
| old node delete | Deferred | Not in 8.14 |
| old node reorder | Deferred | Not in 8.14 |
| full Logic Chain Editor | Deferred | Separate future stage |
| Scratch editor | Deferred | Separate future stage |
| if / else runtime | Deferred | Separate future stage |
| GameController | Deferred | Separate future stage |
| MissionSystem | Deferred | Separate future stage |
| PhaseController | Deferred | Separate future stage |

## Node Capability

| Node Type | Can Create | Pure Config | World Entity Requirement | Required Config | Required Edges | Save Target |
| --- | --- | --- | --- | --- | --- | --- |
| `signal_join` | Yes | Yes | No | ID, mode, scope, reset policy; threshold only for ANY_N / COUNT | at least two `join_input`, exactly one `join_output` | `SignalJoinDefinition`; inputChannels / outputChannel derived from edges |
| `timer` | Yes | Yes | No | ID, mode, scope, duration / interval / maxRuns, start policy | one `timer_outputs_channel`, unless valid onCompleteActions already exist | `TimerDefinition`; outputChannel derived from edge |
| `signal_listener` / virtual listener | No | Yes | No | Pure config listener create exists elsewhere, but Logic Chain canvas create is blocked until a safe editor lock path is added | Deferred | Use SignalListener page first, then edit actions through existing append-only path |
| `action_append` | Append-only | Yes | Owner must already exist | ownerType, ownerId, bucket, one ActionConfig, expectedFingerprint, typed lockId | none; action is appended to owner list | Existing services: SignalListener actions, ActionRelay actions, Region enter / exit / stay actions, Timer onTick / onComplete actions |
| `state_action` | No | Yes | No | Direct StateAction node creation is deferred; state_variable actions can still be appended through `action_append` when existing action validation allows them | Deferred | Deferred |
| `action` | Append-only | Yes | Existing action list owner required | Supported only through `action_append`; old action move / delete / reorder / complex edit remains deferred | none | Existing owner action list; no old action mutation |
| `condition_gate` | No | Reference/config | No | Deferred because target binding edit is not implemented in this stage | Deferred | Deferred |
| `action_gate` | No | Reference/config | No | Deferred because action owner/index mapping is not implemented in this stage | Deferred | Deferred |
| `producer` / world device | No | No | Must already exist | Select existing in future stage | Deferred | Deferred |
| `consumer` / listener / receiver / relay | No | Mixed | Must already exist | Select existing in future stage | Deferred | Deferred |
| `region_controller` | No | No | Must already exist | Select existing in future stage | Deferred | Deferred |

## API

| API | Method | Status | Purpose |
| --- | --- | --- | --- |
| `/api/webadmin/logic-chain-editor/capabilities` | GET | Implemented | Lists supported and deferred node capabilities |
| `/api/webadmin/logic-chain-editor/enter` | POST | Implemented | Permission / CSRF / same-origin / edit lock / graph fingerprint |
| `/api/webadmin/logic-chain-editor/validate-draft` | POST | Implemented | Validates draft without writing config |
| `/api/webadmin/logic-chain-editor/save-draft` | POST | Implemented | Validates and writes Signal Join, Timer, channel metadata drafts, or one append-only ActionConfig |
| `/api/webadmin/logic-chain-editor/cancel` | POST | Implemented | Releases editor lock and discards draft |
| `/api/webadmin/logic-chains` | POST | Implemented | Creates root channel / logic chain metadata for a new independent chain |

## Guard Markers

Required UI / guard markers:

- `data-logic-chain-editor-mvp`
- `data-logic-chain-edit-mode-toggle`
- `data-logic-chain-edit-lock-status`
- `data-logic-chain-lock-target-stable`
- `data-logic-chain-save-failure-preserves-lock`
- `data-logic-chain-save-failure-keeps-edit-session`
- `data-logic-chain-save-failure-keeps-lock`
- `data-logic-chain-second-save-after-validation-fail`
- `data-logic-chain-validation-focus-preserved`
- `data-logic-chain-action-append-lock-required`
- `data-logic-chain-edit-locked-disabled`
- `data-logic-chain-draft-preserved`
- `data-logic-chain-dirty-confirm`
- `data-logic-chain-dirty-modal`
- `data-logic-chain-topology-from-edges`
- `data-logic-chain-no-manual-topology-inputs`
- `data-logic-chain-drag-slot-palette`
- `data-logic-chain-drag-slot-canvas`
- `data-logic-chain-pointer-drag`
- `data-logic-chain-new-node-placement`
- `data-logic-chain-connection-mode`
- `data-logic-chain-save-validation`
- `data-logic-chain-validation-list`
- `data-logic-chain-validation-detail-list`
- `data-logic-chain-structured-validation-errors`
- `data-logic-chain-validation-channel-id`
- `data-logic-chain-validation-fix-hint`
- `data-logic-chain-save-error-reason-visible`
- `data-logic-chain-no-browser-dialogs`
- `data-logic-chain-no-runtime-mutation`
- `data-logic-chain-save-writes-underlying-config`
- `data-logic-chain-world-entity-requires-existing`
- `data-logic-chain-snap-to-canonical-slot`
- `data-logic-chain-nearest-slot-only`
- `data-logic-chain-far-empty-slot-hidden`
- `data-logic-chain-slot-context-derived`
- `data-logic-chain-slot-context-anchor`
- `data-logic-chain-all-draft-types-nearest-slot-policy`
- `data-logic-chain-join-visual-downstream-column`
- `data-logic-chain-join-visual-downstream-slot`
- `data-logic-chain-join-semantic-lane-preserved`
- `data-logic-chain-join-slot-input-channel-adjacent`
- `data-logic-chain-join-slot-hidden-without-input-context`
- `data-logic-chain-join-slot-shared-input-band`
- `data-logic-chain-join-slot-left-channel-column`
- `data-logic-chain-join-slot-upstream-channel-column`
- `data-logic-chain-join-slot-downstream-of-channel`
- `data-logic-chain-join-slot-target-column-may-contain-listener`
- `data-logic-chain-join-slot-no-forced-empty-processing-column`
- `data-logic-chain-join-slot-dynamic-columns`
- `data-logic-chain-join-slot-empty-column-single-middle`
- `data-logic-chain-join-slot-occupied-column-insert-anywhere`
- `data-logic-chain-join-slot-bottom-append`
- `data-logic-chain-join-slot-multi-gap`
- `data-logic-chain-join-slot-not-median-only`
- `data-logic-chain-slot-cannot-overlap-existing-node`
- `data-logic-chain-same-column-make-room`
- `data-logic-chain-green-plus-handle`
- `data-logic-chain-large-hit-target`
- `data-logic-chain-event-delegation`
- `data-logic-chain-new-edge-highlighted`
- `data-logic-chain-new-edge-remains-highlighted`
- `data-logic-chain-join-all-hides-threshold`
- `data-logic-chain-join-any-n-shows-threshold`
- `data-logic-chain-join-count-shows-threshold`
- `data-logic-chain-draft-starts-unplaced`
- `data-logic-chain-slot-proximity`
- `data-logic-chain-snap-animation`
- `data-logic-chain-draft-edge-toggle`
- `data-logic-chain-connected-candidate`
- `data-logic-chain-draft-channel-node`
- `data-logic-chain-action-append`
- `data-logic-chain-action-append-only`
- `data-logic-chain-action-append-saved-layout-parity`
- `data-logic-chain-action-append-listener-right-lane`
- `data-logic-chain-no-old-action-move-delete-reorder`
- `data-logic-chain-join-input-output-mutual-exclusive`
- `data-logic-chain-reference-card-necessary-only`
- `data-logic-chain-reference-card-near-draft`
- `data-logic-chain-output-reference-right-side`
- `data-logic-chain-input-reference-left-side`
- `data-logic-chain-reference-slot-no-overlap`
- `data-logic-chain-visual-upstream-non-input-output-reference`
- `data-logic-chain-same-side-primary-no-reference`
- `data-logic-chain-same-side-reference-no-duplicate`
- `data-logic-chain-connection-mode-same-side-exits`
- `data-logic-chain-connection-mode-canvas-exits`
- `data-logic-chain-escape-exits-connection-mode`
- `data-logic-chain-channel-endpoint-picker`
- `data-logic-chain-channel-picker-existing-channel`
- `data-logic-chain-channel-picker-new-channel`
- `data-logic-chain-channel-metadata-drafts`
- `data-logic-chain-channel-endpoint-no-orphan-metadata`
- `data-logic-chain-output-capable-node`
- `data-logic-chain-timer-output-endpoint`
- `data-logic-chain-create-output-channel-endpoint`
- `data-logic-chain-output-endpoint-right-side`
- `data-logic-chain-shared-output-endpoint-flow`
- `data-logic-chain-placed-draft-redrag`
- `data-logic-chain-new-entry`
- `data-logic-chain-create-root-channel`
- `data-logic-chain-disconnected-draft-new-chain`
- `data-logic-chain-reference-card`
- `data-logic-chain-save-uses-real-channel-id`
- `data-logic-chain-world-entity-not-directly-creatable`
- `data-logic-chain-world-entity-future-game-draft-deferred`
- `data-logic-chain-virtual-listener-disabled-reason`
- `data-logic-chain-signal-listener-pure-config`
- `data-logic-chain-no-world-entity-required`
- `data-logic-chain-timer-action-detail-card`

## Test Matrix

| Area | Coverage |
| --- | --- |
| edit mode / lock | enter requires permission, CSRF and lock; lock conflict blocks edit mode |
| lock lifecycle | canonical lock target is preserved through heartbeat / save / cancel; missing lock and target mismatch fail with Chinese diagnostics; validation failure and typed save failure keep the same main editor lock and draft for retry; new Signal Join / Timer typed locks use the same normalized IDs as their typed services; only explicit main editor lock loss clears the lock |
| draft validation | missing draft, deferred node type, invalid slot / column |
| required config | missing Signal Join / Timer intrinsic config, missing upstream edge and missing downstream edge block save |
| edge validation | duplicate, incomplete, detached, wrong type, wrong endpoint, too few Join inputs, multiple outputs block save |
| structured error reporting | validation errors include Chinese reason plus code, related node / edge / channel when available, severity and fixHint; canvas detail list explains how to repair |
| fingerprint | stale base graph fingerprint blocks save |
| Signal Join save | draft saves through existing Signal Join service and appears in underlying config; raw IDs with spaces / uppercase are normalized before temporary typed lock acquisition |
| Timer save | draft saves through existing Timer service and appears in underlying config; raw IDs with spaces / uppercase are normalized before temporary typed lock acquisition |
| action append | owner / bucket validation covers SignalListener, ActionRelay, Region and Timer; Timer append proves existing order preservation and action conditionGroupId roundtrip |
| action append visual slot | frontend guard | appended draft action uses actionIndex in draft id, the canonical saved-layout action lane and nearest free slot; old action order is preserved |
| new logic chain root field | frontend/backend guard | create flow sends `rootType=channel`, normalized `rootRef`, and recomputes `chainId` from the current Root 引用 after validation failures |
| Join mutual exclusion | frontend filters candidates and backend rejects the same channel in inputChannels and outputChannel |
| Join visual vs semantic direction | visual upstream channel can be selected as output when it is not selected as this Join input, `inputChannels=b,d` / `outputChannel=a` is allowed without a real `a -> b/d` cycle, and the UI creates a right-side output reference card |
| Join cycle guard | direct self-cycle, output-to-input reachable cycle and traversal truncation are rejected with Chinese diagnostics; reachable cycles include a concrete path in the error |
| endpoint capability | backend rejects direct producer / consumer visual endpoints and only saves canonical `channel:<id>` refs |
| channel picker metadata | valid referenced metadata drafts are accepted; invalid, duplicate, or unreferenced channel metadata drafts are rejected |
| placed draft re-drag | only draft cards expose pointer drag; old nodes remain disabled; invalid drop restores the prior slot |
| dirty exit | dirty route exit opens custom discard dialog and preserves the draft |
| save success | successful save exits edit mode and refreshes the read-only viewer |
| mode-specific fields | Join ALL hides threshold; ANY_N / COUNT show threshold; Timer modes keep mode-specific fields |
| draft edge toggle | same candidate click removes the edge, duplicate edges are prevented, final save uses current edges |
| new channel / chain | draft channel endpoint saves canonical channel id; new root channel metadata appears in Logic Chain list |
| reference card direction | same-side primary/reference channel exists -> direct connect, no duplicate reference; no same-side reusable card -> local visual-only reference; save uses real channel id |
| slot policy | nearest valid slot visible; far empty slots hidden; Join slots follow upstream channel cards to their visible downstream column, use one middle slot for empty target columns, and expose one middle insert slot per occupied-column gap plus bottom append |
| output endpoint | Timer draft creates/selects downstream channel endpoint; Signal Join uses the same endpoint flow; failed endpoint connect does not leave orphan metadata |
| action output rendering | timer_start / timer_cancel visible as nodes; Timer bucket actions expand; listener signal action output appears to the right of the action and before consumers |
| virtual listener | canvas create is explicitly disabled with pure-config/no-world-entity reason until a safe listener-create edit lock path exists |
| frontend guard | markers for edit mode, slot, snap, connection capture delegation, large hit target, toast and no browser dialogs |
| no out of scope | no old node move/delete/reorder, no full editor, no Scratch editor, no if / else runtime |
