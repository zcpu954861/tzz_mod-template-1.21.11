package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.device.ContainerItemConditionData;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionSupport;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionType;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.webadmin.WebAdminJsonResponse;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.container.WebAdminContainerTemplateSession;
import com.zcpu.tzzmod.webadmin.container.WebAdminContainerTemplateSessions;
import com.zcpu.tzzmod.webadmin.dto.WebAdminContainerTemplateSessionCancelRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminContainerTemplateSessionStartRequest;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WebAdminVirtualBlockDeviceContainerTemplateSessionService {
    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;

    public WebAdminVirtualBlockDeviceContainerTemplateSessionService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
    }

    public Map<String, Object> overview(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String deviceRef
    ) {
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
            unsupported.put("typeSupported", false);
            unsupported.put("unsupportedReason", "容器内容变化模板编辑只支持 virtual_block_device。");
            return unsupported;
        }
        Map<String, Object> data = templateData(device);
        data.put("supported", true);
        data.put("typeSupported", true);
        data.put("readOnly", true);
        data.put("p3bGhostEditing", true);
        data.put("saveImplemented", true);
        data.put("dryRunGhostInteraction", false);
        data.put("lockTarget", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE + ":" + device.id());
        data.put("expectedFingerprint", fingerprintFor(device));
        data.put("lockStatus", editLockService == null ? null : editLockService.status(
                WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE,
                device.id(),
                user,
                session
        ));
        data.put("notes", List.of(
                "7.9 P3b 通过游戏内箱子式模板 GUI 编辑容器内容变化 itemConditions。",
                "左键复制模板、右键清空模板格、滚轮 / Ctrl+滚轮调整数量。",
                "模板条件默认继承 VBD 容器内容变化频道；不要求每个模板格单独配置 channel。",
                "点击保存才写入 itemConditions；取消不会保存，不会消耗玩家物品或修改世界容器。"
        ));
        return data;
    }

    public Map<String, Object> status(String sessionId) {
        return WebAdminContainerTemplateSessions.status(sessionId);
    }

    public WebAdminWriteResult start(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String deviceRef,
            WebAdminContainerTemplateSessionStartRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String safeDeviceRef = safe(deviceRef);
        SignalDeviceData device = findDevice(server, safeDeviceRef);
        WebAdminWriteTarget target = target(device == null ? safeDeviceRef : device.id(), device == null ? safeDeviceRef : WebAdminReadonlySupport.deviceDisplayName(device));
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION, target);
        if (device == null) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "目标 virtual_block_device 不存在或引用不唯一。");
        }
        if (!SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
            return WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "invalid_type",
                    "容器内容变化模板编辑只支持 virtual_block_device。",
                    device.type()
            )));
        }
        WebAdminWriteResult preflight = writePreflight(
                user,
                session,
                csrfToken,
                sameOrigin,
                WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION,
                target
        );
        if (!preflight.success()) {
            return preflight;
        }
        if (request == null) {
            request = new WebAdminContainerTemplateSessionStartRequest();
        }
        request.deviceId = device.id();
        boolean logicChainDraftOnly = Boolean.TRUE.equals(request.logicChainDraftOnly);
        if (isBlank(request.expectedFingerprint)) {
            return WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "expectedFingerprint",
                    "required",
                    "启动容器模板会话需要 expectedFingerprint，用于避免覆盖其他操作的模板修改。",
                    ""
            )));
        }
        String currentFingerprint = fingerprintFor(device);
        if (!currentFingerprint.equals(request.expectedFingerprint)) {
            Map<String, Object> conflict = new LinkedHashMap<>();
            conflict.put("expectedFingerprint", request.expectedFingerprint);
            conflict.put("currentFingerprint", currentFingerprint);
            conflict.put("currentTemplate", templateData(device));
            return new WebAdminWriteResult(
                    false,
                    WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                    "容器内容变化模板已被其他操作修改，请刷新后再启动编辑会话。",
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
        if (editLockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE,
                    device.id(),
                    request.lockId,
                    user,
                    session
            );
            if (!lockValidation.success()) {
                return lockValidation.result();
            }
        }
        if (logicChainDraftOnly && editLockService != null) {
            WebAdminEditLockService.LockValidation logicChainLock = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_LOGIC_CHAIN_EDITOR,
                    logicChainTargetId(request.logicChainRootType, request.logicChainRootRef),
                    request.logicChainEditLockId,
                    user,
                    session
            );
            if (!logicChainLock.success()) {
                return logicChainLock.result();
            }
        }
        ServerPlayerEntity targetPlayer = findPlayer(server, request.targetPlayerUuid, request.targetPlayerName);
        if (targetPlayer == null) {
            return WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "targetPlayerName",
                    "offline",
                    "目标玩家不在线，无法打开游戏内容器模板 GUI。",
                    safe(request.targetPlayerName)
            )));
        }

        long now = System.currentTimeMillis();
        String sessionId = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        WebAdminContainerTemplateSession templateSession = new WebAdminContainerTemplateSession(
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
                now + WebAdminContainerTemplateSessions.SESSION_TTL_MILLIS,
                user,
                session,
                context,
                itemConditionDtos(device.itemConditions(), device.containerChangeChannel()),
                logicChainDraftOnly,
                safe(request.logicChainCaptureDraftId),
                safe(request.logicChainEditLockId),
                normalizeLogicChainRootType(request.logicChainRootType),
                safe(request.logicChainRootRef),
                safe(request.logicChainDraftNodeId),
                safe(request.logicChainTriggerKey),
                intValue(request.logicChainRequirementIndex)
        );
        return WebAdminContainerTemplateSessions.startSession(server, targetPlayer, editLockService, templateSession);
    }

    public WebAdminWriteResult cancel(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String deviceRef,
            WebAdminContainerTemplateSessionCancelRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        SignalDeviceData device = findDevice(server, deviceRef);
        String safeDeviceId = device == null ? safe(deviceRef) : device.id();
        WebAdminWriteTarget target = target(safeDeviceId, device == null ? safeDeviceId : WebAdminReadonlySupport.deviceDisplayName(device));
        WebAdminWriteContext context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.CANCEL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION, target);
        if (device == null) {
            return WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "目标 virtual_block_device 不存在或引用不唯一。");
        }
        if (!SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
            return WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "invalid_type",
                    "容器内容变化模板会话只支持 virtual_block_device。",
                    device.type()
            )));
        }
        WebAdminWriteResult preflight = writePreflight(
                user,
                session,
                csrfToken,
                sameOrigin,
                WebAdminOperationType.CANCEL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION,
                target
        );
        if (!preflight.success()) {
            return preflight;
        }
        String sessionId = safe(request == null ? "" : request.sessionId);
        if (isBlank(sessionId)) {
            return WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "sessionId",
                    "required",
                    "取消容器模板会话需要 sessionId。",
                    ""
            )));
        }
        return WebAdminContainerTemplateSessions.cancelFromWebAdmin(sessionId, context, request == null ? "" : request.reason);
    }

    public static String fingerprintFor(SignalDeviceData rawDevice) {
        SignalDeviceData device = rawDevice == null ? null : rawDevice.normalized();
        if (device == null) {
            return "";
        }
        String input = "virtual_block_device_container_template|"
                + device.id()
                + "|"
                + device.type()
                + "|"
                + WebAdminJsonResponse.GSON.toJson(fingerprintConditions(device.itemConditions()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private WebAdminWriteResult writePreflight(
            WebAdminUser user,
            WebAdminSession session,
            String csrfToken,
            boolean sameOrigin,
            WebAdminOperationType operationType,
            WebAdminWriteTarget target
    ) {
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
        data.put("containerChangeChannel", device.containerChangeChannel());
        data.put("containerChangeChannelMissingWarning", device.containerChangeChannel().isBlank());
        data.put("itemConditionChannelInheritance", true);
        data.put("p3bItemConditionInheritsContainerChangeChannel", true);
        data.put("perSlotChannelRequired", false);
        data.put("itemConditionChannelInheritanceNote", "P3b 模板条件默认继承 VBD 容器内容变化频道；显式 condition.channel 仅作为覆盖。");
        List<Map<String, Object>> all = itemConditionDtos(device.itemConditions(), device.containerChangeChannel());
        List<Map<String, Object>> slots = new ArrayList<>();
        List<Map<String, Object>> totals = new ArrayList<>();
        List<Map<String, Object>> advanced = new ArrayList<>();
        int maxSlot = 0;
        for (Map<String, Object> condition : all) {
            String type = String.valueOf(condition.getOrDefault("type", ""));
            int slot = intValue(condition.get("slot"));
            if (type.startsWith("slot_")) {
                slots.add(condition);
                maxSlot = Math.max(maxSlot, slot);
            } else if (type.startsWith("total_")) {
                totals.add(condition);
            } else {
                advanced.add(condition);
            }
        }
        data.put("itemConditions", all);
        data.put("slotConditions", List.copyOf(slots));
        data.put("totalConditions", List.copyOf(totals));
        data.put("advancedConditions", List.copyOf(advanced));
        data.put("itemConditionCount", all.size());
        data.put("slotCount", slotPreviewCount(maxSlot, slots.isEmpty()));
        data.put("p3bSavesItemConditions", true);
        data.put("saveEnabledInP3b", true);
        data.put("ghostTemplateEditingEnabled", true);
        data.put("noRealInventoryTransfer", true);
        return data;
    }

    private static List<Map<String, Object>> itemConditionDtos(List<ContainerItemConditionData> rawConditions) {
        return itemConditionDtos(rawConditions, "");
    }

    private static List<Map<String, Object>> itemConditionDtos(List<ContainerItemConditionData> rawConditions, String inheritedContainerChangeChannel) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ContainerItemConditionData raw : rawConditions == null ? List.<ContainerItemConditionData>of() : rawConditions) {
            ContainerItemConditionData condition = raw == null ? null : raw.normalized();
            if (condition == null) {
                continue;
            }
            String effectiveChannel = ContainerItemConditionSupport.effectiveChannel(condition, inheritedContainerChangeChannel, false);
            String effectiveOffChannel = ContainerItemConditionSupport.effectiveChannel(condition, inheritedContainerChangeChannel, true);
            String effectiveChannelSource = ContainerItemConditionSupport.effectiveChannelSource(condition, inheritedContainerChangeChannel, false);
            String effectiveOffChannelSource = ContainerItemConditionSupport.effectiveChannelSource(condition, inheritedContainerChangeChannel, true);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", condition.id());
            data.put("name", condition.name());
            data.put("enabled", condition.enabled());
            data.put("type", condition.type());
            data.put("slot", condition.slot());
            data.put("itemId", condition.itemId());
            data.put("countMode", condition.countMode());
            data.put("count", condition.count());
            data.put("channel", condition.channel());
            data.put("offChannel", condition.offChannel());
            data.put("effectiveChannel", effectiveChannel);
            data.put("effectiveOffChannel", effectiveOffChannel);
            data.put("effectiveChannelSource", effectiveChannelSource);
            data.put("effectiveOffChannelSource", effectiveOffChannelSource);
            data.put("inheritsContainerChangeChannel", "container_change_channel".equals(effectiveChannelSource));
            data.put("perSlotChannelRequired", false);
            data.put("channelInheritanceEnabled", true);
            data.put("mode", condition.mode());
            data.put("lastMatched", condition.lastMatched());
            data.put("lastResult", condition.lastResult());
            data.put("displayZone", displayZone(condition.type()));
            data.put("readonly", false);
            data.put("editableInP3b", true);
            data.put("participatingFieldsDependOnMode", true);
            ItemStackMatcherData matcher = condition.matcher() == null ? ItemStackMatcherData.empty() : condition.matcher().normalized();
            data.put("matcherEnabled", matcher.enabled());
            data.put("matcherTemplateItemId", matcher.templateItemId());
            data.put("matcherTemplateCount", matcher.templateCount());
            data.put("matcherCountMode", matcher.countMode());
            data.put("matcherRequiredCount", matcher.requiredCount());
            data.put("matcherMatchItemId", matcher.matchItemId());
            data.put("matcherMatchDamage", matcher.matchDamage());
            data.put("matcherMatchCustomName", matcher.matchCustomName());
            data.put("matcherMatchLore", matcher.matchLore());
            data.put("matcherMatchCustomData", matcher.matchCustomData());
            data.put("matcherMatchComponents", matcher.matchComponents());
            data.put("matcherTemplateDamage", matcher.templateDamage());
            data.put("matcherTemplateCustomName", matcher.templateCustomName());
            data.put("matcherTemplateLore", matcher.templateLore());
            data.put("matcherSummary", matcherSummary(condition, matcher));
            data.put("templateItemId", templateItemId(condition, matcher));
            data.put("templateCount", templateCount(condition, matcher));
            data.put("templateCountMode", templateCountMode(condition, matcher));
            data.put("hasGhostTemplateItem", !templateItemId(condition, matcher).isBlank());
            result.add(data);
        }
        result.sort(Comparator
                .comparing((Map<String, Object> entry) -> displayZone(String.valueOf(entry.get("type"))))
                .thenComparingInt(entry -> intValue(entry.get("slot")))
                .thenComparing(entry -> String.valueOf(entry.getOrDefault("id", ""))));
        return List.copyOf(result);
    }

    private static List<Map<String, Object>> fingerprintConditions(List<ContainerItemConditionData> rawConditions) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> condition : itemConditionDtos(rawConditions)) {
            Map<String, Object> fp = new LinkedHashMap<>();
            for (String key : List.of(
                    "id",
                    "name",
                    "enabled",
                    "type",
                    "slot",
                    "itemId",
                    "countMode",
                    "count",
                    "channel",
                    "offChannel",
                    "mode",
                    "matcherEnabled",
                    "matcherTemplateItemId",
                    "matcherTemplateCount",
                    "matcherCountMode",
                    "matcherRequiredCount",
                    "matcherMatchItemId",
                    "matcherMatchDamage",
                    "matcherMatchCustomName",
                    "matcherMatchLore",
                    "matcherMatchCustomData",
                    "matcherMatchComponents",
                    "matcherTemplateDamage",
                    "matcherTemplateCustomName",
                    "matcherTemplateLore",
                    "matcherSummary"
            )) {
                fp.put(key, condition.get(key));
            }
            result.add(fp);
        }
        return List.copyOf(result);
    }

    private static String matcherSummary(ContainerItemConditionData condition, ItemStackMatcherData matcher) {
        if (!matcher.enabled()) {
            return "";
        }
        if (!isBlank(matcher.templateSummary())) {
            return matcher.templateSummary();
        }
        String item = isBlank(matcher.templateItemId()) ? condition.itemId() : matcher.templateItemId();
        return (isBlank(item) ? "matcher" : item) + " · " + matcher.countMode() + " " + matcher.requiredCount();
    }

    private static String templateItemId(ContainerItemConditionData condition, ItemStackMatcherData matcher) {
        if (ContainerItemConditionType.SLOT_MATCHER.id().equals(condition.type())
                || ContainerItemConditionType.TOTAL_MATCHER.id().equals(condition.type())) {
            return matcher.templateItemId();
        }
        return condition.itemId();
    }

    private static int templateCount(ContainerItemConditionData condition, ItemStackMatcherData matcher) {
        if (ContainerItemConditionType.SLOT_MATCHER.id().equals(condition.type())
                || ContainerItemConditionType.TOTAL_MATCHER.id().equals(condition.type())) {
            return Math.max(1, matcher.templateCount() <= 0 ? matcher.requiredCount() : matcher.templateCount());
        }
        return Math.max(1, condition.count());
    }

    private static String templateCountMode(ContainerItemConditionData condition, ItemStackMatcherData matcher) {
        if (ContainerItemConditionType.SLOT_MATCHER.id().equals(condition.type())
                || ContainerItemConditionType.TOTAL_MATCHER.id().equals(condition.type())) {
            return matcher.countMode();
        }
        return condition.countMode();
    }

    private static String displayZone(String type) {
        String safeType = safe(type);
        if (safeType.startsWith("slot_")) {
            return "slot";
        }
        if (safeType.startsWith("total_")) {
            return "total";
        }
        return "advanced";
    }

    private static int slotPreviewCount(int maxSlot, boolean empty) {
        if (empty) {
            return 27;
        }
        int needed = Math.max(27, maxSlot + 1);
        int rounded = ((needed + 8) / 9) * 9;
        return Math.min(54, Math.max(27, rounded));
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
        return new WebAdminWriteTarget("VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE", safe(deviceId), safe(displayName));
    }

    private static String logicChainTargetId(String rootType, String rootRef) {
        return (normalizeLogicChainRootType(rootType) + ":" + safe(rootRef)).replaceAll("[\\r\\n\\t]", "_");
    }

    private static String normalizeLogicChainRootType(String rootType) {
        String value = safe(rootType).toLowerCase();
        return switch (value) {
            case "device", "listener", "receiver", "relay", "region", "region_controller", "action", "signal_join", "timer" -> value;
            default -> "channel";
        };
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
