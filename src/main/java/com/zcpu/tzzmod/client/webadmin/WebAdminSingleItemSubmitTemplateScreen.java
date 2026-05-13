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
    private final List<SubmitTemplate> requirements;
    private boolean closedByServer;
    private boolean cancelSent;
    private boolean cancelConfirmOpen;
    private boolean sessionClosing;
    private boolean saveSent;
    private boolean dirty;
    private boolean serverClosePending;
    private String pendingCancelReason = "";
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private boolean compactLayout;
    private int footerButtonY;
    private int templateSlotX;
    private int templateSlotY;
    private int requirementListX;
    private int requirementListY;
    private int requirementListWidth;
    private int requirementListHeight;
    private int selectedRequirementIndex;
    private int requirementScrollOffset;
    private boolean deleteConfirmOpen;
    private int playerGridX;
    private int playerGridY;
    private int hotbarY;
    private int controlStartX;
    private int controlStartY;
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
            SubmitTemplate template,
            List<SubmitTemplate> requirements
    ) {
        super(Text.literal("itemSubmit 条件编辑器"));
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
        this.requirements = new ArrayList<>(requirements == null ? List.of() : requirements);
        this.selectedRequirementIndex = 0;
    }

    public static WebAdminSingleItemSubmitTemplateScreen fromJson(JsonObject body) {
        JsonObject template = body != null && body.has("template") && body.get("template").isJsonObject()
                ? body.getAsJsonObject("template")
                : new JsonObject();
        SubmitTemplate globals = SubmitTemplate.fromJson(template);
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
                globals,
                SubmitTemplate.requirementsFromJson(template, globals)
        );
    }

    public String sessionId() {
        return sessionId;
    }

    public String nonce() {
        return nonce;
    }

    public void closeFromServer() {
        cancelSent = true;
        sessionClosing = true;
        if (client != null && client.currentScreen == this) {
            if (blockIfCursorCarrying("服务端已结束 itemSubmit 条件会话；请先把鼠标上的真实物品放回背包，界面随后会关闭。")) {
                serverClosePending = true;
                return;
            }
            finishServerClose();
        }
    }

    @Override
    protected void init() {
        compactLayout = height < 360 || width < 560 || (height < 430 && width > height * 2);
        int margin = compactLayout ? 4 : 12;
        int availableWidth = Math.max(120, width - margin * 2);
        int availableHeight = Math.max(96, height - margin * 2);
        panelWidth = Math.min(620, availableWidth);
        panelHeight = Math.min(360, availableHeight);
        panelX = Math.max(margin, (width - panelWidth) / 2);
        panelY = Math.max(margin, (height - panelHeight) / 2);
        footerButtonY = panelY + panelHeight - 24;
        int cancelWidth = compactLayout ? 56 : 72;
        int saveWidth = compactLayout ? 76 : 92;
        cancelButton = ButtonWidget.builder(Text.literal("取消"), button -> openCancelConfirm("button_cancel")).dimensions(panelX + 8, footerButtonY, cancelWidth, 20).build();
        addDrawableChild(cancelButton);
        saveButton = ButtonWidget.builder(Text.literal("保存模板"), button -> requestSave()).dimensions(panelX + panelWidth - saveWidth - 8, footerButtonY, saveWidth, 20).build();
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
        int maxInstructionLines = compactLayout ? (panelHeight < 248 ? 0 : 1) : 2;
        int drawnInstructionLines = 0;
        for (OrderedText line : wrappedInstructions(Math.max(120, panelWidth - 16))) {
            if (drawnInstructionLines >= maxInstructionLines || (!compactLayout && textY > panelY + 38)) {
                break;
            }
            context.drawText(textRenderer, line, panelX + 8, textY, textY == panelY + 22 ? 0xFF20505D : 0xFF555555, false);
            textY += textRenderer.fontHeight + 1;
            drawnInstructionLines++;
        }
        layoutAreas(Math.max(textY, panelY + (compactLayout ? 34 : 46)));
        context.drawText(textRenderer, Text.literal("data-single-item-submit-gui-no-overlap"), panelX + panelWidth - 1, panelY + 1, 0x00FFFFFF, false);
        context.drawText(textRenderer, Text.literal("data-single-item-submit-compact-layout footerButtonsDoNotOverlapInventory compactInstructionLines"), panelX + panelWidth - 1, panelY + 2, 0x00FFFFFF, false);
        context.drawText(textRenderer, Text.literal("data-unified-item-submit-editor data-requirement-list-scroll data-delete-requirement-confirm data-multiple-requirements-editable"), panelX + panelWidth - 1, panelY + 3, 0x00FFFFFF, false);
        context.drawText(textRenderer, Text.literal("data-item-submit-adaptive-zero-one-many data-single-requirement-simplified data-single-requirement-no-list-card data-multi-requirement-controls-only-when-many"), panelX + panelWidth - 1, panelY + 4, 0x00FFFFFF, false);
        context.drawText(textRenderer, Text.literal("data-unified-item-submit-compact-layout headerEmptyAddNonOverlap requirementConfigGridNonOverlap footerInventoryNonOverlap compactLongTextHidden layout4kNonOverlap simpleModeConfigHeadingHidden simpleModeTemplateConfigGap"), panelX + panelWidth - 1, panelY + 5, 0x00FFFFFF, false);
        renderRequirementList(context, mouseX, mouseY);
        buildControlButtons();
        renderTemplateSlot(context, mouseX, mouseY);
        renderControlButtons(context, mouseX, mouseY);
        renderPlayerInventory(context, mouseX, mouseY);
        renderCursorStack(context, mouseX, mouseY);
        if (cancelConfirmOpen) {
            renderCancelConfirmOverlay(context);
        }
        if (deleteConfirmOpen) {
            renderDeleteConfirmOverlay(context);
        }
        super.render(context, mouseX, mouseY, delta);
        if (!hoveredStack.isEmpty()) {
            context.drawItemTooltip(textRenderer, hoveredStack, mouseX, mouseY);
        } else if (!hoveredHint.isBlank()) {
            context.drawTooltip(textRenderer, Text.literal(hoveredHint), mouseX, mouseY);
        }
    }

    private void layoutAreas(int instructionBottomY) {
        requirementListX = panelX + (compactLayout ? 8 : 12);
        int mainTopY = Math.max(panelY + (compactLayout ? 46 : 62), instructionBottomY + (compactLayout ? 10 : 14));
        requirementListY = mainTopY + 10;
        requirementListWidth = multiRequirementMode()
                ? (compactLayout ? Math.min(108, Math.max(82, panelWidth / 3)) : Math.min(150, Math.max(120, panelWidth / 4)))
                : Math.min(compactLayout ? 126 : 172, Math.max(110, panelWidth / 3));
        int footerGap = compactLayout ? 5 : 8;
        int inventoryBottom = footerButtonY - footerGap;
        hotbarY = inventoryBottom - SLOT_DRAW_SIZE;
        playerGridY = hotbarY - 4 - 3 * SLOT_SIZE;
        int mainBottomY = playerGridY - (compactLayout ? 8 : 12);
        requirementListHeight = Math.max(compactLayout ? 28 : 46, mainBottomY - requirementListY);
        playerGridX = panelX + Math.max(8, (panelWidth - GRID_COLUMNS * SLOT_SIZE) / 2);
        templateSlotX = multiRequirementMode() ? requirementListX + requirementListWidth + (compactLayout ? 8 : 14) : panelX + (compactLayout ? 12 : 18);
        int preferredTemplateY = compactLayout ? mainTopY + 18 : mainTopY + 20;
        int maxTemplateY = Math.max(mainTopY + 12, mainBottomY - SLOT_DRAW_SIZE - (compactLayout ? 4 : 10));
        templateSlotY = Math.max(mainTopY + 12, Math.min(preferredTemplateY, maxTemplateY));
    }

    @Override
    public void tick() {
        super.tick();
        if (serverClosePending && cursorStack().isEmpty()) {
            finishServerClose();
        }
    }

    private void renderRequirementList(DrawContext context, int mouseX, int mouseY) {
        if (requirements.isEmpty()) {
            context.drawText(textRenderer, Text.literal("itemSubmit 条件"), requirementListX, requirementListY - 10, 0xFF404040, false);
            drawTrimmed(context, "尚未配置提交条件。点击“添加提交条件”开始。", requirementListX, requirementListY + 2, Math.max(120, panelWidth - 24), 0xFF666666);
            return;
        }
        if (simpleRequirementMode()) {
            // Single-requirement mode intentionally does not draw a list card; the template slot
            // and config grid provide the 7.10-style single itemSubmit editing surface.
            return;
        }
        context.drawText(textRenderer, Text.literal(compactLayout ? "条件" : "itemSubmit 条件"), requirementListX, requirementListY - 10, 0xFF404040, false);
        context.fill(requirementListX - 2, requirementListY - 2, requirementListX + requirementListWidth + 2, requirementListY + requirementListHeight + 2, 0xFFB0B8BC);
        context.fill(requirementListX - 1, requirementListY - 1, requirementListX + requirementListWidth + 1, requirementListY + requirementListHeight + 1, 0xFFEFEFEF);
        int rowHeight = compactLayout ? 18 : 24;
        int visibleRows = Math.max(1, requirementListHeight / rowHeight);
        requirementScrollOffset = Math.max(0, Math.min(requirementScrollOffset, Math.max(0, requirements.size() - visibleRows)));
        selectedRequirementIndex = clampIndex(selectedRequirementIndex);
        int y = requirementListY;
        for (int i = 0; i < visibleRows && i + requirementScrollOffset < requirements.size(); i++) {
            int index = i + requirementScrollOffset;
            SubmitTemplate requirement = requirements.get(index);
            boolean selected = index == selectedRequirementIndex;
            boolean hover = inside(mouseX, mouseY, requirementListX, y, requirementListWidth, rowHeight - 1);
            context.fill(requirementListX, y, requirementListX + requirementListWidth, y + rowHeight - 1, selected ? 0xFF9FD3E6 : (hover ? 0xFFD9EBF0 : 0xFFE9E9E9));
            drawTrimmed(context, (index + 1) + ". " + requirement.rowTitle(), requirementListX + 3, y + 3, requirementListWidth - 6, 0xFF303030);
            if (!compactLayout) {
                drawTrimmed(context, requirement.rowSubtitle(), requirementListX + 3, y + 13, requirementListWidth - 6, 0xFF666666);
            }
            y += rowHeight;
        }
        if (requirements.size() > visibleRows) {
            drawTrimmed(context, (requirementScrollOffset + 1) + "-" + Math.min(requirements.size(), requirementScrollOffset + visibleRows) + "/" + requirements.size(), requirementListX + 4, requirementListY + requirementListHeight - 9, requirementListWidth - 8, 0xFF777777);
        }
    }

    private void renderTemplateSlot(DrawContext context, int mouseX, int mouseY) {
        SubmitTemplate requirement = selectedRequirementOrNull();
        if (requirement == null) {
            return;
        }
        context.drawText(textRenderer, Text.literal(multiRequirementMode() ? "当前 requirement 模板" : "提交物品模板"), templateSlotX, templateSlotY - 10, 0xFF404040, false);
        drawSlot(context, templateSlotX, templateSlotY, 0xFF8B8B8B, 0xFFEFEFEF);
        if (requirement.hasItem()) {
            renderStack(context, requirement.stack(), templateSlotX, templateSlotY, mouseX, mouseY, "左键替换模板；右键清空；滚轮调整数量。");
        } else if (inside(mouseX, mouseY, templateSlotX, templateSlotY, SLOT_DRAW_SIZE, SLOT_DRAW_SIZE)) {
            hoveredHint = "从背包拿起物品后左键复制为提交模板。";
        }
        String count = requirement.hasItem() ? (countModeLabel(requirement.countMode) + " " + (ContainerItemCountMode.IGNORE.id().equals(requirement.countMode) ? "不检查" : requirement.count)) : "未配置";
        int summaryX = templateSlotX + 26;
        int summaryWidth = Math.min(170, Math.max(0, controlStartX - summaryX - 6));
        if (summaryWidth >= 54) {
            drawTrimmed(context, "匹配数量：" + count, summaryX, templateSlotY + 2, summaryWidth, 0xFF555555);
        }
        String consume = template.consumeEnabled
                ? ("消耗 " + requirement.consumeCount + (requirement.consumeCountFollowsCount ? " 跟随匹配" : " 手动") + " · " + consumeOrderLabel(template.consumeOrder))
                : "提交后不消耗";
        if (summaryWidth >= 54 && (!compactLayout || summaryWidth >= 90)) {
            drawTrimmed(context, consume, summaryX, templateSlotY + 14, summaryWidth, 0xFF555555);
        }
    }

    private void renderPlayerInventory(DrawContext context, int mouseX, int mouseY) {
        if (!compactLayout || playerGridY - 10 > controlButtonsBottom() + 2) {
            context.drawText(textRenderer, Text.literal("玩家背包"), playerGridX, playerGridY - 10, 0xFF404040, false);
        }
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
        SubmitTemplate requirement = selectedRequirementOrNull();
        int preferredStartX = templateSlotX + (simpleRequirementMode() ? (compactLayout ? 84 : 118) : (compactLayout ? 36 : 128));
        controlStartX = Math.max(panelX + 8, Math.min(panelX + panelWidth - (compactLayout ? 54 : 96), preferredStartX));
        int available = Math.max(48, panelX + panelWidth - 8 - controlStartX);
        int columns = compactLayout
                ? (available >= 300 ? 5 : available >= 220 ? 4 : available >= 142 ? 3 : 2)
                : (available >= 320 ? 4 : available >= 250 ? 3 : 2);
        int gap = compactLayout ? 3 : 6;
        int h = compactLayout ? 13 : 15;
        int rowStep = compactLayout ? 14 : 17;
        int w = Math.max(compactLayout ? 42 : 88, Math.min(compactLayout ? 78 : 112, (available - gap * (columns - 1)) / columns));
        int controlCount = requirement == null ? 1 : (multiRequirementMode() ? 18 : 15);
        int rows = (controlCount + columns - 1) / columns;
        int controlsHeight = (rows - 1) * rowStep + h;
        int minY = requirement == null
                ? requirementListY + (compactLayout ? 24 : 30)
                : Math.max(requirementListY + (compactLayout ? 0 : 4), panelY + (compactLayout ? 50 : 70));
        int naturalY = requirement == null ? minY : Math.max(templateSlotY - (compactLayout ? 2 : 4), minY);
        int maxY = playerGridY - (compactLayout ? 8 : 14) - controlsHeight;
        controlStartY = Math.max(minY, Math.min(naturalY, maxY));
        if (requirement == null) {
            addGridControl(0, columns, controlStartX, controlStartY, w, h, gap, rowStep, "添加提交条件", this::addRequirement, "添加第一个 itemSubmit requirement。");
            return;
        }
        int order = 0;
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "提交 " + labelBool(template.itemSubmitEnabled), () -> {
            template.itemSubmitEnabled = !template.itemSubmitEnabled;
            markTemplateDirty();
        }, "itemSubmitEnabled：启用或禁用 itemSubmit requirements。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "条件 " + labelBool(requirement.requirementEnabled), () -> {
            requirement.requirementEnabled = !requirement.requirementEnabled;
            markTemplateDirty();
        }, "requirement enabled：是否参与 itemSubmit 判断。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "数量 " + countModeLabel(requirement.countMode), () -> {
            requirement.countMode = nextCountMode(requirement.countMode);
            markTemplateDirty();
        }, "countMode at_least / exactly / at_most / ignore。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "原版 " + vanillaPolicyShort(template.vanillaPolicy), () -> {
            template.vanillaPolicy = InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH.equals(InteractionItemVanillaPolicy.normalize(template.vanillaPolicy))
                    ? InteractionItemVanillaPolicy.ALLOW
                    : InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH;
            markTemplateDirty();
        }, "原版交互策略：复用已有 InteractionItemVanillaPolicy，不改运行时语义。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "消耗 " + labelBool(template.consumeEnabled), () -> {
            template.consumeEnabled = !template.consumeEnabled;
            markTemplateDirty();
        }, "itemSubmitConsumeEnabled：提交成功后是否按旧逻辑消耗物品。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, consumeOrderLabel(template.consumeOrder), () -> {
            template.consumeOrder = InventoryConsumeOrder.MAIN_INVENTORY_FIRST.equals(InventoryConsumeOrder.normalize(template.consumeOrder))
                    ? InventoryConsumeOrder.HOTBAR_FIRST
                    : InventoryConsumeOrder.MAIN_INVENTORY_FIRST;
            markTemplateDirty();
        }, "consumeOrder hotbar_first / main_inventory_first。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "跟随 " + labelBool(requirement.consumeCountFollowsCount), () -> toggleConsumeCountFollow(), "data-consume-count-follow-count：开启时 consumeCount 跟随匹配数量；关闭或手动编辑后解耦。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "消耗-", () -> adjustConsumeCount(-1), "consumeCount 减少。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "消耗+", () -> adjustConsumeCount(1), "consumeCount 增加。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "damage " + labelBool(requirement.matchDamage), () -> toggleMatcherOption("damage"), "matchDamage。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "名称 " + labelBool(requirement.matchCustomName), () -> toggleMatcherOption("name"), "matchCustomName。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "Lore " + labelBool(requirement.matchLore), () -> toggleMatcherOption("lore"), "matchLore。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "customData " + labelBool(requirement.matchCustomData), () -> toggleMatcherOption("customData"), "matchCustomData。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "components " + labelBool(requirement.matchComponents), () -> toggleMatcherOption("components"), "matchComponents。");
        addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, simpleRequirementMode() ? "添加另一个条件" : "添加条件", this::addRequirement, "新增一个 itemSubmit requirement，并自动选中。");
        if (multiRequirementMode()) {
            addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "删除条件", this::openDeleteConfirm, "删除当前 requirement，需二次确认。");
            if (selectedRequirementIndex > 0) {
                addGridControl(order++, columns, controlStartX, controlStartY, w, h, gap, rowStep, "上移", () -> moveRequirement(-1), "按列表顺序上移当前 requirement。");
            }
            if (selectedRequirementIndex < requirements.size() - 1) {
                addGridControl(order, columns, controlStartX, controlStartY, w, h, gap, rowStep, "下移", () -> moveRequirement(1), "按列表顺序下移当前 requirement。");
            }
        }
    }

    private void addGridControl(int order, int columns, int startX, int startY, int width, int height, int gap, int rowStep, String label, Runnable action, String hint) {
        int col = order % Math.max(1, columns);
        int row = order / Math.max(1, columns);
        addControl(order, label, startX + col * (width + gap), startY + row * rowStep, width, height, action, hint);
    }

    private void addControl(int order, String label, int x, int y, int width, int height, Runnable action, String hint) {
        controlButtons.add(new ControlButton(order, label, x, y, width, height, action, hint));
    }

    private void renderControlButtons(DrawContext context, int mouseX, int mouseY) {
        if (!requirements.isEmpty() && !simpleRequirementMode() && (!compactLayout || controlStartY - 10 > panelY + 36)) {
            context.drawText(textRenderer, Text.literal(multiRequirementMode() ? (compactLayout ? "单项配置" : "单 requirement 配置") : "单物品配置"), controlStartX, controlStartY - 10, 0xFF404040, false);
        }
        for (ControlButton button : controlButtons) {
            boolean hover = inside(mouseX, mouseY, button.x(), button.y(), button.width(), button.height());
            context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), hover ? 0xFFB8D6E0 : 0xFFBFC7CB);
            context.fill(button.x() + 1, button.y() + 1, button.x() + button.width() - 1, button.y() + button.height() - 1, hover ? 0xFFE8F6FA : 0xFFEFEFEF);
            drawTrimmed(context, button.label(), button.x() + 4, button.y() + 4, button.width() - 8, 0xFF303030);
            if (hover) {
                hoveredHint = button.hint();
            }
        }
        int noteY = controlButtonsBottom() + 6;
        if (!compactLayout && noteY + textRenderer.fontHeight < playerGridY - 12) {
            drawTrimmed(context, "匹配数量和消耗数量独立；模板显示堆叠数按物品最大堆叠限制。", controlStartX, noteY, Math.max(120, panelX + panelWidth - controlStartX - 12), 0xFF666666);
        }
    }

    private int controlButtonsBottom() {
        int bottom = controlStartY;
        for (ControlButton button : controlButtons) {
            bottom = Math.max(bottom, button.y() + button.height());
        }
        return bottom;
    }

    private void renderCursorStack(DrawContext context, int mouseX, int mouseY) {
        ItemStack cursor = cursorStack();
        if (!cursor.isEmpty()) {
            context.drawItem(cursor, mouseX - 8, mouseY - 8);
            context.drawStackOverlay(textRenderer, cursor, mouseX - 8, mouseY - 8);
        }
        int x = panelX + 92;
        String label = !noticeMessage.isBlank()
                ? noticeMessage
                : cursor.isEmpty()
                ? "从下方背包拿起物品，再点提交模板槽复制 ghost。"
                : "鼠标物品：" + Registries.ITEM.getId(cursor.getItem()) + " x" + cursor.getCount();
        drawTrimmed(context, label, x, footerButtonY + 6, Math.max(80, panelWidth - 210), noticeMessage.isBlank() ? (cursor.isEmpty() ? 0xFF6B6B6B : 0xFF20505D) : 0xFF9D4B00);
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
        if (deleteConfirmOpen) {
            int mouseX = (int) click.x();
            int mouseY = (int) click.y();
            int dialogWidth = Math.min(panelWidth - 48, 320);
            int dialogHeight = 116;
            int dialogX = panelX + (panelWidth - dialogWidth) / 2;
            int dialogY = panelY + Math.max(44, (panelHeight - dialogHeight) / 2);
            if (inside(mouseX, mouseY, dialogX + 12, dialogY + 84, 96, 20)) {
                cancelDeleteConfirm();
            } else if (inside(mouseX, mouseY, dialogX + dialogWidth - 108, dialogY + 84, 96, 20)) {
                confirmDeleteRequirement();
            }
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
            int requirementIndex = requirementIndexAt(mouseX, mouseY);
            if (requirementIndex >= 0) {
                selectedRequirementIndex = requirementIndex;
                ensureSelectedVisible();
                syncButtons();
                return true;
            }
            for (ControlButton button : controlButtons) {
                if (inside(mouseX, mouseY, button.x(), button.y(), button.width(), button.height())) {
                    button.action().run();
                    return true;
                }
            }
        }
        if (selectedRequirementOrNull() != null && inside(mouseX, mouseY, templateSlotX, templateSlotY, SLOT_DRAW_SIZE, SLOT_DRAW_SIZE)) {
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
            if (serverClosePending && cursorStack().isEmpty()) {
                finishServerClose();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (multiRequirementMode() && inside((int) mouseX, (int) mouseY, requirementListX, requirementListY, requirementListWidth, requirementListHeight)) {
            requirementScrollOffset = Math.max(0, Math.min(requirementScrollOffset + (verticalAmount < 0 ? 1 : -1), Math.max(0, requirements.size() - 1)));
            return true;
        }
        SubmitTemplate requirement = selectedRequirementOrNull();
        if (requirement != null && inside((int) mouseX, (int) mouseY, templateSlotX, templateSlotY, SLOT_DRAW_SIZE, SLOT_DRAW_SIZE) && requirement.hasItem()) {
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
        currentRequirement().replaceWith(cursor);
        markTemplateDirty();
    }

    private void clearTemplate() {
        if (currentRequirement().hasItem()) {
            currentRequirement().clear();
            markTemplateDirty();
        }
    }

    private void adjustCount(double verticalAmount) {
        SubmitTemplate requirement = currentRequirement();
        if (!requirement.hasItem() || ContainerItemCountMode.IGNORE.id().equals(requirement.countMode)) {
            return;
        }
        int step = ctrlDown() ? 8 : 1;
        int delta = verticalAmount > 0 ? step : -step;
        requirement.count = clampOperationalCount(requirement.count + delta);
        requirement.syncConsumeCountIfFollowing();
        requirement.syncStackCount();
        markTemplateDirty();
    }

    private void adjustConsumeCount(int delta) {
        SubmitTemplate requirement = currentRequirement();
        requirement.consumeCountFollowsCount = false;
        requirement.consumeCount = clampOperationalCount(requirement.consumeCount + delta);
        markTemplateDirty();
    }

    private void toggleConsumeCountFollow() {
        SubmitTemplate requirement = currentRequirement();
        requirement.consumeCountFollowsCount = !requirement.consumeCountFollowsCount;
        if (requirement.consumeCountFollowsCount) {
            requirement.syncConsumeCountToCount();
        }
        markTemplateDirty();
    }

    private void toggleMatcherOption(String option) {
        SubmitTemplate requirement = currentRequirement();
        switch (option) {
            case "damage" -> requirement.matchDamage = !requirement.matchDamage;
            case "name" -> requirement.matchCustomName = !requirement.matchCustomName;
            case "lore" -> requirement.matchLore = !requirement.matchLore;
            case "customData" -> requirement.matchCustomData = !requirement.matchCustomData;
            case "components" -> requirement.matchComponents = !requirement.matchComponents;
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

    private int requirementIndexAt(int mouseX, int mouseY) {
        if (!multiRequirementMode() || !inside(mouseX, mouseY, requirementListX, requirementListY, requirementListWidth, requirementListHeight)) {
            return -1;
        }
        int rowHeight = compactLayout ? 18 : 24;
        int row = (mouseY - requirementListY) / rowHeight;
        int index = requirementScrollOffset + row;
        return index >= 0 && index < requirements.size() ? index : -1;
    }

    private SubmitTemplate selectedRequirementOrNull() {
        if (requirements.isEmpty()) {
            return null;
        }
        selectedRequirementIndex = clampIndex(selectedRequirementIndex);
        return requirements.get(selectedRequirementIndex);
    }

    private SubmitTemplate currentRequirement() {
        if (requirements.isEmpty()) {
            addRequirement();
        }
        selectedRequirementIndex = clampIndex(selectedRequirementIndex);
        return requirements.get(selectedRequirementIndex);
    }

    private boolean simpleRequirementMode() {
        return requirements.size() == 1;
    }

    private boolean multiRequirementMode() {
        return requirements.size() >= 2;
    }

    private int clampIndex(int index) {
        if (requirements.isEmpty()) {
            return 0;
        }
        return Math.max(0, Math.min(index, requirements.size() - 1));
    }

    private void ensureSelectedVisible() {
        int rowHeight = compactLayout ? 18 : 24;
        int visibleRows = Math.max(1, requirementListHeight / Math.max(1, rowHeight));
        if (selectedRequirementIndex < requirementScrollOffset) {
            requirementScrollOffset = selectedRequirementIndex;
        } else if (selectedRequirementIndex >= requirementScrollOffset + visibleRows) {
            requirementScrollOffset = selectedRequirementIndex - visibleRows + 1;
        }
        requirementScrollOffset = Math.max(0, Math.min(requirementScrollOffset, Math.max(0, requirements.size() - visibleRows)));
    }

    private void addRequirement() {
        SubmitTemplate requirement = new SubmitTemplate();
        requirement.requirementId = java.util.UUID.randomUUID().toString();
        requirement.requirementName = "requirement_" + String.format(java.util.Locale.ROOT, "%02d", requirements.size() + 1);
        ItemStack cursor = cursorStack();
        if (!cursor.isEmpty()) {
            requirement.replaceWith(cursor);
        }
        requirements.add(requirement);
        selectedRequirementIndex = requirements.size() - 1;
        ensureSelectedVisible();
        markTemplateDirty();
    }

    private void openDeleteConfirm() {
        if (!multiRequirementMode()) {
            noticeMessage = "单物品模式不显示删除 requirement；需要清空配置请取消或改为禁用 itemSubmit。";
            syncButtons();
            return;
        }
        deleteConfirmOpen = true;
        syncButtons();
    }

    private void confirmDeleteRequirement() {
        if (!multiRequirementMode()) {
            deleteConfirmOpen = false;
            noticeMessage = "唯一 requirement 未删除；删除单项配置需要单独的清空确认入口。";
            syncButtons();
            return;
        }
        requirements.remove(clampIndex(selectedRequirementIndex));
        selectedRequirementIndex = clampIndex(selectedRequirementIndex);
        ensureSelectedVisible();
        deleteConfirmOpen = false;
        markTemplateDirty();
    }

    private void cancelDeleteConfirm() {
        deleteConfirmOpen = false;
        syncButtons();
    }

    private void moveRequirement(int delta) {
        int from = clampIndex(selectedRequirementIndex);
        int to = Math.max(0, Math.min(from + delta, requirements.size() - 1));
        if (from == to) {
            return;
        }
        SubmitTemplate moved = requirements.remove(from);
        requirements.add(to, moved);
        selectedRequirementIndex = to;
        ensureSelectedVisible();
        markTemplateDirty();
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEscape()) {
            if (deleteConfirmOpen) {
                cancelDeleteConfirm();
            } else if (cancelConfirmOpen) {
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
        if (requirements.stream().anyMatch(requirement -> !requirement.hasItem())) {
            noticeMessage = "存在空模板提交条件，请先放入模板物品；多项模式下也可以删除该空条件。";
            syncButtons();
            return;
        }
        saveSent = true;
        sessionClosing = true;
        WebAdminSingleItemSubmitTemplateClient.sendSave(sessionId, nonce, deviceId, expectedFingerprint, toPayload());
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
            cancelButton.visible = !cancelConfirmOpen && !deleteConfirmOpen;
            cancelButton.active = !sessionClosing && !deleteConfirmOpen;
        }
        if (saveButton != null) {
            saveButton.visible = !cancelConfirmOpen && !deleteConfirmOpen;
            saveButton.active = !sessionClosing && dirty && !deleteConfirmOpen;
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

    private Map<String, Object> toPayload() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SubmitTemplate requirement : requirements) {
            if (requirement.hasItem()) {
                rows.add(requirement.toRequirementPayload());
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("itemSubmitEnabled", template.itemSubmitEnabled);
        data.put("itemSubmitConsumeEnabled", template.consumeEnabled);
        data.put("itemSubmitConsumeOrder", InventoryConsumeOrder.normalize(template.consumeOrder));
        data.put("interactionItemVanillaPolicy", InteractionItemVanillaPolicy.normalize(template.vanillaPolicy));
        data.put("requirements", rows);
        data.put("requirementListSaveOrder", true);
        data.put("unifiedItemSubmitEditor", true);
        data.put("noRawJson", true);
        data.put("noConditionEngine", true);
        data.put("noNewConsumeStrategy", true);
        return data;
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

    private void finishServerClose() {
        serverClosePending = false;
        closedByServer = true;
        cancelSent = true;
        sessionClosing = true;
        if (client != null && client.currentScreen == this) {
            client.setScreen(null);
        }
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

    private void renderDeleteConfirmOverlay(DrawContext context) {
        context.fill(0, 0, width, height, 0xAA000713);
        int dialogWidth = Math.min(panelWidth - 48, 320);
        int dialogHeight = 116;
        int dialogX = panelX + (panelWidth - dialogWidth) / 2;
        int dialogY = panelY + Math.max(44, (panelHeight - dialogHeight) / 2);
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xF00A1B2A);
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + 1, 0xFFFF6B6B);
        context.drawTextWithShadow(textRenderer, Text.literal("确认删除当前 requirement？"), dialogX + 12, dialogY + 12, 0xFFFFE7E7);
        String message = "删除只修改本次草稿；保存前不会写入设备配置。";
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(message), Math.max(80, dialogWidth - 24));
        int textY = dialogY + 34;
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            context.drawText(textRenderer, lines.get(i), dialogX + 12, textY, 0xFFB9CAD6, false);
            textY += textRenderer.fontHeight + 2;
        }
        context.fill(dialogX + 12, dialogY + 84, dialogX + 108, dialogY + 104, 0xFFEFEFEF);
        context.drawText(textRenderer, Text.literal("继续编辑"), dialogX + 24, dialogY + 90, 0xFF303030, false);
        context.fill(dialogX + dialogWidth - 108, dialogY + 84, dialogX + dialogWidth - 12, dialogY + 104, 0xFFE45B5B);
        context.drawText(textRenderer, Text.literal("确认删除"), dialogX + dialogWidth - 96, dialogY + 90, 0xFFFFFFFF, false);
    }

    private List<OrderedText> wrappedInstructions(int width) {
        String location = (displayName.isBlank() ? deviceId : displayName) + " · " + dimension + " " + x + " " + y + " " + z + " · " + blockId;
        String line = "统一 itemSubmit requirements：" + location + "。左侧选条件；左键复制模板不消耗，右键清空，滚轮调数量；保存才写入，ESC 取消。";
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
        private String requirementId = "";
        private String requirementName = "";
        private String itemId = "";
        private int count = 1;
        private String countMode = ContainerItemCountMode.AT_LEAST.id();
        private boolean itemSubmitEnabled = true;
        private boolean requirementEnabled = true;
        private boolean matchItemId = true;
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
        private boolean consumeCountFollowsCount = true;
        private String vanillaPolicy = InteractionItemVanillaPolicy.ALLOW;
        private ItemStack displayStack = ItemStack.EMPTY;

        static SubmitTemplate fromJson(JsonObject object) {
            SubmitTemplate template = new SubmitTemplate();
            template.requirementId = getString(object, "requirementId");
            template.requirementName = fallback(getString(object, "requirementName"), getString(object, "name"));
            template.itemId = fallback(getString(object, "templateItemId"), getString(object, "itemId"));
            template.count = clampOperationalCount(getInt(object, "requiredCount", getInt(object, "count", getInt(object, "templateCount", 1))));
            template.countMode = fallback(getString(object, "countMode"), "at_least");
            template.itemSubmitEnabled = getBoolean(object, "itemSubmitEnabled", true);
            template.requirementEnabled = getBoolean(object, "requirementEnabled", true);
            template.matchItemId = getBoolean(object, "matchItemId", true);
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
            boolean hasExplicitConsumeCount = object != null && object.has("consumeCount");
            template.consumeCount = clampOperationalCount(getInt(object, "consumeCount", template.count));
            template.consumeCountFollowsCount = !hasExplicitConsumeCount || template.consumeCount == template.count;
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

        static List<SubmitTemplate> requirementsFromJson(JsonObject object, SubmitTemplate globals) {
            List<SubmitTemplate> result = new ArrayList<>();
            if (object != null && object.has("requirements") && object.get("requirements").isJsonArray()) {
                int index = 0;
                for (com.google.gson.JsonElement element : object.getAsJsonArray("requirements")) {
                    if (element != null && element.isJsonObject()) {
                        SubmitTemplate requirement = SubmitTemplate.fromJson(element.getAsJsonObject());
                        requirement.copyGlobalsFrom(globals);
                        if (requirement.requirementName.isBlank()) {
                            requirement.requirementName = "requirement_" + String.format(java.util.Locale.ROOT, "%02d", index + 1);
                        }
                        result.add(requirement);
                    }
                    index++;
                }
            } else {
                SubmitTemplate requirement = SubmitTemplate.fromJson(object);
                requirement.copyGlobalsFrom(globals);
                if (requirement.hasItem()) {
                    requirement.requirementName = requirement.requirementName.isBlank() ? "requirement_01" : requirement.requirementName;
                    result.add(requirement);
                }
            }
            return result;
        }

        void copyGlobalsFrom(SubmitTemplate globals) {
            if (globals == null) {
                return;
            }
            itemSubmitEnabled = globals.itemSubmitEnabled;
            consumeEnabled = globals.consumeEnabled;
            consumeOrder = globals.consumeOrder;
            vanillaPolicy = globals.vanillaPolicy;
        }

        boolean hasItem() {
            return !safe(itemId).isBlank() && !stack().isEmpty();
        }

        String rowTitle() {
            String label = safe(requirementName).isBlank() ? "requirement" : safe(requirementName);
            return (requirementEnabled ? "✓ " : "○ ") + label;
        }

        String rowSubtitle() {
            if (!hasItem()) {
                return "未配置模板";
            }
            return safe(itemId) + " · " + countModeLabel(countMode) + (ContainerItemCountMode.IGNORE.id().equals(countMode) ? "" : " " + count);
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
            syncConsumeCountIfFollowing();
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
            matchItemId = true;
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
            consumeCountFollowsCount = true;
            displayStack = ItemStack.EMPTY;
        }

        void syncStackCount() {
            if (displayStack != null && !displayStack.isEmpty()) {
                displayStack.setCount(clampStackCount(displayStack, count));
            }
        }

        void syncConsumeCountIfFollowing() {
            if (consumeCountFollowsCount) {
                syncConsumeCountToCount();
            }
        }

        void syncConsumeCountToCount() {
            consumeCount = clampOperationalCount(count);
        }

        Map<String, Object> toRequirementPayload() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("requirementId", safe(requirementId));
            data.put("requirementName", safe(requirementName));
            data.put("itemSubmitEnabled", itemSubmitEnabled);
            data.put("requirementEnabled", requirementEnabled);
            data.put("templateItemId", safe(itemId));
            data.put("itemId", safe(itemId));
            data.put("templateCount", hasItem() ? clampStackCount(stack(), count) : 0);
            data.put("count", hasItem() ? count : 0);
            data.put("requiredCount", hasItem() && !ContainerItemCountMode.IGNORE.id().equals(countMode) ? count : 0);
            data.put("countMode", safe(countMode).isBlank() ? ContainerItemCountMode.AT_LEAST.id() : safe(countMode));
            data.put("matchItemId", matchItemId);
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

        Map<String, Object> toPayload() {
            return toRequirementPayload();
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
        SubmitTemplate requirement = selectedRequirementOrNull();
        data.addProperty("itemSubmitEnabled", template.itemSubmitEnabled);
        data.addProperty("requirementEnabled", requirement != null && requirement.requirementEnabled);
        data.addProperty("countMode", requirement == null ? ContainerItemCountMode.AT_LEAST.id() : ContainerItemCountMode.normalize(requirement.countMode));
        data.addProperty("count", requirement != null && requirement.hasItem() ? requirement.count : 0);
        data.addProperty("consumeEnabled", template.consumeEnabled);
        data.addProperty("consumeOrder", InventoryConsumeOrder.normalize(template.consumeOrder));
        data.addProperty("consumeCount", requirement == null ? 0 : requirement.consumeCount);
        data.addProperty("consumeCountFollowsCount", requirement != null && requirement.consumeCountFollowsCount);
        data.addProperty("vanillaPolicy", InteractionItemVanillaPolicy.normalize(template.vanillaPolicy));
        data.addProperty("requirementCount", requirements.stream().filter(SubmitTemplate::hasItem).count());
        data.addProperty("selectedRequirementIndex", selectedRequirementIndex);
        data.addProperty("selectedRequirementId", requirement == null ? "" : requirement.requirementId);
        data.addProperty("selectedRequirementName", requirement == null ? "" : requirement.requirementName);
        data.addProperty("uiMode", requirements.isEmpty() ? "zero_requirements" : (simpleRequirementMode() ? "single_requirement_simplified" : "multi_requirement_list"));
        data.addProperty("multiRequirementControlsVisible", multiRequirementMode());
        data.addProperty("singleRequirementSimplified", simpleRequirementMode());
        data.addProperty("zeroRequirementAddOnly", requirements.isEmpty());
        JsonObject matcher = new JsonObject();
        matcher.addProperty("matchDamage", requirement != null && requirement.matchDamage);
        matcher.addProperty("matchCustomName", requirement != null && requirement.matchCustomName);
        matcher.addProperty("matchLore", requirement != null && requirement.matchLore);
        matcher.addProperty("matchCustomData", requirement != null && requirement.matchCustomData);
        matcher.addProperty("matchComponents", requirement != null && requirement.matchComponents);
        data.add("matcherOptions", matcher);
        data.add("template", testBridgeTemplateSummary());
        if (includeSlots) {
            JsonArray slots = new JsonArray();
            for (int i = 0; i < requirements.size(); i++) {
                SubmitTemplate row = requirements.get(i);
                JsonObject slot = new JsonObject();
                slot.addProperty("slot", i);
                slot.addProperty("slotId", "submit_template_" + i);
                slot.addProperty("selected", i == selectedRequirementIndex);
                slot.addProperty("editable", true);
                slot.addProperty("empty", !row.hasItem());
                slot.addProperty("requirementId", row.requirementId);
                slot.addProperty("requirementName", row.requirementName);
                slot.addProperty("requirementEnabled", row.requirementEnabled);
                slot.add("item", testBridgeStackSummary(row.stack()));
                slot.addProperty("countMode", ContainerItemCountMode.normalize(row.countMode));
                slot.addProperty("count", row.hasItem() ? row.count : 0);
                slot.addProperty("consumeCount", row.consumeCount);
                slot.addProperty("consumeCountFollowsCount", row.consumeCountFollowsCount);
                slot.addProperty("displayStackCount", row.hasItem() ? row.stack().getCount() : 0);
                slots.add(slot);
            }
            data.add("slots", slots);
        }
        if (includeInventory) {
            data.add("cursor", testBridgeStackSummary(cursorStack()));
        }
        return data;
    }

    public JsonObject testBridgePutItem(int slot, String itemId, int count) {
        selectRequirement(slot);
        ItemStack displaySource = testBridgeStack(itemId, count);
        SubmitTemplate requirement = currentRequirement();
        requirement.replaceWith(displaySource);
        requirement.count = clampOperationalCount(count);
        requirement.syncConsumeCountIfFollowing();
        requirement.syncStackCount();
        requirement.templateDisplayStack = ItemStackDisplaySnapshot.encode(requirement.displayStack, registryLookup());
        markTemplateDirty();
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "put_item");
        data.addProperty("changedSlot", "submit_template_" + selectedRequirementIndex);
        data.addProperty("realInventoryModified", false);
        return data;
    }

    public JsonObject testBridgeClearSlot(int slot) {
        selectRequirement(slot);
        if (requirements.isEmpty()) {
            JsonObject data = testBridgeSnapshot(true, false);
            data.addProperty("action", "clear_slot_no_requirement");
            data.addProperty("realInventoryModified", false);
            return data;
        }
        clearTemplate();
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "clear_slot");
        data.addProperty("changedSlot", "submit_template_" + selectedRequirementIndex);
        data.addProperty("realInventoryModified", false);
        return data;
    }

    public JsonObject testBridgeSetCount(int slot, int count) {
        selectRequirement(slot);
        SubmitTemplate requirement = selectedRequirementOrNull();
        if (requirement == null) {
            throw new IllegalArgumentException("没有可编辑 requirement，请先添加提交条件。");
        }
        if (!requirement.hasItem()) {
            throw new IllegalArgumentException("提交模板为空，无法设置数量。");
        }
        requirement.count = clampOperationalCount(count);
        requirement.syncConsumeCountIfFollowing();
        requirement.syncStackCount();
        requirement.templateDisplayStack = ItemStackDisplaySnapshot.encode(requirement.displayStack, registryLookup());
        markTemplateDirty();
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "set_count");
        data.addProperty("changedSlot", "submit_template_" + selectedRequirementIndex);
        data.addProperty("requirementCount", requirement.count);
        data.addProperty("displayStackCount", requirement.stack().getCount());
        data.addProperty("realInventoryModified", false);
        return data;
    }

    public JsonObject testBridgeSelectRequirement(int slot) {
        selectRequirement(slot);
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "select_requirement");
        return data;
    }

    public JsonObject testBridgeAddRequirement(String itemId, int count) {
        addRequirement();
        if (!safe(itemId).isBlank()) {
            return testBridgePutItem(selectedRequirementIndex, itemId, Math.max(1, count));
        }
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "add_requirement");
        return data;
    }

    public JsonObject testBridgeDeleteRequirement(int slot, boolean confirmed) {
        selectRequirement(slot);
        if (!multiRequirementMode()) {
            JsonObject data = testBridgeSnapshot(true, false);
            data.addProperty("action", "delete_requirement_not_available");
            data.addProperty("code", "SINGLE_REQUIREMENT_DELETE_DENIED");
            data.addProperty("message", "0/1 requirement 状态不显示删除 requirement；不会误删唯一提交条件。");
            data.addProperty("deleteConfirmOpen", false);
            return data;
        }
        if (!confirmed) {
            deleteConfirmOpen = true;
            JsonObject data = testBridgeSnapshot(true, false);
            data.addProperty("action", "delete_requirement_confirm_required");
            data.addProperty("deleteConfirmOpen", true);
            return data;
        }
        confirmDeleteRequirement();
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "delete_requirement");
        return data;
    }

    public JsonObject testBridgeSetCountMode(int slot, String countMode) {
        selectRequirement(slot);
        SubmitTemplate requirement = selectedRequirementOrNull();
        if (requirement == null) {
            throw new IllegalArgumentException("没有可编辑 requirement，请先添加提交条件。");
        }
        requirement.countMode = ContainerItemCountMode.normalize(countMode);
        markTemplateDirty();
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "set_count_mode");
        return data;
    }

    public JsonObject testBridgeSetRequirementEnabled(int slot, boolean enabled) {
        selectRequirement(slot);
        SubmitTemplate requirement = selectedRequirementOrNull();
        if (requirement == null) {
            throw new IllegalArgumentException("没有可编辑 requirement，请先添加提交条件。");
        }
        requirement.requirementEnabled = enabled;
        markTemplateDirty();
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "set_requirement_enabled");
        return data;
    }

    public JsonObject testBridgeSetMatcherOptions(int slot, JsonObject options) {
        selectRequirement(slot);
        SubmitTemplate requirement = selectedRequirementOrNull();
        if (requirement == null) {
            throw new IllegalArgumentException("没有可编辑 requirement，请先添加提交条件。");
        }
        requirement.matchDamage = getBoolean(options, "matchDamage", requirement.matchDamage);
        requirement.matchCustomName = getBoolean(options, "matchCustomName", requirement.matchCustomName);
        requirement.matchLore = getBoolean(options, "matchLore", requirement.matchLore);
        requirement.matchCustomData = getBoolean(options, "matchCustomData", requirement.matchCustomData);
        requirement.matchComponents = getBoolean(options, "matchComponents", requirement.matchComponents);
        markTemplateDirty();
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "set_matcher_options");
        return data;
    }

    public JsonObject testBridgeSetConsume(int slot, int consumeCount, Boolean consumeEnabled, String consumeOrder) {
        selectRequirement(slot);
        SubmitTemplate requirement = selectedRequirementOrNull();
        if (requirement == null) {
            throw new IllegalArgumentException("没有可编辑 requirement，请先添加提交条件。");
        }
        requirement.consumeCountFollowsCount = false;
        requirement.consumeCount = clampOperationalCount(consumeCount);
        if (consumeEnabled != null) {
            template.consumeEnabled = consumeEnabled;
        }
        if (!safe(consumeOrder).isBlank()) {
            template.consumeOrder = InventoryConsumeOrder.normalize(consumeOrder);
        }
        markTemplateDirty();
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "set_consume");
        return data;
    }

    public JsonObject testBridgeSetGlobal(Boolean itemSubmitEnabled, Boolean consumeEnabled, String consumeOrder, String vanillaPolicy) {
        if (itemSubmitEnabled != null) {
            template.itemSubmitEnabled = itemSubmitEnabled;
        }
        if (consumeEnabled != null) {
            template.consumeEnabled = consumeEnabled;
        }
        if (!safe(consumeOrder).isBlank()) {
            template.consumeOrder = InventoryConsumeOrder.normalize(consumeOrder);
        }
        if (!safe(vanillaPolicy).isBlank()) {
            template.vanillaPolicy = InteractionItemVanillaPolicy.normalize(vanillaPolicy);
        }
        markTemplateDirty();
        JsonObject data = testBridgeSnapshot(true, false);
        data.addProperty("action", "set_global");
        return data;
    }

    private void selectRequirement(int slot) {
        if (slot >= 0 && slot < requirements.size()) {
            selectedRequirementIndex = slot;
            ensureSelectedVisible();
        }
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
        data.addProperty("unifiedItemSubmitEditor", true);
        data.addProperty("requirementListScroll", true);
        data.addProperty("deleteRequirementConfirm", true);
        data.addProperty("oldMultiRequirementReadOnlyRefusalRemoved", true);
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
        capabilities.add("select_requirement");
        capabilities.add("add_requirement");
        capabilities.add("delete_requirement");
        capabilities.add("set_count_mode");
        capabilities.add("set_requirement_enabled");
        capabilities.add("set_matcher_options");
        capabilities.add("set_consume");
        capabilities.add("set_global");
        capabilities.add("save");
        capabilities.add("cancel");
        data.add("capabilities", capabilities);
        return data;
    }

    private JsonObject testBridgeTemplateSummary() {
        JsonObject data = new JsonObject();
        SubmitTemplate requirement = selectedRequirementOrNull();
        if (requirement == null) {
            data.addProperty("empty", true);
            data.addProperty("itemId", "");
            data.addProperty("count", 0);
            data.addProperty("displayStackCount", 0);
            data.addProperty("countMode", ContainerItemCountMode.AT_LEAST.id());
            data.addProperty("consumeCount", 0);
            data.addProperty("consumeCountFollowsCount", false);
            data.addProperty("hasDisplaySnapshot", false);
            data.add("item", testBridgeStackSummary(ItemStack.EMPTY));
            return data;
        }
        data.addProperty("empty", !requirement.hasItem());
        data.addProperty("itemId", safe(requirement.itemId));
        data.addProperty("count", requirement.hasItem() ? requirement.count : 0);
        data.addProperty("displayStackCount", requirement.hasItem() ? requirement.stack().getCount() : 0);
        data.addProperty("countMode", ContainerItemCountMode.normalize(requirement.countMode));
        data.addProperty("consumeCount", requirement.consumeCount);
        data.addProperty("consumeCountFollowsCount", requirement.consumeCountFollowsCount);
        data.addProperty("hasDisplaySnapshot", !safe(requirement.templateDisplayStack).isBlank());
        data.add("item", testBridgeStackSummary(requirement.stack()));
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
