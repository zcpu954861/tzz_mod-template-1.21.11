# 8.13 Logic Chain Viewer 增强当前上下文

## 阶段目标

8.13 将 WebAdmin Logic Chain Viewer 从早期 SignalBridge 链路查看器增强为只读 runtime graph：

- 从频道出发能看到上游 producer、下游 consumer、Signal Join、Timer、StateAction、StateVariable、Condition gate 和 single Action gate。
- 从 Join output channel 出发能看到全部 input channels。
- 从 Timer output channel 出发能看到 Timer source。
- 从 state_variable action 出发能看到写入目标变量或动态目标说明。
- 从 gate 节点能跳转 Condition Group、Condition Debugger 和 Doctor。

本阶段仍然只读：不保存 runtime graph，不触发 signal，不执行 action，不写 store，不扫描 world。

## Component-aware traversal

8.13 返修后，Logic Chain Viewer 中的“逻辑链”不再等于单个 channel。逻辑链是由 Signal、Join、Timer、Action、StateAction、StateVariable、Gate 等只读关系连接起来的 logical component / connected subgraph。

- `rootChannel` 仍用于打开 Viewer，但语义改为当前焦点 / 高亮入口，不再作为图边界。
- 从同一 component 内任意 input channel、Join output channel 或 downstream channel 进入，应看到同一组核心频道、Join、Timer、Action 和消费者；差异只应体现在 focus / primary input 高亮。
- Join input channel、Join node、Join output channel 是强关联；从 `A + B -> Join J -> C` 的 A、B、C 任意入口都应看到 A、B、J、C。
- signal action output 会把 action owner channel 与 output channel 归入同一 component；Timer output 和 timer_start / timer_cancel reference 会把 Timer 与相关频道归入同一 component。
- StateAction -> StateVariable、condition/action gate -> ConditionGroup 是只读强关系，但不会扫描全局读者或绑定者。
- 大量共享 StateVariable、ConditionGroup、Timer reference 或高 fan-out consumer 属于弱关联，默认折叠或受 component limit 约束，避免整服 Signal 网络连成一张巨图。
- 组件扩展受 `maxDepth`、`maxGraphNodes`、`maxGraphEdges` 和 `maxComponentChannels` 保护；达到限制时显示中文截断提示，不白屏、不无限递归。

## GraphModel V2

本次返修把 Viewer 从 V1 DAG-like 模型推进到 Join 专用可读布局。V2 仍然是只读图模型，但要求 Join / 多上游不再套普通树状展开：

- 真实节点 vs 引用卡：真实对象使用 `nodeKind=primary` 和稳定 `primaryNodeId`；视觉缩短用的 reference card 使用 `nodeKind=reference`、`primaryNodeId`、`referenceReason`。
- 节点去重：同一 output channel、同一 Signal Join、同一 listener / relay / timer / state_variable 只保留一个 primary node。
- 下游合并：多个 Join 输出到同一 channel 时，边汇合到同一个 `channel:<channel>` 主节点；下游 listener/action 只展开一次。
- 上游在左侧：Join input channel 和 upstream producer 使用左侧 lane；Join 居中；output channel 与 downstream listener/action 在右侧 lane。
- Join 专用布局：Join 节点 metadata 暴露 `inputPorts`、`primaryInput`、`relatedInputs`、`outputChannel`、`outputPort`、`downstreamPrimaryNode` 和 `joinTraversalPolicy=no_recursive_downstream_copy`。
- 颜色分组：signal / join / gate / timer / state / reference edge 使用不同 path group；Join 主输入实线，Join 其他上游虚线，reference edge 使用灰色虚线。
- 连线默认可见：主要连线默认高透明度显示；只有点击或悬停节点后，非关联线才降低透明度。
- 非必要交叉减少：V2 lane 内使用局部 crossing reduction，左侧 source 按 connected target 顺序、右侧 consumer/action 按 source/parent 与 actionIndex 顺序稳定排列；edge 使用 source 右侧 / target 左侧端口和多边 y offset。
- 显示名称解析：节点标题优先使用 WebAdmin 设备 / 频道 displayName，其次 runtime name、频道名、坐标或 short id；技术 ID 保留在副文本和详情中。
- 平滑曲线优先：edge 默认恢复旧版 Bezier 曲线和统一箭头样式；多个 edge 从同一 source 发出时共享同一个卡片出口锚点，多个 edge 指向同一 target 时共享同一个卡片入口锚点并只渲染一个 target 箭头；多线分离只发生在 Bezier control point 上，不再用 endpoint port split，也不默认合成 shared trunk / merge point。
- 直线严格限制：只有 source centerY 与 target centerY 的差值 `<= 1px` 时才允许直线；任意不同高度、reference edge、Join related dashed edge 都回到平滑曲线，避免折线、斜直线和提前合流造成可读性下降。
- 长链布局：V2 使用动态 lane depth 和空 lane compaction；下游 traversal edge 不再被 0..7 固定 lane 截断，长链可横向变长但不折回。
- 当前限制：V2 是稳定 lane 布局，不是完整全局拓扑优化；部分 signal action producer 在跨频道视图中会作为 alias/reference card 指向真实 action。

