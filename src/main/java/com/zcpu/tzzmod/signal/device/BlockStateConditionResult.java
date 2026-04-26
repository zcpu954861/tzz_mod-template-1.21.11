package com.zcpu.tzzmod.signal.device;

public record BlockStateConditionResult(
        boolean success,
        BlockStateCondition condition,
        String error
) {
    public static BlockStateConditionResult success(BlockStateCondition condition) {
        return new BlockStateConditionResult(true, condition, "");
    }

    public static BlockStateConditionResult failure(String error) {
        return new BlockStateConditionResult(false, null, error == null || error.isBlank() ? "条件无效。" : error);
    }
}
