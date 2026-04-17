package com.zcpu.tzzmod.client.ar.ui;

import com.zcpu.tzzmod.client.DeathSyncClient;
import com.zcpu.tzzmod.client.ui.ScreenHelpText;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Base class for all AR headset screens.
 * Centered layout, Apple Vision Pro style, with shared theme, no lock screen,
 * no vanilla background blur, and its own animation system.
 */
public abstract class AbstractARScreen extends Screen {
    // AR panel is wider than phone (16:9 aspect, spatial feel)
    private static final int BASE_AR_WIDTH = 420;
    private static final int BASE_AR_HEIGHT = 260;
    private static final int SCREEN_MARGIN = 16;
    private static final int AR_CHAMFER = 10;

    // --- Dark mode palette ---
    private static final int DARK_BG = 0xCC0C1220;
    private static final int DARK_BG_PANEL = 0xAA101828;
    private static final int DARK_BORDER = 0x661A3A5C;
    private static final int DARK_BORDER_BRIGHT = 0x8800C8BE;
    private static final int DARK_ACCENT = 0xFF00FFE0;
    private static final int DARK_ACCENT_DIM = 0xAA00C8B4;
    private static final int DARK_ACCENT_GLOW = 0x2200FFE0;
    private static final int DARK_TEXT = 0xFFE0F7FF;
    private static final int DARK_TEXT_DIM = 0xFF6B8A9E;
    private static final int DARK_GRID = 0x0C1A3050;
    private static final int DARK_DIVIDER = 0x6600FFE0;
    private static final int DARK_BTN_FILL = 0x440A1A2C;
    private static final int DARK_BTN_HOVER = 0x6610283C;
    private static final int DARK_BTN_BORDER = 0x8800D4BE;
    private static final int DARK_BTN_PRIMARY_FILL = 0x6600B4A0;
    private static final int DARK_BTN_PRIMARY_HOVER = 0x8800DEC8;

    // --- Light mode palette ---
    private static final int LIGHT_BG = 0xCCE8EDF4;
    private static final int LIGHT_BG_PANEL = 0xAAF0F4F8;
    private static final int LIGHT_BORDER = 0x66B0C0D0;
    private static final int LIGHT_BORDER_BRIGHT = 0x880099CC;
    private static final int LIGHT_ACCENT = 0xFF0099CC;
    private static final int LIGHT_ACCENT_DIM = 0xAA0088AA;
    private static final int LIGHT_ACCENT_GLOW = 0x220099CC;
    private static final int LIGHT_TEXT = 0xFF1A2A3A;
    private static final int LIGHT_TEXT_DIM = 0xFF6A7A8A;
    private static final int LIGHT_GRID = 0x0CA0B0C8;
    private static final int LIGHT_DIVIDER = 0x660099CC;
    private static final int LIGHT_BTN_FILL = 0x44D8E4F0;
    private static final int LIGHT_BTN_HOVER = 0x66C0D4E8;
    private static final int LIGHT_BTN_BORDER = 0x8880B0D0;
    private static final int LIGHT_BTN_PRIMARY_FILL = 0x660099CC;
    private static final int LIGHT_BTN_PRIMARY_HOVER = 0x8800AADD;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Instance fields
    protected final Screen parent;
    protected int panelX, panelY, panelWidth, panelHeight;
    protected int contentX, contentY, contentWidth, contentHeight;
    protected int hudBarY;
    private float uiScale = 1.0F;
    private float textScale = 1.0F;
    private final List<ARButtonRenderData> arButtons = new ArrayList<>();
    private boolean helpModeActive;
    private ARButtonRenderData helpButtonData;

    // HUD info
    private int batteryPercent = 85;
    private net.minecraft.util.Identifier cachedSkinTexture = null;

    // Skin UV constants
    private static final int FACE_U = 8;
    private static final int FACE_V = 8;
    private static final int HAT_U = 40;
    private static final int HAT_V = 8;
    private static final int SKIN_TEXTURE_SIZE = 64;

    // Animation state
    private long openAnimStartMs = -1L;
    private long closeAnimStartMs = -1L;
    private boolean closingToParent = false;
    private static final long OPEN_ANIM_DURATION_MS = 250L;
    private static final long CLOSE_ANIM_DURATION_MS = 200L;

    // Theme switch scan animation (issue #6)
    private boolean lastKnownLightMode = false;
    private long themeScanStartMs = -1L;
    private static final long THEME_SCAN_DURATION_MS = 400L;

    // App launch animation
    private AppLaunchAnim appLaunchAnim;
    private long appLaunchStartMs = -1L;
    private static final long APP_LAUNCH_DURATION_MS = 220L;

