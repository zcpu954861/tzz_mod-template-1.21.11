package com.zcpu.tzzmod.scheduler;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import java.util.Map;

public final class TimerRuntimeServiceTest {
    private TimerRuntimeServiceTest() {
    }

    public static void run() {
        testDelayCountdownAndRepeatRuntime();
        testCancelBeforeCompleteAndInfiniteRepeatUntilCancel();
        testStartPolicies();
        testScopeIsolationAndReset();
        testPlayerScopeRequiresContextAndRepeatStatus();
        testDelayCompleteActionsOnlyRunAtCompletionOnce();
        testActionFailureStatusAndDueBudget();
        testStartAndCancelBucketHarnessCoverage();
        testActiveLimitStillAllowsExistingScopePolicies();
    }

    private static void testDelayCountdownAndRepeatRuntime() {
        TimerRuntimeService.TestRuntime delayRuntime = TimerRuntimeService.testRuntime();
        TimerDefinition delay = timer("timer.delay", TimerMode.DELAY);
        delay.durationTicks = 40;
        requireSuccess(delayRuntime.start(delay, "global", 100), "start delay timer");
        delayRuntime.tick(139);
        requireEquals(1, delayRuntime.activeCount(), "delay remains active before duration");
        delayRuntime.tick(140);
        requireEquals(0, delayRuntime.activeCount(), "delay completes after duration");
        requireEquals(1, delayRuntime.completedCount(), "delay completes once");

        TimerRuntimeService.TestRuntime countdownRuntime = TimerRuntimeService.testRuntime();
        TimerDefinition countdown = timer("timer.countdown", TimerMode.COUNTDOWN);
        countdown.durationTicks = 60;
        countdown.intervalTicks = 20;
        requireSuccess(countdownRuntime.start(countdown, "global", 200), "start countdown timer");
        countdownRuntime.tick(220);
        requireEquals(1, countdownRuntime.tickCount(), "countdown onTick executes by interval");
        countdownRuntime.tick(260);
        requireEquals(1, countdownRuntime.completedCount(), "countdown completes at duration");

        TimerRuntimeService.TestRuntime repeatRuntime = TimerRuntimeService.testRuntime();
        TimerDefinition repeat = timer("timer.repeat", TimerMode.REPEAT);
        repeat.intervalTicks = 5;
        repeat.maxRuns = 3;
        requireSuccess(repeatRuntime.start(repeat, "global", 300), "start repeat timer");
        repeatRuntime.tick(305);
        repeatRuntime.tick(310);
        requireEquals(2, repeatRuntime.tickCount(), "repeat ticks before maxRuns");
        requireEquals(1, repeatRuntime.activeCount(), "repeat remains active before maxRuns");
        repeatRuntime.tick(315);
        requireEquals(3, repeatRuntime.tickCount(), "repeat third tick executes");
        requireEquals(1, repeatRuntime.completedCount(), "repeat completes after maxRuns");
        requireEquals(0, repeatRuntime.activeCount(), "repeat maxRuns clears active instance");
    }

    private static void testCancelBeforeCompleteAndInfiniteRepeatUntilCancel() {
        TimerRuntimeService.TestRuntime delayRuntime = TimerRuntimeService.testRuntime();
        TimerDefinition delay = timer("timer.cancel-before-complete", TimerMode.DELAY);
        delay.durationTicks = 20;
        requireSuccess(delayRuntime.start(delay, "global", 10), "start delay before cancel");
        TimerOperationResult cancelled = delayRuntime.cancel("timer.cancel-before-complete", "global");
        requireTrue(cancelled.success(), "cancel before complete succeeds");
        requireTrue(cancelled.changed(), "cancel before complete changes runtime");
        delayRuntime.tick(40);
        requireEquals(0, delayRuntime.completedCount(), "cancel before complete prevents onComplete");
        requireEquals(0, delayRuntime.activeCount(), "cancel before complete clears active instance");

        TimerRuntimeService.TestRuntime repeatRuntime = TimerRuntimeService.testRuntime();
        TimerDefinition repeat = timer("timer.infinite-repeat", TimerMode.REPEAT);
        repeat.intervalTicks = 5;
        repeat.maxRuns = 0;
        requireSuccess(repeatRuntime.start(repeat, "global", 100), "start infinite repeat");
        repeatRuntime.tick(105);
        repeatRuntime.tick(110);
        repeatRuntime.tick(115);
        requireEquals(3, repeatRuntime.tickCount(), "infinite repeat ticks until cancel");
        requireEquals(1, repeatRuntime.activeCount(), "infinite repeat remains active before cancel");
        repeatRuntime.cancel("timer.infinite-repeat", "global");
        repeatRuntime.tick(120);
        requireEquals(3, repeatRuntime.tickCount(), "cancel stops infinite repeat ticks");
        requireEquals(0, repeatRuntime.activeCount(), "cancel clears infinite repeat");
    }

