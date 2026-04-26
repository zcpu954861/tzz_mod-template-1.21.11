package com.zcpu.tzzmod.signal.device;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.device.item.InteractionItemSource;
import com.zcpu.tzzmod.signal.device.item.InteractionItemVanillaPolicy;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherCommandSupport;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherSupport;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
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
                .then(CommandManager.literal("successChannel")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("channel", StringArgumentType.string())
                                        .executes(context -> executeChannel(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "channel"),
                                                true
                                        )))))
                .then(CommandManager.literal("clearSuccessChannel")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeChannel(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                        "",
                                        true
                                ))))
                .then(CommandManager.literal("failChannel")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("channel", StringArgumentType.string())
                                        .executes(context -> executeChannel(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "channel"),
                                                false
                                        )))))
                .then(CommandManager.literal("clearFailChannel")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeChannel(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                        "",
                                        false
                                ))))
                .then(CommandManager.literal("successMessage")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                        .executes(context -> executeMessage(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "message"),
                                                true
                                        )))))
                .then(CommandManager.literal("clearSuccessMessage")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeMessage(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                        "",
                                        true
                                ))))
                .then(CommandManager.literal("failMessage")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                        .executes(context -> executeMessage(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "message"),
                                                false
                                        )))))
                .then(CommandManager.literal("clearFailMessage")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeMessage(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                        "",
                                        false
                                ))))
                .then(CommandManager.literal("successSound")
                        .then(soundBranch(true)))
                .then(CommandManager.literal("clearSuccessSound")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeSound(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                        "",
                                        1.0F,
                                        1.0F,
                                        true
                                ))))
                .then(CommandManager.literal("failSound")
                        .then(soundBranch(false)))
                .then(CommandManager.literal("clearFailSound")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeSound(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                        "",
                                        1.0F,
                                        1.0F,
                                        false
                                ))))
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
                .then(CommandManager.literal("consumeCount")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("count", IntegerArgumentType.integer(1, MAX_COUNT))
                                        .executes(context -> executeConsumeCount(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                IntegerArgumentType.getInteger(context, "count")
                                        )))))
                .then(CommandManager.literal("source")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.literal(InteractionItemSource.MAIN_HAND)
                                        .executes(context -> executeSource(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                InteractionItemSource.MAIN_HAND
                                        )))
                                .then(CommandManager.literal(InteractionItemSource.OFF_HAND)
                                        .executes(context -> executeSource(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                InteractionItemSource.OFF_HAND
                                        )))
                                .then(CommandManager.literal(InteractionItemSource.INVENTORY_CONTAINS)
                                        .executes(context -> executeSource(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                InteractionItemSource.INVENTORY_CONTAINS
                                        )))))
                .then(CommandManager.literal("vanillaInteraction")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.literal(InteractionItemVanillaPolicy.ALLOW)
                                        .executes(context -> executeVanillaPolicy(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                InteractionItemVanillaPolicy.ALLOW
                                        )))
                                .then(CommandManager.literal(InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH)
                                        .executes(context -> executeVanillaPolicy(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH
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

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<ServerCommandSource, ?> soundBranch(boolean success) {
        return CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                .then(CommandManager.argument("soundId", IdentifierArgumentType.identifier())
                        .then(CommandManager.argument("volume", FloatArgumentType.floatArg(0.0F, 10.0F))
                                .then(CommandManager.argument("pitch", FloatArgumentType.floatArg(0.0F, 2.0F))
                                        .executes(context -> executeSound(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                IdentifierArgumentType.getIdentifier(context, "soundId").toString(),
                                                FloatArgumentType.getFloat(context, "volume"),
                                                FloatArgumentType.getFloat(context, "pitch"),
                                                success
                                        )))));
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
        source.sendFeedback(() -> field("数量要求", Text.literal(ItemStackMatcherSupport.countRequirementText(updated.interactionItemMatcher())).formatted(Formatting.LIGHT_PURPLE)), false);
        return 1;
    }

    private static int executeChannel(ServerCommandSource source, BlockPos pos, String rawChannel, boolean success) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null || !hasMatcher(source, device)) {
            return 0;
        }
        String channel = SignalChannel.normalize(rawChannel);
        if (!channel.isBlank() && !SignalChannel.isValid(channel)) {
            sendError(source, SignalChannel.validationError(rawChannel));
            return 0;
        }

        ItemStackMatcherData matcher = success
                ? ItemStackMatcherSupport.withSuccessChannel(device.interactionItemMatcher(), channel)
                : ItemStackMatcherSupport.withFailChannel(device.interactionItemMatcher(), channel);
        SignalDeviceData updated = updateMatcher(source, pos, device, matcher, success ? "已更新成功频道" : "已更新失败频道");
        sendHeader(source, Text.literal(success ? "已更新匹配成功频道" : "已更新匹配失败频道").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field(success ? "成功频道" : "失败频道", channelOrEmpty(success
                ? updated.interactionItemMatcher().successChannel()
                : updated.interactionItemMatcher().failChannel())), false);
        return 1;
    }

    private static int executeMessage(ServerCommandSource source, BlockPos pos, String message, boolean success) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null || !hasMatcher(source, device)) {
            return 0;
        }

        ItemStackMatcherData matcher = success
                ? ItemStackMatcherSupport.withSuccessMessage(device.interactionItemMatcher(), message)
                : ItemStackMatcherSupport.withFailMessage(device.interactionItemMatcher(), message);
        SignalDeviceData updated = updateMatcher(source, pos, device, matcher, success ? "已更新成功消息" : "已更新失败消息");
        sendHeader(source, Text.literal(success ? "已更新匹配成功消息" : "已更新匹配失败消息").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field(success ? "成功消息" : "失败消息", configuredText(success
                ? updated.interactionItemMatcher().successMessage()
                : updated.interactionItemMatcher().failMessage())), false);
        return 1;
    }

    private static int executeSound(ServerCommandSource source, BlockPos pos, String soundId, float volume, float pitch, boolean success) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null || !hasMatcher(source, device)) {
            return 0;
        }
        String cleanSoundId = soundId == null ? "" : soundId.trim().toLowerCase();
        if (!cleanSoundId.isBlank()) {
            Identifier id = Identifier.tryParse(cleanSoundId);
            if (id == null || !Registries.SOUND_EVENT.containsId(id)) {
                sendError(source, Text.literal("音效 ID 无效或不存在：" + soundId));
                return 0;
            }
        }

        ItemStackMatcherData matcher = success
                ? ItemStackMatcherSupport.withSuccessSound(device.interactionItemMatcher(), cleanSoundId, volume, pitch)
                : ItemStackMatcherSupport.withFailSound(device.interactionItemMatcher(), cleanSoundId, volume, pitch);
        SignalDeviceData updated = updateMatcher(source, pos, device, matcher, success ? "已更新成功音效" : "已更新失败音效");
        ItemStackMatcherData updatedMatcher = updated.interactionItemMatcher();
        sendHeader(source, Text.literal(success ? "已更新匹配成功音效" : "已更新匹配失败音效").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field(success ? "成功音效" : "失败音效", soundText(success
                ? updatedMatcher.successSoundId()
                : updatedMatcher.failSoundId(), success
                ? updatedMatcher.successSoundVolume()
                : updatedMatcher.failSoundVolume(), success
                ? updatedMatcher.successSoundPitch()
                : updatedMatcher.failSoundPitch())), false);
        return 1;
    }

    private static int executeConsume(ServerCommandSource source, BlockPos pos, boolean enabled) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null || !hasMatcher(source, device)) {
            return 0;
        }
        if (enabled && !InteractionItemSource.supportsConsume(device.interactionItemMatcher().interactionItemSource())) {
            sendError(source, Text.literal("当前物品来源为 "
                    + device.interactionItemMatcher().interactionItemSource()
                    + "，5.12 MVP 不支持该来源消耗。请先切回 main_hand。"));
            return 0;
        }
        ItemStackMatcherData matcher = ItemStackMatcherSupport.withConsume(device.interactionItemMatcher(), enabled);
        SignalDeviceData updated = updateMatcher(source, pos, device, matcher, enabled ? "已启用成功消耗" : "已禁用成功消耗");
        sendHeader(source, Text.literal(enabled ? "已启用匹配成功消耗" : "已禁用匹配成功消耗").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("消耗", boolText(updated.interactionItemMatcher().consumeEnabled())), false);
        source.sendFeedback(() -> field("消耗数量", Text.literal(Integer.toString(updated.interactionItemMatcher().consumeCount())).formatted(Formatting.LIGHT_PURPLE)), false);
        return 1;
    }

    private static int executeSource(ServerCommandSource source, BlockPos pos, String rawSource) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        ItemStackMatcherData current = device.interactionItemMatcher() == null
                ? ItemStackMatcherData.empty()
                : device.interactionItemMatcher().normalized();
        String nextSource = InteractionItemSource.normalize(rawSource);
        if (!InteractionItemSource.supportsConsume(nextSource) && current.consumeEnabled()) {
            sendError(source, Text.literal("当前已启用成功消耗。切换到 "
                    + nextSource
                    + " 前，请先关闭 consume。"));
            return 0;
        }
        ItemStackMatcherData matcher = ItemStackMatcherSupport.withSource(current, nextSource);
        SignalDeviceData updated = updateMatcher(source, pos, device, matcher, "已更新交互物品来源");
        sendHeader(source, Text.literal("已更新交互物品来源").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("物品来源", Text.literal(InteractionItemSource.displayName(updated.interactionItemMatcher().interactionItemSource())).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("物品匹配启用", boolText(updated.interactionItemMatcherEnabled())), false);
        source.sendFeedback(() -> field("成功消耗", boolText(updated.interactionItemMatcher().consumeEnabled())), false);
        return 1;
    }

    private static int executeVanillaPolicy(ServerCommandSource source, BlockPos pos, String rawPolicy) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        ItemStackMatcherData current = device.interactionItemMatcher() == null
                ? ItemStackMatcherData.empty()
                : device.interactionItemMatcher().normalized();
        ItemStackMatcherData matcher = ItemStackMatcherSupport.withVanillaPolicy(current, rawPolicy);
        SignalDeviceData updated = updateMatcher(source, pos, device, matcher, "已更新原版交互策略");
        sendHeader(source, Text.literal("已更新原版交互策略").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("策略", Text.literal(InteractionItemVanillaPolicy.displayName(updated.interactionItemMatcher().interactionItemVanillaPolicy())).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("物品匹配启用", boolText(updated.interactionItemMatcherEnabled())), false);
        return 1;
    }

    private static int executeConsumeCount(ServerCommandSource source, BlockPos pos, int count) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null || !hasMatcher(source, device)) {
            return 0;
        }
        ItemStackMatcherData matcher = ItemStackMatcherSupport.withConsumeCount(device.interactionItemMatcher(), count);
        SignalDeviceData updated = updateMatcher(source, pos, device, matcher, "已更新成功消耗数量");
        sendHeader(source, Text.literal("已更新匹配成功消耗数量").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("消耗数量", Text.literal(Integer.toString(updated.interactionItemMatcher().consumeCount())).formatted(Formatting.LIGHT_PURPLE)), false);
        return 1;
    }

    private static int executeInfo(ServerCommandSource source, BlockPos pos) {
        SignalDeviceData device = getVirtualDevice(source, pos);
        if (device == null) {
            return 0;
        }
        sendHeader(source, Text.literal("交互物品匹配详情").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("启用", boolText(device.interactionItemMatcherEnabled())), false);
        source.sendFeedback(() -> field("模板", Text.literal(ItemStackMatcherSupport.summary(device.interactionItemMatcher())).formatted(Formatting.WHITE)), false);
        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        source.sendFeedback(() -> field("数量模式", Text.literal(matcher.countMode()).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("数量要求", Text.literal(ItemStackMatcherSupport.countRequirementText(matcher)).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("物品来源", Text.literal(InteractionItemSource.displayName(matcher.interactionItemSource())).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("原版交互策略", Text.literal(InteractionItemVanillaPolicy.displayName(matcher.interactionItemVanillaPolicy())).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("来源支持消耗", boolText(InteractionItemSource.supportsConsume(matcher.interactionItemSource()))), false);
        source.sendFeedback(() -> field("成功频道", channelOrEmpty(matcher.successChannel())), false);
        source.sendFeedback(() -> field("失败频道", channelOrEmpty(matcher.failChannel())), false);
        source.sendFeedback(() -> field("成功消息", configuredText(matcher.successMessage())), false);
        source.sendFeedback(() -> field("失败消息", configuredText(matcher.failMessage())), false);
        source.sendFeedback(() -> field("成功音效", soundText(matcher.successSoundId(), matcher.successSoundVolume(), matcher.successSoundPitch())), false);
        source.sendFeedback(() -> field("失败音效", soundText(matcher.failSoundId(), matcher.failSoundVolume(), matcher.failSoundPitch())), false);
        source.sendFeedback(() -> field("成功消耗", boolText(matcher.consumeEnabled())), false);
        source.sendFeedback(() -> field("消耗数量", Text.literal(Integer.toString(matcher.consumeCount())).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("最近匹配", boolText(device.lastInteractionItemMatched())), false);
        source.sendFeedback(() -> field("最近匹配来源", Text.literal(matcher.lastInteractionItemSource().isBlank() ? "暂无" : matcher.lastInteractionItemSource()).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("最近匹配槽位", Text.literal(Integer.toString(matcher.lastInteractionItemMatchedSlot())).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("最近匹配数量", Text.literal(Integer.toString(matcher.lastInteractionItemMatchedCount())).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("最近来源结果", Text.literal(matcher.lastInteractionItemSourceResult().isBlank() ? "暂无结果" : matcher.lastInteractionItemSourceResult()).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> field("最近结果", Text.literal(device.lastInteractionItemResult().isBlank() ? "暂无结果" : device.lastInteractionItemResult()).formatted(Formatting.WHITE)), false);
        if (device.interactChannel().isBlank() && matcher.successChannel().isBlank()) {
            source.sendFeedback(() -> warning("尚未设置 interactChannel 或成功频道。"), false);
        }
        if (matcher.failChannel().isBlank()) {
            source.sendFeedback(() -> warning("失败频道未设置，失败时不会 emit signal。"), false);
        }
        if (matcher.consumeEnabled() && !InteractionItemSource.supportsConsume(matcher.interactionItemSource())) {
            source.sendFeedback(() -> warning("当前来源不支持 consume，请关闭 consume 或切回 main_hand。"), false);
        }
        return 1;
    }

    private static boolean hasMatcher(ServerCommandSource source, SignalDeviceData device) {
        if (device.interactionItemMatcher() == null || !device.interactionItemMatcher().normalized().enabled()) {
            sendError(source, Text.literal("还没有交互物品模板，请先使用 setFromHand。"));
            return false;
        }
        return true;
    }

    private static SignalDeviceData updateMatcher(
            ServerCommandSource source,
            BlockPos pos,
            SignalDeviceData device,
            ItemStackMatcherData matcher,
            String result
    ) {
        return SignalDeviceStore.updateVirtualInteractionItemMatcher(
                source.getWorld(),
                pos,
                matcher,
                device.interactionItemMatcherEnabled(),
                result
        );
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

    private static MutableText channelOrEmpty(String channel) {
        if (channel == null || channel.isBlank()) {
            return Text.literal("未设置").formatted(Formatting.YELLOW);
        }
        return Text.literal(channel).formatted(Formatting.AQUA);
    }

    private static MutableText configuredText(String value) {
        if (value == null || value.isBlank()) {
            return Text.literal("未设置").formatted(Formatting.YELLOW);
        }
        return Text.literal(value).formatted(Formatting.WHITE);
    }

    private static MutableText soundText(String soundId, float volume, float pitch) {
        if (soundId == null || soundId.isBlank()) {
            return Text.literal("未设置").formatted(Formatting.YELLOW);
        }
        return Text.literal(soundId + " / volume " + volume + " / pitch " + pitch).formatted(Formatting.AQUA);
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
