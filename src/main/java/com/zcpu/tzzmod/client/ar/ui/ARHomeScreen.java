package com.zcpu.tzzmod.client.ar.ui;

import com.zcpu.tzzmod.client.ar.ui.app.*;
import com.zcpu.tzzmod.client.phone.PhoneAppEntry;
import com.zcpu.tzzmod.client.phone.PhoneAppRegistry;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.task.TaskClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * AR Home Screen: centered app grid, no lock screen, Apple Vision Pro style.
 * Opens directly when the AR headset is used.
 */
public class ARHomeScreen extends AbstractARScreen {
    private final List<AppSlot> appSlots = new ArrayList<>();

    public ARHomeScreen() {
        super(Text.translatable("ar.tzz_mod.home"), null);
    }

    @Override
    protected void init() {
        super.init();
        appSlots.clear();

        // Request bootstrap so admin visibility (isOp) is up-to-date
        if (client != null && client.getNetworkHandler() != null) {
            PhoneChatClient.requestWhoAmI();
            PhoneChatClient.requestBootstrap();
        }

        List<PhoneAppEntry> entries = PhoneAppRegistry.getAppEntries();
        if (entries.isEmpty()) return;

        // Horizontal layout: apps in a single row (or 2 rows if many)
        int iconSize = s(48);
        int spacingX = s(12);
        int spacingY = s(8);
        int maxCols = Math.max(1, (contentWidth + spacingX) / (iconSize + spacingX));
        int totalApps = entries.size();
        int cols = Math.min(totalApps, maxCols);
        int rows = (totalApps + cols - 1) / cols;

        int gridWidth = cols * iconSize + (cols - 1) * spacingX;
        int gridHeight = rows * (iconSize + s(14)) + (rows - 1) * spacingY;
        int startX = contentX + (contentWidth - gridWidth) / 2;
        int startY = contentY + s(28) + (contentHeight - s(28) - gridHeight) / 2;

        for (int i = 0; i < totalApps; i++) {
            PhoneAppEntry entry = entries.get(i);
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (iconSize + spacingX);
            int y = startY + row * (iconSize + s(14) + spacingY);
            AppSlot slot = new AppSlot(entry, x, y, iconSize, i);
            appSlots.add(slot);

            addARGhostButton(Text.empty(), x, y, iconSize, iconSize, button -> {
                if (client != null) {
                    Screen nextScreen = createARAppScreen(slot.entry, this);
                    if (nextScreen instanceof AbstractARScreen arScreen) {
                        arScreen.setAppLaunchAnimation(slot.x, slot.y, slot.size, slot.size);
                    }
                    client.setScreen(nextScreen);
                }
            });
        }
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // Title with angular tech background
        Text titleText = Text.translatable("ar.tzz_mod.home");
        int titleCX = contentX + contentWidth / 2;
        int titleY = contentY + s(8);
        int titleW = scaledTextWidth(titleText.getString());
        int titleH = scaledFontHeight();
        int tPadX = s(6);
        int tPadY = s(2);
        // Angular tech frame: cut-corner rectangle with strong theme accent border
        drawARPanelFrame(context,
                titleCX - titleW / 2 - tPadX, titleY - tPadY,
                titleW + tPadX * 2, titleH + tPadY * 2);
        drawScaledCenteredText(context, titleText, titleCX, titleY, themeAccent());

        int chatUnread = PhoneChatClient.getTotalUnreadCount();
        int taskUnread = TaskClient.getTotalUnreadCount();

        for (AppSlot slot : appSlots) {
            // --- 2D theme-aware icon rendering ---
            int centerX = slot.x + slot.size / 2;
            int centerY = slot.y + slot.size / 2;
            String themeFolder = isLightMode() ? "light" : "dark";
            Identifier themeIcon = Identifier.of(
                    com.zcpu.tzzmod.Tzz_mod.MOD_ID,
                    "textures/gui/phone/icons/" + themeFolder + "/" + slot.entry.id() + ".png");

            // Static icon rendering (no bobbing animation)
            // 4-line angular tech border around the icon slot
            int cut = Math.max(1, s(3));
            int borderColor = themeAccent();
            int slotX = slot.x;
            int slotY = slot.y;
            int slotS = slot.size;
            // Top-left diagonal + top edge
            for (int i = 0; i < cut; i++) {
                context.fill(slotX + cut - i, slotY + i, slotX + cut - i + 1, slotY + i + 1, borderColor);
            }
            context.fill(slotX + cut, slotY, slotX + slotS, slotY + 1, borderColor);
            // Bottom-right diagonal + bottom edge
            for (int i = 0; i < cut; i++) {
                context.fill(slotX + slotS - cut + i, slotY + slotS - 1 - i, slotX + slotS - cut + i + 1, slotY + slotS - i, borderColor);
            }
            context.fill(slotX, slotY + slotS - 1, slotX + slotS - cut, slotY + slotS, borderColor);

            Identifier iconToUse = hasResource(themeIcon) ? themeIcon : slot.entry.iconTexture();
            if (hasResource(iconToUse)) {
                int iconSz = Math.max(1, slot.size);
                context.drawTexturedQuad(iconToUse, slot.x, slot.y,
                        slot.x + iconSz, slot.y + iconSz, 0.0F, 1.0F, 0.0F, 1.0F);
            } else {
                drawScaledCenteredText(context, Text.literal("?"), centerX,
                        centerY - s(4), themeTextDim());
            }

            // Cooldown badge for call_admin
            if ("call_admin".equals(slot.entry.id()) && com.zcpu.tzzmod.client.phone.PhoneCallAdminClient.isCoolingDown()) {
                int badgeW = s(14);
                int badgeX = slot.x + slot.size - badgeW - s(2);
                int badgeY = slot.y + slot.size - badgeW - s(2);
                fillChamferedRect(context, badgeX, badgeY, badgeW, badgeW, s(3), 0xCC000000);
                int lockW = badgeW - s(6);
                int lockH = badgeW - s(8);
                int lx = badgeX + (badgeW - lockW) / 2;
                int ly = badgeY + (badgeW - lockH) / 2 + s(1);
                context.fill(lx, ly, lx + lockW, ly + lockH, 0xFFFFFFFF);
                context.fill(lx + s(1), ly - s(3), lx + lockW - s(1), ly, 0xFFFFFFFF);
            }

            // Unread badges
            if ("chat".equals(slot.entry.id()) && chatUnread > 0) {
                renderBadge(context, slot, chatUnread);
            }
            if ("task".equals(slot.entry.id()) && taskUnread > 0) {
                renderBadge(context, slot, taskUnread);
            }

            // App label — allow wider text to accommodate longer names (e.g. Chinese characters)
            String appName = slot.entry.name().getString();
            if (appName == null || appName.isEmpty()) appName = slot.entry.id();
            int availableLabelWidth = Math.max(s(10), Math.round((slot.size + s(24)) / getTextScale()));
            String shownName = textRenderer.trimToWidth(appName, availableLabelWidth);
            if (shownName == null || shownName.isEmpty()) shownName = appName;
            int labelY = slot.y + slot.size + s(4);
            int labelW = scaledTextWidth(shownName);
            int labelH = scaledFontHeight();
            int labelPadX = s(3);
            int labelPadY = s(1);
            // Angular tech-style frame behind text (sharp cut-corners)
            int frameX = slot.x + slot.size / 2 - labelW / 2 - labelPadX;
            int frameW = labelW + labelPadX * 2;
            int frameH = labelH + labelPadY * 2;
            drawARPanelFrame(context, frameX, labelY - labelPadY, frameW, frameH);
            drawScaledCenteredText(context, Text.literal(shownName),
                    slot.x + slot.size / 2, labelY, themeText());
        }
    }

