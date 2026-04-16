package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;

public record AdminModePayload(boolean recording, boolean galleryEnabled) implements CustomPayload {
    public static final CustomPayload.@NonNull Id<AdminModePayload> ID =
        NullSafety.requireNonNull(new CustomPayload.Id<>(Identifier.of(Tzz_mod.MOD_ID, "admin_mode_s2c")));

    public static final @NonNull PacketCodec<RegistryByteBuf, AdminModePayload> CODEC =
        NullSafety.requireNonNull(PacketCodec.tuple(
            PacketCodecs.BOOLEAN,
            AdminModePayload::recording,
            PacketCodecs.BOOLEAN,
            AdminModePayload::galleryEnabled,
            AdminModePayload::new
        ));

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

