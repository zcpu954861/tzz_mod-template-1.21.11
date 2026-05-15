# 8.4 Region / Signal / Logic Chain Conditions Current Context

本阶段名称：8.4 Region / Signal / Logic Chain Conditions / 区域、信号、逻辑链条件包。

当前稳定基线：`v1.49.0-condition-item-inventory-container`。

8.4 只在 ConditionEngine 之上增加 Region / Signal / Logic Chain 的 condition-safe snapshot、只读 condition type、validation、中文 metadata、中文 failureReason、单元测试和 guard。它不是 runtime integration 阶段，不是 WebAdmin condition editor 阶段，也不是具体玩法阶段。

## 范围

8.4 已实现：

- `ConditionRegionSnapshot`：区域快照。
- `ConditionSignalChannelSnapshot`：信号频道快照。
- `ConditionSignalHistorySnapshot` / `ConditionSignalEventSnapshot`：信号历史快照。
- `ConditionLogicChainSnapshot` / node / edge snapshot：逻辑链快照。
- `ConditionEvaluationContext` 中的 key-based snapshot map：
  - `regionSnapshots`
  - `signalChannelSnapshots`
  - `signalHistorySnapshots`
  - `logicChainSnapshots`
- 11 个新增 condition type：
  - `region_exists`
  - `region_enabled`
  - `player_in_region`
  - `region_player_count_compare`
  - `signal_channel_exists`
  - `signal_channel_consumer_count_compare`
  - `signal_event_count_compare`
  - `logic_chain_contains_node`
  - `logic_chain_contains_channel`
  - `logic_chain_has_cycle`
  - `logic_chain_node_count_compare`

## 明确不做

8.4 不做：

- 不接入 VBD runtime。
- 不接入 interactionItem runtime。
- 不接入 itemSubmit runtime。
- 不接入 container runtime。
- 不接入 SignalListener runtime。
- 不接入 RegionController runtime。
- 不接入 ActionRelay runtime。
- 不读取 live world。
- 不读取 live player list。
- 不读取 live RegionController。
- 不读取 live SignalBridge。
- 不读取 live SignalEventHistory。
- 不调用 live Logic Chain Viewer service。
- 不自动构建全局逻辑链。
- 不写 RegionController store。
- 不写 SignalBridge / listener / device store。
- 不写 Logic Chain Viewer state。
- 不 emit signal。
- 不执行 action。
- 不修改玩家或世界。
- 不做 WebAdmin condition editor。
- 不做 WebAdmin API。
- 不做 WebAdmin UI。
- 不做 raw JSON editor。
- 不做具体逃走中任务、游戏开始/结束/结算、GameController、MissionSystem、PhaseController。
- 不新增 MCP tool。
- 不跑 MCP scenario。
- 不生成截图。
- 不启动 Minecraft。

## Snapshot 语义

Runtime 以后负责把 live RegionController / SignalBridge / SignalEventHistory / Logic Chain Viewer 分析结果转换成 snapshot。ConditionEngine 只读取 `ConditionEvaluationContext` 中已经给出的 snapshot，不直接查询 store、service、server player list 或 world。

### Region snapshot

`ConditionRegionSnapshot` 字段：

- `regionId`
- `displayName`
- `enabled`
- `world`
- `playerIdsInside`
- `boundsSummary`
- `metadata`

`player_in_region` 只检查 snapshot 中的 `playerIdsInside`，不读取在线玩家表，不调用 RegionController。

### Signal snapshot

`ConditionSignalChannelSnapshot` 字段：

- `channel`
- `consumerCount`
- `enabledConsumerCount`
- `disabledConsumerCount`
- `actionCount`
- `recentEvents`

`ConditionSignalHistorySnapshot` 持有 `ConditionSignalEventSnapshot` 列表，`signal_event_count_compare` 只在该列表上统计，可选按 channel / sourceType / sourceId 过滤，不读取 live SignalEventHistory。

### Logic Chain snapshot

`ConditionLogicChainSnapshot` 字段：

- `rootChannel`
- `rootNodeId`
- `nodes`
- `edges`
- `channels`
- `hasCycle`
- `maxDepth`

`logic_chain_has_cycle` 读取 snapshot 中的 cycle 标记，并可基于 snapshot edges 做纯内存循环检测。它不调用 7.15 WebAdmin Logic Chain Viewer service，不现算全局逻辑链。

## Player / Channel / Node 语义

- `player_in_region` 支持：
  - `playerMode=context_player`：使用 EvaluationContext 的触发玩家 ID/name。
  - `playerMode=explicit_player`：使用配置中的 `playerId`。
- context_player 且上下文无玩家时安全失败，中文 failureReason。
- `region_player_count_compare` 统计 snapshot 内玩家数量。
- `signal_channel_consumer_count_compare` 统计 snapshot 内 consumerCount。
- `signal_event_count_compare` 统计 signal history snapshot 内事件数量。
- `logic_chain_contains_node` 只查 snapshot nodes。
- `logic_chain_contains_channel` 查 root channel、channels 集合和节点 channel，可用于下游频道。
- `logic_chain_node_count_compare` 统计 snapshot nodes 数量。

