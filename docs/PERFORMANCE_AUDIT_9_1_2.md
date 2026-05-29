# TZZ 9.1.2 Performance Audit

## Scope

This is the Phase 0 audit for 9.1.2 real performance profiling and deep simplification. It is intentionally docs-first:

- no runtime semantic change;
- no WebAdmin API or Logic Chain visible behavior change;
- no Minecraft startup;
- no MCP scenario run;
- no screenshot matrix;
- no React/Vite/npm frontend runtime.

The stable baseline is the 9.1.1 code-health stabilized codebase. This audit treats older 9.1.1 performance notes as historical input, then rechecks the current source shape before planning 9.1.2.

## Hardware And Low-End Models

The local development machine is high-end and must not be treated as the acceptance target:

| Profile | Hardware model | Purpose |
| --- | --- | --- |
| User dev machine | Intel Core i7-14700KF, 64GB DDR5, RTX 4080 Super | Upper-bound reference only. Passing here does not prove low-end safety. |
| Low-end client | older 4 core / 8 thread CPU, 8GB-16GB RAM, integrated or entry GPU, Chromium browser | WebAdmin large Logic Chain must avoid long UI stalls. |
| Average client | 6 core / 12 thread CPU, 16GB RAM, mid GPU | Medium Logic Chain editing should remain fluid. |
| Low-end server / VPS | 2-4 vCPU, 4GB-8GB RAM, ordinary SSD | Runtime tick, store save and condition evaluation must not cause visible TPS jitter. |

Benchmark rows must include local measured time plus low-end estimates:

| Field | Meaning |
| --- | --- |
| `measured_ms` | local measured duration on the current machine |
| `estimated_low_end_ms_x3` | `measured_ms * 3` |
| `estimated_low_end_ms_x5` | `measured_ms * 5` |
| `estimated_very_low_end_ms_x10` | `measured_ms * 10` |
| `scale_factor_from_previous_size` | growth from previous fixture tier |
| `complexity_class_estimate` | observed or static estimate, for example `O(n)`, `O(n log n)`, `O(n*m)`, `O(n^2)` |
| `risk_level` | `PASS`, `WARN`, or `FAIL` |
| `reason` | Chinese explanation of risk, growth curve and semantic guard |
| `bytes` | generated HTML, WebAdmin response, JSON load/save or snapshot package bytes when relevant |
| `serialization_count` | JSON parse/serialize or large response build count when relevant |
| `write_frequency` | per tick, per signal, per save, per manual request or batch |
| `cleanup_complexity` | session/cache/registry cleanup estimate, for example `O(active)` |

Risk policy:

- `PASS`: low-end estimate is still acceptable and growth is bounded or close to linear.
- `WARN`: high-end local timing may pass, but low-end estimate, allocation pressure or growth curve is risky.
- `FAIL`: high-frequency full recompute, tick-path IO, near-square growth, unbounded session/cache growth or behavior equivalence gap.

## Phase 0 Executive Findings

