package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.util.NullSafety;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;

public record MapS2CPayload(String action, String bodyJson) implements CustomPayload {
    public static final CustomPayload.@NonNull Id<MapS2CPayload> ID =
            NullSafety.requireNonNull(new Id<>(Identifier.of(Tzz_mod.MOD_ID, "map_s2c")));

    public static final @NonNull PacketCodec<RegistryByteBuf, MapS2CPayload> CODEC =
            NullSafety.requireNonNull(PacketCodec.tuple(
                    PacketCodecs.STRING,
                    MapS2CPayload::action,
                    PacketCodecs.STRING,
                    MapS2CPayload::bodyJson,
                    MapS2CPayload::new
            ));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}