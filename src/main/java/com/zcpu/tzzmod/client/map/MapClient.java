package com.zcpu.tzzmod.client.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.ModItem.ModItems;
import com.zcpu.tzzmod.network.MapC2SPayload;
import com.zcpu.tzzmod.network.MapS2CPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class MapClient {
    private static final Set<Runnable> LISTENERS = new CopyOnWriteArraySet<>();
    private static MapState state = MapState.empty();

    private MapClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MapS2CPayload.ID, (payload, context) ->
                context.client().execute(() -> handlePayload(context.client(), payload))
        );

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            state = MapState.empty();
            MapCanvasRenderer.reset();
            notifyListeners();
        });

        ClientTickEvents.END_CLIENT_TICK.register(MapClient::spawnMarkerParticles);
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
                getBoolean(settingsObject, "showOtherPlayers", true)
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

        state = new MapState(hasRegion, region, settings, List.copyOf(markers), List.copyOf(players), imageWidth, imageHeight, imageColors, imageHash, regionVersion);
        notifyListeners();
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
        if (client.player == null) {
            return;
        }
        String message = getString(body, "message");
        if (!message.isBlank()) {
            client.player.sendMessage(Text.literal("[Map] " + message), false);
        }
    }

    private static void spawnMarkerParticles(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }
        if (client.world.getTime() % 2L != 0L) {
            return;
        }
        if (!client.player.getMainHandStack().isOf(ModItems.MAP_MARKER) && !client.player.getOffHandStack().isOf(ModItems.MAP_MARKER)) {
            return;
        }
        String dimensionId = client.world.getRegistryKey().getValue().toString();
        long worldTime = client.world.getTime();
        for (int index = 0; index < state.markers().size(); index++) {
            MapMarker marker = state.markers().get(index);
            if (!dimensionId.equals(marker.dimensionId())) {
                continue;
            }
            double dx = marker.x() + 0.5D - client.player.getX();
            double dy = marker.y() + 0.5D - client.player.getY();
            double dz = marker.z() + 0.5D - client.player.getZ();
            if (dx * dx + dy * dy + dz * dz > 256.0D * 256.0D) {
                continue;
            }
            for (int step = 0; step < 8; step++) {
                float hue = ((worldTime * 4L + index * 18L + step * 24L) % 360L) / 360.0F;
                int rgb = hsvToRgb(hue, 0.85F, 1.0F);
                float red = ((rgb >> 16) & 0xFF) / 255.0F;
                float green = ((rgb >> 8) & 0xFF) / 255.0F;
                float blue = (rgb & 0xFF) / 255.0F;
                double particleY = marker.y() + 0.35D + step * 1.4D;
                client.particleManager.addParticle(
                    new DustParticleEffect((Math.round(red * 255.0F) << 16) | (Math.round(green * 255.0F) << 8) | Math.round(blue * 255.0F), 1.2F),
                    marker.x() + 0.5D,
                    particleY,
                    marker.z() + 0.5D,
                    0.0D,
                    0.02D,
                    0.0D
                );
            }
        }
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
            state = new MapState(state.hasRegion(), state.region(), state.settings(), List.copyOf(updated), state.players(), state.imageWidth(), state.imageHeight(), state.imageColors(), state.imageHash(), state.regionVersion());
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
            state = new MapState(state.hasRegion(), state.region(), state.settings(), List.copyOf(updated), state.players(), state.imageWidth(), state.imageHeight(), state.imageColors(), state.imageHash(), state.regionVersion());
            notifyListeners();
            return;
        }
    }

    private static void updateLocalVisibility(String key, boolean enabled) {
        MapSettings settings = switch (key) {
            case "show_self_position" -> new MapSettings(enabled, state.settings().showMarkers(), state.settings().showOtherPlayers());
            case "show_markers" -> new MapSettings(state.settings().showSelfPosition(), enabled, state.settings().showOtherPlayers());
            case "show_other_players" -> new MapSettings(state.settings().showSelfPosition(), state.settings().showMarkers(), enabled);
            default -> state.settings();
        };
        state = new MapState(state.hasRegion(), state.region(), settings, state.markers(), state.players(), state.imageWidth(), state.imageHeight(), state.imageColors(), state.imageHash(), state.regionVersion());
        notifyListeners();
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

    public record MapSettings(boolean showSelfPosition, boolean showMarkers, boolean showOtherPlayers) {
    }

    public record MapMarker(String id, String name, String dimensionId, int x, int y, int z, int color) {
    }

    public record MapPlayer(String uuid, String name, double x, double y, double z, boolean self) {
    }

    public record MapState(boolean hasRegion, MapRegion region, MapSettings settings, List<MapMarker> markers, List<MapPlayer> players, int imageWidth, int imageHeight, int[] imageColors, int imageHash, int regionVersion) {
        public static MapState empty() {
            return new MapState(false, null, new MapSettings(true, true, true), List.of(), List.of(), 0, 0, new int[0], 0, 0);
        }
    }
}