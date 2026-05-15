# 8.2 State Variable System Current Context

## 阶段定位

8.2 State Variable System / 状态变量系统建立在 8.0 ConditionEngine Core 和 8.1 Basic Player / Context Conditions 之上。

本阶段只做状态变量底座，用来替代旧数据包里大量 scoreboard fake player、player tag、全局分数、临时状态位的通用能力参考。旧数据包只提供复杂度参考，不把任何具体任务、关卡、流程或 function 迁入模组。

## 核心原则

- 状态变量系统不是 GameController / MissionSystem / PhaseController。
- Condition 只读取状态变量，不写状态变量。
- Condition evaluation 必须无副作用。
- 状态变量写入必须通过专门的 StateVariableService / future StateAction / future WebAdmin API，不允许在 condition evaluation 中写。
- 当前 8.2 不接入 VBD / SignalListener / RegionController / ActionRelay / itemSubmit runtime。
- 当前 8.2 不接入 runtime。
- 当前 8.2 不改 SignalBridge runtime。
- 当前 8.2 不提供 WebAdmin condition editor，不提供 raw JSON editor。
- 当前 8.2 不提供状态变量 WebAdmin 页面/API。

## 数据模型

新增包：

`com.zcpu.tzzmod.condition.state`

核心类型：

- `StateVariableScope`
- `StateVariableType`
- `StateVariableTargetMode`
- `StateVariableCompareOperator`
- `StateVariableKey`
- `StateVariableValue`
- `StateVariableRecord`
- `StateVariableSnapshot`
- `StateVariableValidation`
- `StateVariableStore`
- `StateVariableService`
- `StateVariableUpdateRequest`
- `StateVariableWriteResult`

## Scope

P0 已支持：

| scope | 中文含义 | target 规则 | 当前状态 |
| --- | --- | --- | --- |
| GLOBAL | 全局 | 固定为 `global` | 已实现 |
| PLAYER | 玩家 | player UUID/name 或显式 targetId | 已实现 |

P1 仅预留方向，当前不实现：

- TEAM
- REGION
- DEVICE
- LISTENER
- ACTION
- LOGIC_CHAIN

## Value Type

P0 已支持：

| type | 中文含义 | 当前状态 |
| --- | --- | --- |
| BOOLEAN | 布尔 | 已实现 |
| INTEGER | 整数 | 已实现 |
| STRING | 文本 | 已实现 |

P1 仅预留方向，当前不实现：

- ENUM
- TIMESTAMP
- DECIMAL

## Store

状态变量存储是 world-scoped。

当前路径：

`<world-save-root>/tzz/webadmin/state_variables.json`

实现要点：

- DataFile version
- load / save
- `flushDirty` marker，8.2 写入由 StateVariableService 同步 flush
- corruption safe fallback：坏 JSON / 坏 record 文件安全回退为空快照，不让损坏存储破坏 condition evaluation
- get snapshot
- get variable
- set variable
- remove variable
- 不写 global config/tzz 作为主存储

## EvaluationContext

8.2 在 `ConditionEvaluationContext` 中新增：

- `StateVariableSnapshot stateVariables`

并保留 8.0 / 8.1 既有上下文字段：

- playerId / playerName / playerOnline / playerOp / playerTags / playerTeam / playerGameMode / playerAlive
- worldId
- sourceType / sourceId
- channel
- deviceId / listenerId / regionId / actionId
- blockPos / itemStackSummary
- gameTime
- eventMetadata
- variables

Condition handler 只读取 `StateVariableSnapshot`，不持有 `StateVariableService` 或 `StateVariableStore`。

## 新增条件类型

