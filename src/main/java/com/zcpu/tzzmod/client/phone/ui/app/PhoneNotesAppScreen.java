package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.note.NoteClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PhoneNotesAppScreen extends AbstractPhoneScreen {
    private Runnable stateListener;
    private int scrollOffset;

    public PhoneNotesAppScreen(Screen parent) {
        super(Text.translatable("phone.tzz_mod.app.notes"), parent);
    }

    @Override
    protected void init() {
        super.init();
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(68), s(20), button -> close());
        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.notes.create"), contentX + contentWidth - s(82), contentY + contentHeight - s(24), s(82), s(20), button -> {
            if (client != null) {
                client.setScreen(new PhoneNoteEditScreen(this, "", "", "", 0L, true));
            }
        });
        stateListener = this::clampScrollOffset;
        NoteClient.addListener(stateListener);
        NoteClient.requestBootstrap();
    }

    @Override
    public void removed() {
        super.removed();
        if (stateListener != null) {
            NoteClient.removeListener(stateListener);
            stateListener = null;
        }
    }

    private int listTop() {
        return contentY + s(32);
    }

    private int listBottom() {
        return contentY + contentHeight - s(32);
    }

    private int rowHeight() {
        return s(34);
    }

    private int rowGap() {
        return s(4);
    }

    private int totalHeight() {
        List<NoteClient.NoteSummaryData> notes = NoteClient.getNotes();
        return notes.isEmpty() ? 0 : notes.size() * rowHeight() + Math.max(0, notes.size() - 1) * rowGap();
    }

    private void clampScrollOffset() {
        int max = Math.max(0, totalHeight() - Math.max(1, listBottom() - listTop()));
        scrollOffset = Math.max(0, Math.min(scrollOffset, max));
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.app.notes"), contentX + contentWidth / 2, contentY + s(8));
        List<NoteClient.NoteSummaryData> notes = NoteClient.getNotes();
        clampScrollOffset();

        if (notes.isEmpty()) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.notes.empty"),
                    contentX + contentWidth / 2, contentY + s(86), themeTextDim());
            return;
        }

        int top = listTop();
        int bottom = listBottom();
        int y = top - scrollOffset;
        for (NoteClient.NoteSummaryData note : notes) {
            int rowTop = y;
            int rowBottom = y + rowHeight();
            if (rowBottom >= top && rowTop <= bottom) {
                renderRow(context, note, rowTop, mouseX, mouseY);
            }
            y += rowHeight() + rowGap();
        }
        renderPhoneScrollbar(context, top, bottom, totalHeight(), scrollOffset);
    }

    private void renderRow(DrawContext context, NoteClient.NoteSummaryData note, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= y && mouseY <= y + rowHeight();
        int fill = hovered ? (isLightMode() ? 0x44C8DDF0 : 0x4410283C) : (isLightMode() ? 0x22D8E4F0 : 0x22101825);
        fillChamferedRect(context, contentX, y, contentWidth - s(4), rowHeight(), s(4), fill);

        int markerColor = switch (note.relation()) {
            case "owned" -> 0xFF3CB371;
            case "admin" -> 0xFFE6C84F;
            default -> 0xFF4AA3FF;
        };
        context.fill(contentX + s(5), y + s(6), contentX + s(8), y + rowHeight() - s(6), markerColor);

        int textX = contentX + s(14);
        int textW = contentWidth - s(22);
        String title = note.title().isBlank() ? Text.translatable("phone.tzz_mod.notes.untitled").getString() : note.title();
        String shownTitle = textRenderer.trimToWidth(title, Math.max(1, Math.round(textW / getTextScale())));
        String owner = Text.translatable("phone.tzz_mod.notes.creator", note.ownerName()).getString();
        String shownOwner = textRenderer.trimToWidth(owner, Math.max(1, Math.round(textW / getTextScale())));
        drawScaledText(context, Text.literal(shownTitle), textX, y + s(6), themeText());
        drawScaledText(context, Text.literal(shownOwner), textX, y + s(19), themeTextDim());
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        int mx = (int) click.x();
        int my = (int) click.y();
        if (mx < contentX || mx > contentX + contentWidth || my < listTop() || my > listBottom()) {
            return false;
        }
        List<RowHit> hits = buildHits();
        for (RowHit hit : hits) {
            if (my >= hit.y && my <= hit.y + rowHeight()) {
                if (client != null) {
                    client.setScreen(new PhoneNoteDetailScreen(this, hit.noteId));
                }
                return true;
            }
        }
        return true;
    }

    private List<RowHit> buildHits() {
        List<RowHit> hits = new ArrayList<>();
        int y = listTop() - scrollOffset;
        for (NoteClient.NoteSummaryData note : NoteClient.getNotes()) {
            hits.add(new RowHit(note.noteId(), y));
            y += rowHeight() + rowGap();
        }
        return hits;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isHelpModeActive()) {
            return true;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (mx >= contentX && mx <= contentX + contentWidth && my >= listTop() && my <= listBottom()) {
            scrollOffset -= (int) Math.round(verticalAmount * (rowHeight() + rowGap()));
            clampScrollOffset();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private record RowHit(String noteId, int y) {
    }
}
