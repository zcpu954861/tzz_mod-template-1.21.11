package com.zcpu.tzzmod.webadmin.dto;

public final class WebAdminSignalListenerActionRequests {
    private WebAdminSignalListenerActionRequests() {
    }

    public static final class ActionAddRequest {
        public String listenerId = "";
        public WebAdminActionRelayActionsUpdateRequest.ActionEntry action = new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        public String expectedFingerprint = "";
        public String lockId = "";
    }

    public static final class ActionClearRequest {
        public String listenerId = "";
        public Boolean confirmed = Boolean.FALSE;
        public String expectedFingerprint = "";
        public String lockId = "";
    }

    public static final class ActionDeleteRequest {
        public String listenerId = "";
        public Object actionIndex = 0;
        public Boolean confirmed = Boolean.FALSE;
        public String expectedFingerprint = "";
        public String lockId = "";
    }

    public static final class ActionUpdateRequest {
        public String listenerId = "";
        public Object actionIndex = 0;
        public WebAdminActionRelayActionsUpdateRequest.ActionEntry action = new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        public String expectedFingerprint = "";
        public String lockId = "";
    }
}
