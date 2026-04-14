package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AR-style create-group screen. Op-only form: group name + comma-separated member UUIDs/names.
 */
public class ARChatCreateGroupScreen extends AbstractARScreen {
    private TextFieldWidget nameField;
    private TextFieldWidget membersField;

    public ARChatCreateGroupScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.chat.create_group"), parent);
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();

        int fieldW = contentWidth - s(8);
        int fieldH = s(18);
        int labelH = scaledFontHeight() + s(4);
        int lx = contentX + s(4);

        // Group name
        int nameY = contentY + s(20);
        nameField = new TextFieldWidget(textRenderer,
                lx, nameY + labelH, fieldW, fieldH,
                Text.translatable("phone.tzz_mod.chat.group_name"));
        nameField.setMaxLength(64);
        styleTextField(nameField);
        addDrawableChild(nameField);

        // Members
        int membersY = nameY + labelH + fieldH + s(10);
        membersField = new TextFieldWidget(textRenderer,
                lx, membersY + labelH, fieldW, fieldH,
                Text.translatable("phone.tzz_mod.chat.group_members_hint"));
        membersField.setMaxLength(512);
        styleTextField(membersField);
        addDrawableChild(membersField);

        // Create button
        int btnW = s(80);
        int btnH = s(20);
        int btnX = contentX + (contentWidth - btnW) / 2;
        int btnY = membersY + labelH + fieldH + s(12);
        addARPrimaryButton(Text.translatable("phone.tzz_mod.chat.create_group_confirm"),
                btnX, btnY, btnW, btnH, btn -> createGroup());
    }

    private void createGroup() {
        if (nameField == null || membersField == null) return;
        String name = nameField.getText().trim();
        if (name.isEmpty()) return;
        String rawMembers = membersField.getText().trim();
        List<String> members = Arrays.stream(rawMembers.split("[,\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        PhoneChatClient.createGroup(name, members);
        close();
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        int lx = contentX + s(4);
        int labelH = scaledFontHeight() + s(4);
        int fieldH = s(18);
        int nameY = contentY + s(20);

        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.create_group"),
                contentX + contentWidth / 2, contentY + s(4), themeAccent());

        // Name label + field background
        drawScaledText(context, Text.translatable("phone.tzz_mod.chat.group_name"),
                lx, nameY, themeText());
        int fieldW = contentWidth - s(8);
        drawARInputFrame(context, lx, nameY + labelH, fieldW, fieldH);

        // Members label + field background
        int membersY = nameY + labelH + fieldH + s(10);
        drawScaledText(context, Text.translatable("phone.tzz_mod.chat.group_members_hint"),
                lx, membersY, themeTextDim());
        drawARInputFrame(context, lx, membersY + labelH, fieldW, fieldH);
    }
}
