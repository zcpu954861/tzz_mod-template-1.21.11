package com.zcpu.tzzmod.webadmin.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WebAdminTemplateRequest {
    public String source = "";
    public String templateId = "";
    public String packageJson = "";
    public String importedTemplateId = "";
    public String importedDisplayName = "";
    public String prefix = "";
    public String displayNamePrefix = "";
    public String rootChannel = "";
    public Map<String, String> placeholderMappings = new LinkedHashMap<>();
    public String expectedFingerprint = "";
    public String lockId = "";
    public boolean confirmed = false;
}
