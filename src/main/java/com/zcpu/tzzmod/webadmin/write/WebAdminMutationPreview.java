package com.zcpu.tzzmod.webadmin.write;

import java.util.Map;

public record WebAdminMutationPreview(
        WebAdminWriteTarget target,
        boolean changed,
        Map<String, Object> beforeSummary,
        Map<String, Object> afterSummary,
        WebAdminWriteResult validationResult
) {
    public WebAdminMutationPreview {
        target = target == null ? WebAdminWriteTarget.none() : target;
        beforeSummary = WebAdminWriteSanitizer.redactMap(beforeSummary);
        afterSummary = WebAdminWriteSanitizer.redactMap(afterSummary);
        validationResult = validationResult == null
                ? WebAdminWriteResult.ok(target, changed, "预览已生成。")
                : validationResult;
    }
}
