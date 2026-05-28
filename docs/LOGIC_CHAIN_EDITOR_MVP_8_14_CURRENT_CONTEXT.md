# 8.14 Logic Chain Editor MVP Current Context

## Stage Goal

8.14 Logic Chain Editor MVP moves the 8.13 read-only runtime graph one step forward: editing happens in the current Viewer canvas, but the scope is deliberately narrow.

This stage implements:

- enter edit mode on the current Viewer page
- edit lock for the Logic Chain editor session
- stable lock target reuse for heartbeat / save / cancel
- new node type selection
- guided config for the supported pure config nodes
- new card placement into legal slot columns
- placed draft card re-drag before save
- snap to canonical slot coordinates
- green plus connection handles
- connection mode toggle by clicking the active plus again; Escape / canvas click exits selection mode
- draft edge toggle by clicking the same connected candidate again
- mode-specific Join fields: ALL hides threshold, ANY_N / COUNT show threshold
- real pointer drag with legal slot proximity and snap
- dark combobox channel endpoint picker for Join / Timer connections: choose existing channel or type a new channel id
- nearest-slot-only placement overlay: each draft node type shows only context-adjacent legal slots instead of scanning a whole empty column; Join slots are allowed only in a column whose immediate left column contains channel cards
- shared output endpoint flow for Signal Join and Timer: select an existing downstream channel or create a new draft channel endpoint, then save only the real channel id and only referenced metadata
- new logic chain entry from the Logic Chain list by creating root channel metadata
- draft channel reference cards that preserve left/right direction and stay near the current draft node when a same-side primary channel card cannot be used
- append one new ActionConfig to an existing SignalListener / ActionRelay / Region / Timer action list
- Timer action graph visibility for `timer_start` / `timer_cancel`, including Timer bucket actions expanded as graph nodes instead of only summary text
- signal action output channel placement as `action -> output channel -> consumer`, or `action -> terminal output channel` when no consumer exists
- same Signal Join cannot use the same channel as both input and output
- bounded Join cycle guard before save
- save validation
- 保存落地到现有配置
- save writes underlying config through existing services
- cancel / dirty exit without leaving the Logic Chain page
- top-center custom toast, auto dismiss, no browser alert / confirm / prompt

This stage does not implement a full Logic Chain Editor. It only supports new node placement, new draft connections, new channel metadata drafts, and append-only ActionConfig creation on existing action containers.

## Supported New Nodes

8.14 supports exactly two pure config node types:

| Node | Placement | Save Mapping | Notes |
| --- | --- | --- | --- |
| Signal Join | dynamic downstream-of-channel visual columns | `WebAdminSignalJoinService.create` writes a real `SignalJoinDefinition` | Modal collects ID / name / mode / scope / reset policy; ALL hides threshold, ANY_N / COUNT show threshold; inputChannels and outputChannel are derived from draft edges |
| Timer | C0 | `WebAdminTimerService.create` writes a real `TimerDefinition` | Modal collects ID / name / mode-specific timing / scope / start policy; outputChannel is derived from a downstream edge unless valid onCompleteActions already exist; C5 Timer reference / target placement remains deferred |
| Action append | Existing action list | Existing typed action services append one new `ActionConfig` | Supported owners: SignalListener actions, ActionRelay actions, Region enter / exit / stay actions, Timer onTick / onComplete actions. This is append-only: no old action move, delete, reorder or complex edit. |

The editor does not save a fake graph document. A saved draft becomes real existing config:

- Signal Join draft -> `signal_joins.json`
- Timer draft -> `timers.json`

The graph is refreshed after save so the new config or appended action summary is displayed by the normal read-only graph builder.

## Deferred Nodes

The following remain deferred:

