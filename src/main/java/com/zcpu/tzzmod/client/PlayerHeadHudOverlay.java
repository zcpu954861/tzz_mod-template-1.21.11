package com.zcpu.tzzmod.client;

import com.zcpu.tzzmod.util.NullSafety;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class PlayerHeadHudOverlay {
    private static final int HEAD_SIZE = 32;
    private static final int HEAD_X = 8;
    private static final int HEAD_Y = 8;
    private static final int NAME_SPACING = 6;
    private static final int PLACEHOLDER_COLOR = 0xFF4A90E2;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
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
        if (client == null || client.player == null || client.currentScreen != null) {
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
        boolean drawn = false;
        try {
            drawLayeredHead(context, player.getSkin());
            drawn = true;
        } catch (Throwable ignored) {
        }

        if (!drawn) {
            drawPlaceholder(context, client);
        }

        String name = player.getName().getString();
        if (name == null) {
            name = "";
        }

        int nameY = HEAD_Y + HEAD_SIZE + NAME_SPACING;
        context.drawTextWithShadow(client.textRenderer, Text.literal(name), HEAD_X, nameY, TEXT_COLOR);
    }

    private static void drawLayeredHead(DrawContext context, SkinTextures skin) {
        Identifier skinTexture = skin.body().texturePath();
        int hatX = HEAD_X - HAT_EXPAND;
        int hatY = HEAD_Y - HAT_EXPAND;
        int hatSize = HEAD_SIZE + HAT_EXPAND * 2;

        context.fill(HEAD_X + 2, HEAD_Y + 2, HEAD_X + HEAD_SIZE + 2, HEAD_Y + HEAD_SIZE + 2, SHADOW_COLOR);
        drawSkinRegion(context, skinTexture, HEAD_X, HEAD_Y, HEAD_SIZE, FACE_U, FACE_V);
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

    private static void drawPlaceholder(DrawContext context, MinecraftClient client) {
        context.fill(HEAD_X + 2, HEAD_Y + 2, HEAD_X + HEAD_SIZE + 2, HEAD_Y + HEAD_SIZE + 2, SHADOW_COLOR);
        context.fill(HEAD_X, HEAD_Y, HEAD_X + HEAD_SIZE, HEAD_Y + HEAD_SIZE, PLACEHOLDER_COLOR);

        var player = client.player;
        if (player == null) {
            return;
        }

        String name = player.getName().getString();
        if (name == null) {
            name = "";
        }

        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        int textX = HEAD_X + HEAD_SIZE / 2 - client.textRenderer.getWidth(initial) / 2;
        int textY = HEAD_Y + HEAD_SIZE / 2 - client.textRenderer.fontHeight / 2;
        context.drawTextWithShadow(client.textRenderer, initial, textX, textY, TEXT_COLOR);
    }
}
