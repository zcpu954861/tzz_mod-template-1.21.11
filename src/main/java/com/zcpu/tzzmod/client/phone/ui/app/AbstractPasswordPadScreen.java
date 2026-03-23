package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.phone.ui.PhoneButtonWidget;
import com.zcpu.tzzmod.client.phone.ui.RoundedRectRenderer;
import com.zcpu.tzzmod.password.PasswordCodeUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public abstract class AbstractPasswordPadScreen extends AbstractPhoneScreen {
    private String inputCode;
    private Text statusText = Text.empty();
    private int statusColor = 0xFFCFD9E6;
    private boolean pendingResponse;

    protected AbstractPasswordPadScreen(Text title, Screen parent, String initialCode) {
        super(title, parent);
        this.inputCode = PasswordCodeUtil.isValid(initialCode) ? initialCode : "";
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();

        int keypadWidth = Math.min(contentWidth - s(8), s(156));
        int buttonHeight = s(26);
        int gap = s(6);
        int keypadX = contentX + (contentWidth - keypadWidth) / 2;
        int buttonWidth = (keypadWidth - gap * 2) / 3;
        int startY = contentY + s(94);

        addDigitButton("1", keypadX, startY, buttonWidth, buttonHeight);
        addDigitButton("2", keypadX + buttonWidth + gap, startY, buttonWidth, buttonHeight);
        addDigitButton("3", keypadX + (buttonWidth + gap) * 2, startY, buttonWidth, buttonHeight);

        startY += buttonHeight + gap;
        addDigitButton("4", keypadX, startY, buttonWidth, buttonHeight);
        addDigitButton("5", keypadX + buttonWidth + gap, startY, buttonWidth, buttonHeight);
        addDigitButton("6", keypadX + (buttonWidth + gap) * 2, startY, buttonWidth, buttonHeight);

        startY += buttonHeight + gap;
        addDigitButton("7", keypadX, startY, buttonWidth, buttonHeight);
        addDigitButton("8", keypadX + buttonWidth + gap, startY, buttonWidth, buttonHeight);
        addDigitButton("9", keypadX + (buttonWidth + gap) * 2, startY, buttonWidth, buttonHeight);

        startY += buttonHeight + gap;
        addPhoneButton(Text.literal("C"), keypadX, startY, buttonWidth, buttonHeight, PhoneButtonWidget.Variant.SECONDARY, () -> false, button -> clearCode());
        addDigitButton("0", keypadX + buttonWidth + gap, startY, buttonWidth, buttonHeight);
        addPhoneButton(Text.literal("←"), keypadX + (buttonWidth + gap) * 2, startY, buttonWidth, buttonHeight, PhoneButtonWidget.Variant.SECONDARY, () -> false, button -> removeLastDigit());

        int bottomY = startY + buttonHeight + gap + s(2);
        int halfWidth = (keypadWidth - gap) / 2;
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), keypadX, bottomY, halfWidth, buttonHeight, PhoneButtonWidget.Variant.SECONDARY, () -> false, button -> close());
        addPhoneButton(Text.translatable(getConfirmTranslationKey()), keypadX + halfWidth + gap, bottomY, halfWidth, buttonHeight, PhoneButtonWidget.Variant.PRIMARY, () -> false, button -> submitCode());
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = contentX + contentWidth / 2;
        drawPhoneTextCenteredFixed(context, Text.translatable(getSubtitleTranslationKey()), centerX, contentY + s(8));
        drawPasswordSlots(context);

        if (!statusText.getString().isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, statusText, centerX, contentY + s(76), statusColor);
        }
    }

    public void handleServerResult(boolean success, String message, boolean closeScreen) {
        pendingResponse = false;
        statusText = Text.literal(message);
        statusColor = success ? 0xFF79F0A0 : 0xFFFF9A9A;
        if (success && closeScreen && client != null) {
            close();
            return;
        }
        if (!success) {
            inputCode = "";
        }
    }

    protected void setStatus(Text text, int color) {
        statusText = text;
        statusColor = color;
    }

    protected void markPending() {
        pendingResponse = true;
    }

    protected abstract void onConfirmCode(String code);

    protected abstract String getSubtitleTranslationKey();

    protected abstract String getConfirmTranslationKey();

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char) input.codepoint();
        if (chr >= '0' && chr <= '9') {
            appendDigit(chr);
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            appendDigit((char) ('0' + (keyCode - GLFW.GLFW_KEY_0)));
            return true;
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            appendDigit((char) ('0' + (keyCode - GLFW.GLFW_KEY_KP_0)));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            removeLastDigit();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            clearCode();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submitCode();
            return true;
        }
        return super.keyPressed(input);
    }

    private void addDigitButton(String digit, int x, int y, int width, int height) {
        addPhoneButton(Text.literal(digit), x, y, width, height, PhoneButtonWidget.Variant.SECONDARY, () -> false, button -> appendDigit(digit.charAt(0)));
    }

    private void appendDigit(char digit) {
        if (pendingResponse || inputCode.length() >= PasswordCodeUtil.CODE_LENGTH) {
            playRejectSound();
            return;
        }
        inputCode = inputCode + digit;
        statusText = Text.empty();
        playKeySound();
    }

    private void removeLastDigit() {
        if (pendingResponse || inputCode.isEmpty()) {
            playRejectSound();
            return;
        }
        inputCode = inputCode.substring(0, inputCode.length() - 1);
        statusText = Text.empty();
        playKeySound();
    }

    private void clearCode() {
        if (pendingResponse || inputCode.isEmpty()) {
            playRejectSound();
            return;
        }
        inputCode = "";
        statusText = Text.empty();
        playKeySound();
    }

    private void submitCode() {
        if (pendingResponse) {
            playRejectSound();
            return;
        }
        if (inputCode.length() != PasswordCodeUtil.CODE_LENGTH) {
            setStatus(Text.translatable("phone.tzz_mod.password.need_four_digits").formatted(Formatting.RED), 0xFFFF8F8F);
            playRejectSound();
            return;
        }
        markPending();
        setStatus(Text.translatable("phone.tzz_mod.password.sending"), 0xFFCFD9E6);
        playKeySound();
        onConfirmCode(inputCode);
    }

    private void drawPasswordSlots(DrawContext context) {
        int boxGap = s(8);
        int boxSize = s(26);
        int totalWidth = boxSize * PasswordCodeUtil.CODE_LENGTH + boxGap * (PasswordCodeUtil.CODE_LENGTH - 1);
        int startX = contentX + (contentWidth - totalWidth) / 2;
        int y = contentY + s(32);
        int radius = Math.max(4, s(6));

        for (int i = 0; i < PasswordCodeUtil.CODE_LENGTH; i++) {
            int x = startX + i * (boxSize + boxGap);
            boolean filled = i < inputCode.length();
            int borderColor = filled ? 0xCCB8F0FF : 0x88DCE8F5;
            int fillColor = filled ? 0xAA2F96D4 : 0x4426303C;
            RoundedRectRenderer.fillRoundedRect(context, x, y, boxSize, boxSize, radius, borderColor);
            RoundedRectRenderer.fillRoundedRect(context, x + 1, y + 1, Math.max(1, boxSize - 2), Math.max(1, boxSize - 2), Math.max(1, radius - 1), fillColor);
            String digit = filled ? String.valueOf(inputCode.charAt(i)) : "_";
            int textColor = filled ? 0xFFFFFFFF : 0xFF93A5B6;
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(digit), x + boxSize / 2, y + MathHelper.floor((boxSize - textRenderer.fontHeight) / 2.0F), textColor);
        }
    }

    private void playKeySound() {
        if (client != null && client.player != null) {
            client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.45F, 1.35F);
        }
    }

    private void playRejectSound() {
        if (client != null && client.player != null) {
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.35F, 0.75F);
        }
    }
}

