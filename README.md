# Tzz_mod

Tzz_mod（mod id: `tzz_mod`）是用于替代复杂“全员逃走中”datapack 逻辑的 Minecraft / Fabric 游戏开发工具。它不是单纯的管理后台：模组同时提供手机、AR、地图区域、任务、封锁卡、SignalBridge、ActionEngine、区域控制器、虚拟监听器、WebAdmin 编辑层和本地测试辅助能力。

- 当前稳定版本：`v1.58.0-scheduler-delay-timer`
- 当前开发基线：`8.13 Logic Chain Viewer 增强`；本阶段把 WebAdmin Logic Chain Viewer 扩展为只读 runtime graph，显示 Signal Join、Timer、StateAction、StateVariable、Condition gate、single Action gate、上游/下游视图、节点类型筛选、recent status、Debugger / Doctor 跳转。发布后建议版本为 `v1.59.0-logic-chain-viewer-runtime-graph-enhancements`（最终以 tag 和 `gradle.properties` 的 `mod_version` 为准）
- 作者：`zcpu`
- 目标 Minecraft：`1.21.11`
- 依赖：Fabric Loader `>=0.18.4`，Fabric API `0.141.3+1.21.11`
- 许可证：`CC0-1.0`

## 当前 WebAdmin / 7.x 编辑层状态

7.x 已把 WebAdmin 从只读观察层推进到受控编辑层。当前已开放或补齐的主要能力包括：

- 登录、session、当前用户改密和 OWNER 密码重置辅助 API。
- Dashboard、设备管理、Signal channel、Doctor、History、用户与设置只读页面。
- 设备显示元数据编辑：显示名、备注、图标 key。
- 设备基础配置编辑：enabled、primary channel。
- 支持设备扩展配置：VBD 交互频道、成功/失败频道、冷却，SignalReceiver pulse ticks，ActionRelay cooldown 等。
- Signal channel metadata 编辑：显示名、备注、图标 key。
- VBD native trigger 编辑：红石、BlockState、右键、容器 open/close/change 等现有触发配置。
- interaction matcher 编辑：模板物品、来源、matcher options、数量模式和原版交互策略。
- itemSubmit unified requirement editor：0/1/N requirement 统一编辑，保留 all-or-nothing / staged consume 语义。
- container template 编辑：通过受控 session / GUI 路径编辑容器条件模板。
- ActionRelay action list 编辑：稳定 summary card + modal/drawer，一条一条新增、查看、删除、清空。
- RegionController editing：创建、删除、enabled/name/regionId/targetFilter/stayInterval 编辑，enter/exit/stay actions 管理。
- SignalListener / 虚拟监听器 editing：创建、删除、enabled/channel/cooldown 编辑，actions 管理，最近事件、edit lock 和运行时 action 支持。
- Logic Chain Viewer MVP：只读跨频道逻辑链查看器，把现有 SignalBridge 生产者、频道、消费者、动作和下游频道以可拖动画布的思维导图式树形视图展示；列表按主链 / 子链 / 多级子链组织，只允许保存 WebAdmin-only 视图 metadata，不修改 runtime。
- 7.15 逻辑链只读查看器不是编辑器；8.10 只把 Signal Join 作为只读节点接入，不提供完整 Logic Chain Editor。
- WebAdmin 写入基础：权限、CSRF / same-origin、edit lock、expectedFingerprint、`WebAdminWriteResult`、audit、realtime 和 dirty guard。

当前能力入口文档：

- [7.14 WebAdmin Editing Stabilization Current Context](docs/WEBADMIN_EDITING_STABILIZATION_7_14_CURRENT_CONTEXT.md)
- [WebAdmin Editing Capability Matrix 7.14](docs/WEBADMIN_EDITING_CAPABILITY_MATRIX_7_14.md)
- [7.15 WebAdmin Logic Chain Viewer Current Context](docs/WEBADMIN_LOGIC_CHAIN_VIEWER_7_15_CURRENT_CONTEXT.md)
- [8.0 ConditionEngine Core Current Context](docs/CONDITION_ENGINE_CORE_8_0_CURRENT_CONTEXT.md)
- [ConditionEngine Capability Matrix 8.0](docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_0.md)
- [8.1 基础玩家 / 上下文条件包 Current Context](docs/CONDITION_BASIC_PLAYER_CONTEXT_8_1_CURRENT_CONTEXT.md)
- [ConditionEngine Capability Matrix 8.1](docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_1.md)
- [8.2 State Variable System Current Context](docs/CONDITION_STATE_VARIABLES_8_2_CURRENT_CONTEXT.md)
- [ConditionEngine Capability Matrix 8.2](docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_2.md)
- [8.3 Item / Inventory / Container Conditions Current Context](docs/CONDITION_ITEM_INVENTORY_CONTAINER_8_3_CURRENT_CONTEXT.md)
- [ConditionEngine Capability Matrix 8.3](docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_3.md)
- [8.4 Region / Signal / Logic Chain Conditions Current Context](docs/CONDITION_REGION_SIGNAL_LOGIC_CHAIN_8_4_CURRENT_CONTEXT.md)
- [ConditionEngine Capability Matrix 8.4](docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_4.md)
- [8.5 WebAdmin Condition Editor Current Context](docs/WEBADMIN_CONDITION_EDITOR_8_5_CURRENT_CONTEXT.md)
- [ConditionEngine Capability Matrix 8.5](docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_5.md)
- [8.6 Condition Runtime Gates Current Context](docs/CONDITION_RUNTIME_GATES_8_6_CURRENT_CONTEXT.md)
- [8.7 Receiver-side Runtime Gates Current Context](docs/CONDITION_RUNTIME_RECEIVER_GATES_8_7_CURRENT_CONTEXT.md)
- [8.8 Condition Runtime Debugger Current Context](docs/CONDITION_RUNTIME_DEBUGGER_8_8_CURRENT_CONTEXT.md)
- [ConditionEngine Capability Matrix 8.8](docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_8.md)
- [8.9 Single Action Runtime Gate Current Context](docs/CONDITION_RUNTIME_SINGLE_ACTION_GATES_8_9_CURRENT_CONTEXT.md)
- [ConditionEngine Capability Matrix 8.9](docs/CONDITION_ENGINE_CAPABILITY_MATRIX_8_9.md)
- [8.10 Signal Join / Barrier / Aggregator Current Context](docs/SIGNAL_JOIN_BARRIER_AGGREGATOR_8_10_CURRENT_CONTEXT.md)
- [SignalBridge Capability Matrix 8.10](docs/SIGNAL_BRIDGE_CAPABILITY_MATRIX_8_10.md)
- [8.11 Controlled State Actions Current Context](docs/CONTROLLED_STATE_ACTIONS_8_11_CURRENT_CONTEXT.md)
- [ActionEngine Capability Matrix 8.11](docs/ACTION_ENGINE_CAPABILITY_MATRIX_8_11.md)
- [8.12 Scheduler / Delay / Timer Current Context](docs/SCHEDULER_DELAY_TIMER_8_12_CURRENT_CONTEXT.md)
- [Scheduler Capability Matrix 8.12](docs/SCHEDULER_CAPABILITY_MATRIX_8_12.md)
- [8.13 Logic Chain Viewer Enhancement Current Context](docs/LOGIC_CHAIN_VIEWER_ENHANCEMENT_8_13_CURRENT_CONTEXT.md)
- [Logic Chain Capability Matrix 8.13](docs/LOGIC_CHAIN_CAPABILITY_MATRIX_8_13.md)

当前仍未完成、不要误认为已完成的方向：

- 8.x：ConditionEngine / 条件判断系统已进入 8.13；当前提供无副作用判断核心、基础玩家 / 上下文条件、类型化状态变量底座、物品 / 背包 / 容器 snapshot 条件、Region / Signal / Logic Chain snapshot 条件、WebAdmin Condition Group 编辑 / 校验 / 模拟评估 MVP，8.6 / 8.7 已将 VBD / itemSubmit / container / SignalListener / ActionRelay / RegionController 作为可选外层 runtime gate 接入，8.8 增加 runtime history / Doctor / replay / WebAdmin 条件调试器，8.9 增加单条 Action gate，8.10 增加 Signal Join / Barrier / Aggregator 多事件汇合能力，8.11 增加 Controlled State Actions 状态变量写入动作，8.12 增加 Scheduler / Delay / Timer 通用时间轴能力，8.13 增强只读 Logic Chain Viewer runtime graph。当前仍不做具体逃走中任务，不接入 SignalReceiver gate、GameController、MissionSystem、PhaseController、failure policy、stop-list policy、fallback action、完整 Logic Chain Editor 或 raw JSON editor。
- 后续：GameController / MissionSystem / PhaseController。
- 未提供 raw JSON / NBT path 编辑器、Scratch-like editor、路径图编辑器或任意 shell。

## 8.0 ConditionEngine Core

8.0 是 TZZ Mod 从“事件工具链 / WebAdmin 编辑层”进入“可配置游戏逻辑判断层”的核心起点。当前只做 ConditionEngine Core：条件树、AND / OR / NOT、最小内置条件、EvaluationContext、ConditionResult debug tree、registry、validation 和安全限制。

8.0 已作为 `v1.46.0-condition-engine-core` 稳定基线发布。

核心边界：

- Condition 只负责判断，不产生副作用。
- 状态写入、发信号、发消息、给物品、传送、执行命令等仍由 Action / Signal / 未来 GameController 负责。
- 旧“全员逃走中”数据包只作为条件复杂度参考，不把旧 function、scoreboard、trigger 或任务流程搬进模组。
- 8.0 不做具体逃走中任务，不做任务一 / 任务二 / 复活任务 / 猎人出生点选择 / 逃走能量 / OP 计时器。
- 8.0 不做 GameController / MissionSystem / PhaseController。
- 8.0 不接入 VBD、SignalListener、RegionController、ActionRelay、itemSubmit runtime，不改 SignalBridge runtime。
- 8.0 不提供 WebAdmin 条件可视化编辑器，不提供 raw JSON / NBT path 编辑器，不新增 MCP tool，不跑 MCP scenario，不生成截图。

## 8.1 基础玩家 / 上下文条件包

8.1 在 8.0 Core 上加入第一批可复用基础条件。新增条件覆盖触发玩家存在、在线快照、管理员状态、玩家 tag、玩家队伍、玩家游戏模式、存活/死亡快照，以及 source/channel/world/device/listener/region/action/gameTime/event metadata 等上下文判断。

8.1 约束：

- 条件显示名支持中文，条件描述、字段名和失败原因也使用中文主文案。
- 条件 type id 仍使用稳定英文 lower_snake_case；WebAdmin / Doctor / Debug / capability 输出优先显示中文显示名、中文描述、中文字段名和中文失败原因。
- 8.1 仍未接入 runtime：VBD、SignalListener、RegionController、ActionRelay、itemSubmit 等运行路径不会自动调用 ConditionEngine。
- 8.1 不做任务/关卡，不做 GameController / MissionSystem / PhaseController。
- 8.1 不做 State Variable System，不做物品 / 背包 / 装备 / 容器条件，不做区域人数聚合，不做任务阶段条件。
- 8.1 不提供 WebAdmin 条件可视化编辑器，不提供 raw JSON / NBT path 编辑器，不新增 MCP tool，不跑 MCP scenario，不生成截图，不启动 Minecraft。
- 8.1 不启动 Minecraft，仍以纯单元测试、guard 和手动审查为主。

## 8.2 State Variable System

8.2 在 8.0 Core 和 8.1 基础条件之上加入类型化状态变量系统，用于替代旧数据包中大量 scoreboard fake player、player tag、全局分数和临时状态位的底层能力参考。

8.2 已提供：

- `GLOBAL / PLAYER` 状态变量 scope。
- `BOOLEAN / INTEGER / STRING` 状态变量类型。
- world-scoped store：`<world-save-root>/tzz/webadmin/state_variables.json`。
- `StateVariableService` 基础 set / remove / snapshot 写入服务，带 validation、fingerprint 和中文诊断。
- `ConditionEvaluationContext` 中的只读 `StateVariableSnapshot`。
- 状态变量条件：`state_variable_exists`、`state_variable_bool_equals`、`state_variable_int_compare`、`state_variable_string_equals`、`state_variable_string_contains`。
- 中文显示名、中文描述、中文字段名、中文 validation error 和中文失败原因。

8.2 约束：

- Condition 只读取状态变量，不写状态变量，evaluation 无副作用。
- 8.2 仍不接入 runtime：VBD、SignalListener、RegionController、ActionRelay、itemSubmit 等运行路径不会自动调用状态变量条件。
- 8.2 不提供 WebAdmin condition editor，不提供状态变量 WebAdmin 页面/API，不提供 raw JSON / NBT path 编辑器。
- 8.2 不做具体任务/关卡，不做 GameController / MissionSystem / PhaseController。
- 8.2 不做 item / inventory / container conditions，不做区域人数聚合，不做任务阶段条件，不做多人聚合条件。
- 8.2 不新增 MCP tool，不跑 MCP scenario，不生成截图，不启动 Minecraft。

## 8.3 Item / Inventory / Container Conditions

