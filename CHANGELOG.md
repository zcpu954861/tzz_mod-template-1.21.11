# Changelog

## v1.24.0-web-admin-region-action

- Added 6.6 WebAdmin Region management readonly page at `/app#/regions`.
- Added 6.6 WebAdmin Region detail readonly page at `/app#/regions/<regionId>`.
- Added 6.6 WebAdmin Action system readonly page at `/app#/actions`.
- Added 6.6 WebAdmin Action detail readonly page at `/app#/actions/<actionId>`.
- Added readonly Region search, world filter, enabled-state filter, Doctor-state filter, player-presence filter, and sorting controls.
- Added readonly Action search, action-type filter, owner-type filter, execution-result filter, Doctor-state filter, and sorting controls.
- Region pages display bounds, target filter, enter/exit/stay action summaries, bound channels, player state summary, recent events, and Doctor summary when available.
- Action pages display action type, owner, references, recent result, config summary, recent executions, downstream signal link, and Doctor summary when available.
- Added readonly Region / Action / Signal / Doctor / History cross-page navigation where route targets are available.
- Added context-aware detail-page back navigation so device, Signal, Region, and Action detail pages return to the page that opened them, with module-list fallback for direct URL entry.
- Kept the 6.6 boundary readonly: no Region or Action creation, editing, deletion, execution, testing, WebSocket, config write, or write API.

## v1.23.0-web-admin-users-settings

- Added 6.5 WebAdmin user management readonly page at `/app#/users`.
- Added 6.5 WebAdmin system settings readonly page at `/app#/settings`.
- Added readonly WebAdmin user summary API with username, display name, role, enabled status, online/session summary, created time, createdBy, and last login time.
- Added readonly WebAdmin settings API with service status, access mode, host, port, storage summary, auth summary, audit status, session TTL, mod version, Minecraft version, server type, and world/save summary.
- User management is OWNER-only and never returns password hash, password salt, session token, cookie value, or plaintext password.
- Settings page hides sensitive storage path details from non-OWNER users while keeping basic readonly runtime status visible.
- Added sidebar navigation for 用户管理 and 系统设置.
- Kept the 6.5 boundary readonly: no user creation, delete, enable/disable, password reset, role change, session kick, host/port/accessMode edit, WebSocket, config write, or write API.
- Not included: Region / Action pages, user-management Web CRUD, system settings editing, permission editing, WebSocket, or configuration writes.

## v1.22.0-web-admin-doctor-history

- Added 6.4 WebAdmin Doctor global diagnostics readonly page at `/app#/doctor`.
- Added 6.4 WebAdmin History readonly timeline page at `/app#/history`.
- Added Doctor issue search, severity filter, object type filter, jump-target filter, summary cards, and readonly navigation to related devices, channels, and history.
- Added History event search, channel filter, source type filter, result filter, time range filter, sorting, timeline table, and readonly navigation to related channels/devices.
- Added Dashboard navigation to Doctor and History pages.
- Added device and Signal channel detail links to related History and global Doctor views.
- Kept timestamps formatted as `YYYY-MM-DD HH:mm:ss` in all new pages.
- Kept the 6.4 boundary readonly: no fix button, issue clear, history delete/export, signal emit, replay, WebSocket, config write, or edit API.
- Not included: WebSocket, automatic realtime sync, configuration editing, Region / Action full pages, user management page, or system settings page.

## v1.21.0-web-admin-signal-channels

- Added 6.3 WebAdmin Signal channel management readonly page at `/app#/signals`.
- Added Signal channel detail readonly page at `/app#/signals/<channel>` with safe hash-route channel encoding.
- Added a readonly horizontal logic-chain preview: trigger sources → channel → consumers → actions / downstream signals.
- Added Signal channel search, consumer filters, status filters, and sorting controls with explicit Chinese labels.
- Added channel consumer summaries for listeners, signal receivers, and action relays.
- Added recent Signal event display and channel-level Doctor summary in the channel detail page.
- Added navigation from Dashboard to Signal management and from device detail channel links to channel detail.
- Formatted WebAdmin visible timestamps as `YYYY-MM-DD HH:mm:ss` instead of raw ISO strings.
- Localized Signal page labels, consumer types, action types, empty states, and readonly hints.
- Improved channel type fallback so normal channels do not show a misleading “unknown channel” label.
- Kept the 6.3 boundary readonly: no channel creation, editing, deletion, listener/action mutation, signal emit test button, config write, WebSocket, export, or write API.

## v1.20.0-web-admin-dashboard-devices

