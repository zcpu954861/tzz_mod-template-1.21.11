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
        testReadmeStabilizationMarkers();
        testWebAdminPasswordUiMarkers();
        testForbiddenToolSourceMarkers();
        System.out.println("Local test MCP foundation guard checks passed.");
    }

    private static void requireFilesExist() {
        List<String> required = List.of(
                "docs/LOCAL_TEST_MCP_FOUNDATION_CURRENT_CONTEXT.md",
                "docs/WEBADMIN_UNIFIED_ITEM_SUBMIT_EDITOR_7_11_CURRENT_CONTEXT.md",
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
                "tools/tzz-test-mcp/src/tools/scenario.ts",
                "tools/tzz-test-mcp/src/tools/testbridge.ts",
                "tools/tzz-test-mcp/src/tools/webadmin.ts",
                "tools/tzz-test-mcp/src/tools/report.ts",
                "tools/tzz-test-mcp/src/tools/repo.ts",
                "tools/tzz-test-mcp/src/smoke.ts",
                "src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeRoutes.java",
                "src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeSecurityService.java",
                "src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeClientGuiBridge.java",
                "src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeGuiServer.java",
                "src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminTestBridgeGuiClient.java",
                "src/main/java/com/zcpu/tzzmod/network/WebAdminTestBridgeGuiPayloads.java",
                "src/main/java/com/zcpu/tzzmod/network/WebAdminTestBridgeGuiS2CPayload.java",
                "src/main/java/com/zcpu/tzzmod/network/WebAdminTestBridgeGuiC2SPayload.java"
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
        requireContains(context, "Step 3 Scope: Auto Enter Test World + Scenario Preparation Foundation", "context documents Step 3 foundation");
        requireContains(context, "`minecraft.start_client` accepts `autoEnterWorld=true` and `worldName`", "context documents start_client autoEnterWorld option");
        requireContains(context, "The launcher checks `run/saves/<worldName>/level.dat`", "context documents existing world check");
        requireContains(context, "Auto-enter appends only the fixed quick play argument `--quickPlaySingleplayer <worldName>`", "context documents fixed quick play args");
        requireContains(context, "`minecraft.wait_world`", "context documents wait_world tool");
        requireContains(context, "`minecraft.prepare_test_area`", "context documents prepare_test_area tool");
        requireContains(context, "`minecraft.prepare_test_player`", "context documents prepare_test_player tool");
        requireContains(context, "`minecraft.prepare_test_world`", "context documents prepare_test_world tool");
        requireContains(context, "Step 3 still does not:", "context documents Step 3 forbidden scope");
        requireContains(context, "Click Minecraft GUI coordinates", "context forbids GUI coordinate click in Step 3");
        requireContains(context, "Step 4 Scope: Minecraft GUI Operation Abstraction Foundation", "context documents Step 4 foundation");
        requireContains(context, "`container_template`: the 7.9 P3b container content-change template GUI", "context documents container template GUI support");
        requireContains(context, "`single_item_submit`: the 7.10 single itemSubmit template GUI", "context documents single itemSubmit GUI support");
        requireContains(context, "server sends a nonce-bound S2C GUI operation request", "context documents client-side GUI payload architecture");
        requireContains(context, "TestBridge token is never sent to the client payload", "context documents token not sent to client payload");
        requireContains(context, "`minecraft.gui_current`", "context documents gui_current tool");
        requireContains(context, "`minecraft.gui_slots`", "context documents gui_slots tool");
        requireContains(context, "`minecraft.gui_put_item`", "context documents gui_put_item tool");
        requireContains(context, "`minecraft.gui_clear_slot`", "context documents gui_clear_slot tool");
        requireContains(context, "`minecraft.gui_set_count`", "context documents gui_set_count tool");
        requireContains(context, "`minecraft.gui_save`", "context documents gui_save tool");
        requireContains(context, "`minecraft.gui_cancel`", "context documents gui_cancel tool");
        requireContains(context, "`minecraft.client_screenshot`", "context documents Minecraft client screenshot tool");
        requireContains(context, "does not use OS screenshots", "context documents no OS screenshot");
        requireContains(context, "reports/mcp/screenshots", "context documents client screenshot output path");
        requireContains(context, "Long-Term UI Screenshot Matrix Rule", "context documents long-term screenshot matrix rule");
        requireContains(context, "All new or modified Minecraft in-game UI must run a screenshot matrix", "context requires game UI screenshot matrix");
        requireContains(context, "All new or modified WebAdmin WebUI must run a WebAdmin viewport screenshot matrix", "context requires WebAdmin screenshot matrix");
        requireContains(context, "user approval is required before checkpoint", "context requires user approval before checkpoint");
        requireContains(context, "`minecraft.client_screenshot_matrix`", "context documents Minecraft screenshot matrix tool");
        requireContains(context, "`webadmin.responsive_matrix`", "context documents WebAdmin responsive matrix tool");
        requireContains(context, "deviceScaleFactor", "context documents WebAdmin responsive deviceScaleFactor marker");
        requireContains(context, "uhd_4k_150_scaled", "context documents WebAdmin 4K scaled 150 profile");
        requireContains(context, "uhd_4k_200_scaled", "context documents WebAdmin 4K scaled 200 profile");
        requireContains(context, "uhd_3840x2160_css_extreme", "context marks 3840 CSS viewport as extreme");
        requireContains(context, "expected physical screenshot size", "context requires physical screenshot size report marker");
        requireContains(context, "does not modify real player inventory or real world containers", "context documents ghost item safety");
        requireContains(context, "does not directly write `SignalDeviceData`", "context documents no raw SignalDeviceData write");
        requireContains(context, "Unsupported screens return `UNSUPPORTED_GUI`; no screen returns `GUI_NOT_OPEN`", "context documents unsupported GUI errors");
        requireContains(context, "Step 4 still does not:", "context documents Step 4 forbidden scope");
        requireContains(context, "Support arbitrary Minecraft screens", "context forbids arbitrary GUI support");
        requireContains(context, "Step 5 Scope: Scenario Test Orchestration Foundation", "context documents Step 5 foundation");
        requireContains(context, "`scenario.list`", "context documents scenario.list tool");
        requireContains(context, "`scenario.run`", "context documents scenario.run tool");
        requireContains(context, "`scenario.report`", "context documents scenario.report tool");
        requireContains(context, "`scenario.cleanup`", "context documents scenario.cleanup tool");
        requireContains(context, "`webadmin.close`", "context documents webadmin.close tool");
        requireContains(context, "reports/mcp/scenarios", "context documents scenario report path");
        requireContains(context, "The scenario step runner stops on failure", "context documents scenario stop on failure");
        requireContains(context, "only calls `SCENARIO_ALLOWED_TOOLS` / `ALLOWED_SCENARIO_TOOLS`", "context documents scenario safe tool allowlist");
        requireContains(context, "Delete reports, screenshots, logs, worlds, or repository files during cleanup", "context documents scenario cleanup no deletion");
        requireContains(context, "WebAdmin UI exposes a current-user password change entry", "context documents WebAdmin password UI");
        requireContains(context, "not written to WebAdmin state, browser storage, logs, reports, or URLs", "context documents password UI secret boundary");
        requireContains(context, "`webadmin.login` must verify real authentication state before returning success", "context documents login auth verification");
        requireContains(context, "observes `/api/auth/login`", "context documents login request observation");
        requireContains(context, "returns `SUBMIT_NOT_TRIGGERED`", "context documents no-submit login failure");
        requireContains(context, "If the page is redirected or stuck on `/login`, it returns `AUTH_REQUIRED`", "context documents goto auth gate");
    }

    private static void testReadmeStabilizationMarkers() throws IOException {
        String readme = read("README.md");
        requireContains(readme, "当前 WebAdmin / 7.x 编辑层状态", "README documents current WebAdmin capabilities");
        requireContains(readme, "WebAdmin Editing Capability Matrix 7.14", "README links 7.14 capability matrix");
        requireContains(readme, "Local Test MCP Foundation", "README documents Local Test MCP auxiliary tools");
        requireContains(readme, "手动测试仍为主", "README states manual testing remains primary");
        requireContains(readme, "MCP remains auxiliary and does not replace user acceptance", "README states MCP is auxiliary");
        requireContains(readme, "7.14 stabilization does not generate screenshots", "README states 7.14 does not generate screenshots");
        requireContains(readme, "does not run MCP scenarios", "README states 7.14 does not run MCP scenarios");
        requireContains(readme, "8.1 不启动 Minecraft", "README states 8.1 does not start Minecraft");
        requireContains(readme, "8.1 不提供 WebAdmin 条件可视化编辑器", "README states 8.1 has no condition editor");
        requireContains(readme, "8.2 不提供 WebAdmin condition editor", "README states 8.2 has no condition editor");
        requireContains(readme, "不提供状态变量 WebAdmin 页面/API", "README states 8.2 has no state variable WebAdmin page or API");
        requireContains(readme, "8.2 仍不接入 runtime", "README states 8.2 has no runtime integration");
        requireContains(readme, "8.2 不做具体任务/关卡", "README states 8.2 has no concrete tasks");
        requireContains(readme, "8.2 不新增 MCP tool", "README states 8.2 has no new MCP tool");
        requireContains(readme, "不跑 MCP scenario", "README states 8.2 does not run MCP scenarios");
        requireContains(readme, "不生成截图", "README states 8.2 does not generate screenshots");
        requireContains(readme, "不启动 Minecraft", "README states 8.2 does not start Minecraft");
        requireContains(readme, "8.3 不新增 MCP tool", "README states 8.3 has no new MCP tool");
        requireContains(readme, "8.3 不跑 MCP scenario", "README states 8.3 does not run MCP scenarios");
        requireContains(readme, "8.3 不生成截图", "README states 8.3 does not generate screenshots");
        requireContains(readme, "8.3 不启动 Minecraft", "README states 8.3 does not start Minecraft");
        requireContains(readme, "8.3 仍不接入 runtime", "README states 8.3 has no runtime integration");
        requireContains(readme, "8.3 不提供 WebAdmin condition editor", "README states 8.3 has no condition editor");
        requireContains(readme, "不提供 WebAdmin API", "README states 8.3 has no WebAdmin API");
        requireContains(readme, "不提供 WebAdmin UI", "README states 8.3 has no WebAdmin UI");
        requireContains(readme, "8.4 不新增 MCP tool", "README states 8.4 has no new MCP tool");
        requireContains(readme, "8.4 不跑 MCP scenario", "README states 8.4 does not run MCP scenarios");
        requireContains(readme, "8.4 不生成截图", "README states 8.4 does not generate screenshots");
        requireContains(readme, "8.4 不启动 Minecraft", "README states 8.4 does not start Minecraft");
        requireContains(readme, "8.4 仍不接入 runtime", "README states 8.4 has no runtime integration");
        requireContains(readme, "8.4 不提供 WebAdmin condition editor", "README states 8.4 has no condition editor");
        requireContains(readme, "8.4 不提供 WebAdmin API", "README states 8.4 has no WebAdmin API");
        requireContains(readme, "8.4 不提供 WebAdmin UI", "README states 8.4 has no WebAdmin UI");
        requireContains(readme, "8.5 WebAdmin Condition Editor", "README documents 8.5 WebAdmin condition editor");
        requireContains(readme, "8.5 仍不接入 runtime", "README states 8.5 has no runtime integration");
        requireContains(readme, "8.5 不新增 MCP tool", "README states 8.5 has no new MCP tool");
        requireContains(readme, "8.5 不跑 MCP scenario", "README states 8.5 does not run MCP scenarios");
        requireContains(readme, "8.5 不生成截图", "README states 8.5 does not generate screenshots");
        requireContains(readme, "8.5 不启动 Minecraft", "README states 8.5 does not start Minecraft");
        requireContains(readme, "MCP 不提供任意 shell", "README keeps no arbitrary shell boundary");
        requireContains(readme, "不提供 git mutation", "README keeps no git mutation boundary");
        requireContains(readme, "不提供 raw JSON / NBT path 编辑", "README keeps no raw JSON boundary");
        requireContains(readme, "8.x：ConditionEngine", "README keeps ConditionEngine as future work");
        requireContains(readme, "Logic Chain Viewer MVP", "README documents 7.15 logic chain viewer as read-only MVP");
        requireFalse(readme.contains("MCP 已完全代替手工验收"), "README must not claim MCP replaces manual testing");
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
        requireContains(tools, "scenarioTools()", "scenario tools registered");
        String lowerTools = tools.toLowerCase();
        requireFalse(lowerTools.contains("logic_chain") || lowerTools.contains("logic-chain") || tools.contains("logicChain") || tools.contains("logic-chains"),
                "7.15 logic chain viewer must not add a new MCP tool");
        requireFalse(lowerTools.contains("condition_engine") || lowerTools.contains("condition-engine") || tools.contains("conditionEngine")
                        || lowerTools.contains("condition.") || lowerTools.contains("player_condition") || lowerTools.contains("webadmin.condition")
                        || lowerTools.contains("minecraft.condition") || lowerTools.contains("state_variable") || lowerTools.contains("state-variable")
                        || tools.contains("stateVariable") || lowerTools.contains("item_condition") || lowerTools.contains("inventory_condition")
                        || lowerTools.contains("container_condition") || lowerTools.contains("condition_item") || lowerTools.contains("condition-inventory")
                        || lowerTools.contains("condition-container") || lowerTools.contains("region_condition") || lowerTools.contains("signal_condition")
                        || lowerTools.contains("logic_chain_condition") || lowerTools.contains("condition-region") || lowerTools.contains("condition-signal")
                        || lowerTools.contains("condition-logic") || lowerTools.contains("game_controller")
                        || lowerTools.contains("mission_system") || lowerTools.contains("phase_controller"),
                "8.4 ConditionEngine region/signal/logic chain conditions must not add MCP tools for conditions, state variables, or high-level game systems");
        String scenarios = read("tools/tzz-test-mcp/src/tools/scenario.ts");
        String lowerScenarios = scenarios.toLowerCase();
        requireFalse(lowerScenarios.contains("logic_chain") || lowerScenarios.contains("logic-chain") || scenarios.contains("logicChain") || scenarios.contains("logic-chains"),
                "7.15 logic chain viewer must not add an MCP scenario");
        requireFalse(lowerScenarios.contains("condition_engine") || lowerScenarios.contains("condition-engine") || scenarios.contains("conditionEngine")
                        || lowerScenarios.contains("player_condition") || lowerScenarios.contains("webadmin.condition")
                        || lowerScenarios.contains("minecraft.condition") || lowerScenarios.contains("state_variable") || lowerScenarios.contains("state-variable")
                        || scenarios.contains("stateVariable") || lowerScenarios.contains("item_condition") || lowerScenarios.contains("inventory_condition")
                        || lowerScenarios.contains("container_condition") || lowerScenarios.contains("condition_item") || lowerScenarios.contains("condition-inventory")
                        || lowerScenarios.contains("condition-container") || lowerScenarios.contains("region_condition") || lowerScenarios.contains("signal_condition")
                        || lowerScenarios.contains("logic_chain_condition") || lowerScenarios.contains("condition-region") || lowerScenarios.contains("condition-signal")
                        || lowerScenarios.contains("condition-logic") || lowerScenarios.contains("game_controller")
                        || lowerScenarios.contains("mission_system") || lowerScenarios.contains("phase_controller"),
                "8.4 ConditionEngine region/signal/logic chain conditions must not add MCP scenarios");

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
        requireContains(safety, "ensureScenarioReportPath", "scenario report path helper exists");
        requireContains(safety, "scenarioReportsDir", "scenario report directory helper exists");
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
        requireContains(webadmin, "deviceScaleFactor", "WebAdmin responsive profile includes deviceScaleFactor marker");
        requireContains(webadmin, "uhd_4k_150_scaled", "WebAdmin responsive includes 4K 150 scaled profile");
        requireContains(webadmin, "uhd_4k_200_scaled", "WebAdmin responsive includes 4K 200 scaled profile");
        requireContains(webadmin, "uhd_3840x2160_css_extreme", "WebAdmin responsive marks 3840 CSS viewport as extreme");
        requireContains(webadmin, "expectedPhysicalWidth", "WebAdmin responsive report includes expected physical width marker");
        requireContains(webadmin, "screenshotActualWidth", "WebAdmin responsive report includes actual screenshot size marker");
        requireContains(webadmin, "readPngDimensions", "WebAdmin responsive reads PNG dimensions marker");
        requireContains(webadmin, "storageState", "WebAdmin responsive preserves storageState across deviceScaleFactor context rebuilds");
        requireContains(webadmin, "webadmin.close", "WebAdmin close tool marker");
        requireContains(webadmin, "webadmin.set_viewport", "WebAdmin set_viewport tool marker");
        requireContains(webadmin, "webadmin.responsive_screenshot", "WebAdmin responsive_screenshot tool marker");
        requireContains(webadmin, "webadmin.responsive_matrix", "WebAdmin responsive_matrix tool marker");
        requireContains(webadmin, "setViewportSize", "WebAdmin responsive tools use Playwright viewport marker");
        requireContains(webadmin, "DEFAULT_WEBADMIN_VIEWPORTS", "WebAdmin default responsive viewport marker");
        requireContains(webadmin, "manualVisualReviewRequired", "WebAdmin responsive matrix manual review marker");
        requireContains(webadmin, "ensureResponsiveReportPath", "WebAdmin responsive report path marker");
        requireContains(webadmin, "closeBrowser(context)", "WebAdmin close uses browser cleanup marker");
        requireContains(webadmin, "baseUrl = undefined", "WebAdmin close clears base URL marker");
        requireContains(webadmin, "/api/webadmin/users/me/password", "WebAdmin password change API marker");
        requireContains(webadmin, "/password-reset", "WebAdmin owner password reset API marker");

        String minecraft = read("tools/tzz-test-mcp/src/tools/minecraft.ts");
        requireContains(minecraft, "minecraft.start_client", "minecraft start_client tool marker");
        requireContains(minecraft, "minecraft.status", "minecraft status tool marker");
        requireContains(minecraft, "minecraft.wait_webadmin", "minecraft wait_webadmin tool marker");
        requireContains(minecraft, "minecraft.stop", "minecraft stop tool marker");
        requireContains(minecraft, "RUN_CLIENT_ARGS", "minecraft launcher uses fixed Gradle whitelist args");
        requireContains(minecraft, "\"--no-daemon\", \"runClient\"", "runClient whitelist marker");
        requireContains(minecraft, "autoEnterWorld", "minecraft start_client supports autoEnterWorld option marker");
        requireContains(minecraft, "worldName", "minecraft start_client supports worldName marker");
        requireContains(minecraft, "QUICK_PLAY_SINGLEPLAYER_FLAG", "minecraft quick play fixed flag marker");
        requireContains(minecraft, "\"--quickPlaySingleplayer\"", "minecraft quick play singleplayer marker");
        requireContains(minecraft, "TZZ_TEST_WORLD_NAME", "minecraft test world env marker");
        requireContains(minecraft, "DEFAULT_TEST_WORLD_NAME", "minecraft default test world marker");
        requireContains(minecraft, "\"run\", \"saves\", worldName, \"level.dat\"", "minecraft existing world level.dat check marker");
        requireContains(minecraft, "path.isAbsolute(worldName)", "worldName absolute path rejection marker");
        requireContains(minecraft, "worldName.includes(\"..\")", "worldName traversal rejection marker");
        requireContains(minecraft, "worldName 只能包含英文字母、数字、点、下划线和短横线", "worldName safe charset marker");
        requireContains(minecraft, "`--args=${QUICK_PLAY_SINGLEPLAYER_FLAG} ${worldName}`", "minecraft fixed Gradle --args marker");
        requireContains(minecraft, "buildGradleSpawnCommand(config.repoRoot, launch.gradleArgs)", "minecraft launcher reuses shared Gradle spawn helper");
        requireContains(minecraft, "windowsHide: false", "minecraft client window is visible marker");
        requireContains(minecraft, "ensureAllowedUrl", "minecraft wait_webadmin uses URL allowlist");
        requireContains(minecraft, "only the runClient process started by this MCP session", "stop only managed process marker");
        requireContains(minecraft, "WINDOWS_MANAGED_PROCESS_TREE_STOP", "Windows managed process tree stop marker");
        requireContains(minecraft, "\"taskkill.exe\"", "Windows taskkill fixed command marker");
        requireContains(minecraft, "[\"/pid\", pidText, \"/t\", \"/f\"]", "Windows taskkill uses managed pid argv marker");
        requireContains(minecraft, "runtime.pid", "stop fallback uses recorded managed pid marker");
        requireContains(minecraft, "WINDOWS_MANAGED_RUNCLIENT_PROCESS_QUERY", "Windows fixed runClient process query marker");
        requireContains(minecraft, "\"powershell.exe\"", "Windows process query uses fixed PowerShell executable marker");
        requireContains(minecraft, "$name -ne 'java.exe' -and $name -ne 'java'", "Windows process query only matches Java processes marker");
        requireContains(minecraft, "gradle-wrapper.jar", "Windows process query limited to Gradle wrapper runClient marker");
        requireContains(minecraft, "devlaunchinjector.Main", "Windows process query limited to Minecraft dev client marker");
        requireContains(minecraft, "config.repoRoot", "Windows process query filters by repo root marker");
        requireContains(minecraft, "runtime.worldName", "Windows process query filters by managed world marker");
        requireContains(minecraft, "waitForNoWindowsManagedRunClientProcesses", "Windows stop waits for managed runClient processes to disappear marker");
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
        requireContains(testbridge, "minecraft.wait_world", "minecraft.wait_world tool marker");
        requireContains(testbridge, "minecraft.prepare_test_area", "minecraft.prepare_test_area tool marker");
        requireContains(testbridge, "minecraft.prepare_test_player", "minecraft.prepare_test_player tool marker");
        requireContains(testbridge, "minecraft.prepare_test_world", "minecraft.prepare_test_world tool marker");
        requireContains(testbridge, "minecraft.wait_testbridge", "minecraft.wait_testbridge tool marker");
        requireContains(testbridge, "minecraft.gui_current", "minecraft.gui_current tool marker");
        requireContains(testbridge, "minecraft.gui_slots", "minecraft.gui_slots tool marker");
        requireContains(testbridge, "minecraft.gui_put_item", "minecraft.gui_put_item tool marker");
        requireContains(testbridge, "minecraft.gui_clear_slot", "minecraft.gui_clear_slot tool marker");
        requireContains(testbridge, "minecraft.gui_set_count", "minecraft.gui_set_count tool marker");
        requireContains(testbridge, "minecraft.gui_select_requirement", "minecraft.gui_select_requirement tool marker");
        requireContains(testbridge, "minecraft.gui_add_requirement", "minecraft.gui_add_requirement tool marker");
        requireContains(testbridge, "minecraft.gui_delete_requirement", "minecraft.gui_delete_requirement tool marker");
        requireContains(testbridge, "minecraft.gui_set_count_mode", "minecraft.gui_set_count_mode tool marker");
        requireContains(testbridge, "minecraft.gui_set_requirement_enabled", "minecraft.gui_set_requirement_enabled tool marker");
        requireContains(testbridge, "minecraft.gui_set_matcher_options", "minecraft.gui_set_matcher_options tool marker");
        requireContains(testbridge, "minecraft.gui_set_consume", "minecraft.gui_set_consume tool marker");
        requireContains(testbridge, "minecraft.gui_set_global", "minecraft.gui_set_global tool marker");
        requireContains(testbridge, "minecraft.gui_save", "minecraft.gui_save tool marker");
        requireContains(testbridge, "minecraft.gui_cancel", "minecraft.gui_cancel tool marker");
        requireContains(testbridge, "minecraft.client_screenshot", "minecraft.client_screenshot tool marker");
        requireContains(testbridge, "minecraft.client_set_window_size", "minecraft.client_set_window_size tool marker");
        requireContains(testbridge, "minecraft.client_set_gui_scale", "minecraft.client_set_gui_scale tool marker");
        requireContains(testbridge, "minecraft.client_screenshot_matrix", "minecraft.client_screenshot_matrix tool marker");
        requireContains(testbridge, "client/window-size", "MCP client window size endpoint marker");
        requireContains(testbridge, "client/gui-scale", "MCP client GUI scale endpoint marker");
        requireContains(testbridge, "DEFAULT_MINECRAFT_MATRIX_SIZES", "MCP Minecraft default matrix size marker");
        requireContains(testbridge, "DEFAULT_GUI_SCALES", "MCP Minecraft default GUI scale marker");
        requireContains(testbridge, "manualVisualReviewRequired", "MCP screenshot matrix manual review marker");
        requireContains(testbridge, "userApprovalRequiredBeforeCheckpoint", "MCP screenshot matrix user approval marker");
        requireContains(testbridge, "client/screenshot", "MCP client screenshot endpoint marker");
        requireContains(testbridge, "safeName", "MCP client screenshot name sanitized marker");
        requireContains(testbridge, "noOsScreenshot", "MCP client screenshot no OS screenshot marker");
        requireContains(testbridge, "noCoordinateClicking", "MCP client screenshot no coordinate clicking marker");
        requireContains(testbridge, "reportCanReferenceScreenshotPath", "MCP report can reference client screenshot path marker");
        requireContains(testbridge, "gui/current", "MCP gui_current endpoint marker");
        requireContains(testbridge, "gui/slots", "MCP gui_slots endpoint marker");
        requireContains(testbridge, "gui/put-item", "MCP gui_put_item endpoint marker");
        requireContains(testbridge, "gui/clear-slot", "MCP gui_clear_slot endpoint marker");
        requireContains(testbridge, "gui/set-count", "MCP gui_set_count endpoint marker");
        requireContains(testbridge, "gui/select-requirement", "MCP gui_select_requirement endpoint marker");
        requireContains(testbridge, "gui/add-requirement", "MCP gui_add_requirement endpoint marker");
        requireContains(testbridge, "gui/delete-requirement", "MCP gui_delete_requirement endpoint marker");
        requireContains(testbridge, "gui/set-count-mode", "MCP gui_set_count_mode endpoint marker");
        requireContains(testbridge, "gui/set-requirement-enabled", "MCP gui_set_requirement_enabled endpoint marker");
        requireContains(testbridge, "gui/set-matcher-options", "MCP gui_set_matcher_options endpoint marker");
        requireContains(testbridge, "gui/set-consume", "MCP gui_set_consume endpoint marker");
        requireContains(testbridge, "gui/set-global", "MCP gui_set_global endpoint marker");
        requireContains(testbridge, "requireRequirementSlot", "MCP 7.11 row-level GUI tools require explicit slot marker");
        requireContains(testbridge, "requires slot or slotIndex", "MCP 7.11 row-level GUI tools validation marker");
        requireContains(testbridge, "gui/save", "MCP gui_save endpoint marker");
        requireContains(testbridge, "gui/cancel", "MCP gui_cancel endpoint marker");
        requireContains(testbridge, "This does not modify real player inventory", "MCP gui_put_item real inventory safety marker");
        requireContains(testbridge, "TZZ_TESTBRIDGE_TOKEN", "MCP TestBridge token env marker");
        requireContains(testbridge, "X-TZZ-TestBridge-Token", "MCP TestBridge token header marker");
        requireContains(testbridge, "ensureAllowedUrl", "MCP TestBridge localhost allowlist marker");
        requireContains(testbridge, "isDangerousCommand", "MCP dangerous command deny marker");
        requireContains(testbridge, "stop\", \"op\", \"deop\", \"ban\", \"kick\", \"whitelist\"", "MCP dangerous command denylist marker");
        requireContains(testbridge, "UseBlockCallback", "MCP use_block production-path description marker");
        requireContains(testbridge, "world/prepare-area", "MCP prepare_test_area endpoint marker");
        requireContains(testbridge, "world/prepare-player", "MCP prepare_test_player endpoint marker");
        requireContains(testbridge, "world/prepare", "MCP prepare_test_world endpoint marker");
        requireContains(testbridge, "Idempotently prepare", "MCP prepare_test_world idempotent marker");

        String scenario = read("tools/tzz-test-mcp/src/tools/scenario.ts");
        requireContains(scenario, "scenario.list", "scenario.list tool marker");
        requireContains(scenario, "scenario.run", "scenario.run tool marker");
        requireContains(scenario, "scenario.report", "scenario.report tool marker");
        requireContains(scenario, "scenario.cleanup", "scenario.cleanup tool marker");
        requireContains(scenario, "basic_environment", "basic environment scenario marker");
        requireContains(scenario, "vbd_right_click", "vbd right click scenario marker");
        requireContains(scenario, "single_item_submit_basic", "single itemSubmit scenario marker");
        requireContains(scenario, "container_template_basic", "container template scenario marker");
        requireContains(scenario, "ALLOWED_SCENARIO_TOOLS", "scenario uses explicit safe tool allowlist marker");
        requireContains(scenario, "scenarioFailure", "scenario step runner stops on failure marker");
        requireContains(scenario, "ensureScenarioReportPath", "scenario report path marker");
        requireContains(scenario, "reports/mcp/scenarios", "scenario report directory marker");
        requireContains(scenario, "webadmin.close", "scenario cleanup closes webadmin marker");
        requireContains(scenario, "minecraft.stop", "scenario cleanup stops managed client marker");
        requireContains(scenario, "fixed_template_session_start", "scenario uses fixed template session start marker");
        requireContains(scenario, "virtual_block_device_single_item_submit", "scenario single itemSubmit lock target marker");
        requireContains(scenario, "virtual_block_device_container_template", "scenario container template lock target marker");
        requireContains(scenario, "X-TZZ-WebAdmin-CSRF", "scenario fixed session start uses CSRF marker");
        requireContains(scenario, "noMinecraftGuiCoordinateClicking", "scenario no coordinate clicking marker");
        requireContains(scenario, "noArbitraryShell", "scenario no arbitrary shell marker");
        requireContains(scenario, "noGitMutation", "scenario no git mutation marker");
        requireContains(scenario, "noExternalHost", "scenario no external host marker");

        String testbridgeSecurity = read("src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeSecurityService.java");
        requireContains(testbridgeSecurity, "TZZ_TESTBRIDGE_ENABLED", "server TestBridge default disabled env marker");
        requireContains(testbridgeSecurity, "TZZ_TESTBRIDGE_TOKEN", "server TestBridge token env marker");
        requireContains(testbridgeSecurity, "X-TZZ-TestBridge-Token", "server TestBridge token header marker");
        requireContains(testbridgeSecurity, "isLoopbackAddress", "server TestBridge loopback-only marker");
        requireContains(testbridgeSecurity, "MessageDigest.isEqual", "server TestBridge constant-time token compare marker");

        String testbridgeRoutes = read("src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeRoutes.java");
        requireContains(testbridgeRoutes, "/api/testbridge/status", "server TestBridge status route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/world/prepare-area", "server prepare_test_area route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/world/prepare-player", "server prepare_test_player route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/world/prepare", "server prepare_test_world route marker");
        requireContains(testbridgeRoutes, "COMMAND_ALLOWLIST", "server Minecraft command allowlist marker");
        requireContains(testbridgeRoutes, "raw vanilla write commands stay denied", "server raw vanilla write commands denied marker");
        requireContains(testbridgeRoutes, "COMMAND_DENYLIST", "server Minecraft dangerous command deny marker");
        requireContains(testbridgeRoutes, "MAX_CLEAR_VOLUME = 4096", "server clear_area max volume marker");
        requireContains(testbridgeRoutes, "DEFAULT_PREPARE_MIN", "server default prepare area marker");
        requireContains(testbridgeRoutes, "prepareArea", "server prepare_test_area implementation marker");
        requireContains(testbridgeRoutes, "preparePlayer", "server prepare_test_player implementation marker");
        requireContains(testbridgeRoutes, "prepareWorld", "server prepare_test_world implementation marker");
        requireContains(testbridgeRoutes, "prepare_test_world", "server prepare_test_world audit marker");
        requireContains(testbridgeRoutes, "player.teleport", "server prepare player teleport marker");
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
        requireContains(testbridgeRoutes, "/api/testbridge/gui/current", "server gui_current route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/slots", "server gui_slots route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/put-item", "server gui_put_item route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/clear-slot", "server gui_clear_slot route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/set-count", "server gui_set_count route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/select-requirement", "server gui_select_requirement route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/add-requirement", "server gui_add_requirement route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/delete-requirement", "server gui_delete_requirement route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/set-count-mode", "server gui_set_count_mode route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/set-requirement-enabled", "server gui_set_requirement_enabled route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/set-matcher-options", "server gui_set_matcher_options route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/set-consume", "server gui_set_consume route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/set-global", "server gui_set_global route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/save", "server gui_save route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/gui/cancel", "server gui_cancel route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/client/screenshot", "server client screenshot route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/client/window-size", "server client window size route marker");
        requireContains(testbridgeRoutes, "/api/testbridge/client/gui-scale", "server client GUI scale route marker");
        requireContains(testbridgeRoutes, "client_screenshot", "server client screenshot payload operation marker");
        requireContains(testbridgeRoutes, "client_set_window_size", "server client window size payload operation marker");
        requireContains(testbridgeRoutes, "client_set_gui_scale", "server client GUI scale payload operation marker");
        requireContains(testbridgeRoutes, "screenshotsDir", "server client screenshot output directory marker");
        requireContains(testbridgeRoutes, "reportsMcpScreenshotsOutputOnly", "server screenshot output restricted marker");
        requireContains(testbridgeRoutes, "tokenInClientPayload", "server screenshot token not in client payload marker");
        requireContains(testbridgeRoutes, "WebAdminTestBridgeClientGuiBridge.request", "server gui route uses client screen bridge marker");
        requireContains(testbridgeRoutes, "rawSignalDeviceDataWrite", "server gui route raw SignalDeviceData write false marker");

        String webAdminServer = read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java");
        requireContains(webAdminServer, "path.startsWith(\"/api/testbridge/gui/\")", "WebAdminServer direct GUI TestBridge route marker");
        requireContains(webAdminServer, "path.startsWith(\"/api/testbridge/client/\")", "WebAdminServer direct client TestBridge route marker");
        requireContains(webAdminServer, "path.startsWith(\"/api/testbridge/\")", "WebAdminServer TestBridge route marker");
        requireContains(webAdminServer, "testBridgeRoutes.handle", "WebAdminServer delegates TestBridge routes marker");

        String guiBridge = read("src/main/java/com/zcpu/tzzmod/webadmin/testbridge/WebAdminTestBridgeClientGuiBridge.java");
        requireContains(guiBridge, "WebAdminTestBridgeGuiS2CPayload", "server GUI bridge sends S2C marker");
        requireContains(guiBridge, "WebAdminTestBridgeGuiC2SPayload", "server GUI bridge receives C2S marker");
        requireContains(guiBridge, "SESSION_DENIED", "server GUI bridge nonce/player validation marker");
        requireContains(guiBridge, "CLIENT_TIMEOUT", "server GUI bridge timeout marker");
        requireContains(guiBridge, "put_item\", \"clear_slot\", \"set_count\"", "server GUI bridge base template operations marker");
        requireContains(guiBridge, "\"save\", \"cancel\"", "server GUI bridge save/cancel operations marker");
        requireContains(guiBridge, "add_requirement", "server GUI bridge add requirement operation marker");
        requireContains(guiBridge, "delete_requirement", "server GUI bridge delete requirement operation marker");
        requireContains(guiBridge, "set_matcher_options", "server GUI bridge matcher options operation marker");
        requireContains(guiBridge, "client_screenshot", "server GUI bridge client screenshot operation marker");
        requireContains(guiBridge, "client_set_window_size", "server GUI bridge client window size operation marker");
        requireContains(guiBridge, "client_set_gui_scale", "server GUI bridge client GUI scale operation marker");

        String guiClient = read("src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminTestBridgeGuiClient.java");
        requireContains(guiClient, "client.currentScreen", "client GUI bridge reads current screen marker");
        requireContains(guiClient, "WebAdminContainerTemplatePreviewScreen", "client GUI bridge supports container template marker");
        requireContains(guiClient, "WebAdminSingleItemSubmitTemplateScreen", "client GUI bridge supports single itemSubmit marker");
        requireContains(guiClient, "UNSUPPORTED_GUI", "client GUI bridge unsupported GUI marker");
        requireContains(guiClient, "GUI_NOT_OPEN", "client GUI bridge no GUI marker");
        requireContains(guiClient, "usesMinecraftClientFramebuffer", "client screenshot uses Minecraft client framebuffer marker");
        requireContains(guiClient, "usesOsScreenshot", "client screenshot no OS screenshot marker");
        requireContains(guiClient, "usesCoordinateClicking", "client screenshot no coordinate clicking marker");
        requireContains(guiClient, "usesClientScreenshotPayload", "client screenshot payload marker");
        requireContains(guiClient, "clientScreenshotOutputRestrictedToReportsMcpScreenshots", "client screenshot output restricted marker");
        requireContains(guiClient, "setWindowedSize", "client window size payload uses Minecraft window API marker");
        requireContains(guiClient, "getGuiScale", "client GUI scale payload uses Minecraft options marker");
        requireContains(guiClient, "doesNotWriteOptionsFile", "client GUI scale does not write options marker");

        String containerScreen = read("src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminContainerTemplatePreviewScreen.java");
        requireContains(containerScreen, "testBridgePutItem", "container GUI testbridge put item marker");
        requireContains(containerScreen, "testBridgeClearSlot", "container GUI testbridge clear slot marker");
        requireContains(containerScreen, "testBridgeSetCount", "container GUI testbridge set count marker");
        requireContains(containerScreen, "testBridgeSave", "container GUI testbridge save marker");
        requireContains(containerScreen, "testBridgeCancel", "container GUI testbridge cancel marker");
        requireContains(containerScreen, "requestSave();", "container GUI testbridge save uses existing save path marker");
        requireContains(containerScreen, "requestCancel", "container GUI testbridge cancel uses existing cancel path marker");
        requireContains(containerScreen, "realInventoryModified", "container GUI testbridge no real inventory modification marker");

        String singleSubmitScreen = read("src/main/java/com/zcpu/tzzmod/client/webadmin/WebAdminSingleItemSubmitTemplateScreen.java");
        requireContains(singleSubmitScreen, "testBridgePutItem", "single itemSubmit GUI testbridge put item marker");
        requireContains(singleSubmitScreen, "testBridgeClearSlot", "single itemSubmit GUI testbridge clear slot marker");
        requireContains(singleSubmitScreen, "testBridgeSetCount", "single itemSubmit GUI testbridge set count marker");
        requireContains(singleSubmitScreen, "testBridgeAddRequirement", "unified itemSubmit GUI testbridge add requirement marker");
        requireContains(singleSubmitScreen, "testBridgeDeleteRequirement", "unified itemSubmit GUI testbridge delete requirement marker");
        requireContains(singleSubmitScreen, "testBridgeSetCountMode", "unified itemSubmit GUI testbridge count mode marker");
        requireContains(singleSubmitScreen, "testBridgeSetRequirementEnabled", "unified itemSubmit GUI testbridge requirement enabled marker");
        requireContains(singleSubmitScreen, "testBridgeSetMatcherOptions", "unified itemSubmit GUI testbridge matcher options marker");
        requireContains(singleSubmitScreen, "testBridgeSetConsume", "unified itemSubmit GUI testbridge consume marker");
        requireContains(singleSubmitScreen, "testBridgeSetGlobal", "unified itemSubmit GUI testbridge global marker");
        requireContains(singleSubmitScreen, "data-item-submit-adaptive-zero-one-many", "unified itemSubmit adaptive 0/1/N UI marker");
        requireContains(singleSubmitScreen, "SINGLE_REQUIREMENT_DELETE_DENIED", "unified itemSubmit unique requirement delete denied marker");
        requireContains(singleSubmitScreen, "testBridgeSave", "single itemSubmit GUI testbridge save marker");
        requireContains(singleSubmitScreen, "testBridgeCancel", "single itemSubmit GUI testbridge cancel marker");
        requireContains(singleSubmitScreen, "requestSave();", "single itemSubmit GUI testbridge save uses existing save path marker");
        requireContains(singleSubmitScreen, "requestCancel", "single itemSubmit GUI testbridge cancel uses existing cancel path marker");
        requireContains(singleSubmitScreen, "realInventoryModified", "single itemSubmit GUI testbridge no real inventory modification marker");

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
        requireContains(readme, "TZZ_TEST_WORLD_NAME", "README documents test world env marker");
        requireContains(readme, "autoEnterWorld=true", "README documents autoEnterWorld marker");
        requireContains(readme, "--quickPlaySingleplayer TZZ_MCP_TEST_WORLD", "README documents fixed quick play marker");
        requireContains(readme, "run/saves/<worldName>/level.dat", "README documents existing world check marker");
        requireContains(readme, "minecraft.wait_world", "README documents wait_world marker");
        requireContains(readme, "minecraft.prepare_test_area", "README documents prepare_test_area marker");
        requireContains(readme, "minecraft.prepare_test_player", "README documents prepare_test_player marker");
        requireContains(readme, "minecraft.prepare_test_world", "README documents prepare_test_world marker");
        requireContains(readme, "Auto Enter Test World / Prepare Tools", "README documents Step 3 section marker");
        requireContains(readme, "不会点击 Minecraft 主菜单坐标", "README documents no GUI coordinate click for auto-enter");
        requireContains(readme, "Minecraft GUI Operation Abstraction Foundation", "README documents Step 4 GUI abstraction section");
        requireContains(readme, "minecraft.gui_current", "README documents gui_current tool");
        requireContains(readme, "minecraft.gui_slots", "README documents gui_slots tool");
        requireContains(readme, "minecraft.gui_put_item", "README documents gui_put_item tool");
        requireContains(readme, "minecraft.gui_clear_slot", "README documents gui_clear_slot tool");
        requireContains(readme, "minecraft.gui_set_count", "README documents gui_set_count tool");
        requireContains(readme, "minecraft.gui_add_requirement", "README documents gui_add_requirement tool");
        requireContains(readme, "minecraft.gui_delete_requirement", "README documents gui_delete_requirement tool");
        requireContains(readme, "minecraft.gui_set_matcher_options", "README documents gui_set_matcher_options tool");
        requireContains(readme, "7.11_unified_item_submit_logic_test", "README documents 7.11 unified itemSubmit logic test");
        requireContains(readme, "partial_match_no_consume", "README documents partial match no consume logic test");
        requireContains(readme, "full_match_consumes_all", "README documents full match staged consume logic test");
        requireContains(readme, "minecraft.gui_save", "README documents gui_save tool");
        requireContains(readme, "minecraft.gui_cancel", "README documents gui_cancel tool");
        requireContains(readme, "minecraft.client_screenshot", "README documents Minecraft client screenshot tool");
        requireContains(readme, "webadmin.set_viewport", "README documents WebAdmin set_viewport tool");
        requireContains(readme, "webadmin.responsive_screenshot", "README documents WebAdmin responsive_screenshot tool");
        requireContains(readme, "webadmin.responsive_matrix", "README documents WebAdmin responsive_matrix tool");
        requireContains(readme, "minecraft.client_set_window_size", "README documents Minecraft window size tool");
        requireContains(readme, "minecraft.client_set_gui_scale", "README documents Minecraft GUI scale tool");
        requireContains(readme, "minecraft.client_screenshot_matrix", "README documents Minecraft screenshot matrix tool");
        requireContains(readme, "响应式 / 分辨率截图矩阵长期规则", "README documents long-term responsive matrix rule");
        requireContains(readme, "以后新增或修改任何 Minecraft 游戏内 UI", "README requires game UI screenshot matrix");
        requireContains(readme, "以后新增或修改任何 WebAdmin WebUI", "README requires WebAdmin screenshot matrix");
        requireContains(readme, "deviceScaleFactor", "README documents WebAdmin DPI scaling profile marker");
        requireContains(readme, "CSS viewport", "README documents CSS viewport marker");
        requireContains(readme, "physical screenshot size", "README documents physical screenshot size marker");
        requireContains(readme, "uhd_4k_150_scaled", "README documents WebAdmin 4K 150 scaled profile");
        requireContains(readme, "uhd_4k_200_scaled", "README documents WebAdmin 4K 200 scaled profile");
        requireContains(readme, "uhd_3840x2160_css_extreme", "README marks 3840 CSS viewport as extreme");
        requireContains(readme, "用户确认前不得 checkpoint", "README requires user approval before checkpoint");
        requireContains(readme, "明显问题预检", "README documents screenshot obvious issue precheck");
        requireContains(readme, "needs_user_review", "README documents screenshot needs_user_review status");
        requireContains(readme, "reports/mcp/responsive", "README documents responsive report path");
        requireContains(readme, "不是 OS 截屏", "README documents no OS screenshot");
        requireContains(readme, "reports/mcp/screenshots", "README documents client screenshot output path");
        requireContains(readme, "不改真实玩家背包", "README documents GUI real inventory safety");
        requireContains(readme, "不直接写 `SignalDeviceData` JSON", "README documents no raw SignalDeviceData write");
        requireContains(readme, "UNSUPPORTED_GUI", "README documents unsupported GUI error");
        requireContains(readme, "GUI_NOT_OPEN", "README documents no GUI error");
        requireContains(readme, "Scenario Test Orchestration Foundation", "README documents Step 5 scenario foundation");
        requireContains(readme, "scenario.list", "README documents scenario.list");
        requireContains(readme, "scenario.run", "README documents scenario.run");
        requireContains(readme, "scenario.report", "README documents scenario.report");
        requireContains(readme, "scenario.cleanup", "README documents scenario.cleanup");
        requireContains(readme, "webadmin.close", "README documents webadmin.close");
        requireContains(readme, "reports/mcp/scenarios", "README documents scenario report path");
        requireContains(readme, "basic_environment", "README documents basic environment scenario");
        requireContains(readme, "vbd_right_click", "README documents VBD right click scenario");
        requireContains(readme, "single_item_submit_basic", "README documents single itemSubmit scenario");
        requireContains(readme, "container_template_basic", "README documents container template scenario");
        requireContains(readme, "场景失败时会停止当前场景步骤", "README documents scenario stop on failure");
        requireContains(readme, "只调用 `minecraft.stop` 和 `webadmin.close`", "README documents cleanup tools");

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
        requireFalse(source.contains("page.mouse"), "MCP source does not expose Playwright mouse automation");
        requireFalse(source.contains("mouse.click"), "MCP source does not expose mouse coordinate clicking");
        requireFalse(source.contains("SendInput"), "MCP source does not expose OS SendInput automation");
        requireFalse(source.contains("robotjs"), "MCP source does not expose robotjs automation");
        requireFalse(source.contains("nutjs"), "MCP source does not expose nutjs automation");
        requireFalse(source.contains("pyautogui"), "MCP source does not expose pyautogui automation");
        requireFalse(source.contains("keyboard"), "MCP source does not expose OS keyboard automation");
        requireFalse(source.contains("ConditionEngine"), "MCP source does not enter ConditionEngine");
        requireFalse(source.contains("StateVariable"), "MCP source does not enter State Variable System");
        requireFalse(source.contains("ItemCondition"), "MCP source does not enter item/inventory/container conditions");
        requireFalse(source.contains("InventoryCondition"), "MCP source does not enter inventory conditions");
        requireFalse(source.contains("ContainerCondition"), "MCP source does not enter container conditions");
        requireFalse(source.contains("RegionCondition"), "MCP source does not enter region conditions");
        requireFalse(source.contains("SignalCondition"), "MCP source does not enter signal conditions");
        requireFalse(source.contains("LogicChainCondition"), "MCP source does not enter logic chain conditions");
        requireFalse(source.contains("SignalJoin"), "MCP source does not expose Signal Join tooling");
        requireFalse(source.contains("signal_join"), "MCP source does not expose signal_join tooling");
        requireFalse(source.contains("signal-join"), "MCP source does not expose signal-join tooling");
        requireFalse(source.contains("Signal Join"), "MCP source does not expose Signal Join labels");
        requireFalse(source.contains("Barrier"), "MCP source does not expose Signal Barrier tooling");
        requireFalse(source.contains("Aggregator"), "MCP source does not expose Signal Aggregator tooling");
        requireFalse(source.contains("GameController"), "MCP source does not enter GameController");
        requireFalse(source.contains("MissionSystem"), "MCP source does not enter MissionSystem");
        requireFalse(source.contains("PhaseController"), "MCP source does not enter PhaseController");
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
