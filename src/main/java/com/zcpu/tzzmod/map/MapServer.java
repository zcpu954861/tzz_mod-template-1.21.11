package com.zcpu.tzzmod.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.network.MapC2SPayload;
import com.zcpu.tzzmod.network.MapS2CPayload;
import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.Locale;

public final class MapServer {
    private static final int MAX_SNAPSHOT_SIZE = 64;

    private MapServer() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> sendStateToPlayer(server, handler.getPlayer()))
        );

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

        double targetX = marker.x() + 0.5D;
        double targetY = marker.y() + 1.0D;
        double targetZ = marker.z() + 0.5D;
        try {
            player.networkHandler.requestTeleport(targetX, targetY, targetZ, player.getYaw(), player.getPitch());
        } catch (Throwable ignored) {
            player.refreshPositionAndAngles(targetX, targetY, targetZ, player.getYaw(), player.getPitch());
        }
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

        JsonArray players = new JsonArray();
        if (snapshot.region() != null) {
            MapDataStore.MapRegionData region = snapshot.region();
            String selfUuid = recipient.getUuidAsString();
            for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
                if (!online.getCommandSource().getWorld().getRegistryKey().getValue().toString().equals(region.dimensionId())) {
                    continue;
                }
                BlockPos pos = online.getBlockPos();
                if (!region.containsHorizontal(pos.getX(), pos.getZ())) {
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

    public record MapImageData(int width, int height, String colorsHex) {
    }

    private record SurfaceSample(int topY, int color) {
    }
}