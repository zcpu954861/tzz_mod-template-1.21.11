package com.zcpu.tzzmod.condition.state;

import java.util.Locale;
import java.util.Optional;

public enum StateVariableScope {
    GLOBAL,
    PLAYER;

    public static Optional<StateVariableScope> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(StateVariableScope.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public String displayName() {
        return switch (this) {
            case GLOBAL -> "全局";
            case PLAYER -> "玩家";
        };
    }
}
