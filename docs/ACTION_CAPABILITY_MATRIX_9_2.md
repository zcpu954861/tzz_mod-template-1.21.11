# TZZ Mod 9.2 Action Capability Matrix

## Baseline

| Item | Value |
| --- | --- |
| Current phase | 9.2 Phase 2 capability matrix + backend validation |
| Baseline | `v1.68.4-docs-accuracy` / `f8fa12c6e5c20ca82a3d5ea0a87f24d26462fb4c` |
| Matrix scope | Existing `ActionConfig` owners only |

This matrix is a planning and audit artifact. It does not grant permission to change runtime execution, owner order, save payloads, WebAdmin API shape or validation results.

Phase 1 implementation adds `ActionOwnerType` and metadata-only `ActionCapability` under `com.zcpu.tzzmod.action.schema`.

Phase 2 implementation adds the authoritative backend matrix:

```text
src/main/java/com/zcpu/tzzmod/action/schema/ActionCapabilityMatrix.java
src/main/java/com/zcpu/tzzmod/action/schema/ActionOwnerCapability.java
src/main/java/com/zcpu/tzzmod/action/validation/ActionValidationService.java
```

The matrix is authoritative for owner/action support, bucket ids, list field names, max action count and condition runtime targets. All current owner buckets keep the existing `maxActions=64` save boundary. It still does not hold edit locks, expected fingerprints, write adapters, store writers, audit writers or realtime adapters; those remain owner-service responsibilities.

## Action Type Coverage

| ActionType | SignalListener | ActionRelay | Region enter | Region exit | Region stay | Timer start | Timer tick | Timer complete | Timer cancel | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `command` | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Requires dangerous command policy and redaction. |
| `message` | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Player context may be absent. |
| `sound` | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Current runtime feedback sound is legacy fixed behavior. |
| `signal` | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Requires channel validation. |
| `state_variable` | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Requires controlled state field validation. |
| `timer_start` | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Requires timer target fields. |
| `timer_cancel` | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Existing | Requires timer target fields. |

At Phase 0, the current code could carry these action types in the listed owner lists, but validation consistency was not equal across all owners. Phase 2 resolves the backend validation gap for Timer buckets; editor rendering and summary unification remain later phases.

## Owner Capability Detail

| Owner / bucket | Storage / DTO boundary | WebAdmin save boundary | Lock / fingerprint | Condition target | List operations | Current summary / audit | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| SignalListener `actions` | `SignalListenerData.actions` | `WebAdminSignalListenerActionsService` and Logic Chain typed writes | SignalListener action config lock and expected fingerprint | SignalListener action target | add / edit / delete / clear; Logic Chain same-bucket reorder | Owner-specific DTO summary and audit | Listener list gate and single-action gate semantics must remain. |
| ActionRelay `actions` | ActionRelay block entity action list | `WebAdminActionRelayActionsService` and Logic Chain typed writes | ActionRelay actions lock and expected fingerprint | ActionRelay action target | add / edit / delete / reorder / clear | Most complete common validation and summary helpers | Runtime must not force-load unloaded relays. |
| Region `enterActions` | `RegionControllerData.enterActions` | `WebAdminRegionControllerService` and Logic Chain typed writes | RegionController config lock and expected fingerprint | Region enter action target | add / edit / delete / clear; Logic Chain reorder | Region service summary / audit | Enter state update order must not change. |
| Region `exitActions` | `RegionControllerData.exitActions` | `WebAdminRegionControllerService` and Logic Chain typed writes | RegionController config lock and expected fingerprint | Region exit action target | add / edit / delete / clear; Logic Chain reorder | Region service summary / audit | Exit state update order must not change. |
| Region `stayActions` | `RegionControllerData.stayActions` | `WebAdminRegionControllerService` and Logic Chain typed writes | RegionController config lock and expected fingerprint | Region stay action target | add / edit / delete / clear; Logic Chain reorder | Region service summary / audit | Stay interval must remain unchanged. |
| Timer `onStartActions` | `TimerDefinition.onStartActions` | `WebAdminTimerService` and Logic Chain typed writes | Timer config lock and expected fingerprint | Timer start action target | add / edit / delete / reorder via Timer config / Logic Chain | Timer service summary / audit | Start creates active instance before onStart. |
| Timer `onTickActions` | `TimerDefinition.onTickActions` | `WebAdminTimerService` and Logic Chain typed writes | Timer config lock and expected fingerprint | Timer tick action target | add / edit / delete / reorder via Timer config / Logic Chain | Timer service summary / audit | DELAY mode clears / ignores tick bucket per existing save semantics. |
| Timer `onCompleteActions` | `TimerDefinition.onCompleteActions` | `WebAdminTimerService` and Logic Chain typed writes | Timer config lock and expected fingerprint | Timer complete action target | add / edit / delete / reorder via Timer config / Logic Chain | Timer service summary / audit | Complete outputChannel is not an `ActionConfig` action. |
| Timer `onCancelActions` | `TimerDefinition.onCancelActions` | `WebAdminTimerService` and Logic Chain typed writes | Timer config lock and expected fingerprint | Timer cancel action target | add / edit / delete / reorder via Timer config / Logic Chain | Timer service summary / audit | Cancel removes active instance before onCancel. |

