package com.zcpu.tzzmod.action;

import com.zcpu.tzzmod.condition.state.StateVariableMutationRequest;
import com.zcpu.tzzmod.condition.state.StateVariableMutationResult;
import com.zcpu.tzzmod.condition.state.StateVariableMutationValidation;
import com.zcpu.tzzmod.scheduler.TimerOperationResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.text.Text;

public record ActionExecutionResult(
        boolean success,
        Text message,
        String code,
        Map<String, Object> details,
        long durationNanos
) {
    public ActionExecutionResult {
        code = code == null ? "" : code;
        details = details == null ? Map.of() : Map.copyOf(details);
        durationNanos = Math.max(0L, durationNanos);
    }

    public static ActionExecutionResult success(Text message) {
        return new ActionExecutionResult(true, message, "success", Map.of(), 0L);
    }

    public static ActionExecutionResult failure(Text message) {
        return new ActionExecutionResult(false, message, "failure", Map.of(), 0L);
    }

    public static ActionExecutionResult stateValidationFailure(
            ActionConfig config,
            String contextPlayerId,
            Text validationError,
            long durationNanos
    ) {
        StateVariableMutationRequest request = config == null ? null : config.stateMutationRequest(contextPlayerId);
        List<String> validationErrors = StateVariableMutationValidation.validate(request).stream()
                .map(StateVariableMutationValidation.Issue::message)
                .toList();
        String message = validationError == null ? "状态变量动作校验失败。" : validationError.getString();
        String targetId = request == null ? "" : StateVariableMutationValidation.resolvedTargetId(request);
        String key = request == null ? "" : request.key();
        return stateMutation(StateVariableMutationResult.failed(
                "validation_error",
                message,
                request,
                targetId,
                key,
                null,
                validationErrors.isEmpty() ? List.of(message) : validationErrors,
                durationNanos
        ));
    }

    public static ActionExecutionResult stateMutation(StateVariableMutationResult result) {
        if (result == null) {
            return new ActionExecutionResult(
                    false,
                    Text.literal("状态变量动作执行失败：结果为空。"),
                    "state_variable_result_missing",
                    Map.of("actionType", "state_variable"),
                    0L
            );
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("actionType", "state_variable");
        details.put("operation", result.operation() == null ? "" : result.operation().id());
        details.put("scope", result.scope() == null ? "" : result.scope().name());
        details.put("target", result.targetId());
        details.put("key", result.key());
        details.put("oldValue", trimValue(result.oldValue()));
        details.put("newValue", trimValue(result.newValue()));
        details.put("oldType", result.oldRecord() == null ? "" : result.oldRecord().type().name());
        details.put("newType", result.newRecord() == null ? "" : result.newRecord().type().name());
        details.put("changed", result.changed());
        details.put("failureReason", result.success() ? "" : result.message());
        details.put("validationErrors", result.validationErrors());
        return new ActionExecutionResult(
                result.success(),
                Text.literal(result.message()),
                result.code(),
                details,
                result.durationNanos()
        );
    }

    public static ActionExecutionResult timerOperation(TimerOperationResult result) {
        if (result == null) {
            return new ActionExecutionResult(
                    false,
                    Text.literal("Timer 动作执行失败：结果为空。"),
                    "timer_result_missing",
                    Map.of("actionType", "timer"),
                    0L
            );
        }
        return new ActionExecutionResult(
                result.success(),
                Text.literal(result.message()),
                result.code(),
                result.actionDetails(),
                0L
        );
    }

    public ActionExecutionResult withMessage(Text newMessage) {
        return new ActionExecutionResult(success, newMessage, code, details, durationNanos);
    }

    private static String trimValue(String value) {
        String safeValue = value == null ? "" : value;
        return safeValue.length() > 96 ? safeValue.substring(0, 96) + "..." : safeValue;
    }
}
