package com.zcpu.tzzmod.webadmin;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class WebAdminAuditLogger {
    private static WebAdminStoragePaths storagePaths;
    private static boolean auditEnabled;

    private WebAdminAuditLogger() {
    }

    public static synchronized void configure(WebAdminStoragePaths paths, WebAdminConfig config) {
        storagePaths = paths;
        auditEnabled = config != null && config.auditEnabled;
        if (paths != null) {
            paths.ensureDirectory();
        }
    }

    public static void server(String action, WebAdminConfig config) {
        if (config == null || !config.auditEnabled) {
            return;
        }
        Tzz_mod.LOGGER.info("[WebAdminAudit] server {} {}:{} mode={}", action, config.host, config.port, config.accessMode);
        append("server action=" + safe(action) + " host=" + config.host + " port=" + config.port + " mode=" + config.accessMode);
    }

    public static void userChanged(String action, String username, String actor) {
        Tzz_mod.LOGGER.info("[WebAdminAudit] user {} username={} actor={}", action, safe(username), safe(actor));
        append("user action=" + safe(action) + " username=" + safe(username) + " actor=" + safe(actor));
    }

    public static void login(boolean success, String username, String reason) {
        Tzz_mod.LOGGER.info("[WebAdminAudit] login {} username={} reason={}", success ? "success" : "failed", safe(username), safe(reason));
        append("login result=" + (success ? "success" : "failed") + " username=" + safe(username) + " reason=" + safe(reason));
    }

    public static void logout(String username) {
        Tzz_mod.LOGGER.info("[WebAdminAudit] logout username={}", safe(username));
        append("logout username=" + safe(username));
    }

    public static void writeEvent(WebAdminAuditEvent event) {
        if (event == null) {
            return;
        }
        Tzz_mod.LOGGER.info(
                "[WebAdminAudit] write operation={} targetType={} targetId={} result={} actor={}",
                safe(event.operationType()),
                safe(event.targetType()),
                safe(event.targetId()),
                safe(event.result()),
                safe(event.actorUsername())
        );
        append("write event=" + WebAdminJsonResponse.GSON.toJson(event));
    }

    private static synchronized void append(String line) {
        if (!auditEnabled || storagePaths == null) {
            return;
        }
        try {
            storagePaths.ensureDirectory();
            Files.writeString(
                    storagePaths.auditLogPath(),
                    Instant.now() + " " + line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to write WebAdmin audit log: {}", exception.getMessage());
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
