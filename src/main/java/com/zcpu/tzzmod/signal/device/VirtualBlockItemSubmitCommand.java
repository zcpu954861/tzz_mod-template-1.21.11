package com.zcpu.tzzmod.signal.device;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zcpu.tzzmod.signal.device.item.InventoryConsumeOrder;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcher;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherCommandSupport;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherSupport;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class VirtualBlockItemSubmitCommand {
    private static final int MAX_COUNT = 64_000;

    private VirtualBlockItemSubmitCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("itemSubmit")
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
                .then(CommandManager.literal("addFromHand")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(addModeBranch(ContainerItemCountMode.AT_LEAST))
                                        .then(addModeBranch(ContainerItemCountMode.EXACTLY))
                                        .then(addModeBranch(ContainerItemCountMode.AT_MOST))
                                        .then(CommandManager.literal(ContainerItemCountMode.IGNORE.id())
                                                .executes(context -> executeAddFromHand(
                                                        context.getSource(),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                        StringArgumentType.getString(context, "name"),
                                                        ContainerItemCountMode.IGNORE,
                                                        0
                                                ))))))
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
                .then(CommandManager.literal("infoAll")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeInfoAll(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
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
                .then(CommandManager.literal("enableRequirement")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(context -> executeRequirementEnabled(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "name"),
                                                true
                                        )))))
                .then(CommandManager.literal("disableRequirement")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(context -> executeRequirementEnabled(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "name"),
                                                false
                                        )))))
                .then(CommandManager.literal("matcherFromHand")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .executes(context -> executeMatcherFromHand(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "name")
                                        )))))
                .then(CommandManager.literal("matcherOption")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(optionBranch("matchDamage"))
                                        .then(optionBranch("matchCustomName"))
                                        .then(optionBranch("matchLore"))
                                        .then(optionBranch("matchCustomData"))
                                        .then(optionBranch("matchComponents")))))
                .then(CommandManager.literal("count")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(countModeBranch(ContainerItemCountMode.AT_LEAST))
                                        .then(countModeBranch(ContainerItemCountMode.EXACTLY))
                                        .then(countModeBranch(ContainerItemCountMode.AT_MOST))
                                        .then(CommandManager.literal(ContainerItemCountMode.IGNORE.id())
                                                .executes(context -> executeCount(
                                                        context.getSource(),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                        StringArgumentType.getString(context, "name"),
                                                        ContainerItemCountMode.IGNORE,
                                                        0
                                                ))))))
                .then(CommandManager.literal("consume")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.literal("enable")
                                        .executes(context -> executeConsume(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                true
                                        )))
                                .then(CommandManager.literal("disable")
                                        .executes(context -> executeConsume(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                false
                                        )))))
                .then(CommandManager.literal("consumeOrder")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.literal(InventoryConsumeOrder.HOTBAR_FIRST)
                                        .executes(context -> executeConsumeOrder(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                InventoryConsumeOrder.HOTBAR_FIRST
                                        )))
                                .then(CommandManager.literal(InventoryConsumeOrder.MAIN_INVENTORY_FIRST)
                                        .executes(context -> executeConsumeOrder(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                InventoryConsumeOrder.MAIN_INVENTORY_FIRST
                                        )))))
                .then(CommandManager.literal("consumeCount")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(CommandManager.argument("count", IntegerArgumentType.integer(1, MAX_COUNT))
                                                .executes(context -> executeConsumeCount(
                                                        context.getSource(),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                        StringArgumentType.getString(context, "name"),
                                                        IntegerArgumentType.getInteger(context, "count")
                                                ))))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> addModeBranch(ContainerItemCountMode mode) {
        return CommandManager.literal(mode.id())
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1, MAX_COUNT))
                        .executes(context -> executeAddFromHand(
                                context.getSource(),
                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                StringArgumentType.getString(context, "name"),
                                mode,
                                IntegerArgumentType.getInteger(context, "count")
                        )));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> countModeBranch(ContainerItemCountMode mode) {
        return CommandManager.literal(mode.id())
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1, MAX_COUNT))
                        .executes(context -> executeCount(
                                context.getSource(),
                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                StringArgumentType.getString(context, "name"),
                                mode,
                                IntegerArgumentType.getInteger(context, "count")
                        )));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> optionBranch(String option) {
        return CommandManager.literal(option)
                .then(CommandManager.literal("enable")
                        .executes(context -> executeMatcherOption(
                                context.getSource(),
                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                StringArgumentType.getString(context, "name"),
                                option,
                                true
                        )))
                .then(CommandManager.literal("disable")
                        .executes(context -> executeMatcherOption(
                                context.getSource(),
                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                StringArgumentType.getString(context, "name"),
                                option,
                                false
                        )));
    }

    private static int executeEnabled(ServerCommandSource source, BlockPos pos, boolean enabled) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        if (enabled && enabledRequirements(device).isEmpty()) {
            sendError(source, Text.literal("物品提交还没有已启用的条件，请先添加并启用至少一个条件。"));
            return 0;
        }
        SignalDeviceData updated = SignalDeviceStore.updateVirtualItemSubmit(
                source.getWorld(),
                pos,
                enabled,
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                device.itemSubmitRequirements(),
                enabled ? "物品提交已启用" : "物品提交已禁用"
        );
        boolean closedSingleMatcher = false;
        if (enabled && updated.interactionItemMatcherEnabled()) {
            updated = SignalDeviceStore.updateVirtualInteractionItemMatcher(
                    source.getWorld(),
                    pos,
                    updated.interactionItemMatcher(),
                    false,
                    "已启用多物品提交，已自动关闭单物品 interactionItem 匹配"
            );
            closedSingleMatcher = true;
        }
        SignalDeviceData finalUpdated = updated;
        sendHeader(source, Text.literal(enabled ? "已启用物品提交" : "已禁用物品提交").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("物品提交启用", boolText(finalUpdated.itemSubmitEnabled())), false);
        if (closedSingleMatcher) {
            source.sendFeedback(() -> Text.literal("已自动关闭单物品 interactionItem 匹配；成功/失败反馈配置仍保留。").formatted(Formatting.YELLOW), false);
        }
        return 1;
    }

    private static int executeAddFromHand(ServerCommandSource source, BlockPos pos, String name, ContainerItemCountMode mode, int count) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        String cleanName = cleanName(name);
        if (cleanName.isBlank()) {
            sendError(source, Text.literal("提交条件名称不能为空。"));
            return 0;
        }
        if (findRequirement(device, cleanName) != null) {
            sendError(source, Text.literal("提交条件名称已存在：" + cleanName));
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

        ItemStackMatcherData matcher = ItemStackMatcherSupport.captureTemplate(stack, mode, count);
        int matchedCount = inventoryMatchedCount(player, matcher);
        boolean matched = matchesInventoryCount(matchedCount, matcher);
        ItemSubmitRequirementData requirement = new ItemSubmitRequirementData(
                java.util.UUID.randomUUID().toString(),
                cleanName,
                true,
                matcher,
                ContainerItemCountMode.IGNORE.id().equals(matcher.countMode()) ? 1 : Math.max(1, matcher.requiredCount()),
                matched,
                matchedCount,
                source.getWorld().getTime(),
                matched ? "当前满足" : "当前不满足"
        ).normalized();
        List<ItemSubmitRequirementData> requirements = new ArrayList<>(device.itemSubmitRequirements());
        requirements.add(requirement);
        SignalDeviceData updated = SignalDeviceStore.updateVirtualItemSubmit(
                source.getWorld(),
                pos,
                device.itemSubmitEnabled(),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                requirements,
                "已添加物品提交条件：" + cleanName
        );
        sendHeader(source, Text.literal("已添加物品提交条件").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("条件名称", Text.literal(cleanName).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("匹配模板", Text.literal(ItemStackMatcherSupport.summary(requirement.matcher())).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("当前是否满足", boolText(requirement.lastMatched())), false);
        source.sendFeedback(() -> field("条件数量", Text.literal(Integer.toString(updated.itemSubmitRequirements().size())).formatted(Formatting.LIGHT_PURPLE)), false);
        return 1;
    }

    private static int executeList(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        sendHeader(source, Text.literal("物品提交条件列表").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("物品提交启用", boolText(device.itemSubmitEnabled())), false);
        source.sendFeedback(() -> field("提交后消耗", boolText(device.itemSubmitConsumeEnabled())), false);
        source.sendFeedback(() -> field("消耗顺序", Text.literal(InventoryConsumeOrder.displayName(device.itemSubmitConsumeOrder())).formatted(Formatting.AQUA)), false);
        if (device.itemSubmitRequirements().isEmpty()) {
            source.sendFeedback(() -> Text.literal("暂无提交条件。").formatted(Formatting.YELLOW), false);
            return 1;
        }
        int index = 1;
        for (ItemSubmitRequirementData requirement : device.itemSubmitRequirements()) {
            ItemSubmitRequirementData data = requirement.normalized();
            int currentIndex = index++;
            source.sendFeedback(() -> Text.literal(currentIndex + ". " + data.name()
                    + " 启用=" + yesNo(data.enabled())
                    + " 模板=" + ItemStackMatcherSupport.summary(data.matcher())
                    + " 消耗数量=" + data.consumeCount()
                    + " 最近满足=" + yesNo(data.lastMatched())
                    + " 最近数量=" + data.lastMatchedCount()).formatted(Formatting.WHITE), false);
        }
        return 1;
    }

    private static int executeInfo(ServerCommandSource source, BlockPos pos, String name) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        ItemSubmitRequirementData requirement = device == null ? null : findRequirement(device, name);
        if (requirement == null) {
            sendError(source, Text.literal("未找到提交条件：" + name));
            return 0;
        }
        sendRequirementInfo(source, requirement.normalized());
        return 1;
    }

    private static int executeInfoAll(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        sendHeader(source, Text.literal("物品提交详情").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("当前匹配模式", Text.literal(device.itemSubmitEnabled()
                ? "多物品 itemSubmit"
                : "单物品 interactionItem").formatted(Formatting.AQUA)), false);
        if (device.itemSubmitEnabled()) {
            source.sendFeedback(() -> Text.literal("itemSubmit 是当前主要匹配条件，不再额外要求 interactionItem matcher。").formatted(Formatting.YELLOW), false);
        }
        source.sendFeedback(() -> field("物品提交启用", boolText(device.itemSubmitEnabled())), false);
        source.sendFeedback(() -> field("提交后消耗", boolText(device.itemSubmitConsumeEnabled())), false);
        source.sendFeedback(() -> field("消耗顺序", Text.literal(InventoryConsumeOrder.displayName(device.itemSubmitConsumeOrder())).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("条件数量", Text.literal(Integer.toString(device.itemSubmitRequirements().size())).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("最近提交满足", boolText(device.lastItemSubmitMatched())), false);
        source.sendFeedback(() -> field("最近失败原因", Text.literal(emptyText(device.lastItemSubmitFailureReason())).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("最近消耗摘要", Text.literal(emptyText(device.lastItemSubmitConsumedSummary())).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("最近结果", Text.literal(emptyText(device.lastItemSubmitResult())).formatted(Formatting.WHITE)), false);
        return 1;
    }

    private static int executeRemove(ServerCommandSource source, BlockPos pos, String name) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        String cleanName = cleanName(name);
        List<ItemSubmitRequirementData> requirements = new ArrayList<>();
        boolean removed = false;
        for (ItemSubmitRequirementData requirement : device.itemSubmitRequirements()) {
            if (requirement.normalized().name().equals(cleanName)) {
                removed = true;
            } else {
                requirements.add(requirement.normalized());
            }
        }
        if (!removed) {
            sendError(source, Text.literal("未找到提交条件：" + cleanName));
            return 0;
        }
        SignalDeviceData updated = SignalDeviceStore.updateVirtualItemSubmit(
                source.getWorld(),
                pos,
                device.itemSubmitEnabled() && !requirements.isEmpty(),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                requirements,
                "已删除物品提交条件：" + cleanName
        );
        sendHeader(source, Text.literal("已删除物品提交条件").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("剩余条件数量", Text.literal(Integer.toString(updated.itemSubmitRequirements().size())).formatted(Formatting.LIGHT_PURPLE)), false);
        return 1;
    }

    private static int executeClear(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        int count = device.itemSubmitRequirements().size();
        SignalDeviceStore.updateVirtualItemSubmit(
                source.getWorld(),
                pos,
                false,
                false,
                device.itemSubmitConsumeOrder(),
                List.of(),
                "已清空物品提交条件"
        );
        sendHeader(source, Text.literal("已清空物品提交条件").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("清理数量", Text.literal(Integer.toString(count)).formatted(Formatting.LIGHT_PURPLE)), false);
        return 1;
    }

    private static int executeRequirementEnabled(ServerCommandSource source, BlockPos pos, String name, boolean enabled) {
        return updateRequirement(source, pos, name, requirement ->
                requirement.withEnabled(enabled, false, 0, source.getWorld().getTime(), enabled ? "条件已启用" : "条件已禁用"),
                enabled ? "已启用物品提交条件" : "已禁用物品提交条件");
    }

    private static int executeMatcherFromHand(ServerCommandSource source, BlockPos pos, String name) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            sendError(source, Text.literal("该命令必须由玩家执行。"));
            return 0;
        }
        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            sendError(source, Text.literal("主手为空，无法捕获物品模板。"));
            return 0;
        }
        return updateRequirement(source, pos, name, requirement -> {
            ItemStackMatcherData oldMatcher = requirement.matcher();
            ItemStackMatcherData matcher = ItemStackMatcherSupport.captureTemplate(
                    stack,
                    ContainerItemCountMode.fromId(oldMatcher.countMode()),
                    oldMatcher.requiredCount()
            );
            return requirement.withMatcher(matcher, requirement.consumeCount())
                    .withResult(false, 0, source.getWorld().getTime(), "已从主手刷新模板");
        }, "已从主手刷新物品提交模板");
    }

    private static int executeMatcherOption(ServerCommandSource source, BlockPos pos, String name, String option, boolean enabled) {
        if (!ItemStackMatcherCommandSupport.isOption(option)) {
            sendError(source, Text.literal("未知匹配选项：" + option));
            return 0;
        }
        return updateRequirement(source, pos, name, requirement ->
                requirement.withMatcher(
                        ItemStackMatcherCommandSupport.withOption(requirement.matcher(), option, enabled),
                        requirement.consumeCount()
                ),
                "已更新物品提交匹配选项");
    }

    private static int executeCount(ServerCommandSource source, BlockPos pos, String name, ContainerItemCountMode mode, int count) {
        return updateRequirement(source, pos, name, requirement -> {
            ItemStackMatcherData matcher = ItemStackMatcherSupport.withCount(requirement.matcher(), mode, count);
            int consumeCount = ContainerItemCountMode.IGNORE.id().equals(matcher.countMode()) ? 1 : Math.max(1, matcher.requiredCount());
            return new ItemSubmitRequirementData(
                    requirement.id(),
                    requirement.name(),
                    requirement.enabled(),
                    matcher,
                    consumeCount,
                    false,
                    0,
                    0L,
                    "数量条件已更新"
            ).normalized();
        }, "已更新物品提交数量条件");
    }

    private static int executeConsume(ServerCommandSource source, BlockPos pos, boolean enabled) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        if (enabled && enabledRequirements(device).isEmpty()) {
            sendError(source, Text.literal("物品提交还没有已启用的条件，请先添加并启用至少一个条件。"));
            return 0;
        }
        SignalDeviceData updated = SignalDeviceStore.updateVirtualItemSubmit(
                source.getWorld(),
                pos,
                device.itemSubmitEnabled(),
                enabled,
                device.itemSubmitConsumeOrder(),
                device.itemSubmitRequirements(),
                enabled ? "物品提交消耗已启用" : "物品提交消耗已禁用"
        );
        sendHeader(source, Text.literal(enabled ? "已启用物品提交消耗" : "已禁用物品提交消耗").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("提交后消耗", boolText(updated.itemSubmitConsumeEnabled())), false);
        return 1;
    }

    private static int executeConsumeOrder(ServerCommandSource source, BlockPos pos, String rawOrder) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        String order = InventoryConsumeOrder.normalize(rawOrder);
        SignalDeviceData updated = SignalDeviceStore.updateVirtualItemSubmit(
                source.getWorld(),
                pos,
                device.itemSubmitEnabled(),
                device.itemSubmitConsumeEnabled(),
                order,
                device.itemSubmitRequirements(),
                "物品提交消耗顺序已更新"
        );
        sendHeader(source, Text.literal("已更新物品提交消耗顺序").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("消耗顺序", Text.literal(InventoryConsumeOrder.displayName(updated.itemSubmitConsumeOrder())).formatted(Formatting.AQUA)), false);
        return 1;
    }

    private static int executeConsumeCount(ServerCommandSource source, BlockPos pos, String name, int count) {
        return updateRequirement(source, pos, name, requirement -> new ItemSubmitRequirementData(
                requirement.id(),
                requirement.name(),
                requirement.enabled(),
                requirement.matcher(),
                count,
                requirement.lastMatched(),
                requirement.lastMatchedCount(),
                requirement.lastCheckGameTime(),
                "消耗数量已更新"
        ).normalized(), "已更新物品提交消耗数量");
    }

    private static int updateRequirement(
            ServerCommandSource source,
            BlockPos pos,
            String name,
            java.util.function.Function<ItemSubmitRequirementData, ItemSubmitRequirementData> updater,
            String result
    ) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        String cleanName = cleanName(name);
        List<ItemSubmitRequirementData> requirements = new ArrayList<>();
        boolean found = false;
        ItemSubmitRequirementData updatedRequirement = null;
        for (ItemSubmitRequirementData requirement : device.itemSubmitRequirements()) {
            ItemSubmitRequirementData normalized = requirement.normalized();
            if (normalized.name().equals(cleanName)) {
                found = true;
                updatedRequirement = updater.apply(normalized).normalized();
                requirements.add(updatedRequirement);
            } else {
                requirements.add(normalized);
            }
        }
        if (!found) {
            sendError(source, Text.literal("未找到提交条件：" + cleanName));
            return 0;
        }
        SignalDeviceStore.updateVirtualItemSubmit(
                source.getWorld(),
                pos,
                device.itemSubmitEnabled(),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                requirements,
                result
        );
        sendHeader(source, Text.literal(result).formatted(Formatting.GREEN));
        if (updatedRequirement != null) {
            sendRequirementInfo(source, updatedRequirement);
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

    private static ItemSubmitRequirementData findRequirement(SignalDeviceData device, String name) {
        String cleanName = cleanName(name);
        for (ItemSubmitRequirementData requirement : device.itemSubmitRequirements()) {
            ItemSubmitRequirementData normalized = requirement.normalized();
            if (normalized.name().equals(cleanName)) {
                return normalized;
            }
        }
        return null;
    }

    private static List<ItemSubmitRequirementData> enabledRequirements(SignalDeviceData device) {
        List<ItemSubmitRequirementData> requirements = new ArrayList<>();
        for (ItemSubmitRequirementData requirement : device.itemSubmitRequirements()) {
            ItemSubmitRequirementData normalized = requirement.normalized();
            if (normalized.enabled()) {
                requirements.add(normalized);
            }
        }
        return requirements;
    }

    private static int inventoryMatchedCount(ServerPlayerEntity player, ItemStackMatcherData matcher) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            if (ItemStackMatcher.matchesIgnoringCount(stack, matcher)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean matchesInventoryCount(int total, ItemStackMatcherData matcher) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        ContainerItemCountMode mode = ContainerItemCountMode.fromId(data.countMode());
        if (mode == ContainerItemCountMode.IGNORE) {
            return total > 0;
        }
        if (mode == ContainerItemCountMode.AT_MOST && total <= 0) {
            return false;
        }
        return mode.matches(total, data.requiredCount());
    }

    private static String cleanName(String name) {
        return name == null ? "" : name.trim();
    }

    private static void sendRequirementInfo(ServerCommandSource source, ItemSubmitRequirementData requirement) {
        sendHeader(source, Text.literal("物品提交条件详情").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("条件名称", Text.literal(requirement.name()).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("启用", boolText(requirement.enabled())), false);
        source.sendFeedback(() -> field("匹配模板", Text.literal(ItemStackMatcherSupport.summary(requirement.matcher())).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("消耗数量", Text.literal(Integer.toString(requirement.consumeCount())).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("最近满足", boolText(requirement.lastMatched())), false);
        source.sendFeedback(() -> field("最近匹配数量", Text.literal(Integer.toString(requirement.lastMatchedCount())).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("最近结果", Text.literal(emptyText(requirement.lastResult())).formatted(Formatting.WHITE)), false);
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

    private static MutableText posText(BlockPos pos) {
        return Text.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ()).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText boolText(boolean value) {
        return Text.literal(value ? "是" : "否").formatted(value ? Formatting.GREEN : Formatting.GRAY);
    }

    private static String emptyText(String value) {
        return value == null || value.isBlank() ? "暂无" : value;
    }

    private static String yesNo(boolean value) {
        return value ? "是" : "否";
    }
}
