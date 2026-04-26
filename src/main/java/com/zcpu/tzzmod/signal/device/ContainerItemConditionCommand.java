package com.zcpu.tzzmod.signal.device;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class ContainerItemConditionCommand {
    private ContainerItemConditionCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("itemCondition")
                .then(CommandManager.literal("addSlotEmpty")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(CommandManager.argument("slot", IntegerArgumentType.integer(0))
                                                .then(CommandManager.argument("channel", StringArgumentType.string())
                                                        .executes(context -> executeAddSlotEmpty(
                                                                context.getSource(),
                                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                                StringArgumentType.getString(context, "name"),
                                                                IntegerArgumentType.getInteger(context, "slot"),
                                                                StringArgumentType.getString(context, "channel")
                                                        )))))))
                .then(CommandManager.literal("addSlotItem")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(CommandManager.argument("slot", IntegerArgumentType.integer(0))
                                                .then(slotItemArgument((source, pos, name, slot, itemId, mode, count, channel) ->
                                                        executeAddSlotItem(source, pos, name, slot, itemId, mode, count, channel)))))))
                .then(CommandManager.literal("addTotalItem")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(totalItemArgument((source, pos, name, itemId, mode, count, channel) ->
                                                executeAddTotalItem(source, pos, name, itemId, mode, count, channel))))))
                .then(CommandManager.literal("list")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeList(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(context -> executeInfo(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "name")
                                        )))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(context -> executeRemove(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "name")
                                        )))))
                .then(CommandManager.literal("clear")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeClear(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("enable")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(context -> executeEnable(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "name"),
                                                true
                                        )))))
                .then(CommandManager.literal("disable")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(context -> executeEnable(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "name"),
                                                false
                                        )))))
                .then(CommandManager.literal("mode")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(CommandManager.literal(BlockStateConditionMode.CONDITION_ENTER.id())
                                                .executes(context -> executeMode(
                                                        context.getSource(),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                        StringArgumentType.getString(context, "name"),
                                                        BlockStateConditionMode.CONDITION_ENTER
                                                )))
                                        .then(CommandManager.literal(BlockStateConditionMode.CONDITION_EXIT.id())
                                                .executes(context -> executeMode(
                                                        context.getSource(),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                        StringArgumentType.getString(context, "name"),
                                                        BlockStateConditionMode.CONDITION_EXIT
                                                )))
                                        .then(CommandManager.literal(BlockStateConditionMode.CONDITION_BOTH.id())
                                                .executes(context -> executeMode(
                                                        context.getSource(),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                        StringArgumentType.getString(context, "name"),
                                                        BlockStateConditionMode.CONDITION_BOTH
                                                ))))))
                .then(CommandManager.literal("offChannel")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(CommandManager.argument("channel", StringArgumentType.string())
                                                .executes(context -> executeOffChannel(
                                                        context.getSource(),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                        StringArgumentType.getString(context, "name"),
                                                        StringArgumentType.getString(context, "channel")
                                                ))))))
                .then(CommandManager.literal("clearOffChannel")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(context -> executeClearOffChannel(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "name")
                                        )))))
                .then(CommandManager.literal("refresh")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(context -> executeRefresh(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "name")
                                        )))))
                .then(CommandManager.literal("test")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(context -> executeTest(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "name")
                                        )))));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, Identifier> slotItemArgument(
            SlotItemExecutor executor
    ) {
        return CommandManager.argument("itemId", IdentifierArgumentType.identifier())
                .then(slotItemModeBranch(ContainerItemCountMode.AT_LEAST, executor))
                .then(slotItemModeBranch(ContainerItemCountMode.EXACTLY, executor))
                .then(slotItemModeBranch(ContainerItemCountMode.AT_MOST, executor));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> slotItemModeBranch(
            ContainerItemCountMode mode,
            SlotItemExecutor executor
    ) {
        return CommandManager.literal(mode.id())
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                        .then(CommandManager.argument("channel", StringArgumentType.string())
                                .executes(context -> executor.execute(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                        StringArgumentType.getString(context, "name"),
                                        IntegerArgumentType.getInteger(context, "slot"),
                                        IdentifierArgumentType.getIdentifier(context, "itemId").toString(),
                                        mode,
                                        IntegerArgumentType.getInteger(context, "count"),
                                        StringArgumentType.getString(context, "channel")
                                ))));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, Identifier> totalItemArgument(
            TotalItemExecutor executor
    ) {
        return CommandManager.argument("itemId", IdentifierArgumentType.identifier())
                .then(totalItemModeBranch(ContainerItemCountMode.AT_LEAST, executor))
                .then(totalItemModeBranch(ContainerItemCountMode.EXACTLY, executor))
                .then(totalItemModeBranch(ContainerItemCountMode.AT_MOST, executor));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> totalItemModeBranch(
            ContainerItemCountMode mode,
            TotalItemExecutor executor
    ) {
        return CommandManager.literal(mode.id())
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                        .then(CommandManager.argument("channel", StringArgumentType.string())
                                .executes(context -> executor.execute(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                        StringArgumentType.getString(context, "name"),
                                        IdentifierArgumentType.getIdentifier(context, "itemId").toString(),
                                        mode,
                                        IntegerArgumentType.getInteger(context, "count"),
                                        StringArgumentType.getString(context, "channel")
                                ))));
    }

    private static int executeAddSlotEmpty(ServerCommandSource source, BlockPos pos, String rawName, int slot, String rawChannel) {
        Target target = validateTarget(source, pos);
        if (target == null || !validateNameAndChannel(source, target.device(), rawName, rawChannel, true)) {
            return 0;
        }
        if (!ContainerItemConditionSupport.isSlotInRange(target.inventory(), slot)) {
            sendError(source, Text.literal("槽位 " + slot + " 超出当前容器范围。"));
            return 0;
        }

        String name = cleanUserText(rawName);
        ContainerItemConditionData condition = newCondition(
                name,
                ContainerItemConditionType.SLOT_EMPTY,
                slot,
                "",
                ContainerItemCountMode.AT_LEAST,
                0,
                rawChannel,
                ContainerItemConditionSupport.matches(target.inventory(), newCondition(
                        name,
                        ContainerItemConditionType.SLOT_EMPTY,
                        slot,
                        "",
                        ContainerItemCountMode.AT_LEAST,
                        0,
                        rawChannel,
                        false
                ))
        );
        SignalDeviceStore.addVirtualItemCondition(source.getWorld(), pos, condition);
        sendCreatedFeedback(source, pos, condition);
        return 1;
    }

    private static int executeAddSlotItem(
            ServerCommandSource source,
            BlockPos pos,
            String rawName,
            int slot,
            String rawItemId,
            ContainerItemCountMode countMode,
            int count,
            String rawChannel
    ) {
        Target target = validateTarget(source, pos);
        if (target == null || !validateNameAndChannel(source, target.device(), rawName, rawChannel, true)) {
            return 0;
        }
        if (!ContainerItemConditionSupport.isSlotInRange(target.inventory(), slot)) {
            sendError(source, Text.literal("槽位 " + slot + " 超出当前容器范围。"));
            return 0;
        }
        String itemId = ContainerItemConditionSupport.normalizeItemId(rawItemId);
        if (!ContainerItemConditionSupport.itemExists(itemId)) {
            sendError(source, Text.literal("物品 ID 不存在：" + rawItemId));
            return 0;
        }

        String name = cleanUserText(rawName);
        ContainerItemConditionData condition = newCondition(
                name,
                ContainerItemConditionType.SLOT_ITEM,
                slot,
                itemId,
                countMode,
                count,
                rawChannel,
                false
        );
        condition = condition.withMatched(ContainerItemConditionSupport.matches(target.inventory(), condition), source.getWorld().getTime(), "已初始化物品条件状态");
        SignalDeviceStore.addVirtualItemCondition(source.getWorld(), pos, condition);
        sendCreatedFeedback(source, pos, condition);
        return 1;
    }

    private static int executeAddTotalItem(
            ServerCommandSource source,
            BlockPos pos,
            String rawName,
            String rawItemId,
            ContainerItemCountMode countMode,
            int count,
            String rawChannel
    ) {
        Target target = validateTarget(source, pos);
        if (target == null || !validateNameAndChannel(source, target.device(), rawName, rawChannel, true)) {
            return 0;
        }
        String itemId = ContainerItemConditionSupport.normalizeItemId(rawItemId);
        if (!ContainerItemConditionSupport.itemExists(itemId)) {
            sendError(source, Text.literal("物品 ID 不存在：" + rawItemId));
            return 0;
        }

        String name = cleanUserText(rawName);
        ContainerItemConditionData condition = newCondition(
                name,
                ContainerItemConditionType.TOTAL_ITEM,
                0,
                itemId,
                countMode,
                count,
                rawChannel,
                false
        );
        condition = condition.withMatched(ContainerItemConditionSupport.matches(target.inventory(), condition), source.getWorld().getTime(), "已初始化物品条件状态");
        SignalDeviceStore.addVirtualItemCondition(source.getWorld(), pos, condition);
        sendCreatedFeedback(source, pos, condition);
        return 1;
    }

    private static int executeList(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getDevice(source, pos);
        if (device == null) {
            return 0;
        }
        sendHeader(source, Text.literal("容器物品条件列表").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("数量", number(device.itemConditions().size())), false);
        if (device.itemConditions().isEmpty()) {
            source.sendFeedback(() -> Text.literal("该虚拟方块设备没有配置物品条件。").formatted(Formatting.YELLOW), false);
            return 1;
        }
        int index = 1;
        for (ContainerItemConditionData condition : device.itemConditions()) {
            int displayIndex = index++;
            source.sendFeedback(() -> Text.literal(displayIndex + ". ").formatted(Formatting.GRAY)
                    .append(nameText(condition.name()))
                    .append(Text.literal("  "))
                    .append(Text.literal(ContainerItemConditionSupport.summary(condition)).formatted(Formatting.WHITE)), false);
            source.sendFeedback(() -> field("  状态", enabledText(condition.enabled()))
                    .append(Text.literal("  "))
                    .append(field("频道", channelOrEmpty(condition.channel())))
                    .append(Text.literal("  "))
                    .append(field("模式", conditionModeText(condition.mode())))
                    .append(Text.literal("  "))
                    .append(field("上次满足", boolText(condition.lastMatched()))), false);
        }
        return 1;
    }

    private static int executeInfo(ServerCommandSource source, BlockPos pos, String rawName) {
        Target target = validateTarget(source, pos);
        if (target == null) {
            return 0;
        }
        ContainerItemConditionData condition = findCondition(target.device(), rawName);
        if (condition == null) {
            sendError(source, Text.literal("找不到物品条件：" + cleanUserText(rawName)));
            return 0;
        }
        sendConditionInfo(source, pos, target.inventory(), condition);
        return 1;
    }

    private static int executeRemove(ServerCommandSource source, BlockPos pos, String rawName) {
        SignalDeviceData device = getDevice(source, pos);
        if (device == null) {
            return 0;
        }
        ContainerItemConditionData condition = findCondition(device, rawName);
        if (condition == null) {
            sendError(source, Text.literal("找不到物品条件：" + cleanUserText(rawName)));
            return 0;
        }
        SignalDeviceStore.removeVirtualItemCondition(source.getWorld(), pos, condition.name());
        sendHeader(source, Text.literal("已删除容器物品条件").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("名称", nameText(condition.name())), false);
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        return 1;
    }

    private static int executeClear(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getDevice(source, pos);
        if (device == null) {
            return 0;
        }
        int count = device.itemConditions().size();
        SignalDeviceStore.clearVirtualItemConditions(source.getWorld(), pos);
        sendHeader(source, Text.literal("已清空容器物品条件").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("清空数量", number(count)), false);
        return 1;
    }

    private static int executeEnable(ServerCommandSource source, BlockPos pos, String rawName, boolean enabled) {
        Target target = validateTarget(source, pos);
        if (target == null) {
            return 0;
        }
        ContainerItemConditionData condition = findCondition(target.device(), rawName);
        if (condition == null) {
            sendError(source, Text.literal("找不到物品条件：" + cleanUserText(rawName)));
            return 0;
        }
        boolean currentMatched = enabled && ContainerItemConditionSupport.matches(target.inventory(), condition);
        ContainerItemConditionData updated = copyCondition(condition, enabled, condition.mode(), condition.offChannel(), currentMatched);
        SignalDeviceStore.updateVirtualItemCondition(source.getWorld(), pos, updated);
        sendHeader(source, Text.literal(enabled ? "已启用容器物品条件" : "已禁用容器物品条件").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("名称", nameText(updated.name())), false);
        source.sendFeedback(() -> field("当前满足", boolText(currentMatched)), false);
        return 1;
    }

    private static int executeMode(ServerCommandSource source, BlockPos pos, String rawName, BlockStateConditionMode mode) {
        SignalDeviceData device = getDevice(source, pos);
        if (device == null) {
            return 0;
        }
        ContainerItemConditionData condition = findCondition(device, rawName);
        if (condition == null) {
            sendError(source, Text.literal("找不到物品条件：" + cleanUserText(rawName)));
            return 0;
        }
        ContainerItemConditionData updated = copyCondition(condition, condition.enabled(), mode.id(), condition.offChannel(), condition.lastMatched());
        SignalDeviceStore.updateVirtualItemCondition(source.getWorld(), pos, updated);
        sendHeader(source, Text.literal("已设置容器物品条件触发模式").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("名称", nameText(updated.name())), false);
        source.sendFeedback(() -> field("模式", conditionModeText(updated.mode())), false);
        return 1;
    }

    private static int executeOffChannel(ServerCommandSource source, BlockPos pos, String rawName, String rawChannel) {
        SignalDeviceData device = getDevice(source, pos);
        if (device == null) {
            return 0;
        }
        ContainerItemConditionData condition = findCondition(device, rawName);
        if (condition == null) {
            sendError(source, Text.literal("找不到物品条件：" + cleanUserText(rawName)));
            return 0;
        }
        String channel = SignalChannel.normalize(rawChannel);
        if (!SignalChannel.isValid(channel)) {
            sendError(source, SignalChannel.validationError(rawChannel));
            return 0;
        }
        ContainerItemConditionData updated = copyCondition(condition, condition.enabled(), condition.mode(), channel, condition.lastMatched());
        SignalDeviceStore.updateVirtualItemCondition(source.getWorld(), pos, updated);
        sendHeader(source, Text.literal("已设置容器物品条件退出频道").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("名称", nameText(updated.name())), false);
        source.sendFeedback(() -> field("退出频道", channelOrEmpty(updated.offChannel())), false);
        return 1;
    }

    private static int executeClearOffChannel(ServerCommandSource source, BlockPos pos, String rawName) {
        SignalDeviceData device = getDevice(source, pos);
        if (device == null) {
            return 0;
        }
        ContainerItemConditionData condition = findCondition(device, rawName);
        if (condition == null) {
            sendError(source, Text.literal("找不到物品条件：" + cleanUserText(rawName)));
            return 0;
        }
        ContainerItemConditionData updated = copyCondition(condition, condition.enabled(), condition.mode(), "", condition.lastMatched());
        SignalDeviceStore.updateVirtualItemCondition(source.getWorld(), pos, updated);
        sendHeader(source, Text.literal("已清空容器物品条件退出频道").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("名称", nameText(updated.name())), false);
        source.sendFeedback(() -> warning("退出边沿会回退使用主频道。"), false);
        return 1;
    }

    private static int executeRefresh(ServerCommandSource source, BlockPos pos, String rawName) {
        Target target = validateTarget(source, pos);
        if (target == null) {
            return 0;
        }
        ContainerItemConditionData condition = findCondition(target.device(), rawName);
        if (condition == null) {
            sendError(source, Text.literal("找不到物品条件：" + cleanUserText(rawName)));
            return 0;
        }
        boolean currentMatched = ContainerItemConditionSupport.matches(target.inventory(), condition);
        ContainerItemConditionData updated = condition.withMatched(currentMatched, source.getWorld().getTime(), "已刷新物品条件状态");
        SignalDeviceStore.updateVirtualItemCondition(source.getWorld(), pos, updated);
        sendHeader(source, Text.literal("已刷新容器物品条件状态").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("名称", nameText(updated.name())), false);
        source.sendFeedback(() -> field("当前满足", boolText(currentMatched)), false);
        return 1;
    }

    private static int executeTest(ServerCommandSource source, BlockPos pos, String rawName) {
        SignalDeviceData device = getDevice(source, pos);
        if (device == null) {
            return 0;
        }
        ContainerItemConditionData condition = findCondition(device, rawName);
        if (condition == null) {
            sendError(source, Text.literal("找不到物品条件：" + cleanUserText(rawName)));
            return 0;
        }
        if (condition.channel().isBlank() || !SignalChannel.isValid(condition.channel())) {
            sendError(source, Text.literal("该物品条件的频道为空或无效。"));
            return 0;
        }
        ActionExecutionResult result = SignalBridgeServer.emit(new SignalEvent(
                condition.channel(),
                source.getEntity() instanceof ServerPlayerEntity player ? player : null,
                source.getWorld(),
                Vec3d.ofCenter(pos),
                ActionSourceType.VIRTUAL_BLOCK_DEVICE,
                device.id(),
                SignalBridgeServer.currentDepth(),
                source.getWorld().getTime(),
                "手动测试物品条件 " + condition.name()
        ));
        SignalDeviceStore.recordVirtualBlockManualTrigger(source.getWorld(), device, result);
        if (!result.success()) {
            sendError(source, result.message());
            return 0;
        }
        sendHeader(source, Text.literal("已测试容器物品条件").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("名称", nameText(condition.name())), false);
        source.sendFeedback(() -> field("频道", channelText(condition.channel())), false);
        source.sendFeedback(() -> field("结果", result.message()), false);
        return 1;
    }

    private static ContainerItemConditionData newCondition(
            String name,
            ContainerItemConditionType type,
            int slot,
            String itemId,
            ContainerItemCountMode countMode,
            int count,
            String channel,
            boolean matched
    ) {
        return new ContainerItemConditionData(
                UUID.randomUUID().toString(),
                name,
                true,
                type.id(),
                slot,
                itemId,
                countMode.id(),
                count,
                SignalChannel.normalize(channel),
                "",
                BlockStateConditionMode.CONDITION_ENTER.id(),
                matched,
                0L,
                0L,
                0L,
                "已初始化物品条件状态"
        ).normalized();
    }

    private static ContainerItemConditionData copyCondition(
            ContainerItemConditionData condition,
            boolean enabled,
            String mode,
            String offChannel,
            boolean matched
    ) {
        return new ContainerItemConditionData(
                condition.id(),
                condition.name(),
                enabled,
                condition.type(),
                condition.slot(),
                condition.itemId(),
                condition.countMode(),
                condition.count(),
                condition.channel(),
                offChannel,
                mode,
                matched,
                condition.lastCheckGameTime(),
                condition.lastTriggerGameTime(),
                condition.lastTriggerWallTimeMillis(),
                condition.lastResult()
        ).normalized();
    }

    private static boolean validateNameAndChannel(
            ServerCommandSource source,
            SignalDeviceData device,
            String rawName,
            String rawChannel,
            boolean requireUnique
    ) {
        String name = cleanUserText(rawName);
        if (name.isBlank()) {
            sendError(source, Text.literal("物品条件名称不能为空。"));
            return false;
        }
        if (requireUnique && findCondition(device, name) != null) {
            sendError(source, Text.literal("该设备已经存在同名物品条件：" + name));
            return false;
        }
        String channel = SignalChannel.normalize(rawChannel);
        if (!SignalChannel.isValid(channel)) {
            sendError(source, SignalChannel.validationError(rawChannel));
            return false;
        }
        return true;
    }

    private static Target validateTarget(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getDevice(source, pos);
        if (device == null) {
            return null;
        }
        BlockState state = source.getWorld().getBlockState(pos);
        if (state.isAir()) {
            sendError(source, Text.literal("当前位置是空气，不能配置容器物品条件。"));
            return null;
        }
        String currentBlockId = VirtualBlockDeviceSupport.blockId(state);
        if (!currentBlockId.equals(device.blockId())) {
            sendError(source, Text.literal("当前方块 ID 与绑定时不一致，请先 refresh 或重新 bind。"));
            return null;
        }
        Inventory inventory = ContainerItemConditionSupport.inventory(source.getWorld(), pos);
        if (inventory == null) {
            sendError(source, Text.literal("当前方块不是可读取 Inventory 的容器。"));
            return null;
        }
        return new Target(device, inventory);
    }

    private static SignalDeviceData getDevice(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = SignalDeviceStore.findVirtualBlockDevice(source.getServer(), source.getWorld(), pos);
        if (device == null) {
            sendError(source, Text.literal("该位置没有虚拟方块发射器。"));
        }
        return device;
    }

    private static ContainerItemConditionData findCondition(SignalDeviceData device, String rawName) {
        if (device == null) {
            return null;
        }
        String name = cleanUserText(rawName);
        for (ContainerItemConditionData condition : device.itemConditions()) {
            if (condition.name().equalsIgnoreCase(name)) {
                return condition.normalized();
            }
        }
        return null;
    }

    private static void sendCreatedFeedback(ServerCommandSource source, BlockPos pos, ContainerItemConditionData condition) {
        sendHeader(source, Text.literal("已添加容器物品条件").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("名称", nameText(condition.name())), false);
        source.sendFeedback(() -> field("类型", typeText(condition.type())), false);
        source.sendFeedback(() -> field("条件", Text.literal(ContainerItemConditionSupport.summary(condition)).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("频道", channelText(condition.channel())), false);
        source.sendFeedback(() -> field("当前满足", boolText(condition.lastMatched())), false);
    }

    private static void sendConditionInfo(
            ServerCommandSource source,
            BlockPos pos,
            Inventory inventory,
            ContainerItemConditionData condition
    ) {
        boolean currentMatched = ContainerItemConditionSupport.matches(inventory, condition);
        List<String> issues = ContainerItemConditionSupport.validate(inventory, condition);
        sendHeader(source, Text.literal("容器物品条件详情").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("名称", nameText(condition.name())), false);
        source.sendFeedback(() -> field("ID", idText(condition.id())), false);
        source.sendFeedback(() -> field("状态", enabledText(condition.enabled())), false);
        source.sendFeedback(() -> field("类型", typeText(condition.type())), false);
        source.sendFeedback(() -> field("条件", Text.literal(ContainerItemConditionSupport.summary(condition)).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("频道", channelText(condition.channel())), false);
        source.sendFeedback(() -> field("退出频道", channelOrEmpty(condition.offChannel())), false);
        source.sendFeedback(() -> field("模式", conditionModeText(condition.mode())), false);
        source.sendFeedback(() -> field("上次满足", boolText(condition.lastMatched())), false);
        source.sendFeedback(() -> field("当前满足", boolText(currentMatched)), false);
        source.sendFeedback(() -> field("最近触发", elapsedOrNever(condition.lastTriggerWallTimeMillis())), false);
        source.sendFeedback(() -> field("最近结果", resultText(condition.lastResult())), false);
        if (issues.isEmpty()) {
            source.sendFeedback(() -> Text.literal("诊断：未发现明显问题。").formatted(Formatting.GREEN), false);
        } else {
            source.sendFeedback(() -> Text.literal("诊断：").formatted(Formatting.YELLOW), false);
            for (String issue : issues) {
                source.sendFeedback(() -> Text.literal("- " + issue).formatted(Formatting.YELLOW), false);
            }
        }
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

    private static MutableText warning(String message) {
        return Text.literal(message).formatted(Formatting.YELLOW);
    }

    private static MutableText nameText(String name) {
        return Text.literal(name == null || name.isBlank() ? "未命名" : name).formatted(Formatting.GOLD);
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

    private static MutableText number(int value) {
        return Text.literal(Integer.toString(value)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText enabledText(boolean enabled) {
        return Text.literal(enabled ? "启用" : "禁用").formatted(enabled ? Formatting.GREEN : Formatting.RED);
    }

    private static MutableText boolText(boolean value) {
        return Text.literal(value ? "是" : "否").formatted(value ? Formatting.GREEN : Formatting.GRAY);
    }

    private static MutableText typeText(String type) {
        return Text.literal(type == null || type.isBlank() ? "未知" : type).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText conditionModeText(String mode) {
        return Text.literal(BlockStateConditionMode.normalize(mode)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText resultText(String value) {
        return Text.literal(value == null || value.isBlank() ? "尚无结果" : value)
                .formatted(value == null || value.isBlank() ? Formatting.YELLOW : Formatting.WHITE);
    }

    private static MutableText elapsedOrNever(long wallTimeMillis) {
        if (wallTimeMillis <= 0L) {
            return Text.literal("尚未触发").formatted(Formatting.YELLOW);
        }
        long elapsedMillis = System.currentTimeMillis() - wallTimeMillis;
        if (elapsedMillis < 3_000L) {
            return Text.literal("刚刚").formatted(Formatting.LIGHT_PURPLE);
        }
        long seconds = Math.max(0L, elapsedMillis / 1_000L);
        if (seconds >= 60L) {
            return Text.literal((seconds / 60L) + " 分 " + (seconds % 60L) + " 秒前").formatted(Formatting.LIGHT_PURPLE);
        }
        return Text.literal(seconds + " 秒前").formatted(Formatting.LIGHT_PURPLE);
    }

    private interface SlotItemExecutor {
        int execute(
                ServerCommandSource source,
                BlockPos pos,
                String name,
                int slot,
                String itemId,
                ContainerItemCountMode mode,
                int count,
                String channel
        );
    }

    private interface TotalItemExecutor {
        int execute(
                ServerCommandSource source,
                BlockPos pos,
                String name,
                String itemId,
                ContainerItemCountMode mode,
                int count,
                String channel
        );
    }

    private record Target(
            SignalDeviceData device,
            Inventory inventory
    ) {
    }
}
