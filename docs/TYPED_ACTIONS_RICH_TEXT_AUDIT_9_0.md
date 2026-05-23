# 9.0 Typed Actions / Rich Text Builder Audit

The old datapack relies on raw vanilla commands and raw text JSON. This document identifies candidates for future typed actions and rich text tooling. It does not add any action type in 9.0.

## Legacy Command Patterns

Approximate counts from 120 old `.mcfunction` files:

| Family | Count | Future direction |
| --- | ---: | --- |
| `execute` | 675 | Mostly Condition / branch / context binding, not a direct action type |
| `scoreboard` | 410 | StateVariable and future scoreboard migration tools |
| `tellraw` | 243 | Rich Text Builder and message action expansion |
| `schedule` | 112 | Timer / Scheduler typed controls |
| `tag` | 74 | Typed tag actions and tag conditions |
| `playsound` | 67 | Sound action coverage, richer target/pitch/volume fields |
| `clone` | 33 | High-risk world mutation action, deferred |
| `setblock` | 31 direct + 1 macro | High-risk world mutation action, deferred |
| `gamemode` | 22 | Typed gamemode action |
| `team` | 16 | Typed team action and team model |
| direct `data` / storage references | 15 direct `data`, 21 storage references | StateVariable/string/random support; raw storage access deferred |
| `effect` | 13 | Typed effect action |
| `title` / `actionbar` | 10 title commands, 8 actionbar uses | Rich Text Builder + title/actionbar action |
| `tp` | 4 direct + 1 macro | Typed teleport action |
| `loot give` | 3 | Typed item grant action |

## Typed Action Candidates

| Candidate | Legacy evidence | Current substitute | Priority | Notes |
| --- | --- | --- | --- | --- |
| Rich message / tellraw | 243 `tellraw`, click/hover buttons, score/selector components | `message` action or raw `command` | High | Needs target selector and component builder. |
| Title / actionbar | OP timer, countdown/task messages | raw `command` | High | Actionbar timer is a major old UX pattern. |
| State variable write | scoreboard set/add/remove/operation | `state_variable` action partially covers | High | Need migration guidance from scoreboard fake players. |
| Player tag add/remove | gender, death, task flags | raw `command`; conditions can read tags | High | Add/remove/clear tag should be typed and gated. |
| Team join/leave/modify | runner/catcher/op teams | raw `command`; conditions can read team | High | Team definitions may belong in GameController. |
| Timer start/cancel | schedule function chains | `timer_start` / `timer_cancel` | Covered/High | Need editor presets and lifecycle binding. |
| Teleport | spawn and task relocation | raw `command` | High | Needs player selector, coordinate/anchor and safety checks. |
| Gamemode | spectator/adventure/creative switches | raw `command` | High | Needs permission, target and audit semantics. |
| Effect give/clear | glowing, cleanup | raw `command` | High | Needs typed effect id, duration, amplifier, particles. |
| Sound | `playsound` feedback | `sound` action | Medium | Current sound action may need richer target/category/pitch fields. |
| Item grant | `loot give` blocking cards | raw `command` | Medium | Should support mod items, components and count. |
| Inventory/container consume | chest item delivery | itemSubmit/container config exists | High | Needs mission-level success/failure action binding and item removal timing semantics. |
| Random number | `random value` into storage | none except raw command | Medium | Could be state random action with bounds. |
| Entity detection/event | task four firework scan near dispensers | no direct typed condition/action | Medium | Old pack de-duplicates firework entities with tags before handoff. |
| Score aggregate | game-end stats | raw command | High | Needs aggregate player/team state query actions or report builder. |
| Gamerule setup | load-time gamerule function | raw command / server config | Low | Should be lifecycle policy, not arbitrary command spam. |
| Particle / visual effect | macro/config visual calls | raw command | Low | Useful for polished tasks, but not core parity. |
| Block set/fill/clone | map object setup | raw command | Later | Requires world-safety design, preview, boundaries, rollback story. |
| Structure placement | 32 `.nbt` assets | none | Later | Requires asset import policy and map placement model. |
| Advancement grant/revoke | onboarding | raw command | Low | Optional unless old onboarding must be preserved. |

## Rich Text Builder Requirements

The old datapack uses text components for interactive UI and result rendering.

Legacy-required support includes:

- plain `text`;
- color names and hex colors;
- line breaks and reusable fragments;
- `score` component;
- `selector` component;
- `nbt` from storage for timer labels;
- hover text;
- click action with safe command/trigger/action target;
- target audience selector, ideally typed as players/team/op/all;
- output channel: chat / title / subtitle / actionbar;
- preview rendering in WebAdmin;
- validation against raw JSON syntax errors.

General builder support should also include `translate` and style flags such as bold / italic / underlined / strikethrough / obfuscated, even where the current old datapack uses them less heavily.

Old datapack details to preserve conceptually:

- 1.21 style lower-case `click_event` and `hover_event` appears in the old pack.
- OP buttons can run powerful commands; future builder should route buttons through typed operations, not arbitrary command text by default.
- Game-end output combines score values and player selectors; a result builder should understand dynamic values.

## Why Raw Commands Are Not Enough Long Term

Raw command fallback is useful for compatibility, but it should not be the main 9.x authoring surface:

- It hides player/world context requirements until runtime.
- It is hard to validate, audit and translate into UI.
- It encourages direct world mutation without preview or recovery boundaries.
- It makes templates hard to parameterize safely.
- It cannot explain failure reasons as well as typed fields.
- It makes Logic Chain / global editor nodes opaque.

9.x should keep raw command support as an escape hatch where already allowed, while adding typed actions for common safe operations.

## Not Implemented In 9.0

No `ActionType` is added in this audit. No rich text editor, world mutation action, team action, tag action or teleport action is implemented here.
