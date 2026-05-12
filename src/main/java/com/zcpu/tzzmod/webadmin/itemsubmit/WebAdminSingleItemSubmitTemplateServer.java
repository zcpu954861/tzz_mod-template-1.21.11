package com.zcpu.tzzmod.webadmin.itemsubmit;

import com.zcpu.tzzmod.network.WebAdminSingleItemSubmitTemplateC2SPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WebAdminSingleItemSubmitTemplateServer {
    private WebAdminSingleItemSubmitTemplateServer() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(WebAdminSingleItemSubmitTemplateC2SPayload.ID, (payload, context) ->
                context.server().execute(() -> handlePayload(context.server(), context.player(), payload))
        );
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                server.execute(() -> WebAdminSingleItemSubmitTemplateSessions.cancelForDisconnect(handler.getPlayer()))
        );
    }

    private static void handlePayload(
            MinecraftServer server,
            ServerPlayerEntity player,
            WebAdminSingleItemSubmitTemplateC2SPayload payload
    ) {
        switch (payload.action()) {
            case "opened" -> WebAdminSingleItemSubmitTemplateSessions.openedFromClient(server, player, payload.bodyJson());
            case "save" -> WebAdminSingleItemSubmitTemplateSessions.saveFromClient(server, player, payload.bodyJson());
            case "cancel" -> WebAdminSingleItemSubmitTemplateSessions.cancelFromClient(server, player, payload.bodyJson());
            default -> {
            }
        }
    }
}
