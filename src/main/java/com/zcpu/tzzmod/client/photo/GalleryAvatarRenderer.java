package com.zcpu.tzzmod.client.photo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class GalleryAvatarRenderer {
    private static final int FACE_U = 8;
    private static final int FACE_V = 8;
    private static final int HAT_U = 40;
    private static final int HAT_V = 8;
    private static final int SKIN_TEXTURE_SIZE = 64;

    private GalleryAvatarRenderer() {
    }

    public static void drawAvatarCentered(DrawContext context, String uploaderUuid, int centerX, int y, int size, int fallbackColor) {
        drawAvatar(context, uploaderUuid, centerX - size / 2, y, size, fallbackColor);
    }

    public static void drawAvatar(DrawContext context, String uploaderUuid, int x, int y, int size, int fallbackColor) {
        Identifier skinTexture = resolveSkinTexture(uploaderUuid);
        if (skinTexture == null) {
            context.fill(x, y, x + size, y + size, fallbackColor);
            return;
        }

        int hatExpand = Math.max(1, size / 8);
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                skinTexture,
                x,
                y,
                (float) FACE_U,
                (float) FACE_V,
                size,
                size,
                8,
                8,
                SKIN_TEXTURE_SIZE,
                SKIN_TEXTURE_SIZE,
                -1
        );
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                skinTexture,
                x - hatExpand,
                y - hatExpand,
                (float) HAT_U,
                (float) HAT_V,
                size + hatExpand * 2,
                size + hatExpand * 2,
                8,
                8,
                SKIN_TEXTURE_SIZE,
                SKIN_TEXTURE_SIZE,
                -1
        );
    }

    private static Identifier resolveSkinTexture(String uploaderUuid) {
        try {
            UUID uuid = parseUuid(uploaderUuid);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                if (client.player != null && client.player.getUuid().equals(uuid)) {
                    return client.player.getSkin().body().texturePath();
                }
                if (client.getNetworkHandler() != null) {
                    var entry = client.getNetworkHandler().getPlayerListEntry(uuid);
                    if (entry != null) {
                        SkinTextures skin = entry.getSkinTextures();
                        return skin.body().texturePath();
                    }
                }
            }

            return DefaultSkinHelper.getSkinTextures(uuid).body().texturePath();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static UUID parseUuid(String uploaderUuid) {
        if (uploaderUuid == null || uploaderUuid.isBlank()) {
            return new UUID(0L, 0L);
        }
        try {
            return UUID.fromString(uploaderUuid);
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(uploaderUuid.getBytes(StandardCharsets.UTF_8));
        }
    }
}