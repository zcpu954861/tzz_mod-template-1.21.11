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
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherCommandSupport;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherSupport;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
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
                .then(CommandManager.literal("addSlotMatchFromHand")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(CommandManager.argument("slot", IntegerArgumentType.integer(0))
                                                .then(slotMatcherModeBranch(ContainerItemCountMode.AT_LEAST, (source, pos, name, slot, mode, count, channel) ->
                                                        executeAddSlotMatcher(source, pos, name, slot, null, mode, count, channel, true)))
                                                .then(slotMatcherModeBranch(ContainerItemCountMode.EXACTLY, (source, pos, name, slot, mode, count, channel) ->
                                                        executeAddSlotMatcher(source, pos, name, slot, null, mode, count, channel, true)))
                                                .then(slotMatcherModeBranch(ContainerItemCountMode.AT_MOST, (source, pos, name, slot, mode, count, channel) ->
                                                        executeAddSlotMatcher(source, pos, name, slot, null, mode, count, channel, true)))))))
                .then(CommandManager.literal("addSlotMatchFromSlot")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(CommandManager.argument("targetSlot", IntegerArgumentType.integer(0))
                                                .then(CommandManager.argument("templateSlot", IntegerArgumentType.integer(0))
                                                        .then(slotMatcherFromSlotModeBranch(ContainerItemCountMode.AT_LEAST, (source, pos, name, targetSlot, templateSlot, mode, count, channel) ->
                                                                executeAddSlotMatcher(source, pos, name, targetSlot, templateSlot, mode, count, channel, false)))
                                                        .then(slotMatcherFromSlotModeBranch(ContainerItemCountMode.EXACTLY, (source, pos, name, targetSlot, templateSlot, mode, count, channel) ->
                                                                executeAddSlotMatcher(source, pos, name, targetSlot, templateSlot, mode, count, channel, false)))
                                                        .then(slotMatcherFromSlotModeBranch(ContainerItemCountMode.AT_MOST, (source, pos, name, targetSlot, templateSlot, mode, count, channel) ->
                                                                executeAddSlotMatcher(source, pos, name, targetSlot, templateSlot, mode, count, channel, false))))))))
                .then(CommandManager.literal("addTotalMatchFromHand")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(totalMatcherModeBranch(ContainerItemCountMode.AT_LEAST, (source, pos, name, mode, count, channel) ->
                                                executeAddTotalMatcher(source, pos, name, null, mode, count, channel, true)))
                                        .then(totalMatcherModeBranch(ContainerItemCountMode.EXACTLY, (source, pos, name, mode, count, channel) ->
                                                executeAddTotalMatcher(source, pos, name, null, mode, count, channel, true)))
                                        .then(totalMatcherModeBranch(ContainerItemCountMode.AT_MOST, (source, pos, name, mode, count, channel) ->
                                                executeAddTotalMatcher(source, pos, name, null, mode, count, channel, true))))))
                .then(CommandManager.literal("addTotalMatchFromSlot")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(CommandManager.argument("templateSlot", IntegerArgumentType.integer(0))
                                                .then(totalMatcherFromSlotModeBranch(ContainerItemCountMode.AT_LEAST, (source, pos, name, templateSlot, mode, count, channel) ->
                                                        executeAddTotalMatcher(source, pos, name, templateSlot, mode, count, channel, false)))
                                                .then(totalMatcherFromSlotModeBranch(ContainerItemCountMode.EXACTLY, (source, pos, name, templateSlot, mode, count, channel) ->
                                                        executeAddTotalMatcher(source, pos, name, templateSlot, mode, count, channel, false)))
                                                .then(totalMatcherFromSlotModeBranch(ContainerItemCountMode.AT_MOST, (source, pos, name, templateSlot, mode, count, channel) ->
                                                        executeAddTotalMatcher(source, pos, name, templateSlot, mode, count, channel, false)))))))
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
                                        )))))
                .then(CommandManager.literal("matcherInfo")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(context -> executeMatcherInfo(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "name")
                                        )))))
                .then(CommandManager.literal("matcherFromHand")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(context -> executeMatcherFromHand(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "name")
                                        )))))
                .then(CommandManager.literal("matcherFromSlot")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(CommandManager.argument("slot", IntegerArgumentType.integer(0))
                                                .executes(context -> executeMatcherFromSlot(
                                                        context.getSource(),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                        StringArgumentType.getString(context, "name"),
                                                        IntegerArgumentType.getInteger(context, "slot")
                                                ))))))
                .then(CommandManager.literal("matcherOption")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(CommandManager.literal("matchDamage")
                                                .then(matcherOptionStateBranch("matchDamage", true))
                                                .then(matcherOptionStateBranch("matchDamage", false)))
                                        .then(CommandManager.literal("matchCustomName")
                                                .then(matcherOptionStateBranch("matchCustomName", true))
                                                .then(matcherOptionStateBranch("matchCustomName", false)))
                                        .then(CommandManager.literal("matchLore")
                                                .then(matcherOptionStateBranch("matchLore", true))
                                                .then(matcherOptionStateBranch("matchLore", false)))
                                        .then(CommandManager.literal("matchCustomData")
                                                .then(matcherOptionStateBranch("matchCustomData", true))
                                                .then(matcherOptionStateBranch("matchCustomData", false)))
                                        .then(CommandManager.literal("matchComponents")
                                                .then(matcherOptionStateBranch("matchComponents", true))
                                                .then(matcherOptionStateBranch("matchComponents", false))))))
                .then(CommandManager.literal("matcherCount")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(matcherCountBranch(ContainerItemCountMode.AT_LEAST))
                                        .then(matcherCountBranch(ContainerItemCountMode.EXACTLY))
                                        .then(matcherCountBranch(ContainerItemCountMode.AT_MOST))
                                        .then(CommandManager.literal(ContainerItemCountMode.IGNORE.id())
                                                .executes(context -> executeMatcherCount(
                                                        context.getSource(),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                        StringArgumentType.getString(context, "name"),
                                                        ContainerItemCountMode.IGNORE,
                                                        0
                                                ))))));
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

    private static LiteralArgumentBuilder<ServerCommandSource> slotMatcherModeBranch(
            ContainerItemCountMode mode,
            SlotMatcherExecutor executor
    ) {
        return CommandManager.literal(mode.id())
                .then(slotMatcherCountBranch(mode, executor));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, Integer> slotMatcherCountBranch(
            ContainerItemCountMode mode,
            SlotMatcherExecutor executor
    ) {
        return CommandManager.argument("count", IntegerArgumentType.integer(1))
                .then(CommandManager.argument("channel", StringArgumentType.string())
                        .executes(context -> executor.execute(
                                context.getSource(),
                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                StringArgumentType.getString(context, "name"),
                                IntegerArgumentType.getInteger(context, "slot"),
                                mode,
                                IntegerArgumentType.getInteger(context, "count"),
                                StringArgumentType.getString(context, "channel")
                        )));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> slotMatcherFromSlotModeBranch(
            ContainerItemCountMode mode,
            SlotMatcherFromSlotExecutor executor
    ) {
        return CommandManager.literal(mode.id())
                .then(slotMatcherFromSlotCountBranch(mode, executor));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, Integer> slotMatcherFromSlotCountBranch(
            ContainerItemCountMode mode,
            SlotMatcherFromSlotExecutor executor
    ) {
        return CommandManager.argument("count", IntegerArgumentType.integer(1))
                .then(CommandManager.argument("channel", StringArgumentType.string())
                        .executes(context -> executor.execute(
                                context.getSource(),
                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                StringArgumentType.getString(context, "name"),
                                IntegerArgumentType.getInteger(context, "targetSlot"),
                                IntegerArgumentType.getInteger(context, "templateSlot"),
                                mode,
                                IntegerArgumentType.getInteger(context, "count"),
                                StringArgumentType.getString(context, "channel")
                        )));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> totalMatcherModeBranch(
            ContainerItemCountMode mode,
            TotalMatcherExecutor executor
    ) {
        return CommandManager.literal(mode.id())
                .then(totalMatcherCountBranch(mode, executor));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, Integer> totalMatcherCountBranch(
            ContainerItemCountMode mode,
            TotalMatcherExecutor executor
    ) {
        return CommandManager.argument("count", IntegerArgumentType.integer(1))
                .then(CommandManager.argument("channel", StringArgumentType.string())
                        .executes(context -> executor.execute(
                                context.getSource(),
                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                StringArgumentType.getString(context, "name"),
                                mode,
                                IntegerArgumentType.getInteger(context, "count"),
                                StringArgumentType.getString(context, "channel")
                        )));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> totalMatcherFromSlotModeBranch(
            ContainerItemCountMode mode,
            TotalMatcherFromSlotExecutor executor
    ) {
        return CommandManager.literal(mode.id())
                .then(totalMatcherFromSlotCountBranch(mode, executor));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, Integer> totalMatcherFromSlotCountBranch(
            ContainerItemCountMode mode,
            TotalMatcherFromSlotExecutor executor
    ) {
        return CommandManager.argument("count", IntegerArgumentType.integer(1))
                .then(CommandManager.argument("channel", StringArgumentType.string())
                        .executes(context -> executor.execute(
                                context.getSource(),
                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                StringArgumentType.getString(context, "name"),
                                IntegerArgumentType.getInteger(context, "templateSlot"),
                                mode,
                                IntegerArgumentType.getInteger(context, "count"),
                                StringArgumentType.getString(context, "channel")
                        )));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> matcherOptionStateBranch(String option, boolean enabled) {
        return CommandManager.literal(enabled ? "enable" : "disable")
                .executes(context -> executeMatcherOption(
                        context.getSource(),
                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                        StringArgumentType.getString(context, "name"),
                        option,
                        enabled
                ));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> matcherCountBranch(ContainerItemCountMode mode) {
        return CommandManager.literal(mode.id())
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                        .executes(context -> executeMatcherCount(
                                context.getSource(),
                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                StringArgumentType.getString(context, "name"),
                                mode,
                                IntegerArgumentType.getInteger(context, "count")
                        )));
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

    private static int executeAddSlotMatcher(
            ServerCommandSource source,
            BlockPos pos,
            String rawName,
            int targetSlot,
            Integer templateSlot,
            ContainerItemCountMode countMode,
            int count,
            String rawChannel,
            boolean fromHand
    ) {
        Target target = validateTarget(source, pos);
        if (target == null || !validateNameAndChannel(source, target.device(), rawName, rawChannel, true)) {
            return 0;
        }
        if (!ContainerItemConditionSupport.isSlotInRange(target.inventory(), targetSlot)) {
            sendError(source, Text.literal("槽位 " + targetSlot + " 超出当前容器范围。"));
            return 0;
        }
        ItemStack template = templateStack(source, target.inventory(), templateSlot, fromHand);
        if (template.isEmpty()) {
            sendError(source, Text.literal(fromHand ? "主手物品不能为空。" : "模板槽位不能为空。"));
            return 0;
        }

        String name = cleanUserText(rawName);
        ItemStackMatcherData matcher = ItemStackMatcherSupport.captureTemplate(template, countMode, count);
        ContainerItemConditionData condition = newMatcherCondition(
                name,
                ContainerItemConditionType.SLOT_MATCHER,
                targetSlot,
                matcher,
                rawChannel,
                false
        );
        condition = condition.withMatched(ContainerItemConditionSupport.matches(target.inventory(), condition), source.getWorld().getTime(), "已初始化物品模板条件状态");
        SignalDeviceStore.addVirtualItemCondition(source.getWorld(), pos, condition);
        sendCreatedFeedback(source, pos, condition);
        return 1;
    }

    private static int executeAddTotalMatcher(
            ServerCommandSource source,
            BlockPos pos,
            String rawName,
            Integer templateSlot,
            ContainerItemCountMode countMode,
            int count,
            String rawChannel,
            boolean fromHand
    ) {
        Target target = validateTarget(source, pos);
        if (target == null || !validateNameAndChannel(source, target.device(), rawName, rawChannel, true)) {
            return 0;
        }
        ItemStack template = templateStack(source, target.inventory(), templateSlot, fromHand);
        if (template.isEmpty()) {
            sendError(source, Text.literal(fromHand ? "主手物品不能为空。" : "模板槽位不能为空。"));
            return 0;
        }

        String name = cleanUserText(rawName);
        ItemStackMatcherData matcher = ItemStackMatcherSupport.captureTemplate(template, countMode, count);
        ContainerItemConditionData condition = newMatcherCondition(
                name,
                ContainerItemConditionType.TOTAL_MATCHER,
                0,
                matcher,
                rawChannel,
                false
        );
        condition = condition.withMatched(ContainerItemConditionSupport.matches(target.inventory(), condition), source.getWorld().getTime(), "已初始化物品模板条件状态");
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

    private static int executeMatcherInfo(ServerCommandSource source, BlockPos pos, String rawName) {
        Target target = validateTarget(source, pos);
        if (target == null) {
            return 0;
        }
        ContainerItemConditionData condition = findCondition(target.device(), rawName);
        if (condition == null) {
            sendError(source, Text.literal("找不到物品条件：" + cleanUserText(rawName)));
            return 0;
        }
        if (!isMatcherCondition(condition)) {
            sendError(source, Text.literal("该物品条件不是 ItemStack 模板条件。"));
            return 0;
        }
        sendMatcherInfo(source, condition);
        return 1;
    }

    private static int executeMatcherFromHand(ServerCommandSource source, BlockPos pos, String rawName) {
        Target target = validateTarget(source, pos);
        if (target == null) {
            return 0;
        }
        ContainerItemConditionData condition = findCondition(target.device(), rawName);
        if (condition == null) {
            sendError(source, Text.literal("找不到物品条件：" + cleanUserText(rawName)));
            return 0;
        }
        if (!isMatcherCondition(condition)) {
            sendError(source, Text.literal("该物品条件不是 ItemStack 模板条件。"));
            return 0;
        }
        ItemStack template = templateStack(source, target.inventory(), null, true);
        if (template.isEmpty()) {
            sendError(source, Text.literal("主手物品不能为空。"));
            return 0;
        }
        ContainerItemConditionData updated = updateMatcher(source, pos, target.inventory(), condition, template);
        sendHeader(source, Text.literal("已从主手更新物品模板").formatted(Formatting.GREEN));
        sendMatcherInfo(source, updated);
        return 1;
    }

    private static int executeMatcherFromSlot(ServerCommandSource source, BlockPos pos, String rawName, int slot) {
        Target target = validateTarget(source, pos);
        if (target == null) {
            return 0;
        }
        ContainerItemConditionData condition = findCondition(target.device(), rawName);
        if (condition == null) {
            sendError(source, Text.literal("找不到物品条件：" + cleanUserText(rawName)));
            return 0;
        }
        if (!isMatcherCondition(condition)) {
            sendError(source, Text.literal("该物品条件不是 ItemStack 模板条件。"));
            return 0;
        }
        if (!ContainerItemConditionSupport.isSlotInRange(target.inventory(), slot)) {
            sendError(source, Text.literal("槽位 " + slot + " 超出当前容器范围。"));
            return 0;
        }
        ItemStack template = target.inventory().getStack(slot);
        if (template.isEmpty()) {
            sendError(source, Text.literal("模板槽位不能为空。"));
            return 0;
        }
        ContainerItemConditionData updated = updateMatcher(source, pos, target.inventory(), condition, template);
        sendHeader(source, Text.literal("已从容器槽位更新物品模板").formatted(Formatting.GREEN));
        sendMatcherInfo(source, updated);
        return 1;
    }

    private static int executeMatcherOption(ServerCommandSource source, BlockPos pos, String rawName, String option, boolean enabled) {
        Target target = validateTarget(source, pos);
        if (target == null) {
            return 0;
        }
        ContainerItemConditionData condition = findCondition(target.device(), rawName);
        if (condition == null) {
            sendError(source, Text.literal("找不到物品条件：" + cleanUserText(rawName)));
            return 0;
        }
        if (!isMatcherCondition(condition)) {
            sendError(source, Text.literal("该物品条件不是 ItemStack 模板条件。"));
            return 0;
        }
        if (!ItemStackMatcherCommandSupport.isOption(option)) {
            sendError(source, Text.literal("未知匹配选项：" + option));
            return 0;
        }
        ItemStackMatcherData matcher = ItemStackMatcherCommandSupport.withOption(condition.matcher(), option, enabled);
        ContainerItemConditionData updated = withMatcher(condition, matcher);
        boolean currentMatched = ContainerItemConditionSupport.matches(target.inventory(), updated);
        updated = updated.withMatched(currentMatched, source.getWorld().getTime(), "已更新物品模板匹配选项");
        SignalDeviceStore.updateVirtualItemCondition(source.getWorld(), pos, updated);
        sendHeader(source, Text.literal("已更新物品模板匹配选项").formatted(Formatting.GREEN));
        sendMatcherInfo(source, updated);
        return 1;
    }

    private static int executeMatcherCount(ServerCommandSource source, BlockPos pos, String rawName, ContainerItemCountMode mode, int count) {
        Target target = validateTarget(source, pos);
        if (target == null) {
            return 0;
        }
        ContainerItemConditionData condition = findCondition(target.device(), rawName);
        if (condition == null) {
            sendError(source, Text.literal("找不到物品条件：" + cleanUserText(rawName)));
            return 0;
        }
        if (!isMatcherCondition(condition)) {
            sendError(source, Text.literal("该物品条件不是 ItemStack 模板条件。"));
            return 0;
        }
        ItemStackMatcherData matcher = ItemStackMatcherSupport.withCount(condition.matcher(), mode, count);
        ContainerItemConditionData updated = withMatcher(condition, matcher);
        boolean currentMatched = ContainerItemConditionSupport.matches(target.inventory(), updated);
        updated = updated.withMatched(currentMatched, source.getWorld().getTime(), "已更新物品模板数量规则");
        SignalDeviceStore.updateVirtualItemCondition(source.getWorld(), pos, updated);
        sendHeader(source, Text.literal("已更新物品模板数量规则").formatted(Formatting.GREEN));
        sendMatcherInfo(source, updated);
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

    private static ContainerItemConditionData newMatcherCondition(
            String name,
            ContainerItemConditionType type,
            int slot,
            ItemStackMatcherData matcher,
            String channel,
            boolean matched
    ) {
        ItemStackMatcherData cleanMatcher = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        return new ContainerItemConditionData(
                UUID.randomUUID().toString(),
                name,
                true,
                type.id(),
                slot,
                cleanMatcher.templateItemId(),
                cleanMatcher.countMode(),
                cleanMatcher.requiredCount(),
                SignalChannel.normalize(channel),
                "",
                BlockStateConditionMode.CONDITION_ENTER.id(),
                matched,
                0L,
                0L,
                0L,
                "已初始化物品模板条件状态",
                cleanMatcher
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
                condition.lastResult(),
                condition.matcher()
        ).normalized();
    }

    private static ContainerItemConditionData withMatcher(
            ContainerItemConditionData condition,
            ItemStackMatcherData matcher
    ) {
        ItemStackMatcherData cleanMatcher = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        return new ContainerItemConditionData(
                condition.id(),
                condition.name(),
                condition.enabled(),
                condition.type(),
                condition.slot(),
                cleanMatcher.templateItemId(),
                cleanMatcher.countMode(),
                cleanMatcher.requiredCount(),
                condition.channel(),
                condition.offChannel(),
                condition.mode(),
                condition.lastMatched(),
                condition.lastCheckGameTime(),
                condition.lastTriggerGameTime(),
                condition.lastTriggerWallTimeMillis(),
                condition.lastResult(),
                cleanMatcher
        ).normalized();
    }

    private static ContainerItemConditionData updateMatcher(
            ServerCommandSource source,
            BlockPos pos,
            Inventory inventory,
            ContainerItemConditionData condition,
            ItemStack template
    ) {
        ItemStackMatcherData existing = condition.matcher() == null ? ItemStackMatcherData.empty() : condition.matcher().normalized();
        ContainerItemCountMode mode = ContainerItemCountMode.fromId(existing.countMode());
        int count = ContainerItemCountMode.IGNORE.id().equals(existing.countMode()) ? 0 : existing.requiredCount();
        ItemStackMatcherData captured = ItemStackMatcherSupport.captureTemplate(template, mode, count);
        captured = new ItemStackMatcherData(
                captured.enabled(),
                captured.templateItemId(),
                captured.templateCount(),
                captured.countMode(),
                captured.requiredCount(),
                captured.matchItemId(),
                existing.matchDamage(),
                existing.matchCustomName(),
                existing.matchLore(),
                existing.matchCustomData(),
                existing.matchComponents(),
                captured.templateDamage(),
                captured.templateCustomName(),
                captured.templateLore(),
                captured.templateCustomData(),
                captured.templateComponents(),
                captured.templateSummary(),
                existing.createdWallTimeMillis() > 0L ? existing.createdWallTimeMillis() : captured.createdWallTimeMillis(),
                System.currentTimeMillis()
        ).normalized();
        ContainerItemConditionData updated = withMatcher(condition, captured);
        boolean currentMatched = ContainerItemConditionSupport.matches(inventory, updated);
        updated = updated.withMatched(currentMatched, source.getWorld().getTime(), "已刷新物品模板匹配状态");
        SignalDeviceStore.updateVirtualItemCondition(source.getWorld(), pos, updated);
        return updated;
    }

    private static ItemStack templateStack(ServerCommandSource source, Inventory inventory, Integer slot, boolean fromHand) {
        if (fromHand) {
            if (source.getEntity() instanceof ServerPlayerEntity player) {
                return player.getMainHandStack();
            }
            return ItemStack.EMPTY;
        }
        if (slot == null || !ContainerItemConditionSupport.isSlotInRange(inventory, slot)) {
            return ItemStack.EMPTY;
        }
        return inventory.getStack(slot);
    }

    private static boolean isMatcherCondition(ContainerItemConditionData condition) {
        String type = condition == null ? "" : ContainerItemConditionType.normalize(condition.type());
        return ContainerItemConditionType.SLOT_MATCHER.id().equals(type)
                || ContainerItemConditionType.TOTAL_MATCHER.id().equals(type);
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

    private static void sendMatcherInfo(ServerCommandSource source, ContainerItemConditionData condition) {
        ItemStackMatcherData matcher = condition.matcher() == null ? ItemStackMatcherData.empty() : condition.matcher().normalized();
        sendHeader(source, Text.literal("ItemStack 模板匹配详情").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("条件名称", nameText(condition.name())), false);
        source.sendFeedback(() -> field("条件类型", typeText(condition.type())), false);
        source.sendFeedback(() -> field("模板", Text.literal(ItemStackMatcherSupport.summary(matcher)).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("物品 ID", Text.literal(matcher.templateItemId().isBlank() ? "未设置" : matcher.templateItemId()).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("数量模式", Text.literal(matcher.countMode()).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("要求数量", Text.literal(Integer.toString(matcher.requiredCount())).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("匹配物品 ID", boolText(matcher.matchItemId())), false);
        source.sendFeedback(() -> field("匹配耐久", boolText(matcher.matchDamage())), false);
        source.sendFeedback(() -> field("匹配自定义名称", boolText(matcher.matchCustomName())), false);
        source.sendFeedback(() -> field("匹配 lore", boolText(matcher.matchLore())), false);
        source.sendFeedback(() -> field("匹配 custom_data", boolText(matcher.matchCustomData())), false);
        source.sendFeedback(() -> field("匹配 data components", boolText(matcher.matchComponents())), false);
        source.sendFeedback(() -> field("上次满足", boolText(condition.lastMatched())), false);
        source.sendFeedback(() -> field("最近结果", resultText(condition.lastResult())), false);
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

    private interface SlotMatcherExecutor {
        int execute(
                ServerCommandSource source,
                BlockPos pos,
                String name,
                int slot,
                ContainerItemCountMode mode,
                int count,
                String channel
        );
    }

    private interface SlotMatcherFromSlotExecutor {
        int execute(
                ServerCommandSource source,
                BlockPos pos,
                String name,
                int targetSlot,
                int templateSlot,
                ContainerItemCountMode mode,
                int count,
                String channel
        );
    }

    private interface TotalMatcherExecutor {
        int execute(
                ServerCommandSource source,
                BlockPos pos,
                String name,
                ContainerItemCountMode mode,
                int count,
                String channel
        );
    }

    private interface TotalMatcherFromSlotExecutor {
        int execute(
                ServerCommandSource source,
                BlockPos pos,
                String name,
                int templateSlot,
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