    private static void testStartPolicies() {
        TimerRuntimeService.TestRuntime runtime = TimerRuntimeService.testRuntime();
        TimerDefinition restart = timer("timer.policy", TimerMode.DELAY);
        restart.startPolicy = TimerStartPolicy.RESTART;
        requireTrue(runtime.start(restart, "global", 10).changed(), "RESTART first start changes runtime");
        requireTrue(runtime.start(restart, "global", 20).changed(), "RESTART replaces running timer");
        requireEquals(1, runtime.activeCount(), "RESTART keeps one active scope");

        TimerDefinition ignore = timer("timer.policy", TimerMode.DELAY);
        ignore.startPolicy = TimerStartPolicy.IGNORE_IF_RUNNING;
        TimerOperationResult ignored = runtime.start(ignore, "global", 30);
        requireTrue(ignored.success(), "IGNORE_IF_RUNNING is success");
        requireFalse(ignored.changed(), "IGNORE_IF_RUNNING keeps existing timer");
        requireTrue(ignored.message().contains("忽略"), "IGNORE_IF_RUNNING reports Chinese no-op reason");

        TimerDefinition fail = timer("timer.policy", TimerMode.DELAY);
        fail.startPolicy = TimerStartPolicy.FAIL_IF_RUNNING;
        TimerOperationResult failed = runtime.start(fail, "global", 40);
        requireFalse(failed.success(), "FAIL_IF_RUNNING fails when already running");
        requireTrue(failed.message().contains("失败"), "FAIL_IF_RUNNING reports Chinese failure reason");
    }

    private static void testScopeIsolationAndReset() {
        TimerRuntimeService.TestRuntime runtime = TimerRuntimeService.testRuntime();
        TimerDefinition player = timer("timer.player", TimerMode.DELAY);
        player.scopeMode = TimerScopeMode.PLAYER;
        requireSuccess(runtime.start(player, "player-a", 10), "PLAYER timer starts for player-a");
        requireSuccess(runtime.start(player, "player-b", 10), "PLAYER timer starts for player-b");
        requireEquals(2, runtime.activeCount(), "PLAYER scope isolates active instances by scope key");
        requireEquals(1, runtime.reset("timer.player", "player-a"), "reset one player scope");
        requireEquals(1, runtime.activeCount(), "reset one player does not cancel another");
        requireEquals(1, runtime.reset("timer.player", ""), "reset timer clears remaining player scopes");
        requireEquals(0, runtime.activeCount(), "reset all clears timer runtime state");
    }

    private static void testPlayerScopeRequiresContextAndRepeatStatus() {
        TimerRuntimeService.TestRuntime runtime = TimerRuntimeService.testRuntime();
        TimerDefinition player = timer("timer.player-context", TimerMode.DELAY);
        player.scopeMode = TimerScopeMode.PLAYER;
        TimerOperationResult missingPlayer = runtime.start(player, "", 10);
        requireFalse(missingPlayer.success(), "PLAYER timer without context fails");
        requireTrue(missingPlayer.message().contains("PLAYER scope Timer"), "missing player failure is Chinese and explicit");

        TimerDefinition repeat = timer("timer.repeat-status", TimerMode.REPEAT);
        repeat.intervalTicks = 5;
        repeat.maxRuns = 3;
        requireSuccess(runtime.start(repeat, "global", 100), "start repeat for status");
        Map<String, Object> status = runtime.status(repeat, 103).instances().getFirst();
        requireEquals(2L, status.get("remainingTicks"), "repeat remainingTicks reports next fire distance");
        requireEquals(2L, status.get("nextFireInTicks"), "repeat nextFireInTicks explicit");
        requireEquals(3L, status.get("remainingRuns"), "repeat remainingRuns before first tick");

        TimerDefinition infinite = timer("timer.repeat-status-infinite", TimerMode.REPEAT);
        infinite.maxRuns = 0;
        requireSuccess(runtime.start(infinite, "global", 200), "start infinite repeat for status");
        Map<String, Object> infiniteStatus = runtime.status(infinite, 200).instances().getFirst();
        requireEquals(-1L, infiniteStatus.get("remainingRuns"), "infinite repeat remainingRuns is -1");
    }

