package com.zcpu.tzzmod.webadmin.write;

public record WebAdminEditLock(
        String lockId,
        String targetType,
        String targetId,
        String holderUsername,
        String holderRole,
        String holderSessionHashSummary,
        long acquiredAtMillis,
        long expiresAtMillis,
        long lastHeartbeatAtMillis
) {
    public WebAdminEditLock {
        lockId = safe(lockId);
        targetType = safe(targetType);
        targetId = safe(targetId);
        holderUsername = safe(holderUsername);
        holderRole = safe(holderRole);
        holderSessionHashSummary = summarizeSession(holderSessionHashSummary);
    }

    public boolean expired(long nowMillis) {
        return expiresAtMillis <= nowMillis;
    }

    public boolean heldBySession(String sessionHashSummary) {
        return !holderSessionHashSummary.isBlank()
                && holderSessionHashSummary.equals(summarizeSession(sessionHashSummary));
    }

    public WebAdminEditLock renew(long nowMillis, long ttlMillis) {
        return new WebAdminEditLock(
                lockId,
                targetType,
                targetId,
                holderUsername,
                holderRole,
                holderSessionHashSummary,
                acquiredAtMillis,
                nowMillis + ttlMillis,
                nowMillis
        );
    }

    private static String summarizeSession(String value) {
        String safe = safe(value);
        return safe.length() <= 12 ? safe : safe.substring(0, 12);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
