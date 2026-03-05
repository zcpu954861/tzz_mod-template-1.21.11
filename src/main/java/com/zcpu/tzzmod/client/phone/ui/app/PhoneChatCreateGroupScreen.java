package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PhoneChatCreateGroupScreen extends AbstractPhoneScreen {
    private TextFieldWidget nameField;
    private TextFieldWidget membersField;

    public PhoneChatCreateGroupScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.chat.create_group"), parent);
    }

    @Override
    protected void init() {
        super.init();

        addDrawableChild(ButtonWidget.builder(Text.translatable("phone.tzz_mod.back"), button -> close())
                .dimensions(contentX, contentY + contentHeight - s(24), s(72), s(20))
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("phone.tzz_mod.chat.create"), button -> createGroup())
                .dimensions(contentX + contentWidth - s(82), contentY + contentHeight - s(24), s(82), s(20))
                .build());

        nameField = new TextFieldWidget(textRenderer, contentX, contentY + s(28), contentWidth, s(20), Text.empty());
        nameField.setPlaceholder(Text.translatable("phone.tzz_mod.chat.group_name"));
        nameField.setMaxLength(32);
        addDrawableChild(nameField);

        membersField = new TextFieldWidget(textRenderer, contentX, contentY + s(56), contentWidth, s(20), Text.empty());
        membersField.setPlaceholder(Text.translatable("phone.tzz_mod.chat.members_hint"));
        membersField.setMaxLength(256);
        addDrawableChild(membersField);
    }

    private void createGroup() {
        if (!PhoneChatClient.isOp()) {
            return;
        }

        String name = nameField.getText().trim();
        String tokens = membersField.getText();
        String[] parts = tokens.split(",");
        List<String> members = new ArrayList<>();
        for (String part : parts) {
            String uuid = PhoneChatClient.resolveUuidByNameOrUuid(part.trim());
            if (!uuid.isBlank()) {
                members.add(uuid);
            }
        }

        PhoneChatClient.createGroup(name, members);
        close();
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.chat.create_group"), contentX + contentWidth / 2, contentY + s(8));
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.chat.group_name"), contentX, contentY + s(18), 0xFFECECEC);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.chat.members_hint"), contentX, contentY + s(46), 0xFFBFC7D5);
    }
}

