package com.zcpu.tzzmod.webadmin.write;

import java.util.List;
import java.util.Map;

public record WebAdminWriteResult(
        boolean success,
        String code,
        String message,
        String targetType,
        String targetId,
        boolean changed,
        List<WebAdminValidationError> validationErrors,
        String auditId,
        String realtimeEventId,
        boolean requiresConfirmation,
        Map<String, Object> conflict,
        Map<String, Object> data
) {
    public WebAdminWriteResult {
        code = safe(code);
        message = safe(message);
        targetType = safe(targetType);
        targetId = safe(targetId);
        validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
        auditId = safe(auditId);
        realtimeEventId = safe(realtimeEventId);
        conflict = WebAdminWriteSanitizer.redactMap(conflict);
        data = WebAdminWriteSanitizer.redactMap(data);
    }

    public static WebAdminWriteResult ok(WebAdminWriteTarget target, boolean changed, String message) {
        WebAdminWriteTarget safeTarget = target == null ? WebAdminWriteTarget.none() : target;
        return new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                isBlank(message) ? WebAdminWriteResultCode.OK.defaultMessage() : message,
                safeTarget.targetType(),
                safeTarget.targetId(),
                changed,
                List.of(),
                "",
                "",
                false,
                Map.of(),
                Map.of()
        );
    }

    public static WebAdminWriteResult failed(WebAdminWriteResultCode code, WebAdminWriteTarget target, String message) {
        WebAdminWriteResultCode resultCode = code == null ? WebAdminWriteResultCode.INTERNAL_ERROR : code;
        WebAdminWriteTarget safeTarget = target == null ? WebAdminWriteTarget.none() : target;
        return new WebAdminWriteResult(
                false,
                resultCode.id(),
                isBlank(message) ? resultCode.defaultMessage() : message,
                safeTarget.targetType(),
                safeTarget.targetId(),
                false,
                List.of(),
                "",
                "",
                resultCode == WebAdminWriteResultCode.DANGEROUS_OPERATION_REQUIRES_CONFIRMATION,
                Map.of(),
                Map.of()
        );
    }

    public static WebAdminWriteResult validationFailed(WebAdminWriteTarget target, List<WebAdminValidationError> errors) {
        WebAdminWriteTarget safeTarget = target == null ? WebAdminWriteTarget.none() : target;
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.VALIDATION_FAILED.id(),
                WebAdminWriteResultCode.VALIDATION_FAILED.defaultMessage(),
                safeTarget.targetType(),
                safeTarget.targetId(),
                false,
                errors == null ? List.of() : List.copyOf(errors),
                "",
                "",
                false,
                Map.of(),
                Map.of()
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
