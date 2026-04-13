package com.zcpu.tzzmod.ModItem;

import com.zcpu.tzzmod.ModBlock.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.zcpu.tzzmod.Tzz_mod;

public final class ModItemGroup {
    public static final ItemGroup ITEM_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.PHONE))
            .displayName(Text.translatable("itemGroup.tzz_mod.main"))
            .entries((displayContext, entries) -> {
                entries.add(ModItems.PHONE);
                entries.add(ModItems.ATTENTION);
                entries.add(ModItems.MAP_MARKER);
                entries.add(ModItems.REGION_PLANNER);
                entries.add(ModItems.TASK_CONFIGURATOR);
                entries.add(ModItems.BLOCKING_CARD_CONFIGURATOR);
                entries.add(ModItems.WHITE_BLOCKING_CARD);
                entries.add(ModItems.LIGHT_GRAY_BLOCKING_CARD);
                entries.add(ModItems.GRAY_BLOCKING_CARD);
                entries.add(ModItems.BLACK_BLOCKING_CARD);
                entries.add(ModItems.BROWN_BLOCKING_CARD);
                entries.add(ModItems.RED_BLOCKING_CARD);
                entries.add(ModItems.ORANGE_BLOCKING_CARD);
                entries.add(ModItems.YELLOW_BLOCKING_CARD);
                entries.add(ModItems.LIME_BLOCKING_CARD);
                entries.add(ModItems.GREEN_BLOCKING_CARD);
                entries.add(ModItems.CYAN_BLOCKING_CARD);
                entries.add(ModItems.LIGHT_BLUE_BLOCKING_CARD);
                entries.add(ModItems.BLUE_BLOCKING_CARD);
                entries.add(ModItems.PURPLE_BLOCKING_CARD);
                entries.add(ModItems.MAGENTA_BLOCKING_CARD);
                entries.add(ModItems.PINK_BLOCKING_CARD);
                entries.add(ModBlocks.CATCHER_CHEST);
            })
            .build();

    public static void inialize() {
        Registry.register(Registries.ITEM_GROUP, Identifier.of("tzz_mod", "main"), ITEM_GROUP);
        Tzz_mod.LOGGER.info("Registered item group: {}:{}", Tzz_mod.MOD_ID, "main");
    }
}
