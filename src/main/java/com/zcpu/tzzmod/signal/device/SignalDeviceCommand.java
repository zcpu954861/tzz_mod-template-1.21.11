package com.zcpu.tzzmod.signal.device;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zcpu.tzzmod.ModBlock.entity.SignalEmitterBlockEntity;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.signal.SignalChannel;
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
    private SignalDeviceCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("device")
                .then(CommandManager.literal("bind")
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("channel", StringArgumentType.string())
                                        .executes(context -> executeBind(
                                                context.getSource(),
                                                BlockPosArgumentType.getLoadedBlockPos(context, "pos"),
                                                StringArgumentType.getString(context, "channel")
                                        )))))
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

    private static int executeBind(ServerCommandSource source, BlockPos pos, String rawChannel) {
        SignalEmitterBlockEntity blockEntity = getEmitter(source, pos);
        if (blockEntity == null) {
            return 0;
        }

        String channel = SignalChannel.normalize(rawChannel);
        if (!SignalChannel.isValid(channel)) {
            sendError(source, SignalChannel.validationError(rawChannel));
            return 0;
        }

        blockEntity.setChannel(channel);
        sendHeader(source, Text.literal("已绑定信号发射器频道").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", Text.literal(positionText(pos)).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("频道", Text.literal(channel).formatted(Formatting.AQUA)), false);
        return 1;
    }

    private static int executeInfo(ServerCommandSource source, BlockPos pos) {
        SignalEmitterBlockEntity blockEntity = getEmitter(source, pos);
        if (blockEntity == null) {
            return 0;
        }

        sendStatus(source, pos, blockEntity);
        return 1;
    }

    private static int executeTest(ServerCommandSource source, BlockPos pos) {
        SignalEmitterBlockEntity blockEntity = getEmitter(source, pos);
        if (blockEntity == null) {
            return 0;
        }

        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
        ActionExecutionResult result = blockEntity.emitSignal(source.getWorld(), player);
        if (!result.success()) {
            sendError(source, result.message());
            return 0;
        }

        sendHeader(source, Text.literal("已测试信号发射器").formatted(Formatting.GREEN));
        source.sendFeedback(() -> field("位置", Text.literal(positionText(pos)).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("频道", Text.literal(blockEntity.channel()).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("结果", result.message()), false);
        return 1;
    }

    private static int executeSetEnabled(ServerCommandSource source, BlockPos pos, boolean enabled) {
        SignalEmitterBlockEntity blockEntity = getEmitter(source, pos);
        if (blockEntity == null) {
            return 0;
        }

        blockEntity.setEnabled(enabled);
        sendHeader(source, Text.literal(enabled ? "已启用信号发射器" : "已禁用信号发射器")
                .formatted(enabled ? Formatting.GREEN : Formatting.RED));
        source.sendFeedback(() -> field("位置", Text.literal(positionText(pos)).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("状态", Text.literal(enabled ? "启用" : "禁用")
                .formatted(enabled ? Formatting.GREEN : Formatting.RED)), false);
        return 1;
    }

    private static SignalEmitterBlockEntity getEmitter(ServerCommandSource source, BlockPos pos) {
        if (source.getWorld().getBlockEntity(pos) instanceof SignalEmitterBlockEntity blockEntity) {
            return blockEntity;
        }

        sendError(source, Text.literal("该位置不是信号发射器。"));
        return null;
    }

    private static void sendStatus(ServerCommandSource source, BlockPos pos, SignalEmitterBlockEntity blockEntity) {
        sendHeader(source, Text.literal("信号发射器").formatted(Formatting.GOLD));
        source.sendFeedback(() -> field("位置", Text.literal(positionText(pos)).formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> field("频道", blockEntity.channel().isBlank()
                ? Text.literal("未绑定").formatted(Formatting.YELLOW)
                : Text.literal(blockEntity.channel()).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> field("状态", Text.literal(blockEntity.enabled() ? "启用" : "禁用")
                .formatted(blockEntity.enabled() ? Formatting.GREEN : Formatting.RED)), false);
        boolean powered = source.getWorld().isReceivingRedstonePower(pos);
        source.sendFeedback(() -> field("红石", Text.literal(powered ? "已通电" : "未通电")
                .formatted(powered ? Formatting.GREEN : Formatting.GRAY)), false);
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

    private static String positionText(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
