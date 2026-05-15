# 8.3 Item / Inventory / Container Conditions Current Context

本阶段名称：8.3 Item / Inventory / Container Conditions / 物品、背包、容器条件包。

当前稳定基线：`v1.48.0-condition-state-variables`。

8.3 只在 ConditionEngine 之上增加 condition-safe 的物品快照、背包快照、容器快照、只读 matcher 与条件类型。它不是 runtime integration 阶段，不是 WebAdmin condition editor 阶段，也不是具体玩法阶段。

## 范围

8.3 已实现：

- `ConditionItemStackSnapshot`：物品快照。
- `ConditionInventorySnapshot`：背包快照。
- `ConditionContainerSnapshot`：容器快照。
- `ConditionItemMatcher` / `ConditionItemMatchConfig` / `ConditionItemMatchResult`：condition-safe 只读 matcher。
- `ConditionEvaluationContext` 中的 key-based snapshot map：
  - `itemSnapshots`
  - `inventorySnapshots`
  - `containerSnapshots`
- 7 个新增 condition type：
  - `item_stack_exists`
  - `item_stack_matches`
  - `inventory_contains_item`
  - `inventory_item_count_compare`
  - `container_slot_empty`
  - `container_slot_item_matches`
  - `container_item_count_compare`

## 明确不做

8.3 不做：

- 不接入 VBD runtime。
- 不接入 interactionItem runtime。
- 不接入 itemSubmit runtime。
- 不接入 container runtime。
- 不接入 SignalListener / RegionController / ActionRelay runtime。
- 不读取 live `ItemStack` / `PlayerInventory` / `BlockEntity` / world。
- 不消耗物品。
- 不移动物品。
- 不写 store。
- 不 emit signal。
- 不执行 action。
- 不做 WebAdmin condition editor。
- 不做 WebAdmin API。
- 不做 WebAdmin UI。
- 不做 raw JSON editor。
- 不做任意 NBT path。
- 不做 BlockEntity NBT path。
- 不做通用脚本表达式。
- 不做深层动态路径查询。
- 不做具体逃走中任务、游戏开始/结束/结算、GameController、MissionSystem、PhaseController。
- 不新增 MCP tool。
- 不跑 MCP scenario。
- 不生成截图。
- 不启动 Minecraft。

## Snapshot 语义

Runtime 以后负责把 live Minecraft 对象转换成 snapshot。ConditionEngine 只读取 `ConditionEvaluationContext` 中已经给出的 snapshot，不直接读取 live world、live inventory 或 live container。

`ConditionItemStackSnapshot` 的 P0 字段：

- `itemId`
- `count`
- `displayName` 预留
- `lore` 预留
- `customData` 预留
- `components` 预留

8.3 matcher 只启用 condition-safe P0 字段：

- `itemId` equals
- `count` compare

`displayName` / `lore` / `customData` / dataComponent-like 简化字段已在 snapshot 中预留，但 8.3 不启用匹配。后续启用时也只能匹配 snapshot 中已抽出的简单字段，不允许任意 NBT path 或深层动态路径查询。

## Empty Item 语义

以下情况视为空物品：

- `itemId` 为空。
- `itemId == minecraft:air`。
- `count <= 0`。

`item_stack_exists` 判断的是指定 `itemKey` 存在且不是空物品，不只是 key 存在。

## Slot / Count 语义

- slot index 使用 0-based。
- invalid slot 包括负数和超出 snapshot slot size。
- inventory/container 总数统计会跨多个 slot 聚合 stack count。
- count compare 统一使用：
  - `eq`
  - `ne`
  - `gt`
  - `gte`
  - `lt`
  - `lte`
- count 不允许小于 0。

## 与 7.x Runtime 的关系

8.3 没有修改 7.x runtime：

- 没有改变 7.11 itemSubmit requirement 的 all-or-nothing / staged consume 语义。
- 没有改变 itemSubmit requirement list 的保存、读取、回显、consumeCount 联动/解耦语义。
- 没有改变 interaction item matcher 的保存、回显、匹配语义。
- 没有改变 container template / itemConditions 现有行为。

本阶段没有复用 live `ItemStackMatcher.matches`，也没有调用 `ItemSubmitEvaluator`、`ConsumePlanner` 或 `ContainerItemConditionSupport`。8.3 只抽象 condition-safe snapshot matcher，避免旧 runtime 行为被条件层改变。

## 中文 Metadata

所有新增 condition type 都有：

- 中文显示名。
- 中文描述。
- 中文字段名。
- 中文 validation error。
- 中文 failureReason。

英文 type id 仅用于存储、API 兼容和代码，不作为用户可见主文案。

## 测试矩阵

`ConditionItemInventoryContainerTest` 覆盖：

- empty item / non-empty item。
- `minecraft:air` 语义。
- `count <= 0` 语义。
- itemId match / mismatch。
- count `eq/ne/gt/gte/lt/lte`。
- invalid operator / invalid count / invalid itemId。
- item_stack_exists 正反例、missing snapshot、wrong snapshot type。
- item_stack_matches 正反例、empty item、air、missing snapshot、wrong snapshot type、empty matcher。
- inventory_contains_item 聚合、empty inventory、missing inventory、wrong snapshot type。
- inventory_item_count_compare 所有 operator、聚合、empty inventory、missing/wrong snapshot。
- container_slot_empty 正反例、负数 slot、越界 slot、missing/wrong snapshot。
- container_slot_item_matches 正反例、empty slot、air、invalid slot、missing/wrong snapshot。
- container_item_count_compare 所有 operator、聚合、empty container、missing/wrong snapshot。
- AND / OR / NOT / nested / disabled node 集成。
- evaluation 不修改 item / inventory / container snapshot。
- repeated evaluation result stable。
- 中文 metadata / validation / failureReason。

## Guard

8.3 guard marker：

- 8.3 context exists marker。
- 8.3 capability matrix marker。
- item / inventory / container snapshot model marker。
- condition-safe matcher marker。
- immutable/equivalent immutable snapshot marker。
- `item_stack_exists` marker。
- `item_stack_matches` marker。
- `inventory_contains_item` marker。
- `inventory_item_count_compare` marker。
- `container_slot_empty` marker。
- `container_slot_item_matches` marker。
- `container_item_count_compare` marker。
- empty item / `minecraft:air` / `count <= 0` marker。
- 0-based slot marker。
- invalid slot negative / out-of-range marker。
- aggregate count across slots marker。
- `eq/ne/gt/gte/lt/lte` marker。
- Chinese metadata / validation / failure reason marker。
- no NBT path marker。
- no BlockEntity NBT path marker。
- no live world / live inventory / live container reads marker。
- no runtime integration marker。
- no WebAdmin condition editor / API / UI marker。
- no concrete task marker。
- no MCP scenario / no screenshot / no Minecraft startup marker。

## 后续

- 8.4：Region / Signal / Logic Chain 条件可在 snapshot/context 基础上继续扩展。
- 8.5：WebAdmin condition editor 另行设计，不在 8.3 中塞 raw JSON editor。
- 8.6 / 8.7：未来再把 VBD、interactionItem、itemSubmit、container、SignalListener、RegionController、ActionRelay 等 runtime 接入 ConditionEngine。
