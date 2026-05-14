package com.zcpu.tzzmod.condition;

import java.util.Locale;

public enum ConditionGroupMode {
    AND("and"),
    OR("or"),
    NOT("not");

    private final String id;

    ConditionGroupMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ConditionGroupMode fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return AND;
        }
        String id = raw.trim().toLowerCase(Locale.ROOT);
        for (ConditionGroupMode value : values()) {
            if (value.id.equals(id) || value.name().equalsIgnoreCase(id)) {
                return value;
            }
        }
        return AND;
    }
}
