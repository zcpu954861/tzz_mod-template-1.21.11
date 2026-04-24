package com.zcpu.tzzmod.datagen;

import com.zcpu.tzzmod.ModBlock.ModBlocks;
import com.zcpu.tzzmod.ModItem.ModItems;
import java.util.List;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.Blocks;
import net.minecraft.client.data.BlockStateModelGenerator;
import net.minecraft.client.data.ItemModelGenerator;
import net.minecraft.client.data.ModelIds;
import net.minecraft.client.data.Models;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

public class TzzModelProvider extends FabricModelProvider {
    private static final List<Item> GENERATED_ITEMS = List.of(
            ModItems.PHONE,
            ModItems.AR_HEADSET,
            ModItems.ATTENTION,
            ModItems.PASSWORD_CONFIG_CARD,
            ModItems.MAP_MARKER,
            ModItems.REGION_PLANNER,
            ModItems.TASK_CONFIGURATOR,
            ModItems.BLOCKING_CARD_CONFIGURATOR,
            ModItems.WHITE_BLOCKING_CARD,
            ModItems.LIGHT_GRAY_BLOCKING_CARD,
            ModItems.GRAY_BLOCKING_CARD,
            ModItems.BLACK_BLOCKING_CARD,
            ModItems.BROWN_BLOCKING_CARD,
            ModItems.RED_BLOCKING_CARD,
            ModItems.ORANGE_BLOCKING_CARD,
            ModItems.YELLOW_BLOCKING_CARD,
            ModItems.LIME_BLOCKING_CARD,
            ModItems.GREEN_BLOCKING_CARD,
            ModItems.CYAN_BLOCKING_CARD,
            ModItems.LIGHT_BLUE_BLOCKING_CARD,
            ModItems.BLUE_BLOCKING_CARD,
            ModItems.PURPLE_BLOCKING_CARD,
            ModItems.MAGENTA_BLOCKING_CARD,
            ModItems.PINK_BLOCKING_CARD,
            ModItems.APP_ICON_MAP,
            ModItems.APP_ICON_CHAT,
            ModItems.APP_ICON_TASK,
            ModItems.APP_ICON_CALL_ADMIN,
            ModItems.APP_ICON_SETTINGS,
            ModItems.APP_ICON_COMPASS,
            ModItems.APP_ICON_ADMIN,
            ModItems.APP_ICON_CAMERA,
            ModItems.APP_ICON_GALLERY,
            ModItems.APP_ICON_NOTES
    );
    private static final Identifier HEAVY_PRESSURE_PLATE_MODEL = ModelIds.getBlockModelId(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
    private static final Identifier HEAVY_PRESSURE_PLATE_DOWN_MODEL =
            ModelIds.getBlockSubModelId(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, "_down");

    public TzzModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        blockStateModelGenerator.blockStateCollector.accept(
                BlockStateModelGenerator.createPressurePlateBlockState(
                        ModBlocks.SILENT_SENSOR_PLATE,
                        BlockStateModelGenerator.createWeightedVariant(HEAVY_PRESSURE_PLATE_MODEL),
                        BlockStateModelGenerator.createWeightedVariant(HEAVY_PRESSURE_PLATE_DOWN_MODEL)
                )
        );
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.SILENT_SENSOR_PLATE, HEAVY_PRESSURE_PLATE_MODEL);
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        for (Item item : GENERATED_ITEMS) {
            itemModelGenerator.register(item, Models.GENERATED);
        }
    }
}
