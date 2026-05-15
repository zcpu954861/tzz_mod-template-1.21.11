package com.zcpu.tzzmod.condition.runtime;

import java.util.List;
import java.util.Map;

public record ConditionGroupCompatibilityResult(
        boolean compatible,
        ConditionRuntimeTargetType targetType,
        String conditionGroupId,
        List<String> reasons
) {
    public ConditionGroupCompatibilityResult {
        conditionGroupId = conditionGroupId == null ? "" : conditionGroupId.trim();
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public static ConditionGroupCompatibilityResult compatible(ConditionRuntimeTargetType targetType, String conditionGroupId) {
        return new ConditionGroupCompatibilityResult(true, targetType, conditionGroupId, List.of());
    }

    public static ConditionGroupCompatibilityResult incompatible(ConditionRuntimeTargetType targetType, String conditionGroupId, List<String> reasons) {
        return new ConditionGroupCompatibilityResult(false, targetType, conditionGroupId, reasons);
    }

    public String message() {
        if (compatible) {
            return "条件组适用于当前触发方式。";
        }
        return reasons.isEmpty() ? "条件组与当前触发方式不兼容。" : String.join("；", reasons);
    }

    public Map<String, Object> summary() {
        return Map.of(
                "compatible", compatible,
                "targetType", targetType == null ? "" : targetType.id(),
                "conditionGroupId", conditionGroupId,
                "reasons", reasons,
                "message", message()
        );
    }
}
