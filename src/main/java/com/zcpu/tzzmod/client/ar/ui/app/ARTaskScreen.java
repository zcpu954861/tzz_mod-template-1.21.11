package com.zcpu.tzzmod.client.ar.ui.app;

import com.google.gson.JsonParser;
import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.task.TaskClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.math.MathHelper;

import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;

/**
 * AR-specific task screen. Shows current task and flow in tabbed HUD layout.
 */
public class ARTaskScreen extends AbstractARScreen {
    private int activeTab; // 0 = current, 1 = flow
    private float scrollOffsetCurrent;
    private float scrollOffsetFlow;

    public ARTaskScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.task"), parent);
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();

        // Tab buttons
        int tabW = s(50);
        int tabH = s(14);
        int tabY = contentY + s(2);
        int tabCX = contentX + contentWidth / 2;

        addARButton(Text.translatable("phone.tzz_mod.task.tab.current"),
                tabCX - tabW - s(2), tabY, tabW, tabH,
                ARButtonVariant.DEFAULT, () -> activeTab == 0,
                btn -> activeTab = 0);
        addARButton(Text.translatable("phone.tzz_mod.task.tab.flow"),
                tabCX + s(2), tabY, tabW, tabH,
                ARButtonVariant.DEFAULT, () -> activeTab == 1,
                btn -> activeTab = 1);
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        int bodyTop = contentY + s(20);
        int bodyBottom = contentY + contentHeight;

        if (activeTab == 0) {
            renderCurrentTab(context, bodyTop, bodyBottom);
        } else {
            renderFlowTab(context, bodyTop, bodyBottom);
        }
    }

    private void renderCurrentTab(DrawContext context, int top, int bottom) {
        TaskClient.CurrentTaskData task = TaskClient.getCurrentTask();
        if (task == null) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.task.current.empty"),
                    contentX + contentWidth / 2, top + s(20), themeTextDim());
            return;
        }

        List<String> lines = new ArrayList<>();
        // Title
        Text title = parseComponentOrLiteral(task.titleJson());
        lines.addAll(wrapText(title.getString(), contentWidth - s(16)));
        lines.add(""); // blank separator
        // Content
        Text content = parseComponentOrLiteral(task.contentJson());
        lines.addAll(wrapText(content.getString(), contentWidth - s(16)));

        int lineH = scaledFontHeight() + s(2);
        int totalH = lines.size() * lineH;
        int visibleH = bottom - top;
        int maxScroll = Math.max(0, totalH - visibleH);
        scrollOffsetCurrent = MathHelper.clamp(scrollOffsetCurrent, 0, maxScroll);

        context.enableScissor(contentX, top, contentX + contentWidth, bottom);
        int y = top - Math.round(scrollOffsetCurrent);
        boolean inTitle = true;
        boolean drewDivider = false;
        for (String line : lines) {
            if (y + lineH > top && y < bottom) {
                if (line.isEmpty()) {
                    // Draw theme-color divider (#10)
                    if (!drewDivider) {
                        int divY = y + lineH / 2;
                        context.fill(contentX + s(4), divY, contentX + contentWidth - s(4), divY + 1, themeAccent());
                        drewDivider = true;
                    }
                    inTitle = false;
                } else {
                    int color = inTitle ? themeAccent() : themeText();
                    // Center title lines (#3); body left-aligned
                    int lx;
                    if (inTitle) {
                        int lineW = scaledTextWidth(line);
                        lx = contentX + contentWidth / 2 - lineW / 2;
                    } else {
                        lx = contentX + s(8);
                    }
                    drawScaledText(context, Text.literal(line), lx, y, color);
                }
            }
            y += lineH;
        }
        context.disableScissor();

        if (totalH > visibleH) {
            renderScrollbar(context, top, bottom, totalH, Math.round(scrollOffsetCurrent));
        }
    }

    private void renderFlowTab(DrawContext context, int top, int bottom) {
        List<TaskClient.TaskLineData> taskLines = TaskClient.getLines();
        if (taskLines.isEmpty()) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.task.flow.empty"),
                    contentX + contentWidth / 2, top + s(20), themeTextDim());
            return;
        }

        int lineH = scaledFontHeight() + s(6);
        int totalH = 0;
        for (TaskClient.TaskLineData line : taskLines) {
            totalH += lineH; // line name header
            totalH += line.tasks().size() * lineH;
            totalH += s(4); // gap between groups
        }

        int visibleH = bottom - top;
        int maxScroll = Math.max(0, totalH - visibleH);
        scrollOffsetFlow = MathHelper.clamp(scrollOffsetFlow, 0, maxScroll);

        context.enableScissor(contentX, top, contentX + contentWidth, bottom);
        int y = top - Math.round(scrollOffsetFlow);

        for (TaskClient.TaskLineData tl : taskLines) {
            if (y + lineH > top && y < bottom) {
                // Line name header
                drawScaledText(context, Text.literal("▶ " + tl.name()),
                        contentX + s(6), y + s(2), themeAccent());
            }
            y += lineH;

            for (TaskClient.TaskNodeData node : tl.tasks()) {
                if (y + lineH > top && y < bottom) {
                    // Dot
                    int dotX = contentX + s(10);
                    int dotY = y + lineH / 2;
                    int dotR = Math.max(1, s(2));
                    context.fill(dotX - dotR, dotY - dotR, dotX + dotR, dotY + dotR,
                            node.triggered() ? 0xFF44FF88 : themeTextDim());

                    // Task title
                    Text title = parseComponentOrLiteral(node.titleJson());
                    drawScaledText(context, title,
                            dotX + s(6), y + s(2),
                            node.triggered() ? themeText() : themeTextDim());
                }
                y += lineH;
            }
            y += s(4);
        }

        context.disableScissor();

        if (totalH > visibleH) {
            renderScrollbar(context, top, bottom, totalH, Math.round(scrollOffsetFlow));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (activeTab == 0) {
            scrollOffsetCurrent -= (float) (verticalAmount * s(16));
        } else {
            scrollOffsetFlow -= (float) (verticalAmount * s(16));
        }
        return true;
    }

    private static Text parseComponentOrLiteral(String json) {
        if (json == null || json.isEmpty()) return Text.empty();
        try {
            var element = JsonParser.parseString(json);
            var result = TextCodecs.CODEC.parse(JsonOps.INSTANCE, element);
            return result.result().orElse(Text.literal(json));
        } catch (Throwable e) {
            return Text.literal(json);
        }
    }
}
