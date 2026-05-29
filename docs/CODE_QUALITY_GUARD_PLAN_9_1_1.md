# TZZ 9.1.1 Code Quality Guard Plan

## Guard strategy

9.1.1 的 guard 目标是防复发，而不是用当前历史债一次性阻塞所有开发。建议分两级：

本文是后续 9.1.1 implementation guard plan，不是当前 docs-only audit 之外的执行授权。

- Baseline warning：记录当前值，超过 current + delta 时 warning。
- Ratchet hard fail：模块拆分完成后，把新模块阈值改为 hard fail，旧巨石只允许下降不允许上升。

新增 guard 不应继续塞进 12,423 行的 `StabilizationGuardTest`。建议新增独立测试：

- `CodeQualityGuardTest`
- `WebAdminFrontendBundleGuardTest`
- `WebAdminPerformanceBaselineGuardTest`
- `DocsConsistencyGuardTest`

`StabilizationGuardTest` 只保留现有行为/marker 守护和必要总入口。

## Current baseline

| Metric | Current |
| --- | ---: |
| `WebAdminFrontendScripts.java` | 8,433 lines / 1,984,343 bytes |
| `WebAdminFrontendStyles.java` | 75 lines / 123,798 bytes |
| `WebAdminLogicChainEditorService.java` | 5,205 lines / 318,695 bytes |
| `WebAdminServer.java` | 3,102 lines / 164,014 bytes |
| `WebAdminProtectedDraftRegistry.java` | 620 lines / 26,369 bytes |
| `StabilizationGuardTest.java` | 12,423 lines / 839,370 bytes |
| `document.addEventListener` in app JS source | 22 |
| all `addEventListener(` in app JS source | 70 |
| `.closest(` in app JS source | 73 |
| `querySelector(` in app JS source | 71 |
| `onclick=` | 251 |
| `oninput=` | 150 |
| `onchange=` | 108 |
| `onkeydown=` | 17 |
| `htmlEvent(` | 45 |
| `htmlHandler(` | 177 |
| `innerHTML` | 61 |
| `BeforeV13-17` total | 97 |
| all `BeforeV\d+` tokens | 184 |

### Existing guard coverage / gaps

| Current guard area | Covers today | Does not cover yet |
| --- | --- | --- |
| `StabilizationGuardTest` 9.1 markers | typed config editor markers, no freeform graph save, protected draft required, conflict markers | code size budget, JS function length, event complexity, app.js/app.css byte budget |
| WebAdmin render smoke harness | basic generated JS/HTML smoke, marker presence | real browser performance, DOM equivalence for hover/click/zoom, modal scroll/focus retention after optimization |
| local test MCP guard | MCP foundation and safety markers | WebAdmin code-health budgets when MCP is not touched |
| Existing docs/current context checks | some phase markers are discoverable | README stable version and 9.1.1 docs-plan consistency need dedicated guard coverage |
| Existing backend service tests | many Logic Chain save paths and protected draft paths | explicit planner/coordinator/executor boundaries, rollback-scope assertions, channel metadata mixed-write boundary |

## File size guard

Initial warning thresholds:

| File/module class | Initial threshold | Later target | Mode |
| --- | ---: | ---: | --- |
| `WebAdminFrontendScripts.java` | current + 0 lines unless removing | facade only, < 300 lines | warning then hard |
| New frontend script module | 800 lines | 600 lines | hard after Phase 2 |
| New frontend style module | 800 lines | 600 lines | hard after CSS split |
| Backend service | 1000 lines | 800 lines | warning then hard |
| Backend coordinator method holder | 600 lines | 400 lines | hard after split |
| Test guard class | 1000 lines | 800 lines | warning then hard |
| Docs current context | 350 lines | case-by-case | soft informational |

Implementation notes:

- Use `git -c core.quotePath=false ls-files` or Java `Path` walk over tracked-like source roots.
- Exclude `.git`, `.codex`, `logs`, `reports/mcp`, screenshots, `node_modules`, `build`, `run`, `.gradle`.
- Treat assets separately; binary PNG line count is not meaningful.

## Function length guard

Suggested thresholds:

