# 9.1.2 Obsidian Second Brain Current Context

## Scope

This document records how 9.1.2 should update the external Obsidian knowledge base:

```text
E:\minecraftserver\fabricmod\tzz-mod-docs-obsidian\Tzz Mod work
```

The vault is not a Git repository. Do not run `git init` there. Do not copy large code blocks into notes. Keep notes as durable project memory, not a phase log dump.

## Precision Repository Index Supplement

The long-term project rule has been upgraded: Obsidian is now a rolling precision repository index and external project memory, not only a summary vault.

The main repository `AGENTS.md` must include the durable rule set:

- read relevant Obsidian notes at the start of TZZ Mod development, refactor, performance, testing, docs, checkpoint, merge and tag tasks;
- write durable source locations, file responsibilities, class/function responsibilities, data flow, state boundaries, guard/test rules, historical pitfalls, performance hotspots, version decisions and deferred risks back to Obsidian;
- update Obsidian when source verification shows a note is stale, marking old information stale/deprecated and recording the last verified commit or tag;
- create/update index notes when adding modules, services, guards, tests, data structures or key flows;
- keep notes structured and avoid copying large source blocks;
- treat Obsidian as auxiliary memory, not a replacement for current source checks and automated validation.

9.1.2 should maintain both the performance-note set below and the precision index notes:

- `12_仓库精确索引/功能到文件索引.md`
- `13_源码文件职责/WebAdmin_文件职责表.md`
- `13_源码文件职责/LogicChain_文件职责表.md`
- `15_测试与Guard索引/改动后该跑哪些测试.md`
- `08_Codex工作流/Codex_工作流与提示词规范.md`
- `99_索引/TZZ_Mod_索引.md`

Phase 2 runtime optimization added another required precision note:

- `13_源码文件职责/Runtime_文件职责表.md`

It records the current runtime hot-path files, accepted Phase 2 indexes/caches and deferred runtime optimizations. If Phase 2 changes again before checkpoint, update that note and the performance notes together.

Phase 5/6 add two more durable index expectations:

- record `WebAdminVirtualBlockDeviceNativeTriggerService.validateGateBinding(...)` as delegating to `WebAdminConditionGateBindingValidator`, with VBD-specific dynamic container compatibility profile still owned by the VBD service;
- record that Phase 6 guard ratchets `WebAdminVirtualBlockDeviceNativeTriggerService.java` line/byte growth in `CodeQualityGuardTest` and that timing rows stay warning/report-only.

The main index quick entries should stay categorized by task instead of becoming a flat link dump:

- start-task required reading;
- 9.1.2 performance notes;
- source responsibility tables;
- system architecture / flow notes;
- history, deferred risks and roadmap notes.

Phase 6 must update at least these vault notes before checkpoint:

- `99_索引/TZZ_Mod_索引.md`
- `07_9.1.2_真实性能优化/9.1.2_总览.md`
- `07_9.1.2_真实性能优化/WebAdmin_大图性能.md`
- `07_9.1.2_真实性能优化/游戏内_Runtime_性能.md`
- `07_9.1.2_真实性能优化/Store_Session_Registry_性能.md`
- `07_9.1.2_真实性能优化/压测样本与Benchmark.md`
- `07_9.1.2_真实性能优化/Deferred_高风险优化.md`
- `12_仓库精确索引/功能到文件索引.md`
- `13_源码文件职责/WebAdmin_文件职责表.md`
- `15_测试与Guard索引/改动后该跑哪些测试.md`

## Source Notes Read At Phase Start

The 9.1.2 audit uses these existing notes as historical context:

- `99_索引/TZZ_Mod_索引.md`
- `00_项目总览/TZZ_Mod_项目总览.md`
- `07_9.1.1_代码健康治理/9.1.1_技术债治理总览.md`
- `09_Roadmap_9x_10x/9x_10x_路线图.md`

The existing vault is currently centered on 9.1.1 code-health and performance baseline knowledge. 9.1.2 should add a separate performance optimization section rather than overwriting 9.1.1 notes.

## Required 9.1.2 Note Set

Create or update:

| Note | Purpose |
| --- | --- |
| `07_9.1.2_真实性能优化/9.1.2_总览.md` | phase goals, boundaries, release relation to 9.1.1. |
| `07_9.1.2_真实性能优化/WebAdmin_大图性能.md` | Logic Chain large graph benchmarks, DOM equivalence, deferred UI risks. |
| `07_9.1.2_真实性能优化/游戏内_Runtime_性能.md` | SignalBridge, ActionEngine, ConditionGate, VBD, RegionController and Timer runtime findings. |
| `07_9.1.2_真实性能优化/Store_Session_Registry_性能.md` | JSON store, snapshot, session and registry findings. |
| `07_9.1.2_真实性能优化/压测样本与Benchmark.md` | deterministic seed, fixture tiers, low-end estimate schema. |
| `07_9.1.2_真实性能优化/Deferred_高风险优化.md` | behavior-sensitive optimizations that remain deferred. |
| `99_索引/TZZ_Mod_索引.md` | add links to the 9.1.2 note set. |

## Low-End Performance Memory

The Obsidian benchmark note must explicitly record:

- user hardware: Intel Core i7-14700KF, 64GB DDR5, RTX 4080 Super;
- why this machine is an upper-bound reference only;
- low-end client model: older 4 core / 8 thread CPU, 8GB-16GB RAM, integrated or entry GPU;
- average client model: 6 core / 12 thread CPU, 16GB RAM, mid GPU;
- low-end server/VPS model: 2-4 vCPU, 4GB-8GB RAM, ordinary SSD;
- required benchmark fields: `measured_ms`, x3/x5/x10 estimates, growth factor, complexity estimate, risk level and reason.

## Durable Boundaries To Preserve

Record these as durable 9.1.2 boundaries:

- performance optimization must not change runtime semantics;
- WebAdmin writes still require permission, CSRF/same-origin, edit lock, expected fingerprint, validation, audit and realtime;
- Logic Chain saves typed resources, not freeform graph documents;
- ConditionEngine remains read-only and side-effect-free;
- blank runtime gates do not load stores or evaluate conditions;
- VBD/RegionController/world-device behavior must not force-load chunks or fake world-backed data;
- timing is report-only at first, while deterministic behavior/DOM/marker invariants can hard fail.

## Phase Update Plan

| Phase | Obsidian update expectation |
| --- | --- |
| Phase 0 | add overview of audit findings and benchmark model. |
| Phase 1 | add actual fixture and guard shape. |
| Phase 2 | add runtime before/after findings and accepted/deferred optimizations. |
| Phase 3 | add WebAdmin large graph before/after and DOM-equivalence notes. |
| Phase 4 | add store/session before/after and cache invalidation decisions. |
| Phase 5 | add complexity simplification decisions. |
| Phase 6 | ratchet final guard knowledge, classify quick index entries, update stale `last_verified_commit` values and ensure index links are complete. |

## Main Repo Source Docs

The vault should link back to:

- `docs/PERFORMANCE_AUDIT_9_1_2.md`
- `docs/RUNTIME_HOTSPOTS_9_1_2.md`
- `docs/WEBADMIN_LARGE_GRAPH_BENCHMARK_PLAN_9_1_2.md`
- `docs/SYNTHETIC_FIXTURES_9_1_2.md`
- `docs/PERFORMANCE_OPTIMIZATION_PLAN_9_1_2.md`
- `docs/OBSIDIAN_SECOND_BRAIN_9_1_2_CURRENT_CONTEXT.md`
- `docs/IF_COMPLEXITY_HOTSPOT_AUDIT_9_1_2.md`
