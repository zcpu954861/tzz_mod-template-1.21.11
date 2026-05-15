package com.zcpu.tzzmod.condition.state;

public record StateVariableValue(
        StateVariableType type,
        String value
) {
    public StateVariableValue {
        type = type == null ? StateVariableType.STRING : type;
        value = StateVariableValidation.normalizeValue(type, value);
    }

    public boolean asBoolean() {
        return Boolean.parseBoolean(value);
    }

    public long asInteger() {
        return Long.parseLong(value);
    }
}
