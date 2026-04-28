package com.zcpu.tzzmod.webadmin.dto;

public record WebAdminEditLockStatusDto(
        String targetType,
        String targetId,
        boolean locked,
        boolean heldByCurrentUser,
        boolean editable,
        String lockId,
        String holderUsername,
        String holderRole,
        String acquiredAt,
        String expiresAt,
        String lastHeartbeatAt
) {
    public WebAdminEditLockStatusDto {
        targetType = safe(targetType);
        targetId = safe(targetId);
        lockId = safe(lockId);
        holderUsername = safe(holderUsername);
        holderRole = safe(holderRole);
        acquiredAt = safe(acquiredAt);
        expiresAt = safe(expiresAt);
        lastHeartbeatAt = safe(lastHeartbeatAt);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
