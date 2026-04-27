# 5.15 稳定化审查报告

审查分支：`feature/stabilization-foundation`

审查范围：5.1 到 5.14 已完成的 SignalBridge、SignalDevice、专用设备、`virtual_block_device`、ActionEngine、RegionController、生命周期和文档。

审查结论摘要：

- 本轮未确认新的 P0 阻断级生产漏洞；5.14 验收中修复过的锁、cooldown、consume、itemSubmit 原子消耗路径在当前代码中已有对应保护。
- 当前主要风险不是单点功能缺失，而是 5.5 到 5.14 快速扩展后，`SignalDeviceData`、`SignalDeviceStore`、`VirtualBlockDeviceInteractionHandler` 和命令类承担了过多职责，后续继续堆功能会提高字段丢失、命令校验不一致和 debug 输出不完整的概率。
- 5.15 第一优先级应是稳定性护栏：字段保留回归测试、interaction/itemSubmit/consume 核心路径拆分、debug/doctor 结构化、命令反馈一致性扫描。
- Web Admin UI 不应直接读写 JSON；命令、游戏内工具、未来 Web UI 应共用 service 层和结构化 DTO。

问题分级统计：

- P0：0
- P1：8
- P2：7

## 1. 当前系统总览

当前底层链路可以简化为：

```text
事件源
→ SignalBridge signal
→ SignalListener / SignalReceiver / ActionRelay
→ ActionEngine / redstone / command / message / sound / signal
```

`virtual_block_device` 的链路是：

```text
virtual_block_device
→ redstone / BlockState / interaction / container / itemCondition / itemSubmit
→ SignalBridge
→ SignalListener / SignalReceiver / ActionRelay
```

### 主要事件源

- `signal_emitter`：专用方块，红石或交互触发 signal。
- `signal_receiver`：专用方块，收到 signal 后输出红石脉冲。
- `action_relay`：专用方块，收到 signal 后执行 ActionEngine actions。
- `virtual_block_device`：管理员手动绑定已有方块坐标后，可通过红石状态、BlockState 条件、右键交互、容器事件、容器物品条件、interactionItem、itemSubmit 等触发 signal。
- `RegionController`：区域 enter / exit / stay 触发 ActionEngine actions，其中 signal action 会回到 SignalBridge。

### 当前核心分发

- `SignalBridgeServer.emit` 是 signal 入口，负责 channel 校验、递归深度保护、receiver/actionRelay/listener 分发和历史记录。
- `SignalReceiverDispatcher` 与 `ActionRelayDispatcher` 在 listener 之前被分发，因此没有 listener 时 receiver/action_relay 仍可工作。
- `ActionEngine` 负责 command / message / sound / signal actions；signal action 继续走 `SignalBridgeServer.emit`，保留递归保护。
- `SignalEventHistory` 是内存历史，当前容量上限为 200 条。

### 未来 GUI / Web Admin UI 接入点

未来 Web Admin UI 应接入 service 层，而不是直接操作：

- `signal_devices.json`
- `signal_listeners.json`
- block entity NBT
- 命令类内部 helper

推荐接入点：

- 读：`SignalDebugService`、`SignalHistoryService`、`DoctorService`
- 写：`SignalDeviceConfigService`、`VirtualBlockDeviceConfigService`、`InteractionItemConfigService`、`ItemSubmitConfigService`、`ActionRelayConfigService`、`RegionConfigService`
- 实时：内部 event bus → WebSocket

## 2. 核心类职责清单

| 类 | 当前职责 | 职责是否过重 | GUI 调用适配 | 建议 |
| --- | --- | --- | --- | --- |
| `SignalBridgeServer` | signal emit、递归保护、receiver/actionRelay/listener 分发、history 记录 | 中等 | 可通过 service 包一层只读/emit DTO | 保持核心入口稳定，新增结构化 emit result |
| `SignalEvent` | signal 上下文数据 | 不重 | 适合 DTO 化 | 可补充 detail/metadata 标准字段 |
| `SignalEventHistory` | 内存环形历史 | 不重 | 只读适配容易 | 未来可按 device/source/channel 查询并分页 |
| `SignalListenerStore` | listener JSON 存储、查询、flush | 中等 | 不建议 Web UI 直接调用写方法 | 抽 ListenerConfigService |
| `SignalChannelInspector` | channel 汇总、最近事件 | 不重 | 适合作为 Web UI 只读服务基础 | 可扩展 receiver/actionRelay 计数 |
| `SignalDoctor` | listener/channel/action 诊断 | 偏窄 | 当前不覆盖 device 复杂状态 | 扩展为结构化 DoctorService |
| `SignalDeviceData` | 所有 signal device 的持久化字段和运行时摘要 | 过重 | 不适合直接暴露给 GUI | 分组为嵌套配置对象或 DTO |
| `SignalDeviceStore` | JSON 存储、查询、设备创建/更新、状态记录、cleanup、复制字段 | 过重 | 不建议 Web UI 直接写 | 抽 config service；store 只做读写和索引 |
| `SignalDeviceCommand` | device list/info/debug/history/cleanup/test/enable/disable 以及部分业务诊断 | 过重 | 不适合复用 | 输出改为调用 DebugService DTO |
| `VirtualBlockDeviceCommand` | bind、redstone、condition、container、itemCondition 等入口组合 | 过重 | 不适合复用 | 拆按功能域 service |
| `VirtualBlockInteractionItemCommand` | interactionItem matcher、source、feedback、consume、vanilla policy 命令 | 过重 | 不适合复用 | 抽 InteractionItemConfigService |
| `VirtualBlockItemSubmitCommand` | itemSubmit requirement、consume、info/list 命令 | 中到重 | 不适合复用 | 抽 ItemSubmitConfigService |
| `VirtualBlockDeviceInteractionHandler` | UseBlockCallback、门半格归一化、item source、itemSubmit、consume plan、feedback、emit、vanilla policy | 明显过重 | 不能直接给 GUI 复用 | P1 拆分为 resolver/evaluator/planner/feedback |
| `VirtualBlockDeviceDispatcher` | redstone 和 BlockState condition tick | 可接受 | GUI 不直接调用 | 保持纯 tick dispatcher |
| `VirtualBlockDeviceContainerHandler` | container pending open、open session、close、content fingerprint、itemCondition tick | 中等偏重 | GUI 不直接调用 | container event 与 itemCondition 可再拆 |
| `ItemStackMatcherData` | matcher 模板、反馈配置、consume/source 状态 | 偏重 | 适合拆 DTO | 将 matcher 与 interaction feedback 分离 |
| `ItemStackMatcher` | ItemStack 匹配 | 不重 | 可直接被 service 复用 | 保持纯逻辑 |
| `ItemStackMatcherSupport` | 捕获模板、摘要、快照、命令配置辅助 | 中等 | 部分可复用 | 区分 command text 和 core helper |
| `ItemSubmitRequirementData` | 单个提交 requirement | 不重 | 可 DTO 化 | 保持 normalized/default |
| `ActionEngine` | 执行 command/message/sound/signal | 中等 | Web UI 应通过 ActionConfigService 配置 | command action 权限边界需文档化 |
| `ActionValidator` | 保存和执行前校验 | 不重 | 可复用 | 增加 Web UI 可读错误码 |
| `ActionAuditLogger` | action 执行日志/通知 | 不重 | 可扩展到 Web audit | 未来接 Web 审计 |
| `RegionControllerStore` | region controller JSON 存储 | 中等 | Web UI 不直接写 | 抽 RegionConfigService |
| `RegionControllerTracker` | 玩家区域状态、enter/exit/stay actions | 中等 | 不直接给 GUI | 输出区域状态给 debug service |
| `TzzLifecycleBootstrap` | tick 注册、flushDirty、stop/clear cache | 不重 | 无 | 保持集中生命周期 |
| `TzzServerBootstrap` | server event handler 注册 | 不重 | 无 | 注意 UseBlockCallback 注册顺序 |

