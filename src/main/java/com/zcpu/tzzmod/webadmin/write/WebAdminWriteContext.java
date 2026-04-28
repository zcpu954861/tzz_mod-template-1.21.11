package com.zcpu.tzzmod.webadmin.write;

import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;

public record WebAdminWriteContext(
        String actorUsername,
        WebAdminRole actorRole,
        String sessionHashSummary,
        String remoteAddress,
        WebAdminOperationType operationType,
        WebAdminWriteTarget target
) {
    public WebAdminWriteContext {
        actorUsername = safe(actorUsername);
        actorRole = actorRole == null ? WebAdminRole.VIEWER : actorRole;
        sessionHashSummary = summarizeSessionHash(sessionHashSummary);
        remoteAddress = safe(remoteAddress);
        operationType = operationType == null ? WebAdminOperationType.READ : operationType;
        target = target == null ? WebAdminWriteTarget.none() : target;
    }

    public static WebAdminWriteContext of(
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminOperationType operationType,
            WebAdminWriteTarget target
    ) {
        return new WebAdminWriteContext(
                user == null ? "" : user.username,
                user == null ? WebAdminRole.VIEWER : user.roleEnum(),
                session == null ? "" : session.sessionIdHash,
                remoteAddress,
                operationType,
                target
        );
    }

    private static String summarizeSessionHash(String value) {
        String safe = safe(value);
        return safe.length() <= 12 ? safe : safe.substring(0, 12);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
