package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TaskC2SPayload(String action, String bodyJson) implements CustomPayload {
    public static final Id<TaskC2SPayload> ID =
            new Id<>(Identifier.of(Tzz_mod.MOD_ID, "task_c2s"));

    public static final PacketCodec<RegistryByteBuf, TaskC2SPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            TaskC2SPayload::action,
            PacketCodecs.STRING,
            TaskC2SPayload::bodyJson,
            TaskC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

