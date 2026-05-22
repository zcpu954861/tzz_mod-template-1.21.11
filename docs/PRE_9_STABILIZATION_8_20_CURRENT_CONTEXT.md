# 8.20 Pre-9 Stabilization / Hardening Current Context

8.20 is the final 8.x stabilization pass before 9.x planning. It does not implement the abandoned WebAdmin visual system implementation and does not start the 9.x game-program layer.

Current baseline:

- Stable tag: `v1.65.0-webadmin-visual-system-design-reference`
- Baseline commit: `daea0557`
- Working branch: `feature/pre-9-stabilization-hardening`

## Goal

The goal is to make the 8.x platform safer as a base for 9.x:

- tighten Snapshot / Rollback recovery visibility
- connect Help and Doctor to Snapshot, Template, Timer, Join, Condition and StateVariable issues
- fill low-risk operationDiff coverage gaps in existing WebAdmin write routes
- preserve Logic Chain Editor controlled-edit boundaries
- add guard coverage for abandoned visual implementation and 9.x out-of-scope systems
- update README and capability documents so the next stage starts from accurate context

## Implemented Stabilization

Snapshot / Rollback:

- Route-level auto snapshots that previously discarded the created record now keep the `WebAdminSnapshotAutoResult` and call `annotateAutoSnapshotAfterWrite` after successful writes.
- Operation diff annotation now covers device metadata/basic/extended config, ActionRelay actions, interaction item matcher, logic-chain metadata create/update/delete, VBD delete/native trigger and RegionController create/update/delete/action add/update/delete/clear, in addition to the previously covered 8.18 paths.
- Snapshot page exposes generic degraded/bad package warnings, a selected-hidden-by-filter notice, and `snapshot.rollback` help links.
- Rollback dry-run operation labels accept both `create/update/delete` and `created/updated/deleted`.

Help / Doctor:

- Help catalog now includes `snapshot.rollback`, Snapshot/Rollback troubleshooting, Snapshot/Rollback glossary terms and a dry-run rollback example.
- Page-level help maps `配置时间轴` / Snapshot / Rollback pages to the Snapshot help topic.
- Doctor object labels and filters cover Timer, Signal Join, ConditionGroup, StateVariable, action buckets, Snapshot and Template.
- Doctor now emits degraded Snapshot and Template diagnostics with navigation to `#/snapshots` and `#/templates`.
- Signal Join diagnostics can navigate to `#/signal-joins/{id}` when the issue text identifies a Join.
- Timer action buckets are included in condition/action gate Doctor scans as `TIMER_ACTION`.

Condition / Timer / State:

- Timer action filters include `TIMER_START` and `TIMER_CANCEL`.
- `timer_cancel` exposes its missing-target behavior in the action editor.
- `clear_variable` hides create-if-missing because clear is no-op success for missing variables.

Logic Chain / Template:

- Logic Chain existing-node and action edit validation reports actual `existingNodeEdits[index]` / `actionEdits[index]` field paths, so multi-edit validation no longer points every error at index 0.
- Logic Chain editor frontend helpers that had accumulated duplicate overwritten definitions are reduced to a single final definition for the controlled-edit/connection overlay path and guarded against reappearing.
- Template docs now describe 8.18 auto-snapshot protection while keeping template-service transaction rollback deferred.

## Explicit Non-Goals

8.20 does not implement:

- WebAdmin visual system implementation
- dark/light theme implementation or theme toggle
- large UI refactor
- GameController
- MissionSystem
- PhaseController
- full Logic Chain Editor
- old node arbitrary move/delete/reorder
- old action arbitrary delete/reorder
- Scratch editor
- if / else runtime
- Git branch / merge / rebase
- remote backup or cross-world migration
- new runtime semantics
- new `ActionType`
- new `ConditionNodeType`

## 8.19 Design Reference Status

The 8.19 visual system design reference remains as documentation only:

- `docs/WEBADMIN_VISUAL_SYSTEM_8_19_SELECTED_DIRECTION.md`
- `docs/WEBADMIN_VISUAL_SYSTEM_UIUX_PRO_MAX_8_19.md`
- `docs/WEBADMIN_VISUAL_SYSTEM_UIUX_PRO_MAX_SAMPLES_V2_8_19.md`
- `docs/visual-system-8-19/uiux-pro-max-v2/**`

These files are not an 8.20 implementation. There is no `WEBADMIN_VISUAL_SYSTEM_IMPLEMENTATION_8_20` context or theme-system code in production.

## 9.x Entry Boundary

9.x may start from this stable 8.x base and separately design:

- 9.0 GameController / Game Program Foundation
- MissionSystem / PhaseController
- typed high-level game program calls
- visual logic/program editor
- if/else branching
- vanilla command-like effects as typed visual blocks

Those are planning targets only. 8.20 keeps runtime behavior unchanged.

## Validation

Required validation for this stage:

- `.\gradlew.bat testClasses`
- JS export plus `node --check build\tmp\webadmin-app.js`
- `cd tools\tzz-test-mcp; npm run build; npm test`
- `.\gradlew.bat clean build`
- `.\gradlew.bat stabilizationGuardTest --rerun-tasks`
- `.\gradlew.bat localTestMcpGuardTest --rerun-tasks`
- `git diff --check`

Do not start Minecraft, run MCP scenarios, generate screenshot matrices, commit, push, merge or tag in this implementation prompt.
