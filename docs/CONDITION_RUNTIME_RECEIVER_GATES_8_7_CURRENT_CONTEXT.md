# 8.7 Receiver-side Runtime Gates 当前上下文

## 8.7 目标

8.7 将 Condition Group gate 接到接收端 / 执行端：

- SignalListener 收到 signal 且旧逻辑准备执行 action list 时，先判断可选 `conditionGroupId`。
- ActionRelay 被 signal 触发且旧逻辑准备执行 action list 时，先判断可选 `conditionGroupId`。
- RegionController 的 enter / exit / stay 三类 action list 分别判断独立 gate：`enterConditionGroupId`、`exitConditionGroupId`、`stayConditionGroupId`。

## 8.7 不做内容

本阶段不做单条 Action condition gate，不做 SignalReceiver redstone output gate，不做 GameController / MissionSystem / PhaseController，不做具体任务关卡，不做 Scratch-like editor，不启动 Minecraft，不跑 MCP scenario，不生成截图矩阵。

单条 Action condition gate 继续 deferred：本阶段只做整条 action list 的外层 gate。

## Optional Gate 原则

未配置 conditionGroupId 时保持旧逻辑 100% 不变：

- 不读取 condition group store。
- 不构造 EvaluationContext。
- 不 evaluate。
- 不改变旧 action 执行顺序、cooldown、recursion guard、SignalBridge emit、RegionController tracking 或 stay interval。

配置后也只作为外层 gate：

```text
旧系统判断准备执行 action list
-> ConditionGateService
-> true: 调旧 action list 原流程
-> false: 不执行 action list，不产生 action side effects
```

## SignalListener gate

`SignalListenerData` 新增可选 `conditionGroupId`。SignalBridge 插入点在：

- listener actions 非空之后。
- listener cooldown 检查之后。
- `ActionContext` 构造、`executeListenerActions`、`LAST_TRIGGER_TICKS.put` 之前。

gate false 只跳过当前 listener，继续处理同 channel 的其它 listener；不会执行 command/message/sound/signal action，不会更新该 listener cooldown 时间戳。history 结果文本会附带“条件阻断监听器”数量。

## ActionRelay gate

`ActionRelayBlockEntity` 新增可选 `conditionGroupId`，随 BlockEntity NBT 保存。

插入点在：

- enabled / actions 非空检查之后。
- 非手动 signal 触发 cooldown 检查之后。
- `ActionContext` 构造和 action loop 之前。

gate false 返回中文原因，不执行 relay action list，不 emit 下游 signal，不执行 command/message/sound，不调用 `updateLastRun`，不更新 active 状态、lastResult 或 action relay run history。`/tzz ... action_relay` 手动触发保持旧语义，不走 8.7 gate。

## RegionController enter / exit / stay gate

`RegionControllerData` 新增：

- `enterConditionGroupId`
- `exitConditionGroupId`
- `stayConditionGroupId`

gate 插入点在 RegionController tracking 已更新之后、`ActionEngine.executeAll` 之前。gate false 只阻断对应 action list，不阻断进入/离开/停留状态记录。

stay gate false 后仍会推进 `lastStayTriggerTicks`，避免每 tick 反复 evaluate 同一次 stay 触发窗口；下一次仍按 stay interval 正常检测。

`/tzz regionctl test` 复用同一个 gate-aware helper，避免命令测试绕过 8.7 gate。

## Target profiles / compatibility

新增 target type：

- `SIGNAL_LISTENER`
- `ACTION_RELAY`
- `REGION_ENTER`
- `REGION_EXIT`
- `REGION_STAY`

SignalListener / ActionRelay profile 保守提供：

- source/channel/world/gameTime/signalDepth 等基础上下文。
- listenerId 或 deviceId / relayId / blockPos。
- GLOBAL state variable。
- PLAYER explicit_target state variable。

它们不提供 context_player，也不提供 item / inventory / container / region / signal history / logic chain snapshot。

Region profiles 提供：

- player context。
- GLOBAL state variable。
- PLAYER context_player / explicit_target state variable。
- region snapshot keys：`region`、`current_region`。

