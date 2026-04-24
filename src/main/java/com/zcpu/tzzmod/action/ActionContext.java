package com.zcpu.tzzmod.action;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public record ActionContext(
        ServerPlayerEntity player,
        ServerWorld world,
        Vec3d position,
        ActionSourceType sourceType,
        String sourceId,
        ItemStack sourceStack
) {
}
