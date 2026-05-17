# ActionEngine Capability Matrix 8.11

## 范围

8.11 Controlled State Actions 只补 ActionEngine 的状态变量写入动作，不改变已有 command / signal / message / sound action 语义。

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| `state_variable` action type | 已实现 | 结构化状态变量写入动作 |
| `set_variable` | 已实现 | BOOLEAN / INTEGER / STRING |
| `increment_variable` | 已实现 | INTEGER + delta，delta > 0 |
| `decrement_variable` | 已实现 | INTEGER - delta，负数允许 |
| `toggle_boolean` | 已实现 | BOOLEAN true / false 切换 |
| `clear_variable` | 已实现 | clear missing = no-op success |
| `GLOBAL` scope | 已实现 | target 固定为 `global` |
| `PLAYER` scope | 已实现 | 支持 `context_player` / `explicit_target` |
| `createIfMissing` | 已实现 | set 创建指定类型；increment/decrement/toggle 使用 initialValue 或默认值 |
| oldValue / newValue result | 已实现 | `ActionExecutionResult.details` 携带状态变化摘要 |
| WebAdmin SignalListener editor | 已实现 | typed fields，不做 raw JSON editor |
| WebAdmin ActionRelay editor | 已实现 | typed fields，不做 raw JSON editor |
| WebAdmin RegionController editor | 已实现 | enter / exit / stay action typed fields |
| WebAdmin 状态变量列表页 | 已实现 | 只读查看 GLOBAL / PLAYER StateVariable |
| WebAdmin 状态变量详情页 | 已实现 | 查看完整 value、target、version、fingerprint、更新时间 |
| WebAdmin 状态变量过滤 / 搜索 / 刷新 | 已实现 | scope / type / key / targetId，手动刷新 |
| WebAdmin StateVariable read API | 已实现 | no-create snapshot，VIEWER 可读，不写 store |
| Doctor diagnostics | 已实现 | 空 key、类型不匹配、缺玩家上下文、缺 explicit target |
| ConditionEngine read-after-write | 已测试 | action 写入后 condition 可从 snapshot 读取 |

## Operation 语义

| operation | valueType | missing + createIfMissing=false | missing + createIfMissing=true |
| --- | --- | --- | --- |
| `set_variable` | BOOLEAN / INTEGER / STRING | 失败 | 创建并写入 `stateValue` |
| `increment_variable` | INTEGER | 失败 | `stateInitialValue` 或 `0` 后加 delta |
| `decrement_variable` | INTEGER | 失败 | `stateInitialValue` 或 `0` 后减 delta |
| `toggle_boolean` | BOOLEAN | 失败 | `stateInitialValue` 或 `false` 后 toggle |
| `clear_variable` | 不需要 | clear missing no-op success | clear missing no-op success |

## Gate / Runtime 顺序

| 场景 | 8.11 行为 |
| --- | --- |
| list-level gate false | 不执行 action list，不写状态 |
| single action gate false | 跳过当前 action，不写状态 |
| single action gate true | 进入 ActionEngine 执行 |
| ActionRelay manual=true | 沿用旧手动测试语义，绕过冷却、列表 gate 和单条 action gate |
| state action 写入成功 | 后续 condition / gate 可读取最新 StateVariable snapshot |
| state action 写入失败 | 按既有 ActionEngine failure result 返回 |

## WebAdmin typed fields

保存字段：

```text
stateOperation
stateScope
stateTargetMode
stateTargetId
stateKey
stateValueType
stateValue
stateDelta
stateCreateIfMissing
stateInitialValue
```

主 UI 文案使用中文；技术 ID 只作为副文本或内部值。WebAdmin 不提供 raw JSON editor。

## WebAdmin StateVariable visibility

| 能力 | 8.11 行为 |
| --- | --- |
| 导航入口 | `状态变量` / `#/state-variables` |
| 列表 API | `GET /api/webadmin/state-variables` |
| 详情 API | `GET /api/webadmin/state-variables/{id}` |
| 权限 | `VIEWER` 可读 |
| 写入 | 不提供 POST / PATCH / DELETE |
| 缺失 store | 返回空列表，不创建 `state_variables.json` |
| 损坏 store | 返回 degraded 和中文提示，不覆盖文件 |
| PLAYER 展示 | 按 resolved `targetId` 分组，同 key 不同玩家是不同记录 |
| realtime | 复用 action execution 事件做页面刷新提示；不新增 variable changed signal |

## 明确不做

- 不做 GameController。
- 不做 MissionSystem。
- 不做 PhaseController。
- 不做 SignalReceiver gate。
- 不做 Scheduler。
- 不做 failure policy / fallback action / stop-list policy。
- 不做 failure channel。
- 不做 variable changed signal。
- 不做大型 StateVariable editor / management page。
- 不做 ENTITY / BLOCK / DEVICE / REGION / TEAM / GAME StateVariable write scope。
- 不做 raw JSON editor。
- 不跑 MCP scenario。
- 不启动 Minecraft。
- 不生成截图矩阵。

## 测试 / Guard

| 测试 | 覆盖 |
| --- | --- |
| `ControlledStateActionServiceTest` | set / increment / decrement / toggle / clear / targetMode / read-after-write / JSON compatibility |
| `WebAdminControlledStateActionServiceTest` | typed save payload / validation / actionFromEntry / no raw JSON save surface |
| `ConditionActionGateServiceTest` | single action gate false skips state action；ActionRelay manual bypass behavior documented |
| `WebAdminConditionRuntimeDoctorServiceTest` | state action Doctor diagnostics |
| `StabilizationGuardTest.testControlledStateActions811` | docs、runtime markers、WebAdmin typed editor markers、no out-of-scope markers |
| `LocalTestMcpFoundationGuardTest` | MCP 基础边界保持，不进入 StateVariable 写入能力 |
