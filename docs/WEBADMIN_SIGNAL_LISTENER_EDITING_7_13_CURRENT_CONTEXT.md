# 7.13 WebAdmin SignalListener Editing Current Context

## Phase

7.13 WebAdmin SignalListener / 虚拟监听器完整编辑。

Stable baseline: `v1.42.0-web-admin-region-controller-editing`.

Working branch: `feature/web-admin-signal-listener-editing`.

## Scope

This phase only completes WebAdmin editing for existing SignalListener capabilities.

Allowed:

- SignalListener list/detail visibility.
- SignalListener create/delete using existing lifecycle semantics.
- Enabled/channel/cooldown basic config editing.
- Action list display.
- Action add, single delete, and clear.
- Dynamic action fields for signal/command/message/sound.
- Dark channel combobox for channel fields.
- Edit lock, expectedFingerprint, WebAdminWriteResult, audit, realtime, and dirty guard.
- Chinese user-facing copy.
- Manual UI validation at small viewport and 4K 200% scaled.

Not in scope:

- ConditionEngine.
- Channel logic chain editor.
- Path visualization / graph.
- Scratch-like editor.
- Raw JSON / NBT path editing.
- SignalBridge runtime rewrite.
- ActionEngine rewrite.
- RegionController editing.
- New Signal runtime semantics.

## Existing SignalListener Data

Current persisted `SignalListenerData` contains:

- `id`
- `name`
- `channel`
- `enabled`
- `cooldownTicks`
- `actions`

`actions` are existing `ActionConfig` entries:

- `type`
- `value`
- `enabled`
- `requiresOp`
- `cooldownTicks`
- `notifyOps`

Runtime state such as listener cooldown and signal history is read-only WebAdmin context and must not be written as SignalListener config.

## Runtime Semantics

SignalBridge dispatch remains unchanged:

- Signal emit dispatches existing receivers/action relays first, then enabled listeners for the channel.
- Disabled listeners do not run.
- Empty action lists are skipped.
- Listener cooldown is enforced by existing `SignalBridgeServer`.
- Actions run through the existing `ActionEngine` execution path in list order.
- Failures use existing ActionEngine semantics.
- Signal recursion depth limit remains in existing SignalBridge runtime.
- Listener action execution must keep using the shared `ActionConfig` / `ActionEngine` path for command, message, signal, and sound actions. The listener path may add diagnostic context such as action index/type to failure messages, but must not introduce a separate listener-only executor.
- Message actions are supported from listener runtime even when the signal source has no player context by broadcasting through the server player list. This prevents WebAdmin from exposing a message action that always fails for non-player signal sources.
- Listener recent events on the detail page are driven by existing Signal history realtime events and must silent-refresh without resetting scroll, open details, modals, or current input.
- Listener detail recent events display at most the latest 3 deduplicated entries.

7.13 WebAdmin writes must not change those semantics.

## WebUI Editing Rules

Channel fields must use the existing dark channel combobox:

- Existing channels can be selected.
- New channel names can be typed.
- Typing a new channel does not create a consumer.
- Native white selects are not acceptable for channel selection.

Action lists must follow the action_relay / 7.12 RegionController pattern:

- Detail pages show stable summary cards.
- Full action rows are managed in a modal/drawer.
- Action add uses dynamic fields based on action type.
- Single action delete requires confirmation but no typed ID/name.
- Clear actions requires confirmation but no typed ID/name.
- Action count and ordering are preserved.

Edit lock UI must be visible:

- Locked-by-other operations show disabled/replaced buttons and lock state.
- Current user lock shows editing state and expiry.
- Backend writes still validate lock and expectedFingerprint.
- The listener basic config edit button on listener detail must render lock state directly. It must not remain as a normal clickable button that only reports lock conflict by toast after click.

## Action Validation

Command actions continue to use existing WebAdmin Action validation.

Dangerous server management commands remain blocked, including direct roots and nested `execute ... run` forms for commands such as `stop`, `op`, `ban`, `kick`, `whitelist`, `save-off`, `save-on`, and `reload`.

No raw JSON editor is added.

## UI Validation

Manual UI validation remains required before checkpoint:

- Small viewport around `854x480`.
- 4K 200% scaled profile, represented as `1920x1080` CSS viewport with `deviceScaleFactor=2`, or the user's real 4K 200% view.
- Modal body scroll, fixed header/footer, visible action buttons, no overflow, no English primary labels.

Codex may assist with screenshots, but final UI acceptance is user-owned. Do not checkpoint before user confirmation.

## Future Planning

7.x still needs a channel-view modular logic chain editor MVP.

8.x ConditionEngine can later attach condition judgment ability, but it must not be introduced in 7.13.
