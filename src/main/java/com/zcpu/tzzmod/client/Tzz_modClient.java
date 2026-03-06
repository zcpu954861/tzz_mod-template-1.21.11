package com.zcpu.tzzmod.client;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatHudOverlay;
import com.zcpu.tzzmod.client.phone.ui.AlertSubtitleOverlay;
import com.zcpu.tzzmod.client.phone.ui.PhoneLockScreen;
import com.zcpu.tzzmod.client.phone.ui.app.TaskConfiguratorScreen;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import com.zcpu.tzzmod.client.task.TaskClient;
import com.zcpu.tzzmod.client.task.TaskHudOverlay;
import com.zcpu.tzzmod.phone.PhoneClientAccess;
import com.zcpu.tzzmod.task.TaskConfiguratorClientAccess;
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
                    client.setScreen(new PhoneLockScreen());
                }
            } catch (Exception exception) {
                Tzz_mod.LOGGER.error("Failed to open phone screen", exception);
            }
        });
        TaskConfiguratorClientAccess.setOpener(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                client.setScreen(new TaskConfiguratorScreen(client.currentScreen));
            } catch (Exception exception) {
                Tzz_mod.LOGGER.error("Failed to open task configurator", exception);
            }
        });
        PhoneSettingsClient.load();

        // register client-side death status receiver
        com.zcpu.tzzmod.client.DeathSyncClient.register();
        com.zcpu.tzzmod.client.phone.chat.PhoneChatClient.register();
        TaskClient.register();
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            PhoneChatHudOverlay.render(context);
            TaskHudOverlay.render(context);
            AlertSubtitleOverlay.render(context);
        });
    }
}
