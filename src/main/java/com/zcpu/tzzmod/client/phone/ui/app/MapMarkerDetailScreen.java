package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.map.MapColors;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MapMarkerDetailScreen extends AbstractPhoneScreen {
    private final String markerId;
    private Runnable stateListener;
    private final List<ColorSwatch> swatches = new ArrayList<>();
    private TextFieldWidget nameField;

    public MapMarkerDetailScreen(Screen parent, String markerId) {
        super(Text.translatable("phone.tzz_mod.marker.detail"), parent);
        this.markerId = markerId;
    }

    @Override
    protected void init() {
        super.init();
        int actionRowY = contentY + contentHeight - s(48);
        int bottomRowY = contentY + contentHeight - s(24);
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, bottomRowY, s(72), s(20), button -> close());
        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.marker.rename"), contentX, actionRowY, s(84), s(20), button -> saveName());
        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.marker.teleport"), contentX + contentWidth - s(88), actionRowY, s(88), s(20), button -> MapClient.teleportToMarker(markerId));
        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.marker.delete"), contentX + contentWidth - s(88), bottomRowY, s(88), s(20), button -> {
            MapClient.deleteMarker(markerId);
            close();
        });

        nameField = new TextFieldWidget(textRenderer, contentX + s(10), contentY + s(48), contentWidth - s(20), s(18), Text.empty());
        nameField.setMaxLength(64);
        nameField.setPlaceholder(Text.translatable("phone.tzz_mod.marker.name_placeholder"));
        addDrawableChild(nameField);

        stateListener = this::syncFromState;
        MapClient.addListener(stateListener);
        rebuildSwatches();
        syncFromState();
    }

    private void saveName() {
        if (nameField == null) {
            return;
        }
        String name = nameField.getText().trim();
        if (name.isBlank()) {
            return;
        }
        MapClient.setMarkerName(markerId, name);
    }

    private void syncFromState() {
        MapClient.MapMarker marker = MapClient.getMarker(markerId);
        if (marker == null || nameField == null || nameField.isFocused()) {
            return;
        }
        if (!marker.name().equals(nameField.getText())) {
            nameField.setText(marker.name());
        }
    }

    private void rebuildSwatches() {
        swatches.clear();
        int startX = contentX + s(10);
        int startY = contentY + s(134);
        int size = s(14);
        int gap = s(6);
        for (int index = 0; index < MapColors.MARKER_PALETTE.length; index++) {
            int row = index / 4;
            int column = index % 4;
            int x = startX + column * (size + gap);
            int y = startY + row * (size + gap);
            swatches.add(new ColorSwatch(x, y, size, MapColors.MARKER_PALETTE[index]));
        }
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.marker.detail"), contentX + contentWidth / 2, contentY + s(8));

        MapClient.MapMarker marker = MapClient.getMarker(markerId);
        if (marker == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.marker.deleted"), contentX + contentWidth / 2, contentY + s(46), 0xFFECECEC);
            return;
        }

        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.marker.name"), contentX + s(10), contentY + s(34), 0xFFECECEC);
        context.drawTextWithShadow(textRenderer, Text.literal("X: " + marker.x()), contentX + s(10), contentY + s(76), 0xFFB7C7D8);
        context.drawTextWithShadow(textRenderer, Text.literal("Y: " + marker.y()), contentX + s(10), contentY + s(88), 0xFFB7C7D8);
        context.drawTextWithShadow(textRenderer, Text.literal("Z: " + marker.z()), contentX + s(10), contentY + s(100), 0xFFB7C7D8);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.marker.color"), contentX + s(10), contentY + s(114), 0xFFECECEC);

        for (ColorSwatch swatch : swatches) {
            context.fill(swatch.x(), swatch.y(), swatch.x() + swatch.size(), swatch.y() + swatch.size(), 0xCC000000);
            context.fill(swatch.x() + 1, swatch.y() + 1, swatch.x() + swatch.size() - 1, swatch.y() + swatch.size() - 1, swatch.color() | 0xFF000000);
            if (swatch.color() == marker.color()) {
                context.fill(swatch.x() - 1, swatch.y() - 1, swatch.x() + swatch.size() + 1, swatch.y(), 0xFFFFFFFF);
                context.fill(swatch.x() - 1, swatch.y() + swatch.size(), swatch.x() + swatch.size() + 1, swatch.y() + swatch.size() + 1, 0xFFFFFFFF);
                context.fill(swatch.x() - 1, swatch.y(), swatch.x(), swatch.y() + swatch.size(), 0xFFFFFFFF);
                context.fill(swatch.x() + swatch.size(), swatch.y(), swatch.x() + swatch.size() + 1, swatch.y() + swatch.size(), 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        int mx = (int) click.x();
        int my = (int) click.y();
        for (ColorSwatch swatch : swatches) {
            if (mx >= swatch.x() && mx <= swatch.x() + swatch.size() && my >= swatch.y() && my <= swatch.y() + swatch.size()) {
                MapClient.setMarkerColor(markerId, swatch.color());
                return true;
            }
        }
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        if (stateListener != null) {
            MapClient.removeListener(stateListener);
            stateListener = null;
        }
    }

    private record ColorSwatch(int x, int y, int size, int color) {
    }
}