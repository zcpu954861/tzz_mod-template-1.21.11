package com.zcpu.tzzmod.webadmin.dto;

import java.util.List;

public final class WebAdminInteractionItemMatcherUpdateRequest {
    public String deviceId = "";
    public Boolean enabled;
    public String templateItemId;
    public String countMode;
    public Object requiredCount;
    public Boolean matchDamage;
    public Object templateDamage;
    public Boolean matchCustomName;
    public String templateCustomName;
    public Boolean matchLore;
    public List<String> templateLore;
    public String interactionItemSource;
    public String interactionItemVanillaPolicy;
    public String expectedFingerprint = "";
    public String lockId = "";
}
