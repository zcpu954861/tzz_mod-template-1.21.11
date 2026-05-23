# 9.x Roadmap Input After 9.0 Audit

This is roadmap input, not a final version plan. User confirmation is required before turning any candidate into a concrete 9.x implementation prompt.

## Audit Conclusion

The old datapack is a complete concrete "全员逃走中" game script. It uses:

- scoreboard fake players and triggers for global/player state;
- team/tag membership for roles and player flags;
- schedule chains for countdown, missions and cleanup;
- raw text JSON for player/OP interfaces;
- direct `setblock` / `clone` / `tp` / `effect` / `gamemode` commands;
- fixed-map structures and coordinates;
- custom loot-table items;
- game-end scoreboard aggregation.

Current 8.x TZZ Mod has strong low-level systems but not a game-program runtime:

- SignalBridge, ActionEngine, ConditionEngine, StateVariable, Timer, Signal Join, RegionController, VBD, itemSubmit/container, Logic Chain, Templates, Snapshot, Help and Doctor are all useful foundations.
- GameController / MissionSystem / PhaseController, if/else runtime and a full global program editor are still not implemented.

## Recommended 9.x Direction

Use the old datapack as a parity target, but do not migrate it function-by-function. Convert recurring patterns into typed, safe, inspectable mod capabilities:

1. State and role model instead of scoreboard fake players.
2. Timer and lifecycle model instead of loose `schedule function` chains.
3. Typed actions instead of raw commands for common operations.
4. Rich Text Builder instead of handwritten tellraw/title JSON.
5. Logic Chain-based global editor surface, but with a higher-level Program/Mission model above the resource graph.
6. Old item/system integration through Signal / Condition / Action / State where it adds real authoring value.

## Candidate Small Versions

| Candidate | Goal | Solves |
| --- | --- | --- |
| 9.1 State / Role / Legacy Mapping Foundation | Define game roles, readiness, global/player state definitions and old scoreboard mapping docs/UI | Team readiness, game active, current task, old fake-player state |
| 9.2 Typed Actions + Rich Text Builder | Add safe typed action candidates and WebAdmin builders after user confirms scope | tellraw/title/actionbar, tag/team/gamemode/effect/tp/item actions |
| 9.3 Global Editor Foundation | Reuse Logic Chain canvas patterns for a higher-level game program editor shell | Visual organization beyond resource graph |
| 9.4 GameController / Lifecycle MVP | Model start, active, pause/reset/end and cleanup boundaries | Old `/reload` and `game_end` cleanup patterns |
| 9.5 MissionSystem / Task Chain MVP | Model mission start, success/failure, timers, conditions and rewards | Old task_one through task_6 schedule chain |
| 9.6 Legacy Item Integration | Bring blocking cards/password machines/sensors into Signal/Condition/Action/Doctor where needed | Old card/password/sensor/game-object parity |
| Later 9.x World Mutation Safety | Design world placement, structure assets, region limits and preview/recovery | Old `setblock`, `clone`, fixed-map structure usage |

These names and boundaries are suggestions only.

## Requires User Confirmation

Before implementation, the user should confirm:

- whether 9.x should prioritize a faithful "全员逃走中" replacement or a generic minigame IDE foundation;
- whether old task one through task six should become built-in templates, example programs or only reference material;
- whether old fixed coordinates/structures matter for compatibility;
- whether typed world mutation actions are acceptable and what safety limits they need;
- whether player runtime UI should be phone-based, WebAdmin-based, chat/tellraw-based or a mix;
- which typed actions are safe enough to add first;
- whether old items should be first-class game-program nodes.

## Cannot Be Implemented In 9.0

9.0 remains docs-only. It must not implement:

- GameController;
- MissionSystem;
- PhaseController;
- typed actions;
- Rich Text Builder;
- if/else runtime;
- full Logic Chain Editor;
- old item integration;
- datapack function migration;
- world structure placement;
- new `ActionType`;
- new `ConditionNodeType`.

## Deferred To 10.x Or Later Unless Reprioritized

- automatic datapack-to-mod conversion;
- Git-like branch / merge / rebase;
- external template marketplace;
- automatic world entity copy;
- full live world/player inventory backup;
- arbitrary NBT path editor;
- raw script expression system;
- complete Scratch-like visual programming environment.

## Handoff Notes

The next planning conversation should start from the six 9.0 audit documents:

- `docs/LEGACY_DATAPACK_AUDIT_9_0.md`
- `docs/LEGACY_DATAPACK_PARITY_MATRIX_9_0.md`
- `docs/LOGIC_CHAIN_GLOBAL_EDITOR_GAP_AUDIT_9_0.md`
- `docs/TYPED_ACTIONS_RICH_TEXT_AUDIT_9_0.md`
- `docs/LEGACY_ITEMS_SYSTEMS_INTEGRATION_AUDIT_9_0.md`
- `docs/NINE_X_ROADMAP_INPUT_AFTER_AUDIT_9_0.md`

The roadmap should stay incremental: strengthen existing 8.x foundations before introducing broad gameplay runtime.

## 9.0 Handoff Checklist

Final 9.0 handoff should report:

- branch, HEAD and `git status --short --branch`;
- old datapack branch / HEAD / commit summary;
- validation result for `git diff --check`;
- whether commit / push / merge / tag were not performed;
- whether `.codex/` and `logs/` remained unhandled;
- whether no runtime code, WebAdmin business logic, `ActionType` or `ConditionNodeType` changed;
- whether no datapack files, `reports/mcp`, screenshots, `node_modules`, `build`, `run` or `.gradle` artifacts were staged or committed.
