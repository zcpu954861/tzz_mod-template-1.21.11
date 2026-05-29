# TZZ 9.1.1 Refactor Plan

## 目标

9.1.1 的目标不是新增功能，而是在不改变 9.1 行为的前提下清理技术债：

```text
先冻结行为和测试基线
再拆前端大文件
再拆事件路由和 Logic Chain render/layout/draft
再拆后端保存协调和顺序写边界
最后加性能优化和 guard 固化
```

9.1.1 不直接迁移 React/Vite，不重写 WebAdmin，不改 UI 视觉系统，不改路由模式，不改数据模型语义，不改 Logic Chain 行为，不改 VBD / WorldDevice / RegionController runtime，不新增 9.2 typed actions，不新增 Rich Text Builder。

本文是后续 9.1.1 implementation plan，不是当前 docs-only audit 之外的执行授权。任何 phase 若发现会改变 9.1 可见行为，必须停止并拆成独立 behavior-fix prompt。

## 行为冻结矩阵

| Area | Must remain unchanged |
| --- | --- |
| Condition runtime gate | Empty `conditionGroupId` skips store/context/evaluate; false gate no signal/action/consume/state write. |
| SignalBridge / ActionEngine | emit order, listener cooldown, action order and action gate semantics unchanged. |
| RegionController | enter/exit/stay trigger timing, single-action gate and realtime publication order unchanged. |
| Logic Chain save | Typed services only; no freeform graph document; permission/CSRF/same-origin/edit lock/fingerprint/validation/audit/realtime/snapshot preserved. |
| Protected draft | VBD / WorldDevice / RegionController must come from client-assisted protected draft; fake world/pos rejected. |
| Capture sessions | itemSubmit/container `logicChainDraftOnly` does not write formal VBD before final Logic Chain save. |
| Channel ownership | Channel Endpoint remains metadata/reference; SignalEmitter/VBD output and SignalReceiver/ActionRelay input remain graph-edge owned and editable channel fields must not be restored. |
| Node delete | reference node delete remains rejected; typed-owned delete remains draft-only; single node delete fail-closed; VBD unbind must not destroy world block; physical delete warning remains. |
| ConditionGroup compatibility | available list / compatibility profile / backend validation stay consistent; missing/disabled/invalid/incompatible groups fail-safe with Chinese-readable errors. |
| StateVariable | only GLOBAL/PLAYER + BOOLEAN/INTEGER/STRING; stable identity rename remains rejected; state_variable action-first visual remains unchanged. |
| UI refresh | silent refresh must not reset scroll/filter/page/input/modal/draft state. |
| Modal/dirty | validation failure keeps user input; dirty exit confirm remains; edit lock visible and disabled states remain. |
| Action model | No new `ActionType`; no old action cross-bucket arbitrary move; same-index/same-bucket semantics preserved. |

## State ownership matrix

| State | Owner | Lifetime | Save payload? | Realtime sync? | 9.1.1 action |
| --- | --- | --- | --- | --- | --- |
| `logicChainCanvas.zoom/pan/hover/selected/detailOpen` | frontend UI | route/view | no | no | Move to `LogicChainCanvasState`; do not add new save/capture target inference from selection. Existing selected fallback is frozen risk pending dedicated behavior fix. |
| `logicChainEditor.nodes/edges/draftChannels` | frontend draft session | edit lock | yes, after validation | no direct sync | Keep shape; add payload guard. |
| `logicChainEditor.existingNodeEdits/actionEdits/nodeDeletes/actionDeletes/actionReorders` | frontend draft + backend authority | edit lock | yes | no direct sync | Separate from UI state; backend remains authority. |
| `connectionMode/previewCandidate` | frontend UI transient | interaction | current behavior must be recorded | no | Isolate state and guard payload boundaries. Removing from dirty calculation is a separate behavior fix, not mechanical refactor. |
| VBD itemSubmit/container capture status | session service + frontend bridge | capture session | merged into draft only | yes status events | Add stable target identifiers and backend consistency validation for writeback. |
| `_pendingDelete` on draft actions | frontend-only draft visual | unsaved draft node | indirectly filtered | no | Keep but isolate helper and guard no leak to payload. |
| Protected draft registry entry | backend registry | protected session / save | referenced by id | terminal events | Split state machine/authorization/cleanup later. |
| Target typed locks | backend edit lock service + frontend heartbeat | edit session | lock ids in payload | edit lock events | Preserve preflight and failure messages. |

