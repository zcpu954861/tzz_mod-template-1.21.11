package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.action.schema.ActionCapabilityMatrix;
import com.zcpu.tzzmod.action.schema.ActionOwnerCapability;
import com.zcpu.tzzmod.action.schema.ActionSchema;
import com.zcpu.tzzmod.action.schema.ActionSchemaRegistry;
import com.zcpu.tzzmod.webadmin.WebAdminActionSchemaScripts;
import com.zcpu.tzzmod.webadmin.service.WebAdminHelpCatalogService;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public final class WebAdminTypedActionHelpGuardTest {
    private WebAdminTypedActionHelpGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        CodeQualityGuardSupport.GuardReport report =
                new CodeQualityGuardSupport.GuardReport("9.2 typed action help/docs consistency guard");
        run(report);
        report.printAndFail();
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws Exception {
        Map<String, Object> catalog = new WebAdminHelpCatalogService().catalog();
        report.require(Boolean.TRUE.equals(catalog.get("readOnly")), "Help catalog must remain read-only");
        report.require(Boolean.TRUE.equals(catalog.get("noWriteApi")), "Help catalog must expose no write API");
        report.require(Boolean.TRUE.equals(catalog.get("copyOnly")), "Help examples must remain copy-only");
        report.require(Boolean.FALSE.equals(catalog.get("worldScoped")), "Help catalog must remain world independent");

        List<?> topics = list(catalog.get("topics"));
        List<?> examples = list(catalog.get("examples"));
        List<?> troubleshooting = list(catalog.get("troubleshooting"));
        List<?> glossary = list(catalog.get("glossary"));
        Map<?, ?> typedTopic = find(topics, "action.typed-schema-capability");
        Map<?, ?> actionTopic = find(topics, "action.config-basics");
        report.require(typedTopic != null, "Typed action schema/capability help topic must exist");
        report.require(actionTopic != null, "Action config help topic must exist");
        report.require(find(examples, "example.typed-action-owner-bucket") != null,
                "Typed action owner/bucket example must exist");
        report.require(find(troubleshooting, "trouble.action-type-unavailable") != null,
                "Typed action unavailable troubleshooting entry must exist");
        for (String termId : List.of(
                "typed-action",
                "action-type",
                "action-owner-type",
                "action-capability-matrix",
                "action-schema-registry",
                "action-config-owner",
                "owner-bucket",
                "explicit-non-owner",
                "fail-closed"
        )) {
            report.require(find(glossary, termId) != null, "Typed action glossary term must exist: " + termId);
        }

        String typedHelp = text(typedTopic) + "\n" + text(actionTopic);
        String catalogText = text(catalog);
        String repoDocs = typedActionRepoDocs();
        String helpDocs = helpDocs();
        String docs = repoDocs + "\n" + helpDocs;

        report.requireContains(catalogText, "9.2 Typed Actions 说明来自 ActionSchemaRegistry / ActionCapabilityMatrix",
                "Help catalog must state typed action source of truth");
        report.requireContains(typedHelp, "帮助和文档以 ActionSchemaRegistry / ActionCapabilityMatrix 为事实来源",
                "Typed action help topic must state docs source of truth");
        report.requireContains(typedHelp, "文档不得与 registry / matrix 漂移",
                "Typed action help topic must state docs drift guard");
        report.requireContains(typedHelp, "ActionValidationService",
                "Typed action help must identify backend validation authority");
        report.requireContains(typedHelp, "unknown action type",
                "Typed action help must explain unknown action fail-closed");
        report.requireContains(typedHelp, "Timer outputChannel",
                "Typed action help must explain timer outputChannel boundary");
        report.requireContains(typedHelp, "maxActions=64",
                "Typed action help must document current max action boundary");

        report.metric("typed_action_help.action_type_count", ActionSchemaRegistry.schemas().size());
        for (ActionSchema schema : ActionSchemaRegistry.schemas()) {
            report.requireContains(typedHelp, schema.id(), "Help must cover current ActionType id " + schema.id());
            report.requireContains(typedHelp, schema.displayName(), "Help must cover ActionType displayName " + schema.id());
            report.requireContains(repoDocs, schema.id(), "9.2 repo docs must cover current ActionType id " + schema.id());
            for (String fieldId : schema.fields().stream().map(field -> field.id()).toList()) {
                report.requireContains(repoDocs, fieldId,
                        "9.2 schema docs must cover current field id " + schema.id() + "." + fieldId);
            }
        }

        report.metric("typed_action_help.owner_count", ActionCapabilityMatrix.capabilities().size());
        for (ActionOwnerCapability capability : ActionCapabilityMatrix.capabilities()) {
            String ownerId = capability.ownerType().id();
            report.requireContains(typedHelp, ownerId, "Help must cover current ActionConfig owner id " + ownerId);
            report.requireContains(typedHelp, capability.listFieldName(),
                    "Help must cover current owner list field " + ownerId);
            report.requireContains(repoDocs, ownerId, "9.2 repo docs must cover owner id " + ownerId);
            report.requireContains(repoDocs, capability.listFieldName(),
                    "9.2 repo docs must cover owner list field " + ownerId);
            report.requireContains(repoDocs, capability.actionConditionTargetType().name(),
                    "9.2 repo docs must cover owner action condition target " + ownerId);
            report.requireContains(repoDocs, "maxActions=64", "9.2 repo docs must document maxActions=64");
            for (ActionSchema schema : ActionSchemaRegistry.schemas()) {
                report.require(capability.supports(schema.actionType()),
                        "Current owner must support current action type: " + ownerId + " -> " + schema.id());
            }
        }

        for (String nonOwner : explicitNonOwners()) {
            report.requireContains(typedHelp, nonOwner, "Help must cover explicit non-owner " + nonOwner);
            report.requireContains(repoDocs, nonOwner, "9.2 repo docs must cover explicit non-owner " + nonOwner);
            report.require(ActionCapabilityMatrix.findByOwnerId(nonOwner).isEmpty(),
                    "Explicit non-owner must not be exposed by backend matrix: " + nonOwner);
        }

        for (String marker : List.of(
                "9.2 Phase 6 typed action help coverage",
                "docs derive from ActionSchemaRegistry and ActionCapabilityMatrix",
                "Help Center remains read-only and world-independent",
                "typed action help covers every current ActionType",
                "typed action help covers every current ActionConfig owner",
                "explicit typed action non-owners: vbd_trigger, item_submit, container_change, branch",
                "does not add ActionType, owner, runtime behavior, WebAdmin API, save payload or snapshot storage",
                "docs must not diverge from registry / matrix"
        )) {
            report.requireContains(docs, marker, "Phase 6 typed action docs marker");
        }
        for (String forbidden : List.of(
                "POST /api/webadmin/help",
                "PATCH /api/webadmin/help",
                "DELETE /api/webadmin/help",
                "new typed action sequence runtime",
                "new Program Model store",
                "new Rich Text Builder"
        )) {
            report.require(!catalogText.contains(forbidden), "Help catalog must not introduce forbidden marker " + forbidden);
        }
    }

    private static String typedActionRepoDocs() throws Exception {
        return String.join("\n",
                CodeQualityGuardSupport.read("docs/TYPED_ACTIONS_ROADMAP_9_2.md"),
                CodeQualityGuardSupport.read("docs/ACTION_CAPABILITY_MATRIX_9_2.md"),
                CodeQualityGuardSupport.read("docs/ACTION_SCHEMA_DESIGN_9_2.md"),
                CodeQualityGuardSupport.read("docs/TYPED_ACTIONS_AUDIT_9_2.md"),
                CodeQualityGuardSupport.read("docs/PROGRAM_MODEL_BOUNDARY_9_2.md")
        );
    }

    private static String helpDocs() throws Exception {
        return String.join("\n",
                CodeQualityGuardSupport.read("docs/WEBADMIN_HELP_EXAMPLE_CENTER_8_17_CURRENT_CONTEXT.md"),
                CodeQualityGuardSupport.read("docs/WEBADMIN_HELP_CAPABILITY_MATRIX_8_17.md")
        );
    }

    @SuppressWarnings("unchecked")
    private static List<String> explicitNonOwners() throws Exception {
        Method method = WebAdminActionSchemaScripts.class.getDeclaredMethod("actionEditorDataForTest");
        method.setAccessible(true);
        Map<String, Object> data = (Map<String, Object>) method.invoke(null);
        return stringList(data.get("explicitNonOwners"));
    }

    private static Map<?, ?> find(List<?> items, String id) {
        for (Object item : items) {
            if (item instanceof Map<?, ?> map && id.equals(string(map.get("id")))) {
                return map;
            }
        }
        return null;
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static List<String> stringList(Object value) {
        return list(value).stream().map(WebAdminTypedActionHelpGuardTest::string).toList();
    }

    private static String text(Object value) {
        StringBuilder builder = new StringBuilder();
        appendText(builder, value);
        return builder.toString();
    }

    private static void appendText(StringBuilder builder, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Object child : map.values()) {
                appendText(builder, child);
            }
            return;
        }
        if (value instanceof Iterable<?> values) {
            for (Object child : values) {
                appendText(builder, child);
            }
            return;
        }
        builder.append(' ').append(value);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
