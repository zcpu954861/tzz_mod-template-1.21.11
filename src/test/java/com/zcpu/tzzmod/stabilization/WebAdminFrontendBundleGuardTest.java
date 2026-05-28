package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.webadmin.WebAdminFrontendAssets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebAdminFrontendBundleGuardTest {
    private static final int APP_JS_WARNING_BASELINE_BYTES = 1_838_292;
    private static final int APP_CSS_WARNING_BASELINE_BYTES = 123_251;

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
        report.metric("webadmin.app_css.bytes", appCssBytes);
        warnIfBeyondFivePercent(report, "app.js", appJsBytes, APP_JS_WARNING_BASELINE_BYTES);
        warnIfBeyondFivePercent(report, "app.css", appCssBytes, APP_CSS_WARNING_BASELINE_BYTES);

        checkFacadeBoundary(report);
        checkSourceCounts(report);
        checkInlineEventAttributes(report);
        checkHotSlices(report);
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

    private static void checkSourceCounts(CodeQualityGuardSupport.GuardReport report) throws IOException {
        String scripts = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java");
        for (Map.Entry<String, Integer> entry : SOURCE_COUNT_BASELINES.entrySet()) {
            int actual = CodeQualityGuardSupport.count(scripts, entry.getKey());
            report.metric("webadmin.source.count." + metricKey(entry.getKey()), actual + " baseline=" + entry.getValue());
            if (actual > entry.getValue()) {
                report.warning("WebAdmin frontend source count grew for `" + entry.getKey() + "`: actual=" + actual + " baseline=" + entry.getValue());
            }
        }
        report.require(scripts.contains("data-logic-chain-vbd-native-json-not-primary-summary"),
                "VBD native trigger raw JSON not-primary-summary marker must remain present");
    }

    private static void checkInlineEventAttributes(CodeQualityGuardSupport.GuardReport report) throws IOException {
        String scripts = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java");
        int total = 0;
        for (Map.Entry<String, Integer> entry : INLINE_EVENT_ATTRIBUTE_BASELINES.entrySet()) {
            int actual = CodeQualityGuardSupport.countRegex(scripts, "\\b" + entry.getKey() + "\\s*=");
            total += actual;
            report.metric("webadmin.source.inline_event." + entry.getKey(), actual + " baseline=" + entry.getValue());
            if (actual > entry.getValue()) {
                report.warning("Inline event attribute count grew for `" + entry.getKey() + "`: actual=" + actual
                        + " baseline=" + entry.getValue());
            }
        }
        report.metric("webadmin.source.inline_event.total_known", total);
        for (String eventName : ZERO_BASELINE_INLINE_EVENTS) {
            int actual = CodeQualityGuardSupport.countRegex(scripts, "\\b" + eventName + "\\s*=");
            report.metric("webadmin.source.inline_event.zero_baseline." + eventName, actual);
            if (actual > 0) {
                report.warning("New zero-baseline inline event attribute detected in Phase 1: `" + eventName
                        + "` count=" + actual);
            }
        }
    }

    private static void checkHotSlices(CodeQualityGuardSupport.GuardReport report) throws IOException {
        reportSlice(report, "global_handlers_1260_1339",
                CodeQualityGuardSupport.lineSlice("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java", 1260, 1339),
                54, 35, 4);
        reportSlice(report, "esc_router_5294",
                CodeQualityGuardSupport.lineSlice("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java", 5294, 5294),
                8, 0, 1);
        reportSlice(report, "logic_chain_handlers_8103_8123",
                CodeQualityGuardSupport.lineSlice("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java", 8103, 8123),
                46, 13, 1);
        reportSlice(report, "vbd_overlay_stack_8128_8397",
                CodeQualityGuardSupport.lineSlice("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java", 8128, 8397),
                362, 1, 3);
    }

    private static void reportSlice(CodeQualityGuardSupport.GuardReport report, String name, String slice,
                                    int ifBaseline, int closestBaseline, int querySelectorBaseline) {
        int ifCount = CodeQualityGuardSupport.countRegex(slice, "\\bif\\s*\\(");
        int closestCount = CodeQualityGuardSupport.count(slice, ".closest(");
        int querySelectorCount = CodeQualityGuardSupport.count(slice, "querySelector(");
        report.metric("webadmin.slice." + name + ".if", ifCount + " baseline=" + ifBaseline);
        report.metric("webadmin.slice." + name + ".closest", closestCount + " baseline=" + closestBaseline);
        report.metric("webadmin.slice." + name + ".querySelector", querySelectorCount + " baseline=" + querySelectorBaseline);
        warnSliceGrowth(report, name, "if", ifCount, ifBaseline);
        warnSliceGrowth(report, name, "closest", closestCount, closestBaseline);
        warnSliceGrowth(report, name, "querySelector", querySelectorCount, querySelectorBaseline);
    }

    private static void warnSliceGrowth(CodeQualityGuardSupport.GuardReport report, String name, String metric, int actual, int baseline) {
        if (actual > baseline) {
            report.warning("WebAdmin hot slice grew for " + name + " " + metric + ": actual=" + actual + " baseline=" + baseline);
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
            report.warning("nativeTriggerJson occurrences increased; ensure raw JSON remains secondary/debug only");
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
}