8.3 在 8.0 Core、8.1 基础条件和 8.2 状态变量系统之上加入物品、背包、容器相关的只读条件能力。它使用 condition-safe snapshot，不直接读取 live `ItemStack`、玩家背包、方块实体或世界。

8.3 已提供：

- `ConditionItemStackSnapshot`、`ConditionInventorySnapshot`、`ConditionContainerSnapshot`。
- `ConditionItemMatcher`，只支持 itemId equals 与 count compare。
- `ConditionEvaluationContext` 中的 `itemSnapshots`、`inventorySnapshots`、`containerSnapshots`。
- 物品条件：`item_stack_exists`、`item_stack_matches`。
- 背包条件：`inventory_contains_item`、`inventory_item_count_compare`。
- 容器条件：`container_slot_empty`、`container_slot_item_matches`、`container_item_count_compare`。
- 空物品语义：空 itemId、`minecraft:air`、`count <= 0` 均视为空。
- slot 使用 0-based；背包/容器数量统计跨多个 slot 聚合；count compare 支持 `eq/ne/gt/gte/lt/lte`。
- 中文显示名、中文描述、中文字段名、中文 validation error 和中文失败原因。

8.3 约束：

- Condition 只读取 snapshot，不消耗物品、不移动物品、不修改 snapshot、不写 store、不 emit signal、不执行 action。
- 8.3 仍不接入 runtime：VBD、interactionItem、itemSubmit、container、SignalListener、RegionController、ActionRelay 等运行路径不会自动调用这些条件。
- 8.3 不提供 WebAdmin condition editor，不提供 WebAdmin API，不提供 WebAdmin UI，不提供 raw JSON / NBT path 编辑器。
- 8.3 不做任意 NBT path、BlockEntity NBT path、通用脚本表达式或深层动态路径查询。
- 8.3 不做具体任务/关卡，不做 GameController / MissionSystem / PhaseController。
- 8.3 不新增 MCP tool。8.3 不跑 MCP scenario。8.3 不生成截图。8.3 不启动 Minecraft。

## 8.4 Region / Signal / Logic Chain Conditions

8.4 在 8.0 Core、8.1 基础条件、8.2 状态变量系统和 8.3 物品 / 背包 / 容器条件之上加入区域、信号、逻辑链相关的只读条件能力。它使用 condition-safe snapshot，不直接读取 live RegionController、SignalBridge、SignalEventHistory、WebAdmin Logic Chain Viewer service、世界或在线玩家列表。

8.4 已提供：

- `ConditionRegionSnapshot`：区域快照，包含 regionId、displayName、enabled、world、playerIdsInside、boundsSummary、metadata。
- `ConditionSignalChannelSnapshot`、`ConditionSignalHistorySnapshot`、`ConditionSignalEventSnapshot`。
- `ConditionLogicChainSnapshot`、logic chain node / edge snapshot。
- `ConditionEvaluationContext` 中的 `regionSnapshots`、`signalChannelSnapshots`、`signalHistorySnapshots`、`logicChainSnapshots`。
- 区域条件：`region_exists`、`region_enabled`、`player_in_region`、`region_player_count_compare`。
- 信号条件：`signal_channel_exists`、`signal_channel_consumer_count_compare`、`signal_event_count_compare`。
- 逻辑链条件：`logic_chain_contains_node`、`logic_chain_contains_channel`、`logic_chain_has_cycle`、`logic_chain_node_count_compare`。
- count compare 支持 `eq/ne/gt/gte/lt/lte`。
- 中文显示名、中文描述、中文字段名、中文 validation error 和中文 failureReason。

8.4 约束：

- Condition 只读取 snapshot，不修改 region / signal / logic chain snapshot，不写 store，不 emit signal，不执行 action。
- 8.4 仍不接入 runtime：VBD、SignalListener、RegionController、ActionRelay、Action、itemSubmit 等运行路径不会自动调用这些条件。
- 8.4 不读取 live world，不读取 live RegionController，不读取 live SignalBridge / SignalEventHistory，不调用 live Logic Chain Viewer service，不自动构建全局逻辑链。
- 8.4 不提供 WebAdmin condition editor。8.4 不提供 WebAdmin API。8.4 不提供 WebAdmin UI。不提供 raw JSON / NBT path 编辑器。
- 8.4 不做具体任务/关卡，不做 GameController / MissionSystem / PhaseController。
- 8.4 不新增 MCP tool。8.4 不跑 MCP scenario。8.4 不生成截图。8.4 不启动 Minecraft。

## 8.5 WebAdmin Condition Editor

8.5 在 8.0-8.4 ConditionEngine 能力之上加入 WebAdmin 条件组编辑器 MVP。它用于创建、保存、校验和模拟评估 Condition Group，但不让条件组实际影响任何设备、监听器、区域、Action 或 itemSubmit。

8.5 已提供：

- Condition Type Catalog：`GET /api/webadmin/condition-types`，只读展示已注册 condition type、中文显示名、中文描述、中文字段名、字段类型和 operator / enum 选项。
- World-scoped Condition Group store：`<world-save-root>/tzz/webadmin/condition_groups.json`。
- Condition Group API：list / detail / create / update / delete / validate / preview。
- WebAdmin UI：`#/condition-groups` 列表与目录，`#/condition-groups/{id}` 详情、结构化节点编辑、校验结果和模拟评估。
- 写入安全：permission、CSRF / same-origin、edit lock、expectedFingerprint、`WebAdminWriteResult`、audit、realtime event。
- Preview MVP：支持基础 context、player snapshot 和手动输入的 GLOBAL / PLAYER state variable snapshot；不会读取 live world、live state store 或 runtime service。

8.5 约束：

- 8.5 仍不接入 runtime：不把 condition group 挂到 VBD、SignalListener、RegionController、ActionRelay、Action、itemSubmit。
- 8.5 不读取 live world、live player list、live inventory/container、live RegionController、live SignalBridge、live Logic Chain Viewer service。
- 8.5 不做 GameController / MissionSystem / PhaseController，不做具体任务/关卡。
- 8.5 不提供 raw JSON editor 作为主要入口，不做任意 NBT path 或通用脚本表达式。
- 8.5 不新增 MCP tool。8.5 不跑 MCP scenario。8.5 不生成截图。8.5 不启动 Minecraft。

## 8.6 Runtime Integration I

8.6 在 8.5 条件组编辑器基础上，第一次把 Condition Group 接入低层运行时触发源。本阶段只做可选外层 gate：未配置 `conditionGroupId` 时不读取 `condition_groups.json`、不创建 `ConditionEvaluationContext`、不 evaluate，直接保持旧逻辑；配置后先由 `ConditionGateService` 判断，false 时阻断旧副作用，true 时进入原流程。

8.6 已接入范围：

- VBD redstone / BlockState gate。
- VBD interaction gate。
- VBD itemSubmit gate，位于 requirement 评估和 consume 前。
- VBD container open / close / change gate；Inventory 容器的 open / close gate 可使用 container snapshot，非 Inventory 或无法解析目标时不显示且后端拒绝 container 条件；container change gate 覆盖直接 change channel 与 itemConditions emit 路径。
- `ConditionRuntimeContextBuilder` 使用 condition-safe snapshot：`ConditionItemStackSnapshot`、`ConditionInventorySnapshot`、`ConditionContainerSnapshot`。
- `ConditionGroupCompatibilityService` 根据 `VBD_REDSTONE`、`VBD_BLOCKSTATE`、`VBD_INTERACTION`、`ITEM_SUBMIT`、`CONTAINER_OPEN`、`CONTAINER_CLOSE`、`CONTAINER_CHANGE` profile 递归过滤 compatible groups；container open / close 会结合 `targetId` 对应绑定方块是否可提供 Inventory snapshot 动态过滤。
- WebAdmin 只读 API：`GET /api/webadmin/condition-groups/available?targetType=<targetType>&targetId=<optional>`。
- VBD 原生触发配置 modal 中的最小 condition group picker，文案明确“未配置条件组 = 不拦截，保持旧逻辑”。

8.6 约束：

- 后端保存时二次执行 compatibility 校验，不能只靠前端隐藏不兼容条件组。
- condition false / missing group / invalid group / incompatible group / evaluation exception 均安全失败，并返回中文 failureReason。
- itemSubmit 条件失败时不 consume；interaction 条件失败时不触发 success channel；container 条件失败时不 emit。
- 不接入 SignalListener condition gate、ActionRelay condition gate、RegionController enter / exit / stay condition gate、Action 单条 action condition gate。
- 不做 GameController / MissionSystem / PhaseController，不做具体任务 / 关卡，不提供 raw JSON editor、任意 NBT path 或通用脚本表达式。

## 8.7 Receiver-side Runtime Gates

8.7 将 Condition Group gate 接到接收端 / 执行端，仍保持“未配置 conditionGroupId = 保持旧逻辑，不拦截”的 Optional Gate 原则。

8.7 已接入范围：

- SignalListener gate：收到 signal 且旧逻辑准备执行 action list 时，先判断可选 `conditionGroupId`。
- ActionRelay gate：非手动 signal 触发且旧逻辑准备执行 action list 时，先判断可选 `conditionGroupId`。
- RegionController enter / exit / stay gate：分别使用 `enterConditionGroupId`、`exitConditionGroupId`、`stayConditionGroupId`。
- WebAdmin picker：SignalListener、ActionRelay、RegionController 配置入口只列出 compatible groups，保存仍由后端二次校验。

8.7 约束：

- gate false 不执行 action list，不 emit 下游 signal，不执行 command/message/sound。
- SignalListener gate false 不更新时间冷却。
- ActionRelay 手动触发保持旧语义。
- RegionController gate false 不阻断 inside / outside tracking；stay gate false 后仍推进 stay interval。
- 不做 SignalReceiver gate、单条 Action gate、GameController / MissionSystem / PhaseController。

## 8.8 Condition Runtime Debugger / Doctor / Simulation

8.8 不继续扩 runtime gate，只让 8.6 / 8.7 gate 可观察、可诊断、可复现。

8.8 已实现方向：

- Runtime history：`ConditionGateHistory` 记录 configured gate 的 allowed / blocked / error，最大 200 条，内存环形缓冲。
- Debug detail：WebAdmin `#/condition-debugger` 展示 target、condition group、result、failureReason、context summary、debug tree。
- Replay 只读：`POST /api/webadmin/condition-gates/history/{id}/replay` 使用历史 snapshot，不写 store、不 emit signal、不执行 action、不读取 live world / player / inventory / region / SignalBridge。
- Doctor 增强：检查 missing / disabled / invalid / incompatible gate binding，覆盖 context_player、container、inventory、signal history、logic chain snapshot 不兼容和 always_false warning。
- WebAdmin 新入口：sidebar “条件调试”，route 为 `#/condition-debugger`。
- 现有页面最近 gate 状态：VBD、SignalListener、ActionRelay、RegionController 页面显示最近一次条件判断和 debug detail 链接。

8.8 约束：

- history 是进程内调试缓冲，不持久化，不写设备 JSON。
- replay 如果 condition group changed，会提示 fingerprint mismatch 并使用历史快照评估；如果 group deleted，会中文 safe failure。
- Doctor 不自动修改配置，不自动清空 conditionGroupId，不把未配置 gate 视为错误。
- 仍不做 SignalReceiver gate、单条 Action gate、GameController / MissionSystem / PhaseController、具体任务 / 关卡、raw JSON editor、任意 NBT path、通用脚本表达式、MCP scenario、启动 Minecraft 或截图矩阵。

## 8.9 Single Action Runtime Gate

8.9 在 8.7 的整组 action list gate 与 8.8 debugger / Doctor / replay 基础上，补齐单条 Action 级别的可选 condition gate。

8.9 已实现方向：

- `ActionConfig.conditionGroupId`：旧 action JSON 未配置时为空，保持兼容。
- SignalListener action gate：整组 listener gate 通过后，在单条 action 执行前判断。
- ActionRelay action gate：runtime signal 触发时先执行 relay 整组 gate，再判断单条 action gate；ActionRelay 手动测试绕过所有 runtime gate，包括单条 action gate。
- RegionController action gate：enter / exit / stay 整组 gate 通过后，分别对对应 action list 的每条 action 判断。
- action gate false：只 skip current action and continue；signal action 被阻断时不 emit downstream signal。
- available list / compatibility：新增 `SIGNAL_LISTENER_ACTION`、`ACTION_RELAY_ACTION`、`REGION_ENTER_ACTION`、`REGION_EXIT_ACTION`、`REGION_STAY_ACTION`，前端 picker 只显示 compatible groups，后端保存二次拒绝 incompatible binding。
- WebAdmin action editor：SignalListener、ActionRelay、RegionController action add/edit 入口显示单条条件组 picker；不兼容当前值不会被静默清空。
- Debugger / replay / Doctor：action gate history 标记 `gateLevel=ACTION`，显示 parent target、actionIndex、actionType；replay 只读；Doctor 扫描 missing / disabled / invalid / incompatible action condition group。

8.9 约束：

