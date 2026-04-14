package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.blocking.BlockingCardConfig;
import com.zcpu.tzzmod.blocking.BlockingCardConfiguratorState;
import com.zcpu.tzzmod.client.blocking.BlockingCardClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.phone.ui.PhoneButtonWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public class BlockingCardConfiguratorScreen extends AbstractPhoneScreen {
    private final Hand hand;
    private BlockingCardConfiguratorState.StoredCards storedCards;

    private TextFieldWidget activationInputField;
    private TextFieldWidget commandField;
    private ButtonWidget saveButton;

    private BlockingCardConfig.ActivationType activationType;
    private boolean notifyOps;
    private boolean showActivationOptions;
    private boolean showActionOptions;
    private String activationInput;
    private String command;
    private String feedback = "";
    private int feedbackColor = 0xFFECECEC;
    private int actionSectionY;

    public BlockingCardConfiguratorScreen(Screen parent, Hand hand, BlockingCardConfiguratorState.StoredCards storedCards) {
        super(Text.translatable("phone.tzz_mod.blocking_card.configurator"), parent);
        this.hand = hand;
        this.storedCards = storedCards;

        BlockingCardConfig.Data config = storedCards == null ? BlockingCardConfig.Data.EMPTY : storedCards.config();
        activationType = config.activationType();
        activationInput = config.activationInput();
        command = config.command();
        notifyOps = config.notifyOps();
    }

    @Override
    protected void init() {
        super.init();

        int buttonY = contentY + contentHeight - s(28);
        addPhoneButton(Text.translatable("phone.tzz_mod.back"), contentX, buttonY, s(68), s(20), button -> close());
        saveButton = addPhoneButton(
            Text.translatable("phone.tzz_mod.blocking_card.save"),
            contentX + contentWidth - s(72),
            buttonY,
            s(72),
            s(20),
            PhoneButtonWidget.Variant.PRIMARY,
            () -> false,
            button -> saveConfiguration()
        );
        saveButton.active = storedCards != null && storedCards.isPresent();

        int y = contentY + s(72);
        int labelGap = s(4);
        int sectionGap = s(10);
        int fieldHeight = textRenderer.fontHeight;

        addPhoneGhostButton(Text.translatable("phone.tzz_mod.blocking_card.select"), contentX + contentWidth - s(52), y, s(52), s(18), button -> {
            persistStateFromWidgets();
            showActivationOptions = !showActivationOptions;
            showActionOptions = false;
            clearAndInit();
        });
        y += s(22);

        if (showActivationOptions) {
            addPhoneTabButton(Text.translatable("phone.tzz_mod.blocking_card.activation.entity"), contentX, y, contentWidth, s(18), () -> activationType == BlockingCardConfig.ActivationType.ENTITY, button -> selectActivationType(BlockingCardConfig.ActivationType.ENTITY));
            y += s(20);
            addPhoneTabButton(Text.translatable("phone.tzz_mod.blocking_card.activation.block"), contentX, y, contentWidth, s(18), () -> activationType == BlockingCardConfig.ActivationType.BLOCK, button -> selectActivationType(BlockingCardConfig.ActivationType.BLOCK));
            y += s(20);
            addPhoneTabButton(Text.translatable("phone.tzz_mod.blocking_card.activation.disabled"), contentX, y, contentWidth, s(18), () -> activationType == BlockingCardConfig.ActivationType.DISABLED, button -> selectActivationType(BlockingCardConfig.ActivationType.DISABLED));
            y += s(24);
        }

        activationInputField = new TextFieldWidget(textRenderer, contentX, y + textRenderer.fontHeight + labelGap, contentWidth, fieldHeight, Text.empty());
        activationInputField.setMaxLength(32767);
        activationInputField.setText(activationInput);
        activationInputField.setPlaceholder(getActivationPlaceholder());
        activationInputField.active = activationType != BlockingCardConfig.ActivationType.DISABLED;
        styleTextField(activationInputField);
        addDrawableChild(activationInputField);
        y = activationInputField.getY() + activationInputField.getHeight() + sectionGap;

        actionSectionY = y;
        addPhoneGhostButton(Text.translatable("phone.tzz_mod.blocking_card.select"), contentX + contentWidth - s(52), y, s(52), s(18), button -> {
            persistStateFromWidgets();
            showActionOptions = !showActionOptions;
            showActivationOptions = false;
            clearAndInit();
        });
        y += s(22);

        if (showActionOptions) {
            addPhoneTabButton(Text.translatable("phone.tzz_mod.blocking_card.action.command"), contentX, y, contentWidth, s(18), () -> true, button -> {
                persistStateFromWidgets();
                showActionOptions = false;
                clearAndInit();
            });
            y += s(24);
        }

        commandField = new TextFieldWidget(textRenderer, contentX, y + textRenderer.fontHeight + labelGap, contentWidth, fieldHeight, Text.empty());
        commandField.setMaxLength(32767);
        commandField.setText(command);
        commandField.setPlaceholder(Text.translatable("phone.tzz_mod.blocking_card.command_placeholder"));
        styleTextField(commandField);
        addDrawableChild(commandField);
        // notifyOps is now rendered as a settings-style row at fixed position in renderPhoneContent
    }

    @Override
    protected boolean hasInitScanAnimation() {
        return true;
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.blocking_card.configurator"), contentX + contentWidth / 2, contentY + s(8));

        int y = contentY + s(24);
        for (OrderedText line : textRenderer.wrapLines(Text.translatable("phone.tzz_mod.blocking_card.instructions"), contentWidth)) {
            context.drawText(textRenderer, line, contentX, y, isLightMode() ? themeTextDim() : 0xFFBFC7D5, !isLightMode());
            y += s(Math.max(10, textRenderer.fontHeight + 2));
        }

        context.drawText(textRenderer, Text.translatable("phone.tzz_mod.blocking_card.current_stack"), contentX, contentY + s(54), isLightMode() ? themeAccent() : 0xFF8BD6FF, !isLightMode());
        context.drawText(textRenderer, getStoredCardsText(), contentX, contentY + s(64), isLightMode() ? themeText() : 0xFFECECEC, !isLightMode());

        context.drawText(textRenderer, Text.translatable("phone.tzz_mod.blocking_card.current_activation", getActivationSummaryText()), contentX, contentY + s(74), isLightMode() ? themeAccent() : 0xFF8BD6FF, !isLightMode());
        context.drawText(textRenderer, Text.translatable("phone.tzz_mod.blocking_card.activation_input"), contentX, activationInputField.getY() - textRenderer.fontHeight - s(4), isLightMode() ? themeTextDim() : 0xFFBFC7D5, !isLightMode());
        renderStyledTextFieldBackground(context, activationInputField);

        context.drawText(textRenderer, Text.translatable("phone.tzz_mod.blocking_card.current_action", Text.translatable("phone.tzz_mod.blocking_card.action.command")), contentX, actionSectionY + s(2), isLightMode() ? themeAccent() : 0xFF8BD6FF, !isLightMode());
        context.drawText(textRenderer, Text.translatable("phone.tzz_mod.blocking_card.command_input"), contentX, commandField.getY() - textRenderer.fontHeight - s(4), isLightMode() ? themeTextDim() : 0xFFBFC7D5, !isLightMode());
        renderStyledTextFieldBackground(context, commandField);

        if (!feedback.isEmpty()) {
            int feedbackY = contentY + contentHeight - s(44);
            for (OrderedText line : textRenderer.wrapLines(Text.literal(feedback), contentWidth)) {
                context.drawText(textRenderer, line, contentX, feedbackY, feedbackColor, !isLightMode());
                feedbackY += s(Math.max(10, textRenderer.fontHeight + 1));
            }
        }

        // NotifyOps settings-style toggle row at fixed position
        int rowH = s(22);
        int rowY = contentY + contentHeight - s(52);
        boolean rowHovered = mouseX >= contentX && mouseX <= contentX + contentWidth
                && mouseY >= rowY && mouseY <= rowY + rowH;
        int chamfer = Math.max(2, s(3));
        int rowBg = rowHovered ? (isLightMode() ? 0x44D8E4F0 : 0x44101825) : (isLightMode() ? 0x33D8E4F0 : 0x33101825);
        fillChamferedRect(context, contentX, rowY, contentWidth, rowH, chamfer, rowBg);
        context.fill(contentX, rowY + chamfer, contentX + 1, rowY + rowH - chamfer,
                notifyOps ? themeAccent() : themeBorder());
        int switchW = s(28);
        int switchH = s(12);
        int switchX = contentX + contentWidth - switchW - s(4);
        int switchY = rowY + (rowH - switchH) / 2;
        float progress = notifyOps ? 1.0F : 0.0F;
        int cut = Math.max(1, switchH / 3);
        int trackFill = isLightMode()
                ? (notifyOps ? 0x330099CC : 0x33C0C8D0)
                : (notifyOps ? 0x3300FFE0 : 0x331A2A3C);
        fillChamferedRect(context, switchX, switchY, switchW, switchH, cut, trackFill);
        int borderCol = notifyOps ? themeAccent() : themeBorder();
        context.fill(switchX + cut, switchY, switchX + switchW, switchY + 1, borderCol);
        context.fill(switchX, switchY + switchH - 1, switchX + switchW - cut, switchY + switchH, borderCol);
        for (int d = 0; d < cut; d++) {
            context.fill(switchX + cut - d, switchY + d, switchX + cut - d + 1, switchY + d + 1, borderCol);
        }
        for (int d = 0; d < cut; d++) {
            context.fill(switchX + switchW - cut + d, switchY + switchH - 1 - d,
                    switchX + switchW - cut + d + 1, switchY + switchH - d, borderCol);
        }
        int knobSize = Math.max(4, switchH - s(4));
        int knobTravel = Math.max(0, switchW - knobSize - s(4));
        int knobX = switchX + s(2) + Math.round(progress * knobTravel);
        int knobY = switchY + (switchH - knobSize) / 2;
        fillChamferedRect(context, knobX, knobY, knobSize, knobSize, Math.max(1, knobSize / 2), 0xFFFFFFFF);
        int labelColor = isLightMode() ? themeText() : 0xFFECECEC;
        context.drawText(textRenderer, getNotifyOpsText(), contentX + s(6),
                rowY + (rowH - textRenderer.fontHeight) / 2, labelColor, !isLightMode());
    }

    public void handleServerResult(boolean success, String message) {
        feedback = message == null ? "" : message;
        feedbackColor = success ? 0xFF7DFFB3 : 0xFFFF9C9C;

        if (success) {
            storedCards = new BlockingCardConfiguratorState.StoredCards(
                    storedCards == null ? "" : storedCards.itemId(),
                    storedCards == null ? 0 : storedCards.count(),
                    new BlockingCardConfig.Data(activationType, activationInput, command, notifyOps)
            );
        }
    }

    private void saveConfiguration() {
        persistStateFromWidgets();
        if (storedCards == null || !storedCards.isPresent()) {
            feedback = Text.translatable("phone.tzz_mod.blocking_card.no_cards").getString();
            feedbackColor = 0xFFFF9C9C;
            return;
        }

        BlockingCardClient.saveConfiguration(hand, activationType.id(), activationInput, command, notifyOps);
        feedback = Text.translatable("phone.tzz_mod.blocking_card.sending").getString();
        feedbackColor = 0xFFECECEC;
    }

    private void selectActivationType(BlockingCardConfig.ActivationType nextType) {
        persistStateFromWidgets();
        activationType = nextType;
        if (activationType == BlockingCardConfig.ActivationType.DISABLED) {
            activationInput = "";
        }
        showActivationOptions = false;
        clearAndInit();
    }

    private void persistStateFromWidgets() {
        if (activationInputField != null) {
            activationInput = activationInputField.getText().trim();
        }
        if (commandField != null) {
            command = commandField.getText().trim();
        }
    }

    private Text getActivationPlaceholder() {
        return switch (activationType) {
            case ENTITY -> Text.translatable("phone.tzz_mod.blocking_card.activation_entity_placeholder");
            case BLOCK -> Text.translatable("phone.tzz_mod.blocking_card.activation_block_placeholder");
            case DISABLED -> Text.translatable("phone.tzz_mod.blocking_card.activation_disabled_placeholder");
        };
    }

    private Text getStoredCardsText() {
        if (storedCards == null || !storedCards.isPresent()) {
            return Text.translatable("phone.tzz_mod.blocking_card.no_cards");
        }

        String display = storedCards.itemId();
        var previewStack = BlockingCardConfiguratorState.extractPreview(storedCards);
        if (!previewStack.isEmpty()) {
            display = previewStack.getName().getString();
        }
        return Text.literal(display + " x" + storedCards.count());
    }

    private Text getActivationSummaryText() {
        return switch (activationType) {
            case ENTITY -> Text.translatable("phone.tzz_mod.blocking_card.activation.entity");
            case BLOCK -> Text.translatable("phone.tzz_mod.blocking_card.activation.block");
            case DISABLED -> Text.translatable("phone.tzz_mod.blocking_card.activation.disabled");
        };
    }

    private Text getNotifyOpsText() {
        String stateKey = notifyOps ? "common.tzz_mod.enabled" : "common.tzz_mod.disabled";
        return Text.translatable("phone.tzz_mod.blocking_card.notify_ops", Text.translatable(stateKey));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        int mx = (int) click.x();
        int my = (int) click.y();
        int rowH = s(22);
        int rowY = contentY + contentHeight - s(52);
        if (mx >= contentX && mx <= contentX + contentWidth && my >= rowY && my <= rowY + rowH) {
            persistStateFromWidgets();
            notifyOps = !notifyOps;
            clearAndInit();
            return true;
        }
        return super.mouseClicked(click, doubleClick);
    }
}