package com.zcpu.tzzmod.client.webadmin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.network.WebAdminContainerTemplateC2SPayload;
import com.zcpu.tzzmod.network.WebAdminContainerTemplateS2CPayload;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class WebAdminContainerTemplateClient {
    private WebAdminContainerTemplateClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(WebAdminContainerTemplateS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> handlePayload(context.client(), payload))
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.currentScreen instanceof WebAdminContainerTemplatePreviewScreen screen) {
                screen.closeFromServer();
            }
        });
    }

    static void sendOpened(String sessionId, String nonce) {
        JsonObject body = baseBody(sessionId, nonce);
        send("opened", body);
    }

    static void sendCancel(String sessionId, String nonce, String deviceId, String reason) {
        JsonObject body = baseBody(sessionId, nonce);
        body.addProperty("deviceId", deviceId == null ? "" : deviceId);
        body.addProperty("reason", reason == null || reason.isBlank() ? "client_close" : reason);
        send("cancel", body);
    }

    static void sendSave(String sessionId, String nonce, String deviceId, String expectedFingerprint, List<Map<String, Object>> itemConditions) {
        JsonObject body = baseBody(sessionId, nonce);
        body.addProperty("deviceId", deviceId == null ? "" : deviceId);
        body.addProperty("expectedFingerprint", expectedFingerprint == null ? "" : expectedFingerprint);
        body.add("itemConditions", WebAdminContainerTemplatePreviewScreen.GSON.toJsonTree(itemConditions == null ? List.of() : itemConditions));
        send("save", body);
    }

    private static void handlePayload(MinecraftClient client, WebAdminContainerTemplateS2CPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "open" -> {
                if (client.player == null) {
                    sendCancel(getString(body, "sessionId"), getString(body, "nonce"), getString(body, "deviceId"), "client_unavailable");
                    return;
                }
                WebAdminContainerTemplatePreviewScreen screen = WebAdminContainerTemplatePreviewScreen.fromJson(body);
                client.setScreen(screen);
                sendOpened(screen.sessionId(), screen.nonce());
            }
            case "saved", "cancelled", "failed", "expired" -> {
                if (client.currentScreen instanceof WebAdminContainerTemplatePreviewScreen screen
                        && screen.sessionId().equals(getString(body, "sessionId"))) {
                    screen.closeFromServer();
                }
            }
            default -> {
            }
        }
    }

    private static void send(String action, JsonObject body) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return;
        }
        ClientPlayNetworking.send(new WebAdminContainerTemplateC2SPayload(action, body.toString()));
    }

    private static JsonObject baseBody(String sessionId, String nonce) {
        JsonObject body = new JsonObject();
        body.addProperty("sessionId", sessionId == null ? "" : sessionId);
        body.addProperty("nonce", nonce == null ? "" : nonce);
        return body;
    }

    private static JsonObject parse(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new JsonObject();
            }
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
