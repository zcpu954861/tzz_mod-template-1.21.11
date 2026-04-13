package com.zcpu.tzzmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zcpu.tzzmod.phone.chat.PhoneChatConfig;
import com.zcpu.tzzmod.phone.chat.PhoneChatService;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public final class SendMsgCommand {
    private SendMsgCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("sendmsg")
            .then(CommandManager.literal("player")
                .then(CommandManager.argument("player", StringArgumentType.string())
                    .suggests((context, builder) -> CommandSuggestionUtil.suggestOnlinePlayerNames(context.getSource(), builder))
                    .then(CommandManager.argument("text", StringArgumentType.greedyString())
                        .executes(context -> executeDirect(
                            context.getSource(),
                            StringArgumentType.getString(context, "player"),
                            StringArgumentType.getString(context, "text")
                        ))
                    )
                )
            )
            .then(CommandManager.literal("group")
                .then(CommandManager.argument("group", StringArgumentType.string())
                    .suggests((context, builder) -> CommandSuggestionUtil.suggestGroupTargets(builder))
                    .then(CommandManager.argument("text", StringArgumentType.greedyString())
                        .executes(context -> executeGroup(
                            context.getSource(),
                            StringArgumentType.getString(context, "group"),
                            StringArgumentType.getString(context, "text")
                        ))
                    )
                )
            )
                        .then(CommandManager.argument("target", StringArgumentType.string())
                .suggests((context, builder) -> CommandSuggestionUtil.suggestSendMsgTargets(context.getSource(), builder))
                                .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                        .executes(context -> execute(context.getSource(), StringArgumentType.getString(context, "target"), StringArgumentType.getString(context, "text")))
                                )
                        )
        );
    }

    private static int execute(ServerCommandSource source, String target, String text) {
        if (source == null || source.getServer() == null) {
            return 0;
        }

        if (!(source.getEntity() instanceof ServerPlayerEntity sender)) {
            source.sendFeedback(() -> Text.literal("/sendmsg 只能由玩家执行。"), false);
            return 0;
        }

        // require OP for this command
        if (!sender.isCreativeLevelTwoOp()) {
            source.sendFeedback(() -> Text.literal("你没有权限使用 /sendmsg（需要 OP）。"), false);
            return 0;
        }

        PhoneChatConfig config = PhoneChatConfig.get(source.getServer());
        if (text == null || text.isBlank() || text.length() > config.maxMessageLength) {
            source.sendFeedback(() -> Text.literal("消息为空或超过最大长度（" + config.maxMessageLength + " 字符）。"), false);
            return 0;
        }

        // Try group first
        var members = PhoneChatService.getGroupMembers(target);
        String resolvedGroupId = target;
        if (members.isEmpty()) {
            // try resolve by group name
            String found = PhoneChatService.findGroupIdByName(target);
            if (!found.isEmpty()) {
                resolvedGroupId = found;
                members = PhoneChatService.getGroupMembers(resolvedGroupId);
            }
        }
        if (!members.isEmpty()) {
            var envelope = PhoneChatService.sendGroup(sender, resolvedGroupId, text, config);
            if (envelope == null) {
                source.sendFeedback(() -> Text.literal("无法向该群发送消息。请确保你是群成员或群组存在。"), false);
                return 0;
            }
            PhoneChatService.deliverToParticipants(source.getServer(), envelope, members);
            source.sendFeedback(() -> Text.literal("已向群 '" + target + "' 发送消息。"), false);
            return 1;
        }

        // Otherwise treat as direct (target should be UUID)
        String targetUuid = resolvePlayerTargetUuid(source, target);
        var envelope = targetUuid.isBlank() ? null : PhoneChatService.sendDirect(source.getServer(), sender, targetUuid, text, config);
        if (envelope == null) {
            source.sendFeedback(() -> Text.literal("目标玩家无效（可使用在线玩家名字或 UUID）。"), false);
            return 0;
        }

        PhoneChatService.deliverToParticipants(source.getServer(), envelope, List.of(sender.getUuidAsString()));

        var receiverEnvelope = envelope.deepCopy();
        receiverEnvelope.addProperty("targetId", sender.getUuidAsString());
        receiverEnvelope.addProperty("title", sender.getName().getString());
        PhoneChatService.deliverToParticipants(source.getServer(), receiverEnvelope, List.of(targetUuid));

        source.sendFeedback(() -> Text.literal("已向 '" + target + "' 发送消息。"), false);
        return 1;
    }

    private static int executeDirect(ServerCommandSource source, String target, String text) {
        return execute(source, target, text);
    }

    private static int executeGroup(ServerCommandSource source, String target, String text) {
        return execute(source, target, text);
    }

    private static String resolvePlayerTargetUuid(ServerCommandSource source, String target) {
        if (target == null || target.isBlank() || source.getServer() == null) {
            return "";
        }
        String trimmed = target.trim();
        try {
            java.util.UUID.fromString(trimmed);
            return trimmed;
        } catch (Exception ignored) {
        }

        for (ServerPlayerEntity online : source.getServer().getPlayerManager().getPlayerList()) {
            if (online.getName().getString().equalsIgnoreCase(trimmed)) {
                return online.getUuidAsString();
            }
        }
        return "";
    }
}
