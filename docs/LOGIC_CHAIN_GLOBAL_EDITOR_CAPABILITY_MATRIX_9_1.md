# Logic Chain / Global Editor Capability Matrix 9.1

## Scope

9.1 extends the existing Logic Chain Viewer canvas as a controlled config editor. The editor saves typed resources, not a freeform graph document.

| Capability | Status | Notes |
| --- | --- | --- |
| Current Viewer canvas edit mode | Preserved | Reuses 8.14 / 8.16 edit toolbar, draft overlay, dirty exit and top-center toast. |
| Main Logic Chain editor lock | Preserved | `logic_chain_editor` lock is still required for draft save / validate / cancel. |
| Typed target locks | Preserved | Existing node and action edits still acquire the underlying config lock. |
| StateVariable definition create | Implemented | Supports GLOBAL / PLAYER scope and BOOLEAN / INTEGER / STRING type. |
| StateVariable definition edit | Implemented | Edits existing displayName, note, type/default/current value fields; stable identity rename is rejected. |
| StateVariable graph/reference entry | Implemented | State nodes and state_variable actions route to `#/state-variables/<id>`. StateVariable action-first visual semantics distinguish action cards from definition/target nodes. |
| ConditionGroup reference select / clear | Implemented | Gate fields use compatible condition group available lists and can clear to empty gate. |
| ConditionGroup editor entry | Implemented via existing page | Existing ConditionGroup page remains the create/edit authoring route. Inline create-and-bind from every picker is deferred. |
| Gate reference semantics | Implemented | Gate nodes and fields are guard references: allow / block / skip, not branch / else / fallback. |
| Virtual SignalListener canvas create | Implemented | Pure config node; channel is derived from legal canvas connection, no world entity required. |
| New node list labels | Selectable protected draft entries | UI list includes Channel Endpoint, Signal Join, Timer, Virtual SignalListener, World Device Reference, RegionController and VBD Virtual Block Device. Pure-config nodes are enabled. World Device Reference, RegionController and VBD entries are selectable and start client-assisted protected draft selection / placement flows with an online-player picker; backend saves require protected draft registry / client-assisted sessions and reject fake world or position payloads. Standalone ActionRelay add-node option is removed. |
| V8 client-assisted modal flow | Implemented | VBD, World Device Reference and RegionController use different selection purpose ids. The modal remains open during the game-side session, cancelled / failed sessions do not create graph cards, and successful selected / placed sessions require an explicit WebAdmin confirmation before adding the draft card. |
| World device hotbar placement | Implemented with manual game verification required | The server applies a protected three-slot hotbar for SignalEmitter / SignalReceiver / ActionRelay, routes right-click through a server use-block handler, blocks break/attack, suppresses the vanilla nine-slot hotbar in placement mode and restores inventory when the session ends. If WebAdmin cancels after placement but before creating the graph card, the backend cancels the protected draft and rolls back the placed block / SignalDeviceStore entry. Ordinary signal-device commands also refuse to mutate protected world-device draft blocks. |
| RegionController corner selection | Implemented with manual game verification required | RegionController uses a RegionPlanner-like point flow with distinct `logic_chain_region_controller_select` purpose and does not fall through to the VBD single-block handler. Client preview reuses the shared RegionPlanner particle / line renderer; the WebUI modal only shows status and confirmation. |
| SignalListener create fields | Implemented with metadata caveat | technical name, enabled, channel, cooldownTicks and conditionGroupId persist; display label / note are draft or audit-only until a real SignalListener metadata field exists. |
| ActionRelay same-index action edit | Implemented | Replace / disable existing action at the same index; action count and order preserved. The canvas summary card opens existing action rows that route into the same action edit modal. ActionRelay loaded exact object reads only an already loaded block entity and never force-loads chunks. |
| Region enter/exit/stay same-index action edit | Implemented | Replace / disable existing action in the selected bucket and index. Region signal-action alias cards carry owner / bucket / index metadata for graph reachability and render as Region owner -> action -> downstream channel. |
| Existing ActionConfig type coverage | Implemented | Existing types only: command, message, sound, signal, state_variable, timer_start, timer_cancel. |
| Action-level conditionGroupId | Implemented | Existing action editor can choose / clear compatible action gate references. |
| Channel endpoint / metadata consistency | Preserved | Channel endpoint is metadata/reference, not a runtime consumer. |
| Typed-owned node delete | Implemented as draft-only | Signal Join, Timer, SignalListener, VBD unbind, and physical SignalEmitter / SignalReceiver / ActionRelay device deletes can be submitted through `nodeDeletes` with confirmation, typed lock and expectedFingerprint. VBD unbind keeps the original world block; physical device delete removes the world device block after explicit warning. Reference nodes are rejected. |
| Action delete | Implemented as draft-only | SignalListener, Timer bucket, ActionRelay and RegionController action delete uses owner / bucket / index plus typed lock and expectedFingerprint. |
| Action reorder | Implemented as draft-only | Same-bucket reorder for SignalListener, Timer bucket, ActionRelay and RegionController; cross-source / cross-bucket movement remains rejected. |
| Existing action list-level maintenance | Implemented | Existing-node maintenance shows edit / disable / delete / up / down controls directly in the action list. The single-action editor only edits content fields. |
| Draft-created action panels | Implemented | Newly created but unsaved virtual SignalListener, ActionRelay world-device reference and RegionController drafts can add / edit / delete / reorder draft actions before save. ActionRelay draft action cards render only after a `channel -> ActionRelay` upstream connection exists, then appear as downstream owner action cards. RegionController requires enter / exit / stay buckets; owner-type action options are filtered by the owning node type. |
| Draft and saved semantic layout | Implemented | Producer drafts and saved producers anchor to the left-adjacent column of the downstream channel. RegionController owner cards follow their action group, and adjusted same-column cards are center-balanced to reduce gaps and flying edges. |
| Multiple draft operations | Implemented with conflict validation | A single edit session may accumulate multiple creates, edits, deletes, reorders and connection changes. Save validates all operations together; same action owner/bucket multi-write conflicts return `logic_chain_action_target_multi_write_conflict`. |
| Target lock preflight | Implemented | Target resource locks are acquired or checked when draft operations are created and prevalidated before save with `logic_chain_target_lock_preflight_validation`. |
| Node clickability / deferred detail | Implemented | Visible cards provide edit, readonly detail, primary-node locate, StateVariable jump or world-backed deferred reason. |
| Snapshot / audit / realtime coverage | Implemented | New StateVariable writes and expanded action writes use WebAdmin write foundation and snapshot hooks. |
| Help / Doctor / docs | Updated | Help catalog and docs describe 9.1 controlled editor completion and deferred boundaries. |
| Stabilization guard | Updated | 9.1 markers and negative guards cover scope and accepted interactions. |

