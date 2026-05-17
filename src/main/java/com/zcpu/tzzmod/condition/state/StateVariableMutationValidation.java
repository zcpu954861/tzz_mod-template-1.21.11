package com.zcpu.tzzmod.condition.state;

import java.util.ArrayList;
import java.util.List;

public final class StateVariableMutationValidation {
    private StateVariableMutationValidation() {
    }

    public record Issue(String field, String code, String message, String rejectedValue) {
    }

    public static List<Issue> validate(StateVariableMutationRequest request) {
        List<Issue> issues = new ArrayList<>();
        if (request == null) {
            issues.add(new Issue("stateAction", "missing_request", "状态变量动作配置不能为空。", ""));
            return List.copyOf(issues);
        }
        validateBase(request, issues);
        if (request.operation() == null) {
            issues.add(new Issue("stateOperation", "missing_operation", "状态变量动作必须选择操作。", ""));
            return List.copyOf(issues);
        }
        switch (request.operation()) {
            case SET_VARIABLE -> validateSet(request, issues);
            case INCREMENT_VARIABLE, DECREMENT_VARIABLE -> validateIntegerDelta(request, issues);
            case TOGGLE_BOOLEAN -> validateToggle(request, issues);
            case CLEAR_VARIABLE -> {
                // UI drafts may retain hidden value fields when users switch operations.
                // Clear ignores those fields instead of failing validation.
            }
        }
        return List.copyOf(issues);
    }

    public static String resolvedTargetId(StateVariableMutationRequest request) {
        if (request == null || request.scope() == null) {
            return "";
        }
        if (request.scope() == StateVariableScope.GLOBAL) {
            return StateVariableValidation.GLOBAL_TARGET;
        }
        if (request.targetMode() == StateVariableTargetMode.CONTEXT_PLAYER) {
            return request.contextPlayerId();
        }
        if (request.targetMode() == StateVariableTargetMode.EXPLICIT_TARGET) {
            return request.targetId();
        }
        return "";
    }

    private static void validateBase(StateVariableMutationRequest request, List<Issue> issues) {
        if (request.scope() == null) {
            issues.add(new Issue("stateScope", "invalid_scope", "状态变量动作只支持 GLOBAL 或 PLAYER 作用域。", ""));
        }
        if (request.targetMode() == null) {
            issues.add(new Issue("stateTargetMode", "invalid_target_mode", "状态变量动作必须选择目标模式。", ""));
        }
        if (request.scope() == StateVariableScope.GLOBAL && request.targetMode() != null
                && request.targetMode() != StateVariableTargetMode.GLOBAL) {
            issues.add(new Issue("stateTargetMode", "invalid_global_target_mode", "GLOBAL 状态变量只能使用 global 目标。", request.targetMode().id()));
        }
        if (request.scope() == StateVariableScope.PLAYER && request.targetMode() == StateVariableTargetMode.GLOBAL) {
            issues.add(new Issue("stateTargetMode", "invalid_player_target_mode", "PLAYER 状态变量必须使用触发玩家或显式目标。", request.targetMode().id()));
        }
        if (request.scope() == StateVariableScope.PLAYER && request.targetMode() == StateVariableTargetMode.EXPLICIT_TARGET
                && request.targetId().isBlank()) {
            issues.add(new Issue("stateTargetId", "missing_target_id", "PLAYER 显式目标必须填写目标玩家 ID。", ""));
        }
        for (StateVariableValidation.Issue issue : StateVariableValidation.validateKeyOnly(
                request.scope() == null ? StateVariableScope.GLOBAL : request.scope(),
                request.scope() == StateVariableScope.PLAYER ? targetForKeyValidation(request) : StateVariableValidation.GLOBAL_TARGET,
                request.key()
        )) {
            if (!"missing_player_target".equals(issue.code())) {
                issues.add(new Issue("stateKey", issue.code(), issue.message(), request.key()));
            }
        }
    }

    private static String targetForKeyValidation(StateVariableMutationRequest request) {
        if (request.targetMode() == StateVariableTargetMode.EXPLICIT_TARGET) {
            return request.targetId();
        }
        return "context_player";
    }

    private static void validateSet(StateVariableMutationRequest request, List<Issue> issues) {
        if (request.valueType() == null) {
            issues.add(new Issue("stateValueType", "missing_value_type", "设置变量必须选择值类型。", ""));
            return;
        }
        List<StateVariableValidation.Issue> valueIssues = new ArrayList<>();
        StateVariableValidation.validateValue(request.valueType(), request.value(), valueIssues);
        for (StateVariableValidation.Issue issue : valueIssues) {
            issues.add(new Issue("stateValue", issue.code(), issue.message(), request.value()));
        }
    }

    private static void validateIntegerDelta(StateVariableMutationRequest request, List<Issue> issues) {
        if (request.delta() <= 0L) {
            issues.add(new Issue("stateDelta", "invalid_delta", "增加 / 减少变量的 delta 必须大于 0。", Long.toString(request.delta())));
        }
        if (request.valueType() == null) {
            issues.add(new Issue("stateValueType", "missing_value_type", "增加 / 减少变量必须选择 INTEGER 类型。", ""));
        } else if (request.valueType() != StateVariableType.INTEGER) {
            issues.add(new Issue("stateValueType", "operation_type_mismatch", "增加 / 减少变量只支持 INTEGER 类型。", request.valueType().name()));
        }
        if (request.createIfMissing() && !request.initialValue().isBlank()) {
            List<StateVariableValidation.Issue> valueIssues = new ArrayList<>();
            StateVariableValidation.validateValue(StateVariableType.INTEGER, request.initialValue(), valueIssues);
            for (StateVariableValidation.Issue issue : valueIssues) {
                issues.add(new Issue("stateInitialValue", issue.code(), "初始值必须是整数。", request.initialValue()));
            }
        }
    }

    private static void validateToggle(StateVariableMutationRequest request, List<Issue> issues) {
        if (request.valueType() == null) {
            issues.add(new Issue("stateValueType", "missing_value_type", "切换布尔变量必须选择 BOOLEAN 类型。", ""));
        } else if (request.valueType() != StateVariableType.BOOLEAN) {
            issues.add(new Issue("stateValueType", "operation_type_mismatch", "切换布尔变量只支持 BOOLEAN 类型。", request.valueType().name()));
        }
        if (request.createIfMissing() && !request.initialValue().isBlank()) {
            List<StateVariableValidation.Issue> valueIssues = new ArrayList<>();
            StateVariableValidation.validateValue(StateVariableType.BOOLEAN, request.initialValue(), valueIssues);
            for (StateVariableValidation.Issue issue : valueIssues) {
                issues.add(new Issue("stateInitialValue", issue.code(), "初始值必须是 true 或 false。", request.initialValue()));
            }
        }
    }
}
