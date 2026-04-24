package com.zcpu.tzzmod.map;

import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class MapDataStore {
    private static final Map<MinecraftServer, MapState> CACHE = new WeakHashMap<>();

    private MapDataStore() {
    }

    public static synchronized MapSnapshot getSnapshot(MinecraftServer server) {
        return getState(server).toSnapshot();
    }

    public static synchronized void flushDirty(MinecraftServer server) {
        MapState state = CACHE.get(server);
        if (state != null) {
            state.flushDirty();
        }
    }

    public static synchronized void clearCache(MinecraftServer server) {
        CACHE.remove(server);
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
        state.markers.removeIf(marker -> !state.region.containsHorizontal(marker.x, marker.z) || !state.region.dimensionId.equals(marker.dimensionId));
        state.markDirty();
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

        if (!state.region.containsHorizontal(pos.getX(), pos.getZ())) {
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
        state.markDirty();
        return new AddMarkerResult(AddMarkerStatus.OK, marker);
    }

    public static synchronized boolean deleteMarker(MinecraftServer server, String markerId) {
        MapState state = getState(server);
        boolean removed = state.markers.removeIf(marker -> marker.id.equals(markerId));
        if (removed) {
            state.markDirty();
        }
        return removed;
    }

    public static synchronized PlannerRegionResult addPlannerRegion(MinecraftServer server, String dimensionId, List<RegionGeometry.Point> points, int color) {
        MapState state = getState(server);
        String cleanDimensionId = safeDimensionId(dimensionId);
        List<RegionGeometry.Point> normalized = normalizePolygon(points);
        if (normalized.size() < 3) {
            return new PlannerRegionResult(PlannerRegionStatus.TOO_FEW_POINTS, null);
        }
        if (!RegionGeometry.isSimplePolygon(normalized)) {
            return new PlannerRegionResult(PlannerRegionStatus.INVALID_SHAPE, null);
        }
        if (state.overlapsPlannerRegion(cleanDimensionId, normalized, null)) {
            return new PlannerRegionResult(PlannerRegionStatus.OVERLAP, null);
        }

        PlannerRegionData region = new PlannerRegionData(
                UUID.randomUUID().toString(),
                state.nextDefaultRegionName(),
                cleanDimensionId,
                List.copyOf(normalized),
                sanitizeRegionColor(color, state.plannerRegions.size())
        );
        state.plannerRegions.add(region);
        state.sortPlannerRegions();
        state.markDirty();
        return new PlannerRegionResult(PlannerRegionStatus.OK, region);
    }

    public static synchronized PlannerRegionResult renamePlannerRegion(MinecraftServer server, String regionId, String name) {
        MapState state = getState(server);
        for (int index = 0; index < state.plannerRegions.size(); index++) {
            PlannerRegionData region = state.plannerRegions.get(index);
            if (!region.id().equals(regionId)) {
                continue;
            }
            String cleanName = sanitizeRegionName(name, "");
            if (cleanName.isBlank()) {
                return new PlannerRegionResult(PlannerRegionStatus.INVALID_NAME, null);
            }
            if (state.hasPlannerRegionName(cleanName, regionId)) {
                return new PlannerRegionResult(PlannerRegionStatus.DUPLICATE_NAME, null);
            }
            PlannerRegionData updated = new PlannerRegionData(region.id(), cleanName, region.dimensionId(), region.points(), region.color());
            state.plannerRegions.set(index, updated);
            state.sortPlannerRegions();
            state.markDirty();
            return new PlannerRegionResult(PlannerRegionStatus.OK, updated);
        }
        return new PlannerRegionResult(PlannerRegionStatus.NOT_FOUND, null);
    }

    public static synchronized PlannerRegionResult setPlannerRegionColor(MinecraftServer server, String regionId, int color) {
        MapState state = getState(server);
        for (int index = 0; index < state.plannerRegions.size(); index++) {
            PlannerRegionData region = state.plannerRegions.get(index);
            if (!region.id().equals(regionId)) {
                continue;
            }
            PlannerRegionData updated = new PlannerRegionData(
                    region.id(),
                    region.name(),
                    region.dimensionId(),
                    region.points(),
                    sanitizeRegionColor(color, index)
            );
            state.plannerRegions.set(index, updated);
            state.sortPlannerRegions();
            state.markDirty();
            return new PlannerRegionResult(PlannerRegionStatus.OK, updated);
        }
        return new PlannerRegionResult(PlannerRegionStatus.NOT_FOUND, null);
    }

    public static synchronized boolean deletePlannerRegion(MinecraftServer server, String regionId) {
        MapState state = getState(server);
        boolean removed = state.plannerRegions.removeIf(region -> region.id().equals(regionId));
        if (removed) {
            state.markDirty();
        }
        return removed;
    }

    public static synchronized PlannerRegionData getPlannerRegion(MinecraftServer server, String regionId) {
        MapState state = getState(server);
        for (PlannerRegionData region : state.plannerRegions) {
            if (region.id().equals(regionId)) {
                return region;
            }
        }
        return null;
    }

    public static synchronized PlannerRegionData findPlannerRegionContaining(MinecraftServer server, String dimensionId, double x, double z) {
        MapState state = getState(server);
        String cleanDimensionId = safeDimensionId(dimensionId);
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        for (PlannerRegionData region : state.plannerRegions) {
            if (!cleanDimensionId.equals(region.dimensionId())) {
                continue;
            }
            if (region.containsBlock(blockX, blockZ)) {
                return region;
            }
        }
        return null;
    }

    public static synchronized boolean setMarkerColor(MinecraftServer server, String markerId, int color) {
        MapState state = getState(server);
        for (int i = 0; i < state.markers.size(); i++) {
            MapMarkerData marker = state.markers.get(i);
            if (!marker.id.equals(markerId)) {
                continue;
            }
            state.markers.set(i, new MapMarkerData(marker.id, marker.name, marker.dimensionId, marker.x, marker.y, marker.z, color));
            state.markDirty();
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
            state.markDirty();
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
            case "show_region_titles" -> {
                boolean previous = state.showRegionTitles;
                state.showRegionTitles = enabled;
                yield previous != enabled;
            }
            default -> false;
        };
        if (changed) {
            state.markDirty();
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
        PersistedState persisted = JsonStoreSupport.readOrDefault(path, PersistedState.class, PersistedState::new, "map state");
        state.apply(persisted);
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

    private static String sanitizeRegionName(String name, String fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        String clean = name.trim();
        if (clean.length() > 48) {
            return clean.substring(0, 48);
        }
        return clean;
    }

    private static int sanitizeRegionColor(int color, int fallbackIndex) {
        int rgb = color & 0xFFFFFF;
        if (rgb == 0) {
            rgb = MapColors.paletteColor(fallbackIndex) & 0xFFFFFF;
        }
        return 0xFF000000 | rgb;
    }

    private static List<RegionGeometry.Point> normalizePolygon(List<RegionGeometry.Point> points) {
        List<RegionGeometry.Point> normalized = new ArrayList<>();
        if (points == null) {
            return normalized;
        }

        RegionGeometry.Point previous = null;
        for (RegionGeometry.Point point : points) {
            if (point == null) {
                continue;
            }
            RegionGeometry.Point cleanPoint = new RegionGeometry.Point(point.x(), point.z());
            if (previous != null && previous.x() == cleanPoint.x() && previous.z() == cleanPoint.z()) {
                continue;
            }
            normalized.add(cleanPoint);
            previous = cleanPoint;
        }

        if (normalized.size() > 1) {
            RegionGeometry.Point first = normalized.get(0);
            RegionGeometry.Point last = normalized.get(normalized.size() - 1);
            if (first.x() == last.x() && first.z() == last.z()) {
                normalized.remove(normalized.size() - 1);
            }
        }
        return normalized;
    }

    public enum AddMarkerStatus {
        OK,
        NO_REGION,
        WRONG_DIMENSION,
        OUTSIDE_REGION
    }

    public record AddMarkerResult(AddMarkerStatus status, MapMarkerData marker) {
    }

    public enum PlannerRegionStatus {
        OK,
        TOO_FEW_POINTS,
        INVALID_SHAPE,
        OVERLAP,
        DUPLICATE_NAME,
        INVALID_NAME,
        NOT_FOUND
    }

    public record PlannerRegionResult(PlannerRegionStatus status, PlannerRegionData region) {
    }

    public record MapSnapshot(MapRegionData region, MapVisibilitySettings settings, List<MapMarkerData> markers, List<PlannerRegionData> plannerRegions, int regionVersion) {
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

    public record PlannerRegionData(String id, String name, String dimensionId, List<RegionGeometry.Point> points, int color) {
        public RegionGeometry.Bounds bounds() {
            return RegionGeometry.bounds(points);
        }

        public boolean containsBlock(int blockX, int blockZ) {
            return RegionGeometry.containsBlock(points, blockX, blockZ);
        }
    }

    public record MapVisibilitySettings(boolean showSelfPosition, boolean showMarkers, boolean showOtherPlayers, boolean showRegionTitles) {
    }

    private static final class MapState {
        private final Path path;
        private MapRegionData region;
        private final List<MapMarkerData> markers = new ArrayList<>();
        private final List<PlannerRegionData> plannerRegions = new ArrayList<>();
        private boolean showSelfPosition = true;
        private boolean showMarkers = true;
        private boolean showOtherPlayers = true;
        private boolean showRegionTitles = true;
        private int regionVersion = 0;
        private boolean dirty;

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

            plannerRegions.clear();
            if (persisted.plannerRegions != null) {
                for (PersistedPlannerRegion persistedRegion : persisted.plannerRegions) {
                    if (persistedRegion == null || persistedRegion.id == null || persistedRegion.id.isBlank()) {
                        continue;
                    }
                    List<RegionGeometry.Point> points = new ArrayList<>();
                    if (persistedRegion.points != null) {
                        for (PersistedRegionPoint persistedPoint : persistedRegion.points) {
                            if (persistedPoint == null) {
                                continue;
                            }
                            points.add(new RegionGeometry.Point(persistedPoint.x, persistedPoint.z));
                        }
                    }
                    List<RegionGeometry.Point> normalized = normalizePolygon(points);
                    if (normalized.size() < 3 || !RegionGeometry.isSimplePolygon(normalized)) {
                        continue;
                    }
                    String fallbackName = "区域" + (plannerRegions.size() + 1);
                    plannerRegions.add(new PlannerRegionData(
                            persistedRegion.id,
                            sanitizeRegionName(persistedRegion.name, fallbackName),
                            safeDimensionId(persistedRegion.dimensionId),
                            List.copyOf(normalized),
                            sanitizeRegionColor(persistedRegion.color, plannerRegions.size())
                    ));
                }
                sortPlannerRegions();
            }

            showSelfPosition = persisted.showSelfPosition;
            showMarkers = persisted.showMarkers;
            showOtherPlayers = persisted.showOtherPlayers;
            showRegionTitles = persisted.showRegionTitles;
            regionVersion = Math.max(0, persisted.regionVersion);
        }

        private MapSnapshot toSnapshot() {
            return new MapSnapshot(
                    region,
                    new MapVisibilitySettings(showSelfPosition, showMarkers, showOtherPlayers, showRegionTitles),
                    List.copyOf(markers),
                    List.copyOf(plannerRegions),
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

        private String nextDefaultRegionName() {
            int index = 1;
            while (true) {
                String candidate = "区域" + index;
                if (!hasPlannerRegionName(candidate, null)) {
                    return candidate;
                }
                index++;
            }
        }

        private boolean hasPlannerRegionName(String candidate, String ignoreRegionId) {
            for (PlannerRegionData region : plannerRegions) {
                if (ignoreRegionId != null && ignoreRegionId.equals(region.id())) {
                    continue;
                }
                if (region.name().equalsIgnoreCase(candidate)) {
                    return true;
                }
            }
            return false;
        }

        private boolean overlapsPlannerRegion(String dimensionId, List<RegionGeometry.Point> points, String ignoreRegionId) {
            for (PlannerRegionData region : plannerRegions) {
                if (ignoreRegionId != null && ignoreRegionId.equals(region.id())) {
                    continue;
                }
                if (!dimensionId.equals(region.dimensionId())) {
                    continue;
                }
                if (RegionGeometry.polygonsOverlap(region.points(), points)) {
                    return true;
                }
            }
            return false;
        }

        private void sortPlannerRegions() {
            plannerRegions.sort(Comparator.comparing(PlannerRegionData::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(PlannerRegionData::id));
        }

        private void markDirty() {
            dirty = true;
        }

        private void flushDirty() {
            if (!dirty) {
                return;
            }
            if (writeToDisk()) {
                dirty = false;
            }
        }

        private boolean writeToDisk() {
            return JsonStoreSupport.write(path, toPersistedState(), "map state");
        }

        private PersistedState toPersistedState() {
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
            persisted.showRegionTitles = showRegionTitles;
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

            persisted.plannerRegions = new ArrayList<>();
            for (PlannerRegionData regionData : plannerRegions) {
                PersistedPlannerRegion persistedRegion = new PersistedPlannerRegion();
                persistedRegion.id = regionData.id();
                persistedRegion.name = regionData.name();
                persistedRegion.dimensionId = regionData.dimensionId();
                persistedRegion.color = regionData.color();
                persistedRegion.points = new ArrayList<>();
                for (RegionGeometry.Point point : regionData.points()) {
                    PersistedRegionPoint persistedPoint = new PersistedRegionPoint();
                    persistedPoint.x = point.x();
                    persistedPoint.z = point.z();
                    persistedRegion.points.add(persistedPoint);
                }
                persisted.plannerRegions.add(persistedRegion);
            }

            return persisted;
        }
    }

    private static final class PersistedState {
        private PersistedRegion region;
        private List<PersistedMarker> markers = new ArrayList<>();
        private List<PersistedPlannerRegion> plannerRegions = new ArrayList<>();
        private boolean showSelfPosition = true;
        private boolean showMarkers = true;
        private boolean showOtherPlayers = true;
        private boolean showRegionTitles = true;
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

    private static final class PersistedPlannerRegion {
        private String id = "";
        private String name = "区域";
        private String dimensionId = "minecraft:overworld";
        private int color = 0;
        private List<PersistedRegionPoint> points = new ArrayList<>();
    }

    private static final class PersistedRegionPoint {
        private int x;
        private int z;
    }
}
