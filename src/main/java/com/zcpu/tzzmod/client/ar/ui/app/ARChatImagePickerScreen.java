package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.phone.chat.ChatUiUtil;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.photo.PhotoManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ARChatImagePickerScreen extends AbstractARScreen {
    private static final int COLUMN_COUNT = 5;

    private final String groupId;
    private final List<PhotoManager.PhotoEntry> localPhotos = new ArrayList<>();
    private ButtonWidget sendButton;
    private Path selectedPhotoPath;
    private boolean uploading;
    private float uploadProgress;
    private int scrollOffset;
    private long observedLocalRevision = -1L;

    public ARChatImagePickerScreen(net.minecraft.client.gui.screen.Screen parent, String groupId) {
        super(Text.translatable("phone.tzz_mod.chat.image_picker"), parent);
        this.groupId = groupId;
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();
        sendButton = addARPrimaryButton(Text.translatable("phone.tzz_mod.chat.send_image"), contentX + contentWidth - s(66), contentY + contentHeight - s(20), s(66), s(16), button -> sendSelectedPhoto());
        refreshPhotos();
    }

    @Override
    public void tick() {
        super.tick();
        if (observedLocalRevision != PhotoManager.getLocalPhotoRevision()) {
            refreshPhotos();
        }
        if (sendButton != null) {
            sendButton.active = selectedPhotoPath != null && !uploading;
        }
    }

    private void refreshPhotos() {
        localPhotos.clear();
        localPhotos.addAll(PhotoManager.loadPhotos());
        observedLocalRevision = PhotoManager.getLocalPhotoRevision();
        if (selectedPhotoPath != null && localPhotos.stream().noneMatch(entry -> entry.path().equals(selectedPhotoPath))) {
            selectedPhotoPath = null;
        }
        clampScroll();
    }

    private void sendSelectedPhoto() {
        if (selectedPhotoPath == null || uploading) {
            return;
        }
        uploading = true;
        uploadProgress = 0.0F;
        PhoneChatClient.sendGroupImage(groupId, selectedPhotoPath, progress -> uploadProgress = progress, success -> {
            uploading = false;
            if (success && client != null) {
                client.setScreen(parent);
            }
        });
    }

    private int getGridTop() {
        return contentY + s(24);
    }

    private int getGridBottom() {
        return contentY + contentHeight - s(26);
    }

    private int getCellSize() {
        return Math.max(s(16), (contentWidth - s(4) * (COLUMN_COUNT - 1)) / COLUMN_COUNT);
    }

    private int getGridHeight() {
        int rows = (localPhotos.size() + COLUMN_COUNT - 1) / COLUMN_COUNT;
        if (rows <= 0) {
            return 0;
        }
        return rows * (getCellSize() + s(10));
    }

    private int getMaxScroll() {
        return Math.max(0, getGridHeight() - Math.max(1, getGridBottom() - getGridTop()));
    }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, getMaxScroll()));
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.image_picker"), contentX + contentWidth / 2, contentY + s(6), themeText());
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.image_select_hint"), contentX + contentWidth / 2, contentY + s(16), themeTextDim());

        int top = getGridTop();
        int bottom = getGridBottom();
        int cellSize = getCellSize();
        int rowStride = cellSize + s(10);
        int colStride = cellSize + s(4);
        int baseY = top - scrollOffset;

        context.enableScissor(contentX, top, contentX + contentWidth, bottom);
        if (localPhotos.isEmpty()) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.gallery.empty"), contentX + contentWidth / 2, top + s(22), themeTextDim());
        }

        for (int index = 0; index < localPhotos.size(); index++) {
            PhotoManager.PhotoEntry entry = localPhotos.get(index);
            int col = index % COLUMN_COUNT;
            int row = index / COLUMN_COUNT;
            int cellX = contentX + col * colStride;
            int cellY = baseY + row * rowStride;
            if (cellY + cellSize < top || cellY > bottom) {
                continue;
            }

            renderSquareThumbnail(context, entry.path(), cellX, cellY, cellSize);
            if (entry.path().equals(selectedPhotoPath)) {
                ChatUiUtil.drawAngularFrame(context, cellX - 1, cellY - 1, cellSize + 2, cellSize + 2, s(3), 0x00000000, themeAccent());
                ChatUiUtil.drawSelectionBadge(context, textRenderer, cellX + cellSize, cellY + cellSize, s(10), themeAccent(), 0xFFFFFFFF);
            }
        }
        context.disableScissor();

        if (uploading) {
            int barX = contentX + s(8);
            int barY = contentY + contentHeight - s(28);
            int barW = contentWidth - s(16);
            int barH = s(5);
            ChatUiUtil.drawAngularFrame(context, barX, barY, barW, barH, s(2), 0x22091420, themeBorder());
            int fillW = Math.max(0, Math.min(barW, Math.round(barW * uploadProgress)));
            if (fillW > 0) {
                ChatUiUtil.drawAngularFrame(context, barX, barY, fillW, barH, s(2), themeAccent(), themeAccent());
            }
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.image_uploading"), contentX + contentWidth / 2, barY - s(10), themeTextDim());
        }

        renderScrollbar(context, top, bottom, Math.max(bottom - top, getGridHeight()), scrollOffset);
    }

    private void renderSquareThumbnail(DrawContext context, Path photoPath, int x, int y, int size) {
        PhotoManager.CachedImage cachedImage = PhotoManager.getThumbnailImage(photoPath, size);
        Identifier textureId = cachedImage.path() != null ? PhotoManager.getOrLoadTexture(cachedImage.path()) : null;
        if (textureId != null) {
            float u0 = 0.0F;
            float v0 = 0.0F;
            float u1 = 1.0F;
            float v1 = 1.0F;
            if (cachedImage.width() > cachedImage.height()) {
                float crop = (cachedImage.width() - cachedImage.height()) / (2.0F * cachedImage.width());
                u0 = crop;
                u1 = 1.0F - crop;
            } else if (cachedImage.height() > cachedImage.width()) {
                float crop = (cachedImage.height() - cachedImage.width()) / (2.0F * cachedImage.height());
                v0 = crop;
                v1 = 1.0F - crop;
            }
            context.drawTexturedQuad(textureId, x, y, x + size, y + size, u0, u1, v0, v1);
        } else {
            ChatUiUtil.drawAngularFrame(context, x, y, size, size, s(3), 0x22091420, themeBorder());
            drawScaledCenteredText(context, Text.literal("..."), x + size / 2, y + size / 2 - scaledFontHeight() / 2, themeTextDim());
        }
        ChatUiUtil.drawAngularFrame(context, x, y, size, size, s(3), 0x00000000, themeBorder());
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        if (uploading) {
            return false;
        }

        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        if (mouseX < contentX || mouseX > contentX + contentWidth || mouseY < getGridTop() || mouseY > getGridBottom()) {
            return false;
        }

        int cellSize = getCellSize();
        int rowStride = cellSize + s(10);
        int colStride = cellSize + s(4);
        int baseY = getGridTop() - scrollOffset;
        for (int index = 0; index < localPhotos.size(); index++) {
            int col = index % COLUMN_COUNT;
            int row = index / COLUMN_COUNT;
            int cellX = contentX + col * colStride;
            int cellY = baseY + row * rowStride;
            if (mouseX >= cellX && mouseX <= cellX + cellSize && mouseY >= cellY && mouseY <= cellY + cellSize) {
                selectedPhotoPath = localPhotos.get(index).path();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= getGridTop() && mouseY <= getGridBottom()) {
            scrollOffset -= (int) Math.round(verticalAmount * s(18));
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}