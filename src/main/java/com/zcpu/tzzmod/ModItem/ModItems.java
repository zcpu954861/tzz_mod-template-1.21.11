package com.zcpu.tzzmod.ModItem;

import com.zcpu.tzzmod.ModItem.custom.PhoneItem;
import com.zcpu.tzzmod.ModItem.custom.ARHeadsetItem;
import com.zcpu.tzzmod.ModItem.custom.BlockingCardConfiguratorItem;
import com.zcpu.tzzmod.ModItem.custom.PasswordConfigCardItem;
import com.zcpu.tzzmod.ModItem.custom.RegionPlannerItem;
import com.zcpu.tzzmod.ModItem.custom.TaskConfiguratorItem;
import com.zcpu.tzzmod.ModItem.custom.MapMarkerItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;


public final class ModItems {
    public static final Item PHONE = register("phone", PhoneItem::new, new Item.Settings().maxCount(1));
    public static final Item AR_HEADSET = register("ar_headset", ARHeadsetItem::new, new Item.Settings().maxCount(1)
            .component(DataComponentTypes.EQUIPPABLE, EquippableComponent.builder(EquipmentSlot.HEAD).build()));
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
    public static final Item BLOCKING_CARD_CONFIGURATOR = register("blocking_card_configurator", BlockingCardConfiguratorItem::new, new Item.Settings().maxCount(1));
    public static final Item PASSWORD_CONFIG_CARD = register("password_config_card", PasswordConfigCardItem::new, new Item.Settings().maxCount(1));
    public static final Item MAP_MARKER = register("map_marker", MapMarkerItem::new, new Item.Settings().maxCount(1));
    public static final Item REGION_PLANNER = register("region_planner", RegionPlannerItem::new, new Item.Settings().maxCount(1));

    // APP icon items — unobtainable, used for 3D rendering in AR headset
    public static final Item APP_ICON_MAP = register("app_icon_map", Item::new, new Item.Settings());
    public static final Item APP_ICON_CHAT = register("app_icon_chat", Item::new, new Item.Settings());
    public static final Item APP_ICON_TASK = register("app_icon_task", Item::new, new Item.Settings());
    public static final Item APP_ICON_CALL_ADMIN = register("app_icon_call_admin", Item::new, new Item.Settings());
    public static final Item APP_ICON_SETTINGS = register("app_icon_settings", Item::new, new Item.Settings());
    public static final Item APP_ICON_COMPASS = register("app_icon_compass", Item::new, new Item.Settings());
    public static final Item APP_ICON_ADMIN = register("app_icon_admin", Item::new, new Item.Settings());
    public static final Item APP_ICON_CAMERA = register("app_icon_camera", Item::new, new Item.Settings());
    public static final Item APP_ICON_GALLERY = register("app_icon_gallery", Item::new, new Item.Settings());

    public static Item register(String path, Function<Item.Settings, Item> factory, Item.Settings settings) {
        final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("tzz_mod", path));
        return Items.register(registryKey, factory, settings);
    }

    public static void initialize(){
    }
}
