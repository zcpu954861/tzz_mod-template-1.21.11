package com.zcpu.tzzmod.action;

import com.zcpu.tzzmod.condition.ConditionGroupIds;
import com.zcpu.tzzmod.condition.state.StateVariableMutationOperation;
import com.zcpu.tzzmod.condition.state.StateVariableMutationRequest;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableTargetMode;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.condition.state.StateVariableValidation;
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
        String stateInitialValue
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

    public boolean isUsable() {
        if (!enabled) {
            return false;
        }
        if (type == ActionType.STATE_VARIABLE) {
            return true;
        }
        return value != null && !value.trim().isEmpty();
    }

    public boolean isStateVariableAction() {
        return type == ActionType.STATE_VARIABLE;
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
                stateInitialValue
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

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String redactedValueSummary(String value) {
        String safeValue = safe(value);
        return safeValue.isEmpty() ? "<empty>" : "<redacted length=" + safeValue.length() + ">";
    }
}
