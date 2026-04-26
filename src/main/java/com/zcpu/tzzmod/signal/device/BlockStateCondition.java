package com.zcpu.tzzmod.signal.device;

import java.util.Map;

public record BlockStateCondition(
        String blockId,
        Map<String, String> properties,
        String raw
) {
}
