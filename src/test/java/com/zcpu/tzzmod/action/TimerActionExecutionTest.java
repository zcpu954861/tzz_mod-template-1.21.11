package com.zcpu.tzzmod.action;

import com.zcpu.tzzmod.scheduler.TimerOperationResult;
import com.zcpu.tzzmod.scheduler.TimerStartPolicy;
import com.zcpu.tzzmod.scheduler.TimerTargetMode;
import com.zcpu.tzzmod.scheduler.TimerValidator;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.service.WebAdminActionRelayActionsService;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.text.Text;

public final class TimerActionExecutionTest {
    private TimerActionExecutionTest() {
    }

    public static void run() {
        testTimerActionConfigAndSummary();
        testTimerActionValidationFailures();
        testWebAdminTimerActionEntryRoundTrip();
        testActionEngineDispatchAndSourceDtoMarkers();
        testTimerOperationResultDetails();
    }

    private static void testTimerActionConfigAndSummary() {
        ActionConfig start = ActionConfig.timerStart(
                "Timer Start A",
                TimerTargetMode.CONTEXT_PLAYER,
                "",
                TimerStartPolicy.IGNORE_IF_RUNNING,
                "condition.group"
        );
        requireEquals(ActionType.TIMER_START, start.type(), "timer_start action type");
        requireEquals("timer-start-a", start.timerId(), "timer id normalized");
        requireEquals("context_player", start.timerTargetMode(), "timer target mode normalized");
        requireEquals("IGNORE_IF_RUNNING", start.timerStartPolicyOverride(), "timer start policy override normalized");
        requireTrue(start.isUsable(), "timer_start usable with timerId");
        requireTrue(start.timerActionSummary().contains("启动 Timer"), "timer_start summary is Chinese");
        requireTrue(start.timerFingerprint().contains("timerId=timer-start-a"), "timer fingerprint carries timer id");

        ActionConfig cancel = ActionConfig.timerCancel("timer.cancel", TimerTargetMode.EXPLICIT_TARGET, "player-a", "");
        requireEquals(ActionType.TIMER_CANCEL, cancel.type(), "timer_cancel action type");
        requireEquals("explicit_target", cancel.timerTargetMode(), "timer_cancel target mode normalized");
        requireEquals("player-a", cancel.timerTargetId(), "timer_cancel target id kept");
        requireTrue(cancel.timerActionSummary().contains("取消 Timer"), "timer_cancel summary is Chinese");

        ActionConfig missing = ActionConfig.timerStart("", TimerTargetMode.GLOBAL, "", null, "");
        requireFalse(missing.isUsable(), "timer action without timerId is not usable");
    }

    private static void testTimerActionValidationFailures() {
        requireTimerValidation(
                timerAction(ActionType.TIMER_START, "", "global", "", "", 0L, "noop_success"),
                "缺少 timerId",
                "missing timerId fails validation"
        );
        requireTimerValidation(
                timerAction(ActionType.TIMER_START, "timer.a", "nearby_player", "", "", 0L, "noop_success"),
                "目标模式无效",
                "invalid target mode fails validation"
        );
        requireTimerValidation(
                timerAction(ActionType.TIMER_START, "timer.a", "explicit_target", "", "", 0L, "noop_success"),
                "指定玩家目标不能为空",
                "explicit target without id fails validation"
        );
        requireTimerValidation(
                timerAction(ActionType.TIMER_START, "timer.a", "global", "", "restart_later", 0L, "noop_success"),
                "启动策略覆盖无效",
                "invalid start policy override fails validation"
        );
        requireTimerValidation(
                timerAction(ActionType.TIMER_START, "timer.a", "global", "", "", TimerValidator.MAX_DURATION_TICKS + 1L, "noop_success"),
                "时长覆盖超出允许范围",
                "duration override over max fails validation"
        );
        requireTimerValidation(
                timerAction(ActionType.TIMER_CANCEL, "timer.a", "global", "", "", 0L, "delete_config"),
                "缺失处理策略无效",
                "invalid cancel missing behavior fails validation"
        );
    }

