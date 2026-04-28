package com.zcpu.tzzmod.webadmin.realtime;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record WebAdminRealtimeEvent(
        String id,
        String type,
        String occurredAt,
        String channel,
        String deviceId,
        String regionId,
        String actionId,
        String sourceType,
        String severity,
        String summary,
        String routeTarget,
        Map<String, Object> payload
) {
    public WebAdminRealtimeEvent {
        id = safe(id);
        type = safe(type);
        occurredAt = safe(occurredAt);
        channel = safe(channel);
        deviceId = safe(deviceId);
        regionId = safe(regionId);
        actionId = safe(actionId);
        sourceType = safe(sourceType);
        severity = safe(severity);
        summary = safe(summary);
        routeTarget = safe(routeTarget);
        payload = payload == null || payload.isEmpty() ? Map.of() : Map.copyOf(payload);
    }

    public static Builder builder(WebAdminRealtimeEventType type) {
        return new Builder(type);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Builder {
        private final WebAdminRealtimeEventType type;
        private String channel = "";
        private String deviceId = "";
        private String regionId = "";
        private String actionId = "";
        private String sourceType = "";
        private String severity = "";
        private String summary = "";
        private String routeTarget = "";
        private final Map<String, Object> payload = new LinkedHashMap<>();

        private Builder(WebAdminRealtimeEventType type) {
            this.type = type;
        }

        public Builder channel(String channel) {
            this.channel = safe(channel);
            return this;
        }

        public Builder deviceId(String deviceId) {
            this.deviceId = safe(deviceId);
            return this;
        }

        public Builder regionId(String regionId) {
            this.regionId = safe(regionId);
            return this;
        }

        public Builder actionId(String actionId) {
            this.actionId = safe(actionId);
            return this;
        }

        public Builder sourceType(String sourceType) {
            this.sourceType = safe(sourceType);
            return this;
        }

        public Builder severity(String severity) {
            this.severity = safe(severity);
            return this;
        }

        public Builder summary(String summary) {
            this.summary = safe(summary);
            return this;
        }

        public Builder routeTarget(String routeTarget) {
            this.routeTarget = safe(routeTarget);
            return this;
        }

        public Builder payload(String key, Object value) {
            if (key != null && !key.isBlank() && value != null) {
                payload.put(key, value);
            }
            return this;
        }

        public WebAdminRealtimeEvent build(String id) {
            WebAdminRealtimeEventType eventType = type == null ? WebAdminRealtimeEventType.HEARTBEAT : type;
            return new WebAdminRealtimeEvent(
                    id,
                    eventType.id(),
                    Instant.now().toString(),
                    channel,
                    deviceId,
                    regionId,
                    actionId,
                    sourceType,
                    severity,
                    summary.isBlank() ? eventType.displayName() : summary,
                    routeTarget,
                    payload
            );
        }
    }
}