- 未配置 action condition 时不读取 condition group store、不构造 EvaluationContext、不 evaluate、不记录 history，保持旧 action 执行语义。
- parent/list-level gate false 时不 evaluate 单条 action gate。
- 不改变 SignalListener cooldown、ActionRelay lastRun / manual test、Region tracking / stay interval。
- 不把 action gate 放进 ActionEngine，不改 command / message / sound / signal action 类型语义。
- 仍不做 SignalReceiver gate、Signal Join / Barrier / Aggregator、GameController / MissionSystem / PhaseController、具体任务 / 关卡、failure policy、stop-list policy、fallback action、raw JSON editor、任意 NBT path、通用脚本表达式、MCP scenario、启动 Minecraft 或截图矩阵。

## 8.10 Signal Join / Barrier / Aggregator

8.10 在 SignalBridge 上补齐多事件汇合能力。Signal Join 是 passive observer：它观察已被 SignalBridge accepted 的 input signal，不阻断原始 signal，不修改原始 payload；满足条件后通过 `SignalBridgeServer.emit` 发出 output signal，因此 output 会自然进入 history、receiver、ActionRelay、SignalListener 和递归保护。

8.10 已实现方向：

- world-scoped store：`<world-save-root>/tzz/webadmin/signal_joins.json`。
- runtime state 内存态：pending / latched state 不持久化，服务器重启清空。
- mode：`ALL`、`ANY_N`、`COUNT`。
- scopeMode：`GLOBAL`、`PLAYER`；PLAYER 缺玩家上下文时 no-op 并记录中文 diagnostic。
- resetPolicy：`RESET_AFTER_EMIT`、`LATCH_UNTIL_MANUAL_RESET`。
- timeoutTicks：lazy timeout，不启动 Scheduler，不做后台 tick 扫描。
- WebAdmin API/UI：`#/signal-joins`、`#/signal-joins/{id}` 和 `/api/webadmin/signal-joins` CRUD/status/reset。
- Logic Chain Viewer：Join 同时显示为 input channel consumer 和 output channel producer。
- Doctor：诊断 invalid Join、duplicate input、self-output、threshold invalid、PLAYER scope 风险、timeout 风险和 Join cycle 风险。

8.10 约束：

- 不做 GameController / MissionSystem / PhaseController。
- 不做具体任务 / 关卡。
- 不做 Scheduler / Delay / Timer。
- 不做 Controlled State Actions。
- 不做完整 Logic Chain Editor / Scratch editor。
- 不做 SignalReceiver gate。
- 不做 failure action / failure channel。
- 不做 per-input condition group。
- 不做 raw JSON editor。
- 不改写 8.6 / 8.7 / 8.9 gate 语义。

## 8.12 Scheduler / Delay / Timer

8.12 在 Signal / Action / State / Join 系统之上补齐通用时间轴能力。Scheduler / Timer 不是 signal-only 组件；它可以由 ActionEngine 的 `timer_start` action 启动，也可以由 WebAdmin 手动启动 / 取消 / 重置运行态。`outputChannel` 可选，只是兼容 SignalBridge 的输出方式；Timer 自身的直接动作入口是 `onTickActions` 和 `onCompleteActions`。

8.12 已实现方向：

- world-scoped store：`<world-save-root>/tzz/webadmin/timers.json`。
- runtime state 内存态：active timer instance 不持久化，服务器停止后清空。
- mode：`DELAY`、`COUNTDOWN`、`REPEAT`。
- scopeMode：`GLOBAL`、`PLAYER`；PLAYER 缺玩家上下文时 fail closed 并返回中文原因。
- startPolicy：`RESTART`、`IGNORE_IF_RUNNING`、`FAIL_IF_RUNNING`。
- ActionEngine action：`timer_start`、`timer_cancel`。
- Timer action list：`onStartActions`、`onTickActions`、`onCompleteActions`、`onCancelActions`，并保留 single action gate。
- WebAdmin API/UI：`#/timers`、`#/timers/{id}` 和 `/api/webadmin/timers` CRUD/status/start/cancel/reset。
- Doctor：诊断 invalid Timer、无输出/动作、REPEAT 高频、无限重复缺取消路径、缺 timerId、引用不存在/停用 Timer、PLAYER context 缺失。
- Logic Chain Viewer：只做最小只读接入，有 outputChannel 的 Timer 显示为 producer/source。

8.12 约束：

- 不做 GameController / MissionSystem / PhaseController。
- 不做具体任务 / 关卡。
- 不做完整 Logic Chain Editor / Scratch editor。
- 不做 StateVariable 新 scope。
- 不做 Scheduler 持久恢复。
- 不做 cron / calendar。
- 不做多服务器调度。
- 不做 version rollback。
- 不做 raw JSON editor。
- 不做任意 NBT path 或脚本表达式。
- 不跑 MCP scenario。
- 不启动 Minecraft。
- 不生成截图矩阵。

## 8.13 Logic Chain Viewer 增强

8.13 增强 WebAdmin Logic Chain Viewer 的只读 runtime graph，不新增 runtime 行为。Viewer 现在能从 channel/root 出发显示更多真实运行结构：Signal Join、Timer、StateAction、StateVariable、Condition gate、single Action gate，以及上游 / 下游 / 双向 / 相关节点浏览。8.13 返修后，Logic Chain 不再等于单个 channel；root channel 只是当前焦点，真实图谱按 Signal / Join / Timer / Action / State / Gate 关系组成的 logical component 展开。

8.13 已实现方向：

- graph model：新增 `signal_join`、`timer`、`state_action`、`timer_action`、`condition_gate`、`action_gate`、`state_variable` 节点。
- edge model：新增 `join_input`、`join_output`、`timer_outputs_channel`、`action_starts_timer`、`action_cancels_timer`、`state_writes`、`gate_guards` 等关键关系。
- GraphModel V2：新增真实节点 vs 引用卡模型，用 `nodeKind`、`primaryNodeId`、`referenceReason` 区分 primary node 和 reference card，支持节点去重、边合并、下游合并，并使用 Join 专用 lane 布局。
- component-aware traversal：`rootChannel` 不再裁剪图谱；从同一 component 内任意 Join input 或 output 进入都应看到同一组核心频道、Join、Timer、Action 和消费者，只改变焦点高亮。
- component safety：强关联纳入同一逻辑组件，弱关联默认折叠；`maxComponentChannels`、`maxGraphNodes`、`maxGraphEdges` 和 `maxDepth` 共同保护大图，并用中文提示截断原因。
- path model：edge 增加 `pathGroupId`、`visualStyle`、`referenceEdge`，用颜色分组、Join 主输入实线、其他输入虚线和灰色引用虚线提升可读性。
- crossing reduction：V2 lane 内按 connected target/source、parent 和 actionIndex 做稳定局部排序，并用 source 右侧 / target 左侧端口与多边 offset 减少非必要交叉。
- display / routing polish：节点标题优先使用 WebAdmin 设备 / 频道 displayName，技术 ID 作为副文本；edge 默认恢复旧版平滑 Bezier 曲线和统一箭头样式，直线只在 source / target centerY 差值 `<= 1px` 时使用，不同高度、reference 和 Join related 边都走平滑曲线；同 source 多线共享一个出口锚点，同 target 多线共享一个入口锚点并只渲染一个 target 箭头，多线分离只放在 Bezier control point；shared trunk / merge point 默认禁用，空 lane 压缩且长链继续向右展开不折回。
- Join：从 input channel 可见 Join consumer；从 output channel 可见 Join source；全部 input channel 在 Join 左侧可见，output channel 和 downstream 在右侧，详情显示 inputPorts、primaryInput、relatedInputs、pending / last result。
- Timer：有 outputChannel 的 Timer 显示为 channel source；timer_start / timer_cancel action 显示为 Timer 引用；详情显示 mode、scope、duration、interval、maxRuns、active count 和 action bucket summary。
- StateAction：`state_variable` action 显示为状态写入节点，能看到 operation、scope、targetMode、key 和静态可解析 StateVariable 链接。
- Gate：list-level condition gate 与 single action gate 分别显示，包含 conditionGroupId、targetType / targetId、recent status、Condition Debugger 和 Doctor 入口。
- UI：增加视图模式筛选、节点类型筛选、增强图例、可关闭节点详情面板、Join 输入摘要、上游展开卡片、引用卡跳转主节点、一阶关联高亮和 no cross-channel long-line mixing 标记；再次点击已选中节点可取消 pinned 高亮，graph card 使用固定高度和固定 title / subtitle / meta 行，长文本省略或 clamp，完整内容在详情面板展示。
- ActionRelay：Logic Chain Viewer 不直接读取 live world / block entity；当前显示 snapshot actionCount 摘要和设备详情入口，后续若要展开 ActionRelay gate 需先进入安全快照。

8.13 语义说明：

- Logic Chain Viewer 不保证全局唯一拓扑排序。
- 同一 channel 下多个 consumers 是并列消费者，不代表严格顺序。
- action list 内部顺序只在同一 owner 内有效。
- Join inputs 无先后顺序。
- Condition gate 只是允许 / 阻断 / 跳过；未来 if / else / else-if / nested branching 是真实控制流，需作为后续能力独立实现。
- 9.x 以后 Timer、StateAction、Message、Title、Sound、Condition、Join 等能力应能作为游戏程序 typed block 直接调用；Signal/channel 不是长期唯一入口。

8.13 约束：

- 不做完整 Logic Chain Editor / Scratch-like editor。
- 不做拖拽编辑。
- 不做 if / else / else-if runtime。
- 不做 GameController / MissionSystem / PhaseController。
- 不做具体任务 / 关卡。
- 不新增 Action type。
- 不新增 Condition type。
- 不修改 SignalBridge / ActionEngine / Timer / Join / StateAction runtime 语义。
- 不做 raw JSON editor。
- 不跑 MCP scenario。
- 不启动 Minecraft。
- 不生成截图矩阵。
- 本返修不 checkpoint，不 commit / push / merge / tag。

## WebAdmin UI 规范

后续类似编辑功能应复用 7.x 已验收交互模式：

- channel 字段使用 dark combobox，可选择已有 channel，也可手动输入新 channel；输入新 channel 不自动创建消费者。
- regionId 使用可搜索 dark selector，不能退回突兀的原生白色 select。
- action list 在详情页显示稳定 summary card，完整列表放在 modal/drawer 中管理，避免 action 数量撑大页面。
- action add form 按类型动态显示字段：signal 只显示频道，command 显示命令/权限/安全提示，message 显示消息，sound 显示音效字段。
- edit lock 不是 toast-only：被别人锁定时按钮应 disabled 或替换为锁状态，当前用户持锁时显示正在编辑 / 到期信息。
- 删除和清空确认要清楚说明对象，通常不要求输入 ID/name。
- 主文案使用中文；技术 ID 可作为副文本保留。
- UI 改动需要小分辨率和 4K 200% scaled 人工确认。7.14 本身不生成截图。

## Local Test MCP Foundation

仓库包含 `tools/tzz-test-mcp` 本地辅助工具箱，用于 WebAdmin / Minecraft dev runtime / TestBridge / report 的受控本地检查。它当前主要作为 Codex 和开发者的辅助工具，不再强制替代用户完整手动验收。

原则：

- 手动测试仍为主，特别是 UI 视觉验收和真实玩法手感。
- MCP 可以辅助启动 dev client、等待 WebAdmin/TestBridge、准备测试区域、执行安全 TestBridge 原子工具、截图和写报告。
- MCP 工具箱包括固定 Gradle preset、本地 WebAdmin browser helpers、loopback/token TestBridge、Minecraft dev runtime start/wait/stop、test world prepare、Minecraft GUI semantic ops、Minecraft client screenshot、WebAdmin responsive matrix、固定 scenario runner 和 cleanup。
- `reports/mcp`、`reports/mcp/screenshots`、responsive reports、scenario reports 都是本地测试输出，不提交。
- MCP 不提供任意 shell、不提供 git mutation、不访问外部 host、不做 OS 鼠标键盘控制、不做 Minecraft GUI 坐标点击。
- TestBridge 仅 loopback/local、需要 token、默认关闭。
- 7.14 stabilization does not generate screenshots and does not run MCP scenarios; MCP remains auxiliary and does not replace user acceptance. MCP screenshots/scenarios are not 8.0 ConditionEngine Core, 8.1 Basic Player / Context Conditions, 8.2 State Variable System, 8.3 Item / Inventory / Container Conditions, or 8.4 Region / Signal / Logic Chain Conditions requirements or new scope.

## 安全边界

- WebAdmin 写入必须走 session、角色权限、CSRF / same-origin、validation、edit lock、expectedFingerprint、`WebAdminWriteResult`、audit 和 realtime。
- Web UI 不直接写业务 JSON，不绕过 store/service/domain 路径。
- 不提交 `logs/`、`reports/mcp`、screenshots、`node_modules`、token、密码、cookie 或 session 文件。
- 不提供 raw JSON / NBT path 编辑。
- ConditionEngine runtime integration 当前已开放 8.6 的 VBD / itemSubmit / container 可选外层 gate、8.7 的 SignalListener / ActionRelay / RegionController 外层 gate，以及 8.9 的 SignalListener / ActionRelay / RegionController 单条 Action gate；8.10 已新增 Signal Join / Barrier / Aggregator 多事件汇合。仍不接入 SignalReceiver gate、failure policy、stop-list policy 或 fallback action。Logic Chain Viewer 仍不是编辑器。GameController / MissionSystem 尚未实现。

