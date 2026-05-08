package com.zcpu.tzzmod.webadmin;

public final class WebAdminFrontendShell {
    private static final String ASSET_VERSION = "7.5-step2.5-realtime-sync";

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
                  <link rel="stylesheet" href="/assets/app.css?v=%s">
                </head>
                <body data-page="login">
                  <main class="login-shell" data-ui="v75">
                    <section class="login-brand-v75" aria-label="TZZ Mod WebAdmin">
                      <span class="logo-mark" data-icon="logo" aria-hidden="true"></span>
                      <h1>游戏开发编辑平台</h1>
                      <p>TZZ Mod WebAdmin 控制台</p>
                    </section>
                    <section class="login-card" data-ui="v75">
                      <div class="login-card-head">
                        <span class="login-card-icon" data-icon="settings"></span>
                        <div>
                          <h2>管理员登录</h2>
                          <p>请输入您的管理员账户信息</p>
                        </div>
                      </div>
                      <form id="login-form">
                        <label class="field-v75"><span>用户名 / 邮箱</span><input id="username" autocomplete="username" required placeholder="用户名 / 邮箱"></label>
                        <label class="field-v75"><span>密码</span><div class="password-row password-row-v75"><input id="password" type="password" autocomplete="current-password" required placeholder="密码"><button type="button" id="toggle-password" aria-label="显示或隐藏密码"><span data-icon="eye"></span></button></div></label>
                        <div class="login-options-v75">
                          <label class="check-row"><input id="remember" type="checkbox" checked> 记住我</label>
                          <button class="link-button" type="button" disabled>忘记密码？</button>
                        </div>
                        <button class="primary login-submit-v75" type="submit"><span data-icon="login"></span><span>登录</span></button>
                        <p class="message" id="message"></p>
                        <div class="security-note-v75">
                          <span data-icon="warning"></span>
                          <div><strong>安全提示</strong><p>请勿将账户信息泄露给他人，所有操作都会被记录。</p></div>
                        </div>
                      </form>
                    </section>
                  </main>
                  <script src="/assets/app.js?v=%s"></script>
                </body>
                </html>
                """.formatted(ASSET_VERSION, ASSET_VERSION);
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
                  <link rel="stylesheet" href="/assets/app.css?v=%s">
                </head>
                <body data-page="app">
                  <main class="admin-shell" data-ui="v75">
                    <aside class="sidebar" data-ui="v75">
                      <div class="sidebar-brand" data-ui="v75"><span class="logo-mark" data-icon="logo" aria-hidden="true"></span><span><strong>游戏开发编辑平台</strong><small>TZZ Mod WebAdmin</small></span></div>
                      <nav class="nav-list" data-ui="v75" aria-label="主导航">
                        <div class="nav-section">
                          <button class="nav-item" data-route="#/dashboard"><span class="nav-icon" data-icon="dashboard"></span>总览</button>
                        </div>
                        <div class="nav-section">
                          <div class="nav-section-title">事件与信号</div>
                          <button class="nav-item" data-route="#/signals"><span class="nav-icon" data-icon="signalbridge-main"></span>SignalBridge</button>
                          <button class="nav-item" data-route="#/listeners"><span class="nav-icon" data-icon="consumer-listener"></span>信号监听器</button>
                          <button class="nav-item" data-route="#/receivers"><span class="nav-icon" data-icon="receiver-main"></span>接收器</button>
                          <button class="nav-item" data-route="#/history"><span class="nav-icon" data-icon="history"></span>事件历史</button>
                          <button class="nav-item" data-route="#/doctor"><span class="nav-icon" data-icon="doctor"></span>信号诊断</button>
                        </div>
                        <div class="nav-section">
                          <div class="nav-section-title">区域控制</div>
                          <button class="nav-item" data-route="#/regions"><span class="nav-icon" data-icon="region"></span>区域管理</button>
                        </div>
                        <div class="nav-section">
                          <div class="nav-section-title">设备管理</div>
                          <button class="nav-item" data-route="#/devices"><span class="nav-icon" data-icon="device"></span>信号设备</button>
                          <button class="nav-item" data-route="#/virtual-block-devices"><span class="nav-icon" data-icon="virtual-block-device"></span>虚拟方块设备</button>
                        </div>
                        <div class="nav-section">
                          <div class="nav-section-title">动作系统</div>
                          <button class="nav-item" data-route="#/actions"><span class="nav-icon" data-icon="action"></span>动作列表</button>
                        </div>
                        <div class="nav-section">
                          <div class="nav-section-title">系统管理</div>
                          <button class="nav-item" data-route="#/users"><span class="nav-icon" data-icon="user"></span>用户与权限</button>
                          <button class="nav-item" data-route="#/settings"><span class="nav-icon" data-icon="settings"></span>系统设置</button>
                        </div>
                      </nav>
                      <button class="sidebar-collapse" type="button" disabled><span data-icon="chevronLeft"></span><span>收起侧边栏</span></button>
                    </aside>
                    <section class="workspace" data-ui="v75">
                      <header class="topbar" data-ui="v75">
                        <div class="topbar-status" data-ui="v75">
                          <span id="server-state" class="status-badge status-badge-ok"><span data-icon="server-online"></span><span>服务器状态：加载中</span></span>
                          <span id="access-mode" class="topbar-chip">访问模式：-</span>
                          <span id="realtime-state" class="topbar-chip">实时同步：未连接</span>
                          <span id="last-realtime-event" class="topbar-chip">最后事件：暂无</span>
                        </div>
                        <div class="topbar-user" data-ui="v75">
                          <span id="topbar-clock" class="topbar-clock">--:--:--</span>
                          <span id="current-user" class="topbar-user-name">用户：-</span>
                          <span id="current-role" class="role-badge">角色：-</span>
                          <button id="logout" class="wa-btn ghost" type="button"><span data-icon="logout"></span><span>退出登录</span></button>
                        </div>
                      </header>
                      <div id="toast" class="toast" hidden></div>
                      <section id="app-view" class="view-panel" data-ui="v75" aria-live="polite">
                        <div class="loading-state">正在加载 WebAdmin...</div>
                      </section>
                    </section>
                  </main>
                  <script src="/assets/app.js?v=%s"></script>
                </body>
                </html>
                """.formatted(ASSET_VERSION, ASSET_VERSION);
    }
}
