package com.zcpu.tzzmod.webadmin.write;

public enum WebAdminOperationType {
    READ("READ", "只读查看"),
    TEST("TEST", "测试 / 校验"),
    ACQUIRE_EDIT_LOCK("ACQUIRE_EDIT_LOCK", "获取编辑锁"),
    RELEASE_EDIT_LOCK("RELEASE_EDIT_LOCK", "释放编辑锁"),
    EDIT_DEVICE_METADATA("EDIT_DEVICE_METADATA", "编辑 WebAdmin 设备显示信息"),
    EDIT_DEVICE_BASIC_CONFIG("EDIT_DEVICE_BASIC_CONFIG", "编辑 WebAdmin 设备基础配置"),
    EDIT_DEVICE_EXTENDED_CONFIG("EDIT_DEVICE_EXTENDED_CONFIG", "编辑 WebAdmin 设备扩展配置"),
    EDIT_DEVICE("EDIT_DEVICE", "编辑设备配置"),
    EDIT_SIGNAL("EDIT_SIGNAL", "编辑 Signal 配置"),
    EDIT_REGION("EDIT_REGION", "编辑区域配置"),
    EDIT_ACTION("EDIT_ACTION", "编辑动作配置"),
    EDIT_ITEM_MATCHER("EDIT_ITEM_MATCHER", "编辑物品匹配模板"),
    EDIT_USER("EDIT_USER", "管理 WebAdmin 用户"),
    EDIT_SYSTEM_SETTINGS("EDIT_SYSTEM_SETTINGS", "编辑系统设置"),
    DANGEROUS_OPERATION("DANGEROUS_OPERATION", "危险操作");

    private final String id;
    private final String displayName;

    WebAdminOperationType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }
}
