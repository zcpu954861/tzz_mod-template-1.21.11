package com.zcpu.tzzmod.client.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.ModItem.ModItems;
import com.zcpu.tzzmod.client.phone.ui.RegionTitleOverlay;
import com.zcpu.tzzmod.map.RegionGeometry;
import com.zcpu.tzzmod.network.MapC2SPayload;
import com.zcpu.tzzmod.network.MapS2CPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class MapClient {
    private static final double PARTICLE_RANGE_SQ = 196.0D * 196.0D;
    private static final Set<Runnable> LISTENERS = new CopyOnWriteArraySet<>();
    private static final Set<String> VISIBLE_REGION_IDS = new CopyOnWriteArraySet<>();
    private static final Set<String> HIDDEN_MARKER_IDS = new CopyOnWriteArraySet<>();
    private static final Set<String> PARTICLE_DISABLED_MARKER_IDS = new CopyOnWriteArraySet<>();
    private static final Set<String> HIGHLIGHT_MARKER_IDS = new CopyOnWriteArraySet<>();
    private static final Set<String> HIGHLIGHT_REGION_IDS = new CopyOnWriteArraySet<>();
    private static boolean markerParticlesEnabled = true;
    private static boolean markerOffHandEnabled = false;
    private static boolean regionOffHandEnabled = false;
    private static MapState state = MapState.empty();

    private MapClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MapS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> handlePayload(context.client(), payload))
        );

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            state = MapState.empty();
            VISIBLE_REGION_IDS.clear();
            HIDDEN_MARKER_IDS.clear();
            PARTICLE_DISABLED_MARKER_IDS.clear();
            HIGHLIGHT_MARKER_IDS.clear();
            HIGHLIGHT_REGION_IDS.clear();
            RegionTitleOverlay.clear();
            MapCanvasRenderer.reset();
            notifyListeners();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            spawnMarkerParticles(client);
            spawnRegionParticles(client);
        });
    }

    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    public static MapState getState() {
        return state;
    }

    public static MapSettings getSettings() {
        return state.settings();
    }

    public static List<MapMarker> getMarkers() {
        return state.markers();
    }

    public static List<PlannerRegion> getPlannerRegions() {
        return state.plannerRegions();
    }

    public static List<PlannerDraft> getPlannerDrafts() {
        return state.plannerDrafts();
    }

    public static PlannerDraft getPlannerDraft() {
        return getLocalPlannerDraft();
    }

    public static PlannerDraft getLocalPlannerDraft() {
        String playerUuid = getLocalPlayerUuid();
        if (playerUuid.isBlank()) {
            return PlannerDraft.empty();
        }
        for (PlannerDraft draft : state.plannerDrafts()) {
            if (playerUuid.equals(draft.ownerUuid())) {
                return draft;
            }
        }
        return PlannerDraft.empty();
    }

    public static List<PlannerDraft> getRemotePlannerDrafts() {
        String playerUuid = getLocalPlayerUuid();
        List<PlannerDraft> drafts = new ArrayList<>();
        for (PlannerDraft draft : state.plannerDrafts()) {
            if (!draft.ownerUuid().equals(playerUuid)) {
                drafts.add(draft);
            }
        }
        return List.copyOf(drafts);
    }

    public static MapMarker getMarker(String markerId) {
        if (markerId == null || markerId.isBlank()) {
            return null;
        }
        for (MapMarker marker : state.markers()) {
            if (marker.id().equals(markerId)) {
                return marker;
            }
        }
        return null;
    }

    public static PlannerRegion getPlannerRegion(String regionId) {
        if (regionId == null || regionId.isBlank()) {
            return null;
        }
        for (PlannerRegion region : state.plannerRegions()) {
            if (region.id().equals(regionId)) {
                return region;
            }
        }
        return null;
    }

    public static PlannerRegion getCurrentRegion() {
        MinecraftClient client = MinecraftClient.getInstance();
        var player = client.player;
        var world = client.world;
        if (player == null || world == null) {
            return null;
        }
        String dimensionId = world.getRegistryKey().getValue().toString();
        int blockX = player.getBlockX();
        int blockZ = player.getBlockZ();
        for (PlannerRegion region : state.plannerRegions()) {
            if (!dimensionId.equals(region.dimensionId())) {
                continue;
            }
            if (RegionGeometry.containsBlock(region.toGeometryPoints(), blockX, blockZ)) {
                return region;
            }
        }
        return null;
    }

    public static boolean isRegionVisible(String regionId) {
        return regionId != null && VISIBLE_REGION_IDS.contains(regionId);
    }

    public static void setRegionVisible(String regionId, boolean visible) {
        if (regionId == null || regionId.isBlank()) {
            return;
        }
        if (visible) {
            VISIBLE_REGION_IDS.add(regionId);
        } else {
            VISIBLE_REGION_IDS.remove(regionId);
        }
        notifyListeners();
    }

    public static boolean isMarkerParticlesEnabled() {
        return markerParticlesEnabled;
    }

    public static void setMarkerParticlesEnabled(boolean enabled) {
        markerParticlesEnabled = enabled;
    }

    public static boolean isMarkerOffHandEnabled() {
        return markerOffHandEnabled;
    }

    public static void setMarkerOffHandEnabled(boolean enabled) {
        markerOffHandEnabled = enabled;
    }

    public static boolean isRegionOffHandEnabled() {
        return regionOffHandEnabled;
    }

    public static void setRegionOffHandEnabled(boolean enabled) {
        regionOffHandEnabled = enabled;
    }

    public static boolean isMarkerHighlighted(String markerId) {
        return markerId != null && HIGHLIGHT_MARKER_IDS.contains(markerId);
    }

    public static void setMarkerHighlighted(String markerId, boolean highlighted) {
        if (markerId == null || markerId.isBlank()) return;
        if (highlighted) {
            HIGHLIGHT_MARKER_IDS.add(markerId);
        } else {
            HIGHLIGHT_MARKER_IDS.remove(markerId);
        }
        notifyListeners();
    }

    public static boolean isRegionHighlighted(String regionId) {
        return regionId != null && HIGHLIGHT_REGION_IDS.contains(regionId);
    }

    public static void setRegionHighlighted(String regionId, boolean highlighted) {
        if (regionId == null || regionId.isBlank()) return;
        if (highlighted) {
            HIGHLIGHT_REGION_IDS.add(regionId);
        } else {
            HIGHLIGHT_REGION_IDS.remove(regionId);
        }
        notifyListeners();
    }

    public static boolean isMarkerParticleEnabled(String markerId) {
        return markerId != null && !PARTICLE_DISABLED_MARKER_IDS.contains(markerId);
    }

    public static void setMarkerParticleEnabled(String markerId, boolean enabled) {
        if (markerId == null || markerId.isBlank()) return;
        if (enabled) {
            PARTICLE_DISABLED_MARKER_IDS.remove(markerId);
        } else {
            PARTICLE_DISABLED_MARKER_IDS.add(markerId);
        }
        notifyListeners();
    }

    public static boolean isMarkerVisible(String markerId) {
        return markerId != null && !HIDDEN_MARKER_IDS.contains(markerId);
    }

    public static void setMarkerVisible(String markerId, boolean visible) {
        if (markerId == null || markerId.isBlank()) {
            return;
        }
        if (visible) {
            HIDDEN_MARKER_IDS.remove(markerId);
        } else {
            HIDDEN_MARKER_IDS.add(markerId);
        }
        notifyListeners();
    }

    public static void requestState() {
        send("request_state", new JsonObject());
    }

    public static void requestSnapshot() {
        send("request_snapshot", new JsonObject());
    }

    public static void deleteMarker(String markerId) {
        JsonObject body = new JsonObject();
        body.addProperty("markerId", markerId);
        send("delete_marker", body);
    }

    public static void setMarkerColor(String markerId, int color) {
        JsonObject body = new JsonObject();
        body.addProperty("markerId", markerId);
        body.addProperty("color", color);
        updateLocalMarkerColor(markerId, color);
        send("set_marker_color", body);
    }

    public static void setMarkerName(String markerId, String name) {
        JsonObject body = new JsonObject();
        body.addProperty("markerId", markerId);
        body.addProperty("name", name);
        updateLocalMarkerName(markerId, name);
        send("rename_marker", body);
    }

    public static void teleportToMarker(String markerId) {
        JsonObject body = new JsonObject();
        body.addProperty("markerId", markerId);
        send("teleport_marker", body);
    }

    public static void renameRegion(String regionId, String name) {
        JsonObject body = new JsonObject();
        body.addProperty("regionId", regionId);
        body.addProperty("name", name);
        send("rename_region", body);
    }

    public static void deleteRegion(String regionId) {
        JsonObject body = new JsonObject();
        body.addProperty("regionId", regionId);
        send("delete_region", body);
    }

    public static void setRegionColor(String regionId, int color) {
        JsonObject body = new JsonObject();
        body.addProperty("regionId", regionId);
        body.addProperty("color", color);
        send("set_region_color", body);
    }

    public static void teleportToRegionCorner(String regionId, int pointIndex) {
        JsonObject body = new JsonObject();
        body.addProperty("regionId", regionId);
        body.addProperty("pointIndex", pointIndex);
        send("teleport_region_corner", body);
    }

    public static void clearPlannerDraft() {
        send("clear_region_draft", new JsonObject());
    }

    public static void trimPlannerDraft(int pointIndex) {
        JsonObject body = new JsonObject();
        body.addProperty("pointIndex", pointIndex);
        send("trim_region_draft", body);
    }

    public static void setDraftColor(int color) {
        JsonObject body = new JsonObject();
        body.addProperty("color", color);
        send("set_draft_color", body);
    }

    public static void teleportToDraftCorner(int pointIndex) {
        JsonObject body = new JsonObject();
        body.addProperty("pointIndex", pointIndex);
        send("teleport_draft_corner", body);
    }

    public static void setVisibility(String key, boolean enabled) {
        JsonObject body = new JsonObject();
        body.addProperty("key", key);
        body.addProperty("enabled", enabled);
        updateLocalVisibility(key, enabled);
        send("set_visibility", body);
    }

    private static void send(String action, JsonObject body) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) {
            return;
        }
        ClientPlayNetworking.send(new MapC2SPayload(action, body.toString()));
    }

    private static void handlePayload(MinecraftClient client, MapS2CPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "state" -> applyState(body, false);
            case "snapshot" -> applyState(body, true);
            case "error" -> showError(client, body);
            case "region_title" -> showRegionTitle(body);
            default -> {
            }
        }
    }

    private static void applyState(JsonObject body, boolean includeSnapshot) {
        boolean hasRegion = getBoolean(body, "hasRegion", false);
        int regionVersion = getInt(body, "regionVersion", 0);

        MapRegion region = null;
        if (hasRegion) {
            region = new MapRegion(
                    getString(body, "dimensionId"),
                    getInt(body, "minX", 0),
                    getInt(body, "minY", 0),
                    getInt(body, "minZ", 0),
                    getInt(body, "maxX", 0),
                    getInt(body, "maxY", 0),
                    getInt(body, "maxZ", 0)
            );
        }

        JsonObject settingsObject = body.has("settings") && body.get("settings").isJsonObject()
                ? body.getAsJsonObject("settings")
                : new JsonObject();
        MapSettings settings = new MapSettings(
                getBoolean(settingsObject, "showSelfPosition", true),
                getBoolean(settingsObject, "showMarkers", true),
                getBoolean(settingsObject, "showOtherPlayers", true),
                getBoolean(settingsObject, "showRegionTitles", true)
        );

        List<MapMarker> markers = new ArrayList<>();
        if (body.has("markers") && body.get("markers").isJsonArray()) {
            for (JsonElement element : body.getAsJsonArray("markers")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject marker = element.getAsJsonObject();
                markers.add(new MapMarker(
                        getString(marker, "id"),
                        getString(marker, "name"),
                        getString(marker, "dimensionId"),
                        getInt(marker, "x", 0),
                        getInt(marker, "y", 0),
                        getInt(marker, "z", 0),
                        getInt(marker, "color", 0xFFE9ECEF)
                ));
            }
        }
        markers.sort(Comparator.comparing(MapMarker::name, String.CASE_INSENSITIVE_ORDER));

        List<PlannerRegion> plannerRegions = new ArrayList<>();
        if (body.has("plannerRegions") && body.get("plannerRegions").isJsonArray()) {
            for (JsonElement element : body.getAsJsonArray("plannerRegions")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject regionObject = element.getAsJsonObject();
                List<RegionPoint> points = parsePoints(regionObject, "points");
                plannerRegions.add(new PlannerRegion(
                        getString(regionObject, "id"),
                        getString(regionObject, "name"),
                        getString(regionObject, "dimensionId"),
                        List.copyOf(points),
                        getInt(regionObject, "color", 0xFF58D68D)
                ));
            }
        }
        plannerRegions.sort(Comparator.comparing(PlannerRegion::name, String.CASE_INSENSITIVE_ORDER));

        List<PlannerDraft> plannerDrafts = new ArrayList<>();
        if (body.has("plannerDrafts") && body.get("plannerDrafts").isJsonArray()) {
            for (JsonElement element : body.getAsJsonArray("plannerDrafts")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject draftObject = element.getAsJsonObject();
                Set<Integer> warningSegments = new HashSet<>();
                if (draftObject.has("warningSegments") && draftObject.get("warningSegments").isJsonArray()) {
                    for (JsonElement warningElement : draftObject.getAsJsonArray("warningSegments")) {
                        try {
                            warningSegments.add(warningElement.getAsInt());
                        } catch (Exception ignored) {
                        }
                    }
                }
                plannerDrafts.add(new PlannerDraft(
                        getString(draftObject, "ownerUuid"),
                        getString(draftObject, "ownerName"),
                        getString(draftObject, "dimensionId"),
                        getInt(draftObject, "color", 0xFF4DABF7),
                        List.copyOf(parsePoints(draftObject, "points")),
                        Set.copyOf(warningSegments)
                ));
            }
        }
        plannerDrafts.sort(Comparator.comparing(PlannerDraft::ownerName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PlannerDraft::ownerUuid));

        List<MapPlayer> players = new ArrayList<>();
        if (body.has("players") && body.get("players").isJsonArray()) {
            for (JsonElement element : body.getAsJsonArray("players")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject player = element.getAsJsonObject();
                players.add(new MapPlayer(
                        getString(player, "uuid"),
                        getString(player, "name"),
                        getDouble(player, "x", 0.0D),
                        getDouble(player, "y", 0.0D),
                        getDouble(player, "z", 0.0D),
                        getBoolean(player, "self", false)
                ));
            }
        }

        int imageWidth = state.imageWidth();
        int imageHeight = state.imageHeight();
        int[] imageColors = state.imageColors();
        int imageHash = state.imageHash();
        if (includeSnapshot) {
            imageWidth = Math.max(0, getInt(body, "imageWidth", 0));
            imageHeight = Math.max(0, getInt(body, "imageHeight", 0));
            imageColors = parseColors(getString(body, "colorsHex"), imageWidth, imageHeight);
            imageHash = Arrays.hashCode(imageColors);
        } else if (!hasRegion) {
            imageWidth = 0;
            imageHeight = 0;
            imageColors = new int[0];
            imageHash = 0;
        } else if (regionVersion != state.regionVersion() || imageColors.length == 0) {
            requestSnapshot();
        }

        retainVisibleRegionIds(plannerRegions);
        state = new MapState(
                hasRegion,
                region,
                settings,
                List.copyOf(markers),
                List.copyOf(players),
                List.copyOf(plannerRegions),
                List.copyOf(plannerDrafts),
                imageWidth,
                imageHeight,
                imageColors,
                imageHash,
                regionVersion
        );
        notifyListeners();
    }

    private static List<RegionPoint> parsePoints(JsonObject object, String key) {
        List<RegionPoint> points = new ArrayList<>();
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return points;
        }
        for (JsonElement pointElement : object.getAsJsonArray(key)) {
            if (!pointElement.isJsonObject()) {
                continue;
            }
            JsonObject pointObject = pointElement.getAsJsonObject();
            points.add(new RegionPoint(getInt(pointObject, "x", 0), getInt(pointObject, "z", 0)));
        }
        return points;
    }

    private static void retainVisibleRegionIds(List<PlannerRegion> plannerRegions) {
        Set<String> validIds = new HashSet<>();
        for (PlannerRegion region : plannerRegions) {
            validIds.add(region.id());
        }
        VISIBLE_REGION_IDS.retainAll(validIds);
        HIGHLIGHT_REGION_IDS.retainAll(validIds);
    }

    private static int[] parseColors(String colorsHex, int imageWidth, int imageHeight) {
        if (colorsHex == null || colorsHex.isBlank() || imageWidth <= 0 || imageHeight <= 0) {
            return new int[0];
        }
        int expectedLength = imageWidth * imageHeight * 6;
        if (colorsHex.length() < expectedLength) {
            return new int[0];
        }
        int[] colors = new int[imageWidth * imageHeight];
        for (int index = 0; index < colors.length; index++) {
            int start = index * 6;
            try {
                colors[index] = Integer.parseInt(colorsHex.substring(start, start + 6), 16);
            } catch (Exception exception) {
                colors[index] = 0x1D2230;
            }
        }
        return colors;
    }

    private static void showError(MinecraftClient client, JsonObject body) {
        var player = client.player;
        if (player == null) {
            return;
        }
        String message = getString(body, "message");
        if (!message.isBlank()) {
            player.sendMessage(Text.literal("[Map] " + message), false);
        }
    }

    private static void showRegionTitle(JsonObject body) {
        String name = getString(body, "name").trim();
        if (!name.isBlank()) {
            RegionTitleOverlay.show(Text.literal(name), getInt(body, "color", 0xFF7FE9AA));
        }
    }

    private static void spawnMarkerParticles(MinecraftClient client) {
        if (!markerParticlesEnabled) {
            return;
        }
        var world = client.world;
        var player = client.player;
        if (world == null || player == null || world.getTime() % 2L != 0L) {
            return;
        }
        boolean holdingTool = player.getMainHandStack().isOf(ModItems.MAP_MARKER)
                || player.getOffHandStack().isOf(ModItems.MAP_MARKER);
        boolean showAll = holdingTool || markerOffHandEnabled;
        if (!showAll && HIGHLIGHT_MARKER_IDS.isEmpty()) {
            return;
        }
        String dimensionId = world.getRegistryKey().getValue().toString();
        long worldTime = world.getTime();
        for (int index = 0; index < state.markers().size(); index++) {
            MapMarker marker = state.markers().get(index);
            if (!dimensionId.equals(marker.dimensionId())) {
                continue;
            }
            boolean highlighted = isMarkerHighlighted(marker.id());
            if (!showAll && !highlighted) {
                continue;
            }
            if (!isMarkerParticleEnabled(marker.id())) {
                continue;
            }
            if (!highlighted) {
                double dx = marker.x() + 0.5D - player.getX();
                double dy = marker.y() + 0.5D - player.getY();
                double dz = marker.z() + 0.5D - player.getZ();
                if (dx * dx + dy * dy + dz * dz > 256.0D * 256.0D) {
                    continue;
                }
            }
            for (int step = 0; step < 8; step++) {
                float hue = ((worldTime * 4L + index * 18L + step * 24L) % 360L) / 360.0F;
                int rgb = hsvToRgb(hue, 0.85F, 1.0F);
                double particleY = marker.y() + 0.35D + step * 1.4D;
                spawnDustSafe(client, marker.x() + 0.5D, particleY, marker.z() + 0.5D, rgb, 1.2F, highlighted);
            }
        }
    }

    private static void spawnRegionParticles(MinecraftClient client) {
        var world = client.world;
        var player = client.player;
        if (world == null || player == null || world.getTime() % 2L != 0L) {
            return;
        }
        boolean holdingTool = player.getMainHandStack().isOf(ModItems.REGION_PLANNER)
                || player.getOffHandStack().isOf(ModItems.REGION_PLANNER);
        boolean showAll = holdingTool || regionOffHandEnabled;

        String dimensionId = world.getRegistryKey().getValue().toString();
        double baseY = -63.0D;
        double topY = Math.max(baseY, player.getY());

        for (PlannerRegion region : state.plannerRegions()) {
            if (!dimensionId.equals(region.dimensionId())) {
                continue;
            }
            boolean visible = VISIBLE_REGION_IDS.contains(region.id());
            boolean highlighted = isRegionHighlighted(region.id());
            if (!highlighted && !(showAll && visible)) {
                continue;
            }
            spawnWireframe(client, region.toGeometryPoints(), baseY, topY, region.color(), true, Set.of(), true, highlighted);
        }

        if (!showAll) {
            return;
        }
        String localPlayerUuid = getLocalPlayerUuid();
        for (PlannerDraft draft : state.plannerDrafts()) {
            if (!dimensionId.equals(draft.dimensionId()) || draft.points().isEmpty()) {
                continue;
            }
            boolean localDraft = localPlayerUuid.equals(draft.ownerUuid());
            int draftColor = localDraft ? draft.color() : mixColor(draft.color(), 0xFFFFFF, 0.2F);
            spawnWireframe(client, toGeometryPoints(draft.points()), baseY, topY, draftColor, false, draft.warningSegments(), localDraft, false);
        }
    }

    private static void spawnWireframe(
            MinecraftClient client,
            List<RegionGeometry.Point> points,
            double baseY,
            double topY,
            int color,
            boolean closed,
            Set<Integer> warningSegments,
            boolean highlightFirstPoint,
            boolean forced
    ) {
        if (points.isEmpty()) {
            return;
        }

        int edgeCount = closed ? points.size() : points.size() - 1;
        for (int index = 0; index < points.size(); index++) {
            RegionGeometry.Point point = points.get(index);
            int pointColor = index == 0 && highlightFirstPoint ? mixColor(color, 0xFFFFFF, 0.4F) : color;
            spawnVerticalEdge(client, point.x() + 0.5D, point.z() + 0.5D, baseY, topY, pointColor,
                    index == 0 && highlightFirstPoint ? 1.1F : 0.8F, forced);
        }

        for (int index = 0; index < edgeCount; index++) {
            RegionGeometry.Point start = points.get(index);
            RegionGeometry.Point end = points.get((index + 1) % points.size());
            int edgeColor = warningSegments.contains(index) ? 0xFFE74C3C : color;
            spawnHorizontalSegment(client, start.x() + 0.5D, start.z() + 0.5D, end.x() + 0.5D, end.z() + 0.5D, baseY + 0.15D, edgeColor, 0.72F, forced);
            if (topY > baseY) {
                spawnHorizontalSegment(client, start.x() + 0.5D, start.z() + 0.5D, end.x() + 0.5D, end.z() + 0.5D, topY + 0.15D, edgeColor, 0.88F, forced);
            }
        }
    }

    private static void spawnVerticalEdge(MinecraftClient client, double x, double z, double baseY, double topY, int color, float scale, boolean forced) {
        double startY = baseY + 0.15D;
        double endY = topY + 0.15D;
        if (endY <= startY) {
            spawnDustSafe(client, x, startY, z, color, scale, forced);
            return;
        }

        double span = endY - startY;
        int steps = Math.max(1, (int) Math.ceil(span / 2.0D));
        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            double y = startY + span * progress;
            float pointScale = step == 0 || step == steps ? scale + 0.08F : scale * 0.82F;
            spawnDustSafe(client, x, y, z, color, pointScale, forced);
        }
    }

    private static void spawnHorizontalSegment(MinecraftClient client, double startX, double startZ, double endX, double endZ, double y, int color, float scale, boolean forced) {
        double deltaX = endX - startX;
        double deltaZ = endZ - startZ;
        double distance = Math.max(0.001D, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ));
        int steps = Math.max(2, (int) Math.ceil(distance / 1.5D));
        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            double x = startX + deltaX * progress;
            double z = startZ + deltaZ * progress;
            spawnDustSafe(client, x, y, z, color, scale, forced);
        }
    }

    private static void spawnDustSafe(MinecraftClient client, double x, double y, double z, int color, float scale, boolean forced) {
        var player = client.player;
        var world = client.world;
        if (player == null || world == null) {
            return;
        }
        if (!forced) {
            double dx = x - player.getX();
            double dz = z - player.getZ();
            if (dx * dx + dz * dz > PARTICLE_RANGE_SQ) {
                return;
            }
            BlockPos pos = BlockPos.ofFloored(x, y, z);
            var blockState = world.getBlockState(pos);
            if (!blockState.isAir() && !blockState.getFluidState().isIn(FluidTags.WATER)) {
                return;
            }
        }
        client.particleManager.addParticle(new DustParticleEffect(color, scale), x, y, z, 0.0D, 0.0D, 0.0D);
    }

    private static List<RegionGeometry.Point> toGeometryPoints(List<RegionPoint> points) {
        List<RegionGeometry.Point> geometryPoints = new ArrayList<>(points.size());
        for (RegionPoint point : points) {
            geometryPoints.add(point.toGeometryPoint());
        }
        return geometryPoints;
    }

    private static int mixColor(int baseColor, int targetColor, float amount) {
        float clamped = Math.max(0.0F, Math.min(1.0F, amount));
        int baseRed = (baseColor >> 16) & 0xFF;
        int baseGreen = (baseColor >> 8) & 0xFF;
        int baseBlue = baseColor & 0xFF;
        int targetRed = (targetColor >> 16) & 0xFF;
        int targetGreen = (targetColor >> 8) & 0xFF;
        int targetBlue = targetColor & 0xFF;
        int red = Math.round(baseRed + (targetRed - baseRed) * clamped);
        int green = Math.round(baseGreen + (targetGreen - baseGreen) * clamped);
        int blue = Math.round(baseBlue + (targetBlue - baseBlue) * clamped);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float wrappedHue = hue - (float) Math.floor(hue);
        float scaled = wrappedHue * 6.0F;
        int sector = (int) Math.floor(scaled);
        float fraction = scaled - sector;
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - fraction * saturation);
        float t = value * (1.0F - (1.0F - fraction) * saturation);

        float red;
        float green;
        float blue;
        switch (sector % 6) {
            case 0 -> {
                red = value;
                green = t;
                blue = p;
            }
            case 1 -> {
                red = q;
                green = value;
                blue = p;
            }
            case 2 -> {
                red = p;
                green = value;
                blue = t;
            }
            case 3 -> {
                red = p;
                green = q;
                blue = value;
            }
            case 4 -> {
                red = t;
                green = p;
                blue = value;
            }
            default -> {
                red = value;
                green = p;
                blue = q;
            }
        }

        return (Math.round(red * 255.0F) << 16) | (Math.round(green * 255.0F) << 8) | Math.round(blue * 255.0F);
    }

    private static void updateLocalMarkerColor(String markerId, int color) {
        List<MapMarker> updated = new ArrayList<>(state.markers());
        for (int index = 0; index < updated.size(); index++) {
            MapMarker marker = updated.get(index);
            if (!marker.id().equals(markerId)) {
                continue;
            }
            updated.set(index, new MapMarker(marker.id(), marker.name(), marker.dimensionId(), marker.x(), marker.y(), marker.z(), color));
            state = state.withMarkers(List.copyOf(updated));
            notifyListeners();
            return;
        }
    }

    private static void updateLocalMarkerName(String markerId, String name) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isBlank()) {
            return;
        }
        List<MapMarker> updated = new ArrayList<>(state.markers());
        for (int index = 0; index < updated.size(); index++) {
            MapMarker marker = updated.get(index);
            if (!marker.id().equals(markerId)) {
                continue;
            }
            updated.set(index, new MapMarker(marker.id(), cleanName, marker.dimensionId(), marker.x(), marker.y(), marker.z(), marker.color()));
            updated.sort(Comparator.comparing(MapMarker::name, String.CASE_INSENSITIVE_ORDER));
            state = state.withMarkers(List.copyOf(updated));
            notifyListeners();
            return;
        }
    }

    private static void updateLocalVisibility(String key, boolean enabled) {
        MapSettings settings = switch (key) {
            case "show_self_position" -> new MapSettings(enabled, state.settings().showMarkers(), state.settings().showOtherPlayers(), state.settings().showRegionTitles());
            case "show_markers" -> new MapSettings(state.settings().showSelfPosition(), enabled, state.settings().showOtherPlayers(), state.settings().showRegionTitles());
            case "show_other_players" -> new MapSettings(state.settings().showSelfPosition(), state.settings().showMarkers(), enabled, state.settings().showRegionTitles());
            case "show_region_titles" -> new MapSettings(state.settings().showSelfPosition(), state.settings().showMarkers(), state.settings().showOtherPlayers(), enabled);
            default -> state.settings();
        };
        state = state.withSettings(settings);
        notifyListeners();
    }

    private static String getLocalPlayerUuid() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return "";
        }
        return client.player.getUuidAsString();
    }

    private static void notifyListeners() {
        for (Runnable listener : LISTENERS) {
            listener.run();
        }
    }

    private static JsonObject parse(String body) {
        try {
            if (body == null || body.isBlank()) {
                return new JsonObject();
            }
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double getDouble(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public record MapRegion(String dimensionId, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public int width() {
            return maxX - minX + 1;
        }

        public int depth() {
            return maxZ - minZ + 1;
        }
    }

    public record MapSettings(boolean showSelfPosition, boolean showMarkers, boolean showOtherPlayers, boolean showRegionTitles) {
    }

    public record MapMarker(String id, String name, String dimensionId, int x, int y, int z, int color) {
    }

    public record MapPlayer(String uuid, String name, double x, double y, double z, boolean self) {
    }

    public record RegionPoint(int x, int z) {
        public RegionGeometry.Point toGeometryPoint() {
            return new RegionGeometry.Point(x, z);
        }
    }

    public record PlannerRegion(String id, String name, String dimensionId, List<RegionPoint> points, int color) {
        public RegionGeometry.Bounds bounds() {
            return RegionGeometry.bounds(toGeometryPoints());
        }

        public List<RegionGeometry.Point> toGeometryPoints() {
            return MapClient.toGeometryPoints(points);
        }
    }

    public record PlannerDraft(String ownerUuid, String ownerName, String dimensionId, int color, List<RegionPoint> points, Set<Integer> warningSegments) {
        public static PlannerDraft empty() {
            return new PlannerDraft("", "", "", 0xFF4DABF7, List.of(), Set.of());
        }

        public boolean isEmpty() {
            return points.isEmpty();
        }
    }

    public record MapState(
            boolean hasRegion,
            MapRegion region,
            MapSettings settings,
            List<MapMarker> markers,
            List<MapPlayer> players,
            List<PlannerRegion> plannerRegions,
            List<PlannerDraft> plannerDrafts,
            int imageWidth,
            int imageHeight,
            int[] imageColors,
            int imageHash,
            int regionVersion
    ) {
        public static MapState empty() {
            return new MapState(false, null, new MapSettings(true, true, true, true), List.of(), List.of(), List.of(), List.of(), 0, 0, new int[0], 0, 0);
        }

        public MapState withMarkers(List<MapMarker> updatedMarkers) {
            return new MapState(hasRegion, region, settings, updatedMarkers, players, plannerRegions, plannerDrafts, imageWidth, imageHeight, imageColors, imageHash, regionVersion);
        }

        public MapState withSettings(MapSettings updatedSettings) {
            return new MapState(hasRegion, region, updatedSettings, markers, players, plannerRegions, plannerDrafts, imageWidth, imageHeight, imageColors, imageHash, regionVersion);
        }
    }
}