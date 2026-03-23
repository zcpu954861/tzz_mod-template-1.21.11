package com.zcpu.tzzmod.client.phone;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PhoneAppsClient {
    private static volatile Map<String, String> apps = new LinkedHashMap<>();

    private PhoneAppsClient() {}

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> apps = new LinkedHashMap<>());
    }

    public static void apply(JsonObject appsObj) {
        if (appsObj == null) {
            apps = new LinkedHashMap<>();
            return;
        }
        Map<String, String> next = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : appsObj.entrySet()) {
            try {
                String v = e.getValue().getAsString();
                next.put(e.getKey(), v == null ? "true" : v.trim().toLowerCase());
            } catch (Exception ex) {
                next.put(e.getKey(), "true");
            }
        }
        apps = next;
    }

    public static String getVisibility(String appId) {
        return apps.getOrDefault(appId, "true");
    }

    public static Map<String, String> getAll() {
        return Map.copyOf(apps);
    }
}

