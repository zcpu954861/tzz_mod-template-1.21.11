package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.ForcedHudClient;
import com.zcpu.tzzmod.client.map.MapClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.phone.ui.RoundedRectRenderer;
import com.zcpu.tzzmod.network.AdminModeC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class PhoneAdminAppScreen extends AbstractPhoneScreen {
    private final List<ToggleRow> rows = new ArrayList<>();
    private double scrollOffset;
    private double targetScroll;

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
        addRow(
                Text.translatable("phone.tzz_mod.admin.show_region_titles"),
                Text.translatable("phone.tzz_mod.admin.show_region_titles.subtitle"),
                () -> MapClient.getSettings().showRegionTitles(),
                enabled -> MapClient.setVisibility("show_region_titles", enabled)
        );

        MapClient.requestState();
    }

    private void addRow(Text label, Text subtitle, BooleanSupplier stateSupplier, Consumer<Boolean> toggleAction) {
        rows.add(new ToggleRow(label, subtitle, stateSupplier, toggleAction));
    }

    private int getListTop() {
        return contentY + s(34);
    }

    private int getListBottom() {
        return contentY + contentHeight - s(30);
    }

    private int getMaxScroll() {
        int visibleHeight = Math.max(1, getListBottom() - getListTop());
        return Math.max(0, getTotalHeight() - visibleHeight);
    }

    private int getTotalHeight() {
        int height = 0;
        for (ToggleRow row : rows) {
            height += getRowHeight(row) + s(6);
        }
        return Math.max(0, height - s(6));
    }

    private int getRowHeight(ToggleRow row) {
        List<String> wrapped = wrap(row.subtitle().getString(), contentWidth - s(70));
        return s(22) + wrapped.size() * (textRenderer.fontHeight + s(1)) + s(8);
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

        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.admin"), contentX + contentWidth / 2, contentY + s(8));

        int rowX = contentX + s(4);
        int rowWidth = contentWidth - s(8);
        int currentY = getListTop() - (int) Math.round(scrollOffset);
        int bottom = getListBottom();

        context.enableScissor(contentX, getListTop(), contentX + contentWidth, bottom);
        for (ToggleRow row : rows) {
            int rowHeight = getRowHeight(row);
            if (currentY + rowHeight >= getListTop() && currentY <= bottom) {
                boolean hovered = mouseX >= rowX && mouseX <= rowX + rowWidth && mouseY >= currentY && mouseY <= currentY + rowHeight;
                boolean enabled = row.stateSupplier().getAsBoolean();
                row.progress = approach(row.progress, enabled ? 1.0F : 0.0F, 0.18F + delta * 0.12F);

                context.fill(rowX, currentY, rowX + rowWidth, currentY + rowHeight, hovered ? 0x33445D78 : 0x22333333);
                context.drawTextWithShadow(textRenderer, row.label(), rowX + s(6), currentY + s(6), 0xFFECECEC);

                List<String> wrapped = wrap(row.subtitle().getString(), rowWidth - s(56));
                for (int line = 0; line < wrapped.size(); line++) {
                    context.drawTextWithShadow(textRenderer, Text.literal(wrapped.get(line)), rowX + s(6), currentY + s(18) + line * (textRenderer.fontHeight + s(1)), 0xFFB8C7D4);
                }

                int switchW = s(36);
                int switchH = s(14);
                int switchX = rowX + rowWidth - switchW - s(6);
                int switchY = currentY + s(8);
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
            currentY += rowHeight + s(6);
        }
        context.disableScissor();

        renderScrollbar(context, getListTop(), bottom, getTotalHeight(), (int) Math.round(scrollOffset));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        int rowX = contentX + s(4);
        int rowWidth = contentWidth - s(8);
        int currentY = getListTop() - (int) Math.round(scrollOffset);
        for (ToggleRow row : rows) {
            int rowHeight = getRowHeight(row);
            if (mouseX >= rowX && mouseX <= rowX + rowWidth && mouseY >= currentY && mouseY <= currentY + rowHeight) {
                row.toggleAction().accept(!row.stateSupplier().getAsBoolean());
                return true;
            }
            currentY += rowHeight + s(6);
        }
        return false;
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
        private final Consumer<Boolean> toggleAction;
        private float progress;

        private ToggleRow(Text label, Text subtitle, BooleanSupplier stateSupplier, Consumer<Boolean> toggleAction) {
            this.label = label;
            this.subtitle = subtitle;
            this.stateSupplier = stateSupplier;
            this.toggleAction = toggleAction;
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

        private Consumer<Boolean> toggleAction() {
            return toggleAction;
        }
    }
}
