package com.zcpu.tzzmod.webadmin.write;

public enum WebAdminOperationType {
    READ("READ", "只读查看"),
    TEST("TEST", "测试 / 校验"),
    ACQUIRE_EDIT_LOCK("ACQUIRE_EDIT_LOCK", "获取编辑锁"),
    RELEASE_EDIT_LOCK("RELEASE_EDIT_LOCK", "释放编辑锁"),
    EDIT_DEVICE_METADATA("EDIT_DEVICE_METADATA", "编辑 WebAdmin 设备显示信息"),
    EDIT_DEVICE_BASIC_CONFIG("EDIT_DEVICE_BASIC_CONFIG", "编辑 WebAdmin 设备基础配置"),
    EDIT_DEVICE_EXTENDED_CONFIG("EDIT_DEVICE_EXTENDED_CONFIG", "编辑 WebAdmin 设备扩展配置"),
    EDIT_ACTION_RELAY_ACTIONS("EDIT_ACTION_RELAY_ACTIONS", "编辑 Action Relay 动作列表"),
    EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS("EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS", "编辑 VBD 原生触发配置"),
    START_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION("START_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION", "发起 VBD 容器变化模板会话"),
    SAVE_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE("SAVE_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE", "保存 VBD 容器变化模板"),
    CANCEL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION("CANCEL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION", "取消 VBD 容器变化模板会话"),
    FAIL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION("FAIL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION", "结束失败的 VBD 容器变化模板会话"),
    EDIT_CHANNEL_METADATA("EDIT_CHANNEL_METADATA", "编辑 WebAdmin 频道显示信息"),
    EDIT_SIGNAL_LISTENER_BASIC_CONFIG("EDIT_SIGNAL_LISTENER_BASIC_CONFIG", "编辑 Signal Listener 基础配置"),
    START_OBJECT_SELECTION("START_OBJECT_SELECTION", "发起 WebAdmin 对象选择"),
    DELETE_VIRTUAL_BLOCK_DEVICE("DELETE_VIRTUAL_BLOCK_DEVICE", "删除 / 解绑虚拟方块设备"),
    CREATE_SIGNAL_LISTENER("CREATE_SIGNAL_LISTENER", "创建 Signal Listener"),
    DELETE_SIGNAL_LISTENER("DELETE_SIGNAL_LISTENER", "删除 Signal Listener"),
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
