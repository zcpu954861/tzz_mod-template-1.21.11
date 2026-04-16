package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * AR-specific settings screen. Shows toggle rows in tech HUD style.
 */
public class ARSettingsScreen extends AbstractARScreen {
    private float scrollOffset;
    private final List<ToggleRow> toggleRows = new ArrayList<>();

    public ARSettingsScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.settings"), parent);
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();
        toggleRows.clear();

        toggleRows.add(new ToggleRow(
                Text.translatable("phone.tzz_mod.settings.light_mode").getString(),
                Text.translatable("phone.tzz_mod.settings.light_mode.subtitle").getString(),
                PhoneSettingsClient::isLightModeEnabled,
                v -> PhoneSettingsClient.setLightModeEnabled(v), 0.0F));
        toggleRows.add(new ToggleRow(
                Text.translatable("phone.tzz_mod.settings.animations").getString(),
                Text.translatable("phone.tzz_mod.settings.animations.subtitle").getString(),
                PhoneSettingsClient::isAnimationsEnabled,
                v -> PhoneSettingsClient.setAnimationsEnabled(v), 0.0F));
        toggleRows.add(new ToggleRow(
            Text.translatable("phone.tzz_mod.settings.gallery_performance_mode").getString(),
            Text.translatable("phone.tzz_mod.settings.gallery_performance_mode.subtitle").getString(),
            PhoneSettingsClient::isGalleryPerformanceModeEnabled,
            v -> PhoneSettingsClient.setGalleryPerformanceModeEnabled(v), 0.0F));
        toggleRows.add(new ToggleRow(
                Text.translatable("phone.tzz_mod.settings.alert_mode").getString(),
                Text.translatable("phone.tzz_mod.settings.alert_mode.subtitle").getString(),
                PhoneSettingsClient::isAlertModeEnabled,
                v -> PhoneSettingsClient.setAlertModeEnabled(v), 0.0F));
        toggleRows.add(new ToggleRow(
                Text.translatable("phone.tzz_mod.settings.always_show_region_title").getString(),
                Text.translatable("phone.tzz_mod.settings.always_show_region_title.subtitle").getString(),
                PhoneSettingsClient::isAlwaysShowRegionTitleEnabled,
                v -> PhoneSettingsClient.setAlwaysShowRegionTitleEnabled(v), 0.0F));
        toggleRows.add(new ToggleRow(
                Text.translatable("phone.tzz_mod.settings.ar_mask").getString(),
                Text.translatable("phone.tzz_mod.settings.ar_mask.subtitle").getString(),
                PhoneSettingsClient::isARMaskEnabled,
                v -> PhoneSettingsClient.setARMaskEnabled(v), 0.0F));

