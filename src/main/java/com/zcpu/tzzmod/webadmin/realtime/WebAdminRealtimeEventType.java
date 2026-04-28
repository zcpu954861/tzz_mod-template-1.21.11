package com.zcpu.tzzmod.webadmin.realtime;

public enum WebAdminRealtimeEventType {
    REALTIME_CONNECTED("realtime_connected", "实时同步已连接"),
    HEARTBEAT("heartbeat", "实时同步心跳"),
    SIGNAL_EMITTED("signal_emitted", "Signal 已发出"),
    HISTORY_APPENDED("history_appended", "历史记录已追加"),
    DEVICE_UPDATED("device_updated", "设备已更新"),
    DOCTOR_CHANGED("doctor_changed", "诊断状态已变化"),
    ACTION_EXECUTED("action_executed", "动作已执行"),
    RECEIVER_PULSE("receiver_pulse", "接收器脉冲"),
    REGION_EVENT("region_event", "区域事件"),
    CONFIG_CHANGED("config_changed", "配置已变化"),
    WRITE_AUDIT_APPENDED("write_audit_appended", "写入审计已追加"),
    PERMISSION_DENIED("permission_denied", "权限被拒绝"),
    VALIDATION_FAILED("validation_failed", "校验失败"),
    USER_CHANGED("user_changed", "用户配置已变化"),
    SYSTEM_SETTINGS_CHANGED("system_settings_changed", "系统设置已变化"),
    DEVICE_CONFIG_CHANGED("device_config_changed", "设备配置已变化"),
    SIGNAL_CONFIG_CHANGED("signal_config_changed", "Signal 配置已变化"),
    REGION_CONFIG_CHANGED("region_config_changed", "区域配置已变化"),
    ACTION_CONFIG_CHANGED("action_config_changed", "动作配置已变化"),
    WEBADMIN_USER_CONNECTED("webadmin_user_connected", "WebAdmin 用户已连接"),
    WEBADMIN_USER_DISCONNECTED("webadmin_user_disconnected", "WebAdmin 用户已断开");

    private final String id;
    private final String displayName;

    WebAdminRealtimeEventType(String id, String displayName) {
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
