package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.photo.GalleryAvatarRenderer;
import com.zcpu.tzzmod.client.photo.GalleryClient;
import com.zcpu.tzzmod.client.photo.PhotoManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Phone Gallery APP screen.
 * Shows photo thumbnails in a 4-column grid with scrolling support.
 * Supports "Local" and "Online Gallery" mode tabs.
 */
public class GalleryAppScreen extends AbstractPhoneScreen {
    private static final int COLS = 4;

    private enum GalleryMode { LOCAL, ONLINE }
    private GalleryMode currentMode = GalleryMode.LOCAL;

    private List<PhotoManager.PhotoEntry> localPhotos = new ArrayList<>();
    private List<GalleryClient.OnlinePhotoEntry> onlinePhotos = new ArrayList<>();

    private double scrollOffset;
    private double targetScroll;
    private boolean onlineGalleryEnabled = false;
    private long observedLocalRevision = -1L;

    public GalleryAppScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.gallery"), parent);
    }

    @Override
    protected void init() {
        super.init();

        // Back button
        addPhoneButton(Text.translatable("phone.tzz_mod.back"),
                contentX, contentY + contentHeight - s(24), s(72), s(20),
                button -> close());

        // Check if online gallery is enabled (multiplayer + admin setting)
        onlineGalleryEnabled = GalleryClient.isOnlineGalleryEnabled();

        // Mode tab buttons
        int tabW = onlineGalleryEnabled ? (contentWidth - s(4)) / 2 : contentWidth;
        int tabY = contentY + s(20);
        addPhoneTabButton(Text.translatable("phone.tzz_mod.gallery.local"),
                contentX, tabY, tabW, s(16),
                () -> currentMode == GalleryMode.LOCAL,
                button -> {
                    currentMode = GalleryMode.LOCAL;
                    scrollOffset = 0;
                    targetScroll = 0;
                    refreshPhotos();
                });
        if (onlineGalleryEnabled) {
            addPhoneTabButton(Text.translatable("phone.tzz_mod.gallery.online"),
                    contentX + tabW + s(4), tabY, tabW, s(16),
                    () -> currentMode == GalleryMode.ONLINE,
                    button -> {
                        currentMode = GalleryMode.ONLINE;
                        scrollOffset = 0;
                        targetScroll = 0;
                        GalleryClient.requestOnlinePhotos();
                        refreshPhotos();
                    });
        }

        refreshPhotos();
    }

    private void refreshPhotos() {
        if (currentMode == GalleryMode.LOCAL) {
            PhotoManager.ensurePhotosDir();
            localPhotos = PhotoManager.loadPhotos();
            observedLocalRevision = PhotoManager.getLocalPhotoRevision();
        } else {
            onlinePhotos = GalleryClient.getOnlinePhotos();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (currentMode == GalleryMode.LOCAL && observedLocalRevision != PhotoManager.getLocalPhotoRevision()) {
            refreshPhotos();
        }
        // Auto-refresh online photos when new data arrives
        if (currentMode == GalleryMode.ONLINE && GalleryClient.hasNewData()) {
            onlinePhotos = GalleryClient.getOnlinePhotos();
            GalleryClient.clearNewDataFlag();
        }
    }

    private int getGridTop() {
        return contentY + s(40);
    }

    private int getGridBottom() {
        return contentY + contentHeight - s(28);
    }

    private int getCellSize() {
        return Math.max(s(10), (contentWidth - s(4) * (COLS - 1)) / COLS);
    }

    private int getTotalRows() {
        int count = currentMode == GalleryMode.LOCAL ? localPhotos.size() : onlinePhotos.size();
        return Math.max(0, (count + COLS - 1) / COLS);
    }

    private int getTotalHeight() {
        int cellSize = getCellSize();
        int rows = getTotalRows();
        return rows * (cellSize + s(14) + s(4));
    }

    private int getMaxScroll() {
        int visibleHeight = Math.max(1, getGridBottom() - getGridTop());
        return Math.max(0, getTotalHeight() - visibleHeight);
    }

    private void clampScroll() {
        double maxScroll = getMaxScroll();
        targetScroll = Math.max(0.0D, Math.min(targetScroll, maxScroll));
        scrollOffset = Math.max(0.0D, Math.min(scrollOffset, maxScroll));
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        scrollOffset += (targetScroll - scrollOffset) * 0.35D;
        clampScroll();

        // Title
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.gallery"),
                contentX + contentWidth / 2, contentY + s(4));

        int gridTop = getGridTop();
        int gridBottom = getGridBottom();
        int cellSize = getCellSize();
        int spacing = s(4);
        int extraH = Math.max(s(14), scaledFontHeight() + s(4));

        context.enableScissor(contentX, gridTop, contentX + contentWidth, gridBottom);

        int currentY = gridTop - (int) Math.round(scrollOffset);

        if (currentMode == GalleryMode.LOCAL) {
            renderLocalGrid(context, mouseX, mouseY, currentY, cellSize, spacing, extraH, gridTop, gridBottom);
        } else {
            renderOnlineGrid(context, mouseX, mouseY, currentY, cellSize, spacing, extraH, gridTop, gridBottom);
        }

        context.disableScissor();

        // Scrollbar
        renderScrollbar(context, gridTop, gridBottom, getTotalHeight(), (int) Math.round(scrollOffset));
    }

    private void renderLocalGrid(DrawContext context, int mouseX, int mouseY,
                                  int startY, int cellSize, int spacing, int extraH,
                                  int gridTop, int gridBottom) {
        if (localPhotos.isEmpty()) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.gallery.empty"),
                    contentX + contentWidth / 2, gridTop + s(30), themeTextDim());
            return;
        }

        for (int i = 0; i < localPhotos.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cellX = contentX + col * (cellSize + spacing);
            int cellY = startY + row * (cellSize + extraH + spacing);

            if (cellY + cellSize + extraH < gridTop || cellY > gridBottom) continue;

            PhotoManager.PhotoEntry entry = localPhotos.get(i);
            renderPhotoThumbnail(context, entry.path(), cellX, cellY, cellSize);

            // Time label below thumbnail
            String timeStr = PhotoManager.formatCaptureTime(entry.metadata().captureTimeMs());
            drawScaledCenteredText(context, Text.literal(timeStr),
                    cellX + cellSize / 2, cellY + cellSize + s(2), themeTextDim());
        }
    }

    private void renderOnlineGrid(DrawContext context, int mouseX, int mouseY,
                                   int startY, int cellSize, int spacing, int extraH,
                                   int gridTop, int gridBottom) {
        if (onlinePhotos.isEmpty()) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.gallery.online_empty"),
                    contentX + contentWidth / 2, gridTop + s(30), themeTextDim());
            return;
        }

        for (int i = 0; i < onlinePhotos.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cellX = contentX + col * (cellSize + spacing);
            int cellY = startY + row * (cellSize + extraH + spacing);

            if (cellY + cellSize + extraH < gridTop || cellY > gridBottom) continue;

            GalleryClient.OnlinePhotoEntry entry = onlinePhotos.get(i);

            // Render thumbnail if cached locally, otherwise placeholder
            if (entry.thumbnailPath() != null) {
                renderPhotoThumbnail(context, entry.thumbnailPath(), cellX, cellY, cellSize);
            } else {
                // Loading placeholder
                fillChamferedRect(context, cellX, cellY, cellSize, cellSize, s(2),
                        isLightMode() ? 0x33D8E4F0 : 0x33101825);
                drawScaledCenteredText(context, Text.literal("..."),
                        cellX + cellSize / 2, cellY + cellSize / 2 - scaledFontHeight() / 2, themeTextDim());
            }

            // Player head avatar below thumbnail (online mode)
            renderPlayerHead(context, entry.uploaderUuid(),
                    cellX + cellSize / 2, cellY + cellSize + s(2));
        }
    }

    /**
     * Render a square center-crop thumbnail of a photo with corner frame decoration.
     */
    private void renderPhotoThumbnail(DrawContext context, Path photoPath, int x, int y, int size) {
        PhotoManager.CachedImage cachedImage = PhotoManager.getThumbnailImage(photoPath, size);
        Identifier texId = cachedImage.path() != null ? PhotoManager.getOrLoadTexture(cachedImage.path()) : null;
        if (texId != null) {
            int imgW = cachedImage.width();
            int imgH = cachedImage.height();

            // Calculate center-crop UVs for a square preview
            float u0, v0, u1, v1;
            if (imgW > imgH) {
                // Wider than tall: crop horizontal sides
                float cropOffset = (imgW - imgH) / (2f * imgW);
                u0 = cropOffset;
                u1 = 1f - cropOffset;
                v0 = 0f;
                v1 = 1f;
            } else if (imgH > imgW) {
                // Taller than wide: crop vertical sides
                float cropOffset = (imgH - imgW) / (2f * imgH);
                u0 = 0f;
                u1 = 1f;
                v0 = cropOffset;
                v1 = 1f - cropOffset;
            } else {
                u0 = 0f; u1 = 1f; v0 = 0f; v1 = 1f;
            }

            context.drawTexturedQuad(texId, x, y, x + size, y + size, u0, u1, v0, v1);
        } else {
            // Placeholder
            fillChamferedRect(context, x, y, size, size, s(2),
                    isLightMode() ? 0x33D8E4F0 : 0x33101825);
        }

        // Corner frame decoration (hide top-left and bottom-right corners)
        int cut = Math.max(2, s(3));
        int border = themeAccent();

        // Top-left corner mask + diagonal
        for (int i = 0; i < cut; i++) {
            context.fill(x + cut - i, y + i, x + cut - i + 1, y + i + 1, border);
        }
        context.fill(x + cut, y, x + size, y + 1, border);

        // Bottom-right corner mask + diagonal
        for (int i = 0; i < cut; i++) {
            context.fill(x + size - cut + i, y + size - 1 - i,
                    x + size - cut + i + 1, y + size - i, border);
        }
        context.fill(x, y + size - 1, x + size - cut, y + size, border);
    }

    /**
     * Render a small player head icon (for online mode uploader display).
     */
    private void renderPlayerHead(DrawContext context, String uuid, int centerX, int y) {
        int headSize = Math.max(s(7), scaledFontHeight() + s(2));
        GalleryAvatarRenderer.drawAvatarCentered(
                context,
                uuid,
                centerX,
                y,
                headSize,
                isLightMode() ? 0xFF4A90E2 : 0xFF00B4A0
        );
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) return true;

        int mouseX = (int) click.x();
        int mouseY = (int) click.y();

        // Check if clicking on a photo thumbnail
        int gridTop = getGridTop();
        int gridBottom = getGridBottom();
        if (mouseY < gridTop || mouseY > gridBottom) return false;

        int cellSize = getCellSize();
        int spacing = s(4);
        int extraH = s(14);
        int currentY = gridTop - (int) Math.round(scrollOffset);

        int count = currentMode == GalleryMode.LOCAL ? localPhotos.size() : onlinePhotos.size();

        for (int i = 0; i < count; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cellX = contentX + col * (cellSize + spacing);
            int cellY = currentY + row * (cellSize + extraH + spacing);

            if (mouseX >= cellX && mouseX <= cellX + cellSize && mouseY >= cellY && mouseY <= cellY + cellSize) {
                openImageViewer(i);
                return true;
            }
        }

        return false;
    }

    private void openImageViewer(int index) {
        if (client == null) return;

        if (currentMode == GalleryMode.LOCAL && index < localPhotos.size()) {
            PhotoManager.PhotoEntry entry = localPhotos.get(index);
            client.setScreen(new GalleryImageViewScreen(this, entry.path(), entry.metadata(), false));
        } else if (currentMode == GalleryMode.ONLINE && index < onlinePhotos.size()) {
            GalleryClient.OnlinePhotoEntry entry = onlinePhotos.get(index);
            client.setScreen(new GalleryImageViewScreen(this, entry, true));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (mx >= contentX && mx <= contentX + contentWidth && my >= getGridTop() && my <= getGridBottom()) {
            targetScroll = Math.max(0.0D, Math.min(targetScroll - verticalAmount * s(20), getMaxScroll()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void renderScrollbar(DrawContext context, int top, int bottom, int totalHeight, int currentScroll) {
        int visibleHeight = Math.max(1, bottom - top);
        if (totalHeight <= visibleHeight) return;
        int trackX = contentX + contentWidth - s(2);
        context.fill(trackX, top, trackX + 1, bottom, 0x335F7489);
        int thumbHeight = Math.max(s(18), Math.round(visibleHeight * (visibleHeight / (float) totalHeight)));
        int maxThumbTravel = Math.max(1, visibleHeight - thumbHeight);
        int thumbY = top + Math.round((currentScroll / (float) Math.max(1, totalHeight - visibleHeight)) * maxThumbTravel);
        context.fill(trackX - 1, thumbY, trackX + 2, thumbY + thumbHeight, 0xAACFE8F9);
    }
}
