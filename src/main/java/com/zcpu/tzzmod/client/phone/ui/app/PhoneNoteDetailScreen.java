package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.note.NoteClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PhoneNoteDetailScreen extends AbstractPhoneScreen {
    private final String noteId;
    private Runnable stateListener;
    private int scrollOffset;

    public PhoneNoteDetailScreen(Screen parent, String noteId) {
        super(Text.translatable("phone.tzz_mod.notes.detail"), parent);
        this.noteId = noteId;
    }

    public boolean referencesNote(String removedNoteId) {
        return noteId.equals(removedNoteId);
    }

    public Screen getNotesHomeScreen() {
        return parent instanceof PhoneNotesAppScreen ? parent : new PhoneNotesAppScreen(null);
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
        stateListener = () -> {
            clampScrollOffset();
            rebuildButtons();
        };
        NoteClient.addListener(stateListener);
        NoteClient.setActiveNote(noteId);
        NoteClient.requestOpen(noteId);
    }

    private void rebuildButtons() {
        resetPhoneButtonLayer();
        int bottomY = contentY + contentHeight - s(24);
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, bottomY, s(52), s(20), button -> close());
        NoteClient.NoteDetailData detail = NoteClient.getDetail(noteId);
        if (detail != null && detail.canManage()) {
            int buttonW = s(46);
            addPhoneButton(Text.translatable("phone.tzz_mod.notes.edit"), contentX + s(56), bottomY, buttonW, s(20), button -> {
                if (client != null) {
                    client.setScreen(new PhoneNoteEditScreen(this, detail.noteId(), detail.title(), detail.content(), detail.version(), false));
                }
            });
            addPhoneButton(Text.translatable("phone.tzz_mod.notes.share"), contentX + s(106), bottomY, buttonW, s(20), button -> {
                if (client != null) {
                    client.setScreen(new PhoneNoteShareScreen(this, noteId));
                }
            });
            addPhoneButton(Text.translatable("phone.tzz_mod.notes.delete"), contentX + contentWidth - buttonW, bottomY, buttonW, s(20), button -> {
                if (client != null) {
                    client.setScreen(new PhoneNoteConfirmDeleteScreen(this, noteId));
                }
            });
        }
    }

    @Override
    public void removed() {
        super.removed();
        NoteClient.clearActiveNote(noteId);
        if (stateListener != null) {
            NoteClient.removeListener(stateListener);
            stateListener = null;
        }
    }

    private int contentTop() {
        return contentY + s(70);
    }

    private int contentBottom() {
        return contentY + contentHeight - s(32);
    }

    private int lineStep() {
        return s(Math.max(10, textRenderer.fontHeight + 2));
    }

    private List<OrderedText> contentLines() {
        NoteClient.NoteDetailData detail = NoteClient.getDetail(noteId);
        if (detail == null) {
            return textRenderer.wrapLines(Text.translatable("phone.tzz_mod.notes.loading"), Math.max(1, contentWidth - s(8)));
        }
        String body = detail.content().isBlank() ? Text.translatable("phone.tzz_mod.notes.empty_content").getString() : detail.content();
        List<OrderedText> lines = new ArrayList<>();
        String[] rawLines = body.split("\\R", -1);
        int wrapWidth = Math.max(1, contentWidth - s(8));
        for (String raw : rawLines) {
            if (raw.isEmpty()) {
                lines.add(OrderedText.styledForwardsVisitedString("", net.minecraft.text.Style.EMPTY));
            } else {
                lines.addAll(textRenderer.wrapLines(Text.literal(raw), wrapWidth));
            }
        }
        return lines;
    }

    private void clampScrollOffset() {
        int total = contentLines().size() * lineStep();
        int visible = Math.max(1, contentBottom() - contentTop());
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, total - visible)));
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        NoteClient.NoteDetailData detail = NoteClient.getDetail(noteId);
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.notes.detail"), contentX + contentWidth / 2, contentY + s(8));
        if (detail == null) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.notes.loading"), contentX + contentWidth / 2, contentY + s(86), themeTextDim());
            return;
        }

        String title = textRenderer.trimToWidth(detail.title(), Math.max(1, Math.round((contentWidth - s(8)) / getTextScale())));
        drawScaledCenteredText(context, Text.literal(title), contentX + contentWidth / 2, contentY + s(30), relationColor(detail.relation()));
        drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.notes.creator", detail.ownerName()),
                contentX + contentWidth / 2, contentY + s(44), themeTextDim());
        if (!detail.sharedWith().isEmpty()) {
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.notes.shared_count", detail.sharedWith().size()),
                    contentX + contentWidth / 2, contentY + s(56), themeTextDim());
        }

        clampScrollOffset();
        int top = contentTop();
        int bottom = contentBottom();
        int y = top - scrollOffset;
        boolean clipped = bottom > top;
        if (clipped) {
            context.enableScissor(contentX + s(2), top, contentX + contentWidth - s(2), bottom);
        }
        for (OrderedText line : contentLines()) {
            if (y + lineStep() >= top && y <= bottom) {
                context.drawText(textRenderer, line, contentX + s(4), y, themeText(), !isLightMode());
            }
            y += lineStep();
        }
        if (clipped) {
            context.disableScissor();
        }
        renderPhoneScrollbar(context, top, bottom, contentLines().size() * lineStep(), scrollOffset);
    }

    private int relationColor(String relation) {
        return switch (relation) {
            case "owned" -> 0xFF3CB371;
            case "admin" -> 0xFFE6C84F;
            default -> 0xFF4AA3FF;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isHelpModeActive()) {
            return true;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (mx >= contentX && mx <= contentX + contentWidth && my >= contentTop() && my <= contentBottom()) {
            scrollOffset -= (int) Math.round(verticalAmount * lineStep() * 2);
            clampScrollOffset();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
