package com.zcpu.tzzmod.webadmin.selection;

public record WebAdminSelectionDraft(
        String channel,
        String displayName,
        String note,
        String iconKey,
        boolean enabled,
        String draftSessionId,
        String editLockId,
        String logicChainRootType,
        String logicChainRootRef,
        String logicChainDraftNodeId
) {
    public WebAdminSelectionDraft {
        channel = safe(channel);
        displayName = safe(displayName);
        note = safe(note);
        iconKey = safe(iconKey).isBlank() ? "auto" : safe(iconKey);
        draftSessionId = safe(draftSessionId);
        editLockId = safe(editLockId);
        logicChainRootType = safe(logicChainRootType);
        logicChainRootRef = safe(logicChainRootRef);
        logicChainDraftNodeId = safe(logicChainDraftNodeId);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
