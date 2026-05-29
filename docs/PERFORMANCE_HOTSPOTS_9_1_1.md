# TZZ 9.1.1 Performance Hotspots

## 性能风险总览

本轮未运行真实浏览器性能测试，结论来自只读静态调用链、只读审查和启发式统计。当前最大性能风险集中在 WebAdmin Logic Chain viewer/editor：

本文是后续 9.1.1 implementation plan 的性能审计输入，不是当前 docs-only audit 之外的执行授权。所有优化建议必须先由 behavior marker / DOM 等价 guard 证明不改变 9.1 可见行为。

- `renderLogicChainViewer` 会在 hover、click、drag preview、zoom、draft input sync 等轻交互中重建整页和整张图。
- `logicChainLayoutGraphV2` 每次全量构建 edge index、visible ids、lane propagation、crossing sort、edge port annotate。
- `logicChainLayoutWithDraft` 与 VBD native trigger overlay 会在 draft 预览时重复 clone/filter nodes/edges。
- `logicChainNodeCard` 每张卡调用 `logicChainRelatedNodeIds`，后者扫描全量 edges，形成约 `O(nodes * edges)` 的渲染热点。
- `logicChainDraftDiffBanner` 在 toolbar render 时同步构造；hover/drag 也会触发 unsaved summary 重算。
- `renderIcons(appView())` 在整页 render 后执行，轻交互也会触发 icon pass。

## WebAdmin app.js / app.css 体积

当前源文件体积：

| Asset source | Lines | Bytes | Risk |
| --- | ---: | ---: | --- |
| `WebAdminFrontendScripts.java` | 8,433 | 1,984,343 | JS bundle Java text block 过大，parse/compile/render smoke 成本持续上升。 |
| `WebAdminFrontendStyles.java` | 75 | 123,798 | CSS 体积不算极端，但压成少数超长行，diff 与 guard 难做。 |
| `WebAdminFrontendAssets.java` | 23 | small | 仅 facade。 |

前端 guard 应直接测 `WebAdminFrontendScripts.appJs()` 的 UTF-8 bytes、`WebAdminFrontendStyles.appCss()` 的 UTF-8 bytes、`node --check` 时间和 `vm.Script` parse/compile 时间，而不是只测 Java source bytes。

当前 cache-busting 风险：

- `WebAdminFrontendShell.java:4`：`ASSET_VERSION = "8.18-snapshot-rollback-timeline-clickfix"`
- `WebAdminFrontendScripts.java:162`：`TZZ_WEBADMIN_ASSET_VERSION = "8.18-snapshot-rollback-timeline-clickfix"`

9.1 功能完成后 asset version 未更新，可能导致浏览器缓存语义与真实功能状态脱节。9.1.1 实施阶段应纳入 docs/guard 检查，但本轮不直接修改。

## Logic Chain render/layout 调用链

当前主调用链：

```text
renderLogicChainViewer(graph, routeInfo, options)
  -> logicChainRenderedGraphWithDraftOverlay(graph)
     -> base graph clone / draft overlay wrappers
     -> logicChainApplyVbdNativeTriggerDraftGraphOverlay(graph)
  -> logicChainNodeMap(renderedGraph)
  -> logicChainGraphWithNewDraftDetails(renderedGraph)
  -> logicChainCanvasToolbar()
     -> logicChainDraftDiffBanner(editor)
  -> logicChainCanvas(renderedGraph, canvasNodes)
     -> logicChainLayoutGraphV2(graph, nodes)
        -> build edgeIndexes / visible ids / graphEdges
        -> lane propagation
        -> downstream foldback fixes
        -> crossing sort
        -> logicChainAnnotateEdgePorts
     -> logicChainApplyFixedNodeHeights(layout)
     -> logicChainLayoutWithDraft(layout, nodes, graph)
        -> draftMetrics / legalSlots
        -> draft channel / draft node / draft action alias / reference endpoint insertions
     -> logicChainAdjustSavedProducerLayout(layout)
     -> logicChainMindMap(layout, graph, nodes)
        -> edge SVG layer
        -> draft slot overlay
        -> all positioned node cards
     -> logicChainMinimap(graph)
  -> logicChainSelectedNodePanel(detailGraph, nodes)
  -> renderIcons(appView())
```

关键位置：