Region profiles 不提供 item / inventory / container / signal history / logic chain snapshot。

## WebAdmin 配置入口

WebAdmin 配置入口：

- SignalListener 基础配置 modal 增加 condition group picker，targetType=`SIGNAL_LISTENER`。
- ActionRelay Action 列表 modal 增加 condition group picker，targetType=`ACTION_RELAY`。
- RegionController 配置 modal 增加 enter / exit / stay 三个 picker，分别使用 `REGION_ENTER`、`REGION_EXIT`、`REGION_STAY`。

picker 只显示 compatible groups，并说明“未配置条件组 = 保持旧逻辑，不拦截”。保存仍走 permission / CSRF / same-origin / edit lock / expectedFingerprint / audit / realtime，后端会二次 reject incompatible binding。

## Failure behavior

missing group、disabled group、invalid group、incompatible group、evaluation exception 和 gate false 均 fail closed，并返回中文原因。未配置 gate 不视为失败。

## 测试矩阵

代码层覆盖：

- `ConditionGroupCompatibilityServiceTest` 覆盖 8.7 target profiles。
- `WebAdminConditionGateConfigTest` 覆盖后端 missing / disabled / invalid / incompatible reject 和 8.7 receiver profiles。
- `StabilizationGuardTest` 守卫 runtime 插入点、WebAdmin 字段、frontend picker、docs 和 no out-of-scope。

手工验收重点：

- 未配置 gate 时 SignalListener / ActionRelay / RegionController 行为与旧逻辑一致。
- gate true 时执行旧 action list。
- gate false 时不执行 action list，且 SignalListener 不更新 cooldown、ActionRelay 不更新 lastResult/active/history、RegionController tracking 和 stay interval 不被破坏。
- WebAdmin picker 保存失败时显示中文错误，不清空当前选择。

建议浏览器验收步骤：

1. 新世界启动 WebAdmin，登录具备 EDITOR 或 OWNER 权限的账号。
2. 进入 Condition Groups，准备三类条件组：`always_true`、一个包含玩家条件的组、一个包含容器或物品快照条件的组。不要用 raw JSON 临时绕过校验。
3. 进入 SignalListener 基础配置 modal，确认外层条件组 picker 只列出 `SIGNAL_LISTENER` 兼容组；选择未配置、兼容组、不兼容旧值三类状态时，保存失败或关闭弹窗都不应清空输入。
4. 进入 ActionRelay Action 列表 modal，确认外层条件组 picker 可保存、可清空；清空后请求体中的 `conditionGroupId` 应为空字符串。
5. 进入 RegionController 配置 modal，分别检查进入动作条件组、离开动作条件组、停留动作条件组三个 picker；三个字段应分别保存、分别回显。
6. 打开 DevTools Console / Network，保存上述配置时确认无前端异常，写请求仍携带 CSRF，失败响应显示中文 validation message。
7. 在两个浏览器标签页分别打开新旧 signal channel 详情，修改 SignalListener channel 后确认新旧 channel 详情都会 silent refresh，不整页 reload、不关闭 modal、不重置滚动。
8. 窄屏和宽屏各检查一次：picker、错误提示、modal body 滚动和 footer 不应重叠或越界。

建议游戏内验收步骤：

1. 精确命令必须以当前代码注册和 TAB 补全为准；不要使用裸父命令如 `/tzz signal` 或 `/tzz regionctl` 作为验收命令。
2. 对 SignalListener：同一 channel 准备至少一个会产生明显副作用的 action，分别测试未配置 gate、gate true、gate false。gate false 时不得执行 action，也不得 emit 下游 signal。
3. 对 ActionRelay：通过 signal 触发 relay action list，分别测试未配置 gate、gate true、gate false；手动触发命令仍保持旧语义。
4. 对 RegionController：分别测试 enter / exit / stay 三个 action bucket。gate false 时不得执行 action list，但玩家 inside / outside 状态和 stay interval 应继续按旧机制推进。
5. missing / disabled / invalid / incompatible group 均应 fail closed，并在 WebAdmin 或日志中出现中文原因。
