package com.zcpu.tzzmod.signal.device;

import java.util.StringJoiner;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.registry.Registries;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class ContainerDeviceSupport {
    private ContainerDeviceSupport() {
    }

    public static boolean isContainer(ServerWorld world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof Inventory || blockEntity instanceof NamedScreenHandlerFactory;
    }

    public static boolean hasInventory(ServerWorld world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof Inventory;
    }

    public static int slotCount(ServerWorld world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof Inventory inventory ? inventory.size() : -1;
    }

    public static String fingerprint(ServerWorld world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof Inventory inventory)) {
            return "";
        }

        StringJoiner joiner = new StringJoiner("|");
        for (int slot = 0; slot < inventory.size(); slot++) {
            net.minecraft.item.ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) {
                joiner.add(slot + ":empty:0:0");
            } else {
                String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                joiner.add(slot + ":" + itemId + ":" + stack.getCount() + ":" + stack.getDamage());
            }
        }
        return joiner.toString();
    }
}
