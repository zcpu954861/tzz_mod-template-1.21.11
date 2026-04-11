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

public record DeathStatusPayload(boolean hasDeath) implements CustomPayload {
    public static final CustomPayload.@NonNull Id<DeathStatusPayload> ID =
        NullSafety.requireNonNull(new CustomPayload.Id<>(Identifier.of(Tzz_mod.MOD_ID, "death_status")));

    public static final @NonNull PacketCodec<RegistryByteBuf, DeathStatusPayload> CODEC =
        NullSafety.requireNonNull(PacketCodec.tuple(
            PacketCodecs.BOOLEAN,
            DeathStatusPayload::hasDeath,
            value -> new DeathStatusPayload(NullSafety.requireNonNull(value).booleanValue())
        ));

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
