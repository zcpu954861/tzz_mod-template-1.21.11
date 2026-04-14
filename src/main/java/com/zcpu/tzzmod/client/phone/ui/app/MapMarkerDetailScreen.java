package com.zcpu.tzzmod.client.phone.ui.app;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.map.MapColors;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.ArrayList;
import java.util.List;
public class MapMarkerDetailScreen extends AbstractPhoneScreen {
    private final String markerId;
    private Runnable stateListener;
    private final List<ColorSwatch> swatches = new ArrayList<>();
    private TextFieldWidget nameField;
    private int previewShift;

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

        nameField = new TextFieldWidget(textRenderer, contentX + s(12), contentY + s(48), contentWidth - s(24), textRenderer.fontHeight, Text.empty());
        nameField.setMaxLength(256);
        nameField.setPlaceholder(Text.translatable("phone.tzz_mod.marker.name_placeholder"));
        styleTextField(nameField);
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
        int startY = contentY + s(120);
        int size = s(12);
        int gap = s(5);
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
            Text deletedText = Text.translatable("phone.tzz_mod.marker.deleted");
            context.drawText(textRenderer, deletedText, contentX + contentWidth / 2 - textRenderer.getWidth(deletedText) / 2, contentY + s(46), isLightMode() ? themeText() : 0xFFECECEC, !isLightMode());
            return;
        }

        context.drawText(textRenderer, Text.translatable("phone.tzz_mod.marker.name"), contentX + s(10), contentY + s(34), isLightMode() ? themeText() : 0xFFECECEC, !isLightMode());
        if (nameField != null) renderStyledTextFieldBackground(context, nameField);

        // Live JSON text preview above the name field
        previewShift = 0;
        if (nameField != null && !nameField.getText().isBlank()) {
            Text previewText = tryParseJsonText(nameField.getText().trim());
            String plain = previewText.getString();
            if (!plain.equals(nameField.getText().trim())) {
                // Parsed text differs from raw => show preview
                // Preview goes in the space between label and field (above field)
                int previewY = contentY + s(34) + textRenderer.fontHeight + s(3);
                int maxW = contentWidth - s(20);
                List<net.minecraft.text.OrderedText> wrapped = textRenderer.wrapLines(previewText, maxW);
                int lineH = textRenderer.fontHeight + s(2);
                previewShift = wrapped.size() * lineH + s(4);
                context.fill(contentX + s(8), previewY - s(2), contentX + contentWidth - s(8), previewY + wrapped.size() * lineH + s(2),
                        isLightMode() ? 0x22000000 : 0x221A2A3C);
                for (int i = 0; i < wrapped.size(); i++) {
                    context.drawText(textRenderer, wrapped.get(i), contentX + s(10), previewY + i * lineH, themeText(), !isLightMode());
                }
            }
        }
        if (previewShift > 0 && nameField != null) {
            nameField.setY(contentY + s(48) + previewShift);
            nameField.setX(contentX + s(12));
        } else if (nameField != null) {
            nameField.setY(contentY + s(48));
            nameField.setX(contentX + s(12));
        }

        context.drawText(textRenderer, Text.literal("X: " + marker.x() + "   Z: " + marker.z()), contentX + s(10), contentY + s(76) + previewShift, isLightMode() ? themeTextDim() : 0xFFB7C7D8, !isLightMode());

        // Map visibility toggle
        int visRowY = contentY + s(88) + previewShift;
        boolean visible = MapClient.isMarkerVisible(markerId);
        int switchW = s(28);
        int switchH = s(12);
        int switchX = contentX + contentWidth - switchW - s(10);
        int switchY = visRowY;
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
        context.drawText(textRenderer, Text.translatable("phone.tzz_mod.marker.map_visible"), contentX + s(10), visRowY + (switchH - textRenderer.fontHeight) / 2, isLightMode() ? themeText() : 0xFFECECEC, !isLightMode());

        context.drawText(textRenderer, Text.translatable("phone.tzz_mod.marker.color"), contentX + s(10), contentY + s(106) + previewShift, isLightMode() ? themeText() : 0xFFECECEC, !isLightMode());

        for (ColorSwatch swatch : swatches) {
            int sy = swatch.y() + previewShift;
            context.fill(swatch.x(), sy, swatch.x() + swatch.size(), sy + swatch.size(), 0xCC000000);
            context.fill(swatch.x() + 1, sy + 1, swatch.x() + swatch.size() - 1, sy + swatch.size() - 1, swatch.color() | 0xFF000000);
            if (swatch.color() == marker.color()) {
                context.fill(swatch.x() - 1, sy - 1, swatch.x() + swatch.size() + 1, sy, 0xFFFFFFFF);
                context.fill(swatch.x() - 1, sy + swatch.size(), swatch.x() + swatch.size() + 1, sy + swatch.size() + 1, 0xFFFFFFFF);
                context.fill(swatch.x() - 1, sy, swatch.x(), sy + swatch.size(), 0xFFFFFFFF);
                context.fill(swatch.x() + swatch.size(), sy, swatch.x() + swatch.size() + 1, sy + swatch.size(), 0xFFFFFFFF);
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
        // Visibility toggle
        int visRowY = contentY + s(88) + previewShift;
        int switchW = s(28);
        int switchH = s(12);
        int switchX = contentX + contentWidth - switchW - s(10);
        if (mx >= switchX && mx <= switchX + switchW && my >= visRowY && my <= visRowY + switchH) {
            MapClient.setMarkerVisible(markerId, !MapClient.isMarkerVisible(markerId));
            return true;
        }
        for (ColorSwatch swatch : swatches) {
            int sy = swatch.y() + previewShift;
            if (mx >= swatch.x() && mx <= swatch.x() + swatch.size() && my >= sy && my <= sy + swatch.size()) {
                MapClient.setMarkerColor(markerId, swatch.color());
                return true;
            }
        }
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        // Auto-save name on close
        if (nameField != null) {
            String name = nameField.getText().trim();
            MapClient.MapMarker marker = MapClient.getMarker(markerId);
            if (!name.isBlank() && marker != null && !name.equals(marker.name())) {
                MapClient.setMarkerName(markerId, name);
            }
        }
        if (stateListener != null) {
            MapClient.removeListener(stateListener);
            stateListener = null;
        }
    }

    private record ColorSwatch(int x, int y, int size, int color) {
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