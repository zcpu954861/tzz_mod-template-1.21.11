package com.zcpu.tzzmod.client.webadmin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import com.zcpu.tzzmod.signal.device.item.InteractionItemVanillaPolicy;
import com.zcpu.tzzmod.signal.device.item.InventoryConsumeOrder;
import com.zcpu.tzzmod.signal.device.item.ItemStackDisplaySnapshot;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class WebAdminSingleItemSubmitTemplateScreen extends Screen {
    static final Gson GSON = new Gson();
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_DRAW_SIZE = 16;
    private static final int GRID_COLUMNS = 9;

    private final String sessionId;
    private final String nonce;
    private final String deviceId;
    private final String displayName;
    private final String dimension;
    private final int x;
    private final int y;
    private final int z;
    private final String blockId;
    private final String expectedFingerprint;
    private final long expiresAtMillis;
    private final SubmitTemplate template;
    private boolean closedByServer;
    private boolean cancelSent;
    private boolean cancelConfirmOpen;
    private boolean sessionClosing;
    private boolean saveSent;
    private boolean dirty;
    private String pendingCancelReason = "";
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int templateSlotX;
    private int templateSlotY;
    private int playerGridX;
    private int playerGridY;
    private int hotbarY;
    private final List<ControlButton> controlButtons = new ArrayList<>();
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private String hoveredHint = "";
    private ButtonWidget cancelButton;
    private ButtonWidget saveButton;
    private ButtonWidget continueButton;
    private ButtonWidget confirmCancelButton;
    private String noticeMessage = "";

    private WebAdminSingleItemSubmitTemplateScreen(
            String sessionId,
            String nonce,
            String deviceId,
            String displayName,
            String dimension,
            int x,
            int y,
            int z,
            String blockId,
            String expectedFingerprint,
            long expiresAtMillis,
            SubmitTemplate template
    ) {
        super(Text.literal("单物品提交模板"));
        this.sessionId = safe(sessionId);
        this.nonce = safe(nonce);
        this.deviceId = safe(deviceId);
        this.displayName = safe(displayName);
        this.dimension = safe(dimension);
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockId = safe(blockId);
        this.expectedFingerprint = safe(expectedFingerprint);
        this.expiresAtMillis = expiresAtMillis;
        this.template = template == null ? new SubmitTemplate() : template;
    }

    public static WebAdminSingleItemSubmitTemplateScreen fromJson(JsonObject body) {
        JsonObject template = body != null && body.has("template") && body.get("template").isJsonObject()
                ? body.getAsJsonObject("template")
                : new JsonObject();
        return new WebAdminSingleItemSubmitTemplateScreen(
                getString(body, "sessionId"),
                getString(body, "nonce"),
                getString(body, "deviceId"),
                getString(body, "displayName"),
                getString(body, "dimension"),
                getInt(body, "x", 0),
                getInt(body, "y", 0),
                getInt(body, "z", 0),
                getString(body, "blockId"),
                getString(body, "expectedFingerprint"),
                getLong(body, "expiresAtMillis", 0L),
                SubmitTemplate.fromJson(template)
        );
    }

    public String sessionId() {
        return sessionId;
    }

    public String nonce() {
        return nonce;
    }

    public void closeFromServer() {
        closedByServer = true;
        cancelSent = true;
        sessionClosing = true;
        if (client != null && client.currentScreen == this) {
            client.setScreen(null);
        }
    }

    @Override
    protected void init() {
        panelWidth = Math.min(width - 24, 460);
        panelHeight = Math.min(height - 24, 336);
        panelX = Math.max(12, (width - panelWidth) / 2);
        panelY = Math.max(12, (height - panelHeight) / 2);
        int buttonY = panelY + panelHeight - 24;
        cancelButton = ButtonWidget.builder(Text.literal("取消"), button -> openCancelConfirm("button_cancel")).dimensions(panelX + 12, buttonY, 72, 20).build();
        addDrawableChild(cancelButton);
        saveButton = ButtonWidget.builder(Text.literal("保存模板"), button -> requestSave()).dimensions(panelX + panelWidth - 104, buttonY, 92, 20).build();
        addDrawableChild(saveButton);
        int confirmWidth = Math.min(panelWidth - 48, 300);
        int confirmX = panelX + (panelWidth - confirmWidth) / 2;
        int confirmY = panelY + Math.max(44, (panelHeight - 116) / 2);
        continueButton = ButtonWidget.builder(Text.literal("继续编辑"), button -> cancelCancelConfirm()).dimensions(confirmX + 12, confirmY + 84, 96, 20).build();
        confirmCancelButton = ButtonWidget.builder(Text.literal("确认取消"), button -> confirmCancelSession()).dimensions(confirmX + confirmWidth - 108, confirmY + 84, 96, 20).build();
        addDrawableChild(continueButton);
        addDrawableChild(confirmCancelButton);
        syncButtons();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        hoveredStack = ItemStack.EMPTY;
        hoveredHint = "";
        context.fill(0, 0, width, height, 0xAA030914);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF0C6C6C6);
        context.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + panelHeight - 2, 0xFFE6E6E6);
        context.drawText(textRenderer, title, panelX + 8, panelY + 7, 0xFF303030, false);
        int textY = panelY + 22;
        for (OrderedText line : wrappedInstructions(Math.max(120, panelWidth - 16))) {
            if (textY > panelY + 46) {
                break;
            }
            context.drawText(textRenderer, line, panelX + 8, textY, textY == panelY + 22 ? 0xFF20505D : 0xFF555555, false);
            textY += textRenderer.fontHeight + 1;
        }
        context.drawText(textRenderer, Text.literal("data-single-item-submit-gui-no-overlap"), panelX + panelWidth - 1, panelY + 1, 0x00FFFFFF, false);
        templateSlotX = panelX + 16;
        templateSlotY = panelY + 72;
        renderTemplateSlot(context, mouseX, mouseY);
        buildControlButtons();
        renderControlButtons(context, mouseX, mouseY);
        playerGridX = panelX + 16;
        playerGridY = Math.max(templateSlotY + 106, panelY + panelHeight - 104);
        hotbarY = playerGridY + 3 * SLOT_SIZE + 4;
        renderPlayerInventory(context, mouseX, mouseY);
        renderCursorStack(context, mouseX, mouseY);
        if (!noticeMessage.isBlank()) {
            drawTrimmed(context, noticeMessage, panelX + 12, panelY + panelHeight - 46, panelWidth - 24, 0xFF9D4B00);
        }
        if (cancelConfirmOpen) {
            renderCancelConfirmOverlay(context);
        }
        super.render(context, mouseX, mouseY, delta);
        if (!hoveredStack.isEmpty()) {
            context.drawItemTooltip(textRenderer, hoveredStack, mouseX, mouseY);
        } else if (!hoveredHint.isBlank()) {
            context.drawTooltip(textRenderer, Text.literal(hoveredHint), mouseX, mouseY);
        }
    }

    private void renderTemplateSlot(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, Text.literal("提交物品模板"), templateSlotX, templateSlotY - 10, 0xFF404040, false);
        drawSlot(context, templateSlotX, templateSlotY, 0xFF8B8B8B, 0xFFEFEFEF);
        if (template.hasItem()) {
            renderStack(context, template.stack(), templateSlotX, templateSlotY, mouseX, mouseY, "左键替换模板；右键清空；滚轮调整数量。");
        } else if (inside(mouseX, mouseY, templateSlotX, templateSlotY, SLOT_DRAW_SIZE, SLOT_DRAW_SIZE)) {
            hoveredHint = "从背包拿起物品后左键复制为提交模板。";
        }
        String count = template.hasItem() ? (countModeLabel(template.countMode) + " " + (ContainerItemCountMode.IGNORE.id().equals(template.countMode) ? "不检查" : template.count)) : "未配置";
        drawTrimmed(context, "匹配数量：" + count, templateSlotX + 26, templateSlotY + 2, 170, 0xFF555555);
        String consume = template.consumeEnabled ? ("消耗 " + template.consumeCount + " · " + consumeOrderLabel(template.consumeOrder)) : "提交后不消耗";
        drawTrimmed(context, consume, templateSlotX + 26, templateSlotY + 14, 170, 0xFF555555);
    }

    private void renderPlayerInventory(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, Text.literal("玩家背包"), playerGridX, playerGridY - 10, 0xFF404040, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int invSlot = 9 + row * GRID_COLUMNS + col;
                int sx = playerGridX + col * SLOT_SIZE;
                int sy = playerGridY + row * SLOT_SIZE;
                renderPlayerSlot(context, invSlot, sx, sy, mouseX, mouseY);
            }
        }
        for (int col = 0; col < GRID_COLUMNS; col++) {
            renderPlayerSlot(context, col, playerGridX + col * SLOT_SIZE, hotbarY, mouseX, mouseY);
        }
    }

    private void renderPlayerSlot(DrawContext context, int invSlot, int x, int y, int mouseX, int mouseY) {
        drawSlot(context, x, y, 0xFF8B8B8B, 0xFFEFEFEF);
        renderStack(context, playerStack(invSlot), x, y, mouseX, mouseY, "像原版背包一样点击拿起或放回物品。");
    }

    private void buildControlButtons() {
        controlButtons.clear();
        int startX = panelX + Math.min(220, Math.max(184, panelWidth / 2));
        int startY = templateSlotY - 4;
        int w = Math.max(88, Math.min(112, (panelX + panelWidth - 12 - startX - 8) / 2));
        int gap = 6;
        int h = 16;
        addControl(0, "提交 " + labelBool(template.itemSubmitEnabled), startX, startY, w, h, () -> {
            template.itemSubmitEnabled = !template.itemSubmitEnabled;
            markTemplateDirty();
        }, "itemSubmitEnabled：启用或禁用单物品 itemSubmit。");
        addControl(1, "条件 " + labelBool(template.requirementEnabled), startX + w + gap, startY, w, h, () -> {
            template.requirementEnabled = !template.requirementEnabled;
            markTemplateDirty();
        }, "requirement enabled：是否参与 itemSubmit 判断。");
        addControl(2, "数量 " + countModeLabel(template.countMode), startX, startY + 18, w, h, () -> {
            template.countMode = nextCountMode(template.countMode);
            markTemplateDirty();
        }, "countMode at_least / exactly / at_most / ignore。");
        addControl(3, "原版 " + vanillaPolicyShort(template.vanillaPolicy), startX + w + gap, startY + 18, w, h, () -> {
            template.vanillaPolicy = InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH.equals(InteractionItemVanillaPolicy.normalize(template.vanillaPolicy))
                    ? InteractionItemVanillaPolicy.ALLOW
                    : InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH;
            markTemplateDirty();
        }, "原版交互策略：复用已有 InteractionItemVanillaPolicy，不改运行时语义。");
        addControl(4, "消耗 " + labelBool(template.consumeEnabled), startX, startY + 36, w, h, () -> {
            template.consumeEnabled = !template.consumeEnabled;
            markTemplateDirty();
        }, "itemSubmitConsumeEnabled：提交成功后是否按旧逻辑消耗物品。");
        addControl(5, consumeOrderLabel(template.consumeOrder), startX + w + gap, startY + 36, w, h, () -> {
            template.consumeOrder = InventoryConsumeOrder.MAIN_INVENTORY_FIRST.equals(InventoryConsumeOrder.normalize(template.consumeOrder))
                    ? InventoryConsumeOrder.HOTBAR_FIRST
                    : InventoryConsumeOrder.MAIN_INVENTORY_FIRST;
            markTemplateDirty();
        }, "consumeOrder hotbar_first / main_inventory_first。");
        addControl(6, "消耗-" , startX, startY + 54, w / 2 - 2, h, () -> adjustConsumeCount(-1), "consumeCount 减少。");
        addControl(7, "消耗+" , startX + w / 2 + 2, startY + 54, w / 2 - 2, h, () -> adjustConsumeCount(1), "consumeCount 增加。");
        addControl(8, "damage " + labelBool(template.matchDamage), startX + w + gap, startY + 54, w, h, () -> toggleMatcherOption("damage"), "matchDamage。");
        addControl(9, "名称 " + labelBool(template.matchCustomName), startX, startY + 72, w, h, () -> toggleMatcherOption("name"), "matchCustomName。");
        addControl(10, "Lore " + labelBool(template.matchLore), startX + w + gap, startY + 72, w, h, () -> toggleMatcherOption("lore"), "matchLore。");
        addControl(11, "customData " + labelBool(template.matchCustomData), startX, startY + 90, w, h, () -> toggleMatcherOption("customData"), "matchCustomData。");
        addControl(12, "components " + labelBool(template.matchComponents), startX + w + gap, startY + 90, w, h, () -> toggleMatcherOption("components"), "matchComponents。");
    }

    private void addControl(int order, String label, int x, int y, int width, int height, Runnable action, String hint) {
        controlButtons.add(new ControlButton(order, label, x, y, width, height, action, hint));
    }

    private void renderControlButtons(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, Text.literal("单 requirement 配置"), panelX + Math.min(220, Math.max(184, panelWidth / 2)), templateSlotY - 16, 0xFF404040, false);
        for (ControlButton button : controlButtons) {
            boolean hover = inside(mouseX, mouseY, button.x(), button.y(), button.width(), button.height());
            context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), hover ? 0xFFB8D6E0 : 0xFFBFC7CB);
            context.fill(button.x() + 1, button.y() + 1, button.x() + button.width() - 1, button.y() + button.height() - 1, hover ? 0xFFE8F6FA : 0xFFEFEFEF);
            drawTrimmed(context, button.label(), button.x() + 4, button.y() + 4, button.width() - 8, 0xFF303030);
            if (hover) {
                hoveredHint = button.hint();
            }
        }
        drawTrimmed(context, "匹配数量和消耗数量独立；模板显示堆叠数按物品最大堆叠限制。", panelX + Math.min(220, Math.max(184, panelWidth / 2)), templateSlotY + 110, panelWidth - Math.min(230, Math.max(194, panelWidth / 2)), 0xFF666666);
    }

    private void renderCursorStack(DrawContext context, int mouseX, int mouseY) {
        ItemStack cursor = cursorStack();
        if (!cursor.isEmpty()) {
            context.drawItem(cursor, mouseX - 8, mouseY - 8);
            context.drawStackOverlay(textRenderer, cursor, mouseX - 8, mouseY - 8);
        }
        int x = panelX + 92;
        int y = panelY + panelHeight - 23;
        String label = cursor.isEmpty()
                ? "从下方背包拿起物品，再点提交模板槽复制 ghost。"
                : "鼠标物品：" + Registries.ITEM.getId(cursor.getItem()) + " x" + cursor.getCount();
        drawTrimmed(context, label, x, y + 6, Math.max(80, panelWidth - 210), cursor.isEmpty() ? 0xFF6B6B6B : 0xFF20505D);
    }

    private void drawSlot(DrawContext context, int x, int y, int border, int fill) {
        context.fill(x, y, x + SLOT_DRAW_SIZE, y + SLOT_DRAW_SIZE, border);
        context.fill(x + 1, y + 1, x + SLOT_DRAW_SIZE - 1, y + SLOT_DRAW_SIZE - 1, fill);
    }

    private void renderStack(DrawContext context, ItemStack stack, int x, int y, int mouseX, int mouseY, String hint) {
        if (!stack.isEmpty()) {
            context.drawItem(stack, x, y);
            context.drawStackOverlay(textRenderer, stack, x, y);
        }
        if (inside(mouseX, mouseY, x, y, SLOT_DRAW_SIZE, SLOT_DRAW_SIZE)) {
            if (!stack.isEmpty()) {
                hoveredStack = stack;
            } else {
                hoveredHint = hint;
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        if (cancelConfirmOpen) {
            return true;
        }
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        boolean leftClick = client != null && client.options.attackKey.matchesMouse(click);
        boolean rightClick = client != null && client.options.useKey.matchesMouse(click);
        if (!leftClick && !rightClick) {
            return false;
        }
        if (leftClick) {
            for (ControlButton button : controlButtons) {
                if (inside(mouseX, mouseY, button.x(), button.y(), button.width(), button.height())) {
                    button.action().run();
                    return true;
                }
            }
        }
        if (inside(mouseX, mouseY, templateSlotX, templateSlotY, SLOT_DRAW_SIZE, SLOT_DRAW_SIZE)) {
            if (rightClick) {
                clearTemplate();
            } else {
                copyCursorToTemplate();
            }
            return true;
        }
        int invSlot = playerInventorySlotAt(mouseX, mouseY);
        if (invSlot >= 0) {
            clickPlayerInventorySlot(invSlot, rightClick ? 1 : 0);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inside((int) mouseX, (int) mouseY, templateSlotX, templateSlotY, SLOT_DRAW_SIZE, SLOT_DRAW_SIZE) && template.hasItem()) {
            adjustCount(verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void copyCursorToTemplate() {
        ItemStack cursor = cursorStack();
        if (cursor.isEmpty()) {
            return;
        }
        template.replaceWith(cursor);
        markTemplateDirty();
    }

    private void clearTemplate() {
        if (template.hasItem()) {
            template.clear();
            markTemplateDirty();
        }
    }

    private void adjustCount(double verticalAmount) {
        if (!template.hasItem() || ContainerItemCountMode.IGNORE.id().equals(template.countMode)) {
            return;
        }
        int step = ctrlDown() ? 8 : 1;
        int delta = verticalAmount > 0 ? step : -step;
        template.count = clampOperationalCount(template.count + delta);
        template.syncStackCount();
        markTemplateDirty();
    }

    private void adjustConsumeCount(int delta) {
        template.consumeCount = clampOperationalCount(template.consumeCount + delta);
        markTemplateDirty();
    }

    private void toggleMatcherOption(String option) {
        switch (option) {
            case "damage" -> template.matchDamage = !template.matchDamage;
            case "name" -> template.matchCustomName = !template.matchCustomName;
            case "lore" -> template.matchLore = !template.matchLore;
            case "customData" -> template.matchCustomData = !template.matchCustomData;
            case "components" -> template.matchComponents = !template.matchComponents;
            default -> {
                return;
            }
        }
        markTemplateDirty();
    }

    private void markTemplateDirty() {
        dirty = true;
        syncButtons();
    }

    private ItemStack cursorStack() {
        if (client != null && client.player != null && client.player.currentScreenHandler != null) {
            ItemStack cursor = client.player.currentScreenHandler.getCursorStack();
            return cursor == null ? ItemStack.EMPTY : cursor;
        }
        return ItemStack.EMPTY;
    }

    private void clickPlayerInventorySlot(int inventorySlot, int button) {
        if (client == null || client.player == null || client.interactionManager == null || client.player.currentScreenHandler == null) {
            return;
        }
        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, screenHandlerSlotForInventorySlot(inventorySlot), button, SlotActionType.PICKUP, client.player);
    }

    private int screenHandlerSlotForInventorySlot(int inventorySlot) {
        return inventorySlot >= 0 && inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
    }

    private ItemStack playerStack(int inventorySlot) {
        if (client == null || client.player == null || inventorySlot < 0) {
            return ItemStack.EMPTY;
        }
        try {
            ItemStack stack = client.player.getInventory().getStack(inventorySlot);
            return stack == null ? ItemStack.EMPTY : stack;
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private int playerInventorySlotAt(int mouseX, int mouseY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int invSlot = 9 + row * GRID_COLUMNS + col;
                int sx = playerGridX + col * SLOT_SIZE;
                int sy = playerGridY + row * SLOT_SIZE;
                if (inside(mouseX, mouseY, sx, sy, SLOT_DRAW_SIZE, SLOT_DRAW_SIZE)) {
                    return invSlot;
                }
            }
        }
        for (int col = 0; col < GRID_COLUMNS; col++) {
            int sx = playerGridX + col * SLOT_SIZE;
            if (inside(mouseX, mouseY, sx, hotbarY, SLOT_DRAW_SIZE, SLOT_DRAW_SIZE)) {
                return col;
            }
        }
        return -1;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEscape()) {
            if (cancelConfirmOpen) {
                cancelCancelConfirm();
            } else {
                openCancelConfirm("esc");
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void close() {
        openCancelConfirm("close");
    }

    @Override
    public void removed() {
        if (!closedByServer && !sessionClosing && !cancelSent) {
            requestCancel("screen_removed");
        }
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void requestSave() {
        if (sessionClosing || saveSent) {
            return;
        }
        if (blockIfCursorCarrying("请先把鼠标上的真实物品放回背包，再保存单物品提交模板。")) {
            return;
        }
        saveSent = true;
        sessionClosing = true;
        WebAdminSingleItemSubmitTemplateClient.sendSave(sessionId, nonce, deviceId, expectedFingerprint, template.toPayload());
        syncButtons();
    }

    private void openCancelConfirm(String reason) {
        if (closedByServer || sessionClosing) {
            return;
        }
        if (blockIfCursorCarrying("请先把鼠标上的真实物品放回背包，再取消单物品提交模板编辑。")) {
            return;
        }
        pendingCancelReason = reason == null || reason.isBlank() ? "client_close" : reason;
        cancelConfirmOpen = true;
        syncButtons();
    }

    private void cancelCancelConfirm() {
        cancelConfirmOpen = false;
        pendingCancelReason = "";
        syncButtons();
    }

    private void confirmCancelSession() {
        requestCancel(pendingCancelReason.isBlank() ? "confirm_cancel" : pendingCancelReason);
    }

    private void requestCancel(String reason) {
        if (sessionClosing) {
            return;
        }
        if (blockIfCursorCarrying("请先把鼠标上的真实物品放回背包，再取消单物品提交模板编辑。")) {
            return;
        }
        sessionClosing = true;
        cancelConfirmOpen = true;
        if (!cancelSent) {
            WebAdminSingleItemSubmitTemplateClient.sendCancel(sessionId, nonce, deviceId, reason);
            cancelSent = true;
        }
        syncButtons();
    }

    private void syncButtons() {
        if (cancelButton != null) {
            cancelButton.visible = !cancelConfirmOpen;
            cancelButton.active = !sessionClosing;
        }
        if (saveButton != null) {
            saveButton.visible = !cancelConfirmOpen;
            saveButton.active = !sessionClosing && dirty;
        }
        if (continueButton != null) {
            continueButton.visible = cancelConfirmOpen;
            continueButton.active = !sessionClosing;
        }
        if (confirmCancelButton != null) {
            confirmCancelButton.visible = cancelConfirmOpen;
            confirmCancelButton.active = !sessionClosing;
        }
    }

    private boolean blockIfCursorCarrying(String message) {
        if (!cursorStack().isEmpty()) {
            noticeMessage = message == null || message.isBlank() ? "请先把鼠标上的真实物品放回背包。" : message;
            syncButtons();
            return true;
        }
        noticeMessage = "";
        return false;
    }

    private void renderCancelConfirmOverlay(DrawContext context) {
        context.fill(0, 0, width, height, 0xAA000713);
        int dialogWidth = Math.min(panelWidth - 48, 300);
        int dialogHeight = 116;
        int dialogX = panelX + (panelWidth - dialogWidth) / 2;
        int dialogY = panelY + Math.max(44, (panelHeight - dialogHeight) / 2);
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xF00A1B2A);
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + 1, 0xFFFF6B6B);
        context.drawTextWithShadow(textRenderer, Text.literal("确认取消单物品提交模板？"), dialogX + 12, dialogY + 12, 0xFFFFE7E7);
        String message = sessionClosing ? "正在结束会话，请等待服务端关闭此 GUI。" : "当前模板尚未保存。取消后本次修改会丢失，不会写入配置。";
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(message), Math.max(80, dialogWidth - 24));
        int textY = dialogY + 34;
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            context.drawText(textRenderer, lines.get(i), dialogX + 12, textY, 0xFFB9CAD6, false);
            textY += textRenderer.fontHeight + 2;
        }
    }

    private List<OrderedText> wrappedInstructions(int width) {
        String location = (displayName.isBlank() ? deviceId : displayName) + " · " + dimension + " " + x + " " + y + " " + z + " · " + blockId;
        String line = "单物品 itemSubmit：" + location + "。左键复制模板不消耗，右键清空，滚轮调匹配数量；保存才写入，ESC 取消。";
        return textRenderer.wrapLines(Text.literal(line), Math.max(80, width));
    }

    private void drawTrimmed(DrawContext context, String value, int x, int y, int maxWidth, int color) {
        String shown = textRenderer.trimToWidth(value == null ? "" : value, Math.max(12, maxWidth));
        context.drawText(textRenderer, Text.literal(shown), x, y, color, false);
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean ctrlDown() {
        if (client == null || client.getWindow() == null) {
            return false;
        }
        return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static final class SubmitTemplate {
        private String itemId = "";
        private int count = 1;
        private String countMode = ContainerItemCountMode.AT_LEAST.id();
        private boolean itemSubmitEnabled = true;
        private boolean requirementEnabled = true;
        private boolean matchDamage;
        private boolean matchCustomName;
        private boolean matchLore;
        private boolean matchCustomData;
        private boolean matchComponents;
        private int templateDamage;
        private String templateCustomName = "";
        private List<String> templateLore = List.of();
        private String templateCustomData = "";
        private String templateComponents = "";
        private String templateDisplayStack = "";
        private boolean consumeEnabled;
        private String consumeOrder = InventoryConsumeOrder.HOTBAR_FIRST;
        private int consumeCount = 1;
        private String vanillaPolicy = InteractionItemVanillaPolicy.ALLOW;
        private ItemStack displayStack = ItemStack.EMPTY;

        static SubmitTemplate fromJson(JsonObject object) {
            SubmitTemplate template = new SubmitTemplate();
            template.itemId = fallback(getString(object, "templateItemId"), getString(object, "itemId"));
            template.count = clampOperationalCount(getInt(object, "requiredCount", getInt(object, "count", getInt(object, "templateCount", 1))));
            template.countMode = fallback(getString(object, "countMode"), "at_least");
            template.itemSubmitEnabled = getBoolean(object, "itemSubmitEnabled", true);
            template.requirementEnabled = getBoolean(object, "requirementEnabled", true);
            template.matchDamage = getBoolean(object, "matchDamage", false);
            template.matchCustomName = getBoolean(object, "matchCustomName", false);
            template.matchLore = getBoolean(object, "matchLore", false);
            template.matchCustomData = getBoolean(object, "matchCustomData", false);
            template.matchComponents = getBoolean(object, "matchComponents", false);
            template.templateDamage = Math.max(0, getInt(object, "templateDamage", 0));
            template.templateCustomName = getString(object, "templateCustomName");
            template.templateLore = getStringList(object, "templateLore");
            template.templateCustomData = getString(object, "templateCustomData");
            template.templateComponents = getString(object, "templateComponents");
            template.templateDisplayStack = getString(object, "templateDisplayStack");
            template.consumeEnabled = getBoolean(object, "itemSubmitConsumeEnabled", false);
            template.consumeOrder = InventoryConsumeOrder.normalize(getString(object, "itemSubmitConsumeOrder"));
            template.consumeCount = clampOperationalCount(getInt(object, "consumeCount", 1));
            template.vanillaPolicy = InteractionItemVanillaPolicy.normalize(getString(object, "interactionItemVanillaPolicy"));
            if (ContainerItemCountMode.IGNORE.id().equals(ContainerItemCountMode.normalize(template.countMode))) {
                template.countMode = ContainerItemCountMode.IGNORE.id();
                template.count = Math.max(1, template.count);
            }
            template.displayStack = template.stackFromDisplaySnapshot(template.templateDisplayStack, getInt(object, "templateCount", template.count));
            if (template.displayStack.isEmpty()) {
                template.displayStack = template.stackFromItemId(template.itemId, getInt(object, "templateCount", template.count));
            }
            return template;
        }

        boolean hasItem() {
            return !safe(itemId).isBlank() && !stack().isEmpty();
        }

        ItemStack stack() {
            if (displayStack != null && !displayStack.isEmpty()) {
                return displayStack;
            }
            return stackFromItemId(itemId, count);
        }

        void replaceWith(ItemStack source) {
            itemId = Registries.ITEM.getId(source.getItem()).toString();
            count = clampOperationalCount(source.getCount());
            countMode = ContainerItemCountMode.AT_LEAST.id();
            templateDamage = source.getDamage();
            templateCustomName = ItemStackMatcherSupport.customNameSnapshot(source);
            templateLore = ItemStackMatcherSupport.loreSnapshot(source);
            templateCustomData = ItemStackMatcherSupport.customDataSnapshot(source);
            templateComponents = ItemStackMatcherSupport.componentsSnapshot(source);
            displayStack = source.copy();
            syncStackCount();
            templateDisplayStack = ItemStackDisplaySnapshot.encode(displayStack, registryLookup());
        }

        void clear() {
            itemId = "";
            count = 1;
            countMode = ContainerItemCountMode.AT_LEAST.id();
            requirementEnabled = true;
            matchDamage = false;
            matchCustomName = false;
            matchLore = false;
            matchCustomData = false;
            matchComponents = false;
            templateDamage = 0;
            templateCustomName = "";
            templateLore = List.of();
            templateCustomData = "";
            templateComponents = "";
            templateDisplayStack = "";
            consumeCount = 1;
            displayStack = ItemStack.EMPTY;
        }

        void syncStackCount() {
            if (displayStack != null && !displayStack.isEmpty()) {
                displayStack.setCount(clampStackCount(displayStack, count));
            }
        }

        Map<String, Object> toPayload() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("itemSubmitEnabled", itemSubmitEnabled);
            data.put("requirementEnabled", requirementEnabled);
            data.put("templateItemId", safe(itemId));
            data.put("itemId", safe(itemId));
            data.put("templateCount", hasItem() ? clampStackCount(stack(), count) : 0);
            data.put("count", hasItem() ? count : 0);
            data.put("requiredCount", hasItem() && !ContainerItemCountMode.IGNORE.id().equals(countMode) ? count : 0);
            data.put("countMode", safe(countMode).isBlank() ? ContainerItemCountMode.AT_LEAST.id() : safe(countMode));
            data.put("matchDamage", matchDamage);
            data.put("matchCustomName", matchCustomName);
            data.put("matchLore", matchLore);
            data.put("matchCustomData", matchCustomData);
            data.put("matchComponents", matchComponents);
            data.put("templateDamage", templateDamage);
            data.put("templateCustomName", safe(templateCustomName));
            data.put("templateLore", templateLore == null ? List.of() : List.copyOf(templateLore));
            data.put("templateCustomData", safe(templateCustomData));
            data.put("templateComponents", safe(templateComponents));
            data.put("templateDisplayStack", safe(templateDisplayStack));
            data.put("itemSubmitConsumeEnabled", consumeEnabled);
            data.put("itemSubmitConsumeOrder", InventoryConsumeOrder.normalize(consumeOrder));
            data.put("consumeCount", consumeCount);
            data.put("interactionItemVanillaPolicy", InteractionItemVanillaPolicy.normalize(vanillaPolicy));
            data.put("summary", hasItem() ? safe(itemId) + " x" + count + " · " + ContainerItemCountMode.normalize(countMode) : "");
            return data;
        }

        private ItemStack stackFromItemId(String rawItemId, int rawCount) {
            if (safe(rawItemId).isBlank()) {
                return ItemStack.EMPTY;
            }
            Identifier id = Identifier.tryParse(rawItemId);
            if (id == null) {
                return ItemStack.EMPTY;
            }
            Item item = Registries.ITEM.get(id);
            if (item == null) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = new ItemStack(item, 1);
            stack.setCount(clampStackCount(stack, rawCount));
            return stack;
        }

        private ItemStack stackFromDisplaySnapshot(String snapshot, int rawCount) {
            ItemStack stack = ItemStackDisplaySnapshot.decode(snapshot, registryLookup());
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            stack.setCount(clampStackCount(stack, rawCount));
            return stack;
        }

        private static int clampStackCount(ItemStack stack, int rawCount) {
            if (stack == null || stack.isEmpty()) {
                return Math.max(1, rawCount);
            }
            return Math.max(1, Math.min(Math.max(1, stack.getMaxCount()), rawCount));
        }
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static List<String> getStringList(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return List.of();
        }
        JsonArray array = object.getAsJsonArray(key);
        List<String> result = new ArrayList<>();
        array.forEach(element -> {
            try {
                result.add(element.getAsString());
            } catch (Exception ignored) {
            }
        });
        return List.copyOf(result);
    }

    private static long getLong(JsonObject object, String key, long fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsLong();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? safe(fallback) : value;
    }

    private static int clampOperationalCount(int rawCount) {
        return Math.max(1, Math.min(64_000, rawCount));
    }

    public JsonObject testBridgeSnapshot(boolean includeSlots, boolean includeInventory) {
        JsonObject data = testBridgeBaseSnapshot();
        data.addProperty("itemSubmitEnabled", template.itemSubmitEnabled);
        data.addProperty("requirementEnabled", template.requirementEnabled);
        data.addProperty("countMode", ContainerItemCountMode.normalize(template.countMode));
        data.addProperty("count", template.hasItem() ? template.count : 0);
        data.addProperty("consumeEnabled", template.consumeEnabled);
        data.addProperty("consumeOrder", InventoryConsumeOrder.normalize(template.consumeOrder));
        data.addProperty("consumeCount", template.consumeCount);
        data.addProperty("vanillaPolicy", InteractionItemVanillaPolicy.normalize(template.vanillaPolicy));
        JsonObject matcher = new JsonObject();
        matcher.addProperty("matchDamage", template.matchDamage);
        matcher.addProperty("matchCustomName", template.matchCustomName);
        matcher.addProperty("matchLore", template.matchLore);
        matcher.addProperty("matchCustomData", template.matchCustomData);
        matcher.addProperty("matchComponents", template.matchComponents);
        data.add("matcherOptions", matcher);
        data.add("template", testBridgeTemplateSummary());
        if (includeSlots) {
            JsonArray slots = new JsonArray();
            JsonObject slot = new JsonObject();
            slot.addProperty("slot", 0);
            slot.addProperty("slotId", "submit_template");
            slot.addProperty("editable", true);
            slot.addProperty("empty", !template.hasItem());
            slot.add("item", testBridgeStackSummary(template.stack()));
            slot.addProperty("countMode", ContainerItemCountMode.normalize(template.countMode));
            slot.addProperty("count", template.hasItem() ? template.count : 0);
            slot.addProperty("displayStackCount", template.hasItem() ? template.stack().getCount() : 0);
            slots.add(slot);
            data.add("slots", slots);
        }
        if (includeInventory) {
            data.add("cursor", testBridgeStackSummary(cursorStack()));
        }
        return data;
    }

    public JsonObject testBridgePutItem(String itemId, int count) {
        ItemStack displaySource = testBridgeStack(itemId, count);
        template.replaceWith(displaySource);
        template.count = clampOperationalCount(count);
        template.syncStackCount();
        template.templateDisplayStack = ItemStackDisplaySnapshot.encode(template.displayStack, registryLookup());
        markTemplateDirty();
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "put_item");
        data.addProperty("changedSlot", "submit_template");
        data.addProperty("realInventoryModified", false);
        return data;
    }

    public JsonObject testBridgeClearSlot() {
        clearTemplate();
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "clear_slot");
        data.addProperty("changedSlot", "submit_template");
        data.addProperty("realInventoryModified", false);
        return data;
    }

    public JsonObject testBridgeSetCount(int count) {
        if (!template.hasItem()) {
            throw new IllegalArgumentException("提交模板为空，无法设置数量。");
        }
        template.count = clampOperationalCount(count);
        template.syncStackCount();
        template.templateDisplayStack = ItemStackDisplaySnapshot.encode(template.displayStack, registryLookup());
        markTemplateDirty();
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "set_count");
        data.addProperty("changedSlot", "submit_template");
        data.addProperty("requirementCount", template.count);
        data.addProperty("displayStackCount", template.stack().getCount());
        data.addProperty("realInventoryModified", false);
        return data;
    }

    public JsonObject testBridgeSave() {
        requestSave();
        JsonObject data = testBridgeSnapshot(false, false);
        data.addProperty("action", "save");
        data.addProperty("saveRequested", saveSent);
        data.addProperty("usesExistingSessionSavePath", true);
        return data;
    }

    public JsonObject testBridgeCancel(String reason) {
        requestCancel(safe(reason).isBlank() ? "testbridge_cancel" : reason);
        JsonObject data = testBridgeSnapshot(false, false);
        data.addProperty("action", "cancel");
        data.addProperty("cancelRequested", cancelSent);
        data.addProperty("usesExistingCancelPath", true);
        return data;
    }

    private JsonObject testBridgeBaseSnapshot() {
        JsonObject data = new JsonObject();
        data.addProperty("open", true);
        data.addProperty("supported", true);
        data.addProperty("type", "single_item_submit");
        data.addProperty("sessionId", sessionId);
        data.addProperty("deviceId", deviceId);
        data.addProperty("targetPlayer", client == null || client.player == null ? "" : client.player.getName().getString());
        data.addProperty("title", title.getString());
        data.addProperty("displayName", displayName);
        data.addProperty("dimension", dimension);
        data.addProperty("x", x);
        data.addProperty("y", y);
        data.addProperty("z", z);
        data.addProperty("blockId", blockId);
        data.addProperty("dirty", dirty);
        data.addProperty("sessionClosing", sessionClosing);
        data.addProperty("saveSent", saveSent);
        data.addProperty("cancelSent", cancelSent);
        data.addProperty("cancelConfirmOpen", cancelConfirmOpen);
        data.addProperty("notice", noticeMessage);
        data.addProperty("realInventoryModified", false);
        JsonArray capabilities = new JsonArray();
        capabilities.add("current");
        capabilities.add("slots");
        capabilities.add("put_item");
        capabilities.add("clear_slot");
        capabilities.add("set_count");
        capabilities.add("save");
        capabilities.add("cancel");
        data.add("capabilities", capabilities);
        return data;
    }

    private JsonObject testBridgeTemplateSummary() {
        JsonObject data = new JsonObject();
        data.addProperty("empty", !template.hasItem());
        data.addProperty("itemId", safe(template.itemId));
        data.addProperty("count", template.hasItem() ? template.count : 0);
        data.addProperty("displayStackCount", template.hasItem() ? template.stack().getCount() : 0);
        data.addProperty("countMode", ContainerItemCountMode.normalize(template.countMode));
        data.addProperty("hasDisplaySnapshot", !safe(template.templateDisplayStack).isBlank());
        data.add("item", testBridgeStackSummary(template.stack()));
        return data;
    }

    private ItemStack testBridgeStack(String rawItemId, int rawCount) {
        Identifier id = Identifier.tryParse(safe(rawItemId));
        if (id == null || !Registries.ITEM.containsId(id)) {
            throw new IllegalArgumentException("物品 ID 无效或不存在：" + safe(rawItemId));
        }
        ItemStack stack = new ItemStack(Registries.ITEM.get(id), 1);
        stack.setCount(SubmitTemplate.clampStackCount(stack, rawCount));
        return stack;
    }

    private JsonObject testBridgeStackSummary(ItemStack stack) {
        JsonObject data = new JsonObject();
        data.addProperty("empty", stack == null || stack.isEmpty());
        if (stack == null || stack.isEmpty()) {
            return data;
        }
        data.addProperty("itemId", Registries.ITEM.getId(stack.getItem()).toString());
        data.addProperty("count", stack.getCount());
        data.addProperty("maxCount", Math.max(1, stack.getMaxCount()));
        data.addProperty("displayName", stack.getName().getString());
        return data;
    }

    private static String nextCountMode(String mode) {
        String clean = ContainerItemCountMode.normalize(mode);
        if (ContainerItemCountMode.AT_LEAST.id().equals(clean)) {
            return ContainerItemCountMode.EXACTLY.id();
        }
        if (ContainerItemCountMode.EXACTLY.id().equals(clean)) {
            return ContainerItemCountMode.AT_MOST.id();
        }
        if (ContainerItemCountMode.AT_MOST.id().equals(clean)) {
            return ContainerItemCountMode.IGNORE.id();
        }
        return ContainerItemCountMode.AT_LEAST.id();
    }

    private static String countModeLabel(String mode) {
        return switch (ContainerItemCountMode.fromId(mode)) {
            case EXACTLY -> "等于";
            case AT_MOST -> "至多";
            case IGNORE -> "不检查";
            default -> "至少";
        };
    }

    private static String consumeOrderLabel(String order) {
        return InventoryConsumeOrder.MAIN_INVENTORY_FIRST.equals(InventoryConsumeOrder.normalize(order)) ? "主背包优先" : "热键栏优先";
    }

    private static String vanillaPolicyShort(String policy) {
        return InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH.equals(InteractionItemVanillaPolicy.normalize(policy)) ? "阻断失败" : "允许";
    }

    private static String labelBool(boolean value) {
        return value ? "开" : "关";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static RegistryWrapper.WrapperLookup registryLookup() {
        try {
            if (net.minecraft.client.MinecraftClient.getInstance().world != null) {
                return net.minecraft.client.MinecraftClient.getInstance().world.getRegistryManager();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private record ControlButton(int order, String label, int x, int y, int width, int height, Runnable action, String hint) {
    }
}
