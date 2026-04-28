package com.zcpu.tzzmod.webadmin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceBasicConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceExtendedConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceMetadataUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.route.WebAdminReadonlyRoutes;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceBasicConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceExtendedConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceMetadataService;
import com.zcpu.tzzmod.webadmin.service.WebAdminUserSettingsService;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteFoundationService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

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
            if (path.startsWith("/api/webadmin/edit-locks/")) {
                handleEditLocks(exchange, auth, path, method);
                return;
            }
            if (path.startsWith("/api/webadmin/device-metadata/")) {
                handleDeviceMetadata(exchange, auth, path, method);
                return;
            }
            if (path.startsWith("/api/webadmin/device-basic-config/")) {
                handleDeviceBasicConfig(exchange, auth, path, method);
                return;
            }
            if (path.startsWith("/api/webadmin/device-extended-config/")) {
                handleDeviceExtendedConfig(exchange, auth, path, method);
                return;
            }
            if (readonlyRoutes.handle(exchange, minecraftServer, path)) {
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

    private void handleEditLocks(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        if (path.equals("/api/webadmin/edit-locks/status")) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, String> query = queryParams(exchange);
            WebAdminJsonResponse.ok(exchange, editLockService.status(
                    query.getOrDefault("targetType", ""),
                    query.getOrDefault("targetId", ""),
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

    private static String decodePathSegment(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
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

    private static final class LoginRequest {
        String username;
        String password;
        boolean rememberMe;
    }

    private record AuthContext(String rawToken, WebAdminSession session, WebAdminUser user) {
    }

    private record HostPort(String host, int port) {
    }
}