    protected AbstractARScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    // --- Theme accessors ---
    protected boolean isLightMode() { return PhoneSettingsClient.isLightModeEnabled(); }
    protected int themeAccent() { return isLightMode() ? LIGHT_ACCENT : DARK_ACCENT; }
    protected int themeAccentDim() { return isLightMode() ? LIGHT_ACCENT_DIM : DARK_ACCENT_DIM; }
    protected int themeAccentGlow() { return isLightMode() ? LIGHT_ACCENT_GLOW : DARK_ACCENT_GLOW; }
    protected int themeBg() { return isLightMode() ? LIGHT_BG : DARK_BG; }
    protected int themeBgPanel() { return isLightMode() ? LIGHT_BG_PANEL : DARK_BG_PANEL; }
    protected int themeBorder() { return isLightMode() ? LIGHT_BORDER : DARK_BORDER; }
    protected int themeBorderBright() { return isLightMode() ? LIGHT_BORDER_BRIGHT : DARK_BORDER_BRIGHT; }
    protected int themeText() { return isLightMode() ? LIGHT_TEXT : DARK_TEXT; }
    protected int themeTextDim() { return isLightMode() ? LIGHT_TEXT_DIM : DARK_TEXT_DIM; }
    protected int themeGrid() { return isLightMode() ? LIGHT_GRID : DARK_GRID; }
    protected int themeDivider() { return isLightMode() ? LIGHT_DIVIDER : DARK_DIVIDER; }
    protected int themeBtnFill() { return isLightMode() ? LIGHT_BTN_FILL : DARK_BTN_FILL; }
    protected int themeBtnHover() { return isLightMode() ? LIGHT_BTN_HOVER : DARK_BTN_HOVER; }
    protected int themeBtnBorder() { return isLightMode() ? LIGHT_BTN_BORDER : DARK_BTN_BORDER; }
    protected int themeBtnPrimaryFill() { return isLightMode() ? LIGHT_BTN_PRIMARY_FILL : DARK_BTN_PRIMARY_FILL; }
    protected int themeBtnPrimaryHover() { return isLightMode() ? LIGHT_BTN_PRIMARY_HOVER : DARK_BTN_PRIMARY_HOVER; }

    protected void styleTextField(net.minecraft.client.gui.widget.TextFieldWidget field) {
        field.setDrawsBackground(false);
        field.setEditableColor(isLightMode() ? 0xFF1A1A2E : 0xFFE0E0E0);
        field.setUneditableColor(isLightMode() ? 0xFF707080 : 0xFF707070);
        field.setTextShadow(!isLightMode());
    }

    protected void renderStyledTextFieldBackground(DrawContext context, net.minecraft.client.gui.widget.TextFieldWidget field) {
        int x = field.getX() - s(2);
        int y = field.getY() - s(1);
        int w = field.getWidth() + s(4);
        int h = field.getHeight() + s(2);
        int bg = isLightMode() ? 0xFFF0F4F8 : 0x221A2A3C;
        int border = field.isFocused() ? themeAccent() : themeBorder();
        context.fill(x, y, x + w, y + h, bg);
        context.fill(x, y, x + w, y + 1, border);
        context.fill(x, y + h - 1, x + w, y + h, border);
        context.fill(x, y, x + 1, y + h, border);
        context.fill(x + w - 1, y, x + w, y + h, border);
    }

    // --- Layout ---
    @Override
    protected void init() {
        arButtons.clear();

        int targetWidth = BASE_AR_WIDTH;
        int targetHeight = BASE_AR_HEIGHT;

        float fitScale = Math.min(1.0F, Math.min(
                (this.width - SCREEN_MARGIN * 2) / (float) targetWidth,
                (this.height - SCREEN_MARGIN * 2) / (float) targetHeight
        ));
        uiScale = Math.max(0.4F, fitScale);
        textScale = Math.min(1.0F, uiScale);

        panelWidth = Math.max(1, Math.round(targetWidth * uiScale));
        panelHeight = Math.max(1, Math.round(targetHeight * uiScale));

        // Centered
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        int inset = s(8);
        int topInset = s(6);
        int hudHeight = s(16);
        int bottomInset = s(6) + hudHeight + s(4);
        contentX = panelX + inset;
        contentY = panelY + topInset;
        contentWidth = Math.max(1, panelWidth - inset * 2);
        contentHeight = Math.max(1, panelHeight - topInset - bottomInset);
        hudBarY = panelY + panelHeight - s(6) - hudHeight;

        if (openAnimStartMs < 0L && areAnimationsEnabled()) {
            openAnimStartMs = System.currentTimeMillis();
        }
        closeAnimStartMs = -1L;
        closingToParent = false;
        lastKnownLightMode = isLightMode();

        // Randomize battery on first open
        batteryPercent = 80 + (int) (Math.random() * 11); // 80-90%
        // Cache skin texture to avoid re-querying every frame
        if (client != null && client.player != null) {
            try {
                cachedSkinTexture = client.player.getSkin().body().texturePath();
            } catch (Throwable ignored) {
                cachedSkinTexture = null;
            }
        }
    }

    @Override
    public void tick() {
        if (closingToParent && getCloseProgress() >= 1.0F && client != null) {
            client.setScreen(parent);
        }
        // Theme change detection for scan animation (issue #6)
        boolean currentLight = isLightMode();
        if (currentLight != lastKnownLightMode) {
            lastKnownLightMode = currentLight;
            themeScanStartMs = System.currentTimeMillis();
        }
    }

    // --- Scaling helpers ---
    protected int s(int base) { return Math.max(1, Math.round(base * uiScale)); }
    protected float getTextScale() { return textScale; }
    protected float getUIScale() { return uiScale; }

    protected int scaledFontHeight() {
        return Math.max(1, Math.round(textRenderer.fontHeight * textScale));
    }

    protected int scaledTextWidth(String text) {
        return Math.max(1, Math.round(textRenderer.getWidth(text) * textScale));
    }

