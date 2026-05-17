package com.zcpu.tzzmod.scheduler;

import java.util.LinkedHashMap;
import java.util.Map;

public record TimerOperationResult(
        boolean success,
        boolean changed,
        String code,
        String message,
        String timerId,
        String scopeKey,
        Map<String, Object> details
) {
    public TimerOperationResult {
        code = code == null ? "" : code;
        message = message == null ? "" : message;
        timerId = TimerStore.normalizeId(timerId);
        scopeKey = scopeKey == null ? "" : scopeKey.trim();
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static TimerOperationResult success(String code, String message, String timerId, String scopeKey, boolean changed) {
        return new TimerOperationResult(true, changed, code, message, timerId, scopeKey, Map.of());
    }

    public static TimerOperationResult failure(String code, String message, String timerId, String scopeKey) {
        return new TimerOperationResult(false, false, code, message, timerId, scopeKey, Map.of());
    }

    public TimerOperationResult withDetail(String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(details);
        copy.put(key, value);
        return new TimerOperationResult(success, changed, code, message, timerId, scopeKey, copy);
    }

    public Map<String, Object> actionDetails() {
        Map<String, Object> data = new LinkedHashMap<>(details);
        data.put("actionType", "timer");
        data.put("timerId", timerId);
        data.put("scopeKey", scopeKey);
        data.put("changed", changed);
        data.put("failureReason", success ? "" : message);
        return Map.copyOf(data);
    }
}
