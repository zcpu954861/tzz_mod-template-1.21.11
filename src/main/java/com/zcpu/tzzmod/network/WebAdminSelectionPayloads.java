package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class WebAdminSelectionPayloads {
    private WebAdminSelectionPayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(WebAdminSelectionC2SPayload.ID, WebAdminSelectionC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WebAdminSelectionS2CPayload.ID, WebAdminSelectionS2CPayload.CODEC);
    }
}