重点判断：

- `VirtualBlockDeviceInteractionHandler` 已经过重。它同时承载 5.7 到 5.14 的多数复杂规则，是 5.15 最应拆分的类。
- `SignalDeviceData` 字段过多，且把配置、运行时状态、debug 摘要混在同一个 record 中。继续追加字段会提高兼容风险。
- 命令类承担了大量业务校验和更新逻辑；未来 GUI 如果复用命令会很脆弱。
- `SignalDeviceStore` 同时承担存储、查询、业务 mutation、运行时 record、cleanup。建议让 store 退回到持久化和索引职责。

## 3. 数据模型审查

### 当前兼容情况

`SignalDeviceData.normalized()` 已覆盖主要默认值：

- 旧 JSON 缺失 `condition*` 字段时默认关闭。
- 旧 JSON 缺失 `interaction*` 字段时默认关闭或空值。
- 旧 JSON 缺失 `container*` 字段时默认关闭，`containerChangeCheckIntervalTicks` 默认修正为 10。
- 旧 JSON 缺失 `itemConditions` 时默认空列表。
- 旧 JSON 缺失 `interactionItemMatcher` 时默认 `ItemStackMatcherData.empty()`。
- 旧 JSON 缺失 `itemSubmitRequirements` 时默认空列表。
- `interactionItemSource`、`vanillaInteractionPolicy`、`consumeSource`、`inventoryConsumeOrder` 等字段在 `ItemStackMatcherData.normalized()` 中有默认值。

当前设计基本满足“旧 JSON 不崩溃”的要求。

### 已重点检查的问题

| 检查项 | 结论 |
| --- | --- |
| 旧 JSON 缺字段安全默认值 | 基本安全，由 `normalized()` 集中处理 |
| 新字段兼容读取 | 基本安全，Gson 缺字段会给 Java 默认值，再由 `normalized()` 修正 |
| `itemSubmitRequirements` 被其他命令误清空 | 当前 `withInteractionItemMatcher`、`withItemSubmit`、`withVirtualSettings` 等 copy 路径已显式保留该列表 |
| `setFromHand` 是否保留 success/fail/source/consume | 当前通过 `ItemStackMatcherSupport` 和 store 更新保留 matcher 相关配置；仍建议加回归测试 |
| `consumeSource` / `itemSubmit` / `vanillaInteractionPolicy` / `interactionItemSource` 命名 | 字段语义清晰，但分散在 `ItemStackMatcherData` 与 `SignalDeviceData` 中 |
| 字段重复含义 | `lastResult`、`lastInteractionResult`、`lastInteractionItemResult`、`lastItemSubmitResult` 有相近含义，建议运行时状态分组 |
| 是否需要嵌套对象 | 是，建议 5.15 先设计迁移方案，不直接大改 JSON |

### 主要结构风险

`SignalDeviceData` 当前把这些域放在一个 record：

- 基础设备字段
- receiver pulse 状态
- action relay 状态
- virtual block redstone
- BlockState condition
- interaction
- container
- itemCondition
- interactionItem matcher
- itemSubmit
- last runtime status

这导致每次新增字段都需要同步维护：

- record 主构造参数
- 多个兼容构造器
- `normalized()`
- `SignalDeviceStore` 的多个 `withX` copy 方法
- info/debug/list/history 展示
- JSON 读写兼容

5.14 验收中出现过 `itemSubmitRequirements` 被清空，说明这个风险已经不是理论风险。

### 建议分组

本轮不建议直接改 JSON 结构，但应先设计目标结构：

- `baseConfig`
  - `id`、`type`、`name`、`dimension`、`x/y/z`、`enabled`
- `redstoneConfig`
  - `channel`、`offChannel`、`mode`、`lastPowered`、`lastPowerLevel`
- `conditionConfig`
  - `conditionEnabled`、`conditionBlockId`、`conditionProperties`、`conditionMode`
- `interactionConfig`
  - `interactionEnabled`、`interactChannel`、`interactionCooldownTicks`
- `interactionItemConfig`
  - matcher、source、vanilla policy、feedback、consume strategy
- `containerConfig`
  - open/close/change channel、cooldown、check interval、fingerprint
- `itemConditionConfig`
  - container item conditions
- `itemSubmitConfig`
  - submit enabled、requirements、consume order、last submit result
- `runtimeStatus`
  - last trigger、last player、last result、last event type、last consume summary
- `audit/historySummary`
  - 给 Web UI 和 doctor 使用的轻量摘要

迁移建议：

