package com.zcpu.tzzmod.client;

import com.zcpu.tzzmod.network.DeathStatusPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class DeathSyncClient {
    private static volatile boolean localPlayerDead;

    private DeathSyncClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DeathStatusPayload.ID, (payload, context) ->
                context.client().execute(() -> localPlayerDead = payload.hasDeath())
        );

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> localPlayerDead = false);
    }

    public static boolean isLocalPlayerDead() {
        return localPlayerDead;
    }
}
