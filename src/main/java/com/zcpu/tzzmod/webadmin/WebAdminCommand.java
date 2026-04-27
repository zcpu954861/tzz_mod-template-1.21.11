package com.zcpu.tzzmod.webadmin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.function.Supplier;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class WebAdminCommand {
    private WebAdminCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("webadmin")
                .requires(WebAdminCommand::canManage)
                .then(CommandManager.literal("status")
                        .executes(context -> executeStatus(context.getSource())))
                .then(CommandManager.literal("user")
                        .then(CommandManager.literal("list")
                                .executes(context -> executeUserList(context.getSource())))
                        .then(CommandManager.literal("create")
                                .then(CommandManager.argument("username", StringArgumentType.word())
                                        .then(CommandManager.argument("role", StringArgumentType.word())
                                                .executes(context -> executeUserCreate(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "username"),
                                                        StringArgumentType.getString(context, "role")
                                                )))))
                        .then(CommandManager.literal("disable")
                                .then(CommandManager.argument("username", StringArgumentType.word())
                                        .executes(context -> executeUserEnabled(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "username"),
                                                false
                                        ))))
                        .then(CommandManager.literal("enable")
                                .then(CommandManager.argument("username", StringArgumentType.word())
                                        .executes(context -> executeUserEnabled(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "username"),
                                                true
                                        ))))
                        .then(CommandManager.literal("resetPassword")
                                .then(CommandManager.argument("username", StringArgumentType.word())
                                        .executes(context -> executeResetPassword(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "username")
                                        )))));
    }

    private static int executeStatus(ServerCommandSource source) {
        if (source.getServer() == null) {
            return 0;
        }
        WebAdminLifecycle.WebAdminRuntimeStatus status = WebAdminLifecycle.status(source.getServer());
        WebAdminConfig config = status.config();
        sendHeader(source, Text.literal("WebAdmin 状态").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("启用状态", booleanText(config.enabled)), false);
        source.sendFeedback(() -> field("HTTP 服务", booleanText(status.running())), false);
        source.sendFeedback(() -> field("监听地址", Text.literal(config.host).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("端口", number(config.port)), false);
        source.sendFeedback(() -> field("访问模式", Text.literal(config.accessModeEnum().displayName()).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("访问 URL", Text.literal("http://" + config.host + ":" + config.port).formatted(Formatting.GREEN)), false);
        source.sendFeedback(() -> field("当前 session", number(status.sessionCount()).append(Text.literal(" 个").formatted(Formatting.GRAY))), false);
        source.sendFeedback(() -> field("用户总数", number(status.userCount()).append(Text.literal(" 个").formatted(Formatting.GRAY))), false);
        if (config.accessModeEnum().needsSecurityWarning()) {
            source.sendFeedback(() -> warning("警告：当前 WebAdmin 允许通过 IP:端口访问。请只向可信协作者提供账号，并避免在不受信网络中暴露端口。"), false);
        }
        if (!config.enabled) {
            source.sendFeedback(() -> warning("WebAdmin 默认关闭。请编辑 config/tzz/web_admin_config.json 并重启服务器后启用。"), false);
        }
        return 1;
    }

    private static int executeUserList(ServerCommandSource source) {
        if (source.getServer() == null) {
            return 0;
        }
        WebAdminUserService service = WebAdminLifecycle.userService(source.getServer());
        sendHeader(source, Text.literal("WebAdmin 用户列表").formatted(Formatting.GOLD));
        var users = service.listUsers();
        if (users.isEmpty()) {
            source.sendFeedback(() -> warning("暂无 WebAdmin 用户。请使用 /tzz webadmin user create <username> <role> 创建。"), false);
            return 0;
        }
        for (WebAdminUser user : users) {
            source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                    .append(Text.literal(user.displayName).formatted(Formatting.GOLD))
                    .append(Text.literal(" / ").formatted(Formatting.GRAY))
                    .append(Text.literal(user.roleEnum().displayName()).formatted(Formatting.AQUA))
                    .append(Text.literal(user.enabled ? " / 启用" : " / 禁用").formatted(user.enabled ? Formatting.GREEN : Formatting.RED)), false);
        }
        return users.size();
    }

    private static int executeUserCreate(ServerCommandSource source, String username, String rawRole) {
        if (source.getServer() == null) {
            return 0;
        }
        WebAdminRole role = WebAdminRole.parse(rawRole);
        if (role == null) {
            sendCommandFeedback(source, () -> error("角色无效。可用角色：VIEWER、TESTER、EDITOR、OWNER。"), false);
            return 0;
        }
        WebAdminUserService.CreateResult result = WebAdminLifecycle.userService(source.getServer())
                .createUser(username, role, actorName(source));
        if (!result.success()) {
            sendCommandFeedback(source, () -> error(result.message()), false);
            return 0;
        }
        sendHeader(source, Text.literal("已创建 WebAdmin 用户").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("用户", Text.literal(result.user().username).formatted(Formatting.GOLD)), false);
        source.sendFeedback(() -> field("角色", Text.literal(result.user().roleEnum().displayName()).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("初始密码", Text.literal(result.initialPassword()).formatted(Formatting.YELLOW)), false);
        source.sendFeedback(() -> warning("请立即保存此密码，并让协作者首次登录后修改密码。"), false);
        return 1;
    }

    private static int executeUserEnabled(ServerCommandSource source, String username, boolean enabled) {
        if (source.getServer() == null) {
            return 0;
        }
        boolean changed = WebAdminLifecycle.userService(source.getServer()).setEnabled(username, enabled, actorName(source));
        if (!changed) {
            sendCommandFeedback(source, () -> error("找不到该 WebAdmin 用户。"), false);
            return 0;
        }
        sendCommandFeedback(source, () -> Text.literal(enabled ? "已启用 WebAdmin 用户：" : "已禁用 WebAdmin 用户：")
                .formatted(enabled ? Formatting.GREEN : Formatting.YELLOW)
                .append(Text.literal(username).formatted(Formatting.GOLD)), false);
        return 1;
    }

    private static int executeResetPassword(ServerCommandSource source, String username) {
        if (source.getServer() == null) {
            return 0;
        }
        WebAdminUserService.CreateResult result = WebAdminLifecycle.userService(source.getServer())
                .resetPassword(username, actorName(source));
        if (!result.success()) {
            sendCommandFeedback(source, () -> error(result.message()), false);
            return 0;
        }
        sendHeader(source, Text.literal("已重置 WebAdmin 用户密码").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("用户", Text.literal(result.user().username).formatted(Formatting.GOLD)), false);
        source.sendFeedback(() -> field("初始密码", Text.literal(result.initialPassword()).formatted(Formatting.YELLOW)), false);
        source.sendFeedback(() -> warning("密码只显示这一次，请立即保存。"), false);
        return 1;
    }

    private static boolean canManage(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity)) {
            return true;
        }
        return source.getPermissions() instanceof LeveledPermissionPredicate leveled
                && leveled.getLevel().isAtLeast(PermissionLevel.OWNERS);
    }

    private static String actorName(ServerCommandSource source) {
        return source.getEntity() instanceof ServerPlayerEntity ? source.getName() : "console";
    }

    private static void sendDivider(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("===========").formatted(Formatting.AQUA), false);
    }

    private static void sendHeader(ServerCommandSource source, Text header) {
        sendDivider(source);
        source.sendFeedback(() -> header, false);
    }

    private static void sendCommandFeedback(ServerCommandSource source, Supplier<Text> feedback, boolean broadcastToOps) {
        sendDivider(source);
        source.sendFeedback(feedback, broadcastToOps);
    }

    private static MutableText field(String label, Text value) {
        return Text.literal(label + "：").formatted(Formatting.GRAY).append(value);
    }

    private static MutableText booleanText(boolean value) {
        return Text.literal(value ? "是" : "否").formatted(value ? Formatting.GREEN : Formatting.RED);
    }

    private static MutableText number(int value) {
        return Text.literal(Integer.toString(value)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText warning(String text) {
        return Text.literal(text).formatted(Formatting.YELLOW);
    }

    private static MutableText error(String text) {
        return Text.literal(text).formatted(Formatting.RED);
    }
}
