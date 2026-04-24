package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.note.NoteClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

public class PhoneNoteConfirmDeleteScreen extends AbstractPhoneScreen {
    private final String noteId;

    public PhoneNoteConfirmDeleteScreen(Screen parent, String noteId) {
        super(Text.translatable("phone.tzz_mod.notes.confirm_delete_title"), parent);
        this.noteId = noteId;
    }

    public boolean referencesNote(String removedNoteId) {
        return noteId.equals(removedNoteId);
    }

    public Screen getNotesHomeScreen() {
        if (parent instanceof PhoneNoteDetailScreen detailScreen) {
            return detailScreen.getNotesHomeScreen();
        }
        return parent == null ? new PhoneNotesAppScreen(null) : parent;
    }

    @Override
    protected void init() {
        super.init();
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(70), s(20), button -> close());
        addPhoneButton(Text.translatable("phone.tzz_mod.notes.delete"), contentX + contentWidth - s(82), contentY + contentHeight - s(24), s(82), s(20), button -> {
            NoteClient.deleteNote(noteId);
            if (client != null) {
                client.setScreen(getNotesHomeScreen());
            }
        });
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.notes.confirm_delete_title"), contentX + contentWidth / 2, contentY + s(8));
        List<OrderedText> lines = textRenderer.wrapLines(Text.translatable("phone.tzz_mod.notes.confirm_delete_prompt"), Math.max(1, contentWidth - s(12)));
        int y = contentY + s(48);
        for (OrderedText line : lines) {
            context.drawText(textRenderer, line, contentX + s(6), y, isLightMode() ? 0xFF9A2E2E : 0xFFFF9A9A, !isLightMode());
            y += s(12);
        }
    }
}