## 主要功能

- 手机系统：地图、聊天、任务、图库、呼叫管理员和设置等内置 App。
- AR 头显：提供空间化的应用入口和调试展示能力。
- 地图与区域工具：创建地图标点、规划区域，并同步到客户端地图。
- 任务配置器：配合数据包创建和编辑任务线。
- 封锁卡系统：保存触发条件和命令动作，并在命中实体或方块条件时执行。
- ActionEngine：统一执行命令、消息、音效等动作。
- RegionController：为已有规划区域绑定进入、离开、停留事件动作。
- Signal 设备：支持发射器、接收器和动作继电器，把红石、signal 与 ActionEngine 串联起来。
- WebAdmin：提供默认关闭的轻量 Web 管理入口，支持登录、session、Dashboard、设备管理、Signal 频道、跨频道逻辑链只读查看器、Doctor 诊断、History 历史、用户管理、系统设置、Region / Action 观测，以及 7.x 受控编辑能力。ActionRelay、RegionController、SignalListener 等已具备对应 WebUI 编辑入口；Action 系统聚合页仍以只读观测为主。

## 命令入口

当前主要命令入口已经统一到 `/tzz`：

```text
/tzz map ...
/tzz task ...
/tzz note ...
/tzz sendmsg ...
/tzz regionctl ...
/tzz signal ...
/tzz webadmin ...
```

旧根命令已迁移到 `/tzz` 子命令下；当前代码不再注册旧的 `/map`、`/task`、`/note`、`/sendmsg` 根命令。

## WebAdmin 历史阶段记录（6.0-7.2，仅背景）

下面内容保留 6.x 到早期 7.x 阶段的历史记录。当这些历史段落写“只读”“暂不开放”或“不包含”时，表示当时阶段边界，不代表当前 7.14 能力。当前能力以本 README 顶部的 7.x 编辑层状态、`docs/WEBADMIN_EDITING_STABILIZATION_7_14_CURRENT_CONTEXT.md` 和 `docs/WEBADMIN_EDITING_CAPABILITY_MATRIX_7_14.md` 为准。

### 7.2 WebAdmin Device Basic Config Editing

7.2 在 7.0 / 7.1 的安全写入链路基础上，开放第一批低风险但会影响游戏逻辑的设备基础配置编辑。当前只允许编辑：

- `enabled`：设备启用 / 禁用状态。
- `channel`：设备主频道 / primary channel。

这些字段会影响当前世界中的设备触发和 Signal 分发，因此所有写入都必须经过 WebAdmin session、`EDITOR` / `OWNER` 权限、CSRF / 同源校验、`device_basic_config` 编辑锁、`expectedFingerprint` 冲突检测、输入校验、结构化 audit 和 realtime 事件发布。

新增 API：

```text
GET /api/webadmin/device-basic-config/{deviceId}
PATCH /api/webadmin/device-basic-config/{deviceId}
```

`GET` 对已登录用户只读开放，返回当前 enabled、主 channel、是否支持编辑、当前 fingerprint 和锁状态摘要。`PATCH` 只允许 `EDITOR` / `OWNER`，必须携带有效 lockId 与 expectedFingerprint；冲突时返回 `conflict_detected`，不会覆盖服务器上的新配置。

7.2 不开放 `interactChannel`、success/fail/off channel、cooldown、pulseTicks、redstone mode、BlockState condition、interactionItem、itemSubmit、matcher、consume、action、command action、region bounds、用户或系统设置编辑。写入通过 `SignalDeviceStore` / domain service 路径执行，不允许前端直接改 JSON，并且必须保留 itemSubmit、interactionItem、container、itemConditions、redstone/condition 等既有字段。

更多说明见 `docs/WEBADMIN_DEVICE_BASIC_CONFIG_7_2.md`，回归测试见 `docs/REGRESSION_TEST_7_2.md`。

### 7.0 WebAdmin Editing Foundation

7.0 是 WebAdmin 配置编辑基础 / 最小安全写入闭环。本阶段只开放低风险 WebAdmin 设备显示元数据编辑：`displayName`、`note`、`iconKey`。这些字段只影响 WebAdmin 展示，不改变 Minecraft 游戏逻辑，不改变 SignalBridge、SignalDevice、VirtualBlockDevice、itemSubmit、RegionController 或 ActionEngine 的运行语义。

新增世界级 WebAdmin 元数据文件：

```text
<world-save-root>/tzz/webadmin/web_admin_device_metadata.json
```

新增 API：

```text
GET /api/webadmin/device-metadata/{deviceId}
PATCH /api/webadmin/device-metadata/{deviceId}
```

`GET` 要求登录，`VIEWER`、`TESTER`、`EDITOR`、`OWNER` 均可读取安全 DTO。`PATCH` 要求有效 WebAdmin session、`EDITOR` 或 `OWNER` 权限、CSRF / 同源写请求安全校验、JSON content、输入 validation、审计记录和 realtime 事件发布。写入结果统一使用 `WebAdminWriteResult`，校验失败返回 `validation_failed`，权限不足返回 `permission_denied`，无变化返回 `no_change`。

设备详情页新增“WebAdmin 显示信息”卡片。`EDITOR` / `OWNER` 可以编辑显示名称、备注和预设图标；`VIEWER` / `TESTER` 只能查看并看到权限说明。保存成功后发布轻量 `config_changed`、`device_config_changed` 和 `write_audit_appended` 事件，前端静默更新，不跳页、不丢滚动位置、不影响详情页返回上下文。

7.0 明确不开放 `enabled`、`channel`、`interactChannel`、success/fail channel、cooldown、redstone mode、interactionItem、itemSubmit、matcher、consume、action、command action、region bounds、用户或系统设置编辑。

更多说明见 `docs/WEBADMIN_EDITING_FOUNDATION_7_0.md`，回归测试见 `docs/REGRESSION_TEST_7_0.md`。

### 6.10 WebAdmin Write Foundation Stabilization

6.10 是 7.0 WebAdmin 配置编辑前的安全闸门，定位为写入前置稳定化 / 编辑阶段前总审查。本阶段不开放真实配置编辑，不新增公开可调用的配置写入 API，不写 JSON，不改变 5.x 已封版底层工具链语义，也不改变 6.2～6.8 已完成的只读观察层和 realtime 行为。

本阶段重点审查并补强 6.9 写入前置体系：权限矩阵、统一写结果模型、validation error 脱敏、CSRF / 同源写请求安全 helper、结构化审计模型、mutation service 规范、realtime 写入事件类型、前端只读边界和敏感信息保护。`stabilizationGuardTest` 增加 6.10 guard，用于确认 VIEWER / TESTER / EDITOR / OWNER 权限矩阵、CSRF token 校验、审计脱敏、写结果 code、realtime 写事件类型和前端无真实写入口。

6.10 结论用于判断是否可以进入 7.0。推荐 7.0 第一批只开放低风险编辑对象，例如设备名称 / 备注 / iconKey、基础 enabled 状态和基础 channel 字段；itemSubmit、action command、region bounds、用户 / 系统安全设置等高风险编辑应继续放到后续独立阶段。

更多说明见 `docs/WEBADMIN_WRITE_STABILIZATION_6_10.md`，回归测试见 `docs/REGRESSION_TEST_6_10.md`。

### 6.9 WebAdmin Write Permission / Audit / Service API Foundation

6.9 是 WebAdmin 配置编辑前的安全地基阶段，不开放真实配置编辑、不新增公开写 API、不写 JSON，也不改变 5.x 已封版底层工具链语义。

本阶段新增写操作统一结果模型、校验错误模型、权限矩阵、CSRF / 同源写请求安全 helper、结构化写操作审计模型、未来 mutation service 接口规范，以及写入相关 realtime 事件类型规范。新增的能力用于后续 7.0 WebAdmin 配置编辑复用，当前只作为基础设施和测试护栏存在。

新增只读能力接口：

```text
GET /api/webadmin/write/capabilities
```

该接口要求有效 WebAdmin session，只返回当前角色的未来写入能力摘要、CSRF 要求和 token，不执行任何写操作。`VIEWER` 仅只读，`TESTER` 预留测试 / dry-run，`EDITOR` 预留普通配置编辑，`OWNER` 预留用户、系统设置和危险操作。

更多说明见 `docs/WEBADMIN_WRITE_FOUNDATION_6_9.md`，回归清单见 `docs/REGRESSION_TEST_6_9.md`。

### 6.8 WebAdmin Realtime Sync Foundation

6.8 是 WebAdmin 实时同步基础阶段，不新增编辑能力、不新增写 API、不做配置写入。当前实现采用认证后的 Server-Sent Events / Event Stream：`GET /api/realtime/events`。

服务端新增轻量 realtime event bus。Signal history 追加时会发布 `signal_emitted` / `history_appended` 事件；WebAdmin 连接建立、断开和 heartbeat 也会发出轻量事件。事件只包含 channel、sourceType、summary、routeTarget 和少量 payload，不推送完整 devices/history/doctor DTO，不包含 password、hash、salt、session token 或 cookie。

前端登录后建立 realtime 连接，topbar 显示“实时同步”状态和最后事件时间。收到事件后按当前 hash route 过滤，并用节流后的当前页面静默局部 refetch 处理相关变化；浏览器标签页在后台时只记录 dirty route，回到前台后再刷新当前相关页面。不做全站轮询、不全页 reload，并保留滚动位置、筛选条件和折叠状态。

更多说明见 `docs/WEBADMIN_REALTIME_SYNC_6_8.md`，完整人工回归清单见 `docs/REGRESSION_TEST_6_8.md`。

### 6.7 WebAdmin Readonly Stabilization

6.7 是 WebAdmin 只读层稳定化 / 前端架构整理版，不新增业务页面、不新增写 API、不接入 WebSocket。

本阶段整理 WebAdmin 前端资源边界：`WebAdminServer` 继续负责 HTTP request dispatch、auth/session 和 API route dispatch，HTML / CSS / JS 静态资源集中到 `WebAdminFrontendAssets`。页面路径、登录、session、只读 API 和 world-save scoped WebAdmin 存储目录均保持兼容。

6.7 同时增加 WebAdmin readonly guard，纳入 `stabilizationGuardTest` / `clean build`，覆盖 app shell / CSS / JS assets 非空、Dashboard / Devices / Signals / Doctor / History / Users / Settings / Regions / Actions 路由存在、时间格式化 helper、详情页上下文返回 helper、中文空状态和只读提示等基础护栏。

更多说明见 `docs/WEBADMIN_READONLY_STABILIZATION_6_7.md`，完整人工回归清单见 `docs/REGRESSION_TEST_6_7.md`。

### 6.6 WebAdmin Region + Action

6.6 在 6.5 用户管理 / 系统设置只读页面基础上接入 Region 管理和 Action 系统只读页面：

```text
/app#/regions
/app#/regions/<regionId>
/app#/actions
/app#/actions/<actionId>
```

Region 管理页用于查看 RegionController 区域、世界、坐标边界、目标过滤、进入 / 离开 / 停留动作数量、绑定频道、当前玩家数量和 Doctor 状态。Region 详情页展示 bounds、目标过滤、事件动作摘要、绑定频道、当前玩家 / 最近事件和诊断摘要。

Action 系统页用于查看 ActionEngine 动作、动作类型、归属对象、关联 channel、引用次数、执行摘要和 Doctor 状态。Action 详情页展示动作基础信息、配置摘要、引用来源、最近执行记录和诊断摘要。

Region / Action 页面与 Signal、Doctor、History、设备详情之间支持只读跨页面跳转。详情页返回按钮会优先回到进入前的上下文页面；直接打开详情 URL 时会回退到对应模块列表页。

6.6 仍然只读：不提供新增、编辑、删除、执行 action、测试 action、修改 region bounds、修改 target filter、修改 enter / exit / stay actions、配置写入、WebSocket 或任何写 API。

### 6.5 WebAdmin Users + Settings

6.5 在 6.4 Doctor / History 只读观测页面基础上接入用户管理和系统设置只读页面：

```text
/app#/users
/app#/settings
```

用户管理页用于查看 WebAdmin 用户、角色、启用状态、在线 / session 摘要、创建时间和最近登录时间。该页面只对 `OWNER` 开放，不返回 password hash、salt、session token、cookie 或明文密码。

系统设置页用于查看 WebAdmin 服务运行状态、监听地址、端口、accessMode、世界级存储目录、安全配置摘要、审计日志状态和系统信息。非 `OWNER` 用户可以查看基础运行状态，但敏感存储路径会隐藏。

6.5 仍然只读：不提供创建用户、删除用户、禁用 / 启用用户、重置密码、修改角色、踢出 session、修改 host / port / accessMode、保存配置、WebSocket 或任何写 API。写操作仍通过 `/tzz webadmin` 命令和后续专门阶段谨慎开放。

### 6.4 WebAdmin Doctor + History

6.4 在 6.3 Signal 频道只读页面基础上接入全局 Doctor 诊断页和 History 历史时间线：

```text
/app#/doctor
/app#/history
```

