package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.condition.runtime.ConditionGateHistory;
import com.zcpu.tzzmod.condition.runtime.ConditionGateHistoryRecord;
import com.zcpu.tzzmod.condition.runtime.ConditionGateReplayResult;
import com.zcpu.tzzmod.condition.runtime.ConditionGateReplayService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

public final class WebAdminConditionGateHistoryService {
    private final ConditionGateReplayService replayService = new ConditionGateReplayService();

    public Map<String, Object> list(Map<String, String> query) {
        int limit = intValue(query == null ? "" : query.get("limit"), ConditionGateHistory.MAX_RECORDS);
        String targetType = upper(query == null ? "" : query.get("targetType"));
        String result = upper(query == null ? "" : query.get("result"));
        String conditionGroupId = WebAdminConditionGroupStore.normalizeId(query == null ? "" : query.get("conditionGroupId"));
        String targetId = safe(query == null ? "" : query.get("targetId"));
        String channel = safe(query == null ? "" : query.get("channel"));
        List<Map<String, Object>> records = ConditionGateHistory.recent(limit).stream()
                .filter(record -> targetType.isBlank() || record.targetTypeId().equals(targetType))
                .filter(record -> result.isBlank() || record.result().equals(result))
                .filter(record -> conditionGroupId.isBlank() || record.conditionGroupId().equals(conditionGroupId))
                .filter(record -> targetId.isBlank() || containsIgnoreCase(record.targetId(), targetId))
                .filter(record -> channel.isBlank() || containsIgnoreCase(record.channel(), channel))
                .map(ConditionGateHistoryRecord::compactDto)
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("readOnly", true);
        data.put("inMemory", true);
        data.put("worldScoped", false);
        data.put("maxRecords", ConditionGateHistory.MAX_RECORDS);
        data.put("count", records.size());
        data.put("records", records);
        data.put("filters", Map.of(
                "targetType", targetType,
                "result", result,
                "conditionGroupId", conditionGroupId,
                "targetId", targetId,
                "channel", channel
        ));
        return Map.copyOf(data);
    }

    public Map<String, Object> detail(String recordId) {
        Optional<ConditionGateHistoryRecord> record = ConditionGateHistory.find(recordId);
        if (record.isEmpty()) {
            return null;
        }
        return record.get().detailDto();
    }

    public ConditionGateReplayResult replay(MinecraftServer server, String recordId) {
        return replayService.replay(server, recordId);
    }

    public static Map<String, Object> recentStatus(ConditionRuntimeTargetType targetType, String targetId) {
        return ConditionGateHistory.latestFor(targetType, targetId)
                .map(WebAdminConditionGateHistoryService::recentStatus)
                .orElseGet(() -> {
                    Map<String, Object> empty = new LinkedHashMap<>();
                    empty.put("configuredHistory", false);
                    empty.put("status", "UNCONFIGURED_OR_NO_HISTORY");
                    empty.put("message", "暂无最近条件判断记录。");
                    empty.put("dataConditionGateRecentStatus", true);
                    return Map.copyOf(empty);
                });
    }

    public static Map<String, Object> recentStatus(ConditionGateHistoryRecord record) {
        if (record == null) {
            return Map.of("configuredHistory", false, "status", "UNCONFIGURED_OR_NO_HISTORY", "message", "暂无最近条件判断记录。");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("configuredHistory", true);
        data.put("recordId", record.id());
        data.put("status", record.result());
        data.put("allowed", record.allowed());
        data.put("code", record.code());
        data.put("conditionGroupId", record.conditionGroupId());
        data.put("conditionGroupDisplayName", record.conditionGroupDisplayName());
        data.put("failureReason", record.failureReason());
        data.put("debugSummary", record.debugSummary());
        data.put("occurredAt", record.occurredAt());
        data.put("targetType", record.targetTypeId());
        data.put("targetId", record.targetId());
        data.put("debuggerRoute", "#/condition-debugger/" + java.net.URLEncoder.encode(record.id(), java.nio.charset.StandardCharsets.UTF_8));
        data.put("dataConditionGateRecentStatus", true);
        return Map.copyOf(data);
    }

    private static boolean containsIgnoreCase(String value, String query) {
        return safe(value).toLowerCase(Locale.ROOT).contains(safe(query).toLowerCase(Locale.ROOT));
    }

    private static String upper(String value) {
        return safe(value).toUpperCase(Locale.ROOT);
    }

    private static int intValue(String value, int fallback) {
        if (safe(value).isBlank()) {
            return fallback;
        }
        try {
            return Math.max(1, Math.min(ConditionGateHistory.MAX_RECORDS, Integer.parseInt(value.trim())));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
