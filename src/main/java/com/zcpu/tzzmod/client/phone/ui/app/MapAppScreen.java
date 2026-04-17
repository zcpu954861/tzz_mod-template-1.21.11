package com.zcpu.tzzmod.client.phone.ui.app;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.zcpu.tzzmod.client.map.MapCanvasRenderer;
import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public class MapAppScreen extends AbstractPhoneScreen {
    private int ticksUntilRefresh;
    private boolean zoomMode = false;

    public MapAppScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.map"), parent);
    }

    @Override
    protected void init() {
        super.init();
        int buttonY = contentY + contentHeight - s(24);
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, buttonY, s(72), s(20), button -> {
            if (zoomMode) { zoomMode = false; } else { close(); }
        });
        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.map.zoom"), contentX + contentWidth - s(120), buttonY, s(120), s(20), button -> zoomMode = true);
        MapClient.requestState();
        MapClient.requestSnapshot();
        ticksUntilRefresh = 30;
    }

    @Override
    public void tick() {
        super.tick();
        ticksUntilRefresh--;
        if (ticksUntilRefresh <= 0) {
            MapClient.requestState();
            ticksUntilRefresh = 30;
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (isHelpModeActive()) {
            return super.keyPressed(input);
        }
        if (zoomMode && input.key() == 256) { // ESC
            zoomMode = false;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (zoomMode) {
            renderZoomOverlay(context, mouseX, mouseY, delta);
        } else {
            super.render(context, mouseX, mouseY, delta);
        }
    }

    private void renderZoomOverlay(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dim the background
        context.fill(0, 0, width, height, 0xCC000000);

        // Centered map area (80% of screen, capped to not overflow)
        int padding = Math.max(s(16), (int)(Math.min(width, height) * 0.08f));
        int overlayW = Math.min(width - padding * 2, (int)(width * 0.88f));
        int overlayH = Math.min(height - padding * 2, (int)(height * 0.88f));
        int overlayX = (width - overlayW) / 2;
        int overlayY = (height - overlayH) / 2;

        // Background fill
        context.fill(overlayX, overlayY, overlayX + overlayW, overlayY + overlayH,
                isLightMode() ? 0xEEE8EDF4 : 0xEE0A0F1A);

        // Corner masking with angular border
        int cut = Math.min(s(20), Math.min(overlayW, overlayH) / 4);
        int bg = isLightMode() ? 0xEEE8EDF4 : 0xEE0A0F1A;
        int accent = themeAccent();

        // Map render area (inset by 1 for border)
        int mapX = overlayX + 1;
        int mapY = overlayY + 1;
        int mapW = overlayW - 2;
        int mapH = overlayH - 2;

        MapCanvasRenderer.RenderResult result = MapCanvasRenderer.render(
                context, MapClient.getState(), mapX, mapY, mapW, mapH, mouseX, mouseY);
        if (!result.rendered()) {
            Text message = MapClient.getState().hasRegion()
                    ? Text.translatable("phone.tzz_mod.map.loading")
                    : Text.translatable("phone.tzz_mod.map.not_configured");
            context.drawCenteredTextWithShadow(textRenderer, message,
                    mapX + mapW / 2, mapY + mapH / 2, 0xFFE0E0E0);
        } else if (!result.hoveredMarkerName().isBlank()) {
            renderTooltip(context, mouseX, mouseY, result.hoveredMarkerName());
        }

        // Corner mask (#2 style)
        for (int i = 0; i < cut; i++) {
            context.fill(mapX, mapY + i, mapX + (cut - i), mapY + i + 1, bg);
        }
        for (int i = 0; i < cut; i++) {
            context.fill(mapX + mapW - (cut - i), mapY + mapH - 1 - i, mapX + mapW, mapY + mapH - i, bg);
        }
        // Angular border lines
        context.fill(mapX + cut, mapY, mapX + mapW, mapY + 1, accent);
        for (int i = 0; i < cut; i++) {
            context.fill(mapX + cut - i, mapY + i, mapX + cut - i + 1, mapY + i + 1, accent);
        }
        context.fill(mapX, mapY + mapH - 1, mapX + mapW - cut, mapY + mapH, accent);
        for (int i = 0; i < cut; i++) {
            context.fill(mapX + mapW - cut + i, mapY + mapH - 1 - i, mapX + mapW - cut + i + 1, mapY + mapH - i, accent);
        }

        // Close hint
        String hint = "[ESC / " + Text.translatable("phone.tzz_mod.back").getString() + "]";
        int hintW = textRenderer.getWidth(hint);
        context.drawTextWithShadow(textRenderer, Text.literal(hint),
                overlayX + overlayW - hintW - s(6), overlayY + overlayH + s(4), 0x88FFFFFF);
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.map"), contentX + contentWidth / 2, contentY + s(8));

        int mapX = contentX + s(8);
        int mapY = contentY + s(26);
        int mapWidth = contentWidth - s(16);
        int mapHeight = contentHeight - s(58);

        MapCanvasRenderer.RenderResult result = MapCanvasRenderer.render(context, MapClient.getState(), mapX, mapY, mapWidth, mapHeight, mouseX, mouseY);
        if (!result.rendered()) {
            Text message = MapClient.getState().hasRegion()
                    ? Text.translatable("phone.tzz_mod.map.loading")
                    : Text.translatable("phone.tzz_mod.map.not_configured");
            context.drawCenteredTextWithShadow(textRenderer, message, mapX + mapWidth / 2, mapY + mapHeight / 2 - s(4), 0xFFE0E0E0);
        } else if (!result.hoveredMarkerName().isBlank()) {
            renderTooltip(context, mouseX, mouseY, result.hoveredMarkerName());
        }
        // Corner masking: fill top-left and bottom-right corners, then draw 4-line angular border (#2)
        renderMapCornerMask(context, mapX, mapY, mapWidth, mapHeight);
    }

    /** Masks the top-left and bottom-right corners of the map area and overlays 4-line angular border (#2). */
    private void renderMapCornerMask(DrawContext context, int mx, int my, int mw, int mh) {
        int cut = Math.min(s(16), Math.min(mw, mh) / 4);
        int bg = themeBgDark();
        int border = themeBorder();
        // top-left mask triangle (fill column by column)
        for (int i = 0; i < cut; i++) {
            context.fill(mx, my + i, mx + (cut - i), my + i + 1, bg);
        }
        // bottom-right mask triangle
        for (int i = 0; i < cut; i++) {
            context.fill(mx + mw - (cut - i), my + mh - 1 - i, mx + mw, my + mh - i, bg);
        }
        // 4-line angular border over map area
        // top edge (left part, from after cut)
        context.fill(mx + cut, my, mx + mw, my + 1, border);
        // top-left diagonal
        for (int i = 0; i < cut; i++) {
            context.fill(mx + cut - i, my + i, mx + cut - i + 1, my + i + 1, border);
        }
        // bottom edge (right part, from 0 to before cut)
        context.fill(mx, my + mh - 1, mx + mw - cut, my + mh, border);
        // bottom-right diagonal
        for (int i = 0; i < cut; i++) {
            context.fill(mx + mw - cut + i, my + mh - 1 - i, mx + mw - cut + i + 1, my + mh - i, border);
        }
    }

    private void renderTooltip(DrawContext context, int mouseX, int mouseY, String text) {
        Text displayText = tryParseJsonText(text);
        int tooltipX = mouseX + s(6);
        int tooltipY = mouseY - s(14);
        int width = textRenderer.getWidth(displayText) + s(8);
        int height = textRenderer.fontHeight + s(6);
        context.fill(tooltipX, tooltipY, tooltipX + width, tooltipY + height, 0xCC0D1117);
        context.drawTextWithShadow(textRenderer, displayText, tooltipX + s(4), tooltipY + s(3), 0xFFFFFFFF);
    }

    private static Text tryParseJsonText(String raw) {
        if (raw == null || raw.isBlank()) return Text.literal(raw == null ? "" : raw);
        if (!raw.startsWith("{") && !raw.startsWith("[") && !raw.startsWith("\"")) {
            return Text.literal(raw);
        }
        try {
            var element = JsonParser.parseString(raw);
            var result = TextCodecs.CODEC.parse(JsonOps.INSTANCE, element);
            Text parsed = result.result().orElse(null);
            if (parsed != null) return parsed;
        } catch (Exception ignored) {}
        return Text.literal(raw);
    }
}
