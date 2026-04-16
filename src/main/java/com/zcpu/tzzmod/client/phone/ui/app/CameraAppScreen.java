package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.photo.CameraModeClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Phone Camera APP entry screen.
 * Immediately opens the full-screen camera mode overlay.
 * The camera mode is identical for phone and AR (requirement #3).
 */
public class CameraAppScreen extends AbstractPhoneScreen {

    public CameraAppScreen(Screen parent) {
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
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // This screen is never actually visible — it immediately opens camera mode
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.camera.opening"),
                contentX + contentWidth / 2, contentY + contentHeight / 2);
    }
}
