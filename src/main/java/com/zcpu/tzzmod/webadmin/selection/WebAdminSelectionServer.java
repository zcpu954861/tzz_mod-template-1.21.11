package com.zcpu.tzzmod.webadmin.selection;

import com.zcpu.tzzmod.network.WebAdminSelectionC2SPayload;
import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;

public final class WebAdminSelectionServer {
    private WebAdminSelectionServer() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer) || !(world instanceof ServerWorld serverWorld)) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }
            if (WebAdminSelectionSessions.handleUseBlock(serverWorld.getServer(), serverPlayer, hand, hitResult)) {
                return NullSafety.requireNonNull(ActionResult.SUCCESS_SERVER);
            }
            if (WebAdminSelectionSessions.shouldBlockProtectedDraftUse(serverPlayer, serverWorld, hitResult.getBlockPos())) {
                return NullSafety.requireNonNull(ActionResult.FAIL);
            }
            return NullSafety.requireNonNull(ActionResult.PASS);
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer) || !(world instanceof ServerWorld serverWorld)) {
                return NullSafety.requireNonNull(ActionResult.PASS);
            }
            return WebAdminSelectionSessions.shouldBlockBreak(serverPlayer)
                    || WebAdminSelectionSessions.shouldBlockProtectedDraftBreak(serverPlayer, serverWorld, pos)
                    ? NullSafety.requireNonNull(ActionResult.FAIL)
                    : NullSafety.requireNonNull(ActionResult.PASS);
        });
        ServerPlayNetworking.registerGlobalReceiver(WebAdminSelectionC2SPayload.ID, (payload, context) ->
                context.server().execute(() -> handlePayload(context.server(), context.player(), payload))
        );
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                server.execute(() -> WebAdminSelectionSessions.cancelForDisconnect(handler.getPlayer()))
        );
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> WebAdminSelectionSessions.restorePendingWorldDeviceHotbarMode(handler.getPlayer()))
        );
    }

    private static void handlePayload(
            net.minecraft.server.MinecraftServer server,
            net.minecraft.server.network.ServerPlayerEntity player,
            WebAdminSelectionC2SPayload payload
    ) {
        switch (payload.action()) {
            case "complete" -> WebAdminSelectionSessions.completeFromClient(server, player, payload.bodyJson());
            case "cancel" -> WebAdminSelectionSessions.cancelFromClient(server, player, payload.bodyJson());
            case "world_device_slot" -> WebAdminSelectionSessions.updateWorldDeviceSelectedSlotFromClient(server, player, payload.bodyJson());
            default -> {
            }
        }
    }
}
