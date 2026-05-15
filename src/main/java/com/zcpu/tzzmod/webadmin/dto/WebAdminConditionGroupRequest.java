package com.zcpu.tzzmod.webadmin.dto;

import com.zcpu.tzzmod.condition.ConditionGroupDefinition;
import java.util.ArrayList;
import java.util.List;

public final class WebAdminConditionGroupRequest {
    public String id = "";
    public String displayName = "";
    public String note = "";
    public String iconKey = "doctor-overview";
    public boolean enabled = true;
    public List<String> tags = new ArrayList<>();
    public ConditionGroupDefinition groupDefinition;
    public String expectedFingerprint = "";
    public String lockId = "";
}
