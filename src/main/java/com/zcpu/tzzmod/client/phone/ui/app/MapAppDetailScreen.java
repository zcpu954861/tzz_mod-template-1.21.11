package com.zcpu.tzzmod.client.phone.ui.app;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.zcpu.tzzmod.client.map.MapCanvasRenderer;
import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public class MapAppDetailScreen extends AbstractPhoneScreen {
    private int ticksUntilRefresh;

    public MapAppDetailScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.map.detail"), parent);
    }

    @Override
    protected void init() {
        super.init();
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(72), s(20), button -> close());
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
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.map.detail"), contentX + contentWidth / 2, contentY + s(8));

        int mapX = contentX + s(8);
        int mapY = contentY + s(26);
        int mapWidth = contentWidth - s(16);
        int mapHeight = contentHeight - s(92);

        MapCanvasRenderer.RenderResult result = MapCanvasRenderer.render(context, MapClient.getState(), mapX, mapY, mapWidth, mapHeight, mouseX, mouseY);
        if (!result.rendered()) {
            Text message = MapClient.getState().hasRegion()
                    ? Text.translatable("phone.tzz_mod.map.loading")
                    : Text.translatable("phone.tzz_mod.map.not_configured");
            context.drawCenteredTextWithShadow(textRenderer, message, mapX + mapWidth / 2, mapY + mapHeight / 2 - s(4), 0xFFE0E0E0);
        } else if (!result.hoveredMarkerName().isBlank()) {
            renderTooltip(context, mouseX, mouseY, result.hoveredMarkerName());
        }
        // Corner masking (#2)
        renderMapCornerMask(context, mapX, mapY, mapWidth, mapHeight);

        MapClient.MapRegion region = MapClient.getState().region();
        if (region != null) {
            context.drawTextWithShadow(textRenderer, Text.literal("X: " + region.minX() + " -> " + region.maxX()), contentX + s(8), contentY + contentHeight - s(62), 0xFFB7C7D8);
            context.drawTextWithShadow(textRenderer, Text.literal("Y: " + region.minY() + " -> " + region.maxY()), contentX + s(8), contentY + contentHeight - s(50), 0xFFB7C7D8);
            context.drawTextWithShadow(textRenderer, Text.literal("Z: " + region.minZ() + " -> " + region.maxZ()), contentX + s(8), contentY + contentHeight - s(38), 0xFFB7C7D8);
        }
    }

    /** Masks the top-left and bottom-right corners of the map area (#2). */
    private void renderMapCornerMask(DrawContext context, int mx, int my, int mw, int mh) {
        int cut = Math.min(s(16), Math.min(mw, mh) / 4);
        int bg = themeBgDark();
        int border = themeBorder();
        for (int i = 0; i < cut; i++) {
            context.fill(mx, my + i, mx + (cut - i), my + i + 1, bg);
        }
        for (int i = 0; i < cut; i++) {
            context.fill(mx + mw - (cut - i), my + mh - 1 - i, mx + mw, my + mh - i, bg);
        }
        context.fill(mx + cut, my, mx + mw, my + 1, border);
        for (int i = 0; i < cut; i++) {
            context.fill(mx + cut - i, my + i, mx + cut - i + 1, my + i + 1, border);
        }
        context.fill(mx, my + mh - 1, mx + mw - cut, my + mh, border);
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
