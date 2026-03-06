package com.zcpu.tzzmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class TaskPayloads {
    private TaskPayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TaskC2SPayload.ID, TaskC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TaskS2CPayload.ID, TaskS2CPayload.CODEC);
    }
}

