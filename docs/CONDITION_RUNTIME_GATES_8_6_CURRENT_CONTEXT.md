# 8.6 Condition Runtime Gates Current Context

阶段名称：8.6 Runtime Integration I / VBD、itemSubmit、container 条件门禁。

## 目标

8.6 在 8.5 WebAdmin Condition Editor 已能创建、编辑、校验、预览 Condition Group 的基础上，第一次把条件组接入低层运行时触发源。本阶段只做外层 gate，不改变旧触发逻辑本身。

本阶段目标：

- virtual_block_device 红石 / BlockState / 玩家右键交互 condition gate。
- itemSubmit condition gate。
- container open / close / change condition gate。
- runtime `ConditionEvaluationContext` 与安全 snapshot 构造。
- Condition Group compatibility / available list。
- WebAdmin VBD 原生触发配置中的最小 condition group picker。
- 测试、guard、文档。

## 不做内容

8.6 不做：

- SignalListener condition gate。
- ActionRelay condition gate。
- RegionController enter / exit / stay condition gate。
- Action 单条 action condition gate。
- GameController / MissionSystem / PhaseController。
- 具体任务 / 关卡。
- raw JSON editor、任意 NBT path、通用脚本表达式。
- MCP scenario、Minecraft 启动、截图矩阵。

## Optional Gate 原则

条件组是可选配置，不是强制项。

`conditionGroupId == null / empty / unset` 时必须：

- 不创建 `ConditionEvaluationContext`。
- 不读取 `condition_groups.json`。
- 不调用 `ConditionEvaluator`。
- 不改变旧 JSON 默认行为。
- 不改变旧触发顺序、冷却、历史、itemSubmit consume、container open / close / change 语义。
- 直接走原有逻辑。

## 外层 Gate 设计

配置了 `conditionGroupId` 后，运行时结构必须是：

```text
触发事件发生
→ 外层 ConditionGateService
→ false：阻断，不进入旧副作用逻辑
→ true：调用旧逻辑原流程
```

禁止把 condition 判断散落进旧逻辑内部；禁止为 condition 改写 consume、container change 检测、SignalBridge emit、cooldown、interval、lastResult 或旧输入参数。

## Runtime Context Profile

8.6 使用 targetType/profile 描述每类触发方式可提供的上下文。可用性宁可保守，不把不适配的条件组显示为可选。

| targetType | 可提供能力 | 不提供能力 |
| --- | --- | --- |
| `VBD_REDSTONE` | sourceType/sourceId/deviceId/world/channel、gameTime、GLOBAL state variable snapshot、event metadata `trigger` / `detail` | player、context_player、item、inventory、container、region、signal history、logic chain |
| `VBD_BLOCKSTATE` | sourceType/sourceId/deviceId/world/channel、gameTime、GLOBAL state variable snapshot、event metadata `trigger` / `detail` | player、context_player、item、inventory、container、region、signal history、logic chain |
| `VBD_INTERACTION` | base context、event metadata `trigger` / `detail` / `hand` / `side`、player context、held item snapshot `held_item` / `main_hand` / `off_hand`、player inventory snapshot `player_inventory`、GLOBAL / PLAYER context_player state variable snapshot | container、region、signal history、logic chain |
| `ITEM_SUBMIT` | base context、event metadata `trigger` / `detail` / `hand` / `side` / `itemSubmit`、player context、submitted item snapshot `submitted_item`、hand snapshots `main_hand` / `off_hand` / `held_item`、player inventory snapshot `player_inventory`、GLOBAL / PLAYER context_player state variable snapshot、itemSubmit metadata | container、region、signal history、logic chain |
| `CONTAINER_OPEN` | base context、player context、GLOBAL / PLAYER context_player state variable snapshot；当绑定目标可解析为已加载且 blockId 匹配的 `Inventory` 容器时，额外提供 container snapshot `container` | 非 `Inventory`、目标缺失、世界/区块未加载、方块不匹配时不提供 container snapshot；submitted item、logic chain、signal history、region |
| `CONTAINER_CLOSE` | base context、player context if stable close actor exists、GLOBAL / PLAYER context_player state variable snapshot when player exists；当绑定目标可解析为已加载且 blockId 匹配的 `Inventory` 容器时，额外提供 container snapshot `container` | 非 `Inventory`、目标缺失、世界/区块未加载、方块不匹配时不提供 container snapshot；submitted item、logic chain、signal history、region |
| `CONTAINER_CHANGE` | base context、event metadata `trigger` / `detail` / `container`、container snapshot `container`、GLOBAL state variable snapshot | player/context_player、item/inventory unless explicitly supplied, logic chain/signal/region |

对 profile 不确定的能力，默认不显示。8.6 的 container change 不提供触发玩家，因此不会把 player conditions 或 `targetMode=context_player` 的条件组列为可选。8.6 的 container open / close 必须动态检查 `targetId` 对应 VBD 绑定目标；只有真实 `Inventory` 容器才向 picker 暴露 `container_*` 条件，非 `Inventory` 或无法构造 snapshot 时继续隐藏并由后端保存校验拒绝。

