package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PhoneChatC2SPayload(String action, String bodyJson) implements CustomPayload {
    public static final Id<PhoneChatC2SPayload> ID =
            new Id<>(Identifier.of(Tzz_mod.MOD_ID, "phone_chat_c2s"));

    public static final PacketCodec<RegistryByteBuf, PhoneChatC2SPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            PhoneChatC2SPayload::action,
            PacketCodecs.STRING,
            PhoneChatC2SPayload::bodyJson,
            PhoneChatC2SPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

