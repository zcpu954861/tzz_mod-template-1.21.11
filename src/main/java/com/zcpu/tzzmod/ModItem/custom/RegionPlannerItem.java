package com.zcpu.tzzmod.ModItem.custom;

import com.zcpu.tzzmod.map.MapServer;
import com.zcpu.tzzmod.map.RegionPlannerClientAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class RegionPlannerItem extends Item {
    public RegionPlannerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!user.isSneaking()) {
            return ActionResult.PASS;
        }
        if (world.isClient()) {
            RegionPlannerClientAccess.openScreen();
            return ActionResult.SUCCESS;
        }
        return ActionResult.CONSUME;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResult.PASS;
        }

        if (player.isSneaking()) {
            if (context.getWorld().isClient()) {
                RegionPlannerClientAccess.openScreen();
                return ActionResult.SUCCESS;
            }
            return ActionResult.CONSUME;
        }

        if (context.getWorld().isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer) || context.getWorld().getServer() == null) {
            return ActionResult.PASS;
        }

        serverPlayer.sendMessage(MapServer.handlePlannerSelection(context.getWorld().getServer(), serverPlayer, context.getBlockPos()), true);
        return ActionResult.CONSUME;
    }
}