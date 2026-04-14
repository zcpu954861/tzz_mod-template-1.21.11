package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ForcedHudClient;
import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.network.AdminModeC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * AR-specific admin screen. Shows admin toggles in tech HUD style.
 */
public class ARAdminScreen extends AbstractARScreen {
    private float scrollOffset;
    private final List<AdminToggleRow> toggleRows = new ArrayList<>();

    public ARAdminScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.admin"), parent);
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();
        toggleRows.clear();

        toggleRows.add(new AdminToggleRow(
                Text.translatable("phone.tzz_mod.admin.record_mode").getString(),
                Text.translatable("phone.tzz_mod.admin.record_mode.subtitle").getString(),
                ForcedHudClient::isForceShowHead,
                v -> {
                    ClientPlayNetworking.send(new AdminModeC2SPayload(v));
                    ForcedHudClient.setServerEnforcedHud(v);
                }, 0.0F));
        toggleRows.add(new AdminToggleRow(
                Text.translatable("phone.tzz_mod.admin.show_self_position").getString(),
                Text.translatable("phone.tzz_mod.admin.show_self_position.subtitle").getString(),
                () -> MapClient.getSettings().showSelfPosition(),
                v -> MapClient.setVisibility("show_self_position", v), 0.0F));
        toggleRows.add(new AdminToggleRow(
                Text.translatable("phone.tzz_mod.admin.show_markers").getString(),
                Text.translatable("phone.tzz_mod.admin.show_markers.subtitle").getString(),
                () -> MapClient.getSettings().showMarkers(),
                v -> MapClient.setVisibility("show_markers", v), 0.0F));
        toggleRows.add(new AdminToggleRow(
                Text.translatable("phone.tzz_mod.admin.show_other_players").getString(),
                Text.translatable("phone.tzz_mod.admin.show_other_players.subtitle").getString(),
                () -> MapClient.getSettings().showOtherPlayers(),
                v -> MapClient.setVisibility("show_other_players", v), 0.0F));
        toggleRows.add(new AdminToggleRow(
                Text.translatable("phone.tzz_mod.admin.show_region_titles").getString(),
                Text.translatable("phone.tzz_mod.admin.show_region_titles.subtitle").getString(),
                () -> MapClient.getSettings().showRegionTitles(),
                v -> MapClient.setVisibility("show_region_titles", v), 0.0F));

        MapClient.requestState();