- `renderLogicChainViewer`: `WebAdminFrontendScripts.java:7529`
- `logicChainRenderedGraphWithDraftOverlay`: `WebAdminFrontendScripts.java:8097`，后续被 V12/V16/VBD wrappers 参与 overlay pipeline。
- `logicChainCanvas`: `WebAdminFrontendScripts.java:7710`
- `logicChainLayoutGraphV2`: `WebAdminFrontendScripts.java:7712`
- `logicChainLayoutWithDraft`: `WebAdminFrontendScripts.java:7749`
- `logicChainMindMap`: `WebAdminFrontendScripts.java:7841`
- `logicChainMinimap`: 由 `logicChainCanvas` 单独生成，不是 `logicChainMindMap` 内部产物。
- `logicChainEdgePath`: `WebAdminFrontendScripts.java:7850`
- `logicChainNodeCard`: `WebAdminFrontendScripts.java:8081`
- `logicChainRelatedNodeIds`: `WebAdminFrontendScripts.java:7606`
- VBD overlay patch stack: `WebAdminFrontendScripts.java:8344`, `8361`, `8390`

## 哪些操作触发全量 render / layout

| Operation | Current path | Performance issue | Local update candidate |
| --- | --- | --- | --- |
| Hover node | `mouseover/mouseout` -> `highlightRelatedEdges` -> `renderLogicChainViewer` | 每次 hover 重跑 overlay/layout/edge/card/diff/icons。 | 先加 DOM 等价 guard；之后才允许预构建 adjacency map 并局部切 class。 |
| Click/select node | `focusLogicChainNodeDetail` -> `renderLogicChainViewer` | selection/detail 变化重建整页和图。 | 先证明 right panel、edge/card class、arrow owner 完全等价，再拆局部更新。 |
| Drag draft preview | `pointermove` -> `logicChainDraftPointerMove` -> `logicChainUpdateDraftDropPreview` | drag active 时绕过 slot unchanged 早退，pointermove 近似每帧全量 layout。 | `requestAnimationFrame` 合并只允许在 slot 不变时生效；拖动 card transform 不得改变 legal slot / preview column。 |
| Zoom | `setLogicChainZoom` -> `renderLogicChainViewer` | zoom 只改 transform 却重建图。 | DOM transform-only 必须保持 toolbar、pan bounds、selection/detail 不变。 |
| Connection mode clear | `clearLogicChainConnectionModeState(..., render=true)` | UI transient 变更触发布局，也可能影响 dirty confirm 当前行为。 | 先隔离和记录 current behavior；是否移出 dirty calculation 是独立 bugfix，不混入机械优化。 |
| VBD native trigger field change | `syncLogicChainExistingEditDraft` -> `rerenderLogicChainEditorPreservingUi` / scheduled graph refresh | modal 输入可能触发背景图重建，造成 typing jank。 | modal draft 与 graph preview 分层；按 draftRevision debounce；不得改变 formal VBD save 时机。 |
| Unsaved diff expanded/changed | toolbar -> `logicChainDraftDiffBanner` | hover/drag 也会重算 summary。 | diff rows 可按 draftRevision memo，但 pending delete / action reorder / channel metadata 必须进入 key。 |

## Hover / click / highlight 热点

当前事件入口：

- 全局 `document.addEventListener('mouseover')`: `WebAdminFrontendScripts.java:1263`
- 全局 `document.addEventListener('mouseout')`: `WebAdminFrontendScripts.java:1264`
- Logic Chain handler: `handleLogicChainEditorDelegatedMouseOver` / `MouseOut`: `8107-8108`
- Highlight function: `highlightRelatedEdges`: `7859`
- Edge path reads hover/selection: `logicChainEdgePath`: `7850`

问题：

- hover 是最高频交互，当前却走 full render。
- edge path 与 node card 都读取 hover/selection 状态，导致 highlight 不能局部切换。
- `logicChainRelatedNodeIds` 在每张卡里扫描全图 edges，hover 前的完整 render 本身已有 `O(nodes * edges)` 风险。

建议：

- 在 `logicChainRenderedGraphWithDraftOverlay` 后构建一次 `adjacencyByNodeId` / `relatedByNodeId`。
- node card 和 edge render 使用预计算 map，不在每张卡里重复扫描。
- hover 只更新 `.is-related` / `.is-dimmed` / `.is-highlighted` class 之前，必须先建立 DOM 等价 baseline：edge `d`、`marker-end`、arrow owner、node/edge class、right panel、diff banner、minimap HTML 都不变。
- 小图 hover 目标 `<16ms`；大图先设 `<50ms` soft guard，后续逐步收紧。

