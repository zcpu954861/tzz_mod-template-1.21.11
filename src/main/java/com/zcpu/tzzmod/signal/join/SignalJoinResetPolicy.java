package com.zcpu.tzzmod.signal.join;

import java.util.Locale;

public enum SignalJoinResetPolicy {
    RESET_AFTER_EMIT,
    LATCH_UNTIL_MANUAL_RESET;

    public static SignalJoinResetPolicy parse(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        for (SignalJoinResetPolicy policy : values()) {
            if (policy.name().equals(value)) {
                return policy;
            }
        }
        return RESET_AFTER_EMIT;
    }

    public String displayName() {
        return switch (this) {
            case RESET_AFTER_EMIT -> "输出后清空，可重复触发";
            case LATCH_UNTIL_MANUAL_RESET -> "输出后锁存，手动重置前不再重复";
        };
    }
}
