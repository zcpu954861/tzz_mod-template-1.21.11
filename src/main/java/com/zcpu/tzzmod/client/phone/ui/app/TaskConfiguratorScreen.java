package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.task.TaskClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

public class TaskConfiguratorScreen extends AbstractPhoneScreen {
    private TextFieldWidget lineNameField;
    private TextFieldWidget indexField;
    private TextFieldWidget titleJsonField;
    private TextFieldWidget contentJsonField;

    private String feedback = "";
    private Runnable stateListener;
    private int previewScrollOffset;

    public TaskConfiguratorScreen(net.minecraft.client.gui.screen.Screen parent) {
        super(Text.translatable("phone.tzz_mod.task.configurator"), parent);
    }

    @Override
    protected void init() {
        super.init();

        int buttonY = contentY + contentHeight - s(28);
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, buttonY, s(70), s(20), button -> close());
        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.task.config.save"), contentX + contentWidth - s(72), buttonY, s(72), s(20), button -> saveTask());
        addPhoneButton(Text.translatable("phone.tzz_mod.task.config.sync"), contentX + contentWidth - s(152), buttonY, s(72), s(20), button -> TaskClient.requestBootstrap());

        int labelGap = s(4);
        int sectionGap = s(10);
        int fieldHeight = s(18);
        int wideFieldHeight = s(24);
        int y = contentY + s(26);

        lineNameField = new TextFieldWidget(textRenderer, contentX, y + textRenderer.fontHeight + labelGap, contentWidth, fieldHeight, Text.empty());
        lineNameField.setPlaceholder(Text.translatable("phone.tzz_mod.task.config.line_name"));
        lineNameField.setMaxLength(64);
        addDrawableChild(lineNameField);
        y = lineNameField.getY() + fieldHeight + sectionGap;

        indexField = new TextFieldWidget(textRenderer, contentX, y + textRenderer.fontHeight + labelGap, contentWidth, fieldHeight, Text.empty());
        indexField.setPlaceholder(Text.translatable("phone.tzz_mod.task.config.index"));
        indexField.setMaxLength(6);
        addDrawableChild(indexField);
        y = indexField.getY() + fieldHeight + sectionGap;

        titleJsonField = new TextFieldWidget(textRenderer, contentX, y + textRenderer.fontHeight + labelGap, contentWidth, fieldHeight, Text.empty());
        titleJsonField.setPlaceholder(Text.translatable("phone.tzz_mod.task.config.title_json"));
        titleJsonField.setMaxLength(25600);
        addDrawableChild(titleJsonField);
        y = titleJsonField.getY() + fieldHeight + sectionGap;

        contentJsonField = new TextFieldWidget(textRenderer, contentX, y + textRenderer.fontHeight + labelGap, contentWidth, wideFieldHeight, Text.empty());
        contentJsonField.setPlaceholder(Text.translatable("phone.tzz_mod.task.config.content_json"));
        contentJsonField.setMaxLength(25600);
        addDrawableChild(contentJsonField);

        stateListener = () -> {
        };
        TaskClient.addListener(stateListener);
        TaskClient.requestBootstrap();
    }

    private int getPreviewHeaderY() {
        return contentJsonField.getY() + contentJsonField.getHeight() + s(12);
    }

    private int getPreviewListTop() {
        return getPreviewHeaderY() + s(12);
    }

    private int getPreviewListBottom() {
        return contentY + contentHeight - s(34);
    }

    private int getPreviewContentHeight() {
        int lineStep = s(Math.max(10, textRenderer.fontHeight + 2));
        int total = 0;
        for (TaskClient.TaskLineData line : TaskClient.getLines()) {
            total += Math.max(lineStep, textRenderer.wrapLines(Text.literal(line.name() + " (" + line.tasks().size() + ")"), Math.max(s(20), contentWidth - s(4))).size() * lineStep) + s(4);
        }
        return total;
    }

    private int getPreviewMaxScroll() {
        int visibleHeight = Math.max(1, getPreviewListBottom() - getPreviewListTop());
        return Math.max(0, getPreviewContentHeight() - visibleHeight);
    }

    private void saveTask() {
        String lineName = lineNameField.getText().trim();
        int index;
        try {
            index = Integer.parseInt(indexField.getText().trim());
        } catch (Exception ignored) {
            feedback = Text.translatable("phone.tzz_mod.task.config.invalid_index").getString();
            return;
        }

        String titleJson = titleJsonField.getText().trim();
        String contentJson = contentJsonField.getText().trim();

        if (lineName.isEmpty() || index < 1) {
            feedback = Text.translatable("phone.tzz_mod.task.config.invalid_index").getString();
            return;
        }

        TaskClient.upsertTask(lineName, index, titleJson, contentJson);
        feedback = Text.translatable("phone.tzz_mod.task.config.saved").getString();
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.task.configurator"), contentX + contentWidth / 2, contentY + s(8));

        int labelGap = s(4);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.task.config.line_name"), contentX, lineNameField.getY() - textRenderer.fontHeight - labelGap, 0xFFBFC7D5);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.task.config.index"), contentX, indexField.getY() - textRenderer.fontHeight - labelGap, 0xFFBFC7D5);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.task.config.title_json"), contentX, titleJsonField.getY() - textRenderer.fontHeight - labelGap, 0xFFBFC7D5);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.task.config.content_json"), contentX, contentJsonField.getY() - textRenderer.fontHeight - labelGap, 0xFFBFC7D5);

        int previewHeaderY = getPreviewHeaderY();
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.task.config.preview"), contentX, previewHeaderY, 0xFF8BD6FF);

        if (!feedback.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.literal(feedback), contentX, Math.min(previewHeaderY, getPreviewListBottom()) - s(12), 0xFFECECEC);
        }

        previewScrollOffset = Math.max(0, Math.min(previewScrollOffset, getPreviewMaxScroll()));
        int y = getPreviewListTop() - previewScrollOffset;
        int lineStep = s(Math.max(10, textRenderer.fontHeight + 2));
        int listTop = getPreviewListTop();
        int listBottom = getPreviewListBottom();

        for (TaskClient.TaskLineData line : TaskClient.getLines()) {
            List<net.minecraft.text.OrderedText> wrapped = textRenderer.wrapLines(Text.literal(line.name() + " (" + line.tasks().size() + ")"), Math.max(s(20), contentWidth - s(4)));
            int blockHeight = Math.max(lineStep, wrapped.size() * lineStep);
            if (y + blockHeight >= listTop && y <= listBottom) {
                for (int i = 0; i < wrapped.size(); i++) {
                    context.drawTextWithShadow(textRenderer, wrapped.get(i), contentX, y + i * lineStep, 0xFFECECEC);
                }
            }
            y += blockHeight + s(4);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        int listTop = getPreviewListTop();
        int listBottom = getPreviewListBottom();
        if (mx >= contentX && mx <= contentX + contentWidth && my >= listTop && my <= listBottom) {
            previewScrollOffset = Math.max(0, Math.min(previewScrollOffset - (int) Math.round(verticalAmount * s(12)), getPreviewMaxScroll()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void removed() {
        super.removed();
        if (stateListener != null) {
            TaskClient.removeListener(stateListener);
            stateListener = null;
        }
    }
}
