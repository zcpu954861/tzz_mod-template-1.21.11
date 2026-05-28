package com.zcpu.tzzmod.signal.device;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zcpu.tzzmod.ModBlock.entity.SignalReceiverBlockEntity;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionSessions;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class SignalReceiverCommand {
    private SignalReceiverCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("receiver")
                .then(CommandManager.literal("pulse")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(
                                                SignalReceiverBlockEntity.MIN_PULSE_TICKS,
                                                SignalReceiverBlockEntity.MAX_PULSE_TICKS
                                        ))
                                        .executes(context -> executePulse(
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

    private static int executePulse(ServerCommandSource source, BlockPos pos, int ticks) {
        if (WebAdminSelectionSessions.shouldBlockProtectedDraftCommandMutation(source, pos, "修改脉冲")) {
            return 0;
        }
        SignalReceiverBlockEntity receiver = getReceiver(source, pos);
        if (receiver == null) {
            return 0;
        }

        receiver.setPulseTicks(ticks);
        SignalDeviceStore.updatePulse(source.getWorld(), pos, receiver);
        sendHeader(source, Text.literal("已设置接收器脉冲时长").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("脉冲时长", Text.literal(receiver.pulseTicks() + " GT").formatted(Formatting.LIGHT_PURPLE)), false);
        return 1;
    }

    private static int executeTrigger(ServerCommandSource source, BlockPos pos) {
        if (WebAdminSelectionSessions.shouldBlockProtectedDraftCommandMutation(source, pos, "手动触发")) {
            return 0;
        }
        SignalReceiverBlockEntity receiver = getReceiver(source, pos);
        if (receiver == null) {
            return 0;
        }

        ActionExecutionResult result = receiver.receiveSignal(source.getWorld());
        if (!result.success()) {
            sendError(source, result.message());
            return 0;
        }

        sendHeader(source, Text.literal("已手动触发信号接收器").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("频道", receiver.channel().isBlank()
                ? Text.literal("未绑定").formatted(Formatting.YELLOW)
                : Text.literal(receiver.channel()).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("结果", result.message()), false);
        return 1;
    }

    private static int executeInfo(ServerCommandSource source, BlockPos pos) {
        SignalReceiverBlockEntity receiver = getReceiver(source, pos);
        if (receiver == null) {
            return 0;
        }

        SignalDeviceStore.upsertReceiver(source.getWorld(), pos, receiver);
        sendHeader(source, Text.literal("信号接收器").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("位置", posText(pos)), false);
        source.sendFeedback(() -> field("频道", receiver.channel().isBlank()
                ? Text.literal("未绑定").formatted(Formatting.YELLOW)
                : Text.literal(receiver.channel()).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("状态", Text.literal(receiver.enabled() ? "启用" : "禁用")
                .formatted(receiver.enabled() ? Formatting.GREEN : Formatting.RED)), false);
        source.sendFeedback(() -> field("脉冲时长", Text.literal(receiver.pulseTicks() + " GT").formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("剩余 ticks", Text.literal(Integer.toString(receiver.remainingPulseTicks()))
                .formatted(Formatting.LIGHT_PURPLE)), false);
        return 1;
    }

    private static SignalReceiverBlockEntity getReceiver(ServerCommandSource source, BlockPos pos) {
        if (source.getWorld().getBlockEntity(pos) instanceof SignalReceiverBlockEntity receiver) {
            return receiver;
        }

        sendError(source, Text.literal("该位置不是信号接收器。"));
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

    private static MutableText posText(BlockPos pos) {
        return Text.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ()).formatted(Formatting.LIGHT_PURPLE);
    }
}
