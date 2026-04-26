package com.zcpu.tzzmod.signal.device;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class VirtualBlockDeviceCommand {
    private VirtualBlockDeviceCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("blockDevice")
                .then(CommandManager.literal("bind")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("channel", StringArgumentType.string())
                                        .executes(context -> executeBind(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "channel")
                                        )))))
                .then(CommandManager.literal("offChannel")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("channel", StringArgumentType.string())
                                        .executes(context -> executeOffChannel(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "channel")
                                        )))))
                .then(CommandManager.literal("clearOffChannel")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeClearOffChannel(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("mode")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.literal(VirtualBlockDeviceMode.REDSTONE_RISING.id())
                                        .executes(context -> executeMode(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                VirtualBlockDeviceMode.REDSTONE_RISING
                                        )))
                                .then(CommandManager.literal(VirtualBlockDeviceMode.REDSTONE_FALLING.id())
                                        .executes(context -> executeMode(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                VirtualBlockDeviceMode.REDSTONE_FALLING
                                        )))
                                .then(CommandManager.literal(VirtualBlockDeviceMode.REDSTONE_BOTH.id())
                                        .executes(context -> executeMode(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                VirtualBlockDeviceMode.REDSTONE_BOTH
                                        )))))
                .then(CommandManager.literal("condition")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("condition", StringArgumentType.greedyString())
                                        .executes(context -> executeCondition(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "condition")
                                        )))))
                .then(CommandManager.literal("clearCondition")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeClearCondition(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("conditionMode")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.literal(BlockStateConditionMode.CONDITION_ENTER.id())
                                        .executes(context -> executeConditionMode(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                BlockStateConditionMode.CONDITION_ENTER
                                        )))
                                .then(CommandManager.literal(BlockStateConditionMode.CONDITION_EXIT.id())
                                        .executes(context -> executeConditionMode(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                BlockStateConditionMode.CONDITION_EXIT
                                        )))
                                .then(CommandManager.literal(BlockStateConditionMode.CONDITION_BOTH.id())
                                        .executes(context -> executeConditionMode(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                BlockStateConditionMode.CONDITION_BOTH
                                        )))))
                .then(CommandManager.literal("conditionInfo")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeConditionInfo(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeInfo(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("test")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeTest(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("unbind")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeUnbind(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("refresh")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeRefresh(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))));
    }

    private static int executeBind(ServerCommandSource source, BlockPos pos, String rawChannel) {
        String channel = SignalChannel.normalize(rawChannel);
        if (!SignalChannel.isValid(channel)) {
            sendError(source, SignalChannel.validationError(rawChannel));
            return 0;
        }

        BlockState state = source.getWorld().getBlockState(pos);
        if (state.isAir()) {
            sendError(source, Text.literal("不能绑定空气方块。"));
            return 0;
        }
        if (VirtualBlockDeviceSupport.isDedicatedSignalDevice(state)) {
            sendError(source, Text.literal("该位置是 TZZ 专用信号设备，请使用 /tzz signal device bind。"));
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.upsertVirtualBlock(source.getWorld(), pos, channel);
        VirtualBlockPowerState powerState = VirtualBlockDeviceSupport.powerState(source.getWorld(), pos);
        sendHeader(source, Text.literal("已绑定虚拟方块发射器").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("方块 ID", idText(powerState.blockId())), false);
        source.sendFeedback(() -> field("频道", channelText(device.channel())), false);
        source.sendFeedback(() -> field("当前通电", boolText(powerState.currentPowered())), false);
        source.sendFeedback(() -> field("红石强度", number(powerState.receivedPowerLevel())), false);
        return 1;
    }

    private static int executeOffChannel(ServerCommandSource source, BlockPos pos, String rawChannel) {
        SignalDeviceData existing = getVirtualDevice(source, pos);
        if (existing == null) {
            return 0;
        }

        String channel = SignalChannel.normalize(rawChannel);
        if ("clear".equalsIgnoreCase(rawChannel) || "none".equalsIgnoreCase(rawChannel)) {
            channel = "";
        } else if (!SignalChannel.isValid(channel)) {
            sendError(source, SignalChannel.validationError(rawChannel));
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.updateVirtualOffChannel(source.getWorld(), pos, channel);
        sendHeader(source, Text.literal(channel.isBlank() ? "已清空虚拟方块发射器断电频道" : "已设置虚拟方块发射器断电频道")
                .formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("断电频道", channelOrEmpty(device.offChannel())), false);
        return 1;
    }

    private static int executeClearOffChannel(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData existing = getVirtualDevice(source, pos);
        if (existing == null) {
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.updateVirtualOffChannel(source.getWorld(), pos, "");
        sendHeader(source, Text.literal("已清空虚拟方块发射器断电频道").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("断电频道", channelOrEmpty(device.offChannel())), false);
        source.sendFeedback(() -> warning("redstone_both 或 condition_both 的退出边沿会回退使用主频道。"), false);
        return 1;
    }

    private static int executeMode(ServerCommandSource source, BlockPos pos, VirtualBlockDeviceMode mode) {
        SignalDeviceData existing = getVirtualDevice(source, pos);
        if (existing == null) {
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.updateVirtualMode(source.getWorld(), pos, mode.id());
        sendHeader(source, Text.literal("已设置虚拟方块发射器红石触发模式").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("模式", modeText(device.mode())), false);
        return 1;
    }

    private static int executeCondition(ServerCommandSource source, BlockPos pos, String rawCondition) {
        SignalDeviceData existing = getVirtualDevice(source, pos);
        if (existing == null) {
            return 0;
        }

        BlockState state = source.getWorld().getBlockState(pos);
        BlockStateConditionResult result = BlockStateConditionParser.parseAndValidate(rawCondition, state);
        if (!result.success()) {
            sendError(source, Text.literal(result.error()));
            return 0;
        }

        boolean currentMatched = BlockStateConditionParser.matches(state, result.condition());
        SignalDeviceData device = SignalDeviceStore.updateVirtualCondition(source.getWorld(), pos, result.condition(), currentMatched);
        sendHeader(source, Text.literal("已设置方块状态条件").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("方块 ID", idText(result.condition().blockId())), false);
        source.sendFeedback(() -> field("条件", conditionText(result.condition().raw())), false);
        source.sendFeedback(() -> field("当前满足", boolText(currentMatched)), false);
        source.sendFeedback(() -> field("主频道", channelOrEmpty(device.channel())), false);
        source.sendFeedback(() -> field("条件模式", conditionModeText(device.conditionMode())), false);
        return 1;
    }

    private static int executeClearCondition(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData existing = getVirtualDevice(source, pos);
        if (existing == null) {
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.clearVirtualCondition(source.getWorld(), pos);
        sendHeader(source, Text.literal("已清空方块状态条件").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("条件", conditionSummary(device)), false);
        return 1;
    }

    private static int executeConditionMode(ServerCommandSource source, BlockPos pos, BlockStateConditionMode mode) {
        SignalDeviceData existing = getVirtualDevice(source, pos);
        if (existing == null) {
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.updateVirtualConditionMode(source.getWorld(), pos, mode.id());
        sendHeader(source, Text.literal("已设置方块状态条件触发模式").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("条件模式", conditionModeText(device.conditionMode())), false);
        if (!device.conditionEnabled()) {
            source.sendFeedback(() -> warning("当前还没有设置方块状态条件。"), false);
        }
        return 1;
    }

    private static int executeConditionInfo(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }

        sendConditionInfo(source, device, pos);
        return 1;
    }

    private static int executeInfo(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }

        sendInfo(source, device);
        return 1;
    }

    private static int executeTest(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        if (device.channel().isBlank() || !SignalChannel.isValid(device.channel())) {
            sendError(source, Text.literal("虚拟方块发射器频道未绑定或无效。"));
            return 0;
        }

        ActionExecutionResult result = SignalBridgeServer.emit(new SignalEvent(
                device.channel(),
                source.getEntity() instanceof ServerPlayerEntity player ? player : null,
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
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("频道", channelText(device.channel())), false);
        source.sendFeedback(() -> field("结果", result.message()), false);
        return 1;
    }

    private static int executeUnbind(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }

        SignalDeviceStore.removeVirtualBlock(source.getServer(), source.getWorld(), pos);
        sendHeader(source, Text.literal("已解绑虚拟方块发射器").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("名称", nameText(SignalDeviceStore.displayName(device))), false);
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("频道", channelOrEmpty(device.channel())), false);
        return 1;
    }

    private static int executeRefresh(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData existing = getVirtualDevice(source, pos);
        if (existing == null) {
            return 0;
        }

        BlockState state = source.getWorld().getBlockState(pos);
        if (state.isAir()) {
            sendError(source, Text.literal("当前位置是空气，不能刷新虚拟方块发射器。"));
            return 0;
        }
        if (VirtualBlockDeviceSupport.isDedicatedSignalDevice(state)) {
            sendError(source, Text.literal("该位置是 TZZ 专用信号设备，请解绑虚拟方块发射器后使用专用设备命令。"));
            return 0;
        }

        SignalDeviceData device = SignalDeviceStore.refreshVirtualBlock(source.getWorld(), pos);
        sendHeader(source, Text.literal("已刷新虚拟方块发射器状态").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("方块 ID", idText(device.blockId())), false);
        source.sendFeedback(() -> field("当前通电", boolText(device.lastPowered())), false);
        source.sendFeedback(() -> field("红石强度", number(device.lastPowerLevel())), false);
        if (device.conditionEnabled() && !device.blockId().equals(device.conditionBlockId())) {
            source.sendFeedback(() -> warning("当前方块 ID 与条件方块 ID 不一致；condition 不会自动改到新方块上，请重新设置 condition。"), false);
        }
        return 1;
    }

    private static SignalDeviceData getVirtualDevice(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = SignalDeviceStore.findVirtualBlockDevice(source.getServer(), source.getWorld(), pos);
        if (device == null) {
            sendError(source, Text.literal("该位置没有虚拟方块发射器。"));
        }
        return device;
    }

    private static void sendInfo(ServerCommandSource source, SignalDeviceData device) {
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        boolean chunkLoaded = source.getWorld().isChunkLoaded(pos);
        VirtualBlockPowerState powerState = chunkLoaded ? VirtualBlockDeviceSupport.powerState(source.getWorld(), pos) : null;
        sendHeader(source, Text.literal("虚拟方块发射器信息").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("名称", nameText(SignalDeviceStore.displayName(device))), false);
        source.sendFeedback(() -> field("ID", idText(device.id())), false);
        source.sendFeedback(() -> field("短ID", idText(SignalDeviceStore.shortId(device.id()))), false);
        source.sendFeedback(() -> field("位置", Text.literal(SignalDeviceStore.positionText(device)).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("绑定时方块 ID", idText(device.blockId())), false);
        source.sendFeedback(() -> field("当前方块 ID", powerState == null ? Text.literal("区块未加载").formatted(Formatting.YELLOW) : idText(powerState.blockId())), false);
        source.sendFeedback(() -> field("BlockState powered", powerState == null ? unknownText() : boolText(powerState.blockStatePowered())), false);
        source.sendFeedback(() -> field("接收红石强度", powerState == null ? unknownText() : number(powerState.receivedPowerLevel())), false);
        source.sendFeedback(() -> field("当前通电", powerState == null ? unknownText() : boolText(powerState.currentPowered())), false);
        source.sendFeedback(() -> field("上次通电", boolText(device.lastPowered())), false);
        source.sendFeedback(() -> field("上次红石强度", number(device.lastPowerLevel())), false);
        source.sendFeedback(() -> field("频道", channelOrEmpty(device.channel())), false);
        source.sendFeedback(() -> field("断电频道", channelOrEmpty(device.offChannel())), false);
        source.sendFeedback(() -> field("红石模式", modeText(device.mode())), false);
        source.sendFeedback(() -> field("方块状态条件", conditionSummary(device)), false);
        source.sendFeedback(() -> field("条件模式", conditionModeText(device.conditionMode())), false);
        source.sendFeedback(() -> field("上次条件满足", boolText(device.lastConditionMatched())), false);
        source.sendFeedback(() -> field("状态", enabledText(device.enabled())), false);
        source.sendFeedback(() -> field("最近触发", elapsedOrNever(device.lastTriggerWallTimeMillis())), false);
        source.sendFeedback(() -> field("最近结果", resultText(device.lastResult())), false);
        source.sendFeedback(() -> field("区块状态", chunkLoaded
                ? Text.literal("已加载").formatted(Formatting.GREEN)
                : Text.literal("未加载，本 tick 会跳过检测").formatted(Formatting.YELLOW)), false);
    }

    private static void sendConditionInfo(ServerCommandSource source, SignalDeviceData device, BlockPos pos) {
        BlockState state = source.getWorld().getBlockState(pos);
        boolean currentMatched = device.conditionEnabled() && BlockStateConditionParser.matches(state, device);
        List<String> issues = BlockStateConditionParser.validateSavedCondition(device, state);

        sendHeader(source, Text.literal("方块状态条件详情").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("已启用", boolText(device.conditionEnabled())), false);
        source.sendFeedback(() -> field("条件", conditionSummary(device)), false);
        source.sendFeedback(() -> field("条件方块 ID", device.conditionBlockId().isBlank() ? Text.literal("未设置").formatted(Formatting.YELLOW) : idText(device.conditionBlockId())), false);
        source.sendFeedback(() -> field("条件属性", Text.literal(propertiesText(device.conditionProperties())).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("条件模式", conditionModeText(device.conditionMode())), false);
        source.sendFeedback(() -> field("上次满足", boolText(device.lastConditionMatched())), false);
        source.sendFeedback(() -> field("当前满足", device.conditionEnabled() ? boolText(currentMatched) : Text.literal("未设置条件").formatted(Formatting.YELLOW)), false);
        source.sendFeedback(() -> field("当前方块 ID", idText(VirtualBlockDeviceSupport.blockId(state))), false);
        source.sendFeedback(() -> field("当前方块支持状态", supportedPropertiesText(state)), false);
        if (issues.isEmpty()) {
            source.sendFeedback(() -> Text.literal("条件诊断：未发现明显问题。").formatted(Formatting.GREEN), false);
        } else {
            source.sendFeedback(() -> Text.literal("条件诊断：").formatted(Formatting.YELLOW), false);
            for (String issue : issues) {
                source.sendFeedback(() -> Text.literal("- " + issue).formatted(Formatting.YELLOW), false);
            }
        }
    }

    private static void sendHeader(ServerCommandSource source, Text title) {
        source.sendFeedback(() -> Text.literal("===========").formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> title, false);
    }

    private static void sendError(ServerCommandSource source, Text message) {
        source.sendFeedback(() -> Text.literal("===========").formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> Text.literal("错误：").formatted(Formatting.RED).append(message.copy().formatted(Formatting.RED)), false);
    }

    private static MutableText field(String label, Text value) {
        return Text.literal(label + "：").formatted(Formatting.GRAY).append(value);
    }

    private static MutableText nameText(String name) {
        return Text.literal(name == null || name.isBlank() ? "未命名信号设备" : name).formatted(Formatting.GOLD);
    }

    private static MutableText idText(String id) {
        return Text.literal(id == null || id.isBlank() ? "未知" : id).formatted(Formatting.AQUA);
    }

    private static MutableText channelText(String channel) {
        return Text.literal(SignalChannel.normalize(channel)).formatted(Formatting.AQUA);
    }

    private static Text channelOrEmpty(String channel) {
        return channel == null || channel.isBlank()
                ? Text.literal("未设置").formatted(Formatting.YELLOW)
                : channelText(channel);
    }

    private static MutableText posText(BlockPos pos) {
        return Text.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ()).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText modeText(String mode) {
        return Text.literal(VirtualBlockDeviceMode.normalize(mode)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText conditionModeText(String mode) {
        return Text.literal(BlockStateConditionMode.normalize(mode)).formatted(Formatting.LIGHT_PURPLE);
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

    private static MutableText number(int value) {
        return Text.literal(Integer.toString(value)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText resultText(String value) {
        return Text.literal(value == null || value.isBlank() ? "尚无结果" : value)
                .formatted(value == null || value.isBlank() ? Formatting.YELLOW : Formatting.WHITE);
    }

    private static MutableText conditionText(String value) {
        return Text.literal(value == null || value.isBlank() ? "未设置" : value)
                .formatted(value == null || value.isBlank() ? Formatting.YELLOW : Formatting.AQUA);
    }

    private static Text conditionSummary(SignalDeviceData device) {
        return device.conditionEnabled()
                ? conditionText(device.conditionRaw())
                : Text.literal("未设置").formatted(Formatting.YELLOW);
    }

    private static Text supportedPropertiesText(BlockState state) {
        String properties = BlockStateConditionParser.supportedProperties(state);
        return properties.isBlank()
                ? Text.literal("无").formatted(Formatting.YELLOW)
                : Text.literal(properties).formatted(Formatting.AQUA);
    }

    private static String propertiesText(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return "未设置";
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            joiner.add(entry.getKey() + "=" + entry.getValue());
        }
        return joiner.toString();
    }

    private static MutableText warning(String message) {
        return Text.literal(message).formatted(Formatting.YELLOW);
    }

    private static MutableText elapsedOrNever(long wallTimeMillis) {
        if (wallTimeMillis <= 0L) {
            return Text.literal("尚未触发").formatted(Formatting.YELLOW);
        }
        return Text.literal(formatElapsed(System.currentTimeMillis() - wallTimeMillis)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static String formatElapsed(long elapsedMillis) {
        if (elapsedMillis < 3_000L) {
            return "刚刚";
        }
        long totalSeconds = Math.max(0L, elapsedMillis / 1_000L);
        long minutes = totalSeconds / 60L;
        if (minutes > 0L) {
            long seconds = totalSeconds % 60L;
            return seconds > 0L ? minutes + " 分 " + seconds + " 秒前" : minutes + " 分前";
        }
        return totalSeconds + " 秒前";
    }
}
