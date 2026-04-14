package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * AR-specific compass screen. Rotating directional indicator in tech HUD style.
 */
public class ARCompassScreen extends AbstractARScreen {

    public ARCompassScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.compass"), parent);
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // Title
        int titleCX = contentX + contentWidth / 2;
        int titleY = contentY + s(2);
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.app.compass"),
                titleCX, titleY, themeAccent());

        // Compass area
        int compassCX = contentX + contentWidth / 2;
        int compassTop = titleY + scaledFontHeight() + s(6);
        int availH = contentY + contentHeight - compassTop;
        int radius = Math.min(contentWidth / 2 - s(10), availH / 2 - s(10));
        int compassCY = compassTop + availH / 2;

        float yaw = getPlayerYaw();

        int dimColor = themeBorder();
        int brightColor = themeAccentDim();

        // --- Fixed dial: NSWE labels at fixed positions (issue #2) ---
        String[] labels = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

        for (int i = 0; i < 8; i++) {
            // Fixed angle: N=top(0°), E=right(90°), S=bottom(180°), W=left(270°)
            double angle = Math.toRadians(i * 45.0);
            double sinA = Math.sin(angle);
            double cosA = Math.cos(angle);
            boolean cardinal = (i % 2 == 0);
            int tickInner = cardinal ? (radius - s(10)) : (radius - s(6));
            int tickOuter = radius;

            // Screen coords: x = cx + sin*r, y = cy - cos*r
            int x1 = compassCX + (int) Math.round(sinA * tickInner);
            int y1 = compassCY - (int) Math.round(cosA * tickInner);
            int x2 = compassCX + (int) Math.round(sinA * tickOuter);
            int y2 = compassCY - (int) Math.round(cosA * tickOuter);
            drawLine(context, x1, y1, x2, y2, cardinal ? brightColor : dimColor);

            if (cardinal) {
                int innerX1 = compassCX + (int) Math.round(sinA * s(6));
                int innerY1 = compassCY - (int) Math.round(cosA * s(6));
                drawLine(context, innerX1, innerY1, x1, y1, dimColor);
            }

            // Labels at fixed positions
            int labelR = tickOuter + s(8);
            int lx = compassCX + (int) Math.round(sinA * labelR);
            int ly = compassCY - (int) Math.round(cosA * labelR);
            int lw = scaledTextWidth(labels[i]);
            int labelColor;
            if (i == 0) {
                labelColor = themeAccent();     // N — accent
            } else if (cardinal) {
                labelColor = themeText();       // E, S, W
            } else {
                labelColor = themeTextDim();    // NE, SE, SW, NW
            }
            drawScaledText(context, Text.literal(labels[i]), lx - lw / 2, ly - scaledFontHeight() / 2, labelColor);
        }

        // Angular dial border: 4-line frame
        int boxR = (int) (radius * 0.82);
        int cut = Math.max(s(6), boxR / 4);
        context.fill(compassCX - boxR + cut, compassCY - boxR, compassCX + boxR, compassCY - boxR + 1, dimColor);
        context.fill(compassCX - boxR, compassCY + boxR - 1, compassCX + boxR - cut, compassCY + boxR, dimColor);
        for (int i = 0; i < cut; i++) {
            context.fill(compassCX - boxR + cut - i, compassCY - boxR + i,
                    compassCX - boxR + cut - i + 1, compassCY - boxR + i + 1, dimColor);
        }
        for (int i = 0; i < cut; i++) {
            context.fill(compassCX + boxR - cut + i - 1, compassCY + boxR - 1 - i,
                    compassCX + boxR - cut + i, compassCY + boxR - i, dimColor);
        }

        // --- Diamond needle pointer (issues #2 + #7) ---
        // Player bearing from north: yaw=0=south → bearing=180° (points down)
        float bearing = ((yaw + 180) % 360 + 360) % 360;
        double bRad = Math.toRadians(bearing);
        // Needle direction in screen coords (y-down)
        double nx = Math.sin(bRad);
        double ny = -Math.cos(bRad);
        // Perpendicular direction
        double px = -ny;
        double py = nx;

        int tipLen = radius - s(10);
        int tailLen = radius / 3;
        int halfWidth = s(5);

        int tipX  = compassCX + (int) Math.round(nx * tipLen);
        int tipY  = compassCY + (int) Math.round(ny * tipLen);
        int tailX = compassCX - (int) Math.round(nx * tailLen);
        int tailY = compassCY - (int) Math.round(ny * tailLen);
        int lwX   = compassCX + (int) Math.round(px * halfWidth);
        int lwY   = compassCY + (int) Math.round(py * halfWidth);
        int rwX   = compassCX - (int) Math.round(px * halfWidth);
        int rwY   = compassCY - (int) Math.round(py * halfWidth);

        // North half (facing) — accent color
        int northColor = themeAccent();
        int southColor = isLightMode() ? 0xFFAAAFC0 : 0xFF8899AA;
        drawLine(context, tipX, tipY, lwX, lwY, northColor);
        drawLine(context, tipX, tipY, rwX, rwY, northColor);
        // South half — dim color
        drawLine(context, tailX, tailY, lwX, lwY, southColor);
        drawLine(context, tailX, tailY, rwX, rwY, southColor);

        // Center dot
        int dotR = Math.max(2, s(2));
        context.fill(compassCX - dotR, compassCY - dotR, compassCX + dotR, compassCY + dotR, themeAccentDim());

        // Direction label box
        int degInt = ((int) bearing) % 360;
        String dirText = getDirectionKey(bearing) + " " + degInt + "°";
        int dirW = scaledTextWidth(dirText) + s(8);
        int dirH = scaledFontHeight() + s(4);
        int dirX = compassCX - dirW / 2;
        int dirY = compassCY + radius + s(14);
        drawAngularTechFrame(context, dirX, dirY, dirW, dirH,
                Math.max(1, s(2)),
                isLightMode() ? 0x88E0E8F0 : 0x88081018,
                dimColor);
        drawScaledCenteredText(context, Text.literal(dirText), compassCX, dirY + s(2), themeAccent());
    }

    private float getPlayerYaw() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            return mc.player.getYaw();
        }
        return 0;
    }

    private String getDirectionKey(float bearing) {
        float norm = ((bearing % 360) + 360) % 360;
        if (norm < 22.5 || norm >= 337.5) return "N";
        if (norm < 67.5) return "NE";
        if (norm < 112.5) return "E";
        if (norm < 157.5) return "SE";
        if (norm < 202.5) return "S";
        if (norm < 247.5) return "SW";
        if (norm < 292.5) return "W";
        return "NW";
    }

    private void drawLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            context.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }
}
