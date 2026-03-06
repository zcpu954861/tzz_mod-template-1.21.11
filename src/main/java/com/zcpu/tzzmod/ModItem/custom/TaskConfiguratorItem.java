package com.zcpu.tzzmod.ModItem.custom;

import com.zcpu.tzzmod.task.TaskConfiguratorClientAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class TaskConfiguratorItem extends Item {
    public TaskConfiguratorItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            TaskConfiguratorClientAccess.openScreen();
            return ActionResult.SUCCESS;
        }

        return ActionResult.CONSUME;
    }
}

