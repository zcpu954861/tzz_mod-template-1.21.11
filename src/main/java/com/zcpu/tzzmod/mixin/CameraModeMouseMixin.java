package com.zcpu.tzzmod.mixin;

import com.zcpu.tzzmod.client.photo.CameraModeClient;
import com.zcpu.tzzmod.client.webadmin.WebAdminSelectionClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Mouse.class)
public class CameraModeMouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void tzz_blockCameraMouseButtons(long window, MouseInput input, int action, CallbackInfo ci) {
        Click click = new Click(0.0D, 0.0D, input);
        if (action == 1 && WebAdminSelectionClient.shouldConsumeMouseClick(click)) {
            ci.cancel();
            return;
        }
        if (action == 1 && CameraModeClient.shouldConsumeMouseClick(click)) {
            ci.cancel();
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void tzz_blockCameraMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (WebAdminSelectionClient.shouldConsumeMouseScroll()) {
            ci.cancel();
            return;
        }
        if (CameraModeClient.shouldConsumeMouseScroll()) {
            ci.cancel();
        }
    }
}
