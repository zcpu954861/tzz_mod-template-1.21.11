# Pre-9 Stabilization Capability Matrix 8.20

8.20 is a hardening pass over 8.10-8.18. It closes small integration gaps and documents the 9.x boundary without adding a new gameplay runtime.

| Area | Status | Notes |
| --- | --- | --- |
| Abandoned visual implementation check | Guarded | 8.19 design reference docs remain; 8.20 visual system implementation, theme toggle and dark/light theme code are not present. |
| Snapshot auto operationDiff coverage | Hardened | Device metadata/basic/extended, ActionRelay actions, interaction matcher, logic-chain metadata, VBD delete/native trigger and RegionController writes now annotate the protecting auto snapshot after successful writes. |
| Snapshot degraded warnings | Hardened | Page/detail warnings use `data-snapshot-degraded-warning` and `data-snapshot-bad-package-warning`; raw parser details stay server-side. |
| Snapshot selected hidden by filter | Hardened | `data-snapshot-selected-hidden-by-filter` tells the user when the route-selected snapshot is excluded by active filters. |
| Rollback operation labels | Hardened | Dry-run labels cover `create/update/delete` and `created/updated/deleted`. |
| Snapshot Help entry | Implemented | Help topic `snapshot.rollback`, troubleshooting and glossary terms explain Snapshot, Rollback, pre_rollback and operationDiff. |
| Snapshot Doctor entry | Implemented | Degraded manifest diagnostics appear as `SNAPSHOT` issues with `#/snapshots` navigation. |
| Template Doctor entry | Implemented | Degraded user template store appears as a `TEMPLATE` issue with `#/templates` navigation. |
| Signal Join Doctor navigation | Hardened | Join issue text can resolve to `SIGNAL_JOIN` and navigate to `#/signal-joins/{id}`. |
| Timer action gate Doctor | Hardened | Timer `onStart/onTick/onComplete/onCancel` action buckets are scanned as `TIMER_ACTION` condition/state action bindings. |
| Doctor object filters | Hardened | Frontend labels and filters include Timer, Signal Join, ConditionGroup, StateVariable, action buckets, Snapshot and Template. |
| Timer action editor | Hardened | `timer_cancel` exposes missing-target behavior; `timer_start` keeps start policy/duration controls. |
| State action editor | Hardened | `clear_variable` hides create-if-missing because missing variable clear is no-op success. |
| Logic Chain multi-edit validation | Hardened | Validation field paths use real `existingNodeEdits[index]` and `actionEdits[index]` instead of hardcoded index 0. |
| Logic Chain frontend helper de-dup | Hardened | Controlled-edit and connection overlay helpers keep one final definition; guard prevents overwritten duplicate functions from returning. |
| Template recovery docs | Updated | Template apply is protected by Snapshot auto capture; in-service transactional rollback remains deferred. |
| 9.x boundary docs | Added | GameController, MissionSystem, PhaseController, if/else branching and visual program editor are recorded as future 9.x directions only. |

## Guard Markers

8.20 guard requires:

- `docs/PRE_9_STABILIZATION_8_20_CURRENT_CONTEXT.md`
- `docs/PRE_9_STABILIZATION_CAPABILITY_MATRIX_8_20.md`
- `data-snapshot-degraded-warning`
- `data-snapshot-bad-package-warning`
- `data-snapshot-selected-hidden-by-filter`
- `data-snapshot-help-topic="snapshot.rollback"`
- `snapshotRollbackOperationLabel`
- `snapshot.rollback`
- `trouble.snapshot-degraded`
- `trouble.rollback-operation-diff`
- `trouble.snapshot-retention`
- `TimerStore.getSnapshot(server)`
- `TIMER_ON_START_ACTION`
- `TIMER_ON_TICK_ACTION`
- `TIMER_ON_COMPLETE_ACTION`
- `TIMER_ON_CANCEL_ACTION`
- `TIMER_ACTION`
- `data-timer-cancel-missing-behavior-field`
- `data-state-action-clear-no-create-if-missing`
- `existingNodeEdits[` and `actionEdits[` dynamic index validation

## Negative Guards

8.20 guard rejects:

- `TZZ_WEBADMIN_THEME_STORAGE_KEY`
- theme toggle / dark-light theme implementation markers
- 8.20 visual system implementation docs or code markers
- `GameController`, `MissionSystem`, `PhaseController` source implementations
- full Logic Chain Editor / Scratch editor / if-else runtime source implementations
- Snapshot branch / merge / rebase classes or UI markers
- new `ActionType` values
- new `ConditionNodeType` values
- discarded route-level `autoSnapshotBeforeWrite(...)` calls that are not captured for annotation

## Deferred To 9.x Or Later

- GameController / Game Program Foundation
- MissionSystem / PhaseController
- if/else branching
- visual logic/program editor
- direct typed module calls
- vanilla command-like effects as typed visual blocks
- Git-like branch / merge / rebase
- remote backup / cross-world migration
- full field-level diff rewrite
- template marketplace and automatic world entity copy

## Validation Matrix

| Command | Required |
| --- | --- |
| `.\gradlew.bat testClasses` | Yes |
| `node --check build\tmp\webadmin-app.js` | Yes, after JS export |
| `cd tools\tzz-test-mcp; npm run build; npm test` | Yes |
| `.\gradlew.bat clean build` | Yes |
| `.\gradlew.bat stabilizationGuardTest --rerun-tasks` | Yes |
| `.\gradlew.bat localTestMcpGuardTest --rerun-tasks` | Yes |
| `git diff --check` | Yes |

No checkpoint, commit, push, merge or tag is part of this prompt.
