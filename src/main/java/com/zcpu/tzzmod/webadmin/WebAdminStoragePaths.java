package com.zcpu.tzzmod.webadmin;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

public record WebAdminStoragePaths(
        Path directory,
        Path configPath,
        Path usersPath,
        Path auditLogPath,
        Path legacyGlobalDirectory,
        Path legacyGlobalConfigPath,
        Path legacyGlobalUsersPath,
        Path legacyGlobalAuditLogPath
) {
    public static final String STORAGE_SCOPE = "WORLD_SAVE";

    public static WebAdminStoragePaths resolve(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("MinecraftServer is required for WebAdmin world-scoped storage.");
        }
        Path directory = server.getSavePath(WorldSavePath.ROOT).resolve("tzz").resolve("webadmin");
        Path legacyDirectory = server.getRunDirectory().resolve("config").resolve("tzz");
        return new WebAdminStoragePaths(
                directory,
                directory.resolve("web_admin_config.json"),
                directory.resolve("web_admin_users.json"),
                directory.resolve("web_admin_audit.log"),
                legacyDirectory,
                legacyDirectory.resolve("web_admin_config.json"),
                legacyDirectory.resolve("web_admin_users.json"),
                legacyDirectory.resolve("web_admin_audit.log")
        );
    }

    public void ensureDirectory() {
        try {
            Files.createDirectories(directory);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create WebAdmin storage directory: " + directory, exception);
        }
    }

    public boolean hasLegacyGlobalFiles() {
        return Files.exists(legacyGlobalConfigPath)
                || Files.exists(legacyGlobalUsersPath)
                || Files.exists(legacyGlobalAuditLogPath);
    }
}
