package com.zcpu.tzzmod.scheduler;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.signal.SignalChannel;
import java.util.ArrayList;
import java.util.List;

public final class TimerValidator {
    public static final long MAX_DURATION_TICKS = 20L * 60L * 60L * 24L;
    public static final long MAX_INTERVAL_TICKS = 20L * 60L * 60L;
    public static final int MAX_RUNS = 100_000;
    public static final int MAX_ACTIONS_PER_LIST = 64;

    private TimerValidator() {
    }

    public static List<TimerValidationIssue> validate(TimerDefinition raw, boolean creating) {
        TimerDefinition source = raw == null ? new TimerDefinition() : raw;
        TimerDefinition timer = source.normalized();
        List<TimerValidationIssue> issues = new ArrayList<>();
        if (timer.id.isBlank()) {
            issues.add(issue("id", "timer_id_required", "Timer ID 不能为空，且只能包含小写字母、数字、_、-、.、:。", raw == null ? "" : raw.id));
        }
        if (timer.displayName.isBlank()) {
            issues.add(issue("displayName", "timer_name_required", "Timer 名称不能为空。", raw == null ? "" : raw.displayName));
        }
        if (source.mode == null) {
            issues.add(issue("mode", "timer_mode_invalid", "模式必须是 DELAY、COUNTDOWN 或 REPEAT。", ""));
        }
        if (source.scopeMode == null) {
            issues.add(issue("scopeMode", "timer_scope_invalid", "作用域必须选择 GLOBAL 或 PLAYER。", ""));
        }
        if (source.startPolicy == null) {
            issues.add(issue("startPolicy", "timer_start_policy_invalid", "启动策略必须是 RESTART、IGNORE_IF_RUNNING 或 FAIL_IF_RUNNING。", ""));
        }
        TimerMode mode = source.mode == null ? timer.mode : source.mode;
        if ((mode == TimerMode.DELAY || mode == TimerMode.COUNTDOWN)
                && (source.durationTicks < 0 || source.durationTicks > MAX_DURATION_TICKS)) {
            issues.add(issue("durationTicks", "timer_duration_invalid", "总时长 tick 必须在 0 到 1728000 之间。", String.valueOf(source.durationTicks)));
        }
        if ((mode == TimerMode.COUNTDOWN || mode == TimerMode.REPEAT)
                && (source.intervalTicks <= 0 || source.intervalTicks > MAX_INTERVAL_TICKS)) {
            issues.add(issue("intervalTicks", "timer_interval_invalid", "COUNTDOWN / REPEAT 的触发间隔 tick 必须在 1 到 72000 之间。", String.valueOf(source.intervalTicks)));
        }
        if (mode == TimerMode.DELAY && (source.intervalTicks < 0 || source.intervalTicks > MAX_INTERVAL_TICKS)) {
            issues.add(issue("intervalTicks", "timer_interval_invalid", "触发间隔 tick 必须在 0 到 72000 之间。", String.valueOf(source.intervalTicks)));
        }
        if (mode == TimerMode.REPEAT && (source.maxRuns < 0 || source.maxRuns > MAX_RUNS)) {
            issues.add(issue("maxRuns", "timer_max_runs_invalid", "重复次数必须在 0 到 100000 之间，0 表示无限重复直到取消。", String.valueOf(source.maxRuns)));
        }
        if (!timer.outputChannel.isBlank() && !SignalChannel.isValid(timer.outputChannel)) {
            issues.add(issue("outputChannel", "timer_output_channel_invalid", "输出频道只能包含小写字母、数字、_、-、.、:；也可以留空。", raw == null ? "" : raw.outputChannel));
        }
        validateActions("onStartActions", timer.onStartActions, issues);
        if (mode != TimerMode.DELAY) {
            validateActions("onTickActions", timer.onTickActions, issues);
        }
        validateActions("onCompleteActions", timer.onCompleteActions, issues);
        validateActions("onCancelActions", timer.onCancelActions, issues);
        return List.copyOf(issues);
    }

    private static void validateActions(String field, List<ActionConfig> actions, List<TimerValidationIssue> issues) {
        List<ActionConfig> safeActions = actions == null ? List.of() : actions;
        if (safeActions.size() > MAX_ACTIONS_PER_LIST) {
            issues.add(issue(field, "timer_too_many_actions", "每个 Timer action list 最多支持 64 条动作。", String.valueOf(safeActions.size())));
        }
        for (int index = 0; index < safeActions.size(); index++) {
            ActionConfig action = safeActions.get(index);
            if (action == null) {
                issues.add(issue(field + "[" + index + "]", "timer_action_required", "Timer action 不能为空。", ""));
            } else if (action.enabled() && !action.isUsable()) {
                issues.add(issue(field + "[" + index + "]", "timer_action_unusable", "Timer action 未启用或缺少必要字段。", action.type() == null ? "" : action.type().id()));
            }
        }
    }

    private static TimerValidationIssue issue(String field, String code, String message, String rejectedValue) {
        return new TimerValidationIssue(field, code, message, rejectedValue);
    }
}
