package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
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
 * AR Gallery APP screen.
 * Shows photo thumbnails in a grid (6 columns for the wider AR display).
 */
public class ARGalleryScreen extends AbstractARScreen {
    private static final int COLS = 6;

    private enum GalleryMode { LOCAL, ONLINE }
    private GalleryMode currentMode = GalleryMode.LOCAL;

    private List<PhotoManager.PhotoEntry> localPhotos = new ArrayList<>();
    private List<GalleryClient.OnlinePhotoEntry> onlinePhotos = new ArrayList<>();

    private double scrollOffset;
    private double targetScroll;
    private boolean onlineGalleryEnabled = false;
    private long observedLocalRevision = -1L;

    public ARGalleryScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.gallery"), parent);
    }

    @Override
    protected void init() {
        super.init();

        addBackButton();

        onlineGalleryEnabled = GalleryClient.isOnlineGalleryEnabled();

        // Mode tabs
        int tabW = onlineGalleryEnabled ? (contentWidth - s(6)) / 2 : contentWidth;
        int tabY = contentY + s(18);
        addARGhostButton(Text.translatable("phone.tzz_mod.gallery.local"),
                contentX, tabY, tabW, s(14),
                button -> {
                    currentMode = GalleryMode.LOCAL;
                    scrollOffset = 0; targetScroll = 0;
                    refreshPhotos();
                });
        if (onlineGalleryEnabled) {
            addARGhostButton(Text.translatable("phone.tzz_mod.gallery.online"),
                    contentX + tabW + s(6), tabY, tabW, s(14),
                    button -> {
                        currentMode = GalleryMode.ONLINE;
                        scrollOffset = 0; targetScroll = 0;
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
        if (currentMode == GalleryMode.ONLINE && GalleryClient.hasNewData()) {
            onlinePhotos = GalleryClient.getOnlinePhotos();
            GalleryClient.clearNewDataFlag();
        }
    }

    private int getGridTop() { return contentY + s(36); }
    private int getGridBottom() { return contentY + contentHeight - s(4); }

    private int getCellSize() {
        return Math.max(s(10), (contentWidth - s(3) * (COLS - 1)) / COLS);
    }

    private int getTotalHeight() {
        int count = currentMode == GalleryMode.LOCAL ? localPhotos.size() : onlinePhotos.size();
        int rows = Math.max(0, (count + COLS - 1) / COLS);
        int cellSize = getCellSize();
        return rows * (cellSize + s(12) + s(3));
    }

    private int getMaxScroll() {
        return Math.max(0, getTotalHeight() - Math.max(1, getGridBottom() - getGridTop()));
    }

    private void clampScroll() {
        double max = getMaxScroll();
        targetScroll = Math.max(0, Math.min(targetScroll, max));
        scrollOffset = Math.max(0, Math.min(scrollOffset, max));
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        scrollOffset += (targetScroll - scrollOffset) * 0.35;
        clampScroll();

        // Title
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.app.gallery"),
                contentX + contentWidth / 2, contentY + s(4), themeAccent());

        int gridTop = getGridTop();
        int gridBottom = getGridBottom();
        int cellSize = getCellSize();
        int spacing = s(3);
        int extraH = Math.max(s(12), scaledFontHeight() + s(4));

        context.enableScissor(contentX, gridTop, contentX + contentWidth, gridBottom);

        int baseY = gridTop - (int) Math.round(scrollOffset);
        int count = currentMode == GalleryMode.LOCAL ? localPhotos.size() : onlinePhotos.size();

        if (count == 0) {
            Text emptyText = currentMode == GalleryMode.LOCAL
                    ? Text.translatable("phone.tzz_mod.gallery.empty")
                    : Text.translatable("phone.tzz_mod.gallery.online_empty");
            drawScaledCenteredText(context, emptyText,
                    contentX + contentWidth / 2, gridTop + s(20), themeTextDim());
        } else {
            for (int i = 0; i < count; i++) {
                int col = i % COLS;
                int row = i / COLS;
                int cellX = contentX + col * (cellSize + spacing);
                int cellY = baseY + row * (cellSize + extraH + spacing);

                if (cellY + cellSize + extraH < gridTop || cellY > gridBottom) continue;

                if (currentMode == GalleryMode.LOCAL) {
                    PhotoManager.PhotoEntry entry = localPhotos.get(i);
                    renderThumbnail(context, entry.path(), cellX, cellY, cellSize);
                    String timeStr = PhotoManager.formatCaptureTime(entry.metadata().captureTimeMs());
                    drawScaledCenteredText(context, Text.literal(timeStr),
                            cellX + cellSize / 2, cellY + cellSize + s(2), themeTextDim());
                } else {
                    GalleryClient.OnlinePhotoEntry entry = onlinePhotos.get(i);
                    if (entry.thumbnailPath() != null) {
                        renderThumbnail(context, entry.thumbnailPath(), cellX, cellY, cellSize);
                    } else {
                        fillChamferedRect(context, cellX, cellY, cellSize, cellSize, s(2),
                                isLightMode() ? 0x33D8E4F0 : 0x33101825);
                        drawScaledCenteredText(context, Text.literal("..."),
                                cellX + cellSize / 2, cellY + cellSize / 2, themeTextDim());
                    }
                    renderPlayerHead(context, entry.uploaderUuid(), cellX + cellSize / 2, cellY + cellSize + s(2));
                }
            }
        }

        context.disableScissor();

        // Scrollbar
        int totalH = getTotalHeight();
        int visH = Math.max(1, gridBottom - gridTop);
        if (totalH > visH) {
            int trackX = contentX + contentWidth - s(2);
            context.fill(trackX, gridTop, trackX + 1, gridBottom, 0x335F7489);
            int thumbH = Math.max(s(12), Math.round(visH * (visH / (float) totalH)));
            int maxTravel = Math.max(1, visH - thumbH);
            int thumbY = gridTop + Math.round(((float) scrollOffset / Math.max(1, totalH - visH)) * maxTravel);
            context.fill(trackX - 1, thumbY, trackX + 2, thumbY + thumbH, 0xAACFE8F9);
        }
    }

    private void renderThumbnail(DrawContext context, Path path, int x, int y, int size) {
        PhotoManager.CachedImage cachedImage = PhotoManager.getThumbnailImage(path, size);
        Identifier texId = cachedImage.path() != null ? PhotoManager.getOrLoadTexture(cachedImage.path()) : null;
        if (texId != null) {
            int[] dims = {cachedImage.width(), cachedImage.height()};
            float u0, v0, u1, v1;
            if (dims[0] > dims[1]) {
                float crop = (dims[0] - dims[1]) / (2f * dims[0]);
                u0 = crop; u1 = 1f - crop; v0 = 0; v1 = 1;
            } else if (dims[1] > dims[0]) {
                float crop = (dims[1] - dims[0]) / (2f * dims[1]);
                u0 = 0; u1 = 1; v0 = crop; v1 = 1f - crop;
            } else {
                u0 = 0; u1 = 1; v0 = 0; v1 = 1;
            }
            context.drawTexturedQuad(texId, x, y, x + size, y + size, u0, u1, v0, v1);
        } else {
            fillChamferedRect(context, x, y, size, size, s(2),
                    isLightMode() ? 0x33D8E4F0 : 0x33101825);
        }

        // Corner frame
        int cut = Math.max(2, s(2));
        int border = themeAccent();
        for (int i = 0; i < cut; i++) {
            context.fill(x + cut - i, y + i, x + cut - i + 1, y + i + 1, border);
        }
        context.fill(x + cut, y, x + size, y + 1, border);
        for (int i = 0; i < cut; i++) {
            context.fill(x + size - cut + i, y + size - 1 - i, x + size - cut + i + 1, y + size - i, border);
        }
        context.fill(x, y + size - 1, x + size - cut, y + size, border);
    }

    private void renderPlayerHead(DrawContext context, String uuid, int centerX, int y) {
        int headSize = Math.max(s(6), scaledFontHeight() + s(1));
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

        int mx = (int) click.x();
        int my = (int) click.y();
        if (my < getGridTop() || my > getGridBottom()) return false;

        int cellSize = getCellSize();
        int spacing = s(3);
        int extraH = s(12);
        int baseY = getGridTop() - (int) Math.round(scrollOffset);
        int count = currentMode == GalleryMode.LOCAL ? localPhotos.size() : onlinePhotos.size();

        for (int i = 0; i < count; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cellX = contentX + col * (cellSize + spacing);
            int cellY = baseY + row * (cellSize + extraH + spacing);
            if (mx >= cellX && mx <= cellX + cellSize && my >= cellY && my <= cellY + cellSize) {
                openViewer(i);
                return true;
            }
        }
        return false;
    }

    private void openViewer(int index) {
        if (client == null) return;
        if (currentMode == GalleryMode.LOCAL && index < localPhotos.size()) {
            PhotoManager.PhotoEntry entry = localPhotos.get(index);
            client.setScreen(new ARGalleryImageViewScreen(this, entry.path(), entry.metadata(), false));
        } else if (currentMode == GalleryMode.ONLINE && index < onlinePhotos.size()) {
            GalleryClient.OnlinePhotoEntry entry = onlinePhotos.get(index);
            client.setScreen(new ARGalleryImageViewScreen(this, entry, true));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        if (isHelpModeActive()) {
            return true;
        }
        int mx = (int) mouseX, my = (int) mouseY;
        if (mx >= contentX && mx <= contentX + contentWidth && my >= getGridTop() && my <= getGridBottom()) {
            targetScroll = Math.max(0, Math.min(targetScroll - vAmt * s(16), getMaxScroll()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, hAmt, vAmt);
    }

    @Override
    protected Text getCustomHelpTooltip(int mouseX, int mouseY) {
        if (mouseY < getGridTop() || mouseY > getGridBottom()) {
            return Text.empty();
        }
        int cellSize = getCellSize();
        int spacing = s(3);
        int extraH = s(12);
        int baseY = getGridTop() - (int) Math.round(scrollOffset);
        int count = currentMode == GalleryMode.LOCAL ? localPhotos.size() : onlinePhotos.size();
        for (int index = 0; index < count; index++) {
            int col = index % COLS;
            int row = index / COLS;
            int cellX = contentX + col * (cellSize + spacing);
            int cellY = baseY + row * (cellSize + extraH + spacing);
            if (mouseX >= cellX && mouseX <= cellX + cellSize && mouseY >= cellY && mouseY <= cellY + cellSize) {
                return Text.translatable("phone.tzz_mod.help.gallery_thumbnail");
            }
        }
        return Text.empty();
    }

}
