package com.zcpu.tzzmod.client.phone.ui.app;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.task.TaskClient;
import com.zcpu.tzzmod.client.phone.ui.TypingSubtitleAnimator;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.ArrayList;
import java.util.List;

public class PhoneTaskAppScreen extends AbstractPhoneScreen {
    private boolean showFlowTab;
    private Runnable stateListener;
    // dynamic per-task detail buttons (store bounds and target payload)
    private final List<DetailButtonData> detailButtons = new ArrayList<>();
    private TypingSubtitleAnimator subtitleAnimator;
    private int lastTotalUnread = 0;
    private int currentScrollOffset;
    private int flowScrollOffset;

    public PhoneTaskAppScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.task"), parent);
    }

    @Override
    protected void init() {
        super.init();

        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(70), s(20), button -> close());

        addPhoneTabButton(Text.translatable("phone.tzz_mod.task.tab.current"), contentX, contentY + s(24), s(86), s(20), () -> !showFlowTab, button -> showFlowTab = false);

        addPhoneTabButton(Text.translatable("phone.tzz_mod.task.tab.flow"), contentX + s(90), contentY + s(24), s(86), s(20), () -> showFlowTab, button -> showFlowTab = true);

        stateListener = () -> {};
        TaskClient.addListener(stateListener);
        TaskClient.clearAllUnread();
        TaskClient.requestBootstrap();

        lastTotalUnread = TaskClient.getTotalUnreadCount();
        if (PhoneSettingsClient.isAlertModeEnabled() && lastTotalUnread > 0) {
            subtitleAnimator = new TypingSubtitleAnimator("收到新任务", 14, s -> playCharSound());
            subtitleAnimator.start();
        }
    }

    private int getTabContentTop() {
        int tabsBottom = contentY + s(24) + s(20);
        int dividerY = tabsBottom + s(10);
        return dividerY + s(10);
    }

    private int getTabContentBottom() {
        return contentY + contentHeight - s(30);
    }

    private int getTextStep() {
        return s(Math.max(10, textRenderer.fontHeight + 2));
    }

    private int getCurrentMaxScroll() {
        int visibleHeight = Math.max(1, getTabContentBottom() - getTabContentTop());
        int totalHeight = buildCurrentLines().size() * getTextStep() + s(8);
        return Math.max(0, totalHeight - visibleHeight);
    }

    private int getFlowMaxScroll() {
        int visibleHeight = Math.max(1, getTabContentBottom() - getTabContentTop());
        int totalHeight = Math.max(0, computeFlowContentHeight() - getTabContentTop());
        return Math.max(0, totalHeight - visibleHeight);
    }

    private void clampScrollOffsets() {
        currentScrollOffset = Math.max(0, Math.min(currentScrollOffset, getCurrentMaxScroll()));
        flowScrollOffset = Math.max(0, Math.min(flowScrollOffset, getFlowMaxScroll()));
    }

    private List<OrderedText> buildCurrentLines() {
        List<OrderedText> lines = new ArrayList<>();
        var current = TaskClient.getCurrentTask();
        if (current == null) {
            lines.addAll(textRenderer.wrapLines(Text.translatable("phone.tzz_mod.task.current.empty"), Math.max(s(20), contentWidth - s(8))));
            return lines;
        }

        int wrapWidth = Math.max(s(20), contentWidth - s(8));
        lines.addAll(textRenderer.wrapLines(parseComponentOrLiteral(current.titleJson()), wrapWidth));
        lines.add(OrderedText.styledForwardsVisitedString("", net.minecraft.text.Style.EMPTY));
        lines.addAll(textRenderer.wrapLines(parseComponentOrLiteral(current.contentJson()), wrapWidth));
        return lines;
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // App header (keeps at top)
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.task"), contentX + contentWidth / 2, contentY + s(8));

        if (subtitleAnimator != null) {
            subtitleAnimator.tick(delta);
            Text sub = subtitleAnimator.getRenderedText();
            if (!sub.getString().isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, sub, contentX + contentWidth / 2, contentY + s(30), 0xFFB8FFD4);
            }
            if (subtitleAnimator.isFinished()) subtitleAnimator = null;
        }

        // draw divider under the tab buttons area so the content area is visually separated
        // leave a bit more breathing room between tabs and divider
        int tabsBottom = contentY + s(24) + s(20); // tab Y + tab height
        int dividerY = tabsBottom + s(10);
        context.fill(contentX + s(2), dividerY, contentX + contentWidth - s(2), dividerY + 1, 0xFFCCCCCC);

        detailButtons.clear();
        clampScrollOffsets();

        if (showFlowTab) {
            renderFlowTab(context);
        } else {
            renderCurrentTab(context);
        }
    }

    private void renderCurrentTab(DrawContext context) {
        int top = getTabContentTop();
        int bottom = getTabContentBottom();
        int lineStep = getTextStep();
        List<OrderedText> lines = buildCurrentLines();
        int y = top - currentScrollOffset;

        for (OrderedText line : lines) {
            if (y + lineStep >= top && y <= bottom) {
                context.drawTextWithShadow(textRenderer, line, contentX + s(2), y, 0xFFECECEC);
            }
            y += lineStep;
        }
    }

    private int computeFlowContentHeight() {
        int y = getTabContentTop();
        int lineStep = getTextStep();
        int textWidth = Math.max(s(20), contentWidth - s(58));
        int buttonHeight = s(12);

        for (TaskClient.TaskLineData line : TaskClient.getLines()) {
            List<TaskClient.TaskNodeData> visibleNodes = new ArrayList<>();
            for (TaskClient.TaskNodeData node : line.tasks()) {
                if (node.triggered()) {
                    visibleNodes.add(node);
                }
            }
            if (visibleNodes.isEmpty()) {
                continue;
            }

            y += lineStep + s(4);
            for (TaskClient.TaskNodeData node : visibleNodes) {
                int textHeight = Math.max(lineStep, textRenderer.wrapLines(parseComponentOrLiteral(node.titleJson()), textWidth).size() * lineStep);
                int rowHeight = Math.max(buttonHeight, textHeight) + s(6);
                y += rowHeight + s(4);
            }
            y += s(4);
        }
        return y;
    }

    private void renderFlowTab(DrawContext context) {
        int top = getTabContentTop();
        int bottom = getTabContentBottom();
        int lineStep = getTextStep();
        int y = top - flowScrollOffset;
        int dotX = contentX + s(6);
        int textX = contentX + s(14);
        int btnW = s(30);
        int btnH = s(12);
        int btnX = contentX + contentWidth - btnW - s(2);
        int textWidth = Math.max(s(20), btnX - textX - s(6));
        boolean drewAny = false;

        for (TaskClient.TaskLineData line : TaskClient.getLines()) {
            List<TaskClient.TaskNodeData> visibleNodes = new ArrayList<>();
            for (TaskClient.TaskNodeData node : line.tasks()) {
                if (node.triggered()) {
                    visibleNodes.add(node);
                }
            }
            if (visibleNodes.isEmpty()) {
                continue;
            }

            drewAny = true;
            if (y + lineStep >= top && y <= bottom) {
                context.drawTextWithShadow(textRenderer, Text.literal(line.name()), contentX + s(2), y, 0xFF8BD6FF);
            }
            y += lineStep + s(4);

            for (TaskClient.TaskNodeData node : visibleNodes) {
                List<OrderedText> wrappedTitle = textRenderer.wrapLines(parseComponentOrLiteral(node.titleJson()), textWidth);
                int textHeight = Math.max(lineStep, wrappedTitle.size() * lineStep);
                int rowHeight = Math.max(btnH, textHeight) + s(6);
                int rowTop = y;
                int rowBottom = y + rowHeight;

                if (rowBottom >= top && rowTop <= bottom) {
                    context.fill(contentX, rowTop, contentX + contentWidth, rowBottom, 0x22333333);
                    drawDot(context, dotX, rowTop + rowHeight / 2, 0xFF66FF66);
                    for (int i = 0; i < wrappedTitle.size(); i++) {
                        int lineY = rowTop + s(3) + i * lineStep;
                        context.drawTextWithShadow(textRenderer, wrappedTitle.get(i), textX, lineY, 0xFFECECEC);
                    }

                    int btnY = rowTop + Math.max(0, (rowHeight - btnH) / 2);
                    context.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0xAA2A8FC1);
                    context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.open_subpage"), btnX + btnW / 2, btnY + Math.max(0, (btnH - textRenderer.fontHeight) / 2), 0xFFECECEC);
                    detailButtons.add(new DetailButtonData(btnX, btnY, btnW, btnH, node.titleJson(), node.contentJson()));
                }

                y += rowHeight + s(4);
            }

            y += s(4);
        }

        if (!drewAny) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.task.flow.empty"), contentX + contentWidth / 2, top, 0xFFECECEC);
        }
    }

    private void drawDot(DrawContext context, int centerX, int centerY, int color) {
        int radius = s(3);
        int squared = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy <= squared) {
                    context.fill(centerX + dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, color);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) return true;
        int mx = (int) click.x();
        int my = (int) click.y();
        for (DetailButtonData d : detailButtons) {
            if (mx >= d.x && mx <= d.x + d.w && my >= d.y && my <= d.y + d.h) {
                if (client != null) client.setScreen(new TaskDetailScreen(this, d.titleJson, d.contentJson));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        int top = getTabContentTop();
        int bottom = getTabContentBottom();

        if (mx >= contentX && mx <= contentX + contentWidth && my >= top && my <= bottom) {
            int delta = (int) Math.round(verticalAmount * s(12));
            if (showFlowTab) {
                flowScrollOffset = Math.max(0, Math.min(flowScrollOffset - delta, getFlowMaxScroll()));
            } else {
                currentScrollOffset = Math.max(0, Math.min(currentScrollOffset - delta, getCurrentMaxScroll()));
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void playCharSound() {
        if (client == null || client.player == null) return;
        try {
            var player = client.player;
            if (player == null) {
                return;
            }
            player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 1.0F);
        } catch (Exception ignored) {}
    }

    private static final class DetailButtonData {
        final int x, y, w, h;
        final String titleJson, contentJson;

        DetailButtonData(int x, int y, int w, int h, String titleJson, String contentJson) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.titleJson = titleJson; this.contentJson = contentJson;
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
}
