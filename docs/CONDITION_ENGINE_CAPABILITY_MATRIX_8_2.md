# ConditionEngine Capability Matrix 8.2

## 当前阶段

8.2 State Variable System / 状态变量系统。

目标：在 8.0 Core 和 8.1 Basic Player / Context Conditions 基础上加入类型化状态变量底座。

## 已实现能力

| 能力 | 当前状态 | 说明 |
| --- | --- | --- |
| State Variable System | 已实现 | `com.zcpu.tzzmod.condition.state` |
| GLOBAL scope | 已实现 | 全局 target 固定为 `global` |
| PLAYER scope | 已实现 | 支持 context_player / explicit_target |
| BOOLEAN | 已实现 | true / false |
| INTEGER | 已实现 | long 整数 |
| STRING | 已实现 | 文本 |
| world-scoped store | 已实现 | `<world-save-root>/tzz/webadmin/state_variables.json` |
| StateVariableService | 已实现 | 基础 set / remove / snapshot |
| fingerprint | 已实现 | 防止覆盖 stale state |
| validation | 已实现 | 中文 validation error |
| condition read snapshot | 已实现 | 只读 `StateVariableSnapshot` |
| evaluation no side effects | 已实现 | condition 不调用 store/service 写入 |

## 新增条件类型

| type id | 中文名称 | 分类 | 字段 | 说明 | 状态 |
| --- | --- | --- | --- | --- | --- |
| `state_variable_exists` | 状态变量存在 | 状态变量条件 | scope, key, targetMode, targetId | 检查状态变量是否存在 | 已实现 |
| `state_variable_bool_equals` | 布尔状态匹配 | 状态变量条件 | scope, key, expected, targetMode, targetId | 检查布尔状态是否匹配 | 已实现 |
| `state_variable_int_compare` | 整数状态比较 | 状态变量条件 | scope, key, operator, value, targetMode, targetId | eq/ne/gt/gte/lt/lte | 已实现 |
| `state_variable_string_equals` | 文本状态匹配 | 状态变量条件 | scope, key, value, ignoreCase, targetMode, targetId | 文本精确匹配 | 已实现 |
| `state_variable_string_contains` | 文本状态包含 | 状态变量条件 | scope, key, value, ignoreCase, targetMode, targetId | 文本包含判断 | 已实现 |

## 状态变量模型

| scope | type | key 示例 | 用途 | 当前状态 |
| --- | --- | --- | --- | --- |
| GLOBAL | BOOLEAN | `game.active` | 全局开关 | 已实现 |
| GLOBAL | INTEGER | `mission.count` | 全局计数 | 已实现 |
| GLOBAL | STRING | `mission.phase` | 全局阶段文本 | 已实现 |
| PLAYER | BOOLEAN | `player.certified` | 玩家认证状态 | 已实现 |
| PLAYER | INTEGER | `player.score` | 玩家计数状态 | 已实现 |
| PLAYER | STRING | `player.role` | 玩家文本状态 | 已实现 |

## TargetMode

| targetMode | 中文名称 | 适用 scope | 当前状态 |
| --- | --- | --- | --- |
| `global` | 全局 | GLOBAL | 已实现 |
| `context_player` | 触发玩家 | PLAYER | 已实现 |
| `explicit_target` | 显式目标 | PLAYER | 已实现 |

## 中文诊断

8.2 所有状态变量条件均提供：

- 中文显示名
- 中文描述
- 中文字段名
- 中文失败原因
- 中文 validation error
- category：状态变量条件

典型失败原因：

- `状态变量不存在：global.game.active。`
- `状态变量类型不匹配：global.mission.count 期望 布尔，实际 整数。`
- `上下文缺少触发玩家，无法读取玩家状态变量：player.certified。`
- `整数状态不满足：global.mission.count 当前 2，要求 >= 3。`

## 安全边界

| 边界 | 当前状态 |
| --- | --- |
| 无 runtime integration | 已确认 |
| 无 WebAdmin condition editor | 已确认 |
| 无 State Variable WebAdmin 页面/API | 已确认 |
| 无 GameController / MissionSystem | 已确认 |
| 无具体任务 / 关卡 | 已确认 |
| 无 item / inventory / container conditions | 已确认 |
| missing player safe failure | 已确认 |
| wrong type safe failure | 已确认 |
| 无 State Variable MCP tool | 已确认 |
| 无 MCP scenario requirement | 已确认 |
| 无截图要求 | 已确认 |
| 无 Minecraft 启动要求 | 已确认 |

## 测试覆盖

| 测试 | 覆盖 |
| --- | --- |
| `ConditionStateVariableTest.testStoreAndService` | create/update/delete/snapshot/fingerprint/validation |
| `testExistsAndBooleanConditions` | exists / bool / targetMode |
| `testIntegerCompareConditions` | eq/ne/gt/gte/lt/lte |
| `testStringConditions` | string equals / contains / ignoreCase |
| `testMissingAndTypeMismatchSafeFailures` | missing player / wrong type / missing variable |
| `testInvalidConfigValidation` | scope / key / targetMode / boolean / int / string validation |
| `testGroupIntegrationAndNoSideEffects` | AND / OR / NOT / no side effects |

## 后续规划

| 阶段 | 方向 | 说明 |
| --- | --- | --- |
| 8.3 | item / inventory / container conditions | 当前 8.2 不做 |
| 8.4 | region / signal / logic chain conditions | 当前 8.2 不做 |
| 8.5 | WebAdmin condition editor | 当前 8.2 不做 |
| 后续 | GameController / MissionSystem / PhaseController | 未来使用变量和条件搭建玩法 |
