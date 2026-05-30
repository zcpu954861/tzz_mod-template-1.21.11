package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.action.schema.ActionCapabilityMatrixTest;
import com.zcpu.tzzmod.action.schema.ActionSchemaRegistryTest;
import com.zcpu.tzzmod.action.validation.ActionValidationServiceTest;
import com.zcpu.tzzmod.webadmin.WebAdminFrontendAssets;
import com.zcpu.tzzmod.webadmin.service.WebAdminActionSummaryServiceTest;

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
    private static final int FRONTEND_SCRIPT_MODULE_MAX_BYTES = 400_000;
    private static final int FRONTEND_STYLE_MODULE_MAX_BYTES = 400_000;
    private static final int NEW_BACKEND_SERVICE_MAX_BYTES = 160_000;
    private static final int GUARD_CLASS_MAX_BYTES = 200_000;
    private static final int WEBADMIN_LOGIC_CHAIN_EDITOR_SERVICE_PHASE7_LINE_BASELINE = 5_005;
    private static final int STABILIZATION_GUARD_TEST_PHASE7_LINE_BASELINE = 12_430;

    private static final Map<String, FileBaseline> KNOWN_FILE_BASELINES = Map.of(
            WEBADMIN_FRONTEND_SCRIPTS, new FileBaseline(8433, 1_984_343),
            "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendStyles.java", new FileBaseline(75, 123_798),
            "src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorService.java",
            new FileBaseline(WEBADMIN_LOGIC_CHAIN_EDITOR_SERVICE_PHASE7_LINE_BASELINE, 305_271),
            "src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminVirtualBlockDeviceNativeTriggerService.java",
            new FileBaseline(1_464, 80_967),
            "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminServer.java", new FileBaseline(3102, 164_014),
            "src/main/java/com/zcpu/tzzmod/webadmin/draft/WebAdminProtectedDraftRegistry.java", new FileBaseline(620, 26_369),
            "src/test/java/com/zcpu/tzzmod/stabilization/StabilizationGuardTest.java",
            new FileBaseline(STABILIZATION_GUARD_TEST_PHASE7_LINE_BASELINE, 826_376)
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

    private static final Map<String, FunctionBaseline> GIANT_JS_FUNCTION_BASELINES = Map.ofEntries(
            Map.entry("logicChainExistingDeviceForm", new FunctionBaseline(16, 16_471)),
            Map.entry("logicChainDefaultDraftChannelAnchor", new FunctionBaseline(59, 15_015)),
            Map.entry("realtimeRouteKeysForEvent", new FunctionBaseline(127, 14_857)),
            Map.entry("showLogicChainNewNodeModal", new FunctionBaseline(2, 12_387)),
            Map.entry("showLogicChainPlacedDraftNodeEditModal", new FunctionBaseline(1, 10_319)),
            Map.entry("renderDeviceDetail", new FunctionBaseline(85, 9_356)),
            Map.entry("interactionItemMatcherForm", new FunctionBaseline(32, 9_081)),
            Map.entry("startDeviceConfigEdit", new FunctionBaseline(81, 6_481))
    );

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
        ActionCapabilityMatrixTest.run();
        ActionSchemaRegistryTest.run();
        ActionValidationServiceTest.run();
        WebAdminActionSummaryServiceTest.run();
        WebAdminActionOwnerMigrationGuardTest.run(report);
        WebAdminActionEditorFrontendGuardTest.run(report);
        WebAdminActionSummaryGuardTest.run(report);
        WebAdminFrontendBundleGuardTest.run(report);
        WebAdminPerformanceBaselineGuardTest.run(report);
        WebAdminLogicChainDomEquivalenceGuardTest.run(report);
        RuntimePerformanceBaselineGuardTest.run(report);
        RuntimeOptimizationEquivalenceGuardTest.run(report);
        StorePerformanceBaselineGuardTest.run(report);
        DocsConsistencyGuardTest.run(report);
        report.printAndFail();
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws IOException {
        Path root = CodeQualityGuardSupport.projectRoot();
        report.metric("branch.scope", "phase7-codebase-health-guard-ratchet");
        checkKnownFileBaselines(report);
        checkLogicChainBackendSplit(report, root);
        checkFrontendScriptsFacade(report);
        checkBeforeVxx(report);
        checkHotspotRedefinitions(report);
        checkModuleBudgets(report, root);
        reportLargeMethods(report, root);
        reportPhase75ComplexityMetrics(report, root);
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
        report.requireContains(facade, "data-webadmin-frontend-bundle-entry-only",
                "WebAdminFrontendScripts facade boundary marker");
        requireOrdered(report, facade,
                "WebAdminFrontendIconScripts.appJs()",
                "WebAdminFrontendCoreScripts.appJs()",
                "WebAdminFrontendCoreEventScripts.appJs()",
                "WebAdminFrontendPageScripts.appJs()",
                "WebAdminLogicChainViewerScripts.appJs()",
                "WebAdminLogicChainCanvasScripts.appJs()",
                "WebAdminLogicChainNodePanelScripts.appJs()",
                "WebAdminLogicChainCanvasScripts.stageAppJs()",
                "WebAdminLogicChainLayoutScripts.appJs()",
                "WebAdminLogicChainDraftOverlayScripts.appJs()",
                "WebAdminLogicChainCanvasScripts.renderAppJs()",
                "WebAdminLogicChainEditorScripts.appJs()",
                "WebAdminLogicChainVbdScripts.appJs()",
                "WebAdminLogicChainVbdOverlayScripts.appJs()",
                "WebAdminFrontendBootstrapScripts.appJs()");
        String editorFacade = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminLogicChainEditorScripts.java");
        report.requireContains(editorFacade, "WebAdminLogicChainDiffScripts.appJs()",
                "Logic Chain editor concat must include diff summary module at the original generated order");
        requireOrdered(report, editorFacade,
                "function confirmLogicChainActionEditDraft",
                "WebAdminLogicChainDiffScripts.appJs()",
                "function renderLogicChainActionAppendChannelCombo");
    }

    private static void checkLogicChainBackendSplit(CodeQualityGuardSupport.GuardReport report, Path root) throws IOException {
        // Phase 7 这里守的是结构边界而不是业务行为：saveDraft 仍由 facade 委托到 coordinator，
        // planner/executor 只冻结顺序和 mixed-write 边界，避免后续把跨 store 写入重新堆回巨型 service。
        String servicePath = "src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorService.java";
        String coordinatorPath = "src/main/java/com/zcpu/tzzmod/webadmin/service/LogicChainDraftSaveCoordinator.java";
        String plannerPath = "src/main/java/com/zcpu/tzzmod/webadmin/service/LogicChainDraftOperationPlanner.java";
        String executorPath = "src/main/java/com/zcpu/tzzmod/webadmin/service/LogicChainTypedWriteExecutor.java";
        String service = CodeQualityGuardSupport.read(servicePath);
        String coordinator = CodeQualityGuardSupport.read(coordinatorPath);
        String planner = CodeQualityGuardSupport.read(plannerPath);
        String executor = CodeQualityGuardSupport.read(executorPath);
        String editorTest = CodeQualityGuardSupport.read("src/test/java/com/zcpu/tzzmod/webadmin/service/WebAdminLogicChainEditorServiceTest.java");

        report.metric("backend.logic_chain_editor_service.lines_after_phase5", CodeQualityGuardSupport.lineCount(servicePath));
        report.metric("backend.logic_chain_save_coordinator.lines", CodeQualityGuardSupport.lineCount(coordinatorPath));
        report.metric("backend.logic_chain_operation_planner.lines", CodeQualityGuardSupport.lineCount(plannerPath));
        report.metric("backend.logic_chain_typed_executor.lines", CodeQualityGuardSupport.lineCount(executorPath));
        report.require(service.contains("private final LogicChainDraftSaveCoordinator saveCoordinator;"),
                "WebAdminLogicChainEditorService must hold the Phase 5 save coordinator facade");
        report.require(service.contains("return saveCoordinator.saveDraft(server, user, session, remoteAddress, request, csrfToken, sameOrigin);"),
                "saveDraft must delegate to LogicChainDraftSaveCoordinator");
        report.require(coordinator.contains("preflight -> editor lock -> fingerprint -> validate"),
                "LogicChainDraftSaveCoordinator must document the frozen saveDraft order");
        report.require(coordinator.contains("不把") && coordinator.contains("完整原子事务"),
                "LogicChainDraftSaveCoordinator must state that Phase 5 does not claim full cross-store atomicity");
        report.require(coordinator.contains("Coordinator 只编排保存流程边界")
                        && coordinator.contains("ServiceAdapter 是兼容旧 typed 写入口的桥"),
                "LogicChainDraftSaveCoordinator must document Phase 7 orchestration and adapter boundaries");
        report.require(executor.contains("按旧顺序调用 typed 写入口"),
                "LogicChainTypedWriteExecutor must document order-preserving typed execution");
        report.require(executor.contains("不重试、不重排、不写 channel metadata"),
                "LogicChainTypedWriteExecutor must document retry/reorder/channel metadata boundaries");
        requireOrdered(report, executor,
                "for (WebAdminLogicChainEditorRequest.DraftNode node : plan.draftNodes())",
                "if (plan.actionAppend() != null)",
                "for (WebAdminLogicChainEditorRequest.ExistingNodeEditDraft edit : plan.existingNodeEdits())",
                "for (WebAdminLogicChainEditorRequest.ActionEditDraft edit : plan.actionEdits())",
                "for (WebAdminLogicChainEditorRequest.ActionDeleteDraft delete : plan.actionDeletes())",
                "for (WebAdminLogicChainEditorRequest.ActionReorderDraft reorder : plan.actionReorders())",
                "for (WebAdminLogicChainEditorRequest.NodeDeleteDraft delete : plan.nodeDeletes())");
        report.require(planner.contains("channel metadata 仍是尾部独立边界"),
                "LogicChainDraftOperationPlanner must document the channel metadata tail boundary");
        report.require(planner.contains("Planner 只做草稿快照和存在性过滤")
                        && planner.contains("OperationPlan 前半段字段顺序就是 typed 写入顺序")
                        && planner.contains("channelMetadataDrafts 是尾部独立边界"),
                "LogicChainDraftOperationPlanner must document Phase 7 planner and OperationPlan boundaries");
        report.require(editorTest.contains("testDraftOperationPlannerPreservesTypedWriteOrderBoundaries"),
                "WebAdminLogicChainEditorServiceTest must cover Phase 5 operation planner ordering");
        String phase5Backend = service + "\n" + coordinator + "\n" + planner + "\n" + executor;
        report.require(!phase5Backend.contains("WebAdminMapServer"),
                "Phase 5 backend split must not introduce non-existent WebAdminMapServer coupling");
        report.require(!phase5Backend.contains("完整跨 store atomic transaction"),
                "Phase 5 backend split must not claim a full cross-store atomic transaction");
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
                report.fail(entry.getKey() + " count grew from " + entry.getValue() + " to " + actual);
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
            report.fail("BeforeV13-17 count grew from " + BEFORE_V13_TO_V17_TOTAL + " to " + v13ToV17);
        }
        if (allBeforeVxx > ALL_BEFORE_VXX_TOTAL) {
            report.fail("All BeforeVxx token count grew from " + ALL_BEFORE_VXX_TOTAL + " to " + allBeforeVxx);
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
                if (bytes > FRONTEND_SCRIPT_MODULE_MAX_BYTES) {
                    report.fail("Frontend script module exceeds Phase 7 byte budget: " + relative
                            + " bytes=" + bytes + " max=" + FRONTEND_SCRIPT_MODULE_MAX_BYTES);
                }
            }
            if (fileName.contains("Styles") && !fileName.equals("WebAdminFrontendStyles.java") && lines > 800) {
                report.fail("Frontend style module exceeds 800-line Phase 1 budget: " + relative + " lines=" + lines);
            }
            if (fileName.contains("Styles") && !fileName.equals("WebAdminFrontendStyles.java") && bytes > FRONTEND_STYLE_MODULE_MAX_BYTES) {
                report.fail("Frontend style module exceeds Phase 7 byte budget: " + relative
                        + " bytes=" + bytes + " max=" + FRONTEND_STYLE_MODULE_MAX_BYTES);
            }
            if (relative.startsWith("src/main/java/com/zcpu/tzzmod/webadmin/service/")
                    && fileName.endsWith(".java")
                    && !LEGACY_WEBADMIN_SERVICES_OVER_1000.contains(fileName)) {
                report.metric("backend.module.lines." + fileName, lines);
                report.metric("backend.module.bytes." + fileName, bytes);
                if (lines > 1000) {
                    report.fail("New backend module exceeds 1000-line Phase 7 budget: " + relative + " lines=" + lines);
                }
                if (bytes > NEW_BACKEND_SERVICE_MAX_BYTES) {
                    report.fail("New backend module exceeds Phase 7 byte budget: " + relative
                            + " bytes=" + bytes + " max=" + NEW_BACKEND_SERVICE_MAX_BYTES);
                }
            }
        }
        for (Path file : CodeQualityGuardSupport.javaFiles(root.resolve("src/test/java/com/zcpu/tzzmod/stabilization"))) {
            String fileName = file.getFileName().toString();
            String relative = root.relativize(file).toString().replace('\\', '/');
            long lines = CodeQualityGuardSupport.lineCount(relative);
            long bytes = CodeQualityGuardSupport.bytes(relative);
            if (!fileName.equals("StabilizationGuardTest.java")) {
                report.metric("guard.class.lines." + fileName, lines);
                report.metric("guard.class.bytes." + fileName, bytes);
                if (lines > 1000) {
                    report.fail("Guard class exceeds 1000-line Phase 1 budget: " + relative + " lines=" + lines);
                }
                if (bytes > GUARD_CLASS_MAX_BYTES) {
                    report.fail("Guard class exceeds Phase 7 byte budget: " + relative
                            + " bytes=" + bytes + " max=" + GUARD_CLASS_MAX_BYTES);
                }
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
        List<CodeQualityGuardSupport.MethodMetric> jsFunctions = CodeQualityGuardSupport.collectLargeJsFunctions(appJs, 200);
        report.metric("js.function.top.count", jsFunctions.size());
        if (jsFunctions.isEmpty()) {
            report.fail("JS function-length detector found no functions in generated app.js");
        }
        index = 1;
        for (CodeQualityGuardSupport.MethodMetric function : jsFunctions) {
            report.metric("js.function.top." + String.format("%03d", index),
                    function.lines + " lines " + function.chars + " chars " + function.name + " " + function.location);
            if (function.lines > 80 || function.chars > 8_000) {
                if (GIANT_JS_FUNCTION_BASELINES.containsKey(function.name)) {
                    report.warning("Grandfathered large JS function report-only: " + function.name + " "
                            + function.location + " lines=" + function.lines + " chars=" + function.chars);
                } else {
                    report.fail("New giant JS function detected after Phase 7 ratchet: " + function.name + " "
                            + function.location + " lines=" + function.lines + " chars=" + function.chars
                            + ". Split the handler/function or add an explicit ratchet baseline in a dedicated phase.");
                }
            }
            index++;
        }
        checkGiantJsFunctionNoGrowth(report, jsFunctions);
    }

    private static void reportPhase75ComplexityMetrics(CodeQualityGuardSupport.GuardReport report, Path root) throws IOException {
        String appJs = WebAdminFrontendAssets.appJs();
        reportMetricTable(report, "phase75.java.if_density.top",
                CodeQualityGuardSupport.collectJavaIfDensityMethods(root, 50));
        reportMetricTable(report, "phase75.js.if_density.top",
                CodeQualityGuardSupport.collectJsIfDensityFunctions(appJs, 50));
        reportMetricTable(report, "phase75.js.selector_density.top",
                CodeQualityGuardSupport.collectJsSelectorDensityFunctions(appJs, 50));
        reportMetricTable(report, "phase75.hotspot.interaction.top",
                CodeQualityGuardSupport.collectJsInteractionHotspots(appJs, 30));
        reportMetricTable(report, "phase75.hotspot.render.top",
                CodeQualityGuardSupport.collectJsRenderHotspots(appJs, 30));
        reportMetricTable(report, "phase75.hotspot.backend.top",
                CodeQualityGuardSupport.collectBackendValidationMethods(root, 30));
    }

    private static void reportMetricTable(CodeQualityGuardSupport.GuardReport report, String prefix,
                                          List<CodeQualityGuardSupport.MethodMetric> metrics) {
        report.metric(prefix + ".count", metrics.size());
        if (metrics.isEmpty()) {
            report.fail("Phase 7.5 complexity metric table is empty: " + prefix);
            return;
        }
        int index = 1;
        for (CodeQualityGuardSupport.MethodMetric metric : metrics) {
            report.metric(prefix + "." + String.format("%03d", index), metric.complexitySummary());
            index++;
        }
    }

    private static void checkGiantJsFunctionNoGrowth(CodeQualityGuardSupport.GuardReport report,
                                                     List<CodeQualityGuardSupport.MethodMetric> jsFunctions) {
        Map<String, CodeQualityGuardSupport.MethodMetric> byName = jsFunctions.stream()
                .collect(java.util.stream.Collectors.toMap(function -> function.name, function -> function, (left, right) -> left));
        for (Map.Entry<String, FunctionBaseline> entry : GIANT_JS_FUNCTION_BASELINES.entrySet()) {
            String name = entry.getKey();
            FunctionBaseline baseline = entry.getValue();
            CodeQualityGuardSupport.MethodMetric actual = byName.get(name);
            if (actual == null) {
                report.metric("js.function.ratchet." + name, "removed_or_split baselineLines="
                        + baseline.lines + " baselineChars=" + baseline.chars);
                continue;
            }
            report.metric("js.function.ratchet." + name, actual.lines + " lines " + actual.chars
                    + " chars baselineLines=" + baseline.lines + " baselineChars=" + baseline.chars);
            if (actual.lines > baseline.lines || actual.chars > baseline.chars) {
                report.fail("Known giant JS function grew after Phase 7 ratchet: " + name
                        + " lines=" + actual.lines + "/" + baseline.lines
                        + " chars=" + actual.chars + "/" + baseline.chars);
            }
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

    private static final class FunctionBaseline {
        final int lines;
        final int chars;

        FunctionBaseline(int lines, int chars) {
            this.lines = lines;
            this.chars = chars;
        }
    }
}