| Function type | Warning | Hard target |
| --- | ---: | ---: |
| Java ordinary method | 120 lines | 100 lines |
| Java test method | 180 lines | 140 lines |
| JS function in generated app | 120 logical lines or 2500 chars | 100 lines or 1800 chars |
| Modal builder function | 2000 chars per builder after split | 1500 chars |
| Event handler function | 80 lines or 1200 chars | 60 lines or 900 chars |

Immediate exceptions:

- `WebAdminFrontendScripts.appJs()` is allowed only while it is the legacy text-block entry. Once split, it must be facade/concat only.
- Existing large test methods in `StabilizationGuardTest` are grandfathered but must not grow.

Heuristic:

- Java: brace-count method parser with constructor support.
- JS-in-Java: parse `function name(`, `name=function`, `async function`, arrow handlers in `appJs()` text block; count chars and rough line span.
- Report top 50 Java methods and top 50 JS functions in guard output.

## No BeforeVxx patch stacking guard

Current counts:

- `BeforeV13`: 23
- `BeforeV14`: 28
- `BeforeV15`: 8
- `BeforeV16`: 16
- `BeforeV17`: 22

Plan:

1. Phase 1: hard fail on newly introduced `BeforeV18`, `BeforeV19`, etc.
2. Phase 1: warning if total `BeforeV\d+` count increases.
3. Phase 4: require VBD overlay pipeline to have one final definition of `logicChainApplyVbdNativeTriggerDraftGraphOverlay`.
4. Phase 7: hard fail if total historical count increases; optionally ratchet down after each cleanup.

Allowed replacement style:

- Explicit pipeline function names, e.g. `logicChainApplyBaseDraftOverlay`, `logicChainApplyVbdTriggerOverlay`.
- Ordered concat in facade is allowed; monkey patch wrappers are not allowed for new code.
- Hard requirement: guard must detect new wrapper/redefinition patterns around existing function names, not only `BeforeV18+` strings.

## WebAdminFrontendScripts no-growth guard

Rule:

- `WebAdminFrontendScripts.java` becomes bundle entry only.
- New WebAdmin business logic cannot be added to this file after Phase 2.
- Before Phase 2, additions require explicit justification in the current phase prompt.

Phase 2 implementation note:

- `WebAdminFrontendScripts.java` has been reduced to an ordered concat facade.
- Existing frontend count, inline handler, BeforeVxx and hotspot checks must scan generated `WebAdminFrontendAssets.appJs()` or all script modules, not the facade source file.
- Hot slice checks should use stable generated markers/function boundaries instead of fixed line numbers from the pre-split source file.
- New `*Scripts.java` modules remain under the Phase 2 800-line budget; large `appJs()` text-block methods are report-only until later semantic extraction phases.

Phase 4 implementation note:

- Logic Chain render/layout/draft code is split into smaller ordered modules: viewer shell, canvas, node panel, layout, draft overlay, diff summary, VBD helper and VBD overlay.
- `WebAdminFrontendScripts.java` remains the only `/assets/app.js` facade and keeps the Logic Chain module order byte-equivalent to the Phase 3 generated output.
- `WebAdminLogicChainCanvasScripts` intentionally has multiple ordered entry methods because the original canvas responsibilities were interleaved with node panel, layout and draft overlay code; this preserves generated source order without changing JS behavior.
- `WebAdminLogicChainEditorScripts` imports `WebAdminLogicChainDiffScripts.appJs()` in the original local order, and the guard checks that local ordering.
- `WebAdminFrontendBundleGuardTest` reports Phase 4 before/after app.js bytes and SHA-256 and performs an exact comparison when `build/tmp/webadmin-app-phase4-before.js` is present in the local phase workspace.
- The Phase 4 exact before artifact is not a committed long-term baseline; later phases that intentionally change generated JS should establish their own current-context baseline and guard mode.

Guard checks:

- line count must not increase unless the commit also creates module files and net appJs behavior is split.
- detect new function declarations inside `WebAdminFrontendScripts.appJs()` after facade conversion.
- detect new inline handler patterns: `onclick=`, `oninput=`, `onchange=`, `onmouseover=`, `htmlHandler(`.

