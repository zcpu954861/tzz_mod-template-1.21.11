package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.condition.ConditionEvaluator;
import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionGroupMode;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeConfig;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConditionGateServiceTest {
    private ConditionGateServiceTest() {
    }

    public static void run() throws Exception {
        testBlankConditionGroupSkipsWithoutStoreOrContext();
        testMissingDisabledInvalidAndIncompatibleGroupsFailClosed();
        testCorruptRuntimeGateStoreFailsClosed();
        testConditionTrueFalseAndExceptionBehavior();
        testRuntimeProfileMatchesInventoryContainerOpenCloseCapability();
        testSummaryShape();
    }

    private static void testBlankConditionGroupSkipsWithoutStoreOrContext() {
        AtomicBoolean loaderCalled = new AtomicBoolean(false);
        AtomicBoolean contextCalled = new AtomicBoolean(false);
        ConditionGateService service = new ConditionGateService(
                (id) -> {
                    loaderCalled.set(true);
                    return null;
                },
                new ConditionEvaluator(),
                new ConditionGroupCompatibilityService()
        );
        ConditionGateResult result = service.evaluate(null, new ConditionGateRequest(
                "",
                ConditionRuntimeTargetType.ITEM_SUBMIT,
                "vbd-1",
                () -> {
                    contextCalled.set(true);
                    return ConditionEvaluationContext.builder().build();
                }
        ));
        requireTrue(result.allowed(), "blank conditionGroupId allowed");
        requireTrue(result.skipped(), "blank conditionGroupId skipped");
        requireFalse(loaderCalled.get(), "blank conditionGroupId does not read store");
        requireFalse(contextCalled.get(), "blank conditionGroupId does not build EvaluationContext");
        requireEquals("condition_gate_skipped", result.code(), "blank gate code");
    }

    private static void testMissingDisabledInvalidAndIncompatibleGroupsFailClosed() {
        WebAdminConditionGroupStore.ConditionGroupEntry disabled = entry("disabled", definition("disabled", leaf(ConditionNodeType.ALWAYS_TRUE)), false);
        WebAdminConditionGroupStore.ConditionGroupEntry invalid = entry("invalid", ConditionGroupDefinition.of("invalid", ConditionNode.group("root", ConditionGroupMode.AND, List.of())), true);
        WebAdminConditionGroupStore.ConditionGroupEntry player = entry("player", definition("player", leaf(ConditionNodeType.PLAYER_EXISTS)), true);
        Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups = Map.of(
                "disabled", disabled,
                "invalid", invalid,
                "player", player
        );
        AtomicBoolean contextCalled = new AtomicBoolean(false);
        ConditionGateService service = service(groups);

        ConditionGateResult missing = service.evaluate(null, request("missing", ConditionRuntimeTargetType.VBD_INTERACTION, contextCalled));
        requireBlocked(missing, "condition_group_missing", "不存在", "missing group fails closed");
        requireFalse(contextCalled.get(), "missing group does not build context");

        ConditionGateResult disabledResult = service.evaluate(null, request("disabled", ConditionRuntimeTargetType.VBD_INTERACTION, contextCalled));
        requireBlocked(disabledResult, "condition_group_disabled", "停用", "disabled group fails closed");

        ConditionGateResult invalidResult = service.evaluate(null, request("invalid", ConditionRuntimeTargetType.VBD_INTERACTION, contextCalled));
        requireBlocked(invalidResult, "condition_group_validation_failed", "校验失败", "invalid group fails closed");

        ConditionGateResult incompatible = service.evaluate(null, request("player", ConditionRuntimeTargetType.VBD_REDSTONE, contextCalled));
        requireBlocked(incompatible, "condition_group_incompatible", "不兼容", "incompatible group fails closed");
        requireFalse(contextCalled.get(), "incompatible group does not build context");
    }

    private static void testCorruptRuntimeGateStoreFailsClosed() throws Exception {
        Path storePath = Files.createTempDirectory("tzz-runtime-gate-corrupt").resolve(ConditionRuntimeGateStore.FILE_NAME);
        Files.writeString(storePath, "{not-json");
        ConditionRuntimeGateStore.ConditionRuntimeGateLoadResult loaded = ConditionRuntimeGateStore.loadWithStatus(storePath);
        requireTrue(loaded.degraded(), "corrupt condition_runtime_gates.json reports degraded status");
        requireContains(loaded.message(), "读取失败", "corrupt gate store reports Chinese degraded message");

        AtomicBoolean contextCalled = new AtomicBoolean(false);
        ConditionGateResult result = service(Map.of()).evaluate(null, new ConditionGateRequest(
                ConditionRuntimeGateStore.STORE_UNAVAILABLE_GROUP_ID,
                ConditionRuntimeTargetType.VBD_REDSTONE,
                "vbd-1",
                () -> {
                    contextCalled.set(true);
                    return ConditionEvaluationContext.builder().build();
                }
        ));
        requireBlocked(result, "condition_runtime_gate_store_unavailable", "读取失败", "corrupt runtime gate store fails closed");
        requireFalse(contextCalled.get(), "corrupt runtime gate store does not build context");
    }

    private static void testConditionTrueFalseAndExceptionBehavior() {
        Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups = Map.of(
                "allow", entry("allow", definition("allow", leaf(ConditionNodeType.ALWAYS_TRUE)), true),
                "deny", entry("deny", definition("deny", leaf(ConditionNodeType.ALWAYS_FALSE)), true)
        );
        ConditionGateService service = service(groups);

        AtomicBoolean contextCalled = new AtomicBoolean(false);
        ConditionGateResult allowed = service.evaluate(null, request("allow", ConditionRuntimeTargetType.CONTAINER_OPEN, contextCalled));
        requireTrue(allowed.allowed(), "true condition gate passes");
        requireFalse(allowed.skipped(), "configured gate is not skipped");
        requireTrue(contextCalled.get(), "configured gate builds context");
        requireEquals("condition_gate_allowed", allowed.code(), "allowed code");

        ConditionGateResult denied = service.evaluate(null, request("deny", ConditionRuntimeTargetType.ITEM_SUBMIT, new AtomicBoolean(false)));
        requireBlocked(denied, "condition_group_not_matched", "未通过", "false condition gate blocks");
        requireTrue(containsChinese(denied.failureReason()), "false condition has Chinese failure reason");

        ConditionGateResult exception = service.evaluate(null, new ConditionGateRequest(
                "allow",
                ConditionRuntimeTargetType.CONTAINER_OPEN,
                "vbd-1",
                () -> {
                    throw new IllegalStateException("boom");
                }
        ));
        requireBlocked(exception, "condition_gate_exception", "异常", "context/evaluation exception fails closed");
    }

    private static void testRuntimeProfileMatchesInventoryContainerOpenCloseCapability() {
        Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups = Map.of(
                "container", entry("container", definition("container", ConditionNode.leaf(
                        "container",
                        ConditionNodeType.CONTAINER_SLOT_ITEM_MATCHES,
                        new ConditionNodeConfig(Map.of(
                                "containerKey", "container",
                                "slot", "0",
                                "itemId", "minecraft:stone",
                                "countOperator", "gte",
                                "count", "1"
                        ))
                )), true)
        );
        ConditionGateService service = service(groups);
        ConditionGroupCompatibilityService compatibility = new ConditionGroupCompatibilityService();

        ConditionGateResult nonInventoryOpen = service.evaluate(null, new ConditionGateRequest(
                "container",
                ConditionRuntimeTargetType.CONTAINER_OPEN,
                "vbd-1",
                () -> ConditionEvaluationContext.builder().build(),
                compatibility.profile(ConditionRuntimeTargetType.CONTAINER_OPEN, false)
        ));
        requireBlocked(nonInventoryOpen, "condition_group_incompatible", "容器快照", "non-Inventory open runtime rejects container condition");

        AtomicBoolean contextCalled = new AtomicBoolean(false);
        ConditionGateResult inventoryOpen = service.evaluate(null, new ConditionGateRequest(
                "container",
                ConditionRuntimeTargetType.CONTAINER_OPEN,
                "vbd-1",
                () -> {
                    contextCalled.set(true);
                    return ConditionEvaluationContext.builder()
                            .containerSnapshot("container", new com.zcpu.tzzmod.condition.item.ConditionContainerSnapshot(List.of(
                                    new com.zcpu.tzzmod.condition.item.ConditionItemStackSnapshot("minecraft:stone", 1, "", List.of(), Map.of(), Map.of())
                            )))
                            .build();
                },
                compatibility.profile(ConditionRuntimeTargetType.CONTAINER_OPEN, true)
        ));
        requireTrue(inventoryOpen.allowed(), "Inventory open runtime accepts and evaluates container condition");
        requireTrue(contextCalled.get(), "Inventory open runtime builds container snapshot context");

        ConditionGateResult inventoryClose = service.evaluate(null, new ConditionGateRequest(
                "container",
                ConditionRuntimeTargetType.CONTAINER_CLOSE,
                "vbd-1",
                () -> ConditionEvaluationContext.builder()
                        .containerSnapshot("container", new com.zcpu.tzzmod.condition.item.ConditionContainerSnapshot(List.of(
                                new com.zcpu.tzzmod.condition.item.ConditionItemStackSnapshot("minecraft:stone", 1, "", List.of(), Map.of(), Map.of())
                        )))
                        .build(),
                compatibility.profile(ConditionRuntimeTargetType.CONTAINER_CLOSE, true)
        ));
        requireTrue(inventoryClose.allowed(), "Inventory close runtime accepts and evaluates container condition");
    }

    private static void testSummaryShape() {
        Map<String, Object> summary = service(Map.of()).summary(ConditionGateResult.skippedResult());
        requireEquals(Boolean.TRUE, summary.get("allowed"), "summary allowed");
        requireEquals(Boolean.TRUE, summary.get("skipped"), "summary skipped");
        requireTrue(summary.containsKey("conditionGroupId"), "summary conditionGroupId");
        requireTrue(summary.containsKey("failureReason"), "summary failureReason");
        requireTrue(summary.containsKey("evaluatedCount"), "summary evaluatedCount");
        requireTrue(summary.containsKey("durationNanos"), "summary duration");
        requireTrue(summary.containsKey("code"), "summary code");
    }

    private static ConditionGateRequest request(String groupId, ConditionRuntimeTargetType targetType, AtomicBoolean contextCalled) {
        return new ConditionGateRequest(
                groupId,
                targetType,
                "vbd-1",
                () -> {
                    contextCalled.set(true);
                    return ConditionEvaluationContext.builder()
                            .source("virtual_block_device", "vbd-1")
                            .deviceId("vbd-1")
                            .worldId("minecraft:overworld")
                            .channel("mission.start")
                            .build();
                }
        );
    }

    private static ConditionGateService service(Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups) {
        return new ConditionGateService(
                groups::get,
                new ConditionEvaluator(),
                new ConditionGroupCompatibilityService()
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
        return ConditionNode.leaf("node-" + type, type, ConditionNodeConfig.EMPTY);
    }

    private static void requireBlocked(ConditionGateResult result, String expectedCode, String expectedReason, String message) {
        requireFalse(result.allowed(), message);
        requireFalse(result.skipped(), message + " not skipped");
        requireEquals(expectedCode, result.code(), message + " code");
        requireContains(result.failureReason(), expectedReason, message + " reason");
        requireTrue(containsChinese(result.failureReason()), message + " Chinese failure reason");
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
