package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class WebAdminSingleItemSubmitTemplatePayloads {
    private WebAdminSingleItemSubmitTemplatePayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(WebAdminSingleItemSubmitTemplateC2SPayload.ID, WebAdminSingleItemSubmitTemplateC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WebAdminSingleItemSubmitTemplateS2CPayload.ID, WebAdminSingleItemSubmitTemplateS2CPayload.CODEC);
    }
}
