package com.zcpu.tzzmod.action.schema;

import com.zcpu.tzzmod.action.ActionType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ActionSchemaRegistry {
    // Typed Action schema 只描述“配置结构”和“编辑/校验/摘要提示”，不执行 action。
    // 运行时仍由现有 ActionEngine 和各 owner adapter 负责；本 registry 不能读取 world/server/store，
    // 也不能把未知 action type fallback 成 command，否则会破坏 Phase 2 fail-closed validation 目标。
    private static final long MAX_ACTION_COOLDOWN_TICKS = 72_000L;
    private static final int MAX_STATE_VALUE_LENGTH = 512;
    private static final long MAX_TIMER_DURATION_OVERRIDE_TICKS = 1_728_000L;

    private static final Set<ActionOwnerType> CURRENT_ACTION_OWNERS = Collections.unmodifiableSet(EnumSet.of(
            ActionOwnerType.SIGNAL_LISTENER,
            ActionOwnerType.ACTION_RELAY,
            ActionOwnerType.REGION_ENTER,
            ActionOwnerType.REGION_EXIT,
            ActionOwnerType.REGION_STAY,
            ActionOwnerType.TIMER_START,
            ActionOwnerType.TIMER_TICK,
            ActionOwnerType.TIMER_COMPLETE,
            ActionOwnerType.TIMER_CANCEL
    ));
    private static final Set<ActionType> CURRENT_ACTION_TYPES = Collections.unmodifiableSet(EnumSet.allOf(ActionType.class));
    private static final Map<ActionType, ActionSchema> SCHEMAS_BY_TYPE = buildSchemasByType();
    private static final List<ActionSchema> SCHEMAS = List.copyOf(SCHEMAS_BY_TYPE.values());
    private static final List<ActionCapability> CAPABILITIES = buildCapabilities();
    private static final Map<ActionOwnerType, Set<ActionType>> ACTION_TYPES_BY_OWNER = buildActionTypesByOwner();

    private ActionSchemaRegistry() {
    }

    public static Optional<ActionSchema> find(ActionType actionType) {
        return actionType == null ? Optional.empty() : Optional.ofNullable(SCHEMAS_BY_TYPE.get(actionType));
    }

    public static Optional<ActionSchema> findById(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return Optional.empty();
        }
        String id = rawId.trim();
        for (ActionSchema schema : SCHEMAS) {
            if (schema.id().equalsIgnoreCase(id)) {
                return Optional.of(schema);
            }
        }
        return Optional.empty();
    }

    public static ActionSchema require(ActionType actionType) {
        return find(actionType).orElseThrow(() -> new IllegalArgumentException("Unknown action type: " + actionType));
    }

    public static List<ActionSchema> schemas() {
        return SCHEMAS;
    }

    public static Map<ActionType, ActionSchema> schemasByType() {
        return SCHEMAS_BY_TYPE;
    }

    public static Set<ActionOwnerType> ownerTypes() {
        return CURRENT_ACTION_OWNERS;
    }

    public static List<ActionCapability> capabilities() {
        return CAPABILITIES;
    }

    public static Set<ActionType> actionTypesForOwner(ActionOwnerType ownerType) {
        return ownerType == null ? Set.of() : ACTION_TYPES_BY_OWNER.getOrDefault(ownerType, Set.of());
    }

    private static Map<ActionType, ActionSchema> buildSchemasByType() {
        EnumMap<ActionType, ActionSchema> schemas = new EnumMap<>(ActionType.class);
        register(schemas, commandSchema());
        register(schemas, messageSchema());
        register(schemas, soundSchema());
        register(schemas, signalSchema());
        register(schemas, stateVariableSchema());
        register(schemas, timerStartSchema());
        register(schemas, timerCancelSchema());
        return Collections.unmodifiableMap(schemas);
    }

    private static void register(EnumMap<ActionType, ActionSchema> schemas, ActionSchema schema) {
        ActionSchema previous = schemas.put(schema.actionType(), schema);
        if (previous != null) {
            throw new IllegalStateException("Duplicate action schema: " + schema.actionType());
        }
    }

    private static List<ActionCapability> buildCapabilities() {
        List<ActionCapability> capabilities = new ArrayList<>();
        for (ActionOwnerType ownerType : CURRENT_ACTION_OWNERS) {
            capabilities.add(new ActionCapability(
                    ownerType,
                    CURRENT_ACTION_TYPES,
                    ownerType.displayName(),
                    "Phase 1 只记录当前 ActionConfig owner 可承载的动作类型；真实 fail-closed 校验在 Phase 2 接入。"
            ));
        }
        return List.copyOf(capabilities);
    }

    private static Map<ActionOwnerType, Set<ActionType>> buildActionTypesByOwner() {
        EnumMap<ActionOwnerType, Set<ActionType>> owners = new EnumMap<>(ActionOwnerType.class);
        for (ActionCapability capability : CAPABILITIES) {
            owners.put(capability.ownerType(), capability.actionTypes());
        }
        return Collections.unmodifiableMap(owners);
    }

    private static ActionSchema commandSchema() {
        List<ActionFieldSchema> fields = commonFields(true);
        fields.add(textarea(
                "value",
                "命令",
                "保存时会去掉开头的 /，执行仍由旧 ActionEngine 命令路径负责。",
                true,
                "",
                512,
                "使用现有命令输入框；危险命令确认和 OP 策略属于后续 validation 阶段。",
                "执行命令 /xxx，摘要必须保留现有审计脱敏边界。"
        ));
        return schema(
                ActionType.COMMAND,
                "执行命令",
                "执行一条服务器命令。",
                "仅描述现有 command action 的配置字段，不改变命令解析、权限或执行顺序。",
                fields,
                false,
                "命令编辑器需要显示中文风险提示，不能把 raw JSON 作为主编辑入口。",
                "主摘要格式为执行命令 /xxx。"
        );
    }

    private static ActionSchema messageSchema() {
        List<ActionFieldSchema> fields = commonFields(true);
        fields.add(textarea(
                "value",
                "消息文本",
                "有玩家上下文时发给触发玩家，否则沿用旧逻辑广播。",
                true,
                "",
                500,
                "Phase 1 不做 Rich Text Builder，只登记纯文本消息字段。",
                "主摘要格式为发送消息 xxx。"
        ));
        return schema(
                ActionType.MESSAGE,
                "发送消息",
                "向触发玩家或在线玩家发送纯文本消息。",
                "消息 action 仍使用旧 runtime 文本发送逻辑；富文本构建器 deferred。",
                fields,
                false,
                "使用 textarea，不引入 tellraw / title / actionbar 新 runtime。",
                "主摘要格式为向玩家显示消息 xxx。"
        );
    }

    private static ActionSchema soundSchema() {
        List<ActionFieldSchema> fields = commonFields(true);
        fields.add(text(
                "value",
                "音效标识",
                "兼容旧配置字段；当前 runtime 仍播放固定经验球反馈音效。",
                true,
                "",
                128,
                "UI 可以保留 sound id 输入，但必须提示当前不是完整自定义音效 runtime。",
                "主摘要格式为播放音效 xxx，并标明 legacy caveat。"
        ));
        return schema(
                ActionType.SOUND,
                "播放音效",
                "播放旧反馈音效动作。",
                "Phase 1 只登记现有字段；不把 value 扩展为新的自定义音效执行语义。",
                fields,
                false,
                "保留 legacy 说明，避免 UI 暗示已支持完整 sound id。",
                "主摘要格式为播放音效 xxx。"
        );
    }

    private static ActionSchema signalSchema() {
        List<ActionFieldSchema> fields = commonFields(true);
        fields.add(new ActionFieldSchema(
                "value",
                "目标频道",
                "SignalBridge channel，保存前必须符合现有频道命名规则。",
                ActionFieldType.CHANNEL_PICKER,
                true,
                "",
                128,
                null,
                null,
                List.of(),
                "使用 channel picker；不存在的频道处理策略由后续 validation/capability 阶段统一。",
                "主摘要格式为发送信号到频道 xxx。"
        ));
        return schema(
                ActionType.SIGNAL,
                "发送信号",
                "向 SignalBridge 频道发出信号。",
                "只描述 channel 字段，不改变 SignalBridge fan-out 顺序或深度语义。",
                fields,
                false,
                "频道选择器必须保持旧保存 payload 的 value 字段。",
                "主摘要格式为发送信号到频道 xxx。"
        );
    }

    private static ActionSchema stateVariableSchema() {
        List<ActionFieldSchema> fields = commonFields(false);
        fields.add(select(
                "stateOperation",
                "状态操作",
                "选择状态变量 mutation 操作。",
                true,
                "set_variable",
                List.of(
                        option("set_variable", "设置变量"),
                        option("increment_variable", "增加整数变量"),
                        option("decrement_variable", "减少整数变量"),
                        option("toggle_boolean", "切换布尔变量"),
                        option("clear_variable", "清除变量")
                ),
                "不同操作的字段启用规则由后续 typed validation/editor 统一。",
                "摘要优先显示操作名称。"
        ));
        fields.add(select(
                "stateScope",
                "状态作用域",
                "选择全局状态或玩家状态。",
                true,
                "GLOBAL",
                List.of(option("GLOBAL", "全局"), option("PLAYER", "玩家")),
                "保存 payload 仍使用现有 StateVariableScope 名称。",
                "摘要显示全局或玩家。"
        ));
        fields.add(select(
                "stateTargetMode",
                "目标模式",
                "选择状态变量作用目标。",
                true,
                "global",
                targetModeOptions(),
                "explicit_target 需要填写 stateTargetId；后端 validation 必须 authoritative。",
                "摘要显示目标模式。"
        ));
        fields.add(text("stateTargetId", "指定目标", "显式玩家目标 ID；非 explicit_target 时可为空。", false, "", 64, "使用玩家目标输入或 picker。", "通常作为副摘要。"));
        fields.add(new ActionFieldSchema(
                "stateKey",
                "状态键",
                "StateVariable key，保存时沿用现有 key normalization。",
                ActionFieldType.STATE_VARIABLE_PICKER,
                true,
                "",
                96,
                null,
                null,
                List.of(),
                "使用 state variable picker 或文本输入。",
                "主摘要显示 key。"
        ));
        fields.add(select(
                "stateValueType",
                "值类型",
                "选择写入值类型。",
                true,
                "BOOLEAN",
                List.of(option("BOOLEAN", "布尔"), option("INTEGER", "整数"), option("STRING", "文本")),
                "后续 editor 根据类型切换输入控件。",
                "摘要显示值类型。"
        ));
        fields.add(text("stateValue", "写入值", "set_variable 使用的目标值。", false, "", MAX_STATE_VALUE_LENGTH, "按 stateValueType 渲染输入控件。", "设置操作显示该值。"));
        fields.add(number("stateDelta", "增减量", "increment/decrement 使用的整数变化量。", false, "1", 1L, null, "只在增减整数变量时编辑。", "增减操作显示 delta。"));
        fields.add(bool("stateCreateIfMissing", "不存在时创建", "变量不存在时是否创建初始值。", false, "false", "用 checkbox 表示。", "作为副摘要。"));
        fields.add(text("stateInitialValue", "初始值", "create-if-missing 使用的初始值。", false, "", MAX_STATE_VALUE_LENGTH, "仅在允许创建时显示。", "通常作为副摘要。"));
        return schema(
                ActionType.STATE_VARIABLE,
                "修改状态变量",
                "通过受控 StateVariable mutation 修改全局或玩家状态。",
                "schema 只描述 mutation 请求字段；实际写入仍由现有状态变量服务执行。",
                fields,
                true,
                "需要状态变量 picker 和目标模式控件；requiresOp/notifyOps 会被 ActionConfig canonical 归零，不作为可编辑字段。",
                "主摘要格式为设置状态变量 xxx = yyy 或对应操作。"
        );
    }

    private static ActionSchema timerStartSchema() {
        List<ActionFieldSchema> fields = commonFields(false);
        addTimerBaseFields(fields);
        fields.add(select(
                "timerStartPolicyOverride",
                "启动策略覆盖",
                "为空时使用 Timer 定义上的 startPolicy。",
                false,
                "",
                List.of(
                        option("", "使用 Timer 定义"),
                        option("RESTART", "重新开始"),
                        option("IGNORE_IF_RUNNING", "运行中则忽略"),
                        option("FAIL_IF_RUNNING", "运行中则失败")
                ),
                "只对 timer_start 显示。",
                "摘要显示覆盖策略或使用 Timer 定义。"
        ));
        fields.add(number(
                "timerDurationOverrideTicks",
                "时长覆盖 ticks",
                "0 表示使用 Timer 定义的 durationTicks。",
                false,
                "0",
                0L,
                MAX_TIMER_DURATION_OVERRIDE_TICKS,
                "只对 timer_start 显示。",
                "作为 timer_start 副摘要。"
        ));
        return schema(
                ActionType.TIMER_START,
                "启动 Timer",
                "通过 Scheduler 启动已有 Timer。",
                "只登记 timer_start 的现有字段，不创建新的 Timer runtime 或改变 onStart 顺序。",
                fields,
                true,
                "需要 Timer picker 和玩家目标模式控件；requiresOp/notifyOps 会被 ActionConfig canonical 归零。",
                "主摘要格式为启动 Timer xxx。"
        );
    }

    private static ActionSchema timerCancelSchema() {
        List<ActionFieldSchema> fields = commonFields(false);
        addTimerBaseFields(fields);
        fields.add(select(
                "timerMissingBehavior",
                "目标不存在时",
                "timer_cancel 找不到目标实例时的处理策略。",
                false,
                "noop_success",
                List.of(option("noop_success", "视为成功"), option("fail", "返回失败"), option("fail_if_missing", "返回失败（旧值兼容）")),
                "只对 timer_cancel 显示；fail_if_missing 是旧值兼容，不建议新 UI 默认选择。",
                "摘要显示缺失处理策略。"
        ));
        return schema(
                ActionType.TIMER_CANCEL,
                "取消 Timer",
                "通过 Scheduler 取消已有 Timer 实例。",
                "只登记 timer_cancel 的现有字段，不改变 cancel 先移除实例再执行 onCancel 的顺序。",
                fields,
                true,
                "需要 Timer picker 和玩家目标模式控件；requiresOp/notifyOps 会被 ActionConfig canonical 归零。",
                "主摘要格式为取消 Timer xxx。"
        );
    }

    private static void addTimerBaseFields(List<ActionFieldSchema> fields) {
        fields.add(text("timerId", "计时器 ID", "引用已有 Timer 配置 ID。", true, "", 96, "使用 Timer picker；高级 ID 可保留。", "主摘要显示 timerId。"));
        fields.add(select(
                "timerTargetMode",
                "目标模式",
                "选择 Timer runtime 实例作用域目标。",
                false,
                "context_player",
                targetModeOptions(),
                "explicit_target 需要填写 timerTargetId。",
                "摘要显示目标模式。"
        ));
        fields.add(text("timerTargetId", "指定目标", "显式玩家目标 ID；非 explicit_target 时可为空。", false, "", 64, "使用玩家目标输入或 picker。", "通常作为副摘要。"));
    }

    private static ActionSchema schema(
            ActionType actionType,
            String displayName,
            String description,
            String helpText,
            List<ActionFieldSchema> fields,
            boolean requiresTargetPicker,
            String editorHint,
            String summaryHint
    ) {
        return new ActionSchema(
                actionType,
                actionType.id(),
                displayName,
                description,
                helpText,
                fields,
                CURRENT_ACTION_OWNERS,
                true,
                requiresTargetPicker,
                editorHint,
                summaryHint
        );
    }

    private static List<ActionFieldSchema> commonFields(boolean includePrivilegeFields) {
        List<ActionFieldSchema> fields = new ArrayList<>();
        fields.add(bool("enabled", "启用", "关闭后该动作在旧 runtime 中会被跳过。", true, "true", "用 checkbox 表示。", "作为副摘要。"));
        fields.add(number("cooldownTicks", "冷却 ticks", "Action 冷却字段，范围 0 到 72000。", false, "0", 0L, MAX_ACTION_COOLDOWN_TICKS, "使用 number input。", "作为副摘要。"));
        if (includePrivilegeFields) {
            fields.add(bool("requiresOp", "需要 OP", "执行前要求玩家具有 OP 权限。", false, "false", "用 checkbox 表示。", "作为风险副摘要。"));
            fields.add(bool("notifyOps", "通知 OP", "执行结果是否通知管理员。", false, "false", "用 checkbox 表示。", "作为审计副摘要。"));
        }
        fields.add(new ActionFieldSchema(
                "conditionGroupId",
                "条件组",
                "可选外层 gate；留空必须保持旧逻辑懒跳过。",
                ActionFieldType.CONDITION_GROUP_PICKER,
                false,
                "",
                96,
                null,
                null,
                List.of(),
                "使用 condition group picker；兼容性由后续 owner capability validation 负责。",
                "作为副摘要。"
        ));
        return fields;
    }

    private static ActionFieldSchema text(
            String id,
            String label,
            String description,
            boolean required,
            String defaultValue,
            int maxLength,
            String editorHint,
            String summaryHint
    ) {
        return new ActionFieldSchema(id, label, description, ActionFieldType.TEXT, required, defaultValue, maxLength, null, null, List.of(), editorHint, summaryHint);
    }

    private static ActionFieldSchema textarea(
            String id,
            String label,
            String description,
            boolean required,
            String defaultValue,
            int maxLength,
            String editorHint,
            String summaryHint
    ) {
        return new ActionFieldSchema(id, label, description, ActionFieldType.TEXTAREA, required, defaultValue, maxLength, null, null, List.of(), editorHint, summaryHint);
    }

    private static ActionFieldSchema number(
            String id,
            String label,
            String description,
            boolean required,
            String defaultValue,
            Long minNumber,
            Long maxNumber,
            String editorHint,
            String summaryHint
    ) {
        return new ActionFieldSchema(id, label, description, ActionFieldType.NUMBER, required, defaultValue, null, minNumber, maxNumber, List.of(), editorHint, summaryHint);
    }

    private static ActionFieldSchema bool(
            String id,
            String label,
            String description,
            boolean required,
            String defaultValue,
            String editorHint,
            String summaryHint
    ) {
        return new ActionFieldSchema(id, label, description, ActionFieldType.BOOLEAN, required, defaultValue, null, null, null, List.of(), editorHint, summaryHint);
    }

    private static ActionFieldSchema select(
            String id,
            String label,
            String description,
            boolean required,
            String defaultValue,
            List<ActionFieldOption> options,
            String editorHint,
            String summaryHint
    ) {
        return new ActionFieldSchema(id, label, description, ActionFieldType.SELECT, required, defaultValue, null, null, null, options, editorHint, summaryHint);
    }

    private static List<ActionFieldOption> targetModeOptions() {
        return List.of(
                option("global", "全局"),
                option("context_player", "触发玩家"),
                option("explicit_target", "指定玩家")
        );
    }

    private static ActionFieldOption option(String value, String label) {
        return new ActionFieldOption(value, label);
    }

    static List<ActionType> declaredActionTypesForTest() {
        return Arrays.asList(ActionType.values());
    }
}
