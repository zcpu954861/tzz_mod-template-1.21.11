# 9.0 Legacy Datapack Audit

This document is a read-only audit input for 9.x planning. It does not migrate datapack files, does not implement gameplay runtime, and does not change the current TZZ Mod code.

## Source Repository

| Field | Value |
| --- | --- |
| Repository | `https://github.com/zcpu954861/1.21.11-tzz_game_datapack` |
| Local read-only path | `E:\minecraftserver\fabricmod\_audit_1.21.11-tzz_game_datapack` |
| Branch | `main` |
| HEAD | `e887a6074d9cee83db36406b7529b21e704156e7` |
| Commit summary | `第一次修bug` |
| Pack description | `tzz_game by zcpu` |
| Pack format | `max_format: 94`, `min_format: [94, 1]` |

The old repository was only read. No datapack files were copied into the mod repository.

## Resource Summary

| Resource type | Count | Notes |
| --- | ---: | --- |
| `.mcfunction` | 120 | Main gameplay, panels, tasks, schedules and map effects |
| `.json` | 12 | load/tick tags, advancements and custom loot tables |
| `.nbt` | 32 | Structure assets for hunter boxes, task devices and reset variants |

Namespaces found under `data/`:

- `minecraft`
- `tzz_game`
- `tzz_game_running`
- `tzz_global_function`
- `tzz_initialize`
- `tzz_item`
- `tzz_main_user_operation_panel`
- `tzz_scheduled_tasks`

The pack uses the current 1.21.11 single-name resource folders such as `function`, `advancement`, `loot_table` and `tags/function`. No `predicate` / `predicates` directory was found in this audit pass.

## Directory Structure

| Path | Purpose |
| --- | --- |
| `data/minecraft/tags/function/load.json` | load entry list |
| `data/minecraft/tags/function/tick.json` | tick entry list |
| `data/tzz_initialize/function/` | scoreboard, team, gamerule, schedule cleanup, reload setup and `tzz_initialize_ok` operator notice |
| `data/tzz_main_user_operation_panel/function/` | player and OP panels, guide/rule text, trigger enable loop |
| `data/tzz_game/function/tzz_start_game/` | start confirm, team check, ready check, hunter spawn selection |
| `data/tzz_game/function/tzz_start_game_10second/` | 10 second start countdown |
| `data/tzz_game/function/tzz_boundary/` | boundary detection and out-of-bounds handling |
| `data/tzz_game/function/tzz_game_energy/` | runner energy loop and display setup |
| `data/tzz_game/function/tzz_config/` | map coordinate action wrappers for blocks, teleports and task layout |
| `data/tzz_game/function/tzz_macros/` | macro functions for `tp`, `setblock`, `fill`, `particle` |
| `data/tzz_game_running/function/` | minigame, task chain, OP timer, game end |
| `data/tzz_global_function/function/` | death spectator and player effects |
| `data/tzz_item/loot_table/` | old blocking-card loot tables |
| `data/tzz_game/advancement/` | onboarding and flow advancements |
| `data/tzz_game/structure/` | structure assets and reset variants |

## Load And Tick Entries

Load entries:

| Function | Role |
| --- | --- |
| `tzz_initialize:tzz_create_scoreboard` | Create objectives, fake players, trigger objectives and cleanup task state |
| `tzz_initialize:tzz_create_team` | Create runner, catcher and OP teams |
| `tzz_initialize:tzz_gamerule` | Set base gamerules |
| `tzz_initialize:tzz_scheduled_task` | Clear stale schedules and start global scheduled tasks |
| `tzz_initialize:tzz_reload_build` | Rebuild or reset map blocks through config functions |
| `tzz_game_running:op_timer/init` | Initialize OP actionbar timer storage |

Tick entries:

| Group | Functions |
| --- | --- |
| Operation panels | `tzz_user_can_always_use`, `tzz_op_user_operation_panel`, `tzz_user_operation_panel`, `tzz_user_text_goto`, `tzz_game_rule`, `tzz_game_guide` |
| Global runtime | `tzz_death_spectator`, `tzz_player_effect` |
| Boundary | `tzz_game:tzz_boundary/tick` |
| Start flow | `tzz_startgame_confirm`, `tzz_catcher_choose_spawn`, `tzz_game_start`, `tzz_game_start_chack_team`, `tzz_game_start_ready_chack` |
| Timer | `tzz_game_running:op_timer/tick` |
| Task polling | `tzz_game_running:tack_4/detect_use_fire`, `tzz_game_running:task_6/test_chest_item` |

