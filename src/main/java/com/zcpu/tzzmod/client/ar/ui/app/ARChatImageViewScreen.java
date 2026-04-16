package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.phone.chat.ChatImageClient;
import com.zcpu.tzzmod.client.phone.chat.ChatUiUtil;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.photo.PhotoManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;

public class ARChatImageViewScreen extends AbstractARScreen {
    private final PhoneChatClient.ChatMessageData message;

    private ButtonWidget zoomButton;
    private Path sourcePath;
    private Path renderedPath;
    private Identifier textureId;
    private int imageWidth = 1;
    private int imageHeight = 1;
    private boolean downloading;
    private float downloadProgress;
    private float zoomMultiplier = 1.0F;

    public ARChatImageViewScreen(net.minecraft.client.gui.screen.Screen parent, PhoneChatClient.ChatMessageData message) {
        super(Text.translatable("phone.tzz_mod.chat.image_viewer"), parent);
        this.message = message;
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();
        zoomButton = addARButton(Text.translatable("phone.tzz_mod.gallery.zoom"), contentX + contentWidth - s(40), contentY + s(2), s(38), s(14), button -> toggleZoom());
        zoomButton.active = false;

        Path fullPath = ChatImageClient.getFullImagePath(message.imageId());
        if (fullPath != null && Files.exists(fullPath)) {
            loadImage(fullPath);
        } else {
            requestFullImage();
        }
    }

    private void toggleZoom() {
        zoomMultiplier = zoomMultiplier >= 1.6F ? 1.0F : 1.6F;
    }

    private void requestFullImage() {
        if (downloading) {
            return;
        }
        downloading = true;
        downloadProgress = 0.0F;
        ChatImageClient.downloadFullImage(message.imageId(), progress -> downloadProgress = progress, path -> {
            downloading = false;
            if (path != null) {
                loadImage(path);
            }
        });
    }

    private void loadImage(Path path) {
        sourcePath = path;
        PhotoManager.CachedImage cachedImage = PhotoManager.getViewerImage(path, Math.max(1, contentWidth - s(10)), Math.max(1, contentHeight - s(22)));
        renderedPath = cachedImage.path();
        imageWidth = cachedImage.width();
        imageHeight = cachedImage.height();
        textureId = renderedPath != null ? PhotoManager.getOrLoadTexture(renderedPath) : null;
        if (zoomButton != null) {
            zoomButton.active = textureId != null && sourcePath != null && Files.exists(sourcePath);
        }
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.image_viewer"), contentX + contentWidth / 2, contentY + s(6), themeText());

        if (downloading) {
            int barX = contentX + s(10);
            int barY = contentY + contentHeight / 2;
            int barW = contentWidth - s(20);
            int barH = s(5);
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.image_loading"), contentX + contentWidth / 2, barY - s(10), themeTextDim());
            ChatUiUtil.drawAngularFrame(context, barX, barY, barW, barH, s(2), 0x22091420, themeBorder());
            int fillW = Math.max(0, Math.min(barW, Math.round(barW * downloadProgress)));
            if (fillW > 0) {
                ChatUiUtil.drawAngularFrame(context, barX, barY, fillW, barH, s(2), themeAccent(), themeAccent());
            }
            return;
        }

        if (textureId == null) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.gallery.no_image"), contentX + contentWidth / 2, contentY + contentHeight / 2, themeTextDim());
            return;
        }

        int maxWidth = contentWidth - s(12);
        int maxHeight = contentHeight - s(28);
        int[] fitted = ChatUiUtil.fitSize(imageWidth, imageHeight, maxWidth, maxHeight);
        int drawWidth = Math.round(fitted[0] * zoomMultiplier);
        int drawHeight = Math.round(fitted[1] * zoomMultiplier);
        int drawX = contentX + (contentWidth - drawWidth) / 2;
        int drawY = contentY + s(18) + (maxHeight - drawHeight) / 2;

        context.enableScissor(contentX, contentY + s(18), contentX + contentWidth, contentY + contentHeight);
        context.drawTexturedQuad(textureId, drawX, drawY, drawX + drawWidth, drawY + drawHeight, 0.0F, 1.0F, 0.0F, 1.0F);
        context.disableScissor();
        ChatUiUtil.drawAngularFrame(context, drawX - s(2), drawY - s(2), drawWidth + s(4), drawHeight + s(4), s(3), 0x00000000, themeAccent());
    }
}