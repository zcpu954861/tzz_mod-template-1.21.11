package com.zcpu.tzzmod.webadmin.dto;

import java.util.ArrayList;
import java.util.List;

public final class WebAdminTimerRequest {
    public String id = "";
    public String displayName = "";
    public String note = "";
    public boolean enabled = true;
    public String mode = "DELAY";
    public String scopeMode = "GLOBAL";
    public long durationTicks = 20L;
    public long intervalTicks = 20L;
    public int maxRuns = 1;
    public String startPolicy = "RESTART";
    public List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> onStartActions = new ArrayList<>();
    public List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> onTickActions = new ArrayList<>();
    public List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> onCompleteActions = new ArrayList<>();
    public List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> onCancelActions = new ArrayList<>();
    public String outputChannel = "";
    public String expectedFingerprint = "";
    public String lockId = "";
    public boolean confirmed = false;
    public String reason = "";
    public String scopeKey = "";
    public String targetMode = "";
    public String targetId = "";
    public String startPolicyOverride = "";
}
