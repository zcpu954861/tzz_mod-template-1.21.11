package com.zcpu.tzzmod.client.webadmin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.network.WebAdminSingleItemSubmitTemplateC2SPayload;
import com.zcpu.tzzmod.network.WebAdminSingleItemSubmitTemplateS2CPayload;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class WebAdminSingleItemSubmitTemplateClient {
    private WebAdminSingleItemSubmitTemplateClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(WebAdminSingleItemSubmitTemplateS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> handlePayload(context.client(), payload))
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.currentScreen instanceof WebAdminSingleItemSubmitTemplateScreen screen) {
                screen.closeFromServer();
            }
        });
    }

    static void sendOpened(String sessionId, String nonce) {
        send("opened", baseBody(sessionId, nonce));
    }

    static void sendCancel(String sessionId, String nonce, String deviceId, String reason) {
        JsonObject body = baseBody(sessionId, nonce);
        body.addProperty("deviceId", deviceId == null ? "" : deviceId);
        body.addProperty("reason", reason == null || reason.isBlank() ? "client_close" : reason);
        send("cancel", body);
    }

    static void sendSave(String sessionId, String nonce, String deviceId, String expectedFingerprint, Map<String, Object> template) {
        JsonObject body = baseBody(sessionId, nonce);
        body.addProperty("deviceId", deviceId == null ? "" : deviceId);
        body.addProperty("expectedFingerprint", expectedFingerprint == null ? "" : expectedFingerprint);
        body.add("template", WebAdminSingleItemSubmitTemplateScreen.GSON.toJsonTree(template == null ? Map.of() : template));
        send("save", body);
    }

    private static void handlePayload(MinecraftClient client, WebAdminSingleItemSubmitTemplateS2CPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "open" -> {
                if (client.player == null) {
                    sendCancel(getString(body, "sessionId"), getString(body, "nonce"), getString(body, "deviceId"), "client_unavailable");
                    return;
                }
                WebAdminSingleItemSubmitTemplateScreen screen = WebAdminSingleItemSubmitTemplateScreen.fromJson(body);
                client.setScreen(screen);
                sendOpened(screen.sessionId(), screen.nonce());
            }
            case "saved", "cancelled", "failed", "expired" -> {
                if (client.currentScreen instanceof WebAdminSingleItemSubmitTemplateScreen screen
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
        ClientPlayNetworking.send(new WebAdminSingleItemSubmitTemplateC2SPayload(action, body.toString()));
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