- Added 6.2 WebAdmin Dashboard app shell using hash routes: `/app#/dashboard`, `/app#/devices`, and `/app#/devices/<deviceId>`.
- Added frontend API client for the 6.1 readonly APIs with unified JSON envelope handling, session-expired redirect, and Chinese loading / empty / error states.
- Added Dashboard overview cards for server status, device count, Signal channel count, Region / Action count, and Doctor warning / error totals.
- Added Dashboard sections for recent Signal history, device overview, and Doctor issue summary.
- Added readonly device management list page with search, type filter, enabled filter, Doctor status filter, and dynamic world filter.
- Added readonly device detail page with identity summary, channel card, debug checks, recent channel history, Doctor issues, and compact config summary.
- Added navigation shell with sidebar and top status bar while keeping 6.0 login/session and 6.1 APIs compatible.
- Added unified dark WebAdmin console styling with a shared 2D inline SVG icon system.
- Improved device list filters with clear Chinese labels for type, enabled state, Doctor status, and world / dimension.
- Improved device list subtitles so raw `minecraft` / namespace prefixes are not shown as truncated primary metadata.
- Improved device detail hierarchy, localized Debug checks, aligned Doctor status with Debug / list state, and collapsed raw config fields by default.
- Fixed WebAdmin sidebar layout so the sidebar stays fixed while the main content scrolls independently.
- Fixed client resource regressions covering lang JSON validity, item group translation keys, and key missing block/item model resources.
- Not included: device create/edit/delete, channel editing, enable/disable buttons, Signal channel detail page, full Doctor/History pages, WebSocket, config editing, Web user CRUD, Region/Action pages, ConditionEngine, or game scheduling.

## v1.19.0-web-admin-readonly-services

- Added 6.1 WebAdmin readonly Service / DTO layer for future dashboard and admin pages.
- Added authenticated readonly APIs: `GET /api/devices`, `GET /api/devices/{id}`, and `GET /api/devices/{id}/debug`.
- Added authenticated readonly Signal APIs: `GET /api/signals/channels`, `GET /api/signals/channels/{channel}`, and `GET /api/signals/history`.
- Added authenticated readonly `GET /api/doctor`.
- Added authenticated readonly Region APIs: `GET /api/regions` and `GET /api/regions/{id}`.
- Added authenticated readonly Action APIs: `GET /api/actions` and `GET /api/actions/{id}`.
- Added DTO and service packages under WebAdmin so future UI code consumes stable structures instead of internal objects or business JSON.
- All new 6.1 APIs require WebAdmin session authentication; `VIEWER`, `TESTER`, `EDITOR`, and `OWNER` can access the readonly endpoints.
- List endpoints apply bounded limits; unavailable runtime statistics return `UNKNOWN`, `null`, or empty arrays instead of forcing heavy scans.
- Changed WebAdmin persistent storage to the world-save scoped directory `<world-save-root>/tzz/webadmin/`, isolating WebAdmin config, users, and audit state per world.
- Legacy global WebAdmin files under `config/tzz` are detected for warnings but are not loaded, copied, migrated, or deleted automatically.
- Performance boundary: APIs read existing stores, history, and diagnostics only; they do not scan worlds, force-load chunks, tick-scan devices, or trigger game logic.
- Compatibility: 6.0 auth/session/status and `/tzz webadmin` commands remain unchanged.
- Compatibility: 5.x SignalBridge, SignalDevice, `virtual_block_device`, ItemStackMatcher, RegionController, ActionEngine, Doctor, History, command semantics, and JSON schema remain unchanged.
- Added `docs/web_admin_readonly_services.md`.
- Not included yet: dashboard pages, device pages, channel logic-chain pages, full Doctor/History pages, WebSocket, Web editing, Region/Action pages, ConditionEngine, or high-level game scheduling.

## v1.18.0-web-admin-foundation

- Added WebAdmin Foundation minimal backend loop.
- Added default-disabled `config/tzz/web_admin_config.json` with `enabled`, `host`, `port`, `accessMode`, session TTL, login code TTL, and audit settings.
- Added access modes `LOCAL_ONLY`, `LAN_DEV`, and `MULTIPLAYER_DEV`; non-local modes require explicit configuration and show safety warnings.
- Added `config/tzz/web_admin_users.json` for WebAdmin users.
- Passwords are stored with JDK `PBKDF2WithHmacSHA256`; plaintext passwords are never persisted.
- Added server-side random initial password generation for user create/reset flows.
- Added in-memory session service and `TZZ_WEBADMIN_SESSION` HttpOnly cookie.
- Added lightweight JDK HTTP server lifecycle: start when enabled, stop on Minecraft server shutdown.
- Added static login page and basic status page using a dark admin-console style.
- Added APIs: `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`, and `GET /api/status`.
- Added `/tzz webadmin status`.
- Added `/tzz webadmin user list|create|disable|enable|resetPassword`.
- WebAdmin commands are limited to console and OP / creative-level administrators.
- Added lightweight WebAdmin audit logging for server start/stop, login success/failure, logout, user creation, enable/disable, and password reset.
- Added `docs/web_admin_foundation.md`.
- Compatibility: 5.x SignalBridge, SignalDevice, `virtual_block_device`, ItemStackMatcher, itemSubmit, Doctor, debug, existing commands, and old JSON schema remain unchanged.
- Not included yet: device pages, signal channel pages, logic-chain view, full Doctor/History pages, WebSocket, Web config editing, Web user CRUD, OAuth, 2FA, HTTPS, public exposure automation, GUI, or ConditionEngine.

