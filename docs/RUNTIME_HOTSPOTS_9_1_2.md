# TZZ 9.1.2 Runtime Hotspots

## Scope

This document records Phase 0 runtime, store and tick-path hotspots. It does not authorize behavior changes. Phase 1 must add deterministic fixtures and report-only timings before Phase 2/4 optimize these paths.

Runtime invariants:

- a blank `conditionGroupId` must keep legacy behavior and must not load condition groups or build an evaluation context;
- configured gates remain an outer gate only;
- gate false must not emit signals, consume items, execute actions, move items or write state;
- signal emit order, action execution order and stop-on-failure behavior must not change;
- VBD paths must not force-load chunks and must only inspect bound objects;
- itemSubmit consume remains all-or-nothing;
- RegionController enter/exit/stay ordering must not change;
- timer due ordering and per-tick due budget must not change.

## Tick Callback Inventory

| Callback | Current entry | Phase 0 risk |
| --- | --- | --- |
| server tick | `TzzLifecycleBootstrap` registers `VirtualBlockDeviceDispatcher.tick(server)` | scans all VBD configs; no force-load is preserved. |
| server tick | `TzzLifecycleBootstrap` registers `VirtualBlockDeviceContainerHandler.tick(server)` | `tickOpenClose` scans open/pending-open sessions; `tickContentChanges` scans VBD configs and fingerprints loaded containers. |
| server tick | `TzzLifecycleBootstrap` calls `MapDataStore.flushDirty`, `TaskDataStore.flushDirty`, `NoteDataStore.flushDirty`, `RegionControllerStore.flushDirty`, `SignalListenerStore.flushDirty`, `SignalDeviceStore.flushDirty` | dirty-gated but possible main-thread JSON IO; low-end VPS SSD and Windows Defender jitter must be measured. |
| server tick | `TzzLifecycleBootstrap` expires WebAdmin sessions and protected drafts | full registry scans are invoked every tick; internal timestamp/state guards must be benchmarked rather than assumed. |
| server tick | `RegionControllerServer` calls `RegionControllerTracker.tick(server, tickCounter)` | players x enabled controllers x region lookup. |
| server tick | `TimerServer` calls `TimerRuntimeService.tick(server)` | bounded active timers but scans active store each tick. |
| server tick | `MapServer.tickPlayerRegions(server)` | map player region detection scans planner regions. |
| server tick | `DeathSyncServer` | low relevance to 9.1.2 Logic Chain runtime, keep out unless benchmarks reveal issue. |
| block entity tick | `SignalReceiverBlockEntity.tickServer` | world block tick path; keep order and loaded block semantics. |
| block entity tick | `ActionRelayBlockEntity.tickServer` | cooldown/action relay path. |
| block entity tick | `SilentSensorPlateBlockEntity.tickServer` | older world device path. |
| client tick | `Tzz_modClient` calls `CameraModeClient.tick` and `WebAdminSelectionClient.tick` | not server TPS path; only inspect if selection sessions regress. |

## Top 50 Runtime Hotspot Candidates

Phase 1 runtime benchmark output must include these low-end classification fields on every row: `tick_path`, `main_thread`, `io_on_hot_path`, `indexed_by_channel_device_region`, `bound_object_only`, `cap_or_cooldown`, `measured_ms`, x3/x5/x10 estimates, scale factor, complexity estimate, `risk_level` and Chinese `reason`.

The static table below keeps Phase 0 compact. Rows marked server tick, per signal or per action must be expanded into the full machine-readable row schema in Phase 1.

