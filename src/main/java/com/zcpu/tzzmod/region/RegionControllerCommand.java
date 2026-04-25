package com.zcpu.tzzmod.region;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionContext;
import com.zcpu.tzzmod.action.ActionEngine;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.action.ActionValidator;
import com.zcpu.tzzmod.command.CommandSuggestionUtil;
import com.zcpu.tzzmod.map.MapDataStore;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public final class RegionControllerCommand {
    private RegionControllerCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("regionctl")
                .requires(source -> !(source.getEntity() instanceof ServerPlayerEntity player) || player.isCreativeLevelTwoOp())
                .then(CommandManager.literal("regions")
                        .executes(context -> executeRegions(context.getSource())))
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("regionId", StringArgumentType.string())
                                .suggests((context, builder) -> CommandSuggestionUtil.suggestPlannerRegionIds(context.getSource(), builder))
                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> executeCreate(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "regionId"),
                                                StringArgumentType.getString(context, "name")
                                        )))))
                .then(CommandManager.literal("list")
                        .executes(context -> executeList(context.getSource())))
                .then(CommandManager.literal("info")
                        .then(controllerIdArgument()
                                .executes(context -> executeInfo(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "controllerId")
                                ))))
                .then(enableDisableDeleteCommand("enable"))
                .then(enableDisableDeleteCommand("disable"))
                .then(enableDisableDeleteCommand("delete"))
                .then(CommandManager.literal("addAction")
                        .then(controllerIdArgument()
                                .then(triggerTypeArgument()
                                        .then(CommandManager.literal("command")
                                                .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                                        .executes(context -> executeAddAction(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "controllerId"),
                                                                StringArgumentType.getString(context, "triggerType"),
                                                                StringArgumentType.getString(context, "command")
                                                        )))))))
                .then(CommandManager.literal("clearActions")
                        .then(controllerIdArgument()
                                .then(triggerTypeArgument()
                                        .executes(context -> executeClearActions(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "controllerId"),
                                                StringArgumentType.getString(context, "triggerType")
                                        )))))
                .then(CommandManager.literal("target")
                        .then(controllerIdArgument()
                                .then(CommandManager.literal("all")
                                        .executes(context -> executeTarget(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "controllerId"),
                                                RegionTargetFilter.all()
                                        )))
                                .then(CommandManager.literal("op")
                                        .executes(context -> executeTarget(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "controllerId"),
                                                new RegionTargetFilter(RegionTargetFilter.Type.OP, "")
                                        )))
                                .then(CommandManager.literal("tag")
                                        .then(CommandManager.argument("tagName", StringArgumentType.string())
                                                .suggests((context, builder) -> CommandSuggestionUtil.suggestOnlinePlayerTags(context.getSource(), builder))
                                                .executes(context -> executeTarget(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "controllerId"),
                                                        new RegionTargetFilter(
                                                                RegionTargetFilter.Type.TAG,
                                                                StringArgumentType.getString(context, "tagName")
                                                        )
                                                ))))))
                .then(CommandManager.literal("stayInterval")
                        .then(controllerIdArgument()
                                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1))
                                        .executes(context -> executeStayInterval(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "controllerId"),
                                                IntegerArgumentType.getInteger(context, "ticks")
                                        )))))
                .then(CommandManager.literal("test")
                        .then(controllerIdArgument()
                                .then(triggerTypeArgument()
                                        .executes(context -> executeTest(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "controllerId"),
                                                StringArgumentType.getString(context, "triggerType")
                                        )))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> enableDisableDeleteCommand(String action) {
        return CommandManager.literal(action)
                .then(controllerIdArgument()
                        .executes(context -> switch (action) {
                            case "enable" -> executeSetEnabled(context.getSource(), StringArgumentType.getString(context, "controllerId"), true);
                            case "disable" -> executeSetEnabled(context.getSource(), StringArgumentType.getString(context, "controllerId"), false);
                            case "delete" -> executeDelete(context.getSource(), StringArgumentType.getString(context, "controllerId"));
                            default -> 0;
                        }));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<ServerCommandSource, String> controllerIdArgument() {
        return CommandManager.argument("controllerId", StringArgumentType.string())
                .suggests((context, builder) -> CommandSuggestionUtil.suggestRegionControllerIds(context.getSource(), builder));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<ServerCommandSource, String> triggerTypeArgument() {
        return CommandManager.argument("triggerType", StringArgumentType.string())
                .suggests((context, builder) -> CommandSuggestionUtil.suggestRegionTriggerTypes(builder));
    }

    private static int executeRegions(ServerCommandSource source) {
        if (source.getServer() == null) {
            return 0;
        }
        List<MapDataStore.PlannerRegionData> regions = MapDataStore.getPlannerRegionsSnapshot(source.getServer());
        source.sendFeedback(() -> Text.literal("Planner regions: " + regions.size()), false);
        for (MapDataStore.PlannerRegionData region : regions) {
            source.sendFeedback(() -> Text.literal(
                    region.id() + " | " + region.name() + " | " + region.dimensionId() + " | points=" + region.points().size()
            ), false);
        }
        return regions.size();
    }

    private static int executeCreate(ServerCommandSource source, String regionId, String name) {
        if (source.getServer() == null) {
            return 0;
        }
        MapDataStore.PlannerRegionData region = MapDataStore.getPlannerRegion(source.getServer(), regionId);
        if (region == null) {
            source.sendFeedback(() -> Text.literal("鎵句笉鍒拌鍒掑尯鍩燂細" + regionId), false);
            return 0;
        }
        RegionControllerData controller = RegionControllerStore.createController(source.getServer(), region.id(), name);
        source.sendFeedback(() -> Text.literal(
                "Created region controller: " + controller.id() + " -> " + controller.regionId()
        ), true);
        return 1;
    }

    private static int executeList(ServerCommandSource source) {
        if (source.getServer() == null) {
            return 0;
        }
        List<RegionControllerData> controllers = RegionControllerStore.getSnapshot(source.getServer());
        source.sendFeedback(() -> Text.literal("Region controllers: " + controllers.size()), false);
        for (RegionControllerData controller : controllers) {
            source.sendFeedback(() -> Text.literal(
                    controller.id()
                            + " | " + controller.name()
                            + " | region=" + controller.regionId()
                            + " | enabled=" + controller.enabled()
                            + " | enter=" + controller.enterActions().size()
                            + " | exit=" + controller.exitActions().size()
                            + " | stay=" + controller.stayActions().size()
            ), false);
        }
        return controllers.size();
    }

    private static int executeInfo(ServerCommandSource source, String controllerId) {
        if (source.getServer() == null) {
            return 0;
        }
        RegionControllerData controller = RegionControllerStore.getController(source.getServer(), controllerId);
        if (controller == null) {
            source.sendFeedback(() -> Text.literal("Region controller not found: " + controllerId), false);
            return 0;
        }
        MapDataStore.PlannerRegionData region = MapDataStore.getPlannerRegion(source.getServer(), controller.regionId());
        String regionName = region == null ? "<missing>" : region.name();
        RegionTargetFilter filter = controller.targetFilter().normalized();
        source.sendFeedback(() -> Text.literal("id=" + controller.id()), false);
        source.sendFeedback(() -> Text.literal("name=" + controller.name()), false);
        source.sendFeedback(() -> Text.literal("enabled=" + controller.enabled()), false);
        source.sendFeedback(() -> Text.literal("regionId=" + controller.regionId()), false);
        source.sendFeedback(() -> Text.literal("regionName=" + regionName), false);
        source.sendFeedback(() -> Text.literal("target=" + filter.type() + (filter.value().isBlank() ? "" : ":" + filter.value())), false);
        source.sendFeedback(() -> Text.literal("stayIntervalTicks=" + controller.stayIntervalTicks()), false);
        source.sendFeedback(() -> Text.literal("enter=" + controller.enterActions().size() + ", exit=" + controller.exitActions().size() + ", stay=" + controller.stayActions().size()), false);
        return 1;
    }

    private static int executeSetEnabled(ServerCommandSource source, String controllerId, boolean enabled) {
        if (source.getServer() == null) {
            return 0;
        }
        boolean changed = RegionControllerStore.setEnabled(source.getServer(), controllerId, enabled);
        if (!changed) {
            source.sendFeedback(() -> Text.literal("Region controller not found: " + controllerId), false);
            return 0;
        }
        source.sendFeedback(() -> Text.literal((enabled ? "Enabled" : "Disabled") + " region controller: " + controllerId), true);
        return 1;
    }

    private static int executeDelete(ServerCommandSource source, String controllerId) {
        if (source.getServer() == null) {
            return 0;
        }
        boolean deleted = RegionControllerStore.deleteController(source.getServer(), controllerId);
        if (!deleted) {
            source.sendFeedback(() -> Text.literal("Region controller not found: " + controllerId), false);
            return 0;
        }
        source.sendFeedback(() -> Text.literal("Deleted region controller: " + controllerId), true);
        return 1;
    }

    private static int executeAddAction(ServerCommandSource source, String controllerId, String triggerTypeId, String command) {
        if (source.getServer() == null) {
            return 0;
        }
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) {
            return 0;
        }
        RegionTriggerType triggerType = parseTriggerType(triggerTypeId);
        if (triggerType == null) {
            source.sendFeedback(() -> Text.literal("Unknown trigger type: " + triggerTypeId), false);
            return 0;
        }
        if (RegionControllerStore.getController(source.getServer(), controllerId) == null) {
            source.sendFeedback(() -> Text.literal("Region controller not found: " + controllerId), false);
            return 0;
        }
        ActionConfig action = ActionConfig.command(command, false);
        Text validationError = ActionValidator.validateForSave(player, action);
        if (validationError != null) {
            source.sendFeedback(() -> validationError, false);
            return 0;
        }
        boolean changed = RegionControllerStore.addAction(source.getServer(), controllerId, triggerType, action);
        if (!changed) {
            source.sendFeedback(() -> Text.literal("Failed to add action for controller: " + controllerId), false);
            return 0;
        }
        source.sendFeedback(() -> Text.literal("Added " + triggerType.name().toLowerCase() + " action to " + controllerId), true);
        return 1;
    }

    private static int executeClearActions(ServerCommandSource source, String controllerId, String triggerTypeId) {
        if (source.getServer() == null) {
            return 0;
        }
        RegionTriggerType triggerType = parseTriggerType(triggerTypeId);
        if (triggerType == null) {
            source.sendFeedback(() -> Text.literal("Unknown trigger type: " + triggerTypeId), false);
            return 0;
        }
        boolean changed = RegionControllerStore.clearActions(source.getServer(), controllerId, triggerType);
        if (!changed) {
            source.sendFeedback(() -> Text.literal("Region controller not found: " + controllerId), false);
            return 0;
        }
        source.sendFeedback(() -> Text.literal("Cleared " + triggerType.name().toLowerCase() + " actions for " + controllerId), true);
        return 1;
    }

    private static int executeTarget(ServerCommandSource source, String controllerId, RegionTargetFilter filter) {
        if (source.getServer() == null) {
            return 0;
        }
        boolean changed = RegionControllerStore.setTargetFilter(source.getServer(), controllerId, filter);
        if (!changed) {
            source.sendFeedback(() -> Text.literal("Region controller not found: " + controllerId), false);
            return 0;
        }
        RegionTargetFilter normalized = filter.normalized();
        source.sendFeedback(() -> Text.literal(
                "Updated target filter for " + controllerId + ": " + normalized.type() + (normalized.value().isBlank() ? "" : ":" + normalized.value())
        ), true);
        return 1;
    }

    private static int executeStayInterval(ServerCommandSource source, String controllerId, int ticks) {
        if (source.getServer() == null) {
            return 0;
        }
        if (ticks < RegionControllerData.MIN_STAY_INTERVAL_TICKS) {
            source.sendFeedback(() -> Text.literal("stayInterval must be at least " + RegionControllerData.MIN_STAY_INTERVAL_TICKS + " ticks"), false);
            return 0;
        }
        boolean changed = RegionControllerStore.setStayInterval(source.getServer(), controllerId, ticks);
        if (!changed) {
            source.sendFeedback(() -> Text.literal("Region controller not found: " + controllerId), false);
            return 0;
        }
        source.sendFeedback(() -> Text.literal("Updated stayInterval for " + controllerId + " to " + ticks + " ticks"), true);
        return 1;
    }

    private static int executeTest(ServerCommandSource source, String controllerId, String triggerTypeId) {
        if (source.getServer() == null) {
            return 0;
        }
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) {
            return 0;
        }
        RegionTriggerType triggerType = parseTriggerType(triggerTypeId);
        if (triggerType == null) {
            source.sendFeedback(() -> Text.literal("Unknown trigger type: " + triggerTypeId), false);
            return 0;
        }
        RegionControllerData controller = RegionControllerStore.getController(source.getServer(), controllerId);
        if (controller == null) {
            source.sendFeedback(() -> Text.literal("Region controller not found: " + controllerId), false);
            return 0;
        }
        List<com.zcpu.tzzmod.action.ActionConfig> actions = controller.actionsFor(triggerType);
        if (actions.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No actions configured for " + triggerType.name().toLowerCase()), false);
            return 0;
        }
        ActionContext context = new ActionContext(
                player,
                player.getEntityWorld(),
                new Vec3d(player.getX(), player.getY(), player.getZ()),
                ActionSourceType.REGION_CONTROLLER,
                controller.id(),
                ItemStack.EMPTY
        );
        ActionExecutionResult result = ActionEngine.executeAll(context, actions);
        source.sendFeedback(() -> result.message(), false);
        return result.success() ? 1 : 0;
    }

    private static ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            return player;
        }
        source.sendFeedback(() -> Text.literal("This command must be executed by a player."), false);
        return null;
    }

    private static RegionTriggerType parseTriggerType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toLowerCase()) {
            case "enter" -> RegionTriggerType.ENTER;
            case "exit" -> RegionTriggerType.EXIT;
            case "stay" -> RegionTriggerType.STAY;
            default -> null;
        };
    }
}
