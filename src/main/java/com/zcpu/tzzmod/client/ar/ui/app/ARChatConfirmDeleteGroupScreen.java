package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ARChatConfirmDeleteGroupScreen extends AbstractARScreen {
    private final String groupId;

    public ARChatConfirmDeleteGroupScreen(Screen parent, String groupId) {
        super(Text.translatable("phone.tzz_mod.chat.confirm_delete_title"), parent);
        this.groupId = groupId;
    }

    public boolean referencesGroup(String removedGroupId) {
        return groupId.equals(removedGroupId);
    }

    public Screen getChatHomeScreen() {
        if (parent instanceof ARChatManageMembersScreen manageMembersScreen) {
            return manageMembersScreen.getChatHomeScreen();
        }
        if (parent instanceof ARChatConversationScreen conversationScreen) {
            return conversationScreen.getChatHomeScreen();
        }
        return parent;
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();
        addARPrimaryButton(Text.translatable("phone.tzz_mod.chat.confirm_delete_confirm"), contentX + contentWidth - s(74), contentY + contentHeight - s(20), s(74), s(16), button -> PhoneChatClient.deleteGroup(groupId));
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.confirm_delete_title"), contentX + contentWidth / 2, contentY + s(10), themeAccent());
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.confirm_delete_prompt"), contentX + contentWidth / 2, contentY + contentHeight / 2 - scaledFontHeight(), themeText());
    }
}