## Snapshot Boundary

ConditionEngine 只能接收 condition-safe snapshot：

- `ItemStack` -> `ConditionItemStackSnapshot`。
- player inventory -> `ConditionInventorySnapshot`。
- container inventory -> `ConditionContainerSnapshot`。

Gate 评估时不得把 live `ItemStack`、`Inventory`、screen handler 或世界容器对象传入 ConditionEngine。缺失上下文必须安全失败，并给中文 failureReason。

## Compatibility / Available List

WebAdmin 选择条件组时必须只返回 compatible groups。后端保存时也必须验证 compatibility，不能只靠前端隐藏。

兼容性分析要求：

- 递归分析 root group、AND、OR、NOT、nested group。
- disabled node 在 8.6 中不参与 runtime evaluation，compatibility 也忽略 disabled node。
- `player_*` 条件需要 `PLAYER_CONTEXT`。
- `STATE_VARIABLE` 中 `targetMode=context_player` 需要 `PLAYER_CONTEXT`。
- `scope=GLOBAL` 只需要 state variable snapshot 能力。
- `scope=PLAYER` + `targetMode=explicit_target` 不需要 context player，但需要显式 `targetId`。
- `item_stack_*` 要求 profile 提供对应 `itemKey`。
- `inventory_*` 要求 profile 提供对应 `inventoryKey`。
- `container_*` 要求 profile 提供对应 `containerKey`。
- `region_*`、`signal_*`、`logic_chain_*` 要求 profile 提供对应 snapshot key。
- `context_equals` / `context_field_exists` 尽量按 profile 已知字段过滤；无法静态判定时允许保守拒绝并说明原因。
- `always_true` / `always_false` 对所有 profile 可用。

## Available List API

8.6 增加只读 API：

```text
GET /api/webadmin/condition-groups/available?targetType=<targetType>&targetId=<optional>
```

返回内容包括：

- `targetType`
- profile capability summary
- compatible groups
- incompatible count
- optional incompatible reasons

该 API 只读，VIEWER 可访问，不泄露内部 Java 类。

## WebAdmin 配置入口

VBD 原生触发配置中为每个相关 trigger 提供最小 condition group picker。

要求：

- 显示“未配置条件组 = 不拦截，保持旧逻辑”。
- picker 只列 compatible groups。
- 不兼容 group 不出现在可选列表。
- 无可用条件组时显示“暂无适用于此触发方式的条件组”。
- 保存继续使用已有 VBD 原生触发配置写链：permission、CSRF、same-origin、edit lock、expectedFingerprint、audit、realtime。
- 保存失败显示中文错误，不清空草稿。

## Failure Behavior

配置了 condition group 时：

- group missing：安全失败，不放行，中文原因。
- group disabled / invalid / validation failed：安全失败，不放行，中文原因。
- incompatible group：安全失败，不放行，中文原因。
- evaluation exception：安全失败，不放行，中文原因。
- evaluation false：阻断，不进入旧副作用逻辑。
- evaluation true：放行，进入旧流程。

失败时不得：

- emit signal。
- consume item。
- move item。
- execute action。
- 写状态变量。
- 改写旧设备配置。
- 改变旧触发输入参数。

MVP 默认只记录 debug/history 可读原因；有玩家场景可选发送中文提示，但不做 failure sound / failure channel / fallback action。

## Runtime Attach Plan

8.6 允许接入：

- VBD redstone / blockstate。
- VBD interaction。
- itemSubmit。
- container open / close / change。

8.6 明确不接入：

- SignalListener。
- ActionRelay。
- RegionController。
- Action。
- GameController / MissionSystem / PhaseController。

## 测试矩阵

必须覆盖：

- no conditionGroupId -> skipped -> 不读 store、不构造 context、不 evaluate、旧逻辑继续。
- condition true -> 原 interaction / itemSubmit / container / VBD emit 路径继续。
- condition false -> 不 emit、不 consume、不执行 action。
- missing / invalid group -> 安全失败，中文 reason。
- EvaluationContext builder 只输出 snapshot，不传 live runtime object。
- compatible group 出现在 available list。
- incompatible group 不出现在 available list。
- backend reject incompatible bind。
- WebAdmin picker 使用 available list，保存失败不清空输入。
- guard 保持 no SignalListener / ActionRelay / RegionController / Action / GameController / MissionSystem。
- README / capability matrix / stabilization guard 更新。

## Deferred

后续阶段再做：

- SignalListener gate。
- ActionRelay / Action gate。
- RegionController enter / exit / stay gate。
- 更完整 live state variable service integration。
- 更完整 region / signal history / logic chain runtime snapshot。
- failure sound / failure channel / fallback action。
- 任务系统与关卡系统。
