package com.zcpu.tzzmod.webadmin.dto;

import java.util.ArrayList;
import java.util.List;

public final class WebAdminLogicChainMetadataRequest {
    public String chainId = "";
    public String displayName = "";
    public String note = "";
    public String iconKey = "auto";
    public List<String> tags = new ArrayList<>();
    public String group = "";
    public String rootType = "channel";
    public String rootRef = "";
    public boolean includeDisabled = true;
    public int maxDepth = 3;
    public String layoutPreference = "auto";
    public String expectedFingerprint = "";
    public String lockId = "";
}