- old node move deferred
- old node delete deferred
- old node reorder deferred
- old node complex edit deferred
- StateVariable direct create deferred
- Condition gate / Action gate editing deferred
- VBD / SignalReceiver / ActionRelay block / Region / physical world entity create deferred
- Virtual SignalListener canvas create deferred until the Logic Chain editor has a safe listener-create edit lock path. SignalListener is pure config and does not require a world entity, but this stage requires admins to create it on the SignalListener page first.
- old action move / delete / reorder / complex edit deferred
- full Logic Chain Editor deferred
- Scratch editor deferred
- if / else runtime deferred
- GameController deferred
- MissionSystem deferred
- PhaseController deferred
- 不做 MissionSystem
- 不做 PhaseController

World entity requirement: devices, receivers, ActionRelay blocks and regions must already exist in game or in their own WebAdmin pages. The Logic Chain editor can only reference existing world entities in later stages; 8.14 does not create world blocks or regions from the canvas.

Deferred future direction: VBD / Region / ActionRelay block can later get an in-game draft create flow where WebAdmin opens a temporary creation session, the admin clicks or selects the world object in game, and cancelling the editor rolls back that temporary world binding. This is the future "游戏内 pending selection + 草稿 + 取消回滚" model. 8.14 records that direction only and keeps the UI explicit that world entities must already exist before they can be referenced: 世界实体必须先存在.

## Edit Lock

The edit session uses `logic_chain_editor` as a dedicated edit lock target.

Flow:

1. User clicks "进入编辑模式".
2. Backend checks `EDIT_LOGIC_CHAIN` permission, CSRF, same-origin and edit lock.
3. Backend returns the current graph snapshot, `baseGraphFingerprint`, canonical `targetType` and canonical `targetId`.
4. UI displays lock status in the canvas toolbar.
5. The edit session stores that canonical lock target and reuses it for heartbeat, validation, save and cancel.
6. Save validates the same lock target, same lock id and same base graph fingerprint.
7. Successful save releases the editor lock after the underlying config write completes.

Lock conflict blocks edit mode. The conflict is shown in the editor toolbar and by the top-center toast; edit controls remain disabled until the user can enter a real edit session.
Validation or fingerprint failures keep the editor lock and the draft so the user can fix and retry with the same lock target. If the lock is missing or expires, the toolbar marks the lock as lost and disables save while preserving the draft.
Save failures from underlying typed writes, such as a temporary Signal Join / Timer / Action container lock conflict, also preserve the main Logic Chain editor lock and draft. Only failures explicitly marked as the main editor lock being lost can clear the editor lock on the frontend; typed lock failures remain visible as structured errors and can be retried after the target config lock is available.
When the editor saves a new Signal Join or Timer, the temporary typed edit lock is acquired against the same canonical ID that the underlying typed service will persist and validate. User-entered IDs with spaces or uppercase letters are normalized before the typed lock is acquired, so the editor does not create a raw-ID lock and then fail the typed save with a false `edit_lock_expired`. IDs that normalize to blank fail draft validation before any temporary typed lock is acquired.

## Placement And Slot Rules

Only the newly created draft card can be dragged or placed. After it has snapped to a slot, it can be picked up again before save and moved to another legal slot; existing graph nodes remain fixed and read-only.

Legal columns and slots:

- Signal Join: legal columns are derived from visible upstream channel cards instead of fixed C2 / C3 processing columns. A column can host a Join slot when its immediate upstream visual column contains channel / downstream channel / channel reference / draft channel endpoint cards. If the target column is empty, only one context-middle slot is shown from the upstream channel y band. If the target column already has listener / action / consumer cards, the editor exposes one middle insert slot per existing-card gap plus a bottom append slot. Join draft mode no longer shifts later lanes right or creates a forced empty processing column; placement is visual only, while inputChannels / outputChannel semantics still come from draft edges.
- Timer: C0 source column, with the same nearest-slot-only overlay.
- C5 Timer 引用 / 目标位需要 action-list 映射，8.14 已 deferred

