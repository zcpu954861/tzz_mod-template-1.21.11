package com.zcpu.tzzmod.webadmin;

import com.zcpu.tzzmod.Tzz_mod;

public final class WebAdminAuditLogger {
    private WebAdminAuditLogger() {
    }

    public static void server(String action, WebAdminConfig config) {
        if (config == null || !config.auditEnabled) {
            return;
        }
        Tzz_mod.LOGGER.info("[WebAdminAudit] server {} {}:{} mode={}", action, config.host, config.port, config.accessMode);
    }

    public static void userChanged(String action, String username, String actor) {
        Tzz_mod.LOGGER.info("[WebAdminAudit] user {} username={} actor={}", action, safe(username), safe(actor));
    }

    public static void login(boolean success, String username, String reason) {
        Tzz_mod.LOGGER.info("[WebAdminAudit] login {} username={} reason={}", success ? "success" : "failed", safe(username), safe(reason));
    }

    public static void logout(String username) {
        Tzz_mod.LOGGER.info("[WebAdminAudit] logout username={}", safe(username));
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