## Draft overlay / unsaved summary 热点

VBD native trigger overlay 当前有多次 wrapper：

- `logicChainApplyVbdNativeTriggerDraftGraphOverlay` first definition: `8344`
- V16 wrapper into `logicChainRenderedGraphWithDraftOverlay`: `8345`
- Stable trigger identity rewrite: `8361`
- Source card rewrite: `8390`

风险：

- 每次 overlay 都 clone nodes/edges 并 filter / push draft edges。
- 同一函数被多次重写，顺序改变就可能破坏 trigger output preview。
- selected node fallback (`8386`) 会让 state coupling 影响 overlay target。它应记录为 correctness-sensitive 行为风险并先冻结/测试；如果移除 fallback 会改变当前可见行为，必须作为独立 bugfix，不作为性能优化混入。

Unsaved summary：

- `logicChainCanvasToolbar` (`7578`) 每次 render 都同步调用 `logicChainDraftDiffBanner`。
- `logicChainDraftActionSummary` 出现 5 次，draft nested action summary 在 overlay 和 diff 中重复构造。
- `_pendingDelete` 过滤 (`8235-8236`) 与 draft channel reference collection (`8239`) 在保存 payload / preview / diff 中重复走对象图。

建议：

- 合并为单一 overlay pipeline：`baseGraph -> draftOverlay -> vbdTriggerOverlay -> layout`。
- 按 `baseGraphFingerprint + draftRevision + selectedTriggerType/sourceNodeId/triggerKey + pendingDeleteState` memo overlay。
- diff 按 `draftRevision` memo，hover/zoom/pan 不改变 revision。
- 新增写回路径必须使用 stable `sourceNodeId / triggerKey / targetId`；现有 selected-node fallback 先作为 deferred behavior fix 处理。

## Correctness-sensitive state coupling

这些点不仅是性能问题，还是“修 A 炸 B”的行为风险：

- UI selection 只能表达详情/高亮/焦点；不得在新代码中作为 save target、capture writeback target 或 overlay identity 的新增 fallback。现有 selected fallback 必须先冻结并测试。
- Capture writeback 必须由后端校验 stable target id / triggerKey / draftSessionId / editLockId 一致性，不能只靠前端把 captured rows 合并到 `existingNodeEdits`。
- `connectionMode/previewCandidate` 是 UI transient。9.1.1 可先隔离和记录 current behavior；是否不计入 dirty / draftRevision 需要独立行为修复和测试。
- `_pendingDelete` 属于 frontend-only draft visual marker，不应泄漏到 save payload；已有 action/node delete 仍用 `actionDeletes` / `nodeDeletes` payload。

建议缓存 key 至少覆盖：

```text
baseGraphFingerprint
draftRevision
selectedNodeId / hoverNodeId
activeDraftNodeId
connectionMode / previewCandidate
previewColumn / previewSlot
VBD selectedTriggerType / triggerKey / sourceNodeId
pending delete state
diff expanded state
```

## Modal rerender / scroll retention 热点

已有保护：

- 通用 modal scroll capture/restore: `WebAdminFrontendScripts.java:3052-3054`
- `withPreservedModalScroll` / focus restore / caret restore 是后续拆分必须保留的行为边界。
- VBD trigger page scroll memory: `8290-8296`

风险：

- `syncLogicChainExistingEditDraft` (`8147`) 会在输入变更后调用 `rerenderLogicChainEditorPreservingUi()`；该函数保留 app view scroll/focus，不等价于保留 nested modal body scroll。
- VBD trigger modal 内 second-level page 和 capture status 在 `8374-8380` 之间由 realtime/capture writeback 直接突变 draft 并 rerender，容易造成输入期间 jank。
- VBD page stack / page key / capture writeback 后返回 detail 的滚动位置需要独立 guard；不能只守护 app view scroll。
- `showLogicChainPlacedDraftNodeEditModal` 与 `showLogicChainNewNodeModal` 都是 10k+ chars 的单行 modal builder，任何局部改动都难审。

建议：

- modal-local state 与 graph-preview state 分离。
- input 同步只更新 draft object 和局部 summary；graph preview 使用 debounce / rAF。
- modal body scroll guard 增加 nested container markers。
- 9.1.1 不改视觉系统，只在现有 modal 机制内拆 builder 与 state sync。

## Backend save validation 热点

当前 Logic Chain save 路径：