    private void renderBadge(DrawContext context, AppSlot slot, int count) {
        int centerX = slot.x + slot.size - s(4);
        int centerY = slot.y + s(4);
        int radius = s(5);
        int badgeSize = radius * 2 + 1;
        fillChamferedRect(context, centerX - radius - 1, centerY - radius - 1, badgeSize + 2, badgeSize + 2,
                Math.max(1, radius / 2), 0xCC000000);
        fillChamferedRect(context, centerX - radius, centerY - radius, badgeSize, badgeSize,
                Math.max(1, radius / 2), isLightMode() ? 0xFF0088BB : 0xFF00B4A0);
        String badge = count > 99 ? "99+" : Integer.toString(count);
        int textWidth = scaledTextWidth(badge);
        drawScaledText(context, Text.literal(badge), centerX - textWidth / 2, centerY - s(3), 0xFFFFFFFF);
    }

    /**
     * Creates the appropriate AR-specific screen for the given phone app entry.
     * Each app has its own dedicated AR UI — phone screens are NOT reused.
     */
    private static Screen createARAppScreen(PhoneAppEntry entry, ARHomeScreen home) {
        return switch (entry.id()) {
            case "map" -> new ARMapScreen(home);
            case "chat" -> new ARChatScreen(home);
            case "task" -> new ARTaskScreen(home);
            case "call_admin" -> new ARCallAdminScreen(home);
            case "settings" -> new ARSettingsScreen(home);
            case "compass" -> new ARCompassScreen(home);
            case "admin" -> new ARAdminScreen(home);
            default -> entry.rootScreenFactory().apply(home);
        };
    }

    @Override
    public void removed() {
        super.removed();
    }

    @Override
    public void close() {
        // AR home screen closes directly (no parent)
        if (client != null) {
            client.setScreen(null);
        }
    }

    private record AppSlot(PhoneAppEntry entry, int x, int y, int size, int index) {}
}
