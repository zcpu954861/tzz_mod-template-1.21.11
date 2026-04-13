package com.zcpu.tzzmod.client.phone.ui;

import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class RegionTitleOverlay {
    private static final long DISPLAY_MS = 5_000L;
    private static final long ANIMATE_IN_MS = 260L;
    private static final long ANIMATE_OUT_MS = 320L;

    private static Text current = Text.empty();
    private static int currentColor = 0xFF7FE9AA;
    private static long shownAtMs = -1L;

    private RegionTitleOverlay() {
    }

    public static void show(Text title, int color) {
        if (title == null || title.getString().isBlank()) {
            return;
        }
        current = title.copy();
        currentColor = 0xFF000000 | (color & 0xFFFFFF);
        shownAtMs = System.currentTimeMillis();
    }

    public static void clear() {
        current = Text.empty();
        currentColor = 0xFF7FE9AA;
        shownAtMs = -1L;
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        MapClient.PlannerRegion activeRegion = MapClient.getCurrentRegion();
        boolean alwaysShow = PhoneSettingsClient.isAlwaysShowRegionTitleEnabled() && activeRegion != null;

        long now = System.currentTimeMillis();
        boolean transientVisible = shownAtMs >= 0L && !current.getString().isBlank();
        boolean animationsEnabled = PhoneSettingsClient.isAnimationsEnabled();
        float alphaFactor = 1.0F;
        float scale = 1.0F;
        float lift = 0.0F;
        Text title = Text.empty();
        int color = currentColor;

        if (transientVisible) {
            long elapsed = now - shownAtMs;
            if (elapsed >= DISPLAY_MS) {
                clear();
                transientVisible = false;
            } else if (!animationsEnabled) {
                title = current;
                color = currentColor;
            } else {
                float enter = clamp(elapsed / (float) ANIMATE_IN_MS);
                float remaining = DISPLAY_MS - elapsed;
                float exit = clamp(remaining / (float) ANIMATE_OUT_MS);
                alphaFactor = Math.min(enter, exit);
                scale = 0.92F + 0.08F * enter;
                lift = (1.0F - enter) * 10.0F + (1.0F - exit) * 6.0F;
                title = current;
                color = currentColor;
            }
        }

        if (!transientVisible && alwaysShow && activeRegion != null) {
            title = Text.literal(activeRegion.name());
            color = activeRegion.color();
        } else if (!transientVisible) {
            return;
        }

        String display = client.textRenderer.trimToWidth(title.getString(), Math.max(48, context.getScaledWindowWidth() - 80));
        if (display.isBlank()) {
            return;
        }

        int textWidth = client.textRenderer.getWidth(display);
        int paddingX = 14;
        int paddingY = 6;
        int boxWidth = textWidth + paddingX * 2;
        int boxHeight = client.textRenderer.fontHeight + paddingY * 2;
        int x = context.getScaledWindowWidth() / 2 - boxWidth / 2;
        int y = Math.max(10, Math.round(18.0F - lift));
        int alpha = Math.max(0, Math.min(255, Math.round(alphaFactor * 255.0F)));

        boolean techUi = PhoneSettingsClient.isExperimentalUiEnabled();
        if (techUi) {
            // Tech-themed region title: dark angular box with cyan accent
            int bgAlpha = Math.round(alpha * 0.75F);
            int lineAlpha = Math.round(alpha * 0.85F);
            int techBg = (bgAlpha << 24) | 0x0A0F1A;
            int techBorder = (lineAlpha << 24) | 0x00D4BE;
            int techAccent = (lineAlpha << 24) | 0x00FFE0;
            int techText = (alpha << 24) | 0xE0F7FF;

            // Chamfered background (approximate with clipped corners)
            int chamfer = 4;
            context.fill(x + chamfer, y, x + boxWidth - chamfer, y + boxHeight, techBg);
            context.fill(x, y + chamfer, x + boxWidth, y + boxHeight - chamfer, techBg);
            for (int i = 0; i < chamfer; i++) {
                int offset = chamfer - i;
                context.fill(x + offset, y + i, x + boxWidth - offset, y + i + 1, techBg);
                context.fill(x + offset, y + boxHeight - 1 - i, x + boxWidth - offset, y + boxHeight - i, techBg);
            }

            // Top accent line
            context.fill(x + chamfer, y, x + boxWidth - chamfer, y + 1, techAccent);
            // Bottom border
            context.fill(x + chamfer, y + boxHeight - 1, x + boxWidth - chamfer, y + boxHeight, techBorder);
            // Small corner accents
            int accentLen = Math.max(4, chamfer + 2);
            context.fill(x + chamfer, y - 1, x + chamfer + accentLen, y, techAccent);
            context.fill(x + boxWidth - chamfer - accentLen, y - 1, x + boxWidth - chamfer, y, techAccent);

            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (context.getScaledWindowWidth() / 2), (float) (y + paddingY));
            context.getMatrices().scale(scale, scale);
            context.drawTextWithShadow(client.textRenderer, Text.literal(display), -textWidth / 2, 0, techText);
            context.getMatrices().popMatrix();
        } else {
            int lineRgb = mixColor(color & 0xFFFFFF, 0xFFFFFF, 0.18F);
            int backgroundRgb = mixColor(color & 0xFFFFFF, 0x08110B, 0.82F);
            int background = (Math.round(alpha * 0.82F) << 24) | backgroundRgb;
            int border = (Math.round(alpha * 0.94F) << 24) | lineRgb;
            int textColor = (alpha << 24) | 0xF5FFF7;

            context.fill(x, y, x + boxWidth, y + boxHeight, background);
            context.fill(x, y, x + boxWidth, y + 1, border);
            context.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, border);

            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (context.getScaledWindowWidth() / 2), (float) (y + paddingY));
            context.getMatrices().scale(scale, scale);
            context.drawTextWithShadow(client.textRenderer, Text.literal(display), -textWidth / 2, 0, textColor);
            context.getMatrices().popMatrix();
        }
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static int mixColor(int base, int target, float amount) {
        float clamped = Math.max(0.0F, Math.min(1.0F, amount));
        int baseRed = (base >> 16) & 0xFF;
        int baseGreen = (base >> 8) & 0xFF;
        int baseBlue = base & 0xFF;
        int targetRed = (target >> 16) & 0xFF;
        int targetGreen = (target >> 8) & 0xFF;
        int targetBlue = target & 0xFF;
        int red = Math.round(baseRed + (targetRed - baseRed) * clamped);
        int green = Math.round(baseGreen + (targetGreen - baseGreen) * clamped);
        int blue = Math.round(baseBlue + (targetBlue - baseBlue) * clamped);
        return (red << 16) | (green << 8) | blue;
    }
}