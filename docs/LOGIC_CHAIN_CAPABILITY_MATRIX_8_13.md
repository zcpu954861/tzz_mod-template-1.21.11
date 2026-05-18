# Logic Chain Capability Matrix 8.13

## 范围

8.13 只增强 WebAdmin Logic Chain Viewer 的只读 runtime graph。它扩展 DTO、graph builder、前端图例 / 筛选 / 节点详情和 guard 文档，不改变 SignalBridge、ActionEngine、Timer、Join、StateVariable 或 Condition gate runtime 语义。

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| read-only graph | 已实现 | Viewer 只读，不执行 action、不 emit signal、不写 store |
| graph safety limit | 已实现 | `maxGraphNodes`、`maxGraphEdges`、nodesTruncated、edgesTruncated |
| component-aware traversal | 已实现 | Logic Chain 是 connected logical component，root channel 只作为 focus |
| component safety limit | 已实现 | `maxComponentChannels`、componentTruncated、中文 truncation reason |
| strong / weak association model | 已实现 | Join / Signal / Timer / StateAction / Gate 为强关联；共享状态 / 条件 / Timer fan-in 等弱关联默认折叠 |
| Signal Join node | 已实现 | input consumer / output producer 均显示为 `signal_join` |
| Join input summary | 已实现 | 全部 input channels 在详情卡片可见，提供上游展开入口 |
| no cross-channel long line mixing | 已实现 | Join inputs 使用左侧 lane 和清晰虚线，不复制 downstream 子图 |
| Timer source node | 已实现 | `outputChannel` 对应 channel 的上游显示 `timer` |
| Timer action reference | 已实现 | `timer_action` 指向目标 Timer，缺失目标显示 disabled reference |
| StateAction node | 已实现 | `state_variable` action 显示为 `state_action` |
| StateVariable target | 已实现 | 静态可解析变量显示 `state_variable` 节点；context_player 显示动态目标说明 |
| condition gate node | 已实现 | SignalListener / Region list-level gate 可见；ActionRelay 在安全 snapshot 暴露时可见，否则仅显示摘要和设备详情入口 |
| action gate node | 已实现 | SignalListener / Region / Timer action bucket single action gate 可见；ActionRelay 受限于安全 snapshot，当前显示摘要和设备详情入口 |
| recent runtime status | 已实现 | Join status、Timer status、Condition gate recent status、StateVariable current value |
| debugger link | 已实现 | gate 节点提供 Condition Debugger 入口 |
| doctor link | 已实现 | 节点详情保留 Doctor 入口 |
| view mode filter | 已实现 | 双向 / 下游 / 上游 / 相关节点 |
| component focus summary | 已实现 | UI 显示当前焦点频道和组件摘要，提供展开相关入口 |
| node type filter | 已实现 | Signal / Join / Timer / State / Gate / Action |
| GraphModel V2 | 已实现 | Join 专用 lane 布局：上游在左侧、Join 居中、output/downstream 在右侧 |
| primary/reference node metadata | 已实现 | `nodeKind`、`primaryNodeId`、`referenceReason`、`visualLane` 区分主节点和 reference card |
| edge visual model | 已实现 | `pathGroupId`、`visualStyle`、`referenceEdge` 支持颜色分组；Join 主输入实线、其他输入虚线、reference 灰色虚线 |
| visible graph edge overlay | 已实现 | V2 直接按当前可见 graph edge 绘制，不再把 incoming upstream 当右侧树 child |
| downstream merge | 已实现 | 多个 Join 输出到同一 channel 时合并到同一个 primary channel，下游 listener/action 不重复展开 |
| Join V2 metadata | 已实现 | `inputPorts`、`primaryInput`、`relatedInputs`、`outputPort`、`downstreamPrimaryNode`、`joinTraversalPolicy` |
| crossing reduction V1 | 已实现 | V2 lane 内按相邻层 source / target 顺序稳定重排，减少非必要交叉 |
| unified edge anchors | 已实现 | 同一 source 的多条输出线共享一个出口锚点，同一 target 的多条输入线共享一个入口锚点；多线分离只放在 Bezier control point |
| target arrow de-duplication | 已实现 | 同一 target 锚点只渲染一个箭头，hover / selected 时相关 edge 优先拥有箭头；reference edge 仍不渲染箭头 |
| display name resolver | 已实现 | Logic Chain 节点标题优先 WebAdmin 设备 / 频道 displayName，技术 ID 保留为副文本 |
| smooth Bezier routing | 已实现 | 默认恢复旧版平滑 Bezier 曲线和统一箭头样式；不默认使用折线 / polyline |
| strict straight routing | 已实现 | 只有 source centerY 与 target centerY 差值 `<= 1px` 时允许直线；不同高度必须使用平滑曲线 |
| shared trunk disabled by default | 已实现 | 多条边指向同一 target 时默认分别以平滑曲线进入 target，不再默认合成 merge point / shared trunk |
| edge routing fix | 已实现 | reference、Join related dashed edge 和跨高度 edge 均使用平滑曲线，避免斜直线和折线破坏可读性 |
| source / target ports | 已实现 | 连线仍使用 source 右侧 / target 左侧卡片锚点，Join input indexed port metadata 只用于排序和 control-point fanout |
| highlight clear toggle | 已实现 | 点击已选中节点会取消 pinned 高亮并保持详情面板；Escape 可清除当前高亮状态 |
| fixed graph card layout | 已实现 | graph card 使用固定高度和固定 title / subtitle / meta 行；长文本省略或两行 clamp，完整内容在详情面板 |
| long-chain lane compaction | 已实现 | 空 lane 不占列，动态 lane depth 允许长链向右延伸并避免下游折回 |
| detail panel close | 已实现 | 节点详情面板可关闭，关闭后不自动重新选择 root；点击节点重新打开 |
| docs / README | 已实现 | 8.13 context、capability matrix、README 更新 |
| stabilization guard | 已实现 | 8.13 marker、docs、UI、no out-of-scope guard |

