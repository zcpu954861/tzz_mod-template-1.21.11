package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditEvent;
import com.zcpu.tzzmod.webadmin.write.WebAdminValidationError;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebAdminControlledStateActionServiceTest {
    private WebAdminControlledStateActionServiceTest() {
    }

    public static void run() {
        testValidTypedStateActionRoundTrip();
        testInvalidStateActionValidation();
        testAuditSummariesRedactStateValues();
        testReadonlyStateActionVisibility();
        testActionEntryDoesNotExposeRawJsonSurface();
    }

    private static void testValidTypedStateActionRoundTrip() {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = validStateEntry();
        List<WebAdminValidationError> errors = WebAdminActionRelayActionsService.validateActionEntries(List.of(entry));
        requireTrue(errors.isEmpty(), "valid state_variable action entry saves through shared WebAdmin validation errors=" + errors);

        ActionConfig action = WebAdminActionRelayActionsService.actionFromEntry(entry);
        requireEquals(ActionType.STATE_VARIABLE, action.type(), "actionFromEntry creates state action");
        requireEquals("", action.value(), "state action does not use legacy value/raw JSON");
        requireFalse(action.requiresOp(), "state action save path forces requiresOp false");
        requireFalse(action.notifyOps(), "state action save path forces notifyOps false");
        requireEquals("set_variable", action.stateOperation(), "state operation round trips");
        requireEquals("GLOBAL", action.stateScope(), "state scope round trips");
        requireEquals("global", action.stateTargetMode(), "state target mode round trips");
        requireEquals("game.ready", action.stateKey(), "state key round trips");
        requireEquals("BOOLEAN", action.stateValueType(), "state value type round trips");
        requireEquals("true", action.stateValue(), "state value round trips");
        requireTrue(action.stateCreateIfMissing(), "state createIfMissing round trips");

        Map<String, Object> dto = new LinkedHashMap<>();
        WebAdminActionRelayActionsService.putStateActionFields(dto, action);
        for (String key : List.of(
                "stateOperation",
                "stateScope",
                "stateTargetMode",
                "stateKey",
                "stateValueType",
                "stateValue",
                "stateDelta",
                "stateCreateIfMissing",
                "stateInitialValue",
                "stateActionSummary"
        )) {
            requireTrue(dto.containsKey(key), "state action DTO emits typed field " + key);
        }
        requireContains(String.valueOf(dto.get("stateActionSummary")), "设置变量", "state action DTO summary is Chinese");
    }

    private static void testInvalidStateActionValidation() {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry missingKey = validStateEntry();
        missingKey.stateKey = "";
        requireError(
                WebAdminActionRelayActionsService.validateActionEntries(List.of(missingKey)),
                "actions[0].stateKey",
                "missing_key",
                "empty key is rejected on WebAdmin save path"
        );

        WebAdminActionRelayActionsUpdateRequest.ActionEntry missingValueType = validStateEntry();
        missingValueType.stateOperation = "increment_variable";
        missingValueType.stateValueType = "";
        missingValueType.stateDelta = 1;
        requireError(
                WebAdminActionRelayActionsService.validateActionEntries(List.of(missingValueType)),
                "actions[0].stateValueType",
                "missing_value_type",
                "increment missing valueType is rejected on WebAdmin save path"
        );

        WebAdminActionRelayActionsUpdateRequest.ActionEntry malformedCreate = validStateEntry();
        malformedCreate.stateCreateIfMissing = "yes";
        requireError(
                WebAdminActionRelayActionsService.validateActionEntries(List.of(malformedCreate)),
                "actions[0].stateCreateIfMissing",
                "invalid_boolean",
                "malformed createIfMissing boolean is rejected"
        );

        WebAdminActionRelayActionsUpdateRequest.ActionEntry invalidDelta = validStateEntry();
        invalidDelta.stateOperation = "increment_variable";
        invalidDelta.stateValueType = "INTEGER";
        invalidDelta.stateDelta = "0";
        requireError(
                WebAdminActionRelayActionsService.validateActionEntries(List.of(invalidDelta)),
                "actions[0].stateDelta",
                "invalid_delta",
                "delta=0 is rejected instead of rewritten to 1"
        );
    }

    private static void testActionEntryDoesNotExposeRawJsonSurface() {
        for (Field field : WebAdminActionRelayActionsUpdateRequest.ActionEntry.class.getFields()) {
            String name = field.getName().toLowerCase(java.util.Locale.ROOT);
            requireFalse(name.contains("raw") || name.contains("json") || name.contains("script") || name.contains("nbt"),
                    "state action save DTO must not expose raw JSON/script/NBT field: " + field.getName());
        }
    }

    private static void testAuditSummariesRedactStateValues() {
        String secretValue = "secret-token-should-not-enter-webadmin-audit-summary";
        String secretInitial = "initial-secret-should-not-enter-webadmin-audit-summary";
        ActionConfig action = new ActionConfig(
                ActionType.STATE_VARIABLE,
                "",
                true,
                false,
                0,
                false,
                "",
                "set_variable",
                "GLOBAL",
                "global",
                "",
                "game.secret",
                "STRING",
                secretValue,
                0,
                true,
                secretInitial
        );
        requireContains(action.stateFingerprint(), secretValue, "internal state fingerprint still tracks exact state value");
        requireContains(action.stateFingerprint(), secretInitial, "internal state fingerprint still tracks exact initial value");
        requireFalse(action.stateAuditFingerprint().contains(secretValue), "audit fingerprint redacts stateValue");
        requireFalse(action.stateAuditFingerprint().contains(secretInitial), "audit fingerprint redacts stateInitialValue");
        requireContains(action.stateAuditFingerprint(), "<redacted length=", "audit fingerprint keeps value length only");

        for (Class<?> serviceClass : List.of(
                WebAdminActionRelayActionsService.class,
                WebAdminSignalListenerActionsService.class,
                WebAdminRegionControllerService.class
        )) {
            String summary = invokeAuditActionSummary(serviceClass, action);
            requireFalse(summary.contains(secretValue), serviceClass.getSimpleName() + " audit summary must redact stateValue");
            requireFalse(summary.contains(secretInitial), serviceClass.getSimpleName() + " audit summary must redact stateInitialValue");
            WebAdminAuditEvent event = new WebAdminAuditEvent(
                    "audit",
                    "now",
                    "editor",
                    "EDITOR",
                    "session",
                    "local",
                    "EDIT_ACTIONS",
                    serviceClass.getSimpleName(),
                    "target",
                    "target",
                    Map.of("actions", List.of(summary)),
                    Map.of("actions", List.of(summary)),
                    "success",
                    "",
                    "ok"
            );
            requireFalse(String.valueOf(event.beforeSummary()).contains(secretValue), serviceClass.getSimpleName() + " beforeSummary must not leak stateValue");
            requireFalse(String.valueOf(event.beforeSummary()).contains(secretInitial), serviceClass.getSimpleName() + " beforeSummary must not leak stateInitialValue");
            requireFalse(String.valueOf(event.afterSummary()).contains(secretValue), serviceClass.getSimpleName() + " afterSummary must not leak stateValue");
            requireFalse(String.valueOf(event.afterSummary()).contains(secretInitial), serviceClass.getSimpleName() + " afterSummary must not leak stateInitialValue");
        }
    }

    private static void testReadonlyStateActionVisibility() {
        ActionConfig action = new ActionConfig(
                ActionType.STATE_VARIABLE,
                "",
                true,
                false,
                0,
                false,
                "",
                "set_variable",
                "GLOBAL",
                "global",
                "",
                "game.ready",
                "BOOLEAN",
                "true",
                0,
                true,
                "false"
        );
        requireEquals("STATE_VARIABLE", WebAdminReadonlySupport.actionType(action), "readonly action list exposes STATE_VARIABLE type");
        String summary = WebAdminReadonlySupport.actionSummary(action);
        requireContains(summary, "state_variable:", "readonly action summary keeps state action prefix");
        requireContains(summary, "设置变量", "readonly action summary keeps Chinese state operation");
        requireFalse(summary.contains("stateValue"), "readonly action summary does not expose stateValue field name");
        requireFalse(summary.contains("stateInitialValue"), "readonly action summary does not expose stateInitialValue field name");
    }

    private static WebAdminActionRelayActionsUpdateRequest.ActionEntry validStateEntry() {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        entry.type = "state_variable";
        entry.value = "ignored legacy value";
        entry.enabled = Boolean.TRUE;
        entry.requiresOp = Boolean.TRUE;
        entry.cooldownTicks = 0;
        entry.notifyOps = Boolean.TRUE;
        entry.conditionGroupId = "";
        entry.stateOperation = "set_variable";
        entry.stateScope = "GLOBAL";
        entry.stateTargetMode = "global";
        entry.stateTargetId = "";
        entry.stateKey = "game.ready";
        entry.stateValueType = "BOOLEAN";
        entry.stateValue = "true";
        entry.stateDelta = 0;
        entry.stateCreateIfMissing = Boolean.TRUE;
        entry.stateInitialValue = "";
        return entry;
    }

    private static void requireError(List<WebAdminValidationError> errors, String field, String code, String message) {
        for (WebAdminValidationError error : errors) {
            if (field.equals(error.field()) && code.equals(error.code())) {
                requireTrue(containsChinese(error.message()), message + " Chinese message");
                return;
            }
        }
        throw new AssertionError(message + " missing field=" + field + " code=" + code + " errors=" + errors);
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireFalse(boolean condition, String message) {
        requireTrue(!condition, message);
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void requireContains(String haystack, String needle, String message) {
        requireTrue(haystack != null && haystack.contains(needle), message + " needle=" + needle + " haystack=" + haystack);
    }

    private static String invokeAuditActionSummary(Class<?> serviceClass, ActionConfig action) {
        try {
            Method method = serviceClass.getDeclaredMethod("auditActionSummary", ActionConfig.class);
            method.setAccessible(true);
            return String.valueOf(method.invoke(null, action));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("failed to call auditActionSummary for " + serviceClass.getSimpleName(), exception);
        }
    }

    private static boolean containsChinese(String value) {
        return value != null && value.codePoints().anyMatch((codePoint) -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }
}
