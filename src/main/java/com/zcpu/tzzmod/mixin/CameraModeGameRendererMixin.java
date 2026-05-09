package com.zcpu.tzzmod.mixin;

import com.zcpu.tzzmod.client.photo.CameraModeClient;
import com.zcpu.tzzmod.client.webadmin.WebAdminSelectionClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class CameraModeGameRendererMixin {

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"
            )
    )
    private void tzz_renderCameraOverlayOrHud(InGameHud hud, DrawContext context, RenderTickCounter tickCounter) {
        if (!CameraModeClient.isActive()) {
            hud.render(context, tickCounter);
            WebAdminSelectionClient.render(context);
            return;
        }
        if (!CameraModeClient.captureCurrentFrame()) {
            CameraModeClient.render(context);
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderAutosaveIndicator(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"
            )
    )
    private void tzz_skipAutosaveIndicatorDuringCamera(InGameHud hud, DrawContext context, RenderTickCounter tickCounter) {
        if (!CameraModeClient.isActive()) {
            hud.renderAutosaveIndicator(context, tickCounter);
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderDebugHud(Lnet/minecraft/client/gui/DrawContext;)V"
            )
    )
    private void tzz_skipDebugHudDuringCamera(InGameHud hud, DrawContext context) {
        if (!CameraModeClient.isActive()) {
            hud.renderDebugHud(context);
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderDeferredSubtitles()V"
            )
    )
    private void tzz_skipDeferredSubtitlesDuringCamera(InGameHud hud) {
        if (!CameraModeClient.isActive()) {
            hud.renderDeferredSubtitles();
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void tzz_skipHandAndHeldItemDuringCamera(float tickProgress, boolean renderBlockOutline, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (CameraModeClient.isActive() || WebAdminSelectionClient.isActive()) {
            ci.cancel();
        }
    }
}
