package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.state.StateVariableMutationOperation;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableTargetMode;
import com.zcpu.tzzmod.scheduler.TimerStartPolicy;
import com.zcpu.tzzmod.scheduler.TimerTargetMode;
import java.util.List;

/**
 * Typed Action 摘要层只服务 WebAdmin 展示和审计：输入仍是现有 ActionConfig，
 * 输出用于卡片、diff、snapshot 和 audit 的中文文本。
 *
 * 这里不执行 action、不做保存校验、不读 store，也不改写 JSON payload；运行时语义仍由
 * ActionEngine 和各 owner service 负责。未来新增 action type 时，必须同时补 schema、
 * capability、validation、editor、summary、docs 和 guard，避免重新分散到 owner 本地分支。
 */
public final class WebAdminActionSummaryService {
    private static final int DISPLAY_LIMIT = 96;

    private WebAdminActionSummaryService() {
    }

    public static String displaySummary(ActionConfig action) {
        return summary(action, false);
    }

    public static String auditSummary(ActionConfig action) {
        return summary(action, true);
    }

    public static List<String> displaySummaryList(List<ActionConfig> actions) {
        return (actions == null ? List.<ActionConfig>of() : actions).stream()
                .map(WebAdminActionSummaryService::displaySummary)
                .toList();
    }

    public static List<String> auditSummaryList(List<ActionConfig> actions) {
        return (actions == null ? List.<ActionConfig>of() : actions).stream()
                .map(WebAdminActionSummaryService::auditSummary)
                .toList();
    }

    private static String summary(ActionConfig raw, boolean audit) {
        if (raw == null) {
            return "未知动作";
        }
        ActionConfig action = raw.normalized();
        String prefix = action.enabled() ? "" : "已禁用 · ";
        String base = switch (type(action)) {
            case COMMAND -> commandSummary(action, audit);
            case MESSAGE -> "向玩家显示消息：" + valueOrMissing(action.value(), "未配置消息");
            case SOUND -> "播放音效：" + valueOrMissing(action.value(), "未配置音效");
            case SIGNAL -> "发送信号到频道 " + valueOrMissing(action.value(), "未配置频道");
            case STATE_VARIABLE -> stateSummary(action, audit);
            case TIMER_START -> timerSummary(action, true, audit);
            case TIMER_CANCEL -> timerSummary(action, false, audit);
        };
        return prefix + base + commonSuffix(action, audit);
    }

    private static ActionType type(ActionConfig action) {
        return action == null || action.type() == null ? ActionType.COMMAND : action.type();
    }

    private static String commandSummary(ActionConfig action, boolean audit) {
        String value = safe(action.value());
        if (audit) {
            return "执行命令 <command redacted length=" + value.length() + ">";
        }
        return value.isBlank() ? "执行命令（未配置）" : "执行命令 /" + truncate(value);
    }

    private static String stateSummary(ActionConfig action, boolean audit) {
        if (audit) {
            return "状态变量动作 " + action.stateActionSummary() + " " + action.stateAuditFingerprint();
        }
        String key = safe(action.stateKey()).isBlank() ? "未配置 key" : safe(action.stateKey());
        StateVariableMutationOperation operation = StateVariableMutationOperation.parse(action.stateOperation()).orElse(null);
        String target = stateTarget(action);
        if (operation == StateVariableMutationOperation.SET_VARIABLE) {
            return "设置状态变量 " + key + " = " + valueOrMissing(action.stateValue(), "未配置值") + target;
        }
        if (operation == StateVariableMutationOperation.INCREMENT_VARIABLE) {
            return "增加状态变量 " + key + " += " + action.stateDelta() + target;
        }
        if (operation == StateVariableMutationOperation.DECREMENT_VARIABLE) {
            return "减少状态变量 " + key + " -= " + action.stateDelta() + target;
        }
        if (operation == StateVariableMutationOperation.TOGGLE_BOOLEAN) {
            return "切换状态变量 " + key + target;
        }
        if (operation == StateVariableMutationOperation.CLEAR_VARIABLE) {
            return "清除状态变量 " + key + target;
        }
        return "状态变量动作 " + action.stateActionSummary();
    }

    private static String stateTarget(ActionConfig action) {
        String scope = StateVariableScope.parse(action.stateScope())
                .map(StateVariableScope::displayName)
                .orElse(safe(action.stateScope()));
        String targetMode = StateVariableTargetMode.parse(action.stateTargetMode())
                .map(StateVariableTargetMode::displayName)
                .orElse(safe(action.stateTargetMode()));
        if (scope.isBlank() && targetMode.isBlank()) {
            return "";
        }
        return " · " + (scope.isBlank() ? "未配置作用域" : scope)
                + " · " + (targetMode.isBlank() ? "未配置目标" : targetMode);
    }

    private static String timerSummary(ActionConfig action, boolean start, boolean audit) {
        String timerId = safe(action.timerId()).isBlank() ? "未配置 Timer" : safe(action.timerId());
        TimerTargetMode targetMode = TimerTargetMode.parse(action.timerTargetMode());
        String target = targetMode == null
                ? (safe(action.timerTargetMode()).isBlank() ? "默认目标" : safe(action.timerTargetMode()))
                : targetMode.displayName();
        String base = (start ? "启动计时器 " : "取消计时器 ") + timerId + " · " + target;
        if (start) {
            TimerStartPolicy policy = TimerStartPolicy.parse(action.timerStartPolicyOverride());
            String policyLabel = policy == null
                    ? (safe(action.timerStartPolicyOverride()).isBlank() ? "使用 Timer 定义策略" : safe(action.timerStartPolicyOverride()))
                    : policy.displayName();
            base += " · " + policyLabel;
            if (action.timerDurationOverrideTicks() > 0L) {
                base += " · 覆盖时长 " + action.timerDurationOverrideTicks() + " ticks";
            }
        } else if (!safe(action.timerMissingBehavior()).isBlank()) {
            base += " · 缺失策略 " + safe(action.timerMissingBehavior());
        }
        return audit ? base + " " + action.timerAuditFingerprint() : base;
    }

    private static String commonSuffix(ActionConfig action, boolean audit) {
        StringBuilder builder = new StringBuilder();
        if (!safe(action.conditionGroupId()).isBlank()) {
            builder.append(" · 条件组 ").append(safe(action.conditionGroupId()));
        }
        if (action.cooldownTicks() > 0) {
            builder.append(" · 冷却 ").append(action.cooldownTicks()).append(" ticks");
        }
        if (action.type() == ActionType.COMMAND) {
            if (action.requiresOp()) {
                builder.append(" · 需要 OP");
            }
            if (action.notifyOps()) {
                builder.append(" · 通知 OP");
            }
        }
        if (audit) {
            builder.append(" · enabled=").append(action.enabled());
        }
        return builder.toString();
    }

    private static String valueOrMissing(String value, String missing) {
        String safe = safe(value);
        return safe.isBlank() ? missing : truncate(safe);
    }

    private static String truncate(String value) {
        String safe = safe(value);
        return safe.length() <= DISPLAY_LIMIT ? safe : safe.substring(0, DISPLAY_LIMIT - 3) + "...";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
