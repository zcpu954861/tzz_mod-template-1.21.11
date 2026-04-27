package com.zcpu.tzzmod.signal.device.item;

import java.util.List;

public record ItemSubmitEvaluationResult(
        boolean matched,
        boolean consumePlanOk,
        boolean finalSuccess,
        String failureReason,
        List<RequirementResult> requirementResults,
        ConsumePlan stagedConsumePlan,
        String consumedSummary
) {
    public ItemSubmitEvaluationResult {
        failureReason = failureReason == null ? "" : failureReason;
        requirementResults = requirementResults == null ? List.of() : List.copyOf(requirementResults);
        stagedConsumePlan = stagedConsumePlan == null ? new ConsumePlan() : stagedConsumePlan;
        consumedSummary = consumedSummary == null ? "" : consumedSummary;
    }

    public record RequirementResult(
            String name,
            boolean enabled,
            boolean matched,
            int matchedCount,
            String failureReason
    ) {
        public RequirementResult {
            name = name == null ? "" : name;
            matchedCount = Math.max(0, matchedCount);
            failureReason = failureReason == null ? "" : failureReason;
        }
    }
}
