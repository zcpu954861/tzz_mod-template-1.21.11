package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MapAppScreen extends AbstractPhoneScreen {
    private static final Identifier MAP_TEXTURE = Identifier.of(Tzz_mod.MOD_ID, "textures/gui/phone/apps/map/map.png");

    public MapAppScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.map"), parent);
    }

    @Override
    protected void init() {
        super.init();

        int buttonY = contentY + contentHeight - s(24);
        addDrawableChild(ButtonWidget.builder(Text.translatable("phone.tzz_mod.back"), button -> close())
                .dimensions(contentX, buttonY, s(72), s(20))
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("phone.tzz_mod.open_subpage"),
                        button -> client.setScreen(new MapAppDetailScreen(this, MAP_TEXTURE)))
                .dimensions(contentX + contentWidth - s(120), buttonY, s(120), s(20))
                .build());
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // 使用不随缩放的文本绘制标题
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.map"), contentX + contentWidth / 2, contentY + s(8));

        int mapX = contentX + s(10);
        int mapY = contentY + s(28);
        int mapWidth = contentWidth - s(20);
        int mapHeight = contentHeight - s(62);
        // Removed texture drawing: render only text fallback/placeholder

        if (hasResource(MAP_TEXTURE)) {
            // Resource exists but we intentionally avoid drawing textures; show a notice instead
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("phone.tzz_mod.map.available_text"),
                    mapX + mapWidth / 2,
                    mapY + mapHeight / 2 - s(4),
                    0xFFE0E0E0);
        } else {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("phone.tzz_mod.map.missing"),
                    mapX + mapWidth / 2,
                    mapY + mapHeight / 2 - s(4),
                    0xFFE0E0E0);
        }
    }
}
