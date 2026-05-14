# ConditionEngine 能力矩阵 8.1

8.1 在 8.0 ConditionEngine Core 上新增基础玩家 / 上下文条件包。ConditionEngine 仍无副作用、无 runtime integration、无 WebAdmin editor。

## 已实现能力

| type id | 中文名称 | 分类 | 字段 | 说明 | 状态 |
| ------- | ---- | -- | -- | -- | -- |
| `always_true` | 永远通过 | 调试条件 | 无 | 总是通过，用于测试和占位。 | 已实现 |
| `always_false` | 永远失败 | 调试条件 | 无 | 总是失败，用于测试和占位。 | 已实现 |
| `context_exists` | 上下文存在 | 上下文条件 | 无 | 检查 EvaluationContext 是否存在。 | 已实现 |
| `context_field_exists` | 上下文字段存在 | 上下文条件 | `field` / 上下文字段 | 检查上下文字段是否存在且非空。 | 已实现 |
| `context_equals` | 上下文字段匹配 | 上下文条件 | `field` / 上下文字段；`expected` / 期望值 | 检查上下文字段是否匹配固定值。 | 已实现 |
| `group` | 条件组 | 组合条件 | `groupMode` / 组合方式；`children` / 子条件 | AND / OR / NOT 对应全部满足、任意满足、条件取反。 | 已实现 |
| `player_exists` | 触发玩家存在 | 玩家条件 | 无 | 检查上下文是否包含触发玩家身份。 | 已实现 |
| `player_online` | 玩家在线 | 玩家条件 | 无 | 检查玩家在线快照。 | 已实现 |
| `player_is_op` | 玩家是管理员 | 玩家条件 | `expected` / 期望结果 | 检查 OP / creative level 2 快照，默认期望 true。 | 已实现 |
| `player_has_tag` | 玩家拥有标签 | 玩家条件 | `tag` / 标签 | 检查玩家 command tag 快照。 | 已实现 |
| `player_lacks_tag` | 玩家没有标签 | 玩家条件 | `tag` / 标签 | 检查玩家缺少指定 command tag。 | 已实现 |
| `player_team_equals` | 玩家队伍匹配 | 玩家条件 | `team` / 队伍 | 检查玩家 scoreboard team 名称快照。 | 已实现 |
| `player_gamemode_equals` | 玩家游戏模式匹配 | 玩家条件 | `gamemode` / 游戏模式 | 支持 survival / creative / adventure / spectator。 | 已实现 |
| `player_alive` | 玩家存活 | 玩家条件 | 无 | 检查玩家存活快照。 | 已实现 |
| `player_dead` | 玩家死亡 | 玩家条件 | 无 | 检查玩家死亡快照。 | 已实现 |
| `source_type_equals` | 来源类型匹配 | 上下文条件 | `sourceType` / 来源类型 | 检查事件来源类型。 | 已实现 |
| `source_id_equals` | 来源 ID 匹配 | 上下文条件 | `sourceId` / 来源 ID | 检查事件来源 ID。 | 已实现 |
| `channel_equals` | 信号频道匹配 | 上下文条件 | `channel` / 信号频道 | 使用 SignalChannel.normalize / validation。 | 已实现 |
| `world_equals` | 世界匹配 | 上下文条件 | `world` / 世界 | 检查 world / dimension id。 | 已实现 |
| `device_id_equals` | 设备 ID 匹配 | 上下文条件 | `deviceId` / 设备 ID | 检查上下文设备 ID。 | 已实现 |
| `listener_id_equals` | 监听器 ID 匹配 | 上下文条件 | `listenerId` / 监听器 ID | 检查上下文 listener ID。 | 已实现 |
| `region_id_equals` | 区域 ID 匹配 | 上下文条件 | `regionId` / 区域 ID | 检查上下文 region ID。 | 已实现 |
| `action_id_equals` | 动作 ID 匹配 | 上下文条件 | `actionId` / 动作 ID | 检查上下文 action ID。 | 已实现 |
| `game_time_compare` | 游戏时间比较 | 时间条件 | `operator` / 比较方式；`value` / 目标 tick | 支持 eq / ne / gt / gte / lt / lte。 | 已实现 |
| `event_metadata_exists` | 事件元数据存在 | 元数据条件 | `key` / 元数据键 | 检查 eventMetadata key 是否存在且非空。 | 已实现 |
| `event_metadata_equals` | 事件元数据匹配 | 元数据条件 | `key` / 元数据键；`value` / 期望值 | 检查 eventMetadata 值是否匹配。 | 已实现 |

## 中文能力

- 中文失败原因已补齐。
- 条件中文显示名已补齐。
- 条件中文描述已补齐。
- 字段中文名已补齐。
- validation error 使用中文。
- failure reason 使用中文，并包含缺少字段或期望/实际值。

## Validation

- required config 不能为空。
- `player_is_op.expected` 必须是 true / false。
- `player_gamemode_equals.gamemode` 必须是 survival / creative / adventure / spectator。
- `channel_equals.channel` 必须通过 SignalChannel 规则。
- `game_time_compare.operator` 必须是 eq / ne / gt / gte / lt / lte。
- `game_time_compare.value` 必须是数字。
- metadata key/value 不能为空。

## Missing Context

- 缺上下文安全失败。
- 缺触发玩家安全失败。
- 缺 channel / world / source / id 安全失败。
- 缺 event metadata 安全失败。
- `player_lacks_tag`、`player_dead` 等负向条件不会因为缺 player 而通过。

## 无 runtime integration

8.1 不接入现有运行时：

- 不接入 VBD runtime。
- 不接入 SignalListener runtime。
- 不接入 RegionController runtime。
- 不接入 ActionRelay runtime。
- 不接入 itemSubmit runtime。
- 不改 SignalBridge runtime。

## 未做能力

- 无 WebAdmin editor。
- 无 raw JSON editor。
- 无 State Variable System。
- 无 item / inventory / container conditions。
- 无区域人数聚合。
- 无任务阶段条件。
- 无多人聚合条件。
- 无 GameController / MissionSystem / PhaseController。
- 无具体逃走中任务/关卡。
- 无 new MCP tool。
- 无 MCP scenario requirement。
- 无截图要求。
- 无 Minecraft 启动要求。

## 后续规划

- 8.2：State Variable / scoreboard-like state。
- 8.3：Item / inventory / container conditions。
- 8.4：Region / Signal condition enhancements。
- 8.5：WebAdmin condition editor。
