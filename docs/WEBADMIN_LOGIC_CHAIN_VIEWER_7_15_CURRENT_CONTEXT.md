# 7.15 WebAdmin Cross-Channel Logic Chain Viewer MVP Current Context

## Stage Position

7.15 is a WebAdmin read-only visualization stage. It adds a cross-channel logic chain viewer MVP for existing SignalBridge relationships.

This stage does not create a new runtime object. A LogicChain is inferred from the current SignalBridge graph:

Producer -> Channel -> Consumer -> Action / Relay / Receiver -> Downstream Channel -> Next Channel Segment

The viewer may save WebAdmin-only metadata for a view, but that metadata does not modify SignalBridge runtime, channel bindings, listeners, receivers, relays, RegionController actions, VBD configuration, or ActionEngine behavior.

## Scope

Implemented / intended scope:

- Logic chain list page: `#/logic-chains`.
- Logic chain detail / viewer page: `#/logic-chains/<chainId>`.
- Temporary resolution route: `#/logic-chains/resolve?rootType=...&rootRef=...`.
- Node detail entry button from Signal channel, SignalListener, Signal device / receiver / action relay detail, and RegionController detail where a root can be resolved.
- Cross-channel tree / mind-map model: each channel remains a logical segment in data, but the UI renders it as a tree node instead of a table row.
- Current channel tree node expands the current channel consumers as parallel branches, then each consumer expands its own action list.
- Downstream channels expand as child subtrees from the action that emits them.
- Cycle detection and Maximum depth guard.
- Disabled nodes and disconnected/orphan channels are surfaced as warnings, not hidden.
- WebAdmin-only metadata: display name, note, icon key, tags, group, root type/ref, include disabled, max depth, and layout preference.
- Metadata writes use role permission, CSRF / same-origin, edit lock, expected fingerprint, `WebAdminWriteResult`, audit, realtime, and dirty guard.

## Channel Segment Semantics

A channel's consumers are parallel consumers. They must not be displayed as a strict sequential chain unless the order is from a concrete action list belonging to a single consumer.

Rules:

- Do not mix consumers from different channels inside one channel segment.
- Do not draw long cross-channel lines to distant consumers.
- Represent downstream channels as child channel nodes in the tree.
- Expand the downstream channel as the next subtree from that action.
- If a channel is already expanded or a cycle is detected, show an already-expanded / cycle reference and stop recursion.
- The graph is read-only.

## Explicit Non-Goals

7.15 does not implement:

- ConditionEngine.
- AND / OR / NOT.
- Conditional branches.
- Player/task/state conditions.
- Runtime node creation.
- Channel binding editing.
- Listener/receiver/relay/action list editing from the logic-chain page.
- Scratch-like runtime editor.
- SignalBridge runtime rewrite.
- GameController / MissionSystem / PhaseController.
- Raw JSON / NBT path editor.
- New MCP tool.
- MCP scenario.
- Automatic screenshot matrix.

## Supported Relationship Sources

Graph construction reads existing systems:

- Signal channels and signal history summaries.
- Signal devices and VBD channel-producing fields. Matcher / container item condition channels are shown only as signal-producing fields, not as ConditionEngine nodes or conditional branches.
- SignalListener channel consumers and signal actions.
- SignalReceiver channel consumers.
- ActionRelay channel consumers and loaded action lists.
- RegionController enter / exit / stay signal actions as producers.
- ActionConfig signal actions as downstream channel emitters.

ActionRelay action details depend on loaded block entity availability. If a relay action list cannot be read, the viewer should keep the relay as a consumer and surface a warning instead of inventing action data.

## UI Rules

- Use the existing dark WebAdmin style.
- The central canvas is a read-only node/canvas viewer, not an editor.
- The central canvas uses a mind-map / tree layout, not a fixed-lane table layout.
- Curved connectors show the left-to-right activation direction.
- The canvas supports mouse drag pan; selecting a node must preserve viewport pan, zoom, and scroll state.
- Local badges are anchored to their node or branch instead of floating at global canvas coordinates.
- parent y is centered by direct child subtree height so channel / consumer / action nodes align with their own branches.
- Same-channel consumers are parallel branches; action order is shown only inside the owning action list.
- Pan/zoom are view-only operations.
- The side panel is read-only node inspection plus navigation shortcuts.
- The legend must state that parallel consumers are not strict sequence.
- The legend must state that ConditionEngine and conditional branches are future 8.x work.
- The viewer must not expose runtime editing buttons.

## Future Direction

After 7.15, a later 7.x stage may refine channel-centric logic-chain viewing or navigation if needed.

The logic chain list uses a main chain / sub-chain hierarchy. A channel emitted by an upstream signal action is a sub-chain of that upstream channel, and a downstream of that sub-chain becomes a deeper sub-chain. Multiple upstream sources are marked as references, and cycles stop expansion instead of nesting forever.

Child-chain rows can be expanded and collapsed both ways. Expanded state is keyed by deterministic chain id, not row index, so search / sort / pagination do not corrupt nested child-chain visibility.

Self-cycle channels, such as A -> A, are marked as cycle references on the main chain and are not generated as their own child chain. Multi-hop loops such as A -> B -> A and A -> B -> C -> B stop at the back reference instead of creating infinite nested hierarchy.

SignalListener action management supports single action edit with the same dynamic action fields as add. The update API keeps action order and listener base fields, and action list modal scroll is restored after delete / edit validation refresh.

8.x ConditionEngine can later provide conditional decision nodes and branch semantics. Those nodes should be integrated into the logic-chain viewer only after the runtime condition system exists.