### Forbidden coupling rules

- UI selection may update details, highlight, focus and scroll-to-node only. New code must not use UI selection as save target, capture writeback target or overlay identity fallback.
- Existing VBD target fallback to `selectedNodeId` is a frozen behavior risk. If removing it changes current visible behavior, that removal is an independent bugfix with its own tests and current context update.
- Capture writeback must include stable target identifiers: target node id, `sourceNodeId`, `triggerKey`, `draftSessionId`, `editLockId` and expected fingerprint where applicable. Backend must validate consistency before accepting writeback.
- `connectionMode` / `previewCandidate` must not enter save payload. Any change to dirty calculation or draftRevision semantics requires explicit behavior-fix approval.
- Protected draft registry is backend-authoritative for protected session terminal state. Frontend state may display status but cannot invent committed/cancelled/expired.
- `_pendingDelete` belongs to frontend-only draft-created action visuals. Existing typed resource deletion must use `actionDeletes` / `nodeDeletes`; `_pendingDelete` must not leak into save payload.

### Protected draft state machine

| State | Frontend allowed action | Backend mutation | Realtime event | Terminal handling |
| --- | --- | --- | --- | --- |
| `selected` | confirm source / open config | create registry entry tied to actor + editLock + draftSession | selection started/status | not terminal |
| `placed` | open second-level config page | reserve protected target; reject fake world/pos | protected draft placed | not terminal |
| `configuring` | edit form, capture retry, cancel | allow mutation only through authorized session service | status/progress | not terminal |
| `saving` | disable destructive UI, show saving | `markSaving`, validate actor/editLock/draftSession | saving/status | not terminal |
| `committed` | close modal / refresh detail | `markCommitted`, release/cleanup as current code does | committed | terminal |
| `failed` | keep draft open, show Chinese error | `markCommitFailed`, return to configuring when recoverable | failed/status | not terminal if recoverable |
| `cancelled` | close/cancel UI | cancel session, cleanup if required | cancelled | terminal |
| `expired` | show expired / cleanup prompt | expire old entry, reject mutation | expired | terminal |
| `cleanup-deferred` | show pending cleanup, block save | wait for server-thread cleanup before terminal mark | cleanup pending | terminal only after cleanup |

### Delete marker ownership

| Marker / payload | Owner | Applies to | Undo / cancel | Save semantics | Guard |
| --- | --- | --- | --- | --- | --- |
| `_pendingDelete` | frontend-only draft visual | draft-created nested actions before backend id exists | undo removes marker; cancel discards local draft | filtered out before payload; never sent raw | negative string/payload guard |
| `actionDeletes` | frontend draft payload + backend authority | existing typed action resources | undo removes id from payload; cancel leaves backend untouched | backend validates target lock/conflict and deletes fail-closed | mixed-write and conflict guard |
| `nodeDeletes` | frontend draft payload + backend authority | existing typed node resources | undo removes id from payload; cancel leaves backend untouched | backend validates node delete boundary and single-delete fail-closed | node-delete fail-closed guard |
| visual pending-delete class | frontend render only | readonly/render feedback | rerender from draft state | no persistence | DOM marker guard |

### Backend state ownership

| Backend state | Owner | Boundary |
| --- | --- | --- |
| write permission / CSRF / same-origin | `WebAdminServer` route + write preflight | must remain before service mutation |
| edit lock / target typed locks | edit lock services + Logic Chain save preflight | failure keeps draft/lock where current behavior does |
| selection session lifecycle | `WebAdminSelectionSessions` / selection services | static synchronized state remains until guarded extraction |
| protected draft state | `WebAdminProtectedDraftRegistry` | actor + editLock + draftSession validation is backend-authoritative |
| protected draft world cleanup | selection sessions / server-thread cleanup path | fake world/pos rejected; no force-load chunk |
| Region planning data | `MapDataStore` + RegionController store | no `WebAdminMapServer.java` assumption |

