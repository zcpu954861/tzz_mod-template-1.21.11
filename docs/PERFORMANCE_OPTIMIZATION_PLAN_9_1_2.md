# TZZ 9.1.2 Performance Optimization Plan

## Scope

This plan sequences 9.1.2 performance work after the Phase 0 audit. It is not an implementation record. Each phase must use the subagent rounds and validation commands required by the 9.1.2 prompt.

## Final Release Status

The completed release artifact is `v1.68.3-real-performance-deep-simplification` at `1188c6601d7071e31aff3bfbc2355e7470bebafe` with tag object `e793aa30e720991d024fafaf4beef99dd54f2993`.

The final guard repair commit is `1188c66 test: align 9.1.2 code quality guard baselines`. It updates guard byte counting to CRLF -> LF normalized UTF-8 measurement and preserves no-growth hard-fail behavior.

Final validation passed for the Gradle guard set and `clean build`. Timing rows remain warning/report-only; deterministic source, DOM, store, cache and no-growth invariants remain hard guarded.

Global rules:

- optimize only after benchmark/guard coverage exists;
- prefer low-risk data structures, cached immutable snapshots and repeated-computation removal;
- preserve runtime/UI/API/save semantics;
- stop on validation failure or behavior-equivalence gap;
- record high-risk candidates as deferred instead of forcing them into 9.1.2.

## Phase Sequence

| Phase | Goal | Main outputs | Validation |
| --- | --- | --- | --- |
| Phase 0 | audit and plan | six docs plus checkpoint | `git diff --check` for docs-only. |
| Phase 1 | synthetic fixture and benchmark harness | `SyntheticFixtureFactory`, runtime/store/webadmin benchmark guards | Gradle guard set from prompt. |
| Phase 2 | runtime optimization | proven lookup/index/cache changes for listener, join, state snapshot and map planner-region paths | Gradle guard set and low-end report. |
| Phase 3 | WebAdmin large graph optimization | only DOM-proven render/interaction improvements | DOM equivalence and Gradle guard set. |
| Phase 4 | store/session/registry optimization | cache/dirty/index/expiry improvements | store benchmarks and Gradle guard set. |
| Phase 5 | deep simplification | process automatically proven complexity hotspots | code-quality guard and docs update. |
| Phase 6 | guard ratchet and Obsidian | hardened benchmark reports and knowledge-base update | Gradle guard set. |
| Phase 7 | final validation, merge and tag | release if all prior phases pass | full feature/master validation. |

## Optimization Backlog

