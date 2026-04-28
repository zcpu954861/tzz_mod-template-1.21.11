# 7.4 WebAdmin Channel Metadata + Signal Listener Basic Config Editing

## 1. Version Positioning

7.4 extends WebAdmin editing from device-only editing into the Signal area, but only for low-risk fields:

- WebAdmin-only Signal channel display metadata.
- Existing Signal Listener basic config.

This stage does not create channels, create listeners, edit actions, or change complex SignalBridge logic.

## 2. Channel Metadata

Channel metadata is stored under the current world WebAdmin directory:

```text
<world-save-root>/tzz/webadmin/web_admin_channel_metadata.json
```

Editable fields:

- `displayName`
- `note`
- `iconKey`

The raw `channel` string remains the technical ID and cannot be changed by metadata editing. Metadata affects only WebAdmin display. It does not create a real channel, listener, receiver, action relay, or action.

API:

```text
GET /api/webadmin/channel-metadata?channel=<urlencodedChannel>
PATCH /api/webadmin/channel-metadata?channel=<urlencodedChannel>
```

PATCH requires `EDITOR` or `OWNER`, CSRF / same-origin checks, `channel_metadata` edit lock, `expectedFingerprint`, validation, audit, and realtime events.

## 3. Signal Listener Basic Config

Editable fields for an existing listener:

- `enabled`
- `channel`
- `cooldownTicks`

The listener identity is its existing `SignalListenerData.id`. WebAdmin uses that stable id as `listenerRef`; listener name is display-only in this stage and is not editable.

API:

```text
GET /api/webadmin/signal-listener-basic-config/{listenerRef}
PATCH /api/webadmin/signal-listener-basic-config/{listenerRef}
```

PATCH updates only the three allowed fields and preserves listener id, name, actions, and other state. Changing the listener channel does not create channel metadata or consumers.

## 4. Validation

Channel metadata:

- channel is required, max 128 chars, no control chars, must follow Signal channel syntax.
- displayName max 64 chars, no control chars.
- note max 512 chars, no control chars.
- iconKey must be an internal preset or blank/auto.

Listener basic config:

- enabled must be boolean.
- channel is required, max 128 chars, no control chars, must follow Signal channel syntax.
- cooldownTicks must be an integer between 0 and 72000.

## 5. Edit Lock And Conflict Detection

Lock targets:

- `channel_metadata:<channel>`
- `signal_listener_basic_config:<listenerId>`

Both PATCH routes require a valid lock and expected fingerprint. Stale saves return `conflict_detected` and do not overwrite current server data.

## 6. Audit And Realtime

Successful operations use:

- `EDIT_CHANNEL_METADATA`
- `EDIT_SIGNAL_LISTENER_BASIC_CONFIG`

Failure paths such as permission denied, CSRF failure, validation failure, lock conflict, and fingerprint conflict are represented by `WebAdminWriteResult` and safe audit summaries.

Realtime events:

- `channel_metadata_changed`
- `signal_listener_config_changed`
- `config_changed`
- `write_audit_appended`

Payloads are lightweight and do not include password hashes, salts, session tokens, cookie values, or raw listener JSON.

## 7. Frontend Behavior

Signal list and channel detail pages display channel metadata when present. Channel detail includes a metadata card and listener basic config editing cards.

Listener channel editing reuses the dark WebAdmin combobox:

- existing channels from `/api/signals/channels`
- searchable dropdown
- manual new channel input
- warning that a new channel does not create listeners, receivers, or action relays

Realtime refresh remains silent and route-filtered: no full-page reload, no scroll reset, no filter reset, and no overwriting active form input.

## 8. Explicitly Not Included

7.4 does not include:

- listener create / delete / rename
- action create / delete / edit / reorder / execute
- command action editing
- signal action editing
- matcher, itemSubmit, interactionItem, consume, container condition editing
- region, user, settings editing
- raw JSON editing
- Scratch-like editor, graph drag editing, ConditionEngine, GameController, or MissionSystem

## 9. Follow-Up

Future stages can expand Signal editing only after separate design for action safety, action validation, recursion handling, bulk change review, and rollback strategy.
