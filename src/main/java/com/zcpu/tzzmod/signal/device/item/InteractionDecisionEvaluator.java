package com.zcpu.tzzmod.signal.device.item;

public final class InteractionDecisionEvaluator {
    private InteractionDecisionEvaluator() {
    }

    public static InteractionDecision evaluate(
            String vanillaInteractionPolicy,
            boolean finalSuccess,
            boolean inCooldown,
            boolean consumeRequired,
            boolean consumePlanOk,
            String failureReason
    ) {
        boolean effectiveSuccess = finalSuccess && (!consumeRequired || consumePlanOk);
        boolean requireItemMatch = InteractionItemVanillaPolicy.blocksVanillaOnFailure(vanillaInteractionPolicy);
        boolean allowVanilla = !requireItemMatch || effectiveSuccess;
        boolean sideEffects = !inCooldown;
        String cleanFailure = failureReason == null ? "" : failureReason;
        if (!effectiveSuccess && cleanFailure.isBlank() && consumeRequired && !consumePlanOk) {
            cleanFailure = "consume_plan_failed";
        }
        return new InteractionDecision(
                allowVanilla,
                effectiveSuccess && consumeRequired && consumePlanOk,
                sideEffects,
                sideEffects,
                sideEffects,
                sideEffects,
                sideEffects,
                cleanFailure
        );
    }
}
