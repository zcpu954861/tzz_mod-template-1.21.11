# 9.1.1 Codebase Health Guard Baseline Current Context

## Baseline

- Stable version: `v1.68.1-codebase-health-audit`
- Stable commit: `57212e5bb40777620742dbdd8ee65a867a993b23`
- Phase 1 branch: `feature/codebase-health-guard-baseline-9-1-1`
- Phase 1 scope: behavior freeze and guard baseline only.

This phase does not change features, WebAdmin UI behavior, runtime semantics, API behavior, data model semantics, Logic Chain render/layout, VBD editor behavior, selection sessions, protected draft behavior or capture writeback behavior.

It also does not perform a React/Vite migration, does not introduce 9.2 typed actions, does not add Rich Text Builder work, and does not start Minecraft, MCP scenario runs or screenshot matrix validation.

## Implemented Guard Classes

- `CodeQualityGuardTest`: aggregate JavaExec entry for Phase 1 guard baseline.
- `CodeQualityGuardSupport`: shared dependency-free guard helper.
- `WebAdminFrontendBundleGuardTest`: generated app.js/app.css size, `node --check`, event/selector/inline-handler metrics, facade and pointer-events checks.
- `WebAdminPerformanceBaselineGuardTest`: deterministic performance baseline metrics and Phase 6 deferral checks.
- `DocsConsistencyGuardTest`: README/current context/audit docs consistency checks.

The new guard code stays in `src/test/java/com/zcpu/tzzmod/stabilization/` and does not add more checks to `StabilizationGuardTest`.

## Current Baseline Metrics

| Metric | Baseline |
| --- | ---: |
| `WebAdminFrontendScripts.java` lines / bytes | 8,433 / 1,984,343 |
| `WebAdminFrontendStyles.java` lines / bytes | 75 / 123,798 |
| `WebAdminLogicChainEditorService.java` lines / bytes | 5,205 / 318,695 |
| `WebAdminServer.java` lines / bytes | 3,102 / 164,014 |
| `WebAdminProtectedDraftRegistry.java` lines / bytes | 620 / 26,369 |
| `StabilizationGuardTest.java` lines / bytes | 12,423 / 839,370 |
| `BeforeV13-17` total | 97 |
| all `BeforeV\d+` tokens | 184 |
| `document.addEventListener` | 22 |
| all `addEventListener(` | 70 |
| `.closest(` | 73 |
| `querySelector(` | 71 |
| `onclick=` | 251 |
| `oninput=` | 150 |
| `onchange=` | 108 |
| `onkeydown=` | 17 |
| `htmlEvent(` | 45 |
| `htmlHandler(` | 177 |
| `innerHTML` | 61 |
| `nativeTriggerJson` | 2 |

Generated bundle metrics are tracked by `WebAdminFrontendBundleGuardTest` at runtime:

- `app.js` UTF-8 bytes: 1,838,292; Phase 1 warning limit is current + 5%.
- `app.css` UTF-8 bytes: 123,251; Phase 1 warning limit is current + 5%.
- `node --check build/tmp/webadmin-app.js`: hard fail.
- `vm.Script` parse/compile timing: report-only soft baseline; first local run recorded about 13 ms on Node `v24.15.0`.

## Hard Fail Rules

- Node.js missing or `node --check build/tmp/webadmin-app.js` fails.
- Any new `BeforeV18+` patch-stacking token.
- Known giant files grow beyond the Phase 1 baseline while this phase forbids touching them.
- New frontend script/style modules exceed 800 lines.
- New backend service exceeds 1000 lines unless explicitly grandfathered in the baseline.
- New guard class exceeds 1000 lines.
- WebAdmin frontend facade boundary is broken.
- Logic Chain pointer-event invariants are broken: edge layer, minimap and draft handles stay `pointer-events:none`; connect plus stays `pointer-events:auto`.
- VBD native trigger raw JSON not-primary-summary marker is missing.
- README stable version is not `v1.68.1-codebase-health-audit`.
- The four 9.1.1 audit docs or this current context doc are missing.

## Warning / Baseline Rules

- Existing giant Java method lengths and generated JS function line/character sizes are reported, not failed.
- Existing `BeforeV13-17` and all `BeforeV\d+` counts are reported. Growth is warning unless it introduces `BeforeV18+`.
- Hotspot wrapper/redefinition counts for known Logic Chain overlay functions are hard no-growth checks to prevent new patch-stacking that avoids the `BeforeVxx` naming convention.
- Existing inline handler, selector and `innerHTML` counts are reported. Phase 1 warns on growth and does not require immediate reduction.
- Known zero-baseline inline event attributes are warning-only in Phase 1.
- `app.js` and `app.css` byte growth above current + 5% is warning unless syntax fails.
- `vm.Script` parse/compile timing is reported only. It is not a hard performance budget because Windows, Node version and CI load can add noise.