## Node Capability

| Node Type | Editable in 9.1 | Fields / Operation | Save Target |
| --- | --- | --- | --- |
| `channel` / `downstream_channel` | Yes | displayName, note, iconKey | Channel metadata |
| `signal_join` | Yes | displayName, note, enabled, mode, threshold, scope, reset, timeout, cooldown, inputs, output | Signal Join config |
| `timer` | Yes | displayName, note, enabled, mode-specific timing, scope, startPolicy, outputChannel | Timer config |
| `signal_listener` | Yes | create from canvas; existing enabled, channel, cooldownTicks, conditionGroupId | SignalListener config |
| `state_variable` | Yes | definition create/edit through state variable page and graph links | StateVariable store |
| `condition_gate` / `action_gate` | Reference edit | choose / clear / open compatible ConditionGroup; no branch semantics | Owning typed config |
| `action` owned by listener | Yes | same-index replace / disable | SignalListener actions |
| `timer_action` owned by timer | Yes | same-index replace / disable | Timer bucket |
| `action` owned by ActionRelay | Yes | same-index replace / disable | ActionRelay actions |
| `action` owned by RegionController | Yes | same-index replace / disable in enter / exit / stay | RegionController config |
| Existing VBD node | Yes | Opens an in-place trigger-card editor. The editable channel field is removed from the node panel; output is graph-owned. Each native trigger card opens a second-level in-node config page with modal-local page stack and per trigger/page scroll restoration for enabled / disabled state, channel, cooldown / interval and compatible condition-group fields. Native trigger unsaved summaries are Chinese readable rows instead of primary raw `nativeTriggerJson`. Trigger output-channel draft changes immediately render VBD -> Channel draft edges and target-channel-adjacent placement before final save. Editing an existing trigger uses stable `(VBD, triggerKey)` identity, updates the clicked source trigger representation and removes stale main / focus / old-channel output edges so the selected trigger connects only to its configured target channel. itemSubmit requirements live under the right-click trigger page and container requirements live under the container-change trigger page; both capture entries reuse the standalone VBD game-assisted template sessions in `logicChainDraftOnly` mode. Saved realtime/status payloads write captured requirements back into the current Logic Chain draft, cancelled / failed sessions can restart from the modal when the context is still valid, retry clears stale session / lock state before starting a fresh game GUI, and all formal VBD writes happen only on final Logic Chain save. | VBD store path plus native-trigger typed lock through Logic Chain save |
| Saved SignalEmitter world device | Yes | displayName, note, icon and enabled edit in Logic Chain. The output channel field is no longer editable in the node panel because graph edges own channel relations. Physical presence is preflighted before basic or metadata save; missing/broken devices fail closed. | Device basic config / metadata |
| Saved SignalReceiver world device | Yes | Consumer/sink card, laid out to the right of a channel with `channel -> SignalReceiver`; safe fields are editable, but the input channel is graph-owned and shown as readonly connection summary. | Device basic config / metadata |
| Saved ActionRelay world device | Yes | Consumer/executor card, laid out to the right of a channel with `channel -> ActionRelay`; safe fields plus one-entry Action Panel maintenance. Downstream actions are rendered from the upstream channel connection, not from producer-side slot placement. The input channel is graph-owned and shown as readonly connection summary. | Device basic config / metadata / ActionRelay actions |
| RegionController saved node | Yes | One Action Panel entry opens enter / exit / stay second-level action sections; deleting the controller does not delete the Region. | RegionController config |
| World Device Reference | Implemented protected draft commit adapter | SignalEmitter / SignalReceiver / ActionRelay require client-assisted protected draft placement through the protected three-slot hotbar; fake world/pos payloads are rejected. The entry is selectable for testing and reports missing online player/session/permission problems at start. Save checks the loaded block, block entity and SignalDeviceStore identity before committing, and cancel / failure rolls back the placed draft block. | SignalDeviceStore / world device config via protected typed commit |
| VBD virtual block device | Implemented protected draft backend path | Reuses VBD selection without prefilled channel; output channel is committed from `vbd_outputs_channel` graph connection after protected draft validation. itemSubmit/container draft payloads are validated and wired into the VBD protected-draft commit path. Browser/game-side selection still needs manual verification. | VBD config when protected draft is valid |
| RegionController create | Implemented protected draft commit adapter | Region selection protected draft carries separate Region + RegionController naming fields and uses RegionPlanner-like corner selection. Fake coordinate entry is rejected. The entry is selectable for testing and reports missing online player/session/permission problems at start. Save creates real Region + RegionController and rolls both back on failure. | RegionStore + RegionControllerStore via protected typed commit |

