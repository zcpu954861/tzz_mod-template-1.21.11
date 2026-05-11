package com.zcpu.tzzmod.webadmin.dto;

import java.util.List;

public final class WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest {
    public String deviceId = "";
    public String expectedFingerprint = "";
    public String lockId = "";

    public Boolean redstoneEnabled;
    public String redstoneMode = "";
    public String channel = "";
    public String offChannel = "";

    public Boolean blockStateEnabled;
    public String conditionMode = "";
    public List<BlockStateConditionRow> conditionProperties = List.of();

    public Boolean interactionEnabled;
    public String interactChannel = "";
    public Object interactionCooldownTicks;

    public Boolean containerOpenEnabled;
    public String containerOpenChannel = "";
    public Boolean containerCloseEnabled;
    public String containerCloseChannel = "";
    public Boolean containerChangeEnabled;
    public String containerChangeChannel = "";
    public Object containerCooldownTicks;
    public Object containerChangeCheckIntervalTicks;

    public static final class BlockStateConditionRow {
        public String property = "";
        public String value = "";
    }
}
