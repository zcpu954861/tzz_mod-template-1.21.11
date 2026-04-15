package com.zcpu.tzzmod.client.phone.ui;

import com.zcpu.tzzmod.client.DeathSyncClient;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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

    // --- Experimental tech UI color palette (DARK mode) ---
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

    // --- Light mode color palette ---
    private static final int LIGHT_ACCENT = 0xFF0099CC;           // blue accent
    private static final int LIGHT_ACCENT_DIM = 0xAA0080AA;
    private static final int LIGHT_ACCENT_GLOW = 0x220099CC;
    private static final int LIGHT_BG_DARK = 0xE0E8EDF4;         // light gray background
    private static final int LIGHT_BG_PANEL = 0xCCF0F4F8;        // light panel interior
    private static final int LIGHT_BORDER = 0xAAB0C0D0;          // border color
    private static final int LIGHT_BORDER_BRIGHT = 0xCC0099CC;   // bright border accent
    private static final int LIGHT_GRID = 0x10A0B0C8;            // grid line color
    private static final int LIGHT_TEXT = 0xFF1A2A3A;            // primary text (dark)
    private static final int LIGHT_TEXT_DIM = 0xFF6A7A8A;        // muted text
    private static final int LIGHT_DIVIDER = 0xAA0099CC;         // status divider
    private static final int LIGHT_BTN_FILL = 0x44D8E4F0;        // button fill
    private static final int LIGHT_BTN_HOVER = 0x66C0D4E8;       // button hover
    private static final int LIGHT_BTN_BORDER = 0xAA80B0D0;      // button border
    private static final int LIGHT_BTN_PRIMARY_FILL = 0x880099CC; // primary button
    private static final int LIGHT_BTN_PRIMARY_HOVER = 0xAA00AADD;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final long APP_OPEN_ANIMATION_MS = 220L;
    private static final long APP_CLOSE_ANIMATION_MS = 180L;
    private static final long INIT_SCAN_DURATION_MS = 700L;

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
    private boolean launchAnimationFired;

    // Init scan animation: triggered when screen is first created
    private final long screenCreatedAtMs = System.currentTimeMillis();

    // Status bar battery (randomized once per open)
    private int statusBatteryPercent = 85;

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
        } else if (appLaunchAnimation == null && !launchAnimationFired && hasDefaultLaunchAnimation()) {
            launchAnimationFired = true;
            int sz = s(16);
            int cx = phoneX + phoneWidth / 2 - sz / 2;
            int cy = phoneY + phoneHeight / 2 - sz / 2;
            appLaunchAnimation = new AppLaunchAnimation(cx, cy, sz, sz);
            appLaunchAnimationStartedAtMs = System.currentTimeMillis();
        }
        closeAnimationStartedAtMs = -1L;
        // Randomize battery only on first init (not on resize or screen-switch return)
        if (statusBatteryPercent == 85) {
            statusBatteryPercent = 80 + (int) (Math.random() * 11);
        }
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
            int scanColor = themeAccent();
            int scanColorRGB = scanColor & 0x00FFFFFF;
            // Unified scan-line animation (same style as AR headset, issue #4)
            int insetX = s(4);
            int left = phoneX + insetX;
            int right = phoneX + phoneWidth - insetX;
            if (animatingOpen) {
                context.getMatrices().translate(translateX, translateY);
                context.getMatrices().scale(scaleX, scaleY);
                renderPhoneScreen(context, renderMouseX, renderMouseY, delta);

                // Single scan line sweeping downward
                int scanY = phoneY + Math.round(openProgress * phoneHeight);
                if (scanY >= phoneY && scanY < phoneY + phoneHeight) {
                    int scanAlpha = Math.round(180 * (1.0F - openProgress));
                    context.fill(left, scanY, right, scanY + Math.max(1, s(2)),
                            (scanAlpha << 24) | scanColorRGB);
                }
            } else {
                // Close animation: single scan line sweeping upward
                context.getMatrices().translate(translateX, translateY);
                context.getMatrices().scale(scaleX, scaleY);
                renderPhoneScreen(context, renderMouseX, renderMouseY, delta);

                renderClosingRevealVeil(context, closeProgress);

                int scanY = phoneY + phoneHeight - Math.round(closeProgress * phoneHeight);
                if (scanY > phoneY && scanY <= phoneY + phoneHeight) {
                    int scanAlpha = Math.round(180 * closeProgress);
                    context.fill(left, scanY, right, scanY + Math.max(1, s(2)),
                            (scanAlpha << 24) | scanColorRGB);
                }
            }
            context.getMatrices().popMatrix();
            return;
        }

        renderPhoneScreen(context, renderMouseX, renderMouseY, delta);
        renderInitScanOverlay(context);
    }

    /** Override and return true to show a one-shot scan line when this screen first opens. */
    protected boolean hasInitScanAnimation() {
        return false;
    }

    /** Override and return true to auto-create a center-zoom launch animation when opened without an explicit animation. */
    protected boolean hasDefaultLaunchAnimation() {
        return false;
    }

    private void renderInitScanOverlay(DrawContext context) {
        if (!hasInitScanAnimation() || !areAnimationsEnabled()) return;
        long elapsed = System.currentTimeMillis() - screenCreatedAtMs;
        if (elapsed >= INIT_SCAN_DURATION_MS) return;
        float prog = elapsed / (float) INIT_SCAN_DURATION_MS;
        int insetX = s(4);
        int scanY = phoneY + (int) Math.round(phoneHeight * prog);
        if (scanY >= phoneY && scanY < phoneY + phoneHeight) {
            int alpha = Math.round(160 * (1.0F - prog));
            int accentRGB = themeAccent() & 0x00FFFFFF;
            context.fill(phoneX + insetX, scanY, phoneX + phoneWidth - insetX,
                    scanY + Math.max(1, s(2)), (alpha << 24) | accentRGB);
        }
    }

    protected void renderPhoneScreen(DrawContext context, int mouseX, int mouseY, float delta) {
        renderTechBackground(context);
        drawTechBorder(context, phoneX, phoneY, phoneWidth, phoneHeight);
        renderTechStatusBar(context);

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
            if (isLightMode()) {
                context.drawText(textRenderer, text, x, y, color, false);
            } else {
                context.drawTextWithShadow(textRenderer, text, x, y, color);
            }
            return;
        }
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(textScale, textScale);
        if (isLightMode()) {
            context.drawText(textRenderer, text, 0, 0, color, false);
        } else {
            context.drawTextWithShadow(textRenderer, text, 0, 0, color);
        }
        context.getMatrices().popMatrix();
    }

    /**
     * Draw centered text with shadow, applying the phone text scale factor.
     */
    protected void drawScaledCenteredText(DrawContext context, Text text, int centerX, int y, int color) {
        if (textScale >= 0.99F) {
            if (isLightMode()) {
                int hw = textRenderer.getWidth(text) / 2;
                context.drawText(textRenderer, text, centerX - hw, y, color, false);
            } else {
                context.drawCenteredTextWithShadow(textRenderer, text, centerX, y, color);
            }
            return;
        }
        int textWidth = textRenderer.getWidth(text);
        float scaledHalfWidth = textWidth * textScale / 2.0F;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX - scaledHalfWidth, y);
        context.getMatrices().scale(textScale, textScale);
        if (isLightMode()) {
            context.drawText(textRenderer, text, 0, 0, color, false);
        } else {
            context.drawTextWithShadow(textRenderer, text, 0, 0, color);
        }
        context.getMatrices().popMatrix();
    }

    protected boolean hasResource(net.minecraft.util.Identifier identifier) {
        return client != null && client.getResourceManager().getResource(identifier).isPresent();
    }

    protected void styleTextField(net.minecraft.client.gui.widget.TextFieldWidget field) {
        field.setDrawsBackground(false);
        field.setEditableColor(isLightMode() ? 0xFF1A1A2E : 0xFFE0E0E0);
        field.setUneditableColor(isLightMode() ? 0xFF707080 : 0xFF707070);
        field.setTextShadow(!isLightMode());
    }

    protected void renderStyledTextFieldBackground(DrawContext context, net.minecraft.client.gui.widget.TextFieldWidget field) {
        field.setEditableColor(isLightMode() ? 0xFF1A1A2E : 0xFFE0E0E0);
        field.setUneditableColor(isLightMode() ? 0xFF707080 : 0xFF707070);
        field.setTextShadow(!isLightMode());
        int x = field.getX() - s(2);
        int y = field.getY() - s(1);
        int w = field.getWidth() + s(4);
        int h = field.getHeight() + s(2);
        // 浅色模式使用不透明背景防止文字重影；深色模式保持半透明深色
        int bg = isLightMode() ? 0xFFF0F4F8 : 0x221A2A3C;
        int border = field.isFocused() ? themeAccent() : themeBorder();
        context.fill(x, y, x + w, y + h, bg);
        // top / bottom
        context.fill(x, y, x + w, y + 1, border);
        context.fill(x, y + h - 1, x + w, y + h, border);
        // left / right
        context.fill(x, y, x + 1, y + h, border);
        context.fill(x + w - 1, y, x + w, y + h, border);
    }

    protected void drawPhoneTextCenteredFixed(DrawContext context, Text text, int centerX, int y) {
        int color = themeText();
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
        int chamfer = s(TECH_CHAMFER);
        int alpha = MathHelper.clamp(Math.round(140.0F * closeProgress), 0, 140);
        int baseColor = isLightMode() ? 0xE8EDF4 : 0x0A0F1A;
        // Fill center rect
        context.fill(screenX + chamfer, screenY, screenX + screenWidth - chamfer, screenY + screenHeight, (alpha << 24) | baseColor);
        // Fill side strips
        context.fill(screenX, screenY + chamfer, screenX + screenWidth, screenY + screenHeight - chamfer, (alpha << 24) | baseColor);
        // Fill diagonal corners
        for (int i = 0; i < chamfer; i++) {
            int offset = chamfer - i;
            int lineAlpha = alpha * (chamfer - i) / chamfer;
            int c = (lineAlpha << 24) | baseColor;
            context.fill(screenX + offset, screenY + i, screenX + screenWidth - offset, screenY + i + 1, c);
            context.fill(screenX + offset, screenY + screenHeight - 1 - i, screenX + screenWidth - offset, screenY + screenHeight - i, c);
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
        fillChamferedRect(context, bgX, bgY, bgW, bgH, chamfer, themeBgDark());
        // Inner panel slightly inset
        fillChamferedRect(context, bgX + s(1), bgY + s(1), Math.max(1, bgW - s(2)), Math.max(1, bgH - s(2)), Math.max(1, chamfer - s(1)), themeBgPanel());

        // Grid lines (horizontal)
        int gridSpacing = s(18);
        int gridColor = themeGrid();
        if (gridSpacing > 0) {
            for (int gy = bgY + gridSpacing; gy < bgY + bgH - chamfer; gy += gridSpacing) {
                context.fill(bgX + chamfer, gy, bgX + bgW - chamfer, gy + 1, gridColor);
            }
        }
        // Grid lines (vertical)
        if (gridSpacing > 0) {
            for (int gx = bgX + chamfer; gx < bgX + bgW - chamfer; gx += gridSpacing) {
                context.fill(gx, bgY + chamfer, gx + 1, bgY + bgH - chamfer, gridColor);
            }
        }

        // Subtle glow at top edge
        context.fill(bgX + chamfer, bgY + s(1), bgX + bgW - chamfer, bgY + s(2), themeAccentGlow());
    }

    /**
     * Draws a shield icon centered at (cx, cy) with given size and color.
     * Top part is rectangular, bottom part narrows to a point.
     */
    protected static void drawShieldIcon(DrawContext context, int cx, int cy, int size, int color) {
        int w = Math.max(3, size);
        int h = Math.max(4, size + size / 4);
        int x = cx - w / 2;
        int y = cy - h / 2;
        int bodyH = h * 3 / 5;
        context.fill(x, y, x + w, y + bodyH, color);
        int triH = h - bodyH;
        for (int row = 0; row <= triH; row++) {
            int inset = (row * (w / 2 + 1)) / Math.max(1, triH + 1);
            int left = x + inset;
            int right = x + w - inset;
            if (right > left) {
                context.fill(left, y + bodyH + row, right, y + bodyH + row + 1, color);
            }
        }
    }

    /**
     * Draw the tech-style chamfered border: angled corner cuts with accent lines.
     */
    protected void drawTechBorder(DrawContext context, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        int t = Math.max(1, s(1));
        int c = s(TECH_CHAMFER);
        int borderBright = themeBorderBright();

        // Top edge (full width between chamfers)
        context.fill(x + c, y, x + width - c, y + t, borderBright);
        // Bottom edge (full width between chamfers)
        context.fill(x + c, y + height - t, x + width - c, y + height, borderBright);

        // Top-left corner diagonal only
        drawChamferCorner(context, x, y, c, t, -1, -1, borderBright);
        // Bottom-right corner diagonal only
        drawChamferCorner(context, x + width, y + height, c, t, 1, 1, borderBright);
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
     * Layout: [time][shield] ... [head+name] ... [signal][battery]
     */
    private void renderTechStatusBar(DrawContext context) {
        int statusX = contentX;
        int statusY = phoneY + s(8);
        int statusWidth = contentWidth;
        int statusHeight = s(STATUS_BAR_HEIGHT);
        int midY = statusY + statusHeight / 2;
        int textY = statusY + s(4);

        String timeText = LocalTime.now().format(TIME_FORMATTER);
        String playerName = client != null && client.player != null
                ? client.player.getName().getString()
                : "Player";
        boolean dead = DeathSyncClient.isLocalPlayerDead();
        int shieldColor = dead ? 0xFFFF4444 : themeAccent();
        int shieldSize = Math.max(4, statusHeight - s(6));

        // --- Left: time + shield ---
        int timeW = scaledTextWidth(timeText);
        drawScaledText(context, Text.literal(timeText), statusX + s(4), textY, themeAccent());
        int shieldCX = statusX + s(4) + timeW + s(4) + shieldSize / 2;
        drawShieldIcon(context, shieldCX, midY, shieldSize, shieldColor);
        int leftEdge = shieldCX + shieldSize / 2 + s(4);

        // --- Right: signal bars + battery ---
        int batteryIconW = s(10);
        int batteryIconH = s(6);
        int batteryStr_len = scaledTextWidth(statusBatteryPercent + "%");
        int signalW = s(12);
        int rightPadding = s(4);
        // Right elements are: batteryIcon + gap + batteryText + gap + signalBars
        int rightTotalW = signalW + s(4) + batteryStr_len + s(2) + batteryIconW + s(2);
        int rightStartX = statusX + statusWidth - rightPadding - rightTotalW;

        // Signal bars
        int signalBaseY = statusY + statusHeight - s(3);
        int barW = Math.max(1, s(2));
        int barGap = Math.max(1, s(1));
        int signalColor = isLightMode() ? 0xFF0099CC : 0xFF00FFE0;
        for (int i = 0; i < 4; i++) {
            int barH = Math.max(1, s(3) + i * s(2));
            int bx = rightStartX + i * (barW + barGap);
            int by = signalBaseY - barH;
            context.fill(bx, by, bx + barW, signalBaseY, signalColor);
        }
        // Battery text + icon
        int batteryTextX = rightStartX + signalW + s(4);
        drawScaledText(context, Text.literal(statusBatteryPercent + "%"), batteryTextX, textY, themeTextDim());
        int biX = batteryTextX + batteryStr_len + s(2);
        int biY = statusY + (statusHeight - batteryIconH) / 2;
        context.fill(biX, biY, biX + batteryIconW, biY + 1, themeTextDim());
        context.fill(biX, biY + batteryIconH - 1, biX + batteryIconW, biY + batteryIconH, themeTextDim());
        context.fill(biX, biY, biX + 1, biY + batteryIconH, themeTextDim());
        context.fill(biX + batteryIconW - 1, biY, biX + batteryIconW, biY + batteryIconH, themeTextDim());
        int nubH = Math.max(2, batteryIconH - s(3));
        int nubY = biY + (batteryIconH - nubH) / 2;
        context.fill(biX + batteryIconW, nubY, biX + batteryIconW + Math.max(1, s(1)), nubY + nubH, themeTextDim());
        int fillW = Math.max(1, (int) ((batteryIconW - 2) * statusBatteryPercent / 100.0));
        int fillColor = statusBatteryPercent > 20 ? (isLightMode() ? 0xFF22AA44 : 0xFF44FF88) : 0xFFFF4444;
        context.fill(biX + 1, biY + 1, biX + 1 + fillW, biY + batteryIconH - 1, fillColor);
        int rightEdge = rightStartX;

        // --- Center: head + name (between leftEdge and rightEdge) ---
        int headSize = Math.max(4, statusHeight - s(4));
        int availW = rightEdge - leftEdge;
        String shownName = textRenderer.trimToWidth(playerName, Math.max(s(10), Math.round((availW - headSize - s(3)) / textScale)));
        int nameW = scaledTextWidth(shownName);
        int totalW = headSize + s(3) + nameW;
        int headX = leftEdge + Math.max(0, (availW - totalW) / 2);
        int headY = statusY + (statusHeight - headSize) / 2;

        if (client != null && client.player != null) {
            try {
                SkinTextures skin = client.player.getSkin();
                Identifier skinTexture = skin.body().texturePath();
                int hatExpand = Math.max(1, s(1));
                context.drawTexture(RenderPipelines.GUI_TEXTURED, skinTexture,
                        headX, headY, 8.0F, 8.0F,
                        headSize, headSize, 8, 8, 64, 64, -1);
                context.drawTexture(RenderPipelines.GUI_TEXTURED, skinTexture,
                        headX - hatExpand, headY - hatExpand,
                        40.0F, 8.0F,
                        headSize + hatExpand * 2, headSize + hatExpand * 2,
                        8, 8, 64, 64, -1);
            } catch (Throwable ignored) {
                context.fill(headX, headY, headX + headSize, headY + headSize,
                        isLightMode() ? 0xFF4A90E2 : 0xFF00B4A0);
            }
        }
        drawScaledText(context, Text.literal(shownName), headX + headSize + s(3), textY, themeText());

        // Geometric divider below status bar
        int dividerY = statusY + statusHeight + s(1);
        int divW = statusWidth - s(4);
        int divX = statusX + s(2);
        context.fill(divX, dividerY, divX + divW, dividerY + 1, themeDivider());

        // Small diamond accent at center of divider
        int diamondCx = statusX + statusWidth / 2;
        int diamondR = s(2);
        for (int dy = -diamondR; dy <= diamondR; dy++) {
            int dxSpan = diamondR - Math.abs(dy);
            context.fill(diamondCx - dxSpan, dividerY + dy, diamondCx + dxSpan + 1, dividerY + dy + 1, themeAccent());
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

        renderTechStyledButton(context, data, hovered, focused, selected);
    }

    private void renderTechStyledButton(DrawContext context, PhoneButtonRenderData data, boolean hovered, boolean focused, boolean selected) {
        int x = data.x();
        int y = data.y();
        int width = data.width();
        int height = data.height();
        boolean active = data.button().active;

        if (data.variant() == PhoneButtonWidget.Variant.GHOST && !hovered && !focused && !selected) {
            // Ghost buttons: only show label
            String label = data.message().getString();
            if (!label.isBlank()) {
                int textY = y + Math.max(0, (height - scaledFontHeight()) / 2);
                drawScaledCenteredText(context, data.message(), x + width / 2, textY, active ? themeTextDim() : 0xFF3A4A5A);
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
            borderColor = hovered ? themeAccent() : themeBtnBorder();
            fillColor = hovered ? themeBtnPrimaryHover() : themeBtnPrimaryFill();
            textColor = themeText();
        } else if (selected) {
            borderColor = themeAccent();
            fillColor = hovered ? 0x8800806A : 0x66005A4A;
            textColor = themeText();
        } else {
            borderColor = hovered ? themeBtnBorder() : themeBorder();
            fillColor = hovered ? themeBtnHover() : themeBtnFill();
            textColor = hovered ? themeText() : (isLightMode() ? 0xFF4A5A6A : 0xFFB0D0E0);
        }

        // Draw angular tech button: fill + 4-line border (#9)
        int cut = Math.max(2, Math.min(height / 3, s(4)));
        // Fill with angular corners (same fill as drawAngularTechFrame)
        for (int row = 0; row < height; row++) {
            int leftCut = 0, rightCut = 0;
            if (row < cut) leftCut = cut - row;
            if (row >= height - cut) rightCut = cut - (height - 1 - row);
            context.fill(x + leftCut, y + row, x + width - rightCut, y + row + 1, fillColor);
        }
        // 4-line border: top edge + top-left diagonal + bottom edge + bottom-right diagonal
        // Top edge (from cut to right end)
        context.fill(x + cut, y, x + width, y + 1, borderColor);
        // Bottom edge (from left to right-cut)
        context.fill(x, y + height - 1, x + width - cut, y + height, borderColor);
        // Top-left diagonal
        for (int i = 0; i < cut; i++) {
            context.fill(x + cut - i, y + i, x + cut - i + 1, y + i + 1, borderColor);
        }
        // Bottom-right diagonal
        for (int i = 0; i < cut; i++) {
            context.fill(x + width - cut + i - 1, y + height - 1 - i,
                    x + width - cut + i, y + height - i, borderColor);
        }

        // Focused: outer glow
        if (focused) {
            fillChamferedRect(context, x - 1, y - 1, width + 2, height + 2, cut + 1, themeAccentGlow());
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
        launchAnimationFired = true;
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
        return true;
    }

    protected final boolean isLightMode() {
        return PhoneSettingsClient.isLightModeEnabled();
    }

    // --- Theme-aware color accessors ---
    protected int themeAccent() { return isLightMode() ? LIGHT_ACCENT : TECH_ACCENT; }
    protected int themeAccentDim() { return isLightMode() ? LIGHT_ACCENT_DIM : TECH_ACCENT_DIM; }
    protected int themeAccentGlow() { return isLightMode() ? LIGHT_ACCENT_GLOW : TECH_ACCENT_GLOW; }
    protected int themeBgDark() { return isLightMode() ? LIGHT_BG_DARK : TECH_BG_DARK; }
    protected int themeBgPanel() { return isLightMode() ? LIGHT_BG_PANEL : TECH_BG_PANEL; }
    protected int themeBorder() { return isLightMode() ? LIGHT_BORDER : TECH_BORDER; }
    protected int themeBorderBright() { return isLightMode() ? LIGHT_BORDER_BRIGHT : TECH_BORDER_BRIGHT; }
    protected int themeGrid() { return isLightMode() ? LIGHT_GRID : TECH_GRID; }
    protected int themeText() { return isLightMode() ? LIGHT_TEXT : TECH_TEXT; }
    protected int themeTextDim() { return isLightMode() ? LIGHT_TEXT_DIM : TECH_TEXT_DIM; }
    protected int themeDivider() { return isLightMode() ? LIGHT_DIVIDER : TECH_DIVIDER; }
    protected int themeBtnFill() { return isLightMode() ? LIGHT_BTN_FILL : TECH_BTN_FILL; }
    protected int themeBtnHover() { return isLightMode() ? LIGHT_BTN_HOVER : TECH_BTN_HOVER; }
    protected int themeBtnBorder() { return isLightMode() ? LIGHT_BTN_BORDER : TECH_BTN_BORDER; }
    protected int themeBtnPrimaryFill() { return isLightMode() ? LIGHT_BTN_PRIMARY_FILL : TECH_BTN_PRIMARY_FILL; }
    protected int themeBtnPrimaryHover() { return isLightMode() ? LIGHT_BTN_PRIMARY_HOVER : TECH_BTN_PRIMARY_HOVER; }

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
