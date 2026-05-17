package com.zcpu.tzzmod.signal.join;

import java.util.Locale;

public enum SignalJoinMode {
    ALL,
    ANY_N,
    COUNT;

    public static SignalJoinMode parse(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        for (SignalJoinMode mode : values()) {
            if (mode.name().equals(value)) {
                return mode;
            }
        }
        return ALL;
    }

    public String displayName() {
        return switch (this) {
            case ALL -> "ALL 所有输入均到达";
            case ANY_N -> "ANY_N 任意 N 个输入到达";
            case COUNT -> "COUNT 累计输入次数";
        };
    }
}