1. 5.15 先补 `SignalDeviceDataCopyTest` 或等价数据复制回归测试。
2. 新增 service/DTO 层，内部仍读旧 record。
3. 6.x 再考虑 JSON schema v2 和自动迁移。

## 4. 命令一致性审查

### 已符合规范的命令范围

以下命令族整体命名清晰，参数有实际语义：

- `/tzz signal device list/info/debug/history/test/enable/disable/cleanup`
- `/tzz signal blockDevice bind/unbind/refresh/info/test`
- `/tzz signal blockDevice condition/clearCondition/conditionMode/conditionInfo`
- `/tzz signal blockDevice interactChannel/clearInteractChannel/interaction/interactionCooldown/interactionInfo`
- `/tzz signal blockDevice containerOpenChannel/containerCloseChannel/containerChangeChannel/containerInfo`
- `/tzz signal blockDevice itemCondition addSlotEmpty/addSlotItem/addTotalItem/list/info/remove/clear`
- `/tzz signal blockDevice interactionItem source/vanillaInteraction/count`
- `/tzz signal blockDevice itemSubmit addFromHand/list/info/infoAll/remove/clear/consume/consumeOrder/consumeCount`

### 5.14 后已修正的命令设计

- `ignore` 不再带无意义 count。
- `interactionItem successChannel/failChannel/message/sound/vanillaInteraction/consumeSource/inventoryConsumeOrder/consumeCount/info` 不再错误要求已有 matcher 模板。
- `interactionItem consumeSource`、`inventoryConsumeOrder`、`itemSubmit` 反馈已从英文硬编码改为中文可读文本。
- `itemSubmit` 与 `interactionItem matcher` 已明确为互斥匹配模式。

### 仍需审查和加护栏的点

| 类型 | 说明 | 建议 |
| --- | --- | --- |
| 中文反馈一致性 | 命令反馈分散在多个 command 类中，仍容易出现英文或内部 enum 直出 | P1：集中 `DisplayName` / `FeedbackText` helper |
| 内部 enum 暴露 | 命令参数仍必须使用 `matched_source`、`hotbar_first` 等内部 ID，这是命令接口需要；玩家反馈不应直接显示内部 ID | P1：所有反馈统一走 displayName |
| 参数顺序 | 大多数命令是 `<pos> ...`，部分 device 命令支持 `<device>`，可接受 | P2：Web UI 不依赖命令参数 |
| 前置条件 | 5.14 已修复 success/fail 配置不应要求 matcher；未来新增命令需要测试 | P1：命令前置条件回归测试 |
| 危险操作反馈 | `clear`、`remove`、`disable` 有反馈，但没有二次确认 | 当前游戏内命令可接受；Web UI 未来需确认弹窗和审计 |

### 需要未来保留兼容别名的命令

目前没有必须立刻改名的命令。未来 GUI 化后可考虑在 UI 中使用更自然的标签：

- `interactionItem` → “单物品交互条件”
- `itemSubmit` → “多物品提交”
- `consumeSource` → “消耗来源”
- `inventoryConsumeOrder` → “背包消耗顺序”

命令名不建议 5.15 改动，以免破坏管理员脚本。

### 参数设计可疑但可接受的命令

- `interactionItem consumeSource main_hand/off_hand/inventory`：当 source 与 consumeSource 不一致时语义较复杂，但已有诊断和失败路径。建议 Web UI 用下拉提示兼容性。
- `itemSubmit consumeCount <name> <count>`：和 matcher count 分开是正确的，但需要 info/debug 始终同时显示“匹配数量要求”和“消耗数量”。
- `interactionItem source inventory_contains`：名称明确，但 Web UI 中应展示“不含副手/装备/盔甲”。

## 5. 逻辑漏洞风险清单

### 5.1 锁与 cooldown

| 风险 | 级别 | 当前判断 | 是否建议 5.15 修复 | 建议位置 |
| --- | --- | --- | --- | --- |
| `require_item_match` 被 cooldown 绕过 | 高 | 当前代码先评估 matcher/itemSubmit/consume plan，再用 cooldown 抑制副作用；已修复并通过验收 | 不需要功能修复；需要回归测试 | `VirtualBlockDeviceInteractionHandler` |
| cooldown 跳过 consume | 高 | 当前 consume plan 在 `inCooldown` 判断前 apply；已修复 | 不需要功能修复；需要回归测试 | `VirtualBlockDeviceInteractionHandler` |
| allow 模式误阻止原版交互 | 中 | `vanillaFailureResult` 只在 policy 阻止时返回 FAIL，allow 保持 PASS | 不需要 | `InteractionItemVanillaPolicy` |
| cooldown 中仍频繁写 lastResult/history | 中 | 当前 interactionItem 在 cooldown 成功路径不会记录 success；失败路径在 cooldown 直接返回 policy 结果 | 建议补诊断说明和测试 | `SignalDeviceStore.recordVirtualInteractionItemResult` |

结论：锁与 cooldown 当前逻辑经 5.14 修复后可接受，但必须加入 5.15 回归测试，防止未来调整 handler 时复发。

### 5.2 consume 风险

| 风险 | 级别 | 当前判断 | 是否建议 5.15 修复 | 建议位置 |
| --- | --- | --- | --- | --- |
| main_hand consume 固定扣 1 | 高 | 当前 `addHandConsumePlan` 使用 `matcher.consumeCount()` 后的 count，已按 consumeCount | 不需要；加测试 | `VirtualBlockDeviceInteractionHandler` |
| off_hand consume 固定扣 1 | 高 | 同 hand plan，已按 consumeCount | 不需要；加测试 | 同上 |
| inventory consume 不跨 stack | 高 | 当前 `addInventoryConsumePlan` 遍历 main stacks，并按 remaining 跨 stack 消耗 | 不需要；加测试 | 同上 |
| inventory consume 读取副手/装备/盔甲 | 中 | 当前使用 `player.getInventory().getMainStacks()`，不含副手/盔甲 | 不需要 | 同上 |
| armor_* consume 被误允许 | 高 | 当前 `resolveConsumeSource` 对 armor source 返回空，命令层也应拒绝 | 需要测试覆盖旧 JSON 兼容 | `VirtualBlockInteractionItemCommand`、handler |
| creative 玩家消耗与文档不一致 | 中 | 当前直接 decrement stack，倾向和 survival 一致；需文档和测试明确 | P1：文档和测试确认 | handler / docs |
| consume plan 部分 apply | 高 | 当前 `ConsumePlan.copy()` + `replaceWith()` + apply 后置，itemSubmit plan 阶段不改真实 inventory | 不需要功能修复；加测试 | handler |
| interactionItem consume 与 itemSubmit consume 重复扣同一 stack | 高 | 当前 itemSubmit 模式不执行 interactionItem consume，互斥降低风险 | 不需要；加测试 | handler |

