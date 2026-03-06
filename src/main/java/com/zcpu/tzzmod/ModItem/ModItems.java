package com.zcpu.tzzmod.ModItem;

import com.zcpu.tzzmod.ModItem.custom.PhoneItem;
import com.zcpu.tzzmod.ModItem.custom.TaskConfiguratorItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;


public final class ModItems {
    public static final Item PHONE = register("phone", PhoneItem::new, new Item.Settings().maxCount(1));
    public static final Item ATTENTION = register("attention", com.zcpu.tzzmod.ModItem.custom.AttentionItem::new, new Item.Settings().maxCount(1));
    public static final Item WHITE_BLOCKING_CARD = register("white_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item LIGHT_GRAY_BLOCKING_CARD = register("light_gray_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item GRAY_BLOCKING_CARD = register("gray_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item BLACK_BLOCKING_CARD = register("black_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item BROWN_BLOCKING_CARD = register("brown_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item RED_BLOCKING_CARD = register("red_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item ORANGE_BLOCKING_CARD = register("orange_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item YELLOW_BLOCKING_CARD = register("yellow_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item LIME_BLOCKING_CARD = register("lime_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item GREEN_BLOCKING_CARD = register("green_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item CYAN_BLOCKING_CARD = register("cyan_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item LIGHT_BLUE_BLOCKING_CARD = register("light_blue_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item BLUE_BLOCKING_CARD = register("blue_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item PURPLE_BLOCKING_CARD = register("purple_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item MAGENTA_BLOCKING_CARD = register("magenta_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item PINK_BLOCKING_CARD = register("pink_blocking_card", com.zcpu.tzzmod.ModItem.custom.BlockingCardItem::new, new Item.Settings().maxCount(64));
    public static final Item TASK_CONFIGURATOR = register("task_configurator", TaskConfiguratorItem::new, new Item.Settings().maxCount(1));

    public static Item register(String path, Function<Item.Settings, Item> factory, Item.Settings settings) {
        final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("tzz_mod", path));
        return Items.register(registryKey, factory, settings);
    }

    public static void initialize(){
    }
}
