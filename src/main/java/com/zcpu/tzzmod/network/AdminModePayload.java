package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AdminModePayload(boolean recording) implements CustomPayload {
    public static final CustomPayload.Id<AdminModePayload> ID =
            new CustomPayload.Id<>(Identifier.of(Tzz_mod.MOD_ID, "admin_mode_s2c"));

    public static final PacketCodec<RegistryByteBuf, AdminModePayload> CODEC =
            PacketCodec.tuple(PacketCodecs.BOOLEAN, AdminModePayload::recording, AdminModePayload::new);

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

