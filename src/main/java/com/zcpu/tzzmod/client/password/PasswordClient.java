package com.zcpu.tzzmod.client.password;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.client.phone.ui.app.PasswordCardScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PasswordMachineScreen;
import com.zcpu.tzzmod.network.PasswordC2SPayload;
import com.zcpu.tzzmod.network.PasswordS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class PasswordClient {
    private PasswordClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PasswordS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> handlePayload(context.client(), payload))
        );
    }

    public static void submitMachineAttempt(BlockPos pos, String code) {
        JsonObject body = new JsonObject();
        body.addProperty("x", pos.getX());
        body.addProperty("y", pos.getY());
        body.addProperty("z", pos.getZ());
        body.addProperty("code", code);
        send("attempt_machine", body);
    }

    public static void saveCardCode(Hand hand, String code) {
        JsonObject body = new JsonObject();
        body.addProperty("hand", hand == Hand.OFF_HAND ? "off_hand" : "main_hand");
        body.addProperty("code", code);
        send("save_card", body);
    }

    private static void send(String action, JsonObject body) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) {
            return;
        }
        ClientPlayNetworking.send(new PasswordC2SPayload(action, body.toString()));
    }

    private static void handlePayload(MinecraftClient client, PasswordS2CPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        boolean success = getBoolean(body, "success", false);
        String message = getString(body, "message");
        boolean close = getBoolean(body, "close", false);

        if (client.currentScreen instanceof PasswordMachineScreen screen && "machine_result".equals(payload.action())) {
            screen.handleServerResult(success, message, close);
            return;
        }
        if (client.currentScreen instanceof PasswordCardScreen screen && "card_saved".equals(payload.action())) {
            screen.handleServerResult(success, message, close);
        }
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

