package com.zcpu.tzzmod.webadmin.container;

import com.zcpu.tzzmod.network.WebAdminContainerTemplateC2SPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WebAdminContainerTemplateServer {
    private WebAdminContainerTemplateServer() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(WebAdminContainerTemplateC2SPayload.ID, (payload, context) ->
                context.server().execute(() -> handlePayload(context.server(), context.player(), payload))
        );
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                server.execute(() -> WebAdminContainerTemplateSessions.cancelForDisconnect(handler.getPlayer()))
        );
    }

    private static void handlePayload(
            MinecraftServer server,
            ServerPlayerEntity player,
            WebAdminContainerTemplateC2SPayload payload
    ) {
        switch (payload.action()) {
            case "opened" -> WebAdminContainerTemplateSessions.openedFromClient(server, player, payload.bodyJson());
            case "cancel" -> WebAdminContainerTemplateSessions.cancelFromClient(server, player, payload.bodyJson());
            default -> {
            }
        }
    }
}
