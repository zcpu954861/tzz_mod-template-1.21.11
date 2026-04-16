package com.zcpu.tzzmod.client;

import com.zcpu.tzzmod.client.photo.GalleryClient;
import com.zcpu.tzzmod.network.AdminModePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class AdminSyncClient {
    private AdminSyncClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(AdminModePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    ForcedHudClient.setServerEnforcedHud(payload.recording());
                    GalleryClient.setAdminGalleryEnabled(payload.galleryEnabled());
                })
        );

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ForcedHudClient.setServerEnforcedHud(null);
            GalleryClient.setAdminGalleryEnabled(true);
        });
    }
}