## Frontend asset / escaping guard

Facade contract:

- `WebAdminFrontendAssets` must continue delegating HTML/CSS/JS to Shell/Styles/Scripts facade methods.
- `WebAdminServer` should keep serving `/assets/app.js` and `/assets/app.css` through the facade rather than directly coupling to split modules.
- No new independent WebAdmin frontend project: hard fail on `vite.config.*`, new React/Vite runtime dependency for WebAdmin, CDN framework script tags, or npm build as WebAdmin runtime prerequisite.

Escaping contract:

- `esc(...)` for HTML text/attribute context.
- `jsString(...)` for JavaScript string literal context.
- `htmlEvent(...)` / `htmlHandler(...)` are transitional legacy inline handler helpers; Phase 3 hard-fails new uses.
- Any new `innerHTML` sink must be paired with explicit escaping/template marker. Raw user values entering `innerHTML` should hard fail.
- JS split guard must export generated app.js and run `node --check` after concat.

CSS order guard:

- split style modules must concat in the same semantic order as current `WebAdminFrontendStyles.appCss()`.
- guard should check critical selector order for Logic Chain overlays, modal/body scroll, and pointer-events selectors, not just bytes.

## app.js / app.css size guard

Metrics:

- `WebAdminFrontendScripts.appJs().getBytes(StandardCharsets.UTF_8).length`
- `WebAdminFrontendStyles.appCss().getBytes(StandardCharsets.UTF_8).length`
- `node --check build/tmp/webadmin-app.js`
- Node `vm.Script` parse/compile time

Initial thresholds:

| Metric | Initial threshold | Mode |
| --- | ---: | --- |
| app.js bytes | current + 5% | warning |
| app.css bytes | current + 5% | warning |
| node syntax | must pass | hard |
| parse/compile time | current + 20% | warning |

After Phase 2:

- app.js may stay similar because concat output is unchanged.
- source module count may rise, but `WebAdminFrontendScripts.java` must shrink.
- New module bytes should be budgeted by feature area.

## Event handler if/closest/querySelector guard

统计口径固定为 `.closest(` 和 `querySelector(` 调用数；单独的 `closest` token 只作为辅助信息，不用于阈值。

Current hot slices:

| Slice | Lines | Current count |
| --- | --- | --- |
| Global capture/bubble click/key handlers | 1260-1339 | 94 `if`, 35 `.closest(` calls, 69 `closest` tokens, 4 `querySelector(` calls, 12 listeners |
| Logic Chain delegated handlers | 8103-8123 | 48 `if`, 13 `.closest(` calls, 19 `closest` tokens, 1 `querySelector(` call, 6 listeners |
| VBD overlay patch stack | 8128-8397 | 368 `if`, 1 `.closest(` call, 3 `closest` tokens, 3 `querySelector(` calls, 33 inline handlers |

Phase 3 event-router baseline after split:

| Slice | Guard marker | Phase 3 count |
| --- | --- | --- |
| Global route handlers | `function globalEventTargetOutside` -> `beforeunload` | 30 `if`, 4 `.closest(` calls, 4 `querySelector(` calls |
| Modal ESC router | `const globalModalEscapeRoutes` -> `function unavailableFeature` | 1 `if`, 0 `.closest(` calls, 0 `querySelector(` calls |
| Logic Chain route handlers | `function handleLogicChainConnectHandleClick` -> node delete confirm marker | 32 `if`, 4 `.closest(` calls, 1 `querySelector(` call |
| VBD overlay stack | VBD trigger graph summary marker -> draft action delete panel | 114 `if`, 1 `.closest(` call, 0 `querySelector(` calls |

Guard rules:

- `document.addEventListener` and total `addEventListener(` counts must not increase after Phase 3.
- `.closest(` and `querySelector(` counts must not increase after Phase 3.
- Known inline handlers, `htmlEvent(`, `htmlHandler(` and `innerHTML` are hard no-growth checks after Phase 3.
- Zero-baseline inline event attributes are hard fail after Phase 3.
- Event route table entries must be named handlers, not long inline lambdas.
- Required markers include `dispatchDelegatedEvent`, `globalClickCommandRoutes`, `globalClickSideEffectRoutes`, `globalClickLateRoutes`, `globalPrimaryKeydownRoutes`, `globalModalEscapeRoutes`, `logicChainClickRoutes`, `logicChainHoverRoutes` and `logicChainMouseOutRoutes`.
- VBD trigger card selection must stay in document bubble routing, after the Logic Chain capture route and before outside-close side effects, so it mirrors the old inline click plus document bubble sequence.
- Phase 2 byte-identical `app.js` SHA is no longer a hard gate after event rewrite; guard still reports SHA and hard-fails if `app.js` exceeds baseline + 5%.

Suggested thresholds after Phase 3:

| Metric | Target |
| --- | ---: |
| Global handler slice `if` | < 25 |
| Global handler slice `.closest(` | <= current baseline, then ratchet below 35 |
| Logic Chain handler slice `if` | < 25 |
| Logic Chain handler slice `.closest(` | <= current baseline, then ratchet below 13 |
| Per event handler chars | < 1200 |

## Pointer-events overlay guard

Must keep:

- `.logic-chain-edge-layer{pointer-events:none}`
- `.logic-chain-minimap{pointer-events:none}`
- `.logic-chain-draft-handles{pointer-events:none}`
- `.logic-chain-connect-plus{pointer-events:auto}`

Add guard:

- CSS marker check for above declarations.
- DOM/render smoke check that edge SVG layer does not receive pointer handlers.
- If `.logic-chain-draft-slot` remains pointer-active, document it as edit-mode behavior and add marker. If changed, require behavior test.

## Raw JSON user-facing summary guard

Current accepted marker:

- `data-logic-chain-vbd-native-json-not-primary-summary`
- `nativeTriggerJson` count currently 2 in frontend source.

Guard:

- User-facing summaries for VBD native triggers must remain Chinese readable rows.
- `nativeTriggerJson` may exist only as secondary/debug/raw detail, not primary summary text.
- New raw JSON summary labels should fail unless explicitly behind advanced/debug disclosure.

## node --check guard

Existing docs already require:

```powershell
node --check build\tmp\webadmin-app.js
```

Plan:

- Export `WebAdminFrontendScripts.appJs()` to `build/tmp/webadmin-app.js` inside guard.
- Run local Node if present; if Node missing in CI/dev environment, guard should fail with clear message because current render smoke already depends on Node.
- Keep `data-logic-chain-no-inline-js-syntax-break` marker.

## Logic Chain marker guard

Existing 9.1 markers in `StabilizationGuardTest` should remain, including:

- `data-logic-chain-global-editor-completion-9-1`
- `data-logic-chain-save-prevalidated-sequential`
- `data-logic-chain-no-freeform-graph-save`
- `data-logic-chain-world-backed-objects-require-client-assisted-draft`
- `data-logic-chain-protected-draft-registry-required`
- `data-logic-chain-vbd-trigger-stable-identity`
- `data-logic-chain-vbd-trigger-no-duplicate-card`
- `logic_chain_action_target_multi_write_conflict`
- `logic_chain_target_lock_preflight_validation`

Add marker guard for 9.1.1:

- `data-code-health-guard-9-1-1`
- `data-webadmin-frontend-bundle-entry-only`
- `data-logic-chain-render-perf-markers`
- `data-logic-chain-state-ownership-boundary`
- `data-no-beforevxx-patch-stacking`

## State ownership guard

Hard/soft checks to prevent state coupling regressions:

| Guard | Mode | Check |
| --- | --- | --- |
| selected node fallback | warning first, hard after behavior fix | no new code path uses `selectedNodeId` as save target, capture writeback target, or overlay identity fallback |
| VBD existing fallback | warning | existing `logicChainFindDraftTargetNode` selected fallback remains documented until dedicated bugfix; any change requires behavior test |
| capture writeback identifiers | hard once API split starts | capture writeback must carry stable target id / `sourceNodeId` / `triggerKey` / `draftSessionId` / `editLockId` and backend consistency validation |
| `connectionMode` payload | hard | `connectionMode` / `previewCandidate` cannot appear in save payload |
| `connectionMode` dirty | marker first | record current dirty behavior; changing dirty/draftRevision semantics requires dedicated behavior-fix tests |
| `_pendingDelete` payload leak | hard | `_pendingDelete` must not be serialized in save payload |
| protected draft authority | hard | committed/cancelled/expired state must come from backend registry event/result, not frontend-only inference |

