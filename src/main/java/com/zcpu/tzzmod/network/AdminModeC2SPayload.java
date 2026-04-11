package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.util.NullSafety;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;

public record AdminModeC2SPayload(boolean recording) implements CustomPayload {
    public static final CustomPayload.@NonNull Id<AdminModeC2SPayload> ID =
        NullSafety.requireNonNull(new CustomPayload.Id<>(Identifier.of(Tzz_mod.MOD_ID, "admin_mode_c2s")));

    public static final @NonNull PacketCodec<RegistryByteBuf, AdminModeC2SPayload> CODEC =
        NullSafety.requireNonNull(PacketCodec.tuple(
            PacketCodecs.BOOLEAN,
            AdminModeC2SPayload::recording,
            value -> new AdminModeC2SPayload(NullSafety.requireNonNull(value).booleanValue())
        ));

    public static void register() {
        // registration is handled centrally by a payloads registry caller; kept for symmetry
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

