package com.zcpu.tzzmod.webadmin.dto;

public final class WebAdminStateVariableWriteRequest {
    public String scope = "GLOBAL";
    public String targetId = "";
    public String key = "";
    public String type = "STRING";
    public String value = "";
    public String displayName = "";
    public String note = "";
    public String expectedFingerprint = "";
    public String lockId = "";
}