### 5.3 itemSubmit 风险

| 风险 | 级别 | 当前判断 | 是否建议 5.15 修复 | 建议位置 |
| --- | --- | --- | --- | --- |
| itemSubmit 与 interactionItem matcher 隐式叠加 | 高 | 当前 `itemSubmitMode` 下 `hasItemMatcher=false`，不执行单物品 matcher | 不需要；加测试 | handler |
| itemSubmitEnabled=true 时仍执行 interactionItem consume | 高 | 当前 itemSubmitMode 下只走 itemSubmit consume | 不需要；加测试 | handler |
| itemSubmit enable 未关闭单物品 matcher | 中 | 命令中启用 itemSubmit 后会关闭单物品 matcher | 不需要；加测试 | `VirtualBlockItemSubmitCommand` |
| itemSubmit disable 后悄悄恢复 matcher | 中 | 当前不会自动恢复 | 不需要 | 同上 |
| interactionItem enable 在 itemSubmitEnabled=true 时未拒绝 | 中 | 5.14 验收已确认拒绝；建议测试锁定 | P1 测试 | `VirtualBlockInteractionItemCommand` |
| disabled requirement 仍参与匹配/消耗 | 中 | evaluate 和 consume plan 均跳过 disabled requirement | 不需要；加测试 | handler |
| remove/clear 触发 signal | 低 | 命令只更新配置，不 emit | 不需要 | itemSubmit command |

### 5.4 blockId / door / cleanup 风险

| 风险 | 级别 | 当前判断 | 是否建议 5.15 修复 | 建议位置 |
| --- | --- | --- | --- | --- |
| blockId 不一致误触发 | 高 | interaction/redstone/container 多处检查当前 blockId 与绑定 blockId | 不需要；加测试 | VBD handlers |
| 门上/下半格绕过锁 | 高 | interaction target resolver 会检查 clicked pos 和另一半门坐标 | 不需要；加测试 | `findInteractionTarget` |
| 门归一化扩展到非 interaction 路径 | 低 | 当前只影响 interaction 锁。redstone/condition/container 仍按绑定坐标，符合范围 | 不需要 | 文档说明即可 |
| cleanup 误删未加载区块设备 | 高 | cleanup 文档和命令反馈说明只检查已加载区块，不强制加载 | 不需要；加测试 | `SignalDeviceStore.cleanupInvalidLoadedDevices` |
| chunk unload 导致误删 | 高 | 未加载区块应跳过 | 不需要；加测试 | cleanup |
| 空气位置 cleanup | 中 | 已作为功能验收通过 | 不需要 | cleanup |

### 5.5 SignalBridge / recursive / history 风险

| 风险 | 级别 | 当前判断 | 是否建议 5.15 修复 | 建议位置 |
| --- | --- | --- | --- | --- |
| signal action 绕过递归保护 | 高 | `ActionEngine.executeSignal` 走 `SignalBridgeServer.emit`，depth + 1 | 不需要；加测试 | `ActionEngine` |
| receiver/action_relay 无 listener 时不工作 | 高 | `SignalBridgeServer.emit` 在 listener 判断前 dispatch receiver/actionRelay | 不需要；加测试 | `SignalBridgeServer` |
| failChannel/successChannel 绕过 SignalBridge | 高 | interaction success/fail emit 均使用 `SignalBridgeServer.emit` | 不需要；加测试 | handler |
| history 内存无限增长 | 中 | `SignalEventHistory.MAX_RECORDS=200`，不是无限增长 | 不需要；P2 优化分页和过滤 | `SignalEventHistory` |
| history 调试信息不够结构化 | 中 | 当前 result/detail 是字符串 | P1：结构化 detail/metadata | `SignalEventRecord` / DebugService |

## 6. 性能边界审查

### 总体结论

当前 5.5 到 5.14 的核心边界仍成立：

- 不扫描世界。
- 不扫描区块。
- 不扫描周围方块。
- 不强制加载区块。
- 不 tick 扫描玩家背包。
- 不 tick 扫描所有容器。
- 只检测已登记设备。
- 只检测已配置条件。
- 内容不变不 emit。
- 状态不变不写 JSON。
- `flushDirty` 有 100 GT 节流。
- server stopping/stopped 会 `forceFlushDirty`。

### tick 入口

| 入口 | 注册位置 | 复杂度 | 风险 |
| --- | --- | --- | --- |
| `VirtualBlockDeviceDispatcher.tick` | `TzzLifecycleBootstrap.END_SERVER_TICK` | O(已登记 virtual_block_device 数量) | 每 tick 遍历所有 VBD，数量很大时需要分页或分桶 |
| `VirtualBlockDeviceContainerHandler.tick` | `TzzLifecycleBootstrap.END_SERVER_TICK` | open/close O(在线 pending/session)，content change O(已登记 VBD × slot count，受 interval 限制) | content change 会遍历 VBD snapshot，但只处理启用 change/itemCondition 的设备 |
| `SignalDeviceStore.flushDirty` | `TzzLifecycleBootstrap.END_SERVER_TICK` | O(设备数量)，仅 dirty 且过节流写 | Web UI 未来不能频繁 markDirty |
| `RegionControllerTracker.tick` | `RegionControllerServer.END_SERVER_TICK` 每 10 tick | O(在线玩家 × enabled controllers) | controller 很多时可优化空间索引 |
| `MapServer.tickPlayerRegions` | `MapServer` 自己注册 | 不属于 5.15 signal 主体 | 需要单独 map 系统审查 |
| `DeathSyncServer` tick | network 模块 | 不属于 signal 主体 | 不纳入本轮重点 |

### 隐藏全局扫描检查

