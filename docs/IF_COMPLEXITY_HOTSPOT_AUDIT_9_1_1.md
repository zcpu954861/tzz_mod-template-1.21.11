# 9.1.1 Phase 7.5 if / complexity / hotspot audit

## Scope

Phase 7.5 is merged into Phase 7 before the Phase 8 Obsidian work. It audits if density, selector density, event routing, render hotspots and backend save/validation complexity without changing WebAdmin UI behavior, runtime semantics, WebAdmin API shape, Logic Chain layout output, save payloads or error codes.

This audit does not try to delete every `if`. Safety-boundary `if` branches remain explicit and fail-closed. Cleanup is limited to behavior-equivalent helper extraction and guard/report hardening that can be verified by existing unit/DOM/hash guards.

## Metrics methodology

`CodeQualityGuardTest` now emits machine-readable Phase 7.5 tables:

- `phase75.java.if_density.top.*`: Top 50 Java methods by if density.
- `phase75.js.if_density.top.*`: Top 50 generated app.js functions by if density.
- `phase75.js.selector_density.top.*`: Top 50 generated app.js functions by `.closest(` + `querySelector(` density.
- `phase75.hotspot.interaction.top.*`: Top 30 high-frequency interaction/event functions.
- `phase75.hotspot.render.top.*`: Top 30 render/layout/draft/minimap hotspots.
- `phase75.hotspot.backend.top.*`: Top 30 WebAdmin backend validation/save methods.

Each row includes file, function, line, chars, lines, `if`, `elseIf`, `switch`, `case`, `.closest(`, `querySelector(`, inline handler count, `addEventListener` count, high-frequency flag, safety-boundary flag, category and recommendation.

Categories:

| Code | Meaning | Phase 7.5 action |
| --- | --- | --- |
| A | Safety boundary | Keep explicit fail-closed checks |
| B | Routing complexity | Defer unless event order is fully guarded |
| C | Render hotspot | Only use render-local index/memo with DOM hash proof |
| D | Draft/state coupling | Defer unless ownership is already isolated |
| E | Patch stacking | Defer unless order can be frozen |
| F | UI builder bloat | Pure helper extraction only when output-equivalent |
| G | Deferred | Record for later focused work |

## Before / after summary

| Metric | Before Phase 7.5 | After Phase 7.5 | Result |
| --- | ---: | ---: | --- |
| `WebAdminLogicChainEditorService.java` lines | 5,012 | 5,005 | Ratcheted down |
| `WebAdminLogicChainEditorService.java` bytes | 305,815 | 305,271 | Ratcheted down |
| Guard metrics collected | 649 | 895 | Phase 7.5 tables added |
| Generated `app.js` bytes | 1,846,211 | 1,846,211 | Unchanged |
| Generated `app.css` bytes | 123,251 | 123,251 | Unchanged |
| `BeforeV\d+` total | 184 | 184 | No growth |
| `BeforeV18+` | 0 | 0 | No new patch stack |
| `document.addEventListener` | 22 | 22 | No growth |
| `addEventListener(` | 70 | 70 | No growth |
| `.closest(` generated app.js actual / hard ceiling | 33 / 73 | 33 / 73 | No growth |
| `querySelector(` source count | 71 | 71 | No growth |
| Known inline handler total | 604 | 604 | No growth |

## Top hotspot snapshot

The full Top 50 / Top 30 tables are emitted by `codeQualityGuardTest`. Current leading rows:

| Table | Leading row | Classification | Phase 7.5 action |
| --- | --- | --- | --- |
| Java if density | `WebAdminLogicChainVbdOverlayScripts.appJs`, line 11, 55 lines, 98 `if` | C render hotspot | Deferred because generated `app.js` is exact-frozen and VBD overlay ordering is sensitive |
| JS if density | `logicChainNodeTypeLabel`, line 6817, 27 `if` | G deferred | Deferred as generated one-line helper debt; no behavior guard justifies rewriting |
| JS selector density | `handleHelpCenterDelegatedClick`, line 4799, 10 `.closest(`, 11 `if` | B routing complexity | Deferred to preserve delegated click order |
| Interaction hotspot | `realtimeRouteKeysForEvent`, line 832, 129 `if` | B routing complexity | Grandfathered no-growth; table-driven rewrite needs route-key golden tests |
| Render hotspot | Emitted by `phase75.hotspot.render.top.*` with explicit render/layout/draft/minimap/VBD names | C / F / G by row | Deferred unless DOM hash proof exists |
| Backend validation/save | `WebAdminTemplateService.buildApplyPlan`, line 430, 171 lines, 25 `if` | F UI builder bloat | Deferred because template apply semantics are outside Phase 7 scope |

## Processed hotspot

| Hotspot | Category | Change | Equivalence proof |
| --- | --- | --- | --- |
| `WebAdminLogicChainEditorService.channelMetadataDraftReferencedChannels` | Backend complexity / safety-aware helper extraction | Extracted repeated signal-action and normalized-channel append logic into local helpers. `LinkedHashSet` order, `channelRef`, `SignalChannel.normalize`, draft edge channel source and channel metadata validation entry points are unchanged. | `testClasses` passed; `codeQualityGuardTest` passed; file line/byte ratchet moved down to 5,005 / 305,271. |

The original method was identified in Round 1 as 68 lines / 20 `if` branches. After helper extraction it no longer appears in the Top 50 if-density table; the remaining helper `addExistingNodeEditReferencedChannels` is 15 lines / 5 `if` branches.

## Kept safety if

These checks are intentionally not optimized away:

- CSRF / same-origin / permission preflight.
- Edit lock and expected fingerprint checks.
- Protected draft actor, edit lock, target lock and session validation.
- Malformed payload and typed save validation branches.
- Mixed-write fail-closed guards in Logic Chain save.
- Per-typed-write fail-fast checks in `LogicChainTypedWriteExecutor`.
- Runtime condition gate missing / disabled / invalid / incompatible / error branches.

Reason: these branches define observable safety behavior, error codes, Chinese validation text, save conflict recovery or runtime no-side-effect guarantees.

## Deferred hotspots

| Hotspot | Reason | Later phase |
| --- | --- | --- |
| Global / Logic Chain / VBD delegated event route cleanup | Capture/bubble order, outside-close timing and VBD retry semantics need browser-level equivalence coverage | Focused event-router cleanup |
| `realtimeRouteKeysForEvent` table rewrite | Needs golden route-key matrix for every event type | Dedicated realtime route guard phase |
| Hover class-only, selection panel-only and zoom transform-only updates | Current behavior affects node/edge classes, arrow ownership, marker-end, panel HTML and toolbar state | Focused interaction performance phase |
| Draft diff memoization | No single reliable draft revision key covers pending delete, reorder, channel metadata and VBD trigger draft | Draft fingerprint/memo phase |
| VBD overlay pipeline consolidation | Source-card priority, selected fallback, capture writeback and trigger identity are sensitive | VBD overlay behavior phase |
| Large modal/card UI builders | Generated `app.js` exact freeze blocks mechanical JS helper extraction unless DOM output is re-baselined | Dedicated UI builder split phase |

## Phase 8 Obsidian notes

Record in Obsidian that Phase 7.5 established a complexity audit vocabulary, not just file-size ratchets. The important maintenance rule is: safety `if` is preserved; routing/render/UI-builder debt is only reduced when equivalent behavior is proven by guard snapshots.

## Validation

Phase 7.5 uses the Phase 7 checkpoint validation set:

```powershell
.\gradlew.bat testClasses
.\gradlew.bat codeQualityGuardTest --rerun-tasks
.\gradlew.bat stabilizationGuardTest --rerun-tasks
.\gradlew.bat localTestMcpGuardTest --rerun-tasks
git diff --check
```

MCP npm build/test is only required if `tools/tzz-test-mcp` changes.