## 数据来源限制

ActionRelay 的详细 actions / conditionGroupId 仍由 loaded block entity 持有。8.13 Viewer 不直接读取 live world / block entity，因此 Logic Chain 中 ActionRelay 只显示 snapshot actionCount、消费者节点、设备详情入口和中文 warning。后续如要展开 ActionRelay gate，应先把它纳入安全快照，而不是在 graph builder 里读取 world。

## 验证计划

本阶段完成后运行：

- `cd tools\tzz-test-mcp; npm run build; npm test`
- `.\gradlew.bat clean build`
- `.\gradlew.bat stabilizationGuardTest --rerun-tasks`
- `.\gradlew.bat localTestMcpGuardTest --rerun-tasks`
- `git diff --check`

## Node Types

| Node type | 说明 |
| --- | --- |
| `channel` | 当前 channel segment |
| `downstream_channel` | 下游子链入口 |
| `producer` | 既有 SignalBridge source / signal action source |
| `consumer` | listener / receiver / relay 等并列消费者 |
| `action` | 普通 action |
| `signal_join` | Join input 或 output |
| `timer` | Timer source 或 Timer action target |
| `state_action` | Controlled State Action |
| `timer_action` | timer_start / timer_cancel |
| `condition_gate` | list-level runtime gate |
| `action_gate` | single action gate |
| `state_variable` | 写入目标变量或动态变量占位 |
| `reference card` | 非真实节点；用于缩短跨频道长线或表示已合并下游，必须带 `primaryNodeId` |

## Edge Types

| Edge type | 说明 |
| --- | --- |
| `emits` | source 发出 channel |
| `consumes` | channel 进入 consumer |
| `executes` | consumer 执行 action |
| `emits_downstream` | signal action 输出下游 channel |
| `join_input` | input channel 进入 Join；primary input 为 solid，related input 为 dashed |
| `join_output` | Join 输出 channel |
| `timer_outputs_channel` | Timer complete 输出 channel |
| `action_starts_timer` | timer_start 引用 Timer |
| `action_cancels_timer` | timer_cancel 引用 Timer |
| `state_writes` | StateAction 写入 StateVariable |
| `gate_guards` | condition / action gate 守卫后续节点 |
| `pathGroupId` | V2 颜色分组：signal / join / gate / timer / state / reference |
| `referenceEdge` | 指向引用卡或别名卡的虚线边，不表示第二份 runtime 逻辑 |

## UI Markers

