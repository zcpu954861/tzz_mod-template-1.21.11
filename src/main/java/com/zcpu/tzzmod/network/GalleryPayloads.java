package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class GalleryPayloads {
    private GalleryPayloads() {}

    public static void register() {
        PayloadTypeRegistry.playC2S().register(GalleryC2SPayload.ID, GalleryC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GalleryS2CPayload.ID, GalleryS2CPayload.CODEC);
    }
}
