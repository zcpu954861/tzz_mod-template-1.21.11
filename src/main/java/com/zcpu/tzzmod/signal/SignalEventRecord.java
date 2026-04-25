package com.zcpu.tzzmod.signal;

public record SignalEventRecord(
        long gameTime,
        long wallTimeMillis,
        String channel,
        String playerName,
        String sourceType,
        String sourceId,
        int listenerCount,
        int executedCount,
        int skippedCooldownCount,
        int skippedEmptyCount,
        int failedCount,
        int depth,
        String resultMessage
) {
}