## v1.17.0-stabilization-foundation

- Added 5.15 Stabilization Foundation / GUI preparation pass for the 5.x SignalBridge toolchain.
- This version does not add new gameplay features, trigger types, GUI, Web UI, ConditionEngine, or JSON schema changes.
- Added stabilization audit reports for Round1 through Round5 and the final 5.15 summary.
- Added `stabilizationGuardTest` and wired it into Gradle `check` / `build`.
- Guard tests cover `SignalDeviceData` field preservation, legacy JSON sample compatibility, consume planning, itemSubmit evaluation, cooldown / vanilla policy decisions, display names, and diagnostic DTO rendering.
- Added `ConsumePlan` / `ConsumePlanner` guard logic for staged consume planning and atomic apply.
- Added `ItemSubmitEvaluator` / `ItemSubmitEvaluationResult` / `ItemSubmitInventoryAdapter`; production itemSubmit evaluation now uses the shared evaluator path.
- Added `InteractionDecisionEvaluator` / `InteractionDecision` to keep vanilla interaction locking separate from cooldown-controlled side effects.
- Strengthened guarantees that cooldown does not unlock `require_item_match` and does not skip enabled successful consume.
- Strengthened itemSubmit atomic consume behavior: all requirements and consume plans are checked before any real stack mutation.
- Enhanced `/tzz signal device debug` with structured device diagnostics.
- Enhanced `/tzz signal doctor` with device-level diagnostic summaries.
- Added diagnostic DTOs and Chinese display rendering for `DiagnosticSeverity`, `DiagnosticIssue`, `DeviceDiagnostic`, `InteractionItemDiagnostic`, `ItemSubmitDiagnostic`, and virtual block device diagnostics.
- Improved displayName / user-facing Chinese rendering for internal modes and diagnostic output.
- Added Web Admin UI preparation notes: future command, game tool, and Web UI writes should share service-layer APIs; Web UI should use DTOs and real-time sync rather than direct JSON edits.
- Compatibility: 5.5 through 5.14 behavior remains compatible.
- Compatibility: `signal_emitter`, `signal_receiver`, `action_relay`, and `virtual_block_device` behavior remains compatible.
- Compatibility: old `signal_devices.json` data remains readable; missing new fields normalize to safe defaults.
- Compatibility: existing command semantics and JSON field names are unchanged.
- Follow-up plan only: 5.15 final regression, then 6.x service layer / API / Web Admin UI preparation or GUI foundation work.

## v1.16.0-consume-submit

- Added 5.14 Consume Strategies / Multi-Item Submission MVP for `virtual_block_device`.
- `interactionItem` consume now supports explicit consume strategies for `main_hand`, `off_hand`, and `inventory_contains`.
- Added `/tzz signal blockDevice interactionItem consumeSource <x> <y> <z> matched_source|inventory|main_hand|off_hand`.
- Added `/tzz signal blockDevice interactionItem inventoryConsumeOrder <x> <y> <z> hotbar_first|main_inventory_first`.
- `armor_head`, `armor_chest`, `armor_legs`, `armor_feet`, and `armor_any` still do not support consume.
- Inventory consume only uses the triggering player's main inventory / hotbar and can consume across multiple matching stacks.
- Consume is atomic: all required items are checked before any stack is decremented.
- Added optional `itemSubmit` multi-item submission, disabled by default.
- `interactionItem` matcher and `itemSubmit` are now mutually exclusive matching modes.
- Enabling `itemSubmit` automatically disables the single-item `interactionItem` matcher while preserving success/fail feedback configuration.
- Added `/tzz signal blockDevice itemSubmit enable|disable <x> <y> <z>`.
- Added `/tzz signal blockDevice itemSubmit addFromHand <x> <y> <z> <name> at_least|exactly|at_most <count>` and `/tzz signal blockDevice itemSubmit addFromHand <x> <y> <z> <name> ignore`.
- Added `/tzz signal blockDevice itemSubmit list|info|infoAll|remove|clear`.
- Added `/tzz signal blockDevice itemSubmit enableRequirement|disableRequirement|matcherFromHand|matcherOption|count|consume|consumeOrder|consumeCount`.
- `itemSubmit` checks only the triggering player's main inventory / hotbar, requires all enabled requirements to match, and can optionally consume atomically.
- When `itemSubmit` is enabled, submit requirements decide success and the single-item `interactionItem` matcher / consume path is not evaluated.
- `ignore` count mode still takes no count parameter and means "do not check matcher count"; inventory matching requires at least one matching stack.
- `require_item_match` remains a lock: in `itemSubmit` mode it locks based on submit success/failure, and cooldown suppresses signal/message/sound/history/extra animation side effects but does not unlock failed matches or skip enabled consume.
- No world scan, chunk scan, neighbor scan, tick backpack scan, forced chunk load, GUI, armor consume, ConditionEngine, or generic NBT path query is implemented.
- Follow-up plan only: 5.15 stabilization / GUI preparation, later ConditionEngine / ConditionGroup, and 6.0 / 7.0 GUI / Admin UI.

