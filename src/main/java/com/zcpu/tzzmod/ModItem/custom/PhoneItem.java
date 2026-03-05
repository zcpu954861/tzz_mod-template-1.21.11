package com.zcpu.tzzmod.ModItem.custom;

import com.zcpu.tzzmod.phone.PhoneClientAccess;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class PhoneItem extends Item {

    public PhoneItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            PhoneClientAccess.openPhoneScreen();
            return ActionResult.SUCCESS;
        }

        return ActionResult.CONSUME;
    }
}
