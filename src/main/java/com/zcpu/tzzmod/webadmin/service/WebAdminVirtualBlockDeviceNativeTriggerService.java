package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.device.BlockStateCondition;
import com.zcpu.tzzmod.signal.device.BlockStateConditionMode;
import com.zcpu.tzzmod.signal.device.BlockStateConditionParser;
import com.zcpu.tzzmod.signal.device.BlockStateConditionResult;
import com.zcpu.tzzmod.signal.device.ContainerDeviceSupport;
import com.zcpu.tzzmod.signal.device.ContainerItemConditionData;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceMode;
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceSupport;
import com.zcpu.tzzmod.signal.device.VirtualBlockPowerState;
import com.zcpu.tzzmod.signal.device.item.ItemStackMatcherData;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;

public final class WebAdminVirtualBlockDeviceNativeTriggerService {
    private static final int MAX_CHANNEL_LENGTH = 128;
    private static final int MAX_TICKS = 72_000;
    private static final String REDSTONE_DISABLED_MODE = "redstone_disabled";
    private static final List<String> TRIGGER_TYPES = List.of(
            "redstone_powered",
            "blockstate",
            "right_click",
            "container_open",
            "container_close",
            "container_change"
    );
    private static final List<String> ALLOWED_REDSTONE_MODES = List.of(
            VirtualBlockDeviceMode.REDSTONE_RISING.id(),
            VirtualBlockDeviceMode.REDSTONE_FALLING.id(),
            VirtualBlockDeviceMode.REDSTONE_BOTH.id()
    );
    private static final List<String> ALLOWED_CONDITION_MODES = List.of(
            BlockStateConditionMode.CONDITION_ENTER.id(),
            BlockStateConditionMode.CONDITION_EXIT.id(),
            BlockStateConditionMode.CONDITION_BOTH.id()
    );

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;