| Area | Current risk | Low-end status | Phase |
| --- | --- | --- | --- |
| Logic Chain hover/click/zoom | `highlightRelatedEdges`, selection and zoom still trigger full viewer render; hover/select also affect arrow owner and `marker-end`. | `WARN` now, `FAIL` if large/stress benchmark shows repeated full layout above low-end frame budget. | Phase 1 guard first, Phase 3 optimize only after DOM equivalence. |
| Logic Chain render/layout | layout, draft overlay, VBD overlay, diff banner and minimap all participate in full render paths. | `WARN`; existing 9.1.1 related-index/minimap memo helps but does not solve interaction full-render. | Phase 1/3. |
| Signal device lookup | several runtime paths call list-backed `SignalDeviceStore` lookup or snapshot helpers. | `WARN`; could become `FAIL` at 5000 devices if channel emit scans unrelated devices. | Phase 1/2. |
| VBD tick | dispatcher and content-change handler scan VBD config snapshots each server tick while respecting chunk-loaded bounds; this is a config scan, not a global world scan. | `WARN` to `FAIL` on low-end VPS if VBD count reaches 1000. | Phase 1/2. |
| RegionController tick | players x enabled controllers, then planner-region lookup and polygon contains. | `FAIL` risk for 500 players x 1000 controllers without bounds/index. | Phase 1/2. |
| TimerRuntimeService tick | active timer map is bounded but scanned each tick; due execution budget exists. | `WARN`; preserve deterministic order before optimizing. | Phase 1/2. |
| ConditionGateService | each nonblank group path loads condition groups and may build state snapshots. | `FAIL` risk for frequent gated actions if JSON reload remains on hot path. | Phase 1/2/4. |
| StateVariableStore | runtime context can read store snapshot through service/load path. | `FAIL` risk if repeated per gate; must cache safely by server/path/fingerprint. | Phase 1/4. |
| Store/session registries | snapshots, protected drafts, selection sessions and template sessions need large-store and expiry tests. | `WARN`; terminal bounded maps are safer than unbounded registries, but expire scans need guard. | Phase 1/4. |
| Snapshot manifest/package | manual WebAdmin path, but large JSON load/diff can allocate heavily. | `WARN`; not tick path, but low-end VPS IO needs measurement. | Phase 1/4. |

Phase 3 follow-up:

- accepted render-local edge traversal index reuse, selected-detail-panel edge-index reuse and nonvisual edge identity attrs for DOM guard extraction;
- added `WebAdminLogicChainDomEquivalenceGuardTest` for hard node/edge/panel/diff/minimap/VBD overlay snapshots and hover/selection/zoom canonical full-render equivalence;
- kept high-risk local DOM update candidates deferred, so the Phase 0 hover/click/zoom full-render warning remains a measured risk rather than an unguarded rewrite.

Phase 4 follow-up:

- accepted bounded content-fingerprint cached loads for StateVariable and ConditionGroup stores, with save invalidation, rollback cache clearing and hard missing/corrupt/repair/external-replacement guards;
- switched only runtime condition gate and read-only replay to cached condition group loads; WebAdmin write validators and editing services remain uncached authoritative reads;
- kept StateVariable writes synchronous and preserved raw missing-file creation behavior through `StateVariableService`;
- accepted next-expiry short-circuit for container and single-item-submit template sessions only; selection sessions and protected draft registry cleanup remain unchanged because world-device protected draft rollback needs server-aware cleanup.

Phase 5 follow-up:

- accepted duplicate backend validation helper cleanup in `WebAdminVirtualBlockDeviceNativeTriggerService.validateGateBinding(...)` by delegating to `WebAdminConditionGateBindingValidator`;
- kept VBD-specific dynamic container compatibility profile ownership in the VBD service, so inventory snapshot availability still determines container open/close gate compatibility;
- strengthened condition gate config characterization for VBD fields, exact error code, rejected value summary, Chinese message fragments, blank-id no-load behavior and degraded store handling;
- kept production JS routing, giant UI builders, BeforeVxx wrappers, protected draft registry, timer due structure and VBD runtime scan narrowing deferred because their behavior equivalence is not yet automatically proven.

Phase 6 follow-up:

- ratcheted `WebAdminVirtualBlockDeviceNativeTriggerService.java` into `CodeQualityGuardTest` file line/byte no-growth baselines after the Phase 5 duplicate validator cleanup;
- confirmed the existing 9.1.2 guard suite already reports runtime, synthetic graph and store/session benchmark rows with low-end estimate fields while keeping noisy timing report-only;
- kept hard-fail scope on deterministic invariants: generated bundle ratchet, BeforeVxx/no-BeforeV18+ growth, inline handler and selector no-growth, DOM/source marker equivalence, store corrupt/missing/cache invalidation and session cleanup markers;
- updated the external Obsidian knowledge-base plan so quick entries are categorized by task and Phase 5/6 decisions are indexed as durable repository memory.

## Top 50 Static Performance Suspects

This table is the Phase 0 static candidate list. Phase 1 must convert it into deterministic benchmark rows with timing and low-end estimates. `Tick path` means server/client tick or synchronous Minecraft main-thread path; `High frequency UI` means hover, pointermove, click, zoom or route silent refresh.

