package com.zcpu.tzzmod.client.phone.ui.app;

import com.zcpu.tzzmod.blocking.BlockingCardConfig;
import com.zcpu.tzzmod.blocking.BlockingCardConfiguratorState;
import com.zcpu.tzzmod.client.blocking.BlockingCardClient;
import com.zcpu.tzzmod.client.phone.ui.AbstractPhoneScreen;
import com.zcpu.tzzmod.client.phone.ui.PhoneButtonWidget;
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
        int fieldHeight = s(18);

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
        addDrawableChild(activationInputField);
        y = activationInputField.getY() + activationInputField.getHeight() + sectionGap;

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
        addDrawableChild(commandField);
        y = commandField.getY() + commandField.getHeight() + sectionGap;

        addPhoneButton(getNotifyOpsText(), contentX, y, contentWidth, s(18), button -> {
            persistStateFromWidgets();
            notifyOps = !notifyOps;
            clearAndInit();
        });
    }

    @Override
    protected void renderPhoneContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawPhoneTextCenteredFixed(context, Text.translatable("phone.tzz_mod.blocking_card.configurator"), contentX + contentWidth / 2, contentY + s(8));

        int y = contentY + s(24);
        for (OrderedText line : textRenderer.wrapLines(Text.translatable("phone.tzz_mod.blocking_card.instructions"), contentWidth)) {
            context.drawTextWithShadow(textRenderer, line, contentX, y, 0xFFBFC7D5);
            y += s(Math.max(10, textRenderer.fontHeight + 2));
        }

        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.blocking_card.current_stack"), contentX, contentY + s(54), 0xFF8BD6FF);
        context.drawTextWithShadow(textRenderer, getStoredCardsText(), contentX, contentY + s(64), 0xFFECECEC);

        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.blocking_card.current_activation", getActivationSummaryText()), contentX, contentY + s(74), 0xFF8BD6FF);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.blocking_card.activation_input"), contentX, activationInputField.getY() - textRenderer.fontHeight - s(4), 0xFFBFC7D5);

        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.blocking_card.current_action", Text.translatable("phone.tzz_mod.blocking_card.action.command")), contentX, commandField.getY() - textRenderer.fontHeight - s(24), 0xFF8BD6FF);
        context.drawTextWithShadow(textRenderer, Text.translatable("phone.tzz_mod.blocking_card.command_input"), contentX, commandField.getY() - textRenderer.fontHeight - s(4), 0xFFBFC7D5);

        if (!feedback.isEmpty()) {
            int feedbackY = contentY + contentHeight - s(44);
            for (OrderedText line : textRenderer.wrapLines(Text.literal(feedback), contentWidth)) {
                context.drawTextWithShadow(textRenderer, line, contentX, feedbackY, feedbackColor);
                feedbackY += s(Math.max(10, textRenderer.fontHeight + 1));
            }
        }
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
}