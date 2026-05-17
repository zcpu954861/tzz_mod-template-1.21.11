package com.zcpu.tzzmod.action;

import com.zcpu.tzzmod.condition.ConditionGroupIds;
import com.zcpu.tzzmod.condition.state.StateVariableMutationOperation;
import com.zcpu.tzzmod.condition.state.StateVariableMutationRequest;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableTargetMode;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.condition.state.StateVariableValidation;
import com.zcpu.tzzmod.scheduler.TimerStartPolicy;
import com.zcpu.tzzmod.scheduler.TimerStore;
import com.zcpu.tzzmod.scheduler.TimerTargetMode;
import com.zcpu.tzzmod.signal.SignalChannel;

public record ActionConfig(
        ActionType type,
        String value,
        boolean enabled,
        boolean requiresOp,
        int cooldownTicks,
        boolean notifyOps,
        String conditionGroupId,
        String stateOperation,
        String stateScope,
        String stateTargetMode,
        String stateTargetId,
        String stateKey,
        String stateValueType,
        String stateValue,
        long stateDelta,
        boolean stateCreateIfMissing,
        String stateInitialValue,
        String timerId,
        String timerTargetMode,
        String timerTargetId,
        String timerStartPolicyOverride,
        long timerDurationOverrideTicks,
        String timerMissingBehavior
) {
    public ActionConfig(
            ActionType type,
            String value,
            boolean enabled,
            boolean requiresOp,
            int cooldownTicks,
            boolean notifyOps
    ) {
        this(type, value, enabled, requiresOp, cooldownTicks, notifyOps, "");
    }

    public ActionConfig(
            ActionType type,
            String value,
            boolean enabled,
            boolean requiresOp,
            int cooldownTicks,
            boolean notifyOps,
            String conditionGroupId
    ) {
        this(
                type,
                value,
                enabled,
                requiresOp,
                cooldownTicks,
                notifyOps,
                conditionGroupId,
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
                "",
                "",
                "",
                "",
                0L,
                ""
        );
    }

    public ActionConfig(
            ActionType type,
            String value,
            boolean enabled,
            boolean requiresOp,
            int cooldownTicks,
            boolean notifyOps,
            String conditionGroupId,
            String stateOperation,
            String stateScope,
            String stateTargetMode,
            String stateTargetId,
            String stateKey,
            String stateValueType,
            String stateValue,
            long stateDelta,
            boolean stateCreateIfMissing,
            String stateInitialValue
    ) {
        this(
                type,
                value,
                enabled,
                requiresOp,
                cooldownTicks,
                notifyOps,
                conditionGroupId,
                stateOperation,
                stateScope,
                stateTargetMode,
                stateTargetId,
                stateKey,
                stateValueType,
                stateValue,
                stateDelta,
                stateCreateIfMissing,
                stateInitialValue,
                "",
                "",
                "",
                "",
                0L,
                ""
        );
    }

    public ActionConfig {
        type = type == null ? ActionType.COMMAND : type;
        value = value == null ? "" : value.trim();
        if (type == ActionType.COMMAND) {
            value = normalizeCommand(value);
        } else if (type == ActionType.SIGNAL) {
            value = SignalChannel.normalize(value);
        } else if (type == ActionType.STATE_VARIABLE) {
            value = "";
        } else if (type == ActionType.TIMER_START || type == ActionType.TIMER_CANCEL) {
            value = "";
        }
        conditionGroupId = ConditionGroupIds.normalize(conditionGroupId);
        cooldownTicks = Math.max(0, cooldownTicks);
        if (type == ActionType.STATE_VARIABLE) {
            stateOperation = normalizeOperation(stateOperation);
            stateScope = normalizeScope(stateScope);
            stateTargetMode = normalizeTargetMode(stateTargetMode);
            stateTargetId = stateTargetId == null ? "" : stateTargetId.trim();
            stateKey = StateVariableValidation.normalizeKey(stateKey);
            stateValueType = normalizeValueType(stateValueType);
            stateValue = stateValue == null ? "" : stateValue.trim();
            stateInitialValue = stateInitialValue == null ? "" : stateInitialValue.trim();
            requiresOp = false;
            notifyOps = false;
        } else {
            stateOperation = "";
            stateScope = "";
            stateTargetMode = "";
            stateTargetId = "";
            stateKey = "";
            stateValueType = "";
            stateValue = "";
            stateDelta = 0L;
            stateCreateIfMissing = false;
            stateInitialValue = "";
        }
        if (type == ActionType.TIMER_START || type == ActionType.TIMER_CANCEL) {
            timerId = TimerStore.normalizeId(timerId);
            timerTargetMode = normalizeTimerTargetMode(timerTargetMode);
            timerTargetId = timerTargetId == null ? "" : timerTargetId.trim();
            timerStartPolicyOverride = type == ActionType.TIMER_START ? normalizeTimerStartPolicy(timerStartPolicyOverride) : "";
            timerDurationOverrideTicks = type == ActionType.TIMER_START ? Math.max(0L, timerDurationOverrideTicks) : 0L;
            timerMissingBehavior = normalizeTimerMissingBehavior(timerMissingBehavior);
            requiresOp = false;
            notifyOps = false;
        } else {
            timerId = "";
            timerTargetMode = "";
            timerTargetId = "";
            timerStartPolicyOverride = "";
            timerDurationOverrideTicks = 0L;
            timerMissingBehavior = "";
        }
    }

    public static ActionConfig command(String command, boolean notifyOps) {
        return new ActionConfig(
                ActionType.COMMAND,
                normalizeCommand(command),
                true,
                false,
                0,
                notifyOps,
                ""
        );
    }

    public static ActionConfig signal(String channel, boolean notifyOps) {
        return new ActionConfig(
                ActionType.SIGNAL,
                SignalChannel.normalize(channel),
                true,
                false,
                0,
                notifyOps,
                ""
        );
    }

    public static ActionConfig stateVariable(
            StateVariableMutationOperation operation,
            StateVariableScope scope,
            StateVariableTargetMode targetMode,
            String targetId,
            String key,
            StateVariableType valueType,
            String value,
            long delta,
            boolean createIfMissing,
            String initialValue,
            String conditionGroupId
    ) {
        return new ActionConfig(
                ActionType.STATE_VARIABLE,
                "",
                true,
                false,
                0,
                false,
                conditionGroupId,
                operation == null ? "" : operation.id(),
                scope == null ? "" : scope.name(),
                targetMode == null ? "" : targetMode.id(),
                targetId,
                key,
                valueType == null ? "" : valueType.name(),
                value,
                delta,
                createIfMissing,
                initialValue
        );
    }

    public static ActionConfig timerStart(
            String timerId,
            TimerTargetMode targetMode,
            String targetId,
            TimerStartPolicy startPolicyOverride,
            String conditionGroupId
    ) {
        return new ActionConfig(
                ActionType.TIMER_START,
                "",
                true,
                false,
                0,
                false,
                conditionGroupId,
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
                targetMode == null ? "" : targetMode.id(),
                targetId,
                startPolicyOverride == null ? "" : startPolicyOverride.id(),
                0L,
                "noop_success"
        );
    }

    public static ActionConfig timerCancel(
            String timerId,
            TimerTargetMode targetMode,
            String targetId,
            String conditionGroupId
    ) {
        return new ActionConfig(
                ActionType.TIMER_CANCEL,
                "",
                true,
                false,
                0,
                false,
                conditionGroupId,
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
                targetMode == null ? "" : targetMode.id(),
                targetId,
                "",
                0L,
                "noop_success"
        );
    }

    public boolean isUsable() {
        if (!enabled) {
            return false;
        }
        if (type == ActionType.STATE_VARIABLE) {
            return true;
        }
        if (type == ActionType.TIMER_START || type == ActionType.TIMER_CANCEL) {
            return timerId != null && !timerId.trim().isEmpty();
        }
        return value != null && !value.trim().isEmpty();
    }

    public boolean isStateVariableAction() {
        return type == ActionType.STATE_VARIABLE;
    }

    public boolean isTimerAction() {
        return type == ActionType.TIMER_START || type == ActionType.TIMER_CANCEL;
    }

    public StateVariableMutationRequest stateMutationRequest(String contextPlayerId) {
        return new StateVariableMutationRequest(
                StateVariableMutationOperation.parse(stateOperation).orElse(null),
                StateVariableScope.parse(stateScope).orElse(null),
                StateVariableTargetMode.parse(stateTargetMode).orElse(null),
                stateTargetId,
                contextPlayerId,
                stateKey,
                StateVariableType.parse(stateValueType).orElse(null),
                stateValue,
                stateDelta,
                stateCreateIfMissing,
                stateInitialValue
        );
    }

    public String stateActionSummary() {
        if (type != ActionType.STATE_VARIABLE) {
            return "";
        }
        String operation = StateVariableMutationOperation.parse(stateOperation)
                .map(StateVariableMutationOperation::displayName)
                .orElse(stateOperation == null || stateOperation.isBlank() ? "未选择操作" : stateOperation);
        String scope = StateVariableScope.parse(stateScope)
                .map(StateVariableScope::displayName)
                .orElse(stateScope == null || stateScope.isBlank() ? "未选择作用域" : stateScope);
        String targetMode = StateVariableTargetMode.parse(stateTargetMode)
                .map(StateVariableTargetMode::displayName)
                .orElse(stateTargetMode == null || stateTargetMode.isBlank() ? "未选择目标" : stateTargetMode);
        String key = stateKey == null || stateKey.isBlank() ? "未填写 key" : stateKey;
        return operation + " · " + scope + " · " + targetMode + " · " + key;
    }

    public String stateFingerprint() {
        if (type != ActionType.STATE_VARIABLE) {
            return "";
        }
        return String.join("|",
                "stateOperation=" + safe(stateOperation),
                "stateScope=" + safe(stateScope),
                "stateTargetMode=" + safe(stateTargetMode),
                "stateTargetId=" + safe(stateTargetId),
                "stateKey=" + safe(stateKey),
                "stateValueType=" + safe(stateValueType),
                "stateValue=" + safe(stateValue),
                "stateDelta=" + stateDelta,
                "stateCreateIfMissing=" + stateCreateIfMissing,
                "stateInitialValue=" + safe(stateInitialValue)
        );
    }

    public String stateAuditFingerprint() {
        if (type != ActionType.STATE_VARIABLE) {
            return "";
        }
        return String.join("|",
                "stateOperation=" + safe(stateOperation),
                "stateScope=" + safe(stateScope),
                "stateTargetMode=" + safe(stateTargetMode),
                "stateTargetId=" + safe(stateTargetId),
                "stateKey=" + safe(stateKey),
                "stateValueType=" + safe(stateValueType),
                "stateValue=" + redactedValueSummary(stateValue),
                "stateDelta=" + stateDelta,
                "stateCreateIfMissing=" + stateCreateIfMissing,
                "stateInitialValue=" + redactedValueSummary(stateInitialValue)
        );
    }

    public String timerActionSummary() {
        if (!isTimerAction()) {
            return "";
        }
        TimerTargetMode parsedTarget = TimerTargetMode.parse(timerTargetMode);
        TimerStartPolicy parsedPolicy = TimerStartPolicy.parse(timerStartPolicyOverride);
        String target = parsedTarget == null
                ? (timerTargetMode == null || timerTargetMode.isBlank() ? "默认作用域目标" : timerTargetMode)
                : parsedTarget.displayName();
        String policy = parsedPolicy == null
                ? (timerStartPolicyOverride == null || timerStartPolicyOverride.isBlank() ? "使用 Timer 定义策略" : timerStartPolicyOverride)
                : parsedPolicy.displayName();
        return (type == ActionType.TIMER_START ? "启动 Timer" : "取消 Timer")
                + " · " + safe(timerId)
                + " · " + target
                + (type == ActionType.TIMER_START ? " · " + policy : "");
    }

    public String timerFingerprint() {
        if (!isTimerAction()) {
            return "";
        }
        return String.join("|",
                "timerId=" + safe(timerId),
                "timerTargetMode=" + safe(timerTargetMode),
                "timerTargetId=" + safe(timerTargetId),
                "timerStartPolicyOverride=" + safe(timerStartPolicyOverride),
                "timerDurationOverrideTicks=" + timerDurationOverrideTicks,
                "timerMissingBehavior=" + safe(timerMissingBehavior)
        );
    }

    public String timerAuditFingerprint() {
        return timerFingerprint();
    }

    public ActionConfig normalized() {
        return new ActionConfig(
                type,
                value,
                enabled,
                requiresOp,
                cooldownTicks,
                notifyOps,
                conditionGroupId,
                stateOperation,
                stateScope,
                stateTargetMode,
                stateTargetId,
                stateKey,
                stateValueType,
                stateValue,
                stateDelta,
                stateCreateIfMissing,
                stateInitialValue,
                timerId,
                timerTargetMode,
                timerTargetId,
                timerStartPolicyOverride,
                timerDurationOverrideTicks,
                timerMissingBehavior
        );
    }

    public static String normalizeCommand(String command) {
        String trimmed = command == null ? "" : command.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1).trim();
        }
        return trimmed;
    }

    private static String normalizeOperation(String raw) {
        String value = safe(raw);
        return StateVariableMutationOperation.parse(value)
                .map(StateVariableMutationOperation::id)
                .orElse(value);
    }

    private static String normalizeScope(String raw) {
        String value = safe(raw);
        return StateVariableScope.parse(value)
                .map(StateVariableScope::name)
                .orElse(value);
    }

    private static String normalizeTargetMode(String raw) {
        String value = safe(raw);
        return StateVariableTargetMode.parse(value)
                .map(StateVariableTargetMode::id)
                .orElse(value);
    }

    private static String normalizeValueType(String raw) {
        String value = safe(raw);
        return StateVariableType.parse(value)
                .map(StateVariableType::name)
                .orElse(value);
    }

    private static String normalizeTimerTargetMode(String raw) {
        String value = safe(raw);
        TimerTargetMode targetMode = TimerTargetMode.parse(value);
        return targetMode == null ? value : targetMode.id();
    }

    private static String normalizeTimerStartPolicy(String raw) {
        String value = safe(raw);
        TimerStartPolicy startPolicy = TimerStartPolicy.parse(value);
        return startPolicy == null ? value : startPolicy.id();
    }

    private static String normalizeTimerMissingBehavior(String raw) {
        String value = safe(raw).toLowerCase(java.util.Locale.ROOT);
        return value.isBlank() ? "noop_success" : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String redactedValueSummary(String value) {
        String safeValue = safe(value);
        return safeValue.isEmpty() ? "<empty>" : "<redacted length=" + safeValue.length() + ">";
    }
}
