package com.zcpu.tzzmod.signal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionContext;
import com.zcpu.tzzmod.action.ActionEngine;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.action.ActionValidator;
import com.zcpu.tzzmod.command.CommandSuggestionUtil;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

public final class SignalCommand {
    private SignalCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("signal")
                .requires(source -> !(source.getEntity() instanceof ServerPlayerEntity player) || player.isCreativeLevelTwoOp())
                .then(CommandManager.literal("emit")
                        .then(channelArgument()
                                .executes(context -> executeEmit(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "channel")
                                ))))
                .then(CommandManager.literal("listen")
                        .then(CommandManager.literal("create")
                                .then(channelArgument()
                                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                                .executes(context -> executeCreate(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "channel"),
                                                        StringArgumentType.getString(context, "name")
                                                )))))
                        .then(CommandManager.literal("list")
                                .executes(context -> executeList(context.getSource())))
                        .then(CommandManager.literal("info")
                                .then(listenerTailArgument()
                                        .executes(context -> executeInfo(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "listener")
                                        ))))
                        .then(enableDisableDeleteCommand("enable"))
                        .then(enableDisableDeleteCommand("disable"))
                        .then(enableDisableDeleteCommand("delete"))
                        .then(CommandManager.literal("addAction")
                                .then(listenerArgument()
                                        .then(CommandManager.literal("command")
                                                .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                                        .executes(context -> executeAddCommandAction(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "listener"),
                                                                StringArgumentType.getString(context, "command")
                                                        ))))
                                        .then(CommandManager.literal("signal")
                                                .then(channelArgument()
                                                        .executes(context -> executeAddSignalAction(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "listener"),
                                                                StringArgumentType.getString(context, "channel")
                                                        ))))))
                        .then(CommandManager.literal("clearActions")
                                .then(listenerTailArgument()
                                        .executes(context -> executeClearActions(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "listener")
                                        ))))
                        .then(CommandManager.literal("cooldown")
                                .then(listenerArgument()
                                        .then(CommandManager.argument("ticks", IntegerArgumentType.integer())
                                                .executes(context -> executeCooldown(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "listener"),
                                                        IntegerArgumentType.getInteger(context, "ticks")
                                                )))))
                        .then(CommandManager.literal("test")
                                .then(listenerTailArgument()
                                        .executes(context -> executeTest(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "listener")
                                        )))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> enableDisableDeleteCommand(String action) {
        return CommandManager.literal(action)
                .then(listenerTailArgument()
                        .executes(context -> switch (action) {
                            case "enable" -> executeSetEnabled(context.getSource(), StringArgumentType.getString(context, "listener"), true);
                            case "disable" -> executeSetEnabled(context.getSource(), StringArgumentType.getString(context, "listener"), false);
                            case "delete" -> executeDelete(context.getSource(), StringArgumentType.getString(context, "listener"));
                            default -> 0;
                        }));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, String> channelArgument() {
        return CommandManager.argument("channel", StringArgumentType.string())
                .suggests((context, builder) -> CommandSuggestionUtil.suggestSignalChannels(context.getSource(), builder));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, String> listenerArgument() {
        return CommandManager.argument("listener", StringArgumentType.string())
                .suggests((context, builder) -> CommandSuggestionUtil.suggestSignalListenerRefs(context.getSource(), builder));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, String> listenerTailArgument() {
        return CommandManager.argument("listener", StringArgumentType.greedyString())
                .suggests((context, builder) -> CommandSuggestionUtil.suggestSignalListenerRefs(context.getSource(), builder));
    }

    private static int executeEmit(ServerCommandSource source, String rawChannel) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        String channel = SignalChannel.normalize(rawChannel);
        if (!SignalChannel.isValid(channel)) {
            source.sendFeedback(() -> error(SignalChannel.validationError(rawChannel).getString()), false);
            return 0;
        }

        SignalEvent event = new SignalEvent(
                channel,
                player,
                player.getCommandSource().getWorld(),
                new Vec3d(player.getX(), player.getY(), player.getZ()),
                ActionSourceType.COMMAND,
                "command",
                0,
                player.getCommandSource().getWorld().getTime()
        );
        ActionExecutionResult result = SignalBridgeServer.emit(event);
        if (result.success()) {
            source.sendFeedback(() -> title("已发出信号：").append(channelText(channel)), false);
            source.sendFeedback(() -> field("结果", result.message()), false);
            return 1;
        }
        source.sendFeedback(() -> error(result.message().getString()), false);
        return 0;
    }

    private static int executeCreate(ServerCommandSource source, String rawChannel, String name) {
        if (source.getServer() == null) {
            return 0;
        }

        String channel = SignalChannel.normalize(rawChannel);
        if (!SignalChannel.isValid(channel)) {
            source.sendFeedback(() -> error(SignalChannel.validationError(rawChannel).getString()), false);
            return 0;
        }

        SignalListenerData listener = SignalListenerStore.createListener(source.getServer(), channel, name);
        source.sendFeedback(() -> title("已创建信号监听器"), true);
        source.sendFeedback(() -> field("名称", listenerName(listener)), false);
        source.sendFeedback(() -> field("监听器", listenerName(listener)
                .append(Text.literal("（ID：").formatted(Formatting.GRAY))
                .append(shortIdText(listener.id()))
                .append(Text.literal("）").formatted(Formatting.GRAY))), false);
        source.sendFeedback(() -> field("频道", channelText(listener.channel())), false);
        source.sendFeedback(() -> field("查看详情", commandText("/tzz signal listen info " + listener.name())), false);
        return 1;
    }

    private static int executeList(ServerCommandSource source) {
        if (source.getServer() == null) {
            return 0;
        }

        List<SignalListenerData> listeners = SignalListenerStore.getSnapshot(source.getServer());
        source.sendFeedback(() -> title("信号监听器列表：").append(number(listeners.size())), false);
        if (listeners.isEmpty()) {
            source.sendFeedback(() -> warning("暂无信号监听器。"), false);
            return 0;
        }

        for (SignalListenerData listener : listeners) {
            source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY).append(listenerName(listener)), false);
            source.sendFeedback(() -> field("  ID", shortIdText(listener.id())), false);
            source.sendFeedback(() -> field("  频道", channelText(listener.channel())), false);
            source.sendFeedback(() -> field("  状态", statusText(listener.enabled())), false);
            source.sendFeedback(() -> field("  冷却", number(listener.cooldownTicks()).append(Text.literal(" tick").formatted(Formatting.GRAY))), false);
            source.sendFeedback(() -> field("  动作", number(listener.actions().size())), false);
        }
        return listeners.size();
    }

    private static int executeInfo(ServerCommandSource source, String listenerRef) {
        SignalListenerData listener = resolveListener(source, listenerRef);
        if (listener == null) {
            return 0;
        }

        source.sendFeedback(() -> title("信号监听器详情"), false);
        source.sendFeedback(() -> field("名称", listenerName(listener)), false);
        source.sendFeedback(() -> field("状态", statusText(listener.enabled())), false);
        source.sendFeedback(() -> field("监听器ID", fullIdText(listener.id())), false);
        source.sendFeedback(() -> field("频道", channelText(listener.channel())), false);
        source.sendFeedback(() -> field("冷却", number(listener.cooldownTicks()).append(Text.literal(" tick").formatted(Formatting.GRAY))), false);
        source.sendFeedback(() -> field("动作数量", number(listener.actions().size())), false);
        return 1;
    }

    private static int executeSetEnabled(ServerCommandSource source, String listenerRef, boolean enabled) {
        SignalListenerData listener = resolveListener(source, listenerRef);
        if (listener == null) {
            return 0;
        }

        boolean changed = SignalListenerStore.setEnabled(source.getServer(), listener.id(), enabled);
        if (!changed) {
            source.sendFeedback(() -> error("监听器状态更新失败：" + listenerRef), false);
            return 0;
        }
        source.sendFeedback(() -> title(enabled ? "已启用信号监听器" : "已禁用信号监听器")
                .append(Text.literal("：").formatted(Formatting.GRAY))
                .append(listenerName(listener)), true);
        return 1;
    }

    private static int executeDelete(ServerCommandSource source, String listenerRef) {
        SignalListenerData listener = resolveListener(source, listenerRef);
        if (listener == null) {
            return 0;
        }

        boolean deleted = SignalListenerStore.deleteListener(source.getServer(), listener.id());
        if (!deleted) {
            source.sendFeedback(() -> error("监听器删除失败：" + listenerRef), false);
            return 0;
        }
        source.sendFeedback(() -> title("已删除信号监听器")
                .append(Text.literal("：").formatted(Formatting.GRAY))
                .append(listenerName(listener)), true);
        return 1;
    }

    private static int executeAddCommandAction(ServerCommandSource source, String listenerRef, String command) {
        SignalListenerData listener = resolveListener(source, listenerRef);
        if (listener == null) {
            return 0;
        }

        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        ActionConfig action = ActionConfig.command(command, false);
        Text validationError = ActionValidator.validateForSave(player, action);
        if (validationError != null) {
            source.sendFeedback(() -> error("动作配置无效，无法保存。"), false);
            return 0;
        }

        boolean changed = SignalListenerStore.addAction(source.getServer(), listener.id(), action);
        if (!changed) {
            source.sendFeedback(() -> error("动作添加失败：" + listenerRef), false);
            return 0;
        }
        source.sendFeedback(() -> title("已添加监听器动作")
                .append(Text.literal("：").formatted(Formatting.GRAY))
                .append(listenerName(listener)), true);
        source.sendFeedback(() -> field("命令", commandText(command)), false);
        return 1;
    }

    private static int executeAddSignalAction(ServerCommandSource source, String listenerRef, String rawChannel) {
        SignalListenerData listener = resolveListener(source, listenerRef);
        if (listener == null) {
            return 0;
        }

        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        String channel = SignalChannel.normalize(rawChannel);
        if (!SignalChannel.isValid(channel)) {
            source.sendFeedback(() -> error(SignalChannel.validationError(rawChannel).getString()), false);
            return 0;
        }

        ActionConfig action = ActionConfig.signal(channel, false);
        Text validationError = ActionValidator.validateForSave(player, action);
        if (validationError != null) {
            source.sendFeedback(() -> error(validationError.getString()), false);
            return 0;
        }

        boolean changed = SignalListenerStore.addAction(source.getServer(), listener.id(), action);
        if (!changed) {
            source.sendFeedback(() -> error("动作添加失败：" + listenerRef), false);
            return 0;
        }
        source.sendFeedback(() -> title("已添加监听器信号动作")
                .append(Text.literal("：").formatted(Formatting.GRAY))
                .append(listenerName(listener)), true);
        source.sendFeedback(() -> field("频道", channelText(channel)), false);
        return 1;
    }

    private static int executeClearActions(ServerCommandSource source, String listenerRef) {
        SignalListenerData listener = resolveListener(source, listenerRef);
        if (listener == null) {
            return 0;
        }

        boolean changed = SignalListenerStore.clearActions(source.getServer(), listener.id());
        if (!changed) {
            source.sendFeedback(() -> error("动作清空失败：" + listenerRef), false);
            return 0;
        }
        source.sendFeedback(() -> title("已清空监听器动作")
                .append(Text.literal("：").formatted(Formatting.GRAY))
                .append(listenerName(listener)), true);
        return 1;
    }

    private static int executeCooldown(ServerCommandSource source, String listenerRef, int ticks) {
        if (ticks < SignalListenerData.MIN_COOLDOWN_TICKS) {
            source.sendFeedback(() -> error("冷却时间不能小于 0 tick"), false);
            return 0;
        }

        SignalListenerData listener = resolveListener(source, listenerRef);
        if (listener == null) {
            return 0;
        }

        boolean changed = SignalListenerStore.setCooldown(source.getServer(), listener.id(), ticks);
        if (!changed) {
            source.sendFeedback(() -> error("冷却时间更新失败：" + listenerRef), false);
            return 0;
        }
        source.sendFeedback(() -> title("已更新监听器冷却")
                .append(Text.literal("：").formatted(Formatting.GRAY))
                .append(listenerName(listener)), true);
        source.sendFeedback(() -> field("冷却", number(ticks).append(Text.literal(" tick").formatted(Formatting.GRAY))), false);
        return 1;
    }

    private static int executeTest(ServerCommandSource source, String listenerRef) {
        SignalListenerData listener = resolveListener(source, listenerRef);
        if (listener == null) {
            return 0;
        }

        if (listener.actions().isEmpty()) {
            source.sendFeedback(() -> warning("该监听器没有配置动作。"), false);
            return 0;
        }

        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        ActionContext context = new ActionContext(
                player,
                player.getCommandSource().getWorld(),
                new Vec3d(player.getX(), player.getY(), player.getZ()),
                ActionSourceType.SIGNAL_BRIDGE,
                listener.id(),
                ItemStack.EMPTY
        );
        ActionExecutionResult result = ActionEngine.executeAll(context, listener.actions());
        if (result.success()) {
            source.sendFeedback(() -> title("监听器测试动作已执行")
                    .append(Text.literal("：").formatted(Formatting.GRAY))
                    .append(listenerName(listener)), false);
            return 1;
        }

        source.sendFeedback(() -> error("监听器测试动作执行失败。"), false);
        return 0;
    }

    private static SignalListenerData resolveListener(ServerCommandSource source, String listenerRef) {
        if (source.getServer() == null) {
            return null;
        }

        String normalizedRef = normalizeListenerRef(listenerRef);
        SignalListenerStore.ResolveResult resolved = SignalListenerStore.resolveListener(source.getServer(), normalizedRef);
        if (resolved.foundUnique()) {
            return resolved.listener();
        }

        if (resolved.ambiguous()) {
            source.sendFeedback(() -> error("匹配到多个监听器，请使用完整 ID："), false);
            for (SignalListenerData match : resolved.matches()) {
                source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                        .append(listenerName(match))
                        .append(Text.literal("（ID：").formatted(Formatting.GRAY))
                        .append(shortIdText(match.id()))
                        .append(Text.literal("）").formatted(Formatting.GRAY)), false);
            }
            return null;
        }

        source.sendFeedback(() -> error("找不到信号监听器：" + listenerRef), false);
        return null;
    }

    private static ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            return player;
        }
        source.sendFeedback(() -> error("该命令必须由玩家执行。"), false);
        return null;
    }

    private static String normalizeListenerRef(String listenerRef) {
        String value = listenerRef == null ? "" : listenerRef.trim();
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }

    private static MutableText title(String text) {
        return Text.literal(text).formatted(Formatting.GREEN);
    }

    private static MutableText error(String text) {
        return Text.literal(text).formatted(Formatting.RED);
    }

    private static MutableText warning(String text) {
        return Text.literal(text).formatted(Formatting.YELLOW);
    }

    private static MutableText field(String label, Text value) {
        return Text.literal(label + "：").formatted(Formatting.GRAY).append(value);
    }

    private static MutableText listenerName(SignalListenerData listener) {
        String name = listener.name() == null || listener.name().isBlank() ? "未命名监听器" : listener.name();
        return Text.literal(name).formatted(Formatting.GOLD);
    }

    private static MutableText statusText(boolean enabled) {
        return Text.literal(enabled ? "启用" : "禁用").formatted(enabled ? Formatting.GREEN : Formatting.RED);
    }

    private static MutableText channelText(String channel) {
        return Text.literal(channel == null || channel.isBlank() ? "未知" : channel).formatted(Formatting.AQUA);
    }

    private static MutableText fullIdText(String id) {
        return Text.literal(id == null || id.isBlank() ? "未知" : id).formatted(Formatting.AQUA);
    }

    private static MutableText shortIdText(String id) {
        return Text.literal(SignalListenerStore.shortId(id)).formatted(Formatting.AQUA);
    }

    private static MutableText number(int value) {
        return Text.literal(Integer.toString(value)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText commandText(String command) {
        return Text.literal(command == null ? "" : command).formatted(Formatting.GREEN);
    }
}
