package com.zcpu.tzzmod.ModItem.custom;

import com.zcpu.tzzmod.map.MapDataStore;
import com.zcpu.tzzmod.map.MapMarkerClientAccess;
import com.zcpu.tzzmod.map.MapServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class MapMarkerItem extends Item {
    public MapMarkerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!user.isSneaking()) {
            return ActionResult.PASS;
        }
        if (world.isClient()) {
            MapMarkerClientAccess.openScreen();
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
                MapMarkerClientAccess.openScreen();
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

        var server = context.getWorld().getServer();
        if (server == null) {
            return ActionResult.PASS;
        }

        MapDataStore.AddMarkerResult result = MapDataStore.addMarker(server, serverPlayer, context.getBlockPos());
        if (result.status() != MapDataStore.AddMarkerStatus.OK) {
            serverPlayer.sendMessage(MapDataStore.describeAddMarkerFailure(result.status()), true);
            return ActionResult.CONSUME;
        }

        serverPlayer.sendMessage(Text.literal("已添加地图标点：" + result.marker().name()), true);
        MapServer.broadcastState(server);
        return ActionResult.CONSUME;
    }
}