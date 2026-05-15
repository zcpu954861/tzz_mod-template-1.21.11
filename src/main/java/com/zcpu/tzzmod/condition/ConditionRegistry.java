package com.zcpu.tzzmod.condition;

import com.zcpu.tzzmod.condition.state.StateVariableCompareOperator;
import com.zcpu.tzzmod.condition.state.StateVariableKey;
import com.zcpu.tzzmod.condition.state.StateVariableRecord;
import com.zcpu.tzzmod.condition.state.StateVariableScope;
import com.zcpu.tzzmod.condition.state.StateVariableTargetMode;
import com.zcpu.tzzmod.condition.state.StateVariableType;
import com.zcpu.tzzmod.condition.state.StateVariableValidation;
import com.zcpu.tzzmod.signal.SignalChannel;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ConditionRegistry {
    private static final String CATEGORY_GROUP = "组合条件";
    private static final String CATEGORY_DEBUG = "调试条件";
    private static final String CATEGORY_CONTEXT = "上下文条件";
    private static final String CATEGORY_PLAYER = "玩家条件";
    private static final String CATEGORY_TIME = "时间条件";
    private static final String CATEGORY_METADATA = "元数据条件";
    private static final String CATEGORY_STATE = "状态变量条件";
    private static final Set<String> GAME_MODES = Set.of("survival", "creative", "adventure", "spectator");
    private static final Set<String> GAME_TIME_OPERATORS = Set.of("eq", "ne", "gt", "gte", "lt", "lte");

    private final Map<String, ConditionTypeHandler> handlers = new LinkedHashMap<>();
    private final Map<String, ConditionTypeMetadata> groupMetadata = new LinkedHashMap<>();

    public ConditionRegistry register(ConditionTypeHandler handler) {
        if (handler == null || handler.metadata() == null || handler.metadata().type().isBlank()) {
            return this;
        }
        handlers.put(handler.metadata().type(), handler);
        return this;
    }

    public ConditionRegistry registerGroupMetadata(ConditionTypeMetadata metadata) {
        if (metadata != null && !metadata.type().isBlank()) {
            groupMetadata.put(metadata.type(), metadata);
        }
        return this;
    }

    public Optional<ConditionTypeHandler> handler(String type) {
        return Optional.ofNullable(handlers.get(normalize(type)));
    }

    public Optional<ConditionTypeMetadata> metadata(String type) {
        String key = normalize(type);
        ConditionTypeHandler handler = handlers.get(key);
        if (handler != null) {
            return Optional.of(handler.metadata());
        }
        return Optional.ofNullable(groupMetadata.get(key));
    }

    public List<ConditionTypeMetadata> metadata() {
        Map<String, ConditionTypeMetadata> all = new LinkedHashMap<>(groupMetadata);
        handlers.values().forEach((handler) -> all.put(handler.metadata().type(), handler.metadata()));
        return List.copyOf(all.values());
    }

    public ConditionValidationResult validate(ConditionNode node) {
        if (node == null) {
            return ConditionValidationResult.error("", "", "condition_node_null", "条件节点为空");
        }
        if (node.isGroup()) {
            return ConditionValidationResult.ok();
        }
        ConditionTypeHandler handler = handlers.get(node.type());
        if (handler == null) {
            return ConditionValidationResult.error(node.id(), "", "condition_type_unknown", "未知条件类型：" + node.type());
        }
        return handler.validate(node);
    }

    public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
        ConditionTypeHandler handler = handlers.get(node == null ? "" : node.type());
        if (handler == null) {
            return ConditionEvaluationResult.error(node, context, "condition_type_unknown", "未知条件类型：" + (node == null ? "" : node.type()));
        }
        ConditionValidationResult validation = handler.validate(node);
        if (!validation.valid()) {
            ConditionValidationIssue issue = validation.issues().getFirst();
            return ConditionEvaluationResult.error(node, context, issue.code(), issue.message());
        }
        try {
            return handler.evaluate(node, context);
        } catch (Exception exception) {
            return ConditionEvaluationResult.error(node, context, "condition_evaluation_exception", "条件判断异常：" + exception.getMessage());
        }
    }

    public static ConditionRegistry defaultRegistry() {
        ConditionRegistry registry = new ConditionRegistry();
        registry.registerGroupMetadata(metadata(
                ConditionNodeType.GROUP,
                "条件组",
                "按全部满足、任意满足或条件取反组合子条件。",
                CATEGORY_GROUP,
                field("groupMode", "组合方式", "enum:AND,OR,NOT", true, "AND=全部条件满足，OR=任意条件满足，NOT=条件取反"),
                field("children", "子条件", "condition-list", true, "参与组合判断的子条件列表")
        ));
        registry.register(new AlwaysTrueHandler());
        registry.register(new AlwaysFalseHandler());
        registry.register(new ContextExistsHandler());
        registry.register(new ContextFieldExistsHandler());
        registry.register(new ContextEqualsHandler());
        registry.register(new PlayerExistsHandler());
        registry.register(new PlayerOnlineHandler());
        registry.register(new PlayerIsOpHandler());
        registry.register(new PlayerTagHandler(ConditionNodeType.PLAYER_HAS_TAG, "玩家拥有标签", true));
        registry.register(new PlayerTagHandler(ConditionNodeType.PLAYER_LACKS_TAG, "玩家没有标签", false));
        registry.register(new PlayerTeamEqualsHandler());
        registry.register(new PlayerGamemodeEqualsHandler());
        registry.register(new PlayerAliveHandler(ConditionNodeType.PLAYER_ALIVE, "玩家存活", true));
        registry.register(new PlayerAliveHandler(ConditionNodeType.PLAYER_DEAD, "玩家死亡", false));
        registry.register(new ContextIdEqualsHandler(ConditionNodeType.SOURCE_TYPE_EQUALS, "来源类型匹配", "sourceType", "来源类型", "检查事件来源类型是否匹配。"));
        registry.register(new ContextIdEqualsHandler(ConditionNodeType.SOURCE_ID_EQUALS, "来源 ID 匹配", "sourceId", "来源 ID", "检查事件来源 ID 是否匹配。"));
        registry.register(new ChannelEqualsHandler());
        registry.register(new ContextIdEqualsHandler(ConditionNodeType.WORLD_EQUALS, "世界匹配", "world", "世界", "检查世界或维度 ID 是否匹配。"));
        registry.register(new ContextIdEqualsHandler(ConditionNodeType.DEVICE_ID_EQUALS, "设备 ID 匹配", "deviceId", "设备 ID", "检查上下文中的设备 ID 是否匹配。"));
        registry.register(new ContextIdEqualsHandler(ConditionNodeType.LISTENER_ID_EQUALS, "监听器 ID 匹配", "listenerId", "监听器 ID", "检查上下文中的监听器 ID 是否匹配。"));
        registry.register(new ContextIdEqualsHandler(ConditionNodeType.REGION_ID_EQUALS, "区域 ID 匹配", "regionId", "区域 ID", "检查上下文中的区域 ID 是否匹配。"));
        registry.register(new ContextIdEqualsHandler(ConditionNodeType.ACTION_ID_EQUALS, "动作 ID 匹配", "actionId", "动作 ID", "检查上下文中的动作 ID 是否匹配。"));
        registry.register(new GameTimeCompareHandler());
        registry.register(new EventMetadataExistsHandler());
        registry.register(new EventMetadataEqualsHandler());
        registry.register(new StateVariableExistsHandler());
        registry.register(new StateVariableBoolEqualsHandler());
        registry.register(new StateVariableIntCompareHandler());
        registry.register(new StateVariableStringEqualsHandler());
        registry.register(new StateVariableStringContainsHandler());
        return registry;
    }

    private static String normalize(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }

    private static ConditionTypeMetadata metadata(
            String type,
            String displayName,
            String description,
            String category,
            ConditionFieldSchema... fields
    ) {
        return new ConditionTypeMetadata(type, displayName, description, category, Arrays.asList(fields));
    }

    private static ConditionFieldSchema field(String name, String displayName, String kind, boolean required, String description) {
        return new ConditionFieldSchema(name, displayName, kind, required, description);
    }

    private static String config(ConditionNode node, String key) {
        return node == null || node.config() == null ? "" : node.config().get(key);
    }

    private static boolean hasConfigKey(ConditionNode node, String key) {
        return node != null && node.config() != null && node.config().values().containsKey(key);
    }

    private static ConditionValidationResult requireNonBlank(ConditionNode node, String key, String displayName) {
        if (node == null || !node.config().has(key)) {
            return ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_missing_" + key, "条件缺少必填字段：" + displayName);
        }
        return ConditionValidationResult.ok();
    }

    private static ConditionValidationResult requireBooleanIfPresent(ConditionNode node, String key, String displayName) {
        if (!hasConfigKey(node, key)) {
            return ConditionValidationResult.ok();
        }
        String value = config(node, key).toLowerCase(Locale.ROOT);
        if ("true".equals(value) || "false".equals(value)) {
            return ConditionValidationResult.ok();
        }
        return ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_" + key, displayName + " 必须是 true 或 false");
    }

    private static boolean configBoolean(ConditionNode node, String key, boolean defaultValue) {
        if (!hasConfigKey(node, key)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(config(node, key));
    }

    private static ConditionEvaluationResult leaf(ConditionNode node, ConditionEvaluationContext context, boolean matched, String code, String message, String label) {
        return ConditionEvaluationResult.leaf(node, context, matched, code, message, label);
    }

    private static String label(ConditionNode node, String displayName) {
        if (node != null && !node.name().isBlank()) {
            return node.name();
        }
        return displayName;
    }

    private static String fieldDisplayName(String field) {
        return switch (field) {
            case "playerId" -> "玩家 UUID";
            case "playerName" -> "玩家名称";
            case "playerOnline" -> "玩家在线状态";
            case "playerOp", "playerIsOp" -> "玩家管理员状态";
            case "playerTags" -> "玩家标签";
            case "playerTeam", "team" -> "玩家队伍";
            case "playerGameMode", "playerGamemode", "gamemode" -> "玩家游戏模式";
            case "playerAlive" -> "玩家存活状态";
            case "world", "worldId" -> "世界";
            case "sourceType" -> "来源类型";
            case "sourceId" -> "来源 ID";
            case "channel" -> "信号频道";
            case "deviceId" -> "设备 ID";
            case "listenerId" -> "监听器 ID";
            case "regionId" -> "区域 ID";
            case "actionId" -> "动作 ID";
            case "gameTime" -> "游戏时间";
            default -> field;
        };
    }

    private static boolean hasPlayer(ConditionEvaluationContext context) {
        return context != null && context.hasPlayerIdentity();
    }

    private static String missingPlayerReason(ConditionEvaluationContext context) {
        return context == null ? "上下文不存在，无法读取触发玩家。" : "触发玩家不存在。";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record AlwaysTrueHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.ALWAYS_TRUE,
                    "永远通过",
                    "测试与占位条件，总是返回通过。",
                    CATEGORY_DEBUG
            );
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            return leaf(node, context, true, "always_true", "条件永远通过。", label(node, "永远通过"));
        }
    }

    private record AlwaysFalseHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.ALWAYS_FALSE,
                    "永远失败",
                    "测试与占位条件，总是返回失败。",
                    CATEGORY_DEBUG
            );
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            return leaf(node, context, false, "always_false", "条件永远失败。", label(node, "永远失败"));
        }
    }

    private record ContextExistsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.CONTEXT_EXISTS,
                    "上下文存在",
                    "检查本次判断是否提供 EvaluationContext。",
                    CATEGORY_CONTEXT
            );
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            boolean matched = context != null;
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "context_exists" : "context_missing",
                    matched ? "上下文存在。" : "上下文不存在。",
                    label(node, "上下文存在")
            );
        }
    }

    private record ContextFieldExistsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.CONTEXT_FIELD_EXISTS,
                    "上下文字段存在",
                    "检查 EvaluationContext 中指定字段是否存在且非空。",
                    CATEGORY_CONTEXT,
                    field("field", "上下文字段", "string", true, "例如 channel、sourceType、variables.flag、event.phase")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "field", "上下文字段");
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String field = config(node, "field");
            String actual = context == null ? "" : context.fieldValue(field);
            boolean matched = !actual.isBlank();
            String display = fieldDisplayName(field);
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "context_field_exists" : "context_field_missing",
                    matched ? "上下文字段存在：" + display + "。" : "上下文字段不存在或为空：" + display + "。",
                    label(node, "上下文字段存在")
            );
        }
    }

    private record ContextEqualsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.CONTEXT_EQUALS,
                    "上下文字段匹配",
                    "检查 EvaluationContext 中指定字段是否等于固定值。",
                    CATEGORY_CONTEXT,
                    field("field", "上下文字段", "string", true, "例如 channel、sourceType、variables.flag、event.phase"),
                    field("expected", "期望值", "string", true, "字段需要匹配的文本")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "field", "上下文字段")
                    .merge(requireNonBlank(node, "expected", "期望值"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String field = config(node, "field");
            String expected = config(node, "expected");
            String actual = context == null ? "" : context.fieldValue(field);
            boolean matched = actual.equals(expected);
            String display = fieldDisplayName(field);
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "context_equals" : "context_not_equal",
                    matched
                            ? "上下文字段匹配：" + display + "。"
                            : "上下文字段不匹配：" + display + "，期望 " + expected + "，实际 " + actual + "。",
                    label(node, "上下文字段匹配")
            );
        }
    }

    private record PlayerExistsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.PLAYER_EXISTS,
                    "触发玩家存在",
                    "检查本次条件判断上下文中是否包含触发玩家身份。",
                    CATEGORY_PLAYER
            );
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            boolean matched = hasPlayer(context);
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "player_exists" : "player_missing",
                    matched ? "触发玩家存在：" + context.playerLabel() + "。" : missingPlayerReason(context),
                    label(node, "触发玩家存在")
            );
        }
    }

    private record PlayerOnlineHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.PLAYER_ONLINE,
                    "玩家在线",
                    "检查触发玩家快照是否确认仍在线。",
                    CATEGORY_PLAYER
            );
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            if (!hasPlayer(context)) {
                return leaf(node, context, false, "player_missing", missingPlayerReason(context), label(node, "玩家在线"));
            }
            if (context.playerOnline() == null) {
                return leaf(node, context, false, "player_online_unknown", "玩家在线状态未知：" + context.playerLabel() + "。", label(node, "玩家在线"));
            }
            boolean matched = context.playerOnline();
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "player_online" : "player_offline",
                    matched ? "玩家在线：" + context.playerLabel() + "。" : "玩家不在线：" + context.playerLabel() + "。",
                    label(node, "玩家在线")
            );
        }
    }

    private record PlayerIsOpHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.PLAYER_IS_OP,
                    "玩家是管理员",
                    "检查触发玩家是否具备 OP / creative level 2 权限。",
                    CATEGORY_PLAYER,
                    field("expected", "期望结果", "boolean", false, "默认 true；设为 false 可判断玩家不是管理员")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireBooleanIfPresent(node, "expected", "期望结果");
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            if (!hasPlayer(context)) {
                return leaf(node, context, false, "player_missing", missingPlayerReason(context), label(node, "玩家是管理员"));
            }
            if (context.playerOp() == null) {
                return leaf(node, context, false, "player_op_unknown", "玩家管理员状态未知：" + context.playerLabel() + "。", label(node, "玩家是管理员"));
            }
            boolean expected = configBoolean(node, "expected", true);
            boolean matched = context.playerOp() == expected;
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "player_op_matched" : "player_op_mismatch",
                    matched
                            ? "玩家管理员状态匹配：" + context.playerLabel() + "。"
                            : "玩家管理员状态不匹配：期望 " + expected + "，实际 " + context.playerOp() + "。",
                    label(node, "玩家是管理员")
            );
        }
    }

    private record PlayerTagHandler(String type, String displayName, boolean shouldHaveTag) implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    type,
                    displayName,
                    shouldHaveTag ? "检查触发玩家是否拥有指定标签。" : "检查触发玩家是否没有指定标签。",
                    CATEGORY_PLAYER,
                    field("tag", "标签", "string", true, "Minecraft command tag，精确匹配")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "tag", "标签");
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String tag = config(node, "tag");
            if (!hasPlayer(context)) {
                return leaf(node, context, false, "player_missing", missingPlayerReason(context), label(node, displayName));
            }
            boolean hasTag = context.hasPlayerTag(tag);
            boolean matched = shouldHaveTag ? hasTag : !hasTag;
            String code = shouldHaveTag
                    ? (matched ? "player_has_tag" : "player_missing_tag")
                    : (matched ? "player_lacks_tag" : "player_unexpected_tag");
            String message = matched
                    ? "玩家标签匹配：" + context.playerLabel() + "，标签 " + tag + "。"
                    : (shouldHaveTag
                            ? "玩家缺少标签：" + tag + "。"
                            : "玩家不应拥有标签但实际拥有：" + tag + "。");
            return leaf(node, context, matched, code, message, label(node, displayName));
        }
    }

    private record PlayerTeamEqualsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.PLAYER_TEAM_EQUALS,
                    "玩家队伍匹配",
                    "检查触发玩家所在 scoreboard team 名称是否匹配。",
                    CATEGORY_PLAYER,
                    field("team", "队伍", "string", true, "期望的队伍名称或 team id")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "team", "队伍");
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String expected = config(node, "team");
            if (!hasPlayer(context)) {
                return leaf(node, context, false, "player_missing", missingPlayerReason(context), label(node, "玩家队伍匹配"));
            }
            String actual = context.playerTeam();
            boolean matched = actual.equals(expected);
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "player_team_equals" : "player_team_mismatch",
                    matched
                            ? "玩家队伍匹配：" + expected + "。"
                            : "玩家队伍不匹配：期望 " + expected + "，实际 " + (actual.isBlank() ? "无队伍" : actual) + "。",
                    label(node, "玩家队伍匹配")
            );
        }
    }

    private record PlayerGamemodeEqualsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.PLAYER_GAMEMODE_EQUALS,
                    "玩家游戏模式匹配",
                    "检查触发玩家当前游戏模式是否匹配。",
                    CATEGORY_PLAYER,
                    field("gamemode", "游戏模式", "enum:survival,creative,adventure,spectator", true, "survival / creative / adventure / spectator")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            ConditionValidationResult result = requireNonBlank(node, "gamemode", "游戏模式");
            String value = config(node, "gamemode").toLowerCase(Locale.ROOT);
            if (!value.isBlank() && !GAME_MODES.contains(value)) {
                result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_gamemode", "游戏模式必须是 survival / creative / adventure / spectator"));
            }
            return result;
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String expected = config(node, "gamemode").toLowerCase(Locale.ROOT);
            if (!hasPlayer(context)) {
                return leaf(node, context, false, "player_missing", missingPlayerReason(context), label(node, "玩家游戏模式匹配"));
            }
            String actual = context.playerGameMode();
            boolean matched = actual.equals(expected);
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "player_gamemode_equals" : "player_gamemode_mismatch",
                    matched
                            ? "玩家游戏模式匹配：" + expected + "。"
                            : "玩家游戏模式不匹配：期望 " + expected + "，实际 " + (actual.isBlank() ? "未知" : actual) + "。",
                    label(node, "玩家游戏模式匹配")
            );
        }
    }

    private record PlayerAliveHandler(String type, String displayName, boolean shouldBeAlive) implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    type,
                    displayName,
                    shouldBeAlive ? "检查触发玩家是否处于存活状态。" : "检查触发玩家是否处于死亡状态。",
                    CATEGORY_PLAYER
            );
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            if (!hasPlayer(context)) {
                return leaf(node, context, false, "player_missing", missingPlayerReason(context), label(node, displayName));
            }
            if (context.playerAlive() == null) {
                return leaf(node, context, false, "player_alive_unknown", "玩家存活状态未知：" + context.playerLabel() + "。", label(node, displayName));
            }
            boolean matched = context.playerAlive() == shouldBeAlive;
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? (shouldBeAlive ? "player_alive" : "player_dead") : "player_alive_mismatch",
                    matched
                            ? "玩家存活状态匹配：" + context.playerLabel() + "。"
                            : "玩家存活状态不匹配：期望 " + (shouldBeAlive ? "存活" : "死亡") + "，实际 " + (context.playerAlive() ? "存活" : "死亡") + "。",
                    label(node, displayName)
            );
        }
    }

    private record ContextIdEqualsHandler(String type, String displayName, String fieldName, String fieldDisplayName, String description) implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    type,
                    displayName,
                    description,
                    CATEGORY_CONTEXT,
                    field(fieldName, fieldDisplayName, "string", true, "期望匹配的" + fieldDisplayName)
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, fieldName, fieldDisplayName);
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String expected = config(node, fieldName);
            String actual = context == null ? "" : context.fieldValue(fieldName);
            boolean matched = actual.equals(expected);
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? type : type + "_mismatch",
                    matched
                            ? fieldDisplayName + "匹配：" + expected + "。"
                            : (actual.isBlank()
                                    ? "上下文缺少" + fieldDisplayName + "。"
                                    : fieldDisplayName + "不匹配：期望 " + expected + "，实际 " + actual + "。"),
                    label(node, displayName)
            );
        }
    }

    private record ChannelEqualsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.CHANNEL_EQUALS,
                    "信号频道匹配",
                    "检查触发信号频道是否匹配，期望值会按 SignalChannel 规则 normalize。",
                    CATEGORY_CONTEXT,
                    field("channel", "信号频道", "signal-channel", true, "期望匹配的信号频道")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            ConditionValidationResult result = requireNonBlank(node, "channel", "信号频道");
            String expected = SignalChannel.normalize(config(node, "channel"));
            if (!expected.isBlank() && !SignalChannel.isValid(expected)) {
                result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_channel", "信号频道无效：" + config(node, "channel")));
            }
            return result;
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String expected = SignalChannel.normalize(config(node, "channel"));
            String actual = SignalChannel.normalize(context == null ? "" : context.channel());
            boolean matched = !expected.isBlank() && actual.equals(expected);
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "channel_equals" : "channel_mismatch",
                    matched
                            ? "信号频道匹配：" + expected + "。"
                            : (actual.isBlank()
                                    ? "上下文缺少信号频道。"
                                    : "信号频道不匹配：期望 " + expected + "，实际 " + actual + "。"),
                    label(node, "信号频道匹配")
            );
        }
    }

    private record GameTimeCompareHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.GAME_TIME_COMPARE,
                    "游戏时间比较",
                    "比较 EvaluationContext.gameTime 与目标 tick。",
                    CATEGORY_TIME,
                    field("operator", "比较方式", "enum:eq,ne,gt,gte,lt,lte", true, "eq/ne/gt/gte/lt/lte"),
                    field("value", "目标 tick", "long", true, "用于比较的目标游戏时间 tick")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            ConditionValidationResult result = requireNonBlank(node, "operator", "比较方式")
                    .merge(requireNonBlank(node, "value", "目标 tick"));
            String operator = config(node, "operator").toLowerCase(Locale.ROOT);
            if (!operator.isBlank() && !GAME_TIME_OPERATORS.contains(operator)) {
                result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_operator", "比较方式必须是 eq/ne/gt/gte/lt/lte"));
            }
            String value = config(node, "value");
            if (!value.isBlank()) {
                try {
                    Long.parseLong(value);
                } catch (NumberFormatException exception) {
                    result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_game_time", "目标 tick 必须是数字"));
                }
            }
            return result;
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String operator = config(node, "operator").toLowerCase(Locale.ROOT);
            long expected;
            try {
                expected = Long.parseLong(config(node, "value"));
            } catch (NumberFormatException exception) {
                return leaf(node, context, false, "game_time_config_invalid", "游戏时间配置无效：" + config(node, "value") + "。", label(node, "游戏时间比较"));
            }
            if (context == null) {
                return leaf(node, null, false, "game_time_context_missing", "上下文不存在，无法读取游戏时间。", label(node, "游戏时间比较"));
            }
            long actual = context.gameTime();
            boolean matched = switch (operator) {
                case "eq" -> actual == expected;
                case "ne" -> actual != expected;
                case "gt" -> actual > expected;
                case "gte" -> actual >= expected;
                case "lt" -> actual < expected;
                case "lte" -> actual <= expected;
                default -> false;
            };
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "game_time_compare" : "game_time_compare_failed",
                    matched
                            ? "游戏时间满足：当前 " + actual + "，要求 " + operator + " " + expected + "。"
                            : "游戏时间不满足：当前 " + actual + "，要求 " + operator + " " + expected + "。",
                    label(node, "游戏时间比较")
            );
        }
    }

    private record EventMetadataExistsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.EVENT_METADATA_EXISTS,
                    "事件元数据存在",
                    "检查事件元数据中是否存在指定 key 且值非空。",
                    CATEGORY_METADATA,
                    field("key", "元数据键", "string", true, "事件元数据 key")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "key", "元数据键");
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "key");
            String actual = context == null ? "" : context.eventMetadata().getOrDefault(key, "");
            boolean matched = !actual.isBlank();
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "event_metadata_exists" : "event_metadata_missing",
                    matched ? "事件元数据存在：" + key + "。" : "事件元数据 key 不存在：" + key + "。",
                    label(node, "事件元数据存在")
            );
        }
    }

    private record EventMetadataEqualsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return ConditionRegistry.metadata(
                    ConditionNodeType.EVENT_METADATA_EQUALS,
                    "事件元数据匹配",
                    "检查事件元数据指定 key 的值是否匹配。",
                    CATEGORY_METADATA,
                    field("key", "元数据键", "string", true, "事件元数据 key"),
                    field("value", "期望值", "string", true, "期望匹配的元数据值")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return requireNonBlank(node, "key", "元数据键")
                    .merge(requireNonBlank(node, "value", "期望值"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            String key = config(node, "key");
            String expected = config(node, "value");
            String actual = context == null ? "" : context.eventMetadata().getOrDefault(key, "");
            boolean matched = actual.equals(expected);
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "event_metadata_equals" : "event_metadata_mismatch",
                    matched
                            ? "事件元数据匹配：" + key + "。"
                            : (actual.isBlank()
                                    ? "事件元数据 key 不存在：" + key + "。"
                                    : "事件元数据不匹配：" + key + "，期望 " + expected + "，实际 " + actual + "。"),
                    label(node, "事件元数据匹配")
            );
        }
    }

    private record StateVariableExistsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return stateVariableMetadata(
                    ConditionNodeType.STATE_VARIABLE_EXISTS,
                    "状态变量存在",
                    "检查指定作用域和目标上的状态变量是否存在。"
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return validateStateVariableBase(node);
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            ResolvedStateVariableTarget target = resolveStateVariableTarget(node, context);
            if (!target.valid()) {
                return leaf(node, context, false, "state_variable_target_missing", target.failureReason(), label(node, "状态变量存在"));
            }
            Optional<StateVariableRecord> record = context == null
                    ? Optional.empty()
                    : context.stateVariables().get(target.scope(), target.targetId(), target.key());
            boolean matched = record.isPresent();
            String path = target.displayPath();
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "state_variable_exists" : "state_variable_missing",
                    matched ? "状态变量存在：" + path + "。" : "状态变量不存在：" + path + "。",
                    label(node, "状态变量存在")
            );
        }
    }

    private record StateVariableBoolEqualsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return stateVariableMetadata(
                    ConditionNodeType.STATE_VARIABLE_BOOL_EQUALS,
                    "布尔状态匹配",
                    "检查布尔状态变量是否等于期望值。",
                    field("expected", "期望值", "boolean", true, "true 或 false")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return validateStateVariableBase(node)
                    .merge(requireNonBlank(node, "expected", "期望值"))
                    .merge(requireBooleanIfPresent(node, "expected", "期望值"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            ResolvedStateVariableTarget target = resolveStateVariableTarget(node, context);
            if (!target.valid()) {
                return leaf(node, context, false, "state_variable_target_missing", target.failureReason(), label(node, "布尔状态匹配"));
            }
            Optional<StateVariableRecord> optionalRecord = findStateVariable(node, context, target, StateVariableType.BOOLEAN, "布尔状态匹配");
            if (optionalRecord.isEmpty()) {
                return missingOrTypeMismatch(node, context, target, StateVariableType.BOOLEAN, "布尔状态匹配");
            }
            boolean expected = Boolean.parseBoolean(config(node, "expected"));
            boolean actual = Boolean.parseBoolean(optionalRecord.get().value());
            boolean matched = actual == expected;
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "state_variable_bool_equals" : "state_variable_bool_mismatch",
                    matched
                            ? "布尔状态匹配：" + target.displayPath() + " = " + actual + "。"
                            : "布尔状态不匹配：" + target.displayPath() + " 当前 " + actual + "，期望 " + expected + "。",
                    label(node, "布尔状态匹配")
            );
        }
    }

    private record StateVariableIntCompareHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return stateVariableMetadata(
                    ConditionNodeType.STATE_VARIABLE_INT_COMPARE,
                    "整数状态比较",
                    "比较整数状态变量与目标整数。",
                    field("operator", "比较方式", "enum:eq,ne,gt,gte,lt,lte", true, "eq/ne/gt/gte/lt/lte"),
                    field("value", "目标整数", "long", true, "用于比较的目标整数")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            ConditionValidationResult result = validateStateVariableBase(node)
                    .merge(requireNonBlank(node, "operator", "比较方式"))
                    .merge(requireNonBlank(node, "value", "目标整数"));
            if (!config(node, "operator").isBlank() && StateVariableCompareOperator.parse(config(node, "operator")).isEmpty()) {
                result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_operator", "比较方式必须是 eq/ne/gt/gte/lt/lte"));
            }
            if (!config(node, "value").isBlank()) {
                try {
                    Long.parseLong(config(node, "value"));
                } catch (NumberFormatException ex) {
                    result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_state_integer", "目标整数必须是数字"));
                }
            }
            return result;
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            ResolvedStateVariableTarget target = resolveStateVariableTarget(node, context);
            if (!target.valid()) {
                return leaf(node, context, false, "state_variable_target_missing", target.failureReason(), label(node, "整数状态比较"));
            }
            Optional<StateVariableRecord> optionalRecord = findStateVariable(node, context, target, StateVariableType.INTEGER, "整数状态比较");
            if (optionalRecord.isEmpty()) {
                return missingOrTypeMismatch(node, context, target, StateVariableType.INTEGER, "整数状态比较");
            }
            StateVariableCompareOperator operator = StateVariableCompareOperator.parse(config(node, "operator")).orElse(StateVariableCompareOperator.EQ);
            long expected = Long.parseLong(config(node, "value"));
            long actual = Long.parseLong(optionalRecord.get().value());
            boolean matched = operator.test(actual, expected);
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "state_variable_int_compare" : "state_variable_int_compare_failed",
                    matched
                            ? "整数状态满足：" + target.displayPath() + " 当前 " + actual + "，要求 " + operator.symbol() + " " + expected + "。"
                            : "整数状态不满足：" + target.displayPath() + " 当前 " + actual + "，要求 " + operator.symbol() + " " + expected + "。",
                    label(node, "整数状态比较")
            );
        }
    }

    private record StateVariableStringEqualsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return stateVariableMetadata(
                    ConditionNodeType.STATE_VARIABLE_STRING_EQUALS,
                    "文本状态匹配",
                    "检查文本状态变量是否等于期望文本。",
                    field("value", "期望文本", "string", true, "需要精确匹配的文本"),
                    field("ignoreCase", "忽略大小写", "boolean", false, "默认 false")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return validateStateVariableBase(node)
                    .merge(requireNonBlank(node, "value", "期望文本"))
                    .merge(requireBooleanIfPresent(node, "ignoreCase", "忽略大小写"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            ResolvedStateVariableTarget target = resolveStateVariableTarget(node, context);
            if (!target.valid()) {
                return leaf(node, context, false, "state_variable_target_missing", target.failureReason(), label(node, "文本状态匹配"));
            }
            Optional<StateVariableRecord> optionalRecord = findStateVariable(node, context, target, StateVariableType.STRING, "文本状态匹配");
            if (optionalRecord.isEmpty()) {
                return missingOrTypeMismatch(node, context, target, StateVariableType.STRING, "文本状态匹配");
            }
            boolean ignoreCase = configBoolean(node, "ignoreCase", false);
            String expected = config(node, "value");
            String actual = optionalRecord.get().value();
            boolean matched = ignoreCase ? actual.equalsIgnoreCase(expected) : actual.equals(expected);
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "state_variable_string_equals" : "state_variable_string_mismatch",
                    matched
                            ? "文本状态匹配：" + target.displayPath() + "。"
                            : "文本状态不匹配：" + target.displayPath() + "，期望 " + expected + "，实际 " + actual + "。",
                    label(node, "文本状态匹配")
            );
        }
    }

    private record StateVariableStringContainsHandler() implements ConditionTypeHandler {
        @Override
        public ConditionTypeMetadata metadata() {
            return stateVariableMetadata(
                    ConditionNodeType.STATE_VARIABLE_STRING_CONTAINS,
                    "文本状态包含",
                    "检查文本状态变量是否包含指定文本。",
                    field("value", "包含文本", "string", true, "需要包含的文本"),
                    field("ignoreCase", "忽略大小写", "boolean", false, "默认 false")
            );
        }

        @Override
        public ConditionValidationResult validate(ConditionNode node) {
            return validateStateVariableBase(node)
                    .merge(requireNonBlank(node, "value", "包含文本"))
                    .merge(requireBooleanIfPresent(node, "ignoreCase", "忽略大小写"));
        }

        @Override
        public ConditionEvaluationResult evaluate(ConditionNode node, ConditionEvaluationContext context) {
            ResolvedStateVariableTarget target = resolveStateVariableTarget(node, context);
            if (!target.valid()) {
                return leaf(node, context, false, "state_variable_target_missing", target.failureReason(), label(node, "文本状态包含"));
            }
            Optional<StateVariableRecord> optionalRecord = findStateVariable(node, context, target, StateVariableType.STRING, "文本状态包含");
            if (optionalRecord.isEmpty()) {
                return missingOrTypeMismatch(node, context, target, StateVariableType.STRING, "文本状态包含");
            }
            boolean ignoreCase = configBoolean(node, "ignoreCase", false);
            String expected = config(node, "value");
            String actual = optionalRecord.get().value();
            boolean matched = ignoreCase
                    ? actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT))
                    : actual.contains(expected);
            return leaf(
                    node,
                    context,
                    matched,
                    matched ? "state_variable_string_contains" : "state_variable_string_contains_failed",
                    matched
                            ? "文本状态包含：" + target.displayPath() + " 包含 " + expected + "。"
                            : "文本状态不包含：" + target.displayPath() + "，期望包含 " + expected + "，实际 " + actual + "。",
                    label(node, "文本状态包含")
            );
        }
    }

    private static ConditionTypeMetadata stateVariableMetadata(String type, String displayName, String description, ConditionFieldSchema... extraFields) {
        java.util.ArrayList<ConditionFieldSchema> fields = new java.util.ArrayList<>();
        fields.add(field("scope", "作用域", "enum:GLOBAL,PLAYER", true, "GLOBAL=全局，PLAYER=玩家"));
        fields.add(field("key", "变量键", "string", true, "例如 game.active、player.certified、mission.phase"));
        fields.add(field("targetMode", "目标模式", "enum:global,context_player,explicit_target", true, "global=全局，context_player=触发玩家，explicit_target=显式目标"));
        fields.add(field("targetId", "显式目标 ID", "string", false, "targetMode=explicit_target 时必填"));
        fields.addAll(Arrays.asList(extraFields));
        return metadata(type, displayName, description, CATEGORY_STATE, fields.toArray(new ConditionFieldSchema[0]));
    }

    private static ConditionValidationResult validateStateVariableBase(ConditionNode node) {
        ConditionValidationResult result = requireNonBlank(node, "scope", "作用域")
                .merge(requireNonBlank(node, "key", "变量键"))
                .merge(requireNonBlank(node, "targetMode", "目标模式"));
        StateVariableScope scope = StateVariableScope.parse(config(node, "scope")).orElse(null);
        StateVariableTargetMode targetMode = StateVariableTargetMode.parse(config(node, "targetMode")).orElse(null);
        if (!config(node, "scope").isBlank() && scope == null) {
            result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_scope", "状态变量作用域必须是 GLOBAL 或 PLAYER"));
        }
        if (!config(node, "targetMode").isBlank() && targetMode == null) {
            result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_target_mode", "目标模式必须是 global、context_player 或 explicit_target"));
        }
        if (scope == StateVariableScope.GLOBAL && targetMode != null && targetMode != StateVariableTargetMode.GLOBAL) {
            result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_target_mode", "GLOBAL 作用域必须使用 global 目标模式"));
        }
        if (scope == StateVariableScope.PLAYER && targetMode == StateVariableTargetMode.GLOBAL) {
            result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_invalid_target_mode", "PLAYER 作用域不能使用 global 目标模式"));
        }
        for (StateVariableValidation.Issue issue : StateVariableValidation.validateKeyOnly(
                scope == null ? StateVariableScope.GLOBAL : scope,
                targetMode == StateVariableTargetMode.EXPLICIT_TARGET ? config(node, "targetId") : (scope == StateVariableScope.GLOBAL ? "global" : "placeholder"),
                config(node, "key")
        )) {
            if ("missing_player_target".equals(issue.code()) && targetMode == StateVariableTargetMode.CONTEXT_PLAYER) {
                continue;
            }
            result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_" + issue.code(), issue.message()));
        }
        if (targetMode == StateVariableTargetMode.EXPLICIT_TARGET && config(node, "targetId").isBlank()) {
            result = result.merge(ConditionValidationResult.error(node == null ? "" : node.id(), "", "condition_config_missing_target_id", "显式目标模式必须填写目标 ID"));
        }
        return result;
    }

    private static ResolvedStateVariableTarget resolveStateVariableTarget(ConditionNode node, ConditionEvaluationContext context) {
        StateVariableScope scope = StateVariableScope.parse(config(node, "scope")).orElse(StateVariableScope.GLOBAL);
        StateVariableTargetMode targetMode = StateVariableTargetMode.parse(config(node, "targetMode")).orElse(StateVariableTargetMode.GLOBAL);
        String key = StateVariableValidation.normalizeKey(config(node, "key"));
        if (scope == StateVariableScope.GLOBAL) {
            return ResolvedStateVariableTarget.valid(scope, StateVariableValidation.GLOBAL_TARGET, key);
        }
        if (targetMode == StateVariableTargetMode.EXPLICIT_TARGET) {
            return ResolvedStateVariableTarget.valid(scope, StateVariableValidation.normalizeTargetId(scope, config(node, "targetId")), key);
        }
        if (context == null || !context.hasPlayerIdentity()) {
            return ResolvedStateVariableTarget.invalid(scope, "", key, "上下文缺少触发玩家，无法读取玩家状态变量：" + key + "。");
        }
        String targetId = !context.playerId().isBlank() ? context.playerId() : context.playerName();
        return ResolvedStateVariableTarget.valid(scope, targetId, key);
    }

    private static Optional<StateVariableRecord> findStateVariable(
            ConditionNode node,
            ConditionEvaluationContext context,
            ResolvedStateVariableTarget target,
            StateVariableType expectedType,
            String displayName
    ) {
        if (context == null) {
            return Optional.empty();
        }
        Optional<StateVariableRecord> record = context.stateVariables().get(target.scope(), target.targetId(), target.key());
        if (record.isEmpty() || record.get().type() != expectedType) {
            return Optional.empty();
        }
        return record;
    }

    private static ConditionEvaluationResult missingOrTypeMismatch(
            ConditionNode node,
            ConditionEvaluationContext context,
            ResolvedStateVariableTarget target,
            StateVariableType expectedType,
            String displayName
    ) {
        if (context == null) {
            return leaf(node, context, false, "state_variable_context_missing", "上下文不存在，无法读取状态变量：" + target.displayPath() + "。", label(node, displayName));
        }
        Optional<StateVariableRecord> record = context.stateVariables().get(target.scope(), target.targetId(), target.key());
        if (record.isEmpty()) {
            return leaf(node, context, false, "state_variable_missing", "状态变量不存在：" + target.displayPath() + "。", label(node, displayName));
        }
        return leaf(
                node,
                context,
                false,
                "state_variable_type_mismatch",
                "状态变量类型不匹配：" + target.displayPath() + " 期望 " + expectedType.displayName() + "，实际 " + record.get().type().displayName() + "。",
                label(node, displayName)
        );
    }

    private record ResolvedStateVariableTarget(
            boolean valid,
            StateVariableScope scope,
            String targetId,
            String key,
            String failureReason
    ) {
        static ResolvedStateVariableTarget valid(StateVariableScope scope, String targetId, String key) {
            return new ResolvedStateVariableTarget(true, scope, targetId, key, "");
        }

        static ResolvedStateVariableTarget invalid(StateVariableScope scope, String targetId, String key, String failureReason) {
            return new ResolvedStateVariableTarget(false, scope, targetId, key, failureReason);
        }

        String displayPath() {
            return new StateVariableKey(scope, targetId, key).displayPath();
        }
    }
}