| Priority | Candidate | Expected win | Safety condition | Phase |
| ---: | --- | --- | --- | --- |
| 1 | add benchmark row schema and low-end reporting | prevents high-end-only conclusions | report-only timing, hard shape guards | 1 |
| 2 | deterministic Logic Chain large graph generator | proves render growth curve | no WebAdmin behavior change | 1 |
| 3 | deterministic runtime fixture generator | proves tick/store hotspots | no Minecraft startup | 1 |
| 4 | deterministic store fixture generator | proves JSON load/save scale | temp dirs only | 1 |
| 5 | blank condition gate no-load guard | protects legacy behavior | hard fail if group/context loaded | 1 |
| 6 | SignalDeviceStore id/source/channel indexes | reduce O(n) lookups | order/duplicate/missing guards | deferred |
| 7 | SignalListener channel index | reduce emit filtering | listener order guard | 2 |
| 8 | condition group cache by path/fingerprint | avoid repeated JSON read | invalidation and status guard | accepted in Phase 4 for runtime gate/replay only |
| 9 | state variable store cache | avoid gate-time JSON read | mutation invalidation guard | accepted in Phase 4 with synchronous save invalidation |
| 10 | VBD tick pre-index by enabled VBD and trigger type | reduce per tick scans | no force-load, bound-object only | deferred |
| 11 | VBD container scan narrow by trigger/cooldown | reduce container fingerprint work | fingerprint semantics | deferred |
| 12 | itemSubmit matcher precompile per requirement set | reduce repeated parsing/scans | consume all-or-nothing | deferred |
| 13 | container matcher total reuse per snapshot | reduce condition x slot scans | matcher equivalence | deferred |
| 14 | RegionController bounding box prefilter | avoid polygon contains for misses | exact polygon still authoritative | 2 |
| 15 | RegionController planner-region id index/cache | avoid repeated synchronized linear region lookup | missing-region behavior and controller order unchanged | 2 |
| 16 | RegionController group by dimension | avoid cross-dimension checks | transition order unchanged | deferred |
| 17 | Timer due bucket or next-due shortcut | reduce every-tick full scan | preserve `LinkedHashMap` due order and `MAX_DUE_EXECUTIONS_PER_TICK` | deferred |
| 18 | Timer active count counter | reduce start storms | max active limit guard | deferred |
| 19 | Logic Chain hover local class update | remove full render on hover | DOM equivalence including arrows | 3 if proven |
| 20 | Logic Chain select panel/class split | reduce full render on click | panel and graph class equivalence | 3 if proven |
| 21 | Logic Chain zoom transform-only update | remove full render on zoom | toolbar/pan state guard | 3 if proven |
| 22 | drag pointermove rAF coalescing | reduce preview churn | legal slot result unchanged | 3 if proven |
| 23 | draft diff memo by draft fingerprint | avoid repeated diff rows | reliable revision key | 3 if proven |
| 24 | VBD overlay memo/pipeline cleanup | reduce clone/filter | selected fallback/source behavior frozen | 3 if proven |
| 25 | WebAdmin list/detail large table memo | reduce silent refresh churn | filter/pagination/dirty state preserved | 3/4 |
| 26 | store dirty/coalesced writes | reduce repeated full JSON writes | save timing semantics preserved | 4 |
| 27 | session expiry bucket/checkpoint | reduce full scans | terminal cleanup unchanged | 4 |
| 28 | snapshot manifest/package streaming or lazy diff | reduce large diff allocation | rollback package semantics unchanged | 4 |
| 29 | WebAdmin protected draft registry split | simplify synchronized hotspots | lifecycle tests cover all states | 4/5 |
| 30 | remaining high-if routing helpers | lower event complexity | route golden matrix | 5 |
| 31 | remaining large UI builders | lower maintenance risk | DOM hash baseline | 5 |

## Required Guard Growth

| Guard area | Current state | 9.1.2 required addition |
| --- | --- | --- |
| WebAdmin graph DOM | 9.1.1 baseline exists | tiers small/medium/large/stress and interaction rows. |
| Runtime performance | `RuntimePerformanceBaselineGuardTest` reports service-level rows and `RuntimeOptimizationEquivalenceGuardTest` hard-checks Phase 2 lookup/index equivalence | keep timing report-only; add semantic hard guards before each new runtime optimization. |
| Store performance | `StorePerformanceBaselineGuardTest` reports temp-dir JSON/store/session rows | keep corrupt/missing fallback hard guards and store timing report-only. |
| Low-end reporting | not required in 9.1.1 | x3/x5/x10 estimates on every row. |
| Complexity curve | code-quality tables exist | benchmark scale factor and complexity estimate. |
| Semantic invariants | scattered service tests | focused hard guards around optimized paths. |
| Runtime ordering | scattered service behavior | hard guards for SignalBridge dispatch/history/join order and ActionEngine `executeAll` stop-on-failure. |
| Store IO reporting | not standardized | bytes, serialization count, write frequency and cleanup complexity. |

## Phase 2 Runtime Optimization Record

Phase 2 accepted only low-risk lookup/index changes that can be proven by deterministic guards:

- `StateVariableSnapshot.get` now uses binary search over the existing sorted `records()` list. Snapshot normalization, duplicate last-write-wins behavior and `records()` ordering are unchanged.
- `SignalListenerStore` now keeps a lazily rebuilt enabled-listeners-by-channel index inside the per-server store state. The index preserves the old listener list order and is invalidated by existing mutation paths.
- `SignalJoinRuntimeService` now uses `SignalJoinStore.loadWithStatusCached` and `SignalJoinFile.enabledJoinsReferencing(channel)` on the accepted-signal observer path. The cache is bounded, keyed by path plus content fingerprint, invalidated on save/server clear/snapshot rollback, and still reports raw file hash IO cost.
- `MapDataStore.getPlannerRegion` now uses a planner-region id index with old first-match semantics. `findPlannerRegionContaining` adds a bounds prefilter before the exact polygon `containsBlock` check.

