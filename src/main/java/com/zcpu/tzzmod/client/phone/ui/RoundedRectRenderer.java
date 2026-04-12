package com.zcpu.tzzmod.client.phone.ui;

import net.minecraft.client.gui.DrawContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RoundedRectRenderer {
    private static final Map<Integer, int[]> RADIUS_SPANS = new ConcurrentHashMap<>();

    private RoundedRectRenderer() {
    }

    public static void fillRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        int safeRadius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));

        context.fill(x + safeRadius, y, x + width - safeRadius, y + height, color);
        context.fill(x, y + safeRadius, x + width, y + height - safeRadius, color);

        fillCorner(context, x + safeRadius, y + safeRadius, safeRadius, -1, -1, color);
        fillCorner(context, x + width - safeRadius - 1, y + safeRadius, safeRadius, 1, -1, color);
        fillCorner(context, x + safeRadius, y + height - safeRadius - 1, safeRadius, -1, 1, color);
        fillCorner(context, x + width - safeRadius - 1, y + height - safeRadius - 1, safeRadius, 1, 1, color);
    }

    private static void fillCorner(DrawContext context, int centerX, int centerY, int radius,
                                   int xDirection, int yDirection, int color) {
        int[] spans = getCornerSpans(radius);
        for (int dy = 0; dy <= radius; dy++) {
            int dx = spans[dy];
            int startX = centerX + (xDirection < 0 ? -dx : 0);
            int endX = centerX + (xDirection < 0 ? 1 : dx + 1);
            int drawY = centerY + (yDirection < 0 ? -dy : dy);
            context.fill(startX, drawY, endX, drawY + 1, color);
        }
    }

    public static int[] getCornerSpans(int radius) {
        int safeRadius = Math.max(0, radius);
        return RADIUS_SPANS.computeIfAbsent(safeRadius, RoundedRectRenderer::buildCornerSpans);
    }

    private static int[] buildCornerSpans(int radius) {
        int[] spans = new int[radius + 1];
        for (int dy = 0; dy <= radius; dy++) {
            spans[dy] = (int) Math.floor(Math.sqrt((radius * radius) - (dy * dy)));
        }
        return spans;
    }
}

