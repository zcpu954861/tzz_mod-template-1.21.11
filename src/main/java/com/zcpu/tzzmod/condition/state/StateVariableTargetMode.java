package com.zcpu.tzzmod.condition.state;

import java.util.Locale;
import java.util.Optional;

public enum StateVariableTargetMode {
    GLOBAL("global", "全局"),
    CONTEXT_PLAYER("context_player", "触发玩家"),
    EXPLICIT_TARGET("explicit_target", "显式目标");

    private final String id;
    private final String displayName;

    StateVariableTargetMode(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<StateVariableTargetMode> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (StateVariableTargetMode mode : values()) {
            if (mode.id.equals(normalized) || mode.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }
}
