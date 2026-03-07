package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class AdminPayloads {
    private AdminPayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(AdminModeC2SPayload.ID, AdminModeC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AdminModePayload.ID, AdminModePayload.CODEC);
    }
}

