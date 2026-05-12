package com.zcpu.tzzmod.webadmin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceBasicConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceExtendedConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminInteractionItemMatcherUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceMetadataUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminChannelMetadataUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminContainerTemplateSessionCancelRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminContainerTemplateSessionStartRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSingleItemSubmitTemplateSessionCancelRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSingleItemSubmitTemplateSessionStartRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSelectionCancelRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSelectionStartRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerBasicConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerCreateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerDeleteRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminVirtualBlockDeviceDeleteRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest;
import com.zcpu.tzzmod.webadmin.route.WebAdminReadonlyRoutes;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeService;
import com.zcpu.tzzmod.webadmin.service.WebAdminActionRelayActionsService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceBasicConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceExtendedConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminInteractionItemMatcherService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceMetadataService;
import com.zcpu.tzzmod.webadmin.service.WebAdminChannelMetadataService;
import com.zcpu.tzzmod.webadmin.service.WebAdminVirtualBlockDeviceContainerTemplateSessionService;
import com.zcpu.tzzmod.webadmin.service.WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSelectionService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSignalListenerBasicConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSignalListenerLifecycleService;
import com.zcpu.tzzmod.webadmin.service.WebAdminUserSettingsService;
import com.zcpu.tzzmod.webadmin.service.WebAdminVirtualBlockDeviceLifecycleService;
import com.zcpu.tzzmod.webadmin.service.WebAdminVirtualBlockDeviceNativeTriggerService;
import com.zcpu.tzzmod.webadmin.testbridge.WebAdminTestBridgeRoutes;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteFoundationService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResultCode;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteTarget;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WebAdminServer {
    private final MinecraftServer minecraftServer;
    private final WebAdminConfig config;
    private final WebAdminUserService userService;
    private final WebAdminSessionService sessionService;
    private final WebAdminReadonlyRoutes readonlyRoutes = new WebAdminReadonlyRoutes();
    private final WebAdminUserSettingsService userSettingsService = new WebAdminUserSettingsService();
    private final WebAdminRealtimeService realtimeService = new WebAdminRealtimeService();
    private final WebAdminWriteSecurityService writeSecurityService = new WebAdminWriteSecurityService();
    private final WebAdminPermissionService permissionService = new WebAdminPermissionService();
    private final WebAdminWriteFoundationService writeFoundationService = new WebAdminWriteFoundationService(writeSecurityService);
    private final WebAdminEditLockService editLockService = new WebAdminEditLockService(permissionService, writeSecurityService);
    private final WebAdminDeviceMetadataService deviceMetadataService = new WebAdminDeviceMetadataService(permissionService, writeSecurityService, editLockService);
    private final WebAdminDeviceBasicConfigService deviceBasicConfigService = new WebAdminDeviceBasicConfigService(permissionService, writeSecurityService, editLockService);
    private final WebAdminDeviceExtendedConfigService deviceExtendedConfigService = new WebAdminDeviceExtendedConfigService(permissionService, writeSecurityService, editLockService);
    private final WebAdminActionRelayActionsService actionRelayActionsService = new WebAdminActionRelayActionsService(permissionService, writeSecurityService, editLockService);
    private final WebAdminInteractionItemMatcherService interactionItemMatcherService = new WebAdminInteractionItemMatcherService(permissionService, writeSecurityService, editLockService);
    private final WebAdminChannelMetadataService channelMetadataService = new WebAdminChannelMetadataService(permissionService, writeSecurityService, editLockService);
    private final WebAdminSelectionService selectionService = new WebAdminSelectionService(permissionService, writeSecurityService);
    private final WebAdminSignalListenerBasicConfigService signalListenerBasicConfigService = new WebAdminSignalListenerBasicConfigService(permissionService, writeSecurityService, editLockService);
    private final WebAdminVirtualBlockDeviceLifecycleService virtualBlockDeviceLifecycleService = new WebAdminVirtualBlockDeviceLifecycleService(permissionService, writeSecurityService);
    private final WebAdminVirtualBlockDeviceNativeTriggerService virtualBlockDeviceNativeTriggerService = new WebAdminVirtualBlockDeviceNativeTriggerService(permissionService, writeSecurityService, editLockService);
    private final WebAdminVirtualBlockDeviceContainerTemplateSessionService containerTemplateSessionService = new WebAdminVirtualBlockDeviceContainerTemplateSessionService(permissionService, writeSecurityService, editLockService);
    private final WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService singleItemSubmitTemplateSessionService = new WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService(permissionService, writeSecurityService, editLockService);
    private final WebAdminSignalListenerLifecycleService signalListenerLifecycleService = new WebAdminSignalListenerLifecycleService(permissionService, writeSecurityService);
    private final WebAdminTestBridgeRoutes testBridgeRoutes = new WebAdminTestBridgeRoutes();
    private HttpServer httpServer;
    private ExecutorService executor;

    public WebAdminServer(
            MinecraftServer minecraftServer,
            WebAdminConfig config,
            WebAdminUserService userService,
            WebAdminSessionService sessionService
    ) {
        this.minecraftServer = minecraftServer;
        this.config = config;
        this.userService = userService;
        this.sessionService = sessionService;
    }

    public synchronized void start() throws IOException {
        if (httpServer != null) {
            return;
        }
        httpServer = HttpServer.create(new InetSocketAddress(config.host, config.port), 0);
        httpServer.createContext("/", this::handle);
        executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "tzz-webadmin");
            thread.setDaemon(true);
            return thread;
        });
        httpServer.setExecutor(executor);
        httpServer.start();
        WebAdminAuditLogger.server("start", config);
        Tzz_mod.LOGGER.info("WebAdmin started at http://{}:{} mode={}", config.host, config.port, config.accessMode);
        if (config.accessModeEnum().needsSecurityWarning()) {
            Tzz_mod.LOGGER.warn("WebAdmin is configured for {}. Expose the port only to trusted collaborators.", config.accessMode);
        }
    }

    public synchronized void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        realtimeService.closeAll();
        editLockService.clear();
        writeSecurityService.clear();
        sessionService.clear();
        WebAdminAuditLogger.server("stop", config);
    }

    public boolean running() {
        return httpServer != null;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = normalizePath(exchange.getRequestURI().getPath());
            String method = exchange.getRequestMethod();
            if (path.equals("/") || path.equals("/login")) {
                sendText(exchange, 200, "text/html; charset=utf-8", WebAdminFrontendAssets.loginHtml());
                return;
            }
            if (path.equals("/app") || path.equals("/status")) {
                sendText(exchange, 200, "text/html; charset=utf-8", WebAdminFrontendAssets.appHtml());
                return;
            }
            if (path.equals("/assets/app.css")) {
                sendText(exchange, 200, "text/css; charset=utf-8", WebAdminFrontendAssets.appCss());
                return;
            }
            if (path.equals("/assets/app.js")) {
                sendText(exchange, 200, "application/javascript; charset=utf-8", WebAdminFrontendAssets.appJs());
                return;
            }
            if (path.equals("/api/auth/login") && method.equalsIgnoreCase("POST")) {
                handleLogin(exchange);
                return;
            }
            if (path.startsWith("/api/testbridge/")) {
                runOnServerThread(() -> testBridgeRoutes.handle(exchange, minecraftServer, path, method));
                return;
            }

            AuthContext auth = requireAuth(exchange);
            if (auth == null) {
                return;
            }
            if (path.equals("/api/auth/logout") && method.equalsIgnoreCase("POST")) {
                handleLogout(exchange, auth);
                return;
            }
            if (path.equals("/api/auth/me") && method.equalsIgnoreCase("GET")) {
                handleMe(exchange, auth);
                return;
            }
            if (path.equals("/api/status") && method.equalsIgnoreCase("GET")) {
                handleStatus(exchange, auth);
                return;
            }
            if (path.equals("/api/realtime/events")) {
                if (!method.equalsIgnoreCase("GET")) {
                    WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                    return;
                }
                realtimeService.handleEventStream(exchange, auth.user);
                return;
            }
            if (path.equals("/api/webadmin/users")) {
                if (!method.equalsIgnoreCase("GET")) {
                    WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                    return;
                }
                handleWebAdminUsers(exchange, auth);
                return;
            }
            if (path.equals("/api/webadmin/settings")) {
                if (!method.equalsIgnoreCase("GET")) {
                    WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                    return;
                }
                handleWebAdminSettings(exchange, auth);
                return;
            }
            if (path.equals("/api/webadmin/write/capabilities")) {
                if (!method.equalsIgnoreCase("GET")) {
                    WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                    return;
                }
                handleWebAdminWriteCapabilities(exchange, auth);
                return;
            }
            if (path.equals("/api/webadmin/users/me/password")) {
                handleWebAdminOwnPassword(exchange, auth, method);
                return;
            }
            if (path.startsWith("/api/webadmin/users/") && path.endsWith("/password-reset")) {
                handleWebAdminUserPasswordReset(exchange, auth, path, method);
                return;
            }
            if (path.equals("/api/webadmin/online-players")) {
                if (!method.equalsIgnoreCase("GET")) {
                    WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                    return;
                }
                runOnServerThread(() -> handleOnlinePlayers(exchange, auth));
                return;
            }
            if (path.startsWith("/api/webadmin/edit-locks/")) {
                runOnServerThread(() -> handleEditLocks(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/device-metadata/")) {
                runOnServerThread(() -> handleDeviceMetadata(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/device-basic-config/")) {
                runOnServerThread(() -> handleDeviceBasicConfig(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/device-extended-config/")) {
                runOnServerThread(() -> handleDeviceExtendedConfig(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/action-relay-actions/")) {
                runOnServerThread(() -> handleActionRelayActions(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/interaction-item-matcher/")) {
                runOnServerThread(() -> handleInteractionItemMatcher(exchange, auth, path, method));
                return;
            }
            if (path.equals("/api/webadmin/channel-metadata")) {
                handleChannelMetadata(exchange, auth, method);
                return;
            }
            if (path.startsWith("/api/webadmin/selection/")) {
                runOnServerThread(() -> handleSelection(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/virtual-block-devices/") && path.endsWith("/native-triggers")) {
                runOnServerThread(() -> handleVirtualBlockDeviceNativeTriggers(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/virtual-block-devices/") && path.contains("/container-template")) {
                runOnServerThread(() -> handleVirtualBlockDeviceContainerTemplate(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/virtual-block-devices/") && path.contains("/single-item-submit")) {
                runOnServerThread(() -> handleVirtualBlockDeviceSingleItemSubmitTemplate(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/virtual-block-devices/")) {
                runOnServerThread(() -> handleVirtualBlockDeviceLifecycle(exchange, auth, path, method));
                return;
            }
            if (path.equals("/api/webadmin/signal-listeners") || path.startsWith("/api/webadmin/signal-listeners/")) {
                handleSignalListenerLifecycle(exchange, auth, path, method);
                return;
            }
            if (path.startsWith("/api/webadmin/signal-listener-basic-config/")) {
                handleSignalListenerBasicConfig(exchange, auth, path, method);
                return;
            }
            final boolean[] readonlyHandled = new boolean[1];
            runOnServerThread(() -> readonlyHandled[0] = readonlyRoutes.handle(exchange, minecraftServer, path));
            if (readonlyHandled[0]) {
                return;
            }
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "接口不存在。");
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("WebAdmin request failed: {}", exception.getMessage());
            if (exchange.getResponseCode() < 0) {
                WebAdminJsonResponse.error(exchange, 500, "INTERNAL_ERROR", "WebAdmin 请求处理失败。");
            }
        }
    }

    private void runOnServerThread(ServerThreadAction action) throws IOException {
        CompletableFuture<Void> future = new CompletableFuture<>();
        minecraftServer.execute(() -> {
            try {
                action.run();
                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        try {
            future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for Minecraft server thread.", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IOException("Minecraft server thread task failed.", cause);
        }
    }

    @FunctionalInterface
    private interface ServerThreadAction {
        void run() throws Exception;
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        LoginRequest request = readJson(exchange, LoginRequest.class);
        if (request == null || isBlank(request.username) || request.password == null) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "请输入用户名和密码。");
            return;
        }
        WebAdminUserService.AuthResult result = userService.authenticate(request.username, request.password);
        if (!result.success() || result.user() == null) {
            WebAdminJsonResponse.error(exchange, 401, "UNAUTHORIZED", result.message());
            return;
        }
        int ttlSeconds = config.effectiveSessionTtlSeconds(request.rememberMe);
        WebAdminSessionService.CreatedSession created = sessionService.create(
                result.user(),
                ttlSeconds,
                sourceIp(exchange),
                header(exchange, "User-Agent")
        );
        exchange.getResponseHeaders().add("Set-Cookie", sessionCookie(created.token(), ttlSeconds));
        WebAdminJsonResponse.ok(exchange, userDto(result.user()));
    }

    private void handleLogout(HttpExchange exchange, AuthContext auth) throws IOException {
        sessionService.invalidate(auth.rawToken);
        exchange.getResponseHeaders().add("Set-Cookie", clearSessionCookie());
        WebAdminAuditLogger.logout(auth.session.username);
        WebAdminJsonResponse.ok(exchange, Map.of("loggedOut", true));
    }

    private void handleMe(HttpExchange exchange, AuthContext auth) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", auth.user.username);
        data.put("displayName", auth.user.displayName);
        data.put("role", auth.user.role);
        data.put("sessionExpiresAt", WebAdminSessionService.formatInstant(auth.session.expiresAt));
        data.put("accessMode", config.accessMode);
        WebAdminJsonResponse.ok(exchange, data);
    }

    private void handleStatus(HttpExchange exchange, AuthContext auth) throws IOException {
        Map<String, Object> webAdmin = new LinkedHashMap<>();
        webAdmin.put("enabled", config.enabled);
        webAdmin.put("running", running());
        webAdmin.put("host", config.host);
        webAdmin.put("port", config.port);
        webAdmin.put("accessMode", config.accessMode);
        webAdmin.put("sessionCount", sessionService.sessionCount());
        webAdmin.put("realtimeClientCount", WebAdminRealtimeEventBus.clientCount());
        WebAdminStoragePaths storagePaths = WebAdminStoragePaths.resolve(minecraftServer);
        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("scope", WebAdminStoragePaths.STORAGE_SCOPE);
        storage.put("directory", storagePaths.directory().toString());
        storage.put("configPath", storagePaths.configPath().toString());
        storage.put("usersPath", storagePaths.usersPath().toString());
        storage.put("auditLogPath", storagePaths.auditLogPath().toString());
        storage.put("legacyGlobalFilesDetected", storagePaths.hasLegacyGlobalFiles());
        webAdmin.put("storage", storage);

        Map<String, Object> server = new LinkedHashMap<>();
        server.put("type", minecraftServer.isDedicated() ? "DEDICATED" : "INTEGRATED");
        server.put("status", "RUNNING");
        server.put("minecraftVersion", "1.21.11");
        server.put("modVersion", modVersion());

        Map<String, Object> authData = new LinkedHashMap<>();
        authData.put("currentUser", auth.user.username);
        authData.put("role", auth.user.role);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("platformName", "游戏开发编辑平台");
        data.put("webAdmin", webAdmin);
        data.put("server", server);
        data.put("auth", authData);
        WebAdminJsonResponse.ok(exchange, data);
    }

    private void handleOnlinePlayers(HttpExchange exchange, AuthContext auth) throws IOException {
        WebAdminRole role = auth.user.roleEnum();
        if (role != WebAdminRole.EDITOR && role != WebAdminRole.OWNER) {
            WebAdminJsonResponse.error(exchange, 403, "FORBIDDEN", "权限不足：只有编辑者或所有者可以查看在线玩家候选。");
            return;
        }
        List<Map<String, Object>> players = minecraftServer.getPlayerManager().getPlayerList().stream()
                .map(WebAdminServer::onlinePlayerDto)
                .toList();
        WebAdminJsonResponse.ok(exchange, players);
    }

    private static Map<String, Object> onlinePlayerDto(ServerPlayerEntity player) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", player.getName().getString());
        data.put("uuid", player.getUuidAsString());
        return data;
    }

    private void handleWebAdminUsers(HttpExchange exchange, AuthContext auth) throws IOException {
        if (auth.user.roleEnum() != WebAdminRole.OWNER) {
            WebAdminJsonResponse.error(exchange, 403, "FORBIDDEN", "权限不足：只有所有者可以查看用户管理。");
            return;
        }
        WebAdminJsonResponse.ok(exchange, userSettingsService.users(userService, sessionService));
    }

    private void handleWebAdminSettings(HttpExchange exchange, AuthContext auth) throws IOException {
        WebAdminJsonResponse.ok(exchange, userSettingsService.settings(minecraftServer, config, sessionService, auth.user));
    }

    private void handleWebAdminWriteCapabilities(HttpExchange exchange, AuthContext auth) throws IOException {
        WebAdminJsonResponse.ok(exchange, writeFoundationService.capabilities(auth.user, auth.session));
    }

    private void handleWebAdminOwnPassword(HttpExchange exchange, AuthContext auth, String method) throws IOException {
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        PasswordChangeRequest request = readJson(exchange, PasswordChangeRequest.class);
        if (request == null) {
            request = new PasswordChangeRequest();
        }
        WebAdminWriteResult security = requirePasswordWriteSecurity(exchange, auth);
        if (!security.success()) {
            WebAdminJsonResponse.ok(exchange, security);
            return;
        }
        WebAdminWriteTarget target = userTarget(auth.user.username);
        if (!safe(request.newPassword).equals(safe(request.confirmPassword))) {
            WebAdminJsonResponse.ok(exchange, WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.VALIDATION_FAILED,
                    target,
                    "两次输入的新密码不一致。"
            ));
            return;
        }
        WebAdminUserService.PasswordUpdateResult update = userService.changeOwnPassword(
                auth.user.username,
                request.oldPassword,
                request.newPassword
        );
        if (!update.success()) {
            WebAdminJsonResponse.ok(exchange, WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.VALIDATION_FAILED,
                    target,
                    update.message()
            ));
            return;
        }
        int invalidated = update.changed() ? sessionService.invalidateUsername(auth.user.username, auth.session.sessionIdHash) : 0;
        if (update.changed()) {
            publishUserPasswordRealtime("password_changed", auth.user.username, auth.user.username);
        }
        WebAdminJsonResponse.ok(exchange, passwordWriteResult(target, update.changed(), update.message(), invalidated));
    }

    private void handleWebAdminUserPasswordReset(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        String prefix = "/api/webadmin/users/";
        String suffix = "/password-reset";
        String username = decodePathSegment(path.substring(prefix.length(), path.length() - suffix.length()));
        PasswordResetRequest request = readJson(exchange, PasswordResetRequest.class);
        if (request == null) {
            request = new PasswordResetRequest();
        }
        WebAdminWriteResult security = requirePasswordWriteSecurity(exchange, auth);
        if (!security.success()) {
            WebAdminJsonResponse.ok(exchange, security);
            return;
        }
        if (auth.user.roleEnum() != WebAdminRole.OWNER) {
            WebAdminJsonResponse.ok(exchange, WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.PERMISSION_DENIED,
                    userTarget(username),
                    "权限不足：只有所有者可以重置 WebAdmin 用户密码。"
            ));
            return;
        }
        if (!safe(request.newPassword).equals(safe(request.confirmPassword))) {
            WebAdminJsonResponse.ok(exchange, WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.VALIDATION_FAILED,
                    userTarget(username),
                    "两次输入的新密码不一致。"
            ));
            return;
        }
        WebAdminUserService.PasswordUpdateResult update = userService.setPassword(username, request.newPassword, auth.user.username);
        if (!update.success() || update.user() == null) {
            WebAdminWriteResultCode code = update.user() == null ? WebAdminWriteResultCode.TARGET_NOT_FOUND : WebAdminWriteResultCode.VALIDATION_FAILED;
            WebAdminJsonResponse.ok(exchange, WebAdminWriteResult.failed(code, userTarget(username), update.message()));
            return;
        }
        int invalidated = update.changed() ? sessionService.invalidateUsername(update.user().username, "") : 0;
        if (update.changed()) {
            publishUserPasswordRealtime("password_reset", update.user().username, auth.user.username);
        }
        WebAdminJsonResponse.ok(exchange, passwordWriteResult(
                userTarget(update.user().username),
                update.changed(),
                update.message(),
                invalidated
        ));
    }

    private void handleEditLocks(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        if (path.equals("/api/webadmin/edit-locks/status")) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, String> query = queryParams(exchange);
            String targetType = query.getOrDefault("targetType", "");
            String targetId = canonicalizeEditLockTargetId(targetType, query.getOrDefault("targetId", ""));
            WebAdminJsonResponse.ok(exchange, editLockService.status(
                    targetType,
                    targetId,
                    auth.user,
                    auth.session
            ));
            return;
        }

        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        WebAdminEditLockRequest request = readJson(exchange, WebAdminEditLockRequest.class);
        if (request == null) {
            request = new WebAdminEditLockRequest();
        }
        request.targetId = canonicalizeEditLockTargetId(request.targetType, request.targetId);
        String csrfToken = header(exchange, "X-TZZ-WebAdmin-CSRF");
        boolean sameOrigin = isWriteSameOrigin(exchange);
        WebAdminWriteResult result;
        if (path.equals("/api/webadmin/edit-locks/acquire")) {
            result = editLockService.acquire(auth.user, auth.session, sourceIp(exchange), request, csrfToken, sameOrigin);
        } else if (path.equals("/api/webadmin/edit-locks/heartbeat")) {
            result = editLockService.heartbeat(auth.user, auth.session, sourceIp(exchange), request, csrfToken, sameOrigin);
        } else if (path.equals("/api/webadmin/edit-locks/release")) {
            result = editLockService.release(auth.user, auth.session, sourceIp(exchange), request, csrfToken, sameOrigin);
        } else {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "编辑锁接口不存在。");
            return;
        }
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleDeviceMetadata(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/device-metadata/";
        String deviceId = decodePathSegment(path.substring(prefix.length()));
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "设备 ID 不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            var device = new com.zcpu.tzzmod.webadmin.service.WebAdminDeviceService().findDevice(minecraftServer, deviceId);
            if (device == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "设备不存在或已被删除。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, deviceMetadataService.metadataFor(minecraftServer, device));
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET 或 PATCH。");
            return;
        }
        WebAdminDeviceMetadataUpdateRequest request = readJson(exchange, WebAdminDeviceMetadataUpdateRequest.class);
        if (request == null) {
            request = new WebAdminDeviceMetadataUpdateRequest();
        }
        request.deviceId = deviceId;
        String csrfToken = header(exchange, "X-TZZ-WebAdmin-CSRF");
        boolean sameOrigin = isWriteSameOrigin(exchange);
        WebAdminWriteResult result = deviceMetadataService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                request,
                csrfToken,
                sameOrigin
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleDeviceBasicConfig(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/device-basic-config/";
        String deviceId = decodePathSegment(path.substring(prefix.length()));
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "设备 ID 不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            var config = deviceBasicConfigService.configFor(minecraftServer, auth.user, auth.session, deviceId);
            if (config == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "设备不存在或已被删除。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, config);
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET 或 PATCH。");
            return;
        }
        WebAdminDeviceBasicConfigUpdateRequest request = readJson(exchange, WebAdminDeviceBasicConfigUpdateRequest.class);
        if (request == null) {
            request = new WebAdminDeviceBasicConfigUpdateRequest();
        }
        request.deviceId = deviceId;
        String csrfToken = header(exchange, "X-TZZ-WebAdmin-CSRF");
        boolean sameOrigin = isWriteSameOrigin(exchange);
        WebAdminWriteResult result = deviceBasicConfigService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                request,
                csrfToken,
                sameOrigin
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleDeviceExtendedConfig(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/device-extended-config/";
        String deviceId = decodePathSegment(path.substring(prefix.length()));
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "设备 ID 不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            var config = deviceExtendedConfigService.configFor(minecraftServer, auth.user, auth.session, deviceId);
            if (config == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "设备不存在或已被删除。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, config);
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET 或 PATCH。");
            return;
        }
        WebAdminDeviceExtendedConfigUpdateRequest request = readJson(exchange, WebAdminDeviceExtendedConfigUpdateRequest.class);
        if (request == null) {
            request = new WebAdminDeviceExtendedConfigUpdateRequest();
        }
        request.deviceId = deviceId;
        String csrfToken = header(exchange, "X-TZZ-WebAdmin-CSRF");
        boolean sameOrigin = isWriteSameOrigin(exchange);
        WebAdminWriteResult result = deviceExtendedConfigService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                request,
                csrfToken,
                sameOrigin
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private String canonicalizeEditLockTargetId(String targetType, String targetId) {
        String safeTargetId = targetId == null ? "" : targetId.trim();
        if (safeTargetId.isBlank() || !isDeviceEditLockTarget(targetType)) {
            return safeTargetId;
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(minecraftServer, safeTargetId);
        if (resolved.foundUnique()) {
            SignalDeviceData device = resolved.device();
            return device == null ? safeTargetId : device.normalized().id();
        }
        return safeTargetId;
    }

    private static boolean isDeviceEditLockTarget(String targetType) {
        String safeTargetType = targetType == null ? "" : targetType.trim().toLowerCase(Locale.ROOT);
        return WebAdminEditLockService.TARGET_DEVICE_METADATA.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_DEVICE_EXTENDED_CONFIG.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_INTERACTION_ITEM_MATCHER.equals(safeTargetType);
    }

    private void handleActionRelayActions(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/action-relay-actions/";
        String deviceId = decodePathSegment(path.substring(prefix.length()));
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "Action Relay 设备 ID 不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            Map<String, Object> actions = actionRelayActionsService.actionsFor(minecraftServer, auth.user, auth.session, deviceId);
            if (actions == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "Action Relay 设备不存在或引用不唯一。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, actions);
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET 或 PATCH。");
            return;
        }
        WebAdminActionRelayActionsUpdateRequest request = readJson(exchange, WebAdminActionRelayActionsUpdateRequest.class);
        if (request == null) {
            request = new WebAdminActionRelayActionsUpdateRequest();
        }
        request.deviceId = deviceId;
        WebAdminWriteResult result = actionRelayActionsService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                deviceId,
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleInteractionItemMatcher(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/interaction-item-matcher/";
        String deviceId = decodePathSegment(path.substring(prefix.length()));
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "virtual_block_device 设备 ID 不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            Map<String, Object> config = interactionItemMatcherService.configFor(minecraftServer, auth.user, auth.session, deviceId);
            if (config == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "virtual_block_device 不存在或引用不唯一。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, config);
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET 或 PATCH。");
            return;
        }
        WebAdminInteractionItemMatcherUpdateRequest request = readJson(exchange, WebAdminInteractionItemMatcherUpdateRequest.class);
        if (request == null) {
            request = new WebAdminInteractionItemMatcherUpdateRequest();
        }
        request.deviceId = deviceId;
        WebAdminWriteResult result = interactionItemMatcherService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                deviceId,
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleChannelMetadata(HttpExchange exchange, AuthContext auth, String method) throws IOException {
        Map<String, String> query = queryParams(exchange);
        String channel = query.getOrDefault("channel", "");
        if (channel.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "频道不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            WebAdminJsonResponse.ok(exchange, channelMetadataService.metadataFor(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    channel,
                    "signal"
            ));
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / PATCH。");
            return;
        }
        WebAdminChannelMetadataUpdateRequest request = readJson(exchange, WebAdminChannelMetadataUpdateRequest.class);
        if (request == null) {
            request = new WebAdminChannelMetadataUpdateRequest();
        }
        request.channel = channel;
        WebAdminWriteResult result = channelMetadataService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleSelection(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        if (path.equals("/api/webadmin/selection/status")) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            if (auth.user.roleEnum() != WebAdminRole.EDITOR && auth.user.roleEnum() != WebAdminRole.OWNER) {
                WebAdminJsonResponse.error(exchange, 403, "FORBIDDEN", "权限不足：只有编辑者或所有者可以查看选择状态。");
                return;
            }
            Map<String, String> query = queryParams(exchange);
            WebAdminJsonResponse.ok(exchange, selectionService.status(query.getOrDefault("selectionId", "")));
            return;
        }
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        if (path.equals("/api/webadmin/selection/start")) {
            WebAdminSelectionStartRequest request = readJson(exchange, WebAdminSelectionStartRequest.class);
            if (request == null) {
                request = new WebAdminSelectionStartRequest();
            }
            WebAdminWriteResult result = selectionService.start(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        if (path.equals("/api/webadmin/selection/cancel")) {
            WebAdminSelectionCancelRequest request = readJson(exchange, WebAdminSelectionCancelRequest.class);
            if (request == null) {
                request = new WebAdminSelectionCancelRequest();
            }
            WebAdminWriteResult result = selectionService.cancel(
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "选择接口不存在。");
    }

    private void handleVirtualBlockDeviceLifecycle(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/virtual-block-devices/";
        String suffix = "/delete";
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备生命周期接口不存在。");
            return;
        }
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        String encodedDeviceId = path.substring(prefix.length(), path.length() - suffix.length());
        String deviceId = decodePathSegment(encodedDeviceId);
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "虚拟方块设备 ID 不能为空。");
            return;
        }
        WebAdminVirtualBlockDeviceDeleteRequest request = readJson(exchange, WebAdminVirtualBlockDeviceDeleteRequest.class);
        if (request == null) {
            request = new WebAdminVirtualBlockDeviceDeleteRequest();
        }
        request.deviceId = deviceId;
        WebAdminWriteResult result = virtualBlockDeviceLifecycleService.delete(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                deviceId,
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleVirtualBlockDeviceNativeTriggers(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/virtual-block-devices/";
        String suffix = "/native-triggers";
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备原生触发配置接口不存在。");
            return;
        }
        if (!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / PATCH。");
            return;
        }
        String encodedDeviceId = path.substring(prefix.length(), path.length() - suffix.length());
        String deviceId = decodePathSegment(encodedDeviceId);
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "虚拟方块设备 ID 不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("PATCH")) {
            WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest request = readJson(exchange, WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest.class);
            if (request == null) {
                request = new WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest();
            }
            request.deviceId = deviceId;
            WebAdminWriteResult result = virtualBlockDeviceNativeTriggerService.update(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    deviceId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        Map<String, Object> data = virtualBlockDeviceNativeTriggerService.overview(minecraftServer, auth.user, auth.session, deviceId);
        if (data == null) {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备不存在。");
            return;
        }
        if (Boolean.FALSE.equals(data.get("supported"))) {
            WebAdminJsonResponse.error(exchange, 400, "VALIDATION_ERROR", String.valueOf(data.getOrDefault("unsupportedReason", "该接口仅支持 virtual_block_device。")));
            return;
        }
        WebAdminJsonResponse.ok(exchange, data);
    }

    private void handleVirtualBlockDeviceContainerTemplate(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/virtual-block-devices/";
        String overviewSuffix = "/container-template";
        String startSuffix = "/container-template-session/start";
        String statusSuffix = "/container-template-session/status";
        String cancelSuffix = "/container-template-session/cancel";
        String suffix;
        if (path.startsWith(prefix) && path.endsWith(overviewSuffix)) {
            suffix = overviewSuffix;
        } else if (path.startsWith(prefix) && path.endsWith(startSuffix)) {
            suffix = startSuffix;
        } else if (path.startsWith(prefix) && path.endsWith(statusSuffix)) {
            suffix = statusSuffix;
        } else if (path.startsWith(prefix) && path.endsWith(cancelSuffix)) {
            suffix = cancelSuffix;
        } else {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备容器模板会话接口不存在。");
            return;
        }
        String encodedDeviceId = path.substring(prefix.length(), path.length() - suffix.length());
        String deviceId = decodePathSegment(encodedDeviceId);
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "虚拟方块设备 ID 不能为空。");
            return;
        }

        if (overviewSuffix.equals(suffix)) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, Object> data = containerTemplateSessionService.overview(minecraftServer, auth.user, auth.session, deviceId);
            if (data == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备不存在。");
                return;
            }
            if (Boolean.FALSE.equals(data.get("supported"))) {
                WebAdminJsonResponse.error(exchange, 400, "VALIDATION_ERROR", String.valueOf(data.getOrDefault("unsupportedReason", "该接口仅支持 virtual_block_device。")));
                return;
            }
            WebAdminJsonResponse.ok(exchange, data);
            return;
        }

        if (statusSuffix.equals(suffix)) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, String> query = queryParams(exchange);
            WebAdminJsonResponse.ok(exchange, containerTemplateSessionService.status(query.getOrDefault("sessionId", "")));
            return;
        }

        if (startSuffix.equals(suffix)) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminContainerTemplateSessionStartRequest request = readJson(exchange, WebAdminContainerTemplateSessionStartRequest.class);
            if (request == null) {
                request = new WebAdminContainerTemplateSessionStartRequest();
            }
            request.deviceId = deviceId;
            WebAdminWriteResult result = containerTemplateSessionService.start(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    deviceId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        WebAdminContainerTemplateSessionCancelRequest request = readJson(exchange, WebAdminContainerTemplateSessionCancelRequest.class);
        if (request == null) {
            request = new WebAdminContainerTemplateSessionCancelRequest();
        }
        request.deviceId = deviceId;
        WebAdminWriteResult result = containerTemplateSessionService.cancel(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                deviceId,
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleVirtualBlockDeviceSingleItemSubmitTemplate(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/virtual-block-devices/";
        String overviewSuffix = "/single-item-submit";
        String startSuffix = "/single-item-submit-session/start";
        String statusSuffix = "/single-item-submit-session/status";
        String cancelSuffix = "/single-item-submit-session/cancel";
        String suffix;
        if (path.startsWith(prefix) && path.endsWith(overviewSuffix)) {
            suffix = overviewSuffix;
        } else if (path.startsWith(prefix) && path.endsWith(startSuffix)) {
            suffix = startSuffix;
        } else if (path.startsWith(prefix) && path.endsWith(statusSuffix)) {
            suffix = statusSuffix;
        } else if (path.startsWith(prefix) && path.endsWith(cancelSuffix)) {
            suffix = cancelSuffix;
        } else {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备单物品提交模板会话接口不存在。");
            return;
        }
        String encodedDeviceId = path.substring(prefix.length(), path.length() - suffix.length());
        String deviceId = decodePathSegment(encodedDeviceId);
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "虚拟方块设备 ID 不能为空。");
            return;
        }
        if (overviewSuffix.equals(suffix)) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, Object> data = singleItemSubmitTemplateSessionService.overview(minecraftServer, auth.user, auth.session, deviceId);
            if (data == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备不存在。");
                return;
            }
            if (Boolean.FALSE.equals(data.get("supported"))) {
                WebAdminJsonResponse.error(exchange, 400, "VALIDATION_ERROR", String.valueOf(data.getOrDefault("unsupportedReason", "该接口仅支持 virtual_block_device。")));
                return;
            }
            WebAdminJsonResponse.ok(exchange, data);
            return;
        }
        if (statusSuffix.equals(suffix)) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, String> query = queryParams(exchange);
            WebAdminJsonResponse.ok(exchange, singleItemSubmitTemplateSessionService.status(query.getOrDefault("sessionId", "")));
            return;
        }
        if (startSuffix.equals(suffix)) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminSingleItemSubmitTemplateSessionStartRequest request = readJson(exchange, WebAdminSingleItemSubmitTemplateSessionStartRequest.class);
            if (request == null) {
                request = new WebAdminSingleItemSubmitTemplateSessionStartRequest();
            }
            request.deviceId = deviceId;
            WebAdminWriteResult result = singleItemSubmitTemplateSessionService.start(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    deviceId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        WebAdminSingleItemSubmitTemplateSessionCancelRequest request = readJson(exchange, WebAdminSingleItemSubmitTemplateSessionCancelRequest.class);
        if (request == null) {
            request = new WebAdminSingleItemSubmitTemplateSessionCancelRequest();
        }
        request.deviceId = deviceId;
        WebAdminWriteResult result = singleItemSubmitTemplateSessionService.cancel(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                deviceId,
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleSignalListenerLifecycle(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        if (path.equals("/api/webadmin/signal-listeners")) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminSignalListenerCreateRequest request = readJson(exchange, WebAdminSignalListenerCreateRequest.class);
            if (request == null) {
                request = new WebAdminSignalListenerCreateRequest();
            }
            WebAdminWriteResult result = signalListenerLifecycleService.create(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        String prefix = "/api/webadmin/signal-listeners/";
        String suffix = "/delete";
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "Signal Listener 生命周期接口不存在。");
            return;
        }
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        String encodedListenerId = path.substring(prefix.length(), path.length() - suffix.length());
        String listenerId = decodePathSegment(encodedListenerId);
        if (listenerId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "Signal Listener ID 不能为空。");
            return;
        }
        WebAdminSignalListenerDeleteRequest request = readJson(exchange, WebAdminSignalListenerDeleteRequest.class);
        if (request == null) {
            request = new WebAdminSignalListenerDeleteRequest();
        }
        request.listenerId = listenerId;
        WebAdminWriteResult result = signalListenerLifecycleService.delete(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                listenerId,
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleSignalListenerBasicConfig(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/signal-listener-basic-config/";
        String listenerRef = decodePathSegment(path.substring(prefix.length()));
        if (listenerRef.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "Listener 引用不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            var config = signalListenerBasicConfigService.configFor(minecraftServer, auth.user, auth.session, listenerRef);
            if (config == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "Signal Listener 不存在或引用不唯一。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, config);
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / PATCH。");
            return;
        }
        WebAdminSignalListenerBasicConfigUpdateRequest request = readJson(exchange, WebAdminSignalListenerBasicConfigUpdateRequest.class);
        if (request == null) {
            request = new WebAdminSignalListenerBasicConfigUpdateRequest();
        }
        request.listenerRef = listenerRef;
        WebAdminWriteResult result = signalListenerBasicConfigService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private AuthContext requireAuth(HttpExchange exchange) throws IOException {
        String token = cookie(exchange, WebAdminSessionService.COOKIE_NAME);
        WebAdminSession session = sessionService.get(token).orElse(null);
        if (session == null) {
            WebAdminJsonResponse.error(exchange, 401, "UNAUTHORIZED", "请先登录。");
            return null;
        }
        WebAdminUser user = userService.find(session.username).orElse(null);
        if (user == null || !user.enabled) {
            sessionService.invalidate(token);
            WebAdminJsonResponse.error(exchange, 401, "UNAUTHORIZED", "请先登录。");
            return null;
        }
        return new AuthContext(token, session, user);
    }

    private <T> T readJson(HttpExchange exchange, Class<T> type) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        if (bytes.length == 0) {
            return null;
        }
        return WebAdminJsonResponse.GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), type);
    }

    private static void sendText(HttpExchange exchange, int status, String contentType, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private String sessionCookie(String token, int ttlSeconds) {
        String cookie = WebAdminSessionService.COOKIE_NAME + "=" + token
                + "; Max-Age=" + ttlSeconds
                + "; Path=/; HttpOnly; SameSite=Lax";
        return config.secureCookie ? cookie + "; Secure" : cookie;
    }

    private String clearSessionCookie() {
        String cookie = WebAdminSessionService.COOKIE_NAME + "=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax";
        return config.secureCookie ? cookie + "; Secure" : cookie;
    }

    private static Map<String, Object> userDto(WebAdminUser user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", user.username);
        data.put("displayName", user.displayName);
        data.put("role", user.role);
        return data;
    }

    private static String cookie(HttpExchange exchange, String name) {
        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");
        if (cookieHeaders == null) {
            return "";
        }
        for (String header : cookieHeaders) {
            String[] entries = header.split(";");
            for (String entry : entries) {
                String[] parts = entry.trim().split("=", 2);
                if (parts.length == 2 && parts[0].equals(name)) {
                    return parts[1];
                }
            }
        }
        return "";
    }

    private static String header(HttpExchange exchange, String name) {
        List<String> values = exchange.getRequestHeaders().get(name);
        return values == null || values.isEmpty() ? "" : values.get(0);
    }

    private boolean isWriteSameOrigin(HttpExchange exchange) {
        String origin = header(exchange, "Origin");
        String referer = header(exchange, "Referer");
        if (isBlank(origin) && isBlank(referer)) {
            return true;
        }
        HostPort hostPort = requestHostPort(exchange);
        return writeSecurityService.isSameOriginOrReferer(origin, referer, hostPort.host(), hostPort.port());
    }

    private HostPort requestHostPort(HttpExchange exchange) {
        String hostHeader = header(exchange, "Host");
        if (!isBlank(hostHeader)) {
            String trimmed = hostHeader.trim();
            int colon = trimmed.lastIndexOf(':');
            if (colon > 0 && colon < trimmed.length() - 1) {
                try {
                    return new HostPort(trimmed.substring(0, colon), Integer.parseInt(trimmed.substring(colon + 1)));
                } catch (NumberFormatException ignored) {
                    return new HostPort(trimmed.substring(0, colon), config.port);
                }
            }
            return new HostPort(trimmed, config.port);
        }
        return new HostPort(config.host, config.port);
    }

    private static WebAdminWriteTarget userTarget(String username) {
        String safeUsername = safe(username);
        return new WebAdminWriteTarget("webadmin_user", safeUsername, safeUsername);
    }

    private static WebAdminWriteResult passwordWriteResult(WebAdminWriteTarget target, boolean changed, String message, int invalidatedSessions) {
        return new WebAdminWriteResult(
                true,
                changed ? WebAdminWriteResultCode.OK.id() : WebAdminWriteResultCode.NO_CHANGE.id(),
                isBlank(message) ? (changed ? "密码已更新。" : "密码未变化。") : message,
                target.targetType(),
                target.targetId(),
                changed,
                List.of(),
                "",
                "",
                false,
                Map.of(),
                Map.of("invalidatedSessions", Math.max(0, invalidatedSessions))
        );
    }

    private static void publishUserPasswordRealtime(String action, String username, String actor) {
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.USER_CHANGED)
                .severity("INFO")
                .summary("WebAdmin 用户密码已更新")
                .routeTarget("#/users")
                .payload("action", action)
                .payload("username", safe(username))
                .payload("actor", safe(actor)));
    }

    private WebAdminWriteResult requirePasswordWriteSecurity(HttpExchange exchange, AuthContext auth) {
        if (!isWriteSameOrigin(exchange)) {
            return WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.CSRF_INVALID,
                    WebAdminWriteTarget.none(),
                    "写操作必须来自同源 WebAdmin 页面。"
            );
        }
        WebAdminWriteResult csrf = writeSecurityService.requireValidCsrf(auth.session, header(exchange, "X-TZZ-WebAdmin-CSRF"));
        if (!csrf.success()) {
            return csrf;
        }
        return WebAdminWriteResult.ok(WebAdminWriteTarget.none(), false, "密码写入安全校验通过。");
    }

    private static String decodePathSegment(String value) {
        return URLDecoder.decode((value == null ? "" : value).replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String part : query.split("&")) {
            if (part.isBlank()) {
                continue;
            }
            String[] pieces = part.split("=", 2);
            String key = URLDecoder.decode(pieces[0], StandardCharsets.UTF_8);
            String value = pieces.length > 1 ? URLDecoder.decode(pieces[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    private static String sourceIp(HttpExchange exchange) {
        return exchange.getRemoteAddress() == null || exchange.getRemoteAddress().getAddress() == null
                ? ""
                : exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private static String normalizePath(String path) {
        return path == null || path.isBlank() ? "/" : path;
    }

    private static String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(Tzz_mod.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class LoginRequest {
        String username;
        String password;
        boolean rememberMe;
    }

    private static final class PasswordChangeRequest {
        String oldPassword;
        String newPassword;
        String confirmPassword;
    }

    private static final class PasswordResetRequest {
        String newPassword;
        String confirmPassword;
    }

    private record AuthContext(String rawToken, WebAdminSession session, WebAdminUser user) {
    }

    private record HostPort(String host, int port) {
    }
}