## Guard Markers

Required 9.1 markers:

- `data-logic-chain-global-editor-completion-9-1`
- `data-logic-chain-action-relay-same-index-edit`
- `data-logic-chain-region-action-same-index-edit`
- `data-logic-chain-state-variable-definition-edit`
- `data-logic-chain-condition-group-reference-edit`
- `data-logic-chain-gate-reference-not-branch`
- `data-logic-chain-virtual-listener-create`
- `data-logic-chain-action-reference-editable`
- `data-logic-chain-action-relay-same-index-edit-reachable`
- `data-logic-chain-action-relay-existing-action-entry`
- `data-logic-chain-action-relay-unloaded-deferred-detail`
- `data-logic-chain-action-delete-draft`
- `data-logic-chain-action-reorder-draft`
- `data-logic-chain-node-delete-draft`
- `data-logic-chain-existing-node-delete-typed-owned-only`
- `data-logic-chain-action-relay-loaded-exact-object`
- `data-logic-chain-action-relay-actions-readable-when-loaded`
- `data-logic-chain-action-relay-no-force-load`
- `data-logic-chain-region-action-owned-node`
- `data-logic-chain-region-action-owned-alias`
- `data-logic-chain-region-action-same-index-edit-reachable`
- `data-logic-chain-region-action-owner-edge`
- `data-region-controller-no-primary-signal-channel-field`
- `data-logic-chain-state-action-action-first`
- `data-logic-chain-state-action-state-target-accent`
- `data-logic-chain-state-action-not-definition`
- `data-logic-chain-readonly-node-detail`
- `data-logic-chain-readonly-node-selects-detail`
- `data-logic-chain-state-variable-resource-jump`
- `data-logic-chain-world-entity-deferred-detail`
- `data-logic-chain-add-node-world-device-reference`
- `data-logic-chain-add-node-region-controller-selection`
- `data-logic-chain-add-node-vbd-draft-selection`
- `data-logic-chain-add-node-action-relay-standalone-removed`
- `data-logic-chain-world-backed-objects-require-client-assisted-draft`
- `data-logic-chain-protected-draft-registry-required`
- `data-logic-chain-add-node-world-device-selectable`
- `data-logic-chain-add-node-region-controller-selectable`
- `data-logic-chain-add-node-vbd-selectable`
- `data-logic-chain-modal-persistent-client-assisted`
- `data-logic-chain-cancelled-failed-no-draft-card`
- `data-logic-chain-online-player-picker`
- `data-logic-chain-client-assisted-online-player-required`
- `data-logic-chain-selection-success-create-card-only`
- `data-logic-chain-world-device-hotbar-protection`
- `data-logic-chain-webui-cancel-confirm-modal`
- `data-logic-chain-webui-cancel-requires-confirmation`
- `data-logic-chain-region-controller-separate-region-name`
- `data-logic-chain-region-controller-name-boundary`
- `data-logic-chain-saved-producer-target-channel-adjacent`
- `data-logic-chain-draft-saved-target-channel-adjacent`
- `data-logic-chain-producer-target-channel-adjacent`
- `data-logic-chain-draft-node-action-panel`
- `data-logic-chain-draft-action-add-delete-reorder-detail`
- `data-logic-chain-owner-type-action-filtering`
- `data-logic-chain-saved-world-device-edit-panel`
- `data-logic-chain-vbd-in-place-editor`
- `data-logic-chain-physical-device-missing-refresh`
- `data-logic-chain-signal-receiver-consumer-sink`
- `data-logic-chain-action-relay-consumer-executor`
- `data-logic-chain-world-device-consumer-right-of-channel`
- `data-logic-chain-world-device-consumer-non-source-style`
- `data-logic-chain-action-relay-upstream-render-precondition`
- `data-logic-chain-action-relay-upstream-required`
- `data-logic-chain-single-action-panel-entry`
- `data-logic-chain-action-panel-second-page`
- `data-logic-chain-signal-action-channel-combobox`
- `data-logic-chain-node-delete-confirm-phrase`
- `logic_chain_node_delete_confirm_phrase_required`
- `logic_chain_node_delete_single_write_fail_closed`
- `logic_chain_reference_node_delete_rejected`
- `data-logic-chain-vbd-delete-keeps-world-block`
- `data-logic-chain-physical-device-delete-removes-world-block-warning`
- `data-logic-chain-existing-vbd-item-submit-container-draft-only`
- `data-logic-chain-graph-owned-channel-summary`
- `data-logic-chain-no-editable-device-channel-field`
- `data-logic-chain-vbd-trigger-card-editor`
- `data-logic-chain-vbd-trigger-second-page`
- `data-logic-chain-vbd-itemsubmit-under-right-click-trigger`
- `data-logic-chain-vbd-container-under-container-change-trigger`
- `data-logic-chain-vbd-no-standalone-detail-navigation`
- `data-logic-chain-vbd-trigger-in-place-config`
- `data-logic-chain-vbd-trigger-enabled-draft`
- `data-logic-chain-vbd-native-trigger-draft-payload`
- `data-logic-chain-vbd-trigger-scroll-preservation`
- `data-logic-chain-vbd-trigger-local-page-stack`
- `data-logic-chain-vbd-itemsubmit-in-place-capture-entry`
- `data-logic-chain-vbd-container-in-place-capture-entry`
- `data-logic-chain-vbd-trigger-readable-draft-summary`
- `data-logic-chain-vbd-native-json-not-primary-summary`
- `data-logic-chain-vbd-trigger-channel-draft-edge`
- `data-logic-chain-vbd-trigger-graph-render-before-save`
- `data-logic-chain-vbd-capture-modal-captured-state`
- `data-logic-chain-vbd-capture-modal-applied-state`
- `data-logic-chain-vbd-capture-button-state`
- `data-logic-chain-vbd-itemsubmit-capture-button-state`
- `data-logic-chain-vbd-container-capture-button-state`
- `data-logic-chain-vbd-trigger-stable-identity`
- `data-logic-chain-vbd-trigger-no-duplicate-card`
- `data-logic-chain-vbd-trigger-target-channel-only`
- `data-logic-chain-vbd-trigger-source-card-draft`
- `data-logic-chain-vbd-capture-cancelled-restart-button`
- `data-logic-chain-vbd-capture-failed-restart-button`
- `data-logic-chain-vbd-itemsubmit-draft-writeback`
- `data-logic-chain-vbd-container-draft-writeback`
- `data-logic-chain-vbd-capture-realtime-draft-writeback`
- `data-logic-chain-vbd-capture-fail-closed-writeback`
- `logicChainVbdTemplateSessionContextPayload`
- `logicChainDraftOnly`
- `dataLogicChainVbdItemSubmitCaptureSessionPurpose`
- `dataLogicChainVbdContainerCaptureSessionPurpose`
- `logic_chain_world_device_input_channel_required`
- `data-logic-chain-card-click-exits-connection-mode`
- `data-logic-chain-connect-success-clears-connection-mode`
- `data-logic-chain-draft-action-graph-render`
- `data-logic-chain-draft-action-non-signal-card`
- `data-logic-chain-draft-nested-action-diff`
- `data-logic-chain-draft-action-pending-delete-diff`
- `data-logic-chain-draft-diff-fail-soft`
- `data-logic-chain-no-inline-js-syntax-break`
- `data-logic-chain-action-delete-from-right-panel`
- `data-logic-chain-pending-delete-grey`
- `.logic-chain-edge-layer{pointer-events:none}`
- `.logic-chain-draft-handles{pointer-events:none}`
- `.logic-chain-minimap{pointer-events:none}`
- `dataLogicChainWorldDeviceHudNoTargetText`
- `dataLogicChainWorldDeviceSelectedSlotSync`
- `logic_chain_world_device_type_mismatch`
- `authoritativeWorldDeviceDraftType`
- `dataLogicChainDraftWorldDeviceMetadataVisible`
- `dataLogicChainSharedSemanticPlacementResolver`
- `regionControllerOwnerFollowsActionGroup`
- `regionControllerDraftOwnerFollowsActionGroup`
- `sameColumnCenterBalanced`
- `shouldBlockProtectedDraftCommandMutation`
- `dataLogicChainWorldBackedCommitRollbackAdapter`
- `logic_chain_region_controller_requires_action_bucket`
- `data-logic-chain-region-controller-no-vbd-handler`
- `logic_chain_region_controller_select`
- `data-logic-chain-vbd-editor-not-globally-deferred`
- `data-logic-chain-vbd-item-submit-entry`
- `data-logic-chain-vbd-container-entry`
- `data-logic-chain-action-list-level-delete`
- `data-logic-chain-action-list-level-reorder`
- `data-logic-chain-multiple-draft-operations-allowed`
- `data-logic-chain-single-delete-reorder-limitation-removed`
- `logic_chain_action_target_multi_write_conflict`
- `dataLogicChainVbdItemSubmitContainerCommitWired`
- `logic_chain_target_lock_preflight_validation`
- `data-logic-chain-new-node-cancel-no-dirty-confirm-when-unchanged`
- `data-logic-chain-placed-draft-cancel-discards`
- `data-logic-chain-placed-draft-update-only-on-confirm`
- `data-logic-chain-save-prevalidated-sequential`
- `data-state-variable-save-lock-guard`
- `data-logic-chain-no-freeform-graph-save`

