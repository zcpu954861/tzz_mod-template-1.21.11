package com.zcpu.tzzmod.region;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionContext;
import com.zcpu.tzzmod.action.ActionEngine;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.action.ActionSourceType;
import com.zcpu.tzzmod.action.ActionValidator;
import com.zcpu.tzzmod.command.CommandSuggestionUtil;
import com.zcpu.tzzmod.map.MapDataStore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

    private static RequiredArgumentBuilder<ServerCommandSource, String> controllerIdArgument() {
        return CommandManager.argument("controllerId", StringArgumentType.string())
                .suggests((context, builder) -> CommandSuggestionUtil.suggestRegionControllerIds(context.getSource(), builder));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, String> triggerTypeArgument() {
        return CommandManager.argument("triggerType", StringArgumentType.string())
                .suggests((context, builder) -> CommandSuggestionUtil.suggestRegionTriggerTypes(builder));
    }

    private static int executeRegions(ServerCommandSource source) {
        if (source.getServer() == null) {
            return 0;
        }

        List<MapDataStore.PlannerRegionData> regions = MapDataStore.getPlannerRegionsSnapshot(source.getServer());
        source.sendFeedback(() -> title("规划区域列表：").append(number(regions.size())), false);
        if (regions.isEmpty()) {
            source.sendFeedback(() -> warning("暂无规划区域。"), false);
            return 0;
        }

        for (MapDataStore.PlannerRegionData region : regions) {
            source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY)
                    .append(regionName(region))
                    .append(Text.literal("（ID：").formatted(Formatting.GRAY))
                    .append(shortIdText(region.id()))
                    .append(Text.literal("）").formatted(Formatting.GRAY)), false);
            source.sendFeedback(() -> Text.literal("  维度：").formatted(Formatting.GRAY)
                    .append(Text.literal(region.dimensionId()).formatted(Formatting.YELLOW))
                    .append(Text.literal("，点数：").formatted(Formatting.GRAY))
                    .append(number(region.points().size())), false);
        }
        return regions.size();
    }

    private static int executeCreate(ServerCommandSource source, String regionId, String name) {
        if (source.getServer() == null) {
            return 0;
        }

        MapDataStore.PlannerRegionData region = MapDataStore.getPlannerRegion(source.getServer(), regionId);
        if (region == null) {
            source.sendFeedback(() -> error("找不到规划区域：" + regionId), false);
            return 0;
        }

        RegionControllerData controller = RegionControllerStore.createController(source.getServer(), region.id(), name);
        source.sendFeedback(() -> title("已创建区域控制器"), true);
        source.sendFeedback(() -> field("名称", controllerName(controller)), false);
        source.sendFeedback(() -> field("控制器", controllerName(controller)
                .append(Text.literal("（ID：").formatted(Formatting.GRAY))
                .append(shortIdText(controller.id()))
                .append(Text.literal("）").formatted(Formatting.GRAY))), false);
        source.sendFeedback(() -> field("绑定区域", regionName(region)
                .append(Text.literal("（ID：").formatted(Formatting.GRAY))
                .append(shortIdText(region.id()))
                .append(Text.literal("）").formatted(Formatting.GRAY))), false);
        source.sendFeedback(() -> field("查看详情", commandText("/tzz regionctl info " + shortId(controller.id()))), false);
        return 1;
    }

    private static int executeList(ServerCommandSource source) {
        if (source.getServer() == null) {
            return 0;
        }

        List<RegionControllerData> controllers = RegionControllerStore.getSnapshot(source.getServer());
        source.sendFeedback(() -> title("区域控制器列表：").append(number(controllers.size())), false);
        if (controllers.isEmpty()) {
            source.sendFeedback(() -> warning("暂无区域控制器。"), false);
            return 0;
        }

        for (RegionControllerData controller : controllers) {
            MapDataStore.PlannerRegionData region = MapDataStore.getPlannerRegion(source.getServer(), controller.regionId());
            source.sendFeedback(() -> Text.literal("- ").formatted(Formatting.GRAY).append(controllerName(controller)), false);
            source.sendFeedback(() -> field("  ID", shortIdText(controller.id())), false);
            source.sendFeedback(() -> field("  绑定区域", regionName(region)
                    .append(Text.literal("（").formatted(Formatting.GRAY))
                    .append(shortIdText(controller.regionId()))
                    .append(Text.literal("）").formatted(Formatting.GRAY))), false);
            source.sendFeedback(() -> field("  状态", statusText(controller.enabled())), false);
            source.sendFeedback(() -> field("  动作", actionCounts(controller)), false);
        }
        return controllers.size();
    }

    private static int executeInfo(ServerCommandSource source, String controllerId) {
        if (source.getServer() == null) {
            return 0;
        }

        RegionControllerData controller = resolveController(source, controllerId);
        if (controller == null) {
            return 0;
        }

        MapDataStore.PlannerRegionData region = MapDataStore.getPlannerRegion(source.getServer(), controller.regionId());
        RegionTargetFilter filter = controller.targetFilter().normalized();
        source.sendFeedback(() -> title("区域控制器详情"), false);
        source.sendFeedback(() -> field("名称", controllerName(controller)), false);
        source.sendFeedback(() -> field("状态", statusText(controller.enabled())), false);
        source.sendFeedback(() -> field("控制器ID", fullIdText(controller.id())), false);
        source.sendFeedback(() -> field("绑定区域", regionName(region)), false);
        source.sendFeedback(() -> field("绑定区域ID", fullIdText(controller.regionId())), false);
        source.sendFeedback(() -> field("触发对象", targetFilterText(filter)), false);
        source.sendFeedback(() -> field("停留间隔", number(controller.stayIntervalTicks())
                .append(Text.literal(" tick").formatted(Formatting.GRAY))), false);
        source.sendFeedback(() -> field("动作数量", actionCounts(controller)), false);
        return 1;
    }

    private static int executeSetEnabled(ServerCommandSource source, String controllerId, boolean enabled) {
        if (source.getServer() == null) {
            return 0;
        }

        RegionControllerData controller = resolveController(source, controllerId);
        if (controller == null) {
            return 0;
        }

        boolean changed = RegionControllerStore.setEnabled(source.getServer(), controller.id(), enabled);
        if (!changed) {
            source.sendFeedback(() -> error("区域控制器状态更新失败：" + controllerId), false);
            return 0;
        }

        source.sendFeedback(() -> title(enabled ? "已启用区域控制器" : "已禁用区域控制器")
                .append(Text.literal("：").formatted(Formatting.GRAY))
                .append(controllerName(controller))
                .append(Text.literal("（ID：").formatted(Formatting.GRAY))
                .append(shortIdText(controller.id()))
                .append(Text.literal("）").formatted(Formatting.GRAY)), true);
        return 1;
    }

    private static int executeDelete(ServerCommandSource source, String controllerId) {
        if (source.getServer() == null) {
            return 0;
        }

        RegionControllerData controller = resolveController(source, controllerId);
        if (controller == null) {
            return 0;
        }

        boolean deleted = RegionControllerStore.deleteController(source.getServer(), controller.id());
        if (!deleted) {
            source.sendFeedback(() -> error("区域控制器删除失败：" + controllerId), false);
            return 0;
        }

        source.sendFeedback(() -> title("已删除区域控制器")
                .append(Text.literal("：").formatted(Formatting.GRAY))
                .append(controllerName(controller))
                .append(Text.literal("（ID：").formatted(Formatting.GRAY))
                .append(shortIdText(controller.id()))
                .append(Text.literal("）").formatted(Formatting.GRAY)), true);
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
            source.sendFeedback(() -> error("未知触发类型：" + triggerTypeId), false);
            return 0;
        }

        RegionControllerData controller = resolveController(source, controllerId);
        if (controller == null) {
            return 0;
        }

        ActionConfig action = ActionConfig.command(command, false);
        Text validationError = ActionValidator.validateForSave(player, action);
        if (validationError != null) {
            source.sendFeedback(() -> error("动作配置无效，无法保存。"), false);
            return 0;
        }

        boolean changed = RegionControllerStore.addAction(source.getServer(), controller.id(), triggerType, action);
        if (!changed) {
            source.sendFeedback(() -> error("动作添加失败：" + controllerId), false);
            return 0;
        }

        source.sendFeedback(() -> title("已添加区域动作")
                .append(Text.literal("：").formatted(Formatting.GRAY))
                .append(triggerText(triggerType))
                .append(Text.literal(" -> ").formatted(Formatting.GRAY))
                .append(controllerName(controller)), true);
        source.sendFeedback(() -> field("命令", commandText(command)), false);
        return 1;
    }

    private static int executeClearActions(ServerCommandSource source, String controllerId, String triggerTypeId) {
        if (source.getServer() == null) {
            return 0;
        }

        RegionTriggerType triggerType = parseTriggerType(triggerTypeId);
        if (triggerType == null) {
            source.sendFeedback(() -> error("未知触发类型：" + triggerTypeId), false);
            return 0;
        }

        RegionControllerData controller = resolveController(source, controllerId);
        if (controller == null) {
            return 0;
        }

        boolean changed = RegionControllerStore.clearActions(source.getServer(), controller.id(), triggerType);
        if (!changed) {
            source.sendFeedback(() -> error("动作清空失败：" + controllerId), false);
            return 0;
        }

        source.sendFeedback(() -> title("已清空区域动作")
                .append(Text.literal("：").formatted(Formatting.GRAY))
                .append(triggerText(triggerType))
                .append(Text.literal(" -> ").formatted(Formatting.GRAY))
                .append(controllerName(controller)), true);
        return 1;
    }

    private static int executeTarget(ServerCommandSource source, String controllerId, RegionTargetFilter filter) {
        if (source.getServer() == null) {
            return 0;
        }

        RegionControllerData controller = resolveController(source, controllerId);
        if (controller == null) {
            return 0;
        }

        boolean changed = RegionControllerStore.setTargetFilter(source.getServer(), controller.id(), filter);
        if (!changed) {
            source.sendFeedback(() -> error("触发对象更新失败：" + controllerId), false);
            return 0;
        }

        RegionTargetFilter normalized = filter.normalized();
        source.sendFeedback(() -> title("已更新触发对象")
                .append(Text.literal("：").formatted(Formatting.GRAY))
                .append(controllerName(controller)), true);
        source.sendFeedback(() -> field("触发对象", targetFilterText(normalized)), false);
        return 1;
    }

    private static int executeStayInterval(ServerCommandSource source, String controllerId, int ticks) {
        if (source.getServer() == null) {
            return 0;
        }

        if (ticks < RegionControllerData.MIN_STAY_INTERVAL_TICKS) {
            source.sendFeedback(() -> error("停留触发间隔不能低于 " + RegionControllerData.MIN_STAY_INTERVAL_TICKS + " tick"), false);
            return 0;
        }

        RegionControllerData controller = resolveController(source, controllerId);
        if (controller == null) {
            return 0;
        }

        boolean changed = RegionControllerStore.setStayInterval(source.getServer(), controller.id(), ticks);
        if (!changed) {
            source.sendFeedback(() -> error("停留间隔更新失败：" + controllerId), false);
            return 0;
        }

        source.sendFeedback(() -> title("已更新停留触发间隔")
                .append(Text.literal("：").formatted(Formatting.GRAY))
                .append(controllerName(controller)), true);
        source.sendFeedback(() -> field("停留间隔", number(ticks).append(Text.literal(" tick").formatted(Formatting.GRAY))), false);
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
            source.sendFeedback(() -> error("未知触发类型：" + triggerTypeId), false);
            return 0;
        }

        RegionControllerData controller = resolveController(source, controllerId);
        if (controller == null) {
            return 0;
        }

        List<ActionConfig> actions = controller.actionsFor(triggerType);
        if (actions.isEmpty()) {
            source.sendFeedback(() -> warning("该触发类型没有配置动作：").append(triggerText(triggerType)), false);
            return 0;
        }

        ActionContext context = new ActionContext(
                player,
                player.getCommandSource().getWorld(),
                new Vec3d(player.getX(), player.getY(), player.getZ()),
                ActionSourceType.REGION_CONTROLLER,
                controller.id(),
                ItemStack.EMPTY
        );
        ActionExecutionResult result = ActionEngine.executeAll(context, actions);
        if (result.success()) {
            source.sendFeedback(() -> title("测试动作已执行")
                    .append(Text.literal("：").formatted(Formatting.GRAY))
                    .append(triggerText(triggerType))
                    .append(Text.literal(" -> ").formatted(Formatting.GRAY))
                    .append(controllerName(controller)), false);
        } else {
            source.sendFeedback(() -> error("测试动作执行失败。"), false);
        }
        return result.success() ? 1 : 0;
    }

    private static ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            return player;
        }
        source.sendFeedback(() -> error("该命令必须由玩家执行。"), false);
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

    private static RegionControllerData resolveController(ServerCommandSource source, String rawControllerId) {
        if (source.getServer() == null) {
            return null;
        }

        String query = rawControllerId == null ? "" : rawControllerId.trim();
        if (query.isBlank()) {
            source.sendFeedback(() -> error("控制器标识不能为空。"), false);
            return null;
        }

        List<RegionControllerData> controllers = RegionControllerStore.getSnapshot(source.getServer());
        for (RegionControllerData controller : controllers) {
            if (controller.id().equals(query)) {
                return controller;
            }
        }

        String shortQuery = query.endsWith("...") ? query.substring(0, query.length() - 3) : query;
        List<RegionControllerData> matches = new ArrayList<>();
        for (RegionControllerData controller : controllers) {
            if (safeName(controller).equals(query)
                    || shortId(controller.id()).equals(query)
                    || (shortQuery.length() >= 8 && controller.id().startsWith(shortQuery))) {
                matches.add(controller);
            }
        }

        if (matches.isEmpty()) {
            source.sendFeedback(() -> error("找不到区域控制器：" + query), false);
            return null;
        }

        if (matches.size() > 1) {
            source.sendFeedback(() -> error("控制器标识不唯一：" + query + "，请使用完整 ID。"), false);
            return null;
        }

        return matches.get(0);
    }

    private static MutableText title(String text) {
        return Text.literal(text).formatted(Formatting.GREEN);
    }

    private static MutableText error(String text) {
        return Text.literal(text).formatted(Formatting.RED);
    }

    private static MutableText warning(String text) {
        return Text.literal(text).formatted(Formatting.YELLOW);
    }

    private static MutableText field(String label, Text value) {
        return Text.literal(label + "：").formatted(Formatting.GRAY).append(value);
    }

    private static MutableText controllerName(RegionControllerData controller) {
        return Text.literal(safeName(controller)).formatted(Formatting.GOLD);
    }

    private static String safeName(RegionControllerData controller) {
        return controller.name() == null || controller.name().isBlank() ? "未命名控制器" : controller.name();
    }

    private static MutableText regionName(MapDataStore.PlannerRegionData region) {
        if (region == null) {
            return Text.literal("缺失区域").formatted(Formatting.YELLOW);
        }
        String name = region.name() == null || region.name().isBlank() ? "未命名区域" : region.name();
        return Text.literal(name).formatted(Formatting.YELLOW);
    }

    private static MutableText statusText(boolean enabled) {
        return Text.literal(enabled ? "启用" : "禁用").formatted(enabled ? Formatting.GREEN : Formatting.RED);
    }

    private static MutableText targetFilterText(RegionTargetFilter filter) {
        RegionTargetFilter normalized = filter == null ? RegionTargetFilter.all() : filter.normalized();
        return switch (normalized.type()) {
            case ALL -> Text.literal("所有玩家").formatted(Formatting.WHITE);
            case OP -> Text.literal("OP 玩家").formatted(Formatting.YELLOW);
            case TAG -> Text.literal("标签 ").formatted(Formatting.GRAY)
                    .append(Text.literal(normalized.value()).formatted(Formatting.AQUA));
        };
    }

    private static MutableText actionCounts(RegionControllerData controller) {
        return triggerText(RegionTriggerType.ENTER)
                .append(Text.literal(" ").formatted(Formatting.GRAY))
                .append(number(controller.enterActions().size()))
                .append(Text.literal("，").formatted(Formatting.GRAY))
                .append(triggerText(RegionTriggerType.EXIT))
                .append(Text.literal(" ").formatted(Formatting.GRAY))
                .append(number(controller.exitActions().size()))
                .append(Text.literal("，").formatted(Formatting.GRAY))
                .append(triggerText(RegionTriggerType.STAY))
                .append(Text.literal(" ").formatted(Formatting.GRAY))
                .append(number(controller.stayActions().size()));
    }

    private static MutableText triggerText(RegionTriggerType triggerType) {
        return switch (triggerType) {
            case ENTER -> Text.literal("进入").formatted(Formatting.GREEN);
            case EXIT -> Text.literal("离开").formatted(Formatting.GOLD);
            case STAY -> Text.literal("停留").formatted(Formatting.LIGHT_PURPLE);
        };
    }

    private static MutableText number(int value) {
        return Text.literal(Integer.toString(value)).formatted(Formatting.LIGHT_PURPLE);
    }

    private static MutableText commandText(String command) {
        return Text.literal(command == null ? "" : command).formatted(Formatting.GREEN);
    }

    private static MutableText fullIdText(String id) {
        return Text.literal(id == null || id.isBlank() ? "未知" : id).formatted(Formatting.AQUA);
    }

    private static MutableText shortIdText(String id) {
        return Text.literal(shortId(id)).formatted(Formatting.AQUA);
    }

    private static String shortId(String id) {
        if (id == null || id.isBlank()) {
            return "未知";
        }
        return id.length() <= 8 ? id : id.substring(0, 8) + "...";
    }
}