| Marker | 用途 |
| --- | --- |
| `data-logic-chain-enhanced-runtime-graph` | 8.13 enhanced viewer page |
| `data-logic-chain-view-mode-filter` | view mode filter |
| `data-logic-chain-node-type-filter` | node type filter |
| `data-logic-chain-node-detail-panel` | detail panel |
| `data-logic-chain-join-input-summary` | Join input summary |
| `data-logic-chain-upstream-expand-card` | upstream expand card |
| `data-logic-chain-timer-node` | Timer node/card |
| `data-logic-chain-state-action-node` | StateAction node/card |
| `data-logic-chain-condition-gate-node` | condition gate node/card |
| `data-logic-chain-action-gate-node` | action gate node/card |
| `data-logic-chain-debugger-link` | debugger link |
| `data-logic-chain-doctor-link` | Doctor link |
| `data-no-cross-channel-long-line-mixing` | no long-line mixing guard |
| `data-logic-chain-reference-card` | 引用卡 marker |
| `data-logic-chain-primary-node` | 主节点 marker |
| `data-logic-chain-path-color-legend` | 路径颜色图例 |
| `data-logic-chain-layout-v2-join-lanes` | V2 Join lane 布局 |
| `data-logic-chain-crossing-reduction` | V2 lane crossing reduction marker |
| `data-logic-chain-source-to-target-ordering` | 左侧 source 按 connected target 顺序 marker |
| `data-logic-chain-consumer-to-action-ordering` | 右侧 consumer/action 按 source/parent 顺序 marker |
| `data-logic-chain-display-name-preferred` | 节点优先使用 WebAdmin / runtime display name marker |
| `data-logic-chain-technical-id-secondary` | 技术 ID 作为副文本 / 详情保留 marker |
| `data-logic-chain-smooth-bezier-default` | 默认平滑 Bezier 路由 marker |
| `data-logic-chain-old-arrow-style` | 旧版统一箭头样式 marker |
| `data-logic-chain-no-polyline-default` | 默认不使用折线 / polyline marker |
| `data-logic-chain-no-shared-trunk-default` | 默认禁用 shared trunk / merge point marker |
| `data-logic-chain-straight-only-dy-le-1` | 直线只允许 centerY 差值 `<= 1px` marker |
| `data-logic-chain-different-height-smooth-curve` | 不同高度使用平滑曲线 marker |
| `data-logic-chain-no-diagonal-straight` | 禁止不同高度斜直线 marker |
| `data-logic-chain-same-row-straight-edge` | 严格同高直线路由 marker |
| `data-logic-chain-complex-curve-edge` | 跨高度 / 复杂关系使用曲线 marker |
| `data-logic-chain-reference-curve-edge` | reference edge 不强行直线 marker |
| `data-logic-chain-join-related-curve-edge` | Join related edge 不强行直线 marker |
| `data-logic-chain-highlight-clear` | 高亮可取消 marker |
| `data-logic-chain-hover-clear-on-leave` | hover 离开恢复 marker |
| `data-logic-chain-selection-highlight-clear` | 再次点击选中节点取消 pinned 高亮 marker |
| `data-logic-chain-escape-clears-highlight` | Escape 清除高亮 marker |
| `data-logic-chain-timer-action-node` | Timer action 节点 marker |
| `data-logic-chain-timer-card-no-overflow` | Timer action 卡片不溢出 marker |
| `data-logic-chain-timer-action-card-wrap` | Timer action 文案折行 / 截断 marker |
| `data-logic-chain-timer-bucket-wrap` | Timer bucket 文案折行 marker |
| `data-logic-chain-timer-instance-wrap` | Timer instance / id 文案折行 marker |
| `data-logic-chain-timer-start-no-overflow` | timer_start 卡片不溢出 marker |
| `data-logic-chain-timer-cancel-no-overflow` | timer_cancel 卡片不溢出 marker |
| `data-logic-chain-timer-no-overflow` | Timer action 相关文本不溢出 marker |
| `data-logic-chain-state-action-no-overflow` | StateAction 卡片不溢出 marker |
| `data-logic-chain-action-no-overflow` | Action 卡片不溢出 marker |
| `data-logic-chain-no-duplicate-action-index` | action index 不重复渲染 marker |
| `data-logic-chain-fixed-card-layout` | graph card 固定高度布局 marker |
| `data-logic-chain-card-title-row-fixed` | title 固定一行 marker |
| `data-logic-chain-card-subtitle-row-fixed` | subtitle 固定两行区域 marker |
| `data-logic-chain-card-meta-row-fixed` | meta 固定一行 marker |
| `data-logic-chain-text-clamp` | title / subtitle / meta 截断和 clamp marker |
| `data-logic-chain-empty-lane-compaction` | 空 lane 压缩 marker |
| `data-logic-chain-no-downstream-foldback` | 下游不折回 marker |
| `data-logic-chain-single-chain-compact-horizontal` | 单链紧凑横向布局 marker |
| `data-logic-chain-dynamic-lane-depth` | 长链动态 lane depth marker |
| `data-logic-chain-action-index-layout-ordering` | action lane 按 parent + actionIndex 排序 marker |
| `data-logic-chain-edge-port-offset` | edge port ordering / offset metadata marker |
| `data-logic-chain-source-right-output-port` | source 右侧输出端口 marker |
| `data-logic-chain-target-left-input-port` | target 左侧输入端口 marker |
| `data-logic-chain-join-input-port-indexed` | Join input indexed port marker |
| `data-logic-chain-join-output-port` | Join output 右侧端口 marker |
| `data-logic-chain-multi-edge-port-offset` | 多边端口偏移 marker |
| `data-logic-chain-single-source-anchor` | 同 source 多条线共享一个出口锚点 marker |
| `data-logic-chain-single-target-anchor` | 同 target 多条线共享一个入口锚点 marker |
| `data-logic-chain-target-arrow-once` | 同 target 锚点只渲染一个箭头 marker |
| `data-logic-chain-target-arrow-owner` | 当前 edge 是 target 箭头 owner marker |
| `data-logic-chain-no-endpoint-port-split` | 最终端点不按 port offset 分裂 marker |
| `data-logic-chain-control-point-fanout` | 多线只在 Bezier control point 分散 marker |
| `data-logic-chain-join-layout-v2` | V2 Join 专用布局 marker |
| `data-logic-chain-default-edge-opacity` | 默认连线清晰可见 marker |
| `data-logic-chain-join-primary-input-edge` | Join 主输入实线 marker |
| `data-logic-chain-join-related-input-edge` | Join 其他输入虚线 marker |
| `data-logic-chain-reference-edge` | 引用灰色虚线 marker |
| `data-logic-chain-join-input-port` | Join input port / 输入行 |
| `data-logic-chain-reference-jump-primary` | 引用卡定位主节点 |
| `data-logic-chain-graph-truncation-marker` | 图规模截断中文提示 |
| `data-logic-chain-component-aware-mode` | component-aware traversal / 组件视图 marker |
| `data-logic-chain-focus-channel` | 当前焦点频道 marker |
| `data-logic-chain-component-summary` | 逻辑组件摘要 marker |
| `data-logic-chain-collapsed-related-marker` | 弱关联 / 规模限制折叠提示 marker |
| `data-logic-chain-expand-related` | 展开相关 / 切换组件视图入口 marker |
| `data-logic-chain-join-all-input-channels-visible` | Join 全 input channel 属于同一组件 marker |