| # | Path | Trigger | Complexity suspect | Risk | Required proof before optimization |
| ---: | --- | --- | --- | --- | --- |
| 1 | `WebAdminLogicChainViewerScripts.renderLogicChainViewer` | hover, click, zoom, edit, silent refresh | full route/render rebuild | WARN | DOM snapshots for canvas, panel, diff, minimap, save payload. |
| 2 | `WebAdminLogicChainCanvasScripts.highlightRelatedEdges` | node hover/out | full Logic Chain render | WARN | Arrow owner and `marker-end` equivalence. |
| 3 | `WebAdminLogicChainCanvasScripts.setLogicChainZoom` | zoom controls | full Logic Chain render for transform-like change | WARN | Toolbar percentage and pan bounds guard. |
| 4 | `WebAdminLogicChainViewerScripts.rerenderLogicChainEditorPreservingUi` | modal/edit state sync | full graph rebuild plus focus restore | WARN | modal scroll/focus/caret guard. |
| 5 | `WebAdminLogicChainCanvasScripts.logicChainCanvas` | every viewer render | layout + canvas + minimap | WARN | render hash guard. |
| 6 | `WebAdminLogicChainLayoutScripts.logicChainLayoutGraphV2` | every graph render | nodes + edges + lane propagation | WARN | node position and edge path guard. |
| 7 | `WebAdminLogicChainDraftOverlayScripts.logicChainLayoutWithDraft` | edit/draft render | clone/filter/slot layout | WARN | draft slot and preview guard. |
| 8 | `WebAdminLogicChainVbdOverlayScripts.logicChainApplyVbdNativeTriggerDraftGraphOverlay` | VBD trigger draft | repeated overlay clone/filter | WARN | VBD source/target/triggerKey guard. |
| 9 | `WebAdminLogicChainDiffScripts.logicChainDraftDiffBanner` | toolbar render | diff recompute on unrelated UI changes | WARN | diff HTML and draft fingerprint guard. |
| 10 | `WebAdminLogicChainCanvasScripts.logicChainMindMap` | every canvas render | node/edge HTML generation | WARN | graph DOM hash. |
| 11 | `WebAdminLogicChainCanvasScripts.logicChainEdgePath` | every edge render | edge class/path decision reads selection/hover | WARN | edge `d`, class, `marker-end`. |
| 12 | `WebAdminLogicChainCanvasScripts.logicChainNodeCard` | every node render | class/detail derivation | WARN | node class snapshot. |
| 13 | `WebAdminLogicChainCanvasScripts.logicChainAnnotateTargetArrowOwners` | every edge render | selection/hover-sensitive ownership | WARN | arrow owner marker guard. |
| 14 | `WebAdminLogicChainCanvasScripts.logicChainMinimap` | every render path | capped HTML but repeated | PASS/WARN | memo key guard, no graph reference retention. |
| 15 | `SignalBridgeServer.emit` | runtime signal emit | channel fan-out and action dispatch | WARN | exact listener/action order guard. |
| 16 | `ActionEngine.execute` | each action | type dispatch, optional condition gate | WARN | action result/error order guard. |
| 17 | `ActionEngine.executeAll` | action chain | chain length, stop-on-failure | WARN | chain ordering and failure behavior guard. |
| 18 | `ConditionGateService.evaluate` | optional runtime gate | load group, compatibility, lazy context | FAIL risk | fail-closed and blank-gate skip guard. |
| 19 | `ConditionRuntimeContextBuilder.baseBuilder` | gated runtime | state variable snapshot load | FAIL risk | context equivalence and no JSON read in hot path. |
| 20 | `ConditionRuntimeContextBuilder.signalEventBuilder` / `signalListener` | signal/listener gate | state + signal context build | FAIL risk | no side effects, same fields. |
| 21 | `ConditionRuntimeContextBuilder.regionController` | region gate | state + region context build | FAIL risk | enter/exit/stay semantics unchanged. |
| 22 | `ConditionEvaluator.evaluateNode` | condition group eval | tree recursion | WARN | result message and fail-closed guard. |
| 23 | `ConditionRegistry.evaluate` predicates | condition nodes | repeated type-specific snapshot scans | WARN | condition snapshot output guard. |
| 24 | `VirtualBlockDeviceDispatcher.tick` | server tick | scans all VBD configs | WARN/FAIL | no force-load and bound-position semantics. |
| 25 | `VirtualBlockDeviceContainerHandler.tick` | server tick | open/close session scan plus content-change scan | WARN | open/close, chunk-loaded and cooldown semantics. |
| 26 | `VirtualBlockDeviceContainerHandler.tickContentChanges` | server tick | scans VBD configs and fingerprints only loaded containers | WARN/FAIL | no force-load and container fingerprint semantics. |
| 27 | `VirtualBlockDeviceInteractionHandler` | player interaction | matcher + gate + emit | WARN | item/container side effects unchanged. |
| 28 | `ItemSubmitEvaluator.evaluate` | VBD itemSubmit | requirements x inventory, consume pass | WARN | all-or-nothing consume guard. |
| 29 | `ContainerItemConditionSupport.TOTAL_MATCHER` | container condition | slot scan per condition | WARN | condition result equivalence. |
| 30 | `SignalDeviceStore.getSnapshot` | runtime + WebAdmin | copy/sort/list snapshot | WARN | ordering and fingerprint guard. |
| 31 | `SignalDeviceStore.getEnabledReceiversForChannel` | emit/channel lookup | snapshot then filter | WARN/FAIL | channel order and enabled filter guard. |
| 32 | `SignalDeviceStore.getEnabledActionRelaysForChannel` | emit/channel lookup | snapshot then filter | WARN/FAIL | action relay order guard. |
| 33 | `SignalDeviceStore.resolveDevice` | WebAdmin/runtime | list-backed id/source lookup | WARN | ambiguity and missing messages. |
| 34 | `SignalDeviceStore.findById` | WebAdmin/runtime | linear lookup | WARN | duplicate handling unchanged. |
| 35 | `SignalDeviceStore.findBySourcePosition` | world device path | linear lookup | WARN | dimension/position identity unchanged. |
| 36 | `RegionControllerTracker.tick` | server tick | players x controllers | FAIL risk | enter/exit/stay ordering guard. |
| 37 | `MapDataStore.getPlannerRegion` | region controller tick | synchronized linear lookup by id | WARN | exact missing-region behavior. |
| 38 | `MapDataStore.findPlannerRegionContaining` | map/player region tick | all planner regions scan | WARN/FAIL | polygon selection equivalence. |
| 39 | `RegionGeometry.containsBlock` | region hit test | polygon point-in-region | WARN | geometry result guard. |
| 40 | `TimerRuntimeService.tick` | server tick | scans active timers | WARN | due order and max-due budget guard. |
| 41 | `TimerRuntimeService.RuntimeStore.activeCount` | start storms | sums all active maps | WARN | max active limit guard. |
| 42 | `TimerRuntimeService.RuntimeStore.start` | action timer_start | duplicate/limit checks | WARN | start policy unchanged. |
| 43 | `TimerRuntimeService.RuntimeStore.cancel` | action timer_cancel | instance lookup/removal | WARN | cancel scope unchanged. |
| 44 | `StateVariableStore.getSnapshot` | runtime context/WebAdmin | synchronous JSON load path suspect | FAIL risk | cached snapshot invalidation guard. |
| 45 | `StateVariableStore.loadSnapshot` | store load | large JSON parse | WARN | corrupt JSON fallback guard. |
| 46 | `WebAdminConditionGroupStore.loadWithStatus` | runtime gate/WebAdmin | large JSON parse per call suspect | FAIL risk | validation status and Chinese messages. |
| 47 | `RegionControllerStore.getSnapshot` | runtime/WebAdmin | copy/load path | WARN | enabled/order guard. |
| 48 | `SignalListenerStore.getSnapshot` | signal emit/WebAdmin | copy/load path | WARN | listener order guard. |
| 49 | `WebAdminProtectedDraftRegistry.expireStale` | session access/expiry | synchronized full registry scan | WARN | terminal cleanup semantics. |
| 50 | `WebAdminSnapshotStore` manifest/package load/diff | manual WebAdmin path | large JSON + diff allocation | WARN | rollback manifest/package semantics. |

