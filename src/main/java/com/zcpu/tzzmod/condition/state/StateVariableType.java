package com.zcpu.tzzmod.condition.state;

import java.util.Locale;
import java.util.Optional;

public enum StateVariableType {
    BOOLEAN,
    INTEGER,
    STRING;

    public static Optional<StateVariableType> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(StateVariableType.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public String displayName() {
        return switch (this) {
            case BOOLEAN -> "布尔";
            case INTEGER -> "整数";
            case STRING -> "文本";
        };
    }
}
