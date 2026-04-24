package com.zcpu.tzzmod.action;

import com.mojang.brigadier.ParseResults;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class ActionValidator {
    private ActionValidator() {
    }

    public static Text validateForSave(ServerPlayerEntity player, ActionConfig config) {
        Text commonError = validateCommon(player, config);
        if (commonError != null) {
            return commonError;
        }

        if (config.type() == ActionType.COMMAND && !isCommandValid(player, config.value())) {
            return Text.literal("命令无效");
        }

        return null;
    }

    public static Text validateForExecute(ActionContext context, ActionConfig config) {
        if (context == null) {
            return Text.literal("动作上下文为空");
        }

        return validateCommon(context.player(), config);
    }

    private static Text validateCommon(ServerPlayerEntity player, ActionConfig config) {
        if (player == null) {
            return Text.literal("玩家为空");
        }

        if (config == null || !config.isUsable()) {
            return Text.literal("动作配置为空或未启用");
        }

        if (config.requiresOp() && !player.isCreativeLevelTwoOp()) {
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
}