        for (AdminToggleRow row : toggleRows) {
            row.progress = row.getter.getAsBoolean() ? 1.0F : 0.0F;
        }
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // Title
        int titleCX = contentX + contentWidth / 2;
        int titleY = contentY + s(2);
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.app.admin"),
                titleCX, titleY, themeAccent());

        int listTop = titleY + scaledFontHeight() + s(6);
        int listBottom = contentY + contentHeight;

        int labelH = scaledFontHeight();
        int toggleW = s(22);
        int toggleH = s(10);
        int rowPadY = s(4);

        int totalH = 0;
        for (AdminToggleRow row : toggleRows) {
            List<String> descLines = wrapText(row.description, contentWidth - s(40));
            int rowH = labelH + descLines.size() * (labelH + s(1)) + rowPadY * 2 + s(4);
            totalH += rowH;
        }

        int visibleH = listBottom - listTop;
        int maxScroll = Math.max(0, totalH - visibleH);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        context.enableScissor(contentX, listTop, contentX + contentWidth, listBottom);
        int y = listTop - Math.round(scrollOffset);

        for (AdminToggleRow row : toggleRows) {
            List<String> descLines = wrapText(row.description, contentWidth - s(40));
            int rowH = labelH + descLines.size() * (labelH + s(1)) + rowPadY * 2 + s(4);

            if (y + rowH > listTop && y < listBottom) {
                boolean hovered = mouseX >= contentX && mouseX <= contentX + contentWidth
                        && mouseY >= Math.max(y, listTop) && mouseY < Math.min(y + rowH, listBottom);

                int fillColor = hovered ? (isLightMode() ? 0x44C0D4E8 : 0x4410283C)
                        : (isLightMode() ? 0x22D8E4F0 : 0x220A1A2C);

                // Left accent line
                boolean on = row.getter.getAsBoolean();
                int accentLine = on ? themeAccent() : themeTextDim();
                context.fill(contentX + s(2), y + s(1), contentX + s(4), y + rowH - s(1), accentLine);

                drawAngularTechFrame(context, contentX + s(5), y,
                        contentWidth - s(7), rowH, Math.max(1, s(2)), fillColor, themeBorder());

                drawScaledText(context, Text.literal(row.label),
                        contentX + s(12), y + rowPadY, themeText());

                int descY = y + rowPadY + labelH + s(2);
                for (String dLine : descLines) {
                    drawScaledText(context, Text.literal(dLine),
                            contentX + s(12), descY, themeTextDim());
                    descY += labelH + s(1);
                }

                // Toggle switch — 4-line angular tech style (issue #4)
                float target = on ? 1.0F : 0.0F;
                row.progress = approach(row.progress, target, delta * 8.0F);

                int tX = contentX + contentWidth - s(10) - toggleW;
                int tY = y + (rowH - toggleH) / 2;
                int cut = Math.max(1, toggleH / 3);
                int trackFill = lerpColor(
                        isLightMode() ? 0x33C0C8D0 : 0x331A2A3C,
                        isLightMode() ? 0x330099CC : 0x3300FFE0,
                        row.progress);
                fillChamferedRect(context, tX, tY, toggleW, toggleH, cut, trackFill);
                // 4-line angular border
                int borderCol = on ? themeAccent() : themeBorder();
                context.fill(tX + cut, tY, tX + toggleW, tY + 1, borderCol);
                context.fill(tX, tY + toggleH - 1, tX + toggleW - cut, tY + toggleH, borderCol);
                for (int di = 0; di < cut; di++) {
                    context.fill(tX + cut - di, tY + di, tX + cut - di + 1, tY + di + 1, borderCol);
                }
                for (int di = 0; di < cut; di++) {
                    context.fill(tX + toggleW - cut + di, tY + toggleH - 1 - di,
                            tX + toggleW - cut + di + 1, tY + toggleH - di, borderCol);
                }
                // White circle knob (same style as PhoneSettingsAppScreen, issue #1)
                int knobSize = toggleH - s(2);
                int knobX = tX + s(1) + Math.round(row.progress * (toggleW - knobSize - s(2)));
                int knobY = tY + s(1);
                fillChamferedRect(context, knobX, knobY, knobSize, knobSize,
                        Math.max(1, knobSize / 2), 0xFFFFFFFF);
            }
            y += rowH;
        }
        context.disableScissor();

        if (totalH > visibleH) {
            renderScrollbar(context, listTop, listBottom, totalH, Math.round(scrollOffset));
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) return true;

        double mouseX = click.x();
        double mouseY = click.y();
        int listTop = contentY + scaledFontHeight() + s(8);
        int y = listTop - Math.round(scrollOffset);
        int labelH = scaledFontHeight();
        int rowPadY = s(4);

        for (AdminToggleRow row : toggleRows) {
            List<String> descLines = wrapText(row.description, contentWidth - s(40));
            int rowH = labelH + descLines.size() * (labelH + s(1)) + rowPadY * 2 + s(4);
            if (mouseX >= contentX && mouseX <= contentX + contentWidth
                    && mouseY >= y && mouseY < y + rowH) {
                boolean current = row.getter.getAsBoolean();
                row.setter.accept(!current);
                return true;
            }
            y += rowH;
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

    private static class AdminToggleRow {
        final String label;
        final String description;
        final BooleanSupplier getter;
        final Consumer<Boolean> setter;
        float progress;

        AdminToggleRow(String label, String description, BooleanSupplier getter, Consumer<Boolean> setter, float progress) {
            this.label = label;
            this.description = description;
            this.getter = getter;
            this.setter = setter;
            this.progress = progress;
        }
    }
}
