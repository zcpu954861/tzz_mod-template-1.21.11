package com.zcpu.tzzmod.condition.state;

import java.util.Locale;
import java.util.Optional;

public enum StateVariableMutationOperation {
    SET_VARIABLE("set_variable", "设置变量"),
    INCREMENT_VARIABLE("increment_variable", "增加整数变量"),
    DECREMENT_VARIABLE("decrement_variable", "减少整数变量"),
    TOGGLE_BOOLEAN("toggle_boolean", "切换布尔变量"),
    CLEAR_VARIABLE("clear_variable", "清除变量");

    private final String id;
    private final String displayName;

    StateVariableMutationOperation(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<StateVariableMutationOperation> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (StateVariableMutationOperation operation : values()) {
            if (operation.id.equals(normalized) || operation.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(operation);
            }
        }
        return Optional.empty();
    }
}
