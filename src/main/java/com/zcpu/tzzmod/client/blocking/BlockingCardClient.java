package com.zcpu.tzzmod.client.blocking;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.client.phone.ui.app.BlockingCardConfiguratorScreen;
import com.zcpu.tzzmod.network.BlockingCardC2SPayload;
import com.zcpu.tzzmod.network.BlockingCardS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

public final class BlockingCardClient {
    private BlockingCardClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(BlockingCardS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> handlePayload(context.client(), payload))
        );
    }

    public static void saveConfiguration(Hand hand, String activationType, String activationInput, String command, boolean notifyOps) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) {
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("hand", hand == Hand.OFF_HAND ? "off_hand" : "main_hand");
        body.addProperty("activationType", activationType);
        body.addProperty("activationInput", activationInput);
        body.addProperty("command", command);
        body.addProperty("notifyOps", notifyOps);
        ClientPlayNetworking.send(new BlockingCardC2SPayload("save_config", body.toString()));
    }

    private static void handlePayload(MinecraftClient client, BlockingCardS2CPayload payload) {
        if (!(client.currentScreen instanceof BlockingCardConfiguratorScreen screen)) {
            return;
        }

        JsonObject body = parse(payload.bodyJson());
        boolean success = getBoolean(body, "success", false);
        String message = getString(body, "message");
        screen.handleServerResult(success, message);
    }

    private static JsonObject parse(String body) {
        try {
            if (body == null || body.isBlank()) {
                return new JsonObject();
            }
            return JsonParser.parseString(body).getAsJsonObject();
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

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }
}