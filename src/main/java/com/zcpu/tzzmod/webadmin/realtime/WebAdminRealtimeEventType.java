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
