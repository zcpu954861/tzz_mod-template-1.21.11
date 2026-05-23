# 9.0 Legacy Datapack Parity Matrix

This matrix maps the old datapack capabilities to the current 8.x TZZ Mod platform and identifies candidate 9.x gaps. It is planning input only; 9.0 does not implement the missing items.

Coverage values:

- `covered_direct`: current 8.x has a direct equivalent.
- `covered_composed`: current 8.x can cover it by composing existing systems.
- `partial`: current 8.x covers only part of the old behavior or requires raw command fallback.
- `missing`: no safe current equivalent.
- `unknown`: needs more detailed old-map or runtime evidence.

## Parity Matrix

| Legacy feature | Legacy evidence | Current TZZ capability | Coverage | Needed 9.x capability | Priority | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Player trigger panel | `tzz_main_user_operation_panel:tzz_user_operation_panel`, trigger objectives | No current runtime equivalent; WebAdmin and phone UI are only possible UX foundations | partial | In-game/game-program operation panel model; typed player commands | High | Current WebAdmin is admin-facing; old player panel is runtime UX. |
| OP operation panel | `tzz_op_user_operation_panel`, direct trigger/scoreboard buttons | No current runtime equivalent; WebAdmin admin pages and Timer controls are only admin-side foundations | partial | Safe OP runtime control surface for GameController | High | Old OP buttons directly run commands; mod should expose typed controls. |
| Load/init/gamerule setup | `tzz_initialize:*`, `tzz_gamerule`, `tzz_initialize_ok` | Bootstrap/config systems exist, but no game-program init node | partial | Game lifecycle init/reset stage and safe gamerule policy | Medium | Do not map `/reload` behavior directly to mod runtime. |
| Team assignment | `tzz_team_runner`, `tzz_team_catcher`, `tzz_team_op` | Condition player team checks; raw command action can call team commands | partial | Typed team actions and team state model | High | No typed `join_team` / team policy action yet. |
| Player readiness check | `tzz_game_start_ready`, `tzz_game_start_ready_cnt`, `tzz_game_start_ready_ok` | StateVariable + ConditionGroup + Signal Join can model low-level readiness signals | partial | Game lifecycle ready-check primitive | High | Current pieces do not provide the old player UI and lifecycle handoff. |
| Game active lifecycle | `Global tzz_game_active`, `tzz_current_task` | StateVariable can store global values | partial | GameController / Program state model | High | StateVariable is storage, not lifecycle orchestration. |
| Start confirmation | `tzz_startgame_confirm`, `tzz_game_start_process` | WebAdmin write confirmations and custom modals | partial | Runtime player/OP confirmation action and dirty-safe workflow | Medium | Existing WebAdmin confirm patterns are not in-game player panels. |
| 10 second countdown | `tzz_start_game_10second/*.mcfunction` | Timer DELAY / COUNTDOWN, action buckets, outputChannel | partial | Countdown preset and rich text/actionbar integration | High | Current Timer can count, but lifecycle cleanup and old actionbar/title UX need typed support. |
| OP actionbar timer | `tzz_game_running:op_timer/*`, `storage tzz_game:op_timer` | Timer status, Timer actions, Help/Doctor | partial | Actionbar typed action + event label builder | High | Current Timer does not provide first-class in-game actionbar display builder. |
| Global broadcast | `tzz_scheduled_tasks:tzz_global_broadcast` | Timer REPEAT + message action can approximate the primitive | partial | Repeat timer preset in global editor | Medium | Persistent schedule recovery and runtime ownership remain deferred. |
| Runner energy | `tzz_game_energy/*`, `tzz_run_energy` | StateVariable integer + Timer REPEAT + state action | partial | Player-scope repeated state update with game lifecycle binding | High | Current Timer runtime is not persistent and not tied to GameController. |
| Death spectator mode | `tzz_death_spectator`, `death` tag, gamemode changes | Player conditions and raw command action | partial | Typed gamemode/tag/death event actions | High | No typed gamemode/tag action; death trigger source needs explicit design. |
| Hunter spawn selection locks | `tzz_catcher_spawn_lock`, `tzz_catcher_spawn_selected`, `tzz_catcher_choose_spawn` | StateVariable, ConditionGroup, Signal Join can model locks | partial | Reservation/choice primitive or mission UI state | High | Large text UI and exclusive selection need a dedicated model. |
| Opening minigame random timer | `gamestart_minigame_1/*`, `random value`, `storage tzz_running_game:random_time` | Timer + StateVariable can approximate | partial | Typed random action and minigame state model | Medium | No typed random/string storage action yet. |
| Task one hunter-box lock | `task_one/*`, `tzz_task_one_catcher_chest_lock`, card loot | itemSubmit / container conditions, StateVariable, ActionEngine | partial | Blocking-card integration and typed item delivery task | High | Current itemSubmit exists under VBD/device; old task is concrete game mission logic. |
| Task two certification | `task_two/*`, gender tags, glowing effect | Conditions for player tag/team; raw command effect | partial | Typed effect/tag actions and mission objective model | High | No typed effect/tag action. |
| Task three catcher lock | `task_3/*`, `tzz_task_3_catcher_lock`, block trigger | VBD native triggers, StateVariable, ActionRelay can cover low-level pieces | partial | Global editor placement and condition/action binding | Medium | World entities must already exist; automatic map object setup and mission semantics are missing. |
| Task four revive | `tack_4/*`, firework entity scan, revive count | Region/VBD can model some triggers; raw command fallback | partial | Entity event conditions, typed revive/gamemode actions | Medium | Tick entity scan around dispenser is not a direct 8.x primitive. |
| Task five reward devices | `task_5/*`, three trigger counters | VBD native trigger + StateVariable + Timer can cover low-level triggers | partial | Mission device preset and per-player first/second reward logic | Medium | Per-player reward ranking needs program-level state. |
| Task six item delivery | `task_6/*`, chest data checks and item removal | container conditions, itemSubmit requirements can cover item checks | partial | Mission delivery preset with typed success/failure actions | High | Current condition/template can check items, but game task flow and item removal timing are manual. |
| Game end stats | `game_end/game_end.mcfunction` | StateVariable read, message action, raw command fallback | partial | Aggregate query/actions and rich result renderer | High | Need typed aggregate stats over teams/players. |
| Rich tellraw panels | player/OP panels, guide/rule/start/task/end files | Message action exists; Help Center docs exist | partial | Rich Text Builder for tellraw/title/actionbar | High | Current action value is not a full component builder. |
| Sound feedback | 67 `playsound` commands | Sound action exists, but current fields are limited | partial | Rich sound action fields and target/category controls | Medium | Old UX uses audio feedback heavily; current sound action should not be treated as full parity. |
| Scoreboard operations | 410 scoreboard commands | StateVariable and state actions | partial | More state scopes and scoreboard/team/tag migration strategy | High | Current scopes are GLOBAL/PLAYER only. |
| Tags | player/task tags | Condition player tag checks; raw command actions | partial | Typed tag add/remove/test actions | High | Condition can read tags; action side is raw command. |
| Teams | team add/join/modify/leave | Condition player team checks; raw command action | partial | Typed team action and game team definition model | High | Needs policy/safety guard. |
| Schedule chain | 112 schedule commands | Timer modes and timer_start/timer_cancel cover isolated scheduling | partial | Timeline orchestration tied to game lifecycle | High | Timer active state is in-memory and not a mission graph. |
| Block placement | `setblock`, `fill`, `clone`, structures | Raw command action only; Snapshot is config-only | missing | Typed world mutation with scope limits, preview and recovery | Medium | High-risk world writes require a separate safety design. |
| Structure assets | 32 `.nbt` files | Template Center does not copy world entities | missing | Map template/structure placement plan | Medium | 9.0 does not copy assets. |
| Custom loot items | `tzz_item:*_card` loot tables | Mod has blocking-card items; templates do not apply loot definitions | partial | Typed item grant / item definition bridge | Medium | Old loot paper cards map conceptually to mod blocking cards. |
| Advancements | `data/tzz_game/advancement/*.json` | Help Center and Doctor are admin docs; no gameplay advancement builder | missing | Optional onboarding/achievement DSL | Low | Not required for core parity unless user wants it. |
| Boundary handling | `tzz_boundary/*`, temp x/z, cooldown | RegionController enter/exit/stay and conditions cover low-level region events | partial | Region preset for game boundary + typed teleport/message | Medium | Typed teleport and old cooldown/penalty semantics are missing. |
| Reload cleanup | `tzz_scheduled_task`, `game_end` schedule clear | Snapshot/Rollback config recovery; Timer reset APIs | partial | Runtime lifecycle reset / cleanup plan | High | 8.x Snapshot does not restore live runtime or world state. |
| Config rollback | Old pack warns reload resets map/data | Snapshot / Rollback allowlisted WebAdmin config | partial | Game runtime save/restore rules | Medium | Snapshot is not a world/entity/player inventory backup. |

