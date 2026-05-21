# 8.18 Snapshot Timeline / Rollback Graph Current Context

8.18 adds a WebAdmin configuration snapshot timeline with manual save points, auto snapshots before important writes, resource-level diff, dry-run rollback and confirmed rollback apply.

This is a configuration recovery feature, not a Git implementation and not a world backup system.

Guard review keywords: snapshot schema, storage path, timeline UI, rollback dry-run, rollback apply, permission/security, retention, covered write operations.

## Scope

Implemented:

- Snapshot package / manifest schema under the world-scoped WebAdmin directory.
- Manual snapshots with title, note and tags.
- Auto snapshots before key WebAdmin write operations.
- Protected `pre_rollback` snapshot before rollback apply.
- Snapshot list and detail APIs:
  - `GET /api/webadmin/snapshots`
  - `GET /api/webadmin/snapshots/{id}`
  - `POST /api/webadmin/snapshots/manual`
  - `POST /api/webadmin/snapshots/{id}/rollback/dry-run`
  - `POST /api/webadmin/snapshots/{id}/rollback/apply`
- Git-graph-like timeline UI at `#/snapshots`.
- Right-side detail rail with metadata, previous-snapshot diff and JSON advanced preview.
- Filters by kind, module, user, created time and search text.
- Filters by resource type, so administrators can narrow the graph to Timer / Join / Condition / State and other changed resources.
- Permission, CSRF, same-origin, edit lock, expectedFingerprint, audit and realtime integration.
- Retention for old auto snapshots while preserving manual and `pre_rollback` snapshots.
- Guard markers:
  - `data-snapshot-timeline-page`
  - `data-snapshot-timeline-graph`
  - `data-snapshot-timeline-not-table`
  - `data-snapshot-node-kind-manual`
  - `data-snapshot-node-kind-auto`
  - `data-snapshot-node-kind-pre-rollback`
  - `data-snapshot-detail-rail`
  - `data-snapshot-detail-diff`
  - `data-snapshot-manual-modal`
  - `data-snapshot-rollback-dry-run-modal`
  - `data-snapshot-rollback-confirm-modal`
  - `data-snapshot-json-preview`

## Storage

World-scoped storage:

```text
<world-save-root>/tzz/webadmin/snapshots/manifest.json
<world-save-root>/tzz/webadmin/snapshots/data/<snapshotId>.json
```

The snapshot store is handled by `WebAdminSnapshotStore`. Snapshot creation is wrapped by `SUPPRESS_AUTO_CAPTURE`, so writing snapshot files does not recursively trigger another auto snapshot.

Bad manifest and bad package files fail closed: they return degraded state and generic Chinese messages instead of saving over corrupted data. Parser and filesystem exception details stay in the server log and are not returned to the snapshot UI.

## Schema

The 8.18 schema uses:

- `SnapshotManifest`
- `SnapshotRecord`
- `SnapshotPackage`
- `SnapshotResource`
- `SnapshotDiffSummary`
- `SnapshotDiff`
- `RollbackPlan`
- `RollbackOperation`
- `SnapshotTrigger`
- `SnapshotKind`

Each record stores snapshot id, sequence, created time, actor, kind, title, note, tags, trigger operation/module/target, previous snapshot id, resource counts, changed resource counts, package fingerprint, storage path and warnings.

Auto snapshot records can also store an `operationDiff`. This is separate from the snapshot-to-previous diff: because auto snapshots are intentionally captured before the write, `operationDiff` records the successful write's before/after resource changes after the write completes. Logic Chain Editor draft saves, Template import/apply, Timer, channel metadata, Signal Join, SignalListener and ConditionGroup writes now persist this operation diff after successful writes so a rename or config edit is shown immediately on the auto snapshot that protected the write. `pre_rollback operation diff` records the rollback operation direction from the current pre-rollback package to the selected rollback target package, so rollback detail shows what the rollback removed, created or updated instead of presenting the protection snapshot's own previous-snapshot diff as the primary change.

