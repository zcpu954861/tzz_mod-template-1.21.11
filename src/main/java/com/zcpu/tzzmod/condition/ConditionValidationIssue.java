package com.zcpu.tzzmod.condition;

public record ConditionValidationIssue(
        String nodeId,
        String path,
        String code,
        String message
) {
    public ConditionValidationIssue {
        nodeId = safe(nodeId);
        path = safe(path);
        code = safe(code);
        message = safe(message);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
