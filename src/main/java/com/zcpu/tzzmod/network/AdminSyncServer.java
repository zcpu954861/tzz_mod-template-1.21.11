package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.atomic.AtomicBoolean;

public final class AdminSyncServer {
    private static final AtomicBoolean recordingMode = new AtomicBoolean(false);
    private static final AtomicBoolean galleryEnabled = new AtomicBoolean(true);

    private AdminSyncServer() {
    }

    public static void register() {
        // send current state to joining players
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> sendStateToPlayer(server, handler.getPlayer()))
        );

        // register C2S receiver
        ServerPlayNetworking.registerGlobalReceiver(AdminModeC2SPayload.ID, (payload, context) ->
                context.server().execute(() -> handleClientToggle(context.server(), context.player(), payload))
        );
    }

    private static void handleClientToggle(MinecraftServer server, ServerPlayerEntity player, AdminModeC2SPayload payload) {
        // only allow server operators to toggle
        if (!player.isCreativeLevelTwoOp()) {
            return;
        }

        boolean changed = false;
        switch (payload.key()) {
            case "recording" -> {
                boolean prev = recordingMode.getAndSet(payload.value());
                changed = prev != payload.value();
            }
            case "gallery_enabled" -> {
                boolean prev = galleryEnabled.getAndSet(payload.value());
                changed = prev != payload.value();
            }
        }
        if (changed) {
            broadcastState(server);
        }
    }

    private static void broadcastState(MinecraftServer server) {
        AdminModePayload payload = new AdminModePayload(recordingMode.get(), galleryEnabled.get());
        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(NullSafety.requireNonNull(online), payload);
        }
    }

    private static void sendStateToPlayer(MinecraftServer server, ServerPlayerEntity player) {
        ServerPlayNetworking.send(NullSafety.requireNonNull(player),
                new AdminModePayload(recordingMode.get(), galleryEnabled.get()));
    }

    public static boolean isRecordingMode() {
        return recordingMode.get();
    }
}
