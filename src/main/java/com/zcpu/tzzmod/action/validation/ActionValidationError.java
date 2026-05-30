package com.zcpu.tzzmod.action.validation;

public record ActionValidationError(
        String field,
        String code,
        String message,
        String rejectedValue
) {
    public ActionValidationError {
        field = safe(field);
        code = safe(code);
        message = safe(message);
        rejectedValue = safe(rejectedValue);
    }

    public ActionValidationError withCodeAndMessage(String nextCode, String nextMessage) {
        return new ActionValidationError(field, nextCode, nextMessage, rejectedValue);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
