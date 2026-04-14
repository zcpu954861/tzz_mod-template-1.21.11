package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class PhoneSettingsAppScreen extends AbstractPhoneScreen {
    private static final float TOGGLE_ANIM_SPEED = 6.0F;
    private static final long SCAN_DURATION_MS = 500L;

    private final List<ToggleRow> rows = new ArrayList<>();
    private double scrollOffset;
    private double targetScroll;

    // Theme scan animation (#1)
    private long scanStartMs = -1L;
    private boolean scanTargetLightMode = false;

    public PhoneSettingsAppScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.settings"), parent);
        PhoneSettingsClient.load();
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        rows.add(new ToggleRow(
            Text.translatable("phone.tzz_mod.settings.light_mode"),
            Text.translatable("phone.tzz_mod.settings.light_mode.subtitle"),
            PhoneSettingsClient::isLightModeEnabled,
            value -> {
                boolean newValue = Boolean.TRUE.equals(value);
                // Start scan animation (#1): apply setting but animate the transition
                PhoneSettingsClient.setLightModeEnabled(newValue);
                scanTargetLightMode = newValue;
                scanStartMs = System.currentTimeMillis();
            }
        ));
        rows.add(new ToggleRow(
            Text.translatable("phone.tzz_mod.settings.animations"),
            Text.translatable("phone.tzz_mod.settings.animations.subtitle"),
            PhoneSettingsClient::isAnimationsEnabled,
            value -> PhoneSettingsClient.setAnimationsEnabled(Boolean.TRUE.equals(value))
        ));
        rows.add(new ToggleRow(
                Text.translatable("phone.tzz_mod.settings.alert_mode"),
                Text.translatable("phone.tzz_mod.settings.alert_mode.subtitle"),
                PhoneSettingsClient::isAlertModeEnabled,
            value -> PhoneSettingsClient.setAlertModeEnabled(Boolean.TRUE.equals(value))
        ));
        rows.add(new ToggleRow(
                Text.translatable("phone.tzz_mod.settings.always_show_region_title"),
                Text.translatable("phone.tzz_mod.settings.always_show_region_title.subtitle"),
                PhoneSettingsClient::isAlwaysShowRegionTitleEnabled,
            value -> PhoneSettingsClient.setAlwaysShowRegionTitleEnabled(Boolean.TRUE.equals(value))
        ));

        // Pre-compute subtitle lines for each row
        int rowWidth = contentWidth - s(16);
        for (ToggleRow row : rows) {
            row.subtitleLines = wrap(row.subtitle.getString(), rowWidth - s(12));
        }

        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(70), s(20), button -> close());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Theme scan animation overlay (#1)
        if (scanStartMs > 0) {
            long elapsed = System.currentTimeMillis() - scanStartMs;
            float progress = Math.min(1.0f, elapsed / (float) SCAN_DURATION_MS);

            // Scan line sweeps from phoneY to phoneY+phoneHeight
            int scanY = phoneY + (int) ((phoneY + phoneHeight - phoneY) * progress);
            int scanColor = scanTargetLightMode ? 0xFF0099CC : 0xFF00FFE0;

            // Bright scan line (2px high)
            context.fill(phoneX, scanY - 1, phoneX + phoneWidth, scanY + 1, scanColor);
            // Faint glow above scan line (area that has "switched")
            int glowAlpha = 0x22;
            context.fill(phoneX, phoneY, phoneX + phoneWidth, scanY,
                    (glowAlpha << 24) | (scanColor & 0x00FFFFFF));

            if (progress >= 1.0f) {
                scanStartMs = -1L;
                // Rebuild the screen to apply new theme
                if (client != null) {
                    client.setScreen(new PhoneSettingsAppScreen(parent));
                }
            }
        }
    }

    private int getListTop() {
        return contentY + s(34);
    }

    private int getListBottom() {
        return contentY + contentHeight - s(30);
    }

    private int getRowHeight(ToggleRow row) {
        return s(22) + row.subtitleLines.size() * (scaledFontHeight() + s(2)) + s(8);
    }

    private int getTotalHeight() {
        int height = 0;
        for (ToggleRow row : rows) {
            height += getRowHeight(row) + s(6);
        }
        return Math.max(0, height - s(6));
    }

    private int getMaxScroll() {
        int visibleHeight = Math.max(1, getListBottom() - getListTop());
        return Math.max(0, getTotalHeight() - visibleHeight);
    }

    private void clampScroll() {
        double maxScroll = getMaxScroll();
        targetScroll = Math.max(0.0D, Math.min(targetScroll, maxScroll));
        scrollOffset = Math.max(0.0D, Math.min(scrollOffset, maxScroll));
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        scrollOffset += (targetScroll - scrollOffset) * 0.35D;
        clampScroll();

        // Tech themed title: centered with accent color
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.app.settings"),
                contentX + contentWidth / 2, contentY + s(8), themeAccent());
        boolean animateToggles = PhoneSettingsClient.isAnimationsEnabled();

        int rowX = contentX + s(8);
        int rowWidth = contentWidth - s(16);
        int currentY = getListTop() - (int) Math.round(scrollOffset);
        int listBottom = getListBottom();

        context.enableScissor(contentX, getListTop(), contentX + contentWidth, listBottom);
        for (ToggleRow row : rows) {
            int rowHeight = getRowHeight(row);
            if (currentY + rowHeight >= getListTop() && currentY <= listBottom) {
                row.target = row.getter.getAsBoolean();
                if (animateToggles) {
                    float direction = row.target ? 1.0F : -1.0F;
                    row.progress = clamp01(row.progress + direction * TOGGLE_ANIM_SPEED * delta);
                } else {
                    row.progress = row.target ? 1.0F : 0.0F;
                }

                // Row background + left accent bar (like admin screen, issue #1)
                boolean hovered = mouseX >= rowX && mouseX <= rowX + rowWidth
                        && mouseY >= currentY && mouseY <= currentY + rowHeight
                        && mouseY >= getListTop() && mouseY <= listBottom;
                int chamfer = Math.max(2, s(3));
                int rowBg = hovered
                        ? (isLightMode() ? 0x44D8E4F0 : 0x44101825)
                        : (isLightMode() ? 0x33D8E4F0 : 0x33101825);
                fillChamferedRect(context, rowX, currentY, rowWidth, rowHeight, chamfer, rowBg);
                context.fill(rowX, currentY + chamfer, rowX + 1, currentY + rowHeight - chamfer,
                        row.target ? themeAccent() : themeBorder());

                int labelY = currentY + Math.max(0, (s(22) - scaledFontHeight()) / 2);
                int labelColor = themeText();
                drawScaledText(context, row.label, rowX + s(6), labelY, labelColor);

                int switchW = s(28);
                int switchH = s(12);
                int switchX = rowX + rowWidth - switchW - s(6);
                int switchY = currentY + Math.max(0, (s(22) - switchH) / 2);

                // Angular tech toggle: 4-line style (#8), white knob (#7)
                int cut = Math.max(1, switchH / 3);
                int trackFill = lerpColor(isLightMode() ? 0x33C0C8D0 : 0x331A2A3C,
                        isLightMode() ? 0x330099CC : 0x3300FFE0, row.progress);
                fillChamferedRect(context, switchX, switchY, switchW, switchH, cut, trackFill);
                // 4-line border (angular tech frame via manual drawing)
                int borderCol = row.target ? themeAccent() : themeBorder();
                // top edge
                context.fill(switchX + cut, switchY, switchX + switchW, switchY + 1, borderCol);
                // bottom edge
                context.fill(switchX, switchY + switchH - 1, switchX + switchW - cut, switchY + switchH, borderCol);
                // top-left diagonal
                for (int di = 0; di < cut; di++) {
                    context.fill(switchX + cut - di, switchY + di, switchX + cut - di + 1, switchY + di + 1, borderCol);
                }
                // bottom-right diagonal
                for (int di = 0; di < cut; di++) {
                    context.fill(switchX + switchW - cut + di, switchY + switchH - 1 - di,
                            switchX + switchW - cut + di + 1, switchY + switchH - di, borderCol);
                }
                // White circle knob (#7 / #8)
                int knobSize = Math.max(4, switchH - s(4));
                int knobTravel = switchW - knobSize - s(4);
                int knobX = switchX + s(2) + Math.round(row.progress * knobTravel);
                int knobY = switchY + (switchH - knobSize) / 2;
                fillChamferedRect(context, knobX, knobY, knobSize, knobSize,
                        Math.max(1, knobSize / 2), 0xFFFFFFFF);

                int subtitleColor = themeTextDim();
                int subtitleY = currentY + s(22) + s(6);
                for (int index = 0; index < row.subtitleLines.size(); index++) {
                    drawScaledText(
                            context,
                            Text.literal(row.subtitleLines.get(index)),
                            rowX + s(6),
                            subtitleY + index * (scaledFontHeight() + s(2)),
                            subtitleColor
                    );
                }
            }
            currentY += rowHeight + s(6);
        }
        context.disableScissor();

        // Scrollbar
        int visibleHeight = Math.max(1, listBottom - getListTop());
        int totalH = getTotalHeight();
        if (totalH > visibleHeight) {
            int trackX = contentX + contentWidth - s(2);
            context.fill(trackX, getListTop(), trackX + 1, listBottom, 0x335F7489);
            int thumbHeight = Math.max(s(18), Math.round(visibleHeight * (visibleHeight / (float) totalH)));
            int maxThumbTravel = Math.max(1, visibleHeight - thumbHeight);
            int thumbY = getListTop() + Math.round(((float) scrollOffset / Math.max(1, totalH - visibleHeight)) * maxThumbTravel);
            context.fill(trackX - 1, thumbY, trackX + 2, thumbY + thumbHeight, 0xAACFE8F9);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        int rowX = contentX + s(8);
        int rowWidth = contentWidth - s(16);
        int currentY = getListTop() - (int) Math.round(scrollOffset);
        for (ToggleRow row : rows) {
            int rowHeight = getRowHeight(row);
            if (mouseX >= rowX && mouseX <= rowX + rowWidth && mouseY >= currentY && mouseY <= currentY + rowHeight
                    && mouseY >= getListTop() && mouseY <= getListBottom()) {
                toggleRow(row);
                return true;
            }
            currentY += rowHeight + s(6);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (mx >= contentX && mx <= contentX + contentWidth && my >= getListTop() && my <= getListBottom()) {
            targetScroll = Math.max(0.0D, Math.min(targetScroll - verticalAmount * s(12), getMaxScroll()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void toggleRow(ToggleRow row) {
        boolean next = !row.getter.getAsBoolean();
        row.setter.accept(next);
        row.target = next;
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            int charLength = Character.charCount(codePoint);
            current.appendCodePoint(codePoint);
            if (textRenderer.getWidth(current.toString()) > maxWidth) {
                current.setLength(current.length() - charLength);
                if (current.isEmpty()) {
                    current.appendCodePoint(codePoint);
                    index += charLength;
                }
                lines.add(current.toString());
                current = new StringBuilder();
                continue;
            }
            index += charLength;
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static int lerpColor(int start, int end, float progress) {
        float clamped = clamp01(progress);
        int sa = (start >> 24) & 0xFF;
        int sr = (start >> 16) & 0xFF;
        int sg = (start >> 8) & 0xFF;
        int sb = start & 0xFF;
        int ea = (end >> 24) & 0xFF;
        int er = (end >> 16) & 0xFF;
        int eg = (end >> 8) & 0xFF;
        int eb = end & 0xFF;
        int a = Math.round(sa + (ea - sa) * clamped);
        int r = Math.round(sr + (er - sr) * clamped);
        int g = Math.round(sg + (eg - sg) * clamped);
        int b = Math.round(sb + (eb - sb) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static final class ToggleRow {
        private final Text label;
        private final Text subtitle;
        private final BooleanSupplier getter;
        private final Consumer<Boolean> setter;
        private List<String> subtitleLines = List.of();
        private float progress;
        private boolean target;

        private ToggleRow(Text label, Text subtitle, BooleanSupplier getter, Consumer<Boolean> setter) {
            this.label = label;
            this.subtitle = subtitle;
            this.getter = getter;
            this.setter = setter;
        }
    }
}