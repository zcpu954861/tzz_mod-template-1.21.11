package com.zcpu.tzzmod.client.ar.ui.app;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.zcpu.tzzmod.client.ar.ui.AbstractARScreen;
import com.zcpu.tzzmod.client.phone.chat.ChatImageClient;
import com.zcpu.tzzmod.client.phone.chat.ChatUiUtil;
import com.zcpu.tzzmod.client.phone.chat.PhoneChatClient;
import com.zcpu.tzzmod.client.photo.GalleryAvatarRenderer;
import com.zcpu.tzzmod.client.photo.PhotoManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ARChatConversationScreen extends AbstractARScreen {
    private final String type;
    private final String targetId;
    private final List<MessageLayout> cachedLayouts = new ArrayList<>();
    private final List<ImageTarget> imageTargets = new ArrayList<>();

    private String title;
    private Runnable stateListener;
    private TextFieldWidget inputField;
    private int messageScrollOffset;
    private int cachedTotalHeight;
    private boolean layoutsDirty = true;
    private boolean manageButtonVisible;

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
        updateOwnerState();

        int inputHeight = textRenderer.fontHeight;
        int sendWidth = s(46);
        int inputY = contentY + contentHeight - s(16);
        int inputX = contentX + s(2);
        int inputWidth = contentWidth - sendWidth - s(8);

        inputField = new TextFieldWidget(textRenderer, inputX, inputY, inputWidth, inputHeight, Text.translatable("phone.tzz_mod.chat.send"));
        inputField.setMaxLength(MathHelper.clamp(PhoneChatClient.getMaxMessageLength(), 16, 25600));
        styleTextField(inputField);
        addDrawableChild(inputField);
        setFocused(inputField);
        inputField.setFocused(true);

        addARPrimaryButton(Text.translatable("phone.tzz_mod.chat.send"), inputX + inputWidth + s(4), inputY - s(2), sendWidth, s(16), button -> sendMessage());

        if ("group".equals(type)) {
            int actionY = contentY + contentHeight - s(34);
            if (manageButtonVisible) {
                addARButton(Text.translatable("phone.tzz_mod.chat.send_image"), contentX + contentWidth - s(120), actionY, s(52), s(14), button -> openImagePicker());
                addARPrimaryButton(Text.translatable("phone.tzz_mod.chat.manage_members"), contentX + contentWidth - s(64), actionY, s(62), s(14), button -> openManageMembers());
            } else {
                addARButton(Text.translatable("phone.tzz_mod.chat.send_image"), contentX + contentWidth - s(54), actionY, s(52), s(14), button -> openImagePicker());
            }
        }

        stateListener = this::refresh;
        PhoneChatClient.addListener(stateListener);
        PhoneChatClient.setActiveConversation(type, targetId);
        PhoneChatClient.requestHistory(type, targetId);
        refresh();
    }

    private void updateOwnerState() {
        manageButtonVisible = PhoneChatClient.getGroups().stream()
                .anyMatch(group -> group.id().equals(targetId) && group.ownerUuid().equals(PhoneChatClient.getSelfUuid()));
    }

    private void refresh() {
        title = PhoneChatClient.getTitle(type, targetId);
        updateOwnerState();
        layoutsDirty = true;
    }

    private void openImagePicker() {
        if (client != null && "group".equals(type)) {
            client.setScreen(new ARChatImagePickerScreen(this, targetId));
        }
    }

    private void openManageMembers() {
        if (client != null) {
            client.setScreen(new ARChatManageMembersScreen(this, targetId, title));
        }
    }

    public boolean referencesGroup(String groupId) {
        return "group".equals(type) && targetId.equals(groupId);
    }

    public Screen getChatHomeScreen() {
        return parent;
    }

    @Override
    public void removed() {
        super.removed();
        if (stateListener != null) {
            PhoneChatClient.removeListener(stateListener);
            stateListener = null;
        }
        PhoneChatClient.clearActiveConversation(type, targetId);
    }

    private int getMessageAreaTop() {
        return contentY + scaledFontHeight() + s(8);
    }

    private int getMessageAreaBottom() {
        return contentY + contentHeight - ("group".equals(type) ? s(40) : s(18));
    }

    private void rebuildLayouts() {
        cachedLayouts.clear();
        cachedTotalHeight = 0;

        int avatarSize = s(14);
        int bubblePaddingX = s(5);
        int bubblePaddingY = s(4);
        int bubbleGap = s(7);
        int maxBubbleWidth = Math.max(s(36), contentWidth - avatarSize - s(26));
        int innerWrapWidth = Math.max(s(24), maxBubbleWidth - bubblePaddingX * 2);
        int maxImageWidth = innerWrapWidth;
        int maxImageHeight = s(68);
        int lineStep = scaledFontHeight() + s(1);
        String selfUuid = PhoneChatClient.getSelfUuid();

        for (PhoneChatClient.ChatMessageData message : PhoneChatClient.getMessages(type, targetId)) {
            boolean self = !selfUuid.isBlank() && selfUuid.equals(message.senderUuid());
            boolean imageMessage = message.isImage() && message.imageId() != null && !message.imageId().isBlank();
            String content = parseComponentOrLiteral(message.content()).getString();
            List<String> lines = content.isBlank() ? List.of() : wrapText(content, innerWrapWidth);

            int textWidth = s(20);
            for (String line : lines) {
                textWidth = Math.max(textWidth, scaledTextWidth(line));
            }

            int imageWidth = 0;
            int imageHeight = 0;
            if (imageMessage) {
                int[] fitted = ChatUiUtil.fitSize(message.imageWidth() > 0 ? message.imageWidth() : 4,
                        message.imageHeight() > 0 ? message.imageHeight() : 3,
                        maxImageWidth,
                        maxImageHeight);
                imageWidth = fitted[0];
                imageHeight = fitted[1];
            }

            int bubbleWidth = imageMessage
                    ? Math.max(imageWidth + bubblePaddingX * 2, textWidth + bubblePaddingX * 2)
                    : Math.max(s(30), Math.min(maxBubbleWidth, textWidth + bubblePaddingX * 2));
            bubbleWidth = Math.min(maxBubbleWidth, bubbleWidth);

            int bubbleHeight = bubblePaddingY * 2;
            if (imageMessage) {
                bubbleHeight += imageHeight;
                if (!lines.isEmpty()) {
                    bubbleHeight += s(3) + lines.size() * lineStep;
                }
            } else {
                bubbleHeight += Math.max(avatarSize - bubblePaddingY * 2, lines.size() * lineStep);
            }
            bubbleHeight = Math.max(avatarSize, bubbleHeight);

            int labelHeight = scaledFontHeight();
            int bubbleY = labelHeight + s(1);
            int totalHeight = bubbleY + bubbleHeight + bubbleGap;
            int avatarX;
            int bubbleX;
            int labelX;
            String senderId = resolveSenderId(message, self);
            if (self) {
                avatarX = contentX + contentWidth - avatarSize;
                bubbleX = avatarX - s(4) - bubbleWidth;
                labelX = Math.max(contentX, bubbleX + bubbleWidth - scaledTextWidth(senderId));
            } else {
                avatarX = contentX;
                bubbleX = avatarX + avatarSize + s(4);
                labelX = bubbleX;
            }

            cachedLayouts.add(new MessageLayout(
                    message,
                    self,
                    senderId,
                    lines,
                    avatarX,
                    labelX,
                    bubbleX,
                    bubbleY,
                    bubbleWidth,
                    bubbleHeight,
                    totalHeight,
                    imageMessage,
                    bubbleX + bubblePaddingX,
                    bubblePaddingY,
                    imageWidth,
                    imageHeight
            ));
            cachedTotalHeight += totalHeight;
        }

        layoutsDirty = false;
    }

    private void sendMessage() {
        if (inputField == null) {
            return;
        }
        String text = inputField.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        if ("group".equals(type)) {
            PhoneChatClient.sendGroup(targetId, text);
        } else {
            PhoneChatClient.sendDirect(targetId, text);
        }
        inputField.setText("");
        messageScrollOffset = 0;
        layoutsDirty = true;
    }

    @Override
    protected void renderARContent(DrawContext context, int mouseX, int mouseY, float delta) {
        if (layoutsDirty) {
            rebuildLayouts();
        }

        int titleY = contentY + s(2);
        drawScaledCenteredText(context, Text.literal(title), contentX + contentWidth / 2, titleY, themeAccent());

        int listTop = getMessageAreaTop();
        int listBottom = getMessageAreaBottom();
        int maxScroll = Math.max(0, cachedTotalHeight - (listBottom - listTop));
        messageScrollOffset = MathHelper.clamp(messageScrollOffset, 0, maxScroll);

        imageTargets.clear();
        drawARPanelFrame(context, contentX + s(2), listTop, contentWidth - s(4), listBottom - listTop);
        drawARInputFrame(context, inputField.getX() - s(2), inputField.getY() - s(1), inputField.getWidth() + s(4), inputField.getHeight() + s(2));

        context.enableScissor(contentX, listTop, contentX + contentWidth, listBottom);
        int currentY = listBottom - cachedTotalHeight + messageScrollOffset;
        for (MessageLayout layout : cachedLayouts) {
            int blockTop = currentY;
            int blockBottom = blockTop + layout.totalHeight();
            if (blockBottom < listTop || blockTop > listBottom) {
                currentY += layout.totalHeight();
                continue;
            }

            int bubbleY = blockTop + layout.bubbleY();
            int bubbleFill = layout.self()
                    ? (isLightMode() ? 0x99DBEEF8 : 0x66317C9A)
                    : (isLightMode() ? 0x66D8E4F0 : 0x33122030);
            int borderColor = layout.self() ? themeAccent() : themeBorderBright();

            drawScaledPlainText(context, layout.senderId(), layout.labelX(), blockTop, layout.self() ? themeAccent() : themeTextDim());
            GalleryAvatarRenderer.drawAvatar(context, layout.message().senderUuid(), layout.avatarX(), bubbleY, s(14), themeAccent());
            ChatUiUtil.drawAngularFrame(context, layout.bubbleX(), bubbleY, layout.bubbleWidth(), layout.bubbleHeight(), s(3), bubbleFill, borderColor);

            int textY = bubbleY + s(4);
            if (layout.imageMessage()) {
                renderImageBubble(context, layout, bubbleY, borderColor);
                textY = bubbleY + s(4) + layout.imageHeight();
                if (!layout.lines().isEmpty()) {
                    textY += s(3);
                }
            }

            for (String line : layout.lines()) {
                drawScaledPlainText(context, line, layout.bubbleX() + s(5), textY, themeText());
                textY += scaledFontHeight() + s(1);
            }

            currentY += layout.totalHeight();
        }
        context.disableScissor();

        if (inputField != null) {
            renderStyledTextFieldBackground(context, inputField);
            inputField.render(context, mouseX, mouseY, delta);
        }

        renderScrollbar(context, listTop, listBottom, Math.max(listBottom - listTop, cachedTotalHeight), messageScrollOffset);
    }

    private void renderImageBubble(DrawContext context, MessageLayout layout, int bubbleY, int borderColor) {
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
            ChatUiUtil.drawAngularFrame(context, drawX, drawY, drawWidth, drawHeight, s(2), 0x22091420, borderColor);
            drawScaledCenteredText(context, Text.translatable("phone.tzz_mod.chat.image_loading"), drawX + drawWidth / 2, drawY + drawHeight / 2 - scaledFontHeight() / 2, themeTextDim());
        }
        ChatUiUtil.drawAngularFrame(context, drawX - 1, drawY - 1, drawWidth + 2, drawHeight + 2, s(2), 0x00000000, borderColor);
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
                    client.setScreen(new ARChatImageViewScreen(this, target.message()));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= getMessageAreaTop() && mouseY <= getMessageAreaBottom()) {
            messageScrollOffset -= (int) (verticalAmount * s(16));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private Text parseComponentOrLiteral(String raw) {
        if (raw == null || raw.isBlank()) {
            return Text.empty();
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[") && !trimmed.startsWith("\"")) {
            return Text.literal(raw);
        }
        try {
            JsonElement json = JsonParser.parseString(trimmed);
            return TextCodecs.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(Text.literal(raw));
        } catch (Exception ignored) {
            return Text.literal(raw);
        }
    }

    private String resolveSenderId(PhoneChatClient.ChatMessageData message, boolean self) {
        if (self && client != null && client.player != null) {
            return client.player.getName().getString();
        }
        if (message.senderName() != null && !message.senderName().isBlank()) {
            return message.senderName();
        }
        return message.senderUuid() == null || message.senderUuid().isBlank() ? Text.translatable("phone.tzz_mod.unknown").getString() : message.senderUuid();
    }

    private void drawScaledPlainText(DrawContext context, String text, int x, int y, int color) {
        float scale = getTextScale();
        if (scale >= 0.99F) {
            context.drawText(textRenderer, Text.literal(text), x, y, color, false);
            return;
        }
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawText(textRenderer, Text.literal(text), 0, 0, color, false);
        context.getMatrices().popMatrix();
    }

    private record MessageLayout(
            PhoneChatClient.ChatMessageData message,
            boolean self,
            String senderId,
            List<String> lines,
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
