package com.zcpu.tzzmod.webadmin.dto;

public final class WebAdminDeviceExtendedConfigUpdateRequest {
    public String deviceId = "";
    public String interactChannel;
    public Boolean clearInteractChannel;
    public String successChannel;
    public Boolean clearSuccessChannel;
    public String failChannel;
    public Boolean clearFailChannel;
    public Object interactionCooldownTicks;
    public Object pulseTicks;
    public Object cooldownTicks;
    public String expectedFingerprint = "";
    public String lockId = "";
}