    private static void testActionFailureStatusAndDueBudget() {
        TimerRuntimeService.TestRuntime completeRuntime = TimerRuntimeService.testRuntime();
        TimerDefinition completeFailure = timer("timer.complete-failure", TimerMode.DELAY);
        completeFailure.durationTicks = 5;
        completeFailure.onCompleteActions = java.util.List.of(messageAction("complete without server"));
        requireSuccess(completeRuntime.start(completeFailure, "global", 10), "start complete failure timer");
        completeRuntime.tickActual(15);
        TimerStatusSnapshot completeStatus = completeRuntime.status(completeFailure, 15);
        requireEquals("COMPLETE_ACTION_FAILED", completeStatus.lastResult(), "onComplete failure is recorded");
        requireTrue(completeStatus.lastFailureReason().contains("Timer action 执行上下文为空"), "onComplete failure reason is Chinese");

        TimerRuntimeService.TestRuntime tickRuntime = TimerRuntimeService.testRuntime();
        TimerDefinition tickFailure = timer("timer.tick-failure", TimerMode.COUNTDOWN);
        tickFailure.durationTicks = 20;
        tickFailure.intervalTicks = 5;
        tickFailure.onTickActions = java.util.List.of(messageAction("tick without server"));
        requireSuccess(tickRuntime.start(tickFailure, "global", 10), "start tick failure timer");
        tickRuntime.tickActual(15);
        TimerStatusSnapshot tickStatus = tickRuntime.status(tickFailure, 15);
        requireEquals("TICK_ACTION_FAILED", tickStatus.lastResult(), "onTick failure is recorded");
        requireTrue(tickStatus.lastFailureReason().contains("Timer action 执行上下文为空"), "onTick failure reason is Chinese");

        TimerRuntimeService.TestRuntime outputRuntime = TimerRuntimeService.testRuntime();
        TimerDefinition outputFailure = timer("timer.output-failure", TimerMode.DELAY);
        outputFailure.durationTicks = 5;
        outputFailure.outputChannel = "timer.output";
        requireSuccess(outputRuntime.start(outputFailure, "global", 10), "start output failure timer");
        outputRuntime.tickActual(15);
        TimerStatusSnapshot outputStatus = outputRuntime.status(outputFailure, 15);
        requireEquals("OUTPUT_FAILED", outputStatus.lastResult(), "outputChannel failure is recorded");
        requireTrue(outputStatus.lastFailureReason().contains("outputChannel"), "output failure reason mentions outputChannel");

        TimerRuntimeService.TestRuntime budgetRuntime = TimerRuntimeService.testRuntime();
        for (int index = 0; index < 129; index++) {
            TimerDefinition timer = timer("timer.budget." + index, TimerMode.REPEAT);
            timer.intervalTicks = 5;
            timer.maxRuns = 1;
            requireSuccess(budgetRuntime.start(timer, "global", 0), "start due budget timer " + index);
        }
        budgetRuntime.tickActual(5);
        requireEquals(1, budgetRuntime.activeCount(), "due budget defers final repeat beyond max per tick");
        budgetRuntime.tickActual(6);
        requireEquals(0, budgetRuntime.activeCount(), "deferred due timer completes on later tick");
    }

    private static void testDelayCompleteActionsOnlyRunAtCompletionOnce() {
        TimerRuntimeService.TestRuntime runtime = TimerRuntimeService.testRuntime();
        TimerDefinition timer = timer("timer.complete-timing", TimerMode.DELAY);
        timer.durationTicks = 5;
        timer.onCompleteActions = java.util.List.of(messageAction("delay complete"));
        requireSuccess(runtime.start(timer, "global", 10), "start delay complete timing timer");

        TimerStatusSnapshot started = runtime.status(timer, 10);
        requireEquals(1, started.activeInstanceCount(), "manual/runtime start creates active timer");
        requireFalse("COMPLETE_ACTION_FAILED".equals(started.lastResult()), "onCompleteActions do not run at start");

        runtime.tickActual(14);
        TimerStatusSnapshot beforeDue = runtime.status(timer, 14);
        requireEquals(1, beforeDue.activeInstanceCount(), "delay timer remains active before duration");
        requireFalse("COMPLETE_ACTION_FAILED".equals(beforeDue.lastResult()), "onCompleteActions do not run before deadline");

        runtime.tickActual(15);
        TimerStatusSnapshot completed = runtime.status(timer, 15);
        requireEquals(0, completed.activeInstanceCount(), "delay timer clears active instance at completion");
        requireEquals("COMPLETE_ACTION_FAILED", completed.lastResult(), "onCompleteActions execute at completion");
        requireTrue(completed.lastFailureReason().contains("Timer action 执行上下文为空"), "complete action failure reason is Chinese");

        runtime.tickActual(16);
        TimerStatusSnapshot afterExtraTick = runtime.status(timer, 16);
        requireEquals(0, afterExtraTick.activeInstanceCount(), "completed delay timer does not reactivate");
        requireEquals("COMPLETE_ACTION_FAILED", afterExtraTick.lastResult(), "complete result is not replaced by a second execution");
    }