## Store Allowlist

Captured store files:

- `web_admin_channel_metadata.json`
- `web_admin_logic_chain_metadata.json`
- `web_admin_device_metadata.json`
- `templates.json`
- `condition_groups.json`
- `condition_runtime_gates.json`
- `tzz_mod/signal_devices.json`
- `signal_joins.json`
- `timers.json`
- `state_variables.json`
- `tzz_mod/signal_listeners.json`
- `tzz_mod/region_controllers.json`

Logical diff resources are extracted for channel metadata, logic-chain metadata, device metadata, templates, condition groups, condition runtime gates, Signal Join, Timer, StateVariable, SignalListener and RegionController entries.

The allowlist excludes:

- `logs/`
- `.codex/`
- `build/`
- `run/`
- `.gradle/`
- `node_modules/`
- runtime history rings
- Condition debugger runtime history
- Timer active runtime state
- Signal Join pending runtime state
- player live inventory
- live world entities

Note: current `state_variables.json` is a configuration/value hybrid from earlier stages. 8.18 captures it as a WebAdmin-visible store, but rollback treats existing StateVariable records as definition rollback: scope / key / type / display name / note can be restored while the current value of an existing variable is preserved when it remains valid for the target type. New or deleted variable definitions still follow the target snapshot definition set. `signal_devices.json` is also a config/runtime hybrid; rollback restores device/VBD config while preserving common `last*` runtime fields for devices that still exist. Snapshots are not live inventory or world-entity backups.

## Auto Snapshot Coverage

covered write operations are listed below for guard and review:

Auto snapshots are created before these WebAdmin write operations:

- Device metadata update.
- Device basic config update.
- Device extended config update.
- ActionRelay action list update.
- Interaction item matcher update.
- Channel metadata update.
- Logic Chain Editor save draft.
- Logic-chain metadata create/update/delete.
- Signal Join create/update/delete and reset/config delete paths.
- Template import and template apply.
- Timer create/update/delete config paths.
- ConditionGroup create/update/delete.
- VirtualBlockDevice delete.
- VirtualBlockDevice native trigger update.
- VirtualBlockDevice object-selection create callback before it writes `signal_devices.json`.
- VirtualBlockDevice container template client save callback before it writes item condition config.
- VirtualBlockDevice single itemSubmit template client save callback before it writes unified requirement config.
- SignalListener create/delete/basic config/action add/update/delete/clear.
- RegionController create/update/delete/action add/update/delete/clear.

Route-level auto snapshots first pass permission, CSRF and same-origin checks. They still run before deeper service validation/fingerprint checks so the saved point represents the configuration immediately before an authorized write attempt. If the auto snapshot cannot be created because collection or storage is degraded, the covered write is stopped fail-closed instead of continuing without a recovery point. Successful and failed auto snapshot creation attempts write audit records; realtime events carry the audit id when a snapshot is created.

Session callback auto snapshots are used only after their WebAdmin session was created through the protected route and after the callback-specific target, lock and fingerprint checks pass. They still run before the final `signal_devices.json` write and fail closed if the snapshot cannot be created.

Known not covered in 8.18:

- Runtime-only state writes, such as Timer active instance state and Signal Join pending state, because they are intentionally not configuration snapshots.
- WebAdmin user/password/settings stores, because those are security/admin local settings rather than gameplay configuration recovery points.
- StateVariable runtime action writes, because they do not go through WebAdmin write endpoints.
- Signal Join reset runtime-status actions and Timer start/cancel/reset runtime actions, because they are runtime state operations, not config writes.

## Diff

Snapshot detail shows diff against the previous snapshot:

- resource created
- resource updated
- resource deleted
- unchanged summary
- by-type counts

Diff entries are clickable and open a read-only `变更详情` modal. The modal shows resource type, id, change type, source store, before/after fingerprints, bounded JSON previews and a shallow JSON field diff for updated resources. Created resources show the new summary/JSON preview; deleted resources show the old summary/JSON preview. The modal has no save action and does not change filters, selection or timeline scroll.

