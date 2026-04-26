package com.zcpu.tzzmod.signal.device;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.ModBlock.entity.SignalEmitterBlockEntity;
import com.zcpu.tzzmod.ModBlock.entity.SignalReceiverBlockEntity;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.command.CommandSuggestionUtil;
import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalChannelInspector;
import com.zcpu.tzzmod.signal.SignalEvent;
import com.zcpu.tzzmod.signal.SignalEventHistory;
import com.zcpu.tzzmod.signal.SignalEventRecord;
import com.zcpu.tzzmod.signal.SignalListenerData;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherSupport;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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
                .then(CommandManager.literal("cleanup")
                        .executes(context -> executeCleanup(context.getSource())))
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
            } else if (isActionRelay(device)) {
                source.sendFeedback(() -> indentField("动作数量", number(device.actionCount())), false);
                source.sendFeedback(() -> indentField("冷却", gtText(device.cooldownTicks())), false);
            } else if (isVirtualBlockDevice(device)) {
                source.sendFeedback(() -> indentField("方块 ID", idText(device.blockId())), false);
                source.sendFeedback(() -> indentField("断电频道", channelOrEmpty(device.offChannel())), false);
                source.sendFeedback(() -> indentField("模式", modeText(device.mode())), false);
                if (device.conditionEnabled()) {
                    source.sendFeedback(() -> indentField("条件", conditionSummary(device)), false);
                }
                if (device.interactionEnabled() || !device.interactChannel().isBlank()) {
                    source.sendFeedback(() -> indentField("交互频道", channelOrEmpty(device.interactChannel())), false);
                    source.sendFeedback(() -> indentField("交互触发", boolText(device.interactionEnabled())), false);
                    source.sendFeedback(() -> indentField("交互冷却", gtText(device.interactionCooldownTicks())), false);
                }
                if (device.containerEnabled()
                        || !device.containerOpenChannel().isBlank()
                        || !device.containerCloseChannel().isBlank()
                        || !device.containerChangeChannel().isBlank()) {
                    source.sendFeedback(() -> indentField("容器事件", boolText(device.containerEnabled())), false);
                    source.sendFeedback(() -> indentField("打开频道", channelOrEmpty(device.containerOpenChannel())), false);
                    source.sendFeedback(() -> indentField("关闭频道", channelOrEmpty(device.containerCloseChannel())), false);
                    source.sendFeedback(() -> indentField("内容变化频道", channelOrEmpty(device.containerChangeChannel())), false);
                    source.sendFeedback(() -> indentField("检查间隔", gtText(device.containerChangeCheckIntervalTicks())), false);
                }
                if (!device.itemConditions().isEmpty()) {
                    source.sendFeedback(() -> indentField("物品条件", itemConditionCountText(device)), false);
                    source.sendFeedback(() -> indentField("最近物品条件", latestItemConditionTriggerText(device)), false);
                }
                source.sendFeedback(() -> indentField("通电状态", boolText(device.lastPowered())
                        .append(Text.literal("，强度 ").formatted(Formatting.GRAY))
                        .append(number(device.lastPowerLevel()))), false);
            }
            source.sendFeedback(() -> indentField(isReceiver(device) ? "最近接收" : isActionRelay(device) ? "最近执行" : "最近触发",
                    elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        }
        if (devices.size() > shown) {
            source.sendFeedback(() -> warning("仅显示前 " + shown + " 个，共 " + devices.size() + " 个。"), false);
        }
        return 1;
    }

    private static int executeCleanup(ServerCommandSource source) {
        int removed = SignalDeviceStore.cleanupInvalidLoadedDevices(source.getServer());
        sendHeader(source, Text.literal("已清理无效信号设备记录").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("清理数量", number(removed)), false);
        source.sendFeedback(() -> warning("仅检查已登记且所在区块已加载的设备；未加载区块不会被强制加载。"), false);
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
        } else if (loadedDevice.receiver() != null) {
            loadedDevice.receiver().setChannel(channel);
            device = SignalDeviceStore.updateChannel(source.getWorld(), pos, loadedDevice.receiver());
        } else {
            loadedDevice.relay().setChannel(channel);
            device = SignalDeviceStore.updateChannel(source.getWorld(), pos, loadedDevice.relay());
        }

        sendHeader(source, Text.literal("已绑定信号设备频道").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("类型", typeText(device)), false);
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("频道", channelText(channel)), false);
        return 1;
    }

    private static int executeName(ServerCommandSource source, BlockPos pos, String rawName) {
        String name = SignalDeviceStore.cleanUserText(rawName);
        if (name.isBlank()) {
            sendError(source, Text.literal("设备名称不能为空。"));
            return 0;
        }

        LoadedDevice loadedDevice = findLoadedDeviceAt(source, pos);
        if (loadedDevice == null) {
            SignalDeviceData virtualDevice = SignalDeviceStore.findVirtualBlockDevice(source.getServer(), source.getWorld(), pos);
            if (virtualDevice == null) {
                sendError(source, Text.literal("该位置不是信号设备。"));
                return 0;
            }

            SignalDeviceData device = SignalDeviceStore.setVirtualName(source.getWorld(), pos, name);
            sendHeader(source, Text.literal("已命名信号设备").formatted(Formatting.GREEN));
            source.sendFeedback(() -> field("位置", positionText(device)), false);
            source.sendFeedback(() -> field("名称", nameText(device.name())), false);
            return 1;
        }

        SignalDeviceData device = loadedDevice.emitter() != null
                ? SignalDeviceStore.setName(source.getWorld(), pos, loadedDevice.emitter(), name)
                : loadedDevice.receiver() != null
                        ? SignalDeviceStore.setName(source.getWorld(), pos, loadedDevice.receiver(), name)
                        : SignalDeviceStore.setName(source.getWorld(), pos, loadedDevice.relay(), name);
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
        LoadedDevice loadedDevice = findLoadedDeviceAt(source, pos);
        if (loadedDevice != null) {
            SignalDeviceData device = upsertLoaded(source.getWorld(), pos, loadedDevice);
            sendInfo(source, device, true);
            return 1;
        }

        SignalDeviceData device = SignalDeviceStore.findVirtualBlockDevice(source.getServer(), source.getWorld(), pos);
        if (device == null) {
            sendError(source, Text.literal("该位置不是信号设备。"));
            return 0;
        }

        sendInfo(source, device, isLoaded(source, device));
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
        if (isVirtualBlockDevice(device)) {
            sendVirtualDebug(source, device);
            return 1;
        }

        SignalEmitterBlockEntity emitter = SignalDeviceStore.getLoadedEmitter(source.getServer(), device);
        SignalReceiverBlockEntity receiver = SignalDeviceStore.getLoadedReceiver(source.getServer(), device);
        ActionRelayBlockEntity relay = SignalDeviceStore.getLoadedActionRelay(source.getServer(), device);
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
        int relayCount = actionRelayCountForChannel(source, device.channel());
        int invalidActionCount = relay == null ? 0 : invalidActionCount(relay);
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
        } else if (isActionRelay(device)) {
            source.sendFeedback(() -> field("冷却", gtText(device.cooldownTicks())), false);
            source.sendFeedback(() -> field("当前冷却剩余", gtText(relay == null ? 0 : (int) relay.remainingCooldownTicks(source.getWorld().getTime()))), false);
            source.sendFeedback(() -> field("动作数量", number(device.actionCount())), false);
            source.sendFeedback(() -> field("动作配置异常", number(invalidActionCount)), false);
        } else {
            source.sendFeedback(() -> field("红石输入", emitterRedstoneText(source, device)), false);
        }
        source.sendFeedback(() -> field(isReceiver(device) ? "最近接收" : isActionRelay(device) ? "最近执行" : "最近触发",
                elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        source.sendFeedback(() -> field("最近结果", resultText(device.lastResult())), false);
        source.sendFeedback(() -> field("方块实体", emitter == null && receiver == null && relay == null
                ? Text.literal("方块未加载或已不存在").formatted(Formatting.YELLOW)
                : Text.literal("已加载并匹配").formatted(Formatting.GREEN)), false);
        source.sendFeedback(() -> field("状态一致性", consistencyText(device, emitter, receiver, relay)), false);
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
        } else if (isActionRelay(device)) {
            source.sendFeedback(() -> field("同频道接收器", number(receiverCount)), false);
            source.sendFeedback(() -> field("同频道动作继电器", number(relayCount)), false);
        }

        List<Text> hints = debugHints(device, emitter, receiver, relay, listeners, receiverCount, relayCount, source.getWorld().getTime());
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
        LoadedDevice loadedDevice = findLoadedDeviceAt(source, pos);
        if (loadedDevice == null) {
            SignalDeviceData virtualDevice = SignalDeviceStore.findVirtualBlockDevice(source.getServer(), source.getWorld(), pos);
            if (virtualDevice != null) {
                return executeVirtualTest(source, pos, virtualDevice);
            }
            sendError(source, Text.literal("该位置不是信号设备。"));
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
        } else if (loadedDevice.receiver() != null) {
            result = loadedDevice.receiver().receiveSignal(source.getWorld());
            device = SignalDeviceStore.upsertReceiver(source.getWorld(), pos, loadedDevice.receiver());
        } else {
            ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
            result = loadedDevice.relay().executeRelayActions(source.getWorld(), player, true);
            device = SignalDeviceStore.upsertActionRelay(source.getWorld(), pos, loadedDevice.relay());
        }

        if (!result.success()) {
            sendError(source, result.message());
            return 0;
        }

        sendHeader(source, Text.literal(loadedDevice.emitter() != null ? "已测试信号发射器"
                        : loadedDevice.receiver() != null ? "已测试信号接收器" : "已测试动作继电器")
                .formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("类型", typeText(device)), false);
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("频道", channelText(device.channel())), false);
        source.sendFeedback(() -> field("结果", result.message()), false);
        return 1;
    }

    private static int executeSetEnabled(ServerCommandSource source, BlockPos pos, boolean enabled) {
        LoadedDevice loadedDevice = findLoadedDeviceAt(source, pos);
        if (loadedDevice == null) {
            SignalDeviceData virtualDevice = SignalDeviceStore.findVirtualBlockDevice(source.getServer(), source.getWorld(), pos);
            if (virtualDevice != null) {
                SignalDeviceData device = SignalDeviceStore.updateVirtualEnabled(source.getWorld(), pos, enabled);
                sendHeader(source, Text.literal(enabled ? "已启用信号设备" : "已禁用信号设备")
                        .formatted(enabled ? Formatting.GREEN : Formatting.RED));
                source.sendFeedback(() -> field("类型", typeText(device)), false);
                source.sendFeedback(() -> field("位置", posText(pos)), false);
                source.sendFeedback(() -> field("状态", enabledText(enabled)), false);
                return 1;
            }
            sendError(source, Text.literal("该位置不是信号设备。"));
            return 0;
        }

        SignalDeviceData device;
        if (loadedDevice.emitter() != null) {
            loadedDevice.emitter().setEnabled(enabled);
            device = SignalDeviceStore.updateEnabled(source.getWorld(), pos, loadedDevice.emitter());
        } else if (loadedDevice.receiver() != null) {
            loadedDevice.receiver().setEnabled(enabled);
            device = SignalDeviceStore.updateEnabled(source.getWorld(), pos, loadedDevice.receiver());
        } else {
            loadedDevice.relay().setEnabled(enabled);
            device = SignalDeviceStore.updateEnabled(source.getWorld(), pos, loadedDevice.relay());
        }

        sendHeader(source, Text.literal(enabled ? "已启用信号设备" : "已禁用信号设备")
                .formatted(enabled ? Formatting.GREEN : Formatting.RED));
        source.sendFeedback(() -> field("类型", typeText(device)), false);
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("状态", enabledText(enabled)), false);
        return 1;
    }

    private static int executeVirtualTest(ServerCommandSource source, BlockPos pos, SignalDeviceData device) {
        if (device.channel().isBlank() || !SignalChannel.isValid(device.channel())) {
            sendError(source, Text.literal("虚拟方块发射器频道未绑定或无效。"));
            return 0;
        }

        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
        ActionExecutionResult result = SignalBridgeServer.emit(new SignalEvent(
                device.channel(),
                player,
                source.getWorld(),
                Vec3d.ofCenter(pos),
                ActionSourceType.VIRTUAL_BLOCK_DEVICE,
                device.id(),
                SignalBridgeServer.currentDepth(),
                source.getWorld().getTime()
        ));
        SignalDeviceStore.recordVirtualBlockManualTrigger(source.getWorld(), device, result);
        if (!result.success()) {
            sendError(source, result.message());
            return 0;
        }

        sendHeader(source, Text.literal("已测试虚拟方块发射器").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("类型", typeText(device)), false);
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("频道", channelText(device.channel())), false);
        source.sendFeedback(() -> field("结果", result.message()), false);
        return 1;
    }

    private static LoadedDevice getDeviceAt(ServerCommandSource source, BlockPos pos) {
        LoadedDevice loadedDevice = findLoadedDeviceAt(source, pos);
        if (loadedDevice != null) {
            return loadedDevice;
        }

        sendError(source, Text.literal("该位置不是信号设备。"));
        return null;
    }

    private static LoadedDevice findLoadedDeviceAt(ServerCommandSource source, BlockPos pos) {
        if (source.getWorld().getBlockEntity(pos) instanceof SignalEmitterBlockEntity emitter) {
            return new LoadedDevice(emitter, null, null);
        }
        if (source.getWorld().getBlockEntity(pos) instanceof SignalReceiverBlockEntity receiver) {
            return new LoadedDevice(null, receiver, null);
        }
        if (source.getWorld().getBlockEntity(pos) instanceof ActionRelayBlockEntity relay) {
            return new LoadedDevice(null, null, relay);
        }

        return null;
    }

    private static SignalDeviceData upsertLoaded(ServerWorld world, BlockPos pos, LoadedDevice loadedDevice) {
        return loadedDevice.emitter() != null
                ? SignalDeviceStore.upsertEmitter(world, pos, loadedDevice.emitter())
                : loadedDevice.receiver() != null
                        ? SignalDeviceStore.upsertReceiver(world, pos, loadedDevice.receiver())
                        : SignalDeviceStore.upsertActionRelay(world, pos, loadedDevice.relay());
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
        } else if (isActionRelay(device)) {
            source.sendFeedback(() -> field("冷却", gtText(device.cooldownTicks())), false);
            source.sendFeedback(() -> field("动作数量", number(device.actionCount())), false);
            source.sendFeedback(() -> field("最近执行", elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        } else if (isVirtualBlockDevice(device)) {
            VirtualBlockPowerState powerState = virtualPowerState(source, device);
            source.sendFeedback(() -> field("绑定时方块 ID", idText(device.blockId())), false);
            source.sendFeedback(() -> field("当前方块 ID", powerState == null ? unknownText() : idText(powerState.blockId())), false);
            source.sendFeedback(() -> field("BlockState powered", powerState == null ? unknownText() : boolText(powerState.blockStatePowered())), false);
            source.sendFeedback(() -> field("接收红石强度", powerState == null ? unknownText() : number(powerState.receivedPowerLevel())), false);
            source.sendFeedback(() -> field("当前通电", powerState == null ? unknownText() : boolText(powerState.currentPowered())), false);
            source.sendFeedback(() -> field("上次通电", boolText(device.lastPowered())), false);
            source.sendFeedback(() -> field("上次红石强度", number(device.lastPowerLevel())), false);
            source.sendFeedback(() -> field("断电频道", channelOrEmpty(device.offChannel())), false);
            source.sendFeedback(() -> field("模式", modeText(device.mode())), false);
            source.sendFeedback(() -> field("方块状态条件", conditionSummary(device)), false);
            source.sendFeedback(() -> field("条件模式", conditionModeText(device.conditionMode())), false);
            source.sendFeedback(() -> field("上次条件满足", boolText(device.lastConditionMatched())), false);
            source.sendFeedback(() -> field("当前条件满足", conditionMatchedText(source, device)), false);
            source.sendFeedback(() -> field("交互触发", boolText(device.interactionEnabled())), false);
            source.sendFeedback(() -> field("交互频道", channelOrEmpty(device.interactChannel())), false);
            source.sendFeedback(() -> field("交互冷却", gtText(device.interactionCooldownTicks())), false);
            source.sendFeedback(() -> field("交互冷却剩余", remainingInteractionCooldownText(device, source.getWorld().getTime())), false);
            source.sendFeedback(() -> field("最近交互", elapsedOrNever(device.lastInteractionWallTimeMillis())), false);
            source.sendFeedback(() -> field("最近交互玩家", playerOrNever(device.lastInteractionPlayerName())), false);
            source.sendFeedback(() -> field("主手物品匹配", boolText(device.interactionItemMatcherEnabled())), false);
            source.sendFeedback(() -> field("匹配成功频道", channelOrEmpty(device.interactionItemMatcher().successChannel())), false);
            source.sendFeedback(() -> field("匹配失败频道", channelOrEmpty(device.interactionItemMatcher().failChannel())), false);
            source.sendFeedback(() -> field("成功消耗", boolText(device.interactionItemMatcher().consumeEnabled())), false);
            source.sendFeedback(() -> field("消耗数量", number(device.interactionItemMatcher().consumeCount())), false);
            source.sendFeedback(() -> field("最近物品匹配结果", resultText(device.lastInteractionItemResult())), false);
            source.sendFeedback(() -> field("容器事件", boolText(device.containerEnabled())), false);
            source.sendFeedback(() -> field("容器打开频道", channelOrEmpty(device.containerOpenChannel())), false);
            source.sendFeedback(() -> field("容器关闭频道", channelOrEmpty(device.containerCloseChannel())), false);
            source.sendFeedback(() -> field("容器内容变化频道", channelOrEmpty(device.containerChangeChannel())), false);
            source.sendFeedback(() -> field("容器冷却", gtText(device.containerCooldownTicks())), false);
            source.sendFeedback(() -> field("内容检查间隔", gtText(device.containerChangeCheckIntervalTicks())), false);
            source.sendFeedback(() -> field("最近容器事件", device.lastContainerEventType().isBlank()
                    ? Text.literal("尚无记录").formatted(Formatting.YELLOW)
                    : Text.literal(device.lastContainerEventType()).formatted(Formatting.LIGHT_PURPLE)), false);
            source.sendFeedback(() -> field("最近容器结果", resultText(device.lastContainerResult())), false);
            source.sendFeedback(() -> field("物品条件", itemConditionCountText(device)), false);
            source.sendFeedback(() -> field("最近物品条件触发", latestItemConditionTriggerText(device)), false);
            source.sendFeedback(() -> field("最近物品条件结果", latestItemConditionResultText(device)), false);
            source.sendFeedback(() -> field("最近触发", elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        } else {
            source.sendFeedback(() -> field("红石输入", emitterRedstoneText(source, device)), false);
            source.sendFeedback(() -> field("最近触发", elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        }
        source.sendFeedback(() -> field("最近结果", resultText(device.lastResult())), false);
        source.sendFeedback(() -> field("状态来源", loaded
                ? Text.literal("已加载方块实体").formatted(Formatting.GREEN)
                : Text.literal("设备注册表，方块未加载或不匹配").formatted(Formatting.YELLOW)), false);
    }

    private static void sendVirtualDebug(ServerCommandSource source, SignalDeviceData device) {
        ServerWorld world = SignalDeviceStore.getDeviceWorld(source.getServer(), device);
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        boolean chunkLoaded = world != null && world.isChunkLoaded(pos);
        VirtualBlockPowerState powerState = chunkLoaded ? VirtualBlockDeviceSupport.powerState(world, pos) : null;
        BlockState currentState = chunkLoaded ? world.getBlockState(pos) : null;
        List<SignalListenerData> listeners = SignalChannelInspector.getListenersForChannel(source.getServer(), device.channel());
        int receiverCount = receiverCountForChannel(source, device.channel());
        int relayCount = actionRelayCountForChannel(source, device.channel());
        List<SignalListenerData> interactionListeners = device.interactChannel().isBlank()
                ? List.of()
                : SignalChannelInspector.getListenersForChannel(source.getServer(), device.interactChannel());
        int interactionReceiverCount = receiverCountForChannel(source, device.interactChannel());
        int interactionRelayCount = actionRelayCountForChannel(source, device.interactChannel());
        long remainingInteractionCooldown = SignalDeviceStore.getRemainingInteractionCooldownTicks(device, source.getWorld().getTime());
        long remainingContainerCooldown = SignalDeviceStore.getRemainingContainerCooldownTicks(device, source.getWorld().getTime());
        boolean containerAvailable = chunkLoaded && currentState != null && !currentState.isAir()
                && ContainerDeviceSupport.isContainer(world, pos);
        int containerSlotCount = containerAvailable ? ContainerDeviceSupport.slotCount(world, pos) : -1;
        Inventory containerInventory = containerAvailable ? ContainerItemConditionSupport.inventory(world, pos) : null;

        sendHeader(source, Text.literal("虚拟方块发射器调试信息").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("名称", nameText(SignalDeviceStore.displayName(device))), false);
        source.sendFeedback(() -> field("ID", idText(device.id())), false);
        source.sendFeedback(() -> field("短ID", idText(SignalDeviceStore.shortId(device.id()))), false);
        source.sendFeedback(() -> field("类型", typeText(device)), false);
        source.sendFeedback(() -> field("位置", positionText(device)), false);
        source.sendFeedback(() -> field("区块", chunkLoaded
                ? Text.literal("已加载").formatted(Formatting.GREEN)
                : Text.literal("未加载，本 tick 跳过检测").formatted(Formatting.YELLOW)), false);
        source.sendFeedback(() -> field("绑定时方块 ID", idText(device.blockId())), false);
        source.sendFeedback(() -> field("当前方块 ID", powerState == null ? unknownText() : idText(powerState.blockId())), false);
        source.sendFeedback(() -> field("方块一致性", powerState == null
                ? unknownText()
                : Text.literal(powerState.blockId().equals(device.blockId()) ? "一致" : "不一致")
                        .formatted(powerState.blockId().equals(device.blockId()) ? Formatting.GREEN : Formatting.YELLOW)), false);
        source.sendFeedback(() -> field("BlockState powered", powerState == null ? unknownText() : boolText(powerState.blockStatePowered())), false);
        source.sendFeedback(() -> field("接收红石强度", powerState == null ? unknownText() : number(powerState.receivedPowerLevel())), false);
        source.sendFeedback(() -> field("当前通电", powerState == null ? unknownText() : boolText(powerState.currentPowered())), false);
        source.sendFeedback(() -> field("上次通电", boolText(device.lastPowered())), false);
        source.sendFeedback(() -> field("上次红石强度", number(device.lastPowerLevel())), false);
        source.sendFeedback(() -> field("频道", channelOrEmpty(device.channel())), false);
        source.sendFeedback(() -> field("断电频道", channelOrEmpty(device.offChannel())), false);
        source.sendFeedback(() -> field("模式", modeText(device.mode())), false);
        source.sendFeedback(() -> field("方块状态条件", conditionSummary(device)), false);
        source.sendFeedback(() -> field("条件模式", conditionModeText(device.conditionMode())), false);
        source.sendFeedback(() -> field("条件方块 ID", device.conditionBlockId().isBlank() ? Text.literal("未设置").formatted(Formatting.YELLOW) : idText(device.conditionBlockId())), false);
        source.sendFeedback(() -> field("条件属性", Text.literal(propertiesText(device.conditionProperties())).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("上次条件满足", boolText(device.lastConditionMatched())), false);
        source.sendFeedback(() -> field("当前条件满足", conditionMatchedText(source, device)), false);
        source.sendFeedback(() -> field("当前方块支持状态", supportedPropertiesText(source, device)), false);
        List<String> conditionIssues = validateConditionIssues(source, device);
        if (!conditionIssues.isEmpty()) {
            source.sendFeedback(() -> Text.literal("条件诊断：").formatted(Formatting.YELLOW), false);
            for (String issue : conditionIssues) {
                source.sendFeedback(() -> Text.literal("- " + issue).formatted(Formatting.YELLOW), false);
            }
        }
        source.sendFeedback(() -> field("交互触发", boolText(device.interactionEnabled())), false);
        source.sendFeedback(() -> field("交互频道", channelOrEmpty(device.interactChannel())), false);
        source.sendFeedback(() -> field("交互冷却", gtText(device.interactionCooldownTicks())), false);
        source.sendFeedback(() -> field("交互冷却剩余", cooldownText(remainingInteractionCooldown)), false);
        source.sendFeedback(() -> field("最近交互", elapsedOrNever(device.lastInteractionWallTimeMillis())), false);
        source.sendFeedback(() -> field("最近交互玩家", playerOrNever(device.lastInteractionPlayerName())), false);
        source.sendFeedback(() -> field("最近交互结果", resultText(device.lastInteractionResult())), false);
        ItemStackMatcherData interactionMatcher = device.interactionItemMatcher().normalized();
        source.sendFeedback(() -> field("主手物品匹配", boolText(device.interactionItemMatcherEnabled())), false);
        source.sendFeedback(() -> field("物品匹配模板", Text.literal(ItemStackMatcherSupport.summary(interactionMatcher)).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("匹配成功频道", channelOrEmpty(interactionMatcher.successChannel())), false);
        source.sendFeedback(() -> field("匹配失败频道", channelOrEmpty(interactionMatcher.failChannel())), false);
        source.sendFeedback(() -> field("成功消息", configuredText(interactionMatcher.successMessage())), false);
        source.sendFeedback(() -> field("失败消息", configuredText(interactionMatcher.failMessage())), false);
        source.sendFeedback(() -> field("成功音效", soundText(interactionMatcher.successSoundId(), interactionMatcher.successSoundVolume(), interactionMatcher.successSoundPitch())), false);
        source.sendFeedback(() -> field("失败音效", soundText(interactionMatcher.failSoundId(), interactionMatcher.failSoundVolume(), interactionMatcher.failSoundPitch())), false);
        source.sendFeedback(() -> field("成功消耗", boolText(interactionMatcher.consumeEnabled())), false);
        source.sendFeedback(() -> field("消耗数量", number(interactionMatcher.consumeCount())), false);
        source.sendFeedback(() -> field("最近主手匹配", boolText(device.lastInteractionItemMatched())), false);
        source.sendFeedback(() -> field("最近主手匹配结果", resultText(device.lastInteractionItemResult())), false);
        source.sendFeedback(() -> field("交互频道监听器", number(interactionListeners.size())), false);
        source.sendFeedback(() -> field("交互频道接收器", number(interactionReceiverCount)), false);
        source.sendFeedback(() -> field("交互频道动作继电器", number(interactionRelayCount)), false);
        source.sendFeedback(() -> field("容器事件", boolText(device.containerEnabled())), false);
        source.sendFeedback(() -> field("容器打开频道", channelOrEmpty(device.containerOpenChannel())), false);
        source.sendFeedback(() -> field("容器关闭频道", channelOrEmpty(device.containerCloseChannel())), false);
        source.sendFeedback(() -> field("容器内容变化频道", channelOrEmpty(device.containerChangeChannel())), false);
        source.sendFeedback(() -> field("容器冷却", gtText(device.containerCooldownTicks())), false);
        source.sendFeedback(() -> field("容器冷却剩余", cooldownText(remainingContainerCooldown)), false);
        source.sendFeedback(() -> field("内容检查间隔", gtText(device.containerChangeCheckIntervalTicks())), false);
        source.sendFeedback(() -> field("当前是否容器", boolText(containerAvailable)), false);
        source.sendFeedback(() -> field("容器槽位", containerSlotCount < 0 ? unknownText() : number(containerSlotCount)), false);
        source.sendFeedback(() -> field("最近容器事件", device.lastContainerEventType().isBlank()
                ? Text.literal("尚无记录").formatted(Formatting.YELLOW)
                : Text.literal(device.lastContainerEventType()).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("最近容器玩家", playerOrNever(device.lastContainerPlayerName())), false);
        source.sendFeedback(() -> field("最近容器结果", resultText(device.lastContainerResult())), false);
        source.sendFeedback(() -> field("物品条件", itemConditionCountText(device)), false);
        source.sendFeedback(() -> field("最近物品条件触发", latestItemConditionTriggerText(device)), false);
        source.sendFeedback(() -> field("最近物品条件结果", latestItemConditionResultText(device)), false);
        if (device.itemConditions().isEmpty()) {
            source.sendFeedback(() -> field("物品条件列表", Text.literal("未配置").formatted(Formatting.YELLOW)), false);
        } else {
            source.sendFeedback(() -> Text.literal("物品条件列表：").formatted(Formatting.GRAY), false);
            for (ContainerItemConditionData condition : device.itemConditions()) {
                source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                        .append(nameText(condition.name()))
                        .append(Text.literal("，类型 ").formatted(Formatting.GRAY))
                        .append(Text.literal(condition.type()).formatted(Formatting.LIGHT_PURPLE))
                        .append(Text.literal("，状态 ").formatted(Formatting.GRAY))
                        .append(enabledText(condition.enabled()))
                        .append(Text.literal("，上次满足 ").formatted(Formatting.GRAY))
                        .append(boolText(condition.lastMatched())), false);
            }
        }
        source.sendFeedback(() -> field("状态", enabledText(device.enabled())), false);
        source.sendFeedback(() -> field("频道监听器", number(listeners.size())), false);
        source.sendFeedback(() -> field("同频道接收器", number(receiverCount)), false);
        source.sendFeedback(() -> field("同频道动作继电器", number(relayCount)), false);
        source.sendFeedback(() -> field("最近触发", elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        source.sendFeedback(() -> field("最近结果", resultText(device.lastResult())), false);

        List<Text> hints = virtualDebugHints(
                device,
                powerState,
                currentState,
                chunkLoaded,
                listeners,
                receiverCount,
                relayCount,
                interactionListeners.size(),
                interactionReceiverCount,
                interactionRelayCount,
                remainingInteractionCooldown,
                remainingContainerCooldown,
                containerAvailable,
                containerInventory
        );
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
    }

    private static List<Text> virtualDebugHints(
            SignalDeviceData device,
            VirtualBlockPowerState powerState,
            BlockState currentState,
            boolean chunkLoaded,
            List<SignalListenerData> listeners,
            int receiverCount,
            int relayCount,
            int interactionListenerCount,
            int interactionReceiverCount,
            int interactionRelayCount,
            long remainingInteractionCooldown,
            long remainingContainerCooldown,
            boolean containerAvailable,
            Inventory containerInventory
    ) {
        List<Text> hints = new ArrayList<>();
        if (device.channel().isBlank()) {
            hints.add(Text.literal("未绑定频道。").formatted(Formatting.YELLOW));
        }
        if (!device.channel().isBlank() && !SignalChannel.isValid(device.channel())) {
            hints.add(Text.literal("频道名称无效。").formatted(Formatting.RED));
        }
        if (!device.offChannel().isBlank() && !SignalChannel.isValid(device.offChannel())) {
            hints.add(Text.literal("断电频道名称无效。").formatted(Formatting.RED));
        }
        if (!device.enabled()) {
            hints.add(Text.literal("设备已禁用。").formatted(Formatting.YELLOW));
        }
        if (!chunkLoaded) {
            hints.add(Text.literal("所在区块未加载，本 tick 会跳过检测。").formatted(Formatting.YELLOW));
        } else if (powerState != null && powerState.air()) {
            hints.add(Text.literal("当前方块为空气，可用 /tzz signal device cleanup 清理记录。").formatted(Formatting.YELLOW));
        } else if (powerState != null && !powerState.blockId().equals(device.blockId())) {
            hints.add(Text.literal("当前方块 ID 与绑定时不一致，MVP 会跳过触发；请 refresh 或重新 bind。").formatted(Formatting.YELLOW));
        }
        if (device.offChannel().isBlank() && VirtualBlockDeviceMode.fromId(device.mode()).triggersFalling()) {
            hints.add(Text.literal("offChannel 未设置，断电触发会使用主频道。").formatted(Formatting.YELLOW));
        }
        if (device.conditionEnabled()) {
            if (device.offChannel().isBlank() && BlockStateConditionMode.fromId(device.conditionMode()).triggersExit()) {
                hints.add(Text.literal("offChannel 未设置，条件退出边沿会使用主频道。").formatted(Formatting.YELLOW));
            }
            if (currentState != null) {
                List<String> conditionIssues = BlockStateConditionParser.validateSavedCondition(device, currentState);
                for (String issue : conditionIssues) {
                    hints.add(Text.literal(issue).formatted(Formatting.YELLOW));
                }
            }
        }
        if (device.interactionEnabled() && device.interactChannel().isBlank()) {
            hints.add(Text.literal("未设置 interactChannel。").formatted(Formatting.YELLOW));
        }
        if (!device.interactionEnabled() && !device.interactChannel().isBlank()) {
            hints.add(Text.literal("interaction 已禁用。").formatted(Formatting.YELLOW));
        }
        if (!device.interactChannel().isBlank() && !SignalChannel.isValid(device.interactChannel())) {
            hints.add(Text.literal("interactChannel 名称无效。").formatted(Formatting.RED));
        }
        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        if (device.interactionItemMatcherEnabled()) {
            if (!matcher.enabled()) {
                hints.add(Text.literal("interactionItem 已启用，但缺少主手物品模板。").formatted(Formatting.RED));
            }
            if (device.interactChannel().isBlank() && matcher.successChannel().isBlank()) {
                hints.add(Text.literal("未设置 interactChannel 或 successChannel，匹配成功时不会 emit signal。").formatted(Formatting.YELLOW));
            }
            if (!matcher.successChannel().isBlank() && !SignalChannel.isValid(matcher.successChannel())) {
                hints.add(Text.literal("successChannel 名称无效。").formatted(Formatting.RED));
            }
            if (!matcher.failChannel().isBlank() && !SignalChannel.isValid(matcher.failChannel())) {
                hints.add(Text.literal("failChannel 名称无效。").formatted(Formatting.RED));
            }
            if (matcher.failChannel().isBlank()) {
                hints.add(Text.literal("failChannel 未设置，匹配失败时不会 emit signal。").formatted(Formatting.YELLOW));
            }
            if (matcher.consumeEnabled() && matcher.consumeCount() <= 0) {
                hints.add(Text.literal("consumeCount 无效。").formatted(Formatting.RED));
            }
        }
        if (remainingInteractionCooldown > 0L) {
            hints.add(Text.literal("正处于 interaction cooldown。").formatted(Formatting.YELLOW));
        }
        if (!device.interactChannel().isBlank()
                && interactionListenerCount <= 0
                && interactionReceiverCount <= 0
                && interactionRelayCount <= 0) {
            hints.add(Text.literal("interactChannel 没有 listener、接收器或动作继电器；signal 仍会发出并记录历史。")
                    .formatted(Formatting.YELLOW));
        }
        boolean hasContainerChannel = !device.containerOpenChannel().isBlank()
                || !device.containerCloseChannel().isBlank()
                || !device.containerChangeChannel().isBlank();
        if (device.containerEnabled() && !hasContainerChannel) {
            hints.add(Text.literal("容器事件已启用，但没有设置任何 container channel。").formatted(Formatting.YELLOW));
        }
        if (!device.containerEnabled() && hasContainerChannel) {
            hints.add(Text.literal("容器事件已禁用。").formatted(Formatting.YELLOW));
        }
        if (hasContainerChannel && !containerAvailable) {
            hints.add(Text.literal("当前方块不是可用容器、方块未加载或 ID 不一致。").formatted(Formatting.YELLOW));
        }
        if (!device.containerOpenChannel().isBlank() && !SignalChannel.isValid(device.containerOpenChannel())) {
            hints.add(Text.literal("containerOpenChannel 名称无效。").formatted(Formatting.RED));
        }
        if (!device.containerCloseChannel().isBlank() && !SignalChannel.isValid(device.containerCloseChannel())) {
            hints.add(Text.literal("containerCloseChannel 名称无效。").formatted(Formatting.RED));
        }
        if (!device.containerChangeChannel().isBlank() && !SignalChannel.isValid(device.containerChangeChannel())) {
            hints.add(Text.literal("containerChangeChannel 名称无效。").formatted(Formatting.RED));
        }
        if (remainingContainerCooldown > 0L) {
            hints.add(Text.literal("正处于 container cooldown。").formatted(Formatting.YELLOW));
        }
        if (!device.itemConditions().isEmpty() && !containerAvailable) {
            hints.add(Text.literal("已配置物品条件，但当前方块不是可用容器、方块未加载或 ID 不一致。").formatted(Formatting.YELLOW));
        }
        for (ContainerItemConditionData condition : device.itemConditions()) {
            if (!condition.enabled()) {
                hints.add(Text.literal("物品条件“" + condition.name() + "”已禁用。").formatted(Formatting.YELLOW));
            }
            if (condition.channel().isBlank() || !SignalChannel.isValid(condition.channel())) {
                hints.add(Text.literal("物品条件“" + condition.name() + "”的频道为空或无效。").formatted(Formatting.RED));
            }
            if (!condition.offChannel().isBlank() && !SignalChannel.isValid(condition.offChannel())) {
                hints.add(Text.literal("物品条件“" + condition.name() + "”的退出频道无效。").formatted(Formatting.RED));
            }
            if (containerInventory != null) {
                for (String issue : ContainerItemConditionSupport.validate(containerInventory, condition)) {
                    hints.add(Text.literal("物品条件“" + condition.name() + "”："
                            + issue).formatted(Formatting.YELLOW));
                }
            }
        }
        if (!device.channel().isBlank() && listeners.isEmpty() && receiverCount <= 0 && relayCount <= 0) {
            hints.add(Text.literal("频道没有 listener、接收器或动作继电器；signal 仍会发出并记录历史。").formatted(Formatting.YELLOW));
        }
        return hints;
    }

    private static List<Text> debugHints(
            SignalDeviceData device,
            SignalEmitterBlockEntity emitter,
            SignalReceiverBlockEntity receiver,
            ActionRelayBlockEntity relay,
            List<SignalListenerData> listeners,
            int receiverCount,
            int relayCount,
            long currentGameTime
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
        } else if (isActionRelay(device)) {
            if (relay == null) {
                hints.add(Text.literal("方块未加载或已不存在。").formatted(Formatting.YELLOW));
            } else {
                if (!SignalChannel.normalize(relay.channel()).equals(SignalChannel.normalize(device.channel()))
                        || relay.enabled() != device.enabled()
                        || relay.cooldownTicks() != device.cooldownTicks()
                        || relay.actions().size() != device.actionCount()) {
                    hints.add(Text.literal("registry 与 BlockEntity 状态不一致。").formatted(Formatting.YELLOW));
                }
                if (relay.actions().isEmpty()) {
                    hints.add(Text.literal("没有配置 actions。").formatted(Formatting.YELLOW));
                }
                if (relay.remainingCooldownTicks(currentGameTime) > 0L) {
                    hints.add(Text.literal("动作继电器可能处于冷却中。").formatted(Formatting.YELLOW));
                }
            }
            if (!device.channel().isBlank() && listeners.isEmpty() && receiverCount <= 0 && relayCount <= 1) {
                hints.add(Text.literal("频道没有其他 listener/receiver；动作继电器仍可独立工作。").formatted(Formatting.YELLOW));
            }
        } else {
            if (emitter == null) {
                hints.add(Text.literal("方块未加载或已不存在。").formatted(Formatting.YELLOW));
            } else if (!SignalChannel.normalize(emitter.channel()).equals(SignalChannel.normalize(device.channel()))
                    || emitter.enabled() != device.enabled()) {
                hints.add(Text.literal("registry 与 BlockEntity 状态不一致。").formatted(Formatting.YELLOW));
            }
            if (!device.channel().isBlank() && listeners.isEmpty() && receiverCount <= 0 && relayCount <= 0) {
                hints.add(Text.literal("频道没有 listener、接收器或动作继电器。").formatted(Formatting.YELLOW));
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
            } else if (isActionRelay(device)) {
                if (device.id().equals(record.sourceId())
                        || SignalChannel.normalize(record.channel()).equals(SignalChannel.normalize(device.channel()))) {
                    matches.add(record);
                }
            } else if (isVirtualBlockDevice(device)) {
                if ("virtual_block_device".equals(record.sourceType()) && device.id().equals(record.sourceId())) {
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

    private static int actionRelayCountForChannel(ServerCommandSource source, String channel) {
        String normalizedChannel = SignalChannel.normalize(channel);
        int count = 0;
        for (SignalDeviceData device : SignalDeviceStore.getSnapshot(source.getServer())) {
            if (isActionRelay(device) && device.enabled() && SignalChannel.normalize(device.channel()).equals(normalizedChannel)) {
                count++;
            }
        }
        return count;
    }

    private static int invalidActionCount(ActionRelayBlockEntity relay) {
        int count = 0;
        for (ActionConfig action : relay.actions()) {
            if (action == null || !action.isUsable()) {
                count++;
            } else if (action.type() == ActionType.SIGNAL && !SignalChannel.isValid(action.value())) {
                count++;
            }
        }
        return count;
    }

    private static boolean isLoaded(ServerCommandSource source, SignalDeviceData device) {
        if (isVirtualBlockDevice(device)) {
            ServerWorld world = SignalDeviceStore.getDeviceWorld(source.getServer(), device);
            return world != null && world.isChunkLoaded(new BlockPos(device.x(), device.y(), device.z()));
        }
        if (isActionRelay(device)) {
            return SignalDeviceStore.getLoadedActionRelay(source.getServer(), device) != null;
        }
        return isReceiver(device)
                ? SignalDeviceStore.getLoadedReceiver(source.getServer(), device) != null
                : SignalDeviceStore.getLoadedEmitter(source.getServer(), device) != null;
    }

    private static boolean isReceiver(SignalDeviceData device) {
        return device != null && SignalDeviceData.TYPE_SIGNAL_RECEIVER.equals(device.type());
    }

    private static boolean isActionRelay(SignalDeviceData device) {
        return device != null && SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type());
    }

    private static boolean isVirtualBlockDevice(SignalDeviceData device) {
        return device != null && SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type());
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

    private static Text configuredText(String value) {
        return value == null || value.isBlank()
                ? Text.literal("未设置").formatted(Formatting.YELLOW)
                : Text.literal(value).formatted(Formatting.WHITE);
    }

    private static Text soundText(String soundId, float volume, float pitch) {
        return soundId == null || soundId.isBlank()
                ? Text.literal("未设置").formatted(Formatting.YELLOW)
                : Text.literal(soundId + " / volume " + volume + " / pitch " + pitch).formatted(Formatting.AQUA);
    }

    private static MutableText typeText(SignalDeviceData device) {
        return Text.literal(device.type()).formatted(isActionRelay(device)
                ? Formatting.GREEN
                : isReceiver(device) ? Formatting.RED : isVirtualBlockDevice(device) ? Formatting.LIGHT_PURPLE : Formatting.WHITE);
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

    private static MutableText boolText(boolean value) {
        return Text.literal(value ? "是" : "否").formatted(value ? Formatting.GREEN : Formatting.GRAY);
    }

    private static MutableText unknownText() {
        return Text.literal("未知").formatted(Formatting.YELLOW);
    }

    private static MutableText modeText(String mode) {
        return Text.literal(VirtualBlockDeviceMode.normalize(mode)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText itemConditionCountText(SignalDeviceData device) {
        return Text.literal(device.itemConditions().size() + " 个，启用 " + enabledItemConditionCount(device) + " 个")
                .formatted(Formatting.LIGHT_PURPLE);
    }

    private static int enabledItemConditionCount(SignalDeviceData device) {
        int count = 0;
        for (ContainerItemConditionData condition : device.itemConditions()) {
            if (condition.enabled()) {
                count++;
            }
        }
        return count;
    }

    private static Text latestItemConditionTriggerText(SignalDeviceData device) {
        long latest = 0L;
        for (ContainerItemConditionData condition : device.itemConditions()) {
            latest = Math.max(latest, condition.lastTriggerWallTimeMillis());
        }
        return elapsedOrNever(latest);
    }

    private static Text latestItemConditionResultText(SignalDeviceData device) {
        ContainerItemConditionData latest = null;
        for (ContainerItemConditionData condition : device.itemConditions()) {
            if (latest == null || condition.lastTriggerWallTimeMillis() > latest.lastTriggerWallTimeMillis()) {
                latest = condition;
            }
        }
        if (latest == null || latest.lastResult().isBlank()) {
            return Text.literal("尚无结果").formatted(Formatting.YELLOW);
        }
        return Text.literal(latest.name() + "：" + latest.lastResult()).formatted(Formatting.WHITE);
    }

    private static MutableText conditionModeText(String mode) {
        return Text.literal(BlockStateConditionMode.normalize(mode)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static Text conditionSummary(SignalDeviceData device) {
        return device.conditionEnabled()
                ? Text.literal(device.conditionRaw()).formatted(Formatting.AQUA)
                : Text.literal("未设置").formatted(Formatting.YELLOW);
    }

    private static Text conditionMatchedText(ServerCommandSource source, SignalDeviceData device) {
        if (!device.conditionEnabled()) {
            return Text.literal("未设置条件").formatted(Formatting.YELLOW);
        }
        BlockState state = currentBlockState(source, device);
        if (state == null) {
            return Text.literal("未知").formatted(Formatting.YELLOW);
        }
        return boolText(BlockStateConditionParser.matches(state, device));
    }

    private static Text supportedPropertiesText(ServerCommandSource source, SignalDeviceData device) {
        BlockState state = currentBlockState(source, device);
        if (state == null) {
            return Text.literal("未知").formatted(Formatting.YELLOW);
        }
        String properties = BlockStateConditionParser.supportedProperties(state);
        return properties.isBlank()
                ? Text.literal("无").formatted(Formatting.YELLOW)
                : Text.literal(properties).formatted(Formatting.AQUA);
    }

    private static List<String> validateConditionIssues(ServerCommandSource source, SignalDeviceData device) {
        if (!device.conditionEnabled()) {
            return List.of();
        }
        BlockState state = currentBlockState(source, device);
        if (state == null) {
            return List.of("当前方块状态不可用，可能是区块未加载。");
        }
        return BlockStateConditionParser.validateSavedCondition(device, state);
    }

    private static BlockState currentBlockState(ServerCommandSource source, SignalDeviceData device) {
        ServerWorld world = SignalDeviceStore.getDeviceWorld(source.getServer(), device);
        if (world == null) {
            return null;
        }
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        return world.isChunkLoaded(pos) ? world.getBlockState(pos) : null;
    }

    private static String propertiesText(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return "未设置";
        }
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            values.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join(", ", values);
    }

    private static MutableText gtText(int ticks) {
        return Text.literal(ticks + " GT").formatted(Formatting.LIGHT_PURPLE);
    }

    private static Text remainingInteractionCooldownText(SignalDeviceData device, long currentGameTime) {
        return cooldownText(SignalDeviceStore.getRemainingInteractionCooldownTicks(device, currentGameTime));
    }

    private static Text cooldownText(long ticks) {
        if (ticks <= 0L) {
            return Text.literal("0 GT").formatted(Formatting.LIGHT_PURPLE);
        }
        double seconds = ticks / 20.0D;
        return Text.literal(ticks + " GT（约 " + String.format(java.util.Locale.ROOT, "%.1f", seconds) + " 秒）")
                .formatted(Formatting.LIGHT_PURPLE);
    }

    private static Text playerOrNever(String playerName) {
        return playerName == null || playerName.isBlank()
                ? Text.literal("尚无记录").formatted(Formatting.YELLOW)
                : Text.literal(playerName).formatted(Formatting.WHITE);
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

    private static VirtualBlockPowerState virtualPowerState(ServerCommandSource source, SignalDeviceData device) {
        ServerWorld world = SignalDeviceStore.getDeviceWorld(source.getServer(), device);
        if (world == null) {
            return null;
        }
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        return world.isChunkLoaded(pos) ? VirtualBlockDeviceSupport.powerState(world, pos) : null;
    }

    private static MutableText consistencyText(
            SignalDeviceData device,
            SignalEmitterBlockEntity emitter,
            SignalReceiverBlockEntity receiver,
            ActionRelayBlockEntity relay
    ) {
        if (isActionRelay(device)) {
            if (relay == null) {
                return Text.literal("无法比较").formatted(Formatting.YELLOW);
            }
            boolean same = SignalChannel.normalize(relay.channel()).equals(SignalChannel.normalize(device.channel()))
                    && relay.enabled() == device.enabled()
                    && relay.cooldownTicks() == device.cooldownTicks()
                    && relay.actions().size() == device.actionCount();
            return Text.literal(same ? "一致" : "不一致").formatted(same ? Formatting.GREEN : Formatting.YELLOW);
        }

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
            SignalReceiverBlockEntity receiver,
            ActionRelayBlockEntity relay
    ) {
    }
}
