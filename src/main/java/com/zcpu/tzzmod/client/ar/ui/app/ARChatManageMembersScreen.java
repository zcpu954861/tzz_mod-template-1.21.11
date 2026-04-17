package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.phone.chat.ChatUiUtil;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.photo.GalleryAvatarRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ARChatManageMembersScreen extends AbstractARScreen {
    private final String groupId;
    private final String groupName;

    private final List<PhoneChatClient.ContactData> contacts = new ArrayList<>();
    private int scrollOffset;

    public ARChatManageMembersScreen(net.minecraft.client.gui.screen.Screen parent, String groupId, String groupName) {
        super(Text.translatable("phone.tzz_mod.chat.manage_members_title"), parent);
        this.groupId = groupId;
        this.groupName = groupName;
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();
        if (canEditMembers()) {
            addARPrimaryButton(Text.translatable("phone.tzz_mod.chat.delete_group"), contentX + contentWidth - s(66), contentY + contentHeight - s(20), s(66), s(16), button -> {
                if (client != null) {
                    client.setScreen(new ARChatConfirmDeleteGroupScreen(this, groupId));
                }
            });
        }
        PhoneChatClient.requestGroupMembers(groupId);
        rebuildContacts();
    }

    public boolean referencesGroup(String removedGroupId) {
        return groupId.equals(removedGroupId);
    }

    public Screen getChatHomeScreen() {
        if (parent instanceof ARChatConversationScreen conversationScreen) {
            return conversationScreen.getChatHomeScreen();
        }
        return parent;
    }

    @Override
    public void tick() {
        super.tick();
        rebuildContacts();
    }

    private void rebuildContacts() {
        contacts.clear();
        contacts.addAll(PhoneChatClient.getContacts());
        contacts.sort(Comparator.comparing(PhoneChatClient.ContactData::name, String.CASE_INSENSITIVE_ORDER));
        clampScroll();
    }

    private List<PhoneChatClient.GroupMemberData> getCurrentMembers() {
        return PhoneChatClient.getGroupMembersList(groupId);
    }

    private boolean isCurrentMember(String uuid) {
        return getCurrentMembers().stream().anyMatch(member -> member.uuid().equals(uuid) && member.isMember());
    }

    private String getOwnerUuid() {
        PhoneChatClient.GroupData entry = PhoneChatClient.getGroups().stream()
                .filter(group -> group.id().equals(groupId))
                .findFirst()
                .orElse(null);
        return entry != null ? entry.ownerUuid() : "";
    }

    private boolean canEditMembers() {
        return PhoneChatClient.getSelfUuid().equals(getOwnerUuid());
    }

    private int getListTop() {
        return contentY + s(26);
    }

    private int getListBottom() {
        return contentY + contentHeight - s(26);
    }

    private int getRowHeight() {
        return s(18);
    }

    private int getRowSpacing() {
        return s(3);
    }

    private int getListHeight() {
        return contacts.size() * (getRowHeight() + getRowSpacing());
    }

    private int getMaxScroll() {
        return Math.max(0, getListHeight() - Math.max(1, getListBottom() - getListTop()));
    }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, getMaxScroll()));
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawScaledCenteredText(context, Text.literal(groupName), contentX + contentWidth / 2, contentY + s(6), themeText());
        drawScaledCenteredText(context, Text.translatable(canEditMembers() ? "phone.tzz_mod.chat.members_hint" : "phone.tzz_mod.chat.group_members_hint"), contentX + contentWidth / 2, contentY + s(16), themeTextDim());

        int top = getListTop();
        int bottom = getListBottom();
        int rowHeight = getRowHeight();
        int rowSpacing = getRowSpacing();
        int y = top - scrollOffset;
        String selfUuid = PhoneChatClient.getSelfUuid();
        String ownerUuid = getOwnerUuid();

        context.enableScissor(contentX, top, contentX + contentWidth, bottom);
        for (PhoneChatClient.ContactData contact : contacts) {
            if (y + rowHeight >= top && y <= bottom) {
                boolean selected = isCurrentMember(contact.uuid());
                boolean isSelf = contact.uuid().equals(selfUuid);
                boolean isOwner = contact.uuid().equals(ownerUuid);
                int fill = selected ? 0x3315A389 : 0x22091420;
                int border = selected ? themeAccent() : themeBorder();
                ChatUiUtil.drawAngularFrame(context, contentX, y, contentWidth - s(4), rowHeight, s(3), fill, border);

                GalleryAvatarRenderer.drawAvatar(context, contact.uuid(), contentX + s(4), y + s(2), s(14), themeAccent());
                int labelX = contentX + s(22);
                drawScaledText(context, Text.literal(contact.name()), labelX, y + s(4), themeText());

                if (isOwner) {
                    drawScaledText(context, Text.translatable("phone.tzz_mod.chat.owner"), contentX + contentWidth - s(34), y + s(4), themeAccent());
                } else if (isSelf) {
                    drawScaledText(context, Text.translatable("phone.tzz_mod.chat.self"), contentX + contentWidth - s(26), y + s(4), themeTextDim());
                } else if (selected) {
                    drawScaledText(context, Text.translatable("phone.tzz_mod.chat.selected"), contentX + contentWidth - s(34), y + s(4), themeAccent());
                }
            }
            y += rowHeight + rowSpacing;
        }
        context.disableScissor();
        renderScrollbar(context, top, bottom, Math.max(bottom - top, getListHeight()), scrollOffset);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        if (!canEditMembers()) {
            return false;
        }

        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        if (mouseX < contentX || mouseX > contentX + contentWidth || mouseY < getListTop() || mouseY > getListBottom()) {
            return false;
        }

        int rowHeight = getRowHeight();
        int rowSpacing = getRowSpacing();
        int y = getListTop() - scrollOffset;
        String selfUuid = PhoneChatClient.getSelfUuid();
        for (PhoneChatClient.ContactData contact : contacts) {
            if (mouseX >= contentX && mouseX <= contentX + contentWidth - s(4) && mouseY >= y && mouseY <= y + rowHeight) {
                if (!contact.uuid().equals(selfUuid)) {
                    if (isCurrentMember(contact.uuid())) {
                        PhoneChatClient.removeGroupMember(groupId, contact.uuid());
                    } else {
                        PhoneChatClient.addGroupMember(groupId, contact.uuid());
                    }
                }
                return true;
            }
            y += rowHeight + rowSpacing;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isHelpModeActive()) {
            return true;
        }
        if (mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= getListTop() && mouseY <= getListBottom()) {
            scrollOffset -= (int) Math.round(verticalAmount * s(18));
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}