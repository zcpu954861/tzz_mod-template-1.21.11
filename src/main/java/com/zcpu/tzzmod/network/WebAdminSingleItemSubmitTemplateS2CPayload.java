package com.zcpu.tzzmod.network;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.util.NullSafety;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;

public record WebAdminSingleItemSubmitTemplateS2CPayload(String action, String bodyJson) implements CustomPayload {
    public static final CustomPayload.@NonNull Id<WebAdminSingleItemSubmitTemplateS2CPayload> ID =
            NullSafety.requireNonNull(new Id<>(Identifier.of(Tzz_mod.MOD_ID, "webadmin_single_item_submit_template_s2c")));

    public static final @NonNull PacketCodec<RegistryByteBuf, WebAdminSingleItemSubmitTemplateS2CPayload> CODEC =
            NullSafety.requireNonNull(PacketCodec.tuple(
                    PacketCodecs.STRING,
                    WebAdminSingleItemSubmitTemplateS2CPayload::action,
                    PacketCodecs.STRING,
                    WebAdminSingleItemSubmitTemplateS2CPayload::bodyJson,
                    WebAdminSingleItemSubmitTemplateS2CPayload::new
            ));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
