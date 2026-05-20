# Logic Chain Editor Capability Matrix 8.16

## Scope

8.16 keeps editing inside the current Viewer canvas. It adds controlled existing-node editing, same-index existing ActionConfig editing and canvas green-plus reconnect saved through typed config fields. It is not a full Logic Chain Editor.

| Capability | Status | Notes |
| --- | --- | --- |
| current Viewer canvas edit mode | Implemented | Reuses 8.14 edit toolbar, dirty exit and top-center toast |
| main Logic Chain editor lock | Implemented | `logic_chain_editor` lock remains required for save / validate / cancel |
| typed target locks | Implemented | Existing node and action edit acquire the underlying config lock |
| existing node editing | Implemented | `existingNodeEdits` payload; multiple different targets can be saved in one draft session |
| Channel metadata edit | Implemented | `displayName`, `note`, `iconKey`; channel id rename deferred |
| Signal Join edit | Implemented | fields plus `inputChannels` / `outputChannel` local reconnect |
| Timer edit | Implemented | fields plus `outputChannel` local reconnect; old timer actions preserved |
| SignalListener basic edit | Implemented | `enabled`, `channel`, `cooldownTicks`, `conditionGroupId` |
| existing ActionConfig edit | Implemented | `actionEdits` payload, same-index replace / disable |
| Timer action bucket edit | Implemented | `start`, `tick`, `complete`, `cancel` via `updateActionInBucket` |
| SignalListener action edit | Implemented | same-index replace via existing listener action service |
| draft overlay preview | Implemented | Existing node/action field changes render immediately on the canvas without writing runtime config; unchanged saved edges keep their normal style |
| compact draft diff | Implemented | Fixed-height latest-change banner with total count and expand/collapse |
| Channel Endpoint add-node | Implemented | `新增节点` can create draft channel endpoint cards and referenced metadata drafts; unconnected endpoints default under the focus/root channel card instead of the currently selected linked channel, while new downstream endpoints compact to the draft Join / Timer output side when they have no other connection |
| draft node config modal | Implemented | New Join / Timer modals expose full creation fields such as note / enabled and change mode-specific fields in place, including Join ALL hiding threshold |
| local reconnect | Implemented | Saved as typed fields, not fake graph edges; existing cards use canvas green-plus handles, connection mode survives panning, the active source keeps its own exit handle, and the existing-node modal shows readonly connection summaries instead of modal-local reconnect pickers |
| reconnect candidate scope | Implemented | Existing and new-node connection modes hide unrelated old edit handles and show legal channel candidates; removed/replaced edges are hidden immediately but kept layout-only until connection mode exits, then detached structure is pruned |
| Timer downstream placement | Implemented | Connecting a Timer output to an already visible channel moves the Timer draft to the channel card's left and draws directly to that card instead of generating an output reference card |
| action append entry | Implemented | The right detail sidebar no longer shows a standalone append card; the append entry is inside the single existing-node maintenance modal |
| save writes underlying config | Implemented | Uses existing typed services |
| action append | Preserved | 8.14 append-only behavior remains |
| old node drag/move | Deferred | Existing cards marked not draggable |
| old node delete/reorder | Deferred | Not exposed in Logic Chain editor |
| old action delete/reorder | Deferred | Guarded out of Logic Chain editor |
| world entity direct editing | Deferred | World entity nodes remain readonly references |
| full Logic Chain Editor | Deferred | Future stage |
| Scratch editor | Deferred | Future stage |
| if / else runtime | Deferred | Future stage |
| GameController / MissionSystem / PhaseController | Deferred | Future stage |

## Node Capability

| Node Type | Editable | Fields | Save Target |
| --- | --- | --- | --- |
| `channel` / `downstream_channel` | Yes | displayName, note, iconKey | Channel metadata |
| `signal_join` | Yes | displayName, note, enabled, mode, threshold, scope, reset, timeout, cooldown, inputs, output | Signal Join config |
| `timer` | Yes | displayName, note, enabled, mode-specific timing, scope, startPolicy, outputChannel | Timer config |
| `signal_listener` | Yes | enabled, channel, cooldownTicks, conditionGroupId | SignalListener basic config |
| `action` / `state_action` owned by listener | Yes | ActionConfig same index | SignalListener actions |
| `action` / `timer_action` owned by timer | Yes | ActionConfig same index | Timer action bucket |
| `action_relay` / `region_controller` old actions | Partial | append-only preserved; existing edit deferred | Existing 8.14 append paths |
| VBD / receiver / relay block / region world entity | No | readonly reference | Deferred |
| state variable direct node | No | readonly reference | Deferred |
| condition gate / action gate nodes | No | readonly reference | Deferred |

## Payload Modes

One save request can contain multiple draft operations:

- new draft node through `nodes` / `edges`
- action append through `actionAppend`
- existing node edits through `existingNodeEdits`
- existing ActionConfig edits through `actionEdits`
- referenced channel metadata through `channelMetadataDrafts`

Existing node / action edit modes reject standalone `edges` because local reconnect is saved to specific typed fields. Existing reconnect is initiated from the canvas green-plus handles on the saved card; modal connection fields are readonly summaries. If `nodes` is present, `edges` are treated as the new-node draft edges. Duplicate edits for the same existing node or same action index are rejected; different targets are validated together and saved sequentially.

Opening an existing node/action edit modal does not create a saveable draft by itself. The toolbar save action is enabled only after a changed modal is explicitly joined into the draft.

## Guard Markers

Required markers:

- `data-logic-chain-existing-node-editing`
- `data-logic-chain-edit-existing-node`
- `data-logic-chain-existing-node-edit-modal`
- `data-logic-chain-draft-diff`
- `data-logic-chain-draft-overlay`
- `data-logic-chain-rendered-graph-overlay`
- `data-logic-chain-draft-diff-compact-banner`
- `data-logic-chain-draft-diff-latest-only`
- `data-logic-chain-draft-diff-change-count`
- `data-logic-chain-draft-diff-expand-all`
- `data-logic-chain-draft-diff-collapse`
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

Negative guard:

- no `moveExistingLogicChainNode`
- no `deleteLogicChainNode`
- no `reorderLogicChainNode`
- no `deleteLogicChainAction`
- no `clearLogicChainActions`
- no `reorderLogicChainAction`
- no `FullLogicChainEditor`
- no `ScratchEditor`
- no `IfElseRuntime`
- no `RawJsonEditor`

Negative checks are scoped to the Logic Chain editor section because other WebAdmin pages may still have their own non-Logic-Chain delete / clear operations.

## Test Matrix

| Area | Coverage |
| --- | --- |
| existing node validation | duplicate same-target edits rejected; multiple different targets allowed with typed fingerprint / lock required |
| Channel metadata | payload shape, channel id rename not supported |
| Signal Join | input/output overlap rejected, cycle guard reused |
| Timer | existing config write path, outputChannel typed reconnect, old action list preserved |
| SignalListener | channel / cooldown / conditionGroupId payload |
| ActionConfig | same-index replace, disable via existing enabled field, conditionGroupId payload |
| Action list maintenance | append preserved; old delete/reorder rejected |
| security/write | permission, CSRF, same-origin, edit lock, expectedFingerprint, audit, realtime through typed services |
| frontend | modal markers, draft overlay, compact diff banner, Channel Endpoint add-node, canvas reconnect, Timer output placement, existing-node modal Action append, dirty exit, no browser dialogs |
| no out of scope | no full editor, Scratch, if/else runtime, GameController/MissionSystem/PhaseController, new ActionType or ConditionNodeType |
