package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PhoneChatPayloads {
    private PhoneChatPayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(PhoneChatC2SPayload.ID, PhoneChatC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PhoneChatS2CPayload.ID, PhoneChatS2CPayload.CODEC);
    }
}

