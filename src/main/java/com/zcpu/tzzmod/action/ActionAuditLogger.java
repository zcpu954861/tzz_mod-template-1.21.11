package com.zcpu.tzzmod.action;

import com.zcpu.tzzmod.Tzz_mod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ActionAuditLogger {
    private ActionAuditLogger() {
    }

    public static void log(ActionContext context, ActionConfig config, ActionExecutionResult result) {
        String playerName = context == null || context.player() == null
                ? "unknown"
                : context.player().getName().getString();

        String sourceType = context == null || context.sourceType() == null
                ? "unknown"
                : context.sourceType().id();

        String actionType = config == null || config.type() == null
                ? "unknown"
                : config.type().id();

        String value = config == null
                ? ""
                : config.value();

        if (result != null && result.success()) {
            Tzz_mod.LOGGER.info(
                    "[ActionEngine] player={} source={} action={} value={}",
                    playerName,
                    sourceType,
                    actionType,
                    value
            );
        } else {
            String reason = result == null || result.message() == null
                    ? "unknown"
                    : result.message().getString();

            Tzz_mod.LOGGER.warn(
                    "[ActionEngine] failed player={} source={} action={} value={} reason={}",
                    playerName,
                    sourceType,
                    actionType,
                    value,
                    reason
            );
        }
    }

    public static void notifyOperators(ActionContext context, ActionConfig config, ActionExecutionResult result) {
        if (context == null || context.player() == null || config == null || result == null) {
            return;
        }

        MutableText message = Text.literal("[ActionEngine] ").formatted(Formatting.GOLD)
                .append(Text.literal(context.player().getName().getString()).formatted(Formatting.YELLOW))
                .append(Text.literal(" 触发 ").formatted(Formatting.GOLD))
                .append(Text.literal(context.sourceType() == null ? "unknown" : context.sourceType().id()).formatted(Formatting.AQUA))
                .append(Text.literal(" -> ").formatted(Formatting.GOLD))
                .append(Text.literal(config.type() == null ? "unknown" : config.type().id()).formatted(Formatting.GREEN))
                .append(Text.literal(": ").formatted(Formatting.GRAY))
                .append(Text.literal(config.value() == null ? "" : config.value()).formatted(Formatting.WHITE));

        if (!result.success()) {
            message.append(Text.literal(" 执行失败: ").formatted(Formatting.RED))
                    .append(Text.literal(result.message() == null ? "unknown" : result.message().getString()).formatted(Formatting.RED));
        }

        for (ServerPlayerEntity onlinePlayer : context.player()
                .getCommandSource()
                .getServer()
                .getPlayerManager()
                .getPlayerList()) {
            if (onlinePlayer.isCreativeLevelTwoOp()) {
                onlinePlayer.sendMessage(message, false);
            }
        }
    }
}
