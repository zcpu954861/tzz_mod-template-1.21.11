package com.zcpu.tzzmod.signal.device;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherCommandSupport;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherSupport;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class VirtualBlockInteractionItemCommand {
    private static final int MAX_COUNT = 64_000;

    private VirtualBlockInteractionItemCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("interactionItem")
                .then(CommandManager.literal("setFromHand")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeSetFromHand(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("clear")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeClear(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("enable")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeEnabled(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                        true
                                ))))
                .then(CommandManager.literal("disable")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeEnabled(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                        false
                                ))))
                .then(CommandManager.literal("option")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(optionBranch("matchDamage"))
                                .then(optionBranch("matchCustomName"))
                                .then(optionBranch("matchLore"))
                                .then(optionBranch("matchCustomData"))
                                .then(optionBranch("matchComponents"))))
                .then(CommandManager.literal("count")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(countBranch(ContainerItemCountMode.AT_LEAST))
                                .then(countBranch(ContainerItemCountMode.EXACTLY))
                                .then(countBranch(ContainerItemCountMode.AT_MOST))
                                .then(CommandManager.literal(ContainerItemCountMode.IGNORE.id())
                                        .executes(context -> executeCount(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                ContainerItemCountMode.IGNORE,
                                                0
                                        )))))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeInfo(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> optionBranch(String option) {
        return CommandManager.literal(option)
                .then(CommandManager.literal("enable")
                        .executes(context -> executeOption(
                                context.getSource(),
                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                option,
                                true
                        )))
                .then(CommandManager.literal("disable")
                        .executes(context -> executeOption(
                                context.getSource(),
                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                option,
                                false
                        )));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> countBranch(ContainerItemCountMode mode) {
        return CommandManager.literal(mode.id())
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1, MAX_COUNT))
                        .executes(context -> executeCount(
                                context.getSource(),
                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                mode,
                                IntegerArgumentType.getInteger(context, "count")
                        )));
    }

    private static int executeSetFromHand(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            sendError(source, Text.literal("该命令必须由玩家执行。"));
            return 0;
        }

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            sendError(source, Text.literal("主手为空，无法捕获物品模板。"));
            return 0;
        }

        ItemStackMatcherData matcher = ItemStackMatcherSupport.captureTemplate(stack, ContainerItemCountMode.AT_LEAST, 1);
        SignalDeviceData updated = SignalDeviceStore.updateVirtualInteractionItemMatcher(
                source.getWorld(),
                pos,
                matcher,
                true,
                "已从主手捕获交互物品模板"
        );
        sendHeader(source, Text.literal("已设置交互主手物品匹配").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("模板", Text.literal(ItemStackMatcherSupport.summary(updated.interactionItemMatcher())).formatted(Formatting.WHITE)), false);
        if (updated.interactChannel().isBlank()) {
            source.sendFeedback(() -> warning("尚未设置 interactChannel，设置后右键才会发出 signal。"), false);
        }
        return 1;
    }

    private static int executeClear(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        SignalDeviceData updated = SignalDeviceStore.clearVirtualInteractionItemMatcher(source.getWorld(), pos);
        sendHeader(source, Text.literal("已清空交互主手物品匹配").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("启用", boolText(updated.interactionItemMatcherEnabled())), false);
        return 1;
    }

    private static int executeEnabled(ServerCommandSource source, BlockPos pos, boolean enabled) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        if (enabled && (device.interactionItemMatcher() == null || !device.interactionItemMatcher().normalized().enabled())) {
            sendError(source, Text.literal("还没有交互物品模板，请先使用 setFromHand。"));
            return 0;
        }

        SignalDeviceData updated = SignalDeviceStore.updateVirtualInteractionItemMatcher(
                source.getWorld(),
                pos,
                device.interactionItemMatcher(),
                enabled,
                enabled ? "已启用交互主手物品匹配" : "已禁用交互主手物品匹配"
        );
        sendHeader(source, Text.literal(enabled ? "已启用交互主手物品匹配" : "已禁用交互主手物品匹配").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("启用", boolText(updated.interactionItemMatcherEnabled())), false);
        return 1;
    }

    private static int executeOption(ServerCommandSource source, BlockPos pos, String option, boolean enabled) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        if (!ItemStackMatcherCommandSupport.isOption(option)) {
            sendError(source, Text.literal("未知匹配选项：" + option));
            return 0;
        }
        if (device.interactionItemMatcher() == null || !device.interactionItemMatcher().normalized().enabled()) {
            sendError(source, Text.literal("还没有交互物品模板，请先使用 setFromHand。"));
            return 0;
        }

        ItemStackMatcherData matcher = ItemStackMatcherCommandSupport.withOption(device.interactionItemMatcher(), option, enabled);
        SignalDeviceData updated = SignalDeviceStore.updateVirtualInteractionItemMatcher(
                source.getWorld(),
                pos,
                matcher,
                device.interactionItemMatcherEnabled(),
                "已更新交互物品匹配选项"
        );
        sendHeader(source, Text.literal("已更新交互物品匹配选项").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("选项", Text.literal(option).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("启用", boolText(enabled)), false);
        source.sendFeedback(() -> field("模板", Text.literal(ItemStackMatcherSupport.summary(updated.interactionItemMatcher())).formatted(Formatting.WHITE)), false);
        return 1;
    }

    private static int executeCount(ServerCommandSource source, BlockPos pos, ContainerItemCountMode mode, int count) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        if (device.interactionItemMatcher() == null || !device.interactionItemMatcher().normalized().enabled()) {
            sendError(source, Text.literal("还没有交互物品模板，请先使用 setFromHand。"));
            return 0;
        }

        ItemStackMatcherData matcher = ItemStackMatcherSupport.withCount(device.interactionItemMatcher(), mode, count);
        SignalDeviceData updated = SignalDeviceStore.updateVirtualInteractionItemMatcher(
                source.getWorld(),
                pos,
                matcher,
                device.interactionItemMatcherEnabled(),
                "已更新交互物品数量匹配"
        );
        sendHeader(source, Text.literal("已更新交互物品数量匹配").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("数量模式", Text.literal(updated.interactionItemMatcher().countMode()).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("数量", Text.literal(Integer.toString(updated.interactionItemMatcher().requiredCount())).formatted(Formatting.LIGHT_PURPLE)), false);
        return 1;
    }

    private static int executeInfo(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        sendHeader(source, Text.literal("交互主手物品匹配详情").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("启用", boolText(device.interactionItemMatcherEnabled())), false);
        source.sendFeedback(() -> field("模板", Text.literal(ItemStackMatcherSupport.summary(device.interactionItemMatcher())).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("最近匹配", boolText(device.lastInteractionItemMatched())), false);
        source.sendFeedback(() -> field("最近结果", Text.literal(device.lastInteractionItemResult().isBlank() ? "暂无结果" : device.lastInteractionItemResult()).formatted(Formatting.WHITE)), false);
        if (device.interactChannel().isBlank()) {
            source.sendFeedback(() -> warning("尚未设置 interactChannel。"), false);
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

    private static MutableText posText(BlockPos pos) {
        return Text.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ()).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText boolText(boolean value) {
        return Text.literal(value ? "是" : "否").formatted(value ? Formatting.GREEN : Formatting.GRAY);
    }
}
