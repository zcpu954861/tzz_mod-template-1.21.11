package com.zcpu.tzzmod.webadmin.dto;

public final class WebAdminSelectionCancelRequest {
    public String selectionId = "";
    public String draftSessionId = "";
    public String protectedDraftId = "";
    public Object cleanupProtectedDraft = Boolean.FALSE;
    public Object confirmed = Boolean.FALSE;
    public String reason = "";
}
