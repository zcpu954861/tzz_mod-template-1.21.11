package com.zcpu.tzzmod.webadmin.realtime;

import com.zcpu.tzzmod.signal.SignalEventRecord;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

public final class WebAdminRealtimeEventBus {
    private static final int MAX_RECENT_EVENTS = 256;
    private static final AtomicLong NEXT_ID = new AtomicLong(1L);
    private static final CopyOnWriteArraySet<WebAdminRealtimeClient> CLIENTS = new CopyOnWriteArraySet<>();
    private static final Deque<WebAdminRealtimeEvent> RECENT_EVENTS = new ArrayDeque<>();

    private WebAdminRealtimeEventBus() {
    }

    public static WebAdminRealtimeClient subscribe(String username, String role) {
        WebAdminRealtimeClient client = new WebAdminRealtimeClient(username, role);
        CLIENTS.add(client);
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

    public static WebAdminRealtimeEvent connectedEvent(String username, String role) {
        return WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.REALTIME_CONNECTED)
                .summary("实时同步已连接")
                .payload("username", username == null ? "" : username)
                .payload("role", role == null ? "" : role)
                .build(String.valueOf(NEXT_ID.getAndIncrement()));
    }

    public static WebAdminRealtimeEvent heartbeatEvent() {
        return WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.HEARTBEAT)
                .summary("实时同步心跳")
                .build(String.valueOf(NEXT_ID.getAndIncrement()));
    }

    public static WebAdminRealtimeEvent publish(WebAdminRealtimeEvent.Builder builder) {
        if (builder == null) {
            return null;
        }
        WebAdminRealtimeEvent event = builder.build(String.valueOf(NEXT_ID.getAndIncrement()));
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
        publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.HISTORY_APPENDED)
                .channel(channel)
                .sourceType(record.sourceType())
                .severity(record.failedCount() > 0 ? "WARNING" : "INFO")
                .summary("Signal 历史已追加：" + channel)
                .routeTarget("#/history?channel=" + encode(channel))
                .payload("history", payload));
    }

    public static List<WebAdminRealtimeEvent> recentEvents() {
        synchronized (RECENT_EVENTS) {
            return List.copyOf(RECENT_EVENTS);
        }
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
}