    // --- Animation helpers ---
    protected boolean areAnimationsEnabled() { return PhoneSettingsClient.isAnimationsEnabled(); }

    private float getOpenProgress() {
        if (!areAnimationsEnabled() || openAnimStartMs < 0L) return 1.0F;
        long elapsed = System.currentTimeMillis() - openAnimStartMs;
        return MathHelper.clamp(elapsed / (float) OPEN_ANIM_DURATION_MS, 0.0F, 1.0F);
    }

    private float getCloseProgress() {
        if (!areAnimationsEnabled() || closeAnimStartMs < 0L) return 0.0F;
        long elapsed = System.currentTimeMillis() - closeAnimStartMs;
        return MathHelper.clamp(elapsed / (float) CLOSE_ANIM_DURATION_MS, 0.0F, 1.0F);
    }

    protected float getAppLaunchProgress() {
        if (appLaunchAnim == null || appLaunchStartMs < 0L) return 1.0F;
        long elapsed = System.currentTimeMillis() - appLaunchStartMs;
        return MathHelper.clamp(elapsed / (float) APP_LAUNCH_DURATION_MS, 0.0F, 1.0F);
    }

    public void setAppLaunchAnimation(int x, int y, int w, int h) {
        this.appLaunchAnim = new AppLaunchAnim(x, y, w, h);
        this.appLaunchStartMs = -1L;
    }

    protected boolean isClosingToParent() { return closingToParent; }

    // --- Disable vanilla blur ---
    @Override
    public boolean shouldPause() { return false; }

    // --- Text rendering ---
    protected void drawScaledText(DrawContext context, Text text, int x, int y, int color) {
        if (textScale >= 0.99F) {
            if (isLightMode()) {
                context.drawText(textRenderer, text, x, y, color, false);
            } else {
                context.drawTextWithShadow(textRenderer, text, x, y, color);
            }
        } else {
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
    }

    protected void drawScaledCenteredText(DrawContext context, Text text, int centerX, int y, int color) {
        int w = scaledTextWidth(text.getString());
        drawScaledText(context, text, centerX - w / 2, y, color);
    }

    // --- Rendering ---
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // No renderBackground - AR is transparent overlay
        float openProg = getOpenProgress();
        float closeProg = getCloseProgress();
        boolean animatingOpen = openProg < 1.0F;
        boolean animatingClose = closingToParent && closeProg < 1.0F;

        if (animatingOpen) {
            // Spatial expand animation (Apple Vision Pro style)
            float ease = easeOutCubic(openProg);
            float scale = 0.85F + 0.15F * ease;
            int alpha = Math.round(255 * ease);

            context.getMatrices().pushMatrix();
            float cx = panelX + panelWidth / 2.0F;
            float cy = panelY + panelHeight / 2.0F;
            context.getMatrices().translate(cx, cy);
            context.getMatrices().scale(scale, scale);
            context.getMatrices().translate(-cx, -cy);

            renderARPanel(context, mouseX, mouseY, delta, alpha);
            context.getMatrices().popMatrix();

            // Scan line sweeps downward during opening (issue #6)
            int scanY = panelY + (int) Math.round(panelHeight * openProg);
            if (scanY >= panelY && scanY < panelY + panelHeight) {
                int scanAlpha = Math.round(180 * (1.0F - openProg));
                int accentRGB = themeAccent() & 0x00FFFFFF;
                context.fill(panelX, scanY, panelX + panelWidth, scanY + Math.max(1, s(2)),
                        (scanAlpha << 24) | accentRGB);
            }
            return;
        }

        if (animatingClose) {
            float ease = easeInCubic(closeProg);
            float scale = 1.0F - 0.15F * ease;
            int alpha = Math.round(255 * (1.0F - ease));

            context.getMatrices().pushMatrix();
            float cx = panelX + panelWidth / 2.0F;
            float cy = panelY + panelHeight / 2.0F;
            context.getMatrices().translate(cx, cy);
            context.getMatrices().scale(scale, scale);
            context.getMatrices().translate(-cx, -cy);

            renderARPanel(context, mouseX, mouseY, delta, alpha);
            context.getMatrices().popMatrix();

            // Scan line sweeps upward during closing (issue #6)
            int scanY = panelY + panelHeight - (int) Math.round(panelHeight * closeProg);
            if (scanY > panelY && scanY <= panelY + panelHeight) {
                int scanAlpha = Math.round(180 * closeProg);
                int accentRGB = themeAccent() & 0x00FFFFFF;
                context.fill(panelX, scanY, panelX + panelWidth, scanY + Math.max(1, s(2)),
                        (scanAlpha << 24) | accentRGB);
            }
            return;
        }

        // App launch transition
        if (appLaunchAnim != null && appLaunchStartMs >= 0L) {
            float prog = getAppLaunchProgress();
            if (prog < 1.0F) {
                float ease = easeOutCubic(prog);
                float sx = MathHelper.lerp(ease, appLaunchAnim.w / (float) Math.max(1, panelWidth), 1.0F);
                float sy = MathHelper.lerp(ease, appLaunchAnim.h / (float) Math.max(1, panelHeight), 1.0F);
                float tx = MathHelper.lerp(ease, appLaunchAnim.x, panelX);
                float ty = MathHelper.lerp(ease, appLaunchAnim.y, panelY);

                context.getMatrices().pushMatrix();
                context.getMatrices().translate(tx - panelX * sx, ty - panelY * sy);
                context.getMatrices().scale(sx, sy);
                renderARPanel(context, mouseX, mouseY, delta, 255);
                context.getMatrices().popMatrix();
                return;
            }
            appLaunchAnim = null;
        }

        renderARPanel(context, mouseX, mouseY, delta, 255);

        // Theme switch scan overlay (issue #6)
        if (themeScanStartMs > 0L) {
            long elapsed = System.currentTimeMillis() - themeScanStartMs;
            float prog = Math.min(1.0F, elapsed / (float) THEME_SCAN_DURATION_MS);
            int scanY = panelY + (int) Math.round(panelHeight * prog);
            if (scanY >= panelY && scanY < panelY + panelHeight) {
                int scanAlpha = Math.round(160 * (1.0F - prog));
                int accentRGB = themeAccent() & 0x00FFFFFF;
                context.fill(panelX, scanY, panelX + panelWidth, scanY + Math.max(1, s(2)),
                        (scanAlpha << 24) | accentRGB);
            }
            if (prog >= 1.0F) {
                themeScanStartMs = -1L;
            }
        }
    }

