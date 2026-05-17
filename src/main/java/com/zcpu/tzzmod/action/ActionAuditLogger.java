package com.zcpu.tzzmod.action;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
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
                : actionValueForLog(config);

        if (result != null && result.success()) {
            Tzz_mod.LOGGER.info(
                    "[ActionEngine] player={} source={} action={} value={} result={}",
                    playerName,
                    sourceType,
                    actionType,
                    value,
                    resultDetailsForLog(result)
            );
        } else {
            String reason = result == null || result.message() == null
                    ? "unknown"
                    : result.message().getString();

            Tzz_mod.LOGGER.warn(
                    "[ActionEngine] failed player={} source={} action={} value={} reason={} result={}",
                    playerName,
                    sourceType,
                    actionType,
                    value,
                    reason,
                    resultDetailsForLog(result)
            );
        }
        WebAdminRealtimeEventBus.publishActionExecution(context, config, result);
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
                .append(Text.literal(actionValueForLog(config)).formatted(Formatting.WHITE));

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

    private static String actionValueForLog(ActionConfig config) {
        if (config == null) {
            return "";
        }
        if (config.type() == ActionType.STATE_VARIABLE) {
            return config.stateActionSummary();
        }
        return config.value() == null ? "" : config.value();
    }

    private static String resultDetailsForLog(ActionExecutionResult result) {
        if (result == null) {
            return "code=missing";
        }
        if (result.details() == null || result.details().isEmpty()) {
            return "code=" + result.code() + " durationNanos=" + result.durationNanos();
        }
        return "code=" + result.code()
                + " changed=" + result.details().getOrDefault("changed", "")
                + " oldValue=" + result.details().getOrDefault("oldValue", "")
                + " newValue=" + result.details().getOrDefault("newValue", "")
                + " failureReason=" + result.details().getOrDefault("failureReason", "")
                + " durationNanos=" + result.durationNanos();
    }
}
