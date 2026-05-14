# WebAdmin RegionController Editing 7.12 Current Context

## Stage

7.12 WebAdmin RegionController Full Editing.

Stable baseline: `v1.41.0-web-admin-unified-item-submit-editor`.

This stage is RegionController WebUI editing only. It connects existing RegionController capabilities to WebAdmin so controllers can be viewed, created, edited, deleted, audited, and refreshed by realtime events.

## Existing RegionController Feature Inventory

`RegionControllerData` currently persists:

- `id`: stable controller UUID, not editable after creation.
- `name`: display name.
- `regionId`: bound planner region id.
- `enabled`: whether the controller participates in runtime ticking.
- `targetFilter`: player target filter, supporting `ALL`, `OP`, and `TAG + value`.
- `stayIntervalTicks`: minimum stay trigger interval, default `100` ticks, minimum `20` ticks.
- `enterActions`: `ActionConfig` list executed when a player enters the region.
- `exitActions`: `ActionConfig` list executed when a player exits the region.
- `stayActions`: `ActionConfig` list executed when a player stays in the region for the configured interval.

## Runtime Semantics

RegionController runtime semantics are not changed in this stage:

- The tracker checks online players every 10 ticks.
- Region membership uses dimension plus the X/Z plane. Current runtime does not use Y.
- `ENTER`: fires when a player moves from outside to inside. Initializing while already inside does not backfill enter.
- `EXIT`: fires when a player moves from inside to outside or changes dimension.
- `STAY`: fires when a player remains inside and `stayIntervalTicks` has elapsed since the previous stay trigger.
- Actions still execute through existing `ActionEngine.executeAll`; this stage does not rewrite ActionEngine semantics.

## WebUI Editing Scope

7.12 P0 covers:

- RegionController list and detail pages.
- Create controller with `regionId` and `name`; defaults are enabled, targetFilter=ALL, stayInterval=100.
- Delete controller with dangerous double confirmation.
- Edit base fields: `enabled`, `name`, `regionId`, `targetFilter`, `stayIntervalTicks`.
- `targetFilter` supports `ALL`, `OP`, and `TAG`; TAG requires a tag value.
- `stayIntervalTicks` below `MIN_STAY_INTERVAL_TICKS` returns validation error instead of silently downgrading.
- enter / exit / stay actions are displayed, addable, and clearable.
- Action add uses safe action entries: `command`, `signal`, `message`, and `sound`.
- Command actions must reuse the WebAdmin action relay dangerous-command blocking rules and must not bypass command validation.
- Writes must use WebAdmin permission, CSRF, same-origin, edit lock, expectedFingerprint, `WebAdminWriteResult`, audit, and realtime.
- Saving one field group must not clear unrelated fields or action lists.

## Forbidden Scope

7.12 does not implement:

- ConditionEngine.
- Virtual listener or SignalListener editing expansion.
- Path visualization, path graph, or logic-chain graph.
- Scratch-like editor or graph editor.
- raw JSON editing.
- arbitrary NBT path editing.
- new region runtime semantics.
- ActionEngine or RegionControllerTracker rewrite.
- WebAdmin redesign.
- mandatory full MCP scenario automation.

## Manual Testing And Screenshots

Manual testing is primary for this stage. Codex runs build, guard, and static checks, but does not force MCP scenario automation or screenshot matrix automation.

Because this stage changes WebAdmin UI, manual acceptance should check:

- `#/region-controllers` list page.
- controller detail page.
- create, edit, delete, action add, and action clear modals.
- small viewport `854x480`.
- 4K 200% scaled visual profile: `1920x1080` CSS viewport + `deviceScaleFactor=2`, or the user's real 4K 200% display.

No checkpoint before user approval.

## 7.x Completion State

RegionController WebUI editing is completed inside 7.12, not deferred to 7.13, and not expanded into a new system.