    private void renderARPanel(DrawContext context, int mouseX, int mouseY, float delta, int masterAlpha) {
        int chamfer = s(AR_CHAMFER);
        int borderColor = applyAlpha(themeBorder(), masterAlpha);
        int accentColor = applyAlpha(themeAccentDim(), masterAlpha);

        // AR background mask (when enabled in settings)
        if (PhoneSettingsClient.isARMaskEnabled()) {
            // Semi-transparent background overlay (very low alpha)
            int maskAlpha = Math.round(0x20 * masterAlpha / 255.0F);
            int bgColor = isLightMode()
                    ? ((maskAlpha << 24) | 0x0040607A)
                    : ((maskAlpha << 24) | 0x00010508);
            context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, bgColor);
            // Grid lines matching phone theme
            int gridColor = isLightMode()
                    ? applyAlpha(0x180060AA, masterAlpha)
                    : applyAlpha(0x1200FFE0, masterAlpha);
            int gridStep = s(12);
            for (int gx = panelX; gx < panelX + panelWidth; gx += gridStep) {
                context.fill(gx, panelY, gx + 1, panelY + panelHeight, gridColor);
            }
            for (int gy = panelY; gy < panelY + panelHeight; gy += gridStep) {
                context.fill(panelX, gy, panelX + panelWidth, gy + 1, gridColor);
            }
        }

        // Border only (subtle frame outline)
        drawChamferedBorder(context, panelX, panelY, panelWidth, panelHeight, chamfer, borderColor, accentColor);

        // Render content
        renderARContent(context, mouseX, mouseY, delta);

        // Render bottom info HUD
        renderBottomHud(context);

        // Render buttons
        for (ARButtonRenderData btn : arButtons) {
            renderARButton(context, mouseX, mouseY, btn);
        }

