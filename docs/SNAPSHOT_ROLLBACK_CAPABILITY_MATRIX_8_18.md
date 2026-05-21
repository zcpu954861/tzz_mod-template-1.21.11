# Snapshot Rollback Capability Matrix 8.18

8.18 implements a first configuration snapshot timeline and safe rollback surface for WebAdmin.

| Capability | Status | Notes |
| --- | --- | --- |
| Snapshot manifest | Implemented | `manifest.json` stores schema version, sequence, records and fingerprint. |
| Snapshot package | Implemented | `data/<snapshotId>.json` stores canonical allowlisted resources. |
| Package integrity check | Implemented | Package fingerprints are recomputed and compared with manifest records before detail/dry-run/apply. |
| World-scoped storage | Implemented | Stored under `tzz/webadmin/snapshots`. |
| Bad manifest fallback | Implemented | Returns degraded empty manifest and generic Chinese warning; parser details stay server-side. |
| Bad package fallback | Implemented | Returns degraded package load failure without partial data; parser details stay server-side. |
| Manual snapshot | Implemented | Title, note and tags through `data-snapshot-manual-modal`. |
| Auto snapshot | Implemented | Created before important WebAdmin config writes. |
| Auto snapshot recursion guard | Implemented | `SUPPRESS_AUTO_CAPTURE` prevents snapshot writes from triggering snapshots. |
| Retention | Implemented | Auto snapshots keep newest 200; manual and `pre_rollback` are protected. |
| Snapshot detail | Implemented | Shows record metadata, trigger metadata, resources and diff. |
| Previous snapshot diff | Implemented | Resource-level created/updated/deleted/unchanged counts. |
| Before-write operation diff | Implemented | Logic Chain Editor draft saves, Template import/apply, Timer, channel metadata, Signal Join, SignalListener and ConditionGroup auto snapshots are annotated after successful write with the operation's resource-level diff, so rename/config changes show as updated on the protecting auto snapshot. |
| Clickable diff detail | Implemented | Previous-snapshot diff and operation diff entries open a read-only `变更详情` modal with resource metadata, before/after fingerprints, shallow field diff and bounded JSON previews. |
| JSON advanced preview | Implemented | `data-snapshot-json-preview` exposes a bounded resource preview. |
| Rollback dry-run | Implemented | Generates `RollbackPlan` with operations, blockers, warnings and dry-run fingerprint. |
| Rollback apply | Implemented | Requires confirmation, expected fingerprint, dry-run fingerprint and edit lock. |
| Pre-rollback protection | Implemented | Applies create `pre_rollback` before restoring files and annotate a `pre_rollback operation diff` from current config to the selected rollback target. |
| Timeline route | Implemented | `#/snapshots`. |
| Timeline graph UI | Implemented | `data-snapshot-timeline-graph`, not ordinary table/list layout. |
| Manual node styling | Implemented | `data-snapshot-node-kind-manual`, green visual accent. |
| Auto node styling | Implemented | `data-snapshot-node-kind-auto`, cyan visual accent. |
| Pre-rollback node styling | Implemented | `data-snapshot-node-kind-pre-rollback`, yellow warning accent. |
| Filter by kind | Implemented | all/manual/auto/pre_rollback. |
| Filter by module | Implemented | Module options come from manifest trigger metadata. |
| Filter by resource type | Implemented | Resource options come from snapshot resource count metadata. |
| Filter by user | Implemented | User options come from manifest actor metadata. |
| Filter by time | Implemented | `from` / `to` query filters. |
| Search | Implemented | title/note/target id/resource trigger text. |
| Permission: view | Implemented | `VIEW_SNAPSHOTS`; viewer and above. |
| Permission: create | Implemented | `CREATE_SNAPSHOT`; editor and owner. |
| Permission: rollback | Implemented | `ROLLBACK_SNAPSHOT`; owner only. |
| Permission: delete | Reserved | `DELETE_SNAPSHOT` exists for future strict delete support; no delete API in MVP. |
| CSRF / same-origin | Implemented | Manual and rollback write endpoints require both. |
| Auto snapshot security precheck | Implemented | Route-level auto snapshots run only after operation permission, CSRF and same-origin pass. |
| Auto snapshot fail-closed | Implemented | Covered writes stop if the pre-write auto snapshot cannot be created. |
| Edit lock | Implemented | `TARGET_SNAPSHOT_ROLLBACK` for apply. |
| Audit | Implemented | Manual, auto snapshot success/failure, dry-run and apply write audit records. |
| Realtime | Implemented | `SNAPSHOT_CREATED`, `SNAPSHOT_ROLLBACK_APPLIED`, `SNAPSHOT_TIMELINE_CHANGED`. |
| Rollback cache refresh | Implemented | Clears SignalDevice, SignalListener, RegionController, StateVariable, condition runtime gate and Timer definition caches after apply. |
| Runtime history backup | Not included | Excluded by design. |
| Timer active state | Not included | Runtime state only. |
| Join pending state | Not included | Runtime state only. |
| Player inventory / world entities | Not included | Not a world backup. |
| Git branch / merge / rebase | Not included | Explicitly deferred. |
| Full Logic Chain Editor | Not included | No expansion beyond existing 8.16 controlled editing. |
| New ActionType | Not allowed | No enum change. |
| New ConditionNodeType | Not allowed | No enum change. |

