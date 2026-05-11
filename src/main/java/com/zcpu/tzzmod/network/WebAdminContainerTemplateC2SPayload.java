package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.util.NullSafety;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;

public record WebAdminContainerTemplateC2SPayload(String action, String bodyJson) implements CustomPayload {
    public static final CustomPayload.@NonNull Id<WebAdminContainerTemplateC2SPayload> ID =
            NullSafety.requireNonNull(new Id<>(Identifier.of(Tzz_mod.MOD_ID, "webadmin_container_template_c2s")));

    public static final @NonNull PacketCodec<RegistryByteBuf, WebAdminContainerTemplateC2SPayload> CODEC =
            NullSafety.requireNonNull(PacketCodec.tuple(
                    PacketCodecs.STRING,
                    WebAdminContainerTemplateC2SPayload::action,
                    PacketCodecs.STRING,
                    WebAdminContainerTemplateC2SPayload::bodyJson,
                    WebAdminContainerTemplateC2SPayload::new
            ));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
