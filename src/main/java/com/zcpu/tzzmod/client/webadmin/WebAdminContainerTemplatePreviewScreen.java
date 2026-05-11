package com.zcpu.tzzmod.client.webadmin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionType;
import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import java.util.ArrayList;
import java.util.Comparator;
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
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class WebAdminContainerTemplatePreviewScreen extends Screen {
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
    private final List<TemplateCondition> conditions;
    private final int slotCount;
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
    private int contentTop;
    private int templateGridX;
    private int templateGridY;
    private int templateRows;
    private int playerGridX;
    private int playerGridY;
    private int hotbarY;
    private int sideX;
    private int sideY;
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private String hoveredHint = "";
    private ButtonWidget cancelButton;
    private ButtonWidget saveButton;
    private ButtonWidget continueButton;
    private ButtonWidget confirmCancelButton;

    private WebAdminContainerTemplatePreviewScreen(
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
            List<TemplateCondition> conditions,
            int slotCount
    ) {
        super(Text.literal("容器内容变化模板"));
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
        this.conditions = new ArrayList<>(conditions == null ? List.of() : conditions);
        this.slotCount = Math.max(27, Math.min(54, slotCount <= 0 ? 27 : ((slotCount + 8) / 9) * 9));
        this.templateRows = Math.max(3, this.slotCount / GRID_COLUMNS);
    }

    public static WebAdminContainerTemplatePreviewScreen fromJson(JsonObject body) {
        List<TemplateCondition> conditions = new ArrayList<>();
        JsonArray array = body != null && body.has("itemConditions") && body.get("itemConditions").isJsonArray()
                ? body.getAsJsonArray("itemConditions")
                : new JsonArray();
        for (JsonElement element : array) {
            if (element != null && element.isJsonObject()) {
                conditions.add(TemplateCondition.fromJson(element.getAsJsonObject()));
            }
        }
        conditions.sort(Comparator.comparing(TemplateCondition::displayZone).thenComparingInt(TemplateCondition::slot).thenComparing(TemplateCondition::id));
        int maxSlot = conditions.stream().filter(TemplateCondition::slotCondition).mapToInt(TemplateCondition::slot).max().orElse(26);
        return new WebAdminContainerTemplatePreviewScreen(
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
                conditions,
                Math.max(27, maxSlot + 1)
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
        templateRows = Math.max(3, Math.min(6, slotCount / GRID_COLUMNS));
        panelWidth = Math.min(width - 24, 520);
        int desiredHeight = 82 + templateRows * SLOT_SIZE + 14 + 4 * SLOT_SIZE + 34;
        panelHeight = Math.min(height - 24, Math.max(246, desiredHeight));
        panelX = Math.max(12, (width - panelWidth) / 2);
        panelY = Math.max(12, (height - panelHeight) / 2);
        int buttonY = panelY + panelHeight - 24;
        cancelButton = ButtonWidget.builder(Text.literal("取消"), button -> openCancelConfirm("button_cancel")).dimensions(panelX + 12, buttonY, 72, 20).build();
        addDrawableChild(cancelButton);
        saveButton = ButtonWidget.builder(Text.literal("保存模板"), button -> requestSave()).dimensions(panelX + panelWidth - 104, buttonY, 92, 20).build();
        addDrawableChild(saveButton);
        int confirmWidth = Math.min(panelWidth - 48, 320);
        int confirmX = panelX + (panelWidth - confirmWidth) / 2;
        int confirmY = panelY + Math.max(46, (panelHeight - 116) / 2);
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

        int textX = panelX + 8;
        int textY = panelY + 22;
        List<OrderedText> instructions = wrappedInstructions(Math.max(120, panelWidth - 16));
        int maxLines = Math.max(2, Math.min(4, (panelHeight - 210) / Math.max(1, textRenderer.fontHeight + 1)));
        for (int i = 0; i < Math.min(maxLines, instructions.size()); i++) {
            context.drawText(textRenderer, instructions.get(i), textX, textY, i == 0 ? 0xFF20505D : 0xFF555555, false);
            textY += textRenderer.fontHeight + 1;
        }
        contentTop = textY + 6;
        layoutAreas();
        renderTemplateGrid(context, mouseX, mouseY);
        renderSideConditions(context, mouseX, mouseY);
        renderPlayerInventory(context, mouseX, mouseY);
        renderCursorStack(context, mouseX, mouseY);
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

    private void layoutAreas() {
        templateGridX = panelX + 8;
        templateGridY = contentTop + 10;
        sideX = templateGridX + GRID_COLUMNS * SLOT_SIZE + 12;
        sideY = templateGridY;
        playerGridX = panelX + 8;
        playerGridY = templateGridY + templateRows * SLOT_SIZE + 18;
        hotbarY = playerGridY + 3 * SLOT_SIZE + 4;
    }

    private void renderTemplateGrid(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, Text.literal("模板槽（ghost）"), templateGridX, templateGridY - 10, 0xFF404040, false);
        for (int slot = 0; slot < slotCount; slot++) {
            int sx = templateGridX + (slot % GRID_COLUMNS) * SLOT_SIZE;
            int sy = templateGridY + (slot / GRID_COLUMNS) * SLOT_SIZE;
            drawSlot(context, sx, sy, 0xFF8B8B8B, 0xFFEFEFEF);
            TemplateCondition condition = conditionForSlot(slot);
            if (condition != null) {
                renderStack(context, condition.stack(), sx, sy, mouseX, mouseY, "左键替换模板；右键清空；滚轮调整数量。");
            } else if (inside(mouseX, mouseY, sx, sy, SLOT_DRAW_SIZE, SLOT_DRAW_SIZE)) {
                hoveredHint = "鼠标拿起物品后左键复制为模板；右键无操作。";
            }
        }
    }

    private void renderSideConditions(DrawContext context, int mouseX, int mouseY) {
        int sideWidth = Math.max(112, panelX + panelWidth - sideX - 10);
        if (sideWidth < 92) {
            return;
        }
        int yCursor = sideY;
        context.drawText(textRenderer, Text.literal("总量条件"), sideX, yCursor - 10, 0xFF20505D, false);
        List<TemplateCondition> totalConditions = conditions.stream().filter(TemplateCondition::totalCondition).toList();
        if (totalConditions.isEmpty()) {
            drawWrapped(context, "总量模板后续支持；当前仅编辑具体槽位模板。", sideX, yCursor, sideWidth, 0xFF666666, 3);
            yCursor += 32;
        } else {
            drawWrapped(context, "以下 total_* 条件按整个容器总数量匹配；P3b 当前只读保留，不在此处编辑。", sideX, yCursor, sideWidth, 0xFF666666, 4);
            yCursor += 40;
            for (TemplateCondition condition : totalConditions) {
                drawTrimmed(context, condition.nameOrType(), sideX, yCursor, sideWidth, 0xFF444444);
                yCursor += 14;
            }
        }
        List<TemplateCondition> advanced = conditions.stream().filter(c -> !c.slotCondition() && !c.totalCondition()).toList();
        if (!advanced.isEmpty()) {
            yCursor += 4;
            context.drawText(textRenderer, Text.literal("高级保留"), sideX, yCursor, 0xFF9A5B00, false);
            yCursor += 12;
            for (TemplateCondition condition : advanced) {
                drawTrimmed(context, condition.nameOrType(), sideX, yCursor, sideWidth, 0xFF555555);
                yCursor += 14;
            }
        }
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
            int sx = playerGridX + col * SLOT_SIZE;
            renderPlayerSlot(context, col, sx, hotbarY, mouseX, mouseY);
        }
    }

    private void renderPlayerSlot(DrawContext context, int invSlot, int x, int y, int mouseX, int mouseY) {
        drawSlot(context, x, y, 0xFF8B8B8B, 0xFFEFEFEF);
        ItemStack stack = playerStack(invSlot);
        renderStack(context, stack, x, y, mouseX, mouseY, "像原版背包一样点击拿起或放回物品。");
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
                ? "从下方背包拿起物品，再点模板槽复制 ghost。"
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
        boolean rightClick = client != null && client.options.useKey.matchesMouse(click);
        boolean leftClick = !rightClick;
        int slot = templateSlotAt(mouseX, mouseY);
        if (slot >= 0) {
            if (rightClick) {
                clearTemplateSlot(slot);
            } else if (leftClick) {
                copySourceToTemplateSlot(slot);
            }
            return true;
        }
        TemplateCondition total = totalConditionAt(mouseX, mouseY);
        if (total != null) {
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
        int slot = templateSlotAt((int) mouseX, (int) mouseY);
        if (slot >= 0) {
            TemplateCondition condition = conditionForSlot(slot);
            if (condition != null && !condition.itemId.isBlank()) {
                adjustCount(condition, verticalAmount);
                return true;
            }
        }
        TemplateCondition total = totalConditionAt((int) mouseX, (int) mouseY);
        if (total != null && !total.itemId.isBlank()) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void adjustCount(TemplateCondition condition, double verticalAmount) {
        if (condition == null || ContainerItemCountMode.IGNORE.id().equals(condition.countMode)) {
            return;
        }
        int step = ctrlDown() ? 8 : 1;
        int delta = verticalAmount > 0 ? step : -step;
        int max = maxCountFor(condition.stack());
        condition.count = Math.max(1, Math.min(max, condition.count + delta));
        condition.syncStackCount();
        dirty = true;
        syncButtons();
    }

    private void copySourceToTemplateSlot(int slot) {
        ItemStack source = sourceStackForTemplate();
        if (source.isEmpty()) {
            return;
        }
        TemplateCondition condition = conditionForSlot(slot);
        if (condition == null) {
            condition = TemplateCondition.newSlotItem(slot, source);
            conditions.add(condition);
        } else {
            condition.replaceWith(source);
        }
        dirty = true;
        syncButtons();
    }

    private void clearTemplateSlot(int slot) {
        TemplateCondition condition = conditionForSlot(slot);
        if (condition != null) {
            conditions.remove(condition);
            dirty = true;
            syncButtons();
        }
    }

    private ItemStack sourceStackForTemplate() {
        ItemStack cursor = cursorStack();
        if (!cursor.isEmpty()) {
            return cursor.copy();
        }
        return ItemStack.EMPTY;
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
        int handlerSlot = screenHandlerSlotForInventorySlot(inventorySlot);
        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, handlerSlot, button, SlotActionType.PICKUP, client.player);
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

    private int templateSlotAt(int mouseX, int mouseY) {
        for (int slot = 0; slot < slotCount; slot++) {
            int sx = templateGridX + (slot % GRID_COLUMNS) * SLOT_SIZE;
            int sy = templateGridY + (slot / GRID_COLUMNS) * SLOT_SIZE;
            if (inside(mouseX, mouseY, sx, sy, SLOT_DRAW_SIZE, SLOT_DRAW_SIZE)) {
                return slot;
            }
        }
        return -1;
    }

    private TemplateCondition totalConditionAt(int mouseX, int mouseY) {
        return null;
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
        saveSent = true;
        sessionClosing = true;
        WebAdminContainerTemplateClient.sendSave(sessionId, nonce, deviceId, expectedFingerprint, payloadConditions());
        syncButtons();
    }

    private List<Map<String, Object>> payloadConditions() {
        List<Map<String, Object>> result = new ArrayList<>();
        conditions.sort(Comparator.comparing(TemplateCondition::displayZone).thenComparingInt(TemplateCondition::slot).thenComparing(TemplateCondition::id));
        for (TemplateCondition condition : conditions) {
            result.add(condition.toPayload());
        }
        return List.copyOf(result);
    }

    private void openCancelConfirm(String reason) {
        if (closedByServer || sessionClosing) {
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
        sessionClosing = true;
        cancelConfirmOpen = true;
        if (!cancelSent) {
            WebAdminContainerTemplateClient.sendCancel(sessionId, nonce, deviceId, reason);
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
            saveButton.active = !sessionClosing;
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

    private void renderCancelConfirmOverlay(DrawContext context) {
        context.fill(0, 0, width, height, 0xAA000713);
        int dialogWidth = Math.min(panelWidth - 48, 320);
        int dialogHeight = 116;
        int dialogX = panelX + (panelWidth - dialogWidth) / 2;
        int dialogY = panelY + Math.max(46, (panelHeight - dialogHeight) / 2);
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xF00A1B2A);
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + 1, 0xFFFF6B6B);
        context.fill(dialogX, dialogY + dialogHeight - 1, dialogX + dialogWidth, dialogY + dialogHeight, 0x88FF6B6B);
        context.fill(dialogX, dialogY, dialogX + 1, dialogY + dialogHeight, 0x88FF6B6B);
        context.fill(dialogX + dialogWidth - 1, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0x88FF6B6B);
        context.drawTextWithShadow(textRenderer, Text.literal("确认取消容器模板编辑？"), dialogX + 12, dialogY + 12, 0xFFFFE7E7);
        String message = sessionClosing
                ? "正在结束会话，请等待服务端关闭此 GUI。"
                : "当前模板编辑尚未保存。取消后本次会话的修改会丢失，不会写入配置。";
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(message), Math.max(80, dialogWidth - 24));
        int textY = dialogY + 34;
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            context.drawText(textRenderer, lines.get(i), dialogX + 12, textY, 0xFFB9CAD6, false);
            textY += textRenderer.fontHeight + 2;
        }
    }

    private List<OrderedText> wrappedInstructions(int width) {
        String location = (displayName.isBlank() ? deviceId : displayName) + " · " + dimension + " " + x + " " + y + " " + z + " · " + blockId;
        String line = "正在编辑 " + location + "。左键复制来源物品为模板，不消耗物品；右键清空模板格；滚轮调整数量，Ctrl+滚轮一次调整 8。Tooltip 仅展示物品信息，实际匹配由 slot/total 与 countMode 决定。ESC 或取消不保存，保存模板才写入 WebAdmin 配置。";
        if (expiresAtMillis > 0L) {
            line += " 会话超时后自动取消。";
        }
        return textRenderer.wrapLines(Text.literal(line), Math.max(80, width));
    }

    private TemplateCondition conditionForSlot(int slot) {
        for (TemplateCondition condition : conditions) {
            if (condition.slotCondition() && condition.slot == slot) {
                return condition;
            }
        }
        return null;
    }

    private void drawTrimmed(DrawContext context, String value, int x, int y, int maxWidth, int color) {
        String shown = textRenderer.trimToWidth(value == null ? "" : value, Math.max(12, maxWidth));
        context.drawText(textRenderer, Text.literal(shown), x, y, color, false);
    }

    private void drawWrapped(DrawContext context, String value, int x, int y, int maxWidth, int color, int maxLines) {
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(value == null ? "" : value), Math.max(24, maxWidth));
        int cursorY = y;
        for (int i = 0; i < Math.min(Math.max(1, maxLines), lines.size()); i++) {
            context.drawText(textRenderer, lines.get(i), x, cursorY, color, false);
            cursorY += textRenderer.fontHeight + 1;
        }
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

    private int maxCountFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 64;
        }
        return Math.max(1, stack.getMaxCount());
    }

    private static final class TemplateCondition {
        private String id;
        private String name;
        private String type;
        private int slot;
        private String itemId;
        private int count;
        private String countMode;
        private String channel;
        private String offChannel;
        private String mode;
        private String displayZone;
        private String summary;
        private boolean enabled;
        private ItemStack displayStack;
        private boolean matcherEnabled;
        private String matcherTemplateItemId;
        private int matcherTemplateCount;
        private String matcherCountMode;
        private int matcherRequiredCount;
        private boolean matcherMatchItemId;
        private boolean matcherMatchDamage;
        private boolean matcherMatchCustomName;
        private boolean matcherMatchLore;
        private boolean matcherMatchCustomData;
        private boolean matcherMatchComponents;
        private int matcherTemplateDamage;
        private String matcherTemplateCustomName;
        private List<String> matcherTemplateLore;
        private String matcherTemplateCustomData;
        private String matcherTemplateComponents;

        static TemplateCondition fromJson(JsonObject object) {
            TemplateCondition condition = new TemplateCondition();
            condition.id = getString(object, "id");
            condition.name = getString(object, "name");
            condition.type = getString(object, "type");
            condition.slot = getInt(object, "slot", 0);
            condition.itemId = fallback(getString(object, "templateItemId"), getString(object, "itemId"));
            condition.count = Math.max(1, getInt(object, "templateCount", getInt(object, "count", 1)));
            condition.countMode = fallback(getString(object, "templateCountMode"), getString(object, "countMode"));
            condition.channel = getString(object, "channel");
            condition.offChannel = getString(object, "offChannel");
            condition.mode = getString(object, "mode");
            condition.displayZone = fallback(getString(object, "displayZone"), zoneForType(condition.type));
            condition.summary = fallback(getString(object, "matcherSummary"), getString(object, "lastResult"));
            condition.enabled = !object.has("enabled") || getBoolean(object, "enabled", true);
            condition.matcherEnabled = getBoolean(object, "matcherEnabled", false);
            condition.matcherTemplateItemId = getString(object, "matcherTemplateItemId");
            condition.matcherTemplateCount = Math.max(1, getInt(object, "matcherTemplateCount", condition.count));
            condition.matcherCountMode = fallback(getString(object, "matcherCountMode"), condition.countMode);
            condition.matcherRequiredCount = Math.max(0, getInt(object, "matcherRequiredCount", condition.count));
            condition.matcherMatchItemId = getBoolean(object, "matcherMatchItemId", true);
            condition.matcherMatchDamage = getBoolean(object, "matcherMatchDamage", false);
            condition.matcherMatchCustomName = getBoolean(object, "matcherMatchCustomName", false);
            condition.matcherMatchLore = getBoolean(object, "matcherMatchLore", false);
            condition.matcherMatchCustomData = getBoolean(object, "matcherMatchCustomData", false);
            condition.matcherMatchComponents = getBoolean(object, "matcherMatchComponents", false);
            condition.matcherTemplateDamage = Math.max(0, getInt(object, "matcherTemplateDamage", 0));
            condition.matcherTemplateCustomName = getString(object, "matcherTemplateCustomName");
            condition.matcherTemplateLore = getStringList(object, "matcherTemplateLore");
            condition.matcherTemplateCustomData = getString(object, "matcherTemplateCustomData");
            condition.matcherTemplateComponents = getString(object, "matcherTemplateComponents");
            condition.displayStack = condition.stackFromItemId(condition.itemId, condition.count);
            if (condition.slotCondition() && !condition.displayStack.isEmpty()) {
                condition.count = condition.displayStack.getCount();
            }
            return condition;
        }

        static TemplateCondition newSlotItem(int slot, ItemStack source) {
            TemplateCondition condition = new TemplateCondition();
            String itemId = Registries.ITEM.getId(source.getItem()).toString();
            condition.id = "slot-" + slot;
            condition.name = "Slot " + slot + " " + itemId;
            condition.type = ContainerItemConditionType.SLOT_ITEM.id();
            condition.slot = slot;
            condition.itemId = itemId;
            condition.count = clampStackCount(source, source.getCount());
            condition.countMode = ContainerItemCountMode.AT_LEAST.id();
            condition.displayZone = "slot";
            condition.enabled = true;
            condition.summary = "";
            condition.channel = "";
            condition.offChannel = "";
            condition.mode = "";
            condition.matcherTemplateLore = List.of();
            condition.displayStack = source.copy();
            condition.syncStackCount();
            return condition;
        }

        boolean slotCondition() {
            return "slot".equals(displayZone);
        }

        boolean totalCondition() {
            return "total".equals(displayZone);
        }

        String displayZone() {
            return displayZone == null ? "" : displayZone;
        }

        int slot() {
            return slot;
        }

        String id() {
            return id == null ? "" : id;
        }

        String nameOrType() {
            String base = safe(name).isBlank() ? type : name;
            String extra = safe(summary).isBlank() ? (safe(itemId).isBlank() ? countMode : itemId + " · " + countMode + " " + count) : summary;
            return safe(extra).isBlank() ? safe(base) : safe(base) + " · " + safe(extra);
        }

        ItemStack stack() {
            if (displayStack != null && !displayStack.isEmpty()) {
                return displayStack;
            }
            return stackFromItemId(itemId, count);
        }

        void replaceWith(ItemStack source) {
            String nextItemId = Registries.ITEM.getId(source.getItem()).toString();
            itemId = nextItemId;
            count = slotCondition() ? clampStackCount(source, source.getCount()) : Math.max(1, source.getCount());
            if (safe(type).isBlank() || ContainerItemConditionType.SLOT_EMPTY.id().equals(type)) {
                type = slotCondition() ? ContainerItemConditionType.SLOT_ITEM.id() : ContainerItemConditionType.TOTAL_ITEM.id();
            }
            if (safe(countMode).isBlank()) {
                countMode = ContainerItemCountMode.AT_LEAST.id();
            }
            displayStack = source.copy();
            syncStackCount();
            if (safe(name).isBlank()) {
                name = slotCondition() ? "Slot " + slot + " " + itemId : "Total " + itemId;
            }
        }

        void syncStackCount() {
            if (displayStack != null && !displayStack.isEmpty()) {
                displayStack.setCount(slotCondition() ? clampStackCount(displayStack, count) : Math.max(1, count));
            }
        }

        Map<String, Object> toPayload() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", safe(id));
            data.put("name", safe(name));
            data.put("enabled", enabled);
            data.put("type", safe(type));
            data.put("slot", slot);
            data.put("itemId", safe(itemId));
            data.put("countMode", safe(countMode).isBlank() ? ContainerItemCountMode.AT_LEAST.id() : safe(countMode));
            int payloadCount = slotCondition() ? clampStackCount(stack(), count) : Math.max(0, count);
            data.put("count", payloadCount);
            data.put("channel", safe(channel));
            data.put("offChannel", safe(offChannel));
            data.put("mode", safe(mode));
            data.put("matcherEnabled", matcherEnabled);
            data.put("matcherTemplateItemId", safe(matcherTemplateItemId));
            data.put("matcherTemplateCount", slotCondition() ? clampStackCount(stack(), Math.max(1, matcherTemplateCount)) : Math.max(1, matcherTemplateCount));
            data.put("matcherCountMode", safe(matcherCountMode).isBlank() ? safe(countMode) : safe(matcherCountMode));
            data.put("matcherRequiredCount", Math.max(0, matcherRequiredCount));
            data.put("matcherMatchItemId", matcherMatchItemId);
            data.put("matcherMatchDamage", matcherMatchDamage);
            data.put("matcherMatchCustomName", matcherMatchCustomName);
            data.put("matcherMatchLore", matcherMatchLore);
            data.put("matcherMatchCustomData", matcherMatchCustomData);
            data.put("matcherMatchComponents", matcherMatchComponents);
            data.put("matcherTemplateDamage", Math.max(0, matcherTemplateDamage));
            data.put("matcherTemplateCustomName", safe(matcherTemplateCustomName));
            data.put("matcherTemplateLore", matcherTemplateLore == null ? List.of() : matcherTemplateLore);
            data.put("matcherTemplateCustomData", safe(matcherTemplateCustomData));
            data.put("matcherTemplateComponents", safe(matcherTemplateComponents));
            data.put("matcherSummary", safe(summary));
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

        private static int clampStackCount(ItemStack stack, int rawCount) {
            if (stack == null || stack.isEmpty()) {
                return Math.max(1, rawCount);
            }
            return Math.max(1, Math.min(Math.max(1, stack.getMaxCount()), rawCount));
        }
    }

    private static String zoneForType(String type) {
        String value = safe(type);
        if (value.startsWith("slot_")) {
            return "slot";
        }
        if (value.startsWith("total_")) {
            return "total";
        }
        return "advanced";
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
        List<String> result = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray(key)) {
            if (element != null && !element.isJsonNull()) {
                try {
                    result.add(element.getAsString());
                } catch (Exception ignored) {
                }
            }
        }
        return List.copyOf(result);
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? safe(fallback) : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
