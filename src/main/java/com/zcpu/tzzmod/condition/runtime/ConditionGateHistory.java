package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import com.zcpu.tzzmod.condition.ConditionEvaluationTrace;
import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class ConditionGateHistory {
    public static final int MAX_RECORDS = 200;
    private static final AtomicLong NEXT_SEQUENCE = new AtomicLong(1L);
    private static final Deque<ConditionGateHistoryRecord> RECORDS = new ArrayDeque<>();

    private ConditionGateHistory() {
    }

    public static ConditionGateHistoryRecord record(
            ConditionGateRequest request,
            WebAdminConditionGroupStore.ConditionGroupEntry entry,
            ConditionEvaluationContext context,
            ConditionEvaluationTrace trace,
            ConditionGateResult result
    ) {
        if (result == null || result.skipped() || result.conditionGroupId().isBlank()) {
            return null;
        }
        try {
            ConditionRuntimeTargetType targetType = request == null || request.targetType() == null
                    ? ConditionRuntimeTargetType.VBD_INTERACTION
                    : request.targetType();
            ConditionGroupDefinition definition = entry == null ? null : entry.groupDefinition;
            long sequence = NEXT_SEQUENCE.getAndIncrement();
            long wallTime = System.currentTimeMillis();
            ConditionGateHistoryRecord record = new ConditionGateHistoryRecord(
                    "gate-" + sequence,
                    sequence,
                    wallTime,
                    ConditionGateHistoryRecord.occurredAt(wallTime),
                    context == null ? 0L : context.gameTime(),
                    context == null ? "" : context.worldId(),
                    targetType,
                    targetType.id(),
                    targetType.displayName(),
                    request == null ? "" : request.targetId(),
                    context == null ? "" : context.sourceType(),
                    context == null ? "" : context.sourceId(),
                    context == null ? "" : context.channel(),
                    context == null ? "" : context.deviceId(),
                    context == null ? "" : context.listenerId(),
                    context == null ? "" : context.regionId(),
                    context == null ? "" : context.actionId(),
                    context == null ? "" : context.playerId(),
                    context == null ? "" : context.playerName(),
                    result.conditionGroupId(),
                    entry == null ? "" : entry.displayName,
                    entry == null ? "" : WebAdminConditionGroupStore.fingerprintFor(entry),
                    definition == null ? "" : definition.stableFingerprint(),
                    resultStatus(result, trace),
                    result.allowed(),
                    result.skipped(),
                    result.code(),
                    result.failureReason(),
                    result.debugSummary(),
                    result.evaluatedCount(),
                    result.durationNanos(),
                    context == null ? java.util.Map.of() : context.summary(),
                    trace == null ? null : ConditionGateDebugNode.from(trace.rootResult()),
                    context,
                    definition
            );
            remember(record);
            publish(record);
            return record;
        } catch (RuntimeException exception) {
            Tzz_mod.LOGGER.warn("Condition gate history recording failed: {}", exception.getMessage());
            return null;
        }
    }

    public static List<ConditionGateHistoryRecord> snapshot() {
        synchronized (RECORDS) {
            return List.copyOf(RECORDS);
        }
    }

    public static List<ConditionGateHistoryRecord> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(MAX_RECORDS, limit));
        List<ConditionGateHistoryRecord> records = snapshot().stream()
                .sorted(Comparator.comparingLong(ConditionGateHistoryRecord::sequence).reversed())
                .limit(safeLimit)
                .toList();
        return List.copyOf(records);
    }

    public static Optional<ConditionGateHistoryRecord> find(String id) {
        String safeId = safe(id);
        if (safeId.isBlank()) {
            return Optional.empty();
        }
        synchronized (RECORDS) {
            return RECORDS.stream()
                    .filter(record -> record.id().equals(safeId))
                    .findFirst();
        }
    }

    public static Optional<ConditionGateHistoryRecord> latestFor(ConditionRuntimeTargetType targetType, String targetId) {
        String safeTargetId = safe(targetId);
        if (targetType == null || safeTargetId.isBlank()) {
            return Optional.empty();
        }
        synchronized (RECORDS) {
            return RECORDS.stream()
                    .filter(record -> record.targetType() == targetType && safeTargetId.equals(record.targetId()))
                    .max(Comparator.comparingLong(ConditionGateHistoryRecord::sequence));
        }
    }

    public static Optional<ConditionGateHistoryRecord> latestForConditionGroup(String conditionGroupId) {
        String safeGroupId = WebAdminConditionGroupStore.normalizeId(conditionGroupId);
        if (safeGroupId.isBlank()) {
            return Optional.empty();
        }
        synchronized (RECORDS) {
            return RECORDS.stream()
                    .filter(record -> safeGroupId.equals(record.conditionGroupId()))
                    .max(Comparator.comparingLong(ConditionGateHistoryRecord::sequence));
        }
    }

    public static void clearForTest() {
        synchronized (RECORDS) {
            RECORDS.clear();
        }
        NEXT_SEQUENCE.set(1L);
    }

    private static void remember(ConditionGateHistoryRecord record) {
        synchronized (RECORDS) {
            while (RECORDS.size() >= MAX_RECORDS) {
                RECORDS.removeFirst();
            }
            RECORDS.addLast(record);
        }
    }

    private static void publish(ConditionGateHistoryRecord record) {
        try {
            WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONDITION_GATE_HISTORY_APPENDED)
                    .channel(record.channel())
                    .deviceId(recordDeviceId(record))
                    .regionId(record.regionId())
                    .actionId(record.actionId())
                    .sourceType(record.sourceType().isBlank() ? "condition_gate" : record.sourceType())
                    .severity("ALLOWED".equals(record.result()) ? "INFO" : ("ERROR".equals(record.result()) ? "ERROR" : "WARNING"))
                    .summary("条件 gate 记录已追加：" + record.targetTypeDisplayName())
                    .routeTarget("#/condition-debugger")
                    .payload("conditionGateHistoryId", record.id())
                    .payload("conditionGroupId", record.conditionGroupId())
                    .payload("targetType", record.targetTypeId())
                    .payload("targetId", record.targetId())
                    .payload("deviceId", recordDeviceId(record))
                    .payload("listenerId", record.listenerId().isBlank() ? listenerTargetId(record) : record.listenerId())
                    .payload("regionId", record.regionId())
                    .payload("actionId", record.actionId())
                    .payload("result", record.result()));
        } catch (RuntimeException exception) {
            Tzz_mod.LOGGER.warn("Condition gate realtime notification failed: {}", exception.getMessage());
        }
    }

    private static String recordDeviceId(ConditionGateHistoryRecord record) {
        if (record == null) {
            return "";
        }
        if (!record.deviceId().isBlank()) {
            return record.deviceId();
        }
        ConditionRuntimeTargetType targetType = record.targetType();
        if (targetType == ConditionRuntimeTargetType.ACTION_RELAY
                || targetType == ConditionRuntimeTargetType.VBD_REDSTONE
                || targetType == ConditionRuntimeTargetType.VBD_BLOCKSTATE
                || targetType == ConditionRuntimeTargetType.VBD_INTERACTION
                || targetType == ConditionRuntimeTargetType.ITEM_SUBMIT
                || targetType == ConditionRuntimeTargetType.CONTAINER_OPEN
                || targetType == ConditionRuntimeTargetType.CONTAINER_CLOSE
                || targetType == ConditionRuntimeTargetType.CONTAINER_CHANGE) {
            return record.targetId();
        }
        return "";
    }

    private static String listenerTargetId(ConditionGateHistoryRecord record) {
        return record != null && record.targetType() == ConditionRuntimeTargetType.SIGNAL_LISTENER ? record.targetId() : "";
    }

    private static String resultStatus(ConditionGateResult result, ConditionEvaluationTrace trace) {
        if (result == null) {
            return "ERROR";
        }
        if (result.skipped()) {
            return "SKIPPED";
        }
        if (result.allowed()) {
            return "ALLOWED";
        }
        String code = result.code().toLowerCase(Locale.ROOT);
        if (code.contains("exception") || code.contains("error")
                || (trace != null && trace.rootResult() != null && trace.rootResult().error())) {
            return "ERROR";
        }
        return "BLOCKED";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