```text
WebAdminServer.handleLogicChainEditor
  -> autoSnapshotBeforeWrite
  -> WebAdminLogicChainEditorService.saveDraft
     -> writePreflight
     -> validateEditorLock
     -> graphFor(...)
     -> validateDraftRequest
        -> targetLockPreflightRequirements
        -> validateTargetLockPreflight
        -> duplicate/conflict validators
     -> sequential typed writes
        -> saveVirtualBlockDeviceDraft
        -> saveWorldDeviceProtectedDraft
        -> saveRegionControllerProtectedDraft
        -> saveActionDelete / saveActionReorder
        -> saveNodeDelete
     -> channel metadata boundary guarded separately
     -> audit/realtime/write result
```

Performance / stability concerns:

- `validateDraftRequest` (`2273`) walks nodes/edges/action edits/deletes/reorders and target lock requirements in one service.
- Duplicate / conflict checks around `3081-3293` repeatedly derive action target keys and node keys.
- Save writes are sequential and may touch VBD store, SignalDeviceStore, MapDataStore, RegionControllerStore, ActionRelay, SignalListener, Timer. Channel metadata is separated by mixed-write guard and needs explicit implementation follow-up.
- Failure handling is correctness-sensitive; performance optimization must not change ordering or fail-closed behavior.

Baseline suggestions:

- Synthetic multi-draft save validation duration: `validateDraftRequest` only.
- Target lock preflight duration by number of existing edits/action deletes/reorders.
- Protected draft save dry-run style validation duration, no actual Minecraft startup.
- Keep as soft guard first; performance regression should report trend before hard fail.

## ProtectedDraftRegistry / synchronized 风险

`WebAdminProtectedDraftRegistry` is 620 lines and uses many `public static synchronized` methods:

- `start`, `get`, `validateForLogicChainSave`, `markSaving`, `markCommitted`, `markCommitFailed`, `cancel`, `activeByEditLock`, `canMutateProtectedObject`, `expireOld`, `cancelAll`
- `expireStale` runs inside multiple synchronized accessors.
- `requiresServerCleanupBeforeTerminal` can skip terminal marking until server cleanup happens.

Risk:

- static global lock serializes all protected draft operations.
- expiry / cleanup / metadata map / authorization are mixed in one class.
- server-thread world cleanup is coupled with registry state.

9.1.1 should not rewrite concurrency semantics immediately. It should first document state transitions and add guard coverage for terminal states, duplicate start, owner/lock validation and cleanup-deferred expiry.

## Pointer-events / overlay 风险

Existing CSS has useful protections:

- `.logic-chain-edge-layer{pointer-events:none}`
- `.logic-chain-minimap{pointer-events:none}`
- `.logic-chain-draft-handles{pointer-events:none}`
- `.logic-chain-connect-plus{pointer-events:auto}`

Risk:

- `.logic-chain-draft-slot` is a real button and `startLogicChainPan` explicitly excludes `.logic-chain-draft-slot`; edit-mode empty legal slots can block canvas panning.
- This may be intended, but it is undocumented as an edit-mode pointer behavior.

Guard suggestion:

- Add marker guard that edge layer/minimap/draft handles remain `pointer-events:none`.
- Add explicit docs/test marker for draft slot pointer behavior before changing it.

## Minimap baseline

当前 minimap 由 `logicChainMinimap(graph)` 单独生成，最多取 `segments.slice(0,24)`，并通过 `.logic-chain-minimap{pointer-events:none}` 避免抢事件。9.1.1 优化不得无意把 minimap 改成 draft-aware、可点击或参与 hover/click selection。建议加 baseline：

- minimap HTML snapshot / marker smoke。
- `segments.slice(0,24)` 上限 marker。
- pointer-events none CSS marker。
- graph render 拆分后 minimap 输入仍是当前语义的 graph。

## Phase 6 implemented performance baseline

Phase 6 first establishes a hard DOM equivalence baseline and then applies only current-render, low-risk optimizations.

Implemented:

- `logicChainRelatedNodeIndex(graph)` precomputes related node ids once inside `logicChainMindMap()` for the current render and passes the index down to each node card. It removes the previous `logicChainNodeCard -> logicChainRelatedNodeIds -> scan all graph.edges` pattern from every card render while preserving current hover/selected class semantics and without mutating the graph object.
- `logicChainMinimapKey(graph)` and `logicChainMinimap(graph)` memoize the exact `segments.slice(0,24)` minimap HTML by channel id and downstream count. The memo stores only `{key, html}`, not graph references; the minimap remains not draft-aware, not hover-aware and not clickable.
- `WebAdminPerformanceBaselineGuardTest` now records app.js before/after bytes and runs synthetic Logic Chain render checks for initial, selected, hover, edit mode, draft overlay, unsaved-expanded diff, VBD trigger overlay, SignalListener/Timer pending-delete, VBD selected fallback/source priority and minimap cap scenarios.
- DOM equivalence signatures hard-guard node position/class output, edge path `d`, `marker-end`, target arrow owner markers, selected/related/dimmed classes, selected panel HTML, diff banner HTML, minimap HTML, VBD overlay markers, pending-delete visual/payload boundaries across SignalListener and Timer draft action buckets, and VBD trigger source identity.

Deferred:

- Hover class-only updates remain deferred because hover and selected node can change target arrow ownership and `marker-end`.
- Click/select panel-only updates remain deferred because selection affects graph card classes, edge classes, arrow ownership and edit-mode draft targeting.
- Zoom transform-only updates remain deferred until toolbar percentage and pan bounds are covered by a focused interaction guard.
- Draft overlay, VBD overlay and diff cross-render memoization remain deferred until a reliable draft fingerprint/revision covers direct mutations, selected fallback and capture writeback.

Phase 6 app.js before baseline:

```text
bytes: 1,843,648
sha256: 057e7e370d555036aff6d542b3ae4361f82d734b8fa95cf429d4d7ac7425beb3
```

Phase 6 app.js current after baseline:

```text
bytes: 1,846,211
sha256: 474cc3093532f70d78583f996e8d6606496f45db831232f32607439a821a0069
delta: +2,563
```

## 建议性能基线

### Unit / smoke 可做

| Baseline | Tooling | Initial mode |
| --- | --- | --- |
| `app.js` UTF-8 bytes | Java unit/guard calls `WebAdminFrontendScripts.appJs()` | soft warning current + 5% |
| `app.css` UTF-8 bytes | Java unit/guard calls `WebAdminFrontendStyles.appCss()` | soft warning current + 5% |
| JS syntax check | export to `build/tmp/webadmin-app.js`, `node --check` | hard fail |
| JS parse/compile time | Node `vm.Script` in render smoke harness | soft trend |
| First WebAdmin app route render | existing Node smoke harness | soft trend |
| Logic Chain initial render | synthetic graph in Node harness | soft trend |
| Enter edit mode render | synthetic graph + edit state | soft trend |
| Add node / draft overlay render | synthetic graph + draft node | soft trend |
| Unsaved changes expand | Node/DOM harness | soft trend |
| DOM equivalence baseline | Node/DOM harness snapshots node position, edge `d`, `marker-end`, class names, panel HTML, minimap HTML, modal scroll/focus | hard after optimization starts |
| Save validation time | unit test around validation-only helper after split | soft trend |

### Browser perf marker 后续做

| Baseline | Tooling | Initial mode |
| --- | --- | --- |
| Hover highlight | browser perf marker or DOM class-only smoke after refactor | soft target `<16ms` small graph |
| Click/select node detail | browser perf marker + DOM equivalence | soft trend |
| Modal typing while graph is visible | browser perf marker + focus/caret/scroll marker | soft trend |
| Drag pointermove frame cost | browser perf marker | soft target no full layout when slot unchanged |
| Zoom/pan | browser perf marker | soft trend |

### 人工 / MCP later 才能做

| Baseline | Tooling | Initial mode |
| --- | --- | --- |
| Real WebAdmin first load parse + paint | manual browser or later MCP scenario | report only |
| Large real Logic Chain map interaction | manual browser or later MCP scenario | report only |
| Modal scroll/focus under realtime refresh | manual browser or later MCP scenario | report only |

## 建议优化顺序

1. Add performance markers and code-health guards before changing implementation or optimizing.
2. Freeze behavior matrix: hover/click/selection/dirty/modal/scroll/save failure semantics.
3. Split frontend bundle into modules with identical concatenated output order.
4. Extract route-table/delegated handler helpers; do not change event order until guarded.
5. Memoize base graph layout and draft overlay separately.
6. Localize hover/selection/zoom/pan updates.
7. Split backend save validation/planning/execution after guard coverage exists.
8. Only then tune ProtectedDraftRegistry synchronization or state/save contracts.