- interaction：事件驱动，只处理 clicked pos；门只额外检查另一半坐标。
- interactionItem source：
  - `main_hand` 只读主手。
  - `off_hand` 只读副手。
  - `inventory_contains` 只在右键时读触发玩家主背包/热键栏。
  - `armor_any` 只读四个盔甲槽。
- itemSubmit：只在右键时读触发玩家主背包/热键栏。
- container change：只轮询已登记、已启用、配置了 change channel 或 itemCondition 的容器设备。

### Web UI 性能注意

未来 Web UI 不能每次表单变化都写 JSON。建议：

- UI 草稿状态存在内存/客户端。
- 点击保存才调用 service mutation。
- mutation 合并后一次 markDirty。
- 高频实时状态走只读 DTO 和 event stream，不写配置 JSON。

## 7. debug / doctor 覆盖度审查

### 已覆盖或基本覆盖

- 设备 disabled：device info/debug 可见。
- interaction disabled：interactionInfo/device debug 可见。
- blockId 不一致：info/debug 已多次加入提示。
- channel 为空：多数 info/debug 显示空频道。
- channel 无 listener：SignalBridge history/doctor 可发现无 listener channel；receiver/actionRelay 单独仍可工作。
- cooldown 中：interaction/container/listener 相关 info/debug 有显示基础信息。
- matcher 未启用 / 模板缺失：interactionItem info/debug 已覆盖。
- source 与 consume 不兼容：5.12/5.13/5.14 命令和 debug 已覆盖。
- armor_* + consumeEnabled=true：命令拒绝，运行时失败/诊断路径存在。
- itemSubmit enabled 但无 requirement：enable 命令拒绝，infoAll 可见。
- cleanup 可清理项：cleanup 只检查已加载区块并反馈数量。
- chunk 未加载：cleanup 和 tick 跳过，不强制加载。

### 未覆盖或建议 5.15 补强

| 诊断项 | 当前缺口 | 建议 |
| --- | --- | --- |
| consume plan 不足的具体 slot/requirement | failureReason 是字符串，玩家/debug 可能只能看到摘要 | P1：结构化 `ConsumePlanDiagnostic` |
| itemSubmit requirement 不满足详情 | 有 name 和 lastMatchedCount，但缺“需要/实际/consumeCount”统一视图 | P1：itemSubmit doctor 输出 |
| itemSubmit 与 interactionItem 互斥模式 | infoAll 有提示，device debug 需保证也突出显示 | P1：device debug 统一“当前匹配模式” |
| 门上下半格绑定情况 | handler 支持，但 debug 不一定明确显示“另一半门匹配设备” | P1：interaction debug 加 door half resolver 状态 |
| failChannel 无 receiver/relay/listener | SignalBridge 会记录无 listener，但配置时不直接诊断 receiver/relay | P2：ChannelInspector 统计所有消费者 |
| Web UI 结构化输出 | 当前大多拼聊天文本 | P1：DTO 化 debug/doctor |
| old JSON 兼容状态 | normalized 没有可视化 “哪些字段由默认值补齐” | P2：doctor 增加 schema/default diagnostics |

### 未来 Web UI 需要结构化字段

- `deviceId`、`displayName`、`type`、`position`
- `enabled`、`loaded`、`blockIdMatches`
- `currentMode`：redstone / interactionItem / itemSubmit / container
- `channelStatus`：channel、hasListener、hasReceiver、hasRelay
- `cooldown`：remaining / configured
- `matcherStatus`：enabled、source、matched、matchedCount、failureReason
- `consumeStatus`：enabled、source、count、planOk、shortage
- `itemSubmitStatus`：enabled、requirementCount、requirements[]
- `cleanupStatus`：canCleanup、reason
- `doorHalfStatus`：clickedHalf、boundHalf、resolvedDeviceId

## 8. GUI / Web Admin UI 前置服务层建议

### 总原则

- Web UI 不应直接改 JSON。
- 命令、游戏内工具、Web Admin UI 必须共用服务层。
- Web UI 未来需要 DTO / structured response。
- debug / doctor 输出应结构化，不能只拼聊天文本。
- service 层应返回“业务结果 + 玩家展示文本 + 结构化字段”，命令只负责把结果渲染成聊天文本。

### 建议 service

#### SignalDeviceConfigService

职责：

- 创建/删除/启用/禁用设备。
- 更新名称、基础 channel、坐标索引。
- 调用 store 并保证字段保留。

目前逻辑散落：

- `SignalDeviceCommand`
- `SignalDeviceStore`
- `VirtualBlockDeviceCommand`

优先抽取方法：

- `renameDevice`
- `setEnabled`
- `cleanupLoadedInvalidDevices`
- `resolveDevice`
- `getDeviceSummary`

#### VirtualBlockDeviceConfigService

职责：

- bind/unbind/refresh。
- redstone mode/offChannel。
- blockId/current block diagnostics。

目前逻辑散落：

- `VirtualBlockDeviceCommand`
- `SignalDeviceStore`
- `VirtualBlockDeviceSupport`

优先抽取方法：

- `bindVirtualBlockDevice`
- `refreshVirtualBlockDevice`
- `setRedstoneMode`
- `diagnoseBlockMatch`

#### InteractionItemConfigService

职责：

- interactionItem matcher 模板、source、vanilla policy、feedback、sound、consumeSource、inventoryConsumeOrder。
- 校验 itemSubmit 与单物品 matcher 互斥。

目前逻辑散落：

- `VirtualBlockInteractionItemCommand`
- `SignalDeviceStore`
- `ItemStackMatcherSupport`

优先抽取方法：

- `setMatcherFromHand`
- `setSource`
- `setVanillaPolicy`
- `setFeedback`
- `setConsumeConfig`
- `validateConsumeCompatibility`

#### ItemSubmitConfigService

职责：

- itemSubmit enable/disable。
- requirement add/remove/clear/update。
- consume enable/order/count。
- itemSubmit info/debug DTO。

目前逻辑散落：

- `VirtualBlockItemSubmitCommand`
- `VirtualBlockDeviceInteractionHandler`
- `SignalDeviceStore`

优先抽取方法：

- `addRequirementFromHand`
- `setRequirementCount`
- `setRequirementConsumeCount`
- `enableSubmitMode`
- `getSubmitDiagnostic`

