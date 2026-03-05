package com.zcpu.tzzmod.client.phone.ui;

import com.zcpu.tzzmod.client.phone.PhoneAppEntry;
import com.zcpu.tzzmod.client.phone.PhoneAppRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PhoneHomeScreen extends AbstractPhoneScreen {
    private static final int MAX_COLUMNS = 4;
    private static final int MAX_ROWS = 6;
    private static final int MAX_APPS = MAX_COLUMNS * MAX_ROWS;

    private final List<AppSlot> appSlots = new ArrayList<>();

    public PhoneHomeScreen() {
        super(Text.translatable("phone.tzz_mod.home"), null);
    }

    @Override
    protected void init() {
        super.init();
        appSlots.clear();

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
        int startY = contentY + s(30);
        int rowHeight = Math.min(s(58), (contentHeight - s(70)) / MAX_ROWS);

        for (int index = 0; index < visibleCount; index++) {
            PhoneAppEntry entry = entries.get(index);
            int col = index % MAX_COLUMNS;
            int row = index / MAX_COLUMNS;

            int x = contentX + spacingX + extraOffset + col * (iconSize + spacingX);
            int y = startY + row * rowHeight;
            AppSlot slot = new AppSlot(entry, x, y, iconSize);
            appSlots.add(slot);

            ClickableWidget widget = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
                        if (client != null) {
                            client.setScreen(slot.entry.rootScreenFactory().apply(this));
                        }
                    })
                    .dimensions(x, y, iconSize, iconSize)
                    .build());
            widget.setAlpha(0.0F);
        }
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.home"), contentX + contentWidth / 2, contentY + s(8));

        for (AppSlot slot : appSlots) {
            // Restore icon texture rendering (draw icon if available), otherwise show a text placeholder.
            if (hasResource(slot.entry.iconTexture())) {
                int iconPadding = s(0);
                int iconSize = Math.max(1, slot.size - iconPadding * 2);
                int x1 = slot.x + iconPadding;
                int y1 = slot.y + iconPadding;
                context.drawTexturedQuad(slot.entry.iconTexture(), x1, y1, x1 + iconSize, y1 + iconSize,
                        0.0F, 1.0F, 0.0F, 1.0F);
            } else {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal("?"), slot.x + slot.size / 2, slot.y + slot.size / 2 - s(4), 0xFF1A1A1A);
            }

            String appName = slot.entry.name().getString();
            if (appName == null || appName.isEmpty()) {
                appName = slot.entry.id();
            }
            String shownName = textRenderer.trimToWidth(appName, Math.max(s(10), slot.size + s(8)));
            if (shownName == null || shownName.isEmpty()) {
                shownName = appName == null || appName.isEmpty() ? slot.entry.id() : appName;
            }
            // Draw label text directly without a background so it appears over the game's blur.
            drawPhoneTextCenteredFixed(context, Text.literal(shownName), slot.x + slot.size / 2, slot.y + slot.size + s(4));
        }
    }

    private record AppSlot(PhoneAppEntry entry, int x, int y, int size) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
        }
    }
}
