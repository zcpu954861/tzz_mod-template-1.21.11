package com.zcpu.tzzmod.webadmin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zcpu.tzzmod.Tzz_mod;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceBasicConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceExtendedConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminInteractionItemMatcherUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDeviceMetadataUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminEditLockRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import com.zcpu.tzzmod.webadmin.dto.WebAdminActionRelayActionsUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminChannelMetadataUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminConditionGroupPreviewRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminConditionGroupRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminLogicChainEditorRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminLogicChainMetadataRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminContainerTemplateSessionCancelRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminContainerTemplateSessionStartRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSingleItemSubmitTemplateSessionCancelRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSingleItemSubmitTemplateSessionStartRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSelectionCancelRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSelectionStartRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerBasicConfigUpdateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerActionRequests;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerCreateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalListenerDeleteRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalJoinRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminTemplateRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminTimerRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminRegionControllerRequests;
import com.zcpu.tzzmod.webadmin.dto.WebAdminVirtualBlockDeviceDeleteRequest;
import com.zcpu.tzzmod.webadmin.dto.WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest;
import com.zcpu.tzzmod.webadmin.route.WebAdminReadonlyRoutes;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventBus;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEvent;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeEventType;
import com.zcpu.tzzmod.webadmin.realtime.WebAdminRealtimeService;
import com.zcpu.tzzmod.webadmin.service.WebAdminActionRelayActionsService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceBasicConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceExtendedConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminInteractionItemMatcherService;
import com.zcpu.tzzmod.webadmin.service.WebAdminDeviceMetadataService;
import com.zcpu.tzzmod.webadmin.service.WebAdminChannelMetadataService;
import com.zcpu.tzzmod.webadmin.service.WebAdminConditionCatalogService;
import com.zcpu.tzzmod.webadmin.service.WebAdminConditionGateHistoryService;
import com.zcpu.tzzmod.webadmin.service.WebAdminConditionGroupService;
import com.zcpu.tzzmod.webadmin.service.WebAdminHelpCatalogService;
import com.zcpu.tzzmod.webadmin.service.WebAdminLogicChainEditorService;
import com.zcpu.tzzmod.webadmin.service.WebAdminLogicChainService;
import com.zcpu.tzzmod.webadmin.service.WebAdminVirtualBlockDeviceContainerTemplateSessionService;
import com.zcpu.tzzmod.webadmin.service.WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSelectionService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSignalListenerActionsService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSignalListenerBasicConfigService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSignalJoinService;
import com.zcpu.tzzmod.webadmin.service.WebAdminSignalListenerLifecycleService;
import com.zcpu.tzzmod.webadmin.service.WebAdminStateVariableService;
import com.zcpu.tzzmod.webadmin.service.WebAdminTemplateService;
import com.zcpu.tzzmod.webadmin.service.WebAdminTimerService;
import com.zcpu.tzzmod.webadmin.service.WebAdminRegionControllerService;
import com.zcpu.tzzmod.webadmin.service.WebAdminUserSettingsService;
import com.zcpu.tzzmod.webadmin.service.WebAdminVirtualBlockDeviceLifecycleService;
import com.zcpu.tzzmod.webadmin.service.WebAdminVirtualBlockDeviceNativeTriggerService;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotService;
import com.zcpu.tzzmod.webadmin.snapshot.WebAdminSnapshotService.WebAdminSnapshotRequest;
import com.zcpu.tzzmod.webadmin.testbridge.WebAdminTestBridgeRoutes;
import com.zcpu.tzzmod.webadmin.write.WebAdminEditLockService;
import com.zcpu.tzzmod.webadmin.write.WebAdminOperationType;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionDecision;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteFoundationService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResultCode;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteTarget;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WebAdminServer {
    private final MinecraftServer minecraftServer;
    private final WebAdminConfig config;
    private final WebAdminUserService userService;
    private final WebAdminSessionService sessionService;
    private final WebAdminReadonlyRoutes readonlyRoutes = new WebAdminReadonlyRoutes();
    private final WebAdminUserSettingsService userSettingsService = new WebAdminUserSettingsService();
    private final WebAdminRealtimeService realtimeService = new WebAdminRealtimeService();
    private final WebAdminWriteSecurityService writeSecurityService = new WebAdminWriteSecurityService();
    private final WebAdminPermissionService permissionService = new WebAdminPermissionService();
    private final WebAdminWriteFoundationService writeFoundationService = new WebAdminWriteFoundationService(writeSecurityService);
    private final WebAdminEditLockService editLockService = new WebAdminEditLockService(permissionService, writeSecurityService);
    private final WebAdminDeviceMetadataService deviceMetadataService = new WebAdminDeviceMetadataService(permissionService, writeSecurityService, editLockService);
    private final WebAdminDeviceBasicConfigService deviceBasicConfigService = new WebAdminDeviceBasicConfigService(permissionService, writeSecurityService, editLockService);
    private final WebAdminDeviceExtendedConfigService deviceExtendedConfigService = new WebAdminDeviceExtendedConfigService(permissionService, writeSecurityService, editLockService);
    private final WebAdminActionRelayActionsService actionRelayActionsService = new WebAdminActionRelayActionsService(permissionService, writeSecurityService, editLockService);
    private final WebAdminInteractionItemMatcherService interactionItemMatcherService = new WebAdminInteractionItemMatcherService(permissionService, writeSecurityService, editLockService);
    private final WebAdminChannelMetadataService channelMetadataService = new WebAdminChannelMetadataService(permissionService, writeSecurityService, editLockService);
    private final WebAdminConditionCatalogService conditionCatalogService = new WebAdminConditionCatalogService();
    private final WebAdminConditionGateHistoryService conditionGateHistoryService = new WebAdminConditionGateHistoryService();
    private final WebAdminConditionGroupService conditionGroupService = new WebAdminConditionGroupService(permissionService, writeSecurityService, editLockService);
    private final WebAdminHelpCatalogService helpCatalogService = new WebAdminHelpCatalogService();
    private final WebAdminLogicChainService logicChainService = new WebAdminLogicChainService(permissionService, writeSecurityService, editLockService);
    private final WebAdminSelectionService selectionService = new WebAdminSelectionService(permissionService, writeSecurityService);
    private final WebAdminSignalListenerBasicConfigService signalListenerBasicConfigService = new WebAdminSignalListenerBasicConfigService(permissionService, writeSecurityService, editLockService);
    private final WebAdminSignalListenerActionsService signalListenerActionsService = new WebAdminSignalListenerActionsService(permissionService, writeSecurityService, editLockService);
    private final WebAdminSignalJoinService signalJoinService = new WebAdminSignalJoinService(permissionService, writeSecurityService, editLockService);
    private final WebAdminTimerService timerService = new WebAdminTimerService(permissionService, writeSecurityService, editLockService);
    private final WebAdminTemplateService templateService = new WebAdminTemplateService(permissionService, writeSecurityService, editLockService);
    private final WebAdminSnapshotService snapshotService = new WebAdminSnapshotService(permissionService, writeSecurityService, editLockService);
    private final WebAdminRegionControllerService regionControllerService = new WebAdminRegionControllerService(permissionService, writeSecurityService, editLockService);
    private final WebAdminLogicChainEditorService logicChainEditorService = new WebAdminLogicChainEditorService(permissionService, writeSecurityService, editLockService, logicChainService, signalJoinService, timerService, channelMetadataService, signalListenerBasicConfigService, signalListenerActionsService, actionRelayActionsService, regionControllerService);
    private final WebAdminStateVariableService stateVariableService = new WebAdminStateVariableService(permissionService);
    private final WebAdminVirtualBlockDeviceLifecycleService virtualBlockDeviceLifecycleService = new WebAdminVirtualBlockDeviceLifecycleService(permissionService, writeSecurityService);
    private final WebAdminVirtualBlockDeviceNativeTriggerService virtualBlockDeviceNativeTriggerService = new WebAdminVirtualBlockDeviceNativeTriggerService(permissionService, writeSecurityService, editLockService);
    private final WebAdminVirtualBlockDeviceContainerTemplateSessionService containerTemplateSessionService = new WebAdminVirtualBlockDeviceContainerTemplateSessionService(permissionService, writeSecurityService, editLockService);
    private final WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService singleItemSubmitTemplateSessionService = new WebAdminVirtualBlockDeviceSingleItemSubmitTemplateSessionService(permissionService, writeSecurityService, editLockService);
    private final WebAdminSignalListenerLifecycleService signalListenerLifecycleService = new WebAdminSignalListenerLifecycleService(permissionService, writeSecurityService);
    private final WebAdminTestBridgeRoutes testBridgeRoutes = new WebAdminTestBridgeRoutes();
    private HttpServer httpServer;
    private ExecutorService executor;

    public WebAdminServer(
            MinecraftServer minecraftServer,
            WebAdminConfig config,
            WebAdminUserService userService,
            WebAdminSessionService sessionService
    ) {
        this.minecraftServer = minecraftServer;
        this.config = config;
        this.userService = userService;
        this.sessionService = sessionService;
    }

    public synchronized void start() throws IOException {
        if (httpServer != null) {
            return;
        }
        httpServer = HttpServer.create(new InetSocketAddress(config.host, config.port), 0);
        httpServer.createContext("/", this::handle);
        executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "tzz-webadmin");
            thread.setDaemon(true);
            return thread;
        });
        httpServer.setExecutor(executor);
        httpServer.start();
        WebAdminAuditLogger.server("start", config);
        Tzz_mod.LOGGER.info("WebAdmin started at http://{}:{} mode={}", config.host, config.port, config.accessMode);
        if (config.accessModeEnum().needsSecurityWarning()) {
            Tzz_mod.LOGGER.warn("WebAdmin is configured for {}. Expose the port only to trusted collaborators.", config.accessMode);
        }
    }

    public synchronized void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        realtimeService.closeAll();
        editLockService.clear();
        writeSecurityService.clear();
        sessionService.clear();
        WebAdminAuditLogger.server("stop", config);
    }

    public boolean running() {
        return httpServer != null;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = normalizePath(exchange.getRequestURI().getPath());
            String method = exchange.getRequestMethod();
            if (path.equals("/") || path.equals("/login")) {
                sendText(exchange, 200, "text/html; charset=utf-8", WebAdminFrontendAssets.loginHtml());
                return;
            }
            if (path.equals("/app") || path.equals("/status")) {
                sendText(exchange, 200, "text/html; charset=utf-8", WebAdminFrontendAssets.appHtml());
                return;
            }
            if (path.equals("/assets/app.css")) {
                sendText(exchange, 200, "text/css; charset=utf-8", WebAdminFrontendAssets.appCss());
                return;
            }
            if (path.equals("/assets/app.js")) {
                sendText(exchange, 200, "application/javascript; charset=utf-8", WebAdminFrontendAssets.appJs());
                return;
            }
            if (path.equals("/api/auth/login") && method.equalsIgnoreCase("POST")) {
                handleLogin(exchange);
                return;
            }
            if (path.startsWith("/api/testbridge/gui/") || path.startsWith("/api/testbridge/client/")) {
                testBridgeRoutes.handle(exchange, minecraftServer, path, method);
                return;
            }
            if (path.startsWith("/api/testbridge/")) {
                runOnServerThread(() -> testBridgeRoutes.handle(exchange, minecraftServer, path, method));
                return;
            }

            AuthContext auth = requireAuth(exchange);
            if (auth == null) {
                return;
            }
            if (path.equals("/api/auth/logout") && method.equalsIgnoreCase("POST")) {
                handleLogout(exchange, auth);
                return;
            }
            if (path.equals("/api/auth/me") && method.equalsIgnoreCase("GET")) {
                handleMe(exchange, auth);
                return;
            }
            if (path.equals("/api/status") && method.equalsIgnoreCase("GET")) {
                handleStatus(exchange, auth);
                return;
            }
            if (path.equals("/api/realtime/events")) {
                if (!method.equalsIgnoreCase("GET")) {
                    WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                    return;
                }
                realtimeService.handleEventStream(exchange, auth.user);
                return;
            }
            if (path.equals("/api/webadmin/users")) {
                if (!method.equalsIgnoreCase("GET")) {
                    WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                    return;
                }
                handleWebAdminUsers(exchange, auth);
                return;
            }
            if (path.equals("/api/webadmin/settings")) {
                if (!method.equalsIgnoreCase("GET")) {
                    WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                    return;
                }
                handleWebAdminSettings(exchange, auth);
                return;
            }
            if (path.equals("/api/webadmin/write/capabilities")) {
                if (!method.equalsIgnoreCase("GET")) {
                    WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                    return;
                }
                handleWebAdminWriteCapabilities(exchange, auth);
                return;
            }
            if (path.equals("/api/webadmin/help")) {
                if (!method.equalsIgnoreCase("GET")) {
                    WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                    return;
                }
                WebAdminJsonResponse.ok(exchange, helpCatalogService.catalog());
                return;
            }
            if (path.equals("/api/webadmin/snapshots") || path.startsWith("/api/webadmin/snapshots/")) {
                runOnServerThread(() -> handleSnapshots(exchange, auth, path, method));
                return;
            }
            if (path.equals("/api/webadmin/users/me/password")) {
                handleWebAdminOwnPassword(exchange, auth, method);
                return;
            }
            if (path.startsWith("/api/webadmin/users/") && path.endsWith("/password-reset")) {
                handleWebAdminUserPasswordReset(exchange, auth, path, method);
                return;
            }
            if (path.equals("/api/webadmin/online-players")) {
                if (!method.equalsIgnoreCase("GET")) {
                    WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                    return;
                }
                runOnServerThread(() -> handleOnlinePlayers(exchange, auth));
                return;
            }
            if (path.startsWith("/api/webadmin/edit-locks/")) {
                runOnServerThread(() -> handleEditLocks(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/device-metadata/")) {
                runOnServerThread(() -> handleDeviceMetadata(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/device-basic-config/")) {
                runOnServerThread(() -> handleDeviceBasicConfig(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/device-extended-config/")) {
                runOnServerThread(() -> handleDeviceExtendedConfig(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/action-relay-actions/")) {
                runOnServerThread(() -> handleActionRelayActions(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/interaction-item-matcher/")) {
                runOnServerThread(() -> handleInteractionItemMatcher(exchange, auth, path, method));
                return;
            }
            if (path.equals("/api/webadmin/channel-metadata")) {
                handleChannelMetadata(exchange, auth, method);
                return;
            }
            if (path.equals("/api/webadmin/condition-types")) {
                handleConditionTypes(exchange, auth, method);
                return;
            }
            if (path.equals("/api/webadmin/condition-gates/history") || path.startsWith("/api/webadmin/condition-gates/history/")) {
                runOnServerThread(() -> handleConditionGateHistory(exchange, auth, path, method));
                return;
            }
            if (path.equals("/api/webadmin/condition-groups") || path.startsWith("/api/webadmin/condition-groups/")) {
                runOnServerThread(() -> handleConditionGroups(exchange, auth, path, method));
                return;
            }
            if (path.equals("/api/webadmin/state-variables") || path.startsWith("/api/webadmin/state-variables/")) {
                runOnServerThread(() -> handleStateVariables(exchange, auth, path, method));
                return;
            }
            if (path.equals("/api/webadmin/logic-chain-editor") || path.startsWith("/api/webadmin/logic-chain-editor/")) {
                runOnServerThread(() -> handleLogicChainEditor(exchange, auth, path, method));
                return;
            }
            if (path.equals("/api/webadmin/logic-chains") || path.startsWith("/api/webadmin/logic-chains/")) {
                runOnServerThread(() -> handleLogicChains(exchange, auth, path, method));
                return;
            }
            if (path.equals("/api/webadmin/templates") || path.startsWith("/api/webadmin/templates/")) {
                runOnServerThread(() -> handleTemplates(exchange, auth, path, method));
                return;
            }
            if (path.equals("/api/webadmin/signal-joins") || path.startsWith("/api/webadmin/signal-joins/")) {
                runOnServerThread(() -> handleSignalJoins(exchange, auth, path, method));
                return;
            }
            if (path.equals("/api/webadmin/timers") || path.startsWith("/api/webadmin/timers/")) {
                runOnServerThread(() -> handleTimers(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/selection/")) {
                runOnServerThread(() -> handleSelection(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/virtual-block-devices/") && path.endsWith("/native-triggers")) {
                runOnServerThread(() -> handleVirtualBlockDeviceNativeTriggers(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/virtual-block-devices/") && path.contains("/container-template")) {
                runOnServerThread(() -> handleVirtualBlockDeviceContainerTemplate(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/virtual-block-devices/") && path.contains("/single-item-submit")) {
                runOnServerThread(() -> handleVirtualBlockDeviceSingleItemSubmitTemplate(exchange, auth, path, method));
                return;
            }
            if (path.startsWith("/api/webadmin/virtual-block-devices/")) {
                runOnServerThread(() -> handleVirtualBlockDeviceLifecycle(exchange, auth, path, method));
                return;
            }
            if (path.equals("/api/webadmin/signal-listeners") || path.startsWith("/api/webadmin/signal-listeners/")) {
                handleSignalListenerLifecycle(exchange, auth, path, method);
                return;
            }
            if (path.startsWith("/api/webadmin/signal-listener-basic-config/")) {
                handleSignalListenerBasicConfig(exchange, auth, path, method);
                return;
            }
            if (path.equals("/api/webadmin/region-controllers") || path.startsWith("/api/webadmin/region-controllers/")) {
                runOnServerThread(() -> handleRegionControllers(exchange, auth, path, method));
                return;
            }
            final boolean[] readonlyHandled = new boolean[1];
            runOnServerThread(() -> readonlyHandled[0] = readonlyRoutes.handle(exchange, minecraftServer, path));
            if (readonlyHandled[0]) {
                return;
            }
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "接口不存在。");
        } catch (AutoSnapshotFailedException exception) {
            Tzz_mod.LOGGER.warn("WebAdmin auto snapshot blocked write: {}", exception.result.message());
            if (exchange.getResponseCode() < 0) {
                WebAdminJsonResponse.ok(exchange, exception.result);
            }
        } catch (Exception exception) {
            Tzz_mod.LOGGER.warn("WebAdmin request failed: {}", exception.getMessage());
            if (exchange.getResponseCode() < 0) {
                WebAdminJsonResponse.error(exchange, 500, "INTERNAL_ERROR", "WebAdmin 请求处理失败。");
            }
        }
    }

    private void runOnServerThread(ServerThreadAction action) throws IOException {
        CompletableFuture<Void> future = new CompletableFuture<>();
        minecraftServer.execute(() -> {
            try {
                action.run();
                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        try {
            future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for Minecraft server thread.", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IOException("Minecraft server thread task failed.", cause);
        }
    }

    @FunctionalInterface
    private interface ServerThreadAction {
        void run() throws Exception;
    }

    private static final class AutoSnapshotFailedException extends RuntimeException {
        private final WebAdminWriteResult result;

        private AutoSnapshotFailedException(WebAdminWriteResult result) {
            super(result == null ? "" : result.message());
            this.result = result;
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        LoginRequest request = readJson(exchange, LoginRequest.class);
        if (request == null || isBlank(request.username) || request.password == null) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "请输入用户名和密码。");
            return;
        }
        WebAdminUserService.AuthResult result = userService.authenticate(request.username, request.password);
        if (!result.success() || result.user() == null) {
            WebAdminJsonResponse.error(exchange, 401, "UNAUTHORIZED", result.message());
            return;
        }
        int ttlSeconds = config.effectiveSessionTtlSeconds(request.rememberMe);
        WebAdminSessionService.CreatedSession created = sessionService.create(
                result.user(),
                ttlSeconds,
                sourceIp(exchange),
                header(exchange, "User-Agent")
        );
        exchange.getResponseHeaders().add("Set-Cookie", sessionCookie(created.token(), ttlSeconds));
        WebAdminJsonResponse.ok(exchange, userDto(result.user()));
    }

    private void handleLogout(HttpExchange exchange, AuthContext auth) throws IOException {
        sessionService.invalidate(auth.rawToken);
        exchange.getResponseHeaders().add("Set-Cookie", clearSessionCookie());
        WebAdminAuditLogger.logout(auth.session.username);
        WebAdminJsonResponse.ok(exchange, Map.of("loggedOut", true));
    }

    private void handleMe(HttpExchange exchange, AuthContext auth) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", auth.user.username);
        data.put("displayName", auth.user.displayName);
        data.put("role", auth.user.role);
        data.put("sessionExpiresAt", WebAdminSessionService.formatInstant(auth.session.expiresAt));
        data.put("accessMode", config.accessMode);
        WebAdminJsonResponse.ok(exchange, data);
    }

    private void handleStatus(HttpExchange exchange, AuthContext auth) throws IOException {
        Map<String, Object> webAdmin = new LinkedHashMap<>();
        webAdmin.put("enabled", config.enabled);
        webAdmin.put("running", running());
        webAdmin.put("host", config.host);
        webAdmin.put("port", config.port);
        webAdmin.put("accessMode", config.accessMode);
        webAdmin.put("sessionCount", sessionService.sessionCount());
        webAdmin.put("realtimeClientCount", WebAdminRealtimeEventBus.clientCount());
        WebAdminStoragePaths storagePaths = WebAdminStoragePaths.resolve(minecraftServer);
        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("scope", WebAdminStoragePaths.STORAGE_SCOPE);
        storage.put("directory", storagePaths.directory().toString());
        storage.put("configPath", storagePaths.configPath().toString());
        storage.put("usersPath", storagePaths.usersPath().toString());
        storage.put("auditLogPath", storagePaths.auditLogPath().toString());
        storage.put("legacyGlobalFilesDetected", storagePaths.hasLegacyGlobalFiles());
        webAdmin.put("storage", storage);

        Map<String, Object> server = new LinkedHashMap<>();
        server.put("type", minecraftServer.isDedicated() ? "DEDICATED" : "INTEGRATED");
        server.put("status", "RUNNING");
        server.put("minecraftVersion", "1.21.11");
        server.put("modVersion", modVersion());

        Map<String, Object> authData = new LinkedHashMap<>();
        authData.put("currentUser", auth.user.username);
        authData.put("role", auth.user.role);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("platformName", "游戏开发编辑平台");
        data.put("webAdmin", webAdmin);
        data.put("server", server);
        data.put("auth", authData);
        WebAdminJsonResponse.ok(exchange, data);
    }

    private void handleOnlinePlayers(HttpExchange exchange, AuthContext auth) throws IOException {
        WebAdminRole role = auth.user.roleEnum();
        if (role != WebAdminRole.EDITOR && role != WebAdminRole.OWNER) {
            WebAdminJsonResponse.error(exchange, 403, "FORBIDDEN", "权限不足：只有编辑者或所有者可以查看在线玩家候选。");
            return;
        }
        List<Map<String, Object>> players = minecraftServer.getPlayerManager().getPlayerList().stream()
                .map(WebAdminServer::onlinePlayerDto)
                .toList();
        WebAdminJsonResponse.ok(exchange, players);
    }

    private static Map<String, Object> onlinePlayerDto(ServerPlayerEntity player) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", player.getName().getString());
        data.put("uuid", player.getUuidAsString());
        return data;
    }

    private void handleWebAdminUsers(HttpExchange exchange, AuthContext auth) throws IOException {
        if (auth.user.roleEnum() != WebAdminRole.OWNER) {
            WebAdminJsonResponse.error(exchange, 403, "FORBIDDEN", "权限不足：只有所有者可以查看用户管理。");
            return;
        }
        WebAdminJsonResponse.ok(exchange, userSettingsService.users(userService, sessionService));
    }

    private void handleWebAdminSettings(HttpExchange exchange, AuthContext auth) throws IOException {
        WebAdminJsonResponse.ok(exchange, userSettingsService.settings(minecraftServer, config, sessionService, auth.user));
    }

    private void handleWebAdminWriteCapabilities(HttpExchange exchange, AuthContext auth) throws IOException {
        WebAdminJsonResponse.ok(exchange, writeFoundationService.capabilities(auth.user, auth.session));
    }

    private WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshotBeforeWrite(HttpExchange exchange, AuthContext auth, WebAdminOperationType operationType, String module, String targetType, String targetId, String reason) {
        if (exchange == null || auth == null || auth.user == null || operationType == null) {
            return WebAdminSnapshotService.WebAdminSnapshotAutoResult.skipped("missing context");
        }
        WebAdminPermissionDecision permission = permissionService.decide(auth.user, operationType);
        if (!permission.allowed()) {
            return WebAdminSnapshotService.WebAdminSnapshotAutoResult.skipped("permission denied");
        }
        WebAdminWriteResult csrf = writeSecurityService.requireValidCsrf(auth.session, header(exchange, "X-TZZ-WebAdmin-CSRF"));
        if (!csrf.success() || !isWriteSameOrigin(exchange)) {
            return WebAdminSnapshotService.WebAdminSnapshotAutoResult.skipped("write security failed");
        }
        WebAdminSnapshotService.WebAdminSnapshotAutoResult result = snapshotService.createAutoBeforeWrite(
                minecraftServer,
                auth.user,
                operationType,
                module,
                targetType,
                targetId,
                reason
        );
        if (!result.created() && !result.skipped()) {
            Tzz_mod.LOGGER.warn("WebAdmin auto snapshot before {} did not complete: {}", operationType, result.message());
            WebAdminWriteTarget target = new WebAdminWriteTarget("SNAPSHOT_AUTO", safe(targetId), "自动快照");
            throw new AutoSnapshotFailedException(WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.INTERNAL_ERROR,
                    target,
                    "写入前自动保存点创建失败，已停止本次写入。请检查快照存储或损坏配置文件。"
            ));
        }
        return result;
    }

    private void annotateAutoSnapshotAfterWrite(WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot, WebAdminWriteResult result) {
        if (autoSnapshot == null || !autoSnapshot.created() || autoSnapshot.record() == null || result == null || !result.success()) {
            return;
        }
        snapshotService.updateAutoSnapshotOperationDiff(minecraftServer, autoSnapshot.record());
    }

    private void handleSnapshots(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String root = "/api/webadmin/snapshots";
        if (path.equals(root)) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, snapshotService.list(minecraftServer, auth.user, queryParams(exchange)));
            return;
        }
        if (path.equals(root + "/manual")) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminSnapshotRequest request = readJson(exchange, WebAdminSnapshotRequest.class);
            if (request == null) {
                request = new WebAdminSnapshotRequest();
            }
            WebAdminWriteResult result = snapshotService.createManual(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        String tail = path.startsWith(root + "/") ? path.substring((root + "/").length()) : "";
        if (tail.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "快照 ID 不能为空。");
            return;
        }
        String[] parts = tail.split("/");
        String snapshotId = decodePathSegment(parts[0]);
        if (snapshotId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "快照 ID 不能为空。");
            return;
        }
        if (parts.length == 1) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, snapshotService.detail(minecraftServer, auth.user, snapshotId));
            return;
        }
        if (parts.length == 3 && "rollback".equals(parts[1]) && "dry-run".equals(parts[2])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminSnapshotRequest request = readJson(exchange, WebAdminSnapshotRequest.class);
            if (request == null) {
                request = new WebAdminSnapshotRequest();
            }
            WebAdminWriteResult result = snapshotService.dryRunRollback(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    snapshotId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        if (parts.length == 3 && "rollback".equals(parts[1]) && "apply".equals(parts[2])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminSnapshotRequest request = readJson(exchange, WebAdminSnapshotRequest.class);
            if (request == null) {
                request = new WebAdminSnapshotRequest();
            }
            WebAdminWriteResult result = snapshotService.applyRollback(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    snapshotId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "配置快照接口不存在。");
    }

    private void handleWebAdminOwnPassword(HttpExchange exchange, AuthContext auth, String method) throws IOException {
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        PasswordChangeRequest request = readJson(exchange, PasswordChangeRequest.class);
        if (request == null) {
            request = new PasswordChangeRequest();
        }
        WebAdminWriteResult security = requirePasswordWriteSecurity(exchange, auth);
        if (!security.success()) {
            WebAdminJsonResponse.ok(exchange, security);
            return;
        }
        WebAdminWriteTarget target = userTarget(auth.user.username);
        if (!safe(request.newPassword).equals(safe(request.confirmPassword))) {
            WebAdminJsonResponse.ok(exchange, WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.VALIDATION_FAILED,
                    target,
                    "两次输入的新密码不一致。"
            ));
            return;
        }
        WebAdminUserService.PasswordUpdateResult update = userService.changeOwnPassword(
                auth.user.username,
                request.oldPassword,
                request.newPassword
        );
        if (!update.success()) {
            WebAdminJsonResponse.ok(exchange, WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.VALIDATION_FAILED,
                    target,
                    update.message()
            ));
            return;
        }
        int invalidated = update.changed() ? sessionService.invalidateUsername(auth.user.username, auth.session.sessionIdHash) : 0;
        if (update.changed()) {
            publishUserPasswordRealtime("password_changed", auth.user.username, auth.user.username);
        }
        WebAdminJsonResponse.ok(exchange, passwordWriteResult(target, update.changed(), update.message(), invalidated));
    }

    private void handleWebAdminUserPasswordReset(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        String prefix = "/api/webadmin/users/";
        String suffix = "/password-reset";
        String username = decodePathSegment(path.substring(prefix.length(), path.length() - suffix.length()));
        PasswordResetRequest request = readJson(exchange, PasswordResetRequest.class);
        if (request == null) {
            request = new PasswordResetRequest();
        }
        WebAdminWriteResult security = requirePasswordWriteSecurity(exchange, auth);
        if (!security.success()) {
            WebAdminJsonResponse.ok(exchange, security);
            return;
        }
        if (auth.user.roleEnum() != WebAdminRole.OWNER) {
            WebAdminJsonResponse.ok(exchange, WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.PERMISSION_DENIED,
                    userTarget(username),
                    "权限不足：只有所有者可以重置 WebAdmin 用户密码。"
            ));
            return;
        }
        if (!safe(request.newPassword).equals(safe(request.confirmPassword))) {
            WebAdminJsonResponse.ok(exchange, WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.VALIDATION_FAILED,
                    userTarget(username),
                    "两次输入的新密码不一致。"
            ));
            return;
        }
        WebAdminUserService.PasswordUpdateResult update = userService.setPassword(username, request.newPassword, auth.user.username);
        if (!update.success() || update.user() == null) {
            WebAdminWriteResultCode code = update.user() == null ? WebAdminWriteResultCode.TARGET_NOT_FOUND : WebAdminWriteResultCode.VALIDATION_FAILED;
            WebAdminJsonResponse.ok(exchange, WebAdminWriteResult.failed(code, userTarget(username), update.message()));
            return;
        }
        int invalidated = update.changed() ? sessionService.invalidateUsername(update.user().username, "") : 0;
        if (update.changed()) {
            publishUserPasswordRealtime("password_reset", update.user().username, auth.user.username);
        }
        WebAdminJsonResponse.ok(exchange, passwordWriteResult(
                userTarget(update.user().username),
                update.changed(),
                update.message(),
                invalidated
        ));
    }

    private void handleEditLocks(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        if (path.equals("/api/webadmin/edit-locks/status")) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, String> query = queryParams(exchange);
            String targetType = query.getOrDefault("targetType", "");
            String targetId = canonicalizeEditLockTargetId(targetType, query.getOrDefault("targetId", ""));
            WebAdminJsonResponse.ok(exchange, editLockService.status(
                    targetType,
                    targetId,
                    auth.user,
                    auth.session
            ));
            return;
        }

        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        WebAdminEditLockRequest request = readJson(exchange, WebAdminEditLockRequest.class);
        if (request == null) {
            request = new WebAdminEditLockRequest();
        }
        request.targetId = canonicalizeEditLockTargetId(request.targetType, request.targetId);
        String csrfToken = header(exchange, "X-TZZ-WebAdmin-CSRF");
        boolean sameOrigin = isWriteSameOrigin(exchange);
        WebAdminWriteResult result;
        if (path.equals("/api/webadmin/edit-locks/acquire")) {
            result = editLockService.acquire(auth.user, auth.session, sourceIp(exchange), request, csrfToken, sameOrigin);
        } else if (path.equals("/api/webadmin/edit-locks/heartbeat")) {
            result = editLockService.heartbeat(auth.user, auth.session, sourceIp(exchange), request, csrfToken, sameOrigin);
        } else if (path.equals("/api/webadmin/edit-locks/release")) {
            result = editLockService.release(auth.user, auth.session, sourceIp(exchange), request, csrfToken, sameOrigin);
        } else {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "编辑锁接口不存在。");
            return;
        }
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleDeviceMetadata(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/device-metadata/";
        String deviceId = decodePathSegment(path.substring(prefix.length()));
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "设备 ID 不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            var device = new com.zcpu.tzzmod.webadmin.service.WebAdminDeviceService().findDevice(minecraftServer, deviceId);
            if (device == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "设备不存在或已被删除。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, deviceMetadataService.metadataFor(minecraftServer, device));
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET 或 PATCH。");
            return;
        }
        WebAdminDeviceMetadataUpdateRequest request = readJson(exchange, WebAdminDeviceMetadataUpdateRequest.class);
        if (request == null) {
            request = new WebAdminDeviceMetadataUpdateRequest();
        }
        request.deviceId = deviceId;
        autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_DEVICE_METADATA, "Device", WebAdminEditLockService.TARGET_DEVICE_METADATA, deviceId, "编辑设备显示信息前自动保存");
        String csrfToken = header(exchange, "X-TZZ-WebAdmin-CSRF");
        boolean sameOrigin = isWriteSameOrigin(exchange);
        WebAdminWriteResult result = deviceMetadataService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                request,
                csrfToken,
                sameOrigin
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleDeviceBasicConfig(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/device-basic-config/";
        String deviceId = decodePathSegment(path.substring(prefix.length()));
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "设备 ID 不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            var config = deviceBasicConfigService.configFor(minecraftServer, auth.user, auth.session, deviceId);
            if (config == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "设备不存在或已被删除。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, config);
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET 或 PATCH。");
            return;
        }
        WebAdminDeviceBasicConfigUpdateRequest request = readJson(exchange, WebAdminDeviceBasicConfigUpdateRequest.class);
        if (request == null) {
            request = new WebAdminDeviceBasicConfigUpdateRequest();
        }
        request.deviceId = deviceId;
        autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_DEVICE_BASIC_CONFIG, "Device", WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG, deviceId, "编辑设备基础配置前自动保存");
        String csrfToken = header(exchange, "X-TZZ-WebAdmin-CSRF");
        boolean sameOrigin = isWriteSameOrigin(exchange);
        WebAdminWriteResult result = deviceBasicConfigService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                request,
                csrfToken,
                sameOrigin
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleDeviceExtendedConfig(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/device-extended-config/";
        String deviceId = decodePathSegment(path.substring(prefix.length()));
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "设备 ID 不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            var config = deviceExtendedConfigService.configFor(minecraftServer, auth.user, auth.session, deviceId);
            if (config == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "设备不存在或已被删除。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, config);
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET 或 PATCH。");
            return;
        }
        WebAdminDeviceExtendedConfigUpdateRequest request = readJson(exchange, WebAdminDeviceExtendedConfigUpdateRequest.class);
        if (request == null) {
            request = new WebAdminDeviceExtendedConfigUpdateRequest();
        }
        request.deviceId = deviceId;
        autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_DEVICE_EXTENDED_CONFIG, "Device", WebAdminEditLockService.TARGET_DEVICE_EXTENDED_CONFIG, deviceId, "编辑设备扩展配置前自动保存");
        String csrfToken = header(exchange, "X-TZZ-WebAdmin-CSRF");
        boolean sameOrigin = isWriteSameOrigin(exchange);
        WebAdminWriteResult result = deviceExtendedConfigService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                request,
                csrfToken,
                sameOrigin
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private String canonicalizeEditLockTargetId(String targetType, String targetId) {
        String safeTargetId = targetId == null ? "" : targetId.trim();
        if (safeTargetId.isBlank() || !isDeviceEditLockTarget(targetType)) {
            return safeTargetId;
        }
        SignalDeviceStore.ResolveResult resolved = SignalDeviceStore.resolveDevice(minecraftServer, safeTargetId);
        if (resolved.foundUnique()) {
            SignalDeviceData device = resolved.device();
            return device == null ? safeTargetId : device.normalized().id();
        }
        return safeTargetId;
    }

    private static boolean isDeviceEditLockTarget(String targetType) {
        String safeTargetType = targetType == null ? "" : targetType.trim().toLowerCase(Locale.ROOT);
        return WebAdminEditLockService.TARGET_DEVICE_METADATA.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_DEVICE_BASIC_CONFIG.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_DEVICE_EXTENDED_CONFIG.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT.equals(safeTargetType)
                || WebAdminEditLockService.TARGET_INTERACTION_ITEM_MATCHER.equals(safeTargetType);
    }

    private void handleActionRelayActions(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/action-relay-actions/";
        String deviceId = decodePathSegment(path.substring(prefix.length()));
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "Action Relay 设备 ID 不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            Map<String, Object> actions = actionRelayActionsService.actionsFor(minecraftServer, auth.user, auth.session, deviceId);
            if (actions == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "Action Relay 设备不存在或引用不唯一。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, actions);
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET 或 PATCH。");
            return;
        }
        WebAdminActionRelayActionsUpdateRequest request = readJson(exchange, WebAdminActionRelayActionsUpdateRequest.class);
        if (request == null) {
            request = new WebAdminActionRelayActionsUpdateRequest();
        }
        request.deviceId = deviceId;
        autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_ACTION_RELAY_ACTIONS, "ActionRelay", WebAdminEditLockService.TARGET_ACTION_RELAY_ACTIONS, deviceId, "编辑 Action Relay 动作前自动保存");
        WebAdminWriteResult result = actionRelayActionsService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                deviceId,
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleInteractionItemMatcher(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/interaction-item-matcher/";
        String deviceId = decodePathSegment(path.substring(prefix.length()));
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "virtual_block_device 设备 ID 不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            Map<String, Object> config = interactionItemMatcherService.configFor(minecraftServer, auth.user, auth.session, deviceId);
            if (config == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "virtual_block_device 不存在或引用不唯一。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, config);
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET 或 PATCH。");
            return;
        }
        WebAdminInteractionItemMatcherUpdateRequest request = readJson(exchange, WebAdminInteractionItemMatcherUpdateRequest.class);
        if (request == null) {
            request = new WebAdminInteractionItemMatcherUpdateRequest();
        }
        request.deviceId = deviceId;
        autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_ITEM_MATCHER, "VirtualBlockDevice", WebAdminEditLockService.TARGET_INTERACTION_ITEM_MATCHER, deviceId, "编辑交互物品匹配前自动保存");
        WebAdminWriteResult result = interactionItemMatcherService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                deviceId,
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleChannelMetadata(HttpExchange exchange, AuthContext auth, String method) throws IOException {
        Map<String, String> query = queryParams(exchange);
        String channel = query.getOrDefault("channel", "");
        if (channel.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "频道不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            WebAdminJsonResponse.ok(exchange, channelMetadataService.metadataFor(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    channel,
                    "signal"
            ));
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / PATCH。");
            return;
        }
        WebAdminChannelMetadataUpdateRequest request = readJson(exchange, WebAdminChannelMetadataUpdateRequest.class);
        if (request == null) {
            request = new WebAdminChannelMetadataUpdateRequest();
        }
        request.channel = channel;
        WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_CHANNEL_METADATA, "SignalBridge", WebAdminEditLockService.TARGET_CHANNEL_METADATA, channel, "编辑频道显示信息前自动保存");
        WebAdminWriteResult result = channelMetadataService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        annotateAutoSnapshotAfterWrite(autoSnapshot, result);
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleLogicChainEditor(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String root = "/api/webadmin/logic-chain-editor";
        if (path.equals(root + "/capabilities")) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, logicChainEditorService.capabilities(auth.user));
            return;
        }
        String tail = path.startsWith(root + "/") ? path.substring((root + "/").length()) : "";
        if (tail.isBlank()) {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "Logic Chain 编辑器接口不存在。");
            return;
        }
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        WebAdminLogicChainEditorRequest request = readJson(exchange, WebAdminLogicChainEditorRequest.class);
        if (request == null) {
            request = new WebAdminLogicChainEditorRequest();
        }
        WebAdminWriteResult result;
        if ("enter".equals(tail)) {
            result = logicChainEditorService.enter(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
        } else if ("validate-draft".equals(tail)) {
            result = logicChainEditorService.validateDraft(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
        } else if ("save-draft".equals(tail)) {
            WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_LOGIC_CHAIN, "Logic Chain", WebAdminEditLockService.TARGET_LOGIC_CHAIN_EDITOR, safe(request.rootType) + ":" + safe(request.rootRef), "保存 Logic Chain 草稿前自动保存");
            result = logicChainEditorService.saveDraft(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            annotateAutoSnapshotAfterWrite(autoSnapshot, result);
        } else if ("cancel".equals(tail)) {
            result = logicChainEditorService.cancel(
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
        } else {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "Logic Chain 编辑器接口不存在。");
            return;
        }
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleLogicChains(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String root = "/api/webadmin/logic-chains";
        if (path.equals(root)) {
            if (method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.ok(exchange, logicChainService.listChains(minecraftServer, auth.user, auth.session, intQuery(exchange, "limit", 500)));
                return;
            }
            if (method.equalsIgnoreCase("POST")) {
                WebAdminLogicChainMetadataRequest request = readJson(exchange, WebAdminLogicChainMetadataRequest.class);
                if (request == null) {
                    request = new WebAdminLogicChainMetadataRequest();
                }
                autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_LOGIC_CHAIN_METADATA, "Logic Chain", WebAdminEditLockService.TARGET_LOGIC_CHAIN_METADATA, safe(request.chainId), "创建逻辑链显示信息前自动保存");
                WebAdminWriteResult result = logicChainService.upsertMetadata(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / POST。");
            return;
        }

        String prefix = root + "/";
        String tail = path.startsWith(prefix) ? path.substring(prefix.length()) : "";
        if (tail.equals("resolve")) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, String> query = queryParams(exchange);
            boolean includeDisabled = !"false".equalsIgnoreCase(query.getOrDefault("includeDisabled", "true"));
            WebAdminJsonResponse.ok(exchange, logicChainService.graphForRoot(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    query.getOrDefault("rootType", "channel"),
                    query.getOrDefault("rootRef", ""),
                    includeDisabled,
                    intQuery(exchange, "maxDepth", 3),
                    null
            ));
            return;
        }

        String[] parts = tail.split("/");
        String chainId = parts.length == 0 ? "" : decodePathSegment(parts[0]);
        if (chainId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "逻辑链 ID 不能为空。");
            return;
        }
        if (parts.length == 1) {
            if (method.equalsIgnoreCase("GET")) {
                Map<String, String> query = queryParams(exchange);
                WebAdminJsonResponse.ok(exchange, logicChainService.graphForChain(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        chainId,
                        query.getOrDefault("focusChannel", query.getOrDefault("focus", ""))
                ));
                return;
            }
            if (method.equalsIgnoreCase("PATCH")) {
                WebAdminLogicChainMetadataRequest request = readJson(exchange, WebAdminLogicChainMetadataRequest.class);
                if (request == null) {
                    request = new WebAdminLogicChainMetadataRequest();
                }
                request.chainId = chainId;
                autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_LOGIC_CHAIN_METADATA, "Logic Chain", WebAdminEditLockService.TARGET_LOGIC_CHAIN_METADATA, chainId, "编辑逻辑链显示信息前自动保存");
                WebAdminWriteResult result = logicChainService.upsertMetadata(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / PATCH。");
            return;
        }
        if (parts.length == 2 && "delete".equals(parts[1])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminLogicChainMetadataRequest request = readJson(exchange, WebAdminLogicChainMetadataRequest.class);
            if (request == null) {
                request = new WebAdminLogicChainMetadataRequest();
            }
            request.chainId = chainId;
            autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_LOGIC_CHAIN_METADATA, "Logic Chain", WebAdminEditLockService.TARGET_LOGIC_CHAIN_METADATA, chainId, "删除逻辑链显示信息前自动保存");
            WebAdminWriteResult result = logicChainService.deleteMetadata(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    chainId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "逻辑链接口不存在。");
    }

    private void handleSignalJoins(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String root = "/api/webadmin/signal-joins";
        if (path.equals(root)) {
            if (method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.ok(exchange, signalJoinService.list(minecraftServer, auth.user, auth.session));
                return;
            }
            if (method.equalsIgnoreCase("POST")) {
                WebAdminSignalJoinRequest request = readJson(exchange, WebAdminSignalJoinRequest.class);
                if (request == null) {
                    request = new WebAdminSignalJoinRequest();
                }
                WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_SIGNAL_JOIN, "Signal Join", WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG, safe(request.id), "创建 Signal Join 前自动保存");
                WebAdminWriteResult result = signalJoinService.create(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                annotateAutoSnapshotAfterWrite(autoSnapshot, result);
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / POST。");
            return;
        }

        String prefix = root + "/";
        String tail = path.startsWith(prefix) ? path.substring(prefix.length()) : "";
        if (tail.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "Signal Join ID 不能为空。");
            return;
        }
        String[] parts = tail.split("/");
        String joinId = decodePathSegment(parts[0]);
        if (joinId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "Signal Join ID 不能为空。");
            return;
        }

        if (parts.length == 1) {
            if (method.equalsIgnoreCase("GET")) {
                Map<String, Object> detail = signalJoinService.detail(minecraftServer, auth.user, auth.session, joinId);
                if (Boolean.TRUE.equals(detail.get("notFound"))) {
                    WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", String.valueOf(detail.getOrDefault("message", "Signal Join 不存在。")));
                    return;
                }
                WebAdminJsonResponse.ok(exchange, detail);
                return;
            }
            if (method.equalsIgnoreCase("PATCH")) {
                WebAdminSignalJoinRequest request = readJson(exchange, WebAdminSignalJoinRequest.class);
                if (request == null) {
                    request = new WebAdminSignalJoinRequest();
                }
                request.id = joinId;
                WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_SIGNAL_JOIN, "Signal Join", WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG, joinId, "编辑 Signal Join 前自动保存");
                WebAdminWriteResult result = signalJoinService.update(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        joinId,
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                annotateAutoSnapshotAfterWrite(autoSnapshot, result);
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            if (method.equalsIgnoreCase("DELETE")) {
                WebAdminSignalJoinRequest request = readJson(exchange, WebAdminSignalJoinRequest.class);
                if (request == null) {
                    request = new WebAdminSignalJoinRequest();
                }
                request.id = joinId;
                WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_SIGNAL_JOIN, "Signal Join", WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG, joinId, "删除 Signal Join 前自动保存");
                WebAdminWriteResult result = signalJoinService.delete(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        joinId,
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                annotateAutoSnapshotAfterWrite(autoSnapshot, result);
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / PATCH / DELETE。");
            return;
        }

        if (parts.length == 2 && "status".equals(parts[1])) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, signalJoinService.status(minecraftServer, auth.user, joinId, queryParams(exchange).getOrDefault("scopeKey", "")));
            return;
        }

        if (parts.length == 2 && "reset".equals(parts[1])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminSignalJoinRequest request = readJson(exchange, WebAdminSignalJoinRequest.class);
            if (request == null) {
                request = new WebAdminSignalJoinRequest();
            }
            request.id = joinId;
            WebAdminWriteResult result = signalJoinService.reset(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    joinId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        if (parts.length == 2 && "delete".equals(parts[1])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminSignalJoinRequest request = readJson(exchange, WebAdminSignalJoinRequest.class);
            if (request == null) {
                request = new WebAdminSignalJoinRequest();
            }
            request.id = joinId;
            WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_SIGNAL_JOIN, "Signal Join", WebAdminEditLockService.TARGET_SIGNAL_JOIN_CONFIG, joinId, "删除 Signal Join 前自动保存");
            WebAdminWriteResult result = signalJoinService.delete(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    joinId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            annotateAutoSnapshotAfterWrite(autoSnapshot, result);
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "Signal Join 接口不存在。");
    }

    private void handleTemplates(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String root = "/api/webadmin/templates";
        if (path.equals(root)) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, templateService.list(minecraftServer, auth.user, auth.session));
            return;
        }
        String tail = path.startsWith(root + "/") ? path.substring((root + "/").length()) : "";
        if (tail.equals("detail")) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, String> query = queryParams(exchange);
            WebAdminJsonResponse.ok(exchange, templateService.detail(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    query.getOrDefault("source", ""),
                    query.getOrDefault("id", "")
            ));
            return;
        }
        if (tail.equals("export")) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, String> query = queryParams(exchange);
            WebAdminJsonResponse.ok(exchange, templateService.exportTemplate(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    query.getOrDefault("source", ""),
                    query.getOrDefault("id", "")
            ));
            return;
        }
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        WebAdminTemplateRequest request = readJson(exchange, WebAdminTemplateRequest.class);
        if (request == null) {
            request = new WebAdminTemplateRequest();
        }
        if (tail.equals("import-preview")) {
            WebAdminJsonResponse.ok(exchange, templateService.previewImport(minecraftServer, auth.user, auth.session, request));
            return;
        }
        if (tail.equals("import")) {
            WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.IMPORT_TEMPLATE, "Template", WebAdminEditLockService.TARGET_TEMPLATE_STORE, safe(request.templateId), "导入模板前自动保存");
            WebAdminWriteResult result = templateService.importUserTemplate(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            annotateAutoSnapshotAfterWrite(autoSnapshot, result);
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        if (tail.equals("apply-preview")) {
            WebAdminWriteResult result = templateService.dryRunApply(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        if (tail.equals("apply")) {
            WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.APPLY_TEMPLATE, "Template", WebAdminEditLockService.TARGET_TEMPLATE_APPLY, safe(request.templateId), "应用模板前自动保存");
            WebAdminWriteResult result = templateService.apply(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            annotateAutoSnapshotAfterWrite(autoSnapshot, result);
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "模板中心接口不存在。");
    }

    private void handleTimers(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String root = "/api/webadmin/timers";
        if (path.equals(root)) {
            if (method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.ok(exchange, timerService.list(minecraftServer, auth.user, auth.session));
                return;
            }
            if (method.equalsIgnoreCase("POST")) {
                WebAdminTimerRequest request = readJson(exchange, WebAdminTimerRequest.class);
                if (request == null) {
                    request = new WebAdminTimerRequest();
                }
                WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_TIMER, "Timer", WebAdminEditLockService.TARGET_TIMER_CONFIG, safe(request.id), "创建 Timer 前自动保存");
                WebAdminWriteResult result = timerService.create(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                annotateAutoSnapshotAfterWrite(autoSnapshot, result);
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / POST。");
            return;
        }

        String prefix = root + "/";
        String tail = path.startsWith(prefix) ? path.substring(prefix.length()) : "";
        if (tail.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "Timer ID 不能为空。");
            return;
        }
        String[] parts = tail.split("/");
        String timerId = decodePathSegment(parts[0]);
        if (timerId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "Timer ID 不能为空。");
            return;
        }

        if (parts.length == 1) {
            if (method.equalsIgnoreCase("GET")) {
                Map<String, Object> detail = timerService.detail(minecraftServer, auth.user, auth.session, timerId);
                if (Boolean.TRUE.equals(detail.get("notFound"))) {
                    WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", String.valueOf(detail.getOrDefault("message", "Timer 不存在。")));
                    return;
                }
                WebAdminJsonResponse.ok(exchange, detail);
                return;
            }
            if (method.equalsIgnoreCase("PATCH")) {
                WebAdminTimerRequest request = readJson(exchange, WebAdminTimerRequest.class);
                if (request == null) {
                    request = new WebAdminTimerRequest();
                }
                request.id = timerId;
                WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_TIMER, "Timer", WebAdminEditLockService.TARGET_TIMER_CONFIG, timerId, "编辑 Timer 前自动保存");
                WebAdminWriteResult result = timerService.update(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        timerId,
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                annotateAutoSnapshotAfterWrite(autoSnapshot, result);
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            if (method.equalsIgnoreCase("DELETE")) {
                WebAdminTimerRequest request = readJson(exchange, WebAdminTimerRequest.class);
                if (request == null) {
                    request = new WebAdminTimerRequest();
                }
                request.id = timerId;
                WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_TIMER, "Timer", WebAdminEditLockService.TARGET_TIMER_CONFIG, timerId, "删除 Timer 前自动保存");
                WebAdminWriteResult result = timerService.delete(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        timerId,
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                annotateAutoSnapshotAfterWrite(autoSnapshot, result);
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / PATCH / DELETE。");
            return;
        }

        if (parts.length == 2 && "status".equals(parts[1])) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, timerService.status(minecraftServer, auth.user, timerId));
            return;
        }

        if (parts.length == 2 && "start".equals(parts[1])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminTimerRequest request = readJson(exchange, WebAdminTimerRequest.class);
            if (request == null) {
                request = new WebAdminTimerRequest();
            }
            request.id = timerId;
            WebAdminWriteResult result = timerService.start(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    timerId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        if (parts.length == 2 && "cancel".equals(parts[1])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminTimerRequest request = readJson(exchange, WebAdminTimerRequest.class);
            if (request == null) {
                request = new WebAdminTimerRequest();
            }
            request.id = timerId;
            WebAdminWriteResult result = timerService.cancel(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    timerId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        if (parts.length == 2 && "reset".equals(parts[1])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminTimerRequest request = readJson(exchange, WebAdminTimerRequest.class);
            if (request == null) {
                request = new WebAdminTimerRequest();
            }
            request.id = timerId;
            WebAdminWriteResult result = timerService.reset(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    timerId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        if (parts.length == 2 && "delete".equals(parts[1])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminTimerRequest request = readJson(exchange, WebAdminTimerRequest.class);
            if (request == null) {
                request = new WebAdminTimerRequest();
            }
            request.id = timerId;
            WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_TIMER, "Timer", WebAdminEditLockService.TARGET_TIMER_CONFIG, timerId, "删除 Timer 前自动保存");
            WebAdminWriteResult result = timerService.delete(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    timerId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            annotateAutoSnapshotAfterWrite(autoSnapshot, result);
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "Timer 接口不存在。");
    }

    private void handleConditionTypes(HttpExchange exchange, AuthContext auth, String method) throws IOException {
        if (!method.equalsIgnoreCase("GET")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
            return;
        }
        WebAdminJsonResponse.ok(exchange, conditionCatalogService.catalog());
    }

    private void handleConditionGateHistory(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String root = "/api/webadmin/condition-gates/history";
        if (path.equals(root)) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, conditionGateHistoryService.list(queryParams(exchange)));
            return;
        }

        String tail = path.startsWith(root + "/") ? path.substring((root + "/").length()) : "";
        if (tail.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "Condition gate history ID 不能为空。");
            return;
        }
        String[] parts = tail.split("/");
        String recordId = decodePathSegment(parts[0]);
        if (recordId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "Condition gate history ID 不能为空。");
            return;
        }
        if (parts.length == 1) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, Object> detail = conditionGateHistoryService.detail(recordId);
            if (detail == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "条件 gate 历史记录不存在或已被淘汰。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, detail);
            return;
        }
        if (parts.length == 2 && "replay".equals(parts[1])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, conditionGateHistoryService.replay(minecraftServer, recordId));
            return;
        }
        WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "Condition gate history 接口不存在。");
    }

    private void handleConditionGroups(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String root = "/api/webadmin/condition-groups";
        // 8.5 Condition Group API marker: /delete /validate /preview.
        if (path.equals(root)) {
            if (method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.ok(exchange, conditionGroupService.list(minecraftServer, auth.user, auth.session));
                return;
            }
            if (method.equalsIgnoreCase("POST")) {
                WebAdminConditionGroupRequest request = readJson(exchange, WebAdminConditionGroupRequest.class);
                if (request == null) {
                    request = new WebAdminConditionGroupRequest();
                }
                WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_CONDITION_GROUP, "Condition", WebAdminEditLockService.TARGET_CONDITION_GROUP, safe(request.id), "创建条件组前自动保存");
                WebAdminWriteResult result = conditionGroupService.create(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                annotateAutoSnapshotAfterWrite(autoSnapshot, result);
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / POST。");
            return;
        }
        // 8.9 action gate available-list marker: condition-groups/available queryMap.
        if (path.equals(root + "/available")) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, String> queryMap = queryParams(exchange);
            WebAdminJsonResponse.ok(exchange, conditionGroupService.available(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    queryMap
            ));
            return;
        }

        String prefix = root + "/";
        String tail = path.startsWith(prefix) ? path.substring(prefix.length()) : "";
        String[] parts = tail.split("/");
        String groupId = parts.length == 0 ? "" : decodePathSegment(parts[0]);
        if (groupId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "条件组 ID 不能为空。");
            return;
        }
        if (parts.length == 1) {
            if (method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.ok(exchange, conditionGroupService.detail(minecraftServer, auth.user, auth.session, groupId));
                return;
            }
            if (method.equalsIgnoreCase("PATCH")) {
                WebAdminConditionGroupRequest request = readJson(exchange, WebAdminConditionGroupRequest.class);
                if (request == null) {
                    request = new WebAdminConditionGroupRequest();
                }
                WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_CONDITION_GROUP, "Condition", WebAdminEditLockService.TARGET_CONDITION_GROUP, groupId, "编辑条件组前自动保存");
                WebAdminWriteResult result = conditionGroupService.update(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        groupId,
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                annotateAutoSnapshotAfterWrite(autoSnapshot, result);
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / PATCH。");
            return;
        }
        String action = parts.length > 1 ? parts[1] : "";
        if ("delete".equals(action)) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminConditionGroupRequest request = readJson(exchange, WebAdminConditionGroupRequest.class);
            if (request == null) {
                request = new WebAdminConditionGroupRequest();
            }
            WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_CONDITION_GROUP, "Condition", WebAdminEditLockService.TARGET_CONDITION_GROUP, groupId, "删除条件组前自动保存");
            WebAdminWriteResult result = conditionGroupService.delete(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    groupId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            annotateAutoSnapshotAfterWrite(autoSnapshot, result);
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        if ("validate".equals(action)) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminConditionGroupPreviewRequest request = readJson(exchange, WebAdminConditionGroupPreviewRequest.class);
            WebAdminJsonResponse.ok(exchange, conditionGroupService.validate(minecraftServer, auth.user, groupId, request == null ? null : request.groupDefinition));
            return;
        }
        if ("preview".equals(action)) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminConditionGroupPreviewRequest request = readJson(exchange, WebAdminConditionGroupPreviewRequest.class);
            WebAdminJsonResponse.ok(exchange, conditionGroupService.preview(minecraftServer, auth.user, groupId, request));
            return;
        }
        WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "条件组接口不存在。");
    }

    private void handleStateVariables(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String root = "/api/webadmin/state-variables";
        if (!method.equalsIgnoreCase("GET")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
            return;
        }
        if (path.equals(root)) {
            WebAdminJsonResponse.ok(exchange, stateVariableService.list(minecraftServer, auth.user, queryParams(exchange)));
            return;
        }
        String prefix = root + "/";
        String id = path.startsWith(prefix) ? decodePathSegment(path.substring(prefix.length())) : "";
        if (id.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "状态变量 ID 不能为空。");
            return;
        }
        WebAdminDtos.StateVariableDetailDto detail = stateVariableService.detail(minecraftServer, auth.user, id);
        if (detail == null) {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "状态变量不存在。");
            return;
        }
        WebAdminJsonResponse.ok(exchange, detail);
    }

    private void handleSelection(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        if (path.equals("/api/webadmin/selection/status")) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            if (auth.user.roleEnum() != WebAdminRole.EDITOR && auth.user.roleEnum() != WebAdminRole.OWNER) {
                WebAdminJsonResponse.error(exchange, 403, "FORBIDDEN", "权限不足：只有编辑者或所有者可以查看选择状态。");
                return;
            }
            Map<String, String> query = queryParams(exchange);
            WebAdminJsonResponse.ok(exchange, selectionService.status(query.getOrDefault("selectionId", "")));
            return;
        }
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        if (path.equals("/api/webadmin/selection/start")) {
            WebAdminSelectionStartRequest request = readJson(exchange, WebAdminSelectionStartRequest.class);
            if (request == null) {
                request = new WebAdminSelectionStartRequest();
            }
            WebAdminWriteResult result = selectionService.start(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        if (path.equals("/api/webadmin/selection/cancel")) {
            WebAdminSelectionCancelRequest request = readJson(exchange, WebAdminSelectionCancelRequest.class);
            if (request == null) {
                request = new WebAdminSelectionCancelRequest();
            }
            WebAdminWriteResult result = selectionService.cancel(
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "选择接口不存在。");
    }

    private void handleVirtualBlockDeviceLifecycle(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/virtual-block-devices/";
        String suffix = "/delete";
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备生命周期接口不存在。");
            return;
        }
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        String encodedDeviceId = path.substring(prefix.length(), path.length() - suffix.length());
        String deviceId = decodePathSegment(encodedDeviceId);
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "虚拟方块设备 ID 不能为空。");
            return;
        }
        WebAdminVirtualBlockDeviceDeleteRequest request = readJson(exchange, WebAdminVirtualBlockDeviceDeleteRequest.class);
        if (request == null) {
            request = new WebAdminVirtualBlockDeviceDeleteRequest();
        }
        request.deviceId = deviceId;
        autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.DELETE_VIRTUAL_BLOCK_DEVICE, "VirtualBlockDevice", "virtual_block_device", deviceId, "删除虚拟方块设备前自动保存");
        WebAdminWriteResult result = virtualBlockDeviceLifecycleService.delete(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                deviceId,
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleVirtualBlockDeviceNativeTriggers(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/virtual-block-devices/";
        String suffix = "/native-triggers";
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备原生触发配置接口不存在。");
            return;
        }
        if (!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / PATCH。");
            return;
        }
        String encodedDeviceId = path.substring(prefix.length(), path.length() - suffix.length());
        String deviceId = decodePathSegment(encodedDeviceId);
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "虚拟方块设备 ID 不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("PATCH")) {
            WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest request = readJson(exchange, WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest.class);
            if (request == null) {
                request = new WebAdminVirtualBlockDeviceNativeTriggersUpdateRequest();
            }
            request.deviceId = deviceId;
            autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS, "VirtualBlockDevice", WebAdminEditLockService.TARGET_VIRTUAL_BLOCK_DEVICE_TRIGGERS, deviceId, "编辑 VBD 原生触发配置前自动保存");
            WebAdminWriteResult result = virtualBlockDeviceNativeTriggerService.update(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    deviceId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        Map<String, Object> data = virtualBlockDeviceNativeTriggerService.overview(minecraftServer, auth.user, auth.session, deviceId);
        if (data == null) {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备不存在。");
            return;
        }
        if (Boolean.FALSE.equals(data.get("supported"))) {
            WebAdminJsonResponse.error(exchange, 400, "VALIDATION_ERROR", String.valueOf(data.getOrDefault("unsupportedReason", "该接口仅支持 virtual_block_device。")));
            return;
        }
        WebAdminJsonResponse.ok(exchange, data);
    }

    private void handleVirtualBlockDeviceContainerTemplate(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/virtual-block-devices/";
        String overviewSuffix = "/container-template";
        String startSuffix = "/container-template-session/start";
        String statusSuffix = "/container-template-session/status";
        String cancelSuffix = "/container-template-session/cancel";
        String suffix;
        if (path.startsWith(prefix) && path.endsWith(overviewSuffix)) {
            suffix = overviewSuffix;
        } else if (path.startsWith(prefix) && path.endsWith(startSuffix)) {
            suffix = startSuffix;
        } else if (path.startsWith(prefix) && path.endsWith(statusSuffix)) {
            suffix = statusSuffix;
        } else if (path.startsWith(prefix) && path.endsWith(cancelSuffix)) {
            suffix = cancelSuffix;
        } else {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备容器模板会话接口不存在。");
            return;
        }
        String encodedDeviceId = path.substring(prefix.length(), path.length() - suffix.length());
        String deviceId = decodePathSegment(encodedDeviceId);
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "虚拟方块设备 ID 不能为空。");
            return;
        }

        if (overviewSuffix.equals(suffix)) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, Object> data = containerTemplateSessionService.overview(minecraftServer, auth.user, auth.session, deviceId);
            if (data == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备不存在。");
                return;
            }
            if (Boolean.FALSE.equals(data.get("supported"))) {
                WebAdminJsonResponse.error(exchange, 400, "VALIDATION_ERROR", String.valueOf(data.getOrDefault("unsupportedReason", "该接口仅支持 virtual_block_device。")));
                return;
            }
            WebAdminJsonResponse.ok(exchange, data);
            return;
        }

        if (statusSuffix.equals(suffix)) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, String> query = queryParams(exchange);
            WebAdminJsonResponse.ok(exchange, containerTemplateSessionService.status(query.getOrDefault("sessionId", "")));
            return;
        }

        if (startSuffix.equals(suffix)) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminContainerTemplateSessionStartRequest request = readJson(exchange, WebAdminContainerTemplateSessionStartRequest.class);
            if (request == null) {
                request = new WebAdminContainerTemplateSessionStartRequest();
            }
            request.deviceId = deviceId;
            WebAdminWriteResult result = containerTemplateSessionService.start(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    deviceId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        WebAdminContainerTemplateSessionCancelRequest request = readJson(exchange, WebAdminContainerTemplateSessionCancelRequest.class);
        if (request == null) {
            request = new WebAdminContainerTemplateSessionCancelRequest();
        }
        request.deviceId = deviceId;
        WebAdminWriteResult result = containerTemplateSessionService.cancel(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                deviceId,
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleVirtualBlockDeviceSingleItemSubmitTemplate(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/virtual-block-devices/";
        String overviewSuffix = "/single-item-submit";
        String startSuffix = "/single-item-submit-session/start";
        String statusSuffix = "/single-item-submit-session/status";
        String cancelSuffix = "/single-item-submit-session/cancel";
        String suffix;
        if (path.startsWith(prefix) && path.endsWith(overviewSuffix)) {
            suffix = overviewSuffix;
        } else if (path.startsWith(prefix) && path.endsWith(startSuffix)) {
            suffix = startSuffix;
        } else if (path.startsWith(prefix) && path.endsWith(statusSuffix)) {
            suffix = statusSuffix;
        } else if (path.startsWith(prefix) && path.endsWith(cancelSuffix)) {
            suffix = cancelSuffix;
        } else {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备单物品提交模板会话接口不存在。");
            return;
        }
        String encodedDeviceId = path.substring(prefix.length(), path.length() - suffix.length());
        String deviceId = decodePathSegment(encodedDeviceId);
        if (deviceId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "虚拟方块设备 ID 不能为空。");
            return;
        }
        if (overviewSuffix.equals(suffix)) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, Object> data = singleItemSubmitTemplateSessionService.overview(minecraftServer, auth.user, auth.session, deviceId);
            if (data == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "虚拟方块设备不存在。");
                return;
            }
            if (Boolean.FALSE.equals(data.get("supported"))) {
                WebAdminJsonResponse.error(exchange, 400, "VALIDATION_ERROR", String.valueOf(data.getOrDefault("unsupportedReason", "该接口仅支持 virtual_block_device。")));
                return;
            }
            WebAdminJsonResponse.ok(exchange, data);
            return;
        }
        if (statusSuffix.equals(suffix)) {
            if (!method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET。");
                return;
            }
            Map<String, String> query = queryParams(exchange);
            WebAdminJsonResponse.ok(exchange, singleItemSubmitTemplateSessionService.status(query.getOrDefault("sessionId", "")));
            return;
        }
        if (startSuffix.equals(suffix)) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminSingleItemSubmitTemplateSessionStartRequest request = readJson(exchange, WebAdminSingleItemSubmitTemplateSessionStartRequest.class);
            if (request == null) {
                request = new WebAdminSingleItemSubmitTemplateSessionStartRequest();
            }
            request.deviceId = deviceId;
            WebAdminWriteResult result = singleItemSubmitTemplateSessionService.start(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    deviceId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        WebAdminSingleItemSubmitTemplateSessionCancelRequest request = readJson(exchange, WebAdminSingleItemSubmitTemplateSessionCancelRequest.class);
        if (request == null) {
            request = new WebAdminSingleItemSubmitTemplateSessionCancelRequest();
        }
        request.deviceId = deviceId;
        WebAdminWriteResult result = singleItemSubmitTemplateSessionService.cancel(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                deviceId,
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleSignalListenerLifecycle(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        if (path.equals("/api/webadmin/signal-listeners")) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminSignalListenerCreateRequest request = readJson(exchange, WebAdminSignalListenerCreateRequest.class);
            if (request == null) {
                request = new WebAdminSignalListenerCreateRequest();
            }
            WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.CREATE_SIGNAL_LISTENER, "Signal Listener", "signal_listener", safe(request.name), "创建 Signal Listener 前自动保存");
            WebAdminWriteResult result = signalListenerLifecycleService.create(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            annotateAutoSnapshotAfterWrite(autoSnapshot, result);
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        String prefix = "/api/webadmin/signal-listeners/";
        if (!path.startsWith(prefix)) {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "Signal Listener 接口不存在。");
            return;
        }
        String tail = path.substring(prefix.length());
        String[] parts = tail.split("/");
        String listenerId = parts.length == 0 ? "" : decodePathSegment(parts[0]);
        if (listenerId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "Signal Listener ID 不能为空。");
            return;
        }

        if (parts.length == 2 && "actions".equals(parts[1])) {
            if (method.equalsIgnoreCase("GET")) {
                Map<String, Object> data = signalListenerActionsService.actionsFor(minecraftServer, auth.user, auth.session, listenerId);
                if (data == null) {
                    WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "Signal Listener 不存在或引用不唯一。");
                    return;
                }
                WebAdminJsonResponse.ok(exchange, data);
                return;
            }
            if (method.equalsIgnoreCase("POST")) {
                WebAdminSignalListenerActionRequests.ActionAddRequest request = readJson(exchange, WebAdminSignalListenerActionRequests.ActionAddRequest.class);
                if (request == null) {
                    request = new WebAdminSignalListenerActionRequests.ActionAddRequest();
                }
                request.listenerId = listenerId;
                WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS, "Signal Listener", WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, listenerId, "追加 Signal Listener Action 前自动保存");
                WebAdminWriteResult result = signalListenerActionsService.addAction(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        listenerId,
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                annotateAutoSnapshotAfterWrite(autoSnapshot, result);
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / POST。");
            return;
        }

        if (parts.length == 3 && "actions".equals(parts[1]) && "clear".equals(parts[2])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminSignalListenerActionRequests.ActionClearRequest request = readJson(exchange, WebAdminSignalListenerActionRequests.ActionClearRequest.class);
            if (request == null) {
                request = new WebAdminSignalListenerActionRequests.ActionClearRequest();
            }
            request.listenerId = listenerId;
            WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS, "Signal Listener", WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, listenerId, "清空 Signal Listener Action 前自动保存");
            WebAdminWriteResult result = signalListenerActionsService.clearActions(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    listenerId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            annotateAutoSnapshotAfterWrite(autoSnapshot, result);
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        if (parts.length == 3 && "actions".equals(parts[1])) {
            if (!method.equalsIgnoreCase("PATCH")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 PATCH。");
                return;
            }
            WebAdminSignalListenerActionRequests.ActionUpdateRequest request = readJson(exchange, WebAdminSignalListenerActionRequests.ActionUpdateRequest.class);
            if (request == null) {
                request = new WebAdminSignalListenerActionRequests.ActionUpdateRequest();
            }
            request.listenerId = listenerId;
            request.actionIndex = decodePathSegment(parts[2]);
            WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS, "Signal Listener", WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, listenerId, "编辑 Signal Listener Action 前自动保存");
            WebAdminWriteResult result = signalListenerActionsService.updateAction(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    listenerId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            annotateAutoSnapshotAfterWrite(autoSnapshot, result);
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        if (parts.length == 4 && "actions".equals(parts[1]) && "delete".equals(parts[3])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminSignalListenerActionRequests.ActionDeleteRequest request = readJson(exchange, WebAdminSignalListenerActionRequests.ActionDeleteRequest.class);
            if (request == null) {
                request = new WebAdminSignalListenerActionRequests.ActionDeleteRequest();
            }
            request.listenerId = listenerId;
            request.actionIndex = decodePathSegment(parts[2]);
            WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_SIGNAL_LISTENER_ACTIONS, "Signal Listener", WebAdminEditLockService.TARGET_SIGNAL_LISTENER_ACTIONS, listenerId, "删除 Signal Listener Action 前自动保存");
            WebAdminWriteResult result = signalListenerActionsService.deleteAction(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    listenerId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            annotateAutoSnapshotAfterWrite(autoSnapshot, result);
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        if (parts.length != 2 || !"delete".equals(parts[1])) {
            WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "Signal Listener 接口不存在。");
            return;
        }
        if (!method.equalsIgnoreCase("POST")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }
        WebAdminSignalListenerDeleteRequest request = readJson(exchange, WebAdminSignalListenerDeleteRequest.class);
        if (request == null) {
            request = new WebAdminSignalListenerDeleteRequest();
        }
        request.listenerId = listenerId;
        WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.DELETE_SIGNAL_LISTENER, "Signal Listener", "signal_listener", listenerId, "删除 Signal Listener 前自动保存");
        WebAdminWriteResult result = signalListenerLifecycleService.delete(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                listenerId,
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        annotateAutoSnapshotAfterWrite(autoSnapshot, result);
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleSignalListenerBasicConfig(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String prefix = "/api/webadmin/signal-listener-basic-config/";
        String listenerRef = decodePathSegment(path.substring(prefix.length()));
        if (listenerRef.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "Listener 引用不能为空。");
            return;
        }
        if (method.equalsIgnoreCase("GET")) {
            var config = signalListenerBasicConfigService.configFor(minecraftServer, auth.user, auth.session, listenerRef);
            if (config == null) {
                WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "Signal Listener 不存在或引用不唯一。");
                return;
            }
            WebAdminJsonResponse.ok(exchange, config);
            return;
        }
        if (!method.equalsIgnoreCase("PATCH")) {
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / PATCH。");
            return;
        }
        WebAdminSignalListenerBasicConfigUpdateRequest request = readJson(exchange, WebAdminSignalListenerBasicConfigUpdateRequest.class);
        if (request == null) {
            request = new WebAdminSignalListenerBasicConfigUpdateRequest();
        }
        request.listenerRef = listenerRef;
        WebAdminSnapshotService.WebAdminSnapshotAutoResult autoSnapshot = autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_SIGNAL_LISTENER_BASIC_CONFIG, "Signal Listener", WebAdminEditLockService.TARGET_SIGNAL_LISTENER_BASIC_CONFIG, listenerRef, "编辑 Signal Listener 基础配置前自动保存");
        WebAdminWriteResult result = signalListenerBasicConfigService.update(
                minecraftServer,
                auth.user,
                auth.session,
                sourceIp(exchange),
                request,
                header(exchange, "X-TZZ-WebAdmin-CSRF"),
                isWriteSameOrigin(exchange)
        );
        annotateAutoSnapshotAfterWrite(autoSnapshot, result);
        WebAdminJsonResponse.ok(exchange, result);
    }

    private void handleRegionControllers(HttpExchange exchange, AuthContext auth, String path, String method) throws IOException {
        String root = "/api/webadmin/region-controllers";
        if (path.equals(root)) {
            if (method.equalsIgnoreCase("GET")) {
                WebAdminJsonResponse.ok(exchange, regionControllerService.listControllers(minecraftServer, auth.user, auth.session));
                return;
            }
            if (method.equalsIgnoreCase("POST")) {
                WebAdminRegionControllerRequests.CreateRequest request = readJson(exchange, WebAdminRegionControllerRequests.CreateRequest.class);
                if (request == null) {
                    request = new WebAdminRegionControllerRequests.CreateRequest();
                }
                autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_REGION, "Region", WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG, safe(request.regionId), "创建 RegionController 前自动保存");
                WebAdminWriteResult result = regionControllerService.create(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / POST。");
            return;
        }

        String prefix = root + "/";
        String tail = path.startsWith(prefix) ? path.substring(prefix.length()) : "";
        if (tail.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "RegionController ID 不能为空。");
            return;
        }
        String[] parts = tail.split("/");
        String controllerId = decodePathSegment(parts[0]);
        if (controllerId.isBlank()) {
            WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "RegionController ID 不能为空。");
            return;
        }

        if (parts.length == 1) {
            if (method.equalsIgnoreCase("GET")) {
                Map<String, Object> data = regionControllerService.controllerFor(minecraftServer, auth.user, auth.session, controllerId);
                if (data == null) {
                    WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "区域控制器不存在。");
                    return;
                }
                WebAdminJsonResponse.ok(exchange, data);
                return;
            }
            if (method.equalsIgnoreCase("PATCH")) {
                WebAdminRegionControllerRequests.UpdateRequest request = readJson(exchange, WebAdminRegionControllerRequests.UpdateRequest.class);
                if (request == null) {
                    request = new WebAdminRegionControllerRequests.UpdateRequest();
                }
                request.controllerId = controllerId;
                autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_REGION, "Region", WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG, controllerId, "编辑 RegionController 前自动保存");
                WebAdminWriteResult result = regionControllerService.update(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        controllerId,
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 GET / PATCH。");
            return;
        }

        if (parts.length == 2 && "delete".equals(parts[1])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            WebAdminRegionControllerRequests.DeleteRequest request = readJson(exchange, WebAdminRegionControllerRequests.DeleteRequest.class);
            if (request == null) {
                request = new WebAdminRegionControllerRequests.DeleteRequest();
            }
            request.controllerId = controllerId;
            autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_REGION, "Region", WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG, controllerId, "删除 RegionController 前自动保存");
            WebAdminWriteResult result = regionControllerService.delete(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    controllerId,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        if (parts.length == 3 && "actions".equals(parts[1])) {
            com.zcpu.tzzmod.region.RegionTriggerType triggerType = parseRegionTriggerType(parts[2]);
            if (triggerType == null) {
                WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "triggerType 只支持 enter / exit / stay。");
                return;
            }
            if (method.equalsIgnoreCase("POST")) {
                WebAdminRegionControllerRequests.ActionAddRequest request = readJson(exchange, WebAdminRegionControllerRequests.ActionAddRequest.class);
                if (request == null) {
                    request = new WebAdminRegionControllerRequests.ActionAddRequest();
                }
                request.controllerId = controllerId;
                request.triggerType = triggerType.name();
                autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_REGION, "Region", WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG, controllerId, "追加 RegionController Action 前自动保存");
                WebAdminWriteResult result = regionControllerService.addAction(
                        minecraftServer,
                        auth.user,
                        auth.session,
                        sourceIp(exchange),
                        controllerId,
                        triggerType,
                        request,
                        header(exchange, "X-TZZ-WebAdmin-CSRF"),
                        isWriteSameOrigin(exchange)
                );
                WebAdminJsonResponse.ok(exchange, result);
                return;
            }
            WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
            return;
        }

        if (parts.length == 4 && "actions".equals(parts[1]) && "clear".equals(parts[3])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            com.zcpu.tzzmod.region.RegionTriggerType triggerType = parseRegionTriggerType(parts[2]);
            if (triggerType == null) {
                WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "triggerType 只支持 enter / exit / stay。");
                return;
            }
            WebAdminRegionControllerRequests.ActionClearRequest request = readJson(exchange, WebAdminRegionControllerRequests.ActionClearRequest.class);
            if (request == null) {
                request = new WebAdminRegionControllerRequests.ActionClearRequest();
            }
            request.controllerId = controllerId;
            request.triggerType = triggerType.name();
            autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_REGION, "Region", WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG, controllerId, "清空 RegionController Action 前自动保存");
            WebAdminWriteResult result = regionControllerService.clearActions(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    controllerId,
                    triggerType,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        if (parts.length == 4 && "actions".equals(parts[1])) {
            if (!method.equalsIgnoreCase("PATCH")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 PATCH。");
                return;
            }
            com.zcpu.tzzmod.region.RegionTriggerType triggerType = parseRegionTriggerType(parts[2]);
            if (triggerType == null) {
                WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "triggerType 只支持 enter / exit / stay。");
                return;
            }
            WebAdminRegionControllerRequests.ActionUpdateRequest request = readJson(exchange, WebAdminRegionControllerRequests.ActionUpdateRequest.class);
            if (request == null) {
                request = new WebAdminRegionControllerRequests.ActionUpdateRequest();
            }
            request.controllerId = controllerId;
            request.triggerType = triggerType.name();
            request.actionIndex = decodePathSegment(parts[3]);
            autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_REGION, "Region", WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG, controllerId, "编辑 RegionController Action 前自动保存");
            WebAdminWriteResult result = regionControllerService.updateAction(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    controllerId,
                    triggerType,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        if (parts.length == 5 && "actions".equals(parts[1]) && "delete".equals(parts[4])) {
            if (!method.equalsIgnoreCase("POST")) {
                WebAdminJsonResponse.error(exchange, 405, "METHOD_NOT_ALLOWED", "该接口只支持 POST。");
                return;
            }
            com.zcpu.tzzmod.region.RegionTriggerType triggerType = parseRegionTriggerType(parts[2]);
            if (triggerType == null) {
                WebAdminJsonResponse.error(exchange, 400, "BAD_REQUEST", "triggerType 只支持 enter / exit / stay。");
                return;
            }
            WebAdminRegionControllerRequests.ActionDeleteRequest request = readJson(exchange, WebAdminRegionControllerRequests.ActionDeleteRequest.class);
            if (request == null) {
                request = new WebAdminRegionControllerRequests.ActionDeleteRequest();
            }
            request.controllerId = controllerId;
            request.triggerType = triggerType.name();
            request.actionIndex = decodePathSegment(parts[3]);
            autoSnapshotBeforeWrite(exchange, auth, WebAdminOperationType.EDIT_REGION, "Region", WebAdminEditLockService.TARGET_REGION_CONTROLLER_CONFIG, controllerId, "删除 RegionController Action 前自动保存");
            WebAdminWriteResult result = regionControllerService.deleteAction(
                    minecraftServer,
                    auth.user,
                    auth.session,
                    sourceIp(exchange),
                    controllerId,
                    triggerType,
                    request,
                    header(exchange, "X-TZZ-WebAdmin-CSRF"),
                    isWriteSameOrigin(exchange)
            );
            WebAdminJsonResponse.ok(exchange, result);
            return;
        }

        WebAdminJsonResponse.error(exchange, 404, "NOT_FOUND", "RegionController 接口不存在。");
    }

    private AuthContext requireAuth(HttpExchange exchange) throws IOException {
        String token = cookie(exchange, WebAdminSessionService.COOKIE_NAME);
        WebAdminSession session = sessionService.get(token).orElse(null);
        if (session == null) {
            WebAdminJsonResponse.error(exchange, 401, "UNAUTHORIZED", "请先登录。");
            return null;
        }
        WebAdminUser user = userService.find(session.username).orElse(null);
        if (user == null || !user.enabled) {
            sessionService.invalidate(token);
            WebAdminJsonResponse.error(exchange, 401, "UNAUTHORIZED", "请先登录。");
            return null;
        }
        return new AuthContext(token, session, user);
    }

    private <T> T readJson(HttpExchange exchange, Class<T> type) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        if (bytes.length == 0) {
            return null;
        }
        return WebAdminJsonResponse.GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), type);
    }

    private static void sendText(HttpExchange exchange, int status, String contentType, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store, max-age=0");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private String sessionCookie(String token, int ttlSeconds) {
        String cookie = WebAdminSessionService.COOKIE_NAME + "=" + token
                + "; Max-Age=" + ttlSeconds
                + "; Path=/; HttpOnly; SameSite=Lax";
        return config.secureCookie ? cookie + "; Secure" : cookie;
    }

    private String clearSessionCookie() {
        String cookie = WebAdminSessionService.COOKIE_NAME + "=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax";
        return config.secureCookie ? cookie + "; Secure" : cookie;
    }

    private static Map<String, Object> userDto(WebAdminUser user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", user.username);
        data.put("displayName", user.displayName);
        data.put("role", user.role);
        return data;
    }

    private static String cookie(HttpExchange exchange, String name) {
        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");
        if (cookieHeaders == null) {
            return "";
        }
        for (String header : cookieHeaders) {
            String[] entries = header.split(";");
            for (String entry : entries) {
                String[] parts = entry.trim().split("=", 2);
                if (parts.length == 2 && parts[0].equals(name)) {
                    return parts[1];
                }
            }
        }
        return "";
    }

    private static String header(HttpExchange exchange, String name) {
        List<String> values = exchange.getRequestHeaders().get(name);
        return values == null || values.isEmpty() ? "" : values.get(0);
    }

    private boolean isWriteSameOrigin(HttpExchange exchange) {
        String origin = header(exchange, "Origin");
        String referer = header(exchange, "Referer");
        if (isBlank(origin) && isBlank(referer)) {
            return true;
        }
        HostPort hostPort = requestHostPort(exchange);
        return writeSecurityService.isSameOriginOrReferer(origin, referer, hostPort.host(), hostPort.port());
    }

    private HostPort requestHostPort(HttpExchange exchange) {
        String hostHeader = header(exchange, "Host");
        if (!isBlank(hostHeader)) {
            String trimmed = hostHeader.trim();
            int colon = trimmed.lastIndexOf(':');
            if (colon > 0 && colon < trimmed.length() - 1) {
                try {
                    return new HostPort(trimmed.substring(0, colon), Integer.parseInt(trimmed.substring(colon + 1)));
                } catch (NumberFormatException ignored) {
                    return new HostPort(trimmed.substring(0, colon), config.port);
                }
            }
            return new HostPort(trimmed, config.port);
        }
        return new HostPort(config.host, config.port);
    }

    private static WebAdminWriteTarget userTarget(String username) {
        String safeUsername = safe(username);
        return new WebAdminWriteTarget("webadmin_user", safeUsername, safeUsername);
    }

    private static WebAdminWriteResult passwordWriteResult(WebAdminWriteTarget target, boolean changed, String message, int invalidatedSessions) {
        return new WebAdminWriteResult(
                true,
                changed ? WebAdminWriteResultCode.OK.id() : WebAdminWriteResultCode.NO_CHANGE.id(),
                isBlank(message) ? (changed ? "密码已更新。" : "密码未变化。") : message,
                target.targetType(),
                target.targetId(),
                changed,
                List.of(),
                "",
                "",
                false,
                Map.of(),
                Map.of("invalidatedSessions", Math.max(0, invalidatedSessions))
        );
    }

    private static void publishUserPasswordRealtime(String action, String username, String actor) {
        WebAdminRealtimeEventBus.publish(WebAdminRealtimeEvent.builder(WebAdminRealtimeEventType.USER_CHANGED)
                .severity("INFO")
                .summary("WebAdmin 用户密码已更新")
                .routeTarget("#/users")
                .payload("action", action)
                .payload("username", safe(username))
                .payload("actor", safe(actor)));
    }

    private WebAdminWriteResult requirePasswordWriteSecurity(HttpExchange exchange, AuthContext auth) {
        if (!isWriteSameOrigin(exchange)) {
            return WebAdminWriteResult.failed(
                    WebAdminWriteResultCode.CSRF_INVALID,
                    WebAdminWriteTarget.none(),
                    "写操作必须来自同源 WebAdmin 页面。"
            );
        }
        WebAdminWriteResult csrf = writeSecurityService.requireValidCsrf(auth.session, header(exchange, "X-TZZ-WebAdmin-CSRF"));
        if (!csrf.success()) {
            return csrf;
        }
        return WebAdminWriteResult.ok(WebAdminWriteTarget.none(), false, "密码写入安全校验通过。");
    }

    private static String decodePathSegment(String value) {
        return URLDecoder.decode((value == null ? "" : value).replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static com.zcpu.tzzmod.region.RegionTriggerType parseRegionTriggerType(String value) {
        String safe = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if ("ENTER".equals(safe)) {
            return com.zcpu.tzzmod.region.RegionTriggerType.ENTER;
        }
        if ("EXIT".equals(safe)) {
            return com.zcpu.tzzmod.region.RegionTriggerType.EXIT;
        }
        if ("STAY".equals(safe)) {
            return com.zcpu.tzzmod.region.RegionTriggerType.STAY;
        }
        return null;
    }

    private static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String part : query.split("&")) {
            if (part.isBlank()) {
                continue;
            }
            String[] pieces = part.split("=", 2);
            String key = URLDecoder.decode(pieces[0], StandardCharsets.UTF_8);
            String value = pieces.length > 1 ? URLDecoder.decode(pieces[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    private static int intQuery(HttpExchange exchange, String name, int fallback) {
        String value = queryParams(exchange).get(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String sourceIp(HttpExchange exchange) {
        return exchange.getRemoteAddress() == null || exchange.getRemoteAddress().getAddress() == null
                ? ""
                : exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private static String normalizePath(String path) {
        return path == null || path.isBlank() ? "/" : path;
    }

    private static String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(Tzz_mod.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class LoginRequest {
        String username;
        String password;
        boolean rememberMe;
    }

    private static final class PasswordChangeRequest {
        String oldPassword;
        String newPassword;
        String confirmPassword;
    }

    private static final class PasswordResetRequest {
        String newPassword;
        String confirmPassword;
    }

    private record AuthContext(String rawToken, WebAdminSession session, WebAdminUser user) {
    }

    private record HostPort(String host, int port) {
    }
}
