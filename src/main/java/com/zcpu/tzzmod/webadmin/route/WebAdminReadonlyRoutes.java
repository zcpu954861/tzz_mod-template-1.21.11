package com.zcpu.tzzmod.webadmin.route;

import com.sun.net.httpserver.HttpExchange;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.webadmin.WebAdminJsonResponse;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.service.WebAdminActionService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDoctorService;
import com.zcpu.tzzmod.webadmin.service.WebAdminRegionService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSignalService;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminReadonlyRoutes {
    private final WebAdminDeviceService deviceService = new WebAdminDeviceService();
    private final WebAdminSignalService signalService = new WebAdminSignalService();
    private final WebAdminDoctorService doctorService = new WebAdminDoctorService();
    private final WebAdminRegionService regionService = new WebAdminRegionService();
    private final WebAdminActionService actionService = new WebAdminActionService();

    public boolean handle(HttpExchange exchange, MinecraftServer server, String path) throws IOException {
        if (!path.startsWith("/api/")) {
            return false;
        }

        if (path.equals("/api/devices")) {
            if (!requireGet(exchange)) {
                return true;
            }
            WebAdminJsonResponse.ok(exchange, deviceService.listDevices(server, intQuery(exchange, "limit", 500)));
            return true;
        }
        if (path.startsWith("/api/devices/")) {
            if (!requireGet(exchange)) {
                return true;
            }
            return handleDevice(exchange, server, path.substring("/api/devices/".length()));
        }

        if (path.equals("/api/signals/channels")) {
            if (!requireGet(exchange)) {
                return true;
            }
            WebAdminJsonResponse.ok(exchange, signalService.listChannels(server, intQuery(exchange, "limit", 500)));
            return true;
        }
        if (path.startsWith("/api/signals/channels/")) {
            if (!requireGet(exchange)) {
                return true;
            }
            String channel = decode(path.substring("/api/signals/channels/".length()));
            if (!signalService.channelExists(server, channel)) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "频道不存在。");
                return true;
            }
            WebAdminJsonResponse.ok(exchange, signalService.channelDetail(server, channel));
            return true;
        }
        if (path.equals("/api/signals/history")) {
            if (!requireGet(exchange)) {
                return true;
            }
            WebAdminJsonResponse.ok(exchange, signalService.history(
                    server,
                    query(exchange).getOrDefault("channel", ""),
                    intQuery(exchange, "limit", 100)
            ));
            return true;
        }

        if (path.equals("/api/doctor")) {
            if (!requireGet(exchange)) {
                return true;
            }
            WebAdminJsonResponse.ok(exchange, doctorService.report(server));
            return true;
        }

        if (path.equals("/api/regions")) {
            if (!requireGet(exchange)) {
                return true;
            }
            WebAdminJsonResponse.ok(exchange, regionService.listRegions(server, intQuery(exchange, "limit", 500)));
            return true;
        }
        if (path.startsWith("/api/regions/")) {
            if (!requireGet(exchange)) {
                return true;
            }
            String id = decode(path.substring("/api/regions/".length()));
            WebAdminDtos.RegionDetailDto region = regionService.detail(server, id);
            if (region == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "区域不存在。");
                return true;
            }
            WebAdminJsonResponse.ok(exchange, region);
            return true;
        }

        if (path.equals("/api/actions")) {
            if (!requireGet(exchange)) {
                return true;
            }
            WebAdminJsonResponse.ok(exchange, actionService.listActions(server, intQuery(exchange, "limit", 500)));
            return true;
        }
        if (path.startsWith("/api/actions/")) {
            if (!requireGet(exchange)) {
                return true;
            }
            String id = decode(path.substring("/api/actions/".length()));
            var action = actionService.findAction(server, id);
            if (action == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "动作不存在。");
                return true;
            }
            WebAdminJsonResponse.ok(exchange, action);
            return true;
        }

        return false;
    }

    private boolean handleDevice(HttpExchange exchange, MinecraftServer server, String tail) throws IOException {
        boolean debug = tail.endsWith("/debug");
        String rawId = debug ? tail.substring(0, tail.length() - "/debug".length()) : tail;
        String id = decode(rawId);
        SignalDeviceData device = deviceService.findDevice(server, id);
        if (device == null) {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "设备不存在。");
            return true;
        }
        if (debug) {
            WebAdminJsonResponse.ok(exchange, deviceService.debug(server, device));
        } else {
            WebAdminJsonResponse.ok(exchange, deviceService.detail(server, device));
        }
        return true;
    }

    private static boolean requireGet(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
            return false;
        }
        return true;
    }

    private static int intQuery(HttpExchange exchange, String name, int fallback) {
        String value = query(exchange).get(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> result = new LinkedHashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String entry : raw.split("&")) {
            if (entry.isBlank()) {
                continue;
            }
            String[] parts = entry.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length == 2 ? decode(parts[1]) : "";
            result.put(key, value);
        }
        return result;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

}
