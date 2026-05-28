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

## README Update

Phase 1 minimally updates README stable version to `v1.68.1-codebase-health-audit`. It does not expand README feature content.

## Validation

Run:

```powershell
.\gradlew.bat testClasses
.\gradlew.bat codeQualityGuardTest --rerun-tasks
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
git diff --check
```

This phase does not run Gradle `clean build`, Minecraft, MCP scenario, screenshot matrix, `tools/tzz-test-mcp npm run build`, or `tools/tzz-test-mcp npm test` unless a later prompt explicitly changes scope.

## Git Hygiene

- `.codex/` and `logs/` remain unhandled and unsubmitted.
- Do not submit `reports/mcp/`, screenshots, node_modules, build output, run output, `.gradle/`, temporary scripts or temporary generated reports.
- Do not commit, push, merge or tag in this phase.

## Next Phase

Phase 2 should start from these guard baselines and split frontend source modules without changing generated asset behavior or WebAdmin runtime semantics.
