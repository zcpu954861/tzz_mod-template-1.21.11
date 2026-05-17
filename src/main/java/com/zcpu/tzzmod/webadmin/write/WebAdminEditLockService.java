package com.zcpu.tzzmod.webadmin.write;

import com.zcpu.tzzmod.webadmin.WebAdminAuditLogger;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockStatusDto;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WebAdminEditLockService {
    public static final String TARGET_DEVICE_METADATA = "device_metadata";
    public static final String TARGET_DEVICE_BASIC_CONFIG = "device_basic_config";
    public static final String TARGET_DEVICE_EXTENDED_CONFIG = "device_extended_config";
    public static final String TARGET_ACTION_RELAY_ACTIONS = "action_relay_actions";
    public static final String TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS = "virtual_block_device_triggers";
    public static final String TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE = "virtual_block_device_container_template";
    public static final String TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT = "virtual_block_device_single_item_submit";
    public static final String TARGET_INTERACTION_ITEM_MATCHER = "interaction_item_matcher";
    public static final String TARGET_CHANNEL_METADATA = "channel_metadata";
    public static final String TARGET_LOGIC_CHAIN_METADATA = "logic_chain_metadata";
    public static final String TARGET_CONDITION_GROUP = "condition_group";
    public static final String TARGET_SIGNAL_LISTENER_BASIC_CONFIG = "signal_listener_basic_config";
    public static final String TARGET_SIGNAL_LISTENER_ACTIONS = "signal_listener_actions";
    public static final String TARGET_SIGNAL_JOIN_CONFIG = "signal_join_config";
    public static final String TARGET_TIMER_CONFIG = "timer_config";
    public static final String TARGET_REGION_CONTROLLER_CONFIG = "region_controller_config";
    public static final long DEFAULT_TTL_MILLIS = 5L * 60L * 1000L;

    private final Map<String, WebAdminEditLock> locks = new ConcurrentHashMap<>();
    private final WebAdminPermissionService permissionService;
    private final WebAdminWriteSecurityService securityService;
    private final long ttlMillis;

    public WebAdminEditLockService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService
    ) {
        this(permissionService, securityService, DEFAULT_TTL_MILLIS);
    }

    public WebAdminEditLockService(
            WebAdminPermissionService permissionService,
            WebAdminWriteSecurityService securityService,
            long ttlMillis
    ) {
        this.permissionService = permissionService == null ? new WebAdminPermissionService() : permissionService;
        this.securityService = securityService == null ? new WebAdminWriteSecurityService() : securityService;
        this.ttlMillis = Math.max(1_000L, ttlMillis);
    }

    public WebAdminEditLockStatusDto status(
            String targetType,
            String targetId,
            WebAdminUser user,
            WebAdminSession session
    ) {
        String safeTargetType = normalizeTargetType(targetType);
        String safeTargetId = safe(targetId);
        WebAdminEditLock lock = currentLock(safeTargetType, safeTargetId);
        boolean editable = canEdit(user, safeTargetType);
        if (lock == null) {
            return new WebAdminEditLockStatusDto(
                    safeTargetType,
                    safeTargetId,
                    false,
                    false,
                    editable,
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            );
        }
        boolean heldByCurrent = lock.heldBySession(session == null ? "" : session.sessionIdHash);
        return toStatus(lock, heldByCurrent, editable);
    }

    public WebAdminWriteResult acquire(
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminEditLockRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String targetType = normalizeTargetType(request == null ? "" : request.targetType);
        String targetId = safe(request == null ? "" : request.targetId);
        WebAdminWriteTarget target = lockTarget(targetType, targetId);
        WebAdminWriteContext context = writeContext(user, session, remoteAddress, WebAdminOperationType.ACQUIRE_EDIT_LOCK, target);

        WebAdminWriteResult preflight = preflight(user, session, csrfToken, sameOrigin, targetType, target);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "lock_acquire"));
            return preflight;
        }

        long now = System.currentTimeMillis();
        String key = key(targetType, targetId);
        WebAdminEditLock existing = currentLock(targetType, targetId, now);
        if (existing != null) {
            if (existing.heldBySession(session.sessionIdHash)) {
                WebAdminEditLock renewed = existing.renew(now, ttlMillis);
                locks.put(key, renewed);
                WebAdminWriteResult result = ok(target, "编辑锁已续期。", renewed, session, user);
                return result;
            }
            WebAdminWriteResult result = lockConflict(target, existing, "当前由 " + existing.holderUsername() + " 正在编辑，请稍后再试。");
            audit(context, result, lockSummary(existing), Map.of("attempt", "lock_conflict"));
            return result;
        }

        WebAdminEditLock lock = new WebAdminEditLock(
                UUID.randomUUID().toString(),
                targetType,
                targetId,
                user == null ? "" : user.username,
                user == null ? WebAdminRole.VIEWER.id() : user.roleEnum().id(),
                session == null ? "" : session.sessionIdHash,
                now,
                now + ttlMillis,
                now
        );
        locks.put(key, lock);
        WebAdminWriteResult result = ok(target, "已获取编辑锁。", lock, session, user);
        WebAdminAuditEvent auditEvent = audit(context, result, Map.of(), lockSummary(lock));
        publishLockChanged(lock, true, user, auditEvent);
        return result;
    }

    public WebAdminWriteResult heartbeat(
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminEditLockRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String targetType = normalizeTargetType(request == null ? "" : request.targetType);
        String targetId = safe(request == null ? "" : request.targetId);
        String lockId = safe(request == null ? "" : request.lockId);
        WebAdminWriteTarget target = lockTarget(targetType, targetId);

        WebAdminWriteResult preflight = preflight(user, session, csrfToken, sameOrigin, targetType, target);
        if (!preflight.success()) {
            return preflight;
        }

        LockValidation validation = validateLock(targetType, targetId, lockId, user, session);
        if (!validation.success()) {
            return validation.result();
        }
        long now = System.currentTimeMillis();
        WebAdminEditLock renewed = validation.lock().renew(now, ttlMillis);
        locks.put(key(targetType, targetId), renewed);
        return ok(target, "编辑锁已续期。", renewed, session, user);
    }

    public WebAdminWriteResult release(
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminEditLockRequest request,
            String csrfToken,
            boolean sameOrigin
    ) {
        String targetType = normalizeTargetType(request == null ? "" : request.targetType);
        String targetId = safe(request == null ? "" : request.targetId);
        String lockId = safe(request == null ? "" : request.lockId);
        WebAdminWriteTarget target = lockTarget(targetType, targetId);
        WebAdminWriteContext context = writeContext(user, session, remoteAddress, WebAdminOperationType.RELEASE_EDIT_LOCK, target);

        WebAdminWriteResult preflight = securityPreflight(session, csrfToken, sameOrigin, target);
        if (!preflight.success()) {
            audit(context, preflight, Map.of(), Map.of("attempt", "lock_release"));
            return preflight;
        }

        WebAdminWriteResult result = releaseInternal(targetType, targetId, lockId, user, session, context, true, "编辑锁已释放。");
        return result;
    }

    public WebAdminWriteResult releaseAfterWrite(
            String targetType,
            String targetId,
            String lockId,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress
    ) {
        WebAdminWriteTarget target = lockTarget(normalizeTargetType(targetType), targetId);
        WebAdminWriteContext context = writeContext(user, session, remoteAddress, WebAdminOperationType.RELEASE_EDIT_LOCK, target);
        return releaseInternal(normalizeTargetType(targetType), targetId, lockId, user, session, context, true, "保存完成，编辑锁已释放。");
    }

    public WebAdminWriteResult releaseForSessionCleanup(
            String targetType,
            String targetId,
            String lockId,
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            String message
    ) {
        WebAdminWriteTarget target = lockTarget(normalizeTargetType(targetType), targetId);
        WebAdminWriteContext context = writeContext(user, session, remoteAddress, WebAdminOperationType.RELEASE_EDIT_LOCK, target);
        return releaseInternal(
                normalizeTargetType(targetType),
                targetId,
                lockId,
                user,
                session,
                context,
                true,
                safe(message).isBlank() ? "会话结束，编辑锁已释放。" : message
        );
    }

    public LockValidation validateLock(
            String targetType,
            String targetId,
            String lockId,
            WebAdminUser user,
            WebAdminSession session
    ) {
        String safeTargetType = normalizeTargetType(targetType);
        String safeTargetId = safe(targetId);
        WebAdminWriteTarget target = lockTarget(safeTargetType, safeTargetId);
        if (safe(lockId).isBlank()) {
            return LockValidation.failed(WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.EDIT_LOCK_REQUIRED,
                    target,
                    "保存前需要先获取编辑锁。"
            ));
        }
        long now = System.currentTimeMillis();
        WebAdminEditLock lock = currentLock(safeTargetType, safeTargetId, now);
        if (lock == null) {
            return LockValidation.failed(WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.EDIT_LOCK_EXPIRED,
                    target,
                    "编辑锁不存在或已过期，请重新进入编辑。"
            ));
        }
        if (!lock.lockId().equals(lockId)) {
            return LockValidation.failed(lockConflict(target, lock, "当前对象已被其他编辑锁占用，请刷新后重试。"));
        }
        if (!lock.heldBySession(session == null ? "" : session.sessionIdHash)) {
            return LockValidation.failed(lockConflict(target, lock, "当前由 " + lock.holderUsername() + " 正在编辑，请稍后再试。"));
        }
        return LockValidation.ok(lock);
    }

    public int activeLockCount() {
        cleanupExpired(System.currentTimeMillis());
        return locks.size();
    }

    public void clear() {
        locks.clear();
    }

    private WebAdminWriteResult releaseInternal(
            String targetType,
            String targetId,
            String lockId,
            WebAdminUser user,
            WebAdminSession session,
            WebAdminWriteContext context,
            boolean audit,
            String message
    ) {
        String safeTargetType = normalizeTargetType(targetType);
        String safeTargetId = safe(targetId);
        WebAdminWriteTarget target = lockTarget(safeTargetType, safeTargetId);
        WebAdminEditLock lock = currentLock(safeTargetType, safeTargetId);
        if (lock == null) {
            WebAdminWriteResult result = WebAdminWriteResult.noChange(target, "当前没有可释放的编辑锁。");
            if (audit) {
                audit(context, result, Map.of(), Map.of("attempt", "lock_release_noop"));
            }
            return result;
        }
        boolean owner = user != null && user.roleEnum() == WebAdminRole.OWNER;
        boolean holder = lock.heldBySession(session == null ? "" : session.sessionIdHash);
        boolean lockIdMatches = safe(lockId).isBlank() || lock.lockId().equals(lockId);
        if (!lockIdMatches || (!holder && !owner)) {
            WebAdminWriteResult result = lockConflict(target, lock, "只有锁持有人或 OWNER 可以释放该编辑锁。");
            if (audit) {
                audit(context, result, lockSummary(lock), Map.of("attempt", "lock_release_denied"));
            }
            return result;
        }
        locks.remove(key(safeTargetType, safeTargetId), lock);
        WebAdminWriteResult result = WebAdminWriteResult.ok(target, true, message);
        WebAdminAuditEvent auditEvent = audit ? audit(context, result, lockSummary(lock), Map.of("released", true)) : null;
        publishLockChanged(lock, false, user, auditEvent);
        return result;
    }

    private WebAdminWriteResult preflight(
            WebAdminUser user,
            WebAdminSession session,
            String csrfToken,
            boolean sameOrigin,
            String targetType,
            WebAdminWriteTarget target
    ) {
        WebAdminOperationType operationType = operationTypeForTarget(targetType);
        if (operationType == null) {
            return WebAdminWriteResult.validationFailed(target, java.util.List.of(new WebAdminValidationError(
                    "targetType",
                    "unsupported_target",
                    "当前阶段只支持已接入 WebAdmin 安全写链路的编辑锁目标。",
                    targetType
            )));
        }
        WebAdminPermissionDecision permission = permissionService.decide(user, operationType);
        if (!permission.allowed()) {
            return permission.asWriteResult(target);
        }
        return securityPreflight(session, csrfToken, sameOrigin, target);
    }

    private WebAdminWriteResult securityPreflight(
            WebAdminSession session,
            String csrfToken,
            boolean sameOrigin,
            WebAdminWriteTarget target
    ) {
        WebAdminWriteResult csrf = securityService.requireValidCsrf(session, csrfToken);
        if (!csrf.success()) {
            return WebAdminWriteResult.failed(resultCode(csrf.code()), target, csrf.message());
        }
        if (!sameOrigin) {
            return WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.CSRF_INVALID,
                    target,
                    "写请求来源校验失败，请刷新页面后重试。"
            );
        }
        return WebAdminWriteResult.ok(target, false, "写请求安全校验通过。");
    }

    private WebAdminWriteResult ok(
            WebAdminWriteTarget target,
            String message,
            WebAdminEditLock lock,
            WebAdminSession session,
            WebAdminUser user
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lock", toStatus(lock, lock.heldBySession(session == null ? "" : session.sessionIdHash), canEdit(user, lock.targetType())));
        data.put("ttlSeconds", ttlMillis / 1000L);
        return new WebAdminWriteResult(
                true,
                WebAdminWriteResultCode.OK.id(),
                message,
                target.targetType(),
                target.targetId(),
                true,
                java.util.List.of(),
                "",
                "",
                false,
                Map.of(),
                data
        );
    }

    private WebAdminWriteResult lockConflict(WebAdminWriteTarget target, WebAdminEditLock lock, String message) {
        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("targetType", lock.targetType());
        conflict.put("targetId", lock.targetId());
        conflict.put("holderUsername", lock.holderUsername());
        conflict.put("holderRole", lock.holderRole());
        conflict.put("expiresAt", iso(lock.expiresAtMillis()));
        return new WebAdminWriteResult(
                false,
                WebAdminWriteResultCode.EDIT_LOCK_CONFLICT.id(),
                message,
                target.targetType(),
                target.targetId(),
                false,
                java.util.List.of(),
                "",
                "",
                false,
                conflict,
                Map.of("lock", toStatus(lock, false, false))
        );
    }

    private WebAdminEditLockStatusDto toStatus(WebAdminEditLock lock, boolean heldByCurrentUser, boolean editable) {
        return new WebAdminEditLockStatusDto(
                lock.targetType(),
                lock.targetId(),
                true,
                heldByCurrentUser,
                editable,
                heldByCurrentUser ? lock.lockId() : "",
                lock.holderUsername(),
                lock.holderRole(),
                iso(lock.acquiredAtMillis()),
                iso(lock.expiresAtMillis()),
                iso(lock.lastHeartbeatAtMillis())
        );
    }

    private WebAdminEditLock currentLock(String targetType, String targetId) {
        return currentLock(targetType, targetId, System.currentTimeMillis());
    }

    private WebAdminEditLock currentLock(String targetType, String targetId, long now) {
        String key = key(targetType, targetId);
        WebAdminEditLock lock = locks.get(key);
        if (lock != null && lock.expired(now)) {
            locks.remove(key, lock);
            publishLockChanged(lock, false, null, null);
            return null;
        }
        return lock;
    }

    private void cleanupExpired(long now) {
        for (Map.Entry<String, WebAdminEditLock> entry : locks.entrySet()) {
            WebAdminEditLock lock = entry.getValue();
            if (lock != null && lock.expired(now)) {
                locks.remove(entry.getKey(), lock);
                publishLockChanged(lock, false, null, null);
            }
        }
    }

    private boolean canEdit(WebAdminUser user, String targetType) {
        WebAdminOperationType operationType = operationTypeForTarget(targetType);
        return operationType != null && permissionService.decide(user, operationType).allowed();
    }

    private WebAdminAuditEvent audit(
            WebAdminWriteContext context,
            WebAdminWriteResult result,
            Map<String, ?> before,
            Map<String, ?> after
    ) {
        WebAdminAuditEvent event = WebAdminAuditWriter.eventForResult(
                WebAdminWriteAuditContext.from(context),
                result,
                before,
                after
        );
        WebAdminAuditLogger.writeEvent(event);
        return event;
    }

    private void publishLockChanged(
            WebAdminEditLock lock,
            boolean locked,
            WebAdminUser actor,
            WebAdminAuditEvent auditEvent
    ) {
        if (lock == null) {
            return;
        }
        WebAdminRealtimeEvent event = WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.EDIT_LOCK_CHANGED)
                .deviceId(isDeviceLockTarget(lock.targetType()) ? lock.targetId() : "")
                .channel(TARGET_CHANNEL_METADATA.equals(lock.targetType()) ? lock.targetId() : "")
                .severity(locked ? "INFO" : "OK")
                .summary(locked ? lockLabel(lock.targetType()) + "编辑锁已获取。" : lockLabel(lock.targetType()) + "编辑锁已释放。")
                .routeTarget(routeTarget(lock))
                .payload("targetType", lock.targetType())
                .payload("targetId", lock.targetId())
                .payload("locked", locked)
                .payload("holderUsername", locked ? lock.holderUsername() : "")
                .payload("holderRole", locked ? lock.holderRole() : "")
                .payload("expiresAt", locked ? iso(lock.expiresAtMillis()) : "")
                .payload("actor", actor == null ? "" : actor.username)
                .payload("auditId", auditEvent == null ? "" : auditEvent.auditId()));
    }

    private static WebAdminWriteContext writeContext(
            WebAdminUser user,
            WebAdminSession session,
            String remoteAddress,
            WebAdminOperationType operationType,
            WebAdminWriteTarget target
    ) {
        return WebAdminWriteContext.of(user, session, remoteAddress, operationType, target);
    }

    private static Map<String, Object> lockSummary(WebAdminEditLock lock) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (lock == null) {
            return summary;
        }
        summary.put("targetType", lock.targetType());
        summary.put("targetId", lock.targetId());
        summary.put("holderUsername", lock.holderUsername());
        summary.put("holderRole", lock.holderRole());
        summary.put("acquiredAt", iso(lock.acquiredAtMillis()));
        summary.put("expiresAt", iso(lock.expiresAtMillis()));
        return summary;
    }

    private static WebAdminWriteTarget lockTarget(String targetType, String targetId) {
        return new WebAdminWriteTarget("EDIT_LOCK", normalizeTargetType(targetType) + ":" + safe(targetId), "WebAdmin 编辑锁");
    }

    private static String lockLabel(String targetType) {
        String safeTargetType = normalizeTargetType(targetType);
        if (TARGET_DEVICE_METADATA.equals(safeTargetType)) {
            return "设备显示信息";
        }
        if (TARGET_DEVICE_BASIC_CONFIG.equals(safeTargetType)) {
            return "设备基础配置";
        }
        if (TARGET_DEVICE_EXTENDED_CONFIG.equals(safeTargetType)) {
            return "设备类型专属配置";
        }
        if (TARGET_ACTION_RELAY_ACTIONS.equals(safeTargetType)) {
            return "Action Relay Action 列表";
        }
        if (TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS.equals(safeTargetType)) {
            return "VBD 原生触发配置";
        }
        if (TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE.equals(safeTargetType)) {
            return "VBD 容器变化模板";
        }
        if (TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT.equals(safeTargetType)) {
            return "VBD 单物品提交模板";
        }
        if (TARGET_INTERACTION_ITEM_MATCHER.equals(safeTargetType)) {
            return "交互物品匹配";
        }
        if (TARGET_CHANNEL_METADATA.equals(safeTargetType)) {
            return "频道显示信息";
        }
        if (TARGET_LOGIC_CHAIN_METADATA.equals(safeTargetType)) {
            return "逻辑链显示信息";
        }
        if (TARGET_CONDITION_GROUP.equals(safeTargetType)) {
            return "条件组";
        }
        if (TARGET_SIGNAL_LISTENER_BASIC_CONFIG.equals(safeTargetType)) {
            return "Signal Listener 基础配置";
        }
        if (TARGET_SIGNAL_LISTENER_ACTIONS.equals(safeTargetType)) {
            return "Signal Listener 动作列表";
        }
        if (TARGET_SIGNAL_JOIN_CONFIG.equals(safeTargetType)) {
            return "Signal Join 汇合配置";
        }
        if (TARGET_TIMER_CONFIG.equals(safeTargetType)) {
            return "Scheduler Timer 配置";
        }
        if (TARGET_REGION_CONTROLLER_CONFIG.equals(safeTargetType)) {
            return "RegionController 配置";
        }
        return "WebAdmin";
    }

    private static String key(String targetType, String targetId) {
        return normalizeTargetType(targetType) + "\n" + safe(targetId);
    }

    private static String normalizeTargetType(String targetType) {
        return safe(targetType).trim().toLowerCase();
    }

    private static boolean isDeviceLockTarget(String targetType) {
        String safeTargetType = normalizeTargetType(targetType);
        return TARGET_DEVICE_METADATA.equals(safeTargetType)
                || TARGET_DEVICE_BASIC_CONFIG.equals(safeTargetType)
                || TARGET_DEVICE_EXTENDED_CONFIG.equals(safeTargetType)
                || TARGET_ACTION_RELAY_ACTIONS.equals(safeTargetType)
                || TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS.equals(safeTargetType)
                || TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE.equals(safeTargetType)
                || TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT.equals(safeTargetType)
                || TARGET_INTERACTION_ITEM_MATCHER.equals(safeTargetType);
    }

    private static WebAdminOperationType operationTypeForTarget(String targetType) {
        String safeTargetType = normalizeTargetType(targetType);
        if (TARGET_DEVICE_METADATA.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_DEVICE_METADATA;
        }
        if (TARGET_DEVICE_BASIC_CONFIG.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_DEVICE_BASIC_CONFIG;
        }
        if (TARGET_DEVICE_EXTENDED_CONFIG.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_DEVICE_EXTENDED_CONFIG;
        }
        if (TARGET_ACTION_RELAY_ACTIONS.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS;
        }
        if (TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS;
        }
        if (TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE.equals(safeTargetType)) {
            return WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION;
        }
        if (TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT.equals(safeTargetType)) {
            return WebAdminOperationType.START_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION;
        }
        if (TARGET_INTERACTION_ITEM_MATCHER.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_ITEM_MATCHER;
        }
        if (TARGET_CHANNEL_METADATA.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_CHANNEL_METADATA;
        }
        if (TARGET_LOGIC_CHAIN_METADATA.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_LOGIC_CHAIN_METADATA;
        }
        if (TARGET_CONDITION_GROUP.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_CONDITION_GROUP;
        }
        if (TARGET_SIGNAL_LISTENER_BASIC_CONFIG.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_SIGNAL_LISTENER_BASIC_CONFIG;
        }
        if (TARGET_SIGNAL_LISTENER_ACTIONS.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS;
        }
        if (TARGET_SIGNAL_JOIN_CONFIG.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_SIGNAL_JOIN;
        }
        if (TARGET_TIMER_CONFIG.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_TIMER;
        }
        if (TARGET_REGION_CONTROLLER_CONFIG.equals(safeTargetType)) {
            return WebAdminOperationType.EDIT_REGION;
        }
        return null;
    }

    private static String routeTarget(WebAdminEditLock lock) {
        if (isDeviceLockTarget(lock.targetType())) {
            return "#/devices/" + encode(lock.targetId());
        }
        if (TARGET_CHANNEL_METADATA.equals(lock.targetType())) {
            return "#/signals/" + encode(lock.targetId());
        }
        if (TARGET_LOGIC_CHAIN_METADATA.equals(lock.targetType())) {
            return "#/logic-chains/" + encode(lock.targetId());
        }
        if (TARGET_CONDITION_GROUP.equals(lock.targetType())) {
            return "#/condition-groups/" + encode(lock.targetId());
        }
        if (TARGET_SIGNAL_LISTENER_BASIC_CONFIG.equals(lock.targetType())) {
            return "#/signals";
        }
        if (TARGET_SIGNAL_LISTENER_ACTIONS.equals(lock.targetType())) {
            return "#/listeners/" + encode(lock.targetId());
        }
        if (TARGET_SIGNAL_JOIN_CONFIG.equals(lock.targetType())) {
            return "#/signal-joins/" + encode(lock.targetId());
        }
        if (TARGET_TIMER_CONFIG.equals(lock.targetType())) {
            return "#/timers/" + encode(lock.targetId());
        }
        if (TARGET_REGION_CONTROLLER_CONFIG.equals(lock.targetType())) {
            return "#/region-controllers/" + encode(lock.targetId());
        }
        return "";
    }

    private static String iso(long epochMillis) {
        return epochMillis <= 0L ? "" : Instant.ofEpochMilli(epochMillis).toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static WebAdminWriteResultCode resultCode(String code) {
        for (WebAdminWriteResultCode value : WebAdminWriteResultCode.values()) {
            if (value.id().equals(code)) {
                return value;
            }
        }
        return WebAdminWriteResultCode.INTERNAL_ERROR;
    }

    public record LockValidation(boolean success, WebAdminEditLock lock, WebAdminWriteResult result) {
        public static LockValidation ok(WebAdminEditLock lock) {
            return new LockValidation(true, lock, null);
        }

        public static LockValidation failed(WebAdminWriteResult result) {
            return new LockValidation(false, null, result);
        }
    }
}
