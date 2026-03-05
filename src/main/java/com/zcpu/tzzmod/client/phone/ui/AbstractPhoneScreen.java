package com.zcpu.tzzmod.client.phone.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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

    protected AbstractPhoneScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        int margin = SCREEN_MARGIN;

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
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw a thin rounded-line border: straight sides + stroked corner arcs (no filled center)
        drawLineBorder(context, phoneX, phoneY, phoneWidth, phoneHeight);

        renderStatusBar(context);
        // Call the phone content render hook (subclasses should only draw text here).
        renderPhoneContent(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderStatusBar(DrawContext context) {
        int statusX = contentX;
        int statusY = phoneY + s(8);
        int statusWidth = contentWidth;
        int statusHeight = s(STATUS_BAR_HEIGHT);

        String timeText = LocalTime.now().format(TIME_FORMATTER);
        String playerName = client != null && client.player != null
                ? client.player.getName().getString()
                : "Player";

        context.drawTextWithShadow(textRenderer, timeText, statusX + s(6), statusY + s(4), 0xFFECECEC);

        String shownName = textRenderer.trimToWidth(playerName, Math.max(s(10), statusWidth - s(40)));
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(shownName), statusX + statusWidth / 2, statusY + s(4), 0xFFECECEC);

        // Keep only the status divider line.
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

    private void drawLineBorder(DrawContext context, int x, int y, int width, int height) {
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

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
