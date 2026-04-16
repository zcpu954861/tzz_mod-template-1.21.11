package com.zcpu.tzzmod.client.phone.chat;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class ChatUiUtil {
    private ChatUiUtil() {
    }

    public static int[] fitSize(int sourceWidth, int sourceHeight, int maxWidth, int maxHeight) {
        int safeWidth = Math.max(1, sourceWidth);
        int safeHeight = Math.max(1, sourceHeight);
        int safeMaxWidth = Math.max(1, maxWidth);
        int safeMaxHeight = Math.max(1, maxHeight);
        double scale = Math.min(1.0D, Math.min(safeMaxWidth / (double) safeWidth, safeMaxHeight / (double) safeHeight));
        return new int[]{
                Math.max(1, (int) Math.round(safeWidth * scale)),
                Math.max(1, (int) Math.round(safeHeight * scale))
        };
    }

    public static void drawAngularFrame(DrawContext context, int x, int y, int width, int height,
                                        int cut, int fillColor, int borderColor) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int clampedCut = Math.min(Math.max(0, cut), Math.min(width / 2, height / 2));
        for (int row = 0; row < height; row++) {
            int leftCut = 0;
            int rightCut = 0;
            if (row < clampedCut) {
                leftCut = clampedCut - row;
            }
            if (row >= height - clampedCut) {
                rightCut = clampedCut - (height - 1 - row);
            }
            context.fill(x + leftCut, y + row, x + width - rightCut, y + row + 1, fillColor);
        }

        context.fill(x + clampedCut, y, x + width, y + 1, borderColor);
        context.fill(x, y + height - 1, x + width - clampedCut, y + height, borderColor);
        for (int index = 0; index < clampedCut; index++) {
            context.fill(x + clampedCut - index, y + index, x + clampedCut - index + 1, y + index + 1, borderColor);
        }
        for (int index = 0; index < clampedCut; index++) {
            context.fill(x + width - clampedCut + index - 1, y + height - 1 - index,
                    x + width - clampedCut + index, y + height - index, borderColor);
        }
    }

    public static void drawSelectionBadge(DrawContext context, TextRenderer textRenderer,
                                          int right, int bottom, int size,
                                          int fillColor, int textColor) {
        int badgeX = right - size;
        int badgeY = bottom - size;
        context.fill(badgeX, badgeY, right, bottom, fillColor);
        String check = "\u2713";
        int textX = badgeX + (size - textRenderer.getWidth(check)) / 2;
        int textY = badgeY + Math.max(0, (size - textRenderer.fontHeight) / 2);
        context.drawText(textRenderer, Text.literal(check), textX, textY, textColor, false);
    }
}