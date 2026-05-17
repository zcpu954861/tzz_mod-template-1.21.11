package com.zcpu.tzzmod.scheduler;

import java.util.Locale;

public enum TimerTargetMode {
    GLOBAL("global", "全局"),
    CONTEXT_PLAYER("context_player", "触发玩家"),
    EXPLICIT_TARGET("explicit_target", "指定玩家");

    private final String id;
    private final String displayName;

    TimerTargetMode(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static TimerTargetMode parse(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        for (TimerTargetMode mode : values()) {
            if (mode.id.equals(value) || mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return null;
    }
}
