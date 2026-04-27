package com.zcpu.tzzmod.webadmin;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zcpu.tzzmod.Tzz_mod;
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
                  <title>游戏开发编辑平台 - 状态</title>
                  <link rel="stylesheet" href="/assets/app.css">
                </head>
                <body data-page="app">
                  <main class="app-shell">
                    <header class="app-header">
                      <div><span class="logo-mark">T</span><span>游戏开发编辑平台</span></div>
                      <button id="logout" class="secondary">退出登录</button>
                    </header>
                    <section class="status-grid">
                      <article class="status-card"><h2>基础状态</h2><div id="status-list" class="status-list">正在加载...</div></article>
                      <article class="status-card"><h2>WebAdmin Foundation</h2><p>WebAdmin Foundation 已启动。</p><p>设备管理、Signal 频道、Doctor、历史记录等页面将在后续阶段接入。</p></article>
                    </section>
                  </main>
                  <script src="/assets/app.js"></script>
                </body>
                </html>
                """;
    }

    private static String appCss() {
        return """
                :root{color-scheme:dark;--bg:#07111f;--panel:#0d1b2e;--panel2:#101f35;--text:#e7f7ff;--muted:#93a8b8;--cyan:#22d3ee;--cyan2:#0ea5e9;--line:#1e3a52;--danger:#fb7185}
                *{box-sizing:border-box}body{margin:0;min-height:100vh;font-family:Inter,Segoe UI,Arial,sans-serif;background:#07111f;color:var(--text);letter-spacing:0}
                .login-shell{min-height:100vh;width:min(100%,1360px);margin:0 auto;display:grid;grid-template-columns:minmax(0,1fr) 440px;gap:64px;align-items:center;justify-content:center;padding:48px clamp(24px,5vw,56px)}
                .brand-panel{min-height:420px;display:flex;flex-direction:column;justify-content:center;gap:72px}.topline,.app-header>div{display:flex;align-items:center;gap:12px;color:#c8f7ff;font-weight:700}.logo-mark{display:inline-grid;place-items:center;width:34px;height:34px;border-radius:8px;background:linear-gradient(135deg,var(--cyan),var(--cyan2));color:#04111d;font-weight:900}
                .hero-copy{max-width:760px}.hero-copy h1{font-size:64px;line-height:1.05;margin:0 0 22px}.hero-copy p{font-size:20px;color:var(--muted);margin:12px 0}.hero-copy .lead{font-size:28px;color:#fff}.tags{color:#9bf3ff!important}
                .login-card,.status-card{background:linear-gradient(180deg,var(--panel),var(--panel2));border:1px solid var(--line);border-radius:16px;box-shadow:0 24px 80px rgba(0,0,0,.35)}.login-card{padding:32px;max-width:440px;width:100%;justify-self:end}.server-pill{display:inline-flex;padding:7px 12px;border:1px solid #1f6d86;border-radius:999px;color:#9bf3ff;background:#092638;font-size:13px}.login-card h2,.status-card h2{margin:20px 0 22px;font-size:26px}
                label{display:block;margin:16px 0 8px;color:#cfe6f4;font-size:14px}input{width:100%;height:44px;border-radius:10px;border:1px solid #23445f;background:#081725;color:var(--text);padding:0 12px;font-size:15px}input:focus{outline:2px solid #1fbce2;border-color:transparent}.password-row{display:flex;gap:8px}.password-row input{flex:1}.password-row button{min-width:64px;border-radius:10px;border:1px solid #28516d;background:#112a42;color:#bfeeff}.check-row{display:flex;gap:10px;align-items:center}.check-row input{width:auto;height:auto}.primary,.secondary{height:44px;border:0;border-radius:10px;padding:0 18px;font-weight:700;cursor:pointer}.primary{width:100%;background:linear-gradient(135deg,var(--cyan),var(--cyan2));color:#04111d}.secondary{background:#12263e;color:#dff8ff;border:1px solid #284963}.secondary:disabled{opacity:.45;cursor:not-allowed}.message{min-height:20px;color:var(--danger)}.help{color:var(--muted);font-size:13px}.divider{display:flex;align-items:center;margin:20px 0;color:#6d8799}.divider:before,.divider:after{content:"";height:1px;background:#213d54;flex:1}.divider span{padding:0 10px}
                .app-shell{min-height:100vh;padding:28px clamp(20px,4vw,56px)}.app-header{height:64px;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid var(--line)}.status-grid{display:grid;grid-template-columns:minmax(0,1.2fr) minmax(320px,.8fr);gap:22px;margin-top:28px}.status-card{padding:24px}.status-list{display:grid;grid-template-columns:180px minmax(0,1fr);gap:12px 20px}.status-list .k{color:var(--muted)}.status-list .v{color:#e7f7ff;word-break:break-word}
                @media(max-width:900px){.login-shell{grid-template-columns:1fr;max-width:720px;gap:34px;align-items:start;padding:32px 20px}.brand-panel{min-height:auto;justify-content:flex-start;gap:32px}.hero-copy h1{font-size:42px}.hero-copy .lead{font-size:22px}.login-card{max-width:none;justify-self:stretch}.status-grid{grid-template-columns:1fr}.status-list{grid-template-columns:1fr}}
                """;
    }

    private static String appJs() {
        return """
                async function api(path, options={}){
                  const res=await fetch(path,{credentials:'same-origin',headers:{'Content-Type':'application/json'},...options});
                  const json=await res.json().catch(()=>({ok:false,error:{message:'响应解析失败'}}));
                  if(!res.ok||!json.ok) throw new Error(json.error?.message||'请求失败');
                  return json.data;
                }
                function row(k,v){return `<div class="k">${k}</div><div class="v">${v ?? ''}</div>`}
                async function initLogin(){
                  const form=document.getElementById('login-form'); if(!form) return;
                  document.getElementById('toggle-password').onclick=()=>{const p=document.getElementById('password');p.type=p.type==='password'?'text':'password'};
                  form.onsubmit=async e=>{e.preventDefault();const msg=document.getElementById('message');msg.textContent='正在登录...';try{await api('/api/auth/login',{method:'POST',body:JSON.stringify({username:username.value,password:password.value,rememberMe:remember.checked})});location.href='/app'}catch(err){msg.textContent=err.message}};
                }
                async function initApp(){
                  const list=document.getElementById('status-list'); if(!list) return;
                  try{
                    const me=await api('/api/auth/me'); const status=await api('/api/status');
                    list.innerHTML=[
                      row('当前服务器状态',status.server.status),
                      row('WebAdmin 运行状态',status.webAdmin.running?'运行中':'未运行'),
                      row('访问模式',status.webAdmin.accessMode),
                      row('监听地址',`${status.webAdmin.host}:${status.webAdmin.port}`),
                      row('当前用户',me.displayName),
                      row('当前角色',me.role),
                      row('Mod 版本',status.server.modVersion),
                      row('Minecraft 版本',status.server.minecraftVersion),
                      row('当前 Web session 数',status.webAdmin.sessionCount)
                    ].join('');
                  }catch(err){location.href='/login'}
                  document.getElementById('logout').onclick=async()=>{try{await api('/api/auth/logout',{method:'POST',body:'{}'});}finally{location.href='/login'}};
                }
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