## 语义边界

- Logic Chain 不再等于单个 channel；它是通过强关联边连接出的只读 logical component。
- Root channel 是打开入口和焦点高亮，不是 graph boundary。
- 强关联包括 channel consume / produce、signal action output、Join input/output、Timer output、timer_start / cancel reference、StateAction write target、condition/action gate。
- 弱关联包括共享 StateVariable 读者、共享 ConditionGroup 绑定、大量 Timer reference 和高 fan-out unrelated consumers；弱关联默认折叠或受 limit 约束。
- Viewer 不保证全局唯一拓扑排序。
- 同一 channel 下多个 consumers 是并列消费者。
- action list 内部顺序只在同一 owner 内有效。
- Join inputs unordered。
- Gate node 是 allow / block / skip guard 可视化，不是 if / else branching。
- Timer 和 StateAction 是直接 runtime 能力；Signal/channel 只是当前兼容入口之一，不应被视为未来唯一入口。

## 明确不做

- full Logic Chain Editor。
- drag editing。
- Scratch-like editor。
- if / else / else-if runtime。
- GameController / MissionSystem / PhaseController。
- 不新增 Action type。
- 不新增 Condition type。
- runtime semantic changes。
- raw JSON editor。
- MCP scenario。
- Minecraft startup。
- screenshot matrix。
- commit / push / merge / tag。