| type id | 中文名称 | 分类 | 字段 | 说明 | 状态 |
| --- | --- | --- | --- | --- | --- |
| `state_variable_exists` | 状态变量存在 | 状态变量条件 | scope, key, targetMode, targetId | 检查变量是否存在 | 已实现 |
| `state_variable_bool_equals` | 布尔状态匹配 | 状态变量条件 | scope, key, expected, targetMode, targetId | 检查布尔值是否匹配 | 已实现 |
| `state_variable_int_compare` | 整数状态比较 | 状态变量条件 | scope, key, operator, value, targetMode, targetId | 支持 eq/ne/gt/gte/lt/lte | 已实现 |
| `state_variable_string_equals` | 文本状态匹配 | 状态变量条件 | scope, key, value, ignoreCase, targetMode, targetId | 文本精确匹配 | 已实现 |
| `state_variable_string_contains` | 文本状态包含 | 状态变量条件 | scope, key, value, ignoreCase, targetMode, targetId | 文本包含判断 | 已实现 |

## TargetMode

| targetMode | 中文含义 | 规则 |
| --- | --- | --- |
| `global` | 全局 | 只允许 GLOBAL scope，target 固定 `global` |
| `context_player` | 触发玩家 | PLAYER scope 使用 EvaluationContext 中的 playerId / playerName |
| `explicit_target` | 显式目标 | PLAYER scope 使用配置中的 targetId |

missing player safe failure：

`上下文缺少触发玩家，无法读取玩家状态变量：player.certified。`

wrong type safe failure：

`状态变量类型不匹配：global.mission.count 期望 布尔，实际 整数。`

## Validation

状态变量 validation：

- scope 必须合法。
- type 必须合法。
- key 非空。
- key 长度限制。
- key 只能包含小写字母、数字、点、下划线、冒号或短横线。
- key 不允许控制字符。
- PLAYER scope 的显式写入需要 target。
- GLOBAL scope 规范化为 `global`。
- BOOLEAN 只接受 true/false。
- INTEGER 只接受整数。
- STRING 不允许控制字符。
- expectedFingerprint 不匹配时拒绝写入。

condition validation：

- `state_variable_exists` 必须有 scope/key/targetMode。
- `state_variable_bool_equals` 必须有 expected 且 expected 为 boolean。
- `state_variable_int_compare` 必须有 operator/value 且 operator 合法、value 为整数。
- `state_variable_string_equals` 必须有 value。
- `state_variable_string_contains` 必须有 value。
- `targetMode=explicit_target` 时必须有 targetId。
- `GLOBAL` 作用域只能使用 `targetMode=global`。
- `PLAYER` 作用域不能使用 `targetMode=global`。

所有 validation error 为中文。

## 当前不做

- 不做具体逃走中任务。
- 不做任何游戏关卡。
- 不做游戏开始 / 结束 / 结算。
- 不做猎人出生点流程。
- 不做逃走能量系统。
- 不做 OP 计时器。
- 不做 GameController。
- 不做 MissionSystem。
- 不做 PhaseController。
- 不接入 VBD / SignalListener / RegionController / ActionRelay / itemSubmit runtime。
- 不改 SignalBridge runtime。
- 不做 WebAdmin condition editor。
- 不提供状态变量 WebAdmin 页面/API。
- 不做物品 / 背包 / 容器条件。
- 不做区域人数聚合。
- 不做任务阶段条件。
- 不做多人聚合条件。
- 不做 raw JSON editor。
- 不新增 MCP tool。
- 不跑 MCP scenario。
- 不生成截图。
- 不启动 Minecraft。

## 后续路线关系

- 8.3：item / inventory / container conditions。
- 8.4：region / signal / logic chain conditions。
- 8.5：WebAdmin condition editor。
- 后续 GameController / MissionSystem / PhaseController 才把状态变量、条件、动作组合成具体玩法。

## 测试方式

以 Java 单元测试和 guard 为主：

- `ConditionStateVariableTest`
- `StabilizationGuardTest`
- `LocalTestMcpFoundationGuardTest`

本阶段不跑 MCP scenario，不生成截图，不启动 Minecraft。
