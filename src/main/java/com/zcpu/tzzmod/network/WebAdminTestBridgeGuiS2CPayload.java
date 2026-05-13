package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.util.NullSafety;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;

public record WebAdminTestBridgeGuiS2CPayload(
        String requestId,
        String nonce,
        String operation,
        String bodyJson
) implements CustomPayload {
    public static final CustomPayload.@NonNull Id<WebAdminTestBridgeGuiS2CPayload> ID =
            NullSafety.requireNonNull(new Id<>(Identifier.of(Tzz_mod.MOD_ID, "webadmin_testbridge_gui_s2c")));

    public static final @NonNull PacketCodec<RegistryByteBuf, WebAdminTestBridgeGuiS2CPayload> CODEC =
            NullSafety.requireNonNull(PacketCodec.tuple(
                    PacketCodecs.STRING,
                    WebAdminTestBridgeGuiS2CPayload::requestId,
                    PacketCodecs.STRING,
                    WebAdminTestBridgeGuiS2CPayload::nonce,
                    PacketCodecs.STRING,
                    WebAdminTestBridgeGuiS2CPayload::operation,
                    PacketCodecs.STRING,
                    WebAdminTestBridgeGuiS2CPayload::bodyJson,
                    WebAdminTestBridgeGuiS2CPayload::new
            ));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
