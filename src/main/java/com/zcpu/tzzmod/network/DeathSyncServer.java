package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DeathSyncServer {
    private static final Map<UUID, Boolean> lastStatus = new ConcurrentHashMap<>();

    private DeathSyncServer() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            boolean hasDeath = player.getCommandTags().contains("death");
            lastStatus.put(player.getUuid(), hasDeath);
            sendStatus(player, hasDeath);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                lastStatus.remove(handler.getPlayer().getUuid())
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                boolean hasDeath = player.getCommandTags().contains("death");
                UUID id = player.getUuid();
                Boolean previous = lastStatus.put(id, hasDeath);
                if (previous == null || previous != hasDeath) {
                    sendStatus(player, hasDeath);
                }
            }
        });
    }

    private static void sendStatus(ServerPlayerEntity player, boolean hasDeath) {
        ServerPlayNetworking.send(player, new DeathStatusPayload(hasDeath));
    }
}