Doctor 页面读取 6.1 的只读诊断 API，展示错误 / 警告 / 信息数量、受影响设备 / 频道、问题搜索、严重级别筛选、对象类型筛选和跳转目标筛选。问题列表以中文显示标题、影响、建议和诊断代码，并可跳转到相关设备、频道或历史视图。

History 页面读取已有 Signal history 只读 API，展示 Signal 事件时间线，支持按关键词、channel、sourceType、result、时间范围和排序筛选。时间显示统一为 `YYYY-MM-DD HH:mm:ss`，不显示 ISO 原始字符串。

6.4 仍然只读：不提供修复按钮、清除问题、删除历史、导出历史、signal emit、重放事件、配置写入、WebSocket 或任何设备 / channel / listener / action / region 编辑能力。

### 6.3 WebAdmin Signal Channels

6.3 在 6.2 Dashboard / 设备管理只读页面基础上接入 Signal 频道管理和频道详情逻辑链只读视图：

```text
/app#/signals
/app#/signals/<channel>
```

Signal 管理页展示频道总数、消费者数量、最近触发、Doctor 状态，并提供频道名搜索、消费者筛选、状态筛选和排序。频道详情页展示频道基础信息、最近事件、诊断摘要，以及“触发源 → 频道 → 消费者 → 动作 / 下游影响”的横向逻辑链雏形。

6.3 继续保持只读边界：不新增 channel，不编辑 / 删除 channel，不修改 listener、receiver、action_relay、device 或 action，不执行 signal emit，不提供配置写入，不接入 WebSocket。设备详情中的关联 channel 可以跳转到频道详情页；Dashboard 也提供进入 Signal 管理的入口。

### 6.2 WebAdmin Dashboard + Devices

6.2 将 6.1 的只读 Service / DTO / API 接入 WebAdmin 前端，提供第一批正式只读页面：

```text
/app#/dashboard
/app#/devices
/app#/devices/<deviceId>
```

登录后默认进入 Dashboard，总览服务器状态、设备数量、Signal channel 数量、最近 Signal 历史、Doctor 摘要、Region / Action 数量。设备管理页提供只读列表、搜索和筛选；设备详情页展示设备身份、关联 channel、debug checks、最近事件、Doctor 问题和配置摘要。

6.2 同时完成 WebAdmin 视觉与可读性整理：统一深色控制台风格、2D inline SVG 图标系统、固定 sidebar、中文筛选器标签、设备类型 / 状态 / Doctor badge 中文化、Debug 中文化、Doctor / Debug 状态一致性、配置摘要收敛和原始字段默认折叠。资源侧修复了 lang JSON、翻译 key 和关键模型 / 贴图加载问题。

本阶段仍然只读：不提供新增设备、编辑设备、删除设备、修改 channel、enable / disable 操作、配置写入或 WebSocket。前端只调用 6.1 只读 API，不扫描世界、不强制加载区块、不触发游戏逻辑。更多说明见 `docs/web_admin_dashboard_devices.md`。

### 6.1 WebAdmin Readonly Services

6.1 建立 WebAdmin 的只读 Service / DTO / API 数据层，面向后续 Dashboard、设备页、Signal 频道页、Doctor / History 页提供稳定后端结构。本阶段不做完整 Web 页面、不做配置编辑、不做 WebSocket，也不改变 5.x 已封版功能语义。

新增只读 API：

```text
GET /api/devices
GET /api/devices/{id}
GET /api/devices/{id}/debug
GET /api/signals/channels
GET /api/signals/channels/{channel}
GET /api/signals/history
GET /api/doctor
GET /api/regions
GET /api/regions/{id}
GET /api/actions
GET /api/actions/{id}
```

所有 6.1 API 都要求 WebAdmin 登录，`VIEWER`、`TESTER`、`EDITOR`、`OWNER` 均可访问这些只读接口。接口只通过 service / DTO 层读取现有系统状态，不直接读写业务 JSON，不扫描世界，不强制加载区块，不触发游戏逻辑，也不修改设备、频道、区域或动作配置。

从 6.1 开始，WebAdmin 持久化文件按当前世界 / 当前存档隔离，目录为 `<world-save-root>/tzz/webadmin/`。该目录包含 `web_admin_config.json`、`web_admin_users.json` 和 `web_admin_audit.log`。WebAdmin 不再读取全局 `config/tzz` 下的旧文件；如需迁移，管理员需要手动复制到对应世界的 `tzz/webadmin/` 目录。

更多说明见 `docs/web_admin_readonly_services.md`。

### 6.0 WebAdmin Foundation

6.0 WebAdmin Foundation 是 WebAdmin 后端地基与登录闭环，不是完整 WebAdmin Dashboard。本阶段默认关闭，不会自动公网开放，不改变 5.x SignalBridge / SignalDevice / `virtual_block_device` / ItemStackMatcher / itemSubmit / Doctor / debug 等既有逻辑。

当前能力：

- WebAdmin 配置文件：`<world-save-root>/tzz/webadmin/web_admin_config.json`（6.1 起按世界 / 存档隔离）。
- 默认 `enabled=false`、`host=127.0.0.1`、`port=18080`、`accessMode=LOCAL_ONLY`。
- 支持访问模式：`LOCAL_ONLY`、`LAN_DEV`、`MULTIPLAYER_DEV`。
- `LAN_DEV` / `MULTIPLAYER_DEV` 必须显式配置，启动日志和 `/tzz webadmin status` 会显示安全提示。
- WebAdmin 用户文件：`<world-save-root>/tzz/webadmin/web_admin_users.json`（6.1 起每个世界独立）。
- 用户密码使用 JDK 原生 `PBKDF2WithHmacSHA256` 保存，不保存明文。
- 初始密码由服务端随机生成，只在 `/tzz webadmin user create` 或 `resetPassword` 时显示一次。
- 登录成功后写入短期 `TZZ_WEBADMIN_SESSION` HttpOnly cookie。
- 浏览器访问 `http://host:port` 会打开登录页，登录后进入基础状态页。
- 已实现 API：`POST /api/auth/login`、`POST /api/auth/logout`、`GET /api/auth/me`、`GET /api/status`。
- `/api/status` 返回 WebAdmin、Minecraft server、当前用户和基础版本状态。
- 服务器停止时释放 WebAdmin HTTP 端口。

WebAdmin 命令：

```text
/tzz webadmin status
/tzz webadmin user list
/tzz webadmin user create <username> <role>
/tzz webadmin user disable <username>
/tzz webadmin user enable <username>
/tzz webadmin user resetPassword <username>
```

权限边界：

- 控制台允许执行 `/tzz webadmin`。
- OP / 创造级管理员允许执行。
- 普通玩家禁止管理 WebAdmin 用户。
- 普通玩家不能通过游戏内命令创建 WebAdmin 账号。

本阶段暂不包含：

- 设备列表、Signal 频道页、逻辑链视图。
- Doctor 完整页、History 完整页。
- WebSocket、实时同步、配置编辑。
- 用户管理 Web 页面完整 CRUD。
- 区域、动作系统、节点编辑或多人协作锁。

后续 Web UI 原则保持 5.15 稳定化结论：

- Web UI 不直接读写 JSON。
- 命令、游戏内工具、Web UI 应共用服务层。
- 未来需要 DTO、权限、审计和 WebSocket 实时同步。
- 游戏内工具负责轻量绑定、选择和定位。
- Web Admin UI 负责全局逻辑视图、模块化卡片、实时调试和配置编辑。

## SignalBridge

### 5.15 Stabilization Foundation / GUI 前置整理版

Version marker: `v1.17.0-stabilization-foundation`.

5.15 是底层工具链稳定化 / GUI 前置整理版，不是新玩法功能版本。本阶段围绕 5.1 到 5.14 已完成的 SignalBridge、SignalDevice、`virtual_block_device`、ItemStackMatcher、consume 和 itemSubmit 链路做审查、测试护栏、诊断输出和 Web Admin UI 前置设计整理。

稳定化审查报告：

- `docs/STABILIZATION_AUDIT_5_15.md`
- `docs/STABILIZATION_AUDIT_5_15_ROUND2.md`
- `docs/STABILIZATION_AUDIT_5_15_ROUND3.md`
- `docs/STABILIZATION_AUDIT_5_15_ROUND4.md`
- `docs/STABILIZATION_AUDIT_5_15_ROUND5.md`
- `docs/STABILIZATION_AUDIT_5_15_FINAL.md`

自动化护栏：

- 新增 `stabilizationGuardTest`，已挂到 Gradle `check` / `build`。
- 执行 `./gradlew.bat clean build` 会自动运行稳定化护栏测试。
- 覆盖 `SignalDeviceData` 字段保留、旧 JSON 样本兼容、`ConsumePlan` / `ConsumePlanner`、`ItemSubmitEvaluator`、`InteractionDecisionEvaluator`、displayName 和 diagnostic DTO。
- 防止 interactionItem / itemSubmit / consume / cooldown / `require_item_match` 组合路径再次出现字段丢失、部分消耗、冷却绕锁等回归。

逻辑稳定化：

- 新增并接入 `ConsumePlan` / `ConsumePlanner`，消耗采用两阶段 plan / apply。
- 新增并接入 `ItemSubmitEvaluator` / `ItemSubmitEvaluationResult` / `ItemSubmitInventoryAdapter`，生产 itemSubmit 路径使用统一 evaluator。
- 新增并接入 `InteractionDecisionEvaluator` / `InteractionDecision`，明确区分原版交互放行、消耗执行和 signal / message / sound / history 等副作用。
- `cooldown` 不解除 `require_item_match` 锁，不跳过成功消耗，只抑制 signal / message / sound / 额外动画 / 高频 history 等副作用。
- `itemSubmit` 原子消耗保持两阶段：先完整模拟 consume plan，再统一 apply；任一 requirement 不足时不消耗任何物品。
- `interactionItem` 与 `itemSubmit` 保持互斥匹配模式：多物品提交启用时不再执行单物品 matcher / consume。

debug / doctor 结构化诊断：

- 新增 `DiagnosticSeverity`、`DiagnosticIssue`、`DeviceDiagnostic`、`InteractionItemDiagnostic`、`ItemSubmitDiagnostic`、`VirtualBlockDeviceDiagnosticService`。
- `/tzz signal device debug <device>` 会输出结构化诊断。
- `/tzz signal doctor` 增加设备层诊断摘要。
- 诊断输出已中文化、分组化，并保留机器可读诊断代码用于未来 Web UI / 高级排查。

GUI / Web Admin UI 前置原则：

- 未来 Web UI 不应直接读写 JSON。
- 命令、游戏内工具和 Web UI 应共用服务层，所有写操作走服务端统一服务。
- 后续需要 service / DTO / internal event bus / WebSocket 实时同步。
- 游戏内工具负责轻量初始化、绑定、选择和定位。
- Web Admin UI 负责全局逻辑视图、模块化卡片、实时调试、配置编辑、history 和 doctor。
- Web UI 最终必须覆盖所有可配置功能，不是命令系统的缩水版。

### 5.14 Consume Strategies / Multi-Item Submission MVP

Version marker: `v1.16.0-consume-submit`.

5.14 extends `virtual_block_device` right-click item matching with optional consume strategies and optional multi-item submission.

- `interactionItem` consume can use `matched_source`, `main_hand`, `off_hand`, or `inventory`.
- `main_hand`, `off_hand`, and `inventory_contains` can consume matched items when explicitly enabled.
- `armor_head`, `armor_chest`, `armor_legs`, `armor_feet`, and `armor_any` still reject consume; equipment / armor consume is not implemented.
- Inventory consume only reads and consumes the triggering player's main inventory / hotbar.
- `inventoryConsumeOrder` supports `hotbar_first` and `main_inventory_first`.
- Consume is atomic: the mod checks every required item before decrementing any stack.
- `itemSubmit` is disabled by default and must be enabled by an admin.
- `interactionItem` matcher and `itemSubmit` are mutually exclusive matching modes.
- Enabling `itemSubmit` automatically disables the single-item `interactionItem` matcher while preserving success/fail feedback configuration.
- `itemSubmit` requirements are captured from the admin's main hand and checked against the triggering player's main inventory / hotbar.
- All enabled `itemSubmit` requirements must match for submit success.
- When `itemSubmit` is enabled, submit requirements decide success and the single-item `interactionItem` matcher / consume path is not evaluated.
- `itemSubmit consume` is optional and atomically consumes all requirement items when enabled.
- `ignore` count mode does not take a count parameter and means the matcher does not check count; inventory matching still requires at least one matching stack.
- `require_item_match` remains a lock. In `itemSubmit` mode it locks based on submit success/failure; cooldown only suppresses signal/message/sound/history/extra animation side effects and does not unlock failed matches or skip enabled consume.
- No GUI, armor consume, backpack tick scan, world scan, ConditionEngine, or generic NBT path query is implemented in this phase.

New commands:

```text
/tzz signal blockDevice interactionItem consumeSource <x> <y> <z> matched_source
/tzz signal blockDevice interactionItem consumeSource <x> <y> <z> inventory
/tzz signal blockDevice interactionItem consumeSource <x> <y> <z> main_hand
/tzz signal blockDevice interactionItem consumeSource <x> <y> <z> off_hand
/tzz signal blockDevice interactionItem inventoryConsumeOrder <x> <y> <z> hotbar_first
/tzz signal blockDevice interactionItem inventoryConsumeOrder <x> <y> <z> main_inventory_first

/tzz signal blockDevice itemSubmit enable <x> <y> <z>
/tzz signal blockDevice itemSubmit disable <x> <y> <z>
/tzz signal blockDevice itemSubmit addFromHand <x> <y> <z> <name> at_least <count>
/tzz signal blockDevice itemSubmit addFromHand <x> <y> <z> <name> exactly <count>
/tzz signal blockDevice itemSubmit addFromHand <x> <y> <z> <name> at_most <count>
/tzz signal blockDevice itemSubmit addFromHand <x> <y> <z> <name> ignore
/tzz signal blockDevice itemSubmit list <x> <y> <z>
/tzz signal blockDevice itemSubmit info <x> <y> <z> <name>
/tzz signal blockDevice itemSubmit infoAll <x> <y> <z>
/tzz signal blockDevice itemSubmit remove <x> <y> <z> <name>
/tzz signal blockDevice itemSubmit clear <x> <y> <z>
/tzz signal blockDevice itemSubmit enableRequirement <x> <y> <z> <name>
/tzz signal blockDevice itemSubmit disableRequirement <x> <y> <z> <name>
/tzz signal blockDevice itemSubmit matcherFromHand <x> <y> <z> <name>
/tzz signal blockDevice itemSubmit matcherOption <x> <y> <z> <name> <option> enable|disable
/tzz signal blockDevice itemSubmit count <x> <y> <z> <name> at_least <count>
/tzz signal blockDevice itemSubmit count <x> <y> <z> <name> exactly <count>
/tzz signal blockDevice itemSubmit count <x> <y> <z> <name> at_most <count>
/tzz signal blockDevice itemSubmit count <x> <y> <z> <name> ignore
/tzz signal blockDevice itemSubmit consume <x> <y> <z> enable
/tzz signal blockDevice itemSubmit consume <x> <y> <z> disable
/tzz signal blockDevice itemSubmit consumeOrder <x> <y> <z> hotbar_first
/tzz signal blockDevice itemSubmit consumeOrder <x> <y> <z> main_inventory_first
/tzz signal blockDevice itemSubmit consumeCount <x> <y> <z> <name> <count>
```

Future plan only: 5.15 stabilization / GUI preparation, later ConditionEngine / ConditionGroup, and 6.0 / 7.0 GUI / Admin UI. These are not implemented in 5.14.

SignalBridge 是服务端事件桥 / 事件频道系统，用于把不同系统产生的事件通过 `signal channel` 串联起来。RegionController、封锁卡、密码机、感应板以及未来工具都可以通过 signal channel 联动，并最终由 listener 触发 ActionEngine 动作。

完整使用说明见 [docs/SIGNAL_BRIDGE.md](docs/SIGNAL_BRIDGE.md)。

### 基本示例

```text
/tzz signal listen create debug.test 测试监听器
/tzz signal listen addAction "测试监听器" command say 收到 debug.test
/tzz signal emit debug.test
```

### signal action 示例

```text
/tzz signal listen create area.a.enter A区进入监听器
/tzz signal listen addAction "A区进入监听器" command say 收到A区进入信号
/tzz regionctl addAction A区控制器 enter signal area.a.enter
```

### channel 规则

channel 是技术标识，会被规范化为小写，只允许小写字母、数字、`_`、`-`、`.`、`:`，长度为 1 到 128 个字符。

```text
area.a.enter
password.main.success
debug.test
```

SignalBridge 内置最大递归深度限制，防止 signal 无限触发自身。listener 也可以设置 `cooldownTicks`，用于限制高频触发。

### SignalEmitter 信号发射器

`signal_emitter` 是一个可放置的信号发射器方块。它可以绑定一个 SignalBridge channel，并在红石从未通电变为通电时发出 signal。

- 红石上升沿触发 signal。
- 持续通电不会重复触发。
- 断电后再次通电可再次触发。
- 右键方块可查看频道、启用状态、红石状态和位置。
- 可通过 `/tzz signal device` 命令配置。

设备命令：

```text
/tzz signal device bind <x> <y> <z> redstone.test
/tzz signal device info <x> <y> <z>
/tzz signal device test <x> <y> <z>
/tzz signal device enable <x> <y> <z>
/tzz signal device disable <x> <y> <z>
```

5.2 阶段补充了设备管理命令：

```text
/tzz signal device list
/tzz signal device name <x> <y> <z> <name>
/tzz signal device clearName <device>
/tzz signal device info <device>
/tzz signal device history <device>
/tzz signal device debug <device>
```

`<device>` 可以是设备名称、完整 sourceId 或短 ID。设备名称包含空格时需要加引号，例如：

```text
/tzz signal device info "大厅拉杆发射器"
```

最小使用示例：

```text
/tzz signal listen create redstone.test 红石测试监听器
/tzz signal listen addAction "红石测试监听器" command say 收到红石信号
/tzz signal device bind <x> <y> <z> redstone.test
```

然后用拉杆或按钮给 `signal_emitter` 通电。SignalEventHistory 会记录来源为 `signal_device` 的事件。

### SignalReceiver 信号接收器

`signal_receiver` 是一个可放置的信号接收器方块。它负责把 SignalBridge channel 转换为红石输出：

```text
signal -> signal_receiver -> 红石输出
```

职责边界：

- `SignalListener` 是虚拟逻辑接收端，用于执行 command / message / sound / signal 等 ActionEngine 动作。
- `signal_receiver` 是世界实体红石接收端，只负责输出红石脉冲。
- `signal_receiver` 不负责执行命令，也不需要 channel 上存在 SignalListener 才能工作。
- 接收器只处理已登记且已加载区块中的方块实体，不扫描世界，也不强制加载区块。

新增命令：

```text
/tzz signal receiver pulse <x> <y> <z> <ticks>
/tzz signal receiver trigger <x> <y> <z>
/tzz signal receiver info <x> <y> <z>
```

`pulse` 用于设置红石输出脉冲时长，单位是 GT。默认 `5 GT`，常用范围建议 `2 GT` 到 `20 GT`。命令参数只输入整数，不输入 `GT` 后缀。

`/tzz signal device bind <x> <y> <z> <channel>` 现在同时支持 `signal_emitter` 和 `signal_receiver`。`device list/info/debug/test` 也会显示和操作接收器：

```text
/tzz signal device bind <x> <y> <z> door.a.open
/tzz signal receiver pulse <x> <y> <z> 5
/tzz signal receiver trigger <x> <y> <z>
/tzz signal device info <x> <y> <z>
/tzz signal device debug <device>
```

最小使用示例：

```text
/tzz signal device bind <receiver-x> <receiver-y> <receiver-z> door.a.open
/tzz signal receiver pulse <receiver-x> <receiver-y> <receiver-z> 5
/tzz signal emit door.a.open
```

也可以由 `signal_emitter` 发出同一 channel：

```text
/tzz signal device bind <emitter-x> <emitter-y> <emitter-z> door.a.open
/tzz signal device bind <receiver-x> <receiver-y> <receiver-z> door.a.open
```

之后给 `signal_emitter` 通电，`signal_receiver` 会收到 `door.a.open` 并输出红石脉冲。

Signal 设备管理索引保存到：

```text
world/tzz_mod/signal_devices.json
```

该文件用于管理显示名、位置、最近触发/接收和调试信息。`SignalEmitterBlockEntity` 仍然保存实际 `channel`、`enabled` 和 `lastPowered`；`SignalReceiverBlockEntity` 保存实际 `channel`、`enabled`、`pulseTicks` 和当前脉冲状态。设备历史来自内存中的 SignalEventHistory，不写入 JSON。设备管理不会扫描未加载区块。

### ActionRelay 动作继电器

`action_relay` 是世界中可见的 ActionEngine 执行节点。它监听一个 SignalBridge channel，收到 signal 后执行自己保存的 `actions[]`：

```text
signal -> action_relay -> ActionEngine actions
```

职责边界：

- `SignalListener` 是后台虚拟逻辑接收端。
- `signal_receiver` 是世界实体红石输出端。
- `action_relay` 是世界中可见的 ActionEngine 执行节点。
- `action_relay` 不输出红石，也不是单纯命令方块；它执行的是 `actions[]`。
- `action_relay` 不需要同一 channel 上存在 SignalListener 才能工作。
- 动作继电器只处理已登记且已加载区块中的方块实体，不扫描世界，也不强制加载区块。

新增命令：

```text
/tzz signal relay bind <x> <y> <z> <channel>
/tzz signal relay addAction <x> <y> <z> command <command>
/tzz signal relay addAction <x> <y> <z> message <message>
/tzz signal relay addAction <x> <y> <z> sound <sound>
/tzz signal relay addAction <x> <y> <z> signal <channel>
/tzz signal relay listActions <x> <y> <z>
/tzz signal relay removeAction <x> <y> <z> <index>
/tzz signal relay clearActions <x> <y> <z>
/tzz signal relay cooldown <x> <y> <z> <ticks>
/tzz signal relay trigger <x> <y> <z>
/tzz signal relay info <x> <y> <z>
```

`cooldown` 的单位是 GT，默认 `0 GT`，表示无冷却。命令参数只输入整数，不输入 `GT` 后缀。

`/tzz signal device bind/info/list/debug/test/enable/disable` 现在也支持 `action_relay`。设备列表会显示动作数量、冷却时间和最近执行结果。

设备维护命令：

```text
/tzz signal device cleanup
```

`cleanup` 只检查 `signal_devices.json` 中已经登记的设备，并且只处理所在区块已加载的记录。如果已加载位置不再是对应类型的 Signal 设备，就会移除该索引记录；未加载区块会跳过，不扫描世界，也不强制加载区块。Signal 设备被破坏后也会自动从 `signal_devices.json` 中移除，powered / pulse / active 等同方块状态变化不会误删索引。

最小使用示例：

```text
/tzz signal relay bind <x> <y> <z> game.start
/tzz signal relay addAction <x> <y> <z> command say 游戏开始
/tzz signal emit game.start
```

`action_relay` 的 `actions[]` 直接使用 ActionEngine 的 `ActionConfig` 格式。后续 ActionEngine 增加新动作类型时，动作继电器可以继续复用同一套动作结构。`signal_devices.json` 继续作为设备管理索引，设备历史仍来自内存中的 SignalEventHistory，不新增永久 history JSON。

### Virtual Block Device 虚拟方块发射器

`virtual_block_device` 是虚拟方块发射器。它不是新方块，而是把管理员手动指定的已有方块坐标登记为 SignalBridge 触发源：

```text
已有方块的红石状态变化 -> virtual_block_device -> emit signal
```

它会同时检测该坐标方块自身的 `powered` 状态和该坐标接收到的红石强度。只有已登记坐标从未通电变为通电，或从通电变为未通电时，才会根据触发模式发出 signal。

新增命令：

```text
/tzz signal blockDevice bind <x> <y> <z> <channel>
/tzz signal blockDevice offChannel <x> <y> <z> <channel>
/tzz signal blockDevice clearOffChannel <x> <y> <z>
/tzz signal blockDevice mode <x> <y> <z> redstone_rising
/tzz signal blockDevice mode <x> <y> <z> redstone_falling
/tzz signal blockDevice mode <x> <y> <z> redstone_both
/tzz signal blockDevice info <x> <y> <z>
/tzz signal blockDevice test <x> <y> <z>
/tzz signal blockDevice unbind <x> <y> <z>
/tzz signal blockDevice refresh <x> <y> <z>
```

触发模式：

- `redstone_rising`：未通电 -> 通电时发出 `channel`。
- `redstone_falling`：通电 -> 未通电时发出 `offChannel`；未设置 `offChannel` 时发出 `channel`。
- `redstone_both`：通电和断电边沿都触发；通电发出 `channel`，断电优先使用 `offChannel`，未设置 `offChannel` 时回退发出 `channel`。

因此执行 `clearOffChannel` 后，如果模式仍是 `redstone_both`，通电和断电都会发出主 `channel`，这是预期行为。

5.6 阶段为虚拟方块发射器增加了方块状态条件触发。它检测的是当前方块公开的 BlockState 属性，不检测方块实体 NBT、容器内容、告示牌文字或命令方块命令。

```text
/tzz signal blockDevice condition <x> <y> <z> <condition>
/tzz signal blockDevice clearCondition <x> <y> <z>
/tzz signal blockDevice conditionMode <x> <y> <z> condition_enter
/tzz signal blockDevice conditionMode <x> <y> <z> condition_exit
/tzz signal blockDevice conditionMode <x> <y> <z> condition_both
/tzz signal blockDevice conditionInfo <x> <y> <z>
```

条件使用完整 BlockState 字符串，例如：

```text
minecraft:lever[powered=true]
minecraft:oak_door[open=true]
minecraft:oak_stairs[waterlogged=true,facing=north]
minecraft:redstone_lamp[lit=true]
minecraft:repeater[delay=4]
minecraft:comparator[mode=subtract]
minecraft:wheat[age=7]
```