## Main Gameplay Flow

The old datapack is a concrete "全员逃走中" game script, not just a low-level utility pack.

1. `/reload` creates scoreboard objectives, teams, schedule cleanup and OP timer setup.
2. Player panel enables trigger-based operations every tick.
3. Players join runner/catcher teams and select gender.
4. OP panel opens start confirmation.
5. Catchers choose one of nine spawn points with scoreboard locks.
6. Team and ready checks set global readiness state.
7. A 10 second schedule chain starts the game.
8. The opening minigame loads; running energy is intended to start during the start / catcher-open flow and needs a later parity pass because the start function contains guarded commands after `tzz_game_active` is set.
9. Task chain progresses through minigame, task one, task two, task three, `tack_4`, task five, task six and game end. Folder numbering and user-facing mission text are not always identical; for example `task_6/start_task_6.mcfunction` displays `任务五`.
10. `game_end` clears scheduled functions, resets key global state and prints scoreboard-based results.

## Scoreboard / Tag / Team / Bossbar / Storage

### Scoreboard Objectives

49 objectives were found:

| Category | Objectives |
| --- | --- |
| Player panel triggers | `tzz_user_operation`, `tzz_join_runner`, `tzz_join_catcher`, `tzz_leave_all_team`, `tzz_call_op`, `tzz_text_expand`, `tzz_show_game_rule`, `tzz_show_game_guide`, `tzz_choose_gender` |
| OP / start triggers | `tzz_op_user_operation`, `tzz_game_start_confirm`, `tzz_game_start_process`, `tzz_game_start_ready`, `tzz_game_start_ready_check`, `tzz_catcher_spawn_point` |
| Player and game state | `tzz_death_spectator`, `tzz_death_spectator_switch`, `tzz_player_count`, `tzz_player_number`, `tzz_player_number_load`, `tzz_unassigned`, `tzz_game_active`, `tzz_current_task`, `tzz_team_check_done` |
| Ready and spawn locks | `tzz_game_start_ready_cnt`, `tzz_game_start_ready_ok`, `tzz_catcher_spawn_lock`, `tzz_catcher_spawn_selected` |
| Energy and statistics | `tzz_run_energy`, `tzz_catcher_kill_count`, `tzz_revive_count` |
| Boundary / temp math | `tzz_boundary_cd`, `tzz_tmp_x`, `tzz_tmp_z` |
| Opening minigame | `tzz_minigame_1_countdown`, `tzz_minigame_1_timer`, `tzz_minigame_1_timer_small`, `tzz_minigame_1_ok` |
| Task state | `tzz_task_one_catcher_chest_lock`, `tzz_task_3_catcher_lock`, `tzz_task_5_1_triggered_count`, `tzz_task_5_2_triggered_count`, `tzz_task_5_3_triggered_count`, `tzz_task_6_1_triggered_count`, `tzz_task_6_2_triggered_count` |
| OP timer | `tzz_op_timer_remaining`, `tzz_op_timer_tick`, `tzz_op_timer_math`, `tzz_op_timer_mode` |

Important fake-player names include `Global`, `switch`, `runner_total`, `runner_alive`, `catcher_total`, `cert_total`, `uncert`, `Total`, `maxEnergy`, `TotalKill`, `player_numbers`, `tzz_spawn_A` through `tzz_spawn_I`, `yellow`, `purple`, `green`, `const_60`, `min`, `sec` and `tmp`.

`tzz_current_task` is narrower than a full task program variable in the current old-pack code: it is explicitly used around `tack_4` detection and reset at game end, while other task handoffs are mostly schedule/function driven.

### Tags

Important player/entity tags include:

