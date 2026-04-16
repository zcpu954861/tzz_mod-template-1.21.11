package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import com.zcpu.tzzmod.client.photo.PhotoManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;

public class GalleryZoomScreen extends Screen {
    private final Screen parent;
    private final Path photoPath;
    private Identifier textureId;
    private int imgW = 1;
    private int imgH = 1;

    public GalleryZoomScreen(Screen parent, Path photoPath) {
        super(Text.empty());
        this.parent = parent;
        this.photoPath = photoPath;
    }

    @Override
    protected void init() {
        super.init();
        if (photoPath == null || !java.nio.file.Files.exists(photoPath)) {
            return;
        }
        PhotoManager.CachedImage cachedImage = PhotoManager.getViewerImage(
                photoPath,
                Math.max(1, width - 48),
                Math.max(1, height - 48)
        );
        imgW = cachedImage.width();
        imgH = cachedImage.height();
        textureId = cachedImage.path() != null ? PhotoManager.getOrLoadTexture(cachedImage.path()) : null;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (textureId == null) {
            return;
        }

        int maxWidth = Math.max(1, width - 48);
        int maxHeight = Math.max(1, height - 48);
        float imageAspect = (float) imgW / imgH;
        float areaAspect = (float) maxWidth / maxHeight;

        int drawWidth;
        int drawHeight;
        if (imageAspect > areaAspect) {
            drawWidth = maxWidth;
            drawHeight = Math.max(1, Math.round(maxWidth / imageAspect));
        } else {
            drawHeight = maxHeight;
            drawWidth = Math.max(1, Math.round(maxHeight * imageAspect));
        }

        int drawX = (width - drawWidth) / 2;
        int drawY = (height - drawHeight) / 2;
        context.drawTexturedQuad(textureId, drawX, drawY, drawX + drawWidth, drawY + drawHeight, 0f, 1f, 0f, 1f);
        drawTechPhotoFrame(context, drawX, drawY, drawWidth, drawHeight);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEscape()) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void drawTechPhotoFrame(DrawContext context, int x, int y, int w, int h) {
        int lineLen = Math.min(w, h) / 4;
        int cut = 3;
        int color = PhoneSettingsClient.isLightModeEnabled() ? 0xFF0099CC : 0xFF00FFE0;
        int pad = 3;
        int fx = x - pad;
        int fy = y - pad;
        int fw = w + pad * 2;
        int fh = h + pad * 2;

        context.fill(fx + cut, fy, fx + lineLen, fy + 1, color);
        context.fill(fx, fy + cut, fx + 1, fy + lineLen, color);
        for (int i = 0; i < cut; i++) {
            context.fill(fx + cut - i, fy + i, fx + cut - i + 1, fy + i + 1, color);
        }

        context.fill(fx + fw - lineLen, fy, fx + fw, fy + 1, color);
        context.fill(fx, fy + fh - lineLen, fx + 1, fy + fh, color);

        context.fill(fx + fw - lineLen, fy + fh - 1, fx + fw - cut, fy + fh, color);
        context.fill(fx + fw - 1, fy + fh - lineLen, fx + fw, fy + fh - cut, color);
        for (int i = 0; i < cut; i++) {
            context.fill(fx + fw - cut + i - 1, fy + fh - 1 - i, fx + fw - cut + i, fy + fh - i, color);
        }
    }
}