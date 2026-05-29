# TZZ 9.1.2 Synthetic Fixtures

## Scope

9.1.2 must not depend on user-provided large real worlds or large real Logic Chains. Phase 1 will add deterministic synthetic fixtures with fixed seed `912012`.

Fixtures must be behavior-safe:

- they must not start Minecraft;
- they must not require MCP scenarios;
- they must not write persistent user world data;
- stress fixtures are report-only unless a deterministic safety marker fails;
- timing budgets start report-only; output shape, invariants and marker equivalence can hard fail.

## Shared Benchmark Row Schema

Every benchmark output row must contain:

| Field | Required | Notes |
| --- | --- | --- |
| `suite` | yes | `webadmin_graph`, `runtime`, `store`, `session`, `snapshot`. |
| `case` | yes | operation name, for example `logic_chain_hover_large`. |
| `tier` | yes | `small`, `medium`, `large`, `stress`. |
| `size_primary` | yes | nodes, devices, regions, timers or bytes. |
| `size_secondary` | optional | edges, players, conditions or sessions. |
| `measured_ms` | yes | local wall time. |
| `estimated_low_end_ms_x3` | yes | `measured_ms * 3`. |
| `estimated_low_end_ms_x5` | yes | `measured_ms * 5`. |
| `estimated_very_low_end_ms_x10` | yes | `measured_ms * 10`. |
| `scale_factor_from_previous_size` | yes except first tier | growth from previous tier. |
| `complexity_class_estimate` | yes | static or measured estimate. |
| `risk_level` | yes | `PASS`, `WARN`, `FAIL`. |
| `reason` | yes | Chinese explanation. |
| `hard_fail` | yes | true only for deterministic safety/shape violation. |
| `bytes` | when relevant | generated HTML/JS payload, JSON load/save bytes or snapshot package bytes. |
| `serialization_count` | when relevant | JSON parse/serialize or large response build count. |
| `write_frequency` | when relevant | per tick, per signal, per save, per manual request or batch. |
| `cleanup_complexity` | when relevant | session/cache cleanup class, for example `O(active)`. |
| `tick_path` | runtime rows | true if called from server/client tick. |
| `main_thread` | runtime rows | true if synchronous on Minecraft server thread or WebAdmin handler. |
| `indexed_by_channel_device_region` | runtime rows | current or optimized lookup key, or `none/list-scan`. |
| `bound_object_only` | VBD/world rows | true only when unloaded chunks avoid world/container reads. |
| `cap_or_cooldown` | runtime rows | active cap, due budget, cooldown, interval or cleanup bound. |

Example:

```text
suite=webadmin_graph
case=logic_chain_hover
tier=large
size_primary=500
size_secondary=1000
measured_ms=8.0
estimated_low_end_ms_x3=24.0
estimated_low_end_ms_x5=40.0
estimated_very_low_end_ms_x10=80.0
scale_factor_from_previous_size=3.8
complexity_class_estimate=near O(n*m)
risk_level=WARN
reason=高配可接受，但增长接近平方，低配 hover 可能卡顿。
hard_fail=false
```

## WebAdmin Logic Chain Fixtures

| Tier | Nodes | Edges | Required extras |
| --- | ---: | ---: | --- |
| small | 20 | 30 | one channel root, one listener, one action, one VBD. |
| medium | 100 | 200 | joins, timers, state variables, gates, VBD trigger overlays. |
| large | 500 | 1000 | multiple components, pending-delete, unsaved expanded diff. |
| stress | 2000 | 5000 | report-only growth, no hard time budget. |

Generator rules:

- seed is fixed at `912012`;
- ids are stable and readable;
- channel distribution includes hubs and long chains;
- graph includes disabled, missing and warning markers;
- draft overlay includes new node, existing edit, action delete, action reorder and node delete;
- VBD trigger overlay includes right-click, itemSubmit and container-change trigger outputs;
- minimap segment count exceeds 24 to guard the cap;
- generated graph must be serializable as the same JSON-like shape consumed by current WebAdmin synthetic guard.

Required WebAdmin operations:

| Operation | Fixture state |
| --- | --- |
| initial render | clean readonly graph. |
| edit mode render | editor state with lock metadata. |
| hover highlight | stable hover node id. |
| click selection | selected node id across producer, channel, VBD, RegionController and action. |
| zoom / pan | fixed zoom percentages and pan offsets. |
| drag preview | legal and illegal slots. |
| draft overlay | new SignalListener, Timer, Join, World Device Reference, VBD, RegionController. |
| VBD trigger overlay | stable `(sourceNodeId, triggerKey)` target. |
| unsaved diff expanded | nested pending delete and action reorder. |
| minimap | more than 24 segments. |

## Runtime Fixtures

Runtime fixtures are service-level data. They must not create or load a real Minecraft world.

| Fixture | small | medium | large | stress |
| --- | ---: | ---: | ---: | ---: |
| signal devices | 100 | 1000 | 5000 | 10000 report-only |
| VBD configs | 100 | 500 | 1000 | 2000 report-only |
| condition groups | 100 | 500 | 1000 | 2000 report-only |
| state variables | 1000 | 5000 | 10000 | 25000 report-only |
| listeners | 100 | 500 | 1000 | 2500 report-only |
| action chains | 100 | 500 | 1000 | 2500 report-only |
| timers | 100 | 500 | 1000 | 2048 hard cap target |
| region controllers | 100 | 500 | 1000 | 2000 report-only |
| players synthetic positions | 10 | 100 | 500 | 1000 report-only |

