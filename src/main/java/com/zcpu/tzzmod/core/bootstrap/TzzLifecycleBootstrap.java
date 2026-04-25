package com.zcpu.tzzmod.core.bootstrap;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.config.PhotoSpeedConfig;
import com.zcpu.tzzmod.map.MapDataStore;
import com.zcpu.tzzmod.map.MapServer;
import com.zcpu.tzzmod.note.NoteDataStore;
import com.zcpu.tzzmod.phone.PhoneAppsConfig;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.task.TaskDataStore;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class TzzLifecycleBootstrap {
    private TzzLifecycleBootstrap() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                PhoneAppsConfig.get(server);
                PhotoSpeedConfig.get(server);
            } catch (Throwable throwable) {
                Tzz_mod.LOGGER.warn("Failed to initialize startup configs: {}", throwable.getMessage());
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            MapDataStore.flushDirty(server);
            TaskDataStore.flushDirty(server);
            NoteDataStore.flushDirty(server);
            RegionControllerStore.flushDirty(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            MapDataStore.flushDirty(server);
            TaskDataStore.flushDirty(server);
            NoteDataStore.flushDirty(server);
            RegionControllerStore.flushDirty(server);
            MapServer.clearServerState();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            MapDataStore.clearCache(server);
            TaskDataStore.clearCache(server);
            NoteDataStore.clearCache(server);
            RegionControllerStore.clearCache(server);
            MapServer.clearServerState();
            PhotoSpeedConfig.clearCache(server);
        });
    }
}
