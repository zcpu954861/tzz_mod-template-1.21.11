package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.note.NoteClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

public class PhoneNoteConfirmSaveScreen extends AbstractPhoneScreen {
    private final String noteId;
    private final String titleValue;
    private final String contentValue;
    private final long version;
    private final boolean creating;

    public PhoneNoteConfirmSaveScreen(Screen parent, String noteId, String titleValue, String contentValue, long version, boolean creating) {
        super(Text.translatable("phone.tzz_mod.notes.confirm_save_title"), parent);
        this.noteId = noteId;
        this.titleValue = titleValue;
        this.contentValue = contentValue;
        this.version = version;
        this.creating = creating;
    }

    public boolean referencesNote(String removedNoteId) {
        return !creating && noteId.equals(removedNoteId);
    }

    public Screen getNotesHomeScreen() {
        if (parent instanceof PhoneNoteEditScreen editScreen) {
            return editScreen.getNotesHomeScreen();
        }
        return parent == null ? new PhoneNotesAppScreen(null) : parent;
    }

    @Override
    protected void init() {
        super.init();
        int bottomY = contentY + contentHeight - s(24);
        addPhoneButton(Text.translatable("phone.tzz_mod.notes.discard"), contentX, bottomY, s(70), s(20), button -> {
            if (client != null) {
                client.setScreen(creating ? getNotesHomeScreen() : new PhoneNoteDetailScreen(getNotesHomeScreen(), noteId));
            }
        });
        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.notes.save"), contentX + contentWidth - s(70), bottomY, s(70), s(20), button -> {
            if (creating) {
                NoteClient.createNote(titleValue, contentValue);
                if (client != null) {
                    client.setScreen(getNotesHomeScreen());
                }
            } else {
                NoteClient.updateNote(noteId, titleValue, contentValue, version);
                if (client != null) {
                    client.setScreen(new PhoneNoteDetailScreen(getNotesHomeScreen(), noteId));
                }
            }
        });
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.notes.confirm_save_title"), contentX + contentWidth / 2, contentY + s(8));
        List<OrderedText> lines = textRenderer.wrapLines(Text.translatable("phone.tzz_mod.notes.confirm_save_prompt"), Math.max(1, contentWidth - s(12)));
        int y = contentY + s(46);
        for (OrderedText line : lines) {
            context.drawText(textRenderer, line, contentX + s(6), y, themeText(), !isLightMode());
            y += s(12);
        }
    }
}
