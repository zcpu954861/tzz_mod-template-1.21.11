package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.webadmin.WebAdminAccessMode;
import com.zcpu.tzzmod.webadmin.WebAdminConfig;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSessionService;
import com.zcpu.tzzmod.webadmin.WebAdminStoragePaths;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.WebAdminUserService;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public final class WebAdminUserSettingsService {
    public WebAdminDtos.WebAdminUsersDto users(WebAdminUserService userService, WebAdminSessionService sessionService) {
        List<WebAdminUser> users = userService.listUsers();
        Map<String, Integer> sessionCounts = sessionService.sessionCountsByUsername();
        List<WebAdminDtos.WebAdminUserListEntryDto> entries = new ArrayList<>();
        int ownerCount = 0;
        int editorCount = 0;
        int testerCount = 0;
        int viewerCount = 0;
        int disabledCount = 0;
        int onlineCount = 0;

        for (WebAdminUser user : users) {
            WebAdminRole role = user.roleEnum();
            int sessions = sessionCounts.getOrDefault(user.username, 0);
            if (sessions > 0) {
                onlineCount++;
            }
            if (!user.enabled) {
                disabledCount++;
            }
            switch (role) {
                case OWNER -> ownerCount++;
                case EDITOR -> editorCount++;
                case TESTER -> testerCount++;
                case VIEWER -> viewerCount++;
            }
            entries.add(new WebAdminDtos.WebAdminUserListEntryDto(
                    user.username,
                    user.displayName,
                    role.id(),
                    role.displayName(),
                    user.enabled,
                    sessions > 0,
                    sessions,
                    isoTime(user.createdAt),
                    safe(user.createdBy),
                    isoTime(user.lastLoginAt),
                    user.forcePasswordChange
            ));
        }

        List<WebAdminDtos.WebAdminRoleSummaryDto> roles = List.of(
                new WebAdminDtos.WebAdminRoleSummaryDto(WebAdminRole.OWNER.id(), WebAdminRole.OWNER.displayName(), ownerCount),
                new WebAdminDtos.WebAdminRoleSummaryDto(WebAdminRole.EDITOR.id(), WebAdminRole.EDITOR.displayName(), editorCount),
                new WebAdminDtos.WebAdminRoleSummaryDto(WebAdminRole.TESTER.id(), WebAdminRole.TESTER.displayName(), testerCount),
                new WebAdminDtos.WebAdminRoleSummaryDto(WebAdminRole.VIEWER.id(), WebAdminRole.VIEWER.displayName(), viewerCount)
        );
        return new WebAdminDtos.WebAdminUsersDto(
                new WebAdminDtos.WebAdminUserSummaryDto(
                        users.size(),
                        onlineCount,
                        ownerCount,
                        editorCount,
                        testerCount,
                        viewerCount,
                        disabledCount
                ),
                List.copyOf(entries),
                roles
        );
    }

    public WebAdminDtos.WebAdminSettingsDto settings(
            MinecraftServer server,
            WebAdminConfig config,
            WebAdminSessionService sessionService,
            WebAdminUser currentUser
    ) {
        boolean ownerView = currentUser != null && currentUser.roleEnum() == WebAdminRole.OWNER;
        WebAdminStoragePaths paths = WebAdminStoragePaths.resolve(server);
        WebAdminAccessMode accessMode = config.accessModeEnum();

        Map<String, Object> service = new LinkedHashMap<>();
        service.put("running", true);
        service.put("host", config.host);
        service.put("port", config.port);
        service.put("accessMode", config.accessMode);
        service.put("accessModeDisplayName", accessMode.displayName());
        service.put("url", "http://" + config.host + ":" + config.port + "/");
        service.put("currentUser", currentUser == null ? "" : currentUser.username);
        service.put("currentRole", currentUser == null ? "" : currentUser.roleEnum().id());

        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("scope", WebAdminStoragePaths.STORAGE_SCOPE);
        storage.put("worldScoped", true);
        storage.put("directory", ownerView ? paths.directory().toString() : "");
        storage.put("configPath", ownerView ? paths.configPath().toString() : "");
        storage.put("usersPath", ownerView ? paths.usersPath().toString() : "");
        storage.put("auditLogPath", ownerView ? paths.auditLogPath().toString() : "");
        storage.put("configExists", Files.exists(paths.configPath()));
        storage.put("usersExists", Files.exists(paths.usersPath()));
        storage.put("auditLogExists", Files.exists(paths.auditLogPath()));
        storage.put("legacyGlobalFilesDetected", paths.hasLegacyGlobalFiles());
        storage.put("restricted", !ownerView);

        Map<String, Object> security = new LinkedHashMap<>();
        security.put("authMode", "USERNAME_PASSWORD");
        security.put("passwordHashAlgorithm", "PBKDF2WithHmacSHA256");
        security.put("sessionCookieName", WebAdminSessionService.COOKIE_NAME);
        security.put("sessionTtlMinutes", config.sessionTtlMinutes);
        security.put("rememberMeTtlMinutes", config.rememberMeTtlMinutes);
        security.put("auditEnabled", config.auditEnabled);
        security.put("secureCookie", config.secureCookie);
        security.put("accessMode", config.accessMode);
        security.put("remoteAccessAllowed", accessMode.needsSecurityWarning());

        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("enabled", config.auditEnabled);
        audit.put("auditLogExists", Files.exists(paths.auditLogPath()));
        audit.put("recentLoginRecords", "暂无数据");
        audit.put("apiAccessStats", "暂无数据");

        Map<String, Object> system = new LinkedHashMap<>();
        system.put("modVersion", modVersion());
        system.put("minecraftVersion", "1.21.11");
        system.put("serverType", server.isDedicated() ? "DEDICATED" : "INTEGRATED");
        system.put("worldName", server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).getFileName() == null
                ? ""
                : server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).getFileName().toString());
        system.put("sessionCount", sessionService.sessionCount());

        Map<String, Object> visibility = new LinkedHashMap<>();
        visibility.put("ownerView", ownerView);
        visibility.put("sensitiveStorageHidden", !ownerView);

        return new WebAdminDtos.WebAdminSettingsDto(service, storage, security, audit, system, visibility);
    }

    private static String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(Tzz_mod.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static String isoTime(long millis) {
        return millis <= 0L ? "" : WebAdminSessionService.formatInstant(millis);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
