# 8.11 Controlled State Actions 当前上下文

## 8.11 目标

8.11 Controlled State Actions 的目标是给 ActionEngine 增加受控、结构化、可验证的状态变量写入动作。

本阶段新增的用户能力是：

```text
状态变量写入动作
```

它让 SignalListener、ActionRelay、RegionController 的 action list 可以通过 ActionEngine 写入 `StateVariableService` 管理的 `GLOBAL` / `PLAYER` StateVariable。ConditionEngine 仍只读；状态写入完成后，后续 condition / gate / debugger 从最新 snapshot 读取这些值。

## Action Type

8.11 使用单一 action type：

```text
state_variable
```

具体动作由结构化字段 `stateOperation` 区分，不把 JSON、脚本、表达式或 NBT path 塞进旧 `value` 字段。

支持的 operation：

| operation | 中文 | 语义 |
| --- | --- | --- |
| `set_variable` | 设置变量 | 写入 BOOLEAN / INTEGER / STRING 值 |
| `increment_variable` | 增加整数变量 | INTEGER 当前值 + delta |
| `decrement_variable` | 减少整数变量 | INTEGER 当前值 - delta |
| `toggle_boolean` | 切换布尔变量 | BOOLEAN true / false 互换 |
| `clear_variable` | 清除变量 | 删除变量；clear missing 为 no-op success |

## Scope

8.11 只支持现有 StateVariable 范围：

```text
GLOBAL
PLAYER
```

不扩展 ENTITY / BLOCK / DEVICE / REGION / TEAM / GAME 写入 scope。

## targetMode

`GLOBAL` 只允许：

```text
targetMode = global
```

`PLAYER` 支持：

```text
context_player
explicit_target
```

`PLAYER + context_player` 使用当前 ActionContext 中的触发玩家。SignalListener 和 ActionRelay 通常没有玩家上下文；Region enter / exit / stay 通常有玩家上下文。如果运行时没有玩家，动作安全失败并返回中文 failureReason，不写状态。

`PLAYER + explicit_target` 使用结构化字段 `stateTargetId`。目标为空时 validation / runtime fail closed。

## createIfMissing

`set_variable`：

- 变量存在：要求 `stateValueType` 与现有类型一致，然后写入新值。
- 变量不存在且 `createIfMissing=false`：失败，中文原因。
- 变量不存在且 `createIfMissing=true`：按 `stateValueType` 创建并写入。

`increment_variable` / `decrement_variable`：

- 只支持 INTEGER。
- `delta` 必须大于 0。
- 变量不存在且 `createIfMissing=true`：以 `stateInitialValue` 或 `0` 为基础，再执行增减。
- 负数结果允许；整数溢出失败且不写入。

`toggle_boolean`：

- 只支持 BOOLEAN。
- 变量不存在且 `createIfMissing=true`：以 `stateInitialValue` 或 `false` 为基础，再 toggle。

`clear_variable`：

- 变量存在：删除变量。
- clear missing：变量不存在时 no-op success，`changed=false`。

## Runtime 边界

- `ActionEngine` 执行 `state_variable` 时调用 `StateVariableStore.mutate` / `StateVariableService`。
- `StateVariableService` 在同一路径锁内完成读取、校验、计算、写入。
- `ActionExecutionResult` 携带 operation、scope、target、key、oldValue、newValue、failureReason 和 duration。
- `ConditionEngine` 只读取 `StateVariableSnapshot`，不写状态，不 emit signal，不执行 action。
- list-level gate 仍先执行。
- single action gate 仍在单条 action 前执行；gate false 时 state action 不写变量。
- state action 失败只按既有 ActionEngine action list 失败语义返回，不新增 failure policy / fallback action / stop-list policy。
- 本阶段不做 variable changed signal，也不因变量变化 emit signal。
- ActionRelay 手动测试路径沿用旧语义：manual=true 时绕过冷却、列表 gate 和单条 action gate，仅用于管理员测试；正常 runtime 路径仍先过 gate，gate false 不写状态。

## WebAdmin

