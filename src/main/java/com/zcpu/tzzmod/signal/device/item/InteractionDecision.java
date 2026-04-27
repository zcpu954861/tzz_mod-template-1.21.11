package com.zcpu.tzzmod.signal.device.item;

public record InteractionDecision(
        boolean allowVanillaInteraction,
        boolean executeConsume,
        boolean executeSignal,
        boolean executeMessage,
        boolean executeSound,
        boolean executeExtraSwing,
        boolean recordHistory,
        String failureReason
) {
    public InteractionDecision {
        failureReason = failureReason == null ? "" : failureReason;
    }
}
