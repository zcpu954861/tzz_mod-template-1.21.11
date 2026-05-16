package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionGroupMode;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.condition.runtime.ConditionGroupCompatibilityService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.write.WebAdminValidationError;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WebAdminConditionGateConfigTest {
    private WebAdminConditionGateConfigTest() {
    }

    public static void run() throws Exception {
        testBackendRejectsMissingDisabledInvalidAndIncompatibleGateBindings();
    }

    private static void testBackendRejectsMissingDisabledInvalidAndIncompatibleGateBindings() throws Exception {
        Path storePath = Files.createTempDirectory("tzz-condition-gate-config").resolve(WebAdminConditionGroupStore.FILE_NAME);
        WebAdminConditionGroupStore.ConditionGroupFile file = new WebAdminConditionGroupStore.ConditionGroupFile();
        file.groups.put("allow", entry("allow", definition("allow", ConditionNode.leaf("allow", ConditionNodeType.ALWAYS_TRUE)), true));
        file.groups.put("disabled", entry("disabled", definition("disabled", ConditionNode.leaf("disabled", ConditionNodeType.ALWAYS_TRUE)), false));
        file.groups.put("invalid", entry("invalid", definition("invalid", ConditionNode.group("root", ConditionGroupMode.AND, List.of())), true));
        file.groups.put("player", entry("player", definition("player", ConditionNode.leaf("player", ConditionNodeType.PLAYER_EXISTS)), true));
        file.groups.put("container", entry("container", definition("container", ConditionNode.leaf("container", ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES,
                new com.zcpu.tzzmod.condition.ConditionNodeConfig(Map.of(
                        "containerKey", "container",
                        "slot", "0",
                        "itemId", "minecraft:stone",
                        "countOperator", "gte",
                        "count", "1"
                )))), true));
        requireTrue(WebAdminConditionGroupStore.save(storePath, file), "seed backend gate condition groups");

        WebAdminVirtualBlockDeviceNativeTriggerService service = new WebAdminVirtualBlockDeviceNativeTriggerService(
                null,
                new WebAdminWriteSecurityService(),
                null,
                storePath
        );

        requireGateError(service, "missing", ConditionRuntimeTargetType.VBD_REDSTONE, "condition_group_missing", "不存在");
        requireGateError(service, "disabled", ConditionRuntimeTargetType.VBD_REDSTONE, "condition_group_disabled", "停用");
        requireGateError(service, "invalid", ConditionRuntimeTargetType.VBD_REDSTONE, "condition_group_validation_failed", "校验失败");
        requireGateError(service, "player", ConditionRuntimeTargetType.VBD_REDSTONE, "condition_group_incompatible", "不兼容");

        List<WebAdminValidationError> errors = new ArrayList<>();
        service.validateGateBinding(null, errors, "interactionConditionGroupId", "player", ConditionRuntimeTargetType.VBD_INTERACTION);
        requireTrue(errors.isEmpty(), "compatible player group accepted by interaction gate backend");
        service.validateGateBinding(null, errors, "itemSubmitConditionGroupId", "player", ConditionRuntimeTargetType.ITEM_SUBMIT);
        requireTrue(errors.isEmpty(), "compatible player group accepted by itemSubmit gate backend");
        service.validateGateBinding(null, errors, "redstoneConditionGroupId", "allow", ConditionRuntimeTargetType.VBD_REDSTONE);
        requireTrue(errors.isEmpty(), "always_true group accepted by redstone gate backend");

        ConditionGroupCompatibilityService compatibility = new ConditionGroupCompatibilityService();
        requireGateError(service, "container", ConditionRuntimeTargetType.CONTAINER_OPEN, "condition_group_incompatible", "容器快照");
        requireGateError(service, "container", ConditionRuntimeTargetType.CONTAINER_CLOSE, "condition_group_incompatible", "容器快照");
        errors.clear();
        service.validateGateBinding(null, errors, "containerOpenConditionGroupId", "container", ConditionRuntimeTargetType.CONTAINER_OPEN,
                compatibility.profile(ConditionRuntimeTargetType.CONTAINER_OPEN, true));
        requireTrue(errors.isEmpty(), "Inventory container open accepts container_slot_item_matches group");
        service.validateGateBinding(null, errors, "containerCloseConditionGroupId", "container", ConditionRuntimeTargetType.CONTAINER_CLOSE,
                compatibility.profile(ConditionRuntimeTargetType.CONTAINER_CLOSE, true));
        requireTrue(errors.isEmpty(), "Inventory container close accepts container_slot_item_matches group");
        service.validateGateBinding(null, errors, "containerOpenConditionGroupId", "container", ConditionRuntimeTargetType.CONTAINER_OPEN,
                compatibility.profile(ConditionRuntimeTargetType.CONTAINER_OPEN, false));
        requireTrue(!errors.isEmpty(), "non-Inventory container open rejects container_slot_item_matches group");
        requireContains(errors.getFirst().message(), "容器快照", "non-Inventory container open rejection explains snapshot");
        errors.clear();
        service.validateGateBinding(null, errors, "containerCloseConditionGroupId", "container", ConditionRuntimeTargetType.CONTAINER_CLOSE,
                compatibility.profile(ConditionRuntimeTargetType.CONTAINER_CLOSE, false));
        requireTrue(!errors.isEmpty(), "non-Inventory container close rejects container_slot_item_matches group");
        requireContains(errors.getFirst().message(), "容器快照", "non-Inventory container close rejection explains snapshot");

        WebAdminConditionGateBindingValidator receiverValidator = new WebAdminConditionGateBindingValidator(storePath);
        requireReceiverGateError(receiverValidator, "player", ConditionRuntimeTargetType.SIGNAL_LISTENER, "condition_group_incompatible", "触发玩家");
        requireReceiverGateError(receiverValidator, "player", ConditionRuntimeTargetType.ACTION_RELAY, "condition_group_incompatible", "触发玩家");
        errors.clear();
        receiverValidator.validate(null, errors, "conditionGroupId", "allow", ConditionRuntimeTargetType.SIGNAL_LISTENER);
        requireTrue(errors.isEmpty(), "always_true group accepted by SignalListener receiver gate backend");
        receiverValidator.validate(null, errors, "conditionGroupId", "allow", ConditionRuntimeTargetType.ACTION_RELAY);
        requireTrue(errors.isEmpty(), "always_true group accepted by ActionRelay receiver gate backend");
        receiverValidator.validate(null, errors, "enterConditionGroupId", "player", ConditionRuntimeTargetType.REGION_ENTER);
        requireTrue(errors.isEmpty(), "player group accepted by Region enter gate backend");
        receiverValidator.validate(null, errors, "exitConditionGroupId", "player", ConditionRuntimeTargetType.REGION_EXIT);
        requireTrue(errors.isEmpty(), "player group accepted by Region exit gate backend");
        receiverValidator.validate(null, errors, "stayConditionGroupId", "player", ConditionRuntimeTargetType.REGION_STAY);
        requireTrue(errors.isEmpty(), "player group accepted by Region stay gate backend");
    }

    private static void requireGateError(
            WebAdminVirtualBlockDeviceNativeTriggerService service,
            String groupId,
            ConditionRuntimeTargetType targetType,
            String expectedCode,
            String expectedChineseMessage
    ) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        service.validateGateBinding(null, errors, "conditionGroupId", groupId, targetType);
        requireTrue(!errors.isEmpty(), "backend rejects gate binding: " + groupId);
        WebAdminValidationError error = errors.getFirst();
        requireEquals(expectedCode, error.code(), "backend gate rejection code: " + groupId);
        requireContains(error.message(), expectedChineseMessage, "backend gate rejection Chinese message: " + groupId);
    }

    private static void requireReceiverGateError(
            WebAdminConditionGateBindingValidator validator,
            String groupId,
            ConditionRuntimeTargetType targetType,
            String expectedCode,
            String expectedChineseMessage
    ) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        validator.validate(null, errors, "conditionGroupId", groupId, targetType);
        requireTrue(!errors.isEmpty(), "8.7 backend rejects receiver gate binding: " + groupId + " target=" + targetType.id());
        WebAdminValidationError error = errors.getFirst();
        requireEquals(expectedCode, error.code(), "8.7 receiver backend gate rejection code: " + groupId);
        requireContains(error.message(), expectedChineseMessage, "8.7 receiver backend gate rejection Chinese message: " + groupId);
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
        return new ConditionGroupDefinition(id, 1, id, "", List.of(), root);
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
}