代码不会硬编码 Wiki 属性白名单，运行时以当前方块实际拥有的 `BlockState.getProperties()` 为准。方块不支持某个属性时会拒绝添加条件，例如 `minecraft:stone[waterlogged=true]`。属性值不在允许值中也会拒绝，例如 `minecraft:repeater[delay=9]`。

条件触发模式：

- `condition_enter`：不满足 -> 满足时发出 `channel`。
- `condition_exit`：满足 -> 不满足时优先发出 `offChannel`，未设置时发出 `channel`。
- `condition_both`：进入条件发出 `channel`，退出条件优先发出 `offChannel`，未设置时回退发出 `channel`。

因此执行 `clearOffChannel` 后，如果模式是 `condition_both`，进入和退出条件都会发出主 `channel`，这是预期行为。

5.7 阶段为虚拟方块发射器增加了右键交互触发。它只对已经登记为 `virtual_block_device` 的坐标生效，玩家右键该坐标方块时可以 emit 独立的 `interactChannel`。

```text
/tzz signal blockDevice interactChannel <x> <y> <z> <channel>
/tzz signal blockDevice clearInteractChannel <x> <y> <z>
/tzz signal blockDevice interaction <x> <y> <z> enable
/tzz signal blockDevice interaction <x> <y> <z> disable
/tzz signal blockDevice interactionCooldown <x> <y> <z> <ticks>
/tzz signal blockDevice interactionInfo <x> <y> <z>
```

交互触发是事件驱动的，不通过 tick 轮询；默认只处理 `MAIN_HAND`，避免主副手双触发。它不会阻止原版右键行为：右键箱子仍会打开箱子，右键门仍会开关门，右键按钮或拉杆仍会正常响应，同时可发出 signal。成功触发 interaction signal 时，触发玩家会播放一次主手挥手动画。`interactionCooldownTicks` 单位是 GT，命令参数只输入整数，不输入 `GT` 后缀。

右键交互会带玩家上下文进入 SignalBridge / ActionEngine。当前方块 ID 与绑定时 `blockId` 不一致时不会触发，`interactionInfo` / `device debug` 会提示 refresh 或重新 bind。一个虚拟方块发射器可以同时配置红石、condition 和 interaction；如果这些触发都指向同一 channel，一次右键可能因原版状态变化和 interaction 同时产生多个 signal，这是可配置行为。

性能边界：

- 不扫描世界。
- 不扫描区块。
- 不扫描周围方块。
- 不自动寻找拉杆、按钮、压力板或红石灯。
- 不强制加载区块。
- 只检测 `signal_devices.json` 中登记过的 `virtual_block_device`。
- 每个设备每次只检测自己的一个坐标。
- 交互触发只检查被右键的一个坐标，不扫描世界、区块或周围方块。
- 不自动寻找可交互方块。
- 不在每次右键时遍历世界内容。
- 有 condition 时 tick 不重新解析 condition 字符串，只比较保存后的 property/value。
- 状态不变不 emit，也不写 JSON。
- `signal_devices.json` 写入已节流，服务端停止时会强制保存。

统一设备命令现在也支持虚拟方块发射器：

```text
/tzz signal device list
/tzz signal device info <device>
/tzz signal device debug <device>
/tzz signal device test <x> <y> <z>
/tzz signal device enable <x> <y> <z>
/tzz signal device disable <x> <y> <z>
/tzz signal device cleanup
```

`device info` 和 `device debug` 会显示 condition 摘要与诊断信息。`cleanup` 对虚拟方块发射器采用保守策略：如果已加载位置变成空气，会删除记录；如果当前方块 ID 与绑定时不一致但不是空气，只在 debug 中提示，不自动删除。condition 无效时也不会自动删除记录，只会提示重新设置 condition 或 `clearCondition`。
`device info` 和 `device debug` 也会显示 interaction 摘要、交互冷却、最近交互玩家和最近交互结果。`device history` 可查看来源为 `virtual_block_device` 的红石、condition 和 interaction 触发记录。

5.8 阶段为虚拟方块发射器增加了容器事件触发。它不是通用 NBT 检测系统，只处理已绑定容器方块的打开、关闭和内容变化事件：

```text
/tzz signal blockDevice containerOpenChannel <x> <y> <z> <channel>
/tzz signal blockDevice clearContainerOpenChannel <x> <y> <z>
/tzz signal blockDevice containerCloseChannel <x> <y> <z> <channel>
/tzz signal blockDevice clearContainerCloseChannel <x> <y> <z>
/tzz signal blockDevice containerChangeChannel <x> <y> <z> <channel>
/tzz signal blockDevice clearContainerChangeChannel <x> <y> <z>
/tzz signal blockDevice container <x> <y> <z> enable
/tzz signal blockDevice container <x> <y> <z> disable
/tzz signal blockDevice containerCooldown <x> <y> <z> <ticks>
/tzz signal blockDevice containerCheckInterval <x> <y> <z> <ticks>
/tzz signal blockDevice containerInfo <x> <y> <z>
```

`containerOpenChannel` 和 `containerCloseChannel` 会在玩家实际打开或关闭对应容器 screen 时触发；`containerChangeChannel` 使用轻量内容指纹检测内容变化。MVP 指纹只包含每个 slot 的物品 registry id、数量和 damage，不做槽位物品条件、物品名称、lore、NBT 或数据组件匹配。

容器事件只对已登记的 `virtual_block_device` 生效，当前方块必须是容器。open / close 使用右键候选加实际 screen 状态确认，不把普通右键直接当作打开；change 只按 `containerChangeCheckIntervalTicks` 轮询已经配置 change channel 的绑定容器。容器事件会带玩家上下文；如果内容变化无法确定玩家，则允许无玩家上下文。

性能边界：

- 不扫描世界、区块或周围方块。
- 不自动寻找箱子、木桶、潜影盒或其他容器。
- 不强制加载区块，未加载区块直接跳过。
- open / close 按玩家实际 screen session 处理。
- content changed 只检查已登记且配置了 change channel 的一个容器坐标。
- 内容不变不 emit，也不写 `signal_devices.json`。
- `containerCooldownTicks` 和 `containerChangeCheckIntervalTicks` 单位都是 GT，命令参数只输入整数，不输入 `GT` 后缀。

职责边界：

- `signal_emitter`：专用方块，红石 / 交互 -> signal。
- `virtual_block_device`：已有方块，红石状态变化 -> signal。
- `signal_receiver`：signal -> 红石输出。
- `action_relay`：signal -> ActionEngine actions。
- `SignalListener`：后台虚拟逻辑接收端。

后续计划仍只记录，不在 5.8 实现：

- 5.10 物品数据 / NBT / 数据组件条件：匹配物品名称、lore、自定义数据、NBT 或新版数据组件。
- 6.0 / 7.0 GUI / Admin UI：通过配置界面管理 SignalBridge、SignalDevice、VirtualBlockDevice、RegionController、ActionEngine、容器/物品条件和游戏主线调度系统。

5.9 阶段为已绑定容器的 `virtual_block_device` 增加了容器槽位 / 物品条件触发。它不是物品 NBT 检测、数据组件匹配或 GUI 配置系统，只比较基础 item registry id 和数量：

```text
/tzz signal blockDevice itemCondition addSlotEmpty <x> <y> <z> <name> <slot> <channel>
/tzz signal blockDevice itemCondition addSlotItem <x> <y> <z> <name> <slot> <itemId> at_least <count> <channel>
/tzz signal blockDevice itemCondition addSlotItem <x> <y> <z> <name> <slot> <itemId> exactly <count> <channel>
/tzz signal blockDevice itemCondition addSlotItem <x> <y> <z> <name> <slot> <itemId> at_most <count> <channel>
/tzz signal blockDevice itemCondition addTotalItem <x> <y> <z> <name> <itemId> at_least <count> <channel>
/tzz signal blockDevice itemCondition addTotalItem <x> <y> <z> <name> <itemId> exactly <count> <channel>
/tzz signal blockDevice itemCondition addTotalItem <x> <y> <z> <name> <itemId> at_most <count> <channel>
/tzz signal blockDevice itemCondition list <x> <y> <z>
/tzz signal blockDevice itemCondition info <x> <y> <z> <name>
/tzz signal blockDevice itemCondition remove <x> <y> <z> <name>
/tzz signal blockDevice itemCondition clear <x> <y> <z>
/tzz signal blockDevice itemCondition enable <x> <y> <z> <name>
/tzz signal blockDevice itemCondition disable <x> <y> <z> <name>
/tzz signal blockDevice itemCondition mode <x> <y> <z> <name> condition_enter
/tzz signal blockDevice itemCondition mode <x> <y> <z> <name> condition_exit
/tzz signal blockDevice itemCondition mode <x> <y> <z> <name> condition_both
/tzz signal blockDevice itemCondition offChannel <x> <y> <z> <name> <channel>
/tzz signal blockDevice itemCondition clearOffChannel <x> <y> <z> <name>
/tzz signal blockDevice itemCondition refresh <x> <y> <z> <name>
/tzz signal blockDevice itemCondition test <x> <y> <z> <name>
```

条件类型：

- `slot_empty`：指定槽位为空时匹配。
- `slot_item`：指定槽位是指定 `itemId`，并且数量满足 `at_least`、`exactly` 或 `at_most`。
- `total_item`：统计整个容器内指定 `itemId` 的总数量，并按 `at_least`、`exactly` 或 `at_most` 判断。

触发规则：

- `condition_enter`：条件从 false -> true 时 emit `channel`。
- `condition_exit`：条件从 true -> false 时优先 emit `offChannel`；未设置时回退 emit `channel`。
- `condition_both`：进入条件 emit `channel`，退出条件优先 emit `offChannel`；未设置时回退 emit `channel`。
- 新增条件时会初始化 `lastMatched` 为当前匹配结果，避免设置瞬间误触发；`refresh` 可手动重新同步当前匹配状态。

性能和边界：

- 只对已绑定、已配置 `itemCondition` 的 `virtual_block_device` 生效。
- 当前方块必须是容器。
- 不扫描世界、区块或周围方块，不强制加载区块，不读取未绑定容器。
- slot 条件只读取指定 slot；total 条件只遍历该容器自身 slot。
- 内容不变不 emit；条件匹配状态不变不 emit；状态不变不写 `signal_devices.json`。
- 本阶段不比较 NBT、数据组件、lore、自定义名称或附魔，也不是通用 NBT 检测系统。
- 如果 `containerChangeChannel` 和 itemCondition channel 指向同一 channel，内容变化和条件边沿可能各自发出 signal，这是配置结果，不是 bug。

5.10 阶段新增可复用 `ItemStackMatcher`。容器物品条件和右键交互主手物品匹配共用同一套模板匹配逻辑，不再各自维护一套判断：

```text
/tzz signal blockDevice itemCondition addSlotMatchFromHand <x> <y> <z> <name> <slot> at_least <count> <channel>
/tzz signal blockDevice itemCondition addSlotMatchFromHand <x> <y> <z> <name> <slot> ignore <channel>
/tzz signal blockDevice itemCondition addSlotMatchFromSlot <x> <y> <z> <name> <targetSlot> <templateSlot> exactly <count> <channel>
/tzz signal blockDevice itemCondition addTotalMatchFromHand <x> <y> <z> <name> at_most <count> <channel>
/tzz signal blockDevice itemCondition addTotalMatchFromHand <x> <y> <z> <name> ignore <channel>
/tzz signal blockDevice itemCondition addTotalMatchFromSlot <x> <y> <z> <name> <templateSlot> at_least <count> <channel>
/tzz signal blockDevice itemCondition matcherInfo <x> <y> <z> <name>
/tzz signal blockDevice itemCondition matcherFromHand <x> <y> <z> <name>
/tzz signal blockDevice itemCondition matcherFromSlot <x> <y> <z> <name> <slot>
/tzz signal blockDevice itemCondition matcherOption <x> <y> <z> <name> matchDamage enable
/tzz signal blockDevice itemCondition matcherOption <x> <y> <z> <name> matchCustomName disable
/tzz signal blockDevice itemCondition matcherCount <x> <y> <z> <name> ignore
```

```text
/tzz signal blockDevice interactionItem setFromHand <x> <y> <z>
/tzz signal blockDevice interactionItem clear <x> <y> <z>
/tzz signal blockDevice interactionItem enable <x> <y> <z>
/tzz signal blockDevice interactionItem disable <x> <y> <z>
/tzz signal blockDevice interactionItem option <x> <y> <z> matchLore enable
/tzz signal blockDevice interactionItem count <x> <y> <z> at_least <count>
/tzz signal blockDevice interactionItem count <x> <y> <z> ignore
/tzz signal blockDevice interactionItem info <x> <y> <z>
```

`slot_matcher` 会用模板匹配指定槽位；`total_matcher` 会统计容器内所有匹配模板的 ItemStack 数量。模板可以从执行者主手捕获，也可以从同一容器的某个槽位捕获。交互物品匹配只检查右键玩家的 `MAIN_HAND`，匹配成功才 emit `interactChannel`；不匹配时不阻止原版交互、不显示失败提示、不消耗物品。

