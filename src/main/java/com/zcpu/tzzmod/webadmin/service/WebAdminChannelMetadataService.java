package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminChannelMetadataStore;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminChannelMetadataUpdateRequest;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class WebAdminChannelMetadataService {
    public static final int MAX_CHANNEL_LENGTH = 128;
    public static final int MAX_DISPLAY_NAME_LENGTH = 64;
    public static final int MAX_NOTE_LENGTH = 512;

    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final WebAdminEditLockService editLockService;

    public WebAdminChannelMetadataService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            WebAdminEditLockService editLockService
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.editLockService = editLockService;
    }

    public WebAdminDtos.ChannelMetadataDto metadataFor(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String rawChannel,
            String fallbackIconKey
    ) {
        String channel = SignalChannel.normalize(rawChannel);
        WebAdminChannelMetadataStore.MetadataFile file = WebAdminChannelMetadataStore.load(server);
        WebAdminChannelMetadataStore.MetadataEntry entry = WebAdminChannelMetadataStore.MetadataEntry.normalized(
                channel,
                file.channels.get(channel)
        );
        return dto(entry, fallbackIconKey, editLockService == null ? null : editLockService.status(
                WebAdminEditLockService.TARGET_CHANNEL_METADATA,
                channel,
                user,
                session
        ));
    }

    public WebAdminWriteResult update(
            MinecraftServer server,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminChannelMetadataUpdateRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String channel = SignalChannel.normalize(request == null ? "" : request.channel);
        WebAdminWriteTarget target = new WebAdminWriteTarget("CHANNEL_METADATA", channel, channel);
        WebAdminWriteContext context = WebAdminWriteContext.of(
                user,
                session,
                remoteAddress,
                WebAdminOperationType.EDIT_CHANNEL_METADATA,
                target
        );

        List<WebAdminValidationError> targetErrors = validateChannel(channel, request == null ? "" : request.channel);
        if (!targetErrors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, targetErrors);
            audit(context, result, Map.of(), Map.of("attempt", "invalid_channel"));
            return result;
        }

        WebAdminPermissionDecision permission = permissionService.decide(user, WebAdminOperationType.EDIT_CHANNEL_METADATA);
        if (!permission.allowed()) {
            WebAdminWriteResult result = permission.asWriteResult(target);
            audit(context, result, Map.of(), Map.of("attempt", "permission_denied"));
            return result;
        }
        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
            audit(context, result, Map.of(), Map.of("attempt", "csrf_failed"));
            return result;
        }
        if (!sameOrigin) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.CSRF_INVALID,
                    target,
                    "写请求来源校验失败，请刷新页面后重试。"
            );
            audit(context, result, Map.of(), Map.of("attempt", "origin_failed"));
            return result;
        }
        if (editLockService != null) {
            WebAdminEditLockService.LockValidation lockValidation = editLockService.validateLock(
                    WebAdminEditLockService.TARGET_CHANNEL_METADATA,
                    channel,
                    request == null ? "" : request.lockId,
                    user,
                    session
            );
            if (!lockValidation.success()) {
                WebAdminWriteResult result = lockValidation.result();
                audit(context, result, Map.of(), Map.of("attempt", "edit_lock_failed"));
                return result;
            }
        }

        WebAdminChannelMetadataStore.MetadataFile file = WebAdminChannelMetadataStore.load(server);
        WebAdminChannelMetadataStore.MetadataEntry before = WebAdminChannelMetadataStore.MetadataEntry.normalized(
                channel,
                file.channels.get(channel)
        );
        if (request == null || isBlank(request.expectedFingerprint)) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, List.of(new WebAdminValidationError(
                    "expectedFingerprint",
                    "required",
                    "保存需要 expectedFingerprint，用于防止覆盖其他用户修改。",
                    ""
            )));
            audit(context, result, beforeSummary(before), Map.of("attempt", "expected_fingerprint_missing"));
            return result;
        }
        if (!fingerprintFor(before).equals(request.expectedFingerprint)) {
            WebAdminWriteResult result = conflictDetected(target, before, request.expectedFingerprint);
            audit(context, result, beforeSummary(before), Map.of("attempt", "fingerprint_conflict"));
            return result;
        }
        Normalized normalized = normalize(request, channel);
        List<WebAdminValidationError> errors = validateMetadata(normalized);
        if (!errors.isEmpty()) {
            WebAdminWriteResult result = WebAdminWriteResult.validationFailed(target, errors);
            audit(context, result, beforeSummary(before), normalized.summary());
            return result;
        }
        if (metadataEquals(before, normalized)) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "没有检测到需要保存的频道显示信息变化。");
            audit(context, result, beforeSummary(before), beforeSummary(before));
            releaseLockAfterWrite(request, user, session, remoteAddress);
            return result;
        }

        WebAdminChannelMetadataStore.MetadataEntry after = new WebAdminChannelMetadataStore.MetadataEntry();
        after.channel = channel;
        after.displayName = normalized.displayName();
        after.note = normalized.note();
        after.iconKey = normalized.iconKey();
        after.updatedAt = Instant.now().toString();
        after.updatedBy = user == null ? "" : user.username;
        after.version = before.version + 1L;
        file.channels.put(channel, after);
        if (!WebAdminChannelMetadataStore.save(server, file)) {
            WebAdminWriteResult result = WebAdminWriteResult.failed(WebAdminWriteResultCode.INTERNAL_ERROR, target, "频道显示信息保存失败，请查看服务端日志。");
            audit(context, result, beforeSummary(before), normalized.summary());
            return result;
        }

        List<String> changedFields = changedFields(before, after);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("metadata", dto(after, "signal", null));
        data.put("changedFields", changedFields);
        WebAdminWriteResult result = new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                "频道显示信息已保存。",
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
        WebAdminAuditEvent auditEvent = audit(context, result, beforeSummary(before), beforeSummary(after));
        publishRealtime(after, auditEvent, changedFields, user);
        releaseLockAfterWrite(request, user, session, remoteAddress);
        return result;
    }

    public static String fingerprintFor(WebAdminChannelMetadataStore.MetadataEntry rawEntry) {
        WebAdminChannelMetadataStore.MetadataEntry entry = WebAdminChannelMetadataStore.MetadataEntry.normalized(
                rawEntry == null ? "" : rawEntry.channel,
                rawEntry
        );
        String input = "channel_metadata|"
                + entry.channel + "|"
                + entry.displayName + "|"
                + entry.note + "|"
                + entry.iconKey + "|"
                + entry.version;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public static WebAdminDtos.ChannelMetadataDto dto(
            WebAdminChannelMetadataStore.MetadataEntry rawEntry,
            String fallbackIconKey,
            com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto lockStatus
    ) {
        WebAdminChannelMetadataStore.MetadataEntry entry = WebAdminChannelMetadataStore.MetadataEntry.normalized(
                rawEntry == null ? "" : rawEntry.channel,
                rawEntry
        );
        String fallbackIcon = isBlank(fallbackIconKey) ? "signal" : fallbackIconKey;
        String effectiveIcon = "auto".equals(entry.iconKey) || entry.iconKey.isBlank() ? fallbackIcon : entry.iconKey;
        String effectiveName = entry.displayName.isBlank() ? entry.channel : entry.displayName;
        return new WebAdminDtos.ChannelMetadataDto(
                entry.channel,
                entry.displayName,
                entry.note,
                entry.iconKey,
                effectiveName,
                effectiveIcon,
                entry.updatedAt,
                entry.updatedBy,
                entry.version,
                fingerprintFor(entry),
                lockStatus
        );
    }

    public static List<WebAdminValidationError> validateChannel(String normalizedChannel, String rawChannel) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        if (isBlank(normalizedChannel)) {
            errors.add(new WebAdminValidationError("channel", "required", "频道不能为空。", rawChannel));
        } else if (normalizedChannel.length() > MAX_CHANNEL_LENGTH) {
            errors.add(new WebAdminValidationError("channel", "too_long", "频道长度不能超过 128 个字符。", rawChannel));
        } else if (containsControl(normalizedChannel)) {
            errors.add(new WebAdminValidationError("channel", "control_character", "频道不能包含控制字符。", rawChannel));
        } else if (!SignalChannel.isValid(normalizedChannel)) {
            errors.add(new WebAdminValidationError("channel", "invalid_channel", "频道只能包含小写字母、数字、下划线、点、冒号和连字符。", rawChannel));
        }
        return List.copyOf(errors);
    }

    public static List<WebAdminValidationError> validateRequest(WebAdminChannelMetadataUpdateRequest request) {
        if (request == null) {
            return List.of(new WebAdminValidationError("metadata", "required", "频道显示信息不能为空。", ""));
        }
        return validateMetadata(normalize(request, ""));
    }

    private static List<WebAdminValidationError> validateMetadata(Normalized normalized) {
        List<WebAdminValidationError> errors = new ArrayList<>();
        if (normalized.displayName().length() > MAX_DISPLAY_NAME_LENGTH) {
            errors.add(new WebAdminValidationError("displayName", "too_long", "显示名不能超过 64 个字符。", normalized.displayName()));
        }
        if (normalized.note().length() > MAX_NOTE_LENGTH) {
            errors.add(new WebAdminValidationError("note", "too_long", "备注不能超过 512 个字符。", normalized.note()));
        }
        if (containsControl(normalized.displayName())) {
            errors.add(new WebAdminValidationError("displayName", "control_character", "显示名不能包含控制字符。", normalized.displayName()));
        }
        if (containsControl(normalized.note())) {
            errors.add(new WebAdminValidationError("note", "control_character", "备注不能包含控制字符。", normalized.note()));
        }
        if (!WebAdminDeviceMetadataService.isAllowedIconKey(normalized.iconKey())) {
            errors.add(new WebAdminValidationError("iconKey", "invalid_icon", "图标必须来自 WebAdmin 预设列表。", normalized.iconKey()));
        }
        return List.copyOf(errors);
    }

    private WebAdminAuditEvent audit(WebAdminWriteContext context, WebAdminWriteResult result, Map<String, ?> before, Map<String, ?> after) {
        WebAdminAuditEvent event = WebAdminAuditWriter.eventForResult(WebAdminWriteAuditContext.from(context), result, before, after);
        WebAdminAuditLogger.writeEvent(event);
        return event;
    }

    private void publishRealtime(
            WebAdminChannelMetadataStore.MetadataEntry metadata,
            WebAdminAuditEvent auditEvent,
            List<String> changedFields,
            WebAdminUser user
    ) {
        String routeTarget = "#/signals/" + encode(metadata.channel);
        WebAdminRealtimeEvent configEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CONFIG_CHANGED)
                .channel(metadata.channel)
                .severity("INFO")
                .summary("频道显示信息已更新。")
                .routeTarget(routeTarget)
                .payload("targetType", "channel_metadata")
                .payload("changedFields", changedFields)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEvent channelEvent = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.CHANNEL_METADATA_CHANGED)
                .channel(metadata.channel)
                .severity("INFO")
                .summary("频道显示信息已更新：" + metadata.channel)
                .routeTarget(routeTarget)
                .payload("changedFields", changedFields)
                .payload("displayName", metadata.displayName)
                .payload("iconKey", metadata.iconKey)
                .payload("actor", user == null ? "" : user.username));
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.WRITE_AUDIT_APPENDED)
                .channel(metadata.channel)
                .severity("INFO")
                .summary("WebAdmin 写入审计已记录。")
                .routeTarget(routeTarget)
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId())
                .payload("configEventId", configEvent == null ? "" : configEvent.id())
                .payload("channelEventId", channelEvent == null ? "" : channelEvent.id()));
    }

    private void releaseLockAfterWrite(WebAdminChannelMetadataUpdateRequest request, WebAdminUser user, WebAdminSession session, String remoteAddress) {
        if (editLockService == null || request == null || isBlank(request.lockId)) {
            return;
        }
        editLockService.releaseAfterWrite(
                WebAdminEditLockService.TARGET_CHANNEL_METADATA,
                SignalChannel.normalize(request.channel),
                request.lockId,
                user,
                session,
                remoteAddress
        );
    }

    private static WebAdminWriteResult conflictDetected(WebAdminWriteTarget target, WebAdminChannelMetadataStore.MetadataEntry current, String expectedFingerprint) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("expectedFingerprint", expectedFingerprint);
        conflict.put("currentFingerprint", fingerprintFor(current));
        conflict.put("currentMetadata", dto(current, "signal", null));
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.CONFLICT_DETECTED.id(),
                "频道显示信息已被其他用户修改，请刷新后再编辑。",
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

    private static Normalized normalize(WebAdminChannelMetadataUpdateRequest request, String channel) {
        return new Normalized(
                channel,
                safe(request == null ? "" : request.displayName).trim(),
                safe(request == null ? "" : request.note).trim(),
                normalizeIcon(request == null ? "" : request.iconKey)
        );
    }

    private static String normalizeIcon(String iconKey) {
        String value = safe(iconKey).trim();
        return value.isBlank() ? "auto" : value;
    }

    private static boolean metadataEquals(WebAdminChannelMetadataStore.MetadataEntry before, Normalized normalized) {
        return safe(before.displayName).equals(normalized.displayName())
                && safe(before.note).equals(normalized.note())
                && safe(before.iconKey).equals(normalized.iconKey());
    }

    private static List<String> changedFields(WebAdminChannelMetadataStore.MetadataEntry before, WebAdminChannelMetadataStore.MetadataEntry after) {
        List<String> fields = new ArrayList<>();
        if (!safe(before.displayName).equals(safe(after.displayName))) {
            fields.add("displayName");
        }
        if (!safe(before.note).equals(safe(after.note))) {
            fields.add("note");
        }
        if (!safe(before.iconKey).equals(safe(after.iconKey))) {
            fields.add("iconKey");
        }
        return List.copyOf(fields);
    }

    private static Map<String, Object> beforeSummary(WebAdminChannelMetadataStore.MetadataEntry entry) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("channel", entry.channel);
        summary.put("displayName", entry.displayName);
        summary.put("note", entry.note);
        summary.put("iconKey", entry.iconKey);
        summary.put("version", entry.version);
        return summary;
    }

    private static boolean containsControl(String value) {
        String safeValue = safe(value);
        for (int i = 0; i < safeValue.length(); i++) {
            if (Character.isISOControl(safeValue.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record Normalized(String channel, String displayName, String note, String iconKey) {
        private Map<String, Object> summary() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("channel", channel);
            summary.put("displayName", displayName);
            summary.put("note", note);
            summary.put("iconKey", iconKey);
            return summary;
        }
    }
}
