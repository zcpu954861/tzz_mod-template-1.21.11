package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.map.MapCanvasRenderer;
import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class MapAppScreen extends AbstractPhoneScreen {
    private int ticksUntilRefresh;

    public MapAppScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.map"), parent);
    }

    @Override
    protected void init() {
        super.init();
        int buttonY = contentY + contentHeight - s(24);
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, buttonY, s(72), s(20), button -> close());
        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.open_subpage"), contentX + contentWidth - s(120), buttonY, s(120), s(20), button -> client.setScreen(new MapAppDetailScreen(this)));
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
    }

    private void renderTooltip(DrawContext context, int mouseX, int mouseY, String text) {
        int tooltipX = mouseX + s(6);
        int tooltipY = mouseY - s(14);
        int width = textRenderer.getWidth(text) + s(8);
        int height = textRenderer.fontHeight + s(6);
        context.fill(tooltipX, tooltipY, tooltipX + width, tooltipY + height, 0xCC0D1117);
        context.drawTextWithShadow(textRenderer, Text.literal(text), tooltipX + s(4), tooltipY + s(3), 0xFFFFFFFF);
    }
}
