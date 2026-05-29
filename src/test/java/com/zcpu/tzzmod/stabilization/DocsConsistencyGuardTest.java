package com.zcpu.tzzmod.stabilization;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class DocsConsistencyGuardTest {
    private static final List<String> REQUIRED_9_1_1_DOCS = List.of(
            "docs/CODEBASE_HEALTH_AUDIT_9_1_1.md",
            "docs/PERFORMANCE_HOTSPOTS_9_1_1.md",
            "docs/REFACTOR_PLAN_9_1_1.md",
            "docs/CODE_QUALITY_GUARD_PLAN_9_1_1.md",
            "docs/IF_COMPLEXITY_HOTSPOT_AUDIT_9_1_1.md"
    );

    private DocsConsistencyGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        CodeQualityGuardSupport.GuardReport report = new CodeQualityGuardSupport.GuardReport("9.1.1 docs consistency guard");
        run(report);
        report.printAndFail();
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws IOException {
        Path root = CodeQualityGuardSupport.projectRoot();
        String readme = CodeQualityGuardSupport.read("README.md");
        report.requireContains(readme, "当前稳定版本：`v1.68.1-codebase-health-audit`", "README latest stable version");
        checkRequiredDocs(report);
        checkAuditDocBoundaries(report);
        checkCurrentContext(report);
        checkReadmeLinks(report, root, readme);
        checkNoIndependentFrontendProject(report, root);
    }

    private static void checkRequiredDocs(CodeQualityGuardSupport.GuardReport report) throws IOException {
        for (String doc : REQUIRED_9_1_1_DOCS) {
            report.require(Files.exists(CodeQualityGuardSupport.path(doc)), "Required 9.1.1 doc missing: " + doc);
            report.metric("docs.exists." + doc, Files.exists(CodeQualityGuardSupport.path(doc)));
        }
    }

    private static void checkAuditDocBoundaries(CodeQualityGuardSupport.GuardReport report) throws IOException {
        String audit = CodeQualityGuardSupport.read("docs/CODEBASE_HEALTH_AUDIT_9_1_1.md");
        String performance = CodeQualityGuardSupport.read("docs/PERFORMANCE_HOTSPOTS_9_1_1.md");
        String refactor = CodeQualityGuardSupport.read("docs/REFACTOR_PLAN_9_1_1.md");
        String guard = CodeQualityGuardSupport.read("docs/CODE_QUALITY_GUARD_PLAN_9_1_1.md");
        String ifAudit = CodeQualityGuardSupport.read("docs/IF_COMPLEXITY_HOTSPOT_AUDIT_9_1_1.md");
        String combined = audit + "\n" + performance + "\n" + refactor + "\n" + guard + "\n" + ifAudit;
        report.requireContains(combined, "不改变 9.1", "9.1 no behavior change boundary");
        report.requireContains(combined, "React/Vite", "No React/Vite migration boundary");
        report.requireContains(combined, "不新增 9.2", "No 9.2 feature boundary");
        report.requireContains(guard, "Baseline warning", "Guard plan baseline warning strategy");
        report.requireContains(guard, "Ratchet hard fail", "Guard plan ratchet hard fail strategy");
        report.requireContains(ifAudit, "Phase 7.5", "Phase 7.5 if complexity audit doc marker");
        report.requireContains(ifAudit, "Kept safety if", "Phase 7.5 safety-if classification");
        report.requireContains(ifAudit, "Deferred hotspots", "Phase 7.5 deferred hotspot classification");
        report.require(!combined.contains("完整跨 store atomic transaction"),
                "9.1.1 docs must not claim a full cross-store atomic transaction");
        for (int phase = 1; phase <= 7; phase++) {
            report.requireContains(refactor, "Phase " + phase, "Refactor plan Phase " + phase);
        }
    }

    private static void checkCurrentContext(CodeQualityGuardSupport.GuardReport report) throws IOException {
        String contextPath = "docs/CODEBASE_HEALTH_GUARD_BASELINE_9_1_1_CURRENT_CONTEXT.md";
        report.require(Files.exists(CodeQualityGuardSupport.path(contextPath)), "Phase 1 current context doc missing");
        if (!Files.exists(CodeQualityGuardSupport.path(contextPath))) {
            return;
        }
        String context = CodeQualityGuardSupport.read(contextPath);
        report.requireContains(context, "v1.68.1-codebase-health-audit", "Phase 1 current context stable tag");
        report.requireContains(context, "57212e5bb40777620742dbdd8ee65a867a993b23", "Phase 1 current context stable commit");
        report.requireContains(context, "does not change features", "Phase 1 current context no feature change");
        report.requireContains(context, "WebAdmin UI behavior", "Phase 1 current context no UI behavior change");
        report.requireContains(context, "runtime semantics", "Phase 1 current context no runtime change");
        report.requireContains(context, "React/Vite", "Phase 1 current context no React/Vite migration");
        report.requireContains(context, "9.2 typed actions", "Phase 1 current context no 9.2 typed actions");
        report.requireContains(context, "CodeQualityGuardTest", "Phase 1 current context guard class list");
        report.requireContains(context, "README", "Phase 1 current context README note");
    }

    private static void checkReadmeLinks(CodeQualityGuardSupport.GuardReport report, Path root, String readme) {
        Pattern docsLink = Pattern.compile("\\((docs/[^)#]+)(?:#[^)]+)?\\)");
        Matcher matcher = docsLink.matcher(readme);
        int checked = 0;
        while (matcher.find()) {
            String link = matcher.group(1).replace("%20", " ");
            if (!Files.exists(root.resolve(link))) {
                report.fail("README docs link is missing target: " + link);
            }
            checked++;
        }
        report.metric("docs.readme_links_checked", checked);
    }

    private static void checkNoIndependentFrontendProject(CodeQualityGuardSupport.GuardReport report, Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !isExcludedFrontendProjectScanPath(root, path))
                    .toList();
            for (Path file : files) {
                String relative = root.relativize(file).toString().replace('\\', '/');
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.startsWith("vite.config")) {
                    report.fail("WebAdmin must not introduce vite.config after Phase 7 ratchet: " + relative);
                }
                if ("package.json".equals(name)) {
                    String json = Files.readString(file);
                    String lower = json.toLowerCase(Locale.ROOT);
                    report.require(!lower.contains("\"react\"") && !lower.contains("\"@vitejs/")
                                    && !lower.contains("\"vite\""),
                            "WebAdmin must not add React/Vite runtime dependency in " + relative);
                }
                if (relative.startsWith("src/main/java/com/zcpu/tzzmod/webadmin/")) {
                    String source = Files.readString(file);
                    report.require(!Pattern.compile("<script[^>]+(?:unpkg|cdn\\.jsdelivr|esm\\.sh|vite|react)", Pattern.CASE_INSENSITIVE)
                                    .matcher(source).find(),
                            "WebAdmin must not load CDN framework/runtime scripts in " + relative);
                    report.require(!Pattern.compile("npm\\s+(?:run\\s+)?(?:build|dev|start).*WebAdmin|WebAdmin.*npm\\s+(?:run\\s+)?(?:build|dev|start)",
                                    Pattern.CASE_INSENSITIVE).matcher(source).find(),
                            "WebAdmin runtime must not require npm as a serving prerequisite in " + relative);
                }
            }
            report.metric("docs.no_independent_frontend_project.files_scanned", files.size());
        }
    }

    private static boolean isExcludedFrontendProjectScanPath(Path root, Path path) {
        String relative = root.relativize(path).toString().replace('\\', '/');
        return relative.startsWith("tools/tzz-test-mcp/")
                || relative.startsWith("build/")
                || relative.startsWith("logs/")
                || relative.startsWith(".git/")
                || relative.startsWith(".codex/")
                || relative.startsWith(".gradle/")
                || relative.startsWith("run/")
                || relative.startsWith("node_modules/")
                || relative.contains("/node_modules/");
    }
}