## Top 50 Store, Serialization And Session Paths

Phase 1 store benchmarks must cover the paths below with large JSON fixtures. Timing starts as report-only. Format/validation behavior is hard-fail guarded.

Store measurements must not assume the high-end development SSD represents production. Reports must include JSON bytes, serialization count, write frequency, x3/x5/x10 estimates, and reason text for ordinary VPS SSD variance, Windows Defender / IO jitter, large JSON growth, full-file rewrite amplification, GC pressure and object allocation pressure.

| # | Path | Data shape | Primary concern |
| ---: | --- | --- | --- |
| 1 | `SignalDeviceStore` private cached load boundary | `signal_devices.json` | large VBD/device parse and status; public callers access the synchronized cached store. |
| 2 | `SignalDeviceStore.State.flushDirty` / `forceFlushDirty` | `signal_devices.json` | full-file write after small edits; main-thread dirty flush is called from server tick. |
| 3 | `SignalDeviceStore.getSnapshot` | device list | copy/sort allocation. |
| 4 | `SignalDeviceStore.flushDirty` | dirty device store | write coalescing boundary and low-end IO jitter risk. |
| 5 | `StateVariableStore.loadSnapshot` | `state_variables.json` | runtime context parse risk. |
| 6 | `StateVariableStore.saveSnapshot` | `state_variables.json` | full-file write. |
| 7 | `WebAdminStateVariableService.load` | state variable WebAdmin | service wrapping allocation. |
| 8 | `WebAdminConditionGroupStore.loadWithStatus` | `condition_groups.json` | runtime gate parse risk. |
| 9 | `WebAdminConditionGroupStore.save` | `condition_groups.json` | full-file write. |
| 10 | `ConditionGateService.loadGroup` | condition group lookup | repeated load/filter. |
| 11 | `SignalListenerStore.loadWithStatus` | `signal_listeners.json` | list parse/order. |
| 12 | `SignalListenerStore.save` | `signal_listeners.json` | action list write. |
| 13 | `RegionControllerStore` private cached load boundary | `region_controllers.json` | controller/action parse; public callers access the synchronized cached store. |
| 14 | `RegionControllerStore.flushDirty` | `region_controllers.json` | full-file write from dirty flush. |
| 15 | `MapDataStore.getSnapshot` | map/planner regions | snapshot copy. |
| 16 | `MapDataStore.save` | planner regions | full store write. |
| 17 | `TimerStore.loadWithStatus` | `timers.json` | timer/action bucket parse. |
| 18 | `TimerStore.save` | `timers.json` | full-file write. |
| 19 | `SignalJoinStore.loadWithStatus` | `signal_joins.json` | join inputs parse. |
| 20 | `SignalJoinRuntimeService.observeAcceptedSignal` -> `SignalJoinStore.loadWithStatus` | `signal_joins.json` | accepted signal path can load/scan joins before emitting join output. |
| 21 | `WebAdminTemplateStore.loadWithStatus` | `templates.json` | template package parse. |
| 22 | `WebAdminTemplateStore.save` | `templates.json` | large template write. |
| 23 | `WebAdminChannelMetadataStore.loadWithStatus` | channel metadata | WebAdmin route load. |
| 24 | `WebAdminChannelMetadataStore.save` | channel metadata | metadata tail write. |
| 25 | `WebAdminLogicChainMetadataStore.loadWithStatus` | logic chain metadata | list route load. |
| 26 | `WebAdminLogicChainMetadataStore.save` | logic chain metadata | full-file write. |
| 27 | `WebAdminSnapshotStore.loadManifest` | snapshot manifest | large timeline parse. |
| 28 | `WebAdminSnapshotStore.saveManifest` | snapshot manifest | full manifest write. |
| 29 | `WebAdminSnapshotStore.loadPackage` | snapshot package | package parse/diff. |
| 30 | `WebAdminSnapshotStore.savePackage` | snapshot package | package write bytes. |
| 31 | `WebAdminSnapshotService.diff` | resource diff | JSON preview allocation. |
| 32 | `WebAdminProtectedDraftRegistry.start` | protected draft registry | synchronized map growth. |
| 33 | `WebAdminProtectedDraftRegistry.validateForLogicChainSave` | save preflight | synchronized lookup/state check. |
| 34 | `WebAdminProtectedDraftRegistry.markCommitted` | terminal transition | terminal cleanup behavior. |
| 35 | `WebAdminProtectedDraftRegistry.expireOld` | periodic cleanup | full scan. |
| 36 | `WebAdminSelectionSessions.expireOld` | selection session | map scan. |
| 37 | `WebAdminContainerTemplateSessions.expireOld` | container template sessions | map scan. |
| 38 | `WebAdminSingleItemSubmitTemplateSessions.expireOld` | item submit sessions | map scan. |
| 39 | `WebAdminVirtualBlockDeviceContainerTemplateSessionService.status` | capture status | session lookup + device resolve. |
| 40 | `WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService.status` | capture status | session lookup + device resolve. |
| 41 | `WebAdminVirtualBlockDeviceNativeTriggerService.fingerprintFor` | VBD native trigger | JSON fingerprint allocation. |
| 42 | `WebAdminVirtualBlockDeviceContainerTemplateSessionService.fingerprintFor` | container conditions | JSON fingerprint allocation. |
| 43 | `WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService.fingerprintFor` | item requirements | JSON fingerprint allocation. |
| 44 | `WebAdminDeviceBasicConfigService.fingerprintFor` | device config | per request fingerprint allocation. |
| 45 | `WebAdminLogicChainEditorService.graphFor` | Logic Chain graph | graph construction allocation. |
| 46 | `LogicChainDraftSaveCoordinator.saveDraft` | Logic Chain save | validation/planning allocation. |
| 47 | `LogicChainDraftOperationPlanner.plan` | save plan | duplicate/conflict scans. |
| 48 | `LogicChainTypedWriteExecutor.execute` | typed writes | sequential write cost. |
| 49 | `WebAdminJsonResponse.GSON.toJson` response paths | WebAdmin payload | large response bytes. |
| 50 | `WebAdminServer.readJson` | WebAdmin write request | request parse allocation and validation. |

