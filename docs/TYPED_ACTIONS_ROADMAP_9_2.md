# TZZ Mod 9.2 Typed Actions Roadmap

## Baseline

| Item | Value |
| --- | --- |
| Start tag | `v1.68.4-docs-accuracy` |
| Start commit | `f8fa12c6e5c20ca82a3d5ea0a87f24d26462fb4c` |
| Runtime lineage | `v1.68.3-real-performance-deep-simplification` |
| Development branch | `feature/typed-actions-unified-editor-9-2` |

This roadmap is for Resource Graph typed action configuration. It is not a 10.x game program roadmap.

## Global Non-Goals

- no GameController;
- no MissionSystem;
- no PhaseController;
- no if/else runtime;
- no typed action sequence runtime;
- no full Rich Text Builder;
- no React / Vite frontend migration;
- no VBD native trigger action-owner expansion;
- no runtime action execution semantic changes.

## Phase Plan

| Phase | Goal | Deliverables | Default validation | Stop conditions |
| --- | --- | --- | --- | --- |
| Phase 0 | Preflight audit | Five 9.2 docs and Obsidian index | `git diff --check` if docs-only | Any doc claims runtime/editor/validation is already unified when it is not. |
| Phase 1 | Action schema registry | Static immutable schema metadata for existing action types; implemented in `com.zcpu.tzzmod.action.schema` | `testClasses`, `codeQualityGuardTest`, `git diff --check` | Registry changes runtime execution or save payloads. |
| Phase 2 | Capability matrix + backend validation | Owner/bucket capability and authoritative validation implemented for current owners | Full guard set | Timer/common validation inconsistency not resolved or compatibility breaks. |
| Phase 3 | Unified action editor | Schema-driven WebAdmin field renderer with owner adapters | Full guard set, node syntax checks via guard | Editor breaks draft, dirty confirm, lock, picker or realtime semantics. |
| Phase 4 | Summary / diff / snapshot / audit | Human Chinese action summaries for cards, diff, snapshot and audit where compatible | Full guard set | Summary work changes snapshot storage or audit/result payload shape. |
| Phase 5 | Owner integration / migration guard | Existing owners wired through schema/capability/summary paths | Full guard set | Old JSON configs, order, index, delete or reorder semantics break. |
| Phase 6 | Help / docs / Obsidian / guard | Help center, docs, Obsidian and consistency guards | Full guard set | Docs diverge from registry / matrix. |
| Phase 7 | Final validation / release | Feature validation, merge and release tag | Full feature and master validation | Any runtime semantic risk remains unproven. |

## Phase 1 Guard Targets

- every existing `ActionType` has exactly one schema;
- schema ids are unique;
- field ids are unique per schema;
- required/default semantics are valid;
- registry is immutable / not runtime-polluted;
- schema has Chinese label and help text.

Implemented guard entry: `src/test/java/com/zcpu/tzzmod/action/schema/ActionSchemaRegistryTest.java`, invoked from `CodeQualityGuardTest`. It also checks strict unknown id lookup, current owner enum boundaries and that the schema package does not import runtime/service dependencies.

## Phase 2 Guard Targets

- owner supports action type checks are backend-authoritative through `ActionCapabilityMatrix`;
- unsupported owner/action combinations fail closed in `ActionValidationService`;
- Timer buckets are brought under the same common field validation standard for command / message / sound / signal / state_variable / timer actions;
- condition group compatibility is checked by owner/bucket/action target through an injected WebAdmin gate validator;
- legacy error code and `WebAdminWriteResult` compatibility is preserved, including Timer's `timer_action_type_invalid` mapping.

## Phase 3 Guard Targets

- schema-driven field render marker;
- channel picker, state variable picker and condition group picker still work;
- owner capability filtering is visible and backend-matched;
- validation failure preserves draft input;
- no raw JSON primary editor;
- no growth in giant script files.

Implemented Phase 3 frontend modules:

- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminActionSchemaScripts.java`
- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminActionFieldRenderScripts.java`
- `src/test/java/com/zcpu/tzzmod/stabilization/WebAdminActionEditorFrontendGuardTest.java`

Phase 3 wires the existing ActionRelay, SignalListener, RegionController, Timer and Logic Chain action editors through schema/capability helpers and a common value-field renderer. Owner services, write APIs, save payloads, edit locks, expected fingerprints, audit, realtime events and runtime execution remain owned by the existing modules.

Phase 3 guard entry: `WebAdminActionEditorFrontendGuardTest`, invoked from `CodeQualityGuardTest`. It checks schema export, owner matrix export, owner fail-closed filtering, Logic Chain draft action renderer use, condition picker markers, draft-preservation markers and Java matrix consistency. Bundle/performance ratchets now also cover the Phase 3 app.js output.

## Phase 4 Guard Targets

- every action type has a Chinese human summary;
- Logic Chain action cards and owner action lists use summaries;
- unsaved diff uses summary-first rows where possible without changing dirty comparison;
- snapshot / operation diff shows compatible action-index summaries without changing snapshot storage;
- audit summaries keep command redaction.

Implemented Phase 4 modules:

- `src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionSummaryService.java`
- `src/main/java/com/zcpu/tzzmod/webadmin/WebAdminActionSummaryScripts.java`
- `src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionSummaryServiceTest.java`
- `src/test/java/com/zcpu/tzzmod/stabilization/WebAdminActionSummaryGuardTest.java`

Phase 4 also updates Timer audit summary lists, `WebAdminSignalService` downstream signal discovery, Logic Chain diff summary rows, snapshot operation diff rows and app.js / DOM ratchet baselines. It does not change runtime action execution, owner action order, save payloads, WebAdmin API shape or snapshot storage shape.

## Phase 5 Guard Targets

- old fixtures validate through the new schema / capability model;
- new editor draft maps back to equivalent old `ActionConfig`;
- same-index edit semantics remain;
- delete and reorder are owner-local and bucket-local;
- no supported action is hidden and no unsupported action is shown.

Implemented Phase 5 guard:

- `src/test/java/com/zcpu/tzzmod/stabilization/WebAdminActionOwnerMigrationGuardTest.java`

Phase 5 is a migration/equivalence guard checkpoint. It compares the WebAdmin owner export from `WebAdminActionSchemaScripts` with `ActionCapabilityMatrix`, executes the frontend owner/type option helpers to prove no supported action is hidden and no unsupported action is shown, keeps explicit non-owners excluded, validates old-style action entries and old JSON store fixtures for every current owner/action pair, proves frontend editor draft payloads normalize to the old runtime `ActionConfig`, rejects unknown action ids fail-closed before legacy fallback, checks Timer invalid-type writes do not persist fallback configs, and locks Logic Chain action append/edit/delete/reorder payloads to owner/bucket/index/fingerprint/lock fields without display-only summary fields.

Phase 5 does not add action types, owners, runtime behavior, WebAdmin write APIs, save payload fields or snapshot storage.

## Manual Acceptance Recommendation

If implementation stays metadata/editor/validation/summary only and does not change runtime action execution, large Minecraft scenario testing is not required. A final human smoke should still open WebAdmin and check:

- SignalListener action add / edit / delete / reorder where available;
- ActionRelay action add / edit / delete / reorder;
- Region enter / exit / stay action editing;
- Timer start / tick / complete / cancel action buckets;
- save / cancel draft flows;
- for Phase 4+ or final 9.2 acceptance, action summary and unsaved diff text.

## Deferred

- Rich Text Builder / tellraw / title / actionbar component editor;
- GameController / MissionSystem / PhaseController;
- branch / if-else runtime;
- typed action sequence runtime;
- world mutation actions;
- automatic datapack-to-mod conversion;
- full Scratch-like editor.