Suggested negative scans:

- `selectedNodeId` near `savePayload`, `captureWriteback`, `triggerKey` assignment or `sourceNodeId` fallback.
- `_pendingDelete` inside JSON payload builders.
- `connectionMode` inside payload serialization.
- capture writeback endpoints missing `draftSessionId` or `editLockId`.

## Backend save boundary guard

| Guard | Mode | Required invariant |
| --- | --- | --- |
| mixed metadata/typed write | hard | typed drafts + `channelMetadataDrafts` stay fail-closed unless a dedicated implementation changes and tests the boundary |
| node delete mixed-write | hard | single node delete and mixed target conflicts remain fail-closed |
| failure preserves editor lock | hard | recoverable save failure keeps draft/editor lock where current behavior does |
| protected draft commit failure | hard | commit failure returns to configuring/recoverable state when current behavior does |
| rollback scope | docs/test marker | no test or doc may claim complete cross-store atomic rollback |
| fake `WebAdminMapServer` reference | hard docs/source grep | use actual `MapServer` / `MapDataStore` names |
| route/snapshot adapter | hard if extracted | write-before snapshot, audit, realtime and CSRF/same-origin remain before mutation |

Phase 5 implementation note:

- `WebAdminLogicChainEditorService.saveDraft()` is reduced to a facade into `LogicChainDraftSaveCoordinator`; the rest of the public service API remains unchanged.
- `LogicChainDraftSaveCoordinator` owns the frozen save flow only: preflight, editor lock, fingerprint, validation, mixed-write fail-closed guards, typed execution, channel metadata tail write and successful editor lock release.
- `LogicChainDraftOperationPlanner` centralizes operation presence checks and the typed/channel metadata boundaries without executing store mutation or introducing freeform graph save.
- `LogicChainTypedWriteExecutor` preserves the original typed order: new nodes -> action append -> existing node edits -> action edits -> action deletes -> action reorders -> node deletes.
- VBD/world device/RegionController/action/node delete typed writes still delegate to the existing adapter methods; rollback remains partial and must not be documented as a full cross-store atomic transaction.
- Guard now ratchets `WebAdminLogicChainEditorService.java` after the coordinator split and requires coordinator/planner/executor markers plus a planner ordering test.

## Performance marker baseline guard

Initial markers:

- `renderLogicChainViewer.total`
- `logicChainRenderedGraphWithDraftOverlay`
- `logicChainLayoutGraphV2`
- `logicChainLayoutWithDraft`
- `logicChainAdjustSavedProducerLayout`
- `logicChainMindMap`
- `logicChainMinimap`
- `logicChainDraftDiffBanner`
- `renderIcons`

Guard mode:

- Phase 1: doc/test registry or existing marker presence only; do not add new `src/main` performance markers just to satisfy this guard.
- Phase 6: synthetic graph timing trend warning.
- Later: hard fail only for extreme regressions after enough baseline data.

Phase 6 implementation note:

- Guard scope marker is `phase6-logic-chain-performance-baseline`.
- `WebAdminPerformanceBaselineGuardTest` now runs a synthetic Logic Chain render harness without Minecraft, MCP scenario or screenshots.
- Hard fail coverage includes marker presence, synthetic render exceptions, node position/class signatures, edge path `d`, `marker-end`, target arrow owner markers, selected/related/dimmed class signatures, selected panel HTML, diff banner HTML, minimap HTML, VBD trigger overlay source markers, SignalListener/Timer pending-delete markers and payload leakage, VBD selected fallback/source priority and minimap cap.
- Timing is still warning/report only. Initial soft targets are small hover/selected around 16 ms and initial/edit/draft/VBD render around 50 ms on the local Node VM harness.
- app.js Phase 6 before baseline is `1,843,648` bytes / SHA-256 `057e7e370d555036aff6d542b3ae4361f82d734b8fa95cf429d4d7ac7425beb3`; current Phase 6 after is `1,846,211` bytes / SHA-256 `474cc3093532f70d78583f996e8d6606496f45db831232f32607439a821a0069` (`+2,563` bytes). Current + 2% is a warning and the existing Phase 3 hard limit remains active.
- Implemented optimization markers are `logicChainRelatedNodeIndex` and `logicChainMinimapKey`. They are intentionally current-render caches, not cross-render layout/draft caches.
- Related-node index is render-local: `logicChainMindMap()` builds it from the current rendered graph edges and passes it through `logicChainPositionedNode()` to `logicChainNodeCard()`. It is never stored on `graph`; hover, selected node, connection mode and draft overlay are still read from current render state.
- Minimap memo stores only `{key, html}` at module scope, not graph references. The key covers only `segments.slice(0,24)` channel id and downstream count; draft, hover, selected, zoom and pan are excluded because they do not affect current minimap semantics.
- Phase 4 local before artifact mismatch is warning-only after Phase 6 only when Phase 6 scope, before artifact bytes/SHA, app.js warning limit and Phase 6 marker checks all match; otherwise it stays a hard failure.
- `BeforeV18+ = 0`, facade boundaries, event router no-growth, pointer-events invariants and raw JSON summary guards remain hard fail.

Phase 7 ratchet implementation note:

- Guard scope marker is `phase7-codebase-health-guard-ratchet`.
- `BeforeV11-17`, `BeforeV13-17` total and all `BeforeV\d+` token growth are now hard fail, not warning. Decreases remain allowed.
- `WebAdminFrontendScripts.java` is still facade-only and carries source marker `data-webadmin-frontend-bundle-entry-only`. Business JS, event handlers and route tables must stay in owning modules.
- New frontend script modules are hard-capped at 800 lines and 400,000 bytes. New frontend style modules are hard-capped at 800 lines and 400,000 bytes.
- Non-grandfathered backend services are hard-capped at 1,000 lines and 160,000 bytes. Guard classes outside `StabilizationGuardTest` are hard-capped at 1,000 lines and 200,000 bytes.
- `WebAdminLogicChainEditorService.java` is ratcheted down by Phase 7.5 to 5,005 lines / 305,271 bytes; `StabilizationGuardTest.java` remains frozen at 12,430 lines / 838,347 bytes.
- Known giant generated JS functions are hard no-growth by line/char baseline: `logicChainExistingDeviceForm`, `logicChainDefaultDraftChannelAnchor`, `realtimeRouteKeysForEvent`, `showLogicChainNewNodeModal`, `showLogicChainPlacedDraftNodeEditModal`, `renderDeviceDetail`, `interactionItemMatcherForm` and `startDeviceConfigEdit`.
- Generated asset ratchets are hard: `app.js` must remain `1,846,211` bytes with SHA-256 `474cc3093532f70d78583f996e8d6606496f45db831232f32607439a821a0069`; `app.css` must remain `123,251` bytes unless a dedicated behavior/DOM-equivalence phase updates the baseline.
- `nativeTriggerJson` occurrence growth is hard fail; raw JSON stays secondary/debug only.
- `DocsConsistencyGuardTest` recursively rejects independent WebAdmin React/Vite/npm/CDN runtime hooks while excluding tooling/build/log directories.
- `WebAdminPerformanceBaselineGuardTest` now runs standalone `node --check`, hard-baselines DOM hashes for pending-delete, VBD fallback, VBD source-priority and minimap-cap scenarios, and source-guards Phase 6 relatedIndex/minimap memo/deferred hover-select-zoom boundaries.

Phase 7.5 if/complexity implementation note:

