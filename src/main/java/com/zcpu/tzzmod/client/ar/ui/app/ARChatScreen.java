package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.phone.chat.ChatUiUtil;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.photo.GalleryAvatarRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * AR-specific chat screen. Shows conversation list in tech HUD style.
 */
public class ARChatScreen extends AbstractARScreen {
    private float scrollOffset;
    private final List<ChatRow> rows = new ArrayList<>();
    private int hoveredIndex = -1;

    public ARChatScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.chat"), parent);
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();
        boolean isSingleplayer = MinecraftClient.getInstance().isIntegratedServerRunning() || MinecraftClient.getInstance().getServer() != null;
        if (PhoneChatClient.isOp() || isSingleplayer) {
            int btnW = s(60);
            int btnH = s(18);
            int bx = contentX + contentWidth - btnW - s(2);
            int by = contentY + s(2);
            addARButton(Text.translatable("phone.tzz_mod.chat.create_group"),
                    bx, by, btnW, btnH,
                    ARButtonVariant.PRIMARY, () -> false,
                    btn -> {
                        if (client != null)
                            client.setScreen(new ARChatCreateGroupScreen(this));
                    });
        }
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();
        for (PhoneChatClient.ContactData c : PhoneChatClient.getContacts()) {
            int unread = PhoneChatClient.getUnreadCount("direct", c.uuid());
            rows.add(new ChatRow(c.name(), c.uuid(), unread, false, c.uuid()));
        }
        for (PhoneChatClient.GroupData g : PhoneChatClient.getGroups()) {
            int unread = PhoneChatClient.getUnreadCount("group", g.id());
            rows.add(new ChatRow(g.name(), g.id(), unread, true, null));
        }
    }

    @Override
    public void tick() {
        super.tick();
        rebuildRows();
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // Title
        Text titleText = Text.translatable("phone.tzz_mod.app.chat");
        int titleCX = contentX + contentWidth / 2;
        int titleY = contentY + s(2);
        drawScaledCenteredText(context, titleText, titleCX, titleY, themeAccent());

        int listTop = titleY + scaledFontHeight() + s(6);
        int listBottom = contentY + contentHeight;
        int rowH = scaledFontHeight() + s(8);

        hoveredIndex = -1;

        if (rows.isEmpty()) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.empty"),
                    titleCX, listTop + s(20), themeTextDim());
            return;
        }

        int totalH = rows.size() * rowH;
        int maxScroll = Math.max(0, totalH - (listBottom - listTop));
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        // Scissor clip
        context.enableScissor(contentX, listTop, contentX + contentWidth, listBottom);

        int y = listTop - Math.round(scrollOffset);
        for (int i = 0; i < rows.size(); i++) {
            ChatRow row = rows.get(i);
            if (y + rowH > listTop && y < listBottom) {
                boolean hovered = mouseX >= contentX && mouseX <= contentX + contentWidth
                        && mouseY >= Math.max(y, listTop) && mouseY < Math.min(y + rowH, listBottom);
                if (hovered) hoveredIndex = i;

                int fillColor = hovered ? (isLightMode() ? 0x44C0D4E8 : 0x4410283C)
                        : (isLightMode() ? 0x22D8E4F0 : 0x220A1A2C);
                int cut = Math.max(1, s(2));
                int borderColor = row.isGroup ? themeAccent() : (hovered ? themeBorderBright() : themeBorder());
                ChatUiUtil.drawAngularFrame(context, contentX + s(2), y + s(1),
                    contentWidth - s(4), rowH - s(2), cut, fillColor, borderColor);

                int textX = contentX + s(8);
                if (!row.isGroup && row.avatarUuid != null) {
                    GalleryAvatarRenderer.drawAvatar(context, row.avatarUuid, contentX + s(6), y + s(2), s(14), themeAccent());
                    textX += s(18);
                    drawScaledText(context, Text.literal(row.name), textX, y + s(4), themeText());
                } else {
                    String prefix = Text.translatable("phone.tzz_mod.chat.group_tag").getString();
                    drawScaledText(context, Text.literal(prefix), textX, y + s(4), themeAccent());
                    textX += Math.max(s(16), scaledTextWidth(prefix) + s(4));
                    drawScaledText(context, Text.literal(row.name), textX, y + s(4), themeText());
                }

                // Unread badge on right
                if (row.unread > 0) {
                    String badge = row.unread > 99 ? "99+" : String.valueOf(row.unread);
                    int badgeW = scaledTextWidth(badge) + s(6);
                    int badgeH = scaledFontHeight() + s(2);
                    int bx = contentX + contentWidth - s(8) - badgeW;
                    int by = y + (rowH - badgeH) / 2;
                    fillChamferedRect(context, bx, by, badgeW, badgeH,
                            Math.max(1, badgeH / 3), isLightMode() ? 0xFF0088BB : 0xFF00B4A0);
                    drawScaledCenteredText(context, Text.literal(badge),
                            bx + badgeW / 2, by + s(1), 0xFFFFFFFF);
                }
            }
            y += rowH;
        }

        context.disableScissor();

        // Scrollbar
        if (totalH > listBottom - listTop) {
            renderScrollbar(context, listTop, listBottom, totalH, Math.round(scrollOffset));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isHelpModeActive()) {
            return true;
        }
        scrollOffset -= (float) (verticalAmount * s(16));
        return true;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) return true;
        if (hoveredIndex >= 0 && hoveredIndex < rows.size()) {
            ChatRow row = rows.get(hoveredIndex);
            String type = row.isGroup ? "group" : "direct";
            String title = PhoneChatClient.getTitle(type, row.id);
            if (client != null)
                client.setScreen(new ARChatConversationScreen(this, type, row.id, title));
            return true;
        }
        return false;
    }

    private record ChatRow(String name, String id, int unread, boolean isGroup, String avatarUuid) {}
}
