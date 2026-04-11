package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.ForcedHudClient;
import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.phone.ui.RoundedRectRenderer;
import com.zcpu.tzzmod.network.AdminModeC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class PhoneAdminAppScreen extends AbstractPhoneScreen {
    private static final float TOGGLE_ANIM_SPEED = 6.0f;
    private final List<ToggleRow> rows = new ArrayList<>();
    private Runnable mapListener;

    public PhoneAdminAppScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.admin"), parent);
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(72), s(20), button -> close());

        addRow(
                Text.translatable("phone.tzz_mod.admin.record_mode"),
                Text.translatable("phone.tzz_mod.admin.record_mode.subtitle"),
                ForcedHudClient::isForceShowHead,
                enabled -> {
                    ClientPlayNetworking.send(new AdminModeC2SPayload(enabled));
                    ForcedHudClient.setServerEnforcedHud(enabled);
                }
        );
        addRow(
                Text.translatable("phone.tzz_mod.admin.show_self_position"),
                Text.translatable("phone.tzz_mod.admin.show_self_position.subtitle"),
                () -> MapClient.getSettings().showSelfPosition(),
                enabled -> MapClient.setVisibility("show_self_position", enabled)
        );
        addRow(
                Text.translatable("phone.tzz_mod.admin.show_markers"),
                Text.translatable("phone.tzz_mod.admin.show_markers.subtitle"),
                () -> MapClient.getSettings().showMarkers(),
                enabled -> MapClient.setVisibility("show_markers", enabled)
        );
        addRow(
                Text.translatable("phone.tzz_mod.admin.show_other_players"),
                Text.translatable("phone.tzz_mod.admin.show_other_players.subtitle"),
                () -> MapClient.getSettings().showOtherPlayers(),
                enabled -> MapClient.setVisibility("show_other_players", enabled)
        );

        mapListener = () -> {
        };
        MapClient.addListener(mapListener);
        MapClient.requestState();
    }

    private void addRow(Text label, Text subtitle, BooleanSupplier stateSupplier, Consumer<Boolean> toggleAction) {
        int rowIndex = rows.size();
        int rowX = contentX + s(8);
        int rowY = contentY + s(34) + rowIndex * s(56);
        int rowWidth = contentWidth - s(16);
        int rowHeight = s(50);
        ButtonWidget button = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> toggleAction.accept(!stateSupplier.getAsBoolean()))
                .dimensions(rowX, rowY, rowWidth, rowHeight)
                .build());
        button.setAlpha(0.0F);
        rows.add(new ToggleRow(label, subtitle, stateSupplier, button));
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.admin"), contentX + contentWidth / 2, contentY + s(8));

        int rowX = contentX + s(8);
        int rowWidth = contentWidth - s(16);
        for (int index = 0; index < rows.size(); index++) {
            ToggleRow row = rows.get(index);
            int rowY = contentY + s(34) + index * s(56);
            boolean enabled = row.stateSupplier().getAsBoolean();
            row.progress = approach(row.progress, enabled ? 1.0F : 0.0F, TOGGLE_ANIM_SPEED * delta);

            context.fill(rowX, rowY, rowX + rowWidth, rowY + s(50), 0x22333333);
            context.drawTextWithShadow(textRenderer, row.label(), rowX + s(6), rowY + s(5), 0xFFECECEC);

            List<String> wrapped = wrap(row.subtitle().getString(), rowWidth - s(52));
            for (int line = 0; line < wrapped.size(); line++) {
                context.drawTextWithShadow(textRenderer, Text.literal(wrapped.get(line)), rowX + s(6), rowY + s(18) + line * (textRenderer.fontHeight + s(1)), 0xFFB8C7D4);
            }

            int switchW = s(36);
            int switchH = s(14);
            int switchX = rowX + rowWidth - switchW - s(6);
            int switchY = rowY + s(6);
            int capsuleColor = lerpColor(0x55333333, 0xFF3FC47F, row.progress);
            int knobColor = enabled ? 0xFFFFFFFF : 0xFFCCCCCC;
            RoundedRectRenderer.fillRoundedRect(context, switchX, switchY, switchW, switchH, switchH / 2, capsuleColor);
            int knobRadius = Math.max(1, switchH - s(2));
            float leftCX = switchX + s(2) + knobRadius / 2.0F;
            float rightCX = switchX + switchW - s(2) - knobRadius / 2.0F;
            int knobCX = Math.round(leftCX + (rightCX - leftCX) * row.progress);
            int knobLeft = knobCX - knobRadius / 2;
            int knobTop = switchY + (switchH - knobRadius) / 2;
            RoundedRectRenderer.fillRoundedRect(context, knobLeft, knobTop, knobRadius, knobRadius, knobRadius / 2, knobColor);
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (mapListener != null) {
            MapClient.removeListener(mapListener);
            mapListener = null;
        }
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            int charLength = Character.charCount(codePoint);
            current.appendCodePoint(codePoint);
            if (textRenderer.getWidth(current.toString()) > maxWidth) {
                current.setLength(current.length() - charLength);
                if (current.isEmpty()) {
                    current.appendCodePoint(codePoint);
                    i += charLength;
                }
                lines.add(current.toString());
                current = new StringBuilder();
                continue;
            }
            i += charLength;
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static float approach(float current, float target, float delta) {
        if (current < target) {
            return Math.min(target, current + delta);
        }
        return Math.max(target, current - delta);
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int aa = (a >> 24) & 0xFF;
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int ba = (b >> 24) & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int ra = Math.round(aa + (ba - aa) * t);
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    private static final class ToggleRow {
        private final Text label;
        private final Text subtitle;
        private final BooleanSupplier stateSupplier;
        @SuppressWarnings("unused")
        private final ButtonWidget button;
        private float progress;

        private ToggleRow(Text label, Text subtitle, BooleanSupplier stateSupplier, ButtonWidget button) {
            this.label = label;
            this.subtitle = subtitle;
            this.stateSupplier = stateSupplier;
            this.button = button;
            this.progress = stateSupplier.getAsBoolean() ? 1.0F : 0.0F;
        }

        private Text label() {
            return label;
        }

        private Text subtitle() {
            return subtitle;
        }

        private BooleanSupplier stateSupplier() {
            return stateSupplier;
        }
    }
}