        // Initialize toggle progress
        for (ToggleRow row : toggleRows) {
            row.progress = row.getter.getAsBoolean() ? 1.0F : 0.0F;
        }
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // Title
        int titleCX = contentX + contentWidth / 2;
        int titleY = contentY + s(2);
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.app.settings"),
                titleCX, titleY, themeAccent());

        int listTop = titleY + scaledFontHeight() + s(8);
        int listBottom = contentY + contentHeight;

        // Compute row heights
        int labelH = scaledFontHeight();
        int rowPadY = s(5);
        int rowGap = s(6); // spacing between rows (#6)
        int toggleW = s(28);
        int toggleH = s(12);

        int totalH = 0;
        for (ToggleRow row : toggleRows) {
            List<String> descLines = wrapText(row.description, contentWidth - s(50));
            int rowH = labelH + descLines.size() * (labelH + s(1)) + rowPadY * 2 + s(4);
            totalH += rowH + rowGap;
        }
        totalH = Math.max(0, totalH - rowGap);

        int visibleH = listBottom - listTop;
        int maxScroll = Math.max(0, totalH - visibleH);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        context.enableScissor(contentX, listTop, contentX + contentWidth, listBottom);
        int y = listTop - Math.round(scrollOffset);

        for (ToggleRow row : toggleRows) {
            List<String> descLines = wrapText(row.description, contentWidth - s(50));
            int rowH = labelH + descLines.size() * (labelH + s(1)) + rowPadY * 2 + s(4);

            if (y + rowH > listTop && y < listBottom) {
                boolean hovered = mouseX >= contentX && mouseX <= contentX + contentWidth
                        && mouseY >= Math.max(y, listTop) && mouseY < Math.min(y + rowH, listBottom);
                boolean on = row.getter.getAsBoolean();

                // Row background: chamfered rect (admin-style #6)
                int chamfer = Math.max(2, s(3));
                int rowBg = hovered ? (isLightMode() ? 0x44D8E4F0 : 0x44101825)
                                    : (isLightMode() ? 0x33D8E4F0 : 0x33101825);
                fillChamferedRect(context, contentX + s(2), y, contentWidth - s(4), rowH, chamfer, rowBg);
                // Left accent line
                context.fill(contentX + s(2), y + chamfer, contentX + s(3), y + rowH - chamfer,
                        on ? themeAccent() : themeBorder());

                // Label
                drawScaledText(context, Text.literal(row.label),
                        contentX + s(8), y + rowPadY, themeText());

                // Description (subtitle lines)
                int descY = y + rowPadY + labelH + s(2);
                for (String dLine : descLines) {
                    drawScaledText(context, Text.literal(dLine),
                            contentX + s(8), descY, themeTextDim());
                    descY += labelH + s(1);
                }

                // Toggle switch: 4-line angular style (#8)
                float target = on ? 1.0F : 0.0F;
                row.progress = approach(row.progress, target, delta * 8.0F);

                int tX = contentX + contentWidth - s(8) - toggleW;
                int tY = y + (rowH - toggleH) / 2;
                drawAngularToggle(context, tX, tY, toggleW, toggleH, row.progress, on);
            }
            y += rowH + rowGap;
        }
        context.disableScissor();

        if (totalH > visibleH) {
            renderScrollbar(context, listTop, listBottom, totalH, Math.round(scrollOffset));
        }
    }

    /**
     * Draws a tech-style toggle using 4-line angular frame (like APP border) with white knob (#8).
     */
    private void drawAngularToggle(DrawContext context, int x, int y, int w, int h, float progress, boolean on) {
        int cut = Math.max(1, h / 3);
        // Track fill (subtle)
        int trackFill = isLightMode()
                ? lerpColor(0x33C0C8D0, 0x330099CC, progress)
                : lerpColor(0x331A2A3C, 0x3300FFE0, progress);
        // Draw track using angular tech frame
        drawAngularTechFrame(context, x, y, w, h, cut, trackFill,
                on ? themeAccent() : themeBorder());
        // White circle knob (#7 / #8)
        int knobSize = Math.max(4, h - s(4));
        int knobTravel = w - knobSize - s(4);
        int knobX = x + s(2) + Math.round(progress * knobTravel);
        int knobY = y + (h - knobSize) / 2;
        int knobR = Math.max(1, knobSize / 2);
        fillChamferedRect(context, knobX, knobY, knobSize, knobSize, knobR, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) return true;

        double mouseX = click.x();
        double mouseY = click.y();
        int titleY = contentY + s(2);
        int listTop = titleY + scaledFontHeight() + s(8);
        int y = listTop - Math.round(scrollOffset);
        int labelH = scaledFontHeight();
        int rowPadY = s(5);
        int rowGap = s(6);

        for (ToggleRow row : toggleRows) {
            List<String> descLines = wrapText(row.description, contentWidth - s(50));
            int rowH = labelH + descLines.size() * (labelH + s(1)) + rowPadY * 2 + s(4);
            if (mouseX >= contentX && mouseX <= contentX + contentWidth
                    && mouseY >= y && mouseY < y + rowH) {
                boolean current = row.getter.getAsBoolean();
                row.setter.accept(!current);
                return true;
            }
            y += rowH + rowGap;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (float) (verticalAmount * s(16));
        return true;
    }

    private static float approach(float current, float target, float speed) {
        if (current < target) return Math.min(current + speed, target);
        if (current > target) return Math.max(current - speed, target);
        return current;
    }

    private static class ToggleRow {
        final String label;
        final String description;
        final BooleanSupplier getter;
        final Consumer<Boolean> setter;
        float progress;

        ToggleRow(String label, String description, BooleanSupplier getter, Consumer<Boolean> setter, float progress) {
            this.label = label;
            this.description = description;
            this.getter = getter;
            this.setter = setter;
            this.progress = progress;
        }
    }
}
