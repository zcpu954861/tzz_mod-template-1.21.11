package com.zcpu.tzzmod.signal;

import com.zcpu.tzzmod.action.ActionSourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public record SignalEvent(
        String channel,
        ServerPlayerEntity player,
        ServerWorld world,
        Vec3d position,
        ActionSourceType sourceType,
        String sourceId,
        int depth,
        long gameTime
) {
}
