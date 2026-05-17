package com.zcpu.tzzmod.signal.join;

public record SignalJoinValidationIssue(String field, String code, String message, String rejectedValue) {
    public SignalJoinValidationIssue {
        field = safe(field);
        code = safe(code);
        message = safe(message);
        rejectedValue = safe(rejectedValue);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
