package com.zcpu.tzzmod.scheduler;

import java.util.Locale;

public enum TimerStartPolicy {
    RESTART("RESTART", "重新开始"),
    IGNORE_IF_RUNNING("IGNORE_IF_RUNNING", "运行中则忽略"),
    FAIL_IF_RUNNING("FAIL_IF_RUNNING", "运行中则失败");

    private final String id;
    private final String displayName;

    TimerStartPolicy(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static TimerStartPolicy parse(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        for (TimerStartPolicy policy : values()) {
            if (policy.id.equals(value) || policy.name().equals(value)) {
                return policy;
            }
        }
        return null;
    }
}
