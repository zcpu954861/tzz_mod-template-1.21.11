package com.zcpu.tzzmod.webadmin.write;

public record WebAdminValidationError(
        String field,
        String code,
        String message,
        String rejectedValueSummary
) {
    public WebAdminValidationError {
        field = safe(field);
        code = safe(code);
        message = safe(message);
        rejectedValueSummary = WebAdminWriteSanitizer.summarize(rejectedValueSummary);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