## v1.15.0-equipment-armor-source

- 扩展 `virtual_block_device` 的 `interactionItem` 玩家物品来源匹配。
- 新增 `/tzz signal blockDevice interactionItem source <x> <y> <z> armor_head|armor_chest|armor_legs|armor_feet|armor_any`。
- `main_hand` 仍为默认来源，保持 5.10 / 5.11 / 5.12 旧配置兼容。
- `armor_head` / `armor_chest` / `armor_legs` / `armor_feet` 只读取触发玩家对应盔甲槽位。
- `armor_any` 只检查头盔、胸甲、护腿、靴子四个盔甲槽，并记录第一个匹配槽位。
- 右键事件仍只处理 `MAIN_HAND`；armor 来源只是匹配槽位 ItemStack，不处理装备事件。
- `consume` 仍只支持 `main_hand`；source 为 `armor_*` 时启用 consume 会被拒绝。
- 运行时遇到旧数据中 `armor_*` source 与 `consumeEnabled=true` 不兼容时不会消耗，并按失败流程处理或在 debug 中提示。
- `require_item_match` 锁定策略对 `armor_*` 同样生效；`interactionCooldownTicks` 不会让锁失效。
- `allow` 模式继续保持不阻止原版右键交互的兼容行为。
- `interactionItem info`、`device info` 和 `device debug` 显示 armor source、最近匹配槽位和匹配数量。
- 本阶段不实现装备 / 盔甲消耗、背包消耗、副手消耗、多物品提交、复杂条件组、GUI 或通用 NBT 查询。
- 后续计划仅记录：5.14 消耗策略 / 多物品提交、5.15 稳定化 / GUI 前置整理版、复杂 ConditionEngine / ConditionGroup 和 GUI / Admin UI。

## v1.14.0-player-item-source

- 增强 `virtual_block_device` 的 `interactionItem` 玩家物品来源匹配。
- 新增 `/tzz signal blockDevice interactionItem source <x> <y> <z> main_hand|off_hand|inventory_contains`。
- `main_hand` 为默认来源，保持 5.10 / 5.11 旧配置兼容。
- `off_hand` 只检查玩家副手物品，不处理副手右键事件。
- `inventory_contains` 只在玩家主手右键已绑定方块时检查触发玩家自己的主背包 / 热键栏。
- `inventory_contains` 不包含副手、装备栏或盔甲栏，不在 tick 中扫描。
- `inventory_contains` 的 `ignore` 表示存在至少一个匹配 stack，`at_least` / `exactly` / `at_most` 作用于匹配物品总数，其中 `at_most` 要求总数大于 0。
- `ignore` 数量模式不接收数量参数，info/debug 显示“数量要求：不检查”，最近匹配数量记录实际匹配到的数量。
- `consume` 仍只支持 `main_hand`；source 为 `off_hand` 或 `inventory_contains` 时启用 consume 会被拒绝。
- 运行时遇到旧数据中 source 与 consume 不兼容时不会消耗，并按失败流程处理或在 debug 中提示。
- `interactionItem info`、`device info` 和 `device debug` 显示 source、最近匹配来源、匹配槽位和匹配数量。
- 新增 `/tzz signal blockDevice interactionItem vanillaInteraction <x> <y> <z> allow|require_item_match`。
- `vanillaInteraction` 默认 `allow`，保持不阻止原版右键行为；显式设置 `require_item_match` 后，interactionItem 匹配失败会阻止原版 use，但仍可执行失败反馈。
- `require_item_match` 可作为锁定策略；`interactionCooldownTicks` 不会让锁失效，冷却中匹配失败仍会阻止原版交互。
- 对门使用 `require_item_match` 时支持上下半格归一化，绑定任一半格后右键另一半也会走同一设备锁定判断。
- 冷却只抑制 success/fail signal、message、sound、额外动画和结果/历史写入等副作用；5.14 起已启用的成功消耗不会被 cooldown 跳过。
- 成功 / 失败 signal 继续通过 SignalBridge emit，保留玩家上下文并记录到 SignalEventHistory / device history。
- 本阶段不实现背包消耗、副手消耗、装备栏 / 盔甲栏匹配、多物品提交、GUI 或通用 NBT 查询。

## v1.13.0-interaction-item-feedback

