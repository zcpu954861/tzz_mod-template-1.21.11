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

        if (showFlowTab) {
            renderFlowTab(context);
        } else {
            renderCurrentTab(context);
        }
    }

    private void renderCurrentTab(DrawContext context) {
        // Display only current task: draw title below divider and render content with JSON component support
        // place the title a bit further below the divider to avoid a cramped look
        int titleY = contentY + s(24) + s(20) + s(16); // lower for more spacing
        int contentTop = titleY + s(20);

        var current = TaskClient.getCurrentTask();
        if (current == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.task.current.empty"), contentX + contentWidth / 2, titleY, 0xFFECECEC);
            return;
        }

        Text title = parseComponentOrLiteral(current.titleJson());
        context.drawCenteredTextWithShadow(textRenderer, title, contentX + contentWidth / 2, titleY, 0xFFECECEC);

        // Render content using textRenderer.wrapLines to preserve JSON component formatting
        Text contentText = parseComponentOrLiteral(current.contentJson());
        int wrapWidth = Math.max(s(20), contentWidth - s(8));
        List<OrderedText> lines = textRenderer.wrapLines(contentText, wrapWidth);
        int step = s(Math.max(10, textRenderer.fontHeight + 2));
        for (int i = 0; i < lines.size(); i++) {
            int y = contentTop + i * step;
            if (y > contentY + contentHeight - s(28)) break;
            context.drawTextWithShadow(textRenderer, lines.get(i), contentX + s(2), y, 0xFFECECEC);
        }
    }

    private void renderFlowTab(DrawContext context) {
        // Compute divider Y similar to renderPhoneContent so flow content starts below the divider
        int tabsBottom = contentY + s(24) + s(20);
        int dividerY = tabsBottom + s(10);
        int topStart = dividerY + s(16);
        int y = topStart;
        int rowGap = s(18);
        int dotX = contentX + s(6);
        int lineMaxX = contentX + contentWidth - s(36);

        for (TaskClient.TaskLineData line : TaskClient.getLines()) {
            // Show only triggered nodes in the flow tab (cancelled/untriggered tasks should be hidden)
            List<TaskClient.TaskNodeData> allNodes = line.tasks();
            List<TaskClient.TaskNodeData> visibleNodes = new ArrayList<>();
            for (TaskClient.TaskNodeData n : allNodes) {
                if (n.triggered()) visibleNodes.add(n);
            }
            if (visibleNodes.isEmpty()) continue;

            // Render only visible (triggered) nodes
            for (int i = 0; i < visibleNodes.size(); i++) {
                TaskClient.TaskNodeData node = visibleNodes.get(i);
                int centerY = y + i * rowGap + s(6);

                // Draw vertical connector between nodes
                if (i < visibleNodes.size() - 1) {
                    int nextCenterY = y + (i + 1) * rowGap + s(6);
                    int connX = dotX;
                    context.fill(connX, centerY + s(4), connX + s(1), nextCenterY - s(4), 0xFF7F8A97);
                }

                // Draw dot
                int radius = s(3);
                drawDot(context, dotX, centerY, 0xFF66FF66);

                // Small horizontal connector to title
                context.fill(dotX + s(4), centerY, dotX + s(10), centerY + 1, 0xFF7F8A97);

                // Draw title to the right
                Text title = parseComponentOrLiteral(node.titleJson());
                context.drawTextWithShadow(textRenderer, title, dotX + s(12), centerY - textRenderer.fontHeight / 2, 0xFFECECEC);

                // Draw small detail button at rightmost area
                int btnW = s(30);
                int btnH = s(12);
                int btnX = lineMaxX;
                int btnY = centerY - btnH / 2;
                // Draw a simple rounded rectangle (approx) for button background
                context.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0xAA2A8FC1);
                context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.open_subpage"), btnX + btnW / 2, btnY + Math.max(0, (btnH - textRenderer.fontHeight) / 2), 0xFFECECEC);

                // register clickable area
                detailButtons.add(new DetailButtonData(btnX, btnY, btnW, btnH, node.titleJson(), node.contentJson()));
            }

            // Advance y for next line group: visibleNodes.size * rowGap + spacing
            y += visibleNodes.size() * rowGap + s(12);
            // stop if exceeding content area
            if (y > contentY + contentHeight - s(28)) break;
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

    private void playCharSound() {
        if (client == null || client.player == null) return;
        try {
            client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 1.0F);
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

    private List<String> wrapStringToWidth(String str, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < str.length(); ) {
            int cp = str.codePointAt(i);
            int charLen = Character.charCount(cp);
            cur.appendCodePoint(cp);
            if (textRenderer.getWidth(cur.toString()) > maxWidth) {
                cur.setLength(cur.length() - charLen);
                if (cur.length() == 0) {
                    cur.appendCodePoint(cp);
                    i += charLen;
                }
                lines.add(cur.toString());
                cur = new StringBuilder();
                continue;
            }
            i += charLen;
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }
}
