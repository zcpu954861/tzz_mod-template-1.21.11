# 8.1 基础玩家 / 上下文条件包 Current Context

阶段名称：8.1 基础玩家 / 上下文条件包（Basic Player / Context Conditions）。

当前稳定基线：`v1.46.0-condition-engine-core`。

建议分支：`feature/condition-basic-player-context`。

建议后续 tag：`v1.47.0-condition-basic-player-context`。

## 阶段定位

8.1 只在 8.0 ConditionEngine Core 上加入第一批基础玩家 / 上下文条件。Condition 仍然只负责判断，不产生副作用。

旧 TZZ 逃走中数据包仍只作为条件复杂度参考。本阶段不复制旧 function、scoreboard、trigger 或任务流程。

## 中文显示规则

每个条件类型必须同时具备：

- 稳定英文 type id，例如 `player_has_tag`。
- 中文显示名，例如 `玩家拥有标签`。
- 中文描述。
- 中文字段名，例如 `tag` 显示为 `标签`。
- 中文失败原因，例如 `玩家缺少标签：runner。`
- 中文 category，例如 `玩家条件`、`上下文条件`、`时间条件`、`元数据条件`。

技术 type id 可以作为副文本保留，但用户可见主文案不能只显示英文 type id。

## 本阶段条件清单

### 玩家条件

- `player_exists`：触发玩家存在。
- `player_online`：玩家在线。
- `player_is_op`：玩家是管理员，字段 `expected` 默认 true。
- `player_has_tag`：玩家拥有标签，字段 `tag`。
- `player_lacks_tag`：玩家没有标签，字段 `tag`。
- `player_team_equals`：玩家队伍匹配，字段 `team`。
- `player_gamemode_equals`：玩家游戏模式匹配，字段 `gamemode`，允许 `survival`、`creative`、`adventure`、`spectator`。
- `player_alive`：玩家存活。
- `player_dead`：玩家死亡。

8.1 不直接读取 live `ServerPlayerEntity`。当前实现使用 `ConditionEvaluationContext` 的玩家快照字段，未来 runtime adapter 负责把真实玩家状态填入上下文。

### 上下文条件

- `source_type_equals`：来源类型匹配，字段 `sourceType`。
- `source_id_equals`：来源 ID 匹配，字段 `sourceId`。
- `channel_equals`：信号频道匹配，字段 `channel`，使用 `SignalChannel.normalize` 和 validation。
- `world_equals`：世界匹配，字段 `world`。
- `device_id_equals`：设备 ID 匹配，字段 `deviceId`。
- `listener_id_equals`：监听器 ID 匹配，字段 `listenerId`。
- `region_id_equals`：区域 ID 匹配，字段 `regionId`。
- `action_id_equals`：动作 ID 匹配，字段 `actionId`。

### 时间 / 元数据条件

- `game_time_compare`：游戏时间比较，字段 `operator` 和 `value`，支持 `eq/ne/gt/gte/lt/lte`。
- `event_metadata_exists`：事件元数据存在，字段 `key`。
- `event_metadata_equals`：事件元数据匹配，字段 `key` 和 `value`。

### 8.0 条件中文补齐

- `always_true`：永远通过。
- `always_false`：永远失败。
- `context_exists`：上下文存在。
- `context_field_exists`：上下文字段存在。
- `context_equals`：上下文字段匹配。
- `group`：条件组；AND / OR / NOT 对应全部满足、任意满足、条件取反。

## EvaluationContext 扩展

8.1 扩展了玩家快照字段：

- `playerOnline`
- `playerOp`
- `playerTags`
- `playerTeam`
- `playerGameMode`
- `playerAlive`

既有字段继续保留：

- `playerId`
- `playerName`
- `worldId`
- `sourceType`
- `sourceId`
- `channel`
- `deviceId`
- `listenerId`
- `regionId`
- `actionId`
- `blockPos`
- `itemStackSummary`
- `triggerType`
- `detail`
- `gameTime`
- `signalDepth`
- `eventMetadata`
- `variables`

本阶段没有引入状态写入，也没有把 ConditionEngine 接入 VBD / SignalListener / RegionController / ActionRelay / itemSubmit runtime。

## Validation / Failure Reason

所有新增条件必须做 validation：

- 必填字段不能为空。
- `expected` 必须为 true / false。
- `gamemode` 必须为 survival / creative / adventure / spectator。
- `channel` 必须通过 SignalChannel 规则。
- `operator` 必须为 eq / ne / gt / gte / lt / lte。
- `gameTime value` 必须是数字。
- metadata key/value 不能为空。

缺上下文、缺 player、缺 channel、缺 world、缺 metadata 均安全失败，不抛异常。失败原因必须使用中文，并说明缺少什么或期望/实际值。

## 不做范围

8.1 不做：

- 不做具体逃走中任务。
- 不做任何游戏关卡。
- 不做 GameController / MissionSystem / PhaseController。
- 不接入 VBD runtime。
- 不接入 SignalListener runtime。
- 不接入 RegionController runtime。
- 不接入 ActionRelay runtime。
- 不接入 itemSubmit runtime。
- 不改 SignalBridge runtime。
- 不做 WebAdmin 条件可视化编辑器。
- 不做 State Variable System。
- 不做物品 / 背包 / 容器条件。
- 不做区域人数聚合。
- 不做任务阶段条件。
- 不做多人聚合条件。
- 不做 raw JSON editor。
- 不做任意 NBT path。
- 不新增 MCP tool。
- 不跑 MCP scenario。
- 不生成截图。
- 不启动 Minecraft。

## 测试方式

本阶段以纯 Java 单元测试和 guard 为主：

- `ConditionBasicPlayerContextTest`
- `ConditionEngineCoreTest`
- `StabilizationGuardTest`
- `LocalTestMcpFoundationGuardTest`

真实 Minecraft 玩家状态需要后续 runtime adapter / integration test 补充。本阶段不启动 Minecraft，不跑 MCP scenario。

## 后续关系

- 8.2 计划：State Variable / scoreboard-like context state。
- 8.3 计划：Item / inventory / container 条件。
- 8.4 计划：Region / Signal 条件增强。
- 8.5 之后：WebAdmin 条件编辑器。

这些后续阶段不属于 8.1。
