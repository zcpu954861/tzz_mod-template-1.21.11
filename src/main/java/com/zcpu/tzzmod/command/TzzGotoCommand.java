package com.zcpu.tzzmod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public final class TzzGotoCommand {
    private TzzGotoCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Intentionally left blank to disable the /tzz_goto command registration.
    }

    private static int execute(ServerCommandSource source, String target) {
        if (source == null || source.getServer() == null) return 0;

        // pattern-variable in a negated instanceof is not in scope for later use in Java.
        // To ensure `player` is available for the rest of the method, check the entity first
        // and then cast explicitly.
        var entity = source.getEntity();
        if (!(entity instanceof ServerPlayerEntity)) {
            source.sendFeedback(() -> Text.literal("命令需由玩家执行。"), false);
            return 0;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) entity;

        if (!player.isCreativeLevelTwoOp()) {
            source.sendFeedback(() -> Text.literal("你没有权限使用此命令。需要 OP。"), false);
            return 0;
        }

        // try by UUID first
        try {
            UUID id = UUID.fromString(target);
            var targetPlayer = source.getServer().getPlayerManager().getPlayer(id);
            if (targetPlayer == null) {
                source.sendFeedback(() -> Text.literal("未找到指定玩家（UUID）。"), false);
                return 0;
            }
            // teleport the caller to target
            double x = targetPlayer.getX();
            double y = targetPlayer.getY();
            double z = targetPlayer.getZ();
            float yaw = player.getYaw();
            float pitch = player.getPitch();
            try {
                player.networkHandler.requestTeleport(x, y, z, yaw, pitch);
            } catch (Throwable ignored) {
                player.refreshPositionAndAngles(x, y, z, yaw, pitch);
            }
            source.sendFeedback(() -> Text.literal("已传送至 " + targetPlayer.getName().getString()), false);
            return 1;
        } catch (Exception ignored) {
            // not a UUID, try by name
        }

        var found = source.getServer().getPlayerManager().getPlayer(target);
        if (found == null) {
            source.sendFeedback(() -> Text.literal("未找到指定玩家。"), false);
            return 0;
        }

        try {
            player.networkHandler.requestTeleport(found.getX(), found.getY(), found.getZ(), player.getYaw(), player.getPitch());
        } catch (Throwable ignored) {
            player.refreshPositionAndAngles(found.getX(), found.getY(), found.getZ(), player.getYaw(), player.getPitch());
        }

        source.sendFeedback(() -> Text.literal("已传送至 " + found.getName().getString()), false);
        return 1;
    }
}
