package com.zcpu.tzzmod.condition.runtime;

import com.zcpu.tzzmod.condition.ConditionEvaluationContext;
import java.util.function.Supplier;

public record ConditionGateRequest(
        String conditionGroupId,
        ConditionRuntimeTargetType targetType,
        String targetId,
        Supplier<ConditionEvaluationContext> contextSupplier,
        ConditionGroupCompatibilityProfile compatibilityProfile,
        String gateLevel,
        ConditionRuntimeTargetType parentTargetType,
        String parentTargetId,
        String parentTargetDisplayName,
        int actionIndex,
        int actionDisplayIndex,
        String actionType,
        String parentActionBucket
) {
    public ConditionGateRequest(
            String conditionGroupId,
            ConditionRuntimeTargetType targetType,
            String targetId,
            Supplier<ConditionEvaluationContext> contextSupplier,
            ConditionGroupCompatibilityProfile compatibilityProfile
    ) {
        this(
                conditionGroupId,
                targetType,
                targetId,
                contextSupplier,
                compatibilityProfile,
                "LIST",
                null,
                "",
                "",
                -1,
                0,
                "",
                ""
        );
    }

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
        gateLevel = gateLevel == null || gateLevel.isBlank() ? "LIST" : gateLevel.trim().toUpperCase(java.util.Locale.ROOT);
        parentTargetId = parentTargetId == null ? "" : parentTargetId.trim();
        parentTargetDisplayName = parentTargetDisplayName == null ? "" : parentTargetDisplayName.trim();
        actionIndex = Math.max(-1, actionIndex);
        actionDisplayIndex = actionDisplayIndex <= 0 && actionIndex >= 0 ? actionIndex + 1 : Math.max(0, actionDisplayIndex);
        actionType = actionType == null ? "" : actionType.trim();
        parentActionBucket = parentActionBucket == null ? "" : parentActionBucket.trim();
    }
}
