package com.zcpu.tzzmod.webadmin.dto;

import java.util.ArrayList;
import java.util.List;

public final class WebAdminActionRelayActionsUpdateRequest {
    public String deviceId = "";
    public String conditionGroupId = "";
    public List<ActionEntry> actions = new ArrayList<>();
    public String expectedFingerprint = "";
    public String lockId = "";

    public static final class ActionEntry {
        public String type = "";
        public String value = "";
        public Object enabled = Boolean.TRUE;
        public Object requiresOp = Boolean.FALSE;
        public Object cooldownTicks = 0;
        public Object notifyOps = Boolean.FALSE;
        public String conditionGroupId = "";
    }
}
