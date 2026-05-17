package com.zcpu.tzzmod.webadmin.dto;

import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import java.util.ArrayList;
import java.util.List;

public final class WebAdminSignalJoinRequest {
    public String id = "";
    public String displayName = "";
    public String note = "";
    public boolean enabled = true;
    public List<SignalJoinInputDefinition> inputChannels = new ArrayList<>();
    public String outputChannel = "";
    public String mode = "ALL";
    public int threshold = 2;
    public String scopeMode = "GLOBAL";
    public String resetPolicy = "RESET_AFTER_EMIT";
    public long timeoutTicks = 0L;
    public long cooldownTicks = 0L;
    public String expectedFingerprint = "";
    public String lockId = "";
    public boolean confirmed = false;
    public String reason = "";
    public String scopeKey = "";
}
