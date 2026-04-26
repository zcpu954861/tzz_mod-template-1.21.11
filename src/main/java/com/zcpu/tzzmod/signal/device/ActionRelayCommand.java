package com.zcpu.tzzmod.signal.device;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.action.ActionValidator;
import com.zcpu.tzzmod.signal.SignalChannel;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class ActionRelayCommand {
    private ActionRelayCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("relay")
                .then(CommandManager.literal("bind")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("channel", StringArgumentType.string())
                                        .executes(context -> executeBind(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "channel")
                                        )))))
                .then(CommandManager.literal("addAction")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.literal("command")
                                        .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                                .executes(context -> executeAddAction(
                                                        context.getSource(),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                        ActionConfig.command(StringArgumentType.getString(context, "command"), false)
                                                ))))
                                .then(CommandManager.literal("message")
                                        .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                                .executes(context -> executeAddAction(
                                                        context.getSource(),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                        new ActionConfig(ActionType.MESSAGE, StringArgumentType.getString(context, "message"), true, false, 0, false)
                                                ))))
                                .then(CommandManager.literal("sound")
                                        .then(CommandManager.argument("sound", StringArgumentType.greedyString())
                                                .executes(context -> executeAddAction(
                                                        context.getSource(),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                        new ActionConfig(ActionType.SOUND, StringArgumentType.getString(context, "sound"), true, false, 0, false)
                                                ))))
                                .then(CommandManager.literal("signal")
                                        .then(CommandManager.argument("channel", StringArgumentType.string())
                                                .executes(context -> executeAddSignalAction(
                                                        context.getSource(),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                        StringArgumentType.getString(context, "channel")
                                                ))))))
                .then(CommandManager.literal("listActions")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeListActions(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("removeAction")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                        .executes(context -> executeRemoveAction(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                IntegerArgumentType.getInteger(context, "index")
                                        )))))
                .then(CommandManager.literal("clearActions")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeClearActions(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("cooldown")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(
                                                ActionRelayBlockEntity.MIN_COOLDOWN_TICKS,
                                                ActionRelayBlockEntity.MAX_COOLDOWN_TICKS
                                        ))
                                        .executes(context -> executeCooldown(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                IntegerArgumentType.getInteger(context, "ticks")
                                        )))))
                .then(CommandManager.literal("trigger")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeTrigger(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(context -> executeInfo(
                                        context.getSource(),
                                        BlockPosArgumentType.getLoadedBlockPos(context, "pos")
                                ))));
    }

    private static int executeBind(ServerCommandSource source, BlockPos pos, String rawChannel) {
        ActionRelayBlockEntity relay = getRelay(source, pos);
        if (relay == null) {
            return 0;
        }

        String channel = SignalChannel.normalize(rawChannel);
        if (!SignalChannel.isValid(channel)) {
            sendError(source, SignalChannel.validationError(rawChannel));
            return 0;
        }

        relay.setChannel(channel);
        SignalDeviceStore.updateChannel(source.getWorld(), pos, relay);
        sendHeader(source, Text.literal("已绑定动作继电器频道").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("频道", channelText(channel)), false);
        return 1;
    }

    private static int executeAddSignalAction(ServerCommandSource source, BlockPos pos, String rawChannel) {
        String channel = SignalChannel.normalize(rawChannel);
        if (!SignalChannel.isValid(channel)) {
            sendError(source, SignalChannel.validationError(rawChannel));
            return 0;
        }
        return executeAddAction(source, pos, ActionConfig.signal(channel, false));
    }

    private static int executeAddAction(ServerCommandSource source, BlockPos pos, ActionConfig action) {
        ActionRelayBlockEntity relay = getRelay(source, pos);
        if (relay == null) {
            return 0;
        }

        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
        if (player == null) {
            sendError(source, Text.literal("该命令需要玩家执行。"));
            return 0;
        }

        Text validationError = ActionValidator.validateForSave(player, action);
        if (validationError != null) {
            sendError(source, Text.literal("动作配置无效：").append(validationError));
            return 0;
        }

        relay.addAction(action);
        SignalDeviceStore.updateActions(source.getWorld(), pos, relay);
        sendHeader(source, Text.literal("已添加动作继电器动作").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("动作", actionSummaryText(action)), false);
        source.sendFeedback(() -> field("动作数量", number(relay.actions().size())), false);
        return 1;
    }

    private static int executeListActions(ServerCommandSource source, BlockPos pos) {
        ActionRelayBlockEntity relay = getRelay(source, pos);
        if (relay == null) {
            return 0;
        }

        sendHeader(source, Text.literal("动作继电器动作列表").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        if (relay.actions().isEmpty()) {
            source.sendFeedback(() -> warning("该动作继电器没有配置动作。"), false);
            return 1;
        }

        for (int index = 0; index < relay.actions().size(); index++) {
            ActionConfig action = relay.actions().get(index);
            int displayIndex = index + 1;
            source.sendFeedback(() -> Text.literal(displayIndex + ". ").formatted(Formatting.GRAY)
                    .append(actionSummaryText(action)), false);
        }
        return 1;
    }

    private static int executeRemoveAction(ServerCommandSource source, BlockPos pos, int oneBasedIndex) {
        ActionRelayBlockEntity relay = getRelay(source, pos);
        if (relay == null) {
            return 0;
        }

        if (!relay.removeAction(oneBasedIndex - 1)) {
            sendError(source, Text.literal("动作序号无效：" + oneBasedIndex));
            return 0;
        }

        SignalDeviceStore.updateActions(source.getWorld(), pos, relay);
        sendHeader(source, Text.literal("已删除动作继电器动作").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("删除序号", number(oneBasedIndex)), false);
        source.sendFeedback(() -> field("剩余动作", number(relay.actions().size())), false);
        return 1;
    }

    private static int executeClearActions(ServerCommandSource source, BlockPos pos) {
        ActionRelayBlockEntity relay = getRelay(source, pos);
        if (relay == null) {
            return 0;
        }

        int oldCount = relay.actions().size();
        relay.clearActions();
        SignalDeviceStore.updateActions(source.getWorld(), pos, relay);
        sendHeader(source, Text.literal("已清空动作继电器动作").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("清空数量", number(oldCount)), false);
        return 1;
    }

    private static int executeCooldown(ServerCommandSource source, BlockPos pos, int ticks) {
        ActionRelayBlockEntity relay = getRelay(source, pos);
        if (relay == null) {
            return 0;
        }

        relay.setCooldownTicks(ticks);
        SignalDeviceStore.updateCooldown(source.getWorld(), pos, relay);
        sendHeader(source, Text.literal("已设置动作继电器冷却").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("冷却", gtText(relay.cooldownTicks())), false);
        return 1;
    }

    private static int executeTrigger(ServerCommandSource source, BlockPos pos) {
        ActionRelayBlockEntity relay = getRelay(source, pos);
        if (relay == null) {
            return 0;
        }

        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
        ActionExecutionResult result = relay.executeRelayActions(source.getWorld(), player, true);
        SignalDeviceStore.updateActions(source.getWorld(), pos, relay);
        if (!result.success()) {
            sendError(source, result.message());
            return 0;
        }

        sendHeader(source, Text.literal("已手动触发动作继电器").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("说明", Text.literal("手动触发已绕过冷却检查。").formatted(Formatting.YELLOW)), false);
        source.sendFeedback(() -> field("结果", result.message()), false);
        return 1;
    }

    private static int executeInfo(ServerCommandSource source, BlockPos pos) {
        ActionRelayBlockEntity relay = getRelay(source, pos);
        if (relay == null) {
            return 0;
        }

        SignalDeviceStore.upsertActionRelay(source.getWorld(), pos, relay);
        sendHeader(source, Text.literal("动作继电器").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("频道", relay.channel().isBlank()
                ? Text.literal("未绑定").formatted(Formatting.YELLOW)
                : channelText(relay.channel())), false);
        source.sendFeedback(() -> field("状态", Text.literal(relay.enabled() ? "启用" : "禁用")
                .formatted(relay.enabled() ? Formatting.GREEN : Formatting.RED)), false);
        source.sendFeedback(() -> field("冷却", gtText(relay.cooldownTicks())), false);
        source.sendFeedback(() -> field("动作数量", number(relay.actions().size())), false);
        source.sendFeedback(() -> field("当前冷却剩余", gtText((int) relay.remainingCooldownTicks(source.getWorld().getTime()))), false);
        source.sendFeedback(() -> field("最近执行", relay.lastRunWallTimeMillis() <= 0
                ? Text.literal("尚未执行").formatted(Formatting.YELLOW)
                : Text.literal(relay.lastResult()).formatted(Formatting.WHITE)), false);
        return 1;
    }

    private static ActionRelayBlockEntity getRelay(ServerCommandSource source, BlockPos pos) {
        if (source.getWorld().getBlockEntity(pos) instanceof ActionRelayBlockEntity relay) {
            return relay;
        }

        sendError(source, Text.literal("该位置不是动作继电器。"));
        return null;
    }

    private static void sendHeader(ServerCommandSource source, Text title) {
        source.sendFeedback(() -> Text.literal("===========").formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> title, false);
    }

    private static void sendError(ServerCommandSource source, Text message) {
        source.sendFeedback(() -> Text.literal("===========").formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> Text.literal("错误：").formatted(Formatting.RED)
                .append(message.copy().formatted(Formatting.RED)), false);
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

    private static MutableText channelText(String channel) {
        return Text.literal(SignalChannel.normalize(channel)).formatted(Formatting.AQUA);
    }

    private static MutableText number(int value) {
        return Text.literal(Integer.toString(value)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText gtText(int ticks) {
        return Text.literal(ticks + " GT").formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText actionSummaryText(ActionConfig action) {
        if (action == null || action.type() == null) {
            return Text.literal("未知动作").formatted(Formatting.YELLOW);
        }
        String value = action.value() == null ? "" : action.value();
        return switch (action.type()) {
            case COMMAND -> Text.literal("命令 ").formatted(Formatting.GREEN)
                    .append(Text.literal(value).formatted(Formatting.GREEN));
            case MESSAGE -> Text.literal("消息 ").formatted(Formatting.WHITE)
                    .append(Text.literal(value).formatted(Formatting.WHITE));
            case SOUND -> Text.literal("音效 ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal(value).formatted(Formatting.LIGHT_PURPLE));
            case SIGNAL -> Text.literal("信号 ").formatted(Formatting.AQUA)
                    .append(channelText(value));
        };
    }
}
