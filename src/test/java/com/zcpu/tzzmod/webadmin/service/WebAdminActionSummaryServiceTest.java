package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.state.StateVariableMutationOperation;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableTargetMode;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.scheduler.TimerStartPolicy;
import com.zcpu.tzzmod.scheduler.TimerTargetMode;
import java.util.List;

public final class WebAdminActionSummaryServiceTest {
    private WebAdminActionSummaryServiceTest() {
    }

    public static void run() {
        testEachActionTypeHasChineseSummary();
        testAuditRedactsCommandAndStateValue();
        testTimerAuditKeepsFingerprintAndEnabledState();
        testListHelpersNormalizeNullsAndDisabledPrefix();
    }

    private static void testEachActionTypeHasChineseSummary() {
        requireContains(WebAdminActionSummaryService.displaySummary(new ActionConfig(ActionType.COMMAND, "/say hello", true, true, 20, true, "gate.alpha")), "执行命令 /say hello", "command display summary");
        requireContains(WebAdminActionSummaryService.displaySummary(new ActionConfig(ActionType.MESSAGE, "欢迎进入大厅", true, false, 0, false)), "向玩家显示消息：欢迎进入大厅", "message display summary");
        requireContains(WebAdminActionSummaryService.displaySummary(new ActionConfig(ActionType.SOUND, "minecraft:block.note_block.pling", true, false, 0, false)), "播放音效：minecraft:block.note_block.pling", "sound display summary");
        requireContains(WebAdminActionSummaryService.displaySummary(ActionConfig.signal("game.next", false)), "发送信号到频道 game.next", "signal display summary");
        requireContains(WebAdminActionSummaryService.displaySummary(ActionConfig.stateVariable(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.GLOBAL,
                StateVariableTargetMode.GLOBAL,
                "",
                "game.ready",
                StateVariableType.BOOLEAN,
                "true",
                0,
                true,
                "false",
                ""
        )), "设置状态变量 game.ready = true", "state variable display summary");
        requireContains(WebAdminActionSummaryService.displaySummary(ActionConfig.timerStart(
                "timer.alpha",
                TimerTargetMode.GLOBAL,
                "",
                TimerStartPolicy.RESTART,
                ""
        )), "启动计时器 timer.alpha", "timer start display summary");
        requireContains(WebAdminActionSummaryService.displaySummary(ActionConfig.timerCancel(
                "timer.alpha",
                TimerTargetMode.GLOBAL,
                "",
                ""
        )), "取消计时器 timer.alpha", "timer cancel display summary");
    }

    private static void testAuditRedactsCommandAndStateValue() {
        ActionConfig command = new ActionConfig(ActionType.COMMAND, "/say ultra-secret-command-value", true, false, 0, false);
        String audit = WebAdminActionSummaryService.auditSummary(command);
        requireContains(audit, "<command redacted length=", "command audit redaction marker");
        requireFalse(audit.contains("ultra-secret-command-value"), "command audit does not expose command body");

        ActionConfig state = ActionConfig.stateVariable(
                StateVariableMutationOperation.SET_VARIABLE,
                StateVariableScope.PLAYER,
                StateVariableTargetMode.EXPLICIT_TARGET,
                "player-one",
                "secret.score",
                StateVariableType.STRING,
                "private-state-value",
                0,
                true,
                "initial-secret",
                "gate.state"
        );
        String stateAudit = WebAdminActionSummaryService.auditSummary(state);
        requireContains(stateAudit, "stateValue=<redacted length=", "state value audit redaction marker");
        requireFalse(stateAudit.contains("private-state-value"), "state audit does not expose state value");
        requireFalse(stateAudit.contains("initial-secret"), "state audit does not expose initial value");
    }

    private static void testTimerAuditKeepsFingerprintAndEnabledState() {
        ActionConfig disabledStart = timerAction(ActionType.TIMER_START, false, "timer.audit", "restart", 80L, "noop_success");
        String startAudit = WebAdminActionSummaryService.auditSummary(disabledStart);
        requireContains(startAudit, "已禁用", "timer start audit keeps disabled prefix");
        requireContains(startAudit, "timerId=timer.audit", "timer start audit keeps timer fingerprint");
        requireContains(startAudit, "timerDurationOverrideTicks=80", "timer start audit keeps duration override fingerprint");
        requireContains(startAudit, "enabled=false", "timer start audit keeps enabled state");

        ActionConfig cancel = timerAction(ActionType.TIMER_CANCEL, true, "timer.audit", "", 0L, "fail_if_missing");
        String cancelAudit = WebAdminActionSummaryService.auditSummary(cancel);
        requireContains(cancelAudit, "取消计时器 timer.audit", "timer cancel audit summary label");
        requireContains(cancelAudit, "timerMissingBehavior=fail_if_missing", "timer cancel audit keeps missing behavior fingerprint");
        requireContains(cancelAudit, "enabled=true", "timer cancel audit keeps enabled state");
    }

    private static void testListHelpersNormalizeNullsAndDisabledPrefix() {
        ActionConfig disabled = new ActionConfig(ActionType.SIGNAL, "done.channel", false, false, 0, false);
        List<String> summaries = WebAdminActionSummaryService.displaySummaryList(List.of(disabled));
        requireEquals(1, summaries.size(), "display summary list size");
        requireContains(summaries.getFirst(), "已禁用", "disabled action display prefix");
        requireContains(WebAdminActionSummaryService.auditSummaryList(List.of(disabled)).getFirst(), "enabled=false", "audit summary keeps enabled state");
        requireEquals("未知动作", WebAdminActionSummaryService.displaySummary(null), "null display action summary");
    }

    private static void requireContains(String text, String marker, String message) {
        if (text == null || !text.contains(marker)) {
            throw new AssertionError(message + " missing=" + marker + " text=" + text);
        }
    }

    private static ActionConfig timerAction(
            ActionType type,
            boolean enabled,
            String timerId,
            String startPolicy,
            long durationOverride,
            String missingBehavior
    ) {
        return new ActionConfig(
                type,
                "",
                enabled,
                false,
                0,
                false,
                "gate.timer",
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
                TimerTargetMode.GLOBAL.id(),
                "",
                startPolicy,
                durationOverride,
                missingBehavior
        );
    }

    private static void requireFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