## Coverage Summary

Current 8.x already provides strong low-level building blocks:

- SignalBridge for event routing.
- ConditionEngine for side-effect-free checks.
- StateVariable for global/player state values.
- ActionEngine for command/message/sound/signal/state/timer actions.
- Timer for delay/countdown/repeat scheduling.
- Signal Join for barrier/aggregator logic.
- RegionController and VBD for world triggers.
- itemSubmit/container conditions for item delivery and container checks.
- Logic Chain Viewer plus controlled canvas editing for visualization and guarded config edits.
- Templates, Snapshot/Rollback, Help and Doctor for reuse, recovery and diagnostics.

The remaining gap is not one missing command. The old datapack is a concrete game program built from scoreboard, schedule and function chains. 9.x should therefore add typed, safe game-program primitives rather than only more raw command fields.

## 9.x End Goal Input

The target for 9.x can be: replace the old datapack's practical authoring workflow as much as possible before 9.x ends. That does not mean 9.0 implements the replacement. It means future 9.x slices should progressively convert the old datapack's patterns into:

- typed actions instead of raw vanilla commands;
- rich text builders instead of handwritten JSON;
- game lifecycle / mission / phase models instead of ad hoc scoreboard fake players;
- global editor UI built on the Logic Chain controlled-edit foundation;
- explicit world mutation safety instead of direct `setblock` / `clone` everywhere.