Snapshot package loading recomputes resource fingerprints and compares package fingerprints against manifest records. A mismatch blocks detail/dry-run/apply instead of trusting the package file. A degraded manifest also blocks creating new snapshots so a corrupt timeline is not overwritten.

For before-write auto snapshots and pre-rollback snapshots, the detail rail shows `本次操作变化` when an operation diff is available. For auto snapshots this is the successful write operation's before/after diff, including Timer rename and other metadata/config edits. For pre-rollback snapshots this is the rollback operation from current config to the selected target. The snapshot-to-previous diff remains available as advanced protection-point context so it does not make rollback-created protection points look like they introduced resources that the rollback is about to remove.

## Rollback

Rollback always uses:

```text
select snapshot -> dry-run -> preview operations/warnings/blockers -> confirm -> create pre_rollback -> write allowlisted store files -> audit/realtime
```

Dry-run compares the current allowlisted store files with the selected snapshot package. Apply validates permissions, CSRF, same-origin, global `snapshot_rollback/timeline` edit lock, expected manifest fingerprint and dry-run fingerprint before writing. Apply stages all target JSON writes before deleting or replacing store files, then moves staged files into place and clears affected caches, including SignalDevice, SignalListener, RegionController, StateVariable, condition runtime gate and Timer definition caches.

Rollback restores only allowlisted configuration files. It does not restore runtime history, active timers, pending joins, online players, inventory, world blocks or entities.

## UI

The `#/snapshots` page is not a normal list/table. It uses:

- metric cards
- filter bar
- resource-type filter
- graph stream with vertical timeline rail
- visibly different manual / auto / `pre_rollback` nodes
- right detail rail
- diff list
- manual snapshot modal
- rollback dry-run modal
- rollback confirm modal

Manual nodes use green accents, auto nodes use cyan accents, and `pre_rollback` nodes use yellow warning accents.

Snapshot APIs return relative snapshot paths and resource metadata/fingerprints for the UI. The detail resource preview does not expose raw canonical JSON or absolute server paths.

## Permissions And Security

Operations:

- `VIEW_SNAPSHOTS`
- `CREATE_SNAPSHOT`
- `ROLLBACK_SNAPSHOT`
- `DELETE_SNAPSHOT` reserved for future deletion support

Role policy:

- OWNER: view, create, rollback and reserved delete.
- EDITOR: view and create.
- TESTER / VIEWER: view.

Rollback uses `TARGET_SNAPSHOT_ROLLBACK` edit lock and expected manifest fingerprint. Manual creation and rollback use CSRF and same-origin checks. Snapshot create/apply writes audit events and publishes `SNAPSHOT_CREATED`, `SNAPSHOT_ROLLBACK_APPLIED` and `SNAPSHOT_TIMELINE_CHANGED` realtime events.

## Retention

Manual snapshots and `pre_rollback` snapshots are protected. Auto snapshots retain the newest 200 records by default. Retention deletes only old auto snapshot packages and their manifest records.

## Not Included

8.18 does not implement:

- Git branch / merge / rebase.
- full Git-like branch graph.
- multi-user conflict merge.
- snapshot editing.
- cross-world or cloud backup.
- GameController / MissionSystem / PhaseController.
- full Logic Chain Editor.
- Scratch editor.
- if / else runtime.
- old node move/delete/reorder.
- old action delete/reorder.
- new ActionType.
- new ConditionNodeType.
- runtime semantic changes.
- Minecraft startup, MCP scenario or screenshot matrix.

## Future Work

- Snapshot labels and pinning.
- Field-level JSON diff.
- Export/import snapshot bundle.
- Scheduled backups.
- Remote backup.
- Template apply integrated recovery view.
- Multi-user conflict resolution.
- Optional WebAdmin settings/user-store protection policy.