The canvas shows legal slot outlines with a white dashed glow. The draft card starts unplaced, uses pointer drag as the primary placement path, follows the cursor while dragging, only previews a legal slot when the pointer is within proximity, and snaps on pointerup. Clicking a slot remains a fallback. Slot outlines render below cards so an already placed draft card can be picked up again with pointerdown / pointermove / pointerup rather than only by clicking another slot. Slot calculation uses current columns / rows and same-column make-room so the draft card and temporary channel endpoints do not overlap existing nodes. Existing nodes remain read-only; they can visually make room for the draft but are not draggable, deletable or reorderable by this MVP.
The slot overlay deliberately hides far empty slots below the current chain. Channel reference cards and append-only Action drafts also use nearest free slots around their owner / draft anchor.

## Connection Rules

After placement, the draft card exposes green plus handles:

- Signal Join: upstream and downstream handles.
- Timer: downstream handle.

Draft connections are highlighted and remain highlighted until save or cancel. Clicking the active green plus again exits that connection mode; pressing Escape or clicking blank canvas also exits connection selection without deleting existing draft edges.
Clicking the same connected candidate again removes that draft edge. Downstream output edges are single-owner and replacing a different downstream candidate updates the final draft edge set.
The draft channel endpoint modal validates the pending edge before storing metadata, prunes metadata when an edge is cancelled or replaced, and the backend rejects any metadata draft whose channel is not referenced by the current draft edge set or Signal action append. In action append mode, draft edges are rejected outright and metadata references can only come from a Signal action value, so forged node edges cannot persist orphan endpoint metadata. Channel metadata is attempted only after the typed Join / Timer / Action write succeeds, so a typed write failure cannot persist orphan endpoint metadata.

Allowed edge types:

- `join_input`
- `join_output`
- `timer_outputs_channel`

Backend validation checks draft node type, placement column, slot, required intrinsic config, required upstream/downstream edge, duplicate edge, edge type per node, edge endpoint direction and graph fingerprint. Topology is authoritative from draft edges: Signal Join requires at least 2 unique upstream inputs and exactly 1 downstream output; the same Join rejects any channel that appears in both `inputChannels` and `outputChannel`; Timer requires exactly 1 downstream output unless valid onCompleteActions are already present. The frontend markers are only UX hints; backend validation is authoritative.

Join candidate filtering is semantic, not visual-lane based. A channel shown in the upstream area can still be chosen as the current Join output if it is not already selected as this Join input; in that case the canvas creates a right-side output reference card near the draft Join and still saves the canonical channel id. The inverse rule applies to input selection for a channel visually downstream but not selected as output.

Green plus handles use a small visual dot with a larger hit target and event delegation with `data-*` attributes; complex connection data is not embedded in inline JavaScript. Connected candidates use a distinct state and `aria-pressed`.

When a draft edge connects to an existing channel, the canvas first scans for a suitable same-side primary or reference channel card and connects directly to that card. It creates a draft-only channel reference card on the required side of the new node only when no same-side reusable card exists. The reference card is placed by nearest free slot around the draft node, not by scanning from the top of the column. The saved payload still uses the canonical `channel:<id>` endpoint; reference cards are visual only, non-traversal, and do not expand downstream.

Before save, Signal Join output selection also runs a bounded traversal over the current graph. Direct input/output self-cycles are rejected, and any reachable path from the selected output channel back to the selected input channels is rejected with a Chinese diagnostic that includes the concrete path. Visual upstream placement alone is not a cycle: `inputChannels = b, d` and `outputChannel = a` is allowed unless the current graph actually contains a path from `a` back to `b` or `d`. The traversal has node and edge limits; hitting the guard limit rejects the save instead of recursing indefinitely.

The editor also supports a draft channel endpoint from connection mode. The endpoint UI is a dark searchable combobox that lists existing channel display names and ids, accepts a new channel id, stores referenced channel display metadata through WebAdmin channel metadata drafts, and saves only the real channel id through the Signal Join / Timer configuration, not a fake graph document.