| # | Method/path | Frequency | Complexity estimate | IO on hot path? | Risk | Guard needed |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | `SignalBridgeServer.emit` | per signal | channel fan-out, currently index status must be proven | no expected | WARN | listener/action order. |
| 2 | `SignalListenerStore.getSnapshot` from emit paths | per signal or WebAdmin | list copy/filter | possible load path must be checked | WARN | enabled/channel ordering. |
| 3 | `SignalDeviceStore.getEnabledReceiversForChannel` | per signal | snapshot + filter | possible store flush/load side effects | WARN | receiver order. |
| 4 | `SignalDeviceStore.getEnabledActionRelaysForChannel` | per signal | snapshot + filter | possible store flush/load side effects | WARN | relay order. |
| 5 | `ActionEngine.execute` | per action | dispatch + optional gate | no expected | WARN | type result messages. |
| 6 | `ActionEngine.executeAll` | action chains | `O(actions)` | no expected | WARN | stop-on-failure. |
| 7 | `ActionEngine.executeMessage` broadcast branch | message action without context player | all online players scan | no expected | WARN | broadcast behavior. |
| 8 | `ActionAuditLogger.notifyOperators` | action with `notifyOps` and context player | all online players scan for ops | no expected | WARN | operator notification behavior. |
| 9 | `ActionEngine.executeSignal` branch | signal action | signal emit nested | no expected | WARN | downstream emit result. |
| 10 | `ConditionGateService.evaluate` | configured gate | group lookup + context | current load suspicion | FAIL risk | blank-gate skip, fail-closed. |
| 11 | `ConditionGateService.loadGroup` | configured gate | load all groups then find | yes suspicion | FAIL risk | group status and Chinese errors. |
| 12 | `ConditionGateService.validateCompatibility` | configured gate | group profile scan | no expected | WARN | incompatible rejected. |
| 13 | `ConditionRuntimeContextBuilder.baseBuilder` | gate context | state snapshot | yes suspicion | FAIL risk | context fields. |
| 14 | `ConditionRuntimeContextBuilder.signalEventBuilder` / `signalListener` | signal gate | base + signal data | yes suspicion | FAIL risk | signal context identity. |
| 15 | `ConditionRuntimeContextBuilder.regionController` | region gate | base + region data | yes suspicion | FAIL risk | trigger bucket. |
| 16 | `ConditionRuntimeContextBuilder.withActionMetadata` | action gate | action metadata copy | yes suspicion | FAIL risk | action gate context. |
| 17 | `StateVariableStore.getSnapshot` | gate/state action | full snapshot | yes suspicion | FAIL risk | mutation invalidation. |
| 18 | `StateVariableSnapshot` constructor | store load/mutation | normalize + sort allocation | no | WARN | corrupt record drop behavior. |
| 19 | `StateVariableSnapshot.get` | condition/state lookup | linear stream lookup | no | WARN | stable id lookup result. |
| 20 | `ConditionEvaluator.evaluate` | per condition group | tree traversal | no expected | WARN | result shape. |
| 21 | `ConditionEvaluator.evaluateNode` | condition node | recursive branch | no expected | WARN | short-circuit semantics. |
| 22 | `ConditionEvaluator.evaluateGroup` | group node | child traversal | no expected | WARN | AND/OR behavior. |
| 23 | `ConditionRegistry.evaluate` | condition node | predicate dispatch | no expected | WARN | node type compatibility. |
| 24 | `VirtualBlockDeviceDispatcher.tick` | server tick | scan all VBD configs | no force-load, no JSON expected | WARN/FAIL | only loaded chunk and bound position. |
| 25 | `VirtualBlockDeviceDispatcher.handleRedstone` | server tick branch | VBD trigger checks | no expected | WARN | rising/falling edge behavior. |
| 26 | `VirtualBlockDeviceDispatcher.handleBlockState` | server tick branch | block state compare | no expected | WARN | last-state behavior. |
| 27 | `VirtualBlockDeviceContainerHandler.tick` | server tick | open/close session scan plus content-change scan | no force-load | WARN | open/close/change semantics. |
| 28 | `VirtualBlockDeviceContainerHandler.tickOpenClose` | server tick | scans `OPEN_SESSIONS` and `PENDING_OPENS` | no expected | WARN | open/close event behavior. |
| 29 | `VirtualBlockDeviceContainerHandler.tickContentChanges` | server tick | VBD x container fingerprint | no expected after loaded-chunk check | WARN/FAIL | fingerprint unchanged. |
| 30 | `VirtualBlockDeviceContainerHandler.findVirtualBlockDevice` | container event | list lookup | no expected | WARN | exact id/source handling. |
| 31 | `VirtualBlockDeviceInteractionHandler.handleUseBlock` | player interaction | VBD lookup + matcher + emit | no expected | WARN | consume/order. |
| 32 | `ItemSubmitEvaluator.evaluate` | right-click/itemSubmit | requirements x inventory plus consume pass | no expected | WARN | all-or-nothing consume. |
| 33 | `ItemSubmitEvaluator.matchedCount` | per requirement | inventory scan | no expected | WARN | matcher count. |
| 34 | `ItemSubmitEvaluator.matchingConsumableStacks` | consume enabled | inventory scan | no expected | WARN | consume stack order. |
| 35 | `ContainerItemConditionSupport.matches` | container conditions | condition x slots | no expected | WARN | item predicate equivalence. |
| 36 | `ContainerItemConditionSupport.TOTAL_ITEM` | container condition | total item count | no expected | WARN | exact count. |
| 37 | `ContainerItemConditionSupport.TOTAL_MATCHER` | container condition | slot scan per condition | no expected | WARN | matcher count. |
| 38 | `SignalDeviceStore.resolveDevice` | runtime/WebAdmin | linear id/source lookup | no expected | WARN | duplicate/missing result. |
| 39 | `SignalDeviceStore.findById` | lookup | `O(devices)` | no expected | WARN | id normalization. |
| 40 | `SignalDeviceStore.findBySourcePosition` | world lookup | `O(devices)` | no expected | WARN | dimension/pos identity. |
| 41 | `SignalDeviceStore.getVirtualBlockDevicesSnapshot` | VBD tick | copy/filter VBDs | possible store flush/load | WARN | enabled/config filter. |
| 42 | `RegionControllerTracker.tick` | server tick | players x controllers, inline lookup/action logic | no expected | FAIL risk | enter/exit/stay transitions. |
| 43 | `MapDataStore.getPlannerRegion` | region tick | synchronized linear lookup by id | no JSON expected | WARN/FAIL | missing region behavior. |
| 44 | `MapDataStore.findPlannerRegionContaining` | map tick | synchronized all-region scan | no JSON expected | WARN/FAIL | selection result. |
| 45 | `RegionGeometry.containsBlock` | region hit test | polygon point test | no | WARN | boundary semantics. |
| 46 | `TimerRuntimeService.tick` | server tick | active stores scan | no expected | WARN | due budget. |
| 47 | `TimerRuntimeService.RuntimeStore.tick` | server tick | active instances scan | no expected | WARN | `LinkedHashMap` order. |
| 48 | `TimerRuntimeService.RuntimeStore.activeCount` | timer start | sums maps | no | WARN | max active limit. |
| 49 | `SignalJoinRuntimeService.observeAcceptedSignal` | accepted signal path | loads/scans SignalJoin definitions then may emit output | yes, store load | WARN/FAIL | accepted-history and join output order. |
| 50 | `WebAdminProtectedDraftRegistry.expireOld` and WebAdmin session expiry | server tick | full map scans | no | WARN | terminal cleanup. |

