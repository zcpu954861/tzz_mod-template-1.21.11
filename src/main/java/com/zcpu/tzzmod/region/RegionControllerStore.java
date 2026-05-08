package com.zcpu.tzzmod.region;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

public final class RegionControllerStore {
    private static final Map<MinecraftServer, State> CACHE = new WeakHashMap<>();

    private RegionControllerStore() {
    }

    public static synchronized List<RegionControllerData> getSnapshot(MinecraftServer server) {
        return List.copyOf(getState(server).controllers);
    }

    public static synchronized RegionControllerData getController(MinecraftServer server, String controllerId) {
        if (controllerId == null || controllerId.isBlank()) {
            return null;
        }
        for (RegionControllerData controller : getState(server).controllers) {
            if (controller.id().equals(controllerId)) {
                return controller;
            }
        }
        return null;
    }

    public static synchronized List<RegionControllerData> getEnabledControllers(MinecraftServer server) {
        List<RegionControllerData> result = new ArrayList<>();
        for (RegionControllerData controller : getState(server).controllers) {
            if (controller.enabled()) {
                result.add(controller);
            }
        }
        return List.copyOf(result);
    }

    public static synchronized RegionControllerData createController(MinecraftServer server, String regionId, String name) {
        State state = getState(server);
        RegionControllerData controller = new RegionControllerData(
                UUID.randomUUID().toString(),
                name == null ? "" : name.trim(),
                regionId == null ? "" : regionId.trim(),
                true,
                RegionTargetFilter.all(),
                RegionControllerData.DEFAULT_STAY_INTERVAL_TICKS,
                List.of(),
                List.of(),
                List.of()
        ).normalized();
        state.controllers.add(controller);
        state.markDirty();
        WebAdminRealtimeEventBus.publishRegionControllerEvent(
                WebAdminRealtimeEventType.REGION_CONTROLLER_CHANGED,
                controller,
                "区域控制器已创建：" + displayName(controller)
        );
        return controller;
    }

    public static synchronized boolean deleteController(MinecraftServer server, String controllerId) {
        State state = getState(server);
        RegionControllerData removedController = null;
        for (RegionControllerData controller : state.controllers) {
            if (controller.id().equals(controllerId)) {
                removedController = controller;
                break;
            }
        }
        boolean removed = state.controllers.removeIf(controller -> controller.id().equals(controllerId));
        if (removed) {
            state.markDirty();
            WebAdminRealtimeEventBus.publishRegionControllerEvent(
                    WebAdminRealtimeEventType.REGION_CONTROLLER_CHANGED,
                    removedController,
                    "区域控制器已删除：" + displayName(removedController)
            );
        }
        return removed;
    }

    public static synchronized boolean setEnabled(MinecraftServer server, String controllerId, boolean enabled) {
        return replace(server, controllerId, controller -> new RegionControllerData(
                controller.id(),
                controller.name(),
                controller.regionId(),
                enabled,
                controller.targetFilter(),
                controller.stayIntervalTicks(),
                controller.enterActions(),
                controller.exitActions(),
                controller.stayActions()
        ).normalized());
    }

    public static synchronized boolean setTargetFilter(MinecraftServer server, String controllerId, RegionTargetFilter filter) {
        return replace(server, controllerId, controller -> new RegionControllerData(
                controller.id(),
                controller.name(),
                controller.regionId(),
                controller.enabled(),
                filter == null ? RegionTargetFilter.all() : filter,
                controller.stayIntervalTicks(),
                controller.enterActions(),
                controller.exitActions(),
                controller.stayActions()
        ).normalized());
    }

    public static synchronized boolean setStayInterval(MinecraftServer server, String controllerId, int ticks) {
        return replace(server, controllerId, controller -> new RegionControllerData(
                controller.id(),
                controller.name(),
                controller.regionId(),
                controller.enabled(),
                controller.targetFilter(),
                ticks,
                controller.enterActions(),
                controller.exitActions(),
                controller.stayActions()
        ).normalized());
    }

