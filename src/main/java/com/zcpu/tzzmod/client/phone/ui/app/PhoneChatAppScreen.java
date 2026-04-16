package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.chat.ChatUiUtil;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.phone.ui.TypingSubtitleAnimator;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import com.zcpu.tzzmod.client.photo.GalleryAvatarRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PhoneChatAppScreen extends AbstractPhoneScreen {
    private static final long STATIC_SUBTITLE_MS = 1_200L;

    private final List<RowEntry> rows = new ArrayList<>();
    private Runnable stateListener;
    private int conversationCount;
    private int scrollOffset;

    private TypingSubtitleAnimator subtitleAnimator;
    private Text staticSubtitle = Text.empty();
    private long staticSubtitleExpiresAtMs;
    private int lastTotalUnread = 0;

    public PhoneChatAppScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.chat"), parent);
    }

    @Override
    protected void init() {
        super.init();

        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(72), s(20), button -> close());

        addPhoneButton(Text.translatable("phone.tzz_mod.chat.refresh"), contentX + s(76), contentY + contentHeight - s(24), s(64), s(20), button -> PhoneChatClient.requestBootstrap());

        // Show the "Create Group" primary button if the client is OP or when running in singleplayer (integrated server).
        // Use a permissive check: either the integrated server is running or a server instance exists.
        boolean isSingleplayer = MinecraftClient.getInstance().isIntegratedServerRunning() || MinecraftClient.getInstance().getServer() != null;
        if (PhoneChatClient.isOp() || isSingleplayer) {
            addPhonePrimaryButton(Text.translatable("phone.tzz_mod.chat.create_group"), contentX + contentWidth - s(88), contentY + s(26), s(88), s(20), button -> client.setScreen(new PhoneChatCreateGroupScreen(this)));
        }

        stateListener = this::rebuildRows;
        PhoneChatClient.addListener(stateListener);
        lastTotalUnread = PhoneChatClient.getTotalUnreadCount();
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
            rows.add(new RowEntry("direct", contact.uuid(), contact.name(), contact.name(), contact.uuid()));
        }

        for (PhoneChatClient.GroupData group : PhoneChatClient.getGroups()) {
            rows.add(new RowEntry("group", group.id(), group.name(), group.name(), null));
        }

        conversationCount = rows.size();
        clampScrollOffset();

        // Check unread change to trigger subtitle
        int totalUnread = PhoneChatClient.getTotalUnreadCount();
        if (PhoneSettingsClient.isAlertModeEnabled() && totalUnread > lastTotalUnread) {
            showTransientSubtitle(Text.literal("你有新消息"));
        }
        lastTotalUnread = totalUnread;
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
            drawScaledCenteredText(context,
                    Text.translatable("phone.tzz_mod.chat.disabled"),
                    contentX + contentWidth / 2,
                    contentY + s(38),
                    isLightMode() ? 0xFFCC4444 : 0xFFFF9999);
            return;
        }

        renderTransientSubtitle(context, delta);

        if (conversationCount == 0) {
            // Ensure the empty-state hint is drawn below the "Create Group" button so it isn't obscured.
            int defaultY = contentY + s(38);
            int emptyY = defaultY;
            // Only apply extra offset if the Create Group button is actually shown (OP or singleplayer)
            if (PhoneChatClient.isOp() || MinecraftClient.getInstance().isIntegratedServerRunning() || MinecraftClient.getInstance().getServer() != null) {
                int createButtonBottom = contentY + s(26) + s(20); // button Y + button height
                emptyY = Math.max(defaultY, createButtonBottom + s(6));
            }
            drawScaledCenteredText(context,
                    Text.translatable("phone.tzz_mod.chat.empty"),
                    contentX + contentWidth / 2,
                    emptyY,
                    themeText());
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
            int fill = hovered
                    ? (isLightMode() ? 0x44DCE7F2 : 0x445A7A92)
                    : (isLightMode() ? 0x22D2DCE7 : 0x220B1420);
            int border = "group".equals(row.type) ? themeAccent() : themeBorder();
            int rowWidth = contentWidth - s(4);
            ChatUiUtil.drawAngularFrame(context, contentX, drawY, rowWidth, rowHeight, s(3), fill, border);

            int labelX = contentX + s(5);
            if ("direct".equals(row.type) && row.avatarUuid != null) {
                GalleryAvatarRenderer.drawAvatar(context, row.avatarUuid, contentX + s(3), drawY + s(2), s(12), themeAccent());
                labelX += s(16);
                drawScaledText(context, Text.literal(row.label), labelX, drawY + s(4), themeText());
            } else {
                String tag = Text.translatable("phone.tzz_mod.chat.group_tag").getString();
                drawScaledText(context, Text.literal(tag), labelX, drawY + s(4), themeAccent());
                labelX += Math.max(s(18), scaledTextWidth(tag) + s(4));
                drawScaledText(context, Text.literal(row.label), labelX, drawY + s(4), themeText());
            }

            int unread = PhoneChatClient.getUnreadCount(row.type, row.targetId);
            if (unread > 0) {
                renderUnreadBadge(context, drawY, unread);
            }
        }

        renderPhoneScrollbar(context, top, bottom,
                conversationCount <= 0 ? 0 : conversationCount * rowHeight + (conversationCount - 1) * gap,
                scrollOffset);
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
                client.setScreen(new PhoneChatConversationScreen(this, row.type, row.targetId, row.title));
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

    private void showTransientSubtitle(Text text) {
        if (PhoneSettingsClient.isAnimationsEnabled()) {
            subtitleAnimator = new TypingSubtitleAnimator(text.getString(), 14, ignored -> playCharSound());
            subtitleAnimator.start();
            staticSubtitle = Text.empty();
            staticSubtitleExpiresAtMs = 0L;
            return;
        }

        subtitleAnimator = null;
        staticSubtitle = text;
        staticSubtitleExpiresAtMs = System.currentTimeMillis() + STATIC_SUBTITLE_MS;
    }

    private void renderTransientSubtitle(DrawContext context, float delta) {
        if (subtitleAnimator != null) {
            subtitleAnimator.tick(delta);
            Text sub = subtitleAnimator.getRenderedText();
            if (!sub.getString().isEmpty()) {
                drawScaledCenteredText(context, sub, contentX + contentWidth / 2, contentY + s(30), isLightMode() ? 0xFF2A8A5A : 0xFFB8FFD4);
            }
            if (subtitleAnimator.isFinished()) {
                subtitleAnimator = null;
            }
            return;
        }

        if (staticSubtitle.getString().isEmpty()) {
            return;
        }

        if (System.currentTimeMillis() >= staticSubtitleExpiresAtMs) {
            staticSubtitle = Text.empty();
            staticSubtitleExpiresAtMs = 0L;
            return;
        }

        drawScaledCenteredText(context, staticSubtitle, contentX + contentWidth / 2, contentY + s(30), isLightMode() ? 0xFF2A8A5A : 0xFFB8FFD4);
    }

    private void playCharSound() {
        if (client == null || client.player == null) return;
        try {
            var player = client.player;
            if (player == null) {
                return;
            }
            player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 1.0F);
        } catch (Exception ignored) {}
    }

    private static final class RowEntry {
        private final String type;
        private final String targetId;
        private final String label;
        private final String title;
        private final String avatarUuid;

        private RowEntry(String type, String targetId, String label, String title, String avatarUuid) {
            this.type = type;
            this.targetId = targetId;
            this.label = label;
            this.title = title;
            this.avatarUuid = avatarUuid;
        }
    }
}
