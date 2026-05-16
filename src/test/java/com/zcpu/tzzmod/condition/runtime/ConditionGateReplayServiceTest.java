package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.condition.ConditionEvaluator;
import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.condition.ConditionGroupMode;
import com.zcpu.tzzmod.condition.ConditionNode;
import com.zcpu.tzzmod.condition.ConditionNodeConfig;
import com.zcpu.tzzmod.condition.ConditionNodeType;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import java.util.List;
import java.util.Map;

public final class ConditionGateReplayServiceTest {
    private ConditionGateReplayServiceTest() {
    }

    public static void run() {
        testReplayAllowedAndBlockedRecords();
        testReplayChangedAndDeletedConditionGroup();
        testReplayMissingRecordAndMissingSnapshotFailSafely();
    }

    private static void testReplayAllowedAndBlockedRecords() {
        ConditionGateHistory.clearForTest();
        Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups = Map.of(
                "allow", entry("allow", definition("allow", leaf(ConditionNodeType.ALWAYS_TRUE)), true),
                "deny", entry("deny", definition("deny", leaf(ConditionNodeType.ALWAYS_FALSE)), true)
        );
        ConditionGateService gateService = gateService(groups);
        gateService.evaluate(null, request("allow", ConditionRuntimeTargetType.VBD_INTERACTION, "vbd-allow"));
        gateService.evaluate(null, request("deny", ConditionRuntimeTargetType.ITEM_SUBMIT, "vbd-deny"));

        ConditionGateReplayService replayService = new ConditionGateReplayService(groups::get, new ConditionEvaluator());
        ConditionGateReplayResult allowed = replayService.replay(null, latest(ConditionRuntimeTargetType.VBD_INTERACTION, "vbd-allow").id());
        requireTrue(allowed.success(), "replay allowed record succeeds");
        requireTrue(allowed.readOnly(), "replay allowed record is read-only");
        requireTrue(allowed.noSideEffects(), "replay allowed record has no side effects");
        requireTrue(allowed.noLiveWorldRead(), "replay allowed record does not read live world");
        requireEquals("ALLOWED", allowed.originalResult(), "replay allowed original result");
        requireEquals("ALLOWED", allowed.replayResult(), "replay allowed result");
        requireTrue(allowed.resultConsistent(), "replay allowed result consistent");
        requireTrue(allowed.evaluatedCount() > 0, "replay allowed evaluated count");
        requireNotNull(allowed.debugTree(), "replay allowed debug tree");
        requireContains(String.join(" ", allowed.warnings()), "不写 store", "replay allowed warning no store write");
        requireContains(String.join(" ", allowed.warnings()), "不读取 live world", "replay allowed warning no live read");

        ConditionGateReplayResult blocked = replayService.replay(null, latest(ConditionRuntimeTargetType.ITEM_SUBMIT, "vbd-deny").id());
        requireTrue(blocked.success(), "replay blocked record succeeds");
        requireEquals("BLOCKED", blocked.originalResult(), "replay blocked original result");
        requireEquals("BLOCKED", blocked.replayResult(), "replay blocked result");
        requireFalse(blocked.replayMatched(), "replay blocked matched false");
        requireNotNull(blocked.debugTree(), "replay blocked debug tree");
        requireContains(blocked.failureReason(), "永远失败", "replay blocked Chinese failure reason");
    }

    private static void testReplayChangedAndDeletedConditionGroup() {
        ConditionGateHistory.clearForTest();
        WebAdminConditionGroupStore.ConditionGroupEntry original = entry("allow", definition("allow", leaf(ConditionNodeType.ALWAYS_TRUE)), true);
        ConditionGateService gateService = gateService(Map.of("allow", original));
        gateService.evaluate(null, request("allow", ConditionRuntimeTargetType.VBD_INTERACTION, "vbd-changed"));
        String recordId = latest(ConditionRuntimeTargetType.VBD_INTERACTION, "vbd-changed").id();

        WebAdminConditionGroupStore.ConditionGroupEntry changed = entry("allow", definition("allow", leaf(ConditionNodeType.ALWAYS_FALSE)), true);
        ConditionGateReplayService changedReplay = new ConditionGateReplayService((id) -> changed, new ConditionEvaluator());
        ConditionGateReplayResult changedResult = changedReplay.replay(null, recordId);
        requireTrue(changedResult.success(), "replay changed condition group succeeds from historical snapshot");
        requireTrue(changedResult.fingerprintChanged(), "replay changed condition group marks fingerprint mismatch");
        requireEquals("ALLOWED", changedResult.replayResult(), "replay changed group uses recorded definition snapshot");
        requireContains(String.join(" ", changedResult.warnings()), "历史快照", "replay changed group Chinese warning");

        ConditionGateReplayService deletedReplay = new ConditionGateReplayService((id) -> null, new ConditionEvaluator());
        ConditionGateReplayResult deleted = deletedReplay.replay(null, recordId);
        requireFalse(deleted.success(), "replay deleted condition group fails safely");
        requireEquals("condition_gate_replay_group_deleted", deleted.code(), "replay deleted condition group code");
        requireTrue(deleted.readOnly(), "replay deleted remains read-only");
        requireTrue(deleted.noSideEffects(), "replay deleted has no side effects");
        requireContains(deleted.failureReason(), "已删除", "replay deleted Chinese message");
    }

