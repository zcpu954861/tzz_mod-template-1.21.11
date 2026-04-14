package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.map.MapCanvasRenderer;
import com.zcpu.tzzmod.client.map.MapClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * AR-specific map screen. Renders map canvas inside the AR panel.
 */
public class ARMapScreen extends AbstractARScreen {
    private MapClient.MapState state;
    private int refreshTicks;

    public ARMapScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.map"), parent);
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();
        state = MapClient.getState();
        MapClient.requestSnapshot();
        refreshTicks = 0;
    }

    @Override
    public void tick() {
        super.tick();
        refreshTicks++;
        if (refreshTicks % 30 == 0) {
            state = MapClient.getState();
            MapClient.requestSnapshot();
        }
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // Title
        Text titleText = Text.translatable("phone.tzz_mod.app.map");
        int titleCX = contentX + contentWidth / 2;
        int titleY = contentY + s(2);
        drawScaledCenteredText(context, titleText, titleCX, titleY, themeAccent());

        int mapTop = titleY + scaledFontHeight() + s(4);
        int mapH = Math.max(1, contentY + contentHeight - mapTop);

        if (state == null || !state.hasRegion()) {
            Text msg = state == null
                    ? Text.translatable("phone.tzz_mod.map.loading")
                    : Text.translatable("phone.tzz_mod.map.not_configured");
            drawScaledCenteredText(context, msg, titleCX, mapTop + mapH / 2, themeTextDim());
            return;
        }

        MapCanvasRenderer.RenderResult result = MapCanvasRenderer.render(
                context, state, contentX, mapTop, contentWidth, mapH, mouseX, mouseY);

        // Corner masking (#2): top-left and bottom-right corners masked with angular border
        renderMapCornerMask(context, contentX, mapTop, contentWidth, mapH);

        if (result.hoveredMarkerName() != null && !result.hoveredMarkerName().isEmpty()) {
            int tipW = scaledTextWidth(result.hoveredMarkerName()) + s(6);
            int tipH = scaledFontHeight() + s(4);
            int tipX = mouseX + s(8);
            int tipY = mouseY - tipH - s(2);
            int cut = Math.max(1, s(2));
            drawAngularTechFrame(context, tipX, tipY, tipW, tipH, cut,
                    isLightMode() ? 0xDD_E0E8F0 : 0xDD_081018,
                    isLightMode() ? 0x88_A0B8CC : 0x88_00C8BE);
            drawScaledText(context, Text.literal(result.hoveredMarkerName()), tipX + s(3), tipY + s(2), themeText());
        }
    }

    /** Masks top-left and bottom-right map corners with angular border lines (#2). */
    private void renderMapCornerMask(DrawContext context, int mx, int my, int mw, int mh) {
        int cut = Math.min(s(14), Math.min(mw, mh) / 4);
        int bg = themeBg();
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
}
