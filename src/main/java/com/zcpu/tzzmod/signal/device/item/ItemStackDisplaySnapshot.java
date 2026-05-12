package com.zcpu.tzzmod.signal.device.item;

import com.mojang.serialization.DataResult;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.RegistryWrapper;

public final class ItemStackDisplaySnapshot {
    private ItemStackDisplaySnapshot() {
    }

    public static String encode(ItemStack stack, RegistryWrapper.WrapperLookup registries) {
        if (stack == null || stack.isEmpty() || registries == null) {
            return "";
        }
        DataResult<NbtElement> result = ItemStack.CODEC.encodeStart(registries.getOps(NbtOps.INSTANCE), stack.copy());
        return result.result().map(NbtElement::toString).orElse("");
    }

    public static ItemStack decode(String snapshot, RegistryWrapper.WrapperLookup registries) {
        if (snapshot == null || snapshot.isBlank() || registries == null) {
            return ItemStack.EMPTY;
        }
        try {
            NbtElement element = StringNbtReader.fromOps(NbtOps.INSTANCE).read(snapshot);
            return ItemStack.CODEC.parse(registries.getOps(NbtOps.INSTANCE), element)
                    .result()
                    .map(ItemStack::copy)
                    .orElse(ItemStack.EMPTY);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }
}
