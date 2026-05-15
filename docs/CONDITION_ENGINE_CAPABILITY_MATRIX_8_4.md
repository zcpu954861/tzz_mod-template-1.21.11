# ConditionEngine Capability Matrix 8.4

8.4 在 8.0 Core、8.1 基础玩家 / 上下文条件、8.2 状态变量系统和 8.3 物品 / 背包 / 容器 snapshot 条件之上，新增 Region / Signal / Logic Chain snapshot 条件。8.4 仍然只做只读判断能力，不接入 runtime，不提供 WebAdmin condition editor/API/UI。

## 新增 Snapshot

| 能力 | 当前状态 | 说明 |
|---|---|---|
| `ConditionRegionSnapshot` | 已实现 | condition-safe 区域快照，包含 regionId、displayName、enabled、world、playerIdsInside、boundsSummary、metadata。 |
| `ConditionSignalChannelSnapshot` | 已实现 | condition-safe 信号频道快照，包含 consumerCount、enabled/disabled consumer count、actionCount、recentEvents。 |
| `ConditionSignalHistorySnapshot` | 已实现 | condition-safe 信号历史快照，只在 snapshot 内统计事件。 |
| `ConditionSignalEventSnapshot` | 已实现 | 信号事件快照，包含 channel、sourceType、sourceId、gameTime、wallTimeMillis、metadata。 |
| `ConditionLogicChainSnapshot` | 已实现 | condition-safe 逻辑链快照，包含 rootChannel、rootNodeId、nodes、edges、channels、hasCycle、maxDepth。 |
| live RegionController / SignalBridge / SignalEventHistory / Logic Chain Viewer service 查询 | 不做 | condition evaluation 不读取 live service，不自动构建全局逻辑链。 |

## 新增 Condition Type

| type id | 中文名称 | 分类 | 字段 | 说明 | 状态 |
|---|---|---|---|---|---|
| `region_exists` | 区域快照存在 | 区域条件 | `regionKey` 区域快照键 | 检查区域快照是否存在。 | 已实现 |
| `region_enabled` | 区域已启用 | 区域条件 | `regionKey`、`expected` | 检查区域 enabled 状态是否符合预期，expected 默认 true。 | 已实现 |
| `player_in_region` | 玩家在区域内 | 区域条件 | `regionKey`、`playerMode`、`playerId` | 支持 context_player 或 explicit_player。 | 已实现 |
| `region_player_count_compare` | 区域玩家数量比较 | 区域条件 | `regionKey`、`operator`、`count` | 比较区域快照中的玩家数量。 | 已实现 |
| `signal_channel_exists` | 信号频道快照存在 | 信号条件 | `signalChannelKey` | 检查信号频道快照是否存在。 | 已实现 |
| `signal_channel_consumer_count_compare` | 信号消费者数量比较 | 信号条件 | `signalChannelKey`、`operator`、`count` | 比较信号频道快照中的 consumerCount。 | 已实现 |
| `signal_event_count_compare` | 信号事件数量比较 | 信号条件 | `signalHistoryKey`、`channel`、`sourceType`、`sourceId`、`operator`、`count` | 比较信号历史快照中的事件数量，可选过滤。 | 已实现 |
| `logic_chain_contains_node` | 逻辑链包含节点 | 逻辑链条件 | `logicChainKey`、`nodeId` | 检查逻辑链快照是否包含节点 ID。 | 已实现 |
| `logic_chain_contains_channel` | 逻辑链包含频道 | 逻辑链条件 | `logicChainKey`、`channel` | 检查逻辑链快照是否包含频道，包括下游频道。 | 已实现 |
| `logic_chain_has_cycle` | 逻辑链存在循环 | 逻辑链条件 | `logicChainKey`、`expected` | 检查逻辑链 snapshot 是否存在循环；expected 默认 true。 | 已实现 |
| `logic_chain_node_count_compare` | 逻辑链节点数量比较 | 逻辑链条件 | `logicChainKey`、`operator`、`count` | 比较逻辑链快照中的节点数量。 | 已实现 |

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
| missing regionKey / signalChannelKey / signalHistoryKey / logicChainKey | 中文 validation error。 |
| missing playerId / channel / nodeId | 中文 validation error。 |
| invalid operator | 中文 validation error；只允许 `eq/ne/gt/gte/lt/lte`。 |
| invalid count | 中文 validation error；count 必须是大于等于 0 的整数。 |
| invalid boolean expected | 中文 validation error；expected 必须是 true 或 false。 |
| invalid channel | 中文 validation error；channel 必须符合 SignalChannel 规则。 |
| missing snapshot | evaluation 安全失败，中文 failureReason。 |
| wrong snapshot type | evaluation 安全失败，中文 failureReason。 |
| context_player 缺少触发玩家 | evaluation 安全失败，中文 failureReason。 |

## 当前不做

- 不接入 VBD / interactionItem / itemSubmit / container / SignalListener / RegionController / ActionRelay runtime。
- 不修改 SignalBridge runtime。
- 不读取 live world / live player list / live RegionController / live SignalBridge / live SignalEventHistory。
- 不调用 live Logic Chain Viewer service。
- 不自动构建全局逻辑链。
- 不修改 region / signal / logic chain snapshot。
- 不写 store，不 emit signal，不执行 action。
- 不做 WebAdmin condition editor。
- 不做 WebAdmin API。
- 不做 WebAdmin UI。
- 不做 raw JSON editor。
- 不做具体任务 / 关卡。
- 不做 GameController / MissionSystem / PhaseController。
- 不新增 MCP tool。
- 不跑 MCP scenario。
- 不生成截图。
- 不启动 Minecraft。

## 旧语义保护

8.4 没有改变：

- RegionController enter / exit / stay 行为。
- RegionController target filter 行为。
- RegionController WebAdmin 编辑行为。
- SignalBridge emit / listener / cooldown / history 行为。
- SignalEventHistory 记录语义。
- Logic Chain Viewer 节点、边、循环、自循环、多上游、下游频道卡片语义。
- WebAdmin readonly DTO/API 现有返回语义。

8.4 的 condition 只服务 ConditionEngine snapshot 判断，不调用旧 runtime 或 WebAdmin service 路径。
