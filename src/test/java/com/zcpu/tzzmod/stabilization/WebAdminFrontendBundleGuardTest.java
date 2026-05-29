package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.webadmin.WebAdminFrontendAssets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebAdminFrontendBundleGuardTest {
    private static final int APP_JS_WARNING_BASELINE_BYTES = 1_838_292;
    private static final int APP_JS_PHASE3_HARD_LIMIT_BYTES = 1_930_207;
    private static final String APP_JS_PHASE2_BASELINE_SHA256 = "1992d2e7634e14ac9611d893cf8439725bbc0fe4ee65f672cc90910b64238b74";
    private static final int APP_CSS_WARNING_BASELINE_BYTES = 123_251;
    private static final int APP_JS_PHASE6_BEFORE_BYTES = 1_843_648;
    private static final String APP_JS_PHASE6_BEFORE_SHA256 = "057e7e370d555036aff6d542b3ae4361f82d734b8fa95cf429d4d7ac7425beb3";
    private static final int APP_JS_PHASE6_WARNING_LIMIT_BYTES = 1_880_521;
    private static final int APP_JS_PHASE7_RATCHET_BYTES = 1_846_211;
    private static final String APP_JS_PHASE7_RATCHET_SHA256 = "474cc3093532f70d78583f996e8d6606496f45db831232f32607439a821a0069";
    private static final int APP_CSS_PHASE7_RATCHET_BYTES = 123_251;

    private static final Map<String, Integer> SOURCE_COUNT_BASELINES = new LinkedHashMap<>();
    private static final Map<String, Integer> INLINE_EVENT_ATTRIBUTE_BASELINES = new LinkedHashMap<>();
    private static final List<String> ZERO_BASELINE_INLINE_EVENTS = List.of(
            "onauxclick", "onblur", "oncontextmenu", "ondblclick", "ondrag", "ondragend",
            "ondragenter", "ondragleave", "ondragover", "ondragstart", "ondrop",
            "onkeyup", "onmouseenter", "onmouseleave", "onmousemove", "onmouseout",
            "onmouseover", "onmouseup", "onpointercancel", "onpointerdown",
            "onpointerenter", "onpointerleave", "onpointermove", "onpointerout",
            "onpointerover", "onpointerup", "ontouchcancel", "ontouchend",
            "ontouchmove", "ontouchstart", "onwheel"
    );

    static {
        SOURCE_COUNT_BASELINES.put("document.addEventListener", 22);
        SOURCE_COUNT_BASELINES.put("addEventListener(", 70);
        SOURCE_COUNT_BASELINES.put(".closest(", 73);
        SOURCE_COUNT_BASELINES.put("querySelector(", 71);
        SOURCE_COUNT_BASELINES.put("onclick=", 251);
        SOURCE_COUNT_BASELINES.put("oninput=", 150);
        SOURCE_COUNT_BASELINES.put("onchange=", 108);
        SOURCE_COUNT_BASELINES.put("onkeydown=", 17);
        SOURCE_COUNT_BASELINES.put("htmlEvent(", 45);
        SOURCE_COUNT_BASELINES.put("htmlHandler(", 177);
        SOURCE_COUNT_BASELINES.put("innerHTML", 61);
        SOURCE_COUNT_BASELINES.put("nativeTriggerJson", 2);

        INLINE_EVENT_ATTRIBUTE_BASELINES.put("onclick", 251);
        INLINE_EVENT_ATTRIBUTE_BASELINES.put("oninput", 150);
        INLINE_EVENT_ATTRIBUTE_BASELINES.put("onchange", 108);
        INLINE_EVENT_ATTRIBUTE_BASELINES.put("onkeydown", 17);
        INLINE_EVENT_ATTRIBUTE_BASELINES.put("onmousedown", 19);
        INLINE_EVENT_ATTRIBUTE_BASELINES.put("onfocus", 13);
        INLINE_EVENT_ATTRIBUTE_BASELINES.put("onsubmit", 42);
        INLINE_EVENT_ATTRIBUTE_BASELINES.put("oncompositionstart", 1);
        INLINE_EVENT_ATTRIBUTE_BASELINES.put("oncompositionend", 1);
        INLINE_EVENT_ATTRIBUTE_BASELINES.put("onerror", 1);
        INLINE_EVENT_ATTRIBUTE_BASELINES.put("onopen", 1);
    }

    private WebAdminFrontendBundleGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        CodeQualityGuardSupport.GuardReport report = new CodeQualityGuardSupport.GuardReport("9.1.1 WebAdmin frontend bundle guard");
        run(report);
        report.printAndFail();
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws Exception {
        String appJs = WebAdminFrontendAssets.appJs();
        String appCss = WebAdminFrontendAssets.appCss();
        int appJsBytes = appJs.getBytes(StandardCharsets.UTF_8).length;
        int appCssBytes = appCss.getBytes(StandardCharsets.UTF_8).length;
        report.metric("webadmin.app_js.bytes", appJsBytes);
        String appJsSha256 = sha256Hex(appJs);
        report.metric("webadmin.app_js.sha256", appJsSha256);
        report.metric("webadmin.app_js.phase2_baseline_sha256", APP_JS_PHASE2_BASELINE_SHA256);
        report.metric("webadmin.app_js.phase3_hard_limit", APP_JS_PHASE3_HARD_LIMIT_BYTES);
        report.metric("webadmin.app_js.phase6_before.bytes", APP_JS_PHASE6_BEFORE_BYTES);
        report.metric("webadmin.app_js.phase6_before.sha256", APP_JS_PHASE6_BEFORE_SHA256);
        report.metric("webadmin.app_js.phase6_after.bytes", appJsBytes);
        report.metric("webadmin.app_js.phase6_after.sha256", appJsSha256);
        report.metric("webadmin.app_js.phase6_delta.bytes", appJsBytes - APP_JS_PHASE6_BEFORE_BYTES);
        report.metric("webadmin.app_css.bytes", appCssBytes);
        report.metric("webadmin.app_js.phase7_ratchet.bytes", APP_JS_PHASE7_RATCHET_BYTES);
        report.metric("webadmin.app_js.phase7_ratchet.sha256", APP_JS_PHASE7_RATCHET_SHA256);
        report.metric("webadmin.app_css.phase7_ratchet.bytes", APP_CSS_PHASE7_RATCHET_BYTES);
        checkPhase4BeforeAfterEquivalence(report, appJs, appJsBytes, appJsSha256);
        if (appJsBytes > APP_JS_PHASE3_HARD_LIMIT_BYTES) {
            report.fail("Phase 3 generated app.js exceeded baseline + 5% hard limit: actualBytes="
                    + appJsBytes + " limit=" + APP_JS_PHASE3_HARD_LIMIT_BYTES + " actualSha256=" + appJsSha256);
        }
        if (appJsBytes != APP_JS_PHASE7_RATCHET_BYTES || !APP_JS_PHASE7_RATCHET_SHA256.equals(appJsSha256)) {
            report.fail("Phase 7 generated app.js ratchet changed: actualBytes=" + appJsBytes
                    + " expectedBytes=" + APP_JS_PHASE7_RATCHET_BYTES
                    + " actualSha256=" + appJsSha256 + " expectedSha256=" + APP_JS_PHASE7_RATCHET_SHA256
                    + ". Dedicated behavior/DOM-equivalence work must update this baseline explicitly.");
        }
        if (appCssBytes != APP_CSS_PHASE7_RATCHET_BYTES) {
            report.fail("Phase 7 generated app.css ratchet changed: actualBytes=" + appCssBytes
                    + " expectedBytes=" + APP_CSS_PHASE7_RATCHET_BYTES);
        }
        if (appJsBytes > APP_JS_PHASE6_WARNING_LIMIT_BYTES) {
            report.warning("Phase 6 generated app.js exceeded baseline + 2% warning limit: actualBytes="
                    + appJsBytes + " limit=" + APP_JS_PHASE6_WARNING_LIMIT_BYTES);
        }
        warnIfBeyondFivePercent(report, "app.js", appJsBytes, APP_JS_WARNING_BASELINE_BYTES);
        warnIfBeyondFivePercent(report, "app.css", appCssBytes, APP_CSS_WARNING_BASELINE_BYTES);

        checkFacadeBoundary(report);
        checkSourceCounts(report, appJs);
        checkInlineEventAttributes(report, appJs);
        checkPhase3EventRouterMarkers(report, appJs);
        checkHotSlices(report, appJs);
        checkPointerEvents(report, appCss);
        checkRawJsonSummary(report, appJs);
        runNodeCheck(report, appJs);
    }

    private static void warnIfBeyondFivePercent(CodeQualityGuardSupport.GuardReport report, String name, int actual, int baseline) {
        int warningLimit = (int) Math.ceil(baseline * 1.05d);
        report.metric("webadmin." + name + ".warning_limit", warningLimit);
        if (actual > warningLimit) {
            report.warning(name + " bytes exceeded current + 5% warning baseline: actual=" + actual + " limit=" + warningLimit);
        }
    }

    private static void checkPhase4BeforeAfterEquivalence(CodeQualityGuardSupport.GuardReport report, String appJs,
                                                          int appJsBytes, String appJsSha256) throws Exception {
        Path before = CodeQualityGuardSupport.projectRoot().resolve("build/tmp/webadmin-app-phase4-before.js");
        boolean beforeExists = Files.exists(before);
        report.metric("webadmin.app_js.phase4_before.exists", beforeExists);
        if (!beforeExists) {
            return;
        }
        String beforeText = Files.readString(before, StandardCharsets.UTF_8);
        int beforeBytes = beforeText.getBytes(StandardCharsets.UTF_8).length;
        String beforeSha256 = sha256Hex(beforeText);
        report.metric("webadmin.app_js.phase4_before.bytes", beforeBytes);
        report.metric("webadmin.app_js.phase4_before.sha256", beforeSha256);
        report.metric("webadmin.app_js.phase4_after.bytes", appJsBytes);
        report.metric("webadmin.app_js.phase4_after.sha256", appJsSha256);
        boolean matchesPhase4Artifact = beforeBytes == appJsBytes && beforeSha256.equals(appJsSha256) && beforeText.equals(appJs);
        boolean phase6Scope = CodeQualityGuardSupport.read("src/test/java/com/zcpu/tzzmod/stabilization/CodeQualityGuardTest.java")
                .contains("phase6-logic-chain-performance-baseline")
                || CodeQualityGuardSupport.read("src/test/java/com/zcpu/tzzmod/stabilization/CodeQualityGuardTest.java")
                .contains("phase7-codebase-health-guard-ratchet");
        boolean phase6BeforeArtifact = beforeBytes == APP_JS_PHASE6_BEFORE_BYTES
                && APP_JS_PHASE6_BEFORE_SHA256.equals(beforeSha256);
        boolean phase6DeltaWithinLimit = appJsBytes <= APP_JS_PHASE6_WARNING_LIMIT_BYTES;
        boolean phase6PerformanceBaseline = phase6Scope
                && phase6BeforeArtifact
                && phase6DeltaWithinLimit
                && appJs.contains("data-logic-chain-render-perf-markers")
                && appJs.contains("function logicChainRelatedNodeIndex")
                && appJs.contains("function logicChainMinimapKey");
        report.metric("webadmin.app_js.phase4_before.matches_current", matchesPhase4Artifact);
        report.metric("webadmin.app_js.phase4_before.phase6_scope", phase6Scope);
        report.metric("webadmin.app_js.phase4_before.phase6_baseline_artifact", phase6BeforeArtifact);
        report.metric("webadmin.app_js.phase4_before.phase6_delta_within_limit", phase6DeltaWithinLimit);
        report.metric("webadmin.app_js.phase4_before.historical_after_phase6", !matchesPhase4Artifact && phase6PerformanceBaseline);
        if (!matchesPhase4Artifact && phase6PerformanceBaseline) {
            // Phase 4 的 before artifact 是本地阶段内等价快照；Phase 6 会有意改变 app.js 来加入
            // 性能 marker 和当前渲染缓存。这里继续报告历史 artifact 差异，但由 Phase 6 DOM 等价
            // guard 负责硬失败，避免本地 build/tmp 快照阻塞后续合法优化。
            report.warning("Phase 4 before artifact is historical after Phase 6 app.js performance changes: beforeBytes="
                    + beforeBytes + " afterBytes=" + appJsBytes + " beforeSha256=" + beforeSha256
                    + " afterSha256=" + appJsSha256);
            return;
        }
        if (!matchesPhase4Artifact) {
            report.fail("Phase 4 Logic Chain module split changed generated app.js output: beforeBytes="
                    + beforeBytes + " afterBytes=" + appJsBytes + " beforeSha256=" + beforeSha256
                    + " afterSha256=" + appJsSha256);
        }
    }

    private static void checkFacadeBoundary(CodeQualityGuardSupport.GuardReport report) throws IOException {
        String assets = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendAssets.java");
        String server = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java");
        report.requireContains(assets, "WebAdminFrontendShell.appHtml()", "WebAdminFrontendAssets app HTML facade");
        report.requireContains(assets, "WebAdminFrontendStyles.appCss()", "WebAdminFrontendAssets CSS facade");
        report.requireContains(assets, "WebAdminFrontendScripts.appJs()", "WebAdminFrontendAssets JS facade");
        report.requireContains(server, "\"/assets/app.css\"", "WebAdminServer app.css route");
        report.requireContains(server, "WebAdminFrontendAssets.appCss()", "WebAdminServer app.css facade use");
        report.requireContains(server, "\"/assets/app.js\"", "WebAdminServer app.js route");
        report.requireContains(server, "WebAdminFrontendAssets.appJs()", "WebAdminServer app.js facade use");
    }

    private static void checkSourceCounts(CodeQualityGuardSupport.GuardReport report, String scripts) {
        for (Map.Entry<String, Integer> entry : SOURCE_COUNT_BASELINES.entrySet()) {
            int actual = CodeQualityGuardSupport.count(scripts, entry.getKey());
            report.metric("webadmin.source.count." + metricKey(entry.getKey()), actual + " baseline=" + entry.getValue());
            if (actual > entry.getValue()) {
                report.fail("Phase 3 WebAdmin frontend source count grew for `" + entry.getKey() + "`: actual=" + actual + " baseline=" + entry.getValue());
            }
        }
        report.require(scripts.contains("data-logic-chain-vbd-native-json-not-primary-summary"),
                "VBD native trigger raw JSON not-primary-summary marker must remain present");
    }

    private static void checkInlineEventAttributes(CodeQualityGuardSupport.GuardReport report, String scripts) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : INLINE_EVENT_ATTRIBUTE_BASELINES.entrySet()) {
            int actual = CodeQualityGuardSupport.countRegex(scripts, "\\b" + entry.getKey() + "\\s*=");
            total += actual;
            report.metric("webadmin.source.inline_event." + entry.getKey(), actual + " baseline=" + entry.getValue());
            if (actual > entry.getValue()) {
                report.fail("Phase 3 inline event attribute count grew for `" + entry.getKey() + "`: actual=" + actual
                        + " baseline=" + entry.getValue());
            }
        }
        report.metric("webadmin.source.inline_event.total_known", total);
        for (String eventName : ZERO_BASELINE_INLINE_EVENTS) {
            int actual = CodeQualityGuardSupport.countRegex(scripts, "\\b" + eventName + "\\s*=");
            report.metric("webadmin.source.inline_event.zero_baseline." + eventName, actual);
            if (actual > 0) {
                report.fail("New zero-baseline inline event attribute detected after Phase 3 router split: `" + eventName
                        + "` count=" + actual);
            }
        }
    }

    private static void checkPhase3EventRouterMarkers(CodeQualityGuardSupport.GuardReport report, String appJs) {
        for (String marker : List.of(
                "function dispatchDelegatedEvent",
                "function dispatchDelegatedSideEffects",
                "const globalClickCommandRoutes",
                "const globalClickSideEffectRoutes",
                "const globalClickLateRoutes",
                "const globalPrimaryKeydownRoutes",
                "const globalModalEscapeRoutes",
                "const logicChainClickRoutes",
                "const logicChainKeydownRoutes",
                "const logicChainPointerDownRoutes",
                "const logicChainHoverRoutes",
                "const logicChainMouseOutRoutes",
                "data-logic-chain-vbd-trigger-route-card"
        )) {
            report.requireContains(appJs, marker, "Phase 3 event router marker present: " + marker);
        }
        int inlineArrowRouteHandlers = CodeQualityGuardSupport.countRegex(appJs,
                "handler\\s*:\\s*(?:async\\s*)?(?:\\([^)]*\\)|[A-Za-z_$][\\w$]*)\\s*=>");
        int inlineFunctionRouteHandlers = CodeQualityGuardSupport.countRegex(appJs,
                "handler\\s*:\\s*(?:async\\s+)?function\\b");
        report.require(inlineArrowRouteHandlers == 0,
                "Phase 3 route entries must use named handlers, not inline arrow handlers; count="
                        + inlineArrowRouteHandlers);
        report.require(inlineFunctionRouteHandlers == 0,
                "Phase 3 route entries must use named handlers, not inline function handlers; count="
                        + inlineFunctionRouteHandlers);
        report.requireContains(appJs, "{selector:'[data-logic-chain-connect-handle]',handler:handleLogicChainConnectHandleClick},{selector:'[data-logic-chain-connect-candidate]',handler:handleLogicChainConnectCandidateClick},{selector:'[data-logic-chain-new-draft-channel]',handler:handleLogicChainNewDraftChannelClick},{selector:'[data-logic-chain-draft-slot-button]',handler:handleLogicChainDraftSlotClick},{selector:'[data-logic-chain-node-action]',handler:handleLogicChainNodeCardClick}",
                "Phase 3 Logic Chain click route order must stay connect/candidate/new-channel/draft-slot/node-card");
        report.requireContains(appJs, "{handler:handleGlobalLogicChainClickRoute},{selector:'[data-logic-chain-vbd-trigger-card]',handler:handleLogicChainVbdTriggerCardClick},{handler:handleSnapshotTimelineNodeClick}",
                "Phase 3 VBD trigger card click must run in document bubble after Logic Chain capture route and before outside-close side effects");
        report.require(!appJs.contains("htmlHandler(`selectLogicChainExistingVbdTrigger("),
                "Phase 3 VBD trigger cards must use data-* delegated route instead of inline select handler");
        int logicChainRoutesStart = appJs.indexOf("const logicChainClickRoutes=[");
        int logicChainRoutesEnd = logicChainRoutesStart >= 0 ? appJs.indexOf("];", logicChainRoutesStart) : -1;
        String logicChainRoutes = logicChainRoutesStart >= 0 && logicChainRoutesEnd > logicChainRoutesStart
                ? appJs.substring(logicChainRoutesStart, logicChainRoutesEnd)
                : "";
        report.require(!logicChainRoutes.contains("data-logic-chain-vbd-trigger-card"),
                "Phase 3 VBD trigger card route must stay out of Logic Chain capture routes");
        report.requireContains(appJs,
                "function handleLogicChainVbdTriggerCardClick(event,card){selectLogicChainExistingVbdTrigger(card.dataset.logicChainVbdTriggerCard||'');return false;}",
                "Phase 3 VBD trigger bubble handler must return false so outside-close side effects still run");
    }

    private static void checkHotSlices(CodeQualityGuardSupport.GuardReport report, String appJs) {
        reportSlice(report, "global_handlers_1260_1339",
                generatedSlice(report, appJs, "function globalEventTargetOutside", "window.addEventListener('beforeunload'"),
                54, 35, 4);
        reportSlice(report, "esc_router_5294",
                generatedSlice(report, appJs, "const globalModalEscapeRoutes", "function unavailableFeature"),
                8, 0, 1);
        reportSlice(report, "logic_chain_handlers_8103_8123",
                generatedSlice(report, appJs, "function handleLogicChainConnectHandleClick", "const LOGIC_CHAIN_NODE_DELETE_CONFIRM_TEXT"),
                46, 13, 1);
        reportSlice(report, "vbd_overlay_stack_8128_8397",
                generatedSlice(report, appJs, "const logicChainV16VbdTriggerGraphSummaryMarkers", "function confirmLogicChainDraftActionDeleteFromPanel"),
                362, 1, 3);
    }

    private static String generatedSlice(CodeQualityGuardSupport.GuardReport report, String appJs,
                                         String startNeedle, String endNeedle) {
        int start = appJs.indexOf(startNeedle);
        int end = start >= 0 ? appJs.indexOf(endNeedle, start) : -1;
        report.require(start >= 0, "Generated app.js hot-slice start marker missing `" + startNeedle + "`");
        report.require(end > start, "Generated app.js hot-slice end marker missing `" + endNeedle + "`");
        if (start < 0 || end <= start) {
            return "";
        }
        return appJs.substring(start, end);
    }

    private static void reportSlice(CodeQualityGuardSupport.GuardReport report, String name, String slice,
                                    int ifBaseline, int closestBaseline, int querySelectorBaseline) {
        int ifCount = CodeQualityGuardSupport.countRegex(slice, "\\bif\\s*\\(");
        int closestCount = CodeQualityGuardSupport.count(slice, ".closest(");
        int querySelectorCount = CodeQualityGuardSupport.count(slice, "querySelector(");
        report.metric("webadmin.slice." + name + ".if", ifCount + " baseline=" + ifBaseline);
        report.metric("webadmin.slice." + name + ".closest", closestCount + " baseline=" + closestBaseline);
        report.metric("webadmin.slice." + name + ".querySelector", querySelectorCount + " baseline=" + querySelectorBaseline);
        requireSliceNoGrowth(report, name, "if", ifCount, ifBaseline);
        requireSliceNoGrowth(report, name, "closest", closestCount, closestBaseline);
        requireSliceNoGrowth(report, name, "querySelector", querySelectorCount, querySelectorBaseline);
    }

    private static void requireSliceNoGrowth(CodeQualityGuardSupport.GuardReport report, String name, String metric, int actual, int baseline) {
        if (actual > baseline) {
            report.fail("WebAdmin hot slice grew for " + name + " " + metric + ": actual=" + actual + " baseline=" + baseline);
        }
    }

    private static void checkPointerEvents(CodeQualityGuardSupport.GuardReport report, String appCss) {
        requirePointerEvents(report, appCss, ".logic-chain-edge-layer", "none",
                "Logic Chain edge layer must keep pointer-events:none");
        requirePointerEvents(report, appCss, ".logic-chain-minimap", "none",
                "Logic Chain minimap must keep pointer-events:none");
        requirePointerEvents(report, appCss, ".logic-chain-draft-handles", "none",
                "Logic Chain draft handles must keep pointer-events:none");
        requirePointerEvents(report, appCss, ".logic-chain-connect-plus", "auto",
                "Logic Chain connect plus must keep pointer-events:auto");
    }

    private static void requirePointerEvents(CodeQualityGuardSupport.GuardReport report, String appCss,
                                             String selector, String expectedValue, String message) {
        List<String> values = CodeQualityGuardSupport.selectorPointerEventsValues(appCss, selector);
        report.metric("webadmin.css.pointer_events." + metricKey(selector), values);
        report.require(values.contains(expectedValue), message);
        for (String value : values) {
            if (!expectedValue.equals(value)) {
                report.fail(message + " but found overriding pointer-events:" + value + " for " + selector);
            }
        }
    }

    private static void checkRawJsonSummary(CodeQualityGuardSupport.GuardReport report, String appJs) {
        int nativeTriggerJsonCount = CodeQualityGuardSupport.count(appJs, "nativeTriggerJson");
        report.metric("webadmin.app_js.nativeTriggerJson", nativeTriggerJsonCount);
        if (nativeTriggerJsonCount > 2) {
            report.fail("nativeTriggerJson occurrences increased after Phase 7 ratchet; raw JSON must remain secondary/debug only");
        }
        report.require(appJs.contains("data-logic-chain-vbd-native-json-not-primary-summary"),
                "Readable VBD native trigger summary marker missing from app.js output");
        requireNoRawJsonSummaryPattern(report, appJs, "\\bfield\\s*:\\s*['\\\"]nativeTriggerJson['\\\"]",
                "nativeTriggerJson must not be selected as a primary summary field");
        requireNoRawJsonSummaryPattern(report, appJs,
                "\\b(?:primarySummary|mainSummary|summaryLabel|summaryField|displaySummary)\\s*:\\s*['\\\"]nativeTriggerJson['\\\"]",
                "nativeTriggerJson must not be wired as a user-facing summary label");
        requireNoRawJsonSummaryPattern(report, appJs, "nativeTriggerJson[^\\n]{0,120}(?:primary|summary|摘要)",
                "nativeTriggerJson must not appear near primary summary text");
        requireNoRawJsonSummaryPattern(report, appJs, "(?:primary|summary|摘要)[^\\n]{0,120}nativeTriggerJson",
                "nativeTriggerJson must not appear near primary summary text");
    }

    private static void requireNoRawJsonSummaryPattern(CodeQualityGuardSupport.GuardReport report, String appJs,
                                                       String regex, String message) {
        int matches = CodeQualityGuardSupport.countRegex(appJs, regex);
        report.metric("webadmin.raw_json_summary.negative_scan." + Math.abs(regex.hashCode()), matches);
        if (matches > 0) {
            report.fail(message + "; regex matched count=" + matches);
        }
    }

    private static void runNodeCheck(CodeQualityGuardSupport.GuardReport report, String appJs) throws Exception {
        Path output = CodeQualityGuardSupport.projectRoot().resolve("build/tmp/webadmin-app.js");
        Files.createDirectories(output.getParent());
        Files.writeString(output, appJs, StandardCharsets.UTF_8);
        String node = CodeQualityGuardSupport.findNodeExecutable();
        CodeQualityGuardSupport.CommandResult version = CodeQualityGuardSupport.runCommand(Duration.ofSeconds(5), node, "--version");
        report.metric("node.version", version.output);
        CodeQualityGuardSupport.CommandResult result = CodeQualityGuardSupport.runCommand(Duration.ofSeconds(30), node, "--check", output.toString());
        report.metric("node.check.build/tmp/webadmin-app.js", "exit=" + result.exitCode);
        if (result.exitCode != 0) {
            report.fail("node --check build/tmp/webadmin-app.js failed: " + result.output);
        }
    }

    private static String metricKey(String key) {
        return key.replace("(", "").replace("=", "").replace(".", "dot").replace(" ", "_").replace("-", "_");
    }

    private static String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b & 0xff));
        }
        return hex.toString();
    }
}