Runtime benchmark cases:

- SignalBridge emit with no listeners, one channel, many unrelated devices and many related listeners;
- ActionEngine chain with no gate, true gate, false gate and missing/invalid gate;
- SignalBridge accepted-signal path including SignalReceiver dispatch, ActionRelay dispatch, listener actions, accepted history and SignalJoin observer order;
- SignalJoin accepted-signal load/scan path with no joins, unrelated joins, matching joins and recursive output guard;
- ActionEngine message broadcast without context player and `notifyOps` operator scan;
- ConditionGate blank group skip, valid group, incompatible group and missing group;
- configured condition gates with runtime history recording enabled;
- VBD tick scan with all chunks unloaded and with bound chunks loaded by synthetic stub where possible;
- VBD unloaded-chunk fixture must prove no block state, container inventory or fingerprint read happens after `isChunkLoaded == false`;
- itemSubmit requirement matching with consume disabled/enabled, multi-requirement scale, late requirement failure, consume-plan failure and proof that staged consume is not applied unless the whole evaluation succeeds;
- container matcher/fingerprint cases across slots x conditions, `TOTAL_ITEM` reuse, `TOTAL_MATCHER` repeated matcher cost, unchanged fingerprint string semantics, cooldown and interval cases;
- protected draft/capture writeback for itemSubmit/container/native-trigger draft-only flows, fail-closed context mismatch, cancelled/failed retry and no formal VBD write until final Logic Chain save;
- RegionController player x controller checks with bounds-compatible and bounds-miss distributions;
- RegionController player x controller x plannerRegions cases, including controller-region id lookup/cache candidates and bounds miss distribution;
- Timer start/cancel/tick with due and not-due instances; due-order and due-budget benchmarks must use `TimerRuntimeService.TestRuntime.tickActual`, not the simplified `tick` helper;
- session expiry with active, expired and terminal entries.

## Store Fixtures

Use temporary directories only.

Low-end store benchmarks must account for ordinary VPS SSD variance, Windows Defender / IO jitter, large JSON growth, full-file rewrite amplification, serialization count, GC pressure and allocation pressure.

| Store file | small | medium | large | stress | Safety expectation |
| --- | ---: | ---: | ---: | ---: | --- |
| `signal_devices.json` | 100 | 1000 | 5000 | 10000 report-only | load/save round trip; duplicate/missing handling unchanged. |
| `state_variables.json` | 1000 | 5000 | 10000 | 25000 report-only | corrupt JSON fallback remains empty/safe. |
| `condition_groups.json` | 100 | 500 | 1000 | 2000 report-only | validation status and Chinese error messages unchanged. |
| `region_controllers.json` | 100 | 500 | 1000 | 2000 report-only | trigger bucket and action order preserved. |
| `timers.json` | 100 | 500 | 1000 | 2048 active cap target | timer fingerprint and action order preserved. |
| snapshot manifest | 100 entries | 1000 entries | 5000 entries | 10000 report-only | rollback timeline semantics unchanged. |
| snapshot package | 1MB target | 5MB target | 20MB target | 50MB report-only | diff preview and package hash unchanged. |
| session/registry maps | 100 active | 1000 active | 5000 active | 10000 report-only | active/expired/terminal cleanup semantics unchanged. |

Store benchmark cases:

- cold load;
- repeated load;
- single small mutation followed by save;
- batch mutation followed by save;
- corrupt JSON fallback;
- fingerprint computation;
- snapshot manifest load/save;
- package diff load.
- session/registry cleanup with active, expired, terminal and cleanup-required entries; report `cleanup_complexity`.

## Proposed Test/Guard Classes

| Class | Responsibility |
| --- | --- |
| `SyntheticFixtureFactory` | deterministic builders for graph, runtime and store fixtures. |
| `WebAdminPerformanceBaselineGuardTest` | extend current synthetic graph/DOM guard and add low-end benchmark row output. |
| `RuntimePerformanceBaselineGuardTest` | service-level runtime timing and semantic hard guards. |
| `StorePerformanceBaselineGuardTest` | temp-dir JSON load/save timing and format hard guards. |
| `CodeQualityGuardTest` | aggregate the new report-only benchmark suites. |

Timing must remain warning/report-only in early 9.1.2. The following should hard fail:

- generated fixture has wrong shape or missing required coverage;
- DOM equivalence hash changes without explicit accepted baseline update;
- `SignalBridgeServer.emit` ordering changes: SignalReceiver dispatch -> ActionRelay dispatch -> listener actions -> accepted history / SignalJoin observer;
- `ActionEngine.executeAll` order or stop-on-first-failure changes;
- blank condition gate loads a group/context;
- itemSubmit consume is not all-or-nothing;
- RegionController transition order changes;
- timer due execution order or due budget changes;
- store corrupt JSON fallback changes;
- session terminal state lifecycle changes.
