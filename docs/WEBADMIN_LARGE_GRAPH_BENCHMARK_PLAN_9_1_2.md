# TZZ 9.1.2 WebAdmin Large Graph Benchmark Plan

## Scope

This plan covers WebAdmin and Logic Chain large-graph performance. It does not approve UI behavior changes. Optimizations in Phase 3 require Phase 1 synthetic fixtures plus hard DOM equivalence coverage.

Important current facts:

- WebAdmin frontend is Java string resources, not React/Vite.
- Logic Chain hover, selection and zoom currently route through full `renderLogicChainViewer` behavior.
- 9.1.1 already added render-local related-node index and minimap memoization.
- Phase 3 adds a hard DOM-equivalence guard and render-local edge index reuse, but hover/select local updates remain deferred because they can change node classes, edge classes, arrow owner and `marker-end`.

## Graph Fixture Sizes

| Tier | Nodes | Edges | Role |
| --- | ---: | ---: | --- |
| small | 20 | 30 | quick smoke and DOM hash baseline |
| medium | 100 | 200 | common admin chain |
| large | 500 | 1000 | low-end risk target |
| stress | 2000 | 5000 | report-only growth curve |

All graph generation must use deterministic seed `912012`.

Required node/edge coverage:

- channel;
- join;
- timer;
- SignalListener;
- ActionRelay;
- SignalReceiver;
- SignalEmitter;
- VBD;
- RegionController;
- StateVariable;
- ConditionGroup gate;
- draft nodes;
- pending-delete nodes/actions;
- VBD trigger overlay;
- unsaved diff expanded.

## Benchmark Operations

Each WebAdmin graph benchmark row must use the required low-end fields below so the graph report is self-contained:

| Field | Meaning |
| --- | --- |
| `measured_ms` | local measured duration. |
| `estimated_low_end_ms_x3` | `measured_ms * 3`. |
| `estimated_low_end_ms_x5` | `measured_ms * 5`. |
| `estimated_very_low_end_ms_x10` | `measured_ms * 10`. |
| `scale_factor_from_previous_size` | growth from previous graph tier. |
| `complexity_class_estimate` | measured/static complexity estimate. |
| `risk_level` | `PASS`, `WARN` or `FAIL`. |
| `reason` | Chinese explanation of low-end risk and DOM-equivalence status. |
| `bytes` | generated HTML/DOM string size when available. |

| Operation | Current suspect | Guard before optimization |
| --- | --- | --- |
| initial render | full route and canvas render | node/edge/panel/minimap hash. |
| edit mode render | draft overlay and toolbar | save payload and draft slot hash. |
| hover highlight | full render through `highlightRelatedEdges` | selected/related/dimmed class, arrow owner, `marker-end`. |
| click selection | full render plus right panel | panel HTML and graph class hash. |
| zoom / pan | zoom currently rerenders, pan is transform oriented | toolbar percentage, pan transform and bounds. |
| drag preview | pointermove can cause preview/full layout | legal slot, preview class and no input loss. |
| draft overlay | cloned graph plus slot layout | draft node/edge stable identity. |
| VBD trigger overlay | VBD trigger output preview | source node, triggerKey, target channel and no duplicate card. |
| unsaved diff expanded | diff rows rebuilt | diff banner HTML. |
| minimap | capped segment HTML | `segments.slice(0,24)` and pointer-events. |

## Top 50 WebAdmin Render/Layout Functions