## Phase 1: 行为冻结和测试基线

目标：

- 建立 9.1 行为冻结表、state ownership matrix、performance baseline 采集点。
- 新增 code-health guard 的 warning 版本，先记录当前预算。
- 不拆业务、不改 runtime、不改变 UI 行为。

允许修改的文件范围：

- `docs/*9_1_1*.md`
- `src/test/java/com/zcpu/tzzmod/stabilization/CodeQualityGuardTest.java` 或同等新 guard
- 可少量修改 `build.gradle` 注册新 guard task（如当前实现需要）
- 可新增测试 helper，不修改 `src/main` 业务代码

禁止修改的行为：

- 所有 runtime、WebAdmin API、WebAdmin UI 行为。
- 不修 README 版本以外的内容；README 版本一致性如要做，应作为 docs-only cleanup 明确记录。

建议模块名：

- `CodeQualityGuardTest`
- `WebAdminPerformanceBaselineGuardTest`
- `DocsConsistencyGuardTest`

验收标准：

- guard 能输出当前文件大小、函数长度、bundle bytes、BeforeVxx、event handler 预算。
- guard 初期可 warning，不因既有超标阻塞。
- `WebAdminFrontendScripts.java` 新增业务逻辑会被 warning。

回滚风险：

- 低；主要是测试/guard 噪音。

必须跑的验证：

```powershell
git diff --check
.\gradlew.bat testClasses
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
```

## Phase 2: 前端大文件拆分

目标：

- 把 `WebAdminFrontendScripts.java` 从业务巨石变成 bundle entry / concat facade。
- 不改变 `/assets/app.js` 行为、函数顺序、全局变量可见性和 marker。
- 拆 CSS 为可读模块，仍输出单个 `/assets/app.css`。

允许修改的文件范围：

- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java`
- 新增 `WebAdminFrontendCoreScripts.java`
- 新增 `WebAdminFrontendPageScripts.java`
- 新增 `WebAdminLogicChainViewerScripts.java`
- 新增 `WebAdminLogicChainEditorScripts.java`
- 新增 `WebAdminLogicChainVbdScripts.java`
- 新增 `WebAdminFrontendStyles*` 模块类，如 `WebAdminFrontendLogicChainStyles.java`
- `WebAdminFrontendAssets.java` facade 如需调整可改
- guard/test 文件

禁止修改的行为：

- 不改 route hash。
- 不改 CSS selector 语义。
- 不改 marker string。
- 不改 API URL。
- 不改 modal 文案和按钮 enabled/disabled 语义。
- 不破坏 `esc` / `jsString` / `htmlEvent` / `htmlHandler` / `innerHTML` escaping contract。
- 不改变 CSS concat 顺序和关键 selector 覆盖关系。

建议拆分模块：

- `WebAdminFrontendCoreScripts`: API, route, realtime, modal, toast, combobox primitives
- `WebAdminFrontendPageScripts`: non-Logic-Chain page renderers
- `WebAdminLogicChainViewerScripts`: viewer render, graph layout, node/edge card, minimap
- `WebAdminLogicChainEditorScripts`: edit session, draft state, save/cancel/lock
- `WebAdminLogicChainVbdScripts`: VBD native trigger, itemSubmit/container capture bridge

验收标准：

- `WebAdminFrontendScripts.appJs()` 只做稳定顺序拼接。
- `WebAdminFrontendAssets` 继续作为 Shell/Styles/Scripts facade，`WebAdminServer` 不直接绕过它。
- 不新增独立 WebAdmin 前端工程、`vite.config`、npm build 运行前置、CDN framework 或 React/Vite runtime。
- `node --check` 通过。
- 现有 render smoke 和 9.1 marker guard 通过。
- app.js bytes 不应意外增长超过 current + 2%。

回滚风险：

- 中；Java text block 拆分可能造成缺分号、作用域顺序或 helper hoisting 问题。

必须跑的验证：

```powershell
.\gradlew.bat stabilizationGuardTest --rerun-tasks
git diff --check
```

如拆分 CSS/JS 大量文本，应额外跑已有 WebAdmin render smoke 或 `node --check` guard。

## Phase 3: 事件路由表 / handler 拆分

目标：

- 把全局 delegated click/key/pointer/mouse handlers 从 if/closest 堆叠迁移为 route table。
- 保留 capture/bubble 顺序和 early return 语义。
- 新增 UI 只用 `data-action` / delegated handler；禁止新 inline `onclick`。

允许修改的文件范围：

- 前端 JS 模块文件
- guard/test 文件

禁止修改的行为：

- 不改变 combobox outside click close 规则。
- 不改变 ESC 优先级：combobox -> connection mode -> condition editor -> cancel confirm -> modal。
- `WebAdminFrontendScripts.java:5294` 的独立 global keydown ESC router 必须纳入同一事件路由审计，不得漏拆或改变优先级。
- 不改变 Logic Chain capture retry pointerup/click 双入口。
- 不改变 modal dirty confirm。

建议模块名：

- `WebAdminFrontendEventRouterScripts`
- `LogicChainEventRouter`
- `CustomComboboxEventRouter`

验收标准：

- `document.addEventListener` 数量不增加。
- `.closest(` 总量下降或至少不增加。
- `handleLogicChainEditorDelegatedClick` 不再直接承载多业务分支。
- Guard 对 global handler slice 的 `if/closest/querySelector` 建预算。

回滚风险：

- 中高；事件顺序极易改行为。必须小步提交、每步 smoke。

必须跑的验证：

```powershell
.\gradlew.bat stabilizationGuardTest --rerun-tasks
git diff --check
```

人工验收建议在后续 prompt 中覆盖：combobox、modal dirty、ESC、Logic Chain card click、connection mode、capture retry。

## Phase 4: Logic Chain render/layout/draft 模块拆分

目标：

- 拆分 render、layout、draft overlay、VBD overlay、diff summary。
- 先显式 pipeline，不立即做激进优化。实际 pipeline 是 `renderLogicChainViewer -> logicChainRenderedGraphWithDraftOverlay -> wrapper/VBD overlay -> logicChainCanvas -> layout -> mind map + minimap`。
- 建立 base layout 与 draft overlay 的输入 key。

允许修改的文件范围：

- `WebAdminLogicChainViewerScripts.java`
- `WebAdminLogicChainEditorScripts.java`
- `WebAdminLogicChainVbdScripts.java`
- guard/test 文件

禁止修改的行为：

- 不改 graph visual semantics：producer/source/consumer/join/timer/state/gate/action/reference 的布局含义。
- 不改 target-channel adjacent placement。
- 不改 RegionController owner follows action group。
- 不改 VBD trigger stable identity。
- 不改 pending-delete visuals。
- 不改 readonly/deferred detail behavior。

建议模块名：

- `LogicChainGraphOverlay`
- `LogicChainLayoutV2`
- `LogicChainDraftOverlay`
- `LogicChainVbdTriggerOverlay`
- `LogicChainDiffSummary`

验收标准：

- `logicChainApplyVbdNativeTriggerDraftGraphOverlay` 只存在一个最终定义。
- `BeforeV13-17` wrapper 数量开始下降。
- hover/click 仍可用；draft slot、connect plus、minimap pointer behavior unchanged。
- minimap 仍由 `logicChainMinimap(graph)` 独立生成，`segments.slice(0,24)` 和 `pointer-events:none` 不变。
- synthetic render baseline 记录 initial render / edit mode / add node / hover / VBD overlay / minimap snapshot.

回滚风险：

- 高；render/layout 与 UI 状态耦合重。必须保留 behavior marker，先拆函数不改算法。

必须跑的验证：

```powershell
.\gradlew.bat stabilizationGuardTest --rerun-tasks
git diff --check
```

如拆分 JS text block，应包含 app.js export + `node --check` + render/marker smoke；不得只依赖 Java 编译。

## Phase 5: 后端 LogicChainEditorService 拆分

目标：

- 把 `WebAdminLogicChainEditorService` 拆为验证、操作规划、保存协调、typed write adapters。
- 保持现有顺序写和 fail-closed 语义。
- 不引入跨 store 原子事务假象。
- 先不大改 `WebAdminServer`；`handleLogicChainEditor` / `autoSnapshotBeforeWrite` 作为 route/snapshot adapter 保持行为，若抽取必须有单独 guard。

允许修改的文件范围：

- `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorService.java`
- 新增同包 services/adapters
- `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorServiceTest.java`
- stabilization guard

禁止修改的行为：

- 不改变 validation error code / 中文消息，除非测试同步明确要求。
- 不改变 write preflight、CSRF/same-origin、edit lock、fingerprint、audit、realtime、snapshot。
- 不改变 save order：node creates / action append/edit/delete/reorder / node delete / typed metadata / channel metadata boundary 的现有约束。
- `channelMetadataDrafts` 不是普通 typed write participant；mixed typed drafts + channel metadata 仍 fail-closed，纯 metadata 边界需单独确认。
- 不放宽 same target multi-write conflict。
- 不改变 recoverable failure keeps draft/lock。

建议模块名：

- `LogicChainDraftValidationService`
- `LogicChainDraftOperationPlanner`
- `LogicChainDraftSaveCoordinator`
- `LogicChainTypedWriteExecutor`
- `LogicChainProtectedDraftCommitService`
- `VbdDraftCommitService`
- `WorldDeviceDraftCommitService`
- `RegionControllerDraftCommitService`
- `LogicChainActionMaintenanceAdapter`
- `LogicChainNodeDeleteAdapter`
- `LogicChainServerRouteSnapshotAdapter` only if guarded; otherwise deferred

建议拆分小步：

1. Extract validation helpers without changing response shape.
2. Extract operation planner that produces typed write plan but does not execute.
3. Extract save coordinator preserving current order and fail-closed checks.
4. Extract typed write executors for VBD / WorldDevice / RegionController / action maintenance / node delete.
5. Extract protected draft commit services with actor + editLock + draftSession validation preserved.
6. Keep `WebAdminServer` route/snapshot adapter stable, or extract only with marker tests.

Dependency fan-in note:

- `WebAdminLogicChainEditorService` currently owns or constructs VBD lifecycle/native trigger/device services. Splitting must either inject these through explicit facades or keep a coordinator facade; do not hide dependencies behind static helpers.
- Region-related planner must reference `MapDataStore` / RegionController stores; do not introduce or document a fake `WebAdminMapServer.java`.

验收标准：

- `WebAdminLogicChainEditorService` 目标降到 < 1000 行或只作为 facade/coordinator。
- `saveDraft` 目标降到 < 120 行。
- Existing tests pass; new tests cover operation planning and conflict validation.
- No runtime or API response shape change.

回滚风险：

- 高；save path 涉及多 store side effects。必须从 pure validation/planning 先拆，再拆 execution。

必须跑的验证：

```powershell
.\gradlew.bat test --tests com.zcpu.tzzmod.webadmin.service.WebAdminLogicChainEditorServiceTest
.\gradlew.bat stabilizationGuardTest --rerun-tasks
git diff --check
```

## Phase 6: 性能优化和缓存

目标：

- 在 Phase 2-5 拆分稳定后做局部优化。
- 优先降低 hover/click/zoom/drag 的全量 render。
- 缓存 base layout 与 draft overlay。

允许修改的文件范围：

- Logic Chain frontend modules
- performance guard/test 文件

禁止修改的行为：

- 不改变 layout visual output，除非有 DOM marker 证明等价；截图矩阵只在后续 prompt 明确授权时运行。
- 不改变 selected/detail/dirty/modal/scroll retention、arrow owner、edge `marker-end`、right panel、modal focus/caret。
- 不改变 save payload。

建议优化：

- `graphKey/fingerprint + viewMode + nodeTypeFilter + focusNode` memo base layout。
- `draftRevision` memo draft overlay / diff summary。
- adjacency map 一次性构建，node card 不重复扫描 edges。
- hover class-only update，必须先有 DOM 等价 baseline。
- zoom DOM transform-only update，必须保持 toolbar/pan bounds/selection/detail 语义。
- drag pointermove rAF 合并，slot unchanged no render。

验收标准：

- Small synthetic graph hover target `<16ms` soft guard。
- Large synthetic graph hover target `<50ms` soft guard。
- Drag pointermove 不在 slot unchanged 时 full layout。
- DOM 等价 baseline 覆盖 node position、edge `d`、`marker-end`、arrow owner、node/edge class、diff banner、right panel、minimap HTML、modal scroll/focus。
- app.js size 不因缓存实现显著增长。

回滚风险：

- 中高；缓存失效 key 错误会产生 stale graph。必须先 marker / debug counters。

必须跑的验证：

```powershell
.\gradlew.bat stabilizationGuardTest --rerun-tasks
git diff --check
```

真实浏览器/MCP 性能验收应作为后续专门 prompt，不在 9.1.1 docs-only audit 中运行。

## Phase 7: Guard 固化

目标：

- 把 9.1.1 清理成果转为防复发 guard。
- 从 soft warning 逐步改 hard fail。

允许修改的文件范围：

- `src/test/java/com/zcpu/tzzmod/stabilization/*GuardTest.java`
- `build.gradle`
- docs / README consistency 文档

禁止修改的行为：

- Guard 不应要求启动 Minecraft。
- Guard 不应跑 MCP scenario。
- Guard 不应生成截图矩阵。

建议 guard：

- file size / method length / JS function char budget
- no new `BeforeV\d+`
- `WebAdminFrontendScripts.java` no new business logic
- app.js / app.css bytes
- `document.addEventListener` / `.closest` / inline handler budgets
- pointer-events overlay guard
- `node --check`
- Logic Chain marker guard
- performance marker baseline
- docs consistency guard

验收标准：

- 9.2 开发前 guard 能阻止重新堆叠巨型文件、BeforeVxx wrapper 和 inline handler。
- README stable baseline 与 current context 不再冲突。
- `StabilizationGuardTest` 不再继续无限膨胀。
- Phase 7 已固化：`BeforeVxx` 历史 token no-growth、app.js/app.css exact ratchet、new module byte budget、known giant JS function no-growth、raw JSON summary no-growth、React/Vite/npm/CDN runtime negative scan、Phase 6 cache/source invariants 和额外 DOM hash baseline。
- Phase 7 中文注释只写在 Java-side 模块边界和 backend split 边界，不进入 JS text block，因此不改变 generated app.js / app.css。
- Phase 7.5 已插入 Phase 7 checkpoint 前：复杂度热点表、safety-if 分类、deferred 清单和 `IF_COMPLEXITY_HOTSPOT_AUDIT_9_1_1.md` 必须完成后才能进入 Phase 8。

回滚风险：

- 低到中；主要是 guard 过严导致开发阻塞。初始阈值应 current + small delta，然后逐步收紧。

必须跑的验证：

```powershell
.\gradlew.bat testClasses
.\gradlew.bat codeQualityGuardTest --rerun-tasks
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
git diff --check
```

如阶段涉及 MCP 工具本身，再跑：

```powershell
cd tools\tzz-test-mcp
npm run build
npm test
```

## 分阶段提交建议

9.1.1 implementation 不应一次性做完整大重构。建议每个 Phase 至少一个 checkpoint，且 checkpoint 前必须满足对应验证命令。每个 checkpoint 只显式暂存本阶段相关文件，不使用 `git add .`，不提交 `.codex/`、`logs/`、`reports/mcp/`、screenshots、node_modules、build、run、`.gradle/`。
