package com.zcpu.tzzmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zcpu.tzzmod.Tzz_mod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.WeakHashMap;

public final class PhotoSpeedConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<MinecraftServer, PhotoSpeedConfig> CACHE = new WeakHashMap<>();

    public double uploadBandwidthMbps = 5.0D;
    public double downloadBandwidthMbps = 5.0D;

    private PhotoSpeedConfig() {
    }

    public static synchronized PhotoSpeedConfig get(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, PhotoSpeedConfig::load);
    }

    public static synchronized void clearCache(MinecraftServer server) {
        CACHE.remove(server);
    }

    public static Path getConfigPath(MinecraftServer server) {
        return server.getRunDirectory().resolve("config").resolve("photospeed.json");
    }

    private static PhotoSpeedConfig load(MinecraftServer server) {
        Path configPath = getConfigPath(server);
        try {
            Files.createDirectories(configPath.getParent());

            PhotoSpeedConfig config = null;
            if (Files.exists(configPath)) {
                try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                    config = GSON.fromJson(reader, PhotoSpeedConfig.class);
                }
            }

            if (config == null) {
                config = migrateLegacyValues(server);
            }

            config.sanitize();
            config.write(configPath);
            return config;
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load photo speed config: {}", exception.getMessage());
            PhotoSpeedConfig fallback = new PhotoSpeedConfig();
            fallback.sanitize();
            return fallback;
        }
    }

    private static PhotoSpeedConfig migrateLegacyValues(MinecraftServer server) {
        PhotoSpeedConfig config = new PhotoSpeedConfig();
        readLegacyValues(server.getRunDirectory().resolve("config").resolve("tzz_mod-phone-chat.json"),
                "imageUploadBandwidthMbps", "imageDownloadBandwidthMbps", config);
        readLegacyValues(server.getRunDirectory().resolve("tzzserverphotos").resolve("config.json"),
                "uploadBandwidthMbps", "downloadBandwidthMbps", config);
        readLegacyValues(server.getSavePath(WorldSavePath.ROOT).resolve("tzz_mod").resolve("gallery").resolve("config.json"),
                "uploadBandwidthMbps", "downloadBandwidthMbps", config);
        return config;
    }

    private static void readLegacyValues(Path path, String uploadKey, String downloadKey, PhotoSpeedConfig config) {
        if (!Files.exists(path)) {
            return;
        }

        try {
            JsonObject object = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            if (object.has(uploadKey)) {
                config.uploadBandwidthMbps = object.get(uploadKey).getAsDouble();
            }
            if (object.has(downloadKey)) {
                config.downloadBandwidthMbps = object.get(downloadKey).getAsDouble();
            }
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to migrate legacy photo speed values from {}: {}", path, exception.getMessage());
        }
    }

    private void write(Path path) throws Exception {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        }
    }

    private void sanitize() {
        if (uploadBandwidthMbps < 0.25D) {
            uploadBandwidthMbps = 0.25D;
        }
        if (uploadBandwidthMbps > 20.0D) {
            uploadBandwidthMbps = 20.0D;
        }
        if (downloadBandwidthMbps < 0.25D) {
            downloadBandwidthMbps = 0.25D;
        }
        if (downloadBandwidthMbps > 20.0D) {
            downloadBandwidthMbps = 20.0D;
        }
    }
}