- `op`
- `death`
- `tzz_gender_male`
- `tzz_gender_female`
- `certification_male_passed`
- `certification_female_passed`
- `tzz_spawn_select_ok`
- `tzz_minigame_1_runner_1`
- `tzz_minigame_1_runner_2`
- `task_4_catcher_adventure`
- `task_4_catcher_creative`
- `tzz_handled_firework`
- `task_5_triggered_*`
- `task_6_triggered_*`

### Teams

| Team | Role |
| --- | --- |
| `tzz_team_runner` | Runner team, green, hidden nametag and death messages |
| `tzz_team_catcher` | Catcher team, red, hidden nametag and death messages |
| `tzz_team_op` | Operator/admin team, gold, visible nametag and death messages |

### Bossbar

No `bossbar` commands were found.

### Storage

| Storage | Use |
| --- | --- |
| `tzz_game:op_timer` | OP actionbar `event` text and remaining timer display |
| `tzz_running_game:random_time` | Opening minigame random target time |

## Command Pattern Summary

Approximate command counts from direct command-start scans in `.mcfunction` files. Macro-prefixed commands are called out where relevant:

| Command family | Count | Notes |
| --- | ---: | --- |
| `execute` | 675 | Main control-flow mechanism |
| `scoreboard` | 410 | State, triggers, fake players, counters and statistics |
| `tellraw` | 243 | Player/OP panels, mission text and game-end results |
| `schedule` | 112 | Countdown, timed tasks, loops and cleanup |
| `tag` | 74 | Player and entity state flags |
| `playsound` | 67 | UI/audio feedback |
| `clone` | 33 | Map/task object placement |
| `setblock` | 31 direct + 1 macro | Map/task object activation and reset |
| `gamemode` | 22 | Spectator/adventure/creative state changes |
| `team` | 16 | Team setup and joins/leaves |
| `data` | 15 | Storage and block item checks/removal |
| `effect` | 13 | Glowing, player effects and cleanup |
| `title` / `actionbar` | 10 title commands, 8 actionbar uses | OP actionbar timer |
| `tp` / `teleport` | 4 direct + 1 macro | Spawn/task relocation |
| `loot give` | 3 | Blocking-card item grants |

Frequent execute shapes:

- `if score`: 406
- `unless score`: 214
- `if entity`: 19
- `if block`: 14
- `if data`: 6
- `store result`: 3

## Schedule Usage

Schedules are used as a gameplay timeline. Important targets include:

- Global broadcast: `tzz_scheduled_tasks:tzz_global_broadcast 60s`
- Initialize operator notice: `tzz_initialize:tzz_initialize_ok`
- Opening 10 second chain: `tzz_game:tzz_start_game_10second/9second` through `0second_start`
- Opening minigame: `gamestart_minigame_1/start`, `start_timer`, `timer_run`, `small_timer_run`, `catcher_45s`, `catcher_open`
- Task chain: `task_one/task_one_start`, `task_one/test_task_one`, `task_two/start_task_two`, `task_two/test_task_two`, `task_3/start_task_3`, `task_3/test_task_3`, `tack_4/start_task_4`, `task_5/start_task_5`, `task_5/test_task_5`, `task_6/start_task_6`, `task_6/test_task_6`
- End: `tzz_game_running:game_end/game_end`

Both `tzz_initialize:tzz_scheduled_task` and `game_end` contain broad schedule cleanup. This is a high-priority parity requirement for any future GameController / MissionSystem runtime.

## Text Components

The datapack makes heavy use of raw text JSON:

- `tellraw` for player panel, OP panel, rules, guide, start confirmation, hunter spawn selection, mission descriptions and game-end statistics.
- `click_event.run_command` for trigger buttons and OP direct-control buttons.
- `hover_event.show_text` for button help.
- `score` components for scoreboard values in game-end result output.
- `selector` components for player names in result lists.
- `nbt storage` components for OP actionbar event text.
- `title actionbar` for the OP timer.

Top text-heavy files include:

- `data/tzz_game/function/tzz_start_game/tzz_catcher_choose_spawn.mcfunction`
- `data/tzz_game/function/tzz_start_game/tzz_startgame_confirm.mcfunction`
- `data/tzz_main_user_operation_panel/function/tzz_user_operation_panel.mcfunction`
- `data/tzz_main_user_operation_panel/function/tzz_op_user_operation_panel.mcfunction`
- `data/tzz_game_running/function/game_end/game_end.mcfunction`

