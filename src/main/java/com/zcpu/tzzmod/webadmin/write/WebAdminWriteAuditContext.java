package com.zcpu.tzzmod.webadmin.write;

import com.zcpu.tzzmod.webadmin.WebAdminRole;

public record WebAdminWriteAuditContext(
        String actorUsername,
        WebAdminRole actorRole,
        String sessionHashSummary,
        String remoteAddress,
        WebAdminOperationType operationType,
        WebAdminWriteTarget target
) {
    public WebAdminWriteAuditContext {
        actorUsername = safe(actorUsername);
        actorRole = actorRole == null ? WebAdminRole.VIEWER : actorRole;
        sessionHashSummary = safe(sessionHashSummary);
        remoteAddress = safe(remoteAddress);
        operationType = operationType == null ? WebAdminOperationType.READ : operationType;
        target = target == null ? WebAdminWriteTarget.none() : target;
    }

    public static WebAdminWriteAuditContext from(WebAdminWriteContext context) {
        if (context == null) {
            return new WebAdminWriteAuditContext("", WebAdminRole.VIEWER, "", "", WebAdminOperationType.READ, WebAdminWriteTarget.none());
        }
        return new WebAdminWriteAuditContext(
                context.actorUsername(),
                context.actorRole(),
                context.sessionHashSummary(),
                context.remoteAddress(),
                context.operationType(),
                context.target()
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
