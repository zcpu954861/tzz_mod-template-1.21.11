package com.zcpu.tzzmod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zcpu.tzzmod.phone.chat.PhoneChatService;
import com.zcpu.tzzmod.task.TaskDataStore;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class CommandSuggestionUtil {
    private CommandSuggestionUtil() {
    }

    public static CompletableFuture<Suggestions> suggestSendMsgTargets(ServerCommandSource source, SuggestionsBuilder builder) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.addAll(getOnlinePlayerNames(source));
        values.addAll(PhoneChatService.getAllGroupNames());
        values.addAll(PhoneChatService.getAllGroupIds());
        return suggestStrings(values, builder);
    }

    public static CompletableFuture<Suggestions> suggestOnlinePlayerNames(ServerCommandSource source, SuggestionsBuilder builder) {
        return suggestStrings(getOnlinePlayerNames(source), builder);
    }

    public static CompletableFuture<Suggestions> suggestGroupTargets(SuggestionsBuilder builder) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.addAll(PhoneChatService.getAllGroupNames());
        values.addAll(PhoneChatService.getAllGroupIds());
        return suggestStrings(values, builder);
    }

    public static CompletableFuture<Suggestions> suggestTaskLineNames(ServerCommandSource source, SuggestionsBuilder builder) {
        if (source.getServer() == null) {
            return builder.buildFuture();
        }
        return suggestStrings(TaskDataStore.getSnapshot(source.getServer()).lines().keySet(), builder);
    }

    public static CompletableFuture<Suggestions> suggestTaskIndexes(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, String lineArgumentName) {
        ServerCommandSource source = context.getSource();
        if (source.getServer() == null) {
            return builder.buildFuture();
        }

        String lineName = getOptionalStringArgument(context, lineArgumentName);
        if (lineName == null) {
            return builder.buildFuture();
        }

        Map<String, List<TaskDataStore.TaskNode>> lines = TaskDataStore.getSnapshot(source.getServer()).lines();
        List<TaskDataStore.TaskNode> tasks = lines.get(lineName);
        if (tasks == null) {
            for (Map.Entry<String, List<TaskDataStore.TaskNode>> entry : lines.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(lineName)) {
                    tasks = entry.getValue();
                    break;
                }
            }
        }

        if (tasks == null || tasks.isEmpty()) {
            return builder.buildFuture();
        }

        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        for (int index = 1; index <= tasks.size(); index++) {
            suggestions.add(Integer.toString(index));
        }
        return suggestStrings(suggestions, builder);
    }

    public static CompletableFuture<Suggestions> suggestCoordinate(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, Axis axis, String mirrorArgumentName) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        BlockPos suggestedPos = resolveSuggestedBlockPos(context.getSource());
        BlockPos sourcePos = BlockPos.ofFloored(context.getSource().getPosition());
        suggestions.add(Integer.toString(extract(axis, suggestedPos)));
        suggestions.add(Integer.toString(extract(axis, sourcePos)));
        if (mirrorArgumentName != null && !mirrorArgumentName.isBlank()) {
            try {
                suggestions.add(Integer.toString(IntegerArgumentType.getInteger(context, mirrorArgumentName)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return suggestStrings(suggestions, builder);
    }

    public static CompletableFuture<Suggestions> suggestStrings(Iterable<String> values, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        Set<String> seen = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String suggestion = quoteIfNeeded(value);
            if (!seen.add(suggestion)) {
                continue;
            }
            String rawLower = value.toLowerCase(Locale.ROOT);
            String suggestionLower = suggestion.toLowerCase(Locale.ROOT);
            if (remaining.isEmpty() || rawLower.startsWith(remaining) || suggestionLower.startsWith(remaining)) {
                builder.suggest(suggestion);
            }
        }
        return builder.buildFuture();
    }

    private static List<String> getOnlinePlayerNames(ServerCommandSource source) {
        if (source.getServer() == null) {
            return List.of();
        }
        return source.getServer().getPlayerManager().getPlayerList().stream()
                .map(player -> player.getName().getString())
                .toList();
    }

    private static BlockPos resolveSuggestedBlockPos(ServerCommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            try {
                HitResult hitResult = player.raycast(64.0D, 0.0F, false);
                if (hitResult.getType() == HitResult.Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
                    return blockHitResult.getBlockPos();
                }
            } catch (Exception ignored) {
            }
            return player.getBlockPos();
        }
        return BlockPos.ofFloored(source.getPosition());
    }

    private static @Nullable String getOptionalStringArgument(CommandContext<ServerCommandSource> context, String argumentName) {
        try {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object argument = context.getArgument(argumentName, (Class) String.class);
            return argument instanceof String value ? value : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static int extract(Axis axis, BlockPos pos) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    private static String quoteIfNeeded(String value) {
        if (value.indexOf(' ') < 0 && value.indexOf('"') < 0 && value.indexOf('\\') < 0) {
            return value;
        }
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    public enum Axis {
        X,
        Y,
        Z
    }
}