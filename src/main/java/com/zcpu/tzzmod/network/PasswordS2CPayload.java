package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PasswordS2CPayload(String action, String bodyJson) implements CustomPayload {
    public static final Id<PasswordS2CPayload> ID =
            new Id<>(Identifier.of(Tzz_mod.MOD_ID, "password_s2c"));

    public static final PacketCodec<RegistryByteBuf, PasswordS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            PasswordS2CPayload::action,
            PacketCodecs.STRING,
            PasswordS2CPayload::bodyJson,
            PasswordS2CPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

