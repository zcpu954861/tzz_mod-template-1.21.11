package com.zcpu.tzzmod.ModItem;

import com.zcpu.tzzmod.ModItem.custom.PhoneItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;


public final class ModItems {
    private void Tzz_modItems() {
    }

    public static final Item PHONE = register("phone", PhoneItem::new, new Item.Settings().maxCount(1));

    public static Item register(String path, Function<Item.Settings, Item> factory, Item.Settings settings) {
        final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("tzz_mod", path));
        return Items.register(registryKey, factory, settings);
    }

    public static void initialize(){
    }
}
