package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.condition.ConditionEvaluator;
import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionGroupMode;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeConfig;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.service.WebAdminConditionGateHistoryService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConditionGateHistoryServiceTest {
    private ConditionGateHistoryServiceTest() {
    }

    public static void run() {
        testConfiguredGateResultsRecordHistory();
        testBlankGateSkipsHistoryAndRuntimeReads();
        testHistoryRingBufferIsBounded();
        testWebAdminHistoryServiceListDetailAndRecentStatus();
    }

    private static void testConfiguredGateResultsRecordHistory() {
        ConditionGateHistory.clearForTest();
        Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups = Map.of(
                "allow", entry("allow", definition("allow", leaf(ConditionNodeType.ALWAYS_TRUE)), true),
                "deny", entry("deny", definition("deny", leaf(ConditionNodeType.ALWAYS_FALSE)), true)
        );
        ConditionGateService service = service(groups);

        ConditionGateResult allowed = service.evaluate(null, request("allow", ConditionRuntimeTargetType.VBD_INTERACTION, "vbd-allow"));
        requireTrue(allowed.allowed(), "gate allowed result is preserved while history records");
        ConditionGateHistoryRecord allowedRecord = latest(ConditionRuntimeTargetType.VBD_INTERACTION, "vbd-allow");
        requireEquals("ALLOWED", allowedRecord.result(), "allowed gate history result");
        requireEquals("allow", allowedRecord.conditionGroupId(), "allowed gate history group id");
        requireEquals("vbd-allow", allowedRecord.targetId(), "allowed gate history target id");
        requireEquals("mission.start", allowedRecord.channel(), "allowed gate history channel");
        requireTrue(allowedRecord.evaluatedCount() > 0, "allowed gate history evaluatedCount");
        requireTrue(allowedRecord.durationNanos() >= 0L, "allowed gate history duration");
        requireTrue(allowedRecord.contextSummary().containsKey("channel"), "allowed gate history context summary");
        requireNotNull(allowedRecord.debugTree(), "allowed gate history debug tree");
        requireNotNull(allowedRecord.replayContext(), "allowed gate history replay context snapshot");
        requireNotNull(allowedRecord.definitionSnapshot(), "allowed gate history definition snapshot");
        requireTrue(allowedRecord.compactDto().containsKey("replayable"), "allowed compact dto exposes replayable");
        requireEquals(Boolean.TRUE, allowedRecord.detailDto().get("replayReadOnly"), "detail dto marks replay read-only");
        requireEquals(Boolean.TRUE, allowedRecord.detailDto().get("noActionExecution"), "detail dto marks no action execution");
        requireEquals(Boolean.TRUE, allowedRecord.detailDto().get("noSignalEmit"), "detail dto marks no signal emit");

        ConditionGateResult blocked = service.evaluate(null, request("deny", ConditionRuntimeTargetType.ITEM_SUBMIT, "vbd-deny"));
        requireFalse(blocked.allowed(), "false gate remains blocked while history records");
        ConditionGateHistoryRecord blockedRecord = latest(ConditionRuntimeTargetType.ITEM_SUBMIT, "vbd-deny");
        requireEquals("BLOCKED", blockedRecord.result(), "blocked gate history result");
        requireContains(blockedRecord.failureReason(), "未通过", "blocked gate history Chinese failure reason");
        requireNotNull(blockedRecord.debugTree(), "blocked gate history debug tree");

        ConditionGateResult missing = service.evaluate(null, request("missing", ConditionRuntimeTargetType.ACTION_RELAY, "relay-1"));
        requireFalse(missing.allowed(), "missing group remains fail-closed");
        ConditionGateHistoryRecord missingRecord = latest(ConditionRuntimeTargetType.ACTION_RELAY, "relay-1");
        requireEquals("BLOCKED", missingRecord.result(), "missing group history records blocked");
        requireContains(missingRecord.failureReason(), "不存在", "missing group history Chinese reason");
        requireTrue(missingRecord.replayContext() == null, "missing group history avoids fake replay context");
        requireTrue(missingRecord.definitionSnapshot() == null, "missing group history avoids fake definition snapshot");

        ConditionGateResult exception = service.evaluate(null, new ConditionGateRequest(
                "allow",
                ConditionRuntimeTargetType.REGION_ENTER,
                "region-controller-1",
                () -> {
                    throw new IllegalStateException("boom");
                }
        ));
        requireFalse(exception.allowed(), "exception gate remains fail-closed");
        ConditionGateHistoryRecord exceptionRecord = latest(ConditionRuntimeTargetType.REGION_ENTER, "region-controller-1");
        requireEquals("ERROR", exceptionRecord.result(), "exception history result is error");
        requireContains(exceptionRecord.failureReason(), "异常", "exception history Chinese reason");
    }

    private static void testBlankGateSkipsHistoryAndRuntimeReads() {
        ConditionGateHistory.clearForTest();
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
                ConditionRuntimeTargetType.SIGNAL_LISTENER,
                "listener-1",
                () -> {
                    contextCalled.set(true);
                    return context("listener-1");
                }
        ));

        requireTrue(result.allowed(), "blank gate still preserves old allowed path");
        requireTrue(result.skipped(), "blank gate is skipped");
        requireFalse(loaderCalled.get(), "blank gate history test does not load condition group store");
        requireFalse(contextCalled.get(), "blank gate history test does not build EvaluationContext");
        requireTrue(ConditionGateHistory.snapshot().isEmpty(), "blank conditionGroupId records no runtime history");
    }

    private static void testHistoryRingBufferIsBounded() {
        ConditionGateHistory.clearForTest();
        ConditionGateService service = service(Map.of(
                "allow", entry("allow", definition("allow", leaf(ConditionNodeType.ALWAYS_TRUE)), true)
        ));
        for (int index = 0; index < ConditionGateHistory.MAX_RECORDS + 5; index++) {
            ConditionGateResult result = service.evaluate(null, request("allow", ConditionRuntimeTargetType.VBD_BLOCKSTATE, "vbd-" + index));
            requireTrue(result.allowed(), "ring buffer seed gate allowed " + index);
        }
        List<ConditionGateHistoryRecord> snapshot = ConditionGateHistory.snapshot();
        requireEquals(ConditionGateHistory.MAX_RECORDS, snapshot.size(), "history ring buffer max records");
        requireTrue(ConditionGateHistory.find("gate-1").isEmpty(), "oldest history record is evicted");
        requireTrue(ConditionGateHistory.find("gate-" + (ConditionGateHistory.MAX_RECORDS + 5)).isPresent(), "latest history record remains");
    }

    private static void testWebAdminHistoryServiceListDetailAndRecentStatus() {
        ConditionGateHistory.clearForTest();
        ConditionGateService service = service(Map.of(
                "allow", entry("allow", definition("allow", leaf(ConditionNodeType.ALWAYS_TRUE)), true),
                "deny", entry("deny", definition("deny", leaf(ConditionNodeType.ALWAYS_FALSE)), true)
        ));
        service.evaluate(null, request("allow", ConditionRuntimeTargetType.VBD_REDSTONE, "vbd-redstone"));
        service.evaluate(null, request("deny", ConditionRuntimeTargetType.SIGNAL_LISTENER, "listener-1"));

        WebAdminConditionGateHistoryService historyService = new WebAdminConditionGateHistoryService();
        Map<String, Object> list = historyService.list(Map.of("result", "BLOCKED", "targetType", "SIGNAL_LISTENER", "limit", "50"));
        requireEquals(Boolean.TRUE, list.get("readOnly"), "history list API is read-only");
        requireEquals(Boolean.TRUE, list.get("inMemory"), "history list API exposes in-memory storage");
        requireEquals(ConditionGateHistory.MAX_RECORDS, list.get("maxRecords"), "history list API exposes maxRecords");
        List<?> records = (List<?>) list.get("records");
        requireEquals(1, records.size(), "history list API filters result and targetType");
        Map<?, ?> row = (Map<?, ?>) records.getFirst();
        requireEquals("SIGNAL_LISTENER", row.get("targetType"), "history list API row target type");
        requireEquals("BLOCKED", row.get("result"), "history list API row result");
        requireTrue(Boolean.TRUE.equals(row.get("replayable")), "history list API row replayable");

        String recordId = String.valueOf(row.get("id"));
        Map<String, Object> detail = historyService.detail(recordId);
        requireNotNull(detail, "history detail API returns record");
        requireTrue(detail.containsKey("debugTree"), "history detail API exposes debug tree");
        requireEquals(Boolean.TRUE, detail.get("noRawJsonEditor"), "history detail API is not a raw JSON editor");

        Map<String, Object> recent = WebAdminConditionGateHistoryService.recentStatus(ConditionRuntimeTargetType.SIGNAL_LISTENER, "listener-1");
        requireEquals(Boolean.TRUE, recent.get("configuredHistory"), "recent status is configured after history");
        requireEquals("BLOCKED", recent.get("status"), "recent status result");
        requireTrue(String.valueOf(recent.get("debuggerRoute")).contains("#/condition-debugger/"), "recent status links debugger");

        Map<String, Object> empty = WebAdminConditionGateHistoryService.recentStatus(ConditionRuntimeTargetType.REGION_STAY, "missing");
        requireEquals(Boolean.FALSE, empty.get("configuredHistory"), "recent status empty marker");
        requireContains(String.valueOf(empty.get("message")), "暂无", "recent status empty Chinese message");
    }

    private static ConditionGateHistoryRecord latest(ConditionRuntimeTargetType targetType, String targetId) {
        return ConditionGateHistory.latestFor(targetType, targetId)
                .orElseThrow(() -> new AssertionError("missing latest history for " + targetType + " " + targetId));
    }

    private static ConditionGateRequest request(String groupId, ConditionRuntimeTargetType targetType, String targetId) {
        return new ConditionGateRequest(groupId, targetType, targetId, () -> context(targetId));
    }

    private static ConditionEvaluationContext context(String targetId) {
        return ConditionEvaluationContext.builder()
                .source("runtime-test", targetId)
                .deviceId(targetId)
                .listenerId(targetId)
                .regionId(targetId)
                .worldId("minecraft:overworld")
                .channel("mission.start")
                .gameTime(123L)
                .build();
    }

    private static ConditionGateService service(Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups) {
        return new ConditionGateService(groups::get, new ConditionEvaluator(), new ConditionGroupCompatibilityService());
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

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireFalse(boolean condition, String message) {
        requireTrue(!condition, message);
    }

    private static void requireNotNull(Object value, String message) {
        requireTrue(value != null, message);
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
