package com.zcpu.tzzmod.action.schema;

import com.zcpu.tzzmod.action.ActionType;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ActionSchema(
        ActionType actionType,
        String id,
        String displayName,
        String description,
        String helpText,
        List<ActionFieldSchema> fields,
        Set<ActionOwnerType> applicableOwners,
        boolean supportsConditionGroup,
        boolean requiresTargetPicker,
        String editorHint,
        String summaryHint
) {
    public ActionSchema {
        actionType = Objects.requireNonNull(actionType, "actionType");
        id = Objects.requireNonNull(id, "id").trim();
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        description = description == null ? "" : description.trim();
        helpText = helpText == null ? "" : helpText.trim();
        fields = fields == null ? List.of() : List.copyOf(fields);
        applicableOwners = applicableOwners == null ? Set.of() : Set.copyOf(applicableOwners);
        editorHint = editorHint == null ? "" : editorHint.trim();
        summaryHint = summaryHint == null ? "" : summaryHint.trim();
    }
}
