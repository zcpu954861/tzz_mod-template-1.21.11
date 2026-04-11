package com.zcpu.tzzmod.phone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zcpu.tzzmod.Tzz_mod;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class PhoneAppsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<MinecraftServer, PhoneAppsConfig> CACHE = new WeakHashMap<>();

    public Map<String, String> apps = new LinkedHashMap<>();

    private PhoneAppsConfig() {
    }

    public static synchronized PhoneAppsConfig get(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, PhoneAppsConfig::load);
    }

    private static PhoneAppsConfig load(MinecraftServer server) {
        Path configPath = server.getRunDirectory().resolve("config").resolve("tzz_mod-phoneapps.json");
        try {
            Files.createDirectories(configPath.getParent());
            if (Files.exists(configPath)) {
                try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                    PhoneAppsConfig loaded = GSON.fromJson(reader, PhoneAppsConfig.class);
                    if (loaded != null) {
                        loaded.sanitize();
                        return loaded;
                    }
                }
            }

            PhoneAppsConfig defaults = defaults();
            defaults.write(configPath);
            return defaults;
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load phone apps config: {}", exception.getMessage());
            PhoneAppsConfig fallback = defaults();
            fallback.sanitize();
            return fallback;
        }
    }

    private static PhoneAppsConfig defaults() {
        PhoneAppsConfig cfg = new PhoneAppsConfig();
        // Use LinkedHashMap to preserve a stable ordering
        cfg.apps.put("map", "true");
        cfg.apps.put("settings", "true");
        cfg.apps.put("chat", "true");
        cfg.apps.put("task", "true");
        cfg.apps.put("call_admin", "true");
        cfg.apps.put("compass", "true");
        cfg.apps.put("admin", "op");
        return cfg;
    }

    private void write(Path configPath) throws IOException {
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        }
    }

    private void sanitize() {
        if (apps == null) apps = new LinkedHashMap<>();
        // sanitize values to accepted set {"true","false","op"}
        apps.replaceAll((k, v) -> {
            if (v == null) return "true";
            String s = v.trim().toLowerCase();
            return switch (s) {
                case "true" -> "true";
                case "false" -> "false";
                case "op" -> "op";
                default -> "true";
            };
        });
    }
}

