package com.zcpu.tzzmod.client.phone;

import com.google.gson.JsonObject;
import com.zcpu.tzzmod.network.PhoneChatC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class PhoneCallAdminClient {
    private PhoneCallAdminClient() {
    }

    private static long cooldownExpiresAt = 0L;

    public static void sendCall() {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) return;
        JsonObject body = new JsonObject();
        // no extra data needed for now
        ClientPlayNetworking.send(new PhoneChatC2SPayload("call_admin", body.toString()));
    }

    // Start a client-side cooldown (milliseconds)
    public static void startCooldown(long millis) {
        cooldownExpiresAt = System.currentTimeMillis() + Math.max(0, millis);
    }

    public static boolean isCoolingDown() {
        return System.currentTimeMillis() < cooldownExpiresAt;
    }

    public static int getRemainingSeconds() {
        long now = System.currentTimeMillis();
        long remaining = Math.max(0L, cooldownExpiresAt - now);
        return (int) ((remaining + 999L) / 1000L);
    }
}
