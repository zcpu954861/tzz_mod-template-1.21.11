package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionGroupMode;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeConfig;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.condition.runtime.ConditionGroupCompatibilityProfile;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.condition.state.StateVariableMutationOperation;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableTargetMode;
import com.zcpu.tzzmod.condition.state.StateVariableType;
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
        testControlledStateActionDiagnostics();
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
                binding("SIGNAL_LISTENER", "listener-player", "player", ConditionRuntimeTargetType.SIGNAL_LISTENER),
                binding("SIGNAL_LISTENER_ACTION", "listener:listener-1:action:0", "missing", ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION),
                binding("ACTION_RELAY_ACTION", "relay:minecraft:overworld@1,2,3:action:0", "disabled", ConditionRuntimeTargetType.ACTION_RELAY_ACTION),
                binding("REGION_CONTROLLER_ACTION", "region:region-1:enter:action:0", "invalid", ConditionRuntimeTargetType.REGION_ENTER_ACTION),
                binding("SIGNAL_LISTENER_ACTION", "listener:listener-player:action:1", "player", ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION)
        ));

        requireIssue(issues, "condition-runtime-missing-group", "ERROR", "不存在", "重新绑定");
        requireIssue(issues, "condition-runtime-disabled-group", "WARNING", "已停用", "启用条件组");
        requireIssue(issues, "condition-runtime-invalid-group", "ERROR", "校验失败", "修复无效节点");
        requireIssue(issues, "condition-runtime-definition-missing", "ERROR", "groupDefinition", "gate");
        requireIssue(issues, "condition-runtime-incompatible-group", "ERROR", "触发玩家", "改绑");
        requireTrue(issues.stream().anyMatch(issue -> issue.relatedObjectType().equals("SIGNAL_LISTENER_ACTION")
                        && issue.id().contains("condition-runtime-missing-group")),
                "doctor reports missing action condition group");
        requireTrue(issues.stream().anyMatch(issue -> issue.relatedObjectType().equals("ACTION_RELAY_ACTION")
                        && issue.id().contains("condition-runtime-disabled-group")),
                "doctor reports disabled action condition group");
        requireTrue(issues.stream().anyMatch(issue -> issue.relatedObjectType().equals("REGION_CONTROLLER_ACTION")
                        && issue.id().contains("condition-runtime-invalid-group")),
                "doctor reports invalid region action condition group");
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
                binding("ACTION_RELAY_ACTION", "relay-state:action:0", "context_player_state", ConditionRuntimeTargetType.ACTION_RELAY_ACTION),
                binding("VIRTUAL_BLOCK_DEVICE", "vbd-container", "container", ConditionRuntimeTargetType.VBD_INTERACTION),
                binding("SIGNAL_LISTENER_ACTION", "listener-container:action:0", "container", ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION),
                binding("REGION_CONTROLLER", "region-inventory", "inventory", ConditionRuntimeTargetType.REGION_ENTER),
                binding("REGION_CONTROLLER", "region-item-stack", "item_stack", ConditionRuntimeTargetType.REGION_ENTER),
                binding("REGION_CONTROLLER", "region-signal-history", "signal_history", ConditionRuntimeTargetType.REGION_STAY),
                binding("REGION_CONTROLLER_ACTION", "region-signal-history:stay:action:0", "signal_history", ConditionRuntimeTargetType.REGION_STAY_ACTION)
        ));

        requireIssue(issues, "condition-runtime-incompatible-group", "ERROR", "context_player", "改绑");
        requireIssue(issues, "condition-runtime-incompatible-group", "ERROR", "容器快照", "改绑");
        requireIssue(issues, "condition-runtime-incompatible-group", "ERROR", "背包快照", "改绑");
        requireIssue(issues, "condition-runtime-incompatible-group", "ERROR", "物品快照", "改绑");
        requireIssue(issues, "condition-runtime-incompatible-group", "ERROR", "信号历史快照", "改绑");
        requireTrue(issues.stream().noneMatch(issue -> issue.message().contains("SignalReceiver")
                        || issue.message().contains("Signal Join")
                        || issue.message().contains("Barrier")
                        || issue.message().contains("Aggregator")),
                "doctor does not report deferred SignalReceiver or Signal Join / Barrier / Aggregator as missing errors");
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
                binding("SIGNAL_LISTENER_ACTION", "listener:listener-blank:action:0", "", ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION),
                binding("ACTION_RELAY", "relay-allow", "allow", ConditionRuntimeTargetType.ACTION_RELAY)
        ));

        requireEquals(1, issues.size(), "doctor only reports always_false warning; blank gate and valid gate have no issue");
        WebAdminDtos.DoctorIssueDto issue = issues.getFirst();
        requireContains(issue.id(), "condition-runtime-always-false-node", "always_false diagnostic code");
        requireEquals("WARNING", issue.severity(), "always_false diagnostic severity");
        requireContains(issue.message(), "always_false", "always_false diagnostic message");
        requireContains(issue.suggestion(), "确认这是预期", "always_false diagnostic suggestion");
    }

    private static void testControlledStateActionDiagnostics() {
        WebAdminConditionRuntimeDoctorService service = new WebAdminConditionRuntimeDoctorService();
        List<WebAdminDtos.DoctorIssueDto> issues = service.diagnoseStateActions(List.of(
                stateBinding("SIGNAL_LISTENER_ACTION", "listener:valid-global:action:0",
                        ActionConfig.stateVariable(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "game.ready", StateVariableType.BOOLEAN, "true", 0, true, "", ""),
                        ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION),
                stateBinding("REGION_CONTROLLER_ACTION", "region:valid-context:enter:action:0",
                        ActionConfig.stateVariable(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.PLAYER, StateVariableTargetMode.CONTEXT_PLAYER, "", "player.ready", StateVariableType.BOOLEAN, "true", 0, true, "", ""),
                        ConditionRuntimeTargetType.REGION_ENTER_ACTION),
                stateBinding("SIGNAL_LISTENER_ACTION", "listener:one:action:0",
                        ActionConfig.stateVariable(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "", StateVariableType.BOOLEAN, "true", 0, true, "", ""),
                        ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION),
                stateBinding("SIGNAL_LISTENER_ACTION", "listener:one:action:1",
                        ActionConfig.stateVariable(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.PLAYER, StateVariableTargetMode.CONTEXT_PLAYER, "", "player.ready", StateVariableType.BOOLEAN, "true", 0, true, "", ""),
                        ConditionRuntimeTargetType.SIGNAL_LISTENER_ACTION),
                stateBinding("REGION_CONTROLLER_ACTION", "region:one:enter:action:0",
                        ActionConfig.stateVariable(StateVariableMutationOperation.SET_VARIABLE, StateVariableScope.PLAYER, StateVariableTargetMode.EXPLICIT_TARGET, "", "player.ready", StateVariableType.BOOLEAN, "true", 0, true, "", ""),
                        ConditionRuntimeTargetType.REGION_ENTER_ACTION),
                stateBinding("ACTION_RELAY_ACTION", "relay:one:action:0",
                        new ActionConfig(ActionConfig.stateVariable(StateVariableMutationOperation.INCREMENT_VARIABLE, StateVariableScope.GLOBAL, StateVariableTargetMode.GLOBAL, "", "count", StateVariableType.STRING, "", 1, false, "", "").type(), "", true, false, 0, false, "", "increment_variable", "GLOBAL", "global", "", "count", "STRING", "", 1, false, ""),
                        ConditionRuntimeTargetType.ACTION_RELAY_ACTION)
        ));

        requireIssue(issues, "state-action-empty-key", "ERROR", "状态变量动作配置无效", "结构化字段");
        requireIssue(issues, "state-action-context-player-without-player", "ERROR", "context_player", "explicit_target");
        requireIssue(issues, "state-action-explicit-target-missing", "ERROR", "显式目标", "结构化字段");
        requireIssue(issues, "state-action-operation-type-mismatch", "ERROR", "INTEGER", "结构化字段");
        requireTrue(issues.stream().noneMatch(issue -> issue.relatedObjectId().contains("valid-global")
                        || issue.relatedObjectId().contains("valid-context")),
                "valid global state action and Region context_player action produce no Doctor issue");
        requireTrue(issues.stream().allMatch(issue -> containsChinese(issue.title()) && containsChinese(issue.message()) && containsChinese(issue.suggestion())),
                "state action doctor diagnostics use Chinese");
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

    private static WebAdminConditionRuntimeDoctorService.StateActionBinding stateBinding(
            String targetType,
            String targetId,
            ActionConfig action,
            ConditionRuntimeTargetType runtimeTargetType
    ) {
        return new WebAdminConditionRuntimeDoctorService.StateActionBinding(targetType, targetId, action, runtimeTargetType, "target:" + targetId);
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
