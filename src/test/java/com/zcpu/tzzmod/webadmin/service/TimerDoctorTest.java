package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.scheduler.TimerDefinition;
import com.zcpu.tzzmod.scheduler.TimerMode;
import com.zcpu.tzzmod.scheduler.TimerScopeMode;
import com.zcpu.tzzmod.scheduler.TimerStartPolicy;
import com.zcpu.tzzmod.scheduler.TimerTargetMode;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TimerDoctorTest {
    private TimerDoctorTest() {
    }

    public static void run() throws Exception {
        testDefinitionDiagnostics();
        testTimerActionReferenceDiagnostics();
    }

    private static void testDefinitionDiagnostics() throws Exception {
        WebAdminTimerDoctorService service = new WebAdminTimerDoctorService();
        TimerDefinition pureStatus = timer("timer.status", TimerMode.DELAY);
        pureStatus.outputChannel = "";
        pureStatus.onCompleteActions = List.of();
        List<WebAdminDtos.DoctorIssueDto> issues = inspectDefinition(service, pureStatus);
        requireIssue(issues, "timer-no-output", "pure status timer warning");

        TimerDefinition startOnly = timer("timer.start-only", TimerMode.DELAY);
        startOnly.outputChannel = "";
        startOnly.onStartActions = List.of(ActionConfig.command("say start", false));
        startOnly.onCompleteActions = List.of();
        List<WebAdminDtos.DoctorIssueDto> startOnlyIssues = inspectDefinition(service, startOnly);
        requireIssue(startOnlyIssues, "timer-no-output", "start-only timer still warns when no tick/complete/output exists");

        TimerDefinition repeat = timer("timer.repeat", TimerMode.REPEAT);
        repeat.intervalTicks = 5;
        repeat.maxRuns = 0;
        List<WebAdminDtos.DoctorIssueDto> repeatIssues = inspectDefinition(service, repeat);
        requireIssue(repeatIssues, "timer-repeat-small-interval", "small repeat interval warning");
        requireIssue(repeatIssues, "timer-infinite-repeat-no-cancel-note", "infinite repeat cancellation warning");
    }

    private static void testTimerActionReferenceDiagnostics() throws Exception {
        WebAdminTimerDoctorService service = new WebAdminTimerDoctorService();
        Map<String, TimerDefinition> timers = new LinkedHashMap<>();
        TimerDefinition disabled = timer("timer.disabled", TimerMode.DELAY);
        disabled.enabled = false;
        TimerDefinition player = timer("timer.player", TimerMode.DELAY);
        player.scopeMode = TimerScopeMode.PLAYER;
        timers.put(disabled.id, disabled.normalized());
        timers.put(player.id, player.normalized());

        List<ActionConfig> actions = List.of(
                ActionConfig.timerStart("", TimerTargetMode.GLOBAL, "", null, ""),
                ActionConfig.timerStart("timer.missing", TimerTargetMode.GLOBAL, "", null, ""),
                ActionConfig.timerStart("timer.disabled", TimerTargetMode.GLOBAL, "", null, ""),
                ActionConfig.timerStart("timer.player", TimerTargetMode.CONTEXT_PLAYER, "", null, "")
        );
        List<WebAdminDtos.DoctorIssueDto> issues = inspectActions(service, timers, actions);
        requireIssue(issues, "timer-action-missing-id", "missing timerId diagnostic");
        requireIssue(issues, "timer-action-missing-target", "missing timer target diagnostic");
        requireIssue(issues, "timer-action-disabled-target", "disabled timer reference diagnostic");
        requireIssue(issues, "timer-action-player-context-missing", "PLAYER context diagnostic");
    }

    @SuppressWarnings("unchecked")
    private static List<WebAdminDtos.DoctorIssueDto> inspectDefinition(WebAdminTimerDoctorService service, TimerDefinition timer) throws Exception {
        Method method = WebAdminTimerDoctorService.class.getDeclaredMethod("inspectDefinition", TimerDefinition.class, List.class, net.minecraft.server.MinecraftServer.class);
        method.setAccessible(true);
        List<WebAdminDtos.DoctorIssueDto> issues = new ArrayList<>();
        method.invoke(service, timer.normalized(), issues, null);
        return issues;
    }

    @SuppressWarnings("unchecked")
    private static List<WebAdminDtos.DoctorIssueDto> inspectActions(
            WebAdminTimerDoctorService service,
            Map<String, TimerDefinition> timers,
            List<ActionConfig> actions
    ) throws Exception {
        Method method = WebAdminTimerDoctorService.class.getDeclaredMethod(
                "inspectActions",
                Map.class,
                List.class,
                String.class,
                String.class,
                List.class,
                ConditionRuntimeTargetType.class,
                String.class
        );
        method.setAccessible(true);
        List<WebAdminDtos.DoctorIssueDto> issues = new ArrayList<>();
        method.invoke(service, timers, issues, "ACTION_RELAY_ACTION", "relay-a", actions, ConditionRuntimeTargetType.ACTION_RELAY_ACTION, "#/devices/relay-a");
        return issues;
    }

    private static TimerDefinition timer(String id, TimerMode mode) {
        TimerDefinition timer = new TimerDefinition();
        timer.id = id;
        timer.displayName = id;
        timer.enabled = true;
        timer.mode = mode;
        timer.scopeMode = TimerScopeMode.GLOBAL;
        timer.durationTicks = 40;
        timer.intervalTicks = mode == TimerMode.DELAY ? 0 : 20;
        timer.maxRuns = mode == TimerMode.REPEAT ? 3 : 1;
        timer.startPolicy = TimerStartPolicy.RESTART;
        timer.outputChannel = "timer.done";
        timer.onCompleteActions = List.of(ActionConfig.command("say done", false));
        return timer.normalized();
    }

    private static void requireIssue(List<WebAdminDtos.DoctorIssueDto> issues, String codePrefix, String message) {
        for (WebAdminDtos.DoctorIssueDto issue : issues) {
            if (String.valueOf(issue.id()).contains(codePrefix)) {
                requireTrue(containsChinese(issue.message()), message + " Chinese diagnostic");
                return;
            }
        }
        throw new AssertionError(message + " missing issue prefix=" + codePrefix + " issues=" + issues);
    }

    private static boolean containsChinese(String value) {
        return value != null && value.chars().anyMatch(ch -> ch >= 0x4E00 && ch <= 0x9FFF);
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
