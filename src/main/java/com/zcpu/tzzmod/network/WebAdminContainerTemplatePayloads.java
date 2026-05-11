package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class WebAdminContainerTemplatePayloads {
    private WebAdminContainerTemplatePayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(WebAdminContainerTemplateC2SPayload.ID, WebAdminContainerTemplateC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WebAdminContainerTemplateS2CPayload.ID, WebAdminContainerTemplateS2CPayload.CODEC);
    }
}