- 增强 `virtual_block_device` 的 `interactionItem` 主手物品匹配反馈。
- 新增匹配成功频道 `successChannel` 和匹配失败频道 `failChannel`，均通过现有 SignalBridge emit。
- 新增匹配成功 / 失败消息配置，消息完全由管理员设置，默认不显示。
- 新增匹配成功 / 失败音效配置，支持 namespaced sound id、volume 和 pitch，默认不播放。
- 新增匹配成功后消耗主手物品的可选配置，默认不消耗，`consumeCount` 默认 1。
- 物品数量不足以消耗时进入失败流程，不 emit 成功频道、不发送成功反馈、不消耗物品。
- `interactionCooldownTicks` 同时限制成功和失败反馈；5.14 起冷却中不 emit、不反馈，但匹配成功并启用消耗时仍会扣除物品。
- 成功和失败交互尝试都会播放 `MAIN_HAND` 主手挥手动画；冷却中不额外播放触发动画。
- 继续只处理 `MAIN_HAND`，不读取背包、副手、装备栏或盔甲栏。
- 保持 5.10 默认行为兼容：未配置 5.11 字段时，匹配成功仍回退使用 `interactChannel`，匹配失败仍默认静默。
- 本阶段不实现 GUI、玩家背包检测、副手匹配、装备栏 / 盔甲栏匹配或通用 NBT 查询。

## v1.12.0-itemstack-matcher

- 新增通用 `ItemStackMatcher`，供容器物品条件和右键交互主手物品匹配共用。
- 新增 `slot_matcher` 和 `total_matcher` 容器物品条件类型。
- 支持从执行者主手或容器槽位捕获 ItemStack 模板。
- 新增 `/tzz signal blockDevice itemCondition addSlotMatchFromHand <x> <y> <z> <name> <slot> at_least|exactly|at_most <count> <channel>`，`ignore` 模式使用 `<slot> ignore <channel>`。
- 新增 `/tzz signal blockDevice itemCondition addSlotMatchFromSlot <x> <y> <z> <name> <targetSlot> <templateSlot> at_least|exactly|at_most <count> <channel>`，`ignore` 模式使用 `<targetSlot> <templateSlot> ignore <channel>`。
- 新增 `/tzz signal blockDevice itemCondition addTotalMatchFromHand <x> <y> <z> <name> at_least|exactly|at_most <count> <channel>`，`ignore` 模式使用 `<name> ignore <channel>`。
- 新增 `/tzz signal blockDevice itemCondition addTotalMatchFromSlot <x> <y> <z> <name> <templateSlot> at_least|exactly|at_most <count> <channel>`，`ignore` 模式使用 `<templateSlot> ignore <channel>`。
- 新增 `matcherInfo`、`matcherFromHand`、`matcherFromSlot`、`matcherOption`、`matcherCount` 管理命令。
- 新增 `/tzz signal blockDevice interactionItem ...` 命令，用于给右键交互配置主手物品模板匹配。
- 右键交互物品匹配只检查 `MAIN_HAND`，匹配失败时不 emit、不阻止原版交互、不显示失败提示、不消耗物品。
- 当前 matcher 支持 item id、count、damage、自定义名称、lore、`custom_data` 和 data components 整体快照匹配。
- 本阶段不实现任意 NBT path 查询、玩家背包扫描、副手匹配、消耗物品、失败提示、装备栏或盔甲栏匹配。
- 保持 5.5 红石虚拟方块发射器、5.6 BlockState condition、5.7 interaction、5.8 container events、5.9 itemCondition id/count 行为兼容。

## v1.11.0-container-item-conditions

- 为 `virtual_block_device` 新增容器槽位 / 物品条件触发能力。
- 新增 `itemConditions` 设备数据，支持 `slot_empty`、`slot_item` 和 `total_item` 三类条件。
- 支持 `at_least`、`exactly`、`at_most` 三种数量比较模式。
- 新增 `/tzz signal blockDevice itemCondition addSlotEmpty <x> <y> <z> <name> <slot> <channel>`。
- 新增 `/tzz signal blockDevice itemCondition addSlotItem <x> <y> <z> <name> <slot> <itemId> at_least|exactly|at_most <count> <channel>`。
- 新增 `/tzz signal blockDevice itemCondition addTotalItem <x> <y> <z> <name> <itemId> at_least|exactly|at_most <count> <channel>`。
- 新增 `/tzz signal blockDevice itemCondition list|info|remove|clear|enable|disable|mode|offChannel|clearOffChannel|refresh|test` 管理命令。
- `itemId` 保存前会校验是否存在于运行时物品注册表。
- slot 条件保存前会校验槽位范围，越界会被拒绝。
- 新增条件时会初始化 `lastMatched`，避免设置瞬间误触发。
- 条件进入 / 退出边沿通过现有 SignalBridge emit，继续记录到 SignalEventHistory 和 device history。
- 复用 5.8 容器内容 fingerprint / check interval，在内容变化后检查 item conditions。
- 只检查已登记、已配置 itemCondition 的绑定容器，不扫描世界、区块或周围方块，不强制加载区块。
- 本阶段只比较 item registry id 和 count，不比较 NBT、数据组件、lore、自定义名称或附魔。
- `/tzz signal blockDevice info`、`containerInfo`、`/tzz signal device info/debug` 显示 itemCondition 摘要和诊断信息。
- 保持 5.5 红石触发、5.6 BlockState condition、5.7 interaction、5.8 container open/close/change 行为兼容。

