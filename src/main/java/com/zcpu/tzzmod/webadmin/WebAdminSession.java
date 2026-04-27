package com.zcpu.tzzmod.webadmin;

public final class WebAdminSession {
    public final String sessionIdHash;
    public final String username;
    public final String role;
    public final long createdAt;
    public final long expiresAt;
    public long lastSeenAt;
    public final String sourceIp;
    public final String userAgent;

    public WebAdminSession(
            String sessionIdHash,
            String username,
            String role,
            long createdAt,
            long expiresAt,
            String sourceIp,
            String userAgent
    ) {
        this.sessionIdHash = sessionIdHash;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.lastSeenAt = createdAt;
        this.sourceIp = sourceIp == null ? "" : sourceIp;
        this.userAgent = userAgent == null ? "" : userAgent;
    }
}
