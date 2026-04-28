package com.zcpu.tzzmod.webadmin.write;

import com.zcpu.tzzmod.webadmin.WebAdminRole;

public record WebAdminPermissionDecision(
        boolean allowed,
        WebAdminRole role,
        WebAdminOperationType operationType,
        String code,
        String message
) {
    public WebAdminPermissionDecision {
        role = role == null ? WebAdminRole.VIEWER : role;
        operationType = operationType == null ? WebAdminOperationType.READ : operationType;
        code = code == null ? "" : code;
        message = message == null || message.isBlank()
                ? (allowed ? "允许执行该操作。" : "权限不足，无法执行该操作。")
                : message;
    }

    public WebAdminWriteResult asWriteResult(WebAdminWriteTarget target) {
        if (allowed) {
            return WebAdminWriteResult.ok(target, false, "权限检查通过。");
        }
        return WebAdminWriteResult.failed(WebAdminWriteResultCode.PERMISSION_DENIED, target, message);
    }
}
