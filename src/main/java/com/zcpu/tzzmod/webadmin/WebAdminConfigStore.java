package com.zcpu.tzzmod.webadmin;

import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;

public final class WebAdminConfigStore {
    private WebAdminConfigStore() {
    }

    public static Path path(MinecraftServer server) {
        return server.getRunDirectory().resolve("config").resolve("tzz").resolve("web_admin_config.json");
    }

    public static WebAdminConfig load(MinecraftServer server) {
        WebAdminConfig config = JsonStoreSupport.readOrDefault(
                path(server),
                WebAdminConfig.class,
                WebAdminConfig::new,
                "web admin config"
        );
        config.normalized();
        JsonStoreSupport.write(path(server), config, "web admin config");
        return config;
    }
}
