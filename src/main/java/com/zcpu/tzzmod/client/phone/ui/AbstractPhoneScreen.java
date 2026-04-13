package com.zcpu.tzzmod.client.phone.ui;

import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public abstract class AbstractPhoneScreen extends Screen {
    // Reference: 3840x2054 with GUI scale 4 => 960x513.5 logical size.
    private static final float REFERENCE_GUI_WIDTH = 3840.0F / 4.0F;
    private static final float REFERENCE_GUI_HEIGHT = 2054.0F / 4.0F;

    private static final int BASE_PHONE_WIDTH = 220;
    private static final int BASE_PHONE_HEIGHT = 360;
    private static final int SCREEN_MARGIN = 8;

    // Use semi-transparent red for the phone border (alpha 0x8A to match previous transparency)
    // Make the phone outline use the same semi-transparent white as the status divider.
    private static final int PHONE_EDGE_OUTER_COLOR = 0x88E6EEF7;
    private static final int STATUS_DIVIDER_COLOR = 0x88E6EEF7;
    private static final int STATUS_BAR_HEIGHT = 16;

    // --- Experimental tech UI color palette ---
    private static final int TECH_ACCENT = 0xFF00FFE0;           // cyan accent
    private static final int TECH_ACCENT_DIM = 0xAA00C8B4;      // dimmed cyan
    private static final int TECH_ACCENT_GLOW = 0x3300FFE0;     // subtle glow
    private static final int TECH_BG_DARK = 0xE00A0F1A;         // deep navy background
    private static final int TECH_BG_PANEL = 0xCC101825;        // panel interior
    private static final int TECH_BORDER = 0xAA1A4A6C;          // border color
    private static final int TECH_BORDER_BRIGHT = 0xCC00D4BE;   // bright border accent
    private static final int TECH_GRID = 0x181A3050;            // grid line color
    private static final int TECH_TEXT = 0xFFE0F7FF;            // primary text
    private static final int TECH_TEXT_DIM = 0xFF6B8A9E;        // muted text
    private static final int TECH_DIVIDER = 0xAA00FFE0;         // status divider
    private static final int TECH_BTN_FILL = 0x660A1A2C;        // button fill
    private static final int TECH_BTN_HOVER = 0x8810283C;       // button hover
    private static final int TECH_BTN_BORDER = 0xAA00D4BE;      // button border
    private static final int TECH_BTN_PRIMARY_FILL = 0x8800B4A0; // primary button
    private static final int TECH_BTN_PRIMARY_HOVER = 0xAA00DEC8;
    private static final int TECH_CHAMFER = 8;                   // corner cut size (in base units)

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final long APP_OPEN_ANIMATION_MS = 220L;
    private static final long APP_CLOSE_ANIMATION_MS = 180L;

    protected final Screen parent;

    protected int phoneX;
    protected int phoneY;
    protected int phoneWidth;
    protected int phoneHeight;

    protected int contentX;
    protected int contentY;
    protected int contentWidth;
    protected int contentHeight;

    private float uiScale = 1.0F;
    private float textScale = 1.0F;
    private final List<PhoneButtonRenderData> phoneButtons = new ArrayList<>();
    private AppLaunchAnimation appLaunchAnimation;
    private long appLaunchAnimationStartedAtMs = -1L;
    private long closeAnimationStartedAtMs = -1L;

    // Phone button styles
    private static final int PHONE_BUTTON_TEXT_COLOR = 0xFFECECEC;
    private static final int PHONE_BUTTON_SUBTLE_TEXT_COLOR = 0xFFCFD9E6;
    private static final int PHONE_BUTTON_DISABLED_TEXT_COLOR = 0xFF7F8A97;
    private static final int PHONE_BUTTON_SECONDARY_FILL = 0x6626303C;
    private static final int PHONE_BUTTON_SECONDARY_HOVER_FILL = 0x88405A73;
    private static final int PHONE_BUTTON_PRIMARY_FILL = 0xAA2A8FC1;
    private static final int PHONE_BUTTON_PRIMARY_HOVER_FILL = 0xCC45B3E6;
    private static final int PHONE_BUTTON_SELECTED_FILL = 0xAA46657E;
    private static final int PHONE_BUTTON_SELECTED_HOVER_FILL = 0xAA5B7992;
    private static final int PHONE_BUTTON_DISABLED_FILL = 0x44161D26;
    private static final int PHONE_BUTTON_SECONDARY_BORDER = 0x88DCE8F5;
    private static final int PHONE_BUTTON_PRIMARY_BORDER = 0xCCB8F0FF;
    private static final int PHONE_BUTTON_SELECTED_BORDER = 0xCCE6F6FF;
    private static final int PHONE_BUTTON_DISABLED_BORDER = 0x508896A5;
    private static final int PHONE_BUTTON_HIGHLIGHT = 0x55FFFFFF;
    private static final int PHONE_BUTTON_GHOST_HOVER = 0x22E6EEF7;
    private static final int PHONE_BUTTON_GHOST_FOCUS = 0x66E6EEF7;

    protected AbstractPhoneScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        int margin = SCREEN_MARGIN;
        phoneButtons.clear();

        // Keep the same visual baseline size as 3840x2054 @ GUI scale 4.
        int targetWidth = Math.max(1, Math.round(REFERENCE_GUI_WIDTH * (BASE_PHONE_WIDTH / REFERENCE_GUI_WIDTH)));
        int targetHeight = Math.max(1, Math.round(REFERENCE_GUI_HEIGHT * (BASE_PHONE_HEIGHT / REFERENCE_GUI_HEIGHT)));

        float fitScale = Math.min(1.0F, Math.min(
                (this.width - margin * 2) / (float) targetWidth,
                (this.height - margin * 2) / (float) targetHeight
        ));
        uiScale = Math.max(0.35F, fitScale);

        // When the phone is smaller than the reference size, scale text down proportionally
        // so text doesn't become disproportionately large at high GUI scales.
        textScale = Math.min(1.0F, uiScale);

        phoneWidth = Math.max(1, Math.round(targetWidth * uiScale));
        phoneHeight = Math.max(1, Math.round(targetHeight * uiScale));

        // Bottom-right anchor.
        phoneX = Math.max(margin, this.width - margin - phoneWidth);
        phoneY = Math.max(margin, this.height - margin - phoneHeight);

        int horizontalInset = s(7);
        int topInset = s(8);
        int statusGap = s(6);
        int bottomInset = s(8);

        contentX = phoneX + horizontalInset;
        contentY = phoneY + topInset + s(STATUS_BAR_HEIGHT) + statusGap;
        contentWidth = Math.max(1, phoneWidth - horizontalInset * 2);
        contentHeight = Math.max(1, phoneHeight - topInset - s(STATUS_BAR_HEIGHT) - statusGap - bottomInset);

        if (!areAnimationsEnabled()) {
            appLaunchAnimation = null;
            appLaunchAnimationStartedAtMs = -1L;
        } else if (appLaunchAnimation != null && appLaunchAnimationStartedAtMs < 0L) {
            appLaunchAnimationStartedAtMs = System.currentTimeMillis();
        }
        closeAnimationStartedAtMs = -1L;
    }

    @Override
    public void tick() {
        if (isClosingToParent() && getCloseAnimationProgress() >= 1.0F && client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean animationsEnabled = areAnimationsEnabled();
        if (!animationsEnabled) {
            appLaunchAnimation = null;
            appLaunchAnimationStartedAtMs = -1L;
            closeAnimationStartedAtMs = -1L;
        }

        boolean animatingOpen = animationsEnabled && hasAppLaunchAnimation();
        boolean animatingClose = animationsEnabled && isClosingToParent();

        float openProgress = getOpenAnimationProgress();
        float closeProgress = getCloseAnimationProgress();

        if (animatingOpen && openProgress >= 1.0F) {
            appLaunchAnimation = null;
            appLaunchAnimationStartedAtMs = -1L;
            animatingOpen = false;
            openProgress = 1.0F;
        }

        int renderMouseX = isTransitionBlockingInteraction() ? Integer.MIN_VALUE : mouseX;
        int renderMouseY = isTransitionBlockingInteraction() ? Integer.MIN_VALUE : mouseY;

        float scaleX = 1.0F;
        float scaleY = 1.0F;
        float translateX = 0.0F;
        float translateY = 0.0F;

        if (animatingOpen && appLaunchAnimation != null) {
            float startScaleX = appLaunchAnimation.width() / (float) Math.max(1, phoneWidth);
            float startScaleY = appLaunchAnimation.height() / (float) Math.max(1, phoneHeight);
            scaleX = MathHelper.lerp(openProgress, startScaleX, 1.0F);
            scaleY = MathHelper.lerp(openProgress, startScaleY, 1.0F);
            float drawX = MathHelper.lerp(openProgress, appLaunchAnimation.x(), phoneX);
            float drawY = MathHelper.lerp(openProgress, appLaunchAnimation.y(), phoneY);
            translateX = drawX - phoneX * scaleX;
            translateY = drawY - phoneY * scaleY;
        } else if (animatingClose) {
            float closeScale = MathHelper.lerp(closeProgress, 1.0F, 0.92F);
            float centerX = phoneX + phoneWidth / 2.0F;
            float centerY = phoneY + phoneHeight / 2.0F;
            scaleX = closeScale;
            scaleY = closeScale;
            translateX = centerX - centerX * closeScale;
            translateY = centerY - centerY * closeScale;
        }

        if (animatingOpen || animatingClose) {
            context.getMatrices().pushMatrix();
            if (isExperimentalUi()) {
                // Tech UI: digital-reveal open / glitch-dissolve close
                if (animatingOpen) {
                    // Phase 1 (0-0.3): content alpha fades in
                    // Phase 2 (0.0-1.0): scale transition + scanline sweep
                    context.getMatrices().translate(translateX, translateY);
                    context.getMatrices().scale(scaleX, scaleY);
                    renderPhoneScreen(context, renderMouseX, renderMouseY, delta);

                    // Digital reveal overlay - multiple scan lines sweeping
                    int insetX = s(4);
                    int left = phoneX + insetX;
                    int right = phoneX + phoneWidth - insetX;

                    // Primary bright scanline sweeping down
                    int wipeY = phoneY + Math.round(openProgress * phoneHeight);
                    if (wipeY > phoneY && wipeY < phoneY + phoneHeight) {
                        context.fill(left, wipeY - s(1), right, wipeY, 0x9900FFE0);
                        context.fill(left, wipeY, right, wipeY + s(1), 0x5500FFE0);
                        // Trailing glow
                        context.fill(left, wipeY - s(3), right, wipeY - s(1), 0x2200FFE0);
                    }

                    // Secondary scanlines at different speeds for depth
                    int wipeY2 = phoneY + Math.round(Math.min(1.0F, openProgress * 1.4F) * phoneHeight);
                    if (wipeY2 > phoneY && wipeY2 < phoneY + phoneHeight) {
                        context.fill(left + (right - left) / 4, wipeY2, right - (right - left) / 4, wipeY2 + 1, 0x3300FFE0);
                    }

                    // Horizontal glitch bars during early transition
                    if (openProgress < 0.6F) {
                        float glitchIntensity = 1.0F - (openProgress / 0.6F);
                        int glitchAlpha = Math.round(50.0F * glitchIntensity);
                        // a few random-ish bars based on progress
                        int barCount = Math.min(4, Math.round(glitchIntensity * 5));
                        for (int i = 0; i < barCount; i++) {
                            int barY = phoneY + (phoneHeight * ((i * 37 + 13) % 100)) / 100;
                            int barH = Math.max(1, s(1 + i % 2));
                            int barLeft = left + ((i * 53) % (Math.max(1, (right - left) / 3)));
                            int barRight = Math.min(right, barLeft + (right - left) / 3 + ((i * 29) % (Math.max(1, (right - left) / 4))));
                            context.fill(barLeft, barY, barRight, barY + barH, (glitchAlpha << 24) | 0x0A0F1A);
                        }
                    }
                } else {
                    // Close animation: glitch-dissolve effect
                    context.getMatrices().translate(translateX, translateY);
                    context.getMatrices().scale(scaleX, scaleY);
                    renderPhoneScreen(context, renderMouseX, renderMouseY, delta);

                    // Digital dissolve veil
                    renderClosingRevealVeil(context, closeProgress);

                    // Glitch bars appearing during close
                    int insetX = s(4);
                    int left = phoneX + insetX;
                    int right = phoneX + phoneWidth - insetX;
                    if (closeProgress > 0.2F) {
                        float glitchIntensity = (closeProgress - 0.2F) / 0.8F;
                        int barCount = Math.min(6, Math.round(glitchIntensity * 6));
                        for (int i = 0; i < barCount; i++) {
                            int barY = phoneY + (phoneHeight * ((i * 41 + 7) % 100)) / 100;
                            int barH = Math.max(1, s(1));
                            context.fill(left, barY, right, barY + barH, (Math.round(40.0F * glitchIntensity) << 24) | 0x00FFE0);
                        }
                    }

                    // Fade-out scanline sweeping upward
                    int scanY = phoneY + phoneHeight - Math.round(closeProgress * phoneHeight);
                    if (scanY > phoneY && scanY < phoneY + phoneHeight) {
                        context.fill(left, scanY, right, scanY + s(1), 0x6600FFE0);
                    }
                }
            } else {
                context.getMatrices().translate(translateX, translateY);
                context.getMatrices().scale(scaleX, scaleY);
                renderPhoneScreen(context, renderMouseX, renderMouseY, delta);
                if (animatingClose) {
                    renderClosingRevealVeil(context, closeProgress);
                }
            }
            context.getMatrices().popMatrix();
            return;
        }

        renderPhoneScreen(context, renderMouseX, renderMouseY, delta);
    }

    protected void renderPhoneScreen(DrawContext context, int mouseX, int mouseY, float delta) {
        if (isExperimentalUi()) {
            renderTechBackground(context);
            drawTechBorder(context, phoneX, phoneY, phoneWidth, phoneHeight);
            renderTechStatusBar(context);
        } else {
            // Draw a thin rounded-line border: straight sides + stroked corner arcs (no filled center)
            drawLineBorder(context, phoneX, phoneY, phoneWidth, phoneHeight);
            renderStatusBar(context);
        }

        renderPhoneContent(context, mouseX, mouseY, delta);
        renderStyledPhoneButtons(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
        renderPhoneOverlay(context, mouseX, mouseY, delta);
    }

    protected void renderPhoneOverlay(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    protected void renderStatusBar(DrawContext context) {
        int statusX = contentX;
        int statusY = phoneY + s(8);
        int statusWidth = contentWidth;
        int statusHeight = s(STATUS_BAR_HEIGHT);

        String timeText = LocalTime.now().format(TIME_FORMATTER);
        String playerName = client != null && client.player != null
                ? client.player.getName().getString()
                : "Player";

        drawScaledText(context, Text.literal(timeText), statusX + s(6), statusY + s(4), 0xFFECECEC);

        String shownName = textRenderer.trimToWidth(playerName, Math.max(s(10), Math.round((statusWidth - s(40)) / textScale)));
        drawScaledCenteredText(context, Text.literal(shownName), statusX + statusWidth / 2, statusY + s(4), 0xFFECECEC);

        boolean hasDeathTag = com.zcpu.tzzmod.client.DeathSyncClient.isLocalPlayerDead();
        Text statusText = hasDeathTag ? Text.literal("死亡") : Text.literal("存活");
        int statusColor = hasDeathTag ? 0xFFFF6666 : 0xFF66FF66;
        int textWidth = scaledTextWidth(statusText.getString());
        int rightPadding = s(6);
        int drawX = statusX + statusWidth - rightPadding - textWidth;
        int drawY = statusY + s(4);
        if (drawX < statusX + s(6)) drawX = statusX + s(6);
        drawScaledText(context, statusText, drawX, drawY, statusColor);

        int dividerY = statusY + statusHeight + s(1);
        context.fill(statusX + s(2), dividerY, statusX + statusWidth - s(2), dividerY + 1, STATUS_DIVIDER_COLOR);
    }

    /**
     * Hook for phone content. Subclasses may override but must only draw text (no textures/panels).
     */
    protected abstract void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta);

    protected int s(int value) {
        return Math.max(1, Math.round(value * uiScale));
    }

    /**
     * Returns the text scale factor for the phone UI. Text is scaled proportionally
     * to the phone size so it doesn't become too large at high GUI scale values.
     */
    protected float getTextScale() {
        return textScale;
    }

    /**
     * Returns the effective scaled font height considering the text scale factor.
     */
    protected int scaledFontHeight() {
        return Math.max(1, Math.round(textRenderer.fontHeight * textScale));
    }

    /**
     * Returns the effective scaled text width considering the text scale factor.
     */
    protected int scaledTextWidth(String text) {
        return Math.max(1, Math.round(textRenderer.getWidth(text) * textScale));
    }

    /**
     * Draw text with shadow, applying the phone text scale factor.
     */
    protected void drawScaledText(DrawContext context, Text text, int x, int y, int color) {
        if (textScale >= 0.99F) {
            context.drawTextWithShadow(textRenderer, text, x, y, color);
            return;
        }
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(textScale, textScale);
        context.drawTextWithShadow(textRenderer, text, 0, 0, color);
        context.getMatrices().popMatrix();
    }

    /**
     * Draw centered text with shadow, applying the phone text scale factor.
     */
    protected void drawScaledCenteredText(DrawContext context, Text text, int centerX, int y, int color) {
        if (textScale >= 0.99F) {
            context.drawCenteredTextWithShadow(textRenderer, text, centerX, y, color);
            return;
        }
        int textWidth = textRenderer.getWidth(text);
        float scaledHalfWidth = textWidth * textScale / 2.0F;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX - scaledHalfWidth, y);
        context.getMatrices().scale(textScale, textScale);
        context.drawTextWithShadow(textRenderer, text, 0, 0, color);
        context.getMatrices().popMatrix();
    }

    protected boolean hasResource(net.minecraft.util.Identifier identifier) {
        return client != null && client.getResourceManager().getResource(identifier).isPresent();
    }

    protected void drawPhoneTextCenteredFixed(DrawContext context, Text text, int centerX, int y) {
        int color = isExperimentalUi() ? TECH_TEXT : 0xFFECECEC;
        drawScaledCenteredText(context, text, centerX, y, color);
    }

    protected void drawLineBorder(DrawContext context, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;

        int t = Math.max(1, s(1)); // line thickness in pixels, scaled

        // corner radius: at least t, at most s(8) and constrained by half of min(width,height)
        int maxCorner = Math.min(width, height) / 2;
        int cornerRadius = Math.max(t, Math.min(s(8), maxCorner));

        // Draw straight side segments between the corner arcs
        // top
        context.fill(x + cornerRadius, y, x + width - cornerRadius, y + t, PHONE_EDGE_OUTER_COLOR);
        // bottom
        context.fill(x + cornerRadius, y + height - t, x + width - cornerRadius, y + height, PHONE_EDGE_OUTER_COLOR);
        // left
        context.fill(x, y + cornerRadius, x + t, y + height - cornerRadius, PHONE_EDGE_OUTER_COLOR);
        // right
        context.fill(x + width - t, y + cornerRadius, x + width, y + height - cornerRadius, PHONE_EDGE_OUTER_COLOR);

        // Draw stroked corner arcs (only the ring part) for 4 corners
        // Use same center positions as RoundedRectRenderer.fillCorner would use
        int tlcx = x + cornerRadius;
        int tlcy = y + cornerRadius;
        drawCornerArc(context, tlcx, tlcy, cornerRadius, t, -1, -1);

        int trcx = x + width - cornerRadius - 1;
        int trcy = y + cornerRadius;
        drawCornerArc(context, trcx, trcy, cornerRadius, t, 1, -1);

        int blcx = x + cornerRadius;
        int blcy = y + height - cornerRadius - 1;
        drawCornerArc(context, blcx, blcy, cornerRadius, t, -1, 1);

        int brcx = x + width - cornerRadius - 1;
        int brcy = y + height - cornerRadius - 1;
        drawCornerArc(context, brcx, brcy, cornerRadius, t, 1, 1);
    }

    private void drawCornerArc(DrawContext context, int centerX, int centerY, int radius, int thickness, int xDir, int yDir) {
        if (radius <= 0 || thickness <= 0) return;
        int innerR = Math.max(0, radius - thickness);
        int[] outerSpans = RoundedRectRenderer.getCornerSpans(radius);
        int[] innerSpans = innerR > 0 ? RoundedRectRenderer.getCornerSpans(innerR) : null;

        for (int dy = 0; dy <= radius; dy++) {
            int dxOuter = outerSpans[dy];
            int dxInner = innerSpans != null && dy < innerSpans.length ? innerSpans[dy] : 0;

            int drawY = centerY + (yDir < 0 ? -dy : dy);

            if (xDir < 0) {
                int startX = centerX - dxOuter;
                int endX = centerX - dxInner; // exclusive
                if (startX < endX) context.fill(startX, drawY, endX, drawY + 1, PHONE_EDGE_OUTER_COLOR);
            } else {
                int startX = centerX + dxInner + 1;
                int endX = centerX + dxOuter + 1; // exclusive
                if (startX < endX) context.fill(startX, drawY, endX, drawY + 1, PHONE_EDGE_OUTER_COLOR);
            }
        }
    }

    private void renderClosingRevealVeil(DrawContext context, float closeProgress) {
        int inset = s(4);
        int screenX = phoneX + inset;
        int screenY = phoneY + inset;
        int screenWidth = Math.max(1, phoneWidth - inset * 2);
        int screenHeight = Math.max(1, phoneHeight - inset * 2);
        if (isExperimentalUi()) {
            int chamfer = s(TECH_CHAMFER);
            int alpha = MathHelper.clamp(Math.round(140.0F * closeProgress), 0, 140);
            // Fill center rect
            context.fill(screenX + chamfer, screenY, screenX + screenWidth - chamfer, screenY + screenHeight, (alpha << 24) | 0x0A0F1A);
            // Fill side strips
            context.fill(screenX, screenY + chamfer, screenX + screenWidth, screenY + screenHeight - chamfer, (alpha << 24) | 0x0A0F1A);
            // Fill diagonal corners
            for (int i = 0; i < chamfer; i++) {
                int offset = chamfer - i;
                int lineAlpha = alpha * (chamfer - i) / chamfer;
                int c = (lineAlpha << 24) | 0x0A0F1A;
                context.fill(screenX + offset, screenY + i, screenX + screenWidth - offset, screenY + i + 1, c);
                context.fill(screenX + offset, screenY + screenHeight - 1 - i, screenX + screenWidth - offset, screenY + screenHeight - i, c);
            }
        } else {
            int radius = Math.max(s(10), s(14));
            int alpha = MathHelper.clamp(Math.round(120.0F * closeProgress), 0, 120);
            RoundedRectRenderer.fillRoundedRect(context, screenX, screenY, screenWidth, screenHeight, radius, alpha << 24);
        }
    }

    // ===== Experimental Tech UI rendering methods =====

    /**
     * Draw the tech-style background: dark glass panel with subtle grid pattern.
     */
    private void renderTechBackground(DrawContext context) {
        int inset = s(2);
        int bgX = phoneX + inset;
        int bgY = phoneY + inset;
        int bgW = Math.max(1, phoneWidth - inset * 2);
        int bgH = Math.max(1, phoneHeight - inset * 2);
        int chamfer = s(TECH_CHAMFER);

        // Main background fill (chamfered)
        fillChamferedRect(context, bgX, bgY, bgW, bgH, chamfer, TECH_BG_DARK);
        // Inner panel slightly inset
        fillChamferedRect(context, bgX + s(1), bgY + s(1), Math.max(1, bgW - s(2)), Math.max(1, bgH - s(2)), Math.max(1, chamfer - s(1)), TECH_BG_PANEL);

        // Grid lines (horizontal)
        int gridSpacing = s(18);
        if (gridSpacing > 0) {
            for (int gy = bgY + gridSpacing; gy < bgY + bgH - chamfer; gy += gridSpacing) {
                context.fill(bgX + chamfer, gy, bgX + bgW - chamfer, gy + 1, TECH_GRID);
            }
        }
        // Grid lines (vertical)
        if (gridSpacing > 0) {
            for (int gx = bgX + chamfer; gx < bgX + bgW - chamfer; gx += gridSpacing) {
                context.fill(gx, bgY + chamfer, gx + 1, bgY + bgH - chamfer, TECH_GRID);
            }
        }

        // Subtle glow at top edge
        context.fill(bgX + chamfer, bgY + s(1), bgX + bgW - chamfer, bgY + s(2), TECH_ACCENT_GLOW);
    }

    /**
     * Draw the tech-style chamfered border: angled corner cuts with accent lines.
     */
    protected void drawTechBorder(DrawContext context, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        int t = Math.max(1, s(1));
        int c = s(TECH_CHAMFER);

        // Top edge (between chamfers)
        context.fill(x + c, y, x + width - c, y + t, TECH_BORDER_BRIGHT);
        // Bottom edge
        context.fill(x + c, y + height - t, x + width - c, y + height, TECH_BORDER);
        // Left edge
        context.fill(x, y + c, x + t, y + height - c, TECH_BORDER);
        // Right edge
        context.fill(x + width - t, y + c, x + width, y + height - c, TECH_BORDER);

        // Chamfered corners (diagonal lines)
        drawChamferCorner(context, x, y, c, t, -1, -1, TECH_BORDER_BRIGHT);   // top-left
        drawChamferCorner(context, x + width, y, c, t, 1, -1, TECH_BORDER_BRIGHT);  // top-right
        drawChamferCorner(context, x, y + height, c, t, -1, 1, TECH_BORDER);  // bottom-left
        drawChamferCorner(context, x + width, y + height, c, t, 1, 1, TECH_BORDER); // bottom-right

        // Small accent marks at top corners
        int accentLen = Math.max(s(3), c / 2);
        // top-left accent horizontal
        context.fill(x + c, y - s(1), x + c + accentLen, y, TECH_ACCENT);
        // top-right accent horizontal
        context.fill(x + width - c - accentLen, y - s(1), x + width - c, y, TECH_ACCENT);
    }

    private void drawChamferCorner(DrawContext context, int cornerX, int cornerY, int chamfer, int thickness, int xDir, int yDir, int color) {
        // Draw a diagonal line from (cornerX, cornerY+c*yDir) to (cornerX+c*xDir, cornerY)
        // xDir/yDir: -1 means extending into the rect, positive means away
        // For top-left: corner is at (x,y), chamfer goes from (x,y+c) to (x+c,y)
        for (int i = 0; i < chamfer; i++) {
            int px, py;
            if (xDir < 0 && yDir < 0) {
                // top-left: line from (cornerX, cornerY+chamfer) to (cornerX+chamfer, cornerY)
                px = cornerX + i;
                py = cornerY + chamfer - 1 - i;
            } else if (xDir > 0 && yDir < 0) {
                // top-right: line from (cornerX-chamfer, cornerY) to (cornerX, cornerY+chamfer)
                px = cornerX - chamfer + i;
                py = cornerY + i;
            } else if (xDir < 0 && yDir > 0) {
                // bottom-left: line from (cornerX, cornerY-chamfer) to (cornerX+chamfer, cornerY)
                px = cornerX + i;
                py = cornerY - chamfer + i;
            } else {
                // bottom-right: line from (cornerX-chamfer, cornerY) to (cornerX, cornerY-chamfer)
                px = cornerX - chamfer + i;
                py = cornerY - 1 - i;
            }
            for (int dt = 0; dt < thickness; dt++) {
                context.fill(px, py + dt, px + 1, py + dt + 1, color);
            }
        }
    }

    /**
     * Fill a rectangle with chamfered (angled) corners.
     */
    protected void fillChamferedRect(DrawContext context, int x, int y, int width, int height, int chamfer, int color) {
        int c = Math.min(chamfer, Math.min(width, height) / 2);
        if (c <= 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }
        // Center rectangle
        context.fill(x + c, y, x + width - c, y + height, color);
        // Left/right strips (between chamfers)
        context.fill(x, y + c, x + c, y + height - c, color);
        context.fill(x + width - c, y + c, x + width, y + height - c, color);
        // Fill chamfer triangles with diagonal fill
        for (int i = 0; i < c; i++) {
            int offset = c - i;
            // Top-left and top-right
            context.fill(x + offset, y + i, x + c, y + i + 1, color);
            context.fill(x + width - c, y + i, x + width - offset, y + i + 1, color);
            // Bottom-left and bottom-right
            context.fill(x + offset, y + height - 1 - i, x + c, y + height - i, color);
            context.fill(x + width - c, y + height - 1 - i, x + width - offset, y + height - i, color);
        }
    }

    /**
     * Draw the tech-style status bar with geometric dividers.
     */
    private void renderTechStatusBar(DrawContext context) {
        int statusX = contentX;
        int statusY = phoneY + s(8);
        int statusWidth = contentWidth;
        int statusHeight = s(STATUS_BAR_HEIGHT);

        String timeText = LocalTime.now().format(TIME_FORMATTER);
        String playerName = client != null && client.player != null
                ? client.player.getName().getString()
                : "Player";

        // Time on left with accent color
        drawScaledText(context, Text.literal(timeText), statusX + s(6), statusY + s(4), TECH_ACCENT);

        // Player name centered
        String shownName = textRenderer.trimToWidth(playerName, Math.max(s(10), Math.round((statusWidth - s(40)) / textScale)));
        drawScaledCenteredText(context, Text.literal(shownName), statusX + statusWidth / 2, statusY + s(4), TECH_TEXT);

        // Death/alive status on right
        boolean hasDeathTag = com.zcpu.tzzmod.client.DeathSyncClient.isLocalPlayerDead();
        Text statusText = hasDeathTag ? Text.literal("死亡") : Text.literal("存活");
        int statusColor = hasDeathTag ? 0xFFFF4444 : TECH_ACCENT;
        int textWidth = scaledTextWidth(statusText.getString());
        int rightPadding = s(6);
        int drawX = statusX + statusWidth - rightPadding - textWidth;
        if (drawX < statusX + s(6)) drawX = statusX + s(6);
        drawScaledText(context, statusText, drawX, statusY + s(4), statusColor);

        // Geometric divider
        int dividerY = statusY + statusHeight + s(1);
        int divW = statusWidth - s(4);
        int divX = statusX + s(2);
        context.fill(divX, dividerY, divX + divW, dividerY + 1, TECH_DIVIDER);

        // Small diamond accent at center of divider
        int diamondCx = statusX + statusWidth / 2;
        int diamondR = s(2);
        for (int dy = -diamondR; dy <= diamondR; dy++) {
            int dxSpan = diamondR - Math.abs(dy);
            context.fill(diamondCx - dxSpan, dividerY + dy, diamondCx + dxSpan + 1, dividerY + dy + 1, TECH_ACCENT);
        }
    }

    protected ButtonWidget addPhoneButton(Text message, int x, int y, int width, int height, PhoneButtonWidget.Variant variant, BooleanSupplier selectedSupplier, ButtonWidget.PressAction onPress) {
        ButtonWidget button = addDrawableChild(ButtonWidget.builder(message, onPress)
                .dimensions(x, y, width, height)
                .build());
        button.setAlpha(0.0F);
        phoneButtons.add(new PhoneButtonRenderData(button, message, x, y, width, height, variant, selectedSupplier));
        return button;
    }

    protected void addPhoneButton(Text message, int x, int y, int width, int height, ButtonWidget.PressAction onPress) {
        addPhoneButton(message, x, y, width, height, PhoneButtonWidget.Variant.SECONDARY, () -> false, onPress);
    }

    protected void addPhonePrimaryButton(Text message, int x, int y, int width, int height, ButtonWidget.PressAction onPress) {
        addPhoneButton(message, x, y, width, height, PhoneButtonWidget.Variant.PRIMARY, () -> false, onPress);
    }

    protected ButtonWidget addPhoneGhostButton(Text message, int x, int y, int width, int height, ButtonWidget.PressAction onPress) {
        return addPhoneButton(message, x, y, width, height, PhoneButtonWidget.Variant.GHOST, () -> false, onPress);
    }

    protected void addPhoneTabButton(Text message, int x, int y, int width, int height, BooleanSupplier selectedSupplier, ButtonWidget.PressAction onPress) {
        addPhoneButton(message, x, y, width, height, PhoneButtonWidget.Variant.SECONDARY, selectedSupplier, onPress);
    }

    private void renderStyledPhoneButtons(DrawContext context, int mouseX, int mouseY) {
        for (PhoneButtonRenderData data : phoneButtons) {
            renderStyledPhoneButton(context, mouseX, mouseY, data);
        }
    }

    private void renderStyledPhoneButton(DrawContext context, int mouseX, int mouseY, PhoneButtonRenderData data) {
        ButtonWidget button = data.button();
        int x = data.x();
        int y = data.y();
        int width = data.width();
        int height = data.height();
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        boolean focused = button.isFocused();
        boolean selected = data.selectedSupplier().getAsBoolean();

        if (isExperimentalUi()) {
            renderTechStyledButton(context, data, hovered, focused, selected);
            return;
        }

        int radius = Math.max(3, Math.min(height / 2, s(7)));

        if (data.variant() != PhoneButtonWidget.Variant.GHOST || hovered || focused || selected) {
            int borderColor = getPhoneButtonBorderColor(data.variant(), button.active, selected, focused);
            int fillColor = getPhoneButtonFillColor(data.variant(), button.active, hovered, selected, focused);
            int borderThickness = focused ? 2 : 1;
            RoundedRectRenderer.fillRoundedRect(context, x, y, width, height, radius, borderColor);
            int innerX = x + borderThickness;
            int innerY = y + borderThickness;
            int innerWidth = Math.max(1, width - borderThickness * 2);
            int innerHeight = Math.max(1, height - borderThickness * 2);
            int innerRadius = Math.max(1, radius - borderThickness);
            RoundedRectRenderer.fillRoundedRect(context, innerX, innerY, innerWidth, innerHeight, innerRadius, fillColor);

            if (button.active && data.variant() != PhoneButtonWidget.Variant.GHOST) {
                int highlightHeight = Math.max(1, height / 5);
                RoundedRectRenderer.fillRoundedRect(context, innerX, innerY, innerWidth, highlightHeight, Math.max(1, innerRadius - 1), PHONE_BUTTON_HIGHLIGHT);
            }
        }

        String label = data.message().getString();
        if (!label.isBlank()) {
            int textColor = getPhoneButtonTextColor(data.variant(), button.active, selected);
            int textY = y + Math.max(0, (height - scaledFontHeight()) / 2);
            drawScaledCenteredText(context, data.message(), x + width / 2, textY, textColor);
        }
    }

    private int getPhoneButtonFillColor(PhoneButtonWidget.Variant variant, boolean active, boolean hovered, boolean selected, boolean focused) {
        if (!active) {
            return PHONE_BUTTON_DISABLED_FILL;
        }
        if (variant == PhoneButtonWidget.Variant.GHOST) {
            if (selected || focused) {
                return PHONE_BUTTON_GHOST_FOCUS;
            }
            return hovered ? PHONE_BUTTON_GHOST_HOVER : 0x00000000;
        }
        if (selected) {
            return hovered ? PHONE_BUTTON_SELECTED_HOVER_FILL : PHONE_BUTTON_SELECTED_FILL;
        }
        if (variant == PhoneButtonWidget.Variant.PRIMARY) {
            return hovered ? PHONE_BUTTON_PRIMARY_HOVER_FILL : PHONE_BUTTON_PRIMARY_FILL;
        }
        return hovered ? PHONE_BUTTON_SECONDARY_HOVER_FILL : PHONE_BUTTON_SECONDARY_FILL;
    }

    private int getPhoneButtonBorderColor(PhoneButtonWidget.Variant variant, boolean active, boolean selected, boolean focused) {
        if (!active) {
            return PHONE_BUTTON_DISABLED_BORDER;
        }
        if (variant == PhoneButtonWidget.Variant.GHOST) {
            return (selected || focused) ? PHONE_BUTTON_GHOST_FOCUS : PHONE_BUTTON_GHOST_HOVER;
        }
        if (selected) {
            return PHONE_BUTTON_SELECTED_BORDER;
        }
        return variant == PhoneButtonWidget.Variant.PRIMARY ? PHONE_BUTTON_PRIMARY_BORDER : PHONE_BUTTON_SECONDARY_BORDER;
    }

    private int getPhoneButtonTextColor(PhoneButtonWidget.Variant variant, boolean active, boolean selected) {
        if (!active) {
            return PHONE_BUTTON_DISABLED_TEXT_COLOR;
        }
        if (selected || variant == PhoneButtonWidget.Variant.PRIMARY) {
            return PHONE_BUTTON_TEXT_COLOR;
        }
        return PHONE_BUTTON_SUBTLE_TEXT_COLOR;
    }

    private void renderTechStyledButton(DrawContext context, PhoneButtonRenderData data, boolean hovered, boolean focused, boolean selected) {
        int x = data.x();
        int y = data.y();
        int width = data.width();
        int height = data.height();
        boolean active = data.button().active;
        int chamfer = Math.max(2, Math.min(height / 4, s(3)));

        if (data.variant() == PhoneButtonWidget.Variant.GHOST && !hovered && !focused && !selected) {
            // Ghost buttons: only show label
            String label = data.message().getString();
            if (!label.isBlank()) {
                int textY = y + Math.max(0, (height - scaledFontHeight()) / 2);
                drawScaledCenteredText(context, data.message(), x + width / 2, textY, active ? TECH_TEXT_DIM : 0xFF3A4A5A);
            }
            return;
        }

        // Determine colors
        int borderColor, fillColor, textColor;
        if (!active) {
            borderColor = 0x44334455;
            fillColor = 0x330A1018;
            textColor = 0xFF3A4A5A;
        } else if (data.variant() == PhoneButtonWidget.Variant.PRIMARY) {
            borderColor = hovered ? TECH_ACCENT : TECH_BTN_BORDER;
            fillColor = hovered ? TECH_BTN_PRIMARY_HOVER : TECH_BTN_PRIMARY_FILL;
            textColor = TECH_TEXT;
        } else if (selected) {
            borderColor = TECH_ACCENT;
            fillColor = hovered ? 0x8800806A : 0x66005A4A;
            textColor = TECH_TEXT;
        } else {
            borderColor = hovered ? TECH_BTN_BORDER : TECH_BORDER;
            fillColor = hovered ? TECH_BTN_HOVER : TECH_BTN_FILL;
            textColor = hovered ? TECH_TEXT : 0xFFB0D0E0;
        }

        // Draw chamfered button
        fillChamferedRect(context, x, y, width, height, chamfer, fillColor);

        // Draw border as thin edges (1px), not a full outer chamfered rect
        // Top edge
        context.fill(x + chamfer, y, x + width - chamfer, y + 1, borderColor);
        // Bottom edge
        context.fill(x + chamfer, y + height - 1, x + width - chamfer, y + height, borderColor);
        // Left edge
        context.fill(x, y + chamfer, x + 1, y + height - chamfer, borderColor);
        // Right edge
        context.fill(x + width - 1, y + chamfer, x + width, y + height - chamfer, borderColor);
        // Chamfer diagonals
        for (int i = 0; i < chamfer; i++) {
            int off = chamfer - i;
            context.fill(x + off, y + i, x + off + 1, y + i + 1, borderColor);
            context.fill(x + width - off - 1, y + i, x + width - off, y + i + 1, borderColor);
            context.fill(x + off, y + height - 1 - i, x + off + 1, y + height - i, borderColor);
            context.fill(x + width - off - 1, y + height - 1 - i, x + width - off, y + height - i, borderColor);
        }

        // Subtle top accent line for PRIMARY or hovered
        if (active && (data.variant() == PhoneButtonWidget.Variant.PRIMARY || hovered)) {
            int accentLen = Math.max(8, width / 3);
            int accentX = x + (width - accentLen) / 2;
            context.fill(accentX, y, accentX + accentLen, y + 1, 0x8800FFE0);
        }

        // Focused: outer glow
        if (focused) {
            fillChamferedRect(context, x - 1, y - 1, width + 2, height + 2, chamfer + 1, TECH_ACCENT_GLOW);
        }

        // Label
        String label = data.message().getString();
        if (!label.isBlank()) {
            int textY = y + Math.max(0, (height - scaledFontHeight()) / 2);
            drawScaledCenteredText(context, data.message(), x + width / 2, textY, textColor);
        }
    }

    @Override
    public void close() {
        if (client == null) {
            return;
        }
        if (parent == null) {
            client.setScreen(null);
            return;
        }
        if (!areAnimationsEnabled()) {
            client.setScreen(parent);
            return;
        }
        if (!isClosingToParent()) {
            closeAnimationStartedAtMs = System.currentTimeMillis();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (isTransitionBlockingInteraction()) {
            return true;
        }
        return super.mouseClicked(click, doubleClick);
    }

    public void setAppLaunchAnimation(int x, int y, int width, int height) {
        if (!areAnimationsEnabled()) {
            appLaunchAnimation = null;
            appLaunchAnimationStartedAtMs = -1L;
            return;
        }
        appLaunchAnimation = new AppLaunchAnimation(x, y, Math.max(1, width), Math.max(1, height));
        appLaunchAnimationStartedAtMs = -1L;
    }

    protected boolean isTransitionBlockingInteraction() {
        if (!areAnimationsEnabled()) {
            return false;
        }
        return hasAppLaunchAnimation() || isClosingToParent();
    }

    protected final boolean areAnimationsEnabled() {
        return PhoneSettingsClient.isAnimationsEnabled();
    }

    protected final boolean isExperimentalUi() {
        return PhoneSettingsClient.isExperimentalUiEnabled();
    }

    private boolean hasAppLaunchAnimation() {
        return appLaunchAnimation != null && appLaunchAnimationStartedAtMs >= 0L;
    }

    private boolean isClosingToParent() {
        return closeAnimationStartedAtMs >= 0L && parent != null;
    }

    private float getOpenAnimationProgress() {
        if (!hasAppLaunchAnimation()) {
            return 1.0F;
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - appLaunchAnimationStartedAtMs);
        return easeOutCubic(MathHelper.clamp(elapsed / (float) APP_OPEN_ANIMATION_MS, 0.0F, 1.0F));
    }

    private float getCloseAnimationProgress() {
        if (!isClosingToParent()) {
            return 0.0F;
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - closeAnimationStartedAtMs);
        return smoothStep(MathHelper.clamp(elapsed / (float) APP_CLOSE_ANIMATION_MS, 0.0F, 1.0F));
    }

    private float easeOutCubic(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    private float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private record AppLaunchAnimation(int x, int y, int width, int height) {
    }


    private record PhoneButtonRenderData(ButtonWidget button, Text message, int x, int y, int width, int height,
                                         PhoneButtonWidget.Variant variant, BooleanSupplier selectedSupplier) {
    }
}
