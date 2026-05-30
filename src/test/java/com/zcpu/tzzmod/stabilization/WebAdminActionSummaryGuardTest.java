package com.zcpu.tzzmod.stabilization;

import com.zcpu.tzzmod.webadmin.WebAdminFrontendAssets;

public final class WebAdminActionSummaryGuardTest {
    private WebAdminActionSummaryGuardTest() {
    }

    public static void main(String[] args) throws Exception {
        CodeQualityGuardSupport.GuardReport report =
                new CodeQualityGuardSupport.GuardReport("9.2 typed action summary guard");
        run(report);
        report.printAndFail();
    }

    static void run(CodeQualityGuardSupport.GuardReport report) throws Exception {
        String appJs = WebAdminFrontendAssets.appJs();
        String pageFacade = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminFrontendPageScripts.java");
        String summaryScripts = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/WebAdminActionSummaryScripts.java");
        String backendSummary = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionSummaryService.java");
        String signalService = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminSignalService.java");
        String timerService = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminTimerService.java");
        String snapshotService = CodeQualityGuardSupport.read(
                "src/main/java/com/zcpu/tzzmod/webadmin/snapshot/WebAdminSnapshotService.java");

        report.requireContains(pageFacade, "WebAdminActionSummaryScripts.appJs()",
                "Action summary script module must load before owner editors");
        requireOrdered(report, pageFacade,
                "WebAdminActionFieldRenderScripts.appJs()",
                "WebAdminActionSummaryScripts.appJs()",
                "WebAdminFrontendDeviceEditorScripts.appJs()");
        for (String marker : new String[]{
                "function typedActionDisplaySummary",
                "function typedActionCleanSummary",
                "执行命令（已隐藏，长度",
                "设置状态变量",
                "发送信号到频道"
        }) {
            report.requireContains(appJs, marker, "frontend typed action summary marker");
        }
        report.require(!summaryScripts.contains("api("), "Action summary helper must not call WebAdmin APIs");
        report.require(!summaryScripts.contains("fetch("), "Action summary helper must not call fetch");
        report.require(!summaryScripts.contains("JSON.stringify"), "Action summary helper must not use JSON serialization as primary summary");

        for (String marker : new String[]{
                "displaySummary(ActionConfig action)",
                "auditSummary(ActionConfig action)",
                "displaySummaryList",
                "auditSummaryList",
                "<command redacted length="
        }) {
            report.requireContains(backendSummary, marker, "backend action summary service marker");
        }
        report.require(!backendSummary.contains("ActionConfig.toString"),
                "Backend summary must not use ActionConfig.toString as the primary summary");
        report.require(!signalService.contains("action.summary()"),
                "Signal channel downstream graph must not parse human action summary text");
        report.requireContains(signalService, "addActionChannels(channels, listener.actions())",
                "Signal downstream graph reads listener ActionConfig lists directly");
        report.requireContains(signalService, "action.type() == ActionType.SIGNAL",
                "Signal downstream graph filters typed signal actions directly");
        report.requireContains(signalService, "add(channels, action.value())",
                "Signal downstream graph uses action value as channel source");
        report.require(!timerService.contains("audit(context, result, TimerStore.summary"),
                "Timer audit path must not pass raw TimerStore action lists into audit summaries");
        report.requireContains(timerService, "private static Map<String, Object> auditSummary(TimerDefinition raw)",
                "Timer audit summary helper must own redacted action-list replacement");
        report.requireContains(timerService, "WebAdminActionSummaryService.auditSummaryList(timer.onCompleteActions)",
                "Timer audit summary must replace raw action lists with redacted summaries");
        report.require(CodeQualityGuardSupport.count(timerService, "TimerStore.summary(") == 2,
                "TimerStore.summary use must stay limited to detailMap response and the redacting auditSummary helper");
        report.requireContains(snapshotService, "actionListFieldDiffs",
                "Snapshot diff must expose action-list summary rows without changing storage shape");
    }

    private static void requireOrdered(CodeQualityGuardSupport.GuardReport report, String text, String... needles) {
        int previous = -1;
        for (String needle : needles) {
            int current = text.indexOf(needle);
            report.require(current >= 0, "Missing ordered marker `" + needle + "`");
            if (current >= 0 && previous >= 0 && current <= previous) {
                report.fail("Module order changed near `" + needle + "`");
            }
            previous = current;
        }
    }
}