| # | Function/path | Trigger | Risk | Required equivalence |
| ---: | --- | --- | --- | --- |
| 1 | `renderLogicChainsPage` | list route | WARN | list table row snapshot. |
| 2 | `renderLogicChainList` | list route/silent refresh | WARN | list state and expanded keys. |
| 3 | `logicChainTable` | list route | PASS/WARN | table HTML. |
| 4 | `logicChainPrimaryRows` | list route | WARN | ordering. |
| 5 | `logicChainTableRow` | list route | WARN | row markers/navigation. |
| 6 | `renderLogicChainDetail` | detail route | WARN | route data load and focus. |
| 7 | `renderLogicChainViewer` | all graph interactions | WARN/FAIL | full DOM signatures. |
| 8 | `logicChainRenderedGraphWithDraftOverlay` | render/edit | WARN | rendered graph identity. |
| 9 | `logicChainGraphWithNewDraftDetails` | edit render | WARN | new draft detail output. |
| 10 | `logicChainCanvasToolbar` | every render | WARN | toolbar/diff HTML. |
| 11 | `logicChainDraftDiffBanner` | toolbar | WARN | diff HTML. |
| 12 | `logicChainDraftDiffRows` | diff expanded | WARN | row order and Chinese text. |
| 13 | `logicChainDraftActionSummary` | diff/overlay | WARN | action summary. |
| 14 | `logicChainCanvas` | every graph render | WARN | canvas shell. |
| 15 | `logicChainLayoutGraphV2` | every graph render | WARN | node coordinates and edge paths. |
| 16 | `logicChainEdgeIndexes` | layout | WARN | graph traversal result. |
| 17 | `logicChainBuildTree` | layout | WARN | tree order. |
| 18 | `logicChainTreeFromNode` | layout | WARN | cycle/max-depth behavior. |
| 19 | `logicChainApplyFixedNodeHeights` | layout | PASS/WARN | node y/height. |
| 20 | `logicChainLayoutWithDraft` | edit render | WARN | draft slot identity. |
| 21 | `logicChainDraftMetrics` | edit render | WARN | slot metrics. |
| 22 | `logicChainAdjustSavedProducerLayout` | layout | WARN | producer placement. |
| 23 | `logicChainMindMap` | canvas render | WARN | node/edge HTML. |
| 24 | `logicChainEdgePath` | edge render | WARN | `d`, class, `marker-end`. |
| 25 | `logicChainEdgeClasses` | edge render | WARN | selected/dimmed/highlight classes. |
| 26 | `logicChainAnnotateTargetArrowOwners` | edge render | WARN | owner markers. |
| 27 | `logicChainNodeCard` | node render | WARN | class, marker, action controls. |
| 28 | `logicChainNodeCardClasses` | node render | WARN | selected/related/dimmed. |
| 29 | `logicChainRelatedNodeIndex` | graph render | PASS/WARN | render-local only, no graph retention. |
| 30 | `logicChainRelatedNodeIds` | fallback/helper | WARN | no repeated full scan in optimized path. |
| 31 | `logicChainMinimap` | canvas render | PASS/WARN | capped HTML. |
| 32 | `logicChainMinimapKey` | canvas render | PASS | key excludes transient state. |
| 33 | `logicChainSelectedNodePanel` | selection render | WARN | right panel HTML. |
| 34 | `logicChainReadonlyNodeDetail` | panel | WARN | readonly/deferred Chinese text. |
| 35 | `logicChainNodeMetadataRows` | panel | WARN | detail rows. |
| 36 | `logicChainVbdNativeTriggerCards` | panel/overlay | WARN | trigger stable identity. |
| 37 | `logicChainVbdNativeTriggerOutputRows` | VBD draft | WARN | output channel rows. |
| 38 | `logicChainVbdTriggerOutputSummary` | VBD diff | WARN | Chinese summary. |
| 39 | `logicChainApplyVbdNativeTriggerDraftGraphOverlay` | VBD edit render | WARN | source/target/triggerKey. |
| 40 | `logicChainVbdOverlaySourceCard` | VBD overlay | WARN | selected fallback/source priority. |
| 41 | `logicChainDraftSlotOverlay` | edit canvas | WARN | legal slots. |
| 42 | `logicChainDraftNodeOverlay` | edit canvas | WARN | draft position/card. |
| 43 | `logicChainDraftChannelOverlay` | edit canvas | WARN | channel endpoint identity. |
| 44 | `logicChainActionDraftsForPreview` | edit render | WARN | unique action key. |
| 45 | `logicChainConfirmedActionDrafts` | save/diff | WARN | payload and diff. |
| 46 | `logicChainConfirmedNodeDeleteDrafts` | save/diff | WARN | pending delete behavior. |
| 47 | `logicChainConfirmedActionDeleteDrafts` | save/diff | WARN | pending delete behavior. |
| 48 | `logicChainConfirmedActionReorderDrafts` | save/diff | WARN | reorder preview. |
| 49 | `syncLogicChainExistingEditDraft` | modal input | WARN | input preservation. |
| 50 | `renderIcons(appView())` | after full render | WARN | icon pass should not hide syntax/render errors. |

