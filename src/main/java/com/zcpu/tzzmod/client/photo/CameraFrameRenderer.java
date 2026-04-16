package com.zcpu.tzzmod.client.photo;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;

public final class CameraFrameRenderer {
    private static final int MIN_MARGIN = 14;
    private static final int MIN_DIAGONAL = 18;
    private static final int MIN_THICKNESS = 2;

    private CameraFrameRenderer() {
    }

    public static int getFrameMargin(int width, int height) {
        return createLayout(width, height).margin();
    }

    public static void drawOnContext(DrawContext context, int width, int height, int color) {
        Layout layout = createLayout(width, height);
        drawHorizontal(context, layout.topLineStart(), layout.topLineEnd(), layout.topY(), layout.thickness(), color);
        drawHorizontal(context, layout.bottomLineStart(), layout.bottomLineEnd(), layout.bottomY(), layout.thickness(), color);
        drawRisingDiagonal(context, layout.diagonalStartX(), layout.topDiagonalBottomY(), layout.diagonalLength(), layout.thickness(), color);
        drawRisingDiagonal(context, layout.bottomDiagonalStartX(), layout.bottomY(), layout.diagonalLength(), layout.thickness(), color);
    }

    public static void drawOnImage(NativeImage image, int width, int height, int argbColor) {
        Layout layout = createLayout(width, height);
        int abgrColor = argbToAbgr(argbColor);
        drawHorizontal(image, width, height, layout.topLineStart(), layout.topLineEnd(), layout.topY(), layout.thickness(), abgrColor);
        drawHorizontal(image, width, height, layout.bottomLineStart(), layout.bottomLineEnd(), layout.bottomY(), layout.thickness(), abgrColor);
        drawRisingDiagonal(image, width, height, layout.diagonalStartX(), layout.topDiagonalBottomY(), layout.diagonalLength(), layout.thickness(), abgrColor);
        drawRisingDiagonal(image, width, height, layout.bottomDiagonalStartX(), layout.bottomY(), layout.diagonalLength(), layout.thickness(), abgrColor);
    }

    private static Layout createLayout(int width, int height) {
        int minSize = Math.max(1, Math.min(width, height));
        int thickness = Math.max(MIN_THICKNESS, minSize / 240);
        int margin = Math.max(MIN_MARGIN, minSize / 18);
        int diagonalLength = Math.max(MIN_DIAGONAL, minSize / 9);
        int lineGap = Math.max(8, diagonalLength / 4);
        int topY = margin;
        int bottomY = Math.max(topY + thickness + diagonalLength, height - margin - thickness);
        int diagonalStartX = margin;
        int topDiagonalBottomY = topY + diagonalLength - 1;
        int topLineStart = Math.min(width - margin, diagonalStartX + diagonalLength + lineGap);
        int topLineEnd = Math.max(topLineStart + thickness, width - margin);
        int bottomLineStart = margin;
        int bottomLineEnd = Math.max(bottomLineStart + thickness, width - margin - diagonalLength - lineGap);
        int bottomDiagonalStartX = Math.max(bottomLineEnd + lineGap, width - margin - diagonalLength);
        return new Layout(margin, thickness, diagonalLength, topY, bottomY, diagonalStartX, topDiagonalBottomY,
                topLineStart, topLineEnd, bottomLineStart, bottomLineEnd, bottomDiagonalStartX);
    }

    private static void drawHorizontal(DrawContext context, int startX, int endX, int y, int thickness, int color) {
        if (endX <= startX) {
            return;
        }
        context.fill(startX, y, endX, y + thickness, color);
    }

    private static void drawHorizontal(NativeImage image, int width, int height,
                                       int startX, int endX, int y, int thickness, int color) {
        if (endX <= startX) {
            return;
        }
        int clampedStartX = Math.max(0, startX);
        int clampedEndX = Math.min(width, endX);
        int clampedStartY = Math.max(0, y);
        int clampedEndY = Math.min(height, y + thickness);
        for (int drawY = clampedStartY; drawY < clampedEndY; drawY++) {
            for (int drawX = clampedStartX; drawX < clampedEndX; drawX++) {
                image.setColorArgb(drawX, drawY, color);
            }
        }
    }

    private static void drawRisingDiagonal(DrawContext context, int startX, int bottomY, int length, int thickness, int color) {
        for (int index = 0; index < length; index++) {
            int x = startX + index;
            int y = bottomY - index;
            context.fill(x, y, x + thickness, y + thickness, color);
        }
    }

    private static void drawRisingDiagonal(NativeImage image, int width, int height,
                                           int startX, int bottomY, int length, int thickness, int color) {
        for (int index = 0; index < length; index++) {
            int x = startX + index;
            int y = bottomY - index;
            for (int offsetX = 0; offsetX < thickness; offsetX++) {
                for (int offsetY = 0; offsetY < thickness; offsetY++) {
                    int drawX = x + offsetX;
                    int drawY = y + offsetY;
                    if (drawX >= 0 && drawX < width && drawY >= 0 && drawY < height) {
                        image.setColorArgb(drawX, drawY, color);
                    }
                }
            }
        }
    }

    private static int argbToAbgr(int argbColor) {
        int a = (argbColor >> 24) & 0xFF;
        int r = (argbColor >> 16) & 0xFF;
        int g = (argbColor >> 8) & 0xFF;
        int b = argbColor & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private record Layout(
            int margin,
            int thickness,
            int diagonalLength,
            int topY,
            int bottomY,
            int diagonalStartX,
            int topDiagonalBottomY,
            int topLineStart,
            int topLineEnd,
            int bottomLineStart,
            int bottomLineEnd,
            int bottomDiagonalStartX
    ) {
    }
}