- `CodeQualityGuardSupport` now classifies Java methods and generated JS functions by `if`, `else if`, `switch/case`, `.closest(`, `querySelector(`, inline handler and listener counts.
- `CodeQualityGuardTest` emits Phase 7.5 Top 50 / Top 30 hotspot tables for Java if density, JS if density, JS selector density, interaction routes, render paths and backend validation/save methods.
- Safety-boundary `if` branches are reportable but not treated as bad complexity by themselves; new hard failures remain tied to no-growth ratchets, generated asset freeze, new giant handlers, BeforeVxx growth, selector/listener/inline growth and module budgets.
- `WebAdminLogicChainEditorService` is ratcheted down after helper extraction in channel metadata referenced-channel collection. No generated `app.js` / `app.css` baseline changes were made.
- Full audit results and deferred hotspot classifications live in `docs/IF_COMPLEXITY_HOTSPOT_AUDIT_9_1_1.md`.

Synthetic scenarios:

- empty/small graph
- graph with join/timer/state/action
- graph with VBD trigger draft overlay
- edit mode with draft node and action delete/reorder
- hover highlight path
- unsaved diff expanded path

DOM equivalence markers before local optimization:

- node position / size
- edge `d`
- edge `marker-end` and arrow owner
- node/edge selected, related and dimmed classes
- right detail panel HTML
- diff banner HTML
- minimap HTML and `segments.slice(0,24)` cap
- modal body scroll, focus and caret markers
- VBD trigger overlay source/target ids and `triggerKey`

## Docs update guard

Docs consistency checks:

- README stable baseline should match latest current context or explicitly state historical baseline.
- Every `docs/*_CURRENT_CONTEXT.md` linked from README should exist.
- 9.1.1 implementation phases that change guard thresholds must update `CODE_QUALITY_GUARD_PLAN_9_1_1.md` or successor current context.
- New WebAdmin write capability must update current context / capability matrix / help text.

Phase 1 consistency baseline:

- README stable version should be `v1.68.1-codebase-health-audit`.
- `docs/CODEBASE_HEALTH_GUARD_BASELINE_9_1_1_CURRENT_CONTEXT.md` records the Phase 1 guard baseline.
- The older audit docs may still mention `v1.68.0` as the historical audit input baseline when clearly scoped as history.

## Acceptance criteria for 9.1.1 guard work

- New guard classes exist and do not further bloat `StabilizationGuardTest`.
- Existing guard suite still passes.
- `WebAdminFrontendScripts.java` is protected from further business logic growth.
- New `BeforeVxx` wrappers fail.
- `node --check` is a hard gate.
- app.js/app.css bytes are tracked.
- event handler complexity is tracked.
- pointer-events invariants are tracked.
- performance marker names are stable.
- docs consistency guard enforces README/current context mismatch as a hard failure for Phase 1 baseline files.

## Overall 9.1.1 acceptance checklist

- Behavior freeze matrix remains unchanged or every behavior change is split into an explicitly approved behavior-fix prompt.
- Frontend split keeps `/assets/app.js` / `/assets/app.css` output route shape and facade contract.
- Escaping contract has guard coverage before inline handler / modal builder edits.
- Logic Chain render split has DOM equivalence baseline before local hover/click/zoom optimization.
- State ownership guard covers selected fallback, capture writeback identifiers, `connectionMode`, `_pendingDelete` and protected draft authority.
- Backend split preserves CSRF/same-origin, edit lock, expectedFingerprint, write-before snapshot, audit, realtime and fail-closed conflicts.
- README / current context / capability matrix are consistent or documented as intentionally historical.
- Required validation commands for the touched phase pass.

## Validation commands

For docs-only audit:

```powershell
git diff --check
git status --short --branch
```

Because new docs are untracked during this audit, `git diff --check` does not inspect them until staged. Checkpoint prompt should run `git diff --cached --check` after explicit staging, or use an equivalent untracked markdown whitespace check before staging.

For guard implementation:

```powershell
.\gradlew.bat testClasses
.\gradlew.bat codeQualityGuardTest --rerun-tasks
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
git diff --check
```

For full checkpoint after code changes when a prompt explicitly asks for the broader release-style pass:

```powershell
.\gradlew.bat clean build
.\gradlew.bat codeQualityGuardTest --rerun-tasks
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
git diff --check
```

Phase 7 B5 intentionally uses the targeted command set above and does not run `clean build`; Phase 9 owns the final release-style `clean build`.

Only run MCP build/test when the current phase actually touches MCP:

```powershell
cd tools\tzz-test-mcp
npm run build
npm test
```
