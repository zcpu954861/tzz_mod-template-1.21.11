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
                "tools/tzz-test-mcp/src/tools/testbridge.ts",
                "tools/tzz-test-mcp/src/tools/webadmin.ts",
                "tools/tzz-test-mcp/src/tools/report.ts",
                "tools/tzz-test-mcp/src/tools/repo.ts",
                "tools/tzz-test-mcp/src/smoke.ts",
                "src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeRoutes.java",
                "src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeSecurityService.java"
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
        requireContains(context, "Step 2.5 Scope: Minecraft TestBridge Foundation", "context documents TestBridge foundation");
        requireContains(context, "It is default disabled", "context documents TestBridge default disabled");
        requireContains(context, "It requires loopback / localhost access", "context documents TestBridge loopback-only");
        requireContains(context, "It requires `TZZ_TESTBRIDGE_TOKEN`", "context documents TestBridge token requirement");
        requireContains(context, "`minecraft.command` is allowlisted", "context documents Minecraft command allowlist");
        requireContains(context, "`minecraft.clear_area` also enforces `maxClearVolume=4096`", "context documents clear area volume limit");
        requireContains(context, "`minecraft.use_block` invokes Minecraft's `UseBlockCallback` production path", "context documents use_block production path");
        requireContains(context, "Step 2.5 still does not automatically enter a Minecraft world", "context documents no auto world entry");
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
        requireContains(safety, "x-tzz-testbridge-token", "TestBridge token header redaction marker");
        requireContains(safety, "TZZ_TESTBRIDGE_TOKEN", "TestBridge token env redaction marker");

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

        String testbridge = read("tools/tzz-test-mcp/src/tools/testbridge.ts");
        requireContains(testbridge, "minecraft.testbridge_status", "minecraft.testbridge_status tool marker");
        requireContains(testbridge, "minecraft.players", "minecraft.players tool marker");
        requireContains(testbridge, "minecraft.command", "minecraft.command tool marker");
        requireContains(testbridge, "minecraft.set_block", "minecraft.set_block tool marker");
        requireContains(testbridge, "minecraft.clear_area", "minecraft.clear_area tool marker");
        requireContains(testbridge, "minecraft.give_item", "minecraft.give_item tool marker");
        requireContains(testbridge, "minecraft.clear_inventory", "minecraft.clear_inventory tool marker");
        requireContains(testbridge, "minecraft.set_main_hand", "minecraft.set_main_hand tool marker");
        requireContains(testbridge, "minecraft.use_block", "minecraft.use_block tool marker");
        requireContains(testbridge, "minecraft.inspect_device", "minecraft.inspect_device tool marker");
        requireContains(testbridge, "minecraft.signal_history", "minecraft.signal_history tool marker");
        requireContains(testbridge, "minecraft.doctor_issues", "minecraft.doctor_issues tool marker");
        requireContains(testbridge, "minecraft.wait_testbridge", "minecraft.wait_testbridge tool marker");
        requireContains(testbridge, "TZZ_TESTBRIDGE_TOKEN", "MCP TestBridge token env marker");
        requireContains(testbridge, "X-TZZ-TestBridge-Token", "MCP TestBridge token header marker");
        requireContains(testbridge, "ensureAllowedUrl", "MCP TestBridge localhost allowlist marker");
        requireContains(testbridge, "isDangerousCommand", "MCP dangerous command deny marker");
        requireContains(testbridge, "stop\", \"op\", \"deop\", \"ban\", \"kick\", \"whitelist\"", "MCP dangerous command denylist marker");
        requireContains(testbridge, "UseBlockCallback", "MCP use_block production-path description marker");

        String testbridgeSecurity = read("src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeSecurityService.java");
        requireContains(testbridgeSecurity, "TZZ_TESTBRIDGE_ENABLED", "server TestBridge default disabled env marker");
        requireContains(testbridgeSecurity, "TZZ_TESTBRIDGE_TOKEN", "server TestBridge token env marker");
        requireContains(testbridgeSecurity, "X-TZZ-TestBridge-Token", "server TestBridge token header marker");
        requireContains(testbridgeSecurity, "isLoopbackAddress", "server TestBridge loopback-only marker");
        requireContains(testbridgeSecurity, "MessageDigest.isEqual", "server TestBridge constant-time token compare marker");

        String testbridgeRoutes = read("src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeRoutes.java");
        requireContains(testbridgeRoutes, "/api/testbridge/status", "server TestBridge status route marker");
        requireContains(testbridgeRoutes, "COMMAND_ALLOWLIST", "server Minecraft command allowlist marker");
        requireContains(testbridgeRoutes, "COMMAND_DENYLIST", "server Minecraft dangerous command deny marker");
        requireContains(testbridgeRoutes, "MAX_CLEAR_VOLUME = 4096", "server clear_area max volume marker");
        requireContains(testbridgeRoutes, "MIN_TEST_X = -128", "server set_block test area min marker");
        requireContains(testbridgeRoutes, "MAX_TEST_X = 128", "server set_block test area max marker");
        requireContains(testbridgeRoutes, "MAX_GIVE_COUNT", "server give_item count limit marker");
        requireContains(testbridgeRoutes, "clearInventory", "server clear_inventory player-scoped marker");
        requireContains(testbridgeRoutes, "setMainHand", "server set_main_hand marker");
        requireContains(testbridgeRoutes, "UseBlockCallback.EVENT.invoker().interact", "server use_block reuses production callback marker");
        requireContains(testbridgeRoutes, "inspectDevice", "server inspect_device read-only marker");
        requireContains(testbridgeRoutes, "signalHistory", "server signal_history read-only marker");
        requireContains(testbridgeRoutes, "doctorIssues", "server doctor_issues read-only marker");
        requireContains(testbridgeRoutes, "WebAdminAuditLogger.testBridge", "server TestBridge audit marker");

        String webAdminServer = read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java");
        requireContains(webAdminServer, "path.startsWith(\"/api/testbridge/\")", "WebAdminServer TestBridge route marker");
        requireContains(webAdminServer, "testBridgeRoutes.handle", "WebAdminServer delegates TestBridge routes marker");

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
        requireContains(readme, "不做 GUI 坐标点击", "README documents no coordinate clicking");
        requireContains(readme, "Minecraft TestBridge Foundation", "README documents TestBridge foundation");
        requireContains(readme, "TZZ_TESTBRIDGE_ENABLED", "README documents TestBridge enable env");
        requireContains(readme, "TZZ_TESTBRIDGE_TOKEN", "README documents TestBridge token env");
        requireContains(readme, "TestBridge default disabled", "README documents TestBridge default disabled");
        requireContains(readme, "TestBridge loopback-only", "README documents TestBridge loopback-only");
        requireContains(readme, "TestBridge token required", "README documents TestBridge token required");
        requireContains(readme, "No token logged", "README documents no token logging");
        requireContains(readme, "minecraft.use_block", "README documents use_block tool");
        requireContains(readme, "UseBlockCallback", "README documents use_block production callback");

        String userService = read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminUserService.java");
        requireContains(userService, "changeOwnPassword", "WebAdmin self password change service marker");
        requireContains(userService, "setPassword", "WebAdmin owner set password service marker");
        requireContains(userService, "WebAdminPasswordHasher.hash", "password changes reuse PBKDF2 hasher");
        requireContains(userService, "WebAdminAuditLogger.userChanged", "password changes write audit marker");
        requireFalse(userService.contains("plainPassword"), "password service does not store plaintext password marker");

        webAdminServer = read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java");
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
