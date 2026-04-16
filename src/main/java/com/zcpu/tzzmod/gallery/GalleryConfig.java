package com.zcpu.tzzmod.gallery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.config.PhotoSpeedConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server-side gallery configuration.
 * Stored in <server root>/tzzserverphotos/config.json
 * Transfer rate is stored in <server root>/config/photospeed.json
 */
public class GalleryConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GalleryConfig cached;
    private static Path cachedPath;

    public boolean enabled = true;
    public transient double uploadBandwidthMbps = 5.0;
    public transient double downloadBandwidthMbps = 5.0;

    public static GalleryConfig get(MinecraftServer server) {
        Path configPath = server.getRunDirectory().resolve("tzzserverphotos").resolve("config.json");
        migrateLegacyConfigIfNeeded(server, configPath);
        if (cached != null && configPath.equals(cachedPath)) {
            return cached;
        }
        cached = load(configPath);
        PhotoSpeedConfig speedConfig = PhotoSpeedConfig.get(server);
        cached.uploadBandwidthMbps = speedConfig.uploadBandwidthMbps;
        cached.downloadBandwidthMbps = speedConfig.downloadBandwidthMbps;
        cachedPath = configPath;
        return cached;
    }

    public static void clearCache() {
        cached = null;
        cachedPath = null;
    }

    private static GalleryConfig load(Path path) {
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                GalleryConfig config = GSON.fromJson(json, GalleryConfig.class);
                if (config != null) return config;
            } catch (Exception e) {
                Tzz_mod.LOGGER.warn("Failed to read gallery config: {}", e.getMessage());
            }
        }
        GalleryConfig defaults = new GalleryConfig();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(defaults), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Tzz_mod.LOGGER.warn("Failed to write default gallery config: {}", e.getMessage());
        }
        return defaults;
    }

    private static void migrateLegacyConfigIfNeeded(MinecraftServer server, Path configPath) {
        if (Files.exists(configPath)) {
            return;
        }

        Path legacyConfigPath = server.getSavePath(WorldSavePath.ROOT)
                .resolve("tzz_mod").resolve("gallery").resolve("config.json");
        if (!Files.exists(legacyConfigPath)) {
            return;
        }

        try {
            Files.createDirectories(configPath.getParent());
            Files.copy(legacyConfigPath, configPath);
        } catch (IOException e) {
            Tzz_mod.LOGGER.warn("Failed to migrate legacy gallery config: {}", e.getMessage());
        }
    }
}
