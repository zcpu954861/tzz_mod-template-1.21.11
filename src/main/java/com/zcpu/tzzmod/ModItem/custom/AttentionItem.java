package com.zcpu.tzzmod.ModItem.custom;

import com.zcpu.tzzmod.Tzz_mod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.sound.SoundEvents;

public class AttentionItem extends Item {
    public AttentionItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        // Client-side: immediate feedback
        if (world.isClient()) {
            user.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
            return ActionResult.SUCCESS;
        }

        // Server-side: snap yaw to nearest 90-degree increment
        float currentYaw = user.getYaw();
        float snapped = Math.round(currentYaw / 90.0F) * 90.0F;

        if (user instanceof ServerPlayerEntity serverPlayer) {
            double x = serverPlayer.getX();
            double y = serverPlayer.getY();
            double z = serverPlayer.getZ();
            float pitch = serverPlayer.getPitch();

            try {
                serverPlayer.networkHandler.requestTeleport(x, y, z, snapped, pitch);
            } catch (Throwable ignored) {
                serverPlayer.refreshPositionAndAngles(x, y, z, snapped, pitch);
            }

            serverPlayer.setHeadYaw(snapped);
            try {
                serverPlayer.setYaw(snapped);
            } catch (Throwable ignored) {
                // ignore if not available
            }

            serverPlayer.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

            Tzz_mod.LOGGER.info("Attention used by {} snapped yaw: {}", serverPlayer.getName().getString(), snapped);

            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }
}

