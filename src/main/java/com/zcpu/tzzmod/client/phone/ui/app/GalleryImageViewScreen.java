package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
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
 * Full image viewer screen for Gallery APP (phone version).
 * Displays the photo at correct aspect ratio with a tech-style frame.
 * Supports upload to online gallery and deletion.
 */
public class GalleryImageViewScreen extends AbstractPhoneScreen {
    private final Path photoPath;
    private final PhotoMetadata metadata;
    private final boolean isOnlineMode;
    private final GalleryClient.OnlinePhotoEntry onlineEntry;

    // Photo dimensions
    private int imgW = 1, imgH = 1;
    private Identifier textureId;
    private Path renderedPhotoPath;
    private Path currentPhotoPath;

    // Upload state
    private boolean uploading = false;
    private float uploadProgress = 0f;
    private boolean uploadSuccess = false;
    private long uploadSuccessTime = -1;

    // Download state (for online photos not yet cached)
    private boolean downloading = false;
    private float downloadProgress = 0f;
    private boolean downloaded = false;

    /**
     * Constructor for local photo viewing.
     */
    public GalleryImageViewScreen(Screen parent, Path photoPath, PhotoMetadata metadata, boolean isOnlineMode) {
        super(Text.translatable("phone.tzz_mod.gallery.viewer"), parent);
        this.photoPath = photoPath;
        this.metadata = metadata;
        this.isOnlineMode = isOnlineMode;
        this.onlineEntry = null;
        this.currentPhotoPath = photoPath;
    }

    /**
     * Constructor for online photo viewing.
     */
    public GalleryImageViewScreen(Screen parent, GalleryClient.OnlinePhotoEntry onlineEntry, boolean isOnlineMode) {
        super(Text.translatable("phone.tzz_mod.gallery.viewer"), parent);
        this.onlineEntry = onlineEntry;
        this.isOnlineMode = isOnlineMode;
        // Online photo may need downloading
        this.photoPath = onlineEntry.localCachePath();
        this.metadata = onlineEntry.metadata();
        this.currentPhotoPath = this.photoPath;
    }

    @Override
    protected void init() {
        super.init();

        // Back button
        addPhoneButton(Text.translatable("phone.tzz_mod.back"),
                contentX, contentY + contentHeight - s(24), s(72), s(20),
                button -> close());

        // Load texture and dimensions
        if (photoPath != null && java.nio.file.Files.exists(photoPath)) {
            loadDisplayImage(photoPath);
        } else if (isOnlineMode && onlineEntry != null) {
            // Need to download from server first
            downloadPhoto();
        }

        boolean isMultiplayer = isMultiplayerContext();
        boolean canUpload = !isOnlineMode && isMultiplayer && GalleryClient.isOnlineGalleryEnabled();
        boolean canDeleteOnline = isOnlineMode && onlineEntry != null && (isCurrentPlayerUploader() || PhoneChatClient.isOp());
        boolean isOp = PhoneChatClient.isOp();
        Text onlineDeleteLabel = isOp
            ? Text.translatable("phone.tzz_mod.gallery.force_delete")
            : Text.translatable("phone.tzz_mod.gallery.cloud_delete");

        int actionCount = 1;
        if (canUpload) {
            actionCount++;
        }
        if (!isOnlineMode || canDeleteOnline) {
            actionCount++;
        }

        int gap = s(4);
        int startX = contentX + s(4);
        int availableWidth = contentWidth - s(8);
        int btnWidth = Math.max(s(36), (availableWidth - gap * (actionCount - 1)) / actionCount);
        int btnHeight = s(18);
        int btnY = contentY + contentHeight - s(48);
        int currentX = startX;

        if (canUpload) {
            addPhonePrimaryButton(Text.translatable("phone.tzz_mod.gallery.upload"),
                    currentX, btnY, btnWidth, btnHeight, button -> startUpload());
            currentX += btnWidth + gap;
        }

        if (!isOnlineMode) {
            addPhoneButton(Text.translatable("phone.tzz_mod.gallery.delete"),
                    currentX, btnY, btnWidth, btnHeight, button -> deleteLocalPhoto());
            currentX += btnWidth + gap;
        } else if (canDeleteOnline) {
            addPhoneButton(onlineDeleteLabel,
                    currentX, btnY, btnWidth, btnHeight,
                    button -> {
                        if (isOp) {
                            forceDeleteFromCloud();
                        } else {
                            deleteFromCloud();
                        }
                    });
            currentX += btnWidth + gap;
        }

        addPhoneButton(Text.translatable("phone.tzz_mod.gallery.zoom"),
                currentX, btnY, btnWidth, btnHeight, button -> openZoomView());
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // Title
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.gallery.viewer"),
                contentX + contentWidth / 2, contentY + s(4));

