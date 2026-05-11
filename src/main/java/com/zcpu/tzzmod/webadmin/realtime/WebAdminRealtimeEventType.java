package com.zcpu.tzzmod.webadmin.realtime;

public enum WebAdminRealtimeEventType {
    REALTIME_CONNECTED("realtime_connected", "实时同步已连接"),
    HEARTBEAT("heartbeat", "实时同步心跳"),
    SYNC_REQUIRED("sync_required", "需要重新同步"),
    DEVICE_REGISTERED("device_registered", "设备已注册"),
    DEVICE_REMOVED("device_removed", "设备已移除"),
    DEVICE_CHANGED("device_changed", "设备已变化"),
    DEVICE_METADATA_CHANGED("device_metadata_changed", "设备显示信息已变化"),
    RECEIVER_CHANGED("receiver_changed", "接收器已变化"),
    RECEIVER_PULSE_CHANGED("receiver_pulse_changed", "接收器脉冲已变化"),
    VIRTUAL_BLOCK_DEVICE_CHANGED("virtual_block_device_changed", "虚拟方块设备已变化"),
    SIGNAL_CHANNEL_CHANGED("signal_channel_changed", "Signal 频道已变化"),
    SIGNAL_HISTORY_APPENDED("signal_history_appended", "Signal 历史已追加"),
    SIGNAL_LISTENER_CHANGED("signal_listener_changed", "Signal Listener 已变化"),
    SIGNAL_LISTENER_ENABLED_CHANGED("signal_listener_enabled_changed", "Signal Listener 启用状态已变化"),
    SIGNAL_LISTENER_ACTION_CHANGED("signal_listener_action_changed", "Signal Listener 动作已变化"),
    ACTION_CHANGED("action_changed", "动作已变化"),
    ACTION_HISTORY_APPENDED("action_history_appended", "动作历史已追加"),
    ACTION_EXECUTION_APPENDED("action_execution_appended", "动作执行已追加"),
    REGION_CHANGED("region_changed", "区域已变化"),
    REGION_CONTROLLER_CHANGED("region_controller_changed", "区域控制器已变化"),
    REGION_EVENT_APPENDED("region_event_appended", "区域事件已追加"),
    DOCTOR_ISSUES_CHANGED("doctor_issues_changed", "诊断问题已变化"),
    WEBADMIN_USER_CHANGED("webadmin_user_changed", "WebAdmin 用户已变化"),
    WEBADMIN_AUDIT_APPENDED("webadmin_audit_appended", "WebAdmin 审计已追加"),
    WEBADMIN_SETTINGS_CHANGED("webadmin_settings_changed", "WebAdmin 设置已变化"),
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
    CHANNEL_METADATA_CHANGED("channel_metadata_changed", "频道显示信息已变化"),
    SIGNAL_LISTENER_CONFIG_CHANGED("signal_listener_config_changed", "Signal Listener 配置已变化"),
    SELECTION_STARTED("selection_started", "对象选择已开始"),
    SELECTION_COMPLETED("selection_completed", "对象选择已完成"),
    SELECTION_CANCELLED("selection_cancelled", "对象选择已取消"),
    SELECTION_FAILED("selection_failed", "对象选择失败"),
    CONTAINER_TEMPLATE_SESSION_STARTED("container_template_session_started", "容器模板会话已开始"),
    CONTAINER_TEMPLATE_SESSION_OPENED("container_template_session_opened", "容器模板 GUI 已打开"),
    CONTAINER_TEMPLATE_SESSION_SAVED("container_template_session_saved", "容器模板会话已保存"),
    CONTAINER_TEMPLATE_SESSION_CANCELLED("container_template_session_cancelled", "容器模板会话已取消"),
    CONTAINER_TEMPLATE_SESSION_FAILED("container_template_session_failed", "容器模板会话失败"),
    CONTAINER_TEMPLATE_SESSION_EXPIRED("container_template_session_expired", "容器模板会话已过期"),
    REGION_CONFIG_CHANGED("region_config_changed", "区域配置已变化"),
    ACTION_CONFIG_CHANGED("action_config_changed", "动作配置已变化"),
    EDIT_LOCK_CHANGED("edit_lock_changed", "编辑锁状态已变化"),
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
