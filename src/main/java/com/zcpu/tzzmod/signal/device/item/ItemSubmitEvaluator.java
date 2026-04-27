package com.zcpu.tzzmod.signal.device.item;

import com.zcpu.tzzmod.signal.device.ContainerItemCountMode;
import com.zcpu.tzzmod.signal.device.ItemSubmitRequirementData;
import java.util.ArrayList;
import java.util.List;

public final class ItemSubmitEvaluator {
    private ItemSubmitEvaluator() {
    }

    @FunctionalInterface
    public interface Matcher {
        boolean matches(SourceStack stack, ItemStackMatcherData matcher);
    }

    public record SourceStack(
            String key,
            String itemId,
            int count,
            String label,
            ConsumePlan.ConsumeAction action
    ) {
        public SourceStack {
            key = key == null ? "" : key;
            itemId = itemId == null ? "" : itemId;
            count = Math.max(0, count);
            label = label == null || label.isBlank() ? key : label;
        }

        public SourceStack(String key, int count, String label, ConsumePlan.ConsumeAction action) {
            this(key, key, count, label, action);
        }

        ConsumePlanner.ConsumableStack consumable() {
            return new ConsumePlanner.ConsumableStack(key, count, label, action);
        }
    }

    public static ItemSubmitEvaluationResult evaluate(
            List<ItemSubmitRequirementData> requirements,
            List<SourceStack> sourceStacks,
            boolean consumeEnabled,
            ConsumePlan existingPlan,
            Matcher matcher
    ) {
        List<ItemSubmitEvaluationResult.RequirementResult> results = new ArrayList<>();
        List<ItemSubmitRequirementData> enabledRequirements = new ArrayList<>();
        List<SourceStack> stacks = sourceStacks == null ? List.of() : sourceStacks;
        Matcher cleanMatcher = matcher == null ? ItemSubmitEvaluator::itemIdMatches : matcher;

        if (requirements != null) {
            for (ItemSubmitRequirementData rawRequirement : requirements) {
                if (rawRequirement == null) {
                    continue;
                }
                ItemSubmitRequirementData requirement = rawRequirement.normalized();
                if (!requirement.enabled()) {
                    results.add(new ItemSubmitEvaluationResult.RequirementResult(
                            requirement.name(),
                            false,
                            true,
                            0,
                            ""
                    ));
                    continue;
                }

                int matchedCount = matchedCount(stacks, requirement.matcher(), cleanMatcher);
                boolean matched = matchesInventoryCount(matchedCount, requirement.matcher());
                results.add(new ItemSubmitEvaluationResult.RequirementResult(
                        requirement.name(),
                        true,
                        matched,
                        matchedCount,
                        matched ? "" : "submit_requirement_not_matched:" + requirement.name()
                ));
                if (!matched) {
                    return new ItemSubmitEvaluationResult(
                            false,
                            false,
                            false,
                            "submit_requirement_not_matched:" + requirement.name(),
                            results,
                            new ConsumePlan(),
                            ""
                    );
                }
                enabledRequirements.add(requirement);
            }
        }

        if (enabledRequirements.isEmpty()) {
            return new ItemSubmitEvaluationResult(
                    false,
                    false,
                    false,
                    "item_submit_no_enabled_requirements",
                    results,
                    new ConsumePlan(),
                    ""
            );
        }

        if (!consumeEnabled) {
            return new ItemSubmitEvaluationResult(true, true, true, "", results, new ConsumePlan(), "");
        }

        ConsumePlan staged = existingPlan == null ? new ConsumePlan() : existingPlan.copy();
        for (ItemSubmitRequirementData requirement : enabledRequirements) {
            List<ConsumePlanner.ConsumableStack> matchingStacks = matchingConsumableStacks(stacks, requirement.matcher(), cleanMatcher);
            String failure = ConsumePlanner.stageAcrossStacks(staged, matchingStacks, requirement.consumeCount());
            if (!failure.isBlank()) {
                return new ItemSubmitEvaluationResult(
                        true,
                        false,
                        false,
                        "item_submit_consume_plan_failed:" + failure + ":" + requirement.name(),
                        results,
                        new ConsumePlan(),
                        ""
                );
            }
        }

        return new ItemSubmitEvaluationResult(true, true, true, "", results, staged, staged.summary());
    }

    public static boolean matchesInventoryCount(int totalCount, ItemStackMatcherData matcher) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        String mode = ContainerItemCountMode.normalize(data.countMode());
        if (ContainerItemCountMode.IGNORE.id().equals(mode)) {
            return totalCount > 0;
        }
        if (ContainerItemCountMode.AT_MOST.id().equals(mode)) {
            return totalCount > 0 && totalCount <= data.requiredCount();
        }
        return ContainerItemCountMode.fromId(mode).matches(totalCount, data.requiredCount());
    }

    private static int matchedCount(List<SourceStack> stacks, ItemStackMatcherData matcher, Matcher stackMatcher) {
        int total = 0;
        for (SourceStack stack : stacks) {
            if (stack != null && stackMatcher.matches(stack, matcher)) {
                total += stack.count();
            }
        }
        return total;
    }

    private static List<ConsumePlanner.ConsumableStack> matchingConsumableStacks(
            List<SourceStack> stacks,
            ItemStackMatcherData matcher,
            Matcher stackMatcher
    ) {
        List<ConsumePlanner.ConsumableStack> matching = new ArrayList<>();
        for (SourceStack stack : stacks) {
            if (stack != null && stackMatcher.matches(stack, matcher)) {
                matching.add(stack.consumable());
            }
        }
        return matching;
    }

    private static boolean itemIdMatches(SourceStack stack, ItemStackMatcherData matcher) {
        ItemStackMatcherData data = matcher == null ? ItemStackMatcherData.empty() : matcher.normalized();
        return data.enabled() && (!data.matchItemId() || stack.itemId().equals(data.templateItemId()));
    }
}
