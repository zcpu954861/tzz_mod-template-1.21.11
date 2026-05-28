package com.zcpu.tzzmod.webadmin.selection;

import com.zcpu.tzzmod.webadmin.WebAdminRole;
import java.util.UUID;

public final class WebAdminSelectionSession {
    public final String selectionId;
    public final String nonce;
    public final String actorUsername;
    public final WebAdminRole actorRole;
    public final String sessionHashSummary;
    public final String remoteAddress;
    public final UUID targetPlayerUuid;
    public final String targetPlayerName;
    public final WebAdminSelectionPurpose purpose;
    public final WebAdminSelectionDraft draft;
    public final long createdAtMillis;
    public boolean completing;
    public int worldDeviceSelectedSlot;

    public WebAdminSelectionSession(
            String selectionId,
            String nonce,
            String actorUsername,
            WebAdminRole actorRole,
            String sessionHashSummary,
            String remoteAddress,
            UUID targetPlayerUuid,
            String targetPlayerName,
            WebAdminSelectionPurpose purpose,
            WebAdminSelectionDraft draft
    ) {
        this.selectionId = safe(selectionId);
        this.nonce = safe(nonce);
        this.actorUsername = safe(actorUsername);
        this.actorRole = actorRole == null ? WebAdminRole.VIEWER : actorRole;
        this.sessionHashSummary = safe(sessionHashSummary);
        this.remoteAddress = safe(remoteAddress);
        this.targetPlayerUuid = targetPlayerUuid;
        this.targetPlayerName = safe(targetPlayerName);
        this.purpose = purpose == null ? WebAdminSelectionPurpose.CREATE_VIRTUAL_BLOCK_DEVICE : purpose;
        this.draft = draft == null ? new WebAdminSelectionDraft("", "", "", "auto", true, "", "", "", "", "") : draft;
        this.createdAtMillis = System.currentTimeMillis();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
