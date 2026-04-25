package com.zcpu.tzzmod.signal.device;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zcpu.tzzmod.ModBlock.entity.SignalEmitterBlockEntity;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.command.CommandSuggestionUtil;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalChannelInspector;
import com.zcpu.tzzmod.signal.SignalEventHistory;
import com.zcpu.tzzmod.signal.SignalEventRecord;
import com.zcpu.tzzmod.signal.SignalListenerData;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class SignalDeviceCommand {
    private static final int LIST_LIMIT = 20;
    private static final int HISTORY_LIMIT = 10;
    private static final int DEBUG_HISTORY_LIMIT = 3;

    private SignalDeviceCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("device")
                .then(CommandManager.literal("list")
                        .executes(context -> executeList(context.getSource())))
                .then(CommandManager.literal("bind")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("channel", StringArgumentType.string())
                                        .executes(context -> executeBind(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "channel")
                                        )))))
                .then(CommandManager.literal("name")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> executeName(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "name")
                                        )))))
                .then(CommandManager.literal("clearName")
                        .then(deviceArgument()
                                .executes(context -> executeClearName(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "device")
                                ))))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeInfoPos(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                )))
                        .then(deviceArgument()
                                .executes(context -> executeInfoDevice(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "device")
                                ))))
                .then(CommandManager.literal("history")
                        .then(deviceArgument()
                                .executes(context -> executeHistory(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "device")
                                ))))
                .then(CommandManager.literal("debug")
                        .then(deviceArgument()
                                .executes(context -> executeDebug(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "device")
                                ))))
                .then(CommandManager.literal("test")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeTest(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("enable")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeSetEnabled(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                        true
                                ))))
                .then(CommandManager.literal("disable")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeSetEnabled(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                        false
                                ))));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, String> deviceArgument() {
        return CommandManager.argument("device", StringArgumentType.greedyString())
                .suggests((context, builder) -> suggestDevices(context.getSource(), builder));
    }

    private static int executeList(ServerCommandSource source) {
        List<SignalDeviceData> devices = SignalDeviceStore.getSnapshot(source.getServer());
        sendHeader(source, Text.literal("信号设备列表：").formatted(Formatting.GREEN)
                .append(number(devices.size())).append(Text.literal(" 个").formatted(Formatting.GRAY)));
        if (devices.isEmpty()) {
            source.sendFeedback(() -> warning("暂无已知信号设备。"), false);
            return 1;
        }

        int shown = Math.min(devices.size(), LIST_LIMIT);
        for (int index = 0; index < shown; index++) {
            SignalDeviceData device = devices.get(index);
            source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                    .append(nameText(SignalDeviceStore.displayName(device))), false);
            source.sendFeedback(() -> indentField("ID", idText(SignalDeviceStore.shortId(device.id()))), false);
            source.sendFeedback(() -> indentField("类型", Text.literal(device.type()).formatted(Formatting.WHITE)), false);
            source.sendFeedback(() -> indentField("频道", channelOrEmpty(device.channel())), false);
            source.sendFeedback(() -> indentField("位置", positionText(device)), false);
            source.sendFeedback(() -> indentField("状态", enabledText(device.enabled())), false);
            source.sendFeedback(() -> indentField("最近触发", elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        }
        if (devices.size() > shown) {
            source.sendFeedback(() -> warning("仅显示前 " + shown + " 个，共 " + devices.size() + " 个。"), false);
        }
        return 1;
    }

    private static int executeBind(ServerCommandSource source, BlockPos pos, String rawChannel) {
        SignalEmitterBlockEntity blockEntity = getEmitter(source, pos);
        if (blockEntity == null) {
            return 0;
        }

        String channel = SignalChannel.normalize(rawChannel);
        if (!SignalChannel.isValid(channel)) {
            sendError(source, SignalChannel.validationError(rawChannel));
            return 0;
        }

        blockEntity.setChannel(channel);
        SignalDeviceStore.updateChannel(source.getWorld(), pos, blockEntity);
        sendHeader(source, Text.literal("已绑定信号发射器频道").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("频道", channelText(channel)), false);
        return 1;
    }

    private static int executeName(ServerCommandSource source, BlockPos pos, String rawName) {
        SignalEmitterBlockEntity blockEntity = getEmitter(source, pos);
        if (blockEntity == null) {
            return 0;
        }

        String name = SignalDeviceStore.cleanUserText(rawName);
        if (name.isBlank()) {
            sendError(source, Text.literal("设备名称不能为空。"));
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.setName(source.getWorld(), pos, blockEntity, name);
        sendHeader(source, Text.literal("已命名信号设备").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", positionText(device)), false);
        source.sendFeedback(() -> field("名称", nameText(device.name())), false);
        return 1;
    }

    private static int executeClearName(ServerCommandSource source, String deviceRef) {
        SignalDeviceStore.ResolveResult resolved = resolveDevice(source, deviceRef);
        if (!resolved.foundUnique()) {
            return 0;
        }

        String oldName = resolved.device().name();
        SignalDeviceStore.ResolveResult cleared = SignalDeviceStore.clearName(source.getServer(), deviceRef);
        if (!cleared.foundUnique()) {
            sendError(source, Text.literal("未能清空设备名称。"));
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.refreshLoadedState(source.getServer(), cleared.device());
        sendHeader(source, Text.literal("已清空信号设备名称").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("原名称", oldName == null || oldName.isBlank()
                ? Text.literal("未命名").formatted(Formatting.YELLOW)
                : nameText(oldName)), false);
        source.sendFeedback(() -> field("位置", positionText(device)), false);
        return 1;
    }

    private static int executeInfoPos(ServerCommandSource source, BlockPos pos) {
        SignalEmitterBlockEntity blockEntity = getEmitter(source, pos);
        if (blockEntity == null) {
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.upsertEmitter(source.getWorld(), pos, blockEntity);
        sendInfo(source, device, true);
        return 1;
    }

    private static int executeInfoDevice(ServerCommandSource source, String deviceRef) {
        SignalDeviceStore.ResolveResult resolved = resolveDevice(source, deviceRef);
        if (!resolved.foundUnique()) {
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.refreshLoadedState(source.getServer(), resolved.device());
        sendInfo(source, device, SignalDeviceStore.getLoadedEmitter(source.getServer(), device) != null);
        return 1;
    }

    private static int executeHistory(ServerCommandSource source, String deviceRef) {
        SignalDeviceStore.ResolveResult resolved = resolveDevice(source, deviceRef);
        if (!resolved.foundUnique()) {
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.refreshLoadedState(source.getServer(), resolved.device());
        List<SignalEventRecord> records = recentDeviceEvents(device, HISTORY_LIMIT);
        sendHeader(source, Text.literal("信号设备历史：").formatted(Formatting.GREEN)
                .append(nameText(SignalDeviceStore.displayName(device))));
        if (records.isEmpty()) {
            source.sendFeedback(() -> warning("该设备暂无 Signal 历史记录。"), false);
            return 1;
        }

        for (SignalEventRecord record : records) {
            source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                    .append(channelText(record.channel()))
                    .append(Text.literal("，距今 ").formatted(Formatting.GRAY))
                    .append(elapsedText(record.wallTimeMillis())), false);
            source.sendFeedback(() -> indentField("玩家", Text.literal(safe(record.playerName())).formatted(Formatting.WHITE)), false);
            source.sendFeedback(() -> indentField("监听器", number(record.listenerCount())
                    .append(Text.literal("，执行 ").formatted(Formatting.GRAY)).append(number(record.executedCount()))
                    .append(Text.literal("，失败 ").formatted(Formatting.GRAY)).append(number(record.failedCount()))), false);
            source.sendFeedback(() -> indentField("深度", number(record.depth())), false);
            source.sendFeedback(() -> indentField("结果", resultText(record)), false);
        }
        return 1;
    }

    private static int executeDebug(ServerCommandSource source, String deviceRef) {
        SignalDeviceStore.ResolveResult resolved = resolveDevice(source, deviceRef);
        if (!resolved.foundUnique()) {
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.refreshLoadedState(source.getServer(), resolved.device());
        SignalEmitterBlockEntity blockEntity = SignalDeviceStore.getLoadedEmitter(source.getServer(), device);
        List<SignalListenerData> listeners = SignalChannelInspector.getListenersForChannel(source.getServer(), device.channel());
        int enabledListeners = 0;
        int actionCount = 0;
        for (SignalListenerData listener : listeners) {
            if (listener.enabled()) {
                enabledListeners++;
            }
            actionCount += listener.actions() == null ? 0 : listener.actions().size();
        }
        int finalEnabledListeners = enabledListeners;
        int finalActionCount = actionCount;

        sendHeader(source, Text.literal("信号设备调试信息").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("名称", nameText(SignalDeviceStore.displayName(device))), false);
        source.sendFeedback(() -> field("ID", idText(device.id())), false);
        source.sendFeedback(() -> field("短ID", idText(SignalDeviceStore.shortId(device.id()))), false);
        source.sendFeedback(() -> field("类型", Text.literal(device.type()).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("位置", positionText(device)), false);
        source.sendFeedback(() -> field("频道", channelOrEmpty(device.channel())), false);
        source.sendFeedback(() -> field("状态", enabledText(device.enabled())), false);
        source.sendFeedback(() -> field("最近触发", elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        source.sendFeedback(() -> field("最近结果", resultText(device.lastResult())), false);
        source.sendFeedback(() -> field("方块实体", blockEntity == null
                ? Text.literal("方块未加载或已不存在").formatted(Formatting.YELLOW)
                : Text.literal("已加载并匹配").formatted(Formatting.GREEN)), false);
        source.sendFeedback(() -> field("状态一致性", consistencyText(device, blockEntity)), false);
        source.sendFeedback(() -> field("频道有效性", SignalChannel.isValid(device.channel())
                ? Text.literal("有效").formatted(Formatting.GREEN)
                : Text.literal("无效或未绑定").formatted(Formatting.YELLOW)), false);
        source.sendFeedback(() -> field("频道监听器", number(listeners.size())
                .append(Text.literal(" 个，启用 ").formatted(Formatting.GRAY))
                .append(number(finalEnabledListeners))
                .append(Text.literal(" 个，动作 ").formatted(Formatting.GRAY))
                .append(number(finalActionCount))
                .append(Text.literal(" 个").formatted(Formatting.GRAY))), false);

        List<Text> hints = debugHints(device, blockEntity, listeners);
        if (!hints.isEmpty()) {
            source.sendFeedback(() -> Text.literal("常见问题提示：").formatted(Formatting.YELLOW), false);
            for (Text hint : hints) {
                source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.YELLOW).append(hint), false);
            }
        }

        List<SignalEventRecord> records = recentDeviceEvents(device, DEBUG_HISTORY_LIMIT);
        if (records.isEmpty()) {
            source.sendFeedback(() -> warning("该设备暂无最近事件。"), false);
        } else {
            source.sendFeedback(() -> Text.literal("最近事件：").formatted(Formatting.GRAY), false);
            for (SignalEventRecord record : records) {
                source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                        .append(channelText(record.channel()))
                        .append(Text.literal("，执行 ").formatted(Formatting.GRAY))
                        .append(number(record.executedCount()))
                        .append(Text.literal("，失败 ").formatted(Formatting.GRAY))
                        .append(number(record.failedCount()))
                        .append(Text.literal("，距今 ").formatted(Formatting.GRAY))
                        .append(elapsedText(record.wallTimeMillis())), false);
            }
        }
        return 1;
    }

    private static int executeTest(ServerCommandSource source, BlockPos pos) {
        SignalEmitterBlockEntity blockEntity = getEmitter(source, pos);
        if (blockEntity == null) {
            return 0;
        }

        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
        ActionExecutionResult result = blockEntity.emitSignal(source.getWorld(), player);
        if (blockEntity.enabled()
                && !blockEntity.channel().isBlank()
                && SignalChannel.isValid(blockEntity.channel())) {
            SignalDeviceStore.recordTrigger(source.getWorld(), pos, blockEntity, result);
        }
        if (!result.success()) {
            sendError(source, result.message());
            return 0;
        }

        sendHeader(source, Text.literal("已测试信号发射器").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("频道", channelText(blockEntity.channel())), false);
        source.sendFeedback(() -> field("结果", result.message()), false);
        return 1;
    }

    private static int executeSetEnabled(ServerCommandSource source, BlockPos pos, boolean enabled) {
        SignalEmitterBlockEntity blockEntity = getEmitter(source, pos);
        if (blockEntity == null) {
            return 0;
        }

        blockEntity.setEnabled(enabled);
        SignalDeviceStore.updateEnabled(source.getWorld(), pos, blockEntity);
        sendHeader(source, Text.literal(enabled ? "已启用信号发射器" : "已禁用信号发射器")
                .formatted(enabled ? Formatting.GREEN : Formatting.RED));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("状态", Text.literal(enabled ? "启用" : "禁用")
                .formatted(enabled ? Formatting.GREEN : Formatting.RED)), false);
        return 1;
    }

    private static SignalEmitterBlockEntity getEmitter(ServerCommandSource source, BlockPos pos) {
        if (source.getWorld().getBlockEntity(pos) instanceof SignalEmitterBlockEntity blockEntity) {
            return blockEntity;
        }

        sendError(source, Text.literal("该位置不是信号发射器。"));
        return null;
    }

    private static SignalDeviceStore.ResolveResult resolveDevice(ServerCommandSource source, String deviceRef) {
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(source.getServer(), deviceRef);
        if (resolved.foundUnique()) {
            return resolved;
        }

        if (resolved.ambiguous()) {
            sendHeader(source, Text.literal("设备引用不唯一").formatted(Formatting.YELLOW));
            source.sendFeedback(() -> warning("请使用完整 ID 或更长的短 ID。"), false);
            for (SignalDeviceData match : resolved.matches()) {
                source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                        .append(nameText(SignalDeviceStore.displayName(match)))
                        .append(Text.literal("（ID：").formatted(Formatting.GRAY))
                        .append(idText(match.id()))
                        .append(Text.literal("）").formatted(Formatting.GRAY)), false);
            }
        } else {
            sendError(source, Text.literal("找不到信号设备：" + SignalDeviceStore.cleanUserText(deviceRef)));
        }
        return resolved;
    }

    private static void sendInfo(ServerCommandSource source, SignalDeviceData device, boolean loaded) {
        sendHeader(source, Text.literal("信号设备信息").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("名称", nameText(SignalDeviceStore.displayName(device))), false);
        source.sendFeedback(() -> field("ID", idText(device.id())), false);
        source.sendFeedback(() -> field("短ID", idText(SignalDeviceStore.shortId(device.id()))), false);
        source.sendFeedback(() -> field("类型", Text.literal(device.type()).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("位置", positionText(device)), false);
        source.sendFeedback(() -> field("频道", channelOrEmpty(device.channel())), false);
        source.sendFeedback(() -> field("状态", enabledText(device.enabled())), false);
        source.sendFeedback(() -> field("红石", redstoneText(source, device)), false);
        source.sendFeedback(() -> field("最近触发", elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        source.sendFeedback(() -> field("最近结果", resultText(device.lastResult())), false);
        source.sendFeedback(() -> field("状态来源", loaded
                ? Text.literal("已加载方块实体").formatted(Formatting.GREEN)
                : Text.literal("设备注册表，方块未加载或不匹配").formatted(Formatting.YELLOW)), false);
    }

    private static List<Text> debugHints(SignalDeviceData device, SignalEmitterBlockEntity blockEntity, List<SignalListenerData> listeners) {
        List<Text> hints = new ArrayList<>();
        if (device.channel().isBlank()) {
            hints.add(Text.literal("未绑定频道。").formatted(Formatting.YELLOW));
        }
        if (!device.enabled()) {
            hints.add(Text.literal("设备已禁用。").formatted(Formatting.YELLOW));
        }
        if (!device.channel().isBlank() && listeners.isEmpty()) {
            hints.add(Text.literal("频道没有 listener。").formatted(Formatting.YELLOW));
        }
        if (blockEntity == null) {
            hints.add(Text.literal("方块未加载或已不存在。").formatted(Formatting.YELLOW));
        } else if (!SignalChannel.normalize(blockEntity.channel()).equals(SignalChannel.normalize(device.channel()))
                || blockEntity.enabled() != device.enabled()) {
            hints.add(Text.literal("registry 与 BlockEntity 状态不一致。").formatted(Formatting.YELLOW));
        }
        if (!device.channel().isBlank() && !SignalChannel.isValid(device.channel())) {
            hints.add(Text.literal("频道名称无效。").formatted(Formatting.RED));
        }
        return hints;
    }

    private static List<SignalEventRecord> recentDeviceEvents(SignalDeviceData device, int limit) {
        List<SignalEventRecord> matches = new ArrayList<>();
        for (SignalEventRecord record : SignalEventHistory.snapshot()) {
            if ("signal_device".equals(record.sourceType()) && device.id().equals(record.sourceId())) {
                matches.add(record);
            }
        }
        int start = Math.max(0, matches.size() - Math.max(0, limit));
        return List.copyOf(matches.subList(start, matches.size()));
    }

    private static CompletableFuture<Suggestions> suggestDevices(ServerCommandSource source, SuggestionsBuilder builder) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (SignalDeviceData device : SignalDeviceStore.getSnapshot(source.getServer())) {
            values.add(device.name());
            values.add(SignalDeviceStore.shortId(device.id()));
            values.add(device.id());
        }
        return CommandSuggestionUtil.suggestStrings(values, builder);
    }

    private static void sendHeader(ServerCommandSource source, Text title) {
        sendDivider(source);
        source.sendFeedback(() -> title, false);
    }

    private static void sendError(ServerCommandSource source, Text message) {
        sendDivider(source);
        source.sendFeedback(() -> Text.literal("错误：").formatted(Formatting.RED).append(message.copy().formatted(Formatting.RED)), false);
    }

    private static void sendDivider(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("===========").formatted(Formatting.AQUA), false);
    }

    private static MutableText field(String label, Text value) {
        return Text.literal(label + "：").formatted(Formatting.GRAY).append(value);
    }

    private static MutableText indentField(String label, Text value) {
        return Text.literal("  " + label + "：").formatted(Formatting.GRAY).append(value);
    }

    private static MutableText warning(String message) {
        return Text.literal(message).formatted(Formatting.YELLOW);
    }

    private static MutableText nameText(String name) {
        return Text.literal(name == null || name.isBlank() ? "未命名信号设备" : name).formatted(Formatting.GOLD);
    }

    private static MutableText idText(String id) {
        return Text.literal(safe(id)).formatted(Formatting.AQUA);
    }

    private static MutableText channelText(String channel) {
        return Text.literal(SignalChannel.normalize(channel)).formatted(Formatting.AQUA);
    }

    private static Text channelOrEmpty(String channel) {
        return channel == null || channel.isBlank()
                ? Text.literal("未绑定").formatted(Formatting.YELLOW)
                : channelText(channel);
    }

    private static MutableText positionText(SignalDeviceData device) {
        return Text.literal(SignalDeviceStore.positionText(device)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText posText(BlockPos pos) {
        return Text.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ()).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText enabledText(boolean enabled) {
        return Text.literal(enabled ? "启用" : "禁用").formatted(enabled ? Formatting.GREEN : Formatting.RED);
    }

    private static Text redstoneText(ServerCommandSource source, SignalDeviceData device) {
        SignalEmitterBlockEntity blockEntity = SignalDeviceStore.getLoadedEmitter(source.getServer(), device);
        if (blockEntity == null) {
            return Text.literal("未知").formatted(Formatting.YELLOW);
        }
        for (ServerWorld world : source.getServer().getWorlds()) {
            if (!world.getRegistryKey().getValue().toString().equals(device.dimension())) {
                continue;
            }
            boolean powered = world.isReceivingRedstonePower(new BlockPos(device.x(), device.y(), device.z()));
            return Text.literal(powered ? "已通电" : "未通电").formatted(powered ? Formatting.GREEN : Formatting.GRAY);
        }
        return Text.literal("未知").formatted(Formatting.YELLOW);
    }

    private static MutableText consistencyText(SignalDeviceData device, SignalEmitterBlockEntity blockEntity) {
        if (blockEntity == null) {
            return Text.literal("无法比较").formatted(Formatting.YELLOW);
        }
        boolean same = SignalChannel.normalize(blockEntity.channel()).equals(SignalChannel.normalize(device.channel()))
                && blockEntity.enabled() == device.enabled();
        return Text.literal(same ? "一致" : "不一致").formatted(same ? Formatting.GREEN : Formatting.YELLOW);
    }

    private static MutableText elapsedOrNever(long wallTimeMillis) {
        if (wallTimeMillis <= 0L) {
            return Text.literal("尚未触发").formatted(Formatting.YELLOW);
        }
        return elapsedText(wallTimeMillis);
    }

    private static MutableText elapsedText(long wallTimeMillis) {
        return Text.literal(formatElapsed(System.currentTimeMillis() - wallTimeMillis)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText number(int value) {
        return Text.literal(Integer.toString(value)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText resultText(String value) {
        return Text.literal(value == null || value.isBlank() ? "尚无结果" : value).formatted(value == null || value.isBlank()
                ? Formatting.YELLOW
                : Formatting.WHITE);
    }

    private static MutableText resultText(SignalEventRecord record) {
        return Text.literal(safe(record.resultMessage())).formatted(record.failedCount() > 0 ? Formatting.RED : Formatting.GREEN);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "未知" : value;
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
}
