package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.device.ItemSubmitRequirementData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import com.zcpu.tzzmod.signal.device.item.InteractionItemVanillaPolicy;
import com.zcpu.tzzmod.signal.device.item.InventoryConsumeOrder;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherSupport;
import com.zcpu.tzzmod.webadmin.WebAdminJsonResponse;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSingleItemSubmitTemplateSessionCancelRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSingleItemSubmitTemplateSessionStartRequest;
import com.zcpu.tzzmod.webadmin.itemsubmit.WebAdminSingleItemSubmitTemplateSession;
import com.zcpu.tzzmod.webadmin.itemsubmit.WebAdminSingleItemSubmitTemplateSessions;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
import com.zcpu.tzzmod.webadmin.write.WebAdminOperationType;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionDecision;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminValidationError;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteContext;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResultCode;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteTarget;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService {
    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;

    public WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
    }

    public Map<String, Object> overview(MinecraftServer server, WebAdminUser user, WebAdminSession session, String deviceRef) {
        SignalDeviceData device = findDevice(server, deviceRef);
        if (device == null) {
            return null;
        }
        if (!SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
            Map<String, Object> unsupported = new LinkedHashMap<>();
            unsupported.put("deviceId", device.id());
            unsupported.put("deviceType", WebAdminReadonlySupport.deviceType(device));
            unsupported.put("displayName", WebAdminReadonlySupport.deviceDisplayName(device));
            unsupported.put("supported", false);
            unsupported.put("unsupportedReason", "单物品 itemSubmit 模板编辑只支持 virtual_block_device。");
            return unsupported;
        }
        Map<String, Object> data = templateData(device);
        data.put("supported", true);
        data.put("typeSupported", true);
        data.put("phase", "7.10");
        data.put("rightClickConditionLayer", true);
        data.put("singleRequirementOnly", device.itemSubmitRequirements().size() <= 1);
        data.put("multiRequirementEditable", false);
        data.put("consumeEditor", true);
        data.put("noRawJson", true);
        data.put("noConditionEngine", true);
        data.put("lockTarget", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT + ":" + device.id());
        data.put("expectedFingerprint", fingerprintFor(device));
        data.put("lockStatus", editLockService == null ? null : editLockService.status(
                WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT,
                device.id(),
                user,
                session
        ));
        return data;
    }

    public Map<String, Object> status(String sessionId) {
        return WebAdminSingleItemSubmitTemplateSessions.status(sessionId);
    }

    public WebAdminWriteResult start(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String deviceRef,
            WebAdminSingleItemSubmitTemplateSessionStartRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        SignalDeviceData device = findDevice(server, deviceRef);
        String safeDeviceId = device == null ? safe(deviceRef) : device.id();
        WebAdminWriteTarget target = target(safeDeviceId, device == null ? safeDeviceId : WebAdminReadonlySupport.deviceDisplayName(device));
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION, target);
        if (device == null) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "目标 virtual_block_device 不存在或引用不唯一。");
        }
        if (!SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
            return WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError("device", "invalid_type", "单物品 itemSubmit 模板编辑只支持 virtual_block_device。", device.type())));
        }
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION, target);
        if (!preflight.success()) {
            return preflight;
        }
        if (!device.interactionEnabled()) {
            return WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError("interactionEnabled", "required", "itemSubmit 属于右键交互后的提交层，请先启用右键交互触发。", "false")));
        }
        if (device.itemSubmitRequirements().size() > 1) {
            return WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError("itemSubmitRequirements", "multi_requirement_readonly", "当前为多物品提交配置，7.10 单物品编辑器不支持编辑；请等待 7.11。", Integer.toString(device.itemSubmitRequirements().size()))));
        }
        if (request == null) {
            request = new WebAdminSingleItemSubmitTemplateSessionStartRequest();
        }
        request.deviceId = device.id();
        if (isBlank(request.expectedFingerprint)) {
            return WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError("expectedFingerprint", "required", "启动单物品提交模板会话需要 expectedFingerprint。", "")));
        }
        String currentFingerprint = fingerprintFor(device);
        if (!currentFingerprint.equals(request.expectedFingerprint)) {
            Map<String, Object> conflict = new LinkedHashMap<>();
            conflict.put("expectedFingerprint", request.expectedFingerprint);
            conflict.put("currentFingerprint", currentFingerprint);
            conflict.put("currentTemplate", templateData(device));
            return new WebAdminWriteResult(false, WebAdminWriteResultCode.CONFLICT_DETECTED.id(), "单物品 itemSubmit 配置已被其他操作修改，请刷新后再启动编辑会话。", target.targetType(), target.targetId(), false, List.of(), "", "", false, conflict, Map.of());
        }
        if (editLockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT,
                    device.id(),
                    request.lockId,
                    user,
                    session
            );
            if (!lockValidation.success()) {
                return lockValidation.result();
            }
        }
        ServerPlayerEntity targetPlayer = findPlayer(server, request.targetPlayerUuid, request.targetPlayerName);
        if (targetPlayer == null) {
            return WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError("targetPlayerName", "offline", "目标玩家不在线，无法打开游戏内单物品提交模板 GUI。", safe(request.targetPlayerName))));
        }
        long now = System.currentTimeMillis();
        String sessionId = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        WebAdminSingleItemSubmitTemplateSession itemSubmitSession = new WebAdminSingleItemSubmitTemplateSession(
                sessionId,
                nonce,
                device.id(),
                WebAdminReadonlySupport.deviceDisplayName(device),
                device.dimension(),
                device.x(),
                device.y(),
                device.z(),
                device.blockId(),
                targetPlayer.getUuid(),
                targetPlayer.getName().getString(),
                request.lockId,
                currentFingerprint,
                now,
                now + WebAdminSingleItemSubmitTemplateSessions.SESSION_TTL_MILLIS,
                user,
                session,
                context,
                singleRequirementDto(device)
        );
        return WebAdminSingleItemSubmitTemplateSessions.startSession(server, targetPlayer, editLockService, itemSubmitSession);
    }

    public WebAdminWriteResult cancel(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String deviceRef,
            WebAdminSingleItemSubmitTemplateSessionCancelRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        SignalDeviceData device = findDevice(server, deviceRef);
        String safeDeviceId = device == null ? safe(deviceRef) : device.id();
        WebAdminWriteTarget target = target(safeDeviceId, device == null ? safeDeviceId : WebAdminReadonlySupport.deviceDisplayName(device));
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.CANCEL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION, target);
        if (device == null) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "目标 virtual_block_device 不存在或引用不唯一。");
        }
        WebAdminWriteResult preflight = writePreflight(user, session, csrfToken, sameOrigin, WebAdminOperationType.CANCEL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION, target);
        if (!preflight.success()) {
            return preflight;
        }
        String sessionId = safe(request == null ? "" : request.sessionId);
        if (isBlank(sessionId)) {
            return WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError("sessionId", "required", "取消单物品提交模板会话需要 sessionId。", "")));
        }
        return WebAdminSingleItemSubmitTemplateSessions.cancelFromWebAdmin(sessionId, context, request == null ? "" : request.reason, device.id());
    }

    public static String fingerprintFor(SignalDeviceData rawDevice) {
        SignalDeviceData device = rawDevice == null ? null : rawDevice.normalized();
        if (device == null) {
            return "";
        }
        Map<String, Object> fp = new LinkedHashMap<>();
        fp.put("deviceId", device.id());
        fp.put("type", device.type());
        fp.put("interactionEnabled", device.interactionEnabled());
        fp.put("itemSubmitEnabled", device.itemSubmitEnabled());
        fp.put("itemSubmitConsumeEnabled", device.itemSubmitConsumeEnabled());
        fp.put("itemSubmitConsumeOrder", device.itemSubmitConsumeOrder());
        fp.put("interactionItemMatcherEnabled", device.interactionItemMatcherEnabled());
        fp.put("vanillaPolicy", device.interactionItemMatcher().normalized().interactionItemVanillaPolicy());
        fp.put("requirementCount", device.itemSubmitRequirements().size());
        fp.put("singleRequirement", singleRequirementFingerprintDto(device));
        String input = "virtual_block_device_single_item_submit|" + WebAdminJsonResponse.GSON.toJson(fp);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public static Map<String, Object> singleRequirementDto(SignalDeviceData device) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("itemSubmitEnabled", device != null && device.itemSubmitEnabled());
        data.put("itemSubmitConsumeEnabled", device != null && device.itemSubmitConsumeEnabled());
        data.put("itemSubmitConsumeOrder", device == null ? "" : device.itemSubmitConsumeOrder());
        data.put("itemSubmitConsumeOrderDisplayName", InventoryConsumeOrder.displayName(device == null ? "" : device.itemSubmitConsumeOrder()));
        data.put("requirementCount", device == null ? 0 : device.itemSubmitRequirements().size());
        data.put("multiRequirementReadOnly", device != null && device.itemSubmitRequirements().size() > 1);
        data.put("advancedMatcherReadOnly", false);
        data.put("advancedMatcherEditable", true);
        data.put("singleItemSubmitOnly", true);
        data.put("consumeReadOnly", false);
        data.put("consumeEditor", true);
        data.put("countModeValues", List.of(
                ContainerItemCountMode.AT_LEAST.id(),
                ContainerItemCountMode.EXACTLY.id(),
                ContainerItemCountMode.AT_MOST.id(),
                ContainerItemCountMode.IGNORE.id()
        ));
        data.put("consumeOrderValues", List.of(
                InventoryConsumeOrder.HOTBAR_FIRST,
                InventoryConsumeOrder.MAIN_INVENTORY_FIRST
        ));
        data.put("vanillaPolicyValues", List.of(
                InteractionItemVanillaPolicy.ALLOW,
                InteractionItemVanillaPolicy.REQUIRE_ITEM_MATCH
        ));
        data.put("rightClickInteractionEnabled", device != null && device.interactionEnabled());
        ItemStackMatcherData vanillaMatcher = device == null ? ItemStackMatcherData.empty() : device.interactionItemMatcher().normalized();
        data.put("interactionItemVanillaPolicy", vanillaMatcher.interactionItemVanillaPolicy());
        data.put("interactionItemVanillaPolicyDisplayName", InteractionItemVanillaPolicy.displayName(vanillaMatcher.interactionItemVanillaPolicy()));
        if (device == null || device.itemSubmitRequirements().isEmpty()) {
            data.put("configured", false);
            data.put("templateItemId", "");
            data.put("templateCount", 0);
            data.put("countMode", "at_least");
            data.put("requiredCount", 1);
            data.put("requirementEnabled", true);
            data.put("consumeCount", 1);
            data.put("matchDamage", false);
            data.put("matchCustomName", false);
            data.put("matchLore", false);
            data.put("matchCustomData", false);
            data.put("matchComponents", false);
            data.put("templateDamage", 0);
            data.put("templateCustomName", "");
            data.put("templateLore", List.of());
            data.put("templateCustomData", "");
            data.put("templateComponents", "");
            data.put("templateDisplayStack", "");
            data.put("displayTemplateComponentsPreserved", false);
            data.put("summary", "未配置单物品提交模板");
            return data;
        }
        ItemSubmitRequirementData requirement = device.itemSubmitRequirements().get(0).normalized();
        ItemStackMatcherData matcher = requirement.matcher().normalized();
        data.put("configured", matcher.enabled() && !matcher.templateItemId().isBlank());
        data.put("requirementId", requirement.id());
        data.put("requirementName", requirement.name());
        data.put("requirementEnabled", requirement.enabled());
        data.put("templateItemId", matcher.templateItemId());
        data.put("templateCount", matcher.templateCount());
        data.put("countMode", matcher.countMode());
        data.put("requiredCount", matcher.requiredCount());
        data.put("consumeCount", requirement.consumeCount());
        data.put("matchDamage", matcher.matchDamage());
        data.put("matchCustomName", matcher.matchCustomName());
        data.put("matchLore", matcher.matchLore());
        data.put("matchCustomData", matcher.matchCustomData());
        data.put("matchComponents", matcher.matchComponents());
        data.put("templateDamage", matcher.templateDamage());
        data.put("templateCustomName", matcher.templateCustomName());
        data.put("templateLore", matcher.templateLore());
        data.put("templateCustomData", matcher.templateCustomData());
        data.put("templateComponents", matcher.templateComponents());
        data.put("templateDisplayStack", matcher.templateDisplayStack());
        data.put("displayTemplateComponentsPreserved", !matcher.templateDisplayStack().isBlank());
        data.put("matcherSummary", matcher.templateSummary());
        data.put("summary", ItemStackMatcherSupport.summary(matcher));
        data.put("lastMatched", requirement.lastMatched());
        data.put("lastMatchedCount", requirement.lastMatchedCount());
        data.put("lastResult", requirement.lastResult());
        return data;
    }

    private static boolean hasUnsupportedAdvancedItemSubmitMatcher(SignalDeviceData device) {
        if (device == null || device.itemSubmitRequirements().size() != 1) {
            return false;
        }
        ItemStackMatcherData matcher = device.itemSubmitRequirements().get(0).normalized().matcher().normalized();
        return matcher.matchDamage()
                || matcher.matchCustomName()
                || matcher.matchLore()
                || matcher.matchCustomData()
                || matcher.matchComponents();
    }

    private static Map<String, Object> singleRequirementFingerprintDto(SignalDeviceData device) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("itemSubmitEnabled", device != null && device.itemSubmitEnabled());
        data.put("requirementCount", device == null ? 0 : device.itemSubmitRequirements().size());
        if (device == null || device.itemSubmitRequirements().isEmpty()) {
            data.put("configured", false);
            return data;
        }
        ItemSubmitRequirementData requirement = device.itemSubmitRequirements().get(0).normalized();
        ItemStackMatcherData matcher = requirement.matcher().normalized();
        data.put("id", requirement.id());
        data.put("name", requirement.name());
        data.put("enabled", requirement.enabled());
        data.put("consumeCount", requirement.consumeCount());
        data.put("matcherEnabled", matcher.enabled());
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
        data.put("templateCustomData", matcher.templateCustomData());
        data.put("templateComponents", matcher.templateComponents());
        data.put("templateDisplayStack", matcher.templateDisplayStack());
        data.put("templateSummary", matcher.templateSummary());
        return data;
    }

    private static Map<String, Object> templateData(SignalDeviceData device) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceId", device.id());
        data.put("deviceType", WebAdminReadonlySupport.deviceType(device));
        data.put("displayName", WebAdminReadonlySupport.deviceDisplayName(device));
        data.put("dimension", device.dimension());
        data.put("x", device.x());
        data.put("y", device.y());
        data.put("z", device.z());
        data.put("blockId", device.blockId());
        data.put("interactionEnabled", device.interactionEnabled());
        data.put("interactChannel", device.interactChannel());
        data.put("itemSubmit", singleRequirementDto(device));
        data.put("notes", List.of(
                "7.10 只编辑右键交互后的单物品 itemSubmit 模板。",
                "左键复制模板、右键清空模板、滚轮 / Ctrl+滚轮调整匹配数量。",
                "本阶段补齐单 requirement 的启用、数量规则、matcher 选项、提交后消耗和原版交互策略；多 requirement、inventory/equipment 和 ConditionEngine 留到后续阶段。",
                "GUI 回显使用独立 display template snapshot 保留附魔、名称、Lore、damage 和组件显示数据；是否参与匹配仍由 matcher option 控制。"
        ));
        return data;
    }

    private WebAdminWriteResult writePreflight(WebAdminUser user, WebAdminSession session, String csrfToken, boolean sameOrigin, WebAdminOperationType operationType, WebAdminWriteTarget target) {
        WebAdminPermissionDecision permission = permissionService.decide(user, operationType);
        if (!permission.allowed()) {
            return permission.asWriteResult(target);
        }
        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            return WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
        }
        if (!sameOrigin) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.CSRF_INVALID, target, "写请求来源校验失败，请刷新页面后重试。");
        }
        return WebAdminWriteResult.ok(target, false, "写入安全校验通过。");
    }

    private static ServerPlayerEntity findPlayer(MinecraftServer server, String uuid, String name) {
        if (server == null) {
            return null;
        }
        if (!isBlank(uuid)) {
            try {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(UUID.fromString(uuid.trim()));
                if (player != null) {
                    return player;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        String safeName = safe(name).trim();
        if (safeName.isBlank()) {
            return null;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getName().getString().equalsIgnoreCase(safeName)) {
                return player;
            }
        }
        return null;
    }

    private static SignalDeviceData findDevice(MinecraftServer server, String deviceId) {
        if (server == null || isBlank(deviceId)) {
            return null;
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, deviceId);
        return resolved.foundUnique() ? resolved.device().normalized() : null;
    }

    private static WebAdminWriteTarget target(String deviceId, String displayName) {
        return new WebAdminWriteTarget("VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT", safe(deviceId), safe(displayName));
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
