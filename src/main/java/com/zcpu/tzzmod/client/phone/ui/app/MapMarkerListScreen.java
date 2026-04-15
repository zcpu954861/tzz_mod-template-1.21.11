package com.zcpu.tzzmod.client.phone.ui.app;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.ArrayList;
import java.util.List;

public class MapMarkerListScreen extends AbstractPhoneScreen {
    private final List<MapClient.MapMarker> rows = new ArrayList<>();
    private Runnable stateListener;
    private double scrollOffset;
    private double targetScroll;

    public MapMarkerListScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.marker.title"), parent);
    }

    @Override
    protected void init() {
        super.init();
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(72), s(20), button -> close());
        addPhoneButton(Text.translatable("phone.tzz_mod.chat.refresh"), contentX + s(76), contentY + contentHeight - s(24), s(64), s(20), button -> MapClient.requestState());

        stateListener = this::rebuildRows;
        MapClient.addListener(stateListener);
        rebuildRows();
        MapClient.requestState();
    }

    private void rebuildRows() {
        rows.clear();
        rows.addAll(MapClient.getMarkers());
        clampScroll();
    }

    private int getListTop() {
        return contentY + s(36);
    }

    private int getListBottom() {
        return contentY + contentHeight - s(30);
    }

    private int getRowHeight() {
        return s(30);
    }

    private int getRowGap() {
        return s(4);
    }

    private int getMaxScroll() {
        int totalHeight = rows.isEmpty() ? 0 : rows.size() * getRowHeight() + (rows.size() - 1) * getRowGap();
        int visibleHeight = Math.max(1, getListBottom() - getListTop());
        return Math.max(0, totalHeight - visibleHeight);
    }

    private void clampScroll() {
        double maxScroll = getMaxScroll();
        targetScroll = Math.max(0.0D, Math.min(targetScroll, maxScroll));
        scrollOffset = Math.max(0.0D, Math.min(scrollOffset, maxScroll));
    }

    @Override
    protected boolean hasInitScanAnimation() {
        return true;
    }

    @Override
    protected boolean hasDefaultLaunchAnimation() {
        return true;
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        scrollOffset += (targetScroll - scrollOffset) * 0.35D;
        clampScroll();
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.marker.title"), contentX + contentWidth / 2, contentY + s(8));

        // Off-hand display toggle
        int offSwitchW = s(24);
        int offSwitchH = s(10);
        int offSwitchX = contentX + contentWidth - offSwitchW - s(2);
        int offSwitchY = contentY + s(21);
        boolean offHand = MapClient.isMarkerOffHandEnabled();
        {
            float prog = offHand ? 1.0F : 0.0F;
            int cut = Math.max(1, offSwitchH / 3);
            int trackFill = isLightMode()
                    ? (offHand ? 0x330099CC : 0x33C0C8D0)
                    : (offHand ? 0x3300FFE0 : 0x331A2A3C);
            fillChamferedRect(context, offSwitchX, offSwitchY, offSwitchW, offSwitchH, cut, trackFill);
            int borderCol = offHand ? themeAccent() : themeBorder();
            context.fill(offSwitchX + cut, offSwitchY, offSwitchX + offSwitchW, offSwitchY + 1, borderCol);
            context.fill(offSwitchX, offSwitchY + offSwitchH - 1, offSwitchX + offSwitchW - cut, offSwitchY + offSwitchH, borderCol);
            for (int d = 0; d < cut; d++) {
                context.fill(offSwitchX + cut - d, offSwitchY + d, offSwitchX + cut - d + 1, offSwitchY + d + 1, borderCol);
            }
            for (int d = 0; d < cut; d++) {
                context.fill(offSwitchX + offSwitchW - cut + d, offSwitchY + offSwitchH - 1 - d,
                        offSwitchX + offSwitchW - cut + d + 1, offSwitchY + offSwitchH - d, borderCol);
            }
            int knobSize = Math.max(3, offSwitchH - s(3));
            int knobTravel = Math.max(0, offSwitchW - knobSize - s(3));
            int knobX = offSwitchX + s(2) + Math.round(prog * knobTravel);
            int knobY = offSwitchY + (offSwitchH - knobSize) / 2;
            fillChamferedRect(context, knobX, knobY, knobSize, knobSize, Math.max(1, knobSize / 2), 0xFFFFFFFF);
        }
        context.drawText(textRenderer, Text.translatable("phone.tzz_mod.marker.off_hand"),
                contentX, offSwitchY + (offSwitchH - textRenderer.fontHeight) / 2,
                isLightMode() ? themeTextDim() : 0xFFB7C7D8, !isLightMode());

        if (rows.isEmpty()) {
            Text emptyText = Text.translatable("phone.tzz_mod.marker.empty");
            context.drawText(textRenderer, emptyText, contentX + contentWidth / 2 - textRenderer.getWidth(emptyText) / 2, contentY + s(42), isLightMode() ? themeText() : 0xFFECECEC, !isLightMode());
            return;
        }

        int top = getListTop();
        int bottom = getListBottom();
        int rowHeight = getRowHeight();
        int gap = getRowGap();
        int currentScroll = (int) Math.round(scrollOffset);

        context.enableScissor(contentX, top, contentX + contentWidth, bottom);
        for (int index = 0; index < rows.size(); index++) {
            MapClient.MapMarker marker = rows.get(index);
            int drawY = top + index * (rowHeight + gap) - currentScroll;
            if (drawY + rowHeight < top || drawY > bottom) {
                continue;
            }
            boolean hovered = mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= drawY && mouseY <= drawY + rowHeight;
            context.fill(contentX, drawY, contentX + contentWidth, drawY + rowHeight, hovered ? 0x33445D78 : 0x22333333);
            context.fill(contentX + s(6), drawY + s(7), contentX + s(12), drawY + s(23), marker.color() | 0xFF000000);

            int switchW = s(28);
            int switchH = s(12);
            int switchX = contentX + contentWidth - s(32);
            int switchY = drawY + (rowHeight - switchH) / 2;
            boolean visible = MapClient.isMarkerParticleEnabled(marker.id());
            float progress = visible ? 1.0F : 0.0F;
            int cut = Math.max(1, switchH / 3);
            int trackFill = isLightMode()
                    ? (visible ? 0x330099CC : 0x33C0C8D0)
                    : (visible ? 0x3300FFE0 : 0x331A2A3C);
            fillChamferedRect(context, switchX, switchY, switchW, switchH, cut, trackFill);
            int borderCol = visible ? themeAccent() : themeBorder();
            context.fill(switchX + cut, switchY, switchX + switchW, switchY + 1, borderCol);
            context.fill(switchX, switchY + switchH - 1, switchX + switchW - cut, switchY + switchH, borderCol);
            for (int d = 0; d < cut; d++) {
                context.fill(switchX + cut - d, switchY + d, switchX + cut - d + 1, switchY + d + 1, borderCol);
            }
            for (int d = 0; d < cut; d++) {
                context.fill(switchX + switchW - cut + d, switchY + switchH - 1 - d,
                        switchX + switchW - cut + d + 1, switchY + switchH - d, borderCol);
            }
            int knobSize = Math.max(4, switchH - s(4));
            int knobTravel = Math.max(0, switchW - knobSize - s(4));
            int knobX = switchX + s(2) + Math.round(progress * knobTravel);
            int knobY = switchY + (switchH - knobSize) / 2;
            fillChamferedRect(context, knobX, knobY, knobSize, knobSize, Math.max(1, knobSize / 2), 0xFFFFFFFF);

            String pos = "X: " + marker.x() + "  Z: " + marker.z();
            Text displayName = tryParseJsonText(marker.name());
            String trimmedName = textRenderer.trimToWidth(displayName, contentWidth - s(52)).getString();
            context.drawText(textRenderer, trimmedName.length() < displayName.getString().length() ? Text.literal(trimmedName + "...") : displayName, contentX + s(16), drawY + s(5), isLightMode() ? themeText() : 0xFFECECEC, !isLightMode());
            context.drawText(textRenderer, Text.literal(pos), contentX + s(16), drawY + s(17), isLightMode() ? themeTextDim() : 0xFFB7C7D8, !isLightMode());
        }
        context.disableScissor();

        renderScrollbar(context, top, bottom, rows.size() * rowHeight + Math.max(0, rows.size() - 1) * gap, currentScroll);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        int mx = (int) click.x();
        int my = (int) click.y();

        // Off-hand toggle click
        int offSwitchW = s(24);
        int offSwitchH = s(10);
        int offSwitchX = contentX + contentWidth - offSwitchW - s(2);
        int offSwitchY = contentY + s(21);
        if (mx >= offSwitchX && mx <= offSwitchX + offSwitchW && my >= offSwitchY && my <= offSwitchY + offSwitchH) {
            MapClient.setMarkerOffHandEnabled(!MapClient.isMarkerOffHandEnabled());
            return true;
        }

        int top = getListTop();
        int bottom = getListBottom();
        int rowHeight = getRowHeight();
        int gap = getRowGap();
        if (mx < contentX || mx > contentX + contentWidth || my < top || my > bottom) {
            return false;
        }
        int currentScroll = (int) Math.round(scrollOffset);
        for (int index = 0; index < rows.size(); index++) {
            int drawY = top + index * (rowHeight + gap) - currentScroll;
            if (my >= drawY && my <= drawY + rowHeight && client != null) {
                int switchW = s(28);
                int switchH = s(12);
                int switchX = contentX + contentWidth - s(32);
                int switchY = drawY + (rowHeight - switchH) / 2;
                MapClient.MapMarker marker = rows.get(index);
                if (mx >= switchX && mx <= switchX + switchW && my >= switchY && my <= switchY + switchH) {
                    MapClient.setMarkerParticleEnabled(marker.id(), !MapClient.isMarkerParticleEnabled(marker.id()));
                    return true;
                }
                MapMarkerDetailScreen detail = new MapMarkerDetailScreen(this, marker.id());
                detail.setAppLaunchAnimation(contentX, drawY, contentWidth, rowHeight);
                client.setScreen(detail);
                return true;
            }
        }
        return true;
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

    @Override
    public void removed() {
        super.removed();
        if (stateListener != null) {
            MapClient.removeListener(stateListener);
            stateListener = null;
        }
    }

    private void renderScrollbar(DrawContext context, int top, int bottom, int totalHeight, int currentScroll) {
        int visibleHeight = Math.max(1, bottom - top);
        if (totalHeight <= visibleHeight) {
            return;
        }
        int trackX = contentX + contentWidth - s(2);
        context.fill(trackX, top, trackX + 1, bottom, 0x335F7489);
        int thumbHeight = Math.max(s(18), Math.round(visibleHeight * (visibleHeight / (float) totalHeight)));
        int maxThumbTravel = Math.max(1, visibleHeight - thumbHeight);
        int thumbY = top + Math.round((currentScroll / (float) Math.max(1, totalHeight - visibleHeight)) * maxThumbTravel);
        context.fill(trackX - 1, thumbY, trackX + 2, thumbY + thumbHeight, 0xAACFE8F9);
    }

    private static Text tryParseJsonText(String raw) {
        if (raw == null || raw.isBlank()) return Text.literal(raw == null ? "" : raw);
        if (!raw.startsWith("{") && !raw.startsWith("[") && !raw.startsWith("\"")) {
            return Text.literal(raw);
        }
        try {
            var element = JsonParser.parseString(raw);
            var result = TextCodecs.CODEC.parse(JsonOps.INSTANCE, element);
            Text parsed = result.result().orElse(null);
            if (parsed != null) return parsed;
        } catch (Exception ignored) {}
        return Text.literal(raw);
    }
}
