# 8.16 Logic Chain Editor Existing Node Editing Current Context

## Stage Goal

8.16 extends the 8.14 controlled Logic Chain Editor MVP from new-node-only editing to limited maintenance of existing nodes. Editing still happens inside the current Logic Chain Viewer canvas; there is no new editor route, no full Logic Chain Editor, no Scratch editor and no raw JSON editor.

This stage implements:

- existing node controlled editing
- Channel metadata edit
- Signal Join edit
- Timer edit
- SignalListener basic config edit
- existing ActionConfig same-index edit
- same-index Action replace / disable when the existing ActionConfig model can express `enabled=false`
- canvas green-plus reconnect saved through typed fields
- draft overlay preview and compact draft diff banner
- Channel Endpoint as an add-node draft card
- multi-operation draft session
- save validation
- save writes underlying config

## Supported Existing Nodes

| Existing node | Status | Save target |
| --- | --- | --- |
| Channel metadata | Implemented | `WebAdminChannelMetadataService.update` |
| Signal Join | Implemented | `WebAdminSignalJoinService.update` writes `SignalJoinDefinition` |
| Timer | Implemented | `WebAdminTimerService.updateBasicConfigPreservingActions` writes `TimerDefinition` without mutating old action buckets |
| SignalListener / virtual listener basic config | Implemented | `WebAdminSignalListenerBasicConfigService.update` |
| SignalListener ActionConfig | Implemented | `WebAdminSignalListenerActionsService.updateAction` |
| Timer bucket ActionConfig | Implemented | `WebAdminTimerService.updateActionInBucket` |

## Local Reconnect

8.16 does not save fake graph edges for existing nodes. Local reconnect is represented as typed config field changes:

- Signal Join `inputChannels`
- Signal Join `outputChannel`
- Timer `outputChannel`
- SignalListener `channel`
- Signal action `value` / output channel

The payload uses `existingNodeEdits` or `actionEdits`; these modes reject standalone `edges`. When a new draft node is also present, `edges` still belong only to that new node. This keeps the graph viewer derived from real config stores.

Existing connectable cards now use the same canvas green-plus reconnect affordance as new draft cards. The existing-node modal keeps readonly connection summaries and hidden typed values instead of modal-local channel pickers; Join input/output, Timer outputChannel, Listener channel and signal Action output are changed from the card handles on the canvas and still save only as typed config fields. Once a green-plus connection mode is active, canvas pan / blank click keeps the mode active; the active source card keeps its own green point visible so the same point can exit the mode, while other old-node edit points are hidden and only legal channel candidates show candidate green points. Draft field changes are overlaid onto the canvas immediately, but saving still writes only after validation. Opening an existing-node/action modal is not itself a saveable draft; the toolbar save button stays disabled until the user changes fields and explicitly joins the draft.

## Unified Draft Session

8.16 now treats the editor as one draft session. The same session may contain a new Signal Join / Timer, Channel Endpoint draft cards, an Action append, multiple existing node edits and multiple Action edits. The backend validates the whole payload first, then writes in a fixed order: new node, Action append, existing node edits, Action edits and referenced channel metadata drafts.

Channel Endpoint is available from `新增节点`. It creates a draft channel card and optional channel metadata draft. Selecting an existing channel reuses the existing visible channel card when possible; it does not create a duplicate endpoint unless the channel is new or the user changes display metadata. New channel metadata is accepted only when referenced by a draft edge, typed reconnect field or Signal action output.

Draft graph behavior remains visual-only: draft edges and their arrows use the same green highlight, draft Join / Timer cards are selected by single click, and moving them requires hold-then-drag without pointer-capture snapback during rerender. Existing-node reconnect overlays only the changed connection as a green dashed draft edge; unchanged saved edges keep their normal style. Removed or replaced original edges stay as layout-only edges while connection mode is still active, so the already-cancelled line is hidden immediately but disconnected downstream cards do not vanish until the user exits connection mode. After exit, the overlay graph is pruned from the current root so fully detached cards and the layout changes are applied. Draft node modals expose full creation fields such as note / enabled and their mode-dependent fields change in place, for example Join ALL hides threshold while ANY_N / COUNT show it. Unconnected draft Channel Endpoint cards default under the current focus/root channel card so they do not jump into the Join column or the currently selected linked channel. When a new channel endpoint is connected as a Join / Timer downstream output and that endpoint has no other graph connection, the draft channel card is compacted to the output side adjacent to the draft node instead of leaving a forced empty C3 gap. When a Timer output is connected to an already visible channel, the Timer draft moves to the channel card's left side and draws directly to that card instead of creating a separate output reference card. Selecting a new Join / Timer draft keeps the right detail panel on that draft node and exposes draft config editing instead of falling back to the linked channel.

## Action Maintenance

Existing ActionConfig editing is intentionally narrow:

- same-index replace is supported for SignalListener actions and Timer `start` / `tick` / `complete` / `cancel` buckets.
- disable is represented by the existing `enabled=false` field when present in ActionConfig.
- action conditionGroupId remains part of the ActionConfig payload.
- state_variable and timer_start / timer_cancel structured fields use the existing action editors and payload mappers.

8.16 does not allow old action delete, old action reorder, arbitrary action move, cross-list action move or new ActionType creation.

## Draft Diff And Locks

The canvas toolbar and edit modal show draft diff markers. The canvas toolbar uses a compact fixed-height banner that shows the latest change, total count and an expand/collapse list:

- `data-logic-chain-draft-diff`
- `data-logic-chain-draft-diff-compact-banner`
- `data-logic-chain-draft-diff-latest-only`
- `data-logic-chain-draft-diff-change-count`
- `data-logic-chain-draft-diff-expand-all`
- `data-logic-chain-draft-diff-collapse`
- `data-logic-chain-diff-field-change`
- `data-logic-chain-diff-connection-change`
- `data-logic-chain-diff-action-change`

Saving existing config requires:

- main `logic_chain_editor` edit lock
- typed target edit lock
- expectedFingerprint
- permission
- CSRF / same-origin
- backend validation
- audit
- realtime

Save failure keeps the Logic Chain editor lock and draft unless the returned result explicitly marks the main editor lock as lost.

## Deferred

The following remain deferred:

- old node move
- old node delete
- old node reorder
- old action delete
- old action reorder
- VBD / SignalReceiver / ActionRelay block / Region in-editor binding
- direct world entity coordinate editing
- full Logic Chain Editor
- Scratch editor
- if / else runtime
- GameController
- MissionSystem
- PhaseController
- new ActionType
- new ConditionNodeType
- runtime semantic changes
- rollback / Git-like branch / merge

World entity nodes remain readonly reference cards in the Logic Chain canvas and are marked with `data-logic-chain-world-entity-readonly-reference`.

## Guard Markers

8.16 frontend / docs / tests intentionally include:

- `data-logic-chain-existing-node-editing`
- `data-logic-chain-edit-existing-node`
- `data-logic-chain-existing-node-edit-modal`
- `data-logic-chain-draft-diff`
- `data-logic-chain-draft-overlay`
- `data-logic-chain-rendered-graph-overlay`
- `data-logic-chain-draft-diff-compact-banner`
- `data-logic-chain-channel-endpoint-add-node-type`
- `data-logic-chain-channel-endpoint-draft-card`
- `data-logic-chain-channel-endpoint-single-card`
- `data-logic-chain-channel-endpoint-no-duplicate-card`
- `data-logic-chain-draft-channel-candidate-connectable`
- `data-logic-chain-draft-channel-no-own-connect-mode`
- `data-logic-chain-draft-channel-card-not-pruned`
- `data-logic-chain-draft-edge-green-arrow`
- `data-logic-chain-draft-click-selects`
- `data-logic-chain-draft-long-press-drag`
- `data-logic-chain-draft-drag-no-capture-snapback`
- `data-logic-chain-draft-node-detail-selectable`
- `data-logic-chain-draft-detail-selects`
- `data-logic-chain-draft-modal-full-config-fields`
- `data-logic-chain-draft-modal-mode-fields`
- `data-logic-chain-draft-channel-default-under-focus-channel`
- `data-logic-chain-draft-channel-direct-downstream-of-join`
- `data-logic-chain-draft-channel-adjacent-to-join-output`
- `data-logic-chain-no-forced-draft-output-c3-gap`
- `data-logic-chain-multi-draft-session`
- `data-logic-chain-diff-field-change`
- `data-logic-chain-diff-connection-change`
- `data-logic-chain-diff-action-change`
- `data-logic-chain-local-reconnect`
- `data-logic-chain-reconnect-cancel`
- `data-logic-chain-existing-canvas-reconnect`
- `data-logic-chain-existing-reconnect-no-modal-fields`
- `data-logic-chain-existing-reconnect-picker`
- `data-logic-chain-green-plus-reconnect`
- `data-logic-chain-connection-mode-pan-keeps-active`
- `data-logic-chain-connection-exit-only-same-green-point`
- `data-logic-chain-connection-target-keeps-own-handles`
- `data-logic-chain-connection-prune-deferred-until-exit`
- `data-logic-chain-existing-reconnect-any-legal-channel`
- `data-logic-chain-new-node-connection-hides-old-edit-handles`
- `data-logic-chain-new-node-connection-legal-candidates`
- `data-logic-chain-only-changed-edge-draft-highlight`
- `data-logic-chain-unchanged-existing-edge-keeps-style`
- `data-logic-chain-removed-edge-hidden-during-connection`
- `data-logic-chain-prune-detached-after-connection-exit`
- `data-logic-chain-reconnect-reference-card`
- `data-logic-chain-timer-output-move-left-of-channel`
- `data-logic-chain-timer-output-no-reference-card`
- `data-logic-chain-action-append-in-existing-node-modal`
- `data-logic-chain-no-op-save-disabled`
- `data-logic-chain-existing-node-not-draggable`
- `data-logic-chain-action-edit`
- `data-logic-chain-action-replace-same-index`
- `data-logic-chain-no-old-action-delete`
- `data-logic-chain-no-old-action-reorder`
- `data-logic-chain-no-old-node-delete`
- `data-logic-chain-no-old-node-reorder`
- `data-logic-chain-world-entity-readonly-reference`

Negative guard: the Logic Chain editor section must not contain old node move/delete/reorder functions, old action delete/reorder functions, full editor, Scratch editor, if/else runtime or raw JSON editor markers.

## Validation Plan

Required validation:

- `.\gradlew.bat testClasses`
- JS export plus `node --check build\tmp\webadmin-app.js`
- `cd tools\tzz-test-mcp; npm run build; npm test`
- `.\gradlew.bat clean build`
- `.\gradlew.bat stabilizationGuardTest --rerun-tasks`
- `.\gradlew.bat localTestMcpGuardTest --rerun-tasks`
- `git diff --check`

Do not start Minecraft. Do not run MCP scenario. Do not generate screenshot matrix. Do not commit / push / merge / tag during implementation review.