WebAdmin Action Editor 在以下位置支持状态变量动作：

- SignalListener action editor
- ActionRelay action editor
- RegionController enter / exit / stay action editor

UI 使用结构化字段：

- 操作 operation 下拉。
- 作用域 scope 下拉：GLOBAL / PLAYER。
- targetMode 下拉：global / context_player / explicit_target。
- explicit_target 时显示 targetId 输入。
- key 输入。
- valueType 下拉。
- BOOLEAN 使用下拉，INTEGER 使用 number input，STRING 使用文本输入。
- delta 仅用于 increment / decrement。
- createIfMissing 使用开关。
- initialValue 仅在需要自动创建的整数 / 布尔操作中显示。

WebAdmin 不做 raw JSON editor，保存 payload 使用 typed fields。

### StateVariable 可视化入口

返修后 WebAdmin 增加只读入口：

```text
状态变量
#/state-variables
GET /api/webadmin/state-variables
GET /api/webadmin/state-variables/{id}
```

页面用于确认 Controlled State Actions 写入是否成功，不是大型变量管理器。

列表显示：

- scope：GLOBAL / PLAYER。
- key。
- type：BOOLEAN / INTEGER / STRING。
- value preview。
- targetId：PLAYER 变量按 resolved targetId 分组显示。
- version。
- fingerprint 短值。
- updatedAt / updatedBy。

详情页显示完整 value、scope、key、targetId、type、version、fingerprint、更新时间、存储位置摘要和条件配置提示。当前 record 没有 createdAt，因此详情中明确显示“当前版本未记录”。

支持过滤 / 搜索：

- scope filter。
- type filter。
- key / value / display path 搜索。
- targetId 搜索。
- 手动刷新按钮。

只读边界：

- 不创建变量。
- 不编辑 value。
- 不删除变量。
- 不批量修改。
- 不导入 / 导出。
- 不提供 raw JSON 主 UI。

状态变量读取 API 使用 no-create snapshot 路径；缺失 `state_variables.json` 时返回空列表但不创建文件，损坏文件返回 degraded 状态和中文提示，避免只读页面静默覆盖坏文件。

最低可见性策略是手动刷新后可见。前端也会把现有 action execution realtime 事件作为页面 dirty hint：只有 `state_variable` action 成功且 changed 时刷新状态变量页面；不新增 SignalBridge variable changed signal。

## Doctor / Audit / Debug

Doctor 诊断：

- state action key 为空。
- operation / valueType 配置不匹配。
- `increment_variable` / `decrement_variable` 使用非 INTEGER。
- `toggle_boolean` 使用非 BOOLEAN。
- `PLAYER + context_player` 用在无玩家上下文的 action source。
- `PLAYER + explicit_target` 缺少 targetId。

Action audit / realtime 会显示状态变量动作摘要和执行结果。当前不把状态写入塞进 Condition gate history；Condition replay 仍是只读判断历史。

## 当前不做

- 不做 ENTITY / BLOCK / DEVICE / REGION / TEAM / GAME StateVariable write scope。
- 不做 Scheduler / Delay / Timer。
- 不做 GameController。
- 不做 MissionSystem。
- 不做 PhaseController。
- 不做具体任务 / 关卡。
- 不做 Signal Join 新功能。
- 不做 SignalReceiver gate。
- 不做 failure policy。
- 不做 fallback action。
- 不做 stop-list policy。
- 不做 failure channel。
- 不做 variable changed signal。
- 不做 full StateVariable management page。
- 不做完整 Logic Chain Editor。
- 不做 raw JSON editor。
- 不做任意表达式、脚本或任意 NBT path。
- 不跑 MCP scenario。
- 不启动 Minecraft。
- 不生成截图矩阵。

## 测试方式

以 Java 单元测试、WebAdmin service test、stabilization guard 和 Local Test MCP guard 为主。

本阶段最终验证命令：

```powershell
cd tools\tzz-test-mcp
npm run build
npm test
cd ..\..
.\gradlew.bat clean build
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
git diff --check
```
