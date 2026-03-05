package com.zcpu.tzzmod.client;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.client.phone.ui.PhoneHomeScreen;
import com.zcpu.tzzmod.phone.PhoneClientAccess;
import net.fabricmc.api.ClientModInitializer;
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
    }
}