Signal action output rendering is kept in the read-only graph model, not as a fake editable node. A listener signal action remains visible as its real action node, then connects to a right-side downstream channel card. If that channel has consumers, the primary output channel and consumer are shown after the action; if it has no consumers, the downstream channel card is terminal and remains to the right of the action instead of creating a far-left producer alias.

Timer actions are no longer only text in the Timer summary. `timer_start` and `timer_cancel` actions from SignalListener / ActionRelay / Region / Timer buckets render as timer action nodes with Timer id, target mode / target id, condition group id when present, and an edge to the target Timer or missing-Timer reference. Timer `onStart` / `onTick` / `onComplete` / `onCancel` buckets are expanded into action nodes when the Timer appears in the graph.

Action append is available from existing action container nodes. It acquires the target container edit lock, reuses existing typed action validation and condition gate binding, shows the appended action as a draft node after the owner in the same right-side action lane that the saved graph uses, and saves through the owner service. The draft lane is derived from the owner card's resolved layout lane without the old C5 clamp, so SignalListener append previews no longer sit in the listener column and should not jump after save. The draft id includes the target action index so multiple existing action cards are not covered by the new draft. The main save button requires both the Logic Chain editor lock and this target container lock; losing the Action container lock keeps the draft but blocks save. It only appends one new action to the end of the selected list/bucket; it does not move, delete, reorder or complex-edit existing actions.

New disconnected logic chains start from the Logic Chain list with "新建逻辑链". That flow creates WebAdmin root channel metadata / logic chain metadata so the new chain appears in the list and can be opened as its own root. It does not force the new root onto the current graph.

8.14 keeps the 8.13 edge routing model: smooth Bezier curves, unified source / target anchors and target arrow de-duplication. It does not restore shared trunk, polyline, elbow routing or merge-point rendering.

## Dirty Exit And Toast

Exit edit mode means leaving edit mode only. It does not navigate away from the Logic Chain page.

If the draft is dirty, WebAdmin shows a custom dirty exit dialog:

- "继续编辑" keeps the draft.
- "丢弃草稿并退出" releases/cancels the edit session and returns to read-only viewer mode.

Dirty route changes are guarded against the exact route that entered edit mode. Switching to the Logic Chain list, another Logic Chain detail or a different resolve route must prompt before discarding draft state; silent refresh of the current route remains allowed.

Save validation failures return structured validation errors with `code`, Chinese `message`, `nodeId`, `edgeId`, `channelId`, `severity` and `fixHint` when available. The top-center toast uses the first concrete reason as a summary, and the canvas shows a detailed validation list with the related node, channel, edge and repair hint. During validation or typed save failure, the canvas preserves the draft, main editor lock, scroll and focus so the admin can retry with the same lock. Browser `alert`, `confirm` and `prompt` are not used.

## Runtime Boundary

8.14 does not change runtime semantics:

- no SignalBridge behavior change
- no ActionEngine behavior change
- no Timer runtime behavior change
- no Signal Join runtime behavior change
- no StateAction runtime behavior change
- no Condition runtime behavior change
- no new `ActionType`
- no new `ConditionNodeType`
- no GameController / MissionSystem / PhaseController

The save path writes existing config through existing typed services and then the normal runtime reads those configs exactly as before.

## Validation Plan

Required validation for this stage:

- `cd tools\tzz-test-mcp; npm run build; npm test`
- `.\gradlew.bat clean build`
- `.\gradlew.bat stabilizationGuardTest --rerun-tasks`
- `.\gradlew.bat localTestMcpGuardTest --rerun-tasks`
- JS export plus `node --check build\tmp\webadmin-app.js`
- `git diff --check`

Do not start Minecraft. Do not run MCP scenario. Do not create screenshots. Do not commit / push / merge / tag during implementation review.
