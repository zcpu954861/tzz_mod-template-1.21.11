package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.action.schema.ActionCapabilityMatrix;
import com.zcpu.tzzmod.action.schema.ActionOwnerCapability;
import com.zcpu.tzzmod.action.schema.ActionSchema;
import com.zcpu.tzzmod.action.schema.ActionSchemaRegistry;
import com.zcpu.tzzmod.webadmin.WebAdminFrontendAssets;
import java.util.List;

public final class WebAdminActionEditorFrontendGuardTest {
    private static final List<String> CURRENT_ACTION_IDS = List.of(
            "command",
            "message",
            "sound",
            "signal",
            "state_variable",
            "timer_start",
            "timer_cancel"
    );
    private static final List<String> OWNER_IDS = List.of(
            "signal_listener",
            "action_relay",
            "region_enter",
            "region_exit",
            "region_stay",
            "timer_on_start",
            "timer_on_tick",
            "timer_on_complete",
            "timer_on_cancel"
    );
    private static final List<String> NON_OWNER_IDS = List.of(
            "vbd_trigger",
            "item_submit",
            "container_change",
            "branch"
    );

    private WebAdminActionEditorFrontendGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        CodeQualityGuardSupport.GuardReport report =
                new CodeQualityGuardSupport.GuardReport("9.2 typed action editor frontend guard");
        run(report);
        report.printAndFail();
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws Exception {
        String appJs = WebAdminFrontendAssets.appJs();
        String pageFacade = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendPageScripts.java");
        String schemaScripts = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminActionSchemaScripts.java");
        String fieldRenderScripts = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminActionFieldRenderScripts.java");
        String logicChainEditor = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminLogicChainEditorScripts.java");
        String logicChainNodePanel = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminLogicChainNodePanelScripts.java");
        report.metric("typed_action_editor.app_js_markers", CodeQualityGuardSupport.count(appJs, "data-typed-action-schema-renderer"));
        requireSchemaExport(report, appJs, pageFacade);
        requireOwnerMatrixExport(report, appJs);
        requireRendererMarkers(report, appJs, fieldRenderScripts);
        requireOwnerFiltering(report, appJs, schemaScripts, logicChainEditor);
        requireLogicChainDraftRenderer(report, appJs, logicChainNodePanel);
        requireDraftPreservationMarkers(report, appJs);
        requireJavaMatrixConsistency(report);
    }

    private static void requireSchemaExport(CodeQualityGuardSupport.GuardReport report, String appJs, String pageFacade) {
        report.requireContains(pageFacade, "WebAdminActionSchemaScripts.appJs()",
                "Action schema script module must be loaded before owner editors");
        report.requireContains(pageFacade, "WebAdminActionFieldRenderScripts.appJs()",
                "Action field renderer script module must be loaded before owner editors");
        requireOrdered(report, pageFacade,
                "WebAdminActionSchemaScripts.appJs()",
                "WebAdminActionFieldRenderScripts.appJs()",
                "WebAdminFrontendDeviceEditorScripts.appJs()");
        report.requireContains(appJs, "data-typed-action-schema-export=\"true\"",
                "Typed action schema export marker");
        report.requireContains(appJs, "function typedActionEditorData(){return Object.freeze",
                "Typed action editor data must be static and readonly at the top-level");
        report.requireContains(appJs, "const TZZ_ACTION_EDITOR_DATA = typedActionEditorData();",
                "Typed action editor top-level constant");
        report.requireContains(appJs, "function actionSchemaByType",
                "Action schema lookup helper");
        report.requireContains(appJs, "function actionTypeOptions",
                "Action type options helper");
        for (String id : CURRENT_ACTION_IDS) {
            report.requireContains(appJs, "\"" + id + "\"", "Action schema export must include " + id);
        }
        for (String fieldType : List.of(
                "channel_picker",
                "state_variable_picker",
                "condition_group_picker",
                "player_target_mode"
        )) {
            report.requireContains(appJs, "\"" + fieldType + "\"",
                    "Action schema export must include field type " + fieldType);
        }
    }

