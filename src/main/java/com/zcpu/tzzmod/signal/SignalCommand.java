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
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.action.ActionValidator;
import com.zcpu.tzzmod.command.CommandSuggestionUtil;
import java.util.List;
import java.util.function.Supplier;
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
                        .then(channelTailArgument()
                                .executes(context -> executeEmit(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "channel")
                                ))))
                .then(CommandManager.literal("channels")
                        .executes(context -> executeChannels(context.getSource())))
                .then(CommandManager.literal("doctor")
                        .executes(context -> executeDoctor(context.getSource())))
                .then(CommandManager.literal("channel")
                        .then(CommandManager.literal("info")
                                .then(channelTailArgument()
                                        .executes(context -> executeChannelInfo(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "channel")
                                        )))))
                .then(CommandManager.literal("history")
                        .executes(context -> executeHistory(context.getSource(), null))
                        .then(CommandManager.argument("channel", StringArgumentType.greedyString())
                                .suggests((context, builder) -> CommandSuggestionUtil.suggestSignalChannels(context.getSource(), builder))
                                .executes(context -> executeHistory(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "channel")
                                ))))
                .then(CommandManager.literal("clearHistory")
                        .executes(context -> executeClearHistory(context.getSource())))
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
                        .then(CommandManager.literal("debug")
                                .then(listenerTailArgument()
                                        .executes(context -> executeDebug(
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

    private static RequiredArgumentBuilder<ServerCommandSource, String> channelTailArgument() {
        return CommandManager.argument("channel", StringArgumentType.greedyString())
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
            sendCommandFeedback(source, () -> title("已发出信号：").append(channelText(channel)), false);
            source.sendFeedback(() -> field("结果", result.message()), false);
            return 1;
        }
        sendCommandFeedback(source, () -> error(result.message().getString()), false);
        return 0;
    }

    private static int executeChannels(ServerCommandSource source) {
        if (source.getServer() == null) {
            return 0;
        }

        List<SignalChannelSummary> summaries = SignalChannelInspector.getSummaries(source.getServer());
        sendHeader(source, title("Signal 频道列表：").append(number(summaries.size())));
        if (summaries.isEmpty()) {
            source.sendFeedback(() -> warning("没有已知 Signal 频道。"), false);
            return 0;
        }

        for (SignalChannelSummary summary : summaries) {
            SignalChannelSummary current = summary;

            source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                    .append(channelText(current.channel()))
                    .append(noListenerMarker(current)), false);
            source.sendFeedback(() -> field("  监听器", number(current.listenerCount())
                    .append(Text.literal(" 个（启用 ").formatted(Formatting.GRAY))
                    .append(enabledNumber(current.enabledListenerCount()))
                    .append(Text.literal("，禁用 ").formatted(Formatting.GRAY))
                    .append(disabledNumber(current.disabledListenerCount()))
                    .append(Text.literal("）").formatted(Formatting.GRAY))), false);
            source.sendFeedback(() -> field("  动作", number(current.actionCount())), false);
            source.sendFeedback(() -> field("  最近触发", current.latestEvent() == null
                    ? Text.literal("尚未触发").formatted(Formatting.YELLOW)
                    : relativeTimeText(current.latestEvent().wallTimeMillis())), false);
            if (current.latestEvent() != null) {
                source.sendFeedback(() -> field("  结果", resultText(current.latestEvent())), false);
            }
        }
        return summaries.size();
    }

    private static int executeDoctor(ServerCommandSource source) {
        if (source.getServer() == null) {
            return 0;
        }

        SignalDoctorReport report = SignalDoctor.inspect(source.getServer());
        sendHeader(source, Text.literal("SignalBridge 诊断报告").formatted(Formatting.GOLD));
        if (report.issues().isEmpty()) {
            source.sendFeedback(() -> Text.literal("未发现明显问题。").formatted(Formatting.GREEN), false);
            sendDoctorOverview(source, report);
            return 1;
        }

        sendDoctorOverview(source, report);
        sendDoctorIssues(source, report, SignalDoctorIssue.Severity.ERROR);
        sendDoctorIssues(source, report, SignalDoctorIssue.Severity.WARNING);
        sendDoctorIssues(source, report, SignalDoctorIssue.Severity.INFO);
        return report.issues().size();
    }

    private static int executeChannelInfo(ServerCommandSource source, String rawChannel) {
        if (source.getServer() == null) {
            return 0;
        }

        String channel = SignalChannel.normalize(rawChannel);
        if (!SignalChannel.isValid(channel)) {
            sendCommandFeedback(source, () -> error(SignalChannel.validationError(rawChannel).getString()), false);
            return 0;
        }

        SignalChannelSummary summary = SignalChannelInspector.getSummary(source.getServer(), channel);
        List<SignalListenerData> listeners = SignalChannelInspector.getListenersForChannel(source.getServer(), channel);
        List<SignalEventRecord> recentEvents = SignalChannelInspector.getRecentEvents(channel, 5);
        if (listeners.isEmpty() && recentEvents.isEmpty()) {
            sendCommandFeedback(source, () -> warning("频道 " + channel + " 没有监听器，也没有历史记录。"), false);
            return 0;
        }

        sendHeader(source, title("Signal 频道详情：").append(channelText(channel)));
        source.sendFeedback(() -> field("监听器", number(summary.listenerCount())
                .append(Text.literal(" 个（启用 ").formatted(Formatting.GRAY))
                .append(enabledNumber(summary.enabledListenerCount()))
                .append(Text.literal("，禁用 ").formatted(Formatting.GRAY))
                .append(disabledNumber(summary.disabledListenerCount()))
                .append(Text.literal("）").formatted(Formatting.GRAY))), false);
        source.sendFeedback(() -> field("动作", number(summary.actionCount())), false);
        source.sendFeedback(() -> field("最近触发", summary.latestEvent() == null
                ? Text.literal("尚未触发").formatted(Formatting.YELLOW)
                : relativeTimeText(summary.latestEvent().wallTimeMillis())), false);

        if (listeners.isEmpty()) {
            source.sendFeedback(() -> field("监听器列表", number(0).append(Text.literal(" 个").formatted(Formatting.GRAY))), false);
        } else {
            source.sendFeedback(() -> title("监听器："), false);
            for (SignalListenerData listener : listeners) {
                source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY).append(listenerName(listener)), false);
                source.sendFeedback(() -> field("  状态", statusText(listener.enabled())), false);
                source.sendFeedback(() -> field("  冷却", number(listener.cooldownTicks()).append(Text.literal(" tick").formatted(Formatting.GRAY))), false);
                source.sendFeedback(() -> field("  动作", number(listener.actions().size())), false);
            }
        }

        if (recentEvents.isEmpty()) {
            source.sendFeedback(() -> warning("最近事件：暂无历史记录。"), false);
        } else {
            source.sendFeedback(() -> title("最近事件："), false);
            for (SignalEventRecord record : recentEvents) {
                sendChannelInfoRecord(source, record);
            }
        }
        return 1;
    }

    private static int executeHistory(ServerCommandSource source, String rawChannel) {
        String channel = rawChannel == null ? null : SignalChannel.normalize(rawChannel);
        if (channel != null && !SignalChannel.isValid(channel)) {
            sendCommandFeedback(source, () -> error(SignalChannel.validationError(rawChannel).getString()), false);
            return 0;
        }

        List<SignalEventRecord> records = channel == null
                ? SignalEventHistory.snapshot()
                : SignalEventHistory.snapshot(channel);
        if (records.isEmpty()) {
            if (channel == null) {
                sendCommandFeedback(source, () -> warning("没有 Signal 历史记录。"), false);
            } else {
                sendCommandFeedback(source, () -> warning("频道 " + channel + " 没有历史记录。"), false);
            }
            return 0;
        }

        int displayCount = Math.min(records.size(), 10);
        if (channel == null) {
            sendHeader(source, title("最近 Signal 事件（最新在下）：")
                    .append(number(displayCount))
                    .append(Text.literal(" / ").formatted(Formatting.GRAY))
                    .append(number(SignalEventHistory.size()))
                    .append(Text.literal("，最多保留 ").formatted(Formatting.GRAY))
                    .append(number(SignalEventHistory.maxSize()))
                    .append(Text.literal(" 条").formatted(Formatting.GRAY)));
        } else {
            sendHeader(source, title("频道 ")
                    .append(channelText(channel))
                    .append(Text.literal(" 的最近 Signal 事件（最新在下）：").formatted(Formatting.GREEN))
                    .append(number(displayCount)));
        }

        int startIndex = Math.max(0, records.size() - displayCount);
        for (int i = startIndex; i < records.size(); i++) {
            sendHistoryRecord(source, records.get(i));
        }
        return displayCount;
    }

    private static int executeClearHistory(ServerCommandSource source) {
        SignalEventHistory.clear();
        sendCommandFeedback(source, () -> title("已清空 Signal 历史记录。"), true);
        return 1;
    }

    private static int executeCreate(ServerCommandSource source, String rawChannel, String name) {
        if (source.getServer() == null) {
            return 0;
        }

        String channel = SignalChannel.normalize(rawChannel);
        if (!SignalChannel.isValid(channel)) {
            sendCommandFeedback(source, () -> error(SignalChannel.validationError(rawChannel).getString()), false);
            return 0;
        }

        String listenerName = cleanUserText(name);
        SignalListenerData listener = SignalListenerStore.createListener(source.getServer(), channel, listenerName);
        sendCommandFeedback(source, () -> title("已创建信号监听器"), true);
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
        sendHeader(source, title("信号监听器列表：").append(number(listeners.size())));
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

        sendHeader(source, title("信号监听器详情"));
        source.sendFeedback(() -> field("名称", listenerName(listener)), false);
        source.sendFeedback(() -> field("状态", statusText(listener.enabled())), false);
        source.sendFeedback(() -> field("监听器ID", fullIdText(listener.id())), false);
        source.sendFeedback(() -> field("频道", channelText(listener.channel())), false);
        source.sendFeedback(() -> field("冷却", number(listener.cooldownTicks()).append(Text.literal(" tick").formatted(Formatting.GRAY))), false);
        source.sendFeedback(() -> field("动作数量", number(listener.actions().size())), false);
        return 1;
    }

    private static int executeDebug(ServerCommandSource source, String listenerRef) {
        SignalListenerData listener = resolveListener(source, listenerRef);
        if (listener == null) {
            return 0;
        }

        long remainingCooldownTicks = SignalBridgeServer.getRemainingCooldownTicks(listener, source.getWorld().getTime());
        SignalChannelSummary summary = SignalChannelInspector.getSummary(source.getServer(), listener.channel());
        List<SignalEventRecord> recentEvents = SignalChannelInspector.getRecentEvents(listener.channel(), 3);

        sendHeader(source, title("信号监听器调试信息"));
        source.sendFeedback(() -> field("名称", listenerName(listener)), false);
        source.sendFeedback(() -> field("状态", statusText(listener.enabled())), false);
        source.sendFeedback(() -> field("监听器ID", fullIdText(listener.id())), false);
        source.sendFeedback(() -> field("短ID", shortIdText(listener.id())), false);
        source.sendFeedback(() -> field("监听频道", channelText(listener.channel())), false);
        source.sendFeedback(() -> field("冷却设置", number(listener.cooldownTicks()).append(Text.literal(" tick").formatted(Formatting.GRAY))), false);
        source.sendFeedback(() -> field("当前冷却剩余", cooldownRemainingText(remainingCooldownTicks)), false);
        source.sendFeedback(() -> field("动作数量", number(listener.actions().size())), false);

        source.sendFeedback(() -> title("同频道概览"), false);
        source.sendFeedback(() -> field("监听器总数", number(summary.listenerCount())), false);
        source.sendFeedback(() -> field("启用监听器", enabledNumber(summary.enabledListenerCount())), false);
        source.sendFeedback(() -> field("禁用监听器", disabledNumber(summary.disabledListenerCount())), false);

        sendDebugActions(source, listener);
        sendDebugRecentEvents(source, recentEvents);
        sendDirectRecursionWarning(source, listener);
        return 1;
    }

    private static int executeSetEnabled(ServerCommandSource source, String listenerRef, boolean enabled) {
        SignalListenerData listener = resolveListener(source, listenerRef);
        if (listener == null) {
            return 0;
        }

        boolean changed = SignalListenerStore.setEnabled(source.getServer(), listener.id(), enabled);
        if (!changed) {
            sendCommandFeedback(source, () -> error("监听器状态更新失败：" + listenerRef), false);
            return 0;
        }
        sendCommandFeedback(source, () -> title(enabled ? "已启用信号监听器" : "已禁用信号监听器")
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
            sendCommandFeedback(source, () -> error("监听器删除失败：" + listenerRef), false);
            return 0;
        }
        sendCommandFeedback(source, () -> title("已删除信号监听器")
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
            sendCommandFeedback(source, () -> error("动作配置无效，无法保存。"), false);
            return 0;
        }

        boolean changed = SignalListenerStore.addAction(source.getServer(), listener.id(), action);
        if (!changed) {
            sendCommandFeedback(source, () -> error("动作添加失败：" + listenerRef), false);
            return 0;
        }
        sendCommandFeedback(source, () -> title("已添加监听器动作")
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
            sendCommandFeedback(source, () -> error(SignalChannel.validationError(rawChannel).getString()), false);
            return 0;
        }

        ActionConfig action = ActionConfig.signal(channel, false);
        Text validationError = ActionValidator.validateForSave(player, action);
        if (validationError != null) {
            sendCommandFeedback(source, () -> error(validationError.getString()), false);
            return 0;
        }

        boolean changed = SignalListenerStore.addAction(source.getServer(), listener.id(), action);
        if (!changed) {
            sendCommandFeedback(source, () -> error("动作添加失败：" + listenerRef), false);
            return 0;
        }
        sendCommandFeedback(source, () -> title("已添加监听器信号动作")
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
            sendCommandFeedback(source, () -> error("动作清空失败：" + listenerRef), false);
            return 0;
        }
        sendCommandFeedback(source, () -> title("已清空监听器动作")
                .append(Text.literal("：").formatted(Formatting.GRAY))
                .append(listenerName(listener)), true);
        return 1;
    }

    private static int executeCooldown(ServerCommandSource source, String listenerRef, int ticks) {
        if (ticks < SignalListenerData.MIN_COOLDOWN_TICKS) {
            sendCommandFeedback(source, () -> error("冷却时间不能小于 0 tick"), false);
            return 0;
        }

        SignalListenerData listener = resolveListener(source, listenerRef);
        if (listener == null) {
            return 0;
        }

        boolean changed = SignalListenerStore.setCooldown(source.getServer(), listener.id(), ticks);
        if (!changed) {
            sendCommandFeedback(source, () -> error("冷却时间更新失败：" + listenerRef), false);
            return 0;
        }
        sendCommandFeedback(source, () -> title("已更新监听器冷却")
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
            sendCommandFeedback(source, () -> warning("该监听器没有配置动作。"), false);
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
            sendCommandFeedback(source, () -> title("监听器测试动作已执行")
                    .append(Text.literal("：").formatted(Formatting.GRAY))
                    .append(listenerName(listener)), false);
            return 1;
        }

        sendCommandFeedback(source, () -> error("监听器测试动作执行失败。"), false);
        return 0;
    }

    private static void sendHistoryRecord(ServerCommandSource source, SignalEventRecord record) {
        source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                .append(channelText(record.channel())), false);
        source.sendFeedback(() -> field("  来源", sourceTypeText(record.sourceType())), false);
        source.sendFeedback(() -> field("  玩家", Text.literal(safeRecordText(record.playerName())).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("  监听器", number(record.listenerCount())
                .append(Text.literal("，执行：").formatted(Formatting.GRAY))
                .append(number(record.executedCount()))
                .append(Text.literal("，冷却跳过：").formatted(Formatting.GRAY))
                .append(number(record.skippedCooldownCount()))
                .append(Text.literal("，空动作：").formatted(Formatting.GRAY))
                .append(number(record.skippedEmptyCount()))
                .append(Text.literal("，失败：").formatted(Formatting.GRAY))
                .append(number(record.failedCount()))), false);
        source.sendFeedback(() -> field("  深度", number(record.depth())
                .append(Text.literal("，时间：").formatted(Formatting.GRAY))
                .append(number(record.gameTime()))
                .append(Text.literal(" tick").formatted(Formatting.GRAY))), false);
        source.sendFeedback(() -> field("  结果", resultText(record)), false);
    }

    private static void sendChannelInfoRecord(ServerCommandSource source, SignalEventRecord record) {
        source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                .append(Text.literal("玩家：").formatted(Formatting.GRAY))
                .append(Text.literal(safeRecordText(record.playerName())).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("  来源", sourceTypeText(record.sourceType())), false);
        source.sendFeedback(() -> field("  监听器", number(record.listenerCount())
                .append(Text.literal("，执行：").formatted(Formatting.GRAY))
                .append(number(record.executedCount()))
                .append(Text.literal("，冷却跳过：").formatted(Formatting.GRAY))
                .append(number(record.skippedCooldownCount()))
                .append(Text.literal("，空动作：").formatted(Formatting.GRAY))
                .append(number(record.skippedEmptyCount()))
                .append(Text.literal("，失败：").formatted(Formatting.GRAY))
                .append(number(record.failedCount()))), false);
        source.sendFeedback(() -> field("  距今", relativeTimeText(record.wallTimeMillis())), false);
        source.sendFeedback(() -> field("  记录时间", number(record.gameTime())
                .append(Text.literal(" tick").formatted(Formatting.GRAY))), false);
        source.sendFeedback(() -> field("  结果", resultText(record)), false);
    }

    private static void sendDebugActions(ServerCommandSource source, SignalListenerData listener) {
        source.sendFeedback(() -> title("动作列表"), false);
        List<ActionConfig> actions = listener.actions();
        if (actions.isEmpty()) {
            source.sendFeedback(() -> warning("该监听器没有配置动作。"), false);
            return;
        }

        for (int i = 0; i < actions.size(); i++) {
            ActionConfig action = actions.get(i);
            int index = i + 1;
            source.sendFeedback(() -> Text.literal(index + ". ").formatted(Formatting.GRAY)
                    .append(actionSummaryText(action)), false);
            source.sendFeedback(() -> field("  状态", statusText(action.enabled())), false);
            source.sendFeedback(() -> field("  通知管理员", booleanText(action.notifyOps())), false);
            source.sendFeedback(() -> field("  需要 OP", booleanText(action.requiresOp())), false);
        }
    }

    private static void sendDebugRecentEvents(ServerCommandSource source, List<SignalEventRecord> recentEvents) {
        source.sendFeedback(() -> title("最近频道事件"), false);
        if (recentEvents.isEmpty()) {
            source.sendFeedback(() -> warning("该频道暂无历史记录。"), false);
            return;
        }

        for (SignalEventRecord record : recentEvents) {
            source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                    .append(Text.literal("玩家：").formatted(Formatting.GRAY))
                    .append(Text.literal(safeRecordText(record.playerName())).formatted(Formatting.WHITE)), false);
            source.sendFeedback(() -> field("  来源", sourceTypeText(record.sourceType())), false);
            source.sendFeedback(() -> field("  执行", number(record.executedCount())
                    .append(Text.literal("，冷却跳过：").formatted(Formatting.GRAY))
                    .append(number(record.skippedCooldownCount()))
                    .append(Text.literal("，空动作：").formatted(Formatting.GRAY))
                    .append(number(record.skippedEmptyCount()))
                    .append(Text.literal("，失败：").formatted(Formatting.GRAY))
                    .append(number(record.failedCount()))), false);
            source.sendFeedback(() -> field("  距今", relativeTimeText(record.wallTimeMillis())), false);
            source.sendFeedback(() -> field("  结果", resultText(record)), false);
        }
    }

    private static void sendDoctorOverview(ServerCommandSource source, SignalDoctorReport report) {
        source.sendFeedback(() -> title("总览："), false);
        source.sendFeedback(() -> field("监听器", number(report.listenerCount())
                .append(Text.literal(" 个（启用 ").formatted(Formatting.GRAY))
                .append(enabledNumber(report.enabledListenerCount()))
                .append(Text.literal("，禁用 ").formatted(Formatting.GRAY))
                .append(disabledNumber(report.disabledListenerCount()))
                .append(Text.literal("）").formatted(Formatting.GRAY))), false);
        source.sendFeedback(() -> field("频道", number(report.channelCount()).append(Text.literal(" 个").formatted(Formatting.GRAY))), false);
        source.sendFeedback(() -> field("历史记录", number(report.historyCount()).append(Text.literal(" 条").formatted(Formatting.GRAY))), false);
    }

    private static void sendDoctorIssues(
            ServerCommandSource source,
            SignalDoctorReport report,
            SignalDoctorIssue.Severity severity
    ) {
        List<SignalDoctorIssue> issues = report.issues().stream()
                .filter(issue -> issue.severity() == severity)
                .toList();
        if (issues.isEmpty()) {
            return;
        }

        source.sendFeedback(() -> doctorSectionTitle(severity), false);
        Formatting itemFormatting = doctorItemFormatting(severity);
        for (SignalDoctorIssue issue : issues) {
            source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                    .append(Text.literal(issue.title()).formatted(itemFormatting)), false);
            if (issue.detail() != null && !issue.detail().isBlank()) {
                source.sendFeedback(() -> Text.literal("  " + issue.detail()).formatted(Formatting.GRAY), false);
            }
        }
    }

    private static void sendDirectRecursionWarning(ServerCommandSource source, SignalListenerData listener) {
        if (hasDirectSignalRecursion(listener)) {
            source.sendFeedback(() -> warning("警告：该监听器会向自身监听频道发出 signal，可能触发递归保护。"), false);
        }
    }

    private static SignalListenerData resolveListener(ServerCommandSource source, String listenerRef) {
        if (source.getServer() == null) {
            return null;
        }

        String normalizedRef = cleanUserText(listenerRef);
        SignalListenerStore.ResolveResult resolved = SignalListenerStore.resolveListener(source.getServer(), normalizedRef);
        if (resolved.foundUnique()) {
            return resolved.listener();
        }

        if (resolved.ambiguous()) {
            sendCommandFeedback(source, () -> error("匹配到多个监听器，请使用完整 ID："), false);
            for (SignalListenerData match : resolved.matches()) {
                source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                        .append(listenerName(match))
                        .append(Text.literal("（ID：").formatted(Formatting.GRAY))
                        .append(shortIdText(match.id()))
                        .append(Text.literal("）").formatted(Formatting.GRAY)), false);
            }
            return null;
        }

        sendCommandFeedback(source, () -> error("找不到信号监听器：" + listenerRef), false);
        return null;
    }

    private static ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            return player;
        }
        sendCommandFeedback(source, () -> error("该命令必须由玩家执行。"), false);
        return null;
    }

    private static String cleanUserText(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() >= 2) {
            boolean doubleQuoted = value.startsWith("\"") && value.endsWith("\"");
            boolean singleQuoted = value.startsWith("'") && value.endsWith("'");
            if (doubleQuoted || singleQuoted) {
                value = value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }

    private static void sendDivider(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("===========").formatted(Formatting.AQUA), false);
    }

    private static void sendCommandFeedback(ServerCommandSource source, Supplier<Text> feedback, boolean broadcastToOps) {
        sendDivider(source);
        source.sendFeedback(feedback, broadcastToOps);
    }

    private static void sendHeader(ServerCommandSource source, Text header) {
        sendDivider(source);
        source.sendFeedback(() -> header, false);
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

    private static MutableText doctorSectionTitle(SignalDoctorIssue.Severity severity) {
        return switch (severity) {
            case ERROR -> Text.literal("错误：").formatted(Formatting.RED);
            case WARNING -> Text.literal("警告：").formatted(Formatting.YELLOW);
            case INFO -> Text.literal("提示：").formatted(Formatting.GRAY);
        };
    }

    private static Formatting doctorItemFormatting(SignalDoctorIssue.Severity severity) {
        return switch (severity) {
            case ERROR -> Formatting.RED;
            case WARNING -> Formatting.YELLOW;
            case INFO -> Formatting.GRAY;
        };
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

    private static MutableText number(long value) {
        return Text.literal(Long.toString(value)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText enabledNumber(int value) {
        return Text.literal(Integer.toString(value)).formatted(Formatting.GREEN);
    }

    private static MutableText disabledNumber(int value) {
        return Text.literal(Integer.toString(value)).formatted(Formatting.RED);
    }

    private static MutableText commandText(String command) {
        return Text.literal(command == null ? "" : command).formatted(Formatting.GREEN);
    }

    private static MutableText cooldownRemainingText(long ticks) {
        MutableText text = number(ticks).append(Text.literal(" tick").formatted(Formatting.GRAY));
        if (ticks > 0) {
            text.append(Text.literal("（约 ").formatted(Formatting.GRAY))
                    .append(Text.literal(String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0D)).formatted(Formatting.LIGHT_PURPLE))
                    .append(Text.literal(" 秒）").formatted(Formatting.GRAY));
        }
        return text;
    }

    private static MutableText actionSummaryText(ActionConfig action) {
        if (action == null || action.type() == null) {
            return Text.literal("未知动作").formatted(Formatting.YELLOW);
        }

        String value = action.value() == null ? "" : action.value();
        if (action.type() == ActionType.SIGNAL) {
            return Text.literal("信号 ").formatted(Formatting.AQUA)
                    .append(channelText(value));
        }
        if (action.type() == ActionType.COMMAND) {
            return Text.literal("命令 ").formatted(Formatting.GREEN)
                    .append(commandText(value));
        }
        return Text.literal(actionTypeLabel(action.type()) + " ").formatted(Formatting.GOLD)
                .append(Text.literal(value).formatted(Formatting.WHITE));
    }

    private static String actionTypeLabel(ActionType type) {
        return switch (type) {
            case MESSAGE -> "消息";
            case SOUND -> "音效";
            case SIGNAL -> "信号";
            case COMMAND -> "命令";
        };
    }

    private static MutableText booleanText(boolean value) {
        return Text.literal(value ? "是" : "否").formatted(value ? Formatting.GREEN : Formatting.RED);
    }

    private static boolean hasDirectSignalRecursion(SignalListenerData listener) {
        String channel = SignalChannel.normalize(listener.channel());
        for (ActionConfig action : listener.actions()) {
            if (action != null
                    && action.type() == ActionType.SIGNAL
                    && SignalChannel.normalize(action.value()).equals(channel)) {
                return true;
            }
        }
        return false;
    }

    private static MutableText noListenerMarker(SignalChannelSummary summary) {
        if (summary.listenerCount() == 0 && summary.latestEvent() != null) {
            return Text.literal("（无监听器）").formatted(Formatting.YELLOW);
        }
        return Text.literal("");
    }

    private static MutableText relativeTimeText(long wallTimeMillis) {
        if (wallTimeMillis <= 0L) {
            return Text.literal("未知").formatted(Formatting.YELLOW);
        }
        return Text.literal(formatElapsed(System.currentTimeMillis() - wallTimeMillis)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static String formatElapsed(long elapsedMillis) {
        if (elapsedMillis < 3_000L) {
            return "刚刚";
        }

        long totalSeconds = Math.max(0L, elapsedMillis / 1_000L);
        long days = totalSeconds / 86_400L;
        if (days > 0) {
            long hours = (totalSeconds % 86_400L) / 3_600L;
            return hours > 0 ? days + " 天 " + hours + " 小时前" : days + " 天前";
        }

        long hours = totalSeconds / 3_600L;
        if (hours > 0) {
            long minutes = (totalSeconds % 3_600L) / 60L;
            return minutes > 0 ? hours + " 小时 " + minutes + " 分前" : hours + " 小时前";
        }

        long minutes = totalSeconds / 60L;
        if (minutes > 0) {
            long seconds = totalSeconds % 60L;
            return seconds > 0 ? minutes + " 分 " + seconds + " 秒前" : minutes + " 分前";
        }

        return totalSeconds + " 秒前";
    }

    private static MutableText sourceTypeText(String sourceType) {
        return Text.literal(safeRecordText(sourceType)).formatted(Formatting.GOLD);
    }

    private static MutableText resultText(SignalEventRecord record) {
        Formatting formatting = record.failedCount() > 0 ? Formatting.RED : Formatting.GREEN;
        return Text.literal(safeRecordText(record.resultMessage())).formatted(formatting);
    }

    private static String safeRecordText(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
