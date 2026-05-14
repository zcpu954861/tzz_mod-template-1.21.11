package com.zcpu.tzzmod.webadmin.dto;

public final class WebAdminRegionControllerRequests {
    private WebAdminRegionControllerRequests() {
    }

    public static final class CreateRequest {
        public String name = "";
        public String regionId = "";
        public String expectedFingerprint = "";
        public String lockId = "";
    }

    public static final class UpdateRequest {
        public String controllerId = "";
        public Object enabled = Boolean.TRUE;
        public String name = "";
        public String regionId = "";
        public String targetFilterType = "ALL";
        public String targetFilterValue = "";
        public Object stayIntervalTicks = 100;
        public String expectedFingerprint = "";
        public String lockId = "";
    }

    public static final class ActionAddRequest {
        public String controllerId = "";
        public String triggerType = "";
        public WebAdminActionRelayActionsUpdateRequest.ActionEntry action = new WebAdminActionRelayActionsUpdateRequest.ActionEntry();
        public String expectedFingerprint = "";
        public String lockId = "";
    }

    public static final class ActionClearRequest {
        public String controllerId = "";
        public String triggerType = "";
        public Boolean confirmed = Boolean.FALSE;
        public String confirmationText = "";
        public String expectedFingerprint = "";
        public String lockId = "";
    }

    public static final class ActionDeleteRequest {
        public String controllerId = "";
        public String triggerType = "";
        public Object actionIndex = 0;
        public Boolean confirmed = Boolean.FALSE;
        public String expectedFingerprint = "";
        public String lockId = "";
    }

    public static final class DeleteRequest {
        public String controllerId = "";
        public Boolean confirmed = Boolean.FALSE;
        public String confirmationText = "";
        public String expectedFingerprint = "";
        public String lockId = "";
    }
}
