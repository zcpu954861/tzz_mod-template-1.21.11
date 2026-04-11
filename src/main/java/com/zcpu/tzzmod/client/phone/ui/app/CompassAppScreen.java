package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CompassAppScreen extends AbstractPhoneScreen {
    private static final Identifier ICON = Identifier.of("tzz_mod", "textures/gui/phone/icons/compass.png");

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
        // Title — moved down slightly so it doesn't get clipped by the panel border
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.compass"), contentX + contentWidth / 2, contentY + s(14));

        // Background panel for the compass area
        int pad = s(8);
        int panelX = contentX + pad;
        // move panel slightly up so its bottom doesn't get overlapped by the app buttons
        int panelY = contentY + s(10);
        int panelW = contentWidth - pad * 2;
        int panelH = contentHeight - s(20) - s(24);
        int panelRadius = Math.max(4, s(8));
        // Use the phone-style faint grey border (similar to phone edge color)
        int panelBorder = 0x88E6EEF7;    // faint grey/cyan used by phone UI

        // Draw phone-style rounded line border (four sides + corner arcs)
        drawRoundedLineBorder(context, panelX, panelY, panelW, panelH, panelRadius, Math.max(1, s(1)), panelBorder);
        // Instead of a solid inner fill, draw an inner stroked rounded border (phone-style) in a light gray
        int innerInset = s(3);
        int innerRadius = Math.max(2, panelRadius - innerInset);
        int innerBorderColor = 0x66E6EEF7; //淡灰色轻边
        drawRoundedLineBorder(context, panelX + innerInset, panelY + innerInset, panelW - innerInset * 2, panelH - innerInset * 2, innerRadius, Math.max(1, s(1)), innerBorderColor);

        // Compass circle area
        int cx = panelX + panelW / 2;
        int cy = panelY + panelH / 2 - s(6);
        int radius = Math.min(panelW, panelH) / 3;
        radius = Math.max(radius, s(18));

        // Draw outer ring and inner ring for a tech look
        drawCircleOutline(context, cx, cy, radius + s(6), s(2), 0x55FFFFFF);
        drawCircleOutline(context, cx, cy, radius + s(2), s(2), 0x5590C8FF);
        drawCircleOutline(context, cx, cy, radius, s(2), 0xAA2A8FC1);

        // Tick marks for 8 directions
        for (int i = 0; i < 8; i++) {
            double ang = Math.toRadians(i * 45.0);
            int outerX = cx + (int) Math.round(Math.sin(ang) * (radius + s(6)));
            int outerY = cy - (int) Math.round(Math.cos(ang) * (radius + s(6)));
            int innerX = cx + (int) Math.round(Math.sin(ang) * (radius - s(2)));
            int innerY = cy - (int) Math.round(Math.cos(ang) * (radius - s(2)));
            drawLine(context, innerX, innerY, outerX, outerY);

            // small labels for cardinal directions (N,E,S,W)
            if (i % 2 == 0) {
                String key = switch (i) {
                    case 0 -> "phone.tzz_mod.direction.north";
                    case 2 -> "phone.tzz_mod.direction.east";
                    case 4 -> "phone.tzz_mod.direction.south";
                    case 6 -> "phone.tzz_mod.direction.west";
                    default -> "";
                };
                // draw cardinal label (N/E/S/W)
                int lx = cx + (int) Math.round(Math.sin(ang) * (radius + s(12)));
                int ly = cy - (int) Math.round(Math.cos(ang) * (radius + s(12)));
                Text labelText = Text.translatable(key);
                context.drawTextWithShadow(textRenderer, labelText, lx - textRenderer.getWidth(labelText) / 2, ly - s(6), 0xFFECECEC);
            }
        }

        // Draw needle based on yaw
        float yaw = getPlayerYaw();
        double angleRad = Math.toRadians(yaw);
        // angle 0 => up (north)
        double nx = Math.sin(angleRad);
        double ny = -Math.cos(angleRad);
        // make the needle slightly longer and thinner for an elongated look
        int needleLen = radius - s(2);

        // center dot half-size (used to anchor the needle)
        int d = s(4);

        // forward tip coordinates (compute from doubles and round to ensure consistency with base center rounding)
        int fx = (int) Math.round(cx + nx * (double) needleLen);
        int fy = (int) Math.round(cy + ny * (double) needleLen);

        // For a thin elongated symmetric triangle, place the triangle base right at the edge of the center square
        // so the forward triangle appears connected to the central gray square.
        // Clamp baseDist so it does not exceed the needle length.
        int baseDist = Math.max(1, Math.min(needleLen - 1, d + 1)); // base located at square edge + 1px overlap
        int baseHalf = Math.max(1, s(2)); // narrow half-width for a thin appearance

        // base center for forward triangle
        int baseOffsetX = (int) Math.round(nx * (double) baseDist);
        int baseOffsetY = (int) Math.round(ny * (double) baseDist);
        int fBaseCx_i = cx + baseOffsetX;
        int fBaseCy_i = cy + baseOffsetY;

        // Compute perpendicular vector from the direction (nx,ny): perp = (-ny, nx)
        // Use normalized float perp so width is stable and avoid redundant integer temp vars
        int offX, offY;
        double px = -ny;
        double py = nx;
        double plen = Math.sqrt(px * px + py * py);
        if (plen != 0.0) { px /= plen; py /= plen; }
        offX = (int) Math.round(px * baseHalf);
        offY = (int) Math.round(py * baseHalf);

        int fbx1 = fBaseCx_i + offX;
        int fby1 = fBaseCy_i + offY;
        int fbx2 = fBaseCx_i - offX;
        int fby2 = fBaseCy_i - offY;

        // draw forward (pure red #FF0000)
        drawFilledTriangle(context, fx, fy, fbx1, fby1, fbx2, fby2, 0xFFFF0000);
        // Mirror forward triangle integer vertices about center (cx,cy) to get exact symmetric tail (pure white #FFFFFF)
        int tailTipX = 2 * cx - fx;
        int tailTipY = 2 * cy - fy;
        int tbx1 = 2 * cx - fbx1;
        int tby1 = 2 * cy - fby1;
        int tbx2 = 2 * cx - fbx2;
        int tby2 = 2 * cy - fby2;
        drawFilledTriangle(context, tailTipX, tailTipY, tbx1, tby1, tbx2, tby2, 0xFFFFFFFF);

        // Center dot
        context.fill(cx - d, cy - d, cx + d + 1, cy + d + 1, 0xFFCCCCCC);

        // Show big direction label and degree — moved down slightly to avoid overlapping the compass dial
        String dirKey = getPlayerDirectionKey();
        int labelY = cy + radius + s(18); // moved down
        int degY = cy + radius + s(30);   // follow below the label
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable(dirKey), cx, labelY, 0xFFFFFFFF);
        int deg = Math.round(getPlayerYaw());
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(deg + "°"), cx, degY, 0xFF99D3FF);

        // Optional small icon at top-left of panel
        if (hasResource(ICON)) {
            int iconSize = s(18);
            int ix = panelX + s(8);
            int iy = panelY + s(8);
            // drawTexturedQuad(destination coords, u0,v1,u1,v0 order floats 0..1)
            context.drawTexturedQuad(ICON, ix, iy, ix + iconSize, iy + iconSize,
                    0.0F, 1.0F, 0.0F, 1.0F);
        }

        // footer hint removed (kept UI clean)
    }

    private float getPlayerYaw() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return 0f;
        var player = client.player;
        if (player == null) {
            return 0f;
        }
        float yaw = player.getYaw() % 360f;
        if (yaw < 0) yaw += 360f;
        return yaw;
    }

    private String getPlayerDirectionKey() {
        float yaw = getPlayerYaw();
        if (yaw >= 337.5 || yaw < 22.5) return "phone.tzz_mod.direction.north";
        if (yaw >= 22.5 && yaw < 67.5) return "phone.tzz_mod.direction.northeast";
        if (yaw >= 67.5 && yaw < 112.5) return "phone.tzz_mod.direction.east";
        if (yaw >= 112.5 && yaw < 157.5) return "phone.tzz_mod.direction.southeast";
        if (yaw >= 157.5 && yaw < 202.5) return "phone.tzz_mod.direction.south";
        if (yaw >= 202.5 && yaw < 247.5) return "phone.tzz_mod.direction.southwest";
        if (yaw >= 247.5 && yaw < 292.5) return "phone.tzz_mod.direction.west";
        if (yaw >= 292.5 && yaw < 337.5) return "phone.tzz_mod.direction.northwest";
        return "phone.tzz_mod.direction.north";
    }

    private void drawCircleOutline(DrawContext context, int cx, int cy, int radius, int thickness, int color) {
        int outer = radius + thickness / 2;
        int inner = Math.max(0, radius - (thickness + 1) / 2);
        for (int y = -outer; y <= outer; y++) {
            int absY = Math.abs(y);
            int maxXOuter = (int) Math.floor(Math.sqrt(Math.max(0, outer * outer - absY * absY)));
            int maxXInner = inner > 0 ? (int) Math.floor(Math.sqrt(Math.max(0, inner * inner - absY * absY))) : 0;
            if (maxXOuter > maxXInner) {
                int start = cx - maxXOuter;
                int end = cx - maxXInner;
                context.fill(start, cy + y, end + 1, cy + y + 1, color);
                start = cx + maxXInner;
                end = cx + maxXOuter;
                context.fill(start, cy + y, end + 1, cy + y + 1, color);
            }
        }
    }

    // Bresenham's line (integer) for a crisp tech look (uses fixed tick color)
    private static final int TICK_COLOR = 0x99FFFFFF;
    private void drawLine(DrawContext context, int x0, int y0, int x1, int y1) {
         int dx = Math.abs(x1 - x0);
         int sx = x0 < x1 ? 1 : -1;
         int dy = -Math.abs(y1 - y0);
         int sy = y0 < y1 ? 1 : -1;
         int err = dx + dy;
         int x = x0;
         int y = y0;
         while (true) {
            context.fill(x, y, x + 1, y + 1, TICK_COLOR);
             if (x == x1 && y == y1) break;
             int e2 = 2 * err;
             if (e2 >= dy) {
                 err += dy;
                 x += sx;
             }
             if (e2 <= dx) {
                 err += dx;
                 y += sy;
             }
         }
     }

    // Filled triangle via horizontal scanline rasterization (integer)
    private void drawFilledTriangle(DrawContext context, int x0, int y0, int x1, int y1, int x2, int y2, int color) {
        // Sort vertices by y ascending (v0.y <= v1.y <= v2.y)
        int[] vx = {x0, x1, x2};
        int[] vy = {y0, y1, y2};
        // simple bubble sort for three elements
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2 - i; j++) {
                if (vy[j] > vy[j + 1]) {
                    int tmpY = vy[j]; vy[j] = vy[j + 1]; vy[j + 1] = tmpY;
                    int tmpX = vx[j]; vx[j] = vx[j + 1]; vx[j + 1] = tmpX;
                }
            }
        }

        int xA = vx[0], yA = vy[0];
        int xB = vx[1], yB = vy[1];
        int xC = vx[2], yC = vy[2];

        if (yA == yC) {
            // Degenerate horizontal triangle
            int minX = Math.min(Math.min(xA, xB), xC);
            int maxX = Math.max(Math.max(xA, xB), xC);
            context.fill(minX, yA, maxX + 1, yA + 1, color);
            return;
        }

        // interpolation helper: compute x on line (x0,y0)-(x1,y1) at given y
        // java.util.function.BiFunction<Integer,Integer,Integer> interp = (yy, ignored) -> 0;
        // We will not use the BiFunction functional form for clarity
        // Instead define a local method via lambda-like pattern
        java.util.function.Function<int[], Integer> edgeX = (params) -> {
            int xa = params[0], ya = params[1], xb_ = params[2], yb_ = params[3], yy = params[4];
            if (yb_ == ya) return xa;
            double t = (yy - ya) / (double)(yb_ - ya);
            return xa + (int)Math.round((xb_ - xa) * t);
        };

        // Rasterize full span from yA to yC
        for (int y = yA; y <= yC; y++) {
            int xa, xb;
            if (y <= yB) {
                xa = edgeX.apply(new int[]{xA, yA, xB, yB, y});
                xb = edgeX.apply(new int[]{xA, yA, xC, yC, y});
            } else {
                xa = edgeX.apply(new int[]{xB, yB, xC, yC, y});
                xb = edgeX.apply(new int[]{xA, yA, xC, yC, y});
            }
            if (xa > xb) { int t = xa; xa = xb; xb = t; }
            context.fill(xa, y, xb + 1, y + 1, color);
        }
    }

    private void drawRoundedLineBorder(DrawContext context, int x, int y, int width, int height, int radius, int thickness, int color) {
        if (width <= 0 || height <= 0) return;
        int maxCorner = Math.min(width, height) / 2;
        int cornerRadius = Math.max(thickness, Math.min(radius, maxCorner));

        // top
        context.fill(x + cornerRadius, y, x + width - cornerRadius, y + thickness, color);
        // bottom
        context.fill(x + cornerRadius, y + height - thickness, x + width - cornerRadius, y + height, color);
        // left
        context.fill(x, y + cornerRadius, x + thickness, y + height - cornerRadius, color);
        // right
        context.fill(x + width - thickness, y + cornerRadius, x + width, y + height - cornerRadius, color);

        // corner arcs
        int tlcx = x + cornerRadius;
        int tlcy = y + cornerRadius;
        drawCornerArc(context, tlcx, tlcy, cornerRadius, thickness, -1, -1, color);

        int trcx = x + width - cornerRadius - 1;
        int trcy = y + cornerRadius;
        drawCornerArc(context, trcx, trcy, cornerRadius, thickness, 1, -1, color);

        int blcx = x + cornerRadius;
        int blcy = y + height - cornerRadius - 1;
        drawCornerArc(context, blcx, blcy, cornerRadius, thickness, -1, 1, color);

        int brcx = x + width - cornerRadius - 1;
        int brcy = y + height - cornerRadius - 1;
        drawCornerArc(context, brcx, brcy, cornerRadius, thickness, 1, 1, color);
    }

    private void drawCornerArc(DrawContext context, int centerX, int centerY, int radius, int thickness, int xDir, int yDir, int color) {
        if (radius <= 0 || thickness <= 0) return;
        int innerR = Math.max(0, radius - thickness);

        for (int dy = 0; dy <= radius; dy++) {
            double rr = radius * (double) radius - dy * (double) dy;
            if (rr < 0) rr = 0;
            int dxOuter = (int) Math.floor(Math.sqrt(rr));

            int dxInner = 0;
            if (innerR > 0) {
                double ri = innerR * (double) innerR - dy * (double) dy;
                if (ri > 0) dxInner = (int) Math.floor(Math.sqrt(ri));
            }

            int drawY = centerY + (yDir < 0 ? -dy : dy);

            if (xDir < 0) {
                int startX = centerX - dxOuter;
                int endX = centerX - dxInner; // exclusive
                if (startX < endX) context.fill(startX, drawY, endX, drawY + 1, color);
            } else {
                int startX = centerX + dxInner + 1;
                int endX = centerX + dxOuter + 1; // exclusive
                if (startX < endX) context.fill(startX, drawY, endX, drawY + 1, color);
            }
        }
    }
}
