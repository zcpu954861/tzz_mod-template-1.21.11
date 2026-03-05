package com.zcpu.tzzmod.phone.chat;

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
import java.util.Map;
import java.util.WeakHashMap;

public final class PhoneChatConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<MinecraftServer, PhoneChatConfig> CACHE = new WeakHashMap<>();

    public boolean enabled = true;
    public int maxMessageLength = 25600;
    public int maxHistoryPerConversation = 120;
    public String notificationSound = "minecraft:entity.experience_orb.pickup";

    private PhoneChatConfig() {
    }

    public static synchronized PhoneChatConfig get(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, PhoneChatConfig::load);
    }

    private static PhoneChatConfig load(MinecraftServer server) {
        Path configPath = server.getRunDirectory().resolve("config").resolve("tzz_mod-phone-chat.json");
        try {
            Files.createDirectories(configPath.getParent());
            if (Files.exists(configPath)) {
                try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                    PhoneChatConfig loaded = GSON.fromJson(reader, PhoneChatConfig.class);
                    if (loaded != null) {
                        loaded.sanitize();
                        return loaded;
                    }
                }
            }

            PhoneChatConfig defaults = new PhoneChatConfig();
            defaults.write(configPath);
            return defaults;
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("Failed to load phone chat config: {}", exception.getMessage());
            PhoneChatConfig fallback = new PhoneChatConfig();
            fallback.sanitize();
            return fallback;
        }
    }

    private void write(Path configPath) throws IOException {
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        }
    }

    private void sanitize() {
        if (maxMessageLength < 16) {
            maxMessageLength = 16;
        }
        if (maxMessageLength > 25600) {
            maxMessageLength = 25600;
        }
        if (maxHistoryPerConversation < 20) {
            maxHistoryPerConversation = 20;
        }
        if (maxHistoryPerConversation > 1000) {
            maxHistoryPerConversation = 1000;
        }
        if (notificationSound == null || notificationSound.isBlank()) {
            notificationSound = "minecraft:entity.experience_orb.pickup";
        }
    }
}
