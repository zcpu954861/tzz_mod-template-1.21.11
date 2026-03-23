package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PasswordPayloads {
    private PasswordPayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(PasswordC2SPayload.ID, PasswordC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PasswordS2CPayload.ID, PasswordS2CPayload.CODEC);
    }
}