## DOM Equivalence Matrix

Hard checks needed before any Phase 3 local-update optimization:

| Surface | Snapshot fields |
| --- | --- |
| node geometry | id, type, x, y, width, height, lane, class list |
| edge path | from, to, type, path `d`, class list |
| marker ownership | `marker-end`, target owner markers |
| hover/selection classes | selected, related, dimmed, highlighted |
| right detail panel | full panel HTML or normalized hash |
| diff banner | collapsed and expanded HTML hashes |
| minimap | marker HTML, segment cap, pointer-events |
| VBD overlay | source node, triggerKey, target channel, no duplicate trigger card |
| draft slots | slot id, owner, column, disabled state |
| save payload | no `_pendingDelete` leakage; same typed payloads |
| zoom/pan | transform, toolbar percentage, pan state |
| modal input | value, caret/focus, scroll and dirty state |

## Phase 3 Guarded Implementation

Implemented guard:

- `src/test/java/com/zcpu/tzzmod/stabilization/WebAdminLogicChainDomEquivalenceGuardTest.java`
- wired from `CodeQualityGuardTest`;
- hard-fails missing snapshots, source marker drift and baseline changes;
- keeps timing and low-end estimates report-only in `WebAdminPerformanceBaselineGuardTest`.

Phase 3 hard snapshots now cover:

| Surface | Phase 3 hard-check detail |
| --- | --- |
| node geometry/class | `nodeHash`, `classHash`, selected/related/dimmed counts for initial, hover, selection, draft, unsaved and VBD scenarios. |
| edge identity/path | `edgeHash` includes from/to/type attrs, visual group/style, route shape, `d`, `marker-end`, arrow owner and VBD trigger identity attrs. |
| right detail panel | `panelHash` covers the selection scenario and selected-panel edge-index reuse. |
| diff banner | `diffHash` covers draft and unsaved-expanded scenarios; `_pendingDelete` leakage remains hard-fail guarded. |
| minimap | `minimapHash` and `minimapSegments=24` preserve the cap. |
| VBD overlay | `vbdHash`, `vbdSourceNodeIds`, `vbdTriggerKeys` and `vbdDraftSourceNodeId` preserve source priority and selected trigger identity. |
| interaction equivalence | hover, selection and zoom interaction paths are compared against canonical full-render states. |

Accepted production changes:

- `logicChainEdgeIndexes` now also returns `traversalForward` / `traversalReverse` so view-mode traversal reuses the same render-local index.
- `renderLogicChainViewer` passes `detailEdgeIndexes` into `logicChainSelectedNodePanel`; the panel reuses `byFrom` / `byTo` rather than scanning all edges twice.
- `logicChainEdgePath` emits nonvisual edge identity attributes for guard extraction. CSS and event delegation do not consume these attrs.

Still deferred:

- hover class-only update;
- selection panel-only update;
- zoom transform-only update;
- drag pointermove coalescing;
- cross-render draft diff memoization without a reliable draft revision/fingerprint;
- broad VBD overlay memo or rewrite.

## Low-End Acceptance Notes

Frame-oriented operations such as hover, click, drag and zoom should be judged against low-end estimates:

- high-end local target: normally under 16ms for frequent interactions;
- x5 low-end estimate: should avoid obvious 50ms+ stalls;
- x10 estimate: used to flag very low-end risk;
- any `O(nodes * edges)` or full render per hover remains at least `WARN`, even if the local machine reports small absolute values.
