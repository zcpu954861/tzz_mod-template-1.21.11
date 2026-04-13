package com.zcpu.tzzmod.blocking;

import com.zcpu.tzzmod.ModItem.custom.BlockingCardItem;
import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public final class BlockingCardUseHandler {
    private BlockingCardUseHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof BlockingCardItem)) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            BlockingCardConfig.Data data = BlockingCardConfig.read(stack);
            if (!data.isConfigured() || data.activationType() != BlockingCardConfig.ActivationType.BLOCK) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            if (world.isClient()) {
                return BlockingCardServer.matchesBlock(world, hitResult.getBlockPos(), data.activationInput())
                        ? NullSafety.requireNonNull(ActionResult.SUCCESS)
                        : NullSafety.requireNonNull(ActionResult.PASS);
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }
            return NullSafety.requireNonNull(BlockingCardServer.activateFromBlock(serverPlayer, hand, hitResult.getBlockPos(), data));
        });

        UseEntityCallback.EVENT.register((PlayerEntity player, net.minecraft.world.World world, net.minecraft.util.Hand hand, net.minecraft.entity.Entity entity, net.minecraft.util.hit.EntityHitResult hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof BlockingCardItem)) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            BlockingCardConfig.Data data = BlockingCardConfig.read(stack);
            if (!data.isConfigured() || data.activationType() != BlockingCardConfig.ActivationType.ENTITY) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }

            if (world.isClient()) {
                return BlockingCardServer.looksLikeEntityMatch(entity, data.activationInput())
                        ? NullSafety.requireNonNull(ActionResult.SUCCESS)
                        : NullSafety.requireNonNull(ActionResult.PASS);
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }
            return NullSafety.requireNonNull(BlockingCardServer.activateFromEntity(serverPlayer, hand, entity, data));
        });
    }
}