## v1.10.0-container-events

- 为 `virtual_block_device` 新增容器事件触发能力。
- 新增 `/tzz signal blockDevice containerOpenChannel <x> <y> <z> <channel>`。
- 新增 `/tzz signal blockDevice clearContainerOpenChannel <x> <y> <z>`。
- 新增 `/tzz signal blockDevice containerCloseChannel <x> <y> <z> <channel>`。
- 新增 `/tzz signal blockDevice clearContainerCloseChannel <x> <y> <z>`。
- 新增 `/tzz signal blockDevice containerChangeChannel <x> <y> <z> <channel>`。
- 新增 `/tzz signal blockDevice clearContainerChangeChannel <x> <y> <z>`。
- 新增 `/tzz signal blockDevice container <x> <y> <z> enable|disable`。
- 新增 `/tzz signal blockDevice containerCooldown <x> <y> <z> <ticks>`。
- 新增 `/tzz signal blockDevice containerCheckInterval <x> <y> <z> <ticks>`。
- 新增 `/tzz signal blockDevice containerInfo <x> <y> <z>`。
- 支持已绑定容器方块打开、关闭和内容变化时 emit signal。
- open / close 基于右键候选和实际 screen session 确认，不把普通右键直接当作打开。
- content changed 使用轻量内容指纹，MVP 仅包含 slot 物品 id、数量和 damage，不做 NBT / 数据组件 / 槽位条件匹配。
- 容器事件只处理已登记的 `virtual_block_device`，不扫描世界、区块或周围方块，不强制加载区块。
- `/tzz signal blockDevice info`、`/tzz signal device info` 和 `/tzz signal device debug` 显示 container 摘要和诊断信息。
- `/tzz signal device history` 可查看 `virtual_block_device` 的容器事件触发记录。
- 保持 5.5 红石触发、5.6 BlockState condition 和 5.7 interaction 行为兼容。

## v1.9.0-block-interaction

- 为 `virtual_block_device` 新增右键交互触发能力。
- 新增 `/tzz signal blockDevice interactChannel <x> <y> <z> <channel>`。
- 新增 `/tzz signal blockDevice clearInteractChannel <x> <y> <z>`。
- 新增 `/tzz signal blockDevice interaction <x> <y> <z> enable|disable`。
- 新增 `/tzz signal blockDevice interactionCooldown <x> <y> <z> <ticks>`。
- 新增 `/tzz signal blockDevice interactionInfo <x> <y> <z>`。
- 交互触发只对已登记的 `virtual_block_device` 坐标生效，不监听未绑定方块。
- 右键事件只检查被右键的一个坐标，不扫描世界、区块或周围方块。
- 默认只处理 `MAIN_HAND`，避免主副手双触发。
- 默认不阻止原版交互，箱子、门、按钮等原有右键行为继续执行。
- 成功触发 interaction signal 时，触发玩家会播放一次主手挥手动画。
- interaction 会带玩家上下文进入 SignalBridge / ActionEngine。
- `interactionCooldownTicks` 单位是 GT，命令参数只输入整数。
- 当前方块 ID 与绑定时 `blockId` 不一致时不触发，并在 info/debug 中提示 refresh 或重新 bind。
- `/tzz signal blockDevice info`、`/tzz signal device info`、`/tzz signal device debug` 会显示 interaction 摘要和诊断信息。
- `/tzz signal device history` 可查看 `virtual_block_device` 的交互触发记录。
- 保持 5.5 红石虚拟方块发射器和 5.6 BlockState condition 功能兼容。

## v1.8.0-blockstate-condition

- 为 `virtual_block_device` 新增方块状态条件触发能力。
- 新增 `/tzz signal blockDevice condition <x> <y> <z> <condition>`。
- 新增 `/tzz signal blockDevice clearCondition <x> <y> <z>`。
- 新增 `/tzz signal blockDevice conditionMode <x> <y> <z> condition_enter|condition_exit|condition_both`。
- 新增 `/tzz signal blockDevice conditionInfo <x> <y> <z>`。
- 支持完整 BlockState 条件字符串，例如 `minecraft:lever[powered=true]`、`minecraft:redstone_lamp[lit=true]`、`minecraft:repeater[delay=4]`。
- 条件设置时按当前运行时方块实际 `BlockState` 验证 block id、property 名和值，不硬编码 Wiki 属性白名单。
- 方块不支持的状态会拒绝保存，例如 `minecraft:stone[waterlogged=true]`。
- 非法状态值会拒绝保存，例如 `minecraft:repeater[delay=9]`。
- `condition_enter` 支持条件从不满足变为满足时 emit `channel`。
- `condition_exit` 支持条件从满足变为不满足时优先 emit `offChannel`，未设置时回退 `channel`。
- `condition_both` 支持进入和退出条件都触发；`clearOffChannel` 后退出边沿回退使用 `channel` 属于设计行为。
- `/tzz signal blockDevice info`、`/tzz signal device info`、`/tzz signal device debug` 会显示 condition 摘要与诊断信息。
- tick 检测仍只遍历已登记的虚拟方块发射器，每个设备只读取自己的一个坐标，不扫描世界、区块或周围方块。
- tick 时不重新解析 condition 字符串，状态不变不 emit，也不写 `signal_devices.json`。
- 5.5 红石虚拟方块发射器功能保持兼容。
- 5.7 交互触发、5.8 容器事件、5.9 多条件触发仍只是后续计划，本阶段未实现。

