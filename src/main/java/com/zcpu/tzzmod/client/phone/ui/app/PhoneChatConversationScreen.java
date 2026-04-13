package com.zcpu.tzzmod.client.phone.ui.app;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.phone.ui.RoundedRectRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PhoneChatConversationScreen extends AbstractPhoneScreen {
    private static final int FACE_U = 8;
    private static final int FACE_V = 8;
    private static final int HAT_U = 40;
    private static final int HAT_V = 8;
    private static final int SKIN_TEXTURE_SIZE = 64;

    private final String type;
    private final String targetId;
    private String title;
    private Runnable stateListener;

    private TextFieldWidget inputField;
    private int messageScrollOffset;
    private boolean manageButtonVisible = false;
    private final List<MessageLayout> cachedLayouts = new ArrayList<>();
    private final Map<String, Identifier> skinTextureCache = new HashMap<>();
    private int cachedTotalHeight;
    private boolean messageLayoutsDirty = true;

    public PhoneChatConversationScreen(Screen parent, String type, String targetId, String title) {
        super(Text.translatable("phone.tzz_mod.chat.conversation"), parent);
        this.type = type;
        this.targetId = targetId;
        this.title = title;
    }

    @Override
    protected void init() {
        super.init();
        invalidateMessageLayouts();

        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(60), s(20), button -> close());

        inputField = new TextFieldWidget(textRenderer, contentX + s(64), contentY + contentHeight - s(24), contentWidth - s(132), s(20), Text.empty());
        inputField.setMaxLength(net.minecraft.util.math.MathHelper.clamp(com.zcpu.tzzmod.client.phone.chat.PhoneChatClient.getMaxMessageLength(), 16, 25600));
        addDrawableChild(inputField);

        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.chat.send"), contentX + contentWidth - s(64), contentY + contentHeight - s(24), s(64), s(20), button -> sendMessage());

        if ("group".equals(type)) {
            // Show Manage Members button to the group owner only (using client-side group metadata)
            boolean isOwner = false;
            for (PhoneChatClient.GroupData g : PhoneChatClient.getGroups()) {
                if (g.id().equals(targetId) && g.ownerUuid().equals(PhoneChatClient.getSelfUuid())) {
                    isOwner = true;
                    break;
                }
            }
            manageButtonVisible = isOwner;
            if (isOwner) {
                // place the Manage Members button up one row so it doesn't overlap the Send button
                addPhonePrimaryButton(Text.translatable("phone.tzz_mod.chat.manage_members"), contentX + contentWidth - s(120), contentY + contentHeight - s(48), s(120), s(20), button -> client.setScreen(new PhoneChatManageMembersScreen(this, targetId)));
            }
        }

        stateListener = this::updateFromState;
        PhoneChatClient.addListener(stateListener);
        PhoneChatClient.setActiveConversation(type, targetId);
        PhoneChatClient.requestHistory(type, targetId);
        updateFromState();
    }

    private void updateFromState() {
        title = PhoneChatClient.getTitle(type, targetId);
        // recompute whether the Manage Members button should be visible (owner may change)
        if ("group".equals(type)) {
            boolean isOwner = false;
            for (PhoneChatClient.GroupData g : PhoneChatClient.getGroups()) {
                if (g.id().equals(targetId) && g.ownerUuid().equals(PhoneChatClient.getSelfUuid())) {
                    isOwner = true;
                    break;
                }
            }
            manageButtonVisible = isOwner;
        } else {
            manageButtonVisible = false;
        }
        invalidateMessageLayouts();
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

    private int getMessagesTop() {
        return contentY + s(24);
    }

    private int getMessagesBottom() {
        // If the Manage Members button is visible, reserve extra vertical space so the button doesn't obscure messages.
        if ("group".equals(type) && manageButtonVisible) {
            return contentY + contentHeight - s(56);
        }
        return contentY + contentHeight - s(30);
    }

    private int getLineStep() {
        return s(Math.max(10, textRenderer.fontHeight + 2));
    }

    private List<MessageLayout> getMessageLayouts() {
        if (messageLayoutsDirty) {
            rebuildMessageLayouts();
        }
        return cachedLayouts;
    }

    private void rebuildMessageLayouts() {
        cachedLayouts.clear();
        cachedTotalHeight = 0;

        int avatarSize = s(12);
        int bubblePaddingX = s(6);
        int bubblePaddingY = s(4);
        int bubbleGap = s(8);
        int maxBubbleWidth = Math.max(s(32), contentWidth - avatarSize - s(22));
        int innerWrapWidth = Math.max(s(20), maxBubbleWidth - bubblePaddingX * 2);
        int lineStep = getLineStep();
        int smallTextHeight = Math.max(1, Math.round(textRenderer.fontHeight * 0.75F));
        var messages = PhoneChatClient.getMessages(type, targetId);
        String selfUuid = PhoneChatClient.getSelfUuid();

        for (PhoneChatClient.ChatMessageData message : messages) {
            boolean self = !selfUuid.isBlank() && selfUuid.equals(message.senderUuid());
            Text content = parseComponentOrLiteral(message.content());
            List<OrderedText> wrappedLines = textRenderer.wrapLines(content, innerWrapWidth);
            int textWidth = s(20);
            for (OrderedText wrappedLine : wrappedLines) {
                textWidth = Math.max(textWidth, textRenderer.getWidth(wrappedLine));
            }

            int bubbleWidth = Math.max(s(30), Math.min(maxBubbleWidth, textWidth + bubblePaddingX * 2));
            int bubbleHeight = Math.max(avatarSize, wrappedLines.size() * lineStep + bubblePaddingY * 2);
            String senderId = resolveSenderId(message, self);
            int labelWidth = getSmallTextWidth(senderId);
            int labelY = 0;
            int bubbleY = labelY + smallTextHeight + s(2);
            int totalHeight = bubbleY + bubbleHeight + bubbleGap;
            int avatarX;
            int bubbleX;
            int labelX;
            if (self) {
                avatarX = contentX + contentWidth - avatarSize;
                bubbleX = avatarX - s(4) - bubbleWidth;
                labelX = Math.max(contentX, bubbleX + bubbleWidth - labelWidth);
            } else {
                avatarX = contentX;
                bubbleX = avatarX + avatarSize + s(4);
                labelX = bubbleX;
            }

            cachedLayouts.add(new MessageLayout(message, self, senderId, wrappedLines, avatarX, labelX, bubbleX, bubbleY, bubbleWidth, bubbleHeight, totalHeight));
            cachedTotalHeight += totalHeight;
        }

        messageLayoutsDirty = false;
    }

    private void invalidateMessageLayouts() {
        messageLayoutsDirty = true;
        cachedTotalHeight = 0;
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
        getMessageLayouts();
        return Math.max(0, cachedTotalHeight - visibleHeight);
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
        int bubblePaddingX = s(6);
        int bubblePaddingY = s(4);
        int avatarSize = s(12);
        List<MessageLayout> layouts = getMessageLayouts();
        int totalHeight = cachedTotalHeight;
        int visibleHeight = Math.max(1, bottom - top);
        int maxScroll = Math.max(0, totalHeight - visibleHeight);
        if (messageScrollOffset > maxScroll) {
            messageScrollOffset = maxScroll;
        }

        int currentY = bottom - totalHeight + messageScrollOffset;
        for (MessageLayout layout : layouts) {
            int blockTop = currentY;
            int blockBottom = blockTop + layout.totalHeight();
            if (blockBottom < top || blockTop > bottom) {
                currentY += layout.totalHeight();
                continue;
            }

            int bubbleColor = layout.self() ? 0xCC2A8FC1 : 0x66313C4B;
            int avatarY = blockTop + layout.bubbleY();
            int bubbleY = blockTop + layout.bubbleY();
            drawSmallText(context, layout.senderId(), layout.labelX(), blockTop, 0xFFB7C7D8);
            drawAvatar(context, layout.message().senderUuid(), layout.self(), layout.avatarX(), avatarY, avatarSize, layout.self() ? 0xFF6EA8FF : 0xFF4D7C9F);
            RoundedRectRenderer.fillRoundedRect(context, layout.bubbleX(), bubbleY, layout.bubbleWidth(), layout.bubbleHeight(), s(8), bubbleColor);

            int textY = bubbleY + bubblePaddingY;
            for (OrderedText wrappedLine : layout.lines()) {
                context.drawTextWithShadow(textRenderer, wrappedLine, layout.bubbleX() + bubblePaddingX, textY, 0xFFF2F5F8);
                textY += lineStep;
            }

            currentY += layout.totalHeight();
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

    private String resolveSenderId(PhoneChatClient.ChatMessageData message, boolean self) {
        if (self && client != null && client.player != null) {
            return client.player.getName().getString();
        }
        if (message.senderName() != null && !message.senderName().isBlank()) {
            return message.senderName();
        }
        if (message.senderUuid() == null || message.senderUuid().isBlank()) {
            return "未知ID";
        }
        return message.senderUuid().length() > 8 ? message.senderUuid().substring(0, 8) : message.senderUuid();
    }

    private int getSmallTextWidth(String text) {
        return Math.max(1, Math.round(textRenderer.getWidth(text) * 0.75F));
    }

    private void drawSmallText(DrawContext context, String text, int x, int y, int color) {
        float scale = 0.75F;
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        context.drawTextWithShadow(textRenderer, Text.literal(text), Math.round(x / scale), Math.round(y / scale), color);
        context.getMatrices().popMatrix();
    }

    private void drawAvatar(DrawContext context, String senderUuid, boolean self, int x, int y, int size, int fallbackColor) {
        Identifier skinTexture = getCachedSkinTexture(senderUuid, self);
        context.fill(x, y, x + size, y + size, 0x66000000);
        if (skinTexture == null) {
            drawAvatarFallback(context, x, y, size, fallbackColor);
            return;
        }
        drawSkinRegion(context, skinTexture, x, y, size, FACE_U, FACE_V);
        drawSkinRegion(context, skinTexture, x, y, size, HAT_U, HAT_V);
    }

    private Identifier getCachedSkinTexture(String senderUuid, boolean self) {
        if (self) {
            return resolveSkinTexture(senderUuid, true);
        }
        if (senderUuid == null || senderUuid.isBlank()) {
            return null;
        }

        Identifier cached = skinTextureCache.get(senderUuid);
        if (cached != null) {
            return cached;
        }

        Identifier resolved = resolveSkinTexture(senderUuid, false);
        if (resolved != null) {
            skinTextureCache.put(senderUuid, resolved);
        }
        return resolved;
    }

    private Identifier resolveSkinTexture(String senderUuid, boolean self) {
        if (client == null) {
            return null;
        }
        if (self && client.player != null) {
            SkinTextures skin = client.player.getSkin();
            return skin.body().texturePath();
        }
        if (client.getNetworkHandler() == null || senderUuid == null || senderUuid.isBlank()) {
            return null;
        }
        try {
            var networkHandler = client.getNetworkHandler();
            if (networkHandler == null) {
                return null;
            }
            var entry = networkHandler.getPlayerListEntry(UUID.fromString(senderUuid));
            if (entry == null) {
                return null;
            }
            SkinTextures skin = entry.getSkinTextures();
            return skin.body().texturePath();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void drawAvatarFallback(DrawContext context, int x, int y, int size, int fallbackColor) {
        context.fill(x, y, x + size, y + size, fallbackColor);
        String initial = "?";
        int textX = x + size / 2 - textRenderer.getWidth(initial) / 2;
        int textY = y + size / 2 - textRenderer.fontHeight / 2;
        context.drawTextWithShadow(textRenderer, initial, textX, textY, 0xFFFFFFFF);
    }

    private void drawSkinRegion(DrawContext context, Identifier texture, int x, int y, int size, int u, int v) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                (float) u,
                (float) v,
                size,
                size,
                8,
                8,
                SKIN_TEXTURE_SIZE,
                SKIN_TEXTURE_SIZE,
                -1
        );
    }

    private record MessageLayout(
            PhoneChatClient.ChatMessageData message,
            boolean self,
            String senderId,
            List<OrderedText> lines,
            int avatarX,
            int labelX,
            int bubbleX,
            int bubbleY,
            int bubbleWidth,
            int bubbleHeight,
            int totalHeight
    ) {
    }
}
