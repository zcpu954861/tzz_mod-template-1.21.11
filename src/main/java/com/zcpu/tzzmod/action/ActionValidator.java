package com.zcpu.tzzmod.action;

import com.mojang.brigadier.ParseResults;
import com.zcpu.tzzmod.condition.state.StateVariableMutationValidation;
import com.zcpu.tzzmod.signal.SignalChannel;
import java.util.List;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class ActionValidator {
    private ActionValidator() {
    }

    public static Text validateForSave(ServerPlayerEntity player, ActionConfig config) {
        Text commonError = validateCommon(player, config, true);
        if (commonError != null) {
            return commonError;
        }

        if (config.type() == ActionType.COMMAND && !isCommandValid(player, config.value())) {
            return Text.literal("命令无效");
        }

        if (config.type() == ActionType.SIGNAL && !SignalChannel.isValid(config.value())) {
            return SignalChannel.validationError(config.value());
        }

        if (config.type() == ActionType.STATE_VARIABLE) {
            Text error = validateStateAction(config, "");
            if (error != null) {
                return error;
            }
        }

        return null;
    }

    public static Text validateForExecute(ActionContext context, ActionConfig config) {
        if (context == null) {
            return Text.literal("动作上下文为空");
        }

        if (context.world() == null || context.position() == null) {
            return Text.literal("动作上下文缺少世界或位置");
        }

        Text common = validateCommon(context.player(), config, false);
        if (common != null) {
            return common;
        }
        if (config.type() == ActionType.STATE_VARIABLE) {
            String contextPlayerId = context.player() == null ? "" : context.player().getUuidAsString();
            return validateStateAction(config, contextPlayerId);
        }
        return null;
    }

    private static Text validateCommon(ServerPlayerEntity player, ActionConfig config, boolean requirePlayer) {
        if (requirePlayer && player == null) {
            return Text.literal("玩家为空");
        }

        if (config == null || !config.isUsable()) {
            return Text.literal("动作配置为空或未启用");
        }

        if (config.requiresOp() && player != null && !player.isCreativeLevelTwoOp()) {
            return Text.literal("该动作需要 OP 权限");
        }

        return null;
    }

    public static boolean isCommandValid(ServerPlayerEntity player, String command) {
        try {
            String normalized = ActionConfig.normalizeCommand(command);

            ServerCommandSource source = player.getCommandSource()
                    .withPermissions(PermissionPredicate.ALL)
                    .withSilent();

            ParseResults<ServerCommandSource> parseResults = source.getServer()
                    .getCommandManager()
                    .getDispatcher()
                    .parse(normalized, source);

            return parseResults.getReader().getRemaining().isEmpty()
                    && parseResults.getExceptions().isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Text validateStateAction(ActionConfig config, String contextPlayerId) {
        List<StateVariableMutationValidation.Issue> issues = StateVariableMutationValidation.validate(
                config == null ? null : config.stateMutationRequest(contextPlayerId)
        );
        if (!issues.isEmpty()) {
            String message = issues.stream()
                    .map(StateVariableMutationValidation.Issue::message)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse("状态变量动作配置无效。");
            return Text.literal(message);
        }
        return null;
    }
}
