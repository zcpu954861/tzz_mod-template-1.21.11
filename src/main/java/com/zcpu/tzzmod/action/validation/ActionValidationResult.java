package com.zcpu.tzzmod.action.validation;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import java.util.List;
import java.util.Optional;

public record ActionValidationResult(
        ActionType actionType,
        ActionConfig normalizedAction,
        List<ActionValidationError> errors
) {
    public ActionValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public boolean valid() {
        return errors.isEmpty();
    }

    public Optional<ActionConfig> action() {
        return Optional.ofNullable(normalizedAction);
    }
}
