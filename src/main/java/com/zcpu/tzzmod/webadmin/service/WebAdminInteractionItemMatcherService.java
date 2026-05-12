package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.item.InteractionItemSource;
import com.zcpu.tzzmod.signal.device.item.InteractionItemVanillaPolicy;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherSupport;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminInteractionItemMatcherUpdateRequest;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditEvent;
import com.zcpu.tzzmod.webadmin.write.WebAdminAuditWriter;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
import com.zcpu.tzzmod.webadmin.write.WebAdminOperationType;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionDecision;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminValidationError;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteAuditContext;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteContext;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResultCode;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteTarget;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class WebAdminInteractionItemMatcherService {
    public static final int MAX_ITEM_ID_LENGTH = 128;
    public static final int MAX_CUSTOM_NAME_LENGTH = 128;
    public static final int MAX_LORE_LINES = 16;
    public static final int MAX_LORE_LINE_LENGTH = 160;
    public static final int MAX_REQUIRED_COUNT = 64;
    private static final List<String> ALLOWED_COUNT_MODES = List.of(
            ContainerItemCountMode.IGNORE.id(),
            ContainerItemCountMode.AT_LEAST.id(),
            ContainerItemCountMode.EXACTLY.id(),
            ContainerItemCountMode.AT_MOST.id()
    );
    private static final List<String> ALLOWED_SOURCES = List.of(
            InteractionItemSource.MAIN_HAND,
            InteractionItemSource.OFF_HAND
    );
    private static final List<String> ALLOWED_VANILLA_POLICIES = List.of(
            InteractionItemVanillaPolicy.ALLOW,
            InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH
    );

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;

    public WebAdminInteractionItemMatcherService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
    }

    public Map<String, Object> configFor(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String deviceId
    ) {
        SignalDeviceData device = findDevice(server, deviceId);
        if (device == null) {
            return null;
        }
        return configFor(device, user, session);
    }

    public Map<String, Object> configFor(
            SignalDeviceData rawDevice,
            WebAdminUser user,
            WebAdminSession session
    ) {
        SignalDeviceData device = rawDevice == null ? null : rawDevice.normalized();
        if (device == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        boolean isVirtualBlockDevice = SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type());
        data.put("deviceId", device.id());
        data.put("deviceType", WebAdminReadonlySupport.deviceType(device));
        data.put("displayName", WebAdminReadonlySupport.deviceDisplayName(device));
        data.put("supported", isVirtualBlockDevice);
        data.put("typeSupported", isVirtualBlockDevice);
        data.put("matcherReadable", isVirtualBlockDevice);
        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        boolean sourceEditable = ALLOWED_SOURCES.contains(matcher.interactionItemSource());
        boolean policyEditable = ALLOWED_VANILLA_POLICIES.contains(matcher.interactionItemVanillaPolicy());
        data.put("matcherEditable", isVirtualBlockDevice && !device.itemSubmitEnabled() && sourceEditable && policyEditable);
        data.put("unsupportedReason", isVirtualBlockDevice
                ? (device.itemSubmitEnabled()
                ? "当前设备已启用 itemSubmit；7.8 只编辑普通 interaction item matcher，不能同时编辑 itemSubmit 匹配。"
                : (!sourceEditable || !policyEditable ? "当前 matcher 使用了 7.8 暂不编辑的物品来源或原版交互策略，只能只读查看。" : ""))
                : "只有 virtual_block_device 支持交互物品匹配配置。");
        data.put("itemSubmitEnabled", device.itemSubmitEnabled());
        data.put("interactionEnabled", device.interactionEnabled());
        data.put("interactChannel", device.interactChannel());
        data.put("interactionCooldownTicks", device.interactionCooldownTicks());
        data.put("matcher", matcherDto(device));
        data.put("allowedCountModes", ALLOWED_COUNT_MODES);
        data.put("allowedSources", ALLOWED_SOURCES);
        data.put("allowedVanillaPolicies", ALLOWED_VANILLA_POLICIES);
        data.put("readOnlyFields", List.of(
                "matchItemId",
                "templateCount",
                "templateSummary",
                "templateCustomData",
                "templateComponents",
                "templateDisplayStack",
                "successChannel",
                "failChannel",
                "successMessage",
                "failMessage",
                "successSound",
                "failSound",
                "consume",
                "inventoryConsumeOrder",
                "lastInteractionItemResult",
                "createdWallTimeMillis",
                "updatedWallTimeMillis"
        ));
        data.put("forbiddenFields", List.of(
                "itemSubmit",
                "consume",
                "inventory",
                "equipment",
                "armor",
                "rawNbt",
                "rawDataComponents",
                "conditionEngine",
                "successFailPathGraph"
        ));
        data.put("expectedFingerprint", fingerprintFor(device));
        data.put("lockStatus", editLockService == null ? null : editLockService.status(
                WebAdminEditLockService.TARGET_INTERACTION_ITEM_MATCHER,
                device.id(),
                user,
                session
        ));
        data.put("noRawJson", true);
        data.put("notes", List.of(
                "本阶段只编辑 virtual_block_device 的普通 interaction item matcher。",
                "successChannel / failChannel / interactChannel / cooldown 仍在设备类型专属配置中编辑，此处只显示摘要。",
                "不会创建 itemSubmit、consume、inventory/equipment matcher 或 ConditionEngine 配置。"
        ));
        return data;
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String deviceId,
            WebAdminInteractionItemMatcherUpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String safeDeviceId = safe(deviceId);
        SignalDeviceData device = findDevice(server, safeDeviceId);
        WebAdminWriteTarget target = target(device == null ? safeDeviceId : device.id(), device == null ? safeDeviceId : WebAdminReadonlySupport.deviceDisplayName(device));
        WebAdminWriteContext writeContext = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.EDIT_ITEM_MATCHER,
                target
        );
        if (device == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.TARGET_NOT_FOUND,
                    target,
                    "目标 virtual_block_device 不存在或引用不唯一。"
            );
            audit(writeContext, result, Map.of(), Map.of());
            return result;
        }
        if (request != null) {
            request.deviceId = device.id();
        }
        target = target(device.id(), WebAdminReadonlySupport.deviceDisplayName(device));
        writeContext = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_ITEM_MATCHER, target);

        if (!SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "invalid_type",
                    "只有 virtual_block_device 支持交互物品匹配配置。",
                    device.type()
            )));
            audit(writeContext, result, currentSummary(device), Map.of("attempt", "invalid_type"));
            return result;
        }
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_ITEM_MATCHER);
        if (!permission.allowed()) {
            WebAdminWriteResult result = permission.asWriteResult(target);
            audit(writeContext, result, currentSummary(device), Map.of("attempt", "permission_denied"));
            return result;
        }
        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
            audit(writeContext, result, currentSummary(device), Map.of("attempt", "csrf_failed"));
            return result;
        }
        if (!sameOrigin) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.CSRF_INVALID,
                    target,
                    "写请求来源校验失败，请刷新页面后重试。"
            );
            audit(writeContext, result, currentSummary(device), Map.of("attempt", "origin_failed"));
            return result;
        }
        if (device.itemSubmitEnabled()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "itemSubmit",
                    "mode_conflict",
                    "当前设备已启用 itemSubmit；7.8 不编辑 itemSubmit 或多物品提交匹配，请先在后续 itemSubmit 阶段处理。",
                    "itemSubmitEnabled"
            )));
            audit(writeContext, result, currentSummary(device), requestSummary(request));
            return result;
        }
        ItemStackMatcherData currentMatcher = device.interactionItemMatcher().normalized();
        if (!ALLOWED_SOURCES.contains(currentMatcher.interactionItemSource()) || !ALLOWED_VANILLA_POLICIES.contains(currentMatcher.interactionItemVanillaPolicy())) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "matcher",
                    "unsupported_existing_matcher",
                    "当前 matcher 使用了 7.8 暂不编辑的物品来源或原版交互策略，只能只读查看，不能通过普通 matcher 表单覆盖。",
                    currentMatcher.interactionItemSource() + "/" + currentMatcher.interactionItemVanillaPolicy()
            )));
            audit(writeContext, result, currentSummary(device), requestSummary(request));
            return result;
        }
        if (editLockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_INTERACTION_ITEM_MATCHER,
                    device.id(),
                    request == null ? "" : request.lockId,
                    user,
                    session
            );
            if (!lockValidation.success()) {
                WebAdminWriteResult result = lockValidation.result();
                audit(writeContext, result, currentSummary(device), Map.of("attempt", "edit_lock_failed"));
                return result;
            }
        }
        if (request == null || isBlank(request.expectedFingerprint)) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "expectedFingerprint",
                    "required",
                    "保存需要 expectedFingerprint，用于防止覆盖其他操作的修改。",
                    ""
            )));
            audit(writeContext, result, currentSummary(device), Map.of("attempt", "expected_fingerprint_missing"));
            return result;
        }
        if (!fingerprintMatches(device, request.expectedFingerprint)) {
            WebAdminWriteResult result = conflictDetected(target, device, request.expectedFingerprint);
            audit(writeContext, result, currentSummary(device), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }

        List<WebAdminValidationError> errors = validateRequest(request);
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(writeContext, result, currentSummary(device), requestSummary(request));
            return result;
        }

        ItemStackMatcherData nextMatcher = matcherFor(device, request);
        boolean nextEnabled = Boolean.TRUE.equals(request.enabled) && nextMatcher.enabled();
        SignalDeviceData targetDevice = SignalDeviceStore.withInteractionItemMatcherForWebAdmin(device, nextMatcher, nextEnabled);
        List<String> changedFields = changedFields(device, targetDevice);
        if (changedFields.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的交互物品匹配变化。");
            audit(writeContext, result, currentSummary(device), currentSummary(device));
            releaseLockAfterWrite(request, user, session, remoteAddress);
            return result;
        }

        ServerWorld world = SignalDeviceStore.getDeviceWorld(server, device);
        if (world == null) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "world",
                    "world_unavailable",
                    "设备所在维度不可用，无法保存 interaction item matcher。",
                    device.dimension()
            )));
            audit(writeContext, result, currentSummary(device), requestSummary(request));
            return result;
        }
        SignalDeviceData updated = SignalDeviceStore.updateVirtualInteractionItemMatcher(
                world,
                new BlockPos(device.x(), device.y(), device.z()),
                nextMatcher,
                nextEnabled,
                nextEnabled ? "WebAdmin 已更新交互物品匹配" : "WebAdmin 已关闭交互物品匹配"
        );
        if (updated == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.TARGET_NOT_FOUND,
                    target,
                    "保存时目标 virtual_block_device 已不存在。"
            );
            audit(writeContext, result, currentSummary(device), requestSummary(request));
            return result;
        }
        SignalDeviceStore.forceFlushDirty(server);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("interactionItemMatcher", configFor(updated, user, session));
        data.put("changedFields", changedFields);
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "交互物品匹配配置已保存。",
                target.targetType(),
                target.targetId(),
                true,
                List.of(),
                "",
                "",
                false,
                Map.of(),
                data
        );
        WebAdminAuditEvent auditEvent = audit(writeContext, result, currentSummary(device), currentSummary(updated));
        publishRealtime(updated, auditEvent, changedFields, user);
        releaseLockAfterWrite(request, user, session, remoteAddress);
        return result;
    }

    public static boolean fingerprintMatches(SignalDeviceData device, String expectedFingerprint) {
        return !isBlank(expectedFingerprint) && fingerprintFor(device).equals(expectedFingerprint);
    }

    public static String fingerprintFor(SignalDeviceData rawDevice) {
        SignalDeviceData device = rawDevice == null ? null : rawDevice.normalized();
        if (device == null) {
            return "";
        }
        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("interactionItemMatcherEnabled", device.interactionItemMatcherEnabled());
        values.put("itemSubmitEnabled", device.itemSubmitEnabled());
        values.put("templateItemId", matcher.templateItemId());
        values.put("countMode", matcher.countMode());
        values.put("requiredCount", matcher.requiredCount());
        values.put("matchDamage", matcher.matchDamage());
        values.put("templateDamage", matcher.templateDamage());
        values.put("matchCustomName", matcher.matchCustomName());
        values.put("templateCustomName", matcher.templateCustomName());
        values.put("matchLore", matcher.matchLore());
        values.put("templateLore", matcher.templateLore());
        values.put("interactionItemSource", matcher.interactionItemSource());
        values.put("interactionItemVanillaPolicy", matcher.interactionItemVanillaPolicy());
        String input = "interaction_item_matcher|" + device.id() + "|" + device.type() + "|" + values;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public static List<WebAdminValidationError> validateRequest(WebAdminInteractionItemMatcherUpdateRequest request) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new WebAdminValidationError("matcher", "required", "交互物品匹配配置不能为空。", ""));
            return List.copyOf(errors);
        }
        boolean enabled = Boolean.TRUE.equals(request.enabled);
        String itemId = normalizeItemId(request.templateItemId);
        if (enabled && itemId.isBlank()) {
            errors.add(new WebAdminValidationError("templateItemId", "required", "启用匹配时必须填写物品 ID。", ""));
        }
        if (!itemId.isBlank()) {
            if (itemId.length() > MAX_ITEM_ID_LENGTH) {
                errors.add(new WebAdminValidationError("templateItemId", "too_long", "物品 ID 不能超过 128 个字符。", itemId));
            } else if (!isValidItemId(itemId)) {
                errors.add(new WebAdminValidationError("templateItemId", "invalid_item_id", "物品 ID 必须使用 namespace:path 格式，例如 minecraft:diamond。", itemId));
            }
        }
        String countMode = normalizeCountMode(request.countMode);
        if (!ALLOWED_COUNT_MODES.contains(countMode)) {
            errors.add(new WebAdminValidationError("countMode", "invalid_count_mode", "数量规则必须是 ignore、at_least、exactly 或 at_most。", safe(request.countMode)));
        }
        Integer requiredCount = parseInteger(request.requiredCount);
        if (!ContainerItemCountMode.IGNORE.id().equals(countMode)) {
            if (requiredCount == null) {
                errors.add(new WebAdminValidationError("requiredCount", "invalid_integer", "数量必须是整数。", String.valueOf(request.requiredCount)));
            } else if (requiredCount < 1 || requiredCount > MAX_REQUIRED_COUNT) {
                errors.add(new WebAdminValidationError("requiredCount", "out_of_range", "数量必须在 1～64 之间。", String.valueOf(requiredCount)));
            }
        }
        if (Boolean.TRUE.equals(request.matchDamage)) {
            Integer damage = parseInteger(request.templateDamage);
            if (damage == null) {
                errors.add(new WebAdminValidationError("templateDamage", "invalid_integer", "耐久 / damage 必须是非负整数。", String.valueOf(request.templateDamage)));
            } else if (damage < 0) {
                errors.add(new WebAdminValidationError("templateDamage", "out_of_range", "耐久 / damage 不能小于 0。", String.valueOf(damage)));
            }
        }
        String customName = safe(request.templateCustomName).trim();
        if (customName.length() > MAX_CUSTOM_NAME_LENGTH) {
            errors.add(new WebAdminValidationError("templateCustomName", "too_long", "自定义名称不能超过 128 个字符。", customName));
        }
        if (Boolean.TRUE.equals(request.matchCustomName) && customName.isBlank()) {
            errors.add(new WebAdminValidationError("templateCustomName", "required", "启用自定义名称匹配时必须填写名称。", ""));
        }
        List<String> lore = normalizeLore(request.templateLore);
        if (lore.size() > MAX_LORE_LINES) {
            errors.add(new WebAdminValidationError("templateLore", "too_many", "Lore 最多支持 " + MAX_LORE_LINES + " 行。", String.valueOf(lore.size())));
        }
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.length() > MAX_LORE_LINE_LENGTH) {
                errors.add(new WebAdminValidationError("templateLore[" + i + "]", "too_long", "单行 Lore 不能超过 " + MAX_LORE_LINE_LENGTH + " 个字符。", line));
            }
        }
        if (Boolean.TRUE.equals(request.matchLore) && lore.isEmpty()) {
            errors.add(new WebAdminValidationError("templateLore", "required", "启用 Lore 匹配时至少需要一行 Lore。", ""));
        }
        String sourceRaw = safe(request.interactionItemSource).trim().toLowerCase(Locale.ROOT);
        String source = InteractionItemSource.normalize(sourceRaw);
        if (sourceRaw.isBlank()) {
            errors.add(new WebAdminValidationError("interactionItemSource", "required", "必须指定物品来源。", ""));
        } else if (!ALLOWED_SOURCES.contains(source) || !ALLOWED_SOURCES.contains(sourceRaw)) {
            errors.add(new WebAdminValidationError("interactionItemSource", "unsupported_source", "7.8 只支持主手或副手物品匹配；背包、盔甲和 equipment matcher 后续阶段处理。", source));
        }
        String policyRaw = safe(request.interactionItemVanillaPolicy).trim().toLowerCase(Locale.ROOT);
        String policy = InteractionItemVanillaPolicy.normalize(policyRaw);
        if (policyRaw.isBlank()) {
            errors.add(new WebAdminValidationError("interactionItemVanillaPolicy", "required", "必须指定原版交互策略。", ""));
        } else if (!ALLOWED_VANILLA_POLICIES.contains(policy) || !ALLOWED_VANILLA_POLICIES.contains(policyRaw)) {
            errors.add(new WebAdminValidationError("interactionItemVanillaPolicy", "invalid_policy", "原版交互策略必须是 allow 或 require_item_match。", safe(request.interactionItemVanillaPolicy)));
        }
        return List.copyOf(errors);
    }

    private static ItemStackMatcherData matcherFor(SignalDeviceData device, WebAdminInteractionItemMatcherUpdateRequest request) {
        ItemStackMatcherData previous = device.interactionItemMatcher().normalized();
        String itemId = normalizeItemId(request.templateItemId);
        String countMode = normalizeCountMode(request.countMode);
        int requiredCount = ContainerItemCountMode.IGNORE.id().equals(countMode) ? 0 : Math.max(1, parseIntegerOr(request.requiredCount, previous.requiredCount() <= 0 ? 1 : previous.requiredCount()));
        boolean matchDamage = Boolean.TRUE.equals(request.matchDamage);
        int templateDamage = matchDamage ? Math.max(0, parseIntegerOr(request.templateDamage, previous.templateDamage())) : Math.max(0, previous.templateDamage());
        boolean matchCustomName = Boolean.TRUE.equals(request.matchCustomName);
        String customName = safe(request.templateCustomName).trim();
        boolean matchLore = Boolean.TRUE.equals(request.matchLore);
        List<String> lore = normalizeLore(request.templateLore);
        String source = InteractionItemSource.normalize(request.interactionItemSource);
        String policy = InteractionItemVanillaPolicy.normalize(request.interactionItemVanillaPolicy);
        long now = System.currentTimeMillis();
        long created = previous.createdWallTimeMillis() <= 0L ? now : previous.createdWallTimeMillis();
        String templateCustomData = previous.templateCustomData();
        String templateComponents = previous.templateComponents();
        String templateDisplayStack = previous.templateDisplayStack();
        boolean matchCustomData = previous.matchCustomData();
        boolean matchComponents = previous.matchComponents();
        ItemStackMatcherData data = new ItemStackMatcherData(
                Boolean.TRUE.equals(request.enabled),
                itemId,
                ContainerItemCountMode.IGNORE.id().equals(countMode) ? Math.max(1, previous.templateCount()) : requiredCount,
                countMode,
                requiredCount,
                true,
                matchDamage,
                matchCustomName,
                matchLore,
                matchCustomData,
                matchComponents,
                templateDamage,
                customName,
                lore,
                templateCustomData,
                templateComponents,
                templateDisplayStack,
                "",
                previous.successChannel(),
                previous.failChannel(),
                previous.successMessage(),
                previous.failMessage(),
                previous.successSoundId(),
                previous.successSoundVolume(),
                previous.successSoundPitch(),
                previous.failSoundId(),
                previous.failSoundVolume(),
                previous.failSoundPitch(),
                previous.consumeEnabled(),
                previous.consumeCount(),
                previous.interactionItemConsumeSource(),
                previous.interactionItemInventoryConsumeOrder(),
                source,
                policy,
                "",
                -1,
                0,
                "",
                previous.lastInteractionItemConsumeSource(),
                previous.lastInteractionItemConsumedSlots(),
                previous.lastInteractionItemConsumeResult(),
                created,
                now
        ).normalized();
        return new ItemStackMatcherData(
                data.enabled(),
                data.templateItemId(),
                data.templateCount(),
                data.countMode(),
                data.requiredCount(),
                data.matchItemId(),
                data.matchDamage(),
                data.matchCustomName(),
                data.matchLore(),
                data.matchCustomData(),
                data.matchComponents(),
                data.templateDamage(),
                data.templateCustomName(),
                data.templateLore(),
                data.templateCustomData(),
                data.templateComponents(),
                data.templateDisplayStack(),
                ItemStackMatcherSupport.summary(data),
                data.successChannel(),
                data.failChannel(),
                data.successMessage(),
                data.failMessage(),
                data.successSoundId(),
                data.successSoundVolume(),
                data.successSoundPitch(),
                data.failSoundId(),
                data.failSoundVolume(),
                data.failSoundPitch(),
                data.consumeEnabled(),
                data.consumeCount(),
                data.interactionItemConsumeSource(),
                data.interactionItemInventoryConsumeOrder(),
                data.interactionItemSource(),
                data.interactionItemVanillaPolicy(),
                data.lastInteractionItemSource(),
                data.lastInteractionItemMatchedSlot(),
                data.lastInteractionItemMatchedCount(),
                data.lastInteractionItemSourceResult(),
                data.lastInteractionItemConsumeSource(),
                data.lastInteractionItemConsumedSlots(),
                data.lastInteractionItemConsumeResult(),
                data.createdWallTimeMillis(),
                data.updatedWallTimeMillis()
        ).normalized();
    }

    private static SignalDeviceData findDevice(MinecraftServer server, String deviceId) {
        if (server == null || isBlank(deviceId)) {
            return null;
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, deviceId);
        return resolved.foundUnique() ? resolved.device().normalized() : null;
    }

    private static Map<String, Object> matcherDto(SignalDeviceData device) {
        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", device.interactionItemMatcherEnabled());
        data.put("configured", matcher.enabled());
        data.put("templateItemId", matcher.templateItemId());
        data.put("templateCount", matcher.templateCount());
        data.put("countMode", matcher.countMode());
        data.put("requiredCount", matcher.requiredCount());
        data.put("matchItemId", matcher.matchItemId());
        data.put("matchDamage", matcher.matchDamage());
        data.put("matchCustomName", matcher.matchCustomName());
        data.put("matchLore", matcher.matchLore());
        data.put("matchCustomData", matcher.matchCustomData());
        data.put("matchComponents", matcher.matchComponents());
        data.put("templateDamage", matcher.templateDamage());
        data.put("templateCustomName", matcher.templateCustomName());
        data.put("templateLore", matcher.templateLore());
        data.put("templateCustomDataPresent", !matcher.templateCustomData().isBlank());
        data.put("templateComponentsPresent", !matcher.templateComponents().isBlank());
        data.put("templateSummary", matcher.templateSummary().isBlank() ? ItemStackMatcherSupport.summary(matcher) : matcher.templateSummary());
        data.put("successChannel", matcher.successChannel());
        data.put("failChannel", matcher.failChannel());
        data.put("interactionItemSource", matcher.interactionItemSource());
        data.put("interactionItemSourceDisplayName", InteractionItemSource.displayName(matcher.interactionItemSource()));
        data.put("interactionItemVanillaPolicy", matcher.interactionItemVanillaPolicy());
        data.put("interactionItemVanillaPolicyDisplayName", InteractionItemVanillaPolicy.displayName(matcher.interactionItemVanillaPolicy()));
        data.put("consumeEnabled", matcher.consumeEnabled());
        data.put("consumeCount", matcher.consumeCount());
        data.put("lastInteractionItemMatched", device.lastInteractionItemMatched());
        data.put("lastInteractionItemResult", device.lastInteractionItemResult());
        data.put("lastInteractionItemSourceResult", matcher.lastInteractionItemSourceResult());
        data.put("createdWallTimeMillis", matcher.createdWallTimeMillis());
        data.put("updatedWallTimeMillis", matcher.updatedWallTimeMillis());
        return data;
    }

    private static Map<String, Object> currentSummary(SignalDeviceData device) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (device == null) {
            return summary;
        }
        summary.put("deviceId", device.id());
        summary.put("deviceType", WebAdminReadonlySupport.deviceType(device));
        summary.put("displayName", WebAdminReadonlySupport.deviceDisplayName(device));
        summary.put("matcher", matcherDto(device));
        summary.put("expectedFingerprint", fingerprintFor(device));
        return summary;
    }

    private static Map<String, Object> requestSummary(WebAdminInteractionItemMatcherUpdateRequest request) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (request == null) {
            return summary;
        }
        summary.put("enabled", Boolean.TRUE.equals(request.enabled));
        summary.put("templateItemId", normalizeItemId(request.templateItemId));
        summary.put("countMode", normalizeCountMode(request.countMode));
        summary.put("requiredCount", request.requiredCount);
        summary.put("matchDamage", Boolean.TRUE.equals(request.matchDamage));
        summary.put("matchCustomName", Boolean.TRUE.equals(request.matchCustomName));
        summary.put("matchLore", Boolean.TRUE.equals(request.matchLore));
        summary.put("interactionItemSource", InteractionItemSource.normalize(request.interactionItemSource));
        summary.put("interactionItemVanillaPolicy", InteractionItemVanillaPolicy.normalize(request.interactionItemVanillaPolicy));
        summary.put("expectedFingerprint", request.expectedFingerprint);
        return summary;
    }

    private static List<String> changedFields(SignalDeviceData before, SignalDeviceData after) {
        Map<String, Object> beforeValues = currentSummary(before);
        Map<String, Object> afterValues = currentSummary(after);
        List<String> changed = new ArrayList<>();
        if (!safe(before == null ? "" : String.valueOf(before.interactionItemMatcherEnabled())).equals(safe(after == null ? "" : String.valueOf(after.interactionItemMatcherEnabled())))) {
            changed.add("enabled");
        }
        ItemStackMatcherData oldMatcher = before == null ? ItemStackMatcherData.empty() : before.interactionItemMatcher().normalized();
        ItemStackMatcherData newMatcher = after == null ? ItemStackMatcherData.empty() : after.interactionItemMatcher().normalized();
        compare(changed, "templateItemId", oldMatcher.templateItemId(), newMatcher.templateItemId());
        compare(changed, "countMode", oldMatcher.countMode(), newMatcher.countMode());
        compare(changed, "requiredCount", oldMatcher.requiredCount(), newMatcher.requiredCount());
        compare(changed, "matchDamage", oldMatcher.matchDamage(), newMatcher.matchDamage());
        compare(changed, "templateDamage", oldMatcher.templateDamage(), newMatcher.templateDamage());
        compare(changed, "matchCustomName", oldMatcher.matchCustomName(), newMatcher.matchCustomName());
        compare(changed, "templateCustomName", oldMatcher.templateCustomName(), newMatcher.templateCustomName());
        compare(changed, "matchLore", oldMatcher.matchLore(), newMatcher.matchLore());
        compare(changed, "templateLore", oldMatcher.templateLore(), newMatcher.templateLore());
        compare(changed, "interactionItemSource", oldMatcher.interactionItemSource(), newMatcher.interactionItemSource());
        compare(changed, "interactionItemVanillaPolicy", oldMatcher.interactionItemVanillaPolicy(), newMatcher.interactionItemVanillaPolicy());
        beforeValues.clear();
        afterValues.clear();
        return List.copyOf(changed);
    }

    private static void compare(List<String> changed, String field, Object before, Object after) {
        if (before == null ? after != null : !before.equals(after)) {
            changed.add(field);
        }
    }

    private WebAdminAuditEvent audit(
            WebAdminWriteContext context,
            WebAdminWriteResult result,
            Map<String, ?> beforeSummary,
            Map<String, ?> afterSummary
    ) {
        WebAdminAuditEvent auditEvent = WebAdminAuditWriter.eventForResult(
                WebAdminWriteAuditContext.from(context),
                result,
                beforeSummary,
                afterSummary
        );
        WebAdminAuditLogger.writeEvent(auditEvent);
        return auditEvent;
    }

    private void publishRealtime(
            SignalDeviceData device,
            WebAdminAuditEvent auditEvent,
            List<String> changedFields,
            WebAdminUser user
    ) {
        String deviceId = device.id();
        String routeTarget = "#/devices/" + encode(deviceId);
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .severity("INFO")
                .summary("交互物品匹配配置已更新。")
                .routeTarget(routeTarget)
                .payload("targetType", "interaction_item_matcher")
                .payload("deviceType", device.type())
                .payload("changedFields", changedFields)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEvent deviceEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.DEVICE_CONFIG_CHANGED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .severity("INFO")
                .summary("交互物品匹配配置已更新：" + WebAdminReadonlySupport.deviceDisplayName(device))
                .routeTarget(routeTarget)
                .payload("targetType", "interaction_item_matcher")
                .payload("deviceType", device.type())
                .payload("changedFields", changedFields)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget)
                .payload("targetType", "interaction_item_matcher")
                .payload("deviceType", device.type())
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("deviceEventId", deviceEvent == null ? "" : deviceEvent.id()));
    }

    private void releaseLockAfterWrite(
            WebAdminInteractionItemMatcherUpdateRequest request,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress
    ) {
        if (editLockService == null || request == null || isBlank(request.lockId)) {
            return;
        }
        editLockService.releaseAfterWrite(
                WebAdminEditLockService.TARGET_INTERACTION_ITEM_MATCHER,
                request.deviceId,
                request.lockId,
                user,
                session,
                remoteAddress
        );
    }

    private static WebAdminWriteResult conflictDetected(
            WebAdminWriteTarget target,
            SignalDeviceData current,
            String expectedFingerprint
    ) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", expectedFingerprint);
        conflict.put("currentFingerprint", fingerprintFor(current));
        conflict.put("currentInteractionItemMatcher", currentSummary(current));
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "交互物品匹配配置已被其他操作修改，请刷新后再编辑。",
                target.targetType(),
                target.targetId(),
                false,
                List.of(),
                "",
                "",
                false,
                conflict,
                Map.of()
        );
    }

    private static WebAdminWriteTarget target(String deviceId, String displayName) {
        return new WebAdminWriteTarget("INTERACTION_ITEM_MATCHER", safe(deviceId), safe(displayName));
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        if (WebAdminWriteResultCode.CSRF_REQUIRED.id().equals(code)) {
            return WebAdminWriteResultCode.CSRF_REQUIRED;
        }
        if (WebAdminWriteResultCode.CSRF_INVALID.id().equals(code)) {
            return WebAdminWriteResultCode.CSRF_INVALID;
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    private static String normalizeItemId(String value) {
        return safe(value).trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeCountMode(String value) {
        String raw = safe(value).trim().toLowerCase(Locale.ROOT);
        return ALLOWED_COUNT_MODES.contains(raw) ? raw : raw;
    }

    private static boolean isValidItemId(String value) {
        return value != null && value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
    }

    private static List<String> normalizeLore(List<String> lines) {
        List<String> result = new ArrayList<>();
        if (lines == null) {
            return List.of();
        }
        for (String line : lines) {
            String text = line == null ? "" : line.trim();
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return List.copyOf(result);
    }

    private static Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (Double.isFinite(doubleValue) && Math.floor(doubleValue) == doubleValue
                    && doubleValue >= Integer.MIN_VALUE && doubleValue <= Integer.MAX_VALUE) {
                return (int) doubleValue;
            }
            return null;
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static int parseIntegerOr(Object value, int fallback) {
        Integer parsed = parseInteger(value);
        return parsed == null ? fallback : parsed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }
}
