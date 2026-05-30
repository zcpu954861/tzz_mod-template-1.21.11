package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.webadmin.WebAdminFrontendAssets;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WebAdminPerformanceBaselineGuardTest {
    private static final int PHASE6_APP_JS_BEFORE_BYTES = 1_843_648;
    private static final String PHASE6_APP_JS_BEFORE_SHA256 = "057e7e370d555036aff6d542b3ae4361f82d734b8fa95cf429d4d7ac7425beb3";
    private static final int PHASE6_APP_JS_WARNING_LIMIT_BYTES = 1_880_521;
    private static final int PHASE7_APP_JS_RATCHET_BYTES = 1_859_186;
    private static final String PHASE7_APP_JS_RATCHET_SHA256 = "beef672762cda9ad23a607755830e6118238a89887c1ad28753f950221c9326d";
    private static final int PHASE7_APP_CSS_RATCHET_BYTES = 123_251;

    private static final Map<String, Map<String, String>> PHASE6_DOM_BASELINES = phase6DomBaselines();

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
        report.metric("performance.phase", "phase6-logic-chain-performance-baseline");
        report.metric("performance.app_js.before.bytes", PHASE6_APP_JS_BEFORE_BYTES);
        report.metric("performance.app_js.before.sha256", PHASE6_APP_JS_BEFORE_SHA256);
        report.metric("performance.app_js.after.bytes", appJsBytes);
        String appJsSha256 = sha256Hex(appJs);
        report.metric("performance.app_js.after.sha256", appJsSha256);
        report.metric("performance.app_js.delta.bytes", appJsBytes - PHASE6_APP_JS_BEFORE_BYTES);
        report.metric("performance.phase7.app_js.ratchet.bytes", PHASE7_APP_JS_RATCHET_BYTES);
        report.metric("performance.phase7.app_js.ratchet.sha256", PHASE7_APP_JS_RATCHET_SHA256);
        report.metric("performance.phase7.app_css.ratchet.bytes", PHASE7_APP_CSS_RATCHET_BYTES);
        if (appJsBytes != PHASE7_APP_JS_RATCHET_BYTES || !PHASE7_APP_JS_RATCHET_SHA256.equals(appJsSha256)) {
            report.fail("Phase 7 performance asset ratchet changed app.js: actualBytes=" + appJsBytes
                    + " expectedBytes=" + PHASE7_APP_JS_RATCHET_BYTES
                    + " actualSha256=" + appJsSha256 + " expectedSha256=" + PHASE7_APP_JS_RATCHET_SHA256);
        }
        if (appCssBytes != PHASE7_APP_CSS_RATCHET_BYTES) {
            report.fail("Phase 7 performance asset ratchet changed app.css: actualBytes=" + appCssBytes
                    + " expectedBytes=" + PHASE7_APP_CSS_RATCHET_BYTES);
        }
        if (appJsBytes > PHASE6_APP_JS_WARNING_LIMIT_BYTES) {
            report.warning("Phase 6 app.js bytes exceeded current + 2% warning limit: actual="
                    + appJsBytes + " limit=" + PHASE6_APP_JS_WARNING_LIMIT_BYTES);
        }
        report.requireContains(appJs, "data-logic-chain-render-perf-markers", "Phase 6 performance marker registry");
        report.requireContains(appJs, "function logicChainRelatedNodeIndex", "Phase 6 related node precomputed map marker");
        report.requireContains(appJs, "function logicChainMinimapKey", "Phase 6 minimap memo key marker");

        Path output = CodeQualityGuardSupport.projectRoot().resolve("build/tmp/webadmin-app.js");
        Files.createDirectories(output.getParent());
        Files.writeString(output, appJs, StandardCharsets.UTF_8);
        String node = CodeQualityGuardSupport.findNodeExecutable();
        CodeQualityGuardSupport.CommandResult nodeCheck =
                CodeQualityGuardSupport.runCommand(Duration.ofSeconds(30), node, "--check", output.toString());
        report.metric("performance.node_check.build/tmp/webadmin-app.js", "exit=" + nodeCheck.exitCode);
        if (nodeCheck.exitCode != 0) {
            report.fail("Performance guard standalone node --check failed: " + nodeCheck.output);
        }
        checkPhase7SourceRatchets(report);
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
        runLogicChainSyntheticBaseline(report, node, output);
        runPhase912RealJsGraphBenchmarks(report, node, output);
        runPhase912JavaProxyGraphBenchmarks(report);

        String performanceDoc = CodeQualityGuardSupport.read("docs/PERFORMANCE_HOTSPOTS_9_1_1.md");
        String currentContext = CodeQualityGuardSupport.read("docs/CODEBASE_HEALTH_GUARD_BASELINE_9_1_1_CURRENT_CONTEXT.md");
        String guardPlan = CodeQualityGuardSupport.read("docs/CODE_QUALITY_GUARD_PLAN_9_1_1.md");
        report.requireContains(performanceDoc, "DOM equivalence baseline", "Performance doc DOM equivalence baseline");
        report.requireContains(performanceDoc, "Phase 6 implemented", "Performance doc Phase 6 implementation note");
        report.requireContains(currentContext, "Phase 6 Logic Chain Performance Baseline Context", "Phase 6 current context section");
        report.requireContains(currentContext, "node --check", "Phase 1 current context node syntax guard");
        report.requireContains(guardPlan, "phase6-logic-chain-performance-baseline", "Phase 6 guard scope marker");
    }

    private static void runLogicChainSyntheticBaseline(CodeQualityGuardSupport.GuardReport report, String node, Path appJs) throws Exception {
        Path smoke = CodeQualityGuardSupport.projectRoot().resolve("build/tmp/webadmin-phase6-logic-chain-perf-smoke.js");
        Files.writeString(smoke, phase6SmokeHarness(), StandardCharsets.UTF_8);
        CodeQualityGuardSupport.CommandResult result = CodeQualityGuardSupport.runCommand(Duration.ofSeconds(45),
                node, smoke.toString(), appJs.toString());
        report.metric("performance.phase6.synthetic.exit", result.exitCode);
        if (result.exitCode != 0) {
            report.fail("Phase 6 Logic Chain synthetic render baseline failed: " + result.output);
            return;
        }
        Map<String, String> metrics = parseMetrics(result.output);
        for (Map.Entry<String, String> entry : metrics.entrySet()) {
            report.metric("performance.phase6." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Map<String, String>> scenario : PHASE6_DOM_BASELINES.entrySet()) {
            for (Map.Entry<String, String> expected : scenario.getValue().entrySet()) {
                requireMetric(report, metrics, "scenario." + scenario.getKey() + "." + expected.getKey(), expected.getValue());
            }
        }
        warnTiming(report, metrics, "initial", 50.0d);
        warnTiming(report, metrics, "selected", 16.0d);
        warnTiming(report, metrics, "hover", 16.0d);
        warnTiming(report, metrics, "edit", 50.0d);
        warnTiming(report, metrics, "draft", 50.0d);
        warnTiming(report, metrics, "unsaved", 50.0d);
        warnTiming(report, metrics, "vbd", 50.0d);
    }

    private static void runPhase912RealJsGraphBenchmarks(
            CodeQualityGuardSupport.GuardReport report,
            String node,
            Path appJs
    ) throws Exception {
        Path harness = CodeQualityGuardSupport.projectRoot().resolve("build/tmp/webadmin-phase912-real-js-graph-bench.js");
        Path metricsPath = CodeQualityGuardSupport.projectRoot().resolve("build/tmp/webadmin-phase912-real-js-graph-bench.metrics");
        Files.writeString(harness, phase912RealJsGraphHarness(), StandardCharsets.UTF_8);
        CodeQualityGuardSupport.CommandResult result = CodeQualityGuardSupport.runCommand(Duration.ofSeconds(240),
                node, harness.toString(), appJs.toString(), metricsPath.toString());
        report.metric("performance.phase912.real_js.exit", result.exitCode);
        if (result.exitCode != 0) {
            report.fail("9.1.2 real app.js Logic Chain graph benchmark failed: " + result.output);
            return;
        }
        String metricsOutput = Files.exists(metricsPath)
                ? Files.readString(metricsPath, StandardCharsets.UTF_8)
                : "";
        Map<String, Double> previousByCase = new LinkedHashMap<>();
        int rowCount = 0;
        for (String line : metricsOutput.split("\\R")) {
            if (!line.startsWith("BENCH;")) {
                continue;
            }
            rowCount++;
            Map<String, String> fields = parseSemicolonFields(line.substring("BENCH;".length()));
            SyntheticFixtureFactory.FixtureTier tier = tierById(fields.get("tier"));
            String operation = fields.getOrDefault("operation", "unknown");
            double ms = parseDouble(fields.get("ms"));
            int nodeCount = parseInt(fields.get("nodeCount"));
            int edgeCount = parseInt(fields.get("edgeCount"));
            int bytes = parseInt(fields.get("bytes"));
            int minimapSegments = parseInt(fields.get("minimapSegments"));
            String hash = fields.getOrDefault("hash", "");
            String error = fields.getOrDefault("error", "");
            report.require(error.isBlank(), "9.1.2 real JS graph benchmark error operation=" + operation
                    + " tier=" + tier.id + " error=" + error);
            report.require(nodeCount > 0, "9.1.2 real JS graph DOM node invariant missing operation=" + operation
                    + " tier=" + tier.id);
            report.require(edgeCount > 0 || tier.graphEdges == 0, "9.1.2 real JS graph DOM edge invariant missing operation=" + operation
                    + " tier=" + tier.id);
            if ("minimap".equals(operation)) {
                report.require(minimapSegments <= 24, "9.1.2 real JS minimap cap changed tier=" + tier.id
                        + " minimapSegments=" + minimapSegments);
            }
            String caseName = "logic_chain.real_js." + operation;
            Double previous = previousByCase.put(caseName, ms);
            SyntheticFixtureFactory.BenchmarkRow row = SyntheticFixtureFactory.benchmarkRow(
                    "webadmin_graph",
                    caseName,
                    tier,
                    tier.graphNodes,
                    tier.graphEdges,
                    ms,
                    previous,
                    realJsComplexity(operation),
                    "真实生成 app.js 在 Node VM 中执行 renderLogicChainViewer；DOM shape/hash 作为 Phase 1 大图硬不变量，timing 只报告低配估算。",
                    false,
                    Map.of(
                            "bytes", bytes,
                            "dom_node_count", nodeCount,
                            "dom_edge_count", edgeCount,
                            "dom_minimap_segments", minimapSegments,
                            "dom_signature_hash", hash,
                            "measurement_mode", "direct",
                            "direct_measured", true,
                            "operation_frequency", highFrequencyOperation(operation) ? "high_frequency_interaction" : "route_or_manual",
                            "dom_equivalence_required", true
                    )
            );
            report.metric("benchmark.webadmin_graph." + caseName + "." + tier.id, row.metricValue());
            if (!"PASS".equals(row.riskLevel())) {
                report.warning("9.1.2 real JS WebAdmin graph benchmark risk " + row.riskLevel()
                        + ": " + caseName + " tier=" + tier.id + " reason=" + row.reason());
            }
        }
        report.require(rowCount == 40, "9.1.2 real JS graph benchmark must emit 40 tier/operation rows; actual=" + rowCount);
    }

    private static void runPhase912JavaProxyGraphBenchmarks(CodeQualityGuardSupport.GuardReport report) {
        Map<String, Double> previousByCase = new LinkedHashMap<>();
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            SyntheticFixtureFactory.GraphFixture graph = SyntheticFixtureFactory.graph(tier);
            checkPhase912GraphShape(report, graph);
            runGraphOperation(report, previousByCase, graph, "logic_chain.java_proxy.initial_render", "O(nodes+edges)",
                    "Java proxy 只补充 fixture shape/summary，不替代真实 app.js DOM benchmark。",
                    () -> renderGraphSummary(graph, false, false));
            runGraphOperation(report, previousByCase, graph, "logic_chain.java_proxy.edit_mode_render", "O(nodes+edges+drafts)",
                    "Java proxy 只补充 edit/draft 摘要；真实 DOM 由 logic_chain.real_js.* 行覆盖。",
                    () -> renderGraphSummary(graph, true, false));
            runGraphOperation(report, previousByCase, graph, "logic_chain.java_proxy.hover_highlight", "O(edges)",
                    "Java proxy 只补充 hover 相关边摘要；真实 renderLogicChainViewer 另有 VM benchmark。",
                    () -> hoverSummary(graph));
            runGraphOperation(report, previousByCase, graph, "logic_chain.java_proxy.click_selection", "O(nodes+edges)",
                    "Java proxy 只补充 selection 摘要；真实右侧 panel/DOM 由 VM benchmark 采集。",
                    () -> clickSummary(graph));
            runGraphOperation(report, previousByCase, graph, "logic_chain.java_proxy.zoom_pan", "O(nodes)",
                    "Java proxy 只补充 transform checksum；真实 zoom/pan render 由 VM benchmark 采集。",
                    () -> zoomPanSummary(graph));
            runGraphOperation(report, previousByCase, graph, "logic_chain.java_proxy.drag_preview", "O(nodes)",
                    "Java proxy 只补充 legal/illegal slot 摘要；真实 app.js benchmark 另行输出。",
                    () -> dragPreviewSummary(graph));
            runGraphOperation(report, previousByCase, graph, "logic_chain.java_proxy.vbd_trigger_overlay", "O(edges)",
                    "Java proxy 只补充 VBD source/target 摘要；真实 VBD overlay DOM 由 VM benchmark 采集。",
                    () -> vbdOverlaySummary(graph));
            runGraphOperation(report, previousByCase, graph, "logic_chain.java_proxy.unsaved_diff_expanded", "O(drafts+deletes)",
                    "Java proxy 只补充 diff 计数；真实 diff banner HTML 由 VM benchmark 采集。",
                    () -> diffSummary(graph));
            runGraphOperation(report, previousByCase, graph, "logic_chain.java_proxy.minimap", "O(min(segments,24))",
                    "Java proxy 只补充 segment cap；真实 minimap DOM 由 VM benchmark 采集。",
                    () -> minimapSummary(graph));
        }
    }

    private static void checkPhase912GraphShape(CodeQualityGuardSupport.GuardReport report,
                                                SyntheticFixtureFactory.GraphFixture graph) {
        report.require(graph.complete(), "9.1.2 synthetic graph fixture incomplete for tier=" + graph.tier().id);
        for (Map.Entry<String, Boolean> entry : graph.coverage().entrySet()) {
            report.require(Boolean.TRUE.equals(entry.getValue()),
                    "9.1.2 graph fixture missing coverage " + entry.getKey() + " tier=" + graph.tier().id);
        }
        report.metric("benchmark.webadmin_graph.fixture." + graph.tier().id + ".nodes", graph.nodes().size());
        report.metric("benchmark.webadmin_graph.fixture." + graph.tier().id + ".edges", graph.edges().size());
        report.metric("benchmark.webadmin_graph.fixture." + graph.tier().id + ".segments", graph.segments().size());
    }

    private static void runGraphOperation(
            CodeQualityGuardSupport.GuardReport report,
            Map<String, Double> previousByCase,
            SyntheticFixtureFactory.GraphFixture graph,
            String caseName,
            String complexity,
            String reason,
            java.util.function.Supplier<String> operation
    ) {
        List<String> outputs = new ArrayList<>(3);
        long nanos = SyntheticFixtureFactory.measureNanos(() -> outputs.add(operation.get()));
        String output = outputs.isEmpty() ? "" : outputs.get(0);
        double ms = SyntheticFixtureFactory.nanosToMillis(nanos);
        Double previous = previousByCase.put(caseName, ms);
        SyntheticFixtureFactory.BenchmarkRow row = SyntheticFixtureFactory.benchmarkRow(
                "webadmin_graph",
                caseName,
                graph.tier(),
                graph.nodes().size(),
                graph.edges().size(),
                ms,
                previous,
                complexity,
                reason,
                false,
                Map.of(
                        "bytes", SyntheticFixtureFactory.utf8Bytes(output),
                        "operation_frequency", caseName.contains("hover") || caseName.contains("drag") || caseName.contains("zoom")
                                ? "high_frequency_interaction"
                                : "route_or_manual",
                        "dom_equivalence_required", true
                )
        );
        report.metric("benchmark.webadmin_graph." + caseName + "." + graph.tier().id, row.metricValue());
        if (!"PASS".equals(row.riskLevel())) {
            report.warning("9.1.2 WebAdmin graph benchmark risk " + row.riskLevel()
                    + ": " + caseName + " tier=" + graph.tier().id + " reason=" + row.reason());
        }
    }

    private static String renderGraphSummary(SyntheticFixtureFactory.GraphFixture graph, boolean editMode, boolean selected) {
        StringBuilder builder = new StringBuilder(graph.nodes().size() * 32 + graph.edges().size() * 24);
        for (SyntheticFixtureFactory.GraphNode node : graph.nodes()) {
            builder.append(node.id()).append('|').append(node.type()).append('|')
                    .append(node.column()).append(',').append(node.row());
            if (editMode && node.draft()) {
                builder.append("|draft");
            }
            if (node.pendingDelete()) {
                builder.append("|pending-delete");
            }
            if (selected && node.id().equals(graph.selectedNodeId())) {
                builder.append("|selected");
            }
            builder.append('\n');
        }
        for (SyntheticFixtureFactory.GraphEdge edge : graph.edges()) {
            builder.append(edge.from()).append("->").append(edge.to()).append('|').append(edge.type()).append('\n');
        }
        return builder.toString();
    }

    private static String hoverSummary(SyntheticFixtureFactory.GraphFixture graph) {
        Set<String> related = graph.relatedIds(graph.selectedNodeId());
        return graph.selectedNodeId() + "|related=" + related.size() + "|edges=" + graph.edges().size();
    }

    private static String clickSummary(SyntheticFixtureFactory.GraphFixture graph) {
        return renderGraphSummary(graph, false, true) + "panel=" + graph.selectedNodeId()
                + "|related=" + graph.relatedIds(graph.selectedNodeId()).size();
    }

    private static String zoomPanSummary(SyntheticFixtureFactory.GraphFixture graph) {
        double zoom = 1.25d;
        int panX = 80;
        int panY = -40;
        long checksum = 0L;
        for (SyntheticFixtureFactory.GraphNode node : graph.nodes()) {
            checksum += Math.round((node.column() * 220 + panX) * zoom);
            checksum += Math.round((node.row() * 120 + panY) * zoom);
        }
        return "zoom=1.25|pan=80,-40|checksum=" + checksum;
    }

    private static String dragPreviewSummary(SyntheticFixtureFactory.GraphFixture graph) {
        int legal = 0;
        int illegal = 0;
        for (SyntheticFixtureFactory.GraphNode node : graph.nodes()) {
            if ((node.column() + node.row()) % 5 == 0) {
                illegal++;
            } else {
                legal++;
            }
        }
        return "legal=" + legal + "|illegal=" + illegal;
    }

    private static String vbdOverlaySummary(SyntheticFixtureFactory.GraphFixture graph) {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (SyntheticFixtureFactory.GraphEdge edge : graph.edges()) {
            if ("vbd_trigger".equals(edge.type())) {
                count++;
                if (builder.length() == 0) {
                    builder.append("source=").append(edge.from())
                            .append("|triggerKey=right_click")
                            .append("|target=").append(edge.to());
                }
            }
        }
        return builder.append("|count=").append(count).toString();
    }

    private static String diffSummary(SyntheticFixtureFactory.GraphFixture graph) {
        int drafts = 0;
        int deletes = 0;
        for (SyntheticFixtureFactory.GraphNode node : graph.nodes()) {
            if (node.draft()) {
                drafts++;
            }
            if (node.pendingDelete()) {
                deletes++;
            }
        }
        return "drafts=" + drafts + "|pendingDelete=" + deletes + "|expanded=true";
    }

    private static String minimapSummary(SyntheticFixtureFactory.GraphFixture graph) {
        return String.join(",", graph.segments().stream().limit(24).toList())
                + "|segmentCount=" + Math.min(24, graph.segments().size())
                + "|pointerEvents=none";
    }

    private static void requireMetric(CodeQualityGuardSupport.GuardReport report, Map<String, String> metrics,
                                      String key, String expected) {
        String actual = metrics.get(key);
        report.require(expected.equals(actual), "Phase 6 DOM equivalence metric changed for " + key
                + ": expected=" + expected + " actual=" + actual);
    }

    private static void warnTiming(CodeQualityGuardSupport.GuardReport report, Map<String, String> metrics,
                                   String scenario, double softLimitMs) {
        String raw = metrics.get("timing." + scenario + ".ms");
        if (raw == null || raw.isBlank()) {
            report.fail("Phase 6 synthetic " + scenario + " timing metric missing");
            return;
        }
        try {
            double value = Double.parseDouble(raw);
            if (value > softLimitMs) {
                report.warning("Phase 6 synthetic " + scenario + " render exceeded soft timing target: "
                        + value + "ms > " + softLimitMs + "ms");
            }
        } catch (NumberFormatException ignored) {
            report.warning("Phase 6 synthetic " + scenario + " timing was not numeric: " + raw);
        }
    }

    private static Map<String, String> parseMetrics(String output) {
        Map<String, String> metrics = new LinkedHashMap<>();
        for (String line : output.split("\\R")) {
            int index = line.indexOf('=');
            if (index <= 0) {
                continue;
            }
            metrics.put(line.substring(0, index).trim(), line.substring(index + 1).trim());
        }
        return metrics;
    }

    private static Map<String, String> parseSemicolonFields(String value) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String part : value.split(";")) {
            int index = part.indexOf('=');
            if (index <= 0) {
                continue;
            }
            fields.put(part.substring(0, index), part.substring(index + 1));
        }
        return fields;
    }

    private static SyntheticFixtureFactory.FixtureTier tierById(String id) {
        for (SyntheticFixtureFactory.FixtureTier tier : SyntheticFixtureFactory.tiers()) {
            if (tier.id.equals(id)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("Unknown fixture tier: " + id);
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value == null ? "0" : value);
        } catch (NumberFormatException ignored) {
            return 0.0d;
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "0" : value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String realJsComplexity(String operation) {
        return switch (operation) {
            case "hover_highlight", "vbd_trigger_overlay" -> "O(nodes+edges)";
            case "minimap" -> "O(nodes+edges)+O(min(segments,24))";
            case "drag_preview" -> "O(nodes+edges+drafts)";
            default -> "O(nodes+edges)";
        };
    }

    private static boolean highFrequencyOperation(String operation) {
        return operation.contains("hover") || operation.contains("drag") || operation.contains("zoom") || operation.contains("click");
    }

    private static Map<String, Map<String, String>> phase6DomBaselines() {
        Map<String, Map<String, String>> baselines = new LinkedHashMap<>();
        putScenario(baselines, "initial", "5", "4", "3", "3", "0", "0", "6", "0", "false",
                "17f9c3d0091d0b6d");
        putScenario(baselines, "selected", "5", "4", "3", "3", "5", "4", "6", "0", "false",
                "f0379248a5b961d2");
        putScenario(baselines, "hover", "5", "4", "3", "3", "7", "2", "6", "0", "false",
                "0a17ab3b1d5376c6");
        putScenario(baselines, "edit", "5", "4", "3", "3", "0", "0", "6", "0", "false",
                "c1fd5bcc6e014cab");
        putScenario(baselines, "draft", "7", "5", "4", "4", "0", "0", "6", "8", "false",
                "839ed60a0b4b0250");
        putScenario(baselines, "unsaved", "7", "5", "4", "4", "0", "0", "6", "9", "false",
                "c1495383aac0995c");
        putScenario(baselines, "vbd", "6", "4", "4", "4", "3", "7", "6", "9", "true",
                "e2e15cb55f58bf43");
        putExtraScenario(baselines, "vbd",
                "vbdStableIdentity", "1",
                "vbdNoDuplicate", "1",
                "vbdTargetChannelOnly", "1",
                "vbdSourceCard", "1",
                "vbdSourceNodeIds", "vbd:one",
                "vbdTriggerKeys", "right_click",
                "vbdSelectedTrigger", "right_click",
                "vbdDraftSourceNodeId", "vbd:one");
        putExtraScenario(baselines, "pendingDelete",
                "hash.dom", "9ef14a29a4e8768a",
                "pendingDeleteCard", "9",
                "pendingDeleteBadge", "4",
                "pendingDeleteDiff", "5",
                "savePayloadPendingDeleteLeak", "false");
        putExtraScenario(baselines, "vbdFallback",
                "hash.dom", "e2e15cb55f58bf43",
                "vbdSourceCard", "1",
                "vbdSourceNodeIds", "vbd:one",
                "vbdTriggerKeys", "right_click",
                "vbdSelectedTrigger", "right_click",
                "vbdDraftSourceNodeId", "");
        putExtraScenario(baselines, "vbdSourcePriority",
                "hash.dom", "7d6699f95a8680f7",
                "vbdSourceCard", "1",
                "vbdSourceNodeIds", "vbd:one",
                "vbdTriggerKeys", "right_click",
                "vbdSelectedTrigger", "right_click",
                "vbdDraftSourceNodeId", "vbd:one");
        putExtraScenario(baselines, "minimapCap", "hash.dom", "aae5ee535b805d59", "minimapSegments", "24");
        return baselines;
    }

    private static void checkPhase7SourceRatchets(CodeQualityGuardSupport.GuardReport report) throws Exception {
        String canvas = CodeQualityGuardSupport.read("src/main/java/com/zcpu/tzzmod/webadmin/WebAdminLogicChainCanvasScripts.java");
        report.requireContains(canvas, "const relatedIndex=logicChainRelatedNodeIndex(graph);",
                "Phase 7 related index remains render-local");
        report.requireContains(canvas, "logicChainPositionedNode(item,nodes,graph,relatedIndex)",
                "Phase 7 related index is passed into node cards rather than stored on graph");
        report.requireContains(canvas, "(graph?.segments||[]).slice(0,24).map(seg=>[String(seg?.channel||''),Number(seg?.downstreamChannels?.length||0)]",
                "Phase 7 minimap key remains capped to first 24 segment channel/downstream counts");
        report.requireContains(canvas, "let logicChainMinimapMemo={key:'',html:''};",
                "Phase 7 minimap memo stores key/html only");
        report.require(!canvas.contains("logicChainMinimapMemo={key,html,graph")
                        && !canvas.contains("logicChainMinimapMemo={graph"),
                "Phase 7 minimap memo must not store graph references");
        report.requireContains(canvas, "function focusLogicChainNodeDetail(id){const next=String(id||'');"
                        + "appState.logicChainCanvas.selectedNodeId=next;"
                        + "appState.logicChainCanvas.focusNodeId=next;"
                        + "appState.logicChainCanvas.selectionPinned=!!next;"
                        + "appState.logicChainCanvas.detailOpen=true;"
                        + "if(appState.currentLogicChainGraph)renderLogicChainViewer(",
                "Phase 7 defers selection-local optimization until full DOM interaction guard exists");
        report.requireContains(canvas, "function highlightRelatedEdges(id){const next=id||'';"
                        + "if(appState.logicChainCanvas.hoverNodeId===next)return;"
                        + "appState.logicChainCanvas.hoverNodeId=next;"
                        + "if(appState.currentLogicChainGraph)renderLogicChainViewer(",
                "Phase 7 defers hover class-only optimization until arrow ownership is guarded");
        report.requireContains(canvas, "function setLogicChainZoom(delta){appState.logicChainCanvas.zoom=Math.max(.45,Math.min(1.8,Number(appState.logicChainCanvas.zoom||1)+Number(delta||0)));"
                        + "if(appState.currentLogicChainGraph)renderLogicChainViewer(",
                "Phase 7 defers zoom transform-only optimization until toolbar/pan equivalence is guarded");
    }

    private static String phase912RealJsGraphHarness() {
        return """
                const fs=require('fs');
                const vm=require('vm');
                const crypto=require('crypto');
                const code=fs.readFileSync(process.argv[2],'utf8');
                const outputPath=process.argv[3];
                const rows=[];
                function emit(line){rows.push(line);}
                function makeEl(){return {innerHTML:'',dataset:{},className:'',style:{},children:[],scrollTop:0,scrollLeft:0,addEventListener(){},removeEventListener(){},querySelector(){return null;},querySelectorAll(){return [];},closest(){return null;},getAttribute(){return null;},setAttribute(){},focus(){},classList:{add(){},remove(){},contains(){return false;},toggle(){}}};}
                const view=makeEl();
                const document={body:makeEl(),documentElement:makeEl(),addEventListener(){},removeEventListener(){},querySelector(){return null;},querySelectorAll(){return [];},getElementById(id){return id==='app-view'?view:null;},createElement(){return makeEl();}};
                const context={console,document,window:null,globalThis:null,navigator:{onLine:true},location:{hash:'#/logic-chains'},localStorage:{getItem(){return null;},setItem(){},removeItem(){}},addEventListener(){},removeEventListener(){},setTimeout,clearTimeout,setInterval,clearInterval,requestAnimationFrame:(cb)=>{cb();return 1;},cancelAnimationFrame(){},performance:{now:()=>Date.now()},URL,URLSearchParams,TextEncoder,TextDecoder,fetch:async()=>({ok:true,json:async()=>({})})};
                context.window=context;
                context.globalThis=context;
                vm.createContext(context);
                vm.runInContext(code+`
                ;globalThis.__phase912Run=function(graph,operation){
                  appState.me={username:'Owner',role:'OWNER'};
                  appState.currentLogicChainGraph=graph;
                  const firstNode=(graph.nodes||[])[Math.min((graph.nodes||[]).length-1,Math.floor((graph.nodes||[]).length/3))]||{};
                  const firstVbd=(graph.nodes||[]).find(n=>String(n.refType||'')==='virtual_block_device')||firstNode;
                  appState.logicChainCanvas={zoom:operation==='zoom_pan'?1.35:1,panX:operation==='zoom_pan'?120:0,panY:operation==='zoom_pan'?-60:0,selectedNodeId:'',focusNodeId:'',hoverNodeId:'',detailOpen:true,graphKey:'CHANNEL:root',collapsedChannels:{},viewMode:'BOTH',nodeTypeFilter:'ALL',routeInfo:{fallback:'#/logic-chains'}};
                  appState.logicChainEditor=null;
                  if(operation==='hover_highlight')appState.logicChainCanvas.hoverNodeId=firstNode.id||'channel:root';
                  if(operation==='click_selection'||operation==='vbd_trigger_overlay')appState.logicChainCanvas.selectedNodeId=firstVbd.id||firstNode.id||'channel:root';
                  if(operation==='click_selection'||operation==='vbd_trigger_overlay')appState.logicChainCanvas.focusNodeId=appState.logicChainCanvas.selectedNodeId;
                  if(operation==='click_selection'||operation==='vbd_trigger_overlay')appState.logicChainCanvas.selectionPinned=true;
                  if(operation==='edit_mode_render'||operation==='draft_overlay'||operation==='unsaved_diff_expanded'||operation==='drag_preview'){
                    appState.logicChainEditor={active:true,routeHash:'#/logic-chains',rootType:'CHANNEL',rootRef:'root',includeDisabled:true,maxDepth:6,lockId:'lock',lockLost:false,baseGraphFingerprint:'base-fp',nodes:[{id:'draft:listener',type:'signal_listener',displayName:'Draft Listener',label:'Draft Listener',column:'C2',slot:1,placed:true,enabled:true,signalListener:{actions:[{type:'message',value:'hello',enabled:true,_pendingDelete:operation==='unsaved_diff_expanded'}]}}],edges:[{from:'channel:root',to:'draft:listener',type:'consumes',label:'draft consumes',metadata:{draft:true,newEdge:true}}],draftChannels:[{channel:'draft.out',displayName:'Draft Out',metadataDraft:true}],existingNodeEdits:[],actionEdits:[],nodeDeletes:operation==='unsaved_diff_expanded'?[{nodeId:firstNode.id||'channel:root',displayName:'Delete Candidate'}]:[],actionDeletes:[],actionReorders:[],dirty:operation!=='edit_mode_render',errors:[],connectionMode:operation==='drag_preview'?'signal_listener':'',saving:false,diffExpanded:operation==='unsaved_diff_expanded'};
                  }
                  if(operation==='vbd_trigger_overlay'){
                    const native={deviceId:'vbd-synthetic',originalJson:'{}',values:{interactionEnabled:true,interactChannel:'draft.out',containerChangeEnabled:true,containerChangeChannel:'container.out'}};
                    const draft={kind:'virtual_block_device',targetId:'vbd-synthetic',deviceId:'vbd-synthetic',displayName:'VBD Synthetic',confirmed:true,sourceNodeId:firstVbd.id||'',original:{},virtualBlockDevice:{selectedTriggerType:'right_click',nativeTriggerDraft:native,itemSubmitRequirements:[{displayName:'Item One',count:1,consumeCount:1}]}};
                    appState.logicChainEditor={active:true,routeHash:'#/logic-chains',rootType:'CHANNEL',rootRef:'root',includeDisabled:true,maxDepth:6,lockId:'lock',lockLost:false,baseGraphFingerprint:'base-fp',nodes:[],edges:[],draftChannels:[{channel:'draft.out',displayName:'Draft Out',metadataDraft:true},{channel:'container.out',displayName:'Container Out',metadataDraft:true}],existingNodeEdits:[draft],existingEdit:draft,actionEdits:[],nodeDeletes:[],actionDeletes:[],actionReorders:[],dirty:true,errors:[],connectionMode:'',saving:false,diffExpanded:true};
                  }
                  renderLogicChainViewer(graph,{fallback:'#/logic-chains'},{silent:true});
                  return document.getElementById('app-view').innerHTML||'';
                };`,context,{filename:'webadmin-app.js'});
                const tiers=[['small',20,30],['medium',100,200],['large',500,1000],['stress',2000,5000]];
                const operations=['initial_render','edit_mode_render','hover_highlight','click_selection','zoom_pan','drag_preview','draft_overlay','vbd_trigger_overlay','unsaved_diff_expanded','minimap'];
                function lcg(seed){let s=seed>>>0;return ()=>{s=(Math.imul(s,1664525)+1013904223)>>>0;return s/4294967296;};}
                function makeGraph(tier,nodesCount,edgesCount){
                  const rand=lcg(912012+nodesCount);
                  const nodes=[{id:'producer:root',type:'producer',label:'Root',channel:'root',enabled:true,metadata:{nodeKind:'primary'}},{id:'channel:root',type:'channel',label:'Root Channel',channel:'root',enabled:true,metadata:{nodeKind:'primary'}}];
                  const cycle=['signal_listener','action','virtual_block_device','timer','signal_join','region_controller','state_variable','condition_group','channel'];
                  for(let i=2;i<nodesCount;i++){
                    const kind=cycle[i%cycle.length];
                    const node={id:`${kind}:${i}`,type:'consumer',refType:kind,label:`节点 ${i}`,channel:`synthetic.channel.${i%97}`,enabled:i%13!==0,metadata:{synthetic:true,nodeKind:'primary'}};
                    if(kind==='action')node.type='action';
                    if(kind==='virtual_block_device'){node.type='producer';node.refId='vbd-synthetic';node.metadata.deviceId='vbd-synthetic';node.metadata.kind='virtual_block_device';}
                    if(kind==='channel'){node.type='channel';node.refType='channel';}
                    nodes.push(node);
                  }
                  const edges=[];
                  for(let i=0;i<Math.min(edgesCount,nodes.length-1);i++){
                    edges.push({from:nodes[i%nodes.length].id,to:nodes[(i+1)%nodes.length].id,type:i%7===0?'vbd_outputs_channel':'consumes',label:`edge ${i}`,pathGroupId:i%5===0?'draft':''});
                  }
                  for(let i=edges.length;i<edgesCount;i++){
                    const fromIndex=Math.floor(rand()*Math.max(1,nodes.length-1));
                    const toIndex=Math.min(nodes.length-1,fromIndex+1+Math.floor(rand()*Math.max(1,nodes.length-fromIndex-1)));
                    const from=nodes[fromIndex];
                    const to=nodes[toIndex];
                    if(from&&to&&from.id!==to.id)edges.push({from:from.id,to:to.id,type:i%11===0?'executes':'consumes',label:`edge ${i}`});
                  }
                  const segmentCount=Math.max(30,Math.ceil(nodesCount/20));
                  const segments=Array.from({length:segmentCount},(_,i)=>({channel:`segment.${tier}.${i}`,downstreamChannels:Array.from({length:i%5},(__,j)=>`segment.${tier}.${i}.${j}`)}));
                  return {id:`synthetic-${tier}`,componentId:`synthetic-${tier}`,displayName:`Synthetic ${tier}`,root:{id:'producer:root',type:'producer',label:'Root',channel:'root'},metadata:{rootType:'CHANNEL',rootRef:'root'},stats:{},segments,nodes,edges};
                }
                function count(s,needle){return String(s||'').split(needle).length-1;}
                function sig(html){
                  const compact=String(html||'').replace(/\\s+/g,' ').slice(0,400000);
                  return crypto.createHash('sha256').update(compact).digest('hex').slice(0,16);
                }
                function safe(value){return String(value||'').replace(/[;\\r\\n=]/g,'_').slice(0,180);}
                for(const [tier,nodes,edges] of tiers){
                  for(const operation of operations){
                    const graph=makeGraph(tier,nodes,edges);
                    const start=process.hrtime.bigint();
                    try{
                      const html=context.__phase912Run(graph,operation);
                      const ms=Number(process.hrtime.bigint()-start)/1e6;
                      const row={ms:ms.toFixed(3),bytes:Buffer.byteLength(html,'utf8'),nodeCount:count(html,'logic-chain-node-card'),edgeCount:count(html,'logic-chain-edge'),minimapSegments:count(html,'logic-chain-minimap-segment'),hash:sig(html)};
                      emit(`BENCH;operation=${operation};tier=${tier};ms=${row.ms};bytes=${row.bytes};nodeCount=${row.nodeCount};edgeCount=${row.edgeCount};minimapSegments=${row.minimapSegments};hash=${row.hash};error=`);
                    }catch(error){
                      const ms=Number(process.hrtime.bigint()-start)/1e6;
                      emit(`BENCH;operation=${operation};tier=${tier};ms=${ms.toFixed(3)};bytes=0;nodeCount=0;edgeCount=0;minimapSegments=0;hash=error;error=${safe(error&&error.stack?error.stack:error)}`);
                    }
                  }
                }
                fs.writeFileSync(outputPath,rows.join('\\n'),'utf8');
                console.log(`rows=${rows.length}`);
                """;
    }

    private static void putScenario(Map<String, Map<String, String>> baselines, String name,
                                    String nodeCount, String edgeCount, String markerEnd, String arrowOwner,
                                    String related, String dimmed, String minimapSegments, String diffCount,
                                    String vbdOverlay, String domHash) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("nodeCount", nodeCount);
        values.put("edgeCount", edgeCount);
        values.put("markerEnd", markerEnd);
        values.put("arrowOwner", arrowOwner);
        values.put("related", related);
        values.put("dimmed", dimmed);
        values.put("minimapSegments", minimapSegments);
        values.put("diffCount", diffCount);
        values.put("vbdOverlay", vbdOverlay);
        values.put("hash.dom", domHash);
        baselines.put(name, values);
    }

    private static void putExtraScenario(Map<String, Map<String, String>> baselines, String name, String... pairs) {
        Map<String, String> values = baselines.computeIfAbsent(name, ignored -> new LinkedHashMap<>());
        for (int index = 0; index + 1 < pairs.length; index += 2) {
            values.put(pairs[index], pairs[index + 1]);
        }
    }

    private static String phase6SmokeHarness() {
        return """
                const fs=require('fs');
                const vm=require('vm');
                const crypto=require('crypto');
                const code=fs.readFileSync(process.argv[2],'utf8');
                function makeEl(){return {innerHTML:'',dataset:{},className:'',style:{},children:[],scrollTop:0,scrollLeft:0,addEventListener(){},removeEventListener(){},querySelector(){return null;},querySelectorAll(){return [];},closest(){return null;},getAttribute(){return null;},setAttribute(){},focus(){},classList:{add(){},remove(){},contains(){return false;},toggle(){}}};}
                const view=makeEl();
                const document={body:makeEl(),documentElement:makeEl(),addEventListener(){},removeEventListener(){},querySelector(){return null;},querySelectorAll(){return [];},getElementById(id){return id==='app-view'?view:null;},createElement(){return makeEl();}};
                const context={console,document,window:null,globalThis:null,navigator:{onLine:true},location:{hash:'#/logic-chains'},localStorage:{getItem(){return null;},setItem(){},removeItem(){}},addEventListener(){},removeEventListener(){},setTimeout,clearTimeout,setInterval,clearInterval,requestAnimationFrame:(cb)=>{cb();return 1;},cancelAnimationFrame(){},performance:{now:()=>Date.now()},URL,URLSearchParams,TextEncoder,TextDecoder,fetch:async()=>({ok:true,json:async()=>({})})};
                context.window=context;
                context.globalThis=context;
                vm.createContext(context);
                vm.runInContext(code+`
                ;globalThis.__phase6Run=function(graph,scenario){
                  appState.me={username:'Owner',role:'OWNER'};
                  appState.currentLogicChainGraph=graph;
                  appState.logicChainCanvas={zoom:1,panX:0,panY:0,selectedNodeId:'',focusNodeId:'',hoverNodeId:'',detailOpen:true,graphKey:'CHANNEL:root',collapsedChannels:{},viewMode:'BOTH',nodeTypeFilter:'ALL',routeInfo:{fallback:'#/logic-chains'}};
                  appState.logicChainEditor=null;
                  if(scenario==='selected'){appState.logicChainCanvas.selectedNodeId='listener:one';appState.logicChainCanvas.focusNodeId='listener:one';appState.logicChainCanvas.selectionPinned=true;}
                  if(scenario==='hover'){appState.logicChainCanvas.hoverNodeId='channel:root';}
                  if(scenario==='edit'){appState.logicChainEditor={active:true,routeHash:'#/logic-chains',rootType:'CHANNEL',rootRef:'root',includeDisabled:true,maxDepth:3,lockId:'lock',lockLost:false,baseGraphFingerprint:'base-fp',nodes:[],edges:[],draftChannels:[],existingNodeEdits:[],actionEdits:[],nodeDeletes:[],actionDeletes:[],actionReorders:[],dirty:false,errors:[],connectionMode:'',saving:false};}
                  if(scenario==='draft'||scenario==='unsaved'){
                    appState.logicChainEditor={active:true,routeHash:'#/logic-chains',rootType:'CHANNEL',rootRef:'root',includeDisabled:true,maxDepth:3,lockId:'lock',lockLost:false,baseGraphFingerprint:'base-fp',nodes:[{id:'draft:join',type:'signal_join',displayName:'Draft Join',label:'Draft Join',column:'C2',slot:1,placed:true,enabled:true,mode:'ALL',threshold:2,scopeMode:'GLOBAL',resetPolicy:'RESET_AFTER_EMIT',timeoutTicks:0,cooldownTicks:0}],edges:[{from:'channel:root',to:'draft:join',type:'join_input',label:'draft input',metadata:{draft:true,newEdge:true}}],draftChannels:[{channel:'draft.out',displayName:'Draft Out',cardDraft:true,metadataDraft:true}],existingNodeEdits:[],actionEdits:[],nodeDeletes:[],actionDeletes:[],actionReorders:[],dirty:true,errors:[],connectionMode:'',saving:false,diffExpanded:scenario==='unsaved'};
                  }
                  if(scenario==='pendingDelete'){
                    appState.logicChainCanvas.selectedNodeId='draft:action:draft:listener:listener:default:0';appState.logicChainCanvas.focusNodeId='draft:action:draft:listener:listener:default:0';appState.logicChainCanvas.selectionPinned=true;
                    appState.logicChainEditor={active:true,routeHash:'#/logic-chains',rootType:'CHANNEL',rootRef:'root',includeDisabled:true,maxDepth:3,lockId:'lock',lockLost:false,baseGraphFingerprint:'base-fp',nodes:[{id:'draft:listener',type:'signal_listener',displayName:'Draft Listener',label:'Draft Listener',column:'C2',slot:1,placed:true,enabled:true,signalListener:{actions:[{type:'message',value:'hello',enabled:true,_pendingDelete:true}]}},{id:'draft:timer',type:'timer',displayName:'Draft Timer',label:'Draft Timer',column:'C2',slot:2,placed:true,enabled:true,timer:{onStartActions:[{type:'message',value:'start',enabled:true,_pendingDelete:true}],onTickActions:[{type:'message',value:'tick',enabled:true,_pendingDelete:true}],onCompleteActions:[{type:'message',value:'done',enabled:true,_pendingDelete:true}],onCancelActions:[{type:'message',value:'cancel',enabled:true,_pendingDelete:true}]}}],edges:[{from:'channel:root',to:'draft:listener',type:'consumes',label:'draft consumes',metadata:{draft:true,newEdge:true}},{from:'draft:timer',to:'channel:root',type:'timer_outputs_channel',label:'timer output',metadata:{draft:true,newEdge:true}}],draftChannels:[],existingNodeEdits:[],actionEdits:[],nodeDeletes:[],actionDeletes:[],actionReorders:[],dirty:true,errors:[],connectionMode:'',saving:false,diffExpanded:true};
                  }
                  if(scenario==='vbd'||scenario==='vbdFallback'||scenario==='vbdSourcePriority'){
                    const native={deviceId:'vbd-one',originalJson:'{}',values:{interactionEnabled:true,interactChannel:'draft.out',containerChangeEnabled:true,containerChangeChannel:'container.out'}};
                    const d={kind:'virtual_block_device',targetId:'vbd-one',deviceId:'vbd-one',displayName:'VBD One',confirmed:true,original:{},virtualBlockDevice:{selectedTriggerType:'right_click',nativeTriggerDraft:native,itemSubmitRequirements:[{displayName:'Item One',count:1,consumeCount:1}]}};
                    if(scenario!=='vbdFallback')d.sourceNodeId='vbd:one';
                    appState.logicChainCanvas.selectedNodeId=scenario==='vbdSourcePriority'?'vbd:two':'vbd:one';appState.logicChainCanvas.selectionPinned=true;
                    appState.logicChainEditor={active:true,routeHash:'#/logic-chains',rootType:'CHANNEL',rootRef:'root',includeDisabled:true,maxDepth:3,lockId:'lock',lockLost:false,baseGraphFingerprint:'base-fp',nodes:[],edges:[],draftChannels:[{channel:'draft.out',displayName:'Draft Out',metadataDraft:true}],existingNodeEdits:[d],existingEdit:d,actionEdits:[],nodeDeletes:[],actionDeletes:[],actionReorders:[],dirty:true,errors:[],connectionMode:'',saving:false,diffExpanded:true};
                  }
                  renderLogicChainViewer(graph,{fallback:'#/logic-chains'},{silent:true});
                  const html=document.getElementById('app-view').innerHTML;
                  let payloadText='';
                  try{payloadText=appState.logicChainEditor?.active?JSON.stringify(logicChainEditorSaveBody()):'';}catch(_err){payloadText='PAYLOAD_ERROR';}
                  const vbdDraft=(appState.logicChainEditor?.existingNodeEdits||[]).find(d=>String(d?.kind||'').toLowerCase()==='virtual_block_device')||null;
                  let vbdTriggerKeys='',vbdSelectedTrigger='',vbdDraftSourceNodeId='';
                  if(vbdDraft){const draft=logicChainVbdStoredNativeTriggerDraft(vbdDraft);vbdSelectedTrigger=String(vbdDraft?.virtualBlockDevice?.selectedTriggerType||logicChainExistingVbdSelectedTrigger(vbdDraft)||'');vbdDraftSourceNodeId=String(vbdDraft.sourceNodeId||'');try{vbdTriggerKeys=logicChainVbdOutputRowsForSelectedTrigger(vbdDraft,draft).map(row=>String(row.triggerKey||row.type||'')).join(',');}catch(_ignored){}}
                  return {html,savePayloadPendingDeleteLeak:payloadText.includes('_pendingDelete')?'true':'false',vbdTriggerKeys,vbdSelectedTrigger,vbdDraftSourceNodeId};
                };`,context,{filename:'webadmin-app.js'});
                const graph={id:'synthetic',componentId:'synthetic',displayName:'Synthetic Chain',root:{id:'producer:root',type:'producer',label:'Root',channel:'root'},metadata:{rootType:'CHANNEL',rootRef:'root'},stats:{},segments:[{channel:'root',downstreamChannels:['draft.out','container.out']},{channel:'draft.out',downstreamChannels:[]},{channel:'container.out',downstreamChannels:[]},{channel:'extra.1',downstreamChannels:[]},{channel:'extra.2',downstreamChannels:[]},{channel:'extra.3',downstreamChannels:[]}],nodes:[{id:'producer:root',type:'producer',label:'Root',channel:'root',enabled:true,metadata:{nodeKind:'primary'}},{id:'channel:root',type:'channel',label:'Root Channel',channel:'root',enabled:true,metadata:{nodeKind:'primary'}},{id:'listener:one',type:'consumer',refType:'signal_listener',label:'Listener',channel:'root',enabled:true,metadata:{}},{id:'action:one',type:'action',label:'Action',enabled:true,metadata:{}},{id:'vbd:one',type:'producer',refType:'virtual_block_device',refId:'vbd-one',label:'VBD One',channel:'root',enabled:true,metadata:{deviceId:'vbd-one',kind:'virtual_block_device',nodeKind:'primary'}}],edges:[{from:'producer:root',to:'channel:root',type:'emits',label:'emits'},{from:'channel:root',to:'listener:one',type:'consumes',label:'consumes'},{from:'listener:one',to:'action:one',type:'executes',label:'executes'},{from:'vbd:one',to:'channel:root',type:'vbd_outputs_channel',label:'vbd output',pathGroupId:'draft'}]};
                function graphForScenario(scenario){const copy=JSON.parse(JSON.stringify(graph));if(scenario==='vbdSourcePriority'){copy.nodes.push({id:'vbd:two',type:'producer',refType:'virtual_block_device',refId:'vbd-one',label:'VBD Two',channel:'root',enabled:true,metadata:{deviceId:'vbd-one',kind:'virtual_block_device',nodeKind:'primary'}});copy.edges.push({from:'vbd:two',to:'channel:root',type:'vbd_outputs_channel',label:'vbd selected output',pathGroupId:'draft'});}if(scenario==='minimapCap')copy.segments=Array.from({length:30},(_,i)=>({channel:`cap.${i}`,downstreamChannels:Array.from({length:i%4},(__,j)=>`cap.${i}.${j}`)}));return copy;}
                function sha(s){return crypto.createHash('sha256').update(s).digest('hex').slice(0,16);}
                function count(s,re){return (s.match(re)||[]).length;}
                function sig(html){
                  function attr(tag,name){const m=tag.match(new RegExp(name+'="([^"]*)"'));return m?m[1]:'';}
                  const pathTags=[...html.matchAll(/<path class="logic-chain-edge[^"]*"[^>]*>/g)].map(m=>m[0]);
                  const paths=pathTags.map(tag=>attr(tag,'d')+'|marker='+(attr(tag,'marker-end')||'0')+'|owner='+(tag.includes('data-logic-chain-target-arrow-owner="true"')?'1':'0'));
                  const edgeAttrs=pathTags.map(tag=>`${attr(tag,'class')}|group=${attr(tag,'data-logic-chain-edge-path-group')}|visual=${attr(tag,'data-logic-chain-edge-visual-style')}|shape=${attr(tag,'data-logic-chain-route-shape')}|marker=${attr(tag,'marker-end')||''}`);
                  const nodes=[...html.matchAll(/<div class="logic-chain-tree-node[^"]*"[^>]*data-logic-chain-node-id="([^"]*)"[^>]*style="left:([^;]+);top:([^;]+);width:([^;]+);height:([^;"]+)/g)].map(m=>`${m[1]}@${m[2]},${m[3]},${m[4]},${m[5]}`);
                  const cardTags=[...html.matchAll(/<div role="button"[^>]*class="logic-chain-node-card[^"]*"[^>]*>/g)].map(m=>m[0]);
                  const cards=cardTags.map(tag=>`${attr(tag,'data-logic-chain-node-id')}:${attr(tag,'class').replace(/\\s+/g,' ').trim()}`);
                  const cardAttrs=cardTags.map(tag=>`${attr(tag,'data-logic-chain-node-id')}|type=${attr(tag,'data-node-type')}|action=${attr(tag,'data-logic-chain-node-action')}|primary=${attr(tag,'data-logic-chain-primary-node-id')}|pending=${tag.includes('data-logic-chain-pending-delete-card="true"')?'1':'0'}|vbdSource=${tag.includes('data-logic-chain-vbd-trigger-source-card-draft="true"')?'1':'0'}`);
                  const vbdSourceNodeIds=cardTags.filter(tag=>tag.includes('data-logic-chain-vbd-trigger-source-card-draft="true"')).map(tag=>attr(tag,'data-logic-chain-node-id')).join(',');
                  const mini=(html.match(/<div class="logic-chain-minimap"[^>]*>([\\s\\S]*?)<\\/div><\\/div>/)||['',''])[1];
                  const panel=(html.match(/<aside class="logic-chain-right"[^>]*>([\\s\\S]*?)<\\/aside>/)||['',''])[1];
                  const diff=(html.match(/<section class="logic-chain-draft-diff-banner[\\s\\S]*?<\\/section>/)||[''])[0];
                  return {nodeCount:nodes.length,edgeCount:paths.length,markerEnd:count(html,/marker-end=/g),arrowOwner:count(html,/data-logic-chain-target-arrow-owner="true"/g),related:count(html,/ related/g),dimmed:count(html,/ dimmed/g),minimapSegments:count(mini,/logic-chain-minimap-segment/g),diffCount:count(html,/data-logic-chain-draft-diff/g),vbdOverlay:html.includes('data-logic-chain-vbd-trigger-graph-render-before-save="true"'),pendingDeleteCard:count(html,/data-logic-chain-pending-delete-card="true"/g),pendingDeleteBadge:count(html,/data-logic-chain-pending-delete-badge="true"/g),pendingDeleteDiff:count(html,/data-logic-chain-draft-action-pending-delete-diff="true"/g),vbdStableIdentity:count(html,/data-logic-chain-vbd-trigger-stable-identity="true"/g),vbdNoDuplicate:count(html,/data-logic-chain-vbd-trigger-no-duplicate-card="true"/g),vbdTargetChannelOnly:count(html,/data-logic-chain-vbd-trigger-target-channel-only="true"/g),vbdSourceCard:count(html,/data-logic-chain-vbd-trigger-source-card-draft="true"/g),vbdSourceNodeIds,hash:{dom:sha([paths.join('\\n'),edgeAttrs.join('\\n'),nodes.join('\\n'),cards.join('\\n'),cardAttrs.join('\\n'),mini,panel,diff].join('\\n---\\n'))}};
                }
                for(const scenario of ['initial','selected','hover','edit','draft','unsaved','vbd','pendingDelete','vbdFallback','vbdSourcePriority','minimapCap']){
                  const start=process.hrtime.bigint();
                  const result=context.__phase6Run(graphForScenario(scenario),scenario);
                  const html=result.html||String(result||'');
                  const ms=Number(process.hrtime.bigint()-start)/1e6;
                  const s=sig(html);
                  const originalScenario=new Set(['initial','selected','hover','edit','draft','unsaved','vbd']).has(scenario);
                  const baseKeys=new Set(['nodeCount','edgeCount','markerEnd','arrowOwner','related','dimmed','minimapSegments','diffCount','vbdOverlay']);
                  const extraKeys={pendingDelete:['pendingDeleteCard','pendingDeleteBadge','pendingDeleteDiff'],vbd:['vbdStableIdentity','vbdNoDuplicate','vbdTargetChannelOnly','vbdSourceCard','vbdSourceNodeIds'],vbdFallback:['vbdSourceCard','vbdSourceNodeIds'],vbdSourcePriority:['vbdSourceCard','vbdSourceNodeIds'],minimapCap:['minimapSegments']};
                  const hashKeys=new Set(['dom']);
                  for(const [key,value] of Object.entries(s)){
                  if(key==='hash'){for(const [hashKey,hashValue] of Object.entries(value)){if(hashKeys.has(hashKey))console.log(`scenario.${scenario}.hash.${hashKey}=${hashValue}`);}}
                    else if((originalScenario&&baseKeys.has(key))||(extraKeys[scenario]||[]).includes(key))console.log(`scenario.${scenario}.${key}=${value}`);
                  }
                  if(scenario==='pendingDelete')console.log(`scenario.${scenario}.savePayloadPendingDeleteLeak=${result.savePayloadPendingDeleteLeak||'false'}`);
                  if(scenario==='vbd'||scenario==='vbdFallback'||scenario==='vbdSourcePriority'){
                    console.log(`scenario.${scenario}.vbdTriggerKeys=${result.vbdTriggerKeys||''}`);
                    console.log(`scenario.${scenario}.vbdSelectedTrigger=${result.vbdSelectedTrigger||''}`);
                    console.log(`scenario.${scenario}.vbdDraftSourceNodeId=${result.vbdDraftSourceNodeId||''}`);
                  }
                  console.log(`timing.${scenario}.ms=${ms.toFixed(3)}`);
                }
                """;
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