## Phase 2 Backend Validation

`ActionValidationService` is now the common save-time validator for current `ActionConfig` owners. It checks strict action type parsing before `ActionConfig` construction, owner support through `ActionCapabilityMatrix`, common boolean / cooldown / value constraints, command management-command blocking, signal channel syntax, state variable mutation field validity, timer target / policy / duration / missing-behavior fields and optional action-level condition group compatibility.

Condition group validation is injected from WebAdmin owner services, so blank `conditionGroupId` remains a lazy skip and does not load the condition group store. Nonblank ids are validated with the owner bucket's `*_ACTION` target from the capability matrix.

`WebAdminActionRelayActionsService`, `WebAdminSignalListenerActionsService`, `WebAdminRegionControllerService` and `WebAdminTimerService` all route save-time action drafts through this validation layer. Timer keeps its legacy `timer_action_type_invalid`, `timer_action_required` and `timer_too_many_actions` error codes while gaining common validation for command / message / sound / signal / state_variable actions.

## Explicit Non-Owners

| Resource / config | Why it is not an ActionConfig owner in 9.2 |
| --- | --- |
| VBD native redstone / blockstate / interaction / container triggers | These are trigger/channel/gate config paths that emit signals or perform trigger-specific behavior. |
| VBD itemSubmit requirements | Requirement matching and consume behavior are not an action list. |
| Container item conditions | Condition / channel trigger config, not `ActionEngine` actions. |
| SignalEmitter | Producer resource, no action list. |
| SignalReceiver | Consumer resource, no action list. |
| Timer `outputChannel` | Completion signal output field, not an `ActionConfig`. |
| Program Model / branch / mission step | Not implemented in 9.2; belongs to later 10.x work. |

## Capability Rules For Later Phases

- Frontend filtering is only a convenience. Backend capability validation is authoritative.
- Unsupported owner/action combinations must fail closed.
- Capability checks must preserve owner, bucket, index and same-index edit semantics.
- Delete and reorder must remain owner-local and bucket-local; cross-owner or cross-bucket movement remains rejected.
- `expectedFingerprint`, edit lock, CSRF/same-origin, confirmation and audit semantics must remain owner-specific where they are today.

## Validation Gaps To Track

| Gap | Risk | Target phase |
| --- | --- | --- |
| Timer action fields are not fully described by a shared schema/capability validator. | UI could look unified while backend checks differ. | Resolved for backend validation in Phase 2; editor/summary still later. |
| Allowed action types are repeated across services and scripts. | Future drift between UI and backend. | Backend save validation resolved in Phase 2; frontend filtering waits for Phase 3. |
| Action summaries are owner-specific. | Diff, audit and card text can disagree. | Phase 4 |
| Snapshot diff is resource-level. | Action-index changes are hard to read. | Phase 4, compatible summary only |

## Phase 1 Registry Owner Enum

The implemented registry limits owner metadata to current `ActionConfig` owners:

- `SIGNAL_LISTENER`
- `ACTION_RELAY`
- `REGION_ENTER`
- `REGION_EXIT`
- `REGION_STAY`
- `TIMER_START` (`timer_on_start`)
- `TIMER_TICK` (`timer_on_tick`)
- `TIMER_COMPLETE` (`timer_on_complete`)
- `TIMER_CANCEL` (`timer_on_cancel`)

It deliberately excludes VBD native trigger, itemSubmit, container, Program Model, branch and sequence owners.