## Phase 1 Active Guards

- File size and line count baselines for known giant files.
- BeforeVxx patch-stacking baseline and `BeforeV18+` hard fail.
- New module size budgets.
- WebAdmin frontend bundle byte metrics and `node --check`.
- Event, selector, inline handler and raw JSON summary metrics.
- CSS pointer-events overlay invariants.
- README/current-context/docs consistency.

## Deferred to Phase 2 / 3 / 6

Deferred to Phase 2:

- Splitting `WebAdminFrontendScripts.java`.
- Splitting CSS modules.
- Turning app.js/app.css byte budgets into tighter ratchets after split.

## Phase 2 Frontend Bundle Split Context

Phase 2 mechanically splits the generated WebAdmin JavaScript bundle while preserving the `/assets/app.js` facade and generated output contract.

- Phase 2 branch: `feature/webadmin-frontend-bundle-split-9-1-1`.
- Phase 2 base: `origin/feature/codebase-health-guard-baseline-9-1-1` at `ede575b25be55424a060ae749c2b36922ff599e1`.
- `WebAdminFrontendScripts.java` is now the bundle entry only and concatenates ordered script modules.
- `WebAdminFrontendAssets.appJs()` still delegates to `WebAdminFrontendScripts.appJs()`.
- `WebAdminServer` is unchanged and still serves `/assets/app.js` through `WebAdminFrontendAssets`.
- `WebAdminFrontendStyles.java` and `/assets/app.css` are unchanged; CSS split remains deferred.
- Generated `app.js` is byte-identical to the Phase 1 guard baseline: `1,838,292` bytes, SHA-256 `1992d2e7634e14ac9611d893cf8439725bbc0fe4ee65f672cc90910b64238b74`.
- Guard scans that previously read only `WebAdminFrontendScripts.java` now use generated `WebAdminFrontendAssets.appJs()` or marker-based generated slices so the split modules remain covered.
- The guard now hard-ratchets `WebAdminFrontendScripts.java` as a facade and asserts the expected module concat order.

Phase 2 frontend script modules:

- `WebAdminFrontendIconScripts`: flat icon registry and SVG geometry bootstrap.
- `WebAdminFrontendCoreScripts`: app state, API, route, realtime, shared formatting and early core helpers.
- `WebAdminFrontendCoreEventScripts`: global delegated handlers and early dashboard bootstrap boundary.
- `WebAdminFrontendPageScripts`: non-Logic-Chain page bundle facade.
- `WebAdminFrontendDashboardScripts`: dashboard and selection entry helpers.
- `WebAdminFrontendDeviceScripts`, `WebAdminFrontendDeviceSessionScripts`, `WebAdminFrontendDeviceEditorScripts`: device detail, VBD/native trigger, itemSubmit/container capture and device write modal helpers.
- `WebAdminFrontendSignalScripts`, `WebAdminFrontendActionTimerScripts`: signal/listener/action/timer pages and editors.
- `WebAdminFrontendHelpScripts`, `WebAdminFrontendModalScripts`, `WebAdminFrontendSnapshotScripts`, `WebAdminFrontendTemplateConfigScripts`, `WebAdminFrontendRegionConditionScripts`: help center, modal infrastructure, snapshots/templates/config, region and condition pages.
- `WebAdminLogicChainViewerScripts`, `WebAdminLogicChainEditorScripts`, `WebAdminLogicChainVbdScripts`: Logic Chain viewer/layout, editor/draft/save helpers and VBD trigger/capture overlays.
- `WebAdminFrontendBootstrapScripts`: final metadata helpers and `initLogin();initApp();`.

Phase 2 deliberately does not change UI behavior, event routing semantics, Logic Chain render/layout algorithms, VBD/runtime behavior, backend APIs, WebAdmin write paths, React/Vite architecture or performance caching.

Deferred to Phase 3:

- Event route table migration.
- Hard fail for new inline handlers after route-table coverage exists.
- `.closest(` / `querySelector(` reduction targets.

Deferred to Phase 6:

