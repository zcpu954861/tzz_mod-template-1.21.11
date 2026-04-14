package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.phone.PhoneCallAdminClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * AR-specific call-admin screen. Centred action button with cooldown.
 */
public class ARCallAdminScreen extends AbstractARScreen {

    public ARCallAdminScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.call_admin"), parent);
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();

        int btnW = s(80);
        int btnH = s(24);
        int bx = contentX + (contentWidth - btnW) / 2;
        int by = contentY + contentHeight / 2 + s(10);
        addARPrimaryButton(Text.translatable("phone.tzz_mod.call_admin.send_btn"), bx, by, btnW, btnH, btn -> {
            if (!PhoneCallAdminClient.isCoolingDown()) {
                PhoneCallAdminClient.sendCall();
                PhoneCallAdminClient.startCooldown(5000L);
                if (client != null && client.player != null) {
                    client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                }
            }
        });
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // Title
        int titleCX = contentX + contentWidth / 2;
        int titleY = contentY + s(2);
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.app.call_admin"),
                titleCX, titleY, themeAccent());

        // Icon area
        int iconSize = s(40);
        int iconX = titleCX - iconSize / 2;
        int iconY = contentY + contentHeight / 2 - iconSize - s(4);

        // Draw icon texture or fallback
        var iconId = net.minecraft.util.Identifier.of("tzz_mod", "textures/gui/phone/icons/call_admin.png");
        if (hasResource(iconId)) {
            context.drawTexturedQuad(iconId, iconX, iconY,
                    iconX + iconSize, iconY + iconSize, 0.0F, 1.0F, 0.0F, 1.0F);
        } else {
            drawScaledCenteredText(context, Text.literal("📞"),
                    titleCX, iconY + iconSize / 2, themeAccent());
        }

        // Cooldown overlay
        if (PhoneCallAdminClient.isCoolingDown()) {
            int remaining = PhoneCallAdminClient.getRemainingSeconds();
            String cdText = Text.translatable("phone.tzz_mod.call_admin.cooldown", remaining).getString();
            int cdW = scaledTextWidth(cdText) + s(8);
            int cdH = scaledFontHeight() + s(4);
            int cdX = titleCX - cdW / 2;
            int cdY = iconY + iconSize + s(4);
            drawAngularTechFrame(context, cdX, cdY, cdW, cdH,
                    Math.max(1, s(2)),
                    isLightMode() ? 0xCC_E0E8F0 : 0xCC_081018,
                    0xAAFF4444);
            drawScaledCenteredText(context, Text.literal(cdText), titleCX, cdY + s(2), 0xFFFF6666);
        }
    }
}
