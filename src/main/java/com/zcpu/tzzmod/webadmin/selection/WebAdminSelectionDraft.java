package com.zcpu.tzzmod.webadmin.selection;

public record WebAdminSelectionDraft(
        String channel,
        String displayName,
        String note,
        String iconKey,
        boolean enabled
) {
    public WebAdminSelectionDraft {
        channel = safe(channel);
        displayName = safe(displayName);
        note = safe(note);
        iconKey = safe(iconKey).isBlank() ? "auto" : safe(iconKey);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
