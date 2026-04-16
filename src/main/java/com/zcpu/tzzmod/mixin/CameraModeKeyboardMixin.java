package com.zcpu.tzzmod.mixin;

import com.zcpu.tzzmod.client.photo.CameraModeClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Keyboard.class)
public class CameraModeKeyboardMixin {

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void tzz_handleCameraModeKey(long window, int action, KeyInput input, CallbackInfo ci) {
        if (CameraModeClient.handleKey(MinecraftClient.getInstance(), action, input)) {
            ci.cancel();
        }
    }
}