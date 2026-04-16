package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class PhoneChatConfirmDeleteGroupScreen extends AbstractPhoneScreen {
    private final String groupId;

    public PhoneChatConfirmDeleteGroupScreen(Screen parent, String groupId) {
        super(Text.translatable("phone.tzz_mod.chat.confirm_delete_title"), parent);
        this.groupId = groupId;
    }

    public boolean referencesGroup(String removedGroupId) {
        return groupId.equals(removedGroupId);
    }

    public Screen getChatHomeScreen() {
        if (parent instanceof PhoneChatManageMembersScreen manageMembersScreen) {
            return manageMembersScreen.getChatHomeScreen();
        }
        if (parent instanceof PhoneChatConversationScreen conversationScreen) {
            return conversationScreen.getChatHomeScreen();
        }
        return parent;
    }

    @Override
    protected void init() {
        super.init();
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(72), s(20), b -> close());
        // Confirm button (red)
        addPhoneButton(Text.translatable("phone.tzz_mod.chat.confirm_delete_confirm"), contentX + contentWidth - s(82), contentY + contentHeight - s(24), s(82), s(20), b -> {
            PhoneChatClient.deleteGroup(groupId);
        });
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.chat.confirm_delete_title"), contentX + contentWidth / 2, contentY + s(8));
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.chat.confirm_delete_prompt"), contentX + s(4), contentY + s(36), 0xFFECECEC);
    }
}

