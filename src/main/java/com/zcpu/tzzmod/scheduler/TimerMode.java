package com.zcpu.tzzmod.scheduler;

import java.util.Locale;

public enum TimerMode {
    DELAY("DELAY", "延迟执行"),
    COUNTDOWN("COUNTDOWN", "倒计时"),
    REPEAT("REPEAT", "重复执行");

    private final String id;
    private final String displayName;

    TimerMode(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static TimerMode parse(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        for (TimerMode mode : values()) {
            if (mode.id.equals(value) || mode.name().equals(value)) {
                return mode;
            }
        }
        return null;
    }
}
