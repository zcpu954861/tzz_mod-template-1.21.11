# Regression Test 7.4

## Scope

Verify 7.4 WebAdmin Signal channel metadata and Signal Listener basic config editing.

## Setup

- Start a server/world with WebAdmin enabled.
- Log in as OWNER or EDITOR for write tests.
- Log in as VIEWER and TESTER for permission tests.
- Prepare at least one existing Signal channel and one existing Signal Listener.

## Channel Metadata

1. Open `/app#/signals`.
2. Open an existing channel detail page.
3. Edit WebAdmin channel display metadata:
   - displayName
   - note
   - iconKey
4. Save and confirm:
   - update succeeds for EDITOR / OWNER
   - raw channel string is unchanged
   - SignalBridge behavior is unchanged
   - metadata persists under `<world-save-root>/tzz/webadmin/web_admin_channel_metadata.json`
5. Try invalid input:
   - displayName over 64 chars
   - note over 512 chars
   - control characters
   - invalid iconKey
6. Confirm validation errors are shown and old metadata is preserved.

## Listener Basic Config

1. Open a channel detail page containing a listener.
2. Edit listener basic config:
   - enabled
   - channel
   - cooldownTicks
3. Confirm the channel field uses the dark WebAdmin combobox.
4. Select an existing channel from `/api/signals/channels`.
5. Manually type a new channel and confirm the warning explains no consumers are created automatically.
6. Save and confirm only enabled/channel/cooldownTicks changed.
7. Confirm actions list, action payloads, listener id, listener name, and other listener fields are preserved.

## Permissions

1. OWNER can edit channel metadata and listener basic config.
2. EDITOR can edit channel metadata and listener basic config.
3. VIEWER cannot acquire edit locks or submit PATCH.
4. TESTER cannot acquire edit locks or submit PATCH.

## CSRF / Lock / Fingerprint

1. PATCH without CSRF fails.
2. PATCH with invalid CSRF fails.
3. PATCH without a valid edit lock fails.
4. PATCH with an expired or wrong lock fails.
5. Stale expectedFingerprint returns `conflict_detected` and does not overwrite newer data.

## Audit And Realtime

1. Successful channel metadata save records audit.
2. Successful listener basic config save records audit.
3. Validation failure and conflict failure are auditable.
4. Realtime events are published:
   - `channel_metadata_changed`
   - `signal_listener_config_changed`
   - `config_changed`
   - `write_audit_appended`
5. Signal pages refresh silently without full reload, scroll reset, filter reset, or clearing active form input.

## Negative Scope

Confirm 7.4 still does not expose:

- listener create/delete/rename
- action create/delete/edit/reorder/execute
- command action editing
- signal action editing
- matcher / itemSubmit / interactionItem / consume / container condition editing
- region / user / settings editing
- raw JSON editing

## Existing Page Smoke Test

Check these routes still load:

- `/app#/dashboard`
- `/app#/devices`
- `/app#/signals`
- `/app#/doctor`
- `/app#/history`
- `/app#/users`
- `/app#/settings`
- `/app#/regions`
- `/app#/actions`

## Build

Run:

```text
./gradlew.bat clean build
./gradlew.bat stabilizationGuardTest --rerun-tasks
```
