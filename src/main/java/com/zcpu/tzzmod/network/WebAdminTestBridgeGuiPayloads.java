package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class WebAdminTestBridgeGuiPayloads {
    private WebAdminTestBridgeGuiPayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(WebAdminTestBridgeGuiC2SPayload.ID, WebAdminTestBridgeGuiC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WebAdminTestBridgeGuiS2CPayload.ID, WebAdminTestBridgeGuiS2CPayload.CODEC);
    }
}
