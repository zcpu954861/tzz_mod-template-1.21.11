package com.zcpu.tzzmod.signal.join;

import java.util.Locale;

public enum SignalJoinScopeMode {
    GLOBAL,
    PLAYER;

    public static SignalJoinScopeMode parse(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        for (SignalJoinScopeMode mode : values()) {
            if (mode.name().equals(value)) {
                return mode;
            }
        }
        return GLOBAL;
    }

    public String displayName() {
        return switch (this) {
            case GLOBAL -> "GLOBAL 全局共享";
            case PLAYER -> "PLAYER 按玩家隔离";
        };
    }
}
