package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.atomic.AtomicBoolean;

public final class AdminSyncServer {
    private static final AtomicBoolean recordingMode = new AtomicBoolean(false);

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

        boolean newState = payload.recording();
        boolean previous = recordingMode.getAndSet(newState);
        if (previous != newState) {
            // broadcast new state to all online players
            broadcastState(server, newState);
        }
    }

    private static void broadcastState(MinecraftServer server, boolean state) {
        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(online, new AdminModePayload(state));
        }
    }

    private static void sendStateToPlayer(MinecraftServer server, ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new AdminModePayload(recordingMode.get()));
    }

    public static boolean isRecordingMode() {
        return recordingMode.get();
    }
}
