package com.zcpu.tzzmod.webadmin;

import com.zcpu.tzzmod.core.storage.JsonStoreSupport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;

public final class WebAdminUserStore {
    public static final class UserFile {
        public List<WebAdminUser> users = new ArrayList<>();

        UserFile normalized() {
            if (users == null) {
                users = new ArrayList<>();
            }
            users.removeIf(user -> user == null || WebAdminUser.clean(user.username).isBlank());
            users.forEach(WebAdminUser::normalized);
            return this;
        }
    }

    private WebAdminUserStore() {
    }

    public static Path path(MinecraftServer server) {
        WebAdminStoragePaths paths = WebAdminStoragePaths.resolve(server);
        paths.ensureDirectory();
        return paths.usersPath();
    }

    public static synchronized UserFile load(MinecraftServer server) {
        UserFile file = JsonStoreSupport.readOrDefault(
                path(server),
                UserFile.class,
                UserFile::new,
                "web admin users"
        );
        file.normalized();
        JsonStoreSupport.write(path(server), file, "web admin users");
        return file;
    }

    public static synchronized boolean save(MinecraftServer server, UserFile file) {
        return JsonStoreSupport.write(path(server), file.normalized(), "web admin users");
    }
}
