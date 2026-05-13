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

    public record PasswordUpdateResult(boolean success, String message, WebAdminUser user, boolean changed) {
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

    public synchronized PasswordUpdateResult changeOwnPassword(String username, String oldPassword, String newPassword) {
        reload();
        Optional<WebAdminUser> optional = findInternal(normalizeUsername(username));
        if (optional.isEmpty()) {
            return new PasswordUpdateResult(false, "找不到当前 WebAdmin 用户。", null, false);
        }
        WebAdminUser user = optional.get();
        if (!WebAdminPasswordHasher.verify(oldPassword, user)) {
            WebAdminAuditLogger.userChanged("changePasswordFailed", user.username, user.username);
            return new PasswordUpdateResult(false, "旧密码不正确。", user, false);
        }
        PasswordUpdateResult validation = validateNewPassword(user, newPassword);
        if (!validation.success()) {
            return validation;
        }
        if (WebAdminPasswordHasher.verify(newPassword, user)) {
            return new PasswordUpdateResult(false, "新密码不能与当前密码相同。", user, false);
        }
        applyPassword(user, newPassword, false);
        save();
        WebAdminAuditLogger.userChanged("changePassword", user.username, user.username);
        return new PasswordUpdateResult(true, "密码已修改。", user, true);
    }

    public synchronized PasswordUpdateResult setPassword(String username, String newPassword, String actor) {
        reload();
        Optional<WebAdminUser> optional = findInternal(normalizeUsername(username));
        if (optional.isEmpty()) {
            return new PasswordUpdateResult(false, "找不到该 WebAdmin 用户。", null, false);
        }
        WebAdminUser user = optional.get();
        PasswordUpdateResult validation = validateNewPassword(user, newPassword);
        if (!validation.success()) {
            return validation;
        }
        boolean changed = !WebAdminPasswordHasher.verify(newPassword, user);
        if (!changed) {
            return new PasswordUpdateResult(true, "密码未变化。", user, false);
        }
        applyPassword(user, newPassword, false);
        save();
        WebAdminAuditLogger.userChanged("setPassword", user.username, actor);
        return new PasswordUpdateResult(true, "已更新 WebAdmin 用户密码。", user, true);
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

    private static PasswordUpdateResult validateNewPassword(WebAdminUser user, String newPassword) {
        String password = newPassword == null ? "" : newPassword;
        if (password.length() < 10) {
            return new PasswordUpdateResult(false, "新密码至少需要 10 个字符。", user, false);
        }
        if (password.length() > 128) {
            return new PasswordUpdateResult(false, "新密码不能超过 128 个字符。", user, false);
        }
        String username = user == null ? "" : user.username;
        if (!username.isBlank() && password.equalsIgnoreCase(username)) {
            return new PasswordUpdateResult(false, "新密码不能与用户名相同。", user, false);
        }
        return new PasswordUpdateResult(true, "", user, false);
    }

    private static void applyPassword(WebAdminUser user, String password, boolean forcePasswordChange) {
        WebAdminPasswordHasher.PasswordHash hash = WebAdminPasswordHasher.hash(password);
        user.passwordSalt = hash.salt();
        user.passwordHash = hash.hash();
        user.passwordAlgorithm = hash.algorithm();
        user.passwordIterations = hash.iterations();
        user.forcePasswordChange = forcePasswordChange;
        user.failedLoginCount = 0;
        user.lockedUntil = 0L;
        user.normalized();
    }
}
