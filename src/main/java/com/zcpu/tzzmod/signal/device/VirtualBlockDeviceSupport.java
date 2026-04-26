package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.ModBlock.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class VirtualBlockDeviceSupport {
    private VirtualBlockDeviceSupport() {
    }

    public static String id(ServerWorld world, BlockPos pos) {
        return SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE + ":" + SignalDeviceStore.sourceId(world, pos);
    }

    public static VirtualBlockPowerState powerState(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        String blockId = blockId(state);
        boolean blockStatePowered = state.contains(Properties.POWERED) && state.get(Properties.POWERED);
        int receivedPowerLevel = Math.max(0, Math.min(15, world.getReceivedRedstonePower(pos)));
        boolean currentPowered = blockStatePowered || receivedPowerLevel > 0;
        return new VirtualBlockPowerState(blockId, blockStatePowered, receivedPowerLevel, currentPowered, state.isAir());
    }

    public static String blockId(BlockState state) {
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        return id == null ? "unknown" : id.toString();
    }

    public static boolean isDedicatedSignalDevice(BlockState state) {
        return state.isOf(ModBlocks.SIGNAL_EMITTER)
                || state.isOf(ModBlocks.SIGNAL_RECEIVER)
                || state.isOf(ModBlocks.ACTION_RELAY);
    }
}
