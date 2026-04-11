package com.zcpu.tzzmod.client.map;

import com.zcpu.tzzmod.client.phone.ui.RoundedRectRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class MapCanvasRenderer {
    private static final Identifier TEXTURE_ID = Identifier.of("tzz_mod", "dynamic/map_canvas");
    private static final int OUTLINE_COLOR = 0xFFFFE15C;
    private static final int SOFT_OUTLINE_COLOR = 0x66FFE15C;
    private static NativeImageBackedTexture cachedTexture;
    private static Identifier cachedTextureId;
    private static int cachedImageWidth = -1;
    private static int cachedImageHeight = -1;
    private static int cachedImageHash = 0;

    private MapCanvasRenderer() {
    }

    public static void reset() {
        if (cachedTexture != null) {
            cachedTexture.close();
            cachedTexture = null;
        }
        cachedTextureId = null;
        cachedImageWidth = -1;
        cachedImageHeight = -1;
        cachedImageHash = 0;
    }

    public static RenderResult render(DrawContext context, MapClient.MapState state, int x, int y, int width, int height, int mouseX, int mouseY) {
        int radius = Math.max(4, Math.min(width, height) / 16);
        RoundedRectRenderer.fillRoundedRect(context, x, y, width, height, radius, 0x4418212B);

        if (!state.hasRegion() || state.region() == null || state.imageWidth() <= 0 || state.imageHeight() <= 0 || state.imageColors().length == 0) {
            return new RenderResult(false, "", x, y, width, height);
        }

        int availableWidth = Math.max(1, width - 4);
        int availableHeight = Math.max(1, height - 4);
        float scale = Math.min(availableWidth / (float) state.imageWidth(), availableHeight / (float) state.imageHeight());
        int drawWidth = Math.max(1, Math.round(state.imageWidth() * scale));
        int drawHeight = Math.max(1, Math.round(state.imageHeight() * scale));
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;

        context.fill(drawX - 1, drawY - 1, drawX + drawWidth + 1, drawY + drawHeight + 1, 0x88485566);

        Identifier textureId = ensureTexture(state);
        if (textureId == null) {
            return new RenderResult(false, "", x, y, width, height);
        }
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                textureId,
                drawX,
                drawY,
                0.0F,
                0.0F,
                drawWidth,
                drawHeight,
                state.imageWidth(),
                state.imageHeight(),
                state.imageWidth(),
                state.imageHeight(),
                -1
        );

        String hoveredMarkerName = "";
        MapClient.MapRegion region = state.region();
        if (state.settings().showMarkers()) {
            for (MapClient.MapMarker marker : state.markers()) {
                if (!region.dimensionId().equals(marker.dimensionId())) {
                    continue;
                }
                int markerX = drawX + Math.round(((marker.x() - region.minX() + 0.5F) / Math.max(1.0F, region.width())) * drawWidth);
                int markerY = drawY + Math.round(((marker.z() - region.minZ() + 0.5F) / Math.max(1.0F, region.depth())) * drawHeight);
                drawMarker(context, markerX, markerY, marker.color());
                int dx = mouseX - markerX;
                int dy = mouseY - markerY;
                if (dx * dx + dy * dy <= 36) {
                    hoveredMarkerName = marker.name();
                }
            }
        }

        for (MapClient.MapPlayer player : state.players()) {
            if (player.self() && !state.settings().showSelfPosition()) {
                continue;
            }
            if (!player.self() && !state.settings().showOtherPlayers()) {
                continue;
            }
            int playerX = drawX + Math.round((float) ((player.x() - region.minX()) / Math.max(1.0D, region.width())) * drawWidth);
            int playerY = drawY + Math.round((float) ((player.z() - region.minZ()) / Math.max(1.0D, region.depth())) * drawHeight);
            drawPlayer(context, playerX, playerY, player.self() ? 0xFFFFFFFF : 0xFF6EC6FF);
        }

        return new RenderResult(true, hoveredMarkerName, drawX, drawY, drawWidth, drawHeight);
    }

    private static void drawMarker(DrawContext context, int centerX, int centerY, int color) {
        context.fill(centerX - 5, centerY - 5, centerX + 6, centerY + 6, SOFT_OUTLINE_COLOR);
        context.fill(centerX - 4, centerY - 4, centerX + 5, centerY + 5, OUTLINE_COLOR);
        context.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, 0xFF15202B);
        context.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, color | 0xFF000000);
    }

    private static void drawPlayer(DrawContext context, int centerX, int centerY, int color) {
        context.fill(centerX - 5, centerY - 2, centerX + 6, centerY + 3, SOFT_OUTLINE_COLOR);
        context.fill(centerX - 2, centerY - 5, centerX + 3, centerY + 6, SOFT_OUTLINE_COLOR);
        context.fill(centerX - 4, centerY - 1, centerX + 5, centerY + 2, OUTLINE_COLOR);
        context.fill(centerX - 1, centerY - 4, centerX + 2, centerY + 5, OUTLINE_COLOR);
        context.fill(centerX - 2, centerY, centerX + 3, centerY + 1, 0xFF101820);
        context.fill(centerX, centerY - 2, centerX + 1, centerY + 3, 0xFF101820);
        context.fill(centerX - 1, centerY, centerX + 2, centerY + 1, color);
        context.fill(centerX, centerY - 1, centerX + 1, centerY + 2, color);
    }

    private static Identifier ensureTexture(MapClient.MapState state) {
        if (cachedTextureId != null
                && cachedTexture != null
                && cachedImageWidth == state.imageWidth()
                && cachedImageHeight == state.imageHeight()
                && cachedImageHash == state.imageHash()) {
            return cachedTextureId;
        }

        reset();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getTextureManager() == null || state.imageWidth() <= 0 || state.imageHeight() <= 0) {
            return null;
        }

        NativeImage image = new NativeImage(state.imageWidth(), state.imageHeight(), false);
        int[] colors = state.imageColors();
        for (int pixelY = 0; pixelY < state.imageHeight(); pixelY++) {
            for (int pixelX = 0; pixelX < state.imageWidth(); pixelX++) {
                image.setColorArgb(pixelX, pixelY, 0xFF000000 | colors[pixelY * state.imageWidth() + pixelX]);
            }
        }

        cachedTexture = new NativeImageBackedTexture(() -> "tzz_mod_map_canvas", image);
        client.getTextureManager().registerTexture(TEXTURE_ID, cachedTexture);
        cachedTextureId = TEXTURE_ID;
        cachedImageWidth = state.imageWidth();
        cachedImageHeight = state.imageHeight();
        cachedImageHash = state.imageHash();
        return cachedTextureId;
    }

    public record RenderResult(boolean rendered, String hoveredMarkerName, int drawX, int drawY, int drawWidth, int drawHeight) {
    }
}