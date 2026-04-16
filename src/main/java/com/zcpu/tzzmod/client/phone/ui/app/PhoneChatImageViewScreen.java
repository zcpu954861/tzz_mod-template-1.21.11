package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.chat.ChatImageClient;
import com.zcpu.tzzmod.client.phone.chat.ChatUiUtil;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.phone.ui.PhoneButtonWidget;
import com.zcpu.tzzmod.client.photo.PhotoManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;

public class PhoneChatImageViewScreen extends AbstractPhoneScreen {
    private final PhoneChatClient.ChatMessageData message;

    private ButtonWidget zoomButton;
    private Path sourcePath;
    private Path renderedPath;
    private Identifier textureId;
    private int imageWidth = 1;
    private int imageHeight = 1;
    private boolean downloading;
    private float downloadProgress;

    public PhoneChatImageViewScreen(Screen parent, PhoneChatClient.ChatMessageData message) {
        super(Text.translatable("phone.tzz_mod.chat.image_viewer"), parent);
        this.message = message;
    }

    @Override
    protected void init() {
        super.init();

        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(72), s(20), button -> close());
        zoomButton = addPhoneButton(
                Text.translatable("phone.tzz_mod.gallery.zoom"),
                contentX + contentWidth - s(72),
                contentY + contentHeight - s(24),
                s(72),
                s(20),
                PhoneButtonWidget.Variant.SECONDARY,
                () -> false,
                button -> openZoomView()
        );
        zoomButton.active = false;

        Path fullPath = ChatImageClient.getFullImagePath(message.imageId());
        if (fullPath != null && Files.exists(fullPath)) {
            loadImage(fullPath);
        } else {
            requestFullImage();
        }
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
        PhotoManager.CachedImage cachedImage = PhotoManager.getViewerImage(path, Math.max(1, contentWidth - s(12)), Math.max(1, contentHeight - s(64)));
        renderedPath = cachedImage.path();
        imageWidth = cachedImage.width();
        imageHeight = cachedImage.height();
        textureId = renderedPath != null ? PhotoManager.getOrLoadTexture(renderedPath) : null;
        if (zoomButton != null) {
            zoomButton.active = textureId != null && sourcePath != null && Files.exists(sourcePath);
        }
    }

    private void openZoomView() {
        if (client == null || sourcePath == null || !Files.exists(sourcePath)) {
            return;
        }
        client.setScreen(new GalleryZoomScreen(this, sourcePath));
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.chat.image_viewer"), contentX + contentWidth / 2, contentY + s(8));

        if (downloading) {
            int barX = contentX + s(12);
            int barY = contentY + contentHeight / 2;
            int barW = contentWidth - s(24);
            int barH = s(7);
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.image_loading"), contentX + contentWidth / 2, barY - s(14), themeTextDim());
            ChatUiUtil.drawAngularFrame(context, barX, barY, barW, barH, s(2), isLightMode() ? 0x44D8E4F0 : 0x330A1622, themeBorder());
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
        int maxHeight = contentHeight - s(56);
        int[] fittedSize = ChatUiUtil.fitSize(imageWidth, imageHeight, maxWidth, maxHeight);
        int drawWidth = fittedSize[0];
        int drawHeight = fittedSize[1];
        int drawX = contentX + (contentWidth - drawWidth) / 2;
        int drawY = contentY + s(22) + (maxHeight - drawHeight) / 2;

        context.drawTexturedQuad(textureId, drawX, drawY, drawX + drawWidth, drawY + drawHeight, 0.0F, 1.0F, 0.0F, 1.0F);
        ChatUiUtil.drawAngularFrame(context, drawX - s(2), drawY - s(2), drawWidth + s(4), drawHeight + s(4), s(4), 0x00000000, themeAccent());
    }
}