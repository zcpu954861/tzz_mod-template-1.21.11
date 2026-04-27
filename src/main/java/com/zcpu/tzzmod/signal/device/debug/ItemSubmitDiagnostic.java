package com.zcpu.tzzmod.signal.device.debug;

import java.util.List;

public record ItemSubmitDiagnostic(
        boolean enabled,
        int requirementCount,
        int enabledRequirementCount,
        boolean consumeEnabled,
        String consumeOrder,
        List<DiagnosticIssue> issues
) {
    public ItemSubmitDiagnostic {
        requirementCount = Math.max(0, requirementCount);
        enabledRequirementCount = Math.max(0, enabledRequirementCount);
        consumeOrder = consumeOrder == null ? "" : consumeOrder.trim();
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
