package com.zcpu.tzzmod.condition;

import java.util.List;

public record ConditionTypeMetadata(
        String type,
        String displayName,
        String description,
        String category,
        List<ConditionFieldSchema> fields
) {
    public ConditionTypeMetadata {
        type = normalize(type);
        displayName = safe(displayName);
        description = safe(description);
        category = safe(category);
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