- Hover class-only updates.
- Click/select local panel updates.
- Zoom transform-only update.
- Drag pointermove rAF coalescing.
- Draft overlay and VBD trigger overlay memoization.
- Unsaved diff memoization.
- Browser performance markers, MCP scenario performance checks and screenshot matrix.

## Phase 3 Event Router Split Context

Phase 3 splits delegated WebAdmin event handling into explicit route tables and named handlers while preserving 9.1 behavior.

- Phase 3 branch: `feature/webadmin-event-router-split-9-1-1`.
- Phase 3 base: `origin/feature/webadmin-frontend-bundle-split-9-1-1` at `e77071b9d929053c45ffa17a25e5f1788975cd7e`.
- Global document listener registration order and capture flags remain unchanged.
- CoreEvent keydown and Modal ESC keydown remain separate document listeners to preserve same-target listener ordering.
- VBD capture retry remains available through both `pointerup` capture and `click` capture.
- Logic Chain capture click routes now preserve the existing order: connect handle, connect candidate, new draft channel, draft slot, node card.
- Logic Chain hover / mouseout still only target read-only node cards and still use the existing highlight render path.
- VBD trigger card selection moved from inline `htmlHandler` to a document bubble route so the old post-click outside-close side effects still run.
- `/assets/app.js`, `WebAdminFrontendAssets`, `WebAdminServer`, `WebAdminFrontendStyles`, backend/runtime services and CSS remain unchanged.

Phase 3 guard changes:

- Phase 2 byte-identical SHA check is replaced by SHA reporting plus `app.js` baseline + 5% hard limit.
- `document.addEventListener`, total `addEventListener(`, `.closest(`, `querySelector(`, known inline event attributes, `htmlEvent(`, `htmlHandler(` and `innerHTML` are hard no-growth checks.
- Route-table markers are required for `dispatchDelegatedEvent`, global click/key routes, modal ESC routes and Logic Chain click/hover/pointer routes.
- Route table entries must use named handlers rather than inline lambdas.
- `node --check build/tmp/webadmin-app.js`, `BeforeV18+ = 0`, facade checks and pointer-events guards remain hard checks.

## Phase 4 Logic Chain Render Module Split Context

Phase 4 mechanically splits Logic Chain render/layout/draft-related frontend script boundaries while preserving the Phase 3 generated JavaScript output.

- Phase 4 branch: `feature/logic-chain-render-module-split-9-1-1`.
- Phase 4 base: `origin/feature/webadmin-event-router-split-9-1-1` at `5dc2b4181ab42262508e6a2827b8c8a2c76dbc23`.
- `WebAdminFrontendScripts.java` remains the bundle facade and now concatenates the smaller Logic Chain render modules in original generated order.
- `WebAdminFrontendAssets`, `WebAdminServer`, `WebAdminFrontendStyles`, backend/runtime services, commands, stores and MCP tooling remain unchanged.
- Generated `app.js` is byte-identical to the Phase 4 before artifact: `1,843,648` bytes, SHA-256 `057e7e370d555036aff6d542b3ae4361f82d734b8fa95cf429d4d7ac7425beb3`.

Phase 4 Logic Chain frontend script modules:

- `WebAdminLogicChainViewerScripts`: Logic Chain list/detail route rendering and viewer shell helpers before canvas composition.
- `WebAdminLogicChainCanvasScripts`: canvas toolbar/minimap helpers, canvas stage entry, mind map/edge path render helpers, and canvas interaction helpers. The class exposes ordered sub-entry methods so generated output remains byte-identical.
- `WebAdminLogicChainNodePanelScripts`: selected-node detail panel, readonly/deferred cards, node metadata rows and action-entry render helpers.
- `WebAdminLogicChainLayoutScripts`: layout V2, fixed node height application, saved producer placement adjustment and layout helper functions.
- `WebAdminLogicChainDraftOverlayScripts`: base draft layout overlay, draft channel/reference insertion, draft slot overlay and related draft layout helpers.
- `WebAdminLogicChainDiffScripts`: existing draft comparison, draft diff rows, unsaved change banner and diff HTML helper.
- `WebAdminLogicChainEditorScripts`: Logic Chain edit/draft/save modal helpers; it imports `WebAdminLogicChainDiffScripts.appJs()` at the original generated order.
- `WebAdminLogicChainVbdScripts`: VBD native trigger readable diff summaries and pre-overlay VBD helper hooks.
- `WebAdminLogicChainVbdOverlayScripts`: VBD native trigger graph overlay, stable trigger identity, capture retry and itemSubmit/container draft writeback wrappers.