## Low-End Runtime Budgets

| Path type | Local target | x5 low-end estimate target | Notes |
| --- | ---: | ---: | --- |
| server tick hot path, common case | under 2ms per subsystem | under 10ms | Multiple subsystems share a 50ms Minecraft tick budget. |
| signal emit with 1000 listeners/devices | report-only in Phase 1 | under 10ms preferred | Growth curve more important than single absolute value. |
| condition gate evaluation | report-only in Phase 1 | under 5ms common case | Blank gate must avoid all store/context work. |
| VBD 1000 tick scan | report-only in Phase 1 | under 10ms preferred | If scan is linear and bounded, WARN; if nested matching grows, FAIL. |
| RegionController 100 players x 1000 controllers | report-only in Phase 1 | must not approach full tick budget | Bounds/index likely needed if benchmark confirms. |
| timer 1000 active instances | report-only in Phase 1 | under 5ms preferred | Must preserve due ordering. |
| store load/save large JSON | report-only in Phase 1 | manual path can be slower, tick path cannot do IO | Large writes can WARN but must not be hidden. |

Runtime report rows must also explicitly answer:

| Field | Required answer |
| --- | --- |
| `tick_path` | true for VBD, RegionController, Timer, dirty flush and session expiry rows. |
| `main_thread` | true for Minecraft server tick, signal/action execution and WebAdmin synchronous write handlers. |
| `indexed_by_channel_device_region` | name current index or `none/list-scan`; after optimization, name the index key. |
| `bound_object_only` | true for VBD/world-device rows only when unloaded chunks avoid block/container/fingerprint reads. |
| `cap_or_cooldown` | timer due budget, active timer cap, VBD cooldown/interval, listener cooldown or session expiry cap. |

## Optimization Candidates By Safety

| Candidate | Safety class | Implementation precondition |
| --- | --- | --- |
| channel to listener/device index | listener index accepted in Phase 2; device index still deferred | listener ordering guard added; device receiver/relay sort/refresh guard still required. |
| VBD id/source maps | safe if dirty invalidation is centralized | lookup equivalence guard. |
| condition group runtime cache | safe only by store path + fingerprint + invalidation | blank-gate no-load guard. |
| state variable snapshot lookup | accepted in Phase 2 as binary search over sorted snapshot records | stable-id lookup equivalence guard. |
| state variable store cache | safe only by path + invalidation on mutation | state mutation tests. |
| SignalJoin accepted-signal cache/channel index | accepted in Phase 2 with bounded content-fingerprint cache and enabled input-channel index | cache invalidation, corrupt fail-closed and join order guards. |
| MapDataStore bounds prefilter | accepted for `findPlannerRegionContaining`, before exact polygon contains | geometry equivalence guard. |
| RegionController planner-region id index/cache | accepted in `MapDataStore.getPlannerRegion` with first-match semantics | player x controller x plannerRegions fixture and id-index equivalence guard. |
| timer due bucket/index | safety-sensitive | due ordering guard before code change. |
| itemSubmit requirement precompilation | safe if matcher output unchanged | consume/all-or-nothing tests. |
| container matcher totals reuse | safe if snapshot identity fixed per evaluation | container condition tests. |
| protected draft expiry batch cleanup | safe if terminal states unchanged | registry lifecycle tests. |
