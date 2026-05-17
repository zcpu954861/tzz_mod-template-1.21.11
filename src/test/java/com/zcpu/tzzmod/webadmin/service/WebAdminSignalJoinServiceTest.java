package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.signal.join.SignalJoinInputDefinition;
import com.zcpu.tzzmod.webadmin.WebAdminRole;
import com.zcpu.tzzmod.webadmin.WebAdminSession;
import com.zcpu.tzzmod.webadmin.WebAdminUser;
import com.zcpu.tzzmod.webadmin.dto.WebAdminSignalJoinRequest;
import com.zcpu.tzzmod.webadmin.write.WebAdminPermissionService;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteResult;
import com.zcpu.tzzmod.webadmin.write.WebAdminWriteSecurityService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WebAdminSignalJoinServiceTest {
    private WebAdminSignalJoinServiceTest() {
    }

    public static void run() throws Exception {
        testCreateAcceptsInternalPayloadValues();
        testCreateRejectsUnsafeRawRequestValues();
        testRejectsDisplayLabelsAsPayloadValues();
        testResetRequiresExpectedFingerprintAndConfirmation();
        testViewerCannotWriteSignalJoin();
    }

    private static void testCreateAcceptsInternalPayloadValues() throws Exception {
        Fixture fixture = fixture();
        WebAdminSignalJoinRequest global = validRequest("join.global");
        global.mode = "ALL";
        global.scopeMode = "GLOBAL";
        global.resetPolicy = "RESET_AFTER_EMIT";
        requireTrue(fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", global, fixture.csrf, true).success(), "create accepts internal GLOBAL payload");

        WebAdminSignalJoinRequest player = validRequest("join.player");
        player.mode = "ANY_N";
        player.threshold = 1;
        player.scopeMode = "PLAYER";
        player.resetPolicy = "LATCH_UNTIL_MANUAL_RESET";
        requireTrue(fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", player, fixture.csrf, true).success(), "create accepts internal PLAYER payload");

        Map<?, ?> detail = fixture.service.detail(null, fixture.editor, fixture.session, "join.player");
        requireTrue("ANY_N".equals(string(detail.get("mode"))), "mode is stored as internal ANY_N value");
        requireTrue("PLAYER".equals(string(detail.get("scopeMode"))), "scopeMode is stored as internal PLAYER value");
        requireTrue("LATCH_UNTIL_MANUAL_RESET".equals(string(detail.get("resetPolicy"))), "resetPolicy is stored as internal value");
    }

    private static void testCreateRejectsUnsafeRawRequestValues() throws Exception {
        Fixture fixture = fixture();
        WebAdminSignalJoinRequest threshold = validRequest("join.threshold");
        threshold.mode = "ANY_N";
        threshold.threshold = 0;
        requireFalse(fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", threshold, fixture.csrf, true).success(), "ANY_N threshold 0 is rejected");

        WebAdminSignalJoinRequest mode = validRequest("join.mode");
        mode.mode = "BOGUS";
        WebAdminWriteResult modeResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", mode, fixture.csrf, true);
        requireFalse(modeResult.success(), "invalid mode is rejected");
        requireTrue(validationMessage(modeResult).contains("模式必须是 ALL、ANY_N 或 COUNT"), "invalid mode reports Chinese validation message");

        WebAdminSignalJoinRequest timeout = validRequest("join.timeout");
        timeout.timeoutTicks = -1L;
        requireFalse(fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", timeout, fixture.csrf, true).success(), "negative timeoutTicks is rejected");

        WebAdminSignalJoinRequest cooldown = validRequest("join.cooldown");
        cooldown.cooldownTicks = -1L;
        requireFalse(fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", cooldown, fixture.csrf, true).success(), "negative cooldownTicks is rejected");
    }

    private static void testRejectsDisplayLabelsAsPayloadValues() throws Exception {
        Fixture fixture = fixture();
        WebAdminSignalJoinRequest mode = validRequest("join.mode-label");
        mode.mode = "ALL：所有输入均到达";
        WebAdminWriteResult modeResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", mode, fixture.csrf, true);
        requireFalse(modeResult.success(), "mode display label is rejected");
        requireTrue(validationMessage(modeResult).contains("模式必须是 ALL、ANY_N 或 COUNT"), "mode display label reports Chinese validation");

        WebAdminSignalJoinRequest scope = validRequest("join.scope-label");
        scope.scopeMode = "GLOBAL：全局共享";
        WebAdminWriteResult scopeResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", scope, fixture.csrf, true);
        requireFalse(scopeResult.success(), "scopeMode display label is rejected");
        requireTrue(validationMessage(scopeResult).contains("作用域必须选择 GLOBAL 或 PLAYER"), "scopeMode display label reports Chinese validation");

        WebAdminSignalJoinRequest reset = validRequest("join.reset-label");
        reset.resetPolicy = "输出后清空，可重复触发";
        WebAdminWriteResult resetResult = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", reset, fixture.csrf, true);
        requireFalse(resetResult.success(), "resetPolicy display label is rejected");
        requireTrue(validationMessage(resetResult).contains("重置策略必须是 RESET_AFTER_EMIT 或 LATCH_UNTIL_MANUAL_RESET"), "resetPolicy display label reports Chinese validation");
    }

    private static void testResetRequiresExpectedFingerprintAndConfirmation() throws Exception {
        Fixture fixture = fixture();
        WebAdminSignalJoinRequest create = validRequest("join.reset");
        WebAdminWriteResult created = fixture.service.create(null, fixture.editor, fixture.session, "127.0.0.1", create, fixture.csrf, true);
        requireTrue(created.success(), "signal join create succeeds before reset checks");
        String fingerprint = string(((Map<?, ?>) fixture.service.detail(null, fixture.editor, fixture.session, "join.reset")).get("expectedFingerprint"));

        WebAdminSignalJoinRequest missingConfirmation = new WebAdminSignalJoinRequest();
        missingConfirmation.expectedFingerprint = fingerprint;
        WebAdminWriteResult missingConfirmationResult = fixture.service.reset(null, fixture.editor, fixture.session, "127.0.0.1", "join.reset", missingConfirmation, fixture.csrf, true);
        requireFalse(missingConfirmationResult.success(), "reset requires explicit confirmation");

        WebAdminSignalJoinRequest missingFingerprint = new WebAdminSignalJoinRequest();
        missingFingerprint.confirmed = true;
        WebAdminWriteResult missingFingerprintResult = fixture.service.reset(null, fixture.editor, fixture.session, "127.0.0.1", "join.reset", missingFingerprint, fixture.csrf, true);
        requireFalse(missingFingerprintResult.success(), "reset requires expectedFingerprint");

        WebAdminSignalJoinRequest reset = new WebAdminSignalJoinRequest();
        reset.confirmed = true;
        reset.expectedFingerprint = fingerprint;
        WebAdminWriteResult resetResult = fixture.service.reset(null, fixture.editor, fixture.session, "127.0.0.1", "join.reset", reset, fixture.csrf, true);
        requireTrue(resetResult.success(), "reset succeeds with confirmation and matching fingerprint");
    }

    private static void testViewerCannotWriteSignalJoin() throws Exception {
        Fixture fixture = fixture();
        WebAdminUser viewer = user(WebAdminRole.VIEWER);
        WebAdminSession viewerSession = session(viewer);
        String viewerCsrf = fixture.security.csrfTokenFor(viewerSession);
        WebAdminWriteResult result = fixture.service.create(null, viewer, viewerSession, "127.0.0.1", validRequest("join.viewer"), viewerCsrf, true);
        requireFalse(result.success(), "VIEWER cannot create signal joins");
    }

    private static Fixture fixture() throws Exception {
        Path path = Files.createTempDirectory("tzz-signal-join-service-test").resolve("signal_joins.json");
        WebAdminWriteSecurityService security = new WebAdminWriteSecurityService();
        WebAdminSignalJoinService service = new WebAdminSignalJoinService(new WebAdminPermissionService(), security, null, path);
        WebAdminUser editor = user(WebAdminRole.EDITOR);
        WebAdminSession session = session(editor);
        return new Fixture(service, security, editor, session, security.csrfTokenFor(session));
    }

    private static WebAdminSignalJoinRequest validRequest(String id) {
        WebAdminSignalJoinRequest request = new WebAdminSignalJoinRequest();
        request.id = id;
        request.displayName = id;
        request.enabled = true;
        request.mode = "ALL";
        request.threshold = 2;
        request.scopeMode = "GLOBAL";
        request.resetPolicy = "RESET_AFTER_EMIT";
        request.inputChannels = List.of(
                new SignalJoinInputDefinition("in.a", "", "", 1),
                new SignalJoinInputDefinition("in.b", "", "", 1)
        );
        request.outputChannel = "out.c";
        return request;
    }

    private static WebAdminUser user(WebAdminRole role) {
        WebAdminUser user = new WebAdminUser();
        user.username = role.id().toLowerCase(Locale.ROOT);
        user.displayName = role.displayName();
        user.role = role.id();
        return user.normalized();
    }

    private static WebAdminSession session(WebAdminUser user) {
        return new WebAdminSession("session-" + user.username, user.username, user.role, 1L, 100000L, "127.0.0.1", "test");
    }

    private static String validationMessage(WebAdminWriteResult result) {
        return result.validationErrors().isEmpty() ? result.message() : result.validationErrors().getFirst().message();
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireFalse(boolean condition, String message) {
        requireTrue(!condition, message);
    }

    private record Fixture(
            WebAdminSignalJoinService service,
            WebAdminWriteSecurityService security,
            WebAdminUser editor,
            WebAdminSession session,
            String csrf
    ) {
    }
}