Phase 2 deliberately deferred Timer structure rewrites, VBD runtime snapshot narrowing, itemSubmit scan rewrites, container `TOTAL_MATCHER` memoization and `SignalDeviceStore` receiver/relay indexes because they need stronger order/fingerprint/no-force-load guards before production changes.

## Phase 3 WebAdmin / Logic Chain Optimization Record

Phase 3 accepted only full-render-path improvements whose visible output is protected by deterministic DOM equivalence:

- `WebAdminLogicChainDomEquivalenceGuardTest` is now wired into `CodeQualityGuardTest`. It hard-checks canonical full-render equivalence for hover, selection and zoom paths, plus structured snapshots for node geometry/class state, edge identity/path/`marker-end`, right detail panel, diff banner, minimap cap and VBD trigger overlay source/triggerKey.
- `logicChainEdgePath` now emits nonvisual `data-logic-chain-edge-from`, `data-logic-chain-edge-to`, `data-logic-chain-edge-type` and VBD trigger identity attributes so guards can prove edge and VBD overlay identity without changing CSS, event handling or visible DOM semantics.
- `logicChainEdgeIndexes` now builds traversal indexes in the same render-local pass as `byFrom` / `byTo`, so view-mode traversal no longer rebuilds forward/reverse maps from `graph.edges`.
- `renderLogicChainViewer` builds a render-local `detailEdgeIndexes` for `detailGraph`; `logicChainSelectedNodePanel` reuses it for incoming/outgoing rows while preserving edge order and right-panel HTML baselines.

Phase 3 deliberately still defers hover class-only updates, selection panel-only updates, zoom transform-only updates, draft diff cross-render memoization and VBD overlay pipeline rewrites. Those require broader browser-like DOM mutation coverage, modal caret/scroll guards or reliable draft revision/fingerprint keys before they can replace the current full-render behavior.

## Phase 4 Store / Session / Registry Optimization Record

Phase 4 accepted conservative opt-in store/session changes only:

- `JsonLoadCacheSupport` provides a shared path-normalized content fingerprint for cached JSON load paths. It reads bytes and hashes content on each cache check, so external replacement, same-size edits and repaired corrupt files refresh instead of returning stale data.
- `StateVariableStore` now has bounded cached raw/status load paths. `StateVariableService` uses them while preserving the legacy raw missing-file creation behavior, and `saveSnapshot` invalidates both caches only after successful synchronous write.
- `WebAdminConditionGroupStore` now has bounded cached load paths. Runtime `ConditionGateService` and read-only `ConditionGateReplayService` use the cached path; WebAdmin write validators and condition group editing services deliberately keep uncached authoritative reads.
- `WebAdminSnapshotService.clearRestoredCaches` now clears StateVariable and ConditionGroup caches after rollback, matching the existing SignalDevice, SignalListener, SignalJoin and RegionController cache cleanup.
- `WebAdminContainerTemplateSessions` and `WebAdminSingleItemSubmitTemplateSessions` now track the earliest active expiry and skip active-session scans before that point. Selection sessions and protected draft registry cleanup remain unchanged because world-device protected draft rollback requires server-aware cleanup checks.
- `StorePerformanceBaselineGuardTest` now hard-checks missing/corrupt/repair/save/external-replacement/cache-bound behavior for StateVariable and ConditionGroup caches, rollback clear markers and session next-expiry source markers. It also emits cached-load benchmark rows with `serialization_count=0` while keeping timing report-only.

Phase 4 deliberately does not change JSON file format, pretty-write output, synchronous save timing, edit lock/draft authority, protected draft terminal states, selection world-device cleanup, snapshot manifest/package semantics or WebAdmin backend write validation authority.

## Phase 5 Deep Simplification Record

Phase 5 accepted one low-risk backend helper simplification after read-only subagent review:

- `WebAdminVirtualBlockDeviceNativeTriggerService.validateGateBinding(...)` now delegates duplicate condition group binding validation to `WebAdminConditionGateBindingValidator`.
- The VBD service still owns VBD-specific target/profile selection through `gateProfile(...)`, including dynamic container open/close inventory snapshot compatibility.
- The local `validateGateBinding` wrapper remains as the backend marker and call boundary for VBD native trigger configuration.
- `WebAdminConditionGateConfigTest` now characterizes VBD gate error field, exact code, rejected value summary, Chinese message fragments, blank optional group behavior, degraded store behavior and container profile compatibility.
- `StabilizationGuardTest` now checks that VBD native trigger validation delegates to the shared validator and that compatibility analysis remains inside the shared validator.

Phase 5 deliberately deferred production JS routing rewrites, giant UI builder extraction, BeforeVxx wrapper cleanup, protected draft registry splitting, edit-lock mapping rewrites, timer due structure changes and VBD runtime scan narrowing. Those areas remain behavior-sensitive and need route/DOM/state/runtime golden guards before simplification.

## Phase 6 Guard Ratchet / Knowledge Base Record

Phase 6 does not add production optimization. It ratchets the accepted 9.1.2 work into guard and documentation boundaries:

- runtime benchmark reports remain wired through `RuntimePerformanceBaselineGuardTest`;
- synthetic graph and generated bundle reports remain wired through `WebAdminPerformanceBaselineGuardTest`, `WebAdminLogicChainDomEquivalenceGuardTest` and `WebAdminFrontendBundleGuardTest`;
- store/cache/session benchmark reports remain wired through `StorePerformanceBaselineGuardTest`;
- `CodeQualityGuardTest` continues hard-failing BeforeVxx growth, new BeforeV18+ patch stacking, generated app.js/app.css ratchet changes, inline handler growth, `.closest(` / `querySelector(` growth and known giant JS function growth;
- the Phase 5 accepted VBD native trigger simplification is now guarded by a file line/byte no-regrowth baseline for `WebAdminVirtualBlockDeviceNativeTriggerService.java`;
- Obsidian remains the external precision repository index; quick entries should be categorized by task rather than listed as a flat note dump.

## Deferred High-Risk Optimizations

| Candidate | Defer reason | Required future proof |
| --- | --- | --- |
| replacing full Logic Chain render with class-only hover immediately | hover affects arrow owner and `marker-end` | focused DOM equivalence plus low-end benchmark. |
| replacing selection full render with right-panel-only update immediately | selection affects graph classes and VBD overlay source fallback | panel, graph and save target guard. |
| broad VBD overlay pipeline rewrite | source priority and capture writeback are behavior-sensitive | golden graph overlay cases. |
| changing timer due data structure without order guard | due execution order is user-visible runtime behavior | deterministic due-order tests. |
| region spatial index that reorders transitions | enter/exit/stay order may change | player-controller transition matrix. |
| write-behind store save across existing save boundary | save timing and crash recovery semantics may change | explicit transactional design. |
| replacing JSON file format | out of 9.1.2 scope | separate migration phase. |
| browser-only performance marker as hard budget | machine/browser noise | keep timing report-only, hard fail deterministic invariants. |
| snapshot manifest/package parsed-object cache | rollback fingerprint, retention and degraded package semantics are behavior-sensitive | package/manifest equivalence guards before caching. |
| protected draft registry expiry bucket/cap | terminal visibility and world-device cleanup-required entries are stateful | protected-draft state matrix and server cleanup guard. |
| production JS routing helper rewrite | delegated event order, modal outside-close timing and realtime route refresh are user-visible | route-key golden matrix and browser-like event-order guard. |
| large UI builder extraction | output HTML, dirty state, focus and scroll preservation are behavior-sensitive | DOM hash and modal caret/scroll guard. |
| remaining BeforeVxx wrapper cleanup | patch-stack cleanup can erase compatibility boundaries | exact source/order markers and no new BeforeV18+ growth. |
| VBD native trigger backend gate validator duplication | accepted in Phase 5 | shared-validator delegation plus exact field/code/message/degraded-store tests. |

## Phase Checkpoint Policy

Checkpoint commits are allowed only after the phase validation command set passes. Explicit staging only; never `git add .` or `git add -A`. Do not stage `.codex/`, `logs/`, `reports/mcp/`, screenshots, node_modules, build output, run output or `.gradle/`.

Suggested Phase 0 checkpoint:

```text
docs: add 9.1.2 performance audit plan
```
