package com.zcpu.tzzmod.ModItem;


import com.zcpu.tzzmod.ModBlock.ModBlocks;
import com.zcpu.tzzmod.Tzz_mod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModFunctionItemGroup {
    public static final ItemGroup FUNCTION_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModBlocks.PASSWORD_MACHINE))
            .displayName(Text.translatable("itemGroup.tzz_mod.function"))
            .entries(((displayContext, entries) -> {
                entries.add(ModBlocks.PASSWORD_MACHINE);
                entries.add(ModBlocks.SILENT_SENSOR_PLATE);
                entries.add(ModItems.BLOCKING_CARD_CONFIGURATOR);
                entries.add(ModItems.PASSWORD_CONFIG_CARD);
            }))
            .build();

    public static void inialize() {
        // Use an id that surely sorts after "main" so the tab appears after the main group in the creative UI
        Registry.register(Registries.ITEM_GROUP, Identifier.of(Tzz_mod.MOD_ID, "zzz_functions"), FUNCTION_GROUP);
        Tzz_mod.LOGGER.info("Registered item group: {}:{}", Tzz_mod.MOD_ID, "zzz_functions");
    }
}