#### ItemStackMatcherConfigService

职责：

- 捕获模板。
- 更新 matchDamage/matchName/matchLore/matchCustomData/matchComponents。
- 更新 countMode。
- 生成 matcher summary 和 mismatch reason。

目前逻辑散落：

- `ItemStackMatcherSupport`
- `ItemStackMatcherCommandSupport`
- command classes

优先抽取方法：

- `captureFromStack`
- `updateOption`
- `updateCountMode`
- `describeMatcher`
- `explainMismatch`

#### ActionRelayConfigService

职责：

- action relay channel/enabled/cooldown/actions 配置。
- action validate 和 preview。

目前逻辑散落：

- relay command/block entity/store 相关类
- `ActionValidator`

优先抽取方法：

- `setRelayChannel`
- `setRelayActions`
- `validateActions`
- `getRelayDebug`

#### RegionConfigService

职责：

- RegionController CRUD。
- enter/exit/stay action 配置。
- target filter 配置。

目前逻辑散落：

- `RegionControllerStore`
- region command/tracker 类

优先抽取方法：

- `createController`
- `setActions`
- `setTargetFilter`
- `getRegionControllerDebug`

#### SignalDebugService

职责：

- 结构化 device debug。
- channel consumer summary。
- block/channel/matcher/consume/container diagnostics。

目前逻辑散落：

- `SignalDeviceCommand`
- `SignalDoctor`
- `SignalChannelInspector`
- 各 command info 方法

优先抽取方法：

- `diagnoseDevice`
- `diagnoseVirtualBlockDevice`
- `diagnoseInteractionItem`
- `diagnoseItemSubmit`
- `diagnoseChannel`

#### SignalHistoryService

职责：

- SignalEventHistory 查询、过滤、分页。
- 按 channel/source/device/player 查询。

目前逻辑散落：

- `SignalEventHistory`
- `SignalChannelInspector`
- `SignalDeviceCommand.history`

优先抽取方法：

- `getRecentEvents`
- `getDeviceHistory`
- `getChannelHistory`
- `appendStructuredEvent`

#### DoctorService

职责：

- 全局诊断。
- 结构化问题列表。
- 严重级别、建议修复命令、受影响对象。

目前逻辑散落：

- `SignalDoctor`
- `SignalDeviceCommand.debug`
- 各功能 command 的错误提示

优先抽取方法：

- `inspectSignalBridge`
- `inspectDevices`
- `inspectVirtualBlockDevices`
- `inspectDataCompatibility`
- `inspectPerformanceRisk`

## 9. Web Admin UI 实时同步前置建议

### 未来需要的事件

| 事件 | 当前数据来源 | 推送建议 |
| --- | --- | --- |
| device created / updated / removed | `SignalDeviceStore.replaceOrAdd`、cleanup | WebSocket + REST list/detail |
| signal emitted | `SignalBridgeServer.emit` / `SignalEventHistory.record` | WebSocket history appended |
| actionRelay executed | `ActionRelayDispatcher` / relay record | WebSocket action event |
| receiver pulse | `SignalReceiverDispatcher` | WebSocket receiver pulse event |
| virtualBlockDevice triggered | VBD dispatcher/handler/container handler | WebSocket device event |
| itemSubmit success/fail | `recordVirtualItemSubmitResult`、interaction handler | WebSocket submit event |
| config changed | service mutation result | WebSocket config changed |
| debug state changed | derived state | 不建议高频推，REST 查询为主 |
| history appended | `SignalEventHistory.record` | WebSocket |
| region enter/exit/stay | `RegionControllerTracker.executeActions` | WebSocket region event |
| future game phase changed | 未来 GameController | WebSocket |

### 需要新增 internal event bus 的位置

当前多数事件是“执行后写 history 或写 store”，没有统一内部事件流。建议新增轻量 internal event bus：

- `SignalInternalEventBus`
- `DeviceConfigChangedEvent`
- `SignalEmittedEvent`
- `ActionRelayExecutedEvent`
- `ReceiverPulseEvent`
- `VirtualDeviceTriggeredEvent`
- `ItemSubmitEvaluatedEvent`
- `RegionTriggeredEvent`

### WebSocket vs REST

适合 WebSocket：

- signal emitted
- actionRelay executed
- receiver pulse
- virtualBlockDevice triggered
- itemSubmit success/fail
- history appended
- config changed
- region enter/exit/stay

适合 REST 查询：

- device list/detail
- listener list/detail
- action relay config
- region controller config
- doctor report
- history pagination
- current debug snapshot

只读状态：

- history
- doctor report
- channel inspector
- current debug snapshot
- runtime cooldown/last result

写操作需要权限和审计：

- 创建设备/删除设备
- 修改 channel
- 修改 actions
- 修改 matcher/source/consume/itemSubmit
- cleanup
- enable/disable
- test emit

Web UI 未来必须记录：

- 操作者
- 来源 IP/session
- 目标 device/listener/region/action
- 修改前后摘要
- 执行结果

## 10. 文档一致性审查

### 当前状态

- `README.md` 顶部版本为 `v1.16.0-consume-submit`。
- `CHANGELOG.md` 顶部为 `v1.16.0-consume-submit`。
- `docs/SIGNAL_BRIDGE.md` 已包含 5.14 consume/itemSubmit 内容。
- `ignore` 主命令已写为不带 count。
- `cooldown` 不跳过 consume、不会解除 `require_item_match` 锁的说明已写入 5.14 相关段落。
- `itemSubmit` 与 `interactionItem matcher` 互斥关系已写入。

### 需要修复或整理的文档问题

| 问题 | 级别 | 说明 | 建议 |
| --- | --- | --- | --- |
| README 5.14 段落大量英文，与项目中文说明风格不完全一致 | P2 | 功能无误，但阅读风格不统一 | 5.15 文档整理时中文化 |
| 历史章节仍保留阶段性旧语义，如早期 “consume 只支持 main_hand” | P2 | 作为历史记录可接受，但读者可能误解当前状态 | 在当前能力总览处强调“以最新章节为准” |
| debug/doctor 文档侧重 SignalBridge listener，未覆盖 5.14 consume/itemSubmit doctor 目标 | P1 | 与后续 GUI 前置目标不匹配 | 扩展 docs 的 doctor 覆盖清单 |
| GUI 计划已有，但缺服务层/DTO/实时同步章节 | P2 | 本报告已提出建议 | 5.15 后续文档补一节 Web Admin UI 架构 |
| 命令反馈规范没有集中成文 | P1 | 多个命令类重复实现反馈文本 | 新增 docs/COMMAND_FEEDBACK_GUIDELINES.md 或 SIGNAL_BRIDGE 子节 |