    public static synchronized boolean addAction(MinecraftServer server, String controllerId, RegionTriggerType triggerType, ActionConfig action) {
        if (triggerType == null || action == null) {
            return false;
        }
        return replace(server, controllerId, controller -> {
            List<ActionConfig> enterActions = controller.enterActions();
            List<ActionConfig> exitActions = controller.exitActions();
            List<ActionConfig> stayActions = controller.stayActions();
            switch (triggerType) {
                case ENTER -> enterActions = appendAction(enterActions, action);
                case EXIT -> exitActions = appendAction(exitActions, action);
                case STAY -> stayActions = appendAction(stayActions, action);
            }
            return new RegionControllerData(
                    controller.id(),
                    controller.name(),
                    controller.regionId(),
                    controller.enabled(),
                    controller.targetFilter(),
                    controller.stayIntervalTicks(),
                    enterActions,
                    exitActions,
                    stayActions
            ).normalized();
        });
    }

    public static synchronized boolean clearActions(MinecraftServer server, String controllerId, RegionTriggerType triggerType) {
        if (triggerType == null) {
            return false;
        }
        return replace(server, controllerId, controller -> new RegionControllerData(
                controller.id(),
                controller.name(),
                controller.regionId(),
                controller.enabled(),
                controller.targetFilter(),
                controller.stayIntervalTicks(),
                triggerType == RegionTriggerType.ENTER ? List.of() : controller.enterActions(),
                triggerType == RegionTriggerType.EXIT ? List.of() : controller.exitActions(),
                triggerType == RegionTriggerType.STAY ? List.of() : controller.stayActions()
        ).normalized());
    }

    public static synchronized void flushDirty(MinecraftServer server) {
        State state = CACHE.get(server);
        if (state != null) {
            state.flushDirty();
        }
    }

    public static synchronized void clearCache(MinecraftServer server) {
        CACHE.remove(server);
    }

    private static List<ActionConfig> appendAction(List<ActionConfig> actions, ActionConfig action) {
        List<ActionConfig> copy = new ArrayList<>(actions == null ? List.of() : actions);
        copy.add(action);
        return List.copyOf(copy);
    }

    private static boolean replace(MinecraftServer server, String controllerId, java.util.function.Function<RegionControllerData, RegionControllerData> updater) {
        if (controllerId == null || controllerId.isBlank()) {
            return false;
        }
        State state = getState(server);
        for (int i = 0; i < state.controllers.size(); i++) {
            RegionControllerData controller = state.controllers.get(i);
            if (!controller.id().equals(controllerId)) {
                continue;
            }
            RegionControllerData updated = updater.apply(controller).normalized();
            state.controllers.set(i, updated);
            state.markDirty();
            WebAdminRealtimeEventBus.publishRegionControllerEvent(
                    WebAdminRealtimeEventType.REGION_CONTROLLER_CHANGED,
                    updated,
                    "区域控制器已变化：" + displayName(updated)
            );
            return true;
        }
        return false;
    }

    private static String displayName(RegionControllerData controller) {
        if (controller == null) {
            return "unknown";
        }
        return controller.name() == null || controller.name().isBlank() ? controller.id() : controller.name();
    }

    private static State getState(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, RegionControllerStore::load);
    }

    private static State load(MinecraftServer server) {
        Path path = server.getSavePath(WorldSavePath.ROOT)
                .resolve("tzz_mod")
                .resolve("region_controllers.json");
        State state = new State(path);
        DataFile dataFile = JsonStoreSupport.readOrDefault(path, DataFile.class, DataFile::new, "region controllers");
        if (dataFile.controllers != null) {
            for (RegionControllerData controller : dataFile.controllers) {
                if (controller == null) {
                    continue;
                }
                RegionControllerData normalized = controller.normalized();
                if (!normalized.id().isBlank()) {
                    state.controllers.add(normalized);
                }
            }
        }
        return state;
    }

    public static final class DataFile {
        public int version = 1;
        public List<RegionControllerData> controllers = new ArrayList<>();
    }

    private static final class State {
        private final Path path;
        private final List<RegionControllerData> controllers = new ArrayList<>();
        private boolean dirty;

        private State(Path path) {
            this.path = path;
        }

        private void markDirty() {
            dirty = true;
        }

        private void flushDirty() {
            if (!dirty) {
                return;
            }
            DataFile dataFile = new DataFile();
            dataFile.controllers = new ArrayList<>(controllers.size());
            for (RegionControllerData controller : controllers) {
                dataFile.controllers.add(controller.normalized());
            }
            if (JsonStoreSupport.write(path, dataFile, "region controllers")) {
                dirty = false;
            }
        }
    }
}
