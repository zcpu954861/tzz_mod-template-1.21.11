package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.phone.ui.RoundedRectRenderer;
import com.zcpu.tzzmod.client.ForcedHudClient;
import com.zcpu.tzzmod.network.AdminModeC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class PhoneAdminAppScreen extends AbstractPhoneScreen {
    private float knobProgress = 0f;
    private boolean knobTarget = false;
    private static final float TOGGLE_ANIM_SPEED = 6.0f;

    public PhoneAdminAppScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.admin"), parent);
    }

    @Override
    protected void init() {
        super.init();
        int topStart = contentY + s(40);
        int rowHeight = s(22);
        int rowX = contentX + s(8);
        int rowY = topStart;
        int rowW = contentWidth - s(16);

        ButtonWidget toggleButton = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            boolean next = !knobTarget;
            // send to server
            ClientPlayNetworking.send(new AdminModeC2SPayload(next));
            // immediately reflect locally (helpful for singleplayer and reduce perceived lag)
            ForcedHudClient.setServerEnforcedHud(next);
            knobTarget = next; // animate UI immediately; server will enforce permissions
        }).dimensions(rowX, rowY, rowW, rowHeight).build());
        toggleButton.setAlpha(0.0f);

        // initialize from client-side forced flag
        knobTarget = ForcedHudClient.isForceShowHead();
        knobProgress = knobTarget ? 1.0f : 0.0f;
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.admin"), contentX + contentWidth / 2, contentY + s(8));

        int topStart = contentY + s(40);
        int rowHeight = s(22);
        int rowX = contentX + s(8);
        int rowY = topStart;
        int rowW = contentWidth - s(16);

        Text label = Text.translatable("phone.tzz_mod.admin.record_mode");
        int labelY = rowY + Math.max(0, (rowHeight - textRenderer.fontHeight) / 2);
        context.drawTextWithShadow(textRenderer, label, rowX + s(6), labelY, 0xFFECECEC);

        int switchW = s(36);
        int switchH = s(14);
        int switchX = rowX + rowW - switchW - s(6);
        int switchY = rowY + Math.max(0, (rowHeight - switchH) / 2);

        float dir = knobTarget ? 1.0f : -1.0f;
        knobProgress += dir * TOGGLE_ANIM_SPEED * delta;
        if (knobProgress < 0f) knobProgress = 0f;
        if (knobProgress > 1f) knobProgress = 1f;

        int offColor = 0x55333333;
        int onColor = 0xFF3FC47F;
        int capsuleColor = lerpColor(offColor, onColor, knobProgress);
        int knobColor = ForcedHudClient.isForceShowHead() ? 0xFFFFFFFF : 0xFFCCCCCC;

        RoundedRectRenderer.fillRoundedRect(context, switchX, switchY, switchW, switchH, switchH / 2, capsuleColor);

        int knobRadius = Math.max(1, switchH - s(2));
        float leftCX = switchX + s(2) + knobRadius / 2f;
        float rightCX = switchX + switchW - s(2) - knobRadius / 2f;
        int knobCX = Math.round(lerp(leftCX, rightCX, knobProgress));
        int knobLeft = knobCX - knobRadius / 2;
        int knobTop = switchY + (switchH - knobRadius) / 2;
        RoundedRectRenderer.fillRoundedRect(context, knobLeft, knobTop, knobRadius, knobRadius, knobRadius / 2, knobColor);

        String subtitleStr = Text.translatable("phone.tzz_mod.admin.record_mode.subtitle").getString();
        int maxWidth = rowW - s(12);
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < subtitleStr.length(); ) {
            int cp = subtitleStr.codePointAt(i);
            int charLen = Character.charCount(cp);
            cur.appendCodePoint(cp);
            if (textRenderer.getWidth(cur.toString()) > maxWidth) {
                cur.setLength(cur.length() - charLen);
                if (cur.isEmpty()) {
                    cur.appendCodePoint(cp);
                    i += charLen;
                }
                lines.add(cur.toString());
                cur = new StringBuilder();
                continue;
            }
            i += charLen;
        }
        if (!cur.isEmpty()) lines.add(cur.toString());

        int subStartY = rowY + rowHeight + s(6);
        for (int li = 0; li < lines.size(); li++) {
            String line = lines.get(li);
            context.drawTextWithShadow(textRenderer, Text.literal(line), rowX + s(6), subStartY + li * (textRenderer.fontHeight + s(2)), 0xFFB8C7D4);
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0f, Math.min(1f, t));
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >> 24) & 0xFF;
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;

        int ba = (b >> 24) & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;

        int ra = Math.round(aa + (ba - aa) * t);
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);

        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }
}

