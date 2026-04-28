package com.zcpu.tzzmod.webadmin.write;

import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WebAdminPermissionService {
    public WebAdminPermissionDecision decide(WebAdminUser user, WebAdminOperationType operationType) {
        if (user == null) {
            return new WebAdminPermissionDecision(
                    false,
                    WebAdminRole.VIEWER,
                    operationType,
                    WebAdminWriteResultCode.UNAUTHENTICATED.id(),
                    WebAdminWriteResultCode.UNAUTHENTICATED.defaultMessage()
            );
        }
        return decide(user.roleEnum(), operationType);
    }

    public WebAdminPermissionDecision decide(WebAdminRole role, WebAdminOperationType operationType) {
        WebAdminRole safeRole = role == null ? WebAdminRole.VIEWER : role;
        WebAdminOperationType operation = operationType == null ? WebAdminOperationType.READ : operationType;
        boolean allowed = WebAdminRolePolicy.allows(safeRole, operation);
        return new WebAdminPermissionDecision(
                allowed,
                safeRole,
                operation,
                allowed ? WebAdminWriteResultCode.OK.id() : WebAdminWriteResultCode.PERMISSION_DENIED.id(),
                allowed
                        ? "允许执行：" + operation.displayName() + "。"
                        : safeRole.displayName() + " 无权执行：" + operation.displayName() + "。"
        );
    }

    public Map<String, Object> capabilitySummary(WebAdminRole role) {
        WebAdminRole safeRole = role == null ? WebAdminRole.VIEWER : role;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("role", safeRole.id());
        summary.put("roleDisplayName", safeRole.displayName());
        Map<String, Boolean> operations = new LinkedHashMap<>();
        for (WebAdminOperationType operation : WebAdminOperationType.values()) {
            operations.put(operation.id(), WebAdminRolePolicy.allows(safeRole, operation));
        }
        summary.put("operations", operations);
        return summary;
    }
}
