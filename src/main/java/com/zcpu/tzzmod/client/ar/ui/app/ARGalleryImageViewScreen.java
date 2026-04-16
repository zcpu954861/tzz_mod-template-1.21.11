package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.photo.GalleryClient;
import com.zcpu.tzzmod.client.photo.PhotoManager;
import com.zcpu.tzzmod.client.photo.PhotoMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;

/**
 * AR Gallery image viewer screen.
 * Displays a single photo at correct aspect ratio with tech frame.
 */
public class ARGalleryImageViewScreen extends AbstractARScreen {
    private final Path photoPath;
    private final PhotoMetadata metadata;
    private final boolean isOnlineMode;
    private final GalleryClient.OnlinePhotoEntry onlineEntry;

    private int imgW = 1, imgH = 1;
    private Identifier textureId;
    private Path renderedPhotoPath;

    private boolean uploading = false;
    private float uploadProgress = 0f;
    private boolean uploadSuccess = false;
    private long uploadSuccessTime = -1;

    private boolean downloading = false;
    private float downloadProgress = 0f;
    private boolean downloaded = false;

    public ARGalleryImageViewScreen(Screen parent, Path photoPath, PhotoMetadata metadata, boolean isOnlineMode) {
        super(Text.translatable("phone.tzz_mod.gallery.viewer"), parent);
        this.photoPath = photoPath;
        this.metadata = metadata;
        this.isOnlineMode = isOnlineMode;
        this.onlineEntry = null;
    }

    public ARGalleryImageViewScreen(Screen parent, GalleryClient.OnlinePhotoEntry onlineEntry, boolean isOnlineMode) {
        super(Text.translatable("phone.tzz_mod.gallery.viewer"), parent);
        this.onlineEntry = onlineEntry;
        this.isOnlineMode = isOnlineMode;
        this.photoPath = onlineEntry.localCachePath();
        this.metadata = onlineEntry.metadata();
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();

        if (photoPath != null && java.nio.file.Files.exists(photoPath)) {
            loadDisplayImage(photoPath);
        } else if (isOnlineMode && onlineEntry != null) {
            downloadPhoto();
        }

        boolean isMultiplayer = MinecraftClient.getInstance() != null
                && MinecraftClient.getInstance().getCurrentServerEntry() != null;
        boolean canUpload = !isOnlineMode && isMultiplayer && GalleryClient.isOnlineGalleryEnabled();

        if (canUpload) {
            int btnW = s(80);
            addARGhostButton(Text.translatable("phone.tzz_mod.gallery.upload"),
                    contentX + contentWidth / 2 - btnW / 2,
                    contentY + contentHeight - s(18), btnW, s(14),
                    button -> startUpload());
        }

        if (isOnlineMode && onlineEntry != null) {
            boolean isUploader = isCurrentPlayerUploader();
            boolean isOp = PhoneChatClient.isOp();
            if (isUploader || isOp) {
                int btnW = s(80);
                int btnX = contentX + contentWidth / 2 - btnW / 2;
                addARGhostButton(isOp
                                ? Text.translatable("phone.tzz_mod.gallery.force_delete")
                                : Text.translatable("phone.tzz_mod.gallery.cloud_delete"),
                        btnX, contentY + contentHeight - s(18), btnW, s(14),
                        button -> {
                            GalleryClient.deleteOnlinePhoto(onlineEntry.photoId(), isOp);
                            close();
                        });
            }
        }
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.gallery.viewer"),
                contentX + contentWidth / 2, contentY + s(2), themeAccent());

