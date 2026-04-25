package com.zcpu.tzzmod.region;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public final class RegionControllerServer {
    private static long tickCounter = 0L;

    private RegionControllerServer() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % 10L != 0L) {
                return;
            }
            RegionControllerTracker.tick(server, tickCounter);
        });
    }

    public static void clearServerState() {
        tickCounter = 0L;
        RegionControllerTracker.clearServerState();
    }

    public static long getTickCounter() {
        return tickCounter;
    }
}
