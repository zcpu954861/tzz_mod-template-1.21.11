package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.map.MapColors;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class RegionPlannerDetailScreen extends AbstractPhoneScreen {
    private final String regionId;
    private final boolean draftMode;
    private Runnable stateListener;
    private TextFieldWidget nameField;
    private double scrollOffset;
    private double targetScroll;

    public RegionPlannerDetailScreen(Screen parent, String regionId, boolean draftMode) {
        super(Text.translatable(draftMode ? "phone.tzz_mod.region.draft_detail" : "phone.tzz_mod.region.detail"), parent);
        this.regionId = regionId;
        this.draftMode = draftMode;
    }

    @Override
    protected void init() {
        super.init();
        int bottomY = contentY + contentHeight - s(24);
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, bottomY, s(54), s(20), button -> close());
        if (!draftMode) {
            addPhonePrimaryButton(Text.translatable("phone.tzz_mod.region.rename"), contentX + s(58), bottomY, s(60), s(20), button -> saveName());
            addPhonePrimaryButton(Text.translatable("phone.tzz_mod.region.delete"), contentX + contentWidth - s(84), bottomY, s(84), s(20), button -> {
                MapClient.deleteRegion(regionId);
                close();
            });
            nameField = new TextFieldWidget(textRenderer, contentX + s(10), contentY + s(48), contentWidth - s(20), s(18), Text.empty());
            nameField.setMaxLength(48);
            nameField.setPlaceholder(Text.translatable("phone.tzz_mod.region.name_placeholder"));
            addDrawableChild(nameField);
        } else {
            addPhonePrimaryButton(Text.translatable("phone.tzz_mod.region.clear_draft"), contentX + contentWidth - s(84), bottomY, s(84), s(20), button -> {
                MapClient.clearPlannerDraft();
                close();
            });
        }

        stateListener = this::syncFromState;
        MapClient.addListener(stateListener);
        syncFromState();
    }

    private void saveName() {
        if (draftMode || nameField == null) {
            return;
        }
        String name = nameField.getText().trim();
        if (!name.isBlank()) {
            MapClient.renameRegion(regionId, name);
        }
    }

    private void syncFromState() {
        clampScroll();
        if (draftMode || nameField == null || nameField.isFocused()) {
            return;
        }
        MapClient.PlannerRegion region = MapClient.getPlannerRegion(regionId);
        if (region != null && !region.name().equals(nameField.getText())) {
            nameField.setText(region.name());
        }
    }

    private int getColorGridX() {
        return contentX + s(10);
    }

    private int getColorGridY() {
        return draftMode ? contentY + s(88) : contentY + s(96);
    }

    private int getColorCellSize() {
        return s(12);
    }

    private int getColorGap() {
        return s(5);
    }

    private int getColorColumns() {
        return 4;
    }

    private int getListTop() {
        return draftMode ? contentY + s(158) : contentY + s(166);
    }

    private int getListBottom() {
        return contentY + contentHeight - s(32);
    }

    private int getRowHeight() {
        return draftMode ? s(34) : s(30);
    }

    private int getRowGap() {
        return s(5);
    }

    private int getPointCount() {
        return draftMode ? MapClient.getPlannerDraft().points().size() : currentRegionPoints();
    }

    private int currentRegionPoints() {
        MapClient.PlannerRegion region = MapClient.getPlannerRegion(regionId);
        return region == null ? 0 : region.points().size();
    }

    private int getMaxScroll() {
        int rowCount = getPointCount();
        int totalHeight = rowCount == 0 ? 0 : rowCount * getRowHeight() + (rowCount - 1) * getRowGap();
        int visibleHeight = Math.max(1, getListBottom() - getListTop());
        return Math.max(0, totalHeight - visibleHeight);
    }

    private void clampScroll() {
        int maxScroll = getMaxScroll();
        targetScroll = Math.max(0.0D, Math.min(targetScroll, maxScroll));
        scrollOffset = Math.max(0.0D, Math.min(scrollOffset, maxScroll));
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        scrollOffset += (targetScroll - scrollOffset) * 0.35D;
        clampScroll();

        if (draftMode) {
            renderDraftContent(context, mouseX, mouseY);
        } else {
            renderRegionContent(context, mouseX, mouseY);
        }
    }

    private void renderRegionContent(DrawContext context, int mouseX, int mouseY) {
        MapClient.PlannerRegion region = MapClient.getPlannerRegion(regionId);
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.region.detail"), contentX + contentWidth / 2, contentY + s(8));
        if (region == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.deleted"), contentX + contentWidth / 2, contentY + s(72), 0xFFECECEC);
            return;
        }

        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.name"), contentX + s(10), contentY + s(34), 0xFFECECEC);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.points_count", region.points().size()), contentX + s(10), contentY + s(72), 0xFFB7C7D8);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.color"), contentX + s(10), contentY + s(84), 0xFFECECEC);
        renderColorGrid(context, region.color());
        renderPointsList(context, mouseX, mouseY, region.name(), region.points(), false);
    }

    private void renderDraftContent(DrawContext context, int mouseX, int mouseY) {
        MapClient.PlannerDraft draft = MapClient.getPlannerDraft();
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.region.draft_detail"), contentX + contentWidth / 2, contentY + s(8));
        if (draft.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.draft_empty"), contentX + contentWidth / 2, contentY + s(72), 0xFFECECEC);
            return;
        }

        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.draft_entry"), contentX + s(10), contentY + s(34), 0xFFECECEC);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.points_count", draft.points().size()), contentX + s(10), contentY + s(48), 0xFFB7C7D8);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.trim_hint"), contentX + s(10), contentY + s(62), 0xFFB7C7D8);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.color"), contentX + s(10), contentY + s(76), 0xFFECECEC);
        renderColorGrid(context, draft.color());
        renderPointsList(context, mouseX, mouseY, Text.translatable("phone.tzz_mod.region.draft_label").getString(), draft.points(), true);
    }

    private void renderColorGrid(DrawContext context, int selectedColor) {
        int columns = getColorColumns();
        int cell = getColorCellSize();
        int gap = getColorGap();
        int startX = getColorGridX();
        int startY = getColorGridY();
        for (int index = 0; index < MapColors.MARKER_PALETTE.length; index++) {
            int column = index % columns;
            int row = index / columns;
            int x = startX + column * (cell + gap);
            int y = startY + row * (cell + gap);
            int color = MapColors.MARKER_PALETTE[index];
            context.fill(x, y, x + cell, y + cell, color);
            boolean selected = (selectedColor & 0xFFFFFF) == (color & 0xFFFFFF);
            if (selected) {
                context.fill(x - 1, y - 1, x + cell + 1, y, 0xFFF6FDFF);
                context.fill(x - 1, y + cell, x + cell + 1, y + cell + 1, 0xFFF6FDFF);
                context.fill(x - 1, y, x, y + cell, 0xFFF6FDFF);
                context.fill(x + cell, y, x + cell + 1, y + cell, 0xFFF6FDFF);
            }
        }
    }

    private void renderPointsList(DrawContext context, int mouseX, int mouseY, String namePrefix, java.util.List<MapClient.RegionPoint> points, boolean allowTrim) {
        int top = getListTop();
        int bottom = getListBottom();
        int rowHeight = getRowHeight();
        int gap = getRowGap();
        int currentScroll = (int) Math.round(scrollOffset);

        context.enableScissor(contentX, top, contentX + contentWidth, bottom);
        for (int index = 0; index < points.size(); index++) {
            MapClient.RegionPoint point = points.get(index);
            int drawY = top + index * (rowHeight + gap) - currentScroll;
            if (drawY + rowHeight < top || drawY > bottom) {
                continue;
            }
            boolean hovered = mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= drawY && mouseY <= drawY + rowHeight;
            context.fill(contentX, drawY, contentX + contentWidth, drawY + rowHeight, hovered ? 0x334A6075 : 0x22333333);

            int teleportWidth = allowTrim ? s(40) : s(52);
            int teleportX = contentX + contentWidth - teleportWidth - s(6);
            int buttonY = drawY + s(7);
            if (allowTrim) {
                int trimWidth = s(28);
                int trimX = teleportX - trimWidth - s(4);
                context.fill(trimX, buttonY, trimX + trimWidth, buttonY + s(16), 0xAAE86E5A);
                context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.trim"), trimX + trimWidth / 2, buttonY + s(4), 0xFFFFFBFA);
            }
            context.fill(teleportX, buttonY, teleportX + teleportWidth, buttonY + s(16), 0xAA2A8FC1);
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.region.teleport"), teleportX + teleportWidth / 2, buttonY + s(4), 0xFFF6FDFF);

            String pointName = trim(namePrefix + (index + 1), contentWidth - (allowTrim ? s(84) : s(66)));
            String pointPos = "X: " + point.x() + "  Z: " + point.z();
            context.drawTextWithShadow(textRenderer, Text.literal(pointName), contentX + s(6), drawY + s(4), 0xFFECECEC);
            context.drawTextWithShadow(textRenderer, Text.literal(pointPos), contentX + s(6), drawY + s(16), 0xFFB7C7D8);
        }
        context.disableScissor();

        int totalHeight = points.size() * rowHeight + Math.max(0, points.size() - 1) * gap;
        renderScrollbar(context, top, bottom, totalHeight, currentScroll);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }

        if (handleColorClick((int) click.x(), (int) click.y())) {
            return true;
        }

        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        int top = getListTop();
        int bottom = getListBottom();
        if (mouseX < contentX || mouseX > contentX + contentWidth || mouseY < top || mouseY > bottom) {
            return false;
        }

        java.util.List<MapClient.RegionPoint> points = draftMode
                ? MapClient.getPlannerDraft().points()
                : MapClient.getPlannerRegion(regionId) == null ? java.util.List.of() : MapClient.getPlannerRegion(regionId).points();
        int rowHeight = getRowHeight();
        int gap = getRowGap();
        int currentScroll = (int) Math.round(scrollOffset);
        for (int index = 0; index < points.size(); index++) {
            int drawY = top + index * (rowHeight + gap) - currentScroll;
            if (mouseY < drawY || mouseY > drawY + rowHeight) {
                continue;
            }
            int buttonY = drawY + s(7);
            int teleportWidth = draftMode ? s(40) : s(52);
            int teleportX = contentX + contentWidth - teleportWidth - s(6);
            if (draftMode) {
                int trimWidth = s(28);
                int trimX = teleportX - trimWidth - s(4);
                if (mouseX >= trimX && mouseX <= trimX + trimWidth && mouseY >= buttonY && mouseY <= buttonY + s(16)) {
                    MapClient.trimPlannerDraft(index);
                    return true;
                }
            }
            if (mouseX >= teleportX && mouseX <= teleportX + teleportWidth && mouseY >= buttonY && mouseY <= buttonY + s(16)) {
                if (draftMode) {
                    MapClient.teleportToDraftCorner(index);
                } else {
                    MapClient.teleportToRegionCorner(regionId, index);
                }
                return true;
            }
        }
        return true;
    }

    private boolean handleColorClick(int mouseX, int mouseY) {
        int cell = getColorCellSize();
        int gap = getColorGap();
        int columns = getColorColumns();
        int startX = getColorGridX();
        int startY = getColorGridY();
        for (int index = 0; index < MapColors.MARKER_PALETTE.length; index++) {
            int column = index % columns;
            int row = index / columns;
            int x = startX + column * (cell + gap);
            int y = startY + row * (cell + gap);
            if (mouseX >= x && mouseX <= x + cell && mouseY >= y && mouseY <= y + cell) {
                int color = MapColors.MARKER_PALETTE[index];
                if (draftMode) {
                    MapClient.setDraftColor(color);
                } else {
                    MapClient.setRegionColor(regionId, color);
                }
                return true;
            }
        }
        return false;
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

    private String trim(String text, int width) {
        return textRenderer.trimToWidth(text, Math.max(s(24), width));
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