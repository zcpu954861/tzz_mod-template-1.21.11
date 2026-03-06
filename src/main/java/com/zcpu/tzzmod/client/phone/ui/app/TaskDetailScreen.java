package com.zcpu.tzzmod.client.phone.ui.app;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.List;

public class TaskDetailScreen extends AbstractPhoneScreen {
    private final String titleJson;
    private final String contentJson;

    public TaskDetailScreen(Screen parent, String titleJson, String contentJson) {
        super(Text.translatable("phone.tzz_mod.task.details"), parent);
        this.titleJson = titleJson == null ? "" : titleJson;
        this.contentJson = contentJson == null ? "" : contentJson;
    }

    @Override
    protected void init() {
        super.init();
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(70), s(20), button -> close());
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.task.details"), contentX + contentWidth / 2, contentY + s(8));

        Text title = parseComponentOrLiteral(titleJson);
        context.drawCenteredTextWithShadow(textRenderer, title, contentX + contentWidth / 2, contentY + s(36), 0xFFECECEC);

        int textTop = contentY + s(52);
        int wrapWidth = Math.max(s(20), contentWidth - s(8));
        List<OrderedText> lines = textRenderer.wrapLines(parseComponentOrLiteral(contentJson), wrapWidth);
        int step = s(Math.max(10, textRenderer.fontHeight + 2));

        for (int i = 0; i < lines.size(); i++) {
            int y = textTop + i * step;
            if (y > contentY + contentHeight - s(28)) break;
            context.drawTextWithShadow(textRenderer, lines.get(i), contentX + s(2), y, 0xFFECECEC);
        }
    }

    private Text parseComponentOrLiteral(String raw) {
        if (raw == null || raw.isBlank()) return Text.empty();
        String trimmed = raw.trim();
        if (!looksLikeJsonComponent(trimmed)) return Text.literal(raw);
        try {
            JsonElement json = JsonParser.parseString(trimmed);
            return TextCodecs.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(Text.literal(raw));
        } catch (Exception ignored) {
            return Text.literal(raw);
        }
    }

    private boolean looksLikeJsonComponent(String value) {
        return value != null && (value.startsWith("{") || value.startsWith("[") || value.startsWith("\""));
    }

    private String titleJson() {
        return titleJson == null ? "" : titleJson;
    }

    private String contentJson() {
        return contentJson == null ? "" : contentJson;
    }
}
