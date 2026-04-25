package com.zcpu.tzzmod.signal.device;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zcpu.tzzmod.ModBlock.entity.SignalEmitterBlockEntity;
import com.zcpu.tzzmod.ModBlock.entity.SignalReceiverBlockEntity;
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
            source.sendFeedback(() -> indentField("类型", typeText(device)), false);
            source.sendFeedback(() -> indentField("频道", channelOrEmpty(device.channel())), false);
            source.sendFeedback(() -> indentField("位置", positionText(device)), false);
            source.sendFeedback(() -> indentField("状态", enabledText(device.enabled())), false);
            if (isReceiver(device)) {
                source.sendFeedback(() -> indentField("脉冲时长", gtText(device.pulseTicks())), false);
            }
            source.sendFeedback(() -> indentField(isReceiver(device) ? "最近接收" : "最近触发",
                    elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        }
        if (devices.size() > shown) {
            source.sendFeedback(() -> warning("仅显示前 " + shown + " 个，共 " + devices.size() + " 个。"), false);
        }
        return 1;
    }

    private static int executeBind(ServerCommandSource source, BlockPos pos, String rawChannel) {
        LoadedDevice loadedDevice = getDeviceAt(source, pos);
        if (loadedDevice == null) {
            return 0;
        }

        String channel = SignalChannel.normalize(rawChannel);
        if (!SignalChannel.isValid(channel)) {
            sendError(source, SignalChannel.validationError(rawChannel));
            return 0;
        }

        SignalDeviceData device;
        if (loadedDevice.emitter() != null) {
            loadedDevice.emitter().setChannel(channel);
            device = SignalDeviceStore.updateChannel(source.getWorld(), pos, loadedDevice.emitter());
        } else {
            loadedDevice.receiver().setChannel(channel);
            device = SignalDeviceStore.updateChannel(source.getWorld(), pos, loadedDevice.receiver());
        }

        sendHeader(source, Text.literal("已绑定信号设备频道").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("类型", typeText(device)), false);
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("频道", channelText(channel)), false);
        return 1;
    }

    private static int executeName(ServerCommandSource source, BlockPos pos, String rawName) {
        LoadedDevice loadedDevice = getDeviceAt(source, pos);
        if (loadedDevice == null) {
            return 0;
        }

        String name = SignalDeviceStore.cleanUserText(rawName);
        if (name.isBlank()) {
            sendError(source, Text.literal("设备名称不能为空。"));
            return 0;
        }

        SignalDeviceData device = loadedDevice.emitter() != null
                ? SignalDeviceStore.setName(source.getWorld(), pos, loadedDevice.emitter(), name)
                : SignalDeviceStore.setName(source.getWorld(), pos, loadedDevice.receiver(), name);
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
        LoadedDevice loadedDevice = getDeviceAt(source, pos);
        if (loadedDevice == null) {
            return 0;
        }

        SignalDeviceData device = upsertLoaded(source.getWorld(), pos, loadedDevice);
        sendInfo(source, device, true);
        return 1;
    }

    private static int executeInfoDevice(ServerCommandSource source, String deviceRef) {
        SignalDeviceStore.ResolveResult resolved = resolveDevice(source, deviceRef);
        if (!resolved.foundUnique()) {
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.refreshLoadedState(source.getServer(), resolved.device());
        sendInfo(source, device, isLoaded(source, device));
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
            source.sendFeedback(() -> indentField("来源", idText(record.sourceId())), false);
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
        SignalEmitterBlockEntity emitter = SignalDeviceStore.getLoadedEmitter(source.getServer(), device);
        SignalReceiverBlockEntity receiver = SignalDeviceStore.getLoadedReceiver(source.getServer(), device);
        List<SignalListenerData> listeners = SignalChannelInspector.getListenersForChannel(source.getServer(), device.channel());
        int enabledListeners = 0;
        int actionCount = 0;
        for (SignalListenerData listener : listeners) {
            if (listener.enabled()) {
                enabledListeners++;
            }
            actionCount += listener.actions() == null ? 0 : listener.actions().size();
        }
        int receiverCount = receiverCountForChannel(source, device.channel());
        int finalEnabledListeners = enabledListeners;
        int finalActionCount = actionCount;

        sendHeader(source, Text.literal("信号设备调试信息").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("名称", nameText(SignalDeviceStore.displayName(device))), false);
        source.sendFeedback(() -> field("ID", idText(device.id())), false);
        source.sendFeedback(() -> field("短ID", idText(SignalDeviceStore.shortId(device.id()))), false);
        source.sendFeedback(() -> field("类型", typeText(device)), false);
        source.sendFeedback(() -> field("位置", positionText(device)), false);
        source.sendFeedback(() -> field("频道", channelOrEmpty(device.channel())), false);
        source.sendFeedback(() -> field("状态", enabledText(device.enabled())), false);
        if (isReceiver(device)) {
            source.sendFeedback(() -> field("脉冲时长", gtText(device.pulseTicks())), false);
            source.sendFeedback(() -> field("剩余脉冲", number(device.remainingPulseTicks()).append(Text.literal(" GT").formatted(Formatting.GRAY))), false);
            source.sendFeedback(() -> field("红石输出", receiverOutputText(source, device)), false);
        } else {
            source.sendFeedback(() -> field("红石输入", emitterRedstoneText(source, device)), false);
        }
        source.sendFeedback(() -> field(isReceiver(device) ? "最近接收" : "最近触发",
                elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        source.sendFeedback(() -> field("最近结果", resultText(device.lastResult())), false);
        source.sendFeedback(() -> field("方块实体", emitter == null && receiver == null
                ? Text.literal("方块未加载或已不存在").formatted(Formatting.YELLOW)
                : Text.literal("已加载并匹配").formatted(Formatting.GREEN)), false);
        source.sendFeedback(() -> field("状态一致性", consistencyText(device, emitter, receiver)), false);
        source.sendFeedback(() -> field("频道有效性", SignalChannel.isValid(device.channel())
                ? Text.literal("有效").formatted(Formatting.GREEN)
                : Text.literal("无效或未绑定").formatted(Formatting.YELLOW)), false);
        source.sendFeedback(() -> field("频道监听器", number(listeners.size())
                .append(Text.literal(" 个，启用 ").formatted(Formatting.GRAY))
                .append(number(finalEnabledListeners))
                .append(Text.literal(" 个，动作 ").formatted(Formatting.GRAY))
                .append(number(finalActionCount))
                .append(Text.literal(" 个").formatted(Formatting.GRAY))), false);
        if (isReceiver(device)) {
            source.sendFeedback(() -> field("同频道接收器", number(receiverCount)), false);
        }

        List<Text> hints = debugHints(device, emitter, receiver, listeners, receiverCount);
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
        LoadedDevice loadedDevice = getDeviceAt(source, pos);
        if (loadedDevice == null) {
            return 0;
        }

        ActionExecutionResult result;
        SignalDeviceData device;
        if (loadedDevice.emitter() != null) {
            ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
            result = loadedDevice.emitter().emitSignal(source.getWorld(), player);
            if (loadedDevice.emitter().enabled()
                    && !loadedDevice.emitter().channel().isBlank()
                    && SignalChannel.isValid(loadedDevice.emitter().channel())) {
                SignalDeviceStore.recordTrigger(source.getWorld(), pos, loadedDevice.emitter(), result);
            }
            device = SignalDeviceStore.upsertEmitter(source.getWorld(), pos, loadedDevice.emitter());
        } else {
            result = loadedDevice.receiver().receiveSignal(source.getWorld());
            device = SignalDeviceStore.upsertReceiver(source.getWorld(), pos, loadedDevice.receiver());
        }

        if (!result.success()) {
            sendError(source, result.message());
            return 0;
        }

        sendHeader(source, Text.literal(loadedDevice.emitter() != null ? "已测试信号发射器" : "已测试信号接收器")
                .formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("类型", typeText(device)), false);
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("频道", channelText(device.channel())), false);
        source.sendFeedback(() -> field("结果", result.message()), false);
        return 1;
    }

    private static int executeSetEnabled(ServerCommandSource source, BlockPos pos, boolean enabled) {
        LoadedDevice loadedDevice = getDeviceAt(source, pos);
        if (loadedDevice == null) {
            return 0;
        }

        SignalDeviceData device;
        if (loadedDevice.emitter() != null) {
            loadedDevice.emitter().setEnabled(enabled);
            device = SignalDeviceStore.updateEnabled(source.getWorld(), pos, loadedDevice.emitter());
        } else {
            loadedDevice.receiver().setEnabled(enabled);
            device = SignalDeviceStore.updateEnabled(source.getWorld(), pos, loadedDevice.receiver());
        }

        sendHeader(source, Text.literal(enabled ? "已启用信号设备" : "已禁用信号设备")
                .formatted(enabled ? Formatting.GREEN : Formatting.RED));
        source.sendFeedback(() -> field("类型", typeText(device)), false);
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("状态", enabledText(enabled)), false);
        return 1;
    }

    private static LoadedDevice getDeviceAt(ServerCommandSource source, BlockPos pos) {
        if (source.getWorld().getBlockEntity(pos) instanceof SignalEmitterBlockEntity emitter) {
            return new LoadedDevice(emitter, null);
        }
        if (source.getWorld().getBlockEntity(pos) instanceof SignalReceiverBlockEntity receiver) {
            return new LoadedDevice(null, receiver);
        }

        sendError(source, Text.literal("该位置不是信号设备。"));
        return null;
    }

    private static SignalDeviceData upsertLoaded(ServerWorld world, BlockPos pos, LoadedDevice loadedDevice) {
        return loadedDevice.emitter() != null
                ? SignalDeviceStore.upsertEmitter(world, pos, loadedDevice.emitter())
                : SignalDeviceStore.upsertReceiver(world, pos, loadedDevice.receiver());
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
        source.sendFeedback(() -> field("类型", typeText(device)), false);
        source.sendFeedback(() -> field("位置", positionText(device)), false);
        source.sendFeedback(() -> field("频道", channelOrEmpty(device.channel())), false);
        source.sendFeedback(() -> field("状态", enabledText(device.enabled())), false);
        if (isReceiver(device)) {
            source.sendFeedback(() -> field("脉冲时长", gtText(device.pulseTicks())), false);
            source.sendFeedback(() -> field("当前输出", receiverOutputText(source, device)), false);
            source.sendFeedback(() -> field("剩余 ticks", number(device.remainingPulseTicks())), false);
            source.sendFeedback(() -> field("最近接收", elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        } else {
            source.sendFeedback(() -> field("红石输入", emitterRedstoneText(source, device)), false);
            source.sendFeedback(() -> field("最近触发", elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        }
        source.sendFeedback(() -> field("最近结果", resultText(device.lastResult())), false);
        source.sendFeedback(() -> field("状态来源", loaded
                ? Text.literal("已加载方块实体").formatted(Formatting.GREEN)
                : Text.literal("设备注册表，方块未加载或不匹配").formatted(Formatting.YELLOW)), false);
    }

    private static List<Text> debugHints(
            SignalDeviceData device,
            SignalEmitterBlockEntity emitter,
            SignalReceiverBlockEntity receiver,
            List<SignalListenerData> listeners,
            int receiverCount
    ) {
        List<Text> hints = new ArrayList<>();
        if (device.channel().isBlank()) {
            hints.add(Text.literal("未绑定频道。").formatted(Formatting.YELLOW));
        }
        if (!device.enabled()) {
            hints.add(Text.literal("设备已禁用。").formatted(Formatting.YELLOW));
        }
        if (!device.channel().isBlank() && !SignalChannel.isValid(device.channel())) {
            hints.add(Text.literal("频道名称无效。").formatted(Formatting.RED));
        }
        if (isReceiver(device)) {
            if (device.pulseTicks() < SignalReceiverBlockEntity.MIN_PULSE_TICKS
                    || device.pulseTicks() > SignalReceiverBlockEntity.MAX_PULSE_TICKS) {
                hints.add(Text.literal("pulseTicks 超出允许范围。").formatted(Formatting.RED));
            }
            if (receiver == null) {
                hints.add(Text.literal("方块未加载或已不存在。").formatted(Formatting.YELLOW));
            } else if (!SignalChannel.normalize(receiver.channel()).equals(SignalChannel.normalize(device.channel()))
                    || receiver.enabled() != device.enabled()
                    || receiver.pulseTicks() != device.pulseTicks()) {
                hints.add(Text.literal("registry 与 BlockEntity 状态不一致。").formatted(Formatting.YELLOW));
            }
            if (!device.channel().isBlank() && listeners.isEmpty()) {
                hints.add(Text.literal("频道没有 listener；接收器仍可独立输出红石。").formatted(Formatting.YELLOW));
            }
            if (!device.channel().isBlank() && receiverCount <= 0) {
                hints.add(Text.literal("当前登记表没有启用的同频道接收器。").formatted(Formatting.YELLOW));
            }
        } else {
            if (emitter == null) {
                hints.add(Text.literal("方块未加载或已不存在。").formatted(Formatting.YELLOW));
            } else if (!SignalChannel.normalize(emitter.channel()).equals(SignalChannel.normalize(device.channel()))
                    || emitter.enabled() != device.enabled()) {
                hints.add(Text.literal("registry 与 BlockEntity 状态不一致。").formatted(Formatting.YELLOW));
            }
            if (!device.channel().isBlank() && listeners.isEmpty() && receiverCount <= 0) {
                hints.add(Text.literal("频道没有 listener，也没有启用的接收器。").formatted(Formatting.YELLOW));
            }
        }
        return hints;
    }

    private static List<SignalEventRecord> recentDeviceEvents(SignalDeviceData device, int limit) {
        List<SignalEventRecord> matches = new ArrayList<>();
        for (SignalEventRecord record : SignalEventHistory.snapshot()) {
            if (isReceiver(device)) {
                if (SignalChannel.normalize(record.channel()).equals(SignalChannel.normalize(device.channel()))) {
                    matches.add(record);
                }
            } else if ("signal_device".equals(record.sourceType()) && device.id().equals(record.sourceId())) {
                matches.add(record);
            }
        }
        int start = Math.max(0, matches.size() - Math.max(0, limit));
        return List.copyOf(matches.subList(start, matches.size()));
    }

    private static int receiverCountForChannel(ServerCommandSource source, String channel) {
        String normalizedChannel = SignalChannel.normalize(channel);
        int count = 0;
        for (SignalDeviceData device : SignalDeviceStore.getSnapshot(source.getServer())) {
            if (isReceiver(device) && device.enabled() && SignalChannel.normalize(device.channel()).equals(normalizedChannel)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isLoaded(ServerCommandSource source, SignalDeviceData device) {
        return isReceiver(device)
                ? SignalDeviceStore.getLoadedReceiver(source.getServer(), device) != null
                : SignalDeviceStore.getLoadedEmitter(source.getServer(), device) != null;
    }

    private static boolean isReceiver(SignalDeviceData device) {
        return device != null && SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(device.type());
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

    private static MutableText typeText(SignalDeviceData device) {
        return Text.literal(device.type()).formatted(isReceiver(device) ? Formatting.RED : Formatting.WHITE);
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

    private static MutableText gtText(int ticks) {
        return Text.literal(ticks + " GT").formatted(Formatting.LIGHT_PURPLE);
    }

    private static Text emitterRedstoneText(ServerCommandSource source, SignalDeviceData device) {
        SignalEmitterBlockEntity blockEntity = SignalDeviceStore.getLoadedEmitter(source.getServer(), device);
        if (blockEntity == null) {
            return Text.literal("未知").formatted(Formatting.YELLOW);
        }
        ServerWorld world = SignalDeviceStore.getDeviceWorld(source.getServer(), device);
        if (world == null) {
            return Text.literal("未知").formatted(Formatting.YELLOW);
        }
        boolean powered = world.isReceivingRedstonePower(new BlockPos(device.x(), device.y(), device.z()));
        return Text.literal(powered ? "已通电" : "未通电").formatted(powered ? Formatting.GREEN : Formatting.GRAY);
    }

    private static Text receiverOutputText(ServerCommandSource source, SignalDeviceData device) {
        SignalReceiverBlockEntity blockEntity = SignalDeviceStore.getLoadedReceiver(source.getServer(), device);
        if (blockEntity == null) {
            return Text.literal("未知").formatted(Formatting.YELLOW);
        }
        boolean powered = blockEntity.remainingPulseTicks() > 0;
        return Text.literal(powered ? "正在输出" : "未输出").formatted(powered ? Formatting.RED : Formatting.GRAY);
    }

    private static MutableText consistencyText(
            SignalDeviceData device,
            SignalEmitterBlockEntity emitter,
            SignalReceiverBlockEntity receiver
    ) {
        if (isReceiver(device)) {
            if (receiver == null) {
                return Text.literal("无法比较").formatted(Formatting.YELLOW);
            }
            boolean same = SignalChannel.normalize(receiver.channel()).equals(SignalChannel.normalize(device.channel()))
                    && receiver.enabled() == device.enabled()
                    && receiver.pulseTicks() == device.pulseTicks();
            return Text.literal(same ? "一致" : "不一致").formatted(same ? Formatting.GREEN : Formatting.YELLOW);
        }

        if (emitter == null) {
            return Text.literal("无法比较").formatted(Formatting.YELLOW);
        }
        boolean same = SignalChannel.normalize(emitter.channel()).equals(SignalChannel.normalize(device.channel()))
                && emitter.enabled() == device.enabled();
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

    private record LoadedDevice(
            SignalEmitterBlockEntity emitter,
            SignalReceiverBlockEntity receiver
    ) {
    }
}
