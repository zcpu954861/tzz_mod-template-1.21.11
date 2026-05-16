package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionGroupMode;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeConfig;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.condition.runtime.ConditionGroupCompatibilityProfile;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebAdminConditionRuntimeDoctorServiceTest {
    private WebAdminConditionRuntimeDoctorServiceTest() {
    }

    public static void run() {
        testMissingDisabledInvalidAndIncompatibleBindings();
        testContextSpecificCompatibilityDiagnostics();
        testContainerOpenCloseDynamicProfileDiagnostics();
        testAlwaysFalseWarningAndBlankGateNoIssue();
    }

    private static void testMissingDisabledInvalidAndIncompatibleBindings() {
        WebAdminConditionRuntimeDoctorService service = new WebAdminConditionRuntimeDoctorService();
        Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups = Map.of(
                "disabled", entry("disabled", definition("disabled", leaf(ConditionNodeType.ALWAYS_TRUE)), false),
                "invalid", entry("invalid", ConditionGroupDefinition.of("invalid", ConditionNode.group("root", ConditionGroupMode.AND, List.of())), true),
                "definition_missing", entry("definition_missing", null, true),
                "player", entry("player", definition("player", leaf(ConditionNodeType.PLAYER_EXISTS)), true)
        );

        List<WebAdminDtos.DoctorIssueDto> issues = service.diagnoseBindings(groups, List.of(
                binding("VIRTUAL_BLOCK_DEVICE", "vbd-missing", "missing", ConditionRuntimeTargetType.VBD_REDSTONE),
                binding("VIRTUAL_BLOCK_DEVICE", "vbd-disabled", "disabled", ConditionRuntimeTargetType.VBD_REDSTONE),
                binding("VIRTUAL_BLOCK_DEVICE", "vbd-invalid", "invalid", ConditionRuntimeTargetType.VBD_REDSTONE),
                binding("VIRTUAL_BLOCK_DEVICE", "vbd-definition-missing", "definition_missing", ConditionRuntimeTargetType.VBD_REDSTONE),
                binding("SIGNAL_LISTENER", "listener-player", "player", ConditionRuntimeTargetType.SIGNAL_LISTENER)
        ));

        requireIssue(issues, "condition-runtime-missing-group", "ERROR", "不存在", "重新绑定");
        requireIssue(issues, "condition-runtime-disabled-group", "WARNING", "已停用", "启用条件组");
        requireIssue(issues, "condition-runtime-invalid-group", "ERROR", "校验失败", "修复无效节点");
        requireIssue(issues, "condition-runtime-definition-missing", "ERROR", "groupDefinition", "gate");
        requireIssue(issues, "condition-runtime-incompatible-group", "ERROR", "触发玩家", "改绑");
        requireTrue(issues.stream().allMatch(issue -> containsChinese(issue.title()) && containsChinese(issue.message()) && containsChinese(issue.suggestion())),
                "doctor diagnostics use Chinese title/message/suggestion");
    }

    private static void testContextSpecificCompatibilityDiagnostics() {
        WebAdminConditionRuntimeDoctorService service = new WebAdminConditionRuntimeDoctorService();
        Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups = Map.of(
                "context_player_state", entry("context_player_state", definition("context_player_state", leaf(
                        ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
                        config("scope", "PLAYER", "key", "ready", "targetMode", "context_player", "expected", "true")
                )), true),
                "container", entry("container", definition("container", leaf(
                        ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES,
                        config("containerKey", "container", "slot", "0", "itemId", "minecraft:stone", "countOperator", "gte", "count", "1")
                )), true),
                "inventory", entry("inventory", definition("inventory", leaf(
                        ConditionNodeType.INVENTORY_CONTAINS_ITEM,
                        config("inventoryKey", "player_inventory", "itemId", "minecraft:stone", "countOperator", "gte", "count", "1")
                )), true),
                "item_stack", entry("item_stack", definition("item_stack", leaf(
                        ConditionNodeType.ITEM_STACK_EXISTS,
                        config("itemKey", "main_hand")
                )), true),
                "signal_history", entry("signal_history", definition("signal_history", leaf(
                        ConditionNodeType.SIGNAL_EVENT_COUNT_COMPARE,
                        config("signalHistoryKey", "history", "channel", "mission.start", "operator", "gte", "count", "1")
                )), true)
        );

        List<WebAdminDtos.DoctorIssueDto> issues = service.diagnoseBindings(groups, List.of(
                binding("ACTION_RELAY", "relay-state", "context_player_state", ConditionRuntimeTargetType.ACTION_RELAY),
                binding("VIRTUAL_BLOCK_DEVICE", "vbd-container", "container", ConditionRuntimeTargetType.VBD_INTERACTION),
                binding("REGION_CONTROLLER", "region-inventory", "inventory", ConditionRuntimeTargetType.REGION_ENTER),
                binding("REGION_CONTROLLER", "region-item-stack", "item_stack", ConditionRuntimeTargetType.REGION_ENTER),
                binding("REGION_CONTROLLER", "region-signal-history", "signal_history", ConditionRuntimeTargetType.REGION_STAY)
        ));

        requireIssue(issues, "condition-runtime-incompatible-group", "ERROR", "context_player", "改绑");
        requireIssue(issues, "condition-runtime-incompatible-group", "ERROR", "容器快照", "改绑");
        requireIssue(issues, "condition-runtime-incompatible-group", "ERROR", "背包快照", "改绑");
        requireIssue(issues, "condition-runtime-incompatible-group", "ERROR", "物品快照", "改绑");
        requireIssue(issues, "condition-runtime-incompatible-group", "ERROR", "信号历史快照", "改绑");
        requireTrue(issues.stream().noneMatch(issue -> issue.message().contains("SignalReceiver") || issue.message().contains("单条 Action")),
                "doctor does not report deferred SignalReceiver or single Action gate as missing errors");
    }

    private static void testContainerOpenCloseDynamicProfileDiagnostics() {
        WebAdminConditionRuntimeDoctorService service = new WebAdminConditionRuntimeDoctorService();
        Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups = Map.of(
                "container", entry("container", definition("container", leaf(
                        ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES,
                        config("containerKey", "container", "slot", "0", "itemId", "minecraft:stone", "countOperator", "gte", "count", "1")
                )), true)
        );

        List<WebAdminDtos.DoctorIssueDto> issues = service.diagnoseBindings(groups, List.of(
                bindingProfile("VIRTUAL_BLOCK_DEVICE", "inventory-container-open", "container", ConditionRuntimeTargetType.CONTAINER_OPEN, true),
                bindingProfile("VIRTUAL_BLOCK_DEVICE", "non-inventory-container-open", "container", ConditionRuntimeTargetType.CONTAINER_OPEN, false)
        ));

        requireEquals(1L, issues.stream().filter(issue -> issue.id().contains("condition-runtime-incompatible-group")).count(),
                "doctor respects dynamic container open/close compatibility profile");
        requireTrue(issues.getFirst().relatedObjectId().contains("non-inventory-container-open"),
                "doctor reports only container bindings without inventory snapshot");
        requireIssue(issues, "condition-runtime-incompatible-group", "ERROR", "容器快照", "改绑");
    }

    private static void testAlwaysFalseWarningAndBlankGateNoIssue() {
        WebAdminConditionRuntimeDoctorService service = new WebAdminConditionRuntimeDoctorService();
        Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups = Map.of(
                "always_false", entry("always_false", definition("always_false", leaf(ConditionNodeType.ALWAYS_FALSE)), true),
                "allow", entry("allow", definition("allow", leaf(ConditionNodeType.ALWAYS_TRUE)), true)
        );

        List<WebAdminDtos.DoctorIssueDto> issues = service.diagnoseBindings(groups, List.of(
                binding("VIRTUAL_BLOCK_DEVICE", "vbd-always-false", "always_false", ConditionRuntimeTargetType.VBD_REDSTONE),
                binding("SIGNAL_LISTENER", "listener-blank", "", ConditionRuntimeTargetType.SIGNAL_LISTENER),
                binding("ACTION_RELAY", "relay-allow", "allow", ConditionRuntimeTargetType.ACTION_RELAY)
        ));

        requireEquals(1, issues.size(), "doctor only reports always_false warning; blank gate and valid gate have no issue");
        WebAdminDtos.DoctorIssueDto issue = issues.getFirst();
        requireContains(issue.id(), "condition-runtime-always-false-node", "always_false diagnostic code");
        requireEquals("WARNING", issue.severity(), "always_false diagnostic severity");
        requireContains(issue.message(), "always_false", "always_false diagnostic message");
        requireContains(issue.suggestion(), "确认这是预期", "always_false diagnostic suggestion");
    }

    private static WebAdminConditionRuntimeDoctorService.Binding binding(
            String targetType,
            String targetId,
            String conditionGroupId,
            ConditionRuntimeTargetType runtimeTargetType
    ) {
        return new WebAdminConditionRuntimeDoctorService.Binding(targetType, targetId, conditionGroupId, runtimeTargetType, "target:" + targetId);
    }

    private static WebAdminConditionRuntimeDoctorService.Binding bindingProfile(
            String targetType,
            String targetId,
            String conditionGroupId,
            ConditionRuntimeTargetType runtimeTargetType,
            boolean containerSnapshot
    ) {
        return new WebAdminConditionRuntimeDoctorService.Binding(
                targetType,
                targetId,
                conditionGroupId,
                runtimeTargetType,
                "target:" + targetId,
                ConditionGroupCompatibilityProfile.forTarget(runtimeTargetType, containerSnapshot)
        );
    }

    private static WebAdminConditionGroupStore.ConditionGroupEntry entry(String id, ConditionGroupDefinition definition, boolean enabled) {
        WebAdminConditionGroupStore.ConditionGroupEntry entry = new WebAdminConditionGroupStore.ConditionGroupEntry();
        entry.id = id;
        entry.displayName = id;
        entry.enabled = enabled;
        entry.groupDefinition = definition;
        return entry;
    }

    private static ConditionGroupDefinition definition(String id, ConditionNode root) {
        return ConditionGroupDefinition.of(id, ConditionNode.group("root", ConditionGroupMode.AND, List.of(root)));
    }

    private static ConditionNode leaf(String type) {
        return leaf(type, ConditionNodeConfig.EMPTY);
    }

    private static ConditionNode leaf(String type, ConditionNodeConfig config) {
        return ConditionNode.leaf("node-" + type, type, config);
    }

    private static ConditionNodeConfig config(String... entries) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            values.put(entries[index], entries[index + 1]);
        }
        return new ConditionNodeConfig(values);
    }

    private static void requireIssue(
            List<WebAdminDtos.DoctorIssueDto> issues,
            String expectedCode,
            String expectedSeverity,
            String expectedMessagePart,
            String expectedSuggestionPart
    ) {
        for (WebAdminDtos.DoctorIssueDto issue : issues) {
            if (issue.id().contains(expectedCode)
                    && expectedSeverity.equals(issue.severity())
                    && issue.message().contains(expectedMessagePart)
                    && issue.suggestion().contains(expectedSuggestionPart)) {
                return;
            }
        }
        throw new AssertionError("missing doctor issue code=" + expectedCode + " severity=" + expectedSeverity
                + " messagePart=" + expectedMessagePart + " issues=" + issues);
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void requireContains(String haystack, String needle, String message) {
        requireTrue(haystack != null && haystack.contains(needle), message + " needle=" + needle + " haystack=" + haystack);
    }

    private static boolean containsChinese(String value) {
        if (value == null) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.UnicodeScript.of(value.charAt(index)) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }
}
