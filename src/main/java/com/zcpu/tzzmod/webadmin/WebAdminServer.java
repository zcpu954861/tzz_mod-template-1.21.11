package com.zcpu.tzzmod.webadmin;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.webadmin.route.WebAdminReadonlyRoutes;
import com.zcpu.tzzmod.webadmin.service.WebAdminUserSettingsService;
import java.io.IOException;
import java.net.InetSocketAddress;
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
                sendText(exchange, 200, "text/html; charset=utf-8", loginHtml());
                return;
            }
            if (path.equals("/app") || path.equals("/status")) {
                sendText(exchange, 200, "text/html; charset=utf-8", appHtml());
                return;
            }
            if (path.equals("/assets/app.css")) {
                sendText(exchange, 200, "text/css; charset=utf-8", appCss());
                return;
            }
            if (path.equals("/assets/app.js")) {
                sendText(exchange, 200, "application/javascript; charset=utf-8", appJs());
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

    private static String loginHtml() {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>游戏开发编辑平台 - 登录</title>
                  <link rel="stylesheet" href="/assets/app.css">
                </head>
                <body data-page="login">
                  <main class="login-shell">
                    <section class="brand-panel">
                      <div class="topline"><span class="logo-mark">T</span><span>游戏开发编辑平台</span></div>
                      <div class="hero-copy">
                        <h1>游戏开发编辑平台</h1>
                        <p class="lead">高效管理游戏事件与逻辑</p>
                        <p>信号、设备、区域、动作、任务一体化管理</p>
                        <p class="tags">多人协作 · 实时同步 · 安全稳定 · 高效开发</p>
                      </div>
                    </section>
                    <section class="login-card">
                      <div class="server-pill">服务器状态：运行中</div>
                      <h2>用户登录</h2>
                      <form id="login-form">
                        <label>用户名<input id="username" autocomplete="username" required></label>
                        <label>密码<div class="password-row"><input id="password" type="password" autocomplete="current-password" required><button type="button" id="toggle-password">显示</button></div></label>
                        <label class="check-row"><input id="remember" type="checkbox"> 记住我（2 小时内自动登录）</label>
                        <button class="primary" type="submit">登录</button>
                        <p class="message" id="message"></p>
                        <div class="divider"><span>或</span></div>
                        <button class="secondary" type="button" disabled>使用一次性登录码登录</button>
                        <p class="help">忘记密码？请联系服务器管理员</p>
                        <p class="help">需要帮助？请联系服务器管理员</p>
                      </form>
                    </section>
                  </main>
                  <script src="/assets/app.js"></script>
                </body>
                </html>
                """;
    }

    private static String appHtml() {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>游戏开发编辑平台 - WebAdmin</title>
                  <link rel="stylesheet" href="/assets/app.css">
                </head>
                <body data-page="app">
                  <main class="admin-shell">
                    <aside class="sidebar">
                      <div class="sidebar-brand"><span class="logo-mark">T</span><span>游戏开发编辑平台</span></div>
                      <nav class="nav-list" aria-label="主导航">
                        <button class="nav-item" data-route="#/dashboard"><span class="nav-icon" data-icon="dashboard"></span>总览</button>
                        <button class="nav-item" data-route="#/devices"><span class="nav-icon" data-icon="device"></span>设备管理</button>
                        <button class="nav-item" data-route="#/signals"><span class="nav-icon" data-icon="signal"></span>Signal 管理</button>
                        <button class="nav-item" data-pending="区域管理将在后续版本接入"><span class="nav-icon" data-icon="region"></span>区域管理</button>
                        <button class="nav-item" data-pending="动作系统将在后续版本接入"><span class="nav-icon" data-icon="action"></span>动作系统</button>
                        <button class="nav-item" data-route="#/doctor"><span class="nav-icon" data-icon="doctor"></span>Doctor 诊断</button>
                        <button class="nav-item" data-route="#/history"><span class="nav-icon" data-icon="history"></span>历史记录</button>
                        <button class="nav-item" data-route="#/users"><span class="nav-icon" data-icon="user"></span>用户管理</button>
                        <button class="nav-item" data-route="#/settings"><span class="nav-icon" data-icon="settings"></span>系统设置</button>
                      </nav>
                    </aside>
                    <section class="workspace">
                      <header class="topbar">
                        <div class="topbar-status">
                          <span id="server-state">服务器状态：加载中</span>
                          <span id="access-mode">访问模式：-</span>
                        </div>
                        <div class="topbar-user">
                          <span id="current-user">用户：-</span>
                          <span id="current-role">角色：-</span>
                          <button id="logout" class="secondary">退出登录</button>
                        </div>
                      </header>
                      <div id="toast" class="toast" hidden></div>
                      <section id="app-view" class="view-panel" aria-live="polite">
                        <div class="loading-state">正在加载 WebAdmin...</div>
                      </section>
                    </section>
                  </main>
                  <script src="/assets/app.js"></script>
                </body>
                </html>
                """;
    }

    private static String appCss() {
        return """
                :root{color-scheme:dark;--bg:#07111f;--panel:#0d1b2e;--panel2:#101f35;--panel3:#0b1728;--text:#e7f7ff;--muted:#93a8b8;--cyan:#22d3ee;--cyan2:#0ea5e9;--line:#1e3a52;--danger:#fb7185;--warning:#facc15;--ok:#34d399}
                *{box-sizing:border-box}body{margin:0;min-height:100vh;font-family:Inter,Segoe UI,Arial,sans-serif;background:#07111f;color:var(--text);letter-spacing:0}
                .login-shell{min-height:100vh;width:min(100%,1360px);margin:0 auto;display:grid;grid-template-columns:minmax(0,1fr) 440px;gap:64px;align-items:center;justify-content:center;padding:48px clamp(24px,5vw,56px)}
                .brand-panel{min-height:420px;display:flex;flex-direction:column;justify-content:center;gap:72px}.topline,.app-header>div{display:flex;align-items:center;gap:12px;color:#c8f7ff;font-weight:700}.logo-mark{display:inline-grid;place-items:center;width:34px;height:34px;border-radius:8px;background:linear-gradient(135deg,var(--cyan),var(--cyan2));color:#04111d;font-weight:900}
                .hero-copy{max-width:760px}.hero-copy h1{font-size:64px;line-height:1.05;margin:0 0 22px}.hero-copy p{font-size:20px;color:var(--muted);margin:12px 0}.hero-copy .lead{font-size:28px;color:#fff}.tags{color:#9bf3ff!important}
                .login-card,.status-card{background:linear-gradient(180deg,var(--panel),var(--panel2));border:1px solid var(--line);border-radius:16px;box-shadow:0 24px 80px rgba(0,0,0,.35)}.login-card{padding:32px;max-width:440px;width:100%;justify-self:end}.server-pill{display:inline-flex;padding:7px 12px;border:1px solid #1f6d86;border-radius:999px;color:#9bf3ff;background:#092638;font-size:13px}.login-card h2,.status-card h2{margin:20px 0 22px;font-size:26px}
                label{display:block;margin:16px 0 8px;color:#cfe6f4;font-size:14px}input{width:100%;height:44px;border-radius:10px;border:1px solid #23445f;background:#081725;color:var(--text);padding:0 12px;font-size:15px}input:focus{outline:2px solid #1fbce2;border-color:transparent}.password-row{display:flex;gap:8px}.password-row input{flex:1}.password-row button{min-width:64px;border-radius:10px;border:1px solid #28516d;background:#112a42;color:#bfeeff}.check-row{display:flex;gap:10px;align-items:center}.check-row input{width:auto;height:auto}.primary,.secondary{height:44px;border:0;border-radius:10px;padding:0 18px;font-weight:700;cursor:pointer}.primary{width:100%;background:linear-gradient(135deg,var(--cyan),var(--cyan2));color:#04111d}.secondary{background:#12263e;color:#dff8ff;border:1px solid #284963}.secondary:disabled{opacity:.45;cursor:not-allowed}.message{min-height:20px;color:var(--danger)}.help{color:var(--muted);font-size:13px}.divider{display:flex;align-items:center;margin:20px 0;color:#6d8799}.divider:before,.divider:after{content:"";height:1px;background:#213d54;flex:1}.divider span{padding:0 10px}
                .admin-shell{height:100vh;overflow:hidden;background:var(--bg)}.sidebar{position:fixed;left:0;top:0;bottom:0;width:260px;height:100vh;overflow-y:auto;border-right:1px solid var(--line);background:#081625;padding:22px 18px;display:flex;flex-direction:column;gap:24px;z-index:10}.sidebar-brand{display:flex;align-items:center;gap:12px;font-weight:800;color:#dffbff}.nav-list{display:grid;gap:8px}.nav-item{height:42px;border:1px solid transparent;border-radius:10px;background:transparent;color:#b9cfde;text-align:left;padding:0 12px;display:flex;align-items:center;gap:10px;cursor:pointer;font-size:14px}.nav-item:hover{background:#0f243b;color:#fff}.nav-item.active{background:#12334d;border-color:#1c6d88;color:#9bf3ff}.nav-icon{display:inline-grid;place-items:center;width:22px;height:22px;color:#72eaff}.icon-svg{width:18px;height:18px;stroke:currentColor;stroke-width:1.8;fill:none;stroke-linecap:round;stroke-linejoin:round}.workspace{min-width:0;margin-left:260px;height:100vh;overflow:hidden;display:grid;grid-template-rows:64px minmax(0,1fr)}.topbar{border-bottom:1px solid var(--line);display:flex;justify-content:space-between;align-items:center;padding:0 24px;background:#091827}.topbar-status,.topbar-user{display:flex;align-items:center;gap:14px;color:#c7d9e6;font-size:14px}.view-panel{padding:26px;overflow:auto;height:calc(100vh - 64px)}.page-head{display:flex;justify-content:space-between;align-items:flex-end;gap:18px;margin-bottom:22px}.page-head h1{margin:0;font-size:30px}.page-head p{margin:8px 0 0;color:var(--muted)}.toolbar{display:flex;flex-wrap:wrap;align-items:flex-end;gap:10px;margin:18px 0}.filter-field{display:grid;gap:6px;margin:0}.filter-field span{font-size:12px;color:var(--muted);font-weight:700}.input,.select{height:38px;border-radius:10px;border:1px solid #23445f;background:#081725;color:var(--text);padding:0 10px}.input{min-width:280px}.select{min-width:150px}.card-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px}.metric-card,.panel-card{background:linear-gradient(180deg,var(--panel),var(--panel2));border:1px solid var(--line);border-radius:14px;padding:18px}.metric-head{display:flex;align-items:center;justify-content:space-between;gap:12px}.metric-icon{display:inline-grid;place-items:center;width:36px;height:36px;border-radius:10px;background:#0b2738;border:1px solid #27677e;color:#8ff5ff}.metric-card .label{color:var(--muted);font-size:13px}.metric-card .value{font-size:28px;font-weight:800;margin-top:8px}.content-grid{display:grid;grid-template-columns:minmax(0,1.45fr) minmax(320px,.75fr);gap:16px;margin-top:16px}.panel-card h2{font-size:18px;margin:0 0 14px}.table-wrap{overflow:auto;border:1px solid var(--line);border-radius:14px;background:#081725}.data-table{width:100%;border-collapse:collapse;min-width:760px}.data-table th,.data-table td{padding:12px 14px;border-bottom:1px solid #142b42;text-align:left;font-size:13px}.data-table th{color:#9fb4c4;font-weight:700;background:#0a1b2c}.data-table tr:hover td{background:#0d2136}.link-button,.text-button{border:0;background:transparent;color:#7cecff;cursor:pointer;padding:0;font:inherit}.pill{display:inline-flex;align-items:center;height:24px;border-radius:999px;padding:0 9px;border:1px solid #2a4c64;color:#d7edf7;background:#102237;font-size:12px}.pill.ok{border-color:#26775c;color:#a7f3d0}.pill.warning{border-color:#826d1b;color:#fde68a}.pill.error{border-color:#91414d;color:#fecdd3}.pill.info{border-color:#246e85;color:#a5f3fc}.muted{color:var(--muted)}.empty-state,.error-state,.loading-state{border:1px dashed #28516d;border-radius:14px;padding:24px;color:#b8cbd9;background:#081725}.error-state{border-color:#7f3542;color:#fecdd3}.list-stack{display:grid;gap:10px}.event-row,.issue-row,.check-row-card,.kv-row,.chain-row,.chain-node,.endpoint-row{display:grid;gap:5px;padding:12px;border:1px solid #17324b;border-radius:10px;background:#091b2d}.event-row .meta,.issue-row .meta,.endpoint-row .meta{color:#91a7b8;font-size:12px}.device-name{display:flex;align-items:center;gap:10px}.device-subtitle{display:block;margin-top:3px;color:var(--muted);font-size:12px}.device-icon{display:inline-grid;place-items:center;width:34px;height:34px;border-radius:9px;border:1px solid #27677e;color:#8ff5ff;background:#0b2738}.device-icon .icon-svg{width:19px;height:19px}.detail-grid{display:grid;grid-template-columns:minmax(0,1fr) minmax(330px,.6fr);gap:16px}.overview-card{grid-column:1/-1}.overview-inline{display:flex;flex-wrap:wrap;gap:10px;align-items:center}.identity-grid{display:grid;grid-template-columns:160px minmax(0,1fr);gap:10px 14px}.identity-grid .k{color:var(--muted)}.identity-grid .v{word-break:break-word}.summary-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.config-section{display:grid;gap:8px;margin-bottom:14px}.config-section h3{margin:0;color:#dffbff;font-size:14px}.raw-config{margin-top:12px;color:var(--muted)}.raw-config summary{cursor:pointer;color:#7cecff}.logic-chain{display:grid;grid-template-columns:minmax(180px,1fr) 38px minmax(190px,1fr) 38px minmax(220px,1.2fr) 38px minmax(220px,1.2fr);gap:10px;align-items:stretch}.chain-arrow{display:grid;place-items:center;color:#6ee7f9;font-weight:800}.chain-node h3{margin:0 0 8px;font-size:14px;color:#dffbff}.endpoint-grid{display:grid;gap:8px}.inline-actions{display:flex;flex-wrap:wrap;gap:8px;align-items:center}.toast{position:fixed;right:24px;bottom:24px;max-width:360px;background:#102b42;border:1px solid #2a7993;color:#dffbff;border-radius:12px;padding:12px 14px;box-shadow:0 18px 50px rgba(0,0,0,.35);z-index:20}.back-row{margin-bottom:16px}
                @media(max-width:1100px){.card-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.content-grid,.detail-grid,.logic-chain{grid-template-columns:1fr}.chain-arrow{transform:rotate(90deg);min-height:24px}.sidebar{width:220px}.workspace{margin-left:220px}}
                @media(max-width:760px){.login-shell{grid-template-columns:1fr;max-width:720px;gap:34px;align-items:start;padding:32px 20px}.brand-panel{min-height:auto;justify-content:flex-start;gap:32px}.hero-copy h1{font-size:42px}.hero-copy .lead{font-size:22px}.login-card{max-width:none;justify-self:stretch}.admin-shell{height:auto;min-height:100vh;overflow:visible}.sidebar{position:static;width:auto;height:auto;max-height:42vh;overflow-y:auto;border-right:0;border-bottom:1px solid var(--line)}.workspace{margin-left:0;height:auto;min-height:100vh;overflow:visible}.nav-list{grid-template-columns:repeat(2,minmax(0,1fr))}.topbar{height:auto;min-height:76px;align-items:flex-start;gap:10px;flex-direction:column;padding:14px 18px}.topbar-status,.topbar-user{flex-wrap:wrap}.view-panel{padding:18px;height:auto;overflow:visible}.page-head{align-items:flex-start;flex-direction:column}.card-grid,.summary-grid{grid-template-columns:1fr}.input{min-width:100%}.identity-grid{grid-template-columns:1fr}}
                """;
    }

    private static String appJs() {
        return new StringBuilder().append("""
                class ApiError extends Error{
                  constructor(status, code, message){super(message || '请求失败');this.status=status;this.code=code || 'ERROR';}
                }
                const appState={me:null,status:null,deviceFilters:{search:'',type:'ALL',enabled:'ALL',doctor:'ALL',world:'ALL'},signalFilters:{search:'',consumer:'ALL',status:'ALL',sort:'RECENT'},doctorFilters:{search:'',severity:'ALL',objectType:'ALL',jump:'ALL'},historyFilters:{search:'',channel:'ALL',sourceType:'ALL',result:'ALL',range:'ALL',sort:'NEWEST'},userFilters:{search:'',role:'ALL',enabled:'ALL',online:'ALL'}};
                function esc(value){return String(value ?? '').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
                function isBlank(value){return value===undefined||value===null||String(value).trim()==='';}
                function svg(paths){return `<svg class="icon-svg" viewBox="0 0 24 24" aria-hidden="true">${paths}</svg>`}
                function icon(name){const p={
                  logo:'<path d="M5 6h14M12 6v12M8 18h8"/>',
                  dashboard:'<path d="M4 13h7V4H4zM13 20h7V4h-7zM4 20h7v-5H4z"/>',
                  device:'<rect x="5" y="5" width="14" height="14" rx="2"/><path d="M9 9h6M9 13h6M9 17h3"/>',
                  signal:'<path d="M4 12h3M17 12h3M8 12a4 4 0 0 1 8 0M11 12a1 1 0 0 1 2 0"/>',
                  receiver:'<path d="M5 12h8M13 8l4 4-4 4"/><path d="M19 5v14"/>',
                  relay:'<path d="M5 7h5v5H5zM14 12h5v5h-5zM10 10h4"/>',
                  virtual:'<path d="M12 3l8 4v10l-8 4-8-4V7z"/><path d="M12 3v18M4 7l8 4 8-4"/>',
                  region:'<path d="M5 5h14v14H5z"/><path d="M9 5v14M15 5v14M5 9h14M5 15h14"/>',
                  action:'<path d="M5 12h12M13 8l4 4-4 4"/><circle cx="5" cy="12" r="2"/>',
                  doctor:'<path d="M12 4v9"/><path d="M12 17h.01"/><path d="M4 20h16L12 3z"/>',
                  history:'<path d="M4 12a8 8 0 1 0 3-6"/><path d="M4 5v5h5"/><path d="M12 8v5l3 2"/>',
                  user:'<circle cx="12" cy="8" r="3"/><path d="M5 20a7 7 0 0 1 14 0"/>',
                  settings:'<circle cx="12" cy="12" r="3"/><path d="M19 12a7 7 0 0 0-.1-1l2-1.5-2-3.4-2.3 1a7 7 0 0 0-1.8-1L14.5 3h-5l-.3 3.1a7 7 0 0 0-1.8 1l-2.3-1-2 3.4L5.1 11a7 7 0 0 0 0 2l-2 1.5 2 3.4 2.3-1a7 7 0 0 0 1.8 1l.3 3.1h5l.3-3.1a7 7 0 0 0 1.8-1l2.3 1 2-3.4-2-1.5q.1-.5.1-1z"/>',
                  ok:'<path d="M20 6L9 17l-5-5"/>',
                  warning:'<path d="M12 4v9"/><path d="M12 17h.01"/><path d="M4 20h16L12 3z"/>'
                }[name]||'<circle cx="12" cy="12" r="8"/><path d="M12 8v4M12 16h.01"/>';return svg(p);}
                function hydrateIcons(){document.querySelectorAll('[data-icon]').forEach(el=>{el.innerHTML=icon(el.dataset.icon);});}
                function statusClass(value){const v=String(value||'').toUpperCase();if(v==='ERROR'||v==='FAILED')return'error';if(v==='WARNING')return'warning';if(v==='INFO'||v==='UNKNOWN')return'info';return'ok';}
                function pill(value){return `<span class="pill ${statusClass(value)}">${esc(labelStatus(value))}</span>`}
                function textPill(text, kind='info'){return `<span class="pill ${esc(kind)}">${esc(text)}</span>`}
                function labelStatus(value){const v=String(value||'UNKNOWN').toUpperCase();return {OK:'正常',INFO:'信息',WARNING:'警告',ERROR:'错误',UNKNOWN:'未知',SUCCESS:'成功',FAILED:'失败',SKIPPED:'跳过'}[v]||value;}
                function labelBool(value){return value?'已启用':'已禁用';}
                function labelType(value){const v=String(value||'UNKNOWN').toUpperCase();return {SIGNAL_EMITTER:'信号发射器',SIGNAL_RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',VIRTUAL_BLOCK_DEVICE:'虚拟方块设备',REGION_CONTROLLER:'区域控制器',UNKNOWN:'未知设备'}[v]||value||'未知设备';}
                function labelSourceType(value){return {DEVICE:'设备',LISTENER:'监听器',RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',REGION:'区域',COMMAND:'命令',MANUAL:'手动',SYSTEM:'系统',UNKNOWN:'未知来源'}[String(value||'UNKNOWN').toUpperCase()]||value||'-';}
                function labelEndpointType(value){return {DEVICE:'触发设备',LISTENER:'监听器',RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',REGION:'区域',COMMAND:'命令',SYSTEM:'系统',UNKNOWN:'未知节点'}[String(value||'UNKNOWN').toUpperCase()]||value||'未知节点';}
                function labelActionType(value){return {COMMAND:'命令动作',MESSAGE:'消息动作',SOUND:'音效动作',SIGNAL:'下游信号',UNKNOWN:'未知动作'}[String(value||'UNKNOWN').toUpperCase()]||value||'未知动作';}
                function labelSubType(value){const v=String(value||'').toLowerCase();return {signal_listener:'监听器',signal_emitter:'信号发射器',signal_receiver:'信号接收器',action_relay:'动作继电器',virtual_block_device:'虚拟方块设备'}[v]||labelType(value);}
                function labelServerStatus(value){return {RUNNING:'运行中',STOPPED:'已停止',STARTING:'启动中',UNKNOWN:'未知'}[String(value||'').toUpperCase()]||value||'-';}
                function labelAccessMode(value){return {LOCAL_ONLY:'本机模式',LAN_DEV:'局域网开发模式',MULTIPLAYER_DEV:'多人开发模式'}[String(value||'').toUpperCase()]||value||'-';}
                function labelRole(value){return {OWNER:'所有者',EDITOR:'编辑者',TESTER:'测试者',VIEWER:'查看者'}[String(value||'').toUpperCase()]||value||'-';}
                function labelRoleFull(value){const id=String(value||'').toUpperCase();return `${labelRole(id)}${id&&id!=='-'?`（${id}）`:''}`;}
                function labelEnabledState(value){return value?'启用':'禁用';}
                function labelOnline(value){return value?'在线':'离线';}
                function labelChannel(value){return isBlank(value)?'未设置':value;}
                function labelChannelType(value){return {DEVICE:'设备频道',REGION:'区域频道',SYSTEM:'系统频道',GAME:'游戏流程频道'}[String(value||'').toUpperCase()]||'频道';}
                function labelConsumerFilter(value){return {ALL:'全部',HAS_CONSUMER:'有消费者',NO_CONSUMER:'无消费者',HAS_LISTENER:'有监听器',HAS_RECEIVER:'有接收器',HAS_RELAY:'有动作继电器'}[value]||value;}
                function labelSignalStatusFilter(value){return {ALL:'全部',RECENT:'最近有事件',NO_RECENT:'暂无事件',WARNING:'有警告'}[value]||value;}
                function labelSignalSort(value){return {RECENT:'最近触发时间',CHANNEL:'频道名',CONSUMERS:'消费者数量'}[value]||value;}
                function labelObjectType(value){return {DEVICE:'设备',CHANNEL:'频道',LISTENER:'监听器',RECEIVER:'接收器',ACTION_RELAY:'动作继电器',ACTION:'动作',REGION:'区域',SYSTEM:'系统',UNKNOWN:'未知'}[String(value||'UNKNOWN').toUpperCase()]||value||'未知';}
                function labelHistoryRange(value){return {ALL:'全部',M10:'最近 10 分钟',H1:'最近 1 小时',H24:'最近 24 小时'}[value]||value;}
                function labelHistorySort(value){return {NEWEST:'最新优先',OLDEST:'最旧优先'}[value]||value;}
                function consumerCount(c){return Number(c?.listenerCount||0)+Number(c?.receiverCount||0)+Number(c?.actionRelayCount||0);}
                function signalHash(channel){return `#/signals/${encodeURIComponent(channel||'')}`;}
                function historyHash(channel){return isBlank(channel)?'#/history':`#/history?channel=${encodeURIComponent(channel)}`;}
                function channelButton(channel){if(isBlank(channel))return '<span class="muted">未设置</span>';return `<button class="link-button" onclick="event.stopPropagation();location.hash='${signalHash(channel)}'">${esc(channel)}</button>`}
                function navigationButton(target,label){if(isBlank(target))return esc(label||'-');if(String(target).startsWith('device:'))return `<button class="link-button" onclick="event.stopPropagation();location.hash='#/devices/${encodeURIComponent(String(target).substring(7))}'">${esc(label)}</button>`;if(String(target).startsWith('channel:'))return `<button class="link-button" onclick="event.stopPropagation();location.hash='${signalHash(String(target).substring(8))}'">${esc(label)}</button>`;return esc(label||target);}
                function labelInteractionSource(value){return {main_hand:'主手',off_hand:'副手',inventory_contains:'背包/热键栏',armor_head:'头盔槽',armor_chest:'胸甲槽',armor_legs:'护腿槽',armor_feet:'靴子槽',armor_any:'任意盔甲槽'}[String(value||'').toLowerCase()]||value;}
                function labelConsumeSource(value){return {matched_source:'匹配来源',main_hand:'主手',off_hand:'副手',inventory:'背包/热键栏'}[String(value||'').toLowerCase()]||value;}
                function labelConsumeOrder(value){return {hotbar_first:'优先热键栏',main_inventory_first:'优先主背包'}[String(value||'').toLowerCase()]||value;}
                function labelVanillaPolicy(value){return {allow:'允许原版交互',require_item_match:'需要物品匹配才允许原版交互'}[String(value||'').toLowerCase()]||value;}
                function posText(pos){return pos?`${pos.x} ${pos.y} ${pos.z}`:'-';}
                function deviceIcon(type){const v=String(type||'UNKNOWN').toUpperCase();return icon({SIGNAL_EMITTER:'signal',SIGNAL_RECEIVER:'receiver',ACTION_RELAY:'relay',VIRTUAL_BLOCK_DEVICE:'virtual',REGION_CONTROLLER:'region',UNKNOWN:'device'}[v]||'device');}
                function parseTime(value){if(isBlank(value))return null;const d=new Date(String(value));return Number.isNaN(d.getTime())?null:d;}
                function pad2(value){return String(value).padStart(2,'0');}
                function formatDateTime(value){if(isBlank(value))return '暂无';const text=String(value).trim();if(text.length>=19&&text.charAt(4)==='-'&&text.charAt(7)==='-'&&(text.charAt(10)==='T'||text.charAt(10)===' '))return `${text.slice(0,10)} ${text.slice(11,19)}`;const d=parseTime(text);if(!d)return '暂无';return `${d.getFullYear()}-${pad2(d.getMonth()+1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;}
                function formatRelativeTime(value){const d=parseTime(value);if(!d)return '暂无';const seconds=Math.max(0,Math.floor((Date.now()-d.getTime())/1000));if(seconds<60)return `${seconds} 秒前`;const minutes=Math.floor(seconds/60);if(minutes<60)return `${minutes} 分钟前`;const hours=Math.floor(minutes/60);if(hours<24)return `${hours} 小时前`;return `${Math.floor(hours/24)} 天前`;}
                function fmtTime(value){return esc(formatDateTime(value));}
                function setView(html){document.getElementById('app-view').innerHTML=html;}
                function loading(text='正在加载...'){return `<div class="loading-state">${esc(text)}</div>`}
                function empty(text){return `<div class="empty-state">${esc(text)}</div>`}
                function errorBlock(text){return `<div class="error-state">${esc(text)}</div>`}
                function toast(text){const box=document.getElementById('toast');if(!box)return;box.textContent=text;box.hidden=false;clearTimeout(box._timer);box._timer=setTimeout(()=>box.hidden=true,2800);}
                function row(k,v){return `<div class="k">${esc(k)}</div><div class="v">${v ?? ''}</div>`}
                async function api(path, options={}){
                  let res;
                  try{
                    res=await fetch(path,{credentials:'same-origin',headers:{'Content-Type':'application/json',...(options.headers||{})},...options});
                  }catch(err){throw new ApiError(0,'NETWORK_ERROR','无法连接 WebAdmin 服务');}
                  const json=await res.json().catch(()=>({ok:false,error:{code:'BAD_JSON',message:'响应解析失败'}}));
                  if(res.status===401 && document.body.dataset.page==='app'){
                    sessionStorage.setItem('webadmin.message','登录已失效，请重新登录');
                    location.href='/login';
                    throw new ApiError(401,'UNAUTHORIZED','登录已失效，请重新登录');
                  }
                  if(!res.ok||!json.ok){
                    throw new ApiError(res.status,json.error?.code,json.error?.message||'请求失败');
                  }
                  return json.data;
                }
                async function initLogin(){
                  const form=document.getElementById('login-form'); if(!form) return;
                  const pending=sessionStorage.getItem('webadmin.message'); if(pending){document.getElementById('message').textContent=pending;sessionStorage.removeItem('webadmin.message');}
                  document.getElementById('toggle-password').onclick=()=>{const p=document.getElementById('password');p.type=p.type==='password'?'text':'password'};
                  form.onsubmit=async e=>{e.preventDefault();const msg=document.getElementById('message');msg.textContent='正在登录...';try{await api('/api/auth/login',{method:'POST',body:JSON.stringify({username:username.value,password:password.value,rememberMe:remember.checked})});location.href='/app#/dashboard'}catch(err){msg.textContent=err.message}};
                }
                async function initApp(){
                  if(document.body.dataset.page!=='app') return;
                  bindChrome();
                  try{
                    appState.me=await api('/api/auth/me');
                    appState.status=await api('/api/status');
                    renderTopbar();
                  }catch(err){return;}
                  if(!location.hash){location.hash='#/dashboard';return;}
                  window.addEventListener('hashchange',route);
                  route();
                }
                function bindChrome(){
                  hydrateIcons();
                  document.querySelectorAll('.nav-item[data-route]').forEach(btn=>btn.onclick=()=>{location.hash=btn.dataset.route});
                  document.querySelectorAll('.nav-item[data-pending]').forEach(btn=>btn.onclick=()=>toast(btn.dataset.pending));
                  document.getElementById('logout').onclick=async()=>{try{await api('/api/auth/logout',{method:'POST',body:'{}'});}finally{location.href='/login'}};
                }
                function renderTopbar(){
                  const s=appState.status, me=appState.me;
                  document.getElementById('server-state').textContent=`服务器状态：${labelServerStatus(s?.server?.status)}`;
                  document.getElementById('access-mode').textContent=`访问模式：${labelAccessMode(s?.webAdmin?.accessMode)}`;
                  document.getElementById('current-user').textContent=`用户：${me?.displayName || me?.username || '-'}`;
                  document.getElementById('current-role').textContent=`角色：${labelRole(me?.role)}`;
                }
                function route(){
                  const hash=decodeURIComponent(location.hash || '#/dashboard');
                  document.querySelectorAll('.nav-item').forEach(btn=>btn.classList.toggle('active', btn.dataset.route && hash.startsWith(btn.dataset.route)));
                  if(hash==='#/dashboard') return renderDashboard();
                  if(hash==='#/devices') return renderDevices();
                  if(hash.startsWith('#/devices/')) return renderDeviceDetail(hash.substring('#/devices/'.length));
                  if(hash==='#/signals') return renderSignals();
                  if(hash.startsWith('#/signals/')) return renderSignalDetail(hash.substring('#/signals/'.length));
                  if(hash==='#/doctor') return renderDoctorPage();
                  if(hash.startsWith('#/history')) return renderHistoryPage(hash.substring('#/history'.length));
                  if(hash==='#/users') return renderUsersPage();
                  if(hash==='#/settings') return renderSettingsPage();
                  renderPlaceholder('页面暂未接入','该页面将在后续版本接入。');
                }
                async function settle(path){try{return{ok:true,data:await api(path)}}catch(err){return{ok:false,error:err}}}
                async function renderDashboard(){
                  setView(loading('正在加载总览...'));
                  const [status,devices,channels,history,doctor,regions,actions]=await Promise.all([
                    settle('/api/status'),settle('/api/devices'),settle('/api/signals/channels'),settle('/api/signals/history?limit=10'),settle('/api/doctor'),settle('/api/regions'),settle('/api/actions')
                  ]);
                  const deviceList=devices.ok?devices.data:[], channelList=channels.ok?channels.data:[], regionList=regions.ok?regions.data:[], actionList=actions.ok?actions.data:[], hist=history.ok?history.data:[], doc=doctor.ok?doctor.data:{summary:{errorCount:0,warningCount:0,infoCount:0},issues:[]};
                  setView(`
                    <div class="page-head"><div><h1>总览</h1><p>查看服务器、设备、信号与诊断状态</p></div><button class="secondary" onclick="location.reload()">刷新</button></div>
                    <section class="card-grid">
                      ${metric('服务器状态',status.ok?labelServerStatus(status.data.server?.status):'加载失败','','dashboard')}
                      ${metric('设备总数',deviceList.length,'','device')}
                      ${metric('信号频道数',channelList.length,'','signal')}
                      ${metric('区域 / 动作',`${regionList.length} / ${actionList.length}`,'','region')}
                      ${metric('诊断错误',doc.summary?.errorCount||0,'error','doctor')}
                      ${metric('诊断警告',doc.summary?.warningCount||0,'warning','warning')}
                      ${metric('当前用户',appState.me?.displayName||appState.me?.username||'-','','user')}
                      ${metric('访问模式',labelAccessMode(appState.status?.webAdmin?.accessMode),'','settings')}
                    </section>
                    <section class="content-grid">
                      <article class="panel-card"><h2>最近信号触发</h2>${history.ok?historyList(hist):errorBlock(history.error.message)}<p class="muted"><button class="link-button" onclick="location.hash='#/history'">查看全部历史</button></p></article>
                      <article class="panel-card"><h2>诊断摘要</h2>${doctor.ok?doctorList(doc.issues||[],5):errorBlock(doctor.error.message)}<p class="muted"><button class="link-button" onclick="location.hash='#/doctor'">查看 Doctor 诊断</button></p></article>
                      <article class="panel-card"><h2>设备概览</h2>${devices.ok?deviceOverview(deviceList):errorBlock(devices.error.message)}</article>
                      <article class="panel-card"><h2>WebAdmin 状态</h2><p class="muted">Dashboard、设备管理、Signal 频道、Doctor 诊断、History 历史、用户管理和系统设置只读页面已接入。编辑、配置写入、WebSocket 和完整写操作将在后续阶段接入。</p><p><button class="link-button" onclick="location.hash='#/signals'">进入 Signal 管理</button> / <button class="link-button" onclick="location.hash='#/doctor'">查看 Doctor</button> / <button class="link-button" onclick="location.hash='#/history'">查看 History</button></p></article>
                    </section>`);
                }
                function metric(label,value,kind='',iconName=''){return `<article class="metric-card ${kind}"><div class="metric-head"><div class="label">${esc(label)}</div>${iconName?`<span class="metric-icon">${icon(iconName)}</span>`:''}</div><div class="value">${esc(value)}</div></article>`}
                function historyList(items){if(!items||items.length===0)return empty('暂无 Signal 历史记录。');return `<div class="list-stack">${items.map(h=>`<div class="event-row"><strong>${esc(labelChannel(h.channel))}</strong><span class="meta">${fmtTime(h.time)} · ${esc(labelSourceType(h.sourceType))} / ${esc(h.sourceName||'-')} · ${labelStatus(h.result)}</span><span>${esc(h.description||'')}</span></div>`).join('')}</div>`}
                function doctorList(items,limit){if(!items||items.length===0)return empty('当前没有诊断问题。');return `<div class="list-stack">${items.slice(0,limit).map(i=>`<div class="issue-row"><strong>${pill(i.severity)} ${esc(i.title||'诊断问题')}</strong><span class="meta">${esc(issueContext(i))}</span><span>${esc(i.suggestion||i.message||'')}</span></div>`).join('')}</div>`}
                function issueContext(i){if(!i)return '';if(!isBlank(i.relatedObjectName))return i.relatedObjectType==='DEVICE'?`设备：${i.relatedObjectName}`:i.relatedObjectName;if(!isBlank(i.channel))return `频道：${i.channel}`;if(!isBlank(i.relatedObjectId))return `${labelSourceType(i.relatedObjectType)}：${i.relatedObjectId}`;return '';}
                function issueNavigation(i){if(!i)return '<span class="muted">暂无跳转目标</span>';const target=i.navigationTarget||(!isBlank(i.relatedObjectId)&&String(i.relatedObjectType).toUpperCase()==='DEVICE'?`device:${i.relatedObjectId}`:(!isBlank(i.channel)?`channel:${i.channel}`:''));const buttons=[];if(target)buttons.push(navigationButton(target,'查看对象'));if(!isBlank(i.channel))buttons.push(`<button class="link-button" onclick="event.stopPropagation();location.hash='${historyHash(i.channel)}'">查看历史</button>`);return buttons.length?buttons.join(' / '):'<span class="muted">暂无跳转目标</span>';}
                function issueTitle(i){return esc(i?.title||'诊断问题');}
                function issueMessage(i){return esc(i?.message||i?.impact||'暂无说明');}
                function issueSuggestion(i){return esc(i?.suggestion||'暂无建议');}
                function historyAction(h){const buttons=[];if(!isBlank(h?.channel))buttons.push(channelButton(h.channel));if(String(h?.sourceType||'').toUpperCase()==='DEVICE'&&!isBlank(h?.sourceId))buttons.push(navigationButton(`device:${h.sourceId}`,'查看设备'));return buttons.length?buttons.join(' / '):'<span class="muted">暂无关联对象</span>';}
                function deviceOverview(items){if(!items||items.length===0)return empty('当前暂无设备数据。');const enabled=items.filter(d=>d.enabled).length;const warn=items.filter(d=>['WARNING','ERROR'].includes(String(d.doctorStatus||'').toUpperCase())).length;return `<div class="summary-grid">${metric('启用设备',enabled)}${metric('禁用设备',items.length-enabled)}${metric('诊断警告/错误',warn)}${metric('虚拟方块设备',items.filter(d=>d.type==='VIRTUAL_BLOCK_DEVICE').length)}</div>`}
                async function renderDevices(){
                  setView(loading('正在加载设备列表...'));
                  let devices;try{devices=await api('/api/devices')}catch(err){setView(errorBlock(err.message));return;}
                  appState.devices=devices||[];
                  renderDeviceList('');
                }
                function renderDeviceList(focusId){
                  const devices=appState.devices||[], worlds=[...new Set(devices.map(d=>d.world).filter(Boolean))].sort();
                  const filtered=filterDevices(devices);
                  setView(`
                    <div class="page-head"><div><h1>设备管理</h1><p>查看信号设备、虚拟方块设备、动作继电器等状态</p></div></div>
                    <section class="card-grid">${metric('设备总数',devices.length,'','device')}${metric('启用设备',devices.filter(d=>d.enabled).length,'','ok')}${metric('禁用设备',devices.filter(d=>!d.enabled).length,'warning','warning')}${metric('诊断警告/错误',devices.filter(d=>['WARNING','ERROR'].includes(String(d.doctorStatus||'').toUpperCase())).length,'','doctor')}</section>
                    <div class="toolbar">
                      <input class="input" id="device-search" placeholder="搜索设备名称 / id / channel / 坐标" value="${esc(appState.deviceFilters.search)}">
                      ${filterSelect('设备类型','device-type',['ALL','SIGNAL_EMITTER','SIGNAL_RECEIVER','ACTION_RELAY','VIRTUAL_BLOCK_DEVICE','UNKNOWN'],appState.deviceFilters.type)}
                      ${filterSelect('启用状态','device-enabled',['ALL','ENABLED','DISABLED'],appState.deviceFilters.enabled)}
                      ${filterSelect('诊断状态','device-doctor',['ALL','OK','INFO','WARNING','ERROR','UNKNOWN'],appState.deviceFilters.doctor)}
                      ${filterSelect('世界/维度','device-world',['ALL',...worlds],appState.deviceFilters.world)}
                    </div>
                    ${filtered.length===0?(devices.length===0?empty('当前暂无设备数据。请在游戏内创建或绑定设备后刷新页面。'):empty('没有匹配当前筛选条件的设备。')):deviceTable(filtered)}
                  `);
                  bindDeviceFilters(focusId);
                }
                function filterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span>${select(id,options,value)}</label>`}
                function select(id,options,value){return `<select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(optionLabel(o))}</option>`).join('')}</select>`}
                function optionLabel(v){return {ALL:'全部',ENABLED:'已启用',DISABLED:'已禁用',SIGNAL_EMITTER:'信号发射器',SIGNAL_RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',VIRTUAL_BLOCK_DEVICE:'虚拟方块设备',UNKNOWN:'未知',OK:'正常',INFO:'信息',WARNING:'警告',ERROR:'错误'}[v]||v;}
                function bindDeviceFilters(focusId){
                  const update=(event)=>{appState.deviceFilters.search=document.getElementById('device-search').value;appState.deviceFilters.type=document.getElementById('device-type').value;appState.deviceFilters.enabled=document.getElementById('device-enabled').value;appState.deviceFilters.doctor=document.getElementById('device-doctor').value;appState.deviceFilters.world=document.getElementById('device-world').value;renderDeviceList(event.target.id);};
                  ['device-search','device-type','device-enabled','device-doctor','device-world'].forEach(id=>document.getElementById(id).addEventListener(id==='device-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterDevices(items){const f=appState.deviceFilters;return items.filter(d=>{const hay=[d.id,d.displayName,d.channel,d.world,posText(d.pos),d.type].join(' ').toLowerCase();if(f.search&& !hay.includes(f.search.toLowerCase()))return false;if(f.type!=='ALL'&&d.type!==f.type)return false;if(f.enabled==='ENABLED'&&!d.enabled)return false;if(f.enabled==='DISABLED'&&d.enabled)return false;if(f.doctor!=='ALL'&&String(d.doctorStatus||'UNKNOWN').toUpperCase()!==f.doctor)return false;if(f.world!=='ALL'&&d.world!==f.world)return false;return true;});}
                function deviceTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>设备</th><th>类型</th><th>世界/维度</th><th>坐标</th><th>主频道</th><th>状态</th><th>最近触发</th><th>诊断</th><th>操作</th></tr></thead><tbody>${items.map(d=>`<tr onclick="location.hash='#/devices/${encodeURIComponent(d.id)}'"><td><span class="device-name"><span class="device-icon">${deviceIcon(d.type)}</span><span><strong>${esc(d.displayName)}</strong>${deviceSubtitle(d)}</span></span></td><td>${esc(labelType(d.type))}</td><td>${esc(d.world||'-')}</td><td>${esc(posText(d.pos))}</td><td>${channelCell(d.channel)}</td><td>${pill(d.enabled?'OK':'WARNING')} ${esc(labelBool(d.enabled))}</td><td>${fmtTime(d.lastTriggeredAt)}</td><td>${pill(d.doctorStatus)}</td><td><button class="text-button" onclick="event.stopPropagation();location.hash='#/devices/${encodeURIComponent(d.id)}'">查看详情</button></td></tr>`).join('')}</tbody></table></div>`}
                function deviceSubtitle(d){const id=shortId(d.id);if(!isBlank(id)&&!String(id).toLowerCase().startsWith('minecraf'))return `<span class="device-subtitle">ID：${esc(id)}</span>`;if(!isBlank(d.world))return `<span class="device-subtitle">维度：${esc(d.world)}</span>`;return '';}
                function channelCell(channel){return channelButton(channel);}
                async function renderDeviceDetail(id){
                  setView(loading('正在加载设备详情...'));
                  const encoded=encodeURIComponent(id);
                  let detail;try{detail=await api(`/api/devices/${encoded}`)}catch(err){setView(`<div class="back-row"><button class="secondary" onclick="location.hash='#/devices'">返回设备管理</button></div>${err.status===404?errorBlock('设备不存在或已被删除。'):errorBlock(err.message)}`);return;}
                  const [debug,history,doctor]=await Promise.all([settle(`/api/devices/${encoded}/debug`),detail.channel?settle(`/api/signals/history?channel=${encodeURIComponent(detail.channel)}&limit=10`):Promise.resolve({ok:true,data:[]}),settle('/api/doctor')]);
                  const relatedDoctor=[...(detail.doctorIssues||[])];
                  if(doctor.ok){relatedDoctor.push(...(doctor.data.issues||[]).filter(i=>i.relatedObjectId===detail.id||(!isBlank(detail.channel)&&i.channel===detail.channel)));}
                  setView(`
                    <div class="back-row"><button class="secondary" onclick="location.hash='#/devices'">返回设备管理</button></div>
                    <div class="page-head"><div><h1>${esc(detail.displayName)}</h1><p>设备详情基础页 · 只读</p></div>${pill(detail.doctorStatus||detail.debugSummary?.status||'UNKNOWN')}</div>
                    <section class="detail-grid">
                      <article class="panel-card overview-card"><h2>设备概览</h2><div class="overview-inline"><span class="device-icon">${deviceIcon(detail.type)}</span><strong>${esc(labelType(detail.type))}</strong>${pill(detail.enabled?'OK':'WARNING')} ${pill(detail.doctorStatus||detail.debugSummary?.status||'UNKNOWN')}<span class="muted">坐标：${esc(posText(detail.pos))}</span><span class="muted">主频道：${esc(labelChannel(detail.channel))}</span></div></article>
                      <article class="panel-card"><h2>设备基础信息</h2><div class="identity-grid">${row('名称',esc(detail.displayName))}${row('类型',esc(labelType(detail.type)))}${row('设备 ID',esc(detail.id))}${row('短 ID',esc(shortId(detail.id)))}${row('世界/维度',esc(detail.world||'-'))}${row('坐标',esc(posText(detail.pos)))}${row('启用状态',esc(labelBool(detail.enabled)))}${row('最近触发',fmtTime(detail.lastTriggeredAt))}</div></article>
                      <article class="panel-card"><h2>关联频道</h2><div class="identity-grid">${row('主频道',channelCell(detail.channel))}${row('成功频道',channelCell(detail.configSummary?.interactionItem?.successChannel))}${row('失败频道',channelCell(detail.configSummary?.interactionItem?.failChannel))}${row('链路预览',chainPreview(detail))}</div><p class="muted">${isBlank(detail.channel)?'当前设备暂无可跳转频道。':`<button class="link-button" onclick="location.hash='${signalHash(detail.channel)}'">查看频道详情 / 逻辑链</button>`}</p></article>
                      <article class="panel-card"><h2>Debug 检查</h2>${debug.ok?debugChecks(debug.data):errorBlock(debug.error.message)}</article>
                      <article class="panel-card"><h2>Doctor 问题</h2>${doctorList(uniqueIssues(relatedDoctor),8)}<p class="muted"><button class="link-button" onclick="location.hash='#/doctor'">查看全局诊断</button></p></article>
                      <article class="panel-card"><h2>最近事件</h2>${history.ok?historyList(history.data):errorBlock(history.error.message)}<p class="muted">${isBlank(detail.channel)?'当前设备暂无关联频道历史。':`<button class="link-button" onclick="location.hash='${historyHash(detail.channel)}'">查看相关历史</button>`}</p></article>
                      <article class="panel-card"><h2>配置摘要</h2>${configSummary(detail)}</article>
                    </section>`);
                }
                function chainPreview(detail){if(isBlank(detail.channel))return '<span class="muted">当前设备没有主频道。</span>';return `<div class="chain-row"><strong>${esc(detail.displayName)}</strong><span class="muted">→ 主频道：${esc(detail.channel)}</span><span class="muted">→ 可在频道详情页查看消费者与最近事件</span></div>`}
                function debugChecks(data){const checks=data?.checks||[];if(checks.length===0)return empty('当前设备暂无 debug 数据。');return `<div class="list-stack">${checks.map(c=>`<div class="check-row-card"><strong>${pill(c.status)} ${esc(debugTitle(c))}</strong><span class="muted">${esc(debugMessage(c))}</span></div>`).join('')}</div>`}
                function debugTitle(c){const name=String(c?.name||'');if(name.includes('_')&&!isBlank(c?.message))return localizeCheckMessage(c);return localizeCheckName(name);}
                function debugMessage(c){const name=String(c?.name||'');if(name.includes('_')&&!isBlank(c?.message))return '';return localizeCheckMessage(c);}
                function localizeCheckName(name){return {enabled:'设备状态',channel:'主频道',block_id:'方块 ID',blockId:'方块 ID'}[String(name||'')]||name||'检查项';}
                function localizeCheckMessage(c){const text=String(c?.message||'');if(text==='Device is enabled.')return'当前设备处于启用状态。';if(text==='Device is disabled.')return'当前设备处于禁用状态。';if(text==='Primary channel is empty.')return'当前设备没有设置主频道。';return text.replace('Device is enabled.','当前设备处于启用状态。').replace('Primary channel is empty.','当前设备没有设置主频道。');}
                function configSummary(detail){const obj=detail?.configSummary||{};if(!obj||Object.keys(obj).length===0)return empty('当前设备暂无配置摘要。');const cfg=obj, item=cfg.interactionItem||{};let html='';
                  html+=configSection('基础配置',[
                    ['短 ID',shortId(detail.id||cfg.shortId)],
                    ['设备类型',labelType(detail.type)],
                    ['方块 ID',cfg.blockId],
                    ['工作模式',cfg.mode],
                    ['冷却时间',formatTicks(cfg.cooldownTicks)],
                    ['脉冲时间',formatTicks(cfg.pulseTicks)]
                  ]);
                  html+=configSection('信号配置',[
                    ['主频道',labelChannel(detail.channel)],
                    ['成功频道',item.successChannel],
                    ['失败频道',item.failChannel],
                    ['动作数量',cfg.actionCount]
                  ]);
                  html+=configSection('交互配置',[
                    ['普通交互',cfg.interactionEnabled?'已启用':''],
                    ['物品匹配',item.enabled?'已启用':''],
                    ['物品来源',item.sourceDisplayName||labelInteractionSource(item.source)],
                    ['原版交互策略',item.vanillaPolicyDisplayName||labelVanillaPolicy(item.vanillaPolicy)],
                    ['消耗策略',item.consumeEnabled?`${item.consumeCount||1} 个，${item.consumeSourceDisplayName||labelConsumeSource(item.consumeSource)}`:''],
                    ['背包消耗顺序',item.consumeEnabled?(item.inventoryConsumeOrderDisplayName||labelConsumeOrder(item.inventoryConsumeOrder)):''],
                    ['物品模板',item.templateSummary]
                  ]);
                  html+=configSection('容器配置',[
                    ['容器事件',cfg.containerEnabled?'已启用':''],
                    ['物品条件数量',cfg.itemConditionCount]
                  ]);
                  html+=configSection('物品提交',[
                    ['多物品提交',cfg.itemSubmitEnabled?'已启用':''],
                    ['提交条件数量',cfg.itemSubmitRequirementCount]
                  ]);
                  const raw=flatten(obj).slice(0,32).map(([k,v])=>`<div class="kv-row"><span class="muted">${esc(k)}</span><strong>${esc(v)}</strong></div>`).join('');
                  return `<div>${html || empty('当前设备没有可展示的关键配置。')}<details class="raw-config"><summary>高级 / 原始字段</summary><div class="list-stack">${raw}</div></details></div>`;
                }
                function configSection(title,rows){const filtered=(rows||[]).filter(([_,v])=>isMeaningful(v));if(filtered.length===0)return '';return `<section class="config-section"><h3>${esc(title)}</h3><div class="list-stack">${filtered.map(([k,v])=>`<div class="kv-row"><span class="muted">${esc(k)}</span><strong>${esc(v)}</strong></div>`).join('')}</div></section>`}
                function isMeaningful(v){if(v===undefined||v===null)return false;if(typeof v==='number')return v!==0;if(typeof v==='boolean')return v;if(Array.isArray(v))return v.length>0;return String(v).trim()!==''&&String(v).trim()!=='-'&&String(v).trim()!=='未设置';}
                function formatTicks(value){const n=Number(value||0);return n>0?`${n} tick`:'';}
                function flatten(obj,prefix=''){const out=[];for(const [k,v] of Object.entries(obj||{})){const key=prefix?`${prefix}.${k}`:k;if(v&&typeof v==='object'&&!Array.isArray(v)){out.push(...flatten(v,key));}else{out.push([key,Array.isArray(v)?`${v.length} 项`:(v ?? '')]);}}return out;}
                function uniqueIssues(items){const seen=new Set();return (items||[]).filter(i=>{const key=i.id||`${i.title}:${i.relatedObjectId}`;if(seen.has(key))return false;seen.add(key);return true;});}
                function shortId(id){return String(id||'').length>12?String(id).slice(0,8):String(id||'');}
                async function renderDoctorPage(){
                  setView(loading('正在加载 Doctor 诊断...'));
                  let report;try{report=await api('/api/doctor')}catch(err){setView(errorBlock(err.message));return;}
                  appState.doctorReport=report||{summary:{},issues:[]};
                  renderDoctorList('');
                }
                function renderDoctorList(focusId){
                  const report=appState.doctorReport||{summary:{},issues:[]}, issues=report.issues||[], filtered=filterDoctorIssues(issues), summary=report.summary||{};
                  const jumpTargets=issues.filter(i=>!isBlank(i.navigationTarget)||!isBlank(i.channel)).length;
                  setView(`
                    <div class="page-head"><div><h1>Doctor 诊断</h1><p>查看设备、频道、Signal 与配置风险</p></div><span class="pill info">只读模式</span></div>
                    <section class="card-grid">
                      ${metric('错误数量',summary.errorCount||0,(summary.errorCount||0)>0?'error':'','doctor')}
                      ${metric('警告数量',summary.warningCount||0,(summary.warningCount||0)>0?'warning':'','warning')}
                      ${metric('信息数量',summary.infoCount||0,'','doctor')}
                      ${metric('受影响设备',summary.affectedDeviceCount ?? '暂无','','device')}
                      ${metric('受影响频道',summary.affectedChannelCount ?? '暂无','','signal')}
                      ${metric('最近诊断',issues.length?formatDateTime(issues[0].detectedAt):'暂无','','history')}
                    </section>
                    <div class="toolbar">
                      <input class="input" id="doctor-search" placeholder="搜索标题 / 对象 / 频道 / 建议" value="${esc(appState.doctorFilters.search)}">
                      ${doctorFilterSelect('严重级别','doctor-severity',['ALL','ERROR','WARNING','INFO'],appState.doctorFilters.severity)}
                      ${doctorFilterSelect('对象类型','doctor-object',['ALL','DEVICE','CHANNEL','LISTENER','RECEIVER','ACTION_RELAY','ACTION','REGION','SYSTEM','UNKNOWN'],appState.doctorFilters.objectType)}
                      ${doctorFilterSelect('跳转目标','doctor-jump',['ALL','HAS_TARGET','NO_TARGET'],appState.doctorFilters.jump)}
                    </div>
                    <section class="content-grid">
                      <article class="panel-card">${filtered.length?doctorTable(filtered):empty(issues.length?'没有匹配当前筛选条件的诊断问题。':'当前没有诊断问题。')}</article>
                      <aside class="panel-card"><h2>诊断摘要</h2>${doctorSummaryPanel(issues,jumpTargets)}</aside>
                    </section>`);
                  bindDoctorFilters(focusId);
                }
                function doctorFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span><select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(doctorOptionLabel(o))}</option>`).join('')}</select></label>`}
                function doctorOptionLabel(v){return {ALL:'全部',ERROR:'错误',WARNING:'警告',INFO:'信息',DEVICE:'设备',CHANNEL:'频道',LISTENER:'监听器',RECEIVER:'接收器',ACTION_RELAY:'动作继电器',ACTION:'动作',REGION:'区域',SYSTEM:'系统',UNKNOWN:'未知',HAS_TARGET:'有跳转目标',NO_TARGET:'无跳转目标'}[v]||v;}
                function bindDoctorFilters(focusId){
                  const update=(event)=>{appState.doctorFilters.search=document.getElementById('doctor-search').value;appState.doctorFilters.severity=document.getElementById('doctor-severity').value;appState.doctorFilters.objectType=document.getElementById('doctor-object').value;appState.doctorFilters.jump=document.getElementById('doctor-jump').value;renderDoctorList(event.target.id);};
                  ['doctor-search','doctor-severity','doctor-object','doctor-jump'].forEach(id=>document.getElementById(id).addEventListener(id==='doctor-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterDoctorIssues(items){const f=appState.doctorFilters;return (items||[]).filter(i=>{const hay=[i.title,i.message,i.relatedObjectName,i.relatedObjectId,i.channel,i.suggestion,i.code,i.id].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.severity!=='ALL'&&String(i.severity||'').toUpperCase()!==f.severity)return false;if(f.objectType!=='ALL'&&String(i.relatedObjectType||'UNKNOWN').toUpperCase()!==f.objectType)return false;const hasTarget=!isBlank(i.navigationTarget)||!isBlank(i.channel);if(f.jump==='HAS_TARGET'&&!hasTarget)return false;if(f.jump==='NO_TARGET'&&hasTarget)return false;return true;});}
                function doctorTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>严重级别</th><th>问题标题</th><th>关联对象</th><th>对象类型</th><th>频道</th><th>影响说明</th><th>建议操作</th><th>诊断代码</th><th>操作</th></tr></thead><tbody>${items.map(i=>`<tr><td>${pill(i.severity)}</td><td><strong>${issueTitle(i)}</strong></td><td>${esc(i.relatedObjectName||i.relatedObjectId||'暂无')}</td><td>${esc(labelObjectType(i.relatedObjectType))}</td><td>${isBlank(i.channel)?'<span class="muted">暂无</span>':channelButton(i.channel)}</td><td>${issueMessage(i)}</td><td>${issueSuggestion(i)}</td><td><span class="muted">代码：${esc(i.id||'unknown')}</span></td><td>${issueNavigation(i)}</td></tr>`).join('')}</tbody></table></div>`}
                function doctorSummaryPanel(issues,jumpTargets){if(!issues||issues.length===0)return empty('当前没有诊断问题。');const bySeverity=countBy(issues,i=>String(i.severity||'UNKNOWN').toUpperCase()), byObject=countBy(issues,i=>String(i.relatedObjectType||'UNKNOWN').toUpperCase());const top=issues.slice(0,5).map(i=>`<div class="issue-row"><strong>${pill(i.severity)} ${issueTitle(i)}</strong><span class="meta">${esc(issueContext(i))}</span><span>${issueSuggestion(i)}</span></div>`).join('');return `<div class="summary-grid">${metric('可跳转问题',jumpTargets)}${metric('无跳转目标',issues.length-jumpTargets)}${metric('频道问题',byObject.CHANNEL||0)}${metric('设备问题',byObject.DEVICE||0)}</div><h3>严重级别分布</h3><div class="list-stack"><div class="kv-row"><span class="muted">错误</span><strong>${bySeverity.ERROR||0}</strong></div><div class="kv-row"><span class="muted">警告</span><strong>${bySeverity.WARNING||0}</strong></div><div class="kv-row"><span class="muted">信息</span><strong>${bySeverity.INFO||0}</strong></div></div><h3>最近问题</h3><div class="list-stack">${top}</div><p class="muted">Doctor 页面只读展示当前可低成本诊断结果，不扫描世界、不强制加载区块。</p>`;}
                function countBy(items,mapper){return (items||[]).reduce((acc,item)=>{const key=mapper(item)||'UNKNOWN';acc[key]=(acc[key]||0)+1;return acc;},{});}
                async function renderHistoryPage(queryTail=''){
                  const params=parseHashParams(queryTail);appState.historyFilters.channel=params.channel||'ALL';
                  setView(loading('正在加载历史记录...'));
                  let history;try{history=await api('/api/signals/history?limit=500')}catch(err){setView(errorBlock(err.message));return;}
                  appState.historyItems=history||[];
                  renderHistoryListPage('');
                }
                function parseHashParams(tail){const raw=String(tail||'');const query=raw.startsWith('?')?raw.substring(1):(raw.includes('?')?raw.substring(raw.indexOf('?')+1):'');const params=new URLSearchParams(query);return {channel:params.get('channel')||''};}
                function renderHistoryListPage(focusId){
                  const items=appState.historyItems||[], filtered=filterHistoryItems(items), channels=uniqueValues(items.map(h=>h.channel).filter(v=>!isBlank(v))), sourceTypes=uniqueValues(items.map(h=>h.sourceType).filter(v=>!isBlank(v)));
                  const success=items.filter(h=>String(h.result||'').toUpperCase()==='SUCCESS').length, failed=items.filter(h=>String(h.result||'').toUpperCase()==='FAILED').length, latest=items[0]?.time||'';
                  setView(`
                    <div class="page-head"><div><h1>历史记录</h1><p>查看 Signal 事件、设备事件与系统观测记录</p></div><span class="pill info">只读模式</span></div>
                    <section class="card-grid">
                      ${metric('最近记录数',items.length,'','history')}
                      ${metric('成功事件',success,'','ok')}
                      ${metric('失败事件',failed,failed>0?'warning':'','warning')}
                      ${metric('涉及频道',channels.length,'','signal')}
                      ${metric('最近事件时间',fmtTime(latest),'','history')}
                    </section>
                    <div class="toolbar">
                      <input class="input" id="history-search" placeholder="搜索频道 / 来源 / 玩家 / 详情 / 结果" value="${esc(appState.historyFilters.search)}">
                      ${historyFilterSelect('频道','history-channel',['ALL',...channels],appState.historyFilters.channel)}
                      ${historyFilterSelect('来源类型','history-source',['ALL',...sourceTypes],appState.historyFilters.sourceType)}
                      ${historyFilterSelect('结果','history-result',['ALL','SUCCESS','FAILED','UNKNOWN'],appState.historyFilters.result)}
                      ${historyFilterSelect('时间范围','history-range',['ALL','M10','H1','H24'],appState.historyFilters.range)}
                      ${historyFilterSelect('排序','history-sort',['NEWEST','OLDEST'],appState.historyFilters.sort)}
                    </div>
                    <section class="content-grid">
                      <article class="panel-card">${filtered.length?historyTable(filtered):empty(items.length?'没有匹配当前筛选条件的历史事件。':'暂无历史事件。')}</article>
                      <aside class="panel-card"><h2>时间线摘要</h2>${historySummaryPanel(filtered,items)}</aside>
                    </section>`);
                  bindHistoryFilters(focusId);
                }
                function historyFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span><select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(historyOptionLabel(id,o))}</option>`).join('')}</select></label>`}
                function historyOptionLabel(id,v){if(v==='ALL')return'全部';if(id==='history-source')return labelSourceType(v);if(id==='history-result')return labelStatus(v);if(id==='history-range')return labelHistoryRange(v);if(id==='history-sort')return labelHistorySort(v);return v;}
                function bindHistoryFilters(focusId){
                  const update=(event)=>{appState.historyFilters.search=document.getElementById('history-search').value;appState.historyFilters.channel=document.getElementById('history-channel').value;appState.historyFilters.sourceType=document.getElementById('history-source').value;appState.historyFilters.result=document.getElementById('history-result').value;appState.historyFilters.range=document.getElementById('history-range').value;appState.historyFilters.sort=document.getElementById('history-sort').value;renderHistoryListPage(event.target.id);};
                  ['history-search','history-channel','history-source','history-result','history-range','history-sort'].forEach(id=>document.getElementById(id).addEventListener(id==='history-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterHistoryItems(items){const f=appState.historyFilters;const cutoff=historyCutoff(f.range);const filtered=(items||[]).filter(h=>{const hay=[h.channel,h.sourceName,h.sourceId,h.playerName,h.result,h.description,h.sourceType].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.channel!=='ALL'&&h.channel!==f.channel)return false;if(f.sourceType!=='ALL'&&h.sourceType!==f.sourceType)return false;if(f.result!=='ALL'&&String(h.result||'UNKNOWN').toUpperCase()!==f.result)return false;if(cutoff){const d=parseTime(h.time);if(!d||d.getTime()<cutoff)return false;}return true;});return filtered.sort((a,b)=>f.sort==='OLDEST'?String(a.time||'').localeCompare(String(b.time||'')):String(b.time||'').localeCompare(String(a.time||'')));}
                function historyCutoff(range){const now=Date.now();if(range==='M10')return now-10*60*1000;if(range==='H1')return now-60*60*1000;if(range==='H24')return now-24*60*60*1000;return 0;}
                function historyTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>时间</th><th>事件类型</th><th>频道</th><th>来源对象</th><th>来源类型</th><th>玩家 / 用户</th><th>结果</th><th>详情</th><th>操作</th></tr></thead><tbody>${items.map(h=>`<tr><td>${fmtTime(h.time)}</td><td>Signal 事件</td><td>${channelButton(h.channel)}</td><td>${esc(h.sourceName||h.sourceId||'暂无')}</td><td>${esc(labelSourceType(h.sourceType))}</td><td>${esc(h.playerName||'无玩家上下文')}</td><td>${pill(h.result||'UNKNOWN')}</td><td>${esc(h.description||'暂无详情')}</td><td>${historyAction(h)}</td></tr>`).join('')}</tbody></table></div>`;}
                function historySummaryPanel(filtered,allItems){if(!allItems||allItems.length===0)return empty('暂无历史事件。');const byResult=countBy(filtered,h=>String(h.result||'UNKNOWN').toUpperCase()), bySource=countBy(filtered,h=>String(h.sourceType||'UNKNOWN').toUpperCase());const recent=filtered.slice(0,5).map(h=>`<div class="event-row"><strong>${esc(labelChannel(h.channel))}</strong><span class="meta">${fmtTime(h.time)} · ${esc(labelSourceType(h.sourceType))} · ${esc(labelStatus(h.result))}</span><span>${esc(h.description||'暂无详情')}</span></div>`).join('');return `<div class="summary-grid">${metric('当前筛选',filtered.length)}${metric('全部记录',allItems.length)}${metric('成功',byResult.SUCCESS||0)}${metric('失败',byResult.FAILED||0)}</div><h3>来源类型</h3><div class="list-stack">${Object.entries(bySource).slice(0,6).map(([k,v])=>`<div class="kv-row"><span class="muted">${esc(labelSourceType(k))}</span><strong>${v}</strong></div>`).join('')||empty('暂无来源统计。')}</div><h3>最近事件</h3><div class="list-stack">${recent||empty('当前筛选下暂无事件。')}</div><p class="muted">History 页面只读展示已有内存历史，不删除、不导出、不重放事件。</p>`;}
                """).append("""
                async function renderUsersPage(){
                  setView(loading('正在加载用户管理...'));
                  let data;try{data=await api('/api/webadmin/users')}catch(err){setView(`<div class="page-head"><div><h1>用户管理</h1><p>查看 WebAdmin 用户、角色、状态与登录情况</p></div><span class="pill info">只读模式</span></div>${err.status===403?errorBlock('权限不足：只有所有者可以查看用户管理。'):errorBlock(err.message)}`);return;}
                  appState.usersData=data||{summary:{},users:[],roles:[]};
                  renderUserList('');
                }
                function renderUserList(focusId){
                  const data=appState.usersData||{summary:{},users:[],roles:[]}, users=data.users||[], summary=data.summary||{}, filtered=filterUsers(users);
                  setView(`
                    <div class="page-head"><div><h1>用户管理</h1><p>查看 WebAdmin 用户、角色、状态与登录情况</p></div><span class="pill info">只读模式</span></div>
                    <section class="card-grid">
                      ${metric('用户总数',summary.totalCount ?? users.length,'','user')}
                      ${metric('在线用户',summary.onlineCount ?? users.filter(u=>u.online).length,'','ok')}
                      ${metric('所有者',summary.ownerCount ?? users.filter(u=>u.role==='OWNER').length,'','user')}
                      ${metric('编辑者',summary.editorCount ?? users.filter(u=>u.role==='EDITOR').length,'','settings')}
                      ${metric('测试者',summary.testerCount ?? users.filter(u=>u.role==='TESTER').length,'','action')}
                      ${metric('查看者',summary.viewerCount ?? users.filter(u=>u.role==='VIEWER').length,'','device')}
                      ${metric('禁用用户',summary.disabledCount ?? users.filter(u=>!u.enabled).length,(summary.disabledCount||0)>0?'warning':'','warning')}
                    </section>
                    <div class="toolbar">
                      <input class="input" id="user-search" placeholder="搜索用户名" value="${esc(appState.userFilters.search)}">
                      ${userFilterSelect('角色','user-role',['ALL','OWNER','EDITOR','TESTER','VIEWER'],appState.userFilters.role)}
                      ${userFilterSelect('状态','user-enabled',['ALL','ENABLED','DISABLED'],appState.userFilters.enabled)}
                      ${userFilterSelect('在线状态','user-online',['ALL','ONLINE','OFFLINE'],appState.userFilters.online)}
                    </div>
                    <section class="content-grid">
                      <article class="panel-card">${filtered.length?userTable(filtered):empty(users.length?'没有匹配当前筛选条件的用户。':'暂无 WebAdmin 用户。')}</article>
                      <aside class="panel-card"><h2>角色与安全说明</h2>${roleSummary(data.roles||[])}${securityTips()}</aside>
                    </section>`);
                  bindUserFilters(focusId);
                }
                function userFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span><select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(userOptionLabel(o))}</option>`).join('')}</select></label>`}
                function userOptionLabel(v){return {ALL:'全部',OWNER:'所有者',EDITOR:'编辑者',TESTER:'测试者',VIEWER:'查看者',ENABLED:'启用',DISABLED:'禁用',ONLINE:'在线',OFFLINE:'离线'}[v]||v;}
                function bindUserFilters(focusId){
                  const update=(event)=>{appState.userFilters.search=document.getElementById('user-search').value;appState.userFilters.role=document.getElementById('user-role').value;appState.userFilters.enabled=document.getElementById('user-enabled').value;appState.userFilters.online=document.getElementById('user-online').value;renderUserList(event.target.id);};
                  ['user-search','user-role','user-enabled','user-online'].forEach(id=>document.getElementById(id).addEventListener(id==='user-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterUsers(users){const f=appState.userFilters;return (users||[]).filter(u=>{const hay=[u.username,u.displayName,u.role,u.createdBy].join(' ').toLowerCase();if(f.search&&!hay.includes(f.search.toLowerCase()))return false;if(f.role!=='ALL'&&String(u.role||'').toUpperCase()!==f.role)return false;if(f.enabled==='ENABLED'&&!u.enabled)return false;if(f.enabled==='DISABLED'&&u.enabled)return false;if(f.online==='ONLINE'&&!u.online)return false;if(f.online==='OFFLINE'&&u.online)return false;return true;});}
                function userTable(users){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>用户名</th><th>角色</th><th>状态</th><th>在线状态</th><th>Session</th><th>最后登录</th><th>创建时间</th><th>创建者</th><th>说明</th></tr></thead><tbody>${users.map(u=>`<tr><td><span class="device-name"><span class="device-icon">${icon('user')}</span><span><strong>${esc(u.displayName||u.username)}</strong><span class="device-subtitle">用户名：${esc(u.username)}</span></span></span></td><td>${esc(labelRoleFull(u.role))}</td><td>${textPill(labelEnabledState(u.enabled),u.enabled?'ok':'warning')}</td><td>${textPill(labelOnline(u.online),u.online?'ok':'info')}</td><td>${esc(Number(u.sessionCount||0))}</td><td>${fmtTime(u.lastLoginAt)}</td><td>${fmtTime(u.createdAt)}</td><td>${esc(u.createdBy||'暂无')}</td><td>${u.forcePasswordChange?'<span class="pill warning">需首次改密</span>':'<span class="muted">暂无备注</span>'}</td></tr>`).join('')}</tbody></table></div>`;}
                function roleSummary(roles){const items=(roles&&roles.length?roles:[{role:'OWNER',displayName:'所有者（OWNER）',count:0},{role:'EDITOR',displayName:'编辑者（EDITOR）',count:0},{role:'TESTER',displayName:'测试者（TESTER）',count:0},{role:'VIEWER',displayName:'查看者（VIEWER）',count:0}]);return `<div class="list-stack">${items.map(r=>`<div class="kv-row"><span class="muted">${esc(r.displayName||labelRoleFull(r.role))}</span><strong>${esc(r.count ?? 0)}</strong></div>`).join('')}</div><h3>角色说明</h3><div class="list-stack"><div class="event-row"><strong>所有者</strong><span>完整管理权限。</span></div><div class="event-row"><strong>编辑者</strong><span>未来用于编辑配置。</span></div><div class="event-row"><strong>测试者</strong><span>未来用于测试触发。</span></div><div class="event-row"><strong>查看者</strong><span>只读查看。</span></div></div>`}
                function securityTips(){return `<h3>安全提示</h3><div class="list-stack"><div class="event-row"><span>密码不会明文保存，服务端使用 PBKDF2 哈希。</span></div><div class="event-row"><span>WebAdmin 用户按当前世界 / 存档目录隔离存储。</span></div><div class="event-row"><span>请只给可信协作者创建账号，多人访问建议配合可信网络、防火墙或反向代理。</span></div><div class="event-row"><span>6.5 页面只读展示，不提供重置密码、禁用、删除或踢出 session。</span></div></div>`}
                async function renderSettingsPage(){
                  setView(loading('正在加载系统设置...'));
                  let data;try{data=await api('/api/webadmin/settings')}catch(err){setView(errorBlock(err.message));return;}
                  const service=data.service||{}, storage=data.storage||{}, security=data.security||{}, audit=data.audit||{}, system=data.system||{}, visibility=data.visibility||{};
                  setView(`
                    <div class="page-head"><div><h1>系统设置</h1><p>查看 WebAdmin 服务、安全、存储与运行信息</p></div><span class="pill info">只读模式</span></div>
                    <section class="card-grid">
                      ${metric('服务状态',service.running?'运行中':'未运行',service.running?'':'warning','settings')}
                      ${metric('访问模式',labelAccessMode(service.accessMode),'','settings')}
                      ${metric('当前 Session',system.sessionCount ?? '暂无','','user')}
                      ${metric('审计日志',audit.enabled?'已启用':'已关闭',audit.enabled?'':'warning','history')}
                      ${metric('服务器类型',labelServerType(system.serverType),'','dashboard')}
                      ${metric('Mod 版本',system.modVersion||'暂无','','device')}
                    </section>
                    <section class="detail-grid" style="margin-top:16px">
                      <article class="panel-card"><h2>服务状态</h2><div class="identity-grid">${row('WebAdmin 服务',esc(service.running?'运行中':'未运行'))}${row('监听地址',esc(service.host||'暂无'))}${row('端口',esc(service.port ?? '暂无'))}${row('访问模式',esc(labelAccessMode(service.accessMode)))}${row('当前访问 URL',esc(service.url||'暂无'))}${row('当前登录用户',esc(service.currentUser||'暂无'))}${row('当前角色',esc(labelRoleFull(service.currentRole)))}</div></article>
                      <article class="panel-card"><h2>存储目录</h2>${storagePanel(storage,visibility)}</article>
                      <article class="panel-card"><h2>安全配置</h2><div class="identity-grid">${row('认证方式',esc(labelAuthMode(security.authMode)))}${row('密码哈希算法',esc(security.passwordHashAlgorithm||'暂无'))}${row('Session Cookie',esc(security.sessionCookieName||'暂无'))}${row('Session 有效期',esc(formatMinutes(security.sessionTtlMinutes)))}${row('记住我有效期',esc(formatMinutes(security.rememberMeTtlMinutes)))}${row('审计日志',esc(security.auditEnabled?'已启用':'已关闭'))}${row('远程访问',esc(security.remoteAccessAllowed?'允许当前访问模式远程协作':'本机访问'))}</div></article>
                      <article class="panel-card"><h2>审计 / History</h2><div class="identity-grid">${row('审计日志状态',esc(audit.enabled?'已启用':'已关闭'))}${row('审计文件',esc(audit.auditLogExists?'已存在':'暂无文件'))}${row('最近登录记录',esc(audit.recentLoginRecords||'暂无数据'))}${row('API 访问统计',esc(audit.apiAccessStats||'暂无数据'))}</div></article>
                      <article class="panel-card"><h2>系统信息</h2><div class="identity-grid">${row('TZZ Mod 版本',esc(system.modVersion||'暂无数据'))}${row('Minecraft 版本',esc(system.minecraftVersion||'暂无数据'))}${row('服务器类型',esc(labelServerType(system.serverType)))}${row('当前世界 / 存档',esc(system.worldName||'暂无数据'))}</div></article>
                      <article class="panel-card"><h2>危险操作说明</h2><p class="muted">6.5 系统设置页面只读展示。修改端口、访问模式、用户、密码等操作当前请使用 /tzz webadmin 命令或服务端配置文件；Web UI 写操作将在后续阶段谨慎开放。</p></article>
                    </section>`);
                }
                function storagePanel(storage,visibility){const restricted=storage.restricted||visibility.sensitiveStorageHidden;const hidden='受限信息已隐藏';return `<div class="identity-grid">${row('存储作用域',esc(storage.scope||'WORLD_SAVE'))}${row('按世界隔离',esc(storage.worldScoped?'是':'否'))}${row('WebAdmin 存储目录',esc(restricted?hidden:(storage.directory||'暂无')))}${row('配置文件',esc(restricted?hidden:(storage.configPath||'暂无')))}${row('用户文件',esc(restricted?hidden:(storage.usersPath||'暂无')))}${row('审计日志',esc(restricted?hidden:(storage.auditLogPath||'暂无')))}${row('配置文件存在',esc(storage.configExists?'是':'否'))}${row('用户文件存在',esc(storage.usersExists?'是':'否'))}${row('审计日志存在',esc(storage.auditLogExists?'是':'否'))}${row('旧全局文件提示',esc(storage.legacyGlobalFilesDetected?'检测到旧 config/tzz WebAdmin 文件，但不会自动加载':'未检测到旧全局文件'))}</div><p class="muted">WebAdmin 持久化文件统一放在当前世界 / 存档目录下的 tzz/webadmin/，不再使用全局 config/tzz。</p>`}
                function labelAuthMode(value){return {USERNAME_PASSWORD:'用户名 / 密码'}[String(value||'').toUpperCase()]||value||'暂无';}
                function labelServerType(value){return {DEDICATED:'专用服务器（DEDICATED）',INTEGRATED:'集成服务器（INTEGRATED）'}[String(value||'').toUpperCase()]||value||'暂无';}
                function formatMinutes(value){const n=Number(value||0);return n>0?`${n} 分钟`:'暂无';}
                function uniqueValues(items){return [...new Set(items)].sort((a,b)=>String(a).localeCompare(String(b)));}
                async function renderSignals(){
                  setView(loading('正在加载 Signal 频道...'));
                  let channels;try{channels=await api('/api/signals/channels')}catch(err){setView(errorBlock(err.message));return;}
                  appState.signals=channels||[];
                  renderSignalList('');
                }
                function renderSignalList(focusId){
                  const channels=appState.signals||[], filtered=filterSignalChannels(channels);
                  const hasConsumers=channels.filter(c=>consumerCount(c)>0).length;
                  const recent=channels.filter(c=>!isBlank(c.lastTriggeredAt)).length;
                  const warning=channels.filter(c=>['WARNING','ERROR'].includes(String(c.doctorStatus||'').toUpperCase())).length;
                  setView(`
                    <div class="page-head"><div><h1>Signal 管理</h1><p>查看频道、消费者、最近触发与逻辑链入口</p></div><span class="pill info">只读模式</span></div>
                    <section class="card-grid">
                      ${metric('频道总数',channels.length,'','signal')}
                      ${metric('有消费者频道',hasConsumers,'','receiver')}
                      ${metric('无消费者频道',channels.length-hasConsumers,(channels.length-hasConsumers)>0?'warning':'','warning')}
                      ${metric('最近触发频道',recent,'','history')}
                      ${metric('最近 Signal 事件',channels.reduce((sum,c)=>sum+Number(c.triggerCountToday||0),0),'','history')}
                      ${metric('Doctor 警告',warning,warning>0?'warning':'','doctor')}
                    </section>
                    <div class="toolbar">
                      <input class="input" id="signal-search" placeholder="搜索频道名" value="${esc(appState.signalFilters.search)}">
                      ${signalFilterSelect('消费者','signal-consumer',['ALL','HAS_CONSUMER','NO_CONSUMER','HAS_LISTENER','HAS_RECEIVER','HAS_RELAY'],appState.signalFilters.consumer)}
                      ${signalFilterSelect('状态','signal-status',['ALL','RECENT','NO_RECENT','WARNING'],appState.signalFilters.status)}
                      ${signalFilterSelect('排序','signal-sort',['RECENT','CHANNEL','CONSUMERS'],appState.signalFilters.sort)}
                    </div>
                    ${filtered.length===0?(channels.length===0?empty('当前暂无 Signal 频道数据。请在游戏内触发 signal 或配置 listener / receiver / action_relay 后刷新。'):empty('没有匹配当前筛选条件的频道。')):signalTable(filtered)}
                    <article class="panel-card" style="margin-top:16px"><h2>预设频道图标说明</h2><p class="muted">6.3 只读阶段按频道状态和类型显示预设 2D 图标，不提供图标编辑或上传。</p></article>
                  `);
                  bindSignalFilters(focusId);
                }
                function signalFilterSelect(label,id,options,value){return `<label class="filter-field"><span>${esc(label)}</span><select class="select" id="${id}">${options.map(o=>`<option value="${esc(o)}" ${o===value?'selected':''}>${esc(signalOptionLabel(o))}</option>`).join('')}</select></label>`}
                function signalOptionLabel(v){return {ALL:'全部',HAS_CONSUMER:'有消费者',NO_CONSUMER:'无消费者',HAS_LISTENER:'有监听器',HAS_RECEIVER:'有接收器',HAS_RELAY:'有动作继电器',RECENT:'最近有事件',NO_RECENT:'暂无事件',WARNING:'有警告',CHANNEL:'频道名',CONSUMERS:'消费者数量'}[v]||labelSignalSort(v)||v;}
                function bindSignalFilters(focusId){
                  const update=(event)=>{appState.signalFilters.search=document.getElementById('signal-search').value;appState.signalFilters.consumer=document.getElementById('signal-consumer').value;appState.signalFilters.status=document.getElementById('signal-status').value;appState.signalFilters.sort=document.getElementById('signal-sort').value;renderSignalList(event.target.id);};
                  ['signal-search','signal-consumer','signal-status','signal-sort'].forEach(id=>document.getElementById(id).addEventListener(id==='signal-search'?'input':'change',update));
                  if(focusId){const el=document.getElementById(focusId);if(el){el.focus();if(el.setSelectionRange&&el.value){el.setSelectionRange(el.value.length,el.value.length);}}}
                }
                function filterSignalChannels(items){
                  const f=appState.signalFilters;
                  const filtered=items.filter(c=>{
                    if(f.search && !String(c.channel||'').toLowerCase().includes(f.search.toLowerCase()))return false;
                    if(f.consumer==='HAS_CONSUMER'&&consumerCount(c)===0)return false;
                    if(f.consumer==='NO_CONSUMER'&&consumerCount(c)>0)return false;
                    if(f.consumer==='HAS_LISTENER'&&Number(c.listenerCount||0)===0)return false;
                    if(f.consumer==='HAS_RECEIVER'&&Number(c.receiverCount||0)===0)return false;
                    if(f.consumer==='HAS_RELAY'&&Number(c.actionRelayCount||0)===0)return false;
                    if(f.status==='RECENT'&&isBlank(c.lastTriggeredAt))return false;
                    if(f.status==='NO_RECENT'&&!isBlank(c.lastTriggeredAt))return false;
                    if(f.status==='WARNING'&&!['WARNING','ERROR'].includes(String(c.doctorStatus||'').toUpperCase()))return false;
                    return true;
                  });
                  return filtered.sort((a,b)=>{
                    if(f.sort==='CHANNEL')return String(a.channel||'').localeCompare(String(b.channel||''));
                    if(f.sort==='CONSUMERS')return consumerCount(b)-consumerCount(a);
                    return String(b.lastTriggeredAt||'').localeCompare(String(a.lastTriggeredAt||'')) || String(a.channel||'').localeCompare(String(b.channel||''));
                  });
                }
                function signalTable(items){return `<div class="table-wrap"><table class="data-table"><thead><tr><th>频道</th><th>消费者摘要</th><th>监听器</th><th>接收器</th><th>动作继电器</th><th>最近触发</th><th>最近来源</th><th>诊断</th><th>操作</th></tr></thead><tbody>${items.map(c=>`<tr onclick="location.hash='${signalHash(c.channel)}'"><td><span class="device-name"><span class="device-icon">${icon(c.iconKey||'signal')}</span><span><strong>${esc(c.displayName||c.channel)}</strong><span class="device-subtitle">${esc(labelChannelType(c.type))}</span></span></span></td><td>${consumerSummary(c)}</td><td>${Number(c.listenerCount||0)}</td><td>${Number(c.receiverCount||0)}</td><td>${Number(c.actionRelayCount||0)}</td><td>${fmtTime(c.lastTriggeredAt)}</td><td>${esc(c.sourceCount?`${c.sourceCount} 个来源`:'暂无')}</td><td>${pill(c.doctorStatus)}</td><td><button class="text-button" onclick="event.stopPropagation();location.hash='${signalHash(c.channel)}'">查看详情</button></td></tr>`).join('')}</tbody></table></div>`}
                function consumerSummary(c){const count=consumerCount(c);if(count===0)return '<span class="muted">暂无消费者</span>';return `<span class="pill info">${count} 个消费者</span>`}
                async function renderSignalDetail(channel){
                  const decoded=channel||'';
                  setView(loading('正在加载频道详情...'));
                  let detail;try{detail=await api(`/api/signals/channels/${encodeURIComponent(decoded)}`)}catch(err){setView(`<div class="back-row"><button class="secondary" onclick="location.hash='#/signals'">返回 Signal 管理</button></div>${err.status===404?errorBlock('频道不存在或当前没有可读取数据。'):errorBlock(err.message)}`);return;}
                  const stats=detail.stats||{}, totalConsumers=Number(stats.listenerCount||0)+Number(stats.receiverCount||0)+Number(stats.actionRelayCount||0);
                  setView(`
                    <div class="back-row"><button class="secondary" onclick="location.hash='#/signals'">返回 Signal 管理</button></div>
                    <div class="page-head"><div><h1>${esc(detail.channel)}</h1><p>频道详情 / 横向逻辑链 · 只读</p></div><div class="inline-actions">${pill((detail.doctorIssues||[]).some(i=>i.severity==='ERROR')?'ERROR':((detail.doctorIssues||[]).length?'WARNING':'OK'))}<span class="pill info">只读模式</span></div></div>
                    <section class="card-grid">
                      ${metric('消费者数量',totalConsumers,'','receiver')}
                      ${metric('监听器',Number(stats.listenerCount||0),'','signal')}
                      ${metric('接收器',Number(stats.receiverCount||0),'','receiver')}
                      ${metric('动作继电器',Number(stats.actionRelayCount||0),'','relay')}
                      ${metric('关联来源',Number(stats.sourceCount||0),'','device')}
                      ${metric('下游 Signal',Number(stats.downstreamSignalCount||0),'','signal')}
                      ${metric('最近事件数',Number(stats.triggerCountToday||0),'','history')}
                      ${metric('最近触发',formatDateTime(stats.lastTriggeredAt),'','history')}
                    </section>
                    <section class="detail-grid" style="margin-top:16px">
                      <article class="panel-card overview-card"><h2>频道基础信息</h2><div class="identity-grid">${row('频道名',esc(detail.channel))}${row('频道类型',esc(labelChannelType(detail.type)))}${row('最近触发',fmtTime(stats.lastTriggeredAt))}${row('消费者',esc(`${totalConsumers} 个`))}${row('监听器',esc(Number(stats.listenerCount||0)))}${row('接收器',esc(Number(stats.receiverCount||0)))}${row('动作继电器',esc(Number(stats.actionRelayCount||0)))}</div></article>
                      <article class="panel-card overview-card"><h2>横向逻辑链</h2>${logicChain(detail)}</article>
                      <article class="panel-card"><h2>消费者</h2>${endpointGroups(detail)}</article>
                      <article class="panel-card"><h2>动作 / 下游影响</h2>${actionsPanel(detail)}</article>
                      <article class="panel-card"><h2>最近 Signal 事件</h2>${historyList(detail.recentHistory||[])}<p class="muted"><button class="link-button" onclick="location.hash='${historyHash(detail.channel)}'">查看该频道历史</button></p></article>
                      <article class="panel-card"><h2>频道诊断</h2>${doctorList(detail.doctorIssues||[],8)}<p class="muted"><button class="link-button" onclick="location.hash='#/doctor'">查看全局诊断</button></p></article>
                    </section>`);
                }
                function logicChain(detail){
                  const sources=detail.sources||[], listeners=detail.listeners||[], receivers=detail.receivers||[], relays=detail.actionRelays||[], actions=detail.actions||[], downstream=detail.downstreamSignals||[];
                  return `<div class="logic-chain">
                    <div class="chain-node"><h3>触发源</h3>${endpointCompact(sources,'暂无可推断触发源')}</div>
                    <div class="chain-arrow">→</div>
                    <div class="chain-node"><h3>频道</h3><strong>${esc(detail.channel)}</strong><span class="muted">${esc(labelChannelType(detail.type))}</span>${pill((detail.doctorIssues||[]).length?'WARNING':'OK')}</div>
                    <div class="chain-arrow">→</div>
                    <div class="chain-node"><h3>消费者</h3>${endpointCompact([...listeners,...receivers,...relays],'暂无消费者')}</div>
                    <div class="chain-arrow">→</div>
                    <div class="chain-node"><h3>动作 / 下游影响</h3>${actions.length?actions.slice(0,4).map(a=>`<span>${esc(labelActionType(a.type))}：${esc(a.summary||a.name||'-')}</span>`).join(''):(downstream.length?downstream.map(c=>`<span>下游频道：${esc(c)}</span>`).join(''):'<span class="muted">暂无可用动作详情</span>')}</div>
                  </div>`;
                }
                function endpointCompact(items,emptyText){if(!items||items.length===0)return `<span class="muted">${esc(emptyText)}</span>`;return items.slice(0,4).map(e=>`<span>${navigationButton(e.navigationTarget,e.name||e.id)} <span class="muted">(${esc(labelEndpointType(e.type))})</span></span>`).join('');}
                function endpointGroups(detail){
                  const groups=[['监听器',detail.listeners||[]],['接收器',detail.receivers||[]],['动作继电器',detail.actionRelays||[]]];
                  return `<div class="list-stack">${groups.map(([name,items])=>`<div class="endpoint-row"><strong>${esc(name)}：${items.length}</strong>${items.length?items.map(endpointRow).join(''): '<span class="muted">暂无</span>'}</div>`).join('')}</div>`;
                }
                function endpointRow(e){return `<span>${navigationButton(e.navigationTarget,e.name||e.id)} <span class="meta">${esc(labelEndpointType(e.type))} / ${esc(labelSubType(e.subType))} / ${e.enabled?'启用':'禁用'}${e.pos?` / ${esc(posText(e.pos))}`:''}</span></span>`}
                function actionsPanel(detail){
                  const actions=detail.actions||[], downstream=detail.downstreamSignals||[];
                  if(actions.length===0&&downstream.length===0)return empty('暂无可用动作详情。');
                  return `<div class="list-stack">${actions.map(a=>`<div class="event-row"><strong>${esc(labelActionType(a.type))}</strong><span class="meta">${esc(a.ownerName||a.ownerId||'-')} · ${esc(labelEndpointType(a.ownerType))}</span><span>${esc(a.summary||'')}</span></div>`).join('')}${downstream.map(c=>`<div class="event-row"><strong>下游频道</strong><span>${channelButton(c)}</span></div>`).join('')}</div>`;
                }
                function renderPlaceholder(title,message){setView(`<div class="page-head"><div><h1>${esc(title)}</h1><p>${esc(message)}</p></div></div>${empty('该模块将在后续版本接入。')}`)}
                initLogin();initApp();
                """).toString();
    }

    private static final class LoginRequest {
        String username;
        String password;
        boolean rememberMe;
    }

    private record AuthContext(String rawToken, WebAdminSession session, WebAdminUser user) {
    }
}
