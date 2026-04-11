package com.zcpu.tzzmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zcpu.tzzmod.task.TaskDataStore;
import com.zcpu.tzzmod.task.TaskServer;
import com.zcpu.tzzmod.network.TaskS2CPayload;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public final class TaskCommand {
    private TaskCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("task")
                        .then(CommandManager.literal("run")
                    .then(buildLineNameArgument()
                        .then(buildTaskIndexArgument("lineName")
                                                .executes(context -> executeRun(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "lineName"),
                                                        IntegerArgumentType.getInteger(context, "taskIndex")
                                                ))
                                        )
                                )
                        )
                        .then(buildDelSubcommands())
                        .then(CommandManager.literal("cancel")
                            .then(buildLineNameArgument()
                                .then(buildTaskIndexArgument("lineName")
                                                .executes(context -> executeCancel(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "lineName"),
                                                        IntegerArgumentType.getInteger(context, "taskIndex")
                                                ))))
        ));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildDelSubcommands() {
        // /task del task <lineName> <taskIndex>
        RequiredArgumentBuilder<ServerCommandSource, String> delTaskLineArg = buildLineNameArgument();
        RequiredArgumentBuilder<ServerCommandSource, Integer> delTaskIndexArg = buildTaskIndexArgument("lineName");

        LiteralArgumentBuilder<ServerCommandSource> delTask = CommandManager.literal("task")
                .then(delTaskLineArg.then(delTaskIndexArg.executes(context -> executeDeleteTask(
                        context.getSource(),
                        StringArgumentType.getString(context, "lineName"),
                        IntegerArgumentType.getInteger(context, "taskIndex")
                ))));

        // /task del taskline <lineName>
        LiteralArgumentBuilder<ServerCommandSource> delTaskline = CommandManager.literal("taskline")
            .then(buildLineNameArgument()
                        .executes(context -> executeDeleteLine(
                                context.getSource(),
                                StringArgumentType.getString(context, "lineName")
                        )));

        return CommandManager.literal("del").then(delTask).then(delTaskline);
    }

        private static RequiredArgumentBuilder<ServerCommandSource, String> buildLineNameArgument() {
        return CommandManager.argument("lineName", StringArgumentType.string())
            .suggests((context, builder) -> CommandSuggestionUtil.suggestTaskLineNames(context.getSource(), builder));
        }

        private static RequiredArgumentBuilder<ServerCommandSource, Integer> buildTaskIndexArgument(String lineArgumentName) {
        return CommandManager.argument("taskIndex", IntegerArgumentType.integer(1))
            .suggests((context, builder) -> CommandSuggestionUtil.suggestTaskIndexes(context, builder, lineArgumentName));
        }

    private static int executeDeleteTask(ServerCommandSource source, String lineName, int taskIndex) {
        if (source == null || source.getServer() == null) return 0;

        boolean allowed = true;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            allowed = player.isCreativeLevelTwoOp();
        }
        if (!allowed) {
            source.sendFeedback(() -> Text.literal("你没有权限使用 /task del（需要 OP）。"), false);
            return 0;
        }

        boolean removed = TaskDataStore.deleteTask(source.getServer(), lineName, taskIndex);
        if (!removed) {
            source.sendFeedback(() -> Text.literal("删除失败：任务线不存在或序号超出范围。"), false);
            return 0;
        }

        TaskServer.syncAll(source.getServer());
        source.sendFeedback(() -> Text.literal("已删除任务线 '" + lineName + "' 的第 " + taskIndex + " 个任务。"), true);
        return 1;
    }

    private static int executeDeleteLine(ServerCommandSource source, String lineName) {
        if (source == null || source.getServer() == null) return 0;

        boolean allowed = true;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            allowed = player.isCreativeLevelTwoOp();
        }
        if (!allowed) {
            source.sendFeedback(() -> Text.literal("你没有权限使用 /task del（需要 OP）。"), false);
            return 0;
        }

        boolean removed = TaskDataStore.deleteTaskLine(source.getServer(), lineName);
        if (!removed) {
            source.sendFeedback(() -> Text.literal("删除失败：任务线不存在。"), false);
            return 0;
        }

        TaskServer.syncAll(source.getServer());
        source.sendFeedback(() -> Text.literal("已删除任务线 '" + lineName + "'。"), true);
        return 1;
    }

    private static int executeRun(ServerCommandSource source, String lineName, int taskIndex) {
        if (source == null || source.getServer() == null) {
            return 0;
        }

        boolean allowed = true;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            allowed = player.isCreativeLevelTwoOp();
        }
        if (!allowed) {
            source.sendFeedback(() -> Text.literal("你没有权限使用 /task run（需要 OP）。"), false);
            return 0;
        }

        boolean triggered = TaskDataStore.triggerTask(source.getServer(), lineName, taskIndex);
        if (!triggered) {
            source.sendFeedback(() -> Text.literal("任务线不存在，或任务序号超出范围。"), false);
            return 0;
        }

        // send explicit 'triggered' notification to clients so they can display alerts immediately
        JsonObject triggeredPayload = new JsonObject();
        triggeredPayload.addProperty("lineName", lineName);
        triggeredPayload.addProperty("taskIndex", taskIndex);
        for (ServerPlayerEntity p : source.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(com.zcpu.tzzmod.util.NullSafety.requireNonNull(p), new TaskS2CPayload("triggered", triggeredPayload.toString()));
        }
        // full sync to clients
        TaskServer.syncAll(source.getServer());
        source.sendFeedback(() -> Text.literal("已触发任务线 '" + lineName + "' 的第 " + taskIndex + " 个任务。"), true);
        return 1;
    }

    private static int executeCancel(ServerCommandSource source, String lineName, int taskIndex) {
        if (source == null || source.getServer() == null) return 0;

        boolean allowed = true;
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            allowed = player.isCreativeLevelTwoOp();
        }
        if (!allowed) {
            source.sendFeedback(() -> Text.literal("你没有权限使用 /task cancel（需要 OP）。"), false);
            return 0;
        }

        boolean removed = TaskDataStore.untriggerTask(source.getServer(), lineName, taskIndex);
        if (!removed) {
            source.sendFeedback(() -> Text.literal("取消触发失败：任务线不存在或序号超出范围，或该任务未被触发。"), false);
            return 0;
        }

        TaskServer.syncAll(source.getServer());
        source.sendFeedback(() -> Text.literal("已取消任务线 '" + lineName + "' 的第 " + taskIndex + " 个任务的触发。"), true);
        return 1;
    }
}
