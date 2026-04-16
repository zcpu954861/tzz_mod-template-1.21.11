package com.zcpu.tzzmod.mixin;

import com.zcpu.tzzmod.client.photo.CameraModeScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin on MinecraftClient to support camera mode overlay.
 * <p>
 * When setScreen() is called with a CameraModeScreen, MC normally unlocks the
 * cursor (because a Screen is open). This mixin re-locks the cursor at the
 * TAIL of setScreen, so the player can still look around with the mouse.
 */
@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public class CameraModeMixin {

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void tzz_lockCursorForCamera(Screen screen, CallbackInfo ci) {
        if (screen instanceof CameraModeScreen) {
            ((MinecraftClient) (Object) this).mouse.lockCursor();
        }
    }
}
