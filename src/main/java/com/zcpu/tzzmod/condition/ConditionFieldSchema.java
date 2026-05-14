package com.zcpu.tzzmod.condition;

public record ConditionFieldSchema(
        String name,
        String displayName,
        String kind,
        boolean required,
        String description
) {
    public ConditionFieldSchema {
        name = safe(name);
        displayName = safe(displayName);
        kind = safe(kind);
        description = safe(description);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
