package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.util.NullSafety;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;

public record TaskC2SPayload(String action, String bodyJson) implements CustomPayload {
    public static final CustomPayload.@NonNull Id<TaskC2SPayload> ID =
        NullSafety.requireNonNull(new Id<>(Identifier.of(Tzz_mod.MOD_ID, "task_c2s")));

    public static final @NonNull PacketCodec<RegistryByteBuf, TaskC2SPayload> CODEC =
        NullSafety.requireNonNull(PacketCodec.tuple(
            PacketCodecs.STRING,
            TaskC2SPayload::action,
            PacketCodecs.STRING,
            TaskC2SPayload::bodyJson,
            TaskC2SPayload::new
        ));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