## 新增图节点

8.13 在既有 channel / producer / consumer / action / downstream_channel 之外加入：

- `signal_join`：Signal Join input consumer 或 output producer。
- `timer`：Timer output source 或 timer_start / timer_cancel 引用目标。
- `state_action`：Controlled State Action 写入动作。
- `timer_action`：timer_start / timer_cancel 动作。
- `condition_gate`：list-level runtime gate。
- `action_gate`：single action gate。
- `state_variable`：StateAction 静态可解析的写入目标，或动态目标占位。

图构建带有安全上限：最大节点数和边数会写入 stats；超过上限时只读截断并给出中文 warning。

ActionRelay 的动作配置仍由方块实体持有；8.13 Logic Chain graph 不直接读取 live world / block entity。Viewer 只用 store snapshot 显示 ActionRelay 消费者和 actionCount 摘要，具体 action / gate 详情继续从设备详情或后续安全快照能力进入。

## 新增关键边

8.13 使用稳定 edge type 表达 runtime 关系：

- `join_input`：input channel 进入 Signal Join；当前 root input 为 solid primary edge，其他 input 为 dashed related edge。
- `join_output`：Signal Join 输出到 outputChannel。
- `timer_outputs_channel`：Timer complete 输出到 channel。
- `action_starts_timer` / `action_cancels_timer`：Timer action 引用 Timer。
- `state_writes`：StateAction 写入 StateVariable。
- `gate_guards`：condition gate / action gate 守卫后续节点。
- `executes`：consumer 进入 action。

旧的 `emits`、`consumes`、`emits_downstream` 语义保留，不改 SignalBridge 或 ActionEngine runtime。

## Signal Join 可视化

Join 在 input channel 展开时显示为 `signal_join` consumer；在 output channel 展开时显示为 `signal_join` producer/source。

Join 节点详情显示：

- Join 名称、mode、scope、resetPolicy、outputChannel。
- 全部 inputChannels。
- 当前 root channel 是否属于 input。
- pending scope、lastResult、lastFailureReason。
- 输入摘要卡片。

输入摘要卡片逐行显示每个 input channel、是否在 pending scope 中已满足、频道详情入口和上游逻辑链展开入口。

V2 会把全部 input channel 作为 Join 左侧输入节点显示。当前 root input 到 Join 使用实线；其他 input 到 Join 使用清晰虚线。output channel 是右侧 primary node；如果多个 Join 共享同一个 output channel，downstream listener/action 只在该 primary output channel 下展开一次。

## Timer 可视化

有 `outputChannel` 的 Timer 会作为 `timer` source 出现在该 channel 的上游。

Timer 节点详情显示：

- mode、scopeMode、durationTicks、intervalTicks、maxRuns、startPolicy。
- activeInstanceCount、lastResult、lastFailureReason、runtimeStatePersistent。
- onStart / onTick / onComplete / onCancel action bucket summary。
- Timer 详情入口和 output channel 入口。

timer_start / timer_cancel action 显示为 `timer_action`，并通过 dashed edge 指向目标 Timer；目标缺失时显示 disabled Timer 引用节点，不创建 Timer。

## StateAction / StateVariable 可视化

`state_variable` action 显示为 `state_action` 节点。

StateAction 详情显示：

- operation、scope、targetMode、targetId、key。
- valueType、delta、createIfMissing。
- state action summary。
- 静态可解析的 StateVariable route。

GLOBAL 和 PLAYER explicit_target 能静态解析到 StateVariable stable id。PLAYER context_player 依赖运行时玩家，Viewer 显示动态目标说明，不猜测玩家。

StateVariable 节点只读显示当前可见值、type、version 和详情入口。缺失或 store degraded 时显示中文空状态 / degraded message。

## Gate 可视化

8.13 显示两类 gate：

