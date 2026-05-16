package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.ModBlock.ModBlocks;
import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.condition.runtime.ConditionActionGateService;
import com.zcpu.tzzmod.condition.runtime.ConditionRuntimeTargetType;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.WebAdminConditionGroupStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class WebAdminActionRelayActionsService {
    public static final int MAX_ACTIONS = 64;
    public static final int MAX_COMMAND_LENGTH = 512;
    public static final int MAX_MESSAGE_LENGTH = 500;
    public static final int MAX_SOUND_ID_LENGTH = 128;

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;
    private final WebAdminConditionGateBindingValidator gateBindingValidator = new WebAdminConditionGateBindingValidator();

    public WebAdminActionRelayActionsService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
    }

    public Map<String, Object> actionsFor(MinecraftServer server, WebAdminUser user, WebAdminSession session, String deviceId) {
        ActionRelayTarget relayTarget = resolveRelay(server, deviceId);
        if (relayTarget.device() == null) {
            return null;
        }
        if (!SignalDeviceData.TYPE_ACTION_RELAY.equals(relayTarget.device().type())) {
            Map<String, Object> data = baseData(relayTarget.device(), null, user, session);
            data.put("supported", false);
            data.put("typeSupported", false);
            data.put("actionsReadable", false);
            data.put("actionsEditable", false);
            data.put("unsupportedReason", "只有 action_relay 支持 Action 列表。");
            data.put("loadedState", "not_action_relay");
            data.put("worldAvailable", false);
            data.put("chunkLoaded", false);
            data.put("blockEntityLoaded", false);
            data.put("blockEntityType", "");
            data.put("blockId", "");
            data.put("expectedBlockId", "");
            return data;
        }
        boolean typeSupported = SignalDeviceData.TYPE_ACTION_RELAY.equals(relayTarget.device().type());
        boolean actionsReadable = relayTarget.actionsReadable();
        boolean actionsEditable = relayTarget.editable();
        Map<String, Object> data = baseData(relayTarget.device(), relayTarget.relay(), user, session);
        data.put("supported", typeSupported);
        data.put("typeSupported", typeSupported);
        data.put("actionsReadable", actionsReadable);
        data.put("actionsEditable", actionsEditable);
        data.put("unsupportedReason", actionsEditable ? "" : relayTarget.unsupportedReason());
        data.put("loadedState", relayTarget.loadedState());
        data.put("worldAvailable", relayTarget.worldAvailable());
        data.put("chunkLoaded", relayTarget.chunkLoaded());
        data.put("blockEntityLoaded", relayTarget.blockEntityLoaded());
        data.put("blockEntityType", relayTarget.blockEntityType());
        data.put("blockId", relayTarget.blockId());
        data.put("expectedBlockId", relayTarget.expectedBlockId());
        data.put("dimension", relayTarget.device().dimension());
        data.put("position", Map.of("x", relayTarget.device().x(), "y", relayTarget.device().y(), "z", relayTarget.device().z()));
        return data;
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String deviceId,
            WebAdminActionRelayActionsUpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String safeDeviceId = safe(deviceId);
        WebAdminWriteTarget target = target(safeDeviceId, safeDeviceId);
        WebAdminWriteContext writeContext = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS,
                target
        );

        ActionRelayTarget relayTarget = resolveRelay(server, safeDeviceId);
        if (relayTarget.device() == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.TARGET_NOT_FOUND,
                    target,
                    "目标设备不存在或已被删除。"
            );
            audit(writeContext, result, Map.of(), Map.of());
            return result;
        }
        target = target(relayTarget.device().id(), WebAdminReadonlySupport.deviceDisplayName(relayTarget.device()));
        writeContext = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS, target);
        if (request != null) {
            request.deviceId = relayTarget.device().id();
        }

        if (!SignalDeviceData.TYPE_ACTION_RELAY.equals(relayTarget.device().type())) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "invalid_type",
                    "只有 action_relay 支持编辑 Action 列表。",
                    relayTarget.device().type()
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "invalid_type"));
            return result;
        }

        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS);
        if (!permission.allowed()) {
            WebAdminWriteResult result = permission.asWriteResult(target);
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "permission_denied"));
            return result;
        }

        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "csrf_failed"));
            return result;
        }
        if (!sameOrigin) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.CSRF_INVALID,
                    target,
                    "写请求来源校验失败，请刷新页面后重试。"
            );
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "origin_failed"));
            return result;
        }

        if (relayTarget.relay() == null || relayTarget.world() == null) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "unsupported_state",
                    "设备当前不可安全编辑：" + relayTarget.unsupportedReason(),
                    relayTarget.device().type()
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "unloaded"));
            return result;
        }

        if (editLockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS,
                    relayTarget.device().id(),
                    request == null ? "" : request.lockId,
                    user,
                    session
            );
            if (!lockValidation.success()) {
                WebAdminWriteResult result = lockValidation.result();
                audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "edit_lock_failed"));
                return result;
            }
        }

        Validation validation = validateRequest(server, request, gateBindingValidator);
        if (!validation.errors().isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, validation.errors());
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), requestSummary(validation.actions(), request == null ? "" : request.conditionGroupId));
            return result;
        }

        if (request == null || isBlank(request.expectedFingerprint)) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "expectedFingerprint",
                    "required",
                    "保存需要 expectedFingerprint，用于防止覆盖其他操作的修改。",
                    ""
            )));
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "expected_fingerprint_missing"));
            return result;
        }
        if (!fingerprintMatches(relayTarget.device(), relayTarget.relay().actions(), relayTarget.relay().conditionGroupId(), request.expectedFingerprint)) {
            WebAdminWriteResult result = conflictDetected(target, relayTarget.device(), relayTarget.relay().actions(), relayTarget.relay().conditionGroupId(), request.expectedFingerprint);
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }

        List<ActionConfig> beforeActions = normalizeActions(relayTarget.relay().actions());
        List<ActionConfig> afterActions = validation.actions();
        String beforeConditionGroupId = relayTarget.relay().conditionGroupId();
        String afterConditionGroupId = WebAdminConditionGroupStore.normalizeId(request.conditionGroupId);
        if (beforeActions.equals(afterActions) && beforeConditionGroupId.equals(afterConditionGroupId)) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的 Action 列表变化。");
            audit(writeContext, result, currentSummary(relayTarget.device(), relayTarget.relay()), currentSummary(relayTarget.device(), relayTarget.relay()));
            releaseLockAfterWrite(request, user, session, remoteAddress);
            return result;
        }

        relayTarget.relay().setConditionGroupId(afterConditionGroupId);
        relayTarget.relay().replaceActions(afterActions);
        SignalDeviceData updated = SignalDeviceStore.updateActions(relayTarget.world(), relayTarget.pos(), relayTarget.relay());
        SignalDeviceStore.forceFlushDirty(server);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("actionList", baseData(updated == null ? relayTarget.device() : updated, relayTarget.relay(), user, session));
        data.put("changedFields", changedFields(beforeActions, afterActions, beforeConditionGroupId, afterConditionGroupId));
        data.put("actionCountBefore", beforeActions.size());
        data.put("actionCountAfter", afterActions.size());
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "Action Relay 动作列表已保存。",
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
        WebAdminAuditEvent auditEvent = audit(
                writeContext,
                result,
                currentSummary(relayTarget.device(), beforeActions, beforeConditionGroupId),
                currentSummary(updated == null ? relayTarget.device() : updated, afterActions, afterConditionGroupId)
        );
        publishRealtime(updated == null ? relayTarget.device() : updated, auditEvent, user, beforeActions, afterActions, afterConditionGroupId);
        releaseLockAfterWrite(request, user, session, remoteAddress);
        return result;
    }

    public static boolean fingerprintMatches(SignalDeviceData device, List<ActionConfig> actions, String conditionGroupId, String expectedFingerprint) {
        return !isBlank(expectedFingerprint) && fingerprintFor(device, actions, conditionGroupId).equals(expectedFingerprint);
    }

    public static String fingerprintFor(SignalDeviceData rawDevice, List<ActionConfig> rawActions) {
        return fingerprintFor(rawDevice, rawActions, "");
    }

    public static String fingerprintFor(SignalDeviceData rawDevice, List<ActionConfig> rawActions, String conditionGroupId) {
        SignalDeviceData device = rawDevice == null ? null : rawDevice.normalized();
        if (device == null) {
            return "";
        }
        String input = "action_relay_actions|" + device.id() + "|" + device.type() + "|" + device.channel()
                + "|conditionGroupId=" + WebAdminConditionGroupStore.normalizeId(conditionGroupId)
                + "|" + actionFingerprintList(rawActions);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public static List<WebAdminValidationError> validateActionEntries(List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> entries) {
        return validateRequest(null, requestFor(entries), new WebAdminConditionGateBindingValidator()).errors();
    }

    private static WebAdminActionRelayActionsUpdateRequest requestFor(List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> entries) {
        WebAdminActionRelayActionsUpdateRequest request = new WebAdminActionRelayActionsUpdateRequest();
        request.actions = entries == null ? List.of() : entries;
        return request;
    }

    private Map<String, Object> baseData(
            SignalDeviceData device,
            ActionRelayBlockEntity relay,
            WebAdminUser user,
            WebAdminSession session
    ) {
        List<ActionConfig> actions = normalizeActions(relay == null ? List.of() : relay.actions());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceId", device.id());
        data.put("deviceType", device.type());
        data.put("displayName", WebAdminReadonlySupport.deviceDisplayName(device));
        data.put("channel", device.channel());
        data.put("actionCount", relay == null ? device.actionCount() : actions.size());
        data.put("snapshotActionCount", device.actionCount());
        data.put("actions", actionDtos(actions, device.id()));
        data.put("allowedActionTypes", List.of("command", "signal", "message", "sound"));
        data.put("conditionGroupId", relay == null ? "" : relay.conditionGroupId());
        data.put("conditionGateTargetType", ConditionRuntimeTargetType.ACTION_RELAY.id());
        data.put("conditionGateTargetId", device.id());
        data.put("recentConditionGate", WebAdminConditionGateHistoryService.recentStatus(ConditionRuntimeTargetType.ACTION_RELAY, device.id()));
        data.put("expectedFingerprint", fingerprintFor(device, actions, relay == null ? "" : relay.conditionGroupId()));
        WebAdminEditLockStatusDto lockStatus = editLockService == null ? null : editLockService.status(
                WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS,
                device.id(),
                user,
                session
        );
        data.put("lockStatus", lockStatus);
        data.put("noRawJson", true);
        data.put("physicalDeviceDeleteAllowed", false);
        data.put("notes", List.of(
                "Action 列表属于 action_relay BlockEntity 配置，不会创建或删除真实方块。",
                "未配置条件组 = 保持旧继电器逻辑，不拦截；配置后仅作为整条 Action 列表外层 gate。",
                "单条 Action 条件组为空 = 此 action 不单独判断，保持旧执行逻辑；配置后仅跳过当前 action 并继续后续 action。",
                "command action 是地图玩法控制能力；WebAdmin 只硬阻断 ban/kick/op/stop/whitelist 等服务器管理高风险命令。",
                "sound action 当前底层只存储 sound id；per-action cooldown/requiresOp 字段会保留，但执行语义以现有 ActionEngine 为准。"
        ));
        return data;
    }

    private static List<Map<String, Object>> actionDtos(List<ActionConfig> actions, String deviceId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int index = 0; index < actions.size(); index++) {
            ActionConfig action = actions.get(index);
            String actionTargetId = ConditionActionGateService.actionTargetId("relay", deviceId, index);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("index", index);
            entry.put("displayIndex", index + 1);
            entry.put("type", action.type().id());
            entry.put("value", action.value());
            entry.put("enabled", action.enabled());
            entry.put("requiresOp", action.requiresOp());
            entry.put("cooldownTicks", action.cooldownTicks());
            entry.put("notifyOps", action.notifyOps());
            entry.put("conditionGroupId", action.conditionGroupId());
            entry.put("actionConditionGateTargetType", ConditionRuntimeTargetType.ACTION_RELAY_ACTION.id());
            entry.put("actionConditionGateTargetId", actionTargetId);
            entry.put("recentActionConditionGate", WebAdminConditionGateHistoryService.recentStatus(
                    ConditionRuntimeTargetType.ACTION_RELAY_ACTION,
                    actionTargetId
            ));
            entry.put("summary", actionSummary(action));
            result.add(entry);
        }
        return List.copyOf(result);
    }

    private static Validation validateRequest(
            MinecraftServer server,
            WebAdminActionRelayActionsUpdateRequest request,
            WebAdminConditionGateBindingValidator gateBindingValidator
    ) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        WebAdminConditionGateBindingValidator validator = gateBindingValidator == null
                ? new WebAdminConditionGateBindingValidator()
                : gateBindingValidator;
        if (server != null) {
            validator.validate(
                    server,
                    errors,
                    "conditionGroupId",
                    request == null ? "" : request.conditionGroupId,
                    ConditionRuntimeTargetType.ACTION_RELAY
            );
        }
        List<WebAdminActionRelayActionsUpdateRequest.ActionEntry> entries = request == null || request.actions == null
                ? List.of()
                : request.actions;
        if (entries.size() > MAX_ACTIONS) {
            errors.add(new WebAdminValidationError("actions", "too_many", "Action 列表最多支持 " + MAX_ACTIONS + " 条。", String.valueOf(entries.size())));
        }
        List<ActionConfig> actions = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            WebAdminActionRelayActionsUpdateRequest.ActionEntry entry = entries.get(index);
            String prefix = "actions[" + index + "]";
            if (entry == null) {
                errors.add(new WebAdminValidationError(prefix, "required", "Action 配置不能为空。", ""));
                continue;
            }
            ActionType type = parseType(entry.type);
            if (type == null) {
                errors.add(new WebAdminValidationError(prefix + ".type", "invalid_type", "Action 类型必须是 command、signal、message 或 sound。", safe(entry.type)));
                continue;
            }
            Boolean enabled = parseBoolean(entry.enabled);
            Boolean requiresOp = parseBoolean(entry.requiresOp);
            Boolean notifyOps = parseBoolean(entry.notifyOps);
            Integer cooldownTicks = parseInteger(entry.cooldownTicks);
            if (enabled == null) {
                errors.add(new WebAdminValidationError(prefix + ".enabled", "invalid_boolean", "启用状态必须是 boolean。", String.valueOf(entry.enabled)));
                enabled = Boolean.TRUE;
            }
            if (requiresOp == null) {
                errors.add(new WebAdminValidationError(prefix + ".requiresOp", "invalid_boolean", "requiresOp 必须是 boolean。", String.valueOf(entry.requiresOp)));
                requiresOp = Boolean.FALSE;
            }
            if (notifyOps == null) {
                errors.add(new WebAdminValidationError(prefix + ".notifyOps", "invalid_boolean", "notifyOps 必须是 boolean。", String.valueOf(entry.notifyOps)));
                notifyOps = Boolean.FALSE;
            }
            if (cooldownTicks == null || cooldownTicks < 0 || cooldownTicks > 72000) {
                errors.add(new WebAdminValidationError(prefix + ".cooldownTicks", "out_of_range", "Action 冷却字段必须是 0～72000 的整数。", String.valueOf(entry.cooldownTicks)));
                cooldownTicks = 0;
            }
            String value = normalizeValue(type, entry.value);
            validateValue(server, errors, prefix + ".value", type, value);
            String actionConditionGroupId = WebAdminConditionGroupStore.normalizeId(entry.conditionGroupId);
            if (server != null) {
                validator.validate(
                        server,
                        errors,
                        prefix + ".conditionGroupId",
                        actionConditionGroupId,
                        ConditionRuntimeTargetType.ACTION_RELAY_ACTION
                );
            }
            actions.add(new ActionConfig(type, value, enabled, requiresOp, cooldownTicks, notifyOps, actionConditionGroupId));
        }
        return new Validation(List.copyOf(errors), errors.isEmpty() ? normalizeActions(actions) : List.copyOf(actions));
    }

    private static void validateValue(
            MinecraftServer server,
            List<WebAdminValidationError> errors,
            String field,
            ActionType type,
            String value
    ) {
        if (value.isBlank()) {
            errors.add(new WebAdminValidationError(field, "empty", "Action 内容不能为空。", value));
            return;
        }
        if (containsControl(value)) {
            errors.add(new WebAdminValidationError(field, "control_character", "Action 内容不能包含控制字符。", value));
            return;
        }
        switch (type) {
            case COMMAND -> {
                if (value.length() > MAX_COMMAND_LENGTH) {
                    errors.add(new WebAdminValidationError(field, "too_long", "命令长度不能超过 " + MAX_COMMAND_LENGTH + " 个字符。", value));
                } else if (isBlockedServerManagementCommand(value)) {
                    errors.add(new WebAdminValidationError(field, "server_management_command_forbidden", "该命令属于服务器管理高风险命令，不允许通过 WebAdmin action_relay 保存。", value));
                }
            }
            case SIGNAL -> {
                if (value.length() > WebAdminDeviceBasicConfigService.MAX_CHANNEL_LENGTH) {
                    errors.add(new WebAdminValidationError(field, "too_long", "频道长度不能超过 128 个字符。", value));
                } else if (!SignalChannel.isValid(value)) {
                    errors.add(new WebAdminValidationError(field, "invalid_channel", "Signal action 的频道只能包含小写字母、数字、下划线、点、冒号和连字符。", value));
                }
            }
            case MESSAGE -> {
                if (value.length() > MAX_MESSAGE_LENGTH) {
                    errors.add(new WebAdminValidationError(field, "too_long", "消息长度不能超过 " + MAX_MESSAGE_LENGTH + " 个字符。", value));
                }
            }
            case SOUND -> {
                if (value.length() > MAX_SOUND_ID_LENGTH) {
                    errors.add(new WebAdminValidationError(field, "too_long", "音效 ID 长度不能超过 " + MAX_SOUND_ID_LENGTH + " 个字符。", value));
                } else if (!value.matches("[a-z0-9_.:-]+(/[a-z0-9_.:-]+)*")) {
                    errors.add(new WebAdminValidationError(field, "invalid_sound_id", "音效 ID 应使用 minecraft:entity.example 这类小写资源 ID。", value));
                }
            }
        }
    }

    private static boolean isBlockedServerManagementCommand(String command) {
        List<String> tokens = commandTokens(command);
        if (tokens.isEmpty()) {
            return false;
        }
        if (isServerManagementRoot(commandRoot(tokens.getFirst()))) {
            return true;
        }
        if ("execute".equals(commandRoot(tokens.getFirst()))) {
            for (int index = 0; index < tokens.size() - 1; index++) {
                if ("run".equals(commandRoot(tokens.get(index)))
                        && isServerManagementRoot(commandRoot(tokens.get(index + 1)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isServerManagementRoot(String root) {
        return "ban".equals(root)
                || "ban-ip".equals(root)
                || "kick".equals(root)
                || "op".equals(root)
                || "deop".equals(root)
                || "reload".equals(root)
                || "save-off".equals(root)
                || "save-on".equals(root)
                || "stop".equals(root)
                || "whitelist".equals(root)
                || "pardon".equals(root)
                || "pardon-ip".equals(root);
    }

    private static String commandRoot(String token) {
        String value = safe(token).trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        int namespace = value.indexOf(':');
        return namespace >= 0 && namespace + 1 < value.length() ? value.substring(namespace + 1) : value;
    }

    private static List<String> commandTokens(String command) {
        String normalized = ActionConfig.normalizeCommand(command);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        for (int index = 0; index < normalized.length(); index++) {
            char c = normalized.charAt(index);
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                current.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                current.append(c);
                quoted = !quoted;
                continue;
            }
            if (!quoted && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return List.copyOf(tokens);
    }

    private static ActionType parseType(String raw) {
        String value = safe(raw).trim().toLowerCase(Locale.ROOT);
        for (ActionType type : ActionType.values()) {
            if (type.id().equals(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

    private static String normalizeValue(ActionType type, String rawValue) {
        String value = safe(rawValue).trim();
        if (type == ActionType.COMMAND) {
            return ActionConfig.normalizeCommand(value);
        }
        if (type == ActionType.SIGNAL) {
            return SignalChannel.normalize(value);
        }
        return value;
    }

    private static List<ActionConfig> normalizeActions(List<ActionConfig> actions) {
        if (actions == null || actions.isEmpty()) {
            return List.of();
        }
        List<ActionConfig> normalized = new ArrayList<>();
        for (ActionConfig action : actions) {
            if (action == null) {
                continue;
            }
            ActionType type = action.type() == null ? ActionType.COMMAND : action.type();
            normalized.add(new ActionConfig(
                    type,
                    normalizeValue(type, action.value()),
                    action.enabled(),
                    action.requiresOp(),
                    Math.max(0, action.cooldownTicks()),
                    action.notifyOps(),
                    action.conditionGroupId()
            ));
        }
        return List.copyOf(normalized);
    }

    private ActionRelayTarget resolveRelay(MinecraftServer server, String deviceId) {
        if (server == null || isBlank(deviceId)) {
            return ActionRelayTarget.missing();
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, deviceId);
        if (!resolved.foundUnique()) {
            return ActionRelayTarget.missing();
        }
        SignalDeviceData device = resolved.device().normalized();
        if (!SignalDeviceData.TYPE_ACTION_RELAY.equals(device.type())) {
            return ActionRelayTarget.unsupportedType(device);
        }
        ServerWorld world = findWorld(server, device.dimension());
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        boolean worldAvailable = world != null;
        boolean chunkLoaded = false;
        BlockEntity blockEntity = null;
        String blockId = "";
        String expectedBlockId = Registries.BLOCK.getId(ModBlocks.ACTION_RELAY).toString();
        if (world != null) {
            chunkLoaded = world.isChunkLoaded(pos);
            if (chunkLoaded) {
                blockId = Registries.BLOCK.getId(world.getBlockState(pos).getBlock()).toString();
                blockEntity = world.getBlockEntity(pos);
            }
        }
        ActionRelayBlockEntity relay = blockEntity instanceof ActionRelayBlockEntity actionRelay ? actionRelay : null;
        return new ActionRelayTarget(
                device,
                relay,
                world,
                pos,
                worldAvailable,
                chunkLoaded,
                blockEntity != null,
                blockEntity == null ? "" : blockEntity.getClass().getSimpleName(),
                blockId,
                expectedBlockId
        );
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
            WebAdminUser user,
            List<ActionConfig> beforeActions,
            List<ActionConfig> afterActions,
            String conditionGroupId
    ) {
        String deviceId = device.id();
        String routeTarget = "#/devices/" + encode(deviceId);
        List<String> affectedChannels = affectedSignalChannels(device, beforeActions, afterActions);
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .severity("INFO")
                .summary("Action Relay 动作列表已更新。")
                .routeTarget(routeTarget)
                .payload("targetType", "action_relay_actions")
                .payload("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId))
                .payload("affectedChannels", affectedChannels)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEvent actionConfigEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.ACTION_CONFIG_CHANGED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .actionId("ACTION_RELAY:" + deviceId)
                .severity("INFO")
                .summary("Action Relay 动作列表已更新：" + WebAdminReadonlySupport.deviceDisplayName(device))
                .routeTarget(routeTarget)
                .payload("targetType", "action_relay_actions")
                .payload("deviceType", device.type())
                .payload("actionCount", device.actionCount())
                .payload("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId))
                .payload("affectedChannels", affectedChannels)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEvent actionEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.ACTION_CHANGED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .actionId("ACTION_RELAY:" + deviceId)
                .severity("INFO")
                .summary("Action Relay 动作已变化。")
                .routeTarget("#/actions")
                .payload("targetType", "action_relay_actions")
                .payload("deviceId", deviceId)
                .payload("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId))
                .payload("affectedChannels", affectedChannels));
        WebAdminRealtimeEvent deviceConfigEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.DEVICE_CONFIG_CHANGED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .severity("INFO")
                .summary("Action Relay 动作列表已更新。")
                .routeTarget(routeTarget)
                .payload("targetType", "action_relay_actions")
                .payload("deviceType", device.type())
                .payload("actionCount", device.actionCount())
                .payload("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId))
                .payload("affectedChannels", affectedChannels)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget)
                .payload("targetType", "action_relay_actions")
                .payload("deviceType", device.type())
                .payload("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId))
                .payload("affectedChannels", affectedChannels)
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("actionConfigEventId", actionConfigEvent == null ? "" : actionConfigEvent.id())
                .payload("actionEventId", actionEvent == null ? "" : actionEvent.id())
                .payload("deviceConfigEventId", deviceConfigEvent == null ? "" : deviceConfigEvent.id()));
    }

    private static List<String> affectedSignalChannels(
            SignalDeviceData device,
            List<ActionConfig> beforeActions,
            List<ActionConfig> afterActions
    ) {
        Set<String> channels = new LinkedHashSet<>();
        if (device != null && !isBlank(device.channel())) {
            channels.add(SignalChannel.normalize(device.channel()));
        }
        collectSignalActionChannels(channels, beforeActions);
        collectSignalActionChannels(channels, afterActions);
        channels.removeIf(WebAdminActionRelayActionsService::isBlank);
        return List.copyOf(channels);
    }

    private static void collectSignalActionChannels(Set<String> channels, List<ActionConfig> actions) {
        if (channels == null || actions == null) {
            return;
        }
        for (ActionConfig action : actions) {
            if (action != null && action.type() == ActionType.SIGNAL && !isBlank(action.value())) {
                channels.add(SignalChannel.normalize(action.value()));
            }
        }
    }

    private void releaseLockAfterWrite(
            WebAdminActionRelayActionsUpdateRequest request,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress
    ) {
        if (editLockService == null || request == null || isBlank(request.lockId)) {
            return;
        }
        editLockService.releaseAfterWrite(
                WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS,
                request.deviceId,
                request.lockId,
                user,
                session,
                remoteAddress
        );
    }

    private static WebAdminWriteResult conflictDetected(
            WebAdminWriteTarget target,
            SignalDeviceData device,
            List<ActionConfig> actions,
            String conditionGroupId,
            String expectedFingerprint
    ) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", expectedFingerprint);
        conflict.put("currentFingerprint", fingerprintFor(device, actions, conditionGroupId));
        conflict.put("currentActionList", currentSummary(device, actions, conditionGroupId));
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "Action Relay 动作列表已被其他操作修改，请刷新后再编辑。",
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

    private static Map<String, Object> currentSummary(SignalDeviceData device, ActionRelayBlockEntity relay) {
        return currentSummary(device, relay == null ? List.of() : relay.actions(), relay == null ? "" : relay.conditionGroupId());
    }

    private static Map<String, Object> currentSummary(SignalDeviceData device, List<ActionConfig> actions) {
        return currentSummary(device, actions, "");
    }

    private static Map<String, Object> currentSummary(SignalDeviceData device, List<ActionConfig> actions, String conditionGroupId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (device == null) {
            return summary;
        }
        List<ActionConfig> normalizedActions = normalizeActions(actions);
        summary.put("deviceId", device.id());
        summary.put("deviceType", WebAdminReadonlySupport.deviceType(device));
        summary.put("channel", device.channel());
        summary.put("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId));
        summary.put("actionCount", normalizedActions.size());
        summary.put("actions", auditActionSummaryList(normalizedActions));
        summary.put("expectedFingerprint", fingerprintFor(device, normalizedActions, conditionGroupId));
        return summary;
    }

    private static Map<String, Object> requestSummary(List<ActionConfig> actions) {
        return requestSummary(actions, "");
    }

    private static Map<String, Object> requestSummary(List<ActionConfig> actions, String conditionGroupId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("conditionGroupId", WebAdminConditionGroupStore.normalizeId(conditionGroupId));
        summary.put("actionCount", actions == null ? 0 : actions.size());
        summary.put("actions", auditActionSummaryList(actions));
        return summary;
    }

    private static List<String> changedFields(
            List<ActionConfig> beforeActions,
            List<ActionConfig> afterActions,
            String beforeConditionGroupId,
            String afterConditionGroupId
    ) {
        List<String> fields = new ArrayList<>();
        if (!normalizeActions(beforeActions).equals(normalizeActions(afterActions))) {
            fields.add("actions");
        }
        if (!WebAdminConditionGroupStore.normalizeId(beforeConditionGroupId).equals(WebAdminConditionGroupStore.normalizeId(afterConditionGroupId))) {
            fields.add("conditionGroupId");
        }
        return List.copyOf(fields);
    }

    private static List<String> actionSummaryList(List<ActionConfig> actions) {
        return normalizeActions(actions).stream().map(WebAdminActionRelayActionsService::actionSummary).toList();
    }

    private static List<String> actionFingerprintList(List<ActionConfig> actions) {
        return normalizeActions(actions).stream()
                .map(action -> action.type().id()
                        + "|value=" + safe(action.value())
                        + "|enabled=" + action.enabled()
                        + "|requiresOp=" + action.requiresOp()
                        + "|cooldownTicks=" + action.cooldownTicks()
                        + "|notifyOps=" + action.notifyOps()
                        + "|conditionGroupId=" + WebAdminConditionGroupStore.normalizeId(action.conditionGroupId()))
                .toList();
    }

    private static List<String> auditActionSummaryList(List<ActionConfig> actions) {
        return normalizeActions(actions).stream().map(WebAdminActionRelayActionsService::auditActionSummary).toList();
    }

    private static String actionSummary(ActionConfig action) {
        if (action == null || action.type() == null) {
            return "unknown";
        }
        String prefix = action.enabled() ? "" : "[disabled] ";
        return prefix + action.type().id() + ": " + safe(action.value());
    }

    private static String auditActionSummary(ActionConfig action) {
        if (action == null || action.type() == null) {
            return "unknown";
        }
        String prefix = action.enabled() ? "" : "[disabled] ";
        String value = safe(action.value());
        if (action.type() == ActionType.COMMAND) {
            value = "<command redacted length=" + value.length() + ">";
        } else if (action.type() == ActionType.MESSAGE && value.length() > 96) {
            value = value.substring(0, 96) + "...";
        }
        return prefix + action.type().id()
                + ": " + value
                + " requiresOp=" + action.requiresOp()
                + " cooldownTicks=" + action.cooldownTicks()
                + " notifyOps=" + action.notifyOps()
                + " conditionGroupId=" + WebAdminConditionGroupStore.normalizeId(action.conditionGroupId());
    }

    private static Boolean parseBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            if ("true".equalsIgnoreCase(string.trim())) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(string.trim())) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    private static Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (Double.isFinite(doubleValue) && Math.floor(doubleValue) == doubleValue
                    && doubleValue >= Integer.MIN_VALUE && doubleValue <= Integer.MAX_VALUE) {
                return (int) doubleValue;
            }
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean containsControl(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static ServerWorld findWorld(MinecraftServer server, String dimension) {
        if (server == null || isBlank(dimension)) {
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            if (world.getRegistryKey().getValue().toString().equals(dimension)) {
                return world;
            }
        }
        return null;
    }

    private static WebAdminWriteTarget target(String deviceId, String displayName) {
        return new WebAdminWriteTarget("ACTION_RELAY_ACTIONS", safe(deviceId), isBlank(displayName) ? safe(deviceId) : displayName);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    private record ActionRelayTarget(
            SignalDeviceData device,
            ActionRelayBlockEntity relay,
            ServerWorld world,
            BlockPos pos,
            boolean worldAvailable,
            boolean chunkLoaded,
            boolean blockEntityLoaded,
            String blockEntityType,
            String blockId,
            String expectedBlockId
    ) {
        static ActionRelayTarget missing() {
            return new ActionRelayTarget(null, null, null, null, false, false, false, "", "", "");
        }

        static ActionRelayTarget unsupportedType(SignalDeviceData device) {
            return new ActionRelayTarget(device, null, null, null, false, false, false, "", "", "");
        }

        boolean actionsReadable() {
            return relay != null;
        }

        boolean editable() {
            return relay != null;
        }

        String loadedState() {
            return WebAdminDeviceExtendedConfigService.classifyPhysicalRuntimeState(
                    worldAvailable,
                    chunkLoaded,
                    blockId,
                    expectedBlockId,
                    blockEntityLoaded,
                    relay != null
            );
        }

        String unsupportedReason() {
            return switch (loadedState()) {
                case "world_unavailable" -> "设备维度不可用或世界未加载。";
                case "chunk_unloaded" -> "该 action_relay 所在区块未加载。WebAdmin 不会强制加载区块；请让玩家靠近该方块后重试。";
                case "block_missing" -> "区块已加载，但该位置是空气或没有可用方块。预期方块：" + expectedBlockId + "。";
                case "physical_block_mismatch" -> "区块已加载，但当前位置不是 action_relay 方块。预期方块：" + expectedBlockId + "。";
                case "block_entity_missing" -> "当前方块是 " + (isBlank(blockId) ? expectedBlockId : blockId)
                        + "，但区块内缺少 ActionRelayBlockEntity。可能是旧存档、外部编辑或方块实体数据未随区块正常恢复。";
                case "block_entity_type_mismatch" -> "区块已加载，但当前位置的方块实体不是 action_relay（当前："
                        + (isBlank(blockEntityType) ? "未知" : blockEntityType) + "，预期：ActionRelayBlockEntity）。";
                default -> "";
            };
        }
    }

    private record Validation(List<WebAdminValidationError> errors, List<ActionConfig> actions) {
    }
}
