package com.zcpu.tzzmod.signal.join;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SignalJoinRuntimeState {
    public final String joinId;
    public final String scopeKey;
    public final Map<String, ChannelHit> channelHits = new LinkedHashMap<>();
    public long generation = 0L;
    public long firstMatchedAt = 0L;
    public long lastMatchedAt = 0L;
    public long lastOutputAt = 0L;
    public long lastResetAt = 0L;
    public long triggerCount = 0L;
    public int totalCount = 0;
    public boolean latched = false;
    public String lastResult = "";
    public String lastFailureReason = "";
    public String lastResetReason = "";

    public SignalJoinRuntimeState(String joinId, String scopeKey) {
        this.joinId = safe(joinId);
        this.scopeKey = safe(scopeKey).isBlank() ? "global" : safe(scopeKey);
    }

    public void recordHit(String channel, long gameTime) {
        String safeChannel = safe(channel);
        ChannelHit hit = channelHits.computeIfAbsent(safeChannel, ignored -> new ChannelHit(gameTime));
        hit.count++;
        hit.lastMatchedAt = gameTime;
        if (firstMatchedAt <= 0L) {
            firstMatchedAt = gameTime;
        }
        lastMatchedAt = gameTime;
        totalCount++;
        generation++;
        lastResult = "PENDING";
        lastFailureReason = "";
    }

    public void resetPending(long gameTime, String reason, String diagnostic) {
        channelHits.clear();
        firstMatchedAt = 0L;
        lastMatchedAt = 0L;
        totalCount = 0;
        latched = false;
        lastResetAt = gameTime;
        lastResetReason = safe(reason);
        lastResult = safe(diagnostic).isBlank() ? "RESET" : "RESET";
        lastFailureReason = safe(diagnostic);
        generation++;
    }

    public Map<String, Object> toMap(SignalJoinDefinition join) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("joinId", joinId);
        data.put("scopeKey", scopeKey);
        data.put("matchedChannels", channelHits.keySet());
        Map<String, Object> counts = new LinkedHashMap<>();
        for (Map.Entry<String, ChannelHit> entry : channelHits.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().toMap());
        }
        data.put("channelCounts", counts);
        data.put("totalCount", totalCount);
        data.put("firstMatchedAt", firstMatchedAt);
        data.put("lastMatchedAt", lastMatchedAt);
        data.put("lastOutputAt", lastOutputAt);
        data.put("triggerCount", triggerCount);
        data.put("latched", latched);
        data.put("lastResult", lastResult);
        data.put("lastFailureReason", lastFailureReason);
        data.put("lastResetAt", lastResetAt);
        data.put("lastResetReason", lastResetReason);
        data.put("requiredDistinctCount", join == null ? 0 : join.inputChannelNames().size());
        data.put("matchedDistinctCount", channelHits.size());
        data.put("threshold", join == null ? 0 : join.threshold);
        data.put("satisfied", join != null && SignalJoinRuntimeService.isSatisfied(join, this));
        return Map.copyOf(data);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class ChannelHit {
        public int count = 0;
        public long firstMatchedAt = 0L;
        public long lastMatchedAt = 0L;

        public ChannelHit(long firstMatchedAt) {
            this.firstMatchedAt = firstMatchedAt;
            this.lastMatchedAt = firstMatchedAt;
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "count", count,
                    "firstMatchedAt", firstMatchedAt,
                    "lastMatchedAt", lastMatchedAt
            );
        }
    }
}