        renderHelpModeOverlay(context, mouseX, mouseY);
    }

    protected abstract void renderARContent(DrawContext context, int mouseX, int mouseY, float delta);

    protected final boolean isHelpModeActive() {
        return helpModeActive;
    }

    protected void onHelpModeChanged(boolean helpModeActive) {
    }

    protected Text getCustomHelpTooltip(int mouseX, int mouseY) {
        return Text.empty();
    }

    private void renderHelpModeOverlay(DrawContext context, int mouseX, int mouseY) {
        if (!helpModeActive) {
            return;
        }
        context.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight,
                isLightMode() ? 0x12FFFFFF : 0x22000000);
        Text tooltip = getHoveredHelpTooltip(mouseX, mouseY);
        if (tooltip == null || tooltip.getString().isBlank()) {
            return;
        }
        renderHelpTooltip(context, mouseX, mouseY, tooltip);
    }

    private Text getHoveredHelpTooltip(int mouseX, int mouseY) {
        for (ARButtonRenderData data : arButtons) {
            if (!data.helpEnabled()) {
                continue;
            }
            if (isInside(mouseX, mouseY, data.x(), data.y(), data.width(), data.height())) {
                Text helpText = data.helpTextSupplier().get();
                if (!helpText.getString().isBlank()) {
                    return helpText;
                }
            }
        }
        Text customTooltip = getCustomHelpTooltip(mouseX, mouseY);
        return customTooltip.getString().isBlank() ? null : customTooltip;
    }

    private void renderHelpTooltip(DrawContext context, int mouseX, int mouseY, Text tooltip) {
        int maxWidth = Math.max(s(96), Math.min(s(188), contentWidth - s(20)));
        List<OrderedText> lines = textRenderer.wrapLines(tooltip, Math.max(48, maxWidth));
        if (lines.isEmpty()) {
            return;
        }
        int lineHeight = textRenderer.fontHeight + s(1);
        int textWidth = 0;
        for (OrderedText line : lines) {
            textWidth = Math.max(textWidth, textRenderer.getWidth(line));
        }
        int paddingX = s(6);
        int paddingY = s(5);
        int boxWidth = textWidth + paddingX * 2;
        int boxHeight = lines.size() * lineHeight + paddingY * 2 - s(1);
        int boxX = MathHelper.clamp(mouseX + s(10), panelX + s(4), panelX + panelWidth - boxWidth - s(4));
        int boxY = MathHelper.clamp(mouseY + s(10), panelY + s(4), panelY + panelHeight - boxHeight - s(4));
        int cut = Math.max(2, s(4));
        int fillColor = isLightMode() ? 0xFFF0F4F8 : 0xFF101825;
        int borderColor = themeAccent();

        for (int row = 0; row < boxHeight; row++) {
            int leftCut = row < cut ? cut - row : 0;
            int rightCut = row >= boxHeight - cut ? cut - (boxHeight - 1 - row) : 0;
            context.fill(boxX + leftCut, boxY + row, boxX + boxWidth - rightCut, boxY + row + 1, fillColor);
        }
        context.fill(boxX + cut, boxY, boxX + boxWidth, boxY + 1, borderColor);
        context.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth - cut, boxY + boxHeight, borderColor);
        for (int index = 0; index < cut; index++) {
            context.fill(boxX + cut - index, boxY + index, boxX + cut - index + 1, boxY + index + 1, borderColor);
            context.fill(boxX + boxWidth - cut + index - 1, boxY + boxHeight - 1 - index,
                    boxX + boxWidth - cut + index, boxY + boxHeight - index, borderColor);
        }

        int textY = boxY + paddingY;
        for (OrderedText line : lines) {
            context.drawText(textRenderer, line, boxX + paddingX, textY, isLightMode() ? themeText() : 0xFFEAF7FF, false);
            textY += lineHeight;
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    // --- Bottom Info HUD ---
    private void renderBottomHud(DrawContext context) {
        if (client == null || client.player == null) return;

        int hudH = s(16);
        int hx = contentX;
        int hy = hudBarY;
        int hw = contentWidth;

        // Subtle divider line above the HUD
        int divColor = isLightMode() ? 0x44A0B8CC : 0x4400C8BE;
        context.fill(hx, hy - 1, hx + hw, hy, divColor);

        // --- Left side: player head + name + alive status ---
        int headSize = Math.max(4, hudH - s(2));
        int headX = hx + s(2);
        int headY = hy + (hudH - headSize) / 2;

        // Draw player head (face + hat layers)
        try {
            Identifier skinTexture = cachedSkinTexture;
            if (skinTexture == null && client.player != null) {
                skinTexture = client.player.getSkin().body().texturePath();
                cachedSkinTexture = skinTexture;
            }
            int hatExpand = Math.max(1, s(1));
            // Face layer
            context.drawTexture(RenderPipelines.GUI_TEXTURED, skinTexture,
                    headX, headY, (float) FACE_U, (float) FACE_V,
                    headSize, headSize, 8, 8, SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE, -1);
            // Hat layer (slightly larger)
            context.drawTexture(RenderPipelines.GUI_TEXTURED, skinTexture,
                    headX - hatExpand, headY - hatExpand,
                    (float) HAT_U, (float) HAT_V,
                    headSize + hatExpand * 2, headSize + hatExpand * 2,
                    8, 8, SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE, -1);
        } catch (Throwable ignored) {
            // Fallback: simple colored square
            context.fill(headX, headY, headX + headSize, headY + headSize,
                    isLightMode() ? 0xFF4A90E2 : 0xFF00B4A0);
        }

        // Player name
        String playerName = client.player != null ? client.player.getName().getString() : "";
        if (playerName == null) playerName = "";
        int nameX = headX + headSize + s(4);
        int nameY = hy + (hudH - scaledFontHeight()) / 2;
        drawScaledText(context, Text.literal(playerName), nameX, nameY, themeText());

        // Alive status - shield icon
        boolean dead = DeathSyncClient.isLocalPlayerDead();
        int statusColor = dead ? 0xFFFF4444 : 0xFF44FF88;
        int shieldSize = Math.max(4, hudH - s(4));
        int statusX = nameX + scaledTextWidth(playerName) + s(6);
        int shieldCY = hy + hudH / 2;
        drawShieldIcon(context, statusX + shieldSize / 2, shieldCY, shieldSize, statusColor);

        // --- Right side: time + signal + battery ---
        int rightX = hx + hw - s(2);

        // Battery: percentage text + outline icon
        String batteryStr = batteryPercent + "%";
        int batteryTextW = scaledTextWidth(batteryStr);
        int batteryIconW = s(10);
        int batteryIconH = s(6);
        int batteryTotalW = batteryTextW + s(2) + batteryIconW + s(2);
        int batteryX = rightX - batteryTotalW;

        // Battery icon
        int biX = rightX - batteryIconW - s(2);
        int biY = hy + (hudH - batteryIconH) / 2;
        // Outline
        context.fill(biX, biY, biX + batteryIconW, biY + 1, themeTextDim());
        context.fill(biX, biY + batteryIconH - 1, biX + batteryIconW, biY + batteryIconH, themeTextDim());
        context.fill(biX, biY, biX + 1, biY + batteryIconH, themeTextDim());
        context.fill(biX + batteryIconW - 1, biY, biX + batteryIconW, biY + batteryIconH, themeTextDim());
        // Nub on right
        int nubH = Math.max(2, batteryIconH - s(3));
        int nubY = biY + (batteryIconH - nubH) / 2;
        context.fill(biX + batteryIconW, nubY, biX + batteryIconW + Math.max(1, s(1)), nubY + nubH, themeTextDim());
        // Fill level
        int fillW = Math.max(1, (int) ((batteryIconW - 2) * batteryPercent / 100.0));
        int fillColor = batteryPercent > 20 ? (isLightMode() ? 0xFF22AA44 : 0xFF44FF88) : 0xFFFF4444;
        context.fill(biX + 1, biY + 1, biX + 1 + fillW, biY + batteryIconH - 1, fillColor);

        // Battery percentage text
        drawScaledText(context, Text.literal(batteryStr), batteryX, nameY, themeTextDim());

        // Signal bars (decorative, 4 bars)
        int signalW = s(12);
        int signalX = batteryX - signalW - s(6);
        int signalBaseY = hy + hudH - s(4);
        int barW = Math.max(1, s(2));
        int barGap = Math.max(1, s(1));
        int signalColor = isLightMode() ? 0xFF0099CC : 0xFF00FFE0;
        for (int i = 0; i < 4; i++) {
            int barH = Math.max(1, s(3) + i * s(2));
            int bx = signalX + i * (barW + barGap);
            int by = signalBaseY - barH;
            context.fill(bx, by, bx + barW, signalBaseY, signalColor);
        }

        // Time display (real-world time)
        String timeStr = LocalTime.now().format(TIME_FORMATTER);
        int timeW = scaledTextWidth(timeStr);
        int timeX = signalX - timeW - s(6);
        drawScaledText(context, Text.literal(timeStr), timeX, nameY, themeTextDim());
    }

    // --- Button system ---
    protected record ARButtonRenderData(ButtonWidget button, int x, int y, int width, int height,
                                        Text message, ARButtonVariant variant, BooleanSupplier selectedSupplier,
                                        Supplier<Text> helpTextSupplier, boolean helpEnabled, boolean helpControl) {}

    public enum ARButtonVariant { DEFAULT, PRIMARY, GHOST }

    protected ButtonWidget addARButton(Text label, int x, int y, int w, int h, ARButtonVariant variant,
                                       BooleanSupplier selected, ButtonWidget.PressAction action) {
        return addARButton(label, x, y, w, h, variant, selected, () -> ScreenHelpText.describeAction(label), true, false, action);
    }

    protected ButtonWidget addARButton(Text label, int x, int y, int w, int h, ARButtonVariant variant,
                                       BooleanSupplier selected, Supplier<Text> helpTextSupplier,
                                       boolean helpEnabled, boolean helpControl, ButtonWidget.PressAction action) {
        ButtonWidget button = ButtonWidget.builder(label, action).dimensions(x, y, w, h).build();
        // Keep visible=true so clicks work (isInteractable checks visible && active).
        // Since render() is fully overridden without super.render(), buttons won't draw vanilla appearance.
        addDrawableChild(button);
        arButtons.add(new ARButtonRenderData(button, x, y, w, h, label, variant, selected,
                helpTextSupplier, helpEnabled, helpControl));
        return button;
    }

    protected ButtonWidget addARButton(Text label, int x, int y, int w, int h, ButtonWidget.PressAction action) {
        return addARButton(label, x, y, w, h, ARButtonVariant.DEFAULT, () -> false, action);
    }

    protected ButtonWidget addARPrimaryButton(Text label, int x, int y, int w, int h, ButtonWidget.PressAction action) {
        return addARButton(label, x, y, w, h, ARButtonVariant.PRIMARY, () -> false, action);
    }

    protected ButtonWidget addARGhostButton(Text label, int x, int y, int w, int h, ButtonWidget.PressAction action) {
        return addARButton(label, x, y, w, h, ARButtonVariant.GHOST, () -> false, action);
    }

    protected ButtonWidget addARGhostButton(Text label, int x, int y, int w, int h, Text helpText, ButtonWidget.PressAction action) {
        return addARButton(label, x, y, w, h, ARButtonVariant.GHOST, () -> false, () -> helpText, true, false, action);
    }

    private void renderARButton(DrawContext context, int mouseX, int mouseY, ARButtonRenderData data) {
        int x = data.x, y = data.y, w = data.width, h = data.height;
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        boolean active = data.button.active;
        boolean selected = data.selectedSupplier.getAsBoolean();

        if (data.variant == ARButtonVariant.GHOST && !hovered && !selected) {
            String label = data.message.getString();
            if (!label.isBlank()) {
                int ty = y + Math.max(0, (h - scaledFontHeight()) / 2);
                drawScaledCenteredText(context, data.message, x + w / 2, ty, active ? themeTextDim() : 0xFF3A4A5A);
            }
            return;
        }

        int borderColor, fillColor, textColor;
        if (!active) {
            borderColor = 0x44334455;
            fillColor = 0x220A1018;
            textColor = 0xFF3A4A5A;
        } else if (data.variant == ARButtonVariant.PRIMARY) {
            borderColor = hovered ? themeAccent() : themeBtnBorder();
            fillColor = hovered ? themeBtnPrimaryHover() : themeBtnPrimaryFill();
            textColor = themeText();
        } else if (selected) {
            borderColor = themeAccent();
            fillColor = hovered ? themeBtnHover() : themeBtnFill();
            textColor = themeText();
        } else {
            borderColor = hovered ? themeBtnBorder() : themeBorder();
            fillColor = hovered ? themeBtnHover() : themeBtnFill();
            textColor = hovered ? themeText() : themeTextDim();
        }

        // Angular tech button: 4-line border + fill (#9)
        int cut = Math.max(2, Math.min(h / 3, s(4)));
        // Fill with angular corners
        for (int row = 0; row < h; row++) {
            int leftCut = 0, rightCut = 0;
            if (row < cut) leftCut = cut - row;
            if (row >= h - cut) rightCut = cut - (h - 1 - row);
            context.fill(x + leftCut, y + row, x + w - rightCut, y + row + 1, fillColor);
        }
        // 4-line border: top-left diagonal + top edge + bottom-right diagonal + bottom edge
        context.fill(x + cut, y, x + w, y + 1, borderColor);
        context.fill(x, y + h - 1, x + w - cut, y + h, borderColor);
        for (int i = 0; i < cut; i++) {
            context.fill(x + cut - i, y + i, x + cut - i + 1, y + i + 1, borderColor);
        }
        for (int i = 0; i < cut; i++) {
            context.fill(x + w - cut + i - 1, y + h - 1 - i, x + w - cut + i, y + h - i, borderColor);
        }

        // Label
        String label = data.message.getString();
        if (!label.isBlank()) {
            int ty = y + Math.max(0, (h - scaledFontHeight()) / 2);
            drawScaledCenteredText(context, data.message, x + w / 2, ty, textColor);
        }
    }

    // --- Navigation ---
    @Override
    public void close() {
        if (areAnimationsEnabled() && !closingToParent) {
            closingToParent = true;
            closeAnimStartMs = System.currentTimeMillis();
            return;
        }
        if (client != null) {
            client.setScreen(parent);
        }
    }

    // --- Back button support ---
    protected void addBackButton() {
        int btnW = s(40);
        int btnH = s(16);
        int bx = contentX + s(2);
        int by = contentY + s(2);
        addARGhostButton(Text.translatable("phone.tzz_mod.back"), bx, by, btnW, btnH, btn -> close());
        ButtonWidget helpButton = addARButton(
                Text.literal("?"),
                bx + btnW + s(4),
                by,
                btnH,
                btnH,
                ARButtonVariant.DEFAULT,
                () -> helpModeActive,
                () -> helpModeActive
                        ? Text.translatable("phone.tzz_mod.help.exit_mode")
                        : Text.translatable("phone.tzz_mod.help.enter_mode"),
                true,
                true,
                btn -> toggleHelpMode()
        );
        helpButtonData = findButtonData(helpButton);
    }

    private void toggleHelpMode() {
        helpModeActive = !helpModeActive;
        onHelpModeChanged(helpModeActive);
    }

    // --- Drawing utilities ---
    protected static void fillChamferedRect(DrawContext context, int x, int y, int w, int h, int chamfer, int color) {
        if (w <= 0 || h <= 0) return;
        chamfer = Math.min(chamfer, Math.min(w / 2, h / 2));
        context.fill(x + chamfer, y, x + w - chamfer, y + h, color);
        context.fill(x, y + chamfer, x + w, y + h - chamfer, color);
        for (int i = 0; i < chamfer; i++) {
            int offset = chamfer - i;
            context.fill(x + offset, y + i, x + w - offset, y + i + 1, color);
            context.fill(x + offset, y + h - 1 - i, x + w - offset, y + h - i, color);
        }
    }

    private void drawChamferedBorder(DrawContext context, int x, int y, int w, int h, int chamfer, int borderColor, int accentColor) {
        // Top edge: full width from chamfer to right wall (like drawAngularTechFrame, issue #5)
        context.fill(x + chamfer, y, x + w, y + 1, accentColor);
        // Bottom edge: full width from left wall to chamfer before right (issue #5)
        context.fill(x, y + h - 1, x + w - chamfer, y + h, accentColor);
        // Top-left corner diagonal
        for (int i = 0; i < chamfer; i++) {
            int offset = chamfer - i;
            context.fill(x + offset, y + i, x + offset + 1, y + i + 1, accentColor);
        }
        // Bottom-right corner diagonal
        for (int i = 0; i < chamfer; i++) {
            context.fill(x + w - chamfer + i - 1, y + h - 1 - i, x + w - chamfer + i, y + h - i, accentColor);
        }
    }

    protected static int applyAlpha(int color, int masterAlpha) {
        if (masterAlpha >= 255) return color;
        int a = (color >>> 24) & 0xFF;
        a = (a * masterAlpha) / 255;
        return (a << 24) | (color & 0x00FFFFFF);
    }

    protected static int lerpColor(int from, int to, float t) {
        t = MathHelper.clamp(t, 0.0F, 1.0F);
        int a = (int) MathHelper.lerp(t, (from >>> 24) & 0xFF, (to >>> 24) & 0xFF);
        int r = (int) MathHelper.lerp(t, (from >>> 16) & 0xFF, (to >>> 16) & 0xFF);
        int g = (int) MathHelper.lerp(t, (from >>> 8) & 0xFF, (to >>> 8) & 0xFF);
        int b = (int) MathHelper.lerp(t, from & 0xFF, to & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // --- Easing functions ---
    private static float easeOutCubic(float t) {
        float f = 1.0F - t;
        return 1.0F - f * f * f;
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    // --- Scrollbar helper ---
    protected void renderScrollbar(DrawContext context, int listTop, int listBottom, int totalHeight, int scrollOffset) {
        int visibleHeight = Math.max(1, listBottom - listTop);
        if (totalHeight <= visibleHeight) return;
        int trackX = contentX + contentWidth - s(2);
        context.fill(trackX, listTop, trackX + 1, listBottom, themeBorder());
        int thumbH = Math.max(s(12), Math.round(visibleHeight * (visibleHeight / (float) totalHeight)));
        int thumbY = listTop + Math.round((scrollOffset / (float) (totalHeight - visibleHeight)) * (visibleHeight - thumbH));
        thumbY = MathHelper.clamp(thumbY, listTop, listBottom - thumbH);
        context.fill(trackX, thumbY, trackX + 1, thumbY + thumbH, themeAccentDim());
    }

    // --- Resource check ---
    protected boolean hasResource(net.minecraft.util.Identifier id) {
        if (client == null) return false;
        var opt = client.getResourceManager().getResource(id);
        return opt.isPresent();
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubleClick) {
        if (helpModeActive) {
            if (helpButtonData != null && isInside((int) click.x(), (int) click.y(), helpButtonData.x(), helpButtonData.y(), helpButtonData.width(), helpButtonData.height())) {
                toggleHelpMode();
            }
            return true;
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (helpModeActive) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (helpModeActive) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (helpModeActive && input.key() != GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        return super.keyPressed(input);
    }

    private ARButtonRenderData findButtonData(ButtonWidget button) {
        for (ARButtonRenderData data : arButtons) {
            if (data.button() == button) {
                return data;
            }
        }
        return null;
    }

    /**
     * Draws a shield icon centered at (cx, cy) with given size and color.
     * Top part is rectangular, bottom part narrows to a point.
     */
    protected static void drawShieldIcon(DrawContext context, int cx, int cy, int size, int color) {
        int w = Math.max(3, size);
        int h = Math.max(4, size + size / 4); // slightly taller than wide
        int x = cx - w / 2;
        int y = cy - h / 2;
        int bodyH = h * 3 / 5;
        // Upper rectangular part
        context.fill(x, y, x + w, y + bodyH, color);
        // Lower tapered part (triangle pointing down)
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
     * Draws an angular tech-style frame with cut corners (trapezoid/polygon).
     * Unlike chamfered rects, this draws sharp diagonal cut corners for a HUD aesthetic.
     */
    protected static void drawAngularTechFrame(DrawContext context, int x, int y, int w, int h,
                                               int cut, int fillColor, int borderColor) {
        if (w <= 0 || h <= 0) return;
        cut = Math.min(cut, Math.min(w / 2, h / 2));
        // Fill: top-left and bottom-right corners are cut
        for (int row = 0; row < h; row++) {
            int leftCut = 0;
            int rightCut = 0;
            // Top-left diagonal cut
            if (row < cut) leftCut = cut - row;
            // Bottom-right diagonal cut
            if (row >= h - cut) rightCut = cut - (h - 1 - row);
            context.fill(x + leftCut, y + row, x + w - rightCut, y + row + 1, fillColor);
        }
        // Border: only top edge + top-left diagonal + bottom edge + bottom-right diagonal
        // Top edge (from top-left cut to right end)
        context.fill(x + cut, y, x + w, y + 1, borderColor);
        // Bottom edge (from left end to bottom-right cut)
        context.fill(x, y + h - 1, x + w - cut, y + h, borderColor);
        // Top-left diagonal
        for (int i = 0; i < cut; i++) {
            context.fill(x + cut - i, y + i, x + cut - i + 1, y + i + 1, borderColor);
        }
        // Bottom-right diagonal
        for (int i = 0; i < cut; i++) {
            context.fill(x + w - cut + i - 1, y + h - 1 - i, x + w - cut + i, y + h - i, borderColor);
        }
    }

    /**
     * Draws an angular tech frame with strong theme accent border and appropriate panel fill.
     * Convenience wrapper for content panels and text input areas.
     */
    protected void drawARPanelFrame(DrawContext context, int x, int y, int w, int h) {
        int cut = Math.max(1, s(3));
        int fill = isLightMode() ? 0xCCE8EDF4 : 0xCC081018;
        drawAngularTechFrame(context, x, y, w, h, cut, fill, themeAccent());
    }

    /**
     * Draws an angular tech frame for a highlighted/active input area (bright accent border).
     */
    protected void drawARInputFrame(DrawContext context, int x, int y, int w, int h) {
        int cut = Math.max(1, s(2));
        int fill = isLightMode() ? 0xDDE8EDF4 : 0xDD060D18;
        drawAngularTechFrame(context, x, y, w, h, cut, fill, themeBorderBright());
    }

    // Internal record for app launch animation
    private record AppLaunchAnim(int x, int y, int w, int h) {}

    // --- Text wrapping utility ---
    protected List<String> wrapText(String text, int maxWidth) {
        if (text == null || text.isEmpty()) return List.of();
        List<String> lines = new ArrayList<>();
        int rawMaxWidth = Math.max(1, Math.round(maxWidth / textScale));
        StringBuilder line = new StringBuilder();
        int lineWidth = 0;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            int chWidth = textRenderer.getWidth(ch);
            if (lineWidth + chWidth > rawMaxWidth && lineWidth > 0) {
                lines.add(line.toString());
                line = new StringBuilder();
                lineWidth = 0;
            }
            line.append(ch);
            lineWidth += chWidth;
            i += Character.charCount(cp);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }
}
