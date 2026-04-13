package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class BlockingCardPayloads {
    private BlockingCardPayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(BlockingCardC2SPayload.ID, BlockingCardC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BlockingCardS2CPayload.ID, BlockingCardS2CPayload.CODEC);
    }
}