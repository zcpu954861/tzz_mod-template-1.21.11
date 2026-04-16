package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.photo.CameraModeClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * AR Camera APP. Immediately opens full-screen camera mode.
 * Camera mode is identical for phone and AR (requirement #3).
 */
public class ARCameraScreen extends AbstractARScreen {

    public ARCameraScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.camera"), parent);
    }

    @Override
    protected void init() {
        super.init();
        if (client != null) {
            CameraModeClient.activate(client);
            client.setScreen(null);
        }
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // Never actually visible — immediately opens camera mode
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.camera.opening"),
                contentX + contentWidth / 2, contentY + contentHeight / 2, themeTextDim());
    }
}
