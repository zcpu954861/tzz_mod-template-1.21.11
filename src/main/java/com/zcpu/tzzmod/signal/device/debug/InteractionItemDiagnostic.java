package com.zcpu.tzzmod.signal.device.debug;

import java.util.List;

public record InteractionItemDiagnostic(
        boolean matcherEnabled,
        String source,
        boolean sourceSupportsConsume,
        boolean consumeEnabled,
        int consumeCount,
        String vanillaPolicy,
        List<DiagnosticIssue> issues
) {
    public InteractionItemDiagnostic {
        source = source == null ? "" : source.trim();
        vanillaPolicy = vanillaPolicy == null ? "" : vanillaPolicy.trim();
        consumeCount = Math.max(0, consumeCount);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
