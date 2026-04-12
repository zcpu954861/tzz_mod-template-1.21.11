package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class RegionPlannerListScreen extends AbstractPhoneScreen {
    private final List<RowEntry> rows = new ArrayList<>();
    private Runnable stateListener;
    private double scrollOffset;
    private double targetScroll;

    public RegionPlannerListScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.region.title"), parent);
    }

    @Override
    protected void init() {
        super.init();
        int bottomY = contentY + contentHeight - s(24);
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, bottomY, s(54), s(20), button -> close());
        addPhoneButton(Text.translatable("phone.tzz_mod.chat.refresh"), contentX + s(58), bottomY, s(54), s(20), button -> MapClient.requestState());
        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.region.clear_draft"), contentX + contentWidth - s(88), bottomY, s(88), s(20), button -> MapClient.clearPlannerDraft());

        stateListener = this::rebuildRows;
        MapClient.addListener(stateListener);
        rebuildRows();
        MapClient.requestState();
    }

    private void rebuildRows() {
        rows.clear();
        MapClient.PlannerDraft draft = MapClient.getPlannerDraft();
        if (!draft.isEmpty()) {
            rows.add(RowEntry.draft(
                    Text.translatable("phone.tzz_mod.region.draft_entry").getString(),
                    Text.translatable("phone.tzz_mod.region.draft_points", draft.points().size()).getString(),
                    draft.color()
            ));
        }
        for (MapClient.PlannerRegion region : MapClient.getPlannerRegions()) {
            rows.add(RowEntry.region(
                    region.id(),
                    region.name(),
                    Text.translatable("phone.tzz_mod.region.points_count", region.points().size()).getString(),
                    region.color()
            ));
        }
        clampScrollTargets();
    }

    private int getListTop() {
        return contentY + s(62);
    }

    private int getListBottom() {
        return contentY + contentHeight - s(32);
    }

    private int getRowHeight() {
        return s(30);
    }

    private int getRowGap() {
        return s(5);
    }

    private int getMaxScroll() {
        int totalHeight = rows.isEmpty() ? 0 : rows.size() * getRowHeight() + (rows.size() - 1) * getRowGap();
        int visibleHeight = Math.max(1, getListBottom() - getListTop());
        return Math.max(0, totalHeight - visibleHeight);
    }

    private void clampScrollTargets() {
        double maxScroll = getMaxScroll();
        targetScroll = Math.max(0.0D, Math.min(targetScroll, maxScroll));
        scrollOffset = Math.max(0.0D, Math.min(scrollOffset, maxScroll));
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        scrollOffset += (targetScroll - scrollOffset) * 0.35D;
        clampScrollTargets();

        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.region.title"), contentX + contentWidth / 2, contentY + s(8));
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.hint"), contentX, contentY + s(22), 0xFFBBD1E1);

        List<MapClient.PlannerDraft> remoteDrafts = MapClient.getRemotePlannerDrafts();
        String summary = remoteDrafts.isEmpty()
                ? Text.translatable("phone.tzz_mod.region.draft_empty").getString()
                : Text.translatable("phone.tzz_mod.region.remote_drafts", remoteDrafts.size()).getString();
        context.drawTextWithShadow(textRenderer, Text.literal(summary), contentX, contentY + s(36), 0xFFECECEC);

        if (rows.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.empty"), contentX + contentWidth / 2, contentY + s(84), 0xFFECECEC);
            return;
        }

        int top = getListTop();
        int bottom = getListBottom();
        int rowHeight = getRowHeight();
        int gap = getRowGap();
        int currentScroll = (int) Math.round(scrollOffset);

        context.enableScissor(contentX, top, contentX + contentWidth, bottom);
        for (int index = 0; index < rows.size(); index++) {
            RowEntry row = rows.get(index);
            int drawY = top + index * (rowHeight + gap) - currentScroll;
            if (drawY + rowHeight < top || drawY > bottom) {
                continue;
            }

            boolean hovered = mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= drawY && mouseY <= drawY + rowHeight;
            context.fill(contentX, drawY, contentX + contentWidth, drawY + rowHeight, hovered ? 0x334A6075 : 0x22333333);
            context.fill(contentX + s(6), drawY + s(7), contentX + s(12), drawY + s(23), row.color());

            int actionX = contentX + contentWidth - s(30);
            if (row.draft()) {
                context.fill(actionX, drawY + s(7), actionX + s(24), drawY + s(21), 0xAA4DABF7);
                context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.draft_label"), actionX + s(12), drawY + s(10), 0xFFF6FDFF);
            } else {
                int toggleWidth = s(24);
                int toggleHeight = s(14);
                int toggleY = drawY + s(8);
                boolean visible = MapClient.isRegionVisible(row.regionId());
                int toggleColor = visible ? 0xAA3FC47F : 0x66415A73;
                context.fill(actionX, toggleY, actionX + toggleWidth, toggleY + toggleHeight, toggleColor);
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(visible ? "开" : "关"), actionX + toggleWidth / 2, toggleY + s(3), 0xFFF4FFFA);
            }

            String title = textRenderer.trimToWidth(row.title(), contentWidth - s(48));
            String subtitle = textRenderer.trimToWidth(row.subtitle(), contentWidth - s(48));
            context.drawTextWithShadow(textRenderer, Text.literal(title), contentX + s(16), drawY + s(5), 0xFFECECEC);
            context.drawTextWithShadow(textRenderer, Text.literal(subtitle), contentX + s(16), drawY + s(17), 0xFFB7C7D8);
        }
        context.disableScissor();

        renderScrollbar(context, top, bottom, rows.size() * rowHeight + Math.max(0, rows.size() - 1) * gap, currentScroll);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        int top = getListTop();
        int bottom = getListBottom();
        if (mouseX < contentX || mouseX > contentX + contentWidth || mouseY < top || mouseY > bottom) {
            return false;
        }

        int rowHeight = getRowHeight();
        int gap = getRowGap();
        int currentScroll = (int) Math.round(scrollOffset);
        for (int index = 0; index < rows.size(); index++) {
            int drawY = top + index * (rowHeight + gap) - currentScroll;
            if (mouseY < drawY || mouseY > drawY + rowHeight) {
                continue;
            }
            RowEntry row = rows.get(index);
            int actionX = contentX + contentWidth - s(30);
            if (!row.draft()) {
                int toggleWidth = s(24);
                int toggleHeight = s(14);
                int toggleY = drawY + s(8);
                if (mouseX >= actionX && mouseX <= actionX + toggleWidth && mouseY >= toggleY && mouseY <= toggleY + toggleHeight) {
                    MapClient.setRegionVisible(row.regionId(), !MapClient.isRegionVisible(row.regionId()));
                    return true;
                }
            }
            if (client != null) {
                client.setScreen(new RegionPlannerDetailScreen(this, row.regionId(), row.draft()));
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
            targetScroll = Math.max(0.0D, Math.min(targetScroll - verticalAmount * s(14), getMaxScroll()));
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

    private record RowEntry(boolean draft, String regionId, String title, String subtitle, int color) {
        private static RowEntry draft(String title, String subtitle, int color) {
            return new RowEntry(true, "", title, subtitle, color);
        }

        private static RowEntry region(String regionId, String title, String subtitle, int color) {
            return new RowEntry(false, regionId, title, subtitle, color);
        }
    }
}