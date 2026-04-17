package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.chat.ChatUiUtil;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.phone.ui.PhoneButtonWidget;
import com.zcpu.tzzmod.client.photo.PhotoManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PhoneChatImagePickerScreen extends AbstractPhoneScreen {
    private static final int COLUMN_COUNT = 4;

    private final String groupId;
    private final List<PhotoManager.PhotoEntry> localPhotos = new ArrayList<>();
    private ButtonWidget sendButton;
    private Path selectedPhotoPath;
    private boolean uploading;
    private float uploadProgress;
    private int scrollOffset;
    private long observedLocalRevision = -1L;

    public PhoneChatImagePickerScreen(Screen parent, String groupId) {
        super(Text.translatable("phone.tzz_mod.chat.image_picker"), parent);
        this.groupId = groupId;
    }

    @Override
    protected void init() {
        super.init();

        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(72), s(20), button -> close());
        sendButton = addPhoneButton(
                Text.translatable("phone.tzz_mod.chat.send_image"),
                contentX + contentWidth - s(86),
                contentY + contentHeight - s(24),
                s(86),
                s(20),
                PhoneButtonWidget.Variant.PRIMARY,
                () -> false,
                button -> sendSelectedPhoto()
        );

        refreshPhotos();
        clampScroll();
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
        PhotoManager.ensurePhotosDir();
        localPhotos.clear();
        localPhotos.addAll(PhotoManager.loadPhotos());
        observedLocalRevision = PhotoManager.getLocalPhotoRevision();
        if (selectedPhotoPath != null && localPhotos.stream().noneMatch(entry -> entry.path().equals(selectedPhotoPath))) {
            selectedPhotoPath = null;
        }
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
        return contentY + s(34);
    }

    private int getGridBottom() {
        return contentY + contentHeight - s(34);
    }

    private int getCellSize() {
        return Math.max(s(18), (contentWidth - s(4) * (COLUMN_COUNT - 1)) / COLUMN_COUNT);
    }

    private int getGridHeight() {
        int rows = (localPhotos.size() + COLUMN_COUNT - 1) / COLUMN_COUNT;
        if (rows <= 0) {
            return 0;
        }
        return rows * (getCellSize() + s(14));
    }

    private int getMaxScroll() {
        return Math.max(0, getGridHeight() - Math.max(1, getGridBottom() - getGridTop()));
    }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, getMaxScroll()));
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.chat.image_picker"), contentX + contentWidth / 2, contentY + s(8));
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.image_select_hint"), contentX + contentWidth / 2, contentY + s(22), themeTextDim());

        int top = getGridTop();
        int bottom = getGridBottom();
        int cellSize = getCellSize();
        int rowStride = cellSize + s(14);
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
            boolean selected = entry.path().equals(selectedPhotoPath);
            if (selected) {
                ChatUiUtil.drawAngularFrame(context, cellX - s(1), cellY - s(1), cellSize + s(2), cellSize + s(2), s(4), 0x00000000, themeAccent());
                ChatUiUtil.drawSelectionBadge(context, textRenderer, cellX + cellSize, cellY + cellSize, s(12), themeAccent(), 0xFFFFFFFF);
            }

            String timeText = PhotoManager.formatCaptureTime(entry.metadata().captureTimeMs());
            drawScaledCenteredText(context, Text.literal(timeText), cellX + cellSize / 2, cellY + cellSize + s(2), themeTextDim());
        }
        context.disableScissor();

        if (uploading) {
            drawUploadProgress(context);
        }
        renderPhoneScrollbar(context, top, bottom, Math.max(bottom - top, getGridHeight()), scrollOffset);
    }

    private void drawUploadProgress(DrawContext context) {
        int barX = contentX + s(8);
        int barY = contentY + contentHeight - s(42);
        int barW = contentWidth - s(16);
        int barH = s(6);
        ChatUiUtil.drawAngularFrame(context, barX, barY, barW, barH, s(2), isLightMode() ? 0x44D8E4F0 : 0x330A1622, themeBorder());
        int fillW = Math.max(0, Math.min(barW, Math.round(barW * uploadProgress)));
        if (fillW > 0) {
            ChatUiUtil.drawAngularFrame(context, barX, barY, fillW, barH, s(2), themeAccent(), themeAccent());
        }
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.image_uploading"), contentX + contentWidth / 2, barY - s(10), themeTextDim());
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
            ChatUiUtil.drawAngularFrame(context, x, y, size, size, s(4), isLightMode() ? 0x44D8E4F0 : 0x220A1622, themeBorder());
            drawScaledCenteredText(context, Text.literal("..."), x + size / 2, y + size / 2 - scaledFontHeight() / 2, themeTextDim());
        }
        ChatUiUtil.drawAngularFrame(context, x, y, size, size, s(4), 0x00000000, themeBorder());
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }

        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        int top = getGridTop();
        int bottom = getGridBottom();
        if (mouseX < contentX || mouseX > contentX + contentWidth || mouseY < top || mouseY > bottom || uploading) {
            return false;
        }

        int cellSize = getCellSize();
        int rowStride = cellSize + s(14);
        int colStride = cellSize + s(4);
        int baseY = top - scrollOffset;
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
        if (isHelpModeActive()) {
            return true;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (mx >= contentX && mx <= contentX + contentWidth && my >= getGridTop() && my <= getGridBottom()) {
            scrollOffset -= (int) Math.round(verticalAmount * s(18));
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}