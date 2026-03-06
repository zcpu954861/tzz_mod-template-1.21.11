package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.client.phone.PhoneCallAdminClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.phone.ui.RoundedRectRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PhoneCallAdminScreen extends AbstractPhoneScreen {
    // remove per-screen cooldown fields (we'll rely on client helper)
    private ButtonWidget callButton;

    private static final Identifier ICON = Identifier.of(Tzz_mod.MOD_ID, "textures/gui/phone/icons/call_admin.png");

    public PhoneCallAdminScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.call_admin"), parent);
    }

    @Override
    protected void init() {
        super.init();

        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(72), s(20), button -> close());

        // compute a centered, slightly-lower icon area
        int iconSize = Math.min(s(72), Math.max(s(24), contentWidth - s(24)));
        int iconX = contentX + (contentWidth - iconSize) / 2;
        int iconY = contentY + (int)(contentHeight * 0.55f) - iconSize / 2;

        // create a transparent button overlaying the icon area
        callButton = addPhoneGhostButton(Text.empty(), iconX, iconY, iconSize, iconSize, button -> onCallPressed());
        callButton.active = !PhoneCallAdminClient.isCoolingDown();
    }

    private void onCallPressed() {
        if (PhoneCallAdminClient.isCoolingDown()) {
            // still cooling down, do nothing
            return;
        }

        // immediate client feedback
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        }

        // send request to server
        PhoneCallAdminClient.sendCall();

        // start client-side cooldown for 5s so UI state persists across screens
        PhoneCallAdminClient.startCooldown(5000L);

        if (callButton != null) callButton.active = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // update visual button state based on client cooldown
        if (callButton != null) callButton.active = !PhoneCallAdminClient.isCoolingDown();

        // Redraw the app icon and badge on top of buttons so the icon is never obscured by
        // the button's styled rendering (ghost hover/focus masks). This ensures when the
        // send call button is pressed the icon remains visually on top.
        int iconSize = Math.min(s(72), Math.max(s(24), contentWidth - s(24)));
        int iconX = contentX + (contentWidth - iconSize) / 2;
        int iconY = contentY + (int)(contentHeight * 0.55f) - iconSize / 2;

        if (hasResource(ICON)) {
            context.drawTexturedQuad(ICON, iconX, iconY, iconX + iconSize, iconY + iconSize,
                    0.0F, 1.0F, 0.0F, 1.0F);
        } else {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.app.call_admin"), iconX + iconSize / 2, iconY + iconSize / 2 - s(4), 0xFF1A1A1A);
        }

        // If cooling down, overlay a small lock badge on the app icon on top layer
        if (PhoneCallAdminClient.isCoolingDown()) {
            int badgeW = s(18);
            int badgeX = iconX + iconSize - badgeW - s(2);
            int badgeY = iconY + iconSize - badgeW - s(2);
            RoundedRectRenderer.fillRoundedRect(context, badgeX, badgeY, badgeW, badgeW, s(4), 0xCC000000);
            int lockW = badgeW - s(8);
            int lockH = badgeW - s(10);
            int lx = badgeX + (badgeW - lockW) / 2;
            int ly = badgeY + (badgeW - lockH) / 2 + s(1);
            context.fill(lx, ly, lx + lockW, ly + lockH, 0xFFFFFFFF);
            context.fill(lx + s(2), ly - s(4), lx + lockW - s(2), ly, 0xFFFFFFFF);
        }
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // Title and hint near the top
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.call_admin"), contentX + contentWidth / 2, contentY + s(8));

        // Draw the app icon centered and slightly lower (use same visual size as home screen icons)
        int iconSize = Math.min(s(72), Math.max(s(24), contentWidth - s(24)));
        int iconX = contentX + (contentWidth - iconSize) / 2;
        int iconY = contentY + (int)(contentHeight * 0.55f) - iconSize / 2;

        if (hasResource(ICON)) {
            context.drawTexturedQuad(ICON, iconX, iconY, iconX + iconSize, iconY + iconSize,
                    0.0F, 1.0F, 0.0F, 1.0F);
        } else {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.app.call_admin"), iconX + iconSize / 2, iconY + iconSize / 2 - s(4), 0xFF1A1A1A);
        }

        // If cooling down, overlay a small lock badge on the app icon so it's visually clear
        if (PhoneCallAdminClient.isCoolingDown()) {
            int badgeW = s(18);
            int badgeX = iconX + iconSize - badgeW - s(2);
            int badgeY = iconY + iconSize - badgeW - s(2);
            RoundedRectRenderer.fillRoundedRect(context, badgeX, badgeY, badgeW, badgeW, s(4), 0xCC000000);
            int lockW = badgeW - s(8);
            int lockH = badgeW - s(10);
            int lx = badgeX + (badgeW - lockW) / 2;
            int ly = badgeY + (badgeW - lockH) / 2 + s(1);
            context.fill(lx, ly, lx + lockW, ly + lockH, 0xFFFFFFFF);
            context.fill(lx + s(2), ly - s(4), lx + lockW - s(2), ly, 0xFFFFFFFF);

            // draw a cooldown text beneath icon as before
            int remaining = PhoneCallAdminClient.getRemainingSeconds();
            String cooldownText = "冷却: " + remaining + "s";
            int textW = textRenderer.getWidth(cooldownText);
            int boxW = Math.max(textW + s(8), s(48));
            int boxH = s(18);
            int boxX = contentX + (contentWidth - boxW) / 2;
            int boxY = iconY + iconSize + s(8);
            context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xAA1A1A1A);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(cooldownText), boxX + boxW / 2, boxY + (boxH - s(10)) / 2, 0xFFFFFFFF);
        }
    }
}
