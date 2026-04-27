package com.zcpu.tzzmod.webadmin;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

public final class WebAdminUserService {
    public record CreateResult(boolean success, String message, WebAdminUser user, String initialPassword) {
    }

    public record AuthResult(boolean success, String message, WebAdminUser user) {
    }

    private final MinecraftServer server;
    private WebAdminUserStore.UserFile userFile;

    public WebAdminUserService(MinecraftServer server) {
        this.server = server;
        this.userFile = WebAdminUserStore.load(server);
    }

    public synchronized List<WebAdminUser> listUsers() {
        reload();
        return userFile.users.stream()
                .map(user -> user)
                .sorted(Comparator.comparing(user -> user.username.toLowerCase(Locale.ROOT)))
                .toList();
    }

    public synchronized Optional<WebAdminUser> find(String username) {
        reload();
        String normalized = normalizeUsername(username);
        return userFile.users.stream()
                .filter(user -> user.username.equalsIgnoreCase(normalized))
                .findFirst();
    }

    public synchronized CreateResult createUser(String username, WebAdminRole role, String createdBy) {
        reload();
        String normalized = normalizeUsername(username);
        if (!isValidUsername(normalized)) {
            return new CreateResult(false, "用户名只能包含字母、数字、下划线、短横线和点，长度 3 到 32。", null, "");
        }
        if (role == null) {
            return new CreateResult(false, "角色无效。", null, "");
        }
        if (findInternal(normalized).isPresent()) {
            return new CreateResult(false, "该 WebAdmin 用户已存在。", null, "");
        }

        String password = WebAdminPasswordHasher.generateInitialPassword();
        WebAdminPasswordHasher.PasswordHash hash = WebAdminPasswordHasher.hash(password);
        WebAdminUser user = new WebAdminUser();
        user.username = normalized;
        user.displayName = normalized;
        user.role = role.id();
        user.enabled = true;
        user.passwordSalt = hash.salt();
        user.passwordHash = hash.hash();
        user.passwordAlgorithm = hash.algorithm();
        user.passwordIterations = hash.iterations();
        user.createdAt = System.currentTimeMillis();
        user.createdBy = createdBy == null ? "" : createdBy;
        user.forcePasswordChange = true;
        user.normalized();
        userFile.users.add(user);
        save();
        WebAdminAuditLogger.userChanged("create", normalized, createdBy);
        return new CreateResult(true, "已创建 WebAdmin 用户。", user, password);
    }

    public synchronized CreateResult resetPassword(String username, String actor) {
        reload();
        Optional<WebAdminUser> optional = findInternal(normalizeUsername(username));
        if (optional.isEmpty()) {
            return new CreateResult(false, "找不到该 WebAdmin 用户。", null, "");
        }
        WebAdminUser user = optional.get();
        String password = WebAdminPasswordHasher.generateInitialPassword();
        WebAdminPasswordHasher.PasswordHash hash = WebAdminPasswordHasher.hash(password);
        user.passwordSalt = hash.salt();
        user.passwordHash = hash.hash();
        user.passwordAlgorithm = hash.algorithm();
        user.passwordIterations = hash.iterations();
        user.forcePasswordChange = true;
        user.failedLoginCount = 0;
        user.lockedUntil = 0L;
        save();
        WebAdminAuditLogger.userChanged("resetPassword", user.username, actor);
        return new CreateResult(true, "已重置 WebAdmin 用户密码。", user, password);
    }

    public synchronized boolean setEnabled(String username, boolean enabled, String actor) {
        reload();
        Optional<WebAdminUser> optional = findInternal(normalizeUsername(username));
        if (optional.isEmpty()) {
            return false;
        }
        optional.get().enabled = enabled;
        save();
        WebAdminAuditLogger.userChanged(enabled ? "enable" : "disable", optional.get().username, actor);
        return true;
    }

    public synchronized AuthResult authenticate(String username, String password) {
        reload();
        Optional<WebAdminUser> optional = findInternal(normalizeUsername(username));
        if (optional.isEmpty()) {
            WebAdminAuditLogger.login(false, normalizeUsername(username), "unknown_user");
            return new AuthResult(false, "用户名或密码错误。", null);
        }
        WebAdminUser user = optional.get();
        long now = System.currentTimeMillis();
        if (!user.enabled) {
            WebAdminAuditLogger.login(false, user.username, "disabled");
            return new AuthResult(false, "用户名或密码错误。", null);
        }
        if (user.lockedUntil > now) {
            WebAdminAuditLogger.login(false, user.username, "locked");
            return new AuthResult(false, "登录暂时被锁定，请稍后再试。", null);
        }
        if (!WebAdminPasswordHasher.verify(password, user)) {
            user.failedLoginCount++;
            if (user.failedLoginCount >= 8) {
                user.lockedUntil = now + 5 * 60_000L;
            }
            save();
            WebAdminAuditLogger.login(false, user.username, "bad_password");
            return new AuthResult(false, "用户名或密码错误。", null);
        }
        user.failedLoginCount = 0;
        user.lockedUntil = 0L;
        user.lastLoginAt = now;
        save();
        WebAdminAuditLogger.login(true, user.username, "");
        return new AuthResult(true, "登录成功。", user);
    }

    public synchronized int userCount() {
        reload();
        return userFile.users.size();
    }

    private Optional<WebAdminUser> findInternal(String username) {
        return userFile.users.stream()
                .filter(user -> user.username.equalsIgnoreCase(username))
                .findFirst();
    }

    private void reload() {
        userFile = WebAdminUserStore.load(server);
    }

    private void save() {
        WebAdminUserStore.save(server, userFile);
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isValidUsername(String username) {
        return username != null && username.matches("[a-z0-9_.-]{3,32}");
    }
}
