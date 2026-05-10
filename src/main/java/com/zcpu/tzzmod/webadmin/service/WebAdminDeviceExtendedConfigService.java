package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.ModBlock.ModBlocks;
import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.ModBlock.entity.SignalReceiverBlockEntity;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceExtendedConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
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
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class WebAdminDeviceExtendedConfigService {
    public static final int MAX_CHANNEL_LENGTH = 128;
    public static final String FIELD_INTERACT_CHANNEL = "interactChannel";
    public static final String FIELD_SUCCESS_CHANNEL = "successChannel";
    public static final String FIELD_FAIL_CHANNEL = "failChannel";
    public static final String FIELD_INTERACTION_COOLDOWN_TICKS = "interactionCooldownTicks";
    public static final String FIELD_PULSE_TICKS = "pulseTicks";
    public static final String FIELD_COOLDOWN_TICKS = "cooldownTicks";

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;

    public WebAdminDeviceExtendedConfigService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
    }

    public WebAdminDtos.DeviceExtendedConfigDto configFor(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String deviceId
    ) {
        SignalDeviceData device = findDevice(server, deviceId);
        return device == null ? null : configFor(server, user, session, device);
    }

    public WebAdminDtos.DeviceExtendedConfigDto configFor(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            SignalDeviceData rawDevice
    ) {
        SignalDeviceData device = rawDevice == null ? null : rawDevice.normalized();
        if (device == null) {
            return null;
        }
        Support support = support(server, device);
        return new WebAdminDtos.DeviceExtendedConfigDto(
                device.id(),
                WebAdminReadonlySupport.deviceType(device),
                values(device, support.fields()),
                support.fields(),
                support.editableFields(),
                fieldLabels(support.fields()),
                clearableFields(support.fields()),
                support.fieldDisabledReasons(),
                support.supported(),
                support.reason(),
                support.runtimeState(),
                support.worldAvailable(),
                support.chunkLoaded(),
                support.blockEntityLoaded(),
                support.blockEntityType(),
                support.blockId(),
                fingerprintFor(device),
                editLockService == null ? null : editLockService.status(
                        WebAdminEditLockService.TARGET_DEVICE_EXTENDED_CONFIG,
                        device.id(),
                        user,
                        session
                )
        );
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminDeviceExtendedConfigUpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String deviceId = request == null ? "" : safe(request.deviceId);
        SignalDeviceData device = findDevice(server, deviceId);
        String canonicalDeviceId = device == null ? deviceId : device.id();
        WebAdminWriteTarget target = new WebAdminWriteTarget(
                "DEVICE_EXTENDED_CONFIG",
                canonicalDeviceId,
                device == null ? canonicalDeviceId : WebAdminReadonlySupport.deviceDisplayName(device)
        );
        WebAdminWriteContext writeContext = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.EDIT_DEVICE_EXTENDED_CONFIG,
                target
        );

        if (device == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.TARGET_NOT_FOUND,
                    target,
                    "目标设备不存在或已被删除。"
            );
            audit(writeContext, result, Map.of(), Map.of());
            return result;
        }
        if (request != null) {
            request.deviceId = device.id();
        }

        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_DEVICE_EXTENDED_CONFIG);
        if (!permission.allowed()) {
            WebAdminWriteResult result = permission.asWriteResult(target);
            audit(writeContext, result, Map.of(), Map.of("attempt", "permission_denied"));
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

        Support support = support(server, device);
        if (!support.supported()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "unsupported",
                    support.reason(),
                    device.type()
            )));
            audit(writeContext, result, currentSummary(device), Map.of("attempt", "unsupported"));
            return result;
        }
        if (support.editableFields().isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "unsupported_state",
                    support.reason().isBlank() ? "设备当前没有可安全写入的类型专属配置字段。" : support.reason(),
                    device.type()
            )));
            audit(writeContext, result, currentSummary(device), Map.of("attempt", "unsupported_state", "runtimeState", support.runtimeState()));
            return result;
        }

        if (editLockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_DEVICE_EXTENDED_CONFIG,
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

        List<WebAdminValidationError> errors = validateRequest(request, support.editableFields());
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(writeContext, result, currentSummary(device), requestSummary(request));
            return result;
        }

        SignalDeviceStore.ExtendedConfigPatch patch = patchFor(request, support.editableFields());
        SignalDeviceData targetDevice = SignalDeviceStore.withExtendedConfigForWebAdmin(device, patch);
        List<String> changedFields = changedFields(device, targetDevice, support.editableFields());
        if (changedFields.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的设备扩展配置变化。");
            audit(writeContext, result, currentSummary(device), currentSummary(device));
            releaseLockAfterWrite(request, user, session, remoteAddress);
            return result;
        }

        SignalDeviceData updated = SignalDeviceStore.updateExtendedConfig(server, device.id(), patch);
        if (updated == null) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "unsupported_state",
                    "设备当前不可安全编辑：方块设备可能未加载，WebAdmin 不会强制加载区块。",
                    device.type()
            )));
            audit(writeContext, result, currentSummary(device), requestSummary(request));
            return result;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("extendedConfig", configFor(server, user, session, updated));
        data.put("changedFields", changedFields);
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "设备扩展配置已保存，修改会立即应用到当前世界中的设备行为。",
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
        Support support = support(null, device);
        Map<String, Object> values = values(device, support.fields());
        String input = "device_extended_config|" + device.id() + "|" + device.type() + "|" + values;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public static List<WebAdminValidationError> validateRequest(
            WebAdminDeviceExtendedConfigUpdateRequest request,
            List<String> supportedFields
    ) {
        List<String> supported = supportedFields == null ? List.of() : supportedFields;
        List<WebAdminValidationError> errors = new ArrayList<>();
        validateUnsupported(errors, request, supported);
        validateChannel(errors, FIELD_INTERACT_CHANNEL, request == null ? null : request.interactChannel,
                request != null && Boolean.TRUE.equals(request.clearInteractChannel), supported);
        validateChannel(errors, FIELD_SUCCESS_CHANNEL, request == null ? null : request.successChannel,
                request != null && Boolean.TRUE.equals(request.clearSuccessChannel), supported);
        validateChannel(errors, FIELD_FAIL_CHANNEL, request == null ? null : request.failChannel,
                request != null && Boolean.TRUE.equals(request.clearFailChannel), supported);
        validateTicks(errors, FIELD_INTERACTION_COOLDOWN_TICKS, request == null ? null : request.interactionCooldownTicks,
                0, 72000, supported);
        validateTicks(errors, FIELD_PULSE_TICKS, request == null ? null : request.pulseTicks,
                SignalReceiverBlockEntity.MIN_PULSE_TICKS, SignalReceiverBlockEntity.MAX_PULSE_TICKS, supported);
        validateTicks(errors, FIELD_COOLDOWN_TICKS, request == null ? null : request.cooldownTicks,
                ActionRelayBlockEntity.MIN_COOLDOWN_TICKS, ActionRelayBlockEntity.MAX_COOLDOWN_TICKS, supported);
        return List.copyOf(errors);
    }

    private static void validateUnsupported(
            List<WebAdminValidationError> errors,
            WebAdminDeviceExtendedConfigUpdateRequest request,
            List<String> supported
    ) {
        if (request == null) {
            return;
        }
        if (!supported.contains(FIELD_INTERACT_CHANNEL) && (request.interactChannel != null || Boolean.TRUE.equals(request.clearInteractChannel))) {
            unsupported(errors, FIELD_INTERACT_CHANNEL, request.interactChannel);
        }
        if (!supported.contains(FIELD_SUCCESS_CHANNEL) && (request.successChannel != null || Boolean.TRUE.equals(request.clearSuccessChannel))) {
            unsupported(errors, FIELD_SUCCESS_CHANNEL, request.successChannel);
        }
        if (!supported.contains(FIELD_FAIL_CHANNEL) && (request.failChannel != null || Boolean.TRUE.equals(request.clearFailChannel))) {
            unsupported(errors, FIELD_FAIL_CHANNEL, request.failChannel);
        }
        if (!supported.contains(FIELD_INTERACTION_COOLDOWN_TICKS) && request.interactionCooldownTicks != null) {
            unsupported(errors, FIELD_INTERACTION_COOLDOWN_TICKS, String.valueOf(request.interactionCooldownTicks));
        }
        if (!supported.contains(FIELD_PULSE_TICKS) && request.pulseTicks != null) {
            unsupported(errors, FIELD_PULSE_TICKS, String.valueOf(request.pulseTicks));
        }
        if (!supported.contains(FIELD_COOLDOWN_TICKS) && request.cooldownTicks != null) {
            unsupported(errors, FIELD_COOLDOWN_TICKS, String.valueOf(request.cooldownTicks));
        }
    }

    private static void validateChannel(
            List<WebAdminValidationError> errors,
            String field,
            String value,
            boolean clear,
            List<String> supported
    ) {
        if (!supported.contains(field) || (value == null && !clear)) {
            return;
        }
        if (clear) {
            return;
        }
        String raw = safe(value);
        String channel = SignalChannel.normalize(raw);
        if (channel.isBlank()) {
            errors.add(new WebAdminValidationError(field, "empty_channel", "该扩展频道为空；如需清空，请使用“设为未设置”。", raw));
        } else if (channel.length() > MAX_CHANNEL_LENGTH) {
            errors.add(new WebAdminValidationError(field, "too_long", "频道长度不能超过 128 个字符。", raw));
        } else if (containsControl(channel)) {
            errors.add(new WebAdminValidationError(field, "control_character", "频道不能包含控制字符。", raw));
        } else if (!SignalChannel.isValid(channel)) {
            errors.add(new WebAdminValidationError(field, "invalid_channel", "频道只能包含小写字母、数字、下划线、点、冒号和连字符。", raw));
        }
    }

    private static void validateTicks(
            List<WebAdminValidationError> errors,
            String field,
            Object value,
            int min,
            int max,
            List<String> supported
    ) {
        if (!supported.contains(field) || value == null) {
            return;
        }
        Integer parsed = parseInteger(value);
        if (parsed == null) {
            errors.add(new WebAdminValidationError(field, "invalid_integer", "该字段必须是整数。", String.valueOf(value)));
            return;
        }
        if (parsed < min || parsed > max) {
            errors.add(new WebAdminValidationError(field, "out_of_range", "该字段必须在 " + min + "～" + max + " tick 范围内。", String.valueOf(value)));
        }
    }

    private static SignalDeviceStore.ExtendedConfigPatch patchFor(
            WebAdminDeviceExtendedConfigUpdateRequest request,
            List<String> supportedFields
    ) {
        List<String> supported = supportedFields == null ? List.of() : supportedFields;
        boolean updateInteract = supported.contains(FIELD_INTERACT_CHANNEL)
                && (request.interactChannel != null || Boolean.TRUE.equals(request.clearInteractChannel));
        boolean updateSuccess = supported.contains(FIELD_SUCCESS_CHANNEL)
                && (request.successChannel != null || Boolean.TRUE.equals(request.clearSuccessChannel));
        boolean updateFail = supported.contains(FIELD_FAIL_CHANNEL)
                && (request.failChannel != null || Boolean.TRUE.equals(request.clearFailChannel));
        return new SignalDeviceStore.ExtendedConfigPatch(
                request.interactChannel,
                updateInteract,
                Boolean.TRUE.equals(request.clearInteractChannel),
                request.successChannel,
                updateSuccess,
                Boolean.TRUE.equals(request.clearSuccessChannel),
                request.failChannel,
                updateFail,
                Boolean.TRUE.equals(request.clearFailChannel),
                supported.contains(FIELD_INTERACTION_COOLDOWN_TICKS) ? parseInteger(request.interactionCooldownTicks) : null,
                supported.contains(FIELD_PULSE_TICKS) ? parseInteger(request.pulseTicks) : null,
                supported.contains(FIELD_COOLDOWN_TICKS) ? parseInteger(request.cooldownTicks) : null
        );
    }

    private static SignalDeviceData findDevice(MinecraftServer server, String deviceId) {
        if (server == null || deviceId == null || deviceId.isBlank()) {
            return null;
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, deviceId);
        return resolved.foundUnique() ? resolved.device().normalized() : null;
    }

    private static Support support(MinecraftServer server, SignalDeviceData device) {
        if (device == null) {
            return Support.unsupported("设备不存在。");
        }
        return switch (device.type()) {
            case SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE -> Support.editable(List.of(
                    FIELD_INTERACT_CHANNEL,
                    FIELD_SUCCESS_CHANNEL,
                    FIELD_FAIL_CHANNEL,
                    FIELD_INTERACTION_COOLDOWN_TICKS
            ));
            case SignalDeviceData.TYPE_SIGNAL_RECEIVER -> physicalSupport(
                    server,
                    device,
                    List.of(FIELD_PULSE_TICKS),
                    SignalReceiverBlockEntity.class,
                    "signal_receiver"
            );
            case SignalDeviceData.TYPE_ACTION_RELAY -> physicalSupport(
                    server,
                    device,
                    List.of(FIELD_COOLDOWN_TICKS),
                    ActionRelayBlockEntity.class,
                    "action_relay"
            );
            case SignalDeviceData.TYPE_SIGNAL_EMITTER -> Support.unsupported("signal_emitter 当前没有类型专属运行参数；请使用显示信息和基础配置编辑 displayName、note、iconKey、enabled、channel。");
            default -> Support.unsupported("该设备类型暂无可编辑扩展配置。");
        };
    }

    private static Support physicalSupport(
            MinecraftServer server,
            SignalDeviceData device,
            List<String> fields,
            Class<? extends BlockEntity> expectedType,
            String label
    ) {
        if (server == null) {
            return Support.editable(fields);
        }
        ServerWorld world = SignalDeviceStore.getDeviceWorld(server, device);
        if (world == null) {
            return Support.runtimeUnavailable(
                    fields,
                    "world_unavailable",
                    false,
                    false,
                    false,
                    "",
                    "",
                    "设备维度不可用或世界未加载，类型专属运行参数暂时只能查看快照。"
            );
        }
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        boolean chunkLoaded = world.isChunkLoaded(pos);
        if (!chunkLoaded) {
            return Support.runtimeUnavailable(
                    fields,
                    "chunk_unloaded",
                    true,
                    false,
                    false,
                    "",
                    "",
                    "该 " + label + " 所在区块未加载。WebAdmin 不会强制加载区块；请让玩家靠近该方块后重试。"
            );
        }
        BlockState blockState = world.getBlockState(pos);
        String blockId = Registries.BLOCK.getId(blockState.getBlock()).toString();
        String expectedBlockId = expectedBlockId(label);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        String blockEntityType = blockEntity == null ? "" : blockEntity.getClass().getSimpleName();
        String runtimeState = classifyPhysicalRuntimeState(
                true,
                true,
                blockId,
                expectedBlockId,
                blockEntity != null,
                blockEntity != null && expectedType.isInstance(blockEntity)
        );
        if (!"ready".equals(runtimeState)) {
            return Support.runtimeUnavailable(
                    fields,
                    runtimeState,
                    true,
                    true,
                    blockEntity != null,
                    blockEntityType,
                    blockId,
                    runtimeReason(runtimeState, label, expectedBlockId, blockId, blockEntityType, expectedType.getSimpleName())
            );
        }
        return Support.editable(fields, blockId, blockEntityType);
    }

    public static String classifyPhysicalRuntimeState(
            boolean worldAvailable,
            boolean chunkLoaded,
            String actualBlockId,
            String expectedBlockId,
            boolean blockEntityLoaded,
            boolean blockEntityMatches
    ) {
        if (!worldAvailable) {
            return "world_unavailable";
        }
        if (!chunkLoaded) {
            return "chunk_unloaded";
        }
        String actual = safe(actualBlockId);
        String expected = safe(expectedBlockId);
        if (actual.isBlank() || "minecraft:air".equals(actual)) {
            return "block_missing";
        }
        if (!expected.isBlank() && !expected.equals(actual)) {
            return "physical_block_mismatch";
        }
        if (!blockEntityLoaded) {
            return "block_entity_missing";
        }
        if (!blockEntityMatches) {
            return "block_entity_type_mismatch";
        }
        return "ready";
    }

    private static String expectedBlockId(String label) {
        if ("signal_receiver".equals(label)) {
            return Registries.BLOCK.getId(ModBlocks.SIGNAL_RECEIVER).toString();
        }
        if ("action_relay".equals(label)) {
            return Registries.BLOCK.getId(ModBlocks.ACTION_RELAY).toString();
        }
        if ("signal_emitter".equals(label)) {
            return Registries.BLOCK.getId(ModBlocks.SIGNAL_EMITTER).toString();
        }
        return "";
    }

    private static String runtimeReason(
            String runtimeState,
            String label,
            String expectedBlockId,
            String actualBlockId,
            String blockEntityType,
            String expectedBlockEntityType
    ) {
        return switch (runtimeState) {
            case "block_missing" -> "区块已加载，但该位置是空气或没有可用方块。预期方块：" + expectedBlockId + "。";
            case "physical_block_mismatch" -> "区块已加载，但当前位置不是 " + label + " 方块。预期方块：" + expectedBlockId + "。";
            case "block_entity_missing" -> "当前方块是 " + (isBlank(actualBlockId) ? expectedBlockId : actualBlockId)
                    + "，但区块内缺少 " + expectedBlockEntityType
                    + "。这通常表示方块状态与 BlockEntity 数据不一致、旧存档/外部编辑留下了坏数据，或方块实体未随区块正常恢复；请重新放置该方块或先运行诊断清理。";
            case "block_entity_type_mismatch" -> "区块已加载，但当前位置的方块实体不是 " + label + "（当前："
                    + (isBlank(blockEntityType) ? "未知" : blockEntityType)
                    + "，预期：" + expectedBlockEntityType + "）。";
            default -> "设备当前运行状态不可写。";
        };
    }

    private static Map<String, Object> values(SignalDeviceData device, List<String> fields) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (device == null || fields == null) {
            return values;
        }
        for (String field : fields) {
            switch (field) {
                case FIELD_INTERACT_CHANNEL -> values.put(field, device.interactChannel());
                case FIELD_SUCCESS_CHANNEL -> values.put(field, device.interactionItemMatcher().successChannel());
                case FIELD_FAIL_CHANNEL -> values.put(field, device.interactionItemMatcher().failChannel());
                case FIELD_INTERACTION_COOLDOWN_TICKS -> values.put(field, device.interactionCooldownTicks());
                case FIELD_PULSE_TICKS -> values.put(field, device.pulseTicks());
                case FIELD_COOLDOWN_TICKS -> values.put(field, device.cooldownTicks());
                default -> {
                }
            }
        }
        return values;
    }

    private static Map<String, String> fieldLabels(List<String> fields) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (fields == null) {
            return labels;
        }
        for (String field : fields) {
            labels.put(field, switch (field) {
                case FIELD_INTERACT_CHANNEL -> "交互频道";
                case FIELD_SUCCESS_CHANNEL -> "成功频道";
                case FIELD_FAIL_CHANNEL -> "失败频道";
                case FIELD_INTERACTION_COOLDOWN_TICKS -> "交互冷却时间";
                case FIELD_PULSE_TICKS -> "脉冲时间";
                case FIELD_COOLDOWN_TICKS -> "动作冷却时间";
                default -> field;
            });
        }
        return labels;
    }

    private static Map<String, Boolean> clearableFields(List<String> fields) {
        Map<String, Boolean> clearable = new LinkedHashMap<>();
        if (fields == null) {
            return clearable;
        }
        for (String field : fields) {
            clearable.put(field, field.equals(FIELD_INTERACT_CHANNEL)
                    || field.equals(FIELD_SUCCESS_CHANNEL)
                    || field.equals(FIELD_FAIL_CHANNEL));
        }
        return clearable;
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
                .summary("设备扩展配置已更新。")
                .routeTarget(routeTarget)
                .payload("targetType", "device_extended_config")
                .payload("deviceType", device.type())
                .payload("changedFields", changedFields)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEvent deviceEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.DEVICE_CONFIG_CHANGED)
                .deviceId(deviceId)
                .channel(device.channel())
                .sourceType(device.type())
                .severity("INFO")
                .summary("设备扩展配置已更新：" + WebAdminReadonlySupport.deviceDisplayName(device))
                .routeTarget(routeTarget)
                .payload("targetType", "device_extended_config")
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
                .payload("targetType", "device_extended_config")
                .payload("deviceType", device.type())
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("deviceEventId", deviceEvent == null ? "" : deviceEvent.id()));
    }

    private void releaseLockAfterWrite(
            WebAdminDeviceExtendedConfigUpdateRequest request,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress
    ) {
        if (editLockService == null || request == null || request.lockId == null || request.lockId.isBlank()) {
            return;
        }
        editLockService.releaseAfterWrite(
                WebAdminEditLockService.TARGET_DEVICE_EXTENDED_CONFIG,
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
        conflict.put("currentExtendedConfig", currentSummary(current));
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "设备扩展配置已被其他操作修改，请刷新后再编辑。",
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

    private static List<String> changedFields(SignalDeviceData before, SignalDeviceData after, List<String> fields) {
        Map<String, Object> beforeValues = values(before, fields);
        Map<String, Object> afterValues = values(after, fields);
        List<String> changed = new ArrayList<>();
        for (String field : fields == null ? List.<String>of() : fields) {
            Object beforeValue = beforeValues.get(field);
            Object afterValue = afterValues.get(field);
            if (beforeValue == null ? afterValue != null : !beforeValue.equals(afterValue)) {
                changed.add(field);
            }
        }
        return List.copyOf(changed);
    }

    private static Map<String, Object> currentSummary(SignalDeviceData device) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (device == null) {
            return summary;
        }
        Support support = support(null, device);
        summary.put("deviceId", device.id());
        summary.put("deviceType", WebAdminReadonlySupport.deviceType(device));
        summary.put("values", values(device, support.fields()));
        summary.put("expectedFingerprint", fingerprintFor(device));
        return summary;
    }

    private static Map<String, Object> requestSummary(WebAdminDeviceExtendedConfigUpdateRequest request) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (request == null) {
            return summary;
        }
        summary.put(FIELD_INTERACT_CHANNEL, SignalChannel.normalize(request.interactChannel));
        summary.put("clearInteractChannel", Boolean.TRUE.equals(request.clearInteractChannel));
        summary.put(FIELD_SUCCESS_CHANNEL, SignalChannel.normalize(request.successChannel));
        summary.put("clearSuccessChannel", Boolean.TRUE.equals(request.clearSuccessChannel));
        summary.put(FIELD_FAIL_CHANNEL, SignalChannel.normalize(request.failChannel));
        summary.put("clearFailChannel", Boolean.TRUE.equals(request.clearFailChannel));
        summary.put(FIELD_INTERACTION_COOLDOWN_TICKS, request.interactionCooldownTicks);
        summary.put(FIELD_PULSE_TICKS, request.pulseTicks);
        summary.put(FIELD_COOLDOWN_TICKS, request.cooldownTicks);
        summary.put("expectedFingerprint", request.expectedFingerprint);
        return summary;
    }

    private static Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (Double.isFinite(doubleValue) && Math.floor(doubleValue) == doubleValue
                    && doubleValue >= Integer.MIN_VALUE && doubleValue <= Integer.MAX_VALUE) {
                return (int) doubleValue;
            }
            return null;
        }
        return null;
    }

    private static void unsupported(List<WebAdminValidationError> errors, String field, String value) {
        errors.add(new WebAdminValidationError(field, "unsupported_field", "该设备类型不支持编辑此字段。", value));
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
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

    private record Support(
            boolean supported,
            String reason,
            List<String> fields,
            List<String> editableFields,
            Map<String, String> fieldDisabledReasons,
            String runtimeState,
            boolean worldAvailable,
            boolean chunkLoaded,
            boolean blockEntityLoaded,
            String blockEntityType,
            String blockId
    ) {
        static Support unsupported(String reason) {
            return new Support(false, reason, List.of(), List.of(), Map.of(), "unsupported", false, false, false, "", "");
        }

        static Support editable(List<String> fields) {
            return editable(fields, "", "");
        }

        static Support editable(List<String> fields, String blockId, String blockEntityType) {
            return new Support(true, "", fields, fields, Map.of(), "ready", true, true, true, blockEntityType, blockId);
        }

        static Support runtimeUnavailable(
                List<String> fields,
                String runtimeState,
                boolean worldAvailable,
                boolean chunkLoaded,
                boolean blockEntityLoaded,
                String blockEntityType,
                String blockId,
                String reason
        ) {
            Map<String, String> disabled = new LinkedHashMap<>();
            for (String field : fields == null ? List.<String>of() : fields) {
                disabled.put(field, reason);
            }
            return new Support(true, reason, fields, List.of(), disabled, runtimeState, worldAvailable, chunkLoaded, blockEntityLoaded, blockEntityType, blockId);
        }

        private Support {
            fields = fields == null ? List.of() : List.copyOf(fields);
            editableFields = editableFields == null ? List.of() : List.copyOf(editableFields);
            fieldDisabledReasons = fieldDisabledReasons == null ? Map.of() : Map.copyOf(fieldDisabledReasons);
            reason = safe(reason);
            runtimeState = safe(runtimeState);
            blockEntityType = safe(blockEntityType);
            blockId = safe(blockId);
        }
    }
}
