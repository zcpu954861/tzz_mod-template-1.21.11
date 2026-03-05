package com.zcpu.tzzmod.client;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatHudOverlay;
import com.zcpu.tzzmod.client.phone.ui.PhoneHomeScreen;
import com.zcpu.tzzmod.phone.PhoneClientAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public class Tzz_modClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Tzz_mod.LOGGER.info("Client initializer loaded.");
        PhoneClientAccess.setOpener(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.setScreen(new PhoneHomeScreen());
                }
            } catch (Exception exception) {
                Tzz_mod.LOGGER.error("Failed to open phone screen", exception);
            }
        });

        // register client-side death status receiver
        com.zcpu.tzzmod.client.DeathSyncClient.register();
        com.zcpu.tzzmod.client.phone.chat.PhoneChatClient.register();
        HudRenderCallback.EVENT.register((context, tickCounter) -> PhoneChatHudOverlay.render(context));
    }
}