## Count Compare

所有数量比较统一使用：

- `eq`
- `ne`
- `gt`
- `gte`
- `lt`
- `lte`

`count` 必须是大于等于 0 的整数。

## Validation / Failure Reason

覆盖场景：

- missing `regionKey` / `signalChannelKey` / `signalHistoryKey` / `logicChainKey`。
- missing region / signal / history / logic chain snapshot。
- wrong snapshot type。
- missing `playerId` / `channel` / `nodeId`。
- invalid operator。
- invalid count。
- invalid boolean expected。
- invalid channel。

所有 validation error 和 failureReason 都是中文。

示例：

```text
上下文缺少区域快照：region。
快照类型不匹配：region 期望 信号频道快照，实际 区域快照。
上下文缺少触发玩家，无法判断玩家是否在区域内。
区域玩家数量不满足：当前 0，要求 >= 1。
信号事件数量不满足：当前 0，要求 >= 1。
逻辑链不包含节点：action:notify。
```

## 与旧行为关系

8.4 没有修改：

- RegionController enter / exit / stay 行为。
- RegionController target filter 行为。
- RegionController WebAdmin 编辑行为。
- SignalBridge emit / listener / cooldown / history 行为。
- SignalEventHistory 现有记录语义。
- Logic Chain Viewer 的节点、边、循环、自循环、多上游、下游频道卡片语义。
- WebAdmin readonly DTO/API 现有返回语义。

本阶段没有复用 live RegionController / SignalBridge / Logic Chain Viewer service。8.4 新增 condition-safe snapshot，避免条件层改变旧 runtime 或 UI 行为。

## 中文 Metadata

所有新增 condition type 都有：

- 中文显示名。
- 中文描述。
- 中文字段名。
- 中文 validation error。
- 中文 failureReason。

英文 type id 仅用于存储、API 兼容和代码，不作为用户可见主文案。

## 测试矩阵

`ConditionRegionSignalLogicChainTest` 覆盖：

- Region snapshot exists / missing / enabled true / enabled false / player list empty / player list non-empty / player count / world / displayName / snapshot 不可变。
- `region_exists` 正例、missing key、missing snapshot、wrong snapshot type、invalid config、中文失败原因。
- `region_enabled` enabled true / enabled false / expected false / missing snapshot / wrong snapshot type。
- `player_in_region` explicit playerId、context_player、missing player、missing playerId、empty region。
- `region_player_count_compare` 所有 operator、invalid operator、invalid count、empty region、missing/wrong snapshot。
- Signal channel snapshot consumer/enabled/disabled/action/event count、snapshot 不可变。
- `signal_channel_exists` 正例、missing key、missing/wrong snapshot。
- `signal_channel_consumer_count_compare` 所有 operator、zero consumers、missing/wrong snapshot。
- `signal_event_count_compare` 所有 operator、zero/multiple events、channel filter、sourceType/sourceId filter、missing/wrong snapshot。
- Logic Chain snapshot empty/simple/multi-node/downstream channel/cycle true/cycle false/node count/channel count、snapshot 不可变。
- `logic_chain_contains_node` / `logic_chain_contains_channel` / `logic_chain_has_cycle` / `logic_chain_node_count_compare` 的正反例、missing key、missing snapshot、wrong snapshot type。
- AND / OR / NOT / nested / disabled node 集成。
- evaluation 不修改 region / signal / logic chain snapshot。
- repeated evaluation result stable。
- 中文 metadata / validation / failureReason。

## Guard

8.4 guard marker：

- 8.4 context exists marker。
- 8.4 capability matrix marker。
- Region / Signal / Logic Chain condition-safe snapshot marker。
- `region_exists` marker。
- `region_enabled` marker。
- `player_in_region` marker。
- `region_player_count_compare` marker。
- `signal_channel_exists` marker。
- `signal_channel_consumer_count_compare` marker。
- `signal_event_count_compare` marker。
- `logic_chain_contains_node` marker。
- `logic_chain_contains_channel` marker。
- `logic_chain_has_cycle` marker。
- `logic_chain_node_count_compare` marker。
- `eq/ne/gt/gte/lt/lte` marker。
- Chinese metadata / validation / failureReason marker。
- no live world marker。
- no live RegionController marker。
- no live SignalBridge / SignalEventHistory marker。
- no live Logic Chain Viewer service marker。
- no automatic global logic chain build marker。
- no runtime integration marker。
- no WebAdmin condition editor / API / UI marker。
- no concrete task marker。
- no MCP scenario / no screenshot / no Minecraft startup marker。

## 后续

- 8.5：WebAdmin condition editor 另行设计，不在 8.4 中塞 raw JSON editor。
- 8.6 / 8.7：未来再把 VBD、SignalListener、RegionController、ActionRelay、Action、itemSubmit 等 runtime 接入 ConditionEngine。
- GameController / MissionSystem / PhaseController 后续才把条件、状态变量、动作和逻辑链组合成具体玩法。