## v1.7.0-virtual-block-device

- 新增 `virtual_block_device` 虚拟方块发射器设备类型。
- 支持把任意已存在方块坐标手动绑定为 signal 触发源。
- 同时检测方块自身 `powered` 属性和该坐标接收到的红石强度。
- 支持 `redstone_rising`、`redstone_falling`、`redstone_both` 三种触发模式。
- 支持可选 `offChannel`，用于断电边沿发出不同 signal。
- 新增 `/tzz signal blockDevice bind <x> <y> <z> <channel>`。
- 新增 `/tzz signal blockDevice offChannel <x> <y> <z> <channel>`。
- 新增 `/tzz signal blockDevice clearOffChannel <x> <y> <z>`。
- 新增 `/tzz signal blockDevice mode <x> <y> <z> redstone_rising|redstone_falling|redstone_both`。
- 新增 `/tzz signal blockDevice info <x> <y> <z>`。
- 新增 `/tzz signal blockDevice test <x> <y> <z>`。
- 新增 `/tzz signal blockDevice unbind <x> <y> <z>`。
- 新增 `/tzz signal blockDevice refresh <x> <y> <z>`。
- `/tzz signal device list/info/debug/test/enable/disable` 现在支持 `virtual_block_device`。
- `/tzz signal device cleanup` 会清理已加载区块中已变为空气的虚拟方块发射器记录。
- 虚拟方块发射器只检测已登记坐标，不扫描世界、不扫描区块、不扫描周围方块、不强制加载区块。
- 状态不变时不 emit，不写 `signal_devices.json`。
- `signal_devices.json` 写入增加节流，服务端停止时强制保存。
- 文档记录 5.6 BlockState 条件、5.7 交互触发、5.8 容器事件、5.9 多条件触发的后续计划，本阶段未实现这些功能。

## v1.6.0-action-relay

- 新增 `action_relay` 动作继电器方块。
- 支持 `signal -> action_relay -> ActionEngine actions`。
- 新增 `ActionRelayBlockEntity`，保存 `channel`、`enabled`、`cooldownTicks`、`actions[]`、最近执行时间和最近结果。
- `actions[]` 使用现有 `ActionConfig` 格式，不新增 action 类型。
- 新增 `/tzz signal relay bind <x> <y> <z> <channel>`。
- 新增 `/tzz signal relay addAction <x> <y> <z> command <command>`。
- 新增 `/tzz signal relay addAction <x> <y> <z> message <message>`。
- 新增 `/tzz signal relay addAction <x> <y> <z> sound <sound>`。
- 新增 `/tzz signal relay addAction <x> <y> <z> signal <channel>`。
- 新增 `/tzz signal relay listActions <x> <y> <z>`。
- 新增 `/tzz signal relay removeAction <x> <y> <z> <index>`。
- 新增 `/tzz signal relay clearActions <x> <y> <z>`。
- 新增 `/tzz signal relay cooldown <x> <y> <z> <ticks>`。
- 新增 `/tzz signal relay trigger <x> <y> <z>`。
- 新增 `/tzz signal relay info <x> <y> <z>`。
- `/tzz signal device bind/list/info/debug/test/enable/disable` 现在支持 `action_relay`。
- 新增 `/tzz signal device cleanup`，用于清理已加载区块中的无效 Signal 设备索引。
- Signal 设备被破坏后会自动从 `signal_devices.json` 移除，避免 `/tzz signal device list` 残留旧设备。
- SignalBridge emit 现在会同时分发到 SignalListener、`signal_receiver` 和已加载的 `action_relay`。
- `action_relay` 不输出红石，保持 SignalReceiver 作为实体红石输出端。
- 保持 SignalListener 作为后台虚拟逻辑接收端，`action_relay` 是世界中可见的 ActionEngine 执行节点。
- `action_relay` 使用与前两个 Signal 设备一致的多元素科技风模型，并采用绿色主题与短暂 active 高亮。

## v1.5.0-signal-receiver