    private static void testReplayMissingRecordAndMissingSnapshotFailSafely() {
        ConditionGateHistory.clearForTest();
        ConditionGateReplayService replayService = new ConditionGateReplayService(Map.<String, WebAdminConditionGroupStore.ConditionGroupEntry>of()::get, new ConditionEvaluator());
        ConditionGateReplayResult missingRecord = replayService.replay(null, "missing-record");
        requireFalse(missingRecord.success(), "replay missing record fails safely");
        requireEquals("condition_gate_history_missing", missingRecord.code(), "replay missing record code");
        requireTrue(missingRecord.readOnly(), "replay missing record still read-only");

        Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups = Map.of(
                "allow", entry("allow", definition("allow", leaf(ConditionNodeType.ALWAYS_TRUE)), true)
        );
        gateService(groups).evaluate(null, new ConditionGateRequest(
                "allow",
                ConditionRuntimeTargetType.ACTION_RELAY,
                "relay-exception",
                () -> {
                    throw new IllegalStateException("boom");
                }
        ));
        ConditionGateReplayResult missingSnapshot = new ConditionGateReplayService(groups::get, new ConditionEvaluator())
                .replay(null, latest(ConditionRuntimeTargetType.ACTION_RELAY, "relay-exception").id());
        requireFalse(missingSnapshot.success(), "replay record without context snapshot fails safely");
        requireEquals("condition_gate_replay_context_missing", missingSnapshot.code(), "replay missing context snapshot code");
        requireContains(missingSnapshot.failureReason(), "上下文快照", "replay missing context Chinese reason");
        requireContains(String.join(" ", missingSnapshot.warnings()), "SignalBridge", "replay missing context no live service read warning");

        ConditionGateHistory.record(
                request("allow", ConditionRuntimeTargetType.ACTION_RELAY, "relay-no-definition-snapshot"),
                entry("allow", null, true),
                ConditionEvaluationContext.builder()
                        .source("replay-test", "relay-no-definition-snapshot")
                        .worldId("minecraft:overworld")
                        .channel("mission.start")
                        .build(),
                null,
                ConditionGateResult.allowed("allow", "manual test record", 0, 0L)
        );
        ConditionGateReplayResult missingDefinition = new ConditionGateReplayService(groups::get, new ConditionEvaluator())
                .replay(null, latest(ConditionRuntimeTargetType.ACTION_RELAY, "relay-no-definition-snapshot").id());
        requireFalse(missingDefinition.success(), "replay record without definition snapshot fails safely");
        requireEquals("condition_gate_replay_definition_missing", missingDefinition.code(), "replay missing definition snapshot code");
        requireContains(missingDefinition.failureReason(), "定义快照", "replay missing definition Chinese reason");
        requireContains(String.join(" ", missingDefinition.warnings()), "SignalBridge", "replay missing definition no live service read warning");
    }

    private static ConditionGateHistoryRecord latest(ConditionRuntimeTargetType targetType, String targetId) {
        return ConditionGateHistory.latestFor(targetType, targetId)
                .orElseThrow(() -> new AssertionError("missing latest history for " + targetType + " " + targetId));
    }

    private static ConditionGateRequest request(String groupId, ConditionRuntimeTargetType targetType, String targetId) {
        return new ConditionGateRequest(groupId, targetType, targetId, () -> ConditionEvaluationContext.builder()
                .source("replay-test", targetId)
                .deviceId(targetId)
                .worldId("minecraft:overworld")
                .channel("mission.start")
                .gameTime(456L)
                .build());
    }

    private static ConditionGateService gateService(Map<String, WebAdminConditionGroupStore.ConditionGroupEntry> groups) {
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
