package com.zcpu.tzzmod.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.network.MapC2SPayload;
import com.zcpu.tzzmod.network.MapS2CPayload;
import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MapServer {
    private static final int MAX_SNAPSHOT_SIZE = 64;
    private static final Map<UUID, PlannerDraft> PLANNER_DRAFTS = new HashMap<>();
    private static final Map<UUID, PlayerRegionTracker> REGION_TRACKERS = new HashMap<>();

    private MapServer() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> sendStateToPlayer(server, handler.getPlayer()))
        );
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                server.execute(() -> {
                    clearPlayerState(handler.getPlayer().getUuid());
                    broadcastState(server);
                })
        );
        ServerTickEvents.END_SERVER_TICK.register(MapServer::tickPlayerRegions);

        ServerPlayNetworking.registerGlobalReceiver(MapC2SPayload.ID, (payload, context) ->
                context.server().execute(() -> handlePayload(context.server(), context.player(), payload))
        );
    }

    public static void broadcastState(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendStateToPlayer(server, player);
        }
    }

    public static void broadcastSnapshot(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendSnapshotToPlayer(server, player);
        }
    }

    public static void sendStateToPlayer(MinecraftServer server, ServerPlayerEntity player) {
        JsonObject body = buildStateBody(server, player, false);
        ServerPlayNetworking.send(NullSafety.requireNonNull(player), new MapS2CPayload("state", body.toString()));
    }

    public static void sendSnapshotToPlayer(MinecraftServer server, ServerPlayerEntity player) {
        JsonObject body = buildStateBody(server, player, true);
        ServerPlayNetworking.send(NullSafety.requireNonNull(player), new MapS2CPayload("snapshot", body.toString()));
    }

    public static Text handlePlannerSelection(MinecraftServer server, ServerPlayerEntity player, BlockPos pos) {
        String dimensionId = player.getCommandSource().getWorld().getRegistryKey().getValue().toString();
        PlannerDraft draft = getOrCreateDraft(player, dimensionId);

        RegionGeometry.Point clickedPoint = new RegionGeometry.Point(pos.getX(), pos.getZ());
        if (draft.points().size() >= 3 && isFirstDraftPoint(draft, clickedPoint)) {
            MapDataStore.PlannerRegionResult result = MapDataStore.addPlannerRegion(server, dimensionId, draft.points(), draft.color());
            if (result.status() == MapDataStore.PlannerRegionStatus.OK && result.region() != null) {
                PLANNER_DRAFTS.remove(player.getUuid());
                broadcastState(server);
                return Text.literal("已完成区域规划：" + result.region().name());
            }
            sendStateToPlayer(server, player);
            return describePlannerFailure(result.status());
        }

        int lastIndex = draft.points().size() - 1;
        if (lastIndex >= 0) {
            RegionGeometry.Point lastPoint = draft.points().get(lastIndex);
            if (lastPoint.x() == clickedPoint.x() && lastPoint.z() == clickedPoint.z()) {
                sendStateToPlayer(server, player);
                return Text.literal("该角点已标记，无需重复添加。");
            }
        }

        draft.points().add(clickedPoint);
        refreshDraftWarnings(server, draft);
        broadcastState(server);

        if (!draft.warningSegments().isEmpty() && draft.warningSegments().contains(draft.points().size() - 2)) {
            return Text.literal("该线段进入了已存在的区域，已用红色粒子标记。");
        }
        if (draft.points().size() == 1) {
            return Text.literal("已标记第一个角点，再次右键继续添加。");
        }
        return Text.literal("已添加角点 " + draft.points().size() + "，回到首点即可完成区域。");
    }

    private static void handlePayload(MinecraftServer server, ServerPlayerEntity player, MapC2SPayload payload) {
        JsonObject body = parse(payload.bodyJson());
        switch (payload.action()) {
            case "request_state" -> sendStateToPlayer(server, player);
            case "request_snapshot" -> sendSnapshotToPlayer(server, player);
            case "delete_marker" -> handleDeleteMarker(server, player, body);
            case "set_marker_color" -> handleSetMarkerColor(server, player, body);
            case "rename_marker" -> handleRenameMarker(server, player, body);
            case "teleport_marker" -> handleTeleportMarker(server, player, body);
            case "set_visibility" -> handleSetVisibility(server, player, body);
            case "rename_region" -> handleRenameRegion(server, player, body);
            case "delete_region" -> handleDeleteRegion(server, player, body);
            case "set_region_color" -> handleSetRegionColor(server, player, body);
            case "teleport_region_corner" -> handleTeleportRegionCorner(server, player, body);
            case "clear_region_draft" -> handleClearRegionDraft(server, player);
            case "trim_region_draft" -> handleTrimRegionDraft(server, player, body);
            case "set_draft_color" -> handleSetDraftColor(server, player, body);
            case "teleport_draft_corner" -> handleTeleportDraftCorner(server, player, body);
            default -> sendError(player, "Unknown map action: " + payload.action());
        }
    }

    private static void handleDeleteMarker(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String markerId = getString(body, "markerId");
        if (markerId.isBlank()) {
            sendError(player, "标点不存在。");
            return;
        }
        if (!MapDataStore.deleteMarker(server, markerId)) {
            sendError(player, "未找到对应的标点。");
            return;
        }
        broadcastState(server);
    }

    private static void handleSetMarkerColor(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String markerId = getString(body, "markerId");
        int color = getInt(body, "color", 0xFFE9ECEF);
        if (markerId.isBlank()) {
            sendError(player, "标点不存在。");
            return;
        }
        if (!MapDataStore.setMarkerColor(server, markerId, color)) {
            sendError(player, "未找到对应的标点。");
            return;
        }
        broadcastState(server);
    }

    private static void handleRenameMarker(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String markerId = getString(body, "markerId");
        String name = getString(body, "name").trim();
        if (markerId.isBlank()) {
            sendError(player, "标点不存在。");
            return;
        }
        if (name.isBlank()) {
            sendError(player, "标点名称不能为空。");
            return;
        }
        if (!MapDataStore.setMarkerName(server, markerId, name)) {
            sendError(player, "未找到对应的标点。");
            return;
        }
        broadcastState(server);
    }

    private static void handleTeleportMarker(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String markerId = getString(body, "markerId");
        if (markerId.isBlank()) {
            sendError(player, "标点不存在。");
            return;
        }

        MapDataStore.MapMarkerData marker = MapDataStore.getMarker(server, markerId);
        if (marker == null) {
            sendError(player, "未找到对应的标点。");
            return;
        }

        String playerDimension = player.getCommandSource().getWorld().getRegistryKey().getValue().toString();
        if (!playerDimension.equals(marker.dimensionId())) {
            sendError(player, "该标点不在当前维度，暂不支持跨维度传送。");
            return;
        }

        teleportPlayer(player, marker.x() + 0.5D, marker.y() + 1.0D, marker.z() + 0.5D);
        player.sendMessage(Text.literal("已传送至标点：" + marker.name()), true);
    }

    private static void handleSetVisibility(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        if (!player.isCreativeLevelTwoOp()) {
            sendError(player, "只有 OP 可以修改地图全局开关。");
            return;
        }

        String key = getString(body, "key");
        boolean enabled = getBoolean(body, "enabled", true);
        if (!MapDataStore.setVisibility(server, key, enabled)) {
            sendError(player, "未识别的地图全局开关。");
            return;
        }
        broadcastState(server);
    }

    private static void handleRenameRegion(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String regionId = getString(body, "regionId");
        String name = getString(body, "name").trim();
        if (regionId.isBlank()) {
            sendError(player, "区域不存在。");
            return;
        }
        MapDataStore.PlannerRegionResult result = MapDataStore.renamePlannerRegion(server, regionId, name);
        if (result.status() != MapDataStore.PlannerRegionStatus.OK) {
            sendError(player, describePlannerFailure(result.status()).getString());
            return;
        }
        broadcastState(server);
    }

    private static void handleDeleteRegion(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String regionId = getString(body, "regionId");
        if (regionId.isBlank()) {
            sendError(player, "区域不存在。");
            return;
        }
        if (!MapDataStore.deletePlannerRegion(server, regionId)) {
            sendError(player, "未找到对应的区域。");
            return;
        }
        for (PlayerRegionTracker tracker : REGION_TRACKERS.values()) {
            if (regionId.equals(tracker.currentRegionId())) {
                tracker.setCurrentRegionId("");
            }
        }
        broadcastState(server);
    }

    private static void handleSetRegionColor(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String regionId = getString(body, "regionId");
        int color = getInt(body, "color", 0xFFE9ECEF);
        if (regionId.isBlank()) {
            sendError(player, "区域不存在。");
            return;
        }
        MapDataStore.PlannerRegionResult result = MapDataStore.setPlannerRegionColor(server, regionId, color);
        if (result.status() != MapDataStore.PlannerRegionStatus.OK) {
            sendError(player, describePlannerFailure(result.status()).getString());
            return;
        }
        broadcastState(server);
    }

    private static void handleTeleportRegionCorner(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        String regionId = getString(body, "regionId");
        int pointIndex = getInt(body, "pointIndex", -1);
        if (regionId.isBlank() || pointIndex < 0) {
            sendError(player, "区域角点不存在。");
            return;
        }
        MapDataStore.PlannerRegionData region = MapDataStore.getPlannerRegion(server, regionId);
        if (region == null || pointIndex >= region.points().size()) {
            sendError(player, "未找到对应的区域角点。");
            return;
        }

        String playerDimension = player.getCommandSource().getWorld().getRegistryKey().getValue().toString();
        if (!playerDimension.equals(region.dimensionId())) {
            sendError(player, "该区域不在当前维度，暂不支持跨维度传送。");
            return;
        }

        ServerWorld world = resolveWorld(server, region.dimensionId());
        if (world == null) {
            sendError(player, "当前维度不可用，无法传送。");
            return;
        }

        RegionGeometry.Point point = region.points().get(pointIndex);
        int safeY = world.getTopY(Heightmap.Type.WORLD_SURFACE, point.x(), point.z()) + 1;
        teleportPlayer(player, point.x() + 0.5D, safeY, point.z() + 0.5D);
        player.sendMessage(Text.literal("已传送至角点：" + formatCornerName(region.name(), pointIndex)), true);
    }

    private static void handleClearRegionDraft(MinecraftServer server, ServerPlayerEntity player) {
        PLANNER_DRAFTS.remove(player.getUuid());
        broadcastState(server);
    }

    private static void handleTrimRegionDraft(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        PlannerDraft draft = PLANNER_DRAFTS.get(player.getUuid());
        int pointIndex = getInt(body, "pointIndex", -1);
        if (draft == null || pointIndex < 0 || pointIndex >= draft.points().size()) {
            sendError(player, "未找到可裁剪的草稿角点。");
            return;
        }
        draft.points().subList(pointIndex, draft.points().size()).clear();
        refreshDraftWarnings(server, draft);
        if (draft.points().isEmpty()) {
            PLANNER_DRAFTS.remove(player.getUuid());
        }
        broadcastState(server);
    }

    private static void handleSetDraftColor(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        PlannerDraft draft = PLANNER_DRAFTS.get(player.getUuid());
        if (draft == null) {
            sendError(player, "当前没有可修改颜色的区域草稿。");
            return;
        }
        draft.setColor(0xFF000000 | (getInt(body, "color", draft.color()) & 0xFFFFFF));
        broadcastState(server);
    }

    private static void handleTeleportDraftCorner(MinecraftServer server, ServerPlayerEntity player, JsonObject body) {
        PlannerDraft draft = PLANNER_DRAFTS.get(player.getUuid());
        int pointIndex = getInt(body, "pointIndex", -1);
        if (draft == null || pointIndex < 0 || pointIndex >= draft.points().size()) {
            sendError(player, "未找到对应的草稿角点。");
            return;
        }
        String playerDimension = player.getCommandSource().getWorld().getRegistryKey().getValue().toString();
        if (!playerDimension.equals(draft.dimensionId())) {
            sendError(player, "草稿不在当前维度，暂不支持跨维度传送。");
            return;
        }
        ServerWorld world = resolveWorld(server, draft.dimensionId());
        if (world == null) {
            sendError(player, "当前维度不可用，无法传送。");
            return;
        }
        RegionGeometry.Point point = draft.points().get(pointIndex);
        int safeY = world.getTopY(Heightmap.Type.WORLD_SURFACE, point.x(), point.z()) + 1;
        teleportPlayer(player, point.x() + 0.5D, safeY, point.z() + 0.5D);
        player.sendMessage(Text.literal("已传送至角点：" + formatCornerName("草稿", pointIndex)), true);
    }

    private static JsonObject buildStateBody(MinecraftServer server, ServerPlayerEntity recipient, boolean includeSnapshot) {
        MapDataStore.MapSnapshot snapshot = MapDataStore.getSnapshot(server);
        JsonObject body = new JsonObject();
        body.addProperty("hasRegion", snapshot.region() != null);
        body.addProperty("regionVersion", snapshot.regionVersion());

        if (snapshot.region() != null) {
            MapDataStore.MapRegionData region = snapshot.region();
            body.addProperty("dimensionId", region.dimensionId());
            body.addProperty("minX", region.minX());
            body.addProperty("minY", region.minY());
            body.addProperty("minZ", region.minZ());
            body.addProperty("maxX", region.maxX());
            body.addProperty("maxY", region.maxY());
            body.addProperty("maxZ", region.maxZ());
        }

        JsonObject settings = new JsonObject();
        settings.addProperty("showSelfPosition", snapshot.settings().showSelfPosition());
        settings.addProperty("showMarkers", snapshot.settings().showMarkers());
        settings.addProperty("showOtherPlayers", snapshot.settings().showOtherPlayers());
        settings.addProperty("showRegionTitles", snapshot.settings().showRegionTitles());
        body.add("settings", settings);

        JsonArray markers = new JsonArray();
        for (MapDataStore.MapMarkerData marker : snapshot.markers()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", marker.id());
            entry.addProperty("name", marker.name());
            entry.addProperty("dimensionId", marker.dimensionId());
            entry.addProperty("x", marker.x());
            entry.addProperty("y", marker.y());
            entry.addProperty("z", marker.z());
            entry.addProperty("color", marker.color());
            markers.add(entry);
        }
        body.add("markers", markers);

        JsonArray plannerRegions = new JsonArray();
        for (MapDataStore.PlannerRegionData region : snapshot.plannerRegions()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", region.id());
            entry.addProperty("name", region.name());
            entry.addProperty("dimensionId", region.dimensionId());
            entry.addProperty("color", region.color());
            JsonArray points = new JsonArray();
            for (RegionGeometry.Point point : region.points()) {
                JsonObject pointEntry = new JsonObject();
                pointEntry.addProperty("x", point.x());
                pointEntry.addProperty("z", point.z());
                points.add(pointEntry);
            }
            entry.add("points", points);
            plannerRegions.add(entry);
        }
        body.add("plannerRegions", plannerRegions);

        JsonArray plannerDrafts = new JsonArray();
        List<PlannerDraft> drafts = new ArrayList<>(PLANNER_DRAFTS.values());
        drafts.sort(Comparator.comparing(PlannerDraft::ownerName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(draft -> draft.ownerUuid().toString()));
        for (PlannerDraft draft : drafts) {
            if (draft.points().isEmpty()) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("ownerUuid", draft.ownerUuid().toString());
            entry.addProperty("ownerName", draft.ownerName());
            entry.addProperty("dimensionId", draft.dimensionId());
            entry.addProperty("color", draft.color());
            JsonArray points = new JsonArray();
            for (RegionGeometry.Point point : draft.points()) {
                JsonObject pointEntry = new JsonObject();
                pointEntry.addProperty("x", point.x());
                pointEntry.addProperty("z", point.z());
                points.add(pointEntry);
            }
            entry.add("points", points);
            JsonArray warnings = new JsonArray();
            for (Integer warningIndex : draft.warningSegments()) {
                warnings.add(warningIndex);
            }
            entry.add("warningSegments", warnings);
            plannerDrafts.add(entry);
        }
        body.add("plannerDrafts", plannerDrafts);

        JsonArray players = new JsonArray();
        if (snapshot.region() != null) {
            MapDataStore.MapRegionData region = snapshot.region();
            String selfUuid = recipient.getUuidAsString();
            for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
                if (!online.getCommandSource().getWorld().getRegistryKey().getValue().toString().equals(region.dimensionId())) {
                    continue;
                }
                BlockPos onlinePos = online.getBlockPos();
                if (!region.containsHorizontal(onlinePos.getX(), onlinePos.getZ())) {
                    continue;
                }
                JsonObject entry = new JsonObject();
                entry.addProperty("uuid", online.getUuidAsString());
                entry.addProperty("name", online.getName().getString());
                entry.addProperty("x", online.getX());
                entry.addProperty("y", online.getY());
                entry.addProperty("z", online.getZ());
                entry.addProperty("self", selfUuid.equals(online.getUuidAsString()));
                players.add(entry);
            }
        }
        body.add("players", players);

        if (includeSnapshot && snapshot.region() != null) {
            MapImageData imageData = buildSnapshotImage(server, snapshot.region());
            body.addProperty("imageWidth", imageData.width());
            body.addProperty("imageHeight", imageData.height());
            body.addProperty("colorsHex", imageData.colorsHex());
        }

        return body;
    }

    private static MapImageData buildSnapshotImage(MinecraftServer server, MapDataStore.MapRegionData region) {
        ServerWorld world = resolveWorld(server, region.dimensionId());
        if (world == null) {
            return new MapImageData(1, 1, "1d2230");
        }

        int regionWidth = Math.max(1, region.width());
        int regionHeight = Math.max(1, region.depth());
        double scale = Math.max(1.0D, Math.max(regionWidth, regionHeight) / (double) MAX_SNAPSHOT_SIZE);
        int imageWidth = Math.max(1, (int) Math.ceil(regionWidth / scale));
        int imageHeight = Math.max(1, (int) Math.ceil(regionHeight / scale));
        StringBuilder colors = new StringBuilder(imageWidth * imageHeight * 6);

        BlockPos.Mutable samplePos = new BlockPos.Mutable();
        for (int imageZ = 0; imageZ < imageHeight; imageZ++) {
            int previousHeight = Integer.MIN_VALUE;
            double worldZ = region.minZ() + (imageZ + 0.5D) * scale;
            int sampleZ = clampToRange((int) Math.floor(worldZ), region.minZ(), region.maxZ());
            for (int imageX = 0; imageX < imageWidth; imageX++) {
                double worldX = region.minX() + (imageX + 0.5D) * scale;
                int sampleX = clampToRange((int) Math.floor(worldX), region.minX(), region.maxX());
                SurfaceSample sample = sampleTopColor(world, samplePos, sampleX, sampleZ);
                float shade = 1.0F;
                if (previousHeight != Integer.MIN_VALUE) {
                    if (sample.topY() > previousHeight) {
                        shade = 1.12F;
                    } else if (sample.topY() < previousHeight) {
                        shade = 0.86F;
                    }
                }
                previousHeight = sample.topY();
                int color = shadeColor(sample.color(), shade);
                colors.append(String.format(Locale.ROOT, "%06x", color & 0xFFFFFF));
            }
        }

        return new MapImageData(imageWidth, imageHeight, colors.toString());
    }

    private static SurfaceSample sampleTopColor(ServerWorld world, BlockPos.Mutable pos, int x, int z) {
        int sampleTopY = Math.max(world.getBottomY(), world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1);
        for (int y = sampleTopY; y >= world.getBottomY(); y--) {
            pos.set(x, y, z);
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            MapColor mapColor = state.getMapColor(world, pos);
            int color = mapColor == null ? 0x5A6573 : mapColor.color;
            if (color == 0) {
                continue;
            }
            return new SurfaceSample(y, color);
        }
        return new SurfaceSample(world.getBottomY(), 0x1D2230);
    }

    private static void tickPlayerRegions(MinecraftServer server) {
        MapDataStore.MapSnapshot snapshot = MapDataStore.getSnapshot(server);
        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            online.add(playerId);
            String dimensionId = player.getCommandSource().getWorld().getRegistryKey().getValue().toString();
            int blockX = player.getBlockX();
            int blockZ = player.getBlockZ();
            PlayerRegionTracker tracker = REGION_TRACKERS.computeIfAbsent(playerId, ignored -> new PlayerRegionTracker(dimensionId, blockX, blockZ, ""));
            if (tracker.lastX() == blockX && tracker.lastZ() == blockZ && tracker.dimensionId().equals(dimensionId)) {
                continue;
            }

            MapDataStore.PlannerRegionData region = MapDataStore.findPlannerRegionContaining(server, dimensionId, player.getX(), player.getZ());
            String regionId = region == null ? "" : region.id();
            if (!regionId.equals(tracker.currentRegionId())) {
                tracker.setCurrentRegionId(regionId);
                if (region != null && snapshot.settings().showRegionTitles()) {
                    sendRegionTitle(player, region.name(), region.color());
                }
            }
            tracker.setDimensionId(dimensionId);
            tracker.setLastX(blockX);
            tracker.setLastZ(blockZ);
        }

        REGION_TRACKERS.keySet().removeIf(playerId -> !online.contains(playerId));
        PLANNER_DRAFTS.keySet().removeIf(playerId -> !online.contains(playerId));
    }

    private static PlannerDraft getOrCreateDraft(ServerPlayerEntity player, String dimensionId) {
        UUID playerId = player.getUuid();
        PlannerDraft draft = PLANNER_DRAFTS.get(playerId);
        if (draft == null || !draft.dimensionId().equals(dimensionId)) {
            draft = new PlannerDraft(playerId, player.getName().getString(), dimensionId, defaultDraftColor(player));
            PLANNER_DRAFTS.put(playerId, draft);
            return draft;
        }
        draft.setOwnerName(player.getName().getString());
        return draft;
    }

    private static int defaultDraftColor(ServerPlayerEntity player) {
        return MapColors.paletteColor(Math.floorMod(player.getUuid().hashCode(), MapColors.MARKER_PALETTE.length));
    }

    private static void refreshDraftWarnings(MinecraftServer server, PlannerDraft draft) {
        draft.warningSegments().clear();
        for (int index = 0; index < draft.points().size() - 1; index++) {
            RegionGeometry.Point start = draft.points().get(index);
            RegionGeometry.Point end = draft.points().get(index + 1);
            if (segmentTouchesExistingRegion(server, draft.dimensionId(), start, end)) {
                draft.warningSegments().add(index);
            }
        }
    }

    private static boolean segmentTouchesExistingRegion(MinecraftServer server, String dimensionId, RegionGeometry.Point start, RegionGeometry.Point end) {
        for (MapDataStore.PlannerRegionData region : MapDataStore.getSnapshot(server).plannerRegions()) {
            if (!dimensionId.equals(region.dimensionId())) {
                continue;
            }
            if (RegionGeometry.segmentTouchesPolygon(start, end, region.points())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFirstDraftPoint(PlannerDraft draft, RegionGeometry.Point point) {
        RegionGeometry.Point firstPoint = draft.points().get(0);
        return firstPoint.x() == point.x() && firstPoint.z() == point.z();
    }

    private static Text describePlannerFailure(MapDataStore.PlannerRegionStatus status) {
        return switch (status) {
            case TOO_FEW_POINTS -> Text.literal("至少需要三个角点才能形成区域。");
            case INVALID_SHAPE -> Text.literal("当前角点形成了无效区域，请避免重复点或自交线。");
            case OVERLAP -> Text.literal("新区域与已有区域重叠，两个区域不得拥有重复的地方。");
            case DUPLICATE_NAME -> Text.literal("区域名称已存在，请使用其他名称。");
            case INVALID_NAME -> Text.literal("区域名称不能为空。");
            case NOT_FOUND -> Text.literal("未找到对应的区域。");
            case OK -> Text.empty();
        };
    }

    private static void clearPlayerState(UUID playerId) {
        PLANNER_DRAFTS.remove(playerId);
        REGION_TRACKERS.remove(playerId);
    }

    private static void sendRegionTitle(ServerPlayerEntity player, String regionName, int color) {
        JsonObject body = new JsonObject();
        body.addProperty("name", regionName);
        body.addProperty("color", color);
        ServerPlayNetworking.send(NullSafety.requireNonNull(player), new MapS2CPayload("region_title", body.toString()));
    }

    private static void teleportPlayer(ServerPlayerEntity player, double targetX, double targetY, double targetZ) {
        try {
            player.networkHandler.requestTeleport(targetX, targetY, targetZ, player.getYaw(), player.getPitch());
        } catch (Throwable ignored) {
            player.refreshPositionAndAngles(targetX, targetY, targetZ, player.getYaw(), player.getPitch());
        }
    }

    private static int shadeColor(int rgb, float multiplier) {
        int red = Math.min(255, Math.max(0, Math.round(((rgb >> 16) & 0xFF) * multiplier)));
        int green = Math.min(255, Math.max(0, Math.round(((rgb >> 8) & 0xFF) * multiplier)));
        int blue = Math.min(255, Math.max(0, Math.round((rgb & 0xFF) * multiplier)));
        return (red << 16) | (green << 8) | blue;
    }

    private static int clampToRange(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ServerWorld resolveWorld(MinecraftServer server, String dimensionId) {
        Identifier id = Identifier.tryParse(dimensionId);
        if (id == null) {
            return server.getOverworld();
        }
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, id);
        ServerWorld world = server.getWorld(key);
        return world == null ? server.getOverworld() : world;
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

    private static void sendError(ServerPlayerEntity player, String message) {
        JsonObject body = new JsonObject();
        body.addProperty("message", message);
        ServerPlayNetworking.send(NullSafety.requireNonNull(player), new MapS2CPayload("error", body.toString()));
    }

    private static String formatCornerName(String base, int pointIndex) {
        return base + (pointIndex + 1);
    }

    public record MapImageData(int width, int height, String colorsHex) {
    }

    private record SurfaceSample(int topY, int color) {
    }

    private static final class PlannerDraft {
        private final UUID ownerUuid;
        private String ownerName;
        private final String dimensionId;
        private int color;
        private final List<RegionGeometry.Point> points = new ArrayList<>();
        private final List<Integer> warningSegments = new ArrayList<>();

        private PlannerDraft(UUID ownerUuid, String ownerName, String dimensionId, int color) {
            this.ownerUuid = ownerUuid;
            this.ownerName = ownerName;
            this.dimensionId = dimensionId;
            this.color = color;
        }

        private UUID ownerUuid() {
            return ownerUuid;
        }

        private String ownerName() {
            return ownerName;
        }

        private void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }

        private String dimensionId() {
            return dimensionId;
        }

        private int color() {
            return color;
        }

        private void setColor(int color) {
            this.color = color;
        }

        private List<RegionGeometry.Point> points() {
            return points;
        }

        private List<Integer> warningSegments() {
            return warningSegments;
        }
    }

    private static final class PlayerRegionTracker {
        private String dimensionId;
        private int lastX;
        private int lastZ;
        private String currentRegionId;

        private PlayerRegionTracker(String dimensionId, int lastX, int lastZ, String currentRegionId) {
            this.dimensionId = dimensionId;
            this.lastX = lastX;
            this.lastZ = lastZ;
            this.currentRegionId = currentRegionId;
        }

        private String dimensionId() {
            return dimensionId;
        }

        private void setDimensionId(String dimensionId) {
            this.dimensionId = dimensionId;
        }

        private int lastX() {
            return lastX;
        }

        private void setLastX(int lastX) {
            this.lastX = lastX;
        }

        private int lastZ() {
            return lastZ;
        }

        private void setLastZ(int lastZ) {
            this.lastZ = lastZ;
        }

        private String currentRegionId() {
            return currentRegionId;
        }

        private void setCurrentRegionId(String currentRegionId) {
            this.currentRegionId = currentRegionId;
        }
    }
}