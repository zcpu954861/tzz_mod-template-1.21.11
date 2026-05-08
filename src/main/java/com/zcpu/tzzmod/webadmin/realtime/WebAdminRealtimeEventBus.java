package com.zcpu.tzzmod.webadmin.realtime;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionContext;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.region.RegionControllerData;
import com.zcpu.tzzmod.region.RegionTriggerType;
import com.zcpu.tzzmod.signal.SignalEventRecord;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WebAdminRealtimeEventBus {
    private static final int MAX_RECENT_EVENTS = 512;
    private static final AtomicLong NEXT_SEQ = new AtomicLong(1L);
    private static final CopyOnWriteArraySet<WebAdminRealtimeClient> CLIENTS = new CopyOnWriteArraySet<>();
    private static final Deque<WebAdminRealtimeEvent> RECENT_EVENTS = new ArrayDeque<>();

    private WebAdminRealtimeEventBus() {
    }

    public static WebAdminRealtimeClient subscribe(String username, String role) {
        return subscribe(username, role, 0L);
    }

    public static WebAdminRealtimeClient subscribe(String username, String role, long lastSeenSeq) {
        WebAdminRealtimeClient client = new WebAdminRealtimeClient(username, role);
        CLIENTS.add(client);
        for (WebAdminRealtimeEvent event : replayEventsSince(lastSeenSeq)) {
            client.offer(event);
        }
        publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WEBADMIN_USER_CONNECTED)
                .summary("WebAdmin 用户已连接")
                .payload("username", username == null ? "" : username)
                .payload("role", role == null ? "" : role));
        return client;
    }

    public static void unsubscribe(WebAdminRealtimeClient client) {
        if (client == null) {
            return;
        }
        boolean removed = CLIENTS.remove(client);
        client.close();
        if (removed) {
            publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WEBADMIN_USER_DISCONNECTED)
                    .summary("WebAdmin 用户已断开")
                    .payload("username", client.username())
                    .payload("role", client.role()));
        }
    }

    public static void closeAll() {
        for (WebAdminRealtimeClient client : CLIENTS) {
            client.close();
        }
        CLIENTS.clear();
    }

    public static int clientCount() {
        return CLIENTS.size();
    }

    public static WebAdminRealtimeEvent connectedEvent(String username, String role, long lastSeenSeq) {
        return WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.REALTIME_CONNECTED)
                .summary("实时同步已连接")
                .payload("username", username == null ? "" : username)
                .payload("role", role == null ? "" : role)
                .payload("lastSeenSeq", Math.max(0L, lastSeenSeq))
                .payload("latestSeq", currentSeq())
                .build(Math.max(0L, lastSeenSeq));
    }

    public static WebAdminRealtimeEvent heartbeatEvent() {
        return WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.HEARTBEAT)
                .summary("实时同步心跳")
                .payload("latestSeq", currentSeq())
                .build(currentSeq());
    }

    public static WebAdminRealtimeEvent publish(WebAdminRealtimeEvent.Builder builder) {
        if (builder == null) {
            return null;
        }
        WebAdminRealtimeEvent event = builder.build(NEXT_SEQ.getAndIncrement());
        remember(event);
        for (WebAdminRealtimeClient client : CLIENTS) {
            client.offer(event);
        }
        return event;
    }

    public static void publishSignalHistory(SignalEventRecord record) {
        if (record == null) {
            return;
        }
        String channel = record.channel() == null || record.channel().isBlank() ? "unknown" : record.channel();
        String result = record.failedCount() > 0 ? "FAILED" : "SUCCESS";
        Map<String, Object> payload = Map.of(
                "playerName", safe(record.playerName()),
                "sourceId", safe(record.sourceId()),
                "result", result,
                "listenerCount", record.listenerCount(),
                "executedCount", record.executedCount(),
                "failedCount", record.failedCount()
        );
        publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.SIGNAL_EMITTED)
                .channel(channel)
                .sourceType(record.sourceType())
                .severity(record.failedCount() > 0 ? "WARNING" : "INFO")
                .summary("Signal 已发出：" + channel)
                .routeTarget("#/signals/" + encode(channel))
                .payload("result", result)
                .payload("sourceId", safe(record.sourceId()))
                .payload("playerName", safe(record.playerName())));
        publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.SIGNAL_HISTORY_APPENDED)
                .channel(channel)
                .sourceType(record.sourceType())
                .severity(record.failedCount() > 0 ? "WARNING" : "INFO")
                .summary("Signal 历史已追加：" + channel)
                .routeTarget("#/history?channel=" + encode(channel))
                .payload("history", payload));
        publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.HISTORY_APPENDED)
                .channel(channel)
                .sourceType(record.sourceType())
                .severity(record.failedCount() > 0 ? "WARNING" : "INFO")
                .summary("Signal 历史已追加：" + channel)
                .routeTarget("#/history?channel=" + encode(channel))
                .payload("history", payload));
    }

    public static void publishDeviceEvent(WebAdminRealtimeEventType type, SignalDeviceData device, String summary) {
        if (device == null) {
            return;
        }
        SignalDeviceData normalized = device.normalized();
        publish(WebAdminRealtimeEvent.builder(type == null ? WebAdminRealtimeEventType.DEVICE_CHANGED : type)
                .deviceId(normalized.id())
                .channel(normalized.channel())
                .sourceType(normalized.type())
                .severity("INFO")
                .summary(summary == null || summary.isBlank() ? "设备已变化：" + displayDeviceName(normalized) : summary)
                .routeTarget("#/devices/" + encode(normalized.id()))
                .payload("deviceType", normalized.type())
                .payload("enabled", normalized.enabled())
                .payload("channel", normalized.channel())
                .payload("pulseTicks", normalized.pulseTicks())
                .payload("cooldownTicks", normalized.cooldownTicks())
                .payload("actionCount", normalized.actionCount()));
    }

    public static void publishDeviceRemoved(String deviceId, String sourceType) {
        publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.DEVICE_REMOVED)
                .deviceId(deviceId)
                .sourceType(sourceType)
                .severity("INFO")
                .summary("设备已移除")
                .routeTarget("#/devices")
                .payload("removed", true));
    }

    public static void publishSignalListenerEvent(WebAdminRealtimeEventType type, SignalListenerData listener, String summary) {
        if (listener == null) {
            return;
        }
        SignalListenerData normalized = listener.normalized();
        publish(WebAdminRealtimeEvent.builder(type == null ? WebAdminRealtimeEventType.SIGNAL_LISTENER_CHANGED : type)
                .channel(normalized.channel())
                .sourceType("signal_listener")
                .severity("INFO")
                .summary(summary == null || summary.isBlank() ? "Signal Listener 已变化：" + displayListenerName(normalized) : summary)
                .routeTarget("#/signals/" + encode(normalized.channel()))
                .payload("listenerId", normalized.id())
                .payload("enabled", normalized.enabled())
                .payload("channel", normalized.channel())
                .payload("cooldownTicks", normalized.cooldownTicks())
                .payload("actionCount", normalized.actions().size()));
    }

    public static void publishActionExecution(ActionContext context, ActionConfig config, ActionExecutionResult result) {
        String sourceType = context == null || context.sourceType() == null ? "unknown" : context.sourceType().id();
        String sourceId = context == null ? "" : safe(context.sourceId());
        String actionType = config == null || config.type() == null ? "unknown" : config.type().id();
        boolean success = result != null && result.success();
        String message = result == null || result.message() == null ? "" : result.message().getString();
        publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.ACTION_EXECUTION_APPENDED)
                .actionId(sourceId)
                .sourceType(sourceType)
                .severity(success ? "INFO" : "WARNING")
                .summary(success ? "动作执行已记录" : "动作执行失败已记录")
                .routeTarget("#/actions")
                .payload("sourceId", sourceId)
                .payload("sourceType", sourceType)
                .payload("actionType", actionType)
                .payload("success", success)
                .payload("message", message));
        publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.ACTION_EXECUTED)
                .actionId(sourceId)
                .sourceType(sourceType)
                .severity(success ? "INFO" : "WARNING")
                .summary(success ? "动作已执行" : "动作执行失败")
                .routeTarget("#/actions")
                .payload("sourceId", sourceId)
                .payload("sourceType", sourceType)
                .payload("actionType", actionType)
                .payload("success", success));
    }

    public static void publishRegionControllerEvent(WebAdminRealtimeEventType type, RegionControllerData controller, String summary) {
        if (controller == null) {
            return;
        }
        RegionControllerData normalized = controller.normalized();
        publish(WebAdminRealtimeEvent.builder(type == null ? WebAdminRealtimeEventType.REGION_CONTROLLER_CHANGED : type)
                .regionId(normalized.regionId())
                .sourceType("region_controller")
                .severity("INFO")
                .summary(summary == null || summary.isBlank() ? "区域控制器已变化：" + displayRegionControllerName(normalized) : summary)
                .routeTarget("#/regions/" + encode(normalized.regionId()))
                .payload("controllerId", normalized.id())
                .payload("regionId", normalized.regionId())
                .payload("enabled", normalized.enabled()));
    }

    public static void publishRegionRuntimeEvent(RegionControllerData controller, RegionTriggerType triggerType, ServerPlayerEntity player) {
        if (controller == null) {
            return;
        }
        RegionControllerData normalized = controller.normalized();
        String trigger = triggerType == null ? "unknown" : triggerType.name().toLowerCase(Locale.ROOT);
        publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.REGION_EVENT_APPENDED)
                .regionId(normalized.regionId())
                .sourceType("region_controller")
                .severity("INFO")
                .summary("区域事件已追加：" + trigger)
                .routeTarget("#/regions/" + encode(normalized.regionId()))
                .payload("controllerId", normalized.id())
                .payload("regionId", normalized.regionId())
                .payload("triggerType", trigger)
                .payload("playerName", player == null ? "" : player.getName().getString()));
        publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.REGION_EVENT)
                .regionId(normalized.regionId())
                .sourceType("region_controller")
                .severity("INFO")
                .summary("区域事件：" + trigger)
                .routeTarget("#/regions/" + encode(normalized.regionId()))
                .payload("controllerId", normalized.id())
                .payload("triggerType", trigger));
    }

    public static List<WebAdminRealtimeEvent> recentEvents() {
        synchronized (RECENT_EVENTS) {
            return List.copyOf(RECENT_EVENTS);
        }
    }

    public static long currentSeq() {
        return Math.max(0L, NEXT_SEQ.get() - 1L);
    }

    private static List<WebAdminRealtimeEvent> replayEventsSince(long lastSeenSeq) {
        if (lastSeenSeq <= 0L) {
            return List.of();
        }
        List<WebAdminRealtimeEvent> events;
        synchronized (RECENT_EVENTS) {
            events = List.copyOf(RECENT_EVENTS);
        }
        if (events.isEmpty()) {
            return currentSeq() > lastSeenSeq ? List.of(syncRequiredEvent(lastSeenSeq, "recent_buffer_empty")) : List.of();
        }
        long newestSeq = events.get(events.size() - 1).seq();
        if (newestSeq <= lastSeenSeq) {
            return List.of();
        }
        long oldestSeq = events.get(0).seq();
        if (lastSeenSeq < oldestSeq - 1L) {
            return List.of(syncRequiredEvent(lastSeenSeq, "recent_buffer_expired"));
        }
        return events.stream()
                .filter(event -> event.seq() > lastSeenSeq)
                .toList();
    }

    private static WebAdminRealtimeEvent syncRequiredEvent(long lastSeenSeq, String reason) {
        return WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.SYNC_REQUIRED)
                .severity("WARNING")
                .summary("实时事件缓冲不足，需要静默补同步。")
                .payload("lastSeenSeq", Math.max(0L, lastSeenSeq))
                .payload("latestSeq", currentSeq())
                .payload("reason", reason)
                .build(currentSeq());
    }

    private static void remember(WebAdminRealtimeEvent event) {
        synchronized (RECENT_EVENTS) {
            while (RECENT_EVENTS.size() >= MAX_RECENT_EVENTS) {
                RECENT_EVENTS.removeFirst();
            }
            RECENT_EVENTS.addLast(event);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static String displayDeviceName(SignalDeviceData device) {
        if (device == null) {
            return "unknown";
        }
        return device.name() == null || device.name().isBlank() ? device.id() : device.name();
    }

    private static String displayListenerName(SignalListenerData listener) {
        if (listener == null) {
            return "unknown";
        }
        return listener.name() == null || listener.name().isBlank() ? listener.id() : listener.name();
    }

    private static String displayRegionControllerName(RegionControllerData controller) {
        if (controller == null) {
            return "unknown";
        }
        return controller.name() == null || controller.name().isBlank() ? controller.id() : controller.name();
    }
}
