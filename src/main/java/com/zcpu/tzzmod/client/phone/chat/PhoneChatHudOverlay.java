package com.zcpu.tzzmod.client.phone.chat;

import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

public final class PhoneChatHudOverlay {
    private PhoneChatHudOverlay() {
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (PhoneSettingsClient.isAlertModeEnabled()) {
            return;
        }
        if (client.options.hudHidden || client.player == null || client.currentScreen != null) {
            return;
        }

        long now = System.currentTimeMillis();
        long expiresAt = PhoneChatClient.getUnreadNotificationExpireAtMs();
        if (expiresAt <= now) {
            return;
        }

        List<PhoneChatClient.UnreadEntry> unreadEntries = PhoneChatClient.getUnreadEntries();
        if (unreadEntries.isEmpty()) {
            return;
        }

        int maxRows = 4;
        int hiddenRows = Math.max(0, unreadEntries.size() - maxRows);
        int shownRows = Math.min(maxRows, unreadEntries.size());

        String title = Text.translatable("phone.tzz_mod.chat.popup.title", unreadEntries.size()).getString();
        int width = Math.max(200, client.textRenderer.getWidth(title) + 22);

        for (int i = 0; i < shownRows; i++) {
            PhoneChatClient.UnreadEntry entry = unreadEntries.get(i);
            String line = entry.count() > 1
                    ? Text.translatable("phone.tzz_mod.chat.popup.line_count", entry.title(), entry.count()).getString()
                    : Text.translatable("phone.tzz_mod.chat.popup.line_single", entry.title()).getString();
            width = Math.max(width, client.textRenderer.getWidth(line) + 22);
        }

        if (hiddenRows > 0) {
            String more = Text.translatable("phone.tzz_mod.chat.popup.more", hiddenRows).getString();
            width = Math.max(width, client.textRenderer.getWidth(more) + 22);
        }

        int lineHeight = client.textRenderer.fontHeight + 4;
        int totalRows = 1 + shownRows + (hiddenRows > 0 ? 1 : 0);
        int height = 10 + totalRows * lineHeight;

        int x = context.getScaledWindowWidth() - width - 12;
        int y = context.getScaledWindowHeight() - height - 12;

        boolean lightMode = PhoneSettingsClient.isLightModeEnabled();
        // Tech-themed notification box: angular panel with accents
        int chamfer = 4;
        int bg = lightMode ? 0xE0E8EDF4 : 0xE00A0F1A;
        // Chamfered background
        context.fill(x + chamfer, y, x + width - chamfer, y + height, bg);
        context.fill(x, y + chamfer, x + width, y + height - chamfer, bg);
        for (int i = 0; i < chamfer; i++) {
            int offset = chamfer - i;
            context.fill(x + offset, y + i, x + width - offset, y + i + 1, bg);
            context.fill(x + offset, y + height - 1 - i, x + width - offset, y + height - i, bg);
        }
        // Top accent line
        context.fill(x + chamfer, y, x + width - chamfer, y + 1, lightMode ? 0xCC0099CC : 0xCC00FFE0);
        // Bottom border
        context.fill(x + chamfer, y + height - 1, x + width - chamfer, y + height, lightMode ? 0x88B0C0D0 : 0x881A4A6C);

        int drawY = y + 6;
        context.drawText(client.textRenderer, title, x + 10, drawY, lightMode ? 0xFF0099CC : 0xFF00FFE0, !lightMode);
        drawY += lineHeight;

        for (int i = 0; i < shownRows; i++) {
            PhoneChatClient.UnreadEntry entry = unreadEntries.get(i);
            String line = entry.count() > 1
                    ? Text.translatable("phone.tzz_mod.chat.popup.line_count", entry.title(), entry.count()).getString()
                    : Text.translatable("phone.tzz_mod.chat.popup.line_single", entry.title()).getString();
            context.drawText(client.textRenderer, line, x + 10, drawY, lightMode ? 0xFF1A2A3A : 0xFFE0F7FF, !lightMode);
            drawY += lineHeight;
        }

        if (hiddenRows > 0) {
            String more = Text.translatable("phone.tzz_mod.chat.popup.more", hiddenRows).getString();
            context.drawText(client.textRenderer, more, x + 10, drawY, lightMode ? 0xFF6A7A8A : 0xFF6B8A9E, !lightMode);
        }
    }
}

