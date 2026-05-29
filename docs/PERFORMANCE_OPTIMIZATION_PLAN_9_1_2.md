# TZZ 9.1.2 Performance Optimization Plan

## Scope

This plan sequences 9.1.2 performance work after the Phase 0 audit. It is not an implementation record. Each phase must use the subagent rounds and validation commands required by the 9.1.2 prompt.

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
| Phase 2 | runtime optimization | channel/device indexes, safe context/store cache, VBD/Region/Timer improvements | Gradle guard set and low-end report. |
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
| 6 | SignalDeviceStore id/source/channel indexes | reduce O(n) lookups | order/duplicate/missing guards | 2 |
| 7 | SignalListener channel index | reduce emit filtering | listener order guard | 2 |
| 8 | condition group cache by path/fingerprint | avoid repeated JSON read | invalidation and status guard | 2/4 |
| 9 | state variable snapshot cache | avoid gate-time JSON read | mutation invalidation guard | 2/4 |
| 10 | VBD tick pre-index by enabled VBD and trigger type | reduce per tick scans | no force-load, bound-object only | 2 |
| 11 | VBD container scan narrow by trigger/cooldown | reduce container fingerprint work | fingerprint semantics | 2 |
| 12 | itemSubmit matcher precompile per requirement set | reduce repeated parsing/scans | consume all-or-nothing | 2 |
| 13 | container matcher total reuse per snapshot | reduce condition x slot scans | matcher equivalence | 2 |
| 14 | RegionController bounding box prefilter | avoid polygon contains for misses | exact polygon still authoritative | 2 |
| 15 | RegionController planner-region id index/cache | avoid repeated synchronized linear region lookup | missing-region behavior and controller order unchanged | 2 |
| 16 | RegionController group by dimension | avoid cross-dimension checks | transition order unchanged | 2 |
| 17 | Timer due bucket or next-due shortcut | reduce every-tick full scan | preserve `LinkedHashMap` due order and `MAX_DUE_EXECUTIONS_PER_TICK` | 2 |
| 18 | Timer active count counter | reduce start storms | max active limit guard | 2 |
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
| Runtime performance | no dedicated runtime benchmark guard yet | service-level `RuntimePerformanceBaselineGuardTest`. |
| Store performance | no dedicated store benchmark guard yet | temp-dir `StorePerformanceBaselineGuardTest`. |
| Low-end reporting | not required in 9.1.1 | x3/x5/x10 estimates on every row. |
| Complexity curve | code-quality tables exist | benchmark scale factor and complexity estimate. |
| Semantic invariants | scattered service tests | focused hard guards around optimized paths. |
| Runtime ordering | scattered service behavior | hard guards for SignalBridge dispatch/history/join order and ActionEngine `executeAll` stop-on-failure. |
| Store IO reporting | not standardized | bytes, serialization count, write frequency and cleanup complexity. |

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

## Phase Checkpoint Policy

Checkpoint commits are allowed only after the phase validation command set passes. Explicit staging only; never `git add .` or `git add -A`. Do not stage `.codex/`, `logs/`, `reports/mcp/`, screenshots, node_modules, build output, run output or `.gradle/`.

Suggested Phase 0 checkpoint:

```text
docs: add 9.1.2 performance audit plan
```
