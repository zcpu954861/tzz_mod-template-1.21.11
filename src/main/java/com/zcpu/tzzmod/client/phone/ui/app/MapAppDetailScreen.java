package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MapAppDetailScreen extends AbstractPhoneScreen {
    private final Identifier mapTexture;

    public MapAppDetailScreen(Screen parent, Identifier mapTexture) {
        super(Text.translatable("phone.tzz_mod.app.map.detail"), parent);
        this.mapTexture = mapTexture;
    }

    @Override
    protected void init() {
        super.init();
        addDrawableChild(ButtonWidget.builder(Text.translatable("phone.tzz_mod.back"), button -> close())
                .dimensions(contentX, contentY + contentHeight - s(24), s(72), s(20))
                .build());
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // 使用不随缩放的文本绘制标题
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.map.detail"), contentX + contentWidth / 2, contentY + s(8));

        int mapX = contentX + s(8);
        int mapY = contentY + s(26);
        int mapWidth = contentWidth - s(16);
        int mapHeight = contentHeight - s(56);

        if (hasResource(mapTexture)) {
            // 直接绘制贴图
            context.drawTexturedQuad(mapTexture, mapX, mapY, mapX + mapWidth, mapY + mapHeight,
                    0.0F, 1.0F, 0.0F, 1.0F);
        } else {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("phone.tzz_mod.map.missing"),
                    mapX + mapWidth / 2,
                    mapY + mapHeight / 2 - s(4),
                    0xE0E0E0);
        }
    }
}
