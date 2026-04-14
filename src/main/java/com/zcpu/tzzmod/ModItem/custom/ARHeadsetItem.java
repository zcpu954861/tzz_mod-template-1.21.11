package com.zcpu.tzzmod.ModItem.custom;

import com.zcpu.tzzmod.ar.ARClientAccess;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class ARHeadsetItem extends Item {

    public ARHeadsetItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            ARClientAccess.openARScreen();
            return ActionResult.SUCCESS;
        }
        return ActionResult.CONSUME;
    }
}
