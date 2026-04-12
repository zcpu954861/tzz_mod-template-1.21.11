package com.zcpu.tzzmod.client.phone.ui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class PhoneLockScreen extends AbstractPhoneScreen {
    private static final long SHELL_RISE_MS = 320L;
    private static final long SCREEN_WAKE_DELAY_MS = 180L;
    private static final long SCREEN_WAKE_MS = 260L;
    private static final float LOCK_TIME_SCALE = 2.35F;
    private static final float LOCK_DATE_SCALE = 1.1F;
    private static final DateTimeFormatter LOCK_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter LOCK_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private long openedAtMs;

    // Unlock animation configuration
    private static final long UNLOCK_ANIM_MS = 360L; // duration of unlock animation
    private boolean unlocking = false;
    private long unlockingStartedAtMs = 0L;
    private boolean unlockSwitchPerformed = false; // ensure we only switch screens once

    public PhoneLockScreen() {
        super(Text.translatable("phone.tzz_mod.lock_screen"), null);
    }

    @Override
    protected void init() {
        super.init();
        openedAtMs = System.currentTimeMillis();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (openedAtMs == 0L) {
            openedAtMs = System.currentTimeMillis();
        }

        int basePhoneY = phoneY;
        int baseContentY = contentY;
        int offsetY = Math.round((1.0F - getShellRiseProgress()) * (phoneHeight + s(40)));

        // Do NOT move the whole phone frame during unlock. Only the internal lock overlay will slide.
        phoneY = basePhoneY + offsetY;
        contentY = baseContentY + offsetY;

        renderPhoneWakeGlow(context);
        drawLineBorder(context, phoneX, phoneY, phoneWidth, phoneHeight);
        renderPhoneScreenSurface(context);

        if (getScreenWakeProgress() > 0.18F && !unlocking) {
            super.renderStatusBar(context);
        }

        renderPhoneContent(context, mouseX, mouseY, delta);

        phoneY = basePhoneY;
        contentY = baseContentY;

        // Safety: if unlocking finished but switch hasn't been performed yet, do it here.
        if (unlocking && getUnlockProgressRaw() >= 1.0F && !unlockSwitchPerformed) {
            if (client != null) {
                client.setScreen(new PhoneHomeScreen());
            }
            unlockSwitchPerformed = true;
        }
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        float screenWake = getScreenWakeProgress();
        if (screenWake <= 0.0F) {
            return;
        }

        int inset = s(4);
        int screenX = phoneX + inset;
        int screenY = phoneY + inset;
        int screenW = phoneWidth - inset * 2;
        int screenH = phoneHeight - inset * 2;
        int radius = Math.max(s(10), s(14));

        // If not unlocking, render normally (full lock UI filling the screen area)
        if (!unlocking) {
            int alpha = Math.max(0, Math.min(255, Math.round(255.0F * screenWake)));
            int centerX = contentX + contentWidth / 2;
            int upperMidY = contentY + Math.max(s(26), contentHeight / 7);

            renderScaledCenteredText(context, Text.literal(LocalTime.now().format(LOCK_TIME_FORMATTER)), centerX, upperMidY, LOCK_TIME_SCALE, withAlpha(0xFFF7FBFF, alpha));
            renderScaledCenteredText(context, Text.literal(LocalDate.now().format(LOCK_DATE_FORMATTER)), centerX, upperMidY + s(54), LOCK_DATE_SCALE, withAlpha(0xFFBFD9EE, Math.max(110, alpha - 35)));

            int lockIconCenterY = upperMidY + s(96);
            renderLockGlyph(context, centerX, lockIconCenterY, alpha);

            Text hint = Text.translatable("phone.tzz_mod.lock_screen.hint");
            context.drawCenteredTextWithShadow(textRenderer, hint, centerX, lockIconCenterY + s(20), withAlpha(0xFFCFDCEE, Math.max(96, alpha - 64)));

            renderUnlockButton(context, mouseX, mouseY, alpha, 0);
            return;
        }

        // --- unlocking: rolling-shutter effect inside the phone screen ---
        float unlockEase = getUnlockEase(); // 0 -> 1

        // Draw the 'desktop' (the content revealed beneath the lock). Keep it inside the phone inner rect.
        // Simple desktop styling: a subtle gradient-ish rectangle. You can replace this with a real snapshot if desired.
        int desktopColor = withAlpha(0xFF0F3856, Math.round(220.0F * screenWake));
        RoundedRectRenderer.fillRoundedRect(context, screenX + s(1), screenY + s(1), Math.max(1, screenW - s(2)), Math.max(1, screenH - s(2)), Math.max(1, radius - s(1)), desktopColor);

        // Compute overlay (lock panel) vertical position: it moves up by screenH * ease (rolling up)
        int overlayY = screenY - Math.round(unlockEase * screenH);

        // Compute visible intersection between overlay rect and screen rect (so we never draw outside phone screen)
        int visTop = Math.max(screenY, overlayY);
        int visBottom = Math.min(screenY + screenH, overlayY + screenH);
        int visH = Math.max(0, visBottom - visTop);

        if (visH > 0) {
            // Draw only the visible slice of the overlay to emulate the shutter.
            // If the top is clipped (overlayY < screenY), we draw a squared top to avoid double-rounded corners.
            if (visTop > overlayY) {
                // top clipped, don't round the top corners for the visible part
                // draw two-step: draw main rectangle and then rounded bottom
                RoundedRectRenderer.fillRoundedRect(context, screenX, visTop, screenW, visH, Math.max(1, s(6)), withAlpha(0xFF0A0F15, Math.round(240.0F * (1.0F - unlockEase))));
            } else {
                // fully within: draw standard rounded rect overlay using radius
                RoundedRectRenderer.fillRoundedRect(context, screenX, overlayY, screenW, screenH, radius, withAlpha(0xFF0A0F15, Math.round(240.0F * (1.0F - unlockEase))));
            }

            // Draw UI elements only when their computed Y falls within [screenY, screenY+screenH]
            int centerX = contentX + contentWidth / 2;
            int upperMidY = contentY + Math.max(s(26), contentHeight / 7);
            // compute how much the overlay has moved relative to content's original positions
            int dy = overlayY - screenY; // negative or zero

            int timeY = upperMidY + dy;
            int dateY = upperMidY + s(54) + dy;
            int lockY = upperMidY + s(96) + dy;
            int hintY = lockY + s(20);
            int buttonY = phoneY + phoneHeight - s(54) + dy;

            int alpha = Math.max(0, Math.min(255, Math.round(255.0F * screenWake * (1.0F - unlockEase))));

            if (isYVisible(timeY, screenY, screenH)) {
                renderScaledCenteredText(context, Text.literal(LocalTime.now().format(LOCK_TIME_FORMATTER)), centerX, timeY, LOCK_TIME_SCALE, withAlpha(0xFFF7FBFF, alpha));
            }
            if (isYVisible(dateY, screenY, screenH)) {
                renderScaledCenteredText(context, Text.literal(LocalDate.now().format(LOCK_DATE_FORMATTER)), centerX, dateY, LOCK_DATE_SCALE, withAlpha(0xFFBFD9EE, Math.max(110, alpha - 35)));
            }
            if (isYVisible(lockY, screenY, screenH)) {
                renderLockGlyph(context, centerX, lockY, alpha);
            }
            if (isYVisible(hintY, screenY, screenH)) {
                Text hint = Text.translatable("phone.tzz_mod.lock_screen.hint");
                context.drawCenteredTextWithShadow(textRenderer, hint, centerX, hintY, withAlpha(0xFFCFDCEE, Math.max(96, alpha - 64)));
            }
            // Render the unlock button if visible; adjust mouse Y because the button visually moved
            if (isYVisible(buttonY + s(0), screenY, screenH)) {
                // compute a local alpha for button based on unlock progress
                int buttonAlpha = Math.max(0, Math.min(255, Math.round(alpha * 1.0F)));
                renderUnlockButton(context, mouseX, mouseY, buttonAlpha, dy);
            }
        }
    }

    private boolean isYVisible(int y, int screenY, int screenH) {
        return y >= screenY && y <= screenY + screenH;
    }

    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (isUnlockReady() && isInsideUnlockButton(click.x(), click.y())) {
            if (!areAnimationsEnabled()) {
                if (client != null) {
                    client.setScreen(new PhoneHomeScreen());
                }
                return true;
            }
            // Start unlock animation instead of immediately switching screens
            if (!unlocking) {
                unlocking = true;
                unlockingStartedAtMs = System.currentTimeMillis();
                unlockSwitchPerformed = false;
            }
            return true;
        }
        return super.mouseClicked(click, doubleClick);
    }

    private void renderPhoneWakeGlow(DrawContext context) {
        float wake = getScreenWakeProgress();
        if (wake <= 0.0F) {
            return;
        }

        int glowX = phoneX - s(8);
        int glowY = phoneY + s(18);
        int glowW = phoneWidth + s(16);
        int glowH = phoneHeight - s(8);
        int glowRadius = Math.max(s(12), s(18));
        RoundedRectRenderer.fillRoundedRect(context, glowX, glowY, glowW, glowH, glowRadius, withAlpha(0xFF46A8FF, Math.round(38.0F * wake)));
    }

    private void renderPhoneScreenSurface(DrawContext context) {
        int inset = s(4);
        int screenX = phoneX + inset;
        int screenY = phoneY + inset;
        int screenW = phoneWidth - inset * 2;
        int screenH = phoneHeight - inset * 2;
        int radius = Math.max(s(10), s(14));
        float wake = getScreenWakeProgress();

        RoundedRectRenderer.fillRoundedRect(context, screenX, screenY, screenW, screenH, radius, 0xE00A1017);
        RoundedRectRenderer.fillRoundedRect(context, screenX + s(1), screenY + s(1), Math.max(1, screenW - s(2)), Math.max(1, screenH - s(2)), Math.max(1, radius - s(1)), withAlpha(0xFF101C28, 200));

        if (wake > 0.0F) {
            RoundedRectRenderer.fillRoundedRect(context, screenX + s(2), screenY + s(2), Math.max(1, screenW - s(4)), Math.max(1, screenH - s(4)), Math.max(1, radius - s(2)), withAlpha(0xFF274763, Math.round(70.0F * wake)));
            RoundedRectRenderer.fillRoundedRect(context, screenX + s(3), screenY + s(3), Math.max(1, screenW - s(6)), Math.max(1, screenH - s(6)), Math.max(1, radius - s(3)), withAlpha(0xFFBFE7FF, Math.round(150.0F * (1.0F - wake))));
        }
    }

    private void renderUnlockButton(DrawContext context, int mouseX, int mouseY, int alpha, int yOffset) {
        int buttonWidth = Math.min(contentWidth - s(18), s(128));
        int buttonHeight = s(28);
        int buttonX = contentX + (contentWidth - buttonWidth) / 2;
        int buttonY = phoneY + phoneHeight - s(54) + yOffset;
        int radius = Math.max(s(8), buttonHeight / 2);
        boolean hovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
        boolean ready = isUnlockReady();

        int borderColor = ready
                ? withAlpha(hovered ? 0xFFF0F9FF : 0xFFD4F1FF, alpha)
                : withAlpha(0xFF8EA4B7, Math.min(alpha, 150));
        int fillColor = ready
                ? withAlpha(hovered ? 0xFF4CBFFF : 0xFF2F96D4, Math.max(180, alpha))
                : withAlpha(0xFF203547, Math.min(alpha, 150));
        int textColor = ready
                ? withAlpha(0xFFFFFFFF, alpha)
                : withAlpha(0xFF97AABA, Math.min(alpha, 170));

        // Dim the button if we're currently playing the unlock animation
        if (unlocking) {
            fillColor = withAlpha(0xFF203547, Math.min(alpha, 120));
            borderColor = withAlpha(0xFF8EA4B7, Math.min(alpha, 120));
            textColor = withAlpha(0xFF97AABA, Math.min(alpha, 120));
        }

        RoundedRectRenderer.fillRoundedRect(context, buttonX, buttonY, buttonWidth, buttonHeight, radius, borderColor);
        RoundedRectRenderer.fillRoundedRect(context, buttonX + 1, buttonY + 1, Math.max(1, buttonWidth - 2), Math.max(1, buttonHeight - 2), Math.max(1, radius - 1), fillColor);
        RoundedRectRenderer.fillRoundedRect(context, buttonX + 2, buttonY + 2, Math.max(1, buttonWidth - 4), Math.max(1, buttonHeight / 3), Math.max(1, radius - 2), withAlpha(0xFFFFFFFF, unlocking ? 26 : 70));
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.unlock"), buttonX + buttonWidth / 2, buttonY + (buttonHeight - textRenderer.fontHeight) / 2, textColor);
    }

    private void renderLockGlyph(DrawContext context, int centerX, int centerY, int alpha) {
        int bodyWidth = s(18);
        int bodyHeight = s(14);
        int bodyX = centerX - bodyWidth / 2;
        int bodyY = centerY - bodyHeight / 2 + s(4);
        int bodyRadius = Math.max(3, s(4));

        RoundedRectRenderer.fillRoundedRect(context, bodyX, bodyY, bodyWidth, bodyHeight, bodyRadius, withAlpha(0xFFD6ECFF, alpha));
        RoundedRectRenderer.fillRoundedRect(context, bodyX + s(2), bodyY + s(2), Math.max(1, bodyWidth - s(4)), Math.max(1, bodyHeight - s(4)), Math.max(1, bodyRadius - 1), withAlpha(0xFF5889AE, Math.max(100, alpha - 50)));

        int shackleWidth = s(10);
        int shackleHeight = s(8);
        int shackleLeft = centerX - shackleWidth / 2;
        int shackleTop = bodyY - shackleHeight + s(2);
        int shackleColor = withAlpha(0xFFEAF6FF, Math.max(120, alpha - 10));
        context.fill(shackleLeft, shackleTop + s(2), shackleLeft + s(2), bodyY + s(1), shackleColor);
        context.fill(shackleLeft + shackleWidth - s(2), shackleTop + s(2), shackleLeft + shackleWidth, bodyY + s(1), shackleColor);
        context.fill(shackleLeft + s(1), shackleTop, shackleLeft + shackleWidth - s(1), shackleTop + s(2), shackleColor);
    }

    private void renderScaledCenteredText(DrawContext context, Text text, int centerX, int y, float scale, int color) {
        int textWidth = textRenderer.getWidth(text);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX - (textWidth * scale) / 2.0F, (float) y);
        context.getMatrices().scale(scale, scale);
        context.drawTextWithShadow(textRenderer, text, 0, 0, color);
        context.getMatrices().popMatrix();
    }

    private boolean isUnlockReady() {
        return getScreenWakeProgress() >= 0.98F;
    }

    private boolean isInsideUnlockButton(double mouseX, double mouseY) {

        int buttonWidth = Math.min(contentWidth - s(18), s(128));
        int buttonHeight = s(28);
        int buttonX = contentX + (contentWidth - buttonWidth) / 2;
        int buttonY = phoneY + phoneHeight - s(54);
        return mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
    }

    private float getShellRiseProgress() {
        if (!areAnimationsEnabled()) {
            return 1.0F;
        }
        return easeOutCubic(getClampedProgress(0L, SHELL_RISE_MS));
    }

    private float getScreenWakeProgress() {
        if (!areAnimationsEnabled()) {
            return 1.0F;
        }
        return smoothStep(getClampedProgress(SCREEN_WAKE_DELAY_MS, SCREEN_WAKE_MS));
    }

    private float getClampedProgress(long delayMs, long durationMs) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - openedAtMs - delayMs);
        return MathHelper.clamp(elapsed / (float) durationMs, 0.0F, 1.0F);
    }

    private float easeOutCubic(float value) {
        float inv = 1.0F - value;
        return 1.0F - inv * inv * inv;
    }

    private float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private int withAlpha(int color, int alpha) {
        int clampedAlpha = MathHelper.clamp(alpha, 0, 255);
        return (clampedAlpha << 24) | (color & 0x00FFFFFF);
    }

    // --- Unlock animation helpers ---
    private float getUnlockProgressRaw() {
        if (!unlocking) return 0.0F;
        if (!areAnimationsEnabled()) return 1.0F;
        long elapsed = Math.max(0L, System.currentTimeMillis() - unlockingStartedAtMs);
        return MathHelper.clamp(elapsed / (float) UNLOCK_ANIM_MS, 0.0F, 1.0F);
    }

    private float getUnlockEase() {
        float raw = getUnlockProgressRaw();
        float eased = easeOutCubic(raw);
        // if finished and switch hasn't happened, perform the switch
        if (raw >= 1.0F && !unlockSwitchPerformed) {
            if (client != null) {
                client.setScreen(new PhoneHomeScreen());
            }
            unlockSwitchPerformed = true;
        }
        return eased;
    }
}