## Top 50 Allocation-Heavy Suspects

Allocation suspects are static until Phase 1 adds counters or timing proxies.

| # | Path | Allocation source |
| ---: | --- | --- |
| 1 | Logic Chain full render | HTML strings for full app view. |
| 2 | Logic Chain layout | maps, sets, edge indexes, positioned nodes. |
| 3 | Logic Chain draft overlay | cloned nodes/edges and filtered arrays. |
| 4 | VBD trigger overlay | cloned trigger cards and output edges. |
| 5 | Logic Chain diff banner | nested draft summaries and rows. |
| 6 | selected node panel | detail HTML per selection. |
| 7 | minimap | capped segment HTML. |
| 8 | icon hydration | DOM icon pass after render. |
| 9 | `SignalDeviceStore.getSnapshot` | sorted copy. |
| 10 | `SignalListenerStore.getSnapshot` | copied list. |
| 11 | `RegionControllerStore.getSnapshot` | copied controller list. |
| 12 | `MapDataStore.getSnapshot` | map snapshot copy. |
| 13 | condition group load | full JSON object graph. |
| 14 | state variable load | full JSON object graph. |
| 15 | runtime context build | snapshot maps and optional player/container data. |
| 16 | condition evaluator | result trees and message lists. |
| 17 | action chain execution | result list. |
| 18 | signal event history | event record allocation. |
| 19 | itemSubmit matched count | repeated slot scans and lists. |
| 20 | itemSubmit consume | matching stack list. |
| 21 | container total matcher | slot/item matching rows. |
| 22 | container fingerprint | serialized slot signature. |
| 23 | VBD tick snapshot | per-tick VBD list copy. |
| 24 | RegionController tick state | player/controller transition sets. |
| 25 | polygon contains | point list traversal. |
| 26 | timer due execution | due result list and action context. |
| 27 | active timer count | map iteration. |
| 28 | snapshot manifest load | manifest object graph. |
| 29 | snapshot package load | package object graph. |
| 30 | snapshot diff preview | before/after JSON snippets. |
| 31 | template import planning | package copy/fingerprint. |
| 32 | WebAdmin route list pages | table row HTML. |
| 33 | WebAdmin detail pages | identity/detail rows. |
| 34 | channel combobox | option HTML. |
| 35 | condition compatible list | filtered options. |
| 36 | timer action bucket UI | action row HTML. |
| 37 | region action bucket UI | action row HTML. |
| 38 | VBD native trigger fingerprint | JSON fingerprint model. |
| 39 | protected draft registry snapshot | record copies. |
| 40 | selection session status | status DTO map. |
| 41 | audit event payload | summary maps. |
| 42 | realtime event payload | JSON response maps. |
| 43 | write-before snapshot | resource package copy. |
| 44 | Logic Chain save validation | normalized key sets. |
| 45 | target lock preflight | requirement lists. |
| 46 | duplicate/conflict validation | action target maps. |
| 47 | channel metadata tail write | referenced channel sets. |
| 48 | WebAdmin generated bundle export | app.js string copy in guard. |
| 49 | Node VM synthetic smoke | script context/bootstrap. |
| 50 | docs/guard metric report | top table row strings. |