        if (!downloaded && downloading) {
            // Show download progress
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.gallery.downloading"),
                    contentX + contentWidth / 2, contentY + contentHeight / 2 - s(20), themeText());
            renderProgressBar(context, contentX + s(12), contentY + contentHeight / 2,
                    contentWidth - s(24), s(8), downloadProgress);
            String pct = String.format("%.0f%%", downloadProgress * 100);
            drawScaledCenteredText(context, Text.literal(pct),
                    contentX + contentWidth / 2, contentY + contentHeight / 2 + s(12), themeTextDim());
            return;
        }

        if (textureId == null) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.gallery.no_image"),
                    contentX + contentWidth / 2, contentY + contentHeight / 2, themeTextDim());
            return;
        }

        // Calculate display rectangle preserving aspect ratio
        int availW = contentWidth - s(16);
        int availH = contentHeight - s(80); // Leave room for buttons
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
        int drawY = contentY + s(20) + (availH - drawH) / 2;

        // Draw the photo
        context.drawTexturedQuad(textureId, drawX, drawY, drawX + drawW, drawY + drawH,
                0f, 1f, 0f, 1f);

        // Draw tech photo frame around the image
        drawTechPhotoFrame(context, drawX, drawY, drawW, drawH);

        // Upload progress bar
        if (uploading) {
            renderProgressBar(context, contentX + s(12), contentY + contentHeight - s(70),
                    contentWidth - s(24), s(6), uploadProgress);
            String pct = String.format("%.0f%%", uploadProgress * 100);
            drawScaledCenteredText(context, Text.literal(pct),
                    contentX + contentWidth / 2, contentY + contentHeight - s(62), themeTextDim());
        }

        // Upload success message
        if (uploadSuccess && uploadSuccessTime > 0) {
            long elapsed = System.currentTimeMillis() - uploadSuccessTime;
            if (elapsed < 3000) {
                drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.gallery.upload_success"),
                        contentX + contentWidth / 2, contentY + contentHeight - s(70),
                        0xFF44FF88);
            }
        }
    }

    /**
     * Draw a tech-style photo frame with straight lines forming a stylized border.
     * Lines at: top-left corner, top edge, bottom edge, bottom-right corner.
     * The frame follows the image size.
     */
    private void drawTechPhotoFrame(DrawContext context, int x, int y, int w, int h) {
        int lineLen = Math.min(w, h) / 4;
        int cut = Math.max(2, s(3));
        int color = themeAccent();
        int pad = s(3); // Frame padding outside the image

        int fx = x - pad;
        int fy = y - pad;
        int fw = w + pad * 2;
        int fh = h + pad * 2;

        // Top-left: horizontal + vertical + diagonal
        context.fill(fx + cut, fy, fx + lineLen, fy + 1, color);
        context.fill(fx, fy + cut, fx + 1, fy + lineLen, color);
        for (int i = 0; i < cut; i++) {
            context.fill(fx + cut - i, fy + i, fx + cut - i + 1, fy + i + 1, color);
        }

        // Top-right: horizontal line
        context.fill(fx + fw - lineLen, fy, fx + fw, fy + 1, color);

        // Bottom-left: vertical line
        context.fill(fx, fy + fh - lineLen, fx + 1, fy + fh, color);

        // Bottom-right: horizontal + vertical + diagonal
        context.fill(fx + fw - lineLen, fy + fh - 1, fx + fw - cut, fy + fh, color);
        context.fill(fx + fw - 1, fy + fh - lineLen, fx + fw, fy + fh - cut, color);
        for (int i = 0; i < cut; i++) {
            context.fill(fx + fw - cut + i - 1, fy + fh - 1 - i,
                    fx + fw - cut + i, fy + fh - i, color);
        }
    }

    private void renderProgressBar(DrawContext context, int x, int y, int w, int h, float progress) {
        // Background
        fillChamferedRect(context, x, y, w, h, s(2), isLightMode() ? 0x33D8E4F0 : 0x33101825);
        // Fill
        int fillW = Math.max(0, Math.round(w * Math.min(1f, progress)));
        if (fillW > 0) {
            fillChamferedRect(context, x, y, fillW, h, s(2), themeAccent());
        }
        // Border
        context.fill(x, y, x + w, y + 1, themeBorder());
        context.fill(x, y + h - 1, x + w, y + h, themeBorder());
    }

    private void startUpload() {
        if (uploading || photoPath == null) return;
        uploading = true;
        uploadProgress = 0f;
        GalleryClient.uploadPhoto(photoPath, metadata, progress -> {
            uploadProgress = progress;
        }, success -> {
            uploading = false;
            if (success) {
                uploadSuccess = true;
                uploadSuccessTime = System.currentTimeMillis();
            }
        });
    }

    private void downloadPhoto() {
        if (downloading || onlineEntry == null) return;
        downloading = true;
        downloadProgress = 0f;
        GalleryClient.downloadPhoto(onlineEntry, progress -> {
            downloadProgress = progress;
        }, (localPath) -> {
            downloading = false;
            if (localPath != null) {
                loadDisplayImage(localPath);
            }
        });
    }

    private void loadDisplayImage(Path sourcePath) {
        currentPhotoPath = sourcePath;
        PhotoManager.CachedImage cachedImage = PhotoManager.getViewerImage(
                sourcePath,
                Math.max(1, contentWidth - s(16)),
                Math.max(1, contentHeight - s(84))
        );
        renderedPhotoPath = cachedImage.path();
        imgW = cachedImage.width();
        imgH = cachedImage.height();
        textureId = renderedPhotoPath != null ? PhotoManager.getOrLoadTexture(renderedPhotoPath) : null;
        downloaded = textureId != null;
    }

    private void deleteLocalPhoto() {
        if (photoPath != null && PhotoManager.deletePhoto(photoPath)) {
            close();
        }
    }

    private void openZoomView() {
        if (client == null) {
            return;
        }
        Path sourcePath = getZoomSourcePath();
        if (sourcePath != null && java.nio.file.Files.exists(sourcePath)) {
            client.setScreen(new GalleryZoomScreen(this, sourcePath));
        }
    }

    private Path getZoomSourcePath() {
        if (currentPhotoPath != null && java.nio.file.Files.exists(currentPhotoPath)) {
            return currentPhotoPath;
        }
        return renderedPhotoPath;
    }

    private void deleteFromCloud() {
        if (onlineEntry == null) return;
        GalleryClient.deleteOnlinePhoto(onlineEntry.photoId(), false);
        close();
    }

    private void forceDeleteFromCloud() {
        if (onlineEntry == null) return;
        GalleryClient.deleteOnlinePhoto(onlineEntry.photoId(), true);
        close();
    }

    private boolean isCurrentPlayerUploader() {
        if (onlineEntry == null || onlineEntry.uploaderUuid() == null) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return false;
        return mc.player.getUuidAsString().equals(onlineEntry.uploaderUuid());
    }

    private boolean isMultiplayerContext() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.getCurrentServerEntry() != null;
    }
}
