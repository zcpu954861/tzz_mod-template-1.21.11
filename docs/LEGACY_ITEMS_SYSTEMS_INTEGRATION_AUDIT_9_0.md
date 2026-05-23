# 9.0 Legacy Items / Old Systems Integration Audit

This document audits old mod items and systems that should be considered for 9.x integration into Signal / Condition / Action / State / Logic Chain. It is read-only planning input.

## Current Registry Evidence

Primary sources:

- `src/main/java/com/zcpu/tzzmod/ModItem/ModItems.java`
- `src/main/java/com/zcpu/tzzmod/ModBlock/ModBlocks.java`
- `src/main/java/com/zcpu/tzzmod/ModBlock/ModBlockEntities.java`
- `src/main/java/com/zcpu/tzzmod/action/ActionSourceType.java`
- `src/main/java/com/zcpu/tzzmod/signal/device/SignalDeviceData.java`
- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java`

Current registered blocks/items include old gameplay objects as well as newer SignalBridge devices.

## Integration Matrix

| Old item / system | Source | Current status | WebAdmin management | Signal emit | Condition gate | Action / typed action | State integration | 9.x recommendation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `phone` | `ModItems.PHONE`, phone client/server packages | Exists | Phone app visibility config exists; no WebAdmin game-program node found | Not directly | Not directly | Not an ActionEngine source | Not directly | Decide whether player operation panel should be phone app, WebAdmin-only, or both. |
| `ar_headset` | `ModItems.AR_HEADSET` | Exists | No direct WebAdmin object page found | Not directly | Not directly | Not an ActionEngine source | Not directly | Keep as player UX surface; do not force into Signal unless needed. |
| `attention` | `AttentionItem` | Exists | No direct WebAdmin page | No | No | Direct yaw snap behavior, not ActionEngine | No | Convert to typed action only if needed for templates; otherwise leave as utility item. |
| 16 blocking cards | `*_BLOCKING_CARD`, `BlockingCardItem` | Exists | Configurator item only; no WebAdmin card manager found | No direct Signal emit | Activation input can match entity/block internally | Executes `ActionEngine` command action with `ActionSourceType.BLOCKING_CARD` | No | Replace raw command payload with typed ActionConfig options and optional Signal emit/action gate support. |
| Blocking card configurator | `BlockingCardConfiguratorItem`, `BlockingCardServer` | Exists | In-game item UI, not WebAdmin | No direct Signal emit | Validates entity/block activation input | Saves command action to cards | No | Add WebAdmin-visible summary/diagnostic and typed action builder before relying on it for game-program tasks. |
| Old yellow/green/purple datapack cards | `data/tzz_item/loot_table/*_card.json` in old datapack | Old datapack only | Not in current WebAdmin | No | Item matching possible through itemSubmit/container | Can grant via raw command today | No | Map conceptually to mod blocking cards, not copy loot tables. |
| `password_config_card` | `ModItems.PASSWORD_CONFIG_CARD`, `PasswordServer` | Exists | No direct WebAdmin page found | No | No | Not ActionEngine; writes code to card/machine | No | Add optional Signal emit on success/failure or typed action output if passwords are used in missions. |
| `password_machine` | `ModBlocks.PASSWORD_MACHINE`, `PasswordMachineBlockEntity` | Exists | No direct WebAdmin page found | Redstone output only | No Condition gate found | Not ActionEngine; success powers block | No | Consider Signal emitter bridge and Condition context for password success/failure. |
| `map_marker` | `ModItems.MAP_MARKER`, map packages | Exists | No direct WebAdmin game-object page found | No | No | Not ActionEngine | No | Useful for coordinate anchors in 9.x; should not be auto-copied from old datapack. |
| `region_planner` | `ModItems.REGION_PLANNER`, `region_controller.md` | Exists | RegionController has WebAdmin pages | Indirect through RegionController actions | Region runtime gates exist | Region actions use ActionConfig | Region conditions exist | Keep as world-selection tool feeding WebAdmin/global editor. |
| `task_configurator` | `ModItems.TASK_CONFIGURATOR` | Exists | No full task system page | No | No | No current GameController action | No | Defer until GameController/MissionSystem exists. |
| `catcher_chest` | `ModBlocks.CATCHER_CHEST` | Exists | No direct WebAdmin page found | Redstone/block behavior only | Can be observed via VBD/native/container if bound externally | Not ActionEngine by itself | No | Treat as world object; integrate via VBD/region or future mission devices. |
| `silent_sensor_plate` | `ModBlocks.SILENT_SENSOR_PLATE`, `SilentSensorPlateBlockEntity` | Exists | No direct WebAdmin page found | Redstone output only | No direct gate found | Not ActionEngine source | No | Add optional Signal device adapter if used as mission trigger. |
| `signal_emitter` | `SignalEmitterBlockEntity` | Exists | WebAdmin device config | Emits SignalBridge channel | VBD/device gates available for device paths; emitter config managed | SignalBridge source | Signal history/Logic Chain visible | Already integrated; keep as base trigger. |
| `signal_receiver` | `SignalReceiverBlockEntity` | Exists | WebAdmin device config | Consumes signal/redstone output | SignalReceiver gate still deferred in docs | Not action source | Visible in Logic Chain | Keep readonly/managed; receiver gate remains future work. |
| `action_relay` | `ActionRelayBlockEntity` | Exists | WebAdmin config and action list | Consumes signal | List-level and action-level gates exist | ActionEngine action list | State actions supported | Already close to 9.x action executor role. |
| `virtual_block_device` | `SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE` | Exists as data device | WebAdmin VBD/native/item/container editors | Emits configured channels | VBD/itemSubmit/container gates exist | Can trigger downstream actions through signal/listeners | State actions via listeners/relays | Core bridge for old block/container triggers. |
| `split_iron_door` | asset/model/texture/lang plus light-mode mixin traces; not registered | Resource residual / unknown | None found | No | No | No | No | Keep as unknown/residual unless user confirms legacy need. |
| App icon items | `APP_ICON_*` | Exists but marked unobtainable | No gameplay object | No | No | No | No | Do not audit as gameplay items. |

## Integration Themes

Current mod already has the low-level bridge systems needed for many old objects:

- `ActionSourceType` includes `blocking_card`, `password_machine`, `silent_sensor_plate`, `region_controller`, `signal_bridge`, `signal_join`, `scheduler_timer`, `signal_device`, `action_relay`, `virtual_block_device`, `command`.
- Current `ActionType` is limited to `command`, `message`, `sound`, `signal`, `state_variable`, `timer_start`, `timer_cancel`.
- Condition gates cover many WebAdmin-managed runtime targets, but old standalone items are not uniformly gate-aware.

## 9.x Recommendations

High-priority candidates, pending user confirmation:

- Consider typed ActionConfig editing for blocking cards instead of command-only card payloads.
- Decide whether password success/failure should emit SignalBridge events.
- Consider old item/system diagnostics in Doctor where they affect game programs.
- Consider RegionPlanner and MapMarker as safe anchor sources for future global editor placement.

Medium-priority candidates:

- Provide WebAdmin readonly summaries for old in-game configured items where possible.
- Consider optional Signal adapters for silent sensor plate and password machine.
- Model blocking-card colors and old yellow/green/purple card parity as typed item requirements.

Deferred:

- TaskConfigurator integration until MissionSystem exists.
- Automatic old datapack loot table import.
- Automatic world block/entity copy.
- Arbitrary item NBT or raw command authoring as the main UI.
