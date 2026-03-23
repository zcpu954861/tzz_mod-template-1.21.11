package com.zcpu.tzzmod.ModBlock;

import com.zcpu.tzzmod.ModBlock.custom.CatcherChestBlock;
import com.zcpu.tzzmod.ModBlock.custom.PasswordMachineBlock;
import com.zcpu.tzzmod.ModItem.ModItems;
import com.zcpu.tzzmod.Tzz_mod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModBlocks {

    public static final Block CATCHER_CHEST = registerBlock(
            "catcher_chest",
            CatcherChestBlock::new,
            AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque()
    );

    public static final Block PASSWORD_MACHINE = registerBlock(
            "password_machine",
            PasswordMachineBlock::new,
            AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(3.0F).nonOpaque()
    );

    /**
     * 显式的静态初始化方法；在模组主类的 onInitialize 中调用。
     * 其目的是确保类被加载并触发上面的静态字段注册。方法本身可以为空（no-op）。
     */
    public static void init() {
        // No-op: calling this method ensures the class is loaded and static fields are initialized.
    }

    private static <T extends Block> T registerBlock(String name, Function<AbstractBlock.Settings, T> blockFactory, AbstractBlock.Settings settings) {
        Identifier id = Identifier.of(Tzz_mod.MOD_ID, name);
        RegistryKey<Block> key = RegistryKey.of(Registries.BLOCK.getKey(), id);
        AbstractBlock.Settings settingsWithKey = settings.registryKey(key);

        T block = blockFactory.apply(settingsWithKey);
        Registry.register(Registries.BLOCK, id, block);
        registerBlockItem(name, block);
        return block;
    }

    private static void registerBlockItem(String name, Block block) {
        // ModItems provides a `register(String, Function<Item.Settings, Item>, Item.Settings)` helper.
        // Use that to register a BlockItem for this block instead of calling a non-existent registerItem.
        ModItems.register(name, settings -> new BlockItem(block, settings), new Item.Settings());
    }
}
