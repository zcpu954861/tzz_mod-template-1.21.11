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
    private int scrollOffset;

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

    private int getContentTop() {
        return contentY + s(36);
    }

    private int getContentBottom() {
        return contentY + contentHeight - s(30);
    }

    private int getLineStep() {
        return s(Math.max(10, textRenderer.fontHeight + 2));
    }

    private List<OrderedText> buildLines() {
        int wrapWidth = Math.max(s(20), contentWidth - s(8));
        List<OrderedText> lines = new java.util.ArrayList<>();
        lines.addAll(textRenderer.wrapLines(parseComponentOrLiteral(titleJson), wrapWidth));
        lines.add(OrderedText.styledForwardsVisitedString("", net.minecraft.text.Style.EMPTY));
        lines.addAll(textRenderer.wrapLines(parseComponentOrLiteral(contentJson), wrapWidth));
        return lines;
    }

    private int getMaxScroll() {
        int totalHeight = buildLines().size() * getLineStep() + s(8);
        int visibleHeight = Math.max(1, getContentBottom() - getContentTop());
        return Math.max(0, totalHeight - visibleHeight);
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.task.details"), contentX + contentWidth / 2, contentY + s(8));

        scrollOffset = Math.max(0, Math.min(scrollOffset, getMaxScroll()));
        int top = getContentTop();
        int bottom = getContentBottom();
        int lineStep = getLineStep();
        int y = top - scrollOffset;

        for (OrderedText line : buildLines()) {
            if (y + lineStep >= top && y <= bottom) {
                context.drawTextWithShadow(textRenderer, line, contentX + s(2), y, 0xFFECECEC);
            }
            y += lineStep;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isHelpModeActive()) {
            return true;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        int top = getContentTop();
        int bottom = getContentBottom();
        if (mx >= contentX && mx <= contentX + contentWidth && my >= top && my <= bottom) {
            scrollOffset = Math.max(0, Math.min(scrollOffset - (int) Math.round(verticalAmount * s(12)), getMaxScroll()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
}
