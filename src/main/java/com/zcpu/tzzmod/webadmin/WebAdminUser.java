package com.zcpu.tzzmod.webadmin;

public final class WebAdminUser {
    public String username = "";
    public String displayName = "";
    public String role = WebAdminRole.VIEWER.id();
    public boolean enabled = true;
    public String passwordHash = "";
    public String passwordSalt = "";
    public String passwordAlgorithm = WebAdminPasswordHasher.ALGORITHM;
    public int passwordIterations = WebAdminPasswordHasher.DEFAULT_ITERATIONS;
    public long createdAt = 0L;
    public String createdBy = "";
    public long lastLoginAt = 0L;
    public int failedLoginCount = 0;
    public long lockedUntil = 0L;
    public boolean forcePasswordChange = false;

    public WebAdminUser normalized() {
        username = clean(username);
        if (displayName == null || displayName.isBlank()) {
            displayName = username;
        }
        WebAdminRole parsedRole = WebAdminRole.parse(role);
        role = parsedRole == null ? WebAdminRole.VIEWER.id() : parsedRole.id();
        if (passwordAlgorithm == null || passwordAlgorithm.isBlank()) {
            passwordAlgorithm = WebAdminPasswordHasher.ALGORITHM;
        }
        if (passwordIterations <= 0) {
            passwordIterations = WebAdminPasswordHasher.DEFAULT_ITERATIONS;
        }
        if (passwordHash == null) {
            passwordHash = "";
        }
        if (passwordSalt == null) {
            passwordSalt = "";
        }
        if (createdBy == null) {
            createdBy = "";
        }
        return this;
    }

    public WebAdminRole roleEnum() {
        WebAdminRole parsed = WebAdminRole.parse(role);
        return parsed == null ? WebAdminRole.VIEWER : parsed;
    }

    static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
