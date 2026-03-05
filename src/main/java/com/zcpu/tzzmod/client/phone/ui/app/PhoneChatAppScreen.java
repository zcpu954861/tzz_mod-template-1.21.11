package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PhoneChatAppScreen extends AbstractPhoneScreen {
    private final List<RowEntry> rows = new ArrayList<>();
    private Runnable stateListener;
    private int conversationCount;
    private int scrollOffset;

    public PhoneChatAppScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.chat"), parent);
    }

    @Override
    protected void init() {
        super.init();

        addDrawableChild(ButtonWidget.builder(Text.translatable("phone.tzz_mod.back"), button -> close())
                .dimensions(contentX, contentY + contentHeight - s(24), s(72), s(20))
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("phone.tzz_mod.chat.refresh"), button -> PhoneChatClient.requestBootstrap())
                .dimensions(contentX + s(76), contentY + contentHeight - s(24), s(64), s(20))
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("phone.tzz_mod.chat.create_group"),
                        button -> client.setScreen(new PhoneChatCreateGroupScreen(this)))
                .dimensions(contentX + contentWidth - s(88), contentY + s(26), s(88), s(20))
                .build());

        stateListener = this::rebuildRows;
        PhoneChatClient.addListener(stateListener);
        rebuildRows();
        PhoneChatClient.requestBootstrap();
    }

    private int getListTop() {
        return contentY + s(50);
    }

    private int getListBottom() {
        return contentY + contentHeight - s(44);
    }

    private int getRowHeight() {
        return s(16);
    }

    private int getRowGap() {
        return s(3);
    }

    private void rebuildRows() {
        rows.clear();

        for (PhoneChatClient.ContactData contact : PhoneChatClient.getContacts()) {
            rows.add(new RowEntry("direct", contact.uuid(), contact.name()));
        }

        for (PhoneChatClient.GroupData group : PhoneChatClient.getGroups()) {
            rows.add(new RowEntry("group", group.id(), "# " + group.name()));
        }

        conversationCount = rows.size();
        clampScrollOffset();
    }

    private void clampScrollOffset() {
        int maxScroll = getMaxScroll();
        if (scrollOffset < 0) {
            scrollOffset = 0;
        } else if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }

    private int getMaxScroll() {
        int rowHeight = getRowHeight();
        int gap = getRowGap();
        int totalHeight = conversationCount <= 0
                ? 0
                : conversationCount * rowHeight + (conversationCount - 1) * gap;
        int visibleHeight = Math.max(1, getListBottom() - getListTop());
        return Math.max(0, totalHeight - visibleHeight);
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.chat"), contentX + contentWidth / 2, contentY + s(8));

        if (!PhoneChatClient.isEnabled()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("phone.tzz_mod.chat.disabled"),
                    contentX + contentWidth / 2,
                    contentY + s(38),
                    0xFFFF9999);
            return;
        }

        if (conversationCount == 0) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("phone.tzz_mod.chat.empty"),
                    contentX + contentWidth / 2,
                    contentY + s(38),
                    0xFFECECEC);
            return;
        }

        int top = getListTop();
        int bottom = getListBottom();
        int rowHeight = getRowHeight();
        int gap = getRowGap();

        for (int i = 0; i < rows.size(); i++) {
            RowEntry row = rows.get(i);
            int drawY = top + i * (rowHeight + gap) - scrollOffset;
            if (drawY + rowHeight < top || drawY > bottom) {
                continue;
            }

            boolean hovered = mouseX >= contentX && mouseX <= contentX + contentWidth
                    && mouseY >= drawY && mouseY <= drawY + rowHeight;
            int color = hovered ? 0x66FFFFFF : 0x22333333;
            context.fill(contentX, drawY, contentX + contentWidth, drawY + rowHeight, color);
            context.drawTextWithShadow(textRenderer, Text.literal(row.label), contentX + s(6), drawY + s(4), 0xFFECECEC);

            int unread = PhoneChatClient.getUnreadCount(row.type, row.targetId);
            if (unread > 0) {
                renderUnreadBadge(context, drawY, unread);
            }
        }
    }

    private void renderUnreadBadge(DrawContext context, int drawY, int unreadCount) {
        int centerX = contentX + contentWidth - s(12);
        int centerY = drawY + s(8);
        int radius = s(5);

        // Dark outline keeps the badge visible on both bright and dark row backgrounds.
        drawCircle(context, centerX, centerY, radius + 1, 0xCC000000);
        drawCircle(context, centerX, centerY, radius, 0xFFE64545);

        if (unreadCount > 1) {
            String text = unreadCount > 99 ? "99+" : Integer.toString(unreadCount);
            int textWidth = textRenderer.getWidth(text);
            context.drawTextWithShadow(textRenderer, text, centerX - textWidth / 2, centerY - s(3), 0xFFFFFFFF);
        }
    }

    private void drawCircle(DrawContext context, int centerX, int centerY, int radius, int color) {
        int squared = radius * radius;
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x * x + y * y <= squared) {
                    context.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        // Let widgets handle clicks first so buttons are always clickable.
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }

        int mx = (int) click.x();
        int my = (int) click.y();
        int top = getListTop();
        int bottom = getListBottom();
        int rowHeight = getRowHeight();
        int gap = getRowGap();

        if (mx < contentX || mx > contentX + contentWidth || my < top || my > bottom) {
            return false;
        }

        for (int i = 0; i < rows.size(); i++) {
            RowEntry row = rows.get(i);
            int drawY = top + i * (rowHeight + gap) - scrollOffset;
            if (my >= drawY && my <= drawY + rowHeight && client != null) {
                client.setScreen(new PhoneChatConversationScreen(this, row.type, row.targetId, row.label));
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
            scrollOffset -= (int) Math.round(verticalAmount * s(10));
            clampScrollOffset();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void removed() {
        super.removed();
        if (stateListener != null) {
            PhoneChatClient.removeListener(stateListener);
            stateListener = null;
        }
    }

    private static final class RowEntry {
        private final String type;
        private final String targetId;
        private final String label;

        private RowEntry(String type, String targetId, String label) {
            this.type = type;
            this.targetId = targetId;
            this.label = label;
        }
    }
}
