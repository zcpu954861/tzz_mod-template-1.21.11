package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.webadmin.WebAdminFrontendAssets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class CodeQualityGuardTest {
    private static final String WEBADMIN_FRONTEND_SCRIPTS =
            "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendScripts.java";
    private static final int WEBADMIN_FRONTEND_SCRIPTS_FACADE_MAX_LINES = 300;
    private static final int WEBADMIN_FRONTEND_SCRIPTS_FACADE_MAX_BYTES = 20_000;

    private static final Map<String, FileBaseline> KNOWN_FILE_BASELINES = Map.of(
            WEBADMIN_FRONTEND_SCRIPTS, new FileBaseline(8433, 1_984_343),
            "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendStyles.java", new FileBaseline(75, 123_798),
            "src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorService.java", new FileBaseline(5205, 318_695),
            "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java", new FileBaseline(3102, 164_014),
            "src/main/java/com/zcpu/tzzmod/webadmin/draft/WebAdminProtectedDraftRegistry.java", new FileBaseline(620, 26_369),
            "src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java", new FileBaseline(12_423, 839_370)
    );

    private static final Map<String, Integer> BEFORE_V_BASELINES = Map.of(
            "BeforeV11", 68,
            "BeforeV12", 19,
            "BeforeV13", 23,
            "BeforeV14", 28,
            "BeforeV15", 8,
            "BeforeV16", 16,
            "BeforeV17", 22
    );

    private static final int BEFORE_V13_TO_V17_TOTAL = 97;
    private static final int ALL_BEFORE_VXX_TOTAL = 184;

    private static final Map<String, Integer> HOTSPOT_REDEFINITION_BASELINES = Map.of(
            "logicChainApplyVbdNativeTriggerDraftGraphOverlay", 3,
            "logicChainRenderedGraphWithDraftOverlay", 5,
            "logicChainNodeCard", 9,
            "renderLogicChainViewer", 3
    );

    private static final List<String> LEGACY_WEBADMIN_SERVICES_OVER_1000 = List.of(
            "WebAdminActionRelayActionsService.java",
            "WebAdminLogicChainEditorService.java",
            "WebAdminLogicChainService.java",
            "WebAdminRegionControllerService.java",
            "WebAdminTemplateService.java",
            "WebAdminTimerService.java",
            "WebAdminVirtualBlockDeviceNativeTriggerService.java"
    );

    private CodeQualityGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        CodeQualityGuardSupport.GuardReport report = new CodeQualityGuardSupport.GuardReport("9.1.1 code quality guard baseline");
        run(report);
        WebAdminFrontendBundleGuardTest.run(report);
        WebAdminPerformanceBaselineGuardTest.run(report);
        DocsConsistencyGuardTest.run(report);
        report.printAndFail();
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws IOException {
        Path root = CodeQualityGuardSupport.projectRoot();
        report.metric("branch.scope", "phase2-frontend-bundle-split");
        checkKnownFileBaselines(report);
        checkFrontendScriptsFacade(report);
        checkBeforeVxx(report);
        checkHotspotRedefinitions(report);
        checkModuleBudgets(report, root);
        reportLargeMethods(report, root);
    }

    private static void checkKnownFileBaselines(CodeQualityGuardSupport.GuardReport report) throws IOException {
        for (Map.Entry<String, FileBaseline> entry : KNOWN_FILE_BASELINES.entrySet()) {
            String path = entry.getKey();
            FileBaseline baseline = entry.getValue();
            long actualLines = CodeQualityGuardSupport.lineCount(path);
            long actualBytes = CodeQualityGuardSupport.bytes(path);
            report.metric("file.lines." + path, actualLines + " baseline=" + baseline.lines);
            report.metric("file.bytes." + path, actualBytes + " baseline=" + baseline.bytes);
            if (actualLines > baseline.lines) {
                report.fail(path + " line count grew from " + baseline.lines + " to " + actualLines);
            }
            if (actualBytes > baseline.bytes) {
                report.fail(path + " byte size grew from " + baseline.bytes + " to " + actualBytes);
            }
        }
    }

    private static void checkFrontendScriptsFacade(CodeQualityGuardSupport.GuardReport report) throws IOException {
        String facade = CodeQualityGuardSupport.read(WEBADMIN_FRONTEND_SCRIPTS);
        long lines = CodeQualityGuardSupport.lineCount(WEBADMIN_FRONTEND_SCRIPTS);
        long bytes = CodeQualityGuardSupport.bytes(WEBADMIN_FRONTEND_SCRIPTS);
        report.metric("frontend.facade.WebAdminFrontendScripts.lines", lines
                + " max=" + WEBADMIN_FRONTEND_SCRIPTS_FACADE_MAX_LINES);
        report.metric("frontend.facade.WebAdminFrontendScripts.bytes", bytes
                + " max=" + WEBADMIN_FRONTEND_SCRIPTS_FACADE_MAX_BYTES);
        if (lines > WEBADMIN_FRONTEND_SCRIPTS_FACADE_MAX_LINES) {
            report.fail("WebAdminFrontendScripts.java must remain a Phase 2 bundle facade; lines=" + lines);
        }
        if (bytes > WEBADMIN_FRONTEND_SCRIPTS_FACADE_MAX_BYTES) {
            report.fail("WebAdminFrontendScripts.java must remain a Phase 2 bundle facade; bytes=" + bytes);
        }
        if (facade.contains(".append(\"\"\"") || facade.contains("class ApiError") || facade.contains("const appState=")) {
            report.fail("WebAdminFrontendScripts.java contains generated JS business text instead of facade-only concat logic");
        }
        requireOrdered(report, facade,
                "WebAdminFrontendIconScripts.appJs()",
                "WebAdminFrontendCoreScripts.appJs()",
                "WebAdminFrontendCoreEventScripts.appJs()",
                "WebAdminFrontendPageScripts.appJs()",
                "WebAdminLogicChainViewerScripts.appJs()",
                "WebAdminLogicChainEditorScripts.appJs()",
                "WebAdminLogicChainVbdScripts.appJs()",
                "WebAdminFrontendBootstrapScripts.appJs()");
    }

    private static void requireOrdered(CodeQualityGuardSupport.GuardReport report, String text, String... needles) {
        int previous = -1;
        for (String needle : needles) {
            int current = text.indexOf(needle);
            report.require(current >= 0, "WebAdminFrontendScripts facade missing ordered module call `" + needle + "`");
            if (current >= 0 && previous >= 0 && current <= previous) {
                report.fail("WebAdminFrontendScripts facade module order changed near `" + needle + "`");
            }
            previous = current;
        }
    }

    private static void checkBeforeVxx(CodeQualityGuardSupport.GuardReport report) throws IOException {
        String scripts = WebAdminFrontendAssets.appJs();
        int v13ToV17 = 0;
        for (Map.Entry<String, Integer> entry : BEFORE_V_BASELINES.entrySet()) {
            int actual = CodeQualityGuardSupport.count(scripts, entry.getKey());
            report.metric("beforev.count." + entry.getKey(), actual + " baseline=" + entry.getValue());
            if (actual > entry.getValue()) {
                report.warning(entry.getKey() + " count grew from " + entry.getValue() + " to " + actual);
            }
            if (entry.getKey().compareTo("BeforeV13") >= 0) {
                v13ToV17 += actual;
            }
        }
        int allBeforeVxx = CodeQualityGuardSupport.countRegex(scripts, "BeforeV\\d+");
        int beforeV18Plus = CodeQualityGuardSupport.countRegex(scripts, "BeforeV(1[8-9]|[2-9]\\d+)");
        report.metric("beforev.count.v13_to_v17", v13ToV17 + " baseline=" + BEFORE_V13_TO_V17_TOTAL);
        report.metric("beforev.count.all", allBeforeVxx + " baseline=" + ALL_BEFORE_VXX_TOTAL);
        report.metric("beforev.count.v18_plus", beforeV18Plus);
        if (v13ToV17 > BEFORE_V13_TO_V17_TOTAL) {
            report.warning("BeforeV13-17 count grew from " + BEFORE_V13_TO_V17_TOTAL + " to " + v13ToV17);
        }
        if (allBeforeVxx > ALL_BEFORE_VXX_TOTAL) {
            report.warning("All BeforeVxx token count grew from " + ALL_BEFORE_VXX_TOTAL + " to " + allBeforeVxx);
        }
        if (beforeV18Plus > 0) {
            report.fail("New BeforeV18+ patch stacking token detected");
        }
    }

    private static void checkHotspotRedefinitions(CodeQualityGuardSupport.GuardReport report) throws IOException {
        String scripts = WebAdminFrontendAssets.appJs();
        for (Map.Entry<String, Integer> entry : HOTSPOT_REDEFINITION_BASELINES.entrySet()) {
            String name = entry.getKey();
            String quoted = Pattern.quote(name);
            String regex = "(?m)(?:\\bfunction\\s+" + quoted + "\\s*\\("
                    + "|\\b" + quoted + "\\s*=\\s*(?:async\\s+)?function\\s*\\("
                    + "|\\b(?:const|let|var)\\s+" + quoted + "[A-Za-z0-9_$]*\\s*=)";
            int actual = CodeQualityGuardSupport.countRegex(scripts, regex);
            report.metric("patch_stack.hotspot_redefinitions." + name, actual + " baseline=" + entry.getValue());
            if (actual > entry.getValue()) {
                report.fail("Hotspot wrapper/redefinition count grew for " + name + ": actual=" + actual
                        + " baseline=" + entry.getValue() + ". Do not add BeforeVxx-free monkey patches in Phase 1.");
            }
        }
    }

    private static void checkModuleBudgets(CodeQualityGuardSupport.GuardReport report, Path root) throws IOException {
        for (Path file : CodeQualityGuardSupport.javaFiles(root.resolve("src/main/java/com/zcpu/tzzmod/webadmin"))) {
            String fileName = file.getFileName().toString();
            String relative = root.relativize(file).toString().replace('\\', '/');
            long lines = CodeQualityGuardSupport.lineCount(relative);
            long bytes = CodeQualityGuardSupport.bytes(relative);
            if (fileName.endsWith("Scripts.java") && !fileName.equals("WebAdminFrontendScripts.java") && lines > 800) {
                report.fail("Frontend script module exceeds 800-line Phase 1 budget: " + relative + " lines=" + lines);
            }
            if (fileName.endsWith("Scripts.java") && !fileName.equals("WebAdminFrontendScripts.java")) {
                report.metric("frontend.script_module.bytes." + fileName, bytes);
                report.metric("frontend.script_module.lines." + fileName, lines);
            }
            if (fileName.contains("Styles") && !fileName.equals("WebAdminFrontendStyles.java") && lines > 800) {
                report.fail("Frontend style module exceeds 800-line Phase 1 budget: " + relative + " lines=" + lines);
            }
            if (relative.startsWith("src/main/java/com/zcpu/tzzmod/webadmin/service/")
                    && fileName.endsWith("Service.java")
                    && lines > 1000
                    && !LEGACY_WEBADMIN_SERVICES_OVER_1000.contains(fileName)) {
                report.fail("New backend service exceeds 1000-line Phase 1 budget: " + relative + " lines=" + lines);
            }
        }
        for (Path file : CodeQualityGuardSupport.javaFiles(root.resolve("src/test/java/com/zcpu/tzzmod/stabilization"))) {
            String fileName = file.getFileName().toString();
            String relative = root.relativize(file).toString().replace('\\', '/');
            long lines = CodeQualityGuardSupport.lineCount(relative);
            if (!fileName.equals("StabilizationGuardTest.java") && lines > 1000) {
                report.fail("Guard class exceeds 1000-line Phase 1 budget: " + relative + " lines=" + lines);
            }
        }
    }

    private static void reportLargeMethods(CodeQualityGuardSupport.GuardReport report, Path root) throws IOException {
        List<CodeQualityGuardSupport.MethodMetric> methods = CodeQualityGuardSupport.collectLargeJavaMethods(root, 50);
        int index = 1;
        for (CodeQualityGuardSupport.MethodMetric method : methods) {
            report.metric("java.method.top." + String.format("%03d", index), method.lines + " lines " + method.name + " " + method.location);
            if (method.lines > 120 && !method.location.contains("StabilizationGuardTest.java")) {
                report.warning("Large Java method report-only: " + method.name + " " + method.location + " lines=" + method.lines);
            }
            index++;
        }
        String appJs = WebAdminFrontendAssets.appJs();
        List<CodeQualityGuardSupport.MethodMetric> jsFunctions = CodeQualityGuardSupport.collectLargeJsFunctions(appJs, 50);
        report.metric("js.function.top.count", jsFunctions.size());
        if (jsFunctions.isEmpty()) {
            report.fail("JS function-length detector found no functions in generated app.js");
        }
        index = 1;
        for (CodeQualityGuardSupport.MethodMetric function : jsFunctions) {
            report.metric("js.function.top." + String.format("%03d", index),
                    function.lines + " lines " + function.chars + " chars " + function.name + " " + function.location);
            if (function.lines > 80 || function.chars > 8_000) {
                report.warning("Large JS function report-only: " + function.name + " " + function.location
                        + " lines=" + function.lines + " chars=" + function.chars);
            }
            index++;
        }
    }

    private static final class FileBaseline {
        final int lines;
        final int bytes;

        FileBaseline(int lines, int bytes) {
            this.lines = lines;
            this.bytes = bytes;
        }
    }
}