        if (!downloaded && downloading) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.gallery.downloading"),
                    contentX + contentWidth / 2, contentY + contentHeight / 2 - s(12), themeText());
            renderProgressBar(context, contentX + s(20), contentY + contentHeight / 2,
                    contentWidth - s(40), s(6), downloadProgress);
            drawScaledCenteredText(context, Text.literal(String.format("%.0f%%", downloadProgress * 100)),
                    contentX + contentWidth / 2, contentY + contentHeight / 2 + s(10), themeTextDim());
            return;
        }

        if (textureId == null) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.gallery.no_image"),
                    contentX + contentWidth / 2, contentY + contentHeight / 2, themeTextDim());
            return;
        }

        // Calculate display area preserving aspect ratio
        int availW = contentWidth - s(12);
        int availH = contentHeight - s(40);
        float imgAspect = (float) imgW / imgH;
        float availAspect = (float) availW / availH;

        int drawW, drawH;
        if (imgAspect > availAspect) {
            drawW = availW;
            drawH = Math.max(1, Math.round(availW / imgAspect));
        } else {
            drawH = availH;
            drawW = Math.max(1, Math.round(availH * imgAspect));
        }

        int drawX = contentX + (contentWidth - drawW) / 2;
        int drawY = contentY + s(16) + (availH - drawH) / 2;

        context.drawTexturedQuad(textureId, drawX, drawY, drawX + drawW, drawY + drawH, 0f, 1f, 0f, 1f);

        // Tech photo frame
        drawTechPhotoFrame(context, drawX, drawY, drawW, drawH);

        if (uploading) {
            renderProgressBar(context, contentX + s(20), contentY + contentHeight - s(26),
                    contentWidth - s(40), s(4), uploadProgress);
            drawScaledCenteredText(context, Text.literal(String.format("%.0f%%", uploadProgress * 100)),
                    contentX + contentWidth / 2, contentY + contentHeight - s(20), themeTextDim());
        }

        if (uploadSuccess && uploadSuccessTime > 0 && System.currentTimeMillis() - uploadSuccessTime < 3000) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.gallery.upload_success"),
                    contentX + contentWidth / 2, contentY + contentHeight - s(26), 0xFF44FF88);
        }
    }

    private void drawTechPhotoFrame(DrawContext context, int x, int y, int w, int h) {
        int lineLen = Math.min(w, h) / 4;
        int cut = Math.max(2, s(2));
        int color = themeAccent();
        int pad = s(2);
        int fx = x - pad, fy = y - pad, fw = w + pad * 2, fh = h + pad * 2;

        context.fill(fx + cut, fy, fx + lineLen, fy + 1, color);
        context.fill(fx, fy + cut, fx + 1, fy + lineLen, color);
        for (int i = 0; i < cut; i++)
            context.fill(fx + cut - i, fy + i, fx + cut - i + 1, fy + i + 1, color);

        context.fill(fx + fw - lineLen, fy, fx + fw, fy + 1, color);
        context.fill(fx, fy + fh - lineLen, fx + 1, fy + fh, color);

        context.fill(fx + fw - lineLen, fy + fh - 1, fx + fw - cut, fy + fh, color);
        context.fill(fx + fw - 1, fy + fh - lineLen, fx + fw, fy + fh - cut, color);
        for (int i = 0; i < cut; i++)
            context.fill(fx + fw - cut + i - 1, fy + fh - 1 - i, fx + fw - cut + i, fy + fh - i, color);
    }

    private void renderProgressBar(DrawContext context, int x, int y, int w, int h, float progress) {
        fillChamferedRect(context, x, y, w, h, s(1), isLightMode() ? 0x33D8E4F0 : 0x33101825);
        int fillW = Math.max(0, Math.round(w * Math.min(1f, progress)));
        if (fillW > 0) fillChamferedRect(context, x, y, fillW, h, s(1), themeAccent());
        context.fill(x, y, x + w, y + 1, themeBorder());
        context.fill(x, y + h - 1, x + w, y + h, themeBorder());
    }

    private void startUpload() {
        if (uploading || photoPath == null) return;
        uploading = true; uploadProgress = 0f;
        GalleryClient.uploadPhoto(photoPath, metadata,
                p -> uploadProgress = p,
                ok -> { uploading = false; if (ok) { uploadSuccess = true; uploadSuccessTime = System.currentTimeMillis(); } });
    }

    private void downloadPhoto() {
        if (downloading || onlineEntry == null) return;
        downloading = true; downloadProgress = 0f;
        GalleryClient.downloadPhoto(onlineEntry,
                p -> downloadProgress = p,
                path -> {
                    downloading = false;
                    if (path != null) {
                        loadDisplayImage(path);
                    }
                });
    }

    private void loadDisplayImage(Path sourcePath) {
        PhotoManager.CachedImage cachedImage = PhotoManager.getViewerImage(
                sourcePath,
                Math.max(1, contentWidth - s(12)),
                Math.max(1, contentHeight - s(40))
        );
        renderedPhotoPath = cachedImage.path();
        imgW = cachedImage.width();
        imgH = cachedImage.height();
        textureId = renderedPhotoPath != null ? PhotoManager.getOrLoadTexture(renderedPhotoPath) : null;
        downloaded = textureId != null;
    }

    private boolean isCurrentPlayerUploader() {
        if (onlineEntry == null || onlineEntry.uploaderUuid() == null) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.player != null && mc.player.getUuidAsString().equals(onlineEntry.uploaderUuid());
    }
}
