package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PhoneChatManageMembersScreen extends AbstractPhoneScreen {
    private static final int FACE_U = 8, FACE_V = 8, HAT_U = 40, HAT_V = 8, SKIN_TEXTURE_SIZE = 64;
    private final String groupId;
    private Runnable stateListener;

    // local selection cache: uuid -> isMember
    private final Map<String, Boolean> selection = new HashMap<>();
    private final Map<String, Identifier> skinTextureCache = new HashMap<>();
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

    public boolean referencesGroup(String removedGroupId) {
        return groupId.equals(removedGroupId);
    }

    public Screen getChatHomeScreen() {
        if (parent instanceof PhoneChatConversationScreen conversationScreen) {
            return conversationScreen.getChatHomeScreen();
        }
        return parent;
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
        drawScaledText(context, Text.translatable("phone.tzz_mod.chat.members_hint"), contentX, contentY + s(28), themeTextDim());

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

            // draw player head avatar
            int avatarSize = Math.max(4, fieldHeight - s(4));
            int avatarX = contentX + s(4);
            int avatarY = y + (fieldHeight - avatarSize) / 2;
            drawPlayerHead(context, entry.uuid(), avatarX, avatarY, avatarSize);
            int nameStartX = avatarX + avatarSize + s(4);
            int nameMaxW = contentX + contentWidth - nameStartX - (boxPadding + boxSize + s(4));
            String displayName = textRenderer.trimToWidth(entry.name(), Math.max(1, nameMaxW));
            drawScaledText(context, Text.literal(displayName), nameStartX, y + Math.max(0, (fieldHeight - textRenderer.fontHeight) / 2), themeText());

            if (entry.uuid().equals(selfUuid)) {
                context.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF3CB371);
                context.drawText(textRenderer, Text.literal("✓"), boxX + (boxSize - textRenderer.getWidth("✓")) / 2, boxY + Math.max(0, (boxSize - textRenderer.fontHeight) / 2), 0xFFFFFFFF, false);
            } else if (isMember) {
                context.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF3CB371);
                context.drawText(textRenderer, Text.literal("✓"), boxX + (boxSize - textRenderer.getWidth("✓")) / 2, boxY + Math.max(0, (boxSize - textRenderer.fontHeight) / 2), 0xFFFFFFFF, false);
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

    private void drawPlayerHead(DrawContext context, String uuid, int x, int y, int size) {
        Identifier skin = getCachedSkin(uuid);
        context.fill(x, y, x + size, y + size, 0x44000000);
        if (skin == null) return;
        drawSkinRegion(context, skin, x, y, size, FACE_U, FACE_V);
        drawSkinRegion(context, skin, x, y, size, HAT_U, HAT_V);
    }

    private Identifier getCachedSkin(String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        Identifier cached = skinTextureCache.get(uuid);
        if (cached != null) return cached;
        Identifier resolved = resolveSkin(uuid);
        if (resolved != null) skinTextureCache.put(uuid, resolved);
        return resolved;
    }

    private Identifier resolveSkin(String uuid) {
        if (client == null || uuid == null || uuid.isBlank()) return null;
        try {
            var handler = client.getNetworkHandler();
            if (handler == null) return null;
            var player = client.player;
            if (player != null && uuid.equals(player.getUuidAsString())) {
                SkinTextures skin = player.getSkin();
                if (skin == null || skin.body() == null) return null;
                return skin.body().texturePath();
            }
            var entry = handler.getPlayerListEntry(UUID.fromString(uuid));
            if (entry == null) return null;
            SkinTextures skin = entry.getSkinTextures();
            if (skin == null || skin.body() == null) return null;
            return skin.body().texturePath();
        } catch (Exception e) { return null; }
    }

    private void drawSkinRegion(DrawContext context, Identifier texture, int x, int y, int size, int u, int v) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, (float) u, (float) v, size, size, 8, 8, SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE, -1);
    }
}
