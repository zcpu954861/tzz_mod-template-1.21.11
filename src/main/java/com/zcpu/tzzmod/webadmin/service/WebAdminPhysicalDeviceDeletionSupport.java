package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.WebAdminDeviceMetadataStore;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

final class WebAdminPhysicalDeviceDeletionSupport {
    private WebAdminPhysicalDeviceDeletionSupport() {
    }

    static DeleteResult deletePhysicalDevice(MinecraftServer server, SignalDeviceData device) {
        if (server == null || device == null) {
            return new DeleteResult(false, false, false);
        }
        ServerWorld world = SignalDeviceStore.getDeviceWorld(server, device);
        if (world == null) {
            return new DeleteResult(false, false, false);
        }
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        boolean worldBlockRemoved = world.setBlockState(pos, Blocks.AIR.getDefaultState());
        if (!worldBlockRemoved) {
            return new DeleteResult(false, false, false);
        }
        boolean registryRemoved = SignalDeviceStore.removeById(server, device.id());
        boolean registryGone = registryRemoved || !SignalDeviceStore.resolveDevice(server, device.id()).foundUnique();
        if (!registryGone) {
            SignalDeviceStore.forceFlushDirty(server);
            return new DeleteResult(false, false, false);
        }
        SignalDeviceStore.forceFlushDirty(server);
        boolean metadataRemoved = WebAdminDeviceMetadataStore.removeDeviceAliases(server, device.id(), device.type());
        return new DeleteResult(true, true, metadataRemoved);
    }

    record DeleteResult(boolean worldBlockRemoved, boolean registryRemoved, boolean metadataRemoved) {
    }
}
