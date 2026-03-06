package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import com.zcpu.tzzmod.client.phone.ui.app.PhoneChatConfirmDeleteGroupScreen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhoneChatManageMembersScreen extends AbstractPhoneScreen {
    private final String groupId;
    private Runnable stateListener;

    // local selection cache: uuid -> isMember
    private final Map<String, Boolean> selection = new HashMap<>();
    // no per-row ButtonWidgets; we handle clicks in mouseClicked
    private int scrollOffset = 0;

    private int getListTop() { return contentY + s(36); }

    private int getListBottom() { return contentY + contentHeight - s(40); }

    private int getRowHeight() { return s(20); }

    private int getRowGap() { return s(3); }

    public PhoneChatManageMembersScreen(Screen parent, String groupId) {
        super(Text.translatable("phone.tzz_mod.chat.manage_members"), parent);
        this.groupId = groupId;
    }

    @Override
    protected void init() {
        super.init();
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(72), s(20), button -> close());

        // Show Delete Group button (red) at bottom-right for group owner only
        boolean isOwner = false;
        for (PhoneChatClient.GroupData g : PhoneChatClient.getGroups()) {
            if (g.id().equals(groupId) && g.ownerUuid().equals(PhoneChatClient.getSelfUuid())) { isOwner = true; break; }
        }
        if (isOwner) {
            // place to the bottom-right; draw label in red
            Text delLabel = Text.translatable("phone.tzz_mod.chat.delete_group").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.RED)));
            addPhoneButton(delLabel, contentX + contentWidth - s(82), contentY + contentHeight - s(24), s(82), s(20), button -> client.setScreen(new PhoneChatConfirmDeleteGroupScreen(this, groupId)));
        }

        // request current member list from server
        // request authoritative list from server (will trigger rebuildRows())
        // initialize selection from cached entries so UI isn't empty while waiting for server
        List<PhoneChatClient.GroupMemberData> initial = PhoneChatClient.getGroupMembersList(groupId);
        selection.clear();
        for (PhoneChatClient.GroupMemberData e : initial) selection.put(e.uuid(), e.isMember());
        PhoneChatClient.requestGroupMembers(groupId);
        stateListener = this::rebuildRows;
        PhoneChatClient.addListener(stateListener);
    }

    private void rebuildRows() {
        // Update selection map from latest server-provided entries.
        List<PhoneChatClient.GroupMemberData> entries = PhoneChatClient.getGroupMembersList(groupId);
        selection.clear();
        for (PhoneChatClient.GroupMemberData e : entries) {
            selection.put(e.uuid(), e.isMember());
        }
        // clamp scroll offset when the content changes
        clampScrollOffset();
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.chat.manage_members"), contentX + contentWidth / 2, contentY + s(8));

        // draw list header
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.chat.members_hint"), contentX, contentY + s(28), 0xFFBFC7D5);

        List<PhoneChatClient.GroupMemberData> entries = PhoneChatClient.getGroupMembersList(groupId);
        int fieldHeight = getRowHeight();
        int rowGap = getRowGap();
        int startY = getListTop();
        int top = getListTop();
        int bottom = getListBottom();
        int visibleHeight = Math.max(1, bottom - top);
        int totalHeight = entries.size() == 0 ? 0 : entries.size() * fieldHeight + Math.max(0, entries.size() - 1) * rowGap;
        int maxScroll = Math.max(0, totalHeight - visibleHeight);
        if (scrollOffset < 0) scrollOffset = 0; else if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        String selfUuid = PhoneChatClient.getSelfUuid();

        for (int i = 0; i < entries.size(); i++) {
            PhoneChatClient.GroupMemberData entry = entries.get(i);
            int y = startY + i * (fieldHeight + rowGap) - scrollOffset;
            boolean isMember = selection.getOrDefault(entry.uuid(), entry.isMember());

            // draw name and checkbox; buttons were created in init() to capture clicks
            int boxSize = s(12);
            int boxPadding = s(6);
            int boxX = contentX + contentWidth - boxPadding - boxSize;
            int boxY = y + Math.max(0, (fieldHeight - boxSize) / 2);

            int maxNameWidth = contentWidth - (boxPadding + boxSize + s(12));
            String displayName = textRenderer.trimToWidth(entry.name(), Math.max(1, maxNameWidth));
            context.drawTextWithShadow(textRenderer, Text.literal(displayName), contentX + s(4), y + Math.max(0, (fieldHeight - textRenderer.fontHeight) / 2), 0xFFECECEC);

            if (entry.uuid().equals(selfUuid)) {
                context.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF3CB371);
                context.drawTextWithShadow(textRenderer, Text.literal("✓"), boxX + (boxSize - textRenderer.getWidth("✓")) / 2, boxY + Math.max(0, (boxSize - textRenderer.fontHeight) / 2), 0xFFFFFFFF);
            } else if (isMember) {
                context.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF3CB371);
                context.drawTextWithShadow(textRenderer, Text.literal("✓"), boxX + (boxSize - textRenderer.getWidth("✓")) / 2, boxY + Math.max(0, (boxSize - textRenderer.fontHeight) / 2), 0xFFFFFFFF);
            } else {
                int t = Math.max(1, s(1));
                context.fill(boxX, boxY, boxX + boxSize, boxY + t, 0xFF7F8A97);
                context.fill(boxX, boxY + boxSize - t, boxX + boxSize, boxY + boxSize, 0xFF7F8A97);
                context.fill(boxX, boxY, boxX + t, boxY + boxSize, 0xFF7F8A97);
                context.fill(boxX + boxSize - t, boxY, boxX + boxSize, boxY + boxSize, 0xFF7F8A97);
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        // Let widgets handle clicks first
        if (super.mouseClicked(click, doubleClick)) return true;
        int mx = (int) click.x();
        int my = (int) click.y();
        int fieldHeight = getRowHeight();
        int rowGap = getRowGap();
        int startY = getListTop();
        int top = getListTop();
        int bottom = getListBottom();
        if (mx < contentX || mx > contentX + contentWidth || my < top || my > bottom) return false;
        List<PhoneChatClient.GroupMemberData> entries = PhoneChatClient.getGroupMembersList(groupId);
        for (int i = 0; i < entries.size(); i++) {
            int y = startY + i * (fieldHeight + rowGap) - scrollOffset;
            if (my >= y && my <= y + fieldHeight) {
                PhoneChatClient.GroupMemberData entry = entries.get(i);
                String uuid = entry.uuid();
                String self = PhoneChatClient.getSelfUuid();
                if (uuid.equals(self)) return true; // cannot toggle self
                boolean currently = selection.getOrDefault(uuid, entry.isMember());
                boolean newState = !currently;
                selection.put(uuid, newState);
                if (newState) PhoneChatClient.addGroupMember(groupId, uuid);
                else PhoneChatClient.removeGroupMember(groupId, uuid);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        int top = getListTop();
        int bottom = getListBottom();
        if (mx >= contentX && mx <= contentX + contentWidth && my >= top && my <= bottom) {
            // scroll by row height units
            int delta = (int) Math.round(verticalAmount * (getRowHeight() + getRowGap()));
            scrollOffset -= delta;
            clampScrollOffset();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void clampScrollOffset() {
        List<PhoneChatClient.GroupMemberData> entries = PhoneChatClient.getGroupMembersList(groupId);
        int visibleHeight = Math.max(1, getListBottom() - getListTop());
        int totalHeight = entries.size() == 0 ? 0 : entries.size() * getRowHeight() + Math.max(0, entries.size() - 1) * getRowGap();
        int maxScroll = Math.max(0, totalHeight - visibleHeight);
        if (scrollOffset < 0) scrollOffset = 0;
        else if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    @Override
    public void removed() {
        super.removed();
        if (stateListener != null) {
            PhoneChatClient.removeListener(stateListener);
            stateListener = null;
        }
    }
}
