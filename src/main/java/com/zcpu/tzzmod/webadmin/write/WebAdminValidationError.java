package com.zcpu.tzzmod.webadmin.write;

public record WebAdminValidationError(
        String field,
        String code,
        String message,
        String rejectedValueSummary,
        String nodeId,
        String edgeId,
        String channelId,
        String severity,
        String fixHint
) {
    public WebAdminValidationError(String field, String code, String message, String rejectedValueSummary) {
        this(field, code, message, rejectedValueSummary, "", "", "", "error", "");
    }

    public WebAdminValidationError {
        field = safe(field);
        code = safe(code);
        message = safe(message);
        rejectedValueSummary = WebAdminWriteSanitizer.isSensitiveKey(field)
                ? "已隐藏"
                : WebAdminWriteSanitizer.summarize(rejectedValueSummary);
        nodeId = safe(nodeId);
        edgeId = safe(edgeId);
        channelId = safe(channelId);
        severity = safe(severity).isBlank() ? "error" : safe(severity);
        fixHint = safe(fixHint);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
