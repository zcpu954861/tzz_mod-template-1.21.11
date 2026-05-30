package com.zcpu.tzzmod.action.validation;

public final class ActionDraft {
    private final String type;
    private final String value;
    private final Object enabled;
    private final Object requiresOp;
    private final Object cooldownTicks;
    private final Object notifyOps;
    private final String conditionGroupId;
    private final String stateOperation;
    private final String stateScope;
    private final String stateTargetMode;
    private final String stateTargetId;
    private final String stateKey;
    private final String stateValueType;
    private final String stateValue;
    private final Object stateDelta;
    private final Object stateCreateIfMissing;
    private final String stateInitialValue;
    private final String timerId;
    private final String timerTargetMode;
    private final String timerTargetId;
    private final String timerStartPolicyOverride;
    private final Object timerDurationOverrideTicks;
    private final String timerMissingBehavior;

    private ActionDraft(Builder builder) {
        // Draft 是 WebAdmin 保存请求和 typed validation 中间层：保留 raw Object 字段，
        // 让后端能在构造 ActionConfig 之前 fail-closed，避免 ActionType.fromId 的 command fallback 污染保存路径。
        this.type = safe(builder.type);
        this.value = safe(builder.value);
        this.enabled = builder.enabled;
        this.requiresOp = builder.requiresOp;
        this.cooldownTicks = builder.cooldownTicks;
        this.notifyOps = builder.notifyOps;
        this.conditionGroupId = safe(builder.conditionGroupId);
        this.stateOperation = safe(builder.stateOperation);
        this.stateScope = safe(builder.stateScope);
        this.stateTargetMode = safe(builder.stateTargetMode);
        this.stateTargetId = safe(builder.stateTargetId);
        this.stateKey = safe(builder.stateKey);
        this.stateValueType = safe(builder.stateValueType);
        this.stateValue = safe(builder.stateValue);
        this.stateDelta = builder.stateDelta;
        this.stateCreateIfMissing = builder.stateCreateIfMissing;
        this.stateInitialValue = safe(builder.stateInitialValue);
        this.timerId = safe(builder.timerId);
        this.timerTargetMode = safe(builder.timerTargetMode);
        this.timerTargetId = safe(builder.timerTargetId);
        this.timerStartPolicyOverride = safe(builder.timerStartPolicyOverride);
        this.timerDurationOverrideTicks = builder.timerDurationOverrideTicks;
        this.timerMissingBehavior = safe(builder.timerMissingBehavior);
    }

    public static Builder builder(String type) {
        return new Builder(type);
    }

    public static ActionDraft simple(String type, String value) {
        return builder(type).value(value).build();
    }

    public String type() {
        return type;
    }

    public String value() {
        return value;
    }

    public Object enabled() {
        return enabled;
    }

    public Object requiresOp() {
        return requiresOp;
    }

    public Object cooldownTicks() {
        return cooldownTicks;
    }

    public Object notifyOps() {
        return notifyOps;
    }

    public String conditionGroupId() {
        return conditionGroupId;
    }

    public String stateOperation() {
        return stateOperation;
    }

    public String stateScope() {
        return stateScope;
    }

    public String stateTargetMode() {
        return stateTargetMode;
    }

    public String stateTargetId() {
        return stateTargetId;
    }

    public String stateKey() {
        return stateKey;
    }

    public String stateValueType() {
        return stateValueType;
    }

    public String stateValue() {
        return stateValue;
    }

    public Object stateDelta() {
        return stateDelta;
    }

    public Object stateCreateIfMissing() {
        return stateCreateIfMissing;
    }

    public String stateInitialValue() {
        return stateInitialValue;
    }

    public String timerId() {
        return timerId;
    }

    public String timerTargetMode() {
        return timerTargetMode;
    }

    public String timerTargetId() {
        return timerTargetId;
    }

    public String timerStartPolicyOverride() {
        return timerStartPolicyOverride;
    }

    public Object timerDurationOverrideTicks() {
        return timerDurationOverrideTicks;
    }

    public String timerMissingBehavior() {
        return timerMissingBehavior;
    }

    public static final class Builder {
        private String type;
        private String value = "";
        private Object enabled = Boolean.TRUE;
        private Object requiresOp = Boolean.FALSE;
        private Object cooldownTicks = 0;
        private Object notifyOps = Boolean.FALSE;
        private String conditionGroupId = "";
        private String stateOperation = "";
        private String stateScope = "";
        private String stateTargetMode = "";
        private String stateTargetId = "";
        private String stateKey = "";
        private String stateValueType = "";
        private String stateValue = "";
        private Object stateDelta = 0;
        private Object stateCreateIfMissing = Boolean.FALSE;
        private String stateInitialValue = "";
        private String timerId = "";
        private String timerTargetMode = "";
        private String timerTargetId = "";
        private String timerStartPolicyOverride = "";
        private Object timerDurationOverrideTicks = 0;
        private String timerMissingBehavior = "";

        private Builder(String type) {
            this.type = type;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder enabled(Object enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder requiresOp(Object requiresOp) {
            this.requiresOp = requiresOp;
            return this;
        }

        public Builder cooldownTicks(Object cooldownTicks) {
            this.cooldownTicks = cooldownTicks;
            return this;
        }

        public Builder notifyOps(Object notifyOps) {
            this.notifyOps = notifyOps;
            return this;
        }

        public Builder conditionGroupId(String conditionGroupId) {
            this.conditionGroupId = conditionGroupId;
            return this;
        }

        public Builder stateOperation(String stateOperation) {
            this.stateOperation = stateOperation;
            return this;
        }

        public Builder stateScope(String stateScope) {
            this.stateScope = stateScope;
            return this;
        }

        public Builder stateTargetMode(String stateTargetMode) {
            this.stateTargetMode = stateTargetMode;
            return this;
        }

        public Builder stateTargetId(String stateTargetId) {
            this.stateTargetId = stateTargetId;
            return this;
        }

        public Builder stateKey(String stateKey) {
            this.stateKey = stateKey;
            return this;
        }

        public Builder stateValueType(String stateValueType) {
            this.stateValueType = stateValueType;
            return this;
        }

        public Builder stateValue(String stateValue) {
            this.stateValue = stateValue;
            return this;
        }

        public Builder stateDelta(Object stateDelta) {
            this.stateDelta = stateDelta;
            return this;
        }

        public Builder stateCreateIfMissing(Object stateCreateIfMissing) {
            this.stateCreateIfMissing = stateCreateIfMissing;
            return this;
        }

        public Builder stateInitialValue(String stateInitialValue) {
            this.stateInitialValue = stateInitialValue;
            return this;
        }

        public Builder timerId(String timerId) {
            this.timerId = timerId;
            return this;
        }

        public Builder timerTargetMode(String timerTargetMode) {
            this.timerTargetMode = timerTargetMode;
            return this;
        }

        public Builder timerTargetId(String timerTargetId) {
            this.timerTargetId = timerTargetId;
            return this;
        }

        public Builder timerStartPolicyOverride(String timerStartPolicyOverride) {
            this.timerStartPolicyOverride = timerStartPolicyOverride;
            return this;
        }

        public Builder timerDurationOverrideTicks(Object timerDurationOverrideTicks) {
            this.timerDurationOverrideTicks = timerDurationOverrideTicks;
            return this;
        }

        public Builder timerMissingBehavior(String timerMissingBehavior) {
            this.timerMissingBehavior = timerMissingBehavior;
            return this;
        }

        public ActionDraft build() {
            return new ActionDraft(this);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
