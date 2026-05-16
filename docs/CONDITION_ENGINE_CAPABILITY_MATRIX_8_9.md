# ConditionEngine Capability Matrix 8.9

8.9 在 8.7 receiver-side runtime gates 和 8.8 debugger / Doctor / replay 之上，新增单条 Action Runtime Gate。它只扩展 action list 内单条 action 的可选 gate，不新增 SignalReceiver gate，不改 ActionEngine action 类型语义。

## Runtime

| 能力 | 状态 | 说明 |
|---|---|---|
| `ActionConfig.conditionGroupId` | 已实现 | 旧 action JSON 未配置时为空，保持兼容。 |
| blank action gate skip | 已实现 | 空值不读 store、不构造 context、不 evaluate、不写 history。 |
| action gate true | 已实现 | 执行当前 action。 |
| action gate false | 已实现 | skip current action and continue，默认继续后续 action。 |
| downstream signal skip | 已实现 | signal action gate false 时不 emit downstream signal。 |
| list-level before action-level | 已实现 | parent/list-level gate false 时不 evaluate 单条 action gate。 |
| ActionRelay manual bypass | 已实现 | 手动测试继续绕过 runtime gate，包括单条 action gate。 |
| failure policy | 不做 | 不做失败时中断整组、fallback action、failure channel 或 stop-list policy。 |

## Target Types / Compatibility

| target type | 状态 | 兼容能力 |
|---|---|---|
| `SIGNAL_LISTENER_ACTION` | 已实现 | channel、listenerId、action metadata、GLOBAL state、explicit PLAYER state。无 player / item / inventory / container / region / signal history / logic chain snapshot。 |
| `ACTION_RELAY_ACTION` | 已实现 | channel、deviceId、relayId、action metadata、GLOBAL state、explicit PLAYER state。无 player / item / inventory / container / region / signal history / logic chain snapshot。 |
| `REGION_ENTER_ACTION` | 已实现 | player context、region/current_region snapshot、action metadata、GLOBAL / PLAYER state。无 item / inventory / container / signal history / logic chain snapshot。 |
| `REGION_EXIT_ACTION` | 已实现 | 同 Region enter action。 |
| `REGION_STAY_ACTION` | 已实现 | 同 Region enter action。 |
| disabled / invalid group filtered | 已实现 | available list 不展示 disabled / invalid groups。 |
| backend reject incompatible | 已实现 | 保存时二次校验，不兼容 binding 被拒绝。 |

## WebAdmin API / UI

| 能力 | 权限 | 状态 | 说明 |
|---|---|---|---|
| `GET /api/webadmin/condition-groups/available` action query | VIEWER | 已实现 | 支持并回显 parent/action metadata。 |
| SignalListener action condition picker | EDITOR/OWNER 写 | 已实现 | add/edit modal 中选择单条 action 条件组。 |
| ActionRelay action condition picker | EDITOR/OWNER 写 | 已实现 | Action list modal 中逐条 action 配置。 |
| RegionController action condition picker | EDITOR/OWNER 写 | 已实现 | enter / exit / stay action add/edit modal 中配置。 |
| incompatible current value warning | 只读展示 | 已实现 | 不静默清空，中文提示并提供清空入口。 |
| action summary condition status | VIEWER | 已实现 | 列表显示未配置 / 已配置，并可展示最近 gate result。 |
| raw JSON editor | 不做 | 不提供 action condition raw JSON 编辑器。 |

## Debugger / Replay / Doctor

| 能力 | 状态 | 说明 |
|---|---|---|
| action gate history | 已实现 | configured action gate allowed / blocked / error 记录 history。 |
| gate level metadata | 已实现 | `gateLevel=ACTION`，包含 parent target、actionIndex、actionType。 |
| debugger filters | 已实现 | 条件调试器支持五类 action target type。 |
| replay read-only | 已实现 | 不执行 action、不 emit signal、不 consume item、不写 store。 |
| Doctor action scan | 已实现 | 扫描 SignalListener / ActionRelay / RegionController action `conditionGroupId`。 |
| Doctor blank no issue | 已实现 | 未配置 action condition 不报错。 |

## 测试覆盖

| 测试 | 覆盖 |
|---|---|
| `ConditionActionGateServiceTest` | ActionConfig JSON 兼容、blank skip、action metadata history、false gate blocked decision、incompatible profile before context build。 |
| `ConditionGroupCompatibilityServiceTest` | 五类 action target profile 的 player / action metadata / region / container / signal history 兼容性。 |
| `WebAdminConditionGroupServiceTest` | action target available list 过滤和 parent/action metadata 回显。 |
| `WebAdminConditionGateConfigTest` | 后端 reject incompatible SignalListener/ActionRelay/Region action binding，允许 compatible action metadata。 |
| `WebAdminConditionRuntimeDoctorServiceTest` | action missing / disabled / invalid / incompatible / blank no issue。 |
| `StabilizationGuardTest` | runtime insertion point、UI markers、docs、no out-of-scope guard。 |

## Scope Guard

8.9 明确不实现：

- SignalReceiver gate。
- Signal Join / Barrier / Aggregator。
- GameController / MissionSystem / PhaseController。
- 具体任务 / 关卡。
- failure policy。
- stop-list policy。
- fallback action。
- raw JSON editor。
- 任意 NBT path。
- 通用脚本表达式。
- MCP scenario。
- 启动 Minecraft。
- 截图矩阵。
