# 9.1.2 Phase 5 if / complexity / hotspot audit

## Scope

Phase 5 rechecks previously deferred complexity hotspots from the 9.1.2 performance work. It accepts only behavior-equivalent simplification that can be proven by existing hard guards or focused characterization tests.

This phase does not change WebAdmin API behavior, production frontend routing, Logic Chain render output, runtime gate semantics, save error codes, Chinese validation messages, fail-closed validation or safety-boundary `if` branches.

## Final Release Status

The final 9.1.2 release is `v1.68.3-real-performance-deep-simplification` at `1188c6601d7071e31aff3bfbc2355e7470bebafe` with tag object `e793aa30e720991d024fafaf4beef99dd54f2993`.

After Phase 6, commit `1188c66 test: align 9.1.2 code quality guard baselines` repaired guard byte counting by switching from raw `Files.size(...)` to CRLF -> LF normalized UTF-8 bytes. This records the platform-independent no-growth baseline and does not weaken the line/byte hard guard.

## Subagent Review

Phase 5 used 6 read-only subagent roles across 3 review rounds before implementation:

| Role | Conclusion |
| --- | --- |
| Boundary / architecture | Do not touch safety `if`, runtime semantics, production JS, route dispatch or BeforeVxx wrappers without stronger golden coverage. |
| Frontend JS / UI | Keep giant JS builder and routing helpers deferred; current app.js no-growth and DOM guards do not justify local mutation rewrites. |
| Docs / Obsidian | Create the missing 9.1.2 complexity audit and record accepted/deferred decisions in docs and the external index. |
| Guard baseline | Preserve BeforeV total, BeforeV18+ hard-fail zero, app.js no-growth and selector/handler no-growth. |
| Backend / service | Accept VBD native trigger gate-binding validator dedupe through `WebAdminConditionGateBindingValidator`; keep dynamic container profiles. |
| Test / Git hygiene | Run Gradle guard set and `git diff --check`; stage only phase files; leave `.codex/` and `logs/` untracked. |

Round 1 identified candidate areas and rejected UI/routing/runtime rewrites as under-guarded. Round 2 narrowed the accepted implementation to VBD native trigger backend validation helper dedupe. Round 3 required a pre-edit stabilization guard run and preservation of the `validateGateBinding` marker.

## Accepted Hotspot

| Hotspot | Category | Change | Equivalence proof |
| --- | --- | --- | --- |
| `WebAdminVirtualBlockDeviceNativeTriggerService.validateGateBinding` duplicate condition group validation | Duplicate backend helper logic | The VBD native trigger service now delegates condition group binding validation to `WebAdminConditionGateBindingValidator`, while keeping the local `validateGateBinding(...)` wrapper and VBD-specific `gateProfile(...)` logic. | `WebAdminConditionGateConfigTest` now asserts VBD field preservation, exact error codes, rejected value summaries, Chinese message fragments, blank-id no-load behavior, degraded store behavior and container profile compatibility. `StabilizationGuardTest` checks the shared validator delegation marker and compatibility analyzer boundary. |

## Before / After Summary

| Metric / boundary | Before Phase 5 | After Phase 5 |
| --- | --- | --- |
| VBD backend gate validator logic | Local duplicate load/normalize/enabled/definition/validation/compatibility branch chain. | Delegates to shared `WebAdminConditionGateBindingValidator`. |
| VBD dynamic container open/close profile | Local `gateProfile(...)` decides inventory snapshot compatibility. | Unchanged; profile is still built by VBD service and passed into the shared validator. |
| Error codes / Chinese messages | Existing shared semantics and VBD local duplicate semantics matched. | Preserved by shared validator and strengthened tests. |
| Blank condition group id | Optional and returns before store load. | Preserved and explicitly tested against a degraded store. |
| WebAdmin write validation authority | WebAdmin write validators use uncached authoritative reads. | Unchanged; no production write path was moved to cached reads. |
| Production frontend / routing | Large JS/router/UI-builder debt exists but is guarded mostly by no-growth and DOM snapshots. | Deferred; no production JS touched. |

## Phase 6 Ratchet

The accepted Phase 5 simplification is now protected by `CodeQualityGuardTest` file line/byte no-growth baselines for `WebAdminVirtualBlockDeviceNativeTriggerService.java`. Future work that grows this service must either split responsibilities further or update the baseline only in a dedicated, behavior-proven guard phase.

## Kept Safety If

These branches remain explicit and are intentionally not collapsed:

- permission, CSRF, same-origin, edit lock and expected fingerprint checks;
- fail-closed condition group missing / disabled / invalid / incompatible / degraded handling;
- runtime gate true/false/no-side-effect boundaries;
- WebAdmin validation error field/code/message preservation;
- container inventory snapshot compatibility decisions.

## Deferred Hotspots

| Hotspot | Reason | Required future proof |
| --- | --- | --- |
| remaining BeforeVxx wrappers | Patch-stack cleanup can hide historical compatibility boundaries. | Dedicated wrapper audit with exact source/order markers and no new BeforeV18+ growth. |
| production JS routing helpers | Delegated route order, modal outside-close timing and realtime refresh behavior are user-visible. | Route-key golden matrix and browser-like event order guard. |
| giant UI builder helpers | HTML output, dirty state, focus and scroll behavior need exact equivalence. | DOM hash snapshots plus modal caret/scroll preservation guard. |
| hover/select/zoom local DOM mutation | Current full render affects node/edge classes, arrow owner, panel HTML, toolbar state and VBD overlay source fallback. | Broader DOM mutation equivalence and low-end interaction benchmark. |
| protected draft registry split | Terminal state visibility and world-device cleanup-required drafts are server-aware. | Protected draft state matrix and cleanup-required server guard. |
| timer due structure rewrite | `LinkedHashMap` due order and per-tick budget are runtime behavior. | Deterministic due-order / budget guard. |
| VBD runtime trigger/container scan narrowing | Must preserve no-force-load, cooldown, fingerprint and event order. | Unloaded chunk no-read and VBD order/fingerprint guard. |

## Phase 5 Validation

Phase 5 validation uses:

```powershell
.\gradlew.bat testClasses
.\gradlew.bat codeQualityGuardTest --rerun-tasks
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
git diff --check
```

MCP npm build/test is not required unless `tools/tzz-test-mcp` changes.

## Phase 6 Validation

Phase 6 guard ratchet / knowledge-base validation uses the same Gradle guard set because it changes guard code and source docs:

```powershell
.\gradlew.bat testClasses
.\gradlew.bat codeQualityGuardTest --rerun-tasks
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
git diff --check
```

For the external Obsidian vault, manually inspect the modified markdown files because the vault is outside the main repository and not covered by `git diff --check`.
