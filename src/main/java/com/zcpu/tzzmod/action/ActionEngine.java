package com.zcpu.tzzmod.action;

import com.zcpu.tzzmod.signal.SignalBridgeServer;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import java.util.List;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class ActionEngine {
    private ActionEngine() {
    }

    public static ActionExecutionResult execute(ActionContext context, ActionConfig config) {
        if (context == null || config == null || !config.isUsable()) {
            return ActionExecutionResult.failure(Text.literal("动作配置为空"));
        }

        Text validationError = ActionValidator.validateForExecute(context, config);
        if (validationError != null) {
            ActionExecutionResult result = ActionExecutionResult.failure(validationError);
            ActionAuditLogger.log(context, config, result);
            return result;
        }

        try {
            ActionExecutionResult result = switch (config.type()) {
                case COMMAND -> executeCommand(context, config);
                case MESSAGE -> executeMessage(context, config);
                case SOUND -> executeSound(context, config);
                case SIGNAL -> executeSignal(context, config);
            };

            ActionAuditLogger.log(context, config, result);

            if (config.notifyOps()) {
                ActionAuditLogger.notifyOperators(context, config, result);
            }

            return result;
        } catch (Exception exception) {
            ActionExecutionResult result = ActionExecutionResult.failure(
                    Text.literal("动作执行失败: " + exception.getMessage())
            );

            ActionAuditLogger.log(context, config, result);

            if (config.notifyOps()) {
                ActionAuditLogger.notifyOperators(context, config, result);
            }

            return result;
        }
    }

    public static ActionExecutionResult executeAll(ActionContext context, List<ActionConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return ActionExecutionResult.failure(Text.literal("动作列表为空"));
        }

        ActionExecutionResult lastResult = ActionExecutionResult.success(Text.literal("未执行动作"));

        for (ActionConfig config : configs) {
            if (config == null || !config.isUsable()) {
                continue;
            }

            lastResult = execute(context, config);

            if (!lastResult.success()) {
                return lastResult;
            }
        }

        return lastResult;
    }

    private static ActionExecutionResult executeCommand(ActionContext context, ActionConfig config) throws Exception {
        String command = ActionConfig.normalizeCommand(config.value());

        ServerCommandSource source = context.player()
                .getCommandSource()
                .withPermissions(PermissionPredicate.ALL)
                .withSilent()
                .withWorld(context.world())
                .withPosition(context.position());

        source.getServer()
                .getCommandManager()
                .getDispatcher()
                .execute(command, source);

        return ActionExecutionResult.success(Text.literal("命令已执行"));
    }

    private static ActionExecutionResult executeMessage(ActionContext context, ActionConfig config) {
        context.player().sendMessage(Text.literal(config.value()), false);
        return ActionExecutionResult.success(Text.literal("消息已发送"));
    }

    private static ActionExecutionResult executeSound(ActionContext context, ActionConfig config) {
        context.world().playSound(
                null,
                BlockPos.ofFloored(context.position()),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS,
                0.65F,
                1.2F
        );

        return ActionExecutionResult.success(Text.literal("音效已播放"));
    }

    private static ActionExecutionResult executeSignal(ActionContext context, ActionConfig config) {
        String channel = SignalChannel.normalize(config.value());
        SignalEvent event = new SignalEvent(
                channel,
                context.player(),
                context.world(),
                context.position(),
                context.sourceType(),
                context.sourceId(),
                SignalBridgeServer.currentDepth() + 1,
                context.world().getTime()
        );
        return SignalBridgeServer.emit(event);
    }
}
