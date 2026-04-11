package com.zcpu.tzzmod.client;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatHudOverlay;
import com.zcpu.tzzmod.client.phone.ui.AlertSubtitleOverlay;
import com.zcpu.tzzmod.client.phone.ui.PhoneLockScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PasswordCardScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PasswordMachineScreen;
import com.zcpu.tzzmod.client.phone.ui.app.TaskConfiguratorScreen;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import com.zcpu.tzzmod.client.password.PasswordClient;
import com.zcpu.tzzmod.client.task.TaskClient;
import com.zcpu.tzzmod.client.task.TaskHudOverlay;
import com.zcpu.tzzmod.ModItem.custom.PasswordConfigCardItem;
import com.zcpu.tzzmod.phone.PhoneClientAccess;
import com.zcpu.tzzmod.password.PasswordCardClientAccess;
import com.zcpu.tzzmod.password.PasswordMachineClientAccess;
import com.zcpu.tzzmod.task.TaskConfiguratorClientAccess;
import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;

public class Tzz_modClient implements ClientModInitializer {
    private static final @NonNull Identifier MAIN_HUD_LAYER_ID =
            NullSafety.requireNonNull(Identifier.of(Tzz_mod.MOD_ID, "main_hud_overlay"));

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
        PasswordMachineClientAccess.setOpener(pos -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                client.setScreen(new PasswordMachineScreen(client.currentScreen, pos));
            } catch (Exception exception) {
                Tzz_mod.LOGGER.error("Failed to open password machine screen", exception);
            }
        });
        PasswordCardClientAccess.setOpener(hand -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player == null) {
                    return;
                }
                var stack = client.player.getStackInHand(hand);
                String initialCode = PasswordConfigCardItem.hasConfiguredPassword(stack)
                        ? PasswordConfigCardItem.getStoredPassword(stack)
                        : "";
                client.setScreen(new PasswordCardScreen(client.currentScreen, hand, initialCode));
            } catch (Exception exception) {
                Tzz_mod.LOGGER.error("Failed to open password card screen", exception);
            }
        });
        PhoneSettingsClient.load();

        // register client-side death status receiver
        com.zcpu.tzzmod.client.DeathSyncClient.register();
        com.zcpu.tzzmod.client.AdminSyncClient.register();
        com.zcpu.tzzmod.client.ForcedHudClient.register();
        com.zcpu.tzzmod.client.phone.chat.PhoneChatClient.register();
        TaskClient.register();
        PasswordClient.register();
        com.zcpu.tzzmod.client.phone.PhoneAppsClient.register();
        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, MAIN_HUD_LAYER_ID, (context, tickCounter) -> {
            // render the player's head and ID in the top-left
            com.zcpu.tzzmod.client.PlayerHeadHudOverlay.render(context);

            PhoneChatHudOverlay.render(context);
            TaskHudOverlay.render(context);
            AlertSubtitleOverlay.render(context);
        });
    }
}