## Covered Write Operations

Auto snapshot coverage:

- Device metadata/basic/extended config.
- Signal Device / VBD config store (`signal_devices.json`), with common runtime `last*` fields preserved during rollback.
- ActionRelay action list.
- Interaction item matcher.
- Channel metadata.
- Logic Chain Editor draft save.
- Logic-chain metadata create/update/delete.
- Signal Join create/update/delete and reset/config delete paths.
- Template import/apply.
- Timer create/update/delete config paths.
- ConditionGroup create/update/delete.
- VirtualBlockDevice delete/native trigger config.
- VirtualBlockDevice object-selection create callback.
- VirtualBlockDevice container template client save callback.
- VirtualBlockDevice single itemSubmit template client save callback.
- SignalListener create/delete/basic config/action add/update/delete/clear.
- RegionController create/update/delete/action add/update/delete/clear.

## Not Covered In 8.18

- WebAdmin user/password/settings stores: security/admin local settings, not gameplay config recovery.
- Runtime-only actions: Timer start/cancel/reset, Signal Join runtime reset/status, StateVariable action runtime writes.
- Existing StateVariable values are not rolled back when their definition still exists and the current value is valid for the target type.
- Existing Signal Device runtime `last*` fields are not rolled back when the device still exists.
- Minecraft world blocks/entities and player live data.
- Full field-level diff.
- Snapshot export/import bundle.
- Manual snapshot deletion or pinning UI.

## Guard Markers

Required markers:

- `data-snapshot-timeline-page`
- `data-snapshot-timeline-graph`
- `data-snapshot-timeline-not-table`
- `data-snapshot-filter-search`
- `data-snapshot-filter-resource`
- `data-snapshot-node-kind-manual`
- `data-snapshot-node-kind-auto`
- `data-snapshot-node-kind-pre-rollback`
- `data-snapshot-detail-rail`
- `data-snapshot-detail-diff`
- `data-snapshot-before-write-explained`
- `data-snapshot-operation-diff`
- `data-snapshot-operation-timer-updated`
- `data-snapshot-diff-entry`
- `data-snapshot-diff-clickable`
- `data-snapshot-operation-diff-item`
- `data-snapshot-previous-diff-item`
- `data-snapshot-diff-detail-modal`
- `data-snapshot-diff-detail-readonly`
- `data-snapshot-diff-detail-no-save`
- `data-snapshot-diff-detail-resource-metadata`
- `data-snapshot-diff-detail-updated-summary`
- `data-snapshot-diff-detail-created-summary`
- `data-snapshot-diff-detail-deleted-summary`
- `data-snapshot-diff-entry-event-delegation`
- `data-snapshot-diff-button-type`
- `data-snapshot-manual-modal`
- `data-snapshot-rollback-dry-run-modal`
- `data-snapshot-rollback-confirm-modal`
- `data-snapshot-json-preview`
- `VIEW_SNAPSHOTS`
- `CREATE_SNAPSHOT`
- `ROLLBACK_SNAPSHOT`
- `TARGET_SNAPSHOT_ROLLBACK`
- `SNAPSHOT_CREATED`
- `SNAPSHOT_ROLLBACK_APPLIED`
- `SUPPRESS_AUTO_CAPTURE`
- `signal_devices.json`
