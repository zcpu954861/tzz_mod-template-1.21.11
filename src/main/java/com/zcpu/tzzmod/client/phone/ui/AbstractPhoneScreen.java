package com.zcpu.tzzmod.client.phone.ui;

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

        if (appLaunchAnimation != null && appLaunchAnimationStartedAtMs < 0L) {
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
        boolean animatingOpen = hasAppLaunchAnimation();
        boolean animatingClose = isClosingToParent();
        if ((animatingOpen || animatingClose) && parent != null) {
            parent.render(context, Integer.MIN_VALUE, Integer.MIN_VALUE, delta);
        }

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
            context.getMatrices().translate(translateX, translateY);
            context.getMatrices().scale(scaleX, scaleY);
            renderPhoneScreen(context, renderMouseX, renderMouseY, delta);
            if (animatingClose) {
                renderClosingRevealVeil(context, closeProgress);
            }
            context.getMatrices().popMatrix();
            return;
        }

        renderPhoneScreen(context, renderMouseX, renderMouseY, delta);
    }

    protected void renderPhoneScreen(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw a thin rounded-line border: straight sides + stroked corner arcs (no filled center)
        drawLineBorder(context, phoneX, phoneY, phoneWidth, phoneHeight);

        renderStatusBar(context);
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

        context.drawTextWithShadow(textRenderer, Text.literal(timeText), statusX + s(6), statusY + s(4), 0xFFECECEC);

        String shownName = textRenderer.trimToWidth(playerName, Math.max(s(10), statusWidth - s(40)));
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(shownName), statusX + statusWidth / 2, statusY + s(4), 0xFFECECEC);

        boolean hasDeathTag = com.zcpu.tzzmod.client.DeathSyncClient.isLocalPlayerDead();
        Text statusText = hasDeathTag ? Text.literal("死亡") : Text.literal("存活");
        int statusColor = hasDeathTag ? 0xFFFF6666 : 0xFF66FF66;
        int textWidth = textRenderer.getWidth(statusText);
        int rightPadding = s(6);
        int drawX = statusX + statusWidth - rightPadding - textWidth;
        int drawY = statusY + s(4);
        if (drawX < statusX + s(6)) drawX = statusX + s(6);
        context.drawTextWithShadow(textRenderer, statusText, drawX, drawY, statusColor);

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

    protected boolean hasResource(net.minecraft.util.Identifier identifier) {
        return client != null && client.getResourceManager().getResource(identifier).isPresent();
    }

    protected void drawPhoneTextCenteredFixed(DrawContext context, Text text, int centerX, int y) {
        context.drawCenteredTextWithShadow(textRenderer, text, centerX, y, 0xFFECECEC);
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

        for (int dy = 0; dy <= radius; dy++) {
            double rr = radius * radius - dy * dy;
            if (rr < 0) rr = 0;
            int dxOuter = (int) Math.floor(Math.sqrt(rr));

            int dxInner = 0;
            if (innerR > 0) {
                double ri = innerR * innerR - dy * dy;
                if (ri > 0) dxInner = (int) Math.floor(Math.sqrt(ri));
            }

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
        int radius = Math.max(s(10), s(14));
        int alpha = MathHelper.clamp(Math.round(120.0F * closeProgress), 0, 120);
        RoundedRectRenderer.fillRoundedRect(context, screenX, screenY, screenWidth, screenHeight, radius, alpha << 24);
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
        int radius = Math.max(3, Math.min(height / 2, s(7)));
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        boolean focused = button.isFocused();
        boolean selected = data.selectedSupplier().getAsBoolean();

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
            int textY = y + Math.max(0, (height - textRenderer.fontHeight) / 2);
            context.drawCenteredTextWithShadow(textRenderer, data.message(), x + width / 2, textY, textColor);
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

    @Override
    public void close() {
        if (client == null) {
            return;
        }
        if (parent == null) {
            client.setScreen(null);
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
        appLaunchAnimation = new AppLaunchAnimation(x, y, Math.max(1, width), Math.max(1, height));
        appLaunchAnimationStartedAtMs = -1L;
    }

    protected boolean isTransitionBlockingInteraction() {
        return hasAppLaunchAnimation() || isClosingToParent();
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
