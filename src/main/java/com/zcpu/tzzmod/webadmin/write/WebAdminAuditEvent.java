package com.zcpu.tzzmod.webadmin.write;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WebAdminAuditEvent(
        String auditId,
        String occurredAt,
        String actorUsername,
        String actorRole,
        String sessionIdHashSummary,
        String remoteAddress,
        String operationType,
        String targetType,
        String targetId,
        String targetDisplayName,
        Map<String, Object> beforeSummary,
        Map<String, Object> afterSummary,
        String result,
        String errorCode,
        String message
) {
    public WebAdminAuditEvent {
        auditId = safe(auditId);
        occurredAt = safe(occurredAt);
        actorUsername = safe(actorUsername);
        actorRole = safe(actorRole);
        sessionIdHashSummary = safe(sessionIdHashSummary);
        remoteAddress = safe(remoteAddress);
        operationType = safe(operationType);
        targetType = safe(targetType);
        targetId = safe(targetId);
        targetDisplayName = safe(targetDisplayName);
        beforeSummary = WebAdminWriteSanitizer.redactMap(beforeSummary);
        afterSummary = WebAdminWriteSanitizer.redactMap(afterSummary);
        result = safe(result);
        errorCode = safe(errorCode);
        message = safe(message);
    }

    public static WebAdminAuditEvent of(
            WebAdminWriteAuditContext context,
            WebAdminAuditResult result,
            WebAdminWriteResult writeResult,
            Map<String, ?> beforeSummary,
            Map<String, ?> afterSummary
    ) {
        WebAdminWriteAuditContext safeContext = context == null
                ? new WebAdminWriteAuditContext("", null, "", "", null, null)
                : context;
        WebAdminAuditResult auditResult = result == null ? WebAdminAuditResult.FAILED : result;
        WebAdminWriteTarget target = safeContext.target();
        return new WebAdminAuditEvent(
                UUID.randomUUID().toString(),
                Instant.now().toString(),
                safeContext.actorUsername(),
                safeContext.actorRole().id(),
                safeContext.sessionHashSummary(),
                safeContext.remoteAddress(),
                safeContext.operationType().id(),
                target.targetType(),
                target.targetId(),
                target.displayName(),
                WebAdminWriteSanitizer.redactMap(beforeSummary),
                WebAdminWriteSanitizer.redactMap(afterSummary),
                auditResult.id(),
                writeResult == null ? "" : writeResult.code(),
                writeResult == null ? auditResult.displayName() : writeResult.message()
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
