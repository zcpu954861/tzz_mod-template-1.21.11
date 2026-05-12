package com.zcpu.tzzmod.stabilization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LocalTestMcpFoundationGuardTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path TOOL_ROOT = ROOT.resolve("tools").resolve("tzz-test-mcp");

    private LocalTestMcpFoundationGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        requireFilesExist();
        testContextBoundaries();
        testPackageAndConfig();
        testToolRegistryMarkers();
        testSafetyMarkers();
        testWebAdminPasswordUiMarkers();
        testForbiddenToolSourceMarkers();
        System.out.println("Local test MCP foundation guard checks passed.");
    }

    private static void requireFilesExist() {
        List<String> required = List.of(
                "docs/LOCAL_TEST_MCP_FOUNDATION_CURRENT_CONTEXT.md",
                "tools/tzz-test-mcp/package.json",
                "tools/tzz-test-mcp/tsconfig.json",
                "tools/tzz-test-mcp/config.example.json",
                "tools/tzz-test-mcp/README.md",
                "tools/tzz-test-mcp/src/index.ts",
                "tools/tzz-test-mcp/src/server.ts",
                "tools/tzz-test-mcp/src/config.ts",
                "tools/tzz-test-mcp/src/gradleSpawn.ts",
                "tools/tzz-test-mcp/src/safety.ts",
                "tools/tzz-test-mcp/src/tools/gradle.ts",
                "tools/tzz-test-mcp/src/tools/logs.ts",
                "tools/tzz-test-mcp/src/tools/minecraft.ts",
                "tools/tzz-test-mcp/src/tools/webadmin.ts",
                "tools/tzz-test-mcp/src/tools/report.ts",
                "tools/tzz-test-mcp/src/tools/repo.ts",
                "tools/tzz-test-mcp/src/smoke.ts"
        );
        for (String file : required) {
            requireTrue(Files.isRegularFile(ROOT.resolve(file)), "required file exists: " + file);
        }
    }

    private static void testContextBoundaries() throws IOException {
        String context = read("docs/LOCAL_TEST_MCP_FOUNDATION_CURRENT_CONTEXT.md");
        requireContains(context, "No arbitrary shell tool", "context forbids arbitrary shell");
        requireContains(context, "No git mutation tools", "context forbids git mutation");
        requireContains(context, "No Minecraft TestBridge implementation in Step 1", "context defers TestBridge");
        requireContains(context, "No Minecraft client GUI coordinate clicking", "context forbids MC GUI coordinate clicking");
        requireContains(context, "No cloud or public internet browser automation", "context forbids public internet automation");
        requireContains(context, "Step 1.5 Scope: WebAdmin Password / Test Account Foundation", "context documents password foundation");
        requireContains(context, "Step 2 Scope: Minecraft Dev Runtime Launcher Foundation", "context documents runtime launcher foundation");
        requireContains(context, "does not click Minecraft GUI coordinates", "context forbids coordinate clicking for launcher");
        requireContains(context, "WebAdmin UI exposes a current-user password change entry", "context documents WebAdmin password UI");
        requireContains(context, "not written to WebAdmin state, browser storage, logs, reports, or URLs", "context documents password UI secret boundary");
        requireContains(context, "`webadmin.login` must verify real authentication state before returning success", "context documents login auth verification");
        requireContains(context, "observes `/api/auth/login`", "context documents login request observation");
        requireContains(context, "returns `SUBMIT_NOT_TRIGGERED`", "context documents no-submit login failure");
        requireContains(context, "If the page is redirected or stuck on `/login`, it returns `AUTH_REQUIRED`", "context documents goto auth gate");
    }

    private static void testPackageAndConfig() throws IOException {
        String pkg = read("tools/tzz-test-mcp/package.json");
        requireContains(pkg, "\"name\": \"@tzz/local-test-mcp\"", "package has local MCP name");
        requireContains(pkg, "\"build\": \"node ./node_modules/typescript/bin/tsc -p tsconfig.json\"", "package exposes build script");
        requireContains(pkg, "\"start\": \"node dist/index.js\"", "package exposes start script");
        requireContains(pkg, "\"playwright\"", "package declares optional Playwright support");

        String config = read("tools/tzz-test-mcp/config.example.json");
        requireContains(config, "\"webAdminUrl\": \"http://127.0.0.1:18080/\"", "config default WebAdmin URL");
        requireContains(config, "\"reportsDir\": \"reports/mcp\"", "config report directory");
        requireContains(config, "\"allowedHosts\"", "config allowed hosts");
        requireFalse(config.toLowerCase().contains("password"), "example config contains no password");
    }

    private static void testToolRegistryMarkers() throws IOException {
        String tools = read("tools/tzz-test-mcp/src/tools/index.ts");
        requireContains(tools, "healthTool()", "health tool registered");
        requireContains(tools, "gradleRunTool()", "gradle tool registered");
        requireContains(tools, "logsTailTool()", "logs tool registered");
        requireContains(tools, "repoStatusTool()", "repo status tool registered");
        requireContains(tools, "reportWriteTool()", "report tool registered");
        requireContains(tools, "minecraftTools()", "minecraft runtime tools registered");
        requireContains(tools, "webAdminTools()", "webadmin tools registered");

        String server = read("tools/tzz-test-mcp/src/server.ts");
        requireContains(server, "tools/list", "MCP tools/list handler exists");
        requireContains(server, "tools/call", "MCP tools/call handler exists");
        requireContains(server, "initialize", "MCP initialize handler exists");
        requireContains(server, "notifications/initialized", "MCP initialized notification handled");
        requireContains(server, "appendSessionAudit", "tool calls audit to reports/mcp session log");
    }

    private static void testSafetyMarkers() throws IOException {
        String safety = read("tools/tzz-test-mcp/src/safety.ts");
        requireContains(safety, "ensureAllowedUrl", "URL allowlist helper exists");
        requireContains(safety, "allowedHosts", "URL allowlist uses configured hosts");
        requireContains(safety, "isLoopbackHost", "URL helper rejects external hosts");
        requireContains(safety, "URL host must be localhost or loopback", "non-loopback host denied marker");
        requireContains(safety, "resolveRepoOutputDir", "report output directory is repo relative");
        requireContains(safety, "ensureReportPath", "report path helper exists");
        requireContains(safety, "resolveAllowedLog", "log path allowlist helper exists");
        requireContains(safety, "redactSecrets", "secret redaction helper exists");
        requireContains(safety, "authorization", "authorization redaction marker");
        requireContains(safety, "x-tzz-webadmin-csrf", "CSRF redaction marker");

        String gradle = read("tools/tzz-test-mcp/src/tools/gradle.ts");
        String gradleSpawn = read("tools/tzz-test-mcp/src/gradleSpawn.ts");
        requireContains(gradle, "PRESETS", "Gradle presets marker");
        requireContains(gradle, "clean build", "clean build preset marker");
        requireContains(gradle, "stabilizationGuardTest", "stabilization guard preset marker");
        requireContains(gradle, "localTestMcpGuardTest", "local MCP guard preset marker");
        requireContains(gradle, "buildGradleSpawnCommand(repoRoot, gradleArgs)", "Gradle runner uses shared safe spawn helper");
        requireContains(gradleSpawn, "resolveGradleExecutable", "shared Gradle executable resolver marker");
        requireContains(gradleSpawn, "existsSync(wrapperPath)", "Gradle wrapper existence check marker");
        requireContains(gradleSpawn, "未找到 ${wrapperName}：${wrapperPath}", "missing Gradle wrapper clear error marker");
        requireContains(gradleSpawn, "[\"/d\", \"/c\", \"call\", wrapperPath, ...gradleArgs]", "Windows Gradle uses safe argv call marker");
        requireContains(gradle, "shell: false", "Gradle runner does not expose shell mode");
        requireFalse(gradle.contains("`\"${gradlew}\"`"), "Gradle runner does not pass quoted wrapper literal");
        requireFalse(gradle.contains("commandLine"), "Gradle runner does not build a command string");

        String webadmin = read("tools/tzz-test-mcp/src/tools/webadmin.ts");
        requireContains(webadmin, "ensureAllowedUrl", "WebAdmin tool checks URL allowlist");
        requireContains(webadmin, "requestfailed", "WebAdmin captures failed requests");
        requireContains(webadmin, "pageerror", "WebAdmin captures page errors");
        requireContains(webadmin, "console", "WebAdmin captures console errors");
        requireContains(webadmin, "TZZ_WEBADMIN_PASSWORD", "WebAdmin credentials read from env");
        requireContains(webadmin, "CONFIG_ERROR", "WebAdmin login checks username/password env presence");
        requireContains(webadmin, "usernameConfigured", "WebAdmin login reports username configured marker");
        requireContains(webadmin, "passwordConfigured", "WebAdmin login reports password configured marker");
        requireContains(webadmin, "probeAuth", "WebAdmin auth probe helper marker");
        requireContains(webadmin, "/api/auth/me", "WebAdmin login verifies auth through /api/auth/me");
        requireContains(webadmin, "waitForLoginHandler", "WebAdmin login waits for form handler marker");
        requireContains(webadmin, "waitForLoginResponse", "WebAdmin login observes login API response marker");
        requireContains(webadmin, "/api/auth/login", "WebAdmin login waits for auth login API marker");
        requireContains(webadmin, "loginRequestObserved", "WebAdmin login returns submit observation diagnostics");
        requireContains(webadmin, "loginResponseStatus", "WebAdmin login returns login response status diagnostics");
        requireContains(webadmin, "authMeStatus", "WebAdmin login returns auth status diagnostics");
        requireContains(webadmin, "fallbackUsed", "WebAdmin login returns fallback diagnostics");
        requireContains(webadmin, "press\", [\"Enter\"", "WebAdmin login can fall back to Enter submit");
        requireContains(webadmin, "requestSubmit", "WebAdmin login can fall back to requestSubmit");
        requireContains(webadmin, "SUBMIT_NOT_TRIGGERED", "WebAdmin login fails if submit never triggers");
        requireContains(webadmin, "BUTTON_CLICK_FAILED", "WebAdmin login reports click failure marker");
        requireContains(webadmin, "saveScreenshot(context, \"login-failed\", true)", "WebAdmin login saves failure diagnostic screenshot");
        requireContains(webadmin, "AUTH_FAILED", "WebAdmin login fails when auth verification fails");
        requireContains(webadmin, "AUTH_REQUIRED", "WebAdmin goto fails when authentication is required");
        requireContains(webadmin, "isLoginUrl(currentUrl)", "WebAdmin goto detects login page marker");
        requireContains(webadmin, "当前未登录，无法进入目标 WebAdmin route。", "WebAdmin goto auth required message marker");
        requireContains(webadmin, "SELECTOR_NOT_FOUND", "WebAdmin login selector missing marker");
        requireContains(webadmin, "BUTTON_DISABLED", "WebAdmin login disabled button marker");
        requireContains(webadmin, "webadmin.change_password", "WebAdmin current-user password tool marker");
        requireContains(webadmin, "webadmin.owner_set_password", "WebAdmin owner password reset tool marker");
        requireContains(webadmin, "/api/webadmin/users/me/password", "WebAdmin password change API marker");
        requireContains(webadmin, "/password-reset", "WebAdmin owner password reset API marker");

        String minecraft = read("tools/tzz-test-mcp/src/tools/minecraft.ts");
        requireContains(minecraft, "minecraft.start_client", "minecraft start_client tool marker");
        requireContains(minecraft, "minecraft.status", "minecraft status tool marker");
        requireContains(minecraft, "minecraft.wait_webadmin", "minecraft wait_webadmin tool marker");
        requireContains(minecraft, "minecraft.stop", "minecraft stop tool marker");
        requireContains(minecraft, "RUN_CLIENT_ARGS", "minecraft launcher uses fixed Gradle whitelist args");
        requireContains(minecraft, "\"--no-daemon\", \"runClient\"", "runClient whitelist marker");
        requireContains(minecraft, "buildGradleSpawnCommand(config.repoRoot, RUN_CLIENT_ARGS)", "minecraft launcher reuses shared Gradle spawn helper");
        requireContains(minecraft, "windowsHide: false", "minecraft client window is visible marker");
        requireContains(minecraft, "ensureAllowedUrl", "minecraft wait_webadmin uses URL allowlist");
        requireContains(minecraft, "only the runClient process started by this MCP session", "stop only managed process marker");
        requireContains(minecraft, "reportsDir, \"runtime\"", "runtime logs under reports/mcp marker");
        requireFalse(minecraft.contains("`\"${gradlew}\"`"), "minecraft launcher does not pass quoted wrapper literal");

        String readme = read("tools/tzz-test-mcp/README.md");
        requireContains(readme, "## 中文快速开始", "README has Chinese quick start");
        requireContains(readme, "启动后不会输出普通日志", "README documents stdio no stdout guidance");
        requireContains(readme, "WebAdmin 密码 / 测试账号", "README documents password/test user foundation");
        requireContains(readme, "点击“修改密码”", "README documents WebAdmin current-user password UI");
        requireContains(readme, "WebAdmin 登录排查", "README documents login troubleshooting");
        requireContains(readme, "通过 `/api/auth/me` 验证真实认证状态", "README documents auth verification");
        requireContains(readme, "观察 `/api/auth/login` 请求", "README documents login submit observation");
        requireContains(readme, "SUBMIT_NOT_TRIGGERED", "README documents no-submit login failure");
        requireContains(readme, "需要同步更新 Codex App MCP 环境变量，然后重启 Codex App / MCP server", "README documents env refresh after password change");
        requireContains(readme, "不要把真实密码", "README forbids committed plaintext passwords");
        requireContains(readme, "Minecraft Dev Runtime Launcher", "README documents runtime launcher");
        requireContains(readme, "没有外网访问", "README documents no external host");
        requireContains(readme, "不做 Minecraft TestBridge", "README documents no TestBridge");
        requireContains(readme, "不做 GUI 坐标点击", "README documents no coordinate clicking");

        String userService = read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminUserService.java");
        requireContains(userService, "changeOwnPassword", "WebAdmin self password change service marker");
        requireContains(userService, "setPassword", "WebAdmin owner set password service marker");
        requireContains(userService, "WebAdminPasswordHasher.hash", "password changes reuse PBKDF2 hasher");
        requireContains(userService, "WebAdminAuditLogger.userChanged", "password changes write audit marker");
        requireFalse(userService.contains("plainPassword"), "password service does not store plaintext password marker");

        String webAdminServer = read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java");
        requireContains(webAdminServer, "/api/webadmin/users/me/password", "self password API route marker");
        requireContains(webAdminServer, "password-reset", "owner password API route marker");
        requireContains(webAdminServer, "X-TZZ-WebAdmin-CSRF", "password APIs require CSRF marker");
        requireContains(webAdminServer, "isWriteSameOrigin", "password APIs require same-origin marker");
        requireContains(webAdminServer, "invalidateUsername", "password changes invalidate sessions marker");
    }

    private static void testWebAdminPasswordUiMarkers() throws IOException {
        String shell = read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendShell.java");
        requireContains(shell, "data-change-password-entry=\"true\"", "topbar change password entry marker");
        requireContains(shell, "id=\"change-password\"", "change password button id marker");
        requireContains(shell, "修改密码", "change password button text marker");

        String frontendScripts = read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java");
        requireContains(frontendScripts, "showChangeOwnPasswordModal", "change password modal function marker");
        requireContains(frontendScripts, "data-change-password-modal=\"true\"", "change password modal marker");
        requireContains(frontendScripts, "change-password-current", "current password field marker");
        requireContains(frontendScripts, "change-password-new", "new password field marker");
        requireContains(frontendScripts, "change-password-confirm", "confirm password field marker");
        requireContains(frontendScripts, "type=\"password\"", "password input type marker");
        requireContains(frontendScripts, "newPassword:next", "new password request payload marker");
        requireContains(frontendScripts, "confirmPassword:confirm", "confirm password request payload marker");
        requireContains(frontendScripts, "/api/webadmin/users/me/password", "current-user password API call marker");
        requireContains(frontendScripts, "'X-TZZ-WebAdmin-CSRF':csrfToken()", "change password uses CSRF helper marker");
        requireContains(frontendScripts, "appState.capabilities=await api('/api/webadmin/write/capabilities')", "change password keeps session and refreshes capabilities marker");
        requireFalse(frontendScripts.contains("localStorage.setItem('password"), "password UI does not write password to localStorage");
        requireFalse(frontendScripts.contains("localStorage.setItem(\"password"), "password UI does not write password to localStorage double-quote marker");
        requireFalse(frontendScripts.contains("sessionStorage.setItem('password"), "password UI does not write password to sessionStorage");
        requireFalse(frontendScripts.contains("sessionStorage.setItem(\"password"), "password UI does not write password to sessionStorage double-quote marker");

        String frontendStyles = read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendStyles.java");
        requireContains(frontendStyles, ".topbar-password-button", "change password topbar style marker");
        requireContains(frontendStyles, ".wa-password-modal", "change password modal style marker");
        requireContains(frontendStyles, ".wa-password-error", "change password error style marker");
    }

    private static void testForbiddenToolSourceMarkers() throws IOException {
        String source = readToolSources();
        requireFalse(source.contains("git push"), "MCP source does not expose git push");
        requireFalse(source.contains("git merge"), "MCP source does not expose git merge");
        requireFalse(source.contains("git tag"), "MCP source does not expose git tag");
        requireFalse(source.contains("git reset"), "MCP source does not expose git reset");
        requireFalse(source.contains("rm -rf"), "MCP source does not expose recursive delete");
        requireFalse(source.contains("Remove-Item"), "MCP source does not expose PowerShell deletion");
        requireFalse(source.contains("run_command"), "MCP source does not expose arbitrary run_command");
        requireFalse(source.contains("mouse_move"), "MCP source does not expose OS mouse automation");
        requireFalse(source.contains("keyboard"), "MCP source does not expose OS keyboard automation");
        requireFalse(source.contains("TestBridge"), "MCP source does not implement Minecraft TestBridge");
        requireFalse(source.contains("ConditionEngine"), "MCP source does not enter ConditionEngine");
    }

    private static String readToolSources() throws IOException {
        StringBuilder source = new StringBuilder();
        try (var stream = Files.walk(TOOL_ROOT.resolve("src"))) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                source.append(Files.readString(file, StandardCharsets.UTF_8)).append('\n');
            }
        }
        return source.toString();
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static void requireContains(String haystack, String needle, String message) {
        requireTrue(haystack.contains(needle), message);
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireFalse(boolean condition, String message) {
        requireTrue(!condition, message);
    }
}
