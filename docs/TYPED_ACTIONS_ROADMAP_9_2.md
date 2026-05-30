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
| Phase 1 | Action schema registry | Static immutable schema metadata for existing action types | `testClasses`, `codeQualityGuardTest`, `git diff --check` | Registry changes runtime execution or save payloads. |
| Phase 2 | Capability matrix + backend validation | Owner/bucket capability and authoritative validation | Full guard set | Timer/common validation inconsistency not resolved or compatibility breaks. |
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

## Phase 2 Guard Targets

- owner supports action type checks are backend-authoritative;
- unsupported owner/action combinations fail closed;
- Timer buckets are brought under the same common field validation standard or explicitly covered by equivalent typed validation;
- condition group compatibility is checked by owner/bucket/action target;
- legacy error code and `WebAdminWriteResult` compatibility is preserved.

## Phase 3 Guard Targets

- schema-driven field render marker;
- channel picker, state variable picker and condition group picker still work;
- owner capability filtering is visible and backend-matched;
- validation failure preserves draft input;
- no raw JSON primary editor;
- no growth in giant script files.

## Phase 4 Guard Targets

- every action type has a Chinese human summary;
- Logic Chain action cards use summaries;
- unsaved diff uses summaries where possible;
- snapshot / operation diff can show compatible action summaries without changing snapshot storage;
- audit summaries keep command redaction.

## Phase 5 Guard Targets

- old fixtures validate through the new schema / capability model;
- new editor draft maps back to equivalent old `ActionConfig`;
- same-index edit semantics remain;
- delete and reorder are owner-local and bucket-local;
- no supported action is hidden and no unsupported action is shown.

## Manual Acceptance Recommendation

If implementation stays metadata/editor/validation/summary only and does not change runtime action execution, large Minecraft scenario testing is not required. A final human smoke should still open WebAdmin and check:

- SignalListener action add / edit / delete / reorder where available;
- ActionRelay action add / edit / delete / reorder;
- Region enter / exit / stay action editing;
- Timer start / tick / complete / cancel action buckets;
- save / cancel draft flows;
- action summary and unsaved diff text.

## Deferred

- Rich Text Builder / tellraw / title / actionbar component editor;
- GameController / MissionSystem / PhaseController;
- branch / if-else runtime;
- typed action sequence runtime;
- world mutation actions;
- automatic datapack-to-mod conversion;
- full Scratch-like editor.
