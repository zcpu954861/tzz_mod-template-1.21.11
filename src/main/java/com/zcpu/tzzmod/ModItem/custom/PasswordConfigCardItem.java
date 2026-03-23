package com.zcpu.tzzmod.ModItem.custom;

import com.zcpu.tzzmod.password.PasswordCardClientAccess;
import com.zcpu.tzzmod.password.PasswordCodeUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class PasswordConfigCardItem extends Item {
    public static final String PASSWORD_KEY = "password_code";

    public PasswordConfigCardItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            PasswordCardClientAccess.openScreen(hand);
            return ActionResult.SUCCESS;
        }
        return ActionResult.CONSUME;
    }

    @Override
    public Text getName(ItemStack stack) {
        if (hasConfiguredPassword(stack)) {
            return Text.translatable("item.tzz_mod.password_config_card.configured");
        }
        return super.getName(stack);
    }

    public static boolean hasConfiguredPassword(ItemStack stack) {
        return PasswordCodeUtil.isValid(getStoredPasswordRaw(stack));
    }

    public static String getStoredPassword(ItemStack stack) {
        String raw = getStoredPasswordRaw(stack);
        return PasswordCodeUtil.isValid(raw) ? raw : PasswordCodeUtil.DEFAULT_CODE;
    }

    public static String getStoredPasswordRaw(ItemStack stack) {
        NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return customData.copyNbt().getString(PASSWORD_KEY).orElse("");
    }

    public static void setStoredPassword(ItemStack stack, String code) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.putString(PASSWORD_KEY, PasswordCodeUtil.normalize(code));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}

