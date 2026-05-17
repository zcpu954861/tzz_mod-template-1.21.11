package com.zcpu.tzzmod.scheduler;

import java.util.Locale;

public enum TimerScopeMode {
    GLOBAL("GLOBAL", "全局"),
    PLAYER("PLAYER", "玩家");

    private final String id;
    private final String displayName;

    TimerScopeMode(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static TimerScopeMode parse(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        for (TimerScopeMode mode : values()) {
            if (mode.id.equals(value) || mode.name().equals(value)) {
                return mode;
            }
        }
        return null;
    }
}
