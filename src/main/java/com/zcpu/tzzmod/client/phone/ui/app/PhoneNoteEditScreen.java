package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class PhoneNoteEditScreen extends AbstractPhoneScreen {
    private static final int MAX_CONTENT_LENGTH = 25600;

    private final String noteId;
    private final String originalTitle;
    private final String originalContent;
    private final long version;
    private final boolean creating;

    private TextFieldWidget titleField;
    private String content;
    private int cursor;
    private int scrollOffset;
    private boolean contentFocused = true;

    public PhoneNoteEditScreen(Screen parent, String noteId, String title, String content, long version, boolean creating) {
        super(Text.translatable("phone.tzz_mod.notes.edit"), parent);
        this.noteId = noteId;
        this.originalTitle = title == null ? "" : title;
        this.originalContent = content == null ? "" : content;
        this.content = this.originalContent;
        this.cursor = this.content.length();
        this.version = version;
        this.creating = creating;
    }

    public boolean referencesNote(String removedNoteId) {
        return !creating && noteId.equals(removedNoteId);
    }

    public Screen getNotesHomeScreen() {
        if (parent instanceof PhoneNoteDetailScreen detailScreen) {
            return detailScreen.getNotesHomeScreen();
        }
        return parent instanceof PhoneNotesAppScreen ? parent : new PhoneNotesAppScreen(null);
    }

    @Override
    protected void init() {
        super.init();
        int titleY = contentY + s(34);
        titleField = new TextFieldWidget(textRenderer, contentX + s(4), titleY, contentWidth - s(8), s(16), Text.empty());
        titleField.setMaxLength(64);
        titleField.setText(originalTitle);
        titleField.setPlaceholder(Text.translatable("phone.tzz_mod.notes.title_placeholder"));
        styleTextField(titleField);
        addDrawableChild(titleField);

        int bottomY = contentY + contentHeight - s(24);
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, bottomY, s(66), s(20), button -> attemptExit());
        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.notes.save"), contentX + contentWidth - s(66), bottomY, s(66), s(20), button -> attemptExit());
    }

    private int editorTop() {
        return contentY + s(64);
    }

    private int editorBottom() {
        return contentY + contentHeight - s(32);
    }

    private int lineStep() {
        return s(Math.max(10, textRenderer.fontHeight + 2));
    }

    private int editorInnerX() {
        return contentX + s(7);
    }

    private int editorInnerWidth() {
        return Math.max(1, contentWidth - s(16));
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, creating ? Text.translatable("phone.tzz_mod.notes.create") : Text.translatable("phone.tzz_mod.notes.edit"),
                contentX + contentWidth / 2, contentY + s(8));
        drawScaledText(context, Text.translatable("phone.tzz_mod.notes.title"), contentX + s(4), contentY + s(22), themeTextDim());
        renderStyledTextFieldBackground(context, titleField);

        int boxY = editorTop();
        int boxH = Math.max(1, editorBottom() - editorTop());
        fillChamferedRect(context, contentX + s(2), boxY, contentWidth - s(4), boxH, s(5), isLightMode() ? 0x55F0F4F8 : 0x55101825);
        context.fill(contentX + s(6), boxY + s(1), contentX + contentWidth - s(6), boxY + s(2), contentFocused ? themeAccent() : themeBorder());

        clampScrollOffset();
        List<LineInfo> lines = buildLines();
        int y = editorTop() + s(6) - scrollOffset;
        int clipTop = editorTop() + s(4);
        int clipBottom = editorBottom() - s(2);
        boolean clipped = clipBottom > clipTop;
        if (clipped) {
            context.enableScissor(contentX + s(4), clipTop, contentX + contentWidth - s(4), clipBottom);
        }
        for (int i = 0; i < lines.size(); i++) {
            LineInfo line = lines.get(i);
            if (y + lineStep() >= editorTop() && y <= editorBottom()) {
                String shown = textRenderer.trimToWidth(line.text, Math.max(1, Math.round(editorInnerWidth() / getTextScale())));
                context.drawText(textRenderer, Text.literal(shown), editorInnerX(), y, themeText(), !isLightMode());
                if (contentFocused && isCursorInLine(line)) {
                    int cursorInLine = Math.max(0, Math.min(cursor - line.start, line.text.length()));
                    String before = line.text.substring(0, cursorInLine);
                    int cursorX = editorInnerX() + Math.min(editorInnerWidth(), textRenderer.getWidth(before));
                    context.fill(cursorX, y, cursorX + 1, y + textRenderer.fontHeight, themeAccent());
                }
            }
            y += lineStep();
        }
        if (clipped) {
            context.disableScissor();
        }
        renderPhoneScrollbar(context, editorTop(), editorBottom(), getTotalEditorHeight(), scrollOffset);
    }

    private boolean isCursorInLine(LineInfo line) {
        return cursor >= line.start && cursor <= line.end;
    }

    private List<LineInfo> buildLines() {
        List<LineInfo> lines = new ArrayList<>();
        int start = 0;
        while (start <= content.length()) {
            int nl = content.indexOf('\n', start);
            if (nl < 0) {
                lines.add(new LineInfo(start, content.length(), content.substring(start)));
                break;
            }
            lines.add(new LineInfo(start, nl, content.substring(start, nl)));
            start = nl + 1;
            if (start == content.length()) {
                lines.add(new LineInfo(start, start, ""));
                break;
            }
        }
        if (lines.isEmpty()) {
            lines.add(new LineInfo(0, 0, ""));
        }
        return lines;
    }

    private int getTotalEditorHeight() {
        return buildLines().size() * lineStep() + s(12);
    }

    private void clampScrollOffset() {
        int max = Math.max(0, getTotalEditorHeight() - Math.max(1, editorBottom() - editorTop()));
        scrollOffset = Math.max(0, Math.min(scrollOffset, max));
        ensureCursorVisible();
    }

    private void ensureCursorVisible() {
        int lineIndex = 0;
        List<LineInfo> lines = buildLines();
        for (int i = 0; i < lines.size(); i++) {
            if (isCursorInLine(lines.get(i))) {
                lineIndex = i;
                break;
            }
        }
        int cursorY = s(6) + lineIndex * lineStep();
        int visibleH = Math.max(1, editorBottom() - editorTop());
        if (cursorY - scrollOffset < s(4)) {
            scrollOffset = Math.max(0, cursorY - s(4));
        } else if (cursorY + lineStep() - scrollOffset > visibleH) {
            scrollOffset = Math.max(0, cursorY + lineStep() - visibleH);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        int mx = (int) click.x();
        int my = (int) click.y();
        if (titleField != null
                && mx >= titleField.getX() && mx <= titleField.getX() + titleField.getWidth()
                && my >= titleField.getY() && my <= titleField.getY() + titleField.getHeight()) {
            contentFocused = false;
            return super.mouseClicked(click, doubleClick);
        }
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        if (mx >= contentX && mx <= contentX + contentWidth && my >= editorTop() && my <= editorBottom()) {
            contentFocused = true;
            if (titleField != null) {
                titleField.setFocused(false);
            }
            setCursorFromMouse(mx, my);
            return true;
        }
        contentFocused = false;
        return false;
    }

    private void setCursorFromMouse(int mx, int my) {
        List<LineInfo> lines = buildLines();
        int relativeY = my - editorTop() - s(6) + scrollOffset;
        int lineIndex = MathHelper.clamp(relativeY / Math.max(1, lineStep()), 0, lines.size() - 1);
        LineInfo line = lines.get(lineIndex);
        int relativeX = Math.max(0, mx - editorInnerX());
        int chars = textRenderer.trimToWidth(line.text, relativeX).length();
        cursor = Math.max(line.start, Math.min(line.start + chars, line.end));
        ensureCursorVisible();
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (!contentFocused) {
            return super.charTyped(input);
        }
        if (input.isValidChar()) {
            insert(input.asString());
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            attemptExit();
            return true;
        }
        if (!contentFocused) {
            return super.keyPressed(input);
        }
        if (input.isPaste()) {
            insert(MinecraftClient.getInstance().keyboard.getClipboard());
            return true;
        }
        if (input.isSelectAll()) {
            cursor = content.length();
            return true;
        }
        switch (input.key()) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                eraseBefore();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                eraseAfter();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                insert("\n");
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                cursor = Math.max(0, cursor - 1);
                ensureCursorVisible();
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                cursor = Math.min(content.length(), cursor + 1);
                ensureCursorVisible();
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveCursorVertical(-1);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveCursorVertical(1);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                cursor = currentLine().start;
                ensureCursorVisible();
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                cursor = currentLine().end;
                ensureCursorVisible();
                return true;
            }
            default -> {
                return super.keyPressed(input);
            }
        }
    }

    private void insert(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        int allowed = MAX_CONTENT_LENGTH - content.length();
        if (allowed <= 0) {
            return;
        }
        if (normalized.length() > allowed) {
            normalized = normalized.substring(0, allowed);
        }
        content = content.substring(0, cursor) + normalized + content.substring(cursor);
        cursor += normalized.length();
        ensureCursorVisible();
    }

    private void eraseBefore() {
        if (cursor <= 0) {
            return;
        }
        content = content.substring(0, cursor - 1) + content.substring(cursor);
        cursor--;
        ensureCursorVisible();
    }

    private void eraseAfter() {
        if (cursor >= content.length()) {
            return;
        }
        content = content.substring(0, cursor) + content.substring(cursor + 1);
        ensureCursorVisible();
    }

    private LineInfo currentLine() {
        for (LineInfo line : buildLines()) {
            if (isCursorInLine(line)) {
                return line;
            }
        }
        return new LineInfo(0, content.length(), content);
    }

    private void moveCursorVertical(int direction) {
        List<LineInfo> lines = buildLines();
        int currentIndex = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (isCursorInLine(lines.get(i))) {
                currentIndex = i;
                break;
            }
        }
        LineInfo current = lines.get(currentIndex);
        int column = Math.max(0, cursor - current.start);
        int nextIndex = MathHelper.clamp(currentIndex + direction, 0, lines.size() - 1);
        LineInfo next = lines.get(nextIndex);
        cursor = next.start + Math.min(column, next.text.length());
        ensureCursorVisible();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isHelpModeActive()) {
            return true;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (mx >= contentX && mx <= contentX + contentWidth && my >= editorTop() && my <= editorBottom()) {
            scrollOffset -= (int) Math.round(verticalAmount * lineStep() * 2);
            int max = Math.max(0, getTotalEditorHeight() - Math.max(1, editorBottom() - editorTop()));
            scrollOffset = Math.max(0, Math.min(scrollOffset, max));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        attemptExit();
    }

    private void attemptExit() {
        String titleValue = titleField == null ? originalTitle : titleField.getText().trim();
        if (titleValue.isBlank()) {
            titleValue = Text.translatable("phone.tzz_mod.notes.untitled").getString();
        }
        boolean changed = creating || !titleValue.equals(originalTitle) || !content.equals(originalContent);
        if (!changed) {
            if (client != null) {
                client.setScreen(parent);
            }
            return;
        }
        if (client != null) {
            client.setScreen(new PhoneNoteConfirmSaveScreen(this, noteId, titleValue, content, version, creating));
        }
    }

    private record LineInfo(int start, int end, String text) {
    }
}
