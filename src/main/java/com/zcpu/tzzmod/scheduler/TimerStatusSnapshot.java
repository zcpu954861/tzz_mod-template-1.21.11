package com.zcpu.tzzmod.scheduler;

import java.util.List;
import java.util.Map;

public record TimerStatusSnapshot(
        String timerId,
        boolean enabled,
        String mode,
        String scopeMode,
        String startPolicy,
        int activeInstanceCount,
        long lastStatusAt,
        String lastResult,
        String lastFailureReason,
        List<Map<String, Object>> instances,
        boolean runtimeStatePersistent
) {
    public TimerStatusSnapshot {
        timerId = TimerStore.normalizeId(timerId);
        mode = mode == null ? "" : mode;
        scopeMode = scopeMode == null ? "" : scopeMode;
        startPolicy = startPolicy == null ? "" : startPolicy;
        lastResult = lastResult == null ? "" : lastResult;
        lastFailureReason = lastFailureReason == null ? "" : lastFailureReason;
        instances = instances == null ? List.of() : List.copyOf(instances);
    }
}
