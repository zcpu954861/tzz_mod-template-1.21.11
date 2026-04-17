package com.zcpu.tzzmod.client.phone.ui.app;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.zcpu.tzzmod.client.phone.chat.ChatImageClient;
import com.zcpu.tzzmod.client.phone.chat.ChatUiUtil;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.photo.GalleryAvatarRenderer;
import com.zcpu.tzzmod.client.photo.PhotoManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PhoneChatConversationScreen extends AbstractPhoneScreen {
    private final String type;
    private final String targetId;
    private final List<MessageLayout> cachedLayouts = new ArrayList<>();
    private final List<ImageTarget> imageTargets = new ArrayList<>();

    private String title;
    private Runnable stateListener;
    private TextFieldWidget inputField;
    private int messageScrollOffset;
    private boolean manageButtonVisible;
    private boolean messageLayoutsDirty = true;
    private int cachedTotalHeight;

    public PhoneChatConversationScreen(Screen parent, String type, String targetId, String title) {
        super(Text.translatable("phone.tzz_mod.chat.conversation"), parent);
        this.type = type;
        this.targetId = targetId;
        this.title = title;
    }

    @Override
    protected void init() {
        super.init();
        updateOwnerState();
        invalidateMessageLayouts();

        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, contentY + contentHeight - s(24), s(46), s(20), button -> close());

        int fieldHeight = textRenderer.fontHeight;
        inputField = new TextFieldWidget(textRenderer, contentX + s(50), contentY + contentHeight - s(20), contentWidth - s(108), fieldHeight, Text.empty());
        inputField.setMaxLength(net.minecraft.util.math.MathHelper.clamp(PhoneChatClient.getMaxMessageLength(), 16, 25600));
        styleTextField(inputField);
        addDrawableChild(inputField);

        addPhonePrimaryButton(Text.translatable("phone.tzz_mod.chat.send"), contentX + contentWidth - s(56), contentY + contentHeight - s(24), s(56), s(20), button -> sendMessage());

        if ("group".equals(type)) {
            int actionY = contentY + contentHeight - s(46);
            if (manageButtonVisible) {
                addPhoneButton(Text.translatable("phone.tzz_mod.chat.send_image"), contentX + contentWidth - s(184), actionY, s(58), s(20), button -> openImagePicker());
                addPhonePrimaryButton(Text.translatable("phone.tzz_mod.chat.manage_members"), contentX + contentWidth - s(120), actionY, s(120), s(20), button -> openManageMembers());
            } else {
                addPhoneButton(Text.translatable("phone.tzz_mod.chat.send_image"), contentX + contentWidth - s(72), actionY, s(72), s(20), button -> openImagePicker());
            }
        }

        stateListener = this::updateFromState;
        PhoneChatClient.addListener(stateListener);
        PhoneChatClient.setActiveConversation(type, targetId);
        PhoneChatClient.requestHistory(type, targetId);
        updateFromState();
    }

    private void updateOwnerState() {
        manageButtonVisible = PhoneChatClient.getGroups().stream()
                .anyMatch(group -> group.id().equals(targetId) && group.ownerUuid().equals(PhoneChatClient.getSelfUuid()));
    }

    private void updateFromState() {
        title = PhoneChatClient.getTitle(type, targetId);
        updateOwnerState();
        invalidateMessageLayouts();
        clampMessageScroll();
    }

    private void openImagePicker() {
        if (client != null && "group".equals(type)) {
            client.setScreen(new PhoneChatImagePickerScreen(this, targetId));
        }
    }

    private void openManageMembers() {
        if (client != null) {
            client.setScreen(new PhoneChatManageMembersScreen(this, targetId));
        }
    }

    public boolean referencesGroup(String groupId) {
        return "group".equals(type) && targetId.equals(groupId);
    }

    public Screen getChatHomeScreen() {
        return parent;
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
        return contentY + contentHeight - ("group".equals(type) ? s(52) : s(26));
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
        int bubblePaddingY = s(5);
        int bubbleGap = s(8);
        int maxBubbleWidth = Math.max(s(34), contentWidth - avatarSize - s(24));
        int innerWrapWidth = Math.max(s(20), maxBubbleWidth - bubblePaddingX * 2);
        int maxImageWidth = Math.max(s(28), innerWrapWidth);
        int maxImageHeight = s(78);
        int lineStep = getLineStep();
        int smallTextHeight = Math.max(1, Math.round(textRenderer.fontHeight * 0.75F));
        String selfUuid = PhoneChatClient.getSelfUuid();

        for (PhoneChatClient.ChatMessageData message : PhoneChatClient.getMessages(type, targetId)) {
            boolean self = !selfUuid.isBlank() && selfUuid.equals(message.senderUuid());
            boolean imageMessage = message.isImage() && message.imageId() != null && !message.imageId().isBlank();
            Text content = parseComponentOrLiteral(message.content());
            List<OrderedText> wrappedLines = content.getString().isBlank() ? List.of() : textRenderer.wrapLines(content, innerWrapWidth);

            int textWidth = s(20);
            for (OrderedText wrappedLine : wrappedLines) {
                textWidth = Math.max(textWidth, textRenderer.getWidth(wrappedLine));
            }

            int imageWidth = 0;
            int imageHeight = 0;
            if (imageMessage) {
                int sourceWidth = message.imageWidth() > 0 ? message.imageWidth() : 4;
                int sourceHeight = message.imageHeight() > 0 ? message.imageHeight() : 3;
                int[] fittedSize = ChatUiUtil.fitSize(sourceWidth, sourceHeight, maxImageWidth, maxImageHeight);
                imageWidth = fittedSize[0];
                imageHeight = fittedSize[1];
            }

            int bubbleWidth = imageMessage
                    ? Math.max(textWidth + bubblePaddingX * 2, imageWidth + bubblePaddingX * 2)
                    : Math.max(s(30), Math.min(maxBubbleWidth, textWidth + bubblePaddingX * 2));
            bubbleWidth = Math.min(maxBubbleWidth, bubbleWidth);

            int bubbleHeight = bubblePaddingY * 2;
            if (imageMessage) {
                bubbleHeight += imageHeight;
                if (!wrappedLines.isEmpty()) {
                    bubbleHeight += s(4) + wrappedLines.size() * lineStep;
                }
            } else {
                bubbleHeight += Math.max(avatarSize - bubblePaddingY * 2, wrappedLines.size() * lineStep);
            }
            bubbleHeight = Math.max(avatarSize, bubbleHeight);

            String senderId = resolveSenderId(message, self);
            int labelWidth = getSmallTextWidth(senderId);
            int bubbleY = smallTextHeight + s(2);
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

            int imageX = bubbleX + bubblePaddingX;
            int imageY = bubblePaddingY;
            cachedLayouts.add(new MessageLayout(message, self, senderId, wrappedLines, avatarX, labelX, bubbleX, bubbleY,
                    bubbleWidth, bubbleHeight, totalHeight, imageMessage, imageX, imageY, imageWidth, imageHeight));
            cachedTotalHeight += totalHeight;
        }

        messageLayoutsDirty = false;
    }

    private void invalidateMessageLayouts() {
        messageLayoutsDirty = true;
        cachedTotalHeight = 0;
        imageTargets.clear();
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
        messageScrollOffset = Math.max(0, Math.min(messageScrollOffset, getMaxMessageScroll()));
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.literal(title), contentX + contentWidth / 2, contentY + s(8));
        renderStyledTextFieldBackground(context, inputField);

        int top = getMessagesTop();
        int bottom = getMessagesBottom();
        int lineStep = getLineStep();
        int bubblePaddingX = s(6);
        int bubblePaddingY = s(5);

        imageTargets.clear();
        List<MessageLayout> layouts = getMessageLayouts();
        int visibleHeight = Math.max(1, bottom - top);
        int maxScroll = Math.max(0, cachedTotalHeight - visibleHeight);
        if (messageScrollOffset > maxScroll) {
            messageScrollOffset = maxScroll;
        }

        context.enableScissor(contentX, top, contentX + contentWidth, bottom);
        int currentY = bottom - cachedTotalHeight + messageScrollOffset;
        for (MessageLayout layout : layouts) {
            int blockTop = currentY;
            int blockBottom = blockTop + layout.totalHeight();
            if (blockBottom < top || blockTop > bottom) {
                currentY += layout.totalHeight();
                continue;
            }

            int bubbleY = blockTop + layout.bubbleY();
            int bubbleFill = layout.self()
                    ? (isLightMode() ? 0xA6E3F2FB : 0x66307FA7)
                    : (isLightMode() ? 0x7ADDE6EF : 0x33203042);
            int bubbleBorder = layout.self() ? themeAccent() : themeBorderBright();

            drawSmallText(context, layout.senderId(), layout.labelX(), blockTop, layout.self() ? themeAccent() : themeTextDim());
            GalleryAvatarRenderer.drawAvatar(context, layout.message().senderUuid(), layout.avatarX(), bubbleY, s(12), layout.self() ? themeAccent() : themeBorderBright());
            ChatUiUtil.drawAngularFrame(context, layout.bubbleX(), bubbleY, layout.bubbleWidth(), layout.bubbleHeight(), s(4), bubbleFill, bubbleBorder);

            int textY = bubbleY + bubblePaddingY;
            if (layout.imageMessage()) {
                renderImageBubble(context, layout, bubbleBorder, bubbleY);
                textY = bubbleY + bubblePaddingY + layout.imageHeight();
                if (!layout.lines().isEmpty()) {
                    textY += s(4);
                }
            }

            for (OrderedText wrappedLine : layout.lines()) {
                context.drawText(textRenderer, wrappedLine, layout.bubbleX() + bubblePaddingX, textY, themeText(), false);
                textY += lineStep;
            }

            currentY += layout.totalHeight();
        }
        context.disableScissor();

        renderPhoneScrollbar(context, top, bottom, Math.max(bottom - top, cachedTotalHeight), messageScrollOffset);
    }

    private void renderImageBubble(DrawContext context, MessageLayout layout, int borderColor, int bubbleY) {
        int drawX = layout.imageX();
        int drawY = bubbleY + layout.imageY();
        int drawWidth = layout.imageWidth();
        int drawHeight = layout.imageHeight();
        Identifier textureId = null;
        Path thumbnailPath = ChatImageClient.getThumbnailSourcePath(layout.message().imageId());
        if (thumbnailPath != null && Files.exists(thumbnailPath)) {
            PhotoManager.CachedImage cachedImage = PhotoManager.getThumbnailImage(thumbnailPath, Math.max(drawWidth, drawHeight));
            if (cachedImage.path() != null) {
                textureId = PhotoManager.getOrLoadTexture(cachedImage.path());
            }
        }

        if (textureId != null) {
            context.drawTexturedQuad(textureId, drawX, drawY, drawX + drawWidth, drawY + drawHeight, 0.0F, 1.0F, 0.0F, 1.0F);
        } else {
            ChatUiUtil.drawAngularFrame(context, drawX, drawY, drawWidth, drawHeight, s(3), isLightMode() ? 0x55E8EDF4 : 0x33091320, borderColor);
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.image_loading"), drawX + drawWidth / 2, drawY + drawHeight / 2 - scaledFontHeight() / 2, themeTextDim());
        }
        ChatUiUtil.drawAngularFrame(context, drawX - 1, drawY - 1, drawWidth + 2, drawHeight + 2, s(3), 0x00000000, borderColor);
        imageTargets.add(new ImageTarget(layout.message(), drawX, drawY, drawWidth, drawHeight));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }

        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        for (ImageTarget target : imageTargets) {
            if (mouseX >= target.x() && mouseX <= target.x() + target.width()
                    && mouseY >= target.y() && mouseY <= target.y() + target.height()) {
                if (client != null) {
                    client.setScreen(new PhoneChatImageViewScreen(this, target.message()));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isHelpModeActive()) {
            return true;
        }
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
            return Text.translatable("phone.tzz_mod.unknown").getString();
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
        context.drawText(textRenderer, Text.literal(text), Math.round(x / scale), Math.round(y / scale), color, false);
        context.getMatrices().popMatrix();
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
            int totalHeight,
            boolean imageMessage,
            int imageX,
            int imageY,
            int imageWidth,
            int imageHeight
    ) {
    }

    private record ImageTarget(PhoneChatClient.ChatMessageData message, int x, int y, int width, int height) {
    }
}