    private static void requireOwnerMatrixExport(CodeQualityGuardSupport.GuardReport report, String appJs) {
        for (String id : OWNER_IDS) {
            report.requireContains(appJs, "\"" + id + "\"", "Owner capability export must include " + id);
        }
        for (String id : NON_OWNER_IDS) {
            report.requireContains(appJs, "\"" + id + "\"", "Non-owner negative marker must include " + id);
        }
        report.requireContains(appJs, "function actionOwnerId(ownerType,bucket='')",
                "Owner id adapter must map old UI owner/bucket names to matrix ids");
        report.requireContains(appJs, "function actionSupportedTypesForOwner",
                "Owner capability filtering helper");
        report.requireContains(appJs, "function typedActionUnsupportedOwnerIds",
                "Frontend negative owner helper");
    }

    private static void requireRendererMarkers(
            CodeQualityGuardSupport.GuardReport report,
            String appJs,
            String fieldRenderScripts
    ) {
        for (String marker : List.of(
                "function renderTypedActionValueEditor",
                "data-action-schema-field-render",
                "data-action-owner-capability-filter",
                "data-action-no-raw-json-primary-editor",
                "data-typed-action-channel-picker",
                "data-typed-action-state-variable-picker",
                "data-typed-action-player-target-mode"
        )) {
            report.requireContains(appJs, marker, "Typed action editor renderer marker");
        }
        report.requireContains(appJs, "actionConditionGatePicker",
                "Owner action editors must keep the existing condition group picker path");
        report.require(!appJs.contains("data-action-raw-json-primary-editor"),
                "Unified action editor must not add a raw JSON primary editor");
        report.require(!fieldRenderScripts.contains("<datalist"),
                "Typed action renderer must use the project channel-combo path instead of datalist");
        report.require(!fieldRenderScripts.contains("api("),
                "Typed action renderer must not call WebAdmin APIs directly");
        report.require(!fieldRenderScripts.contains("fetch("),
                "Typed action renderer must not call fetch directly");
    }

    private static void requireOwnerFiltering(
            CodeQualityGuardSupport.GuardReport report,
            String appJs,
            String schemaScripts,
            String logicChainEditor
    ) {
        report.requireContains(appJs, "actionTypeOptions(action.type,'action_relay')",
                "ActionRelay action select must use owner capability filtering");
        report.requireContains(appJs, "signalListenerActionTypeOptions(value){return actionTypeOptions(value,'signal_listener');}",
                "SignalListener action select must use owner capability filtering");
        report.requireContains(appJs, "actionOwnerId('region_controller'",
                "Region action select must map trigger to region owner capability");
        report.requireContains(appJs, "actionOwnerId('timer',bucket)",
                "Timer action select must map bucket to timer owner capability");
        report.requireContains(logicChainEditor, "return actionSupportedTypesForOwner(actionOwnerId(owner,bucket));",
                "Logic Chain action append/edit must use matrix-backed owner filtering");
        report.requireContains(logicChainEditor, "if(!types.length)return '';return actionTypeOptions(value,'',types);",
                "Logic Chain explicit empty owner support must not be widened by actionTypeOptions fallback");
        report.requireContains(schemaScripts, "if(Array.isArray(fallbackTypes))return fallbackTypes.map(normalizeActionTypeId).filter(type=>!!actionSchemaByType(type));\n                  return [];",
                "Unknown typed action owners must fail closed instead of receiving every action type");
        report.requireContains(schemaScripts, "const canPreserveSelected=!!cap||(Array.isArray(fallbackTypes)&&fallbackTypes.length>0);",
                "Selected action preservation must only apply to known owner capabilities or explicit nonempty fallback lists");
        report.require(!logicChainEditor.contains("type!=='timer_start'||String(bucket||'').toLowerCase()!=='tick'"),
                "Logic Chain Timer tick must not hide timer_start when backend matrix supports it");
    }

