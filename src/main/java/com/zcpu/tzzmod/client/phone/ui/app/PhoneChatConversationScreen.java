package com.zcpu.tzzmod.client.phone.ui.app;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PhoneChatConversationScreen extends AbstractPhoneScreen {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final String type;
    private final String targetId;
    private String title;
    private Runnable stateListener;

    private TextFieldWidget inputField;
    private TextFieldWidget addMemberField;
    private int messageScrollOffset;

    public PhoneChatConversationScreen(Screen parent, String type, String targetId, String title) {
        super(Text.translatable("phone.tzz_mod.chat.conversation"), parent);
        this.type = type;
        this.targetId = targetId;
        this.title = title;
    }

    @Override
    protected void init() {
        super.init();

        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(60), s(20), button -> close());

        inputField = new TextFieldWidget(textRenderer, contentX + s(64), contentY + contentHeight - s(24), contentWidth - s(132), s(20), Text.empty());
        inputField.setMaxLength(net.minecraft.util.math.MathHelper.clamp(com.zcpu.tzzmod.client.phone.chat.PhoneChatClient.getMaxMessageLength(), 16, 25600));
        addDrawableChild(inputField);

        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.chat.send"), contentX + contentWidth - s(64), contentY + contentHeight - s(24), s(64), s(20), button -> sendMessage());

        if ("group".equals(type) && PhoneChatClient.isOp()) {
            addMemberField = new TextFieldWidget(textRenderer, contentX, contentY + contentHeight - s(48), contentWidth - s(70), s(20), Text.empty());
            addMemberField.setMaxLength(64);
            addDrawableChild(addMemberField);

            addPhonePrimaryButton(Text.translatable("phone.tzz_mod.chat.add"), contentX + contentWidth - s(66), contentY + contentHeight - s(48), s(66), s(20), button -> addMember());
        }

        stateListener = this::updateFromState;
        PhoneChatClient.addListener(stateListener);
        PhoneChatClient.setActiveConversation(type, targetId);
        PhoneChatClient.requestHistory(type, targetId);
        updateFromState();
    }

    private void updateFromState() {
        title = PhoneChatClient.getTitle(type, targetId);
        clampMessageScroll();
    }

    private void sendMessage() {
        if (inputField == null) {
            return;
        }
        String rawInput = inputField.getText();
        if (rawInput == null || rawInput.isBlank()) {
            return;
        }

        if ("group".equals(type)) {
            PhoneChatClient.sendGroup(targetId, rawInput);
        } else {
            PhoneChatClient.sendDirect(targetId, rawInput);
        }

        inputField.setText("");
        messageScrollOffset = 0;
    }

    private void addMember() {
        if (addMemberField == null) {
            return;
        }
        String token = addMemberField.getText().trim();
        if (token.isEmpty()) {
            return;
        }

        String memberUuid = PhoneChatClient.resolveUuidByNameOrUuid(token);
        PhoneChatClient.addGroupMember(targetId, memberUuid);
        addMemberField.setText("");
    }

    private int getMessagesTop() {
        return contentY + s(24);
    }

    private int getMessagesBottom() {
        return (addMemberField == null) ? contentY + contentHeight - s(30) : contentY + contentHeight - s(54);
    }

    private int getLineStep() {
        return s(Math.max(10, textRenderer.fontHeight + 2));
    }

    private List<OrderedText> buildWrappedLines() {
        List<OrderedText> lines = new ArrayList<>();
        int wrapWidth = Math.max(s(20), contentWidth - s(6));
        var messages = PhoneChatClient.getMessages(type, targetId);

        for (var message : messages) {
            String hhmm = LocalTime.ofInstant(Instant.ofEpochMilli(message.timestamp()), ZoneId.systemDefault()).format(TIME_FORMATTER);
            MutableText line = Text.literal("[" + hhmm + "] " + message.senderName() + ": ").append(parseComponentOrLiteral(message.content()));
            lines.addAll(textRenderer.wrapLines(line, wrapWidth));
        }

        return lines;
    }

    private Text parseComponentOrLiteral(String raw) {
        if (raw == null || raw.isBlank()) {
            return Text.empty();
        }

        String trimmed = raw.trim();
        if (!looksLikeJsonComponent(trimmed)) {
            return Text.literal(raw);
        }

        try {
            JsonElement json = JsonParser.parseString(trimmed);
            return TextCodecs.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(Text.literal(raw));
        } catch (Exception ignored) {
            return Text.literal(raw);
        }
    }

    private boolean looksLikeJsonComponent(String value) {
        return value.startsWith("{") || value.startsWith("[") || value.startsWith("\"");
    }

    private int getMaxMessageScroll() {
        int visibleHeight = Math.max(1, getMessagesBottom() - getMessagesTop());
        int totalHeight = buildWrappedLines().size() * getLineStep();
        return Math.max(0, totalHeight - visibleHeight);
    }

    private void clampMessageScroll() {
        int maxScroll = getMaxMessageScroll();
        if (messageScrollOffset < 0) {
            messageScrollOffset = 0;
        } else if (messageScrollOffset > maxScroll) {
            messageScrollOffset = maxScroll;
        }
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.literal(title), contentX + contentWidth / 2, contentY + s(8));

        int top = getMessagesTop();
        int bottom = getMessagesBottom();
        int lineStep = getLineStep();

        List<OrderedText> lines = buildWrappedLines();
        int totalHeight = lines.size() * lineStep;
        int visibleHeight = Math.max(1, bottom - top);
        int maxScroll = Math.max(0, totalHeight - visibleHeight);
        if (messageScrollOffset > maxScroll) {
            messageScrollOffset = maxScroll;
        }

        int contentTop = bottom - totalHeight + messageScrollOffset;
        for (int i = 0; i < lines.size(); i++) {
            int y = contentTop + i * lineStep;
            if (y + lineStep < top || y > bottom) {
                continue;
            }
            context.drawTextWithShadow(textRenderer, lines.get(i), contentX + s(2), y, 0xFFECECEC);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        int top = getMessagesTop();
        int bottom = getMessagesBottom();

        if (mx >= contentX && mx <= contentX + contentWidth && my >= top && my <= bottom) {
            messageScrollOffset += (int) Math.round(verticalAmount * s(10));
            clampMessageScroll();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void removed() {
        super.removed();
        PhoneChatClient.clearActiveConversation(type, targetId);
        if (stateListener != null) {
            PhoneChatClient.removeListener(stateListener);
            stateListener = null;
        }
    }
}
