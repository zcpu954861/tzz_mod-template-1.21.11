package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class NotePayloads {
    private NotePayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(NoteC2SPayload.ID, NoteC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NoteS2CPayload.ID, NoteS2CPayload.CODEC);
    }
}
