package com.zcpu.tzzmod.scheduler;

import com.zcpu.tzzmod.action.ActionConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class TimerStoreTest {
    private TimerStoreTest() {
    }

    public static void run() throws Exception {
        testStoreRoundTripAndBadFileFallback();
        testValidationRejectsUnsafeDefinitions();
        testFingerprintSeparatesConcurrencyFromEditableEquality();
    }

    private static void testStoreRoundTripAndBadFileFallback() throws Exception {
        Path path = Files.createTempDirectory("tzz-timer-store-test").resolve(TimerStore.FILE_NAME);
        TimerDefinition timer = timer("timer.delay", TimerMode.DELAY);
        timer.outputChannel = "timer.done";
        TimerStore.TimerFile file = new TimerStore.TimerFile();
        file.timers.put(timer.id, timer);

        requireTrue(TimerStore.save(path, file), "timer store saves world-scoped timers.json");
        TimerStore.TimerLoadResult loaded = TimerStore.loadWithStatus(path);
        requireFalse(loaded.degraded(), "valid timer store does not degrade");
        requireEquals(1, loaded.file().timers.size(), "timer store roundtrips one timer");
        requireEquals("timer.done", loaded.file().timers.get("timer.delay").outputChannel, "timer outputChannel roundtrips");

        Files.writeString(path, "{not json");
        TimerStore.TimerLoadResult bad = TimerStore.loadWithStatus(path);
        requireTrue(bad.degraded(), "bad timer file degrades safely");
        requireTrue(bad.message().contains("读取失败"), "bad timer file reports Chinese diagnostic");
    }

    private static void testValidationRejectsUnsafeDefinitions() {
        TimerDefinition missingId = timer("", TimerMode.DELAY);
        requireIssue(missingId, "timer_id_required", "missing id rejected");

        TimerDefinition missingName = timer("timer.no-name", TimerMode.DELAY);
        missingName.displayName = "";
        requireIssue(missingName, "timer_name_required", "missing name rejected");

        TimerDefinition repeatNoInterval = timer("timer.repeat.invalid", TimerMode.REPEAT);
        repeatNoInterval.intervalTicks = 0;
        requireIssue(repeatNoInterval, "timer_interval_invalid", "repeat interval 0 rejected");

        TimerDefinition negativeDuration = timer("timer.duration.invalid", TimerMode.COUNTDOWN);
        negativeDuration.durationTicks = -1;
        requireIssue(negativeDuration, "timer_duration_invalid", "negative duration rejected");

        TimerDefinition invalidRuns = timer("timer.runs.invalid", TimerMode.REPEAT);
        invalidRuns.maxRuns = -1;
        requireIssue(invalidRuns, "timer_max_runs_invalid", "negative maxRuns rejected");

        TimerDefinition tooManyActions = timer("timer.too-many-actions", TimerMode.DELAY);
        tooManyActions.onCompleteActions = java.util.Collections.nCopies(TimerValidator.MAX_ACTIONS_PER_LIST + 1, ActionConfig.command("say done", false));
        requireIssue(tooManyActions, "timer_too_many_actions", "too many actions rejected");
    }

    private static void testFingerprintSeparatesConcurrencyFromEditableEquality() {
        TimerDefinition before = timer("timer.fingerprint", TimerMode.DELAY).withWriteMetadata("editor", 1, true);
        TimerDefinition after = timer("timer.fingerprint", TimerMode.DELAY).withWriteMetadata("editor", 2, false);
        requireFalse(TimerStore.fingerprintFor(before).equals(TimerStore.fingerprintFor(after)), "expectedFingerprint includes version for conflict detection");
        requireEquals(TimerStore.editableFingerprintFor(before), TimerStore.editableFingerprintFor(after), "editable fingerprint ignores write metadata version");
    }

    private static void requireIssue(TimerDefinition timer, String code, String message) {
        List<TimerValidationIssue> issues = TimerValidator.validate(timer, true);
        for (TimerValidationIssue issue : issues) {
            if (code.equals(issue.code())) {
                requireTrue(containsChinese(issue.message()), message + " Chinese validation message");
                return;
            }
        }
        throw new AssertionError(message + " missing issue " + code + " issues=" + issues);
    }

    static TimerDefinition timer(String id, TimerMode mode) {
        TimerDefinition timer = new TimerDefinition();
        timer.id = id;
        timer.displayName = id.isBlank() ? "Timer" : id;
        timer.enabled = true;
        timer.mode = mode;
        timer.scopeMode = TimerScopeMode.GLOBAL;
        timer.durationTicks = 40;
        timer.intervalTicks = mode == TimerMode.DELAY ? 0 : 20;
        timer.maxRuns = mode == TimerMode.REPEAT ? 3 : 1;
        timer.startPolicy = TimerStartPolicy.RESTART;
        return timer.normalized();
    }

    private static boolean containsChinese(String value) {
        return value != null && value.chars().anyMatch(ch -> ch >= 0x4E00 && ch <= 0x9FFF);
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