## 11. 5.15 修复建议清单

### P0 必须在 5.15 修

当前没有确认的 P0 阻断问题。

说明：本轮只做审查，未发现需要立刻修改生产代码的高危 bug。5.14 验收中出现过的高危问题已经在当前代码中有对应修复路径，但需要 P1 回归测试固化。

### P1 建议在 5.15 修

#### P1-1：为 `SignalDeviceData` 字段保留增加回归测试

- 问题：`SignalDeviceData` 字段很多，`SignalDeviceStore` 多个 `withX` 方法手工复制字段。
- 风险：新增命令或 setter 可能再次清空 `itemSubmitRequirements`、matcher feedback、container/itemCondition 等字段。
- 涉及文件：`SignalDeviceData.java`、`SignalDeviceStore.java`
- 修复建议：新增数据复制回归测试，覆盖 interactionItem setter 不清空 itemSubmit，itemSubmit setter 不清空 interactionItem/container/itemCondition。
- 回归测试建议：构造包含所有 5.14 字段的设备，逐个调用 update 方法，断言未相关字段保持。

#### P1-2：拆分 `VirtualBlockDeviceInteractionHandler`

- 问题：一个类承载事件 hook、门半格、source matcher、itemSubmit、consume plan、feedback、SignalBridge emit。
- 风险：未来 5.15/GUI/ConditionEngine 调整时容易破坏锁、cooldown、consume 原子性。
- 涉及文件：`VirtualBlockDeviceInteractionHandler.java`
- 修复建议：拆为 `InteractionTargetResolver`、`InteractionItemEvaluator`、`ItemSubmitEvaluator`、`ConsumePlanBuilder`、`InteractionFeedbackExecutor`。
- 回归测试建议：锁/cooldown/consume/itemSubmit/door half 测试全部通过。

#### P1-3：抽取 InteractionItem 与 ItemSubmit 配置服务

- 问题：命令类直接做业务校验和 store mutation。
- 风险：未来 Web UI 复制逻辑会造成命令和 UI 行为不一致。
- 涉及文件：`VirtualBlockInteractionItemCommand.java`、`VirtualBlockItemSubmitCommand.java`、`SignalDeviceStore.java`
- 修复建议：新增 `InteractionItemConfigService` 和 `ItemSubmitConfigService`，命令只渲染返回结果。
- 回归测试建议：同一 service 被命令调用，验证错误提示和字段保留。

#### P1-4：补齐 device debug / doctor 对 5.14 状态的结构化诊断

- 问题：`SignalDoctor` 主要诊断 listener/channel/action，device debug 仍是聊天文本拼接。
- 风险：管理员和未来 Web UI 很难定位 consume plan 不足、itemSubmit 互斥模式、door half 归一化等问题。
- 涉及文件：`SignalDoctor.java`、`SignalDeviceCommand.java`
- 修复建议：新增 `SignalDebugService`/`DoctorService` DTO。
- 回归测试建议：构造 matcher 缺失、source/consume 不兼容、itemSubmit 无 requirement、blockId 不一致、door half 绑定等场景。

#### P1-5：建立命令反馈一致性检查

- 问题：玩家可见反馈分散，容易出现英文硬编码、内部 enum、内部字段名直出。
- 风险：5.14 已出现过类似问题。
- 涉及文件：所有 signal/device command 类。
- 修复建议：集中 `DisplayNames` 和 `FeedbackBuilder`；内部 enum 必须走 displayName。
- 回归测试建议：扫描关键命令反馈，不出现 `consumeSource:`、`matched_source` 作为主字段。

#### P1-6：补 interaction/itemSubmit 锁和消耗自动化回归

- 问题：5.14 多个验收 bug 都发生在锁、cooldown、consume、原子消耗组合路径。
- 风险：后续重构极易回归。
- 涉及文件：interaction handler、matcher、itemSubmit。
- 修复建议：能单测的纯逻辑先单测；无法轻松单测的保留游戏内回归脚本清单。
- 回归测试建议：3 钻石 + 1 绿宝石原子失败、cooldown 中成功仍消耗、cooldown 中失败仍锁门。

#### P1-7：补旧 JSON 兼容读写测试

- 问题：新增字段依赖 `normalized()` 默认值，但缺少稳定验证。
- 风险：旧 `signal_devices.json` 或手动编辑数据导致运行时行为异常。
- 涉及文件：`SignalDeviceData.java`、`JsonStoreSupport`、`SignalDeviceStore.java`
- 修复建议：建立最小 JSON 样本，覆盖 5.4、5.5、5.8、5.10、5.14 缺字段记录。
- 回归测试建议：load 后不崩溃，默认值符合文档。

#### P1-8：明确 ActionEngine command action 权限和审计边界

- 问题：`ActionEngine` command action 使用高权限 command source 执行，这是功能需要，但 Web UI 化前需要明确权限和审计。
- 风险：Web UI 若暴露 action 配置，会变成高风险管理入口。
- 涉及文件：`ActionEngine.java`、`ActionValidator.java`、`ActionAuditLogger.java`
- 修复建议：文档和 Doctor 中标记高权限 command action；Web UI 写操作必须有权限和审计。
- 回归测试建议：非 OP 不可配置 requiresOp action；执行日志包含来源。

### P2 可放到 6.x / GUI 前修

#### P2-1：设计 `SignalDeviceData` 嵌套 schema v2

- 问题：当前 flat record 难以长期维护。
- 风险：继续加字段成本高。
- 涉及文件：`SignalDeviceData.java`
- 建议：先设计 schema，不在 5.15 直接迁移。

#### P2-2：Web Admin UI DTO 设计

- 问题：当前 domain object 不适合直接给 UI。
- 建议：新增只读 DTO：DeviceSummary、DeviceDetail、MatcherStatus、SubmitStatus、DoctorIssue。

