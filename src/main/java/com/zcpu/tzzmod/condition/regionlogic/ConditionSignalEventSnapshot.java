package com.zcpu.tzzmod.condition.regionlogic;

import com.zcpu.tzzmod.signal.SignalChannel;
import java.util.Map;
import java.util.TreeMap;

public record ConditionSignalEventSnapshot(
        String channel,
        String sourceType,
        String sourceId,
        long gameTime,
        long wallTimeMillis,
        Map<String, String> metadata
) {
    public ConditionSignalEventSnapshot {
        channel = SignalChannel.normalize(channel);
        sourceType = safe(sourceType);
        sourceId = safe(sourceId);
        gameTime = Math.max(0L, gameTime);
        wallTimeMillis = Math.max(0L, wallTimeMillis);
        metadata = copyMap(metadata);
    }

    public static ConditionSignalEventSnapshot of(String channel, String sourceType, String sourceId, long gameTime) {
        return new ConditionSignalEventSnapshot(channel, sourceType, sourceId, gameTime, 0L, Map.of());
    }

    public boolean matchesFilter(String channelFilter, String sourceTypeFilter, String sourceIdFilter) {
        String expectedChannel = SignalChannel.normalize(channelFilter);
        if (!expectedChannel.isBlank() && !channel.equals(expectedChannel)) {
            return false;
        }
        String expectedSourceType = safe(sourceTypeFilter);
        if (!expectedSourceType.isBlank() && !sourceType.equals(expectedSourceType)) {
            return false;
        }
        String expectedSourceId = safe(sourceIdFilter);
        return expectedSourceId.isBlank() || sourceId.equals(expectedSourceId);
    }

    private static Map<String, String> copyMap(Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        TreeMap<String, String> copy = new TreeMap<>();
        raw.forEach((key, value) -> {
            String safeKey = safe(key);
            if (!safeKey.isBlank()) {
                copy.put(safeKey, safe(value));
            }
        });
        return Map.copyOf(copy);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
