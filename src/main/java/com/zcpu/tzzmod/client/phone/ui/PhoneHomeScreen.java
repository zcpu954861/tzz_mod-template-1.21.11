package com.zcpu.tzzmod.client.phone.ui;

import com.zcpu.tzzmod.client.phone.PhoneAppEntry;
import com.zcpu.tzzmod.client.phone.PhoneAppRegistry;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.task.TaskClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PhoneHomeScreen extends AbstractPhoneScreen {
    private static final int MAX_COLUMNS = 4;
    private static final int MAX_ROWS = 6;
    private static final int MAX_APPS = MAX_COLUMNS * MAX_ROWS;

    private final List<AppSlot> appSlots = new ArrayList<>();

    // listener to refresh apps when server bootstrap/app_state arrives
    private Runnable bootstrapListener = null;
    // persistent listener while phone open to react to dynamic state changes (isOp/apps)
    private Runnable stateListener = null;
    // when true, init() should not request bootstrap (used by refresh to rebuild UI without network calls)
    private boolean suppressBootstrapOnInit = false;
    // debounce to avoid frequent UI rebuilds
    private volatile long lastRefreshMs = 0L;

    public PhoneHomeScreen() {
        super(Text.translatable("phone.tzz_mod.home"), null);
    }

    /**
     * Create a PhoneHomeScreen which can optionally skip requesting bootstrap during init.
     * This is used when we want to immediately rebuild the UI from cached state (no network round-trip).
     */
    public PhoneHomeScreen(boolean suppressBootstrap) {
        this();
        this.suppressBootstrapOnInit = suppressBootstrap;
    }

    @Override
    protected void init() {
        super.init();
        // If connected to a remote server, request bootstrap and wait for server response before building apps.
        // This ensures we use up-to-date OP status and app visibility (handles OP being revoked mid-session).
        appSlots.clear();
        // honor suppress flag
        if (suppressBootstrapOnInit) {
            suppressBootstrapOnInit = false;
            buildAppSlots();
            return;
        }
        try {
            if (client != null && client.getNetworkHandler() != null) {
                // request quick whoami to update OP status asap, and bootstrap for full app state
                PhoneChatClient.requestWhoAmI();
                PhoneChatClient.requestBootstrap();
                // register a one-time listener to refresh the app list when server responds
                if (bootstrapListener == null) {
                    bootstrapListener = () -> {
                        try {
                            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                            if (mc != null) {
                                mc.execute(() -> {
                                    if (mc.currentScreen instanceof PhoneHomeScreen) {
                                        ((PhoneHomeScreen) mc.currentScreen).onBootstrapArrived();
                                    }
                                });
                            }
                        } catch (Throwable ignored) {
                        }
                    };
                    PhoneChatClient.addListener(bootstrapListener);
                }
                // register a persistent state listener to react to any server-provided state changes
                if (stateListener == null) {
                    stateListener = () -> {
                        try {
                            long now = System.currentTimeMillis();
                            if (now - lastRefreshMs < 150L) {
                                return; // debounce: ignore too-frequent updates
                            }
                            lastRefreshMs = now;

                            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                            if (mc != null) {
                                mc.execute(() -> {
                                    if (mc.currentScreen instanceof PhoneHomeScreen) {
                                        ((PhoneHomeScreen) mc.currentScreen).refreshApps();
                                    }
                                });
                            }
                        } catch (Throwable ignored) {
                        }
                    };
                    PhoneChatClient.addListener(stateListener);
                }
                // do not build slots now; onBootstrapArrived() will call refreshApps()
            } else {
                // singleplayer or offline: build immediately
                buildAppSlots();
            }
        } catch (Throwable ignored) {
            buildAppSlots();
        }
    }

    private void onBootstrapArrived() {
        try {
            refreshApps();
        } finally {
            // remove listener after first use
            try {
                if (bootstrapListener != null) {
                    PhoneChatClient.removeListener(bootstrapListener);
                    bootstrapListener = null;
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void close() {
        super.close();
        // clean up listener to avoid leaking
        try {
            if (bootstrapListener != null) {
                PhoneChatClient.removeListener(bootstrapListener);
                bootstrapListener = null;
            }
            if (stateListener != null) {
                PhoneChatClient.removeListener(stateListener);
                stateListener = null;
            }
        } catch (Throwable ignored) {
        }
    }

    // Helper used to rebuild the displayed app slots (keeps same layout logic)
    private void refreshApps() {
        try {
            // Recreate the screen so all previous widget instances are removed and layout is rebuilt
            if (client != null) {
                client.setScreen(new PhoneHomeScreen(true));
            }
        } catch (Throwable ignored) {
        }
    }

    private void buildAppSlots() {
        List<PhoneAppEntry> entries = PhoneAppRegistry.getAppEntries();
        int visibleCount = Math.min(MAX_APPS, entries.size());

        int iconSize = Math.min(s(46), (contentWidth - s(24)) / MAX_COLUMNS);
        // 限制最大间隙为较小值（让图标更紧凑）
        int maxGap = s(6);
        int minIconSize = s(28);
        // 先计算如果使用最大间隙时可分配给图标的大小
        int availableForIcons = contentWidth - maxGap * (MAX_COLUMNS + 1);
        if (availableForIcons > 0) {
            iconSize = Math.min(iconSize, Math.max(minIconSize, availableForIcons / MAX_COLUMNS));
        } else {
            // 如果可用空间非常小，均匀分配
            iconSize = Math.max(1, contentWidth / MAX_COLUMNS - maxGap);
        }
        int spacingX = Math.max(1, Math.min(maxGap, (contentWidth - iconSize * MAX_COLUMNS) / (MAX_COLUMNS + 1)));
        // 当按 spacingX 可用后，可能会有剩余像素；把整体图标组在内容区居中
        int totalGroupWidth = iconSize * MAX_COLUMNS + spacingX * (MAX_COLUMNS + 1);
        int extraOffset = Math.max(0, (contentWidth - totalGroupWidth) / 2);
        // ensure labels under icons have space: compute minimal row height to fit icon + label
        int labelHeight = Math.max(1, scaledFontHeight());
        int rowHeight = Math.max(Math.min(s(58), (contentHeight - s(70)) / MAX_ROWS), iconSize + labelHeight + s(8));
        int startY = contentY + s(20); // bring icons a bit lower so title and first-row labels are visible

        for (int index = 0; index < visibleCount; index++) {
            PhoneAppEntry entry = entries.get(index);
            int col = index % MAX_COLUMNS;
            int row = index / MAX_COLUMNS;

            int x = contentX + spacingX + extraOffset + col * (iconSize + spacingX);
            int y = startY + row * rowHeight;
            AppSlot slot = new AppSlot(entry, x, y, iconSize);
            appSlots.add(slot);

            addPhoneGhostButton(Text.empty(), x, y, iconSize, iconSize, button -> {
                if (client != null) {
                    net.minecraft.client.gui.screen.Screen nextScreen = slot.entry.rootScreenFactory().apply(this);
                    if (nextScreen instanceof AbstractPhoneScreen) {
                        ((AbstractPhoneScreen) nextScreen).setAppLaunchAnimation(slot.x, slot.y, slot.size, slot.size);
                    }
                    client.setScreen(nextScreen);
                }
            });
        }
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        if (isExperimentalUi()) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.home"),
                    contentX + contentWidth / 2, contentY + s(8), 0xFF00FFE0);
        } else {
            drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.home"), contentX + contentWidth / 2, contentY + s(8));
        }

        int chatUnread = PhoneChatClient.getTotalUnreadCount();
        int taskUnread = TaskClient.getTotalUnreadCount();

        for (AppSlot slot : appSlots) {
            // Tech UI: draw a subtle thin border around each icon with a faint glow
            if (isExperimentalUi()) {
                int framePad = s(1);
                int frameX = slot.x - framePad;
                int frameY = slot.y - framePad;
                int frameW = slot.size + framePad * 2;
                int frameH = slot.size + framePad * 2;
                int frameChamfer = Math.max(2, s(2));
                // Outer subtle glow
                fillChamferedRect(context, frameX - 1, frameY - 1, frameW + 2, frameH + 2, frameChamfer + 1, 0x1800FFE0);
                // Thin border line
                fillChamferedRect(context, frameX, frameY, frameW, frameH, frameChamfer, 0x551A4A6C);
                fillChamferedRect(context, frameX + 1, frameY + 1, Math.max(1, frameW - 2), Math.max(1, frameH - 2),
                        Math.max(1, frameChamfer - 1), 0x00000000);
            }

            // Restore icon texture rendering (draw icon if available), otherwise show a text placeholder.
            if (hasResource(slot.entry.iconTexture())) {
                int iconPadding = s(0);
                int iconSize = Math.max(1, slot.size - iconPadding * 2);
                int x1 = slot.x + iconPadding;
                int y1 = slot.y + iconPadding;
                context.drawTexturedQuad(slot.entry.iconTexture(), x1, y1, x1 + iconSize, y1 + iconSize,
                        0.0F, 1.0F, 0.0F, 1.0F);
            } else {
                drawScaledCenteredText(context, Text.literal("?"), slot.x + slot.size / 2, slot.y + slot.size / 2 - s(4), 0xFF1A1A1A);
            }

            // Special overlay: if this is call_admin and it's cooling down, draw a small lock badge
            if ("call_admin".equals(slot.entry.id()) && com.zcpu.tzzmod.client.phone.PhoneCallAdminClient.isCoolingDown()) {
                int badgeW = s(14);
                int badgeX = slot.x + slot.size - badgeW - s(2);
                int badgeY = slot.y + slot.size - badgeW - s(2);
                // dark rounded background
                RoundedRectRenderer.fillRoundedRect(context, badgeX, badgeY, badgeW, badgeW, s(3), 0xCC000000);
                // draw a simple lock shape (rect + shackle)
                int lockW = badgeW - s(6);
                int lockH = badgeW - s(8);
                int lx = badgeX + (badgeW - lockW) / 2;
                int ly = badgeY + (badgeW - lockH) / 2 + s(1);
                context.fill(lx, ly, lx + lockW, ly + lockH, 0xFFFFFFFF);
                context.fill(lx + s(1), ly - s(3), lx + lockW - s(1), ly, 0xFFFFFFFF);
            }

            if ("chat".equals(slot.entry.id()) && chatUnread > 0) {
                renderChatBadge(context, slot, chatUnread);
            }

            if ("task".equals(slot.entry.id()) && taskUnread > 0) {
                renderChatBadge(context, slot, taskUnread);
            }

            String appName = slot.entry.name().getString();
            if (appName == null || appName.isEmpty()) {
                appName = slot.entry.id();
            }
            String shownName = textRenderer.trimToWidth(appName, Math.max(s(10), Math.round((slot.size + s(8)) / getTextScale())));
            if (shownName == null || shownName.isEmpty()) {
                shownName = appName == null || appName.isEmpty() ? slot.entry.id() : appName;
            }
            // Draw label text directly without a background so it appears over the game's blur.
            if (isExperimentalUi()) {
                drawScaledCenteredText(context, Text.literal(shownName), slot.x + slot.size / 2, slot.y + slot.size + s(4), 0xFFE0F7FF);
            } else {
                drawPhoneTextCenteredFixed(context, Text.literal(shownName), slot.x + slot.size / 2, slot.y + slot.size + s(4));
            }
        }
    }

    private void renderChatBadge(DrawContext context, AppSlot slot, int unreadCount) {
        int centerX = slot.x + slot.size - s(4);
        int centerY = slot.y + s(4);
        int radius = s(5);

        if (isExperimentalUi()) {
            // Tech-themed badge: chamfered diamond-ish shape
            int badgeSize = radius * 2 + 1;
            fillChamferedRect(context, centerX - radius - 1, centerY - radius - 1, badgeSize + 2, badgeSize + 2,
                    Math.max(1, radius / 2), 0xCC000000);
            fillChamferedRect(context, centerX - radius, centerY - radius, badgeSize, badgeSize,
                    Math.max(1, radius / 2), 0xFF00B4A0);
        } else {
            drawCircle(context, centerX, centerY, radius + 1, 0xCC000000);
            drawCircle(context, centerX, centerY, radius, 0xFFE64545);
        }

        String badge = unreadCount > 99 ? "99+" : Integer.toString(unreadCount);
        int textWidth = scaledTextWidth(badge);
        drawScaledText(context, Text.literal(badge), centerX - textWidth / 2, centerY - s(3), 0xFFFFFFFF);
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

    private record AppSlot(PhoneAppEntry entry, int x, int y, int size) {
    }
}
