package com.zcpu.tzzmod.ModItem;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItemGroup {
    public static final ItemGroup ITEM_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.PHONE))
            .displayName(Text.translatable("itemGroup.tzz_mod.main"))
            .entries((displayContext, entries) -> {
                entries.add(ModItems.PHONE);
                entries.add(ModItems.GAME_BUILDER);
                entries.add(ModItems.ATTENTION);
            })
            .build();

    public static void inialize() {
        Registry.register(Registries.ITEM_GROUP, Identifier.of("tzz_mod", "main"), ITEM_GROUP);
    }
}
