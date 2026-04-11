package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class MapPayloads {
    private MapPayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(MapC2SPayload.ID, MapC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MapS2CPayload.ID, MapS2CPayload.CODEC);
    }
}