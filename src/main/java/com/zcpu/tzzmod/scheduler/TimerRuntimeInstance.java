package com.zcpu.tzzmod.scheduler;

import java.util.LinkedHashMap;
import java.util.Map;

final class TimerRuntimeInstance {
    final TimerDefinition definition;
    final String scopeKey;
    final String playerId;
    final String playerName;
    final String worldId;
    final double x;
    final double y;
    final double z;
    final long startedAtTick;
    final long deadlineTick;
    long nextTickAt;
    long lastTickAt = 0L;
    long completedAtTick = 0L;
    long cancelledAtTick = 0L;
    int runCount = 0;
    String lastResult = "STARTED";
    String lastFailureReason = "";

    TimerRuntimeInstance(
            TimerDefinition definition,
            String scopeKey,
            String playerId,
            String playerName,
            String worldId,
            double x,
            double y,
            double z,
            long startedAtTick,
            long durationOverrideTicks
    ) {
        this.definition = definition == null ? new TimerDefinition().normalized() : definition.normalized();
        this.scopeKey = scopeKey == null ? "" : scopeKey.trim();
        this.playerId = playerId == null ? "" : playerId.trim();
        this.playerName = playerName == null ? "" : playerName.trim();
        this.worldId = worldId == null ? "" : worldId.trim();
        this.x = x;
        this.y = y;
        this.z = z;
        this.startedAtTick = Math.max(0L, startedAtTick);
        long duration = durationOverrideTicks >= 0L ? durationOverrideTicks : this.definition.durationTicks;
        this.deadlineTick = Math.max(0L, startedAtTick + Math.max(0L, duration));
        this.nextTickAt = this.definition.mode == TimerMode.REPEAT
                ? Math.max(0L, startedAtTick + Math.max(1L, this.definition.intervalTicks))
                : Math.max(0L, startedAtTick + Math.max(1L, this.definition.intervalTicks));
    }

    long remainingTicks(long now) {
        if (definition.mode == TimerMode.REPEAT) {
            return Math.max(0L, nextTickAt - now);
        }
        return Math.max(0L, deadlineTick - now);
    }

    long remainingRuns() {
        if (definition.mode != TimerMode.REPEAT) {
            return 0L;
        }
        if (definition.maxRuns <= 0) {
            return -1L;
        }
        return Math.max(0L, definition.maxRuns - runCount);
    }

    Map<String, Object> toMap(long now) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timerId", definition.id);
        data.put("scopeKey", scopeKey);
        data.put("mode", definition.mode.name());
        data.put("scopeMode", definition.scopeMode.name());
        data.put("playerId", playerId);
        data.put("playerName", playerName);
        data.put("worldId", worldId);
        data.put("startedAtTick", startedAtTick);
        data.put("deadlineTick", deadlineTick);
        data.put("nextTickAt", nextTickAt);
        data.put("remainingTicks", remainingTicks(now));
        data.put("nextFireInTicks", Math.max(0L, nextTickAt - now));
        data.put("remainingRuns", remainingRuns());
        data.put("runCount", runCount);
        data.put("lastTickAt", lastTickAt);
        data.put("completedAtTick", completedAtTick);
        data.put("cancelledAtTick", cancelledAtTick);
        data.put("lastResult", lastResult);
        data.put("lastFailureReason", lastFailureReason);
        return Map.copyOf(data);
    }
}
