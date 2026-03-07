package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AdminModeC2SPayload(boolean recording) implements CustomPayload {
    public static final CustomPayload.Id<AdminModeC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of(Tzz_mod.MOD_ID, "admin_mode_c2s"));

    public static final PacketCodec<RegistryByteBuf, AdminModeC2SPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.BOOLEAN, AdminModeC2SPayload::recording, AdminModeC2SPayload::new);

    public static void register() {
        // registration is handled centrally by a payloads registry caller; kept for symmetry
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

