package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import java.time.Instant;
import java.util.Map;

public record ConditionGateHistoryRecord(
        String id,
        long sequence,
        long wallTimeMillis,
        String occurredAt,
        long gameTime,
        String worldId,
        ConditionRuntimeTargetType targetType,
        String targetTypeId,
        String targetTypeDisplayName,
        String targetId,
        String gateLevel,
        String parentTargetType,
        String parentTargetId,
        String parentTargetDisplayName,
        int actionIndex,
        int actionDisplayIndex,
        String actionType,
        String parentActionBucket,
        String sourceType,
        String sourceId,
        String channel,
        String deviceId,
        String listenerId,
        String regionId,
        String actionId,
        String playerId,
        String playerName,
        String conditionGroupId,
        String conditionGroupDisplayName,
        String conditionGroupFingerprint,
        String definitionFingerprint,
        String result,
        boolean allowed,
        boolean skipped,
        String code,
        String failureReason,
        String debugSummary,
        int evaluatedCount,
        long durationNanos,
        Map<String, String> contextSummary,
        ConditionGateDebugNode debugTree,
        // Replay snapshots must remain value-only: no live world/player/inventory/service references.
        ConditionEvaluationContext replayContext,
        ConditionGroupDefinition definitionSnapshot
) {
    public ConditionGateHistoryRecord {
        id = safe(id);
        occurredAt = safe(occurredAt);
        worldId = safe(worldId);
        targetType = targetType == null ? ConditionRuntimeTargetType.VBD_INTERACTION : targetType;
        targetTypeId = safe(targetTypeId);
        targetTypeDisplayName = safe(targetTypeDisplayName);
        targetId = safe(targetId);
        gateLevel = safe(gateLevel).isBlank() ? "LIST" : safe(gateLevel).toUpperCase(java.util.Locale.ROOT);
        parentTargetType = safe(parentTargetType);
        parentTargetId = safe(parentTargetId);
        parentTargetDisplayName = safe(parentTargetDisplayName);
        actionIndex = Math.max(-1, actionIndex);
        actionDisplayIndex = actionDisplayIndex <= 0 && actionIndex >= 0 ? actionIndex + 1 : Math.max(0, actionDisplayIndex);
        actionType = safe(actionType);
        parentActionBucket = safe(parentActionBucket);
        sourceType = safe(sourceType);
        sourceId = safe(sourceId);
        channel = safe(channel);
        deviceId = safe(deviceId);
        listenerId = safe(listenerId);
        regionId = safe(regionId);
        actionId = safe(actionId);
        playerId = safe(playerId);
        playerName = safe(playerName);
        conditionGroupId = safe(conditionGroupId);
        conditionGroupDisplayName = safe(conditionGroupDisplayName);
        conditionGroupFingerprint = safe(conditionGroupFingerprint);
        definitionFingerprint = safe(definitionFingerprint);
        result = safe(result).isBlank() ? "UNKNOWN" : safe(result);
        code = safe(code);
        failureReason = safe(failureReason);
        debugSummary = safe(debugSummary);
        evaluatedCount = Math.max(0, evaluatedCount);
        durationNanos = Math.max(0L, durationNanos);
        contextSummary = contextSummary == null ? Map.of() : Map.copyOf(contextSummary);
    }

    public static String occurredAt(long wallTimeMillis) {
        return Instant.ofEpochMilli(Math.max(0L, wallTimeMillis)).toString();
    }

    public Map<String, Object> compactDto() {
        java.util.LinkedHashMap<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("id", id);
        data.put("sequence", sequence);
        data.put("wallTimeMillis", wallTimeMillis);
        data.put("occurredAt", occurredAt);
        data.put("gameTime", gameTime);
        data.put("worldId", worldId);
        data.put("targetType", targetTypeId);
        data.put("targetTypeDisplayName", targetTypeDisplayName);
        data.put("targetId", targetId);
        data.put("gateLevel", gateLevel);
        data.put("parentTargetType", parentTargetType);
        data.put("parentTargetId", parentTargetId);
        data.put("parentTargetDisplayName", parentTargetDisplayName);
        data.put("actionIndex", actionIndex);
        data.put("actionDisplayIndex", actionDisplayIndex);
        data.put("actionType", actionType);
        data.put("parentActionBucket", parentActionBucket);
        data.put("sourceType", sourceType);
        data.put("sourceId", sourceId);
        data.put("channel", channel);
        data.put("deviceId", deviceId);
        data.put("listenerId", listenerId);
        data.put("regionId", regionId);
        data.put("actionId", actionId);
        data.put("playerId", playerId);
        data.put("playerName", playerName);
        data.put("conditionGroupId", conditionGroupId);
        data.put("conditionGroupDisplayName", conditionGroupDisplayName);
        data.put("conditionGroupFingerprint", conditionGroupFingerprint);
        data.put("definitionFingerprint", definitionFingerprint);
        data.put("result", result);
        data.put("allowed", allowed);
        data.put("skipped", skipped);
        data.put("code", code);
        data.put("failureReason", failureReason);
        data.put("debugSummary", debugSummary);
        data.put("evaluatedCount", evaluatedCount);
        data.put("durationNanos", durationNanos);
        data.put("contextSummary", contextSummary);
        data.put("replayable", replayContext != null && definitionSnapshot != null);
        data.put("debuggerRoute", "#/condition-debugger/" + java.net.URLEncoder.encode(id, java.nio.charset.StandardCharsets.UTF_8));
        return Map.copyOf(data);
    }

    public Map<String, Object> detailDto() {
        java.util.LinkedHashMap<String, Object> data = new java.util.LinkedHashMap<>(compactDto());
        data.put("debugTree", debugTree);
        data.put("replayReadOnly", true);
        data.put("noActionExecution", true);
        data.put("noSignalEmit", true);
        data.put("noRawJsonEditor", true);
        return data;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