- 新增 `signal_receiver` 信号接收器方块。
- 支持 `signal -> signal_receiver -> 红石输出`。
- 新增 `SignalReceiverBlockEntity`，保存 `channel`、`enabled`、`pulseTicks`、`remainingPulseTicks`、最近接收时间和最近结果。
- `pulseTicks` 默认 `5 GT`，命令参数使用整数 tick，不解析 `GT` 后缀。
- 新增 `/tzz signal receiver pulse <x> <y> <z> <ticks>`。
- 新增 `/tzz signal receiver trigger <x> <y> <z>`。
- 新增 `/tzz signal receiver info <x> <y> <z>`。
- `/tzz signal device bind <pos> <channel>` 现在支持 `signal_receiver`。
- `/tzz signal device list/info/debug/test/enable/disable` 现在支持 `signal_receiver`。
- `signal_receiver` 不需要 SignalListener 也能工作，只处理已登记且已加载的接收器，不扫描世界或强制加载区块。
- SignalBridge emit 现在会同时分发到 SignalListener 和已加载的 `signal_receiver`。
- `signal_receiver` 使用与 `signal_emitter` 一致的多元素科技风模型，红色区分接收端，并支持 powered on/off 视觉状态。
- 保持 SignalListener 作为虚拟逻辑接收端，`signal_receiver` 只负责红石输出，不执行命令动作。

## v1.4.1-signal-device-management

- 新增 `signal_devices.json` 信号设备管理索引。
- 新增 `/tzz signal device list`。
- 新增 `/tzz signal device name <x> <y> <z> <name>`。
- 新增 `/tzz signal device clearName <device>`。
- 新增 `/tzz signal device info <device>`，同时保留坐标版 `info`。
- 新增 `/tzz signal device history <device>`。
- 新增 `/tzz signal device debug <device>`。
- 支持使用设备名称、完整 sourceId 或短 ID 引用信号设备。
- 设备列表与详情优先显示名称，并显示短 ID、位置、频道、启用状态和最近触发时间。
- 设备调试会提示未绑定频道、设备禁用、频道无 listener、方块未加载或注册表与 BlockEntity 状态不一致。
- Signal 设备历史仍来自内存 SignalEventHistory，不新增历史 JSON，不扫描未加载区块。

## v1.4.0-signal-emitter

- 新增 `signal_emitter` 信号发射器方块。
- 新增 `SignalEmitterBlockEntity`，支持保存 `channel`、`enabled`、`lastPowered`。
- 支持红石上升沿 emit signal。
- 持续通电不会重复 emit。
- 支持 `/tzz signal device bind/info/test/enable/disable`。
- 支持右键信号发射器查看状态。
- 支持无玩家上下文的 SignalBridge 触发。
- SignalEventHistory 可记录 `signal_device` 来源。
- 新增 `signal_emitter` 科技风模型与贴图。
- `signal_emitter` 支持 powered on/off 视觉状态。

## v1.3.1-signal-observability

- 新增 Signal 事件历史记录。
- 新增 `/tzz signal history` 和 `/tzz signal clearHistory`。
- 新增 Signal channel 总览和详情命令。
- 新增 `/tzz signal channels`。
- 新增 `/tzz signal channel info <channel>`。
- 新增 `/tzz signal listen debug <listener>`。
- 新增 `/tzz signal doctor`。
- 增强 `/tzz signal` 命令反馈可读性，统一添加青色分割线。
- 修复带空格监听器名称解析。
- SignalBridge 可观测性增强，不改变原有 emit / listener / ActionEngine 执行语义。

## 下一开发版本

- 统一命令入口到 `/tzz`。
- 新增 `/tzz regionctl` 区域事件控制器命令。
- 支持将已有规划区域绑定为区域控制器。
- 支持玩家进入区域触发 `enterActions`。
- 支持玩家离开区域触发 `exitActions`。
- 支持玩家停留区域周期触发 `stayActions`。
- 区域动作接入 `ActionEngine` 统一执行。
- 支持区域控制器启用、禁用、删除、动作测试、目标过滤。
- 支持 `all`、`op`、`tag` 三种触发对象过滤。
- 新增 `region_controllers.json` 持久化存储。
- `/tzz regionctl` 命令反馈已中文化、颜色化，并优先显示名称和短 ID。
- 保留封锁卡原有使用方式，封锁卡命令执行已接入 `ActionEngine`。
- 补充 RegionController 使用说明和最小验收流程文档。

- 新增 SignalBridge 事件桥系统。
- 新增 `/tzz signal` 命令。
- 支持 signal listener 创建、删除、启用、禁用、查看、测试。
- 支持 listener 绑定 command action。
- 支持 listener 绑定 signal action，实现链式事件。
- 支持 listener cooldown。
- 支持 signal 递归保护。
- RegionController `addAction` 已支持 signal 类型。
- 支持 RegionController -> SignalBridge -> Listener -> ActionEngine 联动。
- `/tzz signal` 命令反馈已中文化、颜色化、名称优先。
- 补充 SignalBridge 使用说明和常见问题文档。

## 1.1.5

- 同步当前 GitHub Release 版本。
- 保留现有手机、AR、封锁卡、地图、任务、密码、图库、笔记等功能。