#### P2-3：内部事件总线

- 问题：未来实时同步缺统一事件源。
- 建议：SignalBridge、Store mutation、ActionRelay、RegionController 都投递 internal event。

#### P2-4：REST / WebSocket 分层

- 问题：实时和查询职责需分开。
- 建议：REST 做配置和查询，WebSocket 推送 history/config/runtime event。

#### P2-5：文档语言风格统一

- 问题：README/CHANGELOG 中英文混排。
- 建议：当前能力用中文写主文，命令和 enum 保持 code。

#### P2-6：权限与审计模型

- 问题：Web UI 未来需要明确管理员权限、审计、危险操作确认。
- 建议：定义 AdminPermission、AuditEvent、ConfigChangeLog。

#### P2-7：GUI 工具拆分设计

- 问题：单一 GUI 可能过重。
- 建议：拆分交互条件配置器、物品 matcher 配置器、容器条件配置器、signal 设备配置器、debug/doctor 工具。

## 12. 建议回归测试清单

### signal_emitter

- 红石上升沿触发 channel。
- 交互触发不破坏原版行为。
- 绑定名称、list/info/debug/history 正常。
- 破坏方块后 cleanup 不回归。

### signal_receiver

- 收到 signal 输出红石脉冲。
- 没有 listener 时仍能通过 SignalBridge 被触发。
- pulseTicks 保存和重启恢复。
- enable/disable 正常。

### action_relay

- 收到 signal 执行 command/message/sound/signal actions。
- 没有 listener 时仍能通过 SignalBridge 被触发。
- cooldown 生效。
- signal action 走递归保护。

### virtual_block_device redstone

- 已绑定坐标拉杆 powered 变化触发。
- 普通方块被红石充能触发。
- `redstone_rising` / `redstone_falling` / `redstone_both`。
- `offChannel` / `clearOffChannel` 回退主 channel。
- blockId 不一致不触发。
- 未加载区块跳过。

### BlockState condition

- `open=true`、`waterlogged=true`、`lit=true`、`delay=4`。
- 不支持 property 拒绝。
- 非法 value 拒绝。
- `condition_enter` / `condition_exit` / `condition_both`。
- clearOffChannel 后退出回退主 channel。
- tick 不重新解析 raw condition。

### interaction

- MAIN_HAND 右键触发。
- OFF_HAND 不双触发。
- 箱子/门/按钮/拉杆默认不阻止原版交互。
- 成功触发播放主手挥手动画。
- blockId 不一致、空气、disabled、cooldown 中符合预期。

### container events

- 箱子 open 只在实际打开后触发。
- close 只在对应 screen 关闭后触发。
- content changed 内容变化触发，内容不变不重复触发。
- check interval 和 cooldown 生效。
- 非容器拒绝配置。

### itemCondition

- `slot_empty`。
- `slot_item` at_least/exactly/at_most。
- `total_item`。
- slot 越界拒绝。
- namespaced item id 正常，非法 item id 拒绝。
- refresh/test/list/info/enable/disable/remove/clear。

### ItemStackMatcher

- 从主手捕获模板。
- 从容器槽位捕获模板。
- item id/count/damage/custom name/lore/custom_data/components 按当前支持范围匹配。
- `ignore` 不带 count，显示“不检查数量”。
- 旧 addSlotItem/addTotalItem namespaced item id 不回归。

### interactionItem feedback

- successChannel/failChannel。
- successMessage/failMessage。
- successSound/failSound。
- success/fail 都可挥手。
- cooldown 中不重复 signal/message/sound/额外动画。
- feedback 配置不要求已有 matcher 模板。

### player item source

- `main_hand` 默认兼容。
- `off_hand` 检测副手但仍只处理 MAIN_HAND 右键事件。
- `inventory_contains` 只读主背包/热键栏。
- `ignore` 表示至少存在一个匹配 stack。
- 不读取副手/装备/盔甲，除非 source 显式配置。

### armor source

- `armor_head`。
- `armor_chest`。
- `armor_legs`。
- `armor_feet`。
- `armor_any` 记录实际匹配槽位。
- armor_* source 拒绝 consume。
- require_item_match 对 armor_* 生效。

### consume strategy

- main_hand 按 consumeCount 消耗。
- off_hand 按 consumeCount 消耗。
- inventory_contains 跨 stack 按 consumeCount 消耗。
- inventory consume order：hotbar_first / main_inventory_first。
- armor_* 不消耗。
- creative 玩家处理与文档一致。
- cooldown 中成功仍消耗。

### itemSubmit

- enable 自动关闭单物品 matcher。
- interactionItem enable 在 itemSubmitEnabled=true 时拒绝。
- 所有 enabled requirement 必须同时满足。
- disabled requirement 不参与。
- consume disabled 时只判定不消耗。
- consume enabled 时两阶段 plan/apply。
- 3 钻石 + 1 绿宝石失败不扣任何物品且 require_item_match 锁住。
- 3 钻石 + 2 绿宝石成功扣 3 + 2。
- cooldown 中失败仍锁，成功仍消耗。
- itemSubmit 与 interactionItem consume 不重复扣同一 stack。

### cleanup

- 空气位置 virtual_block_device 可清理。
- blockId 不一致但非空气不自动删除。
- 未加载区块跳过且不强制加载。
- 专用设备破坏后索引清理不回归。

### debug / doctor

- disabled。
- interaction disabled。
- blockId 不一致。
- channel 为空。
- channel 无 listener/receiver/relay。
- cooldown。
- matcher 模板缺失。
- source/consume 不兼容。
- itemSubmit 无 requirement。
- consume plan 不足。
- 门上下半格绑定诊断。
- chunk 未加载。

### 重启保存

- name/channel/offChannel/mode。
- condition。
- interaction。
- interactionItem source/feedback/consume。
- armor source。
- container。
- itemCondition。
- itemSubmit requirements/consumeCount/consumeOrder。

### 旧 JSON 兼容

- 5.4 前无 virtual block 字段。
- 5.5 无 condition 字段。
- 5.6 无 interaction 字段。
- 5.8 无 itemCondition 字段。
- 5.10 无 source/feedback 字段。
- 5.12 无 armor 字段。
- 5.14 无 itemSubmit 字段。