    private static void requireLogicChainDraftRenderer(
            CodeQualityGuardSupport.GuardReport report,
            String appJs,
            String logicChainNodePanel
    ) {
        report.requireContains(logicChainNodePanel, "return renderTypedActionValueEditor('logic-chain-draft-action'",
                "Logic Chain new-node draft action editor must use the unified typed action renderer");
        report.requireContains(appJs, "data-logic-chain-draft-action-typed-fields",
                "Logic Chain draft action editor must carry typed field markers");
        report.require(!logicChainNodePanel.contains("if(type==='state_variable')return stateActionEditor('logic-chain-draft-action'"),
                "Logic Chain draft action editor must not keep the old hand-written state/timer/value branch");
        report.requireContains(appJs, "data-logic-chain-draft-action-condition-gate-picker",
                "Logic Chain draft action condition picker marker must stay present");
        report.requireContains(appJs, "data-logic-chain-action-append-condition-gate-picker",
                "Logic Chain append action condition picker marker must stay present");
        report.requireContains(appJs, "data-logic-chain-action-edit-condition-gate-picker",
                "Logic Chain existing action condition picker marker must stay present");
    }

    private static void requireDraftPreservationMarkers(CodeQualityGuardSupport.GuardReport report, String appJs) {
        for (String marker : List.of(
                "data-timer-validation-preserves-input",
                "data-action-modal-validation-preserves-scroll",
                "data-region-action-preserve-scroll",
                "data-logic-chain-draft-action-detail-editor"
        )) {
            report.requireContains(appJs, marker,
                    "Typed action editor validation/draft preservation marker");
        }
        report.requireContains(appJs, "showRegionControllerActionAddModal(captureRegionControllerModalUiState())",
                "Region action validation failure must preserve modal input and scroll state");
        report.requireContains(appJs, "showLogicChainActionAppendModal();return;",
                "Logic Chain action append validation failure must keep the draft modal open");
    }

    private static void requireJavaMatrixConsistency(CodeQualityGuardSupport.GuardReport report) {
        report.require(ActionSchemaRegistry.schemas().size() == CURRENT_ACTION_IDS.size(),
                "Action schema registry and frontend guard action id list must stay in sync");
        for (String id : CURRENT_ACTION_IDS) {
            ActionSchema schema = ActionSchemaRegistry.findById(id).orElse(null);
            report.require(schema != null, "Missing ActionSchema for " + id);
        }
        report.require(ActionCapabilityMatrix.capabilities().size() == OWNER_IDS.size(),
                "Action capability matrix owner count must stay in sync with frontend export");
        for (String ownerId : OWNER_IDS) {
            ActionOwnerCapability capability = ActionCapabilityMatrix.findByOwnerId(ownerId).orElse(null);
            report.require(capability != null, "Missing ActionOwnerCapability for " + ownerId);
            if (capability != null) {
                for (ActionType type : ActionType.values()) {
                    report.require(capability.supports(type), "Owner " + ownerId + " must support current action type " + type.id());
                }
            }
        }
        for (String nonOwner : NON_OWNER_IDS) {
            report.require(ActionCapabilityMatrix.findByOwnerId(nonOwner).isEmpty(),
                    "Non-owner must not become a backend ActionConfig owner in Phase 3: " + nonOwner);
        }
    }

    private static void requireOrdered(CodeQualityGuardSupport.GuardReport report, String text, String... needles) {
        int previous = -1;
        for (String needle : needles) {
            int current = text.indexOf(needle);
            report.require(current >= 0, "Missing ordered marker `" + needle + "`");
            if (current >= 0 && previous >= 0 && current <= previous) {
                report.fail("Typed action script module order changed near `" + needle + "`");
            }
            previous = current;
        }
    }
}
