package com.zcpu.tzzmod.client;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.ar.ARClientAccess;
import com.zcpu.tzzmod.blocking.BlockingCardConfiguratorClientAccess;
import com.zcpu.tzzmod.blocking.BlockingCardConfiguratorState;
import com.zcpu.tzzmod.client.ar.ui.ARHomeScreen;
import com.zcpu.tzzmod.client.blocking.BlockingCardClient;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatHudOverlay;
import com.zcpu.tzzmod.client.phone.ui.AlertSubtitleOverlay;
import com.zcpu.tzzmod.client.phone.ui.PhoneLockScreen;
import com.zcpu.tzzmod.client.phone.ui.app.BlockingCardConfiguratorScreen;
import com.zcpu.tzzmod.client.phone.ui.app.MapMarkerListScreen;
import com.zcpu.tzzmod.client.phone.ui.RegionTitleOverlay;
import com.zcpu.tzzmod.client.phone.ui.app.RegionPlannerListScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PasswordCardScreen;
import com.zcpu.tzzmod.client.phone.ui.app.PasswordMachineScreen;
import com.zcpu.tzzmod.client.phone.ui.app.TaskConfiguratorScreen;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.client.map.MapHighlightRenderer;
import com.zcpu.tzzmod.client.password.PasswordClient;
import com.zcpu.tzzmod.client.photo.CameraModeClient;
import com.zcpu.tzzmod.client.task.TaskClient;
import com.zcpu.tzzmod.client.task.TaskHudOverlay;
import com.zcpu.tzzmod.ModItem.ModItems;
import com.zcpu.tzzmod.ModItem.custom.PasswordConfigCardItem;
import com.zcpu.tzzmod.map.MapMarkerClientAccess;
import com.zcpu.tzzmod.map.RegionPlannerClientAccess;
import com.zcpu.tzzmod.phone.PhoneClientAccess;
import com.zcpu.tzzmod.password.PasswordCardClientAccess;
import com.zcpu.tzzmod.password.PasswordMachineClientAccess;
import com.zcpu.tzzmod.task.TaskConfiguratorClientAccess;
import com.zcpu.tzzmod.util.NullSafety;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

public class Tzz_modClient implements ClientModInitializer {
    private static final @NonNull Identifier MAIN_HUD_LAYER_ID =
            NullSafety.requireNonNull(Identifier.of(Tzz_mod.MOD_ID, "main_hud_overlay"));

    private static KeyBinding arHeadsetKey;

    @Override
    public void onInitializeClient() {
        Tzz_mod.LOGGER.info("Client initializer loaded.");

        // AR Headset keybinding (default: V)
        arHeadsetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tzz_mod.ar_headset",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KeyBinding.Category.MISC
        ));

        ARClientAccess.setOpener(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.setScreen(new ARHomeScreen());
                }
            } catch (Exception exception) {
                Tzz_mod.LOGGER.error("Failed to open AR headset screen", exception);
            }
        });

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
        BlockingCardConfiguratorClientAccess.setOpener(hand -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                var player = client.player;
                if (player == null) {
                    return;
                }
                var stack = player.getStackInHand(hand);
                client.setScreen(new BlockingCardConfiguratorScreen(client.currentScreen, hand, BlockingCardConfiguratorState.read(stack)));
            } catch (Exception exception) {
                Tzz_mod.LOGGER.error("Failed to open blocking card configurator", exception);
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
                var player = client.player;
                if (player == null) {
                    return;
                }
                var stack = player.getStackInHand(hand);
                String initialCode = PasswordConfigCardItem.hasConfiguredPassword(stack)
                        ? PasswordConfigCardItem.getStoredPassword(stack)
                        : "";
                client.setScreen(new PasswordCardScreen(client.currentScreen, hand, initialCode));
            } catch (Exception exception) {
                Tzz_mod.LOGGER.error("Failed to open password card screen", exception);
            }
        });
        MapMarkerClientAccess.setOpener(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                client.setScreen(new MapMarkerListScreen(client.currentScreen));
            } catch (Exception exception) {
                Tzz_mod.LOGGER.error("Failed to open map marker screen", exception);
            }
        });
        RegionPlannerClientAccess.setOpener(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                client.setScreen(new RegionPlannerListScreen(client.currentScreen));
            } catch (Exception exception) {
                Tzz_mod.LOGGER.error("Failed to open region planner screen", exception);
            }
        });
        PhoneSettingsClient.load();

        // register client-side death status receiver
        com.zcpu.tzzmod.client.DeathSyncClient.register();
        com.zcpu.tzzmod.client.AdminSyncClient.register();
        com.zcpu.tzzmod.client.ForcedHudClient.register();
        MapClient.register();
        MapHighlightRenderer.register();
        com.zcpu.tzzmod.client.phone.chat.PhoneChatClient.register();
        TaskClient.register();
        PasswordClient.register();
        BlockingCardClient.register();
        com.zcpu.tzzmod.client.photo.GalleryClient.register();
        com.zcpu.tzzmod.client.phone.PhoneAppsClient.register();
        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, MAIN_HUD_LAYER_ID, (context, tickCounter) -> {
            // render the player's head and ID in the top-left
            com.zcpu.tzzmod.client.PlayerHeadHudOverlay.render(context);

            PhoneChatHudOverlay.render(context);
            TaskHudOverlay.render(context);
            AlertSubtitleOverlay.render(context);
            RegionTitleOverlay.render(context);
            CameraModeClient.render(context);
        });

        // AR headset keybind tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CameraModeClient.tick(client);
            KeyBinding headsetKey = arHeadsetKey;
            if (headsetKey == null) {
                return;
            }
            while (headsetKey.wasPressed()) {
                var player = client.player;
                if (player != null && client.currentScreen == null) {
                    boolean hasHeadset =
                            player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.AR_HEADSET)
                            || player.getStackInHand(Hand.MAIN_HAND).isOf(ModItems.AR_HEADSET)
                            || player.getStackInHand(Hand.OFF_HAND).isOf(ModItems.AR_HEADSET);
                    if (hasHeadset) {
                        ARClientAccess.openARScreen();
                    }
                }
            }
        });
    }
}
