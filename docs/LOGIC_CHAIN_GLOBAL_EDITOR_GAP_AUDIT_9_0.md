# 9.0 Logic Chain / Global Editor Gap Audit

This document audits whether the current Logic Chain Viewer / Editor can become the foundation for a 9.x global game program editor. It is not an implementation plan for 9.0.

## Current Viewer Capabilities

The Logic Chain Viewer can already display runtime/config graph relationships across:

- channel metadata;
- Signal producers and consumers;
- SignalListener actions;
- ActionRelay consumers and available action summaries, with full ActionRelay action/gate details limited by safe snapshot availability;
- RegionController actions;
- Signal Join inputs and outputs;
- Timer output channels and timer actions;
- StateVariable nodes from resolvable `state_variable` actions;
- condition and action gate guard nodes with ConditionGroup route metadata where available, not arbitrary global condition dependency scanning;
- component-style logic chain list entries discovered from connected channels.

The viewer is valuable because it visualizes how low-level SignalBridge / ActionEngine / Timer / Join / listener resources are connected.

## Current Editor Capabilities

8.14 and 8.16 implement controlled editing inside the viewer canvas. Current editable scope:

| Area | Current support |
| --- | --- |
| New pure-config nodes | Signal Join and Timer |
| Channel metadata affordance | channel endpoint / root channel metadata draft references can create or update channel display metadata, but they are not persisted as standalone runtime graph nodes |
| Channel metadata | displayName, note, iconKey; channel id rename deferred |
| Signal Join | display fields, mode/scope/reset/timeout/cooldown, input channels, output channel |
| Timer | display fields, mode-specific timing, scope, start policy, output channel |
| SignalListener basic config | enabled, channel, cooldownTicks, conditionGroupId |
| Existing ActionConfig | same-index replace/disable for SignalListener actions and Timer action buckets |
| Action append | append-only to existing SignalListener, ActionRelay, Region enter/exit/stay and Timer buckets |
| Local reconnect | saved through typed fields, not fake graph edges |
| Draft behavior | edit lock, dirty confirm, validation, draft overlay, legal slots and green-plus connection handles |

## Current Editor Boundaries

The current editor is intentionally not a full global program editor.

| Gap | Current status | Why it matters for old datapack parity |
| --- | --- | --- |
| Game lifecycle node | Missing | Old datapack has start/active/end/reset lifecycle state. |
| Mission / phase / task model | Missing | Old task chain is schedule-driven game program logic. |
| if/else branching | Missing | Old `execute if/unless` chains represent conditional branches. |
| Loop / tick program node | Missing | Old pack uses tick functions and repeated schedules. |
| StateVariable direct create/edit node | Missing | Old scoreboard fake players should become editable state definitions. |
| ConditionGroup first-class node creation | Missing | Current condition groups are referenced, not graph-authored. |
| Action gate as first-class editable branch node | Missing | Viewer can show guard nodes, but current editor treats `conditionGroupId` as typed config and gates are not else/fallback branches. |
| Team model | Missing | Old teams are core game roles. |
| Typed command-like actions | Mostly missing | Teleport, effect, gamemode, team, tag, give, setblock and clone are raw commands today. |
| Rich text builder | Missing | Old player/OP panels and results are raw JSON components. |
| World entity create/place | Deferred | Current graph references existing world devices/regions; old pack places structures/blocks. |
| Old node arbitrary move/delete/reorder | Deferred | Existing config is protected from arbitrary graph mutation. |
| Old action arbitrary delete/reorder | Deferred in Logic Chain | Same-index replace/disable and append are supported; delete/reorder are guarded. |
| ActionRelay / Region existing action same-index edit | Missing | 8.16 same-index action edit covers SignalListener actions and Timer buckets; ActionRelay / Region stay append-only in this canvas path. |
| SignalReceiver / VBD / physical producer binding edit | Missing | Existing world objects can appear as graph resources, but trigger/binding create or broad edit is not a graph save target. |
| Full component export | Deferred | Template component export and reverse dependency tracing are not implemented. |
| Condition gate as branch control | Missing | Current gates allow/block/skip; they do not create else/fallback/failure paths. |
| Graph document save | Missing by design | Current saves are typed config writes, not saving a freeform graph document. |

## Gap Matrix

| Legacy need | Current graph/editor support | Gap type | Candidate priority | Blocked by |
| --- | --- | --- | --- | --- |
| Show resource-level connected component | Viewer can show connected Signal/Timer/Join/action graph | Graph semantics | 9.1 | Whole game flow needs game-program taxonomy first |
| Edit current 8.x config | Controlled edit covers Join/Timer/channel/listener/action subset | Editor coverage | 9.1/9.2 | Need safe typed field paths |
| Create virtual listener from canvas | Deferred in 8.14/8.16 | Editor lock / lifecycle | 9.1 | Safe listener create flow |
| Direct StateVariable node | Not supported | Config authoring | 9.1 | State definition editor design |
| Direct ConditionGroup node | Not supported | Config authoring | 9.2 | Condition editor / compatibility UX |
| Team/tag actions | Raw command fallback only | Typed action | 9.2 | ActionType expansion and validation |
| Teleport/effect/gamemode actions | Raw command fallback only | Typed action | 9.2 | Permission and player context design |
| Scoreboard migration | StateVariable partially covers values | State model | 9.2 | Scope and migration policy |
| Mission task chain | No program/mission model | Runtime model | Later 9.x | GameController/MissionSystem |
| if/unless branching | Gate can skip; no else branch | Control flow | Later 9.x | If/else runtime |
| Structure/world placement | Raw commands only | World safety | Later 9.x | Region limits, preview, rollback story |
| Runtime reset/cleanup | Timer reset APIs exist; no game runtime reset model | Lifecycle | Later 9.x | GameController |

## What Can Be Reused

The current Logic Chain work should be reused for 9.x:

- canvas layout and graph reading patterns;
- edit lock, CSRF/same-origin, expectedFingerprint and typed-save discipline;
- draft overlay and validation feedback;
- local reconnect via typed config fields;
- compact diff and dirty-exit behavior;
- reference-node handling for resources that are not directly editable.

The current graph should not be treated as a complete program AST. It is a resource graph. A global editor needs a program layer above it.

## Suggested 9.x Slices

The following slices are candidate inputs only. They are not accepted implementation scope until the user confirms the ordering and boundaries.

9.1 candidates:

- Docs and schema input for a global editor model.
- Direct StateVariable definition editing from WebAdmin/global editor.
- Virtual SignalListener create from canvas.
- More complete editor coverage for existing 8.x objects without arbitrary move/delete/reorder.
- Explicit mapping from old datapack scoreboard/task concepts to editor primitives.

9.2 candidates:

- Rich Text Builder.
- Typed player/team/tag/effect/teleport/gamemode/world actions, plus richer message/title builders. Basic `message` and timer actions already exist.
- Condition/action gate nodes as first-class editable references.
- ActionRelay / Region existing action same-index edit parity.

Later 9.x, after user confirmation:

- GameController / Program root.
- MissionSystem / PhaseController.
- if/else and branch runtime.
- safe world mutation and structure placement.
- old datapack task reconstruction as templates or game programs.

## Non-Goals For 9.0

9.0 does not add:

- full Logic Chain Editor;
- Scratch editor;
- if/else runtime;
- GameController / MissionSystem / PhaseController;
- old node arbitrary move/delete/reorder;
- old action arbitrary delete/reorder;
- new ActionType or ConditionNodeType;
- migration of old datapack functions.
