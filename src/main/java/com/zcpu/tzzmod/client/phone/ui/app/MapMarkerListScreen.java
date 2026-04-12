package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

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
        return contentY + s(30);
    }

    private int getListBottom() {
        return contentY + contentHeight - s(30);
    }

    private int getRowHeight() {
        return s(22);
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
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        scrollOffset += (targetScroll - scrollOffset) * 0.35D;
        clampScroll();
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.marker.title"), contentX + contentWidth / 2, contentY + s(8));

        if (rows.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.marker.empty"), contentX + contentWidth / 2, contentY + s(42), 0xFFECECEC);
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
            context.fill(contentX + s(4), drawY + s(5), contentX + s(12), drawY + s(13), marker.color() | 0xFF000000);

            String pos = marker.x() + ", " + marker.y() + ", " + marker.z();
            context.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(marker.name(), contentWidth - s(22))), contentX + s(16), drawY + s(3), 0xFFECECEC);
            context.drawTextWithShadow(textRenderer, Text.literal(pos), contentX + s(16), drawY + s(12), 0xFFB7C7D8);
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
                client.setScreen(new MapMarkerDetailScreen(this, rows.get(index).id()));
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
}
