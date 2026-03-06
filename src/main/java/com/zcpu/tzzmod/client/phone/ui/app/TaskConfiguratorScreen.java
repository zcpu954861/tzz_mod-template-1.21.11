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

    public TaskConfiguratorScreen(net.minecraft.client.gui.screen.Screen parent) {
        super(Text.translatable("phone.tzz_mod.task.configurator"), parent);
    }

    @Override
    protected void init() {
        super.init();

        // layout metrics
        int topStart = contentY + s(28);
        int fieldGap = s(22);
        int fieldHeight = s(18);

        // buttons at bottom
        int buttonY = contentY + contentHeight - s(28);
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, buttonY, s(70), s(20), button -> close());

        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.task.config.save"), contentX + contentWidth - s(72), buttonY, s(72), s(20), button -> saveTask());

        addPhoneButton(Text.translatable("phone.tzz_mod.task.config.sync"), contentX + contentWidth - s(152), buttonY, s(72), s(20), button -> TaskClient.requestBootstrap());

        // fields arranged vertically with consistent gaps
        int y = topStart;
        lineNameField = new TextFieldWidget(textRenderer, contentX, y + s(12), contentWidth, fieldHeight, Text.empty());
        lineNameField.setPlaceholder(Text.translatable("phone.tzz_mod.task.config.line_name"));
        lineNameField.setMaxLength(64);
        addDrawableChild(lineNameField);
        y += fieldGap;

        indexField = new TextFieldWidget(textRenderer, contentX, y + s(12), contentWidth, fieldHeight, Text.empty());
        indexField.setPlaceholder(Text.translatable("phone.tzz_mod.task.config.index"));
        indexField.setMaxLength(6);
        addDrawableChild(indexField);
        y += fieldGap;

        titleJsonField = new TextFieldWidget(textRenderer, contentX, y + s(12), contentWidth, fieldHeight, Text.empty());
        titleJsonField.setPlaceholder(Text.translatable("phone.tzz_mod.task.config.title_json"));
        titleJsonField.setMaxLength(25600);
        addDrawableChild(titleJsonField);
        y += fieldGap;

        // make content field slightly taller to give more room
        contentJsonField = new TextFieldWidget(textRenderer, contentX, y + s(12), contentWidth, s(28), Text.empty());
        contentJsonField.setPlaceholder(Text.translatable("phone.tzz_mod.task.config.content_json"));
        contentJsonField.setMaxLength(25600);
        addDrawableChild(contentJsonField);
        // no further use of y after this point

        stateListener = () -> {
        };
        TaskClient.addListener(stateListener);
        TaskClient.requestBootstrap();
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

        // Labels placed slightly above their corresponding input fields
        int topStart = contentY + s(28);
        int fieldGap = s(22);

        int labelY = topStart;
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.task.config.line_name"), contentX, labelY, 0xFFBFC7D5);

        labelY += fieldGap;
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.task.config.index"), contentX, labelY, 0xFFBFC7D5);

        labelY += fieldGap;
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.task.config.title_json"), contentX, labelY, 0xFFBFC7D5);

        labelY += fieldGap;
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.task.config.content_json"), contentX, labelY, 0xFFBFC7D5);

        if (!feedback.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.literal(feedback), contentX, labelY + s(28), 0xFFECECEC);
        }

        List<TaskClient.TaskLineData> lines = TaskClient.getLines();
        // compute preview start Y based on our fields
        int previewY = topStart + fieldGap * 4 + s(36);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.task.config.preview"), contentX, previewY, 0xFF8BD6FF);
        previewY += s(12);

        int shown = 0;
        for (TaskClient.TaskLineData line : lines) {
            if (previewY > contentY + contentHeight - s(30)) {
                break;
            }
            context.drawTextWithShadow(textRenderer,
                    Text.literal(line.name() + " (" + line.tasks().size() + ")"),
                    contentX,
                    previewY,
                    0xFFECECEC);
            previewY += s(10);
            shown++;
            if (shown >= 6) {
                break;
            }
        }
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
