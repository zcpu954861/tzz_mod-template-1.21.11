package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.phone.ui.RoundedRectRenderer;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PhoneSettingsAppScreen extends AbstractPhoneScreen {
    // animation state for the rounded toggle
    private float knobProgress = 0f; // 0.0 = off (left), 1.0 = on (right)
    private boolean knobTarget = false;
    private static final float TOGGLE_ANIM_SPEED = 6.0f; // progress units per second

    public PhoneSettingsAppScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.settings"), parent);
        PhoneSettingsClient.load();
    }

    @Override
    protected void init() {
        super.init();
        // Leave extra vertical space so the app title above is clearly visible
        int topStart = contentY + s(40);
        int rowHeight = s(22);

        // Layout for labeled row with a capsule-style toggle on the right
        int rowX = contentX + s(8);
        int rowY = topStart;
        int rowW = contentWidth - s(16);

        // Invisible button covers the entire row so clicks toggle the setting
        ButtonWidget toggleButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            boolean next = !PhoneSettingsClient.isAlertModeEnabled();
            PhoneSettingsClient.setAlertModeEnabled(next);
            knobTarget = next; // animate towards the new target
        }).dimensions(rowX, rowY, rowW, rowHeight).build());
        toggleButton.setAlpha(0.0F);

        // initialize animation progress from current saved state
        knobTarget = PhoneSettingsClient.isAlertModeEnabled();
        knobProgress = knobTarget ? 1.0f : 0.0f;

        // Back button at bottom
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(70), s(20), button -> close());
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // Title
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.settings"), contentX + contentWidth / 2, contentY + s(8));

        int topStart = contentY + s(40);
        int rowHeight = s(22);
        int rowX = contentX + s(8);
        int rowY = topStart;
        int rowW = contentWidth - s(16);

        // Draw label at left
        Text label = Text.translatable("phone.tzz_mod.settings.alert_mode");
        int labelY = rowY + Math.max(0, (rowHeight - textRenderer.fontHeight) / 2);
        context.drawTextWithShadow(textRenderer, label, rowX + s(6), labelY, 0xFFECECEC);

        // Draw animated capsule toggle at right
        int switchW = s(36);
        int switchH = s(14);
        int switchX = rowX + rowW - switchW - s(6);
        int switchY = rowY + Math.max(0, (rowHeight - switchH) / 2);

        // animate knobProgress towards target
        float dir = knobTarget ? 1.0f : -1.0f;
        knobProgress += dir * TOGGLE_ANIM_SPEED * delta;
        if (knobProgress < 0f) knobProgress = 0f;
        if (knobProgress > 1f) knobProgress = 1f;

        // colors for off/on states
        int offColor = 0x55333333;
        int onColor = 0xFF3FC47F;
        int capsuleColor = lerpColor(offColor, onColor, knobProgress);
        int knobColor = PhoneSettingsClient.isAlertModeEnabled() ? 0xFFFFFFFF : 0xFFCCCCCC;

        // draw rounded capsule background
        RoundedRectRenderer.fillRoundedRect(context, switchX, switchY, switchW, switchH, switchH / 2, capsuleColor);

        // Knob (rounded) - interpolate center X
        int knobRadius = Math.max(1, switchH - s(2));
        float leftCX = switchX + s(2) + knobRadius / 2f;
        float rightCX = switchX + switchW - s(2) - knobRadius / 2f;
        int knobCX = Math.round(lerp(leftCX, rightCX, knobProgress));
        int knobLeft = knobCX - knobRadius / 2;
        int knobTop = switchY + (switchH - knobRadius) / 2;
        RoundedRectRenderer.fillRoundedRect(context, knobLeft, knobTop, knobRadius, knobRadius, knobRadius / 2, knobColor);

        // Subtitle below row (ensure no overlap) with wrapping
        String subtitleStr = Text.translatable("phone.tzz_mod.settings.alert_mode.subtitle").getString();
        int maxWidth = rowW - s(12);
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < subtitleStr.length(); ) {
            int cp = subtitleStr.codePointAt(i);
            int charLen = Character.charCount(cp);
            cur.appendCodePoint(cp);
            if (textRenderer.getWidth(cur.toString()) > maxWidth) {
                // remove last appended char and push line
                cur.setLength(cur.length() - charLen);
                if (cur.isEmpty()) {
                    // single char too wide; force include
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

    // linear interpolation helper
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0f, Math.min(1f, t));
    }

    // color lerp for ARGB ints
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
