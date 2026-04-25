package com.zcpu.tzzmod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zcpu.tzzmod.map.MapDataStore;
import com.zcpu.tzzmod.map.MapServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class MapCommand {
    private MapCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("map")
                .then(CommandManager.literal("set")
                        .then(CommandManager.literal("xyz")
                                .then(CommandManager.argument("x1", IntegerArgumentType.integer())
                                        .suggests((context, builder) -> CommandSuggestionUtil.suggestCoordinate(context, builder, CommandSuggestionUtil.Axis.X, ""))
                                        .then(CommandManager.argument("y1", IntegerArgumentType.integer())
                                                .suggests((context, builder) -> CommandSuggestionUtil.suggestCoordinate(context, builder, CommandSuggestionUtil.Axis.Y, ""))
                                                .then(CommandManager.argument("z1", IntegerArgumentType.integer())
                                                        .suggests((context, builder) -> CommandSuggestionUtil.suggestCoordinate(context, builder, CommandSuggestionUtil.Axis.Z, ""))
                                                        .then(CommandManager.argument("x2", IntegerArgumentType.integer())
                                                                .suggests((context, builder) -> CommandSuggestionUtil.suggestCoordinate(context, builder, CommandSuggestionUtil.Axis.X, "x1"))
                                                                .then(CommandManager.argument("y2", IntegerArgumentType.integer())
                                                                        .suggests((context, builder) -> CommandSuggestionUtil.suggestCoordinate(context, builder, CommandSuggestionUtil.Axis.Y, "y1"))
                                                                        .then(CommandManager.argument("z2", IntegerArgumentType.integer())
                                                                                .suggests((context, builder) -> CommandSuggestionUtil.suggestCoordinate(context, builder, CommandSuggestionUtil.Axis.Z, "z1"))
                                                                                .executes(context -> executeSet(
                                                                                        context.getSource(),
                                                                                        IntegerArgumentType.getInteger(context, "x1"),
                                                                                        IntegerArgumentType.getInteger(context, "y1"),
                                                                                        IntegerArgumentType.getInteger(context, "z1"),
                                                                                        IntegerArgumentType.getInteger(context, "x2"),
                                                                                        IntegerArgumentType.getInteger(context, "y2"),
                                                                                        IntegerArgumentType.getInteger(context, "z2")
                                                                                ))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                );
    }

    private static int executeSet(ServerCommandSource source, int x1, int y1, int z1, int x2, int y2, int z2) {
        if (source.getServer() == null || source.getWorld() == null) {
            return 0;
        }

                if (source.getEntity() instanceof ServerPlayerEntity player && !player.isCreativeLevelTwoOp()) {
                        source.sendFeedback(() -> Text.literal("你没有权限使用 /map set（需要 OP）。"), false);
                        return 0;
                }

        String dimensionId = source.getWorld().getRegistryKey().getValue().toString();
        MapDataStore.setRegion(source.getServer(), dimensionId, x1, y1, z1, x2, y2, z2);
        MapServer.broadcastSnapshot(source.getServer());
        source.sendFeedback(() -> Text.literal("地图区域已更新为 [" + x1 + ", " + y1 + ", " + z1 + "] -> [" + x2 + ", " + y2 + ", " + z2 + "]。"), true);
        return 1;
    }
}
