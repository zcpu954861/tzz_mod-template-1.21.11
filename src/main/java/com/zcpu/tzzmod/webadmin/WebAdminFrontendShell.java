package com.zcpu.tzzmod.webadmin;

public final class WebAdminFrontendShell {
    private WebAdminFrontendShell() {
    }

    public static String loginHtml() {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>游戏开发编辑平台 - 登录</title>
                  <link rel="icon" href="data:,">
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

    public static String appHtml() {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>游戏开发编辑平台 - WebAdmin</title>
                  <link rel="icon" href="data:,">
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
                        <button class="nav-item" data-route="#/regions"><span class="nav-icon" data-icon="region"></span>区域管理</button>
                        <button class="nav-item" data-route="#/actions"><span class="nav-icon" data-icon="action"></span>动作系统</button>
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
                          <span id="realtime-state">实时同步：未连接</span>
                          <span id="last-realtime-event">最后事件：暂无</span>
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
}