当前 `ItemStackMatcher` 支持 item registry id、count、damage、自定义名称、lore、`custom_data` 和 data components 的整体快照匹配。默认只启用 item id 与数量规则；更严格的 damage / 名称 / lore / custom_data / components 需要管理员显式开启。本阶段不是任意 NBT path 查询系统，也不检测告示牌文字、命令方块命令、刷怪笼 NBT、BlockEntity NBT、玩家 NBT 或实体 NBT。

`ignore` 数量模式不接收数量参数，表示 matcher 不检查数量；info/debug 中显示“数量要求：不检查”。如果需要至少 2 个物品，应使用 `at_least 2`。`consumeCount` 是成功后消耗数量，和 `countMode=ignore` 无关，启用 consume 时仍会检查主手数量是否足够。

5.11 阶段增强了 interactionItem 主手匹配反馈。成功 / 失败频道、消息、音效和成功后消耗物品都可选配置，默认不显示消息、不播放音效、不触发失败频道、不消耗物品。`successChannel` 为空时成功回退使用 `interactChannel`；失败时 `failChannel` 为空则不 emit。成功和失败交互尝试都会播放 `MAIN_HAND` 主手挥手动画；冷却中不会 emit、不会反馈，也不会额外播放触发动画。当前 5.14 语义下，已启用的成功消耗属于开锁成本：匹配成功并放行原版交互时仍会扣除物品，即使处于 cooldown。

```text
/tzz signal blockDevice interactionItem successChannel <x> <y> <z> <channel>
/tzz signal blockDevice interactionItem clearSuccessChannel <x> <y> <z>
/tzz signal blockDevice interactionItem failChannel <x> <y> <z> <channel>
/tzz signal blockDevice interactionItem clearFailChannel <x> <y> <z>
/tzz signal blockDevice interactionItem successMessage <x> <y> <z> <message>
/tzz signal blockDevice interactionItem clearSuccessMessage <x> <y> <z>
/tzz signal blockDevice interactionItem failMessage <x> <y> <z> <message>
/tzz signal blockDevice interactionItem clearFailMessage <x> <y> <z>
/tzz signal blockDevice interactionItem successSound <x> <y> <z> <soundId> <volume> <pitch>
/tzz signal blockDevice interactionItem clearSuccessSound <x> <y> <z>
/tzz signal blockDevice interactionItem failSound <x> <y> <z> <soundId> <volume> <pitch>
/tzz signal blockDevice interactionItem clearFailSound <x> <y> <z>
/tzz signal blockDevice interactionItem consume <x> <y> <z> enable
/tzz signal blockDevice interactionItem consume <x> <y> <z> disable
/tzz signal blockDevice interactionItem consumeCount <x> <y> <z> <count>
```

5.11 的消耗只处理右键玩家 `MAIN_HAND`，不搜索背包、副手、装备栏或盔甲栏；物品数量不足以消耗时会进入失败流程。成功 / 失败 signal 都继续通过 SignalBridge emit，保留玩家上下文，并记录到 history。

5.12 阶段把 `interactionItem` 的检测来源扩展为可配置的玩家物品来源。默认仍是 `main_hand`，旧配置没有新字段时保持 5.10 / 5.11 行为；`off_hand` 和 `inventory_contains` 只有管理员显式配置后才启用。右键事件本身仍只处理 `MAIN_HAND`，`off_hand` 只是检查玩家副手物品，`inventory_contains` 只在玩家右键该绑定方块时检查该玩家自己的主背包 / 热键栏，不包含副手、装备栏或盔甲栏，也不会在 tick 中扫描。

```text
/tzz signal blockDevice interactionItem source <x> <y> <z> main_hand
/tzz signal blockDevice interactionItem source <x> <y> <z> off_hand
/tzz signal blockDevice interactionItem source <x> <y> <z> inventory_contains
/tzz signal blockDevice interactionItem source <x> <y> <z> armor_head
/tzz signal blockDevice interactionItem source <x> <y> <z> armor_chest
/tzz signal blockDevice interactionItem source <x> <y> <z> armor_legs
/tzz signal blockDevice interactionItem source <x> <y> <z> armor_feet
/tzz signal blockDevice interactionItem source <x> <y> <z> armor_any
/tzz signal blockDevice interactionItem vanillaInteraction <x> <y> <z> allow
/tzz signal blockDevice interactionItem vanillaInteraction <x> <y> <z> require_item_match
```

5.13 阶段继续扩展 `interactionItem` 的玩家物品来源，新增 `armor_head`、`armor_chest`、`armor_legs`、`armor_feet`、`armor_any`。这些来源必须由管理员显式配置；右键事件仍只处理 `MAIN_HAND`，armor 来源只是读取触发玩家对应盔甲槽位的 ItemStack，不处理装备事件，也不做装备 / 盔甲消耗。`armor_any` 只检查头盔、胸甲、护腿、靴子四个盔甲槽，并记录第一个匹配槽位。

`inventory_contains` 会用同一套 `ItemStackMatcher` 先匹配非数量条件，再统计主背包 / 热键栏内匹配 ItemStack 的总数：`ignore` 表示至少存在一个匹配 stack，`at_least` / `exactly` / `at_most` 作用于总数量，其中 `at_most` 要求总数大于 0，避免没有物品也满足条件。消耗仍只支持 `main_hand`；source 为 `off_hand` 或 `inventory_contains` 时启用 consume 会被拒绝，旧数据中出现不兼容配置时运行时不会消耗，并会在 debug 中提示。

`consume` 仍只支持 `main_hand`；source 为 `off_hand`、`inventory_contains` 或任意 `armor_*` 时启用 consume 会被拒绝。旧数据中出现 `armor_*` source 同时 `consumeEnabled=true` 时，运行时不会消耗，并会按失败流程处理或在 debug 中提示。

`vanillaInteraction` 默认是 `allow`，保持旧行为：即使 interactionItem 匹配失败，也不阻止箱子、门、按钮、拉杆等原版右键行为。管理员显式设置为 `require_item_match` 后，它会作为锁定策略生效：只有 interactionItem 匹配成功才允许原版交互继续；匹配失败、空手不匹配或数量不足以 consume 时会返回阻止原版 use 的结果，不触发成功频道、不消耗物品。`interactionCooldownTicks` 不会让这个锁失效；冷却中匹配失败仍会阻止箱子打开、门开关、按钮/拉杆切换等原版交互。cooldown 只抑制 signal、message、sound、history / lastResult 和额外挥手动画；不会跳过已启用的成功消耗。匹配成功并放行原版交互时仍会扣除物品，即使处于 cooldown。设备禁用、interaction 禁用、matcher 未启用、blockId 不一致、空气或未绑定方块仍保持 `PASS`。

门会按上下半格做最小归一化：如果管理员绑定门下半格，玩家右键上半格时会尝试匹配下半格设备；如果绑定上半格，右键下半格也会尝试匹配上半格设备。该逻辑只检查当前点击坐标和门的另一半坐标，不扫描世界，用于避免 `require_item_match` 被右键另一半门绕过。

性能边界保持不变：只检查被右键的一个坐标，不扫描世界、区块或周围方块，不强制加载区块；`main_hand` 只读主手，`off_hand` 只读副手，`inventory_contains` 只读触发玩家的主背包 / 热键栏，`armor_head` / `armor_chest` / `armor_legs` / `armor_feet` 只读对应盔甲槽，`armor_any` 只读四个盔甲槽，不读取其他玩家，也不在 tick 中检查装备。

后续计划仍只记录，不在 5.13 实现：

- 5.14 消耗策略 / 多物品提交，包括背包消耗、副手消耗和更复杂的提交规则。
- 复杂 ConditionEngine / ConditionGroup 后续单独设计。
- 5.15 稳定化 / GUI 前置整理版。
- 更完整的 GUI / Admin UI：所有 source、matcher、consume 和反馈配置未来都应进入 GUI；可拆分成交互条件配置器、物品 matcher 配置器、容器条件配置器、signal 设备配置器、debug/doctor 工具。

### SignalBridge 可观测性命令

4.5 阶段补充了 SignalBridge 的只读观测与诊断命令，用于排查 signal 是否发出、channel 是否存在 listener、listener 是否处于冷却或存在递归风险。

```text
/tzz signal history
/tzz signal history <channel>
/tzz signal clearHistory
/tzz signal channels
/tzz signal channel info <channel>
/tzz signal listen debug <listener>
/tzz signal doctor
```

- `history`：查看最近 signal 事件，默认显示最近 10 条。
- `history <channel>`：只查看指定 channel 的最近 signal 事件。
- `clearHistory`：清空内存中的 signal 历史记录。
- `channels`：查看所有已知 signal channel，包括 listener 数量、动作数量和最近触发时间。
- `channel info <channel>`：查看某个 channel 的 listener 列表和最近事件。
- `listen debug <listener>`：查看单个 listener 的动作、冷却剩余、最近频道事件和直接递归风险。
- `doctor`：全局诊断 SignalBridge 配置问题，例如空动作 listener、无 listener channel、全部禁用 channel、直接 signal 自递归、异常 cooldown 和脏数据。

这些命令只用于查看、清理内存历史或诊断配置，不改变 SignalBridge 的 `emit`、listener 或 ActionEngine 执行语义。

## RegionController

RegionController 是“区域事件控制器”，用于让已有规划区域拥有逻辑触发能力：

```text
已有规划区域
-> 创建区域控制器
-> 玩家进入区域触发 enterActions
-> 玩家离开区域触发 exitActions
-> 玩家停留区域触发 stayActions
-> 动作通过 ActionEngine 执行
```

RegionController 不改变区域本身数据。`PlannerRegionData` 仍然只负责区域形状、名称、维度等地图数据；`RegionControllerData` 单独保存触发逻辑。

完整使用说明见 [docs/region_controller.md](docs/region_controller.md)。

### 快速示例

```text
/tzz regionctl regions
/tzz regionctl create <区域名称或区域ID> A区控制器
/tzz regionctl addAction A区控制器 enter command say 玩家进入A区
/tzz regionctl addAction A区控制器 exit command say 玩家离开A区
/tzz regionctl addAction A区控制器 stay command say 玩家仍在A区
/tzz regionctl stayInterval A区控制器 100
/tzz regionctl target A区控制器 all
/tzz regionctl test A区控制器 enter
```

### 触发对象过滤

- `all`：所有玩家触发。
- `op`：只有 OP 玩家触发。
- `tag <tagName>`：只有拥有指定 scoreboard tag 的玩家触发。

示例：

```text
/tzz regionctl target A区控制器 tag runner
```

### STAY 语义

`stayActions` 是玩家持续停留在区域内时周期触发的动作。

- 默认间隔为 `100 tick`。
- 最小间隔为 `20 tick`。
- 进入区域后不会立刻触发 `stay`，而是在达到间隔后触发。

### 事件语义

- 玩家第一次被扫描时，不触发 `ENTER`。
- 玩家退出服务器时，不触发 `EXIT`。
- 玩家跨维度时，对原区域触发 `EXIT`。
- 玩家传送跨过边界，也会触发 `ENTER` / `EXIT`。
- 区域边界是否算区域内，沿用现有区域几何判断。

### 配置文件

RegionController 配置保存到：

```text
world/tzz_mod/region_controllers.json
```

该文件由模组自动维护，不建议手动编辑，除非你熟悉当前 JSON 结构。

## 最小验收流程

1. 创建一个规划区域。
2. 执行 `/tzz regionctl regions`。
3. 执行 `/tzz regionctl create <region> 测试控制器`。
4. 添加 `enter` 动作。
5. 添加 `exit` 动作。
6. 执行 `/tzz regionctl test <controller> enter`。
7. 实际走入区域。
8. 实际走出区域。
9. 添加 `stay` 动作并测试。
10. 重启世界后确认配置仍存在。

## 物品与使用

- `phone`：右键打开手机界面。
- `ar_headset`：可装备到头部，右键打开 AR 界面。
- `attention`：右键播放提示音并将玩家朝向对齐到最近的 90 度方向。
- `*_blocking_card`：保存实体或方块触发配置，并在满足条件时执行动作。
- `blocking_card_configurator`：批量装入、取出和配置封锁卡。
- `password_config_card`：打开密码配置界面。
- `map_marker`：添加地图标点。
- `region_planner`：创建和编辑规划区域。
- `task_configurator`：创建和编辑任务配置。
- `signal_emitter`：可绑定 SignalBridge channel，并在红石上升沿发出 signal。
- `signal_receiver`：接收 SignalBridge channel 并输出红石脉冲。
- `action_relay`：接收 SignalBridge channel 并执行 ActionEngine actions。

## 开发与构建

要求：JDK 21、Fabric Loader、Fabric API。

运行客户端：

```bash
./gradlew.bat runClient
```

构建：

```bash
./gradlew.bat build
```

完整验证：

```bash
./gradlew.bat clean build
```

当前 7.x / Local Test MCP 验证通常还需要：

```powershell
cd tools\tzz-test-mcp
npm run build
npm test

cd ..\..
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
git diff --check
```

构建产物位于 `build/libs/`。

## 贡献与许可

欢迎提交 Issue 和 Pull Request。建议先使用 `runClient` 本地调试。

许可证：`CC0-1.0`，详见 [LICENSE](LICENSE)。