Negative guard expectations:

- no GameController, MissionSystem or PhaseController implementation;
- no if / else runtime, branch node, else path or fallback action;
- no full Scratch editor or full Logic Chain Editor;
- no freeform graph document save;
- no new ActionType or ConditionNodeType;
- no old node arbitrary move / reorder and no reference-node direct delete;
- no old action arbitrary cross-bucket move;
- no freeform world mutation or automatic world entity creation outside protected client-assisted world-device placement.
- no standalone `action_relay` add-node option; ActionRelay belongs under World Device Reference.
- no `world_device`, `virtual_block_device` or `region_controller` fake draft save without protected draft registry.

## Deferred

| Candidate | Reason |
| --- | --- |
| Inline ConditionGroup create-and-bind from every gate picker | Existing ConditionGroup page already creates/edits groups; picker-level create callback can be a later UX slice. |
| Template apply for StateVariable / ConditionGroup definitions | Template package application still blocks those resources to avoid partial cross-store writes. |
| Typed actions / Rich Text Builder | 9.2+ scope. |
| Game Program AST / GameController / MissionSystem / PhaseController | Later 9.x runtime model. |
| If / else branching runtime | Later 9.x control-flow model. |
| Freeform/general world placement outside protected flow | Requires separate safety, preview and rollback design; 9.1 only permits the protected client-assisted World Device Reference placement path. |
| Full manual UX verification for game-side placement/selection | Server/client wiring and commit / rollback adapters exist, but a real Minecraft client pass is still required for pointer feel, inventory restoration, draft block cleanup/protection, safe-area HUD and region corner ergonomics. |
| Logic Chain VBD itemSubmit/container capture UX polish | Logic Chain VBD trigger pages now enter the mature standalone VBD game-assisted template sessions with draft-only context and return captured requirements to the Logic Chain modal; manual browser/game verification is still required. |