- `condition_gate`：list-level gate，例如 SignalListener、ActionRelay、Region enter / exit / stay action list。
- `action_gate`：single Action gate，例如 listener / relay / region / timer action bucket 中某一条 action。

gate 节点详情显示：

- conditionGroupId。
- targetType / targetId。
- gateLevel。
- parent target。
- action index / action type。
- recentConditionGate status。
- Condition Group、Condition Debugger、Doctor 跳转。

gate false 只表示 Viewer 中的 guard 关系；runtime 仍沿用 8.6 到 8.12 已有 gate 语义。

其中 ActionRelay gate 只有在安全 snapshot 已暴露对应配置时才能展开；当前 8.13 不通过 Logic Chain Viewer 读取 loaded block entity，因此 ActionRelay 侧以摘要和设备详情入口为主。

## 多频道语义

Logic Chain Viewer 是只读浏览视图，不保证全局唯一拓扑排序。

- 同一 channel 下多个 consumers 是并列消费者，不代表严格顺序。
- action list 内部的 actionIndex 才是本地顺序。
- Join inputs 之间无先后顺序。
- downstream channel 是子链展开入口。
- upstream view 使用入边和 producer/source 摘要，不混入跨频道长线。
- cycle / self-loop / duplicate channel reference 会停止递归并显示引用状态。

## WebAdmin UI

8.13 UI 增加：

- 组件焦点摘要：显示当前焦点频道、当前逻辑组件频道数 / Join 数 / Timer 数 / 消费者数。
- 组件视图：root channel 只作为 focus，不再裁剪同组件内其他相关上游。
- 视图模式：双向 / 下游 / 上游 / 相关节点。
- 节点类型筛选：Signal / Join / Timer / State / Gate / Action。
- 增强图例。
- 节点详情面板。
- Join 输入摘要区域。
- Timer runtime/action bucket 摘要。
- StateAction / StateVariable 摘要。
- Gate 最近运行状态、Debugger / Doctor 跳转。

UI 继续使用 WebAdmin 暗色后台风格，主文案中文，技术 ID 作为副文本。realtime / silent refresh 不清空筛选、不关闭详情、不重置 pan/zoom。

8.13 返修后，点击已选中的节点会取消 pinned 高亮并恢复默认连线清晰度，但保留节点详情面板和当前视口；Escape 也可清除当前高亮。graph card 使用固定高度和固定 title / subtitle / meta 行，长文本通过一行省略或两行 clamp 处理，完整文本保留在节点详情面板，避免 `timer_start`、`timer_cancel`、state/action 摘要等长文本撑破卡片或改变卡片高度。

## Doctor / Debugger / Recent Status

8.13 复用已有只读数据：

- SignalJoinRuntimeService status。
- TimerRuntimeService status。
- StateVariableStore no-create snapshot。
- WebAdminConditionGateHistoryService recent status。
- Doctor 页面跳转。

没有 recent history 时显示中文空状态，不构造运行时记录。
Signal Join 状态使用只读 status snapshot，不触发 lazy timeout 清理；打开或刷新 Viewer 不改变 Join runtime state。

## 验证计划

完成本阶段修复后按顺序运行：

- `cd tools\tzz-test-mcp; npm run build; npm test`
- 回到仓库根目录运行 `.\gradlew.bat clean build`
- `.\gradlew.bat stabilizationGuardTest --rerun-tasks`
- `.\gradlew.bat localTestMcpGuardTest --rerun-tasks`
- `git diff --check`

不启动 Minecraft，不跑 MCP scenario，不生成截图矩阵。

## 本阶段明确不做

- 完整 Logic Chain Editor。
- 拖拽编辑。
- Scratch-like editor。
- if / else / else-if runtime。
- GameController。
- MissionSystem。
- PhaseController。
- 具体任务 / 关卡。
- 修改 Signal / Action / Timer / Join / StateAction runtime 行为。
- 不新增 Action type。
- 不新增 Condition type。
- raw JSON editor。
- MCP scenario。
- 启动 Minecraft。
- 截图矩阵。
- commit / push / merge / tag。

## 后续 deferred

- full Logic Chain Editor。
- 真正的 if / else / else-if / nested branching 控制流。Condition gate 只是允许 / 阻断 / 跳过，不等于多分支编程语义。
- direct typed game-program calls：Timer、StateAction、Message、Title、Sound、Condition、Join 等未来应能作为游戏程序 typed block 直接调用；Signal/channel 不是长期唯一入口。
- Scratch-like editor。
- GameController / MissionSystem。
- version rollback。
