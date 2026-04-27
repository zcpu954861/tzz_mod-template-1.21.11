package com.zcpu.tzzmod.webadmin;

import com.zcpu.tzzmod.Tzz_mod;
import net.minecraft.server.MinecraftServer;

public final class WebAdminLifecycle {
    private static WebAdminConfig config;
    private static WebAdminUserService userService;
    private static WebAdminSessionService sessionService;
    private static WebAdminServer server;

    private WebAdminLifecycle() {
    }

    public static synchronized void start(MinecraftServer minecraftServer) {
        config = WebAdminConfigStore.load(minecraftServer);
        userService = new WebAdminUserService(minecraftServer);
        sessionService = new WebAdminSessionService();
        if (!config.enabled) {
            Tzz_mod.LOGGER.info("WebAdmin config loaded but server is disabled.");
            return;
        }
        server = new WebAdminServer(minecraftServer, config, userService, sessionService);
        try {
            server.start();
        } catch (Exception exception) {
            Tzz_mod.LOGGER.error("Failed to start WebAdmin server: {}", exception.getMessage());
            server = null;
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
        if (sessionService != null) {
            sessionService.clear();
        }
    }

    public static synchronized WebAdminRuntimeStatus status(MinecraftServer minecraftServer) {
        ensureLoaded(minecraftServer);
        return new WebAdminRuntimeStatus(config, server != null && server.running(),
                sessionService == null ? 0 : sessionService.sessionCount(),
                userService == null ? 0 : userService.userCount());
    }

    public static synchronized WebAdminUserService userService(MinecraftServer minecraftServer) {
        ensureLoaded(minecraftServer);
        return userService;
    }

    public static synchronized void reloadStores(MinecraftServer minecraftServer) {
        config = WebAdminConfigStore.load(minecraftServer);
        userService = new WebAdminUserService(minecraftServer);
        if (sessionService == null) {
            sessionService = new WebAdminSessionService();
        }
    }

    private static void ensureLoaded(MinecraftServer minecraftServer) {
        if (config == null || userService == null || sessionService == null) {
            reloadStores(minecraftServer);
        }
    }

    public record WebAdminRuntimeStatus(WebAdminConfig config, boolean running, int sessionCount, int userCount) {
    }
}
