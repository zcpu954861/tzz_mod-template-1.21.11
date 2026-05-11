package com.zcpu.tzzmod.client.webadmin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class WebAdminContainerTemplatePreviewScreen extends Screen {
    private final String sessionId;
    private final String nonce;
    private final String deviceId;
    private final String displayName;
    private final String dimension;
    private final int x;
    private final int y;
    private final int z;
    private final String blockId;
    private final long expiresAtMillis;
    private final List<TemplateCondition> conditions;
    private final int slotCount;
    private boolean closedByServer;
    private boolean cancelSent;
    private boolean cancelConfirmOpen;
    private boolean sessionClosing;
    private String pendingCancelReason = "";
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentTop;
    private int slotGridX;
    private int slotGridY;
    private int slotSize;
    private TemplateCondition hoveredCondition;
    private int scrollOffset;
    private ButtonWidget cancelButton;
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
        this.expiresAtMillis = expiresAtMillis;
        this.conditions = conditions == null ? List.of() : List.copyOf(conditions);
        this.slotCount = Math.max(27, Math.min(54, slotCount <= 0 ? 27 : ((slotCount + 8) / 9) * 9));
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
        panelWidth = Math.min(width - 24, 420);
        panelHeight = Math.min(height - 24, 236);
        panelX = Math.max(12, (width - panelWidth) / 2);
        panelY = Math.max(12, (height - panelHeight) / 2);
        int buttonY = panelY + panelHeight - 24;
        cancelButton = ButtonWidget.builder(Text.literal("取消"), button -> openCancelConfirm("button_cancel")).dimensions(panelX + 12, buttonY, 76, 20).build();
        addDrawableChild(cancelButton);
        ButtonWidget save = ButtonWidget.builder(Text.literal("保存（P3b）"), button -> {
        }).dimensions(panelX + panelWidth - 110, buttonY, 98, 20).build();
        save.active = false;
        addDrawableChild(save);
        int confirmWidth = Math.min(panelWidth - 48, 320);
        int confirmX = panelX + (panelWidth - confirmWidth) / 2;
        int confirmY = panelY + Math.max(46, (panelHeight - 116) / 2);
        continueButton = ButtonWidget.builder(Text.literal("继续编辑"), button -> cancelCancelConfirm()).dimensions(confirmX + 12, confirmY + 84, 96, 20).build();
        confirmCancelButton = ButtonWidget.builder(Text.literal("确认取消"), button -> confirmCancelSession()).dimensions(confirmX + confirmWidth - 108, confirmY + 84, 96, 20).build();
        addDrawableChild(continueButton);
        addDrawableChild(confirmCancelButton);
        syncCancelConfirmButtons();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        hoveredCondition = null;
        context.fill(0, 0, width, height, 0xAA030914);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xEE071827);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF22D3EE);
        context.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0x8845F3FF);
        context.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0x8845F3FF);
        context.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0x8845F3FF);

        int textX = panelX + 12;
        int textY = panelY + 10;
        context.drawTextWithShadow(textRenderer, title, textX, textY, 0xFFE7FAFF);
        textY += 16;
        int wrapWidth = Math.max(120, panelWidth - 24);
        List<OrderedText> instructions = wrappedInstructions(wrapWidth);
        int maxInstructionLines = Math.max(3, Math.min(7, (panelHeight - 110) / Math.max(1, textRenderer.fontHeight + 2)));
        for (int i = 0; i < Math.min(maxInstructionLines, instructions.size()); i++) {
            context.drawText(textRenderer, instructions.get(i), textX, textY, i == 0 ? 0xFF8BE9FF : 0xFFB9CAD6, false);
            textY += textRenderer.fontHeight + 2;
        }
        contentTop = textY + 6;
        renderTemplateContent(context, mouseX, mouseY);
        if (cancelConfirmOpen) {
            renderCancelConfirmOverlay(context);
        }
        super.render(context, mouseX, mouseY, delta);
        if (hoveredCondition != null && !hoveredCondition.stack().isEmpty()) {
            context.drawItemTooltip(textRenderer, hoveredCondition.stack(), mouseX, mouseY);
        }
    }

    private void renderTemplateContent(DrawContext context, int mouseX, int mouseY) {
        int bottom = panelY + panelHeight - 34;
        int availableHeight = Math.max(24, bottom - contentTop);
        List<TemplateCondition> slotConditions = conditions.stream().filter(TemplateCondition::slotCondition).toList();
        List<TemplateCondition> totalConditions = conditions.stream().filter(TemplateCondition::totalCondition).toList();
        List<TemplateCondition> advancedConditions = conditions.stream().filter(c -> !c.slotCondition() && !c.totalCondition()).toList();
        if (conditions.isEmpty()) {
            context.fill(panelX + 12, contentTop, panelX + panelWidth - 12, Math.min(bottom, contentTop + 28), 0x66112335);
            context.drawText(textRenderer, Text.literal("当前没有已保存的容器内容变化物品模板。"), panelX + 20, contentTop + 9, 0xFFB9CAD6, false);
            return;
        }
        int gridColumns = 9;
        slotSize = 18;
        slotGridX = panelX + 12;
        slotGridY = contentTop - scrollOffset;
        int rows = Math.max(1, slotCount / gridColumns);
        int gridHeight = rows * slotSize;
        for (int slot = 0; slot < slotCount; slot++) {
            int sx = slotGridX + (slot % gridColumns) * slotSize;
            int sy = slotGridY + (slot / gridColumns) * slotSize;
            if (sy + slotSize < contentTop || sy > bottom) {
                continue;
            }
            context.fill(sx, sy, sx + 16, sy + 16, 0xAA132334);
            context.fill(sx, sy, sx + 16, sy + 1, 0x6645F3FF);
            TemplateCondition condition = conditionForSlot(slotConditions, slot);
            if (condition != null) {
                renderConditionStack(context, condition, sx, sy, mouseX, mouseY);
            }
        }
        int sideX = Math.min(panelX + panelWidth - 148, slotGridX + gridColumns * slotSize + 16);
        int sideY = contentTop - scrollOffset;
        context.drawText(textRenderer, Text.literal("总量模板"), sideX, sideY, 0xFF8BE9FF, false);
        sideY += 12;
        for (TemplateCondition condition : totalConditions) {
            if (sideY + 18 >= contentTop && sideY <= bottom) {
                renderSmallCondition(context, condition, sideX, sideY, Math.max(90, panelX + panelWidth - sideX - 14), mouseX, mouseY);
            }
            sideY += 22;
        }
        if (!advancedConditions.isEmpty()) {
            sideY += 4;
            context.drawText(textRenderer, Text.literal("高级只读摘要"), sideX, sideY, 0xFFFFC66D, false);
            sideY += 12;
            for (TemplateCondition condition : advancedConditions) {
                if (sideY + 16 >= contentTop && sideY <= bottom) {
                    drawTrimmed(context, condition.nameOrType(), sideX, sideY, Math.max(90, panelX + panelWidth - sideX - 14), 0xFFB9CAD6);
                }
                sideY += 16;
            }
        }
        int contentHeight = Math.max(gridHeight, sideY - (contentTop - scrollOffset));
        if (contentHeight > availableHeight) {
            int barX = panelX + panelWidth - 7;
            int thumbH = Math.max(18, availableHeight * availableHeight / Math.max(availableHeight, contentHeight));
            int maxScroll = Math.max(1, contentHeight - availableHeight);
            int thumbY = contentTop + (availableHeight - thumbH) * scrollOffset / maxScroll;
            context.fill(barX, contentTop, barX + 3, bottom, 0x44182D3D);
            context.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xAA45F3FF);
        }
    }

    private void renderSmallCondition(DrawContext context, TemplateCondition condition, int x, int y, int width, int mouseX, int mouseY) {
        context.fill(x, y, x + width, y + 18, 0xAA132334);
        renderConditionStack(context, condition, x + 1, y + 1, mouseX, mouseY);
        drawTrimmed(context, condition.nameOrType(), x + 22, y + 5, width - 24, 0xFFE7FAFF);
    }

    private void renderConditionStack(DrawContext context, TemplateCondition condition, int x, int y, int mouseX, int mouseY) {
        ItemStack stack = condition.stack();
        if (stack.isEmpty()) {
            context.drawText(textRenderer, Text.literal(condition.type().contains("empty") ? "空" : "?"), x + 4, y + 5, 0xFF93A8B8, false);
        } else {
            context.drawItem(stack, x, y);
            context.drawStackOverlay(textRenderer, stack, x, y);
        }
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            hoveredCondition = condition;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int bottom = panelY + panelHeight - 34;
        if (mouseY < contentTop || mouseY > bottom) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int rows = Math.max(1, slotCount / 9);
        int contentHeight = Math.max(rows * 18, conditions.size() * 22);
        int maxScroll = Math.max(0, contentHeight - Math.max(24, bottom - contentTop));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + (verticalAmount < 0 ? 18 : -18)));
        return true;
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

    private void openCancelConfirm(String reason) {
        if (closedByServer || sessionClosing) {
            return;
        }
        pendingCancelReason = reason == null || reason.isBlank() ? "client_close" : reason;
        cancelConfirmOpen = true;
        syncCancelConfirmButtons();
    }

    private void cancelCancelConfirm() {
        cancelConfirmOpen = false;
        pendingCancelReason = "";
        syncCancelConfirmButtons();
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
        syncCancelConfirmButtons();
    }

    private void syncCancelConfirmButtons() {
        if (cancelButton != null) {
            cancelButton.visible = !cancelConfirmOpen;
            cancelButton.active = !sessionClosing;
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
                ? "正在取消会话，请等待服务端关闭此 GUI。"
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
        String line = "正在编辑 " + location + "。当前是 7.9 P3a skeleton：只展示已保存模板，不写入配置；P3b 才支持左键复制、右键清空、滚轮数量和保存。ESC 或取消按钮会先打开取消确认。";
        if (expiresAtMillis > 0L) {
            line += " 会话超时后自动取消。";
        }
        return textRenderer.wrapLines(Text.literal(line), Math.max(80, width));
    }

    private TemplateCondition conditionForSlot(List<TemplateCondition> slotConditions, int slot) {
        for (TemplateCondition condition : slotConditions) {
            if (condition.slot() == slot) {
                return condition;
            }
        }
        return null;
    }

    private void drawTrimmed(DrawContext context, String value, int x, int y, int maxWidth, int color) {
        String shown = textRenderer.trimToWidth(value == null ? "" : value, Math.max(12, maxWidth));
        context.drawText(textRenderer, Text.literal(shown), x, y, color, false);
    }

    private record TemplateCondition(
            String id,
            String name,
            String type,
            int slot,
            String itemId,
            int count,
            String countMode,
            String displayZone,
            String summary
    ) {
        static TemplateCondition fromJson(JsonObject object) {
            return new TemplateCondition(
                    getString(object, "id"),
                    getString(object, "name"),
                    getString(object, "type"),
                    getInt(object, "slot", 0),
                    fallback(getString(object, "templateItemId"), getString(object, "itemId")),
                    Math.max(1, getInt(object, "templateCount", getInt(object, "count", 1))),
                    fallback(getString(object, "templateCountMode"), getString(object, "countMode")),
                    fallback(getString(object, "displayZone"), zoneForType(getString(object, "type"))),
                    fallback(getString(object, "matcherSummary"), getString(object, "lastResult"))
            );
        }

        boolean slotCondition() {
            return "slot".equals(displayZone);
        }

        boolean totalCondition() {
            return "total".equals(displayZone);
        }

        String nameOrType() {
            String base = name.isBlank() ? type : name;
            String extra = summary.isBlank() ? (itemId.isBlank() ? countMode : itemId + " · " + countMode) : summary;
            return extra.isBlank() ? base : base + " · " + extra;
        }

        ItemStack stack() {
            if (itemId.isBlank()) {
                return ItemStack.EMPTY;
            }
            Identifier id = Identifier.tryParse(itemId);
            if (id == null) {
                return ItemStack.EMPTY;
            }
            Item item = Registries.ITEM.get(id);
            if (item == null) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item, Math.max(1, Math.min(99, count)));
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

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? safe(fallback) : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
