package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class CompassAppScreen extends AbstractPhoneScreen {

    public CompassAppScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.compass"), parent);
    }

    @Override
    protected void init() {
        super.init();
        int buttonY = contentY + contentHeight - s(24);
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, buttonY, s(72), s(20), button -> close());
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.compass"),
                contentX + contentWidth / 2, contentY + s(14));

        // Compass center
        int cx = contentX + contentWidth / 2;
        int availH = contentHeight - s(24) - s(40);
        int radius = Math.min(contentWidth / 2 - s(12), availH / 2 - s(4));
        radius = Math.max(radius, s(18));
        int cy = contentY + s(26) + availH / 2;

        float yaw = getPlayerYaw();

        int dimColor = themeBorder();
        int brightColor = themeAccentDim();

        // --- Fixed dial: NSWE fixed labels + tick marks (issue #2) ---
        String[] labels = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int armGap = s(6);

        for (int i = 0; i < 8; i++) {
            // Fixed angle: N=top(0°), E=right(90°), S=bottom(180°), W=left(270°)
            double angle = Math.toRadians(i * 45.0);
            double sinA = Math.sin(angle);
            double cosA = Math.cos(angle);
            boolean cardinal = (i % 2 == 0);
            int tickInner = cardinal ? (radius - s(10)) : (radius - s(6));
            int tickOuter = radius;

            // Screen coords: x = cx + sin*r, y = cy - cos*r
            int x1 = cx + (int) Math.round(sinA * tickInner);
            int y1 = cy - (int) Math.round(cosA * tickInner);
            int x2 = cx + (int) Math.round(sinA * tickOuter);
            int y2 = cy - (int) Math.round(cosA * tickOuter);
            drawColorLine(context, x1, y1, x2, y2, cardinal ? brightColor : dimColor);

            if (cardinal) {
                int innerX1 = cx + (int) Math.round(sinA * armGap);
                int innerY1 = cy - (int) Math.round(cosA * armGap);
                drawColorLine(context, innerX1, innerY1, x1, y1, dimColor);
            }

            // Cardinal labels use N/E/S/W in accent color, intercardinals dimmer
            int labelR = tickOuter + s(10);
            int lx = cx + (int) Math.round(sinA * labelR);
            int ly = cy - (int) Math.round(cosA * labelR);
            int lw = textRenderer.getWidth(labels[i]);
            // N label uses accent color to stand out
            int labelColor;
            if (i == 0) {
                labelColor = themeAccent(); // N
            } else if (cardinal) {
                labelColor = themeText();   // E, S, W
            } else {
                labelColor = themeTextDim(); // NE, SE, SW, NW
            }
            context.drawText(textRenderer, Text.literal(labels[i]), lx - lw / 2,
                    ly - textRenderer.fontHeight / 2, labelColor, false);
        }

        // Dial border: 4-line angular frame
        int boxR = (int) (radius * 0.82);
        int cut = Math.max(s(5), boxR / 4);
        context.fill(cx - boxR + cut, cy - boxR, cx + boxR, cy - boxR + 1, dimColor);
        context.fill(cx - boxR, cy + boxR - 1, cx + boxR - cut, cy + boxR, dimColor);
        for (int i = 0; i < cut; i++) {
            context.fill(cx - boxR + cut - i, cy - boxR + i, cx - boxR + cut - i + 1, cy - boxR + i + 1, dimColor);
        }
        for (int i = 0; i < cut; i++) {
            context.fill(cx + boxR - cut + i - 1, cy + boxR - 1 - i, cx + boxR - cut + i, cy + boxR - i, dimColor);
        }

        // --- Diamond needle pointer (issues #2 + #7) ---
        // Player facing direction: bearing from north = (yaw + 180) % 360
        // yaw=0=south, yaw=180=north in Minecraft
        float bearing = ((yaw + 180) % 360 + 360) % 360;
        double bRad = Math.toRadians(bearing);
        // Needle unit vector in screen coords (y-down): x=sin, y=-cos
        double nx = Math.sin(bRad);
        double ny = -Math.cos(bRad);
        // Perpendicular direction
        double px = -ny;
        double py = nx;

        int tipLen = radius - s(10);    // north tip distance from center
        int tailLen = radius / 3;       // south tail distance from center
        int halfWidth = s(6);           // diamond half-width at center

        int tipX = cx + (int) Math.round(nx * tipLen);
        int tipY = cy + (int) Math.round(ny * tipLen);
        int tailX = cx - (int) Math.round(nx * tailLen);
        int tailY = cy - (int) Math.round(ny * tailLen);
        int lwX = cx + (int) Math.round(px * halfWidth);
        int lwY = cy + (int) Math.round(py * halfWidth);
        int rwX = cx - (int) Math.round(px * halfWidth);
        int rwY = cy - (int) Math.round(py * halfWidth);

        // North half (tip to wings) — accent color (issue #7)
        int northColor = themeAccent();
        int southColor = isLightMode() ? 0xFFAAAFC0 : 0xFF8899AA;
        drawColorLine(context, tipX, tipY, lwX, lwY, northColor);
        drawColorLine(context, tipX, tipY, rwX, rwY, northColor);
        // South half (tail to wings) — dim color
        drawColorLine(context, tailX, tailY, lwX, lwY, southColor);
        drawColorLine(context, tailX, tailY, rwX, rwY, southColor);

        // Center dot
        int dotR = Math.max(2, s(2));
        context.fill(cx - dotR, cy - dotR, cx + dotR, cy + dotR, themeAccentDim());

        // Direction label below compass
        int labelY = cy + radius + s(14);
        String degText = degText(bearing) + "  " + (((int) bearing) % 360) + "°";
        int textW = textRenderer.getWidth(degText);
        fillChamferedRect(context, cx - textW / 2 - s(4), labelY - s(1), textW + s(8), s(14), Math.max(1, s(2)),
                isLightMode() ? 0x33D8E4F0 : 0x33081018);
        context.fill(cx - textW / 2 - s(4), labelY - s(1),
                cx - textW / 2 - s(4) + 1, labelY + s(13), themeAccentDim());
        context.drawText(textRenderer, Text.literal(degText), cx - textW / 2, labelY + s(2),
                themeText(), !isLightMode());
    }

    /** Returns the cardinal/intercardinal label for a bearing (0=N, clockwise). */
    private static String degText(float bearing) {
        float b = ((bearing % 360) + 360) % 360;
        if (b < 22.5 || b >= 337.5) return "N";
        if (b < 67.5) return "NE";
        if (b < 112.5) return "E";
        if (b < 157.5) return "SE";
        if (b < 202.5) return "S";
        if (b < 247.5) return "SW";
        if (b < 292.5) return "W";
        return "NW";
    }

    /** Bresenham line with explicit color. */
    private void drawColorLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
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

    private float getPlayerYaw() {
        MinecraftClient mc = MinecraftClient.getInstance();
        var player = mc == null ? null : mc.player;
        if (player == null) return 0f;
        float yaw = player.getYaw() % 360f;
        if (yaw < 0) yaw += 360f;
        return yaw;
    }
}
