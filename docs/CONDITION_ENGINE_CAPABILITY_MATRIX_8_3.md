# ConditionEngine Capability Matrix 8.3

8.3 在 8.0 Core、8.1 基础玩家 / 上下文条件和 8.2 状态变量系统之上，新增物品、背包、容器 snapshot 条件。8.3 仍然只做只读判断能力，不接入 runtime，不提供 WebAdmin condition editor/API/UI。

## 新增 Snapshot / Matcher

| 能力 | 当前状态 | 说明 |
|---|---|---|
| `ConditionItemStackSnapshot` | 已实现 | condition-safe 物品快照；空 itemId、`minecraft:air`、`count <= 0` 视为空物品。 |
| `ConditionInventorySnapshot` | 已实现 | 背包快照；slot 为 0-based；总数统计跨多个 slot 聚合。 |
| `ConditionContainerSnapshot` | 已实现 | 容器快照；slot 为 0-based；负数和越界 slot 均安全失败。 |
| `ConditionItemMatcher` | 已实现 | 只读 matcher；支持 itemId equals 与 count compare。 |
| `ConditionItemMatchConfig` | 已实现 | 包含 itemId、数量比较方式和目标数量。 |
| displayName / lore / customData / dataComponent-like 匹配 | Deferred | snapshot 字段预留，8.3 不启用；后续也只允许匹配已抽出的简单字段。 |
| 任意 NBT path / BlockEntity NBT path / 通用脚本表达式 | 不做 | 8.3 明确禁止。 |

## 新增 Condition Type

| type id | 中文名称 | 分类 | 字段 | 说明 | 状态 |
|---|---|---|---|---|---|
| `item_stack_exists` | 物品快照存在 | 物品条件 | `itemKey` 物品快照键 | 指定 itemKey 存在且不是空物品。 | 已实现 |
| `item_stack_matches` | 物品快照匹配 | 物品条件 | `itemKey`、`itemId`、`countOperator`、`count` | 检查指定物品快照的 itemId 与数量。 | 已实现 |
| `inventory_contains_item` | 背包包含物品 | 背包条件 | `inventoryKey`、`itemId`、`countOperator`、`count` | 聚合背包多个 slot 的目标物品数量并判断。 | 已实现 |
| `inventory_item_count_compare` | 背包物品数量比较 | 背包条件 | `inventoryKey`、`itemId`、`operator`、`count` | 聚合背包目标物品总数，并按 `eq/ne/gt/gte/lt/lte` 判断。 | 已实现 |
| `container_slot_empty` | 容器槽位为空 | 容器条件 | `containerKey`、`slot` | 检查 0-based 指定 slot 是否为空。 | 已实现 |
| `container_slot_item_matches` | 容器槽位物品匹配 | 容器条件 | `containerKey`、`slot`、`itemId`、`countOperator`、`count` | 检查指定容器 slot 的物品和数量。 | 已实现 |
| `container_item_count_compare` | 容器物品数量比较 | 容器条件 | `containerKey`、`itemId`、`operator`、`count` | 聚合容器多个 slot 的目标物品总数并判断。 | 已实现 |

## Count Compare

| operator | 含义 | 状态 |
|---|---|---|
| `eq` | 等于 | 已实现 |
| `ne` | 不等于 | 已实现 |
| `gt` | 大于 | 已实现 |
| `gte` | 大于等于 | 已实现 |
| `lt` | 小于 | 已实现 |
| `lte` | 小于等于 | 已实现 |

## Validation / Failure Reason

| 场景 | 当前行为 |
|---|---|
| missing itemKey / inventoryKey / containerKey | 中文 validation error。 |
| missing matcher / empty matcher | 中文 validation error：物品匹配器为空。 |
| invalid itemId | 中文 validation error；要求命名空间格式且不能是 `minecraft:air`。 |
| invalid slot negative | 中文 validation error；slot 必须是 0-based 非负整数。 |
| invalid slot out of range | evaluation 安全失败，中文 failureReason。 |
| invalid operator | 中文 validation error；只允许 `eq/ne/gt/gte/lt/lte`。 |
| invalid count | 中文 validation error；count 必须是大于等于 0 的整数。 |
| missing snapshot | evaluation 安全失败，中文 failureReason。 |
| wrong snapshot type | evaluation 安全失败，中文 failureReason。 |
| empty item / `minecraft:air` / `count <= 0` | 视为空物品，中文 failureReason。 |

## 当前不做

- 不接入 VBD / interactionItem / itemSubmit / container / SignalListener / RegionController / ActionRelay runtime。
- 不修改 SignalBridge runtime。
- 不读取 live world / live inventory / live container / live BlockEntity。
- 不消耗物品，不移动物品，不修改快照。
- 不做 WebAdmin condition editor。
- 不做 WebAdmin API。
- 不做 WebAdmin UI。
- 不做 raw JSON editor。
- 不做任意 NBT path。
- 不做 BlockEntity NBT path。
- 不做通用脚本表达式。
- 不做具体任务 / 关卡。
- 不做 GameController / MissionSystem / PhaseController。
- 不新增 MCP tool。
- 不跑 MCP scenario。
- 不生成截图。
- 不启动 Minecraft。

## 旧语义保护

8.3 没有改变：

- 7.11 itemSubmit all-or-nothing / staged consume 语义。
- itemSubmit requirement list 保存、读取、回显、consumeCount 语义。
- interaction item matcher 保存、回显、匹配语义。
- container template / itemConditions 现有行为。

8.3 的 matcher 只服务 ConditionEngine snapshot 判断，不调用旧 runtime consume 或 emit 路径。
