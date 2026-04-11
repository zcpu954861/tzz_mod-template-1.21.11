package com.zcpu.tzzmod.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zcpu.tzzmod.Tzz_mod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class MapDataStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<MinecraftServer, MapState> CACHE = new WeakHashMap<>();

    private MapDataStore() {
    }

    public static synchronized MapSnapshot getSnapshot(MinecraftServer server) {
        return getState(server).toSnapshot();
    }

    public static synchronized boolean setRegion(MinecraftServer server, String dimensionId, int x1, int y1, int z1, int x2, int y2, int z2) {
        MapState state = getState(server);
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);
        state.region = new MapRegionData(safeDimensionId(dimensionId), minX, minY, minZ, maxX, maxY, maxZ);
        state.regionVersion++;
        state.markers.removeIf(marker -> !state.region.contains(marker.x, marker.y, marker.z) || !state.region.dimensionId.equals(marker.dimensionId));
        state.writeToDisk();
        return true;
    }

    public static synchronized AddMarkerResult addMarker(MinecraftServer server, ServerPlayerEntity player, BlockPos pos) {
        MapState state = getState(server);
        if (state.region == null) {
            return new AddMarkerResult(AddMarkerStatus.NO_REGION, null);
        }

        String dimensionId = player.getCommandSource().getWorld().getRegistryKey().getValue().toString();
        if (!state.region.dimensionId.equals(dimensionId)) {
            return new AddMarkerResult(AddMarkerStatus.WRONG_DIMENSION, null);
        }

        if (!state.region.contains(pos.getX(), pos.getY(), pos.getZ())) {
            return new AddMarkerResult(AddMarkerStatus.OUTSIDE_REGION, null);
        }

        String cleanName = state.nextDefaultMarkerName();
        MapMarkerData marker = new MapMarkerData(
                UUID.randomUUID().toString(),
                cleanName,
                dimensionId,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                MapColors.paletteColor(state.markers.size())
        );
        state.markers.add(marker);
        state.writeToDisk();
        return new AddMarkerResult(AddMarkerStatus.OK, marker);
    }

    public static synchronized boolean deleteMarker(MinecraftServer server, String markerId) {
        MapState state = getState(server);
        boolean removed = state.markers.removeIf(marker -> marker.id.equals(markerId));
        if (removed) {
            state.writeToDisk();
        }
        return removed;
    }

    public static synchronized boolean setMarkerColor(MinecraftServer server, String markerId, int color) {
        MapState state = getState(server);
        for (int i = 0; i < state.markers.size(); i++) {
            MapMarkerData marker = state.markers.get(i);
            if (!marker.id.equals(markerId)) {
                continue;
            }
            state.markers.set(i, new MapMarkerData(marker.id, marker.name, marker.dimensionId, marker.x, marker.y, marker.z, color));
            state.writeToDisk();
            return true;
        }
        return false;
    }

    public static synchronized boolean setMarkerName(MinecraftServer server, String markerId, String name) {
        MapState state = getState(server);
        for (int i = 0; i < state.markers.size(); i++) {
            MapMarkerData marker = state.markers.get(i);
            if (!marker.id.equals(markerId)) {
                continue;
            }
            String cleanName = sanitizeMarkerName(name, marker.name);
            state.markers.set(i, new MapMarkerData(marker.id, cleanName, marker.dimensionId, marker.x, marker.y, marker.z, marker.color));
            state.writeToDisk();
            return true;
        }
        return false;
    }

    public static synchronized MapMarkerData getMarker(MinecraftServer server, String markerId) {
        MapState state = getState(server);
        for (MapMarkerData marker : state.markers) {
            if (marker.id.equals(markerId)) {
                return marker;
            }
        }
        return null;
    }

    public static synchronized boolean setVisibility(MinecraftServer server, String key, boolean enabled) {
        MapState state = getState(server);
        boolean changed = switch (key) {
            case "show_self_position" -> {
                boolean previous = state.showSelfPosition;
                state.showSelfPosition = enabled;
                yield previous != enabled;
            }
            case "show_markers" -> {
                boolean previous = state.showMarkers;
                state.showMarkers = enabled;
                yield previous != enabled;
            }
            case "show_other_players" -> {
                boolean previous = state.showOtherPlayers;
                state.showOtherPlayers = enabled;
                yield previous != enabled;
            }
            default -> false;
        };
        if (changed) {
            state.writeToDisk();
        }
        return changed;
    }

    public static Text describeAddMarkerFailure(AddMarkerStatus status) {
        return switch (status) {
            case NO_REGION -> Text.literal("地图区域尚未设置，请先使用 /map set xyz 设置区域。");
            case WRONG_DIMENSION -> Text.literal("当前维度与地图区域不一致，无法标点。");
            case OUTSIDE_REGION -> Text.literal("目标方块不在地图区域内，无法标点。");
            case OK -> Text.empty();
        };
    }

    private static MapState getState(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, MapDataStore::load);
    }

    private static MapState load(MinecraftServer server) {
        Path path = server.getSavePath(WorldSavePath.ROOT).resolve("tzz_mod").resolve("map_state.json");
        MapState state = new MapState(path);
        try {
            Files.createDirectories(path.getParent());
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    PersistedState persisted = GSON.fromJson(reader, PersistedState.class);
                    state.apply(persisted);
                    return state;
                }
            }
            state.writeToDisk();
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load map state: {}", exception.getMessage());
        }
        return state;
    }

    private static String safeDimensionId(String dimensionId) {
        if (dimensionId == null || dimensionId.isBlank()) {
            return "minecraft:overworld";
        }
        return dimensionId.trim();
    }

    private static String sanitizeMarkerName(String name, String fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        String clean = name.trim();
        if (clean.length() > 64) {
            return clean.substring(0, 64);
        }
        return clean;
    }

    public enum AddMarkerStatus {
        OK,
        NO_REGION,
        WRONG_DIMENSION,
        OUTSIDE_REGION
    }

    public record AddMarkerResult(AddMarkerStatus status, MapMarkerData marker) {
    }

    public record MapSnapshot(MapRegionData region, MapVisibilitySettings settings, List<MapMarkerData> markers, int regionVersion) {
    }

    public record MapRegionData(String dimensionId, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }

        public boolean containsHorizontal(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        public int width() {
            return maxX - minX + 1;
        }

        public int depth() {
            return maxZ - minZ + 1;
        }
    }

    public record MapMarkerData(String id, String name, String dimensionId, int x, int y, int z, int color) {
    }

    public record MapVisibilitySettings(boolean showSelfPosition, boolean showMarkers, boolean showOtherPlayers) {
    }

    private static final class MapState {
        private final Path path;
        private MapRegionData region;
        private final List<MapMarkerData> markers = new ArrayList<>();
        private boolean showSelfPosition = true;
        private boolean showMarkers = true;
        private boolean showOtherPlayers = true;
        private int regionVersion = 0;

        private MapState(Path path) {
            this.path = path;
        }

        private void apply(PersistedState persisted) {
            if (persisted == null) {
                return;
            }
            if (persisted.region != null) {
                region = new MapRegionData(
                        safeDimensionId(persisted.region.dimensionId),
                        Math.min(persisted.region.x1, persisted.region.x2),
                        Math.min(persisted.region.y1, persisted.region.y2),
                        Math.min(persisted.region.z1, persisted.region.z2),
                        Math.max(persisted.region.x1, persisted.region.x2),
                        Math.max(persisted.region.y1, persisted.region.y2),
                        Math.max(persisted.region.z1, persisted.region.z2)
                );
            }

            markers.clear();
            if (persisted.markers != null) {
                for (PersistedMarker persistedMarker : persisted.markers) {
                    if (persistedMarker == null || persistedMarker.id == null || persistedMarker.id.isBlank()) {
                        continue;
                    }
                    String fallbackName = "地图标点" + (markers.size() + 1);
                    markers.add(new MapMarkerData(
                            persistedMarker.id,
                            sanitizeMarkerName(persistedMarker.name, fallbackName),
                            safeDimensionId(persistedMarker.dimensionId),
                            persistedMarker.x,
                            persistedMarker.y,
                            persistedMarker.z,
                            persistedMarker.color == 0 ? MapColors.paletteColor(markers.size()) : persistedMarker.color
                    ));
                }
            }

            showSelfPosition = persisted.showSelfPosition;
            showMarkers = persisted.showMarkers;
            showOtherPlayers = persisted.showOtherPlayers;
            regionVersion = Math.max(0, persisted.regionVersion);
        }

        private MapSnapshot toSnapshot() {
            return new MapSnapshot(
                    region,
                    new MapVisibilitySettings(showSelfPosition, showMarkers, showOtherPlayers),
                    List.copyOf(markers),
                    regionVersion
            );
        }

        private String nextDefaultMarkerName() {
            int index = 1;
            while (true) {
                String candidate = "地图标点" + index;
                boolean exists = false;
                for (MapMarkerData marker : markers) {
                    if (candidate.equals(marker.name)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    return candidate;
                }
                index++;
            }
        }

        private void writeToDisk() {
            try {
                Files.createDirectories(path.getParent());
                PersistedState persisted = new PersistedState();
                if (region != null) {
                    persisted.region = new PersistedRegion();
                    persisted.region.dimensionId = region.dimensionId;
                    persisted.region.x1 = region.minX;
                    persisted.region.y1 = region.minY;
                    persisted.region.z1 = region.minZ;
                    persisted.region.x2 = region.maxX;
                    persisted.region.y2 = region.maxY;
                    persisted.region.z2 = region.maxZ;
                }
                persisted.showSelfPosition = showSelfPosition;
                persisted.showMarkers = showMarkers;
                persisted.showOtherPlayers = showOtherPlayers;
                persisted.regionVersion = regionVersion;
                persisted.markers = new ArrayList<>();
                for (MapMarkerData marker : markers) {
                    PersistedMarker persistedMarker = new PersistedMarker();
                    persistedMarker.id = marker.id;
                    persistedMarker.name = marker.name;
                    persistedMarker.dimensionId = marker.dimensionId;
                    persistedMarker.x = marker.x;
                    persistedMarker.y = marker.y;
                    persistedMarker.z = marker.z;
                    persistedMarker.color = marker.color;
                    persisted.markers.add(persistedMarker);
                }
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    GSON.toJson(persisted, writer);
                }
            } catch (Exception exception) {
                Tzz_mod.LOGGER.warn("Failed to write map state: {}", exception.getMessage());
            }
        }
    }

    private static final class PersistedState {
        private PersistedRegion region;
        private List<PersistedMarker> markers = new ArrayList<>();
        private boolean showSelfPosition = true;
        private boolean showMarkers = true;
        private boolean showOtherPlayers = true;
        private int regionVersion = 0;
    }

    private static final class PersistedRegion {
        private String dimensionId = "minecraft:overworld";
        private int x1;
        private int y1;
        private int z1;
        private int x2;
        private int y2;
        private int z2;
    }

    private static final class PersistedMarker {
        private String id = "";
        private String name = "Marker";
        private String dimensionId = "minecraft:overworld";
        private int x;
        private int y;
        private int z;
        private int color;
    }
}