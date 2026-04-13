package com.zcpu.tzzmod.client.task;

import com.zcpu.tzzmod.client.phone.ui.RoundedRectRenderer;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

public final class TaskHudOverlay {
    private TaskHudOverlay() {
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
        long expiresAt = TaskClient.getUnreadNotificationExpireAtMs();
        if (expiresAt <= now) {
            return;
        }

        List<TaskClient.UnreadEntry> unreadEntries = TaskClient.getUnreadEntries();
        if (unreadEntries.isEmpty()) {
            return;
        }

        int maxRows = 4;
        int hiddenRows = Math.max(0, unreadEntries.size() - maxRows);
        int shownRows = Math.min(maxRows, unreadEntries.size());

        String title = Text.translatable("phone.tzz_mod.task.popup.title", unreadEntries.size()).getString();
        int width = Math.max(200, client.textRenderer.getWidth(title) + 22);

        for (int i = 0; i < shownRows; i++) {
            TaskClient.UnreadEntry entry = unreadEntries.get(i);
            String line = entry.count() > 1
                    ? Text.translatable("phone.tzz_mod.task.popup.line_count", entry.title(), entry.count()).getString()
                    : Text.translatable("phone.tzz_mod.task.popup.line_single", entry.title()).getString();
            width = Math.max(width, client.textRenderer.getWidth(line) + 22);
        }

        if (hiddenRows > 0) {
            String more = Text.translatable("phone.tzz_mod.task.popup.more", hiddenRows).getString();
            width = Math.max(width, client.textRenderer.getWidth(more) + 22);
        }

        int lineHeight = client.textRenderer.fontHeight + 4;
        int totalRows = 1 + shownRows + (hiddenRows > 0 ? 1 : 0);
        int height = 10 + totalRows * lineHeight;

        int x = context.getScaledWindowWidth() - width - 12;
        int y = context.getScaledWindowHeight() - height - 12;

        boolean techUi = PhoneSettingsClient.isExperimentalUiEnabled();
        if (techUi) {
            int chamfer = 4;
            int bg = 0xE00A0F1A;
            context.fill(x + chamfer, y, x + width - chamfer, y + height, bg);
            context.fill(x, y + chamfer, x + width, y + height - chamfer, bg);
            for (int i = 0; i < chamfer; i++) {
                int offset = chamfer - i;
                context.fill(x + offset, y + i, x + width - offset, y + i + 1, bg);
                context.fill(x + offset, y + height - 1 - i, x + width - offset, y + height - i, bg);
            }
            context.fill(x + chamfer, y, x + width - chamfer, y + 1, 0xCC00FFE0);
            context.fill(x + chamfer, y + height - 1, x + width - chamfer, y + height, 0x881A4A6C);
        } else {
            RoundedRectRenderer.fillRoundedRect(context, x, y, width, height, 8, 0xD9181F2A);
            RoundedRectRenderer.fillRoundedRect(context, x + 1, y + 1, width - 2, 2, 1, 0xA0FFFFFF);
        }

        int drawY = y + 6;
        context.drawTextWithShadow(client.textRenderer, title, x + 10, drawY, techUi ? 0xFF00FFE0 : 0xFFF6F7FA);
        drawY += lineHeight;

        for (int i = 0; i < shownRows; i++) {
            TaskClient.UnreadEntry entry = unreadEntries.get(i);
            String line = entry.count() > 1
                    ? Text.translatable("phone.tzz_mod.task.popup.line_count", entry.title(), entry.count()).getString()
                    : Text.translatable("phone.tzz_mod.task.popup.line_single", entry.title()).getString();
            context.drawTextWithShadow(client.textRenderer, line, x + 10, drawY, techUi ? 0xFFE0F7FF : 0xFFE9EBF1);
            drawY += lineHeight;
        }

        if (hiddenRows > 0) {
            String more = Text.translatable("phone.tzz_mod.task.popup.more", hiddenRows).getString();
            context.drawTextWithShadow(client.textRenderer, more, x + 10, drawY, techUi ? 0xFF6B8A9E : 0xFFD3D8E6);
        }
    }
}
