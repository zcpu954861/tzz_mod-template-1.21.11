package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PasswordC2SPayload(String action, String bodyJson) implements CustomPayload {
    public static final Id<PasswordC2SPayload> ID =
            new Id<>(Identifier.of(Tzz_mod.MOD_ID, "password_c2s"));

    public static final PacketCodec<RegistryByteBuf, PasswordC2SPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            PasswordC2SPayload::action,
            PacketCodecs.STRING,
            PasswordC2SPayload::bodyJson,
            PasswordC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

