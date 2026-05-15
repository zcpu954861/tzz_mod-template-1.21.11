package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import java.util.function.Supplier;

public record ConditionGateRequest(
        String conditionGroupId,
        ConditionRuntimeTargetType targetType,
        String targetId,
        Supplier<ConditionEvaluationContext> contextSupplier,
        ConditionGroupCompatibilityProfile compatibilityProfile
) {
    public ConditionGateRequest(
            String conditionGroupId,
            ConditionRuntimeTargetType targetType,
            String targetId,
            Supplier<ConditionEvaluationContext> contextSupplier
    ) {
        this(conditionGroupId, targetType, targetId, contextSupplier, null);
    }

    public ConditionGateRequest {
        conditionGroupId = conditionGroupId == null ? "" : conditionGroupId.trim();
        targetId = targetId == null ? "" : targetId.trim();
    }
}
