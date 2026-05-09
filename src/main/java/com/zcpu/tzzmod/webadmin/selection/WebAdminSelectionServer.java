package com.zcpu.tzzmod.webadmin.selection;

import com.zcpu.tzzmod.network.WebAdminSelectionC2SPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class WebAdminSelectionServer {
    private WebAdminSelectionServer() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(WebAdminSelectionC2SPayload.ID, (payload, context) ->
                context.server().execute(() -> handlePayload(context.server(), context.player(), payload))
        );
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                server.execute(() -> WebAdminSelectionSessions.cancelForDisconnect(handler.getPlayer()))
        );
    }

    private static void handlePayload(
            net.minecraft.server.MinecraftServer server,
            net.minecraft.server.network.ServerPlayerEntity player,
            WebAdminSelectionC2SPayload payload
    ) {
        switch (payload.action()) {
            case "complete" -> WebAdminSelectionSessions.completeFromClient(server, player, payload.bodyJson());
            case "cancel" -> WebAdminSelectionSessions.cancelFromClient(server, player, payload.bodyJson());
            default -> {
            }
        }
    }
}
