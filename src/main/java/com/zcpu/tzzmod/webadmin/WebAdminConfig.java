package com.zcpu.tzzmod.webadmin;

public final class WebAdminConfig {
    public boolean enabled = false;
    public String host = "127.0.0.1";
    public int port = 18080;
    public String accessMode = WebAdminAccessMode.LOCAL_ONLY.id();
    public int sessionTtlMinutes = 120;
    public int rememberMeTtlMinutes = 120;
    public int loginCodeTtlSeconds = 120;
    public boolean auditEnabled = true;
    public boolean secureCookie = false;

    public WebAdminConfig normalized() {
        if (host == null || host.isBlank()) {
            host = "127.0.0.1";
        }
        if (port < 1 || port > 65_535) {
            port = 18080;
        }
        accessMode = WebAdminAccessMode.parse(accessMode).id();
        if (sessionTtlMinutes < 5) {
            sessionTtlMinutes = 120;
        }
        if (rememberMeTtlMinutes < 5) {
            rememberMeTtlMinutes = sessionTtlMinutes;
        }
        if (loginCodeTtlSeconds < 30) {
            loginCodeTtlSeconds = 120;
        }
        return this;
    }

    public WebAdminAccessMode accessModeEnum() {
        return WebAdminAccessMode.parse(accessMode);
    }

    public int effectiveSessionTtlSeconds(boolean rememberMe) {
        int minutes = rememberMe ? rememberMeTtlMinutes : sessionTtlMinutes;
        return Math.max(1, minutes) * 60;
    }
}