    private static void testWebAdminTimerActionEntryRoundTrip() {
        WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        entry.type = "timer_start";
        entry.timerId = "Timer Round Trip";
        entry.timerTargetMode = "explicit_target";
        entry.timerTargetId = "player-a";
        entry.timerStartPolicyOverride = "FAIL_IF_RUNNING";
        entry.timerDurationOverrideTicks = 60L;
        entry.timerMissingBehavior = "noop_success";

        ActionConfig action = WebAdminActionRelayActionsService.actionFromEntry(entry);
        Map<String, Object> data = new LinkedHashMap<>();
        WebAdminActionRelayActionsService.putTimerActionFields(data, action);

        requireEquals("timer-round-trip", action.timerId(), "WebAdmin timer action normalizes timer id");
        requireEquals("explicit_target", data.get("timerTargetMode"), "WebAdmin timer action target mode roundtrips");
        requireEquals("player-a", data.get("timerTargetId"), "WebAdmin timer action target id roundtrips");
        requireEquals("FAIL_IF_RUNNING", data.get("timerStartPolicyOverride"), "WebAdmin timer action policy roundtrips");
        requireEquals(60L, data.get("timerDurationOverrideTicks"), "WebAdmin timer action duration override roundtrips");
        requireTrue(String.valueOf(data.get("timerActionSummary")).contains("启动 Timer"), "WebAdmin timer action summary is Chinese");
    }

    private static void testActionEngineDispatchAndSourceDtoMarkers() {
        String actionEngine = read("src/main/java/com/zcpu/tzzmod/action/ActionEngine.java");
        requireContains(actionEngine, "case TIMER_START -> TimerRuntimeService.startFromAction", "ActionEngine dispatches timer_start");
        requireContains(actionEngine, "case TIMER_CANCEL -> TimerRuntimeService.cancelFromAction", "ActionEngine dispatches timer_cancel");
        requireContains(actionEngine, "ActionAuditLogger.log(context, config, result)", "ActionEngine audits timer action results through common path");

        String signalListenerService = read("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminSignalListenerActionsService.java");
        String actionRelayService = read("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminActionRelayActionsService.java");
        String regionService = read("src/main/java/com/zcpu/tzzmod/webadmin/service/WebAdminRegionControllerService.java");
        requireContains(signalListenerService, "putTimerActionFields(entry, normalized)", "SignalListener timer actions expose timer DTO fields");
        requireContains(actionRelayService, "putTimerActionFields(entry, action)", "ActionRelay timer actions expose timer DTO fields");
        requireContains(regionService, "putTimerActionFields(entry, action)", "Region timer actions expose timer DTO fields");
    }

    private static void testTimerOperationResultDetails() {
        TimerOperationResult result = TimerOperationResult.success("timer_started", "Timer 已启动。", "timer.a", "global", true);
        ActionExecutionResult actionResult = ActionExecutionResult.timerOperation(result);
        requireTrue(actionResult.success(), "timer operation success maps to ActionExecutionResult");
        requireEquals("timer_started", actionResult.code(), "timer operation code preserved");
        requireEquals("timer.a", actionResult.details().get("timerId"), "timer operation details include timerId");
        requireEquals("global", actionResult.details().get("scopeKey"), "timer operation details include scopeKey");
        requireEquals(Boolean.TRUE, actionResult.details().get("changed"), "timer operation details include changed flag");

        ActionExecutionResult failed = ActionExecutionResult.timerOperation(TimerOperationResult.failure("timer_missing", "Timer 不存在。", "timer.missing", ""));
        requireFalse(failed.success(), "timer operation failure maps to failed action result");
        requireTrue(String.valueOf(failed.details().get("failureReason")).contains("Timer 不存在"), "timer failure reason is Chinese");
    }

    private static void requireTimerValidation(ActionConfig config, String expectedMessage, String message) {
        Text error = invokeTimerValidation(config);
        requireTrue(error != null, message + " returns validation error");
        requireContains(error.getString(), expectedMessage, message + " Chinese message");
    }

    private static Text invokeTimerValidation(ActionConfig config) {
        try {
            Method method = ActionValidator.class.getDeclaredMethod("validateTimerAction", ActionConfig.class);
            method.setAccessible(true);
            return (Text) method.invoke(null, config);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("failed to invoke timer action validation", exception);
        }
    }

    private static ActionConfig timerAction(
            ActionType type,
            String timerId,
            String targetMode,
            String targetId,
            String startPolicyOverride,
            long durationOverrideTicks,
            String missingBehavior
    ) {
        return new ActionConfig(
                type,
                "",
                true,
                false,
                0,
                false,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                0L,
                false,
                "",
                timerId,
                targetMode,
                targetId,
                startPolicyOverride,
                durationOverrideTicks,
                missingBehavior
        );
    }

    private static String read(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) {
            throw new AssertionError("failed to read " + relativePath, exception);
        }
    }

    private static void requireContains(String actual, String expected, String message) {
        requireTrue(actual != null && actual.contains(expected), message + " missing=" + expected);
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireFalse(boolean condition, String message) {
        requireTrue(!condition, message);
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
