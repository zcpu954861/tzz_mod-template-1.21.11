package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.webadmin.WebAdminFrontendAssets;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class WebAdminPerformanceBaselineGuardTest {
    private WebAdminPerformanceBaselineGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        CodeQualityGuardSupport.GuardReport report = new CodeQualityGuardSupport.GuardReport("9.1.1 WebAdmin performance baseline guard");
        run(report);
        report.printAndFail();
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws Exception {
        String appJs = WebAdminFrontendAssets.appJs();
        String appCss = WebAdminFrontendAssets.appCss();
        int appJsBytes = appJs.getBytes(StandardCharsets.UTF_8).length;
        int appCssBytes = appCss.getBytes(StandardCharsets.UTF_8).length;
        report.metric("performance.baseline.app_js.bytes", appJsBytes);
        report.metric("performance.baseline.app_css.bytes", appCssBytes);
        report.metric("performance.baseline.mode", "Phase 1 report/warning only; optimization deferred");

        Path output = CodeQualityGuardSupport.projectRoot().resolve("build/tmp/webadmin-app.js");
        Files.createDirectories(output.getParent());
        Files.writeString(output, appJs, StandardCharsets.UTF_8);
        String node = CodeQualityGuardSupport.findNodeExecutable();
        String parseScript = "const fs=require('fs');const vm=require('vm');const p=process.argv[1];"
                + "const code=fs.readFileSync(p,'utf8');const start=process.hrtime.bigint();"
                + "new vm.Script(code);const ms=Number(process.hrtime.bigint()-start)/1e6;"
                + "console.log(ms.toFixed(3));";
        try {
            CodeQualityGuardSupport.CommandResult parse = CodeQualityGuardSupport.runCommand(Duration.ofSeconds(30), node, "-e", parseScript, output.toString());
            if (parse.exitCode == 0) {
                report.metric("performance.baseline.vm_script_parse_ms", parse.output);
            } else {
                report.warning("Node vm.Script parse timing failed; syntax is covered by node --check. Output: " + parse.output);
            }
        } catch (AssertionError error) {
            report.warning("Node vm.Script parse timing timed out; syntax is covered by node --check. " + error.getMessage());
        }

        String performanceDoc = CodeQualityGuardSupport.read("docs/PERFORMANCE_HOTSPOTS_9_1_1.md");
        String currentContext = CodeQualityGuardSupport.read("docs/CODEBASE_HEALTH_GUARD_BASELINE_9_1_1_CURRENT_CONTEXT.md");
        report.requireContains(performanceDoc, "DOM equivalence baseline", "Performance doc DOM equivalence baseline");
        report.requireContains(currentContext, "Deferred to Phase 6", "Phase 1 current context performance deferral");
        report.requireContains(currentContext, "node --check", "Phase 1 current context node syntax guard");
        report.requireContains(currentContext, "warning", "Phase 1 current context warning strategy");
    }
}
