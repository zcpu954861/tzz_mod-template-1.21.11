package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.util.NullSafety;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;

public record WebAdminTestBridgeGuiC2SPayload(
        String requestId,
        String nonce,
        String operation,
        String bodyJson
) implements CustomPayload {
    public static final CustomPayload.@NonNull Id<WebAdminTestBridgeGuiC2SPayload> ID =
            NullSafety.requireNonNull(new Id<>(Identifier.of(Tzz_mod.MOD_ID, "webadmin_testbridge_gui_c2s")));

    public static final @NonNull PacketCodec<RegistryByteBuf, WebAdminTestBridgeGuiC2SPayload> CODEC =
            NullSafety.requireNonNull(PacketCodec.tuple(
                    PacketCodecs.STRING,
                    WebAdminTestBridgeGuiC2SPayload::requestId,
                    PacketCodecs.STRING,
                    WebAdminTestBridgeGuiC2SPayload::nonce,
                    PacketCodecs.STRING,
                    WebAdminTestBridgeGuiC2SPayload::operation,
                    PacketCodecs.STRING,
                    WebAdminTestBridgeGuiC2SPayload::bodyJson,
                    WebAdminTestBridgeGuiC2SPayload::new
            ));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
