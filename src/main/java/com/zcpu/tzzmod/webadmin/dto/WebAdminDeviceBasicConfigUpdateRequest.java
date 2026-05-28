package com.zcpu.tzzmod.webadmin.dto;

public final class WebAdminDeviceBasicConfigUpdateRequest {
    public String deviceId = "";
    public Object enabled;
    public String channel = "";
    public String expectedFingerprint = "";
    public String lockId = "";
    public Boolean strictPhysicalPresence = Boolean.FALSE;
}
