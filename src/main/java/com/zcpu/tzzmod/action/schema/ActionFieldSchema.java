package com.zcpu.tzzmod.action.schema;

import java.util.List;
import java.util.Objects;

public record ActionFieldSchema(
        String id,
        String label,
        String description,
        ActionFieldType type,
        boolean required,
        String defaultValue,
        Integer maxLength,
        Long minNumber,
        Long maxNumber,
        List<ActionFieldOption> options,
        String editorHint,
        String summaryHint
) {
    public ActionFieldSchema {
        id = Objects.requireNonNull(id, "id").trim();
        label = Objects.requireNonNull(label, "label").trim();
        description = description == null ? "" : description.trim();
        type = Objects.requireNonNull(type, "type");
        defaultValue = defaultValue == null ? "" : defaultValue;
        options = options == null ? List.of() : List.copyOf(options);
        editorHint = editorHint == null ? "" : editorHint.trim();
        summaryHint = summaryHint == null ? "" : summaryHint.trim();
    }
}
