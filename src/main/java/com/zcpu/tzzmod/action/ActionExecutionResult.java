package com.zcpu.tzzmod.action;

import net.minecraft.text.Text;

public record ActionExecutionResult(
        boolean success,
        Text message
) {
    public static ActionExecutionResult success(Text message) {
        return new ActionExecutionResult(true, message);
    }

    public static ActionExecutionResult failure(Text message) {
        return new ActionExecutionResult(false, message);
    }
}
