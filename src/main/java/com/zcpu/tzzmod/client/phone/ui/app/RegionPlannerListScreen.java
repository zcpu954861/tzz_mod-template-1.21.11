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
        clampScrollTargets();

        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.region.title"), contentX + contentWidth / 2, contentY + s(8));
        context.drawText(textRenderer, Text.translatable("phone.tzz_mod.region.hint"), contentX, contentY + s(22), 0xFFBBD1E1, !isLightMode());

        // Off-hand display toggle (right side of the hint row)
        int offSwitchW = s(24);
        int offSwitchH = s(10);
        int offSwitchX = contentX + contentWidth - offSwitchW - s(2);
        int offSwitchY = contentY + s(21);
        boolean offHand = MapClient.isRegionOffHandEnabled();
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
        context.drawText(textRenderer, Text.translatable("phone.tzz_mod.region.off_hand"),
                contentX, offSwitchY + (offSwitchH - textRenderer.fontHeight) / 2,
                isLightMode() ? themeTextDim() : 0xFFB7C7D8, !isLightMode());

        List<MapClient.PlannerDraft> remoteDrafts = MapClient.getRemotePlannerDrafts();
        String summary = remoteDrafts.isEmpty()
                ? Text.translatable("phone.tzz_mod.region.draft_empty").getString()
                : Text.translatable("phone.tzz_mod.region.remote_drafts", remoteDrafts.size()).getString();
        context.drawText(textRenderer, Text.literal(summary), contentX, contentY + s(36), isLightMode() ? themeText() : 0xFFECECEC, !isLightMode());

        if (rows.isEmpty()) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.region.empty"), contentX + contentWidth / 2, contentY + s(84), themeText());
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

            int switchW = s(28);
            int switchH = s(12);
            int switchX = contentX + contentWidth - s(32);
            int switchY = drawY + (rowHeight - switchH) / 2;
            if (row.draft()) {
                context.fill(switchX, drawY + s(7), switchX + switchW, drawY + s(21), 0xAA4DABF7);
                context.drawText(textRenderer, Text.translatable("phone.tzz_mod.region.draft_label"), switchX + switchW / 2 - textRenderer.getWidth(Text.translatable("phone.tzz_mod.region.draft_label")) / 2, drawY + s(10), 0xFFF6FDFF, true);
            } else {
                boolean visible = MapClient.isRegionVisible(row.regionId());
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
            }

            String title = textRenderer.trimToWidth(row.title(), contentWidth - s(48));
            String subtitle = textRenderer.trimToWidth(row.subtitle(), contentWidth - s(48));
            context.drawText(textRenderer, Text.literal(title), contentX + s(16), drawY + s(5), isLightMode() ? themeText() : 0xFFECECEC, !isLightMode());
            context.drawText(textRenderer, Text.literal(subtitle), contentX + s(16), drawY + s(17), isLightMode() ? themeTextDim() : 0xFFB7C7D8, !isLightMode());
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

        // Off-hand toggle click
        int offSwitchW = s(24);
        int offSwitchH = s(10);
        int offSwitchX = contentX + contentWidth - offSwitchW - s(2);
        int offSwitchY = contentY + s(21);
        if (mouseX >= offSwitchX && mouseX <= offSwitchX + offSwitchW && mouseY >= offSwitchY && mouseY <= offSwitchY + offSwitchH) {
            MapClient.setRegionOffHandEnabled(!MapClient.isRegionOffHandEnabled());
            return true;
        }

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
            int switchW = s(28);
            int switchH = s(12);
            int switchX = contentX + contentWidth - s(32);
            int switchY = drawY + (rowHeight - switchH) / 2;
            if (!row.draft() && mouseX >= switchX && mouseX <= switchX + switchW
                    && mouseY >= switchY && mouseY <= switchY + switchH) {
                MapClient.setRegionVisible(row.regionId(), !MapClient.isRegionVisible(row.regionId()));
                return true;
            }
            if (client != null) {
                RegionPlannerDetailScreen detail = new RegionPlannerDetailScreen(this, row.regionId(), row.draft());
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