    public WebAdminVirtualBlockDeviceNativeTriggerService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
    }

    public Map<String, Object> overview(MinecraftServer server, String deviceRef) {
        return overview(server, null, null, deviceRef);
    }

    public Map<String, Object> overview(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String deviceRef
    ) {
        if (server == null || isBlank(deviceRef)) {
            return null;
        }
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
            unsupported.put("readOnly", true);
            unsupported.put("writeApiEnabled", false);
            unsupported.put("unsupportedReason", "原生触发配置只支持 virtual_block_device。");
            return unsupported;
        }

        NativeTriggerRuntime runtime = runtime(server, device);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceId", device.id());
        data.put("deviceType", WebAdminReadonlySupport.deviceType(device));
        data.put("displayName", WebAdminReadonlySupport.deviceDisplayName(device));
        data.put("supported", true);
        data.put("typeSupported", true);
        data.put("readOnly", false);
        data.put("writeApiEnabled", true);
        data.put("nativeTriggerWriteApiEnabled", true);
        data.put("redstoneDisabledMode", REDSTONE_DISABLED_MODE);
        data.put("availableTriggerTypes", TRIGGER_TYPES);
        data.put("allowedRedstoneModes", ALLOWED_REDSTONE_MODES);
        data.put("allowedConditionModes", ALLOWED_CONDITION_MODES);
        data.put("boundBlock", boundBlock(device, runtime));
        Map<String, Object> triggers = triggers(device, runtime);
        data.put("triggers", triggers);
        data.put("activeTriggerTypes", activeTriggerTypes(triggers));
        data.put("expectedFingerprint", fingerprintFor(device));
        data.put("lockStatus", editLockService == null ? null : editLockService.status(
                WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS,
                device.id(),
                user,
                session
        ));
        data.put("forbiddenInP2", List.of(
                "itemSubmit",
                "consume",
                "inventory",
                "equipment",
                "armor",
                "conditionEngine",
                "successFailPathGraph",
                "scratchLikeEditor",
                "containerItemTemplateGui",
                "rawJsonTextarea",
                "arbitraryNbtPath"
        ));
        data.put("notes", List.of(
                "7.9 P2 支持 VBD 六类原生触发源的普通 Web 表单编辑。",
                "interaction item matcher 是右键交互之后的条件层，只有右键交互启用时才显示入口；隐藏不会清空 matcher 数据。",
                "容器内容变化物品模板 GUI 规划在 7.9 P3。"
        ));
        return data;
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String deviceRef,
            WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String safeDeviceRef = safe(deviceRef);
        SignalDeviceData device = findDevice(server, safeDeviceRef);
        WebAdminWriteTarget target = target(device == null ? safeDeviceRef : device.id(), device == null ? safeDeviceRef : WebAdminReadonlySupport.deviceDisplayName(device));
        WebAdminWriteContext context = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS,
                target
        );
        if (device == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "目标 virtual_block_device 不存在或引用不唯一。");
            audit(context, result, Map.of(), Map.of());
            return result;
        }
        if (request != null) {
            request.deviceId = device.id();
        }
        target = target(device.id(), WebAdminReadonlySupport.deviceDisplayName(device));
        context = WebAdminWriteContext.of(user, session, remoteAddress, WebAdminOperationType.EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS, target);

        if (!SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE.equals(device.type())) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "device",
                    "invalid_type",
                    "只有 virtual_block_device 支持原生触发配置。",
                    device.type()
            )));
            audit(context, result, currentSummary(device), Map.of("attempt", "invalid_type"));
            return result;
        }
        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS);
        if (!permission.allowed()) {
            WebAdminWriteResult result = permission.asWriteResult(target);
            audit(context, result, currentSummary(device), Map.of("attempt", "permission_denied"));
            return result;
        }
        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
            audit(context, result, currentSummary(device), Map.of("attempt", "csrf_failed"));
            return result;
        }
        if (!sameOrigin) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.CSRF_INVALID, target, "写请求来源校验失败，请刷新页面后重试。");
            audit(context, result, currentSummary(device), Map.of("attempt", "origin_failed"));
            return result;
        }
        if (editLockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS,
                    device.id(),
                    request == null ? "" : request.lockId,
                    user,
                    session
            );
            if (!lockValidation.success()) {
                WebAdminWriteResult result = lockValidation.result();
                audit(context, result, currentSummary(device), Map.of("attempt", "edit_lock_failed"));
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
            audit(context, result, currentSummary(device), Map.of("attempt", "expected_fingerprint_missing"));
            return result;
        }
        if (!fingerprintMatches(device, request.expectedFingerprint)) {
            WebAdminWriteResult result = conflictDetected(target, device, request.expectedFingerprint);
            audit(context, result, currentSummary(device), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }

        NativeTriggerRuntime runtime = runtime(server, device);
        Validation validation = validateRequest(server, device, runtime, request);
        if (!validation.errors().isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, validation.errors());
            audit(context, result, currentSummary(device), requestSummary(request));
            return result;
        }
        SignalDeviceStore.WebAdminNativeTriggerPatch patch = validation.patch();
        SignalDeviceData preview = applyPreview(device, patch);
        List<String> changedFields = changedFields(device, preview);
        if (changedFields.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的 VBD 原生触发配置变化。");
            audit(context, result, currentSummary(device), currentSummary(device));
            releaseLockAfterWrite(request, user, session, remoteAddress);
            return result;
        }

        ServerWorld world = SignalDeviceStore.getDeviceWorld(server, device);
        if (world == null) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "world",
                    "world_unavailable",
                    "设备所在维度不可用，无法保存 VBD 原生触发配置。",
                    device.dimension()
            )));
            audit(context, result, currentSummary(device), requestSummary(request));
            return result;
        }
        SignalDeviceData updated = SignalDeviceStore.updateVirtualNativeTriggersForWebAdmin(
                world,
                new BlockPos(device.x(), device.y(), device.z()),
                patch
        );
        if (updated == null) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.TARGET_NOT_FOUND, target, "保存时目标 virtual_block_device 已不存在。");
            audit(context, result, currentSummary(device), requestSummary(request));
            return result;
        }
        SignalDeviceStore.forceFlushDirty(server);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("nativeTriggers", overview(server, user, session, updated.id()));
        resultData.put("changedFields", changedFields);
        resultData.put("changedTriggerTypes", changedTriggerTypes(changedFields));
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "VBD 原生触发配置已保存。",
                target.targetType(),
                target.targetId(),
                true,
                List.of(),
                "",
                "",
                false,
                Map.of(),
                resultData
        );
        WebAdminAuditEvent auditEvent = audit(context, result, currentSummary(device), currentSummary(updated));
        publishRealtime(device, updated, auditEvent, changedFields, user);
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
        String input = "virtual_block_device_triggers|" + device.id() + "|" + device.type() + "|" + editableSummary(device);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private Validation validateRequest(
            MinecraftServer server,
            SignalDeviceData device,
            NativeTriggerRuntime runtime,
            WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest request
    ) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new WebAdminValidationError("nativeTriggers", "required", "原生触发配置不能为空。", ""));
            return new Validation(List.copyOf(errors), null);
        }
        boolean redstoneEnabled = Boolean.TRUE.equals(request.redstoneEnabled);
        String redstoneMode = redstoneEnabled
                ? normalizeMode(request.redstoneMode, ALLOWED_REDSTONE_MODES, device.mode(), "redstoneMode", errors)
                : VirtualBlockDeviceMode.REDSTONE_DISABLED.id();
        boolean blockStateEnabled = Boolean.TRUE.equals(request.blockStateEnabled);
        String channel = validateChannel(errors, "channel", request.channel, redstoneEnabled || blockStateEnabled);
        String offChannel = validateChannel(errors, "offChannel", request.offChannel, false);

        String conditionMode = blockStateEnabled
                ? normalizeMode(request.conditionMode, ALLOWED_CONDITION_MODES, device.conditionMode(), "conditionMode", errors)
                : BlockStateConditionMode.normalize(device.conditionMode());
        String conditionBlockId = device.conditionBlockId();
        Map<String, String> conditionProperties = new LinkedHashMap<>(device.conditionProperties());
        String conditionRaw = device.conditionRaw();
        boolean currentMatched = device.lastConditionMatched();
        String conditionResult = blockStateEnabled ? device.lastConditionResult() : "WebAdmin 已关闭 BlockState 条件";
        if (blockStateEnabled) {
            if (runtime.state() == null || !runtime.chunkLoaded()) {
                errors.add(new WebAdminValidationError("blockstate", "block_unavailable", "当前绑定方块状态不可读取，无法保存 BlockState 条件。", runtime.status()));
            } else if (runtime.state().isAir()) {
                errors.add(new WebAdminValidationError("blockstate", "air", "当前位置是空气，无法保存 BlockState 条件。", runtime.status()));
            } else if (!safe(runtime.actualBlockId()).equals(device.blockId())) {
                errors.add(new WebAdminValidationError("blockstate", "block_mismatch", "当前方块 ID 与绑定时不一致，无法保存 BlockState 条件。", runtime.actualBlockId()));
            } else {
                Map<String, String> requestedProperties = conditionPropertiesFrom(request, errors);
                if (requestedProperties.isEmpty()) {
                    errors.add(new WebAdminValidationError("conditionProperties", "required", "启用 BlockState 条件时至少需要一条属性条件。", ""));
                } else {
                    BlockStateConditionResult result = BlockStateConditionParser.fromPropertiesAndValidate(requestedProperties, runtime.state());
                    if (!result.success()) {
                        errors.add(new WebAdminValidationError("conditionProperties", "invalid_property", result.error(), requestedProperties.toString()));
                    } else {
                        BlockStateCondition condition = result.condition();
                        conditionBlockId = condition.blockId();
                        conditionProperties = new LinkedHashMap<>(condition.properties());
                        conditionRaw = condition.raw();
                        currentMatched = BlockStateConditionParser.matches(runtime.state(), condition);
                        conditionResult = currentMatched ? "WebAdmin 保存时条件已匹配" : "WebAdmin 保存时条件未匹配";
                    }
                }
            }
        }

        boolean interactionEnabled = Boolean.TRUE.equals(request.interactionEnabled);
        String interactChannel = validateChannel(errors, "interactChannel", request.interactChannel, interactionEnabled);
        int interactionCooldownTicks = validateTicks(errors, "interactionCooldownTicks", request.interactionCooldownTicks, 0, MAX_TICKS, device.interactionCooldownTicks());

        boolean containerOpenEnabled = Boolean.TRUE.equals(request.containerOpenEnabled);
        boolean containerCloseEnabled = Boolean.TRUE.equals(request.containerCloseEnabled);
        boolean containerChangeEnabled = Boolean.TRUE.equals(request.containerChangeEnabled);
        String openChannel = validateChannel(errors, "containerOpenChannel", request.containerOpenChannel, containerOpenEnabled);
        String closeChannel = validateChannel(errors, "containerCloseChannel", request.containerCloseChannel, containerCloseEnabled);
        String changeChannel = validateChannel(errors, "containerChangeChannel", request.containerChangeChannel, containerChangeEnabled);
        int containerCooldownTicks = validateTicks(errors, "containerCooldownTicks", request.containerCooldownTicks, 0, MAX_TICKS, device.containerCooldownTicks());
        int checkIntervalTicks = validateTicks(errors, "containerChangeCheckIntervalTicks", request.containerChangeCheckIntervalTicks, 1, MAX_TICKS, device.containerChangeCheckIntervalTicks());
        String fingerprint = device.lastContainerFingerprint();
        boolean anyContainerEnabled = containerOpenEnabled || containerCloseEnabled || containerChangeEnabled;
        if (anyContainerEnabled) {
            ServerWorld world = SignalDeviceStore.getDeviceWorld(server, device);
            BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
            if (world == null) {
                errors.add(new WebAdminValidationError("container", "world_unavailable", "设备所在维度不可用，无法保存容器触发配置。", device.dimension()));
            } else if (!world.isChunkLoaded(pos)) {
                errors.add(new WebAdminValidationError("container", "chunk_unloaded", "设备所在区块未加载，无法保存容器触发配置。", device.id()));
            } else if (runtime.state() == null || runtime.state().isAir()) {
                errors.add(new WebAdminValidationError("container", "air", "当前位置是空气，无法保存容器触发配置。", runtime.status()));
            } else if (!safe(runtime.actualBlockId()).equals(device.blockId())) {
                errors.add(new WebAdminValidationError("container", "block_mismatch", "当前方块 ID 与绑定时不一致，无法保存容器触发配置。", runtime.actualBlockId()));
            } else if (!ContainerDeviceSupport.isContainer(world, pos)) {
                errors.add(new WebAdminValidationError("container", "not_container", "当前绑定方块不是可打开容器，无法启用容器打开 / 关闭 / 内容变化触发。", runtime.actualBlockId()));
            } else if (containerChangeEnabled && !ContainerDeviceSupport.hasInventory(world, pos)) {
                errors.add(new WebAdminValidationError("containerChange", "not_inventory", "当前容器不能读取 Inventory，无法启用容器内容变化触发。", runtime.actualBlockId()));
            } else if (containerChangeEnabled) {
                fingerprint = ContainerDeviceSupport.fingerprint(world, pos);
            }
        }

        if (!errors.isEmpty()) {
            return new Validation(List.copyOf(errors), null);
        }
        SignalDeviceStore.WebAdminNativeTriggerPatch patch = new SignalDeviceStore.WebAdminNativeTriggerPatch(
                redstoneEnabled,
                channel,
                offChannel,
                redstoneMode,
                blockStateEnabled,
                conditionBlockId,
                conditionProperties,
                conditionRaw,
                conditionMode,
                currentMatched,
                conditionResult,
                interactionEnabled,
                interactChannel,
                interactionCooldownTicks,
                containerOpenEnabled,
                openChannel,
                containerCloseEnabled,
                closeChannel,
                containerChangeEnabled,
                changeChannel,
                containerCooldownTicks,
                checkIntervalTicks,
                fingerprint
        );
        return new Validation(List.of(), patch);
    }

    private static Map<String, String> conditionPropertiesFrom(
            WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest request,
            List<WebAdminValidationError> errors
    ) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest.BlockStateConditionRow row : request.conditionProperties == null
                ? List.<WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest.BlockStateConditionRow>of()
                : request.conditionProperties) {
            String property = safe(row == null ? "" : row.property).trim();
            String value = safe(row == null ? "" : row.value).trim();
            if (property.isBlank() && value.isBlank()) {
                continue;
            }
            if (property.isBlank() || value.isBlank()) {
                errors.add(new WebAdminValidationError("conditionProperties", "blank_property_value", "BlockState 属性名和值都必须填写。", property + "=" + value));
                continue;
            }
            if (properties.containsKey(property)) {
                errors.add(new WebAdminValidationError("conditionProperties", "duplicate_property", "BlockState 条件不能重复设置属性：" + property, property));
                continue;
            }
            properties.put(property, value);
        }
        return properties;
    }

    private static String normalizeMode(
            String raw,
            List<String> allowed,
            String fallback,
            String field,
            List<WebAdminValidationError> errors
    ) {
        String value = safe(raw).trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return allowed.contains(fallback) ? fallback : allowed.get(0);
        }
        if (!allowed.contains(value)) {
            errors.add(new WebAdminValidationError(field, "invalid_mode", field + " 不支持值：" + value, value));
            return allowed.contains(fallback) ? fallback : allowed.get(0);
        }
        return value;
    }

    private static String validateChannel(List<WebAdminValidationError> errors, String field, String raw, boolean required) {
        String channel = SignalChannel.normalize(raw);
        if (required && channel.isBlank()) {
            errors.add(new WebAdminValidationError(field, "required", "启用该触发方式时必须填写频道。", ""));
            return channel;
        }
        if (channel.length() > MAX_CHANNEL_LENGTH) {
            errors.add(new WebAdminValidationError(field, "too_long", "频道不能超过 128 个字符。", channel));
        } else if (!channel.isBlank() && !SignalChannel.isValid(channel)) {
            errors.add(new WebAdminValidationError(field, "invalid_channel", "频道名称无效。允许小写字母、数字、_、-、.、:。", channel));
        }
        return channel;
    }

    private static int validateTicks(List<WebAdminValidationError> errors, String field, Object raw, int min, int max, int fallback) {
        Integer parsed = parseInteger(raw);
        if (parsed == null) {
            errors.add(new WebAdminValidationError(field, "invalid_integer", field + " 必须是整数。", String.valueOf(raw)));
            return Math.max(min, fallback);
        }
        if (parsed < min || parsed > max) {
            errors.add(new WebAdminValidationError(field, "out_of_range", field + " 必须在 " + min + "～" + max + " 之间。", String.valueOf(parsed)));
        }
        return Math.max(min, Math.min(max, parsed));
    }

    private static Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (Double.isFinite(doubleValue) && Math.floor(doubleValue) == doubleValue) {
                return (int) doubleValue;
            }
            return null;
        }
        String text = String.valueOf(value == null ? "" : value).trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static SignalDeviceData applyPreview(SignalDeviceData device, SignalDeviceStore.WebAdminNativeTriggerPatch patch) {
        boolean nextContainerEnabled = patch.containerOpenEnabled() || patch.containerCloseEnabled() || patch.containerChangeEnabled();
        String nextOpen = patch.containerOpenEnabled()
                ? SignalChannel.normalize(patch.containerOpenChannel())
                : (nextContainerEnabled ? "" : SignalChannel.normalize(patch.containerOpenChannel()));
        String nextClose = patch.containerCloseEnabled()
                ? SignalChannel.normalize(patch.containerCloseChannel())
                : (nextContainerEnabled ? "" : SignalChannel.normalize(patch.containerCloseChannel()));
        String nextChange = patch.containerChangeEnabled()
                ? SignalChannel.normalize(patch.containerChangeChannel())
                : (nextContainerEnabled ? "" : SignalChannel.normalize(patch.containerChangeChannel()));
        return new SignalDeviceData(
                device.id(),
                device.type(),
                device.name(),
                device.dimension(),
                device.x(),
                device.y(),
                device.z(),
                SignalChannel.normalize(patch.channel()),
                device.enabled(),
                device.pulseTicks(),
                device.remainingPulseTicks(),
                device.cooldownTicks(),
                device.actionCount(),
                device.createdWallTimeMillis(),
                device.updatedWallTimeMillis(),
                device.lastTriggerGameTime(),
                device.lastTriggerWallTimeMillis(),
                device.lastResult(),
                device.blockId(),
                SignalChannel.normalize(patch.offChannel()),
                patch.mode(),
                device.lastPowered(),
                device.lastPowerLevel(),
                patch.conditionEnabled(),
                patch.conditionBlockId(),
                patch.conditionProperties(),
                patch.conditionRaw(),
                patch.conditionMode(),
                patch.currentConditionMatched(),
                device.lastConditionCheckGameTime(),
                patch.conditionResult(),
                patch.interactionEnabled(),
                patch.interactChannel(),
                patch.interactionCooldownTicks(),
                device.lastInteractionGameTime(),
                device.lastInteractionWallTimeMillis(),
                device.lastInteractionPlayerName(),
                device.lastInteractionPlayerUuid(),
                device.lastInteractionResult(),
                device.lastInteractionHand(),
                device.lastInteractionSide(),
                nextContainerEnabled,
                nextOpen,
                nextClose,
                nextChange,
                patch.containerCooldownTicks(),
                patch.containerChangeCheckIntervalTicks(),
                device.lastContainerCheckGameTime(),
                patch.containerFingerprint(),
                device.lastContainerOpenGameTime(),
                device.lastContainerOpenWallTimeMillis(),
                device.lastContainerCloseGameTime(),
                device.lastContainerCloseWallTimeMillis(),
                device.lastContainerChangeGameTime(),
                device.lastContainerChangeWallTimeMillis(),
                device.lastContainerPlayerName(),
                device.lastContainerPlayerUuid(),
                device.lastContainerResult(),
                device.lastContainerEventType(),
                device.itemConditions(),
                device.interactionItemMatcherEnabled(),
                device.interactionItemMatcher(),
                device.lastInteractionItemMatched(),
                device.lastInteractionItemResult(),
                device.itemSubmitEnabled(),
                device.itemSubmitConsumeEnabled(),
                device.itemSubmitConsumeOrder(),
                device.itemSubmitRequirements(),
                device.lastItemSubmitMatched(),
                device.lastItemSubmitFailureReason(),
                device.lastItemSubmitConsumedSummary(),
                device.lastItemSubmitResult()
        ).normalized();
    }

    private static Map<String, Object> triggers(SignalDeviceData device, NativeTriggerRuntime runtime) {
        Map<String, Object> triggers = new LinkedHashMap<>();
        triggers.put("redstone_powered", redstone(device, runtime));
        triggers.put("blockstate", blockState(device, runtime));
        triggers.put("right_click", rightClick(device));
        triggers.put("container_open", containerOpen(device));
        triggers.put("container_close", containerClose(device));
        triggers.put("container_change", containerChange(device));
        return triggers;
    }

    private static Map<String, Object> redstone(SignalDeviceData device, NativeTriggerRuntime runtime) {
        boolean configured = !isBlank(device.channel()) || !isBlank(device.offChannel());
        boolean enabled = configured
                && !VirtualBlockDeviceMode.REDSTONE_DISABLED.id().equals(VirtualBlockDeviceMode.normalize(device.mode()));
        boolean runtimeEnabled = device.enabled() && enabled;
        Map<String, Object> data = baseTrigger("redstone_powered", "红石 / 受电状态", enabled, configured);
        data.put("deviceEnabled", device.enabled());
        data.put("runtimeEnabled", runtimeEnabled);
        data.put("mode", device.mode());
        data.put("modeDisplayName", VirtualBlockDeviceMode.displayName(device.mode()));
        data.put("channel", device.channel());
        data.put("offChannel", device.offChannel());
        data.put("lastPowered", device.lastPowered());
        data.put("lastPowerLevel", device.lastPowerLevel());
        data.put("lastTriggerResult", device.lastResult());
        data.put("runtimeAvailable", runtime.powerState() != null);
        data.put("currentPoweredExpression", "currentPowered = blockStatePowered || receivedPowerLevel > 0");
        if (runtime.powerState() != null) {
            VirtualBlockPowerState power = runtime.powerState();
            data.put("currentPowered", power.currentPowered());
            data.put("currentPowerLevel", power.receivedPowerLevel());
            data.put("blockStatePowered", power.blockStatePowered());
            data.put("actualBlockId", power.blockId());
        }
        return data;
    }

    private static Map<String, Object> blockState(SignalDeviceData device, NativeTriggerRuntime runtime) {
        Map<String, Object> data = baseTrigger(
                "blockstate",
                "BlockState 条件",
                device.conditionEnabled(),
                device.conditionEnabled() || !isBlank(device.conditionBlockId()) || !device.conditionProperties().isEmpty() || !isBlank(device.conditionRaw())
        );
        data.put("conditionEnabled", device.conditionEnabled());
        data.put("conditionBlockId", device.conditionBlockId());
        data.put("conditionProperties", device.conditionProperties());
        data.put("conditionRaw", device.conditionRaw());
        data.put("conditionMode", device.conditionMode());
        data.put("conditionModeDisplayName", BlockStateConditionMode.displayName(device.conditionMode()));
        data.put("lastConditionMatched", device.lastConditionMatched());
        data.put("lastConditionCheckGameTime", device.lastConditionCheckGameTime());
        data.put("lastConditionResult", device.lastConditionResult());
        data.put("runtimeState", runtime.status());
        data.put("supportedPropertyCount", runtime.properties().size());
        data.put("supportedProperties", runtime.properties());
        boolean hasBoundBlockState = runtime.state() != null && !runtime.state().isAir();
        data.put("allowedValuesFromBoundBlock", hasBoundBlockState);
        data.put("propertiesFromBoundBlock", hasBoundBlockState);
        data.put("serverValidatesBoundBlockProperties", true);
        data.put("propertySourceStatus", runtime.status());
        data.put("currentMatched", hasBoundBlockState && device.conditionEnabled() ? BlockStateConditionParser.matches(runtime.state(), device) : null);
        data.put("validationIssues", runtime.state() == null ? List.of("当前绑定方块状态不可用，无法校验已保存的 BlockState 条件。") : BlockStateConditionParser.validateSavedCondition(device, runtime.state()));
        return data;
    }

    private static Map<String, Object> rightClick(SignalDeviceData device) {
        boolean configured = device.interactionEnabled() || !isBlank(device.interactChannel()) || device.interactionCooldownTicks() > 0;
        Map<String, Object> data = baseTrigger("right_click", "玩家右键交互", device.interactionEnabled(), configured);
        data.put("interactionEnabled", device.interactionEnabled());
        data.put("interactChannel", device.interactChannel());
        data.put("interactionCooldownTicks", device.interactionCooldownTicks());
        data.put("lastInteractionGameTime", device.lastInteractionGameTime());
        data.put("lastInteractionWallTimeMillis", device.lastInteractionWallTimeMillis());
        data.put("lastInteractionPlayerName", device.lastInteractionPlayerName());
        data.put("lastInteractionPlayerUuid", device.lastInteractionPlayerUuid());
        data.put("lastInteractionResult", device.lastInteractionResult());
        data.put("lastInteractionHand", device.lastInteractionHand());
        data.put("lastInteractionSide", device.lastInteractionSide());
        ItemStackMatcherData matcher = device.interactionItemMatcher().normalized();
        data.put("interactionItemMatcherLayer", Map.of(
                "enabled", device.interactionItemMatcherEnabled(),
                "configured", matcher.enabled(),
                "templateItemId", matcher.templateItemId(),
                "countMode", matcher.countMode(),
                "requiredCount", matcher.requiredCount(),
                "source", matcher.interactionItemSource(),
                "matchCustomName", matcher.matchCustomName(),
                "matchLore", matcher.matchLore(),
                "summary", matcher.templateSummary()
        ));
        data.put("conditionLayerNote", "interaction item matcher 是右键交互之后的条件/判定层，不是新的原生触发源。");
        return data;
    }

    private static Map<String, Object> containerOpen(SignalDeviceData device) {
        boolean configured = !isBlank(device.containerOpenChannel());
        Map<String, Object> data = baseTrigger("container_open", "容器打开", device.containerEnabled() && configured, configured);
        data.put("containerEnabled", device.containerEnabled());
        data.put("containerOpenChannel", device.containerOpenChannel());
        data.put("containerCooldownTicks", device.containerCooldownTicks());
        data.put("lastContainerOpenGameTime", device.lastContainerOpenGameTime());
        data.put("lastContainerOpenWallTimeMillis", device.lastContainerOpenWallTimeMillis());
        data.put("lastContainerPlayerName", device.lastContainerPlayerName());
        data.put("lastContainerPlayerUuid", device.lastContainerPlayerUuid());
        data.put("lastContainerEventType", device.lastContainerEventType());
        data.put("lastContainerResult", device.lastContainerResult());
        data.put("sharedSwitchNote", "容器打开、关闭和内容变化共用 containerEnabled。");
        return data;
    }

    private static Map<String, Object> containerClose(SignalDeviceData device) {
        boolean configured = !isBlank(device.containerCloseChannel());
        Map<String, Object> data = baseTrigger("container_close", "容器关闭", device.containerEnabled() && configured, configured);
        data.put("containerEnabled", device.containerEnabled());
        data.put("containerCloseChannel", device.containerCloseChannel());
        data.put("containerCooldownTicks", device.containerCooldownTicks());
        data.put("lastContainerCloseGameTime", device.lastContainerCloseGameTime());
        data.put("lastContainerCloseWallTimeMillis", device.lastContainerCloseWallTimeMillis());
        data.put("lastContainerPlayerName", device.lastContainerPlayerName());
        data.put("lastContainerPlayerUuid", device.lastContainerPlayerUuid());
        data.put("lastContainerEventType", device.lastContainerEventType());
        data.put("lastContainerResult", device.lastContainerResult());
        data.put("sharedSwitchNote", "容器打开、关闭和内容变化共用 containerEnabled。");
        return data;
    }

    private static Map<String, Object> containerChange(SignalDeviceData device) {
        boolean hasEnabledItemCondition = hasEnabledContainerItemCondition(device.itemConditions());
        boolean configured = !isBlank(device.containerChangeChannel()) || hasEnabledItemCondition;
        Map<String, Object> data = baseTrigger("container_change", "容器内容变化", device.containerEnabled() && configured, configured);
        data.put("containerEnabled", device.containerEnabled());
        data.put("containerChangeChannel", device.containerChangeChannel());
        data.put("containerCooldownTicks", device.containerCooldownTicks());
        data.put("containerChangeCheckIntervalTicks", device.containerChangeCheckIntervalTicks());
        data.put("lastContainerCheckGameTime", device.lastContainerCheckGameTime());
        data.put("lastContainerFingerprint", device.lastContainerFingerprint());
        data.put("lastContainerChangeGameTime", device.lastContainerChangeGameTime());
        data.put("lastContainerChangeWallTimeMillis", device.lastContainerChangeWallTimeMillis());
        data.put("lastContainerPlayerName", device.lastContainerPlayerName());
        data.put("lastContainerPlayerUuid", device.lastContainerPlayerUuid());
        data.put("lastContainerEventType", device.lastContainerEventType());
        data.put("lastContainerResult", device.lastContainerResult());
        data.put("itemConditionCount", device.itemConditions().size());
        data.put("itemConditions", containerItemConditionSummaries(device.itemConditions()));
        data.put("itemConditionsReadOnly", true);
        data.put("templateEditorPhase", "7.9 P3");
        data.put("sharedSwitchNote", "容器打开、关闭和内容变化共用 containerEnabled；物品模板 GUI 不在 P2 普通 Web 表单中实现。");
        return data;
    }

    private static Map<String, Object> baseTrigger(String type, String label, boolean enabled, boolean configured) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("label", label);
        data.put("enabled", enabled);
        data.put("configured", configured);
        data.put("readOnly", false);
        return data;
    }

    private static List<String> activeTriggerTypes(Map<String, Object> triggers) {
        List<String> active = new ArrayList<>();
        for (String type : TRIGGER_TYPES) {
            Object value = triggers.get(type);
            if (value instanceof Map<?, ?> trigger && Boolean.TRUE.equals(trigger.get("enabled"))) {
                active.add(type);
            }
        }
        return List.copyOf(active);
    }

    private static List<Map<String, Object>> containerItemConditionSummaries(List<ContainerItemConditionData> rawConditions) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (ContainerItemConditionData raw : rawConditions == null ? List.<ContainerItemConditionData>of() : rawConditions) {
            ContainerItemConditionData condition = raw == null ? null : raw.normalized();
            if (condition == null) {
                continue;
            }
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", condition.id());
            summary.put("name", condition.name());
            summary.put("enabled", condition.enabled());
            summary.put("type", condition.type());
            summary.put("slot", condition.slot());
            summary.put("itemId", condition.itemId());
            summary.put("countMode", condition.countMode());
            summary.put("count", condition.count());
            summary.put("channel", condition.channel());
            summary.put("offChannel", condition.offChannel());
            summary.put("mode", condition.mode());
            summary.put("lastMatched", condition.lastMatched());
            summary.put("lastResult", condition.lastResult());
            summaries.add(summary);
        }
        return List.copyOf(summaries);
    }

    private static boolean hasEnabledContainerItemCondition(List<ContainerItemConditionData> conditions) {
        for (ContainerItemConditionData raw : conditions == null ? List.<ContainerItemConditionData>of() : conditions) {
            ContainerItemConditionData condition = raw == null ? null : raw.normalized();
            if (condition != null && condition.enabled()) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> boundBlock(SignalDeviceData device, NativeTriggerRuntime runtime) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dimension", device.dimension());
        data.put("pos", Map.of("x", device.x(), "y", device.y(), "z", device.z()));
        data.put("expectedBlockId", device.blockId());
        data.put("actualBlockId", runtime.actualBlockId());
        data.put("status", runtime.status());
        data.put("worldAvailable", runtime.worldAvailable());
        data.put("chunkLoaded", runtime.chunkLoaded());
        data.put("air", runtime.state() != null && runtime.state().isAir());
        data.put("blockMatches", !isBlank(runtime.actualBlockId()) && runtime.actualBlockId().equals(device.blockId()));
        data.put("supportedPropertyCount", runtime.properties().size());
        return data;
    }

    private static NativeTriggerRuntime runtime(MinecraftServer server, SignalDeviceData device) {
        ServerWorld world = SignalDeviceStore.getDeviceWorld(server, device);
        if (world == null) {
            return new NativeTriggerRuntime(false, false, "world_unavailable", "", null, null, List.of());
        }
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        if (!world.isChunkLoaded(pos)) {
            return new NativeTriggerRuntime(true, false, "chunk_unloaded", "", null, null, List.of());
        }
        BlockState state = world.getBlockState(pos);
        VirtualBlockPowerState power = VirtualBlockDeviceSupport.powerState(world, pos);
        String blockId = VirtualBlockDeviceSupport.blockId(state);
        String status = state.isAir() ? "air" : (!isBlank(device.blockId()) && !blockId.equals(device.blockId()) ? "block_mismatch" : "ready");
        return new NativeTriggerRuntime(true, true, status, blockId, state, power, propertyDtos(state, device));
    }

    private static List<Map<String, Object>> propertyDtos(BlockState state, SignalDeviceData device) {
        if (state == null || state.isAir()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Property<?> property : state.getProperties()) {
            String currentValue = valueName(state, property);
            String targetValue = device.conditionProperties().get(property.getName());
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("name", property.getName());
            dto.put("kind", propertyKind(property));
            dto.put("currentValue", currentValue);
            dto.put("allowedValues", valueNames(property));
            dto.put("targetValue", targetValue == null ? "" : targetValue);
            dto.put("targetMatched", targetValue != null && targetValue.equals(currentValue));
            dto.put("selectedInCondition", targetValue != null);
            result.add(dto);
        }
        return List.copyOf(result);
    }

    private static String propertyKind(Property<?> property) {
        if (property instanceof BooleanProperty) {
            return "boolean";
        }
        if (property instanceof IntProperty) {
            return "integer";
        }
        if (property instanceof EnumProperty<?>) {
            return "enum";
        }
        return "value";
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<String> valueNames(Property<?> property) {
        if (property == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Property rawProperty = property;
        Collection values = rawProperty.getValues();
        for (Object value : values) {
            result.add(rawProperty.name((Comparable) value));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String valueName(BlockState state, Property<?> property) {
        if (state == null || property == null) {
            return "";
        }
        Property rawProperty = property;
        Comparable value = state.get(rawProperty);
        return rawProperty.name(value);
    }

    private static SignalDeviceData findDevice(MinecraftServer server, String deviceId) {
        if (server == null || isBlank(deviceId)) {
            return null;
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(server, deviceId);
        return resolved.foundUnique() ? resolved.device().normalized() : null;
    }

    private static Map<String, Object> editableSummary(SignalDeviceData device) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (device == null) {
            return summary;
        }
        summary.put("channel", device.channel());
        summary.put("offChannel", device.offChannel());
        summary.put("mode", device.mode());
        summary.put("conditionEnabled", device.conditionEnabled());
        summary.put("conditionBlockId", device.conditionBlockId());
        summary.put("conditionProperties", new TreeMap<>(device.conditionProperties()));
        summary.put("conditionRaw", device.conditionRaw());
        summary.put("conditionMode", device.conditionMode());
        summary.put("interactionEnabled", device.interactionEnabled());
        summary.put("interactChannel", device.interactChannel());
        summary.put("interactionCooldownTicks", device.interactionCooldownTicks());
        summary.put("containerEnabled", device.containerEnabled());
        summary.put("containerOpenChannel", device.containerOpenChannel());
        summary.put("containerCloseChannel", device.containerCloseChannel());
        summary.put("containerChangeChannel", device.containerChangeChannel());
        summary.put("containerCooldownTicks", device.containerCooldownTicks());
        summary.put("containerChangeCheckIntervalTicks", device.containerChangeCheckIntervalTicks());
        return summary;
    }

    private static Map<String, Object> currentSummary(SignalDeviceData device) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (device == null) {
            return summary;
        }
        summary.put("deviceId", device.id());
        summary.put("deviceType", WebAdminReadonlySupport.deviceType(device));
        summary.put("displayName", WebAdminReadonlySupport.deviceDisplayName(device));
        summary.put("nativeTriggers", editableSummary(device));
        summary.put("containerItemConditionsReadonlyCount", device.itemConditions().size());
        summary.put("interactionItemMatcherPreserved", device.interactionItemMatcher().normalized().enabled());
        summary.put("itemSubmitPreserved", device.itemSubmitEnabled());
        summary.put("expectedFingerprint", fingerprintFor(device));
        return summary;
    }

    private static Map<String, Object> requestSummary(WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest request) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (request == null) {
            return summary;
        }
        summary.put("redstoneEnabled", Boolean.TRUE.equals(request.redstoneEnabled));
        summary.put("blockStateEnabled", Boolean.TRUE.equals(request.blockStateEnabled));
        summary.put("interactionEnabled", Boolean.TRUE.equals(request.interactionEnabled));
        summary.put("containerOpenEnabled", Boolean.TRUE.equals(request.containerOpenEnabled));
        summary.put("containerCloseEnabled", Boolean.TRUE.equals(request.containerCloseEnabled));
        summary.put("containerChangeEnabled", Boolean.TRUE.equals(request.containerChangeEnabled));
        summary.put("expectedFingerprint", request.expectedFingerprint);
        return summary;
    }

    private static List<String> changedFields(SignalDeviceData before, SignalDeviceData after) {
        Map<String, Object> beforeValues = editableSummary(before);
        Map<String, Object> afterValues = editableSummary(after);
        List<String> changed = new ArrayList<>();
        for (String key : beforeValues.keySet()) {
            Object beforeValue = beforeValues.get(key);
            Object afterValue = afterValues.get(key);
            if (beforeValue == null ? afterValue != null : !beforeValue.equals(afterValue)) {
                changed.add(key);
            }
        }
        return List.copyOf(changed);
    }

    private static List<String> changedTriggerTypes(List<String> changedFields) {
        Set<String> types = new LinkedHashSet<>();
        for (String field : changedFields == null ? List.<String>of() : changedFields) {
            if (List.of("enabled", "channel", "offChannel", "mode").contains(field)) {
                types.add("redstone_powered");
            } else if (field.startsWith("condition")) {
                types.add("blockstate");
            } else if (field.startsWith("interaction") || field.equals("interactChannel")) {
                types.add("right_click");
            } else if (field.equals("containerOpenChannel")) {
                types.add("container_open");
            } else if (field.equals("containerCloseChannel")) {
                types.add("container_close");
            } else if (field.startsWith("container")) {
                types.add("container_change");
            }
        }
        return List.copyOf(types);
    }

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> beforeSummary, Map<String, ?> afterSummary) {
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
            SignalDeviceData before,
            SignalDeviceData after,
            WebAdminAuditEvent auditEvent,
            List<String> changedFields,
            WebAdminUser user
    ) {
        String deviceId = after.id();
        String routeTarget = "#/devices/" + encode(deviceId);
        List<String> affectedChannels = affectedSignalChannels(before, after);
        List<String> changedTriggerTypes = changedTriggerTypes(changedFields);
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .deviceId(deviceId)
                .channel(after.channel())
                .sourceType(after.type())
                .severity("INFO")
                .summary("VBD 原生触发配置已更新。")
                .routeTarget(routeTarget)
                .payload("targetType", "virtual_block_device_triggers")
                .payload("deviceType", after.type())
                .payload("changedFields", changedFields)
                .payload("changedTriggerTypes", changedTriggerTypes)
                .payload("affectedChannels", affectedChannels)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEvent deviceEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.DEVICE_CONFIG_CHANGED)
                .deviceId(deviceId)
                .channel(after.channel())
                .sourceType(after.type())
                .severity("INFO")
                .summary("VBD 原生触发配置已更新：" + WebAdminReadonlySupport.deviceDisplayName(after))
                .routeTarget(routeTarget)
                .payload("targetType", "virtual_block_device_triggers")
                .payload("deviceType", after.type())
                .payload("changedFields", changedFields)
                .payload("changedTriggerTypes", changedTriggerTypes)
                .payload("affectedChannels", affectedChannels)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEvent vbdEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.VIRTUAL_BLOCK_DEVICE_CHANGED)
                .deviceId(deviceId)
                .channel(after.channel())
                .sourceType(after.type())
                .severity("INFO")
                .summary("虚拟方块设备原生触发配置已变化。")
                .routeTarget(routeTarget)
                .payload("targetType", "virtual_block_device_triggers")
                .payload("deviceType", after.type())
                .payload("changedFields", changedFields)
                .payload("changedTriggerTypes", changedTriggerTypes)
                .payload("affectedChannels", affectedChannels));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .deviceId(deviceId)
                .channel(after.channel())
                .sourceType(after.type())
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget)
                .payload("targetType", "virtual_block_device_triggers")
                .payload("deviceType", after.type())
                .payload("affectedChannels", affectedChannels)
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("deviceEventId", deviceEvent == null ? "" : deviceEvent.id())
                .payload("vbdEventId", vbdEvent == null ? "" : vbdEvent.id()));
    }

    private static List<String> affectedSignalChannels(SignalDeviceData before, SignalDeviceData after) {
        Set<String> channels = new LinkedHashSet<>();
        collectChannels(channels, before);
        collectChannels(channels, after);
        channels.removeIf(WebAdminVirtualBlockDeviceNativeTriggerService::isBlank);
        return List.copyOf(channels);
    }

    private static void collectChannels(Set<String> channels, SignalDeviceData device) {
        if (channels == null || device == null) {
            return;
        }
        channels.add(SignalChannel.normalize(device.channel()));
        channels.add(SignalChannel.normalize(device.offChannel()));
        channels.add(SignalChannel.normalize(device.interactChannel()));
        channels.add(SignalChannel.normalize(device.containerOpenChannel()));
        channels.add(SignalChannel.normalize(device.containerCloseChannel()));
        channels.add(SignalChannel.normalize(device.containerChangeChannel()));
    }

    private void releaseLockAfterWrite(
            WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest request,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress
    ) {
        if (editLockService == null || request == null || isBlank(request.lockId)) {
            return;
        }
        editLockService.releaseAfterWrite(
                WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS,
                request.deviceId,
                request.lockId,
                user,
                session,
                remoteAddress
        );
    }

    private static WebAdminWriteResult conflictDetected(WebAdminWriteTarget target, SignalDeviceData current, String expectedFingerprint) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", expectedFingerprint);
        conflict.put("currentFingerprint", fingerprintFor(current));
        conflict.put("currentNativeTriggers", currentSummary(current));
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "VBD 原生触发配置已被其他操作修改，请刷新后再编辑。",
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
        return new WebAdminWriteTarget("VIRTUAL_BLOCK_DEVICE_TRIGGERS", safe(deviceId), safe(displayName));
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

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record Validation(
            List<WebAdminValidationError> errors,
            SignalDeviceStore.WebAdminNativeTriggerPatch patch
    ) {
    }

    private record NativeTriggerRuntime(
            boolean worldAvailable,
            boolean chunkLoaded,
            String status,
            String actualBlockId,
            BlockState state,
            VirtualBlockPowerState powerState,
            List<Map<String, Object>> properties
    ) {
    }
}