Phase 4 guard changes:

- `CodeQualityGuardTest` facade order now includes the new Logic Chain modules and the ordered Canvas sub-entry methods.
- `CodeQualityGuardTest` verifies `WebAdminLogicChainEditorScripts` includes `WebAdminLogicChainDiffScripts.appJs()` in the expected local order.
- `WebAdminFrontendBundleGuardTest` compares generated `app.js` with `build/tmp/webadmin-app-phase4-before.js` when that local phase artifact exists, and reports before/after bytes and SHA-256.
- `node --check`, `BeforeV18+ = 0`, event/router no-growth checks, pointer-events invariants and raw JSON summary guards remain hard checks.

Phase 4 deliberately does not change UI behavior, event routing semantics, Logic Chain render/layout algorithms, VBD/native trigger behavior, backend APIs, runtime behavior, CSS, React/Vite architecture or performance caching.

## Phase 5 Backend Logic Chain Service Split Context

Phase 5 mechanically splits the backend Logic Chain draft save coordination while preserving the 9.1 WebAdmin API, save transaction boundaries and runtime semantics.

- Phase 5 branch: `feature/logic-chain-backend-service-split-9-1-1`.
- Phase 5 base: `origin/feature/logic-chain-render-module-split-9-1-1` at `dc6ec4f28806aef2f8c19cd27f810998428ced95`.
- `WebAdminServer` remains unchanged: route parsing, CSRF/same-origin context, write-before snapshot, audit/realtime response wrapping and `/api/webadmin/logic-chain-editor/save-draft` response shape stay at the server boundary.
- `WebAdminLogicChainEditorService.saveDraft()` is now a facade into `LogicChainDraftSaveCoordinator`.
- `LogicChainDraftSaveCoordinator` preserves the frozen order: write preflight -> editor lock -> base graph fingerprint -> validation -> mixed-write fail-closed guards -> typed write execution -> channel metadata tail write -> successful editor lock release.
- `LogicChainDraftOperationPlanner` describes which typed operations are present and keeps channel metadata as an independent tail boundary. It does not execute store mutation and does not create a freeform graph save model.
- `LogicChainTypedWriteExecutor` only calls the existing typed write adapter methods in the original order: new nodes -> action append -> existing node edits -> action edits -> action deletes -> action reorders -> node deletes.
- The existing VBD, world device, RegionController, action maintenance and node delete store writes remain in `WebAdminLogicChainEditorService` adapter methods, so protected draft state transitions, partial cleanup and typed service calls keep their previous semantics.
- The split explicitly does not claim full cross-store atomic transactions or complete rollback; existing recoverable failure behavior keeps the Logic Chain editor lock and draft as before.

Phase 5 guard changes:

- `CodeQualityGuardTest` scope marker is `phase5-backend-logic-chain-service-split`.
- `WebAdminLogicChainEditorService.java` is ratcheted down after the save coordinator split.
- Guard checks require the new coordinator/planner/executor files, the saveDraft facade delegation, the frozen typed write order, the channel metadata tail boundary, a planner ordering test, and no fake `WebAdminMapServer` / full cross-store atomic transaction claim.
- Existing frontend bundle, `node --check`, `BeforeV18+ = 0`, event/router no-growth checks and pointer-events guards remain unchanged.

Phase 5 deliberately does not change UI behavior, WebAdmin route/API behavior, request/response shape, error codes, Chinese error messages, edit lock / target lock behavior, expectedFingerprint handling, write-before snapshot behavior, audit/realtime publication, VBD/world device/RegionController runtime semantics, CSS, frontend modules, commands, stores data format, MCP tooling or performance caching.

## Phase 6 Logic Chain Performance Baseline Context

Phase 6 establishes a Logic Chain render performance and DOM-equivalence baseline before applying small, safe optimizations.

- Phase 6 branch: `feature/logic-chain-performance-baseline-9-1-1`.
- Phase 6 base: `origin/feature/logic-chain-backend-service-split-9-1-1` at `995f3dd79c7927f707068883770446cc3380878e`.
- `origin/master` and `v1.68.1-codebase-health-audit` remain `57212e5bb40777620742dbdd8ee65a867a993b23`.
- WebAdmin routes, `WebAdminFrontendAssets`, `WebAdminServer`, `WebAdminFrontendStyles`, backend save services, runtime classes, commands, stores and MCP tooling remain unchanged.
- This phase does not run Minecraft, MCP scenario checks or screenshot matrix validation.

Phase 6 performance guard changes:

- `WebAdminPerformanceBaselineGuardTest` now reports `performance.phase=phase6-logic-chain-performance-baseline`.
- The guard records app.js before/after bytes and SHA-256. Phase 6 before is `1,843,648` bytes, SHA-256 `057e7e370d555036aff6d542b3ae4361f82d734b8fa95cf429d4d7ac7425beb3`; current Phase 6 after is `1,846,211` bytes, SHA-256 `474cc3093532f70d78583f996e8d6606496f45db831232f32607439a821a0069` (`+2,563` bytes).
- Synthetic Logic Chain render baselines cover initial readonly render, selected-node render, hover render, edit-mode render, draft overlay render, unsaved-expanded diff render, VBD trigger overlay render, pending-delete draft action render, VBD selected fallback/source priority and minimap segment cap.
- DOM equivalence is hard-guarded through stable signatures for node positions/classes, edge path `d`, `marker-end`, target arrow owner markers, selected/related/dimmed classes, selected panel HTML, diff banner HTML, minimap HTML and VBD trigger overlay source markers.
- Pending-delete card/badge/diff markers across SignalListener and Timer draft action buckets, `_pendingDelete` save payload leakage, VBD trigger stable identity/no duplicate/target-channel/source-card markers, VBD selected fallback/source priority and `segments.slice(0,24)` minimap cap are hard-guarded.
- The Phase 4 local before artifact comparison is downgraded to a warning only when Phase 6 scope marker, Phase 6 before bytes/SHA, Phase 6 app.js warning limit and Phase 6 performance markers all match. Otherwise the Phase 4 before artifact mismatch remains a hard failure.
- Render timing is report/warning only. Syntax, markers and DOM equivalence are hard fail.
- Existing `node --check`, `BeforeV18+ = 0`, event/router no-growth checks, pointer-events invariants, facade checks and raw JSON summary guards remain active.

Phase 6 low-risk optimizations:

- `logicChainRelatedNodeIndex(graph)` precomputes related node sets once inside `logicChainMindMap()` for the current render and passes the index down to positioned node cards. It deliberately does not write cache fields onto `graph`, so read-only paths that reuse WebAdmin API response objects are not polluted.
- `logicChainMinimap(graph)` memoizes the current `segments.slice(0,24)` HTML in a module-level `{key, html}` memo. The key is derived only from channel id and downstream count; draft overlay, hover, selected node, zoom and pan are intentionally excluded because they must not affect minimap output.
- These optimizations do not cache WebAdmin API response object references, edit lock state, modal inputs, protected draft terminal state or save payload data.
- Chinese comments document cache keys, invalidation conditions and stale graph risks.

Deferred after Phase 6:

- Hover/select class-only updates remain deferred because hover and selection also affect arrow ownership and `marker-end`.
- Zoom transform-only update remains deferred until toolbar percentage and DOM equivalence are covered by a focused interaction guard.
- Draft overlay / VBD overlay cross-render memoization remains deferred because there is no single authoritative `draftRevision` and VBD overlay still depends on selected-node fallback and capture writeback mutation.
- VBD overlay pipeline unification, selected fallback behavior changes, dirty calculation changes and backend save performance tuning remain separate behavior-fix or later-phase work.

## README Update

Phase 1 minimally updates README stable version to `v1.68.1-codebase-health-audit`. It does not expand README feature content.

## Validation

Run:

```powershell
.\gradlew.bat testClasses
.\gradlew.bat test --tests com.zcpu.tzzmod.webadmin.service.WebAdminLogicChainEditorServiceTest
.\gradlew.bat codeQualityGuardTest --rerun-tasks
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
git diff --check
```

This phase does not run Gradle `clean build`, Minecraft, MCP scenario, screenshot matrix, `tools/tzz-test-mcp npm run build`, or `tools/tzz-test-mcp npm test` unless a later prompt explicitly changes scope. The targeted Gradle `test --tests` command may be a no-op for the current JavaExec-style service test; `stabilizationGuardTest` still invokes `WebAdminLogicChainEditorServiceTest.run()` and remains the authoritative service-test execution path.

## Git Hygiene

- `.codex/` and `logs/` remain unhandled and unsubmitted.
- Do not submit `reports/mcp/`, screenshots, node_modules, build output, run output, `.gradle/`, temporary scripts or temporary generated reports.
- Do not commit, push, merge or tag in this phase.

## Next Phase

Phase 6 should address performance-local render/layout optimizations only after a separate prompt confirms scope and baseline.
