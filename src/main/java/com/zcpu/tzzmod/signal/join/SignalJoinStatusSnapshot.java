package com.zcpu.tzzmod.signal.join;

import java.util.List;
import java.util.Map;

public record SignalJoinStatusSnapshot(
        String joinId,
        boolean enabled,
        String mode,
        String scopeMode,
        String resetPolicy,
        int pendingScopeCount,
        long lastStatusAt,
        String lastResult,
        String lastFailureReason,
        List<Map<String, Object>> scopes
) {
}
