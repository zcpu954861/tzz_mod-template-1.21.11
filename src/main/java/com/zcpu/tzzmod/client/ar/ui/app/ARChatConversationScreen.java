package com.zcpu.tzzmod.client.ar.ui.app;

import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * AR-style conversation screen: shows message history + send input for one chat/group.
 */
public class ARChatConversationScreen extends AbstractARScreen {
    private final String type;
    private final String targetId;
    private String title;
    private Runnable stateListener;

    private TextFieldWidget inputField;
    private int messageScrollOffset;
    private final List<OrderedText> cachedLines = new ArrayList<>();
    private int cachedTotalHeight;
    private boolean linesDirty = true;

    public ARChatConversationScreen(Screen parent, String type, String targetId, String title) {
        super(Text.literal(title), parent);
        this.type = type;
        this.targetId = targetId;
        this.title = title;
    }

    @Override
    protected void init() {
        super.init();
        addBackButton();

        int inputH = s(18);
        int sendW = s(50);
        int inputY = contentY + contentHeight - inputH - s(1);
        int inputX = contentX + s(2);
        int inputW = contentWidth - sendW - s(6);

        inputField = new TextFieldWidget(textRenderer,
                inputX, inputY, inputW, inputH,
                Text.translatable("phone.tzz_mod.chat.send"));
        inputField.setMaxLength(MathHelper.clamp(PhoneChatClient.getMaxMessageLength(), 16, 25600));
        styleTextField(inputField);
        addDrawableChild(inputField);

        addARPrimaryButton(Text.translatable("phone.tzz_mod.chat.send"),
                inputX + inputW + s(4), inputY, sendW, inputH,
                btn -> sendMessage());

        stateListener = this::refresh;
        PhoneChatClient.addListener(stateListener);
        PhoneChatClient.setActiveConversation(type, targetId);
        PhoneChatClient.requestHistory(type, targetId);
        refresh();
    }

    @Override
    public void removed() {
        super.removed();
        if (stateListener != null) {
            PhoneChatClient.removeListener(stateListener);
            stateListener = null;
        }
        PhoneChatClient.setActiveConversation("", "");
    }

    private void refresh() {
        title = PhoneChatClient.getTitle(type, targetId);
        linesDirty = true;
    }

    private int getMessageAreaTop() {
        return contentY + scaledFontHeight() + s(8);
    }

    private int getMessageAreaBottom() {
        return contentY + contentHeight - s(22);
    }

    private void rebuildLines() {
        cachedLines.clear();
        List<PhoneChatClient.ChatMessageData> messages = PhoneChatClient.getMessages(type, targetId);
        float ts = getTextScale();
        int rawMaxW = ts > 0 ? (int) ((contentWidth - s(10)) / ts) : (contentWidth - s(10));
        for (PhoneChatClient.ChatMessageData msg : messages) {
            String line = msg.senderName() + ": " + msg.content();
            List<OrderedText> wrapped = textRenderer.wrapLines(Text.literal(line), rawMaxW);
            cachedLines.addAll(wrapped);
        }
        int lineH = scaledFontHeight() + s(2);
        cachedTotalHeight = cachedLines.size() * lineH;
        linesDirty = false;
    }

    private void sendMessage() {
        if (inputField == null) return;
        String text = inputField.getText();
        if (text == null || text.isBlank()) return;
        if ("group".equals(type)) {
            PhoneChatClient.sendGroup(targetId, text);
        } else {
            PhoneChatClient.sendDirect(targetId, text);
        }
        inputField.setText("");
        messageScrollOffset = 0;
        linesDirty = true;
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        if (linesDirty) rebuildLines();

        // Title bar
        int titleY = contentY + s(2);
        drawScaledCenteredText(context, Text.literal(title),
                contentX + contentWidth / 2, titleY, themeAccent());

        int inputH = s(18);
        int listTop = getMessageAreaTop();
        int listBottom = getMessageAreaBottom();

        int lineH = scaledFontHeight() + s(2);
        int maxScroll = Math.max(0, cachedTotalHeight - (listBottom - listTop));
        messageScrollOffset = MathHelper.clamp(messageScrollOffset, 0, maxScroll);

        drawARPanelFrame(context, contentX + s(2), listTop, contentWidth - s(4),
                listBottom - listTop);

        context.enableScissor(contentX, listTop, contentX + contentWidth, listBottom);

        float ts = getTextScale();
        int y = listTop + s(2) - messageScrollOffset;
        for (OrderedText line : cachedLines) {
            if (y + lineH > listTop && y < listBottom) {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(contentX + s(5), y);
                context.getMatrices().scale(ts, ts);
                context.drawText(textRenderer, line, 0, 0, themeText(), !isLightMode());
                context.getMatrices().popMatrix();
            }
            y += lineH;
        }

        context.disableScissor();

        // Divider above input
        int divY = listBottom + s(1);
        context.fill(contentX + s(4), divY, contentX + contentWidth - s(4), divY + 1, themeBorder());

        // Input field background frame
        int inputY = contentY + contentHeight - inputH - s(1);
        drawARInputFrame(context, contentX + s(2), inputY - s(1),
                contentWidth - s(4), inputH + s(2));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        messageScrollOffset -= (int) (verticalAmount * s(16));
        return true;
    }
}