## Structures And Loot Tables

Structure files are paired with reset variants for game objects:

- Opening hunter box: `gamestart_catcher_box`, `gamestart_catcher_box_none`
- Task one hunter boxes: `rw1_catcher_box`, `rw1_catcher_box_none`
- Task three certification/lock: `rw3_boy_check`, `rw3_girl_check`, `rw3_lock` and `_none` variants
- Task four revive points: `rw4_revive_1` through `rw4_revive_4` and `_none` variants
- Task five devices: `rw5_1` through `rw5_3` and `_none` variants
- Task six devices and chests: `task_6_1`, `task_6_2`, `task_6_chest_1`, `task_6_chest_2` and `_none` variants

Loot tables:

| Loot table | Item shape |
| --- | --- |
| `tzz_item:yellow_card` | `paper` with `custom_data.id=tzz_game:yellow_card`, item model and colored item name |
| `tzz_item:green_card` | `paper` with `custom_data.id=tzz_game:green_card`, item model and colored item name |
| `tzz_item:purple_card` | `paper` with `custom_data.id=tzz_game:purple_card`, item model and colored item name |

## High Complexity Chains

| Chain | Evidence | Risk |
| --- | --- | --- |
| Hunter spawn selection | `tzz_catcher_choose_spawn.mcfunction` and `tzz_catcher_select_*.mcfunction` | Nine shared locks, per-player selected slot, cancellation, team filtering and large text UI |
| Start flow | `tzz_startgame_confirm`, `tzz_game_start_chack_team`, `tzz_game_start_ready_chack`, `tzz_game_start` | Tick-based trigger handling, fake-player counters and start race prevention |
| Timed task chain | `task_one` through `task_6`, plus `game_end` | Schedule cleanup and handoff must be explicit in any future runtime |
| Task four firework detection | `tack_4/detect_use_fire.mcfunction` | Tick entity scan around dispensers, de-dup through entity tag |
| Task six item delivery | `task_6/test_chest_item.mcfunction` | Block NBT checks and direct block mutation |
| Game end | `game_end/game_end.mcfunction` | Clears schedules, resets global state and computes aggregate scoreboard stats |
| Coordinate macros / structures | `tzz_config/**`, `tzz_macros/**`, `structure/*.nbt` | Current mod cannot safely auto-copy world entities or structures without a separate design |

## Capability Modules Present In The Datapack

- Player and OP command panels.
- Team assignment and ready checks.
- Game lifecycle activation and countdown.
- Hunter spawn selection with exclusive locks.
- Runner energy system.
- Death spectator mode.
- Persistent player effects / minimap-worldmap assumptions through `tzz_global_function:tzz_player_effect`.
- OP actionbar timer.
- Global scheduled broadcast.
- Schedule-driven mission chain.
- Multiple tasks with block, item, entity, team and scoreboard conditions.
- Custom blocking-card loot tables.
- Fixed-map structure and coordinate placement.
- Boundary/out-of-area handling.
- Advancement onboarding.
- Game-end statistics and reporting.

## Uncertain Or Not Fully Parsed

- `.nbt` structure content was not decoded; only names and references were audited.
- Some coordinates are hidden behind macro/config functions and require a separate map-layout pass.
- High-signal task files still need a deeper mission-specific pass, including `task_two/male_certification.mcfunction`, `task_two/female_certification.mcfunction`, `task_5/trigger_1..3.mcfunction`, `task_6/test_point_1..2.mcfunction` and `task_6/test_chest_item.mcfunction`.
- The old README documents only part of the current task set; code includes later task files beyond the README summary.
- `tzz_game_start.mcfunction` sets `Global tzz_game_active` before several following commands that still require `tzz_game_active matches 0`; this may be an old bug or may be compensated by another path. 9.x planning should not copy that sequence verbatim.
- The old datapack relies on `/reload` cleanup and warns that reload after game start resets data/map state; a mod replacement needs explicit lifecycle and recovery semantics rather than a reload convention.
