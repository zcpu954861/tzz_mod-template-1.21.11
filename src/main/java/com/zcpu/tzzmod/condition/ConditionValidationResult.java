package com.zcpu.tzzmod.condition;

import java.util.ArrayList;
import java.util.List;

public record ConditionValidationResult(List<ConditionValidationIssue> issues) {
    public ConditionValidationResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public static ConditionValidationResult ok() {
        return new ConditionValidationResult(List.of());
    }

    public static ConditionValidationResult error(String nodeId, String path, String code, String message) {
        return new ConditionValidationResult(List.of(new ConditionValidationIssue(nodeId, path, code, message)));
    }

    public boolean valid() {
        return issues.isEmpty();
    }

    public ConditionValidationResult merge(ConditionValidationResult other) {
        if (other == null || other.issues().isEmpty()) {
            return this;
        }
        List<ConditionValidationIssue> merged = new ArrayList<>(issues);
        merged.addAll(other.issues());
        return new ConditionValidationResult(merged);
    }
}
