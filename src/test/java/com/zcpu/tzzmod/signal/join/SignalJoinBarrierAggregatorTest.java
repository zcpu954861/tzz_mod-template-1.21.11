package com.zcpu.tzzmod.signal.join;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SignalJoinBarrierAggregatorTest {
    private SignalJoinBarrierAggregatorTest() {
    }

    public static void run() throws Exception {
        testStoreRoundTripAndBadFileFallback();
        testAllModeResetAfterEmit();
        testAnyNModeUsesDistinctInputChannels();
        testCountModeCountsRepeatedEvents();
        testPlayerScopeIsolationAndMissingContextDiagnostic();
        testLatchUntilManualReset();
        testLazyTimeoutReset();
        testLatchIsNotClearedByLazyTimeout();
        testValidationRejectsUnsafeDefinitions();
    }

    private static void testStoreRoundTripAndBadFileFallback() throws Exception {
        Path dir = Files.createTempDirectory("tzz-signal-join-test");
        Path path = dir.resolve(SignalJoinStore.FILE_NAME);
        SignalJoinDefinition join = join("join.a", SignalJoinMode.ALL, "out.c", "in.a", "in.b");
        SignalJoinStore.SignalJoinFile file = new SignalJoinStore.SignalJoinFile();
        file.joins.put(join.id, join);
        requireTrue(SignalJoinStore.save(path, file), "signal join store saves world-scoped file");
        SignalJoinStore.SignalJoinLoadResult loaded = SignalJoinStore.loadWithStatus(path);
        requireFalse(loaded.degraded(), "valid signal join store does not degrade");
        requireEquals(1, loaded.file().joins.size(), "signal join store roundtrips one join");
        requireEquals("out.c", loaded.file().joins.get("join.a").outputChannel, "signal join output channel roundtrips");

        Files.writeString(path, "{not json");
        SignalJoinStore.SignalJoinLoadResult bad = SignalJoinStore.loadWithStatus(path);
        requireTrue(bad.degraded(), "bad signal join file degrades safely");
        requireTrue(bad.message().contains("读取失败"), "bad signal join file reports Chinese diagnostic");
    }

    private static void testAllModeResetAfterEmit() {
        SignalJoinRuntimeService.TestRuntime runtime = SignalJoinRuntimeService.testRuntime();
        SignalJoinDefinition join = join("join.all", SignalJoinMode.ALL, "out.c", "in.a", "in.b");
        requireTrue(runtime.observe(List.of(join), "in.a", "", 10L).isEmpty(), "ALL A only does not output");
        requireTrue(runtime.observe(List.of(join), "in.a", "", 11L).isEmpty(), "ALL duplicate A does not output");
        requireEquals(List.of("out.c"), runtime.observe(List.of(join), "in.b", "", 12L), "ALL A then B outputs once");
        SignalJoinStatusSnapshot status = runtime.status(join, 13L);
        requireEquals(0, status.pendingScopeCount(), "RESET_AFTER_EMIT clears active pending scope after output");
        requireEquals("OUTPUT_RESET", status.scopes().get(0).get("lastResult"), "RESET_AFTER_EMIT records output reset result");
        requireTrue(runtime.observe(List.of(join), "in.b", "", 14L).isEmpty(), "ALL starts a fresh window after reset");
        requireEquals(List.of("out.c"), runtime.observe(List.of(join), "in.a", "", 15L), "ALL fresh B then A outputs again");
    }

    private static void testAnyNModeUsesDistinctInputChannels() {
        SignalJoinRuntimeService.TestRuntime runtime = SignalJoinRuntimeService.testRuntime();
        SignalJoinDefinition join = join("join.any", SignalJoinMode.ANY_N, "out.any", "in.a", "in.b", "in.c");
        join.threshold = 2;
        requireTrue(runtime.observe(List.of(join), "in.a", "", 20L).isEmpty(), "ANY_N one distinct input does not output");
        requireTrue(runtime.observe(List.of(join), "in.a", "", 21L).isEmpty(), "ANY_N duplicate same input does not satisfy distinct threshold");
        requireEquals(List.of("out.any"), runtime.observe(List.of(join), "in.c", "", 22L), "ANY_N second distinct input outputs");
    }

    private static void testCountModeCountsRepeatedEvents() {
        SignalJoinRuntimeService.TestRuntime runtime = SignalJoinRuntimeService.testRuntime();
        SignalJoinDefinition join = join("join.count", SignalJoinMode.COUNT, "out.count", "in.a", "in.b");
        join.threshold = 3;
        requireTrue(runtime.observe(List.of(join), "in.a", "", 30L).isEmpty(), "COUNT first event does not output");
        requireTrue(runtime.observe(List.of(join), "in.a", "", 31L).isEmpty(), "COUNT second repeated event does not output below threshold");
        requireEquals(List.of("out.count"), runtime.observe(List.of(join), "in.a", "", 32L), "COUNT repeated events count toward threshold");
    }

    private static void testPlayerScopeIsolationAndMissingContextDiagnostic() {
        SignalJoinRuntimeService.TestRuntime runtime = SignalJoinRuntimeService.testRuntime();
        SignalJoinDefinition join = join("join.player", SignalJoinMode.ALL, "out.player", "in.a", "in.b");
        join.scopeMode = SignalJoinScopeMode.PLAYER;
        requireTrue(runtime.observe(List.of(join), "in.a", "player-1", 40L).isEmpty(), "PLAYER player-1 A does not output alone");
        requireTrue(runtime.observe(List.of(join), "in.b", "player-2", 41L).isEmpty(), "PLAYER player-2 B does not combine with player-1 A");
        requireEquals(List.of("out.player"), runtime.observe(List.of(join), "in.b", "player-1", 42L), "PLAYER same player A+B outputs");
        requireTrue(runtime.observe(List.of(join), "in.a", "", 43L).isEmpty(), "PLAYER signal without player context is ignored");
        requireTrue(runtime.status(join, 44L).lastFailureReason().contains("PLAYER scope 需要玩家上下文"), "PLAYER missing context records Chinese diagnostic");
    }

    private static void testLatchUntilManualReset() {
        SignalJoinRuntimeService.TestRuntime runtime = SignalJoinRuntimeService.testRuntime();
        SignalJoinDefinition join = join("join.latch", SignalJoinMode.ALL, "out.latch", "in.a", "in.b");
        join.resetPolicy = SignalJoinResetPolicy.LATCH_UNTIL_MANUAL_RESET;
        runtime.observe(List.of(join), "in.a", "", 50L);
        requireEquals(List.of("out.latch"), runtime.observe(List.of(join), "in.b", "", 51L), "latch emits first satisfied output");
        requireTrue(runtime.observe(List.of(join), "in.a", "", 52L).isEmpty(), "latched join does not repeat before manual reset");
        requireEquals(1, runtime.reset("join.latch", ""), "manual reset clears latched runtime state");
        runtime.observe(List.of(join), "in.b", "", 53L);
        requireEquals(List.of("out.latch"), runtime.observe(List.of(join), "in.a", "", 54L), "manual reset allows latch to emit again");
    }

    private static void testLazyTimeoutReset() {
        SignalJoinRuntimeService.TestRuntime runtime = SignalJoinRuntimeService.testRuntime();
        SignalJoinDefinition join = join("join.timeout", SignalJoinMode.ALL, "out.timeout", "in.a", "in.b");
        join.timeoutTicks = 5L;
        runtime.observe(List.of(join), "in.a", "", 60L);
        requireEquals(1, runtime.status(join, 64L).pendingScopeCount(), "pending state remains before timeout");
        SignalJoinStatusSnapshot expired = runtime.status(join, 66L);
        requireEquals(0, expired.pendingScopeCount(), "status query lazily resets expired pending state");
        requireTrue(String.valueOf(expired.scopes().get(0).get("lastFailureReason")).contains("超时"), "lazy timeout records Chinese diagnostic");
        requireTrue(runtime.observe(List.of(join), "in.b", "", 67L).isEmpty(), "post-timeout next input starts a fresh window");
    }

    private static void testLatchIsNotClearedByLazyTimeout() {
        SignalJoinRuntimeService.TestRuntime runtime = SignalJoinRuntimeService.testRuntime();
        SignalJoinDefinition join = join("join.latch.timeout", SignalJoinMode.ALL, "out.latch.timeout", "in.a", "in.b");
        join.resetPolicy = SignalJoinResetPolicy.LATCH_UNTIL_MANUAL_RESET;
        join.timeoutTicks = 5L;
        runtime.observe(List.of(join), "in.a", "", 70L);
        requireEquals(List.of("out.latch.timeout"), runtime.observe(List.of(join), "in.b", "", 71L), "latch timeout fixture emits once");
        SignalJoinStatusSnapshot status = runtime.status(join, 100L);
        requireEquals(1, status.pendingScopeCount(), "lazy timeout does not clear latched scope");
        requireTrue(Boolean.TRUE.equals(status.scopes().get(0).get("latched")), "latched scope remains latched after timeout query");
        requireTrue(runtime.observe(List.of(join), "in.a", "", 101L).isEmpty(), "latched join still does not repeat after timeout");
    }

    private static void testValidationRejectsUnsafeDefinitions() {
        SignalJoinDefinition duplicate = join("join.invalid", SignalJoinMode.ALL, "out.invalid", "in.a", "in.a");
        requireTrue(hasIssue(duplicate, "signal_join_input_channel_duplicate"), "validator rejects duplicate input channels");

        SignalJoinDefinition self = join("join.self", SignalJoinMode.ALL, "in.a", "in.a", "in.b");
        requireTrue(hasIssue(self, "signal_join_output_equals_input"), "validator rejects output equal input");

        SignalJoinDefinition any = join("join.any.invalid", SignalJoinMode.ANY_N, "out.any", "in.a", "in.b");
        any.threshold = 3;
        requireTrue(hasIssue(any, "signal_join_any_n_threshold_invalid"), "validator rejects ANY_N threshold greater than input count");

        SignalJoinDefinition anyZero = join("join.any.zero", SignalJoinMode.ANY_N, "out.any", "in.a", "in.b");
        anyZero.threshold = 0;
        requireTrue(hasIssue(anyZero, "signal_join_any_n_threshold_invalid"), "validator rejects ANY_N non-positive raw threshold");

        SignalJoinDefinition countZero = join("join.count.zero", SignalJoinMode.COUNT, "out.count", "in.a", "in.b");
        countZero.threshold = 0;
        requireTrue(hasIssue(countZero, "signal_join_count_threshold_invalid"), "validator rejects COUNT non-positive raw threshold");

        SignalJoinDefinition negativeTimeout = join("join.timeout.invalid", SignalJoinMode.ALL, "out.timeout", "in.a", "in.b");
        negativeTimeout.timeoutTicks = -1L;
        requireTrue(hasIssue(negativeTimeout, "signal_join_timeout_invalid"), "validator rejects negative timeoutTicks before normalization");
    }

    private static boolean hasIssue(SignalJoinDefinition join, String code) {
        return SignalJoinValidator.validate(join, true).stream().anyMatch(issue -> code.equals(issue.code()));
    }

    private static SignalJoinDefinition join(String id, SignalJoinMode mode, String output, String... inputs) {
        SignalJoinDefinition join = new SignalJoinDefinition();
        join.id = id;
        join.displayName = id;
        join.enabled = true;
        join.mode = mode;
        join.outputChannel = output;
        join.inputChannels = java.util.Arrays.stream(inputs)
                .map(channel -> new SignalJoinInputDefinition(channel, "", "", 1))
                .toList();
        join.threshold = mode == SignalJoinMode.ALL ? inputs.length : 2;
        return join.normalized();
    }

    private static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireFalse(boolean condition, String message) {
        requireTrue(!condition, message);
    }

    private static void requireEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
