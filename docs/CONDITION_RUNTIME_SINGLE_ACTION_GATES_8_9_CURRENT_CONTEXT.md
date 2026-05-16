# 8.9 Single Action Runtime Gate 当前上下文

## 8.9 目标

8.9 在 8.7 的整组 action list gate 和 8.8 的 runtime debugger / Doctor / replay 之上，补齐单条 Action 级别的可选 condition gate。

核心能力：

- `ActionConfig.conditionGroupId` 可为空。
- 未配置单条 action 条件组时，不读取 condition group store，不构造 action gate context，不 evaluate，不记录 history，保持旧执行逻辑。
- 配置后，在执行当前 action 前 evaluate condition group。
- action gate true 时执行当前 action。
- action gate false / error 时只 skip current action and continue，默认继续后续 action。
- signal action 被 action gate false 阻断时，不 emit downstream signal。

## 与整组 list gate 的顺序

8.9 保持 8.7 决策顺序：

```text
1. 先执行 parent/list-level gate
2. list-level gate false：跳过整个 action list，不 evaluate 单条 action gate
3. list-level gate true 或未配置：进入 action list
4. 每条 action 执行前判断 action.conditionGroupId
5. action gate false：只跳过当前 action
6. action gate true 或未配置：执行当前 action
```

action-level gate false 不改变 list-level gate history，不改变 SignalListener cooldown、ActionRelay lastRun 或 Region tracking / stay interval 的旧语义。

## Runtime target types

新增 action-level runtime target type：

- `SIGNAL_LISTENER_ACTION`
- `ACTION_RELAY_ACTION`
- `REGION_ENTER_ACTION`
- `REGION_EXIT_ACTION`
- `REGION_STAY_ACTION`

target id 约定：

- `listener:<listenerId>:action:<index>`
- `relay:<deviceId>:action:<index>`
- `region:<controllerId>:<enter|exit|stay>:action:<index>`

history 和 debugger 使用 `gateLevel=ACTION` 区分单条 action gate，保留 `parentTargetType`、`parentTargetId`、`actionIndex`、`actionType` 和 `parentActionBucket`。

## SignalListener action gate

SignalListener 先执行整组 listener gate。整组 gate 通过或未配置后，逐条 action 判断自己的 `conditionGroupId`。

语义：

- 未配置 action condition：旧 action 执行路径不变。
- action gate false：跳过当前 action，继续后续 action。
- list-level gate false：不更新时间冷却，也不 evaluate 单条 action gate。
- list-level gate true 但单条 action 被跳过：listener 已处理该 signal，cooldown 行为保持旧 action list processing 语义。

## ActionRelay action gate

ActionRelay runtime signal 触发时，先执行 relay 整组 gate，再逐条 action 判断单条 gate。

语义：

- 未配置 action condition：旧 action 执行路径不变。
- action gate false：跳过当前 action，继续后续 action。
- gate false 的 signal action 不 emit downstream signal。
- ActionRelay 手动测试绕过所有 runtime gate，包括单条 action gate，保持 8.7 手动测试绕过 gate 的语义。

## RegionController action gate

RegionController enter / exit / stay 保持原有整组 gate。整组 gate 通过或未配置后，分别对 enter / exit / stay action list 中的单条 action 做判断。

语义：

- 未配置 action condition：旧 action 执行路径不变。
- action gate false：跳过当前 action，继续后续 action。
- Region tracking 不被单条 action gate false 影响。
- stay interval 在一次 stay 执行尝试后仍推进，不因为 action skip 变成每 tick 重试。

## Available list / compatibility

WebAdmin `GET /api/webadmin/condition-groups/available` 支持 action target query，并回显：

- `parentTargetType`
- `parentTargetId`
- `actionType`
- `actionIndex`
- `actionBucket`

兼容性规则：

- SignalListener action：提供 channel / listenerId / action metadata / GLOBAL state / explicit PLAYER state；不提供触发玩家、物品、背包、容器、区域、信号历史或逻辑链快照。
- ActionRelay action：提供 channel / deviceId / relayId / action metadata / GLOBAL state / explicit PLAYER state；不提供触发玩家、物品、背包、容器、区域、信号历史或逻辑链快照。
- Region action：提供 player context、region / current_region snapshot、action metadata、GLOBAL / PLAYER state；不提供物品、背包、容器、信号历史或逻辑链快照。

前端 picker 只展示 compatible groups。后端保存仍二次执行 compatibility 校验，不能只靠前端隐藏不兼容项。

## WebAdmin action editor

以下 action editor 增加单条 Action 条件组 picker：

- SignalListener action add/edit modal。
- ActionRelay action list modal。
- RegionController enter / exit / stay action add/edit modal。

文案原则：

```text
未配置 = 此 action 不单独判断，保持旧执行逻辑
```

不兼容当前值不会被静默清空，会显示中文 warning，并要求用户显式清空或更换。保存失败不清空输入。写操作继续走 edit lock、expectedFingerprint、permission、CSRF / same-origin、audit 和 realtime。

## Debugger / replay / Doctor

8.9 action gate 接入 8.8 runtime debugger：

- action gate allowed / blocked / error 记录 history。
- history detail 显示 gate level、parent target、action index、action type。
- replay 只读，不执行 action、不 emit signal、不 consume item、不写 store。
- Doctor 扫描 SignalListener action、ActionRelay action、RegionController enter / exit / stay action 上的 `conditionGroupId`。
- Doctor 诊断 missing / disabled / invalid / incompatible action condition group。
- 未配置 action.conditionGroupId 不报错。

## Deferred / 不做

8.9 不实现：

- SignalReceiver gate。
- Signal Join / Barrier / Aggregator。
- GameController / MissionSystem / PhaseController。
- 具体任务 / 关卡。
- failure policy。
- stop-list policy。
- fallback action。
- action gate false 后 emit failure channel。
- action gate false 后修改 state。
- raw JSON editor。
- 任意 NBT path。
- 通用脚本表达式。
- MCP scenario。
- 启动 Minecraft。
- 截图矩阵。
