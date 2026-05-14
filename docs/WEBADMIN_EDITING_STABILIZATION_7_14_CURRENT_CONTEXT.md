# 7.14 WebAdmin Editing Stabilization Current Context

## Phase

7.14 WebAdmin Editing Stabilization / 7.x 编辑层稳定补齐。

Stable baseline: `v1.43.0-web-admin-signal-listener-editing`.

Working branch: `feature/web-admin-editing-stabilization`.

This phase is a stabilization and documentation pass. It is not a new feature expansion stage.

## Goals

- Consolidate the current 7.x WebAdmin editing capability status.
- Align README and current-context documentation with the implemented WebAdmin / MCP state.
- Add guard coverage for the 7.14 stabilization boundary and long-term editing rules.
- Record the remaining large systems so later work does not treat them as already complete.

## Current WebAdmin Editing Capability Snapshot

Completed editing capabilities include:

- WebAdmin login/session and current-user password change.
- Device display metadata: display name, note, icon key.
- Device basic config: enabled and primary channel.
- Device extended config for supported physical devices and VBD fields.
- Signal channel metadata: display name, note, icon key.
- ActionRelay action list editing with dynamic action fields and dangerous command validation.
- VBD native trigger editing.
- Interaction item matcher editing.
- Unified itemSubmit requirement list editor.
- Container template editor.
- RegionController WebUI list/detail/create/delete/edit and enter/exit/stay action management.
- SignalListener WebUI list/detail/create/delete/edit and action management.

Observation and support surfaces include:

- Dashboard, devices, signals, regions, actions, users, settings, Doctor, History, debug summaries, realtime, audit, edit lock status, and write capabilities.
- Local Test MCP tools for assisted local checks, WebAdmin browser helpers, TestBridge, screenshots, and reports.

## Current Testing Position

Manual testing is primary after 7.13. Local Test MCP remains available as an auxiliary tool, but it is no longer mandatory for Codex to run full scenario automation for every WebAdmin editing stage.

7.14 explicitly does not:

- Generate screenshots.
- Run screenshot matrices.
- Run MCP scenarios.
- Start Minecraft clients for scenario testing.

Build and guard verification still applies:

- `cd tools\tzz-test-mcp && npm run build && npm test`
- `.\gradlew.bat clean build`
- `.\gradlew.bat stabilizationGuardTest --rerun-tasks`
- `.\gradlew.bat localTestMcpGuardTest --rerun-tasks`
- `git diff --check`

## WebAdmin UI Consistency Rules

Future editing work should reuse the mature 7.x interaction patterns:

- Channel fields use the dark channel combobox and may select existing channels or type new channel names.
- Region binding uses a searchable dark region selector.
- Action lists use stable summary cards on detail pages and modal/drawer management for full lists.
- Action add forms show only fields relevant to the selected action type.
- Edit lock state must be visible directly on affected buttons; toast-only lock feedback is not acceptable.
- Delete and clear confirmations must be explicit, but should not require typing IDs or names unless the user explicitly asks for that stricter pattern.
- Primary user-facing copy should be Chinese. Technical IDs may remain as secondary text.
- UI changes still require small viewport and 4K 200% scaled manual review before checkpoint, but 7.14 itself does not generate screenshots.

## Known Follow-up Stabilization Notes

7.14 documents these findings without expanding scope into new runtime or UI systems:

- Some older high-risk confirmations still use stricter typed ID/name confirmation. The current preferred pattern is explicit confirmation without typing IDs/names, but changing older flows should be done as a focused UI consistency pass.
- VBD delete/selection flows and SignalListener lifecycle flows already use WebAdmin write safety primitives, but their edit-lock/fingerprint behavior should be reviewed before future broad write-safety cleanup.
- Existing same-origin checks are paired with CSRF validation. Any stricter missing-Origin/Referer policy should be evaluated separately to avoid breaking local WebAdmin clients.
- Success writes consistently publish realtime refreshes; failure audit realtime publication is not completely uniform and can be normalized later.
- Region selectors use the dark searchable selector and preserve unknown current region IDs. When catalog data is incomplete, UI copy should clearly say the user is selecting or pasting an existing region ID, not creating a new region.
- Full WebUI user CRUD is not complete. Current user/password capability is limited to current-user password change and OWNER password reset/set helpers.

## Not Done In 7.14

These are still future systems and must not be described as complete:

- 7.15 Channel Logic Chain Viewer / 频道视角模块化逻辑链查看器 MVP.
- 8.x ConditionEngine / 条件判断.
- Channel logic chain editor.
- Path visualization / graph editor.
- Scratch-like editor.
- GameController / MissionSystem / PhaseController.
- Raw JSON or arbitrary NBT path editors.
- New trigger sources or consume strategies.

## Safety Boundaries

- No arbitrary shell.
- No git mutation tools in MCP.
- No screenshot/report/log/node_modules commits.
- TestBridge remains local-only, token-protected, and loopback-restricted.
- WebAdmin writes continue through role permission, CSRF/same-origin, edit lock, expected fingerprint, `WebAdminWriteResult`, audit, and realtime.
