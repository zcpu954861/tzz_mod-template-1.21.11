package com.zcpu.tzzmod.webadmin.write;

import java.util.Map;

public final class WebAdminAuditWriter {
    private WebAdminAuditWriter() {
    }

    public static WebAdminAuditEvent eventForResult(
            WebAdminWriteAuditContext context,
            WebAdminWriteResult result,
            Map<String, ?> beforeSummary,
            Map<String, ?> afterSummary
    ) {
        WebAdminAuditResult auditResult;
        if (result != null && result.success()) {
            auditResult = WebAdminAuditResult.SUCCESS;
        } else if (result != null && WebAdminWriteResultCode.PERMISSION_DENIED.id().equals(result.code())) {
            auditResult = WebAdminAuditResult.DENIED;
        } else if (result != null && WebAdminWriteResultCode.VALIDATION_FAILED.id().equals(result.code())) {
            auditResult = WebAdminAuditResult.VALIDATION_FAILED;
        } else {
            auditResult = WebAdminAuditResult.FAILED;
        }
        return WebAdminAuditEvent.of(context, auditResult, result, beforeSummary, afterSummary);
    }
}
