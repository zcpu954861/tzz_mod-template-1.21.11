package com.zcpu.tzzmod.webadmin;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.webadmin.route.WebAdminReadonlyRoutes;
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
                        <button class="nav-item" data-pending="信号管理将在后续版本接入"><span class="nav-icon" data-icon="signal"></span>信号管理</button>
                        <button class="nav-item" data-pending="区域管理将在后续版本接入"><span class="nav-icon" data-icon="region"></span>区域管理</button>
                        <button class="nav-item" data-pending="动作系统将在后续版本接入"><span class="nav-icon" data-icon="action"></span>动作系统</button>
                        <button class="nav-item" data-pending="诊断完整页将在后续版本接入"><span class="nav-icon" data-icon="doctor"></span>诊断中心</button>
                        <button class="nav-item" data-pending="历史记录完整页将在后续版本接入"><span class="nav-icon" data-icon="history"></span>历史记录</button>
                        <button class="nav-item" data-pending="用户管理 Web 页面将在后续版本接入"><span class="nav-icon" data-icon="user"></span>用户管理</button>
                        <button class="nav-item" data-pending="系统设置页面将在后续版本接入"><span class="nav-icon" data-icon="settings"></span>系统设置</button>
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
                .admin-shell{height:100vh;overflow:hidden;background:var(--bg)}.sidebar{position:fixed;left:0;top:0;bottom:0;width:260px;height:100vh;overflow-y:auto;border-right:1px solid var(--line);background:#081625;padding:22px 18px;display:flex;flex-direction:column;gap:24px;z-index:10}.sidebar-brand{display:flex;align-items:center;gap:12px;font-weight:800;color:#dffbff}.nav-list{display:grid;gap:8px}.nav-item{height:42px;border:1px solid transparent;border-radius:10px;background:transparent;color:#b9cfde;text-align:left;padding:0 12px;display:flex;align-items:center;gap:10px;cursor:pointer;font-size:14px}.nav-item:hover{background:#0f243b;color:#fff}.nav-item.active{background:#12334d;border-color:#1c6d88;color:#9bf3ff}.nav-icon{display:inline-grid;place-items:center;width:22px;height:22px;color:#72eaff}.icon-svg{width:18px;height:18px;stroke:currentColor;stroke-width:1.8;fill:none;stroke-linecap:round;stroke-linejoin:round}.workspace{min-width:0;margin-left:260px;height:100vh;overflow:hidden;display:grid;grid-template-rows:64px minmax(0,1fr)}.topbar{border-bottom:1px solid var(--line);display:flex;justify-content:space-between;align-items:center;padding:0 24px;background:#091827}.topbar-status,.topbar-user{display:flex;align-items:center;gap:14px;color:#c7d9e6;font-size:14px}.view-panel{padding:26px;overflow:auto;height:calc(100vh - 64px)}.page-head{display:flex;justify-content:space-between;align-items:flex-end;gap:18px;margin-bottom:22px}.page-head h1{margin:0;font-size:30px}.page-head p{margin:8px 0 0;color:var(--muted)}.toolbar{display:flex;flex-wrap:wrap;align-items:flex-end;gap:10px;margin:18px 0}.filter-field{display:grid;gap:6px;margin:0}.filter-field span{font-size:12px;color:var(--muted);font-weight:700}.input,.select{height:38px;border-radius:10px;border:1px solid #23445f;background:#081725;color:var(--text);padding:0 10px}.input{min-width:280px}.select{min-width:150px}.card-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px}.metric-card,.panel-card{background:linear-gradient(180deg,var(--panel),var(--panel2));border:1px solid var(--line);border-radius:14px;padding:18px}.metric-head{display:flex;align-items:center;justify-content:space-between;gap:12px}.metric-icon{display:inline-grid;place-items:center;width:36px;height:36px;border-radius:10px;background:#0b2738;border:1px solid #27677e;color:#8ff5ff}.metric-card .label{color:var(--muted);font-size:13px}.metric-card .value{font-size:28px;font-weight:800;margin-top:8px}.content-grid{display:grid;grid-template-columns:minmax(0,1.45fr) minmax(320px,.75fr);gap:16px;margin-top:16px}.panel-card h2{font-size:18px;margin:0 0 14px}.table-wrap{overflow:auto;border:1px solid var(--line);border-radius:14px;background:#081725}.data-table{width:100%;border-collapse:collapse;min-width:760px}.data-table th,.data-table td{padding:12px 14px;border-bottom:1px solid #142b42;text-align:left;font-size:13px}.data-table th{color:#9fb4c4;font-weight:700;background:#0a1b2c}.data-table tr:hover td{background:#0d2136}.link-button,.text-button{border:0;background:transparent;color:#7cecff;cursor:pointer;padding:0;font:inherit}.pill{display:inline-flex;align-items:center;height:24px;border-radius:999px;padding:0 9px;border:1px solid #2a4c64;color:#d7edf7;background:#102237;font-size:12px}.pill.ok{border-color:#26775c;color:#a7f3d0}.pill.warning{border-color:#826d1b;color:#fde68a}.pill.error{border-color:#91414d;color:#fecdd3}.pill.info{border-color:#246e85;color:#a5f3fc}.muted{color:var(--muted)}.empty-state,.error-state,.loading-state{border:1px dashed #28516d;border-radius:14px;padding:24px;color:#b8cbd9;background:#081725}.error-state{border-color:#7f3542;color:#fecdd3}.list-stack{display:grid;gap:10px}.event-row,.issue-row,.check-row-card,.kv-row,.chain-row{display:grid;gap:5px;padding:12px;border:1px solid #17324b;border-radius:10px;background:#091b2d}.event-row .meta,.issue-row .meta{color:#91a7b8;font-size:12px}.device-name{display:flex;align-items:center;gap:10px}.device-subtitle{display:block;margin-top:3px;color:var(--muted);font-size:12px}.device-icon{display:inline-grid;place-items:center;width:34px;height:34px;border-radius:9px;border:1px solid #27677e;color:#8ff5ff;background:#0b2738}.device-icon .icon-svg{width:19px;height:19px}.detail-grid{display:grid;grid-template-columns:minmax(0,1fr) minmax(330px,.6fr);gap:16px}.overview-card{grid-column:1/-1}.overview-inline{display:flex;flex-wrap:wrap;gap:10px;align-items:center}.identity-grid{display:grid;grid-template-columns:160px minmax(0,1fr);gap:10px 14px}.identity-grid .k{color:var(--muted)}.identity-grid .v{word-break:break-word}.summary-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.config-section{display:grid;gap:8px;margin-bottom:14px}.config-section h3{margin:0;color:#dffbff;font-size:14px}.raw-config{margin-top:12px;color:var(--muted)}.raw-config summary{cursor:pointer;color:#7cecff}.toast{position:fixed;right:24px;bottom:24px;max-width:360px;background:#102b42;border:1px solid #2a7993;color:#dffbff;border-radius:12px;padding:12px 14px;box-shadow:0 18px 50px rgba(0,0,0,.35);z-index:20}.back-row{margin-bottom:16px}
                @media(max-width:1100px){.card-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.content-grid,.detail-grid{grid-template-columns:1fr}.sidebar{width:220px}.workspace{margin-left:220px}}
                @media(max-width:760px){.login-shell{grid-template-columns:1fr;max-width:720px;gap:34px;align-items:start;padding:32px 20px}.brand-panel{min-height:auto;justify-content:flex-start;gap:32px}.hero-copy h1{font-size:42px}.hero-copy .lead{font-size:22px}.login-card{max-width:none;justify-self:stretch}.admin-shell{height:auto;min-height:100vh;overflow:visible}.sidebar{position:static;width:auto;height:auto;max-height:42vh;overflow-y:auto;border-right:0;border-bottom:1px solid var(--line)}.workspace{margin-left:0;height:auto;min-height:100vh;overflow:visible}.nav-list{grid-template-columns:repeat(2,minmax(0,1fr))}.topbar{height:auto;min-height:76px;align-items:flex-start;gap:10px;flex-direction:column;padding:14px 18px}.topbar-status,.topbar-user{flex-wrap:wrap}.view-panel{padding:18px;height:auto;overflow:visible}.page-head{align-items:flex-start;flex-direction:column}.card-grid,.summary-grid{grid-template-columns:1fr}.input{min-width:100%}.identity-grid{grid-template-columns:1fr}}
                """;
    }

    private static String appJs() {
        return """
                class ApiError extends Error{
                  constructor(status, code, message){super(message || '请求失败');this.status=status;this.code=code || 'ERROR';}
                }
                const appState={me:null,status:null,deviceFilters:{search:'',type:'ALL',enabled:'ALL',doctor:'ALL',world:'ALL'}};
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
                function labelStatus(value){const v=String(value||'UNKNOWN').toUpperCase();return {OK:'正常',INFO:'信息',WARNING:'警告',ERROR:'错误',UNKNOWN:'未知',SUCCESS:'成功',FAILED:'失败',SKIPPED:'跳过'}[v]||value;}
                function labelBool(value){return value?'已启用':'已禁用';}
                function labelType(value){const v=String(value||'UNKNOWN').toUpperCase();return {SIGNAL_EMITTER:'信号发射器',SIGNAL_RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',VIRTUAL_BLOCK_DEVICE:'虚拟方块设备',REGION_CONTROLLER:'区域控制器',UNKNOWN:'未知设备'}[v]||value||'未知设备';}
                function labelSourceType(value){return {DEVICE:'设备',LISTENER:'监听器',RECEIVER:'信号接收器',ACTION_RELAY:'动作继电器',REGION:'区域',COMMAND:'命令',SYSTEM:'系统',UNKNOWN:'未知来源'}[value]||value||'-';}
                function labelServerStatus(value){return {RUNNING:'运行中',STOPPED:'已停止',STARTING:'启动中',UNKNOWN:'未知'}[String(value||'').toUpperCase()]||value||'-';}
                function labelAccessMode(value){return {LOCAL_ONLY:'本机模式',LAN_DEV:'局域网开发模式',MULTIPLAYER_DEV:'多人开发模式'}[String(value||'').toUpperCase()]||value||'-';}
                function labelRole(value){return {OWNER:'所有者',EDITOR:'编辑者',TESTER:'测试者',VIEWER:'只读查看者'}[String(value||'').toUpperCase()]||value||'-';}
                function labelChannel(value){return isBlank(value)?'未设置':value;}
                function labelInteractionSource(value){return {main_hand:'主手',off_hand:'副手',inventory_contains:'背包/热键栏',armor_head:'头盔槽',armor_chest:'胸甲槽',armor_legs:'护腿槽',armor_feet:'靴子槽',armor_any:'任意盔甲槽'}[String(value||'').toLowerCase()]||value;}
                function labelConsumeSource(value){return {matched_source:'匹配来源',main_hand:'主手',off_hand:'副手',inventory:'背包/热键栏'}[String(value||'').toLowerCase()]||value;}
                function labelConsumeOrder(value){return {hotbar_first:'优先热键栏',main_inventory_first:'优先主背包'}[String(value||'').toLowerCase()]||value;}
                function labelVanillaPolicy(value){return {allow:'允许原版交互',require_item_match:'需要物品匹配才允许原版交互'}[String(value||'').toLowerCase()]||value;}
                function posText(pos){return pos?`${pos.x} ${pos.y} ${pos.z}`:'-';}
                function deviceIcon(type){const v=String(type||'UNKNOWN').toUpperCase();return icon({SIGNAL_EMITTER:'signal',SIGNAL_RECEIVER:'receiver',ACTION_RELAY:'relay',VIRTUAL_BLOCK_DEVICE:'virtual',REGION_CONTROLLER:'region',UNKNOWN:'device'}[v]||'device');}
                function fmtTime(value){if(isBlank(value))return '暂无';return esc(value);}
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
                      <article class="panel-card"><h2>最近信号触发</h2>${history.ok?historyList(hist):errorBlock(history.error.message)}</article>
                      <article class="panel-card"><h2>诊断摘要</h2>${doctor.ok?doctorList(doc.issues||[],5):errorBlock(doctor.error.message)}<p class="muted"><button class="link-button" onclick="toast('诊断完整页将在后续版本接入')">查看详情</button></p></article>
                      <article class="panel-card"><h2>设备概览</h2>${devices.ok?deviceOverview(deviceList):errorBlock(devices.error.message)}</article>
                      <article class="panel-card"><h2>WebAdmin 状态</h2><p class="muted">Dashboard 与设备管理只读页面已接入。设备编辑、Signal 频道详情、Doctor 完整页和 History 完整页将在后续阶段接入。</p></article>
                    </section>`);
                }
                function metric(label,value,kind='',iconName=''){return `<article class="metric-card ${kind}"><div class="metric-head"><div class="label">${esc(label)}</div>${iconName?`<span class="metric-icon">${icon(iconName)}</span>`:''}</div><div class="value">${esc(value)}</div></article>`}
                function historyList(items){if(!items||items.length===0)return empty('暂无 Signal 历史记录。');return `<div class="list-stack">${items.map(h=>`<div class="event-row"><strong>${esc(labelChannel(h.channel))}</strong><span class="meta">${fmtTime(h.time)} · ${esc(labelSourceType(h.sourceType))} / ${esc(h.sourceName||'-')} · ${labelStatus(h.result)}</span><span>${esc(h.description||'')}</span></div>`).join('')}</div>`}
                function doctorList(items,limit){if(!items||items.length===0)return empty('当前没有诊断问题。');return `<div class="list-stack">${items.slice(0,limit).map(i=>`<div class="issue-row"><strong>${pill(i.severity)} ${esc(i.title||'诊断问题')}</strong><span class="meta">${esc(issueContext(i))}</span><span>${esc(i.suggestion||i.message||'')}</span></div>`).join('')}</div>`}
                function issueContext(i){if(!i)return '';if(!isBlank(i.relatedObjectName))return i.relatedObjectType==='DEVICE'?`设备：${i.relatedObjectName}`:i.relatedObjectName;if(!isBlank(i.channel))return `频道：${i.channel}`;if(!isBlank(i.relatedObjectId))return `${labelSourceType(i.relatedObjectType)}：${i.relatedObjectId}`;return '';}
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
                function channelCell(channel){if(isBlank(channel))return '<span class="muted">未设置</span>';return `<button class="link-button" onclick="event.stopPropagation();toast('频道详情 / 逻辑链将在后续 6.3 接入')">${esc(channel)}</button>`}
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
                      <article class="panel-card"><h2>关联频道</h2><div class="identity-grid">${row('主频道',channelCell(detail.channel))}${row('成功频道',channelCell(detail.configSummary?.interactionItem?.successChannel))}${row('失败频道',channelCell(detail.configSummary?.interactionItem?.failChannel))}${row('链路预览',chainPreview(detail))}</div><p class="muted"><button class="link-button" onclick="toast('频道详情 / 逻辑链将在后续 6.3 接入')">频道详情 / 逻辑链将在后续 6.3 接入</button></p></article>
                      <article class="panel-card"><h2>Debug 检查</h2>${debug.ok?debugChecks(debug.data):errorBlock(debug.error.message)}</article>
                      <article class="panel-card"><h2>Doctor 问题</h2>${doctorList(uniqueIssues(relatedDoctor),8)}</article>
                      <article class="panel-card"><h2>最近事件</h2>${history.ok?historyList(history.data):errorBlock(history.error.message)}</article>
                      <article class="panel-card"><h2>配置摘要</h2>${configSummary(detail)}</article>
                    </section>`);
                }
                function chainPreview(detail){if(isBlank(detail.channel))return '<span class="muted">当前设备没有主频道。</span>';return `<div class="chain-row"><strong>${esc(detail.displayName)}</strong><span class="muted">→ 主频道：${esc(detail.channel)}</span><span class="muted">→ 消费者数量将在频道详情页接入</span></div>`}
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
                function renderPlaceholder(title,message){setView(`<div class="page-head"><div><h1>${esc(title)}</h1><p>${esc(message)}</p></div></div>${empty('该模块暂未在 6.2 接入。')}`)}
                initLogin();initApp();
                """;
    }

    private static final class LoginRequest {
        String username;
        String password;
        boolean rememberMe;
    }

    private record AuthContext(String rawToken, WebAdminSession session, WebAdminUser user) {
    }
}
