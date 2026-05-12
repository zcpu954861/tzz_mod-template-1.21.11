package com.zcpu.tzzmod.core.bootstrap;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.config.PhotoSpeedConfig;
import com.zcpu.tzzmod.map.MapDataStore;
import com.zcpu.tzzmod.map.MapServer;
import com.zcpu.tzzmod.note.NoteDataStore;
import com.zcpu.tzzmod.phone.PhoneAppsConfig;
import com.zcpu.tzzmod.region.RegionControllerServer;
import com.zcpu.tzzmod.region.RegionControllerStore;
import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalListenerStore;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceContainerHandler;
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceDispatcher;
import com.zcpu.tzzmod.task.TaskDataStore;
import com.zcpu.tzzmod.webadmin.WebAdminLifecycle;
import com.zcpu.tzzmod.webadmin.container.WebAdminContainerTemplateSessions;
import com.zcpu.tzzmod.webadmin.itemsubmit.WebAdminSingleItemSubmitTemplateSessions;
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionSessions;
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
                WebAdminLifecycle.start(server);
            } catch (Throwable throwable) {
                Tzz_mod.LOGGER.warn("Failed to initialize startup configs: {}", throwable.getMessage());
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            VirtualBlockDeviceDispatcher.tick(server);
            VirtualBlockDeviceContainerHandler.tick(server);
            MapDataStore.flushDirty(server);
            TaskDataStore.flushDirty(server);
            NoteDataStore.flushDirty(server);
            RegionControllerStore.flushDirty(server);
            SignalListenerStore.flushDirty(server);
            SignalDeviceStore.flushDirty(server);
            WebAdminContainerTemplateSessions.expireOld(server);
            WebAdminSingleItemSubmitTemplateSessions.expireOld(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            WebAdminSelectionSessions.clearAll(server, "server_stopping");
            WebAdminContainerTemplateSessions.clearAll(server, "server_stopping");
            WebAdminSingleItemSubmitTemplateSessions.clearAll(server, "server_stopping");
            MapDataStore.flushDirty(server);
            TaskDataStore.flushDirty(server);
            NoteDataStore.flushDirty(server);
            RegionControllerStore.flushDirty(server);
            SignalListenerStore.flushDirty(server);
            SignalDeviceStore.forceFlushDirty(server);
            WebAdminLifecycle.stop();
            MapServer.clearServerState();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            WebAdminSelectionSessions.clearAll(server, "server_stopped");
            WebAdminContainerTemplateSessions.clearAll(server, "server_stopped");
            WebAdminSingleItemSubmitTemplateSessions.clearAll(server, "server_stopped");
            WebAdminLifecycle.stop();
            SignalDeviceStore.forceFlushDirty(server);
            MapDataStore.clearCache(server);
            TaskDataStore.clearCache(server);
            NoteDataStore.clearCache(server);
            RegionControllerStore.clearCache(server);
            SignalListenerStore.clearCache(server);
            SignalDeviceStore.clearCache(server);
            MapServer.clearServerState();
            RegionControllerServer.clearServerState();
            SignalBridgeServer.clearServerState();
            PhotoSpeedConfig.clearCache(server);
        });
    }
}
