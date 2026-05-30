package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.action.schema.ActionCapabilityMatrix;
import com.zcpu.tzzmod.action.schema.ActionOwnerCapability;
import com.zcpu.tzzmod.action.schema.ActionOwnerType;
import com.zcpu.tzzmod.action.schema.ActionSchema;
import com.zcpu.tzzmod.action.schema.ActionSchemaRegistry;
import com.zcpu.tzzmod.action.validation.ActionDraft;
import com.zcpu.tzzmod.action.validation.ActionValidationResult;
import com.zcpu.tzzmod.action.validation.ActionValidationService;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.scheduler.TimerDefinition;
import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.webadmin.WebAdminActionSchemaScripts;
import com.zcpu.tzzmod.webadmin.WebAdminFrontendAssets;
import com.zcpu.tzzmod.webadmin.WebAdminJsonResponse;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.service.WebAdminActionRelayActionsService;
import com.zcpu.tzzmod.webadmin.write.WebAdminValidationError;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WebAdminActionOwnerMigrationGuardTest {
    private static final List<String> NON_OWNER_IDS = List.of(
            "vbd_trigger",
            "item_submit",
            "container_change",
            "branch"
    );

    private WebAdminActionOwnerMigrationGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        CodeQualityGuardSupport.GuardReport report =
                new CodeQualityGuardSupport.GuardReport("9.2 typed action owner migration guard");
        run(report);
        report.printAndFail();
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws Exception {
        requireFrontendOwnerExportMatchesBackendMatrix(report);
        requireFrontendHelperOptionsMatchBackendMatrix(report);
        requireOldEntriesValidateAndMatchRuntimeConfig(report);
        requireOldJsonConfigFixturesRemainReadable(report);
        requireEditorDraftPayloadMatchesOldRuntimeConfig(report);
        requireUnknownActionTypesFailClosedForEveryOwner(report);
        requireLogicChainActionPayloadsStayOwnerBucketIndexScoped(report);
        requireExistingOwnerOperationCoverage(report);
    }

    private static void requireFrontendOwnerExportMatchesBackendMatrix(CodeQualityGuardSupport.GuardReport report) throws Exception {
        Map<String, Object> data = actionEditorData();
        Map<String, Object> owners = map(data.get("owners"));
        Map<String, Object> actions = map(data.get("actions"));
        List<String> actionOrder = stringList(data.get("actionOrder"));
        List<String> expectedActionOrder = ActionSchemaRegistry.schemas().stream().map(ActionSchema::id).toList();
        report.require(expectedActionOrder.equals(actionOrder),
                "Frontend action order export must match ActionSchemaRegistry order expected=" + expectedActionOrder
                        + " actual=" + actionOrder);
        Set<String> actionTypeIds = EnumSet.allOf(ActionType.class).stream()
                .map(ActionType::id)
                .collect(java.util.stream.Collectors.toSet());
        report.require(actionTypeIds.equals(Set.copyOf(actionOrder)),
                "Frontend action order must cover every current ActionType exactly once");
        for (String id : actionOrder) {
            ActionSchema schema = ActionSchemaRegistry.findById(id).orElseThrow();
            Map<String, Object> exportedAction = map(actions.get(id));
            report.require(schema.displayName().equals(string(exportedAction.get("displayName"))),
                    "Action export displayName mismatch for " + id);
        }
        report.metric("typed_action_owner_migration.exported_owner_count", owners.size());
        report.require(owners.size() == ActionCapabilityMatrix.capabilities().size(),
                "Frontend owner export must match backend capability matrix owner count");
        for (ActionOwnerCapability capability : ActionCapabilityMatrix.capabilities()) {
            String ownerId = capability.ownerType().id();
            Map<String, Object> exported = map(owners.get(ownerId));
            report.require(ownerId.equals(string(exported.get("id"))), "Owner export id mismatch for " + ownerId);
            report.require(capability.ownerType().displayName().equals(string(exported.get("displayName"))),
                    "Owner export displayName mismatch for " + ownerId);
            report.require(number(exported.get("maxActions")) == capability.maxActions(),
                    "Owner export maxActions mismatch for " + ownerId);
            report.require(capability.actionConditionTargetType().name().equals(string(exported.get("actionConditionTargetType"))),
                    "Owner export action condition target mismatch for " + ownerId);
            report.require(Boolean.valueOf(capability.supportsSameBucketReorder()).equals(exported.get("supportsSameBucketReorder")),
                    "Owner export reorder flag mismatch for " + ownerId);
            List<String> expectedTypes = supportedIdsInSchemaOrder(actionOrder, capability);
            report.require(expectedTypes.equals(stringList(exported.get("supportedActionTypes"))),
                    "Owner export supportedActionTypes mismatch for " + ownerId + " expected=" + expectedTypes
                            + " actual=" + stringList(exported.get("supportedActionTypes")));
        }
        List<String> explicitNonOwners = stringList(data.get("explicitNonOwners"));
        for (String nonOwner : NON_OWNER_IDS) {
            report.require(explicitNonOwners.contains(nonOwner), "Frontend must keep explicit non-owner marker " + nonOwner);
            report.require(ActionCapabilityMatrix.findByOwnerId(nonOwner).isEmpty(),
                    "Backend matrix must not expose non ActionConfig owner " + nonOwner);
        }
    }

    private static void requireFrontendHelperOptionsMatchBackendMatrix(CodeQualityGuardSupport.GuardReport report) throws Exception {
        List<String> ownerIds = ActionCapabilityMatrix.capabilities().stream()
                .map(capability -> capability.ownerType().id())
                .toList();
        String script = """
                function esc(value){return String(value ?? '');}
                """
                + WebAdminActionSchemaScripts.appJs()
                + """
                function optionValues(html){
                  return Array.from(String(html||'').matchAll(/<option value="([^"]+)"/g), match => match[1]);
                }
                const owners = %s;
                const nonOwners = %s;
                const lines = [];
                for (const owner of owners) {
                  lines.push('supported:' + owner + ':' + actionSupportedTypesForOwner(owner).join(','));
                  lines.push('options:' + owner + ':' + optionValues(actionTypeOptions('signal', owner)).join(','));
                }
                for (const owner of nonOwners) {
                  lines.push('nonowner:' + owner + ':' + actionSupportedTypesForOwner(owner).length + ':' + optionValues(actionTypeOptions('signal', owner)).length);
                }
                lines.push('unknown:' + actionSupportedTypesForOwner('unknown_owner').length + ':' + optionValues(actionTypeOptions('signal', 'unknown_owner')).length);
                lines.push('alias:listener:' + actionOwnerId('listener',''));
                lines.push('alias:region_enter:' + actionOwnerId('region_controller','enter'));
                lines.push('alias:region_exit:' + actionOwnerId('region_controller','exit'));
                lines.push('alias:region_stay:' + actionOwnerId('region_controller','stay'));
                lines.push('alias:timer_tick:' + actionOwnerId('timer','tick'));
                console.log(lines.join('\\n'));
                """.formatted(
                        WebAdminJsonResponse.GSON.toJson(ownerIds),
                        WebAdminJsonResponse.GSON.toJson(NON_OWNER_IDS)
                );
        Path temp = Files.createTempFile("tzz-action-owner-helper-", ".js");
        try {
            Files.writeString(temp, script, StandardCharsets.UTF_8);
            CodeQualityGuardSupport.CommandResult result = CodeQualityGuardSupport.runCommand(
                    Duration.ofSeconds(10),
                    CodeQualityGuardSupport.findNodeExecutable(),
                    temp.toString()
            );
            report.require(result.exitCode == 0,
                    "Frontend action owner helper parity script must execute output=" + result.output);
            Map<String, Object> data = actionEditorData();
            List<String> actionOrder = stringList(data.get("actionOrder"));
            for (ActionOwnerCapability capability : ActionCapabilityMatrix.capabilities()) {
                String ownerId = capability.ownerType().id();
                String expected = String.join(",", supportedIdsInSchemaOrder(actionOrder, capability));
                report.requireContains(result.output, "supported:" + ownerId + ":" + expected,
                        "actionSupportedTypesForOwner must match backend matrix for " + ownerId);
                report.requireContains(result.output, "options:" + ownerId + ":" + expected,
                        "actionTypeOptions must expose every supported action and no unsupported action for " + ownerId);
            }
            for (String nonOwner : NON_OWNER_IDS) {
                report.requireContains(result.output, "nonowner:" + nonOwner + ":0:0",
                        "Explicit non-owner helper must expose no supported options for " + nonOwner);
            }
            report.requireContains(result.output, "unknown:0:0",
                    "Unknown owner helper must fail closed with no fallback action options");
            for (String alias : List.of(
                    "alias:listener:signal_listener",
                    "alias:region_enter:region_enter",
                    "alias:region_exit:region_exit",
                    "alias:region_stay:region_stay",
                    "alias:timer_tick:timer_on_tick"
            )) {
                report.requireContains(result.output, alias, "Frontend owner id adapter must preserve " + alias);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void requireOldEntriesValidateAndMatchRuntimeConfig(CodeQualityGuardSupport.GuardReport report) {
        int checked = 0;
        for (ActionOwnerType ownerType : ActionOwnerType.values()) {
            ActionOwnerCapability capability = ActionCapabilityMatrix.require(ownerType);
            for (ActionType actionType : EnumSet.allOf(ActionType.class)) {
                WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = validEntry(actionType);
                List<WebAdminValidationError> webErrors = WebAdminActionRelayActionsService.validateActionEntries(List.of(entry), ownerType);
                report.require(webErrors.isEmpty(), ownerType.id() + " old fixture " + actionType.id()
                        + " must validate through typed owner path errors=" + webErrors);
                ActionValidationResult validation = ActionValidationService.validate(
                        ownerType,
                        capability.listFieldName() + "[0]",
                        WebAdminActionRelayActionsService.actionDraftFromEntry(entry)
                );
                report.require(validation.valid(), ownerType.id() + " draft " + actionType.id()
                        + " must validate through backend matrix errors=" + validation.errors());
                ActionConfig oldRuntimeConfig = WebAdminActionRelayActionsService.actionFromEntry(entry).normalized();
                report.require(validation.action().orElseThrow().equals(oldRuntimeConfig),
                        ownerType.id() + " draft " + actionType.id()
                                + " must produce old runtime ActionConfig equivalent");
                checked++;
            }
        }
        report.metric("typed_action_owner_migration.old_fixture_equivalence_cases", checked);
    }

    private static void requireOldJsonConfigFixturesRemainReadable(CodeQualityGuardSupport.GuardReport report) {
        String actions = oldActionConfigArrayJson();
        SignalListenerStore.DataFile listeners = WebAdminJsonResponse.GSON.fromJson("""
                {"version":1,"listeners":[{"id":"phase5.listener","name":"Phase 5 Listener","channel":"phase5.old","enabled":true,"cooldownTicks":0,"conditionGroupId":"","actions":%s}]}
                """.formatted(actions), SignalListenerStore.DataFile.class);
        SignalListenerData listener = listeners.listeners.getFirst().normalized();
        validateLoadedActions(report, ActionOwnerType.SIGNAL_LISTENER, "signal listener old JSON", listener.actions());

        RegionControllerStore.DataFile regions = WebAdminJsonResponse.GSON.fromJson("""
                {"version":1,"controllers":[{"id":"phase5.region","name":"Phase 5 Region","regionId":"arena","enabled":true,"stayIntervalTicks":100,"enterActions":%s,"exitActions":%s,"stayActions":%s}]}
                """.formatted(actions, actions, actions), RegionControllerStore.DataFile.class);
        RegionControllerData region = regions.controllers.getFirst().normalized();
        validateLoadedActions(report, ActionOwnerType.REGION_ENTER, "region enter old JSON", region.enterActions());
        validateLoadedActions(report, ActionOwnerType.REGION_EXIT, "region exit old JSON", region.exitActions());
        validateLoadedActions(report, ActionOwnerType.REGION_STAY, "region stay old JSON", region.stayActions());

        TimerStore.TimerFile timers = WebAdminJsonResponse.GSON.fromJson("""
                {"version":1,"timers":{"phase5.timer":{"id":"phase5.timer","displayName":"Phase 5 Timer","enabled":true,"mode":"COUNTDOWN","scopeMode":"GLOBAL","durationTicks":40,"intervalTicks":20,"maxRuns":2,"startPolicy":"RESTART","outputChannel":"phase5.done","onStartActions":%s,"onTickActions":%s,"onCompleteActions":%s,"onCancelActions":%s}}}
                """.formatted(actions, actions, actions, actions), TimerStore.TimerFile.class).normalized();
        TimerDefinition timer = timers.timers.get("phase5.timer").normalized();
        validateLoadedActions(report, ActionOwnerType.TIMER_START, "timer onStart old JSON", timer.onStartActions);
        validateLoadedActions(report, ActionOwnerType.TIMER_TICK, "timer onTick old JSON", timer.onTickActions);
        validateLoadedActions(report, ActionOwnerType.TIMER_COMPLETE, "timer onComplete old JSON", timer.onCompleteActions);
        validateLoadedActions(report, ActionOwnerType.TIMER_CANCEL, "timer onCancel old JSON", timer.onCancelActions);
        report.metric("typed_action_owner_migration.old_json_fixture_actions", ActionType.values().length * ActionOwnerType.values().length);
    }

    private static void validateLoadedActions(
            CodeQualityGuardSupport.GuardReport report,
            ActionOwnerType ownerType,
            String label,
            List<ActionConfig> actions
    ) {
        report.require(actions.size() == ActionType.values().length, label + " must load every current ActionType");
        for (int index = 0; index < actions.size(); index++) {
            ActionConfig action = actions.get(index).normalized();
            ActionValidationResult validation = ActionValidationService.validate(
                    ownerType,
                    ActionCapabilityMatrix.require(ownerType).listFieldName() + "[" + index + "]",
                    draftFromAction(action)
            );
            report.require(validation.valid(), label + " action " + index
                    + " must validate through typed owner path errors=" + validation.errors());
            report.require(validation.action().orElseThrow().equals(action),
                    label + " action " + index + " must keep normalized old ActionConfig fields");
        }
    }

    private static void requireEditorDraftPayloadMatchesOldRuntimeConfig(CodeQualityGuardSupport.GuardReport report) throws Exception {
        String deviceScripts = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendDeviceEditorScripts.java");
        report.requireContains(deviceScripts,
                "function actionDraftPayload(d={}){const type=String(d.type||'signal').toLowerCase(), timerType=type==='timer_start'||type==='timer_cancel';return {type,value:(type==='state_variable'||timerType)?'':String(d.value||''),enabled:d.enabled!==false,requiresOp:type==='command'&&!!d.requiresOp,cooldownTicks:Number(d.cooldownTicks||0),notifyOps:type==='command'&&!!d.notifyOps,conditionGroupId:d.conditionGroupId||'',...stateActionPayload(d),...timerActionPayload({...d,type})};}",
                "Frontend actionDraftPayload must keep the Phase 5 draft-to-DTO normalization contract");
        for (ActionType actionType : EnumSet.allOf(ActionType.class)) {
            WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = editorDraftPayload(actionType);
            ActionValidationResult validation = ActionValidationService.validate(
                    ActionOwnerType.ACTION_RELAY,
                    "actions[0]",
                    WebAdminActionRelayActionsService.actionDraftFromEntry(entry)
            );
            report.require(validation.valid(), "Editor draft mirror " + actionType.id()
                    + " must validate errors=" + validation.errors());
            ActionConfig oldRuntimeConfig = WebAdminActionRelayActionsService.actionFromEntry(entry).normalized();
            report.require(validation.action().orElseThrow().equals(oldRuntimeConfig),
                    "Editor draft mirror " + actionType.id()
                            + " must produce old runtime ActionConfig equivalent");
        }
    }

    private static void requireUnknownActionTypesFailClosedForEveryOwner(CodeQualityGuardSupport.GuardReport report) {
        for (ActionOwnerType ownerType : ActionOwnerType.values()) {
            WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = validEntry(ActionType.MESSAGE);
            entry.type = "unknown_action";
            List<WebAdminValidationError> errors = WebAdminActionRelayActionsService.validateActionEntries(List.of(entry), ownerType);
            report.require(errors.stream().anyMatch(error -> "invalid_type".equals(error.code())),
                    ownerType.id() + " must reject unknown action type before ActionConfig fallback");
            ActionValidationResult validation = ActionValidationService.validate(
                    ownerType,
                    ActionCapabilityMatrix.require(ownerType).listFieldName() + "[0]",
                    WebAdminActionRelayActionsService.actionDraftFromEntry(entry)
            );
            report.require(validation.errors().stream().anyMatch(error -> "invalid_type".equals(error.code())),
                    ownerType.id() + " backend validation must report invalid_type for unknown action ids");
            report.require(validation.action().isEmpty(),
                    ownerType.id() + " unknown action type must not produce persistent ActionConfig");
        }
    }

    private static void requireLogicChainActionPayloadsStayOwnerBucketIndexScoped(CodeQualityGuardSupport.GuardReport report) {
        String appJs = WebAdminFrontendAssets.appJs();
        String append = functionBody(appJs, "function logicChainActionAppendSavePayload");
        requirePayload(report, append, "action append payload", List.of(
                "ownerType:String(d.ownerType||'').toLowerCase()",
                "ownerId:String(d.ownerId||'')",
                "bucket:String(d.bucket||'').toLowerCase()",
                "expectedFingerprint:d.expectedFingerprint||''",
                "lockId:d.lockId||''",
                "action:actionDraftPayload(d.action||{})"
        ));
        requireNoDisplayOnlyPayloadFields(report, append, "action append payload");
        String edit = functionBody(appJs, "function logicChainActionEditSavePayload");
        requirePayload(report, edit, "action edit payload", List.of(
                "ownerType:String(d.ownerType||'').toLowerCase()",
                "ownerId:String(d.ownerId||'')",
                "bucket:String(d.bucket||'').toLowerCase()",
                "actionIndex:Number(d.actionIndex||0)",
                "operation:d.operation||'replace'",
                "expectedFingerprint:d.expectedFingerprint||''",
                "lockId:d.lockId||''",
                "action:actionDraftPayload(d.action||{})"
        ));
        requireNoDisplayOnlyPayloadFields(report, edit, "action edit payload");
        String delete = functionBody(appJs, "function logicChainActionDeleteSavePayload");
        requirePayload(report, delete, "action delete payload", List.of(
                "ownerType:String(d.ownerType||'').toLowerCase()",
                "ownerId:String(d.ownerId||'')",
                "bucket:String(d.bucket||'').toLowerCase()",
                "actionIndex:Number(d.actionIndex||0)",
                "confirmed:true",
                "expectedFingerprint:d.expectedFingerprint||''",
                "lockId:d.lockId||''"
        ));
        requireNoDisplayOnlyPayloadFields(report, delete, "action delete payload");
        String reorder = functionBody(appJs, "function logicChainActionReorderSavePayload");
        requirePayload(report, reorder, "action reorder payload", List.of(
                "ownerType:String(d.ownerType||'').toLowerCase()",
                "ownerId:String(d.ownerId||'')",
                "bucket:String(d.bucket||'').toLowerCase()",
                "fromIndex:Number(d.fromIndex||0)",
                "toIndex:Number(d.toIndex||0)",
                "confirmed:true",
                "expectedFingerprint:d.expectedFingerprint||''",
                "lockId:d.lockId||''"
        ));
        requireNoDisplayOnlyPayloadFields(report, reorder, "action reorder payload");
    }

    private static void requirePayload(
            CodeQualityGuardSupport.GuardReport report,
            String payload,
            String label,
            List<String> markers
    ) {
        for (String marker : markers) {
            report.requireContains(payload, marker, label + " must preserve owner/bucket/index save boundary");
        }
    }

    private static void requireNoDisplayOnlyPayloadFields(
            CodeQualityGuardSupport.GuardReport report,
            String payload,
            String label
    ) {
        String lower = payload.toLowerCase(java.util.Locale.ROOT);
        for (String marker : List.of("summary", "display", "label", "html", "actionsummary", "summarytext")) {
            report.require(!lower.contains(marker), label + " must not include display-only field marker " + marker);
        }
    }

    private static void requireExistingOwnerOperationCoverage(CodeQualityGuardSupport.GuardReport report) throws Exception {
        String logicChainEditorTest = CodeQualityGuardSupport.read(
                "src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorServiceTest.java");
        String timerServiceTest = CodeQualityGuardSupport.read(
                "src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminTimerServiceTest.java");
        for (String marker : List.of(
                "testActionAppendValidationCoversSupportedOwnersBucketsAndConditionGroup",
                "testActionEditValidationCoversActionRelayAndRegionBuckets",
                "Timer same-index action edit saves through Timer action bucket",
                "SignalListener same-index action edit saves through listener action service",
                "same-index action edit preserves Timer action count",
                "same-index listener action edit preserves action count",
                "non-conflicting action delete/reorder drafts for different action lists save together",
                "action reorder preserves same bucket content",
                "same action index cannot mix edit and delete/reorder",
                "same action list cannot mix append with delete/reorder/edit"
        )) {
            report.requireContains(logicChainEditorTest, marker,
                    "Existing owner operation coverage must keep action order/index guard " + marker);
        }
        for (String marker : List.of(
                "testActionBucketRoundTripDoesNotMixBuckets",
                "timer action bucket create succeeds",
                "onStartActions roundtrips start action only",
                "onTickActions roundtrips tick action only",
                "onCompleteActions roundtrips complete action only",
                "onCancelActions roundtrips cancel action only",
                "unknown timer action type rejected",
                "unknown timer action type does not save fallback command config"
        )) {
            report.requireContains(timerServiceTest, marker,
                    "Existing Timer owner operation coverage must keep bucket-local guard " + marker);
        }
    }

    private static String oldActionConfigArrayJson() {
        return """
                [
                  {"type":"COMMAND","value":"say old command","enabled":true,"requiresOp":true,"cooldownTicks":3,"notifyOps":true,"conditionGroupId":""},
                  {"type":"MESSAGE","value":"old message","enabled":true,"requiresOp":false,"cooldownTicks":4,"notifyOps":false,"conditionGroupId":""},
                  {"type":"SOUND","value":"minecraft:block.note_block.pling","enabled":true,"requiresOp":false,"cooldownTicks":5,"notifyOps":false,"conditionGroupId":""},
                  {"type":"SIGNAL","value":"phase5.old.signal","enabled":true,"requiresOp":false,"cooldownTicks":6,"notifyOps":false,"conditionGroupId":""},
                  {"type":"STATE_VARIABLE","value":"","enabled":true,"requiresOp":false,"cooldownTicks":7,"notifyOps":false,"conditionGroupId":"","stateOperation":"set_variable","stateScope":"GLOBAL","stateTargetMode":"global","stateTargetId":"","stateKey":"phase5.old.flag","stateValueType":"BOOLEAN","stateValue":"true","stateDelta":0,"stateCreateIfMissing":true,"stateInitialValue":"false"},
                  {"type":"TIMER_START","value":"","enabled":true,"requiresOp":false,"cooldownTicks":8,"notifyOps":false,"conditionGroupId":"","timerId":"phase5.timer","timerTargetMode":"context_player","timerTargetId":"","timerStartPolicyOverride":"RESTART","timerDurationOverrideTicks":0,"timerMissingBehavior":"noop_success"},
                  {"type":"TIMER_CANCEL","value":"","enabled":true,"requiresOp":false,"cooldownTicks":9,"notifyOps":false,"conditionGroupId":"","timerId":"phase5.timer","timerTargetMode":"context_player","timerTargetId":"","timerStartPolicyOverride":"","timerDurationOverrideTicks":0,"timerMissingBehavior":"fail_if_missing"}
                ]
                """;
    }

    private static ActionDraft draftFromAction(ActionConfig action) {
        ActionConfig safe = action == null ? new ActionConfig(null, "", true, false, 0, false) : action.normalized();
        return ActionDraft.builder(safe.type().id())
                .value(safe.value())
                .enabled(safe.enabled())
                .requiresOp(safe.requiresOp())
                .cooldownTicks(safe.cooldownTicks())
                .notifyOps(safe.notifyOps())
                .conditionGroupId(safe.conditionGroupId())
                .stateOperation(safe.stateOperation())
                .stateScope(safe.stateScope())
                .stateTargetMode(safe.stateTargetMode())
                .stateTargetId(safe.stateTargetId())
                .stateKey(safe.stateKey())
                .stateValueType(safe.stateValueType())
                .stateValue(safe.stateValue())
                .stateDelta(safe.stateDelta())
                .stateCreateIfMissing(safe.stateCreateIfMissing())
                .stateInitialValue(safe.stateInitialValue())
                .timerId(safe.timerId())
                .timerTargetMode(safe.timerTargetMode())
                .timerTargetId(safe.timerTargetId())
                .timerStartPolicyOverride(safe.timerStartPolicyOverride())
                .timerDurationOverrideTicks(safe.timerDurationOverrideTicks())
                .timerMissingBehavior(safe.timerMissingBehavior())
                .build();
    }

    private static WebAdminActionRelayActionsUpdateRequest.ActionEntry editorDraftPayload(ActionType actionType) {
        String type = actionType.id();
        boolean timerType = actionType == ActionType.TIMER_START || actionType == ActionType.TIMER_CANCEL;
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("type", type);
        raw.put("value", switch (actionType) {
            case COMMAND -> "/say editor draft";
            case MESSAGE -> "editor draft message";
            case SOUND -> "minecraft:block.note_block.pling";
            case SIGNAL -> "phase5.editor.signal";
            case STATE_VARIABLE, TIMER_START, TIMER_CANCEL -> "must be cleared";
        });
        raw.put("enabled", Boolean.TRUE);
        raw.put("requiresOp", Boolean.TRUE);
        raw.put("cooldownTicks", "11");
        raw.put("notifyOps", Boolean.TRUE);
        raw.put("conditionGroupId", "");
        raw.put("stateOperation", "set_variable");
        raw.put("stateScope", "PLAYER");
        raw.put("stateTargetMode", "explicit_target");
        raw.put("stateTargetId", "player-one");
        raw.put("stateKey", "phase5.editor.flag");
        raw.put("stateValueType", "BOOLEAN");
        raw.put("stateValue", "true");
        raw.put("stateDelta", 0);
        raw.put("stateCreateIfMissing", Boolean.TRUE);
        raw.put("stateInitialValue", "false");
        raw.put("timerId", "phase5.editor.timer");
        raw.put("timerTargetMode", "explicit_target");
        raw.put("timerTargetId", "player-one");
        raw.put("timerStartPolicyOverride", "RESTART");
        raw.put("timerDurationOverrideTicks", "40");
        raw.put("timerMissingBehavior", "fail");

        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry =
                new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        entry.type = type;
        entry.value = (actionType == ActionType.STATE_VARIABLE || timerType) ? "" : string(raw.get("value"));
        entry.enabled = !Boolean.FALSE.equals(raw.get("enabled"));
        entry.requiresOp = actionType == ActionType.COMMAND && Boolean.TRUE.equals(raw.get("requiresOp"));
        entry.cooldownTicks = number(raw.get("cooldownTicks"));
        entry.notifyOps = actionType == ActionType.COMMAND && Boolean.TRUE.equals(raw.get("notifyOps"));
        entry.conditionGroupId = string(raw.get("conditionGroupId"));
        entry.stateOperation = string(raw.get("stateOperation"));
        entry.stateScope = string(raw.get("stateScope"));
        entry.stateTargetMode = string(raw.get("stateTargetMode"));
        entry.stateTargetId = "PLAYER".equals(entry.stateScope) && "explicit_target".equals(entry.stateTargetMode)
                ? string(raw.get("stateTargetId"))
                : "";
        entry.stateKey = string(raw.get("stateKey"));
        entry.stateValueType = string(raw.get("stateValueType"));
        entry.stateValue = string(raw.get("stateValue"));
        entry.stateDelta = number(raw.get("stateDelta"));
        entry.stateCreateIfMissing = Boolean.TRUE.equals(raw.get("stateCreateIfMissing"));
        entry.stateInitialValue = string(raw.get("stateInitialValue"));
        entry.timerId = string(raw.get("timerId"));
        entry.timerTargetMode = string(raw.get("timerTargetMode")).isBlank()
                ? "context_player"
                : string(raw.get("timerTargetMode")).toLowerCase(java.util.Locale.ROOT);
        entry.timerTargetId = "explicit_target".equals(entry.timerTargetMode) ? string(raw.get("timerTargetId")) : "";
        entry.timerStartPolicyOverride = actionType == ActionType.TIMER_START ? string(raw.get("timerStartPolicyOverride")) : "";
        entry.timerDurationOverrideTicks = actionType == ActionType.TIMER_START ? number(raw.get("timerDurationOverrideTicks")) : 0;
        entry.timerMissingBehavior = string(raw.get("timerMissingBehavior")).isBlank()
                ? "noop_success"
                : string(raw.get("timerMissingBehavior")).toLowerCase(java.util.Locale.ROOT);
        return entry;
    }

    private static WebAdminActionRelayActionsUpdateRequest.ActionEntry validEntry(ActionType actionType) {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        entry.type = actionType.id();
        entry.enabled = Boolean.TRUE;
        entry.requiresOp = Boolean.FALSE;
        entry.cooldownTicks = 5;
        entry.notifyOps = Boolean.FALSE;
        switch (actionType) {
            case COMMAND -> {
                entry.value = "/say phase5";
                entry.notifyOps = Boolean.TRUE;
            }
            case MESSAGE -> entry.value = "phase5 message";
            case SOUND -> entry.value = "minecraft:block.note_block.pling";
            case SIGNAL -> entry.value = "phase5.signal";
            case STATE_VARIABLE -> {
                entry.stateOperation = "set_variable";
                entry.stateScope = "GLOBAL";
                entry.stateTargetMode = "global";
                entry.stateKey = "phase5.flag";
                entry.stateValueType = "BOOLEAN";
                entry.stateValue = "true";
                entry.stateCreateIfMissing = Boolean.TRUE;
                entry.stateInitialValue = "false";
            }
            case TIMER_START -> {
                entry.timerId = "phase5.timer";
                entry.timerTargetMode = "context_player";
                entry.timerStartPolicyOverride = "RESTART";
                entry.timerDurationOverrideTicks = 0;
            }
            case TIMER_CANCEL -> {
                entry.timerId = "phase5.timer";
                entry.timerTargetMode = "context_player";
                entry.timerMissingBehavior = "fail_if_missing";
            }
        }
        return entry;
    }

    private static List<String> supportedIdsInSchemaOrder(List<String> actionOrder, ActionOwnerCapability capability) {
        List<String> result = new ArrayList<>();
        for (String id : actionOrder) {
            ActionSchema schema = ActionSchemaRegistry.findById(id).orElseThrow();
            if (capability.supports(schema.actionType())) {
                result.add(id);
            }
        }
        return List.copyOf(result);
    }

    private static String functionBody(String text, String name) {
        int start = text.indexOf(name);
        if (start < 0) {
            throw new AssertionError("Missing JS function " + name);
        }
        int open = text.indexOf('{', start);
        if (open < 0) {
            throw new AssertionError("Missing JS function body " + name);
        }
        int depth = 0;
        for (int index = open; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, index + 1);
                }
            }
        }
        throw new AssertionError("Unclosed JS function " + name);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> actionEditorData() throws Exception {
        Method method = WebAdminActionSchemaScripts.class.getDeclaredMethod("actionEditorDataForTest");
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            result.add(string(item));
        }
        return List.copyOf(result);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return Integer.MIN_VALUE;
            }
        }
        return Integer.MIN_VALUE;
    }
}