## Phase 0 Deferred Or Blocked Items

| Candidate | Status | Reason |
| --- | --- | --- |
| Hover class-only update | Deferred | Current hover affects node classes, edge classes, arrow ownership and `marker-end`; needs focused DOM equivalence. |
| Click/select panel-only update | Deferred | Selection affects graph, panel, save target fallback and VBD overlay source priority. |
| Zoom transform-only update | Deferred | Toolbar percentage, pan state and canvas transform need interaction guard. |
| Draft overlay cross-render memo | Deferred | No authoritative draft revision key yet. |
| VBD overlay pipeline consolidation | Deferred | Existing selected fallback/source priority is behavior-sensitive. |
| Condition group runtime cache | Accepted in Phase 4 for runtime gate/replay | Bounded path/content-fingerprint cache; save and snapshot rollback invalidate; blank gate still returns before store load. |
| State variable runtime snapshot cache | Accepted in Phase 4 through StateVariableService cached loads | Synchronous save invalidates; corrupt/status fallback and legacy raw missing-file creation are guarded. |
| Signal device channel index | Planned | Must preserve order, enabled filter, duplicate/missing handling and dirty flush semantics. |
| RegionController planner-region id index/cache | Accepted in Phase 2 | `MapDataStore.getPlannerRegion` now uses a planner-region id index with old first-match and missing-region behavior guarded. |
| Region bounds prefilter | Accepted in Phase 2 | `findPlannerRegionContaining` now prefilters by bounds before exact polygon contains while preserving polygon result. |
| Timer bucket/index | Planned | Must preserve `LinkedHashMap` order and due execution budget. |
| Snapshot manifest/package parsed cache | Deferred | Rollback package fingerprint, retention and degraded-message semantics need stronger package equivalence guards. |
| Protected draft expiry bucketing | Deferred | Terminal visibility and cleanup-required world-device drafts must remain server-cleanup-aware. |
| VBD native trigger duplicate gate validator | Accepted in Phase 5 | Duplicate load/normalize/validate/compatibility logic now delegates to `WebAdminConditionGateBindingValidator`; VBD dynamic container profile remains local and exact errors are tested. |
| Production JS routing helper rewrite | Deferred | Route/event order, modal outside-close timing and realtime refresh behavior need a route-key/event-order golden matrix. |
| Giant UI builder extraction | Deferred | HTML output, dirty state, focus/caret and scroll preservation need stronger DOM/modal guards before helper extraction. |
| Remaining BeforeVxx wrapper cleanup | Deferred | Patch-stack cleanup needs exact source/order markers and must preserve BeforeV18+ zero-growth ratchet. |
