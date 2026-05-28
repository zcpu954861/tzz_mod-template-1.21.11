package com.zcpu.tzzmod.webadmin.dto;

import java.util.ArrayList;
import java.util.List;

public final class WebAdminSignalListenerCreateRequest {
    public String name;
    public String displayName;
    public String channel;
    public Object enabled;
    public Object cooldownTicks;
    public String conditionGroupId = "";
    public List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> actions = new ArrayList<>();
    public String note = "";
    public String expectedFingerprint = "";
    public String lockId = "";
}
