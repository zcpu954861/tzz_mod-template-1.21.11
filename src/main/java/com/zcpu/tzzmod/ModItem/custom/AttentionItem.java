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
        // Client-side: play a sound so the player gets immediate feedback and return SUCCESS so the
        // client shows the use animation right away.
        if (world.isClient()) {
            user.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
            return ActionResult.SUCCESS;
        }

        // Server-side: snap the player's yaw to the nearest multiple of 90 degrees
        float currentYaw = user.getYaw();
        // Normalize and snap
        float snapped = Math.round(currentYaw / 90.0F) * 90.0F;

        // Ensure we have a server player so the rotation update is sent to the client
        if (user instanceof ServerPlayerEntity serverPlayer) {
            // Keep position and pitch unchanged, only update yaw (body rotation)
            double x = serverPlayer.getX();
            double y = serverPlayer.getY();
            double z = serverPlayer.getZ();
            float pitch = serverPlayer.getPitch();

            // Use the network handler teleport/request method so the client receives a rotation
            // update even when the position doesn't change. Also update server-side yaw/head yaw
            // fields so server-side logic sees the change immediately.
            try {
                // Attempt to request a teleport via the player's network handler. This will send a
                // PlayerPositionLook packet to the client which forces the client to apply the
                // new rotation.
                serverPlayer.networkHandler.requestTeleport(x, y, z, snapped, pitch);
            } catch (Throwable ignored) {
                // Fallback: call refreshPositionAndAngles which may update rotation in some mappings
                serverPlayer.refreshPositionAndAngles(x, y, z, snapped, pitch);
            }

            // Update server-side yaw/head yaw/body yaw so game logic on server reflects it.
            serverPlayer.setHeadYaw(snapped);
            // The following field/methods may differ across mappings; set yaw via the public API if available.
            try {
                serverPlayer.setYaw(snapped);
            } catch (Throwable ignored) {
                // If setYaw isn't available, set bodyYaw via reflection as a best-effort (avoid reflection here
                // to keep compatibility). Many mappings expose setYaw, but if not, head yaw above is usually
                // sufficient for most server-side logic.
            }

            // Play a sound for the player on the server (ensures the client hears it even if
            // the client-side playback was missed)
            serverPlayer.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

            // Remove actionbar/chat messages per request: keep only a server log for debugging
            Tzz_mod.LOGGER.info("Attention used by {} snapped yaw: {}", serverPlayer.getName().getString(), snapped);

            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }
}