    private static void testStartAndCancelBucketHarnessCoverage() {
        TimerRuntimeService.TestRuntime runtime = TimerRuntimeService.testRuntime();
        TimerDefinition timer = timer("timer.start-cancel-actions", TimerMode.DELAY);
        timer.onStartActions = java.util.List.of(messageAction("start action"));
        timer.onCancelActions = java.util.List.of(messageAction("cancel action"));

        requireSuccess(runtime.start(timer, "global", 10), "start timer with onStart action bucket");
        requireEquals(1, runtime.startCount(), "test runtime tracks onStart action bucket execution");
        TimerOperationResult cancelled = runtime.cancel("timer.start-cancel-actions", "global");
        requireTrue(cancelled.success(), "cancel timer with onCancel action bucket");
        requireTrue(cancelled.changed(), "cancel with active timer changes runtime");
        requireEquals(1, runtime.cancelCount(), "test runtime tracks onCancel action bucket execution");
        runtime.tick(40);
        requireEquals(0, runtime.completedCount(), "cancel action bucket does not turn into completion");
    }

    private static void testActiveLimitStillAllowsExistingScopePolicies() {
        TimerRuntimeService.TestRuntime runtime = TimerRuntimeService.testRuntime();
        for (int index = 0; index < TimerRuntimeService.MAX_ACTIVE_TIMERS_PER_SERVER; index++) {
            TimerDefinition timer = timer("timer.cap." + index, TimerMode.DELAY);
            requireSuccess(runtime.start(timer, "scope-" + index, 1), "start timer to active limit " + index);
        }
        requireEquals(TimerRuntimeService.MAX_ACTIVE_TIMERS_PER_SERVER, runtime.activeCount(), "test runtime reaches active limit");

        TimerDefinition newTimer = timer("timer.cap.new", TimerMode.DELAY);
        TimerOperationResult overLimit = runtime.start(newTimer, "new-scope", 2);
        requireFalse(overLimit.success(), "new timer over active limit fails");
        requireTrue(overLimit.message().contains("上限"), "active limit failure is Chinese");

        TimerDefinition ignoreExisting = timer("timer.cap.0", TimerMode.DELAY);
        ignoreExisting.startPolicy = TimerStartPolicy.IGNORE_IF_RUNNING;
        TimerOperationResult ignored = runtime.start(ignoreExisting, "scope-0", 3);
        requireTrue(ignored.success(), "IGNORE_IF_RUNNING existing scope still succeeds at active limit");
        requireFalse(ignored.changed(), "IGNORE_IF_RUNNING existing scope remains no-op at active limit");

        TimerDefinition restartExisting = timer("timer.cap.0", TimerMode.DELAY);
        restartExisting.startPolicy = TimerStartPolicy.RESTART;
        TimerOperationResult restarted = runtime.start(restartExisting, "scope-0", 4);
        requireTrue(restarted.success(), "RESTART existing scope still succeeds at active limit");
        requireTrue(restarted.changed(), "RESTART existing scope replaces instance at active limit");
        requireEquals(TimerRuntimeService.MAX_ACTIVE_TIMERS_PER_SERVER, runtime.activeCount(), "existing scope policies do not increase active count");
    }

    private static TimerDefinition timer(String id, TimerMode mode) {
        return TimerStoreTest.timer(id, mode);
    }

    private static ActionConfig messageAction(String value) {
        return new ActionConfig(ActionType.MESSAGE, value, true, false, 0, false);
    }

    private static void requireSuccess(TimerOperationResult result, String message) {
        requireTrue(result.success(), message + " result=" + result);
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
