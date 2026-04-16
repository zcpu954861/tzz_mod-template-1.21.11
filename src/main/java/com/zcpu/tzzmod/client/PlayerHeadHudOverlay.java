package com.zcpu.tzzmod.client;

import com.zcpu.tzzmod.client.photo.CameraModeClient;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import com.zcpu.tzzmod.util.NullSafety;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class PlayerHeadHudOverlay {
    private static final int HEAD_SIZE = 32;
    private static final int OVERLAY_X = 8;
    private static final int OVERLAY_Y = 8;
    private static final int NAME_SPACING = 6;
    private static final int PANEL_CHAMFER = 4;
    private static final int HEAD_PANEL_PADDING = 4;
    private static final int NAME_PADDING_X = 8;
    private static final int NAME_PADDING_Y = 4;
    private static final int SHADOW_COLOR = 0x55000000;
    private static final int FACE_U = 8;
    private static final int FACE_V = 8;
    private static final int HAT_U = 40;
    private static final int HAT_V = 8;
    private static final int SKIN_TEXTURE_SIZE = 64;
    private static final int HAT_EXPAND = 1;

    private PlayerHeadHudOverlay() {}

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.currentScreen != null || CameraModeClient.isActive()) {
            return;
        }

        Boolean enforced = ForcedHudClient.getServerEnforcedHud();
        if (enforced != null) {
            // server enforces visibility: false = hide, true = show
            if (!enforced) return;
        } else {
            // no server enforcement; respect local HUD hidden setting
            if (client.options.hudHidden) return;
        }

        var player = NullSafety.requireNonNull(client.player);
        boolean lightMode = isLightMode();
        boolean drawn = false;

        String name = player.getName().getString();
        if (name == null) {
            name = "";
        }

        int maxTextWidth = Math.max(HEAD_SIZE, context.getScaledWindowWidth() - OVERLAY_X - 16 - NAME_PADDING_X * 2);
        String shownName = client.textRenderer.trimToWidth(name, maxTextWidth);
        int panelWidth = Math.max(HEAD_SIZE + HEAD_PANEL_PADDING * 2,
                client.textRenderer.getWidth(shownName) + NAME_PADDING_X * 2);
        int headPanelHeight = HEAD_SIZE + HEAD_PANEL_PADDING * 2;
        int namePanelHeight = client.textRenderer.fontHeight + NAME_PADDING_Y * 2;
        int headX = OVERLAY_X + (panelWidth - HEAD_SIZE) / 2;
        int headY = OVERLAY_Y + HEAD_PANEL_PADDING;
        int namePanelY = OVERLAY_Y + headPanelHeight + NAME_SPACING;

        drawTechPanel(context, OVERLAY_X, OVERLAY_Y, panelWidth, headPanelHeight, lightMode);
        try {
            drawLayeredHead(context, player.getSkin(), headX, headY);
            drawn = true;
        } catch (Throwable ignored) {
        }

        if (!drawn) {
            drawPlaceholder(context, client, headX, headY, lightMode);
        }

        drawTechPanel(context, OVERLAY_X, namePanelY, panelWidth, namePanelHeight, lightMode);
        int textX = OVERLAY_X + (panelWidth - client.textRenderer.getWidth(shownName)) / 2;
        int textY = namePanelY + (namePanelHeight - client.textRenderer.fontHeight) / 2;
        context.drawText(client.textRenderer, Text.literal(shownName), textX, textY, getTextColor(lightMode), !lightMode);
    }

    private static void drawLayeredHead(DrawContext context, SkinTextures skin, int headX, int headY) {
        Identifier skinTexture = skin.body().texturePath();
        int hatX = headX - HAT_EXPAND;
        int hatY = headY - HAT_EXPAND;
        int hatSize = HEAD_SIZE + HAT_EXPAND * 2;

        context.fill(headX + 2, headY + 2, headX + HEAD_SIZE + 2, headY + HEAD_SIZE + 2, SHADOW_COLOR);
        drawSkinRegion(context, skinTexture, headX, headY, HEAD_SIZE, FACE_U, FACE_V);
        drawSkinRegion(context, skinTexture, hatX, hatY, hatSize, HAT_U, HAT_V);
    }

    private static void drawSkinRegion(DrawContext context, Identifier texture, int x, int y, int size, int u, int v) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                (float) u,
                (float) v,
                size,
                size,
                8,
                8,
                SKIN_TEXTURE_SIZE,
                SKIN_TEXTURE_SIZE,
                -1
        );
    }

    private static void drawPlaceholder(DrawContext context, MinecraftClient client, int headX, int headY, boolean lightMode) {
        context.fill(headX + 2, headY + 2, headX + HEAD_SIZE + 2, headY + HEAD_SIZE + 2, SHADOW_COLOR);
        context.fill(headX, headY, headX + HEAD_SIZE, headY + HEAD_SIZE, getPlaceholderColor(lightMode));

        var player = client.player;
        if (player == null) {
            return;
        }

        String name = player.getName().getString();
        if (name == null) {
            name = "";
        }

        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        int textX = headX + HEAD_SIZE / 2 - client.textRenderer.getWidth(initial) / 2;
        int textY = headY + HEAD_SIZE / 2 - client.textRenderer.fontHeight / 2;
        context.drawText(client.textRenderer, initial, textX, textY, lightMode ? 0xFFFFFFFF : 0xFF081018, false);
    }

    private static void drawTechPanel(DrawContext context, int x, int y, int width, int height, boolean lightMode) {
        int glowColor = lightMode ? 0x220099CC : 0x3300FFE0;
        int fillColor = lightMode ? 0xCCF0F4F8 : 0xCC101825;
        int borderColor = lightMode ? 0xCC0099CC : 0xCC00D4BE;
        fillChamferedRect(context, x - 1, y - 1, width + 2, height + 2, PANEL_CHAMFER + 1, glowColor);
        fillChamferedRect(context, x, y, width, height, PANEL_CHAMFER, fillColor);
        drawTechBorder(context, x, y, width, height, borderColor);
    }

    private static void drawTechBorder(DrawContext context, int x, int y, int width, int height, int borderColor) {
        context.fill(x + PANEL_CHAMFER, y, x + width - PANEL_CHAMFER, y + 1, borderColor);
        context.fill(x + PANEL_CHAMFER, y + height - 1, x + width - PANEL_CHAMFER, y + height, borderColor);
        drawChamferCorner(context, x, y, -1, -1, borderColor);
        drawChamferCorner(context, x + width, y + height, 1, 1, borderColor);
    }

    private static void drawChamferCorner(DrawContext context, int cornerX, int cornerY, int xDir, int yDir, int color) {
        for (int index = 0; index < PANEL_CHAMFER; index++) {
            int drawX;
            int drawY;
            if (xDir < 0 && yDir < 0) {
                drawX = cornerX + index;
                drawY = cornerY + PANEL_CHAMFER - 1 - index;
            } else {
                drawX = cornerX - PANEL_CHAMFER + index;
                drawY = cornerY - 1 - index;
            }
            context.fill(drawX, drawY, drawX + 1, drawY + 1, color);
        }
    }

    private static void fillChamferedRect(DrawContext context, int x, int y, int width, int height, int chamfer, int color) {
        int clampedChamfer = Math.min(chamfer, Math.min(width, height) / 2);
        if (clampedChamfer <= 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        context.fill(x + clampedChamfer, y, x + width - clampedChamfer, y + height, color);
        context.fill(x, y + clampedChamfer, x + clampedChamfer, y + height - clampedChamfer, color);
        context.fill(x + width - clampedChamfer, y + clampedChamfer, x + width, y + height - clampedChamfer, color);
        for (int index = 0; index < clampedChamfer; index++) {
            int offset = clampedChamfer - index;
            context.fill(x + offset, y + index, x + clampedChamfer, y + index + 1, color);
            context.fill(x + width - clampedChamfer, y + index, x + width - offset, y + index + 1, color);
            context.fill(x + offset, y + height - 1 - index, x + clampedChamfer, y + height - index, color);
            context.fill(x + width - clampedChamfer, y + height - 1 - index, x + width - offset, y + height - index, color);
        }
    }

    private static boolean isLightMode() {
        return PhoneSettingsClient.isLightModeEnabled();
    }

    private static int getPlaceholderColor(boolean lightMode) {
        return lightMode ? 0xFF4A90E2 : 0xFF00B4A0;
    }

    private static int getTextColor(boolean lightMode) {
        return lightMode ? 0xFF1A2A3A : 0xFFE0F7FF;
    }
}
